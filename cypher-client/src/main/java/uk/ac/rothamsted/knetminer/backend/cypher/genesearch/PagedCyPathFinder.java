package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.machinezoo.noexception.Exceptions;

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
	
	private boolean wasClosed = false;
	
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
	 */
	private void nextPage ()
	{
		if ( this.wasClosed ) ExceptionUtils.throwEx ( 
			IllegalArgumentException.class, 
			"The Cypher Path Finder for this query was closed, gene: <%s>, offset: %d, query: [%s]",
			this.startGeneIri,
			this.offset,
			this.query
		);
		
		// Close the stream that is going to be disposed
		if ( this.currentPageStream != null ) this.currentPageStream.close ();
		
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
		);
			
		this.currentPageIterator = currentPageStream.iterator ();
		
		// Force the closure of the last empty query
		if ( !this.currentPageIterator.hasNext () ) this.currentPageStream.close ();
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
		// do it the first time
		if ( currentPageIterator == null ) this.nextPage ();
		// and whenever the current page is over
		else if ( !currentPageIterator.hasNext () ) nextPage ();
		
		// if false the first time => no result. Else, it becomes false for the first offset that is empty
		return currentPageIterator.hasNext ();
	}

	@Override
	public List<ONDEXEntity> next ()
	{
		// If you call it at the appropriate time, it was prepared by the hasNext() method above
		return currentPageIterator.next ();
	}

	@Override
	public void close ()
	{
		if ( this.wasClosed ) return;
		if ( this.currentPageStream != null ) this.currentPageStream.close ();
		this.wasClosed = true;
	}	
}