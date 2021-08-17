export KNET_DATA_HOME="${KNET_DATA_HOME-/home/data/knetminer}"

export KNET_DATA_TARGET="${KNET_DATA_TARGET-$KNET_DATA_HOME/pub/endpoints}"

export KNET_SOFTWARE_HOME="${KNET_SOFTWARE_HOME-$KNET_DATA_HOME/software}"
export KNET_RDFEXP_HOME="${KNET_RDFEXP_HOME-$KNET_SOFTWARE_HOME/rdf-export-2-cli}"
export KNET_NEOEXPORT_HOME="${KNET_NEOEXPORT_HOME-$KNET_SOFTWARE_HOME/ondex-mini/tools/neo4j-exporter}"
export NEO4J_HOME="${NEO4J_HOME-$KNET_SOFTWARE_HOME/neo4j-test}"

export KNET_NEO_URL="${KNET_NEO_URL-bolt://localhost:7687}"
export KNET_NEO_USR="${KNET_NEO_USR-neo4j}"
export KNET_NEO_PWD="${KNET_NEO_PWD-test}"

# TODO: remove, we use SLURM for Snakemake
#export KNET_CONDA_ENV="${KNET_CONDA_ENV-snakemake}"
