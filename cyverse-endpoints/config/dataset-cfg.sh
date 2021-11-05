# The named graph that contains this dataset in Virtuoso
export CY_DATASET_GRAPH="${CY_DATASET_GRAPH_PREFIX}$CY_DATASET_ID"
# Where Neo4j for this dataset is (on the neo server)
[[ -z "$CY_DATASET_NEO_HOME" ]] && export CY_DATASET_NEO_HOME="$CY_SOFTWARE_DIR/neo4j"

# Define it on config/datasets/*-cfg.sh script, when your dataset has an agrischema mapping file. 
# this is used as tail for the named graph URI
# 
# export CY_DATASET_AGSCHEMA_ID="knetAgrischemas"
