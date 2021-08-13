export KNET_SCRIPTS_HOME=`realpath "${BASH_SOURCE[0]}"`
export KNET_SCRIPTS_HOME=`dirname "$KNET_SCRIPTS_HOME"`
cd "$KNET_SCRIPTS_HOME/../.."
export KNET_SCRIPTS_HOME="`pwd`"

export KNET_ENV_POSTFIX="-brandizi"

export KNET_DATA_HOME=/tmp/knetminer-data-test

ondex_mods="/Users/brandizi/Documents/Work/RRes/ondex_git/ondex-knet-builder/ondex-knet-builder/modules"
export KNET_RDFEXP_HOME="$ondex_mods/rdf-export-2-cli/target/rdf-export-2-cli_4.0-SNAPSHOT"

export KNET_NEOEXPORT_HOME="$ondex_mods/neo4j-export/target/neo4j-exporter"
export NEO4J_HOME="$ondex_mods/neo4j-export/target/neo4j.server/neo4j-community-4.3.2"

export KNET_NEO_URL="bolt://localhost:7687"
export KNET_NEO_USR="neo4j"
export KNET_NEO_PWD="test"

. config/common-cfg.sh

conda activate snakemake
