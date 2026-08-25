"""Print one DEV pass, patch by patch, so the next prompt version can fix it.

The port of `defects4j/errors.py`. The protocol is one loop: run a version on
dev, read its dev errors, write the next version to repair the dominant error
class. This is the "read its errors" step, and it reads the dev side only.

Usage (from src/):
    # the whole pass: counts, the grid, then each error with its reasoning
    uv run -m baseline_llmjudge.project_zero.errors \\
        --records ../results/llmjudge_pz_dev_p2_*/records.jsonl

    # the grid alone — every repetition of every patch, one line each
    uv run -m baseline_llmjudge.project_zero.errors --records ... --grid_only

    # one error class, with more of the reasoning
    uv run -m baseline_llmjudge.project_zero.errors --records ... \\
        --kind FN --samples_shown 2 --chars 3000

A false positive is a correct fix called overfitting. A false negative is an
overfitting fix called correct. The two need opposite repairs, so the counts
come first: fix the class that dominates, not the one that reads worst.

ONE ADDITION THE DEFECTS4J MODULE DOES NOT HAVE. The grid prints all five
repetitions of every patch, right and wrong alike, as `O` and `C` cells. The
Defects4J version prints errors only. Here the sample spread is the thing worth
seeing: a version can be wrong on every patch and still disagree with itself on
a few, and those few are where the next version has something to work with. A
column of identical cells says the opposite — that the wording, not the noise,
is what has to move.

HOLDOUT RECORDS ARE REFUSED. Stage B selects on holdout F1. If a holdout error
log were read, that number would become a sentence, the sentence would enter
the next prompt, and the holdout would stop being held out. This module is the
place that rule is enforced rather than remembered.
"""
import argparse
import glob
import json
import sys
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

from baseline_llmjudge.shared.scoring import HEADLINE_RULE   # noqa: E402

#: The rule an error is judged under. Imported, so this module and the
#: evaluator cannot disagree about which prediction counts.
RULE = HEADLINE_RULE

#: One cell of the grid per repetition.
CELL = {True: 'O', False: 'C', None: '?'}


def load(pattern: str) -> List[Dict]:
    paths = sorted(glob.glob(pattern))
    if not paths:
        raise SystemExit(f'no records file matched {pattern!r}')
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

    The run's own `summary.json` is the authority, because `--out_dir` can give
    a run any directory name. The directory name is the fallback, so a run
    whose summary is missing — an interrupted pass, for instance — is still
    classified."""
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
    if name.startswith('llmjudge_pz_holdout_'):
        return 'holdout'
    if name.startswith('llmjudge_pz_dev_'):
        return 'dev'
    return None


def refuse_holdout(patterns: List[str]) -> int:
    """0 when every matched run is a dev run. 2 when any is a holdout run.

    A run of unknown side is allowed through with a warning, because the side
    is a property of the run directory and a custom `--out_dir` can hide it."""
    for path in sorted(set(sum((glob.glob(p) for p in patterns), []))):
        side = side_of(path)
        if side == 'holdout':
            print('!' * 70, file=sys.stderr)
            print(f'REFUSING: {path} is a HOLDOUT run.', file=sys.stderr)
            print('Stage B refines from the DEV log and selects on the '
                  'holdout F1.', file=sys.stderr)
            print('Reading a holdout error log would carry holdout evidence '
                  'into the next', file=sys.stderr)
            print('prompt, and the holdout would no longer be held out. See '
                  'README section 11.11.', file=sys.stderr)
            print('!' * 70, file=sys.stderr)
            return 2
        if side is None:
            print(f'WARNING: cannot tell which side {path} belongs to. '
                  f'Confirm it is a dev run.\n', file=sys.stderr)
    return 0


def _stage_warning(rows: List[Dict]) -> None:
    """Stage A is blind. Say so when these records come from a stage-A design.

    Reading one design's errors before the others have run would give the later
    designs information the earlier ones did not have, and the bake-off would
    stop being a fair comparison."""
    stages = {r.get('prompt_stage') for r in rows if r.get('prompt_stage')}
    if 'A' not in stages:
        return
    version = next((r.get('prompt_version') for r in rows
                    if r.get('prompt_stage') == 'A'), '?')
    print('!' * 70)
    print(f'NOTE: {version} is a STAGE-A design, and stage A is blind.')
    print('Read these errors only after every stage-A design has been scored,')
    print('and only for the winner. See README section 11.11.')
    print('!' * 70 + '\n')


def _incomplete_warning(rows: List[Dict], records_path: str) -> None:
    """Say so when the pass has no summary, so the counts are partial."""
    for path in sorted(glob.glob(records_path)):
        if not (Path(path).parent / 'summary.json').exists():
            print(f'NOTE: {Path(path).parent.name} has no summary.json, so '
                  f'that pass did not finish.\n      Its {len(rows)} record(s) '
                  f'are read, and the counts below are partial.\n')


def _pool_of(rows: List[Dict]) -> str:
    """The bug pool these records belong to."""
    pools = sorted({r.get('bug_kind') for r in rows if r.get('bug_kind')})
    return '+'.join(pools) if pools else 'unknown-pool'


def _version_of(rows: List[Dict]) -> str:
    versions = sorted({r.get('prompt_version') for r in rows
                       if r.get('prompt_version')})
    return '+'.join(versions) if versions else '?'


def print_grid(rows: List[Dict]) -> None:
    """Every repetition of every patch. `O` is overfitting, `C` is correct.

    `?` is a sample whose answer carried no VERDICT line. It counts as the
    negative class, and `shared/verdict.py` says why."""
    width = max((len(r.get('fix_id') or '?') for r in rows), default=8)
    print(f"{'fix':<{width}}  {'truth':<12} {'samples':<11} "
          f"{'majority':<12} {'agree':>5}  where")
    print('-' * (width + 56))
    for r in rows:
        vote = r.get('vote') or {}
        cells = ' '.join(CELL[s.get('verdict')]
                         for s in (r.get('samples') or []))
        said = 'overfitting' if (r.get('decisions')
                                 or {}).get(RULE) else 'correct'
        wrong = '  <- WRONG' if classify_error(r) != 'right' else ''
        agree = vote.get('agreement')
        print(f"{r.get('fix_id', '?'):<{width}}  {r['label']:<12} "
              f"{cells:<11} {said:<12} "
              f"{'n/a' if agree is None else f'{agree:.2f}':>5}  "
              f"{r.get('pair', '?')}/{r.get('which', '?')}{wrong}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--records', required=True,
                    help="a run's records.jsonl (globs are allowed)")
    ap.add_argument('--kind', choices=['FP', 'FN', 'both'], default='both',
                    help='which ERROR class to print in full. This is not the '
                         'bug pool: a run states its pool in its own records')
    ap.add_argument('--grid_only', action='store_true',
                    help='print the per-patch grid and stop')
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
    _incomplete_warning(all_rows, args.records)

    buckets: Dict[str, List[Dict]] = {'FP': [], 'FN': [], 'right': []}
    for r in rows:
        buckets[classify_error(r)].append(r)

    print(f'project_zero {_pool_of(all_rows)} pass, version '
          f'{_version_of(all_rows)}, {RULE} rule')
    print(f'scored {len(rows)} fixes under the {RULE} rule')
    print(f"  right : {len(buckets['right'])}")
    print(f"  FP    : {len(buckets['FP'])}  (correct fix called overfitting)")
    print(f"  FN    : {len(buckets['FN'])}  (overfitting fix called correct)")
    print()
    print_grid(rows)
    if args.grid_only:
        return 0

    n_fp, n_fn = len(buckets['FP']), len(buckets['FN'])
    disagreeing = sum(1 for r in rows
                      if 0 < (r.get('vote') or {}).get('n_positive', 0)
                      < (r.get('vote') or {}).get('n_samples', 0))
    print()
    if not n_fp and not n_fn:
        print('no errors on this dev pass — this version has no dominant '
              'class to repair.\nThe turn still runs: write the next '
              'iteration against the reasoning above,\nand record that the '
              'dev log was empty.')
    elif n_fp == n_fn:
        print(f'FP and FN are tied at {n_fp}. Read both below, then pick the '
              f'class whose reasoning is more clearly repairable.')
    else:
        dominant = 'FP' if n_fp > n_fn else 'FN'
        print(f'dominant error class: {dominant} — fix this one in the next '
              f'prompt version')
    # A version that never disagrees with itself is not being pushed by noise.
    # Its wording is what has to change, and no single patch is a soft target.
    print(f'fixes where the five samples disagreed: {disagreeing} of '
          f'{len(rows)}')
    if rows and not disagreeing:
        print('  Every fix was unanimous. So no patch is a near miss, and the '
              'next version\n  has to move the wording rather than tip a '
              'close call.')
    print()

    wanted = ['FP', 'FN'] if args.kind == 'both' else [args.kind]
    for kind in wanted:
        for r in buckets[kind]:
            vote = r.get('vote') or {}
            print('=' * 70)
            print(f"{kind}  {r.get('fix_id')}  truth={r['label']}  "
                  f"{vote.get('n_positive')}/{vote.get('n_samples')} said "
                  f"overfitting  agreement={vote.get('agreement')}")
            print(f"     {r.get('codebase')} — {r.get('pair')}/"
                  f"{r.get('which')}")
            print(f"     files: {', '.join(r.get('touched_files') or [])}")
            if vote.get('n_parse_failures'):
                print(f"     {vote['n_parse_failures']} sample(s) unparsed")
            for s in (r.get('samples') or [])[:args.samples_shown]:
                text = (s.get('text') or '').strip()
                if not text:
                    continue
                print('-' * 70)
                print(text[:args.chars])
    return 0


if __name__ == '__main__':
    sys.exit(main())
