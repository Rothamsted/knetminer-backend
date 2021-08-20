set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

echo -e "\n\n\tSynchronising file dumps with their web publishing location\n"
echo -e "Please, beware, logs from this command report hashed dirs\n"

secret=`cat "$KNET_WEB_SECRETS_DIR/$KNET_DATASET_ID-$KNET_DATASET_VERSION.key"`
web_target="$KNET_DOWNLOAD_SSH:$KNET_DOWNLOAD_DIR/$KNET_DATASET_ID/$KNET_DATASET_VERSION/$secret"
rsync --exclude=tmp $RSYNC_DFLT_OPTS $RSYNC_MIRROR_OPTS "$KNET_DATASET_TARGET/" "$web_target"

# TODO: babvs72/73

echo -e "\n\n\tSynchronising file dump with Neo4j server\n"
rsync $RSYNC_DFLT_OPTS $RSYNC_BKP_OPTS "$KNET_DATASET_TARGET/neo4j.dump" "$KNET_NEO_SERVER_SSH:$KNET_NEO_SERVER_DATA_DIR/$KNET_DATASET_ID-$KNET_DATASET_VERSION-neo4j.dump" 

echo -e "\n\n\tRe-populating the Neo4j Server\n"
./config/neo4j/neo-server-update-$KNET_DATASET_ID.sh

