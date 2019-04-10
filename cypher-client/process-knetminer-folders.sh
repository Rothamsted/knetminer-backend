# Processes Knetminer folders, to add Cypher Queries translated from the SemanticMotifs.txt files
#

# Parameters:
species_dir="$1" # $1, the dir to process
out_dir=${2:-$species_dir} # $2, where to save Cypher files, same as $1 if null


# Just a reminder to myself
#species_dir=/Users/brandizi/Documents/Work/RRes/ondex_git/knetminer/species

for i in "$species_dir/"*
do
  [ -d "$i" ] || continue;
  base_name=$(basename $i)
  
  echo -e "\n\n\t$base_name\n"
  
	sm_file="$i/SemanticMotifs.txt"
	if [ ! -e "$sm_file" ]; then
		echo -e "No semantic motif file, skipping"
		continue;
	fi
  
  cy_out="$out_dir/$base_name/neo4j/semantic-motif-queries"
  mkdir --parent "$cy_out"
  rm -f "$cy_out/sm-"*.cypher
  
	./motif2cypher.sh "$sm_file" "$cy_out" src/test/resources/wheat-metadata.xml
	
  # ln -s $out_dir/$n/state-machine.svg $out_dir/$n-sm.svg
done
