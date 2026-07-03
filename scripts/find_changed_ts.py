import sys

def process_git_diffn_result(git_diffn_output, tsf_result_v0, tsf_result_v1, v0, v1):
    with open(git_diffn_output, 'r') as f:
        lines = f.readlines()
        context_windows = []
        shrinked_file = None
        expanded_file = None
        current_file = None
        context_window_started = False
        for line in lines:
            line = line.strip()
            if line.startswith('-'):
                if line.startswith('---') and line.endswith('.java'):
                    shrinked_file = line.split('/', 1)[1] # Set this variable so that the lines that follows are counted as lines in this file
                    current_file = shrinked_file
                else:
                    if shrinked_file is not None:
                        if not context_window_started:
                            context_window_started = True
                            context_windows.append({"-": [], "+": []})
                        line_number = line.split(':')[0].split('-')[1].strip()
                        if is_key_in_tsf(f"{shrinked_file};{line_number};", tsf_result_v0):
                            corresponding_line = get_line_with_key(f"{shrinked_file};{line_number};", tsf_result_v0)
                            context_windows[-1]["-"].append(f"{v0};{v1};{v0};{corresponding_line}")
            elif line.startswith('+'):
                if line.startswith('+++') and line.endswith('.java'):
                    expanded_file = line.split('/', 1)[1] # Set this variable so that the lines that follows are counted as lines in this file
                    current_file = expanded_file
                else:
                    if expanded_file is not None:
                        if not context_window_started:
                            context_window_started = True
                            context_windows.append({"-": [], "+": []})
                        line_number = line.split(':')[0].split('+')[1].strip()
                        if is_key_in_tsf(f"{expanded_file};{line_number};", tsf_result_v1):
                            corresponding_line = get_line_with_key(f"{expanded_file};{line_number};", tsf_result_v1)
                            context_windows[-1]["+"].append(f"{v0};{v1};{v1};{corresponding_line}")
            else:
                context_window_started = False
    return context_windows

def is_key_in_tsf(search_key, tsf_result):
    for line in tsf_result:
        if search_key in line:
            return True
    return False

def get_line_with_key(search_key, tsf_result):
    for line in tsf_result:
        if search_key in line:
            return line
    return None

if __name__ == "__main__":
    git_diffn_output = sys.argv[1]
    tsf_result_v0_file = sys.argv[2]
    tsf_result_v1_file = sys.argv[3]
    v0 = sys.argv[4]
    v1 = sys.argv[5]
    with open(tsf_result_v0_file, 'r') as f:
        tsf_result_v0 = f.readlines()
    with open(tsf_result_v1_file, 'r') as f:
        tsf_result_v1 = f.readlines()
    context_windows = process_git_diffn_result(git_diffn_output, tsf_result_v0, tsf_result_v1, v0, v1)
    for context in context_windows:
        if len(context["-"]) > 0 and len(context["+"]) == 0:
            for ele in context["-"]:
                print(f"{ele.strip()};DELETE")
        if len(context["-"]) == 0 and len(context["+"]) > 0:
            for ele in context["+"]:
                print(f"{ele.strip()};ADD")
        if len(context["-"]) == 1 and len(context["+"]) == 1:
            for ele in context["-"]:
                print(f"{ele.strip()};ONE_TO_ONE")
            for ele in context["+"]:
                print(f"{ele.strip()};ONE_TO_ONE")
        if len(context["-"]) > 1 and len(context["+"]) == 1:
            for ele in context["-"]:
                print(f"{ele.strip()};MANY_TO_ONE")
            for ele in context["+"]:
                print(f"{ele.strip()};MANY_TO_ONE")
        if len(context["-"]) == 1 and len(context["+"]) > 1:
            for ele in context["-"]:
                print(f"{ele.strip()};ONE_TO_MANY")
            for ele in context["+"]:
                print(f"{ele.strip()};ONE_TO_MANY")
        if len(context["-"]) > 1 and len(context["+"]) > 1:
            for ele in context["-"]:
                print(f"{ele.strip()};MANY_TO_MANY")
            for ele in context["+"]:
                print(f"{ele.strip()};MANY_TO_MANY")
