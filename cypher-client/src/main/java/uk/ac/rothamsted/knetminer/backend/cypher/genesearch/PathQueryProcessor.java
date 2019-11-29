package uk.ac.rothamsted.knetminer.backend.cypher.genesearch;

import static java.lang.Math.ceil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>28 Nov 2019</dd></dl>
 *
 */
@Component
public class PathQueryProcessor implements ApplicationContextAware
{
	// Protected allows inner classes to access without synthetic methods
	protected ApplicationContext springContext;
	
	@Resource( name = "semanticMotifsQueries" )
	private List<String> semanticMotifsQueries; 

	@Autowired
	private CyTraverserPerformanceTracker cyTraverserPerformanceTracker;

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
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public PathQueryProcessor () {
	}
	
	@SuppressWarnings ( { "rawtypes" } )
	public void process ( ONDEXGraph graph, Collection<ONDEXConcept> concepts, Map<ONDEXConcept, List<EvidencePathNode>> result )
	{
		this.cyTraverserPerformanceTracker.reset ();
				
		PercentProgressLogger queryProgressLogger = new PercentProgressLogger ( 
			"{}% of graph traversing queries processed",
			(long) ceil ( 1.0 * concepts.size () / this.queryBatchSize ) * semanticMotifsQueries.size (),
			1 // TODO: debug, go back to 10 sooner than later
		);
		
		this.semanticMotifsQueries.parallelStream ()
			.forEach ( query -> 
			{
				SinglePathQueryProcessor thisQueryProc = this.processorCache.getUnchecked ( query );
				thisQueryProc.process ( graph, concepts, result, queryProgressLogger ); 
		});
		
		this.cyTraverserPerformanceTracker.logStats ();
	}
	
	
	@Override
	public void setApplicationContext ( ApplicationContext applicationContext ) throws BeansException
	{
		this.springContext = applicationContext;
	}	
}
