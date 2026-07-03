#!/bin/bash

SCRIPT_DIR="$(cd $(dirname "$0") && pwd)"
CACHE_DIR="${SCRIPT_DIR}/../../cache"

DATA_SET=${1:-${SCRIPT_DIR}/../../data/ONE_TO_ONE.txt}
COMMANDS_FILE=$(realpath $2)
THREADS=${3:-10}
EXPERIMENT_NAME=${4:-"itest_in_evo"}
RUN_EXLI=${5:-""} # If empty, don't run ExLi.
USE_MANUAL_ITESTS=${6:-""} # If empty, don't use manual inline tests.

DATA_DIR="${SCRIPT_DIR}/../../data/generated-data/${EXPERIMENT_NAME}"

function construct_command {
  local line1=$1
  local line2=$2
  local project=$(echo $line1 | cut -d '/' -f 1)
  local hash=$(echo -e "${line1}\n${line2}" | md5sum | cut -d ' ' -f 1)
  local first_commit=$(echo $line1 | cut -d ';' -f 1 | cut -d ':' -f 2)
  local second_commit=$(echo $line2 | cut -d ';' -f 2)
  local first_file=$(echo $line1 | cut -d ';' -f 5 | sed "s|${project}/||")
  local second_file=$(echo $line1 | cut -d ';' -f 5 | sed "s|${project}/||")
  local first_line=$(echo $line1 | cut -d ';' -f 6)
  local second_line=$(echo $line2 | cut -d ';' -f 6)
  local command="bash $SCRIPT_DIR/one.sh -p $project -a $first_commit -b $second_commit -f $first_file -g $second_file -l $first_line -m $second_line -h $hash -n $EXPERIMENT_NAME"
  if [[ -n "${RUN_EXLI}" ]]; then
    command+=" -e ${RUN_EXLI}"
  fi
  if [[ -n "${USE_MANUAL_ITESTS}" ]]; then
    command+=" -u ${USE_MANUAL_ITESTS}"
  fi
  mkdir -p "${DATA_DIR}/${hash}"
  echo "$line1" > "${DATA_DIR}/${hash}/pair.txt"
  echo "$line2" >> "${DATA_DIR}/${hash}/pair.txt"
  echo $command >> $COMMANDS_FILE
}

function main {
  mkdir -p $DATA_DIR
  >$COMMANDS_FILE
  if [[ "${EXPERIMENT_NAME}" == "itest_in_evo"* ]]; then
    echo "Constructing commands"
    while IFS= read -r line1 && IFS= read -r line2; do
      construct_command "$line1" "$line2"
    done < $DATA_SET
  elif [[ "${EXPERIMENT_NAME}" == "generate_and_run_inline_tests"* ]]; then
    python3 construct_command.py ${DATA_SET} ${COMMANDS_FILE} ${EXPERIMENT_NAME} "${RUN_EXLI}" "${USE_MANUAL_ITESTS}"
  fi
  parallel --progress --bar --jobs "${THREADS}" < $COMMANDS_FILE
}

main
