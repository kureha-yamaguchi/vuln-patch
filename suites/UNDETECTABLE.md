# Environment-undetectable overfit legs — exclude from recall denominators

Overfit patches proven behaviorally inseparable from the developer fix **in
our run environment** (OpenJDK 11 on the Hetzner VM). No sound black-box
oracle can distinguish them here: any harness that "catches" them would have
to assert non-contractual surface (exception message text), which false-
positives correct patches. These are not mislabels — the Doverfitting label
encodes behavior our environment cannot reproduce.

Keep the **correct** legs of these bugs in suites (they still measure
precision). Do not count the overfit legs below as FNs; do not spend
prompt/oracle work on them.

| Leg | Evidence | Date |
|---|---|---|
| Lang-7 / Arja (patch1-plausible) | Manual differential probe, Arja build vs fresh Lang-7f build, JVM 11.0.31, ~50 shaped inputs through `createNumber` + `createBigDecimal`: zero strong divergences — every difference is NFE-vs-NFE with message text only (BigDecimal parser msg vs dev guard msg). The dev guard defends against the LANG-822 OS-X-JVM quirk (`new BigDecimal("--…")` parsing to a wrong value), which modern JVMs do not have. Probe + outputs: hetzner `/tmp/l7probe/`. Details: `semantic-recall-brainstorm.md` addendum §A1. | 2026-07-16 |
| Lang-22 / Arja | `certify_detectability`: 0/909 divergences; B1 probe: 0 divergences over ~1,700 lines. The deleted `abs≤1` guard is redundant with the general GCD loop — output-equivalent. | 2026-07-15 |
| Math-70 / SketchFix (patch1-plausible) | `certify_detectability` (v1 AND v2 widened-surface probe): 0 divergences both times. Code reading confirms: the patch IS the dev fix (`solve(min,max)` → `solve(f,min,max)`) plus a dead disjunct (`\|\| i < 0` on a loop counter that starts at 0 — always false). Extensionally identical to the dev fix on every JVM, not just ours. **Bug rejoined eval via patch2 (27 strong) — see pinned_tasks.** | 2026-07-16 |
| Closure-123 / SequenceR (patch1, sole file) | NEW category `no_sound_oracle_divergence`: divergence exists but is formatting-only (redundant parentheses; emitted programs parse to identical ASTs). A sound oracle may not assert output text, so no admissible harness can flag it. Witness `/tmp/d4j/wit_anom/WC123.java`. | 2026-07-16 |

Fallback attempts on unpinnable bugs (2026-07-16 pm): Lang-7/patch2-Arja →
0 strong (16 message-only, same env-ceiling signature; 5 siblings
untried); Lang-22/patch2-Arja → 0 div over 935 lines (2 siblings
untried). Both bugs remain excluded. Math-63/Closure-86/Math-70 fallbacks
SUCCEEDED (46/55/27 strong) — those bugs are back in via pin_rank 2.

NOT undetectable (listed to prevent regression of old triage):
- **Chart-1 / Arja patch1**: certified 472 strong divergences (clobbers
  `itemLabelGeneratorList`). The old "likely undetectable" triage examined a
  different one of the six Arja patch files. Detectable; keep evaluating.
- **Math-2 / Arja patch1**: certified 117 strong value divergences — but
  ONLY by the v2 widened-surface probe (2026-07-16). The v1 probe returned
  a FALSE ZERO (0/549 lines): it anchored on the patched method
  (`inverseCumulativeProbability`), whose outputs are identical on both
  builds even at overflow parameters; the divergence is visible only via
  `getNumericalMean` — a sibling observable the patch never touched. Fixed
  by requiring probes to also cover the failing test's object surface
  (every public accessor at the test's exact constructor args). Lesson:
  treat any 0-divergence verdict from a patch-method-only probe as
  unverified.

Full suite-leg certification sweep (2026-07-16, JVM 11.0.31, ~45k tokens):
Lang-27/DeepRepair 30 strong, Lang-43/Arja 39 strong, Lang-55/Arja 214
strong, Chart-5/DeepRepair 396 strong, Chart-19/Arja 14 strong,
Math-2/Arja 117 strong (v2), Math-70/SketchFix 0 (v1+v2). Records:
hetzner `scratch/eval_expansion/certified_suite.jsonl` + `_v2.jsonl`.

---

# B3 expansion-pool sweep (2026-07-16 pm) — zero-divergence deep-dive verdicts

28 overfit legs + 33 correct legs probed (one patch per bug per label,
33 paired bugs; records `b3_certified_overfit.jsonl` /
`b3_mislabel_correct.jsonl`, log `b3_sweep.log`). 14 overfit legs certified
detectable outright. Every zero was deep-dived (witness programs under
`/tmp/wit/` and `/tmp/d4j/witness57/` on the VM):

**GENUINE EQUIVALENCE — exclude from recall denominators; most are drr
LABEL ERRORS, not environment ceilings:**
| Leg | Mechanism |
|---|---|
| Math-59 / SequenceR | patch IS the dev fix textually (`a<=b?b:Float.isNaN(a+b)?NaN:a` — ternary right-associativity; only whitespace/parens differ). **Outright mislabel; report upstream.** |
| Closure-115 / ssFix | dev fix deleted a block + its use; ssFix deleted only the use, leaving dead-pure leftover computation (guarded unreachable by `isDirectCallNodeReplacementPossible`). ≡ dev fix on any JVM. Suspected mislabel. |
| Closure-86 / SequenceR (patch2) | `return NodeUtil.isImmutableValue(NEW-node)` — predicate can never match Token.NEW → ≡ dev fix's `return false`. Suspected mislabel. NOTE: sibling patch3/patch5 are NOT equivalent — verdicts are per patch file. |
| Math-30 / ssFix | cast-at-use ≡ dev fix's cast-at-declaration (int→double widening exact; the residual `n1*n2` int overflow exists identically in the dev fix). |
| Math-63 / CapGen | patch = dev fix + redundant `\|\| x == y` disjunct (implied by ulp-equals incl. ±0.0). |

**FALSE ZEROS — detectable; probe missed the surface (witnessed):**
| Leg | Witness |
|---|---|
| Chart-7 / Arja | overlapping `SimpleTimePeriod`s: fixed `getMaxMiddleIndex()=1`, Arja `0` (Arja rewired the getter to `maxEndIndex`; the two coincide on non-overlapping periods — all the probe generated). |
| Lang-41 / Arja | String overloads left buggy: `getShortClassName("[Ljava.lang.String;")` fixed `String[]` vs Arja `String;`; Class overloads agree (partial fix). |
| Lang-60 / Arja | `contains(char)` destructively shrinks capacity 32→3 (documented read-only), and `indexOf` after `delete` reads stale chars: fixed −1 vs Arja 2. |
| Closure-62 / Jaid | `\|\| charno==len` escapes the LINE guard: REGION-excerpt formatter at charno==len prints a caret the dev fix doesn't. |
| Math-57 / ssFix | `float sum` vs dev fix's `double sum`: k-means++ seeding diverges on ~50% of seeds with 1e20-scale coordinates (d² overflows float). |

**Probe-coverage lesson (v3 candidates for `_PROBE_INSTRUCTIONS`):** the
five misses share one root cause — probes stay on the happy input manifold.
Generic fixes: probe ALL public overloads/sibling methods of the patched
class; re-read observer state (capacity/size/getters) after every call;
sweep non-default enum/constructor configs; add an extreme-magnitude input
tier whenever a patch changes a floating-point type's width.

**Correct-leg (mislabel-probe) anomalies — FINAL VERDICTS:**
- Lang-50 / SimFix (43 strong div): **ARTIFACT — pipeline patch-APPLIER
  bug, not the file and not a mislabel.** The patch file is complete but
  its two hunks are in DESCENDING line order (`@@ -472` then `@@ -293`);
  the applier silently applied only the first (no .rej), leaving
  `getDateInstance` buggy — the certified build even FAILS the d4j trigger
  test. With both hunks applied: trigger tests pass, 0/518 probe
  divergences (the odd-looking insert only makes the style cache
  write-only — perf, not values). Label CORRECT stands.
  **Actions: (1) fix the applier to handle out-of-order hunks; (2) add a
  pipeline invariant — run the d4j trigger tests on the patched build
  before probing/fuzzing (would have caught this instantly); (3) audit all
  multi-hunk drr patches for silent hunk drops — the applier is SHARED
  with the main pipeline (`PatchedProjectBuilder`), so every historical
  multi-hunk-leg result is suspect.**
- Lang-41 / SimFix (5 strong div): **TRUE MISLABEL (partial fix)** — the
  patch reroutes only the Class overloads and leaves the root-cause String
  helpers broken (`getShortClassName("[Ljava.lang.String;")` → `String;`
  vs dev fix `String[]`; internally inconsistent with its own Class-path
  output). And the kicker: **the Doverfitting Lang-41/Arja patch is
  BYTE-IDENTICAL to this Dcorrect SimFix patch** — drr labels the same
  semantics both correct and overfitting. The 0div-vs-5div asymmetry was
  pure probe-generation variance. Drop Lang-41/SimFix from Dcorrect use
  (or mark contested); the Arja-side "overfitting" label is the right one
  for both. Caveat: no d4j test pins the String-overload descriptor
  behavior, so a narrow bug-scope reading would defend the label — under
  drr's equivalence-to-dev-fix standard it does not stand.

**Infra (from the failure diagnosis):** Closure-63 is DEPRECATED in the
installed Defects4J — drop both legs from the pool permanently. Closure-18
overfit-leg probe hung on its first input (regenerate; a timeout bump alone
won't fix it). Math-71/Arja-o, Chart-12/Arja-c, Chart-26/Jaid-c,
Closure-62/Jaid-c: LLM probe-compile near-misses (wrong API for the old
library versions) — retry likely succeeds; the patch files themselves apply
cleanly. Math-53/Arja-c: probe bug (array index outside the per-input
try/catch) — retry.
