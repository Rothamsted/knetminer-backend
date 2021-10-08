
# TODO: document that Docker scripts are at /opt/software/virtuoso-docker
# and can't be put into a public repo due to passwords
#

set -e

echo -e "\n\n\tDownloading Ontologies\n"

onto_dir="$CY_DATA_DIR/rdf/ontologies"
mkdir -p "$onto_dir"

"$CY_SCRIPTS_HOME/utils/knet-download.sh" --multi "rdf/ontologies/" "$onto_dir"

echo -e "\n\n\tReloading Ontologies\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" -r "$onto_dir" "${CY_DATASET_GRAPH_PREFIX}ontologies"

# Define this variable to skip the dataset and work on the agrischemas mappings only.
# This is useful when you do the updates in two stages
# 
if [[ -z "$CY_DATASET_AGSCHEMA_ONLY" ]]; then

	echo -e "\n\n\tDownloading Dataset\n"

	data_dir="$CY_DATA_DIR/rdf/$CY_DATASET_ID"
	mkdir -p "$data_dir"

	"$CY_SCRIPTS_HOME/utils/knet-download.sh" "rdf/knowledge-graph.ttl.bz2" "$data_dir/${CY_DATASET_ID}.ttl.bz2"

	echo -e "\n\n\tReloading the Dataset into Virtuoso\n"
	"$VIRTUOSO_UTILS_HOME/virt_load.sh" "$data_dir" "${CY_DATASET_GRAPH_PREFIX}${CY_DATASET_ID}"
fi

# Agrischemas

[[ -z "$CY_DATASET_AGSCHEMA_ID" ]] && exit 0

echo -e "\n\n\tDownloading Agrischemas mappings\n"
# we need to use its own dir, cause virt_load.sh uses ld_dir(), which only supports dirs
mkdir -p "$data_dir/agrischemas"
"$CY_SCRIPTS_HOME/utils/knet-download.sh" "rdf/agrischemas-map.nt.bz2" "$data_dir/agrischemas/${CY_DATASET_ID}-agrischemas.ttl.bz2"

echo -e "\n\n\tReloading the Agrischemas mappings into Virtuoso\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" "$data_dir/agrischemas" "${CY_DATASET_GRAPH_PREFIX}${CY_DATASET_AGSCHEMA_ID}"
