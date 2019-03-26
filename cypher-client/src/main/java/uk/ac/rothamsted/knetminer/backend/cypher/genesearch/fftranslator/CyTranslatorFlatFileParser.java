package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.exceptions.InvalidFileException;
import net.sourceforge.ondex.algorithm.graphquery.exceptions.StateMachineInvalidException;
import net.sourceforge.ondex.algorithm.graphquery.flatfile.StateMachineFlatFileParser2;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>25 Mar 2019</dd></dl>
 *
 */
public class CyTranslatorFlatFileParser extends StateMachineFlatFileParser2
{
  private BiMap<Integer, State> stateIndex;

  public CyTranslatorFlatFileParser ()
	{
		super ();
	}

	@Override
	public void initalizeStateMachine ( 
		Map<String, Transition> transitionRts, Map<Integer, State> statesConceptClass,
		State startingState, Set<State> finishingStates 
	) throws InvalidFileException, StateMachineInvalidException
	{
		super.initalizeStateMachine ( transitionRts, statesConceptClass, startingState, finishingStates );
		
		stateIndex = HashBiMap.create ();
		statesConceptClass.forEach ( (id, state) -> stateIndex.put ( Integer.valueOf ( id ), state ) );
	}

	public BiMap<Integer, State> getStateIndex () {
		return stateIndex;
	}

}
