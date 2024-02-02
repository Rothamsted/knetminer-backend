
######## Check mandatory parameters

if [[ $# -le 2 ]]; then
	echo -e "\n\n\tUsage: $0 <dataset-id> <dataset-version> [environment-id]\n\n"
	exit 1
fi

# The dataset ID and version point to the processed dataset and its details
export KETL_DATASET_ID="$1"
export KETL_DATASET_VERSION="$2"
export KETL_ENVIRONMENT="$3"

########## General variables

# Usually, this is set by the main invoker, but we redefine it here just in case.
# 
if [[ -z "$KETL_HOME" ]]; then
	KETL_HOME=`realpath "${BASH_SOURCE[0]}"`
	KETL_HOME=`dirname "$KETL_HOME"`
	cd "$KETL_HOME/.."
	export KETL_HOME="`pwd`"
fi

# The path to the input OXL. This is the start point for the whole pipeline.
# This is usually defined in config/datasets/$datasetid-$version-cfg.sh
export KETL_SRC_OXL=''

# Where all the output goes, including to-be-exported and temp files
export KETL_OUT="$KETL_HOME/output"

# If true, it processes the Neo4j-related steps (rdf2neo, etc)
export KETL_HAS_NEO4J=false

# Options passed to the 'snakemake' command
export KETL_SNAKE_OPTS=''


##### Software tools

# Usually these are set by some -env.sh file, which is then specified via KETL_ENVIRONMENT

# The Ondex RDF exporter
export KETL_RDFEXP_HOME=''

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME=''

# The Neo4j server home. This uses their own naming convention
# export NEO4J_HOME=''

# You might need special, environment-dependent scripts to start/stop Neo
export KETL_NEO_START="$KETL_HOME/utils/neo-start.sh" 
export KETL_NEO_STOP="$KETL_HOME/utils/neo-stop.sh" 

# The credentials for the server where you want upload the OXL export.
# Note that usually this IS NOT a production server  
export KETL_NEO_USR='neo4j'
export KETL_NEO_PWD='testTest' # Recent Neo doesn't accept 'test'

# You might have special ways, dynamic ways to establish the BOLT URL of your 
# Neo server 
function ketl_get_neo_url ()
{
	return 'bolt://localhost:7687'
}


###### Invoke specific configs. They override common defs above.

## Optional environment
#
if [[ ! -z "$KETL_ENVIRONMENT" ]]; then
  . "${KETL_HOME}/config/environments/${KETL_ENVIRONMENT}-env.sh"
fi

## Dataset-specific config
#
. "${KETL_HOME}/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION}-cfg.sh"
