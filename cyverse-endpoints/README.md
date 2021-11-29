# CyVerse Raw Data Endpoints

## Introduction

RDF and Neo4j dumps [produced at Rothamsed](../rres-endpoints) are used on our CyVerse servers, in 
order to expose our data to the world, via our [SPARQL and Neo4j endpoints][10].

[10]: https://knetminer.com/data

In this document, we explain a few details.

The raw data pipelines in the Cyverse servers picks up from the data dumps spawned by the RRes 
pipeline and published on [knetminer.com](http://knetminer.com/downloads). The scripts described
below a [dedicated utility](utils/knet-download.sh) for the download part.  

## Main scripts

We have two main scripts to work under CyVerse. One is [3store-load.sh](3store-load.sh), used to 
update the Virtuoso instance, the other is [neo4j-load.sh](neo4j-load.sh), used to update the
Neo4j server. 

See below for a list of data services available via CyVerse and the server hosts we use for them. 

## Configuration

As in the case of the [RRes pipelines](../rres-endpoints), most of the scripts for CyVerse requires that an enviroment
is set manually, by bash-sourcing one of the scripts into [config/environments](config/environments) (only one is 
available at the moment). Scripts of this type usually invokes [config/common-cfg.sh](config/common-cfg.sh), in order 
to define general settings, after environment-specific ones.  
  
Morover, the `3store-load.sh` and `neo4j-load.sh` scrips mentioned above requires a dataset-id and version parameter 
pair, in order to do some dataset-specific configuration. This is started by [config/init-dataset.sh](config/init-dataset.sh),
which first lookup for dataset-specific scripts into [config/datasets](config/datasets). For instance, the invocation
`./3store-load.sh poaceae 51` will cause the search for [config/datasets/poaceae-51-cfg.sh](config/datasets/poaceae-51-cfg.sh) 
and `config/datasets/poaceae-common-cfg.sh`. Both are optional and, of course, should contain configuration options 
valid for all versions of a dataset or those valid for a specific version.  

As a last step, the configuration sources [config/dataset-cfg.sh](config/dataset-cfg.sh), which has configuration options
to be set after dataset-specific settings have been defined.

## Loading RDF data

As said above, the [3store-load.sh](3store-load.sh) invokes a dataset-specific loading script located into 
[load](load), for instance, [load/poaceae-3sload.sh](load/poaceae-3sload.sh). In turn, these scripts might be using
the generic [load/knet-3sload.sh](load/knet-3sload.sh). This has these steps:

1. it downloads the RDF dump for the dataset into `/opt/data` (if not already there).
1. Using [virtuoso-utils][20], it loads the dataset into Virtuoso, putting it into the configured
   named graph for the dataset.
   
The steps above are repeated for those datasets that have an [AgriSchemas](https://github.com/Rothamsted/agri-schemas) 
mapping (ie, additional RDF that maps our data to bioschemas-based standards).  

Moreover, before the first step, the script also downloads ontology files that are part of the dataset, putting them 
into `/opt/data/rdf/ontologies` and then it loads them into Virtuoso, putting them into an ontology-dedicated named
graph. Note that multiple datasets might add up to the contents of this directory and they usually don't have 
conflicting files (only new files are downloaded anyway).

in [load](load) you can find other dataset-specific loading scripts that don't use the procedure above, since they
have their own peculiar characteristics. For instance, the [GXA data loader](load/gxa-3sload.sh) uses data produced
by the [AgriSchemas scripts][30], which are already based on the standards, and therefore the schema mappings aren't
used and the corresponding step isn't necessary. 


[20]: https://github.com/marco-brandizi/rdfutils/tree/master/virtuoso-utils
[30]: https://github.com/Rothamsted/agri-schemas/tree/master/dfw-dataset/gxa


## Loading Neo4j data

This should be done in the respective host (see below), using the [neo4j-load.sh](neo4j-load.sh) script.
This also uses a [generi Neo4j uploading script](load/neo-load.sh).  

**Note that we don't have (as yet) any Neo4j-dedicated script to update the COVID-19 dataset.**   

## Other scripts
Other scripts are in [utils](utils), including a [configuration backup script](utils/sys-bkp.sh) (at the moment it's 
manually run as necessary), the mentioned [data downloader](utils/knet-download.sh) and 
[controller](utils/neo-servers-service.sh) for the Neo4j servers.


## Services and hosts

### On '3store'

This is the internal host you reach when SSH-ing to CyVerse.  

*New accesses to our servers have to be agreed with CyVerse IT admins. The host names given here are mapped to internal
IPs via `/etc/hosts` files*

It contains the services.

**Tomcat Web server for the SPARQL browser**

The Tomcat web server serves both the [data entry page](https://knetminer.com/data) and
the [SPARQL browser][520]. The latter is based on a [customisation][530] of our own of the 
[LODEStar browser][540]. This software has several good features: a nice, customisable interface to play with SPARQL, 
a SPARQL and URI resolver, which can serve data in various formats, using [content negotiation][550]. [Example][560].    
  
This is deployed on `/opt/software/tomcat` (all the non-OS, non-packaged software is put under `/opt/software`).  
  
As said above, all data dumps and other data are under `/opt/data`.
  
**Virtuoso triple store**

The SPARQL browser sits on top of this. Of course, this is where all the RDF data are stored
and made accessible. We use Virtuoso to store multiple datasets, since this triple store supports the approach of 
associating named graphs to datasets, so, for example, a dataset can be deleted and re-uploaded from RDF files, while
keeping the others untouched. See [here][570] for a list of current datasets.

This service is based on the Docker image available for Virtuoso. In `/opt/software/virtuoso-docker`, you can find scripts
to restart the service from scratch (ie, they download the Docker image, or clean the Docker environment), Virtuoso is
restarted automatically when the Docker service is restarted (including, at boot).  

[520]: https://knetminer.com/data/sparql
[530]: https://github.com/Rothamsted/knetminer-lodestar/tree/knetminer
[540]: https://github.com/EBISPOT/lodestar
[550]: https://www.ebi.ac.uk/rdf/documentation/contentNegotiation/
[560]: http://knetminer.org/data/rdf/resources/gene_traescs1d02g156000
[570]: https://tinyurl.com/y2tt4dcy


### On 'wheat'

This host has an instance of Neo4j, which stores data about the poaceae dataset (initially, it has the T. aestivum 
dataset). Publicly, this serves [this Neo4j web browser](http://knetminer-wheat.cyverseuk.org:7474/) and 
the BOLT endpoint `bolt://knetminer-neo4j.cyverseuk.org:7687`.  

The Neo4j server is deployed on `/opt/software/neo4j` and is restarted by crond, using the script at
`/opt/software/cyverse-pipelines/utils/neo-servers-service.sh`.  

 
### On 'Arabidopsis'

Whe keep this host to serve the [COVID-19 data](https://f1000research.com/articles/10-703) via Neo4j. 
dataset). Publicly, this serves [this Neo4j web browser](http://knetminer-covid19.cyverseuk.org:7476/) and 
the BOLT endpoint `bolt://knetminer-neo4j.cyverseuk.org:7689`.  

As above, the Neo4j server is deployed on `/opt/software/neo4j-covid19` and the crond uses the same script at
`/opt/software/cyverse-pipelines/utils/neo-servers-service.sh`.  

This host has also a Neo4j server for the old Arabidopsis dataset, since this is embedded into the poaceae dataset 
mentioned above, we have turned off this Neo4j instance.
 
