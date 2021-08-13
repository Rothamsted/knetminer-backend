set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
rm -Rf "$NEO4J_HOME/data/databases/"* "$NEO4J_HOME/data/transactions/"*
config/neo4j/neo-start$KNET_ENV_POSTFIX.sh

cd "$KNET_SCRIPTS_HOME"
rdf_target="$KNET_DATASET_TARGET/rdf"
tdb="$rdf_target/tdb"
if [[ ! -e "$tdb" ]]; then
  # If it exists, we assume it's already populated and the command below takes it as-is when no RDF is 
  # given.
  # Also, the operations below are done once only
  
  # Same for this
  echo "Downloading Ontologies"
  mkdir -p "$rdf_target/ontologies"
  "$KNET_NEOEXPORT_HOME/get_ontologies.sh" "$rdf_target/ontologies"
  
  rdf_files="$KNET_DATASET_TARGET/rdf/knowledge-graph.ttl.bz2"
 	rdf_files="$rdf_files ontologies/*"
fi

export KNET_NEO_URL="bolt://localhost:7687"
export KNET_NEO_USR="neo4j"
export KNET_NEO_PWD="test"

# TODO: wrong, need to be passed as Java opts, and the rdf2neo.sh script need to be
# reviewed.
export neo4j.boltUrl="$KNET_NEO_URL"
export neo4j.user="$KNET_NEO_USR"
export neo4j.password="$KNET_NEO_PWD"

"$KNET_NEOEXPORT_HOME/ondex2neo.sh" --tdb "$KNET_DATASET_TARGET/tdb" "$rdf_files"

config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
