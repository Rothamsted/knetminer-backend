package uk.ac.rothamsted.knetminer.backend.cypher;

import org.junit.rules.ExternalResource;

import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.util.GraphMemIndex;
import net.sourceforge.ondex.parser.oxl.Parser;

/**
 * An {@link ExternalResource} that initialiases a new {@link ONDEXGraph} to be used for the tests and also prepares/resets
 * its {@link GraphMemIndex}.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jan 2019</dd></dl>
 *
 */
public class TestGraphResource extends ExternalResource
{
	private static ONDEXGraph graph = null;
	
	@Override
	protected synchronized void before ()
	{
		if ( graph == null )
			graph = Parser.loadOXL ( "target/dependency/poaceae-sample.oxl" );
		//GraphMemIndex.getInstance ( graph ).updateIndex ();
	}

	@Override
	protected synchronized void after ()
	{
		//GraphMemIndex.getInstance ( graph ).clear ();
	}

	
	public ONDEXGraph getGraph () {
		return graph;
	}
	
}
