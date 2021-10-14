# TODO: document that Docker scripts are at /opt/software/virtuoso-docker
# and can't be put into a public repo due to passwords
#

set -e

data_dir="$CY_DATA_DIR/rdf/$CY_DATASET_ID"
mkdir -p "$data_dir"

"$CY_SCRIPTS_HOME/utils/knet-download.sh" "human-covid19-rdf.tar.bz2" "$data_dir/human-covid19-rdf.tar.bz2"

echo -e "\n\n\tReloading the Dataset into Virtuoso\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" "$data_dir" "${CY_DATASET_GRAPH_PREFIX}human-covid19"
