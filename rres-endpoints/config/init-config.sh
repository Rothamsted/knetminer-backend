param_file="$1"

if [[ -z "$param_file" ]]; then
	echo -e "\n\n\tUsage: $0 <dataset-params in datasets/>\n\n"
	exit 1
fi

. "config/datasets/$param_file"
. "config/common-cfg.sh"
. "config/datasets/${PARAM_DATASET_ID}.sh"
