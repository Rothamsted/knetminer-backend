set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

rdf_target="$KNET_DATASET_TARGET/rdf"
tdb="$KNET_DATASET_TARGET/tmp/tdb"
if [[ ! -d "$tdb" ]]; then
  # If it exists, we assume it's already populated and the command below takes it as-is when no RDF is 
  # given.
  
  rdf_files=""$rdf_target/knowledge-graph.ttl.bz2""
fi

# Same for this
if [[ ! -d "$rdf_target/ontologies" ]]; then

  echo -e "\n\tDownloading Ontologies\n"
  
  mkdir -p "$rdf_target/ontologies"
  "$KNET_NEOEXPORT_HOME/get_ontologies.sh" "$rdf_target/ontologies"
  rdf_files="$rdf_files "$rdf_target/ontologies/"*.*"
fi

echo -e "\n\tLoading RDF into '$tdb' \n"
# -l option does the trick
"$KNET_NEOEXPORT_HOME/ondex2neo.sh" -l --tdb "$tdb" $rdf_files
