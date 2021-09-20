set -e

echo -e "\n\n\tDownloading Ontologies\n"

onto_dir="$CY_DATA_DIR/ontologies"
mkdir -p "$onto_dir"

"$CY_SCRIPTS_HOME/utils/knet-download.sh" "rdf/ontologies/" "$onto_dir"

echo -e "\n\n\tReloading Ontologies\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" -r "$onto_dir" "${CY_DATASET_GRAPH_PREFIX}ontologies"


echo -e "\n\n\tDownloading Dataset\n"

data_dir="$CY_DATA_DIR/$CY_DATASET_ID"
mkdir -p "$data_dir"

"$CY_SCRIPTS_HOME/utils/knet-download.sh" "rdf/knowledge-graph.ttl.bz2" "$data_dir/${CY_DATASET_ID}.ttl.bz2"

echo -e "\n\n\tReloading the Dataset into Virtuoso\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" "$data_dir" "${CY_DATASET_GRAPH_PREFIX}${CY_DATASET_ID}"
