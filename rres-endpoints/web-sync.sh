set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

echo -e "\n\n\tSynchronising file dumps with their web publishing location\n"
echo -e "Please, beware, logs from this command report hashed dirs\n"

secret=`cat "$KNET_WEB_SECRETS_DIR/$KNET_DATASET_ID-$KNET_DATASET_VERSION.key"`
web_target="$KNET_DOWNLOAD_HOST:$KNET_DOWNLOAD_DIR/$KNET_DATASET_ID/$KNET_DATASET_VERSION/$secret"
rsync $RSYNC_DFLT_OPTS $RSYNC_MIRROR_OPTS "$KNET_DATASET_TARGET/" "$web_target"
