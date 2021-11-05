# This is dataset-independent config and it's brought up by the config/environments/*-env.sh scripts.
# (ie, you should call this at the end of your env script).
#

# Common location for all the dataset dumps
[[ -z "$CY_DATA_DIR" ]] && export CY_DATA_DIR=/opt/data

# Common loaction for all software binaries (which are not part of the Linux distro)
[[ -z "$CY_SOFTWARE_DIR" ]] && export CY_SOFTWARE_DIR=/opt/software

# The tmp we prefer to use
[[ -z "$CY_TMP" ]] && export CY_TMP="$CY_DATA_DIR/tmp"

# Where we keep secrets (passwords, hashes for directories, etc)
export CY_SECRETS_DIR=/opt/etc/secrets
export CY_WEB_SECRETS_DIR="$CY_SECRETS_DIR/web-data"

# The common prefix for Virtuoso named graphs (usual architecture is one NGs per named graph)
export CY_DATASET_GRAPH_PREFIX=http://knetminer.org/data/rdf/resources/graphs/

# Cyverse datasets are produced by the RRes pipeline, published on this download location and then 
# downloaded here
export CY_DATA_URL=https://knetminer.org/downloads
# Specific subdirectory where there are private datasets, which are named using hash codes, available
# from CY_WEB_SECRETS_DIR
export CY_PRIVATE_DATA_URL="$CY_DATA_URL/reserved"
