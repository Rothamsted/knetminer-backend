
# Endpoint building step to add URIs to an OXL. 
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
#   

set -e

in_oxl="$1"
out_oxl="$2"

mkdir -p "`dirname "$out_oxl"`"

echo -e "\n\nAdding URIs to '$in_oxl' and saving to '$out_oxl'"
"$KETL_RDFEXP_HOME/add-uris.sh" "$in_oxl" "$out_oxl"
