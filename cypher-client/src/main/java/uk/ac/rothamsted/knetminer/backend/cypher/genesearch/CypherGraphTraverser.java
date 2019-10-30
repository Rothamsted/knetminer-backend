package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;
import static uk.ac.rothamsted.knetminer.backend.cypher.genesearch.CyTraverserPerformanceTracker.CFGOPT_PERFORMANCE_REPORT_FREQ;
import static uk.ac.rothamsted.knetminer.backend.cypher.genesearch.CyTraverserPerformanceTracker.CFGOPT_TRAVERSER_PERFORMANCE;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
 * <p>This traverser expects the following in {@link #getOptions()}:
 * <ul>
 * 	<li>{@link #CFGOPT_PATH} set to a proper Spring config file.</li>
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
	public static final String CFGOPT_PATH = "knetminer.backend.configPath";

	/**
	 * This allows for changing {@link #getPageSize()} via {@link #setOption(String, Object)}.
	 */
	public static final String CFGOPT_CY_PAGE_SIZE = "knetminer.backend.cypher.pageSize";

	/**
	 * This allows for stopping queries when they don't complete within a given time.
	 * We use a pretty low default (usually 1s) if you don't specify it. 
	 * -1 means the queries aren't timed out on the client side, but the Neo4j server might still be 
	 * enforcing limits.
	 */
	public static final String CFGOPT_CY_QUERY_TIMEOUT = "knetminer.backend.cypher.queryTimeoutMs";

	
	protected static AbstractApplicationContext springContext;
	

	/** Tracks how many queries x genes have been completed so far */
	private PercentProgressLogger queryProgressLogger = null;
	
	private static final TimeLimiter TIME_LIMITER = new SimpleTimeLimiter ();

	
	/**
	 * Has to be initialised by {@link AbstractGraphTraverser#traverseGraph(ONDEXGraph, java.util.Set, FilterPaths)}.
	 */
	protected CyTraverserPerformanceTracker performanceTracker = null;

	protected final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public CypherGraphTraverser () {}
	
	
	private void init ()
	{
		// Double-check lazy init (https://www.geeksforgeeks.org/java-singleton-design-pattern-practices-examples/)
		if ( springContext != null ) return;
		
		synchronized ( CypherGraphTraverser.class )
		{
			if ( springContext != null ) return;
			
			String cfgPath = this.getOption ( CFGOPT_PATH, "backend/config.xml" );
			File cfgFile = new File ( cfgPath );
			
			if ( !cfgFile.exists () ) ExceptionUtils.throwEx ( 
				UncheckedFileNotFoundException.class,
				"Backend configuration file '%s' not found, please set %s correctly",
				cfgFile.getAbsolutePath (),
				CFGOPT_PATH
			);
			
			String furl = "file:///" + cfgFile.getAbsolutePath ();
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
	 *  <li>Each returned Cypher node/relation must carry an {@code iri} property, coherent with the parameter {@code graph}.</li>
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
  	long queryTimeout = this.getOption ( CFGOPT_CY_QUERY_TIMEOUT, 3000l, Long::parseLong );
 		
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
		// All the results
		final List<EvidencePathNode> result = Collections.synchronizedList ( new ArrayList<> ( 40 ) );

		// Loop over all the queries with the parameter gene
		// We close this stream here because we noticed "connection already closed" errors with neo4j
		//
		try ( Stream<String> queries = getCypherQueries ().parallelStream (); )
		{
			queries.forEach ( query -> 
			{
				// Results from this query in raw format, ie, lists of URIs
				// We need to do this in two steps, ie, first the IRIs then the Ondex entities obtained via
				// Ondex Lucene Index, because the query can be interrupted and this is very bad for lucene
				// (https://issues.apache.org/jira/browse/LUCENE-7248).
				//
				// Note on sycnrhonizedList: it's because we're seeing unexplicable ConcurrentModificationException
				// I suspect they come from interrupted threads.
				//
				List<List<String>> queryResultIris = new ArrayList<> ( 20 );
				final List<List<String>> roQueryResultIris = queryResultIris; // just for the lambda RO requirements
				
				// Used below, by the performanceTracker
				int counters[] = { 0, 0 };

				Runnable queryAction = 
				() -> {
					try (	final PagedCyPathFinder pathsItr = 
									new PagedCyPathFinder (	startGeneIri, query, getPageSize (), cyProvider )
					)
					{
		  			// For each configured semantic motif query, get the paths from Neo4j + indexed resource
						pathsItr.forEachRemaining ( 
						pathIris -> {
							roQueryResultIris.add ( pathIris );
	  					counters [ 0 ]++; // no. of resulting paths
	  					counters [ 1 ] += pathIris.size (); // total path lengths
						});
					} // try-with pathsItr
	  		}; // queryAction				
				
  			// Don't allow it to run too long (if queryTimeout != -1)
				Runnable timedQueryAction = () -> timedQuery ( queryAction, queryTimeout, startGeneIri, query ); 
	
				// Further wrap it with machinery that accumulates query performance-related stats
				try {
					performanceTrackedQuery ( timedQueryAction, query, counters );
				}
				catch ( UncheckedTimeoutException ex ) {
					// The query didn't complete within the timeout, results are partial, we must invalidate
					// everything
					queryResultIris = null;
					if ( log.isTraceEnabled () )
						log.trace ( "Query timed out. Gene: <{}>, query: {}", startGeneIri, query );
				}

				// DEBUG log.info ( "--- DONE (Cypher): <{}>, query: \"{}\"", startGeneIri, escapeJava ( query ) );

				// OK, now convert the IRIs into Ondex entities
				if ( queryResultIris != null )
					try
					{
						// TODO: possibly fixed, remove synch
						// We're seeing ConcurrentModificationException, my suspect is this is caused by 
						// interrupted threads.
						// synchronized ( queryResultIris ) 
						{
							CypherClient.findPathsFromIris ( graph, queryResultIris.stream () )
							.map ( this::buildEvidencePath )
							.forEach ( result::add );
						}
					}
					// TODO: possibly fixed, remove synch and CME
					catch ( ConcurrentModificationException ex ) 
					{
						// TODO: shouldn't happen, but just in case
						log.warn ( 
							"For some reason, I got ConcurrentModificationException while traversing Gene: <{}>, with query: {}", 
							startGeneIri,	query
						);
					}

				// DEBUG log.info ( "--- DONE (Lucene): <{}>, query: \"{}\"", startGeneIri, escapeJava ( query ) );
				
				if ( this.queryProgressLogger != null ) this.queryProgressLogger.updateWithIncrement ();
			}); // query stream.forEach
		} // try-with queries
				
		
		// This is an optional method to filter out unwanted results. In Knetminer it's usually null
		return filter == null ? result : filter.filterPaths ( result );
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
	 * Runs a query action with time restrictions (if queryTimeOutMs != -1).
	 * 
	 * if the query can't run within its time limits, #UncheckedTimeoutException is thrown. This is possibly
	 * intercepted by {@link CyTraverserPerformanceTracker}.
	 * 
	 */
	private void timedQuery ( Runnable queryAction, long queryTimeoutMs, String startGeneIri, String query )
	{
		try
		{
			// No timeout wanted
			if ( queryTimeoutMs == -1l ) {
				queryAction.run ();
				return;
			}
						
			TIME_LIMITER.callWithTimeout ( 
				Executors.callable ( queryAction ), queryTimeoutMs, TimeUnit.MILLISECONDS, true 
			);
		}
		catch ( UncheckedTimeoutException|InterruptedException ex ) {
			// Don't wrap it with other exception types, but let it flow to the performance tracker
			throw ExceptionUtils.buildEx ( 
				UncheckedTimeoutException.class,
				ex,
				"Timed out query: %s. Gene IRI is: <%s>. Query is: \"%s\"",
				ex.getMessage (),
				startGeneIri,
				StringEscapeUtils.escapeJava ( query )
			);
		}
		catch ( Exception ex )
		{
			throw ExceptionUtils.buildEx ( 
				GenericNeo4jException.class,
				ex,
				"Error while traversing Neo4j gene graph: %s. Gene IRI is: <%s>. Query is: \"%s\"",
				ex.getMessage (),
				startGeneIri,
				StringEscapeUtils.escapeJava ( query )
			);
		}			
	}	
	
	/**
	 * Another wrapper for a query action that is used to 
	 * {@link CyTraverserPerformanceTracker track the query performance}. 
	 * 
	 */
	private void performanceTrackedQuery ( Runnable queryAction, String query, int[] counters )
		throws UncheckedTimeoutException
	{
		if ( this.performanceTracker == null )
			queryAction.run ();
		else
			this.performanceTracker.track ( query, queryAction, () -> counters [ 0 ], () -> counters [ 1 ] );		
	}
	
	
	
	
	/**
	 * The page size used with Cypher queries. This is used with PagedCyPathFinder to query the Cypher server in a paged
	 * fashion, using pages of this size. This value can be set via {@link #setOption(String, Object)}, 
	 * using {@link #CFGOPT_CY_PAGE_SIZE}. The default of 2500 should be fine in most cases.
	 * 
	 */
	public long getPageSize () {
		return this.getOption ( CFGOPT_CY_PAGE_SIZE, 2500l, Long::parseUnsignedLong );
	}

	public void setPageSize ( long pageSize ) {
		this.setOption ( CFGOPT_CY_PAGE_SIZE, pageSize );
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
		
		if ( this.getOption ( CFGOPT_TRAVERSER_PERFORMANCE, false, Boolean::parseBoolean ) )
		{
			this.performanceTracker = new CyTraverserPerformanceTracker ();
			this.performanceTracker.setReportFrequency (
				this.getOption ( CFGOPT_PERFORMANCE_REPORT_FREQ, -1, Integer::parseInt )
			);
		}
				
		this.queryProgressLogger = new PercentProgressLogger ( 
			"{}% of graph traversing queries processed",
			concepts.size () * getCypherQueries ().size (),
			1 // TODO: debug, go back to 10 sooner than later
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

	
	@SuppressWarnings ( "unchecked" )
	protected static List<String> getCypherQueries ()
	{
		return (List<String>) springContext.getBean ( "semanticMotifsQueries" );
	}
	
}
