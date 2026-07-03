#!/usr/bin/env python3
import os
import sys
import subprocess
import linecache
from pathlib import Path

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

revision_folder = sys.argv[1]
target_file = sys.argv[2]
diff_folder = sys.argv[3]
repo_folder = sys.argv[4]
output_file = os.path.abspath(sys.argv[5])

targets = {} # {project -> []}
projects = []

all_projects_diff_history = {} # {project -> []}
all_projects_revisions = {} # {project -> {sha -> order}}
all_projects_revisions_raw = {} # {project -> [oldest, ..., newest]}
projects_to_last_checked_sha = {}

all_diff_history = {} # {project -> {diff -> [(oldest_sha, newest_sha)]}}
one_to_one_history = {} # {str(diff) -> diff}


class Diff:
    def __init__(self, project, old_sha, new_sha, old_path, new_path, old_lines, new_lines, category):
        self.project = project
        self.old_sha = old_sha
        self.new_sha = new_sha
        self.old_path = old_path
        self.new_path = new_path
        if isinstance(old_lines, int):
            self.old_lines = [old_lines]
        else:
            self.old_lines = old_lines
        if isinstance(new_lines, int):
            self.new_lines = [new_lines]
        else:
            self.new_lines = new_lines
        self.category = category
        self.claimed_by = []

    def __repr__(self):
        return self.__str__()

    def __str__(self):
        return f"Diff(project={self.project}, old_sha={self.old_sha}, new_sha={self.new_sha}, old_path={self.old_path}, new_path={self.new_path}, old_lines={self.old_lines}, new_lines={self.new_lines}, category={self.category})"


def setup():
    with open(output_file, 'w') as f:
        f.write('hash,class_changed,method_changed,statement_changed,total_revision\n')
    os.chdir(os.path.join(SCRIPT_DIR, '..', 'count-changes'))
    subprocess.run(['mvn', 'clean', 'package'], check=True)
    os.chdir(SCRIPT_DIR)

def checkout_commit(project, commit_sha):
    # Checkout a commit: e.g., checkout_commit('HadoopGenomics/Hadoop-BAM', '2b6740f75196047b2fde0074498f3e5023ecd7dc')
    if project in projects_to_last_checked_sha:
        if projects_to_last_checked_sha[project] == commit_sha:
            return True
    original_dir = os.getcwd()
    try:
        os.chdir(os.path.join(repo_folder, project.replace('/', '_')))
        result = subprocess.run(
            ['git', 'checkout', commit_sha],
            capture_output=True,
            text=True,
            check=True
        )
        projects_to_last_checked_sha[project] = commit_sha
        linecache.clearcache() # different sha, clear the cache
        return True
    except subprocess.CalledProcessError as e:
        print('ERROR: checkout failed', e)
        return False
    finally:
        os.chdir(original_dir)


def get_revisions(project):
    # Get all revisions for a project: e.g., get_revisions('HadoopGenomics/Hadoop-BAM')
    revision_file = os.path.join(revision_folder, project.replace('/', '_') + '.txt')
    with open(revision_file, 'r') as f:
        all_projects_revisions[project] = {}
        all_projects_revisions_raw[project] = []
        order = 0
        for line in f.readlines():
            if line.strip():
                all_projects_revisions[project][line.strip()] = order
                all_projects_revisions_raw[project].append(line.strip())
            order += 1
        

def read_targets():
    # Read all ONE_TO_ONE diff, they are the "target", we hope to find the history for them from ADD/MANY_TO_ONE/ONE_TO_MANY
    with open(target_file) as f:
        inputs = []
        for line in f.readlines():
            if line:
                inputs.append(line.strip())
            if len(inputs) == 2:
                project = inputs[0].split('/')[0].replace('_', '/', 1)
                if project not in projects:
                    projects.append(project)

                old_sha = inputs[0].split(':')[1].split(';')[0]
                new_sha = inputs[0].split(':')[1].split(';')[1]
                old_path = inputs[0].split(':')[1].split(';')[4].partition('/')[2]
                new_path = inputs[1].split(':')[1].split(';')[4].partition('/')[2]
                old_line = inputs[0].split(':')[1].split(';')[5]
                new_line = inputs[1].split(':')[1].split(';')[5]

                if project not in targets:
                    targets[project] = []
                targets[project].append(Diff(project, old_sha, new_sha, old_path, new_path, int(old_line), int(new_line), 'ONE_TO_ONE'))
                inputs = []


def build_diff():
    # Read evo-splitted directory to construct the diff history, sort them based on oldest SHA to newest SHA
    files = [p.name for p in Path(diff_folder).iterdir() if p.is_file()]
    for file in files:
        with open(os.path.join(diff_folder, file)) as f:
            lines = []
            for line in f.readlines():
                line = line.strip()
                lines.append(line)
            # e.g., 6addf3aad776d6640c6e79e5e935f328399d7f0e;bc1bd1dfa6710ea392283ee94dc1dd7262a42469;6addf3aad776d6640c6e79e5e935f328399d7f0e;target stmt bit;apache_datasketches-java/sketches/src/main/java/com/yahoo/sketches/quantiles/DoublesToByteArrayImpl.java;79;null;;null;ONE_TO_ONE
            project = lines[0].split(';')[4].split('/')[0].replace('_', '/', 1)
            if project not in all_projects_diff_history:
                all_projects_diff_history[project] = []
            category = lines[0].split(';')[9]
            if category == 'ONE_TO_ONE':
                old_sha = lines[0].split(';')[0]
                new_sha = lines[0].split(';')[1]
                old_path = lines[0].split(';')[4].partition('/')[2]
                new_path = lines[1].split(';')[4].partition('/')[2]
                old_lines = lines[0].split(';')[5]
                new_lines = lines[1].split(';')[5]
                all_projects_diff_history[project].append(Diff(project, old_sha, new_sha, old_path, new_path, int(old_lines), int(new_lines), category))
            elif category == 'ADD':
                new_sha = lines[0].split(';')[1]
                new_path = lines[0].split(';')[4].partition('/')[2]
                new_lines = lines[0].split(';')[5]
                all_projects_diff_history[project].append(Diff(project, None, new_sha, None, new_path, None, int(new_lines), category))
            elif category == 'DELETE':
                old_sha = lines[0].split(';')[0]
                old_path = lines[0].split(';')[4].partition('/')[2]
                old_lines = lines[0].split(';')[5]
                all_projects_diff_history[project].append(Diff(project, old_sha, None, old_path, None, int(old_lines), None, category))
            elif category == 'ONE_TO_MANY':
                old_sha = lines[0].split(';')[0]
                new_sha = lines[0].split(';')[1]
                old_path = lines[0].split(';')[4].partition('/')[2]
                new_path = lines[1].split(';')[4].partition('/')[2]
                old_lines = lines[0].split(';')[5]
                new_lines = [int(line.split(';')[5]) for line in lines[1:]]
                all_projects_diff_history[project].append(Diff(project, old_sha, new_sha, old_path, new_path, int(old_lines), new_lines, category))
            elif category == 'MANY_TO_ONE':
                old_sha = lines[0].split(';')[0]
                new_sha = lines[0].split(';')[1]
                old_path = lines[0].split(';')[4].partition('/')[2]
                new_path = lines[-1].split(';')[4].partition('/')[2]
                old_lines = [int(line.split(';')[5]) for line in lines[:-1]]
                new_lines = lines[-1].split(';')[5]
                all_projects_diff_history[project].append(Diff(project, old_sha, new_sha, old_path, new_path, old_lines, int(new_lines), category))
    for project in all_projects_diff_history:
        get_revisions(project)
        all_projects_diff_history[project].sort(key=lambda d: all_projects_revisions[project].get(d.new_sha, float('inf')))


def get_target_statement(project, sha, file_path, line_number):
    # Checkout the commit, grap the line of code from the file
    if not checkout_commit(project, sha):
        # cannot checkout
        return 'ERROR: checkout failed'
    file_path = os.path.join(repo_folder, project.replace('/', '_'), file_path)
    if not os.path.exists(file_path):
        # cannot find the file
        return 'ERROR:file not found'
    return linecache.getline(file_path, line_number).strip()
    

def find_history_for_all_projects():
    # Find history for all projects
    for project in projects:
        # if project in ['Omertron/api-themoviedb']:#, 'Pragmatists/JUnitParams', 'tecsinapse/tecsinapse-data-io']:
            # continue
        try:
            find_history_for_project(project)
        except Exception as e:
            print('ERROR in find_history_for_all_projects: ', e)
            print('project: ', project)
            continue


def find_history_for_project(project):
    # Find history for a project
    print('Finding history for project: ', project)

    for i in range(len(all_projects_diff_history[project])):
        current_diff = all_projects_diff_history[project][i]
        print('current diff: ', current_diff)
        if current_diff.category == 'ADD' or (current_diff.category == 'ONE_TO_ONE' and len(current_diff.claimed_by) == 0) or current_diff.category == 'MANY_TO_ONE' or current_diff.category == 'ONE_TO_MANY':
            try:
                    # this diff is from the first SHA for this target statement
                if current_diff.category == 'ONE_TO_ONE' and len(current_diff.claimed_by) == 0:
                    print('oh no, one_to_one is not claimed!!!')
                    current_diff.claimed_by.append(current_diff)
                    one_to_one_history[str(current_diff)] = current_diff
            
                
                for new_line in current_diff.new_lines:
                    targetStmt = get_target_statement(project, current_diff.new_sha, current_diff.new_path, new_line)

                    diff = current_diff
                    oldest_sha = diff.new_sha
                    newest_sha = None
                    number_of_one_to_one = 0
                    update_history = {oldest_sha: new_line}

                    # from i+1, search changes
                    for j in range(i, len(all_projects_diff_history[project])):
                        if newest_sha:
                            break;
                        diff2 = all_projects_diff_history[project][j]
                        if diff2.category == 'ONE_TO_ONE':
                            if diff2.old_path == diff.new_path and get_target_statement(project, diff2.old_sha, diff2.old_path, diff2.old_lines[0]) == targetStmt:
                                diff2.claimed_by.append(current_diff)
                                one_to_one_history[str(diff2)] = diff2
                                targetStmt = get_target_statement(project, diff2.new_sha, diff2.new_path, diff2.new_lines[0])
                                print('one_to_one matched, it is now changed to', targetStmt)
                                print(diff2)
                                update_history[diff2.new_sha] = diff2.new_lines[0]
                                diff = diff2
                                number_of_one_to_one += 1
                        elif diff2.category == 'ONE_TO_MANY':
                            if diff2.old_path == diff.new_path and get_target_statement(project, diff2.old_sha, diff2.old_path, diff2.old_lines[0]) == targetStmt:
                                print('found one_to_many, this is the end of target statement')
                                newest_sha = diff2.old_sha
                                update_history[diff2.old_sha] = diff2.old_lines[0]
                        elif diff2.category == 'DELETE':
                            if diff2.old_path == diff.new_path and get_target_statement(project, diff2.old_sha, diff2.old_path, diff2.old_lines[0]) == targetStmt:
                                print('found delete, this is the end of target statement')
                                newest_sha = diff2.old_sha
                                update_history[diff2.old_sha] = diff2.old_lines[0]
                        elif diff2.category == 'MANY_TO_ONE':
                            if diff2.new_path == diff.new_path:
                                for line in diff2.old_lines:
                                    if get_target_statement(project, diff2.old_sha, diff2.old_path, line) == targetStmt:
                                        print('found many_to_one, this is the end of target statement')
                                        newest_sha = diff2.old_sha
                                        update_history[diff2.old_sha] = line
                                        break
                    if not newest_sha:
                        print('reaching the end, this is now the end of target statement')
                        print(oldest_sha + '...')
                    else:
                        print(oldest_sha + '...' + newest_sha)

                    if project not in all_diff_history:
                        all_diff_history[project] = {}
        
                    all_diff_history[project][str(current_diff)] = (oldest_sha, newest_sha, update_history)

                    print()
                    print()
            except Exception as e:
                continue
        else:
            if current_diff.category == 'ONE_TO_ONE' and len(current_diff.claimed_by) > 0:
                print('claimed by: ', current_diff.claimed_by)

    
def get_results():
    print()
    for project, the_targets in targets.items():
        for target in the_targets:
            try:
                print(target)
                if str(target) not in one_to_one_history:
                    print('missing?????')
                    print(target)
                else:
                    print('found history')
                    if len(one_to_one_history[str(target)].claimed_by) > 1:
                        print('claimed by multiple!!')
                        print(one_to_one_history[str(target)].claimed_by)
                    elif len(one_to_one_history[str(target)].claimed_by) != 1:
                        print('not claimed???')
                    else:
                        oldest_sha, newest_sha, line_history = all_diff_history[project][str(one_to_one_history[str(target)].claimed_by[0])]
                        started = False
                        with open(os.path.join(SCRIPT_DIR, 'tmp.csv'), 'w') as f:
                            for sha in all_projects_revisions_raw[project]:
                                if sha == oldest_sha:
                                    started = True
                                    print('{},{},{}'.format(sha, one_to_one_history[str(target)].new_path, line_history.get(sha)))
                                    f.write('{},{},{}\n'.format(sha, one_to_one_history[str(target)].new_path, line_history.get(sha)))
                                elif sha == newest_sha:
                                    print('{},{},{}'.format(sha, one_to_one_history[str(target)].new_path, line_history.get(sha)))
                                    f.write('{},{},{}\n'.format(sha, one_to_one_history[str(target)].new_path, line_history.get(sha)))
                                    break
                                elif started:
                                    print('{},{},{}'.format(sha, one_to_one_history[str(target)].new_path, line_history.get(sha)))
                                    f.write('{},{},{}\n'.format(sha, one_to_one_history[str(target)].new_path, line_history.get(sha)))
                        result = subprocess.run(['java', '-jar', SCRIPT_DIR + '/../count-changes/target/count-changes-1.0-SNAPSHOT.jar', SCRIPT_DIR + '/tmp.csv', project, os.path.abspath(repo_folder)], capture_output=True, text=True, check=True)
                        print(result.stdout)
                        # print(' '.join(['java', '-jar', SCRIPT_DIR + '/../count-changes/target/count-changes-1.0-SNAPSHOT.jar', SCRIPT_DIR + '/tmp.csv', project, os.path.abspath(repo_folder)]))
                        with open(output_file, 'a') as f:
                            f.write("\"" + str(target) + "\"," + result.stdout)
                print()
            except Exception as e:
                print('ERROR in get_results: ', e)
                print('project: ', project)
                print('target: ', target)

                with open(output_file, 'a') as f:
                    f.write("\"" + str(target) + "\",failed to find history\n")
                continue

        print()
        print()
        # print(all_projects_diff_history[project])

setup()
read_targets()
build_diff()
find_history_for_all_projects()
get_results()
