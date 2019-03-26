package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			String outName = "semantic-motif-" + name + ".cypher";
			
			log.info ( "Writing {}", outName );
			
			Files.write ( Paths.get ( outPath, outName ),  query.getBytes ( "UTF-8" )	);
		}
		
		log.info ( "The End." );
	}
}
