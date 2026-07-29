# night20c (2026-07-29) — second 20-leg roll, all cycle-6 fixes

20 legs, width 7, 5.04M tokens. Raw: `night20c_20260729.md`.
Verbatim score line: **`TP=8 FN=6 FP=2 TN=4  P=0.80 R=0.57 F1=0.67`**

## Per-leg, against night20b

| | night20b | night20c |
|---|---|---|
| catches (of 14) | 11 | **8** |
| false accusations (of 6) | 5 | **2** |
| F1 | 0.73 | 0.67 |

**Precision improved sharply**: Closure-62, Chart-26 and Math-65 all cleared (FP → TN). Only
Math-30 and Math-73-c remain.
**Recall fell**: Closure-92, Lang-63 and Math-68 lost (TP → FN).

## Two things this run DOES establish

**1. Chart-19 passes the two-roll rule.** Caught in night20b AND night20c, after never being caught
in five prior rolls, with the mechanism attributed in the first roll (fuzz-derived install indices
→ sparse container → the state item-4 proved was unreachable from constants). This is the first
catch of the campaign to clear the standing two-roll bar with a named mechanism.

**2. The row-32 tripwire did NOT fire.** The pre-committed trigger was "if Math-73-ACS-family
catches die, 6B is first suspect and gets reverted". Math-73-ACS held TP in both rolls. 6B is not
implicated, and the logged open question closes without a revert.

## What it does NOT establish — and the reason is a gap we must fix first

**The mechanism checklist read 0 for every cycle-6 item** (`skipped=`, `was DIVERTED`,
`6B-INDISCRIMINATE-DROP`, `6C-partial-fix-keep`, `muted-replay pass`, `buggy-rate delivery`).
That is **not evidence they didn't fire**: those are `print` diagnostics, and `run_suite.sh`
deletes `run.log` on success, so print-only output cannot reach `trace.md` at all. Confirmed:
0 `run.log` files exist in the run (every leg succeeded).

What IS visible via the recorded-event path proves the older machinery ran: `fire-rate fact` and
`muted-replay fact` appear in **13 of 20 legs**. But `DIVERTED before this check` appears in 0 —
which is ambiguous between "no diversion occurred" and "diversion detection never ran".

**So we cannot say whether 6B, 6C, the diversion counter, the iterated passes, or the new rate
delivery acted in this run.** This is the same observability hole I fixed once already for the
one-door/universal-screen diagnostics by routing them through `record_event`; the cycle-6
mechanisms were built print-only and inherited it.

**Consequence for the plan:** running the 30-leg measurement twice (~10M tokens) *before* closing
this gap would buy a number without knowing which mechanisms produced it — the same mistake as
measuring with an inert Spec M or a blind escape. **Fix observability first** (route the cycle-6
decisions through `record_event`), then measure.

## On the score itself
Confounded and not directly comparable: night20b and night20c differ in BOTH code and draw, and a
±3-leg swing sits inside this pipeline's measured verdict variance (5 of 10 untouched rows flip
between identical draws). The precision gain is in the right direction and the direction the
cycle-6 work targeted, but attribution needs the observability fix before it can be claimed.
