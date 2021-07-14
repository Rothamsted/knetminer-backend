export CFG_OXL_SRC="/tmp/aratiny.oxl"

# This is a dummy test OXL 
if [[ ! -e "$CFG_OXL_SRC" ]]; then 
	echo -e "\n\tDownloading aratiny.oxl"
	wget -O "$CFG_OXL_SRC" https://s3.eu-west-2.amazonaws.com/nfventures-testing.knetminer/default.oxl
fi
