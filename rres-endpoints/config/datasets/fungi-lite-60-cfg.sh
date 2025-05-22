# /home/data/knetminer/etl-test/fungi-lite/60/generic/knowledge-network.oxl

# Unfortunately, there isn't consistence, so we can use KETL_DATASET_ID here
oxl_home="$KNET_HOME/etl-test/fungi-lite/$KETL_DATASET_VERSION"

export KETL_SRC_OXL="$oxl_home/generic/knowledge-network.oxl"

# Sam 20240909 - New versioning convention
#
# TODO: this is quite inconsistent, the proper way to do it should be:
# - create fungi-lite-60RC1.sh
# - from that file, load ('source') $KETL_HOME/config/datasets/fungi-lite-60-cfg.sh 
#   (or a fungi-lite-common.sh, depends on how much you share across versions)
# - from the same file, override the few things that you need.
#
# See fungi-lite-60TEST-cfg.sh for an example. The idea is that '60RC1' is the version, so 
# you should not need to tweak it further.
# 
# Also, to me the 'v' prefix is completely redundant. 
# 
# You should fix this in all the config files.
# 
export KETL_OUT="$KETL_OUT_HOME/$KETL_DATASET_ID/v$KETL_DATASET_VERSION-RC1"

## Neo 
#
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.23.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl"

## Knet Initialiser
#
# The name within the code base, which identifies the config dir to be
# used for the KnetMiner initialiser
# This is not needed if it's equal to $KETL_DATASET_ID (which is a default)
# export KNET_INIT_DATASET_ID="fungi-lite"

# TODO: This is disabled for Nova, so it can be removed 

##### Values for server-sync.sh
#

## RRes Neo server
#export KNET_NEO_SSH=neo4j@babvs65.rothamsted.ac.uk
#export KNET_NEO_DATA=/opt/data


## RRes Test instances for Knetminer
#

# Test servers like babvs73
export KNET_TESTINST_DATA_PATH=/opt/data/knetminer-datasets/fungi-lite
# babvs73: based on old Traverser, available at knetminer.com/ci-test
# babvs72: based on Neo4j+OXL Traverser, available at knetminer.com/ci-test-cypher
# export KNET_TESTINST_SSH="brandizim@babvs73.rothamsted.ac.uk brandizim@babvs72.rothamsted.ac.uk"
