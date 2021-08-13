cd "$NEO4J_HOME"
bin/neo4j stop
rm -Rf data/databases/* data/transactions/*
bin/neo4j start
