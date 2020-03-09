package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.helpers;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.buildEx;
import static uk.ac.ebi.utils.exceptions.ExceptionUtils.throwEx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
import uk.ac.ebi.utils.threading.HackedBlockingQueue;
import uk.ac.ebi.utils.threading.ThreadUtils;
import uk.ac.ebi.utils.threading.batchproc.BatchProcessor;
import uk.ac.ebi.utils.threading.batchproc.processors.ListBasedBatchProcessor;
import uk.ac.rothamsted.knetminer.backend.cypher.CypherClient;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.CypherGraphTraverser;
import uk.ac.rothamsted.neo4j.utils.GenericNeo4jException;

/**
 * An helper (used by {@link SinglePathQueryProcessor}) to traverse a list of genes with a single query. 
 * This is based on {@link BatchProcessor}, which is used to group start genes into batches and run one
 * query per batch as a parallel task. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>25 Nov 2019</dd></dl>
 *
 */
@Component @Scope ( "prototype" )
class SinglePathQueryProcessor
	extends ListBasedBatchProcessor<ONDEXConcept, Consumer<List<ONDEXConcept>>>
{	
	/** This is a configurable parameter */
	@Autowired ( required = false) @Qualifier ( "queryBatchSize" ) 
	private long queryBatchSize = DEFAULT_QUERY_BATCH_SIZE;
	
	/** This is a configurable parameter */
	@Autowired ( required = false ) @Qualifier ( "queryTimeoutMs" )
	private long queryTimeoutMs = 20 * 1000;

	
	/**
	 * The pool size used by the query processor to send semantic motif queries in parallel to
	 * Neo4j.
	 * 
	 * Namely, this is used with {@link #setExecutor(ExecutorService)}, which, in the default implementation,
	 * uses this parameter and {@link #threadQueueSize} with {@link HackedBlockingQueue#createExecutor(int, int)},
	 * ie, we use a fixed number of active threads, and a fixed number of thread queue.
	 * 
	 * If this value is -1, the default is {@link Runtime#availableProcessors()}.
	 * If {@link #threadQueueSize} is -1, it's set to the default of {@link #threadPoolSize} * 2.
	 * 
	 * This is a configurable parameter, but we have it so just to test performance issues with Neo4j, it's unlikely
	 * you will need to change the defaults.
	 * 
	 */
	@Autowired ( required = false ) @Qualifier ( "queryThreadPoolSize" )
	private int threadPoolSize = -1;
	
	/**
	 * @see #threadPoolSize
	 */
	@Autowired ( required = false ) @Qualifier ( "queryThreadQueueSize" )
	private int threadQueueSize = -1;
	
	
	public static final long DEFAULT_QUERY_BATCH_SIZE = 500;

	
	private String pathQuery;
	
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
  
  /** Used by {@link #timedQuery(Runnable, long, List, String)}. */
	private static final TimeLimiter TIME_LIMITER = new SimpleTimeLimiter ();

	private boolean isInterrupted = false; 
	
	{
		this.setJobLogPeriod ( -1 ); // We do all the logging about submitted/completed jobs
	}
	
	public SinglePathQueryProcessor ()
	{
		super ();
	}	

	@PostConstruct
	private void init ()
	{
		this.getBatchCollector ().setMaxBatchSize ( this.queryBatchSize );
				
		synchronized ( SinglePathQueryProcessor.class ) 
		{
			if ( SHARED_EXECUTOR != null )
				this.setExecutor ( SHARED_EXECUTOR );
			else
			{
				if ( this.threadPoolSize == -1 && this.threadQueueSize == -1 )
					SHARED_EXECUTOR = this.getExecutor ();
				else
				{
					int poolSize = this.threadPoolSize != -1 ? this.threadPoolSize : Runtime.getRuntime().availableProcessors();
					int queueSize = this.threadQueueSize != -1 ? this.threadQueueSize : poolSize * 2;
					this.setExecutor ( SHARED_EXECUTOR = HackedBlockingQueue.createExecutor ( poolSize, queueSize ) );
				}
				ThreadUtils.setNamingThreadFactory ( SinglePathQueryProcessor.class, SHARED_EXECUTOR );
			}
		}
	}
	
	
	/**
	 * This is the entry point used by {@link PathQueryProcessor#process(ONDEXGraph, Collection)}.
	 */
	@SuppressWarnings ( "rawtypes" )
	public void process ( 
		ONDEXGraph graph,
		Collection<ONDEXConcept> concepts,
		Map<ONDEXConcept, List<EvidencePathNode>> result,
		PercentProgressLogger queryProgressLogger
	)
	{
		this.isInterrupted = false;
		
		this.setBatchJob ( batch -> 
		{
			if ( this.isInterrupted ) return;
			this.queryJob ( graph, batch, result ); 
			queryProgressLogger.updateWithIncrement ();
		});

		// TODO: parallelStream() might be worth here and should work, but needs testing.
		// This is only about scanning the concepts and build the batches in parallel or not, 
		// querying the batches of concepts is parallel anyway.
		//super.process ( concept -> concepts.stream ().forEach ( concept ) );	
		
		boolean wasInterrupted[] = new boolean[] { false };
		super.process (
			conceptConsumer -> wasInterrupted [ 0 ] = 
				!concepts.stream ()
				.peek ( conceptConsumer )
				.allMatch ( concept -> !this.isInterrupted )
		);
		if ( wasInterrupted [ 0 ] ) log.debug ( "Query processor was interrupted, query is:\n  {}", this.pathQuery );
	}
	
	
	@SuppressWarnings ( "rawtypes" )
	private void queryJob ( ONDEXGraph graph, List<ONDEXConcept> batch, Map<ONDEXConcept, List<EvidencePathNode>> result )
	{
		// So, let's get the starting IRIs from the concepts parameter.
		//
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

		// Don't allow it to run too long (if queryTimeoutMs != -1)
		Runnable timedQueryAction = () -> timedQuery ( queryAction, startGeneIris ); 

		
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
		catch ( UncheckedTimeoutException ex ) 
		{
			if ( log.isTraceEnabled () )
				log.trace ( "Query timed out. First gene: <{}>, query: {}", startGeneIris.get ( 0 ), pathQuery );

			// The query didn't complete within the timeout, results are partial, we must invalidate
			// everything
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
			
			result.computeIfAbsent ( firstGene, k -> Collections.synchronizedList ( new ArrayList<> () ) )
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
			}
		);
	}
	
	
	/**
	 * Runs a query action with time restrictions (if queryTimeOutMs != -1).
	 * 
	 * if the query can't run within its time limits, #UncheckedTimeoutException is thrown. This is possibly
	 * intercepted by {@link CyTraverserPerformanceTracker}.
	 * 
	 */
	private void timedQuery ( Runnable queryAction, List<String> startGeneIris )
	{
		try
		{
			// No timeout wanted
			if ( this.queryTimeoutMs == -1l ) {
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
				escapeJava ( this.pathQuery )
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
				escapeJava ( this.pathQuery )
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

	/**
	 * Causes {@link #process(ONDEXGraph, Collection, Map, PercentProgressLogger) the ongoing} query processing to 
	 * stop. This is used by {@link PathQueryProcessor#interrupt()}, see also {@link CypherGraphTraverser#interrupt()}.
	 */
	void interrupt () {
		this.isInterrupted = true;
	}

}
