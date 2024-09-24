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

# Comment this to skip the traverser. BE CAREFUL
neo_motifs_flag='--neo-motifs'
"$KNET_INITIALIZER_HOME/knet-init.sh" \
-c "$knet_cfg/config/config-etl.yml" --neo-index=config:// $neo_motifs_flag --in "$oxl_src"


# Sam 2024/09/13: Run the Cypher query to generate stats node in Neo4j
printf "\nRunning Cypher query to generate stats node in Neo4j\n"

# TODO: this is too much stuff to stay here, move it to its own script

# TODO: this is wrong, you're hardwiring the Neo version and location
# and that's useless anyway, since you already have $NEO4J_HOME.
# 
# Also, it's not worth to change PATH in a script, you can obtain the same
# in a cleaner way via:
#
#   "$NEO4J_HOME/bin/cypher-shell"
#
# or, if you need it often:
# 
#   cycmd="$NEO4J_HOME/bin/cypher-shell"
#   "$cycmd" -a "$running_host" ...
#
export PATH=$PATH:$KNET_SOFTWARE/neo4j-community-5.20.0-etl/bin

# TODO: remove. This is alredy available in "$neo_url", which was already used
# above.
# running_host=bolt://`cat "$KETL_OUT/tmp/neo4j-slurm.host"`:7687

current_date=$(date +%Y-%m-%d)

# TODO: Probably you want to count (n:Concept) - [] -> (m:Concept), not all 
# the graph
# TODO: why toString()? Shouldn't they be numeric properties?
# TODO: to be reviewed against https://schema.org/Dataset ?
query="MATCH (n)-[r]-()
WITH count(distinct n) AS nodeCount, count(distinct r) AS edgeCount
CREATE (s:Metadata { 
    nodeCount: toString(nodeCount),
    edgeCount: toString(edgeCount),
    version: \"${KETL_DATASET_VERSION}\",
    fileLocation: \"s3://knet-data-store/${KETL_DATASET_ID}/v${KETL_DATASET_VERSION}-RC1/neo4j-5.20.0.dump\",
    date: \"${current_date}\"
})"

cypher-shell -a "$neo_url" -u "$KETL_NEO_USR" -p "$KETL_NEO_PWD" --format plain "$query"

echo -e "\nAll Neo4j indexing and stats generation done\n"
echo `date` >"$out_flag"
