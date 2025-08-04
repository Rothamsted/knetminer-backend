export KETL_REL_NOTES="\n\
---------------------------\n\
Version v61-RC2 01-08-2025:\n\
\n\
- New semantic motif categories. Read more: https://github.com/KnetMiner/knetminer-schemas/tree/main/semantic-motif-taxonomy/knet-motif-categories-doc\n\
- Added release notes to the graphs!\n\
---------------------------\n\
Version v61-RC1 24-07-2025:\n\
\n\
- Removed redundant alternative names for Arabidopsis Thaliana (Thale Cress).\n\
- Fixed character encoding for non-English letters is publication titles/authors/etc.\n\
- Replaced hash code identifiers in Phenotype nodes with short LLM-generated labels. Read more: https://github.com/KnetMiner/knetML/tree/main/Entity_Relation_extraction/bedrock_phenotype_title\n\
---------------------------\n\
Version v60-RC4 05-06-2025:\n\
\n\
- Added semantic motif categories.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"

# WARNING: see cereals-dummy-2.sh for details on how -cfg/-common composition works.

# We don't need to change anything else about the dataset common defs, this file is 
# just a version ID placeholder
#
