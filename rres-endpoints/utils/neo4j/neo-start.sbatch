#!/usr/bin/env bash

# Helper used by the dataset build pipeline to wrap the Neo4j server controller
# and launch on a SLURM node. 
# 
# This is usually launched by <home>/config/neo4j/neo-start[-xxx].sh, see details in that script
#
#SBATCH --mem=96g
#SBATCH --qos=long
#SBATCH --time=1-0:0:0
#SBATCH --cpus-per-task=8

# The console command is used, because this is invoked from the sbatch command, which is going to 
# be asynchronous for the top invoker.
# Heap left below --mem to leave headroom for page cache + OS. The transaction memory
# pool is heap-bound (limits are unset/commented in neo4j.conf), so heap is the lever.
export JAVA_TOOL_OPTIONS="-Xmx64G"
"$NEO4J_HOME/bin/neo4j" console
