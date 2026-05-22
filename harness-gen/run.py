"""Generate a Jazzer harness for a Defects4J bug given a patch from the
ASSERT-KTH/drr dataset, then compile it against the buggy project to
check it builds.

Pipeline stages (each lives in its own module):

    PatchSelector       (patches.py)  pick a random patch + d4j checkout
    TargetAnalyzer      (analysis.py) parse patch + run fuzz-introspector
    PromptBuilder       (prompts.py)  build the chat-completion prompt
    HarnessGenerator    (llm.py)      call the local LLM
    HarnessBuilder      (build.py)    extract + javac the generated source
    JazzerEnvironment   (jazzer.py)   resolve the jazzer-api jar
    config              (config.py)   env-driven constants

Example usage (choose project_name from Chart/Closure/Lang/Math/Time):
    uv run -m run -c --project_name Closure
"""
import argparse
import json
import sys

from analysis import TargetAnalyzer
from build import HarnessBuilder
from jazzer import JazzerEnvironment
from llm import HarnessGenerator
from patches import PatchSelector
from prompts import PromptBuilder


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate and compile a Jazzer harness for a "
                    "Defects4J bug from the drr dataset.",
    )
    parser.add_argument("-c", "--correct", action="store_true",
                        help="Flag for semantically correct patch")
    parser.add_argument("-o", "--overfitting", action="store_true",
                        help="Flag for semantically incorrect patch")
    parser.add_argument("--project_name", type=str,
                        help="Choose from Chart/Closure/Lang/Math/Time")
    parser.add_argument("--language", type=str, nargs='?', default='Java',
                        help='Programming language of project')
    return parser.parse_args()


def main():
    args = parse_args()

    if not (args.correct or args.overfitting):
        print("Please select either --correct flag or --overfitting flag")
        sys.exit(1)

    # 1) Make sure the Jazzer API jar is available before we spend time
    #    checking out the project and prompting the LLM.
    jazzer_api_jar = JazzerEnvironment().ensure()

    # 2) Pick a random patch and check out the corresponding buggy d4j
    #    version.
    selection = PatchSelector(
        project_name=args.project_name,
        correct=args.correct,
        overfitting=args.overfitting,
    ).select()

    # 3) Extract the patch + every project function it touches +
    #    cross-references for each of those functions.
    context = TargetAnalyzer(language=args.language).analyze(
        patch_path=selection.patch_path,
        buggy_dir=selection.buggy_dir,
    )
    print(json.dumps(context.as_dict(), indent=2))

    # 4) Build the chat-completion prompt.
    messages = PromptBuilder(language=args.language).build(
        buggy_dir=selection.buggy_dir,
        context=context,
    )

    # 5) Generate the harness.
    raw = HarnessGenerator().generate(messages)
    print("#" * 20 + " result " + "#" * 20)
    print(raw)
    print("#" * 48)

    # 6) Compile the harness against the buggy project + jazzer-api.
    builder = HarnessBuilder(jazzer_api_jar=jazzer_api_jar)
    source = builder.extract_source(raw)
    build = builder.build(source, selection.buggy_dir)

    print("#" * 20 + " build  " + "#" * 20)
    print(f"harness path : {build.harness_path}")
    print(f"class name   : {build.class_name}")
    print(f"compiled     : {build.compiled}")
    if build.stdout:
        print("--- javac stdout ---")
        print(build.stdout)
    if not build.compiled and build.stderr:
        print("--- javac stderr ---")
        print(build.stderr)
    print("#" * 48)

    # Exit non-zero on compile failure so this can be piped into eval
    # scripts that scan for build outcomes (e.g. measuring the fraction
    # of harnesses that build per APR tool / per bug class).
    sys.exit(0 if build.compiled else 2)


if __name__ == '__main__':
    main()