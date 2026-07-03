#!/bin/bash

SKIPS="-Dcheckstyle.skip -Drat.skip -Denforcer.skip -Danimal.sniffer.skip -Dmaven.javadoc.skip -Dfindbugs.skip -Dwarbucks.skip -Dmodernizer.skip -Dimpsort.skip -Dpmd.skip -Dxjc.skip -Djacoco.skip -Dinvoker.skip -DskipDocs -DskipITs -Dbuildhelper.uptodateproperty.skip -Dbuildhelper.uptodateproperties.skip"
SKIPS_NO_JACOCO="-Dcheckstyle.skip -Drat.skip -Denforcer.skip -Danimal.sniffer.skip -Dmaven.javadoc.skip -Dfindbugs.skip -Dwarbucks.skip -Dmodernizer.skip -Dimpsort.skip -Dpmd.skip -Dxjc.skip -Dinvoker.skip -DskipDocs -DskipITs -Dbuildhelper.uptodateproperty.skip -Dbuildhelper.uptodateproperties.skip"

function execute_command {
  local command=$1
  local project=$2
  local container_id=$3
  local log_file=$4
  local aux_arg=${5:-""}
  local delimiter=";"
  if [[ ${command} == "git_clean" ]]; then
    (docker exec -w /root/${project} ${container_id} git clean . -xfd) &> ${log_file}
  elif [[ ${command} == "git_checkout" ]]; then
    (docker exec -w /root/${project} ${container_id} git checkout -f ${aux_arg}) &> ${log_file}
  elif [[ ${command} == "check_if_maven" ]]; then
    (docker exec -w /root/${project}/${aux_arg} ${container_id} test -f "pom.xml") &> ${log_file}
  elif [[ ${command} == "maven_compile" ]]; then
    (docker exec -w /root/${project}/${aux_arg} ${container_id} mvn clean test-compile --no-transfer-progress ${SKIPS}) &> ${log_file}
  elif [[ ${command} == "genie_generation" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local file_path=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    local line_numbers=$(echo ${aux_arg} | cut -d ${delimiter} -f 3)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} mvn clean genie:genie -DfilePath="${file_path}" -Dtool="evosuite" -DlineNumbers="${line_numbers}" -DevosuiteCustomTimeout=360) &> ${log_file}
  elif [[ ${command} == "parse" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local file_path=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/.genie/deps.txt)" org.inlinetest.InlineTestRunnerSourceCode --input_file="${file_path}" --assertion_style=junit --output_dir=/root/${project}/${path_to_maven}/.genie/parsed-itests/src/r2 --multiple_test_classes=true --dep_file_path=/root/${project}/${path_to_maven}/.genie/deps.txt --app_src_path=src/main/java) &> ${log_file}
  elif [[ ${command} == "parse_manual" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local file_path=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} mkdir -p parsed-itests/src/r2) &> ${log_file}
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/deps.txt)" org.inlinetest.InlineTestRunnerSourceCode --input_file="${file_path}" --assertion_style=junit --output_dir=/root/${project}/${path_to_maven}/parsed-itests/src/r2 --multiple_test_classes=true --dep_file_path=/root/${project}/${path_to_maven}/deps.txt --app_src_path=src/main/java) &> ${log_file}
  elif [[ ${command} == "compile_itests" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local itest_src_name=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} javac -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/.genie/deps.txt)":/root/${project}/${path_to_maven}/.genie/parsed-itests/src/r2 -d /root/${project}/${path_to_maven}/.genie/parsed-itests/bin/r2 /root/${project}/${path_to_maven}/.genie/parsed-itests/src/r2/${itest_src_name}) &> ${log_file}
  elif [[ ${command} == "compile_itests_manual" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local itest_src_name=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} mkdir -p parsed-itests/bin/r2) &> ${log_file}
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} javac -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/deps.txt)":/root/${project}/${path_to_maven}/parsed-itests/src/r2 -d /root/${project}/${path_to_maven}/parsed-itests/bin/r2 /root/${project}/${path_to_maven}/parsed-itests/src/r2/${itest_src_name}) &> ${log_file}
  elif [[ ${command} == "execute_itests" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local package=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -jar /root/${project}/${path_to_maven}/.genie/jars/junit-platform-console-standalone-1.12.0.jar -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/.genie/deps.txt | sed 's|target/test-classes||g')":/root/${project}/${path_to_maven}/.genie/parsed-itests/bin/r2 --select-package "${package}") &> ${log_file}
  elif [[ ${command} == "execute_itests_manual" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local package=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -jar /root/genie/genie-maven-plugin/src/main/resources/jars/junit-platform-console-standalone-1.12.0.jar -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/deps.txt | sed 's|target/test-classes||g')":/root/${project}/${path_to_maven}/parsed-itests/bin/r2 --select-package "${package}") &> ${log_file}
  elif [[ ${command} == "git_clean_src" ]]; then
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} git clean -f src && git restore -f src) &> ${log_file}
  elif [[ ${command} == "sed_itest" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local first_line=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    local second_line=$(echo ${aux_arg} | cut -d ${delimiter} -f 3)
    local first_file=$(echo ${aux_arg} | cut -d ${delimiter} -f 4)
    local second_file=$(echo ${aux_arg} | cut -d ${delimiter} -f 5)
    docker exec -w /root/${project}/${path_to_maven} ${container_id} cp /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests.txt /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests-first.txt
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} sed -i "s|${first_file};|${second_file};|g" /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests.txt) &> "${log_file}"
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} sed -i "s/;${first_line};/;${second_line};/g" /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests.txt) &>> "${log_file}"
    docker exec -w /root/${project}/${path_to_maven} ${container_id} cp /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests.txt /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests-second.txt
  elif [[ ${command} == "inject" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/.genie/deps.txt)" org.raninline.App a /root/${project}/${path_to_maven}/.genie/r2-evosuite-inlinetests.txt) &> ${log_file}
  elif [[ ${command} == "inject_manual" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local hash=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -cp "$(docker exec ${container_id} cat /root/${project}/${path_to_maven}/deps.txt)" org.raninline.App a /root/ssb4it/data/tests/${hash}.java) &> ${log_file}
  elif [[ ${command} == "write_deps" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} mvn dependency:build-classpath -Dmdep.outputFile=tmp_deps.txt) &> ${log_file}
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} bash -c "echo \"\$(cat tmp_deps.txt):/root/exli/java/raninline/target/raninline-1.0-SNAPSHOT.jar:/root/inlinetest/java/target/inlinetest-1.0.jar:\$(pwd)/target/classes\" > deps.txt") &>> ${log_file}
  elif [[ ${command} == "mvn_test" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} timeout 3600 mvn test ${SKIPS}) &> ${log_file}
    if [[ $? -eq 124 ]]; then
      echo "TIMEOUT_3600" >> ${log_file}
    fi
  elif [[ ${command} == "exli_generation" ]]; then
    local path_to_maven=$(echo ${aux_arg} | cut -d ${delimiter} -f 1)
    local file_path=$(echo ${aux_arg} | cut -d ${delimiter} -f 2)
    local line_numbers=$(echo ${aux_arg} | cut -d ${delimiter} -f 3)
    # Remove .inlinegen directory if it exists
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} rm -rf .inlinegen) &> ${log_file}
    # Clean up and restore source code
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} git clean src -f && git restore src) &>> ${log_file}
    # Instrument the source code
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} java -jar /root/exli/java/raninline/target/raninline-1.0-SNAPSHOT.jar instrument ${file_path} ${line_numbers} .exli/log.txt .exli/inlinetest-r0.txt .exli/inlinetest-r1.txt target/classes) &>> ${log_file}
    # Execute tests
    (docker exec -w /root/${project}/${path_to_maven} ${container_id} timeout 3600 mvn clean test ${SKIPS_NO_JACOCO} -Dmaven.ext.class.path=/root/exli/jacoco-extension/target/jacoco-extension-1.0-SNAPSHOT.jar) &>> ${log_file}
    # Check if there are any generated inline tests
    if [[ !$(docker exec -w /root/${project}/${path_to_maven} ${container_id} test -d .exli) ]]; then
      return 1
    fi
    if [[ -n $(docker exec -w /root/${project}/${path_to_maven} ${container_id} ls .exli) ]]; then
      # If ExLi generated something, rename that to r2-evosuite-inlinetests.txt
      # TODO: Currently this is treating ExLi's R1 as Genie's R2. Maybe not the best way, but is easy to integrate.
      (docker exec -w /root/${project}/${path_to_maven} ${container_id} mv .exli/inlinetest-r1.txt .genie/r2-evosuite-inlinetests.txt) &>> ${log_file}
      return 0
    else
      return 1
    fi
  fi
}
