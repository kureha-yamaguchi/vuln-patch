# Ground-truth backtrack — Phase 1a/2, the seven decisive cases

The seven that set the bucket-(c) ceiling count: the three chronic accusations
and the pair's four variance accusations. Done first because this count is what
the pair and fresh12 decisions wait on.

**Ground truth used:** all seven are `Dcorrect` patches, so every accusing check
is a false accusation by construction. `defects4j` is empty on the VM, so no
dev-fix replay was available — but none was needed: the question Phase 1 asks is
*which fact decides the case*, and that is answerable from the recorded material
(the check's claim, the observed values, the shown class source, the failing
test). This is the same material the earlier per-case adjudications used.

**Firewall respected:** ground truth (the `Dcorrect` label) informs the design
question only. No fact proposed below requires knowing the label at detection
time.

24 accusing checks across the 7 cases. Each deciding fact is stated as a general
mechanism, per the statement test.

## The table

| case | accusing claim, in short | the fact that decides it | bucket |
|---|---|---|---|
| **Math-65** | `getChiSquare()` must equal Σ weight·residual² | the method's own body computes `residual²/weight` — it divides | **(a)** shown, ignored |
| **Lang-60** | `contains('\0')` must be false outside the active prefix | `contains()`'s body reads `char[] thisBuf = buffer;` and scans the raw array, not the prefix | **(a)** shown, ignored |
| **Chart-26** | the entity's `getAxis()` must be `this` | the shown source constructs it from `this` — so the retrieved entity is not the one just added; a harness-retrieval divergence | **(a)** shown, quoted, still accused |
| **Math-39** | the solver must not evaluate derivatives outside the interval | embedded Runge-Kutta stepsize control legitimately probes beyond the endpoint; the step logic is in the shown class | **(a)** shown, ignored |
| **Math-73** | `solve(f,min,max,initial)` on `f(x)=x−min` must return exactly `min` | the solver's documented accuracy contract permits any point within tolerance | **(a)** if the javadoc is shown |
| **Math-30** | a p-value must be in [0,1], symmetric, non-NaN | the firing input sits at an arithmetic-overflow boundary (46341² is the first int-overflowing square), where the correct behaviour is undefined | **(b)** computable, uncollected |
| **Closure-62** | the formatted output must match | the harness normalises whitespace before comparing, so the fired value is a derivative of the pinned value — the raw pre-normalisation value is never recorded | **(b)** collectible, uncollected |

## Phase 2 counts

* **bucket (a) — collected but not binding: 5 of 7.** The deciding fact was in
  the material the reviewer was shown, and the accusation was made anyway. In
  Chart-26's case the reviewer *quoted the source verbatim* and still accused.
* **bucket (b) — collectible at detection time, not collected: 2 of 7.**
  Math-30's arithmetic-boundary fact and Closure-62's raw-value recording. Both
  were independently identified earlier this cycle, which is a useful convergence.
* **bucket (c) — requires ground truth itself: 0 of 7.**

## The ceiling is zero, and that is not the good news it sounds like

No decisive case is structurally unwinnable. Every one is decidable from facts
that are either already present or mechanically collectible. **So the precision
ceiling is not imposed by the firewall.**

But bucket (a) dominating at 5 of 7 collides head-on with the separating-fact
study, and the collision is the real finding:

* The backtrack says *the deciding fact was there* in 5 of 7 cases.
* The separating study says *no recorded feature distinguishes* a kept genuine
  catch from a kept false accusation — corroboration count, firing location,
  fire-rate presence and replay-confirmation all fail to separate.

Both are true, and together they say something sharper than either alone: **the
deciding fact is case-specific, not a feature.** What decides Math-65 is *that
particular formula*; what decides Lang-60 is *that particular loop bound*. There
is no general property shared by "the fact that refutes this accusation" across
cases, which is precisely why no feature-based gate separates the populations.

**So bucket (a) is not an enforcement backlog.** Enforcing it would require a
mechanism that reads the shown source and reasons about whether it contradicts
the check — which is exactly what the reviewer already does, and fails at, 5 times
out of 7. A gate cannot do case-specific reasoning; that is what the model is for.

## What that leaves, honestly

1. **Bucket (b) is the only buildable menu**, and it is two items — see Phase 3.
   Neither addresses the 5 bucket-(a) cases.
2. ~~**The bucket-(a) five are a model-capability limit, not a missing-evidence
   limit.**~~ **SUPERSEDED — see the correction below.** The evidence is present,
   adjacent, and sometimes quoted, and better delivery has failed three times
   (the disputed-computation fact, the placement audit, Chart-26's own citation).
   But "capability limit" does not survive the data: the same model on the same
   evidence both quotes the deciding line and ignores it.
3. **The measured precision ceiling on this trap set is ~5 false accusations
   under the CURRENT TASK STRUCTURE** — not because of the firewall, and not
   because the model cannot do it, but because nothing requires the question to be
   asked.

The honest cycle-8 question is no longer "what fact should we collect" but
"what changes the reviewer's behaviour when the refuting fact is already in front
of it".

## CORRECTION: "model-capability limit" was too strong

That phrase overreached, and the refutation is in this project's own data. On
Math-65, the SAME model on the SAME evidence both quotes the deciding formula and
ignores it — 2 dismissals quote `residualsWeights[i]` verbatim, 4 accusations
cite NONE. A capability limit does not vary run to run on identical input. What
varies is **engagement**: whether the question gets asked at all, while the
reviewer weighs everything else at once and accusations require no citation.

That is a task-structure gap, and this project has already closed one of those:
the CITATION line turned dismissal grounding from prose into a structured,
mechanically-verifiable output, at 100% compliance. The equivalent move on the
accusation side has never been tried.

## But the narrow-contradiction design addresses only HALF the population

Before that experiment is funded, one measurement, because it changes the success
criteria. Not every accusation fails by contradicting the shown source:

| case | checks | is the deciding fact a premise-vs-source contradiction? |
|---|---|---|
| Lang-60 | 2 | **yes** — "only the active prefix is searchable" vs `contains()` scanning the raw buffer |
| Math-39 | 4 | likely — depends on the step logic being shown |
| Math-73 | 1 | likely — depends on the tolerance javadoc being shown |
| Math-65 | 5 | **4 yes, 1 no** — one accusation states the CORRECT formula and accuses anyway |
| Chart-26 | 2 | **no** — the premise AGREES with the source; the error is which entity the harness retrieved |
| Math-30 | 6 | **no** — a universal property, contradicted by nothing in the source |
| Closure-62 | 4 | **no** — the deciding fact is the harness's own normalisation |

**~11 of 24 could plausibly be voided by the narrow question; ~13 would correctly
answer NONE and leave the accusation standing.**

The Math-65 roll-B verdict is the instructive one: *"any correct implementation
must return the sum of `residuals[i] * residuals[i] / residualsWeights[i]`"* —
correct formula, divide not multiply — followed by an accusation that the patched
build deviates from it. Nothing in the source contradicts that premise. The error
is downstream, in what the reviewer believed the build returned.

So the experiment is still worth running, and its criteria should be set against
~11 rather than 24. A result of "voids 8 of 11 contradiction-class accusations and
answers NONE on all genuine catches" would be a strong success; measured against
24 the same result would look like a failure.

That tension is the finding, and it should be resolved before more precision work
is funded, not during it — with the experiment as the resolution step.
