package uk.ac.rothamsted.knetminer.backend.cypher.genesearch.smtranslator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.ondex.core.ONDEXGraph;
import net.sourceforge.ondex.core.memory.MemoryONDEXGraph;
import net.sourceforge.ondex.exception.type.ParsingFailedException;
import net.sourceforge.ondex.parser.oxl.Parser;
import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.knetminer.backend.cypher.genesearch.smtranslator.StateMachine2CyTranslator;

/**
 * Tests {@link StateMachine2CyTranslator}. Tests are based on dummy SM files in src/test/resources, open them
 * to get an idea of their structure. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Mar 2019</dd></dl>
 *
 */
public class SMTranslatorTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	/**
	 * Simple, without loops or len constraints
	 */
	@Test
	public void testBasics ()
	{
		convert ( 
			"basic-sm",
			5,
			new String [] { "L05_path_4", "L05_enzyme_8", "L05_path_5" },
			new String [] {	
				"gene_1:Gene{ iri: $startIri }",
				"(bioProc_3:BioProc)",
				"- [part_of_3_5:part_of] - (path_5:Path)"
			}
		);
	}

	
	/**
	 * Like {@link #testBasics()}, but with directed transitions.
	 */
	@Test
	public void testDirectedEdge ()
	{
		convert ( 
			"directed-edge-sm",
			5,
			new String [] { "L05_path_4", "L05_enzyme_8", "L05_path_5" },
			new String [] {	
				"gene_1:Gene{ iri: $startIri }",
				"(bioProc_3:BioProc)",
				"- [part_of_3_5:part_of] - (path_5:Path)",
				"- [regulates_1_7_d:regulates] -> (tO_7:TO)"
			}
		);
	}	
	
	/**
	 * Has a loop and a few len constraints
	 */
	@Test
	public void testLoop ()
	{
		convert ( 
			"loop-sm",
			5,
			new String [] { 
				"L05_path_4",
				"L05_publication_6",
				"L05_path_5",
				"L05_molFunc_9",
				"L05_enzyme_8"
			},
			new String [] {	
				"gene_1:Gene{ iri: $startIri }",
				"- [part_of_3_3:part_of*0..1] - (bioProc_3b:BioProc)",
				"- [asso_wi_7_9_2:asso_wi*1..2] - (molFunc_9:MolFunc)",
				"- [asso_wi_7_8:asso_wi] - (enzyme_8:Enzyme)"
			}
		);
	}
	
	/**
	 * Loop and complex len constraints, including multiple relations (with different constraints) between
	 * the sam node pair.
	 * 
	 */
	@Test
	public void testLenConstraints ()
	{
		convert ( 
			"len-constraints-sm",
			4,
			new String [] { "L05_path_5", "L05_tO_7" },
			new String [] {	
				"gene_1:Gene{ iri: $startIri }",
				"(bioProc_3:BioProc)",
				// I must split transitions between the same nodes into two, since they have different length constraints
				"- [is_part_of_3_5_2:is_part_of*1..2] - (path_5:Path)",
				"- [part_of_3_5_3:part_of*1..3] - (path_5:Path)",
				// These instead have the same constraint, so there must be a single multi-type relation clause
				"- [rel_1_6_2:asso_wi|pub_in*1..2] - (publication_6:Publication)"
			}
		);
	}	
	
	/**
	 * A real use case
	 */
	@Test
	public void testArabidopsis ()
	{
		convert ( 
			"ara-sm",
			"wheat-metadata.xml",
			35, 
			new String [] {
				"L03_publication_2",
				"L05_publication_2",
				"L05_protein_7",
				"L07_reaction_133",
				"L09_path_14"
			}, 
			new String [] {
				"- [rel_10_10_d_2:genetic|physical*0..2] -> (protein_10b:Protein)",
				"- [rel_10_10:h_s_s|ortho|xref*0..1] - (protein_10b:Protein)",
				"(protein_10b:Protein)\n  - [rel_10_7:h_s_s|ortho|xref] - (protein_7:Protein)",
				"(protein_10b:Protein)\n  - [rel_10_7_d_2:genetic|physical*1..2] -> (protein_7:Protein)",
				"(gene_1:Gene{ iri: $startIri })\n  - [rel_1_9:genetic|physical] - (gene_9:Gene)\n  - [cooc_wi_9_16:cooc_wi] - (trait_16:Trait)",
				"(gene_1:Gene{ iri: $startIri })\n  - [has_variation_1_15:has_variation] - (sNP_15:SNP)\n  - [associated_with_15_16:associated_with] - (trait_16:Trait)",
				"(gene_1:Gene{ iri: $startIri })\n  - [cooc_wi_1_16:cooc_wi] - (trait_16:Trait)"
			} 
		);
	}
	
	/**
	 * Uses the default metadata.
	 */
	private Map<String, String> convert ( 
		String smName, int expectedQueryCount, String[] expectedQueryNames, String[] expectedQueryFragments 
	)
	{
		return convert ( smName, null, expectedQueryCount, expectedQueryNames, expectedQueryFragments );
	}
	
	
	/**
	 * Basic code to load a SM file, translate it to queries and log its output. 
	 */
	private Map<String, String> convert ( 
		String smName, String metadataPath, int expectedQueryCount, String[] expectedQueryNames, String[] expectedQueryFragments 
	)
	{
		ONDEXGraph metaGraph = null;
		if ( metadataPath != null )
		{
			try
			{
				metaGraph = new MemoryONDEXGraph ( "metadata" );
				Reader reader = IOUtils.openResourceReader ( metadataPath );
				Parser oxlParser = new Parser ();
				oxlParser.setONDEXGraph ( metaGraph );
				oxlParser.start ( reader );
			}
			catch ( ParsingFailedException | IOException ex ) {
				throw new RuntimeException ( "Internal error: " + ex.getMessage (), ex );
			}
		}
		
		StateMachine2CyTranslator translator = new StateMachine2CyTranslator (
			"target/test-classes/statemachine2cypher-trns/" + smName + ".txt", metaGraph
		);
		Map<String, String> queries = translator.getCypherQueries ();
		
		log.info ( "Found {} queries:", queries.size () );
		queries.forEach ( (name,q) -> log.info ( "\n\nQUERY '{}': {}\n", name, q ) );
		
		assertEquals ( "Wrong no. of results!", expectedQueryCount, queries.size () );
		
		for ( String name: expectedQueryNames )
			assertTrue ( 
				name + " name not found!", 
				queries.keySet ().stream ().anyMatch ( n -> n.contains ( name ) ) 
			);

		for ( String qfrag: expectedQueryFragments )
			assertTrue ( 
				"Cypher fragment: \"" + qfrag + "\" not found!", 
				queries.values ().stream ().anyMatch ( q -> q.contains ( qfrag ) ) 
			);		
		
		return queries;
	}
}
