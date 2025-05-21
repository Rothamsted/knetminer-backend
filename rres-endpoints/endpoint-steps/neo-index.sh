# Runs the KnetMiner initialising tools to create semantic motif data, indexes and other 
# database initliasiation.
#
# This is the version based on the new KnetMiner Nova API
#
set -e

out_flag="$1" # Creates this file to signal that the step was successfully completed

neo_url=$(ketl_get_neo_url)


printf "\n\n  Running the Neo4j Nova Initialiser\n\n"
printf "\n  You may want to check that $KNET_INITIALIZER_HOME is updated (usually done automatically by the API CI build)\n\n"

cd "$KNET_INITIALIZER_HOME"

# WARNING! Given this set of params, $KNET_INIT_DATASET_ID MUST be defined in 
# $KNET_INITIALIZER_HOME/configs/knetminer/api-dev.yml
# 
# 

export KNET_NEO_URI="$neo_url"
export KNET_NEO_USER="$KETL_NEO_USR"
export KNET_NEO_PWD="$KETL_NEO_PWD"

./init-dataset.sh --config-dir configs/knetminer --config-file classpath:/api-etl-writer.yml \
  --resource "$KNET_INIT_DATASET_ID" 


# Sam 2024/09/13: Run the Cypher query to generate stats node in Neo4j
# TODO: Likely, it needs review and upgrades to Nova, so for now it's disabled
# TODO: 'source' was removed, likely, it was only used to get neo_url, but the proper way
# to get this is $(ketl_get_neo_url) (see the script).
"$KETL_HOME/utils/neo4j/neo-stats.sh"

echo -e "\nAll Neo4j indexing and stats generation done\n"
echo `date` >"$out_flag"
