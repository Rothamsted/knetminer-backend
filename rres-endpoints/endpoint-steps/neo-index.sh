# Runs the KnetMiner initialising tools to create semantic motif data, indexes and other 
# database initliasiation.
#
# This is the version based on the new KnetMiner Nova API
#
set -e

out_flag="$1" # Creates this file to signal that the step was successfully completed

neo_url=$(ketl_get_neo_url)

if [[ -z "$neo_url" ]]; then
  # This happens when you restart a partially completed pipeline from here.
  printf "Neo4j server is down, restarting it\n"
  "$KETL_NEO_START"
  neo_url=$(ketl_get_neo_url)
  
  # We noticed unability to connect straight after this startup, so let's hope for the best
  # with yeat another pause (the startup script waits to see the job in squeue)
  sleep 30s
fi

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

# --java-opts '' disables the default -Xmx2G and let it to use JAVA_TOOL_OPTIONS 
./init-dataset.sh --config-dir configs/knetminer --config-file classpath:/api-etl-writer.yml \
  --java-opts '' --resource "$KNET_INIT_DATASET_ID" 


# Sam 2024/09/13: Run the Cypher query to generate stats node in Neo4j
# TODO: Likely, it needs review and upgrades to Nova, so for now it's disabled
# Run stats fail-soft: it's the final cosmetic step (builds :Metadata/:Summary nodes
# only), so a failure here must NOT discard the hours of indexing done above. The graph
# and semantic-motif data are already in place; the flag is still written so Snakemake
# does not re-run the whole (very expensive) neo_index rule.
if ! "$KETL_HOME/utils/neo4j/neo-stats.sh"; then
  echo -e "\n\n  WARNING: neo-stats.sh failed - :Metadata/:Summary nodes may be missing/stale." >&2
  echo -e "  The graph and semantic-motif data are unaffected; re-run neo-stats.sh once fixed.\n\n" >&2
fi

echo -e "\nAll Neo4j indexing and stats generation done\n"
echo $(date) >"$out_flag"
