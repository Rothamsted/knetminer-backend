package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.helpers;

import static java.lang.Math.ceil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.sourceforge.ondex.algorithm.graphquery.nodepath.EvidencePathNode;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import uk.ac.ebi.utils.runcontrol.PercentProgressLogger;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.CypherGraphTraverser;

/**
 * An helper for {@link CypherGraphTraverser}, which manages {@link SinglePathQueryProcessor}-s, by dispatching the 
 * starting gene list received by 
 * {@link CypherGraphTraverser#traverseGraph(ONDEXGraph, java.util.Set, net.sourceforge.ondex.algorithm.graphquery.FilterPaths)}
 * one {@link SinglePathQueryProcessor} per query configured in {@link #semanticMotifsQueries} (via Spring).
 * 
 * The query-specific path processors are cached in this class, in order to avoid performance problems.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>28 Nov 2019</dd></dl>
 *
 */
@Component
public class PathQueryProcessor implements ApplicationContextAware
{
	/** This is a configurable parameter */
	@Autowired @Qualifier ( "semanticMotifsQueries" )
	private List<String> semanticMotifsQueries; 

	@Autowired
	private CyTraverserPerformanceTracker cyTraverserPerformanceTracker;

	/** This is a configurable parameter */
	@Autowired ( required = false) @Qualifier ( "queryBatchSize" ) 
	private long queryBatchSize = SinglePathQueryProcessor.DEFAULT_QUERY_BATCH_SIZE;

	
	private LoadingCache<String, SinglePathQueryProcessor> processorCache = 
		CacheBuilder.newBuilder ()
		.maximumSize ( 1000 )
		.build ( new CacheLoader<String, SinglePathQueryProcessor> ()
		{
			@Override
			public SinglePathQueryProcessor load ( String pathQuery )
			{
				SinglePathQueryProcessor result = springContext.getBean ( SinglePathQueryProcessor.class );
				result.setPathQuery ( pathQuery );
				return result;
			} 
		});
	
	// Protected allows inner classes to access without synthetic methods
	protected ApplicationContext springContext;
	
	private boolean isInterrupted = false; 
	
	private PercentProgressLogger queryProgressLogger = null;
			
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	public PathQueryProcessor () {
	}
	
	/**
	 * This is the entry point used by 
	 * {@link CypherGraphTraverser#traverseGraph(ONDEXGraph, java.util.Set, net.sourceforge.ondex.algorithm.graphquery.FilterPaths)}.
	 */
	@SuppressWarnings ( { "rawtypes" } )
	public Map<ONDEXConcept, List<EvidencePathNode>> process ( ONDEXGraph graph, Collection<ONDEXConcept> concepts )
	{
		this.isInterrupted = false;

		if ( this.semanticMotifsQueries == null || this.semanticMotifsQueries.isEmpty () ) {
			log.warn ( "We don't have any configured Cypher query to run the traverser! Returning empty result" );
			return new ConcurrentHashMap<> ( 0 );
		}
		
		int threadPoolSize = 
			this.processorCache.getUnchecked ( this.semanticMotifsQueries.iterator ().next () )
			.getThreadPoolSize ();

		doLogConfig ();
		
		int nconcepts = concepts.size ();
		
		Map<ONDEXConcept, List<EvidencePathNode>> result = new ConcurrentHashMap<> ( nconcepts, 0.75f, threadPoolSize );
		this.cyTraverserPerformanceTracker.reset ();
		
		queryProgressLogger = new PercentProgressLogger ( 
			"{}% of graph traversing queries processed",
			(long) ceil ( 1.0 * concepts.size () / this.queryBatchSize ) * semanticMotifsQueries.size (),
			10
		);
			
		this.semanticMotifsQueries
		.stream ()
		.forEach ( query -> 
		{
			if ( isInterrupted ) return;
			SinglePathQueryProcessor thisQueryProc = this.processorCache.getUnchecked ( query );
			thisQueryProc.process ( graph, concepts, result, queryProgressLogger ); 
		});
		
		
		log.info ( "Cypher traverser finished" );
		Map<String, Collection<ONDEXConcept>> timedOutQueries = cyTraverserPerformanceTracker.getTimedOutQueries ();
		if ( !timedOutQueries.isEmpty () )
			log.warn ( "Some queries couldn't complete, see the summary statistics (must be enabled)" );
		this.cyTraverserPerformanceTracker.logStats ();
		
		return result;
	}
	
	
	@Override
	public void setApplicationContext ( ApplicationContext applicationContext ) throws BeansException
	{
		this.springContext = applicationContext;
	}

	
	/** 
	 * 
	 * There are debugging/testing components reporting this (eg, cypher debugger in Knetminer).
	 * @see {@link CypherGraphTraverser#getSemanticMotifsQueries()}.
	 */
	public List<String> getSemanticMotifsQueries () {
		return semanticMotifsQueries;
	}

	/**
	 * There are components that redefine queries dynamically, out of Spring, so we need the setter here.
	 */
	public void setSemanticMotifsQueries ( List<String> semanticMotifsQueries )
	{
		this.semanticMotifsQueries = semanticMotifsQueries;
	}

	/**
	 * Tells a percentage of completed queries. This assumes {@link #process(ONDEXGraph, Collection)} has been invoked and
	 * doesn't do any synchronisation, ie, you are supposed to be asking approximate results.
	 */
	public double getPercentProgress ()
	{
		return this.queryProgressLogger == null
			? 0d : this.queryProgressLogger.getPercentProgress ();
	}
	
	/**
	 * @see {@link CypherGraphTraverser#interrupt()} and {@link CypherGraphTraverser#isInterrupted()}.
	 * 
	 */
	public boolean isInterrupted () {
		return isInterrupted;
	}

	/**
	 * @see {@link CypherGraphTraverser#interrupt()} and {@link CypherGraphTraverser#isInterrupted()}.
	 * 
	 * This propagates the interruption trigger to all {@link PathQueryProcessor#interrupt() query processors}  that the 
	 * component is using.
	 */
	public void interrupt ()
	{
		this.isInterrupted = true;
		log.warn ( "Traversal was interrupted, stopping everything" );

		this.semanticMotifsQueries.forEach ( query ->
		{
			SinglePathQueryProcessor thisQueryProc = this.processorCache.getUnchecked ( query );
			thisQueryProc.interrupt ();
		});
	}
	
	/**
	 * Logs some config params, it's invoked by {@link #process(ONDEXGraph, Collection)} for
	 * diagnostic purposes.
	 */
	private void doLogConfig ()
	{
		log.info ( "---- Cypher Graph Traverser, Config -----" );
		log.info ( "queryBatchSize = {}", this.queryBatchSize );
		
		BiConsumer<String, Class<?>> ctxBeanLogger = (name, cls) -> log.info ( "{} = {}",
			name,
			this.springContext.containsBean ( name ) 
				? this.springContext.getBean ( name, cls )
				: "<default>"
		);

		ctxBeanLogger.accept ( "queryTimeoutMs", Long.class );
		ctxBeanLogger.accept ( "performanceReportFrequency", Integer.class );
		ctxBeanLogger.accept ( "queryPageSize", Long.class );
		ctxBeanLogger.accept ( "queryThreadPoolSize", Integer.class );
		ctxBeanLogger.accept ( "queryThreadQueueSize", Integer.class );
		
		log.info ( "---- /CypherQueryTraverser, Config -----" );
	}
}
