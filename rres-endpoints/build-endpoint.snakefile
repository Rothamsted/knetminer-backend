
# TODO: envvars doesn't work in SLURM, check them manually
#
KNET_SRC_OXL = os.environ [ "KNET_SRC_OXL" ]
KNET_DATASET_TARGET = os.environ [ "KNET_DATASET_TARGET" ]
	
dataset_id = config [ "dataset_id" ]
dataset_version = config [ "dataset_version" ]

rule all:
	input:
		f"{KNET_DATASET_TARGET}/knowledge-graph.ttl.bz2"

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
		f"{KNET_DATASET_TARGET}/knowledge-graph.ttl.bz2"
	shell:
		f"./rdf-export.sh '{dataset_id}' '{dataset_version}'"
