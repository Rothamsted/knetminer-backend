printf "\nRunning Cypher query to generate medata and summary nodes in Neo4j\n"
current_date=$(date +%Y-%m-%d)

# TODO: to be reviewed against https://schema.org/Dataset ?
query="
MATCH (n:Metadata) DETACH DELETE n;

MATCH (n)-[r]-()
WITH count(distinct n) AS nodeCount, count(distinct r) AS edgeCount
CREATE (s:Metadata { 
    nodeCount: nodeCount,
    edgeCount: edgeCount,
    version: \"${KETL_DATASET_VERSION}\",
    fileLocation: \"s3://knet-data-store/${KETL_DATASET_ID}/v${KETL_DATASET_VERSION}-RC1\",
    date: \"${current_date}\"
});

MATCH (n:Summary) DETACH DELETE n;

MATCH (n)
UNWIND LABELS(n) as label
WITH label, count(label) as NodeCount
WITH COLLECT({NodeType: label, NodeCount: NodeCount}) as breakdown
CREATE (m:Summary)
SET m.nodeBreakdown = apoc.convert.toJson(breakdown);

MATCH ()-[r]->()
WITH distinct type(r) as EdgeType, count(r) as EdgeCount
WITH COLLECT ({EdgeType: EdgeType, EdgeCount: EdgeCount}) as breakdown
MERGE (m:Summary)
SET m.edgeBreakdown = apoc.convert.toJson(breakdown);

MATCH (protein1:Protein)-[r:ortho]->(protein2:Protein)
WITH protein1.TAXID as TAX1, protein2.TAXID as TAX2, COUNT(*) as HomologyCount
WITH COLLECT ({TAX1: TAX1, TAX2: TAX2, HomologyCount: HomologyCount}) as breakdown
MERGE (m:Summary)
SET m.homologyBreakdown = apoc.convert.toJson(breakdown);

MATCH (entity)-[:dataSource]-(ds:DataSource)
WHERE entity:Gene OR entity:Protein
WITH ds.identifier AS dataSource,
       entity.TAXID AS TAXID,
       COUNT(CASE WHEN entity:Gene THEN 1 END) AS geneCount,
       COUNT(CASE WHEN entity:Protein THEN 1 END) AS proteinCount
WITH COLLECT({dataSource: dataSource, TAXID: TAXID, geneCount: geneCount, proteinCount:proteinCount}) as breakdown
MERGE (m:Summary)
SET m.dataSourceBreakdown = apoc.convert.toJson(breakdown);

MATCH (entity)
WHERE entity:Gene OR entity:Protein
WITH 
       CASE 
           WHEN entity:Gene THEN 'Gene'
           WHEN entity:Protein THEN 'Protein'
       END AS label,
       entity.TAXID AS TAXID,
       count(entity) AS total,
       count(CASE WHEN entity.altName IS NOT NULL THEN entity END) AS withAltName,
       count(CASE WHEN entity.prefName IS NOT NULL THEN entity END) AS withPrefName,
       count(CASE WHEN (entity.prefName IS NOT NULL AND entity.altName IS NOT NULL) THEN entity END) AS withBothNames
WITH COLLECT({label: label, TAXID: TAXID, total: total, withAltName: withAltName, withPrefName: withPrefName, withBothNames: withBothNames}) as breakdown
MERGE (m:Summary)
SET m.namesBreakdown = apoc.convert.toJson(breakdown);

MATCH (gene:Gene)-[:hasMotifLink]->(pub:Publication)
WITH pub.AbstractHeader AS header, pub.prefName AS prefName, pub.YEAR AS year, gene.TAXID AS TAXID, COUNT(gene) AS geneCount
WITH COLLECT({header: header, prefName: prefName, year: year, TAXID: TAXID, geneCount: geneCount}) as breakdown
MERGE (m:Summary)
SET m.pubsBreakdown = apoc.convert.toJson(breakdown);

MATCH (gene:Gene)-[:hasMotifLink]-(trait:Trait)
WITH trait.identifier AS ID, trait.prefName AS prefName, gene.TAXID AS TAXID, COUNT(gene) AS geneCount
WITH COLLECT({ID: ID, prefName: prefName, TAXID: TAXID, geneCount: geneCount}) as breakdown
MERGE (m:Summary)
SET m.traitBreakdown = apoc.convert.toJson(breakdown);

MATCH (gene:Gene)-[:hasMotifLink]-(bio:BioProc)
WITH bio.identifier AS ID, bio.prefName AS prefName, gene.TAXID AS TAXID, COUNT(gene) AS geneCount
WITH COLLECT({ID: ID, prefName: prefName, TAXID: TAXID, geneCount: geneCount}) as breakdown
MERGE (m:Summary)
SET m.bioProcBreakdown = apoc.convert.toJson(breakdown);

MATCH (species:Species)--(acc:Accession)
WITH species.prefName AS prefName, acc.identifier AS TAXID
WITH COLLECT({prefName: prefName, TAXID: TAXID}) as species
MERGE (m:Summary)
SET m.species = apoc.convert.toJson(species);

MATCH (trait:Trait)
OPTIONAL MATCH path = (trait)-[:is_a*]->(parent:Trait)
WITH trait, path
ORDER BY LENGTH(path) DESC
WITH trait, COLLECT(path)[0] AS longestPath
WITH trait, REVERSE([parent IN TAIL(NODES(longestPath)) | parent.prefName]) AS parentNames
WITH trait, 
     CASE 
         WHEN SIZE(parentNames) > 0 THEN apoc.map.fromLists(
             [i IN RANGE(1, SIZE(parentNames)) | 'level ' + i],
             parentNames
         )
         ELSE {}
     END AS parentTags
WITH {
    prefName: trait.prefName,
    identifier: trait.identifier,
    parents: parentTags
} AS result
WITH COLLECT(result) AS jsonResult
MERGE (m:Summary)
SET m.traits = apoc.convert.toJson(jsonResult);

MATCH (bp:BioProc)
OPTIONAL MATCH path = (bp)-[:is_a*]->(parent:BioProc)
WITH bp, path
ORDER BY LENGTH(path) DESC
WITH bp, COLLECT(path)[0] AS longestPath
WITH bp, REVERSE([parent IN TAIL(NODES(longestPath)) | parent.prefName]) AS parentNames
WITH bp, 
     CASE 
         WHEN SIZE(parentNames) > 0 THEN apoc.map.fromLists(
             [i IN RANGE(1, SIZE(parentNames)) | 'level ' + i],
             parentNames
         )
         ELSE {}
     END AS parentTags
WITH {
    prefName: bp.prefName,
    identifier: bp.identifier,
    parents: parentTags
} AS result
WITH COLLECT(result) AS jsonResult
MERGE (m:Summary)
SET m.bioProcs = apoc.convert.toJson(jsonResult);
"

$NEO4J_HOME/bin/cypher-shell -a "$neo_url" -u "$KETL_NEO_USR" -p "$KETL_NEO_PWD" --format plain "$query"

echo -e "\nAll Neo4j indexing and stats generation done\n"
echo `date` >"$out_flag"