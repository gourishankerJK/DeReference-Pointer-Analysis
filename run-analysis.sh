#!/usr/bin/env bash

set -e

## ./run-analysis-one.sh  <Dir>  <MainClass>  <TargetClass>  <TargetMethod>


#./run-analysis-one.sh "./target1-pub" "AddNumbers"  "AddNumbers"  "main"
#./run-analysis-one.sh "./target1-pub" "AddNumFun"   "AddNumFun"   "expr"


# XXX you can add / delete / comment / uncomment lines below
./run-analysis-one.sh "./target2-mine" "BasicTest"   "BasicTest"   "fun7_public"

    javac   "AutoMatedTester.java"
time \
    java -Xms800m -Xmx3g "AutoMatedTester" "./target2-mine/Public_Test_fun4.txt" "./Result_fun4_public.txt"