set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

snakemake --cores \
  --snakefile build-endpoint.snakefile \
  --config dataset_id="$KNET_DATASET_ID" dataset_version="$KNET_DATASET_VERSION"\
  $KNET_SNAKE_OPTS
