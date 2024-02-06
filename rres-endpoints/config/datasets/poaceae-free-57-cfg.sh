# /home/data/knetminer/etl-test/poaceae/57/generic/knowledge-network-free.oxl

oxl_home="$RRES_HOME/etl-test/$KETL_DATASET_ID/$KETL_DATASET_VERSION"

export KETL_SRC_OXL="$oxl_home/generic/knowledge-network-free.oxl"

# Neo 
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.15.0'
export NEO4J_HOME="$RRES_SOFTWARE_HOME/neo4j-community-$KETL_NEO_VERSION-etl"

# Knet Initialiser
export KNET_DATASET_ID="poaceae-test"
