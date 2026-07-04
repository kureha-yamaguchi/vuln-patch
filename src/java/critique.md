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

**Introspector PARSE cost + stall.** `analyse_end_to_end` re-parses the WHOLE
library every run (~15-35s for math3), no caching, and the batch re-samples the
same bug. Memoize per bug id. Worse, Math-2's parse STALLS (0% CPU, blocked)
even on a FRESH checkout — a genuine frontend issue, not pollution. Mitigation
shipped: wrap the parse in a SIGALRM wall-clock cap
(INTROSPECTOR_TIMEOUT_SECONDS, default 120) so it degrades to no-steering
instead of hanging. Root-causing the stall (which source file trips the JVM
frontend) is still open.

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
