# Endpoint building step to export the dataset's OXL to RDF.
#
# This is a wrapper of the corresponding tool in the Ondex RDF exporter (TODO: link).
# It assumes the input for the tool is the URI-equipped OXL, which was produced from the 
# previous add-uri step (or from the outside the pipeline, eg, the Ondex Mini Workflow, see 
# notes on the Snakemake file).
#

set -e

in_oxl="$1"
out_rdf="$2"

out_dir="`dirname "$out_rdf"`"
mkdir -p "$out_dir"

tmp_dir="$ETL_OUT/tmp"
mkdir -p "$tmp_dir"
fifo="$tmp_dir/rdf-export.fifo"

echo -e "\n\nRDF Exporting '$in_oxl' to '$out_rdf'"
rm -f "$fifo"
mkfifo "$fifo" # no stdout available here, hence we need the Unix FIFO to pipe things lazily. 
"$KETL_RDFEXP_HOME/odx2rdf.sh" "$in_oxl" "$fifo" &
bzip2 -c <"$fifo" >"$out_rdf"
rm -f "$fifo"
