import json
import sys
from util import get_custom_hash

def set_minus(set1, set2):
    set2_hashes = set(get_custom_hash(item) for item in set2)
    return [item for item in set1 if get_custom_hash(item) not in set2_hashes]

def set_union(set1, set2):
    set1_hashes = set(get_custom_hash(item) for item in set1)
    return set1 + [item for item in set2 if get_custom_hash(item) not in set1_hashes]

def set_intersection(set1, set2):
    set2_hashes = set(get_custom_hash(item) for item in set2)
    return [item for item in set1 if get_custom_hash(item) in set2_hashes]

if __name__ == "__main__":
    original_set = json.load(open(sys.argv[1], "r"))
    output_file = sys.argv[2]
    operations = sys.argv[3:]

    result = original_set
    print(f'Begin with {len(result)} items from {sys.argv[1]}')
    for operation in operations:
        op = operation.split(",")[0]
        obj = json.load(open(operation.split(",")[1], "r"))
        print(f'Applying {op} to {operation.split(",")[1]}')
        if op == "minus":
            result = set_minus(result, obj)
        elif op == "union":
            result = set_union(result, obj)
        elif op == "intersection":
            result = set_intersection(result, obj)
        else:
            raise ValueError(f"Invalid operation: {operation}")
    
    with open(output_file, "w") as f:
        print(f'Writing {len(result)} items to {output_file}')
        json.dump(result, f, indent=4)
