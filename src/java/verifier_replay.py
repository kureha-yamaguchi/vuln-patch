"""Offline replay harness for the relation verifier — measure its error
rates on logged cases without spending a single build or fuzz run.

The verifier is the last gate before a verdict, so its error rate bounds
the whole pipeline: it has both LEAKED (passed unsound oracles that became
FPs) and OVER-KILLED (dropped a genuine detection by reasoning abstractly
past the harness's catch-and-skip structure). Every past batch logged the
inputs the verifier saw — harness source, fired oracle, verdict — plus the
ground-truth label, so its decisions can be re-run and scored offline, k
times, under prompt/ensemble variants, for pennies.

Input: a JSONL file, one case per line:
  {
    "id":              "t4syn_overfit_a1",     # any unique string
    "harness_source":  "...java...",           # or "harness_path": "..."
    "fired_assertion": "java.lang.RuntimeException: metamorphic ...",
    "trusted_values":  ["2.5", "0/1"],         # optional
    "concrete_evidence": "== Java Exception ...",  # optional crash block
    "failing_test":    "[REAL FAILING TEST ...]",  # the bug's trigger test,
                                                   # verbatim; family_duty is
                                                   # unanswerable without it
    "label":           "overfitting" | "correct", # ground truth of the PATCH
    "note":            "free text"              # optional
  }
Scoring: for a case whose patch is truly overfitting, the verifier KEEPING
the finding is correct (a drop is an over-kill / manufactured FN). For a
correct patch, DROPPING is correct (a keep is a leak / passed FP). This is
exactly the asymmetry the pipeline lives with, measured directly.

Output (suite-style layout, matching run_suite.sh conventions):
  <out>/config.json     replay parameters
  <out>/results.jsonl   one line per (case x repeat): verdict + reason
  <out>/summary.md      per-case keep rates + aggregate kill/leak rates
                        + exact token usage

Usage (on the VM, from src/):
  uv run python java/verifier_replay.py --cases cases.jsonl \
      --out /home/code/scratch/runs/vreplay_$(date +%Y%m%d_%H%M%S) \
      --repeats 3 --votes 1
"""
import argparse
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents if (p / 'config.py').exists())))

from llm import HarnessGenerator, token_usage, usage_totals  # noqa: E402
from java.relations.relation_verifier import RelationVerifier  # noqa: E402
from java.relations.judge_decision import adjudicate  # noqa: E402
from java.relations.evidence_facts import rate_profile  # noqa: E402


# --- guard-input reconstruction -------------------------------------------
# adjudicate runs the FULL shipped decision (base verify -> 5B void-and-re-ask
# -> 5C terminal identical gate), so the replay must feed it the same guard
# inputs run.py builds live. run.py derives the 5B(ii) drift-kill signature
# {buggy_silent, deterministic_trigger, patched_firing} from screen/replay
# counts it no longer has here; the logged concrete_evidence text carries the
# same facts as tagged phrases, so reconstruct each from those tags. Where a
# signal has NO tag, default to the CONSERVATIVE value (False) — it makes the
# drift signature incomplete, so verdict_needs_citation never fires a
# citation-void, i.e. the replay never MANUFACTURES a void it cannot justify.
_PROFILE_TAGS = {
    # run.py: _b_silent (buggy screen fired on <1% of inputs)
    'buggy_silent': ('silent on the buggy build',
                     'fired on 0/', 'never held', 'without firing this check'),
    # run.py: f.get('tier') == 'trigger' (deterministic trigger-tier replay)
    'deterministic_trigger': ('deterministic', 'trigger-tier fact',
                              "test's own input literals"),
    # run.py: f.get('patched_violated') (the firing itself, on the patched build)
    'patched_firing': ('on the patched build', 'fires on the patched',
                       'on this patched build'),
}


def reconstruct_evidence_profile(concrete_evidence):
    """Rebuild the 5B(ii) drift-kill signature from logged evidence text.

    Cycle-6 PART 1 — TAG FIRST. `buggy_silent` used to be inferred purely by
    keyword-matching prose, including the prose of the [fire-rate fact] note,
    which is exactly the branch that note ALREADY knows. When the evidence
    carries a cycle-6 rate tag, that tag DECIDES this signal and no prose is
    consulted: `rate-catch-signal` is the only profile that means "silent on
    the buggy build"; `rate-indiscriminate` and `rate-ambiguous` both mean it
    is measurably NOT silent, and must override any phrase elsewhere in the
    blob that reads like silence. Untagged evidence keeps the prose path
    unchanged.

    Returns (profile_dict, missing_signals) where missing_signals is the list
    of keys that had NO reconstructable tag and were defaulted to False."""
    low = (concrete_evidence or '').lower()
    profile, missing = {}, []
    for key, tags in _PROFILE_TAGS.items():
        present = any(t in low for t in tags)
        profile[key] = present
        if not present:
            missing.append(key)
    try:
        rp = rate_profile(concrete_evidence)
    except Exception:  # pragma: no cover - defensive; prose result stands
        rp = None
    if rp is not None:
        profile['buggy_silent'] = (rp == 'catch-signal')
        # A measured answer is not a missing signal, whichever way it went.
        missing = [k for k in missing if k != 'buggy_silent']
    return profile, missing


def parse_args():
    p = argparse.ArgumentParser(
        description="Replay logged fired-oracle cases through the relation "
                    "verifier and score keep/drop against ground truth.")
    p.add_argument("--cases", required=True,
                   help="JSONL of cases (see module docstring)")
    p.add_argument("--out", required=True,
                   help="output directory (suite-style layout)")
    p.add_argument("--repeats", type=int, default=3,
                   help="times to re-run each case (measures verdict "
                        "stability; default 3)")
    p.add_argument("--votes", type=int, default=1,
                   help="ensemble votes per verify() call (1 = single "
                        "review; >1 = diverse-lens majority)")
    p.add_argument("--model", default=None,
                   help="model/deployment for the verifier (default: "
                        "config default)")
    p.add_argument("--no-evidence", action="store_true",
                   help="ablation: withhold concrete_evidence even when a "
                        "case carries it")
    p.add_argument("--no-trusted", action="store_true",
                   help="ablation: withhold trusted_values even when a "
                        "case carries them")
    return p.parse_args()


def load_cases(path):
    cases = []
    with open(path, encoding='utf-8') as fh:
        for lineno, line in enumerate(fh, 1):
            line = line.strip()
            if not line:
                continue
            try:
                c = json.loads(line)
            except ValueError as e:
                print(f"  skipping line {lineno}: bad JSON ({e})")
                continue
            src = c.get('harness_source')
            if not src and c.get('harness_path'):
                try:
                    src = open(c['harness_path'], encoding='utf-8',
                               errors='replace').read()
                except OSError as e:
                    print(f"  skipping {c.get('id', lineno)}: "
                          f"unreadable harness_path ({e})")
                    continue
            if not src:
                print(f"  skipping {c.get('id', lineno)}: no harness source")
                continue
            c['harness_source'] = src
            c.setdefault('id', f'case_{lineno}')
            cases.append(c)
    return cases


def main():
    args = parse_args()
    cases = load_cases(args.cases)
    if not cases:
        print("no usable cases; nothing to do")
        sys.exit(1)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    gen = (HarnessGenerator(model=args.model, temperature=0.0, top_p=1.0)
           if args.model else None)
    rv = RelationVerifier(generator=gen, votes=args.votes)

    (out / 'config.json').write_text(json.dumps({
        'cases_file': os.path.abspath(args.cases),
        'n_cases': len(cases),
        'repeats': args.repeats,
        'votes': args.votes,
        'model': args.model or 'config-default',
        'no_evidence': args.no_evidence,
        'no_trusted': args.no_trusted,
    }, indent=2) + '\n')

    results_path = out / 'results.jsonl'
    per_case = {}
    # Track which cases lacked a reconstructable drift-kill signal (defaulted
    # to the conservative False), so the summary records what the replay could
    # not reconstruct from logged evidence text.
    missing_signal_cases = {}
    # Cases whose Spec-J ladder rung could not be reconstructed from the
    # original run's trace: fd_prior is a guess there, so they are RUN and
    # reported but kept OUT of the scored kill/leak totals.
    unresolved_ladder = set()
    with open(results_path, 'w', encoding='utf-8') as rf:
        for c in cases:
            keeps = 0
            evid = (None if args.no_evidence else c.get('concrete_evidence'))
            ev_profile, missing = reconstruct_evidence_profile(evid)
            if missing:
                missing_signal_cases[c['id']] = missing
            if c.get('fd_prior_unresolved'):
                unresolved_ladder.add(c['id'])
            # Per-guard replay input derivation:
            #  * pinned_source = harness_source — replay has no pinned dict;
            #    passing the raw source string makes dismissal_invokes_pinned
            #    conservative (never a pin-void).
            #  * evidence_profile = reconstructed drift-kill signature (above).
            #  * failing_block = the case's `failing_test` — the REAL FAILING
            #    TEST block run.py renders via _j3_failing_test_block, recovered
            #    verbatim from the leg's trace (scripts/backfill_failing_test.py).
            #    family_duty's whole question is "does this check assert the
            #    FAILING TEST's own observable?", so with '' here the escape can
            #    essentially never answer YES and every family-duty-escaped rule
            #    gets measured with its escape disabled — the harness artifact
            #    that invalidated the v6 gate's cost side. A case whose trace
            #    genuinely has no block still carries '' (fails to the strict
            #    side), but tests/test_fixture_fidelity.py pins that to zero.
            #  * check_source = harness_source.
            #  * fd_prior = the value RECONSTRUCTED from the original run's
            #    trace (scripts/reconstruct_fd_prior.py) — what run.py's Spec-J
            #    ladder actually handed the 5C gate for this firing. Hard-coding
            #    None here made the replay STRICTER than production: on a firing
            #    the ladder had already settled (trigger-input exemption ->
            #    fd_prior=True) the gate re-asked family_duty and dropped a
            #    catch the pipeline keeps. Cases whose rung is not recoverable
            #    carry fd_prior_unresolved and are reported separately instead
            #    of being scored on a guess.
            failing_block = (c.get('failing_test') or c.get('failing_block')
                             or '')
            for rep in range(args.repeats):
                ok, why = adjudicate(
                    rv,
                    harness_source=c['harness_source'],
                    fired_assertion=c.get('fired_assertion'),
                    trusted_values=(None if args.no_trusted
                                    else c.get('trusted_values')),
                    concrete_evidence=evid,
                    code_context=c.get('code_context'),
                    pinned_source=c['harness_source'],
                    evidence_profile=ev_profile,
                    failing_block=failing_block,
                    check_source=c['harness_source'],
                    fd_prior=c.get('fd_prior'),
                )
                keeps += bool(ok)
                rf.write(json.dumps({
                    'id': c['id'], 'repeat': rep, 'kept': bool(ok),
                    'label': c.get('label'), 'reason': why,
                    'fd_prior': c.get('fd_prior'),
                    'fd_prior_unresolved': bool(c.get('fd_prior_unresolved')),
                }) + '\n')
                rf.flush()
            per_case[c['id']] = (keeps, args.repeats, c.get('label'),
                                 c.get('note', ''))
            print(f"  {c['id']} [{c.get('label')}]: kept "
                  f"{keeps}/{args.repeats}")

    # ---- score --------------------------------------------------------
    # Overfitting patch -> the finding is TRUE -> keeping is correct.
    # Correct patch     -> the finding is FALSE -> dropping is correct.
    # Cases with an unreconstructable ladder rung are excluded from BOTH
    # totals: their fd_prior would be a guess, and a guess must not move the
    # gate number in either direction.
    scored = {k: v for k, v in per_case.items() if k not in unresolved_ladder}
    overfit = {k: v for k, v in scored.items() if v[2] == 'overfitting'}
    correct = {k: v for k, v in scored.items() if v[2] == 'correct'}
    kills = sum(reps - keeps for keeps, reps, _, _ in overfit.values())
    kill_den = sum(reps for _, reps, _, _ in overfit.values())
    leaks = sum(keeps for keeps, reps, _, _ in correct.values())
    leak_den = sum(reps for _, reps, _, _ in correct.values())

    tok = usage_totals()
    lines = [
        f"# verifier replay ({out.name})", "",
        f"{len(cases)} cases x {args.repeats} repeats, votes={args.votes},"
        f" model={args.model or 'config-default'}",
        "",
        f"scored: {len(scored)} cases "
        f"({len(unresolved_ladder)} excluded — unresolved ladder, below)",
        "",
        "| case | label | kept | note |",
        "|---|---|---|---|",
    ]
    for cid, (keeps, reps, label, note) in per_case.items():
        lines.append(f"| {cid} | {label} | {keeps}/{reps} | {note} |")
    lines += [
        "",
        f"**OVER-KILL rate (true findings dropped): "
        f"{kills}/{kill_den}"
        + (f" = {kills / kill_den:.0%}" if kill_den else " (no TP cases)")
        + "**",
        f"**LEAK rate (false findings kept): {leaks}/{leak_den}"
        + (f" = {leaks / leak_den:.0%}" if leak_den else " (no FP cases)")
        + "**",
        "",
        f"Tokens: {tok['total_tokens']:,} total "
        f"({tok['prompt_tokens']:,} in + {tok['completion_tokens']:,} out, "
        f"{tok['calls']} calls)",
        f"By model: {json.dumps(token_usage())}",
    ]
    # ---- unresolved-ladder bucket (run, reported, NOT scored) ----------
    lines += [
        "",
        f"## unresolved-ladder ({len(unresolved_ladder)} cases, excluded "
        f"from the rates above)",
        "",
        "The original run's trace shows the Spec-J ladder was ARMED for these "
        "firings (the buggy-replay value comparison returned identical) but "
        "records no family-duty event, so the rung it took — trigger-input "
        "exemption (fd_prior=True) or setup-divergence (fd_prior left None) — "
        "is not recoverable. They are replayed on fd_prior=None and reported "
        "here rather than scored on a guess.",
        "",
    ]
    if unresolved_ladder:
        lines.append("| case | label | kept | note |")
        lines.append("|---|---|---|---|")
        for cid in unresolved_ladder:
            keeps, reps, label, note = per_case[cid]
            lines.append(f"| {cid} | {label} | {keeps}/{reps} | {note} |")
    else:
        lines.append("(none)")
    if missing_signal_cases:
        lines += [
            "",
            f"Drift-kill signals not reconstructable from logged evidence "
            f"(defaulted to conservative False) in "
            f"{len(missing_signal_cases)}/{len(cases)} cases:",
        ]
        for cid, miss in missing_signal_cases.items():
            lines.append(f"  - {cid}: {', '.join(miss)}")
    (out / 'summary.md').write_text('\n'.join(lines) + '\n')
    print(f"\nsummary: {out / 'summary.md'}")
    print('\n'.join(lines[-6:]))


if __name__ == '__main__':
    main()
