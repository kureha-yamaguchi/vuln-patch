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
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config
from analysis import TargetAnalyzer
from build import HarnessBuilder
from campaign import HarnessCampaign, CampaignResult
from crash_input import CrashInputExtractor
from failure_test import FailureTestExtractor, classify_bug_kind, is_crashing_bug
from fuzz_runner import (FuzzRunner, HarnessVerifier, PatchApplyError,
                         PatchedProjectBuilder, TriggerVerificationError)
from jazzer import JazzerEnvironment
from llm import (HarnessGenerator, reset_token_usage, token_usage,
                 usage_totals)
from patches import DeprecatedBugError, PatchSelector
from prompts import PromptBuilder
from java_source import candidate_anchor_literals, expected_assert_literals



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
    parser.add_argument("--patch_file", type=str, default=None,
                        metavar="PATH",
                        help="evaluate exactly this .patch file instead of "
                             "randomly sampling one (project/apr_tool/"
                             "bug_id are all derived from its path); "
                             "--project_name is not needed when this is set")
    parser.add_argument("--skip_semantic", action="store_true",
                        help="bail out right after bug-kind classification "
                             "if the bug is semantic (no crash signature), "
                             "before the costly TargetAnalyzer/LLM campaign. "
                             "Default is to run semantic bugs too.")
    parser.add_argument("--language", type=str, nargs='?', default='Java',
                        help='Programming language of project')
    parser.add_argument("--model", type=str, default=None, metavar="DEPLOYMENT",
                        help="force a SINGLE model/deployment for harness "
                             "generation (disables two-tier escalation). "
                             "Without it, uses config.HARNESS_MODEL_PRIMARY and "
                             "escalates to HARNESS_MODEL_ESCALATION after "
                             "HARNESS_ESCALATE_AFTER attempts with no accepted "
                             "harness. E.g. --model gpt-5.4 to always use the "
                             "flagship.")
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
    parser.add_argument("--introspector_depth_cap", type=int, default=None,
                        metavar="D",
                        help="cap for fuzz-introspector's method-depth metric "
                             "(default: config.INTROSPECTOR_METHOD_DEPTH_CAP). "
                             "Bounds the otherwise-O(N^2) DFS that stalls on "
                             "large libraries; lower = faster parse.")
    parser.add_argument("--verify_relations", action="store_true",
                        help="before counting a harness that crashed the "
                             "patched code as overfitting evidence, ask an "
                             "LLM critic whether its oracle is SOUND (true for "
                             "any correct implementation). Drops unsound "
                             "findings (invented relations that fire on correct "
                             "code) — a non-cheating false-positive filter. "
                             "Off by default.")
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
    parser.add_argument("--mined_oracles", dest="mined_oracles",
                        action="store_true",
                        help="mine sibling test methods from the bug's own "
                             "test class as extra trusted oracles (semantic "
                             "bugs only). OFF by default: the gpt-5.4 A/B "
                             "measured mining neutral-to-negative (it cracked "
                             "no hard miss and a flood of mined assertions "
                             "regressed a previously-caught bug), so it is "
                             "opt-in, and capped at 10 total assertions when "
                             "on.")
    parser.add_argument("--no-mined-oracles", dest="mined_oracles",
                        action="store_false",
                        help="(kept for old batch scripts) explicitly disable "
                             "mining — already the default.")
    parser.add_argument("--synthesize_relations", action="store_true",
                        help="synthesize codebase-specific invariants/"
                             "metamorphic relations for the patched method "
                             "(semantic bugs), mechanically screen them on "
                             "the BUGGY build (relation_screen: drop "
                             "candidates that fire indiscriminately on "
                             "known-mostly-correct behaviour), and inject "
                             "only the survivors as screened candidates. "
                             "Targets overfits whose discriminating input is "
                             "in no test. Off by default (adds an LLM call + "
                             "screening builds). Synthesis always uses the "
                             "escalation/flagship model — proposing sound "
                             "relations is the hardest reasoning step in the "
                             "pipeline and the cheap model demonstrably "
                             "invents unsound ones.")
    parser.add_argument("--results_json", type=str, default=None,
                        metavar="PATH",
                        help="append a one-line JSON record describing this "
                             "run's outcome to PATH (machine-readable; used "
                             "by the batch evaluation harness)")
    parser.set_defaults(require_trigger=True, mined_oracles=False)
    return parser.parse_args()


def _emit_record(path, *, label, status, selection=None,
                 result=None, fuzz_results=None, bug_kind=None,
                 extras=None):
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
        # What fired on the buggy version per accepted harness (exception
        # headline). Lets the aggregator ask, per FN, "did the set only
        # ever trigger via the reported symptom?" — the masked-symptom
        # failure mode — without rerunning anything.
        "accepted_trigger_details": list(
            getattr(result, "accepted_trigger_details", []) or []),
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
    # Exact token spend for this run (all models: harness gen, escalation,
    # verifier, synthesis). Lets the aggregator sum real cost per batch.
    rec["tokens_total"] = usage_totals()
    rec["tokens_by_model"] = token_usage()
    # Free-form flags the caller wants queryable per run (e.g.
    # context_degraded when the touched-function extraction came up empty,
    # so an aggregator can tell "feature tested and failed" from "feature
    # never ran" without grepping logs).
    rec.update(extras or {})
    with open(path, "a") as fh:
        fh.write(_json.dumps(rec) + "\n")


def _print_token_usage():
    by_model = token_usage()
    if not by_model:
        return
    tot = usage_totals()
    print("\n" + "=" * 20 + " token usage " + "=" * 20)
    for model, u in by_model.items():
        print(f"  {model}: {u['calls']} calls, "
              f"{u['prompt_tokens']:,} in + {u['completion_tokens']:,} out "
              f"= {u['total_tokens']:,} tokens")
    print(f"  TOTAL: {tot['calls']} calls, {tot['total_tokens']:,} tokens "
          f"({tot['prompt_tokens']:,} in + {tot['completion_tokens']:,} out)")


def main():
    args = parse_args()
    # Token totals are process-global; start this patch's accounting from
    # zero so a future multi-patch-per-process driver can't accumulate.
    reset_token_usage()

    if not (args.correct or args.overfitting):
        print("Please select either --correct flag or --overfitting flag")
        sys.exit(1)

    # 1) When evaluating an explicit patch file, project_name/bug_id are
    #    fully determined by its path — no checkout needed to know them.
    #    Semantic bugs can be classified from defects4j's static root-cause
    #    metadata alone (`defects4j info -b`), so when we're going to
    #    discard semantic bugs anyway (--skip_semantic), check that here
    #    and bail before paying for the checkout, jazzer setup, or test
    #    source extraction below — a ~7s checkout for a bug we'd throw
    #    away regardless. (Random-sampling mode re-samples a fresh patch
    #    on checkout failure, so this shortcut is scoped to the
    #    deterministic --patch_file path. The full classification further
    #    down remains the source of truth and still runs for bugs that
    #    pass this gate — it also covers the "no trigger test at all"
    #    case this metadata-only check can't see.)
    if args.skip_semantic and args.patch_file:
        peek = PatchSelector.peek_patch_file(args.patch_file)
        if classify_bug_kind(peek.project_name, peek.bug_id) == "semantic":
            print(f"\n{peek.project_name} {peek.bug_id} "
                  f"({peek.apr_tool}) is a semantic bug (no crash "
                  "signature) — skipping before checkout.")
            _emit_record(args.results_json,
                         label='correct' if args.correct else 'overfitting',
                         status='semantic_skip', selection=peek,
                         bug_kind='semantic')
            sys.exit(4)

    # 2) Resolve Jazzer jars up front so failures surface before the slow
    #    checkout + LLM campaign. The standalone (driver) jar is needed
    #    both for the final patched-code run AND for the in-campaign
    #    trigger gate, so fetch it if either is active.
    jazzer_env = JazzerEnvironment()
    jazzer_api_jar = jazzer_env.ensure()
    needs_driver = args.fuzz_timeout > 0 or args.require_trigger
    jazzer_standalone_jar = (jazzer_env.ensure_driver()
                             if needs_driver else None)

    # 3) Pick a random patch and check out the corresponding buggy d4j
    #    version.  Retry sampling if we land on a deprecated bug (defects4j
    #    refuses to check it out) so we don't propagate an unhandled error.
    selector = PatchSelector(
        project_name=args.project_name,
        correct=args.correct,
        overfitting=args.overfitting,
        patch_file=args.patch_file,
    )
    while True:
        try:
            selection = selector.select()
            break
        except DeprecatedBugError as exc:
            print(f"  skipping deprecated bug: {exc}")

    # 4a) Extract the bug-triggering test(s) shipped with this d4j bug.
    #     They seed the prompt with a worked example of a crashing
    #     input — the LLM sees what values already drive the buggy code
    #     path and shapes its FuzzedDataProvider calls accordingly.
    failure_tests = FailureTestExtractor().extract(
        selection.buggy_dir,
        project_name=selection.project_name,
        bug_id=selection.bug_id,
    )
    _print_failure_tests(failure_tests)

    # 4a-bis) Classify the bug. Crashing bugs fail their trigger test with a
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

    # This pipeline only evaluates crashing bugs. Bail here, before the
    # costly TargetAnalyzer/LLM campaign, rather than after spending time
    # and API calls on a bug we're going to discard anyway.
    if args.skip_semantic and bug_kind != "crashing":
        print(f"\n{selection.project_name} {selection.bug_id} "
              f"({selection.apr_tool}) is a semantic bug (no crash "
              "signature) — skipping.")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status='semantic_skip', selection=selection,
                     bug_kind=bug_kind)
        sys.exit(4)

    # 4a-ter) P0.1 safety net, buggy half: the bug's own trigger tests must
    #     FAIL on the unpatched checkout before we spend a single LLM token
    #     on it. Lang-7 burned weeks because the bug's behavior didn't
    #     exist on our JVM and nothing ever said so. Costs one `defects4j
    #     test -t` per trigger test, cached per checkout.
    try:
        PatchedProjectBuilder().verify_bug_reproduces(selection.buggy_dir)
    except TriggerVerificationError as exc:
        print(f"\nSAFETY NET ({selection.project_name}-{selection.bug_id}): "
              f"{exc}")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status=exc.status, selection=selection,
                     bug_kind=bug_kind,
                     extras={'safety_net': str(exc)})
        sys.exit(5)

    # 4b) Extract the patch + every project function it touches +
    #     cross-references for each of those functions.
    context = TargetAnalyzer(
        reachable_node_cap=args.reachable_node_cap,
        reachable_max_depth=args.reachable_max_depth,
        introspector_depth_cap=args.introspector_depth_cap,
    ).analyze(
        patch_path=selection.patch_path,
        buggy_dir=selection.buggy_dir,
    )
    print(json.dumps(context.as_dict(), indent=2))

    # Empty touched-function extraction silently disables everything that
    # keys on the patched method — the function blocks in the prompt,
    # mining tokens, and relation synthesis (which returns [] without a
    # word when patched_sources is empty). A whole diagnostic leg once
    # "tested" synthesis that never ran because of this. Say it loudly and
    # stamp the record so the aggregator can exclude/flag the run instead
    # of misreading it as "feature ran and found nothing".
    context_degraded = not context.functions
    if context_degraded:
        print("\n" + "!" * 60)
        print("!! DEGRADED CONTEXT: no touched function could be extracted")
        print("!! from the patch (AST pass AND regex fallback both empty).")
        print("!! Prompt will lack function bodies; mining and relation")
        print("!! synthesis are disabled for this run.")
        print("!" * 60)
    record_extras = {"context_degraded": context_degraded}

    # Class-level codebase context for the LLM judgment stages: relation
    # synthesis, relation verification, and the 'consistency' harness slot
    # (one skeleton-aware harness per set). Task inspection showed the
    # discriminating invariant routinely lives OUTSIDE the patched method
    # (constructor invariants, complementary sibling functions, class
    # javadoc contracts), and the verifier's measured leaks were
    # domain-knowledge failures. Built once from the buggy checkout —
    # label-free. Assembled for every semantic bug (it is a cheap local
    # parse): gating it on the synthesis/verifier flags silently starved
    # the consistency slot in flag-off configs.
    class_ctx = []
    if bug_kind == "semantic" and not context_degraded:
        from code_context import assemble_class_context
        class_ctx = assemble_class_context(
            selection.buggy_dir,
            context.modified_files or [],
            [fn.func_name for fn in context.functions])
        if class_ctx:
            print(f"  [class-ctx] {len(class_ctx)} class skeleton(s), "
                  f"{sum(len(b) for b in class_ctx):,} chars")

    # 4c) Capture the GROUND-TRUTH crashing input by running the trigger
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
            candidate_literals = candidate_anchor_literals(
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

    # 4.5) Mine trusted sibling oracles (semantic bugs). The lifted-assertion
    #      oracle covers ONE trigger test; the same test class holds many more
    #      assertions on the patched method — sibling tests and the trigger
    #      test's other lines. Each is a developer-written literal the buggy
    #      code already passes, so a correct patch must too, while an overfit
    #      patch that special-cases the reported input fails a different one.
    #      Pure text mining (no compile, no model); injected into the prompt
    #      as extra trusted pairs. Same provenance as the lifted seed — uses
    #      the project's own tests, never the developer fix or the label.
    mined_oracles = []
    if bug_kind == "semantic" and args.mined_oracles:
        from test_oracle_miner import mine_sibling_tests
        # Read every trigger test's class source once.
        class_srcs, seen_src = [], set()
        for ft in failure_tests:
            path = getattr(ft, 'source_path', None)
            if not path or path in seen_src:
                continue
            seen_src.add(path)
            try:
                class_srcs.append(
                    open(path, encoding='utf-8', errors='replace').read())
            except OSError:
                continue
        # Prefer the patched METHOD names — precise when the method is public.
        # When the patch touches a PRIVATE helper (e.g. greatestCommonDivisor)
        # the tests exercise it only through the public API, so method-name
        # mining is empty; fall back to the patched CLASS name, which catches
        # the public-API tests that reach the helper. Over-inclusion is safe:
        # every mined test is still trusted (the buggy code passes it).
        method_tokens = [fn.func_name for fn in context.functions]
        class_tokens = sorted(set(re.findall(
            r'^\+\+\+\s+.*?/([A-Za-z_]\w*)\.java',
            context.patch_text or '', re.MULTILINE)))
        # The trigger tests are already lifted verbatim — don't re-mine them.
        exclude = [ft.test_method for ft in failure_tests if ft.test_method]
        used_tokens = method_tokens
        for tokens in (method_tokens, class_tokens):
            if not tokens:
                continue
            seen_names, batch, asserts_total = set(), [], 0
            for text in class_srcs:
                for t in mine_sibling_tests(text, tokens,
                                            exclude_methods=exclude):
                    # The per-call cap bounds each source file; re-apply
                    # the TOTAL cap across files so multi-file trigger
                    # classes can't reassemble the assertion flood the
                    # miner just prevented (36 injected assertions once
                    # regressed a caught bug to a miss).
                    if t.name in seen_names:
                        continue
                    if asserts_total + t.num_asserts > 10:
                        continue
                    seen_names.add(t.name)
                    asserts_total += t.num_asserts
                    batch.append(t)
            if batch:
                mined_oracles = batch
                used_tokens = tokens
                break
        if mined_oracles:
            names = ', '.join(f"{t.name}({t.num_asserts})"
                              for t in mined_oracles)
            print(f"  [mined-oracles] {len(mined_oracles)} sibling test "
                  f"method(s) on {', '.join(used_tokens) or 'target'}: {names}")

    # 4.6) Synthesize codebase-specific relation CANDIDATES (semantic bugs).
    #      Mining only covers TESTED inputs; an overfit can pass every test
    #      yet stay wrong on an untested input whose oracle exists in no
    #      test. Here we ask an LLM to propose invariants/metamorphic
    #      relations over the patched API, grounded in the diff and the
    #      touched methods' javadoc. Candidates are HYPOTHESES: they are
    #      mechanically screened on the buggy build (relation_screen, run
    #      after the builder exists — see step 6) and ONLY survivors ever
    #      reach a prompt. If screening cannot run, nothing is injected.
    # Documented contracts (javadoc) of the touched methods, extracted once
    # from the buggy sources. Consumed twice: relation synthesis grounds its
    # candidates in them, and the harness prompt's documented-preconditions
    # block feeds the valid-by-construction rule the actual @param/@throws
    # contract instead of making the generator guess it from test source.
    # Best-effort — empty on any miss (undocumented code), and every
    # consumer falls back cleanly.
    touched_javadocs = []
    if context.functions:
        from relation_synth import javadoc_for
        for rel in (context.modified_files or []):
            full = Path(selection.buggy_dir) / rel
            try:
                src_text = full.read_text(encoding='utf-8', errors='replace')
            except OSError:
                continue
            for fn in context.functions:
                jd = javadoc_for(src_text, fn.func_name)
                if jd and jd not in touched_javadocs:
                    touched_javadocs.append(jd)

    synthesized_relations = []
    if bug_kind == "semantic" and args.synthesize_relations:
        if context_degraded:
            print("  [synth] skipped: no touched function extracted "
                  "(context degraded — see warning above)")
        else:
            from relation_synth import RelationSynthesizer
            # Always the escalation/flagship model, regardless of the
            # harness-generation tier: proposing relations that must hold
            # for EVERY correct implementation is the hardest reasoning
            # step in the pipeline, and the nano batch showed the cheap
            # model invents unsound out-of-domain oracles. `--model X`
            # still wins so a forced single-model run stays single-model.
            synth_model = args.model or config.HARNESS_MODEL_ESCALATION
            patched_sources = [fn.func_source for fn in context.functions]
            _syn_cls = sorted(set(re.findall(
                r'^\+\+\+\s+.*?/([A-Za-z_]\w*)\.java',
                context.patch_text or '', re.MULTILINE)))
            class_name = _syn_cls[0] if _syn_cls else ''
            synthesizer = RelationSynthesizer(
                HarnessGenerator(model=synth_model,
                                 temperature=0.3, top_p=1.0))
            # P2.1: hand synthesis the bug's own failing test — the one
            # trusted source of the correct DIRECTION. Was '' for the whole
            # project history, so synthesis read only the buggy body and
            # inverted relations (Lang-7). Build from the primary test's
            # source plus the exact values it asserts.
            trigger_test_block = ''
            if primary_test is not None and primary_test.has_source:
                exp = expected_assert_literals(
                    primary_test.method_source or '')
                block = [f"// {primary_test.test_class}::"
                         f"{primary_test.test_method}",
                         primary_test.method_source or '']
                if exp:
                    block.append(
                        "// values this test asserts as CORRECT "
                        "(the patched code must reproduce these): "
                        + ", ".join(exp[:12]))
                trigger_test_block = "\n".join(block)
            candidates = synthesizer.synthesize(
                patched_sources, class_name,
                context.root_cause_reachable or [], mined_oracles, '',
                patch_text=context.patch_text or '',
                javadocs=touched_javadocs,
                class_context=class_ctx,
                source_imports=context.source_imports,
                trigger_test_block=trigger_test_block)
            if candidates:
                print(f"  [synth] {len(candidates)} candidate relation(s) "
                      f"({synth_model}): "
                      f"{', '.join(r.name for r in candidates)}")
            else:
                print("  [synth] WARNING: synthesis returned no candidates "
                      "(after one retry) — nothing to screen or inject")
            synthesized_relations = candidates
            record_extras["synth_candidates"] = len(candidates)

    # 5) Build the chat-completion prompt. Rather than a single fixed
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
    _rr_state = {"i": 0, "s": 0, "m": 0}
    # Rotate the variant strategy across the harness SET so each is tried by
    # at least one harness — otherwise the model picks one stochastically and
    # may never write, e.g., the consistency cross-check that catches a
    # masked-symptom bug. One tick per fresh prompt build.
    _STRATEGIES = ['a', 'b', 'c']

    def prompt_factory(covered_functions, found_signatures):
        semantic_test = None
        if bug_kind == "semantic" and failure_tests:
            semantic_test = failure_tests[_rr_state["i"] % len(failure_tests)]
            _rr_state["i"] += 1
            print(f"  [semantic] lifting assertion from "
                  f"{semantic_test.test_class}::{semantic_test.test_method}")
        strategy = _STRATEGIES[_rr_state["s"] % len(_STRATEGIES)]
        _rr_state["s"] += 1
        if context.root_cause_reachable:
            print(f"  [variant] assigned strategy ({strategy})")
        # Mechanism rotation (semantic): each harness carries the lifted
        # trigger block plus ONE extra oracle mechanism, instead of every
        # prompt stacking all of them — stacked blocks contradicted each
        # other and a flood of mined pairs once distracted the generator
        # off a bug it had been catching. Only mechanisms that actually
        # have content this run enter the rotation. NOTE: reads the
        # closure variables at call time, so it automatically sees the
        # post-screen `synthesized_relations`.
        mechanism = None
        if bug_kind == "semantic":
            mechs = ['consistency']
            if mined_oracles:
                mechs.insert(0, 'pairs')
            if synthesized_relations:
                mechs.append('relations')
            if len(mechs) > 1:
                mechanism = mechs[_rr_state["m"] % len(mechs)]
                _rr_state["m"] += 1
                print(f"  [mechanism] assigned ({mechanism})")
        return prompt_builder.build(
            buggy_dir=selection.buggy_dir,
            context=context,
            failure_tests=failure_tests,
            covered_functions=covered_functions,
            found_signatures=found_signatures,
            crash_input=crash_input,
            bug_kind=bug_kind,
            semantic_test=semantic_test,
            variant_strategy=strategy,
            # When the relation verifier will screen fired oracles (6b),
            # the prompt may push for strong, JUSTIFIED assertions instead
            # of hedging to vacuous ones (the verifier is a backstop, and
            # the prompt says so without overpromising).
            verifier_enabled=args.verify_relations,
            mined_oracles=mined_oracles,
            synthesized_relations=synthesized_relations,
            oracle_mechanism=mechanism,
            touched_javadocs=touched_javadocs,
            # Only the 'consistency' slot renders this (one
            # skeleton-aware harness per set); other slots stay lean.
            class_context=class_ctx,
        )

    # 6) Run the campaign: regenerate, recompile, and (by default) verify
    #    each compiled harness crashes the BUGGY version before accepting
    #    it. Acceptance = "compiles AND triggers".
    builder = HarnessBuilder(jazzer_api_jar=jazzer_api_jar)

    # 6a-pre) Mechanically screen the synthesized relation candidates on
    #    the BUGGY build (needs the builder + jazzer driver, hence here).
    #    Candidates that fire indiscriminately on known-mostly-correct
    #    behaviour are out-of-domain and dropped; only survivors reach the
    #    prompt factory (which reads this variable at call time — the
    #    initial prompt is deliberately built AFTER this point). No driver
    #    jar / screen failure => nothing is injected: unscreened candidates
    #    never reach a prompt under any circumstances.
    if synthesized_relations:
        if jazzer_standalone_jar:
            print("\n" + "#" * 20 + " relation screening " + "#" * 20)
            from relation_screen import screen_relations
            # P2.2 direction/determinism check needs the failing test's own
            # input literals (the values the bug is about). Same extraction
            # as the acceptance-gate seed corpus below.
            _trig_lits = []
            for ft in failure_tests:
                if getattr(ft, 'method_source', None):
                    _trig_lits += re.findall(
                        r'"((?:[^"\\]|\\.){1,120})"', ft.method_source)
            _trig_lits = [s for s in dict.fromkeys(_trig_lits)
                          if s.strip()][:32]
            try:
                synthesized_relations = screen_relations(
                    synthesized_relations,
                    builder=builder,
                    buggy_dir=selection.buggy_dir,
                    jazzer_standalone_jar=jazzer_standalone_jar,
                    package=context.package,
                    imports=context.source_imports,
                    jazzer_api_jar=jazzer_api_jar,
                    trigger_literals=_trig_lits,
                )
            except Exception as exc:
                print(f"  [screen] screening failed ({exc}) — dropping all "
                      "candidates rather than injecting unscreened")
                synthesized_relations = []
            print(f"  [screen] {len(synthesized_relations)} relation(s) "
                  "survived screening")
        else:
            print("  [screen] no jazzer driver available — dropping all "
                  "candidates rather than injecting unscreened")
            synthesized_relations = []
        record_extras["synth_survivors"] = len(synthesized_relations)

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

    # Seed corpus for the buggy-version trigger gate: string literals from
    # the trigger tests and mined siblings, written one per file. libFuzzer
    # starts from these instead of from nothing, so the gate's short budget
    # begins in the neighbourhood of known-valid inputs — the region an
    # overfit special-cased — rather than spending it discovering input
    # shape. Best-effort: no literals, no corpus, no behaviour change.
    # SEMANTIC BUGS ONLY: only the acceptance gate is seeded, never the
    # patched-build fuzz run, so for a crashing bug a harness accepted
    # only because a seed reached the trigger may fail to re-find it on
    # the patched build within budget — a TP lost at the second stage.
    # Semantic harnesses construct their inputs in code and are far less
    # corpus-dependent; the asymmetry is harmless there.
    corpus_dir = None
    seed_literals = []
    if bug_kind == "semantic":
        for ft in failure_tests:
            if getattr(ft, 'method_source', None):
                seed_literals += re.findall(
                    r'"((?:[^"\\]|\\.){1,120})"', ft.method_source)
        for t in mined_oracles:
            seed_literals += re.findall(
                r'"((?:[^"\\]|\\.){1,120})"', getattr(t, 'source', ''))
        seed_literals = [s for s in dict.fromkeys(seed_literals)
                         if s.strip()][:64]
    if seed_literals:
        corpus_path = Path(selection.buggy_dir) / 'fuzz' / 'corpus'
        try:
            corpus_path.mkdir(parents=True, exist_ok=True)
            for i, lit in enumerate(seed_literals):
                (corpus_path / f'seed_{i:03d}').write_text(
                    lit, encoding='utf-8', errors='replace')
            corpus_dir = str(corpus_path)
            print(f"  [corpus] seeded {len(seed_literals)} test literals "
                  f"into {corpus_dir}")
        except OSError as exc:
            print(f"  [corpus] seeding failed ({exc}); continuing without")

    verifier = None
    buggy_cp = None  # resolved lazily; also reused by the attribution check
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
            corpus_dir=corpus_dir,
        )

    # Two-tier generation: a cheap PRIMARY model, escalating to a stronger
    # ESCALATION model if the primary can't produce an accepted harness.
    # `--model X` forces a single model (primary == escalation == X).
    if args.model:
        primary_model = escalation_model = args.model
    else:
        primary_model = config.HARNESS_MODEL_PRIMARY
        escalation_model = config.HARNESS_MODEL_ESCALATION
    primary_gen = HarnessGenerator(model=primary_model,
                                   temperature=0.6, top_p=1.0)
    escalation_gen = None
    if escalation_model != primary_model and config.HARNESS_ESCALATE_AFTER > 0:
        escalation_gen = HarnessGenerator(model=escalation_model,
                                          temperature=0.6, top_p=1.0)
        print(f"  [model] primary={primary_model}, escalate to "
              f"{escalation_model} after {config.HARNESS_ESCALATE_AFTER} "
              "attempts with no accepted harness")
    else:
        print(f"  [model] {primary_model} (no escalation)")

    # The set-empty prompt used for attempt 1 — built HERE, after relation
    # screening, so the very first prompt already reflects the screened
    # (not raw) candidate set.
    messages = prompt_factory([], [])

    campaign = HarnessCampaign(
        generator=primary_gen,
        builder=builder,
        target_successes=args.target_successes,
        max_attempts=args.max_attempts,
        max_repair_failures=args.max_repair_failures,
        verifier=verifier,
        require_trigger=args.require_trigger,
        escalation_generator=escalation_gen,
        escalate_after=config.HARNESS_ESCALATE_AFTER,
    )
    result = campaign.run(messages, selection.buggy_dir,
                          prompt_factory=prompt_factory,
                          patch_text=context.patch_text)

    _print_summary(selection, result)

    # 6-0) P2.3: injected-but-not-implemented check. A screened relation
    #    handed to harness generation is worthless if no accepted harness
    #    actually contains it — the model can silently drop a relation it
    #    cannot implement (Math-2's convicting mean-relation needed a
    #    forbidden subclass). Diff the injected relation names against the
    #    accepted harness sources and log any that never made it in.
    if synthesized_relations and result.successful_results:
        accepted_src = ''
        for br in result.successful_results:
            try:
                with open(br.harness_path, encoding='utf-8',
                          errors='replace') as fh:
                    accepted_src += fh.read() + '\n'
            except OSError:
                pass
        missing = [getattr(r, 'name', '?') for r in synthesized_relations
                   if getattr(r, 'name', '') and
                   getattr(r, 'name') not in accepted_src]
        if missing:
            print(f"  [synth] INJECTED-BUT-NOT-IMPLEMENTED: {missing} — "
                  f"screened relation(s) that no accepted harness contains "
                  f"(the model dropped them; may be unimplementable under "
                  f"the harness rules)")
            record_extras['relations_not_implemented'] = missing

    # 6-bis) P0.4 latent-oracle scan: acceptance is whole-harness ("did
    #    ANYTHING fire on buggy?"), so a check listed after an
    #    always-firing one may never run at all — and then meets its
    #    first-ever execution on the patched build, where a false alarm
    #    from it has zero evidence behind it (Chart-26 attempt_003).
    #    Re-fuzz each accepted harness on the BUGGY build with keep_going
    #    and record which named oracles ever fire; the rest are LATENT.
    #    v1: flag loudly + hand to the verifier as context. No cutting.
    latent_map: dict = {}
    if result.successful_results and jazzer_standalone_jar:
        from java_source import oracle_ids_in_text
        _fr_lat = FuzzRunner(
            jazzer_standalone_jar=jazzer_standalone_jar,
            timeout_seconds=args.fuzz_timeout,
            expected_exceptions=expected_exceptions,
            jazzer_api_jar=jazzer_api_jar,
        )
        if buggy_cp is None:
            buggy_cp = builder.test_classpath(selection.buggy_dir)
        print("\n" + "#" * 20 + " latent-oracle scan (buggy) " + "#" * 20)
        for br in result.successful_results:
            try:
                with open(br.harness_path, encoding='utf-8',
                          errors='replace') as fh:
                    declared = oracle_ids_in_text(fh.read())
            except OSError:
                declared = set()
            if not declared:
                print(f"  {br.attempt_label or br.class_name}: no named "
                      f"oracles found in source — cannot scan")
                continue
            out = _fr_lat.keep_going_output(
                br.harness_path, br.class_name, buggy_cp)
            if not out:
                print(f"  {br.attempt_label or br.class_name}: scan run "
                      f"failed — no latent information")
                continue
            fired = oracle_ids_in_text(out)
            latent = declared - fired
            latent_map[br.harness_path] = latent
            if latent:
                print(f"  {br.attempt_label or br.class_name}: LATENT "
                      f"oracle(s) never fired on buggy: "
                      f"{sorted(latent)} (fired: {sorted(fired & declared)})"
                      f" — a patched-build firing from these has no "
                      f"buggy-side evidence behind it")
            else:
                print(f"  {br.attempt_label or br.class_name}: all "
                      f"{len(declared)} named oracle(s) exercised on buggy")
        record_extras['latent_oracles'] = {
            os.path.basename(k): sorted(v)
            for k, v in latent_map.items() if v}

    # 7) Fuzz every successful harness against the patched code to check
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
        except (PatchApplyError, TriggerVerificationError) as exc:
            # P0.1 safety net, patched half. These are NOT generic infra
            # hiccups: the program under test is not what we believe it
            # is. Recording 'no_harnesses' here is how do-nothing runs
            # got counted as passes for weeks — mark the run with its
            # specific unscoreable status and stop.
            status = getattr(exc, 'status', 'bad_patch')
            print(f"\nSAFETY NET: {exc}")
            _emit_record(args.results_json,
                         label='correct' if args.correct else 'overfitting',
                         status=status, selection=selection,
                         result=result, bug_kind=bug_kind,
                         extras={**(record_extras or {}),
                                 'safety_net': str(exc)})
            _print_token_usage()
            sys.exit(5)
        except Exception as exc:
            print(f"  patched-code fuzzing failed: {exc}")

    # 7b) Differential-firing ATTRIBUTION check — mechanical, label-free,
    #     and independent of the LLM verifier (which judges oracle
    #     SOUNDNESS; it cannot judge whether the patch CAUSED the firing —
    #     every verifier config kept the Lang-27-shaped FP because "a
    #     correct parser shouldn't expose internal crashes" is plausible
    #     and wrong about that codebase). Scoped by firing KIND:
    #     * Our own oracle throws (FuzzerSecurityIssue*, RuntimeException
    #       relation/consistency messages) firing on buggy are the TP
    #       signal — the patch failed to fix that family member. Never
    #       touched here; is_generic_escape() excludes them by class.
    #     * An escaped GENERIC JDK exception whose EXACT firing input
    #       reproduces the SAME crash signature on the buggy build is
    #       pre-existing crash surface, not patch-caused. Dropped, loudly
    #       — unless a keep_going re-fuzz shows a non-generic oracle also
    #       fires, in which case the finding stands on that oracle.
    #     Abstains (falls through to the verifier with a note) whenever
    #     the artifact is missing, the replay doesn't crash, or either
    #     signature lacks a stack-frame anchor ('Exc@Class.method') —
    #     a frame-less type-only match could equate two different crashes.
    #     SEMANTIC BUGS ONLY. For a crashing bug, "the same crash
    #     reproduces on the buggy build" is the TP condition itself —
    #     every accepted harness reproduces on buggy by construction
    #     (that's the acceptance gate), and an overfitting patch that
    #     fails to fix the crash fires with the identical signature on
    #     the patched build. Running this check there would read exactly
    #     that TP pattern as "pre-existing surface" and flip it to an FN.
    attribution_notes: dict = {}
    if (bug_kind == "semantic"
            and fuzz_results and any(r.triggered for r in fuzz_results)):
        from fuzz_runner import (cause_signature, crash_signature,
                                 is_generic_cause, is_generic_escape)
        from oracle_strength import exception_headline as _headline
        generic_hits = [
            r for r in fuzz_results if r.triggered
            and is_generic_escape(_headline((r.stdout or '') + '\n'
                                            + (r.stderr or '')))
        ]
        # P0.3: LAUNDERED firings — the headline is a harness-own alarm
        # (so the loop above skips it by design), but its `Caused by:`
        # chain bottoms out in a generic JDK escape. The alarm is just a
        # re-labelled library crash; whether it wraps or not is model
        # coin-flip, so without this check the same pre-existing crash is
        # dismissed one day and counted the next (the Chart-26 FP).
        laundered_hits = [
            r for r in fuzz_results if r.triggered
            and r not in generic_hits
            and is_generic_cause(cause_signature(
                (r.stdout or '') + '\n' + (r.stderr or '')))
        ]
        if generic_hits or laundered_hits:
            print("\n" + "#" * 20 + " attribution check " + "#" * 20)
            if buggy_cp is None:
                buggy_cp = builder.test_classpath(selection.buggy_dir)
            _fr = FuzzRunner(
                jazzer_standalone_jar=jazzer_standalone_jar,
                timeout_seconds=args.fuzz_timeout,
                expected_exceptions=expected_exceptions,
                jazzer_api_jar=jazzer_api_jar,
            )
            for r in generic_hits:
                out = (r.stdout or '') + '\n' + (r.stderr or '')
                patched_sig = crash_signature(out)
                if not r.artifact_path:
                    attribution_notes[id(r)] = (
                        "differential replay ABSTAINED: crashing input "
                        "artifact not captured")
                    print(f"  ? abstain (no artifact): {r.harness_path}")
                    continue
                if not patched_sig or '@' not in patched_sig:
                    attribution_notes[id(r)] = (
                        "differential replay ABSTAINED: patched-build crash "
                        "signature has no stack-frame anchor")
                    print(f"  ? abstain (frame-less signature): "
                          f"{r.harness_path}")
                    continue
                buggy_sig = _fr.replay_input(
                    r.harness_path, r.class_name, buggy_cp, r.artifact_path)
                if buggy_sig != patched_sig or '@' not in (buggy_sig or ''):
                    attribution_notes[id(r)] = (
                        f"differential replay: the exact firing input does "
                        f"NOT reproduce this crash on the buggy build "
                        f"(patched={patched_sig}, buggy="
                        f"{buggy_sig or 'no crash'}) — the crash is "
                        f"introduced by the patch")
                    print(f"  ✓ patch-caused ({patched_sig}): "
                          f"{r.harness_path}")
                    continue
                # Same generic crash on both builds. Before dropping, check
                # whether any NON-generic oracle in this harness also fires
                # on the patched code — a pre-existing crash must not bury
                # a genuine sibling detection.
                fired_all = _fr.collect_fired_oracles(
                    r.harness_path, r.class_name,
                    selection.patch_path, selection.buggy_dir)
                non_generic = [f for f in fired_all
                               if f and not is_generic_escape(f)]
                if non_generic:
                    attribution_notes[id(r)] = (
                        f"differential replay: generic firing {patched_sig} "
                        f"reproduces identically on the buggy build "
                        f"(pre-existing surface, ignore it), but non-generic "
                        f"oracle(s) also fire: {'; '.join(non_generic[:3])}")
                    print(f"  ✓ kept: {patched_sig} is pre-existing, but "
                          f"non-generic oracles also fire: "
                          f"{non_generic[0][:80]}")
                    continue
                r.triggered = False
                print(f"  [attribution] dropped: {patched_sig} reproduces "
                      f"on buggy build (pre-existing, not patch-caused): "
                      f"{r.harness_path}")
            for r in laundered_hits:
                out = (r.stdout or '') + '\n' + (r.stderr or '')
                patched_cause = cause_signature(out)
                if not r.artifact_path:
                    attribution_notes[id(r)] = (
                        "laundering check ABSTAINED: harness-own alarm "
                        f"wraps generic cause {patched_cause}, but no "
                        "crashing-input artifact was captured")
                    print(f"  ? abstain (no artifact): {r.harness_path}")
                    continue
                if not patched_cause or '@' not in patched_cause:
                    attribution_notes[id(r)] = (
                        "laundering check ABSTAINED: wrapped cause "
                        f"'{patched_cause}' has no stack-frame anchor")
                    print(f"  ? abstain (frame-less cause): "
                          f"{r.harness_path}")
                    continue
                buggy_sig, buggy_cause = _fr.replay_input_signatures(
                    r.harness_path, r.class_name, buggy_cp, r.artifact_path)
                # Pre-existing iff the same underlying crash appears on the
                # unpatched build — either escaping directly (headline) or
                # wrapped by the same alarm (cause). A DIFFERENT crash site
                # on buggy (e.g. the bug's own NPE) is the TP pattern and
                # must survive: that is exactly Chart-26's overfit side.
                if patched_cause in (buggy_sig, buggy_cause):
                    r.triggered = False
                    print(f"  [attribution] dropped LAUNDERED firing: "
                          f"harness alarm wraps {patched_cause}, which "
                          f"reproduces on the buggy build (pre-existing "
                          f"library surface): {r.harness_path}")
                else:
                    attribution_notes[id(r)] = (
                        f"laundering check: alarm wraps generic cause "
                        f"{patched_cause}; buggy-build replay gives "
                        f"headline={buggy_sig or 'no crash'}, cause="
                        f"{buggy_cause or 'none'} — NOT the same "
                        f"pre-existing crash")
                    print(f"  ✓ kept (cause not pre-existing): "
                          f"{r.harness_path}")

    # 6b) [optional] Relation verification — a non-cheating FP filter. A
    #     harness that crashed the patched code is only evidence of
    #     overfitting if its ORACLE is sound (true for any correct impl).
    #     Ask an LLM critic; drop findings whose oracle is judged unsound
    #     (invented relations that also fire on correct code). Uses only the
    #     harness source, never the developer fix.
    if args.verify_relations and fuzz_results:
        triggered = [r for r in fuzz_results if r.triggered]
        if triggered:
            print("\n" + "#" * 20 + " relation verification " + "#" * 20)
            from relation_verifier import RelationVerifier
            from oracle_strength import exception_headline, crash_excerpt
            # Thread the run's model explicitly: the default
            # HarnessGenerator resolves from .env, and a stale deployment
            # there once 404'd EVERY verify call — the verifier then
            # fail-opened on all of them and the whole stage silently
            # became a no-op that "kept" everything. Same tier logic as
            # synthesis: judging soundness is flagship work.
            verifier_model = args.model or config.HARNESS_MODEL_ESCALATION
            print(f"  [verifier] model={verifier_model}, "
                  f"votes={config.RELATION_VERIFIER_VOTES}")
            rv = RelationVerifier(
                HarnessGenerator(model=verifier_model,
                                 temperature=0.0, top_p=1.0),
                votes=config.RELATION_VERIFIER_VOTES)
            fr = FuzzRunner(
                jazzer_standalone_jar=jazzer_standalone_jar,
                timeout_seconds=args.fuzz_timeout,
                expected_exceptions=expected_exceptions,
                jazzer_api_jar=jazzer_api_jar,
            )
            # EXPECTED values lifted from the trigger tests' own equality
            # assertions (assertEquals first-arg literals). An assertion
            # that fires by disagreeing with one of these is checking
            # GROUND TRUTH (the correct code is known to produce these
            # values), so the verifier must not reject it as an over-tight
            # speculative relation. NOT the input literals — feeding inputs
            # into this channel made the short-circuit protect the wrong
            # thing. Bug-agnostic: empty on any extraction miss, which just
            # falls back to plain per-oracle review.
            trusted_values = []
            for ft in failure_tests:
                if getattr(ft, 'method_source', None):
                    trusted_values.extend(
                        expected_assert_literals(
                            ft.method_source))
            trusted_values = list(dict.fromkeys(trusted_values))
            for r in triggered:
                try:
                    with open(r.harness_path) as fh:
                        src = fh.read()
                except OSError:
                    continue
                # Collect EVERY oracle that fires on the patched code, not
                # just the first Jazzer surfaced — a multi-oracle harness
                # can fire via a sound oracle on one input and an unsound
                # one on another, and judging only the surfaced firing would
                # let the unsound sibling sink the finding. Re-fuzz with
                # --keep_going, and ALWAYS union in the originally captured
                # headline: the re-fuzz is nondeterministic and may surface
                # a different oracle set, and the original firing must never
                # drop out of the judged list just because it didn't
                # re-fire.
                single = exception_headline(
                    (r.stdout or '') + '\n' + (r.stderr or ''))
                fired_all = fr.collect_fired_oracles(
                    r.harness_path, r.class_name,
                    selection.patch_path, selection.buggy_dir)
                if single and single not in fired_all:
                    fired_all.append(single)
                if not fired_all:
                    fired_all = [None]
                # Concrete evidence of the ORIGINAL firing (exception line
                # + stack). Passed only when judging the oracle it belongs
                # to — evidence from firing A must not colour the judgment
                # of oracle B.
                excerpt = crash_excerpt(
                    (r.stdout or '') + '\n' + (r.stderr or ''))
                # The attribution check's differential outcome is hard
                # evidence about the original firing (does the exact input
                # reproduce on buggy?) — ride it along with the excerpt so
                # it reaches the critic under the same per-oracle gating.
                _attr_note = attribution_notes.get(id(r))
                if _attr_note:
                    excerpt = (excerpt + "\n[differential replay] "
                               + _attr_note).strip()
                # KEEP the finding if ANY fired oracle is sound or trusted —
                # one sound firing is sufficient proof the patch is wrong.
                # DROP only if every fired oracle is judged unsound.
                kept_reason = None
                drop_reasons = []
                for fired in fired_all:
                    evid = (excerpt if excerpt and fired
                            and fired[:40] in excerpt else None)
                    # P0.4: a firing from an oracle that never fired on
                    # the buggy build is running for the first time ever
                    # — the verifier should know it has no buggy-side
                    # evidence behind it.
                    from java_source import oracle_ids_in_text as _oids
                    _latent_here = (latent_map.get(r.harness_path) or set())
                    _fired_ids = _oids(fired or '')
                    if _fired_ids & _latent_here:
                        _note = ("[latent oracle] check(s) "
                                 + ", ".join(sorted(_fired_ids
                                                    & _latent_here))
                                 + " NEVER fired on the buggy build at "
                                 "acceptance — this patched-build firing "
                                 "is their first-ever execution; there is "
                                 "no buggy-side evidence behind them.")
                        evid = (evid + "\n" + _note) if evid else _note
                    ok, why = rv.verify(src, fired_assertion=fired,
                                        trusted_values=trusted_values,
                                        concrete_evidence=evid,
                                        code_context=('\n\n'.join(class_ctx)
                                                      if class_ctx else None))
                    if ok:
                        kept_reason = (fired, why)
                        break
                    drop_reasons.append((fired, why))
                if kept_reason is not None:
                    print(f"  ✓ sound: {r.harness_path}")
                    print(f"      kept via: {kept_reason[0]}")
                    print(f"      {kept_reason[1]}")
                else:
                    print(f"  ✗ dropped (all {len(drop_reasons)} fired "
                          f"oracles unsound): {r.harness_path}")
                    for fired, why in drop_reasons:
                        print(f"      fired: {fired}")
                        print(f"        {why}")
                    r.triggered = False  # no longer counts as a finding

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
                 bug_kind=bug_kind, extras=record_extras)
    _print_token_usage()

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