import hashlib
import json

# Generate a custom hash for an entry based on key fields but not all fields.
def get_custom_hash(entry):
    keys = ['projectName', 'fixCommitParentSHA1', 'fixCommitSHA1', 'bugFilePath', 'bugLineNum', 'fixLineNum']
    entry_json = json.dumps({key: entry[key] for key in keys}, sort_keys=True)
    return hashlib.md5(entry_json.encode()).hexdigest()
