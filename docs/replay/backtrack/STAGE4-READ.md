# Stage 4 — the read against the pre-registered gates

Run `stage4_20260808_130106`, `git=15852a9`, `PARALLEL=1`, four legs serial,
all exit=0, 83 minutes, 675,076 tokens. Raw committed before this was written.

| bug | label | crashed | outcome | tokens |
|---|---|---|---|---|
| Math-65 | correct | True | FP | 250,039 |
| Math-2 | overfit | True | TP | 153,800 |
| Math-30 | correct | True | FP | 140,170 |
| Math-53 | correct | True | FP | 131,067 |

`TP=1 FN=0 FP=3 TN=0`  P=0.25 R=1.00 F1=0.40.

## The gates

| gate | criterion | result |
|---|---|---|
| S4-a | HARD STOP if the clean leg's accusation traces to the mechanism | **DOES NOT TRIGGER** — Math-53 shows 0 facts, 0 admissions, 0 voids; its one reference was discarded before it could speak. The accusation is ordinary, not ours. |
| S4-b | Math-30: mechanism evidence visibly present | **PARTIAL** — the chain ran twice and left a full event trail, but both attempts discarded and no evidence reached the judge. Nothing banks. |
| S4-c | Math-2: catch retained AND zero voids | **PASS** — TP, 0 voids, the `getNumericalMean` fact emitted again |
| S4-d | Math-65: record, don't bank | **HELD** — FP this roll; the gate abstained, 0 voids |
| S4-e | zero facts from discarded references | **PASS** — facts appear for exactly one method per admitting leg (`getChiSquare`, `getNumericalMean`); all 8 discards emitted none |
| S4-f | rule 7 | **NOT TRIGGERED** — new material, new failures |

**Both-signs ledger:** catches gained 0, catches lost 0 (Math-2 held). Accusations
gained 0 and lost 0 that trace to the mechanism — every FP here is upstream.

**Admission rate: 2 of 10 triggered references (20%).** Cost per leg 131k–250k.

## The two Math-30 discards, both honest

**(1) `mannWhitneyUTest` — too thin to screen.** Screening surface resolved to
**0 computed sibling observables**, against the standing minimum of 3, and it was
discarded *before* the twin build and two JVM runs. The cheap death, working.

**(2) `cumulativeProbability` — reference omits the disputed observable.**
Generation produced `MannWhitneyUTest`'s own methods; none normalizes to the
requested name, so it was discarded.

Discard (2) should never have been triggered, and that is the finding.

## Defect: a call statement satisfies the declaration test

`_method_declared` matches `name(...)` followed by `{` **or `;`**. The `;` branch
exists for abstract and interface declarations. But a call statement also ends in
`);`, so this line in `MannWhitneyUTest`'s body:

    return 2 * standardNormal.cumulativeProbability(z);

registers `cumulativeProbability` as declared by the patched class. It is not
declared anywhere in the context — `NormalDistribution` is not in it at all. The
message path declines correctly (`disputed_observables(msg, ctx)` → `[]`); the
check-source path accepts it.

Measured on the untruncated fixture: **18,496 declaration matches across 228
contexts, of which 3,858 (20.9%) are calls rather than declarations**, spanning
130 distinct names. Not bug-specific.

Cost of this instance: one full generation, discarded downstream. **Fail-closed
held** — no fact was emitted — so this is wasted spend and a false trigger, not a
soundness breach.

## The structural finding: stateless classes have no screening surface

`MannWhitneyUTest` is a utility class that takes its data as arguments and keeps
no state worth observing. Its screening surface is **0 computed siblings on the
receiver's own type**, so the screen can never reach its minimum of 3, and no
reference for such a class can ever be admitted — however good it is.

Every admission the ladder has recorded is on a **stateful receiver**:
`CurveFitter`/optimizer (Math-65), `HypergeometricDistribution` (Math-2). The
mechanism's reach is not a property of the bug's difficulty but of whether the
patched class carries computable sibling state. That is a real and general
ceiling, and it was invisible until a fresh correct bug of a different shape ran.

## What stage 4 says about the correct-patch side

The question it was launched to answer — can the mechanism produce attributable
evidence on a fresh correct patch — is answered **no, not on this one**, and for
a reason that has nothing to do with the agreement direction being weak: the
class shape made admission impossible before agreement could be tested. The
agreement side remains **untested**, not refuted.
