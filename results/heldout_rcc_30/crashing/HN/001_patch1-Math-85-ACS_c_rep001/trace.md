# Pipeline trace — Math-85

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java
@@ -195,7 +195,9 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+//ACS's patch begin
+	if (fa * fb >= 0.0 &&!(fa * fb==0.0)) {
+//ACS's patch end
             throw new ConvergenceException(
                       "number of iterations={0}, maximum iterations={1}, " +
                       "initial={2}, lower bound={3}, upper bound={4}, final a value={5}, " +
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.distribution.NormalDistributionTest', test_method='t (~712 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.distribution.NormalDistributionTest', test_method='testMath280', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/001_patch1-Math-85-ACS_c_rep001/Math_85_buggy/src/test/org/apache/commons/math/distribution/NormalDistributionTest.java', method_source='    public void testMath280() throws MathException {\\n        NormalDistribution normal = new NormalDistributionImpl(0,1);\\n        double result = normal.inverseCumulativeProbability(0.9772498680518209);\\n        assertEquals(2.0, result, 1.0e-12);\\n    }', exception_type='org.apache.commons.math.MathException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~13,715 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java\n+++ /src/java/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java\n@@ -195,7 +195,9 @@\n         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && \n                 ((a > lowerBound) || (b < upperBound)));\n    \n-        if (fa * fb >= 0.0 ) {\n+//ACS's patch begin\n+\tif (fa * fb >= 0.0 &&!(fa * fb==0.0)) {\n+//ACS's patch end\n             throw new ConvergenceException(\n                       \"number of iterations={0}, maximum iterations={1}, \" +\n                       \"initial={2}, lower bound={3}, upper bound={4}, final a value={5}, \" +\n",
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
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,int)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,double,double,double)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "Math.max(float,double)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "Math.min(float,double)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "[org.apache.commons.math.analysis.UnivariateRealFunction].value(org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "[org.apache.commons.math.analysis.UnivariateRealFunction].value(org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
          "[org.apache.commons.math.ConvergenceException].<init>(String,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,int,double,double,double,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,double,double)"
        ],
        [
          "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)",
          "[org.apache.commons.math.MathRuntimeException].buildMessage(org.apache.commons.math.MathRuntimeException,String,Object[])"
        ],
        [
          "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)",
          "[IllegalArgumentException].<init>(String)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)": 0,
        "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)": 1,
        "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,int)": 1,
        "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,double,double,double)": 1,
        "Math.max(float,double)": 1,
        "Math.min(float,double)": 1,
        "[org.apache.commons.math.analysis.UnivariateRealFunction].value(org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils)": 1,
        "[org.apache.commons.math.ConvergenceException].<init>(String,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,int,double,double,double,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,double,double)": 1,
        "[org.apache.commons.math.MathRuntimeException].buildMessage(org.apache.commons.math.MathRuntimeException,String,Object[])": 2,
        "[IllegalArgumentException].<init>(String)": 2
      },
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
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,int)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,double,double,double)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "Math.max(float,double)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "Math.min(float,double)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "[org.apache.commons.math.analysis.UnivariateRealFunction].value(org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)",
      "[org.apache.commons.math.ConvergenceException].<init>(String,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,int,double,double,double,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,double,double)"
    ],
    [
      "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)",
      "[org.apache.commons.math.MathRuntimeException].buildMessage(org.apache.commons.math.MathRuntimeException,String,Object[])"
    ],
    [
      "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)",
      "[IllegalArgumentException].<init>(String)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].bracket(org.apache.commons.math.analysis.UnivariateRealFunction,double,double,double,int)": 0,
    "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String)": 1,
    "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,int)": 1,
    "[org.apache.commons.math.MathRuntimeException].createIllegalArgumentException(String,double,double,double)": 1,
    "Math.max(float,double)": 1,
    "Math.min(float,double)": 1,
    "[org.apache.commons.math.analysis.UnivariateRealFunction].value(org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils)": 1,
    "[org.apache.commons.math.ConvergenceException].<init>(String,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,int,double,double,double,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils,double,double)": 1,
    "[org.apache.commons.math.MathRuntimeException].buildMessage(org.apache.commons.math.MathRuntimeException,String,Object[])": 2,
    "[IllegalArgumentException].<init>(String)": 2
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~21,794 chars)</summary>

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
@@ -195,7 +195,9 @@
         } while ((fa * fb > 0.0) && (numIterations < maximumIterations) && 
                 ((a > lowerBound) || (b < upperBound)));
    
-        if (fa * fb >= 0.0 ) {
+//ACS's patch begin
+	if (fa * fb >= 0.0 &&!(fa * fb==0.0)) {
+//ACS's patch end
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
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `bracket`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
<details open><summary>▸ Output (~7,893 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            anchorNormalTrigger();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        try {
            anchorDirectBracketAndOracle();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        int meanInt = data.consumeInt(-100, 100);
        int sdInt = data.consumeInt(1, 100);
        int steps = data.consumeInt(1, 20);
        boolean positiveSide = data.consumeBoolean();

        double mean = meanInt;
        double sd = sdInt;
        double x = positiveSide ? mean + (2.0 * sd) : mean - (2.0 * sd);

        try {
            exploreNormalRoundTrip(mean, sd, x);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        try {
            exploreBracketOverloads(data);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void anchorNormalTrigger() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        normal.inverseCumulativeProbability(0.9772498680518209);
    }

    private static void anchorDirectBracketAndOracle() throws Exception {
        UnivariateRealFunction f = new SinFunction();
        double[] r4 = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, 2.0);
        double[] r5 = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, 2.0, Integer.MAX_VALUE);

        /* Contract/oracle: bracket returns values bracketing a root; a correct result must satisfy f(a)*f(b) <= 0.
           The 4-arg and 5-arg overloads document the same operation, so on equivalent valid inputs they must agree. */
        double fa4 = f.value(r4[0]);
        double fb4 = f.value(r4[1]);
        if (!(fa4 * fb4 <= 0.0)) {
            throw new RuntimeException("[oracle:bracket-sign] metamorphic violation: returned interval does not bracket a root input=[1.0,0.0,2.0] lhs=" + r4[0] + "," + r4[1] + " rhs=" + fa4 + "," + fb4);
        }
        if (Double.compare(r4[0], r5[0]) != 0 || Double.compare(r4[1], r5[1]) != 0) {
            throw new RuntimeException("[oracle:bracket-overload] metamorphic violation: equivalent bracket overloads disagree input=[1.0,0.0,2.0] lhs=[" + r4[0] + "," + r4[1] + "] rhs=[" + r5[0] + "," + r5[1] + "]");
        }
    }

    private static void exploreNormalRoundTrip(double mean, double sd, double x) throws MathException {
        NormalDistribution dist = new NormalDistributionImpl(mean, sd);
        double p = dist.cumulativeProbability(x);
        double inv = dist.inverseCumulativeProbability(p);

        /* Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
           We construct p from cumulativeProbability(x), so the input is valid by construction and a correct implementation
           must recover x (within floating-point tolerance). A patch that merely suppresses the crash but returns a wrong
           value violates this observable round-trip relation. */
        double tol = Math.max(1.0e-9, Math.abs(sd) * 1.0e-9);
        if (Math.abs(inv - x) > tol) {
            throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: inverse(cdf(x)) != x input=mean=" + mean + ",sd=" + sd + ",x=" + x + ",p=" + p + " lhs=" + inv + " rhs=" + x);
        }
    }

    private static void exploreBracketOverloads(FuzzedDataProvider data) throws Exception {
        UnivariateRealFunction f = new SinFunction();
        int k = data.consumeInt(-8, 8);
        double root = k * Math.PI;
        double initial = root + (data.consumeBoolean() ? 1.0 : -1.0);
        double lower = Math.min(initial - 1.0, root);
        double upper = Math.max(initial + 1.0, root);
        if (!(lower < initial && initial < upper)) {
            return;
        }

        double[] r4;
        double[] r5;
        try {
            r4 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
            r5 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, Integer.MAX_VALUE);
        } catch (Throwable t) {
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        /* Contract/oracle: for valid inputs, returned endpoints bracket a root, and same-name overloads with equivalent
           parameters must agree. */
        double fa4 = f.value(r4[0]);
        double fb4 = f.value(r4[1]);
        if (!(fa4 * fb4 <= 0.0)) {
            throw new RuntimeException("[oracle:bracket-sign-fuzz] metamorphic violation: returned interval does not bracket a root input=initial=" + initial + ",lower=" + lower + ",upper=" + upper + " lhs=[" + r4[0] + "," + r4[1] + "] rhs=[" + fa4 + "," + fb4 + "]");
        }
        if (Math.abs(r4[0] - r5[0]) > 0.0 || Math.abs(r4[1] - r5[1]) > 0.0) {
            throw new RuntimeException("[oracle:bracket-overload-fuzz] metamorphic violation: equivalent bracket overloads disagree input=initial=" + initial + ",lower=" + lower + ",upper=" + upper + " lhs=[" + r4[0] + "," + r4[1] + "] rhs=[" + r5[0] + "," + r5[1] + "]");
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        return hasMathExceptionInChain(t) && hasBracketInChain(t);
    }

    private static boolean hasMathExceptionInChain(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketInChain(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Illegal") || name.contains("Invalid")) {
                return true;
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,000 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int rounds = 1 + Math.max(0, Math.min(8, data.remainingBytes()));
        for (int i = 0; i < rounds; i++) {
            exploreOne(data);
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: failing-test seed should recover x=2.0 input=" + p + " lhs=" + result + " rhs=2.0");
            }

            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p for the known valid seed input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exploreOne(FuzzedDataProvider data) {
        int z = data.consumeInt(-8, 8);

        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        double p;
        try {
            p = normal.cumulativeProbability((double) z);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (!(p > 0.0d && p < 1.0d)) {
            return;
        }

        double x;
        try {
            x = normal.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        try {
            double p2 = normal.cumulativeProbability(x);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * Here p is built by construction as cumulativeProbability(z) for a real NormalDistribution,
             * so a correct implementation is obliged to accept it and recover the original quantile.
             * A patch that only suppresses the throw in bracket but returns a wrong value would violate
             * either x ~= z or cumulativeProbability(x) ~= p.
             */
            if (Math.abs(x - (double) z) > 1.0e-9d) {
                throw new RuntimeException("[oracle:quantile-recover] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(z)) must recover z input=" + z + " lhs=" + x + " rhs=" + z);
            }
            if (Math.abs(p2 - p) > 1.0e-9d) {
                throw new RuntimeException("[oracle:round-trip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p input=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (StackTraceElement ste : stack) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                            && "bracket".equals(ste.getMethodName())) {
                        return true;
                    }
                }
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
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,421 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Local {
            boolean hasBracket(Throwable t) {
                Throwable cur = t;
                while (cur != null) {
                    StackTraceElement[] st = cur.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                return true;
                            }
                        }
                    }
                    cur = cur.getCause();
                }
                return false;
            }

            boolean isValidation(Throwable t) {
                Throwable cur = t;
                while (cur != null) {
                    if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                        return true;
                    }
                    String n = cur.getClass().getName();
                    if (n.contains("IllegalArgument") || n.contains("Invalid") || n.contains("OutOfRange")
                            || n.contains("NotPositive") || n.contains("NotStrictlyPositive")
                            || n.contains("NoData") || n.contains("NullArgument")) {
                        return true;
                    }
                    cur = cur.getCause();
                }
                return false;
            }

            boolean isRootCause(Throwable t) {
                return (t instanceof MathException) && hasBracket(t);
            }

            <T extends Throwable> void sneakyThrow(Throwable t) throws T {
                throw (T) t;
            }

            void maybePropagate(Throwable t) {
                if (isValidation(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    this.<RuntimeException>sneakyThrow(t);
                }
            }
        }
        Local local = new Local();

        try {
            NormalDistribution anchor = new NormalDistributionImpl(0.0, 1.0);
            double anchorP = 0.9772498680518209d;
            double anchorResult = anchor.inverseCumulativeProbability(anchorP);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(0.9772498680518209) must equal 2.0 input="
                        + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            local.maybePropagate(t);
            return;
        }

        int mean = data.consumeInt(-1000, 1000);
        boolean upperSide = data.consumeBoolean();
        double sigma = 1.0d;
        double x = mean + (upperSide ? 2.0d : -2.0d);

        try {
            NormalDistribution dist = new NormalDistributionImpl((double) mean, sigma);
            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * For a valid probability produced by the same distribution, inverseCumulativeProbability(cumulativeProbability(x))
             * should recover x (up to numerical tolerance). This directly catches a patch that merely suppresses the throw
             * or otherwise returns the wrong quantile after reaching bracket with fa*fb == 0.
             */
            if (Double.isNaN(inv) || Double.isInfinite(inv) || Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverse(cdf(x)) must recover x input="
                        + x + " lhs=" + inv + " rhs=" + x + " mean=" + mean + " sigma=" + sigma + " p=" + p);
            }
        } catch (Throwable t) {
            local.maybePropagate(t);
        }
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,546 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);

            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-inverse] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 input=" + p + " lhs=" + result + " rhs=2.0");
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

    private static void runExplore(FuzzedDataProvider data) {
        double mean = data.consumeInt(-100, 100);
        double sd = data.consumeInt(1, 1000) / 10.0d;

        try {
            NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);

            double x;
            int mode = data.consumeInt(0, 3);
            if (mode == 0) {
                x = mean;
            } else if (mode == 1) {
                x = mean + 2.0d * sd;
            } else if (mode == 2) {
                x = mean - 2.0d * sd;
            } else {
                int k = data.consumeInt(-3, 3);
                x = mean + (2.0d * k) * sd;
            }

            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability.
             * For p constructed from the same distribution as p = cumulativeProbability(x),
             * a correct implementation must recover x (within numeric tolerance).
             * A patch that merely suppresses the bracket failure but returns a wrong value
             * would violate this observable post-condition without throwing.
             */
            double tol = 1.0e-6d * Math.max(1.0d, Math.abs(x));
            if (Double.isNaN(inv) || Math.abs(inv - x) > tol) {
                throw new RuntimeException("[oracle:roundtrip-normal] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x input=" + x + " p=" + p + " lhs=" + inv + " rhs=" + x);
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
            if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st == null) {
                continue;
            }
            for (int i = 0; i < st.length; i++) {
                StackTraceElement e = st[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
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
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,091 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistributionImpl anchorDist =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0, 1);

        // ANCHOR: exact failing test input from NormalDistributionTest.testMath280.
        try {
            double anchorResult = anchorDist.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                        "[oracle:anchor] metamorphic violation: exact regression seed should round-trip to 2.0 inputP=0.9772498680518209 result="
                                + anchorResult);
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException
                            || t instanceof NumberFormatException;
            boolean hasBracket = false;
            for (Throwable c = t; c != null && !hasBracket; c = c.getCause()) {
                StackTraceElement[] st = c.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                && "bracket".equals(st[i].getMethodName())) {
                            hasBracket = true;
                            break;
                        }
                    }
                }
            }
            if (!cleanRejection && t instanceof org.apache.commons.math.MathException && hasBracket) {
                throw new RuntimeException(t);
            }
        }

        // EXPLORE:
        // Root cause property: the quantile equals a bracketing endpoint exactly, so fa*fb == 0.0.
        // Build valid inputs by construction through the real public API:
        // choose mean, sd, and an integer endpoint target = mean + k*sd, then p = CDF(target).
        // For any correct implementation, inverseCumulativeProbability(CDF(x)) ~= x.
        double mean = data.consumeInt(-50, 50);
        double sd = data.consumeInt(1, 20);
        int kChoice = data.consumeInt(0, 5);
        int k;
        switch (kChoice) {
            case 0:
                k = 2;   // exact buggy seed shape
                break;
            case 1:
                k = 0;   // lower endpoint possibility
                break;
            case 2:
                k = 1;
                break;
            case 3:
                k = -1;
                break;
            case 4:
                k = -2;
                break;
            default:
                k = data.consumeInt(-6, 6);
                break;
        }

        org.apache.commons.math.distribution.NormalDistributionImpl dist =
                new org.apache.commons.math.distribution.NormalDistributionImpl(mean, sd);

        double x = mean + ((double) k) * sd;
        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (Throwable t) {
            return;
        }

        if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            // Mandatory post-condition / metamorphic oracle:
            // For a correct distribution implementation, inverseCumulativeProbability is the inverse
            // of cumulativeProbability on valid probabilities. Deleting the patched throw or otherwise
            // "fixing" by returning the wrong value would violate this observable round-trip.
            double tol = 1.0e-9d * Math.max(1.0d, Math.abs(x));
            if (Math.abs(inv - x) > tol) {
                throw new RuntimeException(
                        "[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x"
                                + " mean=" + mean
                                + " sd=" + sd
                                + " k=" + k
                                + " x=" + x
                                + " p=" + p
                                + " inv=" + inv
                                + " tol=" + tol);
            }

            // Also exercise the patched function directly via the real library type, using the
            // same endpoint-root property, and compare the two overloads for sibling agreement.
            try {
                final org.apache.commons.math.analysis.UnivariateRealFunction f =
                        new org.apache.commons.math.analysis.UnivariateRealFunction() {
                            public double value(double v) {
                                return v - x;
                            }
                        };
                double initial = mean + sd;
                double lower = Math.min(initial, x) - 1.0d;
                double upper = Math.max(initial, x) + 1.0d;
                if (lower < upper) {
                    double[] b1 = org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket(
                            f, initial, lower, upper);
                    double[] b2 = org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils.bracket(
                            f, initial, lower, upper, Integer.MAX_VALUE);

                    if (b1 == null || b2 == null || b1.length != 2 || b2.length != 2
                            || b1[0] != b2[0] || b1[1] != b2[1]) {
                        throw new RuntimeException(
                                "[oracle:overload] metamorphic violation: bracket overloads must agree"
                                        + " initial=" + initial
                                        + " lower=" + lower
                                        + " upper=" + upper
                                        + " x=" + x
                                        + " b1=[" + (b1 == null ? "null" : (b1[0] + "," + b1[1])) + "]"
                                        + " b2=[" + (b2 == null ? "null" : (b2[0] + "," + b2[1])) + "]");
                    }
                    if (!((b2[0] <= x && x <= b2[1]) || (b2[1] <= x && x <= b2[0]))) {
                        throw new RuntimeException(
                                "[oracle:bracketed] metamorphic violation: returned interval must bracket the constructed root"
                                        + " x=" + x
                                        + " interval=[" + b2[0] + "," + b2[1] + "]");
                    }
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (org.apache.commons.math.FunctionEvaluationException e) {
                return;
            } catch (org.apache.commons.math.ConvergenceException e) {
                return;
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException
                            || t instanceof NumberFormatException;
            boolean hasBracket = false;
            for (Throwable c = t; c != null && !hasBracket; c = c.getCause()) {
                StackTraceElement[] st = c.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                && "bracket".equals(st[i].getMethodName())) {
                            hasBracket = true;
                            break;
                        }
                    }
                }
            }
            if (!cleanRejection && t instanceof org.apache.commons.math.MathException && hasBracket) {
                throw new RuntimeException(t);
            }
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,877 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability(cdf(2)) must return 2 for the standard normal input from the regression test input=0.9772498680518209 lhs=" + result + " rhs=2.0");
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

    private static void runExplore(FuzzedDataProvider data) {
        double mean = boundedDouble(data.consumeInt(), -100.0d, 100.0d);
        double sd = boundedPositiveScale(data.consumeInt());
        int sign = data.consumeBoolean() ? 1 : -1;
        int multiplier = data.consumeInt(1, 6);

        double target = mean + sign * 2.0d * sd;
        if (multiplier > 2) {
            target = mean + sign * ((double) multiplier) * sd;
        }

        try {
            NormalDistribution dist = new NormalDistributionImpl(mean, sd);

            double p = dist.cumulativeProbability(target);

            if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
                return;
            }

            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p from the same real distribution as p = CDF(target), so a correct implementation
             * is obliged to accept it and recover target (up to numerical tolerance). A patch that merely
             * deletes the throw or skips the bracketing logic can silently return a wrong value, which this
             * round-trip oracle catches.
             */
            double tol = Math.max(1.0e-12d, Math.abs(target) * 1.0e-9d);
            if (Math.abs(inv - target) > tol) {
                throw new RuntimeException("[oracle:cdf-icdf-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x for a valid NormalDistribution input=x mean=" + mean + " sd=" + sd + " p=" + p + " lhs=" + inv + " rhs=" + target);
            }

            double p2 = dist.cumulativeProbability(inv);
            double probTol = 1.0e-10d;
            if (Math.abs(p2 - p) > probTol) {
                throw new RuntimeException("[oracle:probability-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p for valid p mean=" + mean + " sd=" + sd + " p=" + p + " lhs=" + p2 + " rhs=" + p);
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

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrameInChain(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument")
                    || name.contains("Invalid")
                    || name.contains("OutOfRange")
                    || name.contains("NoData")
                    || name.contains("NotPositive")
                    || name.contains("NotStrictlyPositive")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasFrameInChain(Throwable t, String className, String methodName) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long unsigned = raw & 0xffffffffL;
        double fraction = unsigned / (double) 0xffffffffL;
        return min + (max - min) * fraction;
    }

    private static double boundedPositiveScale(int raw) {
        long unsigned = raw & 0xffffffffL;
        double fraction = unsigned / (double) 0xffffffffL;
        return 0.1d + 49.9d * fraction;
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
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,605 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = data.consumeInt(-1000, 1000);
        double sd = data.consumeInt(1, 1000);
        int target = data.consumeInt(-8, 8);
        boolean useAnchorDistribution = data.consumeBoolean();

        NormalDistribution dist = useAnchorDistribution
                ? new NormalDistributionImpl(0.0, 1.0)
                : new NormalDistributionImpl(mean, sd);

        tryExploreRoundTrip(dist, target);

        int extraTargets = data.consumeInt(0, 4);
        for (int i = 0; i < extraTargets; i++) {
            int t = data.consumeInt(-8, 8);
            tryExploreRoundTrip(dist, t);
        }
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: faithful regression check input=0.9772498680518209 result=" + result + " expected=2.0");
            }
            double roundTrip = normal.cumulativeProbability(result);
            if (Math.abs(roundTrip - 0.9772498680518209d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: inverse/cdf round-trip input=0.9772498680518209 inv=" + result + " cdf(inv)=" + roundTrip);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void tryExploreRoundTrip(NormalDistribution dist, int target) {
        try {
            double p = dist.cumulativeProbability((double) target);
            if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
                return;
            }

            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p from the real cumulativeProbability(x), so p is valid by construction.
             * A patch that only deletes/suppresses the throw in bracket but returns a wrong value
             * will violate this round-trip relation.
             */
            double tolerance = 1.0e-9d;
            if (Math.abs(inv - (double) target) > tolerance) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverse(cdf(x))==x target=" + target + " p=" + p + " inv=" + inv);
            }

            double p2 = dist.cumulativeProbability(inv);
            if (Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:cdfinv] metamorphic violation: cdf(inv(p))==p target=" + target + " p=" + p + " inv=" + inv + " cdf(inv)=" + p2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] trace = cur.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement e = trace[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
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
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,258 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact failing test input first.
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-eq2] metamorphic violation: exact failing test should recover x=2.0 input=0.9772498680518209 result=" + result);
            }
            // Contract/oracle: inverseCumulativeProbability(p) returns x such that cumulativeProbability(x) ~= p.
            // A patch that only deletes the throw but returns the wrong endpoint/value would violate this.
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - 0.9772498680518209d) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: cdf(inv(p)) == p input=0.9772498680518209 inv=" + result + " roundTrip=" + roundTrip);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
                return;
            }
        }

        // EXPLORE: construct valid probabilities by p = CDF(x) for moderate integer x.
        // Property: when x lands exactly on a bracket endpoint, fa*fb == 0 in bracket(), which the buggy code mishandles.
        int magnitude = data.consumeInt(2, 32);
        boolean negative = data.consumeBoolean();
        int offset = data.consumeInt(0, 3);
        double x = negative ? -(magnitude + offset) : (magnitude + offset);

        try {
            double p = normal.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d)) {
                return;
            }

            double inv;
            try {
                inv = normal.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                    return;
                }
                return;
            }

            // Oracle from the input itself: for p constructed as CDF(x), inverseCumulativeProbability(p) should recover x.
            if (Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException("[oracle:inv-cdf] metamorphic violation: inv(cdf(x)) == x inputX=" + x + " p=" + p + " inv=" + inv);
            }

            // Equivalent contract phrasing: CDF(inverseCumulativeProbability(p)) ~= p for valid p in (0,1).
            double p2;
            try {
                p2 = normal.cumulativeProbability(inv);
            } catch (Throwable ignored) {
                return;
            }
            if (Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:cdf-inv] metamorphic violation: cdf(inv(p)) == p inputX=" + x + " p=" + p + " inv=" + inv + " roundTrip=" + p2);
            }
        } catch (MathException ignored) {
            return;
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Illegal") >= 0 || name.indexOf("Invalid") >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException && hasBracketFrame(cur)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,059 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runOneCase(0.0, 2.0, 1.0e-12, true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            int meanInt = data.consumeInt(-1000, 1000);
            int offset = data.consumeInt(1, 20);
            boolean positiveSide = data.consumeBoolean();

            double mean = (double) meanInt;
            double x = positiveSide ? mean + offset : mean - offset;

            runOneCase(mean, x, 1.0e-9, false);
        }
    }

    private static void runOneCase(double mean, double x, double tol, boolean anchor) {
        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, 1.0);
            double p = normal.cumulativeProbability(x);
            double inv = normal.inverseCumulativeProbability(p);

            /*
             * Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid inputs.
             * We construct valid inputs by choosing a real x, computing p = CDF(x) from the same
             * real library object, then requiring inverseCumulativeProbability(p) ~= x.
             * A patch that merely suppresses the buggy throw but returns the wrong endpoint/value
             * violates this observable post-condition.
             */
            if (Double.isNaN(inv) || Math.abs(inv - x) > tol) {
                throw new RuntimeException(
                    "[oracle:normal-inverse-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input="
                    + "mean=" + mean + ",x=" + x + ",p=" + p + " lhs=" + inv + " rhs=" + x
                );
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
            if (anchor) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid")
                    || name.contains("OutOfRange") || name.contains("NoData")
                    || name.contains("NotStrictlyPositive") || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        boolean sawMathException = false;
        boolean sawBracket = false;

        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            for (StackTraceElement ste : cur.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())) {
                    sawBracket = true;
                }
            }
        }

        return sawMathException && sawBracket;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,932 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Post-condition asserted below:
         * For a real NormalDistribution, inverseCumulativeProbability(cumulativeProbability(x)) == x
         * for values x we construct ourselves. This round-trip uses only real library calls and
         * exercises the public API that reaches bracket(). A patch that merely suppresses the throw
         * but returns the wrong quantile violates this observable contract.
         */
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact failing test input first.
        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability returned wrong value input=" + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            boolean rootCause = false;
            if (t instanceof org.apache.commons.math.MathException) {
                Throwable c = t;
                while (c != null && !rootCause) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                    && "bracket".equals(st[i].getMethodName())) {
                                rootCause = true;
                                break;
                            }
                        }
                    }
                    c = c.getCause();
                }
            }
            if (rootCause || t instanceof RuntimeException) {
                throw (RuntimeException) (t instanceof RuntimeException ? t : new RuntimeException(t));
            }
            return;
        }

        // EXPLORE: construct many valid probabilities p = CDF(x) for integer x with |x| >= 2.
        // This targets the root cause property: the true root can land exactly on a bracket endpoint,
        // where buggy code treats fa*fb == 0 as failure instead of success.
        int trials = data.consumeInt(1, 8);
        for (int i = 0; i < trials; i++) {
            int sign = data.consumeBoolean() ? 1 : -1;
            int magnitude = data.consumeInt(2, 6);
            double x = sign * magnitude;

            double p;
            try {
                p = normal.cumulativeProbability(x);
            } catch (Throwable t) {
                // Any exception here means this input is not usable for the round-trip oracle.
                return;
            }

            try {
                double inv = normal.inverseCumulativeProbability(p);
                if (Math.abs(inv - x) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException;

                if (cleanRejection) {
                    return;
                }

                boolean rootCause = false;
                if (t instanceof org.apache.commons.math.MathException) {
                    Throwable c = t;
                    while (c != null && !rootCause) {
                        StackTraceElement[] st = c.getStackTrace();
                        if (st != null) {
                            for (int j = 0; j < st.length; j++) {
                                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[j].getClassName())
                                        && "bracket".equals(st[j].getMethodName())) {
                                    rootCause = true;
                                    break;
                                }
                            }
                        }
                        c = c.getCause();
                    }
                }

                if (rootCause || t instanceof RuntimeException) {
                    throw (RuntimeException) (t instanceof RuntimeException ? t : new RuntimeException(t));
                }
                return;
            }
        }
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,096 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing-test contract input=0.9772498680518209 result=" + result + " expected=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000) / 10.0d;
        double sd = Math.max(0.1d, data.consumeInt(1, 200) / 10.0d);
        int n = data.consumeInt(-8, 8);
        if (n == 0) {
            n = 2;
        }

        NormalDistribution dist = new NormalDistributionImpl(mean, sd);
        double x = mean + (sd * n);
        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (Throwable t) {
            return;
        }

        if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            if (Double.isNaN(inv) || Double.isInfinite(inv)) {
                throw new RuntimeException("[oracle:norm-inv] metamorphic violation: inverse returned non-finite inputP=" + p + " mean=" + mean + " sd=" + sd + " expectedX=" + x + " actual=" + inv);
            }

            /* Contract asserted:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p by calling the real cumulativeProbability(x), so a correct implementation
             * must return the original x (up to numerical tolerance). A patch that merely suppresses
             * the throw and returns a wrong value breaks this observable relation.
             */
            double tolX = Math.max(1.0e-9d, Math.abs(sd) * 1.0e-9d);
            if (Math.abs(inv - x) > tolX) {
                throw new RuntimeException("[oracle:norm-inv] metamorphic violation: inverse(cdf(x)) != x inputX=" + x + " mean=" + mean + " sd=" + sd + " p=" + p + " actual=" + inv + " tol=" + tolX);
            }

            double back;
            try {
                back = dist.cumulativeProbability(inv);
            } catch (Throwable t) {
                return;
            }

            double tolP = 1.0e-9d;
            if (Double.isNaN(back) || Math.abs(back - p) > tolP) {
                throw new RuntimeException("[oracle:norm-rt] metamorphic violation: cdf(inv(p)) != p mean=" + mean + " sd=" + sd + " x=" + x + " p=" + p + " inv=" + inv + " back=" + back + " tol=" + tolP);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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
            String name = cur.getClass().getName();
            if (name != null) {
                if (name.contains("Illegal") || name.contains("Invalid")) {
                    return true;
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
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,431 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        // Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability
        // on valid probabilities; the regression test requires this exact input to return 2.0.
        exerciseExactProbability(normal, 0.9772498680518209d, 2.0d, 1.0e-12d, true);

        // EXPLORE: generate many valid probabilities with the triggering property:
        // p = CDF(k) for an integer k whose exact root can land on a bracketing endpoint.
        // These are valid-by-construction inputs because p comes from the real distribution API.
        int magnitude = data.consumeInt(2, 6);
        int sign = data.consumeBoolean() ? 1 : -1;
        int xInt = sign * magnitude;
        double x = (double) xInt;

        try {
            double p = normal.cumulativeProbability(x);
            exerciseExactProbability(normal, p, x, 1.0e-9d, false);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        // Additional varied but still real and valid-by-construction cases:
        // perturb around integer roots using a second real-library round-trip seed.
        int deltaNum = data.consumeInt(-3, 3);
        double shiftedX = x + (deltaNum * 0.25d);
        if (shiftedX > -8.0d && shiftedX < 8.0d) {
            try {
                double p2 = normal.cumulativeProbability(shiftedX);
                double inv2 = normal.inverseCumulativeProbability(p2);
                double back2 = normal.cumulativeProbability(inv2);
                if (Math.abs(back2 - p2) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverse/cdf round-trip input=" + shiftedX + " p=" + p2 + " inv=" + inv2 + " back=" + back2);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
            }
        }
    }

    private static void exerciseExactProbability(NormalDistribution normal, double p, double expectedX, double tol, boolean anchor) {
        try {
            double inv = normal.inverseCumulativeProbability(p);

            if (Math.abs(inv - expectedX) > tol) {
                throw new RuntimeException("[oracle:exact-inverse] metamorphic violation: expected inverseCumulativeProbability to recover known x input=" + p + " expected=" + expectedX + " actual=" + inv);
            }

            // Contract/oracle: for valid probabilities, cumulativeProbability(inverseCumulativeProbability(p)) == p.
            // If a patch merely suppresses the throw and returns the wrong value, this observable relation breaks.
            try {
                double back = normal.cumulativeProbability(inv);
                if (Math.abs(back - p) > Math.max(1.0e-12d, tol)) {
                    throw new RuntimeException("[oracle:cdf-inverse-roundtrip] metamorphic violation: cdf(inverseCumulativeProbability(p)) != p input=" + p + " inv=" + inv + " back=" + back);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }

            if (anchor && Math.abs(inv - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-value] metamorphic violation: regression seed must return 2.0 input=" + p + " actual=" + inv);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && stackHasBracket(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean stackHasBracket(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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
        if (name.startsWith("org.apache.commons.math.")) {
            String simple = t.getClass().getSimpleName();
            if (simple.contains("Illegal") || simple.contains("Invalid")) {
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
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,469 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
            runExplore(data);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAnchor() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        double p = 0.9772498680518209d;
        double result = normal.inverseCumulativeProbability(p);
        if (Math.abs(result - 2.0d) > 1.0e-12d) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression seed should invert to 2.0 input="
                    + p + " lhs=" + result + " rhs=2.0");
        }
    }

    private static void runExplore(FuzzedDataProvider data) throws MathException {
        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int meanInt = data.consumeInt(-1000, 1000);
            boolean upperSide = data.consumeBoolean();
            double mean = meanInt;
            double target = mean + (upperSide ? 2.0d : -2.0d);

            NormalDistribution dist = new NormalDistributionImpl(mean, 1.0d);

            double p;
            try {
                p = dist.cumulativeProbability(target);
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }

            try {
                double inv = dist.inverseCumulativeProbability(p);

                /* Contract/oracle:
                 * We construct p from a REAL call p = cumulativeProbability(x) on the same distribution.
                 * For a correct inverseCumulativeProbability implementation, inverseCumulativeProbability(p)
                 * must recover x (up to solver tolerance). A patch that merely deletes/suppresses the throw
                 * but returns a wrong value violates this observable round-trip relation.
                 */
                if (Math.abs(inv - target) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input="
                            + target + " lhs=" + inv + " rhs=" + target);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }

            try {
                // Also exercise the patched method directly via the real overload pair, using a real library function.
                // The root-cause property is fa*fb == 0 at a bracket endpoint; x^2 - 4 has roots at ±2.
                org.apache.commons.math.analysis.polynomials.PolynomialFunction f =
                        new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] { -4.0d, 0.0d, 1.0d });
                double initial = upperSide ? 1.0d : -1.0d;
                double lower = upperSide ? 0.0d : -Double.MAX_VALUE;
                double upper = upperSide ? Double.MAX_VALUE : 0.0d;

                double[] via4 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
                double[] via5 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, Integer.MAX_VALUE);

                if (via4 == null || via5 == null || via4.length != 2 || via5.length != 2) {
                    throw new RuntimeException("[oracle:bracket-shape] metamorphic violation: bracket overloads must return two endpoints input="
                            + initial + " lhs=" + stringify(via4) + " rhs=" + stringify(via5));
                }

                /* Contract/oracle:
                 * Both bracket overloads document the same operation, with the 4-arg form delegating to the 5-arg
                 * form using a default iteration limit. For this easy polynomial input, both should produce the same
                 * bracketing interval. A patch that only masks the exception path or skips intended behaviour can
                 * make the overloads diverge observably.
                 */
                if (Math.abs(via4[0] - via5[0]) > 0.0d || Math.abs(via4[1] - via5[1]) > 0.0d) {
                    throw new RuntimeException("[oracle:overload] metamorphic violation: bracket overloads disagree input="
                            + initial + " lhs=" + stringify(via4) + " rhs=" + stringify(via5));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String stringify(double[] a) {
        if (a == null) {
            return "null";
        }
        if (a.length != 2) {
            return "len=" + a.length;
        }
        return "[" + a[0] + "," + a[1] + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,398 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = data.consumeInt(-1000, 1000);
        double sd = data.consumeInt(1, 1000) / 10.0;
        int k = data.consumeInt(2, 50);

        runExplore(mean, sd, k);

        if (data.consumeBoolean()) {
            double mean2 = data.consumeInt(-1000, 1000);
            double sd2 = data.consumeInt(1, 1000) / 10.0;
            int k2 = data.consumeInt(2, 50);
            runExplore(mean2, sd2, k2);
        }
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        try {
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: exact regression result input=" + p + " lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void runExplore(double mean, double sd, int k) {
        NormalDistribution normal = new NormalDistributionImpl(mean, sd);
        try {
            double target = mean + (sd * k);
            double p = normal.cumulativeProbability(target);

            double recovered;
            try {
                recovered = normal.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                handleThrowable(t);
                return;
            }

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We build p by construction from cumulativeProbability(target), so p is valid and accepted by any
             * correct implementation. A "fix" that merely suppresses the throw or skips proper bracketing can
             * return the wrong quantile; then this round-trip observable fails.
             */
            if (Math.abs(recovered - target) > 1.0e-9) {
                throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: inverse(cdf(x)) != x input=mean=" + mean + ",sd=" + sd + ",k=" + k + ",p=" + p + " lhs=" + recovered + " rhs=" + target);
            }

            try {
                double p2 = normal.cumulativeProbability(recovered);
                if (Math.abs(p2 - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:cdf-inverse-cdf] metamorphic violation: cdf(inverse(p)) != p input=mean=" + mean + ",sd=" + sd + ",k=" + k + ",p=" + p + " lhs=" + p2 + " rhs=" + p);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("NotStrictlyPositive")
                    || name.contains("OutOfRange")
                    || name.contains("Invalid")
                    || name.contains("NoData")
                    || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean mathExceptionFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                mathExceptionFamily = true;
                break;
            }
        }
        return mathExceptionFamily && stackPassesThroughBracket(t);
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st == null) {
                continue;
            }
            for (int i = 0; i < st.length; i++) {
                StackTraceElement e = st[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
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
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,122 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double EPS = 1.0e-12d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test first. On the buggy version this reaches
        // AbstractContinuousDistribution.inverseCumulativeProbability -> UnivariateRealSolverUtils.bracket
        // and throws MathException wrapping the faulty ConvergenceException.
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);
            double result = normal.inverseCumulativeProbability(ANCHOR_P);

            // Contract/oracle: the failing test itself establishes that this exact input
            // must return 2.0 for a correct implementation. A patch that merely suppresses
            // the throw or returns a wrong value is caught here.
            if (Math.abs(result - 2.0d) > EPS) {
                throw new RuntimeException(
                    "[oracle:anchor-val] metamorphic violation: exact regression input must recover x=2.0 input="
                        + ANCHOR_P + " lhs=" + result + " rhs=2.0");
            }

            // Contract/oracle: inverse CDF is the inverse of CDF on valid probabilities.
            // For p in (0,1), cumulativeProbability(inverseCumulativeProbability(p)) must recover p.
            double back = normal.cumulativeProbability(result);
            if (Math.abs(back - ANCHOR_P) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:cdf-inv-anchor] metamorphic violation: cdf(invCDF(p)) must equal p input="
                        + ANCHOR_P + " lhs=" + back + " rhs=" + ANCHOR_P);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
                return;
            }
            return;
        }

        // EXPLORE:
        // Root-cause property: choose p exactly as cumulativeProbability(x) for a standard
        // normal x that lies on the solver's integer-stepping bracket boundary. Then the real
        // root-finding path reaches bracket with fa * fb == 0, the condition changed by the patch.
        int magnitude = data.consumeInt(2, 20);
        boolean negative = data.consumeBoolean();
        int xInt = negative ? -magnitude : magnitude;
        double x = (double) xInt;

        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);
            double p = normal.cumulativeProbability(x);

            // Input is valid by construction: p comes from the real CDF of the same distribution,
            // so p is in (0,1) and inverseCumulativeProbability is obliged to accept it.
            double inv = normal.inverseCumulativeProbability(p);

            // Oracle from the input itself: because p was constructed as CDF(x), a correct inverse
            // must recover x. A throw-deleting patch that returns a nearby wrong endpoint breaks this.
            if (Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException(
                    "[oracle:recover-x] metamorphic violation: invCDF(CDF(x)) must recover x input="
                        + x + " lhs=" + inv + " rhs=" + x);
            }

            // Equivalent observable contract using only real library calls:
            // for valid p, cdf(invCDF(p)) must return p.
            double back = normal.cumulativeProbability(inv);
            if (Math.abs(back - p) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:cdf-inv] metamorphic violation: cdf(invCDF(p)) must equal p input="
                        + p + " lhs=" + back + " rhs=" + p);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
                return;
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || containsClassNameFragment(t, "Illegal")
            || containsClassNameFragment(t, "Invalid");
    }

    private static boolean isRootCause(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        return stackContainsBracket(t);
    }

    private static boolean stackContainsBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace != null) {
                for (StackTraceElement e : trace) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean containsClassNameFragment(Throwable t, String fragment) {
        Throwable cur = t;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (name != null && name.indexOf(fragment) >= 0) {
                return true;
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
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,063 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistribution normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (java.lang.Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing test contract input=" + anchorP + " result=" + anchorResult + " expected=2.0");
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException);
            if (!cleanRejection) {
                boolean throughBracket = false;
                for (Throwable c = t; c != null && !throughBracket; c = c.getCause()) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                throughBracket = true;
                                break;
                            }
                        }
                    }
                }
                if ((t instanceof org.apache.commons.math.MathException) && throughBracket) {
                    throw (org.apache.commons.math.MathException) t;
                }
                if (t instanceof RuntimeException) {
                    RuntimeException rt = (RuntimeException) t;
                    String msg = rt.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw rt;
                    }
                }
            }
        }

        int x = data.consumeInt(2, 50);
        if (data.consumeBoolean()) {
            x = 2;
        }

        try {
            double p = normal.cumulativeProbability((double) x);
            double roundTrip = normal.inverseCumulativeProbability(p);

            /* Documented/metamorphic guarantee:
               inverseCumulativeProbability is the inverse of cumulativeProbability for valid probabilities.
               We construct p by calling the real cumulativeProbability on a real x, so p is valid by construction.
               A throw-deleting or silently-wrong patch would violate this round-trip relation. */
            if (java.lang.Math.abs(roundTrip - (double) x) > 1.0e-6d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " p=" + p + " lhs=" + roundTrip + " rhs=" + x);
            }

            double p2 = normal.cumulativeProbability(roundTrip);
            if (java.lang.Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:prob-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) != p input=" + x + " p=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException);
            if (!cleanRejection) {
                boolean throughBracket = false;
                for (Throwable c = t; c != null && !throughBracket; c = c.getCause()) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                throughBracket = true;
                                break;
                            }
                        }
                    }
                }
                if ((t instanceof org.apache.commons.math.MathException) && throughBracket) {
                    throw (org.apache.commons.math.MathException) t;
                }
                if (t instanceof RuntimeException) {
                    RuntimeException rt = (RuntimeException) t;
                    String msg = rt.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw rt;
                    }
                }
            }
        }
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,695 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistribution normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (java.lang.Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing test contract input=" + anchorP + " result=" + anchorResult + " expected=2.0");
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException);
            if (!cleanRejection) {
                boolean throughBracket = false;
                for (Throwable c = t; c != null && !throughBracket; c = c.getCause()) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                throughBracket = true;
                                break;
                            }
                        }
                    }
                }
                if ((t instanceof org.apache.commons.math.MathException) && throughBracket) {
                    throw (org.apache.commons.math.MathException) t;
                }
                if (t instanceof RuntimeException) {
                    RuntimeException rt = (RuntimeException) t;
                    String msg = rt.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw rt;
                    }
                }
            }
        }

        int x = data.consumeInt(2, 50);
        if (data.consumeBoolean()) {
            x = 2;
        }

        try {
            double p = normal.cumulativeProbability((double) x);
            double roundTrip = normal.inverseCumulativeProbability(p);

            /* Documented/metamorphic guarantee:
               inverseCumulativeProbability is the inverse of cumulativeProbability for valid probabilities.
               We construct p by calling the real cumulativeProbability on a real x, so p is valid by construction.
               A throw-deleting or silently-wrong patch would violate this round-trip relation. */
            if (java.lang.Math.abs(roundTrip - (double) x) > 1.0e-6d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " p=" + p + " lhs=" + roundTrip + " rhs=" + x);
            }

            double p2 = normal.cumulativeProbability(roundTrip);
            if (java.lang.Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:prob-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) != p input=" + x + " p=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException);
            if (!cleanRejection) {
                boolean throughBracket = false;
                for (Throwable c = t; c != null && !throughBracket; c = c.getCause()) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            StackTraceElement e = st[i];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                                    && "bracket".equals(e.getMethodName())) {
                                throughBracket = true;
                                break;
                            }
                        }
                    }
                }
                if ((t instanceof org.apache.commons.math.MathException) && throughBracket) {
                    throw (org.apache.commons.math.MathException) t;
                }
                if (t instanceof RuntimeException) {
                    RuntimeException rt = (RuntimeException) t;
                    String msg = rt.getMessage();
                    if (msg != null && msg.startsWith("[oracle:")) {
                        throw rt;
                    }
                }
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/001_patch1-Math-85-ACS_c_rep001/Math_85_buggy/fuzz/attempt_016/FuzzHarness.java:36: error: unreported exception MathException; must be caught or declared to be thrown
                    throw (org.apache.commons.math.MathException) t;
                    ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/001_patch1-Math-85-ACS_c_rep001/Math_85_buggy/fuzz/attempt_016/FuzzHarness.java:89: error: unreported exception MathException; must be caught or declared to be thrown
                    throw (org.apache.commons.math.MathException) t;
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
<details open><summary>▸ Output (~4,675 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.distribution.NormalDistribution normal =
                new org.apache.commons.math.distribution.NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (java.lang.Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing test contract input=" + anchorP + " result=" + anchorResult + " expected=2.0");
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int x = data.consumeInt(2, 50);
        if (data.consumeBoolean()) {
            x = 2;
        }

        try {
            double p = normal.cumulativeProbability((double) x);
            double roundTrip = normal.inverseCumulativeProbability(p);

            /* Contract/oracle:
               inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
               We build p via the real cumulativeProbability call, so p is valid by construction.
               A patch that only suppresses the throw or returns a silent wrong value breaks this relation. */
            if (java.lang.Math.abs(roundTrip - (double) x) > 1.0e-6d) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " p=" + p + " lhs=" + roundTrip + " rhs=" + x);
            }

            double p2 = normal.cumulativeProbability(roundTrip);
            if (java.lang.Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:prob-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) != p input=" + x + " p=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t == null) {
            return;
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (t instanceof RuntimeException) {
            String msg = ((RuntimeException) t).getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (isGroundTruthRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name != null) {
                if (name.endsWith("IllegalArgumentException")
                        || name.endsWith("NumberFormatException")
                        || name.endsWith("InvalidRepresentationException")
                        || name.endsWith("InvalidMatrixException")
                        || name.endsWith("NotPositiveException")
                        || name.endsWith("OutOfRangeException")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        boolean sawMathException = false;
        boolean throughBracket = false;

        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof org.apache.commons.math.MathException) {
                sawMathException = true;
            }
            StackTraceElement[] st = c.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        throughBracket = true;
                        break;
                    }
                }
            }
            if (throughBracket && sawMathException) {
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
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,334 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution anchorDist = new NormalDistributionImpl(0.0, 1.0);

        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = anchorDist.inverseCumulativeProbability(anchorP);

            /* Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * The failing test documents that this exact valid input must produce 2.0; a "fix" that merely suppresses the
             * throw but returns a wrong value would violate this observable post-condition.
             */
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: exact regression input should invert to 2.0 input=" + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            boolean hasMathException = false;
            boolean passesBracket = false;
            Throwable cur = t;
            while (cur != null) {
                if (cur instanceof MathException) {
                    hasMathException = true;
                }
                StackTraceElement[] trace = cur.getStackTrace();
                if (trace != null) {
                    for (int i = 0; i < trace.length; i++) {
                        StackTraceElement ste = trace[i];
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                && "bracket".equals(ste.getMethodName())) {
                            passesBracket = true;
                            break;
                        }
                    }
                }
                if (passesBracket && hasMathException) {
                    break;
                }
                cur = cur.getCause();
            }
            if (hasMathException && passesBracket) {
                throw new RuntimeException(t);
            }
        }

        int trials = 1 + Math.min(4, data.remainingBytes());
        for (int i = 0; i < trials; i++) {
            double mean = data.consumeInt(-10, 10);
            double sd = data.consumeInt(1, 10);

            int step = data.consumeInt(2, 50);
            if (data.consumeBoolean()) {
                step = -step;
            }

            double x = mean + (sd * step);
            NormalDistribution dist = new NormalDistributionImpl(mean, sd);

            double p;
            try {
                p = dist.cumulativeProbability(x);
            } catch (Throwable ignored) {
                continue;
            }

            try {
                double inv = dist.inverseCumulativeProbability(p);

                /* Contract/oracle: for a value x we construct first, p = CDF(x) is a valid probability by construction,
                 * so inverseCumulativeProbability(p) must recover x (within numerical tolerance) for a correct implementation.
                 * This specifically exercises the patched bracket path on endpoint roots such as x = mean + sd * integer.
                 */
                if (Math.abs(inv - x) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:cdf-inverse] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException;
                if (cleanRejection) {
                    continue;
                }

                boolean hasMathException = false;
                boolean passesBracket = false;
                Throwable cur = t;
                while (cur != null) {
                    if (cur instanceof MathException) {
                        hasMathException = true;
                    }
                    StackTraceElement[] trace = cur.getStackTrace();
                    if (trace != null) {
                        for (int j = 0; j < trace.length; j++) {
                            StackTraceElement ste = trace[j];
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                    && "bracket".equals(ste.getMethodName())) {
                                passesBracket = true;
                                break;
                            }
                        }
                    }
                    if (passesBracket && hasMathException) {
                        break;
                    }
                    cur = cur.getCause();
                }

                if (hasMathException && passesBracket) {
                    throw new RuntimeException(t);
                }
            }
        }
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,686 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        double mean = data.consumeInt(-100, 100) / 10.0;
        double sd = Math.max(0.1, data.consumeInt(1, 100) / 10.0);
        int sign = data.consumeBoolean() ? 1 : -1;
        double target = mean + sign * 2.0 * sd;

        try {
            ContinuousDistribution dist = new NormalDistributionImpl(mean, sd);
            double p = dist.cumulativeProbability(target);
            if (p > 0.0 && p < 1.0) {
                double inv = dist.inverseCumulativeProbability(p);
                if (Math.abs(inv - target) > 1.0e-9) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) should recover x for this continuous normal input=" + target + " lhs=" + inv + " rhs=" + target);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }

        exploreBracketOracle(data);
    }

    private static void anchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-result] metamorphic violation: failing test requires inverseCumulativeProbability(0.9772498680518209) == 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void exploreBracketOracle(FuzzedDataProvider data) {
        double lower = 0.0;
        double upper = Math.PI;
        double initial = Math.PI - 1.0;
        int maxIterations = Math.max(1, data.consumeInt(1, 8));

        try {
            SinFunction f = new SinFunction();
            double[] r1 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
            double fa = f.value(r1[0]);
            double fb = f.value(r1[1]);
            if (!(fa * fb <= 0.0)) {
                throw new RuntimeException("[oracle:bracket-sign] metamorphic violation: documented bracketing result must bound a root with opposite signs or an endpoint root input=" + initial + "," + lower + "," + upper + " lhs=" + fa + " rhs=" + fb);
            }

            double[] r2 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);
            double ga = f.value(r2[0]);
            double gb = f.value(r2[1]);
            if (!(ga * gb <= 0.0)) {
                throw new RuntimeException("[oracle:bracket-sign2] metamorphic violation: documented bracketing result must bound a root with opposite signs or an endpoint root input=" + initial + "," + lower + "," + upper + "," + maxIterations + " lhs=" + ga + " rhs=" + gb);
            }

            if (Math.abs(r1[0] - r2[0]) > 1.0e-12 || Math.abs(r1[1] - r2[1]) > 1.0e-12) {
                throw new RuntimeException("[oracle:bracket-overload] metamorphic violation: same-name bracket overloads must agree on equivalent inputs input=" + initial + "," + lower + "," + upper + "," + maxIterations + " lhs=[" + r1[0] + "," + r1[1] + "] rhs=[" + r2[0] + "," + r2[1] + "]");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.contains("Invalid") || n.contains("Illegal") || n.contains("NoData") || n.contains("OutOfRange")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        boolean sawMathException = false;
        boolean sawBracket = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (StackTraceElement e : st) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        sawBracket = true;
                        break;
                    }
                }
            }
        }
        return sawMathException && sawBracket;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,446 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double STRICT_TOL = 1.0e-12d;
    private static final double ROUNDTRIP_TOL = 1.0e-9d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistributionImpl anchorDist = new NormalDistributionImpl(0.0, 1.0);

        try {
            double result = anchorDist.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - 2.0d) > STRICT_TOL) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 input="
                        + ANCHOR_P + " lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t);
            return;
        }

        int trials = data.consumeInt(1, 4);
        for (int i = 0; i < trials; i++) {
            NormalDistributionImpl dist = new NormalDistributionImpl(0.0, 1.0);
            int target = 2 + data.consumeInt(0, 32);

            double p;
            try {
                p = dist.cumulativeProbability((double) target);
            } catch (Throwable t) {
                handleLibraryThrowable(t);
                return;
            }

            try {
                double recovered = dist.inverseCumulativeProbability(p);
                if (Math.abs(recovered - (double) target) > ROUNDTRIP_TOL) {
                    /*
                     * Contract asserted: inverseCumulativeProbability is the inverse CDF.
                     * Since p is built by a real call to cumulativeProbability(target), a correct
                     * implementation must recover target (within floating tolerance). A patch that
                     * merely deletes the throw or bypasses the bracketing logic can silently return
                     * the wrong quantile and violate this observable round-trip relation.
                     */
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x input="
                            + target + " lhs=" + recovered + " rhs=" + target);
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t);
                return;
            }
        }
    }

    private static void handleLibraryThrowable(Throwable t) {
        if (t instanceof RuntimeException && t.getClass() == RuntimeException.class
                && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.apache.commons.math")
                    && (name.contains("Invalid") || name.contains("Illegal") || name.contains("Argument"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException && stackPassesThroughBracket(cur)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
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
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,786 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double TOL = 1.0e-12d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        try {
            double result = normal.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - 2.0d) > TOL) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(0.9772498680518209) must equal 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int trials = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < trials; i++) {
            int k = data.consumeInt(1, 6);
            if (data.consumeBoolean()) {
                k = 2;
            }

            try {
                double p = normal.cumulativeProbability((double) k);
                double inv = normal.inverseCumulativeProbability(p);

                if (Math.abs(inv - (double) k) > TOL) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x for the continuous normal distribution input=" + k + " lhs=" + inv + " rhs=" + k);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        if (data.remainingBytes() <= 0) {
            return;
        }

        int extraTrials = 1 + data.consumeInt(0, 6);
        for (int i = 0; i < extraTrials; i++) {
            int base = data.consumeInt(1, 6);
            int deltaNum = data.consumeInt(-1000, 1000);
            double x = base + (deltaNum / 1000.0d);

            try {
                double p = normal.cumulativeProbability(x);
                if (!(p > 0.0d && p < 1.0d)) {
                    continue;
                }
                double inv = normal.inverseCumulativeProbability(p);

                if (Math.abs(inv - x) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:roundtrip-fuzz] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) should recover x for moderate x input=" + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t == null) {
            return;
        }
        if (t instanceof RuntimeException && isOracleRuntime((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isOracleRuntime(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        return (t instanceof MathException) && hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
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
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NotStrictlyPositive");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,054 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double ANCHOR_X = 2.0d;
    private static final double TOL = 1.0e-12;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double anchor = normal.inverseCumulativeProbability(ANCHOR_P);
            if (!approximatelyEqual(anchor, ANCHOR_X, TOL)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability must recover the test's documented result input="
                        + ANCHOR_P + " lhs=" + anchor + " rhs=" + ANCHOR_X);
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            int x = data.consumeInt(-8, 8);
            try {
                double p = normal.cumulativeProbability((double) x);
                double inv = normal.inverseCumulativeProbability(p);
                /* Contract asserted:
                 * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
                 * We construct p by calling the real cumulativeProbability on a known finite x, so p is valid by construction.
                 * For a correct implementation, inverseCumulativeProbability(cumulativeProbability(x)) must recover x.
                 * A patch that merely deletes/suppresses the throw in bracket but returns a wrong value would violate this.
                 */
                if (!approximatelyEqual(inv, (double) x, TOL)) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x input="
                            + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                handleThrowable(t, true);
            }
            return;
        }

        if (mode == 1) {
            int x1 = data.consumeInt(-8, 8);
            int x2 = data.consumeInt(-8, 8);
            int xLo = Math.min(x1, x2);
            int xHi = Math.max(x1, x2);
            try {
                double pLo = normal.cumulativeProbability((double) xLo);
                double pHi = normal.cumulativeProbability((double) xHi);
                double qLo = normal.inverseCumulativeProbability(pLo);
                double qHi = normal.inverseCumulativeProbability(pHi);
                /* Contract asserted:
                 * cumulativeProbability is monotone, and inverseCumulativeProbability is its inverse on valid probabilities.
                 * For xLo <= xHi, the recovered quantiles must satisfy qLo <= qHi.
                 * This uses only real library calls on valid probabilities constructed by cumulativeProbability itself.
                 */
                if (qLo > qHi + TOL) {
                    throw new RuntimeException("[oracle:monotone] metamorphic violation: inverse results must preserve order input=("
                            + xLo + "," + xHi + ") lhs=(" + qLo + "," + qHi + ") rhs=ordered");
                }
            } catch (Throwable t) {
                handleThrowable(t, true);
            }
            return;
        }

        int x = data.consumeInt(-8, 8);
        boolean useAnchor = data.consumeBoolean();
        try {
            double p = useAnchor ? ANCHOR_P : normal.cumulativeProbability((double) x);
            double inv1 = normal.inverseCumulativeProbability(p);
            double inv2 = normal.inverseCumulativeProbability(p);
            /* Contract asserted:
             * inverseCumulativeProbability is a pure query on the distribution; repeating the same valid call must return the same result.
             * If a patch makes the buggy branch unreachable or silently changes bookkeeping/output, repeated calls on the same input must still agree.
             */
            if (!approximatelyEqual(inv1, inv2, TOL)) {
                throw new RuntimeException("[oracle:idempotent] metamorphic violation: repeated inverseCumulativeProbability calls must agree input="
                        + p + " lhs=" + inv1 + " rhs=" + inv2);
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (isOracleFailure(t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isGroundTruthRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument")
                    || name.contains("Invalid")
                    || name.contains("OutOfRange")
                    || name.contains("NoData")
                    || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack == null) {
                continue;
            }
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement e = stack[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,264 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input first.
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: exact regression input should invert to 2.0 input=" + p + " lhs=" + result + " rhs=2.0");
            }
            // Contract/oracle: for a continuous distribution, cumulativeProbability(inverseCumulativeProbability(p))
            // should recover p for valid p in (0,1). A throw-deleting patch that returns a wrong value breaks this.
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: cdf(icdf(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // EXPLORE: build valid distributions and valid probabilities by construction.
        // Root-cause property: choose p = CDF(mu + 2*sigma), so the true inverse is exactly an endpoint
        // that the buggy bracketing code can hit with f(endpoint) == 0.
        int meanTenths = data.consumeInt(-100, 100);
        int sigmaTenths = data.consumeInt(1, 100);
        double mean = meanTenths / 10.0;
        double sigma = sigmaTenths / 10.0;

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sigma);
            double x = mean + 2.0 * sigma;
            double p = normal.cumulativeProbability(x);
            double result = normal.inverseCumulativeProbability(p);

            // Contract/oracle: inverse cumulative is the inverse of cumulative for valid probabilities.
            if (Math.abs(result - x) > 1.0e-9) {
                throw new RuntimeException("[oracle:explore-rt] metamorphic violation: icdf(cdf(x)) == x input=" + x + " lhs=" + result + " rhs=" + x);
            }

            // Equivalent round-trip from the probability side for the same valid input.
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:explore-cdf] metamorphic violation: cdf(icdf(p)) == p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t == null) {
            return;
        }
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(f.getClassName())
                    && "bracket".equals(f.getMethodName())) {
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
            String name = cur.getClass().getName();
            if (name.startsWith("org.apache.commons.math.")
                    && (name.contains("Illegal") || name.contains("Invalid") || name.contains("OutOfRange"))) {
                return true;
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
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,449 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = (double) data.consumeInt(-1000, 1000);
        double sd = 1.0;

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);

            int shape = data.consumeInt(0, 4);
            double x;
            if (shape == 0) {
                x = mean + 2.0;
            } else if (shape == 1) {
                x = mean;
            } else if (shape == 2) {
                x = mean - 2.0;
            } else if (shape == 3) {
                x = mean + data.consumeInt(-10, 10);
            } else {
                x = mean + (data.consumeBoolean() ? 2.0 : -2.0);
            }

            double p;
            try {
                p = normal.cumulativeProbability(x);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwAsUnchecked(t);
                }
                return;
            }

            try {
                double inv = normal.inverseCumulativeProbability(p);

                /*
                 * Contract asserted: inverseCumulativeProbability(p) returns a point whose
                 * cumulative probability is p; therefore for p constructed as
                 * cumulativeProbability(x) on a real NormalDistribution, the round-trip
                 * inverseCumulativeProbability(cumulativeProbability(x)) must recover x
                 * (within the test tolerance 1E-6 used by NormalDistributionTest).
                 * A patch that merely deletes/suppresses the throw but returns a wrong value
                 * will violate this observable API-level relation.
                 */
                if (!approximatelyEqual(inv, x, 1.0e-6)) {
                    throw new RuntimeException(
                        "[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) ~= x input="
                            + x + " lhs=" + inv + " rhs=" + x + " mean=" + mean + " p=" + p);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwAsUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwAsUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (!approximatelyEqual(result, 2.0, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:anchor-exact] metamorphic violation: failing-test seed should recover x=2.0 input=0.9772498680518209 lhs="
                        + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwAsUnchecked(t);
            }
        }
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return a == b;
        }
        return Math.abs(a - b) <= tol;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("IllegalArgument") >= 0
                    || name.indexOf("Invalid") >= 0
                    || name.indexOf("OutOfRange") >= 0
                    || name.indexOf("NoData") >= 0
                    || name.indexOf("NullArgument") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean sawMathException = false;
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                int i;
                for (i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return sawMathException;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
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
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,203 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int mean = data.consumeInt(-1000, 1000);
        explore(mean);

        int extraCases = data.consumeInt(0, 4);
        for (int i = 0; i < extraCases; i++) {
            int m = data.consumeInt(-1000, 1000);
            explore(m);
        }
    }

    private static void anchor() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: failing-test contract input=0.9772498680518209 result="
                        + result + " expected=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void explore(int mean) {
        NormalDistribution dist = new NormalDistributionImpl(mean, 1.0d);
        double x = mean + 2.0d;
        try {
            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * For a continuous normal distribution, inverseCumulativeProbability is the inverse
             * of cumulativeProbability on valid probabilities. We build p by construction as
             * cumulativeProbability(x), so a correct implementation must recover x (within solver
             * tolerance). A patch that merely suppresses the throw in bracket but returns a wrong
             * value will violate this round-trip relation.
             */
            if (Math.abs(inv - x) > 1.0e-6d) {
                throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: inverse(cdf(x)) != x mean="
                        + mean + " x=" + x + " p=" + p + " inv=" + inv);
            }

            try {
                double p2 = dist.cumulativeProbability(inv);
                if (Math.abs(p2 - p) > 1.0e-6d) {
                    throw new RuntimeException(
                        "[oracle:cdf-roundtrip] metamorphic violation: cdf(inverse(p)) != p mean="
                            + mean + " x=" + x + " p=" + p + " inv=" + inv + " p2=" + p2);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean sawMathException = false;
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof MathException) {
                sawMathException = true;
            }
            for (StackTraceElement ste : c.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())
                        && sawMathException) {
                    return true;
                }
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
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,470 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression seed must invert to 2.0 input=0.9772498680518209 result=" + result);
            }
        } catch (Throwable t) {
            if (isValidationRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        double mean = data.consumeInt(-100, 100);
        double sd = data.consumeInt(1, 100);
        boolean upperTail = data.consumeBoolean();

        /*
         * Contract/oracle:
         * inverseCumulativeProbability(p) returns an x such that cumulativeProbability(x)=p.
         * We construct p from the real distribution's cumulativeProbability(x), so a correct
         * implementation should round-trip back to x (within numerical tolerance).
         *
         * To exercise the patched bracket line, choose x so that on the NormalDistribution
         * production path it lands exactly on a bracket endpoint after the first expansion:
         * upper tail: x = mean + sd + 1
         * lower tail: x = mean - sd - 1
         * This makes f(a)*f(b) == 0.0 when the endpoint is the exact root.
         */
        double x = upperTail ? (mean + sd + 1.0d) : (mean - sd - 1.0d);

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);
            double p = normal.cumulativeProbability(x);
            double inv = normal.inverseCumulativeProbability(p);

            double tol = 1.0e-6d * Math.max(1.0d, Math.abs(x));
            if (Double.isNaN(inv) || Math.abs(inv - x) > tol) {
                throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x inputMean="
                        + mean + " inputSd=" + sd + " x=" + x + " p=" + p + " recovered=" + inv + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isValidationRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return stackPassesThroughBracket(t);
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (StackTraceElement e : stack) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isValidationRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n != null) {
                if (n.contains("Illegal") || n.contains("Invalid")) {
                    return true;
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
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,004 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double TEST_P = 0.9772498680518209d;
    private static final double TEST_EXPECTED = 2.0d;
    private static final double STRICT_EPS = 1.0e-12d;
    private static final double ROUNDTRIP_EPS = 1.0e-9d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution anchorDist = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double result = anchorDist.inverseCumulativeProbability(TEST_P);
            if (Math.abs(result - TEST_EXPECTED) > STRICT_EPS) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 inputP="
                        + TEST_P + " result=" + result + " expected=" + TEST_EXPECTED);
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

        int iterations = data.consumeInt(1, 4);
        for (int i = 0; i < iterations; i++) {
            double mean = boundedDouble(data.consumeInt(), -10.0d, 10.0d);
            double sd = boundedPositive(data.consumeInt(), 0.1d, 10.0d);
            int z = data.consumeInt(0, 8);

            NormalDistribution dist = new NormalDistributionImpl(mean, sd);

            try {
                double x = mean + sd * z;
                double p = dist.cumulativeProbability(x);

                if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
                    return;
                }

                double recovered = dist.inverseCumulativeProbability(p);

                /* Contract/oracle:
                 * For a continuous distribution, inverseCumulativeProbability(cumulativeProbability(x))
                 * should recover x (up to numerical tolerance) for valid probabilities.
                 * A patch that merely deletes the throw or otherwise changes the branch to return a
                 * wrong value would violate this observable round-trip relation without crashing.
                 */
                if (Math.abs(recovered - x) > ROUNDTRIP_EPS) {
                    throw new RuntimeException("[oracle:norm-icdf-roundtrip] metamorphic violation: inverse(cdf(x)) != x mean="
                            + mean + " sd=" + sd + " x=" + x + " p=" + p + " recovered=" + recovered);
                }

                double p2 = dist.cumulativeProbability(recovered);
                if (Math.abs(p2 - p) > ROUNDTRIP_EPS) {
                    throw new RuntimeException("[oracle:norm-cdf-consistency] metamorphic violation: cdf(inverse(p)) != p mean="
                            + mean + " sd=" + sd + " x=" + x + " p=" + p + " recovered=" + recovered + " recdf=" + p2);
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
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        Throwable cur = t;
        while (cur != null) {
            if (hasBracketFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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
            String name = cur.getClass().getName();
            if (name != null) {
                if (name.endsWith("InvalidRepresentationException")
                        || name.endsWith("InvalidMatrixException")
                        || name.endsWith("NotPositiveException")
                        || name.endsWith("NotStrictlyPositiveException")
                        || name.endsWith("OutOfRangeException")
                        || name.endsWith("NoBracketingException")
                        || name.endsWith("NullArgumentException")
                        || name.endsWith("IllegalArgumentException")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        double span = max - min;
        long nonNeg = raw & 0x7fffffffL;
        return min + (nonNeg % 1000000L) * (span / 999999.0d);
    }

    private static double boundedPositive(int raw, double min, double max) {
        double v = boundedDouble(raw, min, max);
        return v <= 0.0d ? min : v;
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
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,030 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact regression input must recover x=2.0 input=0.9772498680518209 result=" + result);
            }
        } catch (Throwable t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection) {
                Throwable c = t.getCause();
                while (c != null && !cleanRejection) {
                    if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                        cleanRejection = true;
                        break;
                    }
                    c = c.getCause();
                }
            }
            if (cleanRejection) {
                return;
            }

            boolean hasMathException = false;
            Throwable cur = t;
            while (cur != null) {
                if (cur instanceof MathException) {
                    hasMathException = true;
                    break;
                }
                cur = cur.getCause();
            }

            boolean passesThroughBracket = false;
            cur = t;
            while (cur != null && !passesThroughBracket) {
                StackTraceElement[] frames = cur.getStackTrace();
                if (frames != null) {
                    for (int i = 0; i < frames.length; i++) {
                        StackTraceElement ste = frames[i];
                        if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                && "bracket".equals(ste.getMethodName())) {
                            passesThroughBracket = true;
                            break;
                        }
                    }
                }
                cur = cur.getCause();
            }

            if (hasMathException && passesThroughBracket) {
                throw new RuntimeException(t);
            }
            return;
        }

        int trials = 1 + data.consumeInt(1, 4);
        for (int i = 0; i < trials; i++) {
            double mean = data.consumeInt(-1000, 1000) / 100.0d;
            double sd = data.consumeInt(1, 1000) / 100.0d;
            int z = data.consumeInt(1, 6);
            int sign = data.consumeBoolean() ? 1 : -1;
            double x = mean + sign * z * sd;

            try {
                NormalDistribution dist = new NormalDistributionImpl(mean, sd);
                double p = dist.cumulativeProbability(x);

                try {
                    double inv = dist.inverseCumulativeProbability(p);

                    // Contract/oracle: for p constructed from the same distribution as p = CDF(x),
                    // a correct inverse must recover x (round-trip through real library code).
                    // A throw-deleting patch at bracket could silently return the wrong value and violate this.
                    double tol = 1.0e-9d * (1.0d + Math.abs(x));
                    if (Math.abs(inv - x) > tol) {
                        throw new RuntimeException("[oracle:cdf-inv-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x inputMean="
                                + mean + " inputSd=" + sd + " x=" + x + " p=" + p + " lhs=" + inv + " rhs=" + x);
                    }
                } catch (Throwable t) {
                    boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                    if (!cleanRejection) {
                        Throwable c = t.getCause();
                        while (c != null && !cleanRejection) {
                            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                                cleanRejection = true;
                                break;
                            }
                            c = c.getCause();
                        }
                    }
                    if (cleanRejection) {
                        continue;
                    }

                    boolean hasMathException = false;
                    Throwable cur = t;
                    while (cur != null) {
                        if (cur instanceof MathException) {
                            hasMathException = true;
                            break;
                        }
                        cur = cur.getCause();
                    }

                    boolean passesThroughBracket = false;
                    cur = t;
                    while (cur != null && !passesThroughBracket) {
                        StackTraceElement[] frames = cur.getStackTrace();
                        if (frames != null) {
                            for (int j = 0; j < frames.length; j++) {
                                StackTraceElement ste = frames[j];
                                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                                        && "bracket".equals(ste.getMethodName())) {
                                    passesThroughBracket = true;
                                    break;
                                }
                            }
                        }
                        cur = cur.getCause();
                    }

                    if (hasMathException && passesThroughBracket) {
                        throw new RuntimeException(t);
                    }
                }
            } catch (Throwable t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (!cleanRejection) {
                    Throwable c = t.getCause();
                    while (c != null && !cleanRejection) {
                        if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                            cleanRejection = true;
                            break;
                        }
                        c = c.getCause();
                    }
                }
                if (!cleanRejection) {
                    return;
                }
            }
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,768 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double STRICT_TOL = 1.0e-12d;
    private static final double ROUNDTRIP_TOL = 1.0e-6d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double result = normal.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - 2.0d) > STRICT_TOL) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(cdf(2)) should recover 2.0 input=" + ANCHOR_P + " lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }

        int junkCount = data.consumeInt(0, 4);
        for (int i = 0; i < junkCount; i++) {
            if (data.consumeBoolean()) {
                data.consumeAsciiString(data.consumeInt(0, 32));
            } else {
                data.consumeBytes(data.consumeInt(0, 32));
            }
        }

        int k = data.consumeInt(2, 16);

        try {
            double p = normal.cumulativeProbability((double) k);
            double recovered = normal.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability for valid probabilities.
             * We build p by construction as cumulativeProbability(k) from the same real NormalDistribution,
             * so p is valid and a correct implementation must recover k (within solver tolerance).
             * A patch that merely suppresses the throw but returns a wrong value violates this round-trip.
             */
            if (Math.abs(recovered - (double) k) > ROUNDTRIP_TOL) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) ~= x input=" + k + " lhs=" + recovered + " rhs=" + k);
            }

            if (data.consumeBoolean()) {
                double p2 = normal.cumulativeProbability((double) k);
                double recovered2 = normal.inverseCumulativeProbability(p2);
                if (Math.abs(recovered2 - recovered) > ROUNDTRIP_TOL) {
                    throw new RuntimeException("[oracle:idempotent-roundtrip] metamorphic violation: repeated round-trip on same distribution/input must agree input=" + k + " lhs=" + recovered + " rhs=" + recovered2);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Illegal") || name.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            return msg != null && msg.startsWith("[oracle:");
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrameInChain(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean hasFrameInChain(Throwable t, String className, String methodName) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (StackTraceElement e : stack) {
                    if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                        return true;
                    }
                }
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
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upper bound=179,769,313,486,231…

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,933 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double TOL = 1.0e-12;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mean = data.consumeInt(-100, 100);
        int mode = data.consumeInt(0, 3);

        double x;
        if (mode == 0) {
            x = mean + 2.0;
        } else if (mode == 1) {
            x = mean - 2.0;
        } else if (mode == 2) {
            x = mean + 2.0 + data.consumeInt(-3, 3);
        } else {
            x = mean - 2.0 + data.consumeInt(-3, 3);
        }

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, 1.0);

        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            /*
             * Metamorphic contract: inverse cumulative probability is the inverse of
             * cumulative probability on valid probabilities. We construct p by calling
             * cumulativeProbability(x) on a real NormalDistributionImpl with positive
             * standard deviation, so p is valid by construction. A correct implementation
             * should therefore recover x (within tolerance). A "fix" that merely deletes
             * the throw or returns an arbitrary nearby value would violate this.
             */
            if (Double.isNaN(inv) || Math.abs(inv - x) > TOL) {
                throw new RuntimeException(
                    "[oracle:normal-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x input="
                        + x + " mean=" + mean + " p=" + p + " lhs=" + inv + " rhs=" + x);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Double.isNaN(result) || Math.abs(result - 2.0d) > TOL) {
                throw new RuntimeException(
                    "[oracle:anchor-result] metamorphic violation: failing test must return 2.0 input=0.9772498680518209 lhs="
                        + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (StackTraceElement e : st) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
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
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: org.apache.commons.math.MathException: org.apache.commons.math.ConvergenceException: number of iterations=1, maximum iterations=2,147,483,647, initial=1, lower bound=0, upp…

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **29/29 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov; 29 raw fuzz log(s)**
<details><summary>▸ detail (~14,404 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 810}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 876}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 804}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 809}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 806}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 804}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 806}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 808}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 807}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 808}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 806}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 807}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 804}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 808}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 808}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 806}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 810}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 804}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 804}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 807}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 810}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 805}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 803}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/001_patch1-Math-85-ACS_c_rep001/harness_src/attempt_030.java']}

</details>
