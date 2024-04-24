## Configuration

As explained in the main [README](README.md), the pipeline can be configured to work with a given dataset ID and a given dataset version (eg, cereals 57), and a given dataset+version can work with a given environment.

The configuration is hierarchical. Defaults are set by [config/default-cfg.sh](config/default-cfg.sh), which invokes `config/environments/$envNsme-env.sh` (`$envName` is a command line parameter), and then invokes `config/datasets/$datasetId-$version-cfg.sh` (`$datasetId ` and `$version` are command line parameters too). This means that environment-specific config settings can override or extend defaults (by using them) and then dataset-specific config can override/extend either defaults or environment settings.

Most pipeline scripts invoke (using the Bash [source command](https://www.baeldung.com/linux/source-include-files)) `default-cfg.sh` as a first step. This script has also a special behaviour: it checks the three command line arguments, which must be: `datasetId`, `datasetVersion` and an optional `environmentId`. These parameters are used to find specific config scripts, as said above. 

So, for instance, the dataset building pipeline can be launched this way:

```bash
# This is where we have the pipeline scripts deployed, we won't repeat this in the examples below
cd /home/data/knetminer/software/knetminer-backend/rres-endpoints
git pull # Optional, this is a mirror of the knetminer-backend repo and you might want to update it
./build-endpoint.sh 'cereals-free' 1 rres # quote datasetId if it contains punctuation
```
All the scripts that need it, will call the `defeult-cfg.sh`, which will check the CLI arguments and invoke specific config scripts as said above.

**Tip**: a quick way to see the same variables that the pipeline scripts see is:

```bash

```

### Dataset config
As explained in the README, for a new dataset, you should define a dataset+version specific config and place it in `config/datasets/$datasetId-$version-cfg.sh`. In our walkthrough example, this is [config/datasets/cereals-dummy-1-cfg.sh](config/datasets/cereals-dummy-1-cfg.sh).

In this file, `KETL_OUT` defines that all the pipeline output files are rooted at `/home/data/knetminer/pub/endpoints/cereals-dummy/1/`. The value of this depends the previous definition of `KETL_OUT_HOME`, which in turn, depends on KNET_HOME. Both these two vars are defined in the environment config, at [config/environments/rres-env.sh](config/environments/rres-env.sh).

### Environment configuration
For this walkthrough, we'll use the RRes environment, its shared directories our deployments on them and SLURM, the cluster framework to send batch jobs to high-performant computing hosts and in parallel (more below).

As per the main README, the config for this environment is at [config/environments/rres-env.sh](config/environments/rres-env.sh). As said above, this defines the pipeline working directory and the path of the input OXL. It also has pointers to software tools such as the OXL-to-RDF exporter or the Neo4j server that the pipeline uses to prepare a Neo dump from the OXL (more below). These tools are pre-installed before running the pipeline.

