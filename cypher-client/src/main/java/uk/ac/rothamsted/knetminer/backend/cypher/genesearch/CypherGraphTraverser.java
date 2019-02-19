package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import net.sourceforge.ondex.algorithm.graphquery.AbstractGraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.FilterPaths;
import net.sourceforge.ondex.algorithm.graphquery.GraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.State;
import net.sourceforge.ondex.algorithm.graphquery.Transition;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode.EvidenceConceptNode;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode.EvidenceRelationNode;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode.FirstEvidenceConceptNode;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.searchable.LuceneEnv;
import net.sourceforge.ondex.core.util.ONDEXGraphUtils;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClientProvider;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Jan 2019</dd></dl>
 *
 */
public class CypherGraphTraverser extends AbstractGraphTraverser
{
	public static final String CONFIG_PATH_OPT = "knetminer.backend.configPath";
	
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private static AbstractApplicationContext springContext;
	
	public CypherGraphTraverser ()
	{
	}

	private void init ()
	{
		synchronized ( GraphTraverser.class )
		{
			if ( springContext != null ) return;
			
			String cfgPath = this.getOption ( CONFIG_PATH_OPT, "backend/config.xml" );
			File cfgFile = new File ( cfgPath );
			
			if ( !cfgFile.exists () ) ExceptionUtils.throwEx ( 
				UncheckedFileNotFoundException.class,
				"Backend configuration file '%s' not found, please set %s correctly",
				cfgFile.getAbsolutePath (),
				CONFIG_PATH_OPT
			);
			
			String furl = "file://" + cfgFile.getAbsolutePath ();
			log.info ( "Configuring {} from <{}>", this.getClass ().getCanonicalName (), furl );
			springContext = new FileSystemXmlApplicationContext ( furl );
			springContext.registerShutdownHook ();
			log.info ( "{} configured", this.getClass ().getCanonicalName () );
		}		
	}
	
	
	@Override
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
	public List<EvidencePathNode> traverseGraph ( 
		ONDEXGraph graph, ONDEXConcept concept, FilterPaths<EvidencePathNode> filter
	)
	{
		init ();
		
		LuceneEnv luceneMgr = this.getOption ( "LuceneEnv" );
  	if ( luceneMgr == null ) throw new IllegalArgumentException (
  			"Cannot initialise the Cypher resourceResource traverser: "
  		+ "you must pass me the LuceneEnv option (see OndexServiceProvider)"
  	);
 		
		List<String> cypherQueries = (List<String>) springContext.getBean ( "semanticMotifsQueries" );
		CypherClientProvider cyProvider = springContext.getBean ( CypherClientProvider.class );
		
		String startIri = Optional
			.ofNullable ( ONDEXGraphUtils.getAttribute ( graph, concept, "iri" ) )
			.map ( attr -> (String) attr.getValue () )
			.orElseThrow ( () -> ExceptionUtils.buildEx (
				IllegalStateException.class, 
				"No attribute 'iri' defined for the concept %s, Cypher backend needs OXL files with IRI/URI attributes", 
				ONDEXGraphUtils.getString ( concept ) 
		));
				
		Value startIriParam = Values.parameters ( "startIri", startIri );

		List<EvidencePathNode> result = cypherQueries
		.parallelStream ()
		.flatMap ( query -> 
		{
			// For each configured semantic motif query, get the paths from Neo4j + indexed resource
			Stream<List<ONDEXEntity>> cypaths = cyProvider.query (
				cyClient -> cyClient.findPaths ( luceneMgr, query, startIriParam ) 
			);
			// Now map the paths to the format required by the traverser
			// We're returning a stream of paths, (each query can return more than one)
			return cypaths.map ( path -> buildEvidencePath ( path ) );
		})
		.collect ( Collectors.toList () ); // And eventually we extract List<EvPathNode>
				
		// This is an optional method to filter out unwanted results. In Knetminer it's usually null
		if ( filter != null ) result = filter.filterPaths ( result );
		return result;
	}

	
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
	private EvidencePathNode buildEvidencePath ( List<ONDEXEntity> ondexEntities )
	{
		EvidencePathNode result = null; 
		for ( ONDEXEntity odxEnt: ondexEntities ) 
		{ 
			if ( odxEnt == null ) throw buildEx ( 
				IllegalStateException.class, 
				"Internal error: Cypher Graph Traverser got a null ONDEX entity from Cypher"
			);

			if ( odxEnt instanceof ONDEXConcept )
			{
				ONDEXConcept concept = (ONDEXConcept) odxEnt;
				State evidence = new State ( concept.getOfType () );
				result = result == null 
					? new FirstEvidenceConceptNode ( concept, evidence )
					: new EvidenceConceptNode ( concept, evidence, result );	
			}
			else if ( odxEnt instanceof ONDEXRelation )
			{
				ONDEXRelation rel = (ONDEXRelation) odxEnt;
				Transition evidence = new Transition ( rel.getOfType () );
				result = new EvidenceRelationNode ( rel, evidence, result );
			}
			else throwEx ( 
				IllegalStateException.class, 
				"Internal error: Cypher Graph Traverser cannot deal with instances of %s",
				odxEnt.getClass ().getCanonicalName () 
			);
		}
		if ( result == null ) throwEx ( 
			IllegalStateException.class, 
			"Internal error: Cypher Graph Traverser got a null result from entity->evidence-path conversion"
		);
		return result;
	}
}
