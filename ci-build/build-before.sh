# The SSL for https://knetminer.rothamsted.ac.uk has expired and this is the only way
# to make Maven behave. TODO: remove after renewal
#
export MAVEN_BUILD_ARGS="$MAVEN_BUILD_ARGS -Djavax.net.ssl.trustStore=ci-build/untrusted-certs.jks"
# The JKS contains the Java defaults and the troubled certificate, so no actual secrets.
export MAVEN_BUILD_ARGS="$MAVEN_BUILD_ARGS -Djavax.net.ssl.trustStorePassword=not-a-problem"
