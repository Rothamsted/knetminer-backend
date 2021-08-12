set -e
export MY_HOME=`realpath "$0"`
export MY_HOME=`dirname "$MY_HOME"`
cd "$MY_HOME"
. config/init-config.sh

mkdir -p "$CFG_DATASET_TARGET"
outf="$CFG_DATASET_TARGET/knowledge-graph.ttl"
echo -e "\n\nRDF Exporting '$CFG_DATASET_TARGET/knowledge-graph-uris.oxl' to '${outf}.bz2'"
rm -f "${outf}.fifo"
mkfifo "${outf}.fifo"
"$CFG_RDFEXP_HOME/odx2rdf.sh" "$CFG_DATASET_TARGET/knowledge-graph-uris.oxl" "${outf}.fifo" &
bzip2 -c <"${outf}.fifo" >"${outf}.bz2"
rm -f "${outf}.fifo"
