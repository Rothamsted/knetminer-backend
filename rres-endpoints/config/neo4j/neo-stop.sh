set -e
echo -e "\n\tStopping Neo4j\n"

if [[ ! -f "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid" ]]; then
  echo -e "\nNo JOB ID file\n"
  exit
fi

job=`cat "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid"`
scancel $job

# Wait until done
while true; do
  status=`squeue -j $job --noheader`
  [[ -z "$status" ]] && break;
  sleep 5
done
# It seems it still needs some synching
sleep 20

rm -f "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid"
rm -f "$KNET_DATASET_TARGET/tmp/neo4j-slurm.host"
