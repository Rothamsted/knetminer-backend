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
printf "\n  ====> MAKE SURE $KNET_INITIALIZER_HOME IS UPDATED!!!\n\n"

knet_cfg="$KETL_OUT/tmp/knet-init"
# Re-creating it all is the safest option, comment this at your own risk, and 
# DO NOT push the commented version back to github (or leave it in the RRes file
# system).
rm -Rf "$knet_cfg"
mkdir -p "$knet_cfg"

"$KNET_WEBAPP/docker/dataset-init.sh" --force "$knet_cfg" "$KNET_INIT_DATASET_ID"
cp -R -v "$KETL_HOME/config/knet-init"/* "$knet_cfg/config"

# This does all of base indexing, --neo-index and --neo-motifs in one go (in this order).
# --neo-motifs is provisional, we need it until we can replace it with the new traverser.
#
printf "\n\n  Creating Neo indexing (full-text and semantic motifs)\n\n"
"$KNET_INITIALIZER_HOME/knet-init.sh" \
-c "$knet_cfg/config/config-etl.yml" --neo-index=config:// --neo-motifs --in "$oxl_src"


echo -e "\nAll Neo4j indexing done\n"
echo `date` >"$out_flag"
