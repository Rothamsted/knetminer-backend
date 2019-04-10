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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
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
  
  /** Used in a couple of point to assign a deterministic order to transition lists **/
  private static Comparator<Transition> transitionComparator = 
  	(t1, t2) -> t1.getValidRelationType ().getId ().compareTo ( t2.getValidRelationType ().getId () );		
 
  
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
		traverseStateMachine ( stateMachine.getStart (), "", result, 1, false );
		return result;
	}
	
	
	/**
	 * Recursive method to visit the {@link StateMachine} graph and collect query clauses.
	 * 
	 * At the end of a path, a new result is put into the result variable. During the path traversing, partialQuery
	 * is filled with Cypher clauses. distance is the length of the path built so far, used internally for things like
	 * length constraints (initially it's 1, for coherence with the semantics used in the {@link StateMachine}).
	 * 
	 * isLoopMode is used to manage two different calls with nodes having looping transitions (ie, having the same node
	 * at both ends).
	 * 
	 * @see #getCypherQueries to get an idea on how parameters are initialised upon first call. 
	 */
	private void traverseStateMachine ( 
		final State state, String partialQuery, Map<String, String> result, final int distance, final boolean isLoopMode 
	)
	{
		try
		{
			// First, let's add the node match
			partialQuery += buildNodeMatch ( state, isLoopMode );

			// Recursion ends here
			if ( stateMachine.isFinish ( state ) ) 
			{
				// A new path, choose a name and build the final query by wrapping the clauses collected so far from
				// upstream nodes.
				String qname = format ( "%03d_L%02d_%s", result.size () + 1, distance, buildNodeId ( state ) );
				result.put ( qname, "MATCH path = " + partialQuery + "\nRETURN path" );
				return;
			}

			// Now, let's recurse over the transitions
			Set<Transition> transitions = this.stateMachine.getOutgoingTransitions ( state );

			// We need to create different departures for different states and/or different constraint lengths
			// So, this will contain: (destination state d, maxRepeats) -> [state->d transitions]
			Map<Pair<State, Integer>, List<Transition>> byLenTrns = 
				transitions.stream ().collect ( 
					Collectors.groupingBy ( t ->
						Pair.of ( this.stateMachine.getTransitionTarget ( t ), getMaxRelRepeats ( t, distance ) )
			));
						
			// If we have loops, we need to make a recursion step that creates a hop with the loops only, and then
			// let the recursion to continue from the loop node, as if it were another node at the loop's end.
			//
			Map<Pair<State, Integer>, List<Transition>> loops = byLenTrns
				.entrySet ()
				.stream ()
				.filter ( e -> state.equals ( e.getKey ().getLeft () ) )
				.collect ( Collectors.toMap ( Map.Entry::getKey, Map.Entry::getValue ) );
			
			final boolean nextLoopMode;
			
			// So, we do have loops
			if ( !loops.isEmpty () )
			{
				if ( !isLoopMode ) {
					// First time we meet a loop on this node, we make a step with the loop transitions only 
					// (as if the single involving  node were two different ones) and loop mode on
					byLenTrns = loops;
					nextLoopMode = true;
				}
				else {
					// Or, if we come up to this same situation a second time, we continue with the regular
					// transitions only (ie, from the "second node" of the loop)
					//
					byLenTrns.entrySet ().removeAll ( loops.entrySet () );
					nextLoopMode = false;
				}
			}
			else 
			{
				// No loop met for this node. If you look at the above logics, loop mode should be always off here, 
				// let's fail, we cannot risk that this unexpected state goes unnoticed
				if ( isLoopMode ) ExceptionUtils.throwEx 
				( 
					IllegalStateException.class, 
					"Loop mode is on over a non-loop node: %s (%d)",
					state.getValidConceptClass ().getId (),
					this.stateIndex.inverse ().get ( state )
				); 
				nextLoopMode = false;
			}
			
			final String partialQueryFinal = partialQuery; // Just because lambdas want final vars
			
			// Loops don't contribute to the computation of the max path length, since their transition might
			// be skipped. Else, we must include the transition and the target in the new distance.
			final int nextDistance = nextLoopMode ? distance : distance + 2;

			// We want them in a given order, it helps with generating multiple versions, which need to be committed
			// into git.
			List<Map.Entry<Pair<State, Integer>, List<Transition>>> sortedByLenTrns = 
				this.sortTransitionGroups ( byLenTrns );
			
			sortedByLenTrns.forEach ( e -> 
			{
				Pair<State, Integer> grp = e.getKey ();
				List<Transition> trnsGroup = e.getValue ();
				
				int maxTrnsRepeats = grp.getRight ();
				State target = grp.getLeft ();

				// Path constraint that cannot be fulfilled, so let's skip it
				if ( maxTrnsRepeats <= 0 ) {
					BiMap<State, Integer> states2Indexes = this.stateIndex.inverse ();
					log.warn ( 
						"The transition {}({}) - [{}] -> {}({}) that doesn't match its length constraints",
						state.getValidConceptClass ().getId (),
						states2Indexes.get ( state ),
						trnsGroup.iterator ().next ().getValidRelationType ().getId (),
						target.getValidConceptClass ().getId (),
						states2Indexes.get ( target )						
					);
					return;
				}
					
				String relMatch = buildRelMatch ( trnsGroup, maxTrnsRepeats );
				String nextPartialQuery = partialQueryFinal + "\n  " + relMatch;
						
				// Yeah! Let's recurse!
				traverseStateMachine ( target, nextPartialQuery, result, nextDistance, nextLoopMode );
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

	/** 
	 * Builds a Cypher construct like: (protein_12:Protein) that matches the given state.
	 *  
	 * Nodes in a loop participates in matches of type protein_12 -> protein_12b, so the 'b'
	 * tail is added when the isLoopMode flag is set.
	 * 
	 */
	private String buildNodeMatch ( State s, boolean isLoopMode )
	{		
		// eg, (gene_1:Gene). The number is the node index in the original SM.
		String nodeMatchStr = buildNodeId ( s );
		if ( isLoopMode ) nodeMatchStr += "b";
		nodeMatchStr += ":" + s.getValidConceptClass ().getId ();
		
		if ( stateMachine.getStart ().equals ( s ) ) {
			// for the first state, the form is (gene_1:Gene{ iri: $startIri}), where the placeholder
			// is instantiated by the graph traverser.
			nodeMatchStr += "{ iri: $startIri }";
		}
				
		return "(" + nodeMatchStr + ")";
	}
	
	
	/** 
	 * Builds a Cypher clause like: - [gene_1_protein_2:encode] ->, which matches a transition.
	 */
	private String buildRelMatch ( List<Transition> transitions, int maxRelRepeats )
	{
		// It all begins with something like rel_xxx:
		String relVarId = buildRelId ( transitions );

		// Then it becomes rel_xxx:R or rel_xxx:R1|R2...Rn
		//
		
		// R1|R2 etc are written in order, useful for tests that compare resulting strings
		Collections.sort ( transitions, transitionComparator );

		String relTypesStr = null;
		for ( Transition t: transitions )
		{
			if ( relTypesStr == null ) relTypesStr = "";
			else relTypesStr += "|";
			relTypesStr += t.getValidRelationType ().getId ();
		}
		
		
		// Now let's consider a possible path length constraint.
		// 
		
		// min len isn't suppoted by the SM syntax, but 0 is needed for loop transitions
		int minLenConstraint = 1;
		
		Transition anyTrans = transitions.iterator ().next ();
		State src = this.stateMachine.getTransitionSource ( anyTrans );
		State dst = this.stateMachine.getTransitionTarget ( anyTrans );

		if ( src.equals ( dst ) ) minLenConstraint = 0; 
		
		// min    max    	Cypher format
		//  1      -      r:R
		//  1      n      r:R*1..n (but R only if n == 1)
		//  0      -      r:R*0..
		//  0      n      r:R*0..n
		String lenConstrStr = "";
		if ( minLenConstraint == 0 || maxRelRepeats != Integer.MAX_VALUE )
			lenConstrStr += "*" + minLenConstraint + "..";
		if ( maxRelRepeats != Integer.MAX_VALUE ) lenConstrStr += maxRelRepeats;
		if ( lenConstrStr.length () == 0 || "*1..1".equals ( lenConstrStr ) )
			lenConstrStr = "";
		
		if ( maxRelRepeats != Integer.MAX_VALUE && maxRelRepeats > 1 )
			// relation variable becomes rel_n1_n2_repeats
			// that's because we might have different relations between the same nodes, with different repeats and
			// they must have independent variables
			relVarId += "_" + maxRelRepeats;
			
		String relMatchStr = relVarId + ":" + relTypesStr + lenConstrStr;		

		return "- [" + relMatchStr + "] -> ";
	}
	
	
	/** 
	 * Provides a suitable node variable name for a Cypher node match, eg. gene_1 from index 1 and node of type Gene
	 */
	private String buildNodeId ( State s )
	{
		int idx = this.stateIndex.inverse ().get ( s );
		String cc = s.getValidConceptClass ().getId ();
				
		return StringUtils.uncapitalize ( cc ) + "_" + idx;
	}
	
	/**
	 * Provides a suitable node variable name for Cypher relation match, eg. encode_1_2 (numbers are the node indexes in
	 * the semantic motif file).
	 * 
	 */
	private String buildRelId ( List<Transition> transitions )
	{
		Transition t = transitions.iterator ().next ();
		State src = stateMachine.getTransitionSource ( t );
		State dst = stateMachine.getTransitionTarget ( t );
	
		BiMap<State, Integer> stateIds = this.stateIndex.inverse ();
		int srcIdx = stateIds.get ( src );
		int dstIdx = stateIds.get ( dst );
		
		// We use the relation name when there is only 1, else, a generic prefix
		String prefix = transitions.size () == 1 ? t.getValidRelationType ().getId ().toLowerCase () : "rel";
	
		return prefix + "_" + srcIdx + "_" + dstIdx;
	}
	
	
	/**
	 * Translates path length constraints for a transition in the SM (which are based on the distance from the initial 
	 * node) into max relation repeats, which are used for Cypher relation clauses.
	 * 
	 * Should this return &<= 0 values, you should consider the constraint as not matchable. 
	 */
	private int getMaxRelRepeats ( Transition t, int distance )
	{
		// In the SM files length constraints specified in terms of total path length up to the current transition (where
		// first node has distance = 1, nodes and edges count as 1 each), so we need to convert this constraints to no. of
		// max repetitions allowed for the current transition/relation
		//
		int maxRelRepeats;

		int len = t.getMaxLength ();
		if ( len == Integer.MAX_VALUE ) return Integer.MAX_VALUE; // No value marker 

		// In the computation of the the path length, both nodes and edges count 1 each, so
		// a given distance is made up of node+relation pairs
		maxRelRepeats =  (int) Math.ceil ( (len - distance) / 2d );

		// This can be <= 0 and you should consider this case;
		return maxRelRepeats;
	}
	
	/**
	 * Sorts transitions grouped by departing state and max transition repeats. These are computed by
	 * {@link #traverseStateMachine(State, String, Map, int, boolean)} and we order them, in order to ensure
	 * some determinism in the creation of numbered query names and the paths associated to them. 
	 * 
	 */
	private List<Map.Entry<Pair<State, Integer>, List<Transition>>> sortTransitionGroups (
		Map<Pair<State, Integer>, List<Transition>> byLenTrns
	)
	{
		List<Map.Entry<Pair<State, Integer>, List<Transition>>> sortedByLenTrns = 
			new ArrayList<> ( byLenTrns.entrySet () );
		
		Collections.sort 
		(
			sortedByLenTrns, 
			(e1, e2) -> 
			{
				Pair<State, Integer> k1 = e1.getKey (), k2 = e2.getKey ();
				
				// Compare the keys
				//
				String c1 = k1.getLeft ().getValidConceptClass ().getId ();
				String c2 = k2.getLeft ().getValidConceptClass ().getId ();
				int cmp = c1.compareTo ( c2 ); if ( cmp != 0 ) return cmp;

				if ( ( cmp = Integer.compare ( k1.getRight (), k2.getRight () ) ) != 0 ) return cmp;

				// If they're not enough, use the transitions too
				//
				List<Transition> trnsGrp1 = e1.getValue (), trnsGrp2 = e2.getValue ();
									
				Collections.sort ( trnsGrp1, transitionComparator );
				Collections.sort ( trnsGrp2, transitionComparator );
				
				// It's not efficient to not stopping at the first that don't match, but they're not very long, 
				// so this approach is simpler.
				//
				String trnsStr1 = e1.getValue ().stream ()
					.map ( t -> t.getValidRelationType ().getId () )
					.collect ( Collectors.joining () );
				
				String trnsStr2 = e1.getValue ().stream ()
					.map ( t -> t.getValidRelationType ().getId () )
					.collect ( Collectors.joining () );
				
				return trnsStr1.compareTo ( trnsStr2 );
			}	
		);
		return sortedByLenTrns;
	}
}
