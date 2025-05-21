# Used to test a real dataset without interfering with actual output

. "$KETL_HOME/config/datasets/ascomycota-premium-60-cfg.sh"

export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/$KETL_DATASET_VERSION"
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl-test"
