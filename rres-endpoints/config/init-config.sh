param_prefix="$1"

if [[ -z "$param_prefix" ]]; then
	echo -e "\n\n\tUsage: $0 <param-file-prefix in config/datasets/>\n\n"
	exit 1
fi

common_cfg_file=${KNET_CFG-config/common-cfg.sh}

. "config/datasets/${param_prefix}-params.sh"
. "$common_cfg_file"
. "config/datasets/${PARAM_DATASET_ID}-cfg.sh"
