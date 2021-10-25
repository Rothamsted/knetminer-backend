# Endpoint building step to export the dataset's OXL to RDF.
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
# It assumes the input for the tool is the URI-equipped OXL, which was produced from the 
# previous add-uri step (or from the outside the pipeline, eg, the Ondex Mini Workflow, see 
# notes on the Snakemake file).
#

set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

rdf_target="$KNET_DATASET_TARGET/rdf"
mkdir -p "$rdf_target"
outf="$rdf_target/knowledge-graph.ttl"

echo -e "\n\nRDF Exporting '$KNET_DATASET_TARGET/knowledge-graph-uris.oxl' to '${outf}.bz2'"
rm -f "${outf}.fifo"
mkfifo "${outf}.fifo" # no stdout available here, hence we need the Unix FIFO to pipe things lazily. 
"$KNET_RDFEXP_HOME/odx2rdf.sh" "$KNET_DATASET_TARGET/knowledge-graph-uris.oxl" "${outf}.fifo" &
bzip2 -c <"${outf}.fifo" >"${outf}.bz2"
rm -f "${outf}.fifo"
