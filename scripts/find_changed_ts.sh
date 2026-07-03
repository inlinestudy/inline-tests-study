#!/bin/bash


SCRIPT_DIR=$(cd $(dirname $0) && pwd)
DATA_DIR=$SCRIPT_DIR/../data
CACHE_DIR=$SCRIPT_DIR/../cache

while getopts :c:d: opts; do
  case "${opts}" in
    c ) CACHE_DIR="${OPTARG}" ;;
    d ) DATA_DIR="${OPTARG}" ;;
  esac
done
shift $((${OPTIND} - 1))

OUTPUT_DIR=$DATA_DIR/generated-data/mine-ts-changes
mkdir -p $OUTPUT_DIR

PROJECT_NAME=$1
ORGANIZATION=$(echo ${PROJECT_NAME} | cut -d '_' -f 1)
REPO_NAME=$(echo ${PROJECT_NAME} | cut -d '_' -f 2-)
RANINLINE_JAR=${CACHE_DIR}/exli/java/raninline/target/raninline-1.0-SNAPSHOT.jar
mkdir -p ${DATA_DIR}/revisions/
mkdir -p ${OUTPUT_DIR}/${PROJECT_NAME}

: '
Logic of the script:
1. For each v0, v1 pair in the revision list, do the following:
	1. Call get_git_diff using v0 and v1, save the result somewhere (in the form of file, line number pair).
	2. TODO: Automatically collect changed files
	3. Checkout to v0 and run tsf on the changed files.
	4. Record if ts file and line numbers pair is in one of the "-" lines
		1. If all the ts do not have line numbers in the "-" lines, skip
	5. Checkout to v1 and run tsf on the changed files.
    6. Record if ts file and line numbers pair is in one of the "+" lines
        1. If all the ts do not have line numbers in the "+" lines, skip
    7. For a particular ts, if it is in both the "-" and "+" lines, then it is a changed ts.
'

# Credit: https://stackoverflow.com/questions/24455377/git-diff-with-line-numbers-git-log-with-line-numbers/61997003#61997003
function get_git_diff {
    pushd ${CACHE_DIR}/${PROJECT_NAME} &> /dev/null
    local v0=$1
    local v1=$2
    local output=$3
    git diff $v0 $v1 | gawk '
    match($0,"^@@ -([0-9]+),([0-9]+) [+]([0-9]+),([0-9]+) @@",a){
        left=a[1]
        ll=length(a[2])
        right=a[3]
        rl=length(a[4])
    }
    /^(---|\+\+\+|[^-+ ])/{ print;next }
    { line=substr($0,2) }
    /^[-]/{ printf "-%"ll"s %"rl"s:%s\n",left++,""     ,line;next }
    /^[+]/{ printf "+%"ll"s %"rl"s:%s\n",""    ,right++,line;next }
            { printf " %"ll"s %"rl"s:%s\n",left++,right++,line }
    ' > $output
    popd &> /dev/null
}

function get_commits_for_project {
    if [ ! -d $CACHE_DIR ]; then
        mkdir -p $CACHE_DIR
    fi
    pushd $CACHE_DIR &> /dev/null
        if [ ! -d $PROJECT_NAME ]; then
            git clone https://github.com/${ORGANIZATION}/${REPO_NAME}.git $PROJECT_NAME &> /dev/null
        fi
        if [ -f ${DATA_DIR}/revisions/${PROJECT_NAME}.txt ]; then
            return # Revision is already collected
        fi

        pushd $PROJECT_NAME &> /dev/null
            default_branch=$(git symbolic-ref --short refs/remotes/origin/HEAD | sed 's@^origin/@@')
            # From oldest to newest, only on the default branch
            git log --first-parent --no-merges --topo-order --remove-empty --name-status --branches ${default_branch} | grep 'java\|^commit' | grep -B1 'java$' | grep ^commit | cut -d ' ' -f 2 | tac > ${DATA_DIR}/revisions/${PROJECT_NAME}.txt
        popd &> /dev/null
    popd &> /dev/null
}

function setup {
    if [[ ! -f ${RANINLINE_JAR} ]]; then
        pushd ${CACHE_DIR}
            if [[ ! -d exli ]]; then
                git clone https://github.com/EngineeringSoftware/exli
            fi
            pushd exli/java
                mvn clean package -DskipTests
            popd
        popd
    fi
}

function find_ts {
    local v0=$1
    local v1=$2

    pushd ${CACHE_DIR}/${PROJECT_NAME} &> /dev/null
        git checkout ${v0} &> /dev/null
        for file in $(cat /tmp/${PROJECT_NAME}_git_diffn.txt | grep "^---" | cut -d '/' -f 2- | grep "\.java"); do
            if [[ -f ${file} ]]; then
                touch /tmp/${PROJECT_NAME}_old_raninline_tmp.txt
                java -jar ${RANINLINE_JAR} target-stmt $(pwd)/${file} /tmp/${PROJECT_NAME}_old_raninline_tmp.txt
                cat /tmp/${PROJECT_NAME}_old_raninline_tmp.txt >> /tmp/${PROJECT_NAME}_old_raninline.txt
                rm -rf /tmp/${PROJECT_NAME}_old_raninline_tmp.txt
            fi
        done

        git checkout - &> /dev/null
        git checkout ${v1} &> /dev/null
        for file in $(cat /tmp/${PROJECT_NAME}_git_diffn.txt | grep "^+++" | cut -d '/' -f 2- | grep "\.java"); do
            if [[ -f ${file} ]]; then
                touch /tmp/${PROJECT_NAME}_new_raninline_tmp.txt
                java -jar ${RANINLINE_JAR} target-stmt $(pwd)/${file} /tmp/${PROJECT_NAME}_new_raninline_tmp.txt
                cat /tmp/${PROJECT_NAME}_new_raninline_tmp.txt >> /tmp/${PROJECT_NAME}_new_raninline.txt
                rm -rf /tmp/${PROJECT_NAME}_new_raninline_tmp.txt
            fi
        done
        git checkout - &> /dev/null

        if [[ -f /tmp/${PROJECT_NAME}_old_raninline.txt ]]; then
            mv /tmp/${PROJECT_NAME}_old_raninline.txt ${OUTPUT_DIR}/${PROJECT_NAME}/old_${v0}-${v1}.txt
        else
            touch ${OUTPUT_DIR}/${PROJECT_NAME}/old_${v0}-${v1}.txt
        fi
        if [[ -f /tmp/${PROJECT_NAME}_new_raninline.txt ]]; then
            mv /tmp/${PROJECT_NAME}_new_raninline.txt ${OUTPUT_DIR}/${PROJECT_NAME}/new_${v0}-${v1}.txt
        else
            touch ${OUTPUT_DIR}/${PROJECT_NAME}/new_${v0}-${v1}.txt
        fi
        if [[ -f /tmp/${PROJECT_NAME}_git_diffn.txt ]]; then
            mv /tmp/${PROJECT_NAME}_git_diffn.txt ${OUTPUT_DIR}/${PROJECT_NAME}/diff_${v0}-${v1}.txt
        fi
    popd &> /dev/null
}

function main {
    if [ ! -d $CACHE_DIR ]; then
        mkdir -p $CACHE_DIR
    fi
    setup &> /dev/null
    get_commits_for_project $PROJECT_NAME
    
    if [[ $(cat ${DATA_DIR}/revisions/${PROJECT_NAME}.txt  | wc -l | xargs) -lt 2 ]]; then
        echo "Project has less than two SHAs"
        exit 1
    fi

    >${OUTPUT_DIR}/${PROJECT_NAME}/changed-ts.txt
    for i in $(seq 2 $(cat ${DATA_DIR}/revisions/${PROJECT_NAME}.txt  | wc -l | xargs)); do
        v0=$(sed "$((i-1))q;d" ${DATA_DIR}/revisions/${PROJECT_NAME}.txt)
        v1=$(sed "${i}q;d" ${DATA_DIR}/revisions/${PROJECT_NAME}.txt)
        # echo "Checking diff ${v0}..${v1}"
        get_git_diff $v0 $v1 /tmp/${PROJECT_NAME}_git_diffn.txt
        find_ts ${v0} ${v1}        
        python3 $SCRIPT_DIR/find_changed_ts.py ${OUTPUT_DIR}/${PROJECT_NAME}/diff_${v0}-${v1}.txt ${OUTPUT_DIR}/${PROJECT_NAME}/old_${v0}-${v1}.txt ${OUTPUT_DIR}/${PROJECT_NAME}/new_${v0}-${v1}.txt ${v0} ${v1} &>> ${OUTPUT_DIR}/${PROJECT_NAME}/changed-ts.txt
    done
}

main
