package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.neo4j.driver.v1.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import net.sourceforge.ondex.algorithm.graphquery.AbstractGraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.FilterPaths;
import net.sourceforge.ondex.algorithm.graphquery.GraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode.EvidenceConceptNode;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode.EvidenceRelationNode;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode.FirstEvidenceConceptNode;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import net.sourceforge.ondex.core.util.ONDEXGraphUtils;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.ebi.utils.runcontrol.PercentProgressLogger;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClientProvider;
import uk.ac.rothamsted.neo4j.utils.GenericNeo4jException;

/**
 * <p>A {@link AbstractGraphTraverser graph traverser} based on Cypher queries against a property graph database
 * storing a BioKNO-based model of an Ondex/Knetminer graph. Currently the backend datbase is based on Neo4j.</p>
 * 
 * <p>This traverser expects certain initialisation parameters:
 * <ul>
 * 	<li>{@link #CONFIG_PATH_OPT} set to a proper Spring config file.</li>
 * 	<li>{@code LuceneEnv}, which must be a proper {@link LuceneEnv} instance, corresponding to the {@code graph} parameter
 * 	received by {@link #traverseGraph(ONDEXGraph, ONDEXConcept, FilterPaths)}</li>
 * </ul>
 * 
 * Usually the above params are properly set by {@code rres.knetminer.datasource.ondexlocal.OndexServiceProvider}. 
 * </p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Jan 2019</dd></dl>
 *
 */
public class CypherGraphTraverser extends AbstractGraphTraverser
{
	/**
	 * <p>We use a Spring-based configuration file to read Neo4j configuration, which also has to declare the list or the
	 * folder where .cypher query files are stored.</p> 
	 * 
	 * <p>Each query select a path departing from a gene passed as parameter, as it is specified by {@link CypherClient}.</p>
	 * 
	 * <p>See tests for examples of the kind of file expected here. Default value for this option is {@code "backend/config.xml"}.</p>
	 * 
	 */
	public static final String CONFIG_PATH_OPT = "knetminer.backend.configPath";

	/**
	 * This allows for changing {@link #getPageSize()} via {@link #setOption(String, Object)}.
	 */
	public static final String CONFIG_CY_PAGE_SIZE = "knetminer.backend.cypher.pageSize";

	/**
	 * This allows for stopping queries when they don't complete within a given time.
	 * We use a pretty low default (usually 1s) if you don't specify it. 
	 * -1 means the queries aren't timed out on the client side, but the Neo4j server might still be 
	 * enforcing limits.
	 */
	public static final String CONFIG_CY_QUERY_TIMEOUT = "knetminer.backend.cypher.queryTimeoutMs";

	
	protected static AbstractApplicationContext springContext;
	

	/** Tracks how many queries x genes have been completed so far */
	private PercentProgressLogger queryProgressLogger = null;
	
	/**
	 * Has to be initialised by {@link AbstractGraphTraverser#traverseGraph(ONDEXGraph, java.util.Set, FilterPaths)}.
	 */
	protected CyTraverserPerformanceTracker performanceTracker = null;

	/** Used in {@link #queryWithTimeout(String, String, LuceneEnv, CypherClientProvider, long)} */
	private TimeLimiter timeLimiter = new SimpleTimeLimiter ();

	protected final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public CypherGraphTraverser () {}
	
	
	private void init ()
	{
		// Double-check lazy init (https://www.geeksforgeeks.org/java-singleton-design-pattern-practices-examples/)
		if ( springContext != null ) return;
		
		synchronized ( GraphTraverser.class )
		{
			if ( springContext != null ) return;
			
			String cfgPath = this.getOption ( CONFIG_PATH_OPT, "backend/config.xml" );
			File cfgFile = new File ( cfgPath );
			
			if ( !cfgFile.exists () ) ExceptionUtils.throwEx ( 
				UncheckedFileNotFoundException.class,
				"Backend configuration file '%s' not found, please set %s correctly",
				cfgFile.getAbsolutePath (),
				CONFIG_PATH_OPT
			);
			
			String furl = "file://" + cfgFile.getAbsolutePath ();
			log.info ( "Configuring {} from <{}>", this.getClass ().getCanonicalName (), furl );
			springContext = new FileSystemXmlApplicationContext ( furl );
			springContext.registerShutdownHook ();
			log.info ( "{} configured", this.getClass ().getCanonicalName () );
		}		
	}
	
	/**
	 * Uses queries in the {@link #springContext} bean named {@code semanticMotifsQueries}. Each query must fulfil certain 
	 * requirement:
	 * 
	 * <ul>
	 * 	<li>The query must return a path as first projected result (see {@link CypherClient#findPathIris(String, Value)}</li>
	 * 	<li>Every returned path must match an Ondex gene concept as first node, followed by Cypher entities corresponding
	 * to Ondex concept/relation pairs</li>
	 * 	<li>The first matching node of each query must receive a {@code startQuery} parameter: 
	 * {@code MATCH path = (g1:Gene{ iri: $startIri }) ...}. See the tests in this hereby project for details.</li>
	 *  <li>Each returned Cypher node/relation must carry an {@code iri} property, coherent with the parameter {@code graph}.
	 *  The parameter OXL graph must also match the {@link LuceneEnv Lucene index} set by {@link #init()}, via the 
	 *  {@code LuceneEnv} {@link #getOption(String) option}.
	 *  </li>
	 * </ul>
	 * 
	 */
	@Override
	@SuppressWarnings ( { "rawtypes" } )
	public List<EvidencePathNode> traverseGraph ( 
		ONDEXGraph graph, ONDEXConcept concept, FilterPaths<EvidencePathNode> filter
	)
	{
		init ();
		
		LuceneEnv luceneMgr = this.getOption ( "LuceneEnv" );
  	if ( luceneMgr == null ) throw new IllegalArgumentException (
  			"Cannot initialise the Cypher resourceResource traverser: "
  		+ "you must pass me the LuceneEnv option (see OndexServiceProvider)"
  	);
  	
  	long queryTimeout = this.getOption ( CONFIG_CY_QUERY_TIMEOUT, 1000 );
 		
		CypherClientProvider cyProvider = springContext.getBean ( CypherClientProvider.class );
		
		// So, let's get the starting IRI from the concept parameter.
		String startGeneIri = Optional
			.ofNullable ( ONDEXGraphUtils.getAttribute ( graph, concept, "iri" ) )
			.map ( attr -> (String) attr.getValue () )
			.orElseThrow ( () -> ExceptionUtils.buildEx (
				IllegalStateException.class, 
				"No attribute 'iri' defined for the concept %s, Cypher backend needs OXL files with IRI/URI attributes", 
				ONDEXGraphUtils.getString ( concept ) 
			));
				
		// And now let's hand it to Cypher.
		List<EvidencePathNode> result;
		
		// By wrapping the stream in a a try/with, close() is triggered at the end that closes 
		// the underlining Cypher transactions
		//
		try ( 
			// We're returning a stream of paths per query (each query can return more than one)
			// which need to be flat-mapped to the outside (the stream of stream of paths becomes a single stream of paths)
			Stream<EvidencePathNode> resultStrm = getCypherQueries ()
				.parallelStream ()
				.flatMap ( query -> 
				{
					//log.info ( "traversing the gene: \"{}\" with the query: <<{}>>", startGeneIri, query );

					// For each configured semantic motif query, get the paths from Neo4j + indexed resource
					// But we also need to wrap it into a timeout trigger
					Stream<List<ONDEXEntity>> cypaths = this.queryWithTimeout ( 
						query, startGeneIri, luceneMgr, cyProvider, queryTimeout 
					);
											
					// Now map the paths to the format required by the traverser (see above)
					return cypaths
						.map ( path -> buildEvidencePath ( path ) )
						.onClose ( () -> 
						{ 
							if ( queryProgressLogger != null ) queryProgressLogger.updateWithIncrement ();
							//log.info ( "/end traversal of the gene: \"{}\" with the query: [{}]", startGeneIri, query ) ;
						});
				})
		) // try-with
		{
			// Now further convert into a collection, the format required as return value.
			result = resultStrm.collect ( Collectors.toList () );
		}
					
		// This is an optional method to filter out unwanted results. In Knetminer it's usually null
		if ( filter != null ) result = filter.filterPaths ( result );

		return result;
	}

	/**
	 * Utility to convert a list of {@link ONDEXEntity}ies, interpreted as a chain of concept/relation pairs, to an
	 * {@link EvidencePathNode evidence path}, as defined by the {@link AbstractGraphTraverser graph traverser interface}.
	 * 
	 */
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
	private EvidencePathNode buildEvidencePath ( List<ONDEXEntity> ondexEntities )
	{
		EvidencePathNode result = null; 
		for ( ONDEXEntity odxEnt: ondexEntities ) 
		{ 
			if ( odxEnt == null ) throw buildEx ( 
				IllegalStateException.class, 
				"Internal error: Cypher Graph Traverser got a null ONDEX entity from Cypher"
			);

			if ( odxEnt instanceof ONDEXConcept )
			{
				ONDEXConcept concept = (ONDEXConcept) odxEnt;
				State evidence = new State ( concept.getOfType () );
				result = result == null 
					? new FirstEvidenceConceptNode ( concept, evidence )
					: new EvidenceConceptNode ( concept, evidence, result );	
			}
			else if ( odxEnt instanceof ONDEXRelation )
			{
				ONDEXRelation rel = (ONDEXRelation) odxEnt;
				Transition evidence = new Transition ( rel.getOfType () );
				result = new EvidenceRelationNode ( rel, evidence, result );
			}
			else throwEx ( 
				IllegalStateException.class, 
				"Internal error: Cypher Graph Traverser cannot deal with instances of %s",
				odxEnt.getClass ().getCanonicalName () 
			);
		}
		if ( result == null ) throwEx ( 
			IllegalStateException.class, 
			"Internal error: Cypher Graph Traverser got a null result from entity->evidence-path conversion"
		);
		return result;
	}
	
	/**
	 * The page size used with Cypher queries. {@link #findPathsWithPaging(LuceneEnv, String, String)} is the method where
	 * this is used. This value can be set via {@link #setOption(String, Object)}, using {@link #CONFIG_CY_PAGE_SIZE}.
	 * 
	 */
	public long getPageSize () {
		return this.getOption ( CONFIG_CY_PAGE_SIZE, 500 );
	}

	public void setPageSize ( long pageSize ) {
		this.setOption ( CONFIG_CY_PAGE_SIZE, pageSize );
	}
	
	/**
	 * Wraps the default implementation to enable to track query performance, via {@link CyTraverserPerformanceTracker}. 
	 */
	@Override
	@SuppressWarnings ( "rawtypes" )
	public Map<ONDEXConcept, List<EvidencePathNode>> traverseGraph ( 
		ONDEXGraph graph, Set<ONDEXConcept> concepts, FilterPaths<EvidencePathNode> filter )
	{
		init ();
		
		if ( this.getOption ( "isPerformanceTrackingEnabled", false ) )
			this.performanceTracker = new CyTraverserPerformanceTracker ();
				
		this.queryProgressLogger = new PercentProgressLogger ( 
			"{}% of graph traversing queries processed",
			concepts.size () * getCypherQueries ().size () 
		);
		
		try {
			return super.traverseGraph ( graph, concepts, filter );
		}
		finally 
		{
			if ( this.performanceTracker != null ) this.performanceTracker.logStats ();
			this.performanceTracker = null;
			this.queryProgressLogger = null;
		}
	}
	
	
	private Stream<List<ONDEXEntity>> queryWithTimeout ( 
		String query, String startGeneIri, LuceneEnv luceneMgr, CypherClientProvider cyProvider,
		long timeoutMs
	)
	{
		Callable<Stream<List<ONDEXEntity>>> queryAction = () -> PagedCyPathFinder.findPathsWithPaging ( 
			startGeneIri, query, this.getPageSize (), luceneMgr, cyProvider, performanceTracker
		);

		try
		{
			// No timeout wanted
			if ( timeoutMs == -1 ) return queryAction.call ();
			
			return this.timeLimiter.callWithTimeout ( 
				queryAction, timeoutMs, TimeUnit.MILLISECONDS, true 
			);
		}
		catch ( UncheckedTimeoutException ex ) 
		{
			if ( this.performanceTracker != null )
				// TODO: log once when the tracker is disabled
				this.performanceTracker.trackTimeout ( query );
			return Stream.empty ();
		}
		catch ( Exception ex )
		{
			throw ExceptionUtils.buildEx ( 
				GenericNeo4jException.class, 
				"Error while traversing Neo4j gene graph: {}. Gene IRI is: <{}>. Query is: \"{}\"",
				ex.getMessage (),
				startGeneIri,
				StringEscapeUtils.escapeJava ( query )
			);
		}	
	}
	
	@SuppressWarnings ( "unchecked" )
	protected static List<String> getCypherQueries ()
	{
		return (List<String>) springContext.getBean ( "semanticMotifsQueries" );
	}
	
}
