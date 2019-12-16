package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.helpers;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

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
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import net.sourceforge.ondex.algorithm.graphquery.FilterPaths;
import net.sourceforge.ondex.core.ONDEXGraph;
import uk.ac.ebi.utils.time.XStopWatch;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.CypherGraphTraverser;

/**
 * <p>An helper for {@link CypherGraphTraverser}, which tracks the performance of multiple queries, during the execution 
 * of the {@link CypherGraphTraverser#traverseGraph(ONDEXGraph, Set, FilterPaths) multi-gene traversal}.</p>
 * 
 * <p>Note that, because queries are run against batches of starting genes, as per {@link SinglePathQueryProcessor}, many
 * of the figures reported by this component refer to the performance per single batch, not per single gene. 
 * TODO: add the batch size as parameter and change the reporting.</p>
 */
@Component
public class CyTraverserPerformanceTracker 
{
	public static final String CFGOPT_TRAVERSER_PERFORMANCE = "knetminer.backend.traverserPerformanceTracking.enabled";

	/**
	 * <p>Based on this option given to the {@link CypherGraphTraverser}, reports partial results via {@link #logStats()} 
	 * every a number of {@link #track(String, Runnable, Supplier, Supplier)} invocations equal to this value.</p>
	 * 
	 * <p>Consider that every invocation corresponds to one gene and one query, 
	 * so this value should reflect a fraction of genes*queries.</p>
	 * 
	 * <p>if -1, doesn't do any periodic report</p>
	 * 
	 */
	public static final String CFGOPT_PERFORMANCE_REPORT_FREQ = "knetminer.backend.traverserPerformanceTracking.reportFrequency";

	
	/** Times to fetch all the results **/
	private Map<String, Long> query2ExecTimes = Collections.synchronizedMap ( new HashMap<> () );

	/** No of results (pahts) returned by each query **/
	private Map<String, Integer> query2Results = Collections.synchronizedMap ( new HashMap<> () );

	/** No of query invocations ({@code #invocations = #queries x #genes}) **/
	private Map<String, Integer> query2Invocations = Collections.synchronizedMap ( new HashMap<> () );
	
	/** No. of queries that timed out */
	private Map<String, Integer> query2Timeouts = Collections.synchronizedMap ( new HashMap<> () );
	
	/** Sums of returned path lengths for the each query **/
	private Map<String, Long> query2PathLens = Collections.synchronizedMap ( new HashMap<> () );
	
	/** Total no. of invocations, ie #queries x #genes / {@link #queryBatchSize} **/ 
	private AtomicInteger invocations = new AtomicInteger ( 0 );
	
	
	@Autowired(required = false) @Qualifier ( "performanceReportFrequency" )
	private int reportFrequency = 0;

	
	@Resource( name = "semanticMotifsQueries" )
	private List<String> semanticMotifsQueries; 

	@Autowired ( required = false) @Qualifier ( "queryBatchSize" ) 
	private long queryBatchSize = SinglePathQueryProcessor.DEFAULT_QUERY_BATCH_SIZE;
	
	
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
			
	CyTraverserPerformanceTracker () {
	}

	
	@PostConstruct
	public void reset ()
	{
		if ( this.reportFrequency < 0 ) return; // tracking is disabled
		invocations.getAndSet ( 0 );
		this.semanticMotifsQueries.forEach ( q -> {
			this.query2ExecTimes.put ( q, 0l );
			this.query2Invocations.put ( q, 0 );
			this.query2Timeouts.put ( q, 0 );
			this.query2PathLens.put ( q, 0l );
			this.query2Results.put ( q, 0 );
		});
	}
	
	/**
	 * <p>Does the tracking. This runs the {@code queryAction} and registers the time it took to run.</p>
	 * 
	 * <p>Then, it also tracks the paths returned by this query and the sum of their lengths. As you can see, the invoker 
	 * has to provide code to compute such counts.</p>
	 * 
	 * <p>Moreover, if {@code queryAction} throws {@link UncheckedTimeoutException}, time and path counters aren't updated,
	 * the timeout counter is update instead. This is useful when the query action makes use of {@link TimeLimiter}</p>
	 * 
	 */
	void track ( String query, Runnable queryAction, Supplier<Integer> pathsCounter, Supplier<Integer> pathLensCounter )
	{
		if ( this.reportFrequency < 0 ) {
			// tracking is disabled
			queryAction.run ();
		}

		try {
			long time = XStopWatch.profile ( queryAction );
			this.query2ExecTimes.compute ( query, (k, t) -> t + time );
			this.query2Results.compute ( query, (k, n) -> n + pathsCounter.get () );
			this.query2PathLens.compute ( query, (k, l) -> l + pathLensCounter.get () );
		}
		catch ( UncheckedTimeoutException ex ) {
			// Track the query timed out, the other updates above are skipped by the exec flow.
			this.query2Timeouts.compute ( query, (q, n) -> n + 1 );
			// Don't wrap it with other exception types, but let it flow to the invoker, which 
			// needs to know it, in order to perform cancelling operations
			throw ex;
		}
		finally
		{
			this.query2Invocations.compute ( query, (k, n) -> n + 1 );
			this.invocations.incrementAndGet ();
			if ( this.reportFrequency > 0 && invocations.get () % this.reportFrequency == 0 ) logStats ();
		}
	}

	
	void logStats ()
	{
		String stats = getStats ();
		if ( stats == null ) return;
		
		log.info ( "\n\n  -------------- Cypher Graph Traverser, Query Stats --------------\n{}", stats );
	}

	
	/**
	 * Reports the stats accumulated so far using the underlining logging system.
	 */
	public String getStats ()
	{
		if ( this.reportFrequency < 0 )
		{
			log.debug ( 
				"{}.logStats(): performance tracking is disabled, ignoring this invocation",
				this.getClass ().getSimpleName ()
			);
			return null;
		}

		StringWriter statsSW = new StringWriter ();
		PrintWriter out = new PrintWriter ( statsSW );
		
		final int nTotQueries = invocations.get ();
		out.printf ( "\n\nTotal queries issued: %s\n", nTotQueries );
		if ( nTotQueries == 0 ) return statsSW.toString ();
		
		out.println (   
			"Query\tTot Invocations\t% Timeouts\tTot Returned Paths\tAvg Ret Paths x Gene\tAvg Time(ms)\tAvg Path Len" 
		);
		
		SortedSet<String> queries = new TreeSet<> ( query2Invocations.keySet () );
		for ( String query: queries )
		{
			int nresults = query2Results.get ( query );
			int nqueries = query2Invocations.get ( query );
			int ntimeouts = query2Timeouts.get ( query );
			int ncompleted = nqueries - ntimeouts;
							
			out.printf (
				"\"%s\"\t%d\t%#6.2f\t%d\t%#6.2f\t%#6.2f\t%#6.2f\n",
				escapeJava ( query ),
				nqueries,
				nqueries == 0 ? 0d : 100d * ntimeouts  / nqueries,
				nresults,
				ncompleted == 0 ? 0d : 1d * nresults / ( ncompleted * this.queryBatchSize ),
				ncompleted == 0 ? 0d : 1d * query2ExecTimes.get ( query ) / ncompleted,
				nresults == 0 ? 0d : 1d * query2PathLens.get ( query ) / nresults
			);
		}
		out.println ( "" );
		return statsSW.toString ();
	}


	public int getReportFrequency () {
		return reportFrequency;
	}


	public void setReportFrequency ( int reportFrequency ) {
		this.reportFrequency = reportFrequency;
	}	
}