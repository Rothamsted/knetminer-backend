package uk.ac.rothamsted.knetminer.backend.cypher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.ebi.utils.runcontrol.ProgressLogger;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Jul 2019</dd></dl>
 *
 */
@Ignore ( "Not a real unit test, used to manually check how Neo4j behaves with semantic motif queries" )
public class KnetminerCypherTest
{
	private static Driver neoDriver;
	private static CypherClientProvider cyProvider;

	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@BeforeClass
	public static void initNeo4j ()
	{
		
		neoDriver = GraphDatabase.driver (
			"bolt://babvs65.rothamsted.ac.uk:7688",
			AuthTokens.basic ( "rouser", "rouser" )
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
			StatementResult res = session.run ( "MATCH (g:Gene{TAXID:'4565'}) RETURN g.iri LIMIT 10000" );
			return res.list ( r -> r.get ( 0 ).asString () );
		}
	}

	private List<String> getQueries () throws IOException
	{
		String qpath = 
			"/Users/brandizi/Documents/Work/RRes/ondex_git/knetminer/species/wheat-directed/ws/neo4j/state-machine-queries"; 
		
		return Arrays.asList ( 
			IOUtils.readFiles ( qpath, new WildcardFileFilter ( "*.cypher" ) )
		);
	}

	@Test 
	public void testSemanticMotifQueries () throws IOException
	{
		List<String> geneIris = getInputGeneUris ();
		List<String> queries = getQueries ();
		ProgressLogger tracker = new ProgressLogger ( "{} paths returned", 100 );
		
		// For each IRI and for each query
		geneIris
		.parallelStream ()
		.forEach ( iri -> 
		{
			queries
			.parallelStream ()
			.forEach ( query -> 
			{
				// get paginated results
				
				long pageSize = 2500;
				final String pgQuery = query + "\nSKIP $offset LIMIT " + pageSize;

				for ( long offset = 0;; offset += pageSize ) 
				{
					long offsetFinal = offset;
					// wrappint it into try() forces closure as soon as it's no longer needed
					try ( 
						Stream<List<String>> paths = cyProvider.queryToStream ( 
							client -> client.findPathIris ( 
								pgQuery, 
								Values.parameters ( "startIri", iri, "offset", offsetFinal ) 
							)
						)
						//.onClose ( () -> log.info ( "CLOSING" ) )
					)
					{
						long ct = paths.count ();
						if ( ct == 0 ) break;
						tracker.updateWithIncrement ( ct );
					}
				}
			});
		});
	}
}
