#!/usr/bin/env bash

# Helper used by the dataset build pipeline to wrap the Neo4j server controller
# and launch on a SLURM node. 
# 
# This is usually launched by <home>/config/neo4j/neo-start[-xxx].sh, see details in that script
#
#SBATCH --mem=13g
#SBATCH --qos=long
#SBATCH --time=1-0:0:0

# The synch command is used, because this is invoked from the sbatch command, which is going to 
# be asynchronous for the top invoker. 
"$NEO4J_HOME/bin/neo4j" console
