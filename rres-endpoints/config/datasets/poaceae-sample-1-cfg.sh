# The aratiny dataset from the Knetminer test data, used for testing the ETL
# scripts. 

# This is a dummy test OXL
export KETL_SRC_OXL="${KETL_OUT}/tmp/${KETL_DATASET_ID}.oxl"

# Neo 
export KETL_HAS_NEO4J=true
export KETL_NEO_VERSION='5.23.0'
export NEO4J_HOME="/tmp/neo4j-community-$KETL_NEO_VERSION"

# the new KnetMiner Nova Initialiser
# We fit this into the Plants Lite resource, which is rather arbitrary, just to make the initialiser
# work locally and against the Community Neo4j
export KNET_INIT_DATASET_ID="plants-lite"

# This is usually not done for a real dataset, since
# the OXL comes from another workflow (based on Ondex Mini) and
# it's already in place in ${KETL_SRC_OXL} (see eg, poaceae-free-57-cfg.oxl
# 
# In this dummy test the dummy OXL is downloaded from the place
# where we make it available for all software components that need it.
#
if [[ ! -e "${KETL_SRC_OXL}" ]]; then

	echo -e "\n\tDownloading $KETL_DATASET_ID.oxl"
	mkdir -p "$KETL_OUT" "$KETL_OUT/tmp"

	knet_download_url_base=https://knetminer.com/downloads/test
	# We want back to this after the Nova Migration in 2025
	# TODO: we're waiting for SSL renewal
	knet_download_url_base=https://knetminer.rothamsted.ac.uk/downloads/test
	
  wget --no-check-certificate -O "${KETL_SRC_OXL}"\
      "${knet_download_url_base}/${KETL_DATASET_ID}.oxl"
	
	touch "$KETL_SRC_OXL" # to trigger following steps in SnakeMake
fi
