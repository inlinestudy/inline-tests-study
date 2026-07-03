import json
import os
import sys
import hashlib

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# Generate a custom hash for an entry based on key fields but not all fields.
def get_custom_hash(entry):
    keys = ['projectName', 'fixCommitParentSHA1', 'fixCommitSHA1', 'bugFilePath', 'bugLineNum', 'fixLineNum']
    entry_json = json.dumps({key: entry[key] for key in keys}, sort_keys=True)
    return hashlib.md5(entry_json.encode()).hexdigest()

if __name__ == "__main__":
    dataset_file = sys.argv[1]
    command_file = sys.argv[2]
    experiment_name = sys.argv[3]
    use_exli = sys.argv[4]
    use_manual_itests = sys.argv[5]

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
            command = (
                f"bash {SCRIPT_DIR}/one.sh"
                f" -p {project}"
                f" -a {fix_commit}"
                f" -b {bug_commit}"
                f" -f {first_file}"
                f" -g {second_file}"
                f" -l {fix_line}"
                f" -m {bug_line}"
                f" -h {hash_value}"
                f" -n {experiment_name}"
            )
            if use_exli:
                command += " -e " + use_exli
            if use_manual_itests:
                command += " -u " + use_manual_itests
            f.write(command + "\n")
