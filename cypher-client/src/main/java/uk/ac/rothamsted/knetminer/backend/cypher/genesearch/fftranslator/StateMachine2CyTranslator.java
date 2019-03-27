package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import static java.lang.String.format;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.BiMap;

import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.StateMachine;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.exceptions.InvalidFileException;
import net.sourceforge.ondex.algorithm.graphquery.exceptions.StateMachineInvalidException;
import net.sourceforge.ondex.algorithm.graphquery.flatfile.StateMachineFlatFileParser2;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.memory.MemoryONDEXGraph;
import net.sourceforge.ondex.exception.type.ParsingFailedException;
import net.sourceforge.ondex.parser.oxl.Parser;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.ebi.utils.exceptions.UnexpectedValueException;
import uk.ac.ebi.utils.io.IOUtils;

/**
 * Converts a semantic motif file (parsed via {@link StateMachineFlatFileParser2}) to a set
 * of Cypher queries, which can be used with our new KnetMiner backend.
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

  /**
   * The {@link StateMachineFlatFileParser2} uses metadata definitions in metadataGraph. If the latter
   * is null, the Ondex default will be used.
   */
  public StateMachine2CyTranslator ( Reader smReader, ONDEXGraph metadataGraph )
  {
  	this.init ( smReader, metadataGraph );
  }

  /**
   * This is to be used with the output of {@link CyTranslatorFlatFileParser}. 
   */
	public StateMachine2CyTranslator ( StateMachine sm, BiMap<Integer, State> stateIndex )
	{
		this.init ( sm, stateIndex );
	}
  
  /** See {@link #StateMachine2CyTranslator(Reader, ONDEXGraph)} */
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

    	StateMachineFlatFileParser2 smParser = new StateMachineFlatFileParser2 ();
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
  	
	/** 
	 * @see #StateMachine2CyTranslator(StateMachine, BiMap) 
	 */
	private void init ( StateMachine sm, BiMap<Integer, State> stateIndex )
	{
		this.stateMachine = sm;
		this.stateIndex = stateIndex;
	}
	
	/**
	 * Get the queries corresponding to the paths in the original {@link StateMachine}. The keys
	 * contain suitable query names (to be used to write files), which are based on last node ID and type, 
	 * plus path length.
	 * 
	 */
	public Map<String, String> getCypherQueries ()
	{
		Map<String, String> result = new HashMap<> ();
		traverseStateMachine ( stateMachine.getStart (), null, result, new HashSet<> (), -1 );
		return result;
	}
	
	/**
	 * Support recursive method to visit the {@link StateMachine} graph and collect query clauses.
	 * 
	 * At the end of a path, a new result is put into the result variable. During the path traversing, partialQuery
	 * is filled with Cypher clauses. distance is the length of the path built so far, used internally for things like
	 * length constraints.
	 * 
	 * @see #getCypherQueries to get an idea on how parameters are initialised upon first call. 
	 */
	private void traverseStateMachine ( 
		State s, String partialQuery, Map<String, String> result, Set<Transition> visitedEdges, int distance )
	{
		// eg, (gene_1:Gene). The number is the node index in the original SM.
		
		String nodeMatchStr = enumerate ( s ) + ":" + s.getValidConceptClass ().getId ();
		
		if ( stateMachine.getStart ().equals ( s ) ) {
			// for the first state, the form is (gene_1:Gene{ iri: $startIri}), where the placeholder
			// is instantiated by the graph traverser.
			nodeMatchStr += "{ iri: $startIri }";
			partialQuery = "";
			distance = 0;
		}
				
		partialQuery += "(" + nodeMatchStr + ")";
					
		try
		{
			if ( stateMachine.isFinish ( s ) ) 
			{
				// A new path, choose a name and build the final query by wrapping the clauses collected so far from
				// upstream nodes.
				String qname = format ( "%03d_L%02d_%s", result.size () + 1, distance, enumerate ( s ) );
				result.put ( qname, "MATCH path = " + partialQuery + "\nRETURN path" );
				return;
			}
			
			final String partialQueryFinal = partialQuery;
			final int newDistance = distance + 1;
			stateMachine.getOutgoingTransitions ( s ).forEach ( 
				t -> traverseStateMachine ( t, partialQueryFinal, result, visitedEdges, newDistance )
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

	/**
	 * Support method to visit the {@link StateMachine} graph and collect query clauses.
	 * @see {@link #traverseStateMachine(State, String, Map, Set, int)}.
	 */
	private void traverseStateMachine ( 
		Transition t, String partialQuery, Map<String, String> result, Set<Transition> visitedEdges, int distance )
	{
		if ( visitedEdges.contains ( t ) ) return;
		visitedEdges.add ( t );
		
		// eg, - [encode_1_2: encode] -> The number are the endpoint indexes coming from the original SM.
		String relMatchStr = enumerate ( t ) + ":" + t.getValidRelationType ().getId ();
		
		State src = this.stateMachine.getTransitionSource ( t );
		State dst = this.stateMachine.getTransitionTarget ( t );
		
		// We need the form [encode_1_2: encode*1..3] if constraints on number of repeated edges occur 
		//
		
		// min len isn't suppoted by the SM syntax, but 0 is needed for loop transitions
		int minLenConstraint = 1;
		if ( src.equals ( dst ) ) minLenConstraint = 0; 
	
		
		// In the SM files length constraints specified in terms of total path length up to the current transition (where
		// first node has distance = 1, nodes and edges count as 1 each), so we need to convert this constraints to no. of
		// max repetitions allowed for the current transition/relation
		//
		int maxRelRepeats = -1;
		int len = t.getMaxLength ();
		if ( len < Integer.MAX_VALUE )
		{
			if ( distance + 1 > len ) return;
			maxRelRepeats = len - distance;
		}

		// min    max    format
		//  1      -      r:R
		//  1      n      r:R*1..n
		//  0      -      r:R*0..
		//  0      n      r:R*0..n
		String lenConstrStr = "";
		if ( minLenConstraint == 0 || maxRelRepeats != -1 )
			lenConstrStr += "*" + minLenConstraint + "..";
		if ( maxRelRepeats != -1 ) 
			lenConstrStr += maxRelRepeats;
		if ( ! ( lenConstrStr.length () == 0 || "*1..1".equals ( lenConstrStr ) ) )
			relMatchStr += lenConstrStr;
		
		partialQuery += "\n  - [" + relMatchStr + "] -> ";
		traverseStateMachine ( stateMachine.getTransitionTarget ( t ), partialQuery, result, visitedEdges, distance + 1 );
	}

	/** 
	 * Provides a suitable node variable name for a SM node, eg. gene_1 from index 1 and node of type Gene
	 */
	private String enumerate ( State s )
	{
		int idx = this.stateIndex.inverse ().get ( s );
		String cc = s.getValidConceptClass ().getId ();
				
		return StringUtils.uncapitalize ( cc ) + "_" + idx;
	}
	
	/** 
	 * Provides a suitable relation variable name for a SM node, eg. encode_1_2 from relation type encode and endpoints 
	 * with indexes 1 and 2.
	 */
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
