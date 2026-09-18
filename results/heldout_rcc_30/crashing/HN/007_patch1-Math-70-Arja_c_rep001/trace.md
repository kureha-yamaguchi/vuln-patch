# Pipeline trace — Math-70

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java
@@ -69,7 +69,7 @@
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f, min, max);
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method=' (~723 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method='testMath369', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/src/test/java/org/apache/commons/math/analysis/solvers/BisectionSolverTest.java', method_source='    public void testMath369() throws Exception {\\n        UnivariateRealFunction f = new SinFunction();\\n        UnivariateRealSolver solver = new BisectionSolver();\\n        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~6,973 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java\n+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java\n@@ -69,7 +69,7 @@\n     /** {@inheritDoc} */\n     public double solve(final UnivariateRealFunction f, double min, double max, double initial)\n         throws MaxIterationsExceededException, FunctionEvaluationException {\n-        return solve(min, max);\n+        return solve(f, min, max);\n     }\n \n     /** {@inheritDoc} */\n",
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
<details><summary>▸ Prompt (2 message(s), ~17,085 chars)</summary>

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
@@ -69,7 +69,7 @@
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f, min, max);
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

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
<details open><summary>▸ Output (~4,439 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runOne(f, 3.0d, 3.2d, 3.1d, true);

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases += Math.abs(data.consumeInt(0, 3));
        }

        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            double left = data.consumeInt(1, 1000) / 1000.0d;
            double right = data.consumeInt(1, 1000) / 1000.0d;

            double min = root - left;
            double max = root + right;
            if (!(min < max)) {
                continue;
            }

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0d);

            runOne(f, min, max, initial, true);
        }
    }

    private static void runOne(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            double withInitial = solver.solve(f, min, max, initial);

            // POST-CONDITION / METAMORPHIC CHECK:
            // The solver exposes same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // with matching documented semantics for the same bracketed problem; the initial guess
            // must not change the bisection root that is returned for a valid bracket.
            // A "fix" that merely avoids the NPE but ignores the function/arguments or returns a
            // different result would violate this sibling-agreement relation.
            try {
                BisectionSolver sibling = new BisectionSolver();
                double withoutInitial = sibling.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 8.0d;
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=min=" + min
                            + ",max=" + max
                            + ",initial=" + initial
                            + " lhs=" + withInitial
                            + " rhs=" + withoutInitial);
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (RuntimeException e) {
                if (isRootCause(e) && validByConstruction) {
                    throw e;
                }
                return;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCause(e) && validByConstruction) {
                throw e;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
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
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,537 chars)</summary>

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
        explore(data);
    }

    private static void anchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        try {
            double r = solver.solve(f, 3.0, 3.2, 3.1);

            /*
             * Oracle: same-name overloads documented to agree on equivalent inputs.
             * For a correct implementation, solve(f,min,max,initial) and solve(f,min,max)
             * must compute the same root for the same function and interval; deleting the
             * failing statement or bypassing the real function would break this agreement.
             */
            BisectionSolver sibling = new BisectionSolver();
            double r2 = sibling.solve(f, 3.0, 3.2);
            if (!closeEnough(r, r2, Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()))) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromSolve(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = positiveWidth(data);
        double rightWidth = positiveWidth(data);

        double min = root - leftWidth;
        double max = root + rightWidth;

        if (!(min < root && root < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double fraction = data.consumeInt(1, 999) / 1000.0;
            initial = min + (max - min) * fraction;
        }

        if (!(min <= initial && initial <= max)) {
            initial = root;
        }

        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();

        double lhs;
        double rhs;
        try {
            lhs = solverA.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromSolve(t) && isValidConstructedInput(f, min, max, initial, root)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            rhs = solverB.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        /*
         * Oracle: overload agreement on equivalent inputs. Both public solve overloads
         * operate on the same function and bracket, so any correct implementation must
         * return the same root up to solver accuracy. If either side throws, we skip.
         */
        double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
        if (!closeEnough(lhs, rhs, tol)) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
        }

        /*
         * Oracle from known construction: we built the interval around a known root of sin(x),
         * namely k*pi, with min < k*pi < max. A correct bisection solve on SinFunction must
         * therefore return that constructed root within solver accuracy.
         */
        if (!closeEnough(lhs, root, tol)) {
            throw new RuntimeException("[oracle:known-root] metamorphic violation: result must recover constructed sin root input=[" + min + "," + max + "," + initial + "] expected=" + root + " actual=" + lhs);
        }
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int milli = data.consumeInt(1, 1000);
        return milli / 1000.0;
    }

    private static boolean isValidConstructedInput(UnivariateRealFunction f, double min, double max, double initial, double root) {
        return f != null
                && !Double.isNaN(min)
                && !Double.isNaN(max)
                && !Double.isNaN(initial)
                && !Double.isInfinite(min)
                && !Double.isInfinite(max)
                && !Double.isInfinite(initial)
                && min < max
                && min <= initial
                && initial <= max
                && min < root
                && root < max;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Invalid") || n.contains("Illegal") || n.contains("Argument");
    }

    private static boolean isRootCauseNpeFromSolve(Throwable t) {
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

    private static boolean closeEnough(double a, double b, double tol) {
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= Math.max(tol, 1e-12 * scale);
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,101 chars)</summary>

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

        // ANCHOR: exact trigger from the regression test.
        runSolveAndMaybePropagate(new BisectionSolver(), f, 3.0, 3.2, 3.1);

        // EXPLORE: build many valid-by-construction brackets around k*pi.
        // For sin(x), any interval [root-delta, root+delta] with 0 < delta < pi
        // brackets a real root at root = k*pi and is a valid input for the solver.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        int milli = data.consumeInt(50, 1000);
        double delta = milli / 1000.0; // 0.05 .. 1.0, safely < pi

        double min = root - delta;
        double max = root + delta;

        int initialSelector = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (initialSelector / 1000.0);

        runSolveAndMaybePropagate(new BisectionSolver(), f, min, max, initial);

        // Mandatory oracle:
        // The overloads solve(f, min, max, initial) and solve(f, min, max) are
        // same-operation overloads on the same valid bracket; for bisection, the
        // initial guess must not change which root is returned for this interval.
        // If either side throws, this check is skipped.
        try {
            BisectionSolver s1 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);

            BisectionSolver s2 = new BisectionSolver();
            double withoutInitial = s2.solve(f, min, max);

            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 4.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", k=" + k + "}"
                                + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            // Oracle from the input itself: we constructed the bracket around the known root k*pi.
            if (Math.abs(withInitial - root) > tol) {
                throw new RuntimeException(
                        "[oracle:known-root] metamorphic violation: solving sin on a bracket centered on k*pi must recover that root"
                                + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", k=" + k + ", root=" + root + "}"
                                + " lhs=" + withInitial + " rhs=" + root);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static void runSolveAndMaybePropagate(BisectionSolver solver, UnivariateRealFunction f,
                                                  double min, double max, double initial) {
        try {
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("Convergence")
                || name.contains("Argument");
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,245 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static boolean anchored = false;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (!anchored) {
            anchored = true;
            runAnchor();
        }
        runExplore(data);
    }

    private static void runAnchor() {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;

        try {
            double r1 = solver.solve(f, min, max, initial);

            /* Contract/oracle:
             * The test case constructs a known valid bracket for sin(x) around the root pi.
             * A correct implementation of solve(f,min,max,initial) must return that root, and
             * the same-name overload solve(f,min,max) must agree on equivalent inputs.
             * A "fix" that merely suppresses the crash but skips using f would violate this.
             */
            BisectionSolver solver2 = new BisectionSolver();
            double r2 = solver2.solve(f, min, max);

            double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(r1 - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-root] metamorphic violation: known sin bracket around pi did not recover pi input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + Math.PI);
            }
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on same valid bracket input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        int n = data.consumeInt(-100, 100);
        double root = n * Math.PI;

        int milli = data.consumeInt(10, 1000);
        double delta = milli / 1000.0;

        double min = root - delta;
        double max = root + delta;

        int initMilli = data.consumeInt(0, milli * 2);
        double initial = min + (initMilli / 1000.0);

        boolean validByConstruction = f != null && min < max && initial >= min && initial <= max && delta > 0.0 && delta < (Math.PI / 2.0);

        if (!validByConstruction) {
            return;
        }

        try {
            BisectionSolver solverA = new BisectionSolver();
            double r1 = solverA.solve(f, min, max, initial);

            /* Contract/oracle:
             * For the same non-null function and same valid interval, the overloads
             * solve(f,min,max,initial) and solve(f,min,max) are documented siblings that
             * solve the same problem; for bisection, the initial guess must not change the root.
             * We construct a genuine bracket around the known root n*pi of sin(x), so a correct
             * implementation is obliged to accept it and return that root within solver accuracy.
             */
            BisectionSolver solverB = new BisectionSolver();
            double r2;
            try {
                r2 = solverB.solve(f, min, max);
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 8.0;
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
            }
            if (Math.abs(r1 - root) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: valid sin bracket around n*pi must recover that root input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + root);
            }
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
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
        if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("Invalid") >= 0
                || name.indexOf("NoBracketing") >= 0
                || name.indexOf("Convergence") >= 0;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,495 chars)</summary>

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

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(anchorFunction, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        int iterations = 1 + data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double leftWidth = 0.01 + (data.consumeInt(1, 1000) / 1000.0) * 1.4;
            double rightWidth = 0.01 + (data.consumeInt(1, 1000) / 1000.0) * 1.4;

            double min = root - leftWidth;
            double max = root + rightWidth;
            if (!(min < max)) {
                continue;
            }

            double selector = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * selector;

            UnivariateRealFunction f = new SinFunction();

            try {
                BisectionSolver solver = new BisectionSolver();
                solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                if (shouldPropagateRootCause(t, true)) {
                    throwUnchecked(t);
                }
                continue;
            }

            /* Contract/oracle:
             * The same-name overloads solve(f, min, max, initial) and solve(f, min, max)
             * are documented to agree on equivalent inputs. For BisectionSolver, the initial
             * guess must not change the computed root on a valid bracket. A patch that merely
             * suppresses the crash or skips the real computation can violate this sibling-
             * agreement relation without throwing.
             */
            try {
                BisectionSolver lhsSolver = new BisectionSolver();
                double lhs = lhsSolver.solve(f, min, max, initial);

                BisectionSolver rhsSolver = new BisectionSolver();
                double rhs = rhsSolver.solve(f, min, max);

                double tol = Math.max(lhsSolver.getAbsoluteAccuracy(), rhsSolver.getAbsoluteAccuracy()) + 1e-12;
                if (Double.isNaN(lhs) != Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                if (shouldPropagateRootCause(t, true)) {
                    throwUnchecked(t);
                }
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,246 chars)</summary>

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

        // ANCHOR: exact regression-test input from BisectionSolverTest.testMath369.
        runCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction inputs that preserve the root cause property:
        // a non-null real function and a valid bracketing interval for a known root.
        int k = data.consumeInt(-100, 100);
        double center = k * Math.PI;
        double delta = 0.01 + (data.consumeInt(0, 1000) / 1000.0) * 1.0; // in [0.01, 1.01]
        if (delta >= (Math.PI / 2.0)) {
            delta = (Math.PI / 2.0) - 0.01;
        }
        double min = center - delta;
        double max = center + delta;

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = center;
        }

        runCase(f, min, max, initial, true);
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();

        try {
            double withInitial = solver.solve(f, min, max, initial);

            // Documented overload-agreement oracle:
            // these solve overloads are same-name variants for the same solver and inputs;
            // for a correct implementation, adding an initial guess must not change the solved root.
            try {
                double withoutInitial = solver.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1e-9);
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) ||
                    Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " lhs=" + withInitial
                            + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (t instanceof NullPointerException && passesThroughSolve(t)) {
            return true;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return false;
        }
        if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
            return false;
        }
        return false;
    }

    private static boolean passesThroughSolve(Throwable t) {
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
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,337 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from the regression test.
        try {
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        // EXPLORE: valid-by-construction bracketing intervals around k*pi for sin(x).
        // For widths in (0, pi/2), sin changes sign across the interval and there is a known root at k*pi.
        int k = data.consumeInt(-100, 100);
        double expectedRoot = k * Math.PI;

        double leftWidth = data.consumeInt(100, 1500) / 1000.0;
        double rightWidth = data.consumeInt(100, 1500) / 1000.0;
        double min = expectedRoot - leftWidth;
        double max = expectedRoot + rightWidth;

        int selector = data.consumeInt(0, 1000);
        double initial;
        if (selector == 0) {
            initial = expectedRoot;
        } else {
            double frac = selector / 1000.0;
            initial = min + (max - min) * frac;
            if (initial <= min) {
                initial = Math.nextUp(min);
            } else if (initial >= max) {
                initial = Math.nextAfter(max, min);
            }
        }

        Double withInitial = null;
        Double withoutInitial = null;
        double acc1;
        double acc2;

        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            acc1 = solver1.getAbsoluteAccuracy();

            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
            acc2 = solver2.getAbsoluteAccuracy();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        // Documented sibling-agreement / post-condition:
        // These solve overloads are same-name variants for the same solver contract; for the same function and
        // same bracketing interval, providing an initial guess must not change the root that BisectionSolver returns.
        // A throw-deleting or misrouting patch can make the (f,min,max,initial) overload ignore f or otherwise
        // diverge from the real solve path, so we compare it to the sibling overload and to the known sine root.
        double tol = Math.max(Math.max(acc1, acc2), 1.0e-6);

        if (Math.abs(withInitial - withoutInitial) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + withoutInitial);
        }

        if (Math.abs(withInitial - expectedRoot) > tol) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: solver did not recover the constructed sine root"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + expectedRoot);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            RuntimeException rt = (RuntimeException) t;
            String msg = rt.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw rt;
            }
            if (validByConstruction && rt instanceof NullPointerException && hasBisectionSolveFrame(rt)) {
                throw rt;
            }
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }

        return;
    }

    private static boolean hasBisectionSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,690 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from the regression test.
        try {
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        // EXPLORE: valid-by-construction bracketing intervals around k*pi for sin(x).
        // For widths in (0, pi/2), sin changes sign across the interval and there is a known root at k*pi.
        int k = data.consumeInt(-100, 100);
        double expectedRoot = k * Math.PI;

        double leftWidth = data.consumeInt(100, 1500) / 1000.0;
        double rightWidth = data.consumeInt(100, 1500) / 1000.0;
        double min = expectedRoot - leftWidth;
        double max = expectedRoot + rightWidth;

        int selector = data.consumeInt(0, 1000);
        double initial;
        if (selector == 0) {
            initial = expectedRoot;
        } else {
            double frac = selector / 1000.0;
            initial = min + (max - min) * frac;
            if (initial <= min) {
                initial = Math.nextUp(min);
            } else if (initial >= max) {
                initial = Math.nextAfter(max, min);
            }
        }

        Double withInitial = null;
        Double withoutInitial = null;
        double acc1;
        double acc2;

        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            acc1 = solver1.getAbsoluteAccuracy();

            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
            acc2 = solver2.getAbsoluteAccuracy();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        // Documented sibling-agreement / post-condition:
        // These solve overloads are same-name variants for the same solver contract; for the same function and
        // same bracketing interval, providing an initial guess must not change the root that BisectionSolver returns.
        // A throw-deleting or misrouting patch can make the (f,min,max,initial) overload ignore f or otherwise
        // diverge from the real solve path, so we compare it to the sibling overload and to the known sine root.
        double tol = Math.max(Math.max(acc1, acc2), 1.0e-6);

        if (Math.abs(withInitial - withoutInitial) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + withoutInitial);
        }

        if (Math.abs(withInitial - expectedRoot) > tol) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: solver did not recover the constructed sine root"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + expectedRoot);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            RuntimeException rt = (RuntimeException) t;
            String msg = rt.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw rt;
            }
            if (validByConstruction && rt instanceof NullPointerException && hasBisectionSolveFrame(rt)) {
                throw rt;
            }
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }

        return;
    }

    private static boolean hasBisectionSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/fuzz/attempt_007/FuzzHarness.java:8: error: cannot find symbol
import org.apache.commons.math.analysis.UnivariateRealSolver;
                                       ^
  symbol:   class UnivariateRealSolver
  location: package org.apache.commons.math.analysis
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~4,512 chars)</summary>

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

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        int k = data.consumeInt(-100, 100);
        double expectedRoot = k * Math.PI;

        double leftWidth = data.consumeInt(100, 1500) / 1000.0;
        double rightWidth = data.consumeInt(100, 1500) / 1000.0;
        double min = expectedRoot - leftWidth;
        double max = expectedRoot + rightWidth;

        int selector = data.consumeInt(0, 1000);
        double initial;
        if (selector == 0) {
            initial = expectedRoot;
        } else {
            double frac = selector / 1000.0;
            initial = min + (max - min) * frac;
            if (initial <= min) {
                initial = Math.nextUp(min);
            } else if (initial >= max) {
                initial = Math.nextAfter(max, min);
            }
        }

        Double withInitial;
        Double withoutInitial;
        double acc1;
        double acc2;

        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            acc1 = solver1.getAbsoluteAccuracy();

            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
            acc2 = solver2.getAbsoluteAccuracy();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        double tol = Math.max(Math.max(acc1, acc2), 1.0e-6);

        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + withoutInitial);
        }

        if (Math.abs(withInitial.doubleValue() - expectedRoot) > tol) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: solver did not recover the constructed sine root"
                    + " input={k=" + k
                    + ", min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + "} lhs=" + withInitial
                    + " rhs=" + expectedRoot);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            RuntimeException rt = (RuntimeException) t;
            String msg = rt.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw rt;
            }
            if (validByConstruction && rt instanceof NullPointerException && hasBisectionSolveFrame(rt)) {
                throw rt;
            }
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }
    }

    private static boolean hasBisectionSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,737 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input first.
        trySolveAndCheck(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: generate many valid-by-construction intervals for the same real API path.
        // Property: non-null function, min < max, and [min,max] brackets a real root of sin(x),
        // with initial inside the interval. A correct implementation must handle these inputs.
        int rootIndex = data.consumeInt(-1000, 1000);
        double center = rootIndex * Math.PI;

        double leftWidth = 0.0001 + (data.consumeInt(1, 100000) / 100000.0) * 3.0;
        double rightWidth = 0.0001 + (data.consumeInt(1, 100000) / 100000.0) * 3.0;

        if (leftWidth > Math.PI / 2.0) {
            leftWidth = Math.PI / 2.0;
        }
        if (rightWidth > Math.PI / 2.0) {
            rightWidth = Math.PI / 2.0;
        }

        double min = center - leftWidth;
        double max = center + rightWidth;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 100000) / 100000.0;
        double initial = min + (max - min) * fraction;

        trySolveAndCheck(f, min, max, initial, true);

        int extraRounds = data.consumeInt(0, 3);
        for (int i = 0; i < extraRounds; i++) {
            int deltaLeft = data.consumeInt(0, 1000);
            int deltaRight = data.consumeInt(0, 1000);
            double min2 = center - Math.max(0.0001, leftWidth * (1.0 - deltaLeft / 2000.0));
            double max2 = center + Math.max(0.0001, rightWidth * (1.0 - deltaRight / 2000.0));
            if (!(min2 < max2)) {
                continue;
            }
            double frac2 = data.consumeInt(0, 100000) / 100000.0;
            double initial2 = min2 + (max2 - min2) * frac2;
            trySolveAndCheck(f, min2, max2, initial2, true);
        }
    }

    private static void trySolveAndCheck(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solverA = new BisectionSolver();
        try {
            double withInitial = solverA.solve(f, min, max, initial);

            // Oracle: same-name overload agreement.
            // For a correct BisectionSolver, the overload taking an initial guess must agree with
            // the overload without it on the same function and bracketing interval; bisection does
            // not semantically depend on the initial guess for the returned root.
            BisectionSolver solverB = new BisectionSolver();
            double withoutInitial;
            try {
                withoutInitial = solverB.solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 2.0;
            if (Double.isNaN(withInitial) != Double.isNaN(withoutInitial) ||
                (!Double.isNaN(withInitial) && Math.abs(withInitial - withoutInitial) > tol)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                throw rethrow(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        return hasSolveFrame(t);
    }

    private static boolean hasSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,944 chars)</summary>

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

        // ANCHOR: exact failing test inputs. These are valid-by-construction:
        // non-null function, a bracketing interval around pi, and an in-interval initial guess.
        runSolve(f, 3.0d, 3.2d, 3.1d, true);
        checkSiblingAgreement(f, 3.0d, 3.2d, 3.1d);

        // EXPLORE: the root cause is that solve(f, min, max, initial) ignored the provided
        // function and delegated to solve(min, max), so any valid call through this overload
        // can hit the same NPE. Build many real, valid bracketing intervals for SinFunction.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);
        double rightWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);

        double min = root - leftWidth;
        double max = root + rightWidth;

        // Keep magnitudes moderate and ensure the initial guess is inside [min, max].
        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min + (max - min) * boundedUnit(data.consumeInt());
                break;
            default:
                initial = (min + max) / 2.0d;
                break;
        }
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        runSolve(f, min, max, initial, true);
        checkSiblingAgreement(f, min, max, initial);
    }

    private static void runSolve(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkSiblingAgreement(UnivariateRealFunction f, double min, double max, double initial) {
        // Contract/oracle: the same-name solve overloads are documented to agree on equivalent
        // inputs. For BisectionSolver, the initial guess is not semantically relevant to the
        // bisection result, so solve(f,min,max,initial) and solve(f,min,max) must produce the
        // same root for any valid bracketing interval. A patch that merely suppresses the crash
        // but returns a wrong value will violate this sibling-agreement relation.
        double withInitial;
        double withoutInitial;
        double tolerance;
        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            tolerance = solver1.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) ||
            Double.isInfinite(withInitial) || Double.isInfinite(withoutInitial)) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve overloads returned non-finite results input=min=" +
                    min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }

        double allowed = Math.max(tolerance, 1.0e-12d);
        if (Math.abs(withInitial - withoutInitial) > allowed) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min=" +
                    min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + allowed);
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static double boundedUnit(int x) {
        long nonNegative = x & 0x7fffffffL;
        return nonNegative / (double) Integer.MAX_VALUE;
    }

    private static double boundedDouble(int x, double min, double max) {
        return min + (max - min) * boundedUnit(x);
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,202 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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

        // ANCHOR: exact failing test inputs. These are valid-by-construction:
        // non-null function, a bracketing interval around pi, and an in-interval initial guess.
        runSolve(f, 3.0d, 3.2d, 3.1d, true);
        checkSiblingAgreement(f, 3.0d, 3.2d, 3.1d);

        // EXPLORE: the root cause is that solve(f, min, max, initial) ignored the provided
        // function and delegated to solve(min, max), so any valid call through this overload
        // can hit the same NPE. Build many real, valid bracketing intervals for SinFunction.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);
        double rightWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);

        double min = root - leftWidth;
        double max = root + rightWidth;

        // Keep magnitudes moderate and ensure the initial guess is inside [min, max].
        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min + (max - min) * boundedUnit(data.consumeInt());
                break;
            default:
                initial = (min + max) / 2.0d;
                break;
        }
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        runSolve(f, min, max, initial, true);
        checkSiblingAgreement(f, min, max, initial);
    }

    private static void runSolve(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkSiblingAgreement(UnivariateRealFunction f, double min, double max, double initial) {
        // Contract/oracle: the same-name solve overloads are documented to agree on equivalent
        // inputs. For BisectionSolver, the initial guess is not semantically relevant to the
        // bisection result, so solve(f,min,max,initial) and solve(f,min,max) must produce the
        // same root for any valid bracketing interval. A patch that merely suppresses the crash
        // but returns a wrong value will violate this sibling-agreement relation.
        double withInitial;
        double withoutInitial;
        double tolerance;
        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            tolerance = solver1.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) ||
            Double.isInfinite(withInitial) || Double.isInfinite(withoutInitial)) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve overloads returned non-finite results input=min=" +
                    min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }

        double allowed = Math.max(tolerance, 1.0e-12d);
        if (Math.abs(withInitial - withoutInitial) > allowed) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min=" +
                    min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + allowed);
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static double boundedUnit(int x) {
        long nonNegative = x & 0x7fffffffL;
        return nonNegative / (double) Integer.MAX_VALUE;
    }

    private static double boundedDouble(int x, double min, double max) {
        return min + (max - min) * boundedUnit(x);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/fuzz/attempt_010/FuzzHarness.java:70: error: exception IllegalArgumentException has already been caught
        } catch (IllegalArgumentException t) {
          ^
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~4,944 chars)</summary>

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

        runSolve(f, 3.0d, 3.2d, 3.1d, true);
        checkSiblingAgreement(f, 3.0d, 3.2d, 3.1d);

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);
        double rightWidth = boundedDouble(data.consumeInt(), 0.01d, 1.0d);

        double min = root - leftWidth;
        double max = root + rightWidth;

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min + (max - min) * boundedUnit(data.consumeInt());
                break;
            default:
                initial = (min + max) / 2.0d;
                break;
        }

        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        runSolve(f, min, max, initial, true);
        checkSiblingAgreement(f, min, max, initial);
    }

    private static void runSolve(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkSiblingAgreement(UnivariateRealFunction f, double min, double max, double initial) {
        double withInitial;
        double withoutInitial;
        double tolerance;

        try {
            BisectionSolver solver1 = new BisectionSolver();
            withInitial = solver1.solve(f, min, max, initial);
            tolerance = solver1.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver solver2 = new BisectionSolver();
            withoutInitial = solver2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                || Double.isInfinite(withInitial) || Double.isInfinite(withoutInitial)) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve overloads returned non-finite results input=min="
                    + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }

        double allowed = Math.max(tolerance, 1.0e-12d);
        if (Math.abs(withInitial - withoutInitial) > allowed) {
            throw new RuntimeException("[oracle:sibling-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min="
                    + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + allowed);
        }
    }

    private static boolean isRootCause(Throwable t) {
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
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static double boundedUnit(int x) {
        long nonNegative = x & 0x7fffffffL;
        return nonNegative / (double) Integer.MAX_VALUE;
    }

    private static double boundedDouble(int x, double min, double max) {
        return min + (max - min) * boundedUnit(x);
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,842 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runAnchor(f);

        // EXPLORE: generate many valid-by-construction bracketed intervals around a real root
        // so solve(f,min,max,initial) is obligated to handle them.
        int cases = 1 + Math.max(0, Math.min(8, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            double root = data.consumeInt(-1000, 1000) * Math.PI;

            // Positive offsets strictly less than pi keep opposite signs around n*pi.
            double left = positiveOffset(data);
            double right = positiveOffset(data);

            double min = root - left;
            double max = root + right;

            double initial;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    initial = min;
                    break;
                case 1:
                    initial = max;
                    break;
                default:
                    double t = data.consumeInt(0, 10000) / 10000.0;
                    initial = min + (max - min) * t;
                    break;
            }

            runValidCase(f, root, min, max, initial);
        }
    }

    private static void runAnchor(UnivariateRealFunction f) {
        double min = 3.0;
        double max = 3.2;
        double initial = 3.1;
        double expected = Math.PI;

        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, min, max, initial);
            double tol = solver.getAbsoluteAccuracy();
            if (Double.isNaN(result) || Math.abs(result - expected) > tol) {
                throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact regression input should solve sin(x)=0 near PI input=min=3.0,max=3.2,initial=3.1 result=" + result + " expected=" + expected + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract/oracle: same-name overloads that solve the same problem must agree.
        // For a correct implementation, solve(f,min,max,initial) and solve(f,min,max)
        // solve the same bracketed equation on the same interval; bisection ignores the
        // initial guess, so deleting/changing the delegation would make these diverge.
        try {
            BisectionSolver s1 = new BisectionSolver();
            BisectionSolver s2 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);
            double withoutInitial = s2.solve(f, min, max);
            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 8.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:sibling-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            // Per hygiene rules, if either side throws, skip the check.
        }
    }

    private static void runValidCase(UnivariateRealFunction f, double expectedRoot, double min, double max, double initial) {
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, min, max, initial);
            double tol = Math.max(solver.getAbsoluteAccuracy() * 8.0, 1.0e-8);
            if (Double.isNaN(result) || result < min || result > max || Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: for a valid bracket around n*PI, solver must return that known root within accuracy input=min=" + min + ",max=" + max + ",initial=" + initial + " expected=" + expectedRoot + " result=" + result + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract/oracle: the two public overloads with and without initial guess must
        // agree on equivalent valid inputs. Both sides use real library calls only.
        try {
            BisectionSolver s1 = new BisectionSolver();
            BisectionSolver s2 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);
            double withoutInitial = s2.solve(f, min, max);
            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 8.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:sibling-explore] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            // If either side throws, this relation does not apply for that input.
        }
    }

    private static double positiveOffset(FuzzedDataProvider data) {
        int units = data.consumeInt(1, 1_000_000);
        return (Math.PI / 4.0) * (units / 1_000_000.0);
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        return t instanceof NullPointerException && hasSolveFrame(t);
    }

    private static boolean hasSolveFrame(Throwable t) {
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
        if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
            return true;
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid")
                || name.contains("illegal")
                || name.contains("argument")
                || name.contains("range")
                || name.contains("bounds")
                || name.contains("validation");
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,111 chars)</summary>

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

        {
            BisectionSolver solver = new BisectionSolver();
            double min = 3.0;
            double max = 3.2;
            double initial = 3.1;
            boolean validByConstruction = true;
            try {
                double withInitial = solver.solve(f, min, max, initial);
                try {
                    double withoutInitial = solver.solve(f, min, max);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                    if (Math.abs(withInitial - withoutInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (NumberFormatException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                return;
            } catch (Throwable ignored) {
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        if (!(initial >= min && initial <= max)) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        boolean validByConstruction = true;
        try {
            double withInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle: the same-name solve overloads taking the same function and interval
             * are documented to agree where their docs match; the extra initial guess must not change
             * the solved root on a valid bracketing interval. A patch that merely suppresses the crash
             * or bypasses the real solve path can violate this observable agreement without throwing.
             */
            try {
                double withoutInitial = solver.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                if (Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + ",root=" + root + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    throughSolve = true;
                    break;
                }
            }
            if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                throw t;
            }
            return;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,643 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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

        {
            BisectionSolver solver = new BisectionSolver();
            double min = 3.0;
            double max = 3.2;
            double initial = 3.1;
            boolean validByConstruction = true;
            try {
                double withInitial = solver.solve(f, min, max, initial);
                try {
                    double withoutInitial = solver.solve(f, min, max);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                    if (Math.abs(withInitial - withoutInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (NumberFormatException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                return;
            } catch (Throwable ignored) {
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        if (!(initial >= min && initial <= max)) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        boolean validByConstruction = true;
        try {
            double withInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle: the same-name solve overloads taking the same function and interval
             * are documented to agree where their docs match; the extra initial guess must not change
             * the solved root on a valid bracketing interval. A patch that merely suppresses the crash
             * or bypasses the real solve path can violate this observable agreement without throwing.
             */
            try {
                double withoutInitial = solver.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                if (Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + ",root=" + root + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    throughSolve = true;
                    break;
                }
            }
            if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                throw t;
            }
            return;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/fuzz/attempt_013/FuzzHarness.java:32: error: exception NumberFormatException has already been caught
            } catch (NumberFormatException e) {
              ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/fuzz/attempt_013/FuzzHarness.java:93: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
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
<details open><summary>▸ Output (~4,975 chars)</summary>

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

        {
            BisectionSolver solver = new BisectionSolver();
            double min = 3.0;
            double max = 3.2;
            double initial = 3.1;
            boolean validByConstruction = true;
            try {
                double withInitial = solver.solve(f, min, max, initial);
                try {
                    double withoutInitial = solver.solve(f, min, max);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                    if (Math.abs(withInitial - withoutInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                return;
            } catch (Throwable ignored) {
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double left = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double right = 0.05 + (data.consumeInt(0, 950) / 1000.0);
        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        if (!(initial >= min && initial <= max)) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        boolean validByConstruction = true;
        try {
            double withInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle: the same-name solve overloads taking the same function and interval
             * are documented to agree where their docs match; the extra initial guess must not change
             * the solved root on a valid bracketing interval. A patch that merely suppresses the crash
             * or bypasses the real solve path can violate this observable agreement without throwing.
             */
            try {
                double withoutInitial = solver.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
                if (Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + ",root=" + root + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    throughSolve = true;
                    break;
                }
            }
            if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                throw t;
            }
            return;
        } catch (Throwable ignored) {
            return;
        }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,998 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test from BisectionSolverTest.testMath369.
        exerciseScenario(f, 3.0, 3.2, 3.1, Math.PI, true);

        // EXPLORE: build many valid bracketing intervals around a known sin root n*pi.
        // This guarantees f != null and a real root inside [min, max], so a correct
        // implementation is obliged to handle the input.
        int n = data.consumeInt(-1000, 1000);
        double expectedRoot = n * Math.PI;

        int deltaMicros = data.consumeInt(1, 1_000_000);
        double delta = deltaMicros / 1_000_000.0;
        if (delta >= (Math.PI / 2.0)) {
            delta = (Math.PI / 2.0) - 1e-6;
        }
        if (delta <= 0.0) {
            delta = 1e-6;
        }

        double min = expectedRoot - delta;
        double max = expectedRoot + delta;

        int initialMicros = data.consumeInt(-deltaMicros, deltaMicros);
        double initial = expectedRoot + (initialMicros / 1_000_000.0);
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        exerciseScenario(f, min, max, initial, expectedRoot, true);
    }

    private static void exerciseScenario(UnivariateRealFunction f, double min, double max, double initial,
                                         double expectedRoot, boolean validByConstruction) {
        UnivariateRealSolver solverWithInitial = new BisectionSolver();
        double withInitial;
        try {
            withInitial = solverWithInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        UnivariateRealSolver solverWithoutInitial = new BisectionSolver();
        double withoutInitial;
        try {
            withoutInitial = solverWithoutInitial.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle:
        // A solver.solve(...) call returns a zero of the supplied function in the interval,
        // to within the solver's absolute accuracy. We construct [n*pi-delta, n*pi+delta]
        // with 0 < delta < pi/2 for sin(x), so the unique zero in the interval is exactly n*pi.
        // A throw-deleting or wrong-delegation patch can avoid the NPE yet still return a wrong value.
        double tol1 = Math.max(((BisectionSolver) solverWithInitial).getAbsoluteAccuracy() * 4.0, 1e-8);
        if (Double.isNaN(withInitial) || Double.isInfinite(withInitial)
                || Math.abs(withInitial - expectedRoot) > tol1) {
            throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed unique sin root not recovered input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + expectedRoot);
        }

        // Sibling-agreement oracle:
        // The overloads solve(f,min,max,initial) and solve(f,min,max) are documented siblings for the
        // same solving operation. On our valid interval they must agree on the same root.
        double tol2 = Math.max(Math.max(((BisectionSolver) solverWithInitial).getAbsoluteAccuracy(),
                                        ((BisectionSolver) solverWithoutInitial).getAbsoluteAccuracy()) * 4.0, 1e-8);
        if (Double.isNaN(withoutInitial) || Double.isInfinite(withoutInitial)
                || Math.abs(withInitial - withoutInitial) > tol2) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagree input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,351 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test from BisectionSolverTest.testMath369.
        exerciseScenario(f, 3.0, 3.2, 3.1, Math.PI, true);

        // EXPLORE: build many valid bracketing intervals around a known sin root n*pi.
        // This guarantees f != null and a real root inside [min, max], so a correct
        // implementation is obliged to handle the input.
        int n = data.consumeInt(-1000, 1000);
        double expectedRoot = n * Math.PI;

        int deltaMicros = data.consumeInt(1, 1_000_000);
        double delta = deltaMicros / 1_000_000.0;
        if (delta >= (Math.PI / 2.0)) {
            delta = (Math.PI / 2.0) - 1e-6;
        }
        if (delta <= 0.0) {
            delta = 1e-6;
        }

        double min = expectedRoot - delta;
        double max = expectedRoot + delta;

        int initialMicros = data.consumeInt(-deltaMicros, deltaMicros);
        double initial = expectedRoot + (initialMicros / 1_000_000.0);
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        exerciseScenario(f, min, max, initial, expectedRoot, true);
    }

    private static void exerciseScenario(UnivariateRealFunction f, double min, double max, double initial,
                                         double expectedRoot, boolean validByConstruction) {
        UnivariateRealSolver solverWithInitial = new BisectionSolver();
        double withInitial;
        try {
            withInitial = solverWithInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        UnivariateRealSolver solverWithoutInitial = new BisectionSolver();
        double withoutInitial;
        try {
            withoutInitial = solverWithoutInitial.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle:
        // A solver.solve(...) call returns a zero of the supplied function in the interval,
        // to within the solver's absolute accuracy. We construct [n*pi-delta, n*pi+delta]
        // with 0 < delta < pi/2 for sin(x), so the unique zero in the interval is exactly n*pi.
        // A throw-deleting or wrong-delegation patch can avoid the NPE yet still return a wrong value.
        double tol1 = Math.max(((BisectionSolver) solverWithInitial).getAbsoluteAccuracy() * 4.0, 1e-8);
        if (Double.isNaN(withInitial) || Double.isInfinite(withInitial)
                || Math.abs(withInitial - expectedRoot) > tol1) {
            throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed unique sin root not recovered input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + expectedRoot);
        }

        // Sibling-agreement oracle:
        // The overloads solve(f,min,max,initial) and solve(f,min,max) are documented siblings for the
        // same solving operation. On our valid interval they must agree on the same root.
        double tol2 = Math.max(Math.max(((BisectionSolver) solverWithInitial).getAbsoluteAccuracy(),
                                        ((BisectionSolver) solverWithoutInitial).getAbsoluteAccuracy()) * 4.0, 1e-8);
        if (Double.isNaN(withoutInitial) || Double.isInfinite(withoutInitial)
                || Math.abs(withInitial - withoutInitial) > tol2) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagree input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/fuzz/attempt_015/FuzzHarness.java:8: error: cannot find symbol
import org.apache.commons.math.analysis.UnivariateRealSolver;
                                       ^
  symbol:   class UnivariateRealSolver
  location: package org.apache.commons.math.analysis
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~4,208 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        exerciseScenario(f, 3.0, 3.2, 3.1, Math.PI, true);

        int n = data.consumeInt(-1000, 1000);
        double expectedRoot = n * Math.PI;

        int deltaMicros = data.consumeInt(1, 1_000_000);
        double delta = deltaMicros / 1_000_000.0;
        if (delta >= (Math.PI / 2.0)) {
            delta = (Math.PI / 2.0) - 1e-6;
        }
        if (delta <= 0.0) {
            delta = 1e-6;
        }

        double min = expectedRoot - delta;
        double max = expectedRoot + delta;

        int initialMicros = data.consumeInt(-deltaMicros, deltaMicros);
        double initial = expectedRoot + (initialMicros / 1_000_000.0);
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        exerciseScenario(f, min, max, initial, expectedRoot, true);
    }

    private static void exerciseScenario(UnivariateRealFunction f, double min, double max, double initial,
                                         double expectedRoot, boolean validByConstruction) {
        BisectionSolver solverWithInitial = new BisectionSolver();
        double withInitial;
        try {
            withInitial = solverWithInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        BisectionSolver solverWithoutInitial = new BisectionSolver();
        double withoutInitial;
        try {
            withoutInitial = solverWithoutInitial.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle: for the constructed interval [n*pi-delta, n*pi+delta] with 0 < delta < pi/2,
        // sin(x) has the known root n*pi in the interval, so a correct solve call must recover that root.
        double tol1 = Math.max(solverWithInitial.getAbsoluteAccuracy() * 4.0, 1e-8);
        if (Double.isNaN(withInitial) || Double.isInfinite(withInitial)
                || Math.abs(withInitial - expectedRoot) > tol1) {
            throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed unique sin root not recovered input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + expectedRoot);
        }

        // Sibling-agreement oracle: solve(f,min,max,initial) and solve(f,min,max) are same-operation overloads;
        // on this valid bracket around a unique root they must agree within solver accuracy.
        double tol2 = Math.max(Math.max(solverWithInitial.getAbsoluteAccuracy(),
                                        solverWithoutInitial.getAbsoluteAccuracy()) * 4.0, 1e-8);
        if (Double.isNaN(withoutInitial) || Double.isInfinite(withoutInitial)
                || Math.abs(withInitial - withoutInitial) > tol2) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagree input="
                    + "min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,587 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runValidCase(f, 3.0d, 3.2d, 3.1d, Math.PI, true);

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double left = 0.05d + (Math.abs(data.consumeInt(-1000, 1000)) / 1000.0d) * 1.0d;
        double right = 0.05d + (Math.abs(data.consumeInt(-1000, 1000)) / 1000.0d) * 1.0d;

        double min = root - left;
        double max = root + right;
        if (!(min < max)) {
            return;
        }

        double ratio = Math.abs(data.consumeInt(-1000, 1000)) / 1000.0d;
        double initial = min + (max - min) * ratio;
        if (initial < min) {
            initial = min;
        } else if (initial > max) {
            initial = max;
        }

        runValidCase(f, min, max, initial, root, false);
    }

    private static void runValidCase(UnivariateRealFunction f, double min, double max, double initial, double expectedRoot, boolean anchor) {
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, min, max, initial);

            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double withoutInitial = solverWithoutInitial.solve(f, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());

            /* Contract/oracle:
             * The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are documented
             * as equivalent solver entry points for the same function and interval; a correct
             * implementation must therefore agree on the located root. A patch that merely avoids
             * the crash but skips using the provided function or otherwise returns the wrong answer
             * will violate this sibling-agreement relation.
             */
            if (Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            /* Oracle from constructed input:
             * We built a non-null SinFunction and an interval [root-left, root+right] around the
             * known root k*pi, so a correct solver is obligated to return that root within its
             * advertised absolute accuracy.
             */
            if (Math.abs(withInitial - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solver must recover constructed sin root input=min=" + min + ",max=" + max + ",initial=" + initial + ",expected=" + expectedRoot + " lhs=" + withInitial + " rhs=" + expectedRoot);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (anchor && isWrappedRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.indexOf("invalid") >= 0 || lower.indexOf("illegal") >= 0 || lower.indexOf("argument") >= 0) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        return (t instanceof NullPointerException) && hasSolveFrame(t);
    }

    private static boolean isWrappedRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur != t && (cur instanceof NullPointerException) && hasSolveFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasSolveFrame(Throwable t) {
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,930 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input. On the buggy version, this reaches
        // BisectionSolver.solve(f, min, max, initial) and throws NPE from solve.
        try {
            BisectionSolver solver = new BisectionSolver();
            double r4 = solver.solve(f, 3.0, 3.2, 3.1);

            // Metamorphic/post-condition:
            // The same-name overloads are documented to solve the same problem for the same
            // function and interval; the extra "initial" argument must not change the root
            // found by bisection here. A throw-deleting or branch-skipping patch could avoid
            // the crash yet return a wrong value, so compare against the sibling overload.
            BisectionSolver sibling = new BisectionSolver();
            double r3 = sibling.solve(f, 3.0, 3.2);
            double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 4.0;
            if (!(Double.isNaN(r4) || Double.isNaN(r3)) && Math.abs(r4 - r3) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r4 + " rhs=" + r3);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        int cases = 1 + Math.abs(data.consumeInt(0, 4));
        for (int i = 0; i < cases; i++) {
            double[] interval = makeValidSinBracket(data);
            double min = interval[0];
            double max = interval[1];
            double initial = min + (max - min) * boundedFraction(data);

            try {
                BisectionSolver solver4 = new BisectionSolver();
                double r4 = solver4.solve(f, min, max, initial);

                // Same documented problem, sibling overload agreement on the same valid input.
                BisectionSolver solver3 = new BisectionSolver();
                double r3 = solver3.solve(f, min, max);

                double tol = Math.max(solver4.getAbsoluteAccuracy(), solver3.getAbsoluteAccuracy()) * 4.0;
                if (!(Double.isNaN(r4) || Double.isNaN(r3)) && Math.abs(r4 - r3) > tol) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + r4 + " rhs=" + r3);
                }
            } catch (Throwable t) {
                if (shouldPropagate(t, true)) {
                    throwUnchecked(t);
                }
                return;
            }
        }
    }

    private static double[] makeValidSinBracket(FuzzedDataProvider data) {
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        // Keep the width moderate and non-degenerate; symmetric interval around k*pi
        // guarantees sign change for sin as long as width is in (0, pi).
        int milli = data.consumeInt(50, 3000);
        double width = milli / 1000.0;
        if (width >= Math.PI) {
            width = Math.PI - 0.001;
        }

        double min = root - width;
        double max = root + width;
        if (!(min < max)) {
            min = root - 0.5;
            max = root + 0.5;
        }
        return new double[] { min, max };
    }

    private static double boundedFraction(FuzzedDataProvider data) {
        int v = data.consumeInt(0, 1000000);
        return v / 1000000.0;
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (t instanceof RuntimeException) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return false;
            }
            if (t instanceof NullPointerException && passesThroughPatchedSolve(t)) {
                return true;
            }
            if (t.getClass() == RuntimeException.class && isOracleException(t)) {
                return true;
            }
            return false;
        }
        // Checked exceptions from argument checking / solver rejection are clean post-fix behavior.
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return false;
        }
        return false;
    }

    private static boolean isOracleException(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean passesThroughPatchedSolve(Throwable t) {
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,372 chars)</summary>

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

        {
            BisectionSolver solver = new BisectionSolver();
            boolean validInput = true;
            try {
                double result = solver.solve(f, 3.0, 3.2, 3.1);
                double expected = Math.PI;
                if (Math.abs(result - expected) > solver.getAbsoluteAccuracy()) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: documented overload agreement/root result for SinFunction near pi input=[3.0,3.2,3.1] lhs=" + result + " rhs=" + expected);
                }
            } catch (Throwable t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                boolean validation = t instanceof IllegalArgumentException
                        || t instanceof NumberFormatException
                        || (t.getClass().getName().startsWith("org.apache.commons.math")
                            && t.getClass().getSimpleName().toLowerCase().contains("invalid"))
                        || (t instanceof FunctionEvaluationException)
                        || (t instanceof MaxIterationsExceededException);
                if (validInput && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (!validation) {
                    return;
                }
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;
        int milliWidth = data.consumeInt(1, 1000);
        double width = milliWidth / 1000.0;
        double min = center - width;
        double max = center + width;
        double initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);

        {
            BisectionSolver solver = new BisectionSolver();
            boolean validInput = true;
            try {
                solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                boolean validation = t instanceof IllegalArgumentException
                        || t instanceof NumberFormatException
                        || (t.getClass().getName().startsWith("org.apache.commons.math")
                            && (t.getClass().getSimpleName().toLowerCase().contains("invalid")
                                || t.getClass().getSimpleName().toLowerCase().contains("convergence")
                                || t.getClass().getSimpleName().toLowerCase().contains("evaluation")))
                        || (t instanceof FunctionEvaluationException)
                        || (t instanceof MaxIterationsExceededException);
                if (validInput && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (!validation) {
                    return;
                }
            }
        }

        {
            BisectionSolver solverA = new BisectionSolver();
            BisectionSolver solverB = new BisectionSolver();
            Double lhs = null;
            Double rhs = null;

            try {
                lhs = solverA.solve(f, min, max, initial);
            } catch (Throwable t) {
                return;
            }

            try {
                rhs = solverB.solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            if (lhs != null && rhs != null) {
                double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());
                if (Double.isNaN(lhs.doubleValue()) || Double.isNaN(rhs.doubleValue()) || Math.abs(lhs.doubleValue() - rhs.doubleValue()) > tol) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: same-name solve overloads are documented to agree on equivalent valid inputs for the same function and interval input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
                }
            }
        }

        {
            BisectionSolver solver = new BisectionSolver();
            double initial2 = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
            Double r1 = null;
            Double r2 = null;

            try {
                r1 = solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                return;
            }

            try {
                r2 = solver.solve(f, min, max, initial2);
            } catch (Throwable t) {
                return;
            }

            if (r1 != null && r2 != null) {
                double tol = solver.getAbsoluteAccuracy();
                if (Double.isNaN(r1.doubleValue()) || Double.isNaN(r2.doubleValue()) || Math.abs(r1.doubleValue() - r2.doubleValue()) > tol) {
                    throw new RuntimeException("[oracle:initial-ignored] metamorphic violation: bisection solve on the same function and bracketing interval must agree across equivalent initial guesses input=[" + min + "," + max + "," + initial + "," + initial2 + "] lhs=" + r1 + " rhs=" + r2);
                }
            }
        }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,033 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        runScenario(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction inputs with the same root-cause property:
        // a non-null real function and a bracketing interval around a known sin root,
        // with an initial guess inside the interval.
        int scenarios = 1 + Math.abs(data.consumeInt(0, 3));
        for (int i = 0; i < scenarios; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int milli = data.consumeInt(50, 1000);
            double delta = milli / 1000.0; // moderate, non-zero, <= 1.0

            double min = root - delta;
            double max = root + delta;

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0);

            runScenario(f, min, max, initial, true);
        }
    }

    private static void runScenario(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        double lhs;
        try {
            UnivariateRealSolver solver = new BisectionSolver();
            lhs = solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrowUnchecked(t);
            }
            return;
        }

        // Oracle: same-name overload agreement. The 4-arg solve(f, min, max, initial)
        // and 3-arg solve(f, min, max) are documented sibling overloads and the patch
        // itself changes delegation from the former to the latter, so any correct
        // implementation must produce the same root (within solver accuracy) on the
        // same valid bracketing interval. A "fix" that only suppresses the crash or
        // changes control flow incorrectly will violate this relation.
        double rhs;
        double tol;
        try {
            BisectionSolver solver = new BisectionSolver();
            rhs = solver.solve(f, min, max);
            tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(lhs) != Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,386 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealSolver;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        runScenario(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction inputs with the same root-cause property:
        // a non-null real function and a bracketing interval around a known sin root,
        // with an initial guess inside the interval.
        int scenarios = 1 + Math.abs(data.consumeInt(0, 3));
        for (int i = 0; i < scenarios; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int milli = data.consumeInt(50, 1000);
            double delta = milli / 1000.0; // moderate, non-zero, <= 1.0

            double min = root - delta;
            double max = root + delta;

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0);

            runScenario(f, min, max, initial, true);
        }
    }

    private static void runScenario(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        double lhs;
        try {
            UnivariateRealSolver solver = new BisectionSolver();
            lhs = solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrowUnchecked(t);
            }
            return;
        }

        // Oracle: same-name overload agreement. The 4-arg solve(f, min, max, initial)
        // and 3-arg solve(f, min, max) are documented sibling overloads and the patch
        // itself changes delegation from the former to the latter, so any correct
        // implementation must produce the same root (within solver accuracy) on the
        // same valid bracketing interval. A "fix" that only suppresses the crash or
        // changes control flow incorrectly will violate this relation.
        double rhs;
        double tol;
        try {
            BisectionSolver solver = new BisectionSolver();
            rhs = solver.solve(f, min, max);
            tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(lhs) != Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/007_patch1-Math-70-Arja_c_rep001/Math_70_buggy/fuzz/attempt_020/FuzzHarness.java:8: error: cannot find symbol
import org.apache.commons.math.analysis.UnivariateRealSolver;
                                       ^
  symbol:   class UnivariateRealSolver
  location: package org.apache.commons.math.analysis
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~3,120 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runScenario(f, 3.0, 3.2, 3.1, true);

        int scenarios = data.consumeInt(1, 4);
        for (int i = 0; i < scenarios; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            double leftDelta = data.consumeInt(50, 1000) / 1000.0;
            double rightDelta = data.consumeInt(50, 1000) / 1000.0;

            double min = root - leftDelta;
            double max = root + rightDelta;

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0);

            runScenario(f, min, max, initial, true);
        }
    }

    private static void runScenario(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        double lhs;
        try {
            BisectionSolver solver = new BisectionSolver();
            lhs = solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrowUnchecked(t);
            }
            return;
        }

        double rhs;
        double tol;
        try {
            BisectionSolver solver = new BisectionSolver();
            rhs = solver.solve(f, min, max);
            tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
        } catch (Throwable t) {
            return;
        }

        if ((Double.isNaN(lhs) && !Double.isNaN(rhs))
                || (!Double.isNaN(lhs) && Double.isNaN(rhs))
                || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,332 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        exerciseSolveOverload(f, 3.0, 3.2, 3.1, true, "anchor");

        // EXPLORE: valid-by-construction intervals that bracket a real root of sin(x).
        // Root-cause property: calling the public overload solve(f, min, max, initial)
        // with a non-null function on otherwise valid inputs must behave like the sibling
        // overload solve(f, min, max), not dereference an uninitialized stored function.
        int cases = 1 + Math.abs(data.consumeInt() % 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double leftWidth = 0.05 + (Math.abs(data.consumeInt()) % 450) / 1000.0;
            double rightWidth = 0.05 + (Math.abs(data.consumeInt()) % 450) / 1000.0;

            double min = root - leftWidth;
            double max = root + rightWidth;

            double ratio = (Math.abs(data.consumeInt()) % 1000) / 1000.0;
            double initial = min + (max - min) * ratio;

            exerciseSolveOverload(f, min, max, initial, true, "explore:" + k + ":" + i);
        }
    }

    private static void exerciseSolveOverload(UnivariateRealFunction f, double min, double max, double initial,
                                              boolean validByConstruction, String label) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isGroundTruthRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Metamorphic/post-condition:
        // The documented same-name overloads are required to agree on equivalent inputs.
        // For BisectionSolver, solve(f, min, max, initial) must be observationally consistent
        // with solve(f, min, max) on valid inputs; a "fix" that merely suppresses the NPE but
        // returns a wrong result would violate this relation.
        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double rhs = withoutInitial.solve(f, min, max);

            double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy());
            if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
                return;
            }
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=" + label + " min=" + min + " max=" + max + " initial=" + initial
                        + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:overload-agreement]")) {
                throw (RuntimeException) t;
            }
            // If either side throws, this oracle does not apply to that input.
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
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
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("Argument")
                || name.contains("Convergence")
                || name.contains("Evaluation")
                || name.contains("MaxIterations")
                || name.contains("NoBracketing")
                || name.contains("NoData");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 25: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,895 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        exerciseSolveOverload(f, 3.0, 3.2, 3.1, true, "anchor");

        // EXPLORE: valid-by-construction intervals that bracket a real root of sin(x).
        // Root-cause property: calling the public overload solve(f, min, max, initial)
        // with a non-null function on otherwise valid inputs must behave like the sibling
        // overload solve(f, min, max), not dereference an uninitialized stored function.
        int cases = 1 + Math.abs(data.consumeInt() % 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double leftWidth = 0.05 + (Math.abs(data.consumeInt()) % 450) / 1000.0;
            double rightWidth = 0.05 + (Math.abs(data.consumeInt()) % 450) / 1000.0;

            double min = root - leftWidth;
            double max = root + rightWidth;

            double ratio = (Math.abs(data.consumeInt()) % 1000) / 1000.0;
            double initial = min + (max - min) * ratio;

            exerciseSolveOverload(f, min, max, initial, true, "explore:" + k + ":" + i);
        }
    }

    private static void exerciseSolveOverload(UnivariateRealFunction f, double min, double max, double initial,
                                              boolean validByConstruction, String label) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isGroundTruthRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Metamorphic/post-condition:
        // The documented same-name overloads are required to agree on equivalent inputs.
        // For BisectionSolver, solve(f, min, max, initial) must be observationally consistent
        // with solve(f, min, max) on valid inputs; a "fix" that merely suppresses the NPE but
        // returns a wrong result would violate this relation.
        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double rhs = withoutInitial.solve(f, min, max);

            double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy());
            if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
                return;
            }
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=" + label + " min=" + min + " max=" + max + " initial=" + initial
                        + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:overload-agreement]")) {
                throw (RuntimeException) t;
            }
            // If either side throws, this oracle does not apply to that input.
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
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
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("Argument")
                || name.contains("Convergence")
                || name.contains("Evaluation")
                || name.contains("MaxIterations")
                || name.contains("NoBracketing")
                || name.contains("NoData");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 25: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~4,210 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        exerciseSolveOverload(f, 3.0, 3.2, 3.1, true, "anchor");

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double leftWidth = 0.05 + data.consumeInt(0, 449) / 1000.0;
            double rightWidth = 0.05 + data.consumeInt(0, 449) / 1000.0;

            double min = root - leftWidth;
            double max = root + rightWidth;

            double ratio = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * ratio;

            exerciseSolveOverload(f, min, max, initial, true, "explore:" + k + ":" + i);
        }
    }

    private static void exerciseSolveOverload(UnivariateRealFunction f, double min, double max, double initial,
                                              boolean validByConstruction, String label) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (validByConstruction && isGroundTruthRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double rhs = withoutInitial.solve(f, min, max);

            double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy());
            if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
                return;
            }
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=" + label + " min=" + min + " max=" + max + " initial=" + initial
                        + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:overload-agreement]")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("Argument")
                || name.contains("Convergence")
                || name.contains("Evaluation")
                || name.contains("MaxIterations")
                || name.contains("NoBracketing")
                || name.contains("NoData");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,265 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        trySolveAndMaybePropagate(new SinFunction(), 3.0, 3.2, 3.1, true);

        // EXPLORE: same root cause property with varied, valid-by-construction inputs.
        UnivariateRealFunction f;
        int which = data.consumeInt(0, 1);
        if (which == 0) {
            f = new SinFunction();

            int k = data.consumeInt(-50, 50);
            double root = k * Math.PI;

            double left = boundedPositive(data.consumeInt(1, 1000) / 1000.0);
            double right = boundedPositive(data.consumeInt(1, 1000) / 1000.0);

            double min = root - left;
            double max = root + right;
            double initial = chooseInitial(data, min, max);

            trySolveAndMaybePropagate(f, min, max, initial, true);
            siblingAgreementOracle(f, min, max, initial);
        } else {
            f = new QuinticFunction();

            double left = boundedPositive(data.consumeInt(1, 1000) / 1000.0);
            double right = boundedPositive(data.consumeInt(1, 1000) / 1000.0);

            double min = -left;
            double max = right;
            double initial = chooseInitial(data, min, max);

            trySolveAndMaybePropagate(f, min, max, initial, true);
            siblingAgreementOracle(f, min, max, initial);
        }
    }

    private static double boundedPositive(double x) {
        if (x <= 0.0) {
            return 0.001;
        }
        if (x > 1.0) {
            return 1.0;
        }
        return x;
    }

    private static double chooseInitial(FuzzedDataProvider data, double min, double max) {
        if (data.consumeBoolean()) {
            return min;
        }
        if (data.consumeBoolean()) {
            return max;
        }
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        return min + (max - min) * fraction;
    }

    private static void trySolveAndMaybePropagate(
            UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (validByConstruction && isRootCauseNpeFromSolve(e)) {
                throw e;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }
    }

    private static boolean isRootCauseNpeFromSolve(Throwable t) {
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

    private static void siblingAgreementOracle(
            UnivariateRealFunction f, double min, double max, double initial) {
        double withInitial;
        double withoutInitial;
        double acc;

        try {
            BisectionSolver s1 = new BisectionSolver();
            withInitial = s1.solve(f, min, max, initial);
            acc = s1.getAbsoluteAccuracy();
        } catch (Throwable t) {
            return;
        }

        try {
            BisectionSolver s2 = new BisectionSolver();
            withoutInitial = s2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)) {
            return;
        }

        // Documented overload-agreement oracle:
        // the overload solve(f, min, max, initial) should behave like solve(f, min, max)
        // for BisectionSolver because bisection does not use the initial guess; a "fix"
        // that merely suppresses the crash but returns a different result would violate this.
        double tolerance = Math.max(acc * 2.0, 1.0e-12);
        if (Math.abs(withInitial - withoutInitial) > tolerance) {
            throw new RuntimeException(
                    "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max)"
                            + " input=min=" + min
                            + ", max=" + max
                            + ", initial=" + initial
                            + ", function=" + f.getClass().getName()
                            + " lhs=" + withInitial
                            + ", rhs=" + withoutInitial);
        }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,945 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(anchorFunction, 3.0, 3.2, 3.1);

            // Contract / sibling-agreement oracle:
            // The overloads solve(f, min, max, initial) and solve(f, min, max) are documented
            // as same-name solver entry points for the same problem. For bisection, the initial
            // guess is not part of the algorithm's result; a correct implementation must still
            // solve the same bracketed function. A "fix" that merely suppresses the crash but
            // ignores f or otherwise changes behavior would disagree with the sibling overload.
            try {
                BisectionSolver sibling = new BisectionSolver();
                double siblingResult = sibling.solve(anchorFunction, 3.0, 3.2);
                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 4.0;
                if (Math.abs(result - siblingResult) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=[min=3.0,max=3.2,initial=3.1]"
                            + " lhs=" + result
                            + " rhs=" + siblingResult);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: generate many valid-by-construction brackets around known sin roots k*pi.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = positiveWidth(data);
        double rightWidth = positiveWidth(data);
        double min = root - leftWidth;
        double max = root + rightWidth;

        if (!(min < root && root < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            // interior point
            double fraction = data.consumeInt(1, 999) / 1000.0;
            initial = min + (max - min) * fraction;
        } else {
            // arbitrary ordered point in range
            initial = root + (data.consumeInt(-500, 500) / 1000.0) * Math.min(leftWidth, rightWidth);
            if (initial <= min) {
                initial = Math.nextUp(min);
            }
            if (initial >= max) {
                initial = Math.nextDown(max);
            }
        }

        if (!(min < initial && initial < max)) {
            return;
        }

        UnivariateRealFunction f = anchorFunction;

        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            // Metamorphic oracle:
            // For the same non-null function and same bracket [min, max], the overload with an
            // extra initial parameter must compute the same root as the sibling overload without it.
            // Both sides are real library calls; if either side rejects, the check is skipped.
            try {
                BisectionSolver withoutInitial = new BisectionSolver();
                double rhs = withoutInitial.solve(f, min, max);
                double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()) * 4.0;
                if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                        "[oracle:sibling-explore] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=[k=" + k
                            + ",min=" + min
                            + ",max=" + max
                            + ",initial=" + initial
                            + "] lhs=" + lhs
                            + " rhs=" + rhs);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double positiveWidth(FuzzedDataProvider data) {
        int milli = data.consumeInt(1, 1000);
        return milli / 1000.0;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement ste : trace) {
            if (ste == null) {
                continue;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return true;
        }
        String name = t.getClass().getName();
        String lower = name.toLowerCase();
        return lower.contains("invalid")
            || lower.contains("illegal")
            || lower.contains("argument")
            || lower.contains("bounds")
            || lower.contains("range");
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,061 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction anchorFunction = new SinFunction();

        final class Helpers {
            boolean stackHasPatchedSolve(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isCleanRejection(Throwable t) {
                if (t == null) {
                    return false;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                String n = t.getClass().getName();
                return n.contains("ConvergenceException")
                        || n.contains("FunctionEvaluationException")
                        || n.contains("MaxIterationsExceededException")
                        || n.contains("MathRuntimeException")
                        || n.contains("IllegalArgument");
            }

            void maybePropagateRootCause(Throwable t, boolean validByConstruction) {
                if (t instanceof NullPointerException && validByConstruction && stackHasPatchedSolve(t)) {
                    throw (NullPointerException) t;
                }
            }

            void callAndHandle(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
                try {
                    UnivariateRealSolver solver = new BisectionSolver();
                    solver.solve(f, min, max, initial);
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    maybePropagateRootCause(t, validByConstruction);
                }
            }
        }

        Helpers h = new Helpers();

        h.callAndHandle(anchorFunction, 3.0d, 3.2d, 3.1d, true);

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftWidth = 0.05d + (data.consumeInt(0, 900) / 1000.0d);
        double rightWidth = 0.05d + (data.consumeInt(0, 900) / 1000.0d);
        if (leftWidth >= Math.PI / 2.0d) {
            leftWidth = 1.0d;
        }
        if (rightWidth >= Math.PI / 2.0d) {
            rightWidth = 1.0d;
        }

        double min = root - leftWidth;
        double max = root + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            double fraction = data.consumeInt(0, 1000) / 1000.0d;
            initial = min + (max - min) * fraction;
        }

        String mode = data.consumeAsciiString(8);
        UnivariateRealFunction f = anchorFunction;
        boolean validByConstruction = true;

        if ("anchor".equals(mode) || "sin".equals(mode) || mode.length() == 0) {
            f = anchorFunction;
            validByConstruction = true;
        } else {
            f = anchorFunction;
            validByConstruction = true;
        }

        try {
            UnivariateRealSolver solverA = new BisectionSolver();
            double rWithInitial = solverA.solve(f, min, max, initial);

            UnivariateRealSolver solverB = new BisectionSolver();
            double rWithoutInitial = solverB.solve(f, min, max);

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 8.0d;

            /* Contract/oracle:
             * We construct a valid bracket [min, max] around the known zero k*pi of SinFunction,
             * so a correct solver must return that root. Also the documented same-name overloads
             * solve(f,min,max,initial) and solve(f,min,max) must agree on equivalent inputs.
             * A "fix" that merely suppresses the crash or ignores the function/arguments can return
             * a different value and violate one or both observable properties.
             */
            if (Math.abs(rWithInitial - root) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solve(f,min,max,initial) should recover constructed sin root"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", root=" + root + "}"
                        + " lhs=" + rWithInitial + " rhs=" + root);
            }
            if (Math.abs(rWithoutInitial - root) > tol) {
                throw new RuntimeException("[oracle:known-root-noinit] metamorphic violation: solve(f,min,max) should recover constructed sin root"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", root=" + root + "}"
                        + " lhs=" + rWithoutInitial + " rhs=" + root);
            }
            if (Math.abs(rWithInitial - rWithoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagree"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", root=" + root + "}"
                        + " lhs=" + rWithInitial + " rhs=" + rWithoutInitial);
            }
        } catch (Throwable t) {
            if (h.isCleanRejection(t)) {
                return;
            }
            h.maybePropagateRootCause(t, validByConstruction);
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,268 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction f = new SinFunction();

        {
            double min = 3.0;
            double max = 3.2;
            double initial = 3.1;
            try {
                BisectionSolver solverWithInitial = new BisectionSolver();
                double r1 = solverWithInitial.solve(f, min, max, initial);

                BisectionSolver solverWithoutInitial = new BisectionSolver();
                double r2 = solverWithoutInitial.solve(f, min, max);

                double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());

                if (Double.isNaN(r1) || Double.isInfinite(r1)) {
                    throw new RuntimeException("[oracle:anchor-finite] metamorphic violation: anchored solve returned non-finite result input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
                }

                if (Math.abs(r1 - Math.PI) > solverWithInitial.getAbsoluteAccuracy()) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: anchored solve should recover the known root pi for SinFunction input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + Math.PI);
                }

                /* Contract/oracle: the documented same-name solve overloads are intended to agree on equivalent inputs;
                   adding an initial guess must not change the root found for the same valid function and bracketing interval. */
                if (Math.abs(r1 - r2) > tol) {
                    throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2);
                }
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                            && "solve".equals(st[i].getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
            } catch (Exception t) {
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        if (k == 0) {
            k = 1;
        }

        double root = k * Math.PI;
        double width = data.consumeInt(1, 1000) / 1000.0;
        if (width <= 0.0) {
            width = 0.1;
        }
        if (width > 1.0) {
            width = 1.0;
        }

        double min = root - width;
        double max = root + width;
        if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        int initialMode = data.consumeInt(0, 4);
        double initial;
        if (initialMode == 0) {
            initial = root;
        } else if (initialMode == 1) {
            initial = min + (max - min) * 0.25;
        } else if (initialMode == 2) {
            initial = min + (max - min) * 0.5;
        } else if (initialMode == 3) {
            initial = min + (max - min) * 0.75;
        } else {
            double fraction = data.consumeInt(0, 1000) / 1000.0;
            initial = min + (max - min) * fraction;
        }

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double r1 = solverWithInitial.solve(f, min, max, initial);

            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double r2 = solverWithoutInitial.solve(f, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());

            /* Contract/oracle: these inputs are valid by construction for SinFunction:
               f is non-null and [min,max] brackets the known root k*pi.
               Therefore a correct implementation should solve them, and the returned root should recover the known value we constructed. */
            if (Double.isNaN(r1) || Double.isInfinite(r1)) {
                throw new RuntimeException("[oracle:finite] metamorphic violation: solve returned non-finite result input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
            }

            if (Math.abs(r1 - root) > solverWithInitial.getAbsoluteAccuracy()) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed SinFunction interval around known root k*pi was not recovered input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + root);
            }

            /* Contract/oracle: same-name overload agreement on equivalent valid inputs. A patch that merely suppresses the crash
               but skips installing/using the function will make solve(f,min,max,initial) diverge from solve(f,min,max). */
            if (Math.abs(r1 - r2) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
            }
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                        && "solve".equals(st[i].getMethodName())) {
                    throughSolve = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && throughSolve) {
                throw t;
            }
        } catch (Exception t) {
            return;
        }
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,515 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runCase(sin, 3.0, 3.2, 3.1, true);

        // EXPLORE: the buggy overload NPEs because it forgets to bind the supplied function
        // before delegating. Generate many valid-by-construction bracketed intervals for a real
        // library function so a correct implementation is obligated to handle them.
        int cases = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-50, 50);
            double root = k * Math.PI;

            double left = 0.01 + (data.consumeInt(0, 990) / 1000.0);
            double right = 0.01 + (data.consumeInt(0, 990) / 1000.0);

            // Keep the interval narrow enough to bracket exactly one known root of sin(x).
            if (left > 1.0) {
                left = 1.0;
            }
            if (right > 1.0) {
                right = 1.0;
            }

            double min = root - left;
            double max = root + right;

            double fraction = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * fraction;

            runCase(sin, min, max, initial, true);
        }
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double r1 = solverWithInitial.solve(f, min, max, initial);

            // Documented same-name overload agreement: solve(f, min, max, initial) and
            // solve(f, min, max) must agree on equivalent inputs. A "fix" that merely avoids the
            // crash but drops the supplied function or changes behavior would violate this.
            double r2;
            try {
                BisectionSolver solverWithoutInitial = new BisectionSolver();
                r2 = solverWithoutInitial.solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), 1.0e-12);
            if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name.contains("Convergence") || name.contains("Invalid") || name.contains("NoBracketing")) {
            return true;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,658 chars)</summary>

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
        exerciseAndCheck(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction inputs with the same root-cause property:
        // a non-null function and a bracketing interval around PI for sin(x), so a correct
        // implementation is obligated to handle them and the patched line is exercised.
        int variants = 1 + Math.abs(data.consumeInt(0, 8));
        for (int i = 0; i < variants; i++) {
            double left = 0.000001 + (data.consumeInt(1, 100000) / 100000.0);
            double right = 0.000001 + (data.consumeInt(1, 100000) / 100000.0);
            double min = Math.PI - left;
            double max = Math.PI + right;
            if (!(min < Math.PI && Math.PI < max)) {
                continue;
            }

            double fraction = data.consumeInt(0, 1000000) / 1000000.0;
            double initial = min + (max - min) * fraction;
            if (data.consumeBoolean()) {
                initial = Math.PI;
            }

            exerciseAndCheck(f, min, max, initial, false);
        }
    }

    private static void exerciseAndCheck(UnivariateRealFunction f, double min, double max, double initial, boolean rethrowRootCause) {
        if (f == null) {
            return;
        }
        if (!(min < max)) {
            return;
        }
        if (!(min <= initial && initial <= max)) {
            return;
        }
        if (!(min < Math.PI && Math.PI < max)) {
            return;
        }

        Double withInitial = null;
        try {
            BisectionSolver solver = new BisectionSolver();
            withInitial = solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (rethrowRootCause && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        Double withoutInitial = null;
        try {
            BisectionSolver solver = new BisectionSolver();
            withoutInitial = solver.solve(f, min, max);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        }

        if (withInitial == null || withoutInitial == null) {
            return;
        }

        double tolerance = new BisectionSolver().getAbsoluteAccuracy();

        // Contract used for this oracle:
        // the same-name overloads solve(f, min, max, initial) and solve(f, min, max)
        // are documented to agree where their docs match. For BisectionSolver, the initial
        // guess is not part of the bisection computation, so both real library calls must
        // return the same root on the same valid bracketing interval. A "fix" that merely
        // suppresses the crash but skips using f correctly would violate this agreement.
        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tolerance) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + withInitial + " rhs=" + withoutInitial
            );
        }

        if (Math.abs(withInitial.doubleValue() - Math.PI) > tolerance) {
            throw new RuntimeException(
                "[oracle:pi-root] metamorphic violation: sin(x) on an interval bracketing PI must solve to PI"
                    + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                    + " lhs=" + withInitial + " rhs=" + Math.PI
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (e != null
                    && "org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
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
                || name.contains("EvaluationException")
                || name.contains("Argument")
                || name.contains("Invalid");
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,689 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle: solve(...) returns a zero of f in the provided interval.
            // For sin(x) over [3.0, 3.2], the only intended root is pi, so a correct
            // implementation must return a value close to Math.PI.
            double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-8);
            if (Double.isNaN(result) || Double.isInfinite(result) || Math.abs(result - Math.PI) > tol * 8.0) {
                throw new RuntimeException("[oracle:anchor-root] metamorphic violation: solver did not recover the known sin root input=[3.0,3.2,3.1] result=" + result + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                            && "solve".equals(st[i].getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughSolve) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        } catch (IllegalArgumentException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        } catch (Throwable t) {
            return;
        }

        // EXPLORE: root-cause property is a valid, non-null function passed via
        // solve(f, min, max, initial), where the buggy implementation ignores f and
        // delegates to solve(min, max). We build valid-by-construction intervals around
        // a known sin root k*pi, with min < initial < max and opposite signs at the ends.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        int leftMillis = data.consumeInt(1, 1000);
        int rightMillis = data.consumeInt(1, 1000);
        double left = leftMillis / 1000.0;
        double right = rightMillis / 1000.0;

        double min = root - left;
        double max = root + right;
        double initial;
        if (data.consumeBoolean()) {
            initial = root;
        } else {
            int side = data.consumeBoolean() ? 1 : -1;
            int offsetMillis = data.consumeInt(0, side > 0 ? rightMillis : leftMillis);
            initial = root + side * (offsetMillis / 1000.0);
            if (initial < min) {
                initial = min;
            }
            if (initial > max) {
                initial = max;
            }
        }

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, min, max, initial);

            // Mandatory post-condition/metamorphic check:
            // documented same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // should agree on equivalent valid inputs; the initial guess should not change
            // the root being solved for on this bracketed sin interval.
            BisectionSolver solverNoInitial = new BisectionSolver();
            double noInitial = solverNoInitial.solve(f, min, max);

            double tol = Math.max(
                    Math.max(solverWithInitial.getAbsoluteAccuracy(), solverNoInitial.getAbsoluteAccuracy()),
                    1.0e-8);

            if (Double.isNaN(withInitial) || Double.isInfinite(withInitial)) {
                throw new RuntimeException("[oracle:finite] metamorphic violation: solve(f,min,max,initial) returned non-finite result input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial);
            }
            if (Double.isNaN(noInitial) || Double.isInfinite(noInitial)) {
                throw new RuntimeException("[oracle:finite-sibling] metamorphic violation: solve(f,min,max) returned non-finite result input=[" + min + "," + max + "] rhs=" + noInitial);
            }

            if (Math.abs(withInitial - root) > tol * 8.0) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solver did not recover constructed sin root input=[" + min + "," + max + "," + initial + "] result=" + withInitial + " expected=" + root + " tol=" + tol);
            }

            if (Math.abs(withInitial - noInitial) > tol * 8.0) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent solve overloads disagree input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + noInitial + " tol=" + tol);
            }
        } catch (RuntimeException t) {
            boolean throughSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(st[i].getClassName())
                            && "solve".equals(st[i].getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughSolve) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        } catch (IllegalArgumentException t) {
            return;
        } catch (FunctionEvaluationException t) {
            return;
        } catch (MaxIterationsExceededException t) {
            return;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **23/23 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov; 23 raw fuzz log(s)**
<details><summary>▸ detail (~11,559 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 490}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 488}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 491}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 483}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 490}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 483}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 489}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 560}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 487}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_008', 'attempt_009', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_008', 'attempt_009', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/007_patch1-Math-70-Arja_c_rep001/harness_src/attempt_029.java']}

</details>
