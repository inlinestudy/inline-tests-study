#!/bin/bash
#
# Construct a TSV based on a 1:1 evolution dataset.
# Note: You need to use the version of dataset that:
# 1. Already has the edit distance calculated.
# 2. Has the commit message.
# Sample input:
# line 1: Omertron_api-fanarttv/changed-ts.txt:7f3e891cb83b8358d30f36b530052da0d9101daf;eeb631d4c78b0f0f4844bdf6b085b4441889b0ca;7f3e891cb83b8358d30f36b530052da0d9101daf;target stmt string;Omertron_api-fanarttv/fanarttvapi/src/main/java/com/moviejukebox/fanarttv/tools/Base64.java;28;null;;null;ONE_TO_ONE;1
# line 2: Omertron_api-fanarttv/changed-ts.txt:7f3e891cb83b8358d30f36b530052da0d9101daf;eeb631d4c78b0f0f4844bdf6b085b4441889b0ca;eeb631d4c78b0f0f4844bdf6b085b4441889b0ca;target stmt string;Omertron_api-fanarttv/fanarttvapi/src/main/java/com/moviejukebox/fanarttv/tools/Base64.java;33;null;;null;ONE_TO_ONE;1
# Sample usage: bash construct_csv.sh ../../data/ONE_TO_ONE_DEFAULT_BRANCH-ed-1.txt tmp.csv ../../cache 1 itest_in_evo-ed-1

SCRIPT_DIR="$(cd $(dirname "$0") && pwd)"
DELIM=$'\t'

DATASET=$1
OUTPUT_FILE=$2
CACHE_DIR=$3
ID_BEGIN=$4
EXP_NAME=$5 # In case evolution experiment is run for this dataset, this is the name of the experiment, e.g. itest_in_evo-ed-1.

function main {
    > $OUTPUT_FILE
    echo "id${DELIM}project${DELIM}v0${DELIM}v1${DELIM}oldPath${DELIM}newPath${DELIM}oldLine${DELIM}newLine${DELIM}v0Src${DELIM}v1Src${DELIM}url${DELIM}commit_message${DELIM}str_edit_distance${DELIM}custom_hash${DELIM}it_actual" >> $OUTPUT_FILE
    exec 3< "$DATASET"
    id=${ID_BEGIN}
    while true; do
        read -r line1 <&3 || break
        read -r line2 <&3 || break
        project=$(echo $line1 | cut -d '/' -f 1)
        org=$(echo $project | cut -d '_' -f 1)
        repo=$(echo $project | cut -d '_' -f 2-)
        v0=$(echo $line1 | cut -d ';' -f 3)
        v1=$(echo $line2 | cut -d ';' -f 3)
        oldPath=$(echo $line1 | cut -d ';' -f 5 | sed "s|${project}/||")
        newPath=$(echo $line2 | cut -d ';' -f 5 | sed "s|${project}/||")
        oldLine=$(echo $line1 | cut -d ';' -f 6)
        newLine=$(echo $line2 | cut -d ';' -f 6)
        url="https://github.com/${org}/${repo}/commit/${v1}#diff-${v0}L${oldLine}"
        str_edit_distance=$(echo $line1 | cut -d ';' -f 11)
        hash=$(echo -e "${line1}\n${line2}" | md5sum | cut -d ' ' -f 1)
        if [[ -n "${EXP_NAME}" ]]; then
            it_actual=$(cat $SCRIPT_DIR/../../data/generated-data/$EXP_NAME/${hash}/result.txt)
        else
            it_actual=""
        fi
        pushd $CACHE_DIR/$project &> /dev/null
            git checkout -f $v0 &> /dev/null
            v0Src=$(awk "NR == ${oldLine}" $oldPath | sed "s|${DELIM}||g" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            git checkout -f $v1 &> /dev/null
            v1Src=$(awk "NR == ${newLine}" $newPath | sed "s|${DELIM}||g" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
            commit_message=$(git log -1 --format=%s $v1 | sed "s|${DELIM}||g" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        popd &> /dev/null
        echo "${id}${DELIM}${project}${DELIM}${v0}${DELIM}${v1}${DELIM}${oldPath}${DELIM}${newPath}${DELIM}${oldLine}${DELIM}${newLine}${DELIM}${v0Src}${DELIM}${v1Src}${DELIM}${url}${DELIM}${commit_message}${DELIM}${str_edit_distance}${DELIM}${hash}${DELIM}${it_actual}" >> $OUTPUT_FILE
        id=$((id+1))
    done
    exec 3<&-
}

main
