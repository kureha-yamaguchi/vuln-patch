"""Generate Jazzer harnesses for a Defects4J bug given a patch from the
ASSERT-KTH/drr dataset. Keep regenerating with the same prompt until a
target number of harnesses compile against the buggy project.

Pipeline stages (each lives in its own module):

    PatchSelector       (patches.py)   pick a random patch (given correct/overfitting label and project namne label) + d4j checkout
    TargetAnalyzer      (analysis.py)  parse patch + run fuzz-introspector
    PromptBuilder       (prompts.py)   build the chat-completion prompt
    HarnessGenerator    (llm.py)       call the local LLM
    HarnessBuilder      (build.py)     extract + javac the generated source
    HarnessCampaign     (campaign.py)  loop generate→build until N succeed
    JazzerEnvironment   (jazzer.py)    resolve the jazzer-api jar
    config              (config.py)    env-driven constants

Example usage (choose project_name from Chart/Closure/Lang/Math/Time):
    uv run -m run -c --project_name Closure
    uv run -m run -c --project_name Closure -n 5 -m 50
"""
import argparse
import json
import sys

from analysis import TargetAnalyzer
from build import HarnessBuilder
from campaign import HarnessCampaign, CampaignResult
from jazzer import JazzerEnvironment
from llm import HarnessGenerator
from patches import PatchSelector
from prompts import PromptBuilder


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate and compile Jazzer harnesses for a "
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
    parser.add_argument("-n", "--target_successes", type=int, default=5,
                        help="Stop once this many harnesses compile "
                             "(default: 5)")
    parser.add_argument("-m", "--max_attempts", type=int, default=50,
                        help="Hard cap on total generation attempts "
                             "(default: 50)")
    parser.add_argument("--max_repair_failures", type=int, default=2,
                        help="maximum number of failures in a row before resetting the prompt context")
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
    context = TargetAnalyzer().analyze(
        patch_path=selection.patch_path,
        buggy_dir=selection.buggy_dir,
    )
    print(json.dumps(context.as_dict(), indent=2))

    # 4) Build the chat-completion prompt (once — same prompt for every
    #    attempt in the campaign).
    messages = PromptBuilder(language=args.language).build(
        buggy_dir=selection.buggy_dir,
        context=context,
    )

    # 5) Run the campaign: regenerate + recompile until we have
    #    target_successes wins or hit max_attempts.
    campaign = HarnessCampaign(
        generator=HarnessGenerator(temperature=0.6, top_p=1.0),
        builder=HarnessBuilder(jazzer_api_jar=jazzer_api_jar),
        target_successes=args.target_successes,
        max_attempts=args.max_attempts,
        max_repair_failures=args.max_repair_failures
    )
    result = campaign.run(messages, selection.buggy_dir)

    _print_summary(selection, result)

    sys.exit(0 if result.converged else 2)


def _print_summary(selection, result: CampaignResult) -> None:
    print("\n" + "#" * 20 + " campaign " + "#" * 20)
    print(f"project       : {selection.project_name}")
    print(f"bug           : {selection.bug_id} ({selection.apr_tool})")
    print(f"buggy dir     : {selection.buggy_dir}")
    print(f"target wins   : {result.target_successes}")
    print(f"attempts used : {result.attempts}")
    print(f"wins          : {result.achieved_successes}")
    print(f"success rate  : {result.success_rate:.1%}")
    print(f"converged     : {result.converged}")
    if result.successful_results:
        print("successful harnesses:")
        for br in result.successful_results:
            print(f"  - {br.harness_path}")
    print("#" * 50)


if __name__ == '__main__':
    main()