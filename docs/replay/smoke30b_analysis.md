# Math-30 re-smoke (2026-07-29) — plumbing is COMPLETE; the residual is a JUDGING problem

Run `smoke30b_*`, one case, ~250k. Verdict: **still FP**.

## The delivery fix works — and that is the finding

- The 92% buggy-side rate now **reaches the judge**: `[fact:rate-ambiguous]` appears 3× (it was
  discarded entirely before).
- **Zero** `cycle6_rate_absent` events — no firing now arrives at judging with an unexplained
  missing rate. Every "no rate" path either has a rate or says why not.
- 6B: 6× `not-applicable`, exactly as predicted — 92% sits below its 0.95 bar, and the fix was
  decision-neutral by construction.

So the pre-stated disjunction resolves cleanly: **the remaining Math-30 conviction is not a
plumbing problem.** The judge now sees a measurement saying the check misbehaves on 92% of
ordinary valid inputs on the KNOWN-BROKEN build, and convicts the correct patch anyway, with
`CITATION: NONE` — an uncited hypothetical, the same shape the whole cycle has been fighting.

## What NOT to do

Lower 6B's bar from 0.95 to catch 92%. That is threshold tuning to fit one case, and the campaign
has already measured what it costs: the reverted 5D path used exactly such a widened two-sided bar
and was net-negative (it killed 4 confirmed catches to buy ~0 leaks). 6B is safe *because* it only
fires where the signal is unambiguous. Math-30 stays a known residual with a named cause rather
than a tuned-away one.

## What this unblocks

The 30-leg measurement was withheld for one reason: **we could not tell which mechanisms were live**.
That question is now fully answered:
- all six cycle-6 mechanisms emit permanent considered/decided events (confirmed live, smoke 1);
- 6B runs and drops at its bar (2 firings at 0.999/1.000, smoke 1);
- delivery is complete and self-reporting (this run: rate present, zero unexplained absences).

The blocking condition is satisfied, and the remaining Math-30/Math-73-c convictions are a
characterised judging residual, not an unknown. **The measurement is now worth spending** — its
number will be attributable.

Cost of establishing all this: ~500k across two smoke runs, versus 10M for the measurement.
