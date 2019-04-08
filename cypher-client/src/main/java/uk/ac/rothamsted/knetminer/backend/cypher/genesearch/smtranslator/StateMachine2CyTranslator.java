package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.smtranslator;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  
  private Logger log = LoggerFactory.getLogger ( this.getClass () );
  
  
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
		traverseStateMachine ( stateMachine.getStart (), "", result, 0, false );
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
		State s, String partialQuery, Map<String, String> result, final int distance, boolean isLoopMode 
	)
	{
		try
		{
			// First, let's add the node match
			partialQuery += buildNodeMatch ( s );

			// Recursion ends here
			if ( stateMachine.isFinish ( s ) ) 
			{
				// A new path, choose a name and build the final query by wrapping the clauses collected so far from
				// upstream nodes.
				String qname = format ( "%03d_L%02d_%s", result.size () + 1, distance, buildNodeId ( s ) );
				result.put ( qname, "MATCH path = " + partialQuery + "\nRETURN path" );
				return;
			}

			Set<Transition> transitions = this.stateMachine.getOutgoingTransitions ( s );

			// We need to create different departures for different states and/or different constraint lengths
			
			Map<Pair<State, Integer>, List<Transition>> byLenTrns = 
				transitions.stream ().collect ( Collectors.groupingBy ( 
				t -> Pair.of ( this.stateMachine.getTransitionTarget ( t ), t.getMaxLength () )
			));
			
			// If we have a loop, we need to make an iteration that creates a hop with the loop only, and then
			// let the recursion to continue from the loop node, as if it were another node at the loop's end.
			//
			Map.Entry<Pair<State, Integer>, List<Transition>> loops = byLenTrns
				.entrySet ()
				.stream ()
				.filter ( e -> s.equals ( e.getKey ().getLeft () ) )
				.findAny ()
				.orElse ( null );
			
			if ( loops != null )
			{
				// So, we do have loops, if we're in loop mode, we already worked the loop transitions
				if ( isLoopMode ) {
					byLenTrns.entrySet ().remove ( loops );
					isLoopMode = false;
				}
				else {
					// Or, if we haven't worked out the loop yet, we need to do it with the loop transitions
					// only and setting the loop mode.
					byLenTrns = new HashMap<> ();
					byLenTrns.put ( loops.getKey (), loops.getValue () );
					isLoopMode = true;
				}
			}
			else 
			{
				// If you look at the above logics, this shouldn't happen, let's report a warning
				if ( isLoopMode ) log.warn ( 
					"wrong state: no loop in loop mode for state {}", s.getValidConceptClass ().getId ()
				);
				isLoopMode = false;
			}
			
			final String partialQueryFinal = partialQuery;
			final boolean nextLoopMode = isLoopMode;
			
			byLenTrns.forEach ( (grp, trnsGroup) -> 
			{
				String nextPartialQuery = partialQueryFinal + "\n  " + buildRelationMatch ( trnsGroup, distance );
				State target = grp.getLeft ();
				traverseStateMachine ( target, nextPartialQuery, result, distance + 1, nextLoopMode );
			});
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

	
	private String buildNodeMatch ( State s )
	{		
		// eg, (gene_1:Gene). The number is the node index in the original SM.
		String nodeMatchStr = buildNodeId ( s ) + ":" + s.getValidConceptClass ().getId ();
		
		if ( stateMachine.getStart ().equals ( s ) ) {
			// for the first state, the form is (gene_1:Gene{ iri: $startIri}), where the placeholder
			// is instantiated by the graph traverser.
			nodeMatchStr += "{ iri: $startIri }";
		}
				
		return "(" + nodeMatchStr + ")";
	}
	
	
	private String buildRelationMatch ( List<Transition> transitions, int distance )
	{
		// It all begins with something like rel_xxx:
		String relMatchStr = buildRelId ( transitions ) + ":";

		// Then it becomes rel_xxx:R or rel_xxx:R1|R2...Rn
		String relTypesStr = null;
		for ( Transition t: transitions )
		{
			if ( relTypesStr == null ) relTypesStr = "";
			else relTypesStr += "|";
			relTypesStr += t.getValidRelationType ().getId ();
		}
		relMatchStr += relTypesStr;
		
		
		// Now let's consider a possible path length constraint.
		// 
		
		// In the SM files length constraints specified in terms of total path length up to the current transition (where
		// first node has distance = 1, nodes and edges count as 1 each), so we need to convert this constraints to no. of
		// max repetitions allowed for the current transition/relation
		//
		int maxRelRepeats = -1;
		// Remember we're assuming all have the same len constraint, so this will do
		int len = transitions.iterator ().next ().getMaxLength ();
		if ( len < Integer.MAX_VALUE ) {
			// We cannot match the len constraint, report null
			if ( distance + 1 > len ) return null;
			maxRelRepeats = len - distance;
		}

		
		// If some len constraint is defined, it needs to become rel_xxx:RR*1..MAX
		// (where RR is either a single relation or a '|' OR of multiple relations
		if ( maxRelRepeats != -1 && maxRelRepeats != 1 ) relMatchStr += "*1.." + maxRelRepeats;
		
		return "- [" + relMatchStr + "] ->";
	}
	
	
	/** 
	 * Provides a suitable node variable name for a SM node, eg. gene_1 from index 1 and node of type Gene
	 */
	private String buildNodeId ( State s )
	{
		int idx = this.stateIndex.inverse ().get ( s );
		String cc = s.getValidConceptClass ().getId ();
				
		return StringUtils.uncapitalize ( cc ) + "_" + idx;
	}
	
	
	private String buildRelId ( List<Transition> transitions )
	{
		Transition t = transitions.iterator ().next ();
		State src = stateMachine.getTransitionSource ( t );
		State dst = stateMachine.getTransitionTarget ( t );
	
		BiMap<State, Integer> stateIds = this.stateIndex.inverse ();
		int srcIdx = stateIds.get ( src );
		int dstIdx = stateIds.get ( dst );
		
		String prefix = transitions.size () == 1 ? t.getValidRelationType ().getId ().toLowerCase () : "rel";
	
		return prefix + "_" + srcIdx + "_" + dstIdx;
	}
}
