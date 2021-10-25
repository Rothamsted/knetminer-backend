# Stops the server launched via neo-start.sh
#
set -e
echo -e "\n\tStopping Neo4j\n"

if [[ ! -f "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid" ]]; then
  echo -e "\nNo JOB ID file\n"
  exit
fi

job=`cat "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid"`
host=`cat "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host"`

# --signal doesn't work and without it, KILL seems to be sent
# scancel --signal=TERM --full $job

srun -w "$host" pkill -u $USER -f 'org.neo4j.server.CommunityEntryPoint' || true

# Wait until done
while true; do
  status=`squeue -j $job --noheader` || err=$?
  [[ $err != 0 ||-z "$status" ]] && break
  sleep 5
done

rm -f "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid"
rm -f "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host"
