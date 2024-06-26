package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
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
		this.initSpring ();
		
		// Sometimes this is set via options for debugging purposes 
		Integer reportFrequency = this.getOption ( "performanceReportFrequency" );
		if ( reportFrequency != null )
		{
			log.debug ( "Overriding performanceReportFrequency with the value {}", reportFrequency );
			springContext
				.getBean ( CyTraverserPerformanceTracker.class )
				.setReportFrequency ( reportFrequency );
		}
	}
	
	private void initSpring ()
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
	 * Uses queries in the {@link #springContext} bean named {@code semanticMotifsQueries}, which can be overridden with
	 * {@link #setSemanticMotifsQueries(List)}. Each query must fulfil certain requirements:
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
	 * <p>The implementation of this method is based on {@link PathQueryProcessor}.</p>
	 */	
	@Override
	@SuppressWarnings ( { "rawtypes", "static-access" } )
	public Map<ONDEXConcept, List<EvidencePathNode>> traverseGraph ( 
		ONDEXGraph graph, Set<ONDEXConcept> concepts, FilterPaths<EvidencePathNode> filter )
	{
		init ();

		this.removeDuplicatedQueries ();
		
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
	 * 
	 * <b>WARNING</b>: duplicated queries are removed by the call to {@link #traverseGraph(ONDEXGraph, Set, FilterPaths)},
	 * @see #removeDuplicatedQueries()
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
	 * This is invoked by {@link #traverseGraph(ONDEXGraph, Set, FilterPaths)}, to check that there aren't duplicated
	 * queries in {@link #getSemanticMotifsQueries()}. The possible new query list is propagated to all components needing
	 * it via {@link #setSemanticMotifsQueries(List)}. 
	 * 
	 */
	private void removeDuplicatedQueries ()
	{
		// The new list isn't created from existing because we want to preserve the original order
		//
		List<String> newQueries = new ArrayList<> (),
								 currentQueries = this.getSemanticMotifsQueries ();
		Set<String> existing = new HashSet<> ();
		
		for ( String q: currentQueries )
		{
			if ( existing.contains ( q ) ) {
				log.warn ( "Removing duplicated traverser query: {}", q );
				continue; 
			}
			existing.add ( q );
			newQueries.add ( q );
		}
		
		if ( newQueries.size () == currentQueries.size () ) return;
		
		log.warn ( "Some duplicated traverser queries were removed, see log messages above" );
		this.setSemanticMotifsQueries ( newQueries );
	}
	
	/**
	 * Wrapper of {@link PathQueryProcessor#getPercentProgress()}.
	 */
	public double getPercentProgress ()
	{
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		return qp.getPercentProgress ();
	}
	
	/**
	 * @see #interrupt()
	 */
	public boolean isInterrupted ()
	{
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		return qp.isInterrupted ();
	}
	
	/**
	 * <p>This and isInterrupted() allows for stopping the traversal, a feature used in Knetminer {@code CypherDebugger} 
	 * component.</p>
	 * 
	 * <p>They are wrappers of {@link PathQueryProcessor#interrupt()} and {@link PathQueryProcessor#isInterrupted()}.</p>
	 * 
	 * <p>There is an integration test about this.</p>
	 */
	public void interrupt ()
	{
		PathQueryProcessor qp = springContext.getBean ( PathQueryProcessor.class );
		qp.interrupt ();
	}
	
	/**
	 * The Neo4j driver that comes from the traverser configuration. This is useful for 
	 * certain command line tools, such as the KnetMiner initialiser.
	 */
	public Driver getNeo4jDriver ()
	{
		init ();
		
		var driver = springContext.getBean ( "neoDriver", Driver.class );
		return driver;
	}
}
