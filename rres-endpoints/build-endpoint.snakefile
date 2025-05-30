
# TODO: env vars don't work in SLURM?
#
KETL_SRC_OXL = os.environ [ "KETL_SRC_OXL" ]
KETL_OUT = os.environ [ "KETL_OUT" ]
KETL_NEO_VERSION = os.environ [ "KETL_NEO_VERSION" ]

dataset_id = config [ "dataset_id" ]
dataset_version = config [ "dataset_version" ]

all_results = [
	f"{KETL_OUT}/knowledge-graph-annotated.oxl",
	f"{KETL_OUT}/rdf/knowledge-graph.ttl.bz2",
	f"{KETL_OUT}/rdf/ontologies",
	f"{KETL_OUT}/rdf/knowledge-graph-metadata.ttl",
	f"{KETL_OUT}/tmp/tdb",
	f"{KETL_OUT}/rdf/tdb.tar.bz2"
]

# This is done only when KETL_HAS_NEO4J is set
# TODO: possibly, we need a similar trick for ontologies, TDB, etc
if os.environ [ "KETL_HAS_NEO4J" ] == 'true':
	all_results.append ( f"{KETL_OUT}/neo4j-{KETL_NEO_VERSION}.dump" )


# The usual umbrella, which possibly triggers everything else
rule all:
	input:
		all_results

# Steps are listed in the same order they're executed.
# When possible, input and output params are passed as a convenient way to keep them
# synched with the hereby file.
#

rule add_uris:
	input:
		KETL_SRC_OXL
	output:
		f"{KETL_OUT}/tmp/knowledge-graph-uris.oxl"
	shell:
		'./endpoint-steps/add-uris.sh "{input}" "{output}"'


# This should happen in parallel with rdf_export
rule dataset_metadata:
	input:
		f"{KETL_OUT}/tmp/knowledge-graph-uris.oxl"
	output:
		f"{KETL_OUT}/knowledge-graph-annotated.oxl",
		meta_rdf = f"{KETL_OUT}/rdf/knowledge-graph-metadata.ttl"
	shell:
		'./endpoint-steps/create-dataset-metadata.sh "{input}" "{output[0]}" "{output.meta_rdf}"'


rule rdf_export:
	input:
		f"{KETL_OUT}/tmp/knowledge-graph-uris.oxl"
	output:
		f"{KETL_OUT}/rdf/knowledge-graph.ttl.bz2"
	shell:
		'./endpoint-steps/rdf-export.sh "{input}" "{output}"'


rule tdb_load:
	input:
		f"{KETL_OUT}/rdf/knowledge-graph.ttl.bz2",
		meta_rdf = f"{KETL_OUT}/rdf/knowledge-graph-metadata.ttl"
	output:
		directory ( f"{KETL_OUT}/tmp/tdb" ),
		ontologies = directory ( f"{KETL_OUT}/rdf/ontologies" )
	shell:
		'./endpoint-steps/tdb-load.sh "{input[0]}" "{input.meta_rdf}" "{output[0]}" "{output.ontologies}"'	

# ==> NOTE: review the options in $NEO4J_HOME about the transaction log retentions, 
# to get rid of past transactions retention, else, the dump can be much bigger than 
# necessary. 
#
# See: https://neo4j.com/docs/operations-manual/current/database-internals/transaction-logs/
#

rule neo_export:
	input:
		f"{KETL_OUT}/tmp/tdb"
	output:
		f"{KETL_OUT}/tmp/neo-export.flag"
	shell:
		'./endpoint-steps/neo-export.sh "{input}" "{output}"'


rule neo_index:
	input:
		f"{KETL_OUT}/tmp/neo-export.flag"
	output:
		f"{KETL_OUT}/tmp/neo-index.flag"		
	shell:
		'./endpoint-steps/neo-index.sh "{output}"'


rule neo_dump:
	input:
		f"{KETL_OUT}/tmp/neo-index.flag"		
	output:
		f"{KETL_OUT}/neo4j-{KETL_NEO_VERSION}.dump"
	shell:
		'./endpoint-steps/neo-dump.sh "{output}"'


# We deliver a zipped TDB, ready for download. 
# The TDB is re-used by AgriSchemas, to build the Knetminer mapping.
#
# WARNING: for sake of performance, this is done in parallel with rdf2neo and 
# its downstream steps. This implies that if something about Neo export fails
# due to TDB or RDF being wrong, you MUST delete the corresponding intermediate
# files manually. 
#
rule tdb_zip:
	input:
		f"{KETL_OUT}/tmp/tdb"
	output:
		f"{KETL_OUT}/rdf/tdb.tar.bz2"
	shell:
		'./endpoint-steps/tdb-zip.sh "{input}" "{output}"'

# You might need to run servers-sync.sh manually after this
#	