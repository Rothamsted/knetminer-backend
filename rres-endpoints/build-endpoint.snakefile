
# TODO: env vars don't work in SLURM?
#
KNET_SRC_OXL = os.environ [ "KNET_SRC_OXL" ]
KNET_DATASET_TARGET = os.environ [ "KNET_DATASET_TARGET" ]
	
dataset_id = config [ "dataset_id" ]
dataset_version = config [ "dataset_version" ]

all_results = [
	f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl",
	f"{KNET_DATASET_TARGET}/rdf/knowledge-graph.ttl.bz2",
	f"{KNET_DATASET_TARGET}/rdf/ontologies",
	f"{KNET_DATASET_TARGET}/rdf/knowledge-graph-metadata.ttl",
	f"{KNET_DATASET_TARGET}/tmp/tdb",
	f"{KNET_DATASET_TARGET}/rdf/tdb.tar.bz2"
]

# TODO: possibly, we need a similar trick for ontologies, TDB, etc
if os.environ [ "KNET_DATASET_HAS_NEO4J" ]:
	all_results.append ( f"{KNET_DATASET_TARGET}/neo4j.dump" )


# The usual umbrella, which possibly triggers everything else
rule all:
	input:
		all_results

# Steps are listed in the same order they're executed
#

# TODO: we might want a condition for the case where the OXL already has URIs, eg, in this 
# case we just link it to the new name.
# At the moment, you can skip this rule by placing an URI-containing OXL on to the rule's 
# target.
#
rule add_uris:
	input:
		KNET_SRC_OXL
	output:
		f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl"
	shell:
		f"./endpoint-steps/add-uris.sh '{dataset_id}' '{dataset_version}'"


# This should happen in parallel with rdf_export
rule dataset_metadata:
	input:
		f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl"
	output:
		f"{KNET_DATASET_TARGET}/rdf/knowledge-graph-metadata.ttl"
	shell:
		f"./endpoint-steps/create-dataset-metadata.sh '{dataset_id}' '{dataset_version}'"


rule rdf_export:
	input:
		f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl"
	output:
		f"{KNET_DATASET_TARGET}/rdf/knowledge-graph.ttl.bz2"
	shell:
		f"./endpoint-steps/rdf-export.sh '{dataset_id}' '{dataset_version}'"


rule tdb_load:
	input:
		f"{KNET_DATASET_TARGET}/rdf/knowledge-graph.ttl.bz2"
	output:
		directory ( f"{KNET_DATASET_TARGET}/rdf/ontologies" ),
		directory ( f"{KNET_DATASET_TARGET}/tmp/tdb" )
	shell:
		f"./endpoint-steps/tdb-load.sh '{dataset_id}' '{dataset_version}'"	


rule neo_export:
	input:
		f"{KNET_DATASET_TARGET}/tmp/tdb"
	output:
		f"{KNET_DATASET_TARGET}/neo4j.dump"
	shell:
		f"./endpoint-steps/neo-export.sh '{dataset_id}' '{dataset_version}'"


# We deliver a zipped TDB, ready for download. This is done after we are sure it was good for Neo4j.
# The TDB is re-used by AgriSchemas, to build the Knetminer mapping.
#
rule tdb_zip:
	input:
		f"{KNET_DATASET_TARGET}/tmp/tdb"
	output:
		f"{KNET_DATASET_TARGET}/rdf/tdb.tar.bz2"
	shell:
		f"./endpoint-steps/tdb-zip.sh '{dataset_id}' '{dataset_version}'"

# You might need to run servers-sync.sh manually after this
#	