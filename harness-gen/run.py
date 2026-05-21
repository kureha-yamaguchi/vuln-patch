"""
Generate a libFuzzer harness for a Defects4J bug given a patch from the
ASSERT-KTH/drr dataset. The script picks a random patch for the chosen
project, checks out the corresponding buggy program version via the
defects4j CLI, and uses fuzz-introspector to pull the source and
cross-references of every function the patch touches. That context, along
with the patch itself, is then sent to a local OpenAI-compatible LLM
(Ollama or LM Studio) which is asked to return a compilable libFuzzer
harness exercising the touched code.

Example usage:
uv run -m run -c --project_name Closure #Choose from Chart/Closure/Lang/Math/Time

"""

import os
import sys
import json

import openai

from fuzz_introspector import commands as fi_commands
import argparse
import random
import os
import subprocess, re


# --- Local model configuration ---------------------------------------------
# By default we target a local Ollama server. To use LM Studio instead, run:
#   export LOCAL_LLM_BASE_URL=http://localhost:1234/v1
#   export LOCAL_LLM_MODEL=openai/gpt-oss-20b      # or whatever LM Studio shows
LOCAL_LLM_BASE_URL = os.getenv('LOCAL_LLM_BASE_URL', 'http://localhost:11434/v1')
LOCAL_LLM_API_KEY  = os.getenv('LOCAL_LLM_API_KEY', 'not-needed')  # dummy
LOCAL_LLM_MODEL    = os.getenv('LOCAL_LLM_MODEL', 'gpt-oss:20b')
# ---------------------------------------------------------------------------

def parse_args():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description="Pull a dataset from Hugging Face Hub and save as CSV")
    parser.add_argument("-c", "--correct", action="store_true",
                        help="Flag for semantically correct patch")
    parser.add_argument("-o", "--overfitting", action="store_true",
                        help="Flag for semantically incorrect patch")
    parser.add_argument("--project_name", type=str,
                       help="Choose from Chart/Closure/Lang/Math/Time")
    parser.add_argument("--language", type=str, nargs='?', default='Java',
                        help='Programming language of project')
    return parser.parse_args()

def extract_data_about_target(language, patch_path, buggy_dir):
  """Run fuzz-introspector on the buggy project and, for every function the
  patch touches, pull its source and cross-references the same way the
  original single-function path did."""

  # 1) Parse the patch: collect modified file paths and candidate function
  #    names. We look in two places:
  #      - the trailing context of `@@ ... @@` hunk headers (often the
  #        enclosing Java method signature when the patch was produced by
  #        git with `*.java diff=java`),
  #      - any `identifier(` on +/- changed lines (called or declared
  #        methods inside the change).
  modified_files = []
  candidate_names = set()
  java_call_re = re.compile(r'\b([A-Za-z_]\w*)\s*\(')

  with open(patch_path) as fh:
    for line in fh:
      m = re.match(r'^---\s+(?:a/)?(\S+)', line)
      if m and m.group(1) != '/dev/null':
        modified_files.append(m.group(1))
        continue
      if line.startswith('@@'):
        tail = line.split('@@', 2)[-1]
        candidate_names.update(java_call_re.findall(tail))
        continue
      if line.startswith(('+', '-')) and not line.startswith(('+++', '---')):
        candidate_names.update(java_call_re.findall(line[1:]))

  # 2) Analyse the buggy checkout once.
  _, report = fi_commands.analyse_end_to_end(arg_language=language,
                                             target_dir=buggy_dir,
                                             module_only=True,
                                             dump_files=False)
  project = report['light-project']

  # 3) For each candidate the project actually knows, collect source +
  #    xrefs. find_function_by_name returns None for unknown names, which
  #    filters out language keywords and non-project identifiers.
  functions = []
  seen = set()
  for name in candidate_names:
    if name in seen:
      continue
    seen.add(name)
    function = project.find_function_by_name(name, True)
    if not function:
      continue
    xrefs = project.get_cross_references_by_name(function.name)
    functions.append({
        'func_name': function.name,
        'func_signature': function.sig,
        'func_source': function.function_source_code_as_text(),
        'xrefs': [xref.function_source_code_as_text() for xref in xrefs],
    })

  # Read the patch verbatim so the prompt can quote it directly.
  with open(patch_path) as fh:
    patch_text = fh.read()

  return {
      'modified_files': modified_files,
      'patch_text': patch_text,
      'functions': functions,
  }

def create_prompt_from_data(language, buggy_dir, context):
    """Build a libFuzzer-harness prompt that includes the patch and every
    function fuzz-introspector resolved for the patch's touched code."""

    codebase = os.path.basename(buggy_dir.rstrip('/'))

    # Create an introduction
    prompt = """Hello. You are a %s security engineer and you need to write a fuzzing harness for a codebase you are analysing.

    The codebase is called %s. A patch has been applied that touches the functions listed below. Your harness should exercise those functions so the behaviour changed by the patch is reachable from the fuzz entrypoint.\n"""%(language, codebase)

    # Include the patch itself so the model sees exactly what changed.
    prompt += """The patch under analysis is:
    <patch>
    %s
    </patch>
    """%(context['patch_text'])

    # One block per touched function with signature, source, and xrefs,
    # mirroring the original single-function prompt layout.
    for fn in context['functions']:
        prompt += """Function: %s has the following function signature:
    <signature>
    %s
    </signature>

    and the following source code:

    <code>
    %s
    </code>
    """%(fn['func_name'], fn['func_signature'], fn['func_source'])

        if fn['xrefs']:
            prompt += """The function is used in other places of the code. Use these cross-references as examples of how to call the target function in the fuzzing harness you write:\n"""
            for xref in fn['xrefs']:
                prompt += '<xref>\n%s\n</xref>\n'%(xref)


    # Provide closing statements
    prompt += """I expect you to be great at writing fuzz harnesses and already have a lot of experience writing fuzzing harnesses. You should use the knowledge you have to compose the harness for me. Here are a few more guidelines:

    - The harness you write should be in libFuzzer style.

    That means, the entrypoint of the harness should be the function
    <code>int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)</code>

    - Make sure that the fuzz harness you write will explore code coverage in the target codebase, using the touched functions provided.


    The only thing you should return is the code itself. Please do not return any other textual description, and the code you return should be fully compilable.
    """

    print("#" * 20 + " prompt " + "#"*20)
    print(prompt)
    print("#"*48)

    # gpt-oss models follow the OpenAI "harmony" chat format. The local
    # servers (Ollama / LM Studio) apply that template automatically when
    # you use the standard chat-completions API, so we don't have to do
    # anything special here. Putting the long instructions as a user
    # message (rather than system) tends to give better results with
    # gpt-oss-20b because its system channel is intended for short, stable
    # instructions like a persona / policy.
    llm_prompt = [
        {'role': 'system', 'content':
            'You are an expert %s security engineer who writes libFuzzer '
            'harnesses. Return only compilable code, no commentary.' % language},
        {'role': 'user', 'content': prompt},
    ]

    return llm_prompt

def main():
    args = parse_args()

    if args.correct:
        patch_dir = "../drr/Patches/Dcorrect"
    elif args.overfitting:
        patch_dir = "../drr/Patches/Doverfitting"
    else:
        print("Please select either --correct flag or --overfitting flag")

    while True:
        apr_tool = random.choice(['ACS','Arja','CapGen','DeepRepair','Elixir','HDRepair','JGenProg2015','Jaid','Nopol2015','SOFix','SequenceR','SimFix','SketchFix','ssFix'])
        target_dir = os.path.join(patch_dir, apr_tool, args.project_name)
        if os.path.isdir(target_dir) and os.listdir(target_dir):
            break

    chosen_file = random.choice(os.listdir(target_dir))
    bug_id = chosen_file.split('-')[2]
    patch_path = os.path.join(target_dir, chosen_file)

    buggy_dir = f"/tmp/d4j/{args.project_name}_{bug_id}_buggy"
    if not os.path.isdir(buggy_dir):
        subprocess.run(
            ["defects4j", "checkout",
            "-p", args.project_name, "-v", f"{bug_id}b", "-w", buggy_dir],
            check=True,
        )

    # # The patch's `--- a/path` header tells you which file it touches, which is the buggy file we want to feed to the LLM.
    # modified_files = []
    # with open(patch_path) as fh:
    #     for line in fh:
    #         m = re.match(r'^---\s+(?:a/)?(\S+)', line)
    #         if m and m.group(1) != '/dev/null':
    #             modified_files.append(os.path.join(buggy_dir, m.group(1)))

    context = extract_data_about_target(args.language, patch_path, buggy_dir)
    print(json.dumps(context, indent=2))

    llm_prompt = create_prompt_from_data(args.language, buggy_dir, context)

    # Point the OpenAI SDK at a local OpenAI-compatible server instead of
    # api.openai.com. Both Ollama (port 11434) and LM Studio (port 1234)
    # speak the /v1/chat/completions protocol, so no other code has to change.
    client = openai.OpenAI(
        base_url=LOCAL_LLM_BASE_URL,
        api_key=LOCAL_LLM_API_KEY,
    )
    result = client.chat.completions.create(
        messages=llm_prompt,
        model=LOCAL_LLM_MODEL,
        # gpt-oss-20b recommended sampling: temperature=1.0, top_p=1.0.
        # Lower temperature if you want more deterministic harnesses.
        temperature=1.0,
        top_p=1.0,
    )

    print("#" * 20 + " result " + "#"*20)
    print(result.choices[0].message.content)
    print("#"*48)


if __name__ == '__main__':
   main()