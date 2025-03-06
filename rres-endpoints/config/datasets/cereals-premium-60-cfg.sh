
# Unfortunately, there isn't consistence, so we can use KETL_DATASET_ID here
oxl_home="$KNET_HOME/etl-test/cereals-premium/$KETL_DATASET_VERSION"

export KETL_SRC_OXL="$oxl_home/generic/knowledge-network.oxl"

# Sam 20240909 - New versioning convention
export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/v$KETL_DATASET_VERSION-RC1"

## Neo 
#
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.26.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl"

## Knet Initialiser
#
# The name within the code base, which identifies the config dir to be
# used for the KnetMiner initialiser
export KNET_INIT_DATASET_ID="cereals-premium"


##### Values for server-sync.sh
#

## RRes Neo server
#export KNET_NEO_SSH=neo4j@babvs65.rothamsted.ac.uk
#export KNET_NEO_DATA=/opt/data


## RRes Test instances for Knetminer
#

# Test servers like babvs73
#export KNET_TESTINST_DATA_PATH=/opt/data/knetminer-datasets/poaceae-ci
# babvs73: based on old Traverser, available at knetminer.com/ci-test
# babvs72: based on Neo4j+OXL Traverser, available at knetminer.com/ci-test-cypher
#export KNET_TESTINST_SSH="brandizim@babvs73.rothamsted.ac.uk brandizim@babvs72.rothamsted.ac.uk"
