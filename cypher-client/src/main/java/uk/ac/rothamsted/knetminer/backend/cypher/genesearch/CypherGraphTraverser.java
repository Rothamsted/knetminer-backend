package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
	
	protected final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private static AbstractApplicationContext springContext;
	
	private static class QueryPerformanceTracker 
	{
		/** Times to send the query and get a first reply **/
		private Map<String, Long> query2StartTimes = Collections.synchronizedMap ( new HashMap<> () );
		/** Times to fetch all the results **/
		private Map<String, Long> query2FetchTimes = Collections.synchronizedMap ( new HashMap<> () );
		/** No of results (pahts) returned by each query **/
		private Map<String, Integer> query2Results = Collections.synchronizedMap ( new HashMap<> () );

		/** No of query invocations ({@code #invocations = #queries x #genes}) **/
		private Map<String, Integer> query2Invocations = Collections.synchronizedMap ( new HashMap<> () );
		/** Sums of returned path lengths for the each query **/
		private Map<String, Long> query2PathLen = Collections.synchronizedMap ( new HashMap<> () );
		
		/** Total no. of invocations, ie #queries x #genes **/ 
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
			
			long startTime = XStopWatch.profile ( () -> { result [ 0 ] = queryAction.apply ( query ); } );
			query2StartTimes.compute ( query, (q,t) -> t == null ? startTime : t + startTime );
			
			XStopWatch fetchTimer = new XStopWatch ();
			
			result [ 0 ] = result [ 0 ]
			.peek ( pathEls -> 
			{
				// stats timing the first time/path it's invoked, then onClose() will get the total time elapsed
				// after all the stream has been consumed
				fetchTimer.resumeOrStart (); 
				query2Results.compute ( query, (q,nr) -> nr == null ? 1 : nr + 1 );
				query2PathLen.compute ( query, (q,pl) -> pl == null ? pathEls.size () : pl + pathEls.size () );
			})
			.onClose ( () ->
				// Track what is presumably the fetch time 
			  // (we come here after all the paths in the stream have been consumed) 
				query2FetchTimes.compute (
					query, 
					(q,t) -> !fetchTimer.isStarted () ? 0 : fetchTimer.getTime () + ( t == null ? 0 : t ) )
			);
			
			query2Invocations.compute ( query, (q,n) -> n == null ? 0 : n + 1 );
			invocations.incrementAndGet ();
			
			return result [ 0 ];
		}
		
		public void logStats ()
		{
			StringWriter statsSW = new StringWriter ();
			PrintWriter out = new PrintWriter ( statsSW );
			
			final int nTotQueries = invocations.get ();
			out.printf ( "\n\nTotal queries issued: %s\n", nTotQueries );
			if ( nTotQueries == 0 ) return;
			
			out.println (   
				"Query\tTot Invocations\tTot Returned Paths\tAvg Ret Paths\tAvg Start Time(ms)\tAvg Fetch Time(ms)\tAvg Path Len" 
			);
			
			SortedSet<String> queries = new TreeSet<> ( (s1, s2) -> s1.length () - s2.length () );
			queries.addAll ( query2Invocations.keySet () );
			for ( String query: queries )
			{
				int nresults = query2Results.getOrDefault ( query, 0 );
				int nqueries = query2Invocations.get ( query );
				
				out.printf (
					"\"%s\"\t%d\t%d\t%#6.2f\t%#6.2f\t%#6.2f\t%#6.2f\n",
					escapeJava ( query ),
					nqueries,
					nresults,
					nqueries == 0 ? 0d : 1d * nresults / nqueries,
					nqueries == 0 ? 0d : 1d * query2StartTimes.getOrDefault ( query, 0l ) / nqueries,
					nqueries == 0 ? 0d : 1d * query2FetchTimes.getOrDefault ( query, 0l ) / nqueries,
					nresults == 0 ? 0d : 1d * query2PathLen.getOrDefault ( query, 0l ) / nresults
				);
			}
			out.println ( "" );

			log.info ( "\n\n  -------------- Cypher Graph Traverser, Query Stats --------------\n{}", statsSW.toString () );
		}
	}
	
	/**
	 * Has to be initialised by {@link AbstractGraphTraverser#traverseGraph(ONDEXGraph, java.util.Set, FilterPaths)}.
	 */
	protected QueryPerformanceTracker performanceTracker = null;
	
	
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
			Stream<EvidencePathNode> resultStrm = cypherQueries
				.parallelStream ()
				.flatMap ( query -> 
				{
					// DEBUG log.info ( "traversing the gene: \"{}\" with the query: <<{}>>", startIri, query );

					// For each configured semantic motif query, get the paths from Neo4j + indexed resource
					Stream<List<ONDEXEntity>> cypaths = this.findPathsWithPaging ( 
						query, startGeneIri, luceneMgr, cyProvider 
					);
					// DEBUG log.info ( "/end traversal of the gene: \"{}\" with the query: [{}]", startIri, query );
						
					// Now map the paths to the format required by the traverser (see above)
					return cypaths.map ( path -> buildEvidencePath ( path ) );
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
	 * Wraps the default implementation to enable to track query performance, via {@link QueryPerformanceTracker}. 
	 */
	@Override
	@SuppressWarnings ( "rawtypes" )
	public Map<ONDEXConcept, List<EvidencePathNode>> traverseGraph ( 
		ONDEXGraph graph, Set<ONDEXConcept> concepts, FilterPaths<EvidencePathNode> filter )
	{
		if ( this.getOption ( "isPerformanceTrackingEnabled", false ) )
			this.performanceTracker = new QueryPerformanceTracker ();
		
		try {
			return super.traverseGraph ( graph, concepts, filter );
		}
		finally {
			if ( this.performanceTracker != null ) this.performanceTracker.logStats ();
		}
	}
	
	/**
	 * This uses {@link CypherClient#findPaths(LuceneEnv, String, Value)} to get the paths for a gene
	 * that are reachable from the query parameter. Additionally, this method query the Neo4j server 
	 * in a paginated fashion, by fetching {@link #getPageSize()} paths per query.
	 *  
	 */
	protected Stream<List<ONDEXEntity>> findPathsWithPaging ( 
		String query, String startGeneIri, LuceneEnv luceneMgr, CypherClientProvider 	cyProvider
	)
	{
		String pagedQuery = query + "\nSKIP $offset LIMIT $pageSize";

		long pageSize = this.getPageSize ();

		// We need this special iterator to do the paging trick. 
		// Its hasNext() method asks the underlining stream if it has more items. When not, it issues another
		// query with a new offset value and returns false when the latter does.
		//
		Iterator<List<ONDEXEntity>> pathsItr = new Iterator<List<ONDEXEntity>>()
		{
			private long offset = -pageSize;
			private Stream<List<ONDEXEntity>> currentPageStream = null;
			private Iterator<List<ONDEXEntity>> currentPageIterator = null;

			/**
			 * Issue the query with the current offset.
			 */
			private void nextPage ()
			{
				// Close the stream that is going to be disposed
				if ( this.currentPageStream != null ) this.currentPageStream.close ();
					
				offset += pageSize;
				log.trace ( "offset: {} for query: {}", offset, query );
				
				Value params = Values.parameters ( 
					"startIri", startGeneIri,
					"offset", offset,
					"pageSize", pageSize
				); 
				
				Function<String, Stream<List<ONDEXEntity>>> queryAction = q -> cyProvider.queryToStream (
					cyClient -> cyClient.findPaths ( luceneMgr, q, params )
				);
				
				this.currentPageStream = performanceTracker == null 
					? queryAction.apply ( pagedQuery )
					: performanceTracker.track ( queryAction, pagedQuery );
					
				this.currentPageIterator = currentPageStream.iterator ();
				
				// Force the closure of the last empty query
				if ( !this.currentPageIterator.hasNext () ) this.currentPageStream.close ();
			}
			
			/**
			 * Behaves as explained above
			 */
			@Override
			public boolean hasNext ()
			{
				return wrapException ( () -> 
				{
					// do it the first time
					if ( currentPageIterator == null ) this.nextPage ();
					// and whenever the current page is over
					else if ( !currentPageIterator.hasNext () ) nextPage ();
					
					// if false the first time => no result. Else, it becomes false for the first offset that is empty
					return currentPageIterator.hasNext ();
				});
			}

			@Override
			public List<ONDEXEntity> next ()
			{
				// If you call it at the appropriate time, it was prepared by the hasNext() method above
				return wrapException ( () -> currentPageIterator.next () );
			}
			
			/** In case of exception, re-throws Neo4jException with the query that caused it */
			private <T> T wrapException ( Supplier<T> action ) 
			{
				try {
					return action.get ();
				}
				catch ( RuntimeException ex ) 
				{
					throw buildEx ( 
						GenericNeo4jException.class, ex, 
						"Error: %s. While finding paths for <%s>, using the query: %s", 
						ex.getMessage (), startGeneIri, query 
					);
				}
			}
		}; // iterator
		
		// So, the iterator above goes through multiple streams (one per query page), let's turn it back to
		// a stream, as expected by the nethod invoker
		return StreamSupport.stream ( 
			spliteratorUnknownSize ( pathsItr,	IMMUTABLE	| NONNULL	), 
			false 
		);		
	}
	
}
