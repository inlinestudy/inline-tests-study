import json
import sys

# Reassign serials in-place.
if __name__ == "__main__":
    dataset_file = sys.argv[1]
    with open(dataset_file, "r") as f:
        data = json.load(f)
    for idx, entry in enumerate(data, 1):
        entry['serial'] = idx
    with open(dataset_file, "w") as f:
        json.dump(data, f, indent=4)
