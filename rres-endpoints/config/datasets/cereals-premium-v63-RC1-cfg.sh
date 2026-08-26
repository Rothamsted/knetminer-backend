export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-07:\n\
\n\
- Ensembl Plants release 63; bread wheat on IWGSC RefSeq v2.1.\n\
- Nine cereals/grasses incl. hexaploid wheat + oat; Arabidopsis reference by orthology.\n\
- Homoeolog-aware gene naming + literature text-mining for polyploids.\n\
- Hybrid gene-publication layer (PubTator3 text-mined + NCBI-curated + HunFlair2) with AI summaries.\n\
- Curated crop-genomics extension (co-expression, GWAS/markers, cultivar PAV, gene regulation) for\n\
  wheat, maize and rice; v1.1->v2.1 wheat id mapping.\n\
- Full annotation + KG pipeline on the ROGER GPU cluster.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
