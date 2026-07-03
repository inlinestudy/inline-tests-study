import json
import sys

def remove_duplicates(data: list):
    seen = set()
    unique_data = []
    for entry in data:
        entry_tuple = tuple(sorted(entry.items()))
        if entry_tuple not in seen:
            seen.add(entry_tuple)
            unique_data.append(entry)
    return unique_data

if __name__ == "__main__":
    output_file = sys.argv[1] # Output file.
    input_files = sys.argv[2:] # List of input files to merge.
    data = []
    for file in input_files:
        with open(file, "r") as f:
            data.extend(json.load(f)) # Append the data from the file to the list.
    data = remove_duplicates(data)
    with open(output_file, "w") as f:
        json.dump(data, f, indent=4)
