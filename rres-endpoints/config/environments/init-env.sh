export KNET_SCRIPTS_HOME=`realpath "${BASH_SOURCE[0]}"`
export KNET_SCRIPTS_HOME=`dirname "$KNET_SCRIPTS_HOME"`
cd "$KNET_SCRIPTS_HOME"

export JAVA_TOOL_OPTIONS="-Xmx64G"
export KNET_SNAKE_OPTS="--profile config/snakemake/slurm"

. config/common-cfg.sh

conda activate "$KNET_DATA_HOME/software/conda/mamba/envs/snakemake"
