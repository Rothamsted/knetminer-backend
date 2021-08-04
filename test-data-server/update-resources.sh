# TODO: check for requirements

set -e

odxmods=/Users/brandizi/Documents/Work/RRes/ondex_git/ondex-knet-builder/ondex-knet-builder/modules/
export ODX2RDF_HOME="$odxmods/rdf-export-2-cli/target/rdf-export-2-cli_4.0-SNAPSHOT"
export RDF2NEO_HOME="$odxmods/neo4j-export/target/neo4j-exporter"

cd `dirname "$0"`
wdir=`pwd`
data_target_dir="$wdir/src/main/resources"

echo -e "\n\n\tDownloading OXL Test File to temp location\n"
sample_base="poaceae-sample"
curl --output "$data_target_dir/${sample_base}.oxl"\
     https://knetminer.org/downloads/test/${sample_base}.oxl

echo -e "\n\n\Adding some test data to the downloaded OXL\n"
mvn test-compile exec:java -D'exec.mainClass=uk.ac.rothamsted.knetminer.backend.test.OxlTestDataCreator' \
  	-Dexec.classpathScope=test \
  	-Dexec.args="$data_target_dir/${sample_base}.oxl $data_target_dir/${sample_base}.oxl" \
  	-Dexec.cleanupDaemonThreads=false # about ondex attribute compressors

echo -e "\n\n\tAdding URIs to OXL Test File\n"
"$ODX2RDF_HOME/add-uris.sh" "$data_target_dir/${sample_base}.oxl" "$data_target_dir/${sample_base}.oxl"

echo -e "\n\n\tGenerating Test RDF\n"
"$ODX2RDF_HOME/odx2rdf.sh" "$data_target_dir/${sample_base}.oxl" "$data_target_dir/${sample_base}.ttl"
bzip2 --force "$data_target_dir/${sample_base}.ttl"

echo -e "\n\n\tGenerating Test Neo4j Database\n"
# Default port, so that we can use the default config for rdf2neo.
# We use a non-standard port in the POM, in order to avoid problems with some of our deployment servers, 
# but this script is supposed to be run by the developer on its own computer, so, this should be fine. 
mvn neo4j-server:start -Dneo4j.server.boltPort=7687 -Dneo4j.server.deleteDb=true


  rdf2pg_path=`pwd`/target/rdf2neo-tdb
  rm -Rf "${rdf2pg_path}"
  "$RDF2NEO_HOME/ondex2neo.sh" --tdb "${rdf2pg_path}" "$data_target_dir/${sample_base}.ttl.bz2"
	
	#TODO: Remove echo "Pausing, as required by Neo4j"
	#sleep 3m
	
echo "Shutting down Neo4j, might take a while"
# Apparently we need this to give it time to close the indexes
# As per https://neo4j.com/developer/kb/how-to-properly-shutdown-a-neo4j-database/
export NEO4J_SHUTDOWN_TIMEOUT=600
mvn neo4j-server:stop

rm -f "$data_target_dir/poaceae-sample-neo4j.dump"
cd target/neo4j.server/neo4j-community-*
bin/neo4j-admin dump --to="$data_target_dir/poaceae-sample-neo4j.dump"


echo -e "\n\n\tMoving logs to target/rdf2neo-logs\n"
cd "$wdir"
mv logs target/rdf2neo-logs

echo -e "\n\n\tThe End\n"
