cd "$(dirname $0)"
mvn --projects test-data-server -P server-mode pre-integration-test
