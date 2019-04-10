package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.smtranslator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import net.sourceforge.ondex.algorithm.graphquery.flatfile.StateMachineDotExporter;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.parser.oxl.Parser;

/**
 * A quick-n-dirty command line interface to invoke the {@link StateMachine2CyTranslator}. This
 * is currently used through the {@code motif2cypher.sh} script on top of my POM project.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Mar 2019</dd></dl>
 *
 */
public class Motif2CypherCLI
{
	private static Logger log = LoggerFactory.getLogger ( Motif2CypherCLI.class );
	
	/**
	 * Arguments:
	 * <ul>
	 * 		<li>0 path to SemanticMotifs.txt</li>
	 * 		<li>1 directory where to create .cypher query files</li>
	 * 		<li>[2] path to an OXL file that contains metadata definitions referred by the semantic motif file. If omitted, 
	 * 		the usual default in the Ondex datadir/ project will be used.</li>
	 * </ul>
	 */
	public static void main ( String[] args ) throws UnsupportedEncodingException, IOException
	{
		String motifPath = args [ 0 ];
		String outPath = args [ 1 ];
		
		ONDEXGraph metaGraph = null;
		if ( args.length > 2 )
		{
			String metaPath = args [ 2 ];
			metaGraph = Parser.loadOXL ( metaPath );
		}
		
		StateMachine2CyTranslator converter = new StateMachine2CyTranslator ( motifPath, metaGraph );
		Map<String, String> queries = converter.getCypherQueries ();
		
		File outDir = new File ( outPath );
		if ( !outDir.exists () ) outDir.mkdirs ();
		
		for ( String name: queries.keySet () )
		{
			String query = queries.get ( name );
			String outName = "sm-" + name + ".cypher";
			Path outFilePath = Paths.get ( outPath, outName );
			
			log.info ( "Writing {}", outFilePath.toAbsolutePath ().toString () );
			
			Files.write ( Paths.get ( outPath, outName ),  query.getBytes ( "UTF-8" )	);
		}
		
		if ( metaGraph == null )
			log.warn ( "This version cannot write DOT rendering without loading a metadata OXL graph" );
		else
		{
			log.info ( "Writing DOT graph" );
			StateMachineDotExporter dotx = new StateMachineDotExporter ( motifPath, metaGraph );
			MutableGraph dotGraph = dotx.getGraph ();
			Graphviz gviz = Graphviz.fromGraph ( dotGraph ).width ( 1000 );
			gviz.render ( Format.XDOT ).toFile ( new File ( outPath + "/state-machine.dot" ) );
			gviz.render ( Format.SVG ).toFile ( new File ( outPath + "/state-machine.svg" ) );
		}
		
		
		log.info ( "The End." );
	}
}
