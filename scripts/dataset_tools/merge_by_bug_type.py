# Merge entries with the same key fields but different bugType

import json
import os
import sys

if __name__ == "__main__":
    path_to_ds = sys.argv[1]
    output_file = sys.argv[2]
    if not os.path.exists(path_to_ds):
        raise FileNotFoundError(f"Dataset {path_to_ds} not found.")
    if not os.path.exists(os.path.dirname(output_file)):
        os.makedirs(os.path.dirname(output_file))
    
    with open(path_to_ds, "r") as f:
        data = json.load(f)

        # Create a dictionary to group entries by their key fields
        grouped_entries = {}
        
        # Define the fields that should be identical for merging
        key_fields = [
            'fixCommitSHA1', 'fixCommitParentSHA1', 'bugFilePath', 'fixPatch',
            'projectName', 'bugLineNum', 'bugNodeStartChar', 'bugNodeLength',
            'fixLineNum', 'fixNodeStartChar', 'fixNodeLength',
            'sourceBeforeFix', 'sourceAfterFix'
        ]
        
        # Group entries by their key fields
        for entry in data:
            # Create a tuple of all key fields to use as dictionary key
            key_tuple = tuple(entry.get(field) for field in key_fields)
            
            if key_tuple not in grouped_entries:
                grouped_entries[key_tuple] = []
            
            grouped_entries[key_tuple].append(entry)
        
        # Merge entries with the same key fields but different bugType
        compressed_data = []
        
        for entries in grouped_entries.values():
            if len(entries) == 1:
                # No duplicates, add as is
                compressed_data.append(entries[0])
            else:
                # Merge entries with different bugTypes
                merged_entry = entries[0].copy()
                bug_types = [entry['bugType'] for entry in entries]
                merged_entry['bugType'] = ','.join(bug_types)
                compressed_data.append(merged_entry)
        
        print(f"Original entries: {len(data)}")
        print(f"Compressed entries: {len(compressed_data)}")

        # Write the compressed data back to the file
        with open(output_file, "w") as out_file:
            json.dump(compressed_data, out_file, indent=4)
