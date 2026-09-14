# Pipeline trace — Math-85

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 5 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java	
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +
```

25 sequential steps — 6 LLM calls, 19 deterministic. Read top to bottom.

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
<details><summary>▸ output — modified_files": [ (~5,666 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java\t\n+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java\n@@ -195,7 +195,7 @@\n         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && \n                 ((a > lowerBound) || (b < upperBound)));\n    \n-        if (fa * fb >= 0.0 ) {\n+        if((fa> fb)&&((fa*fb)>=0.0)){\n             throw new ConvergenceException(\n                       \"number of iterations={0}, maximum iterations={1}, \" +\n                       \"initial={2}, lower bound={3}, upper bound={4}, final a value={5}, \" +\n",
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
<details><summary>▸ Prompt (2 message(s), ~26,784 chars)</summary>

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
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
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
<details open><summary>▸ Output (~6,489 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases += data.consumeInt(0, 4);
        }

        for (int i = 0; i < cases; i++) {
            double mean = data.consumeInt(-100, 100);

            // Root-cause-shaped input: build p from the real public API so that
            // inverseCumulativeProbability should recover x = mean + 2 exactly
            // for a correct implementation, while the buggy version can fail in
            // the internal bracketing path when the bracket endpoint is a root.
            NormalDistribution dist = new NormalDistributionImpl(mean, 1.0);
            double expectedX;
            double p;
            try {
                expectedX = mean + 2.0;
                p = dist.cumulativeProbability(expectedX);
            } catch (Throwable t) {
                if (shouldPropagate(t)) {
                    sneakyThrow(t);
                }
                continue;
            }

            exerciseAndCheck(dist, p, expectedX);

            // Additional nearby probabilities keep driving the same public entry
            // point with varied surrounding values; any rejection is swallowed.
            int nearbyCount = data.consumeInt(0, 3);
            for (int j = 0; j < nearbyCount; j++) {
                double delta = data.consumeInt(-1000, 1000) / 1_000_000.0;
                double nearbyP = p + delta;
                if (nearbyP <= 0.0) {
                    nearbyP = Double.MIN_VALUE;
                } else if (nearbyP >= 1.0) {
                    nearbyP = 1.0 - 1e-16;
                }
                exerciseWithoutOracle(dist, nearbyP);
            }
        }
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        exerciseAndCheck(normal, 0.9772498680518209, 2.0);
    }

    private static void exerciseAndCheck(NormalDistribution dist, double p, double expectedX) {
        try {
            double reported = dist.inverseCumulativeProbability(p);

            // Contract/metamorphic check:
            // inverseCumulativeProbability(p) should return the quantile x for p.
            // Here p was constructed from the same real distribution as
            // cumulativeProbability(expectedX), so a correct implementation must
            // recover expectedX (within solver tolerance). A throw-deleting or
            // silently-wrong patch would violate this observable relation.
            if (Math.abs(reported - expectedX) > 1.0e-6) {
                throw new RuntimeException(
                    "[oracle:inv-roundtrip] metamorphic violation: inverse(cdf(x)) != x"
                        + " p=" + p
                        + " expected=" + expectedX
                        + " reported=" + reported);
            }

            // Independent consistency cross-check using only real library calls:
            // cdf(inverse(p)) must return p for valid p in (0,1).
            double roundTrip = dist.cumulativeProbability(reported);
            if (Math.abs(roundTrip - p) > 1.0e-6) {
                throw new RuntimeException(
                    "[oracle:cdf-inv] metamorphic violation: cdf(inverse(p)) != p"
                        + " p=" + p
                        + " inverse=" + reported
                        + " roundTrip=" + roundTrip);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void exerciseWithoutOracle(NormalDistribution dist, double p) {
        try {
            double x = dist.inverseCumulativeProbability(p);
            try {
                double back = dist.cumulativeProbability(x);
                if (Double.isNaN(back) || Math.abs(back - p) > 1.0e-6) {
                    throw new RuntimeException(
                        "[oracle:cdf-inv-nearby] metamorphic violation: cdf(inverse(p)) != p"
                            + " p=" + p
                            + " inverse=" + x
                            + " roundTrip=" + back);
                }
            } catch (Throwable ignored) {
                // If the post-condition read throws, the check does not apply.
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasRelevantFrame(t);
    }

    private static boolean hasRelevantFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace != null) {
                for (StackTraceElement e : trace) {
                    String cls = e.getClassName();
                    String m = e.getMethodName();
                    if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                            && "bracket".equals(m))
                        || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls)
                            && "value".equals(m))
                        || ("org.apache.commons.math.ConvergenceException".equals(cls)
                            && "<init>".equals(m))
                        || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                            && ("createIllegalArgumentException".equals(m) || "buildMessage".equals(m)))) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
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
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,746 chars, 1 new)</summary>

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
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
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
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseAndCheck
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
Check FAMILIES already covered by accepted harnesses: {cdf-inv, cdf-inv-nearby, inv-roundtrip}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
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
<details open><summary>▸ Output (~8,249 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int root = data.consumeInt(-1000, 1000);
        int scaleInt = data.consumeInt(1, 16);
        int extraUpper = data.consumeInt(0, 1000);
        int maxIterations = data.consumeInt(1, 32);

        exerciseBracketBoundary(root, scaleInt, extraUpper, maxIterations);

        double p = seededProbability(data);
        exerciseNormalSymmetry(p, data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCauseFromInverse(t)) {
                return;
            }
        }
    }

    private static void exerciseBracketBoundary(int root, int scaleInt, int extraUpper, int maximumIterations) {
        double r = root;
        double scale = scaleInt;
        PolynomialFunction f = new PolynomialFunction(new double[] { -scale * r, scale });
        double initial = r - 1.0d;
        double lower = r - 2.0d;
        double upper = r + 2.0d + extraUpper;

        double[] withMax;
        try {
            withMax = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maximumIterations);
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCauseFromBracket(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double[] defaultMax;
        try {
            defaultMax = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCauseFromBracket(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (withMax == null || withMax.length != 2 || defaultMax == null || defaultMax.length != 2) {
            throw new RuntimeException("[oracle:bracket-shape] metamorphic violation: bracket overload returned malformed array");
        }

        double fa = valueSafely(f, withMax[0]);
        double fb = valueSafely(f, withMax[1]);
        if (!Double.isNaN(fa) && !Double.isNaN(fb) && fa * fb > 0.0d) {
            throw new RuntimeException("[oracle:bracket-sign] metamorphic violation: returned interval does not bracket a root root="
                    + r + " interval=[" + withMax[0] + "," + withMax[1] + "] fa=" + fa + " fb=" + fb);
        }

        if (Math.abs(withMax[0] - defaultMax[0]) > 0.0d || Math.abs(withMax[1] - defaultMax[1]) > 0.0d) {
            throw new RuntimeException("[oracle:bracket-overload] metamorphic violation: bracket overloads disagree root="
                    + r + " max=" + maximumIterations + " lhs=[" + withMax[0] + "," + withMax[1]
                    + "] rhs=[" + defaultMax[0] + "," + defaultMax[1] + "]");
        }
    }

    private static void exerciseNormalSymmetry(double p, FuzzedDataProvider data) {
        if (!(p > 1.0e-12d && p < 1.0d - 1.0e-12d)) {
            return;
        }

        double mu = data.consumeInt(-1000, 1000);
        double sigma = data.consumeInt(1, 1000);

        NormalDistribution std = new NormalDistributionImpl(0, 1);
        NormalDistribution shifted = new NormalDistributionImpl(mu, sigma);

        double x1;
        double x2;
        try {
            x1 = std.inverseCumulativeProbability(p);
            x2 = std.inverseCumulativeProbability(1.0d - p);
        } catch (Throwable t) {
            if (isValidation(t) || isRootCauseFromInverse(t)) {
                return;
            }
            return;
        }

        /* For a correct normal distribution implementation, quantiles are symmetric:
           Q(1-p) = -Q(p) for N(0,1). A patch that merely suppresses the historical throw
           but returns an arbitrary value will violate this relation. */
        double symErr = Math.abs(x1 + x2);
        double symTol = 1.0e-9d * (1.0d + Math.max(Math.abs(x1), Math.abs(x2)));
        if (symErr > symTol) {
            throw new RuntimeException("[oracle:normal-symmetry] metamorphic violation: p=" + p
                    + " q(p)=" + x1 + " q(1-p)=" + x2 + " err=" + symErr);
        }

        double shiftedQ;
        try {
            shiftedQ = shifted.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isValidation(t) || isRootCauseFromInverse(t)) {
                return;
            }
            return;
        }

        /* Location-scale family contract: if X ~ N(mu,sigma), then its quantile is
           mu + sigma * Q_std(p). This is an independent real-library cross-check. */
        double expected = mu + sigma * x1;
        double tol = 1.0e-8d * (1.0d + Math.max(Math.abs(expected), Math.abs(shiftedQ)));
        if (Math.abs(shiftedQ - expected) > tol) {
            throw new RuntimeException("[oracle:location-scale] metamorphic violation: p=" + p
                    + " mu=" + mu + " sigma=" + sigma + " shifted=" + shiftedQ + " expected=" + expected);
        }
    }

    private static double seededProbability(FuzzedDataProvider data) {
        try {
            NormalDistribution std = new NormalDistributionImpl(0, 1);
            int k = data.consumeInt(2, 6);
            if (data.consumeBoolean()) {
                return std.cumulativeProbability(k);
            }
            double delta = data.consumeInt(-1000, 1000) / 1000000.0d;
            double p = 0.9772498680518209d + delta;
            if (p <= 1.0e-12d) {
                p = 1.0e-12d;
            } else if (p >= 1.0d - 1.0e-12d) {
                p = 1.0d - 1.0e-12d;
            }
            return p;
        } catch (MathException e) {
            return 0.9772498680518209d;
        }
    }

    private static double valueSafely(PolynomialFunction f, double x) {
        try {
            return f.value(x);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromBracket(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(ste.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromInverse(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(ste.getClassName())
                    && "inverseCumulativeProbability".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
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
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=1, initial=-1,001, lower bound=-1,002, upper bound=-998, final a value=-1,002, fina…

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~29,168 chars, 1 new)</summary>

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
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
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
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseAndCheck
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseBracketBoundary
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked
- org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
- org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- org.apache.commons.math.distribution.NormalDistributionImpl.inverseCumulativeProbability
Crashes already found:
- org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
Check FAMILIES already covered by accepted harnesses: {bracket-overload, bracket-shape, bracket-sign, cdf-inv, cdf-inv-nearby, inv-roundtrip, location-scale, normal-symmetry}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,330 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        probeValidationPaths(data);

        exerciseBoundaryRootWithRealFunction(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isKnownInverseCrash(t)) {
                return;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void probeValidationPaths(FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        try {
            UnivariateRealSolverUtils.bracket(null, 0.5d, 0.0d, 2.0d, 1);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                return;
            }
        }

        int badIterations = data.consumeBoolean() ? 0 : -Math.abs(data.consumeInt());
        try {
            UnivariateRealSolverUtils.bracket(sin, 0.5d, 0.0d, 2.0d, badIterations);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                return;
            }
        }

        double lower = 1.0d;
        double upper = 0.0d;
        double initial = data.consumeBoolean() ? -1.0d : 2.0d;
        try {
            UnivariateRealSolverUtils.bracket(sin, initial, lower, upper, 1);
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseBoundaryRootWithRealFunction(FuzzedDataProvider data) {
        UnivariateRealFunction sin = new SinFunction();

        double initial = data.consumeInt(0, 1_000_000) / 1_000_000.0d;
        double upper = 1.1d + (data.consumeInt(0, 1_800_000) / 1_000_000.0d);
        int maxIterations = data.consumeInt(1, 8);

        double[] bracket;
        try {
            bracket = UnivariateRealSolverUtils.bracket(sin, initial, 0.0d, upper, maxIterations);
        } catch (Throwable t) {
            if (isRootCauseFromBracket(t)) {
                throwUnchecked(t);
                return;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (bracket == null || bracket.length != 2) {
            throw new RuntimeException("[oracle:bracket-array] metamorphic violation: bracket must return two endpoints");
        }

        double a = bracket[0];
        double b = bracket[1];

        if (!(0.0d <= a && a <= initial && initial <= b && b <= upper)) {
            throw new RuntimeException("[oracle:bounds-order] metamorphic violation: returned interval not ordered within requested bounds inputInitial="
                    + initial + " upper=" + upper + " a=" + a + " b=" + b);
        }

        try {
            UnivariateRealSolver solver = new BisectionSolver();
            double root = solver.solve(sin, a, b);

            /*
             * Contract asserted: bracket returns an interval that brackets a root.
             * Independent check: a real solver run on that returned interval must
             * recover a point in the same interval whose function value is ~0.
             * A patch that merely deletes the throw but returns a non-bracketing
             * interval will violate this observable relation.
             */
            double valueAtRoot = sin.value(root);
            if (root < a || root > b || Math.abs(valueAtRoot) > 1.0e-6d) {
                throw new RuntimeException("[oracle:solver-from-bracket] metamorphic violation: inputInitial="
                        + initial + " upper=" + upper + " a=" + a + " b=" + b
                        + " root=" + root + " f(root)=" + valueAtRoot);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isKnownInverseCrash(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrame(t, "org.apache.commons.math.distribution.AbstractContinuousDistribution",
                "inverseCumulativeProbability");
    }

    private static boolean isRootCauseFromBracket(Throwable t) {
        if (!(t instanceof ConvergenceException) && !(t instanceof MathException)) {
            return false;
        }
        return hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")
                || hasFrame(t, "org.apache.commons.math.ConvergenceException", "<init>");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        return hasFrame(t, "org.apache.commons.math.MathRuntimeException", "createIllegalArgumentException")
                || hasFrame(t, "org.apache.commons.math.MathRuntimeException", "buildMessage");
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement e : t.getStackTrace()) {
            if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
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
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=1, initial=0, lower bound=0, upper bound=1.1, final a value=0, final b value=1, f(a)=0, f(b)=0.841

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~29,553 chars, 1 new)</summary>

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
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
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
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseAndCheck
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseBoundaryRootWithRealFunction
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseBracketBoundary
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked
- org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
- org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- org.apache.commons.math.distribution.NormalDistributionImpl.inverseCumulativeProbability
Crashes already found:
- org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked
- org.apache.commons.math.ConvergenceException@org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
Check FAMILIES already covered by accepted harnesses: {bounds-order, bracket-array, bracket-overload, bracket-shape, bracket-sign, cdf-inv, cdf-inv-nearby, inv-roundtrip, location-scale, normal-symmetry, solver-from-bracket}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked; org.apache.commons.math.ConvergenceException@org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~7,917 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mean = data.consumeInt(-20, 20);
        int sigma = data.consumeInt(1, 8);
        int extra = data.consumeInt(1, 4);
        int gap = data.consumeInt(1, 3);

        exerciseLocationScale(mean, sigma, extra, gap);

        int mean2 = data.consumeInt(-10, 10);
        int sigma2 = data.consumeInt(1, 5);
        int step = data.consumeInt(1, 3);
        exerciseMonotoneQuantiles(mean2, sigma2, step);
    }

    private static void runAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0, 1);
        double p = 0.9772498680518209d;
        try {
            double q = normal.inverseCumulativeProbability(p);
            if (Math.abs(q - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-quantile] metamorphic violation: inverseCumulativeProbability(cdf(2)) should recover 2.0 input="
                        + p + " lhs=" + q + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionThrowable(t)) {
                throw new RuntimeException("[oracle:anchor-quantile] metamorphic violation: valid inverse(cdf(x)) rejected for x=2.0 p="
                        + p, t);
            }
        }
    }

    private static void exerciseLocationScale(int mean, int sigma, int extra, int gap) {
        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sigma);

        double x1 = mean + sigma + extra;
        double x2 = x1 + gap;

        double p1;
        double p2;
        try {
            p1 = dist.cumulativeProbability(x1);
            p2 = dist.cumulativeProbability(x2);
        } catch (Throwable t) {
            return;
        }

        if (!(p1 > 0.0d && p1 < 1.0d && p2 > 0.0d && p2 < 1.0d && p1 < p2)) {
            return;
        }

        double q1;
        double q2;
        try {
            q1 = dist.inverseCumulativeProbability(p1);
            q2 = dist.inverseCumulativeProbability(p2);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionThrowable(t)) {
                throw new RuntimeException("[oracle:recover-order] metamorphic violation: valid quantiles from cumulativeProbability were rejected input=("
                        + mean + "," + sigma + "," + x1 + "," + x2 + ")", t);
            }
            return;
        }

        /*
         * Documented contract used for this oracle:
         * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
         * We build p1 and p2 by construction from cumulativeProbability on the SAME real distribution,
         * so a correct implementation must recover ordered quantiles q1 <= q2 for x1 < x2.
         * A throw-deleting or branch-skipping patch can still leave ordering wrong; this checks that
         * related quantities computed two ways agree, not merely that no exception was thrown.
         */
        if (!(q1 <= q2)) {
            throw new RuntimeException("[oracle:recover-order] metamorphic violation: quantile order reversed input=("
                    + mean + "," + sigma + "," + p1 + "," + p2 + ") lhs=(" + q1 + "," + q2 + ") rhs=ordered");
        }

        double tol1 = 1.0e-9d * Math.max(1.0d, Math.abs(x1));
        double tol2 = 1.0e-9d * Math.max(1.0d, Math.abs(x2));
        if (Math.abs(q1 - x1) > tol1 || Math.abs(q2 - x2) > tol2) {
            throw new RuntimeException("[oracle:recover-order] metamorphic violation: inverse(cdf(x)) did not recover source points input=("
                    + mean + "," + sigma + "," + x1 + "," + x2 + ") lhs=(" + q1 + "," + q2 + ") rhs=(" + x1 + "," + x2 + ")");
        }
    }

    private static void exerciseMonotoneQuantiles(int mean, int sigma, int step) {
        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sigma);
        double x = mean + sigma + 1.0d;
        double y = x + step;

        double pLow;
        double pHigh;
        try {
            pLow = dist.cumulativeProbability(x);
            pHigh = dist.cumulativeProbability(y);
        } catch (Throwable t) {
            return;
        }

        if (!(pLow > 0.0d && pLow < 1.0d && pHigh > 0.0d && pHigh < 1.0d && pLow < pHigh)) {
            return;
        }

        double qLow;
        try {
            qLow = dist.inverseCumulativeProbability(pLow);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionThrowable(t)) {
                throw new RuntimeException("[oracle:monotone-quantile] metamorphic violation: valid lower quantile rejected input=("
                        + mean + "," + sigma + "," + x + "," + pLow + ")", t);
            }
            return;
        }

        double qHigh;
        try {
            qHigh = dist.inverseCumulativeProbability(pHigh);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionThrowable(t)) {
                throw new RuntimeException("[oracle:monotone-quantile] metamorphic violation: valid upper quantile rejected input=("
                        + mean + "," + sigma + "," + y + "," + pHigh + ")", t);
            }
            return;
        }

        if (!(qLow < qHigh)) {
            throw new RuntimeException("[oracle:monotone-quantile] metamorphic violation: inverse should preserve strict order for probabilities built from ordered x values input=("
                    + mean + "," + sigma + "," + x + "," + y + ") lhs=(" + qLow + "," + qHigh + ") rhs=strictly-increasing");
        }
    }

    private static boolean isPatchedRegionThrowable(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!(t instanceof MathException || t instanceof RuntimeException || t instanceof Error)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls)
                    && "value".equals(method)) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isPatchedRegionThrowable(cause);
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange")
                || name.contains("NoData") || name.contains("NotStrictlyPositive")
                || name.contains("NullArgument")) {
            return true;
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isCleanRejection(cause);
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:anchor-quantile] metamorphic violation: valid inverse(cdf(x)) rejected for x=2.0 p=0.9772498680518209

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~29,787 chars, 1 new)</summary>

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
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
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
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseAndCheck
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseBoundaryRootWithRealFunction
- org.apache.commons.math.analysis.solvers.FuzzHarness.exerciseBracketBoundary
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor
- org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked
- org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
- org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- org.apache.commons.math.distribution.NormalDistributionImpl.inverseCumulativeProbability
Crashes already found:
- org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked
- org.apache.commons.math.ConvergenceException@org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- MathRuntimeException.createIllegalArgumentException(String)
- MathRuntimeException.createIllegalArgumentException(String, int)
- MathRuntimeException.createIllegalArgumentException(String, double, double, double)
- UnivariateRealFunction.value
- ConvergenceException.<init>
- MathRuntimeException.buildMessage
Check FAMILIES already covered by accepted harnesses: {anchor-quantile, bounds-order, bracket-array, bracket-overload, bracket-shape, bracket-sign, cdf-inv, cdf-inv-nearby, inv-roundtrip, location-scale, monotone-quantile, normal-symmetry, recover-order, solver-from-bracket}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): org.apache.commons.math.MathException@org.apache.commons.math.distribution.AbstractContinuousDistribution.inverseCumulativeProbability; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.throwUnchecked; org.apache.commons.math.ConvergenceException@org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.runAnchor. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,069 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // ANCHOR: exact trigger from NormalDistributionTest.testMath280.
        try {
            NormalDistribution anchor = new NormalDistributionImpl(0, 1);
            double r = anchor.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(r - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: inverseCumulativeProbability(CDF(2)) must recover 2.0 input=0.9772498680518209 lhs=" + r + " rhs=2.0");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
        } catch (Throwable t) {
            // Known buggy symptom is already covered elsewhere; swallow here.
        }

        // EXPLORE: flip the patched condition by constructing probabilities that are
        // exactly CDF(mu +/- k*sigma). These are valid by construction and tend to
        // make bracketing land with a root on one endpoint, where old/new behavior differs.
        int iterations = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < iterations; i++) {
            double mu = data.consumeInt(-100, 100) / 10.0d;
            double sigma = (1 + data.consumeInt(0, 99)) / 20.0d; // (0.05 .. 5.0]
            int k = 2 + data.consumeInt(0, 6); // moderate, non-degenerate
            boolean upperSide = data.consumeBoolean();

            NormalDistribution dist;
            try {
                dist = new NormalDistributionImpl(mu, sigma);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
                continue;
            } catch (Throwable t) {
                continue;
            }

            double sourceX = mu + (upperSide ? 1.0d : -1.0d) * k * sigma;
            double p;
            try {
                p = dist.cumulativeProbability(sourceX);
            } catch (Throwable t) {
                continue;
            }

            // Valid-by-construction fence for inverse CDF.
            if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p) || Double.isInfinite(p)) {
                continue;
            }

            double q;
            try {
                q = dist.inverseCumulativeProbability(p);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
                continue;
            } catch (Throwable t) {
                // Clean rejection / known buggy exception: do not turn it into a finding.
                continue;
            }

            // MANDATORY post-condition / independent oracle:
            // By contract, inverseCumulativeProbability(p) returns a point whose cumulative
            // probability is p. We constructed p from cumulativeProbability(sourceX), so the
            // interval probability between q and sourceX must be ~0 on any correct implementation.
            // A throw-deleting or wrong-value patch can return a finite but incorrect q; then the
            // interval probability over [min(q,sourceX), max(q,sourceX)] is observably non-zero.
            try {
                double lo = Math.min(q, sourceX);
                double hi = Math.max(q, sourceX);
                double interval = dist.cumulativeProbability(lo, hi);
                if (!(Double.isNaN(interval) || Double.isInfinite(interval)) && Math.abs(interval) > 1.0e-6d) {
                    throw new RuntimeException(
                        "[oracle:interval-zero] metamorphic violation: interval probability between x and inv(CDF(x)) must be ~0"
                            + " mu=" + mu
                            + " sigma=" + sigma
                            + " k=" + k
                            + " upperSide=" + upperSide
                            + " x=" + sourceX
                            + " p=" + p
                            + " q=" + q
                            + " interval=" + interval);
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
            } catch (Throwable t) {
                // If the post-condition read throws, the check does not apply.
            }

            // Additional consistency check using a fresh, identically-constructed object.
            // Two equivalent NormalDistributionImpl instances must compute the same quantile.
            try {
                NormalDistribution fresh = new NormalDistributionImpl(mu, sigma);
                double q2 = fresh.inverseCumulativeProbability(p);
                if (!(Double.isNaN(q) || Double.isNaN(q2) || Double.isInfinite(q) || Double.isInfinite(q2))
                        && Math.abs(q - q2) > 1.0e-12d) {
                    throw new RuntimeException(
                        "[oracle:fresh-agree] metamorphic violation: identical distributions must agree on the same quantile"
                            + " mu=" + mu
                            + " sigma=" + sigma
                            + " p=" + p
                            + " q1=" + q
                            + " q2=" + q2);
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
            } catch (Throwable t) {
                // Rejection means this check does not apply for this input.
            }
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,103 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // ANCHOR: exact trigger from NormalDistributionTest.testMath280.
        try {
            NormalDistribution anchor = new NormalDistributionImpl(0, 1);
            double r = anchor.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(r - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: inverseCumulativeProbability(CDF(2)) must recover 2.0 input=0.9772498680518209 lhs=" + r + " rhs=2.0");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
        } catch (Throwable t) {
            // Known buggy symptom is already covered elsewhere; swallow here.
        }

        // EXPLORE: flip the patched condition by constructing probabilities that are
        // exactly CDF(mu +/- k*sigma). These are valid by construction and tend to
        // make bracketing land with a root on one endpoint, where old/new behavior differs.
        int iterations = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < iterations; i++) {
            double mu = data.consumeInt(-100, 100) / 10.0d;
            double sigma = (1 + data.consumeInt(0, 99)) / 20.0d; // (0.05 .. 5.0]
            int k = 2 + data.consumeInt(0, 6); // moderate, non-degenerate
            boolean upperSide = data.consumeBoolean();

            NormalDistribution dist;
            try {
                dist = new NormalDistributionImpl(mu, sigma);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
                continue;
            } catch (Throwable t) {
                continue;
            }

            double sourceX = mu + (upperSide ? 1.0d : -1.0d) * k * sigma;
            double p;
            try {
                p = dist.cumulativeProbability(sourceX);
            } catch (Throwable t) {
                continue;
            }

            // Valid-by-construction fence for inverse CDF.
            if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p) || Double.isInfinite(p)) {
                continue;
            }

            double q;
            try {
                q = dist.inverseCumulativeProbability(p);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
                continue;
            } catch (Throwable t) {
                // Clean rejection / known buggy exception: do not turn it into a finding.
                continue;
            }

            // MANDATORY post-condition / independent oracle:
            // By contract, inverseCumulativeProbability(p) returns a point whose cumulative
            // probability is p. We constructed p from cumulativeProbability(sourceX), so the
            // interval probability between q and sourceX must be ~0 on any correct implementation.
            // A throw-deleting or wrong-value patch can return a finite but incorrect q; then the
            // interval probability over [min(q,sourceX), max(q,sourceX)] is observably non-zero.
            try {
                double lo = Math.min(q, sourceX);
                double hi = Math.max(q, sourceX);
                double interval = dist.cumulativeProbability(lo, hi);
                if (!(Double.isNaN(interval) || Double.isInfinite(interval)) && Math.abs(interval) > 1.0e-6d) {
                    throw new RuntimeException(
                        "[oracle:interval-zero] metamorphic violation: interval probability between x and inv(CDF(x)) must be ~0"
                            + " mu=" + mu
                            + " sigma=" + sigma
                            + " k=" + k
                            + " upperSide=" + upperSide
                            + " x=" + sourceX
                            + " p=" + p
                            + " q=" + q
                            + " interval=" + interval);
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
            } catch (Throwable t) {
                // If the post-condition read throws, the check does not apply.
            }

            // Additional consistency check using a fresh, identically-constructed object.
            // Two equivalent NormalDistributionImpl instances must compute the same quantile.
            try {
                NormalDistribution fresh = new NormalDistributionImpl(mu, sigma);
                double q2 = fresh.inverseCumulativeProbability(p);
                if (!(Double.isNaN(q) || Double.isNaN(q2) || Double.isInfinite(q) || Double.isInfinite(q2))
                        && Math.abs(q - q2) > 1.0e-12d) {
                    throw new RuntimeException(
                        "[oracle:fresh-agree] metamorphic violation: identical distributions must agree on the same quantile"
                            + " mu=" + mu
                            + " sigma=" + sigma
                            + " p=" + p
                            + " q1=" + q
                            + " q2=" + q2);
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
            } catch (Throwable t) {
                // Rejection means this check does not apply for this input.
            }
        }
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java	
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,7 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+        if((fa> fb)&&((fa*fb)>=0.0)){
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +


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
<details open><summary>▸ Output (~2,255 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            NormalDistributionImpl anchor = new NormalDistributionImpl(0.0, 1.0);
            anchor.inverseCumulativeProbability(0.9772498680518209d);
        } catch (MathException e) {
            throw new RuntimeException("Anchor trigger reached buggy bracket path", e);
        }

        double mean = data.consumeInt(-100, 100) / 10.0d;
        double sd = (data.consumeInt(1, 100) / 20.0d);
        int steps = data.consumeInt(1, 6);
        boolean positiveSide = data.consumeBoolean();

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);

        double x = mean + (positiveSide ? 1.0d : -1.0d) * steps * sd;
        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (MathException e) {
            throw new RuntimeException("Unexpected checked exception from cumulativeProbability", e);
        }

        if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p) || Double.isInfinite(p)) {
            return;
        }

        double q;
        try {
            q = dist.inverseCumulativeProbability(p);
        } catch (MathException e) {
            throw new RuntimeException("Fuzzed trigger reached buggy bracket path", e);
        }

        try {
            double roundTrip = dist.cumulativeProbability(q);
            if (Math.abs(roundTrip - p) > 1.0e-9d) {
                throw new RuntimeException(
                    "[oracle:cdf-inv-roundtrip] metamorphic violation: CDF(invCDF(p)) must equal p"
                        + " mean=" + mean
                        + " sd=" + sd
                        + " x=" + x
                        + " p=" + p
                        + " q=" + q
                        + " roundTrip=" + roundTrip);
            }
        } catch (MathException e) {
            throw new RuntimeException("Unexpected checked exception from round-trip check", e);
        }
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: Anchor trigger reached buggy bracket path

---
## [15] ⚙️ corpus-seed · `attempt_001`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [16] ⚙️ corpus-seed · `attempt_002`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [17] ⚙️ corpus-seed · `attempt_003`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [18] ⚙️ corpus-seed · `attempt_004`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [19] ⚙️ corpus-seed · `attempt_006`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['0.9772498680518209', '2.0', '1.0e-12', '0.9772498680518209L', '0.9772498680518209l', '0.9772498680518209F', '0.9772498680518209f', '0.9772498680518209D']}

---
## [20] ⚙️ patched-fuzz · `attempt_001`
**output:** **quiet on patched build (no overfit signal)**

---
## [21] ⚙️ patched-fuzz · `attempt_002`
**output:** **FIRED — [oracle:normal-symmetry] metamorphic violation: p=0.9772498680518209 q(p)=2.0 q(1-p)=-2.0000000034677394 err=3.4677394289417407E-9**
- reproducing_input_file: /tmp/d4j/Math_85_buggy/fuzz/attempt_002/crashes/crash-b01099cd35354914bd9244ed61741f7b705886d8

---
## [22] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**

---
## [23] ⚙️ patched-fuzz · `attempt_004`
**output:** **quiet on patched build (no overfit signal)**

---
## [24] ⚙️ patched-fuzz · `attempt_006`
**output:** **FIRED — [oracle:cdf-inv-roundtrip] metamorphic violation: CDF(invCDF(p)) must equal p mean=-10.0 sd=0.05 x=-10.05 p=0.15865525393145358 q=-10.050000102799993 roundTrip=0.15865475644019167**
- reproducing_input_file: /tmp/d4j/Math_85_buggy/fuzz/attempt_006/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709
