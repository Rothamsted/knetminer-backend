# Endpoint building step to prepare the zipped TDB triple store and place it in the pipeline's output
# directory, ready for the dataset's downloadings.
#

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