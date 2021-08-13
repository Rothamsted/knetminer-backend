set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

rdf_target="$KNET_DATASET_TARGET/rdf"
mkdir -p "$rdf_target"
outf="$rdf_target/knowledge-graph.ttl"

echo -e "\n\nRDF Exporting '$KNET_DATASET_TARGET/knowledge-graph-uris.oxl' to '${outf}.bz2'"
rm -f "${outf}.fifo"
mkfifo "${outf}.fifo" # no stdout available here, so the usual trick 
"$KNET_RDFEXP_HOME/odx2rdf.sh" "$KNET_DATASET_TARGET/knowledge-graph-uris.oxl" "${outf}.fifo" &
bzip2 -c <"${outf}.fifo" >"${outf}.bz2"
rm -f "${outf}.fifo"
