set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
rm -Rf "$NEO4J_HOME/data/databases/"* "$NEO4J_HOME/data/transactions/"*
config/neo4j/neo-start$KNET_ENV_POSTFIX.sh

cd "$KNET_SCRIPTS_HOME"
rdf_target="$KNET_DATASET_TARGET/rdf"
tdb="$KNET_DATASET_TARGET/tmp/tdb"
if [[ ! -d "$tdb" ]]; then
  # If it exists, we assume it's already populated and the command below takes it as-is when no RDF is 
  # given.
  
  rdf_files=""$rdf_target/knowledge-graph.ttl.bz2""
fi

# Same for this
if [[ ! -d "$rdf_target/ontologies" ]]; then

  echo -e "\n\tDownloading Ontologies\n"
  
  mkdir -p "$rdf_target/ontologies"
  "$KNET_NEOEXPORT_HOME/get_ontologies.sh" "$rdf_target/ontologies"
	rdf_files="$rdf_files "$rdf_target/ontologies/"*.*"
fi

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$KNET_NEO_URL'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KNET_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KNET_NEO_PWD'"


echo -e "\n\tStarting neo4j-export\n"
"$KNET_NEOEXPORT_HOME/ondex2neo.sh" --tdb "$tdb" $rdf_files

config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh

echo -e "\n\tNeo4j Dump to '$KNET_DATASET_TARGET/neo4j.dump'\n"
"$NEO4J_HOME/bin/neo4j-admin" dump --to="$KNET_DATASET_TARGET/neo4j.dump"
