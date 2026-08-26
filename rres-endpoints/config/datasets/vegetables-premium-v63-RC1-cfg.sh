export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-07:\n\
\n\
- Ensembl Plants release 63.\n\
- Hybrid gene-publication literature layer: PubTator3 (text-mined + NCBI-curated) unioned with an\n\
  in-house HunFlair2 NER pipeline; per-link AI functional summaries (LiteratureSnippet nodes).\n\
- Sequence-based Ensembl<->Entrez crosswalk recovering literature links missed by identifier mapping.\n\
- Full annotation pipeline ported to the ROGER GPU cluster.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
