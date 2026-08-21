"""Print the errors of one DEV pass, so the next prompt version can fix them.

The protocol is one loop: run a version on dev, read its dev errors, write the
next version to fix the dominant error class. This is the "read its errors"
step, and it reads the dev side only.

Usage (from src/):
    uv run -m baseline_llmjudge.defects4j.errors --records ../results/llmjudge_dev_v1_*/records.jsonl

A false positive is a correct patch called overfitting. A false negative is an
overfitting patch called correct. The two need opposite repairs, so the counts
come first: fix the class that dominates, not the one that reads worst.

HOLDOUT RECORDS ARE REFUSED. Stage B selects on holdout F1, so the holdout is
scored three times. Each of those passes gives back one number per iteration.
If a holdout error log were read, that number would become a sentence, the
sentence would enter the next prompt, and the holdout would stop being held
out. This module is the place that rule is enforced rather than remembered.
"""
import argparse
import glob
import json
import sys
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

RULE = 'majority'


def load(pattern: str) -> List[Dict]:
    paths = sorted(glob.glob(pattern))
    if not paths:
        raise SystemExit(f"no records file matched {pattern!r}")
    rows = []
    for path in paths:
        for line in Path(path).read_text().splitlines():
            if line.strip():
                rows.append(json.loads(line))
    return rows


def classify_error(row: Dict) -> str:
    """'FP', 'FN', or 'right'."""
    truth = row['label'] == 'overfitting'
    said = bool((row.get('decisions') or {}).get(RULE))
    if said == truth:
        return 'right'
    return 'FP' if said else 'FN'


def side_of(records_path: str):
    """'dev', 'holdout', or None when the run's side cannot be determined.

    The run's own `summary.json` is the authority, because `--out_dir` can
    give a run any directory name. The name is the fallback, so a run whose
    summary is missing is still classified."""
    path = Path(records_path)
    summary = path.parent / 'summary.json'
    if summary.exists():
        try:
            side = json.loads(summary.read_text()).get('side')
        except (ValueError, OSError):
            side = None
        if side in ('dev', 'holdout'):
            return side
    name = path.parent.name
    if name.startswith('llmjudge_holdout_'):
        return 'holdout'
    if name.startswith('llmjudge_dev_'):
        return 'dev'
    return None


def refuse_holdout(patterns: List[str]) -> int:
    """0 when every matched run is a dev run. 2 when any is a holdout run.

    A run of unknown side is allowed through with a warning, because the side
    is a property of the run directory and a custom `--out_dir` can hide it."""
    for path in sorted(set(sum((glob.glob(p) for p in patterns), []))):
        side = side_of(path)
        if side == 'holdout':
            print("!" * 70, file=sys.stderr)
            print(f"REFUSING: {path} is a HOLDOUT run.", file=sys.stderr)
            print("Stage B refines from the DEV log and selects on the "
                  "holdout F1.", file=sys.stderr)
            print("Reading a holdout error log would carry holdout evidence "
                  "into the next", file=sys.stderr)
            print("prompt, and the holdout would no longer be held out. See "
                  "README section 6.", file=sys.stderr)
            print("!" * 70, file=sys.stderr)
            return 2
        if side is None:
            print(f"WARNING: cannot tell which side {path} belongs to. "
                  f"Confirm it is a dev run.\n", file=sys.stderr)
    return 0


def _stage_warning(rows: List[Dict]) -> None:
    """Stage A is blind. Say so when these records come from a stage-A design.

    Reading one stage-A design's errors before the other two have run would
    give the later designs information the earlier ones did not have, and the
    bake-off would stop being a fair comparison."""
    stages = {r.get('prompt_stage') for r in rows if r.get('prompt_stage')}
    if 'A' not in stages:
        return
    version = next((r.get('prompt_version') for r in rows
                    if r.get('prompt_stage') == 'A'), '?')
    print("!" * 70)
    print(f"NOTE: {version} is a STAGE-A design, and stage A is blind.")
    print("Read these errors only after all three stage-A designs have been")
    print("scored, and only for the winner. See README section 6.")
    print("!" * 70 + "\n")


def _pool_of(rows: List[Dict]) -> str:
    """The bug pool these records belong to.

    Each pool has its own frozen split and its own family of prompt versions,
    so a dev log read for the wrong pool would repair the wrong wording."""
    pools = sorted({r.get('bug_kind') for r in rows if r.get('bug_kind')})
    return '+'.join(pools) if pools else 'unknown-pool'


def _version_of(rows: List[Dict]) -> str:
    versions = sorted({r.get('prompt_version') for r in rows
                       if r.get('prompt_version')})
    return '+'.join(versions) if versions else '?'


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--records', required=True,
                    help="a run's records.jsonl (globs are allowed)")
    ap.add_argument('--kind', choices=['FP', 'FN', 'both'], default='both',
                    help='which ERROR class to print. This is not the bug '
                         'pool: a run states its pool in its own records')
    ap.add_argument('--samples_shown', type=int, default=1,
                    help='how many sample answers to print per error')
    ap.add_argument('--chars', type=int, default=1200,
                    help='characters of each sample answer to print')
    args = ap.parse_args()

    refused = refuse_holdout([args.records])
    if refused:
        return refused

    all_rows = load(args.records)
    rows = [r for r in all_rows if r.get('status') == 'evaluated']
    _stage_warning(all_rows)
    buckets: Dict[str, List[Dict]] = {'FP': [], 'FN': [], 'right': []}
    for r in rows:
        buckets[classify_error(r)].append(r)

    print(f"{_pool_of(all_rows)} pass, version "
          f"{_version_of(all_rows)}, {RULE} rule")
    print(f"scored {len(rows)} patches under the {RULE} rule")
    print(f"  right : {len(buckets['right'])}")
    print(f"  FP    : {len(buckets['FP'])}  "
          f"(correct patch called overfitting)")
    print(f"  FN    : {len(buckets['FN'])}  (overfitting patch called correct)")
    n_fp, n_fn = len(buckets['FP']), len(buckets['FN'])
    if not n_fp and not n_fn:
        print("\nno errors on this dev pass — this version has no dominant "
              "class to repair.\nThe turn still runs: write the next "
              "iteration against the reasoning above,\nand record that the "
              "dev log was empty.\n")
    elif n_fp == n_fn:
        print(f"\nFP and FN are tied at {n_fp}. Read both below, then pick "
              f"the class whose reasoning is more clearly repairable.\n")
    else:
        dominant = 'FP' if n_fp > n_fn else 'FN'
        print(f"\ndominant error class: {dominant} — fix this one in the next "
              f"prompt version\n")

    wanted = ['FP', 'FN'] if args.kind == 'both' else [args.kind]
    for kind in wanted:
        for r in buckets[kind]:
            vote = r.get('vote') or {}
            print("=" * 70)
            print(f"{kind}  {r['project']}-{r['bug_id']} ({r['apr_tool']})  "
                  f"truth={r['label']}  "
                  f"{vote.get('n_positive')}/{vote.get('n_samples')} said "
                  f"overfitting  agreement={vote.get('agreement')}")
            print(f"     {r['patch']}")
            if vote.get('n_parse_failures'):
                print(f"     {vote['n_parse_failures']} sample(s) unparsed")
            for s in (r.get('samples') or [])[:args.samples_shown]:
                text = (s.get('text') or '').strip()
                if not text:
                    continue
                print("-" * 70)
                print(text[:args.chars])
    return 0


if __name__ == '__main__':
    sys.exit(main())
