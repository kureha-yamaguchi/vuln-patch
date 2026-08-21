"""Build the scored population of the Project Zero baseline.

The counterpart of `java/dataset/build_split_queue.py`. That script is invoked
as a subprocess by `evaluate.py`, because the pipeline's own evaluator runs the
same script and the two populations must not drift apart. No Project Zero
pipeline evaluator exists yet, so this one is a library that `evaluate`
imports. There is no second reader to keep in step with.

THE UNIT IS ONE FIX, NOT ONE PAIR. 43 pair directories hold only 35 distinct
`fix0` commits, and one prior CVE carries six pairs on its own. Every pair of
that CVE renders the same diff, so a per-pair queue would enter one fix into
the confusion matrix six times and pay for it five times over.

THE TWO CLASSES.

  * Positive, `overfitting`: every distinct `fix0`. It shipped, and a later CVE
    proved that it left a sibling bug.
  * Negative, `correct`: every distinct `fix1`, less two exclusions.

TWO EXCLUSIONS ON THE NEGATIVE CLASS, and each one removes a fix that is known
to be incomplete:

  1. A `fix1` commit that is also a `fix0` somewhere in the dataset. Five CVE
     ids of this dataset act in both roles.
  2. A `fix1` whose CVE is a prior CVE somewhere. Same rule, matched on the
     identifier rather than the commit, because the two sides of a pair can
     name one CVE through two different commits.

A fix that left a sibling bug cannot serve as an example of one that did not.

THE CONFOUND, STATED HERE BECAUSE IT IS A PROPERTY OF THIS POPULATION. The
negative class is the later fix of the same pair. A later fix is later in time,
and it had more scrutiny. `firewall` removes the dates, the identifiers and
the commit message, so no token states the order. It cannot remove a difference
in coding style. `evaluate` therefore reports a diff-size proxy control
beside the model's own score, and the README says what that control means.
"""
import argparse
import hashlib
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

from baseline_llmjudge.project_zero import bugkind, firewall  # noqa: E402


@dataclass(frozen=True)
class QueueRow:
    """One scored fix, plus the selector fields that found it."""
    fix: firewall.Fix
    pair: str          # selector only
    which: str         # selector only

    @property
    def label(self) -> str:
        return self.fix.label


def build_queue(*, require_source: bool = True,
                bug_kind: Optional[str] = None,
                kinds: Optional[Dict[str, str]] = None,
                side: Optional[str] = None,
                sides: Optional[Dict[str, str]] = None
                ) -> Tuple[List[QueueRow], Dict]:
    """`(rows, stats)`. One row per distinct fix commit.

    `require_source` drops a fix whose context fetch produced no file. Such a
    fix would carry the diff and nothing else, so it would answer a weaker
    question than the rest of the population.

    `bug_kind` keeps one pool. It needs `kinds`, the `{fix_id: kind}` map that
    `bugkind.load()` returns.

    `side` keeps one side of the frozen split. It needs `sides`, the
    `{pair name: side}` map that `split.load()` returns. The side is frozen per
    root-cause group, so both fixes of a pair always land on one side."""
    pairs = firewall.read_pairs()
    ever_fix0 = {p.fix0_commit for p in pairs if p.fix0_commit}
    ever_prior = {p.prior_cve for p in pairs if p.prior_cve}

    rows: List[QueueRow] = []
    seen: Dict[str, str] = {}         # fix_id -> diff digest
    dropped: Dict[str, int] = {}
    diff_mismatch: List[str] = []

    def drop(reason: str) -> None:
        dropped[reason] = dropped.get(reason, 0) + 1

    for pair in pairs:
        if side is not None and (sides or {}).get(pair.name) != side:
            # Counted once per pair, not once per side, because the split
            # assigns a pair whole.
            drop(f'not_on_the_{side}_side')
            continue
        for which in firewall.WHICH:
            commit = pair.commit(which)
            if not commit:
                drop('no_commit_id')
                continue
            if which == 'fix1':
                if commit in ever_fix0:
                    drop('negative_is_a_prior_fix_elsewhere')
                    continue
                if pair.later_cve in ever_prior:
                    drop('negative_cve_is_a_prior_cve_elsewhere')
                    continue
            try:
                fix = firewall.clean_view(pair, which)
            except firewall.FixUnavailable as exc:
                drop(exc.status)
                continue

            digest = hashlib.sha256(fix.diff.encode()).hexdigest()
            if fix.fix_id in seen:
                # Two pairs share this fix. They should hold the same patch
                # file, so a mismatch is a dataset problem worth naming.
                if seen[fix.fix_id] != digest:
                    diff_mismatch.append(f'{fix.fix_id} ({pair.name}/{which})')
                drop('duplicate_commit_collapsed')
                continue

            if require_source and not fix.sources:
                drop('no_fetched_source')
                continue
            if bug_kind is not None:
                if (kinds or {}).get(fix.fix_id) != bug_kind:
                    drop(f'not_{bug_kind}')
                    continue

            seen[fix.fix_id] = digest
            rows.append(QueueRow(fix=fix, pair=pair.name, which=which))

    return rows, _stats(pairs, rows, dropped, diff_mismatch)


def _stats(pairs, rows: List[QueueRow], dropped: Dict[str, int],
           diff_mismatch: List[str]) -> Dict:
    positives = [r for r in rows if r.label == 'overfitting']
    negatives = [r for r in rows if r.label == 'correct']
    codebases: Dict[str, int] = {}
    for r in rows:
        codebases[r.fix.codebase] = codebases.get(r.fix.codebase, 0) + 1
    return {
        'pairs_read': len(pairs),
        'rows': len(rows),
        'overfitting': len(positives),
        'correct': len(negatives),
        'positive_class_prior': (len(positives) / len(rows)) if rows else None,
        'dropped': dropped,
        'codebase_mix': dict(sorted(codebases.items(),
                                    key=lambda kv: (-kv[1], kv[0]))),
        'duplicate_commits_with_differing_diffs': diff_mismatch,
    }


def print_stats(stats: Dict, out=sys.stderr) -> None:
    p = lambda s: print(s, file=out)                      # noqa: E731
    p(f"pairs read     : {stats['pairs_read']}")
    p(f"rows queued    : {stats['rows']}  "
      f"(-o overfitting={stats['overfitting']}, "
      f"-c correct={stats['correct']})")
    prior = stats['positive_class_prior']
    p(f"positive prior : {prior:.2f}" if prior is not None
      else "positive prior : n/a")
    p("dropped:")
    for reason in sorted(stats['dropped']):
        p(f"  {stats['dropped'][reason]:3d}  {reason}")
    p("codebase mix:")
    for name, n in stats['codebase_mix'].items():
        p(f"  {n:3d}  {name}")
    if stats['duplicate_commits_with_differing_diffs']:
        p("WARNING: one commit, two different patch files: "
          + ', '.join(stats['duplicate_commits_with_differing_diffs']))
    if stats['rows'] < 18:
        p(f"\nNOTE: {stats['rows']} rows. Read the README's limit section "
          f"before you quote any F1 from this population.")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out', type=Path,
                    help='write the queue here instead of stdout')
    ap.add_argument('--side', default=None, choices=['dev', 'holdout'],
                    help='keep one side of the frozen split; needs '
                         'project_zero_split.jsonl')
    ap.add_argument('--bug_kind', default=None,
                    choices=[bugkind.CRASHING, bugkind.SEMANTIC],
                    help='keep one pool; needs bug_kind.jsonl')
    ap.add_argument('--allow_missing_source', action='store_true',
                    help='keep a fix whose context fetch produced no file')
    ap.add_argument('--stats_json', type=Path,
                    help='also write the stats block here')
    args = ap.parse_args()

    kinds = bugkind.load() if args.bug_kind else None
    if args.bug_kind and not kinds:
        print(f'REFUSING: --bug_kind needs {bugkind.DEFAULT_OUT}. '
              f'Run `uv run -m baseline_llmjudge.project_zero.bugkind` first.',
              file=sys.stderr)
        return 2

    # Imported here, not at module scope. `split.py` needs the queue rules to
    # count the rows of a group, so a module-level import either way round
    # would be a cycle. `build_queue` takes `sides` as a plain parameter, so
    # only this CLI needs the split file at all.
    from baseline_llmjudge.project_zero import split

    sides = split.load() if args.side else None
    if args.side and not sides:
        print(f'REFUSING: --side needs {split.SPLIT_FILE}. Run '
              f'`uv run -m baseline_llmjudge.project_zero.split` first.',
              file=sys.stderr)
        return 2

    rows, stats = build_queue(
        require_source=not args.allow_missing_source,
        bug_kind=args.bug_kind, kinds=kinds,
        side=args.side, sides=sides)

    lines = [f"{'-o' if r.label == 'overfitting' else '-c'} {r.pair}|{r.which}"
             for r in rows]
    if args.out:
        args.out.write_text('\n'.join(lines) + '\n')
    else:
        print('\n'.join(lines))
    if args.stats_json:
        args.stats_json.write_text(json.dumps(stats, indent=2))
    print_stats(stats)
    return 0


if __name__ == '__main__':
    sys.exit(main())
