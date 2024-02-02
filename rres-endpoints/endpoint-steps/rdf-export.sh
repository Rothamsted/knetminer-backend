# Endpoint building step to export the dataset's OXL to RDF.
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
# It assumes the input for the tool is the URI-equipped OXL, which was produced from the 
# previous add-uri step (or from the outside the pipeline, eg, the Ondex Mini Workflow, see 
# notes on the Snakemake file).
#

set -e

rdf_out="$KETL_OUT/rdf"
mkdir -p "$rdf_out"
outf="$rdf_out/knowledge-graph.ttl"

echo -e "\n\nRDF Exporting '$KETL_OUT/knowledge-graph-uris.oxl' to '${outf}.bz2'"
rm -f "${outf}.fifo"
mkfifo "${outf}.fifo" # no stdout available here, hence we need the Unix FIFO to pipe things lazily. 
"$KETL_RDFEXP_HOME/odx2rdf.sh" "$KETL_OUT/knowledge-graph-uris.oxl" "${outf}.fifo" &
bzip2 -c <"${outf}.fifo" >"${outf}.bz2"
rm -f "${outf}.fifo"
