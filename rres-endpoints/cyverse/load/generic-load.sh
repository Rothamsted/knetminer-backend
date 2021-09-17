set -e
wkey_file="$CY_WEB_SECRETS_DIR/$CY_DATASET_ID-$CY_DATASET_VERSION.key"
[[ -e "$wkey_file" ]] && secret=`cat "$wkey_file"`

[[ -z "$secret" ]] && data_url="$CY_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION" \
|| data_url="$CY_PRIVATE_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION/$secret"


data_url="$data_url/rdf"


echo -e "\n\n\tDownloading Ontologies\n"

onto_dir="$CY_DATA_DIR/ontologies"
mkdir -p "$onto_dir"

lftp="set ssl:verify-certificate no;"
lftp="$lftp mirror -v --continue $data_url/ontologies/ '$onto_dir';"
lftp="$lftp exit"

lftp -e "$lftp"

echo -e "\n\n\tReloading Ontologies\n"
"$VIRTUOSO_UTILS_HOME/virt_load.sh" -r "$onto_dir" "${CY_DATASET_GRAPH_PREFIX}ontologies"
