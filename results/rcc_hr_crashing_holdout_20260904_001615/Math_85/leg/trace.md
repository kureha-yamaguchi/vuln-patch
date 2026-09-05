# Pipeline trace — Math-85

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,7 @@ public class UnivariateRealSolverUtils {
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if (fa * upperBound >= 0.0 ) {
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +
```

15 sequential steps — 3 LLM calls, 12 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)
- **patched-fuzz** — fuzzes an accepted harness against the PATCHED build — FIRED (with the input + mismatch) means the overfit was caught; quiet means it escaped

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.distribution.NormalDistributionTest', test_method='t (~622 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.distribution.NormalDistributionTest', test_method='testMath280', source_path='/tmp/d4j/Math_85_buggy/src/test/org/apache/commons/math/distribution/NormalDistributionTest.java', method_source='    public void testMath280() throws MathException {\\n        NormalDistribution normal = new NormalDistributionImpl(0,1);\\n        double result = normal.inverseCumulativeProbability(0.9772498680518209);\\n        assertEquals(2.0, result, 1.0e-12);\\n    }', exception_type='org.apache.commons.math.MathException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
<details><summary>▸ output — test": "org.apache.commons.math.distribution.NormalDistributionTest::testMath280 (~889 chars)</summary>

**output:** 
```json
[
  {
    "test": "org.apache.commons.math.distribution.NormalDistributionTest::testMath280",
    "failure_message": "--- org.apache.commons.math.distribution.NormalDistributionTest::testMath280\norg.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0",
    "support_chars": 148
  }
]
```

</details>

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~5,706 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java\n+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java\n@@ -195,7 +195,7 @@ public class UnivariateRealSolverUtils {\n         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && \n                 ((a > lowerBound) || (b < upperBound)));\n    \n-        if (fa * fb >= 0.0 ) {\n+        if (fa * upperBound >= 0.0 ) {\n             throw new ConvergenceException(\n                       \"number of iterations={0}, maximum iterations={1}, \" +\n                       \"initial={2}, lower bound={3}, upper bound={4}, final a value={5}, \" +\n",
  "functions": [
    {
      "func_name": "bracket",
      "func_signature": "public static double[] bracket(UnivariateRealFunction function, double initial, double lowerBound, double upperBound, int maximumIterations) throws ConvergenceException, FunctionEvaluationException",
      "func_source": "    public static double[] bracket(UnivariateRealFunction function,\n            double initial, double lowerBound, double upperBound, \n            int maximumIterations) throws ConvergenceException, \n            FunctionEvaluationException {\n        \n        if (function == null) {\n            throw MathRuntimeException.createIllegalArgumentException(\"function is null\");\n        }\n        if (maximumIterations <= 0)  {\n            throw MathRuntimeException.createIllegalArgumentException(\n                  \"bad value for maximum iterations number: {0}\", maximumIterations);\n        }\n        if (initial < lowerBound || initial > upperBound || lowerBound >= upperBound) {\n            throw MathRuntimeException.createIllegalArgumentException(\n                  \"invalid bracketing parameters:  lower bound={0},  initial={1}, upper bound={2}\",\n                  lowerBound, initial, upperBound);\n        }\n        double a = initial;\n        double b = initial;\n        double fa;\n        double fb;\n        int numIterations = 0 ;\n    \n        do {\n            a = Math.max(a - 1.0, lowerBound);\n            b = Math.min(b + 1.0, upperBound);\n            fa = function.value(a);\n            \n            fb = function.value(b);\n            numIterations++ ;\n        } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && \n                ((a > lowerBound) || (b < upperBound)));\n   \n        if (fa * fb >= 0.0 ) {\n            throw new ConvergenceException(\n                      \"number of iterations={0}, maximum iterations={1}, \" +\n                      \"initial={2}, lower bound={3}, upper bound={4}, final a value={5}, \" +\n                      \"final b value={6}, f(a)={7}, f(b)={8}\",\n                      numIterations, maximumIterations, initial,\n                      lowerBound, upperBound, a, b, fa, fb);\n        }\n        \n        return new double[]{a, b};\n    }",
      "func_class": "UnivariateRealSolverUtils",
      "func_class_fq": "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils",
      "func_param_types": [
        "UnivariateRealFunction",
        "double",
        "double",
        "double",
        "int"
      ],
      "fi_name": "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "overload_types": [
        [
          "UnivariateRealFunction",
          "double",
          "double",
          "double"
        ],
        [
          "UnivariateRealFunction",
          "double",
          "double",
          "double",
          "int"
        ]
      ],
      "xrefs": [],
      "reachable": [
        "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)",
        "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,int)",
        "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,double,double,double)",
        "Math.max(float,double)",
        "Math.min(float,double)",
        "[org.apache.commons.math.analysis.UnivariateRealFunction].value(org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils)",
        "[org.apache.commons.math.ConvergenceException].<init>(String,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,int,double,double,double,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,double,double)",
        "[org.apache.commons.math.MathRuntimeException].buildMessage(org.apache.commons.math.MathRuntimeException,String,Object[])",
        "[IllegalArgumentException].<init>(String)"
      ],
      "related_callees": [],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.analysis.solvers",
  "root_cause_reachable": [
    "MathRuntimeException.createIllegalArgumentException(String)",
    "MathRuntimeException.createIllegalArgumentException(String, int)",
    "MathRuntimeException.createIllegalArgumentException(String, double, double, double)",
    "UnivariateRealFunction.value",
    "ConvergenceException.<init>",
    "MathRuntimeException.buildMessage"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import org.apache.commons.math.FunctionEvaluationException;",
    "import org.apache.commons.math.ConvergenceException;",
    "import org.apache.commons.math.MathRuntimeException;",
    "import org.apache.commons.math.analysis.UnivariateRealFunction;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,825 chars)</summary>

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

Codebase: `Math_85_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,7 @@ public class UnivariateRealSolverUtils {
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if (fa * upperBound >= 0.0 ) {
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `bracket`:
<signature>
public static double[] bracket(UnivariateRealFunction function, double initial, double lowerBound, double upperBound, int maximumIterations) throws ConvergenceException, FunctionEvaluationException
</signature>
<code>
    public static double[] bracket(UnivariateRealFunction function,
            double initial, double lowerBound, double upperBound, 
            int maximumIterations) throws ConvergenceException, 
            FunctionEvaluationException {
        
        if (function == null) {
            throw MathRuntimeException.createIllegalArgumentException("function is null");
        }
        if (maximumIterations <= 0)  {
            throw MathRuntimeException.createIllegalArgumentException(
                  "bad value for maximum iterations number: {0}", maximumIterations);
        }
        if (initial < lowerBound || initial > upperBound || lowerBound >= upperBound) {
            throw MathRuntimeException.createIllegalArgumentException(
                  "invalid bracketing parameters:  lower bound={0},  initial={1}, upper bound={2}",
                  lowerBound, initial, upperBound);
        }
        double a = initial;
        double b = initial;
        double fa;
        double fb;
        int numIterations = 0 ;
    
        do {
            a = Math.max(a - 1.0, lowerBound);
            b = Math.min(b + 1.0, upperBound);
            fa = function.value(a);
            
            fb = function.value(b);
            numIterations++ ;
        } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                ((a > lowerBound) || (b < upperBound)));
   
        if (fa * fb >= 0.0 ) {
            throw new ConvergenceException(
                      "number of iterations={0}, maximum iterations={1}, " +
                      "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +
                      "final b value={6}, f(a)={7}, f(b)={8}",
                      numIterations, maximumIterations, initial,
                      lowerBound, upperBound, a, b, fa, fb);
        }
        
        return new double[]{a, b};
    }
</code>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NormalDistribution`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.MathException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `bracket` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: org.apache.commons.math.MathException
message: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0
thrown_at: org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability(AbstractContinuousDistribution.java:104)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.distribution.NormalDistributionTest" method="testMath280">
    public void testMath280() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0,1);
        double result = normal.inverseCumulativeProbability(0.9772498680518209);
        assertEquals(2.0, result, 1.0e-12);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.distribution.NormalDistributionTest::testMath280
org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.distribution.NormalDistributionTest">
// --- setUp() (test setup) ---
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setTolerance(1E-6);
    }
</test_support>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f the function.
  - @param x0 the lower bound for the interval.
  - @param x1 the upper bound for the interval.
  - @throws ConvergenceException if the iteration count was exceeded
  - @throws FunctionEvaluationException if an error occurs evaluating
  - @throws IllegalArgumentException if f is null or the endpoints do not
  - @param f the function
  - @param x0 the lower bound for the interval
  - @param x1 the upper bound for the interval
  - @param absoluteAccuracy the accuracy to be used by the solver
  - @throws ConvergenceException if the iteration count is exceeded
  - @throws FunctionEvaluationException if an error occurs evaluating the
  - @throws IllegalArgumentException if f is null, the endpoints do not
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  bracket(UnivariateRealFunction function, 
            double initial, double lowerBound, double upperBound) / (UnivariateRealFunction function,
            double initial, double lowerBound, double upperBound, 
            int maximumIterations)
  solve(UnivariateRealFunction f, double x0, double x1) / (UnivariateRealFunction f, double x0, double x1,
            double absoluteAccuracy)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
</root_cause_reachable>
First harness: establish the most direct path from the fuzz entrypoint through the patched code.
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
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
<details open><summary>▸ Output (~6,715 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = consumeFiniteMean(data);
        double sd = consumePositiveSd(data);

        try {
            exploreDistribution(mean, sd, data);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double x = normal.inverseCumulativeProbability(p);

            /* Contract asserted: for a valid probability p in (0,1), inverseCumulativeProbability(p)
             * should return x such that cumulativeProbability(x) == p (up to numerical tolerance).
             * A patch that merely suppresses the throw or skips correct bracketing can return a wrong
             * finite value; this round-trip detects that observable wrong-output behavior. */
            double roundTrip = normal.cumulativeProbability(x);
            if (!closeEnough(roundTrip, p, 1e-12, 1e-9)) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: cdf(icdf(p)) != p input="
                        + p + " x=" + x + " lhs=" + roundTrip + " rhs=" + p);
            }

            NormalDistribution fresh = new NormalDistributionImpl(0.0, 1.0);
            double x2 = fresh.inverseCumulativeProbability(p);
            if (!closeEnough(x, x2, 1e-12, 1e-12)) {
                throw new RuntimeException("[oracle:anchor-fresh] metamorphic violation: fresh identical distribution disagrees input="
                        + p + " lhs=" + x + " rhs=" + x2);
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

    private static void exploreDistribution(double mean, double sd, FuzzedDataProvider data) throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(mean, sd);

        int mode = data.consumeInt(0, 2);
        double targetX;
        if (mode == 0) {
            targetX = mean + 2.0d * sd;
        } else if (mode == 1) {
            targetX = mean - 2.0d * sd;
        } else {
            int num = data.consumeInt(-20, 20);
            targetX = mean + (num / 10.0d) * sd;
        }

        double p = normal.cumulativeProbability(targetX);
        if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
            return;
        }

        double inv = normal.inverseCumulativeProbability(p);

        /* Contract asserted: inverseCumulativeProbability is the inverse of cumulativeProbability
         * on valid in-range probabilities. We build p from the real cumulativeProbability, so the
         * input is valid by construction. A throw-deleting or branch-skipping patch can produce a
         * wrong finite inverse; the round-trip below must still recover p for every correct build. */
        double roundTrip = normal.cumulativeProbability(inv);
        if (!closeEnough(roundTrip, p, 1e-10, 1e-8)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: cdf(icdf(p)) != p input="
                    + p + " mean=" + mean + " sd=" + sd + " inv=" + inv + " lhs=" + roundTrip + " rhs=" + p);
        }

        /* Independent consistency check using a second, identically constructed real object:
         * two fresh NormalDistributionImpl instances with the same parameters must agree on the
         * same inverse probability query. */
        NormalDistribution fresh = new NormalDistributionImpl(mean, sd);
        double inv2 = fresh.inverseCumulativeProbability(p);
        if (!closeEnough(inv, inv2, 1e-12, 1e-10)) {
            throw new RuntimeException("[oracle:fresh-agree] metamorphic violation: identical distributions disagree input="
                    + p + " mean=" + mean + " sd=" + sd + " lhs=" + inv + " rhs=" + inv2);
        }
    }

    private static double consumeFiniteMean(FuzzedDataProvider data) {
        int scaled = data.consumeInt(-10000, 10000);
        return scaled / 100.0d;
    }

    private static double consumePositiveSd(FuzzedDataProvider data) {
        int scaled = data.consumeInt(1, 10000);
        return scaled / 100.0d;
    }

    private static boolean closeEnough(double a, double b, double abs, double rel) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= abs || diff <= rel * Math.max(1.0d, scale);
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid")
                    || name.contains("OutOfRange") || name.contains("NoData")
                    || name.contains("NotPositive") || name.contains("NotFinite")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean sawMathException = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            for (StackTraceElement ste : cur.getStackTrace()) {
                String cls = ste.getClassName();
                String method = ste.getMethodName();
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                        && "bracket".equals(method)) {
                    return sawMathException || (t instanceof MathException);
                }
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
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,731 chars, 1 new)</summary>

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

Codebase: `Math_85_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,7 @@ public class UnivariateRealSolverUtils {
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if (fa * upperBound >= 0.0 ) {
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `bracket`:
<signature>
public static double[] bracket(UnivariateRealFunction function, double initial, double lowerBound, double upperBound, int maximumIterations) throws ConvergenceException, FunctionEvaluationException
</signature>
<code>
    public static double[] bracket(UnivariateRealFunction function,
            double initial, double lowerBound, double upperBound, 
            int maximumIterations) throws ConvergenceException, 
            FunctionEvaluationException {
        
        if (function == null) {
            throw MathRuntimeException.createIllegalArgumentException("function is null");
        }
        if (maximumIterations <= 0)  {
            throw MathRuntimeException.createIllegalArgumentException(
                  "bad value for maximum iterations number: {0}", maximumIterations);
        }
        if (initial < lowerBound || initial > upperBound || lowerBound >= upperBound) {
            throw MathRuntimeException.createIllegalArgumentException(
                  "invalid bracketing parameters:  lower bound={0},  initial={1}, upper bound={2}",
                  lowerBound, initial, upperBound);
        }
        double a = initial;
        double b = initial;
        double fa;
        double fb;
        int numIterations = 0 ;
    
        do {
            a = Math.max(a - 1.0, lowerBound);
            b = Math.min(b + 1.0, upperBound);
            fa = function.value(a);
            
            fb = function.value(b);
            numIterations++ ;
        } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                ((a > lowerBound) || (b < upperBound)));
   
        if (fa * fb >= 0.0 ) {
            throw new ConvergenceException(
                      "number of iterations={0}, maximum iterations={1}, " +
                      "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +
                      "final b value={6}, f(a)={7}, f(b)={8}",
                      numIterations, maximumIterations, initial,
                      lowerBound, upperBound, a, b, fa, fb);
        }
        
        return new double[]{a, b};
    }
</code>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NormalDistribution`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.MathException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `bracket` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: org.apache.commons.math.MathException
message: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0
thrown_at: org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability(AbstractContinuousDistribution.java:104)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.distribution.NormalDistributionTest" method="testMath280">
    public void testMath280() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0,1);
        double result = normal.inverseCumulativeProbability(0.9772498680518209);
        assertEquals(2.0, result, 1.0e-12);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.distribution.NormalDistributionTest::testMath280
org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.distribution.NormalDistributionTest">
// --- setUp() (test setup) ---
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setTolerance(1E-6);
    }
</test_support>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f the function.
  - @param x0 the lower bound for the interval.
  - @param x1 the upper bound for the interval.
  - @throws ConvergenceException if the iteration count was exceeded
  - @throws FunctionEvaluationException if an error occurs evaluating
  - @throws IllegalArgumentException if f is null or the endpoints do not
  - @param f the function
  - @param x0 the lower bound for the interval
  - @param x1 the upper bound for the interval
  - @param absoluteAccuracy the accuracy to be used by the solver
  - @throws ConvergenceException if the iteration count is exceeded
  - @throws FunctionEvaluationException if an error occurs evaluating the
  - @throws IllegalArgumentException if f is null, the endpoints do not
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  bracket(UnivariateRealFunction function, 
            double initial, double lowerBound, double upperBound) / (UnivariateRealFunction function,
            double initial, double lowerBound, double upperBound, 
            int maximumIterations)
  solve(UnivariateRealFunction f, double x0, double x1) / (UnivariateRealFunction f, double x0, double x1,
            double absoluteAccuracy)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor
- org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
- org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- org.apache.commons.math.distribution.NormalDistributionImpl.inverseCumulativeProbability
Crashes already found:
- org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
Check FAMILIES already covered by accepted harnesses: {anchor-fresh, anchor-roundtrip, fresh-agree, roundtrip}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
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
<details open><summary>▸ Output (~6,263 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double ORACLE_TOL = 1.0e-12;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        int k = data.consumeInt(2, 12);
        double eps = Math.pow(10.0, -data.consumeInt(3, 8));
        if (data.consumeBoolean()) {
            eps *= 0.5;
        }

        double xLo = k - eps;
        double xMid = k;
        double xHi = k + eps;

        double pLo;
        double pMid;
        double pHi;
        try {
            pLo = normal.cumulativeProbability(xLo);
            pMid = normal.cumulativeProbability(xMid);
            pHi = normal.cumulativeProbability(xHi);
        } catch (Throwable t) {
            return;
        }

        if (!(pLo < pMid && pMid < pHi)) {
            return;
        }

        double invLo;
        double invMid;
        double invHi;
        try {
            invLo = normal.inverseCumulativeProbability(pLo);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:ordered-neighbors] metamorphic violation: inverseCumulativeProbability rejected lower neighbor for valid probability pLo=" + pLo + " xLo=" + xLo, t);
            }
            return;
        }

        try {
            invMid = normal.inverseCumulativeProbability(pMid);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:ordered-neighbors] metamorphic violation: inverseCumulativeProbability rejected exact interior point constructed from cumulativeProbability(" + xMid + "), pMid=" + pMid, t);
            }
            return;
        }

        try {
            invHi = normal.inverseCumulativeProbability(pHi);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:ordered-neighbors] metamorphic violation: inverseCumulativeProbability rejected upper neighbor for valid probability pHi=" + pHi + " xHi=" + xHi, t);
            }
            return;
        }

        /* Contract/oracle:
         * For a correct distribution implementation, inverseCumulativeProbability is monotone in p.
         * We construct pLo < pMid < pHi using the same distribution's cumulativeProbability on xLo < xMid < xHi,
         * so the returned quantiles must satisfy invLo <= invMid <= invHi. This catches boundary-condition
         * regressions around the patched bracket check even if a band-aid merely suppresses the known throw.
         */
        if (invLo > invMid + ORACLE_TOL || invMid > invHi + ORACLE_TOL) {
            throw new RuntimeException(
                "[oracle:ordered-neighbors] metamorphic violation: expected ordered quantiles for ordered probabilities"
                    + " k=" + k
                    + " eps=" + eps
                    + " pLo=" + pLo
                    + " pMid=" + pMid
                    + " pHi=" + pHi
                    + " invLo=" + invLo
                    + " invMid=" + invMid
                    + " invHi=" + invHi);
        }

        /* Additional boundary-focused check:
         * xMid is an exact integer >= 2, which is the shape that drives bracket to an endpoint root.
         * Since pMid was produced by cumulativeProbability(xMid) on the same object, a correct inverse
         * must recover xMid to within floating-point error.
         */
        if (Math.abs(invMid - xMid) > 1.0e-9) {
            throw new RuntimeException(
                "[oracle:integer-root] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x"
                    + " x=" + xMid
                    + " p=" + pMid
                    + " inv=" + invMid);
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            normal.inverseCumulativeProbability(ANCHOR_P);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                return;
            }
        }
    }

    private static boolean isRootCauseMathException(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")
            || hasFrame(t, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability");
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if (className.equals(ste.getClassName()) && methodName.equals(ste.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return hasFrame(cause, className, methodName);
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.endsWith("IllegalArgumentException")
            || n.endsWith("InvalidRepresentationException")
            || n.endsWith("InvalidMatrixException")
            || n.endsWith("NotStrictlyPositiveException")
            || n.endsWith("OutOfRangeException");
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:ordered-neighbors] metamorphic violation: inverseCumulativeProbability rejected exact interior point constructed from cumulativeProbability(2.0), pMid=0.97724986805…

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,965 chars, 1 new)</summary>

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

Codebase: `Math_85_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,7 @@ public class UnivariateRealSolverUtils {
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if (fa * upperBound >= 0.0 ) {
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
</source_imports>

Function `bracket`:
<signature>
public static double[] bracket(UnivariateRealFunction function, double initial, double lowerBound, double upperBound, int maximumIterations) throws ConvergenceException, FunctionEvaluationException
</signature>
<code>
    public static double[] bracket(UnivariateRealFunction function,
            double initial, double lowerBound, double upperBound, 
            int maximumIterations) throws ConvergenceException, 
            FunctionEvaluationException {
        
        if (function == null) {
            throw MathRuntimeException.createIllegalArgumentException("function is null");
        }
        if (maximumIterations <= 0)  {
            throw MathRuntimeException.createIllegalArgumentException(
                  "bad value for maximum iterations number: {0}", maximumIterations);
        }
        if (initial < lowerBound || initial > upperBound || lowerBound >= upperBound) {
            throw MathRuntimeException.createIllegalArgumentException(
                  "invalid bracketing parameters:  lower bound={0},  initial={1}, upper bound={2}",
                  lowerBound, initial, upperBound);
        }
        double a = initial;
        double b = initial;
        double fa;
        double fb;
        int numIterations = 0 ;
    
        do {
            a = Math.max(a - 1.0, lowerBound);
            b = Math.min(b + 1.0, upperBound);
            fa = function.value(a);
            
            fb = function.value(b);
            numIterations++ ;
        } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                ((a > lowerBound) || (b < upperBound)));
   
        if (fa * fb >= 0.0 ) {
            throw new ConvergenceException(
                      "number of iterations={0}, maximum iterations={1}, " +
                      "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +
                      "final b value={6}, f(a)={7}, f(b)={8}",
                      numIterations, maximumIterations, initial,
                      lowerBound, upperBound, a, b, fa, fb);
        }
        
        return new double[]{a, b};
    }
</code>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NormalDistribution`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.MathException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `bracket` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: org.apache.commons.math.MathException
message: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0
thrown_at: org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability(AbstractContinuousDistribution.java:104)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.distribution.NormalDistributionTest" method="testMath280">
    public void testMath280() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0,1);
        double result = normal.inverseCumulativeProbability(0.9772498680518209);
        assertEquals(2.0, result, 1.0e-12);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.distribution.NormalDistributionTest::testMath280
org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000, final a value=0, final b value=2, f(a)=-0.477, f(b)=0
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.distribution.NormalDistributionTest">
// --- setUp() (test setup) ---
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setTolerance(1E-6);
    }
</test_support>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param f the function.
  - @param x0 the lower bound for the interval.
  - @param x1 the upper bound for the interval.
  - @throws ConvergenceException if the iteration count was exceeded
  - @throws FunctionEvaluationException if an error occurs evaluating
  - @throws IllegalArgumentException if f is null or the endpoints do not
  - @param f the function
  - @param x0 the lower bound for the interval
  - @param x1 the upper bound for the interval
  - @param absoluteAccuracy the accuracy to be used by the solver
  - @throws ConvergenceException if the iteration count is exceeded
  - @throws FunctionEvaluationException if an error occurs evaluating the
  - @throws IllegalArgumentException if f is null, the endpoints do not
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  bracket(UnivariateRealFunction function, 
            double initial, double lowerBound, double upperBound) / (UnivariateRealFunction function,
            double initial, double lowerBound, double upperBound, 
            int maximumIterations)
  solve(UnivariateRealFunction f, double x0, double x1) / (UnivariateRealFunction f, double x0, double x1,
            double absoluteAccuracy)

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor
- org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
- org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- org.apache.commons.math.distribution.NormalDistributionImpl.inverseCumulativeProbability
Crashes already found:
- org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
Check FAMILIES already covered by accepted harnesses: {anchor-fresh, anchor-roundtrip, fresh-agree, integer-root, ordered-neighbors, roundtrip}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
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
<details open><summary>▸ Output (~7,311 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int root = data.consumeInt(2, 1000);
        int lowerSlack = data.consumeInt(0, 8);
        int upperSlack = data.consumeInt(0, 8);
        int maxIterations = data.consumeInt(1, 32);

        double rootD = root;
        double initial = rootD - 1.0;
        double lowerBound = Math.max(0.0, rootD - 2.0 - lowerSlack);
        double upperBound = rootD + upperSlack;

        if (!(lowerBound < upperBound)) {
            return;
        }
        if (initial < lowerBound || initial > upperBound) {
            return;
        }

        PolynomialFunction f = new PolynomialFunction(new double[] { -rootD, 1.0 });

        checkConstructedBracket(f, rootD, initial, lowerBound, upperBound, maxIterations);
        checkOverloadAgreement(f, initial, lowerBound, upperBound);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new IllegalStateException("[oracle:anchor-value] inverseCumulativeProbability returned " + result + " instead of 2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t)) {
                throwAsRuntime(t);
            }
            // Known symptom already covered elsewhere; do not re-report it from this harness.
        }
    }

    private static void checkConstructedBracket(PolynomialFunction f, double root, double initial,
                                                double lowerBound, double upperBound, int maxIterations) {
        final double[] bracket;
        try {
            bracket = UnivariateRealSolverUtils.bracket(f, initial, lowerBound, upperBound, maxIterations);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (passesThroughBracket(t)) {
                throw new IllegalStateException(
                        "[oracle:valid-bracket] valid-by-construction input was rejected on bracket path: root=" + root
                                + " initial=" + initial + " lower=" + lowerBound + " upper=" + upperBound
                                + " maxIterations=" + maxIterations,
                        t);
            }
            return;
        }

        if (bracket == null || bracket.length != 2) {
            throw new IllegalStateException("[oracle:valid-bracket] bracket returned malformed interval");
        }

        double a = bracket[0];
        double b = bracket[1];

        try {
            double fa = f.value(a);
            double fb = f.value(b);

            // Contract from bracket: returned endpoints must bracket a root (or hit it exactly),
            // so f(a) * f(b) <= 0 and the known root must lie inside [a, b].
            if (!(a <= root && root <= b) || fa * fb > 0.0d) {
                throw new IllegalStateException(
                        "[oracle:valid-bracket] returned interval does not bracket the constructed root: a=" + a
                                + " b=" + b + " fa=" + fa + " fb=" + fb + " root=" + root);
            }

            // For this specific valid construction, the first expansion evaluates b = initial + 1 = root,
            // so a correct implementation must return that endpoint as the exact root when it succeeds.
            if (Math.abs(b - root) != 0.0d) {
                throw new IllegalStateException(
                        "[oracle:endpoint-root] expected right endpoint to hit constructed root exactly: b=" + b
                                + " root=" + root);
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throwAsRuntime(t);
            }
        }
    }

    private static void checkOverloadAgreement(PolynomialFunction f, double initial,
                                               double lowerBound, double upperBound) {
        double[] fourArg = null;
        double[] fiveArg = null;
        Throwable fourErr = null;
        Throwable fiveErr = null;

        try {
            fourArg = UnivariateRealSolverUtils.bracket(f, initial, lowerBound, upperBound);
        } catch (Throwable t) {
            fourErr = t;
        }

        try {
            fiveArg = UnivariateRealSolverUtils.bracket(f, initial, lowerBound, upperBound, Integer.MAX_VALUE);
        } catch (Throwable t) {
            fiveErr = t;
        }

        if (fourErr != null || fiveErr != null) {
            if (isCleanRejection(fourErr) || isCleanRejection(fiveErr)) {
                return;
            }
            if (passesThroughBracket(fourErr) || passesThroughBracket(fiveErr)) {
                throw new IllegalStateException(
                        "[oracle:overload-agree] equivalent bracket overloads disagreed by rejection on valid input",
                        fourErr != null ? fourErr : fiveErr);
            }
            return;
        }

        if (fourArg == null || fiveArg == null || fourArg.length != 2 || fiveArg.length != 2) {
            throw new IllegalStateException("[oracle:overload-agree] equivalent overloads returned malformed results");
        }

        // Documented same-name overloads with matching docs must agree on equivalent inputs.
        if (Double.compare(fourArg[0], fiveArg[0]) != 0 || Double.compare(fourArg[1], fiveArg[1]) != 0) {
            throw new IllegalStateException(
                    "[oracle:overload-agree] bracket overloads disagree: fourArg=[" + fourArg[0] + ", " + fourArg[1]
                            + "] fiveArg=[" + fiveArg[0] + ", " + fiveArg[1] + "]");
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean passesThroughBracket(Throwable t) {
        if (t == null) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsRuntime(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new IllegalStateException(t);
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IllegalStateException: [oracle:valid-bracket] valid-by-construction input was rejected on bracket path: root=2.0 initial=1.0 lower=0.0 upper=2.0 maxIterations=1

---
## [9] ⚙️ corpus-seed · `attempt_001`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [10] ⚙️ corpus-seed · `attempt_002`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [11] ⚙️ corpus-seed · `attempt_003`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [12] ⚙️ patched-fuzz · `attempt_001`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Math_85_buggy/fuzz/attempt_001/crashes/crash-e656fa8b081bd0e20a240dbf7d1590eb346db259

---
## [13] ⚙️ patched-fuzz · `attempt_002`
**output:** **FIRED — [oracle:integer-root] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x x=10.0 p=0.9999999999999982 inv=7.9999995**
- reproducing_input_file: /tmp/d4j/Math_85_buggy/fuzz/attempt_002/crashes/crash-0580e5539d5608d3052a705275bb1529c1a82826

---
## [14] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**
