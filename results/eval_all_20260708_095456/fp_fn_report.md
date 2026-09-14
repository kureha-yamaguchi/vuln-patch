# False Positive / False Negative Report — Harness-Based Overfitting Detection Run

**Scope:** 50 patches from the ASSERT-KTH dataset (`records.jsonl` + `run.log`), Defects4J subjects Lang / Math / Chart, crashing-bug kind only. Prediction rule as run: a patch is flagged **overfitting** if at least one of the (up to 5) generated Jazzer harnesses crashes on the patched code (`crashed_on_patch = true`).

## Headline numbers

Of the 50 patches, 49 were evaluated; one (Lang-43 / CapGen, item 47) produced no harnesses after 70 attempts and fell through as "no crash." Among the 49 evaluated: 31 labelled overfitting, 18 labelled correct. The run produced 27 true positives, 4 false negatives, 10 true negatives, and 8 false positives — precision 0.77, recall 0.87, accuracy 75.5%.

Sensitivity of the decision rule to the crash-count threshold (computed from the records):

| Rule (≥ N of 5 harnesses crash) | TP | FP | TN | FN | Precision | Recall |
|---|---|---|---|---|---|---|
| ≥ 1 (as run) | 27 | 8 | 10 | 4 | 0.77 | 0.87 |
| ≥ 2 | 26 | 6 | 12 | 5 | 0.81 | 0.84 |
| ≥ 3 (majority) | 21 | 4 | 14 | 10 | 0.84 | 0.68 |
| ≥ 5 (unanimous) | 10 | 2 | 16 | 21 | 0.83 | 0.32 |

Moving to ≥ 2 removes exactly the two single-harness false positives (Lang-20/Arja and Math-61/ACS, both 1/5) at the cost of one true positive (Chart-13/Elixir, a genuine overfitting patch that only crashed 1/5). Majority voting is too blunt: several genuinely overfitting patches only crash 2/5 (Chart-13/Arja, Lang-39/Arja, Lang-20/Arja-overfit, Math-85 patch9).

---

## False negatives (4) — overfitting patches that never crashed

All four false negatives share a single mechanism: **the overfitting patch suppresses the crash without restoring correct semantics**, converting a loud exception into silent data corruption. A crash-only oracle is structurally blind to this. In every one of these four cases the log's campaign block shows `distinct crashes (buggy ver): 1` — all five winning harnesses keyed on the identical exception signature, so once the patch silenced that one exception, the entire harness set went dark simultaneously.

**Math-49 / Arja, patch3 (item 49, 0/5) and patch7 (item 19, 0/5).** The ground-truth crash is a `ConcurrentModificationException` raised via `MathRuntimeException.createConcurrentModificationException` from `OpenIntToDoubleHashMap`'s fail-fast iterator. Both patches gut `doRemove()`: patch3 simply deletes `++count;` and patch7 replaces it with `keys[index]=0; index=changeIndexSign(index);`. The `count` field is the modification counter the iterator checks — removing the increment means the fail-fast check *can never fire again*. Every harness's oracle was exactly that exception, so all ten runs (5 per patch) came back clean. The patch is arguably worse than the bug: iterators now silently traverse a map that was mutated under them instead of throwing. Detecting this requires an invariant oracle (e.g., remove a key mid-iteration and assert the iterator either throws or yields a consistent key set), not a crash oracle.

**Math-32 / Jaid (item 34, 0/5).** Ground truth is a `ClassCastException` in `PolygonsSet.computeGeometricalProperties` (`BoundaryAttribute` cast to `Boolean`). Jaid's patch wraps the cast in `if ((tree == tree.getCut()) != false && (Boolean) tree.getAttribute())`. The added conjunct compares the tree to its own cut sub-hyperplane and is effectively always false, so the guard short-circuits: the CCE branch becomes unreachable — and so does the *intended* behaviour of that branch (marking the set as covering the whole plane with infinite size and NaN barycenter). The result is silently wrong geometry for whole-space polygon sets. All five harnesses shared the single CCE signature and all went clean on the patch.

**Chart-5 / DeepRepair (item 41, 0/5).** Ground truth is `IndexOutOfBoundsException: Index: -1` in `XYSeries.addOrUpdate`. The patch replaces the sorted insertion `this.data.add(-index - 1, new XYDataItem(x, y))` in the `autoSort` branch with an unconditional append `data.add(new XYDataItem(x, y))`. No more negative index, no more IOOBE — but the series' sortedness invariant under `autoSort = true` is silently broken; subsequent binary searches and rendering operate on unsorted data. All five harnesses keyed the IOOBE; none asserted post-condition sortedness.

**Why the metamorphic safety net didn't catch these.** The generation prompt explicitly requests a metamorphic check precisely for "overfitting patches that don't crash — they return a wrong value." But the campaign's stopping criterion is 5 *wins*, where a win means reproducing a crash on the buggy version — and the cheapest win is always re-triggering the same ground-truth exception. The campaign therefore converged on five near-clones of the same crash reproducer (distinct crashes = 1) rather than diversifying into invariant checks. Contrast this with Lang-16 (item 46), where the campaign achieved 4 distinct buggy-version crash signatures including two assertion-helper oracles — that diversity is what these four FN campaigns lacked.

---

## False positives (8) — correct patches that crashed

The eight FPs decompose into three mechanisms. A key observation sharpening the analysis: for at least three of the eight (Chart-9/SequenceR, Lang-27/SimFix, Math-85/Elixir), the "correct" patch is **textually identical or equivalent to the Defects4J developer fix**. Any harness that crashes on those patches would, by construction, also crash on the ground-truth fixed version — meaning the harness itself is invalid, and a developer-fix differential check would have filtered it before evaluation.

### Mechanism A — root-cause signature collides with legitimate rejection

**Chart-9 / SequenceR (item 22, 2/5 crashed + 1 timeout).** The patch `endIndex < 0` → `endIndex < startIndex` *is* the developer fix. The ground-truth crash is `IllegalArgumentException: "Requires start <= end."` at `TimeSeries.createCopy:883` — but that very same exception, at that very same line, is the *documented, correct* rejection when a caller passes an inverted period range. The generation prompt tells the harness to swallow documented rejections and propagate root-cause failures, but here the two are indistinguishable by class, message, and location. Any fuzzed input where `start.compareTo(end) > 0` crashes the fixed code identically. Two of five harnesses fell into this trap; a third timed out on the patched run (counted as clean).

**Math-58 / Arja (item 32, 2/5 crashed + 2 timeouts).** Ground truth is `NotStrictlyPositiveException` from `Gaussian$Parametric.validateParameters` — again a validation exception doubling as the bug signal. The buggy `fit()` passed a bad initial guess into the parametric Gaussian; Arja's patch routes through the one-argument `fit(guess)` overload instead, avoiding the bad construction. But a fuzzer feeding arbitrary observation points can produce degenerate datasets (e.g., non-positive sigma estimates) for which even a fully correct fitter legitimately raises the same NSPE. Two harnesses let it propagate. Side note: the recorded buggy-version crash signature for all five harnesses is the literal string `INFO@...validateParameters` — the crash-triage parser has captured a logging-level token where the throwable class should be. That parser bug is worth fixing independently; mis-parsed signatures will silently poison any future signature-matching logic.

**Lang-44 / Nopol2015 (item 39, 5/5 crashed).** Ground truth is `StringIndexOutOfBoundsException` in `NumberUtils.createNumber` for single-character type-suffix inputs like `"l"`. Nopol's guard `if (val.length() != 1)` around the L-case both passes the trigger test and (via switch fall-through to the default `throw NumberFormatException`) cleanly rejects length-1 inputs, which is presumably why the dataset labels it correct. Yet all five harnesses crashed on it. The harness sources in the log show they pump raw fuzzed ASCII through `createNumber` while catching only `NumberFormatException` (and sometimes NPE). `createNumber` in this Lang 2.x vintage contains *multiple* latent indexing defects beyond the one under test (the `indexOf('e') + indexOf('E') + 1` exponent-position arithmetic being the notorious one, later fixed as Lang-27). An SIOOBE from a sibling latent bug escapes the catch blocks and fires on the patched version — and would fire on the developer-fixed version too. The 5/5 uniformity across five structurally different harnesses strongly suggests a shared latent-sibling crash rather than a genuine semantic gap in the patch. Caveat: the log records only `exit 77` for patched-run crashes, with no stack trace, so this cannot be confirmed post hoc from the log alone — see recommendations.

**Lang-27 / SimFix (item 43, 4/5 crashed).** SimFix's patch (`expPos > -1` → `expPos > -1 && expPos < str.length() - 1`) is textually the developer fix. Every crash on it is therefore a harness false alarm by construction. Three of the four crashing harnesses targeted SIOOBE@createNumber on the buggy version (latent-sibling escape, as with Lang-44 — this codebase's `createNumber` still contains other index hazards); the fourth (attempt_004) crashed the buggy version via its own `RuntimeException@FuzzHarness` metamorphic check, i.e., a mis-specified relation (Mechanism B).

**Lang-16 / SimFix (item 46, 5/5 crashed).** The patch is `str = str.toLowerCase()` at the top of `createNumber`, versus a bug about rejecting `"0Xfade"`-style capital-X hex. Three of the five winning harnesses crashed the buggy version through their own assertion helpers (`requireValidUppercaseHex`, `requireUppercaseHexConsistency`, a generic harness `RuntimeException`) — property checks about uppercase-hex behaviour that the lowercasing rewrite then also violates. This one deserves a manual look rather than being scored purely against the pipeline: blanket `toLowerCase()` genuinely changes observable behaviour (exception messages now contain the lowercased string; `String.toLowerCase()` is default-locale-sensitive, with the classic Turkish dotless-ı hazard for inputs containing `I`). The ASSERT-KTH "correct" label for this patch is itself debatable — this may be partial label noise rather than a pure pipeline failure, and the harnesses may in fact be catching a real behavioural divergence from the developer fix.

### Mechanism B — mis-specified metamorphic relations

**Lang-20 / Arja (item 2, 1/5 crashed).** Arja replaces the buggy `StringBuilder` capacity expression (which NPE'd on a leading null array element) with a flat `new StringBuilder(256)` — capacity only, output-identical, correct. Four harnesses that keyed the real NPE went clean on the patch; the single crasher, attempt_004, is the one whose *buggy-version* signature was already `RuntimeException@FuzzHarness.fuzzerTestOneInput` — its own metamorphic check. A relation that fires on a behaviour-identical patch is a relation a correct implementation legally violates (join semantics around null elements and separators are an easy place to get this wrong). A ≥ 2 threshold or a dev-fix validation pass eliminates this FP.

**Math-85 / Elixir (item 45, 4/5 crashed).** The patch (`fa * fb >= 0.0` → `fa * fb > 0.0` in `UnivariateRealSolverUtils.bracket`) is exactly the developer fix. Strikingly, *all five* winning harnesses detected the buggy version only through `RuntimeException@FuzzHarness` — none reproduced the native `MathException`/`ConvergenceException` chain directly. On the fixed code, four of five still fired. Bracketing legitimately fails (ConvergenceException) for fuzzed inputs where the function has no sign change in the expandable interval; the developer fix only rescues the boundary case `fa * fb == 0`. Harnesses that wrap or assert around every ConvergenceException treat correct rejections as root-cause failures. This is the purest oracle failure in the run: a correct-by-definition patch, 80% crash rate.

### Mechanism C — exception-identity brittleness

**Math-61 / ACS (item 44, 1/5 crashed).** ACS's patch throws `new NotStrictlyPositiveException(null)` in place of `MathRuntimeException.createIllegalArgumentException(...)` for a non-positive Poisson mean. Semantically this remains an `IllegalArgumentException`-family rejection of invalid input (and matches the direction of the eventual developer fix), but the concrete class, the anonymous-class identity (`MathRuntimeException$4`), and the message (`null`) all change. Four harnesses recognized the new exception as a clean rejection; one evidently matched on the original exception identity or message, failed to recognize the replacement, and let it propagate. Rejection recognition should match on supertype family and throw-site, never on concrete class identity or message text.

---

## Coverage gap (not an FP/FN, but scores as one)

**Lang-43 / CapGen (item 47, status `no_harnesses`).** 70 generation attempts, 0 wins. The ground-truth failure is `OutOfMemoryError: Requested array size exceeds VM limit` from an unbounded append loop in `ExtendedMessageFormat.appendQuotedString` — a resource-exhaustion bug, not a thrown-exception bug. Jazzer wins are hard to score for OOM/hang failures under default memory and time limits, so the campaign never converged, and the record falls out as `crashed_on_patch: false`. In this run the patch happened to be labelled correct, so no error was charged — but an *overfitting* patch for the same bug would sail through as a silent false negative. The sibling record (item 13, Lang-43 / SimFix, `converged: false`, 1 harness) shows the same bug straining the pipeline. Resource-exhaustion ground truths need a dedicated oracle (`-rss_limit_mb`, wall-clock timeout treated as a win on the buggy version) or an explicit `unevaluable` verdict rather than defaulting into the "no crash" bucket.

---

## Recommendations, in order of expected leverage

**Add a developer-fix differential filter at harness-selection time.** Before accepting a winning harness, run it against the Defects4J developer-fixed version. Any harness that crashes there is invalid regardless of why (bad metamorphic relation, latent sibling bug, legit-rejection misclassification) and should be discarded or regenerated. For the three FPs whose patch equals the dev fix (Chart-9, Lang-27, Math-85) this filter provably removes every false alarm, and it would very likely also catch the latent-sibling crashers in Lang-44. This one change plausibly eliminates 5–7 of the 8 FPs.

**Log the patched-run crash signature and stack trace.** The log currently records only "CRASH FOUND (exit 77)" for patched runs, which made same-root-cause vs. different-crash impossible to distinguish in this analysis. Capturing the Jazzer dedup token and top frames enables an automatic rule: patched-crash signature ≠ buggy-crash signature → flag for review instead of auto-counting as overfitting evidence.

**Enforce oracle diversity before declaring convergence.** All four FNs had `distinct crashes (buggy ver): 1`. Require at least one win whose buggy-version signal is an invariant/metamorphic violation (a harness-raised assertion) rather than the ground-truth exception, specifically to survive crash-suppressing patches. Where the ground-truth exception is itself a validation exception (Chart-9, Math-58), require a second, non-signature signal before counting a patched-run crash.

**Validate metamorphic relations on both the buggy and dev-fixed versions.** A relation that fires on the developer fix is wrong by definition (Lang-20 attempt_004, all of Math-85). This is the same differential run as the first recommendation, applied per-harness.

**Match rejections by exception family and throw-site, not identity.** Fixes the Math-61 class of FP: `instanceof IllegalArgumentException` at the guard site should count as clean rejection even when the concrete subclass or message changes.

**Consider a ≥ 2-of-5 decision threshold.** On this run it trades one TP for two FPs (precision 0.77 → 0.81, recall 0.87 → 0.84) and specifically neutralizes single-rogue-harness noise. With the differential filter in place, ≥ 1 likely becomes safe again.

**Fix the crash-triage parser.** The `INFO@...` signature in Math-58 shows a log line being captured as a throwable class name.

**Manually review the Lang-16 / SimFix label.** The `toLowerCase()` patch changes observable behaviour (messages, locale sensitivity); the 5/5 crash rate may be the pipeline being right and the label being generous.

---

## Appendix — full case index

False positives (label = correct, ≥ 1 patched crash): item 2 Lang-20/Arja (1/5, metamorphic-relation artifact); item 22 Chart-9/SequenceR (2/5 + timeout, signature collision, patch = dev fix); item 32 Math-58/Arja (2/5 + 2 timeouts, validation-exception ambiguity, triage-parser bug); item 39 Lang-44/Nopol2015 (5/5, latent sibling SIOOBE suspected); item 43 Lang-27/SimFix (4/5, patch = dev fix, sibling SIOOBE + one bad metamorphic); item 44 Math-61/ACS (1/5, exception-identity brittleness); item 45 Math-85/Elixir (4/5, patch = dev fix, over-broad ConvergenceException oracle); item 46 Lang-16/SimFix (5/5, uppercase-hex property checks vs. debatable label).

False negatives (label = overfitting, 0/5 patched crashes): item 19 Math-49/Arja patch7 and item 49 Math-49/Arja patch3 (modCount increment removed, CME oracle silenced); item 34 Math-32/Jaid (vacuous guard makes crash branch and intended behaviour unreachable); item 41 Chart-5/DeepRepair (sorted insert replaced by append, sortedness invariant silently broken).

Coverage gap: item 47 Lang-43/CapGen (no harnesses after 70 attempts; OOM-class ground truth).