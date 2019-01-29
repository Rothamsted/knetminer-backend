cd "$(dirname $0)"
goal="${1-verify}" 
mvn --projects test-data-server pre-integration-test
mvn --projects '!test-data-server' $goal
mvn --projects test-data-server neo4j-server:stop
