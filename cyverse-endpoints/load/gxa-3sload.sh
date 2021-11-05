# We don't use knet-3load.sh, cause we have our peculiarities.
#

set -e

data_dir="$CY_DATA_DIR/rdf/$CY_DATASET_ID"
mkdir -p "$data_dir"

"$CY_SCRIPTS_HOME/utils/knet-download.sh" --multi "/" "$data_dir"

echo -e "\n\n\tReloading the Dataset into Virtuoso\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" "$data_dir" "${CY_DATASET_GRAPH_PREFIX}${CY_DATASET_AGSCHEMA_ID}"
