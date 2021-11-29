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

At the moment, we 

## Configuration



* Configuration and Alike
* Loading RDF Data
* Loading Neo4j

* Server services, paths and other utilities