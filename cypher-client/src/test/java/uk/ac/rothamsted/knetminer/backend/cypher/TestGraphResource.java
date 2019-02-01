package uk.ac.rothamsted.knetminer.backend.cypher;

import org.junit.rules.ExternalResource;

import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import net.sourceforge.ondex.parser.oxl.Parser;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jan 2019</dd></dl>
 *
 */
public class TestGraphResource extends ExternalResource
{
	private static LuceneEnv luceneMgr = null;
	private static ONDEXGraph graph = null;
	
	@Override
	protected synchronized void before () throws Throwable
	{
		if ( graph != null ) return;
		
		graph = Parser.loadOXL ( "target/test-classes/ara-tiny.oxl" );
		luceneMgr = new LuceneEnv ( "target/ara-tiny-lucene", true );
		luceneMgr.setONDEXGraph ( graph );
	}

	@Override
	protected synchronized void after ()
	{
		if ( luceneMgr == null ) return;
		luceneMgr.closeIndex ();
	}

	
	public LuceneEnv getLuceneMgr () {
		return luceneMgr;
	}

	public ONDEXGraph getGraph () {
		return graph;
	}
	
}
