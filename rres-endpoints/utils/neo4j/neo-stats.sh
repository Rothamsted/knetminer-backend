printf "\nRunning Cypher query to generate stats node in Neo4j\n"
current_date=$(date +%Y-%m-%d)

# TODO: to be reviewed against https://schema.org/Dataset ?
query="MATCH (n)-[r]-()
WITH count(distinct n) AS nodeCount, count(distinct r) AS edgeCount
CREATE (s:Metadata { 
    nodeCount: nodeCount,
    edgeCount: edgeCount,
    version: \"${KETL_DATASET_VERSION}\",
    fileLocation: \"s3://knet-data-store/${KETL_DATASET_ID}/v${KETL_DATASET_VERSION}-RC2\",
    date: \"${current_date}\"
})"

$NEO4J_HOME/bin/cypher-shell -a "$neo_url" -u "$KETL_NEO_USR" -p "$KETL_NEO_PWD" --format plain "$query"

echo -e "\nAll Neo4j indexing and stats generation done\n"
echo `date` >"$out_flag"