set -e

# Updates the RRes Neo4j server, containing Cereals data

echo -e "\n\n\tUpdating and restarting $KETL_DATASET_ID  Neo4j server at '$KNET_NEO_SSH'"

# As you can see, it issue SSH commands to stop the Neo4j service, load from the dump, restart it.
#
ssh "$KNET_NEO_SSH" 'bash -il' << EOT

set -e
export JAVA_HOME=/etc/alternatives/jre_21
cd /opt/neo4j-cereals/bin

# This has entries in /etc/sudoers.d/neo4j to make it working without password
sudo systemctl stop neo4j-cereals.service

./neo4j-admin database load --from-stdin --overwrite-destination=true neo4j \
  <"$KNET_NEO_DATA/$KETL_DATASET_ID-$KETL_DATASET_VERSION-neo4j.dump"

sudo systemctl start neo4j-cereals.service

EOT
