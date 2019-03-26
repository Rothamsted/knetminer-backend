package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>25 Mar 2019</dd></dl>
 *
 */
public class FFTranslatorTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Test
	public void testTranslate ()
	{
		StateMachine2CyTranslator translator = new StateMachine2CyTranslator (
			"target/test-classes/test-state-machine.txt"
		);
		List<String> queries = translator.getCypherQueries ();
		
		log.info ( "Found {} queries:", queries.size () );
		queries.forEach ( q -> log.info ( "\n\nQUERY: {}\n", q ) );
	}

	@Test
	public void testLoop ()
	{
		StateMachine2CyTranslator translator = new StateMachine2CyTranslator (
			"target/test-classes/test-state-machine-loop.txt"
		);
		List<String> queries = translator.getCypherQueries ();
		
		log.info ( "Found {} queries:", queries.size () );
		queries.forEach ( q -> log.info ( "\n\nQUERY: {}\n", q ) );
	}
}
