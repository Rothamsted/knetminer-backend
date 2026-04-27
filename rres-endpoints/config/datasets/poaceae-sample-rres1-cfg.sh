# This is like poaceae-sample-1-cfg.sh, except it points Neo4j to the RRes location.
# As said elsewhere, this is done in dataset config files, to keep track of which Neo4j 
# was used for each dataset

version_no="$(echo "$KETL_DATASET_VERSION" | sed -E s/'^.*([0-9]+).*'/'\1'/)"
. "$KETL_HOME/config/datasets/$KETL_DATASET_ID-${version_no}-cfg.sh"

export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='2026.04.0'
export NEO4J_HOME="$KNET_SOFTWARE/neo4j-community-$KETL_NEO_VERSION"

# TODO: remove, it's a temp hack to avoid a clash with another running pipeline
# 
function _ketl_get_neo_url ()
{
	host_file="$KETL_OUT/tmp/neo4j-slurm.host"
	if [[ ! -f "$host_file" ]]; then
		# it's probably down, we signal it this way
		echo ''
		return
	fi
  neo_host="$(cat "$host_file")"
  echo "bolt://$neo_host:7688"
}
export -f _ketl_get_neo_url

export KETL_NEO_IDX_PAUSE_TIME=1m
