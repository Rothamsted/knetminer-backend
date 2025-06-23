###### General

if ! which snakemake >/dev/null; then
  cat <<EOT

  WARNING: snakemake not found in PATH. If you're missing conda initialisation, run something like
  SLURM initialisation or config/environments/rres-conda-init.sh BEFORE running Snakemake-based scripts, 
  else they will FAIL

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

# Needed in tdb_load, see comments in there
export JENA_HOME="$KNET_SOFTWARE/jena"

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME="$KNET_SOFTWARE/ondex-mini/tools/neo4j-exporter"

# WARNING: this is not auto-updated, cd in this dir and do 'git pull'
export KNET_WEBAPP="$KNET_SOFTWARE/knetminer"

export KNET_INITIALIZER_HOME="$KNET_SOFTWARE/knetminer-nova/knetminer-initializer"

if [[ -z "$JAVA_HOME" ]]; then
  # This is usually a symbolic link, pointing at the last/current version
  # TODO: migrate all to 21
  export JAVA_HOME="$KNET_SOFTWARE/jdk21"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# WARNING: this must be compatible with SLURM limits set in config/snakemake/slurm
export JAVA_TOOL_OPTIONS="-Xmx310G"

export KETL_SNAKE_OPTS="--profile config/snakemake/slurm"
# export KETL_SNAKE_OPTS="$KETL_SNAKE_OPTS --dry-run"



###### Neo4j

# TODO: you need to do it manually, since it relies on sourced files
# conda activate snakemake

function ketl_get_neo_url ()
{
	host_file="$KETL_OUT/tmp/neo4j-slurm.host"
	if [[ ! -f "$host_file" ]]; then
		# it's probably down, we signal it this way
		echo ''
		return
	fi
  neo_host="$(cat "$host_file")"
  echo "bolt://$neo_host:7687"
}

export KETL_NEO_START="$KETL_HOME/utils/neo4j/neo-start-slurm.sh" 
export KETL_NEO_STOP="$KETL_HOME/utils/neo4j/neo-stop-slurm.sh" 

# Before dumping large databases, Neo4j needs to be restarted, paused for this time and
# then stopped again. 
# This is used in neo-dump.sh It's unset by default (ie, no 2nd restart/pause done)
#
export KETL_NEO_IDX_PAUSE_TIME='20m' # Needs at least 17min for cereals-premium


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


