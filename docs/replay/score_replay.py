#!/usr/bin/env python3
"""Score a verifier_replay summary.md against the fixture's gold labels.

Reproduces the iteration-1 numbers: over-kill (gold=SOUND dropped) and leak
(gold=UNSOUND kept), plus the IDENT annotation that separates the fd_prior
gate-fidelity artifact from genuine over-kill.

  python3 runs-archive/replay/score_replay.py <summary.md> [fixture.jsonl]
"""
import json
import re
import sys

IDENT_MARKERS = ('identical', 'both builds', 'patch-failed-to-fix',
                 'fires-on-buggy', 'same check on the buggy')


def main():
    summary = sys.argv[1]
    fixture = sys.argv[2] if len(sys.argv) > 2 else 'tests/fixtures/cases228.jsonl'
    by_row = {}
    for line in open(fixture):
        c = json.loads(line)
        by_row[c['provenance'].get('inventory_row')] = c

    rows = []
    pat = re.compile(r'\| (\S+) \| (\w+) \| (\d)/1 \| \[[^\]]*row (\d+)\] '
                     r'gold=(\S+) class=(\S+(?: \(\S+\))?) legoutcome=(\w+)')
    for line in open(summary):
        m = pat.match(line)
        if not m:
            continue
        case, label, kept, row, gold, cls, out = m.groups()
        fx = by_row.get(int(row), {})
        ev = (fx.get('concrete_evidence') or '').lower()
        rows.append(dict(case=case, label=label, kept=int(kept), row=int(row),
                         gold=gold, cls=cls, out=out,
                         ident=any(k in ev for k in IDENT_MARKERS)))

    sound = [r for r in rows if r['gold'] == 'SOUND']
    unsound = [r for r in rows if r['gold'] == 'UNSOUND']
    overkill = [r for r in sound if not r['kept']]
    leak = [r for r in unsound if r['kept']]
    # fd_prior artifact: production's J-ladder keeps IDENT-carrying catches via
    # the trigger-input exemption (fd_prior=True); replay passes None, so 5C
    # freshly asks family_duty and drops them. Not a regression — a gate bug.
    artifact = [r for r in overkill if r['ident']]
    genuine = [r for r in overkill if not r['ident']]

    print(f"scored rows: {len(rows)}  (SOUND {len(sound)} / UNSOUND {len(unsound)})")
    print(f"over-kill (gold SOUND dropped): {len(overkill)}")
    print(f"  - fd_prior gate artifact (IDENT-carrying): {len(artifact)}")
    print(f"  - genuine over-kill:                       {len(genuine)}")
    print(f"leak (gold UNSOUND kept): {len(leak)}"
          f"  [on correct legs: {sum(1 for r in leak if r['label'] == 'correct')}]")
    for title, group in (("FD_PRIOR ARTIFACT", artifact),
                         ("GENUINE OVER-KILL", genuine),
                         ("LEAKS", leak)):
        print(f"\n=== {title} ===")
        for r in group:
            print(f"  row {r['row']:>3} {r['case'][:50]:50} cls={r['cls']} out={r['out']}")


if __name__ == '__main__':
    main()
