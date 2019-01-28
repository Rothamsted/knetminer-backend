package uk.ac.rothamsted.knetminer.backend.cypher;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
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

import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Oct 2018</dd></dl>
 *
 */
public class CypherClient implements AutoCloseable
{
	private Session neoSession;
	private Transaction tx;
	
	protected CypherClient ( Session neoSession )
	{
		super ();
		this.neoSession = neoSession;
	}

	public Stream<List<ONDEXEntity>> findPaths ( LuceneEnv luceneMgr, String query, Value params )
	{
		Stream<List<String>> rawResults = findPathIris ( query, params );
		
		return rawResults.map ( 
			iris -> { 
				int[] pathIdx = new int [] { -1 };
				return iris.stream ()
					.map ( iri -> { 
						ONDEXEntity oe = ++pathIdx[ 0 ] % 2 == 0 
						  ? luceneMgr.getConceptByIRI ( iri ) 
						  : luceneMgr.getRelationByIRI ( iri );
						if ( oe == null ) ExceptionUtils.throwEx (
							IllegalStateException.class, "Cannot find any Ondex concept/relation for URI '%s'", iri 
						);
						return oe;
					}).collect ( Collectors.toList () );
			});
	}

	public Stream<List<ONDEXEntity>> findPaths ( LuceneEnv luceneMgr, String query ) {
		return findPaths ( luceneMgr, query, null );
	}

	
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
	
  public Stream<List<String>> findPathIris ( String query ) {
  	return findPathIris ( query, null );
  }

  /**
   * Begins a new transaction.
   * 
   * TODO: maybe session demarcation doesn't need to be public.
   */
	public synchronized void begin () {
		tx = neoSession.beginTransaction ();
	}

	/**
	 * Ends/commits a transaction.
	 */
	public synchronized void end ()
	{
		tx.close ();
		tx = null;
	}
		
	public synchronized boolean isOpen () {
		return this.neoSession.isOpen ();
	}
	
	public synchronized boolean isTxOpen () {
		return this.tx != null;
	}
	
	/**
	 * Throws an error if no transaction was opened with {@link #begin()}.
	 */
	public void checkOpen () 
	{
		if ( !this.isOpen () ) throw new IllegalStateException (
				this.getClass ().getSimpleName () + " is closed and requires a new instance to perform further operations"
			);
			if ( !this.isTxOpen () ) this.begin ();
	}
	
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

	public Void runTx ( Runnable action ) {
		return runTx ( () -> { action.run (); return null; } );
	}
	
	@Override
	public synchronized void close ()
	{
		if ( this.isTxOpen () ) this.end ();
		this.neoSession.close ();
		neoSession = null;
	}	
}
