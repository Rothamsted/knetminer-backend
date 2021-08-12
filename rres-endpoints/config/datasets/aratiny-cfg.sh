# This is a dummy test OXL 
export CFG_OXL_SRC="$CFG_DATASET_TARGET/aratiny.oxl"

if [[ ! -e "$CFG_OXL_SRC" ]]; then 
	echo -e "\n\tDownloading aratiny.oxl"
	# TODO: migrate to the new test dataset.
	wget -O "$CFG_OXL_SRC" https://s3.eu-west-2.amazonaws.com/nfventures-testing.knetminer/default.oxl
	touch "$CFG_OXL_SRC" # to trigger following steps in SnakeMake
fi
