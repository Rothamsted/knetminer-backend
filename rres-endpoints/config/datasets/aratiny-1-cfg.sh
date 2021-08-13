# This is the same as dataset-cfg.sh, but we need it earlier than that
export KNET_DATASET_TARGET="$KNET_DATA_TARGET/$KNET_DATASET_ID/$KNET_DATASET_VERSION"

# This is a dummy test OXL 
export KNET_SRC_OXL="$KNET_DATASET_TARGET/aratiny.oxl"

if [[ ! -e "$KNET_SRC_OXL" ]]; then 
	echo -e "\n\tDownloading aratiny.oxl"
	# TODO: migrate to the new test dataset.
	wget -O "$KNET_SRC_OXL" https://s3.eu-west-2.amazonaws.com/nfventures-testing.knetminer/default.oxl
	touch "$KNET_SRC_OXL" # to trigger following steps in SnakeMake
fi
