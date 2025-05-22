# Used to test a real dataset without interfering with actual output

version_no="$(echo "$KETL_DATASET_VERSION" | sed -E s/'^([0-9]+).*'/'\1'/)"

. "$KETL_HOME/config/datasets/fungi-lite-${version_no}-cfg.sh"

# /home/data/knetminer/etl-test/fungi-lite/60/generic/knowledge-network.oxl
# Unfortunately, there isn't consistence, so we can use KETL_DATASET_ID here
oxl_home="$KNET_HOME/etl-test/fungi-lite/${version_no}"
export KETL_SRC_OXL="$oxl_home/generic/knowledge-network.oxl"
export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/$KETL_DATASET_VERSION"


## Neo 
#
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.26.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl-test"
