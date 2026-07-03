#!/usr/bin/env python3

import os
import hashlib
import sys

script_dir = os.path.dirname(os.path.abspath(__file__))
data_dir = os.path.join(script_dir, '../../data')
cache_dir = os.path.abspath(os.path.join(script_dir, '../../cache/'))

new_entries = []

def main():
    exp_name = sys.argv[1]
    directory = os.path.join(data_dir, 'generated-data', exp_name)
    output = os.path.join(data_dir, 'generated-data', 'evo-splitted')
    os.makedirs(output, exist_ok=True)
    projects = [item for item in os.listdir(directory) if os.path.isdir(os.path.join(directory, item))]
    for project in projects:
        file_path = os.path.join(directory, project, 'changed-ts.txt')
        if not os.path.exists(file_path):
            continue
        with open(file_path) as f:
            current = None
            started_second = False
            entries = []
            for line in f.readlines():
                line = line.strip()
                if not line:
                    continue
                data = line.split(';')
                if len(data) < 10:
                    continue
                if current is None:
                    if data[9] in {'ONE_TO_ONE', 'ONE_TO_MANY', 'MANY_TO_ONE', 'MANY_TO_MANY'}:
                        # start tracking (first)
                        started_second = False
                        current = data[0] + ':' + data[1]

                        url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                        entries.append(line + ';' + url)
                    else:
                        # start new entry: ADD or DELETE
                        entries = []
                        started_second = False
                        current = None
                        # entry ends, compute hash
                        hash_value = hashlib.md5('{}\n'.format(line).encode()).hexdigest()
                        url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                        with open(os.path.join(output, '{}.txt'.format(hash_value)), 'a') as f:
                            f.write(line.replace(cache_dir + os.path.sep, '') + '\n')
                        new_entries.append('{};{};{};{}'.format(project, hash_value, line, url))
                else:
                    if data[9] in {'ONE_TO_ONE', 'ONE_TO_MANY', 'MANY_TO_ONE', 'MANY_TO_MANY'}:
                        if not started_second and data[1] == data[2]:
                            # started second
                            started_second = True
                            
                            url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                            entries.append(line + ';' + url)
                        elif data[1] == data[2]:
                            # continue second
                            url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                            entries.append(line + ';' + url)
                        elif not started_second and data[0] == data[2]:
                            # continue first
                            url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                            entries.append(line + ';' + url)
                        elif started_second and data[0] == data[2]:
                            # previous entry ends, compute hash
                            hash_value = hashlib.md5('{}\n'.format('\n'.join([e.rpartition(';')[0] for e in entries])).encode()).hexdigest()
                            for entry in entries:
                                with open(os.path.join(output, '{}.txt'.format(hash_value)), 'a') as f:
                                    f.write(';'.join(entry.split(';')[:-1]).replace(cache_dir + os.path.sep, '') + '\n')
                                new_entries.append('{};{};{}'.format(project, hash_value, entry))

                            # start new entry
                            entries = []
                            started_second = False
                            url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                            current = data[0] + ':' + data[1]
                            entries.append(line + ';' + url)
                    else:
                        # previous entry ends, compute hash
                        hash_value = hashlib.md5('{}\n'.format('\n'.join([e.rpartition(';')[0] for e in entries])).encode()).hexdigest()
                        for entry in entries:
                            with open(os.path.join(output, '{}.txt'.format(hash_value)), 'a') as f:
                                f.write(';'.join(entry.split(';')[:-1]).replace(cache_dir + os.path.sep, '') + '\n')
                            new_entries.append('{};{};{}'.format(project, hash_value, entry))

                        # start new entry: ADD or DELETE
                        entries = []
                        started_second = False
                        current = None
                        # entry ends, compute hash
                        hash_value = hashlib.md5('{}\n'.format(line).encode()).hexdigest()
                        url="https://github.com/{}/{}/commit/{}#diff-{}L{}".format(data[4].split('/')[6].split('_')[0], data[4].split('/')[6].split('_')[1], data[1], data[0], data[5])
                        with open(os.path.join(output, '{}.txt'.format(hash_value)), 'a') as f:
                            f.write(line.replace(cache_dir + os.path.sep, '') + '\n')
                        new_entries.append('{};{};{};{}'.format(project, hash_value, line, url))

        if len(entries) > 0:
            hash_value = hashlib.md5('{}\n'.format('\n'.join([e.rpartition(';')[0] for e in entries])).encode()).hexdigest()
            for entry in entries:
                with open(os.path.join(output, '{}.txt'.format(hash_value)), 'a') as f:
                    f.write(';'.join(entry.split(';')[:-1]).replace(cache_dir + os.path.sep, '') + '\n')
                new_entries.append('{};{};{}'.format(project, hash_value, entry))
        # for entry in new_entries:
        #     print(entry)

if __name__ == "__main__":
    main()
