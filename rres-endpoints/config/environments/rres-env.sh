###### General

if ! which snakemake >/dev/null; then
  cat <<EOT

  WARNING: snakemake not found in PATH. If you're missing conda initialisation, run this:
  
  BEFORE running Snakemake-based scripts, else they will FAIL

EOT

fi 


# The KnetMiner team has all of its stuff here
export KNET_HOME=/home/data/knetminer
# Where we keep software executables
export KNET_SOFTWARE="$KNET_HOME/software"

# Where all the KETL_OUT output is sent. Each dataset should set its own 
# output under this root, via 
export KETL_OUT_HOME="$KNET_HOME/pub/endpoints"



###### Software

# The Ondex RDF exporter
export KETL_RDFEXP_HOME="$KNET_SOFTWARE/rdf-export-2-cli"

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME="$KNET_SOFTWARE/ondex-mini/tools/neo4j-exporter"

# WARNING: this is not auto-updated, cd in this dir and do 'git pull'
export KNET_WEBAPP="$KNET_SOFTWARE/knetminer"
export KNET_INITIALIZER_HOME="$KNET_WEBAPP/knetminer-initializer-cli/target/knetminer-initializer-cli-5.7-SNAPSHOT"

# WARNING: this must be compatible with SLURM limits set in config/snakemake/slurm
export JAVA_TOOL_OPTIONS="-Xmx56G"

export KETL_SNAKE_OPTS="--profile config/snakemake/slurm"
#export KETL_SNAKE_OPTS="$KETL_SNAKE_OPTS --dry-run"



###### Neo4j

# TODO: you need to do it manually, since it relies on sourced files
# conda activate snakemake

function ketl_get_neo_url ()
{
  neo_host=`cat "$KETL_OUT/tmp/neo4j-slurm.host"`
  echo "bolt://$neo_host:7687"
}

export KETL_NEO_START="$KETL_HOME/utils/neo4j/neo-start-slurm.sh" 
export KETL_NEO_STOP="$KETL_HOME/utils/neo4j/neo-stop-slurm.sh" 


###### Servers-sync options

## The web dump coordinates

# The SSH coordinates fo the knetminer.com web server, where file dumps are copied
# for download
export KNET_WEB_SSH='brandizim@babvs59.rothamsted.ac.uk'

# The root of the download directory, corresponding to
# something like knetminer.com/downloads
export KNET_WEB_DUMPS=/var/www/html/knetminer/downloads

# The dir where we keep secrets to be used with applications and scripts
export KNET_SECRETS="$KNET_SOFTWARE/secrets"
export KNET_WEB_SECRETS="$KNET_SECRETS/web-data"

# This script contains the stupid initialisation that 'conda init' puts in .bashrc
# You need to SOURCE it manually, from your own shell
# We rely on the Conda/Mamba/Snakemake installation at:
# /home/data/knetminer/software/conda/mamba
# TODO: make this script available to everyone

# . /home/usern/brandizim/.local/bin/conda-init.sh

# And then you need this too
# . conda activate snakemake

#module load snakemake/6.1.0-foss-2020b


