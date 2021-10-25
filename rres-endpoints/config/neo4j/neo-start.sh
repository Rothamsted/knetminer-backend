# Helper used by the dataset build pipeline to wrap the Neo4j server controller
# and launch on a SLURM node. 
#
set -e

# These .jobid and .host files track the job details where the server is launched 
# 

if [[ -e "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid" ]]; then
  job=`cat "$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid"`
  echo -e "\n\n\tNeo4j appears to be running with JOB #$job"
  echo -e "\tDelete '$KNET_DATASET_TARGET/tmp/neo4j-slurm.*' if that's not the case\n\n"
  exit 1
fi   

echo -e "\n\tStarting Neo4j on SLURM\n"
out="$KNET_DATASET_TARGET/tmp/neo4j-slurm.out"
job=`sbatch --parsable -o "$out" -e "$out" "$KNET_SCRIPTS_HOME/utils/neo-slurm-start.sbatch"`

# Wait until submitted, so that we can get the host
while true; do
  host=`squeue -j $job -o '%B' --noheader`
  [[ "$host" != 'n/a' ]] && break;
  sleep 5
done

echo -e "\nJob submitted to host $host, ID:$job\n"
echo $job >"$KNET_DATASET_TARGET/tmp/neo4j-slurm.jobid"
echo $host >"$KNET_DATASET_TARGET/tmp/neo4j-slurm.host"
