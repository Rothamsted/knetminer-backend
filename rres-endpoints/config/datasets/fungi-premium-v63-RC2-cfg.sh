export KETL_REL_NOTES="\n\
---------------------------\n\
Version ${KETL_DATASET_VERSION} 2026-08:\n\
\n\
- Fusarium oxysporum Fo47 genome upgraded from GCA_000271705.2 (2014 Broad Institute scaffold draft, no chromosome pseudomolecule) to GCA_013085055.1 (2020 Xi'an Jiaotong University, 12-chromosome complete genome) -- fixes missing Map View / Genome Region Search for this organism.\n\
- Chromosome density bands added to all basemaps (50 bins per chromosome, gene counts per bin).\n\
---------------------------\n\
Version v63-RC1 2026-07-15:\n\
\n\
- Added 4 new species (Fusarium odoratissimum, Fusarium oxysporum lycopersici, Fusarium oxysporum Fo47, Verticillium dahliae) to legacy Ascomycota-Premium bundle and created Fungi-Premium.\n\
---------------------------\n\
"
. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION_NUM}-cfg.sh"
