package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static java.lang.String.format;
import static net.sourceforge.ondex.core.util.ONDEXGraphUtils.getString;
import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.Function3;
import net.sourceforge.ondex.algorithm.graphquery.AbstractGraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.StateMachineComponent;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode;
import net.sourceforge.ondex.core.MetaData;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.util.GraphMemIndex;
import net.sourceforge.ondex.core.util.ONDEXGraphUtils;
import uk.ac.rothamsted.knetminer.backend.cypher.TestGraphResource;

/**
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Jan 2019</dd></dl>
 *
 */
public class CypherGraphTraverserIT
{
	private static AbstractGraphTraverser graphTraverser;
	
	@ClassRule
	public static TestGraphResource graphResource = new TestGraphResource ();

	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	@BeforeClass
	public static void initTraverser ()
	{
		Map<String, Object> options = new HashMap<> ();
		options.put ( "GraphTraverserClass", CypherGraphTraverser.class.getName () );
		options.put ( CypherGraphTraverser.CFGOPT_PATH, "target/test-classes/test-config/config.xml" );
		graphTraverser = AbstractGraphTraverser.getInstance ( options );
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
		
		List<EvidencePathNode> paths = graphTraverser.traverseGraph ( graph, startConcept, null );
		assertTrue ( "No EvidencePath returned!", paths.size () > 0 );
		
		log.info ( "======== Results from traverseGraph() =======" );
		logPaths ( paths );

		
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
		assertTrue ( 
			"Expected result not found (Evidence Path)!",
			checkPathEntities ( paths,
				"R{h_s_s", 3,
				"C{Protein:P13027", 4,
				"C{Publication", 6
		));
		
		// ------ And also check the StateMachine components that were attached to the path
		// (I've no idea if they're useful in the current Knetminer, but I'm returning them to be safe)
		//
		assertTrue ( 
			"Expected results not found (Evidences in Evidence Path)!",
			checkPathEvidences ( paths,
				Transition.class, "h_s_s", 3, 
				State.class, "Protein", 4,
				State.class, "Publication", 6 
		));
	}
	
	
	@Test
	@SuppressWarnings ( "rawtypes" )
	public void testMultipleStartConcepts ()
	{
		Stream<String> conceptIris = Stream.of ( 
			iri ( "bkr:gene_at4g26080_locus_2005488"	),
			iri ( "bkr:gene_at5g35550_locus_2169538"	),
			iri ( "bkr:gene_at2g40220_locus_2005490" ),
			iri ( "bkr:gene_at1g63650_locus_2026629" )
		);
				
		ONDEXGraph graph = graphResource.getGraph ();

		GraphMemIndex memIdx = GraphMemIndex.getInstance ( graph );
		List<ONDEXConcept> startConcepts = conceptIris.map ( iri -> {
			ONDEXConcept c = memIdx.get ( "iri", iri );
			if ( c == null ) throwEx (
				IllegalArgumentException.class,
				"Concept <%s> not found!", 
				iri 
			);
			return c;
		})
		.collect ( Collectors.toList () );
		
		Map<ONDEXConcept, List<EvidencePathNode>> pathsMap = 
			graphTraverser.traverseGraph ( graph, new HashSet<> ( startConcepts ), null );
		
		assertNotNull ( "No result from traverseGraph(<set>) (null)", pathsMap );
		logResultPaths ( pathsMap );
		
		assertTrue ( "No result from traverseGraph(<set>) (size)", pathsMap.size () > 0 );
		assertEquals ( "traverseGraph(<set>) wrong size returned!", startConcepts.size (), pathsMap.keySet ().size () );
		
		pathsMap.forEach ( (concept, paths) -> {
			log.info ( "----- PATHS FOR {} ----", ONDEXGraphUtils.getString ( concept ) );
			logPaths ( paths );
		});
		
		// ---- 2nd start concept
		//
		ONDEXConcept probeConcept = startConcepts.get ( 1 );
		List<EvidencePathNode> probePaths = pathsMap.get ( probeConcept );
		
		assertTrue ( 
			"Test path for " + probeConcept.getPID () + " not found!", 
			checkPathEvidences ( probePaths, 
				State.class, "Gene", 0,
				Transition.class, "cooc_wi", 1,
				State.class, "TO", 2,
				Transition.class, "cooc_wi", 3,
				State.class, "Gene", 4,
				Transition.class, "participates_in", 5,
				State.class, "BioProc", 6
			)
		);
		
		
		// ---- 4th start concept (like testTraverseGraph())
		//
		probeConcept = startConcepts.get ( 3 );
		probePaths = pathsMap.get ( probeConcept );
		
		assertTrue ( 
			"Test path for " + probeConcept.getPID () + " not found!", 
			checkPathEvidences ( probePaths, 
				Transition.class, "h_s_s", 3, 
				State.class, "Protein", 4,
				State.class, "Publication", 6 
			)
		);		
	}
	
	/**
	 * Tests some additions we make to the base OXL.
	 */
	@Test
	@SuppressWarnings ( "rawtypes" )
	public void testProbeGene ()
	{
		ONDEXGraph graph = graphResource.getGraph ();
		
		ONDEXConcept startConcept = graph.getConcepts ()
			.parallelStream ()
			.filter ( c -> "TEST-GENE-01".equals ( c.getPID () ) )
			.findAny ()
			.orElseThrow ( () -> new IllegalStateException ( "Couldn't find the test start concept" ) );
		
		List<EvidencePathNode> paths = graphTraverser.traverseGraph ( graph, startConcept, null );

		assertTrue ( "No EvidencePath returned!", paths.size () > 0 );
		
		
		log.info ( "======== Results from traverseGraph() =======" );
		logPaths ( paths );
		
		
		// ------ Let's see if we have a probe path
		//		
		assertTrue ( 
			"Expected result not found (Evidence Path)!",
			checkPathEntities ( paths,
				"C{Gene:TEST-GENE-01", 0,
				"R{has_test_relation", 1,
				"C{TestCC:TEST-ENT-01", 2
		));		
	}
	
	
	@Test
	@SuppressWarnings ( "rawtypes" )
	public void testInterruption () throws InterruptedException, ExecutionException
	{
		Stream<String> conceptIris = Stream.of ( 
			iri ( "bkr:gene_at4g26080_locus_2005488"	),
			iri ( "bkr:gene_at5g35550_locus_2169538"	),
			iri ( "bkr:gene_at2g40220_locus_2005490" ),
			iri ( "bkr:gene_at1g63650_locus_2026629" )
		);
				
		ONDEXGraph graph = graphResource.getGraph ();

		GraphMemIndex memIdx = GraphMemIndex.getInstance ( graph );
		List<ONDEXConcept> startConcepts = conceptIris.map ( iri -> {
			ONDEXConcept c = memIdx.get ( "iri", iri );
			if ( c == null ) throwEx (
				IllegalArgumentException.class,
				"Concept <%s> not found!", 
				iri 
			);
			return c;
		})
		.collect ( Collectors.toList () );

		CypherGraphTraverser cytraverser = (CypherGraphTraverser) graphTraverser;
		
		// We need some slow query to keep it busy while we interrupt it.
		List<String> queries = new ArrayList<> ( cytraverser.getSemanticMotifsQueries () );
		queries.add (
			"MATCH path = (g:Gene) -[r*1..2]-> (c:Concept)\n"
			+ "WHERE NONE ( rr IN relationships(path) WHERE TYPE(rr) = 'relatedConcept' )\n"
			+ "  AND g.iri IN $startGeneIris\n"
			+ "RETURN path"
		);
		cytraverser.setSemanticMotifsQueries ( queries );
		
		ExecutorService executor = Executors.newSingleThreadExecutor ();
		Future<Map<ONDEXConcept, List<EvidencePathNode>>> pathsMapFuture
			= executor.submit ( () -> cytraverser.traverseGraph ( graph, new HashSet<> ( startConcepts ), null ) );
		Thread.sleep ( 500 ); // It needs sometime to startup and set interrupted flag to false 
		cytraverser.interrupt ();
		assertTrue ( "Interruption flag isn't set!", cytraverser.isInterrupted () );
		assertEquals ( "Interruption didn't work!", 0, pathsMapFuture.get ().size () );
	}
	
	
	/**
	 * Checks that paths contains a path with a given set of ondex entity constraints.
	 * 
	 * @param testedParams is a list of Ondex entity restrictions, having the form:
	 * `<String, Integer>*`, i.e., each pair tests that at a given position index (2nd element)
	 *  we have a concept or relation returning a given label (1st parameter). The latter must match
	 *  {@link ONDEXGraphUtils#getString(ONDEXEntity)}.
	 */
	@SuppressWarnings ( "rawtypes" )
	private boolean checkPathEntities ( List<EvidencePathNode> paths, Object... testedParams )
	{
		return paths.stream ().anyMatch ( path -> 
		{ 
			BiFunction<String, Integer, Boolean> oeChecker = ( label, idx ) -> 
			ONDEXGraphUtils.getString ( path.getEntityAtPosition ( idx ) ).startsWith ( label );
			
			for ( int i = 0; i < testedParams.length; )
				if ( !oeChecker.apply ( (String) testedParams [ i++ ], (int) testedParams [ i++ ] ) ) return false;
			
			return true;			
		});		
	}
	
	
	/**
	 * Checks that paths contains a path with a given set of evidence constraints.
	 * 
	 * @param testedParams is a list of evidence restrictions, having the form:
	 * `<StateMachineComponent, String, Integer>*`, i.e., each triple tests that at a given position index (3rd element)
	 *  we have a given {@link StateMachineComponent} (1st element), a concept or relation of a given type (2nd element).
	 *  
	 */
	@SuppressWarnings ( { "unchecked", "rawtypes" } )
	private boolean checkPathEvidences ( List<EvidencePathNode> paths, Object... testedParams )
	{
		return paths.stream ()
		.map ( EvidencePathNode::getEvidencesInPositionOrder )
		.anyMatch ( evidences -> 
		{ 
			// Little helper to check if evidence testedIdx is instance of exptEvidence and has concept/relation type named
			// exptEvType
			//
			Function3<Class<? extends StateMachineComponent>, String, Integer, Boolean> evidenceChecker =
			( exptEvidence, exptEvType, testedIdx ) -> 
			{
				StateMachineComponent ev = (StateMachineComponent) evidences.get ( testedIdx );
				
				if ( !exptEvidence.isInstance ( ev ) ) return false;
				
				MetaData evType = ev instanceof State 
					? ((State) ev).getValidConceptClass ()
					: ((Transition) ev).getValidRelationType ();
				
				return exptEvType.equals ( evType.getId () );
			};
			
			// A path is valid if all the probed positions are as expected.
			for ( int i = 0; i < testedParams.length; )
				if ( !evidenceChecker.apply (
				  (Class<Transition>) testedParams [ i++ ],
				  (String) testedParams [ i++ ],
				  (int) testedParams [ i++ ]				  
				)) return false;
			
			return true;
		});		
	}
	
	/**
	 * Logs info about paths, showing all the steps and type of {@link StateMachineComponent} in all the paths. 
	 */
	@SuppressWarnings ( { "rawtypes" } )
	private void logPaths ( List<EvidencePathNode> paths )
	{
		int i = 0;
		for ( EvidencePathNode pathNode: paths )
		{
			for ( ; pathNode != null; pathNode = pathNode.getPrevious () )
			{
				ONDEXEntity odxe = pathNode.getEntity ();
				StateMachineComponent ev = pathNode.getStateMachineComponent ();
				
				String odxLabel = odxe instanceof ONDEXConcept 
					? getString ( odxe )
					: "{" + ((ONDEXRelation) odxe).getOfType ().getId () + "}";
					
				String evType = ev instanceof State 
					? ( (State) ev ).getValidConceptClass ().getId ()
					: ( (Transition) ev).getValidRelationType ().getId ();
				
				log.info ( format ( 
					"\t#%d: [ID:%s SM:%s/%s]", 
					i, odxLabel, ev.getClass ().getSimpleName (), evType 
				));
			}
			i++;
		}
	}
	
	
	private void logResultPaths ( Map<ONDEXConcept, List<EvidencePathNode>> pathsMap )
	{
		pathsMap.forEach ( (gene, paths) ->
			paths.forEach ( 
				path -> log.info ( "FOUND PATH: {} => [{}]", getString ( gene ), join ( path, ", " ) )
			)
		);
		
	}
}
