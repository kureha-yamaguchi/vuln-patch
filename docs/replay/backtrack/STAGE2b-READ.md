# Stage 2, roll 2 — the read against the five pre-registered gates

Run `stage2b_20260808_063833`, `git=29c8a44` (detector reads the check source),
`PARALLEL=1`, both legs exit=0. Raw committed at 502928c before this was written.

| bug | label | crashed | outcome |
|---|---|---|---|
| Math-65 | correct | False | TN |
| Math-2 | overfit | True | TP |

`TP=1 FN=0 FP=0 TN=1`  P=1.00 R=1.00 F1=1.00.  378,789 tokens.

**Neither correct verdict is attributable to the mechanism.** The scoreboard is
perfect and the gates still do not pass.

## The gates

| gate | criterion | result |
|---|---|---|
| S2-a | zero voids on the fake-patch leg | PASS, again **vacuously** — 0 voids because there was no admitted reference to void with |
| S2-b | Math-2 catch retained | PASS — TP |
| S2-c | disagreement side observed on a new bug | **NOT OBSERVED** — reference-chain reach on Math-2 is still exactly zero |
| S2-d | Math-65 counts only via a traceable conviction VOIDED event | **NOT MET** — 0 voids; the FP→TN flip is unattributable |

Stage 2 does not pass. Stage 4 does not fire.

## Why Math-2's reach is still zero

The check-source fix is correct and it is not what was blocking this leg. The
kept relation `hypergeom-mean-formula` disputes `getNumericalMean`, and the
detector declines with:

    the firing names no method whose body is shown; the mechanism has nothing
    to reimplement

That decline is right. Measured on this leg's own context (17,094 chars,
byte-identical between roll 1 and roll 2):

- `getNumericalMean` — 2 mentions, **body elided**. The Arja patch never touched
  it, so the context truncates it to `{ … }`. Body-shown means patch-touched,
  and this method is not.
- the patched method `inverseCumulativeProbability` **has its full body in the
  context** and `_method_body` still returns None, for two independent reasons.

## Two general detector gaps, both found by this leg

**(1) The `throws` clause.** `_method_body` matches `name\s*\([^)]*\)\s*\{` —
the brace must follow the paren. Any method declaring `throws` is invisible:

    public int inverseCumulativeProbability(final double p) throws OutOfRangeException {

Scope on the untruncated fixture: **976 throws-clause method definitions, 913
invisible to the strict matcher** (93.5%). Not bug-specific — Java methods
routinely declare `throws`.

**(2) The quoting cap.** Even with `throws` handled, this body is **1,518 chars
against `_MAX_QUOTED_BODY = 900`**, so it is dropped anyway. Two locks on the
same door; fixing either alone changes nothing here.

Both statements pass the statement test: neither names a bug.

## Math-65: the roll-13 trap, second instance

Roll 1 `crashed=True → FP`. Roll 2 `crashed=False → TN`. The patched build did
not crash at all this roll, so no alarm ever stood, so there was nothing for the
gate to void — and the trace confirms 0 VOIDED events. The flip is upstream of
the mechanism, in harness/fuzzing variation. It is the invention lottery, and it
must not be banked.

The gate did reach its two decision points on Math-65 and abstained both times
with the 8.4 reason — *the firing reports no observable the admitted reference
computes* — unchanged from roll 1.

## Rule 7

Two consecutive rolls with no change in gate outcome (S2-c not observed, S2-d not
met, both rolls). Rule 7 says stop iterating at this stage and change something
structural rather than re-rolling a third time.
