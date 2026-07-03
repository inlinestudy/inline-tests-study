import json
import sys

# Assuming that the json file is a list of entries, get its size.
if __name__ == "__main__":
    input_file = sys.argv[1]
    with open(input_file, "r") as f:
        data = json.load(f)
    print(len(data))
