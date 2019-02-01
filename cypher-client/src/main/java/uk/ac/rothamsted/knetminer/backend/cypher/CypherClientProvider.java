package uk.ac.rothamsted.knetminer.backend.cypher;

import static org.neo4j.driver.v1.AccessMode.READ;
import static org.neo4j.driver.v1.AccessMode.WRITE;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Resource;

import org.neo4j.driver.v1.Driver;
import org.springframework.stereotype.Component;

/**
 * TODO: comment me!
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
