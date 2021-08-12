set -e
export MY_HOME=`realpath "$0"`
export MY_HOME=`dirname "$MY_HOME"`
cd "$MY_HOME"
. config/init-config.sh

mkdir -p "$CFG_DATASET_TARGET"
echo -e "\n\nAdding URIs to '$CFG_OXL_SRC' and saving to '$CFG_DATASET_TARGET/knowledge-graph-uris.oxl'"
java -version
"$CFG_RDFEXP_HOME/add-uris.sh" "$CFG_OXL_SRC" "$CFG_DATASET_TARGET/knowledge-graph-uris.oxl"
