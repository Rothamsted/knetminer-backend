set -e
cd "`dirname $0`"

goal=verify
if [[ ! -z "$1" ]]; then
  goal="$1"
  shift 
fi

echo -e "\n\n\tInitialising resources (data server etc)\n"
./resources-init.sh

echo -e "\n\n\tBuilding\n"
mvn $goal $*

echo -e "\n\n\tTerminating resources\n"
./resources-shutdown.sh
