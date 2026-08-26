. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-common.sh"
export -f ketl_get_neo_url
export KETL_NEO_IDX_PAUSE_TIME=15m

# Version-ID placeholder; common.sh resolves the OXL from etl-test/<id>/<VERSION_NUM>/generic/.

# ROGER guard (2026-07-29). brassicaceae-premium-common.sh predates the ROGER port: it still pins
# KETL_NEO_VERSION=5.26.0 and points NEO4J_HOME at $KNET_SOFTWARE/neo4j-community-5.26.0-etl, whose
# data/ currently holds a 26 GB build. neo-init does `rm -Rf "$NEO4J_HOME/data/databases/"*` before
# every load, so running stage 91 without this guard would DESTROY that database. (fungi-premium and
# vegetables-premium had their -common.sh migrated to 2026.05.0 + the roger path; brassicaceae did not.)
#
# Overridden HERE, after common.sh is sourced, rather than by editing common.sh -- versions 60/61
# still share that file and must keep their existing behaviour.
if [[ "$KETL_ENVIRONMENT" == "roger" ]]; then
  export KETL_NEO_VERSION='2026.05.0'
  # honor ENDPOINT_ISOLATE's private home (KETL_NEO_HOME_OVERRIDE) for parallel neo on GPU nodes;
  # else the shared roger install. Must NOT hardcode, or concurrent jobs collide on one DB dir.
  export NEO4J_HOME="${KETL_NEO_HOME_OVERRIDE:-$KNET_HOME/etl-test/temp/neo4j-roger/neo4j-community-$KETL_NEO_VERSION}"
fi
