cd "$(dirname $0)"
goal="${1-verify}" 
./resources-init.sh
mvn --projects '!test-data-server' $goal
./resources-shutdown.sh
