mvn exec:java \
  -Dexec.mainClass="uk.ac.rothamsted.knetminer.backend.cypher.genesearch.fftranslator.Motif2CypherCLI" \
  -Dexec.args="$1 $2 $3" \
	-Dexec.classpathScope="test"
