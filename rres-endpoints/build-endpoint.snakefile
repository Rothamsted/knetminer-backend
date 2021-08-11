
envvars:
	"CFG_OXL_SRC",
	"CFG_DATASET_TARGET"

CFG_OXL_SRC = os.environ [ "CFG_OXL_SRC" ]
CFG_DATASET_TARGET = os.environ [ "CFG_DATASET_TARGET" ]
	
param_prefix = config [ "param_prefix" ]

rule add_uris:
	input:
		CFG_OXL_SRC
	output:
		f"{CFG_DATASET_TARGET}/knowledge-graph-uris.oxl"
	shell:
		f"./add-uris.sh '{param_prefix}'"

