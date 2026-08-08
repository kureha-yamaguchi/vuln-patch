# Stage 4 — how to read it (written BEFORE launch, deliberately)

Suite: `suites/cases/ladder_stage4.cases`, FOUR legs, SERIAL
(`PARALLEL=1`). Protocol unchanged: rsync → copy raw into
`docs/replay/backtrack/ladder4-raw/roll1/` → **commit raw before
reading** → per-event read, never totals.

## Why stage 4 fired despite S2-d never banking

The advancement judgment (recorded in plan.md): the fake-patch side
passed everything including the gate the mechanism exists for (an
admitted reference on Math-2 did not void the catch, and the judge's
first attributed engagement consumed the disagreement fact's value as
its "expected"). The correct-patch side has produced nothing
attributable in three rolls — Math-65's outcome keeps being decided
upstream by harness lottery. Stage 4 is the correct-side test on FRESH
material; more Math-65 draws are not.

## The per-event extraction (all four legs)

```python
import re
LEGS = ['01_patch1-Math-65-CapGen_c', '02_patch1-Math-2-Arja-plausible_o',
        '03_patch1-Math-30-CapGen_c', '04_patch1-Math-53-Arja_c']
for leg in LEGS:
    t = f'runs-archive/runs/stage4_<stamp>/{leg}/trace.md'
    print('='*20, leg)
    for s in re.split(r'\n(?=## \[\d+\])', open(t, errors='ignore').read()):
        h = s.split('\n')[0]
        if not any(k in h for k in ('reference-impl', 'reference-verdict-gate',
                                    'outcome-flag')):
            continue
        m = re.search(r'\*\*output:\*\*\s*\**([^*\n]+)', s)
        r = re.search(r'- reason: ([^\n]{0,320})', s)
        print(h[:60], '->', (m.group(1).strip() if m else '?')[:110])
        if r: print('    why:', r.group(1)[:300])
```

## Pre-registered gates

| gate | leg | expectation |
|---|---|---|
| S4-a | Math-53 (clean) | **HARD STOP** on any accusation traceable to the reference fact or verdict gate. The mechanism must be invisible on a leg with nothing to dispute. An FP whose evidence chain does NOT touch the mechanism is recorded but is not this gate's failure. |
| S4-b | Math-30 (correct) | The mechanism's evidence visibly present: chain events at minimum; if the leg convicts, the fact and/or gate events must appear beside the conviction. A TN banks for the mechanism ONLY with attributable events. Engagement, if any, attributed the roll-3 way: a value the judge could only have gotten from the fact. |
| S4-c | Math-2 (overfit) | Catch retained AND zero voids — the stage-2 result must reproduce. A lost catch here is a HARD STOP (recall damage). |
| S4-d | Math-65 (correct) | Attribution rule unchanged: record, don't bank. |
| S4-e | all | Zero facts from discarded references (standing; 17-for-17 so far). |
| S4-f | — | Rule 7: two no-change iterations → stop and report. |

## Also record (pre-registered denominators, not gates)

* Admission rate: legs where the chain ran / admitted / emitted, with the
  discard step and reason for each non-admission.
* Cost per leg (tokens), so the stage-8 decision has real economics.
* BOTH SIGNS: catches gained/lost, accusations gained/lost vs the same
  legs' historical outcomes.
* Math-30 specifics: does the twin handle a `long`/`BigInteger`-flavored
  state? Does the reference use exact arithmetic where the buggy build
  overflows? Any new literal/parse shape is one read, per the ladder's
  defect discipline.
