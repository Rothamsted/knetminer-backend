# Restart one of the KnetMiner server instances, ie, redeploys the Docker container and relaunches it.
#
set -e
host="$1"

if [[ -z "$host" ]]; then
  echo -e "\n\n$0 <knetminer-host>\n"
  exit 1
fi

echo -e "\nRestarting $host\n"
# We need this convoluted thing, SSH with a personal user, then sudo as docker.
# 'docker' is the user controlling everything about Docker.
# 
# If you try 'ssh docker' only, you'll get a 'password expired' message. We might have IT people 
# fixing it, but this is quicker and less dependant on them.
#
sudo_cmd="sudo -u docker --set-home --login --non-interactive"
ssh "$host" 'bash -il' << EOT

set -e
$sudo_cmd /opt/software/knetminer-ci/docker/dataset-cleanup.sh  '$KNET_TEST_DATASET_DIR'
$sudo_cmd /opt/software/knetminer-ci/rres/dockerhub-deploy.sh
EOT
