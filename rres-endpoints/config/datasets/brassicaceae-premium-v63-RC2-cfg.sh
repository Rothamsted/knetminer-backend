export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-09-07:\n\
\n\
- Shortened gene display names. Genes with no curated symbol previously displayed their full assembly-tagged accession (for example BOLC102_C01P000080.1_BOLC102_V1); they now display the informative part alone (C01P000080.1). Affects Brassica napus, Brassica oleracea, Brassica oleracea var. botrytis (cauliflower) and Brassica rapa.\n\
- Gene identifiers and accessions are unchanged: this affects the displayed label only, so existing links, searches and saved queries by gene ID continue to work.\n\
---------------------------\n\
Version v63-RC1 2026-07-29:\n\
\n\
- Added Brassica oleracea var. botrytis (cauliflower, taxid 3715) as a distinct organism, taking the bundle to 6.\n\
- Upgraded Brassica napus to Darmor-bzh v10 (chromosome-level, replacing the 2015 scaffold assembly).\n\
- Upgraded Brassica oleracea to BOLC102.1 (taxid 3712), replacing the 2014 TO1000 assembly.\n\
- Orthology now from FastOMA RootHOGs, replacing Ensembl Compara.\n\
- Gene names transferred from curated Arabidopsis symbols over hierarchical orthologous groups.\n\
- Gene-publication evidence from HunFlair2 NER plus a PubTator3 hybrid layer with LLM-extracted evidence summaries.\n\
- Chromosome labels derived from each assembly's own GFF3 region records, so genome-browser basemaps align with the gene nodes.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
