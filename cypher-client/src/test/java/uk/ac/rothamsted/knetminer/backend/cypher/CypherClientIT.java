package uk.ac.rothamsted.knetminer.backend.cypher;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.util.ONDEXGraphUtils;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>14 Nov 2018</dd></dl>
 *
 */
public class CypherClientIT
{
	private static Driver neoDriver;	

	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@ClassRule
	public static TestGraphResource graphResource = new TestGraphResource ();

		
	@BeforeClass
	public static void initNeo4j ()
	{
		neoDriver = GraphDatabase.driver (
			"bolt://127.0.0.1:" + System.getProperty ( "neo4j.server.boltPort" ),
			AuthTokens.basic ( "neo4j", "testTest" )
		);		
	}

	@AfterClass
	public static void closeNeo4j ()
	{
		if ( neoDriver == null ) return;
		neoDriver.close ();		
	}
	
	
	@Test
	public void findPathIrisTest ()
	{
		CypherClientProvider cyProvider = new CypherClientProvider ( neoDriver );
		
		String query = 
			"MATCH path = \n" + 
			"(g:Gene)-[enc:enc]->(p:Protein)\n" + 
			"  -[hss:h_s_s]->(p1:Protein)\n" + 
			"  -[pubref:pub_in]->(pub:Publication)\n" +
			"RETURN path\n" +
			"LIMIT 100\n"; // Normally not needed, it's just for safety in this test 
		
		List<List<String>> iris = cyProvider.query ( 
			client -> client.findPathIris ( query ).collect ( Collectors.toList () ) 
		);
		
		Assert.assertNotNull ( "Null result!", iris );
		
		log.info ( "======== Results from findPathIris() =======" );
		int i = 0;
		for ( List<String> pathIris: iris )
			log.info ( "\t#{}: [{}]", i++, String.join ( ", ", pathIris ) );
		
		
		verifyResultIris ( "Expected Result about traescs1d02g284700 not found!", iris,
			"gene_traescs1d02g284700", 0,
			"enc_", 1,
			"h_s_s_", 3,							
			"publication_16407447", 6
		);
		
		verifyResultIris ( "Expected os11g0569733 not found!", iris,
			"gene_os11g0569733", 0,
			"enc_", 1,
			"protein_g7jik2", 4,
			"pub_in_", 5,
			"publication_22399647", 6
		);
	}
	

	@Test
	public void findPathsTest ()
	{
		CypherClientProvider cyProvider = new CypherClientProvider ( neoDriver );
		
		String query =
			"MATCH path = \n" + 
			"(g:Gene)-[enc:enc]->(p:Protein)\n" + 
			"  -[hss:h_s_s]->(p1:Protein)\n" + 
			"  -[pubref:pub_in]->(pub:Publication)\n" +
			"RETURN path\n" +
			"LIMIT 100\n"; // Normally not needed, it's just for safety in this test 
		
		List<List<ONDEXEntity>> odxEnts = cyProvider.query ( 
			client -> client.findPaths ( graphResource.getGraph (), query ).collect ( Collectors.toList () ) 
		);
		
		Assert.assertNotNull ( "Null result!", odxEnts );
		
		log.info ( "======== Results from findPaths() =======" );
				
		int i = 0;
		for ( List<ONDEXEntity> oes: odxEnts )
		{
			String pathStr = oes.stream ()
			.map ( ONDEXGraphUtils::getString )
			.collect ( Collectors.joining ( ", ", "[", "]") );
			
			log.info ( "\t#{}: [{}]", i++, pathStr );
		}
		
				
		assertTrue ( 
			"Expected Result not found!", 
			odxEnts.stream().anyMatch ( pathIris -> 
			{
				BiFunction<String, Integer, Boolean> oeChecker = (label, idx) -> 
					ONDEXGraphUtils.getString ( pathIris.get ( idx ) ).startsWith ( label );
					
				return oeChecker.apply ( "C{Gene:ZM00001EB205480", 0 )
					&& oeChecker.apply ( "R{h_s_s", 3 )
					&& oeChecker.apply ( "C{Protein:Q8GU88", 4 )
					&& oeChecker.apply ( "C{Publication:16506311", 6 );
			})
		);
	}
	
	/**
	 * Tests with parametric query.
	 */
	@Test
	public void findPathIrisWithParamsTest ()
	{
		CypherClientProvider cyProvider = new CypherClientProvider ( neoDriver );
		
		String query = 
			"MATCH path = \n" + 
			"(g:Gene)-[enc:enc]->(p:Protein)\n" + 
			"  -[hss:h_s_s]->(p1:Protein)\n" + 
			"  -[pubref:pub_in]->(pub:Publication)\n" +
			"WHERE g.iri IN $startGeneIris\n" +
			"RETURN path\n" +
			"LIMIT 100\n"; // Normally not needed, it's just for safety in this test 
		
		List<String> probeIris = Arrays.asList (
			iri ( "bkr", "gene_traescs1d02g284700" ),
			iri ( "bkr", "gene_zm00001eb009240" )
		);
		
		
		List<List<String>> iris = cyProvider.query ( 
			client -> client.findPathIris ( query, Values.parameters ( "startGeneIris", probeIris ) )
									.collect ( Collectors.toList () ) 
		);
		
		Assert.assertNotNull ( "Null result!", iris );
		
		log.info ( "======== Results from findPathIris() =======" );
		int i = 0;
		for ( List<String> pathIris: iris )
			log.info ( "\t#{}: [{}]", i++, String.join ( ", ", pathIris ) );
		
		verifyResultIris ( "Expected Result about traescs1d02g284700 not found!", iris,
			"gene_traescs1d02g284700", 0,
			"enc_", 1,
			"h_s_s_", 3,							
			"publication_16407447", 6
		);
		
		verifyResultIris ( "Expected zm00001eb009240 not found!", iris,
			"gene_zm00001eb009240", 0,
			"enc_", 1,
			"protein_q94g17", 4,
			"pub_in_", 5,
			"publication_11457457", 6
		);
	}
	
	
	private void verifyResultIris (
		String failMsg,
		List<List<String>> pathsIris, Object ...iriIdAndIdxProbes 
	)
	{
		Assert.assertTrue ( 
			failMsg, 
			pathsIris.stream().filter ( pathIris -> 
			{
				for ( int i = 0; i < iriIdAndIdxProbes.length - 1; i++ )
				{
					String id = (String) iriIdAndIdxProbes [ i ];
					int idx = (int) iriIdAndIdxProbes [ ++i ];
					if ( idx >= pathIris.size () || !pathIris.get ( idx ).startsWith ( iri ( "bkr", id )) )
						return false;
				}
				return true;
			})
			.findAny ()
			.isPresent ()
		);
	}
}
