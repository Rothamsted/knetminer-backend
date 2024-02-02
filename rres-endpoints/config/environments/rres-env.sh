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



# TODO: review

export KNET_SCRIPTS_HOME=`realpath "${BASH_SOURCE[0]}"`
export KNET_SCRIPTS_HOME=`dirname "$KNET_SCRIPTS_HOME"`
cd "$KNET_SCRIPTS_HOME/../.."
export KNET_SCRIPTS_HOME="`pwd`"

export JAVA_TOOL_OPTIONS="-Xmx64G"
export KNET_SNAKE_OPTS="--profile config/snakemake/slurm"

# TODO: fix config/neo4j scripts to accept KNET_ENV_POSTFIX=rres

. config/common-cfg.sh

