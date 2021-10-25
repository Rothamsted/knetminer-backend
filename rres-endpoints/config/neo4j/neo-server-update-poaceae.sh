set -e

echo -e "\n\n\tUpdating and restarting $KNET_DATASET_ID  Neo4j server at '$KNET_NEO_SERVER_SSH'"

# As you can see, it issue SSH commands to stop the Neo4j service, load from the dupm, restart it.
#
ssh "$KNET_NEO_SERVER_SSH" 'bash -il' << EOT

set -e
export JAVA_HOME=/etc/alternatives/jre_11_openjdk
cd /opt/neo4j-wheat/bin

sudo systemctl stop neo4j-wheat.service
./neo4j-admin load --force --from="$KNET_NEO_SERVER_DATA_DIR/$KNET_DATASET_ID-$KNET_DATASET_VERSION-neo4j.dump"
sudo systemctl start neo4j-wheat.service

EOT
