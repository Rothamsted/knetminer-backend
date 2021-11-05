# Loads the Neo4j dump about a dataset into the its own Cyverse Neo4j instance.
#

set -e

# We need a manually-initialised environment
# 
[[ -z "$CY_SCRIPTS_HOME" ]] && { 
	echo "CY_SCRIPTS_HOME not set, 'source' some config/environments/*-env.sh script"
	exit 1 
}

cd "$CY_SCRIPTS_HOME"

# We need the dataset id and version and then we can initialise dataset-specific config. 
. config/init-dataset-cfg.sh

# And finally run the dataset-specific Neo loader. 
./load/$CY_DATASET_ID-neo-load.sh
