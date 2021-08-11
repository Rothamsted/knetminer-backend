set -e
export MY_HOME=`realpath "$0"`
export MY_HOME=`dirname "$MY_HOME"`
cd "$MY_HOME"
. config/init-config.sh

# TODO: remove
# nextflow run ./build-endpoint.nf -resume --param_file="$param_file" 

snakemake --cores --snakefile build-endpoint.snakefile --config param_prefix="$param_prefix"
