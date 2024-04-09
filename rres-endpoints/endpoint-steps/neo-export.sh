# Endpoint building step to populate a Neo4j database with the dataset contents.
#
# This uses the Ondex Neo4j exporter, starting from the dataset's RDF stored in a Jena TDB
# triple store, as it was produced from the previous tdb-load.sh step.
#

# As you can see below. the scripts uses utils to launch a Neo4j server on top of the RRes SLURM cluster.
# eventually, it stops such DB and uses the Neo4j dump command to place a Neo4j dump on the dataset's 
# output directory. 
#
set -e

tdb="$1"
out_flag="$2" # Creates this file to signal that the step was successfully completed

"$KETL_NEO_INIT"

rdf_target="$KETL_OUT/rdf"
neo_url=`ketl_get_neo_url`

export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.boltUrl='$neo_url'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.user='$KETL_NEO_USR'"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Dneo4j.password='$KETL_NEO_PWD'"


echo -e "\n\tStarting neo4j-export\n"
"$KETL_NEOEXPORT_HOME/ondex2neo.sh" --tdb "$tdb"

echo `date` >"$out_flag"
