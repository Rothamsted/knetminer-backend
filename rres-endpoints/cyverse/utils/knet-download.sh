set -e

[[ "$#" != 2 ]] && {
	echo -e "\n\nDownloads from The Knetminer data download URL\n"
	echo -e "\t$0 <url path within the dataset dir> <local path>\n"

	echo -e "\nFor URL directories, use a final '/',\n"
	echo -e "for repeating their basename, use '/' for the local path\n\n"

	exit 1 
}

url_path="$1"
out_path="$2"


wkey_file="$CY_WEB_SECRETS_DIR/$CY_DATASET_ID-$CY_DATASET_VERSION.key"
[[ -e "$wkey_file" ]] && secret=`cat "$wkey_file"`

[[ -z "$secret" ]] && data_url="$CY_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION" \
|| data_url="$CY_PRIVATE_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION/$secret"

lftp="set ssl:verify-certificate no;"
lftp="$lftp mirror -v --continue '$data_url/$url_path' '$out_path';"
lftp="$lftp exit"
lftp -e "$lftp"

