code_home=/Users/brandizi/Documents/Work/RRes/code
odx_home="$code_home/ondex/ondex-knet-builder/ondex-knet-builder/modules"

# The Ondex RDF exporter
export KETL_RDFEXP_HOME="$odx_home/rdf-export-2-cli/target/rdf-export-2-cli_7.0.2-SNAPSHOT"

# The Ondex flavour of the rdf2neo converter
export KETL_NEOEXPORT_HOME="$odx_home/neo4j-export/target/neo4j-exporter"

export KNET_INITIALIZER_HOME="$code_home/knetminer-nova/knetminer-api/knetminer-initializer/target/knetminer-initializer-1.0-SNAPSHOT"

# export KETL_SNAKE_OPTS='--dry-run'

# TODO: you need to do it manually, since it relies on sourced files
# conda activate snakemake
