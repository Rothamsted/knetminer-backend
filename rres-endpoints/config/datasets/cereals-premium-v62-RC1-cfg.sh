export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 05-06-2026:\n\
\n\
- Arabidopsis Thaliana (Thale Cress) genome updated to TAIR12 release.\n\
- Triticum Aestivum (Bread Wheat) genome upgraded to RefSeq v2.1.\n\
- New HomologyTree node type and part_of relation to it from proteins, extracted from OMA. Read more: https://github.com/KnetMiner/knetminer-etl/issues/1\n\
- New Arabidopsis Thaliana (Thale Cress) and Oryza sativa (Rice) pangenome nodes with gene presence/absence variation (PAV) data.\n\
- New gene-phenotype association extracted with LLM from recent publications.\n\
- Protein fasta, CDS fasta, GFF and species data now come from Ensembl Beta website.\n\
- Avena Sativa (Oat) removed temporarily.\n\
---------------------------\n\
Version v61-RC1 01-08-2025:\n\
\n\
- New semantic motif categories. Read more: https://github.com/KnetMiner/knetminer-schemas/tree/main/semantic-motif-taxonomy/knet-motif-categories-doc\n\
- Fixed issue with missing Triticum Turgidum (Durum Wheat) data.\n\
- Removed redundant alternative names for Arabidopsis Thaliana (Thale Cress).\n\
- Replaced Avena Sativa (Oat) variety from ot_3098 to sang.\n\
- Fixed character encoding for non-English letters is publication titles/authors/etc.\n\
- Replaced hash code identifiers in Phenotype nodes with short LLM-generated labels. Read more: https://github.com/KnetMiner/knetML/tree/main/Entity_Relation_extraction/bedrock_phenotype_title\n\
- Added release notes to the graphs!\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"