. "$KETL_HOME/config/datasets/${KETL_DATASET_ID}-common.sh"
export -f ketl_get_neo_url
export KETL_NEO_IDX_PAUSE_TIME=20m

# ROGER test: dedicated Neo4j home so neo-init does not wipe a shared/production DB.
if [[ "$KETL_ENVIRONMENT" == "roger" ]]; then
  # honor ENDPOINT_ISOLATE's private home (KETL_NEO_HOME_OVERRIDE) for parallel neo on GPU nodes;
  # else the shared roger install. Must NOT hardcode, or concurrent jobs collide on one DB dir.
  export NEO4J_HOME="${KETL_NEO_HOME_OVERRIDE:-$KNET_HOME/etl-test/temp/neo4j-roger/neo4j-community-2026.05.0}"
fi
