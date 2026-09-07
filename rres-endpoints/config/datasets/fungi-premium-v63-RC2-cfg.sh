####################################################################################################
# Fo47 is PINNED to GCA_000271705.2 in config/genomes/fungi-premium.tsv. Do NOT upgrade it without
# regenerating Erika Kroll's fuseff extension inputs (RootHOGs_prefix.tsv + kg-data/*.tsv) and
# clearing it with Keywan/Erika first -- the 2020 assembly renames every Fo47 gene FOZG_* ->
# FOBCDRAFT_*, which breaks the accession-based merge fuseff depends on. That upgrade was built,
# deployed and rolled back once already (2026-08-20), then silently reappeared in the 2026-09-03
# build because only the deployed dump was reverted, not the ETL build tree.
#
# The v63-RC2 dump in S3 was replaced 2026-09-07 19:36 (6,316,645,436 bytes) with a build verified
# at FOZG_ 18,191 / FOBCDRAFT_ 0. See knet-pipelines docs/session-handover.md, 2026-09-07.
####################################################################################################
export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-09-07:\n\
\n\
- PHI-base provenance now stamped directly on organism protein/gene nodes (a PHIBASE-namespace\n\
  accession carrying the matched PHI-base target's UniProtKB ID, >=80 percent BLAST identity), not\n\
  just on a separate linked node reachable only via an h_s_s edge. Makes PHI-base-derived genes\n\
  identifiable without a graph traversal (knet-pipelines#35).\n\
- Chromosome density bands added to all basemaps (50 bins per chromosome, gene counts per bin).\n\
\n\
Note: Fusarium oxysporum Fo47 remains on GCA_000271705.2, unchanged from v63-RC1. A 2020\n\
chromosome-level assembly exists and would fix Map View for this organism, but it renames every\n\
Fo47 gene and breaks the Fusarium effector (fuseff) extension that merges onto them by accession.\n\
---------------------------\n\
Version v63-RC1 2026-07-15:\n\
\n\
- Added 4 new species (Fusarium odoratissimum, Fusarium oxysporum lycopersici, Fusarium oxysporum Fo47, Verticillium dahliae) to legacy Ascomycota-Premium bundle and created Fungi-Premium.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
