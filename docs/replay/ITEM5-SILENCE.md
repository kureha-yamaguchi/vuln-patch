# Item 5 (silent-case retry) — NOT BUILT, and what the measurement revealed

Measured before building, per the standing rule. The measurement killed item 5
outright and produced the most useful single fact of the cycle — which also
re-prices repair-in-place.

## Silence is the most reliable signal in the pipeline

16 of 60 legs across the two paired runs are **silent**: zero alarms ever reached
the reviewer. The split is not close.

| | legs |
|---|---|
| silent, correct patch | **14** |
| silent, fake patch | 2 |

And silence predicts the verdict almost perfectly:

| correct patches | count | cleared | wrongly accused |
|---|---|---|---|
| stayed silent | 14 | **14** | **0** |
| raised any alarm | 18 | 8 | **10** |

**A correct patch that raises any alarm is wrongly accused 56% of the time.**
A correct patch that stays quiet was cleared 14 times out of 14.

## So item 5's premise is inverted

The item read: "when a run produces zero alarms for a suspect patch, that run
learned nothing — give those cases one extra round of alarm-writing."

A silent run has not learned nothing. On a correct patch it has learned the most
trustworthy thing the system produces. And the retry must be label-blind — the
pipeline cannot know which patches are fake — so it would fire on all 16 silent
legs: 14 correct, 2 fake. Every woken correct patch carries a 56% chance of a
wrong accusation.

There is a second reason, independent of the first: **the silent legs are not
width-starved.** They already carry a median of 5 accepted harnesses — the cap.
They are silent because their harnesses do not fire, not because there are too
few. A sixth harness does not address why they are quiet; it adds another lottery
ticket on a patch that was going to be cleared.

**Not built.** Recorded rather than parked: the premise is measurably false, not
merely unsupported.

## The same measurement re-prices repair-in-place

Repair-in-place recovers 84 harnesses across the corpus. Where they land:

| | legs | extra harnesses |
|---|---|---|
| correct patch, silent | 11 | 22 |
| correct patch, already loud | 15 | 28 |
| fake patch, silent | 1 | 2 |
| fake patch, already loud | 20 | 32 |

**50 of the 84 (60%) land on correct patches**, and 22 of those on silent correct
patches that were cleared 14 times out of 14.

That is the pre-registered width-increase concern, now with a number attached
instead of a caveat: waking 11 silent correct patches at a 56% accusation rate is
the mechanism by which this could cost precision.

### But the benefit side is real, on exactly the right legs

This is not a one-sided finding, and an earlier draft of this note overstated it.
Of the 8 fake-patch leg-instances that were MISSED in a roll, 6 gain at least one
repaired harness — including the motivating case in **both** rolls:

```
A Chart-19-Arja-plausible_o    9 rejections -> +1 repaired
B Chart-19-Arja-plausible_o    8 rejections -> +1 repaired
A Lang-63-Arja-plausible_o     2 rejections -> +1
A Math-104-Elixir-plausible_o  1 rejection  -> +1
B Math-104-Elixir-plausible_o  4 rejections -> +2
B Lang-60-Arja-plausible_o     2 rejections -> +2
A Closure-38-SequenceR_o       1 rejection  -> +0
A Math-73-ACS-plausible_o      6 rejections -> +0
```

So repair-in-place does put harnesses back on the legs we are actually missing,
including Chart-19, whose rules provably died at construction.

### The honest expectation

Both effects scale with one unmeasurable quantity: how often a repaired harness
actually fires. Call it *p*.

* recall: ~6 missed-fake leg-instances × *p* × P(the firing convicts)
* precision: 11 woken silent correct patches × *p* × 56%

The correct-patch exposure is roughly double the fake-patch opportunity, and the
56% is measured with the cycle-6 gates already live, so it is not obviously
improved by the precision work already shipped. *p* cannot be measured offline —
it needs a run.

**This is a decision, not a conclusion.** The options:

1. **Ship it into the pair as planned.** The two-tier bar already prices the risk:
   the PASS tier requires zero accusations on historically clean cases, which is
   precisely the clause that fails if the cost side dominates. We would learn *p*
   and the true trade in one measurement.
2. **Hold it out of the pair**, measure the batch's precision work cleanly, then
   add repair-in-place in a following pair so its effect is attributable.
3. **Gate it** — but no label-blind gate is available. "Only repair on legs with
   few accepted harnesses" does not help: the silent legs are already at the cap.

Option 2 is the cleanest attribution and costs one extra pair. Option 1 is one
measurement but confounds a recall lever with a precision batch — and this cycle
has already shown how hard that is to disentangle afterwards.
