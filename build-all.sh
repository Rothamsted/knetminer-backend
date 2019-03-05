cd "$(dirname $0)"
goal="${1-verify}"
shift 

echo -e "\n\n\tInitialising the data server\n"
./resources-init.sh

echo -e "\n\n\tBuilding\n"
mvn $goal $*

echo -e "\n\n\tTerminating the data server\n"
./resources-shutdown.sh
