"""Compare Project Zero runs and name the winner, for either stage.

The counterpart of `defects4j/compare.py`, and it reads the two sides the same
way:

    stage A   selects on dev F1
    stage B   selects on HOLDOUT F1

The difference is deliberate. A stage-B iteration is written from a dev error
log, so its dev score is the score of a version tuned against that same log.
Its holdout score is not. Selection therefore reads the holdout. Nothing flows
back from a holdout run into a prompt, so that is not leakage.

One bias remains, and it must be reported: the selected iteration's holdout F1
is a maximum over the iterations, so it is optimistic. Publish every holdout
row, not the winner's row alone.

Ties break the same way in both stages: the lower false-positive count.

ONE COLUMN THAT THE DEFECTS4J TABLE DOES NOT HAVE. Every row prints `floor`,
the higher of the two baselines that read no code — the always-positive rule
and the best diff-size rule. A version whose F1 does not clearly beat its own
run's floor is unproven, whatever its rank in the table. So the table marks it
rather than leaving a reader to look the number up.

Usage (from src/):
    uv run -m baseline_llmjudge.project_zero.compare --stage A
    uv run -m baseline_llmjudge.project_zero.compare --stage B --base p2
"""
import argparse
import json
import sys
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

from baseline_llmjudge.project_zero import prompts        # noqa: E402

REPO = Path(__file__).resolve().parents[3]
RESULTS = REPO / 'results'

#: How far above its own floor an F1 must sit before the run counts as more
#: than a baseline that reads no code. The same margin `evaluate.py` prints.
FLOOR_MARGIN = 0.05


def load_runs(results_dir: Path, side: str) -> Dict[str, Dict]:
    """Every scored run of one side, keyed by prompt version.

    When one version was run more than once, the latest run wins — a rerun of
    the same frozen text supersedes the earlier one."""
    runs: Dict[str, Dict] = {}
    for path in sorted(results_dir.glob(f'llmjudge_pz_{side}_*/summary.json')):
        s = json.loads(path.read_text())
        s['run_dir'] = str(path.parent)
        runs[s['prompt_version']] = s
    return runs


def floor_of(run: Dict) -> Optional[float]:
    """The higher of the run's two baselines that read no code."""
    baselines = run.get('baselines') or {}
    values = [(baselines.get(name) or {}).get('f1')
              for name in ('always_positive', 'size_rule')]
    values = [v for v in values if v is not None]
    return max(values) if values else None


def select(runs: List[Dict]) -> Optional[Dict]:
    """Highest F1, ties broken by fewer false positives."""
    scored = [r for r in runs
              if (r.get('headline') or {}).get('f1') is not None]
    if not scored:
        return None
    return sorted(scored, key=lambda r: (-r['headline']['f1'],
                                         r['headline']['FP']))[0]


def rows_for(runs: Dict[str, Dict], names: List[str]) -> List[Dict]:
    return [runs[n] for n in names if n in runs]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--stage', required=True, choices=['A', 'B'])
    ap.add_argument('--base', default=None,
                    help="stage B only: the stage-A winner, e.g. 'p2'")
    ap.add_argument('--results_dir', default=str(RESULTS))
    args = ap.parse_args()

    if args.stage == 'B' and not args.base:
        print('--stage B needs --base (the stage-A winner)', file=sys.stderr)
        return 2
    if args.base is not None and args.base not in prompts.BASE_VERSIONS:
        print(f'REFUSING: unknown base {args.base!r}; expected one of '
              f'{prompts.BASE_VERSIONS}', file=sys.stderr)
        return 2

    results_dir = Path(args.results_dir)
    if args.stage == 'A':
        return _stage_a(results_dir)
    return _stage_b(results_dir, args.base)


# --- stage A -----------------------------------------------------------------

def _stage_a(results_dir: Path) -> int:
    """The stage-A designs, scored on dev."""
    runs = load_runs(results_dir, 'dev')
    wanted = list(prompts.BASE_VERSIONS)
    candidates = rows_for(runs, wanted)

    print(f'stage A — Project Zero dev runs found in {results_dir}')
    print('selection: highest dev F1, ties broken by fewer false positives\n')
    _table(candidates)
    _report_missing(wanted, candidates, 'dev',
                    'The winner is only meaningful once every design has run.')

    winner = select(candidates)
    if winner is None:
        print('\nno scored candidate yet — nothing to select.')
        return 0
    _announce(winner, 'dev')
    print(f"\nnext: refine {winner['prompt_version']} by hand. Read its DEV "
          f"records first:")
    print(f"  {winner['run_dir']}/records.jsonl")
    return 0


# --- stage B -----------------------------------------------------------------

def _stage_b(results_dir: Path, base: str) -> int:
    """The iterations of one design, refined on dev, selected on holdout."""
    dev = load_runs(results_dir, 'dev')
    hold = load_runs(results_dir, 'holdout')
    wanted = [n for n in prompts.known_versions()
              if prompts.is_iteration(n) and prompts.base_of(n) == base]
    candidates = rows_for(hold, wanted)

    print(f'stage B — Project Zero holdout runs found in {results_dir}')
    print('selection: highest HOLDOUT F1, ties broken by fewer false '
          'positives')
    print('the dev column is the refinement side. It is shown for the record, '
          'and it does not select.\n')
    _table(rows_for(hold, [base]) + candidates, dev=dev, base=base)

    _report_missing(wanted, candidates, 'holdout',
                    'Selection reads the holdout, so every iteration needs a '
                    'holdout pass.')
    no_dev = [n for n in wanted if n not in dev]
    if no_dev:
        print(f'no DEV run: {no_dev}')
        print('  A turn is refined from the previous iteration\'s dev log, so '
              'every turn but the last needs one.')
    if base not in hold:
        print(f'\nno holdout run for the base {base!r}. Run it as the '
              f'reference row, or the iteration log cannot say whether an '
              f'iteration improved on its own parent.')

    winner = select(candidates)
    if winner is None:
        print('\nno scored iteration yet — nothing to select.')
        return 0
    _announce(winner, 'holdout')
    scores = [r['headline']['f1'] for r in candidates
              if r['headline']['f1'] is not None]
    if len(scores) > 1:
        print(f"\nThe winner's holdout F1 is a maximum over "
              f"{len(scores)} iterations, so it is optimistic. The honest "
              f"sentence names the population it was selected from:")
        print('  "of %d iterations refined on dev, the best scored F1=%.2f '
              'on holdout; the %d scored %s."'
              % (len(scores), max(scores), len(scores),
                 ', '.join(f'{s:.2f}' for s in sorted(scores, reverse=True))))
    return 0


# --- printing ----------------------------------------------------------------

def _table(rows: List[Dict], dev: Dict[str, Dict] = None,
           base: str = None) -> None:
    dev_col = dev is not None
    header = (f"{'version':<10} " + (f"{'devF1':>7} " if dev_col else '')
              + f"{'P':>7} {'R':>7} {'F1':>7} {'floor':>7} {'FP':>4} "
              f"{'FN':>4} {'parse':>6}  run")
    print(header)
    print('-' * len(header))
    for r in rows:
        name = r['prompt_version']
        h = r['headline']
        floor = floor_of(r)
        beats = (h['f1'] is not None and floor is not None
                 and h['f1'] - floor > FLOOR_MARGIN)
        tag = ' (reference)' if base and name == base else ''
        if floor is not None and not beats:
            tag += '  <- does not beat its floor'
        line = f'{name:<10} '
        if dev_col:
            line += _f(((dev.get(name) or {}).get('headline')
                        or {}).get('f1')) + ' '
        line += (f"{_f(h['precision'])} {_f(h['recall'])} {_f(h['f1'])} "
                 f"{_f(floor)} {h['FP']:>4} {h['FN']:>4} "
                 f"{r.get('parse_failures', 0):>6}  "
                 f"{Path(r['run_dir']).name}{tag}")
        print(line)


def _report_missing(wanted: List[str], candidates: List[Dict], side: str,
                    why: str) -> None:
    have = {r['prompt_version'] for r in candidates}
    missing = [n for n in wanted if n not in have]
    if missing:
        print(f'\nno {side} run: {missing}')
        print(f'  {why}')


def _announce(winner: Dict, side: str) -> None:
    h = winner['headline']
    floor = floor_of(winner)
    print(f"\nwinner on {side} F1: {winner['prompt_version']}  "
          f"(F1={_f(h['f1']).strip()}, P={_f(h['precision']).strip()}, "
          f"R={_f(h['recall']).strip()})")
    print(f"  run: {winner['run_dir']}")
    if floor is None:
        return
    if h['f1'] - floor > FLOOR_MARGIN:
        print(f'  it beats its floor of {floor:.3f} by '
              f'{h["f1"] - floor:+.3f}.')
    else:
        print(f'  WARNING: its floor is {floor:.3f}, so it beats a baseline '
              f'that reads no code by only {h["f1"] - floor:+.3f}.')
        print('  Report this winner as unproven on this population. It is the '
              'best of the designs, and that is not the same claim.')


def _f(v) -> str:
    return f'{"n/a":>7}' if v is None else f'{v:>7.3f}'


if __name__ == '__main__':
    sys.exit(main())
