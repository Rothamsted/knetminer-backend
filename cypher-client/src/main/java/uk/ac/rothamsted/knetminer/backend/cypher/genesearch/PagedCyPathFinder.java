package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClientProvider;

/**
 * <p>Support component for {@link CypherGraphTraverser} that manages Cypher queries against a Knetminer Neo4j database.</p>
 * 
 * <p>This uses {@link CypherClient#findPaths(LuceneEnv, String, Value)} to get the paths for a gene
 * that are reachable from the query parameter. Additionally, this method queries the Neo4j server 
 * in a paginated fashion, by fetching {@link #getPageSize()} paths per query.</p>
 * 
 * <p>Because this is used to process a single query sequentially, this method isn't thread-safe</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>10 Jul 2019</dd></dl>
 *
 */
class PagedCyPathFinder implements Iterator<List<ONDEXEntity>>, AutoCloseable
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
	
	private long offset;
	private Stream<List<ONDEXEntity>> currentPageStream = null;
	private Iterator<List<ONDEXEntity>> currentPageIterator = null;
	
	private boolean isClosed = false, isFinished = false;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	PagedCyPathFinder ( 
		String startGeneIri, String query, long pageSize, CypherClientProvider cyProvider,
		LuceneEnv luceneMgr	
	)
	{
		this.startGeneIri = startGeneIri;
		this.query = query;
		this.pageSize = pageSize;
		this.cyProvider = cyProvider;
		this.luceneMgr = luceneMgr;
		this.offset = -pageSize;
	}

	/**
	 * Issues the query with the current offset. @see {@link #hasNext()}.
	 * @return true if it found a non-empty page. Else, invokes {@link #closePage()}, sets {@link #isFinished} and 
	 * returns false.
	 */
	private boolean nextPage ()
	{
		if ( this.isFinished ) return false; // you're calling me after both hasNext() and the last page said we're over.
		
		// Close the current exhausted stream, which is going to be disposed (if non-null)
		this.closePage ();
		
		offset += pageSize;
		log.trace ( "offset: {} for query: {}", offset, query );
		
		Value params = Values.parameters ( 
			"startIri", startGeneIri,
			"offset", offset,
			"pageSize", pageSize
		); 
					
		String pagedQuery = query + PAGINATION_TRAIL;

		this.currentPageStream = cyProvider.queryToStream (
			cyClient -> cyClient.findPaths ( luceneMgr, pagedQuery, params )
		)
		.sequential ();
		this.currentPageIterator = currentPageStream.iterator ();
		
		if ( this.currentPageIterator.hasNext () ) return true;
		
		// else, no more pages, let's close and mark it's all over
		this.closePage ();
		this.isFinished = true;
		return false;
	}

	
	/**
	 * How it works: We need this special iterator to do the paging trick. 
	 * Its hasNext() method asks the underlining stream if it has more items. When not, it issues another
	 * query with a new offset value and returns false when the latter does.
	 * 
	 */
	@Override
	public boolean hasNext ()
	{
		if ( this.isClosed ) throwEx ( 
			IllegalStateException.class, "The Cypher Path Finder for this query was closed"
		);
		
		// You're still calling me after the last empty page 
		if ( this.isFinished ) return false;
		
		// get a first iterator if it's the first time we're called
		if ( this.currentPageIterator == null ) return this.nextPage ();
		// or check the current iterator if it was already created by previous calls, possibly advance
		// one page if the iterator is exhausted
		return this.currentPageIterator.hasNext () || nextPage ();
	}

	@Override
	public List<ONDEXEntity> next ()
	{		
		if ( !this.hasNext () ) throwEx ( 
			NoSuchElementException.class, "Cypher Path Finder has no more items (hasNext() == false)"
		);
			
		// If you call it at the appropriate time, it was prepared by the hasNext() method above
		return currentPageIterator.next ();
	}

	
	@Override
	public void close ()
	{
		if ( this.isClosed ) return;
		this.closePage ();
		this.isClosed = this.isFinished = true;
	}
	
	private void closePage ()
	{
		if ( this.currentPageIterator == null ) return;
		this.currentPageStream.close ();
		this.currentPageIterator = null;
		this.currentPageStream = null;
	}
	
	
	/**
	 * A template to report exceptions, adds query and gene to the prefixMsg.
	 */
	private void throwEx ( Class<? extends RuntimeException> ex, String prefixMsg )
	{
		ExceptionUtils.throwEx ( 
			ex, 
			"%s, gene: <%s>, offset: %d, query: [%s]",
			prefixMsg,
			this.startGeneIri,
			this.offset,
			this.query
		);		
	}

}