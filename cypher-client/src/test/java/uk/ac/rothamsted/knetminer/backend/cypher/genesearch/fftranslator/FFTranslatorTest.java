package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import java.util.List;
import java.util.Map;

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
		Map<String, String> queries = convert ( "target/test-classes/test-state-machine.txt" );
		// TODO: verify
	}

	@Test
	public void testLoop ()
	{
		Map<String, String> queries = convert ( "target/test-classes/test-state-machine-loop.txt" );
		// TODO: verify
	}
	
	private Map<String, String> convert ( String smPath )
	{
		StateMachine2CyTranslator translator = new StateMachine2CyTranslator ( smPath );
		Map<String, String> queries = translator.getCypherQueries ();
		
		log.info ( "Found {} queries:", queries.size () );
		queries.forEach ( (name,q) -> log.info ( "\n\nQUERY '{}': {}\n", name, q ) );
		return queries;
	}
}
