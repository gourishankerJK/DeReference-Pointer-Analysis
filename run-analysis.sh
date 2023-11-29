#!/usr/bin/env bash

set -e

## ./run-analysis-one.sh  <Dir>  <MainClass>  <TargetClass>  <TargetMethod>


#./run-analysis-one.sh "./target1-pub" "AddNumbers"  "AddNumbers"  "main"
#./run-analysis-one.sh "./target1-pub" "AddNumFun"   "AddNumFun"   "expr"


# XXX you can add / delete / comment / uncomment lines below
spinner() {
    local  spinners="/-\|"
    local  delay=0.1  # Adjust the delay value to control the speed of the spinner

    while :; do
        printf "\r%c" "${spinners}"
        sleep "$delay"
        spinners=${spinners#?}${spinners%???}
    done
}

# Usage example:
cleanup() {
    if [[ -n "$spinner_pid" ]]; then
        kill "$spinner_pid"
    fi
}

# Register the cleanup function to be called on script exit or error
trap cleanup EXIT

spinner & spinner_pid=$! 
 sleep 0.5
mkdir -p iterations
rm -rf iterations/*
./run-analysis-one.sh "./target2-mine" "BasicTest"   "BasicTest"   "f"

find . -name "*.dot" -print0 | while IFS="" read -r -d "" file; do
    dot -Tsvg -Gsize="24,24" "$file" -o "$file.svg"
done

printf "%s\n" iterations/*.svg | sort -t'_' -k2 -n | xargs  rsvg-convert -f pdf -o iterations/FullOutput.pdf
printf "\rFiles Generated.......!\n"
kill "$spinner_pid"



