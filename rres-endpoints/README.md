# The Knetminer Raw Data Pipelines

The script you find in the hereby folder and [/cyverse-endpoints](../cyverse-endpoints) are used to
produce machine-readable data from the Knetminer datasets, when these are shipped in our custom
OXL format.  

Note that files in these directories are independent on the main POM project, they're included in the same
git repository due to their conceptually-similar function.


# RRes Endpoints 

For the RRes pipeline, the data are built mostly via [SnakeMake][10] and running SnakeMake steps (rules)
against our SLURM cluster. The RRes environment (see below), loads SnakeMake from the available SLURM modules.

[10]: https://snakemake.readthedocs.io/en/stable/getting_started/installation.html

### Environments

All the endpoint scripts and pipelines require that you manually initialise an environment.
Essentially, an environment is a set of operating system variables (and possibly, other settings), 
which of values are specific to the particular host/infrastructure/etc where you are operating.

Typically, we have an [environment for the Rothmasted Research hosts](config/environments/rres-env.sh) 
(including the SLURM cluster nodes), and then each KnetMiner developer has its own environment, which refers to 
his/her personal PC ([example](config/environments/brandizi-env.sh)).

As you can see, environments are initialised via [`config/environments/xxx-env.sh`](config/environments/) scripts.
You should add further environments there.

### Common configuration and defaults overriding  
An environment script should always [bash-source](https://linuxize.com/post/bash-source-command/) the 
file [`config/common-cfg.sh`](config/common-cfg.sh) as final step. This initialises default and common settings, 
which usually apply to all enviroments.  

Many of the variables that this scripts defines are defaults that can be overridden by your environment script.
For instance, if your `xxxx-env.sh` file defines `KNET_DATA_TARGET` (the output root directory), then the common
config script won't redefine this as `/home/data/knetminer/pub/endpoints` (the default location in the NFS file system).  

Note that, some of the variables defined by the common script can't be overriden at the moment, since they aren't defined
with the pattern "set to the default unless it already has a value". Change the `common-cfg.sh` to introduce this pattern
where needed (please, think carefully of all the use cases at issue and notify your collaborators).   

### Dataset-depending and Common Configuration

Most of the scripts described below get their configuration from the chain described in this section. 

* First, they rely on an environment, which you've to initialise manually. As said above, the environment initialisation
  should brings up defaults too.
  
* As a fist step, the pipeline scripts usually bash-source [`config/init-dataset-cfg.sh`](config/init-dataset-cfg.sh).
  This checks that the parent script has been called with two parameters: a dataset-id and a dataset version. 
  The two are used to set the variables `KNET_DATASET_ID` and `KNET_DATASET_VERSION`, then the two variables are used 
  to seek for and run the file `config/datasets/${KNET_DATASET_ID}-${KNET_DATASET_VERSION}-cfg.sh`, for instance, 
  [`config/datasets/poaceae-51-cfg.sh`](config/datasets/poaceae-51-cfg.sh). 
  Then, [`config/dataset-cfg.sh`](config/dataset-cfg.sh) is also invoked, which defines a couple of common, 
  dataset-depending settings (which rely on the above variables). So, the idea is that part of the pipeline configuration
  depend on a dataset ID and version, hence, this parameter pair has always to be specified upon pipeline script 
  invocations and a corresponding dataset initialisation script has to be defined in `config/datasets-$id-$ver-cfg.sh`.
  The latter might be empty, we enforce its presence in order to have at least a placeholder that marks that a given 
  dataset exists and is supported by our pipeline.
  
  
## The RDF/Neo4j Building Pipeline 

This is the main Knetminer raw data pipeline. It starts from an Ondex file (`.oxl`) containing a dataset, converts
this custom knowledge graph format into RDF, uses the RDF to populate a Neo4j database. The pipeline's output is a set
of dump files. These are used with other scripts and pipelines (see below) to do things like re-populating our
data servers (Virtuoso for RDF, Neo4j) and similar tasks.

As said above, the RDF/Neo4j pipeline is based on the SnakeMake workflow system. As you can see in the 
[Snakemake file](build-endpoint.snakefile), the pipeline is a chain of steps. We recommend that you invoke Snakemake
via the [build-endpoint.sh](build-endpoint.sh) wrapper. As explained above, this should be run after having initialised
some environment and using dataset parameters.

The pipeline steps are as follow.

1. [add-uris](endpoint-steps/add-uris.sh): this adds URIs to the nodes/edges of the initial OXL and outputs a new OXL
   as result. This is based on the URI-adding tool available from the [OXL/RDF exporter][10]. If you already have 
   the URIs in the OXL (typically, because the URI addition is included in the Mini workflow that generated the pipeline), 
   you can skip this step by placing the URI-equipped OXL onto the path that the next step expects as input 
   (`$KNET_DATASET_TARGET/knowledge-graph-uris.oxl`). 
1. [dataset-metadata](endpoint-steps/create-dataset-metadata.sh): this invokes the [OXL metadata descriptor tool][10] to produce a schema.org-compatible RDF file, which describes the OXL as a whole.
1. [rdf-export](endpoint-steps/rdf-export.sh): this exports the OXL to RDF, using the [OXL/RDF exporter][10].

1. [tdb-load](endpoint-steps/tdb-load.sh): this takes the Knetminer's RDF produced at the previous step and uses the 
   [Ondex Neo4j exporter][20] to load that RDF, plus a number of static files (mainly, relevant ontology definitions), 
   into a new [Jena TDB triple store][22]. This is used by the downstream steps, as well as other Rothamsted tools, 
   such as the [AgriSchema mappers][24]. Due to that, the TDB files produced by this step are made as part of the 
   data downloads for the dataset (see below).

1. [neo-export](endpoint-steps/neo-export.sh): this uses the [Ondex/Neo4j Exporter][20] to convert the TDB generated at 
	 the previous step into a Neo4j database. In order to do that, a Neo4j database server is emptied, started, used with 
	 the exporter, then the database is stopped and finally the Neo4j's admin command is used to produce a database dump. 
   The Neo4j database that is used for this step is configurable on a per-environment basis, via scripts in 
   [config/neo4j](config/neo4j). In particular, the RRes has its own Neo4j location on the file system
   and has special scripts for [starting](config/neo4j/neo-start.sh)/[stopping](config/neo4j/neo-stop.sh) Neo4j on top 
   of our SLURM cluster (see also [utils/neo-slurm-start.sbatch](utils/neo-slurm-start.sbatch).  

1. [tdb-zip](endpoint-steps/tdb-zip.sh): after a successful RDF export and Neo4j population, this step simply zips 
   the TDB files, preparing a tarball to be used as part of data downloads (see below).
 
[10]: https://github.com/Rothamsted/knetbuilder/blob/master/ondex-knet-builder/modules/rdf-export-2/README.md 
[20]: https://github.com/Rothamsted/knetbuilder/tree/master/ondex-knet-builder/modules/neo4j-export
[22]: https://jena.apache.org/documentation/tdb/
[24]: https://github.com/Rothamsted/agri-schemas

## The Servers Updating Pipeline

After the data are created by the RDF/Neo4j pipeline above, these are used to update a number of test servers in the
RRes infrastructure (explained in this section), and also to update external resources in the Cyverse infrastructure
(explained in the next section).

Regarding the RRes updates, these are based on the [servers-sync.sh](servers-sync.sh) script (which doesn't require
Snakemake). It contains the steps:

* Updates the data download locations at the [Knetminer web site](https://knetminer.com/downloads/), using rsync to
  transfer the data dumps that the pipeline above created for a working dataset.
   
* Updates the Neo4j test server for the current dataset (with the Neo4j data dump).

* Updates the test Knetminer instances that use the current dataset. As usually, these are based on Docker and this 
  step transfers the URI-added OXL to the relevant Docker servers and re-start their containers via SSH.

Any above steps can be omitted, if a dataset doesn't need them. This is controlled by variables that can be unset
by the dataset-specific configuration (see above). See the `servers-sync.sh` file for details. 

### Secrets management

For private (non-public) Knetminer datasets, we define secret values in these default directories:

```bash
export KNET_SECRETS_DIR=/home/data/knetminer/software/secrets
export KNET_WEB_SECRETS_DIR="$KNET_SECRETS_DIR/web-data"
```

In particular, `KNET_WEB_SECRETS_DIR` contains hash codes that are used in the data download locations to create obfuscated
directories (which are only accessible to users knowing the hash). These directories have to be created manually on the
destination web server for knetminer.com.


# CyVerse Endpoints

Part of the RDF and Neo4j dumps produced at Rothamsed are used on our CyVerse servers, in order 
to expose our data to the world, via our [SPARQL and Neo4j endpoints][10].

[10]: https://knetminer.com/data

See [here](../cyverse-endpoints) for details.
