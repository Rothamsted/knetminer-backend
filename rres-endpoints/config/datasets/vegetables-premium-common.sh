version_no="$(echo "$KETL_DATASET_VERSION" | sed -E 's/[^0-9]*([0-9]+).*/\1/')"

. "$KETL_HOME/config/datasets/vegetables-premium-${version_no}-cfg.sh"

# Unfortunately, there isn't consistence, so we can use KETL_DATASET_ID here
oxl_home="$KNET_HOME/etl-test/vegetables-premium/${version_no}"
export KETL_SRC_OXL="$oxl_home/generic/knowledge-network.oxl"
export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/$KETL_DATASET_VERSION"


## Neo 
#
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.26.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl-test"

export JAVA_TOOL_OPTIONS="-Xmx64G" # Default is huge, we don't need it here
