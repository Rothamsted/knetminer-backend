# Processes Knetminer folders, to add Cypher Queries translated from the SemanticMotifs.txt files
#

# Parameters:
species_dir="$1" # $1, the dir to process
out_dir=${2:-$species_dir} # $2, where to save Cypher files, same as $1 if null

# Just a reminder to myself
#species_dir=/Users/brandizi/Documents/Work/RRes/ondex_git/knetminer/species

for i in "$species_dir/"* "$species_dir/fungi/"*
do
  [ -d "$i" ] || continue;
  base_name=$(basename $i)
  
  echo -e "\n\n\t$base_name\n"
  
	sm_file="$i/ws/SemanticMotifs.txt"
	if [ ! -e "$sm_file" ]; then
		echo -e "No semantic motif file, skipping"
		continue;
	fi

  # Consider the species in sub-directories
  out_base=$(echo "$i" | sed "s|^$species_dir/||")
  cy_out="$out_dir/$out_base/ws/neo4j/state-machine-queries"
    
  mkdir -p "$cy_out"
  rm -f "$cy_out/sm-"*.cypher
  
	./motif2cypher.sh "$sm_file" "$cy_out" src/test/resources/wheat-metadata.xml
	
  # ln -s $out_dir/$n/state-machine.svg $out_dir/$n-sm.svg
done
