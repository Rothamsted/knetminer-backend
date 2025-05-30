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
export KETL_NEO_VERSION='5.26.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl-test"

# the new KnetMiner Nova Initialiser
# We fit this into the Plants Lite resource, which is rather arbitrary, just to make the initialiser
# work locally and against the Community Neo4j
export KNET_INIT_DATASET_ID="plants-lite"

export KETL_NEO_IDX_PAUSE_TIME=10m # Not much needed with this small DB
export JAVA_TOOL_OPTIONS="-Xmx20G" # Default is huge, we don't need it here

