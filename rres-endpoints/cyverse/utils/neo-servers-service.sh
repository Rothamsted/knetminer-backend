#!/usr/bin/env bash
#
# 
#

set -e

cmd="$1"

if [[ ! "$cmd" =~ ^(start|stop|status)$ ]]; then

  echo -e "\n\n\t`basename "$0"` <start|stop|status>\n"
  exit 1
  
fi

sw=/opt/software
NEO_HOMES="$sw/neo4j"
[[ `hostname -a` == 'arabidopsis' ]] && NEO_HOMES="$NEO_HOMES $sw/neo4j-covid19"

[[ "cmd" == 'start' ]] && ulimit -n 40000

for neo_home in $NEO_HOMES 
do

  echo -e "\n---- Running '$cmd' at '$neo_home'"

  cd "$neo_home"
  ./bin/neo4j $cmd

done

echo -e "\n\n\tThe End\n"
