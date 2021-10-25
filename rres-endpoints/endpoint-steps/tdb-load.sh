# Endpoint building step to create a Jena TDB triple store, using the dataset RDF obtained from
# the OXL and other static files (eg, ontologies).
# 
# This is essentially a wrapper of the Ondex Neo4j exporter, which is invoked with the -l option
# (ie, TDB loading-only mode). The Neo4j population is split into these two steps, in order to make 
# the pipeline more incremental.
# 
set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

rdf_target="$KNET_DATASET_TARGET/rdf"
tdb="$KNET_DATASET_TARGET/tmp/tdb"
if [[ ! -d "$tdb" ]]; then
  # If it exists, we assume it's already populated and the command below takes the TDB as-is when no RDF is 
  # given.
  
  rdf_files=""$rdf_target/knowledge-graph.ttl.bz2""
fi

# Same for this
if [[ ! -d "$rdf_target/ontologies" ]]; then

  echo -e "\n\tDownloading Ontologies\n"
  
  mkdir -p "$rdf_target/ontologies"
  "$KNET_NEOEXPORT_HOME/get_ontologies.sh" "$rdf_target/ontologies"

  # smaller ones first, don't postpone stupid errors with these
  rdf_files=""$rdf_target/ontologies/"*.* $rdf_files"
fi

echo -e "\n\tLoading RDF into '$tdb' \n"
# -l option does the trick
"$KNET_NEOEXPORT_HOME/ondex2neo.sh" -l --tdb "$tdb" $rdf_files
