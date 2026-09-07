export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-08-27:\n\
\n\
- PHI-base provenance now stamped directly on organism protein/gene nodes (a PHIBASE-namespace\n\
  accession carrying the matched PHI-base target's UniProtKB ID, >=80% BLAST identity), not just\n\
  on a separate linked node reachable only via an h_s_s edge. Makes PHI-base-derived genes\n\
  identifiable without a graph traversal (knet-pipelines#35).\n\
- Chromosome density bands added to all basemaps (50 bins per chromosome, gene counts per bin).\n\
\n\
Note: an earlier build under this same version tag also included a Fusarium oxysporum Fo47\n\
genome upgrade (GCA_000271705.2 -> GCA_013085055.1). That upgrade was rolled back on 2026-08-20\n\
(breaks the fuseff extension) before ever reaching a real deploy, and is NOT part of this build --\n\
Fo47 remains on GCA_000271705.2, matching v63-RC1.\n\
---------------------------\n\
Version v63-RC1 2026-07-15:\n\
\n\
- Added 4 new species (Fusarium odoratissimum, Fusarium oxysporum lycopersici, Fusarium oxysporum Fo47, Verticillium dahliae) to legacy Ascomycota-Premium bundle and created Fungi-Premium.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
