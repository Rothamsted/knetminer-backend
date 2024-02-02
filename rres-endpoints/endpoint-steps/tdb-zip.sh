# Endpoint building step to prepare the zipped TDB triple store and place it in the pipeline's output
# directory, ready for the dataset's downloadings.
#

set -e

tdb="$1"
tdb_zip="$2"

echo -e "\n\tCompressing '$tdb' to '$tdb_zip' \n"
rm -f "$tdb_zip"
cd "$tdb/.."
tar cv --bzip2 -f "$tdb_zip" "$tdb"
