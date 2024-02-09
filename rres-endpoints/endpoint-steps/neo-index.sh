# Runs the KnetMiner initialising tools to create indexes and other data for the 
# new KnetMiner API
set -e

oxl_src="$1" # the OXL with URIs (mandatory) and metadata annotations (optional)
out_flag="$2" # Creates this file to signal that the step was successfully completed

neo_url=`ketl_get_neo_url`

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$neo_url'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KETL_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KETL_NEO_PWD'"


printf "\n\n  Creating KnetMiner initialisation files\n\n"
knet_cfg="$KETL_OUT/tmp/knet-init"
mkdir -p "$knet_cfg"
"$KNET_WEBAPP/docker/dataset-init.sh" --force "$knet_cfg" "$KNET_DATASET_ID"
cp -R -v "$KETL_HOME/config/knet-init"/* "$knet_cfg/config"

printf "\n\n  Creating Neo full-text index for concpept searching\n\n"
"$KNET_INITIALIZER_HOME/knet-init.sh" \
-c "$knet_cfg/config/config-etl.yml" --neo-index=config:// --in "$oxl_src"

# This is provisional, we need it until we can replace it with the new traverser
printf "\n\n  Creating Semantic Motif Links\n\n"
"$KNET_INITIALIZER_HOME/knet-init.sh" \
  -c "$knet_cfg/config/config-etl.yml" --neo-motifs --neo-url=config:// --in "$oxl_src"

echo -e "\nAll Neo4j indexing done\n"
echo `date` >"$out_flag"
