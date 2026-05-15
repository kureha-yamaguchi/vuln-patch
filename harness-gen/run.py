import os
import sys
import json

import openai

from fuzz_introspector import commands as fi_commands

def extract_data_about_target(
    language,
    target_dir,
    target_function_name):

  _, report = fi_commands.analyse_end_to_end(arg_language=language,
                                            target_dir=target_dir,
                                            module_only=True,
                                            dump_files=False)
  project = report['light-project']

  # Get target function
  if target_function_name:
    function = project.find_function_by_name(target_function_name,
                                            True)
  else:
    return None

  if function:
    # Get the source code of the function as a string
    function_source = function.function_source_code_as_text()
  
    # Get a list of cross-refences
    xrefs = project.get_cross_references_by_name(function.name)

    # Convert cross-references into functions as text
    xref_strings = [xref.function_source_code_as_text() for xref in xrefs]

    context = {
        'func_source': function_source,
        'func_signature': function.sig,
        'xrefs': xref_strings,
    }
    return context

def create_prompt_from_data(
    language,
    target_dir,
    target_function_name,
    context):

   # Create an introduction
    prompt = """Hello. You are a %s security engineer and you need to write a fuzzing harness for a codebase you are analysing.

    The codebase is called %s and the target function you need to write a fuzzing harness for is %s\n"""%(language, os.path.basename(target_dir), target_function_name)


    # Create a description of our target
    prompt += """The target function has the following function signature:
    <signature>
    %s
    </signature>

    and the following source code:

    <code>
    %s
    </code>
    """%(context['func_signature'], context['func_source'])


    # Create xrefs if there are any
    if context['xrefs']:
        prompt += """The function is used in other places of the code. Use these cross-references as examples of how to call the target function in the fuzzing harness you write:\n"""
        for xref in context['xrefs']:
            prompt += '<xref>\n%s\n</xref>\n'%(xref)


    # Provide closing statements
    prompt += """I expect you to be great at writing fuzz harnesses and already have a lot of experience writing fuzzing harnesses. You should use the knowledge you have to compose the harness for me. Here are a few more guidelines:

    - The harness you write should be in libFuzzer style.

    That means, the entrypoint of the harness should be the function
    <code>int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)</code>

    - Make sure that the fuzz harness you write will explore code coverage in the target codebase, using the target function provided.


    The only thing you should return is the code itself. Please do not return any other textual description, and the code you return should be fully compilable.
    """

    print("#" * 20 + " prompt " + "#"*20)
    print(prompt)
    print("#"*48)

    llm_prompt = [{'role': 'system', 'content': prompt}]

    return llm_prompt

def main():
  arg_language = sys.argv[1]
  arg_target = sys.argv[2]
  arg_func = sys.argv[3]

  context = extract_data_about_target(arg_language, arg_target, arg_func)
  print(json.dumps(context, indent=2))

  llm_prompt = create_prompt_from_data(arg_language, arg_target, arg_func, context)

  client = openai.OpenAI(
      api_key=os.getenv('VLLM_API_KEY', 'EMPTY'),
      base_url=os.getenv('VLLM_BASE_URL', 'http://localhost:8000/v1'),
  )
  result = client.chat.completions.create(
              messages=llm_prompt, model='openai/gpt-oss-20b')

  print("#" * 20 + " result " + "#"*20)
  print(result.choices[0].message.content)
  print("#"*48)
  

if __name__ == '__main__':
   main()