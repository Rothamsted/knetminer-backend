set -e
cd "$KNET_SCRIPTS_HOME"
. config/init-dataset-cfg.sh

rdf_target="$KNET_DATASET_TARGET/rdf"
tdb="$KNET_DATASET_TARGET/tmp/tdb"
tdb_zip="$rdf_target/tdb.tar.bz2"

echo -e "\n\tCompressing '$tdb' to '$tdb_zip' \n"
rm -f "$tdb_zip"
cd "$tdb/.."
tar cv --bzip2 -f "$tdb_zip" tdb
