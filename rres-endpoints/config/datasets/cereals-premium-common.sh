oxl_home="$KNET_HOME/etl-test/$KETL_DATASET_ID/$KETL_DATASET_VERSION_NUM"
export KETL_SRC_OXL="$oxl_home/generic/knowledge-network.oxl"
export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/$KETL_DATASET_VERSION"

export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='2026.04.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION"
