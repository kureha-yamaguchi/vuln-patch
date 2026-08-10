# Math-65 formula-relation desk read (2026-08-10)

Answers the flag raised in `docs/plan.md` 8.40: Math-65's CORRECT CapGen
patch is convicted FP in six consecutive fresh draws, every conviction a
chi-square / RMS "documented formula" relation. History: 8.29 (the
instability), 8.34, and the varbase baseline `varbase_20260808_183839`
legs 01-03 (FP / TN / FP).

Analysis only. No code changed, no runs launched, nothing pushed to the VM.

**Stations this read targets**

| Station | Module | Failure mode found here |
|---|---|---|
| Relation screening | `src/java/relations/relation_screen.py` | the direction-confirmed exemption lets a check that fires on 38-91% of buggy inputs past the out-of-domain cap |
| Patched-side replay / buggy replay | `src/java/run.py` ~2985-3300 | the buggy-side value comparison is SHADOWED by another oracle, so it returns `unknown` and no value fact can decide |
| Evidence facts | `src/java/relations/evidence_facts.py` | `[fact:rate-indiscriminate]` is computed, stamped, and then overridable by "a shown contract" |
| Relation synthesis prompt | `src/java/relations/relation_synth.py` | the documented-formula-first + coverage + deep-dive clauses concentrate 5-7 of ~11 slots on one broken observable family |

---

## 0. The short version

The javadoc is RIGHT. The patch is RIGHT. Both relation families are
wrong, but **not** in the way 8.40 guessed.

8.40 guessed "the formula relations mis-state the javadoc formula (weight
vs inverse-weight)". Half true — one family does. But that is not why they
fire. **Every one of the eleven convicting relations fires on the BUGGY
build too**, at 38%-91% of random inputs and on the failing test's own
literals. They were not kept for being silent on buggy; they were kept for
being LOUD on buggy (`kept: direction-confirmed`), which the screen reads
as "aimed at the defect" and which EXEMPTS them from the 20% out-of-domain
cap.

The reason they fire on both builds has nothing to do with weights. It is
this: `LevenbergMarquardtOptimizer` overwrites the field
`AbstractLeastSquaresOptimizer.residuals` with `Qᵀ·r` before `optimize()`
returns. `getChiSquare()` and `getRMS()` read that field. The relations
recompute `r = target[i] - optimum.getValueRef()[i]`. `Qᵀ` preserves the
length of the residual vector but not its components, so any per-component
weighted formula disagrees — on the buggy build, on the patched build, for
either weight convention.

---

## 1. What the code actually is

### 1.1 The patch

`/home/code/drr/Patches/Dcorrect/CapGen/Math/patch1-Math-65-CapGen.patch`
(VM `hetzner`), verbatim:

```
--- /src/main/java/org/apache/commons/math/optimization/general/AbstractLeastSquaresOptimizer.java
+++ /src/main/java/org/apache/commons/math/optimization/general/AbstractLeastSquaresOptimizer.java
@@ -255,7 +255,7 @@ public abstract class AbstractLeastSquaresOptimizer implements DifferentiableMul
         double chiSquare = 0;
         for (int i = 0; i < rows; ++i) {
             final double residual = residuals[i];
-            chiSquare += residual * residual / residualsWeights[i];
+            chiSquare += ((residualsWeights[i]) * residual) * residual;
         }
         return chiSquare;
     }
```

`diff` of the two checkouts confirms this is the ONLY difference in the
whole file, and `LevenbergMarquardtOptimizer.java` is byte-identical
between the two builds:

```
258c258
<             chiSquare += residual * residual / residualsWeights[i];
---
>             chiSquare += ((residualsWeights[i]) * residual) * residual;
--- LM diff ---
IDENTICAL
```

### 1.2 The javadoc, verbatim

`/tmp/d4j/Math_65_buggy/src/main/java/org/apache/commons/math/optimization/general/AbstractLeastSquaresOptimizer.java`

Lines 248-261 (getChiSquare, buggy build):

```java
    /**
     * Get a Chi-Square-like value assuming the N residuals follow N
     * distinct normal distributions centered on 0 and whose variances are
     * the reciprocal of the weights.
     * @return chi-square value
     */
    public double getChiSquare() {
        double chiSquare = 0;
        for (int i = 0; i < rows; ++i) {
            final double residual = residuals[i];
            chiSquare += residual * residual / residualsWeights[i];
        }
        return chiSquare;
    }
```

Lines 229-246 (getRMS, IDENTICAL on both builds):

```java
    /**
     * Get the Root Mean Square value.
     * Get the Root Mean Square value, i.e. the root of the arithmetic
     * mean of the square of all weighted residuals. This is related to the
     * criterion that is minimized by the optimizer as follows: if
     * <em>c</em> if the criterion, and <em>n</em> is the number of
     * measurements, then the RMS is <em>sqrt (c/n)</em>.
     *
     * @return RMS value
     */
    public double getRMS() {
        double criterion = 0;
        for (int i = 0; i < rows; ++i) {
            final double residual = residuals[i];
            criterion += residual * residual * residualsWeights[i];
        }
        return Math.sqrt(criterion / rows);
    }
```

### 1.3 Does the doc match the correct implementation? YES

The javadoc says the residuals' **variances** are the reciprocal of the
weights: `σᵢ² = 1/wᵢ`. A chi-square is `Σ rᵢ²/σᵢ²`. Substituting,
`Σ rᵢ² · wᵢ` — multiply by the weight. That is the PATCHED line. The buggy
line divides by the weight, i.e. it treats the weight as the variance.

Three independent confirmations that the weight-MULTIPLY reading is the
intended one:

1. `updateResidualsAndCost()`, line 222 (identical on both builds), the
   criterion the optimizer minimises:
   `cost += residualsWeights[i] * residual * residual;`
2. `getRMS()`, line 243 (identical on both builds):
   `criterion += residual * residual * residualsWeights[i];`
3. The failing test itself. `d4j.tests.trigger` is
   `LevenbergMarquardtOptimizerTest::testCircleFitting`, whose second half
   sets every weight to 2.0 and pins the chi-square-derived answer:

```java
431:        double[] target = new double[circle.getN()];
432:        Arrays.fill(target, 0.0);
433:        double[] weights = new double[circle.getN()];
434:        Arrays.fill(weights, 2.0);
435:        optimizer.optimize(circle, target, weights, new double[] { 98.680, 47.345 });
...
441:        errors = optimizer.guessParametersErrors();
442:        assertEquals(0.004, errors[0], 0.001);
443:        assertEquals(0.004, errors[1], 0.001);
```

`guessParametersErrors()` is `Math.sqrt(getChiSquare() / (rows - cols))`
scaled by the covariance diagonal (lines 310-324). The test's non-unit
weights are what make the two conventions differ, and the pinned value
0.004 is the weight-multiply answer.

**Verdict: the doc is right and the patched implementation matches it.**
There is no contract ambiguity to arbitrate.

### 1.4 The mechanism nobody modelled: `residuals` is `Qᵀ·r` after optimize

`/tmp/d4j/Math_65_buggy/src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java`
(byte-identical on both builds), inside `doOptimize()`:

```java
276:            qrDecomposition();
277:
278:            // compute Qt.res
279:            qTy(residuals);
```

and, inside the inner loop, the field is swapped with a saved copy on
every iteration and swapped back on a failed one:

```java
342:                double previousCost = cost;
343:                double[] tmpVec = residuals;
344:                residuals = oldRes;
345:                oldRes    = tmpVec;
...
424:                } else {
425:                    // failed iteration, reset the previous values
426:                    cost = previousCost;
...
431:                    tmpVec    = residuals;
432:                    residuals = oldRes;
433:                    oldRes    = tmpVec;
434:                }
```

`qTy` (lines 863-875) multiplies the vector in place by the orthogonal
`Qᵀ` of the jacobian's QR decomposition. So when `optimize()` returns:

- `residuals[]` holds `Qᵀ·r`, **not** `target − objective`;
- `‖Qᵀ·r‖₂ = ‖r‖₂` (orthogonal transform preserves length), but the
  individual components are completely different;
- therefore `Σ wᵢ·(Qᵀr)ᵢ²  ≠  Σ wᵢ·rᵢ²` whenever the weights are not all
  equal, on EITHER build.

Worked example, from `stack_confirm_20260810_140852/05`, the firing line of
`chiSquare_matches_documented_sum_residual_squared_over_weight`:

```
targetValues=[-672.0, -665.0]  residualsWeights=[15.0, 1.0]
objective=[-671.5495873833432, -671.5495873833432]
residuals=[-1.2012868595973567, 6.454213960349189]
cost=6.777917653547891
```

- true residuals `r = target − objective = [-0.45041, 6.54959]`;
  `‖r‖₂ = 6.5651`
- stored `residuals` `ρ = [-1.20129, 6.45421]`; `‖ρ‖₂ = 6.5651`
  — same length, different components. `ρ = Qᵀr`, confirmed numerically.
- `cost = sqrt(Σ wᵢ rᵢ²) = sqrt(15·0.2029 + 42.897) = 6.7779` — matches the
  recorded `cost`, so `updateResidualsAndCost` did store the true `r` and
  `qTy` then overwrote it.
- patched `getChiSquare()` = `Σ wᵢ ρᵢ² = 15·1.44312 + 41.6567 = 63.3035`
  — matches the recorded `got 63.3032`.
- the relation's `expected` = `Σ rᵢ²/wᵢ = 42.9106` — matches the recorded
  `expected 42.9106`.
- and the BUGGY build would return `Σ ρᵢ²/wᵢ = 41.753` — which also
  violates the same check, by 1.16, against a tolerance of `1e-9 × 42.9 ≈
  4.3e-8`.

That last line is the whole story in one number.

---

## 2. Every convicting relation, verbatim

Eleven relations across the six legs survived the patched-side replay
(`relation_replay_kept`) and carried the conviction. Bodies extracted from
`runs-archive/runs/*/0*/trace.md`; screen ratios from the
`screen-fuzz-buggy` events; patched-side counts from each relation's
`note`.

### Family A — weight-MULTIPLY (states the doc correctly)

**`chiSquare_matches_weighted_residual_formula`** (mechb/07)

> contract: `AbstractLeastSquaresOptimizer.getChiSquare(): "Get a Chi-Square-like value assuming the N residuals follow N distinct normal distributions centered on 0 and whose variances are the reciprocal of the weights." Therefore the value is the sum, over all measurements, of weight[i] * residual[i]^2.`

```java
double expected = weights[0] * (target[0] - value[0]) * (target[0] - value[0])
                + weights[1] * (target[1] - value[1]) * (target[1] - value[1]);
double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(expected), Math.abs(chi)));
if (Math.abs(chi - expected) > tol) { ... }
```

buggy 16486/20000 = 82%; patched 9241/20000 = 46%; `kept: direction-confirmed`

**`chiSquare_matches_weighted_squared_residuals`** (mechb/08 and stack/06)

> contract (stack/06): `AbstractLeastSquaresOptimizer.getChiSquare(): "Get a Chi-Square-like value assuming the N residuals follow N distinct normal distributions centered on 0 and whose variances are the reciprocal of the weights." For least-squares residuals r_i = target_i - objective_i, this is the weighted sum of squared residuals.`

```java
double[] value = optimum.getValueRef();
double expected = weights[0] * (target[0] - value[0]) * (target[0] - value[0])
                + weights[1] * (target[1] - value[1]) * (target[1] - value[1]);
double tol = 1.0e-9;
if (!(Math.abs(chiSquare - expected) <= tol * Math.max(1.0, Math.max(Math.abs(chiSquare), Math.abs(expected))))) { ... }
```

mechb/08: buggy 18119/20000 = 91%; patched 7871/20000 = 39%.
stack/06: buggy 14851/20000 = 74%; patched 9270/20000 = 46%. Both
`kept: direction-confirmed`.

Judge's keep reason, stack/06, verbatim:

> "The fired check asserts the documented contract that, after a successful least-squares optimize call, `getChiSquare()` must equal the weighted sum of squared residuals `Σ weight_i * (target_i - objective_i)^2`, and no correct implementation that honors "variances are the reciprocal of the weights" could complete on this input and return the observed mismatching value `2.2222...` instead of `3.5555...`. CITATION: NONE"

### Family A(RMS) — the same formula, read through getRMS

Six of the eleven. `getRMS()` is byte-identical on both builds and
`optimize()` is byte-identical on both builds, so **these relations return
the same numbers on both builds by construction.** They have exactly zero
power to discriminate this patch.

**`rms_matches_documented_mean_of_weighted_squares`** (stack/06)

> contract: `AbstractLeastSquaresOptimizer.getRMS(): "the root of the arithmetic mean of the square of all weighted residuals" and "if c is the criterion, and n is the number of measurements, then the RMS is sqrt(c/n)".`

```java
double criterion = weights[0] * (target[0] - value[0]) * (target[0] - value[0])
                 + weights[1] * (target[1] - value[1]) * (target[1] - value[1]);
double expected = Math.sqrt(criterion / 2.0);
if (!(Math.abs(rms - expected) <= tol * Math.max(1.0, Math.max(Math.abs(rms), Math.abs(expected))))) { ... }
```

buggy 8077/20000 = 40%; patched 9135/20000 = 46%; `kept: direction-confirmed`

The other five, all the same shape:

| relation | leg | buggy | patched |
|---|---|---|---|
| `rms_matches_documented_weighted_residual_formula` | mechb/08 | 9113/20000 = 46% | 10192/20000 = 51% |
| `rms_matches_weighted_residual_mean` | mechb/09 | 9182/20000 = 46% | 9602/20000 = 48% |
| `rms_matches_weighted_residual_mean_square` | stack/04 | 10483/20000 = 52% | 11445/20000 = 57% |
| `rms_matches_documented_weighted_residual_formula` | stack/05 | 8104/20000 = 41% | 7912/20000 = 40% |
| `rms_matches_weighted_residual_mean` | stack/05 | 10457/20000 = 52% | 11539/20000 = 58% |

Judge's keep reason, mechb/08, verbatim:

> "The fired check is a direct recomputation of the documented `getRMS()` contract from the returned optimum point and public weights/targets, and a correct implementation cannot complete on this valid input with `getRMS()` returning 2.6986702517631276 when the weighted-residual formula gives 2.1005611561970694. CITATION: NONE"

That claim is false in a way the pipeline could have computed: the buggy
build returns 2.6986702517631276 as well.

### Family B — inverse-weight (mis-states the doc, and takes the removed patch line as the spec)

**`chiSquare_matches_documented_sum_over_inverse_weights`** (stack/04)

> contract: `AbstractLeastSquaresOptimizer.getChiSquare(): "Get a Chi-Square-like value assuming the N residuals follow N distinct normal distributions centered on 0 and whose variances are the reciprocal of the weights." The visible implementation contract is the sum of squared residuals divided by the corresponding weights.`

```java
double expected = ((target[0] - x) * (target[0] - x)) / weights[0]
                + ((target[1] - x) * (target[1] - x)) / weights[1];
```

buggy 10470/20000 = 52% (a second candidate of the same name: 11771/20000
= 59%); patched 17208/20000 = 86%; `kept: direction-confirmed`

**`chiSquare_matches_documented_inverse_weight_formula`** (stack/05)

> contract: `AbstractLeastSquaresOptimizer.getChiSquare(): "Get a Chi-Square-like value assuming the N residuals follow N distinct normal distributions centered on 0 and whose variances are the reciprocal of the weights." The removed line shows the contract formula as sum(residual^2 / weight).`

buggy 10688/20000 = 53%; patched 15629/20000 = 78%;
`kept: direction-confirmed`

Note the second sentence. This relation names the patch's `-` line as the
contract. That is diff-targeting in its purest form: asserting the
pre-patch behaviour as the spec.

### Family C — the reflection variant (in varbase only, not in the six)

Worth recording because it is a genuinely different failure and because it
kills one obvious fix. `varbase/01`'s `chiSquare_matches_documented_formula`
installs the optimizer's internal state directly by reflection, so it
never touches `Qᵀ`:

```java
for (int i = 0; i < n; i++) {
  residuals[i] = data.consumeInt(-1000000, 1000000) / 1000.0;
  weights[i] = data.consumeInt(1, 1000000) / 1000.0;
  expected += (residuals[i] * residuals[i]) / weights[i];
}
...
rowsF.setInt(opt, n);
residualsF.set(opt, residuals);
weightsF.set(opt, weights);
actual = opt.getChiSquare();
```

Its stated contract:

> `// holds because: getChiSquare is documented to return a "Chi-Square-like value assuming the N residuals follow N distinct normal distributions centered on 0 and whose variances are the reciprocal of the weights"; this is the sum over measurements of residual[i]^2 / weight[i].`

This is the buggy implementation, exactly. It scored **0/20000 on the
buggy build** and was kept as a "silent on buggy (tripwire)" — the shape
the screen calls the strongest catch signal — and it fired on the patched
build and convicted. It is the only genuine "silent on buggy, loud on
patched" case in any of the nine Math-65 legs, and it appears in exactly
one draw.

It also refutes the fix option 8.40's (c) suggested: *"formula relations
must quote the javadoc line they encode."* This relation DOES quote the
javadoc line, verbatim and correctly, and then derives the wrong formula
from it in the very next clause. Quoting is not the missing step;
arithmetic is.

---

## 3. The 2×2 verdict

The brief's grid is `relation-matches-doc × doc-matches-correct-impl`.
The right column is settled: **the doc matches the correct implementation**
(section 1.3). So no relation lands in the "contract ambiguity" cell.
But the grid turns out not to be the deciding axis at all, so a third
column is needed — whether the relation's *arguments* match what the method
reads.

| Family | Relations | States the doc? | Matches buggy impl? | Matches patched impl? | Reads what the method reads? | Verdict |
|---|---|---|---|---|---|---|
| A (chi, `Σ w·r²`) | 3 (mechb/07, mechb/08, stack/06) | **yes, correctly** | no | formula yes | **NO** — recomputes `r` from `optimum.getValueRef()`, method reads `Qᵀr` | RELATION WRONG (wrong argument, right formula) |
| A(RMS) (`sqrt(Σ w·r² / n)`) | 6 | **yes, correctly** | n/a — getRMS is identical on both builds | n/a | **NO** — same `Qᵀr` mistake | RELATION WRONG, and **zero discriminating power by construction** |
| B (chi, `Σ r²/w`) | 2 (stack/04, stack/05) | **no** — reads "variances are the reciprocal of the weights" as "divide by the weights", and one of them names the removed patch line as the spec | formula yes | no | **NO** — same `Qᵀr` mistake | RELATION WRONG twice over (wrong formula AND wrong argument); diff-targeting |
| C (reflection, `Σ r²/w`) | 1 (varbase/01 only) | **no** — same inversion | **exactly** | no | yes (state installed directly) | RELATION WRONG — encodes the bug; the classic invention |

Nothing lands in "doc wrong". **All eleven convictions in the six legs are
relation-wrong.** The doc-vs-implementation axis is not where the FP lives.

---

## 4. (e) The asymmetry — and why the brief's premise was inverted

The brief asks: "the buggy-side screen kept these relations (they must
have been silent on buggy — how, if the formula mis-states the doc for
both builds)?"

**They were not silent on buggy.** All eleven were kept as
`kept: direction-confirmed`, which means the exact opposite: they fired on
the buggy build's copy of the failing test's own input literals, on both
of two replays. Their random-input buggy fire rates are 40%, 41%, 46%,
46%, 46%, 52%, 52%, 52%, 53%, 74%, 82%, 91%.

Here is the mechanism, from `relation_screen.py`. The module's own header
sets the rule:

> "A relation that fires on a LARGE fraction of random valid inputs on the
> buggy build is out-of-domain — it contradicts behaviour we know is
> mostly correct, so it would flag almost any implementation. DROP."

with `MAX_FIRE_RATIO = 0.20`. But the direction-confirm branch overrides it:

```python
        if direction == 'confirmed':
            # The reliable, correctly-aimed case: fires exactly where the
            # failing test says the buggy code is wrong. Rank first and
            # EXEMPT from the ratio cap (a correct check aimed at the bug
            # legitimately fires on almost every input once P0.2 stopped
            # swallowing its alarm).
```

The exemption is unconditional. A relation firing on 91% of buggy inputs
is promoted to rank ONE, ahead of every selective firer, purely because it
also fires on the trigger corpus.

**So there is no asymmetry to explain.** The relation is loud on both
builds. The conviction does not come from a discrimination signal at all.
It comes from the buggy-side replay failing to produce a value.

The failure is SHADOWING. `run.py` replays the exact firing input on the
buggy build using the FULL harness — every oracle compiled in. On the
buggy build a *different* oracle throws first, the JVM dies there, and the
firing relation's own message is never printed. Verbatim from
`stack_confirm_20260810_140852/06/trace.md`:

> "[buggy-replay fact] on this exact input a DIFFERENT check fired first on the buggy build (circle-dense-errors-0), so whether THIS check fires there is UNKNOWN — the replay is shadowed, not confirming. The screening DIRECTION-CONFIRMED fact was established on screening inputs, which may lie in a different input regime than this firing; it does NOT by itself establish the buggy build violates this check at THIS input. With no per-input attribution fact, judge on soundness alone, sceptically: to keep, the check's expected value must be justified by a shown contract or trusted value that covers THIS input's regime."

The recorded outcome of the value comparison, same trace:

```
recorded 0 buggy / 4 patched key(s); value-verdict=unknown
{'buggy_values': {}, 'patched_values': {...}, 'value_verdict': 'unknown',
 'buggy_msg_present': False, 'buggy_replay_status': 'crashed'}
```

The muted re-replay then tried to mute the shadowing oracle and gave up:

> "pass=1/4 mute_set_size=1 status=crashed diverted=False — muted=circle-dense-errors-0 fired=['circle-dense-errors-0'] -> stop: mute set stopped growing, UNKNOWN kept"

At that point the instruction is "judge on soundness alone, sceptically:
to keep, the check's expected value must be justified by a shown
contract". These relations have a shown contract — a verbatim javadoc
quote. They keep.

The rate fact fires too and is also overridden. `evidence_facts.py`
stamped `[fact:rate-indiscriminate]` 4-6 times per leg on these very
firings, with text ending:

> "...indiscriminate; the firing is intrinsic to the check/setup
> construction, not a detection of the defect. **Keep only with a shown
> contract that makes every one of those inputs a genuine violation.**"

The escape hatch and the pro-keep instruction are the same clause. Two
computed facts both point at the check, and both are worded so that a
javadoc quotation beats them.

**The key mechanical fact, stated once:** `getRMS()` and `optimize()` are
byte-identical between the buggy and patched builds — only `getChiSquare()`
differs — so for any input, the six RMS relations compute the SAME `actual`
and the SAME `expected` on both builds. They cannot distinguish the two
builds even in principle. They convicted six times because the one
measurement that would have said so (the buggy-side value at the firing
input) was shadowed out and returned `unknown`.

---

## 5. (d) Why the new stack made this WORSE than varbase

First, what did NOT change. The clauses 8.40 points at — the
documented-formula-first standing strategy, the R2 coverage requirement,
the deep-dive enumeration protocol — all date to commit `3c6b3ff`
(2026-07-21), *before* varbase ran on 2026-08-08. `git log -S` on each
clause returns only that commit. They are the standing cause, not the
trend.

The standing cause, verbatim from `relation_synth.py`:

```
"STANDING STRATEGY — DOCUMENTED FORMULAS (checked first, before"
" anything else): scan the javadoc of the touched class's numeric"
" getters ... for a stated closed-form formula ... If ANY such"
" formula exists, your FIRST relation MUST be that"
" formula: recompute it independently from the object's own parameters"
" and compare with the generous magnitude-scaled tolerance above. A"
" documented formula is the strongest relation class there is — it is"
" deterministic, it holds for every correct implementation by"
" definition, and a patch that leaves the value wrong anywhere in the"
" domain cannot pass it."
```

`MUST` guarantees at least one member of this family in every single
draw, and "recompute it independently from the object's own parameters"
is precisely the instruction that produces the `Qᵀr` mistake — it tells
the generator to rebuild the inputs itself rather than read what the
method reads.

The deep-dive protocol then multiplies it:

```
"DEEP-DIVE PROTOCOL — do this enumeration BEFORE proposing:"
" (1) LIST every documented observable in scope — each stated"
" formula, each declared @throws, each documented range/format,"
" each documented family agreement, each read-only/state guarantee"
" ...; (2) MARK which of these the"
" patch text could plausibly affect, directly or through shared"
" state; (3) SPEND your slots on the marked ones per the coverage"
" requirement above."
```

Step (2) is the amplifier. The patch text IS the chi-square line, so every
documented observable that mentions chi-square or RMS gets marked, and
step (3) says spend the slots there. `getRMS()` gets marked "through
shared state" — correctly, it does share `residuals` — and becomes a
second, independent documented-formula recomputation of the same broken
model.

What DID change between varbase and the six legs: commits `6ef2e3b`
(8.37, the two-tier catch) on 08-09 and `530b71f` (8.39, the doc guard) on
08-10. The two-tier catch mandates that every probe call on the patched
class gets its own try block that RE-THROWS rather than returns. Under the
old single-catch shape, a check whose `optimize()` or `getValueRef()` path
threw simply returned. Under the new shape the comparison always runs to
completion.

The measured effect, counting members of the chi/RMS formula family among
the relations that reached the buggy screen:

| suite | leg | family members / all screened | direction-confirmed survivors | relations that fired on patched | kept | verdict |
|---|---|---|---|---|---|---|
| varbase | 01 | 6/11 | 2 | 3 | 3 | FP |
| varbase | 02 | 4/12 | 4 | 2 | 0 | **TN** |
| varbase | 03 | 4/12 | 3 | 2 | 1 | FP |
| mechb | 07 | 7/11 | 6 | 6 | 1 | FP |
| mechb | 08 | 7/11 | 6 | 5 | 2 | FP |
| mechb | 09 | 6/12 | 6 | 8 | 1 | FP |
| stack | 04 | 5/12 | 3 | 2 | 2 | FP |
| stack | 05 | 7/12 | 6 | 5 | 3 | FP |
| stack | 06 | 6/10 | 5 | 4 | 2 | FP |

Concentration rose from ~4/12 to ~6.5/11, direction-confirmed survivors
from 2-4 to 3-6, and firings per leg from 2-3 to 2-8. Nothing about the
individual relation got more convincing — varbase carried the identical
relations at identical fire rates. What changed is the number of
independent draws from the same broken urn per leg. The verifier only has
to keep ONE. varbase/02 was a TN because the verifier happened to drop
both of that leg's two firings; with five or six firings that stops
happening.

This is the same lottery mechanic 8.40 celebrated on the Chart-19 side
("Compare invdiv (1 catch in 3 draws) and varbase (1-2 of 3): this leg is
now deterministic"). More coverage makes real catches deterministic and it
makes systematic false accusations deterministic. The stack did not
introduce this FP; it removed the variance that was hiding it.

---

## 6. The one recommended fix

### 6.1 What it is

**Make the buggy-side replay UNSHADOWABLE, then let the observed values
decide.**

Today `run.py` replays the firing input on the buggy build using the FULL
harness. Any other oracle that throws first kills the JVM and the firing
relation's own message is never emitted, so `compare_fired_values` gets
nothing and `_value_verdict` stays `"unknown"` — which is exactly the
state in which the judge is told to fall back on the check's stated
contract.

The change: when the full-harness buggy replay returns `unknown` for a
firing relation, recompile a harness on the buggy build containing **only
that relation's check body** and run it on **only that one crashing
input**. Nothing else is compiled in, so nothing can shadow it. The
machinery already exists —
`relation_screen.measure_single_check` (Spec M) already compiles one check
body against the buggy classpath through `_screen_harness_source`, and
`_measure_on_corpus` already runs a check against a specific byte corpus.
The new code is the wiring plus reading the printed message.

Feed the resulting message into the existing `observed_values` /
`compare_fired_values` pair and attach ONE new fact with two terminal
readings:

- **R1 (degenerate case).** Buggy `actual` equals patched `actual` at this
  input → `[fact:same-value-both-builds]`. The patch changed nothing this
  check observes. Terminal dismissal. This is the existing
  `_value_verdict == "identical"` rung, newly reachable.
- **R2.** Both builds violate with different values, AND the patched value
  is **strictly closer** to the check's own `expected` than the buggy value
  is → `[fact:patch-moves-toward-the-checks-own-expected]`. The check's own
  yardstick says the patch improved the observable it is condemning.
  Terminal dismissal.
- Anything else (buggy silent, buggy farther, non-numeric values, harness
  fails to build or run) → `unknown`, behaviour exactly as today. Fail
  closed.

No prose, no "judge harder" — both readings are arithmetic on two numbers
the pipeline already prints.

### 6.2 What it would have done to the six legs

| leg | kept relations | R1/R2 outcome | leg verdict |
|---|---|---|---|
| mechb/07 | chi-A | R2: expected 3.9375, patched 4.5636 (Δ 0.63), buggy 0.0564 (Δ 3.88) → dismiss | **converts** |
| mechb/08 | RMS + chi-A | RMS → R1; chi-A → R1 or R2 (values not recorded in the trace excerpt; unverified) | **likely converts** |
| mechb/09 | RMS only | R1 (identical by construction) | **converts** |
| stack/04 | chi-B + RMS | RMS → R1; chi-B survives (its expected is the buggy formula, so buggy is *closer*) | stays FP |
| stack/05 | RMS ×2 + chi-B | RMS → R1; chi-B survives (expected 42.9106, patched Δ 20.39, buggy Δ 1.16) | stays FP |
| stack/06 | chi-A + RMS | chi-A → R1 (buggy 2.2222 = patched 2.2222, both vs expected 3.5556); RMS → R1 | **converts** |

Expected: 3 legs convert outright, 1 likely, 2 remain. Six of the eleven
relations (all the RMS ones) are killed with mathematical certainty, since
`getRMS` and `optimize` are byte-identical across the builds.

### 6.3 Why not the alternatives

- **Make `[fact:rate-indiscriminate]` terminal.** Tempting — all eleven
  carry both rates above the 20% cap. But it is UNSAFE: the archived
  Chart-19 TP relations carry the same profile. `mechb/01`'s
  `indexOf-null-absent-is-minus-one` is buggy 61% / patched 100%;
  `varbase/12`'s `categoryplot-nullRangeAxisProbe-throwsIAE` is buggy 100%
  / patched 42%. Making this terminal would destroy every Chart-19
  conviction. Rejected on the evidence.
- **Terminal-dismiss on "buggy also violates at this input".** Also
  unsafe: `stack_confirm/01` and `/03` are TP legs that record
  `[fact:fires-on-both-confirmed] the exact firing input fires the SAME
  check on the BUGGY build`. Firing on both is not enough; the VALUES have
  to be compared. Hence R1/R2 rather than a bare both-fired rule.
- **"Formula relations must quote the javadoc line they encode."**
  Refuted by Family C, which quotes the line correctly and then derives the
  wrong formula from it in the next clause (section 2, Family C).
- **"Screen the documented formula on the buggy build at non-trigger
  inputs."** The brief asked whether this is sound given the bug is IN
  this method. It is not, for this bug. The defect (`r²/w` vs `w·r²`)
  changes the answer at essentially every input with a non-unit weight, so
  a *correct* formula check would also fire on almost every buggy input.
  The screen cannot separate "correct check detecting a pervasive defect"
  from "broken check", which is exactly why the direction-confirm
  exemption exists. Worse, the one subdomain where the two builds provably
  agree — unit weights — is also the subdomain where `Qᵀ` is harmless
  (`Σ 1·(Qᵀr)ᵢ² = ‖Qᵀr‖² = ‖r‖² = Σ 1·rᵢ²`), so the broken checks are
  silent there too. Every unit-weight relation in every leg scored
  **0/20000 on buggy**. The screen would find nothing.

### 6.4 Pre-registration gates

Written before any build, per house rule. Nothing here is built against
n=6.

- **G1 — no catch regresses.** Replay every archived leg with a TP
  conviction (Chart-19 ×8 across invdiv/varbase/mechb/stack, Lang-41 ×3,
  plus the Math-2 and Chart-26 correct-side legs). Every current
  conviction must still convict, and every current TN must stay TN. Any
  single TP→FN converts the fix to REJECTED, not to "tune the threshold".
- **G2 — the target converts.** At least 3 of the 6 archived Math-65 legs
  flip FP→TN on replay, and the six RMS relations are dismissed in 6/6 of
  the legs where they appear. Fewer than 3 legs → the fix is real but
  insufficient; ship only if G1 is clean and record the shortfall.
- **G3 — the mechanism is demonstrably live, not idle.** Every Math-65
  leg's trace must carry an `isolated-buggy-replay` event with a
  NON-`unknown` value verdict on every firing that previously recorded
  `buggy_replay_status='crashed'` + `value_verdict='unknown'`. Zero
  shadowed outcomes on those firings. (This is the 8.39 lesson: prove the
  guard was active, not merely silent.)
- **G4 — fail closed.** Inject a deliberate compile failure and a
  deliberate timeout into the isolated harness; both must yield `unknown`
  and leave the verdict byte-for-byte identical to today's. No new
  dismissal path may be reachable from a failed measurement.
- **G5 — no cross-run pooling.** The isolated replay is built from THIS
  run's own crash input and THIS run's own check body, discarded at run
  end. Nothing persists across runs.
- **G6 — held-out confirmation.** After G1-G5 pass on the archived
  replay, one fresh 3-arm roll on bugs NOT used to design this (not
  Math-65, not Chart-19, not Lang-41) before the fix counts as shipped.
  Then the full 24-leg flagship sweep as the milestone before/after.

---

## 7. Unresolved

1. **Family B (2 of 11) survives the recommended fix.** Its `expected` is
   the buggy formula, so the buggy build sits *closer* to it and R2 does
   not fire. Its real defect is different and has its own name:
   `stack/05`'s relation states its contract as *"The removed line shows
   the contract formula as sum(residual^2 / weight)"* — it read the
   patch's `-` line as the specification. That is diff-targeting, a named
   failure mode this codebase already fights elsewhere, and it deserves
   its own read: a mechanical detector would compare the check's
   `expected` expression against the normalised removed hunk text. I am
   not proposing it here because textual expression matching is brittle
   and I have n=2.
2. **The two-tier catch's exact contribution is inferred, not proved.**
   Section 5's counts show concentration and firing rate both rose between
   varbase and the six legs, and `6ef2e3b`/`530b71f` are the only relevant
   commits in the window. I did not isolate which of the two moved the
   numbers. A zero-LLM replay of the varbase Math-65 legs under the new
   prompt would settle it cheaply.
3. **One value pair is unverified.** `mechb/08`'s chi-A firing
   (`1196.857776451311` vs `817.2909405208217`) does not carry its
   `residualsWeights` / `residuals` in the trace excerpt I extracted, so I
   could not compute the buggy-side value by hand. Section 6.2 marks that
   leg "likely converts" rather than "converts".
4. **The direction-confirmed exemption is still unconditional.** This read
   found no mechanical predicate that separates Math-65's
   direction-confirmed relations from Chart-19's at screen time — the fire
   rates overlap completely (Math-65 40-91%, Chart-19 0-100%). The fix
   above works at replay time instead. If a screen-time separator exists,
   this read did not find it.
