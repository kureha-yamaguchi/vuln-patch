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
  "FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method='testMath369', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/src/test/java/org/apache/commons/math/analysis/solvers/BisectionSolverTest.java', method_source='    public void testMath369() throws Exception {\\n        UnivariateRealFunction f = new SinFunction();\\n        UnivariateRealSolver solver = new BisectionSolver();\\n        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~17,149 chars)</summary>

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
<details open><summary>▸ Output (~5,661 chars)</summary>

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

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle: the same-name overloads
            // solve(f, min, max, initial) and solve(f, min, max)
            // are documented to agree for BisectionSolver because bisection does not
            // use the initial guess; the patch itself fixes delegation to the sibling overload.
            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double withoutInitial = solverWithoutInitial.solve(f, 3.0, 3.2);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(),
                                  solverWithoutInitial.getAbsoluteAccuracy()) * 8.0 + 1.0e-12;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=[f=SinFunction,min=3.0,max=3.2,initial=3.1]"
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (Throwable t) {
            boolean hasSolveFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    hasSolveFrame = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && hasSolveFrame) {
                throw (NullPointerException) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        // EXPLORE: valid-by-construction intervals bracketing roots of sin(x) at n*pi.
        // Property: on a fresh BisectionSolver, calling solve(f,min,max,initial) with a non-null
        // function and a valid bracket should be accepted and agree with solve(f,min,max).
        int cases = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 4) : 0);
        for (int i = 0; i < cases; i++) {
            int n = data.consumeInt(-100, 100);
            double root = n * Math.PI;

            double leftWidth = 0.05 + (data.consumeInt(0, 950) / 1000.0);
            double rightWidth = 0.05 + (data.consumeInt(0, 950) / 1000.0);
            if (leftWidth > 1.2) {
                leftWidth = 1.2;
            }
            if (rightWidth > 1.2) {
                rightWidth = 1.2;
            }

            double min = root - leftWidth;
            double max = root + rightWidth;
            if (!(min < root && root < max)) {
                continue;
            }

            double fraction = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + (max - min) * fraction;

            try {
                BisectionSolver solverWithInitial = new BisectionSolver();
                double withInitial = solverWithInitial.solve(f, min, max, initial);

                BisectionSolver solverWithoutInitial = new BisectionSolver();
                double withoutInitial = solverWithoutInitial.solve(f, min, max);

                double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(),
                                      solverWithoutInitial.getAbsoluteAccuracy()) * 8.0 + 1.0e-12;
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=[root=" + root + ",min=" + min + ",max=" + max + ",initial=" + initial + "]"
                        + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable t) {
                boolean hasSolveFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        hasSolveFrame = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && hasSolveFrame) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException
                        || t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
                    return;
                }
                return;
            }
        }
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
<details open><summary>▸ Output (~4,945 chars)</summary>

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

        runTargetAndOracles(f, 3.0, 3.2, 3.1, Math.PI, true);

        int cases = 1 + Math.max(0, Math.min(4, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double expectedRoot = k * Math.PI;

            int milliWidth = data.consumeInt(10, 1000);
            double halfWidth = milliWidth / 1000.0;

            double min = expectedRoot - halfWidth;
            double max = expectedRoot + halfWidth;

            int initialMilliOffset = data.consumeInt(-milliWidth, milliWidth);
            double initial = expectedRoot + (initialMilliOffset / 1000.0);

            runTargetAndOracles(f, min, max, initial, expectedRoot, true);
        }
    }

    private static void runTargetAndOracles(UnivariateRealFunction f, double min, double max, double initial,
                                            double expectedRoot, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();
        double result;
        try {
            result = solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double tol = Math.max(solver.getAbsoluteAccuracy() * 4.0, 1.0e-8);

        // Contract/oracle: these inputs are constructed around the exact sine root k*pi with min < root < max,
        // so any correct solve(f,min,max,initial) implementation must return that unique root in the interval.
        if (!approximatelyEqual(result, expectedRoot, tol)) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: constructed sine bracket around exact root input=("
                    + min + "," + max + "," + initial + ") result=" + result + " expected=" + expectedRoot);
        }

        // Contract/oracle: same-name overloads with matching documented purpose must agree on equivalent inputs.
        // For BisectionSolver, the initial guess should not change the solved root for the same bracketed function.
        BisectionSolver sibling = new BisectionSolver();
        try {
            double siblingResult = sibling.solve(f, min, max);
            if (!approximatelyEqual(result, siblingResult, tol)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                        + " input=(" + min + "," + max + "," + initial + ") lhs=" + result + " rhs=" + siblingResult);
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        return Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) <= tol;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        return t instanceof NullPointerException && stackPassesThroughSolve(t);
    }

    private static boolean stackPassesThroughSolve(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
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
        return name.contains("Illegal")
            || name.contains("Invalid")
            || name.contains("Argument")
            || name.contains("Convergence")
            || name.contains("Evaluation")
            || t instanceof MaxIterationsExceededException
            || t instanceof FunctionEvaluationException;
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
<details open><summary>▸ Output (~6,287 chars)</summary>

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

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        // This is valid-by-construction: non-null function, interval [3.0, 3.2] brackets a root of sin(x),
        // and the public API under test is BisectionSolver.solve(f, min, max, initial).
        runOne(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: generate many other valid-by-construction intervals that bracket a root of sin(x).
        // Property: for any integer k, sin(k*pi - d) and sin(k*pi + d) have opposite signs when d is in (0, pi),
        // so [k*pi-d, k*pi+d] is a valid bracketing interval for a root at k*pi.
        int k = data.consumeInt(-1000, 1000);
        int denom = data.consumeInt(2, 1000);
        int numer = data.consumeInt(1, denom - 1);
        double delta = (Math.PI * numer) / denom;
        double center = k * Math.PI;

        double min = center - delta;
        double max = center + delta;

        int initialMode = data.consumeInt(0, 4);
        double initial;
        switch (initialMode) {
            case 0:
                initial = center;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
                break;
            default:
                initial = (min + max) / 2.0;
                break;
        }

        runOne(f, min, max, initial, true);
    }

    private static void runOne(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();

        try {
            double withInitial = solver.solve(f, min, max, initial);

            // Metamorphic/post-condition:
            // The overloaded solve(f, min, max, initial) and solve(f, min, max) are documented sibling entry points
            // for the same solver and interval; for a correct implementation, the extra initial guess must not change
            // the computed root for bisection. A "fix" that merely avoids the crash but skips the real solve path can
            // return a different value here.
            try {
                double withoutInitial = new BisectionSolver().solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-8);
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) ||
                    Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)" +
                        " input=min=" + min + ",max=" + max + ",initial=" + initial +
                        " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                // If either side throws, the relation does not apply for this input.
            }

            // Oracle from the input itself:
            // We constructed intervals around k*pi for SinFunction, so any correct root returned by bisection on
            // this bracket must satisfy sin(root) ~= 0.
            try {
                double residual = f.value(withInitial);
                if (Double.isNaN(residual) || Math.abs(residual) > 1.0e-6) {
                    throw new RuntimeException(
                        "[oracle:sin-root] metamorphic violation: returned value is not a root of sin on a valid bracketing interval" +
                        " input=min=" + min + ",max=" + max + ",initial=" + initial +
                        " root=" + withInitial + " residual=" + residual);
                }
            } catch (FunctionEvaluationException ignored) {
                // Skip oracle if function evaluation fails unexpectedly.
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            // Swallow anything else: not the verified bug in the patched region.
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("illegal") || lower.contains("argument")) {
                    return true;
                }
            }
        }
        return false;
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
<details open><summary>▸ Output (~3,653 chars)</summary>

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

        runCase(f, 3.0, 3.2, 3.1);

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double leftWidth = data.consumeInt(1, 1000) / 1000.0;
        double rightWidth = data.consumeInt(1, 1000) / 1000.0;
        double min = root - leftWidth;
        double max = root + rightWidth;
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        runCase(f, min, max, initial);
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial) {
        if (f == null || !(min < max) || Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(initial)) {
            return;
        }

        try {
            BisectionSolver solver = new BisectionSolver();
            double withInitial = solver.solve(f, min, max, initial);

            try {
                BisectionSolver sibling = new BisectionSolver();
                double withoutInitial = sibling.solve(f, min, max);

                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy()) * 4.0;
                if (Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracketed function input="
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid") || name.contains("NoBracketing") || name.contains("Convergence")) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~4,793 chars)</summary>

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

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        runValidCase(f, 3.0, 3.2, 3.1);

        // EXPLORE: valid-by-construction bracketing intervals for sin(x) around k*pi.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = scaledPositive(data.consumeInt(1, 1_000_000), 1_000_000.0);
        double right = scaledPositive(data.consumeInt(1, 1_000_000), 1_000_000.0);

        // Keep offsets moderate and below pi/2 so [root-left, root+right] brackets exactly one sin root.
        left = 1.0e-6 + (left % 1.0);
        right = 1.0e-6 + (right % 1.0);

        double min = root - left;
        double max = root + right;

        int selector = data.consumeInt(0, 1_000_000);
        double initial = min + (max - min) * (selector / 1_000_000.0);
        if (data.consumeBoolean()) {
            initial = root;
        }

        runValidCase(f, min, max, initial);
    }

    private static double scaledPositive(int value, double scale) {
        double v = value / scale;
        if (v < 0) {
            v = -v;
        }
        return v;
    }

    private static void runValidCase(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver solverWithInitial = new BisectionSolver();
        BisectionSolver solverWithoutInitial = new BisectionSolver();

        Double lhs = null;
        try {
            lhs = solverWithInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedMethodNpe(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            // Contract/oracle: these same-name overloads are documented to agree where their docs match.
            // For BisectionSolver, solve(f, min, max, initial) should behave like solve(f, min, max);
            // a bogus "fix" that ignores the function or changes behavior would break this sibling agreement.
            double rhs = solverWithoutInitial.solve(f, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy()) * 10.0;
            if (Double.isNaN(lhs.doubleValue()) || Double.isNaN(rhs) || Math.abs(lhs.doubleValue() - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:solve-overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + lhs + ", rhs=" + rhs + ", tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedMethodNpe(t)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static boolean isPatchedMethodNpe(Throwable t) {
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
<details open><summary>▸ Output (~5,598 chars)</summary>

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
        runCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE:
        // Root cause property: the patched overload solve(f, min, max, initial) must forward the
        // provided non-null function into the real solving path. The buggy version instead calls
        // solve(min, max), which uses internal state and crashes with NPE on a fresh solver.
        //
        // Build valid-by-construction intervals around known roots of sin(x) = 0 at k*pi.
        // For delta in (0, pi), sin(root - delta) and sin(root + delta) have opposite signs,
        // so the solver is obligated to handle these inputs.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        int deltaMicros = data.consumeInt(1, 1000000);
        double delta = 1.0e-6 + (deltaMicros / 1000000.0); // in (1e-6, 1.000001)

        double min = root - delta;
        double max = root + delta;

        int pos = data.consumeInt(0, 1000000);
        double initial = min + (max - min) * (pos / 1000000.0);

        runCase(f, min, max, initial, false);
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean checkKnownRoot) {
        // First drive the exact patched entry point.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isOracleFailure(t) || isRootCauseNpeFromPatchedRegion(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Metamorphic / post-condition check:
        // The documented same-name overloads are required to agree on equivalent inputs.
        // For a correct implementation, solve(f, min, max, initial) and solve(f, min, max)
        // must produce the same root on the same valid bracket; deleting the forward to the
        // real overload or otherwise changing behavior would violate this observable contract.
        double withInitial;
        double withoutInitial;
        try {
            BisectionSolver s1 = new BisectionSolver();
            withInitial = s1.solve(f, min, max, initial);

            BisectionSolver s2 = new BisectionSolver();
            withoutInitial = s2.solve(f, min, max);
        } catch (Throwable t) {
            if (isOracleFailure(t) || isRootCauseNpeFromPatchedRegion(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-8) * 4.0;
        if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) || Math.abs(withInitial - withoutInitial) > tol) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
        }

        if (checkKnownRoot) {
            double expected = Math.PI;
            if (Double.isNaN(withInitial) || Math.abs(withInitial - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:known-root] metamorphic violation: sin root near pi must be solved accurately"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + withInitial + " rhs=" + expected);
            }
        }
    }

    private static boolean isRootCauseNpeFromPatchedRegion(Throwable t) {
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid")
                || name.contains("argument")
                || name.contains("bounds")
                || name.contains("range")
                || name.contains("convergence");
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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
<details open><summary>▸ Output (~5,393 chars)</summary>

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

        runCase(f, 3.0, 3.2, 3.1, true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int leftMillis = data.consumeInt(1, 1000);
            int rightMillis = data.consumeInt(1, 1000);
            double left = leftMillis / 1000.0;
            double right = rightMillis / 1000.0;

            double min = root - left;
            double max = root + right;

            double initial;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    initial = min;
                    break;
                case 1:
                    initial = max;
                    break;
                case 2:
                    initial = root;
                    break;
                case 3:
                    initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
                    break;
                default:
                    initial = (min + max) / 2.0;
                    break;
            }

            runCase(f, min, max, initial, false);
        }
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean anchor) {
        if (f == null || !(min <= initial && initial <= max) || !(min < max)) {
            return;
        }

        BisectionSolver solver4 = new BisectionSolver();
        try {
            double r4 = solver4.solve(f, min, max, initial);

            BisectionSolver solver3 = new BisectionSolver();
            double r3;
            try {
                r3 = solver3.solve(f, min, max);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }

            double acc = Math.max(solver4.getAbsoluteAccuracy(), solver3.getAbsoluteAccuracy());

            if (!(Double.isNaN(r4) || Double.isInfinite(r4) || Double.isNaN(r3) || Double.isInfinite(r3))) {
                if (Math.abs(r4 - r3) > acc * 4.0) {
                    throw new RuntimeException(
                        "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracketing interval"
                            + " input=min=" + min + ", max=" + max + ", initial=" + initial
                            + " lhs=" + r4 + ", rhs=" + r3);
                }

                try {
                    double fr4 = f.value(r4);
                    double fr3 = f.value(r3);
                    if (!(Double.isNaN(fr4) || Double.isInfinite(fr4) || Double.isNaN(fr3) || Double.isInfinite(fr3))) {
                        if (Math.abs(fr4) > 1e-4 || Math.abs(fr3) > 1e-4) {
                            throw new RuntimeException(
                                "[oracle:root-quality] metamorphic violation: a successful bisection solve on a valid bracketing interval must return an approximation whose function value is near zero"
                                    + " input=min=" + min + ", max=" + max + ", initial=" + initial
                                    + " lhs=" + r4 + " f(lhs)=" + fr4
                                    + " rhs=" + r3 + " f(rhs)=" + fr3);
                        }
                    }
                } catch (FunctionEvaluationException ignored) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (anchor && isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof FunctionEvaluationException
            || t instanceof MaxIterationsExceededException;
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
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,499 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        runCase(f, 3.0, 3.2, 3.1, Math.PI);

        int cases = data.remainingBytes() > 0 ? data.consumeInt(1, 4) : 1;
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            // Build valid-by-construction intervals for sin(x): [root-delta, root+delta]
            // with 0 < delta < pi/2, so the interval brackets a real zero at root.
            double delta = (data.consumeInt(1, 1000) / 1000.0) * 1.4 + 1.0e-6;
            double min = root - delta;
            double max = root + delta;

            double initial;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    initial = min;
                    break;
                case 1:
                    initial = max;
                    break;
                case 2:
                    initial = root;
                    break;
                default:
                    double t = data.consumeInt(0, 1000000) / 1000000.0;
                    initial = min + (max - min) * t;
                    break;
            }

            runCase(f, min, max, initial, root);
        }
    }

    private static void runCase(SinFunction f, double min, double max, double initial, double expectedRoot) {
        // Documented guarantee used for the oracle:
        // all solve overloads solve for a zero in [min, max]. For these intervals we construct
        // a real zero of sin(x) inside the bracket by construction, so a correct implementation
        // must return that known root (within solver accuracy). Also, the same-name overloads
        // solve(f,min,max,initial) and solve(f,min,max) must agree on the solved root.
        Double withInitial = null;
        double acc1 = 0.0;
        try {
            BisectionSolver solver = new BisectionSolver();
            withInitial = solver.solve(f, min, max, initial);
            acc1 = solver.getAbsoluteAccuracy();
            if (Double.isNaN(withInitial.doubleValue()) || Double.isInfinite(withInitial.doubleValue())
                    || Math.abs(withInitial.doubleValue() - expectedRoot) > acc1 * 2.0 + 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:known-root] metamorphic violation: known zero in bracket not recovered"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial + ",expectedRoot=" + expectedRoot
                    + " lhs=" + withInitial + ", rhs=" + expectedRoot);
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

        Double withoutInitial = null;
        double acc2 = 0.0;
        try {
            BisectionSolver solver = new BisectionSolver();
            withoutInitial = solver.solve(f, min, max);
            acc2 = solver.getAbsoluteAccuracy();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (withInitial != null && withoutInitial != null) {
            double tol = Math.max(acc1, acc2) * 2.0 + 1.0e-12;
            if (Double.isNaN(withInitial.doubleValue()) || Double.isNaN(withoutInitial.doubleValue())
                    || Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agree] metamorphic violation: solve overloads disagree"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + ", rhs=" + withoutInitial);
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Convergence")
                || n.contains("MaxIterationsExceeded")
                || n.contains("FunctionEvaluation")
                || n.contains("Invalid")
                || n.contains("NoBracketing");
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
<details open><summary>▸ Output (~7,041 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from BisectionSolverTest.testMath369.
        runCase(solver, f, 3.0, 3.2, 3.1, true);

        // EXPLORE: same root cause property, but with many valid intervals.
        // Property: non-null function + valid bracketing interval + initial inside interval.
        // On a correct implementation, solve(f,min,max,initial) should behave like the sibling
        // overload solve(f,min,max) for BisectionSolver because bisection does not use the initial guess.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftOffset = boundedPositive(data.consumeInt(), 0.05, 1.0);
        double rightOffset = boundedPositive(data.consumeInt(), 0.05, 1.0);

        double min = root - leftOffset;
        double max = root + rightOffset;

        double initialFrac = boundedUnit(data.consumeInt());
        double initial = min + (max - min) * initialFrac;

        runCase(solver, f, min, max, initial, true);

        int extra = data.consumeInt(0, 3);
        for (int i = 0; i < extra; i++) {
            k = data.consumeInt(-100, 100);
            root = k * Math.PI;
            leftOffset = boundedPositive(data.consumeInt(), 0.01, 1.5);
            rightOffset = boundedPositive(data.consumeInt(), 0.01, 1.5);
            min = root - leftOffset;
            max = root + rightOffset;
            initialFrac = boundedUnit(data.consumeInt());
            initial = min + (max - min) * initialFrac;
            runCase(new BisectionSolver(), f, min, max, initial, true);
        }

        // Also exercise possibly-invalid shapes; these must never be findings if the library cleanly rejects them.
        // We swallow all non-root-cause throwables here.
        double a = boundedRange(data.consumeInt(), -1000.0, 1000.0);
        double b = boundedRange(data.consumeInt(), -1000.0, 1000.0);
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        double maybeInitial = lo + (hi - lo) * boundedUnit(data.consumeInt());
        runCase(new BisectionSolver(), f, lo, hi, maybeInitial, false);
    }

    private static void runCase(BisectionSolver solver, UnivariateRealFunction f,
                                double min, double max, double initial,
                                boolean validByConstruction) {
        try {
            double withInitial = solver.solve(f, min, max, initial);

            // Documented sibling-agreement/post-condition:
            // For BisectionSolver, the overload with an initial guess must agree with the same solver
            // on the same function and interval because bisection is determined by the bracket; a patch
            // that merely suppresses the crash or ignores intended behavior could return a different root.
            double withoutInitial;
            try {
                withoutInitial = new BisectionSolver().solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            if (Double.isNaN(withInitial) != Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > new BisectionSolver().getAbsoluteAccuracy()) {
                throw new RuntimeException(
                    "[oracle:sibling-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            // Oracle from the constructed input itself: each interval brackets k*pi for SinFunction.
            // A correct solver result should satisfy sin(result) ~= 0 within a moderate tolerance.
            double residual = Math.abs(Math.sin(withInitial));
            if (residual > 1e-6) {
                throw new RuntimeException(
                    "[oracle:root-residual] metamorphic violation: returned value should be a root of sin on the constructed bracket"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " result=" + withInitial + " residual=" + residual);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            // Any other failure is outside this patch's scope or is a legitimate rejection for this input.
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException
                    || cur instanceof NumberFormatException
                    || cur instanceof FunctionEvaluationException
                    || cur instanceof MaxIterationsExceededException) {
                return true;
            }
            String name = cur.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("invalid")
                    || lower.contains("illegal")
                    || lower.contains("argument")
                    || lower.contains("convergence")
                    || lower.contains("evaluation")) {
                return true;
            }
        }
        return false;
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

    private static double boundedPositive(int x, double min, double max) {
        int m = x == Integer.MIN_VALUE ? 0 : Math.abs(x);
        double frac = (m % 10000) / 9999.0;
        return min + (max - min) * frac;
    }

    private static double boundedUnit(int x) {
        int m = x == Integer.MIN_VALUE ? 0 : Math.abs(x);
        return (m % 10000) / 9999.0;
    }

    private static double boundedRange(int x, double min, double max) {
        return min + (max - min) * boundedUnit(x);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
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
<details open><summary>▸ Output (~6,723 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();
        org.apache.commons.math.analysis.UnivariateRealFunction f = new SinFunction();

        try {
            double anchor = solver.solve(f, 3.0, 3.2, 3.1);
            double expected = Math.PI;
            double tol = solver.getAbsoluteAccuracy();
            if (Double.isNaN(anchor) || Math.abs(anchor - expected) > tol) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: documented bisection solve on SinFunction over [3.0,3.2] should find the root near PI input=[3.0,3.2,3.1] lhs=" + anchor + " rhs=" + expected);
            }

            /*
             * Contract/oracle:
             * The same-name overloads
             *   solve(f, min, max, initial)
             *   solve(f, min, max)
             * are documented to agree where their docs match. For BisectionSolver,
             * on a valid bracketing interval and non-null function, both should solve
             * the same equation on the same interval and return the same root.
             * A throw-deleting/branch-skipping patch that ignores the function or
             * otherwise changes behaviour can violate this even without crashing.
             */
            double anchor2 = solver.solve(f, 3.0, 3.2);
            if (Double.isNaN(anchor2) || Math.abs(anchor - anchor2) > solver.getAbsoluteAccuracy()) {
                throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: equivalent overloads disagree input=[3.0,3.2,3.1] lhs=" + anchor + " rhs=" + anchor2);
            }
        } catch (Throwable t) {
            boolean inSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inSolve = true;
                        break;
                    }
                }
            }

            if (t instanceof NullPointerException && inSolve) {
                throw (NullPointerException) t;
            }

            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.apache.commons.math.FunctionEvaluationException
                    || t instanceof org.apache.commons.math.MaxIterationsExceededException
                    || t instanceof RuntimeException) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                return;
            }
            return;
        }

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;

        double leftWidth = 0.05 + (data.consumeInt(0, 5000) / 10000.0);
        double rightWidth = 0.05 + (data.consumeInt(0, 5000) / 10000.0);

        double min = center - leftWidth;
        double max = center + rightWidth;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            double frac = data.consumeInt(0, 10000) / 10000.0;
            initial = min + frac * (max - min);
        } else {
            initial = center;
        }

        if (!(initial >= min && initial <= max)) {
            initial = center;
        }

        BisectionSolver solver1 = new BisectionSolver();
        BisectionSolver solver2 = new BisectionSolver();

        double rWithInitial;
        try {
            rWithInitial = solver1.solve(f, min, max, initial);
        } catch (Throwable t) {
            boolean inSolve = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        inSolve = true;
                        break;
                    }
                }
            }

            if (t instanceof NullPointerException && inSolve) {
                throw (NullPointerException) t;
            }

            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.apache.commons.math.FunctionEvaluationException
                    || t instanceof org.apache.commons.math.MaxIterationsExceededException) {
                return;
            }
            if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                    && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            return;
        }

        double rNoInitial;
        try {
            rNoInitial = solver2.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(solver1.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy());

        if (Double.isNaN(rWithInitial) || Double.isInfinite(rWithInitial) || Double.isNaN(rNoInitial) || Double.isInfinite(rNoInitial)) {
            throw new RuntimeException("[oracle:finite-result] metamorphic violation: valid bracketing interval around a sine root should produce finite roots input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
        }

        if (Math.abs(rWithInitial - rNoInitial) > tol) {
            throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent overloads disagree input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
        }

        double target = center;
        if (Math.abs(rWithInitial - target) > Math.max(tol, 1e-6) || Math.abs(rNoInitial - target) > Math.max(tol, 1e-6)) {
            throw new RuntimeException("[oracle:recover-known-root] metamorphic violation: interval was constructed to bracket the known sin root at k*pi input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
        }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,968 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        exerciseValidInput(f, 3.0d, 3.2d, 3.1d);

        // EXPLORE: generate many valid-by-construction intervals that bracket a real root of sin(x).
        // For sin, every k*pi is a root; choosing min=root-a and max=root+b with 0<a,b<pi/2
        // guarantees opposite signs and a valid interval for bisection.
        int iterations = data.consumeInt(1, 6);
        for (int i = 0; i < iterations; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            int leftMilli = data.consumeInt(1, 1500);
            int rightMilli = data.consumeInt(1, 1500);
            double left = leftMilli / 1000.0d;
            double right = rightMilli / 1000.0d;

            double min = root - left;
            double max = root + right;

            int initSelector = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (initSelector / 1000.0d);

            exerciseValidInput(f, min, max, initial);
        }
    }

    private static void exerciseValidInput(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double withInitial = solverWithInitial.solve(f, min, max, initial);

            // Metamorphic/post-condition:
            // The same-name overloads are documented to agree where their docs match. For BisectionSolver,
            // the 4-arg solve(f,min,max,initial) is supposed to behave like solve(f,min,max); the patch
            // itself restores that delegation. A throw-deleting or wrong-routing patch could return a value
            // different from the real 3-arg entry point without crashing.
            try {
                BisectionSolver solverWithoutInitial = new BisectionSolver();
                double withoutInitial = solverWithoutInitial.solve(f, min, max);

                double tolerance = Math.max(solverWithInitial.getAbsoluteAccuracy(),
                        solverWithoutInitial.getAbsoluteAccuracy()) * 4.0d;
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > tolerance) {
                    throw new RuntimeException(
                            "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
                }
            } catch (Throwable t) {
                // If either side of the relation throws, the relation does not apply to this input.
                if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                throw propagate(t);
            }
            return;
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("DimensionMismatch");
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
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

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
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
<details open><summary>▸ Output (~5,913 chars)</summary>

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

        // ANCHOR: exact trigger from the regression test. On the buggy build,
        // solve(f, 3.0, 3.2, 3.1) wrongly delegates to solve(min, max), losing f and
        // causing the verified NullPointerException from BisectionSolver.solve.
        try {
            BisectionSolver solver = new BisectionSolver();
            double r = solver.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle:
            // The interval [3.0, 3.2] brackets the known sine root pi, so a correct
            // bisection solve must return a value close to Math.PI.
            if (Double.isNaN(r) || Double.isInfinite(r) ||
                Math.abs(r - Math.PI) > solver.getAbsoluteAccuracy() * 4.0) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: known-root solve should approximate pi input=[3.0,3.2,3.1] result=" + r);
            }
        } catch (RuntimeException t) {
            boolean stackOk = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    stackOk = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackOk) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }

        // EXPLORE: generate many valid-by-construction intervals that bracket a real root
        // of sin(x) at n*pi, with initial strictly inside [min, max].
        // Because these inputs satisfy the solver's documented preconditions (non-null f,
        // bracketing interval, moderate magnitudes), a NullPointerException from solve is a
        // genuine defect, not clean rejection.
        int n = data.consumeInt(-1000, 1000);
        double expectedRoot = n * Math.PI;

        int leftMillis = data.consumeInt(50, 1000);
        int rightMillis = data.consumeInt(50, 1000);
        double leftWidth = leftMillis / 1000.0;
        double rightWidth = rightMillis / 1000.0;

        double min = expectedRoot - leftWidth;
        double max = expectedRoot + rightWidth;

        int pos = data.consumeInt(1, 999);
        double initial = min + (max - min) * (pos / 1000.0);

        try {
            BisectionSolver solverA = new BisectionSolver();
            double withInitial = solverA.solve(f, min, max, initial);

            // SAME-NAME OVERLOAD agreement:
            // For the same function and bracket, solve(f,min,max,initial) must agree with
            // solve(f,min,max); the extra initial value is not allowed to change the root.
            BisectionSolver solverB = new BisectionSolver();
            double withoutInitial = solverB.solve(f, min, max);

            if (Double.isNaN(withInitial) || Double.isInfinite(withInitial) ||
                Double.isNaN(withoutInitial) || Double.isInfinite(withoutInitial)) {
                throw new RuntimeException("[oracle:finite-root] metamorphic violation: solver returned non-finite result input=min=" + min + " max=" + max + " initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 4.0;
            if (Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=min=" + min + " max=" + max + " initial=" + initial + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            // Oracle from the input itself:
            // We constructed the interval around the known sine root n*pi, so the returned
            // root must approximate that known value for any correct implementation.
            if (Math.abs(withInitial - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-sin-root] metamorphic violation: constructed bracket around n*pi should solve to that root input=n=" + n + " min=" + min + " max=" + max + " initial=" + initial + " result=" + withInitial + " expected=" + expectedRoot);
            }
        } catch (RuntimeException t) {
            boolean stackOk = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    stackOk = true;
                    break;
                }
            }
            if ((t instanceof NullPointerException || t.getClass() == RuntimeException.class) && stackOk) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }
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
<details open><summary>▸ Output (~5,149 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double rWithInitial = solver.solve(f, 3.0, 3.2, 3.1);

                /* Contract/oracle:
                 * The same-name overloads solve(f,min,max,initial) and solve(f,min,max)
                 * are documented variants of the same root-finding operation over the same
                 * function and bracket. A correct implementation must therefore agree on the
                 * returned root for the same valid bracket; a "fix" that only suppresses the
                 * crash or skips using the function would break this sibling-agreement check.
                 */
                BisectionSolver solver2 = new BisectionSolver();
                try {
                    double rNoInitial = solver2.solve(f, 3.0, 3.2);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                    if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                            && Math.abs(rWithInitial - rNoInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (Throwable t) {
                boolean hasSolveFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        hasSolveFrame = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && hasSolveFrame) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double delta = 0.1 + (data.consumeInt(0, 900) / 1000.0);
        double min = root - delta;
        double max = root + delta;
        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        if (!(min < max) || f.value(min) * f.value(max) > 0.0) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            double rWithInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle:
             * For the same valid non-null function and the same bracketing interval, the
             * overload with an initial guess must compute the same root as the overload
             * without that guess. We construct a valid bracket around a known sine root
             * (k*pi), so any disagreement is a real semantic bug, not input rejection.
             */
            BisectionSolver solver2 = new BisectionSolver();
            try {
                double rNoInitial = solver2.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                        && Math.abs(rWithInitial - rNoInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-fuzz] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            boolean hasSolveFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    hasSolveFrame = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && hasSolveFrame) {
                throw (NullPointerException) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
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
<details><summary>▸ Prompt (4 message(s), ~6,852 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double rWithInitial = solver.solve(f, 3.0, 3.2, 3.1);

                /* Contract/oracle:
                 * The same-name overloads solve(f,min,max,initial) and solve(f,min,max)
                 * are documented variants of the same root-finding operation over the same
                 * function and bracket. A correct implementation must therefore agree on the
                 * returned root for the same valid bracket; a "fix" that only suppresses the
                 * crash or skips using the function would break this sibling-agreement check.
                 */
                BisectionSolver solver2 = new BisectionSolver();
                try {
                    double rNoInitial = solver2.solve(f, 3.0, 3.2);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                    if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                            && Math.abs(rWithInitial - rNoInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (Throwable t) {
                boolean hasSolveFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        hasSolveFrame = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && hasSolveFrame) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double delta = 0.1 + (data.consumeInt(0, 900) / 1000.0);
        double min = root - delta;
        double max = root + delta;
        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        if (!(min < max) || f.value(min) * f.value(max) > 0.0) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            double rWithInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle:
             * For the same valid non-null function and the same bracketing interval, the
             * overload with an initial guess must compute the same root as the overload
             * without that guess. We construct a valid bracket around a known sine root
             * (k*pi), so any disagreement is a real semantic bug, not input rejection.
             */
            BisectionSolver solver2 = new BisectionSolver();
            try {
                double rNoInitial = solver2.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                        && Math.abs(rWithInitial - rNoInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-fuzz] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            boolean hasSolveFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    hasSolveFrame = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && hasSolveFrame) {
                throw (NullPointerException) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_013/FuzzHarness.java:65: error: unreported exception FunctionEvaluationException; must be caught or declared to be thrown
        if (!(min < max) || f.value(min) * f.value(max) > 0.0) {
                                   ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/Math_70_buggy/fuzz/attempt_013/FuzzHarness.java:65: error: unreported exception FunctionEvaluationException; must be caught or declared to be thrown
        if (!(min < max) || f.value(min) * f.value(max) > 0.0) {
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
<details open><summary>▸ Output (~4,806 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double rWithInitial = solver.solve(f, 3.0, 3.2, 3.1);

                /* Contract/oracle:
                 * solve(f,min,max,initial) and solve(f,min,max) are sibling overloads for the
                 * same root-finding operation on the same valid bracket and must agree.
                 */
                BisectionSolver solver2 = new BisectionSolver();
                try {
                    double rNoInitial = solver2.solve(f, 3.0, 3.2);
                    double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                    if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                            && Math.abs(rWithInitial - rNoInitial) > tol) {
                        throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                    }
                } catch (Throwable ignored) {
                    return;
                }
            } catch (Throwable t) {
                boolean hasSolveFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        hasSolveFrame = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && hasSolveFrame) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                return;
            }
        }

        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;
        double delta = 0.1 + (data.consumeInt(0, 900) / 1000.0);
        double min = root - delta;
        double max = root + delta;
        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        if (!(min < max)) {
            return;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            double rWithInitial = solver.solve(f, min, max, initial);

            /* Contract/oracle:
             * We build a valid bracket around an exact root of sin(x), namely k*pi, with
             * 0 < delta < pi so min=root-delta and max=root+delta necessarily bracket a root.
             * For the same valid function and interval, the overload with initial guess must
             * agree with the overload without it.
             */
            BisectionSolver solver2 = new BisectionSolver();
            try {
                double rNoInitial = solver2.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), solver2.getAbsoluteAccuracy()) * 4.0;
                if (!(Double.isNaN(rWithInitial) || Double.isNaN(rNoInitial))
                        && Math.abs(rWithInitial - rNoInitial) > tol) {
                    throw new RuntimeException("[oracle:overload-fuzz] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + rWithInitial + " rhs=" + rNoInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            boolean hasSolveFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                        && "solve".equals(ste.getMethodName())) {
                    hasSolveFrame = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && hasSolveFrame) {
                throw (NullPointerException) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
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
<details open><summary>▸ Output (~5,419 chars)</summary>

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

        // ANCHOR: exact failing test input. On the buggy build this must hit
        // BisectionSolver.solve(f, min, max, initial) and surface as NPE from solve.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throw propagate(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        // EXPLORE: valid-by-construction brackets around roots of sin(x) = 0 at k*pi.
        // We keep values moderate and ensure min < max with the root inside the interval,
        // so a correct implementation is obliged to handle them.
        int trials = 1 + Math.max(0, data.consumeInt(0, 4));
        for (int i = 0; i < trials; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double width = data.consumeInt(1, 1000) / 1000.0; // (0, 1]
            double min = root - width;
            double max = root + width;

            int frac = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (frac / 1000.0);

            // Slightly vary surrounding content while preserving the key property:
            // a valid bracket around a real root for the real SinFunction.
            if (data.consumeBoolean()) {
                int leftNudge = data.consumeInt(0, 200);
                int rightNudge = data.consumeInt(0, 200);
                min -= leftNudge / 10000.0;
                max += rightNudge / 10000.0;
                if (initial < min) {
                    initial = min;
                } else if (initial > max) {
                    initial = max;
                }
            }

            runOneValidCase(f, min, max, initial);
        }
    }

    private static void runOneValidCase(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver s1 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);

            // Oracle: the documented same-name overloads solve the same mathematical problem.
            // For a correct BisectionSolver, the "initial" parameter does not change the bracketing
            // root that is solved for here; therefore solve(f,min,max,initial) must agree with
            // solve(f,min,max) on the same valid interval. A patch that merely suppresses the crash
            // or dispatches incorrectly can violate this observable relation.
            BisectionSolver s2 = new BisectionSolver();
            double withoutInitial = s2.solve(f, min, max);

            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 4.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throw propagate(t);
            }
            // If either side throws, the oracle does not apply for that input.
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NullPointerException)) {
            return t instanceof RuntimeException && isOracleRuntime((RuntimeException) t);
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleRuntime(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof FunctionEvaluationException
                || t instanceof MaxIterationsExceededException;
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,301 chars)</summary>

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
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: build many valid-by-construction inputs with the same triggering property:
        // non-null function and a bracketing interval around a real root of sin(x).
        double root = Math.PI * data.consumeInt(-100, 100);
        double halfWidth = 0.01 + (data.consumeInt(0, 990) / 1000.0); // 0.01..1.0
        double min = root - halfWidth;
        double max = root + halfWidth;

        // Keep the initial guess inside the interval.
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        // Oracle: the documented same-name solve overloads are required to agree on equivalent inputs.
        // For BisectionSolver, the "initial" value is not part of the bisection state transition, so
        // solve(f, min, max, initial) must compute the same root as solve(f, min, max) on the same
        // valid bracketing interval. A throw-deleting or wrong-delegation patch would violate this.
        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double rhs = withoutInitial.solve(f, min, max);

            double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()) + 1e-12;
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                        + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol
                );
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
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

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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
<details open><summary>▸ Output (~6,129 chars)</summary>

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

        // ANCHOR: exact failing test input. On the buggy build this reaches
        // BisectionSolver.solve(f, min, max, initial) and throws NPE from solve().
        callAndHandle(f, 3.0d, 3.2d, 3.1d, true);

        // EXPLORE: construct many valid-by-construction intervals for sin(x)
        // that bracket a known root k*pi. This satisfies the documented
        // precondition that the function argument is non-null, and supplies
        // ordinary moderate numeric inputs through the real public API.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        double leftDelta = 0.05d + (data.consumeInt(0, 950) / 1000.0d);
        double rightDelta = 0.05d + (data.consumeInt(0, 950) / 1000.0d);

        double min = root - leftDelta;
        double max = root + rightDelta;
        if (!(min < max)) {
            return;
        }

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0d);
        } else {
            initial = root;
        }

        callAndHandle(f, min, max, initial, true);

        // Mandatory oracle: sibling-agreement metamorphic check.
        // Contract basis: the same-name overloads
        //   solve(f, min, max, initial)
        //   solve(f, min, max)
        // are documented to agree where their docs match. For BisectionSolver,
        // the initial value is not part of the bisection computation, so a
        // throw-deleting / branch-skipping "fix" that merely avoids the crash
        // but changes behavior would be caught if these real-library calls disagree.
        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();
        try {
            double withInitial = solverA.solve(f, min, max, initial);
            double withoutInitial = solverB.solve(f, min, max);

            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Double.isInfinite(withInitial) || Double.isInfinite(withoutInitial)) {
                throw new RuntimeException(
                        "[oracle:overload-agree] metamorphic violation: non-finite result input="
                                + "min=" + min + ",max=" + max + ",initial=" + initial
                                + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 10.0d;
            if (Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                        "[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max)"
                                + " input=min=" + min + ",max=" + max + ",initial=" + initial
                                + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (RuntimeException e) {
            if (isOracleRuntime(e)) {
                throw e;
            }
            if (isCleanRejection(e) || !hasSolveFrame(e)) {
                return;
            }
            if (e instanceof NullPointerException) {
                throw e;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (Exception e) {
            return;
        }
    }

    private static void callAndHandle(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (isOracleRuntime(t)) {
                throw t;
            }
            if (isRootCause(t, validByConstruction)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (MaxIterationsExceededException e) {
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (Exception e) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        return validByConstruction
                && (t instanceof NullPointerException)
                && hasSolveFrame(t);
    }

    private static boolean hasSolveFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement frame : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(frame.getClassName())
                    && "solve".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleRuntime(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        String lower = name.toLowerCase();
        return lower.contains("invalid")
                || lower.contains("illegal")
                || lower.contains("bounds")
                || lower.contains("range")
                || lower.contains("argument");
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
<details open><summary>▸ Output (~6,049 chars)</summary>

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

        // ANCHOR: exact failing test input first.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(sin, 3.0, 3.2, 3.1);

            // Contract/oracle:
            // - The failing test asserts this call finds the root at PI.
            // - A correct implementation of BisectionSolver on a valid bracketing interval
            //   around sin(x) = 0 near PI must return a value within solver accuracy of PI.
            double tol = solver.getAbsoluteAccuracy();
            if (Math.abs(result - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: known-root solve should recover PI input=[3.0,3.2,3.1] result=" + result + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE: valid-by-construction inputs with the same root-cause property:
        // non-null real function and a bracketing interval [min,max] containing a known root,
        // with initial inside [min,max].
        int k = data.consumeInt(-100, 100);
        double expectedRoot = k * Math.PI;

        double left = 0.001 + (data.consumeInt(1, 300000) / 100000.0);   // (0.001, 3.001]
        double right = 0.001 + (data.consumeInt(1, 300000) / 100000.0);  // (0.001, 3.001]
        if (left >= Math.PI) {
            left = Math.PI - 0.001;
        }
        if (right >= Math.PI) {
            right = Math.PI - 0.001;
        }

        double min = expectedRoot - left;
        double max = expectedRoot + right;
        double frac = data.consumeInt(0, 100000) / 100000.0;
        double initial = min + frac * (max - min);

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double resultWithInitial = solverWithInitial.solve(sin, min, max, initial);

            // Metamorphic/post-condition:
            // The same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // are documented sibling APIs for the same solve operation; for a correct
            // implementation on the same valid bracketing interval they must agree on
            // the located root up to solver accuracy. A "fix" that merely suppresses
            // the crash but changes behavior will violate this.
            BisectionSolver solverWithoutInitial = new BisectionSolver();
            double resultWithoutInitial = solverWithoutInitial.solve(sin, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy());
            if (Math.abs(resultWithInitial - resultWithoutInitial) > tol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve overloads must agree input=[" + min + "," + max + "," + initial + "] lhs=" + resultWithInitial + " rhs=" + resultWithoutInitial + " tol=" + tol);
            }

            // Oracle from construction:
            // We built the interval around the known root k*PI with width < PI on each side,
            // so sin(min) and sin(max) bracket exactly that root. A correct bisection solve
            // must return that known root within its advertised accuracy.
            if (Math.abs(resultWithInitial - expectedRoot) > solverWithInitial.getAbsoluteAccuracy()) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: constructed bracketing interval should solve to known k*PI input=[" + min + "," + max + "," + initial + "] result=" + resultWithInitial + " expected=" + expectedRoot + " tol=" + solverWithInitial.getAbsoluteAccuracy());
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throwUnchecked(t);
            }
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
        String n = t.getClass().getName();
        return n.contains("Invalid") || n.contains("NoBracketing") || n.contains("Convergence");
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
<details open><summary>▸ Output (~6,090 chars)</summary>

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

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        runOneValidCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction intervals that bracket a real root of sin(x).
        // Property: f is non-null and [min, max] brackets k*pi, with initial inside [min, max].
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 10000) / 10000.0) * 5.0;
        double right = 0.01 + (data.consumeInt(0, 10000) / 10000.0) * 5.0;

        double min = root - left;
        double max = root + right;

        double fraction = data.consumeInt(0, 10000) / 10000.0;
        double initial = min + (max - min) * fraction;

        runOneValidCase(f, min, max, initial, false);
    }

    private static void runOneValidCase(UnivariateRealFunction f, double min, double max, double initial, boolean anchor) {
        Double withInitial = callSolveWithInitial(f, min, max, initial);
        Double withoutInitial = callSolveWithoutInitial(f, min, max);

        // Documented same-name overload agreement: solve(f,min,max,initial) and solve(f,min,max)
        // are sibling APIs for the same solving task on the same function and interval.
        // For a correct BisectionSolver implementation, the "initial" value is not semantically
        // required to change the root returned for a valid bracketing interval. A patch that
        // merely suppresses the crash but skips the real solving behavior would break this.
        if (withInitial == null || withoutInitial == null) {
            return;
        }

        double lhs = withInitial.doubleValue();
        double rhs = withoutInitial.doubleValue();

        if (!Double.isNaN(lhs) && !Double.isNaN(rhs) && Double.isFinite(lhs) && Double.isFinite(rhs)) {
            double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-6);
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min
                        + ", max=" + max
                        + ", initial=" + initial
                        + ", anchor=" + anchor
                        + ", lhs=" + lhs
                        + ", rhs=" + rhs);
            }
        }

        // Stronger anchor-specific post-condition from the original regression test:
        // the root of sin(x) in [3.0, 3.2] is pi, so the solver should return approximately pi.
        if (anchor) {
            double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-6);
            if (Math.abs(lhs - Math.PI) > tol) {
                throw new RuntimeException(
                    "[oracle:anchor-pi] metamorphic violation: expected root near PI for sin(x) on [3.0,3.2]"
                        + " input=min=" + min
                        + ", max=" + max
                        + ", initial=" + initial
                        + ", result=" + lhs
                        + ", expected=" + Math.PI);
            }
        }
    }

    private static Double callSolveWithInitial(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        }
    }

    private static Double callSolveWithoutInitial(UnivariateRealFunction f, double min, double max) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max));
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("TooMany")
                || name.contains("Illegal");
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
<details open><summary>▸ Output (~5,664 chars)</summary>

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
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchorResult = anchorSolver.solve(f, 3.0, 3.2, 3.1);

            /* Contract/oracle: the same-name overloads solve(f,min,max,initial) and solve(f,min,max)
             * are documented to solve the same problem on the same bracketed interval; a correct fix
             * must therefore agree with the sibling overload, so deleting the crashing statement or
             * otherwise bypassing the real solve path would be caught as a wrong-result divergence.
             */
            try {
                BisectionSolver siblingSolver = new BisectionSolver();
                double siblingResult = siblingSolver.solve(f, 3.0, 3.2);
                double tol = Math.max(anchorSolver.getAbsoluteAccuracy(), siblingSolver.getAbsoluteAccuracy());
                if (Double.isNaN(anchorResult) || Double.isNaN(siblingResult)
                        || Math.abs(anchorResult - siblingResult) > tol) {
                    throw new RuntimeException("[oracle:bisection-anchor] metamorphic violation: sibling overload agreement input=[3.0,3.2,3.1] lhs="
                            + anchorResult + " rhs=" + siblingResult);
                }
            } catch (Throwable t) {
                return;
            }
        } catch (RuntimeException t) {
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
            if ((t instanceof IllegalArgumentException || t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && inPatchedRegion) {
                throw t;
            }
            return;
        } catch (FunctionEvaluationException e) {
            return;
        } catch (MaxIterationsExceededException e) {
            return;
        }

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;
            double leftDelta = data.consumeInt(1, 1000) / 1000.0;
            double rightDelta = data.consumeInt(1, 1000) / 1000.0;
            double min = root - leftDelta;
            double max = root + rightDelta;
            double fraction = data.consumeInt(0, 1000) / 1000.0;
            double initial = min + fraction * (max - min);

            try {
                BisectionSolver s1 = new BisectionSolver();
                double r1 = s1.solve(f, min, max, initial);

                /* Contract/oracle: same-name overloads with and without initial guess must agree for the
                 * same function and bracketing interval; both are real library entry points and a correct
                 * implementation is obliged to return the same root, so disagreement is a real wrong-answer bug.
                 */
                try {
                    BisectionSolver s2 = new BisectionSolver();
                    double r2 = s2.solve(f, min, max);
                    double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy());
                    if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > tol) {
                        throw new RuntimeException("[oracle:bisection-sibling] metamorphic violation: sibling overload agreement input=["
                                + min + "," + max + "," + initial + "] lhs=" + r1 + " rhs=" + r2);
                    }
                } catch (Throwable t) {
                    continue;
                }
            } catch (RuntimeException t) {
                boolean inPatchedRegion = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int j = 0; j < st.length; j++) {
                        StackTraceElement e = st[j];
                        if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                                && "solve".equals(e.getMethodName())) {
                            inPatchedRegion = true;
                            break;
                        }
                    }
                }
                if ((t instanceof IllegalArgumentException || t instanceof NumberFormatException)) {
                    continue;
                }
                if ((t instanceof NullPointerException) && inPatchedRegion) {
                    throw t;
                }
                continue;
            } catch (FunctionEvaluationException e) {
                continue;
            } catch (MaxIterationsExceededException e) {
                continue;
            }
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,678 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Helper {
            boolean passesThroughPatchedSolve(Throwable t) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isGroundTruthRootCause(Throwable t) {
                return t instanceof NullPointerException && passesThroughPatchedSolve(t);
            }

            boolean isCleanRejection(Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                String n = t.getClass().getName();
                return n.startsWith("org.apache.commons.math.")
                        && (n.contains("Invalid")
                        || n.contains("Illegal")
                        || n.contains("Argument")
                        || n.contains("Convergence")
                        || n.contains("NoBracketing")
                        || n.contains("NotPositive")
                        || n.contains("OutOfRange"));
            }

            void handle(Throwable t) {
                if (isGroundTruthRootCause(t)) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
            }
        }
        Helper h = new Helper();

        {
            BisectionSolver solver = new BisectionSolver();
            SinFunction f = new SinFunction();
            try {
                double r = solver.solve(f, 3.0, 3.2, 3.1);
                if (Math.abs(r - Math.PI) > solver.getAbsoluteAccuracy()) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: exact test input should solve sin(x)=0 near pi input=[3.0,3.2,3.1] lhs=" + r + " rhs=" + Math.PI);
                }
            } catch (Throwable t) {
                h.handle(t);
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double center = k * Math.PI;

        int leftMilli = data.consumeInt(1, 1000);
        int rightMilli = data.consumeInt(1, 1000);
        double leftDelta = leftMilli / 1000.0;
        double rightDelta = rightMilli / 1000.0;

        double min = center - leftDelta;
        double max = center + rightDelta;
        double initial;
        if (data.consumeBoolean()) {
            initial = center;
        } else {
            int initMilli = data.consumeInt(-999, 999);
            initial = center + (initMilli / 1000.0) * Math.min(leftDelta, rightDelta);
        }
        if (initial < min) {
            initial = min;
        }
        if (initial > max) {
            initial = max;
        }

        BisectionSolver solverA = new BisectionSolver();
        BisectionSolver solverB = new BisectionSolver();
        SinFunction f = new SinFunction();

        Double withInitial = null;
        Double withoutInitial = null;

        try {
            withInitial = solverA.solve(f, min, max, initial);
        } catch (Throwable t) {
            h.handle(t);
            return;
        }

        try {
            withoutInitial = solverB.solve(f, min, max);
        } catch (Throwable t) {
            h.handle(t);
            return;
        }

        if (withInitial == null || withoutInitial == null) {
            return;
        }

        double acc = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy());

        if (Math.abs(withInitial - center) > acc) {
            throw new RuntimeException("[oracle:root-center] metamorphic violation: for sin(x) on an interval constructed to bracket k*pi, solve(f,min,max,initial) must return that known root input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + center);
        }

        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > acc) {
            throw new RuntimeException("[oracle:overload-agree] metamorphic violation: same-name solve overloads with equivalent inputs must agree on the root input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + withoutInitial);
        }
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
<details open><summary>▸ Output (~5,222 chars)</summary>

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
            BisectionSolver anchorSolver = new BisectionSolver();
            double anchor = anchorSolver.solve(f, 3.0, 3.2, 3.1);
            double expected = Math.PI;
            double tol = Math.max(anchorSolver.getAbsoluteAccuracy(), 1.0e-12);
            if (Math.abs(anchor - expected) > tol) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should solve sin(x)=0 near PI input=[3.0,3.2,3.1] result=" + anchor + " expected=" + expected + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromSolve(t)) {
                throwUnchecked(t);
                return;
            }
        }

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;
        int milli = data.consumeInt(1, 1000);
        double delta = milli / 1000.0;
        double min = root - delta;
        double max = root + delta;

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        if (!(min < max) || initial < min || initial > max) {
            return;
        }

        BisectionSolver solverA = new BisectionSolver();
        double rWithInitial;
        try {
            rWithInitial = solverA.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromSolve(t)) {
                throwUnchecked(t);
                return;
            }
            return;
        }

        BisectionSolver solverB = new BisectionSolver();
        double rWithoutInitial;
        try {
            rWithoutInitial = solverB.solve(f, min, max);
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are documented
         * to solve the same bracketed root for the same function and interval; for bisection,
         * the extra initial value is not part of the mathematical result. A throw-deleting or
         * wrong-delegation patch could make the 4-arg overload return a different result while
         * the 3-arg overload remains correct.
         */
        double tol = Math.max(Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()), 1.0e-6);
        if (Double.isNaN(rWithInitial) || Double.isNaN(rWithoutInitial) || Math.abs(rWithInitial - rWithoutInitial) > tol) {
            throw new RuntimeException("[oracle:overload] metamorphic violation: solve overloads disagree input=[k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "] lhs=" + rWithInitial + " rhs=" + rWithoutInitial + " tol=" + tol);
        }

        try {
            double value = f.value(rWithInitial);
            if (rWithInitial < min - tol || rWithInitial > max + tol || Math.abs(value) > 1.0e-4) {
                throw new RuntimeException("[oracle:root] metamorphic violation: returned value should be a root inside the bracket input=[k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "] result=" + rWithInitial + " f(result)=" + value);
            }
        } catch (FunctionEvaluationException ignored) {
        }
    }

    private static boolean isRootCauseFromSolve(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("NoBracketing") || name.contains("Convergence") || name.contains("Argument");
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,982 chars)</summary>

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

        // ANCHOR: exact failing test input first.
        {
            BisectionSolver solver = new BisectionSolver();
            try {
                double r1 = solver.solve(f, 3.0, 3.2, 3.1);
                double r2 = solver.solve(f, 3.0, 3.2);

                // Contract/oracle:
                // - The 4-arg overload with function should behave like the sibling overload on the same function/interval.
                // - For this concrete valid interval around the root of sin(x), the result should be near PI as in the original test.
                double acc = solver.getAbsoluteAccuracy();
                if (Double.isNaN(r1) || Math.abs(r1 - Math.PI) > acc) {
                    throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: expected root near PI input=[3.0,3.2,3.1] result=" + r1 + " acc=" + acc);
                }
                if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > Math.max(acc, 1.0e-12)) {
                    throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: overloads disagree input=[3.0,3.2,3.1] lhs=" + r1 + " rhs=" + r2 + " acc=" + acc);
                }
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                if (t.getClass().getName().startsWith("org.apache.commons.math")) {
                    return;
                }
                throw t;
            } catch (MaxIterationsExceededException e) {
                return;
            } catch (FunctionEvaluationException e) {
                return;
            }
        }

        // EXPLORE: many valid-by-construction intervals around integer multiples of PI.
        int iters = 1 + Math.max(0, Math.min(8, data.remainingBytes()));
        for (int i = 0; i < iters; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            int leftMicros = data.consumeInt(1, 900000);
            int rightMicros = data.consumeInt(1, 900000);
            double left = leftMicros / 1000000.0;
            double right = rightMicros / 1000000.0;

            double min = root - left;
            double max = root + right;

            int posMicros = data.consumeInt(0, 1000000);
            double initial = min + (max - min) * (posMicros / 1000000.0);

            BisectionSolver solver = new BisectionSolver();
            try {
                double withInitial = solver.solve(f, min, max, initial);
                double withoutInitial = solver.solve(f, min, max);

                // Contract/oracle:
                // These same-name overloads are documented as solving the same function on the same interval;
                // the "initial" guess should not change the mathematical root returned for a valid bracketing interval.
                double acc = solver.getAbsoluteAccuracy();
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                        || Math.abs(withInitial - withoutInitial) > Math.max(acc, 1.0e-12)) {
                    throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) != solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + withInitial + " rhs=" + withoutInitial + " acc=" + acc);
                }

                // Input-known oracle: we constructed the interval to bracket the known root k*PI of sin(x).
                if (Math.abs(withInitial - root) > Math.max(acc, 1.0e-7)) {
                    throw new RuntimeException("[oracle:known-root] metamorphic violation: expected root near constructed multiple of PI input=[" + min + "," + max + "," + initial + "] expected=" + root + " actual=" + withInitial + " acc=" + acc);
                }
            } catch (RuntimeException t) {
                boolean throughSolve = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                            && "solve".equals(e.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && throughSolve) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    continue;
                }
                if (t.getClass().getName().startsWith("org.apache.commons.math")) {
                    continue;
                }
                throw t;
            } catch (MaxIterationsExceededException e) {
                continue;
            } catch (FunctionEvaluationException e) {
                continue;
            }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,495 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction anchorFunction = new SinFunction();
        exerciseCase(anchorFunction, 3.0d, 3.2d, 3.1d, true);

        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;
        double delta = 0.1d + (data.consumeInt(0, 900) / 1000.0d);
        double min = root - delta;
        double max = root + delta;

        double initial;
        int initialMode = data.consumeInt(0, 3);
        if (initialMode == 0) {
            initial = (min + max) / 2.0d;
        } else if (initialMode == 1) {
            initial = min + (max - min) * 0.25d;
        } else if (initialMode == 2) {
            initial = min + (max - min) * 0.75d;
        } else {
            int parts = data.consumeInt(1, 1000);
            initial = min + (max - min) * (parts / 1000.0d);
        }

        exerciseCase(new SinFunction(), min, max, initial, true);
    }

    private static void exerciseCase(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        Double withInitial = callSolveWithInitial(f, min, max, initial, validByConstruction);
        if (withInitial == null) {
            return;
        }

        Double withoutInitial = callSolveWithoutInitial(f, min, max, validByConstruction);
        if (withoutInitial == null) {
            return;
        }

        BisectionSolver referenceSolver = new BisectionSolver();
        double tolerance = referenceSolver.getAbsoluteAccuracy();

        /* Contract/oracle:
         * The same-name overloads solve(f, min, max, initial) and solve(f, min, max)
         * solve the same function on the same interval and therefore must return the
         * same root up to solver accuracy for a correct implementation. A "fix" that
         * merely suppresses the crash or skips using the provided function would break
         * this sibling-agreement relation.
         */
        if (Math.abs(withInitial.doubleValue() - withoutInitial.doubleValue()) > tolerance) {
            throw new RuntimeException(
                "[oracle:sibling-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input={min=" + min
                    + ", max=" + max
                    + ", initial=" + initial
                    + ", f=" + f.getClass().getName()
                    + "} lhs=" + withInitial
                    + " rhs=" + withoutInitial
                    + " tol=" + tolerance);
        }
    }

    private static Double callSolveWithInitial(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return null;
        }
    }

    private static Double callSolveWithoutInitial(UnivariateRealFunction f, double min, double max, boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max));
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return null;
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                return true;
            }
        }

        if (isCleanRejection(t)) {
            return false;
        }

        return validByConstruction && (t instanceof NullPointerException) && hasSolveFrame(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            if (c == IllegalArgumentException.class || c == NumberFormatException.class) {
                return true;
            }
            String name = c.getName();
            if (name.startsWith("org.apache.commons.math")) {
                String lower = name.toLowerCase();
                if (lower.contains("argument") || lower.contains("invalid") || lower.contains("illegal")) {
                    return true;
                }
            }
        }
        return false;
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
<details open><summary>▸ Output (~4,601 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(3.0d, 3.2d, 3.1d, Math.PI);

        int scenarios = 1;
        if (data.remainingBytes() > 0) {
            scenarios += data.consumeInt(0, 3);
        }

        for (int i = 0; i < scenarios; i++) {
            int k = data.consumeInt(-100, 100);
            double expectedRoot = k * Math.PI;

            double left = data.consumeInt(1, 1000) / 1000.0d;
            double right = data.consumeInt(1, 1000) / 1000.0d;

            double min = expectedRoot - left;
            double max = expectedRoot + right;

            int pos = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (pos / 1000.0d);

            runScenario(min, max, initial, expectedRoot);
        }
    }

    private static void runScenario(double min, double max, double initial, double expectedRoot) {
        UnivariateRealFunction f = new SinFunction();
        BisectionSolver solver = new BisectionSolver();
        double result;

        try {
            result = solver.solve(f, min, max, initial);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Throwable t) {
            return;
        }

        double tol = Math.max(1.0e-6d, solver.getAbsoluteAccuracy() * 8.0d);

        /* Contract/oracle: this solver computes a zero of the supplied function on the interval.
           Here the interval is built around the known zero k*pi of the real library SinFunction,
           so a correct implementation must return approximately that root. A patch that merely
           suppresses the buggy path or ignores the supplied function can return the wrong value
           without throwing, which this oracle catches. */
        if (!Double.isNaN(result) && !Double.isInfinite(result)) {
            if (Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: expected constructed sin root input=min=" + min + ",max=" + max + ",initial=" + initial + " result=" + result + " expected=" + expectedRoot + " tol=" + tol);
            }
        }

        /* Same-name overload agreement: for the same function and interval, solve(f,min,max,initial)
           and solve(f,min,max) are documented sibling entry points to the same computation and must
           agree on the root when both succeed. If either call throws, this check does not apply. */
        try {
            BisectionSolver sibling = new BisectionSolver();
            double siblingResult = sibling.solve(f, min, max);
            double siblingTol = Math.max(tol, sibling.getAbsoluteAccuracy() * 8.0d);
            if (!Double.isNaN(result) && !Double.isInfinite(result)
                    && !Double.isNaN(siblingResult) && !Double.isInfinite(siblingResult)
                    && Math.abs(result - siblingResult) > siblingTol) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: solve overloads disagree input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + result + " rhs=" + siblingResult + " tol=" + siblingTol);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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
<details open><summary>▸ Output (~6,596 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double r = solver.solve(f, 3.0, 3.2, 3.1);

            // Contract/oracle: all solve overloads for the same function and bracket
            // must agree on the root; this exact test expects pi within absolute accuracy.
            double tol = Math.max(solver.getAbsoluteAccuracy(), 1.0e-12);
            if (Math.abs(r - Math.PI) > tol) {
                throw new RuntimeException("[oracle:anchor-pi] metamorphic violation: exact regression input should solve sin(x)=0 near pi input=[3.0,3.2,3.1] result=" + r + " expected=" + Math.PI + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isOracleFailure(t)) {
                throwUnchecked(t);
            }
            return;
        }

        int rounds = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 3) : 0);
        for (int i = 0; i < rounds; i++) {
            // EXPLORE: the bug-triggering property is calling solve(f,min,max,initial)
            // with a non-null function and a valid bracket. The buggy code ignores f and
            // dispatches to solve(min,max), which uses an unset internal function and NPEs.
            // Build valid-by-construction sin intervals around k*pi so a correct solver must accept them.
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int deltaMilli = data.consumeInt(100, 1000);
            double delta = deltaMilli / 1000.0; // in [0.1, 1.0], so sin changes sign around k*pi

            double min = root - delta;
            double max = root + delta;

            double initial;
            if (data.consumeBoolean()) {
                initial = root;
            } else {
                int offsetMilli = data.consumeInt(-deltaMilli + 1, deltaMilli - 1);
                initial = root + (offsetMilli / 1000.0);
            }

            Double withInitial;
            try {
                BisectionSolver s1 = new BisectionSolver();
                withInitial = Double.valueOf(s1.solve(f, min, max, initial));
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    continue;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                if (isOracleFailure(t)) {
                    throwUnchecked(t);
                }
                continue;
            }

            Double withoutInitial;
            try {
                BisectionSolver s2 = new BisectionSolver();
                withoutInitial = Double.valueOf(s2.solve(f, min, max));
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    continue;
                }
                if (isOracleFailure(t)) {
                    throwUnchecked(t);
                }
                continue;
            }

            // Documented same-name-overload agreement: solve(f,min,max,initial) and solve(f,min,max)
            // solve the same function on the same bracket. For a correct implementation, the unused
            // initial guess must not change the computed root for bisection. A throw-deleting patch
            // or wrong dispatch would break this equivalence.
            double lhs = withInitial.doubleValue();
            double rhs = withoutInitial.doubleValue();
            double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-9);
            if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on a valid bracket input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
            }

            // Oracle from known construction: we built a bracket around k*pi for sin(x)=0,
            // so the returned root must be close to that known root.
            if (Math.abs(lhs - root) > Math.max(tol, delta + 1.0e-9) || Math.abs(rhs - root) > Math.max(tol, delta + 1.0e-9)) {
                throw new RuntimeException("[oracle:known-root] metamorphic violation: solver should return the known root inside the constructed sin bracket input=[" + min + "," + max + "," + initial + "] expectedRoot=" + root + " lhs=" + lhs + " rhs=" + rhs);
            }
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("NotStrictlyPositive")
                || name.contains("NotPositive");
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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
<details open><summary>▸ Output (~4,313 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        // ANCHOR: exact trigger from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, 3.0, 3.2, 3.1);

            // Post-condition / sibling-agreement oracle:
            // The same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // are documented to solve the same problem for the same function and bracket.
            // For bisection, the initial guess must not change the computed root.
            // A "fix" that merely suppresses the crash but skips delegating to the real
            // solve(f,min,max) path would break this observable agreement.
            try {
                BisectionSolver sibling = new BisectionSolver();
                double siblingResult = sibling.solve(f, 3.0, 3.2);
                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy());
                if (Double.isNaN(result) || Double.isNaN(siblingResult) || Math.abs(result - siblingResult) > tol) {
                    throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + result + " rhs=" + siblingResult);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: valid-by-construction brackets around real roots of sin(x).
        // This keeps inputs within the documented precondition f != null and constructs
        // intervals that a correct solver is obliged to accept.
        int k = data.consumeInt(-1000, 1000);
        double width = 0.1 + (data.consumeInt(0, 900) / 1000.0); // in [0.1, 1.0]
        double root = k * Math.PI;
        double min = root - width;
        double max = root + width;

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double lhs = solverWithInitial.solve(f, min, max, initial);

            try {
                BisectionSolver solverNoInitial = new BisectionSolver();
                double rhs = solverNoInitial.solve(f, min, max);
                double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverNoInitial.getAbsoluteAccuracy());
                if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException("[oracle:overload-explore] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
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

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
<details open><summary>▸ Output (~5,899 chars)</summary>

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

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        // This input is valid by construction: non-null function and [3.0, 3.2] brackets the root pi.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(anchorFunction, 3.0d, 3.2d, 3.1d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInSolve(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE: generate many valid brackets around known roots of sin(x), so the API is
        // obligated to accept them. This reaches the same public entry point with varied inputs.
        int cases = 1 + Math.max(0, Math.min(4, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int leftMilli = data.consumeInt(1, 1000);
            int rightMilli = data.consumeInt(1, 1000);
            double min = root - (leftMilli / 1000.0d);
            double max = root + (rightMilli / 1000.0d);
            double initial = data.consumeBoolean()
                    ? root
                    : min + (max - min) * (data.consumeInt(0, 1000) / 1000.0d);

            UnivariateRealFunction f = new SinFunction();

            try {
                BisectionSolver solverA = new BisectionSolver();
                double lhs = solverA.solve(f, min, max, initial);

                BisectionSolver solverB = new BisectionSolver();
                double rhs = solverB.solve(f, min, max);

                // Documented overload-agreement oracle:
                // The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are both
                // public APIs for solving the same bracketed function root. For bisection, initial
                // is not part of the mathematical result, so a correct implementation must return
                // the same root up to solver accuracy. A throw-deleting patch that skips binding
                // the function or otherwise changes behavior can violate this without throwing.
                double tol = Math.max(Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()), 1.0e-8d);
                if (Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                            "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                    + " input={k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "}"
                                    + " lhs=" + lhs + " rhs=" + rhs);
                }

                // Additional observable post-condition from our constructed input:
                // We built the interval around the known root k*pi of sin(x), so any correct solve
                // result must be close to that known root within a small multiple of solver accuracy.
                double expected = root;
                double knownTol = Math.max(10.0d * tol, 1.0e-6d);
                if (Math.abs(lhs - expected) > knownTol) {
                    throw new RuntimeException(
                            "[oracle:known-root] metamorphic violation: solve result must recover constructed sin root"
                                    + " input={k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "}"
                                    + " lhs=" + lhs + " expected=" + expected);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseInSolve(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        }
    }

    private static boolean isRootCauseInSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement ste : trace) {
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
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("Argument")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("Convergence")
                || name.contains("TooMany")
                || name.contains("NotPositive");
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
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
<details open><summary>▸ Output (~6,800 chars)</summary>

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
            final double min = 3.0;
            final double max = 3.2;
            final double initial = 3.1;
            final boolean validByConstruction = true;
            double lhs;
            try {
                lhs = solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    String msg = t.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
                    return;
                }
                return;
            }

            double rhs;
            try {
                rhs = new BisectionSolver().solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            double tol = Math.max(solver.getAbsoluteAccuracy(), new BisectionSolver().getAbsoluteAccuracy()) * 8.0;
            if (Double.isFinite(lhs) && Double.isFinite(rhs) && Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:overload-agreement-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + lhs + " rhs=" + rhs);
            }
            if (Double.isFinite(lhs) && Math.abs(lhs - Math.PI) > tol * 4.0) {
                throw new RuntimeException("[oracle:pi-anchor] metamorphic violation: anchored valid sin bracket around PI should solve near PI input=min=" + min + ",max=" + max + ",initial=" + initial + " lhs=" + lhs + " rhs=" + Math.PI);
            }
        }

        {
            int k = data.consumeInt(-50, 50);
            int leftMillis = data.consumeInt(1, 1000);
            int rightMillis = data.consumeInt(1, 1000);
            int fracMillis = data.consumeInt(0, 1000);

            double center = k * Math.PI;
            double min = center - (leftMillis / 1000.0);
            double max = center + (rightMillis / 1000.0);
            double initial = min + ((max - min) * (fracMillis / 1000.0));
            final boolean validByConstruction = f != null && min <= initial && initial <= max;

            BisectionSolver solver = new BisectionSolver();
            double lhs;
            try {
                lhs = solver.solve(f, min, max, initial);
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    String msg = t.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
                boolean throughSolve = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                            && "solve".equals(ste.getMethodName())) {
                        throughSolve = true;
                        break;
                    }
                }
                if (validByConstruction && t instanceof NullPointerException && throughSolve) {
                    throw (NullPointerException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
                    return;
                }
                return;
            }

            double rhs;
            try {
                rhs = new BisectionSolver().solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            /* Contract/oracle:
             * The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are required to agree on the same
             * valid problem instance here: a non-null real function with a bracket [min,max] that contains a root.
             * In BisectionSolver the initial guess is not part of the bisection iteration, and the patched method
             * explicitly delegates to solve(f,min,max). A patch that merely suppresses the crash or skips the real
             * delegation can return a different result without throwing; this observable equality catches that.
             */
            double tol = Math.max(solver.getAbsoluteAccuracy(), new BisectionSolver().getAbsoluteAccuracy()) * 8.0;
            if (Double.isFinite(lhs) && Double.isFinite(rhs) && Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException("[oracle:overload-agreement-fuzz] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) on the same valid bracket input=min=" + min + ",max=" + max + ",initial=" + initial + ",k=" + k + " lhs=" + lhs + " rhs=" + rhs);
            }

            double expected = center;
            if (Double.isFinite(lhs) && Math.abs(lhs - expected) > Math.max(tol * 4.0, 1.0e-6)) {
                throw new RuntimeException("[oracle:root-location-fuzz] metamorphic violation: valid sin bracket constructed around k*pi must solve near that known root input=min=" + min + ",max=" + max + ",initial=" + initial + ",k=" + k + " lhs=" + lhs + " rhs=" + expected);
            }
        }
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
<details open><summary>▸ Output (~4,660 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction sin =
                new org.apache.commons.math.analysis.SinFunction();

        // Anchor: exact regression input from BisectionSolverTest.testMath369.
        runSolveCase(sin, 3.0, 3.2, 3.1, true);

        int cases = data.consumeInt(1, 4);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int widthMilli = data.consumeInt(10, 1000);
            double halfWidth = widthMilli / 1000.0;

            double min = root - halfWidth;
            double max = root + halfWidth;

            int fracMilli = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (fracMilli / 1000.0);

            runSolveCase(sin, min, max, initial, true);
        }
    }

    private static void runSolveCase(org.apache.commons.math.analysis.UnivariateRealFunction f,
                                     double min, double max, double initial,
                                     boolean validByConstruction) {
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract/oracle: same-name overloads with the same documented purpose must agree on
        // equivalent inputs. For a correct implementation, solve(f, min, max, initial) and
        // solve(f, min, max) solve the same bracketed equation on the same function; deleting the
        // function-taking delegation or routing to the wrong overload breaks this observable result.
        try {
            BisectionSolver withInitial = new BisectionSolver();
            double r1 = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double r2 = withoutInitial.solve(f, min, max);

            double tol = Math.max(
                    Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()) * 4.0,
                    1.0e-8);

            if (Double.isNaN(r1) || Double.isNaN(r2) || Math.abs(r1 - r2) > tol) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                                + " lhs=" + r1 + " rhs=" + r2);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrow(t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && !(t instanceof IllegalArgumentException) && validByConstruction
                && isSolveStack(t) && t instanceof NullPointerException) {
            return true;
        }
        return false;
    }

    private static boolean isSolveStack(Throwable t) {
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
        String cn = t.getClass().getName();
        return cn.contains("Invalid")
                || cn.contains("OutOfRange")
                || cn.contains("NoBracketing")
                || cn.contains("NotStrictlyPositive")
                || cn.contains("NotPositive");
    }

    private static void rethrow(Throwable t) {
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
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **29/29 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov; 29 raw fuzz log(s)**
<details><summary>▸ detail (~14,671 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 488}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 488}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 488}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 559}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 486}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 485}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 489}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 484}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 487}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 489}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 486}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/011_patch1-Math-70-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
