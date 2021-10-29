**This is still a draft**

# The Knetminer Open Data Pipelines

The script you find in the hereby folder and [/cyverse-endpoints](../cyverse-endpoints) are used to
produce machine-readable data from the Knetminer datasets, when these are shipped in our custom
OXL format.  

Note that files in these directories are independent on the main POM project.


# RRes Endpoints 

For the RRes pipeline, the data are built mostly via SnakeMake and running SnakeMake steps (rules)
against our SLURM cluster.

## Configuration and alike

### Requirements

* The pipeline uses [SnakeMake][10]. The RRes environment (see below), loads SnakeMake from the 
  available SLURM modules.

[10]: https://snakemake.readthedocs.io/en/stable/getting_started/installation.html

### Environments

All the endpoint scripts and pipelines require that you manually initialise an environment.
Essentially, an environment is a set of operating system variables (and possibly, other settings), 
which of values are specific to the particular host/infrastructure/etc. where you are operating.

Typically, we have an [environment for the Rothmasted Research](config/environments/rres-env.sh) hosts 
(including the SLURM cluster nodes), and then each KnetMiner developer has its own environment 
([example](config/environments/brandizi-env.sh)), which refers to his/her personal PC.

As you can see, environments are initialised via [`config/environments/xxx-env.sh`](config/environments/) scripts. You should add further environments 

### Dataset-depending and Common Configuration


## The RDF/Neo4j Building Pipeline 


## The Servers Updating Pipeline
### Downloading and other storage locations
### Knetminer Instances
### Neo4j Instances


# Cyverse Endpoints

* Intro (where we pick up)
* Configuration and Alike
* Loading RDF Data
* Loading Neo4j

