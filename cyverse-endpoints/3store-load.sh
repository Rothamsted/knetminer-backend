# Loads the RDF about a dataset into the Cyverse Virtuoso.
#

set -e

# Needs an environment
[[ -z "$CY_SCRIPTS_HOME" ]] && { 
	echo "CY_SCRIPTS_HOME not set, 'source' some config/environments/*-env.sh script"
	exit 1 
}

cd "$CY_SCRIPTS_HOME"

# Check that dataset-id and version are passed to the CLI and initialised the 
# dataset-depending config
# 
. config/init-dataset-cfg.sh

# Do it, by running the dataset-specific flavour.
./load/$CY_DATASET_ID-3sload.sh
