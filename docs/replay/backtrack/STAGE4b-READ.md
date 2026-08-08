# Stage 4, roll 2 — the read against the pre-registered gates

Legs 1–2: `stage4b_20260808_144352` (`git=402b844`). Legs 3–4:
`stage4legs34_20260808_151137` (`git=fd8a632`), split out and run concurrently.
`stage4b`'s driver was stopped after leg 2 so it could not re-run legs 3–4; leg 2
finished as an orphan and wrote its own artifacts, so there is no suite summary
for it and the table below is assembled from `result.jsonl`. Raw committed first.

| leg | label | crashed | outcome | tokens |
|---|---|---|---|---|
| Math-65 (CapGen) | correct | True | FP | 214,744 |
| Math-2 (Arja) | overfit | True | TP | 112,204 |
| Math-2 (SOFix) | correct | False | TN | 144,811 |
| Math-53 (Arja) | correct | False | TN | 134,214 |

`TP=1 FN=0 FP=1 TN=2`  605,973 tokens.

## The gates

| gate | criterion | result |
|---|---|---|
| S4-a | HARD STOP if the clean leg's accusation traces to the mechanism | **NO TRIGGER** — Math-53 came back clean, with zero mechanism events of any kind |
| S4-b | the agreement-side leg's evidence visibly present | **FAILED** — Math-2-SOFix produced no admission and no fact |
| S4-c | Math-2 catch retained, zero voids | **PASS** |
| S4-d | Math-65 record, don't bank | **HELD** — FP, 0 admissions, 0 voids |
| S4-e | zero facts from discarded references | **PASS** — 5 generations, 1 admission, 4 discards, no facts leaked |
| S4-f | rule 7 | **TRIGGERED** — two consecutive rolls where the agreement side went untested |

**Both-signs ledger:** no catches gained or lost; no accusations gained or lost
that trace to the mechanism. Math-53 flipped FP→TN and Math-65 held FP, both
upstream. **Admission rate 1 of 5 generations (20%)**, matching roll 1 exactly.

## Reproducibility, worth banking

The Math-2-Arja fact came back **digit-for-digit identical** to stage 2 roll 3,
in a different run on a different day:

    getNumericalMean: patched='-49.759350398538686'
                      independent reference='49.821236993679285'

Independent generations converging on the same value is now three for three.

## Why the agreement side went untested again

Math-2-SOFix is the same class that admitted twice, with a **7-sibling screening
surface** — the richest yet recorded. It still produced nothing, and not because
agreement is hard to establish. The chain never attempted the method that
matters.

**Defect: candidate selection is positional, and failure is memoized.**
`run.py:133` is `method = disputed[0]`. The trace records up to four candidates
and only ever tries the first; the memo then caches the failure so the leg never
revisits the observable. On this leg the candidate lists were:

    ['getSampleSize', 'sample', 'getNumericalMean', 'getNumberOfSuccesses']
    ['getNumberOfSuccesses', 'getPopulationSize', 'sample', 'getNumericalMean']

`getNumericalMean` — the one method with a documented closed-form contract, the
one that has admitted on this exact class twice — appears in both lists and was
attempted in neither. Both attempts went to **stored-field accessors**, the least
informative reimplementation targets available and the hardest to map, since
their parameters are constructor arguments rather than state.

This is general: nothing about it is specific to a bug. A leg fails whenever the
productive candidate is not in position 0, which is a coin flip the mechanism
currently loses silently.

### Two signature-path failures, downstream of that choice

1. `getSampleSize` — `declared_signature` returned the truncated
   `'(int numberOfSuccesses'`, then `declared: []`, so the reference was
   discarded as omitting its own observable.
2. `getNumberOfSuccesses` — the signature parsed as `'int, int, int'`, types with
   no names, so `match_parameters` treated the literal string `int` as a
   parameter name: *parameter `int` matches no canonical state field*.

Both are real, both fail closed, and neither was reached by a candidate worth
reimplementing. Whether they would matter for a well-chosen candidate is unknown
— fixing selection first would tell us.

## Where this leaves the mechanism

Two rolls have now been spent trying to test the agreement side, and it remains
untested for two unrelated reasons — a shape-blocked class (roll 1) and a
positional candidate choice (roll 2). The conviction side works, reproducibly.
Rule 7 says stop re-rolling and fix the selection.
