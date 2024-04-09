# Creates dataset metadata
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
# It assumes the input for the tool is the URI-equipped OXL.
#

set -e

in_oxl="$1"
out_oxl="$2" # Annotated OXL
out_rdf="$3" # Metadata RDF

mkdir -p \
	"`dirname "$out_oxl"`"\
	"`dirname "$out_rdf"`"

echo -e "\n\nRDF Generating dataset descriptor into:\n  '$out_oxl'\n  '$out_rdf'"
"$KETL_RDFEXP_HOME/oxl-descriptor.sh" \
  --template "$KETL_RDFEXP_HOME/knetminer-descriptors/knetminer-metadata-template.ttl" \
  --config "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION}-metadata-descriptor.properties" \
  --export "$out_rdf" "$in_oxl" "$out_oxl"
