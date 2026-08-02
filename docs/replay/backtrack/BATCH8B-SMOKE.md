# Second batch smoke — the pre-registered number is MET, with one thing flagged.

`batch8b_20260802_135255`, git `26cb728`, same two legs. 370,764 tokens.

## The deciding number, pre-registered before the run

> of the alarms whose checks normalize, how many still carry `actualRaw=` by the
> time the comparison runs?

```
  01_patch1-Closure-62-Jaid_c    normalizing alarms: 2   carrying actualRaw=: 2
      len  414  escapes=yes  capped-form-would-have-lost-it=yes
      len  440  escapes=yes  capped-form-would-have-lost-it=yes
  02_patch1-Closure-38-SequenceR_o  normalizing alarms: 1  carrying actualRaw=: 1
      len  191  escapes=no   capped-form-would-have-lost-it=no

  TOTAL: 3/3 = 100%          (first smoke: 1/4 = 25%)
```

**Both halves of the fix are load-bearing, and the run shows each doing its own
job.** The two long alarms (414, 440 characters) carry escaped newlines — so the
escaping instruction took — *and* are flagged `capped-form-would-have-lost-it`,
so the consumer split is what let them arrive. Either fix alone would have
recovered neither.

n = 3. Three alarms is not a rate; it is an existence proof that the path is
open. The rate belongs to the pair.

## The rung is alive for the first time

The trigger-lift note on Closure-62 now reads:

> …the REAL test passes on this build, BUT the fired value **differs** from
> every value the test itself pins by more than the rounding floor — this is NOT
> a replay of the test's scenario; it is a candidate generalization catch…

and *"no numeric value could be compared"* now appears **0 times**, against
every prior run where that was the only branch this rung could reach.

## Other batch members, live

```
  gate 0c2 rejections : 0 on both legs   (lint agrees: nothing to reject)
  repair markers (8.7): 1 on EACH leg    (both legs now, vs 1 leg before)
```

## FLAGGED — not attributed

Closure-62 went **TN → FP**, and `crashed_on_patch` is **True** for the first
time on this leg.

**I am not attributing that to 8.4, and it is not scored.** Two reasons to
withhold, and one reason it still has to be written down:

* Closure-62 is the corpus's least stable leg: **4 FP / 3 TN over seven
  observations** (FP, FP, TN, TN, FP, TN, FP). A single flip sits well inside
  the measured run-to-run variance.
* `crashed_on_patch=True` is a *separate accusation channel* that appeared this
  run and did not exist in the last one. It is at least as plausible a driver.

But the thing worth stating plainly: **8.4 made a dead rung live, and a live
rung has two branches.** Design and guarding concentrated on the `matches`
branch, which pushes toward dismissal. The `differs` branch is keep-leaning — it
tells the judge test-passage does not exonerate the firing — and its **first
live firing landed on a correct patch**.

That is not evidence of harm. It is the precise thing the pair must read
per-event: how often `differs` fires on correct-patch legs, and whether those
legs accuse.

## Status

**The batch passes.** The build runs end to end; 8.7 fires on both legs; gate
0c2 is correctly silent with the lint agreeing; 8.4's raw values now reach the
comparison at 3/3 and the rung produces real verdicts instead of `unknown`.

Verdicts remain **recorded, not scored** — rule 16, no baseline differing by
exactly one change.

Carried into the pair as a named read: the `differs` branch on correct-patch
legs.
