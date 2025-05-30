#!/usr/bin/env bash
set -e

cat <<EOT

  DO NOT USE ME!
  
This script used to git-update the old KnetMiner Web code, which was partially used by the old
data initialiser.

Now this is superseded by the Nova initialiser and we redeploy it automatically at: 

  '$KNET_INITIALIZER_HOME' 

TODO: this is actually still TO BE DONE!

EOT

exit 1

#cd $(dirname "$0")/..
cd ..
TOP_DIR=/home/data/knetminer
export PATH=${TOP_DIR}/software/apache-maven-3.8.2/bin:${PATH}
. config/environments/rres-conda-init.sh

# We need KNET_WEBAPP to present the next message
echo "Loading Environment config"
. config/environments/rres-env.sh


cat <<EOT 


Trying to update the KnetMiner web code base (including the initialiser), if something fails,
probably you've to merge uncommitted changes in the code base clone at:
  
"\"$KNET_WEBAPP\""

EOT
 

unset JAVA_TOOL_OPTIONS # Not needed here

echo "---  git status\n"
pushd "$KNET_WEBAPP"

if ! git status --porcelain; then
  printf "ERROR: It seems there are uncommitted changes in "$KNET_WEBAPP", align the local clone to github and then retry this script\n\n"
  exit 1
fi

printf "\n---  git pull\n\n"
if ! git pull; then
  printf "ERROR: git pull failed at "$KNET_WEBAPP", align the local clone to github manually and then retry this script\n\n"
  exit 1
fi

printf "\n--- Maven build (without tests)\n\n"
if ! mvn clean package -DskipTests; then
  printf "ERROR: mvn clean package failed, check if you can fix it manually\n\n"
  exit 1
fi

printf "\n--- KnetMiner initialiser deployment\n\n"
cd knetminer-initializer-cli/target
if ! unzip knetminer-initializer-cli-*.zip; then 
  printf "ERROR: can't unzip "$KNET_WEBAPP/knetminer-initializer-cli/target/knetminer-initializer-cli-*.zip", try to fix things manually\n\n"
  exit 1
fi

printf "\n\n--- All done! The initialiser should be available at "$KNET_INITIALIZER_HOME"\n\n"
