# The aratiny dataset from the Knetminer test data, used for testing the ETL
# scripts. 

# This is the same as dataset-cfg.sh, but we need it earlier than that
# TODO: review
export KETL_OUT="${KETL_OUT}/${KETL_DATASET_ID}/${KETL_DATASET_VERSION}"

# This is a dummy test OXL
export KETL_SRC_OXL="${KETL_OUT}/${KETL_DATASET_ID}.oxl"

if [[ ! -e "${KETL_SRC_OXL}" ]]; then

	echo -e "\n\tDownloading $KETL_DATASET_ID.oxl"
	mkdir -p "$KETL_OUT"
	
  wget -O "${KETL_SRC_OXL}"\
      https://knetminer.com/downloads/test/${KETL_DATASET_ID}.oxl
	
	touch "$KETL_SRC_OXL" # to trigger following steps in SnakeMake
fi
