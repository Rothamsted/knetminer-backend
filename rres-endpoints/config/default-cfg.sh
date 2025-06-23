
######## CLI arguments

if [[ $# -le 2 ]]; then
	echo -e "\n\n\tUsage: $0 <dataset-id> <dataset-version> [environment-id]\n\n"
	exit 1
fi

# The dataset ID and version point to the processed dataset and its details
#
export KETL_DATASET_ID="$1"
export KETL_DATASET_VERSION="$2"
export KETL_ENVIRONMENT="$3"


# The version number, without postfixes like -RC
# Should you have a non-numbered version (eg, foo-TEST-cfg.sh, not recommended), this will be empty
#
export KETL_DATASET_VERSION_NUM="$(echo "$KETL_DATASET_VERSION" | sed -E --quiet 's/[^0-9]*([0-9]+).*/\1/p')"


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

# We had to reintroduce Jena CLI tools, sinche the rdf2pg loading (based on Java calls) was taking
# huge time for the tdb_load step.
export JENA_HOME='' 

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME=''

# This is used to define the name of the Neo4j dump. 
# You can possibly use it for NEO4J_HOME too
#
# ===> Override this in config/datasets/name-ver.sh, so that we can keep track
# of the version that was used for a given dataset
#
export KETL_NEO_VERSION='5.26.0'

# The KnetMiner codebase
export KNET_WEBAPP=''
# This is the home of the new Nova initialiser.
export KNET_INITIALIZER_HOME=''
 
# This is the resource Id, used by the Nova initialiser. It's usually the same as the 
# KnetSpace resource ID and the same as KETL_DATASET_ID.
#
# See endpoint-steps/neo-index.sh for details
# 
export KNET_INIT_DATASET_ID="$KETL_DATASET_ID"


### Neo4j settings

# These are normally overwritten by config/datasets/xxx-vv-cfg.sh, since each dataset knows 
# if it has a Neo DB.

# If true, it processes the Neo4j-related steps (rdf2neo, etc)
export KETL_HAS_NEO4J=false

# The Neo4j server home. This uses their own naming convention.
# This is the server that is used to populate an empty DB with the current dataset and then
# to produce the Neo4j dump about the dataset.
#
# This IS NOT any production or test database (not until we change these scripts).
# If you're using SLURM, the start/stopping scripts will use this either, to launch
# the server on a SLURM node.
#
# export NEO4J_HOME=''

# You might need special, environment-dependent scripts to start/stop Neo
export KETL_NEO_START="$KETL_HOME/utils/neo4j/neo-start.sh" 
export KETL_NEO_STOP="$KETL_HOME/utils/neo4j/neo-stop.sh"

# Empty/init the dataset's DB, see the default file for details
export KETL_NEO_INIT="$KETL_HOME/utils/neo4j/neo-init.sh" 

# Before dumping large databases, Neo4j needs to be restarted, paused for this time and
# then stopped again. 
# This is used in neo-dump.sh It's unset by default (ie, no 2nd restart/pause done)
#
export KETL_NEO_IDX_PAUSE_TIME='' # Needs at least 17min for cereals-premium

# The credentials for the server where you want upload the OXL export.
# Note that usually this IS NOT a production server.
export KETL_NEO_USR='neo4j'
export KETL_NEO_PWD='testTest' # Recent Neo doesn't accept 'test'

# You might have special/dynamic ways to establish the BOLT URL of your 
# Neo server 
function ketl_get_neo_url ()
{
	echo 'bolt://localhost:7687'
}
export -f ketl_get_neo_url



###### rsync options, used for cross-server copies

# Default rsync options. --inplace or --append are dangerous when rsync is interruped
export RSYNC_DFLT_OPTS="--progress --human-readable --stats --rsh=ssh --partial --sparse"

# Backup options, the only things not preserved are devices and owner. This is
# because we have a regular user doing this, not necessarily the root.
#
export RSYNC_BKP_OPTS="--recursive --links --times --specials --perms"


# Mirror options, as usually, the destination is synched with the source, but not vice-versa
# (you need to run a dest->source synch too in this case)
#
export RSYNC_MIRROR_OPTS="$RSYNC_BKP_OPTS --delete --delete-during"

# export RSYNC_BKP_OPTS="$RSYNC_BKP_OPTS --dry-run"



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

# DO NOT DO THIS! The flow is supposed to be:
# ${KETL_DATASET_ID}-${KETL_DATASET_VERSION}-cfg.sh
#   which is self contained or POSSIBLY calls something like
#   ${KETL_DATASET_ID}-common.sh
#  
# The use of xxx-common.sh IS NOT MANDATORY, since, for simpler cases, we can just redefine a couple
# of things in the versioned file.
# 
# . "${KETL_HOME}/config/datasets/${KETL_DATASET_ID}-common.sh"
