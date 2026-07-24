# Overfit-Patch Detection — Deep Analysis of a 3-Arm Flag Experiment (2026-07-21)

> **Purpose of this document.** A self-contained write-up of a controlled experiment on our
> overfit-patch detector, with a mechanistic root-cause analysis of every individual result.
> It is meant to be handed to a second reviewer for an independent opinion. Sections 6–8 are
> where I most want a second opinion (root causes, fixes, open questions). Findings are
> evidence-backed from the run traces; where I'm inferring, I say so.

---

## 0. Executive summary

We ran a 3-arm A/B/C on **15 fresh, never-tuned-on bugs** (18 leg-tasks: 8 overfit "recall" legs + 10 correct "precision" legs), flagship model `gpt-5.4`, differing only in two experimental flags. Aggregate result:

| Arm | TP | FN | FP | TN | P | R | F1 |
|---|---|---|---|---|---|---|---|
| **A — off (baseline)** | 5 | 3 | 3 | 7 | 0.62 | 0.62 | **0.62** |
| **B — +focused_synthesis** | 5 | 3 | 5 | 5 | 0.50 | 0.62 | 0.56 |
| **C — +attribution_judge** | 2 | 6 | 4 | 6 | 0.33 | 0.25 | 0.29 |

**Four headline conclusions, in order of confidence:**

1. **The single-roll comparison is confounded by run-to-run variance** (the pipeline's relation synthesis is an LLM sampling process). A dedicated trace audit showed the arm-C "collapse" was **not** caused by its flag — the attribution judge flipped **zero** outcomes; the regressions were independent synthesis rolls producing different oracles. **We cannot rank the arms from one roll each.** Flag effects are within the ±1–2-leg noise band.

2. **`--attribution_judge` should be retired** — but on **structural**, not numeric, grounds (see §5.2). Its decision criterion is orthogonal to soundness and it is provably unable to clear the false-positive class we actually generate.

3. **Trustworthy recall is materially lower than the 0.62 headline (~0.44).** Of 5 catches, only 3 are robust; 1 is fuzz-luck, 1 is a knife-edge sub-rounding-floor numeric catch (§4.1).

4. **The precision and recall failures share one root cause:** the pipeline reasons about *hypothetical* correctness ("could **any** correct implementation produce this firing?") rather than *measuring the actual build* (where the firing originates, what value it produces). This single policy produces **both** over-acquittals (false negatives) and over-convictions (false positives). The most promising fix — a **blast-radius fact** (does the firing come from code the patch touched?) — is developed in §6/RC-1, along with an honest account of why the naive "it fires on the buggy build too" version I first wrote down does **not** work.

---

## 1. What the pipeline does (context for the reviewer)

Goal: given a **buggy** Java method, its failing trigger test, and a **candidate APR patch**, decide whether the patch is **overfitting** (passes the test suite but diverges from the intended/developer fix) or **correct** — **without access to the developer fix** (that's the ground truth we're approximating).

Pipeline stations (per leg):
1. **Relation synthesis** — an LLM proposes metamorphic/contract "relations" the correct method should satisfy (e.g. "`solve` throws `IllegalArgumentException` for a non-bracketing interval", "`getChiSquare()` equals the weighted residual sum").
2. **Screening** — relations are compiled into Jazzer fuzz harnesses and run on the **buggy** build; a relation that fires on the buggy build is tagged `direction-confirmed` (taken as evidence it detects the defect).
3. **Harness acceptance** — a harness must compile **and** fire on the buggy build (the tripwire). *NB: `accepted_trigger_details` in each `result.jsonl` records these **buggy-build** firings, not patched-build ones.*
4. **Patched-fuzz** — accepted harnesses are run on the **candidate patch**. A firing here is a candidate overfit signal.
5. **Replay-on-patched** — surviving relations are replayed on the patched build (deterministic "trigger tier" replay on the failing test's own input literals, plus fuzzing).
6. **Soundness judge** (LLM) — for each patched-build firing, decides SOUND (a real contract violation ⇒ convict "overfitting") or UNSOUND (a correct implementation could legitimately produce this ⇒ ignore).
7. **Attribution judge** (LLM, only with `--attribution_judge`) — a second, facts-only judge that can veto a conviction.

Final prediction: **overfitting** if any firing survives as sound/attributed, else **correct**.

The experiment set: 15 bugs drawn (seeded, project-stratified) from the 27 semantic dev-split bugs **not** seen in the last 15 tuning runs, so the pipeline was never tuned on them. Each bug contributes its pinned overfit patch and/or pinned correct patch → 18 legs.

The two flags under test:
- **`--focused_synthesis`**: runs 4 per-source synthesis passes (formula / throws / family / state) instead of one, then unions + re-screens. Pitched to improve recall.
- **`--attribution_judge`**: adds the second veto judge (§7 above). Pitched to improve precision.

---

## 2. The critical caveat: run-to-run variance

The pipeline is **nondeterministic**: relation synthesis is an LLM sampling process, so each arm is an **independent draw** of the relation set, and downstream fuzzing/judging adds more variance. A dedicated audit of arm C (see §5.2) found that on the legs where arm C differed from arm A, **the flag itself changed nothing** — the differences were different synthesis rolls surfacing (or failing to surface) different oracles.

**Consequences:**
- The arm ranking (A > B > C by F1) is **not reliable** from one roll each. A ±1–2-leg swing on 18 legs moves F1 by ~0.1.
- There is a standing note in the codebase to this effect ("don't compare single samples"). This experiment violated it; the design should have been **N rolls per arm**, paired.
- **What survives the variance critique = results that are consistent across all three arms.** Those are the signal analyzed below.

Consistent-across-all-arms (real signal, not noise):
- **False positives:** Closure-70, Math-30, Math-65 (FP in all 3 arms).
- **False negatives:** Closure-38, Lang-63 (FN in all 3 arms).
- **Robust true positives:** Math-74, Math-82 (TP in all 3 arms); Math-73/Math-68 robust where they appear.
- **Robust true negatives:** Chart-11, Chart-19-correct, Lang-22, Math-82-correct, Math-86.

Variable-across-arms (largely noise): Chart-19-overfit, Math-104, Math-68, Math-73-overfit, Math-39, Math-73-correct.

---

## 3. How good is recall, really? (True Positives)

Arm A caught 5 of 8 overfits. Robustness audit of each catch:

| Leg | Catching oracle | Divergence | How caught | Verdict |
|---|---|---|---|---|
| **Math-73** (ACS) | `nonbracketing_zero_initial_throws` — `solve(f,min,max,initial)` must throw `IllegalArgumentException` for a non-bracketing interval | categorical: throw → **no-throw** | deterministic, **20000/20000** fuzz | **ROBUST** |
| **Math-82** (HDRepair) | returned optimum **violates constraint** (x=1.5 under `x≤1.0`) + value 10.0 vs 11.5 | categorical: **infeasible LP point** | deterministic seed | **ROBUST** |
| **Math-68** (Arja) | `jennrich-seed-p0` — trusted MINPACK regression param, off by **1.2e-5** at tol **1e-9** | large, above floor | deterministic seed | **ROBUST** (one sibling `fr-RMS` harness was itself unsound / pre-existing surface — don't double-count it) |
| **Math-74** (Arja) | `integrate_reaches_requested_end` — returns 10.9 instead of 11.0 | large & sound | **but** every seed/trigger oracle was UNSOUND; survived on one sound relation hit by only **285/20000 (~1.4%)** fuzz inputs; one judge stage was even fooled into keeping an unsound `ctor` relation | **FUZZ-FRAGILE** |
| **Math-104** (Elixir) | `gamma-p-1-1` — `regularizedGammaP(1,1)` off by **6.4e-11** | **below the pipeline's own 1e-9 rounding floor**; survived only via a verbatim 1e-14 test tolerance + an "exact closed-form" loophole | deterministic seed | **FRAGILE** (B and C both lost it — tolerance normalization makes 6.4e-11 vanish) |

**Reading:** the three robust catches rest on **categorical or large, contract-anchored divergences** (documented `@throws`; LP feasibility; trusted regression at a sane ≥1e-9 tolerance) — no plausible FP risk. The fragile tail is exactly the **sub-rounding-floor numeric** catch (104) and the **fuzz-hit-rate-dependent** catch (74).

> **Trustworthy recall ≈ 3.5/8 ≈ 0.44**, not the 0.62 headline (discount 104 entirely — already lost in B/C — and half-credit 74). The robust core is contract-anchored; the fragile tail is the first thing to disappear under tolerance normalization or fuzz-seed variance.

---

## 4. The precision problem (False Positives) — the highest-value target

Three correct patches were flagged as overfit in **all three arms** (so this is real, not variance). Every one is the **same two-stage failure**, and — critically — **all three fire on the BUGGY build too**, which is what makes them detectable *with information we have at detection time*.

| Leg | Convicting oracle | Fires on buggy build? | Why it's unsound (a correct impl produces the same firing) |
|---|---|---|---|
| **Math-30** (CapGen) | `large_identical_samples_pvalue_one` (p-value must ≈ 1.0 for identical samples) → `actual=NaN, n=46350` | **100% buggy / 100% patched** | The patch does **not** prevent the overflow (`n1*n2` is still `int*int` *before* widening to double), and there's a **separate int-overflow in `mannWhitneyU`'s `(n1*(n1+1))/2` that neither the patch nor the developer fix touches**. NaN at n=46350 is a shared artifact of untouched code. The comparison is even NaN-poisoned (`tol = 1e-6·max(1,\|NaN\|) = NaN`). |
| **Math-65** (CapGen) | `chiSquare_matches_weighted_residual_sum` → `chi=3661257.8 vs expected=2646059.4` (~38% gap) | **74% buggy / 48% patched** | The oracle re-derives `Σ wᵢ·(f(optimum)−tᵢ)²` and asserts it equals `getChiSquare()`, which actually sums the optimizer's **internal stored residuals** — a different bookkeeping convention. **The CapGen `getChiSquare` is byte-identical to the developer fix**, so this oracle fires on the dev fix identically → it cannot be a valid overfit detector. *(Correction to an earlier draft: the `errors[0] expected=0.004 vs 0.0020` oracle was the buggy-build **acceptance** trigger and stays quiet on the patch — it did NOT convict.)* |
| **Closure-70** (Jaid) | escaped `java.lang.IllegalStateException` at `TypeCheck.processForTesting:360` | **yes — structurally** | Thrown by **unpatched infrastructure** (`TypeCheck.java`, not the patched `TypedScopeCreator.java`) because the harness hand-rolls the type-checker driver but **omits the compiler/externs `init`** the real test fixture performs. Sibling warning-count oracles also fuzz the program identifiers while asserting the *original* hard-coded warning string. |

**Stage 1 — screening keeps a non-discriminating oracle.** The keep rule is "fires on the buggy build" (`direction-confirmed`), taken as evidence the check detects the defect. But a relation can fire on the buggy build for reasons **intrinsic to its own construction** (untouched int-overflow, missing test-fixture init, internal-vs-recomputed bookkeeping). Because there is no known-correct reference at screening time, these build-**independent** oracles pass the gate.

**Stage 2 — the judge convicts against an idealized ("Platonic") contract.** Every SOUND verdict reasons *"no correct implementation could produce this"* — "identical samples ⇒ p=1.0," "getChiSquare ⇒ Σw·r²," "valid JS ⇒ no exception." In every case the **ground-truth-correct patch (and, for Math-65, the byte-identical developer fix) does produce it**, because the divergence lives in code/inputs the fix legitimately doesn't touch. The judge is idealizing the API instead of measuring the actual build. Two aggravators: Math-65 was **4-UNSOUND / 2-SOUND** across judge calls on one oracle family yet convicted on the minority; Closure-70 had **two contradictory verdicts on the identical exception** (Judge-A UNSOUND, Judge-B SOUND).

**The single fix that catches all three — and it uses only information available at detection time.** Feed the soundness judge the **differential-replay fact**: re-run the *specific reproducing input* on the **BUGGY build**; if the oracle fires identically there, the firing is **build-independent** (pre-existing surface) and cannot prove the patch is overfit. This is a *mechanical fact*, not "judging harder" (consistent with the standing principle *"every FP class is fixed by computing a fact into its evidence, never by asking the judge to judge harder"*). **Closure-70 is direct proof it works:** the judge that had this fact ("occurs identically on the unpatched buggy build") correctly acquitted its harness; the judge that lacked it convicted the identical exception. Note this is NOT the dev-fix reference I worried about in an earlier draft — the **buggy build is available at detection**, which is what makes the fix viable (see RC-1, now revised).

**Secondary FP hardening** (each would independently catch one of the three): a **domain-regime guard** (Math-30's relation extrapolated to n≈46350, orders of magnitude past the test's sizes); a **reconstruction-fidelity gate** (Closure-70's harness omitted the fixture's compiler `init` and fuzzed identifiers while asserting a fixed output); a **NaN-poisoned-comparison guard** (`tol=NaN` should never count as a violation); and **verdict-split awareness** (a 4-UNSOUND/2-SOUND split should not convict on the minority).

---

## 5. The recall gap (False Negatives)

Three overfits were missed. Each missed at a different station:

- **Chart-19** (Arja) — *right observable, wrong input fence.* The overfit puts the null-guard one level too deep (`AbstractObjectList.indexOf` instead of `CategoryPlot`), so it diverges only on **direct `indexOf(null)` calls where null is genuinely absent**. The distinguishing relation (`indexOf(null) == -1`) **was** synthesized, but its harness builds a **sparse list with null holes** (`set(0..3); set(4..7)`), so `indexOf(null)` returns a hole index on *every* build → the judge **correctly** ruled the observed firing UNSOUND. The clean discriminator (empty/dense list → overfit throws) was never constructed. *(Arm B's focused_synthesis re-fenced the input and caught it — the one real focused_synthesis win.)*
- **Closure-38** (SequenceR) — *over-cautious acquittal (architectural).* The overfit drops a sign test so compact subtraction prints `0- 0` instead of `0-0`. The oracle **fired on exactly the right input**, but the soundness judge dismissed it: "whitespace is semantically-equivalent JS, a correct printer could emit `0- 0`." Worse, the harness-generation prompt itself orders "**never compare raw strings — normalize whitespace**." For a code **printer/serializer, whitespace IS the contract**, and the divergence lives precisely where the pipeline is designed to look away. Missed by all arms.
- **Lang-63** (Arja) — *synthesis-reach gap.* The overfit adds a spurious `end.add(DATE,-1)` that only executes on a field **borrow** (`endValue < startValue`). Synthesis produced borrow-forcing inputs (in an overload-agreement relation) **and** an independent ground-truth observable (`whole-months`) — but **never combined them**: the `whole-months` oracle was fenced to day-1/midnight endpoints (borrow-free), where the overfit is dormant. No independent oracle ever stressed the borrow path.

---

## 6. True Negatives (for completeness) and the fragility they reveal

All 7 TNs are mechanism **"nothing fired"** — the correct patch honored every oracle, so the soundness judge **never even ran on a correct patch** in arm A. So the TN column is "easy," carried by clean passes, not by the judge earning them.

But **2 of 7 are fragile near-misses that flip to FP under the other arms**, via the *same* channel as the §4 FPs:
- **Math-39** — a brittle **exact-float** oracle (`if (finalTime != 0.01d) throw`) stays quiet only because the correct patch's step clamp lands *exactly* on 0.01 for the seed; a generalized target-time relation (arms B/C) fires a 1-ULP mismatch → FP.
- **Math-73-correct** — the correct patch legitimately **adds strictness** (throws `IllegalArgumentException` for non-bracketing intervals); a **sibling/overload-agreement** relation (arm B) breaks because one overload now throws while the deprecated one doesn't → FP on correct code.

This is the strongest evidence that **the FP mechanism and the "recall-boosting" machinery are the same thing**: turning up oracle/relation generation (focused_synthesis) makes these brittle relations fire, converting robust TNs into FPs.

---

## 7. The two flags

### 7.1 `--focused_synthesis` — one real gain bought with two FPs and a lost catch; its core design claim is refuted

Mechanism: arm A runs **one** broad "write your best N relations" synthesis call; arm B runs **four narrow passes** — formula / throws / family / state — each told to *exhaustively enumerate* its kind, then unions + re-screens. On the 4 flipped legs, **arm A scored 3/4, arm B scored 1/4**, at ~2× the token cost:

- **`+` Chart-19 (FN→TP), real gain.** The **throws pass** reliably generated a *generalized* `@throws` relation (`getRangeAxisIndex_null_throws_after_extra_range_axis`) that arm A's single-pass lottery never emitted; it fired on a fuzzed variant (7310/20000) the overfit's input-special-casing couldn't dodge. This is the roll-variance fix the flag was designed for — and the only clean win.
- **`−` Math-104 (TP→FN), lost catch via over-fencing.** Arm A caught it with a **broadly-fenced** overload-agreement oracle (`a,x` independent over [0,20]), so fuzzing reached the divergence region. Arm B's **family pass** emitted six overload variants but, per its own soundness rule, fenced them all **narrowly** (`x≈a`) — "more correct" (bit-exact) yet **blind**, never reaching the divergence. The union+re-screen didn't drop the relation, it **re-fenced it into silence**. (Consistent with the TP agent's separate finding that Math-104 is fragile anyway — its other catch, the 6.4e-11 seed oracle, is below the rounding floor.)
- **`−` Math-39 (TN→FP), manufactured over-generalization.** The formula/state passes produced reinforcing "derivative-stays-within-interval" relations with **stricter-than-test bounds** (plain `t>end`, no ulp slack) plus one that fuzzes **reversed/negative intervals** — driving a correct adaptive integrator into a regime where it legitimately evaluates trial stages past the endpoint. Judge upheld SOUND by anchoring on the trusted test contract.
- **`−` Math-73-correct (TN→FP), degenerate-case relation.** The formula pass's "enumerate ALL exact-value rules" instruction produced `four_arg_returns_min_when_min_is_root` — which puts the root **exactly at an endpoint**, violating `BrentSolver`'s bracketing precondition, so a correct solver may return garbage. The judge was fooled by the large magnitude of the "wrong" value.

**The design claim is refuted.** The flag's own help text asserts "the union is screened identically, so no new FP risk." That's false: **the screen only tests *compiles + fires-selectively-on-the-buggy-build* — it is not a soundness test.** Unsound-but-selective over-generalizations pass it, and the *only* real FP gate (the soundness judge) is simultaneously **overloaded** — 2–3× more candidates ⇒ more patched-build firings ⇒ more judge calls (Math-104 went 2→8) ⇒ more chances for a false-SOUND leak. The "enumerate ALL" framing of the formula/state passes **systematically manufactures boundary/degenerate relations** a single best-N pass wouldn't bother to emit. Net on these flips: it trades a few roll-variance catches for a larger number of judge-leaked FPs plus mass-effect losses. If salvaged at all, it needs a **soundness-aware screen before the judge** and a **fencing-preservation guard** so consolidation can't over-narrow a broad convicting oracle.

### 7.2 `--attribution_judge` — retire on structural grounds
A dedicated audit found:
- Attribution **ran on only 6/18 legs and flipped 0 outcomes.** The arm-C recall regressions (Math-68/73/104 TP→FN) **never reached it** — they were killed upstream by the soundness judge ruling firings unsound (tolerance arguments) or the harness not firing in that roll. So the F1=0.29 was **variance, not the flag**.
- **Structural flaw:** its dominant criterion is Q1 = "does the **buggy** build also violate this check?" (a `direction-confirmed` fact). That is **orthogonal to soundness** — it asks "is the divergence also on the buggy build / is it a documented contract," not "could a **correct** implementation produce this firing." The property that makes a firing a false positive (the correct patch's residual also appears on the buggy build) is exactly the property that makes attribution say **ATTRIBUTED** → it **cannot clear the FP class we generate**. And it "fails open" (anything but an explicit `NOT_ATTRIBUTED` keeps the conviction), so on true positives it can only subtract.
- Its own help text documents a prior run measuring it "vetoing ~100% of sound generalization catches." Even though that harm didn't manifest in *this* roll, the upside is structurally unreachable.

**Recommendation: retire the flag** (or leave it hard-off, which it already is). Re-keying it to the right question just re-litigates soundness, which its prompt forbids.

---

## 8. Unifying root causes (where I want a second opinion)

**RC-1 — The judge idealizes the API instead of measuring the actual build (highest leverage; fixable *now*, but the exact signal needs care).**
The soundness judge convicts against a "Platonic" contract — "identical samples ⇒ p=1.0," "getChiSquare ⇒ Σw·r²," "valid JS ⇒ no exception" — asserting "no correct implementation could produce this." All 3 consistent FPs falsify that: the ground-truth-correct patch (and Math-65's byte-identical dev fix) **does** produce the firing, because it lives in code/inputs the fix legitimately doesn't touch.

The FP deep-dive proposed a clean fix — "replay the convicting input on the buggy build; if it fires identically, it's pre-existing surface ⇒ acquit." **I stress-tested this and it is NOT clean as stated.** Harness *acceptance requires* firing on the buggy build (the tripwire), so **every** accepted oracle — TP and FP alike — is `direction-confirmed`. I verified that Math-73 (a true positive) and Math-30 (a false positive) are *superficially identical*: both `direction-confirmed`, both "fire on the failing test's own input literals on the patched build." So "fires on buggy ⇒ acquit" would **also acquit the true positives** → mass false negatives. The naive version fails.

The **defensible** signal is finer — *where the firing originates*, not merely *that it fires on buggy*:
- The 3 FPs fire because of code **outside the patch's changed region and the bug's locus**: Math-30's NaN comes from an int-overflow in `mannWhitneyU` (untouched); Closure-70's exception is thrown in `TypeCheck.java` (the patch is in `TypedScopeCreator.java`); Math-65's gap is internal-residual bookkeeping vs a re-derived formula. The firing's **root cause is not in the patch's blast radius.**
- A TP like Math-73 fires on the non-bracketing input because the overfit's own modified `solve` fails to generalize — the firing **is** in the patch's scope, and a correct fix (per the `@throws` contract) would behave differently.
- *Proposed fix:* a **coverage/blast-radius fact** fed to the judge — does the firing's stack/root-cause lie in code the patch changed (or the bug's documented locus)? If it originates in untouched code, treat as pre-existing surface. Corroborate with a value-identity check (patched value == buggy value on the same input). This is available at detection time and does **not** need the dev fix. (Closure-70 shows a weaker version already works: the judge that noted the exception is thrown by unpatched infra acquitted; the one that didn't convicted.)
- **Honest caveat for the reviewer:** distinguishing "unsound oracle (a correct impl fires)" from "sound oracle, overfit fires" is fundamentally the dev-fix question. The blast-radius heuristic is a *proxy* and will have its own failure modes (e.g. a bug whose correct fix legitimately touches shared downstream code). This is the single most important thing to pressure-test.
- **Scope limit:** even done right, this kills only the *pre-existing-surface* FP class (the 3 consistent FPs). The *"correct patch introduces a new-but-still-correct behavior"* class (Math-39 exact-float, Math-73-correct added-strictness) does **not** fire on the buggy build and needs RC-3/RC-4 instead.

**RC-2 — Screening tests "fires selectively on buggy," not soundness, so the judge is the *only* FP gate — and it's overloadable.**
`direction-confirmed` = "compiles + fires on the buggy build." That admits (a) relations that fire for reasons **intrinsic to their construction** (Chart-19's sparse list fires on *every* build; the FN version) and (b) **unsound-but-selective over-generalizations** (focused_synthesis's `four_arg_returns_min`, strict `t>end`) that genuinely fire on the buggy build yet also fire on correct code. Both survive to the soundness judge, which is the sole precision gate — and inflating candidate volume (focused_synthesis) overloads it into more false-SOUND leaks. *Fix:* a **soundness-aware pre-judge screen** that drops relations asserting exact-value / no-slack / precondition-violating properties the contract doesn't guarantee, so volume can't buy FPs.

**RC-3 — Input-fence discipline.**
Distinguishing relations exist but their inputs land in sub-domains where overfit and correct **agree** (sparse collections with null holes; borrow-free dates). *Fix:* synthesis rules that fence inputs to the boundary/dense/empty/borrow-forcing sub-domains where divergences live; down-weight sibling/overload-agreement relations when the patched line is in code shared by both siblings (structurally blind).

**RC-4 — Numeric fragility around the rounding floor.**
Sub-1e-9 catches (Math-104) are knife-edge and vanish under tolerance normalization; brittle exact-float oracles (Math-39) flip TN→FP. *Fix:* treat sub-floor numeric catches as low-confidence by design; never synthesize exact-float-equality oracles on values that depend on floating-point accumulation.

**RC-5 — Format-erasure blind spot.**
Whitespace/format normalization (sensible for most methods) erases the *entire* signal for printer/serializer methods. *Fix:* a method-class carve-out where formatting is the contract.

**RC-6 — Nondeterminism / evaluation methodology.**
Single-roll comparisons are unreliable. *Fix:* N rolls per configuration, paired; report confidence intervals; only act on differences that exceed the variance band.

---

## 9. Recommended actions (prioritized)

1. **Give the soundness judge a blast-radius fact** (RC-1): for each patched-build firing, compute whether its root cause (stack/coverage) lies in code the patch changed or the bug's documented locus; if it originates in **untouched** code, treat as pre-existing surface. Targets all 3 consistent FPs (Math-30, Math-65, Closure-70), needs no dev fix, and is mechanical. **Do not** implement the naive "fires on buggy ⇒ acquit" version — I verified it would also acquit the true positives (they're all `direction-confirmed`). Pressure-test the blast-radius proxy first (see §10 Q1).
2. **Re-run the flag A/B/C with ≥3 rolls per arm** before drawing any flag conclusion beyond "off is not worse" (RC-6). One roll per arm cannot separate flag effect from ±1–2-leg synthesis noise.
3. **Retire `--attribution_judge`** (structural; §7.2) — do NOT re-enable focused_synthesis either, pending a soundness-aware screen.
4. **Soundness-aware pre-judge screen** (RC-2): drop relations asserting exact-value / no-slack / precondition-violating properties before they reach (and overload) the judge.
5. **Input-fence discipline + down-weight sibling-agreement on shared code** (RC-3) — recovers Chart-19 and Lang-63 recall without the focused_synthesis FP cost.
6. **Printer/format carve-out** (RC-5) — recovers Closure-38 (whitespace *is* the contract for serializers).
7. **Down-weight sub-floor numeric catches** (RC-4) — stop crediting Math-104-style knife-edge catches (below the 1e-9 floor) as reliable recall; never synthesize exact-float-equality oracles.

---

## 10. Open questions for the reviewer

1. **Is the blast-radius proxy (RC-1) sound, and where does it fail?** Distinguishing "unsound oracle" from "sound oracle, overfit fires" is fundamentally the dev-fix question, which we don't have at detection. The blast-radius heuristic ("firing originates in untouched code ⇒ pre-existing surface") correctly separates the 3 FPs from the TPs *in this sample*, but its failure mode is a bug whose correct fix legitimately touches shared downstream code (then a real overfit's firing could originate in "untouched" code and be wrongly acquitted). Is this proxy good enough, and what's the right way to bound its error? **This is the crux question.**
2. Is the right primary metric **trustworthy recall** (robust catches only) rather than raw recall? If so, how should we operationalize "robust" (contract-anchored / above-floor / deterministic replay)?
3. Given the variance, is a **panel of N synthesis rolls with majority-vote conviction** a better production design than a single roll — trading cost for stability?
4. `focused_synthesis` has a real, isolated win (Chart-19 re-fencing) buried under noise and FP cost. Is the win worth salvaging by porting *just* the input-refencing behavior into the base synthesizer, rather than the whole 4-pass flag?
5. Are we over-indexing on Math-heavy bugs? 9 of 15 subset bugs are `Math` — the fragility findings (numeric floors) may be Math-biased. Would a re-draw with more Chart/Closure/Lang change the precision/recall balance?

---

## Appendix A — full per-leg outcomes across the three arms

| # | Leg | Truth | A (off) | B (+focused) | C (+attr) | Notes |
|---|---|---|---|---|---|---|
| 01 | Chart-11-CapGen | correct | TN | TN | TN | robust (boolean equality) |
| 02 | Chart-19-Arja | overfit | **FN** | **TP** | FN | wrong input fence; B re-fenced |
| 03 | Chart-19-ACS | correct | TN | TN | TN | safe but weak oracle set |
| 04 | Closure-38-SequenceR | overfit | **FN** | FN | FN | whitespace erased (architectural) |
| 05 | Closure-70-Jaid | correct | **FP** | FP | FP | brittle warning-count oracle |
| 06 | Lang-22-DeepRepair | correct | TN | TN | TN | robust (integer overflow) |
| 07 | Lang-63-Arja | overfit | **FN** | FN | FN | synthesis-reach (borrow path) |
| 08 | Math-30-CapGen | correct | **FP** | FP | FP | correct NaN at overflow boundary |
| 09 | Math-39-Arja | correct | TN | **FP** | **FP** | brittle exact-float `!=0.01d` |
| 10 | Math-65-CapGen | correct | **FP** | FP | FP | over-strong formula identity |
| 11 | Math-68-Arja | overfit | TP | TP | **FN** | robust catch; C lost to variance |
| 12 | Math-73-ACS | overfit | TP | TP | **FN** | robust `@throws`; C lost to variance |
| 13 | Math-73-Arja | correct | TN | **FP** | TN | correct adds strictness → overload-agreement FP |
| 14 | Math-74-Arja | overfit | TP | TP | TP | right answer, fuzz-lucky (~1.4%) |
| 15 | Math-82-HDRepair | overfit | TP | TP | TP | robust (infeasible LP point) |
| 16 | Math-82-ACS | correct | TN | TN | TN | robust (large gap oracle) |
| 17 | Math-86-Arja | correct | TN | TN | TN | robust (fixed-matrix `@throws`) |
| 18 | Math-104-Elixir | overfit | TP | **FN** | **FN** | knife-edge 6.4e-11, below floor |

## Appendix B — provenance

- Runs: `armA_off_20260721_122647`, `armB_focused_20260721_133026`, `armC_attr_20260721_150801` (VM `scratch/runs/`; ~330 KB `trace.md` per leg).
- Cases: `suites/cases/{armA_off,armB_focused,armC_attr}.cases` (identical 18-leg set; flags differ only).
- Bug selection: `suites/splits/semantic_split.jsonl` (dev side) minus bugs seen in the last 15 runs; seeded, project-stratified.
- Analysis method: 6 parallel trace-reading agents (per outcome class + per flag), cross-checked. Sections 4 (FP) and 7.1 (focused_synthesis) synthesize corroborating evidence from the TN/TP/FN/attribution audits.
- Key code refs: soundness/attribution judges `src/java/relations/relation_verifier.py`; orchestration `src/java/run.py`; harness prompts `src/java/harness/prompts.py`.
