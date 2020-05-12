package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.helpers;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.common.util.concurrent.Uninterruptibles;

import net.sourceforge.ondex.algorithm.graphquery.FilterPaths;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import uk.ac.ebi.utils.time.XStopWatch;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.CypherGraphTraverser;

/**
 * <p>An helper for {@link CypherGraphTraverser}, which tracks the performance of multiple queries, during the execution 
 * of the {@link CypherGraphTraverser#traverseGraph(ONDEXGraph, Set, FilterPaths) multi-gene traversal}.</p>
 * 
 * <p>Note that, because queries are run against batches of starting genes, as per {@link SinglePathQueryProcessor}, many
 * of the figures reported by this component refer to the performance per single batch, not per single gene.</p> 
 */
@Component
public class CyTraverserPerformanceTracker 
{
	/** This is a configurable parameter */
	@Autowired(required = false) @Qualifier ( "performanceReportFrequency" )
	private int reportFrequency = 0;
	
	/** This is a configurable parameter */
	@Autowired ( required = false) @Qualifier ( "queryBatchSize" ) 
	private long queryBatchSize = SinglePathQueryProcessor.DEFAULT_QUERY_BATCH_SIZE;

	/** This is a configurable parameter */
	@Resource( name = "semanticMotifsQueries" )
	private List<String> semanticMotifsQueries; 
	
	/** TODO: comment me*/
	@Autowired(required = false) @Qualifier ( "timeoutReportFilesPathPrefix" )
	private String timeoutReportFilesPathPrefix = 
		Optional.ofNullable ( System.getenv ( "CATALINA_HOME" ) )
		.map ( base -> base += "/logs/knetminer-cy-timeout-report-%s.txt.gz" )
		.orElse ( null );
	
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

	/** Used by {@link #logTimedOutQuery(String, List)}  */
	private AtomicLong currentTime = new AtomicLong ( 0 );
	
	
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
			
	CyTraverserPerformanceTracker () {
	}

	
	@PostConstruct
	public void reset ()
	{
		if ( this.reportFrequency < 0 ) return; // tracking is disabled
		invocations.getAndSet ( 0 );
		
		this.query2ExecTimes.clear ();
		this.query2Invocations.clear ();
		this.query2Timeouts.clear ();
		this.query2PathLens.clear ();
		this.query2Results.clear ();
		
		this.semanticMotifsQueries.forEach ( q ->
		{
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
	void track ( 
		String query, Runnable queryAction, 
		Supplier<Integer> pathsCounter, Supplier<Integer> pathLensCounter, 
		List<ONDEXConcept> startGenes 
	)
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
			this.logTimedOutQuery ( query, startGenes );
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
	
	
	private void logTimedOutQuery ( String query, List<ONDEXConcept> startGenes )
	{
		if ( this.timeoutReportFilesPathPrefix == null ) return;
		
		// To have different file names below
		Uninterruptibles.sleepUninterruptibly ( 1, TimeUnit.MILLISECONDS );
		
		long ts = this.currentTime.updateAndGet ( prev -> System.currentTimeMillis () );
		String tsStr = DateFormatUtils.format ( ts, "yyyyMMddHHmmss.SSS" );
		String outPath = String.format ( this.timeoutReportFilesPathPrefix, tsStr );
		
		try ( PrintStream out = new PrintStream ( new GZIPOutputStream ( new FileOutputStream ( outPath ), 2<<20 ) ) )
		{
			out.println ( StringEscapeUtils.escapeJava ( query ) );
			startGenes
			.stream ()
			.map ( ONDEXConcept::getPID )
			.forEach ( out::println );
		}
		catch ( IOException ex ) {
			throw new UncheckedIOException ( "Error while saving timed out query: " + ex.getMessage (), ex );
		}
	}
	

	/**
	 * Sends {@link #getStats()} to the logging system.
	 */
	public void logStats ()
	{
		String stats = getStats ();
		if ( stats == null ) return;
		
		log.info ( "\n\n  -------------- Cypher Graph Traverser, Query Stats --------------\n{}", stats );
	}

	
	/**
	 * Reports the stats accumulated so far.
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
		out.printf ( "Total queries issued: %s\n", nTotQueries );
		if ( nTotQueries == 0 ) return statsSW.toString ();
		
		out.println (   
			"Query\tTot Invocations\t% Timeouts\tTot Returned Paths\tAvg Ret Paths x Gene\tAvg Time(ms)\tAvg Path Len\tTot Time(min)" 
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
				ncompleted == 0 ? 0d : 1d * nresults / ( ncompleted * this.queryBatchSize ),
				ncompleted == 0 ? 0d : 1d * query2ExecTimes.get ( query ) / ncompleted,
				nresults == 0 ? 0d : 1d * query2PathLens.get ( query ) / nresults,
				ncompleted == 0 ? 0 : query2ExecTimes.get ( query ) / ( 1000d * 60 )
			);
		}
		out.println ( "" );
		return statsSW.toString ();
	}


	public int getReportFrequency () {
		return reportFrequency;
	}

	/**
	 * We need to set this programmatically, not just via Spring
	 */
	public void setReportFrequency ( int reportFrequency ) {
		this.reportFrequency = reportFrequency;
	}

	/**
	 * We need to set this programmatically, not just via Spring.
	 * This invokes {@link #reset()}, so every stats is lost.
	 */
	public void setSemanticMotifsQueries ( List<String> semanticMotifsQueries )
	{
		this.semanticMotifsQueries = semanticMotifsQueries;
		this.reset ();
	}	
}