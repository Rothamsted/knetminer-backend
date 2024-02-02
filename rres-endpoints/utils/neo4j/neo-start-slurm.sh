# Helper used by the dataset build pipeline to wrap the Neo4j server controller
# and launch on a SLURM node. 
#
set -e

# These .jobid and .host files track the job details where the server is launched 
# 

if [[ -e "$KETL_OUT/tmp/neo4j-slurm.jobid" ]]; then
  job=`cat "$KETL_OUT/tmp/neo4j-slurm.jobid"`
  echo -e "\n\n\tNeo4j appears to be running with JOB #$job"
  echo -e "\tDelete '$KETL_OUT/tmp/neo4j-slurm.*' if that's not the case\n\n"
  exit 1
fi   

echo -e "\n\tStarting Neo4j on SLURM\n"
out="$KETL_OUT/tmp/neo4j-slurm.out"
job=`sbatch --parsable -o "$out" -e "$out" "$KETL_HOME/utils/neo4/neo-start.sbatch"`

# Wait until submitted, so that we can get the host
while true; do
  host=`squeue -j $job -o '%B' --noheader`
  [[ "$host" != 'n/a' ]] && break;
  sleep 5
done

echo -e "\nJob submitted to host $host, ID:$job\n"
echo $job >"$KETL_OUT/tmp/neo4j-slurm.jobid"
echo $host >"$KETL_OUT/tmp/neo4j-slurm.host"
