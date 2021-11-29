#!/usr/bin/env bash
#
# Maintenance script to start/stop/status Neo servers. 
#

set -e

cmd="$1"

if [[ ! "$cmd" =~ ^(start|stop|status)$ ]]; then

  echo -e "\n\n\t`basename "$0"` <start|stop|status> [server-home]\n"
  exit 1
  
fi

if [[ -z "$CY_SCRIPTS_HOME" ]]; then

  mydir=`realpath "${BASH_SOURCE[0]}"`
  mydir=`dirname "$mydir"`

	cd "$mydir/.."
	. config/environments/cyverse-env.sh  
fi 

neo_homes="$2"
if [[ -z "$neo_homes" ]]; then
	neo_homes="$CY_SOFTWARE_DIR/neo4j"
	[[ `hostname -a` == 'arabidopsis' ]] && neo_homes="$CY_SOFTWARE_DIR/neo4j-covid19"
fi

[[ "$cmd" == 'start' ]] && ulimit -n 40000

for neo_home in $neo_homes 
do
  echo -e "\n\n---- Running '$cmd' at '$neo_home' ----\n"

  cd "$neo_home"
  ./bin/neo4j $cmd
done

echo -e "\n\n\tThe End\n"
