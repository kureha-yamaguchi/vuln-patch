#!/usr/bin/env python3
"""J1 — offline audit of the two judges (soundness + attribution) over the
archived run traces. No VM, no LLM calls: it reads runs-archive/runs/*/*/
and cross-tabulates every judge DECISION against the leg's GROUND TRUTH.

Why: two independent lines this cycle (the attribution collapse and the
focused-synthesis FPs) showed the judges are the precision/recall ceiling.
Every judge change so far was validated on 2-5 legs by intuition. This
measures the judges on everything we have already run.

Ground truth per leg (from result.jsonl):
  label = correct | overfitting ; crashed_on_patch (kept a conviction)
  -> TP overfit+kept, FN overfit+not-kept, FP correct+kept, TN correct+not.

A judge decision is 'error-aligned' when it points the wrong way for the
leg's truth:
  - soundness SOUND on a CORRECT leg  -> risks an FP (should usually be UNSOUND)
  - soundness UNSOUND on an OVERFIT leg -> risks a FN (killed a possible catch)
  - attribution NOT_ATTRIBUTED on an OVERFIT leg -> risks a FN
  - attribution ATTRIBUTED on a CORRECT leg -> risks an FP
These are HEURISTIC alignments (a correct leg SHOULD have every check ruled
UNSOUND/NOT_ATTRIBUTED; an overfit leg needs only ONE SOUND+ATTRIBUTED to
catch). So the per-decision counts below are a RATE signal, and the
per-leg OUTCOME table is the authoritative bottom line.
"""
import json
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.join(os.path.dirname(__file__), '..', 'runs-archive', 'runs')
ROOT = os.path.abspath(ROOT)

_VERDICT = re.compile(r'^VERDICT: (SOUND|UNSOUND)$')
_ATTRIB = re.compile(r'^ATTRIBUTION: (ATTRIBUTED|NOT_ATTRIBUTED|INCONCLUSIVE)$')
_WHY = re.compile(r'^WHY:\s*(.*)$')


def parse_decisions(trace_path):
    """Return (soundness, attribution) lists of (verdict, why) from a trace.
    Matches only EXACT verdict lines (the prompt templates carry
    'VERDICT: SOUND | UNSOUND' with the pipe, so they never match)."""
    sound, attrib = [], []
    try:
        lines = open(trace_path, encoding='utf-8', errors='replace').read().splitlines()
    except OSError:
        return sound, attrib
    for i, ln in enumerate(lines):
        s = ln.strip()
        mv = _VERDICT.match(s)
        ma = _ATTRIB.match(s)
        if not (mv or ma):
            continue
        why = ''
        for j in range(i + 1, min(i + 6, len(lines))):
            mw = _WHY.match(lines[j].strip())
            if mw:
                why = mw.group(1)[:200]
                break
        (sound if mv else attrib).append(
            ((mv or ma).group(1), why))
    return sound, attrib


def leg_truth(result_path):
    try:
        r = json.load(open(result_path, encoding='utf-8'))
    except (OSError, ValueError):
        return None
    label = (r.get('label') or '').lower()
    if label.startswith('overfit'):
        truth = 'overfit'
    elif label.startswith('correct'):
        truth = 'correct'
    else:
        return None
    kept = bool(r.get('crashed_on_patch'))
    status = r.get('status', '')
    if status not in ('evaluated',) and not kept and status:
        # unscored legs (no_harnesses etc.) — record but flag
        pass
    outcome = {('overfit', True): 'TP', ('overfit', False): 'FN',
               ('correct', True): 'FP', ('correct', False): 'TN'}[(truth, kept)]
    return {'truth': truth, 'kept': kept, 'outcome': outcome,
            'status': status}


def main():
    batches = sorted(d for d in os.listdir(ROOT)
                     if os.path.isdir(os.path.join(ROOT, d)))
    # per (truth, verdict) tallies
    s_tally = defaultdict(int)      # ('correct','SOUND') etc.
    a_tally = defaultdict(int)
    error_decisions = []           # judge decisions that point wrong for truth
    outcome_tally = defaultdict(int)
    n_legs = 0
    per_batch = defaultdict(lambda: defaultdict(int))

    for b in batches:
        bdir = os.path.join(ROOT, b)
        for leg in sorted(os.listdir(bdir)):
            ldir = os.path.join(bdir, leg)
            rp = os.path.join(ldir, 'result.jsonl')
            tp = os.path.join(ldir, 'trace.md')
            if not (os.path.isfile(rp) and os.path.isfile(tp)):
                continue
            truth = leg_truth(rp)
            if not truth:
                continue
            n_legs += 1
            outcome_tally[truth['outcome']] += 1
            per_batch[b][truth['outcome']] += 1
            sound, attrib = parse_decisions(tp)
            for v, why in sound:
                s_tally[(truth['truth'], v)] += 1
                # error-aligned: SOUND on correct, UNSOUND on overfit
                if (truth['truth'] == 'correct' and v == 'SOUND') or \
                   (truth['truth'] == 'overfit' and v == 'UNSOUND'):
                    error_decisions.append(
                        ('soundness', b, leg, truth['truth'],
                         truth['outcome'], v, why))
            for v, why in attrib:
                a_tally[(truth['truth'], v)] += 1
                if (truth['truth'] == 'correct' and v == 'ATTRIBUTED') or \
                   (truth['truth'] == 'overfit' and v == 'NOT_ATTRIBUTED'):
                    error_decisions.append(
                        ('attribution', b, leg, truth['truth'],
                         truth['outcome'], v, why))

    print(f"J1 JUDGE AUDIT — {n_legs} legs across {len(batches)} batches\n")
    print("OUTCOME totals (authoritative):")
    tp, fn = outcome_tally['TP'], outcome_tally['FN']
    fp, tn = outcome_tally['FP'], outcome_tally['TN']
    print(f"  TP={tp} FN={fn} FP={fp} TN={tn}")
    if tp + fp:
        print(f"  precision={tp/(tp+fp):.2f}  recall={tp/(tp+fn):.2f}")
    print()

    print("SOUNDNESS judge — verdicts by ground truth:")
    print(f"  on CORRECT legs: SOUND={s_tally[('correct','SOUND')]:3d}"
          f"  UNSOUND={s_tally[('correct','UNSOUND')]:3d}"
          f"   (SOUND here is the FP-risk cell)")
    print(f"  on OVERFIT legs: SOUND={s_tally[('overfit','SOUND')]:3d}"
          f"  UNSOUND={s_tally[('overfit','UNSOUND')]:3d}"
          f"   (UNSOUND here is the FN-risk cell)")
    print()
    print("ATTRIBUTION judge — verdicts by ground truth:")
    for t in ('correct', 'overfit'):
        print(f"  on {t.upper():7s} legs:"
              f"  ATTRIBUTED={a_tally[(t,'ATTRIBUTED')]:3d}"
              f"  NOT_ATTRIBUTED={a_tally[(t,'NOT_ATTRIBUTED')]:3d}"
              f"  INCONCLUSIVE={a_tally[(t,'INCONCLUSIVE')]:3d}")
    print()

    print(f"ERROR-ALIGNED DECISIONS ({len(error_decisions)}) — a judge call"
          " pointing the wrong way for the leg's truth:")
    # group by (stage, truth, verdict) and show a few WHYs each
    grp = defaultdict(list)
    for stage, b, leg, truth, outcome, v, why in error_decisions:
        grp[(stage, truth, v)].append((b, leg, outcome, why))
    for key in sorted(grp):
        stage, truth, v = key
        rows = grp[key]
        print(f"\n  [{stage}] {v} on {truth} legs — {len(rows)} times:")
        for b, leg, outcome, why in rows[:4]:
            print(f"    {b[:22]:22s} {leg[:34]:34s} ({outcome}) {why[:90]}")
        if len(rows) > 4:
            print(f"    ... +{len(rows)-4} more")

    # J1's headline: cluster the soundness-on-correct (FP-risk) WHYs by
    # keyword to see if they share a fixable mechanism.
    print("\nFP-RISK CLUSTERING (soundness SOUND on correct legs) — by WHY"
          " keyword:")
    kw = defaultdict(int)
    for stage, b, leg, truth, outcome, v, why in error_decisions:
        if stage != 'soundness' or truth != 'correct':
            continue
        w = why.lower()
        for k, pats in (('rounding/tolerance', ('round', 'ulp', 'toler',
                                                'epsilon', 'floating')),
                        ('bounds/support', ('bound', 'support', 'range')),
                        ('lazy/state', ('cache', 'lazy', 'reader', 'mutat')),
                        ('exception-class', ('exception', 'throw', 'reject')),
                        ('magnitude', ('magnitude', 'overflow', 'large',
                                       'nan'))):
            if any(p in w for p in pats):
                kw[k] += 1
                break
        else:
            kw['other'] += 1
    for k, n in sorted(kw.items(), key=lambda x: -x[1]):
        print(f"  {k:20s} {n}")


if __name__ == '__main__':
    main()
