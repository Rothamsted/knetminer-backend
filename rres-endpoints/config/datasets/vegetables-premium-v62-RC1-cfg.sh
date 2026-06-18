export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-06-18:\n\
\n\
- Arabidopsis Thaliana (Thale Cress) genome updated to TAIR12 release.\n\
- Solanum Lycopersicum (Tomato) genome upgraded to SL4.0.\n\
- Lactuca Sativa (Lettuce) genome upgraded to LSAT_SALINAS_v11.\n\
- New HomologyTree node type and part_of relation to it from proteins, extracted from OMA. Read more: https://github.com/KnetMiner/knetminer-etl/issues/1\n\
- New Arabidopsis Thaliana (Thale Cress) pangenome nodes with gene presence/absence variation (PAV) data.\n\
- New gene-phenotype association extracted with LLM from recent publications.\n\
- Protein fasta, CDS fasta, GFF and species data now come from Ensembl Beta website.\n\
- More accurate gene synonyms and alternative names.\n\
---------------------------\n\
Version v61-RC2 2025-08-04:\n\
\n\
- New semantic motif categories. Read more: https://github.com/KnetMiner/knetminer-schemas/tree/main/semantic-motif-taxonomy/knet-motif-categories-doc\n\
- Added release notes to the graphs!\n\
---------------------------\n\
Version v61-RC1 2025-07-24:\n\
\n\
- Removed redundant alternative names for Arabidopsis Thaliana (Thale Cress).\n\
- Fixed character encoding for non-English letters is publication titles/authors/etc.\n\
- Replaced hash code identifiers in Phenotype nodes with short LLM-generated labels. Read more: https://github.com/KnetMiner/knetML/tree/main/Entity_Relation_extraction/bedrock_phenotype_title\n\
---------------------------\n\
Version v60-RC4 2025-06-05:\n\
\n\
- Added semantic motif categories.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"

# WARNING: see cereals-dummy-2.sh for details on how -cfg/-common composition works.

# We don't need to change anything else about the dataset common defs, this file is 
# just a version ID placeholder
#
