# Semantic (non-crashing) bug runs — full analysis

Detailed readthrough of every semantic-bug patch we have run through the
pipeline, what harness/oracle was generated, and whether the verdict was
correct. Companion to [critique.md](critique.md).

**Scope & caveats.** All runs below are **pre-fix** (before the reachable-set
repair, commit 58ccfc1), so variant-steering was OFF. The post-fix re-run
(`sem2`) is a separate comparison, pending. Sample sizes are small — read this
as *mechanism analysis*, not statistics. Sources: full run logs in
`/home/code/scratch/sem_logs/` on the dev VM; patches from the drr dataset;
trigger tests from Defects4J.

## How to read a verdict

The classifier flags a patch **overfitting** if ≥1 generated harness fires
(throws) on the *patched* code. Ground truth comes from the drr label.

| | patch really overfit | patch really correct |
|---|---|---|
| **harness fired** | TP ✓ (caught) | **FP ✗ (false accusation)** |
| **harness clean** | **FN ✗ (missed)** | TN ✓ (cleared) |

For a semantic bug the harness *is* the oracle: it reconstructs the trigger
test's call, compares against the expected value (the "lifted" oracle), and/or
asserts a metamorphic relation, throwing on mismatch.

## Summary of all semantic runs

| Bug | Tool | Truth | Verdict | Result | Batch |
|---|---|---|---|---|---|
| Math-104 | Elixir | overfit | flagged | **TP** | 2 |
| Time-11 | Arja | overfit | flagged | **TP** | 2 |
| Time-11 | Nopol2015 | overfit | flagged | **TP** | 2 |
| Time-11 | Arja | overfit | flagged | **TP** | 2 |
| Math-80 | SimFix | overfit | flagged | **TP** | 1 |
| Time-19 | SOFix | correct | clean | **TN** | 1, 2 (×3) |
| Math-33 | ssFix | correct | clean | **TN** | 2 |
| Time-15 | ACS | correct | clean | **TN** | 2 |
| Time-15 | Arja | correct | clean | **TN** | 2 |
| Math-59 | SimFix | correct | clean | **TN** | 2 |
| Math-30 | SequenceR | correct | clean | **TN** | 2 |
| **Time-19** | **HDRepair** | **correct** | **flagged** | **FP** | 2 |
| **Math-2** | **SOFix** | **overfit** | **clean** | **FN** | 2 |
| **Math-2** | **SOFix** | **overfit** | **clean** | **FN** | 2 |
| Time-15 (run15) | — | overfit | (no record) | — | 2 |

Batch-2 semantic totals: **TP=4 FN=2 FP=1 TN=8** → precision 0.80, recall
0.67, F1 ≈ 0.73.

---

# Correctly caught overfits (TP)

## Math-104 (Elixir) — series-convergence bug

**Patch** (`Gamma.regularizedGammaP`): flips the convergence test
`while (Math.abs(an) > epsilon …)` → `while (Math.sqrt(an) > epsilon …)`.
Wrong stop condition → wrong result, no throw.

**Trigger test:** `regularizedGammaP(1.0,1.0)` must equal `0.632120558828558`.

**Harness oracle(s):**
1. Lifted seed assertion (sound): `regularizedGammaP(1.0,1.0) == 0.632…`.
2. Metamorphic: `regularizedGammaP(a,x)` non-decreasing in `x` (a CDF — true
   for any correct impl).

**Why caught:** the `sqrt` patch breaks the value at the seed, so oracle (1)
fires. ✓

**Latent FP risk to remember:** the monotonicity relation is true for the
*ideal* function, but the impl is an iterative approximation controlled by
`epsilon`/`maxIterations`. The harness lets `maxIter` be as low as 1 and
`epsilon` as large as 0.1 — the *non-converged* regime, where even CORRECT code
can violate monotonicity numerically. On a correct patch that relation could
false-positive. (See FP recommendations.)

## Time-11 (Arja ×2, Nopol2015) — tail-zone construction bug

**Patch(es)** (`DateTimeZoneBuilder`): three different overfits to tail-zone
handling — one deletes the duplicate-name-key rename, one adds a bogus
`if(!((ruleSetCount<=1)&&…))` guard, one replaces `rs.buildTailZone(id)` with a
`System.out.println`.

**Trigger test:** `testDateTimeZoneBuilder` — `assertNotNull(zone[0])` after
building a zone in another thread.

**Harness oracle(s):**
1. Lifted: `assertNotNull(zone[0])` reconstructed → throw if null.
2. Metamorphics: `toDateTimeZone` determinism (same builder/id must yield same
   offset/standardOffset/nameKey), cached-name-key/offset stability.

**Why caught:** all three overfits break tail-zone construction so the built
zone / its determinism fails. ✓ across all three patch variants — good sign the
harness set locks onto the root cause, not one patch's surface.

## Math-80 (SimFix) — batch 1, no saved log

Recorded as semantic overfit, flagged (TP). Full log not captured (batch 1);
re-run needed for harness detail.

---

# Correctly cleared correct patches (TN)

## Time-19 (SOFix ×3) — DST cutover fix

**Patch** (`DateTimeZone.getOffsetFromLocal`): `else if (offsetLocal > 0)` →
`>= 0`. A correct fix for the London fall-back overlap.

**Trigger test:** `testDateTimeCreation_london` — `base.toString()` ==
`"2011-10-30T01:15:00.000+01:00"` and `plusHours(1)` == `"…Z"`.

**Harness oracle(s):** lifted the two `toString` assertions (sound), PLUS —
notably — a round-trip metamorphic (`DateTime.parse(dt.toString())` millis must
match) and a later/earlier composition relation.

**Why cleared:** the patch is correct, so the seed assertions hold. ✓
**⚠ Important:** run 4 added the *same* round-trip relation that causes the FP
in run 16 (below) yet stayed clean here — it just didn't fuzz into the
DST-ambiguous window. So the FP is latent in the TN runs too; it fired only
when the fuzzer hit the ambiguous domain. The relation is unsound; clearing was
partly luck.

## Math-33 (ssFix) — simplex tolerance form

**Patch** (`SimplexTableau`): `Precision.compareTo(entry,0d,maxUlps)` →
`…,epsilon)` — an equivalent tolerance expression.

**Harness oracle(s):** lifted the solution-point/value assertions from
`testMath781`; metamorphic: scaling the objective by `k` scales the optimum by
`k`; constraint-order independence.

**Why cleared:** behaviour unchanged → seed + metamorphics hold. ✓

## Time-15 (ACS, Arja) — safeMultiply overflow guards

**Patch(es)** (`FieldUtils.safeMultiply`): valid `Long.MIN_VALUE` overflow
checks.

**Harness oracle(s):** lifted the full `testSafeMultiplyLongInt` assertion
table (many exact values + expected `ArithmeticException`s); metamorphic:
`safeMultiply(v,k) == safeAdd(safeMultiply(v,k-1), v)`.

**Why cleared:** correct fixes satisfy the exact table and the identity. ✓
This is the *strongest* oracle shape we saw — a rich exact-value table plus a
sound algebraic identity. Note how much better-conditioned this is than a loose
inequality (contrast Math-2 FN below).

## Math-59 (SimFix) — FastMath.max NaN branch

**Patch** (`FastMath.max(float,float)`): NaN-branch `: b` → `: a` (correct).

**Harness oracle(s):** lifted `min`/`max` vs `Math.min`/`Math.max` within
`MathUtils.EPSILON`; metamorphic: `max(a,b)+min(a,b) == a+b`.

**Why cleared:** correct → all hold. ✓ (Good sound identity.)

## Math-30 (SequenceR) — Mann-Whitney overflow fix

**Patch** (`MannWhitneyUTest`): `int n1n2prod = n1*n2` → `double` (avoids int
overflow on large n).

**Harness oracle(s):** lifted `result > 0.1`; metamorphics: swap symmetry
`p(a,b)==p(b,a)`, affine invariance.

**Why cleared:** correct → hold. ✓

---

# The false positive — Time-19 (HDRepair) 🔴

**The patch is CORRECT** — identical `offsetLocal > 0` → `>= 0` as the three
SOFix TN runs. So the patch is not the problem; the harness is.

**What went wrong:** the generated harness lifted the sound seed assertions,
then added its own oracle:
```java
// Metamorphic relation (round-trip): formatting then parsing must preserve the formatted form.
String s = dt.toString();   // … parse(s) and compare millis
```
and biased the fuzzer toward the cutover ("*stay near the known problematic
cutover date*"). But **Oct 30 2011, 01:00–02:00 London occurs twice**
(fall-back), so local↔instant round-trip is *legitimately* ambiguous there.
The relation is **not universally true** → it fires on CORRECT code → FP.

**Key contrast:** the *same* patch cleared in runs 4/12/18. The FP is a product
of (a) an unsound invented relation and (b) the fuzzer happening to hit its
invalid domain. Root mechanism = the model inventing an oracle it can't
justify. This is the dominant semantic-FP pattern (also latent in Math-104).

---

# The false negatives — Math-2 (SOFix) ×2 🔴 REAL misses (masked-symptom overfit)

**Verified 2026-07-02 by differential testing** (see methodology note). My first
read that these were "output-equivalent / undetectable" was WRONG — they are
genuine false negatives. Here is the confirmed anatomy.

**The real root cause** is NOT where the overfit patches. It is
`HypergeometricDistribution.getNumericalMean()`:
`(double)(getSampleSize() * getNumberOfSuccesses()) / getPopulationSize()` — the
int product `sampleSize * numberOfSuccesses` **overflows** for large values →
garbage (even negative) mean. The developer fix rewrites this to divide first
(`sampleSize * (successes / (double)N)`).

**The overfit patch** (`AbstractIntegerDistribution.inverseCumulativeProbability`):
`if (tmp < upper)` → `tmp > upper` (run 17) / `tmp >= upper` (run 19), a
*downstream* line. It does NOT touch the mean. With the mean still broken, the
Chebyshev bound `tmp` is garbage, but the flipped condition makes the code SKIP
the garbage-based bracket-narrowing → a valid full-support bracket → the
**bisection downstream self-corrects** to the right quantile. So it **masks the
symptom exactly where the trigger test looks** while leaving the root cause
broken.

**Empirical proof it's detectable (just not where the harness looked):**

| method | overfit | fixed |
|---|---|---|
| `inverseCumulativeProbability(0.5)` (seed dist) | 50 | 50 (identical — masked) |
| `getNumericalMean()` (seed dist) | **−49.76** | 49.82 |
| `getNumericalMean()` (N=1e9,m=5e8,n=10) | **0.705** | 5.0 |

`inverseCumulativeProbability` is identical across 7 distributions (incl. int
overflow) — the bisection washes out the bug. But `getNumericalMean()` is
observably wrong. So the patch is a **genuine overfit** (dataset label CORRECT):
it fixes the symptom, not the root cause.

**Why the harness missed it:** it anchored on the trigger test's method
(`sample()` → `inverseCumulativeProbability`) — precisely the self-correcting
symptom path where the bug is hidden. It **never called `getNumericalMean()`
directly**, so it saw correct output and cleared the patch. This is a
**detection-scope gap: it tested where the symptom appeared, not where the root
cause lives.**

**The steering connection (testable prediction):** `getNumericalMean` IS in the
root-cause reachable neighbourhood of the touched function (which calls it).
These runs were pre-fix (steering OFF, reachable set empty), so the harness was
never told to also probe `getNumericalMean`. With the reachable-set fix
(commit 58ccfc1), steering lists it → a steered harness that calls it with a
sound oracle (a hypergeometric mean must satisfy `0 ≤ mean ≤ sampleSize`;
−49.76 violates it) would flag the patch. **Prediction: Math-2 flips FN→TP with
steering on** — being tested in the post-fix batch.

## Methodology notes (two ways differential testing lied to me)

1. **Force a clean recompile.** My first manual overfit build returned buggy
   values (`-50`) because I copied a checkout that already had compiled
   `.class` files and ant skipped rebuilding the edited source. Always
   `rm -rf` the classes dir before comparing. (Relevant to the pipeline too:
   ensure the patched build actually recompiles.)
2. **Probe the root-cause method, not just the symptom method.** Diffing only
   `inverseCumulativeProbability` showed "equivalent"; the divergence only
   appeared when I diffed `getNumericalMean`. This is the project's own thesis
   in miniature — exercise the root cause, not the symptom.

## Related crashing FN (context)

**Math-49 (JGenProg)** — `entries.remove(index)` → `entries.put(index,value)`
for a ConcurrentModification bug. Harness didn't reproduce the specific
iteration-during-modification path → missed. (Coverage/path problem, not oracle
problem.)

## No-record

**run 15 (Time, overfit)** — `DateTimeZoneBuilder` `if(tailZone!=null) break`.
Produced no scoreable record (build/verify issue). Not counted.

---

# Cross-cutting patterns

1. **The lifted seed assertion is reliable but pins only one input.** All the
   *discrimination* comes from generalization (metamorphic / extra inputs) —
   which is exactly where BOTH failure modes originate:
   - FP ← an **unsound** invented relation (round-trip on ambiguous times;
     monotonicity outside convergence).
   - FN ← a **too-loose** oracle (inequality both impls satisfy) or a
     **masked-symptom** overfit whose bug only shows in a sibling method the
     harness never probes (Math-2's `getNumericalMean`).
2. **Sound oracle shapes win.** Exact-value tables (Time-15) and true algebraic
   identities (`max+min=a+b`, swap symmetry) never caused FPs. Free-form
   round-trip/monotonicity guesses did.
3. **FP was a single-harness fluke.** The same correct patch cleared in 3 runs
   and FP'd in 1 — the bad relation appeared in only that harness.
4. **Masked-symptom overfits need root-cause probing** (Math-2): the bug is
   observable in a sibling method, not the trigger test's method, so a harness
   anchored only on the trigger path misses it. Truly output-equivalent
   (undetectable-by-construction) patches exist in principle, but Math-2 is NOT
   one — it was detectable, just not where we looked.

---

# Recommendations

## To reduce FALSE POSITIVES

FPs come from the model asserting a relation that isn't universally true.

1. **Validate every invented relation before trusting it (no cheating needed).**
   A metamorphic relation is supposed to hold for *any* correct impl, so check
   it against behaviour you already trust:
   - the project's **passing test suite** (known-correct behaviour on the buggy
     checkout — free);
   - **non-triggering fuzz inputs on the buggy checkout** (buggy code is correct
     everywhere except the root-cause path).
   If the candidate relation ever fails on clearly-valid input → **discard it**
   before it can flag a patch. This is the honest version of "validate against a
   reference" (does NOT use the developer fix).
2. **Domain-guard relations; make the model state the precondition.** Prompt:
   "*state the exact precondition under which your relation holds and guard on
   it; if you cannot state one, do not assert it.*" Round-trip only on
   unambiguous values; monotonicity only when converged (small epsilon / large
   maxIter). Both observed FPs violate an unstated precondition.
3. **Rank oracle classes; treat free-form metamorphic as last resort.**
   Preference order: (a) lifted seed assertion, (b) construct-from-answer
   (sound by construction), (c) known algebraic identities, (d) free-form
   metamorphic guesses. Weight the prompt toward a–c; require (d) to pass
   validation (rec 1).
4. **Require a quorum to FLAG, not just ≥1 harness.** The FP was one harness of
   k firing on a bad relation while others cleared. Flag only if ≥2 harnesses
   fire, or if the *lifted/construct-from-answer* oracle fires (not only a
   free-form relation). Trade-off: may cost some recall — A/B test it.
5. **Confidence-tag findings.** If ONLY a free-form relation fires (not the
   trusted seed/constructed oracle), mark the finding low-confidence and require
   corroboration before reporting overfit.

## To reduce FALSE NEGATIVES

FNs come from oracles too loose to discriminate, from missing the discriminating
input, or from **masked-symptom** overfits (bug hidden behind a self-correcting
downstream computation; observable only in a sibling method — Math-2).

1. **Tighten loose-bound oracles to exact values.** When the trigger assertion
   is an inequality (`0 ≤ sample ≤ n`), supplement with an exact-value oracle
   via construct-from-answer or a differential reference — a loose bound passes
   for wrong impls.
2. **Patch-directed input construction.** Feed the *changed condition* into the
   prompt: e.g. "the patch changed `tmp < upper` to `tmp > upper`; construct
   inputs that make these two predicates differ, and assert there." This drives
   the fuzzer to the exact input class where the overfit's wrong branch is
   taken — the thing generic fuzzing misses.
3. **Probe root-cause / sibling methods directly, not just the trigger path.**
   Math-2's bug is invisible in the trigger method (`inverseCumulativeProbability`,
   self-corrected) but blatant in a sibling (`getNumericalMean` = −49.76). The
   harness should exercise the functions in the **reachable neighbourhood**
   directly (this is what the reachable-set fix enables) with sound per-method
   oracles (e.g. a hypergeometric mean must satisfy `0 ≤ mean ≤ sampleSize`),
   not only the top-level assertion the trigger test happens to use.
4. **Watch for self-correcting downstream stages (masking).** If the patched
   value feeds a bisection / convergence loop / clamp that washes out errors,
   the top-level output won't reveal the bug — assert on the intermediate or on
   a sibling method instead (see rec 3). Genuinely output-equivalent patches
   (undetectable by construction) also exist; distinguish them at eval time via a
   buggy/patched/fixed differential *across multiple methods* (not just the
   trigger method — that was the trap with Math-2) before excluding any from the
   recall denominator.
5. **Spend more coverage where it pays.** k=5 harnesses × 25s is small; recall
   grows with coverage. But prefer *directed* generation (rec 2) over brute
   force — diminishing returns otherwise. The now-fixed reachable-set steering
   helps spread across the neighbourhood; combine with patch-directed
   construction for the specific discriminating input.

## The central tension

Discrimination lives in the generalization step, which is simultaneously the FP
source (unsound relations) and the FN cure (tighter/more oracles). The two
fixes that attack both ends: **validate invented relations against
known-correct behaviour** (kills FPs) and **construct the discriminating input
from the patch diff** (kills FNs). Everything else is tuning.
