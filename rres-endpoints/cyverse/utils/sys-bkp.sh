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
tar cv --bzip2 -f root.tar.bz2 --exclude /root/tmp /root

dpkg --get-selections >packages.lst

cd ..
rclone copy --progress "$bkp_dir" "rres_onedrive:bkp/cyverse-$bkp_name"
rm -Rf "$bkp_dir"

 