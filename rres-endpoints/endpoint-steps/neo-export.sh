# Endpoint building step to populate a Neo4j database with the dataset contents.
#
# This uses the Ondex Neo4j exporter, starting from the dataset's RDF stored in a Jena TDB
# triple store, as it was produced from the previous tdb-load.sh step.
#

# As you can see below. the scripts uses utils to launch a Neo4j server on top of the RRes SLURM cluster.
# eventually, it stops such DB and uses the Neo4j dump command to place a Neo4j dump on the dataset's 
# output directory. 
#
set -e

tdb="$1"
neo_dump="$2"

echo -e "\nClearing Neo4j at '$NEO4J_HOME'"
"$KETL_NEO_STOP"
rm -Rf "$NEO4J_HOME/data/databases/"* "$NEO4J_HOME/data/transactions/"*
"$NEO4J_HOME/bin/neo4j-admin" set-initial-password "$KNET_NEO_PWD"
"$KETL_NEO_START"

rdf_target="$KETL_OUT/rdf"
neo_url=`ketl_get_neo_url`

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$ketl_get_neo_url'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KETL_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KETL_NEO_PWD'"


echo -e "\n\tStarting neo4j-export\n"
"$KETL_NEOEXPORT_HOME/ondex2neo.sh" --tdb "$tdb"

echo -e "\nDone. Waiting before stopping Neo4j\n"
sleep 60
"$KETL_NEO_STOP"

echo -e "Another pause, just in case.\n"
sleep 20

# TODO: remove, seemed to be needed in the past, now it should be fixed.
# WARNING: it is broken anyway, we don't use is_slurm_neo anymore, move it
# to neo-stop-slurm.sh
# 
if false && `$is_slurm_neo`; then
  echo -e "\nOne more restart, needed under SLURM"
  
  sleep 60
  "$KETL_NEO_START"

  sleep 60
  "$KETL_NEO_STOP"
fi

# TODO: review the options in $NEO4J_HOME about the transaction log retentions, we need to get rid 
# of any past transactions, else, the dump is much bigger than necessary. 
#
echo -e "\n\tNeo4j Dump to '$neo_dump'\n"
"$NEO4J_HOME/bin/neo4j-admin" dump --to="$neo_dump"
