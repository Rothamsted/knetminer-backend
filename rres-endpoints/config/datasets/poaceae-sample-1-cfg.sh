# The aratiny dataset from the Knetminer test data, used for testing the ETL
# scripts. 

# This is the same as dataset-cfg.sh, but we need it earlier than that
# TODO: review
export KETL_OUT="${KETL_OUT}/${KETL_DATASET_ID}/${KETL_DATASET_VERSION}"

# This is a dummy test OXL
export KETL_SRC_OXL="${KETL_OUT}/tmp/${KETL_DATASET_ID}.oxl"

# Neo 
export KETL_HAS_NEO4J=true
export NEO4J_HOME="/tmp/neo4j-community-$KETL_NEO_VERSION"

# Knet Initialiser
export KNET_DATASET_ID="poaceae-test"

if [[ ! -e "${KETL_SRC_OXL}" ]]; then

	echo -e "\n\tDownloading $KETL_DATASET_ID.oxl"
	mkdir -p "$KETL_OUT" "$KETL_OUT/tmp"
	
  wget -O "${KETL_SRC_OXL}"\
      https://knetminer.com/downloads/test/${KETL_DATASET_ID}.oxl
	
	touch "$KETL_SRC_OXL" # to trigger following steps in SnakeMake
fi
