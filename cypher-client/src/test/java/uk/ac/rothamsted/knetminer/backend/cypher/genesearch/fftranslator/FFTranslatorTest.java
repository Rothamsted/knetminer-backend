package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests {@link StateMachine2CyTranslator}. Tests are based on dummy SM files in src/test/resources, open them
 * to get an idea of their structure. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Mar 2019</dd></dl>
 *
 */
public class FFTranslatorTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Test
	public void testTranslate ()
	{
		Map<String, String> queries = convert ( "target/test-classes/test-state-machine.txt" );

		assertEquals ( "Wrong no. of results!", 5, queries.size () );
		
		Stream.of ( "L04_path_4", "L04_enzyme_8", "L04_path_5" ).forEach ( name ->
			assertTrue ( 
				name + " name not found!", 
				queries.keySet ().stream ().anyMatch ( n -> n.contains ( name ) ) 
		));

		Stream.of ( 
			"gene_1:Gene{ iri: $startIri }",
			"(bioProc_3:BioProc)",
			"- [part_of_3_5:part_of] -> (path_5:Path)" 
		)
		.forEach ( qfrag ->
			assertTrue ( 
				"Cypher fragment: " + qfrag + " not found!", 
				queries.values ().stream ().anyMatch ( q -> q.contains ( qfrag ) ) 
		));
	}

	
	@Test
	public void testLoop ()
	{
		Map<String, String> queries = convert ( "target/test-classes/test-state-machine-loop.txt" );
		
		assertEquals ( "Wrong no. of results!", 5, queries.size () );
		
		Stream.of ( "L04_path_4", "L06_publication_6", "L06_path_5" ).forEach ( name ->
			assertTrue ( 
				name + " name not found!", 
				queries.keySet ().stream ().anyMatch ( n -> n.contains ( name ) ) 
		));

		Stream.of ( 
			"gene_1:Gene{ iri: $startIri }",
			"(bioProc_3:BioProc)",
			"- [part_of_3_5:part_of] -> (path_5:Path)",
			"asso_wi_7_8:asso_wi*1..2",
			"part_of_3_3:part_of*0..2"
		)
		.forEach ( qfrag ->
			assertTrue ( 
				"Cypher fragment: " + qfrag + " not found!", 
				queries.values ().stream ().anyMatch ( q -> q.contains ( qfrag ) ) 
		));
	}
	
	/**
	 * Basic code to load a SM file, translate it to queries and log its output. 
	 */
	private Map<String, String> convert ( String smPath )
	{
		StateMachine2CyTranslator translator = new StateMachine2CyTranslator ( smPath );
		Map<String, String> queries = translator.getCypherQueries ();
		
		log.info ( "Found {} queries:", queries.size () );
		queries.forEach ( (name,q) -> log.info ( "\n\nQUERY '{}': {}\n", name, q ) );
		return queries;
	}
}
