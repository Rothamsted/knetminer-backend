# Used as an example for the walkthrough example at 
# /pipeline-walkthrough.md

# /home/data/knetminer/etl-test/cereals-dummy/cereals-dummy-1.oxl

# Unfortunately, there isn't consistence, so we can use KETL_DATASET_ID here
oxl_home="$KNET_HOME/etl-test/cereals-dummy"

export KETL_SRC_OXL="$oxl_home/$KETL_DATASET_ID-$KETL_DATASET_VERSION.oxl"

export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/$KETL_DATASET_VERSION"

## Neo
# See default-cfg.sh for details.
#
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.16.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl"

## Knet Initialiser
#
# The name within the code base, which identifies the config dir to be
# used for the KnetMiner initialiser
export KNET_INIT_DATASET_ID="poaceae-test"

# TODO: more to be added.