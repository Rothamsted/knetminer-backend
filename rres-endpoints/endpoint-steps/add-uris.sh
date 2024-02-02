
# Endpoint building step to add URIs to an OXL. 
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
#   

set -e

mkdir -p "$KETL_OUT"
mkdir -p "$KETL_OUT/tmp"

echo -e "\n\nAdding URIs to '$KETL_SRC_OXL' and saving to '$KETL_OUT/knowledge-graph-uris.oxl'"
"$KETL_RDFEXP_HOME/add-uris.sh" "$KETL_SRC_OXL" "$KETL_OUT/tmp/knowledge-graph-uris.oxl"
