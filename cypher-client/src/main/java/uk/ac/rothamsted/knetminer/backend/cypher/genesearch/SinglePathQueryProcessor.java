package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import net.sourceforge.ondex.algorithm.graphquery.AbstractGraphTraverser;
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
import net.sourceforge.ondex.core.util.ONDEXGraphUtils;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.runcontrol.PercentProgressLogger;
import uk.ac.ebi.utils.threading.batchproc.processors.ListBasedBatchProcessor;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.neo4j.utils.GenericNeo4jException;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>25 Nov 2019</dd></dl>
 *
 */
@Component @Scope ( "prototype" )
public class SinglePathQueryProcessor
	extends ListBasedBatchProcessor<ONDEXConcept, Consumer<List<ONDEXConcept>>>
{	
	public static final long DEFAULT_QUERY_BATCH_SIZE = 10000;
	
	private String pathQuery;
		
	@Autowired ( required = false) @Qualifier ( "queryBatchSize" ) 
	private long queryBatchSize = DEFAULT_QUERY_BATCH_SIZE;
	
	@Autowired ( required = false ) @Qualifier ( "queryTimeout" )
	private long queryTimeout = 2500;
	
	@Autowired
	private CyTraverserPerformanceTracker cyTraverserPerformanceTracker;
	
  @Lookup // this is a prototype-scoped bean, so we need lookup+getter. 
  public PagedCyPathFinder getCyPathFinder () {
    return null;
  }	
	
  /**
   * <p>We share a single executor between all the single query path processors, in order to
   * avoid too much load created by parallel per-query processors.</p>
   * 
   * <p>This is initialised in the {@link SinglePathQueryProcessor constructor}, using (once)
   * {@link #getExecutor() the processor executor}.</p>
   * 
   * <p><b>WARNING</b>: that means that invoking
   * {@link #setExecutor(ExecutorService)} after initialisation can break this executor sharing
   * policy.</p>
   */
  private static ExecutorService SHARED_EXECUTOR;
  
	private static final TimeLimiter TIME_LIMITER = new SimpleTimeLimiter ();

	
	public SinglePathQueryProcessor ()
	{
		super ();
		
		synchronized ( SinglePathQueryProcessor.class ) {
			if ( SHARED_EXECUTOR == null ) SHARED_EXECUTOR = this.getExecutor ();
			else this.setExecutor ( SHARED_EXECUTOR );
		}
	}	

	@PostConstruct
	private void init () {
		this.getBatchCollector ().setMaxBatchSize ( this.queryBatchSize );
	}
	
		
	@SuppressWarnings ( "rawtypes" )
	public void process ( 
		ONDEXGraph graph,
		Collection<ONDEXConcept> concepts,
		Map<ONDEXConcept, List<EvidencePathNode>> result,
		PercentProgressLogger queryProgressLogger
	)
	{
		this.setBatchJob ( batch -> 
		{
			this.queryJob ( graph, batch, result ); 
			queryProgressLogger.updateWithIncrement ();
		});

		// TODO: parallelStream() might be worth here and should work, but need testing.
		// This is only about scanning the concepts in parallel or not, the queries are 
		// parallel anyway.
		super.process ( concept -> concepts.stream ().forEach ( concept ) );
	}
	
	
	@SuppressWarnings ( "rawtypes" )
	private void queryJob ( ONDEXGraph graph, List<ONDEXConcept> batch, Map<ONDEXConcept, List<EvidencePathNode>> result )
	{
		// So, let's get the starting IRIs from the concepts parameter.
		List<String> startGeneIris = batch.parallelStream ()
			.map ( concept -> Optional
				.ofNullable ( ONDEXGraphUtils.getAttribute ( graph, concept, "iri" ) )
				.map ( attr -> (String) attr.getValue () )
				.orElseThrow ( () -> ExceptionUtils.buildEx (
					IllegalStateException.class, 
					"No attribute 'iri' defined for the concept %s, Cypher backend needs OXL files with IRI/URI attributes", 
					ONDEXGraphUtils.getString ( concept )
				))
		).collect ( Collectors.toList () );
		

		// Collects IRIs resulting from the query
		final List<List<String>> queryResultIris = new ArrayList<> ();
		
		// Used below, by the cyTraverserPerformanceTracker
		int performanceCounters[] = { 0, 0 };

		// Base Cypher query action
		Runnable queryAction = () -> this.doQuery ( startGeneIris, queryResultIris, performanceCounters );

		// Don't allow it to run too long (if queryTimeout != -1)
		Runnable timedQueryAction = () -> timedQuery ( queryAction, this.queryTimeout, startGeneIris, pathQuery ); 

		
		// Wrap it further with the machinery that accumulates query performance-related stats
		// (when that's feature is disabled, it just runs the query action)
		//
		try {
			this.cyTraverserPerformanceTracker.track
			( 
				pathQuery, 
				timedQueryAction,
				() -> performanceCounters [ 0 ],
				() -> performanceCounters [ 1 ] 
			);			
		}
		catch ( UncheckedTimeoutException ex ) {
			// The query didn't complete within the timeout, results are partial, we must invalidate
			// everything
			if ( log.isTraceEnabled () )
				log.trace ( "Query timed out. First gene: <{}>, query: {}", startGeneIris.get ( 0 ), pathQuery );
			return;
		}
		
		
		// And eventually, let's collect the results
		//
		CypherClient.findPathsFromIris ( graph, queryResultIris.parallelStream () )
		.parallel ()
		.forEach ( pathEntities ->
		{
			// Do it before the following, it checks the first entity is a concept.
			EvidencePathNode path = this.buildEvidencePath ( pathEntities );
			ONDEXConcept firstGene = (ONDEXConcept) pathEntities.get ( 0 );
			
			result.computeIfAbsent ( firstGene, k -> new ArrayList<> () )
				.add ( path );
		});
	}

	
	private void doQuery ( 
		List<String> startGeneIris, List<List<String>> queryResultIris, int performanceCounters[] 
	)
	{
		PagedCyPathFinder pathsItr = this.getCyPathFinder ();
		pathsItr.init ( startGeneIris, this.pathQuery );
		
		// For each configured semantic motif query, get the paths from Neo4j + indexed resource
		pathsItr.forEachRemaining ( 
		pathIris -> {
			queryResultIris.add ( pathIris );
			performanceCounters [ 0 ]++; // no. of resulting paths
			performanceCounters [ 1 ] += pathIris.size (); // total path lengths
		});
	}
	
	
	/**
	 * Runs a query action with time restrictions (if queryTimeOutMs != -1).
	 * 
	 * if the query can't run within its time limits, #UncheckedTimeoutException is thrown. This is possibly
	 * intercepted by {@link CyTraverserPerformanceTracker}.
	 * 
	 */
	private void timedQuery ( Runnable queryAction, long queryTimeoutMs, List<String> startGeneIris, String query )
	{
		try
		{
			// No timeout wanted
			if ( queryTimeoutMs == -1l ) {
				queryAction.run ();
				return;
			}
						
			TIME_LIMITER.callWithTimeout ( 
				Executors.callable ( queryAction ), queryTimeoutMs, TimeUnit.MILLISECONDS, true 
			);
		}
		catch ( UncheckedTimeoutException|InterruptedException ex ) 
		{
			// Don't wrap it with other exception types, but let it flow to the performance tracker
			throw ExceptionUtils.buildEx ( 
				UncheckedTimeoutException.class,
				ex,
				"Timed out query: %s. First gene IRI is: <%s>. Query is: \"%s\"",
				ex.getMessage (),
				startGeneIris.get ( 0 ),
				escapeJava ( query )
			);
		}
		catch ( Exception ex )
		{
			throw ExceptionUtils.buildEx ( 
				GenericNeo4jException.class,
				ex,
				"Error while traversing Neo4j gene graph: %s. First gene IRI is: <%s>. Query is: \"%s\"",
				ex.getMessage (),
				startGeneIris.get ( 0 ),
				escapeJava ( query )
			);
		}			
	}		
		
	
	/**
	 * Utility to convert a list of {@link ONDEXEntity}ies, interpreted as a chain of concept/relation pairs, to an
	 * {@link EvidencePathNode evidence path}, as defined by the {@link AbstractGraphTraverser graph traverser interface}.
	 * 
	 */
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
	private EvidencePathNode buildEvidencePath ( List<ONDEXEntity> ondexPathEntities )
	{
		EvidencePathNode result = null; 
		for ( ONDEXEntity odxEnt: ondexPathEntities ) 
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
				if ( result == null ) throwEx ( 
					IllegalArgumentException.class, 
					"Internal error: Cypher traverser returned a path not having a concept as first node, entity is: %s",
					ONDEXGraphUtils.getString ( odxEnt )
				);
				
				ONDEXRelation rel = (ONDEXRelation) odxEnt;
				Transition evidence = new Transition ( rel.getOfType () );
				result = new EvidenceRelationNode ( rel, evidence, result );
			}
			else throwEx ( 
				IllegalArgumentException.class, 
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

	public void setPathQuery ( String pathQuery ) {
		this.pathQuery = pathQuery;
	}
}