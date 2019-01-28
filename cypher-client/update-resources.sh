# TODO: check for requirements

export JENA_HOME=/Applications/local/dev/semantic_web/apache-jena-3.4.0
export ODX2RDF_HOME=/Users/brandizi/Documents/Work/RRes/ondex_git/ondex-full/ondex-knet-builder/modules/rdf-export-2-cli/target/rdf-export-2-cli_2.1.1-SNAPSHOT
export RDF2NEO_HOME=/Users/brandizi/Documents/Work/RRes/ondex_git/ondex-full/ondex-knet-builder/modules/neo4j-export/target/neo4j-exporter

wdir=$(pwd)
tmp_dir="$wdir/target"
mkdir -p "$tmp_dir"

echo -e "\n\n\tDownloading OXL Test File to temp location\n"
test_dir="$wdir/src/test/resources"
sample_base="ara-tiny"
curl --output "$tmp_dir/${sample_base}.oxl"\
     https://s3.eu-west-2.amazonaws.com/nfventures-testing.knetminer/default.oxl

echo -e "\n\n\tAdding URIs to OXL Test File and putting result in place\n"
"$ODX2RDF_HOME/add-uris.sh" "$tmp_dir/${sample_base}.oxl" "$test_dir/${sample_base}.oxl"


echo -e "\n\n\tGenerating Test RDF\n"
"$ODX2RDF_HOME/odx2rdf.sh" "$test_dir/${sample_base}.oxl" "$tmp_dir/${sample_base}.ttl"


echo -e "\n\n\tGenerating Test Neo4j Database\n"
mvn neo4j-server:stop
# Default port, so that we can use the default config for rdf2neo.
# We use a non-standard port in the POM, in order to avoid problems with some of our deployment servers, 
# but this script is supposed to be run by the developer on its own computer, so, this should be fine. 
mvn neo4j-server:start -Dneo4j.server.boltPort=7687 -Dneo4j.server.deleteDb=true

  "$RDF2NEO_HOME/ondex2neo.sh" "$tmp_dir/${sample_base}.ttl"

mvn neo4j-server:stop

cd target/neo4j-server/neo4j-community-*/data/databases
tar cv --bzip2 -f "$test_dir/${sample_base}-neo4j.tar.bz2" graph.db

echo -e "\n\n\tThe End\n"
