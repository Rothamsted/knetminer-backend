
# Endpoint building step to add URIs to an OXL. 
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
#   

set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

mkdir -p "$KNET_DATASET_TARGET"
mkdir -p "$KNET_DATASET_TARGET/tmp"

echo -e "\n\nAdding URIs to '$KNET_SRC_OXL' and saving to '$KNET_DATASET_TARGET/knowledge-graph-uris.oxl'"
"$KNET_RDFEXP_HOME/add-uris.sh" "$KNET_SRC_OXL" "$KNET_DATASET_TARGET/knowledge-graph-uris.oxl"
