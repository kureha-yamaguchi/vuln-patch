# Stage 2, roll 3 — the read against the five pre-registered gates

Run `stage2c_20260808_092623`, `git=06be7bb` (option A: the trigger requires a
DECLARATION, not a shown body), `PARALLEL=1`, both legs exit=0, 51 minutes.
Raw committed before this was written.

| bug | label | crashed | outcome |
|---|---|---|---|
| Math-65 | correct | False | TN |
| Math-2 | overfit | True | TP |

`TP=1 FN=0 FP=0 TN=1`  P=1.00 R=1.00 F1=1.00.  409,849 tokens.

The scoreboard is identical to roll 2. Everything that matters is different.

## The gates, against their pre-registered text

| gate | criterion | result |
|---|---|---|
| S2-a | zero `conviction VOIDED` on the fake-patch leg | **PASS, and no longer vacuous** — a reference was admitted on Math-2 and did not void the catch |
| S2-b | TP retained, both signs recorded | **PASS** |
| S2-c | reference admitted? disagreement fact emitted? zero facts from discards? | **PASS on all three** |
| S2-d | a TN counts only with a traceable VOIDED event | **NOT BANKED** — 0 voids, `crashed=False`; the lottery again |
| S2-e | rule 7, two no-change iterations | **NOT TRIGGERED** — this roll changed a great deal |

## S2-c: the mechanism did the thing it was built for

Math-2's chain ran end to end for the first time on a held-out bug — generation
for a distribution class, signature mapping, twin setup from `testMath1021`, the
screen — and emitted:

    [reference-implementation fact] an independent implementation of
    `getNumericalMean`, written from the DOCUMENTATION alone …
    It computes a DIFFERENT value at the disputed point:
        getNumericalMean: patched='-49.759350398538686'
                          independent reference='49.821236993679285'

Standing was earned the honest way: the reference reproduced the buggy build on
**6 off-defect sibling observables** at the failing test's own state — values it
was never shown and had to compute from documented formulas. The pin check
ABSTAINED (no overlap) rather than borrowing a neighbouring literal.

A hypergeometric mean is `n * m / N` and cannot be negative. The patched build
returns **−49.76**. The disagreement is a sign flip, not a rounding argument.

### The judge engaged it — attributed, not assumed

Judge call 138's WHY:

> the documented contract fixes `getNumericalMean()` to `n * m / N`, and the
> observed patched value `-49.759350398538686` versus expected
> `49.821236993679285` is a large sign-flipped disagreement far beyond the
> generous tolerance

**`49.821236993679285` occurs exactly twice in the whole trace**: once in the
reference fact, once in that sentence. The relation's own fired message carries
no such number (its note is a fire-rate line; the triggers say `sample=-50`).
The judge could not have got that value anywhere else. This is the first
engagement recorded — the same gate stood at **0 of 9** in roll 12.

### Zero facts from discards

Facts were emitted for exactly one method per leg — `getChiSquare` on Math-65,
`getNumericalMean` on Math-2 — and every discarded reference produced none. The
`sample` trigger option A switched on did fire, and its reference **failed to
compile and was discarded silently**, exactly the honest death predicted for a
stochastic observable. Bounded spend, no leakage.

## S2-d: Math-65 is the lottery for the third roll running

`crashed=False`, 0 VOIDED events. The patched build never crashed, so no
conviction stood for the gate to remove. The TN is recorded and **not banked**.
The gate reached its decision point and abstained with the unchanged 8.4 reason —
*the firing reports no observable the admitted reference computes*.

Three rolls, three different Math-65 outcomes upstream of the mechanism. The
correct-patch side still has **no attributable evidence of any kind**.

## Where this leaves the stage

The fake-patch side of stage 2 passed on every gate written for it, including the
one the whole mechanism exists to satisfy. The correct-patch side produced
nothing bankable, and S2-d is written as an attribution rule — a TN that does not
count — rather than a fail condition, so whether stage 4 fires is a judgment
call, not a reading.
