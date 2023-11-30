TARGETMETHOD=${1:-default3}
DIRECTORY=${2:-target1-pub}
CLASS=${3:-PubTest}

source environ.sh

echo "Running java -Xms800m -Xmx3g Analysis $DIRECTORY  $CLASS  $CLASS  "$TARGETMETHOD" test"
time \
    mkdir -p iterations
  find iterations ! -name '*.pdf' -type f -exec rm -f {} +
   find $DIRECTORY ! -name '*.java' '*.class' -type f -exec rm -f {} +
    java -Xms800m -Xmx3g Analysis  $DIRECTORY  $CLASS  $CLASS "$TARGETMETHOD" test
time \
    ./run-generate-pdf.sh $CLASS $TARGETMETHOD 
    find iterations ! -name '*.pdf' -type f -exec rm -f {} +

#!/bin/bash

# Define the source folders
source_folder1=$DIRECTORY
source_folder2="expected-output"

GREEN='\033[0;32m'
RED='\033[0;31m'
RESET='\033[0m'

file_name="$CLASS.$TARGETMETHOD.output.txt"
# echo "comparing actual-output/$file_name with expected-output/$file_name"
if [ -e "$source_folder2/$file_name" ]; then
    difference=$(diff --suppress-common-lines --side-by-side "$source_folder1/$file_name" "$source_folder2/$file_name")
    if [ -z "$difference" ]; then

        echo "${GREEN}Test $TARGETMETHOD passed${RESET}"
    else
        echo "${RED}Test $TARGETMETHOD failed"
        echo "$difference${RESET}"
    fi
else
    echo "File does not exist in $source_folder2"
fi

