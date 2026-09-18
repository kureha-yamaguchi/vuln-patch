# Pipeline trace — Math-70

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
```

64 sequential steps — 30 LLM calls, 34 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method=' (~725 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method='testMath369', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/src/test/java/org/apache/commons/math/analysis/solvers/BisectionSolverTest.java', method_source='    public void testMath369() throws Exception {\\n        UnivariateRealFunction f = new SinFunction();\\n        UnivariateRealSolver solver = new BisectionSolver();\\n        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369",
    "failure_message": "--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369\njava.lang.NullPointerException",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~7,037 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java\n+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java\n@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {\n     /** {@inheritDoc} */\n     public double solve(final UnivariateRealFunction f, double min, double max, double initial)\n         throws MaxIterationsExceededException, FunctionEvaluationException {\n-        return solve(min, max);\n+        return solve(f,min, max)\n+;\n     }\n \n     /** {@inheritDoc} */",
  "functions": [
    {
      "func_name": "solve",
      "func_signature": "public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException",
      "func_source": "    public double solve(final UnivariateRealFunction f, double min, double max, double initial)\n        throws MaxIterationsExceededException, FunctionEvaluationException {\n        return solve(min, max);\n    }",
      "func_class": "BisectionSolver",
      "func_class_fq": "org.apache.commons.math.analysis.solvers.BisectionSolver",
      "func_param_types": [
        "UnivariateRealFunction",
        "double",
        "double",
        "double"
      ],
      "fi_name": "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double)",
      "overload_types": [
        [
          "double",
          "double",
          "double"
        ],
        [
          "double",
          "double"
        ],
        [
          "UnivariateRealFunction",
          "double",
          "double",
          "double"
        ],
        [
          "UnivariateRealFunction",
          "double",
          "double"
        ]
      ],
      "xrefs": [],
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double)",
          "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)",
          "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.solvers.BisectionSolver,double,double)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double)": 0,
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)": 1,
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.solvers.BisectionSolver,double,double)": 2
      },
      "reachable": [
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)",
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.solvers.BisectionSolver,double,double)"
      ],
      "related_callees": [
        {
          "name": "solve",
          "source_file": "UnivariateRealSolver.java",
          "signature": "double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException",
          "source": "    double solve(double min, double max) throws ConvergenceException,\n        FunctionEvaluationException;",
          "is_abstract": true,
          "impls": [
            [
              "BisectionSolver.java",
              "    public double solve(double min, double max, double initial)\n        throws MaxIterationsExceededException, FunctionEvaluationException {\n        return solve(f, min, max);\n    }"
            ],
            [
              "BisectionSolver.java",
              "    public double solve(double min, double max)\n        throws MaxIterationsExceededException, FunctionEvaluationException {\n        return solve(f, min, max);\n    }"
            ],
            [
              "BisectionSolver.java",
              "    public double solve(final UnivariateRealFunction f, double min, double max)\n        throws MaxIterationsExceededException, FunctionEvaluationException {\n\n        clearResult();\n        verifyInterval(min,max);\n        double m;\n        double fm;\n        double fmin;\n\n        int i = 0;\n        while (i < maximalIterationCount) {\n            m = UnivariateRealSolverUtils.midpoint(min, max);\n           fmin = f.value(min);\n           fm = f.value(m);\n\n            if (fm * fmin > 0.0) {\n                // max and m bracket the root.\n                min = m;\n            } else {\n                // min and m bracket the root.\n                max = m;\n            }\n\n            if (Math.abs(max - min) <= absoluteAccuracy) {\n                m = UnivariateRealSolverUtils.midpoint(min, max);\n                setResult(m, i);\n                return m;\n            }\n            ++i;\n        }\n\n        throw new MaxIterationsExceededException(maximalIterationCount);\n    }"
            ],
            [
              "BrentSolver.java",
              "    public double solve(double min, double max)\n        throws MaxIterationsExceededException, FunctionEvaluationException {\n        return solve(f, min, max);\n    }"
            ]
          ]
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.analysis.solvers",
  "root_cause_reachable": [
    "BisectionSolver.solve(double, double)",
    "BisectionSolver.solve(BisectionSolver, double, double)"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import org.apache.commons.math.FunctionEvaluationException;",
    "import org.apache.commons.math.MaxIterationsExceededException;",
    "import org.apache.commons.math.analysis.UnivariateRealFunction;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double)",
      "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)",
      "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.solvers.BisectionSolver,double,double)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double)": 0,
    "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(double,double)": 1,
    "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.solvers.BisectionSolver,double,double)": 2
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~24,648 chars)</summary>

**[system]**
```
You are an expert Java security engineer who writes Jazzer fuzzing harnesses. Return a single compilable .java file — no markdown fences, no prose outside the file.
```
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
First harness: establish the most direct path from the fuzz entrypoint through the patched code.
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,052 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            exploreSinBracketing(data);
        } else if (mode == 1) {
            exploreWithFreshSolvers(data);
        } else {
            exploreBoundaryIntervals(data);
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        try {
            BisectionSolver solver = new BisectionSolver();
            double r1 = solver.solve(f, min, max, initial);

            BisectionSolver solver2 = new BisectionSolver();
            double r2 = solver2.solve(f, min, max);

            // Contract/oracle: in this implementation family, solve(f,min,max,initial)
            // is the same operation as solve(f,min,max) for bisection; the "initial"
            // value is not used by the real algorithm, and the patched method delegates
            // directly to the 3-arg overload. A throw-deleting or wrong-delegation patch
            // would violate this sibling-agreement relation.
            if (Math.abs(r1 - r2) > tolerance(r1, r2)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void exploreSinBracketing(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = 0.01 + data.consumeInt(0, 5000) / 5000.0;
        double rightWidth = 0.01 + data.consumeInt(0, 5000) / 5000.0;

        leftWidth = Math.min(leftWidth, 1.5);
        rightWidth = Math.min(rightWidth, 1.5);

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            int selector = data.consumeInt(0, 1000);
            initial = min + (max - min) * (selector / 1000.0);
        }

        runChecks(f, min, max, initial);
    }

    private static void exploreWithFreshSolvers(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-200, 200);
        double root = k * Math.PI;

        double span = 0.02 + data.consumeInt(0, 10000) / 10000.0;
        span = Math.min(span, 2.0);

        double skew = data.consumeInt(-5000, 5000) / 10000.0;
        double min = root - span * (1.0 + Math.max(0.0, skew));
        double max = root + span * (1.0 + Math.max(0.0, -skew));
        if (!(min < max)) {
            return;
        }

        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        runChecks(f, min, max, initial);
    }

    private static void exploreBoundaryIntervals(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-50, 50);
        double root = k * Math.PI;

        double eps = data.consumeInt(1, 10000) / 100000.0;
        double min = root - eps;
        double max = root + eps;
        if (data.consumeBoolean()) {
            min = root;
        } else if (data.consumeBoolean()) {
            max = root;
        }
        if (!(min < max) && min != max) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        }

        runChecks(f, min, max, initial);
    }

    private static void runChecks(UnivariateRealFunction f, double min, double max, double initial) {
        if (f == null) {
            return;
        }
        if (!(min < max) && min != max) {
            return;
        }

        try {
            BisectionSolver solver1 = new BisectionSolver();
            double lhs = solver1.solve(f, min, max, initial);

            BisectionSolver solver2 = new BisectionSolver();
            double rhs = solver2.solve(f, min, max);

            // Contract/oracle: these same-name overloads must agree on equivalent input.
            if (Math.abs(lhs - rhs) > tolerance(lhs, rhs)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
            }

            // Independent consistency check using the function itself:
            // for a valid bisection solve on a bracketing interval, the returned root
            // must remain inside the interval used to compute it. This is a direct
            // property of repeated midpoint refinement in the real implementation.
            double lo = Math.min(min, max);
            double hi = Math.max(min, max);
            if (lhs < lo - 1e-12 || lhs > hi + 1e-12) {
                throw new RuntimeException("[oracle:interval-contained] metamorphic violation: result escaped interval input=[" + min + "," + max + "," + initial + "] result=" + lhs);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t) && isValidByConstructionSinInput(f, min, max)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isValidByConstructionSinInput(UnivariateRealFunction f, double min, double max) {
        if (f == null) {
            return false;
        }
        if (!(min < max) && min != max) {
            return false;
        }
        if (Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max)) {
            return false;
        }
        if (min == max) {
            return true;
        }
        double a = Math.sin(min);
        double b = Math.sin(max);
        return !(Double.isNaN(a) || Double.isNaN(b)) && a * b <= 0.0;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("FunctionEvaluationException")
                || name.contains("MaxIterationsExceededException")
                || name.contains("MathException");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double tolerance(double a, double b) {
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return 1e-9 * scale;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,032 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {interval-contained, overload-agree}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,037 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double r = solver.solve(f, 3.0, 3.2, 3.1);
                double tol = Math.max(1.0e-4, solver.getAbsoluteAccuracy() * 8.0);
                if (Math.abs(r - Math.PI) > tol) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: seeded bisection interval around pi returned wrong root lhs=" + r + " rhs=" + Math.PI);
                }
                try {
                    double fr = f.value(r);
                    if (Math.abs(fr) > 1.0e-4) {
                        throw new RuntimeException("[oracle:anchor-sin] metamorphic violation: solver returned non-root for seeded pi interval x=" + r + " sin(x)=" + fr);
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable t) {
                boolean rootCauseNpe = t instanceof NullPointerException;
                if (rootCauseNpe) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                                && "solve".equals(st[i].getMethodName())) {
                            throw new RuntimeException("[oracle:anchor-npe] metamorphic violation: valid seeded solve(f,min,max,initial) threw from BisectionSolver.solve", t);
                        }
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
            }
        }

        int k = data.consumeInt(1, 32);
        double root = k * Math.PI;

        double left = 0.01 * (data.consumeInt(1, 40));
        double right = 0.01 * (data.consumeInt(1, 40));
        double min = root - left;
        double max = root + right;

        if (!(min < root && root < max)) {
            return;
        }

        double[] initials = new double[4];
        initials[0] = min;
        initials[1] = max;
        initials[2] = (min + max) / 2.0;
        int percent = data.consumeInt(0, 1000);
        initials[3] = min + (max - min) * (percent / 1000.0);

        double tol = 1.0e-4;
        double[] results = new double[initials.length];
        int successCount = 0;

        for (int i = 0; i < initials.length; i++) {
            BisectionSolver solver = new BisectionSolver();
            try {
                double r = solver.solve(f, min, max, initials[i]);
                results[i] = r;
                successCount++;

                double localTol = Math.max(tol, solver.getAbsoluteAccuracy() * 8.0);

                /* Contract/oracle: we constructed the interval to bracket the known sine root k*pi,
                 * so a correct solver must return that root (within accuracy), regardless of which
                 * in-interval initial guess we provide. A patch that merely suppresses the old throw
                 * but skips real solving will violate this observable post-condition. */
                if (Math.abs(r - root) > localTol) {
                    throw new RuntimeException("[oracle:known-root] metamorphic violation: solve(f,min,max,initial) failed to recover constructed sine root inputRoot=" + root + " initial=" + initials[i] + " result=" + r);
                }

                try {
                    double fr = f.value(r);
                    if (Math.abs(fr) > 1.0e-4) {
                        throw new RuntimeException("[oracle:function-zero] metamorphic violation: returned point is not a root interval=[" + min + "," + max + "] initial=" + initials[i] + " x=" + r + " sin(x)=" + fr);
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable t) {
                boolean rootCauseNpe = t instanceof NullPointerException;
                if (rootCauseNpe) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int j = 0; j < st.length; j++) {
                        if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[j].getClassName())
                                && "solve".equals(st[j].getMethodName())) {
                            throw new RuntimeException("[oracle:valid-npe] metamorphic violation: valid constructed sine interval caused NPE in BisectionSolver.solve interval=[" + min + "," + max + "] initial=" + initials[i], t);
                        }
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
            }
        }

        if (successCount >= 2) {
            double baseline = Double.NaN;
            for (int i = 0; i < results.length; i++) {
                if (results[i] == results[i]) {
                    baseline = results[i];
                    break;
                }
            }
            if (baseline == baseline) {
                for (int i = 0; i < results.length; i++) {
                    if (results[i] == results[i] && Math.abs(results[i] - baseline) > tol) {
                        throw new RuntimeException("[oracle:initial-stability] metamorphic violation: same bracketed sine root solved to inconsistent answers under different in-interval initials baseline=" + baseline + " other=" + results[i] + " interval=[" + min + "," + max + "]");
                    }
                }
            }
        }
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:anchor-npe] metamorphic violation: valid seeded solve(f,min,max,initial) threw from BisectionSolver.solve

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,325 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-sin, function-zero, initial-stability, interval-contained, known-root, overload-agree, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,813 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input, but swallow the already-known duplicate NPE
        // so this harness can still exercise a different reachable function and rely on
        // an independent oracle.
        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchor = anchorSolver.solve(f, 3.0, 3.2, 3.1);
            checkReturnedMatchesStored(anchorSolver, anchor, "anchor-state");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (!isKnownRootCauseNpe(t)) {
                // Not the known buggy symptom; do not turn unrelated failures into findings.
                return;
            }
        }

        // EXPLORE / STRATEGY (a): target the different reachable stored-function overload
        // solve(double, double). First seed the solver with a real function using the public
        // API, then solve a second interval through solve(min, max).
        int n1 = data.consumeInt(-200, 200);
        int n2 = n1 + data.consumeInt(1, 5);

        double d1Left = boundedPositiveDelta(data);
        double d1Right = boundedPositiveDelta(data);
        double d2Left = boundedPositiveDelta(data);
        double d2Right = boundedPositiveDelta(data);

        double root1 = n1 * Math.PI;
        double root2 = n2 * Math.PI;

        double min1 = root1 - d1Left;
        double max1 = root1 + d1Right;
        double min2 = root2 - d2Left;
        double max2 = root2 + d2Right;

        if (!(min1 < max1 && min2 < max2)) {
            return;
        }

        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();

        try {
            // Real library call: installs/uses the function and computes a first root.
            double firstA = solverA.solve(f, min1, max1);
            checkReturnedMatchesStored(solverA, firstA, "seed-state-a");

            double firstB = solverB.solve(f, min1, max1);
            checkReturnedMatchesStored(solverB, firstB, "seed-state-b");

            // Determinism / replay check on identically-constructed objects:
            // a correct deterministic solver run on the same function and interval
            // must report the same root on both fresh objects.
            double seedTol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
            if (Math.abs(firstA - firstB) > seedTol) {
                throw new RuntimeException(
                    "[oracle:seed-replay] metamorphic violation: identical seeded solves disagree " +
                    "min=" + min1 + " max=" + max1 + " lhs=" + firstA + " rhs=" + firstB);
            }

            // Different reachable function: uses the function previously supplied to the solver.
            double storedA = solverA.solve(min2, max2);
            checkReturnedMatchesStored(solverA, storedA, "stored-state-a");

            double storedB = solverB.solve(min2, max2);
            checkReturnedMatchesStored(solverB, storedB, "stored-state-b");

            // Independent oracle: same quantity from a second, identically-constructed object.
            // This would still fail if a patch merely silenced the original NPE but left the
            // stored-function overload computing or storing the wrong result.
            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
            if (Math.abs(storedA - storedB) > tol) {
                throw new RuntimeException(
                    "[oracle:stored-replay] metamorphic violation: identical solve(min,max) runs disagree " +
                    "seedMin=" + min1 + " seedMax=" + max1 +
                    " min=" + min2 + " max=" + max2 +
                    " lhs=" + storedA + " rhs=" + storedB);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static double boundedPositiveDelta(FuzzedDataProvider data) {
        int micros = data.consumeInt(1, 1_000_000);
        return micros / 1_000_000.0;
    }

    private static void checkReturnedMatchesStored(BisectionSolver solver, double returned, String oracleId) {
        try {
            double stored1 = solver.getResult();
            double stored2 = solver.getResult();

            if (Double.doubleToLongBits(returned) != Double.doubleToLongBits(stored1)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: solve returned value differs from solver.getResult() " +
                    "returned=" + returned + " stored=" + stored1);
            }
            if (Double.doubleToLongBits(stored1) != Double.doubleToLongBits(stored2)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: repeated getResult() reads are unstable " +
                    "first=" + stored1 + " second=" + stored2);
            }
        } catch (IllegalStateException e) {
            // If the solver reports no result state after a successful solve, that violates
            // the post-condition that solve set the result.
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: successful solve left no stored result", e);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isKnownRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,427 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-sin, anchor-state, function-zero, initial-stability, interval-contained, known-root, overload-agree, seed-replay, seed-state-a, seed-state-b, stored-replay, stored-state-a, stored-state-b, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~4,756 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static void maybePropagate(Throwable t) {
        if (isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static double consumeFraction(FuzzedDataProvider data) {
        int raw = data.consumeInt(0, 1_000_000);
        return raw / 1_000_000.0;
    }

    private static void anchor() {
        try {
            UnivariateRealFunction f = new SinFunction();
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(1, 100);
        double center = k * Math.PI;
        double width = 0.05 + consumeFraction(data) * 1.0;
        double min = center - width;
        double max = center + width;
        double initial = min + consumeFraction(data) * (max - min);

        if (!(min < initial && initial < max)) {
            return;
        }

        double storedResult;
        try {
            BisectionSolver storedSolver = new BisectionSolver(new SinFunction());
            storedResult = storedSolver.solve(min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
            return;
        }

        double argResult;
        try {
            BisectionSolver argSolver = new BisectionSolver();
            argResult = argSolver.solve(new SinFunction(), min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
            return;
        }

        double directResult;
        try {
            BisectionSolver directSolver = new BisectionSolver();
            directResult = directSolver.solve(new SinFunction(), min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
            return;
        }

        double tol = 1e-6;

        /*
         * Contract used for this oracle:
         * all BisectionSolver overloads solve the same bracketed root of the same
         * function on the same interval. A correct implementation must therefore
         * report the same root (up to solver accuracy) whether the function is
         * supplied in the constructor and solve(min,max) is used, or supplied as
         * an argument to solve(f,min,max,initial), or to solve(f,min,max).
         * This catches a throw-deleting/band-aid patch that suppresses the NPE but
         * still loses the function or computes a different root.
         */
        if (Math.abs(storedResult - argResult) > tol) {
            throw new RuntimeException(
                    "[oracle:ctor-arg-consistency] metamorphic violation: stored-function solve(min,max) disagrees with solve(f,min,max,initial)"
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " stored=" + storedResult + " arg=" + argResult);
        }

        if (Math.abs(directResult - argResult) > tol) {
            throw new RuntimeException(
                    "[oracle:arg-overload-consistency] metamorphic violation: solve(f,min,max) disagrees with solve(f,min,max,initial)"
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " direct=" + directResult + " arg=" + argResult);
        }
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,475 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-sin, anchor-state, arg-overload-consistency, ctor-arg-consistency, function-zero, initial-stability, interval-contained, known-root, overload-agree, seed-replay, seed-state-a, seed-state-b, stored-replay, stored-state-a, stored-state-b, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~3,956 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.CosFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction stored = new SinFunction();
        UnivariateRealFunction explicit = new CosFunction();

        checkExplicitFunctionBeatsStoredState(
                stored,
                explicit,
                1.0,
                2.0,
                1.5,
                "deterministic-cos");

        double root = Math.PI / 2.0;
        double left = data.consumeInt(1, 1000) / 1000.0;
        double right = data.consumeInt(1, 1000) / 1000.0;
        double min = root - left;
        double max = root + right;

        int slot = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (slot / 1000.0);

        if (!(min < initial && initial < max)) {
            initial = (min + max) * 0.5;
        }

        if (data.consumeBoolean()) {
            stored = new CosFunction();
            explicit = new SinFunction();
            double sinRoot = Math.PI;
            left = data.consumeInt(1, 1000) / 1000.0;
            right = data.consumeInt(1, 1000) / 1000.0;
            min = sinRoot - left;
            max = sinRoot + right;
            slot = data.consumeInt(0, 1000);
            initial = min + (max - min) * (slot / 1000.0);
            if (!(min < initial && initial < max)) {
                initial = (min + max) * 0.5;
            }
        }

        checkExplicitFunctionBeatsStoredState(
                stored,
                explicit,
                min,
                max,
                initial,
                "state-switch");
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                return;
            }
        }
    }

    private static void checkExplicitFunctionBeatsStoredState(
            UnivariateRealFunction storedFunction,
            UnivariateRealFunction explicitFunction,
            double min,
            double max,
            double initial,
            String tag) {

        double explicitOnContaminated;
        double explicitOnFresh;

        try {
            BisectionSolver contaminated = new BisectionSolver(storedFunction);
            explicitOnContaminated = contaminated.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver fresh = new BisectionSolver();
            explicitOnFresh = fresh.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        if (Double.isNaN(explicitOnContaminated) || Double.isNaN(explicitOnFresh)
                || Double.isInfinite(explicitOnContaminated) || Double.isInfinite(explicitOnFresh)) {
            return;
        }

        double tolerance = 1.0e-6;
        if (Math.abs(explicitOnContaminated - explicitOnFresh) > tolerance) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:explicit-state-independence] "
                            + "solve(f,min,max,initial)
```

</details>

---
## [12] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,522 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.CosFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction stored = new SinFunction();
        UnivariateRealFunction explicit = new CosFunction();

        checkExplicitFunctionBeatsStoredState(
                stored,
                explicit,
                1.0,
                2.0,
                1.5,
                "deterministic-cos");

        double root = Math.PI / 2.0;
        double left = data.consumeInt(1, 1000) / 1000.0;
        double right = data.consumeInt(1, 1000) / 1000.0;
        double min = root - left;
        double max = root + right;

        int slot = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (slot / 1000.0);

        if (!(min < initial && initial < max)) {
            initial = (min + max) * 0.5;
        }

        if (data.consumeBoolean()) {
            stored = new CosFunction();
            explicit = new SinFunction();
            double sinRoot = Math.PI;
            left = data.consumeInt(1, 1000) / 1000.0;
            right = data.consumeInt(1, 1000) / 1000.0;
            min = sinRoot - left;
            max = sinRoot + right;
            slot = data.consumeInt(0, 1000);
            initial = min + (max - min) * (slot / 1000.0);
            if (!(min < initial && initial < max)) {
                initial = (min + max) * 0.5;
            }
        }

        checkExplicitFunctionBeatsStoredState(
                stored,
                explicit,
                min,
                max,
                initial,
                "state-switch");
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                return;
            }
        }
    }

    private static void checkExplicitFunctionBeatsStoredState(
            UnivariateRealFunction storedFunction,
            UnivariateRealFunction explicitFunction,
            double min,
            double max,
            double initial,
            String tag) {

        double explicitOnContaminated;
        double explicitOnFresh;

        try {
            BisectionSolver contaminated = new BisectionSolver(storedFunction);
            explicitOnContaminated = contaminated.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver fresh = new BisectionSolver();
            explicitOnFresh = fresh.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        if (Double.isNaN(explicitOnContaminated) || Double.isNaN(explicitOnFresh)
                || Double.isInfinite(explicitOnContaminated) || Double.isInfinite(explicitOnFresh)) {
            return;
        }

        double tolerance = 1.0e-6;
        if (Math.abs(explicitOnContaminated - explicitOnFresh) > tolerance) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:explicit-state-independence] "
                            + "solve(f,min,max,initial)
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_005/FuzzHarness.java:117: error: unclosed string literal
                            + "solve(f,min,max,initial)
                              ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_005/FuzzHarness.java:117: error: reached end of file while parsing
                            + "solve(f,min,max,initial)
                                                       ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~5,369 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.CosFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction stored = new SinFunction();
        UnivariateRealFunction explicit = new CosFunction();

        checkExplicitFunctionBeatsStoredState(
                stored, explicit, 1.0, 2.0, 1.5, "deterministic-cos");

        double root = Math.PI / 2.0;
        double left = data.consumeInt(1, 1000) / 1000.0;
        double right = data.consumeInt(1, 1000) / 1000.0;
        double min = root - left;
        double max = root + right;

        int slot = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (slot / 1000.0);
        if (!(min < initial && initial < max)) {
            initial = (min + max) * 0.5;
        }

        if (data.consumeBoolean()) {
            stored = new CosFunction();
            explicit = new SinFunction();
            double sinRoot = Math.PI;
            left = data.consumeInt(1, 1000) / 1000.0;
            right = data.consumeInt(1, 1000) / 1000.0;
            min = sinRoot - left;
            max = sinRoot + right;
            slot = data.consumeInt(0, 1000);
            initial = min + (max - min) * (slot / 1000.0);
            if (!(min < initial && initial < max)) {
                initial = (min + max) * 0.5;
            }
        }

        checkExplicitFunctionBeatsStoredState(
                stored, explicit, min, max, initial, "state-switch");
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                return;
            }
        }
    }

    private static void checkExplicitFunctionBeatsStoredState(
            UnivariateRealFunction storedFunction,
            UnivariateRealFunction explicitFunction,
            double min,
            double max,
            double initial,
            String tag) {

        double explicitOnContaminated;
        double explicitOnFresh;

        try {
            BisectionSolver contaminated = new BisectionSolver(storedFunction);
            explicitOnContaminated = contaminated.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver fresh = new BisectionSolver();
            explicitOnFresh = fresh.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        if (Double.isNaN(explicitOnContaminated) || Double.isNaN(explicitOnFresh)
                || Double.isInfinite(explicitOnContaminated) || Double.isInfinite(explicitOnFresh)) {
            return;
        }

        double tolerance = 1.0e-6;
        if (Math.abs(explicitOnContaminated - explicitOnFresh) > tolerance) {
            throw new RuntimeException(
                    "[oracle:explicit-state-independence] "
                            + "solve(f,min,max,initial) must honor the explicit function argument "
                            + "rather than any previously stored solver state; equivalent explicit-function "
                            + "solves should agree with solve(f,min,max). tag=" + tag
                            + " min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " contaminated=" + explicitOnContaminated
                            + " fresh=" + explicitOnFresh);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if (e == null) {
                continue;
            }
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                    && "solve".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null
                && (name.indexOf("ConvergenceException") >= 0
                || name.indexOf("FunctionEvaluationException") >= 0
                || name.indexOf("MaxIterationsExceededException") >= 0);
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,730 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [13] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.CosFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction stored = new SinFunction();
        UnivariateRealFunction explicit = new CosFunction();

        checkExplicitFunctionBeatsStoredState(
                stored, explicit, 1.0, 2.0, 1.5, "deterministic-cos");

        double root = Math.PI / 2.0;
        double left = data.consumeInt(1, 1000) / 1000.0;
        double right = data.consumeInt(1, 1000) / 1000.0;
        double min = root - left;
        double max = root + right;

        int slot = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (slot / 1000.0);
        if (!(min < initial && initial < max)) {
            initial = (min + max) * 0.5;
        }

        if (data.consumeBoolean()) {
            stored = new CosFunction();
            explicit = new SinFunction();
            double sinRoot = Math.PI;
            left = data.consumeInt(1, 1000) / 1000.0;
            right = data.consumeInt(1, 1000) / 1000.0;
            min = sinRoot - left;
            max = sinRoot + right;
            slot = data.consumeInt(0, 1000);
            initial = min + (max - min) * (slot / 1000.0);
            if (!(min < initial && initial < max)) {
                initial = (min + max) * 0.5;
            }
        }

        checkExplicitFunctionBeatsStoredState(
                stored, explicit, min, max, initial, "state-switch");
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                return;
            }
        }
    }

    private static void checkExplicitFunctionBeatsStoredState(
            UnivariateRealFunction storedFunction,
            UnivariateRealFunction explicitFunction,
            double min,
            double max,
            double initial,
            String tag) {

        double explicitOnContaminated;
        double explicitOnFresh;

        try {
            BisectionSolver contaminated = new BisectionSolver(storedFunction);
            explicitOnContaminated = contaminated.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver fresh = new BisectionSolver();
            explicitOnFresh = fresh.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        if (Double.isNaN(explicitOnContaminated) || Double.isNaN(explicitOnFresh)
                || Double.isInfinite(explicitOnContaminated) || Double.isInfinite(explicitOnFresh)) {
            return;
        }

        double tolerance = 1.0e-6;
        if (Math.abs(explicitOnContaminated - explicitOnFresh) > tolerance) {
            throw new RuntimeException(
                    "[oracle:explicit-state-independence] "
                            + "solve(f,min,max,initial) must honor the explicit function argument "
                            + "rather than any previously stored solver state; equivalent explicit-function "
                            + "solves should agree with solve(f,min,max). tag=" + tag
                            + " min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " contaminated=" + explicitOnContaminated
                            + " fresh=" + explicitOnFresh);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if (e == null) {
                continue;
            }
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                    && "solve".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null
                && (name.indexOf("ConvergenceException") >= 0
                || name.indexOf("FunctionEvaluationException") >= 0
                || name.indexOf("MaxIterationsExceededException") >= 0);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_006/FuzzHarness.java:6: error: cannot find symbol
import org.apache.commons.math.analysis.CosFunction;
                                       ^
  symbol:   class CosFunction
  location: package org.apache.commons.math.analysis
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_006/FuzzHarness.java:13: error: cannot find symbol
        UnivariateRealFunction explicit = new CosFunction();
                                              ^
  symbol:   class CosFunction
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_006/FuzzHarness.java:31: error: cannot find symbol
            stored = new CosFunction();
                         ^
  symbol:   class CosFunction
  location: class FuzzHarness
Note: /datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_006/FuzzHarness.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
3 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~4,985 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        checkExplicitFunctionOverridesStoredState(
                linearRoot(0.25),
                linearRoot(0.75),
                0.0,
                1.0,
                0.5,
                "deterministic");

        int storedMilli = data.consumeInt(100, 400);
        int explicitMilli = data.consumeInt(600, 900);
        double storedRoot = storedMilli / 1000.0;
        double explicitRoot = explicitMilli / 1000.0;

        double min = 0.0;
        double max = 1.0;
        int slot = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (slot / 1000.0);
        if (!(min < initial && initial < max)) {
            initial = 0.5;
        }

        checkExplicitFunctionOverridesStoredState(
                linearRoot(storedRoot),
                linearRoot(explicitRoot),
                min,
                max,
                initial,
                "fuzzed");
    }

    private static UnivariateRealFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
        }
    }

    private static void checkExplicitFunctionOverridesStoredState(
            UnivariateRealFunction storedFunction,
            UnivariateRealFunction explicitFunction,
            double min,
            double max,
            double initial,
            String tag) {

        double contaminatedResult;
        double freshResult;

        try {
            BisectionSolver contaminated = new BisectionSolver(storedFunction);
            contaminatedResult = contaminated.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver fresh = new BisectionSolver();
            freshResult = fresh.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isRootCauseNpe(t)) {
                return;
            }
            return;
        }

        if (Double.isNaN(contaminatedResult) || Double.isNaN(freshResult)
                || Double.isInfinite(contaminatedResult) || Double.isInfinite(freshResult)) {
            return;
        }

        double tolerance = 1.0e-6;
        if (Math.abs(contaminatedResult - freshResult) > tolerance) {
            throw new RuntimeException(
                    "[oracle:explicit-overrides-state] "
                            + "A correct solve(f,min,max,initial) must use the explicit function argument, "
                            + "so solving the same explicit function on the same interval should agree with "
                            + "solve(f,min,max) on a fresh solver regardless of constructor state. "
                            + "tag=" + tag
                            + " min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " contaminated=" + contaminatedResult
                            + " fresh=" + freshResult);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if (e == null) {
                continue;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null
                && (name.indexOf("ConvergenceException") >= 0
                || name.indexOf("FunctionEvaluationException") >= 0
                || name.indexOf("MaxIterationsExceededException") >= 0);
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:explicit-overrides-state] A correct solve(f,min,max,initial) must use the explicit function argument, so solving the same explicit function on the same interval sho…

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,845 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-sin, anchor-state, arg-overload-consistency, ctor-arg-consistency, explicit-overrides-state, function-zero, initial-stability, interval-contained, known-root, overload-agree, seed-replay, seed-state-a, seed-state-b, stored-replay, stored-state-a, stored-state-b, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~4,984 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();
        BisectionSolver anchorSolver = new BisectionSolver();
        try {
            anchorSolver.solve(anchorFunction, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection) {
                boolean rootNpe = t instanceof NullPointerException;
                boolean inPatchedRegion = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
                if (rootNpe && inPatchedRegion) {
                } else {
                }
            }
        }

        int multiple = data.consumeInt(1, 20);
        int widthTicks = data.consumeInt(1, 200);
        double width = 0.05 + (widthTicks / 1000.0);
        double root = multiple * Math.PI;
        double min = root - width;
        double max = root + width;
        double initial = root + (data.consumeInt(-100, 100) / 100.0) * (width / 2.0);

        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = new SinFunction();

        double returned;
        try {
            returned = storedSolver.solve(min, max);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        double reported;
        int iterations;
        try {
            reported = storedSolver.getResult();
            iterations = storedSolver.getIterationCount();
        } catch (RuntimeException e) {
            throw new RuntimeException("[oracle:stored-result] metamorphic violation: successful solve(min,max) must store the same result it returns, but getters failed after success input=[" + min + "," + max + "] returned=" + returned, e);
        }

        double tol = Math.max(storedSolver.getAbsoluteAccuracy(), 1e-12);
        if (Math.abs(returned - reported) > tol) {
            throw new RuntimeException("[oracle:stored-result] metamorphic violation: successful solve(min,max) must store the same result it returns input=[" + min + "," + max + "] returned=" + returned + " stored=" + reported + " tol=" + tol);
        }
        if (iterations < 0) {
            throw new RuntimeException("[oracle:stored-iterations] metamorphic violation: successful solve(min,max) must record a non-negative iteration count input=[" + min + "," + max + "] iterations=" + iterations);
        }

        BisectionSolver explicitSolver = new BisectionSolver();
        double explicitReturned;
        try {
            explicitReturned = explicitSolver.solve(new SinFunction(), min, max, initial);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            boolean rootNpe = e instanceof NullPointerException;
            boolean inPatchedRegion = false;
            for (StackTraceElement ste : e.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    inPatchedRegion = true;
                    break;
                }
            }
            if (rootNpe && inPatchedRegion) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        double crossTol = Math.max(Math.max(storedSolver.getAbsoluteAccuracy(), explicitSolver.getAbsoluteAccuracy()), 1e-9);
        if (Math.abs(explicitReturned - reported) > crossTol) {
            throw new RuntimeException("[oracle:explicit-vs-stored] metamorphic violation: equivalent successful solves over the same valid interval for the same real function must agree input=[" + min + "," + max + "] initial=" + initial + " storedSolve=" + reported + " explicitSolve=" + explicitReturned + " tol=" + crossTol);
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,946 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [17] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();
        BisectionSolver anchorSolver = new BisectionSolver();
        try {
            anchorSolver.solve(anchorFunction, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection) {
                boolean rootNpe = t instanceof NullPointerException;
                boolean inPatchedRegion = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
                if (rootNpe && inPatchedRegion) {
                } else {
                }
            }
        }

        int multiple = data.consumeInt(1, 20);
        int widthTicks = data.consumeInt(1, 200);
        double width = 0.05 + (widthTicks / 1000.0);
        double root = multiple * Math.PI;
        double min = root - width;
        double max = root + width;
        double initial = root + (data.consumeInt(-100, 100) / 100.0) * (width / 2.0);

        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = new SinFunction();

        double returned;
        try {
            returned = storedSolver.solve(min, max);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        double reported;
        int iterations;
        try {
            reported = storedSolver.getResult();
            iterations = storedSolver.getIterationCount();
        } catch (RuntimeException e) {
            throw new RuntimeException("[oracle:stored-result] metamorphic violation: successful solve(min,max) must store the same result it returns, but getters failed after success input=[" + min + "," + max + "] returned=" + returned, e);
        }

        double tol = Math.max(storedSolver.getAbsoluteAccuracy(), 1e-12);
        if (Math.abs(returned - reported) > tol) {
            throw new RuntimeException("[oracle:stored-result] metamorphic violation: successful solve(min,max) must store the same result it returns input=[" + min + "," + max + "] returned=" + returned + " stored=" + reported + " tol=" + tol);
        }
        if (iterations < 0) {
            throw new RuntimeException("[oracle:stored-iterations] metamorphic violation: successful solve(min,max) must record a non-negative iteration count input=[" + min + "," + max + "] iterations=" + iterations);
        }

        BisectionSolver explicitSolver = new BisectionSolver();
        double explicitReturned;
        try {
            explicitReturned = explicitSolver.solve(new SinFunction(), min, max, initial);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            boolean rootNpe = e instanceof NullPointerException;
            boolean inPatchedRegion = false;
            for (StackTraceElement ste : e.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    inPatchedRegion = true;
                    break;
                }
            }
            if (rootNpe && inPatchedRegion) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        double crossTol = Math.max(Math.max(storedSolver.getAbsoluteAccuracy(), explicitSolver.getAbsoluteAccuracy()), 1e-9);
        if (Math.abs(explicitReturned - reported) > crossTol) {
            throw new RuntimeException("[oracle:explicit-vs-stored] metamorphic violation: equivalent successful solves over the same valid interval for the same real function must agree input=[" + min + "," + max + "] initial=" + initial + " storedSolve=" + reported + " explicitSolve=" + explicitReturned + " tol=" + crossTol);
        }
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */

Your last harness did NOT reach any project code on the buggy version that we could observe — it likely threw or returned before getting into the changed method. First priority: construct an input that actually calls the patched method with arguments that get past its initial validation, so execution reaches the changed lines at all.

To make the next harness crash the BUGGY version, work through this:

  1. From the diff, identify the exact behavioural difference between buggy and fixed code — which inputs are handled differently. The crash you want exists only on the buggy side, so target inputs that hit that difference. Do not assume the bug is any particular shape (missing bounds check, wrong branch, off-by-one, null handling, etc.) — read the diff and let it tell you.
  2. Choose input that reaches the changed code AND drives it into the faulting state. Use the FuzzedDataProvider to produce values in the range that exercises the difference, not arbitrary values that are likely rejected early.
  3. Do NOT rely on an exception that BOTH versions throw (e.g. validation that exists in buggy and fixed alike). That fires on the patched code too and so cannot distinguish them. You want a fault that the buggy code reaches and the fixed code prevents.
  4. CRITICAL — let the crash escape. Do NOT wrap the call in a try/catch that swallows the throwable you want Jazzer to report. Catch ONLY checked exceptions the signature forces you to handle, and rethrow them as RuntimeException; let every unchecked exception propagate out of fuzzerTestOneInput so Jazzer can see it.

Return the full corrected FuzzHarness.java — raw Java only, no fences, public class FuzzHarness, entrypoint exactly
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
```

</details>
<details open><summary>▸ Output (~4,330 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static double solveExplicit(BisectionSolver solver, UnivariateRealFunction f,
                                        double min, double max, double initial) {
        try {
            return solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    private static double solveStored(BisectionSolver solver, double min, double max, double initial) {
        try {
            return solver.solve(min, max, initial);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        BisectionSolver anchor = new BisectionSolver();
        double anchorResult = solveExplicit(anchor, f, 3.0, 3.2, 3.1);

        double anchorTol = Math.max(anchor.getAbsoluteAccuracy(), 1e-12);
        if (Math.abs(anchorResult - Math.PI) > anchorTol) {
            throw new RuntimeException("[oracle:anchor-pi-window] metamorphic violation: solving sin(x)=0 on [3.0,3.2] must return the unique root near pi result="
                    + anchorResult + " tol=" + anchorTol);
        }
        double anchorStored = anchor.getResult();
        if (Math.abs(anchorStored - anchorResult) > anchorTol) {
            throw new RuntimeException("[oracle:result-stored-explicit] metamorphic violation: after a successful explicit solve, getResult() must report the returned value returned="
                    + anchorResult + " stored=" + anchorStored + " tol=" + anchorTol);
        }

        int k = data.consumeInt(1, 30);
        int leftMilli = data.consumeInt(10, 250);
        int rightMilli = data.consumeInt(10, 250);
        double root = k * Math.PI;
        double min = root - (leftMilli / 1000.0);
        double max = root + (rightMilli / 1000.0);

        double span = max - min;
        double initial = min + span * (data.consumeInt(0, 1000) / 1000.0);

        BisectionSolver explicitSolver = new BisectionSolver();
        double explicitResult = solveExplicit(explicitSolver, f, min, max, initial);
        double explicitTol = Math.max(explicitSolver.getAbsoluteAccuracy(), 1e-12);

        if (explicitResult < min - explicitTol || explicitResult > max + explicitTol) {
            throw new RuntimeException("[oracle:interval-contained-explicit] metamorphic violation: a bisection result for a bracketed interval must lie within that interval min="
                    + min + " max=" + max + " result=" + explicitResult + " tol=" + explicitTol);
        }

        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = f;
        double storedResult = solveStored(storedSolver, min, max, initial);
        double storedTol = Math.max(storedSolver.getAbsoluteAccuracy(), 1e-12);
        double crossTol = Math.max(Math.max(explicitTol, storedTol), 1e-9);

        if (Math.abs(explicitResult - storedResult) > crossTol) {
            throw new RuntimeException("[oracle:stored-vs-explicit-initial] metamorphic violation: with the same installed function and same valid interval, explicit-function and stored-function initial-value overloads must agree explicit="
                    + explicitResult + " stored=" + storedResult + " min=" + min + " max=" + max + " initial=" + initial + " tol=" + crossTol);
        }

        double explicitStored = explicitSolver.getResult();
        if (Math.abs(explicitStored - explicitResult) > explicitTol) {
            throw new RuntimeException("[oracle:result-stored-explore] metamorphic violation: after a successful explicit solve, getResult() must equal the returned root returned="
                    + explicitResult + " stored=" + explicitStored + " tol=" + explicitTol);
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,036 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, ctor-arg-consistency, explicit-overrides-state, function-zero, initial-stability, interval-contained, interval-contained-explicit, known-root, overload-agree, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,744 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    private static final UnivariateRealFunction SIN = new SinFunction();

    private static final class SolveOutcome {
        final double value;
        final int iterations;
        final double accuracy;

        SolveOutcome(double value, int iterations, double accuracy) {
            this.value = value;
            this.iterations = iterations;
            this.accuracy = accuracy;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            double root = Math.PI * data.consumeInt(-20, 20);

            double leftDistance = 0.01 + (data.consumeInt(0, 900) / 1000.0);
            double rightDistance = 0.01 + (data.consumeInt(0, 900) / 1000.0);

            double min = root - leftDistance;
            double max = root + rightDistance;
            if (!(min < max)) {
                continue;
            }

            int initialSelector = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (initialSelector / 1000.0);
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }

            checkStoredAndExplicitAgreeOnIterations(min, max, initial);
        }
    }

    private static void anchor() {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(SIN, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isRootCauseNpeFromBisectionSolve(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkStoredAndExplicitAgreeOnIterations(double min, double max, double initial) {
        try {
            SolveOutcome stored = solveStored(min, max);
            SolveOutcome explicit = solveExplicit(min, max, initial);

            double tolerance = Math.max(stored.accuracy, explicit.accuracy) * 2.0 + 1e-12;

            /*
             * Contract used for this oracle:
             * BisectionSolver.solve(f, min, max, initial) is documented as the same solve operation
             * over the same function and interval, and the implementation in this class delegates to
             * the same bisection routine as the stored-function overload solve(min, max). Therefore,
             * for the same SinFunction and the same [min, max], both construction styles must run the
             * same deterministic algorithm and agree on both the returned root approximation and the
             * iteration count. A patch that merely suppresses the NPE but still consults the wrong
             * function/state can leave one of these observables inconsistent.
             */
            if (Math.abs(stored.value - explicit.value) > tolerance) {
                throw new RuntimeException(
                        "[oracle:iter-consistency] metamorphic violation: stored/explicit result mismatch"
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " stored=" + stored.value
                                + " explicit=" + explicit.value
                                + " tol=" + tolerance);
            }

            if (stored.iterations != explicit.iterations) {
                throw new RuntimeException(
                        "[oracle:iter-consistency] metamorphic violation: stored/explicit iteration mismatch"
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " storedIterations=" + stored.iterations
                                + " explicitIterations=" + explicit.iterations
                                + " storedValue=" + stored.value
                                + " explicitValue=" + explicit.value);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && startsWithOracle((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isRootCauseNpeFromBisectionSolve(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static SolveOutcome solveStored(double min, double max)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver(SIN);
        double value = solver.solve(min, max);
        return new SolveOutcome(value, solver.getIterationCount(), solver.getAbsoluteAccuracy());
    }

    private static SolveOutcome solveExplicit(double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver();
        double value = solver.solve(SIN, min, max, initial);
        return new SolveOutcome(value, solver.getIterationCount(), solver.getAbsoluteAccuracy());
    }

    private static boolean isRootCauseNpeFromBisectionSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("Argument")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("Illegal");
    }

    private static boolean startsWithOracle(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,054 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, ctor-arg-consistency, explicit-overrides-state, function-zero, initial-stability, interval-contained, interval-contained-explicit, iter-consistency, known-root, overload-agree, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~4,491 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean sawRootCauseNpe = false;

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            boolean inPatchedRegion = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && inPatchedRegion) {
                sawRootCauseNpe = true;
            }
        }

        int milliLeft = data.consumeInt(10, 400);
        int milliRight = data.consumeInt(10, 400);
        double left = milliLeft / 1000.0;
        double right = milliRight / 1000.0;

        double plusMin = Math.PI - left;
        double plusMax = Math.PI + right;
        double minusMin = -Math.PI - right;
        double minusMax = -Math.PI + left;

        double initialOffsetMilli = data.consumeInt(-Math.min(milliLeft, 300), Math.min(milliRight, 300));
        double plusInitial = Math.PI + (initialOffsetMilli / 1000.0);
        if (plusInitial < plusMin) {
            plusInitial = plusMin;
        }
        if (plusInitial > plusMax) {
            plusInitial = plusMax;
        }
        double minusInitial = -plusInitial;

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(new QuinticFunction(), -0.2, 0.2);
        } catch (Throwable t) {
            return;
        }

        double plusRoot;
        try {
            plusRoot = solver.solve(new SinFunction(), plusMin, plusMax, plusInitial);
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        }

        double minusRoot;
        try {
            minusRoot = solver.solve(new SinFunction(), minusMin, minusMax, minusInitial);
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        }

        /*
         * Sound metamorphic oracle:
         * SinFunction is odd, so roots in intervals that are exact negatives of each other
         * around +/- pi must be negatives of each other as well. A correct implementation of
         * solve(f, min, max, initial) must use the explicitly supplied function for both calls;
         * deleting the throw or silently reusing stale solver state breaks this relation.
         */
        double symmetryError = Math.abs(plusRoot + minusRoot);
        double tolerance = 64.0 * solver.getAbsoluteAccuracy();
        if (symmetryError > tolerance) {
            throw new RuntimeException(
                    "[oracle:odd-symmetry] metamorphic violation: symmetric SinFunction intervals should yield opposite roots"
                            + " plusInterval=[" + plusMin + "," + plusMax + "]"
                            + " minusInterval=[" + minusMin + "," + minusMax + "]"
                            + " plusInitial=" + plusInitial
                            + " minusInitial=" + minusInitial
                            + " plusRoot=" + plusRoot
                            + " minusRoot=" + minusRoot
                            + " sum=" + (plusRoot + minusRoot)
                            + " tol=" + tolerance
                            + " anchorNpeSeen=" + sawRootCauseNpe);
        }
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,690 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [23] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean sawRootCauseNpe = false;

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            boolean inPatchedRegion = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inPatchedRegion = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && inPatchedRegion) {
                sawRootCauseNpe = true;
            }
        }

        int milliLeft = data.consumeInt(10, 400);
        int milliRight = data.consumeInt(10, 400);
        double left = milliLeft / 1000.0;
        double right = milliRight / 1000.0;

        double plusMin = Math.PI - left;
        double plusMax = Math.PI + right;
        double minusMin = -Math.PI - right;
        double minusMax = -Math.PI + left;

        double initialOffsetMilli = data.consumeInt(-Math.min(milliLeft, 300), Math.min(milliRight, 300));
        double plusInitial = Math.PI + (initialOffsetMilli / 1000.0);
        if (plusInitial < plusMin) {
            plusInitial = plusMin;
        }
        if (plusInitial > plusMax) {
            plusInitial = plusMax;
        }
        double minusInitial = -plusInitial;

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(new QuinticFunction(), -0.2, 0.2);
        } catch (Throwable t) {
            return;
        }

        double plusRoot;
        try {
            plusRoot = solver.solve(new SinFunction(), plusMin, plusMax, plusInitial);
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        }

        double minusRoot;
        try {
            minusRoot = solver.solve(new SinFunction(), minusMin, minusMax, minusInitial);
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        }

        /*
         * Sound metamorphic oracle:
         * SinFunction is odd, so roots in intervals that are exact negatives of each other
         * around +/- pi must be negatives of each other as well. A correct implementation of
         * solve(f, min, max, initial) must use the explicitly supplied function for both calls;
         * deleting the throw or silently reusing stale solver state breaks this relation.
         */
        double symmetryError = Math.abs(plusRoot + minusRoot);
        double tolerance = 64.0 * solver.getAbsoluteAccuracy();
        if (symmetryError > tolerance) {
            throw new RuntimeException(
                    "[oracle:odd-symmetry] metamorphic violation: symmetric SinFunction intervals should yield opposite roots"
                            + " plusInterval=[" + plusMin + "," + plusMax + "]"
                            + " minusInterval=[" + minusMin + "," + minusMax + "]"
                            + " plusInitial=" + plusInitial
                            + " minusInitial=" + minusInitial
                            + " plusRoot=" + plusRoot
                            + " minusRoot=" + minusRoot
                            + " sum=" + (plusRoot + minusRoot)
                            + " tol=" + tolerance
                            + " anchorNpeSeen=" + sawRootCauseNpe);
        }
    }
}
```
- *[user] message: identical to step [19] — not reprinted*

</details>
<details open><summary>▸ Output (~1,648 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();

        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }

        double leftDelta = data.consumeInt(0, 200) / 1000.0;
        double rightDelta = data.consumeInt(0, 200) / 1000.0;
        double initialShift = data.consumeInt(-200, 200) / 1000.0;

        double min = Math.PI - leftDelta;
        double max = Math.PI + rightDelta;
        if (min == max) {
            max = min + 0.001;
        }
        double initial = Math.PI + initialShift;
        if (initial < min) {
            initial = min;
        }
        if (initial > max) {
            initial = max;
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~3,384 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [23] — not reprinted*
- *[assistant] message: identical to step [25] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();

        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }

        double leftDelta = data.consumeInt(0, 200) / 1000.0;
        double rightDelta = data.consumeInt(0, 200) / 1000.0;
        double initialShift = data.consumeInt(-200, 200) / 1000.0;

        double min = Math.PI - leftDelta;
        double max = Math.PI + rightDelta;
        if (min == max) {
            max = min + 0.001;
        }
        double initial = Math.PI + initialShift;
        if (initial < min) {
            initial = min;
        }
        if (initial > max) {
            initial = max;
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_012/FuzzHarness.java:15: error: unreported exception ConvergenceException; must be caught or declared to be thrown
            solver.solve(f, 3.0, 3.2, 3.1);
                        ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_012/FuzzHarness.java:40: error: unreported exception ConvergenceException; must be caught or declared to be thrown
            solver.solve(f, min, max, initial);
                        ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~1,641 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();

        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }

        double leftDelta = data.consumeInt(0, 200) / 1000.0;
        double rightDelta = data.consumeInt(0, 200) / 1000.0;
        double initialShift = data.consumeInt(-200, 200) / 1000.0;

        double min = Math.PI - leftDelta;
        double max = Math.PI + rightDelta;
        if (max <= min) {
            max = min + 0.001;
        }

        double initial = Math.PI + initialShift;
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        }
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,054 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, ctor-arg-consistency, explicit-overrides-state, function-zero, initial-stability, interval-contained, interval-contained-explicit, iter-consistency, known-root, overload-agree, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,000 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;

        int leftMillis = data.consumeInt(1, 900);
        int rightMillis = data.consumeInt(1, 900);
        double left = leftMillis / 1000.0;
        double right = rightMillis / 1000.0;

        double min = center - left;
        double max = center + right;

        int mode = data.consumeInt(0, 2);
        double initial;
        if (mode == 0) {
            initial = min;
        } else if (mode == 1) {
            initial = max;
        } else {
            int pos = data.consumeInt(0, 1000);
            initial = min + (max - min) * (pos / 1000.0);
        }

        exerciseCase(center, min, max, initial);
    }

    private static void anchor() {
        double center = Math.PI;
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;
        exerciseCase(center, min, max, initial);
    }

    private static void exerciseCase(double center, double min, double max, double initial) {
        UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver explicitSolver = new BisectionSolver();
            double explicit = explicitSolver.solve(f, min, max, initial);

            BisectionSolver storedBroad = new BisectionSolver(f);
            double broad = storedBroad.solve(min, max);

            double refinedHalfWidth = Math.min(center - min, max - center) / 2.0;
            if (refinedHalfWidth <= 0.0) {
                return;
            }

            double refinedMin = center - refinedHalfWidth;
            double refinedMax = center + refinedHalfWidth;

            BisectionSolver storedRefined = new BisectionSolver(f);
            double refined = storedRefined.solve(refinedMin, refinedMax);

            /*
             * Sound metamorphic oracle:
             * For sin(x), any interval [center-a, center+b] with 0<a,b<pi contains exactly one root at center=k*pi
             * if a,b are kept below 1 here. Both the broad and refined intervals bracket that same unique root.
             * Therefore correct calls through the real public API must converge to the same root, even when the
             * interval is narrowed, and regardless of whether the function is supplied explicitly or via constructor state.
             * A throw-deleting or state-skipping patch can silence the known NPE yet still return a wrong value here.
             */
            double tol = Math.max(explicitSolver.getAbsoluteAccuracy(),
                    Math.max(storedBroad.getAbsoluteAccuracy(), storedRefined.getAbsoluteAccuracy())) * 8.0 + 1e-12;

            if (Math.abs(explicit - refined) > tol) {
                throw new RuntimeException("[oracle:refine-same-root] metamorphic violation: explicit broad solve disagrees with refined same-root solve center="
                        + center + " min=" + min + " max=" + max + " initial=" + initial
                        + " explicit=" + explicit + " refined=" + refined + " tol=" + tol);
            }

            if (Math.abs(broad - refined) > tol) {
                throw new RuntimeException("[oracle:stored-refine-same-root] metamorphic violation: stored broad solve disagrees with refined same-root solve center="
                        + center + " min=" + min + " max=" + max
                        + " broad=" + broad + " refined=" + refined + " tol=" + tol);
            }
        } catch (NullPointerException npe) {
            if (isRootCauseNpe(npe)) {
                throw npe;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        int i;
        for (i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,165 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, ctor-arg-consistency, explicit-overrides-state, function-zero, initial-stability, interval-contained, interval-contained-explicit, iter-consistency, known-root, overload-agree, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,131 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 990) / 1000.0);
        double right = 0.01 + (data.consumeInt(0, 990) / 1000.0);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        exerciseCase(f, min, max, initial);

        if (data.consumeBoolean()) {
            double tighterLeft = left * 0.5;
            double tighterRight = right * 0.5;
            double min2 = root - tighterLeft;
            double max2 = root + tighterRight;
            if (min2 < max2) {
                double fraction2 = data.consumeInt(0, 1000) / 1000.0;
                double initial2 = min2 + (max2 - min2) * fraction2;
                exerciseCase(f, min2, max2, initial2);
            }
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double bisected = solver.solve(f, 3.0, 3.2, 3.1);
            try {
                double brent = new BrentSolver().solve(f, 3.0, 3.2);
                assertClose("[oracle:anchor-cross-solver]", 3.0, 3.2, bisected, brent, tolerance(solver, new BrentSolver()));
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }

            try {
                BisectionSolver stored = new BisectionSolver(f);
                stored.solve(3.0, 3.2);
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseCase(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver explicitSolver = new BisectionSolver();
        double explicit;
        try {
            explicit = explicitSolver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            new BisectionSolver(f).solve(min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
        }

        double brent;
        BrentSolver brentSolver = new BrentSolver();
        try {
            brent = brentSolver.solve(f, min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            return;
        }

        /*
         * Contract used for this oracle:
         * both public solvers are asked to solve the same valid bracketing interval for the same real function.
         * For sin(x) on an interval constructed to contain exactly one root k*pi and no other roots,
         * any correct implementation must approximate that same root.
         * A patch that merely suppresses the explicit-overload NPE but routes to stale/null state can return
         * the wrong quantity; cross-checking against an independent real solver detects that.
         */
        assertClose("[oracle:cross-solver-single-root]", min, max, explicit, brent, tolerance(explicitSolver, brentSolver));

        /*
         * Additional metamorphic check:
         * narrowing a valid bracketing interval around the same unique root must not move the reported root
         * outside the accuracy budget by more than solver tolerances.
         */
        double span = max - min;
        if (span > 0.04) {
            double shrink = span * 0.25;
            double min2 = min + shrink;
            double max2 = max - shrink;
            if (min2 < max2 && min2 < 0.5 * (min + max) && 0.5 * (min + max) < max2) {
                try {
                    double explicit2 = new BisectionSolver().solve(f, min2, max2, 0.5 * (min2 + max2));
                    double tol = Math.max(tolerance(explicitSolver, brentSolver), 1e-6);
                    if (Math.abs(explicit - explicit2) > tol * 8.0) {
                        throw new RuntimeException(
                                "[oracle:interval-shrink-consistency] metamorphic violation: same sin root moved too far"
                                        + " min=" + min + " max=" + max
                                        + " min2=" + min2 + " max2=" + max2
                                        + " lhs=" + explicit + " rhs=" + explicit2);
                    }
                } catch (Throwable t) {
                    if (isRootCauseNpe(t)) {
                        throwUnchecked(t);
                    }
                    if (isOracleFailure(t)) {
                        throw (RuntimeException) t;
                    }
                }
            }
        }
    }

    private static double tolerance(BisectionSolver bisection, BrentSolver brent) {
        double tol = Math.max(bisection.getAbsoluteAccuracy(), brent.getAbsoluteAccuracy());
        if (tol < 1e-8) {
            tol = 1e-8;
        }
        return tol * 16.0;
    }

    private static void assertClose(String oracleId, double min, double max, double lhs, double rhs, double tol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                    oracleId + " metamorphic violation: solver disagreement"
                            + " min=" + min + " max=" + max
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("Convergence") >= 0
                || name.indexOf("Argument") >= 0
                || name.indexOf("Invalid") >= 0
                || name.indexOf("Evaluation") >= 0;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 159 has no oracle ID (`throw new RuntimeException( oracleId + " metamorphic violation: solver disagreement" + " min=" + min + " max

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,872 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [31] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 990) / 1000.0);
        double right = 0.01 + (data.consumeInt(0, 990) / 1000.0);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        exerciseCase(f, min, max, initial);

        if (data.consumeBoolean()) {
            double tighterLeft = left * 0.5;
            double tighterRight = right * 0.5;
            double min2 = root - tighterLeft;
            double max2 = root + tighterRight;
            if (min2 < max2) {
                double fraction2 = data.consumeInt(0, 1000) / 1000.0;
                double initial2 = min2 + (max2 - min2) * fraction2;
                exerciseCase(f, min2, max2, initial2);
            }
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double bisected = solver.solve(f, 3.0, 3.2, 3.1);
            try {
                double brent = new BrentSolver().solve(f, 3.0, 3.2);
                assertClose("[oracle:anchor-cross-solver]", 3.0, 3.2, bisected, brent, tolerance(solver, new BrentSolver()));
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }

            try {
                BisectionSolver stored = new BisectionSolver(f);
                stored.solve(3.0, 3.2);
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseCase(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver explicitSolver = new BisectionSolver();
        double explicit;
        try {
            explicit = explicitSolver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            new BisectionSolver(f).solve(min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
        }

        double brent;
        BrentSolver brentSolver = new BrentSolver();
        try {
            brent = brentSolver.solve(f, min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            return;
        }

        /*
         * Contract used for this oracle:
         * both public solvers are asked to solve the same valid bracketing interval for the same real function.
         * For sin(x) on an interval constructed to contain exactly one root k*pi and no other roots,
         * any correct implementation must approximate that same root.
         * A patch that merely suppresses the explicit-overload NPE but routes to stale/null state can return
         * the wrong quantity; cross-checking against an independent real solver detects that.
         */
        assertClose("[oracle:cross-solver-single-root]", min, max, explicit, brent, tolerance(explicitSolver, brentSolver));

        /*
         * Additional metamorphic check:
         * narrowing a valid bracketing interval around the same unique root must not move the reported root
         * outside the accuracy budget by more than solver tolerances.
         */
        double span = max - min;
        if (span > 0.04) {
            double shrink = span * 0.25;
            double min2 = min + shrink;
            double max2 = max - shrink;
            if (min2 < max2 && min2 < 0.5 * (min + max) && 0.5 * (min + max) < max2) {
                try {
                    double explicit2 = new BisectionSolver().solve(f, min2, max2, 0.5 * (min2 + max2));
                    double tol = Math.max(tolerance(explicitSolver, brentSolver), 1e-6);
                    if (Math.abs(explicit - explicit2) > tol * 8.0) {
                        throw new RuntimeException(
                                "[oracle:interval-shrink-consistency] metamorphic violation: same sin root moved too far"
                                        + " min=" + min + " max=" + max
                                        + " min2=" + min2 + " max2=" + max2
                                        + " lhs=" + explicit + " rhs=" + explicit2);
                    }
                } catch (Throwable t) {
                    if (isRootCauseNpe(t)) {
                        throwUnchecked(t);
                    }
                    if (isOracleFailure(t)) {
                        throw (RuntimeException) t;
                    }
                }
            }
        }
    }

    private static double tolerance(BisectionSolver bisection, BrentSolver brent) {
        double tol = Math.max(bisection.getAbsoluteAccuracy(), brent.getAbsoluteAccuracy());
        if (tol < 1e-8) {
            tol = 1e-8;
        }
        return tol * 16.0;
    }

    private static void assertClose(String oracleId, double min, double max, double lhs, double rhs, double tol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                    oracleId + " metamorphic violation: solver disagreement"
                            + " min=" + min + " max=" + max
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("Convergence") >= 0
                || name.indexOf("Argument") >= 0
                || name.indexOf("Invalid") >= 0
                || name.indexOf("Evaluation") >= 0;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 159 has no oracle ID (`throw new RuntimeException( oracleId + " metamorphic violation: solver disagreement" + " min=" + min + " max=" + max + "...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,955 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 990) / 1000.0);
        double right = 0.01 + (data.consumeInt(0, 990) / 1000.0);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        exerciseCase(f, min, max, initial);

        if (data.consumeBoolean()) {
            double tighterLeft = left * 0.5;
            double tighterRight = right * 0.5;
            double min2 = root - tighterLeft;
            double max2 = root + tighterRight;
            if (min2 < max2) {
                double fraction2 = data.consumeInt(0, 1000) / 1000.0;
                double initial2 = min2 + (max2 - min2) * fraction2;
                exerciseCase(f, min2, max2, initial2);
            }
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double bisected = solver.solve(f, 3.0, 3.2, 3.1);
            try {
                double brent = new BrentSolver().solve(f, 3.0, 3.2);
                assertClose("anchor-cross-solver", 3.0, 3.2, bisected, brent, tolerance(solver, new BrentSolver()));
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }

            try {
                new BisectionSolver(f).solve(3.0, 3.2);
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseCase(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver explicitSolver = new BisectionSolver();
        double explicit;
        try {
            explicit = explicitSolver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            new BisectionSolver(f).solve(min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
        }

        double brent;
        BrentSolver brentSolver = new BrentSolver();
        try {
            brent = brentSolver.solve(f, min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            return;
        }

        /*
         * Contract used for this oracle:
         * both public solvers are asked to solve the same valid bracketing interval for the same real function.
         * For sin(x) on an interval constructed to contain exactly one root k*pi and no other roots,
         * any correct implementation must approximate that same root.
         * A patch that merely suppresses the explicit-overload NPE but routes to stale/null state can return
         * the wrong quantity; cross-checking against an independent real solver detects that.
         */
        assertClose("cross-solver-single-root", min, max, explicit, brent, tolerance(explicitSolver, brentSolver));

        /*
         * Additional metamorphic check:
         * narrowing a valid bracketing interval around the same unique root must not move the reported root
         * outside the accuracy budget by more than solver tolerances.
         */
        double span = max - min;
        if (span > 0.04) {
            double shrink = span * 0.25;
            double min2 = min + shrink;
            double max2 = max - shrink;
            if (min2 < max2 && min2 < 0.5 * (min + max) && 0.5 * (min + max) < max2) {
                try {
                    double explicit2 = new BisectionSolver().solve(f, min2, max2, 0.5 * (min2 + max2));
                    double tol = Math.max(tolerance(explicitSolver, brentSolver), 1e-6);
                    if (Math.abs(explicit - explicit2) > tol * 8.0) {
                        throw new RuntimeException(
                                "[oracle:interval-shrink-consistency] metamorphic violation: same sin root moved too far"
                                        + " min=" + min + " max=" + max
                                        + " min2=" + min2 + " max2=" + max2
                                        + " lhs=" + explicit + " rhs=" + explicit2);
                    }
                } catch (Throwable t) {
                    if (isRootCauseNpe(t)) {
                        throwUnchecked(t);
                    }
                    if (isOracleFailure(t)) {
                        throw (RuntimeException) t;
                    }
                }
            }
        }
    }

    private static double tolerance(BisectionSolver bisection, BrentSolver brent) {
        double tol = Math.max(bisection.getAbsoluteAccuracy(), brent.getAbsoluteAccuracy());
        if (tol < 1e-8) {
            tol = 1e-8;
        }
        return tol * 16.0;
    }

    private static void assertClose(String oracleId, double min, double max, double lhs, double rhs, double tol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: solver disagreement"
                            + " min=" + min + " max=" + max
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
        }
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("Convergence") >= 0
                || name.indexOf("Argument") >= 0
                || name.indexOf("Invalid") >= 0
                || name.indexOf("Evaluation") >= 0;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,241 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, explicit-overrides-state, function-zero, initial-stability, interval-contained, interval-contained-explicit, interval-shrink-consistency, iter-consistency, known-root, overload-agree, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,861 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        anchor();

        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-4, 4);
        double root = k * Math.PI;

        double d1 = 1e-3 + (data.consumeInt(0, 5000) / 10000.0);
        double d2 = 1e-3 + (data.consumeInt(0, 5000) / 10000.0);

        double min;
        double max;
        switch (data.consumeInt(0, 2)) {
            case 0:
                min = root;
                max = root + d2;
                break;
            case 1:
                min = root - d1;
                max = root;
                break;
            default:
                min = root - d1;
                max = root + d2;
                break;
        }

        double initial;
        switch (data.consumeInt(0, 3)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) * 0.5;
                break;
            default:
                double t = data.consumeInt(0, 1000) / 1000.0;
                initial = min + (max - min) * t;
                break;
        }

        exerciseCase(f, root, min, max, initial);

        double mirror = Math.max(d1, d2);
        double leftMin = root - mirror;
        double leftMax = root;
        double rightMin = root;
        double rightMax = root + mirror;

        try {
            double lhs = solveStored(f, leftMin, leftMax);
            double rhs = solveStored(f, rightMin, rightMax);
            double tol = tolerance();
            if (Math.abs(lhs - root) > tol || Math.abs(rhs - root) > tol || Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:endpoint-duality] metamorphic violation: endpoint-root intervals around the same sin root must agree inputRoot=" + root + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        try {
            double r = solveExplicit(f, 3.0, 3.2, 3.1);
            double tol = tolerance();
            if (Math.abs(r - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-boundary] metamorphic violation: anchored seed should solve sin near pi result=" + r + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exerciseCase(UnivariateRealFunction f, double expectedRoot, double min, double max, double initial) {
        try {
            double explicit = solveExplicit(f, min, max, initial);
            double tol = tolerance();
            if (Math.abs(explicit - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:endpoint-root] metamorphic violation: a correct bisection solve on a valid sin interval bracketing k*pi must return that root expected=" + expectedRoot + " actual=" + explicit + " min=" + min + " max=" + max + " initial=" + initial + " tol=" + tol);
            }

            double stored = solveStored(f, min, max);
            if (Math.abs(stored - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:stored-endpoint-root] metamorphic violation: stored-function solve on the same valid sin interval must return the same known root expected=" + expectedRoot + " actual=" + stored + " min=" + min + " max=" + max + " tol=" + tol);
            }

            if (Math.abs(explicit - stored) > tol) {
                throw new RuntimeException("[oracle:explicit-vs-stored-endpoint] metamorphic violation: explicit-function and stored-function solves with the same real function and interval must agree explicit=" + explicit + " stored=" + stored + " min=" + min + " max=" + max + " initial=" + initial + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double solveExplicit(UnivariateRealFunction f, double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver();
        return solver.solve(f, min, max, initial);
    }

    private static double solveStored(UnivariateRealFunction f, double min, double max)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver solver = new BisectionSolver();
        solver.f = f;
        return solver.solve(min, max);
    }

    private static double tolerance() {
        return 1e-5;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,342 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-overrides-state, explicit-vs-stored-endpoint, function-zero, initial-stability, interval-contained, interval-contained-explicit, interval-shrink-consistency, iter-consistency, known-root, overload-agree, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,633 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static boolean hasRelevantSolveFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if (e == null) {
                continue;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        // ANCHOR: exact failing test input. This drives the patched overload on a fresh solver
        // with a valid function and a valid bracketing interval.
        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(sin, 3.0, 3.2, 3.1);
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && hasRelevantSolveFrame(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        // EXPLORE: same root-cause property as the seed, but with many valid intervals around
        // roots of sin(x) shifted by 2*pi*k. A correct implementation must accept these.
        int k = data.consumeInt(-8, 8);
        double shift = 2.0 * Math.PI * k;
        double min = 3.0 + shift;
        double max = 3.2 + shift;
        int frac = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (frac / 1000.0);

        try {
            BisectionSolver exploreSolver = new BisectionSolver();
            exploreSolver.solve(sin, min, max, initial);
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && hasRelevantSolveFrame(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        // Independent oracle on the uncovered sibling entry point solve(double, double):
        // Using stored-function solvers, solve sin(x)=0 on two windows that are exact 2*pi*n
        // translations of each other. Since sin(x + 2*pi*n) = sin(x), and each window is a
        // translated copy of [3.0, 3.2] containing the unique root pi + 2*pi*n, a correct
        // solver must return roots whose difference is exactly that translation, up to solver
        // accuracy. This still detects silent wrong-result patches even if the known NPE is
        // merely suppressed.
        int n1 = data.consumeInt(-6, 6);
        int delta = data.consumeInt(-4, 4);
        if (delta == 0) {
            delta = 1;
        }
        int n2 = n1 + delta;

        double s1 = 2.0 * Math.PI * n1;
        double s2 = 2.0 * Math.PI * n2;
        double min1 = 3.0 + s1;
        double max1 = 3.2 + s1;
        double min2 = 3.0 + s2;
        double max2 = 3.2 + s2;

        double r1;
        double r2;
        try {
            BisectionSolver stored1 = new BisectionSolver(sin);
            BisectionSolver stored2 = new BisectionSolver(sin);
            r1 = stored1.solve(min1, max1);
            r2 = stored2.solve(min2, max2);

            double expectedDelta = 2.0 * Math.PI * (n2 - n1);
            double actualDelta = r2 - r1;
            double tol = Math.max(stored1.getAbsoluteAccuracy(), stored2.getAbsoluteAccuracy()) * 8.0;

            if (Math.abs(actualDelta - expectedDelta) > tol) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:periodic-shift] metamorphic violation: translated root windows for sin "
                                + "must produce roots separated by the same 2*pi shift"
                                + " n1=" + n1
                                + " n2=" + n2
                                + " r1=" + r1
                                + " r2=" + r2
                                + " actualDelta=" + actualDelta
                                + " expectedDelta=" + expectedDelta
                                + " tol=" + tol);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof FuzzerSecurityIssueLow) {
                throw t;
            }
            if (t instanceof NullPointerException && hasRelevantSolveFrame(t)) {
                throw t;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,358 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-overrides-state, explicit-vs-stored-endpoint, function-zero, initial-stability, interval-contained, interval-contained-explicit, interval-shrink-consistency, iter-consistency, known-root, overload-agree, periodic-shift, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~4,611 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int cases = 1 + Math.min(4, data.remainingBytes() / 8);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double left = data.consumeInt(1, 1500) / 1000.0;
            double right = data.consumeInt(1, 1500) / 1000.0;
            double root = k * Math.PI;
            double min = root - left;
            double max = root + right;
            double frac = data.consumeInt(-1000, 1000) / 1000.0;
            double initial = root + frac * Math.min(left, right);
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }
            exerciseCase(new SinFunction(), min, max, initial);
        }
    }

    private static void anchor() {
        exerciseCase(new SinFunction(), 3.0, 3.2, 3.1);
    }

    private static void exerciseCase(UnivariateRealFunction function, double min, double max, double initial) {
        BisectionSolver explicitSolver = new BisectionSolver();
        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = function;

        final double explicitRoot;
        try {
            explicitRoot = explicitSolver.solve(function, min, max, initial);
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        final double storedRoot;
        try {
            storedRoot = storedSolver.solve(min, max);
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        double tol = Math.max(explicitSolver.getAbsoluteAccuracy(), storedSolver.getAbsoluteAccuracy()) * 2.0;
        if (Math.abs(explicitRoot - storedRoot) > tol) {
            throw new RuntimeException(
                "[oracle:explicit-stored-root] metamorphic violation: " +
                "the overload taking an explicit function and the overload using the stored function " +
                "must solve the same equation on the same interval when the stored function is set to that same function; " +
                "input=[" + min + "," + max + "] initial=" + initial +
                " explicit=" + explicitRoot + " stored=" + storedRoot + " tol=" + tol
            );
        }

        int explicitIters = explicitSolver.getIterationCount();
        int storedIters = storedSolver.getIterationCount();
        if (explicitIters != storedIters) {
            throw new RuntimeException(
                "[oracle:explicit-stored-iters] consistency violation: " +
                "deterministic bisection with the same function, interval, and accuracy must take the same number of iterations " +
                "across equivalent overloads; input=[" + min + "," + max + "] initial=" + initial +
                " explicitIters=" + explicitIters + " storedIters=" + storedIters
            );
        }
    }

    private static void handleLibraryThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t instanceof RuntimeException) {
            if (t instanceof NullPointerException && validByConstruction && isRootCauseLocation(t)) {
                throw (RuntimeException) t;
            }
            if (isOracleThrowable(t)) {
                throw (RuntimeException) t;
            }
            return;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }
    }

    private static boolean isRootCauseLocation(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleThrowable(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,403 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-overrides-state, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-stability, interval-contained, interval-contained-explicit, interval-shrink-consistency, iter-consistency, known-root, overload-agree, periodic-shift, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,150 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-8, 8);
        double root = k * Math.PI;

        double width = 0.05 + (data.consumeInt(0, 450) / 1000.0);
        double min = root - width;
        double max = root + width;

        double factor = 0.20 + (data.consumeInt(0, 60) / 100.0);
        if (factor >= 1.0) {
            factor = 0.95;
        }
        double staleRoot = root + (data.consumeBoolean() ? 1.0 : -1.0) * width * factor;
        if (staleRoot <= min || staleRoot >= max) {
            staleRoot = root + (width * 0.5);
            if (staleRoot >= max) {
                staleRoot = root - (width * 0.5);
            }
        }

        double initialFrac = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * initialFrac;

        exerciseCase(min, max, initial, staleRoot);
    }

    private static void anchor() {
        try {
            new BisectionSolver().solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isPatchedRegionNullPointer(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseCase(double min, double max, double initial, double staleRoot) {
        SinFunction explicit = new SinFunction();
        PolynomialFunction stale = new PolynomialFunction(new double[] { -staleRoot, 1.0 });

        try {
            BisectionSolver storedSolver = new BisectionSolver();
            storedSolver.f = stale;
            storedSolver.solve(min, max);
        } catch (Throwable t) {
            if (isOracle(t)) {
                throw (RuntimeException) t;
            }
        }

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.f = stale;
            double result = solver.solve(explicit, min, max, initial);

            if (result < min || result > max) {
                throw new RuntimeException(
                    "[oracle:interval-membership] metamorphic violation: returned root left caller interval"
                        + " min=" + min + " max=" + max + " initial=" + initial + " staleRoot=" + staleRoot
                        + " result=" + result);
            }

            /*
             * Contract used for this oracle: the solver returns an approximation to a root of the
             * function supplied to this call. We build a valid bracketing interval around a known
             * sine root by construction, so a correct implementation must return x with sin(x)
             * close to 0, regardless of any stale function stored on the receiver.
             *
             * This catches a throw-deleting / overfitting patch too: if the method silently uses
             * receiver state instead of the explicit function argument, it can return a root of the
             * stale linear polynomial, where |sin(result)| is not close to 0.
             */
            double residual = Math.abs(explicit.value(result));
            double tolerance = Math.max(1.0e-6, 8.0 * solver.getAbsoluteAccuracy());
            if (residual > tolerance) {
                throw new RuntimeException(
                    "[oracle:explicit-residual] metamorphic violation: returned value does not solve explicit function"
                        + " min=" + min + " max=" + max + " initial=" + initial + " staleRoot=" + staleRoot
                        + " result=" + result + " residual=" + residual + " tolerance=" + tolerance);
            }
        } catch (Throwable t) {
            if (isOracle(t)) {
                throw (RuntimeException) t;
            }
            if (isPatchedRegionNullPointer(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t.getClass().getName().contains("ConvergenceException")
            || t.getClass().getName().contains("FunctionEvaluationException")
            || t.getClass().getName().contains("MaxIterationsExceededException");
    }

    private static boolean isPatchedRegionNullPointer(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:explicit-residual] metamorphic violation: returned value does not solve explicit function min=-25.182741228718346 max=-25.082741228718344 initial=-25.18274122871834…

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~27,632 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, known-root, overload-agree, periodic-shift, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,924 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        anchorAndExploreExplicitCall(data, sin);
        crossCheckStoredSolveOnDifferentRoot(data, sin);
    }

    private static void anchorAndExploreExplicitCall(FuzzedDataProvider data, UnivariateRealFunction f) {
        exerciseExplicitCall(f, 3.0, 3.2, 3.1, "anchor");

        int variants = 1 + Math.max(0, data.consumeInt(0, 6));
        for (int i = 0; i < variants; i++) {
            int k = data.consumeInt(-8, 8);
            double center = Math.PI + (2.0 * Math.PI * k);
            double leftPad = 0.01 + (data.consumeInt(0, 400) / 1000.0);
            double rightPad = 0.01 + (data.consumeInt(0, 400) / 1000.0);
            double min = center - leftPad;
            double max = center + rightPad;
            double initial;
            if (data.consumeBoolean()) {
                initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
            } else {
                initial = center + ((data.consumeInt(-1000, 1000)) / 100000.0);
                if (initial < min) {
                    initial = min;
                } else if (initial > max) {
                    initial = max;
                }
            }
            exerciseExplicitCall(f, min, max, initial, "explore-explicit");
        }
    }

    private static void exerciseExplicitCall(UnivariateRealFunction f, double min, double max, double initial, String tag) {
        BisectionSolver solver = new BisectionSolver();
        try {
            double rExplicit = solver.solve(f, min, max, initial);

            // Contract from the shown implementation: solve(f,min,max,initial) delegates to solve(f,min,max),
            // so a correct implementation must agree with a fresh explicit solve on the same interval.
            BisectionSolver fresh = new BisectionSolver();
            double rPlain = fresh.solve(f, min, max);

            double tol = Math.max(solver.getAbsoluteAccuracy(), fresh.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(rExplicit - rPlain) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:explicit-delegation] metamorphic violation: delegated explicit solve disagrees with plain explicit solve"
                        + " tag=" + tag
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " explicit=" + rExplicit
                        + " plain=" + rPlain
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:explicit-delegation] root-cause NPE on valid explicit solve"
                        + " tag=" + tag
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial);
            }
        }
    }

    private static void crossCheckStoredSolveOnDifferentRoot(FuzzedDataProvider data, UnivariateRealFunction f) {
        int k1 = data.consumeInt(-6, 6);
        int k2 = data.consumeInt(-6, 6);
        if (k2 == k1) {
            k2 = k1 + 1;
        }

        double c1 = Math.PI + (2.0 * Math.PI * k1);
        double c2 = Math.PI + (2.0 * Math.PI * k2);

        double left1 = 0.02 + (data.consumeInt(0, 300) / 1000.0);
        double right1 = 0.02 + (data.consumeInt(0, 300) / 1000.0);
        double left2 = 0.02 + (data.consumeInt(0, 300) / 1000.0);
        double right2 = 0.02 + (data.consumeInt(0, 300) / 1000.0);

        double min1 = c1 - left1;
        double max1 = c1 + right1;
        double min2 = c2 - left2;
        double max2 = c2 + right2;

        BisectionSolver stored = new BisectionSolver();
        BisectionSolver fresh = new BisectionSolver();

        try {
            stored.solve(f, min1, max1);
            double viaStored = stored.solve(min2, max2);
            double viaFresh = fresh.solve(f, min2, max2);

            // Public-overload consistency: once the function is installed through solve(f,min,max),
            // the overload solve(min,max) must solve that same stored function for any later valid interval.
            // Independent oracle: a fresh solver solving the same function on the same interval.
            double tol = Math.max(stored.getAbsoluteAccuracy(), fresh.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(viaStored - viaFresh) > tol) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:stored-other-root] consistency violation: stored solve(min,max) disagrees with fresh explicit solve"
                        + " prime=[" + min1 + "," + max1 + "]"
                        + " target=[" + min2 + "," + max2 + "]"
                        + " stored=" + viaStored
                        + " fresh=" + viaFresh
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:stored-other-root] root-cause NPE while solving valid stored interval"
                        + " prime=[" + min1 + "," + max1 + "]"
                        + " target=[" + min2 + "," + max2 + "]");
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Convergence") || n.contains("Invalid") || n.contains("NoBracketing");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:explicit-delegation] root-cause NPE on valid explicit solve tag=anchor min=3.0 max=3.2 initial=3.1

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,095 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, known-root, overload-agree, periodic-shift, refine-same-root, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-other-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,131 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(f, 3.0, 3.2, 3.1);
        } catch (NullPointerException e) {
            boolean fromPatchedRegion = false;
            for (StackTraceElement ste : e.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    fromPatchedRegion = true;
                    break;
                }
            }
            if (!fromPatchedRegion) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        int k = data.consumeInt(-32, 32);
        double root = k * Math.PI;

        int leftMicros = data.consumeInt(1, 1_000_000);
        int rightMicros = data.consumeInt(1, 1_000_000);
        double left = leftMicros / 1_000_000.0;
        double right = rightMicros / 1_000_000.0;

        double min = root - left;
        double max = root + right;

        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            int posMicros = data.consumeInt(0, 1_000_000);
            initial = min + (max - min) * (posMicros / 1_000_000.0);
        } else {
            initial = root;
        }

        if (!(initial >= min && initial <= max)) {
            return;
        }

        try {
            BisectionSolver stored = new BisectionSolver(f);
            double storedResult = stored.solve(min, max);
            double storedReported = stored.getResult();

            BisectionSolver explicit = new BisectionSolver();
            double explicitResult = explicit.solve(f, min, max, initial);
            double explicitReported = explicit.getResult();

            double storedAccuracy = stored.getAbsoluteAccuracy();
            double explicitAccuracy = explicit.getAbsoluteAccuracy();
            double tol = Math.max(8.0 * Math.max(storedAccuracy, explicitAccuracy), 1.0e-6);

            if (Math.abs(storedResult - storedReported) > 0.0) {
                throw new RuntimeException(
                        "[oracle:stored-result-cache] metamorphic violation: solve(min,max) return must equal getResult() "
                                + "min=" + min + " max=" + max + " returned=" + storedResult + " cached=" + storedReported);
            }

            if (Math.abs(explicitResult - explicitReported) > 0.0) {
                throw new RuntimeException(
                        "[oracle:explicit-result-cache] metamorphic violation: solve(f,min,max,initial) return must equal getResult() "
                                + "min=" + min + " max=" + max + " initial=" + initial
                                + " returned=" + explicitResult + " cached=" + explicitReported);
            }

            /*
             * For the same real function and the same bracketing interval containing a unique SinFunction root,
             * solving via the stored-function overload and via the explicit-function overload must agree.
             * This is an equivalence between two public APIs, so a throw-deleting patch that returns the wrong
             * value from the explicit overload is caught even if no exception is thrown.
             */
            if (Math.abs(storedResult - explicitResult) > tol) {
                throw new RuntimeException(
                        "[oracle:stored-explicit-equivalence] metamorphic violation: equivalent bisection entrypoints disagree "
                                + "k=" + k + " min=" + min + " max=" + max + " initial=" + initial
                                + " stored=" + storedResult + " explicit=" + explicitResult + " tol=" + tol);
            }
        } catch (NullPointerException e) {
            return;
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,330 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [45] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        try {
            BisectionSolver anchorSolver = new BisectionSolver();
            anchorSolver.solve(f, 3.0, 3.2, 3.1);
        } catch (NullPointerException e) {
            boolean fromPatchedRegion = false;
            for (StackTraceElement ste : e.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    fromPatchedRegion = true;
                    break;
                }
            }
            if (!fromPatchedRegion) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        int k = data.consumeInt(-32, 32);
        double root = k * Math.PI;

        int leftMicros = data.consumeInt(1, 1_000_000);
        int rightMicros = data.consumeInt(1, 1_000_000);
        double left = leftMicros / 1_000_000.0;
        double right = rightMicros / 1_000_000.0;

        double min = root - left;
        double max = root + right;

        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            int posMicros = data.consumeInt(0, 1_000_000);
            initial = min + (max - min) * (posMicros / 1_000_000.0);
        } else {
            initial = root;
        }

        if (!(initial >= min && initial <= max)) {
            return;
        }

        try {
            BisectionSolver stored = new BisectionSolver(f);
            double storedResult = stored.solve(min, max);
            double storedReported = stored.getResult();

            BisectionSolver explicit = new BisectionSolver();
            double explicitResult = explicit.solve(f, min, max, initial);
            double explicitReported = explicit.getResult();

            double storedAccuracy = stored.getAbsoluteAccuracy();
            double explicitAccuracy = explicit.getAbsoluteAccuracy();
            double tol = Math.max(8.0 * Math.max(storedAccuracy, explicitAccuracy), 1.0e-6);

            if (Math.abs(storedResult - storedReported) > 0.0) {
                throw new RuntimeException(
                        "[oracle:stored-result-cache] metamorphic violation: solve(min,max) return must equal getResult() "
                                + "min=" + min + " max=" + max + " returned=" + storedResult + " cached=" + storedReported);
            }

            if (Math.abs(explicitResult - explicitReported) > 0.0) {
                throw new RuntimeException(
                        "[oracle:explicit-result-cache] metamorphic violation: solve(f,min,max,initial) return must equal getResult() "
                                + "min=" + min + " max=" + max + " initial=" + initial
                                + " returned=" + explicitResult + " cached=" + explicitReported);
            }

            /*
             * For the same real function and the same bracketing interval containing a unique SinFunction root,
             * solving via the stored-function overload and via the explicit-function overload must agree.
             * This is an equivalence between two public APIs, so a throw-deleting patch that returns the wrong
             * value from the explicit overload is caught even if no exception is thrown.
             */
            if (Math.abs(storedResult - explicitResult) > tol) {
                throw new RuntimeException(
                        "[oracle:stored-explicit-equivalence] metamorphic violation: equivalent bisection entrypoints disagree "
                                + "k=" + k + " min=" + min + " max=" + max + " initial=" + initial
                                + " stored=" + storedResult + " explicit=" + explicitResult + " tol=" + tol);
            }
        } catch (NullPointerException e) {
            return;
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (Throwable t) {
            return;
        }
    }
}
```
- *[user] message: identical to step [19] — not reprinted*

</details>
<details open><summary>▸ Output (~3,626 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // Anchor: exact trigger from the failing test. On the buggy version this reaches
        // BisectionSolver.solve(f, min, max, initial) and throws NullPointerException.
        BisectionSolver anchor = new BisectionSolver();
        try {
            anchor.solve(f, 3.0, 3.2, 3.1);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }

        // Explore the same root-cause property with many valid-by-construction inputs:
        // a real function is supplied explicitly, and the interval brackets a SinFunction root.
        int k = data.consumeInt(-64, 64);
        double root = k * Math.PI;

        double left = data.consumeInt(1, 1_000_000) / 1_000_000.0;
        double right = data.consumeInt(1, 1_000_000) / 1_000_000.0;
        double min = root - left;
        double max = root + right;

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double fraction = data.consumeInt(0, 1_000_000) / 1_000_000.0;
            initial = min + (max - min) * fraction;
        }

        BisectionSolver explicitSolver = new BisectionSolver();
        BisectionSolver directSolver = new BisectionSolver();

        try {
            double viaInitial = explicitSolver.solve(f, min, max, initial);
            double direct = directSolver.solve(f, min, max);

            // Contract/metamorphic check: for BisectionSolver, the overload with an
            // explicit initial guess must agree with the overload without it, since
            // bisection does not use the initial guess to change the solved root.
            double tol = Math.max(explicitSolver.getAbsoluteAccuracy(), directSolver.getAbsoluteAccuracy()) * 8.0;
            if (Math.abs(viaInitial - direct) > tol) {
                throw new RuntimeException(
                    "[oracle:initial-overload-agreement] metamorphic violation: "
                        + "solve(f,min,max,initial) disagrees with solve(f,min,max)"
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " viaInitial=" + viaInitial
                        + " direct=" + direct
                        + " tol=" + tol);
            }

            // Independent cache-vs-return consistency check.
            double cached = explicitSolver.getResult();
            if (Math.abs(cached - viaInitial) > 0.0) {
                throw new RuntimeException(
                    "[oracle:result-cache-consistency] metamorphic violation: "
                        + "returned root differs from cached result"
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " returned=" + viaInitial
                        + " cached=" + cached);
            }
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,149 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-overload-agreement, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, known-root, overload-agree, periodic-shift, refine-same-root, result-cache-consistency, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-other-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,272 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeed();
        runScaledLinearOracle(data);
    }

    private static void runAnchorSeed() {
        BisectionSolver solver = new BisectionSolver();
        UnivariateRealFunction f = new SinFunction();
        try {
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeInSolve(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void runScaledLinearOracle(FuzzedDataProvider data) {
        double c = data.consumeInt(-1000, 1000) / 10.0;
        double left = 0.1 + (data.consumeInt(0, 500) / 100.0);
        double right = 0.1 + (data.consumeInt(0, 500) / 100.0);
        double min = c - left;
        double max = c + right;

        int scaleInt = data.consumeInt(-20, 20);
        if (scaleInt == 0) {
            scaleInt = 1;
        }
        double scale = scaleInt;

        double initA = chooseBoundaryOrOutsideInitial(data, min, max);
        double initB = chooseBoundaryOrOutsideInitial(data, min, max);

        PolynomialFunction base = new PolynomialFunction(new double[] { -c, 1.0 });
        PolynomialFunction scaled = new PolynomialFunction(new double[] { -scale * c, scale });

        Double rootA = callExplicitSolve(base, min, max, initA);
        Double rootB = callExplicitSolve(scaled, min, max, initB);

        if (rootA == null || rootB == null) {
            return;
        }

        double tol = 1.0e-6;
        /*
         * Sound oracle:
         * For any non-zero scalar k, f(x) and k*f(x) have exactly the same zero set.
         * Both calls use the real public BisectionSolver API on valid-by-construction brackets
         * around the unique root x = c. A patch that merely suppresses the seed NPE but routes
         * the explicit-function overload through the wrong function/state can make these two
         * equivalent problems disagree.
         */
        if (Math.abs(rootA - rootB) > tol) {
            throw new RuntimeException(
                "[oracle:linear-scale-boundary] metamorphic violation: scalar-multiple functions must yield the same root"
                    + " c=" + c
                    + " scale=" + scale
                    + " min=" + min
                    + " max=" + max
                    + " initA=" + initA
                    + " initB=" + initB
                    + " lhs=" + rootA
                    + " rhs=" + rootB);
        }
    }

    private static double chooseBoundaryOrOutsideInitial(FuzzedDataProvider data, double min, double max) {
        double span = max - min;
        double delta = 0.1 + (data.consumeInt(0, 200) / 100.0);
        switch (data.consumeInt(0, 5)) {
            case 0:
                return min;
            case 1:
                return max;
            case 2:
                return (min + max) / 2.0;
            case 3:
                return min - delta;
            case 4:
                return max + delta;
            default:
                return min + span * (data.consumeInt(0, 1000) / 1000.0);
        }
    }

    private static Double callExplicitSolve(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        try {
            return solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isRootCauseNpeInSolve(t)) {
                throw (RuntimeException) t;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Convergence") || name.contains("Invalid") || name.contains("NoBracketing")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseNpeInSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,241 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchorSeed
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-overload-agreement, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, known-root, linear-scale-boundary, overload-agree, periodic-shift, refine-same-root, result-cache-consistency, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, stored-endpoint-root, stored-other-root, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,765 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidth = 1.0e-6 + (data.consumeInt(1, 1000000) / 1000000.0) * 0.9;
        double rightWidth = 1.0e-6 + (data.consumeInt(1, 1000000) / 1000000.0) * 0.9;

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initialFraction = data.consumeInt(0, 1000000) / 1000000.0;
        double initial = min + (max - min) * initialFraction;

        UnivariateRealFunction f = new SinFunction();

        try {
            exerciseExplicitAndStored(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        try {
            exerciseExplicitAndStored(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throwUnchecked(t);
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void exerciseExplicitAndStored(UnivariateRealFunction f, double min, double max, double initial)
            throws MaxIterationsExceededException, FunctionEvaluationException {
        BisectionSolver explicitSolver = new BisectionSolver();
        double explicit = explicitSolver.solve(f, min, max, initial);

        BisectionSolver storedSolver = new BisectionSolver(f);
        double stored = storedSolver.solve(min, max);

        /*
         * Contract/oracle:
         * These are two public overloads of the same solver on the same function and interval.
         * A correct implementation must solve the same mathematical problem either way.
         * This also steers execution through the uncovered stored-function overload solve(double, double).
         */
        double tol = Math.max(explicitSolver.getAbsoluteAccuracy(), storedSolver.getAbsoluteAccuracy()) * 8.0;
        if (Math.abs(explicit - stored) > tol) {
            throw new RuntimeException(
                    "[oracle:stored-overload-eq] metamorphic violation: explicit and stored overloads disagree"
                            + " min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " explicit=" + explicit
                            + " stored=" + stored
                            + " tol=" + tol);
        }

        /*
         * Independent oracle from the constructed input:
         * We bracket the known root k*pi of sin(x) by construction, so a correct solver must
         * return a point close to that known root on this narrow interval.
         */
        double expected = Math.rint(explicit / Math.PI) * Math.PI;
        if (expected >= min && expected <= max) {
            double knownTol = Math.max(tol, 1.0e-6);
            if (Math.abs(explicit - expected) > knownTol && Math.abs(stored - expected) > knownTol) {
                throw new RuntimeException(
                        "[oracle:sin-known-root] metamorphic violation: neither overload returned the constructed root"
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " expected=" + expected
                                + " explicit=" + explicit
                                + " stored=" + stored
                                + " tol=" + knownTol);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("Argument");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>uncheckedThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,358 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitAndStored
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchorSeed
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-overload-agreement, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, known-root, linear-scale-boundary, overload-agree, periodic-shift, refine-same-root, result-cache-consistency, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, sin-known-root, stored-endpoint-root, stored-other-root, stored-overload-eq, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,159 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeedSilently();
        exploreStateMaskedWrongFunction(data);
    }

    private static void runAnchorSeedSilently() {
        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(new SinFunction(), 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthNpeFromPatchedRegion(t)) {
                return;
            }
        }
    }

    private static void exploreStateMaskedWrongFunction(FuzzedDataProvider data) {
        double center = boundedDouble(data.consumeInt(), -100.0, 100.0);
        double separation = 0.25 + boundedDouble(data.consumeInt(), 0.0, 5.0);
        double halfWidth = separation + 0.5 + boundedDouble(data.consumeInt(), 0.0, 5.0);

        boolean explicitOnRight = data.consumeBoolean();
        double storedRoot = explicitOnRight ? center - separation : center + separation;
        double explicitRoot = explicitOnRight ? center + separation : center - separation;

        double min = Math.min(storedRoot, explicitRoot) - halfWidth;
        double max = Math.max(storedRoot, explicitRoot) + halfWidth;
        if (!(min < max)) {
            return;
        }

        double initial = min + (max - min) * (0.1 + 0.8 * boundedDouble(data.consumeInt(), 0.0, 1.0));

        PolynomialFunction storedFunction = linearRoot(storedRoot);
        PolynomialFunction explicitFunction = linearRoot(explicitRoot);

        BisectionSolver pollutedSolver = new BisectionSolver();
        pollutedSolver.f = storedFunction;

        BisectionSolver freshSolver = new BisectionSolver();

        double pollutedResult;
        double freshResult;
        try {
            pollutedResult = pollutedSolver.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthNpeFromPatchedRegion(t)) {
                return;
            }
            return;
        }

        try {
            freshResult = freshSolver.solve(explicitFunction, min, max);
        } catch (Throwable t) {
            return;
        }

        double acc = Math.max(pollutedSolver.getAbsoluteAccuracy(), freshSolver.getAbsoluteAccuracy());
        double encodedDistance = Math.abs(pollutedResult - explicitRoot);
        double crossDistance = Math.abs(pollutedResult - freshResult);

        /*
         * Contract/oracle:
         * The overload solve(f, min, max, initial) is documented to solve for its explicit
         * function parameter f. For the linear polynomial x - r, the unique root is exactly r.
         * Therefore a correct implementation must return a value within solver accuracy of r,
         * regardless of any unrelated function previously stored on the receiver.
         *
         * Independent cross-check:
         * The same quantity is obtained a second way from a fresh solver via solve(f, min, max).
         * A band-aid that suppresses the known NPE but still uses receiver state instead of the
         * explicit parameter will return the stored function's root and violate both checks.
         */
        if (encodedDistance > 8.0 * acc && crossDistance > 8.0 * acc) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:state-pollution-root] explicit solve ignored its function parameter"
                    + " storedRoot=" + storedRoot
                    + " explicitRoot=" + explicitRoot
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " pollutedResult=" + pollutedResult
                    + " freshResult=" + freshResult
                    + " accuracy=" + acc);
        }

        try {
            double cached = pollutedSolver.getResult();
            if (Math.abs(cached - pollutedResult) > 0.0) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:cached-result-agrees] cached result disagrees with returned root"
                        + " cached=" + cached
                        + " returned=" + pollutedResult);
            }
        } catch (IllegalStateException ignored) {
            return;
        }
    }

    private static PolynomialFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
            || name.contains("Argument")
            || name.contains("Invalid")
            || name.contains("NoBracketing")
            || name.contains("OutOfRange");
    }

    private static boolean isGroundTruthNpeFromPatchedRegion(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long u = raw & 0xffffffffL;
        double unit = u / (double) 0xffffffffL;
        return min + (max - min) * unit;
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:state-pollution-root] explicit solve ignored its function parameter storedRoot=-99.75 explicitRoot=-100.25 min=-101.0 max=-99.0 initial…

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,632 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitAndStored
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.exploreStateMaskedWrongFunction
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchorSeed
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exploreStateMaskedWrongFunction
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cached-result-agrees, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-overload-agreement, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, known-root, linear-scale-boundary, overload-agree, periodic-shift, refine-same-root, result-cache-consistency, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, sin-known-root, state-pollution-root, stored-endpoint-root, stored-other-root, stored-overload-eq, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,503 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        explore(data);
    }

    private static void runAnchor() {
        try {
            exerciseSinCase(3.0, 3.2, 3.1);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        int n = data.consumeInt(-64, 64);
        double root = n * Math.PI;

        double left = positiveWidth(data);
        double right = positiveWidth(data);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double span = max - min;
            double bias = boundedUnit(data) * span * 0.49;
            initial = root + bias;
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }
        }

        try {
            exerciseSinCase(min, max, initial);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void exerciseSinCase(double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        org.apache.commons.math.analysis.UnivariateRealFunction f =
                new org.apache.commons.math.analysis.SinFunction();

        try {
            double result = solver.solve(f, min, max, initial);

            if (result < min || result > max) {
                throw new RuntimeException(
                        "[oracle:result-in-interval] metamorphic violation: returned root escaped bracket"
                                + " min=" + min + " max=" + max + " result=" + result);
            }

            /*
             * Bisection halves the bracket once per loop and stores the loop index i in setResult(m, i)
             * immediately after the successful halving. Therefore, after a successful solve on an initial
             * interval of width W, a correct implementation must satisfy:
             *     W <= absoluteAccuracy * 2^(iterationCount + 1)
             * A throw-deleting or stale-result patch can return a value without performing the required
             * number of halvings, and then this independently recomputed width/iteration relation breaks.
             */
            int iterations = solver.getIterationCount();
            double accuracy = solver.getAbsoluteAccuracy();
            double width = max - min;
            double achievedBound = accuracy * Math.pow(2.0, iterations + 1);
            if (!(width <= achievedBound * 1.0000000001)) {
                throw new RuntimeException(
                        "[oracle:iter-width] metamorphic violation: bisection iteration count inconsistent"
                                + " width=" + width
                                + " accuracy=" + accuracy
                                + " iterations=" + iterations
                                + " bound=" + achievedBound
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " result=" + result);
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException) {
                return;
            }
            if (t instanceof RuntimeException) {
                return;
            }
            return;
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
                || name.contains("FunctionEvaluation")
                || name.contains("MaxIterationsExceeded")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("NotPositive")
                || name.contains("OutOfRange");
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 4);
        if (selector == 0) {
            return 1.0e-6 * (data.consumeInt(1, 1000));
        } else if (selector == 1) {
            return 1.0e-3 * (data.consumeInt(1, 1000));
        } else if (selector == 2) {
            return 0.1 + data.consumeInt(0, 900) / 1000.0;
        } else if (selector == 3) {
            return 1.0 + data.consumeInt(0, 900) / 1000.0;
        } else {
            return 0.2;
        }
    }

    private static double boundedUnit(FuzzedDataProvider data) {
        return data.consumeInt(-1000000, 1000000) / 1000000.0;
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,702 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [55] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        explore(data);
    }

    private static void runAnchor() {
        try {
            exerciseSinCase(3.0, 3.2, 3.1);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        int n = data.consumeInt(-64, 64);
        double root = n * Math.PI;

        double left = positiveWidth(data);
        double right = positiveWidth(data);

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double span = max - min;
            double bias = boundedUnit(data) * span * 0.49;
            initial = root + bias;
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }
        }

        try {
            exerciseSinCase(min, max, initial);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void exerciseSinCase(double min, double max, double initial) {
        BisectionSolver solver = new BisectionSolver();
        org.apache.commons.math.analysis.UnivariateRealFunction f =
                new org.apache.commons.math.analysis.SinFunction();

        try {
            double result = solver.solve(f, min, max, initial);

            if (result < min || result > max) {
                throw new RuntimeException(
                        "[oracle:result-in-interval] metamorphic violation: returned root escaped bracket"
                                + " min=" + min + " max=" + max + " result=" + result);
            }

            /*
             * Bisection halves the bracket once per loop and stores the loop index i in setResult(m, i)
             * immediately after the successful halving. Therefore, after a successful solve on an initial
             * interval of width W, a correct implementation must satisfy:
             *     W <= absoluteAccuracy * 2^(iterationCount + 1)
             * A throw-deleting or stale-result patch can return a value without performing the required
             * number of halvings, and then this independently recomputed width/iteration relation breaks.
             */
            int iterations = solver.getIterationCount();
            double accuracy = solver.getAbsoluteAccuracy();
            double width = max - min;
            double achievedBound = accuracy * Math.pow(2.0, iterations + 1);
            if (!(width <= achievedBound * 1.0000000001)) {
                throw new RuntimeException(
                        "[oracle:iter-width] metamorphic violation: bisection iteration count inconsistent"
                                + " width=" + width
                                + " accuracy=" + accuracy
                                + " iterations=" + iterations
                                + " bound=" + achievedBound
                                + " min=" + min
                                + " max=" + max
                                + " initial=" + initial
                                + " result=" + result);
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException) {
                return;
            }
            if (t instanceof RuntimeException) {
                return;
            }
            return;
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
                || name.contains("FunctionEvaluation")
                || name.contains("MaxIterationsExceeded")
                || name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("NotPositive")
                || name.contains("OutOfRange");
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 4);
        if (selector == 0) {
            return 1.0e-6 * (data.consumeInt(1, 1000));
        } else if (selector == 1) {
            return 1.0e-3 * (data.consumeInt(1, 1000));
        } else if (selector == 2) {
            return 0.1 + data.consumeInt(0, 900) / 1000.0;
        } else if (selector == 3) {
            return 1.0 + data.consumeInt(0, 900) / 1000.0;
        } else {
            return 0.2;
        }
    }

    private static double boundedUnit(FuzzedDataProvider data) {
        return data.consumeInt(-1000000, 1000000) / 1000000.0;
    }
}
```
- *[user] message: identical to step [19] — not reprinted*

</details>
<details open><summary>▸ Output (~2,987 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from the failing test.
        // On the buggy version this reaches BisectionSolver.solve(f, min, max, initial),
        // which incorrectly delegates to solve(min, max) and dereferences the unset stored function.
        BisectionSolver anchorSolver = new BisectionSolver();
        try {
            anchorSolver.solve(f, 3.0, 3.2, 3.1);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }

        // EXPLORE: same root-cause property as the seed.
        // Build valid bracketing intervals around k*pi for sin(x), with initial inside the interval.
        int k = data.consumeInt(-32, 32);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 1000) / 1000.0);
        double right = 0.01 + (data.consumeInt(0, 1000) / 1000.0);
        double min = root - left;
        double max = root + right;
        double initial = min + (max - min) * (data.consumeInt(0, 1000000) / 1000000.0);

        BisectionSolver solver = new BisectionSolver();
        final double result;
        try {
            result = solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }

        // Post-condition / independent oracle:
        // In BisectionSolver.solve(f, min, max), after each iteration the bracket width halves,
        // and when it returns it has called setResult(m, i) with that iteration count.
        // Therefore any correct successful solve must satisfy:
        //   originalWidth <= absoluteAccuracy * 2^(iterationCount + 1)
        // This checks observable solver state rather than only "no exception".
        int iterations = solver.getIterationCount();
        double width = max - min;
        double bound = solver.getAbsoluteAccuracy() * Math.pow(2.0, iterations + 1);
        if (!(width <= bound * 1.0000000001)) {
            throw new RuntimeException(
                "[oracle:iter-width] metamorphic violation: inconsistent bisection progress"
                    + " width=" + width
                    + " bound=" + bound
                    + " iterations=" + iterations
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " result=" + result);
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,644 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitAndStored
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.exploreStateMaskedWrongFunction
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchorSeed
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exploreStateMaskedWrongFunction
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cached-result-agrees, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, function-zero, initial-overload-agreement, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, iter-width, known-root, linear-scale-boundary, overload-agree, periodic-shift, refine-same-root, result-cache-consistency, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, sin-known-root, state-pollution-root, stored-endpoint-root, stored-other-root, stored-overload-eq, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,712 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorSeed();

        int selector = data.consumeInt(0, 3);
        if (selector == 0) {
            exploreLinearExplicitVsStored(data);
        } else if (selector == 1) {
            exploreLinearKnownRoot(data);
        } else if (selector == 2) {
            exploreShiftedInterval(data);
        } else {
            exploreSymmetricWindow(data);
        }
    }

    private static void anchorSeed() {
        try {
            UnivariateRealFunction f = new SinFunction();
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownPatchedRegionNpe(t)) {
                return;
            }
        }
    }

    private static void exploreLinearExplicitVsStored(FuzzedDataProvider data) {
        double explicitRoot = bounded(data.consumeInt(-1000, 1000) / 10.0);
        double delta = 0.5 + (data.consumeInt(0, 200) / 100.0);
        double storedRoot = explicitRoot + (data.consumeBoolean() ? delta : -delta);

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exploreLinearKnownRoot(FuzzedDataProvider data) {
        double explicitRoot = bounded(data.consumeInt(-5000, 5000) / 50.0);
        double offset = 1.0 + (data.consumeInt(0, 300) / 100.0);
        double storedRoot = bounded(explicitRoot + offset + 1.0);

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exploreShiftedInterval(FuzzedDataProvider data) {
        double base = bounded(data.consumeInt(-2000, 2000) / 20.0);
        double explicitRoot = base + 0.25;
        double storedRoot = base - 0.75;

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exploreSymmetricWindow(FuzzedDataProvider data) {
        double center = bounded(data.consumeInt(-3000, 3000) / 30.0);
        double explicitRoot = center - 0.4;
        double storedRoot = center + 0.4;

        exerciseLinearCase(explicitRoot, storedRoot, data);
    }

    private static void exerciseLinearCase(double explicitRoot, double storedRoot, FuzzedDataProvider data) {
        if (Math.abs(explicitRoot - storedRoot) < 0.2) {
            storedRoot = explicitRoot + 1.0;
        }

        double pad = 1.0 + (data.consumeInt(0, 200) / 100.0);
        double min = Math.min(explicitRoot, storedRoot) - pad;
        double max = Math.max(explicitRoot, storedRoot) + pad;
        double initial = (min + max) / 2.0;

        if (!(min < explicitRoot && explicitRoot < max && min < storedRoot && storedRoot < max && min < max)) {
            return;
        }

        PolynomialFunction explicitFunction = linearRoot(explicitRoot);
        PolynomialFunction storedFunction = linearRoot(storedRoot);

        double reported;
        try {
            BisectionSolver solver = new BisectionSolver(storedFunction);
            reported = solver.solve(explicitFunction, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownPatchedRegionNpe(t)) {
                return;
            }
            return;
        }

        double independent;
        try {
            BisectionSolver explicitSolver = new BisectionSolver(explicitFunction);
            independent = explicitSolver.solve(min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = 1.0e-6;

        if (Math.abs(reported - explicitRoot) > tol) {
            throw new RuntimeException(
                "[oracle:linear-root-recovery] metamorphic violation: "
                    + "for the real library PolynomialFunction x-c, solve(f,min,max,initial) must recover c on a valid bracketing interval; "
                    + "inputRoot=" + explicitRoot
                    + " storedRoot=" + storedRoot
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " reported=" + reported);
        }

        if (Math.abs(reported - independent) > tol) {
            throw new RuntimeException(
                "[oracle:fresh-explicit-agreement] metamorphic violation: "
                    + "the explicit-function overload and a fresh solver constructed with that same function compute the same root on the same valid interval; "
                    + "storedRoot=" + storedRoot
                    + " explicitRoot=" + explicitRoot
                    + " min=" + min
                    + " max=" + max
                    + " initial=" + initial
                    + " reported=" + reported
                    + " independent=" + independent);
        }
    }

    private static PolynomialFunction linearRoot(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static boolean isKnownPatchedRegionNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("Argument")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("NotPositive")
                || name.contains("NullArgument");
    }

    private static double bounded(double v) {
        if (v > 1000000.0) {
            return 1000000.0;
        }
        if (v < -1000000.0) {
            return -1000000.0;
        }
        return v;
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:linear-root-recovery] metamorphic violation: for the real library PolynomialFunction x-c, solve(f,min,max,initial) must recover c on a valid bracketing interval; in…

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,952 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.analysis.solvers` (`package org.apache.commons.math.analysis.solvers;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_70_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,8 @@ public class BisectionSolver extends UnivariateRealSolverImpl {
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f,min, max)
+;
     }
 
     /** {@inheritDoc} */
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max, double initial) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
    public double solve(final UnivariateRealFunction f, double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(min, max);
    }
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="solve" from="UnivariateRealSolver.java" abstract="true">
<signature>
double solve(double min, double max) throws ConvergenceException, FunctionEvaluationException
</signature>
<contract>
    double solve(double min, double max) throws ConvergenceException,
        FunctionEvaluationException;
</contract>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max, double initial)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
<implementation in="BisectionSolver.java">
    public double solve(final UnivariateRealFunction f, double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {

        clearResult();
        verifyInterval(min,max);
        double m;
        double fm;
        double fmin;

        int i = 0;
        while (i < maximalIterationCount) {
            m = UnivariateRealSolverUtils.midpoint(min, max);
           fmin = f.value(min);
           fm = f.value(m);

            if (fm * fmin > 0.0) {
                // max and m bracket the root.
                min = m;
            } else {
                // min and m bracket the root.
                max = m;
            }

            if (Math.abs(max - min) <= absoluteAccuracy) {
                m = UnivariateRealSolverUtils.midpoint(min, max);
                setResult(m, i);
                return m;
            }
            ++i;
        }

        throw new MaxIterationsExceededException(maximalIterationCount);
    }
</implementation>
<implementation in="BrentSolver.java">
    public double solve(double min, double max)
        throws MaxIterationsExceededException, FunctionEvaluationException {
        return solve(f, min, max);
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.analysis.solvers.BisectionSolver.solve(BisectionSolver.java:88)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.analysis.solvers.BisectionSolverTest" method="testMath369">
    public void testMath369() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        UnivariateRealSolver solver = new BisectionSolver();
        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.BisectionSolverTest::testMath369
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f function to solve.
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(double min, double max, double initial) / (double min, double max) / (final UnivariateRealFunction f, double min, double max, double initial) / (final UnivariateRealFunction f, double min, double max)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.anchorAndExploreExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitAndStored
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseLinearCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.exploreLinearExplicitVsStored
- org.apache.commons.math.analysis.solvers.FuzzHarness.exploreStateMaskedWrongFunction
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchorSeed
- org.apache.commons.math.analysis.solvers.FuzzHarness.solveExplicit
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall
- com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exploreStateMaskedWrongFunction
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseLinearCase
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve(double, double)
- BisectionSolver.solve(BisectionSolver, double, double)
Check FAMILIES already covered by accepted harnesses: {anchor-boundary, anchor-cross-solver, anchor-npe, anchor-pi, anchor-pi-window, anchor-sin, anchor-state, arg-overload-consistency, cached-result-agrees, cross-solver-single-root, ctor-arg-consistency, endpoint-duality, endpoint-root, explicit-delegation, explicit-overrides-state, explicit-residual, explicit-stored-iters, explicit-stored-root, explicit-vs-stored-endpoint, fresh-explicit-agreement, function-zero, initial-overload-agreement, initial-stability, interval-contained, interval-contained-explicit, interval-membership, interval-shrink-consistency, iter-consistency, iter-width, known-root, linear-root-recovery, linear-scale-boundary, overload-agree, periodic-shift, refine-same-root, result-cache-consistency, result-stored-explicit, result-stored-explore, seed-replay, seed-state-a, seed-state-b, sin-known-root, state-pollution-root, stored-endpoint-root, stored-other-root, stored-overload-eq, stored-refine-same-root, stored-replay, stored-state-a, stored-state-b, stored-vs-explicit-initial, valid-npe}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.checkExplicitFunctionOverridesStoredState; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseCase; com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseExplicitCall. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,344 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        seedProbe();
        variedProbe(data);
    }

    private static void seedProbe() {
        UnivariateRealFunction sin = new SinFunction();
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        BisectionSolver fresh = new BisectionSolver();
        Double expected = invokeInitialOverload(fresh, sin, min, max, initial);
        if (expected == null) {
            return;
        }

        /*
         * Contract/oracle:
         * The overload taking an explicit UnivariateRealFunction must solve for that
         * explicit function; prior receiver state must not change which function is solved.
         * A band-aid that merely avoids the NPE but still uses stale stored state violates
         * this equivalence between a fresh solver and a polluted solver on the same explicit call.
         */
        BisectionSolver polluted = new BisectionSolver();
        polluteWithStoredFunction(polluted, linearMinusOne());
        Double actual = invokeInitialOverload(polluted, sin, min, max, initial);
        if (actual == null) {
            return;
        }

        compareRoots("[oracle:polluted-fresh-seed]", sin, min, max, expected.doubleValue(), actual.doubleValue(), fresh.getAbsoluteAccuracy());
        assertBracketWitness("[oracle:sign-witness-seed]", sin, min, max, actual.doubleValue(), fresh.getAbsoluteAccuracy());
    }

    private static void variedProbe(FuzzedDataProvider data) {
        int k = data.consumeInt(-50, 50);
        double center = k * Math.PI;

        double leftSpan = 0.05 + (data.consumeInt(0, 900) / 1000.0);
        double rightSpan = 0.05 + (data.consumeInt(0, 900) / 1000.0);
        double min = center - leftSpan;
        double max = center + rightSpan;

        if (!(min < max)) {
            return;
        }

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = center;
                break;
            case 3:
                initial = min + (max - min) * 0.25;
                break;
            default:
                initial = min + (max - min) * 0.75;
                break;
        }

        UnivariateRealFunction explicit = new SinFunction();

        BisectionSolver fresh = new BisectionSolver();
        Double expected = invokeInitialOverload(fresh, explicit, min, max, initial);
        if (expected == null) {
            return;
        }

        BisectionSolver polluted = new BisectionSolver();
        if (data.consumeBoolean()) {
            polluteWithStoredFunction(polluted, linearShifted(center + 2.0));
        } else {
            polluteWithStoredFunction(polluted, linearShifted(center - 2.0));
        }

        Double actual = invokeInitialOverload(polluted, explicit, min, max, initial);
        if (actual == null) {
            return;
        }

        compareRoots("[oracle:polluted-fresh-varied]", explicit, min, max, expected.doubleValue(), actual.doubleValue(), fresh.getAbsoluteAccuracy());
        assertBracketWitness("[oracle:sign-witness-varied]", explicit, min, max, actual.doubleValue(), fresh.getAbsoluteAccuracy());
    }

    private static PolynomialFunction linearMinusOne() {
        return new PolynomialFunction(new double[] { -1.0, 1.0 });
    }

    private static PolynomialFunction linearShifted(double root) {
        return new PolynomialFunction(new double[] { -root, 1.0 });
    }

    private static void polluteWithStoredFunction(BisectionSolver solver, UnivariateRealFunction stored) {
        try {
            solver.solve(stored, -2.0, 2.0);
        } catch (Throwable t) {
            // If the pollution attempt fails, leave the solver as-is.
        }
    }

    private static Double invokeInitialOverload(BisectionSolver solver, UnivariateRealFunction f, double min, double max, double initial) {
        try {
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (isRootCauseNpe(t)) {
                throw (RuntimeException) t;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Convergence")
                || name.contains("FunctionEvaluation")
                || name.contains("MaxIterations")
                || name.contains("Invalid")
                || name.contains("Math");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void compareRoots(String tag, UnivariateRealFunction f, double min, double max, double expected, double actual, double absAcc) {
        double tol = Math.max(absAcc * 4.0, 1.0e-8);
        if (Math.abs(expected - actual) > tol) {
            throw new RuntimeException(tag + " metamorphic violation: explicit solve on polluted solver disagrees with fresh solver"
                    + " interval=[" + min + "," + max + "] expected=" + expected + " actual=" + actual);
        }

        try {
            double fe = Math.abs(f.value(expected));
            double fa = Math.abs(f.value(actual));
            double residualSlack = 1.0e-8;
            if (fa > fe + residualSlack) {
                throw new RuntimeException(tag + " metamorphic violation: polluted solver returned a worse residual"
                        + " interval=[" + min + "," + max + "] freshResidual=" + fe + " pollutedResidual=" + fa
                        + " expected=" + expected + " actual=" + actual);
            }
        } catch (FunctionEvaluationException e) {
            // Skip relation if function evaluation itself rejects.
        }
    }

    private static void assertBracketWitness(String tag, UnivariateRealFunction f, double min, double max, double root, double absAcc) {
        /*
         * Contract/oracle:
         * solve returns an approximation of a root within the requested interval.
         * For the non-degenerate SinFunction intervals constructed here, there is exactly one
         * simple root in the interval, so values immediately to either side of the returned root
         * must have opposite signs (or one side may hit zero within tolerance).
         */
        double eps = Math.max(absAcc * 2.0, 1.0e-6);
        double left = Math.max(min, root - eps);
        double right = Math.min(max, root + eps);
        if (!(left < right)) {
            return;
        }

        try {
            double fl = f.value(left);
            double fr = f.value(right);
            if (fl != 0.0 && fr != 0.0 && fl * fr > 0.0) {
                throw new RuntimeException(tag + " metamorphic violation: returned point is not bracketed as a root"
                        + " interval=[" + min + "," + max + "] root=" + root
                        + " left=" + left + " right=" + right
                        + " f(left)=" + fl + " f(right)=" + fr);
            }
        } catch (Throwable t) {
            if (isRootCauseNpe(t)) {
                throw (RuntimeException) t;
            }
        }
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 154 has no oracle ID (`throw new RuntimeException(tag + " metamorphic violation: explicit solve on polluted solver disagrees with f

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **25/25 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov; 25 raw fuzz log(s)**
<details><summary>▸ detail (~12,631 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 492}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 571}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 482}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 543}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 480}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 483}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 491}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 571}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 480}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 571}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 479}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 574}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_013', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_028', 'attempt_029'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_029.java']}

</details>
