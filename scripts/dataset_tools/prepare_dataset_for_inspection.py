# Add custom hash as well as experiment results to each entry in the dataset.
# Results are assumed to be in data/generated-data/${experiment_name}/${hash}/result.txt
# Only applicable for the sstubs dataset so far.

import json
import sys
import os
from util import get_custom_hash

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def get_experiment_results(custom_hash, experiment_name):
    result_file = f"{SCRIPT_DIR}/../../data/generated-data/{experiment_name}/{custom_hash}/result.txt"
    if os.path.exists(result_file):
        with open(result_file, "r") as f:
            return f.read().strip()
    return None

def get_excluded_hashes(exclude_entries_from_dataset):
    excluded_hashes = []
    if not exclude_entries_from_dataset:
        return excluded_hashes
    with open(exclude_entries_from_dataset, "r") as f:
        data = json.load(f)
    for entry in data:
        excluded_hashes.append(get_custom_hash(entry))
    return excluded_hashes

def prepare_dataset_for_inspection(dataset_file, experiment_name, output_file, exclude_entries_from_dataset):
    with open(dataset_file, "r") as f:
        data = json.load(f)
    for entry in data:
        entry["customHash"] = get_custom_hash(entry)
        entry["experimentResults"] = get_experiment_results(entry["customHash"], experiment_name)
    with open(output_file, "w") as f:
        excluded_hashes = get_excluded_hashes(exclude_entries_from_dataset)
        json.dump([entry for entry in data if entry["customHash"] not in excluded_hashes], f, indent=4)

if __name__ == "__main__":
    dataset_file = sys.argv[1]
    experiment_name = sys.argv[2]
    output_file = sys.argv[3]
    exclude_entries_from_dataset = sys.argv[4] if len(sys.argv) > 4 else None
    prepare_dataset_for_inspection(dataset_file, experiment_name, output_file, exclude_entries_from_dataset)
