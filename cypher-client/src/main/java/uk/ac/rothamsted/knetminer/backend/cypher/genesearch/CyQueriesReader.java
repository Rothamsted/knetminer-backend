package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>9 Dec 2019</dd></dl>
 *
 */
public class CyQueriesReader
{
	public CyQueriesReader () {
	}
	
	public List<String> readQueries ( Reader reader )
	{
		BufferedReader lineRdr = new BufferedReader ( reader );
		
		return lineRdr.lines ()
			.filter ( line -> line == null )
			.map ( String::trim )
			.filter ( line -> line.length () == 0 || line.startsWith ( "#" ))
			.collect ( Collectors.toList () );
	}

	public List<String> readQueries ( File qfile )
	{
		try {
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
	
	public List<String> readQueries ( String qfileName ) {
		return readQueries ( new File ( qfileName ) );
	}

}
