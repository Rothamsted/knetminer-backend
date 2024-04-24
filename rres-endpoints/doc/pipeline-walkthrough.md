## Updating installed scripts and tools

Before running the endpoints scripts, we typically need to update the software that it uses:

```bash
cd /home/data/knetminer/software # Everything is here

cd knetminer-backend # This hereby repo
git status # Check if there are pending changes, commit or delete them
git pull

# Clone of https://github.com/Rothamsted/knetminer, its /datasets/ dir is needed
cd ../knetminer 
git status/pull

cd ..
# These two are auto-updated, but they're worth a check:
# - Check rdf-export-2-cli
# - Check ondex-mini, where there is tools/neo4j-exporter, the OXL/RDF/Neo4j converter
# 
```

## Configuration

As explained in the main [README](../README.md), the pipeline can be configured to work with a given dataset ID and a given dataset version (eg, cereals 57), and a given dataset+version can work with a given environment.

The configuration is hierarchical. Defaults are set by [config/default-cfg.sh](../config/default-cfg.sh), which invokes `config/environments/$envName-env.sh` (`$envName` is a command line parameter), and then invokes `config/datasets/$datasetId-$version-cfg.sh` (`$datasetId ` and `$version` are command line parameters too). This means that environment-specific config settings can override or extend defaults (by using them) and then dataset-specific config can override/extend either defaults or environment settings.

Most pipeline scripts invoke (using the Bash [source command](https://www.baeldung.com/linux/source-include-files)) `default-cfg.sh` as a first step. This script has also a special behaviour: it checks the three command line arguments, which must be: `$datasetId`, `$datasetVersion` and an optional `$environmentId`. These parameters are used to find specific config scripts, as said above. 

So, for instance, the dataset building pipeline can be launched this way:

```bash
# ==> This is where we have the pipeline scripts deployed, we won't repeat this 
# in the examples below
cd /home/data/knetminer/software/knetminer-backend/rres-endpoints
git pull # Optional, this is a mirror of the knetminer-backend repo and you might want to update it
./build-endpoint.sh 'cereals-dummy' 1 rres # quote datasetId if it contains punctuation
```
All the scripts that need it, will call the `default-cfg.sh`, which will check the CLI arguments and invoke specific config scripts as said above.

**Tip**: a quick way to see the same variables that the pipeline scripts see is:

```bash
# '.' is the abbreviations of 'source', ie, run it as it was commands typed in the invoking shell
. config/default-cfg.sh 'cereals-dummy' 1 rres

$ echo $KETL_SRC_OXL
/home/data/knetminer/etl-test/cereals-dummy/cereals-dummy-1.oxl
$ echo $KETL_OUT
/home/data/knetminer/pub/endpoints/cereals-dummy/1
$ set | grep 'KETL|KNET|RSYNC|JAVA' # shows all the variables defined by the config files
...
```

### Dataset config
As explained in the README, for a new dataset, you should define a dataset+version specific config and place it in `config/datasets/$datasetId-$version-cfg.sh`. In our walkthrough example, this is [config/datasets/cereals-dummy-1-cfg.sh](../config/datasets/cereals-dummy-1-cfg.sh).

In this file, `KETL_OUT` defines that all the pipeline output files are rooted at `/home/data/knetminer/pub/endpoints/cereals-dummy/1/`. The value of this depends the previous definition of `KETL_OUT_HOME`, which in turn, depends on KNET_HOME. Both these two vars are defined in the environment config, at [config/environments/rres-env.sh](../config/environments/rres-env.sh).

## Metadata descriptor
Another configuration file that you need is the [metadata descriptor](../config/datasets/cereals-dummy-1-metadata-descriptor.properties). This contains properties to describe the dataset as a whole, and it's used to generate corresponding RDF, by the metadata exporter tool, which is invoked as part of the endpoint pipeline (see the [metadata creation step](../endpoint-steps/create-dataset-metadata.sh)).

### Environment configuration
For this walkthrough, we'll use the RRes environment, its shared directories our deployments on them and SLURM, the cluster framework to send batch jobs to high-performant computing hosts and in parallel (more below).

As per the main README, the config for this environment is at [config/environments/rres-env.sh](config/environments/rres-env.sh). As said above, this defines the pipeline working directory and the path of the input OXL. It also has pointers to software tools such as the OXL-to-RDF exporter or the Neo4j server that the pipeline uses to prepare a Neo dump from the OXL (more below). These tools are pre-installed before running the pipeline.

The RRes environment uses this:

```bash
export KETL_SNAKE_OPTS="--profile config/snakemake/slurm"
```

Which is picked by `build-endpoint.sh` and passed to SnakeMake, this tells SnakeMake to use SLURM to run workflows, and with the SLURM config options specified in the `--profile` directory (in a format prescribed by Snakemake). In other words, with this options, SnakeMake runs workflow steps automatically on SLURM, including submitting parallel jobs when the workflow allows for it.


## Running the data building workflow

After having defined all the config you need as explained above, you can run the first stage of the endpoint building, the one that prepares data dumps from the OXL. As explained in the main README, RDF and Neo4j dumps are the main output files produced by this stage.

It works like shown above:

```bash
. config/environments/rres-conda-init.sh # Conda/Snakemake environment needed, see below
./build-endpoint.sh 'cereals-dummy' 1 rres
```

**WARNING**: this **requires** that Snakemake is configured and in your executables PATH. Usually, Snakemake is installed from the Mamba, an Anaconda/Conda framework. The `rres-conda-init.sh` script above activates Conda and activates the environment where SnakeMake was installed and configured (Conda supports multiple working environment). You can use it or your own flavour of 
Snakemake, just **initialise it somehow**.

**Tip**: you might want to start testing this with uncommenting this in `rres-env.sh`:

```bash
export KETL_SNAKE_OPTS="$KETL_SNAKE_OPTS --dry-run"
```

That is, with `--dry-run`, Snakemake will simulate its work, without actually running workflow steps.

You should see the pipeline described in the main README running, with output about the completion of each stage. You'll also see that SLURM jobs are submitted at each step. Something like this:

```bash
$ ./build-endpoint.sh 'cereals-dummy' 1 rres
Building DAG of jobs...
Using shell: /usr/bin/bash
Provided cluster nodes: 16
Job stats:
job                 count    min threads    max threads
----------------  -------  -------------  -------------
all                     1              1              1
dataset_metadata        1              1              1
neo_dump                1              1              1
neo_export              1              1              1
neo_index               1              1              1
tdb_load                1              1              1
tdb_zip                 1              1              1
total                   7              1              1

Select jobs to execute...

[Wed Apr 24 18:36:22 2024]
rule dataset_metadata:
    input: /home/data/knetminer/pub/endpoints/cereals-dummy/1/tmp/knowledge-graph-uris.oxl
    output: /home/data/knetminer/pub/endpoints/cereals-dummy/1/knowledge-graph-annotated.oxl, /home/data/knetminer/pub/endpoints/cereals-dummy/1/rdf/knowledge-graph-metadata.ttl
    jobid: 1
    resources: tmpdir=/tmp

Submitted job 1 with external jobid 'Submitted batch job 328746'.
...
```

**Tip**: You can use the '&' modifier at the end of the command line above to, to let the pipeline run asynchronously and meanwhile, get back to the shell and use it check the generated output. For real datasets, completing the pipeline will take a few hours, so you'll want both '&' and 'nohup' (redirect the nohup output to `logs/build-endpoint.out`). These are [all Bash features](https://www.maketecheasier.com/run-bash-commands-background-linux/). 

As you see, the running pipeline should generate output in `/home/data/knetminer/pub/endpoints/cereals-dummy/1/`. As explained in the main README, this includes:

```bash
$ ll /home/data/knetminer/pub/endpoints/cereals-dummy/1/
total 38M
# the OXL + URIs and metadata
-rwxrwx--- 1 brandizim knetminer users 3.5M Apr 24 18:37 knowledge-graph-annotated.oxl*
# The Neo4j dump (in the format provided by the neo4j-admin command)
-rwxrwx--- 1 brandizim knetminer users  29M Apr 24 18:53 neo4j-5.16.0.dump*
# RDF data (which are to be published and were used for the Neo dump)
drwxrws--- 3 brandizim knetminer users  144 Apr 24 18:38 rdf/
# Temp/working files that can be disposed
drwxrws--- 4 brandizim knetminer users  188 Apr 24 18:52 tmp/
(snakemake) 19:14:01 [brandizim@rothhpc4-login rres-endpoints]$

# The RDF files, used to populate the Neo4j dump and also to update the SPARQL endpoint,
# using scripts in /cyverse-endpoints

$ ll /home/data/knetminer/pub/endpoints/cereals-dummy/1/rdf
total 19M
# The metadata descriptor
-rwxrwx--- 1 brandizim knetminer users 4.2K Apr 24 18:37 knowledge-graph-metadata.ttl*
# The data
-rwxrwx--- 1 brandizim knetminer users 3.4M Apr 24 17:29 knowledge-graph.ttl.bz2*
# The schema/ontology files, coming from the RDF exporter config
drwxrws--- 3 brandizim knetminer users  151 Apr 24 18:38 ontologies/
# TDB is an RDF database, which we make available for local access to RDF
-rwxrwx--- 1 brandizim knetminer users  14M Apr 24 18:39 tdb.tar.bz2*
(snakemake) 19:16:01 [brandizim@rothhpc4-login rres-endpoints]$
```

**Other relevant output** is in `rres-endpoints/.snakemake/log/`. In particular, here you find files named `slurm-*.out`, each corresponding to a pipeline step and each reporting their standard output + error. See a [sample output here](example-log).



## Updating target servers
Once the Snakemake pipeline has generwted [servers-sync.sh](../servers-sync.sh). This has a couple of tasks:
* It copies dump files onto the location accessible from `https://knetminer.com/downloads`, to make them available to the public (usually upon request, see below), as well as to ourselves (eg, to update RRes-external servers).
* It updates an internal Neo4j server with the latest generated dump.
* For the `poaceae-free` dataset, it updates the servers running our (old KnetMiner) test instances, [Ondex-based version](https://knetminer.com/ci-test/client/) and [Cypher-based version](https://knetminer.com/ci-test-cypher/client/).

All of the steps are enabled/disabled via config variables, see below.

### Checking/preparing the server synch config

TODO


### Updating the web download location

This requires you pre-define a couple of things:

* Create a new secret for the dataset+version: 
```bash
# KNET_WEB_SECRETS is set to /home/data/knetminer/software/secrets/web-data
# 
cat /dev/urandom \
  | head -n 10000 \
  | shasum -a 1 \
  | cut -d' ' -f1 \
  >$KNET_WEB_SECRETS/cereals-dummy-1.key
# Explaination:
# - cat gets an endless stream of random chars
# - head stops the at 10k chars
# - shasum generates a hash (plus some decorating chars)
# - cut keeps the hash
```

The secret is needed if you want to put the downloads behind hashed URLs, so that only those who have the hashed URLs can download the files. That's usually the case for production datasets.

* SSH to the web server (use another terminal) and create the corresponding hashed dir:

```bash
# Obviously, YOU NEED THE CORRESPONDING PERMISSIONS:
$ ssh -C brandizim@babvs59.rothamsted.ac.uk
$ sudo -H bash -i # not needed if you're in the right Unix group
$ cd /var/www/html/knetminer/downloads/reserved
# This is the value you stored in $KNET_WEB_SECRETS/cereals-dummy-1.key The code used
# here IS NOT REAL
$ mkdir -p cereals-dummy/1/3aeb45

# IMPORTANT: ensure that files are not listed by the web server 
# s = new files are assigned to the knetminer_access group
# x = (for dirs) the dir contents can be accessed, but not listed when 'r' is missing
# 
$ chmod u=rwx,g=rs,o=rx cereals-dummy 
$ chmod u=rwx,g=s,o=x cereals-dummy/1
$ chmod u=rwx,g=rwsx,o=rx cereals-dummy/1/3aeb45
$ echo 'Hello World' >cereals-dummy/1/3aeb45/test.txt
$ chmod ug=rw,o=r cereals-dummy/1/3aeb45/test.txt

# OR USE YOUR USER, the same you use to connect with servers-sync.sh from the SLURM manager 
# (rothhpc4-login)
$ chown -R brandizim cereals-dummy 
```

**Important**: Now check this on a browser:
* https://knetminer.com/downloads/reserved says forbidden
* https://knetminer.com/downloads/reserved/cereals-dummy lists the '1' version
* https://knetminer.com/downloads/reserved/cereals-dummy/1 says forbidden
* https://knetminer.com/downloads/reserved/cereals-dummy/1/3aeb45 lists test.txt and it can be opened


### Running the sync

When this works, you're ready for updating the downloads/ dir. Go back to rothhpc4-login and run:

```bash
$ ./servers-sync.sh 'cereals-dummy' 1 rres
```

If you didn't see errors, you should now see the dump files at https://knetminer.com/downloads/reserved/cereals-dummy/1/3aeb45

### Updating the Neo4j server
TODO

### Updating the CI test instances
TODO

