# Rung-2 gate result (`rung2_20260801_164156`, git `87fcf5f`, 2 legs)

## Criteria as written, and how they landed

| # | criterion | result |
|---|---|---|
| 1 | deliberately mislabeled cases file refuses to launch | **not testable by this run** — narrowed before spending; evidence is 8.8's offline both-directions test |
| 2 | repair marker appears on a repaired-accepted harness | **PASS** |
| 3 | rate-state split observed live | **PASS** |
| 4 | zero verdict changes vs smoke7 | **ILL-POSED — see below** |

## Criterion 2 — PASS, and it exercises new code for the first time

```
accepted-from-repair markers : 7
from_repaired_attempt fields : 10
repairs applied              : swallowed-alarm 6 · missing-alarm-id 2 · boolean-swallow 1
```

Sample: `ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]`

The boolean-swallow firing is its **first live occurrence** — it was built and
compile-validated offline over 111 pairs, but had never run in production.

## Criterion 3 — PASS

Live states observed: `below-bar` (2), `no-measurement` (3),
`no-fires-on-both-confirmation` (3), `alarm-already-discarded` (6), with a
measured `rate=0.9990` present. Four distinct states in a two-leg run, against a
gate of "at least two". The rarer states (`buggy-side-unmeasured`,
`catch-profile-skipped`) are defensive branches that real evidence seldom
produces — their absence is expected, not a miss.

## Criterion 4 — the criterion was ill-posed. My error.

Closure-62 went **TN → FP**. Rung 2's items have no verdict surface (8.7 adds a
trace field, 8.8 refuses bad input, 8.12(a) is tests only), so this looked like a
regression.

It is not, and the criterion could never have shown one. **The baseline is not a
rung-2 delta.** smoke7 ran at `f307dd9`; this ran at `87fcf5f`. Between them sit
three changes that DO affect verdicts:

* boolean-swallow repair (`108772b`, `dcf681c`) — changes which harnesses are accepted
* the alarm-ID gate correction — same
* literal-concatenation folding (`47a5460`) — changes trusted-value extraction

The third was **built specifically to make Closure-62's trusted values extract**,
and it is confirmed active here:

```
TRUSTED ground-truth blocks in judge prompts:  5   (smoke7: 0)
trigger-lift notes delivered:                  2
...reaching the must-be-dismissed branch:      0
```

So the evidence the judge saw on this leg genuinely changed, by design, from a
cycle-7 commit — not from rung 2.

**Two things follow, and neither is a pass:**

1. The gate cannot certify "no verdict surface" from this comparison. To do that
   the baseline must be the **immediately preceding commit**, not a smoke from
   three verdict-affecting changes ago.
2. The flip cannot be attributed to the fold either, on one roll. Closure-62's
   record is now FP, FP (pair), TN (smoke7), TN (re-smoke), FP (here) — **3 FP / 2
   TN over five observations**, comfortably inside the measured 27% flip rate.

Also confirmed: the fold delivers trusted values but **still reaches no dismissal**
(`must be dismissed` = 0), exactly as 8.14c predicted — Closure-62's fired value is
whitespace-normalized and cannot match the test's raw literal. The fold made the
comparison possible; it did not make it succeed. That is 8.4's job.

## Gate verdict

**Rung 2 closes on criteria 2 and 3, with criterion 1 offline-verified and
criterion 4 withdrawn as ill-posed.**

Corrected rule for future gates: *a "zero verdict changes" criterion is only
meaningful against a baseline that differs by exactly the change under test.*
Otherwise it measures accumulated drift and reports it as regression — the
mirror of rule 15's family, where a check passes while testing nothing; here a
check fails while testing something else.
