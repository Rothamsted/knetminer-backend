# WARNING! This is about the agrischemas/GXA pipeline and it's only used with
# server-sync.sh

# The data generation happen in its own pipeline, at 
# /home/data/knetminer/software/agri-schemas/dfw-dataset/gxa
#

export KNET_DATA_TARGET=/home/data/knetminer/pub/endpoints/agri-schemas
export KNET_DOWNLOAD_DIR=/var/www/html/knetminer/downloads/agri-schemas
export KNET_TEST_SERVERS=''
export KNET_DATASET_HAS_NEO4J=false
