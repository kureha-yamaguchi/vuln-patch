# Engagement experiment — RESULT: fails all three criteria

One shot, as pre-committed. No phrasing iteration. 91 cases, ~1M tokens, API only.

## Scores against the criteria fixed before running

| criterion | required | measured | |
|---|---|---|---|
| 1. finds the known line in contradiction-class checks | ≥7 of ~11 | **2** | **FAIL** |
| 2. guards answered NONE | ≥95% (≤3 wrong voids) | **77.8% — 10 wrong voids** | **FAIL** |
| 3. NONE on non-contradiction accusing checks | all | **voided 5** (Closure-62 ×3, Math-30 ×2) | **FAIL** |

Criterion 2 is the one that kills it outright, and the protocol said so in advance.

## What it actually did

```
verdicts:  LEGITIMATE 44 · OVER-TIGHT 22 · ERROR 25

false-accusation cases  24 total, 21 answered, 7 voided with a grounded quote
   of those 7, contradiction-class:  2
   by bug: Closure-62 x3, Math-30 x2, Math-65 x1, Math-39 x1

guard cases (genuine catches)  67 total, 45 answered, 10 WRONGLY voided
   by bug: Lang-50 x4, Math-68 x4, Closure-92 x1, Math-73 x1
```

**It voided genuine catches at 22% of the answered guard set.** That is the
failure mode the guard population exists to catch, and it caught it.

**And it voided the wrong false accusations.** Five of its seven hits are
Closure-62 and Math-30 — the two cases the backtrack classified as *not*
contradiction-class, where the deciding fact is the harness's own normalisation
and an arithmetic-overflow boundary respectively. Nothing in the shown source
contradicts those premises. So the mechanism is not finding real contradictions;
it is finding plausible-looking quotes, which is precisely why it also voids
catches.

Five further OVER-TIGHT claims were rejected by the mechanical grounding check —
the quote did not appear in the shown material. That guard worked, and without it
the wrong-void count would have been higher.

## The rate-limit caveat, and why it does not change the verdict

25 of 91 calls failed with HTTP 429 (throughput, not model behaviour), so only 66
were answered — 21 false-accusation and 45 guard.

This does not rescue the result. **The 10 wrong voids are a count, not a rate.**
Missing data can only shrink the denominator, never the numerator: on the answered
set the guard-clean rate is 77.8%, worse than the 85.1% computed over all 67. No
completion of the missing 25 can bring 10 wrong voids under the threshold of 3.

Re-running the 25 would complete the picture but cannot change the verdict, and
the one-shot rule forbids re-rolling the question regardless.

## Conclusion

**The engagement hypothesis is refuted.** Isolating the question and forcing a
verbatim quote did not make the reviewer reliably find the case-specific deciding
fact. It made it produce plausible quotes — including for premises that nothing
contradicts, and including against checks that were right.

This closes the last live alternative. The precision position is now:

* No recorded feature separates kept genuine catches from kept false accusations
  (separating-fact study).
* The deciding fact is present in 5 of 7 decisive cases but is case-specific, so
  no feature-based gate can act on it (backtrack Phase 1a/2).
* Delivering that fact adjacently and verbatim does not bind the accusation
  (Math-65 fact block, delivered 4× and ignored).
* Asking a narrow, structured, mechanically-graded question about it voids
  genuine catches at 22% (this experiment).

**Adopt: precision ceiling ~5 false accusations on this trap set under the
current architecture.** Not on the strength of an argument, but over the measured
refutation of every alternative proposed this cycle.

The remaining honest options are architectural, not incremental: change what
evidence is collected (the raw-value recording contract, bucket (b), ~17% reach),
or change who decides. Both are cycle-8 scope.
