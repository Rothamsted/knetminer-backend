# Creates a dump of the Neo4j database
#
set -e

neo_dump="$2" # The output of this step is the Neo dump, ready for deployment

neo_url=`ketl_get_neo_url`

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$neo_url'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KETL_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KETL_NEO_PWD'"

echo -e "\n\n  Waiting before stopping Neo4j\n"
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
echo -e "\n  Neo4j Dump to '$neo_dump'\n"
"$NEO4J_HOME/bin/neo4j-admin" database dump --to-stdout neo4j >"$neo_dump"

echo -e "\n  Neo4j Dump done\n"
