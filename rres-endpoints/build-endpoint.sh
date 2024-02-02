# Start the pipeline with this.
# 
# Remember, this requires that you bash-source an environment, for the RRes infrastructure, use 
# <home>/config/environments/rres-env.sh, add <your-own>-env.sh environment for your PC or testing
# infrastructure.
# 
set -e

# Environments like SLURM need absolute paths. If you don't set this from the root invoker,
# you might have problems with finding config/default-cfg.sh, invoked below
#
KETL_HOME=`realpath "$0"`
KETL_HOME=`dirname "$KETL_HOME"`
cd "$KETL_HOME"
export KETL_HOME="`pwd`"

cd "$KETL_HOME"

# This defines a few defaults and then invokes other specific config files
# 
. config/default-cfg.sh

snakemake --cores --until all \
  --snakefile build-endpoint.snakefile \
  --config dataset_id="$KETL_DATASET_ID" dataset_version="$KETL_DATASET_VERSION"\
  $KETL_SNAKE_OPTS
