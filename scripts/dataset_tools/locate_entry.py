import json
import sys
from util import get_custom_hash

if __name__ == "__main__":
    input_arg = sys.argv[1]
    with open(sys.argv[2], "r") as f:
        data = json.load(f)

    try:
        index = int(input_arg)
    except ValueError:
        target_hash = input_arg
        for i, entry in enumerate(data):
            if get_custom_hash(entry) == target_hash:
                index = i
                break
        else:
            print(f"Hash '{target_hash}' not found in dataset")
            sys.exit(1)

    print(json.dumps(data[index], indent=4))
