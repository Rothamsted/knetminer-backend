# Quick-n-dirty facility to invoke the converter for SemanticMotifs.txt files.
# See the invoked class for details.
#
# $0 path to SemanticMotifs.txt
# $1 directory where to create .cypher query files
# $2 (optional) path to an OXL file that contains metadata definitions referred by the semantic motif file. If omitted, 
#
mvn exec:java \
  -Dexec.mainClass="uk.ac.rothamsted.knetminer.backend.cypher.genesearch.smtranslator.Motif2CypherCLI" \
  -Dexec.args="$1 $2 $3" \
	-Dexec.classpathScope="test"