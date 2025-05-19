set -e

# Deletes an existing DB and prepares an empty one.
# This default version assumes you have Neo4j community and it deletes all the data files
# 
# TODO: a version for the enterprise edition
#

echo -e "\nClearing Neo4j at '$NEO4J_HOME'"
"$KETL_NEO_STOP"
rm -Rf "$NEO4J_HOME/data/databases/"* "$NEO4J_HOME/data/transactions/"*
"$NEO4J_HOME/bin/neo4j-admin" dbms set-initial-password "$KETL_NEO_PWD"
"$KETL_NEO_START"
