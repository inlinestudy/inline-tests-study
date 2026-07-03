# A backbone script for a variety of dataset processing tasks,
# based on the observation that most of the tasks will iterate over the dataset.

import json
import sys
import os
import subprocess
import hashlib
import create_contained_environment
from datetime import datetime

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CACHE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'cache')

# For a particular entry, find the last occurrence of "Tests run:" pattern with regex, and extract the number of tests run, failures, errors, and skipped.
# Then, append the results to the output file.
def probe_failed_tests(entry, output_file):
    project_name = entry['projectName']
    organization = project_name.split('_')[0]
    repository_name = project_name.split('_', 1)[1]
    broken_commit = entry['fixCommitParentSHA1']
    fixed_commit = entry['fixCommitSHA1']
    os.chdir(os.path.join(CACHE_DIR))
    if not os.path.exists(project_name):
        subprocess.run(["git", "clone", f"https://github.com/{organization}/{repository_name}.git", project_name])
    os.chdir(os.path.join(CACHE_DIR, project_name))
    for commit in [broken_commit, fixed_commit]:
        subprocess.run(["git", "checkout", "-f", commit])
        subprocess.run(["mvn", "-l", "tmp-test-log.txt", "clean", "test"])
        # Read the test log file and find the last occurrence of test results
        if os.path.exists("tmp-test-log.txt"):
            with open("tmp-test-log.txt", "r") as f:
                lines = f.readlines()
            # Search for the last occurrence of "Tests run:" pattern with regex
            import re
            test_result_pattern = r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)"
            last_test_result = None
            last_match = None
            for line in reversed(lines):
                match = re.search(test_result_pattern, line)
                if match:
                    last_test_result = line.strip()
                    last_match = match
                    break
            if last_test_result:
                tests_run = int(last_match.group(1))
                failures = int(last_match.group(2))
                errors = int(last_match.group(3))
                skipped = int(last_match.group(4))
                all_tests = tests_run + failures + errors + skipped
                with open(output_file, 'a') as f:
                    f.write(f"{project_name},{commit},{failures + errors},{all_tests}\n")
            else:
                print(f"No test results found for {project_name}")
            os.remove("tmp-test-log.txt")
        else:
            print(f"Test log file not found for {project_name}")

# For a particular entry, check if the project is a Maven project.
# If it is, return the entry.
# If it is not, clone the project, checkout the fix commit, and check if the project is a Maven project.
# If it is, return the entry.
# If it is not, return None.
def remove_non_maven(entry):
    project_name = entry['projectName']
    organization = project_name.split('_')[0]
    repository_name = project_name.split('_', 1)[1]
    os.chdir(os.path.join(CACHE_DIR))
    if not os.path.exists(project_name):
        subprocess.run(["git", "clone", f"https://github.com/{organization}/{repository_name}.git", project_name])
    os.chdir(os.path.join(CACHE_DIR, project_name))
    for commit in [entry['fixCommitParentSHA1'], entry['fixCommitSHA1']]:
        subprocess.run(["git", "checkout", "-f", commit])
        result = subprocess.run(["find", ".", "-name", "pom.xml"], capture_output=True, text=True)
        if len(result.stdout) != 0:
            return entry

# For a dataset, find all entries that are TSF-supported.
def find_tsf_supported_entries(data):
    return [entry for entry in data if str(entry['tsType']).startswith('target stmt')]

# Generate a custom hash for an entry based on key fields but not all fields.
def get_custom_hash(entry):
    keys = ['projectName', 'fixCommitParentSHA1', 'fixCommitSHA1', 'bugFilePath', 'bugLineNum', 'fixLineNum']
    entry_json = json.dumps({key: entry[key] for key in keys}, sort_keys=True)
    return hashlib.md5(entry_json.encode()).hexdigest()

# For a particular entry, generate inline tests and run them.
def generate_and_run_inline_tests(entry, github_access_token):
    if not create_contained_environment.has_image(entry['projectName'], entry['fixCommitParentSHA1'][0:7], entry['fixCommitSHA1'][0:7]):
        return

    entry_hash = get_custom_hash(entry)

    try:
        subprocess.run(["bash", "generate_and_run_inline_tests.sh", "-p", str(entry['projectName']), "-b", str(entry['fixCommitParentSHA1']), "-c", str(entry['fixCommitSHA1']), "-f", str(entry['bugFilePath']), "-l", str(entry['bugLineNum']), "-m", str(entry['fixLineNum']), "-t", str(github_access_token), "-h", str(entry_hash)], timeout=3600)
    except subprocess.TimeoutExpired:
        if os.path.exists(f"{SCRIPT_DIR}/../logs/generated-logs/generate-and-run-inline-tests/{entry_hash}/log.txt"):
            with open(f"{SCRIPT_DIR}/../logs/generated-logs/generate-and-run-inline-tests/{entry_hash}/log.txt", "a") as f:
                f.write("TIMEOUT_1H\n")

# For a particular entry and a counter, check if the entry is containerized.
# If it is, increment the counter.
# If it is not, return the counter.
def count_containerized(entry, counter):
    project_name = entry['projectName']
    broken_commit = entry['fixCommitParentSHA1']
    fixed_commit = entry['fixCommitSHA1']
    if create_contained_environment.has_image(project_name, broken_commit[0:7], fixed_commit[0:7]):
        return counter + 1
    return counter

# For a particular entry, append a custom hash to the entry.
def append_custom_hash(entry):
    entry['customHash'] = get_custom_hash(entry)
    return entry

# For a particular entry, classify the statement of that entry.
def classify_statement(entry):
    os.chdir(os.path.join(SCRIPT_DIR, "statement-classifier"))
    if not os.path.exists(os.path.join(SCRIPT_DIR, "statement-classifier", "target", "statement-classifier-1.0-SNAPSHOT.jar")):
        subprocess.run(["mvn", "clean", "package"])
    os.chdir(CACHE_DIR)
    if not os.path.exists(entry['projectName']):
        subprocess.run(["git", "clone", f"https://github.com/{entry['projectName'].split('_')[0]}/{entry['projectName'].split('_', 1)[1]}.git", entry['projectName']])
    os.chdir(os.path.join(CACHE_DIR, entry['projectName']))
    subprocess.run(["git", "checkout", "-f", entry['fixCommitSHA1']])
    result = subprocess.run(["java", "-jar", os.path.join(SCRIPT_DIR, "statement-classifier", "target", "statement-classifier-1.0-SNAPSHOT.jar"), entry['bugFilePath'], str(entry['fixLineNum'])], capture_output=True, text=True)
    verdicts = [verdict.strip() for verdict in result.stdout.split('\n') if verdict != ""]
    priority_verdicts = ["target stmt bit", "target stmt stream", "target stmt regex", "target stmt string"]
    # If tsType contains one of the four priority verdicts, use that; otherwise as before
    priority_verdicts_set = set(["target stmt bit", "target stmt stream", "target stmt regex", "target stmt string"])
    priority_verdict = next((verdict for verdict in verdicts if verdict in priority_verdicts_set), None)
    if priority_verdict is not None:
        entry['tsType'] = priority_verdict
    else:
        entry['tsType'] = next((verdict for verdict in verdicts if "target stmt" in verdict), verdicts[-1] if verdicts else "")
    return entry

# For a particular entry, collect coverage for the broken and fixed versions of the project.
def collect_coverage(entry):
    if not os.path.exists(os.path.join(SCRIPT_DIR, "coverage-checker", "target", "coverage-checker-1.0-SNAPSHOT.jar")):
        subprocess.run(["mvn", "clean", "package"], cwd=os.path.join(SCRIPT_DIR, "coverage-checker"))
    project_name = entry['projectName']
    broken_commit = entry['fixCommitParentSHA1']
    fixed_commit = entry['fixCommitSHA1']
    if not create_contained_environment.has_image(project_name, broken_commit[0:7], fixed_commit[0:7]):
        entry['brokenCoverage'] = "NO_CONTAINER"
        entry['fixedCoverage'] = "NO_CONTAINER"
        return entry
    entry_hash = get_custom_hash(entry)
    coverage_collect_dir = os.path.join(SCRIPT_DIR, "..", "data", "generated-data", "collect-coverage")
    coverage_log_dir = os.path.join(SCRIPT_DIR, "..", "logs", "generated-logs", "collect-coverage")
    broken_coverage_dir = os.path.join(coverage_collect_dir, f"{project_name}_{broken_commit[0:7]}")
    fixed_coverage_dir = os.path.join(coverage_collect_dir, f"{project_name}_{fixed_commit[0:7]}")
    if not os.path.exists(os.path.join(broken_coverage_dir, "jacoco.exec")) or not os.path.exists(os.path.join(fixed_coverage_dir, "jacoco.exec")):
        try:
            subprocess.run(["bash", "collect_coverage_for_containerized.sh", "-b", broken_commit, "-c", fixed_commit, "-p", project_name, "-d", coverage_collect_dir, "-l", coverage_log_dir, "-f", entry['bugFilePath']], timeout=3600)
        except subprocess.TimeoutExpired:
            entry['brokenCoverage'] = "TIMEOUT_1H"
            entry['fixedCoverage'] = "TIMEOUT_1H"
            return entry
    if not os.path.exists(os.path.join(broken_coverage_dir, "jacoco.exec")):
        entry['brokenCoverage'] = "NO_JACOCO_EXEC"
    elif not os.path.exists(os.path.join(broken_coverage_dir, "classes")):
        entry['brokenCoverage'] = "NO_COMPILED_SOURCES"
    else:
        result = subprocess.run(["java", "-jar", os.path.join(SCRIPT_DIR, "coverage-checker", "target", "coverage-checker-1.0-SNAPSHOT.jar"), os.path.join(broken_coverage_dir, "jacoco.exec"), os.path.join(broken_coverage_dir, "classes"), entry['bugFilePath'], str(entry['bugLineNum'])], capture_output=True, text=True)
        entry['brokenCoverage'] = result.stdout.strip()
    if not os.path.exists(os.path.join(fixed_coverage_dir, "jacoco.exec")):
        entry['fixedCoverage'] = "NO_JACOCO_EXEC"
    elif not os.path.exists(os.path.join(fixed_coverage_dir, "classes")):
        entry['fixedCoverage'] = "NO_COMPILED_SOURCES"
    else:
        result = subprocess.run(["java", "-jar", os.path.join(SCRIPT_DIR, "coverage-checker", "target", "coverage-checker-1.0-SNAPSHOT.jar"), os.path.join(fixed_coverage_dir, "jacoco.exec"), os.path.join(fixed_coverage_dir, "classes"), entry['bugFilePath'], str(entry['bugLineNum'])], capture_output=True, text=True)
        entry['fixedCoverage'] = result.stdout.strip()
    return entry

# For a dataset, remove all entries that do not contain "src/main/java" in the bug file path.
def remove_test(data):
    return [entry for entry in data if "src/main/java" in entry['bugFilePath']]

# For a dataset, remove all entries that have bug types that inline test does not handle.
def remove_uninteresting_bug_types(data):
    uninteresting_bug_types = ["ADD_THROWS_EXCEPTION", "CHANGE_MODIFIER", "DELETE_THROWS_EXCEPTION"]
    return [entry for entry in data if str(entry['bugType']) not in uninteresting_bug_types]

def remove_uninteresting_ts_types(data):
    uninteresting_ts_types = ["", "RHS too simple", "condition too simple", "method or constructor declaration",
            "not a statement", "return statement", "statement without assignment", "throw statement",
            "other statements"]
    return [entry for entry in data if str(entry['tsType']) not in uninteresting_ts_types]

# A pre-processing step that rebuilds the statement classifier once and change to cache directory.
def preprocess(task, data):
    if task == "classify_statement":
        os.chdir(os.path.join(SCRIPT_DIR, "statement-classifier"))
        subprocess.run(["mvn", "clean", "package"])
        os.chdir(CACHE_DIR)

if __name__ == "__main__":
    start = datetime.now()
    if not os.path.exists(CACHE_DIR):
        os.makedirs(CACHE_DIR)
    task = sys.argv[1]
    dataset_file = sys.argv[2]
    if len(sys.argv) > 3:
        aux_arg = str(sys.argv[3])
    else:
        aux_arg = None

    data = json.load(open(dataset_file, 'r'))
    processed_data = []
    preprocess(task, data)
    progress = 0
    total = len(data)

    for entry in data:
        if task == "probe_failed_tests":
            aux_arg = os.path.realpath(aux_arg)
            probe_failed_tests(entry, aux_arg)
        if task == "remove_non_maven":
            result = remove_non_maven(entry)
            if result is not None:
                processed_data.append(result)
        if task == "generate_and_run_inline_tests":
            generate_and_run_inline_tests(entry, aux_arg)
        if task == "count_containerized":
            # Remember to feed 0 to aux_arg when running this task.
            aux_arg = int(aux_arg)
            aux_arg = count_containerized(entry, aux_arg)
        if task == "append_custom_hash":
            processed_data.append(append_custom_hash(entry))
        if task == "classify_statement":
            processed_data.append(classify_statement(entry))
        if task == "collect_coverage":
            processed_data.append(collect_coverage(entry))
        progress = progress + 1
        print(f'Progress: {progress}/{total}')

    if task == "remove_test":
        processed_data = remove_test(data)
    if task == "find_tsf_supported_entries":
        processed_data = find_tsf_supported_entries(data)
    if task == "remove_uninteresting_bug_types":
        processed_data = remove_uninteresting_bug_types(data)
    if task == "count_containerized":
        print(aux_arg)
    if task == "remove_uninteresting_ts_types":
        processed_data = remove_uninteresting_ts_types(data)

    if task == "append_custom_hash" or task == "classify_statement" or task == "collect_coverage" or task == "remove_non_maven" or task == "remove_test" or task == "find_tsf_supported_entries" or task == "remove_uninteresting_bug_types" or task == "remove_uninteresting_ts_types":
        print('Size Before:', len(data))
        print('Size After:', len(processed_data))
        os.chdir(SCRIPT_DIR)
        with open(aux_arg, 'w') as f:
            json.dump(processed_data, f, indent=4)
    print('Time taken: {:.2f} seconds'.format((datetime.now() - start).total_seconds()))

