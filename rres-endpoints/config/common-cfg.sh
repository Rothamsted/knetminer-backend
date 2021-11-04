export KNET_DATA_HOME="${KNET_DATA_HOME-/home/data/knetminer}"

export KNET_DATA_TARGET="${KNET_DATA_TARGET-$KNET_DATA_HOME/pub/endpoints}"

export KNET_SOFTWARE_HOME="${KNET_SOFTWARE_HOME-$KNET_DATA_HOME/software}"
export KNET_RDFEXP_HOME="${KNET_RDFEXP_HOME-$KNET_SOFTWARE_HOME/rdf-export-2-cli}"
export KNET_NEOEXPORT_HOME="${KNET_NEOEXPORT_HOME-$KNET_SOFTWARE_HOME/ondex-mini/tools/neo4j-exporter}"
export NEO4J_HOME="${NEO4J_HOME-$KNET_SOFTWARE_HOME/neo4j-test}"

export KNET_NEO_URL="${KNET_NEO_URL-bolt://localhost:7687}"
export KNET_NEO_USR="${KNET_NEO_USR-neo4j}"
export KNET_NEO_PWD="${KNET_NEO_PWD-test}"

# TODO: remove, we use SLURM for Snakemake
#export KNET_CONDA_ENV="${KNET_CONDA_ENV-snakemake}"

# To synch resources after the Snakemake file generation
# 
export KNET_SECRETS_DIR=/home/data/knetminer/software/secrets
export KNET_WEB_SECRETS_DIR="$KNET_SECRETS_DIR/web-data"

# This is used with rsync/ssh, without user or auth credentials, so you need to have 
# a user with proper rights (ie, authorized_keys in the target host) 
#
export KNET_SSH_USER=brandizim # unless overridden

export KNET_DOWNLOAD_HOST=babvs59
export KNET_DOWNLOAD_SSH=$KNET_SSH_USER@$KNET_DOWNLOAD_HOST
export KNET_DOWNLOAD_DIR="${KNET_DOWNLOAD_DIR-/var/www/html/knetminer/downloads}"

export KNET_NEO_SERVER=babvs65
export KNET_NEO_SERVER_SSH=neo4j@$KNET_NEO_SERVER
export KNET_NEO_SERVER_DATA_DIR=/opt/data

# Test servers like babvs73
export KNET_TEST_DATA_DIR=/opt/data/knetminer-datasets/wheat-ci/data
export KNET_TEST_SERVERS="${KNET_TEST_SERVERS-babvs73 babvs72}"


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
