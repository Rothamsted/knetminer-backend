set -e

[[ "$#" < 2 ]] && {
	echo -e "\n\nDownloads from The Knetminer data download URL\n"
	echo -e "\t$0 [-m|--multi] <url path within the dataset dir> <local path>\n"

	echo -e "\nFor URL directories, use a final '/',\n"
	echo -e "for repeating their basename, use '/' for the local path\n"
	echo -e "Use -m to download multiple files from a directory"
	echo -e "\n"
	exit 1 
}

is_multi='false'
if [[ "$1" == '-m' ]] || [[ "$1" == '--multi' ]]; then
	is_multi='true'
	shift
fi

url_path="$1"
out_path="$2"


# Different URL paths for public and protected files
#
wkey_file="$CY_WEB_SECRETS_DIR/$CY_DATASET_ID-$CY_DATASET_VERSION.key"
[[ -e "$wkey_file" ]] && secret=`cat "$wkey_file"`

[[ -z "$secret" ]] && data_url="$CY_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION" \
	|| data_url="$CY_PRIVATE_DATA_URL/$CY_DATASET_ID/$CY_DATASET_VERSION/$secret"


# Build the download command. mirror doesn't work for single files, that's
# why there is the -m option (https://stackoverflow.com/questions/46180800)
#
lftp="set ssl:verify-certificate no;"

$is_multi && lftp="$lftp mirror -v --continue '$data_url/$url_path' '$out_path'" \
  || lftp="$lftp get --continue '$data_url/$url_path' -o '$out_path'"

lftp="$lftp;exit"
lftp -e "$lftp"
