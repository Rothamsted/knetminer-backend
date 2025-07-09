# The OXL is somewhere like:
#   /home/data/knetminer/etl-test/brassicaceae-premium/61/generic/knowledge-network.oxl
# Unfortunately, there isn't consistence, so we need the following

oxl_home="$KNET_HOME/etl-test/$KETL_DATASET_ID/$KETL_DATASET_VERSION_NUM"

export KETL_SRC_OXL="$oxl_home/generic/knowledge-network.oxl"

export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/$KETL_DATASET_VERSION"

## Neo 
#
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.26.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl"


# TODO: to be remove, we don't deploy on RRes anymore

##### Values for server-sync.sh
#

## RRes Neo server
#export KNET_NEO_SSH=neo4j@babvs65.rothamsted.ac.uk
#export KNET_NEO_DATA=/opt/data


## RRes Test instances for Knetminer
#

# Test servers like babvs73
#export KNET_TESTINST_DATA_PATH=/opt/data/knetminer-datasets/brassicaceae-premium
# babvs73: based on old Traverser, available at knetminer.com/ci-test
# babvs72: based on Neo4j+OXL Traverser, available at knetminer.com/ci-test-cypher
#export KNET_TESTINST_SSH="brandizim@babvs73.rothamsted.ac.uk brandizim@babvs72.rothamsted.ac.uk"
