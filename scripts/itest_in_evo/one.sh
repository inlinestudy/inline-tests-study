#!/bin/bash

SCRIPT_DIR="$(cd $(dirname "$0") && pwd)"
CACHE_DIR="${SCRIPT_DIR}/../../cache"

source ../util.sh

while getopts :p:a:b:f:g:l:m:h:n:d:e:u: opts; do
  case "${opts}" in
    p ) PROJECT="${OPTARG}" ;;
    a ) FIRST_COMMIT="${OPTARG}" ;;
    b ) SECOND_COMMIT="${OPTARG}" ;;
    f ) FIRST_FILE="${OPTARG}" ;;
    g ) SECOND_FILE="${OPTARG}" ;;
    l ) FIRST_LINE="${OPTARG}" ;;
    m ) SECOND_LINE="${OPTARG}" ;;
    h ) HASH="${OPTARG}" ;;
    n ) EXPERIMENT_NAME="${OPTARG}" ;;
    d ) RUN_TESTS="${OPTARG}" ;;
    e ) RUN_EXLI="${OPTARG}" ;;
    u ) USE_MANUAL_ITESTS="${OPTARG}" ;;
  esac
done

ENTRY_DATA_DIR="${SCRIPT_DIR}/../../data/generated-data/${EXPERIMENT_NAME}/${HASH}"
ENTRY_LOG_DIR="${SCRIPT_DIR}/../../logs/generated-logs/${EXPERIMENT_NAME}/${HASH}"

function setup {
  mkdir -p ${ENTRY_DATA_DIR}
  mkdir -p ${ENTRY_LOG_DIR}
  IMAGE="ssb4it"
  if ! docker images | grep -q $(cut -d':' -f2 <<< "${IMAGE}"); then
    echo "Docker image ${IMAGE} not found"
    exit 1
  fi
  CONTAINER_ID=$(docker run -itd --rm ${IMAGE})
  if [[ -z "${CONTAINER_ID}" ]]; then
    echo "Failed to start docker container"
    exit 1
  fi
  (
    docker exec -w /root ${CONTAINER_ID} python3 -m pip install universalmutator --break-system-packages
    docker exec -w /root ${CONTAINER_ID} git clone https://github.com/EngineeringSoftware/inlinetest.git
    docker exec -w /root/inlinetest/java ${CONTAINER_ID} mvn clean install --no-transfer-progress
    docker exec -w /root ${CONTAINER_ID} git clone https://github.com/EngineeringSoftware/exli.git
    docker exec -w /root/exli/java/raninline ${CONTAINER_ID} mvn clean install --no-transfer-progress
    docker cp ${SCRIPT_DIR}/../file_replacement/JacocoExtension.java ${CONTAINER_ID}:/root/exli/jacoco-extension/src/main/java/edu/illinois/extension/JacocoExtension.java # This is to make sure that the JaCoCo version used by ExLi is 0.8.8.
    docker exec -w /root/exli/jacoco-extension ${CONTAINER_ID} mvn clean package --no-transfer-progress
    docker cp ${SCRIPT_DIR}/../../genie ${CONTAINER_ID}:/root/genie
    docker exec -w /root/genie ${CONTAINER_ID} mvn clean install --no-transfer-progress
    docker exec -w /root ${CONTAINER_ID} mkdir -p /usr/share/maven/lib/ext/
    docker exec -w /root ${CONTAINER_ID} cp genie/genie-integration-extension/target/genie-integration-extension-1.0-SNAPSHOT.jar /usr/share/maven/lib/ext/
    docker cp ${SCRIPT_DIR}/../../itest-study-artifact ${CONTAINER_ID}:/root/ssb4it
    local org=$(echo $PROJECT | cut -d '_' -f 1)
    local repo=$(echo $PROJECT | cut -d '_' -f 2-)
    docker exec -w /root ${CONTAINER_ID} git clone "https://github.com/${org}/${repo}.git" ${PROJECT}
  ) &> ${ENTRY_LOG_DIR}/setup.log
}

function cleanup {
  if docker exec ${CONTAINER_ID} test -e /root/${PROJECT}/${PATH_TO_MAVEN}/.genie; then
    docker cp ${CONTAINER_ID}:/root/${PROJECT}/${PATH_TO_MAVEN}/.genie ${ENTRY_DATA_DIR}
  fi
  if docker exec ${CONTAINER_ID} test -e /root/${PROJECT}/${PATH_TO_MAVEN}/.inlinegen; then
    docker cp ${CONTAINER_ID}:/root/${PROJECT}/${PATH_TO_MAVEN}/.inlinegen ${ENTRY_DATA_DIR}
  fi
  if docker exec ${CONTAINER_ID} test -e /root/${PROJECT}/${PATH_TO_MAVEN}/.exli; then
    docker cp ${CONTAINER_ID}:/root/${PROJECT}/${PATH_TO_MAVEN}/.exli ${ENTRY_DATA_DIR}
  fi
  if docker exec ${CONTAINER_ID} test -e /root/${PROJECT}; then
    docker cp ${CONTAINER_ID}:/root/${PROJECT} ${ENTRY_DATA_DIR}/project
  fi
  docker container stop ${CONTAINER_ID}
}

function check_result {
  local result=$1
  local message=$2
  local log_file=$3
  if [[ ${result} -ne 0 ]]; then
    echo ${message} > ${log_file}
    cleanup
    exit 1
  fi
}

function main {
  local project_path="${ENTRY_DATA_DIR}/project"
  # STEP: Clean the repository completely
  execute_command "git_clean" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/git-clean.log
  # STEP: Checkout to the first commit
  execute_command "git_checkout" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/checkout-first.log ${FIRST_COMMIT}
  check_result $? "FIRST_CHECKOUT_FAIL" ${ENTRY_DATA_DIR}/result.txt
  # STEP: Check if the first file is a maven project
  if [[ "${FIRST_FILE}" == *"/src"* ]]; then
    PATH_TO_MAVEN=${FIRST_FILE%"/src"*}
  else
    PATH_TO_MAVEN=""
  fi
  maven_path=${project_path}/${PATH_TO_MAVEN}
  execute_command "check_if_maven" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/check-if-maven.log ${PATH_TO_MAVEN}
  check_result $? "FIRST_NON_MAVEN" ${ENTRY_DATA_DIR}/result.txt
  # STEP: Compile the first commit
  execute_command "maven_compile" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/compile-first.log ${PATH_TO_MAVEN}
  check_result $? "FIRST_FAIL_TO_COMPILE" ${ENTRY_DATA_DIR}/result.txt
  # STEP: Execute tests for the first commit, but don't halt in any case
  if [[ -n "${RUN_TESTS}" ]]; then
    execute_command "mvn_test" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/first-test.log ${PATH_TO_MAVEN}
  fi
  use_manual_itests=false
  if [[ -n "${USE_MANUAL_ITESTS}" ]]; then
    [ -f "${SCRIPT_DIR}/../../data/tests/${HASH}.java" ] && use_manual_itests=true || use_manual_itests=false
  fi
  # If no manually-written inline tests, generate them.
  if [[ ${use_manual_itests} == false ]]; then
    # STEP: Generate inline tests for the first commit
    execute_command "genie_generation" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/generation.log "${PATH_TO_MAVEN};${FIRST_FILE};${FIRST_LINE}"
    local genie_result=$?
    if [[ ${genie_result} -ne 0 ]] && [[ -z "${RUN_EXLI}" ]]; then # If genie failed and we don't run ExLi, just say failed
      check_result ${genie_result} "ITEST_GENERATION_FAIL" ${ENTRY_DATA_DIR}/result.txt
    elif [[ ${genie_result} -ne 0 ]] && [[ -n "${RUN_EXLI}" ]]; then # If genie failed and we run ExLi, try exli
      execute_command "exli_generation" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/exli-generation.log "${PATH_TO_MAVEN};${FIRST_FILE};${FIRST_LINE}"
      check_result $? "ITEST_GENERATION_FAIL" ${ENTRY_DATA_DIR}/result.txt
    fi
    # STEP: Post-compile the first commit
    execute_command "maven_compile" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/post-compile-first.log ${PATH_TO_MAVEN}
    check_result $? "FIRST_POST_COMPILE_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Parse the first commit
    execute_command "parse" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/parse.log "${PATH_TO_MAVEN};${FIRST_FILE}"
    check_result $? "FIRST_PARSE_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Compile the inline tests for the first commit
    local src_name="$(echo ${FIRST_FILE} | rev | cut -d '/' -f 1 | rev)"
    local itest_src_name="$(echo ${src_name} | sed 's/.java//g')_${FIRST_LINE}Test.java"
    execute_command "compile_itests" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/first-compile-itests.log "${PATH_TO_MAVEN};${itest_src_name}"
    check_result $? "FIRST_COMPILE_ITESTS_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Execute the first commit
    local package="$(echo ${FIRST_FILE} | sed 's|src/main/java/||g' | sed "s|/${src_name}||g" | sed 's|/|.|g')"
    execute_command "execute_itests" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/first-execute-itests.log "${PATH_TO_MAVEN};${package}"
    check_result $? "FIRST_EXECUTE_ITESTS_FAIL" ${ENTRY_DATA_DIR}/result.txt
  fi
  # STEP: Clean the source code of the first commit
  execute_command "git_clean_src" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/restore.log ${PATH_TO_MAVEN}
  # STEP: Checkout to the second commit
  execute_command "git_checkout" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-checkout.log ${SECOND_COMMIT}
  check_result $? "SECOND_CHECKOUT_FAIL" ${ENTRY_DATA_DIR}/result.txt
  # STEP: Check if the second file is a maven project
  if [[ "${SECOND_FILE}" == *"/src"* ]]; then
    PATH_TO_MAVEN=${SECOND_FILE%"/src"*}
  else
    PATH_TO_MAVEN=""
  fi
  maven_path=${project_path}/${PATH_TO_MAVEN}
  execute_command "check_if_maven" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/check-if-maven.log ${PATH_TO_MAVEN}
  check_result $? "SECOND_NON_MAVEN" ${ENTRY_DATA_DIR}/result.txt
  # STEP: Compile the second commit
  execute_command "maven_compile" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/compile-second.log ${PATH_TO_MAVEN}
  check_result $? "SECOND_FAIL_TO_COMPILE" ${ENTRY_DATA_DIR}/result.txt
  # STEP: Execute tests for the second commit, but don't halt in any case
  if [[ -n "${RUN_TESTS}" ]]; then
    execute_command "mvn_test" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-test.log ${PATH_TO_MAVEN}
  fi
  if [[ ${use_manual_itests} == false ]]; then
    # STEP: Modify the inline test file
    execute_command "sed_itest" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-sed.log "${PATH_TO_MAVEN};${FIRST_LINE};${SECOND_LINE};${FIRST_FILE};${SECOND_FILE}"
    # STEP: Inject the inline tests into the second commit
    execute_command "inject" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-inject.log "${PATH_TO_MAVEN}"
    check_result $? "SECOND_INJECT_FAIL" ${ENTRY_DATA_DIR}/result.txt
  else
    # TODO: Skip sed for now
    execute_command "write_deps" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-write-deps.log "${PATH_TO_MAVEN}"
    check_result $? "SECOND_WRITE_DEPS_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # Inject manually-written inline tests
    execute_command "inject_manual" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-inject-manual.log "${PATH_TO_MAVEN};${HASH}"
    check_result $? "SECOND_INJECT_MANUAL_FAIL" ${ENTRY_DATA_DIR}/result.txt
  fi
  # STEP: Post-compile the second commit
  execute_command "maven_compile" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/post-compile-second.log ${PATH_TO_MAVEN}
  check_result $? "SECOND_POST_COMPILE_FAIL" ${ENTRY_DATA_DIR}/result.txt
  if [[ ${use_manual_itests} == false ]]; then
  # STEP: Parse the second commit
    execute_command "parse" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-parse.log "${PATH_TO_MAVEN};${SECOND_FILE}"
    check_result $? "SECOND_PARSE_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Compile the inline tests for the second commit
    # Note: For the file name, match with the first commit's line number,
    #  because although the place of injection is updated using sed,
    #  the generated file still uses the first version's line number in its file name.
    local itest_src_name="$(echo ${SECOND_FILE} | rev | cut -d '/' -f 1 | rev | sed 's/.java//g')_${FIRST_LINE}Test.java"
    execute_command "compile_itests" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-compile-itests.log "${PATH_TO_MAVEN};${itest_src_name}"
    check_result $? "SECOND_COMPILE_ITESTS_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Execute the second commit
    execute_command "execute_itests" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-execute-itests.log "${PATH_TO_MAVEN};${package}"
    check_result $? "SECOND_EXECUTE_ITESTS_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Success and cleanup
    echo "SUCCESS" > ${ENTRY_DATA_DIR}/result.txt
    cleanup
  else
    execute_command "parse_manual" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-parse-manual.log "${PATH_TO_MAVEN};${SECOND_FILE}"
    check_result $? "SECOND_PARSE_MANUAL_FAIL" ${ENTRY_DATA_DIR}/result.txt
    local itest_src_name="$(echo ${SECOND_FILE} | rev | cut -d '/' -f 1 | rev | sed 's/.java//g')_${FIRST_LINE}Test.java"
    execute_command "compile_itests_manual" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-compile-itests-manual.log "${PATH_TO_MAVEN};${itest_src_name}"
    check_result $? "SECOND_COMPILE_ITESTS_MANUAL_FAIL" ${ENTRY_DATA_DIR}/result.txt
    execute_command "execute_itests_manual" ${PROJECT} ${CONTAINER_ID} ${ENTRY_LOG_DIR}/second-execute-itests-manual.log "${PATH_TO_MAVEN};${package}"
    check_result $? "SECOND_EXECUTE_ITESTS_MANUAL_FAIL" ${ENTRY_DATA_DIR}/result.txt
    # STEP: Success and cleanup
    echo "SUCCESS_MANUAL" > ${ENTRY_DATA_DIR}/result.txt
    cleanup
  fi

}

setup
main
