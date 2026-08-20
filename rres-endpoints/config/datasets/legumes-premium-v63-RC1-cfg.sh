export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-07-29:\n\
\n\
- Added 2 species: Medicago truncatula (model legume) and Cicer arietinum (chickpea), taking the bundle to 7.\n\
- Upgraded Glycine max to Glycine_max_v4.0 (Wm82.a4) and Phaseolus vulgaris to v2.0, both sourced from NCBI/JGI.\n\
- Orthology now from FastOMA RootHOGs, replacing Ensembl Compara.\n\
- Gene names transferred from curated Arabidopsis symbols over hierarchical orthologous groups.\n\
- Gene-publication evidence from HunFlair2 NER plus a PubTator3 hybrid layer with LLM-extracted evidence summaries.\n\
- Chromosome labels derived from each assembly's own GFF3 region records, so genome-browser basemaps align with the gene nodes.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
