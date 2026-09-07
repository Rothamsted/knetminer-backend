export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-08-25:\n\
\n\
- Ported fungi-lite onto the modern ROGER numbered-stage pipeline and the RootHOGs (FastOMA)\n\
  orthology join, replacing the old compara-based workflow_fungi-lite.xml.\n\
- Migrated etl_conf_fungi-lite.py onto etl_conf_beta, matching fungi-premium's data quality\n\
  (Ensembl-Beta alignment + PubMed-query fixes).\n\
- Fusarium graminearum genome corrected to the current PH-1 assembly (GCA_900044135.1,\n\
  2025-08-HammondKosackLab community genebuild), matching fungi-premium.\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 03-11-2025:\n\
\n\
- Fixed missing Gene-enc-Protein relationships for Fusarium Graminarium.\n\
---------------------------\n\
Version v61-RC1 05-08-2025:\n\
\n\
- New semantic motif categories. Read more: https://github.com/KnetMiner/knetminer-schemas/tree/main/semantic-motif-taxonomy/knet-motif-categories-doc\n\
- Fixed character encoding for non-English letters in publication titles/authors/etc.\n\
- Added release notes to the graphs!\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
