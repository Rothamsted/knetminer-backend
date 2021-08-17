#!/usr/bin/env bash
#SBATCH --mem=13g
#SBATCH --qos=long
#SBATCH --time=1-0:0:0
"$NEO4J_HOME/bin/neo4j" console
