"""Compare runs and name the winner, for either stage of the protocol.

Stage A picks a design out of three blind dev runs. Stage B picks an iteration
out of three refinement iterations. The two stages read different sides:

    stage A   selects on dev F1
    stage B   selects on HOLDOUT F1

The difference is deliberate. A stage-B iteration is written from a dev error
log, so its dev score is the score of a version tuned against that same log.
Its holdout score is not. Selection therefore reads the holdout.

That is not leakage, because nothing flows back from a holdout run into a
prompt. The refinement input is the dev log only, and `errors.py` refuses
holdout records. Each holdout pass returns one number per iteration, never a
sentence.

One bias remains, and it must be reported: the selected iteration's holdout F1
is a maximum over three iterations, so it is optimistic. Publish all three
holdout rows, not the winner's row alone.

Ties break the same way in both stages: the lower false-positive count.

Usage (from src/):
    # stage A — the three blind designs, scored on dev
    uv run -m baseline_llmjudge.compare --stage A

    # stage B — the iterations of the stage-A winner, scored on holdout
    uv run -m baseline_llmjudge.compare --stage B --base v2

`--stage B` reports the base design's own holdout score too, as a reference
row. If no iteration beats it, the protocol still selects the best iteration —
but a regression that large belongs in the iteration log.
"""
import argparse
import json
import sys
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

from baseline_llmjudge import prompts        # noqa: E402

REPO = Path(__file__).resolve().parents[2]
RESULTS = REPO / 'results'

#: Which side each stage selects on. Stage B reads the holdout, because an
#: iteration written from the dev log is tuned against the dev log.
SELECTION_SIDE = {'A': 'dev', 'B': 'holdout'}


def load_runs(results_dir: Path, side: str = 'dev') -> Dict[str, Dict]:
    """Every scored run of one side, keyed by prompt version.

    When one version was run more than once, the latest run wins — a rerun of
    the same frozen text supersedes the earlier one."""
    runs: Dict[str, Dict] = {}
    for path in sorted(results_dir.glob(f'llmjudge_{side}_*/summary.json')):
        s = json.loads(path.read_text())
        s['run_dir'] = str(path.parent)
        runs[s['prompt_version']] = s
    return runs


def select(runs: List[Dict]) -> Optional[Dict]:
    """Highest F1, ties broken by fewer false positives."""
    scored = [r for r in runs if (r.get('headline') or {}).get('f1') is not None]
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
                    help="stage B only: the stage-A winner, e.g. 'v2'")
    ap.add_argument('--results_dir', default=str(RESULTS))
    args = ap.parse_args()

    if args.stage == 'B' and not args.base:
        print("--stage B needs --base (the stage-A winner)", file=sys.stderr)
        return 2

    results_dir = Path(args.results_dir)
    if args.stage == 'A':
        return _stage_a(results_dir)
    return _stage_b(results_dir, args.base)


# --- stage A -----------------------------------------------------------------

def _stage_a(results_dir: Path) -> int:
    """Three blind designs, scored on dev."""
    runs = load_runs(results_dir, 'dev')
    wanted = list(prompts.BASE_VERSIONS)
    candidates = rows_for(runs, wanted)

    print(f"stage A — dev runs found in {results_dir}")
    print("selection: highest dev F1, ties broken by fewer false positives\n")
    header = (f"{'version':<10} {'P':>7} {'R':>7} {'F1':>7} {'FP':>4} "
              f"{'FN':>4} {'parse':>6}  run")
    print(header)
    print('-' * len(header))
    for r in candidates:
        h = r['headline']
        print(f"{r['prompt_version']:<10} {_f(h['precision'])} "
              f"{_f(h['recall'])} {_f(h['f1'])} {h['FP']:>4} {h['FN']:>4} "
              f"{r.get('parse_failures', 0):>6}  {Path(r['run_dir']).name}")

    _report_missing(wanted, candidates, 'dev',
                    "The winner is only meaningful once every design has run.")

    winner = select(candidates)
    if winner is None:
        print("\nno scored candidate yet — nothing to select.")
        return 0
    _announce(winner, 'dev')

    print(f"\nnext: refine {winner['prompt_version']} three times. Read its "
          f"DEV errors first:")
    print(f"  uv run -m baseline_llmjudge.errors "
          f"--records {winner['run_dir']}/records.jsonl")
    return 0


# --- stage B -----------------------------------------------------------------

def _stage_b(results_dir: Path, base: str) -> int:
    """Three refinement iterations, refined on dev, selected on holdout."""
    dev = load_runs(results_dir, 'dev')
    hold = load_runs(results_dir, 'holdout')
    wanted = prompts.iterations_of(base)
    candidates = rows_for(hold, wanted)

    print(f"stage B — holdout runs found in {results_dir}")
    print("selection: highest HOLDOUT F1, ties broken by fewer false "
          "positives")
    print("the dev column is the refinement side. It is shown for the record, "
          "and it does not select.\n")
    header = (f"{'version':<10} {'devF1':>7} {'P':>7} {'R':>7} {'F1':>7} "
              f"{'FP':>4} {'FN':>4} {'parse':>6}  run")
    print(header)
    print('-' * len(header))
    for r in rows_for(hold, [base]) + candidates:
        name = r['prompt_version']
        h = r['headline']
        dev_f1 = ((dev.get(name) or {}).get('headline') or {}).get('f1')
        tag = ' (reference)' if name == base else ''
        print(f"{name:<10} {_f(dev_f1)} {_f(h['precision'])} "
              f"{_f(h['recall'])} {_f(h['f1'])} {h['FP']:>4} {h['FN']:>4} "
              f"{r.get('parse_failures', 0):>6}  "
              f"{Path(r['run_dir']).name}{tag}")

    _report_missing(wanted, candidates, 'holdout',
                    "Selection reads the holdout, so every iteration needs a "
                    "holdout pass.")
    no_dev = [n for n in wanted if n not in dev]
    if no_dev:
        print(f"no DEV run: {no_dev}")
        print("  A turn is refined from the previous iteration's dev log, so "
              "turns 1 and 2 need one.")
    if base not in hold:
        print(f"\nno holdout run for the base {base!r}. Run it as the "
              f"reference row, or the iteration log cannot say whether "
              f"refinement helped.")

    winner = select(candidates)
    if winner is None:
        print("\nno scored candidate yet — nothing to select.")
        return 0
    _announce(winner, 'holdout')

    ref = rows_for(hold, [base])
    if ref and ref[0]['headline']['f1'] is not None:
        delta = winner['headline']['f1'] - ref[0]['headline']['f1']
        print(f"\nagainst the base {base} on holdout: F1 {delta:+.3f}")
        if delta <= 0:
            print("  No iteration beat the base. Record that in the "
                  "iteration log — it is a finding about the method.")

    print(f"\nreport: the winner's holdout F1 is a maximum over "
          f"{len(candidates)} iterations, so it is optimistic. Publish every "
          f"row above, not this one alone.")
    return 0


# --- shared ------------------------------------------------------------------

def _report_missing(wanted: List[str], candidates: List[Dict], side: str,
                    why: str) -> None:
    have = {r['prompt_version'] for r in candidates}
    missing = [n for n in wanted if n not in have]
    if missing:
        print(f"\nno {side.upper()} run: {missing}")
        print(f"  {why}")


def _announce(winner: Dict, side: str) -> None:
    print(f"\nwinner: {winner['prompt_version']} "
          f"(F1={_f(winner['headline']['f1']).strip()}, "
          f"FP={winner['headline']['FP']})")
    print(f"  rule : highest {side} F1, ties broken by fewer false positives")
    print(f"  dir  : {winner['run_dir']}")


def _f(v):
    return '    n/a' if v is None else f'{v:>7.3f}'


if __name__ == '__main__':
    sys.exit(main())
