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
from crash_input import CrashInputExtractor
from failure_test import FailureTestExtractor, is_crashing_bug
from fuzz_runner import FuzzRunner, HarnessVerifier
from jazzer import JazzerEnvironment
from llm import HarnessGenerator
from patches import DeprecatedBugError, PatchSelector
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
    parser.add_argument("--max_repair_failures", type=int, default=3,
                        help="maximum number of failures in a row before resetting the prompt context")
    parser.add_argument("--reachable_node_cap", type=int, default=None,
                        metavar="N",
                        help="budget for the root-cause reachable-set BFS: "
                             "max functions to visit (default: config."
                             "REACHABLE_NODE_CAP). Higher = wider neighbourhood "
                             "but slower analysis.")
    parser.add_argument("--reachable_max_depth", type=int, default=None,
                        metavar="D",
                        help="max call-graph depth for the reachable-set BFS "
                             "(default: config.REACHABLE_MAX_DEPTH). Direct "
                             "callees are depth 1.")
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
    parser.add_argument("--results_json", type=str, default=None,
                        metavar="PATH",
                        help="append a one-line JSON record describing this "
                             "run's outcome to PATH (machine-readable; used "
                             "by the batch evaluation harness)")
    parser.set_defaults(require_trigger=True)
    return parser.parse_args()


def _emit_record(path, *, label, status, selection=None,
                 result=None, fuzz_results=None, bug_kind=None):
    """Append one JSON line summarising this run. `label` is the
    ground-truth class ('correct' or 'overfitting'); `status` is one of
    'evaluated', 'non_crashing', 'no_harnesses', 'error'. A run is only
    scoreable when status == 'evaluated'. `bug_kind` ('crashing' /
    'semantic' / None) lets the aggregator score the two oracle types
    separately — their recall ceilings differ, so blending them hides
    which oracle is working."""
    if not path:
        return
    import json as _json
    rec = {
        "label": label,
        "status": status,
        "bug_kind": bug_kind,
        "project": getattr(selection, "project_name", None),
        "bug_id": getattr(selection, "bug_id", None),
        "apr_tool": getattr(selection, "apr_tool", None),
        "converged": bool(getattr(result, "converged", False)),
        "harnesses_built": len(getattr(result, "successful_results", []) or []),
        "harnesses_run": 0,
        "harnesses_crashed": 0,
        # crashed_on_patch: did ANY harness still crash the patched code?
        # This is the classifier's positive signal ("flagged overfitting").
        "crashed_on_patch": False,
    }
    if fuzz_results is not None:
        triggered = [r for r in fuzz_results if r.triggered]
        rec["harnesses_run"] = len(fuzz_results)
        rec["harnesses_crashed"] = len(triggered)
        rec["crashed_on_patch"] = len(triggered) > 0
    with open(path, "a") as fh:
        fh.write(_json.dumps(rec) + "\n")


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
    #    version.  Retry sampling if we land on a deprecated bug (defects4j
    #    refuses to check it out) so we don't propagate an unhandled error.
    selector = PatchSelector(
        project_name=args.project_name,
        correct=args.correct,
        overfitting=args.overfitting,
    )
    while True:
        try:
            selection = selector.select()
            break
        except DeprecatedBugError as exc:
            print(f"  skipping deprecated bug: {exc}")

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

    # 3a-bis) Classify the bug. Crashing bugs fail their trigger test with a
    #     thrown application exception; semantic bugs fail a JUnit assertion
    #     (wrong value, no throw). Both are now in scope — they differ only in
    #     the oracle the harness is built around (see prompt_factory below and
    #     the semantic path in PromptBuilder). If we couldn't determine any
    #     exception type, is_crashing_bug is conservatively False, so such
    #     bugs take the semantic (assertion-lifting) path.
    bug_kind = "crashing" if is_crashing_bug(failure_tests) else "semantic"
    if not failure_tests:
        # With no trigger test at all there is nothing to lift or anchor on;
        # neither oracle can be built. Keep skipping these.
        print(f"\n{selection.project_name} {selection.bug_id} "
              f"({selection.apr_tool}) has no bug-triggering tests — "
              "no oracle can be built. Skipping.")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status='non_crashing', selection=selection)
        sys.exit(3)
    print(f"\nbug kind: {bug_kind}")

    # 3b) Extract the patch + every project function it touches +
    #     cross-references for each of those functions.
    context = TargetAnalyzer(
        reachable_node_cap=args.reachable_node_cap,
        reachable_max_depth=args.reachable_max_depth,
    ).analyze(
        patch_path=selection.patch_path,
        buggy_dir=selection.buggy_dir,
    )
    print(json.dumps(context.as_dict(), indent=2))

    # 3c) Capture the GROUND-TRUTH crashing input by running the trigger
    #     test against the buggy checkout and reading the value back out
    #     of the failure output. This removes the model's need to guess
    #     which of (possibly many) test inputs actually crashes — the
    #     single biggest cause of wasted attempts. Bug-type-agnostic and
    #     purely additive: on any capture failure this is None and the
    #     prompt falls back to test-source-only behaviour.
    #
    #     Crashing bugs only: a semantic bug throws nothing, so there is no
    #     crashing value to read back. The semantic path lifts its anchor
    #     (the expected value) straight from the trigger test source instead.
    crash_input = None
    primary_test = next((ft for ft in failure_tests if ft.has_source),
                        failure_tests[0] if failure_tests else None)
    if bug_kind == "crashing" and primary_test is not None:
        # Fallback anchors mined from the test source, scoped to the
        # patched (target) methods — used only when the runtime message
        # does not itself echo the crashing value.
        candidate_literals = []
        if primary_test.method_source:
            candidate_literals = PromptBuilder.candidate_anchor_literals(
                primary_test.method_source,
                [fn.func_name for fn in context.functions],
            )
        crash_input = CrashInputExtractor().extract(
            buggy_dir=selection.buggy_dir,
            test_class=primary_test.test_class,
            test_method=primary_test.test_method,
            candidate_literals=candidate_literals,
        )
    _print_crash_input(crash_input)

    # 4) Build the chat-completion prompt. Rather than a single fixed
    #    prompt, we wrap PromptBuilder in a factory the campaign calls
    #    before each fresh attempt: it injects which reachable functions
    #    and crashes the set already covers, so each new harness is a
    #    *variant* steered at the uncovered part of the root-cause region.
    #
    #    For semantic bugs with several trigger tests, we additionally
    #    round-robin which test each attempt lifts its assertion from, so the
    #    harness set spreads across all of the bug's failing behaviours rather
    #    than piling onto the first. The campaign passes no attempt index, so
    #    the closure keeps its own counter (one tick per fresh prompt build).
    prompt_builder = PromptBuilder(language=args.language)
    _rr_state = {"i": 0}

    def prompt_factory(covered_functions, found_signatures):
        semantic_test = None
        if bug_kind == "semantic" and failure_tests:
            semantic_test = failure_tests[_rr_state["i"] % len(failure_tests)]
            _rr_state["i"] += 1
            print(f"  [semantic] lifting assertion from "
                  f"{semantic_test.test_class}::{semantic_test.test_method}")
        return prompt_builder.build(
            buggy_dir=selection.buggy_dir,
            context=context,
            failure_tests=failure_tests,
            covered_functions=covered_functions,
            found_signatures=found_signatures,
            crash_input=crash_input,
            bug_kind=bug_kind,
            semantic_test=semantic_test,
        )

    # The set-empty prompt used for attempt 1.
    messages = prompt_factory([], [])

    # 5) Run the campaign: regenerate, recompile, and (by default) verify
    #    each compiled harness crashes the BUGGY version before accepting
    #    it. Acceptance = "compiles AND triggers".
    builder = HarnessBuilder(jazzer_api_jar=jazzer_api_jar)

    # Throwable names this bug raises, gathered from D4J root-cause metadata
    # and the captured runtime crash. Both fully-qualified and simple names
    # are included so detection matches whichever form Jazzer prints. Used by
    # both the in-campaign verifier (buggy code) and the post-campaign
    # FuzzRunner (patched code) so crash detection is symmetric — otherwise a
    # harness accepted as crashing could be wrongly reported clean on the
    # patched code, masking an overfitting patch.
    # Throwable names that count as "the harness fired", used by both the
    # in-campaign verifier (buggy code) and the post-campaign FuzzRunner
    # (patched code) so detection is symmetric — otherwise a harness accepted
    # as triggering could be wrongly reported clean on the patched code,
    # masking an overfitting patch.
    #
    # The two bug kinds fire differently:
    #   * crashing — the bug's OWN throwable escapes the library. Expect the
    #     root-cause exception type (from D4J metadata + captured runtime
    #     crash), both fully-qualified and simple.
    #   * semantic — the library throws nothing; the HARNESS throws when the
    #     lifted assertion fails. Expect the throwable the harness raises
    #     (Jazzer's FuzzerSecurityIssue*, or a bare AssertionError if the
    #     model used assert/JUnit instead). Note Jazzer also flags an
    #     uncaught throwable via its finding exit code and crash markers, so
    #     this list mainly hardens the deterministic-first-input (rc=1) path.
    expected_exceptions = []
    if bug_kind == "crashing":
        for ft in failure_tests:
            if ft.exception_type:
                expected_exceptions.append(ft.exception_type)
                expected_exceptions.append(
                    ft.exception_type.rsplit('.', 1)[-1])
        if crash_input is not None and crash_input.exception_type:
            expected_exceptions.append(crash_input.exception_type)
            expected_exceptions.append(
                crash_input.exception_type.rsplit('.', 1)[-1])
    else:  # semantic
        expected_exceptions = [
            'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow',
            'com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium',
            'com.code_intelligence.jazzer.api.FuzzerSecurityIssueHigh',
            'FuzzerSecurityIssueLow',
            'FuzzerSecurityIssueMedium',
            'FuzzerSecurityIssueHigh',
            'java.lang.AssertionError',
            'AssertionError',
        ]
    seen = set()
    expected_exceptions = [e for e in expected_exceptions
                           if e and not (e in seen or seen.add(e))]

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
            expected_exceptions=expected_exceptions,
            jazzer_api_jar=jazzer_api_jar,
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
                          prompt_factory=prompt_factory,
                          patch_text=context.patch_text)

    _print_summary(selection, result)

    # 6) Fuzz every successful harness against the patched code to check
    #    whether the vulnerability is still reachable (overfitting signal).
    fuzz_results = None
    if args.fuzz_timeout > 0 and result.successful_results:
        print("\n" + "#" * 20 + " fuzzing patched code " + "#" * 20)
        try:
            fuzz_results = FuzzRunner(
                jazzer_standalone_jar=jazzer_standalone_jar,
                timeout_seconds=args.fuzz_timeout,
                expected_exceptions=expected_exceptions,
                jazzer_api_jar=jazzer_api_jar,
            ).run_all(
                successful_results=result.successful_results,
                patch_path=selection.patch_path,
                buggy_dir=selection.buggy_dir,
            )
            _print_fuzz_summary(fuzz_results)
        except Exception as exc:
            print(f"  patched-code fuzzing failed: {exc}")

    # A run is scoreable only if we actually fuzzed at least one harness
    # against the patched code; otherwise we have no overfitting verdict.
    if fuzz_results:
        status = 'evaluated'
    else:
        status = 'no_harnesses'
    _emit_record(args.results_json,
                 label='correct' if args.correct else 'overfitting',
                 status=status, selection=selection,
                 result=result, fuzz_results=fuzz_results,
                 bug_kind=bug_kind)

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


def _print_crash_input(crash_input) -> None:
    if crash_input is None or not crash_input.has_evidence:
        print("No ground-truth crash input captured — prompt will fall "
              "back to test-source anchoring.")
        return
    print("Captured ground-truth crash input:")
    if crash_input.exception_type:
        print(f"  throwable : {crash_input.exception_type}")
    if crash_input.message:
        print(f"  message   : {crash_input.message}")
    if crash_input.throw_site:
        print(f"  thrown_at : {crash_input.throw_site}")
    if crash_input.literals:
        print(f"  literals  : {crash_input.literals}")


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