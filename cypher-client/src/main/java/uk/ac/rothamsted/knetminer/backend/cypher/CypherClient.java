package uk.ac.rothamsted.knetminer.backend.cypher;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Entity;
import org.neo4j.driver.v1.types.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;

/**
 * A facade for Cypher/Neo4j clients, allowing  for simplified transaction management and specific Knetminer functions.
 * A client instance is associated to a Neo4j {@link Session}, this is either passed to the constructor, or set  
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Oct 2018</dd></dl>
 *
 */
public class CypherClient implements AutoCloseable
{
	private Session neoSession;
	private Transaction tx;
	
  // Allows for some sanity check and diagnostics.
	private AtomicInteger openTxsCount = new AtomicInteger ( 0 );
	
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	/**
	 * Visibility is protected cause you're not supposed to instantiate me directly, you should use the
	 * {@link CypherClientProvider provider} instead. 
	 */
	protected CypherClient ( Session neoSession )
	{
		super ();
		this.neoSession = neoSession;
	}

	/**
	 * <p>Runs a Cypher {@code query} using this client and gets Ondex entities corresponding to the URIs returned 
	 * by the query. More precisely, it is assumed that the underlining graph database is aligned to the memory Ondex graph
	 * which has been indexed into {@code luceneManager} (ie, the graph database was created via the neo4j-export).</p>
	 *  
	 * <p>It is also assumed that the query returns a list of nodes/relations, from which the {@code iri} property is 
	 * extracted.</p>
	 * 
	 * <p>if params is non-null, it is used to instantiate the query, via {@link #queryToStream(String, Value)}.</p>
	 * 
	 * <p>This is based on {@link #queryToStream(String, Value)}, see the note there about parallelism.</p>
	 * 
	 */
	public Stream<List<ONDEXEntity>> findPaths ( LuceneEnv luceneMgr, String query, Value params )
	{
		Stream<List<String>> rawResults = findPathIris ( query, params );
		
		return rawResults.map ( 
			iris -> { 
				int[] pathIdx = new int [] { -1 };
				return iris.stream ()
					.map ( iri ->
					{ 
						ONDEXEntity oe = ++pathIdx[ 0 ] % 2 == 0 
						  ? luceneMgr.getConceptByIRI ( iri ) 
						  : luceneMgr.getRelationByIRI ( iri );
						if ( oe == null ) ExceptionUtils.throwEx (
							IllegalStateException.class, 
							"Cannot find any Ondex %s for URI '%s'. Cypher Query is: %s. Index fetched from it is %d.",
							pathIdx[ 0 ] % 2 == 0 ? "concept" : "relation", iri, query, pathIdx[ 0 ]
						);
						return oe;
					}).collect ( Collectors.toList () );
			});
	}

	/**
	 * Wrapper without query parameters.
	 */
	public Stream<List<ONDEXEntity>> findPaths ( LuceneEnv luceneMgr, String query ) {
		return findPaths ( luceneMgr, query, null );
	}

	/**
	 * <p>Low-level Neo4j querying.</p>
	 * 
	 * <p>Uses the Neo4j client to issue the query, wrapping it into facilities like transaction auto-opening, and then
	 * wraps the resulting records into a stream, in a dynamic/lazy way, that is, a {@link StatementResult} is 
	 * iterated only when the corresponding {@link Stream} methods are invoked.</p>
	 * 
	 * <p>Note that this method returns an immutable parallel stream, so you might need {@link Stream#sequential()}
	 * in your code (eg, in {@link Stream#forEach(java.util.function.Consumer)}, to ensure thread safety).</p>
	 * 
	 */
	protected Stream<Record> queryToStream ( String query, Value params )
	{
		this.checkOpen ();
		StatementResult cursor = params == null 
			? this.tx.run ( query )
			: this.tx.run ( query, params )				
		;
				
		Spliterator<Record> splitr = spliteratorUnknownSize ( cursor, Spliterator.IMMUTABLE );
		return StreamSupport.stream ( splitr, false );		
	}
	
  
	/**
	 * <p>Runs the query via {@link #queryToStream(String, Value)} and, for each Cypher node/relation returned, 
	 * extracts the {@code iri} property and eventually returns a stream of iri lists, one list per path.
	 * This assumes the query returns a path ({@code MATCH p = .... RETURN p}) as first projection. The IRIs are used
	 * by methods like {@link #findPaths(LuceneEnv, String, Value)}, to convert IRIs to Ondex entities, which assumes 
	 * the Neo4j database corresponds to the in-memory Ondex graph (i.e., was created using the neo4j export tool, using
	 * the in-memory OXL).</p>
	 * 
	 * <p>This is based on {@link #queryToStream(String, Value)}, see the note there about parallelism.</p>
	 */
  public Stream<List<String>> findPathIris ( String query, Value params )
  {
  	Stream<Record> qresult = queryToStream ( query, params );
  	
  	// Each record from the query result must return paths of Ondex nodes/relations 
  	// (MATCH p = (...) ... RETURN p). Every node/relation must have the iri property.
  	// 
  	return qresult.map ( rec -> 
  	{
  		List<String> ids = new ArrayList<> (); 
  		Path path = rec.get ( 0 ).asPath ();
  		// Each segment is like (n1)-[r]-(n2), whatever the direction of r
  		// So, we collect n1 + r at every iteration, then we pickup n2 from the last segment
  		final String[] lastId = new String [] { null };
  		Function<Entity, String> iriMapper = e -> e.get ( "iri" ).asString ();  		
  		path.forEach ( seg -> { 
  			ids.add ( iriMapper.apply ( seg.start () ) );
  			ids.add ( iriMapper.apply ( seg.relationship () ) );
  			lastId [ 0 ] = iriMapper.apply ( seg.end () );
  		});
  		if ( lastId [ 0 ] != null ) ids.add ( lastId [ 0 ] );
  		return ids;
  	});
  }	
	
  /**
   * Wrapper without Cypher parameters.
   */
  public Stream<List<String>> findPathIris ( String query ) {
  	return findPathIris ( query, null );
  }
  
  /**
   * Begins a new transaction in the session this client is based upon, using the 
   * {@link Session#beginTransaction() corresponding Neo4j method}.
   * 
   * Multiple transactions can be opened during a given session, 
	 * but sequentially.
   */
	public synchronized void begin () 
	{
		tx = neoSession.beginTransaction ();

		// There are more opened transactions than the expected degree of parallelism.
		// This shouldn't happen, this limits are expected when using of ForkJoinPool. 
		if ( openTxsCount.incrementAndGet () > 256 || openTxsCount.get () > Runtime.getRuntime ().availableProcessors () )
			log.error ( 
				"====> It seems there are too many Cypher transactions opened ({}), could be a bug", openTxsCount.get () 
			);
	}

	/**
	 * Ends and possibly commits a transaction, using {@link Transaction#close() Neo4j method}.
	 * The commit will happen unless {@link #fail()} was invoked.
	 */
	public synchronized void end ()
	{
		tx.close ();
		tx = null;
		openTxsCount.decrementAndGet ();
	}
	
	/**
	 * Fails/rollback the current transaction. This doesn't {@link #end()} the transaction, call that method 
	 * for that.
	 */
	public synchronized void fail ()
	{
		tx.failure ();
	}
		
	/**
	 * Tells if the session for this client is open. Sessions are opened upon client creation and 
	 * closed via {@link #close()}. Once a session is closed, it cannot be re-opened.
	 * 
	 */
	public synchronized boolean isOpen () {
		return this.neoSession.isOpen ();
	}
	
	/**
	 * Tells if there is an ongoing transaction.
	 */
	public synchronized boolean isTxOpen () {
		return this.tx != null;
	}
	
	/**
	 * Uses {@link #begin()} to open a transaction, if not already done.
	 * 
	 * @throws IllegalStateException, if !{@link #isOpen()}
	 */
	public void checkOpen () 
	{
		if ( !this.isOpen () ) throw new IllegalStateException (
				this.getClass ().getSimpleName () + " is closed and requires a new instance to perform further operations"
			);
			if ( !this.isTxOpen () ) this.begin ();
	}
	
	/**
	 * Runs an action that is supposed to deal with this client/Neo4j under a transaction demarked by 
	 * {@link #begin()} and {@link #end()}.
	 *  
	 */
	public <T> T runTx ( Supplier<T> action )
	{
		this.checkOpen ();
		try {
			return action.get ();
		}
		finally {
			this.end ();
		}
	}  

	/**
	 * Wrapper without parameters.
	 */
	public Void runTx ( Runnable action ) {
		return runTx ( () -> { action.run (); return null; } );
	}
	
	/**
	 * Closes the underlining Neo4j {@link Session}.
	 * After this, {@link #isOpen()} will always be false and the client cannot be used anymore.
	 */
	@Override
	public synchronized void close ()
	{
		if ( this.isTxOpen () ) this.end ();
		if ( this.neoSession.isOpen () ) this.neoSession.close ();
		neoSession = null;
	}	
}
