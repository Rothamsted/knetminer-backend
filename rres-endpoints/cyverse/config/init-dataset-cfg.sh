if [[ $# != 2 ]]; then
	echo -e "\n\n\tUsage: $0 <dataset-id> <dataset-version>\n\n"
	exit 1
fi

export CY_DATASET_ID="$1"
export CY_DATASET_VERSION="$2"

. "config/datasets/${CY_DATASET_ID}-${CY_DATASET_VERSION}-cfg.sh"
. "config/dataset-cfg.sh"
