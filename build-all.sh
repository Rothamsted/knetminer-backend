cd "$(dirname $0)"
goal="${1-verify}" 
./resources-init.sh
mvn $goal
./resources-shutdown.sh
