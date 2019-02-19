package uk.ac.rothamsted.knetminer.backend.test;

import java.util.Collections;

import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.DataSource;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.ONDEXGraphMetaData;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.export.oxl.Export;
import net.sourceforge.ondex.parser.oxl.Parser;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>19 Feb 2019</dd></dl>
 *
 */
public class OxlTestDataCreator
{

	public static void main ( String[] args )
	{
		String inOxlPath = args [ 0 ];
		String outOxlPath = args [ 0 ];

		ONDEXGraph graph = Parser.loadOXL ( inOxlPath );
		ONDEXGraphMetaData gmeta = graph.getMetaData ();
		
		ConceptClass geneCC = gmeta.getConceptClass ( "Gene" );
		ConceptClass testCC = gmeta.createConceptClass (
			"TestCC", "A Test Concept Class", "Introduced by backend/test-data-server", null
		);
		RelationType testRel = gmeta.createRelationType ( 
			"has_test_relation", "A Test Relation", "Introduced by backend/test-data-server", 
			"", false, false, false, false, null 
		);
		
		DataSource ensmblDs = gmeta.getDataSource ( "BIOGRID:ENSEMBL:TAIR" );
		EvidenceType impdEv = gmeta.getEvidenceType ( "IMPD" );
		AttributeName taxIdAttrName = gmeta.getAttributeName ( "TAXID" );
		
//		ONDEXConcept g0 = graph.
//			getConceptsOfConceptClass ( geneCC )
//			.parallelStream ()
//			.filter ( g -> "AT3G13540;locus:2092820".equals ( g.getPID () ) )
//			.findAny ()
//			.orElseThrow ( () -> new IllegalStateException ( "Reference gene not found!" ) ); 
				
		ONDEXConcept g1 = graph.createConcept ( 
			"TEST-GENE-01", "", "A Test Gene, introduced by backend/test-data-server", ensmblDs, geneCC, 
			Collections.singleton ( impdEv ) 
		);
		g1.createConceptName ( "TEST-GENE-01", true );
		g1.createAttribute ( taxIdAttrName, "3702", true );
		
		ONDEXConcept testEnt1 = graph.createConcept ( 
			"TEST-ENT-01", "", 
			"A Test Entity, introduced by backend/test-data-server. " +
			"Lorem ipsum dolor sit amet, consectetur adipiscing elit", 
			ensmblDs, testCC, 
			Collections.singleton ( impdEv ) 
		);
		testEnt1.createConceptName ( "TEST-ENT-01", true );
		
		graph.createRelation ( g1, testEnt1, testRel, Collections.singleton ( impdEv )  );
		
		Export.exportOXL ( graph, outOxlPath, true, true );
	}

}
