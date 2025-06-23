# Endpoint building step to create a Jena TDB triple store, using the dataset RDF obtained from
# the OXL and other static files (eg, ontologies).
# 
# This is essentially a wrapper of the Ondex Neo4j exporter, which is invoked with the -l option
# (ie, TDB loading-only mode). The Neo4j population is split into these two steps, in order to make 
# the pipeline more incremental.
# 
set -e

# Inputs
kg_rdf="$1"
meta_rdf="$2"

# Outputs
tdb="$3"
ontos="$4"


rdf_files="$meta_rdf"

if [[ ! -d "$tdb" ]]; then
  # If it exists, we assume it's already populated and the command below takes the TDB as-is when no RDF is 
  # given.
  
  rdf_files="$kg_rdf"
fi

# Same for this
if [[ ! -d "$ontos" ]]; then

  echo -e "\n\tDownloading Ontologies\n"
  
  mkdir -p "$ontos"
  "$KETL_NEOEXPORT_HOME/get_ontologies.sh" "$ontos"

  # smaller ones first, don't postpone stupid errors with these
  rdf_files=""$ontos/"*.* $rdf_files"
fi

echo -e "\n\tLoading RDF into '$tdb' \n"

# We had to reintroduce Jena CLI tools, sinche the rdf2pg loading (based on Java calls) was taking
# huge time for the tdb_load step.
# -l option does the trick of just loading the TDB and not running the whole thing
# "$KETL_NEOEXPORT_HOME/ondex2neo.sh" --rdf-load --tdb "$tdb" $rdf_files

"$JENA_HOME/bin/tdb2.xloader" --loc "$tdb" $rdf_files
