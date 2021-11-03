# Start the pipeline with this.
# 
# Remember, this requires that you bash-source an environment, for the RRes infrastructure, use 
# <home>/config/environments/rres-env.sh, add <your-own>-env.sh environment for your PC or testing
# infrastructure.
# 
set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

snakemake --cores --until all \
  --snakefile build-endpoint.snakefile \
  --config dataset_id="$KNET_DATASET_ID" dataset_version="$KNET_DATASET_VERSION"\
  $KNET_SNAKE_OPTS
