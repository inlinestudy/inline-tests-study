import json
import sys
import os
import subprocess

SKIPS = "-Djacoco.skip -Dcheckstyle.skip -Drat.skip -Denforcer.skip -Danimal.sniffer.skip -Dmaven.javadoc.skip -Dfindbugs.skip -Dwarbucks.skip -Dmodernizer.skip -Dimpsort.skip -Dpmd.skip -Dxjc.skip -Dair.check.skip-all"
SKIPS_ARRAY = SKIPS.split(' ')

def treat_special(project_name):
    if project_name == "adyliu_jafka":
        to_remove = ['ZookeeperStringTest.java', 'SimpleConsumerTest.java']
        for file in to_remove:
            subprocess.run(f"find . -name {file} | xargs rm -f", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

if __name__ == "__main__":
    dataset_file = sys.argv[1]
    cache_dir = os.path.realpath(sys.argv[2]) # The directory that stores projects
    output_json = sys.argv[3]

    data = json.load(open(dataset_file, 'r'))
    output_data = []
    if not os.path.exists(cache_dir):
        os.makedirs(cache_dir)

    for i, item in enumerate(data):
        print(f"Processing {i + 1}/{len(data)}")
        project_name = item['projectName']
        organization = project_name.split('_')[0]
        repository_name = project_name.split('_', 1)[1]
        broken_commit = item['fixCommitParentSHA1']
        fixed_commit = item['fixCommitSHA1']

        os.chdir(cache_dir)
        if not os.path.exists(project_name):
            subprocess.run(f"git clone https://github.com/{organization}/{repository_name}.git {project_name}", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        os.chdir(os.path.join(cache_dir, project_name))
        subprocess.run(f"git checkout -f {fixed_commit}", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        # Check 1: Compiles for the fixed commit
        exit_code = subprocess.run(f"mvn clean test-compile", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode
        item['fixCommitCompiles'] = exit_code == 0
        # Check 2: Test passes for the fixed commit
        treat_special(project_name)
        exit_code = subprocess.run(f"mvn -l {fixed_commit}-test-log.txt clean test", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode
        item['fixCommitTestsPass'] = exit_code == 0
        # Checkout to the broken commit
        subprocess.run(f"git checkout -f {broken_commit}", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        # Check 3: Compiles for the broken commit
        exit_code = subprocess.run(f"mvn clean test-compile", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode
        item['fixCommitParentCompiles'] = exit_code == 0
        # Check 4: Test passes for the broken commit
        treat_special(project_name)
        exit_code = subprocess.run(f"mvn -l {broken_commit}-test-log.txt clean test", shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode
        item['fixCommitParentTestsPass'] = exit_code == 0
        output_data.append(item)

    os.chdir(os.path.dirname(os.path.join(os.path.abspath(__file__), '..', 'data')))
    json.dump(output_data, open(output_json, 'w'), indent=4)
