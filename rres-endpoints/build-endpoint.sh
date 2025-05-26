# Start the pipeline with this.
# 
# Remember, as per config/default-cfg.sh below, this should be executed as:
#
#   build-end-point.sh <dataset_id> <ver_id> [env_id]
# 
# and it will search for:
# 
# - config/datasets/$dataset_id-$ver_id-cfg.sh
# - config/environments/$env_id.sh
#
# if you omit env_id, you should bash-source manually before running me
#
# Use: config/environments/rres-env.sh for RRes environment
# Add <your-own>-env.sh environment to the same directory for your PC/testing environment
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

snakemake --cores all --until all \
  --snakefile build-endpoint.snakefile \
  --config dataset_id="$KETL_DATASET_ID" dataset_version="$KETL_DATASET_VERSION"\
  $KETL_SNAKE_OPTS
