package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.ondex.algorithm.graphquery.FilterPaths;
import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.ONDEXGraph;
import uk.ac.ebi.utils.time.XStopWatch;

/**
 * An helper for {@link CypherGraphTraverser}, which tracks the performance of multiple queries, during the execution 
 * of the {@link CypherGraphTraverser#traverseGraph(ONDEXGraph, Set, FilterPaths) multi-gene traversal}.
 */
class CyTraverserPerformanceTracker 
{
	/** Times to send the query and get a first reply **/
	private Map<String, Long> query2StartTimes = Collections.synchronizedMap ( new HashMap<> () );
	
	/** Times to fetch all the results **/
	private Map<String, Long> query2FetchTimes = Collections.synchronizedMap ( new HashMap<> () );

	/** No of results (pahts) returned by each query **/
	private Map<String, Integer> query2Results = Collections.synchronizedMap ( new HashMap<> () );

	/** No of query invocations ({@code #invocations = #queries x #genes}) **/
	private Map<String, Integer> query2Invocations = Collections.synchronizedMap ( new HashMap<> () );
	
	/** No. of queries that timed out */
	private Map<String, Integer> query2Timeouts = Collections.synchronizedMap ( new HashMap<> () );
	
	/** Sums of returned path lengths for the each query **/
	private Map<String, Long> query2PathLen = Collections.synchronizedMap ( new HashMap<> () );
	
	
	/** Total no. of invocations, ie #queries x #genes **/ 
	private AtomicInteger invocations = new AtomicInteger ( 0 );
	
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
			
	public CyTraverserPerformanceTracker () 
	{
		CypherGraphTraverser.getCypherQueries().forEach ( q -> {
			this.query2StartTimes.put ( q, 0l );
			this.query2FetchTimes.put ( q, 0l );
			this.query2Invocations.put ( q, 0 );
			this.query2Timeouts.put ( q, 0 );
			this.query2PathLen.put ( q, 0l );
			this.query2Results.put ( q, 0 );
		});
	}

	public Stream<List<ONDEXEntity>> track (
		Function<String, Stream<List<ONDEXEntity>>> queryAction,
		String query
	)
	{
		// We keep them without the pagination tail
		final String queryNrm = StringUtils.removeEnd ( query, PagedCyPathFinder.PAGINATION_TRAIL );
		
		@SuppressWarnings ( "unchecked" )
		Stream<List<ONDEXEntity>> result[] = new Stream [ 1 ];
		
		long startTime = XStopWatch.profile ( () -> { result [ 0 ] = queryAction.apply ( query ); } );
		
		
		query2StartTimes.compute ( queryNrm, (q,t) -> t + startTime );
		
		XStopWatch fetchTimer = new XStopWatch ();
		
		result [ 0 ] = result [ 0 ]
		.peek ( pathEls -> 
		{
			// stats timing the first time/path it's invoked, then onClose() will get the total time elapsed
			// after all the stream has been consumed
			fetchTimer.resumeOrStart (); 
			query2Results.compute ( queryNrm, (q,nr) -> nr + 1 );
			query2PathLen.compute ( queryNrm, (q,pl) -> pl + pathEls.size () );
		})
		.onClose ( () -> {
			// Track what is presumably the fetch time 
		  // (we come here after all the paths in the stream have been consumed) 
			query2FetchTimes.compute (
				queryNrm, 
				(q,t) -> ( !fetchTimer.isStarted () ? 0 : fetchTimer.getTime () ) + t
			);
			// TODO: DEBUG, requires better engineering
			if ( invocations.get () % 100000 == 0 ) logStats (); 
		});
		
		query2Invocations.compute ( queryNrm, (q,n) -> n + 1 );
		invocations.incrementAndGet ();
					
		return result [ 0 ];
	}
	
	/** 
	 * Tracks the fact a query has timed out. We have invocations from the main class, so we define this method.
	 * @return the no. of currently timed out queries. 
	 */
	public int trackTimeout ( String query ) {
		return this.query2Timeouts.compute ( query, (q, ct) -> ct + 1 );
	}
	
	/**
	 * Reports the stats accumulated so far using the underlining logging system.
	 */
	public void logStats ()
	{
		StringWriter statsSW = new StringWriter ();
		PrintWriter out = new PrintWriter ( statsSW );
		
		final int nTotQueries = invocations.get ();
		out.printf ( "\n\nTotal queries issued: %s\n", nTotQueries );
		if ( nTotQueries == 0 ) return;
		
		out.println (   
			"Query\tTot Invocations\t% Timeouts\tTot Returned Paths\tAvg Ret Paths\tAvg Start Time(ms)\tAvg Fetch Time(ms)\tAvg Path Len" 
		);
		
		SortedSet<String> queries = new TreeSet<> ( query2Invocations.keySet () );
		for ( String query: queries )
		{
			int nresults = query2Results.get ( query );
			int nqueries = query2Invocations.get ( query );
			int ntimeouts = query2Timeouts.get ( query );
			int ncompleted = nqueries - ntimeouts;
							
			out.printf (
				"\"%s\"\t%d\t%#6.2f\t%d\t%#6.2f\t%#6.2f\t%#6.2f\t%#6.2f\n",
				escapeJava ( query ),
				nqueries,
				nqueries == 0 ? 0d : 100d * ntimeouts  / nqueries,
				nresults,
				ncompleted == 0 ? 0d : 1d * nresults / ncompleted,
				ncompleted == 0 ? 0d : 1d * query2StartTimes.get ( query ) / ncompleted,
				ncompleted == 0 ? 0d : 1d * query2FetchTimes.get ( query ) / ncompleted,
				nresults == 0 ? 0d : 1d * query2PathLen.get ( query ) / nresults
			);
		}
		out.println ( "" );

		log.info ( "\n\n  -------------- Cypher Graph Traverser, Query Stats --------------\n{}", statsSW.toString () );
	}
}