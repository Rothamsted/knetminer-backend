# This is like poaceae-sample-1-cfg.sh, except it points Neo4j to the RRes location.
# As said elsewhere, this is done in dataset config files, to keep track of which Neo4j 
# was used for each dataset

version_no="$(echo "$KETL_DATASET_VERSION" | sed -E s/'^([0-9]+).*'/'\1'/)"
. "$KETL_HOME/config/datasets/$KETL_DATASET_ID-$version_no-cfg.sh"

export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.26.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION-etl"
