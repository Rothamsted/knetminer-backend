# The SSL for https://knetminer.rothamsted.ac.uk has expired and this is the only way
# to make Maven behave. TODO: remove after renewal.
#

# The JKS contains the Java defaults and the troubled certificate, so no actual secrets.
# It is a copy of $JAVA_HOME/lib/security/cacerts, to which the SSL of knetminer.rothamsted.ac.uk
# was added. The pwd is the one provided with cacerts by the JDK.
# 
jks_path="$(realpath ci-build/untrusted-certs-temurin-17.jks)"

export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$jks_path -Djavax.net.ssl.trustStorePassword=changeit"

# --- Details:
# The SSL certificate was obtained via:
# $ openssl s_client -connect my.repo.example.com:443 -showcerts </dev/null \
#   | awk '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/' > repo-chain.crt
# 
# And then:
# $ keytool -importcert -file repo-chain.crt \
#     -keystore untrusted-certs-temurin-17.jks \
#     -storepass changeit \
#     -alias knetminer-rres
# 