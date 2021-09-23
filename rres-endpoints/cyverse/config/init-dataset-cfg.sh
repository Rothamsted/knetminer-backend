if [[ $# != 2 ]]; then
	echo -e "\n\n\tUsage: $0 <dataset-id> <dataset-version>\n\n"
	exit 1
fi

export CY_DATASET_ID="$1"
export CY_DATASET_VERSION="$2"

for cfg_prefix in "${CY_DATASET_ID}-${CY_DATASET_VERSION}" "${CY_DATASET_ID}-common"
do 
  cfg_script="config/datasets/${cfg_prefix}-cfg.sh"
	[[ -e "$cfg_script" ]] && . "$cfg_script"
done
. "config/dataset-cfg.sh"
