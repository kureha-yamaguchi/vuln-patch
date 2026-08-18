"""Compare dev runs and name the winner, for either stage of the protocol.

Stage A picks a design out of three blind runs. Stage B picks an iteration out
of three refinement runs. Both use the same rule, so both use this module:

    highest dev F1, ties broken by the lower false-positive count.

Usage (from src/):
    # stage A — the three blind designs
    uv run -m baseline_llmjudge.compare --stage A

    # stage B — the iterations of the stage-A winner
    uv run -m baseline_llmjudge.compare --stage B --base v2

`--stage B` reports the base design's own score too, as a reference row. If no
iteration beats it, the protocol still selects the best iteration — but a
regression that large is worth seeing before the holdout is spent on it.
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


def load_runs(results_dir: Path, side: str = 'dev') -> List[Dict]:
    """Every scored run under `results_dir`, newest last.

    When one version was run more than once, the latest run wins — a rerun of
    the same frozen text supersedes the earlier one."""
    runs: Dict[str, Dict] = {}
    for path in sorted(results_dir.glob(f'llmjudge_{side}_*/summary.json')):
        s = json.loads(path.read_text())
        s['run_dir'] = str(path.parent)
        runs[s['prompt_version']] = s
    return list(runs.values())


def select(runs: List[Dict]) -> Optional[Dict]:
    """Highest F1, ties broken by fewer false positives."""
    scored = [r for r in runs if (r.get('headline') or {}).get('f1') is not None]
    if not scored:
        return None
    return sorted(scored, key=lambda r: (-r['headline']['f1'],
                                         r['headline']['FP']))[0]


def rows_for(runs: List[Dict], names: List[str]) -> List[Dict]:
    by_name = {r['prompt_version']: r for r in runs}
    return [by_name[n] for n in names if n in by_name]


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

    runs = load_runs(Path(args.results_dir))
    if args.stage == 'A':
        wanted = list(prompts.BASE_VERSIONS)
        reference: List[str] = []
    else:
        wanted = prompts.iterations_of(args.base)
        reference = [args.base]

    candidates = rows_for(runs, wanted)
    missing = [n for n in wanted if n not in
               {r['prompt_version'] for r in candidates}]

    print(f"stage {args.stage} — dev runs found in {args.results_dir}\n")
    header = (f"{'version':<10} {'P':>6} {'R':>6} {'F1':>6} {'FP':>4} "
              f"{'FN':>4} {'parse':>6}  run")
    print(header)
    print('-' * len(header))
    for r in rows_for(runs, reference) + candidates:
        h = r['headline']
        tag = ' (reference)' if r['prompt_version'] in reference else ''
        print(f"{r['prompt_version']:<10} {_f(h['precision'])} "
              f"{_f(h['recall'])} {_f(h['f1'])} {h['FP']:>4} {h['FN']:>4} "
              f"{r.get('parse_failures', 0):>6}  "
              f"{Path(r['run_dir']).name}{tag}")

    if missing:
        print(f"\nNOT YET RUN: {missing}")
        print("The winner is only meaningful once every candidate has run.")

    winner = select(candidates)
    if winner is None:
        print("\nno scored candidate yet — nothing to select.")
        return 0

    print(f"\nwinner: {winner['prompt_version']} "
          f"(F1={_f(winner['headline']['f1']).strip()}, "
          f"FP={winner['headline']['FP']})")
    print(f"  rule : highest dev F1, ties broken by fewer false positives")
    print(f"  dir  : {winner['run_dir']}")

    if args.stage == 'A':
        print(f"\nnext: refine {winner['prompt_version']} three times. Read its "
              f"errors first:")
        print(f"  uv run -m baseline_llmjudge.errors "
              f"--records {winner['run_dir']}/records.jsonl")
    else:
        ref = rows_for(runs, reference)
        if ref and ref[0]['headline']['f1'] is not None:
            delta = winner['headline']['f1'] - ref[0]['headline']['f1']
            print(f"\nagainst the base {ref[0]['prompt_version']}: "
                  f"F1 {delta:+.3f}")
            if delta <= 0:
                print("  No iteration beat the base. Record that in the "
                      "iteration log before the holdout run.")
        print(f"\nnext: freeze {winner['prompt_version']}, then run the "
              f"holdout ONCE:")
        print(f"  uv run -m baseline_llmjudge.evaluate --side holdout "
              f"--prompt_version {winner['prompt_version']} --confirm_holdout")
    return 0


def _f(v):
    return '   n/a' if v is None else f'{v:>6.3f}'


if __name__ == '__main__':
    sys.exit(main())
