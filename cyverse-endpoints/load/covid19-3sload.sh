# We don't use knet-3load.sh, cause we have our peculiarities.
#
set -e

data_dir="$CY_DATA_DIR/rdf/$CY_DATASET_ID"
mkdir -p "$data_dir"

# TODO: ontologies/ being missed, they're the usual ones.
#
"$CY_SCRIPTS_HOME/utils/knet-download.sh" "rdf/knowledge-graph.ttl.bz2" "$data_dir/human-covid19-rdf.tar.bz2"

echo -e "\n\n\tReloading the Dataset into Virtuoso\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" "$data_dir" "${CY_DATASET_GRAPH_PREFIX}human-covid19"
