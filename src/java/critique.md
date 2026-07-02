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
introspector`, plus apt `clang libxml2-dev libxslt1-dev`). Result on Java
Defects4J bugs:
- `analyse_end_to_end(module_only=True)` runs WITHOUT error and DOES enrich
  callee context (related_callees with real impls), BUT
- `reachable` / `root_cause_reachable` come back **EMPTY** — the Java
  call-graph/reachability isn't computed in module_only mode. So the
  variant-analysis *coverage-map steering* is effectively OFF even with
  introspector installed.
- It also adds ~2x per-run latency (tree-sitter parse of the project).
Net: buys callee context, NOT coverage spread. Open Q: does full-analysis
mode (module_only=False) populate Java reachability? Until then, introspector
is optional for throughput batches.

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
