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
    with open(results_path, 'w', encoding='utf-8') as rf:
        for c in cases:
            keeps = 0
            for rep in range(args.repeats):
                ok, why = rv.verify(
                    c['harness_source'],
                    fired_assertion=c.get('fired_assertion'),
                    trusted_values=(None if args.no_trusted
                                    else c.get('trusted_values')),
                    concrete_evidence=(None if args.no_evidence
                                       else c.get('concrete_evidence')),
                )
                keeps += bool(ok)
                rf.write(json.dumps({
                    'id': c['id'], 'repeat': rep, 'kept': bool(ok),
                    'label': c.get('label'), 'reason': why,
                }) + '\n')
                rf.flush()
            per_case[c['id']] = (keeps, args.repeats, c.get('label'),
                                 c.get('note', ''))
            print(f"  {c['id']} [{c.get('label')}]: kept "
                  f"{keeps}/{args.repeats}")

    # ---- score --------------------------------------------------------
    # Overfitting patch -> the finding is TRUE -> keeping is correct.
    # Correct patch     -> the finding is FALSE -> dropping is correct.
    overfit = {k: v for k, v in per_case.items() if v[2] == 'overfitting'}
    correct = {k: v for k, v in per_case.items() if v[2] == 'correct'}
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
    (out / 'summary.md').write_text('\n'.join(lines) + '\n')
    print(f"\nsummary: {out / 'summary.md'}")
    print('\n'.join(lines[-6:]))


if __name__ == '__main__':
    main()
