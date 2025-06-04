# Used as an example for the walkthrough example at 
# /pipeline-walkthrough.md

# PLEASE NOTE: this override ALL of the variables that were defined in cereals-dummy-common.sh
# hence, the -common.sh file IS NOT NEEDED in a case like this and you can stick with the old
# pattern of having this versioned files only.
# 
# If you have a -common.sh file, VERSIONED files like the hereby load the common and then
# override/extend definitions that are shared between version numbers.
# NOT VICE VERSA.
#

# Unfortunately, there isn't consistence, so we need the following
oxl_home="$KNET_HOME/etl-test/cereals-dummy/$KETL_DATASET_VERSION_NUM"

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

