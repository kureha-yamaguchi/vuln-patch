# Stage 4 — how to read it (written BEFORE launch, deliberately)

Suite: `suites/cases/ladder_stage4.cases`, TEN legs (four decisive + six
scout), PARALLEL=6 (user
rule: never serial). Protocol unchanged: rsync → copy raw into
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
        '03_patch1-Math-2-SOFix_c', '04_patch1-Math-53-Arja_c',
        '05_patch1-Closure-62-Jaid_c', '06_patch1-Math-39-Arja_c',
        '07_patch1-Lang-41-Arja-plausible_o', '08_patch1-Time-4-Elixir_c',
        '09_patch1-Closure-92-SequenceR_o', '10_patch1-Closure-38-SequenceR_o']
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
| S4-b | Math-2-c (correct, roll-2 swap) | THE AGREEMENT-SIDE TEST, shape-selected onto the one class where admission is proven. Expect: chain admits, the fact's AGREEMENT side emits (reference computes what the patch computes), and — if the leg convicts — the verdict gate gets its first live decision points with an admitted reference on a correct patch. A TN banks for the mechanism ONLY with attributable events. Engagement attributed the roll-3 way: a value the judge could only have gotten from the fact. |
| S4-c | Math-2 (overfit) | Catch retained AND zero voids — the stage-2 result must reproduce. A lost catch here is a HARD STOP (recall damage). |
| S4-d | Math-65 (correct) | Attribution rule unchanged: record, don't bank. |
| S4-e | all | Zero facts from discarded references (standing; 17-for-17 so far). |
| S4-f | — | Rule 7: two no-change iterations → stop and report. |
| SCOUT-a | Lang-41 (overfit) | HARD STOP on any void — same class of rule as S4-c: the gate must not destroy a stable catch on non-Math material. |
| SCOUT-b | Time-4 (clean) | HARD STOP on any mechanism-traceable accusation — S4-a's rule, second guard leg. |
| SCOUT-c | Closure-62 (correct, STABLE FP both pair runs) | Informational, weighted: the second live precision target. Read: does the chain trigger/admit on a Closure receiver, and if the leg convicts (expected — stable FP), do fact/gate events appear? Attribution rules as everywhere. |
| SCOUT-d | Math-39 (correct) | Informational: reach datum on the plan-named stage-8 leg. |
| SCOUT-e | Closure-92, Closure-38 (overfit, STABLE FN both pairs, dismissal-type) | THE RECALL READ: firings reach the judge on these legs and get dismissed. Does the chain trigger at the dismissal site? Does a disagreement fact emit and engage (roll-3 attribution)? An FN→TP with an attributed engagement = the first recall win. An FN staying FN = status quo, recorded. |
| SCOUT-f | ALL overfit legs | Blanket hard stop: zero gate voids on any fake patch. |

## Also record (pre-registered denominators, not gates)

* Admission rate: legs where the chain ran / admitted / emitted, with the
  discard step and reason for each non-admission.
* Cost per leg (tokens), so the stage-8 decision has real economics.
* BOTH SIGNS: catches gained/lost, accusations gained/lost vs the same
  legs' historical outcomes.
* Math-2-c specifics: the reference should compute the DOCUMENTED mean —
  which the correct patch also computes — so the disputed-point comparison
  should read AGREEMENT. Any conviction on this leg is the Math-65 shape
  on a second bug: the exoneration evidence exists and the question is
  whether fact or gate moves the outcome. Both channels' events are the
  read.
