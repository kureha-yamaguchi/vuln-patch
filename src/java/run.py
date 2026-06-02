"""Generate Jazzer harnesses for a Defects4J bug given a patch from the
ASSERT-KTH/drr dataset. Keep regenerating with the same prompt until a
target number of harnesses compile against the buggy project, then fuzz
the successful harnesses against the patched code to check for overfitting.

Pipeline stages (each lives in its own module):

    PatchSelector       (patches.py)     pick a random patch + d4j checkout
    FailureTestExtractor(failure_test.py) read the d4j bug-triggering test
    TargetAnalyzer      (analysis.py)    parse patch + run fuzz-introspector
    PromptBuilder       (prompts.py)     build the chat-completion prompt
    HarnessGenerator    (llm.py)         call the local LLM
    HarnessBuilder      (build.py)       extract + javac the generated source
    HarnessCampaign     (campaign.py)    loop generate→build until N succeed
    JazzerEnvironment   (jazzer.py)      resolve jazzer jars
    FuzzRunner          (fuzz_runner.py) run harnesses against patched code
    config              (config.py)      env-driven constants

Example usage (choose project_name from Chart/Closure/Lang/Math/Time):
    uv run -m run -o --project_name Lang -n 5 -m 50 --fuzz_timeout 60
"""
import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config
from analysis import TargetAnalyzer
from build import HarnessBuilder
from campaign import HarnessCampaign, CampaignResult
from failure_test import FailureTestExtractor, is_crashing_bug
from fuzz_runner import FuzzRunner, HarnessVerifier
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
    parser.add_argument("-n", "--target_successes", type=int, default=3,
                        help="Stop once this many harnesses compile "
                             "(default: 5)")
    parser.add_argument("-m", "--max_attempts", type=int, default=50,
                        help="Hard cap on total generation attempts "
                             "(default: 50)")
    parser.add_argument("--max_repair_failures", type=int, default=5,
                        help="maximum number of failures in a row before resetting the prompt context")
    parser.add_argument("--fuzz_timeout", type=int, default=60,
                        metavar="SECONDS",
                        help="seconds Jazzer runs per harness against the "
                             "patched code (default: 30; 0 to skip fuzzing)")
    parser.add_argument("--verify_timeout", type=int,
                        default=None, metavar="SECONDS",
                        help="seconds Jazzer runs per harness against the "
                             "BUGGY code to verify it triggers before "
                             "accepting it (default: config."
                             "VERIFY_TIMEOUT_SECONDS)")
    parser.add_argument("--no-require-trigger", dest="require_trigger",
                        action="store_false",
                        help="accept harnesses on compile alone (old "
                             "behaviour); skip the buggy-version trigger "
                             "gate. Default is to require a trigger.")
    parser.set_defaults(require_trigger=True)
    return parser.parse_args()


def main():
    args = parse_args()

    if not (args.correct or args.overfitting):
        print("Please select either --correct flag or --overfitting flag")
        sys.exit(1)

    # 1) Resolve Jazzer jars up front so failures surface before the slow
    #    checkout + LLM campaign. The standalone (driver) jar is needed
    #    both for the final patched-code run AND for the in-campaign
    #    trigger gate, so fetch it if either is active.
    jazzer_env = JazzerEnvironment()
    jazzer_api_jar = jazzer_env.ensure()
    needs_driver = args.fuzz_timeout > 0 or args.require_trigger
    jazzer_standalone_jar = (jazzer_env.ensure_driver()
                             if needs_driver else None)

    # 2) Pick a random patch and check out the corresponding buggy d4j
    #    version.
    selection = PatchSelector(
        project_name=args.project_name,
        correct=args.correct,
        overfitting=args.overfitting,
    ).select()

    # 3a) Extract the bug-triggering test(s) shipped with this d4j bug.
    #     They seed the prompt with a worked example of a crashing
    #     input — the LLM sees what values already drive the buggy code
    #     path and shapes its FuzzedDataProvider calls accordingly.
    failure_tests = FailureTestExtractor().extract(
        selection.buggy_dir,
        project_name=selection.project_name,
        bug_id=selection.bug_id,
    )
    _print_failure_tests(failure_tests)

    # 3a-bis) Scope gate: this pipeline currently handles only *crashing*
    #     bugs, whose trigger test fails with a thrown application
    #     exception. Semantic bugs (trigger test fails a JUnit assertion)
    #     need the differential oracle that isn't built yet, and the crash
    #     gate would just burn the whole attempt budget failing on them —
    #     exactly the wasted run seen on Closure_73. Skip them up front.
    if not is_crashing_bug(failure_tests):
        print(f"\n{selection.project_name} {selection.bug_id} "
              f"({selection.apr_tool}) is not a crashing bug "
              "(trigger test fails on an assertion, or its failure type "
              "could not be determined) — out of scope for the crash "
              "pipeline. Skipping.")
        sys.exit(3)

    # 3b) Extract the patch + every project function it touches +
    #     cross-references for each of those functions.
    context = TargetAnalyzer().analyze(
        patch_path=selection.patch_path,
        buggy_dir=selection.buggy_dir,
    )
    print(json.dumps(context.as_dict(), indent=2))

    # 4) Build the chat-completion prompt. Rather than a single fixed
    #    prompt, we wrap PromptBuilder in a factory the campaign calls
    #    before each fresh attempt: it injects which reachable functions
    #    and crashes the set already covers, so each new harness is a
    #    *variant* steered at the uncovered part of the root-cause region.
    prompt_builder = PromptBuilder(language=args.language)

    def prompt_factory(covered_functions, found_signatures):
        return prompt_builder.build(
            buggy_dir=selection.buggy_dir,
            context=context,
            failure_tests=failure_tests,
            covered_functions=covered_functions,
            found_signatures=found_signatures,
        )

    # The set-empty prompt used for attempt 1.
    messages = prompt_factory([], [])

    # 5) Run the campaign: regenerate, recompile, and (by default) verify
    #    each compiled harness crashes the BUGGY version before accepting
    #    it. Acceptance = "compiles AND triggers".
    builder = HarnessBuilder(jazzer_api_jar=jazzer_api_jar)
    verifier = None
    if args.require_trigger:
        # Resolve the buggy classpath once (compiles the project), then
        # hand it to the verifier so each gate run is just a Jazzer
        # invocation.
        buggy_cp = builder.test_classpath(selection.buggy_dir)
        verify_timeout = (args.verify_timeout
                          if args.verify_timeout is not None
                          else config.VERIFY_TIMEOUT_SECONDS)
        verifier = HarnessVerifier(
            jazzer_standalone_jar=jazzer_standalone_jar,
            buggy_classpath=buggy_cp,
            timeout_seconds=verify_timeout,
        )

    campaign = HarnessCampaign(
        generator=HarnessGenerator(temperature=0.6, top_p=1.0),
        builder=builder,
        target_successes=args.target_successes,
        max_attempts=args.max_attempts,
        max_repair_failures=args.max_repair_failures,
        verifier=verifier,
        require_trigger=args.require_trigger,
    )
    result = campaign.run(messages, selection.buggy_dir,
                          prompt_factory=prompt_factory)

    _print_summary(selection, result)

    # 6) Fuzz every successful harness against the patched code to check
    #    whether the vulnerability is still reachable (overfitting signal).
    if args.fuzz_timeout > 0 and result.successful_results:
        print("\n" + "#" * 20 + " fuzzing patched code " + "#" * 20)
        fuzz_results = FuzzRunner(
            jazzer_standalone_jar=jazzer_standalone_jar,
            timeout_seconds=args.fuzz_timeout,
        ).run_all(
            successful_results=result.successful_results,
            patch_path=selection.patch_path,
            buggy_dir=selection.buggy_dir,
        )
        _print_fuzz_summary(fuzz_results)

    sys.exit(0 if result.converged else 2)


def _print_failure_tests(failure_tests) -> None:
    if not failure_tests:
        print("No bug-triggering tests found — continuing without seed.")
        return
    print(f"Found {len(failure_tests)} bug-triggering test(s):")
    for ft in failure_tests:
        marker = '✓' if ft.has_source else '?'
        exc = f"  [{ft.exception_type}]" if ft.exception_type else ""
        print(f"  {marker} {ft.test_class}::{ft.test_method}{exc}")


def _print_summary(selection, result: CampaignResult) -> None:
    print("\n" + "#" * 20 + " campaign " + "#" * 20)
    print(f"project       : {selection.project_name}")
    print(f"bug           : {selection.bug_id} ({selection.apr_tool})")
    print(f"buggy dir     : {selection.buggy_dir}")
    print(f"target wins   : {result.target_successes}")
    print(f"attempts used : {result.attempts}")
    print(f"wins          : {result.achieved_successes}")
    print(f"success rate  : {result.success_rate:.1%}")
    print(f"distinct crashes (buggy ver): {result.distinct_signatures}")
    if result.successful_results:
        print("successful harnesses:")
        for br, sig in zip(result.successful_results,
                           result.accepted_signatures):
            tag = f"  [crash: {sig}]" if sig else ""
            print(f"  - {br.harness_path}{tag}")
    print("#" * 50)


def _print_fuzz_summary(fuzz_results) -> None:
    triggered = [r for r in fuzz_results if r.triggered]
    clean     = [r for r in fuzz_results if not r.triggered and not r.timed_out]
    timeouts  = [r for r in fuzz_results if r.timed_out]

    print("\n" + "#" * 20 + " fuzz summary " + "#" * 20)
    print(f"harnesses run  : {len(fuzz_results)}")
    print(f"crashed        : {len(triggered)}  "
          "(vulnerability still reachable — patch may be overfitting)")
    print(f"clean          : {len(clean)}  "
          "(vulnerability not triggered — patch appears to fix the bug)")
    print(f"timed out      : {len(timeouts)}")
    if triggered:
        print("crashing harnesses:")
        for r in triggered:
            print(f"  - {r.harness_path}")
    print("#" * 50)


if __name__ == '__main__':
    main()