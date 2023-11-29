TARGETMETHOD=$1
source environ.sh

echo "Running java -Xms800m -Xmx3g Analysis target1-pub  PubTest  PubTest  "$TARGETMETHOD" test"
time \
    java -Xms800m -Xmx3g Analysis target1-pub  PubTest  PubTest  "$TARGETMETHOD" test

#!/bin/bash

# Define the source folders
source_folder1="target1-pub"
source_folder2="expected-output"

GREEN='\033[0;32m'
RED='\033[0;31m'
RESET='\033[0m'

file_name="PubTest.$TARGETMETHOD.output.txt"
# echo "comparing actual-output/$file_name with expected-output/$file_name"
if [ -e "$source_folder2/$file_name" ]; then
    difference=$(diff "$source_folder1/$file_name" "$source_folder2/$file_name")
    if [ -z "$difference" ]; then

        echo "${GREEN}Test $TARGETMETHOD passed${RESET}"
    else
        echo "${RED}Test $TARGETMETHOD failed"
        echo "$difference${RESET}"
    fi
else
    echo "File does not exist in $source_folder2"
fi

