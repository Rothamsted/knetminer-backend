package uk.ac.rothamsted.knetminer.backend.cypher;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
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
			AuthTokens.basic ( "neo4j", "test" )
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
			"MATCH path = (g:Gene)-[enc:enc]->(p:Protein)-[hss:h_s_s]->(p1:Protein)\n" +
			"-[pubref:pub_in]->(pub:Publication)\n" + 
			"RETURN path\n" + 
			"ORDER BY hss.E_VALUE, hss.PERCENTALIGNMENT DESC\n" + 
			"LIMIT 10";
		
		List<List<String>> iris = cyProvider.query ( 
			client -> client.findPathIris ( query ).collect ( Collectors.toList () ) 
		);
		
		Assert.assertNotNull ( "Null result!", iris );
		
		log.info ( "======== Results from findPathIris() =======" );
		int i = 0;
		for ( List<String> pathIris: iris )
			log.info ( "\t#{}: [{}]", i++, String.join ( ", ", pathIris ) );
		
		Assert.assertTrue ( 
			"Expected Result not found!", 
			iris.stream().filter ( pathIris -> 
			{
				BiFunction<String, Integer, Boolean> uriChecker = 
					( id, idx ) -> pathIris.get ( idx ).startsWith ( iri ( "bkr", id ) ); 

				return uriChecker.apply ( "gene_at1g63650_locus_2026629", 0 )
						&& uriChecker.apply ( "enc_", 1 )
						&& uriChecker.apply ( "h_s_s_", 3 )							
						&& uriChecker.apply ( "publication_2674946", 6 );
			})
			.findAny ()
			.isPresent ()
		);
	}
	

	@Test
	public void findPathsTest ()
	{
		CypherClientProvider cyProvider = new CypherClientProvider ( neoDriver );
		
		String query = 
			"MATCH path = (g:Gene)-[enc:enc]->(p:Protein)-[hss:h_s_s]->(p1:Protein)\n" +
			"-[pubref:pub_in]->(pub:Publication)\n" + 
			"RETURN path\n" + 
			"ORDER BY hss.E_VALUE, hss.PERCENTALIGNMENT DESC\n" + 
			"LIMIT 10";
		
		List<List<ONDEXEntity>> odxEnts = cyProvider.query ( 
			client -> client.findPaths ( graphResource.getLuceneMgr (), query ).collect ( Collectors.toList () ) 
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
					
				return oeChecker.apply ( "C{Gene:AT1G63650;locus:2026629", 0 )
					&& oeChecker.apply ( "R{h_s_s", 3 )
					&& oeChecker.apply ( "C{Protein:P13027", 4 )
					&& oeChecker.apply ( "C{Publication", 6 );
			})
		);
	}	
}
