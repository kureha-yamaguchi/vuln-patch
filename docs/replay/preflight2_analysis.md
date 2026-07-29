# Pre-flight smokes (2026-07-29) — gate 1 PASSES on mechanism; gate 2 inconclusive-but-not-blocking

Run `preflight2_20260729_104020`, 2 cases, width 5, ~500k. Verdict line:
`TP=0 FN=1 FP=1 TN=0` (both cases are known-hard; the scores are not the point).

## GATE 1 — the diverted-replay fix (Chart-26 correct patch): **PASS on the mechanism**

- **7 `cycle6_diversion_decided` events**, all `diverted=False`. The instrumentation ran on every
  replay and reached a determination each time — it is not inert.
- **The fabricated claim is GONE**: 0 occurrences of "buggy build handles this exact input cleanly
  WITHOUT firing", "existence proof", or "INTRODUCED the violation" anywhere in the leg. In
  night20b this same leg carried that false statement.
- "DIVERTED wording: 0" is the CORRECT result here, not a miss: no diversion occurred this run
  (`diverted=False` × 7), so the diverted note rightly never appeared. Absence of the note with
  presence of the decisions is exactly the intended behaviour.

**But Chart-26 is still an FP** — on other firings (`axis-entity-side-effect`, `bar-null-info`,
`chart-info-axis-entity`, `fresh-peer-range`). So, like Math-30, Chart-26 converts from a
**fabricated-evidence** problem into a **judging residual**. The fabrication is fixed; the
accusation persists on independent grounds.

## GATE 2 — 6C's different-values protection (Lang-63 overfit): **inconclusive, not failed**

Lang-63 was MISSED with **0 firings on the patched build** (`overfit MISSED — all harnesses quiet`),
hence 0 judged firings, 0 muted replays, 0 6C considerations. With nothing fired there is nothing
to protect, so 0 is correct behaviour, not a defect. 6C remains never-observed-live.

**Not a launch blocker:** 6C is fail-safe by construction — it can only *prevent* a drop, never
cause one. If it never fires during the measurement, the measurement is unaffected.

## Correction to a pre-declared residual

The pre-commitment (`aa687b5`) lists Lang-63 as a "known coin-flip". After this run it is
**1-for-3** (caught night20b; missed night20c; missed here), and its misses are **fuzz-reach**
(harnesses quiet), not wrong dismissals. Reclassifying it in advance of the measurement:
**reach-limited, one lucky catch** — not 50/50. Correcting a pre-declared expectation *before* the
measurement is the point of having declared it.

## Launch recommendation

Both pre-flight questions are answered:
- the fabricated-evidence mechanism is live, decides every time, and its false claim is gone;
- 6C is unobserved but structurally incapable of corrupting a measurement.

Every cycle-6 mechanism is now either confirmed live (6B, diversion, rate delivery, iterated
replay, observability events) or fail-safe-if-silent (6C). The measurement's results will be
attributable. **Recommend proceeding** to the paired 30-leg run at width 5 under the committed
protocol — on the user's word.
