package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterators.spliteratorUnknownSize;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringEscapeUtils;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClientProvider;
import uk.ac.rothamsted.neo4j.utils.GenericNeo4jException;

/**
 * This uses {@link CypherClient#findPaths(LuceneEnv, String, Value)} to get the paths for a gene
 * that are reachable from the query parameter. Additionally, this method query the Neo4j server 
 * in a paginated fashion, by fetching {@link #getPageSize()} paths per query.
 * 
 * New instances are requested via 
 * {@link #findPathsWithPaging(String, String, LuceneEnv, CypherClientProvider, long, CyTraverserPerformanceTracker)}.
 *
 * TODO: very messy, clean-up.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>10 Jul 2019</dd></dl>
 *
 */
class PagedCyPathFinder implements Iterator<List<ONDEXEntity>>
{
	/**
	 * Used internally to compose the Cypher queries
	 */
	static final String PAGINATION_TRAIL = "\nSKIP $offset LIMIT $pageSize";

	private final String startGeneIri;
	private final String query;
	private final long pageSize;
	private final CypherClientProvider cyProvider;
	private final LuceneEnv luceneMgr;
	private final CyTraverserPerformanceTracker performanceTracker;
	private final long queryTimeoutMs;

	private static final TimeLimiter TIME_LIMITER = new SimpleTimeLimiter ();
	
	private long offset;
	private Stream<List<ONDEXEntity>> currentPageStream = null;
	private Iterator<List<ONDEXEntity>> currentPageIterator = null;
	
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	private PagedCyPathFinder ( 
		String startGeneIri, String query, long pageSize, CypherClientProvider cyProvider,
		LuceneEnv luceneMgr, CyTraverserPerformanceTracker performanceTracker,
		long queryTimeoutMs
	)
	{
		this.startGeneIri = startGeneIri;
		this.query = query;
		this.pageSize = pageSize;
		this.cyProvider = cyProvider;
		this.luceneMgr = luceneMgr;
		this.performanceTracker = performanceTracker;
		this.queryTimeoutMs = queryTimeoutMs;
		this.offset = -pageSize;
	}

	/**
	 * Issues the query with the current offset.
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
			
		String pagedQuery = query + PAGINATION_TRAIL;

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
		return wrappedExec 
		(  
			() -> {
				// do it the first time
				if ( currentPageIterator == null ) this.nextPage ();
				// and whenever the current page is over
				else if ( !currentPageIterator.hasNext () ) nextPage ();
				
				// if false the first time => no result. Else, it becomes false for the first offset that is empty
				return currentPageIterator.hasNext ();
			},
			false
		);
	}

	@Override
	public List<ONDEXEntity> next ()
	{
		// If you call it at the appropriate time, it was prepared by the hasNext() method above
		return wrappedExec ( () -> currentPageIterator.next (), Collections.emptyList () );
	}

	
	/**
	 * Decorates an iterator operation with common controls, namely: executes with time constraints and 
	 * throws a {@link GenericNeo4jException} if something bad happens.
	 * 
	 * In case of timeout, returns {@code timeoutReturnValue} 
	 */
	private <T> T wrappedExec ( Callable<T> queryAction, T timeoutReturnValue )
	{
		try
		{
			// No timeout wanted
			if ( queryTimeoutMs == -1 ) return queryAction.call ();
			
			return TIME_LIMITER.callWithTimeout ( 
				queryAction, queryTimeoutMs, TimeUnit.MILLISECONDS, true 
			);
		}
		catch ( UncheckedTimeoutException ex ) 
		{
			if ( this.performanceTracker != null )
				// TODO: log once when the tracker is disabled
				this.performanceTracker.trackTimeout ( query );
			return timeoutReturnValue;
		}
		catch ( Exception ex )
		{
			throw ExceptionUtils.buildEx ( 
				GenericNeo4jException.class, 
				"Error while traversing Neo4j gene graph: %s. Gene IRI is: <%s>. Query is: \"%s\"",
				ex.getMessage (),
				startGeneIri,
				StringEscapeUtils.escapeJava ( query )
			);
		}			
	}
	
	/**
	 * Issues a new {@link PagedCyPathFinder} for the query and then builds a stream based on it. 
	 */
	static Stream<List<ONDEXEntity>> findPathsWithPaging ( 
		String startGeneIri, String query, long pageSize, LuceneEnv luceneMgr, CypherClientProvider cyProvider,
		CyTraverserPerformanceTracker performanceTracker, long queryTimeoutMs
	)
	{
		// We need this special iterator to do the paging trick. 
		// Its hasNext() method asks the underlining stream if it has more items. When not, it issues another
		// query with a new offset value and returns false when the latter does.
		//
		Iterator<List<ONDEXEntity>> pathsItr = new PagedCyPathFinder ( 
			startGeneIri, query, pageSize, cyProvider, luceneMgr, performanceTracker, queryTimeoutMs
		);
		
		// So, the iterator above goes through multiple streams (one per query page), let's turn it back to
		// a stream, as expected by the nethod invoker
		return StreamSupport.stream ( 
			spliteratorUnknownSize ( pathsItr,	IMMUTABLE	| NONNULL	), 
			false 
		);		
	}
}