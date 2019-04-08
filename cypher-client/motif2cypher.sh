# Quick-n-dirty facility to invoke the converter for SemanticMotifs.txt files.
# See the invoked class for details.
#
mvn exec:java \
  -Dexec.mainClass="uk.ac.rothamsted.knetminer.backend.cypher.genesearch.smtranslator.Motif2CypherCLI" \
  -Dexec.args="$1 $2 $3" \
	-Dexec.classpathScope="test"
