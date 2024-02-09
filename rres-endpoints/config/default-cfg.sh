
######## CLI arguments

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
export KETL_OUT="$KETL_HOME/output/${KETL_DATASET_ID}/${KETL_DATASET_VERSION}"

# Options passed to the 'snakemake' command
export KETL_SNAKE_OPTS=''


##### Software tools

# Usually these are set by some -env.sh file, which is then specified via KETL_ENVIRONMENT

# The Ondex RDF exporter
export KETL_RDFEXP_HOME=''

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME=''

# This is used to define the name of the Neo4j dump. 
# You can possibly use it for NEO4J_HOME too
export KETL_NEO_VERSION='5.15.0'

# The KnetMiner codebase
export KNET_WEBAPP=''
# Eg, export KNET_INITIALIZER_HOME="$KNET_WEBAPP/knetminer-initializer-cli/target/knetminer-initializer-cli-5.7-SNAPSHOT"
export KNET_INITIALIZER_HOME='' 
# Used by $KNET_WEBAPP/docker/dataset-init.sh, it's one of the dirs in 
# $KNET_WEBAPP/datasets. It might be different from the default, since multiple
# datasets might have the same config
export KNET_DATASET_ID="$KETL_DATASET_ID"


### Neo4j settings

# These are normally overwritten by config/datasets/xxx-vv-cfg.sh, since each dataset knows 
# if it has a Neo DB.

# If true, it processes the Neo4j-related steps (rdf2neo, etc)
export KETL_HAS_NEO4J=false

# The Neo4j server home. This uses their own naming convention.
# export NEO4J_HOME=''

# You might need special, environment-dependent scripts to start/stop Neo
export KETL_NEO_START="$KETL_HOME/utils/neo4j/neo-start.sh" 
export KETL_NEO_STOP="$KETL_HOME/utils/neo4j/neo-stop.sh"

# Empty/init the dataset's DB, see the default file for details
export KETL_NEO_INIT="$KETL_HOME/utils/neo4j/neo-init.sh" 

# The credentials for the server where you want upload the OXL export.
# Note that usually this IS NOT a production server.
export KETL_NEO_USR='neo4j'
export KETL_NEO_PWD='testTest' # Recent Neo doesn't accept 'test'

# You might have special ways, dynamic ways to establish the BOLT URL of your 
# Neo server 
function ketl_get_neo_url ()
{
	echo 'bolt://localhost:7687'
}
export -f ketl_get_neo_url



###### rsync options, used for cross-server copies

# Default rsync options. --inplace or --append are dangerous when rsync is interruped
export RSYNC_DFLT_OPTS="--progress --human-readable --stats --rsh=ssh --partial --sparse --dry-run"

# Backup options, the only things not preserved are devices and owner. This is
# because we have a regular user doing this, not necessarily the root.
#
export RSYNC_BKP_OPTS="--recursive --links --times --specials --perms"

# Mirror options, as usually, the destination is synched with the source, but not vice-versa
# (you need to run a dest->source synch too in this case)
#
export RSYNC_MIRROR_OPTS="$RSYNC_BKP_OPTS --delete --delete-during"




###### Invokes specific configs. They override common defs above.

# As you can see, these additional specific configs are invoked based on the parameters
# passed at the begin of the hereby file, which usually come from some top ETL invoker.

## Optional environment
#
if [[ ! -z "$KETL_ENVIRONMENT" ]]; then
  . "${KETL_HOME}/config/environments/${KETL_ENVIRONMENT}-env.sh"
fi

## Dataset-specific config.
#
. "${KETL_HOME}/config/datasets/${KETL_DATASET_ID}-${KETL_DATASET_VERSION}-cfg.sh"
