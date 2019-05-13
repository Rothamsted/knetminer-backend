package uk.ac.rothamsted.knetminer.backend.cypher;

import static org.neo4j.driver.v1.AccessMode.READ;
import static org.neo4j.driver.v1.AccessMode.WRITE;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Resource;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.springframework.stereotype.Component;

/**
 * <p>An interface to the Neo4j client. This can be used in two ways:
 * <ul>
 * 	<li>You get a {@link #newClient(boolean) new client} and then it's up to you to mark the transaction boundaries
 * 	via {@link CypherClient#begin()} and {@link CypherClient#end()}. This is useful when your client is used across multiple
 * 	methods in a complex way, so you prefer to control the transaction boundaries in your ways.</li>
 *  <li>Or, you can manage a whole session and single transaction in a simplified ways, by using lambda-receiving methods 
 *  like {@link #query(Function, boolean, boolean)}.</li>
 * </ul>
 * </p>
 * 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>15 Oct 2018</dd></dl>
 *
 */
@Component
public class CypherClientProvider
{
	@Resource ( name = "neoDriver" )
	private Driver neoDriver;
	
	CypherClientProvider () {
		this ( null );
	}
	
	CypherClientProvider ( Driver neoDriver ) {
		this.neoDriver = neoDriver;
	}

	
	public CypherClient newClient ( boolean writeAccessMode ) {
		return new CypherClient ( neoDriver.session ( writeAccessMode ? WRITE : READ ) );
	}

	/**
	 * Defaults to read-only mode.
	 */
	public CypherClient newClient () {
		return newClient ( false );
	}

	/**
	 * <p>Gets a {@link #newClient(boolean) new client} (which is also associated to a new Neo4j {@link Session}) and uses 
	 * it to run some code with a {@link CypherClient}, which is supposed to return something from one or more queries.</p>
	 * 
	 * <p>If the {@code inTransaction} parameter is set, wraps the parameter {@code action} into a new transaction. 
	 * At the end, the created client is closed and disposed. So, this is a simplified way to run Cypher-related actions 
	 * against the underlining Neo4j client.</p>  
	 */
	public <T> T query ( Function<CypherClient, T> action, boolean writeAccessMode, boolean inTransaction )
	{
		try ( CypherClient client = this.newClient ( writeAccessMode ) ) {
			return inTransaction 
				? client.runTx ( () -> action.apply ( client ) ) 
				: action.apply ( client );
		}
	}

	/** Defaults to `inTransaction` = true */
	public <T> T query ( Function<CypherClient, T> action, boolean writeAccessMode )
	{
		return query ( action, writeAccessMode, true );
	}
	
	
	/**
	 * Defaults to read-only mode and inTransaction = true.
	 */	
	public <T> T query ( Function<CypherClient, T> action ) {
		return query ( action, false );
	}

	/**
	 * A wrapper of {@link #query(Function, boolean, boolean)} to be used for actions that don't need to 
	 * return anything back. 
	 */
	public void run ( Consumer<CypherClient> action, boolean writeAccessMode, boolean inTransaction ) {
		query ( cli -> { action.accept ( cli ); return null; }, writeAccessMode, inTransaction );
	}

	/** Defaults to `inTransaction` = true */	
	public void run ( Consumer<CypherClient> action, boolean writeAccessMode ) {
		run ( action, writeAccessMode, true );
	}

	
	/**
	 * Defaults to read-only mode and inTransaction = true.
	 */		
	public void run ( Consumer<CypherClient> action ) {
		run ( action, false );
	}
}
