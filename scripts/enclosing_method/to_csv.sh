#!/bin/bash
#
# Based on the experiment data (under enclosing_method_cov), construct a CSV file.
# Sample usage: bash to_csv.sh

SCRIPT_DIR="$(cd $(dirname "$0") && pwd)"
DATA_GENERATED_DATA_DIR=$SCRIPT_DIR/../../data/generated-data
LOG_GENERATED_DATA_DIR=$SCRIPT_DIR/../../logs/generated-logs
EXP_NAME="enclosing_method_cov"
OUTPUT="$SCRIPT_DIR/../../data/enclosing_method_cov.csv"

function main {
    > $OUTPUT
    echo "hash,method_cov,stmt_cov" >> $OUTPUT
    for hash in $(ls -d $DATA_GENERATED_DATA_DIR/$EXP_NAME/*); do
        if [[ ! -f $hash/method-result.txt || ! -f $hash/stmt-result.txt ]]; then
            continue
        fi
        method_cov=$(cat $hash/method-result.txt)
        stmt_cov=$(cat $hash/stmt-result.txt)
        hash=$(basename $hash)
        echo "$hash,$method_cov,$stmt_cov" >> $OUTPUT
    done
}

main
