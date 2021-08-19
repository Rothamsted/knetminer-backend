set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh


echo -e "\nClearing Neo4j at '$NEO4J_HOME'"
config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
rm -Rf "$NEO4J_HOME/data/databases/"* "$NEO4J_HOME/data/transactions/"*
"$NEO4J_HOME/bin/neo4j-admin" set-initial-password "$KNET_NEO_PWD"
config/neo4j/neo-start$KNET_ENV_POSTFIX.sh

cd "$KNET_SCRIPTS_HOME"
rdf_target="$KNET_DATASET_TARGET/rdf"
tdb="$KNET_DATASET_TARGET/tmp/tdb"

# Under SLURM, this is where Neo is running
[[ -e "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host" ]] && is_slurm_neo=true || is_slurm_neo=false 
if `$is_slurm_neo`; then
  neo_host=`cat "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host"`
  export KNET_NEO_URL="bolt://$neo_host:7687"
fi

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$KNET_NEO_URL'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KNET_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KNET_NEO_PWD'"


echo -e "\n\tStarting neo4j-export\n"
"$KNET_NEOEXPORT_HOME/ondex2neo.sh" --tdb "$tdb"

echo -e "\nDone. Waiting before stopping Neo4j\n"
sleep 60
config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh

echo -e "Another pause, just in case.\n"
sleep 20

# TODO: remove
if false && `$is_slurm_neo`; then
  # It seems it's needed
  echo -e "\nOne more restart, needed under SLURM"
  
  sleep 60
  config/neo4j/neo-start$KNET_ENV_POSTFIX.sh

  sleep 60
  config/neo4j/neo-stop$KNET_ENV_POSTFIX.sh
fi

echo -e "\n\tNeo4j Dump to '$KNET_DATASET_TARGET/neo4j.dump'\n"
"$NEO4J_HOME/bin/neo4j-admin" dump --to="$KNET_DATASET_TARGET/neo4j.dump"
