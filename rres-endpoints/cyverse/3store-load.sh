set -e
[[ -z "$CY_SCRIPTS_HOME" ]] && { 
	echo "CY_SCRIPTS_HOME not set, 'source' some config/environments/*-env.sh script"
	exit 1 
}

cd "$CY_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

./load/$CY_DATASET_ID-3sload.sh

