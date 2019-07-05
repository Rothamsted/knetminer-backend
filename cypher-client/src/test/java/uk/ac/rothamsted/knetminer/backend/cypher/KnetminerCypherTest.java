package uk.ac.rothamsted.knetminer.backend.cypher;

import java.util.List;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Jul 2019</dd></dl>
 *
 */
public class KnetminerCypherTest
{
	private static Driver neoDriver;
	private static CypherClientProvider cyProvider;

	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@BeforeClass
	public static void initNeo4j ()
	{
		neoDriver = GraphDatabase.driver (
			"bolt://127.0.0.1:" + System.getProperty ( "neo4j.server.boltPort" ),
			AuthTokens.basic ( "neo4j", "test" )
		);	
		
		cyProvider = new CypherClientProvider ( neoDriver );
	}

	@AfterClass
	public static void closeNeo4j ()
	{
		if ( neoDriver == null ) return;
		neoDriver.close ();		
	}
	

	private List<String> getInputGeneUris ()
	{
		try ( Session session = neoDriver.session ( AccessMode.READ ) )
		{
			StatementResult res = session.run ( "MATCH (g:Gene{TAXID:'4565'}) RETURN g.iri LIMIT 250" );
			return res.list ( r -> r.get ( 0 ).asString () );
		}
	}
	
	public void testSemanticMotifQueries ()
	{
		
	}
}
