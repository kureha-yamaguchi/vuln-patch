# Early semantic-bug analysis (2026-06-29 → 07-04) — FROZEN

Moved from `src/java/` to `docs/` on 2026-07-31 (documentation does not belong in a
source package). Two companion documents of the earliest era, merged here: the
critique/open-questions notes and the per-run readthrough they reference.

**This is the OLDEST layer of analysis in the project** — it predates
`docs/progress.md` (from 07-15) and `docs/plan-history.md` PART 0 (from 07-16), so
nothing else in docs/ covers this period. It is kept for that reason alone.

**Read as history, not as a description of the pipeline.** Several claims were true
in early July and are false now, e.g.:
- "Differential fuzzing … not implemented — there is no differential code in the
  tree." The second half is outdated, the first half still stands, and the two must
  not be conflated (see the terminology note in
  `docs/crashing-bug-exposure-2026-07-31.md` §0). Differential *fuzzing* — fuzz both
  builds independently and diff their outputs — is still not implemented for either
  bug kind. Differential *replay* — re-run one exact firing input on the buggy build
  — very much is, and is not kind-gated (`run.py:2456`).
- The FP=6/10 and F1=0.55 figures describe a pipeline with no relation synthesis,
  no screening, no judge, and no computed evidence facts. Current state:
  `docs/plan.md` (CURRENT STATE section).
- The two-mechanism framing (metamorphic block vs lifted assertions) is still
  broadly right, but a third engine — synthesized+screened relations replayed on
  the patched build — was added on 07-15 and became the largest recall mechanism.

Where its questions ended up: the "model invents oracles in the GENERALISE step"
critique is the ancestor of what the campaign later called invented-contract false
accusations, three of which remain open residuals (`docs/plan.md`, CURRENT STATE).
The "accept gate filters for firing, not firing for the right reason" critique is
what the buggy-build screening stage and the fire-rate facts now address.

=============================================================================
PART 1 — CRITIQUE & OPEN QUESTIONS (formerly src/java/critique.md)
=============================================================================

# Critique & open questions — semantic (non-crashing) bug handling

Running notes so we don't re-derive these. Focus: the non-crashing oracle.
Last reviewed: 2026-06-29.

## Status snapshot

Two *different* mechanisms exist for non-crashing bugs (the PDF blurs them):

1. **Metamorphic check** — `prompts.py::_metamorphic_block`. Injected into the
   crashing prompt too. Asserts a relation between two real calls that holds for
   any correct impl (round-trip, idempotence, equivalent-inputs, construct-from-
   answer). Mature; lifted crashing-bug F1 ~0.73 -> 0.8 (PDF Table 7).
2. **Lifted-assertion path** — `prompts.py::_build_semantic` /
   `_lifted_assertion_block`, classified by `failure_test.py::is_crashing_bug`,
   routed in `run.py`. Lifts the expected value out of one trigger test's
   `assertEquals`, reconstructs the call, throws on mismatch. NEW and rough:
   F1=0.55, **FP=6/10** (PDF Table 8). This is a first run, not tuned.

Differential fuzzing is named in the PDF ("changes after attempt 4") but **not
implemented** — there is no differential code in the tree.

## Critiques (why FP=6 happens)

1. **The model invents oracles in the GENERALISE step.** A guessed assertion
   (e.g. assuming case-insensitivity that doesn't hold) fires on CORRECT patches
   too -> false positive. We pushed the oracle problem into the prompt and hoped
   the model stays disciplined; FP=6 says it doesn't.
2. **The accept gate filters for *firing*, not firing *for the right reason*.**
   `campaign.py` accepts a harness if it crashes the buggy version. A harness with
   a bad invented assertion fires on buggy AND on correct code; the gate never
   tests correct code, so it can't tell a good harness from a false-positive
   machine.
3. **Classifier is Defects4J-only + mishandles mixed bugs.** `is_crashing_bug`
   reads `defects4j info` (absent for the Project Zero CVE dataset) and returns
   "crashing" if ANY trigger throws, so a bug with both a crash and a separate
   wrong-output regression never gets the semantic check.
4. **evaluate.sh now silently blends two F1s.** Semantic bugs used to `exit 3`
   (skipped); they now run through as `status=evaluated`. The jq aggregation
   (evaluate.sh ~L105) lumps crashing (0.8) and semantic (0.55) into one
   uninterpretable number. `bug_kind` is already in every record — split the
   aggregation by it. DO BEFORE the next eval.
5. **One lifted test per harness wastes signal.** Round-robin gives each harness
   a single known input/output pair; we never combine several trusted pairs into
   one cross-checking harness.

## Idea assessments (after pushback 2026-06-29)

- **A. Differential (buggy vs patched-under-test): NOT a correctness oracle.**
  Buggy is only known-wrong on the trigger input(s); on other inputs it may be
  correct, so "patched == buggy" is uninformative. Buggy-vs-patched only reveals
  WHERE the patch changed behaviour (coverage signal), not whether the change is
  right. Turning divergence into a verdict still needs a trusted reference we
  don't have. Drop as an oracle; keep only as a reachability signal.
- **B. Gate against the developer fix: CHEATING.** Leaks the answer; unavailable
  in the real world. Only legitimate as a research-time check on the *generator*
  ("does it ever fire on a known-good version?"), never as part of the verdict.
- **C. Validating metamorphic relations (the honest version of B):** a relation
  is a universal truth, so validate it against behaviour we already trust, NOT
  against the fix:
    (i)  the project's existing PASSING test suite (known-correct behaviour we
         already have on the buggy checkout);
    (ii) non-triggering fuzz inputs on the buggy code (buggy is correct
         everywhere except the root-cause path, so a sound relation holds on
         almost all random inputs even on buggy);
    (iii) a second LLM as critic ("true for every correct impl? counterexample?").
  Discard any relation that breaks on clearly-fine inputs.
- **D. Construct-from-answer: sound but NARROW.** Zero-FP (you start from the
  answer) but only works for invertible functions (parse<->format, encode<->
  decode). Does not generalise (e.g. Math 85 bracketing has no inverse). Real
  for its slice; not a general fix.

## fuzz-introspector finding (2026-06-29)

Installed on a Python **3.11** venv (lxml 4.9.1 pin won't build on 3.12+;
no new VM needed — `uv venv --clear --python 3.11` + `uv sync --extra
introspector`, plus apt `clang libxml2-dev libxslt1-dev`). Initial symptom: `reachable` / `root_cause_reachable` came back EMPTY, so the
variant-analysis coverage-map steering was silently OFF.

ROOT CAUSE (fixed 2026-07-02): NOT a fuzz-introspector limitation — two
API-drift bugs in analysis.py:
  1. `project.find_function_by_name(name, True)` returns None in the current
     JVM frontend (it wants a mangled name, not a bare method name), so the
     whole enrichment loop `continue`d — losing xrefs AND reachable. (The
     callee context in prompts came from our AST resolver, not introspector.)
  2. `_reachable_of` probed `get_reached_functions_by_name`; the real API is
     `project.get_reachable_functions(function=<mangled>)`.
FIX: resolve touched functions to introspector's mangled name
(`[pkg.Class].method(argtypes)`) ourselves via `all_functions`, disambiguate
overloads by arg count, call `get_reachable_functions`, and filter to project
functions by RECEIVER bracket (not whole-name substring — the frontend
mis-types some args with the project package, which leaks JDK statics).
VERIFIED: Math-104 now yields root_cause_reachable = {regularizedGammaQ,
logGamma, evaluate, getA, getB, <init>} (was []).
Caveats: introspector adds ~30s/run (tree-sitter parse); the sembatch
semantic numbers below were collected BEFORE this fix (steering off), so a
re-run should be done to measure the fix's effect on recall.

## Reachable-set: perf regression + bounded-BFS fix (2026-07-03)

The first cut of the fix called `get_reachable_functions(function=...)`, which
does an UNBOUNDED transitive call-graph walk. Fine for a leaf like Math-104's
`regularizedGammaP` (~20 reachable, instant), but on a HUB like Math-2's
`inverseCumulativeProbability` it ran for 10+ min at 99% CPU. Replaced with a
budget-bounded BFS over immediate call-sites (`base_callsites`):
- Direct callees always included (BFS visits them first), then expand
  breadth-first until REACHABLE_NODE_CAP nodes / REACHABLE_MAX_DEPTH levels.
- O(cap) regardless of call-graph size -> cannot hang.
- CLI-overridable: `--reachable_node_cap`, `--reachable_max_depth`.
Validated Math-104: depth 1 -> {regularizedGammaQ, logGamma, <init>};
depth 3 -> + {evaluate, getA, getB}. ~35s (all parse).

**Why shallow is enough (evidence, not a guess).** Measured where the bug is
observable relative to the patched function across tasks:
- DOWNSTREAM manifest-sites / siblings all sit at **depth 1**: Math-2's
  `getNumericalMean` (direct callee, line 125 of icp), Math-104's
  `regularizedGammaQ`. No downstream case at depth >=3 in the batch.
- Most bugs are actually observed **UPSTREAM** at the public entry point
  (Math-30: `mannWhitneyUTest` is 1 level up from the patched helper; Time-19:
  `DateTime.toString` is ~3-4 levels above `getOffsetFromLocal`) — a DIFFERENT
  axis the callee-BFS doesn't cover, handled separately by the entry-point hint.
So downstream depth 1-2 suffices; direction (upstream vs downstream) matters
more than depth. The node cap is what guarantees no hang.

**Virtual-call gap + fix (2026-07-04).** Diagnosed why Math-2's reachable set
was `{<init>, log}` (missing `getNumericalMean`, the masked-symptom sibling):
introspector's JVM `base_callsites` records only STATICALLY-resolvable calls
(constructors, static methods). Virtual/abstract dispatch is invisible — and
`getNumericalMean` is abstract in `AbstractIntegerDistribution`, resolved to
`HypergeometricDistribution` at runtime, so it's dropped. FIX: union the static
BFS with SOURCE-extracted callees — parse the touched function's body with
javalang, keep UNQUALIFIED invocations (calls on `this`, i.e. the
inherited/abstract virtual calls), and resolve them to project methods via the
fi index (drops JDK; qualifier filter avoids name-collisions like
`Math.log`->`FastMath.log`). Validated: recovers `getNumericalMean`,
`getNumericalVariance`, `getSupport{Lower,Upper}Bound` for Math-2; Math-104
unchanged. NOTE still not SUFFICIENT for Math-2 FN->TP: the model doesn't probe
a sibling unprompted (needs a "call each reachable sibling, assert a sound
invariant" prompt), and Math-2's parse itself stalls (see below).

**Introspector PARSE stall — ROOT-CAUSED + FIXED (2026-07-04).** Not I/O; a
faulthandler stack dump showed the parse stuck in the JVM frontend's
`generate_report`, in two O(N^2) per-method metrics: `calculate_method_depth`
(an unbounded DFS whose `visited` is a LIST with O(n) membership checks) and
`calculate_method_uses` (an O(N) per-call scan). Called once per method over
~4500 methods on Math-2's commons-math3 snapshot -> minutes; Math-104's smaller
snapshot was fine (15s). We consume neither metric (only base_callsites /
all_functions / reachability, which `all_functions` sets independently). FIX
(monkeypatch on JvmProject, guarded): `calculate_method_depth` -> a
depth-BOUNDED recursion returning min(true_depth, cap) with a set-based on-path
guard + cached method map (cap = INTROSPECTOR_METHOD_DEPTH_CAP, default 3, also
`--introspector_depth_cap`); `calculate_method_uses` -> an O(1) cached
reverse-index (exact count). RESULT: Math-2 parses in **45s** (was hanging), and
its reachable set now includes `getNumericalMean` + the other virtual siblings.
The SIGALRM parse cap (INTROSPECTOR_TIMEOUT_SECONDS) stays as a backstop. Still
open: memoize the parse per bug id (it re-parses every run).

## Deep-dive method + worked example (Math-104, semantic TP)

To analyse any case pull: the drr patch, the trigger test (expected value),
the accepted harness `/tmp/d4j/<P>_<b>_buggy/fuzz/attempt_*/FuzzHarness.java`,
and the verdict. Ask: which oracle fired, is it sound, would it FP on correct
code, what info would have made it robust.

Math-104: patch flips the series convergence test
`Math.abs(an) > epsilon` -> `Math.sqrt(an) > epsilon` in
`Gamma.regularizedGammaP`. Harness used THREE oracles: (a) lifted seed
assertion `regularizedGammaP(1.0,1.0)==0.632…` (sound, one point), (b) bare
exploration calls, (c) a genuinely SOUND metamorphic relation — CDF
monotonicity: `regularizedGammaP(a,x)` non-decreasing in x. Correctly
FLAGGED (TP).
- **Latent FP risk (the lesson):** the monotonicity relation is true for the
  IDEAL function but the implementation is an *iterative approximation*
  parameterised by epsilon/maxIterations. The harness lets maxIter be as low
  as 1 and epsilon as large as 1e-1, i.e. the NON-converged regime, where even
  CORRECT code can violate monotonicity numerically at tol 1e-10 -> false
  positive. This is a concrete source of the semantic FP rate.
- **What info is lacking / would solve it:** the harness doesn't "know" the
  patched function is a convergence loop, so it asserts math relations outside
  the converged regime. The PATCH ITSELF touches the convergence condition —
  a smarter prompt could say "the patched line is the convergence test; only
  assert mathematical relations once converged (small epsilon, large
  maxIter)." General rule: **metamorphic relations on iterative/approximate
  code must be guarded to the converged regime**, else they FP on correct code.

## The fundamental point

For arbitrary non-crashing bugs there is **no complete sound oracle without a
spec or trusted reference** — the oracle problem, unsolvable in general. So:

- **Soundness (no FP) is achievable** — never trust an unvalidated oracle.
- **Completeness (no FN) is not** for semantic bugs without a spec.

Right design: a TOOLBOX of partial sound oracles (lifted-seed assertion +
*validated* metamorphic + construct-from-answer + type/contract invariants),
each covering a slice, with recall honestly capped at "fraction of bugs that
fall into some slice." Report semantic results as sound-oracle coverage + a
recall ceiling, not as an F1 expected to reach 1.0.


=============================================================================
PART 2 — PER-RUN READTHROUGH (formerly src/java/semantic-runs-analysis.md)
=============================================================================

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

# Post-fix batch (sem3, 2026-07-03) — steering ON, deduped

First batch after the reachable-set + bounded-BFS fixes. Deduped to 16 DISTINCT
bugs (vs the old batch's 4× repeats). NOTE: different bugs were sampled than
batch 2, so this is NOT a clean A/B — mechanism confirmation, not a controlled
delta.

| Kind | Post-fix (sem3) | Pre-fix (sem) |
|---|---|---|
| crashing | TP=4 FN=1 FP=0 TN=3 → **P=1.0 R=0.8 F1=0.89** | — |
| semantic | TP=2 FN=0 FP=**2** TN=4 → **P=0.5 R=1.0 F1=0.67** | F1≈0.73 |

**Two semantic FPs — both invented, unsound metamorphic relations (recurring):**
- **Math-5** (`Complex.reciprocal`, correct patch `NaN→INF` for `reciprocal(0)`):
  the lifted assertion passes; the invented `z * z.reciprocal() == 1` FIRES —
  false at `z=0` (`0·INF=NaN`) and under overflow.
- **Math-50** (`BaseSecantSolver`/Regula Falsi, correct): invented `more evals
  must not change the root` / `scaling must not change whether
  TooManyEvaluationsException is thrown` — both false for an iterative solver.

Crashing is clean (FP=0). Semantic precision is the binding constraint, entirely
the unsound-invented-relation pattern — orthogonal to steering (steering helps
recall, not precision). Also **Time-4** (overfit) → FN with `bug_kind=None`: the
classifier had no exception metadata and fell through (a classification gap).

**Math-2 steering prediction — TESTED (pinned run), FAILED.** Prediction was that
steering would surface `getNumericalMean` and flip Math-2 FN→TP. It stayed FN,
and NONE of the harnesses called `getNumericalMean`. Two causes: (i) the reachable
set came back sparse (`{<init>, log}`) — `getNumericalMean` was NOT captured,
even though it's a direct callee of the touched `inverseCumulativeProbability`
(a `base_callsites` gap to investigate); (ii) even seeing `getNumericalMean` in
the function source, the model does not spontaneously probe a sibling with a
sound invariant — it stays on the trigger method. So the reachable-set fix is
NECESSARY BUT NOT SUFFICIENT for masked-symptom FNs; needs (a) the sibling
actually in the reachable set and (b) explicit "probe each reachable sibling
with a sound invariant" prompting.

**FP fix — the legitimate (non-cheating) path.** A dev-fix soundness gate was
prototyped and then REMOVED: validating an invented relation by running the
harness against the Defects4J developer fix hinges on info unavailable in
deployment (cheating). The deployable version validates the relation against
**known-correct behaviour we legitimately have**: the project's PASSING test
suite (those tests pass on the buggy checkout → known-correct inputs), and/or
NON-triggering fuzz inputs on the buggy checkout (buggy is correct everywhere
except the root-cause path). Discard any relation that fires there before it can
flag a patch. That would kill both sem3 FPs (`z·1/z==1` breaks on passing-test
inputs too) without ever touching the developer fix.

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
