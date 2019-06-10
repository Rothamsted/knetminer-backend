package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

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
import uk.ac.ebi.utils.time.XStopWatch;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClientProvider;

/**
 * <p>A {@link AbstractGraphTraverser graph traverser} based on Cypher queries against a property graph database
 * storing a BioKNO-based model of an Ondex/Knetminer graph. Currently the backend datbase is based on Neo4j.</p>
 * 
 * <p>This traverser expects certain initialisaiton parameters:
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
	
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private static AbstractApplicationContext springContext;
	
	private static class QueryPerformanceTracker 
	{
		private Map<String, Long> query2Times = Collections.synchronizedMap ( new HashMap<> () );
		private Map<String, Integer> query2Results = Collections.synchronizedMap ( new HashMap<> () );
		private Map<String, Long> query2PathLen = Collections.synchronizedMap ( new HashMap<> () );
		private AtomicInteger invocations = new AtomicInteger ( 0 );
		
		private final Logger log = LoggerFactory.getLogger ( this.getClass () );
		
		/**
		 * A wrapper that tracks the performance of multiple queries, during the execution of the 
		 * {@link CypherGraphTraverser#traverseGraph(ONDEXGraph, Set, FilterPaths) multi-gene traversal}.
		 */
		public QueryPerformanceTracker () {}

		public Stream<List<ONDEXEntity>> track (
			Function<String, Stream<List<ONDEXEntity>>> queryAction,
			String query
		)
		{
			@SuppressWarnings ( "unchecked" )
			Stream<List<ONDEXEntity>> result[] = new Stream [ 1 ]; 
			long time = XStopWatch.profile ( () -> { result [ 0 ] = queryAction.apply ( query ); } );
			result [ 0 ] = result [ 0 ].peek ( pathEls -> {
				query2Times.compute ( query, (q,t) -> t == null ? time : t + time );
				query2Results.compute ( query, (q,nr) -> nr == null ? 1 : nr + 1 );
				query2PathLen.compute ( query, (q,pl) -> pl == null ? pathEls.size () : pl + pathEls.size () );
			});
			invocations.incrementAndGet ();
			return result [ 0 ];
		}
		
		public void logStats ()
		{
			StringWriter statsSW = new StringWriter ();
			PrintWriter out = new PrintWriter ( statsSW );
			
			final int nqueries = invocations.get ();
			out.printf ( "\n\nTotal queries issued: %s\n", nqueries );
			if ( nqueries == 0 ) return;
			
			out.printf ( "\nQuery\tTot Results\tAvg Time(ms)\tAvg #Result\tAvg Path Len\t\n" );
			
			SortedSet<String> queries = new TreeSet<> ( (s1, s2) -> s1.length () - s2.length () );
			queries.addAll ( query2Times.keySet () );
			for ( String query: queries )
			{
				int nresults = query2Results.get ( query );
				out.printf (
					"\"%s\"\t%d\t%#6.2f\t%#6.2f\t%#6.2f", 
					escapeJava ( query ),
					nresults,
					1d * query2Times.get ( query ) / nqueries,
					1d * nresults / nqueries,
					nresults == 0 ? 0d : 1d * query2PathLen.get ( query ) / nresults
				);
			}
			out.println ( "" );

			log.info ( "\n\n  -------------- Cypher Graph Traverser, Query Stats --------------\n{}", statsSW.toString () );
		}
	}
	
	/**
	 * Has to be initialised by {@link AbstractGraphTraverser#traverseGraph(ONDEXGraph, java.util.Set, FilterPaths)}.
	 */
	private QueryPerformanceTracker performanceTracker = null;
	
	
	public CypherGraphTraverser () {}
	
	
	private void init ()
	{
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
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
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
 		
		List<String> cypherQueries = (List<String>) springContext.getBean ( "semanticMotifsQueries" );
		CypherClientProvider cyProvider = springContext.getBean ( CypherClientProvider.class );
		
		// So, let's get the starting IRI from the concept parameter.
		String startIri = Optional
			.ofNullable ( ONDEXGraphUtils.getAttribute ( graph, concept, "iri" ) )
			.map ( attr -> (String) attr.getValue () )
			.orElseThrow ( () -> ExceptionUtils.buildEx (
				IllegalStateException.class, 
				"No attribute 'iri' defined for the concept %s, Cypher backend needs OXL files with IRI/URI attributes", 
				ONDEXGraphUtils.getString ( concept ) 
		));
				
		// And now let's hand it to Cypher
		//
		
		Value startIriParam = Values.parameters ( "startIri", startIri );

		// Query and convert the results to the appropriate format.
		List<EvidencePathNode> result = cypherQueries
		.parallelStream ()
		.flatMap ( query -> 
		{
			// For each configured semantic motif query, get the paths from Neo4j + indexed resource
			Function<String, Stream<List<ONDEXEntity>>> queryAction = q -> cyProvider.query (
				cyClient -> cyClient.findPaths ( luceneMgr, query, startIriParam )
			);

			// Possibly wrap it into the performance tracker
			Stream<List<ONDEXEntity>> cypaths = this.performanceTracker == null  
				? queryAction.apply ( query )
				: this.performanceTracker.track ( queryAction, query );
					
			// Now map the paths to the format required by the traverser
			// We're returning a stream of paths, (each query can return more than one)
			return cypaths.map ( path -> buildEvidencePath ( path ) );
		})
		.collect ( Collectors.toList () ); // And eventually we extract List<EvPathNode>
				
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
	 * Wraps the default implementation to enable to track query performance, via {@link QueryPerformanceTracker}. 
	 */
	@Override
	@SuppressWarnings ( "rawtypes" )
	public Map<ONDEXConcept, List<EvidencePathNode>> traverseGraph ( ONDEXGraph graph, Set<ONDEXConcept> concepts,
			FilterPaths<EvidencePathNode> filter )
	{
		this.performanceTracker = new QueryPerformanceTracker ();
		try {
			return super.traverseGraph ( graph, concepts, filter );
		}
		finally {
			this.performanceTracker.logStats ();
		}
	}
		
}
