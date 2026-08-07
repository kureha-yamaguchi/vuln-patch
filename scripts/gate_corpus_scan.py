#!/usr/bin/env python3
"""8.25 reach measurement: the verdict gate over every RECORDED firing.

Post-roll-13 (the lottery TN: the gate never ran, so the roll tested
nothing). Instead of buying stochastic draws, this scans the ladder's
whole recorded corpus and gates every value-bearing firing on both doors,
using the admitted Math-65 reference values (two independent generations
converged on them digit-for-digit).

Measured 2026-08-07 (13 rolls, ladder1b..ladder1n):
  * REPLAY door (where phase 1 is wired):  31 firings, 0 void — the
    synthesized-relation replays fire at fuzz inputs; values never
    coincide with test state.
  * HARNESS door (NOT wired — Spec-K parity gap, now with measured
    stakes): 18 value-bearing firings on the patched build, 2 VOID —
    ladder1g `circle-dense-chi-formula`, ladder1k
    `constant-weight-rms-chi`. The fuzz harness replays the failing
    test's scenario, so its firings carry test-state-coincident values:
    the door with reach is the unwired one.
  * Neither roll flips outright; each had one more patched firing:
    ladder1g's prints `getChiSquare()=<the reference's exact value>` but
    the `()` on the key defeats parsing (a mechanical accommodation
    away); ladder1k's prints only lhs/rhs with no observable names (the
    honest abstain only the 8.4 firing-state extension can reach).

Run from repo root:  python3 scripts/gate_corpus_scan.py
"""
import glob
import json
import re
import sys

sys.path.insert(0, 'src')
sys.path.insert(0, 'tests')

from java.relations.reference_impl import reference_verdict_gate   # noqa: E402
from test_reference_statetwin import ADMITTED_M65                  # noqa: E402


def firings(section):
    # Two recorded shapes: harness firings carry an `[oracle:name]` tag;
    # replay-on-patched events print `FIRED ... relation <name> violated`.
    # Normalized to the oracle tag where present so the two patterns never
    # double-count one firing. No value filter: a valueless firing is the
    # gate's abstain to report, not the scan's to hide.
    seen = set()
    for pat in (r'\[oracle:[^\]]+\][^\n\\"]{0,300}', r'FIRED[^\n]{0,320}'):
        for m in re.finditer(pat, section):
            s = m.group(0)
            if '[oracle:' in s:
                s = s[s.find('[oracle:'):]
            if s not in seen:
                seen.add(s)
                yield s


def main():
    doors = {'replay': [], 'harness-patched': []}
    for d in sorted(glob.glob('runs-archive/runs/ladder1*/01_*')):
        roll = d.split('/')[2].split('_')[0]
        text = open(d + '/trace.md', errors='ignore').read()
        for sec in re.split(r'\n(?=## \[\d+\])', text):
            head = sec.split('\n')[0]
            door = ('replay' if 'replay-on-patched' in head else
                    'harness-patched' if 'patched-fuzz' in head else None)
            if not door:
                continue
            for f in firings(sec):
                v, why = reference_verdict_gate(f, ADMITTED_M65)
                doors[door].append((roll, f.split(']')[0][8:], v, f))
    for door, rows in doors.items():
        vs = [v for _, _, v, _ in rows]
        print(f'{door:16s} n={len(vs):3d} void={vs.count("void"):2d} '
              f'abstain={vs.count("abstain")}')
        for roll, name, v, f in rows:
            if v == 'void':
                print(f'    VOID {roll} {name[:44]}')


if __name__ == '__main__':
    main()
