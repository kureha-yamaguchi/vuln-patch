# Paired 30-leg measurement — the campaign's honest number

Two identical runs, same code (commit `0ebcf9e`), same width (`-n 5 -m 12`), queued
back-to-back on the VM so no edit could slip in between. 30 legs each: 14 patches that
are secretly wrong (we want them caught) and 16 patches that are genuinely correct
(we want them left alone).

Both runs archived and verified: `final30A_20260729_121819` (43 files),
`final30B_20260729_145001` (63 files). VM pruned.

## The scores, verbatim

```
ROLL A:  **TP=9 FN=5 FP=5 TN=11**   P=0.64 R=0.64 F1=0.64
ROLL B:  **TP=11 FN=3 FP=5 TN=11**  P=0.69 R=0.79 F1=0.73
```

Mean F1 = **0.685**, against the pre-campaign reference of **0.49**.

Tokens: 5,973,680 (A) + 6,038,623 (B).

Read the mean, not the better roll. Roll B alone would be an overclaim; roll A alone
would be an underclaim. The spread between two identical runs *is* the headline finding
as much as the level is.

## What actually moved: recall. Precision did not.

| | roll A | roll B |
|---|---|---|
| caught (of 14 bad patches) | 9 | 11 |
| false accusations (of 16 good patches) | 5 | 5 |

Precision is **flat at exactly 5 wrong accusations in both rolls**. Every point of
improvement came from catching more bad patches, none from accusing fewer good ones.
That is worth stating plainly because the whole of cycles 5 and 6 was aimed at
precision. Cycle 6 did not move the false-accusation count.

## Per-bug, across both rolls

**Bad patches we caught in both rolls — 8 of 14, the reliable core:**
Math-2, Lang-41, Lang-50, Chart-7, Closure-92, Math-68, Math-74, Math-82

Closure-92 is new to this list. It was a coin-flip before; it is now steady.

**Caught in one roll, missed in the other — 4:**
Lang-60 (A only), Closure-38, Lang-63, Math-73 (B only)

**Missed in both — 2:** Chart-19, Math-104

Chart-19 is the pre-recorded width-5 caveat, written down before the run: its win was
established at a wider setting, and at width 5 it is expected to miss. That is a config
note, not a retraction of its mechanism. Math-104 was pre-declared as missed by design.

**Good patches left alone in both rolls — 9 of 16.**

**Good patches accused in both rolls — 3:** Closure-62, Math-30, Math-65.
These are the chronic three. Math-30 was pre-declared a characterized judging residual.

**Accused in one roll only — 4:** Chart-26 and Math-39 (A only), Lang-60 and Math-73
(B only).

## Variance is the dominant remaining effect

**8 of 30 legs flipped outcome between two runs of identical code.** That is 27%. Any
claim resting on a single roll — mine or anyone's — is inside the noise unless the
mechanism is visible in the trace. The two-roll rule was the right discipline to have
adopted; this run vindicates it.

Note Lang-60 flipped in *both* directions: caught in A / missed in B on the bad-patch
side, and left alone in A / accused in B on the good-patch side. That is pure draw
noise, not a mechanism.

## The cycle-6 machinery is finally live

In the previous 20-leg run every cycle-6 mechanism logged **zero** firings — the code
existed but never reached a judge. That is fixed:

| event | roll A | roll B |
|---|---|---|
| gate entry | 98 | 103 |
| 6B indiscriminate decided | 83 | 78 |
| 6C fires-on-both considered | 83 | 78 |
| buggy-rate delivered | 63 | 57 |
| diversion decided | 130 | 134 |
| muted-replay pass | 65 | 76 |

6B actually dropped an accusation on 3 legs (A) and 5 legs (B); 6C actually rescued
one on 3 legs (A) and 2 legs (B).

**One attributed precision win:** Chart-26 was falsely accused in roll A; in roll B a
6B drop fired on it and it came out clean. **One attributed recall win:** Lang-63, which
was pre-declared as expected-missed, was rescued by a 6C keep in roll B and caught.

**Math-30 got a 6B drop in roll A and was still falsely accused** — the gate killed one
accusation and a second one survived. Consistent with its standing classification.

## The most useful thing this run taught us

Math-73 flipped, and the trace says exactly why — and it is not what I expected.

- Roll A: the rule the system invented for Math-73 was
  `negative-same-sign-must-throw`. It fired on the buggy build **and** on the correct
  build, 100% of the time. 6B correctly threw it out as evidence that proves nothing.
  With no other rule to convict on, the bug was missed.
- Roll B: the system happened to invent a *different* rule,
  `functionValue-matches-result`, which produced **different observed values** on the
  two builds. 6C kept it as genuine conviction evidence. The bug was caught.

So the two gates were not fighting each other, and neither was wrong. **The flip
happened one station upstream, in rule invention.** Whether a bug gets caught depends on
whether that roll's rule-writer happened to produce a rule that can tell the two builds
apart. The gates then handle it correctly either way.

This relocates the remaining recall problem. It is not a judging problem and not a gate
problem — it is a **rule-diversity** problem. The lever is more *varied* rules per bug,
not more harnesses per rule and not more judge persuasion. That is a general statement:
it names no bug and no dataset.

## Where this leaves the pipeline

Fixed and confirmed: the judge now receives mechanically computed facts and the cycle-6
gates act on them; the fail-open discipline held (no gate manufactured a verdict); the
false-fact bug that fabricated evidence against correct patches is gone from both rolls.

Still open, in priority order:

1. **Rule diversity** — the newly-located cause of ~4 legs of recall swing.
2. **The chronic three** (Closure-62, Math-30, Math-65) — 3 of the 5 standing false
   accusations, unmoved by cycle 6.
3. **Run-to-run variance at 27%** — bounds how finely anything can be measured from
   here. Future comparisons need paired runs, not single ones.

## Holdout status

The 12 never-seen bugs remain **unrun**, as instructed. This measurement was taken
entirely on the existing pool. The decision on whether to spend the holdout now returns
to you with real numbers in hand.
