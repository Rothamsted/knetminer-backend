# Runs the KnetMiner initialising tools to create semantic motif data, indexes and other 
# database initliasiation.
#
# This is the version based on the new KnetMiner Nova API
#
set -e

out_flag="$1" # Creates this file to signal that the step was successfully completed

neo_url=`ketl_get_neo_url`


printf "\n\n  Running the Neo4j Nova Initialiser\n\n"
printf "\n  ====> MAKE SURE $KNET_INITIALIZER_HOME IS UPDATED!!!\n\n"

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
# TODO: Why 'source'?!
# TODO: Likely, it needs review and upgrades to Nova
#source "$KETL_HOME/utils/neo4j/neo-stats.sh"

echo -e "\nAll Neo4j indexing and stats generation done\n"
echo `date` >"$out_flag"
