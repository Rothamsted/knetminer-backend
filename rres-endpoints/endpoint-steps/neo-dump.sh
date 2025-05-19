# Creates a dump of the Neo4j database
#
set -e

neo_dump="$1" # The output of this step is the Neo dump, ready for deployment


echo -e "\n\n  Waiting before stopping Neo4j\n"
sleep 30
"$KETL_NEO_STOP"

if [[ "$KETL_ENVIRONMENT" == "rres" ]]; then

	sleep_time=20m  #Needs to be at least 17min for cereals-premium

	cat <<EOT

  *** IMPORTANT ****
  
  We're using Neo4j through SLURM and it seems Neo4j needs
  one more restart with a long pause before the final shutdown, 
  to complete transactions.
  
  So, now we will wait $sleep_time before stopping Neo again.
  
  If you see problems with the dump command, restart Neo manually,
  check "$NEO4J_HOME/logs/debug.log" 
  to ensure the server actually restarted, then run the ETL workflow 
  again, to have this hereby script re-running. 

EOT
  
  "$KETL_NEO_START"

  sleep $sleep_time
  "$KETL_NEO_STOP"
fi

echo -e "\n  Neo4j Dump to '$neo_dump'\n"
# WARNING!!! DO NOT USE --verbose!!! It sends diagnostic messages to the
# standard output!!! See: https://github.com/neo4j/neo4j/issues/13397
"$NEO4J_HOME/bin/neo4j-admin" database dump --to-stdout neo4j >"$neo_dump"

echo -e "\n  Neo4j Dump done\n"
