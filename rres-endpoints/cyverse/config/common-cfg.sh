[[ -z "$CY_DATA_DIR" ]] && export CY_DATA_DIR=/opt/data/rdf
[[ -z "$CY_SOFTWARE_DIR" ]] && export CY_SOFTWARE_DIR=/opt/software

[[ -z "$CY_TMP" ]]&& export CY_TMP="$CY_DATA_DIR/tmp"

export CY_CONTAINER_DATA_DIR="$CY_DATA_DIR"


export CY_SECRETS_DIR=/opt/etc/secrets
export CY_WEB_SECRETS_DIR="$CY_SECRETS_DIR/web-data"
export CY_DATASET_GRAPH_PREFIX=http://knetminer.org/data/rdf/resources/graphs/

export CY_DATA_URL=https://knetminer.org/downloads
export CY_PRIVATE_DATA_URL="$CY_DATA_URL/reserved"
