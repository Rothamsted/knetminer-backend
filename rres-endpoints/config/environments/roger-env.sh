# ROGER environment for the oxl -> rdf -> neo endpoint pipeline (ported from rres-env.sh).
#
# Strategy: run the WHOLE pipeline locally inside one ROGER `ulmn` sbatch (snakemake --cores all) --
# rdf-export, tdb_load, rdf2neo and a LOCAL Neo4j all on the 900 GB big-memory node. No snakemake
# SLURM executor and no separate Neo4j-as-a-service job (the fiddliest rothhpc4 bits), which keeps the
# port tractable and testable. Snakemake must already be on PATH (activate the conda env in the sbatch).
#
# Java: 21 from the conda `nextflow` env (this is what the Ondex KG build used and is known-good for
# these Ondex/Jena tools; software/jdk on ROGER is Java 25, untested with these tools).

export KNET_HOME=/home/data/knetminer
export KNET_SOFTWARE="$KNET_HOME/software"

# Test output area (NOT the production pub/endpoints); Neo4j dump + zipped TDB land here.
export KETL_OUT_HOME="$KNET_HOME/etl-test/temp/endpoints"

# ROGER exports APPTAINER_TMPDIR/CACHEDIR to a read-only system path (/home/gpuapps/apptainer) which
# snakemake 9.x tries to use for its metadata and fails with PermissionError. This pipeline uses no
# containers, so redirect them to a writable location.
export APPTAINER_TMPDIR="$KETL_OUT_HOME/.apptainer/tmp"
export APPTAINER_CACHEDIR="$KETL_OUT_HOME/.apptainer/cache"
mkdir -p "$APPTAINER_TMPDIR" "$APPTAINER_CACHEDIR" 2>/dev/null || true

###### Software (shared /home/data -- same tools rothhpc4 uses)
export KETL_RDFEXP_HOME="$KNET_SOFTWARE/rdf-export-2-cli"          # Ondex OXL->RDF exporter
export JENA_HOME="$KNET_SOFTWARE/jena"                              # tdb2.xloader for tdb_load
export KETL_NEOEXPORT_HOME="$KNET_SOFTWARE/ondex-mini/tools/neo4j-exporter"  # rdf2neo
export KNET_WEBAPP="$KNET_SOFTWARE/knetminer"
export KNET_INITIALIZER_HOME="$KNET_SOFTWARE/knetminer-nova/knetminer-initializer"

###### Java 21 (conda nextflow env) + PATH
if [[ -z "$JAVA_HOME" || "$JAVA_HOME" != *nextflow* ]]; then
  export JAVA_HOME="$KNET_SOFTWARE/envs/nextflow"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
export PATH="$KNET_SOFTWARE/bin:$PATH"

# Heap for tdb2.xloader / rdf-export / rdf2neo. NOTE: Neo4j also reads JAVA_TOOL_OPTIONS, so keep this
# below the node's memory and let neo4j.conf govern the DB heap; 200G is ample for the veg-scale TDB.
export JAVA_TOOL_OPTIONS="-Xmx200G"

###### Snakemake: run everything on THIS node (no SLURM profile / cluster executor).
# build-endpoint.sh already passes `--cores all --until all`, so leave this empty (setting the rres
# `--profile config/snakemake/slurm` here is exactly what we are avoiding).
export KETL_SNAKE_OPTS="--rerun-incomplete"

###### Neo4j: LOCAL on this sbatch node. Deliberately DO NOT override KETL_NEO_START/STOP to the
# -slurm variants (keep the default local launchers from default-cfg.sh) and keep the default
# ketl_get_neo_url = bolt://localhost:7687. KETL_HAS_NEO4J / NEO4J_HOME / KETL_NEO_VERSION come from
# the dataset's -common.sh.
export KETL_NEO_IDX_PAUSE_TIME='5m'
