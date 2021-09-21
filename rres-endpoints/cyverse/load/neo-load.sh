#
# We use this to populate the Neo4j instances on our Cyverse servers, starting from dump files uploaded on line
# (currently, on OneDrive).
#
# TODO: comment the code
#
set -e
TRACKER=/tmp/neo-load.running

if [[ -e "$TRACKER" ]]; then
  echo -e "\n  Script is already running, as per '$TRACKER', quitting\n"
  exit
fi

echo 1 >"$TRACKER"

export NEO4J_HOME=${NEO4J_HOME:=/opt/software/neo4j}
# You can also use NEO_TAR_OPTS

echo -e "\n\n\tUpgrading Neo4j instance\n"

cd "$NEO4J_HOME"

echo -e "Stopping Neo4j\n"
./bin/neo4j stop

echo -e "Downloading the dump\n"
dump_path=/tmp/neo4j.dump
"$CY_SCRIPTS_HOME/utils/knet-download.sh" "neo4j.dump" "$dump_path"

echo -e "Uploading Neo4j\n"
./bin/neo4j-admin load --force --from="dump_path"

echo -e "Restarting Neo4j\n"
ulimit -n 40000
./bin/neo4j start

rm -f "$TRACKER"
echo -e "\n\n\tNeo4j Updated\n"
