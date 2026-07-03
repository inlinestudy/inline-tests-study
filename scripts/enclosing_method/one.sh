#!/bin/bash

source ../util.sh

SCRIPT_DIR=$(cd $(dirname $0) && pwd)
DATA_GENERATED_DATA_DIR=$SCRIPT_DIR/../../data/generated-data
COMMAND_FILE=$SCRIPT_DIR/tmp_commands.txt
CACHE_DIR="${SCRIPT_DIR}/../../cache"

URL=$1
COMMIT=$2
FILE_PATH=$3
LINE_NUMBER=$4
HASH=$5
EXP_NAME=$6

ENTRY_DATA_DIR="${SCRIPT_DIR}/../../data/generated-data/${EXP_NAME}/${HASH}"
ENTRY_LOG_DIR="${SCRIPT_DIR}/../../logs/generated-logs/${EXP_NAME}/${HASH}"

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
  docker container stop ${CONTAINER_ID}
}

setup
docker exec -w /root/ssb4it/scripts/coverage-checker-enclosing-method ${CONTAINER_ID} mvn clean package
docker exec -w /root/ssb4it/scripts/coverage-checker-enclosing-method ${CONTAINER_ID} java -jar target/coverage-checker-1.0-SNAPSHOT.jar ${URL} ${COMMIT} ${FILE_PATH} ${LINE_NUMBER} ${HASH} ../../local_dependencies
docker cp ${CONTAINER_ID}:/root/ssb4it/scripts/coverage-checker-enclosing-method/${HASH} ${ENTRY_DATA_DIR}
cleanup
