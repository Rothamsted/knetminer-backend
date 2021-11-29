# Maintenance script to backup configuration files.
# Data files don't quite need backup, cause they can been rebuilt from   

set -e
bkp_name=`hostname -a`
bkp_dir="bkp-$bkp_name"

cd /tmp
mkdir --parents "$bkp_dir"
cd "$bkp_dir"

tar cv --bzip2 -f etc.tar.bz2 /etc
tar cv --bzip2 -f software.tar.bz2 \
  --exclude '/opt/software/neo4j-*/data' \
  /opt/software
tar cv --bzip2 -f root.tar.bz2 --exclude /root/tmp --exclude /root/.cache /root

if [[ "$bkp_name" == '3store' ]]; then
  tar cv --bzip2 -f data.tar.bz2 /data/virtuoso/db/virtuoso.ini
fi

dpkg --get-selections >packages.lst

cd ..
rclone copy --progress "$bkp_dir" "rres_onedrive:bkp/cyverse-$bkp_name"
rm -Rf "$bkp_dir"
