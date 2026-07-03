#!/bin/bash

DATASET=$1
THREADS=${2:-10}

total=$(grep "\"full_name\"" $DATASET | cut -d '"' -f 4 | wc -l | xargs)
i=1
command_file=/tmp/mine-ts-changes-commands.txt
>$command_file
# for project in $(grep "\"full_name\"" $DATASET | cut -d '"' -f 4); do
for project in $(cat $DATASET); do
    # echo "[$i/$total] Processing $project"
    echo bash find_changed_ts.sh $project &>> $command_file
    # i=$((i+1))
done
parallel --progress --bar --jobs "${THREADS}" < $command_file
