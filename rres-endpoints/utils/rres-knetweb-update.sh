#!/usr/bin/env bash

set -e

cat <<EOT 


Trying to update the KnetMiner web code base (including the initialiser), if something fails, probably you've to merge uncommitted changes in the 
code base clone at "$KNET_WEBAPP"

EOT
 

cd $(dirname "$0")/..

echo "Loading Environment config"
. config/environments/rres-env.sh
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
