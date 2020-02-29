package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;

/**
 * Single-string, single-file Cypher query reader.
 * 
 * <p>For the moment, reads queries in a simple format: one query per line, blank lines and lines starting with
 * {@code '#'} are ignored.</p>
 * 
 * <p>TODO: advanced version, supporting multi-line queries.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>9 Dec 2019</dd></dl>
 *
 */
public class CyQueriesReader
{
	private static final Logger log = LoggerFactory.getLogger ( CyQueriesReader.class );
	
	private CyQueriesReader () {
	}
	
	public static List<String> readQueries ( Reader reader )
	{
		BufferedReader lineRdr = new BufferedReader ( reader );
		
		return lineRdr.lines ()
			.filter ( line -> line != null )
			.map ( String::trim )
			.filter ( line -> !( line.length () == 0 || line.startsWith ( "#" ) ) )
			.collect ( Collectors.toList () );
	}

	public static List<String> readQueries ( File qfile )
	{
		try {
			log.info ( "Loading queries from '{}'", qfile.getAbsolutePath () );
			return readQueries ( new FileReader ( qfile ) );
		}
		catch ( FileNotFoundException ex )
		{
			throw ExceptionUtils.buildEx (
				UncheckedFileNotFoundException.class,
				ex,
				"Error while reading query file '%s': %s",
				qfile.getAbsolutePath (),
				ex.getMessage ()
			);
		}
	}
	
	public static List<String> readQueries ( String qfileName ) {
		return readQueries ( new File ( qfileName ) );
	}

	public static List<String> readQueriesFromString ( String queriesString ) {
		return readQueries ( new StringReader ( queriesString ) );
	}
	
}
