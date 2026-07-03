import json
import sys
import random
from util import get_custom_hash

def remove_entries(dataset_file, key, value, output_file, aux_args):
    exact_match = aux_args[0] == "" # False by default
    with open(dataset_file, "r") as f:
        data = json.load(f)
    for entry in data:
        if value in entry[key] and not bool(exact_match):
            data.remove(entry)
        elif value == entry[key] and bool(exact_match):
            data.remove(entry)
    with open(output_file, "w") as f:
        json.dump(data, f, indent=4)

def sample_entries(dataset_file, key, value, output_file, aux_args):
    size = int(aux_args[0])
    if len(aux_args) > 1:
        exact_match = aux_args[1] == "" # False by default
    else:
        exact_match = False
    population = []

    with open(dataset_file, "r") as f:
        data = json.load(f)
    for entry in data:
        if not entry[key]:
            print(f"Warning: Key {key} is None in entry {get_custom_hash(entry)}")
            continue
        if value in entry[key] and not bool(exact_match):
            population.append(entry)
        elif value == entry[key] and bool(exact_match):
            population.append(entry)
    with open(output_file, "w") as f:
        sample = random.sample(population, min(size, len(population)))
        for idx, entry in enumerate(sample, 1):
            entry['serial'] = idx
        json.dump(sample, f, indent=4)

if __name__ == "__main__":
    operation = sys.argv[1]
    dataset_file = sys.argv[2]
    key = sys.argv[3]
    value = sys.argv[4]
    output_file = sys.argv[5]
    aux_args = sys.argv[6:]
    if operation == "remove":
        remove_entries(dataset_file, key, value, output_file, aux_args)
    elif operation == "sample":
        sample_entries(dataset_file, key, value, output_file, aux_args)
    else:
        raise ValueError(f"Invalid operation: {operation}")

