# Creates dataset metadata
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
# It assumes the input for the tool is the URI-equipped OXL.
#

set -e

echo -e "\n\nRDF Generating dataset descriptor into '$outf'"
"$KETL_RDFEXP_HOME/oxl-descriptor.sh" \
  --template "$KETL_RDFEXP_HOME/knetminer-descriptors/knetminer-metadata-template.ttl" \
  --config "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-metadata-descriptor.properties" \
  --export "$KETL_OUT/rdf/knowledge-graph-metadata.ttl" \
  "$KETL_OUT/tmp/knowledge-graph-uris.oxl" \
  "$KETL_OUT/knowledge-graph-uri-metadata.oxl"
