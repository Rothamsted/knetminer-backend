package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.ondex.algorithm.graphquery.AbstractGraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.StateMachineComponent;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode;
import net.sourceforge.ondex.core.MetaData;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.util.ONDEXGraphUtils;
import uk.ac.rothamsted.knetminer.backend.cypher.TestGraphResource;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Jan 2019</dd></dl>
 *
 */
public class CypherGraphTraverserIT
{
	private static AbstractGraphTraverser graphTraverser = new CypherGraphTraverser ();
	
	@ClassRule
	public static TestGraphResource graphResource = new TestGraphResource ();

	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	@BeforeClass
	public static void initTraverser ()
	{
		graphTraverser.setOption ( CypherGraphTraverser.CONFIG_PATH_OPT, "target/test-classes/test-config/config.xml" );
		graphTraverser.setOption ( "LuceneEnv", graphResource.getLuceneMgr () );
	}
			
	@Test
	@SuppressWarnings ( { "rawtypes" } )
	public void testTraverseGraph ()
	{
		ONDEXGraph graph = graphResource.getGraph ();
		
		ONDEXConcept startConcept = graph.getConcepts ()
			.parallelStream ()
			.filter ( c -> "AT1G63650;locus:2026629".equals ( c.getPID () ) )
			.findAny ()
			.orElseThrow ( () -> new IllegalStateException ( "Couldn't find the test start concept" ) );
		
		List<EvidencePathNode> paths = graphTraverser.traverseGraph ( graphResource.getGraph (), startConcept, null );

		assertTrue ( "No EvidencePath returned!", paths.size () > 0 );
		
		
		log.info ( "======== Results from traverseGraph() =======" );
		int i = 0;
		for ( EvidencePathNode path: paths )
			log.info ( "\t#{}: [{}]", i++, path.toString () );

		
		// ------ First, let's check if they start with the right concept
		//
		boolean haveRightStart = paths.parallelStream ()
			.map ( path -> (ONDEXConcept) path.getEntityAtPosition ( 0 ) )
			.allMatch ( c -> 
			{
				if ( ! startConcept.getPID ().equals ( c.getPID () ) ) {
					log.error ( "Unexpected start concept {} in the traverser result!", c.getPID () );
					return false;
				}
				return true;
			});
		
		assertTrue ( "Some paths have a bad start concept!", haveRightStart );
		
		
		// ------ Now, let's see if we have a probe path
		//		
		assertTrue
		( 
			"Expected result not found (Evidence Path)!",
			
			paths.parallelStream ().anyMatch ( path -> 
			{ 
				BiFunction<String, Integer, Boolean> oeChecker = ( label, idx ) -> 
				ONDEXGraphUtils.getString ( path.getEntityAtPosition ( idx ) ).startsWith ( label );
				
				return oeChecker.apply ( "R{h_s_s", 3 )
						&& oeChecker.apply ( "C{Protein:P13027", 4 )
						&& oeChecker.apply ( "C{Publication", 6 );
			})
		);
		
		// ------ And also check the StateMachine components that were attached to the path
		// (I've no idea if they're useful in the current Knetminer, but I'm returning them to be safe)
		//
		assertTrue 
		( 
			"Expected results not found (Evidences in Evidence Path)!",
			
			paths.parallelStream ()
			.map ( EvidencePathNode::getEvidencesInPositionOrder )
			.anyMatch ( evidences -> 
			{ 
				// Little helper to check if evidence idx is instance of smPair[0] and has concept/relation type named
				// smPair[1] (we need a pair since there is no 'tri-function' in Java).
				//
				BiFunction<Pair<Class<? extends StateMachineComponent>, String>, Integer, Boolean> evidenceChecker =
				( smPair, idx ) -> 
				{
					StateMachineComponent ev = (StateMachineComponent) evidences.get ( idx );
					
					if ( !smPair.getLeft ().isInstance ( ev ) ) return false;
					
					MetaData evType = ev instanceof State 
						? ((State) ev).getValidConceptClass ()
						: ((Transition) ev).getValidRelationType ();
					
					return smPair.getRight ().equals ( evType.getId () );
				};
				
				return evidenceChecker.apply ( Pair.of ( Transition.class, "h_s_s" ), 3 )
					&& evidenceChecker.apply ( Pair.of ( State.class, "Protein" ), 4 )
					&& evidenceChecker.apply ( Pair.of ( State.class, "Publication" ), 6 );
			})
		);
	}
}
