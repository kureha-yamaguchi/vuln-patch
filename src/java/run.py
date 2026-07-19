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
                 usage_totals, enable_recording, reset_events, get_events,
                 record_event)
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
                             "invents unsound ones. "
                             "Run WITH --replay_relations_on_patched: rule "
                             "INJECTION into harness prompts is not a "
                             "contributor (2026-07-19 ablation, 5 bugs, "
                             "identical recall/precision — consistent with "
                             "p23gate), but rule REPLAY is: full30's "
                             "result.jsonl records verifier-kept replay "
                             "convictions on 5 of 8 caught overfits, and "
                             "Math-2-o is caught ONLY this way (fuzzed-tier "
                             "replay; the overfit passes the trigger scenario "
                             "by construction, so no lifted test can see it). "
                             "The 2026-07-19 'not a contributor' conclusion "
                             "was measured with replay accidentally OFF in "
                             "both ablation arms and is retracted — see the "
                             "CORRECTION section in "
                             "semantic-recall-brainstorm.md.")
    parser.add_argument("--replay_relations_on_patched", action="store_true",
                        help="P3.2 replay: execute every screened relation "
                             "(own + pooled) directly against the PATCHED "
                             "build — trigger-literal replay (deterministic) "
                             "plus a fuzzed pass — and hand firings to the "
                             "relation verifier as candidate findings. "
                             "Removes the two coin flips (harness must "
                             "implement the relation AND fuzzing must find "
                             "the inputs) that cost Math-2-o its verdict. "
                             "Requires --synthesize_relations and "
                             "--verify_relations (replay never convicts "
                             "without the verifier).")
    parser.add_argument("--screen_runs", type=int, default=20000,
                        help="fuzz iterations per candidate during relation "
                             "screening AND patched-side replay. Default "
                             "20000. Drop to ~5000 for the cheap rule-gen "
                             "iteration loop (faster, slightly noisier "
                             "fire-ratio); keep 20000 for a measurement that "
                             "is compared apples-to-apples.")
    parser.add_argument("--synth_max_rules", type=int, default=4,
                        help="how many candidate relations synthesis may "
                             "propose per leg (default 4). Raising it is a "
                             "numbers game against generation variance — more "
                             "draws, higher odds the discriminating relation "
                             "appears. Only the count changes; the guidance is "
                             "unchanged. Compare 4 vs 8 over MULTIPLE samples "
                             "(single-sample convict noise is +-1-2 legs).")
    parser.add_argument("--rule_compile_repair", action="store_true",
                        help="R1: on a rule candidate's compile failure, make "
                             "ONE model call to fix it before dropping it "
                             "(recovers fixable typos; ~22%% of candidates die "
                             "at compile today). Measured on/off in "
                             "--rulegen_only mode.")
    parser.add_argument("--rule_soundness_harden", action="store_true",
                        help="Soundness pass: probe each surviving relation "
                             "with real extreme/boundary values (canned "
                             "FuzzedDataProvider); if it fires there it may be "
                             "UNSOUND (asserts more than the contract "
                             "guarantees at an extreme, e.g. NaN-result implies "
                             "NaN-operand, which Inf+-Inf breaks). Ask the model "
                             "to repair it from the contract, accepting the "
                             "repair only if it still catches the bug and fires "
                             "on fewer extremes. Attacks false positives from "
                             "unsound rules. Measured on/off in --rulegen_only.")
    parser.add_argument("--rulegen_only", action="store_true",
                        help="RULE-GENERATION QUALITY MODE. Run synthesis + "
                             "screening (on buggy) + replay (on THIS leg's "
                             "patched build), emit the rule-gen metrics, and "
                             "STOP before harness generation and the judge. "
                             "The cheap iterate-on-rules loop: ~10-15k tokens "
                             "vs ~50-100k for a full leg. Requires "
                             "--synthesize_relations. Join a bug's -o and -c "
                             "records offline: convict = a relation fires on "
                             "the overfit-patch build and stays quiet on the "
                             "correct one; false-fire = fires on the correct "
                             "one.")
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


def _fmt_messages(messages):
    parts = []
    for m in messages or []:
        role = m.get('role', '?') if isinstance(m, dict) else '?'
        content = m.get('content', '') if isinstance(m, dict) else str(m)
        parts.append(f"**[{role}]**\n```\n{content}\n```")
    return "\n\n".join(parts)


def _llm_role(messages):
    """Label an LLM call by its stage, read from its system prompt."""
    sysmsg = ''
    for m in messages or []:
        if isinstance(m, dict) and m.get('role') == 'system':
            sysmsg = (m.get('content') or '').lower()
            break
    if 'software-verification expert' in sysmsg or 'propose relation' in sysmsg:
        return 'rule synthesis'
    if 'skeptical reviewer' in sysmsg or 'prove a java relation' in sysmsg:
        return 'rule soundness-repair'
    if 'jazzer fuzzing harness' in sysmsg or 'security engineer' in sysmsg:
        return 'harness generation'
    if 'failed to compile' in sysmsg or 'fix a java snippet' in sysmsg:
        return 'compile-repair'
    if 'verif' in sysmsg or 'judge' in sysmsg or 'dismiss' in sysmsg:
        return 'verifier / judge'
    return 'LLM'


# What each pipeline component/step in the trace is (only those that appear
# are shown). Keeps the sequential trace self-explanatory.
_STEP_LEGEND = [
    ("failing-tests-found",
     "the project's own tests that expose the bug (extracted from Defects4J)"),
    ("analysis (TargetAnalyzer)",
     "parses the patch and builds the code context the model reasons over — "
     "the touched method(s), their documented contract, the call-graph "
     "reachable set + sibling members (via fuzz-introspector), and imports"),
    ("rule synthesis (LLM)",
     "proposes candidate RELATIONS — invariants / metamorphic properties a "
     "correct implementation must satisfy — from the documented contract"),
    ("screen-fuzz-buggy",
     "compiles each candidate rule and fuzzes it many times on the BUGGY "
     "build; the output is checked/violated/fire-ratio"),
    ("screen",
     "the keep/drop decision for each rule (direction-confirmed, selective, "
     "silent, or dropped by a lint/compile/cap)"),
    ("rule soundness-repair (LLM)",
     "a skeptical reviewer that rewrites a rule which fired on extreme inputs "
     "if it is unsound (asserts more than the contract guarantees)"),
    ("soundness-harden",
     "the deterministic outcome of probing a rule with extreme values: "
     "no-fire / artifact-skip / model-KEEP / HARDENED"),
    ("screening-survivors",
     "the final set of rules kept — these are passed to replay / harness "
     "generation"),
    ("replay-on-patched",
     "(rulegen mode) runs each surviving rule directly on the patched build"),
    ("harness generation (LLM)",
     "writes a Jazzer fuzzing harness that embeds the surviving rules plus "
     "oracles lifted from the failing test"),
    ("harness-attempt",
     "accept/reject of one generated harness — ACCEPTED = it compiles AND "
     "crashes the BUGGY build (with the triggering input shown)"),
    ("patched-fuzz",
     "fuzzes an accepted harness against the PATCHED build — FIRED (with the "
     "input + mismatch) means the overfit was caught; quiet means it escaped"),
]


def _fmt_det_output(o):
    """Format a deterministic step's output: pretty-print structured values,
    fence long/multiline strings, bold short scalars."""
    if isinstance(o, (dict, list)):
        return ("\n```json\n"
                + json.dumps(o, indent=2, ensure_ascii=False, default=str)
                + "\n```")
    s = '' if o is None else str(o)
    if len(s) > 200 or '\n' in s:
        return "\n```\n" + s + "\n```"
    return f"**{s}**"


def _write_trace_md(path, bug, label, events, outcome=None):
    """ONE purely SEQUENTIAL markdown transcript: every deterministic step and
    every LLM call, in the exact order they happened. Each LLM step shows its
    full prompt (deduped: a repeat of an earlier prompt is noted, not
    reprinted) and full output; each deterministic step shows its method,
    target and output. Nothing is summarised out of order — the sequence IS
    the record."""
    n_llm = sum(1 for e in events if e.get('kind') == 'llm')
    L = [f"# Pipeline trace — {bug}\n"]
    L.append(f"**Patch label:** {label}  "
             f"*(the patch under analysis is a "
             f"{'known-OVERFIT' if 'over' in str(label).lower() else 'known-CORRECT'}"
             f" fix — the pipeline is not told this)*")
    if outcome is not None:
        L.append(f"\n**Outcome:** {outcome}")
    # Patch under analysis — pulled from the analysis event's output so it sits
    # up top for orientation (it is also inside step [1] in full).
    _patch = ''
    for e in events:
        out = e.get('output')
        if e.get('kind') != 'llm' and isinstance(out, dict) and out.get(
                'patch_text'):
            _patch = out['patch_text']
            break
    if _patch:
        L.append("\n**Patch under analysis:**\n```diff\n"
                 + _patch.strip() + "\n```")
    L.append(f"\n{len(events)} sequential steps — {n_llm} LLM calls, "
             f"{len(events) - n_llm} deterministic. Read top to bottom.\n")
    # Legend — describe only the step types that actually appear.
    present = set()
    for e in events:
        if e.get('kind') == 'llm':
            present.add(_llm_role(e.get('messages')) + ' (LLM)')
        else:
            present.add(str(e.get('method', '')))
    shown = [(n, d) for n, d in _STEP_LEGEND
             if n in present or n.split(' (')[0] in present]
    if shown:
        L.append("<details><summary>Legend — what each step is</summary>\n")
        for n, d in shown:
            L.append(f"- **{n}** — {d}")
        L.append("\n</details>\n")
    # Per-MESSAGE dedup: the harness-generation calls share a huge identical
    # system + instruction/context message and differ only in the tail (repair
    # feedback, updated coverage). So collapse any message already shown
    # verbatim in an earlier step, and print only the NEW messages of a call.
    seen_msg = {}
    for e in events:
        seq = e.get('seq')
        if e.get('kind') == 'llm':
            msgs = e.get('messages') or []
            L.append(f"\n---\n## [{seq}] 🧠 LLM call — **{_llm_role(msgs)}** "
                     f"— model `{e.get('model', '')}`")
            L.append("**Prompt:**\n")
            _new = 0
            for m in msgs:
                role = m.get('role', '?') if isinstance(m, dict) else '?'
                content = (m.get('content', '') if isinstance(m, dict)
                           else str(m)) or ''
                key = (role, content)
                if key in seen_msg:
                    L.append(f"- *[{role}] message: identical to step "
                             f"[{seen_msg[key]}] — not reprinted*")
                else:
                    seen_msg[key] = seq
                    _new += 1
                    L.append(f"**[{role}]**\n```\n{content}\n```")
            if _new == 0:
                L.append("*(every message identical to earlier steps)*")
            L.append("\n**Output:**\n```\n"
                     + str(e.get('output', '')).strip() + "\n```")
        else:
            det = {k: v for k, v in e.items()
                   if k not in ('seq', 'kind', 'method', 'target', 'output')}
            L.append(f"\n---\n## [{seq}] ⚙️ {e.get('method', '')}"
                     + (f" · `{e.get('target')}`" if e.get('target') else ''))
            L.append("**output:** " + _fmt_det_output(e.get('output')))
            for k, v in det.items():
                L.append(f"- {k}: {v}")
    with open(path, 'w', encoding='utf-8') as fh:
        fh.write("\n".join(L) + "\n")


def main():
    args = parse_args()
    # Token totals are process-global; start this patch's accounting from
    # zero so a future multi-patch-per-process driver can't accumulate.
    reset_token_usage()
    # Record EVERY pipeline event (LLM calls + deterministic decisions) so the
    # leg can dump a complete, ordered, auditable transcript.
    enable_recording()
    reset_events()

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
    record_event('deterministic', method='failing-tests-found',
                 output=[getattr(t, 'method_name', str(t))
                         for t in (failure_tests or [])])

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

    # H2: the safety net just ran every trigger test on the buggy build and
    # its failure message names the diverging observable AND the wrong
    # value the bug produces ('expected:<NaN> but was:<4.0>') — attach it
    # to each FailureTest so the harness prompt and the H3 acceptance gate
    # can use it instead of throwing it away.
    _trigger_msgs = PatchedProjectBuilder.trigger_failure_messages(
        selection.buggy_dir)
    for _ft in failure_tests:
        _ft.failure_message = _trigger_msgs.get(
            f'{_ft.test_class}::{_ft.test_method}')
    # H1: resolve the parts of the test class each trigger test actually
    # uses (setUp/@Before, helpers, constants, fixture files) — the
    # harness writer replicates the real scenario instead of improvising
    # the setup, which was the root of every setup-divergence failure.
    from failure_test import resolve_test_support
    for _ft in failure_tests:
        try:
            _ft.support_source = resolve_test_support(
                _ft, checkout_dir=selection.buggy_dir)
        except Exception as _exc:   # context is best-effort, never fatal
            print(f"  [test-support] {_ft.test_method}: resolution failed "
                  f"({_exc}) — prompt falls back to the bare method body")
    record_event(
        'deterministic', method='test-context (H1/H2)',
        output=[{'test': f'{t.test_class}::{t.test_method}',
                 'failure_message': t.failure_message,
                 'support_chars': len(t.support_source or '')}
                for t in failure_tests])

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
    try:
        record_event('deterministic', method='analysis (TargetAnalyzer)',
                     output=context.as_dict())
    except Exception:
        pass

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
    _all_candidates = []
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
            trigger_methods: list = []
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
                # P3.2b root-region anchoring: the methods/types the
                # failing test actually exercises. The discriminating
                # relation may constrain ONE of THESE even when the patch
                # edited a different method (Math-2's overflow surfaces in
                # getNumericalMean() though Arja edited elsewhere), so
                # feeding them widens the synthesis anchor past the
                # patch-touched region.
                src_t = primary_test.method_source or ''
                _JUNIT = {'assertEquals', 'assertTrue', 'assertFalse',
                          'assertNull', 'assertNotNull', 'assertSame',
                          'assertArrayEquals', 'assertThat', 'assertThrows',
                          'fail', 'expect', 'valueOf', 'toString', 'equals'}
                for name in re.findall(r'\.([a-zA-Z_]\w*)\s*\(', src_t):
                    if name not in _JUNIT and name not in trigger_methods:
                        trigger_methods.append(name)
                for ctor in re.findall(r'\bnew\s+([A-Z]\w*)\s*\(', src_t):
                    if ctor not in trigger_methods:
                        trigger_methods.append(ctor)
                trigger_methods = trigger_methods[:20]
            candidates = synthesizer.synthesize(
                patched_sources, class_name,
                context.root_cause_reachable or [], mined_oracles, '',
                patch_text=context.patch_text or '',
                javadocs=touched_javadocs,
                class_context=class_ctx,
                source_imports=context.source_imports,
                trigger_test_block=trigger_test_block,
                trigger_methods=trigger_methods,
                max_rules=getattr(args, 'synth_max_rules', 4))
            if candidates:
                print(f"  [synth] {len(candidates)} candidate relation(s) "
                      f"({synth_model}): "
                      f"{', '.join(r.name for r in candidates)}")
            else:
                print("  [synth] WARNING: synthesis returned no candidates "
                      "(after one retry) — nothing to screen or inject")
            # P3.2 pooling: add relations screened SOUND on this bug's
            # buggy build in earlier legs/runs. Synthesis is stochastic —
            # the mean-formula that convicts Math-2's overfit was proposed
            # only on the correct leg — so unioning the pool makes each
            # bug's best discriminators available on EVERY leg. The pool
            # carries no label or patch identity, only project+bug+text.
            from relation_pool import load_pool, merge_candidates
            pooled = load_pool(selection.project_name, selection.bug_id)
            if pooled:
                before = len(candidates)
                candidates = merge_candidates(candidates, pooled)
                added = len(candidates) - before
                if added:
                    print(f"  [pool] +{added} relation(s) from earlier legs "
                          f"of {selection.project_name}-{selection.bug_id} "
                          f"(re-screened here before use)")
            synthesized_relations = candidates
            record_extras["synth_candidates"] = len(candidates)
            # Keep references to EVERY candidate (pre-screen) so the trace can
            # dump the ones screening/hardening later drop, with the reason —
            # screen_relations attaches .screen_decision/.harden_decision to
            # these same objects.
            _all_candidates = list(candidates)

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
    # Relations actually shown to the harness generator. Filled after
    # screening (6a-pre): at most 2 of this leg's OWN relations, best-first.
    # Pooled sibling-leg relations never enter the prompt — injected pool
    # mass displaced the generator's own free-form checks in p23gate
    # (Lang-60-o lost the capacity oracle that convicted at baseline).
    prompt_relations = []
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
        # post-screen `prompt_relations`.
        mechanism = None
        if bug_kind == "semantic":
            mechs = ['consistency']
            if mined_oracles:
                mechs.insert(0, 'pairs')
            if prompt_relations:
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
            synthesized_relations=prompt_relations,
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
                # max_keep=8: the old cap of 3 sized the PROMPT; the prompt
                # is now sliced separately (prompt_relations, ≤2 own-leg),
                # so screening may keep more survivors for pool/replay use.
                # R1 compile-repair: one model call to fix a candidate that
                # fails to compile, before dropping it. On behind
                # --rule_compile_repair so it can be measured on/off.
                _repair = None
                if getattr(args, 'rule_compile_repair', False):
                    _imp = context.source_imports
                    _repair = (lambda rel, err:
                               synthesizer.repair_check(rel, err, imports=_imp))
                # Soundness hardening (--rule_soundness_harden): probe each
                # survivor with real extreme values; if it fires there, ask the
                # model to repair it from the contract, accepting only a repair
                # that still catches the bug and fires on fewer extremes.
                _harden = None
                if getattr(args, 'rule_soundness_harden', False):
                    _imp = context.source_imports
                    _harden = (lambda rel, extremes, n, n_ord, imports=_imp:
                               synthesizer.harden_for_soundness(
                                   rel, extremes, n, n_ord, imports=imports))
                synthesized_relations = screen_relations(
                    synthesized_relations,
                    builder=builder,
                    buggy_dir=selection.buggy_dir,
                    jazzer_standalone_jar=jazzer_standalone_jar,
                    package=context.package,
                    imports=context.source_imports,
                    jazzer_api_jar=jazzer_api_jar,
                    trigger_literals=_trig_lits,
                    max_keep=8,
                    repair_fn=_repair,
                    harden_fn=_harden,
                    runs=args.screen_runs,
                )
                record_event('deterministic', method='screening-survivors',
                             output={'kept': [getattr(r, 'name', '?')
                                              for r in synthesized_relations],
                                     'count': len(synthesized_relations)})
            except Exception as exc:
                print(f"  [screen] screening failed ({exc}) — dropping all "
                      "candidates rather than injecting unscreened")
                synthesized_relations = []
            print(f"  [screen] {len(synthesized_relations)} relation(s) "
                  "survived screening")
            # W1.1 (p23gate regression fix): the harness prompt sees at most
            # 2 relations, best-first (screening returns direction-confirmed
            # first), and ONLY this leg's own — pooled sibling-leg relations
            # are screening/replay material, never prompt material.
            prompt_relations = [
                r for r in synthesized_relations
                if not getattr(r, 'from_pool', False)
                and not getattr(r, 'screen_note', '').startswith(
                    'INVERTED-SUSPECT')][:2]
            if len(prompt_relations) != len(synthesized_relations):
                print(f"  [screen] prompt gets {len(prompt_relations)} "
                      f"relation(s) (own-leg, best-first); the other "
                      f"{len(synthesized_relations) - len(prompt_relations)} "
                      "stay screening/replay-only")
            # P3.2 pooling: persist this leg's screened survivors so the
            # bug's OTHER legs can reuse them. Sound-on-buggy is a
            # per-bug property, so this is label-free.
            if synthesized_relations:
                from relation_pool import save_relations
                n = save_relations(selection.project_name, selection.bug_id,
                                   synthesized_relations)
                if n:
                    print(f"  [pool] saved {n} screened relation(s) for "
                          f"{selection.project_name}-{selection.bug_id}")
        else:
            print("  [screen] no jazzer driver available — dropping all "
                  "candidates rather than injecting unscreened")
            synthesized_relations = []
        record_extras["synth_survivors"] = len(synthesized_relations)

    # RULE-GENERATION QUALITY MODE: replay the screened relations directly
    # against THIS leg's patched build, record what fired, and stop before
    # the expensive harness-generation + judge stages. Everything here
    # reuses the exact synthesis/screen/replay the full pipeline uses — the
    # only difference is we skip steps 6+. Metrics land in the record so an
    # offline join of a bug's -o and -c legs gives convict-recall and
    # false-fire per bug.
    if args.rulegen_only:
        replay_fired = []
        if (synthesized_relations and jazzer_standalone_jar
                and bug_kind == "semantic"):
            try:
                from relation_screen import replay_on_patched
                _pdir = PatchedProjectBuilder().build_patched_dir(
                    selection.buggy_dir, selection.patch_path)
                _tl = []
                for ft in failure_tests:
                    if getattr(ft, 'method_source', None):
                        _tl += re.findall(
                            r'"((?:[^"\\]|\\.){1,120})"', ft.method_source)
                _tl = [s for s in dict.fromkeys(_tl) if s.strip()][:32]
                _f = replay_on_patched(
                    synthesized_relations, builder=builder,
                    patched_dir=_pdir,
                    jazzer_standalone_jar=jazzer_standalone_jar,
                    package=context.package, imports=context.source_imports,
                    jazzer_api_jar=jazzer_api_jar, trigger_literals=_tl,
                    runs=args.screen_runs)
                replay_fired = [
                    {'name': x['name'], 'tier': x['tier'],
                     'note': x['note']} for x in _f]
            except (PatchApplyError, TriggerVerificationError) as exc:
                record_extras['rulegen_status'] = getattr(
                    exc, 'status', 'bad_patch')
            except Exception as exc:
                record_extras['rulegen_status'] = f'replay_error: {exc}'
        record_extras['rulegen_only'] = True
        record_extras['relation_replay_fired'] = replay_fired
        record_extras['screened_relation_names'] = [
            getattr(r, 'name', '?') for r in (synthesized_relations or [])]
        print(f"\n[rulegen] candidates={record_extras.get('synth_candidates',0)}"
              f" survivors={len(synthesized_relations or [])}"
              f" replay-fired={len(replay_fired)}: "
              f"{[x['name'] for x in replay_fired]}")
        # Full inspectable trace: the exact prompt + context the model saw,
        # and every surviving rule with its full body (name/kind/contract/
        # input/check/screen_note) — so a run.log's names can be read as
        # actual rules against the actual context.
        try:
            if args.results_json:
                _tp = os.path.join(os.path.dirname(args.results_json),
                                   'trace.md')
                _write_trace_md(
                    _tp, f"{selection.project_name}-{selection.bug_id}",
                    'correct' if args.correct else 'overfitting',
                    get_events(), outcome='rulegen_only (no harness/verdict)')
                print(f"  [trace] wrote {_tp} ({len(get_events())} steps)")
        except Exception as _e:
            print(f"  [trace] dump failed: {_e}")
        _emit_record(args.results_json,
                     label='correct' if args.correct else 'overfitting',
                     status='rulegen_only', selection=selection,
                     result=None, bug_kind=bug_kind, extras=record_extras)
        _print_token_usage()
        sys.exit(0)

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

    # H3: the wrong values the real trigger tests observe on the buggy
    # build (parsed from their captured failure messages). Semantic bugs
    # only — a crash-shaped failure has no expected/actual pair, and
    # real_wrong_values returns [] there, which disables the gate.
    from oracle_strength import real_wrong_values
    trigger_wrong_values = (
        real_wrong_values([ft.failure_message for ft in failure_tests])
        if bug_kind == 'semantic' else [])
    if trigger_wrong_values:
        print(f"  [H3] real wrong value(s) on buggy: {trigger_wrong_values}")

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
        trigger_wrong_values=trigger_wrong_values,
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
    # P3.3: per-harness map {oracle id -> exception types underlying its
    # BUGGY-side firings}, from the same keep_going scan. On the patched
    # build, a firing of the same oracle from a disjoint set of underlying
    # exception types is a DIFFERENT crash wearing the same alarm.
    buggy_crash_types: dict = {}
    if result.successful_results and jazzer_standalone_jar:
        from java_source import oracle_ids_in_text
        from fuzz_runner import per_oracle_crash_types
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
            _octypes = per_oracle_crash_types(out)
            if _octypes:
                buggy_crash_types[br.harness_path] = _octypes
                print(f"  {br.attempt_label or br.class_name}: buggy-side "
                      f"crash identity per oracle: "
                      + ", ".join(f"{k}={sorted(v)}"
                                  for k, v in sorted(_octypes.items())))
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
            for _fr in (fuzz_results or []):
                _fired = getattr(_fr, 'triggered', False)
                _kw = {}
                if _fired:
                    _blob = ((getattr(_fr, 'stderr', '') or '') + '\n'
                             + (getattr(_fr, 'stdout', '') or ''))
                    # The oracle message names the DISCRIMINATING INPUT and the
                    # mismatch, e.g. "x.add(new Complex(1, NaN)).getReal()
                    # expected NaN but got 4.0" — i.e. exactly what caught it.
                    _m = re.search(r'\[oracle:[^\]]*\][^\n]*', _blob)
                    _out = ('FIRED — ' + (_m.group(0)[:500] if _m
                                          else 'crash on patched build'))
                    # The raw reproducing input Jazzer persisted (the bytes the
                    # FuzzedDataProvider decoded into that triggering input).
                    _art = getattr(_fr, 'artifact_path', None)
                    if _art:
                        _kw['reproducing_input_file'] = _art
                else:
                    _out = 'quiet on patched build (no overfit signal)'
                record_event(
                    'deterministic', method='patched-fuzz',
                    target=getattr(_fr, 'attempt_label', 'harness'),
                    output=_out, **_kw)
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
        from java_source import oracle_ids_in_text as _oids_attr

        def _non_alarm_escape(h):
            # ANY escaped exception (not our alarm, no oracle ID) on a
            # SEMANTIC leg is differential-replay eligible, not just the
            # JDK generics: a semantic oracle is an alarm throw, so an
            # escaped library exception — including library validation
            # types like NotPositiveException — can only be an input
            # rejection or pre-existing surface. If its exact input
            # reproduces the same crash on buggy, it is not the patch's
            # fault. (minfix_w2b Math-2-c: a constructor
            # NotPositiveException on a junk fuzzed input was kept by the
            # verifier as a conviction of the CORRECT patch.)
            return bool(h) and 'FuzzerSecurityIssue' not in h \
                and not _oids_attr(h)
        generic_hits = [
            r for r in fuzz_results if r.triggered
            and (is_generic_escape(_headline((r.stdout or '') + '\n'
                                             + (r.stderr or '')))
                 or _non_alarm_escape(_headline((r.stdout or '') + '\n'
                                                + (r.stderr or ''))))
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
            # P4.3 reconciliation state: oracle IDs the verifier judged
            # unsound anywhere this run, and the findings it kept.
            _unsound_oracle_ids: dict = {}
            _unsound_scope: dict = {}      # oracle id -> harness paths
            _kept_findings: list = []
            # Names of INJECTED relations: the same name in two harnesses
            # is genuinely the same check (both implement the injected
            # relation), so a dismissal transfers. A model-invented ID
            # (e.g. `lifted-test`) can name DIFFERENT checks in different
            # harnesses — full30 lost the Closure-62-o catch when an
            # unsound verdict on one harness's `lifted-test` killed the
            # sound keep of another's — so those reconcile only within
            # the same harness.
            _injected_rel_names = {
                getattr(_rel, 'name', '')
                for _rel in (synthesized_relations or [])} - {''}
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
                _trigger_method_names = {
                    getattr(ft, 'test_method', '') for ft in failure_tests
                    if getattr(ft, 'test_method', '')}
                # P3.3: underlying exception types per oracle for the
                # ORIGINAL patched-side firing output.
                from fuzz_runner import per_oracle_crash_types as _poct
                _patched_types = _poct(
                    (r.stdout or '') + '\n' + (r.stderr or ''))
                _buggy_types = buggy_crash_types.get(r.harness_path) or {}
                for fired in fired_all:
                    evid = (excerpt if excerpt and fired
                            and fired[:40] in excerpt else None)
                    from java_source import oracle_ids_in_text as _oids
                    _latent_here = (latent_map.get(r.harness_path) or set())
                    _fired_ids = _oids(fired or '')
                    # P0.4 step 2, REVISED after minfix_w1: never dismiss on
                    # latency alone. The buggy-side scan stops at the first
                    # firing oracle per input, so a sound check behind an
                    # always-firing seed oracle looks latent although its
                    # first real chance to run comes exactly when the
                    # overfit silences the seed — a mechanical dismissal
                    # here killed the true Lang-60-o capacity catch.
                    # Instead: mechanically replay THIS firing's exact
                    # input on the BUGGY build and give the verifier the
                    # one fact latency can't provide.
                    _latent_note = None
                    if _fired_ids and _fired_ids <= _latent_here:
                        if buggy_cp is None:
                            buggy_cp = builder.test_classpath(
                                selection.buggy_dir)
                        _rep = None
                        if getattr(r, 'artifact_path', None):
                            _rep = fr.replay_input_oracles(
                                r.harness_path, r.class_name, buggy_cp,
                                r.artifact_path)
                        _base = ("[latent oracle] check(s) "
                                 + ", ".join(sorted(_fired_ids))
                                 + " never fired during the buggy-side "
                                 "acceptance scan (that scan stops at the "
                                 "first firing oracle per input, so this "
                                 "alone proves nothing). ")
                        if _rep is None:
                            _latent_note = (
                                _base + "Replay of this firing's input on "
                                "the buggy build was unavailable — judge "
                                "on soundness alone, sceptically.")
                        elif _fired_ids & _rep:
                            _latent_note = (
                                _base + "Mechanical replay: this firing's "
                                "EXACT input fires the SAME check on the "
                                "BUGGY build — the violated behaviour "
                                "exists on both builds, i.e. the patch "
                                "did not change it. If the violated "
                                "contract is the very behaviour this bug "
                                "report is about, the patch left the bug "
                                "unfixed and this finding is SOUND; if it "
                                "is an out-of-domain artifact unrelated "
                                "to the reported bug (undocumented edge "
                                "case, documented @throws territory), it "
                                "is UNSOUND.")
                            print(f"      [latent] replay: same check "
                                  f"fires on buggy for this input — "
                                  f"verifier decides")
                        else:
                            _latent_note = (
                                _base + "Mechanical replay: this firing's "
                                "exact input does NOT fire this check on "
                                "the buggy build — the patch INTRODUCED "
                                "the violation; strong evidence against "
                                "the patch unless the check itself is "
                                "unsound.")
                            print(f"      [latent] replay: check quiet on "
                                  f"buggy for this input — "
                                  f"patch-introduced violation")
                    # P3.3 crash-site pinning (mechanical): the same alarm
                    # fired on both builds, but from DISJOINT underlying
                    # exception types — a different (pre-existing) crash
                    # wearing the alarm the buggy-side crash earned
                    # (Chart-26-c: the axis-label NPE on buggy vs the
                    # unrelated text-measuring crash on patched). Compared
                    # at TYPE level only, so a half-fix that moves the same
                    # exception to a nearby frame stays a catch. Applies
                    # only when BOTH sides carry identity — a value-mismatch
                    # alarm has no underlying exception and is untouched.
                    _pin_mismatch = None
                    for _oid in _fired_ids:
                        _bt = _buggy_types.get(_oid) or set()
                        _pt = _patched_types.get(_oid) or set()
                        if _bt and _pt and not (_bt & _pt):
                            _pin_mismatch = (_oid, _bt, _pt)
                            break
                    if _pin_mismatch:
                        _oid, _bt, _pt = _pin_mismatch
                        _why = ("CRASH-PIN-DISMISSED (mechanical): oracle "
                                f"{_oid} fired on buggy from "
                                f"{sorted(_bt)} but on patched from "
                                f"{sorted(_pt)} — disjoint underlying "
                                "exception types mean this is a different "
                                "crash than the one the check pinned on "
                                "buggy, not the bug surviving the patch.")
                        print(f"      [crash-pin] auto-dismissed firing: "
                              f"{(fired or '')[:100]}")
                        print(f"        buggy={sorted(_bt)} "
                              f"patched={sorted(_pt)}")
                        drop_reasons.append((fired, _why))
                        continue
                    # Escape-shaped firing on a SEMANTIC leg: no oracle ID
                    # and not our alarm type — this exception ESCAPED the
                    # library, it is not one of the harness's checks. The
                    # common case is constructor/argument validation on a
                    # junk fuzzed input (minfix_w2b/w2c Math-2-c: a
                    # NotPositiveException from consumeInt junk was kept
                    # TWICE as a conviction of the correct patch). The
                    # differential-replay path only sees the run's FIRST
                    # firing, so escapes surfaced by the keep-going re-fuzz
                    # need the fact stated here.
                    if (bug_kind == 'semantic' and fired
                            and not _fired_ids
                            and 'FuzzerSecurityIssue' not in fired):
                        _note = ("[escaped exception] this firing is NOT "
                                 "one of the harness's own checks — it is "
                                 "an exception that escaped the library "
                                 "(no oracle ID). On a semantic bug the "
                                 "oracle is an alarm throw; an escaped "
                                 "exception is almost always input "
                                 "rejection (constructor/argument "
                                 "validation of a junk fuzzed value — "
                                 "check the harness's input construction "
                                 "for ranges that can go negative or "
                                 "overflow) or pre-existing crash surface. "
                                 "Judge it UNSOUND unless the patch itself "
                                 "demonstrably introduces this exception "
                                 "on a VALID input.")
                        evid = (evid + "\n" + _note) if evid else _note
                    if _latent_note:
                        evid = ((evid + "\n" + _latent_note)
                                if evid else _latent_note)
                    elif (_fired_ids and r.harness_path in latent_map
                          and _fired_ids <= (_oids(src) - _latent_here)):
                        # SYMMETRIC firing: this check also fired on the
                        # buggy build during acceptance, so the patch did
                        # not change the violated behaviour. That is the
                        # classic overfit-catch pattern ONLY when the
                        # violated contract belongs to the reported bug's
                        # own behaviour family (Lang-41: the sibling
                        # String-variant of the very method the test
                        # fails); when it concerns an unrelated feature or
                        # a setup/guard-dependent observable, it is
                        # pre-existing surface that fires on ANY build
                        # (Chart-26-c: an axis-entity check with no
                        # relation to the null-info crash the bug is
                        # about). The verifier owns that judgment — say so.
                        _note = ("[symmetric firing] check(s) "
                                 + ", ".join(sorted(_fired_ids))
                                 + " ALSO fired on the buggy build during "
                                 "the acceptance scan — the patch did NOT "
                                 "change this behaviour. Keep this finding "
                                 "only if the violated contract belongs to "
                                 "the reported bug's own behaviour family "
                                 "(the failing test's methods and "
                                 "observables); if it concerns an "
                                 "unrelated feature or a setup-dependent "
                                 "observable that would fire on any "
                                 "build, it is pre-existing surface — "
                                 "dismiss it.")
                        evid = (evid + "\n" + _note) if evid else _note
                    elif _fired_ids & _latent_here:
                        _note = ("[latent oracle] check(s) "
                                 + ", ".join(sorted(_fired_ids
                                                    & _latent_here))
                                 + " NEVER fired on the buggy build at "
                                 "acceptance — this patched-build firing "
                                 "is their first-ever execution; there is "
                                 "no buggy-side evidence behind them.")
                        evid = (evid + "\n" + _note) if evid else _note
                    # W1.5 (p23gate FP fix): when the fired oracle is a lift
                    # of a trigger test, tell the verifier the decisive fact
                    # it otherwise never learns — the REAL trigger test was
                    # rerun on this patched build and PASSES (the pipeline
                    # exits with bad_patch before fuzzing otherwise). If the
                    # harness replays the test's own scenario, a firing here
                    # means the harness's reconstruction diverges from the
                    # real test's setup (missing source/locale/format
                    # wiring), not that the patch is wrong. A lift firing on
                    # OTHER inputs than the test's own remains a legitimate
                    # generalisation catch.
                    _lifted_of = {t for t in _trigger_method_names
                                  if any(t in fid for fid in _fired_ids)
                                  or (fired and t in fired)}
                    if _lifted_of:
                        _note = ("[trigger-test lift] this oracle lifts "
                                 + ", ".join(sorted(_lifted_of))
                                 + " — the REAL test was rerun on this "
                                 "patched build and PASSES. If this firing "
                                 "replays the test's own scenario/inputs, "
                                 "it is harness-setup divergence and must "
                                 "be dismissed; keep it only if the firing "
                                 "input genuinely differs from the test's "
                                 "own.")
                        evid = (evid + "\n" + _note) if evid else _note
                    ok, why = rv.verify(src, fired_assertion=fired,
                                        trusted_values=trusted_values,
                                        concrete_evidence=evid,
                                        code_context=('\n\n'.join(class_ctx)
                                                      if class_ctx else None))
                    if ok:
                        kept_reason = (fired, why)
                        break
                    # P4.3: remember which ORACLE the verifier judged
                    # unsound (and where), so a keep of the same check from
                    # another firing (whose message happened to hide the
                    # exculpating detail) can be reconciled below.
                    for _uid in _fired_ids:
                        _unsound_oracle_ids.setdefault(_uid, why)
                        _unsound_scope.setdefault(_uid, set()).add(
                            r.harness_path)
                    drop_reasons.append((fired, why))
                if kept_reason is not None:
                    print(f"  ✓ sound: {r.harness_path}")
                    print(f"      kept via: {kept_reason[0]}")
                    print(f"      {kept_reason[1]}")
                    _kept_findings.append(
                        (r, _oids(kept_reason[0] or ''), kept_reason[0]))
                else:
                    print(f"  ✗ dropped (all {len(drop_reasons)} fired "
                          f"oracles unsound): {r.harness_path}")
                    for fired, why in drop_reasons:
                        print(f"      fired: {fired}")
                        print(f"        {why}")
                    r.triggered = False  # no longer counts as a finding
            # P4.3 ("one decision per crash, not per firing"): the same
            # check judged UNSOUND on one firing and kept on another is a
            # contradiction — the messages differ, the oracle doesn't. On
            # contradiction the DISMISSAL wins: the unsound verdict was
            # reached on a firing whose message exposed the exculpating
            # detail (minfix_w2 Lang-60-c: contains('\0') kept once where
            # the message hid that the input really contained '\0', and
            # dropped twice where it showed it).
            for _r, _kids, _kfired in _kept_findings:
                _clash = {
                    k for k in (_kids & set(_unsound_oracle_ids))
                    if k in _injected_rel_names
                    or _r.harness_path in _unsound_scope.get(k, ())}
                if _clash and _r.triggered:
                    _oid = sorted(_clash)[0]
                    print(f"  ✗ reconciled (dismissal wins): oracle "
                          f"{_oid} was judged unsound on another firing "
                          f"of the same check — dropping the kept "
                          f"finding {_r.harness_path}")
                    print(f"      unsound because: "
                          f"{_unsound_oracle_ids[_oid]}")
                    _r.triggered = False

    # 7d) P3.2 replay: execute every screened relation (own + pooled)
    #     DIRECTLY against the patched build. Until now a relation only
    #     mattered if the harness writer implemented it AND the patched
    #     fuzz found the right inputs — two coin flips that cost Math-2-o
    #     its verdict (the probe-validated mean-formula separates the pair
    #     deterministically, but no harness ever carried it to the right
    #     inputs). Firings NEVER convict on their own: each goes through
    #     the same LLM verifier as a harness firing; only a verifier-kept
    #     finding flips the verdict.
    if (args.replay_relations_on_patched and args.verify_relations
            and synthesized_relations and fuzz_results is not None
            and jazzer_standalone_jar):
        print("\n" + "#" * 20 + " relation replay on patched " + "#" * 20)
        try:
            from relation_screen import replay_on_patched
            from relation_verifier import RelationVerifier
            # Idempotent: run_all already built+verified this checkout, so
            # this returns the cached patched copy.
            _patched_dir = PatchedProjectBuilder().build_patched_dir(
                selection.buggy_dir, selection.patch_path)
            _trig_lits_r = []
            for ft in failure_tests:
                if getattr(ft, 'method_source', None):
                    _trig_lits_r += re.findall(
                        r'"((?:[^"\\]|\\.){1,120})"', ft.method_source)
            _trig_lits_r = [s for s in dict.fromkeys(_trig_lits_r)
                            if s.strip()][:32]
            _replay_findings = replay_on_patched(
                synthesized_relations,
                builder=builder,
                patched_dir=_patched_dir,
                jazzer_standalone_jar=jazzer_standalone_jar,
                package=context.package,
                imports=context.source_imports,
                jazzer_api_jar=jazzer_api_jar,
                trigger_literals=_trig_lits_r,
                runs=args.screen_runs,
            )
            record_extras['relation_replay_fired'] = [
                {'name': f['name'], 'tier': f['tier'], 'note': f['note']}
                for f in _replay_findings]
            if _replay_findings:
                _verifier_model = (args.model
                                   or config.HARNESS_MODEL_ESCALATION)
                _rv2 = RelationVerifier(
                    HarnessGenerator(model=_verifier_model,
                                     temperature=0.0, top_p=1.0),
                    votes=config.RELATION_VERIFIER_VOTES)
                _tvals = []
                for ft in failure_tests:
                    if getattr(ft, 'method_source', None):
                        _tvals.extend(
                            expected_assert_literals(ft.method_source))
                _tvals = list(dict.fromkeys(_tvals))
                _kept_replays = []
                for f in _replay_findings:
                    rel = f['relation']
                    _fired = (f"relation {f['name']} violated "
                              f"[replay-on-patched, {f['tier']} tier]")
                    _evid = ("[relation replay] the check below was "
                             "mechanically screened on the buggy build ("
                             + (getattr(rel, 'screen_note', '') or
                                'no screen note')
                             + ") and, compiled UNCHANGED against the "
                             "patched build, " + f['note'] + ". A correct "
                             "patch makes a sound contract relation go "
                             "quiet; judge whether the relation itself is "
                             "sound for ANY correct implementation "
                             "(tolerances generous, inputs fenced).")
                    _src = ("// relation: " + f['name'] + "\n"
                            + "// holds because: "
                            + (getattr(rel, 'contract', '') or '?') + "\n"
                            + "// valid input: "
                            + (getattr(rel, 'input_spec', '') or '?') + "\n"
                            + (getattr(rel, 'check', '') or ''))
                    ok, why = _rv2.verify(
                        _src, fired_assertion=_fired,
                        trusted_values=_tvals,
                        concrete_evidence=_evid,
                        code_context=('\n\n'.join(class_ctx)
                                      if class_ctx else None))
                    if ok:
                        print(f"  ✓ replay conviction kept: {f['name']} "
                              f"[{f['tier']}]")
                        print(f"      {why}")
                        _kept_replays.append(
                            {'name': f['name'], 'tier': f['tier'],
                             'note': f['note'], 'why': why})
                    else:
                        print(f"  ✗ replay firing dropped as unsound: "
                              f"{f['name']} — {why}")
                if _kept_replays:
                    record_extras['relation_replay_kept'] = _kept_replays
                    # extras are applied to the record LAST, so this
                    # overrides the harness-derived False. Only ever set
                    # on conviction — never write False here.
                    record_extras['crashed_on_patch'] = True
                    print(f"  [replay] verdict: {len(_kept_replays)} "
                          "verifier-kept relation conviction(s) — patch "
                          "flagged as overfitting")
        except Exception as exc:
            print(f"  [replay] failed ({exc}) — replay contributes nothing "
                  "this run")

    # A run is scoreable only if we actually fuzzed at least one harness
    # against the patched code; otherwise we have no overfitting verdict.
    if fuzz_results:
        status = 'evaluated'
    else:
        status = 'no_harnesses'
    # ONE complete markdown transcript for the full run too (harness
    # generation + judge LLM calls are captured via the global recorder).
    try:
        if args.results_json:
            _tp = os.path.join(os.path.dirname(args.results_json), 'trace.md')
            _caught = bool(fuzz_results
                           and any(getattr(r, 'triggered', False)
                                   for r in fuzz_results))
            _lbl = 'overfitting' if args.overfitting else 'correct'
            if _lbl == 'overfitting':
                _verdict = ('OVERFIT CAUGHT (a harness fired on the patched '
                            'build)' if _caught else
                            'overfit MISSED (all harnesses quiet on the '
                            'patched build)')
            else:
                _verdict = ('FALSE ALARM (a harness fired on this CORRECT '
                            'patch)' if _caught else
                            'correctly quiet (no false alarm)')
            _write_trace_md(
                _tp, f"{selection.project_name}-{selection.bug_id}", _lbl,
                get_events(),
                outcome=f"{_verdict}. [{status}; "
                        f"{len(fuzz_results or [])} harness(es) fuzzed on the "
                        f"patched build; campaign converged="
                        f"{getattr(result, 'converged', None)}]")
            print(f"  [trace] wrote {_tp} ({len(get_events())} steps)")
    except Exception as _e:
        print(f"  [trace] dump failed: {_e}")
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