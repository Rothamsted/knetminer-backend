# Runs the KnetMiner initialising tools to create indexes and other data for the 
# new KnetMiner API
set -e

oxl_src="$1"
neo_dump="$2" # The output of this step is the Neo dump, ready for deployment

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$neo_url'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KETL_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KETL_NEO_PWD'"


printf "\n\n  Creating KnetMiner initialisation files\n\n"
knet_cfg="$KETL_OUT/tmp/knet-init"
mkdir -p "$knet_cfg"
"$KNET_HOME/docker/dataset-init.sh" --force "$knet_cfg" "$KNET_DATASET_ID"
cp -R -v "$KETL_HOME/config/knet-init"/* "$knet_cfg/config"

printf "\n\n  Creating Neo full-text index for concpept searching\n\n"
"$KNET_INITIALIZER_HOME/knet-init.sh" \
-c "$knet_cfg/config/config-etl.yml" --neo-index=config:// --in "$oxl_src"

# This is provisional, we need it until we can replace it with the new traverser
printf "\n\n  Creating Semantic Motif Links\n\n"
"$KNET_INITIALIZER_HOME/knet-init.sh" \
  -c "$knet_cfg/config/config-etl.yml" --neo-motifs --neo-url=config:// --in "$oxl_src"

echo -e "\nAll Neo4j indexing done. Waiting before stopping Neo4j\n"
sleep 60
"$KETL_NEO_STOP"

echo -e "Another pause, just in case.\n"
sleep 20

# TODO: remove, seemed to be needed in the past, now it should be fixed.
# WARNING: it is broken anyway, we don't use is_slurm_neo anymore, move it
# to neo-stop-slurm.sh
# 
if false && `$is_slurm_neo`; then
  echo -e "\nOne more restart, needed under SLURM"
  
  sleep 60
  "$KETL_NEO_START"

  sleep 60
  "$KETL_NEO_STOP"
fi

# TODO: review the options in $NEO4J_HOME about the transaction log retentions, we need to get rid 
# of any past transactions, else, the dump is much bigger than necessary. 
#
echo -e "\n\tNeo4j Dump to '$neo_dump'\n"
"$NEO4J_HOME/bin/neo4j-admin" database dump --to-stdout neo4j >"$neo_dump"
