# Test Data Server for Knetminer

This is a test data server, which prepares data resources for testing Knetminer. What it does at the moment is:

  * It crafts a modified version of a [small arabidopsis sample dataset][1], which makes these changes to the original 
  OXL:
    * adds URIs to all concepts and relations, using the [URI Addition plugin][2].
    * [adds a few reference concepts][3], which are useful to write tests.
    * The above modified OXL is made (manually, during development) using [this script](update-resources.sh). 
  * It crafts a test Neo4j database, based on the modified sample OXL above. It can also run it before running your tests
  (see [here](../resources-init.sh)).
  
The generated test files (Neo4j DB, test OXL) are included in the bunlde that Maven builds with this project, 
An example of how such test data can be downloaded and used is [available on Knetminer][4] 
(look for `<phase>generate-test-resources</phase>`).

[1]: https://s3.eu-west-2.amazonaws.com/nfventures-testing.knetminer/default.oxl%20
[2]: https://github.com/Rothamsted/ondex-knet-builder/blob/master/modules/rdf-export-2/src/main/java/net/sourceforge/ondex/rdf/export/URIAdditionPlugin.java
[3]: src/test/java/uk/ac/rothamsted/knetminer/backend/test/OxlTestDataCreator.java  
[4]: https://github.com/Rothamsted/knetminer/blob/master/common/aratiny/aratiny-ws/pom.xml#L60