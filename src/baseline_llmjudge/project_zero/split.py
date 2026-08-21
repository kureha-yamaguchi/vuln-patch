"""Freeze the dev/holdout split of the Project Zero population.

THE SPLIT UNIT IS A ROOT-CAUSE GROUP, NOT A PAIR AND NOT A FIX. The Defects4J
splits assign a side per BUG, so every candidate patch of one bug lands on one
side. The counterpart here is the root-cause group, and the reason is stronger
than convention: the two fixes of a pair repair one root cause, and they often
touch one file. A judge tuned on one side of a pair, then scored on the other,
would be scored on code it was tuned against.

HOW A GROUP IS BUILT. Two pairs join one group when they share any identifier —
a CVE id in either role, or a commit on either side. The union is transitive,
so a chain of pairs collapses into one group. Three real examples of why that
matters:

  1. Six pairs share one prior fix, `CVE-2019-13720`. All six render the same
     `fix0` diff.
  2. Five CVE ids act as a prior in one pair and a later in another, so those
     pairs form a chain: `CVE-2016-5128 -> CVE-2022-1096 -> CVE-2022-1232`.
  3. Three Mozilla pairs share one later CVE, `CVE-2020-6820`.

43 pairs collapse to 20 groups.

THE SIDE IS FROZEN PER GROUP, NOT PER ROW. A group keeps its side even when a
later context fetch adds a row to it. That is deliberate, and it is the same
property the Defects4J splits have: the side assignment survives, and only the
leg counts recorded in the split rows go stale. `rows_at_freeze` and
`rows_with_source_at_freeze` are informational. `queue.py` recounts.

BALANCE. Groups are sorted by row count, largest first, and each one goes to
whichever side leaves the smaller total imbalance. The cost of an assignment
adds three gaps between the sides:

    cost = |rows|  +  |positives|  +  sum over codebases of |rows of it|

Each term earns its place, and the second and third were both added after the
one before them produced a bad split:

  1. Rows alone gave 29/30 with priors of 0.55 and 0.68. A prompt tuned
     against one prior over-predicts the positive class against the other, and
     the Defects4J crashing pool already lists a prior shift as a threat.
  2. Rows plus positives fixed the prior, and left the dev side entirely
     `chrome`. Its `by_codebase` breakdown would have held one line, and the
     holdout would have carried every other project on its own. Worse, a
     prompt tuned only on browser C++ would then meet Linux kernel C and a
     Mali driver for the first time on the selection side.
  3. All three terms give the split this module freezes.

A SECOND PASS REFINES THE SWEEP. A hill-climb follows it, over two kinds of
change: move one group to the other side, or swap one dev group for one holdout
group. Each round applies the single change that lowers the cost most, and it
stops when no change lowers it.

Both shapes are needed. Single moves alone got stuck, because the improvement
needed one group to go each way at once.

WHAT THIS FREEZES, MEASURED.

| Side | Groups | Rows at freeze | Prior | Rows with source | Codebases |
|---|---|---|---|---|---|
| dev | 10 | 31 | 0.58 | 27 | chrome 23, apple-webkit 2, qualcomm-android 2 |
| holdout | 10 | 28 | 0.64 | 27 | chrome 22, mozilla-gecko 3, mali-gpu-driver 2 |

The scored priors are 0.56 and 0.63, because the scored rows are the subset
with fetched source. A gap of 0.07 is well inside what the protocol tolerates
elsewhere: the Defects4J crashing pool carries 0.21.

THE HILL-CLIMB IS NOT PROVED OPTIMAL. It is a local search, so a better
assignment may exist. An exhaustive search over 20 groups is 1M assignments,
and it doubles with every group the dataset gains, so it is not the rule here.
If a later dataset makes the remaining gap matter, widen the neighbourhood
rather than enumerate.

A FROZEN SPLIT IS FROZEN. `--force` is required to overwrite one. A split
rewritten after a version has been scored against it would silently change
what that score refers to.
"""
import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

from baseline_llmjudge.project_zero import firewall, queue     # noqa: E402

REPO = Path(__file__).resolve().parents[3]
SPLIT_FILE = REPO / 'suites' / 'splits' / 'project_zero_split.jsonl'

SIDES = ('dev', 'holdout')


def root_cause_groups(pairs: List[firewall.PairRecord]
                      ) -> Dict[str, List[str]]:
    """`{group id: [pair name]}`. Two pairs join when they share an id."""
    parent: Dict[str, str] = {}

    def find(x: str) -> str:
        parent.setdefault(x, x)
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a: str, b: str) -> None:
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb

    for pair in pairs:
        anchor = f'pair:{pair.name}'
        for key in (f'cve:{pair.prior_cve}', f'cve:{pair.later_cve}',
                    f'commit:{pair.fix0_commit}',
                    f'commit:{pair.fix1_commit}'):
            union(anchor, key)

    groups: Dict[str, List[str]] = {}
    for pair in pairs:
        groups.setdefault(find(f'pair:{pair.name}'), []).append(pair.name)
    # Name a group after its first pair, so the id is readable and stable.
    return {min(names): sorted(names) for names in groups.values()}


def build_split(pairs: Optional[List[firewall.PairRecord]] = None
                ) -> Tuple[List[Dict], Dict]:
    """`(rows, stats)`. One row per root-cause group, with its side."""
    pairs = pairs if pairs is not None else firewall.read_pairs()
    groups = root_cause_groups(pairs)

    # Row counts per group. `require_source=False` counts every row the queue
    # rules admit, so a group with no fetched source still gets a side.
    all_rows, _ = queue.build_queue(require_source=False)
    with_source, _ = queue.build_queue(require_source=True)
    sourced = {(r.pair, r.which) for r in with_source}

    group_of = {name: gid for gid, names in groups.items() for name in names}
    per_group: Dict[str, Dict] = {
        gid: {'rows': 0, 'overfitting': 0, 'correct': 0, 'with_source': 0,
              'codebases': Counter()}
        for gid in groups}
    for row in all_rows:
        slot = per_group[group_of[row.pair]]
        slot['rows'] += 1
        slot[row.label] += 1
        slot['codebases'][row.fix.codebase] += 1
        if (row.pair, row.which) in sourced:
            slot['with_source'] += 1

    # Largest group first. A large group placed late would unbalance whichever
    # side it landed on, and no later group could correct it.
    order = sorted(groups, key=lambda g: (-per_group[g]['rows'], g))
    side_rows = {s: 0 for s in SIDES}
    side_pos = {s: 0 for s in SIDES}
    rows: List[Dict] = []

    assigned: Dict[str, str] = {}
    for gid in order:
        counts = per_group[gid]
        # The cost of a placement is read from the partial assignment, so the
        # sweep uses the same function the refinement does.
        def cost_if(side: str, gid: str = gid) -> Tuple[int, str]:
            trial = dict(assigned)
            trial[gid] = side
            return _cost(trial, per_group), side

        side = min(SIDES, key=cost_if)
        assigned[gid] = side
        side_rows[side] += counts['rows']
        side_pos[side] += counts['overfitting']

    assigned = _refine(assigned, per_group)

    rows = [{
        'group': gid,
        'side': assigned[gid],
        'pairs': groups[gid],
        'rows_at_freeze': per_group[gid]['rows'],
        'overfitting_at_freeze': per_group[gid]['overfitting'],
        'correct_at_freeze': per_group[gid]['correct'],
        'rows_with_source_at_freeze': per_group[gid]['with_source'],
    } for gid in sorted(groups)]

    rows.sort(key=lambda r: (r['side'], r['group']))
    return rows, _stats(rows)


def _cost(assigned: Dict[str, str], per_group: Dict[str, Dict]) -> int:
    """The three imbalance gaps, added. Lower is better.

    Rows, positives, and codebases. The third term stops one side taking every
    project: without it the dev side came out entirely `chrome`, so its
    `by_codebase` breakdown held one row and the holdout carried every other
    project on its own."""
    rows = {s: 0 for s in SIDES}
    pos = {s: 0 for s in SIDES}
    books: Dict[str, Counter] = {s: Counter() for s in SIDES}
    for gid, side in assigned.items():
        rows[side] += per_group[gid]['rows']
        pos[side] += per_group[gid]['overfitting']
        books[side].update(per_group[gid]['codebases'])
    every = set(books[SIDES[0]]) | set(books[SIDES[1]])
    spread = sum(abs(books[SIDES[0]][name] - books[SIDES[1]][name])
                 for name in every)
    return (abs(rows[SIDES[0]] - rows[SIDES[1]])
            + abs(pos[SIDES[0]] - pos[SIDES[1]])
            + spread)


def _refine(assigned: Dict[str, str],
            per_group: Dict[str, Dict]) -> Dict[str, str]:
    """Improve the assignment while any single move or swap lowers the cost.

    THE NEIGHBOURHOOD NEEDS BOTH SHAPES. Single moves alone got stuck: the
    greedy sweep left a cost of 5, and no one move lowered it, because the
    improvement needs one group to go each way at once. Adding the swap takes
    it to 3. It does not reach the exhaustive optimum of 1, and the module
    docstring records why that is accepted.

    A move that empties a side is refused: two sides are the point. The change
    applied is the one with the lowest cost, and the group names break a tie,
    so the result does not depend on dictionary order."""
    assigned = dict(assigned)
    best_cost = _cost(assigned, per_group)
    other = {SIDES[0]: SIDES[1], SIDES[1]: SIDES[0]}
    while True:
        candidates = []
        for gid in sorted(assigned):
            trial = dict(assigned)
            trial[gid] = other[assigned[gid]]
            if len(set(trial.values())) < len(SIDES):
                continue
            candidates.append((_cost(trial, per_group), (gid,), trial))
        here = [g for g in sorted(assigned) if assigned[g] == SIDES[0]]
        there = [g for g in sorted(assigned) if assigned[g] == SIDES[1]]
        for a in here:
            for b in there:
                trial = dict(assigned)
                trial[a], trial[b] = assigned[b], assigned[a]
                candidates.append((_cost(trial, per_group), (a, b), trial))
        if not candidates:
            return assigned
        cost, _names, trial = min(candidates, key=lambda c: (c[0], c[1]))
        if cost >= best_cost:
            return assigned
        assigned, best_cost = trial, cost


def _stats(rows: List[Dict]) -> Dict:
    out: Dict = {'groups': len(rows), 'pairs': sum(len(r['pairs'])
                                                   for r in rows),
                 'sides': {}}
    for side in SIDES:
        mine = [r for r in rows if r['side'] == side]
        n = sum(r['rows_at_freeze'] for r in mine)
        pos = sum(r['overfitting_at_freeze'] for r in mine)
        out['sides'][side] = {
            'groups': len(mine),
            'rows_at_freeze': n,
            'overfitting_at_freeze': pos,
            'correct_at_freeze': n - pos,
            'rows_with_source_at_freeze': sum(
                r['rows_with_source_at_freeze'] for r in mine),
            'positive_prior_at_freeze': (pos / n) if n else None,
        }
    return out


def load(path: Path = SPLIT_FILE) -> Dict[str, str]:
    """`{pair name: side}` from the frozen split, or `{}` when absent."""
    if not path.exists():
        return {}
    out: Dict[str, str] = {}
    for line in path.read_text().splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        for name in row['pairs']:
            out[name] = row['side']
    return out


def print_stats(stats: Dict, out=sys.stdout) -> None:
    print(f"groups : {stats['groups']}   pairs: {stats['pairs']}", file=out)
    for side in SIDES:
        s = stats['sides'][side]
        prior = s['positive_prior_at_freeze']
        print(f"  {side:<8} {s['groups']:2d} groups, "
              f"{s['rows_at_freeze']:2d} rows "
              f"(-o {s['overfitting_at_freeze']}, "
              f"-c {s['correct_at_freeze']}, "
              f"prior {prior:.2f})   "
              f"{s['rows_with_source_at_freeze']:2d} with source"
              if prior is not None else f"  {side}: empty", file=out)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out', type=Path, default=SPLIT_FILE,
                    help=f'default: {SPLIT_FILE}')
    ap.add_argument('--force', action='store_true',
                    help='overwrite a split that is already frozen')
    ap.add_argument('--dry_run', action='store_true',
                    help='print the split and write nothing')
    args = ap.parse_args()

    if args.out.exists() and not (args.force or args.dry_run):
        print(f'REFUSING: {args.out} is already frozen. A split rewritten '
              f'after a version was scored against it would silently change '
              f'what that score refers to. Pass --force to overwrite.',
              file=sys.stderr)
        return 2

    rows, stats = build_split()
    print_stats(stats)
    print()
    for row in rows:
        print(f"  {row['side']:<8} {row['group'][:44]:<44} "
              f"{row['rows_at_freeze']:2d} rows, "
              f"{len(row['pairs'])} pair(s)")

    if args.dry_run:
        print('\n--dry_run — nothing written.')
        return 0
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text('\n'.join(json.dumps(r) for r in rows) + '\n')
    print(f'\nWrote {args.out}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
