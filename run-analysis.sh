#!/usr/bin/env bash

set -e

## ./run-analysis-one.sh  <Dir>  <MainClass>  <TargetClass>  <TargetMethod>


#./run-analysis-one.sh "./target1-pub" "AddNumbers"  "AddNumbers"  "main"
#./run-analysis-one.sh "./target1-pub" "AddNumFun"   "AddNumFun"   "expr"


# XXX you can add / delete / comment / uncomment lines below
mkdir -p iterations
./run-analysis-one.sh "./target1-pub" "PubTest"   "PubTest"   "test1"
dot -Tpng callgraph.dot -o callgraph.png

find . -name "*.dot" -print0 | while IFS="" read -r -d "" file; do
    echo "Processing: $file"
    dot -Tpng "$file" -o "$file.png"
done