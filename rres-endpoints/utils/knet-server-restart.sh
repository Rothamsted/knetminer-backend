# Restart one of the KnetMiner server instances, ie, redeploys the Docker container and relaunch it 
set -e
host="$1"

if [[ -z "$host" ]]; then
  echo -e "\n\n$0 <knetminer-host>\n"
  exit 1
fi

ssh "$KNET_SSH_USER@$host" 'bash -il' << EOT

set -e
sudo --non-interactive /opt/software/knetminer-ci/rres/dockerhub-deploy.sh
EOT
