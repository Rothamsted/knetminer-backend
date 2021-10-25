# Synchronisation of pipeline's output data with test and production servers around the RRes 
# infrastructure. 
#
# This has to be ran manually, it isn't part of build-endpoint.snakefile.
#
set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

echo -e "\n\n\tSynchronising file dumps with their web publishing location\n"
echo -e "Please, beware, logs from this command report hashed dirs\n"

web_target="$KNET_DOWNLOAD_SSH:$KNET_DOWNLOAD_DIR/$KNET_DATASET_ID/$KNET_DATASET_VERSION"

# If the dataset isn't public, it is published on the same target, but under an obfuscated
# directory, which is based on this hash code.
#
secret_path="$KNET_WEB_SECRETS_DIR/$KNET_DATASET_ID-$KNET_DATASET_VERSION.key"
if [[ -e "$secret_path" ]]; then
	secret=`cat "$secret_path"`
	web_target="$web_target/$secret"
fi

chmod -R ugo+rX "$KNET_DATASET_TARGET"
rsync --exclude=tmp --exclude='.*' $RSYNC_DFLT_OPTS $RSYNC_MIRROR_OPTS "$KNET_DATASET_TARGET/" "$web_target"


# The Neo4j stuff.
#
# This transfers the dump and restart the Neo4j server, on configured machines. As you can see, you can disable this
# step completely with a flag, which is to be set upon dataset configuration.
#

if [[ "$KNET_DATASET_HAS_NEO4J" != 'false' ]]; then

	echo -e "\n\n\tSynchronising file dump with Neo4j server\n"
	rsout=`rsync $RSYNC_DFLT_OPTS $RSYNC_BKP_OPTS "$KNET_DATASET_TARGET/neo4j.dump" \
	             "$KNET_NEO_SERVER_SSH:$KNET_NEO_SERVER_DATA_DIR/$KNET_DATASET_ID-$KNET_DATASET_VERSION-neo4j.dump" | tee /dev/tty`
	    
	# Synch Neo4j only if the dump was actually updated         
	if [[ ! "$rsout" =~ 'Number of regular files transferred: 0' ]]; then 
	
	  echo -e "\n\n\tRe-populating the Neo4j Server\n"
	  
	  # This issues commands to stop, re-load from the dump, restart.
	  # eg, see neo-server-update-poaceae.sh
	  #
	  ./config/neo4j/neo-server-update-$KNET_DATASET_ID.sh
	
	fi
fi

# The KnetMiner instances (eg, babvs72/73).
# 
# These require that the new OXL is put in place and the Knetminer's Docker container(s) is (are) restarted, so that
# they can reload the new data.
#
if [[ ! -z "$KNET_TEST_SERVERS" ]]; then
	echo -e "\n\n\tSynchronising OXL data on KnetMiner app servers (requires manual reload)\n"
	for host in $KNET_TEST_SERVERS
	do
	  echo -e "\nSynchronising OXL dump with '$host' Knetminer server\n"
	  rsout=`rsync $RSYNC_DFLT_OPTS $RSYNC_BKP_OPTS \
	        "$KNET_DATASET_TARGET/knowledge-graph-uris.oxl" \
	        "$KNET_SSH_USER@$host:$KNET_TEST_DATA_DIR/knowledge-network.oxl" | tee /dev/tty`
	
	  [[ "$rsout" =~ 'Number of regular files transferred: 0' ]] && continue
	
	  ./utils/knet-server-restart.sh $host
	
	done
fi
