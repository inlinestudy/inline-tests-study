import json
import sys

# When checking reproducibility, some stages are skipped. This script fills in the missing fields.
if __name__ == "__main__":
    reproducibility_file = sys.argv[1]

    raw_data = json.load(open(reproducibility_file))
    processed_data = []
    for entry in raw_data:
        if 'fixCommitCompiles' not in entry:
            entry['fixCommitCompiles'] = False
        if 'fixCommitTestsPass' not in entry:
            entry['fixCommitTestsPass'] = False
        if 'fixCommitParentCompiles' not in entry:
            entry['fixCommitParentCompiles'] = False
        if 'fixCommitParentTestsPass' not in entry:
            entry['fixCommitParentTestsPass'] = False
        processed_data.append(entry)
    json.dump(processed_data, open(reproducibility_file, "w"), indent=4)