package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.BiMap;

import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.StateMachine;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.exceptions.InvalidFileException;
import net.sourceforge.ondex.algorithm.graphquery.exceptions.StateMachineInvalidException;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.memory.MemoryONDEXGraph;
import net.sourceforge.ondex.exception.type.ParsingFailedException;
import net.sourceforge.ondex.parser.oxl.Parser;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.ebi.utils.exceptions.UnexpectedValueException;
import uk.ac.ebi.utils.io.IOUtils;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>25 Mar 2019</dd></dl>
 *
 */
public class StateMachine2CyTranslator
{
	private StateMachine stateMachine;

  private BiMap<Integer, State> stateIndex;
  	  
  
  public StateMachine2CyTranslator ( String smPath )
  {
  	this ( smPath, null );
  }

  public StateMachine2CyTranslator ( String smPath, ONDEXGraph metadataGraph ) 
  {
  	this ( new File ( smPath ), metadataGraph );
  }
  
  public StateMachine2CyTranslator ( File smFile )
  {
  	this ( smFile, null );
  }

  
  public StateMachine2CyTranslator ( File smFile, ONDEXGraph metadataGraph )
  {  	
  	try {
			this.init ( new BufferedReader ( new FileReader ( smFile ) ), metadataGraph );
		}
		catch ( FileNotFoundException ex )
		{
			throwEx ( 
				UncheckedFileNotFoundException.class, ex, 
				"State machine file <%s> not found",
				smFile.getAbsolutePath ()
			);
		}
  	catch ( RuntimeException ex )
  	{
			throwEx (
				RuntimeException.class, ex,
				"Error while parsing the state machine definition <%s>: %s",
				smFile.getAbsoluteFile (),
				ex.getMessage () 
			);
  	}
  }

  public StateMachine2CyTranslator ( Reader smReader )
  {
  	this.init ( smReader, null );
  }

  public StateMachine2CyTranslator ( Reader smReader, ONDEXGraph metadataGraph )
  {
  	this.init ( smReader, metadataGraph );
  }

	public StateMachine2CyTranslator ( StateMachine sm, BiMap<Integer, State> stateIndex )
	{
		this.init ( sm, stateIndex );
	}
  
  
  private void init ( Reader smReader, ONDEXGraph metadataGraph )
  {
  	try 
  	{
    	if ( metadataGraph == null ) 
    	{
    		metadataGraph = new MemoryONDEXGraph ( "metadata" );
    		Parser oxlParser = new Parser ();

    		Reader reader = IOUtils.openResourceReader ( "data/xml/ondex_metadata.xml" );
    		
    		oxlParser.setONDEXGraph ( metadataGraph );
    		oxlParser.start ( reader );
    	}

    	CyTranslatorFlatFileParser smParser = new CyTranslatorFlatFileParser ();
			smParser.parseReader ( smReader, metadataGraph );
			this.init ( smParser.getStateMachine (), smParser.getStateIndex () );
		}
		catch ( InvalidFileException | StateMachineInvalidException ex )
		{
			throwEx (
				UnexpectedValueException.class, ex,
				"Error while parsing a state machine definition: %s",
				ex.getMessage () 
			);
		}
		catch ( IOException ex )
		{
			throwEx (
				UncheckedIOException.class, ex,
				"Error while parsing a state machine definition: %s",
				ex.getMessage () 
			);
		}
		catch ( ParsingFailedException ex )
		{
			throwEx (
				UncheckedIOException.class, ex,
				"Internal unexpected error while loading Ondex metadata: %s",
				ex.getMessage () 
			);
		}
  }
  	
	
	private void init ( StateMachine sm, BiMap<Integer, State> stateIndex )
	{
		this.stateMachine = sm;
		this.stateIndex = stateIndex;
	}
	
	public List<String> getCypherQueries ()
	{
		List<String> result = new ArrayList<> ();
		getCypherQueries ( stateMachine.getStart (), null, result, new HashSet<> (), -1 );
		return result;
	}
	
	private void getCypherQueries ( 
		State s, String partialQuery, List<String> result, Set<Transition> visited, int distance )
	{
		String nodeMatchStr = enumerate ( s ) + ":" + s.getValidConceptClass ().getId ();
		
		if ( stateMachine.getStart ().equals ( s ) ) {
			nodeMatchStr += "{ iri: $startIri }";
			partialQuery = "";
			distance = 0;
		}
				
		partialQuery += "(" + nodeMatchStr + ")";
					
		try
		{
			if ( stateMachine.isFinish ( s ) ) {
				result.add ( "MATCH path = " + partialQuery + "\nRETURN path" );
				return;
			}
			
			final String partialQueryFinal = partialQuery;
			final int newDistance = distance + 1;
			stateMachine.getOutgoingTransitions ( s ).forEach ( 
				t -> getCypherQueries ( t, partialQueryFinal, result, visited, newDistance )
			);
		}
		catch ( StateMachineInvalidException ex )
		{
			throwEx ( 
				IllegalArgumentException.class,
				ex,
				"Internal error while visiting a StateMachine: %s",
				ex.getMessage () 
			);
		}
	}

	private void getCypherQueries ( 
		Transition t, String partialQuery, List<String> result, Set<Transition> visited, int distance )
	{
		if ( visited.contains ( t ) ) return;
		visited.add ( t );
		
		String relMatchStr = enumerate ( t ) + ":" + t.getValidRelationType ().getId ();
		
		State src = this.stateMachine.getTransitionSource ( t );
		State dst = this.stateMachine.getTransitionTarget ( t );
		
		int minLenConstraint = 1;
		
		if ( src.equals ( dst ) ) {
			minLenConstraint = 0; 
		}
		
		int maxLenConstraint = -1;
		int len = t.getMaxLength ();
		if ( len < Integer.MAX_VALUE )
		{
			if ( distance + 1 > len ) return;
			maxLenConstraint = len - distance;
		}

		// min    max    format
		//  1      -      r:R
		//  1      n      r:R*1..n
		//  0      -      r:R*0..
		//  0      n      r:R*0..n
		String lenConstrStr = "";
		if ( minLenConstraint == 0 || maxLenConstraint != -1 )
			lenConstrStr += "*" + minLenConstraint + "..";
		if ( maxLenConstraint != -1 ) 
			lenConstrStr += maxLenConstraint;
		if ( !"*1..1".equals ( lenConstrStr ) )
			relMatchStr += lenConstrStr;
		
		partialQuery += "\n  - [" + relMatchStr + "] -> ";
		getCypherQueries ( stateMachine.getTransitionTarget ( t ), partialQuery, result, visited, distance + 1 );
	}

	private String enumerate ( State s )
	{
		int idx = this.stateIndex.inverse ().get ( s );
		String cc = s.getValidConceptClass ().getId ();
		
		return cc.toLowerCase () + "_" + idx;
	}
	
	private String enumerate ( Transition t )
	{
		State src = stateMachine.getTransitionSource ( t );
		State dst = stateMachine.getTransitionTarget ( t );
		
		BiMap<State, Integer> stateIds = this.stateIndex.inverse ();
		int srcIdx = stateIds.get ( src );
		int dstIdx = stateIds.get ( dst );
		String rt = t.getValidRelationType ().getId ();
		
		return rt.toLowerCase () + "_" + srcIdx + "_" + dstIdx;
	}
}
