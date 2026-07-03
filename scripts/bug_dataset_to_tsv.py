import json
import os
from dataset_tools.prepare_dataset_for_inspection import prepare_dataset_for_inspection

script_dir = os.path.dirname(os.path.abspath(__file__))
data_dir = os.path.join(script_dir, "..", "data")
spreadsheet_dir = os.path.join(script_dir, "..", "spreadsheets")
SEP = "\t"

if __name__ == "__main__":
    os.chdir(os.path.join(script_dir, "dataset_tools"))
    prepare_dataset_for_inspection(os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts.json"), "generate_and_run_inline_tests_auto", os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts_res_auto.json"), None)
    prepare_dataset_for_inspection(os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts.json"), "generate_and_run_inline_tests", os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts_res.json"), None)
    auto_dataset_file = os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts_res_auto.json")
    manual_dataset_file = os.path.join(data_dir, "sstubs_merged_mvn_src_bt_classify_ts_res.json")
    with open(auto_dataset_file, "r") as f:
        auto_data = json.load(f)
    with open(manual_dataset_file, "r") as f:
        manual_data = json.load(f)
    with open(os.path.join(spreadsheet_dir, "bug_upload.tsv"), "w") as f:
        for i in range(len(auto_data)):
            entry = auto_data[i]
            manual_entry = manual_data[i]
            f.write(f"{entry['customHash']}{SEP}{entry['bugType']}{SEP}{entry['projectName']}{SEP}{entry['fixCommitParentSHA1']}{SEP}{entry['fixCommitSHA1']}{SEP}{entry['tsType']}{SEP}https://github.com/{entry['projectName'].replace('_', '/')}/commit/{entry['fixCommitSHA1']}{SEP}{entry['experimentResults'] if entry['experimentResults'] else ''}{SEP}{manual_entry['experimentResults'] if manual_entry['experimentResults'] else ''}\n")
