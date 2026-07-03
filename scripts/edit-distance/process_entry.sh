#!/bin/bash

SCRIPTS_DIR="$(cd $(dirname "$0") && pwd)"
DATASET=$1
OUTPUT_FILE=$2
OUTPUT_TYPE=$3
CACHE_DIR=$4

function main {
    > $OUTPUT_FILE
    while IFS= read -r line; do
        project=$(echo $line | cut -d '/' -f 1)
        org=$(echo $project | cut -d '_' -f 1)
        repo=$(echo $project | cut -d '_' -f 2-)
        commit=$(echo $line | cut -d ';' -f 2)
        if [[ "$OUTPUT_TYPE" == "link" ]]; then
            link="https://github.com/${org}/${repo}/commit/${commit}"
            echo $link >> $OUTPUT_FILE
        elif [[ "$OUTPUT_TYPE" == "message" ]]; then
            pushd $CACHE_DIR/$project &> /dev/null
                message=$(git log -1 --format=%s $commit)
            popd &> /dev/null
            link="https://github.com/${org}/${repo}/commit/${commit}"
            echo $link:$message >> $OUTPUT_FILE
        fi
    done < $DATASET
}

main
