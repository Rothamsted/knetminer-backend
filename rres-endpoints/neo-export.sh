set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
echo -e "\nClearing Neo4j at '$NEO4J_HOME'"
rm -Rf "$NEO4J_HOME/data/databases/"* "$NEO4J_HOME/data/transactions/"*
"$NEO4J_HOME/bin/neo4j-admin" set-initial-password test
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

# Under SLURM, this is where Neo is running
[[ -e "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host" ]] && is_slurm=true || is_slurm=false 
if `$is_slurm_neo`; then
  neo_host=`cat "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host"`
  export KNET_NEO_URL="bolt://$neo_host:7687"
fi

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$KNET_NEO_URL'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KNET_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KNET_NEO_PWD'"


echo -e "\n\tStarting neo4j-export\n"
"$KNET_NEOEXPORT_HOME/ondex2neo.sh" --tdb "$tdb" $rdf_files

config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh

if `$is_slurm_neo`; then
  # It seems it's needed
  echo -e "\nOne more restart, needed under SLURM"
  config/neo4j/neo-start$KNET_ENV_POSTFIX.sh
  sleep 60
  config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
fi

echo -e "\n\tNeo4j Dump to '$KNET_DATASET_TARGET/neo4j.dump'\n"
"$NEO4J_HOME/bin/neo4j-admin" dump --to="$KNET_DATASET_TARGET/neo4j.dump"
