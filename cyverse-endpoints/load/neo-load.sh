# Reloads a dataset into its own Neo4j server instance.
#
# This is usually based on downloading the Neo4j dump that was created by the RRes pipeline and
# restoring it on the right Cyverse Neo instance. 
# 
# This is a generic version of this script, having dataset-specific variables set by specific
# *-neo-load.sh scripts, in this same directory.
# 
set -e

echo -e "\n\n\tUpgrading Neo4j instance at '$CY_DATASET_NEO_HOME'\n"

cd "$CY_DATASET_NEO_HOME"

echo -e "Downloading the dump\n"
dump_path="$CY_TMP/neo4j.dump"
"$CY_SCRIPTS_HOME/utils/knet-download.sh" "neo4j.dump" "$dump_path"

# Stop it
"$CY_SCRIPTS_HOME/utils/neo-servers-service.sh" stop "$CY_DATASET_NEO_HOME"

echo -e "Uploading Neo4j\n"
./bin/neo4j-admin load --force --from="$dump_path"

# Restart
"$CY_SCRIPTS_HOME/utils/neo-servers-service.sh" start "$CY_DATASET_NEO_HOME"

echo -e "\n\n\tNeo4j Updated\n"
