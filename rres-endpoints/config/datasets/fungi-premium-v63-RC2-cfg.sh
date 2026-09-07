####################################################################################################
# ⚠️  The v63-RC2 artefact built 2026-09-03 and currently in S3 does NOT match these notes.
#
# That build silently re-included the Fusarium oxysporum Fo47 genome upgrade (GCA_000271705.2 ->
# GCA_013085055.1) which was rolled back on 2026-08-20 for breaking Erika Kroll's fuseff extension.
# The rollback had only ever been applied to the deployed Neo4j dump and the KnetSpace records --
# the ETL build tree was never reverted, so the PHI-base rebuild picked the new assembly back up.
# Verified in the OXL, not inferred: 16,497 FOBCDRAFT_ gene ids.
#
# Fo47 was reverted to GCA_000271705.2 in the build tree on 2026-09-07 (18,191 genes / 102 scaffolds,
# matching v63-RC1) and fungi-premium is being rebuilt to overwrite that S3 artefact. These notes
# describe the REBUILD. Delete this header once the replacement dump is in S3.
#
# Fo47 is now PINNED in config/genomes/fungi-premium.tsv -- do not upgrade it without regenerating
# fuseff's inputs and clearing it with Keywan/Erika first.
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
