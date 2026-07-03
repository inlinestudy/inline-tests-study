import os
import sys
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

if __name__ == "__main__":
    exp_name = "enclosing_method_cov_bug"
    data_dir = os.path.join(SCRIPT_DIR, "../../data")
    with open(os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts_res.json"), "r") as f:
        data = json.load(f)
    for entry in data:
        method_result = "NO_INFO"
        if os.path.exists(os.path.join(data_dir, "generated-data", exp_name, entry["customHash"], "method-result.txt")):
            with open(os.path.join(data_dir, "generated-data", exp_name, entry["customHash"], "method-result.txt"), "r") as f:
                method_result = f.read().strip()
        stmt_result = "NO_INFO"
        if os.path.exists(os.path.join(data_dir, "generated-data", exp_name, entry["customHash"], "stmt-result.txt")):
            with open(os.path.join(data_dir, "generated-data", exp_name, entry["customHash"], "stmt-result.txt"), "r") as f:
                stmt_result = f.read().strip()
        print(f"{entry['customHash']},{method_result},{stmt_result}")
