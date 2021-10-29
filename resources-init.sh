# Prepares the POM project build, by starting the test Neo4j server with 
# sample data.
#
set -e
cd `$(dirname $0)`
mvn --projects test-data-server -P server-mode pre-integration-test
