# Creates dataset metadata
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
# It assumes the input for the tool is the URI-equipped OXL.
#
# TODO: still to be added to the Snakemake workflow and tested
#

set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

rdf_target="$KNET_DATASET_TARGET/rdf"
mkdir -p "$rdf_target"
outf="$rdf_target/knowledge-graph-metadata.ttl"

echo -e "\n\nRDF Generating dataset descriptor into '$outf'"
"$KNET_RDFEXP_HOME/oxl-descriptor.sh" \
  --template "$KNET_RDFEXP_HOME/knetminer-descriptors/knetminer-metadata-template.ttl" \
  --config "$KNET_SCRIPTS_HOME/config/${}KNET_DATASET_ID}-metadata-descriptor.properties" \
  "$KNET_DATASET_TARGET/knowledge-graph-uris.oxl" "$outf"
