source ../util.sh

SCRIPT_DIR=$(cd $(dirname $0) && pwd)
DATA_GENERATED_DATA_DIR=$SCRIPT_DIR/../../data/generated-data
LOG_GENERATED_DATA_DIR=$SCRIPT_DIR/../../logs/generated-logs
COMMAND_FILE=$SCRIPT_DIR/tmp_commands.txt
CACHE_DIR="${SCRIPT_DIR}/../../cache"

DATASET=$1 # It is CRUCIAL that the dataset should match experiment name, see below.
THREADS=${2:-40}
EXP_NAME=$3 # Only choose from these two values: enclosing_method_cov_bug, enclosing_method_cov_evo
LOCAL=${4:-""}

# For local setup
function setup {
    pushd $SCRIPT_DIR/../coverage-checker-enclosing-method
        mvn clean package
    popd
    mkdir -p "${DATA_GENERATED_DATA_DIR}/${EXP_NAME}"
    mkdir -p "${LOG_GENERATED_DATA_DIR}/${EXP_NAME}"
}

function main {
  > $COMMAND_FILE
  if [[ "${EXP_NAME}" == "enclosing_method_cov_bug" ]]; then
    python3 construct_command.py $DATASET $COMMAND_FILE $EXP_NAME $LOCAL
  elif [[ "${EXP_NAME}" == "enclosing_method_cov_evo" ]]; then
    exec 3< "$DATASET"
    while true; do
      read -r line1 <&3 || break
      read -r line2 <&3 || break
      construct_command "$line1" "$line2"
    done
    exec 3<&-
  fi
}

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
  # Do this after getting first_file and second_file, otherwise it will break.
  project=$(echo "$project" | sed 's/_/\//')
  if [[ -n "${LOCAL}" ]]; then
    local command="java -jar ${SCRIPT_DIR}/../coverage-checker-enclosing-method/target/coverage-checker-1.0-SNAPSHOT.jar https://github.com/$project $first_commit $first_file $first_line $DATA_GENERATED_DATA_DIR/$EXP_NAME/$hash ../../local_dependencies ../../cache &> ${LOG_GENERATED_DATA_DIR}/${EXP_NAME}/${hash}.log"
  fi
  if [[ -z "${LOCAL}" ]]; then
    local command="bash one.sh https://github.com/$project $first_commit $first_file $first_line $hash ../../local_dependencies"
  fi
  echo $command >> $COMMAND_FILE
}

function run {
    parallel --progress --bar --jobs $THREADS < $COMMAND_FILE
}

setup
main
run
