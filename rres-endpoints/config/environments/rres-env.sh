export KNET_SCRIPTS_HOME=`realpath "${BASH_SOURCE[0]}"`
export KNET_SCRIPTS_HOME=`dirname "$KNET_SCRIPTS_HOME"`
cd "$KNET_SCRIPTS_HOME/../.."
export KNET_SCRIPTS_HOME="`pwd`"

export JAVA_TOOL_OPTIONS="-Xmx64G"
export KNET_SNAKE_OPTS="--profile config/snakemake/slurm"

. config/common-cfg.sh

# TODO: remove it from both here and my home
#conda activate "$KNET_DATA_HOME/software/conda/mamba/envs/snakemake"

module load snakemake/6.1.0-foss-2020b
