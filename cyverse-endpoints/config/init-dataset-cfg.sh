# Brings up that part of Cyverse pipeline configuration that depends on a working
# dataset. This is usually invoked as first step by the main script in the CY_SCRIPTS_HOME root 
#

if [[ $# != 2 ]]; then
	echo -e "\n\n\tUsage: $0 <dataset-id> <dataset-version>\n\n"
	exit 1
fi

export CY_DATASET_ID="$1"
export CY_DATASET_VERSION="$2"

for cfg_prefix in "${CY_DATASET_ID}-${CY_DATASET_VERSION}" "${CY_DATASET_ID}-common"
do 
	# eg, config/dataset/poaceae-51-cfg.sh, config/dataset/poaceae-common-cfg.sh
  cfg_script="config/datasets/${cfg_prefix}-cfg.sh"
  
  # If a dataset has a specific configuration here, pick it up
	[[ -e "$cfg_script" ]] && . "$cfg_script"
done

# And then, the common stuff that apply to all datasets (which can use the specific vars defined above)
. "config/dataset-cfg.sh"
