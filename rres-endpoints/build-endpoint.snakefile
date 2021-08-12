
# TODO: envvars doesn't work in SLURM, check them manually
#
CFG_OXL_SRC = os.environ [ "CFG_OXL_SRC" ]
CFG_DATASET_TARGET = os.environ [ "CFG_DATASET_TARGET" ]
	
param_prefix = config [ "param_prefix" ]

rule all:
	input:
		f"{CFG_DATASET_TARGET}/knowledge-graph.ttl.bz2"

rule add_uris:
	input:
		CFG_OXL_SRC
	output:
		f"{CFG_DATASET_TARGET}/knowledge-graph-uris.oxl"
	shell:
		f"./add-uris.sh '{param_prefix}'"

rule rdf_export:
	input:
		f"{CFG_DATASET_TARGET}/knowledge-graph-uris.oxl"
	output:
		f"{CFG_DATASET_TARGET}/knowledge-graph.ttl.bz2"
	shell:
		f"./rdf-export.sh '{param_prefix}'"
