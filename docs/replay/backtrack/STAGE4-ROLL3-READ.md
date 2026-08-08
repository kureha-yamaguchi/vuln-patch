# Stage 4, roll 3 — the read

`stage4r3_20260808_163007`, `git=2998de4`, ten legs, 107 minutes,
**2,209,669 tokens**. Run at `PARALLEL=4`, not the handoff's mandated
`PARALLEL=1` — see the deviation note at the end. Raw committed before reading.

`TP=4 FN=0 FP=3 TN=3`  P=0.57 R=1.00 F1=0.73.

## Hard stops — checked first, all clear

**Blanket stop (zero gate voids on any fake patch): CLEAR.** Zero
`conviction VOIDED` events on all ten legs, fake and correct alike.

**S4-a (clean-guard accusation traceable to the mechanism): NO TRIGGER.** Both
guards came back clean — Math-53 TN with no mechanism events at all, Time-4 TN
with three references generated and all three discarded as too thin.

**S4-c (Math-2 catch retained, zero voids): PASS.** TP, 0 voids, the
`getNumericalMean` disagreement fact emitted again.

## S4-b — the agreement side finally ran, and it works

The candidate-ordering fix did exactly what it was built to do. Math-2-SOFix
attempted **`getNumericalMean` first and only** — one generation, one admission,
zero discards, against roll 2's two accessor attempts and nothing to show. The
fact:

    It computes the SAME value at the disputed point:
        getNumericalMean:     both compute '49.821236993679285'
        getNumberOfSuccesses: both compute '42976365'
        getNumericalVariance: both compute '0.17812367970822698'
        getPopulationSize:    both compute '43130568'

earned on **7 off-defect siblings**, the richest standing yet recorded.

### The A/B, in one line

Same class, same documentation, same doc-derived reference value, in the same
run:

| leg | patched build | reference | comparison |
|---|---|---|---|
| Math-2 Arja (**fake**) | −49.759350398538686 | 49.821236993679285 | **disagrees** |
| Math-2 SOFix (**correct**) | 49.821236993679285 | 49.821236993679285 | **agrees** |

The reference computed the identical value on both legs; only the patch's
correctness moved the comparison. That is the mechanism's core claim, now
demonstrated on both signs from a single run — and it is the fourth
digit-for-digit reproduction of that value across independent generations.

## The asymmetry held, and hardened

The agreement fact reached the judge and **the judge did not engage it**. Both
judge calls on that leg dismissed for an unrelated reason — a 1-ulp
exact-equality check — citing the check's own line:

> WHY: A correct implementation … would … produce the observed 1-ulp mismatch
> `750.9141286917456` vs `750.9141286917455`, so this exact-equality oracle can
> falsely fire.
> CITATION: `"if (Double.compare(actual, expected) != 0) {"`

By the occurrence-count standard, `49.821236993679285` appears **twice in the
trace, both inside the fact block, neither in a judge sentence**. Correct
dismissals, sound reasoning — but nothing the reference contributed.

Cumulative engagement: **conviction-side facts 1 of 1; agreement-side facts 0 of
11** (roll 12's nine, plus these two). The keep-bias is now measured on two
different bugs rather than inferred from one, which is what the deterministic
verdict gate exists to answer.

**S4-b verdict: the agreement side is demonstrated; the TN does not bank.** The
leg was already clean before the fact arrived, so no gate decision point existed
and the roll-13 attribution rule applies unchanged.

## Scout denominators (for the stage-8 decision, not gating stage 4)

| leg | outcome | gen | adm | discards, by reason |
|---|---|---|---|---|
| Math-65-c | FP | 7 | 1 | screen 2, signature unmappable 3, omits observable 1 |
| Math-2-Arja-o | TP | 5 | 1 | signature unmappable 2, screen 1, buggy twin failed 1 |
| Math-2-SOFix-c | TN | 1 | 1 | — |
| Math-53-c | TN | 0 | 0 | — |
| Closure-62-c | FP | 4 | 0 | too thin 2, omits observable 2 |
| Math-39-c | FP | 3 | 0 | omits observable 1, signature unmappable 1, twin failed 1 |
| Lang-41-o | TP | 0 | 0 | — |
| Time-4-c | TN | 3 | 0 | too thin 3 |
| Closure-92-o | TP | 3 | 0 | too thin 2, omits observable 1 |
| Closure-38-o | TP | 4 | 0 | too thin 4 |

**Admission rate 3 of 30 generations (10%)** — half the 20% seen on the
Math-only rolls, and the composition says why: **11 of 27 discards are "too thin
to screen"**, and every one of them is on Closure or Time. Those are the
stateless-receiver ceiling from roll 1, now measured across a wider population
instead of inferred from one leg. The second-largest class is signature
mapping (6), the roll-2 defect, still live on richer classes.

**Both signs:** no catches gained or lost; no accusations gained or lost that
trace to the mechanism. All three FPs (Math-65, Closure-62, Math-39) have zero
gate events behind them. Every recall leg was already a catch without the
mechanism's help.

## Deviation from the handoff

§2 mandates `PARALLEL=1` for "the tiny-suite pooling rule". That rule protected
relation pooling, which was **deleted on 2026-07-19** under the
benchmark-farming rule; `run_suite.sh:58` records the removal and every leg is
self-contained (own rundir, log, `D4J_CHECKOUT_ROOT`). Run at `PARALLEL=4`: 107
minutes against an estimated 3–5 hours, with no shared state to contaminate.
§1's `which defects4j` check also fails as written in a non-login shell — the
suite sources `/home/code/vpenv.sh` at line 51 and resolves it correctly.
Both should be corrected in the doc.
