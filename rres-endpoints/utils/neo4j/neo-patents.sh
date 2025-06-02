set -e

# This is an experimental script to generate patent nodes in Neo4j.
# It will be called from neo-index.sh after being fully tested.

printf "\nRunning Cypher query to generate patent nodes in Neo4j\n"
current_date=$(date +%Y-%m-%d)
export PATNETS_FILE="$KNET_HOME/etl-test/knet-pipelines/experiment/Patent/processed_file.csv" 

query="
MATCH (p:PatentDescription) DETACH DELETE p;

DROP CONSTRAINT PatentDescription_ondexId IF EXISTS;

CREATE CONSTRAINT PatentDescription_ondexId IF NOT EXISTS
FOR (x:PatentDescription)
REQUIRE x.ondexId IS UNIQUE;

MATCH (n)
WITH MAX(toInteger(n.ondexId)) AS maxId
CREATE (p:PatentDescription {ondexId: maxId})
SET p.headline = NULL,
p.abstract = NULL,
p.authorsList = NULL,
p.creatorsList = NULL,
p.copyrightHolder = NULL,
p.countryOfOrigin = NULL,
p.dateCreated = NULL,
p.datePublished = NULL,
p.publicationKindLabel = NULL,
p.citedPatentsCount = NULL,
p.citedByPatentsCount = NULL,
p.patentLegalStatus = NULL,
p.patentDisplayKey = NULL,
p.patentApplicationNumber = NULL;

LOAD CSV WITH HEADERS 
FROM '${PATNETS_FILE}' AS row //temporary file location
WITH row
MATCH (a:PatentDescription)
CALL apoc.atomic.add(a, 'ondexId', 1) YIELD newValue
MERGE (p:PatentDescription {ondexId: newValue})
SET p.headline = row.Title,
p.abstract = row.Abstract,
p.authorsList = split(row.Inventors, ';;'),
p.creatorsList = split(row.Applicants, ';;'),
p.copyrightHolder = split(row.Owners, ';;'),
p.countryOfOrigin = row.Jurisdiction,
//p.dateCreated = date(row.\`Application Date\`), //TODO: Fix dates
//p.datePublished = date(row.\`Publication Date\`),
p.dateCreated = row.\`Application Date\`,
p.datePublished = row.\`Publication Date\`,
p.publicationKindLabel = row.\`Document Type\`,
p.citedPatentsCount = row.\`Cites Patent Count\`,
p.citedByPatentsCount = row.\`Cited by Patent Count\`,
p.patentLegalStatus = row.\`Legal Status\`,
p.patentDisplayKey = row.\`Display Key\`,
p.patentApplicationNumber = row.\`Application Number\`;

LOAD CSV WITH HEADERS
FROM 'https://drive.google.com/uc?export=download&id=11ib0bW7ABYO-X-cJA9TIMMCY5t8xz0Z1' AS row
MATCH (g:Gene {identifier: row.GeneID})
MATCH (p:PatentDescription {patentDisplayKey: row.\`Display Key\`})
CREATE (p)-[r:about]->(g)
SET
r.start = p.ondexId,
r.end = g.ondexId;

MATCH (p:PatentDescription)-[:about]->()
WITH DISTINCT p SET p:Concept;

MATCH (p:PatentDescription)
WHERE NOT EXISTS {
    MATCH (p)-[]-(:Gene)
}
DETACH DELETE p;
"

$NEO4J_HOME/bin/cypher-shell -a "$neo_url" -u "$KETL_NEO_USR" -p "$KETL_NEO_PWD" --format plain "$query"

echo -e "\nPatent nodes and their relationships created successfully.\n"
echo `date` >"$out_flag"