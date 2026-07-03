import json
import math
import sys
import os

# Partition a json list into multiple files.
# The input file is a json list of entries.
# The output files are named as output_prefix_i.json, where i is the partition number.
# The output directory is the directory where the output files are created.
# The number of partitions is specified by num_parts.
def partition_json_list(input_file: str, output_prefix: str, output_dir: str, num_parts: int):
    with open(input_file, 'r') as f:
        data = json.load(f)
    total_size = len(data)
    part_size = math.ceil(total_size / num_parts)
    for i in range(num_parts):
        start_idx = i * part_size
        end_idx = min((i + 1) * part_size, total_size)
        
        partition = data[start_idx:end_idx]
        
        output_file = f"{output_dir}/{output_prefix}_{i+1}.json"
        with open(output_file, 'w') as f:
            json.dump(partition, f, indent=4)
        
        print(f"Created partition {i+1} with {len(partition)} items")

if __name__ == "__main__":
    input_file = sys.argv[1] # e.g.: "../data/augmented_maven_only_merged.json"
    num_parts = int(sys.argv[2]) # How many partitions to create.
    output_prefix = sys.argv[3] # e.g.: "augmented_maven_only_merged"
    output_dir = sys.argv[4] # e.g.: "../data/augmented_maven_only_merged"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    partition_json_list(input_file, output_prefix, output_dir, num_parts)
