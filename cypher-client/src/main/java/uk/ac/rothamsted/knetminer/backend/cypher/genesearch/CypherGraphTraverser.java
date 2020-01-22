package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.neo4j.driver.v1.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import net.sourceforge.ondex.algorithm.graphquery.AbstractGraphTraverser;
import net.sourceforge.ondex.algorithm.graphquery.FilterPaths;
import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.helpers.CyTraverserPerformanceTracker;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.helpers.PathQueryProcessor;

/**
 * <p>A {@link AbstractGraphTraverser graph traverser} based on Cypher queries against a property graph database
 * storing a BioKNO-based model of an Ondex/Knetminer graph. Currently the backend datbase is based on Neo4j.</p>
 * 
 * <p>This traverser expects the following in {@link #getOptions()}:
 * <ul>
 * 	<li>{@link #CFGOPT_PATH} set to a proper Spring config file. Many other options are
 *  defined in this file, see <a href = "https://tinyurl.com/tt5qe96">our test config</a> for details.</li>
 * </ul>
 * 
 * </p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Jan 2019</dd></dl>
 *
 */
public class CypherGraphTraverser extends AbstractGraphTraverser
{
	/**
	 * <p>We use a Spring-based configuration file to read Neo4j configuration, which also has to declare the list or the
	 * folder where .cypher query files are stored.</p> 
	 * 
	 * <p>Each query select a path departing from a gene passed as parameter, as it is specified by {@link CypherClient}.</p>
	 * 
	 * <p>See tests for examples of the kind of file expected here. Default value for this option is {@code "backend/config.xml"}.</p>
	 * 
	 */
	public static final String CFGOPT_PATH = "knetminer.backend.configPath";

	private static AbstractApplicationContext springContext;
			
	private final Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	public CypherGraphTraverser () {
	}
	
	private void init ()
	{
		// Double-check lazy init (https://www.geeksforgeeks.org/java-singleton-design-pattern-practices-examples/)
		if ( springContext != null ) return;
		
		synchronized ( CypherGraphTraverser.class )
		{
			if ( springContext != null ) return;
			
			String cfgPath = this.getOption ( CFGOPT_PATH, "backend/config.xml" );
			File cfgFile = new File ( cfgPath );
			
			if ( !cfgFile.exists () ) ExceptionUtils.throwEx ( 
				UncheckedFileNotFoundException.class,
				"Backend configuration file '%s' not found, please set %s correctly",
				cfgFile.getAbsolutePath (),
				CFGOPT_PATH
			);
			
			String furl = "file:///" + cfgFile.getAbsolutePath ();
			log.info ( "Configuring {} from <{}>", this.getClass ().getCanonicalName (), furl );
			springContext = new FileSystemXmlApplicationContext ( furl );
			springContext.registerShutdownHook ();
			
			// Sometimes this is set via options for debugging purposes 
			Integer reportFrequency = this.getOption ( "performanceReportFrequency" );
			if ( reportFrequency != null )
				springContext.getBean ( CyTraverserPerformanceTracker.class )
					.setReportFrequency ( reportFrequency );
			
			log.info ( "{} configured", this.getClass ().getCanonicalName () );
		}		
	}
	
	/**
	 * This is implemented as a wrapper of the
	 * {@link #traverseGraph(ONDEXGraph, Set, FilterPaths) multi-gene version}.
	 */
	@Override
	@SuppressWarnings ( { "rawtypes" } )
	public List<EvidencePathNode> traverseGraph ( 
		ONDEXGraph graph, ONDEXConcept concept, FilterPaths<EvidencePathNode> filter
	)
	{
		Map<ONDEXConcept, List<EvidencePathNode>> result = 
			this.traverseGraph ( graph, Collections.singleton ( concept ), filter );
		
		return Optional.ofNullable ( result.get ( concept ) )
			.orElse ( new ArrayList<> () );
	}

		
	/**
	 * Uses queries in the {@link #springContext} bean named {@code semanticMotifsQueries}. Each query must fulfil certain 
	 * requirement:
	 * 
	 * <ul>
	 * 	<li>The query must return a path as first projected result (see {@link CypherClient#findPathIris(String, Value)}</li>
	 * 	<li>Every returned path must match an Ondex gene concept as first node, followed by Cypher entities corresponding
	 * to Ondex concept/relation pairs</li>
	 * 	<li>The query must deal with the {@code $startGeneIris} parameter, of type list of strings, which are a set of 
	 * starting genes that the traverser wants to explore. Typically, this will be arranged this way: 
	 * {@code MATCH path = (g1:Gene) ... WHERE g1.iri IN $startGeneIris RETURN path}. See the tests in this hereby project 
	 * for details.</li>
	 *  <li>Each returned Cypher node/relation must carry an {@code iri} property, coherent with the parameter {@code graph}.</li>
	 * </ul>
	 * 
	 * The implementation of this method is based on {@link PathQueryProcessor}.
	 */	
	@Override
	@SuppressWarnings ( { "rawtypes", "static-access" } )
	public Map<ONDEXConcept, List<EvidencePathNode>> traverseGraph ( 
		ONDEXGraph graph, Set<ONDEXConcept> concepts, FilterPaths<EvidencePathNode> filter )
	{
		init ();

		log.info ( "Graph Traverser, beginning parallel traversing of {} concept(s)", concepts.size () );
				
		PathQueryProcessor queryProcessor = this.springContext.getBean ( PathQueryProcessor.class );
		Map<ONDEXConcept, List<EvidencePathNode>> result = queryProcessor.process ( graph, concepts );

		if ( this.isInterrupted () ) return new HashMap<> ();
		
		if ( filter == null ) return result;
		
		result.entrySet ()
			.parallelStream ()
			.map ( Entry::getValue )
			.forEach ( paths ->	paths.retainAll ( filter.filterPaths ( paths ) ) );
		
		return result;
	}

	/**
	 * A wrapper of {@link CyTraverserPerformanceTracker#getStats()}.
	 */
	public String getPerformanceStats ()
	{
		if ( springContext == null ) throw new IllegalStateException (
			"getPerformanceStats() cannot be invoked before init(), ignoring it at this stage" 
		);
		CyTraverserPerformanceTracker performanceTracker = springContext.getBean ( CyTraverserPerformanceTracker.class );
		return performanceTracker.getStats ();
	}
	
	/**
	 * There are components that redefine queries dynamically, out of Spring, and report the current ones, so we need 
	 * this here. This is just a wrapper for {@link PathQueryProcessor#setSemanticMotifsQueries(List)}.
	 */
	public List<String> getSemanticMotifsQueries ()
	{
		init ();
				
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		return qp.getSemanticMotifsQueries ();
	}
	
	
	/**
	 * There are components that redefine queries dynamically, out of Spring, so we need this here.
	 * This is just a wrapper for {@link PathQueryProcessor#setSemanticMotifsQueries(List)}.
	 */
	public void setSemanticMotifsQueries ( List<String> semanticMotifsQueries )
	{
		init ();
		
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		qp.setSemanticMotifsQueries ( semanticMotifsQueries );

		CyTraverserPerformanceTracker performanceTracker = springContext.getBean ( CyTraverserPerformanceTracker.class );
		performanceTracker.setSemanticMotifsQueries ( semanticMotifsQueries );
	}

	
	
	/**
	 * Wrapper of {@link PathQueryProcessor#getPercentProgress()}.
	 */
	public double getPercentProgress ()
	{
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		return qp.getPercentProgress ();
	}
	
	
	public boolean isInterrupted ()
	{
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		return qp.isInterrupted ();
	}
	
	public void interrupt ()
	{
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		qp.interrupt ();
	}	
}
