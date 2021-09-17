set -e
wkey_file="$CY_WEB_SECRETS_DIR/$CY_DATASET_ID-$CY_DATASET_VERSION.key"
[[ -e "$wkey_file" ]] && secret=`cat "$wkey_file"`

[[ -z "$secret" ]] && data_url="$CY_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION" \
|| data_url="$CY_PRIVATE_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION/$secret"


data_url="$data_url/rdf"


echo -e "\n\n\tDownloading Ontologies"

onto_dir="$CY_DATA_DIR/ontologies"
mkdir -p "$onto_dir"

wget_cmd="wget --recursive --no-parent --continue --no-host-directories --cut-dirs=7 --reject 'index.html*'"
wget_cmd="$wget_cmd -e robots=off --directory-prefix"

$wget_cmd "$onto_dir" "$data_url/ontologies/"

echo -e "\n\n\tReloading Ontologies"
echo "$VIRTUOSO_UTILS_HOME/virt_load.sh" -r "$onto_dir" "${CY_DATASET_GRAPH_PREFIX}ontologies"
