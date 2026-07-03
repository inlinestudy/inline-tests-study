#!/bin/bash

SCRIPTS_DIR="$(cd $(dirname "$0") && pwd)"
DATASET=$1
CACHE_DIR=$2
OUTPUT_FILE=$3

function setup {
    pushd ${SCRIPTS_DIR}/edit-distance
        mvn clean package
    popd
}

function main {
    > $OUTPUT_FILE
    exec 3< "$DATASET"
    while true; do
        read -r line1 <&3 || break
        read -r line2 <&3 || break
        result=$(java -jar ${SCRIPTS_DIR}/edit-distance/target/edit-distance-1.0-SNAPSHOT.jar "${CACHE_DIR}" "${line1}" "${line2}" | tail -n 1 | cut -d ':' -f 2 | xargs)
        echo "${line1};${result}" >> $OUTPUT_FILE
        echo "${line2};${result}" >> $OUTPUT_FILE
    done
    exec 3<&-
}

setup
main
