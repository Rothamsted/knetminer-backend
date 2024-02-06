export RRES_HOME=data_home=/home/data/knetminer
export RRES_SOFTWARE_HOME="$RRES_HOME/software"

# The Ondex RDF exporter
export KETL_RDFEXP_HOME="$RRES_SOFTWARE_HOME/rdf-export-2-cli"

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME="$RRES_SOFTWARE_HOME/ondex-mini/tools/neo4j-exporter"

export KNET_HOME="$RRES_SOFTWARE_HOME/knetminer"
export KNET_INITIALIZER_HOME="$KNET_HOME/knetminer-initializer-cli/target/knetminer-initializer-cli-5.7-SNAPSHOT"

export JAVA_TOOL_OPTIONS="-Xmx64G"

export KETL_SNAKE_OPTS="--profile config/snakemake/slurm"
# export KETL_SNAKE_OPTS="$KETL_SNAKE_OPTS --dry-run"


# TODO: you need to do it manually, since it relies on sourced files
# conda activate snakemake

function ketl_get_neo_url ()
{
  neo_host=`cat "$KETL_OUT/tmp/neo4j-slurm.host"`
  echo "bolt://$neo_host:7687"
}

export KETL_NEO_START="$KETL_HOME/utils/neo-start-slurm.sh" 
export KETL_NEO_STOP="$KETL_HOME/utils/neo-stop-slurm.sh" 

# TODO: remove it from both here and my home
#conda activate "$KNET_DATA_HOME/software/conda/mamba/envs/snakemake"

module load snakemake/6.1.0-foss-2020b


