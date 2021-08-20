
# TODO: envvars doesn't work in SLURM, check them manually
#
KNET_SRC_OXL = os.environ [ "KNET_SRC_OXL" ]
KNET_DATASET_TARGET = os.environ [ "KNET_DATASET_TARGET" ]
	
dataset_id = config [ "dataset_id" ]
dataset_version = config [ "dataset_version" ]

rule all:
	input:
		f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl",
		f"{KNET_DATASET_TARGET}/rdf/knowledge-graph.ttl.bz2",
		f"{KNET_DATASET_TARGET}/rdf/ontologies",
		f"{KNET_DATASET_TARGET}/neo4j.dump",
		f"{KNET_DATASET_TARGET}/tmp/tdb"

rule add_uris:
	input:
		KNET_SRC_OXL
	output:
		f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl"
	shell:
		f"./add-uris.sh '{dataset_id}' '{dataset_version}'"

rule rdf_export:
	input:
		f"{KNET_DATASET_TARGET}/knowledge-graph-uris.oxl"
	output:
		f"{KNET_DATASET_TARGET}/rdf/knowledge-graph.ttl.bz2"
	shell:
		f"./rdf-export.sh '{dataset_id}' '{dataset_version}'"

rule tdb_load:
	input:
		f"{KNET_DATASET_TARGET}/rdf/knowledge-graph.ttl.bz2"
	output:
		directory ( f"{KNET_DATASET_TARGET}/rdf/ontologies" ),
		directory ( f"{KNET_DATASET_TARGET}/tmp/tdb" )
	shell:
		f"./tdb-load.sh '{dataset_id}' '{dataset_version}'"	

rule neo_export:
	input:
		directory ( f"{KNET_DATASET_TARGET}/tmp/tdb" )
	output:
		f"{KNET_DATASET_TARGET}/neo4j.dump"
	shell:
		f"./neo-export.sh '{dataset_id}' '{dataset_version}'"

rule tdb_zip:
	input:
		directory ( f"{KNET_DATASET_TARGET}/tmp/tdb" )
	output:
		f"{KNET_DATASET_TARGET}/rdf/tdb.tar.bz2"
	shell:
		f"./tdb-zip.sh '{dataset_id}' '{dataset_version}'"
