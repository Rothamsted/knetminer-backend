host="$1"

if [[ -z "$host" ]]; then
  echo -e "\n\n$0 <knetminer-host>\n"
  exit 1
fi

