export MY_HOME=`dirname "$0"`
cd "$MY_HOME"
. config/init-config.sh

mkdir -p "$CFG_DATASET_TARGET"
"$CFG_RDFEXP_HOME/add-uris.sh" "$CFG_OXL_SRC" "$CFG_DATASET_TARGET/knowledge-graph-uris.oxl"
