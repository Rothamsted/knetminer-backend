export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 26-03-2026:\n\
\n\
- New HomologyTree node type and part_of relation to it from proteins, extracted from OMA. Read more: https://github.com/KnetMiner/knetminer-etl/issues/1\n\
- Protein fasta, CDS fasta, GFF and species data now come from Ensembl Beta website.
---------------------------\n\
Version v62-RC1 24-01-2026:\n\
\n\
- New Arabidopsis Thaliana (Thale Cress) pangenome nodes with gene presence/absence variation (PAV) data.\n\
- New gene-phenotype association extracted with LLM from recent publications.\n\
---------------------------\n\
Version v61-RC1 04-08-2025:\n\
\n\
- New semantic motif categories. Read more: https://github.com/KnetMiner/knetminer-schemas/tree/main/semantic-motif-taxonomy/knet-motif-categories-doc\n\
- Removed redundant alternative names for Arabidopsis Thaliana (Thale Cress).\n\
- Fixed character encoding for non-English letters is publication titles/authors/etc.\n\
- Replaced hash code identifiers in Phenotype nodes with short LLM-generated labels. Read more: https://github.com/KnetMiner/knetML/tree/main/Entity_Relation_extraction/bedrock_phenotype_title\n\
- Added release notes to the graphs!\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"