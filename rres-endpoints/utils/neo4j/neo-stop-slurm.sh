# Stops the server launched via neo-start.sh
#
set -e
echo -e "\n\tStopping Neo4j\n"

if [[ ! -f "$KETL_OUT/tmp/neo4j-slurm.jobid" ]]; then
  printf "\nWARN: No JOB ID file "$KETL_OUT/tmp/neo4j-slurm.jobid", hope this is fine.\n"
  printf "(bcancel any dangling Neo4j server if necessary)\n"
  exit
fi

job=`cat "$KETL_OUT/tmp/neo4j-slurm.jobid"`
host=`cat "$KETL_OUT/tmp/neo4j-slurm.host"`

# In the past, --signal didn't work and without it, KILL seemed to be sent
# Now it seems to be working
scancel --signal=TERM --full $job

# TODO: remove? It's the alt way to shutdown
# srun -w "$host" pkill -u $USER -f 'org.neo4j.server.CommunityEntryPoint' || true

## Wait until done
#while true; do
#  status=`squeue -j $job --noheader` || err=$?
#  [[ $err != 0 ||-z "$status" ]] && break
#  sleep 5
#done

rm -f "$KETL_OUT/tmp/neo4j-slurm.jobid"
rm -f "$KETL_OUT/tmp/neo4j-slurm.host"
