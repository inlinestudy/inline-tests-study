import json
import os
import sys
import hashlib

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
LOGS_GENERATED_LOGS_DIR = os.path.join(SCRIPT_DIR, '../../logs/generated-logs')
DATA_GENERATED_DATA_DIR = os.path.join(SCRIPT_DIR, '../../data/generated-data')

# Generate a custom hash for an entry based on key fields but not all fields.
def get_custom_hash(entry):
    keys = ['projectName', 'fixCommitParentSHA1', 'fixCommitSHA1', 'bugFilePath', 'bugLineNum', 'fixLineNum']
    entry_json = json.dumps({key: entry[key] for key in keys}, sort_keys=True)
    return hashlib.md5(entry_json.encode()).hexdigest()

if __name__ == "__main__":
    dataset_file = sys.argv[1]
    command_file = sys.argv[2]
    experiment_name = sys.argv[3]
    if len(sys.argv) > 4:
        local = sys.argv[4]
    else:
        local = ""

    with open(command_file, 'w') as f:
        data = json.load(open(dataset_file, 'r'))
        for entry in data:
            project = entry['projectName']
            bug_commit = entry['fixCommitParentSHA1']
            fix_commit = entry['fixCommitSHA1']
            first_file = entry['bugFilePath']
            second_file = entry['bugFilePath']
            fix_line = entry['bugLineNum']
            bug_line = entry['fixLineNum']
            hash_value = get_custom_hash(entry)
            url = f"https://github.com/{project.replace('_', '/', 1)}"
            if local != "":
                command = f"java -jar {SCRIPT_DIR}/../coverage-checker-enclosing-method/target/coverage-checker-1.0-SNAPSHOT.jar {url} {bug_commit} {first_file} {bug_line} {DATA_GENERATED_DATA_DIR}/{experiment_name}/{hash_value} ../../local_dependencies ../../cache &> {LOGS_GENERATED_LOGS_DIR}/{experiment_name}/{hash_value}.log"
            else:
                command = f"bash one.sh {url} {bug_commit} {first_file} {bug_line} {hash_value} {experiment_name}"
            f.write(command + "\n")
