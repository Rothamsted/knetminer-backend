# See resources-init.sh
set -e
cd “`dirname $0`"
mvn --projects test-data-server -P server-mode neo4j-server:stop
