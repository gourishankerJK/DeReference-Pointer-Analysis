#!/usr/bin/env bash

set -e

## ./run-analysis-one.sh  <Dir>  <MainClass>  <TargetClass>  <TargetMethod>


#./run-analysis-one.sh "./target1-pub" "AddNumbers"  "AddNumbers"  "main"
#./run-analysis-one.sh "./target1-pub" "AddNumFun"   "AddNumFun"   "expr"


# XXX you can add / delete / comment / uncomment lines below
mkdir -p iterations
./run-analysis-one.sh "./target1-pub" "PubTest"   "PubTest"   "test1"
dot -Tsvg callgraph.dot -o callgraph.svg

find . -name "*.dot" -print0 | while IFS="" read -r -d "" file; do
    echo "Processing: $file"
    dot -Tsvg "$file" -o "$file.svg"
done

rm -rf iterations/*.dot

rm iterations.pdf
rsvg-convert -f pdf -o iterations.pdf iterations/*.svg
