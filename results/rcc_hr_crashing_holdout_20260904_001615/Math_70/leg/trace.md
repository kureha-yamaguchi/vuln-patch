# Pipeline trace — Math-70

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** overfit MISSED (all harnesses quiet on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

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
@@ -88,7 +88,7 @@
            fmin = f.value(min);
            fm = f.value(m);
 
-            if (fm * fmin > 0.0) {
+            if (fm * fmin > 0.0||i<0) {
                 // max and m bracket the root.
                 min = m;
             } else {
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method=' (~632 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.analysis.solvers.BisectionSolverTest', test_method='testMath369', source_path='/tmp/d4j/Math_70_buggy/src/test/java/org/apache/commons/math/analysis/solvers/BisectionSolverTest.java', method_source='    public void testMath369() throws Exception {\\n        UnivariateRealFunction f = new SinFunction();\\n        UnivariateRealSolver solver = new BisectionSolver();\\n        assertEquals(Math.PI, solver.solve(f, 3.0, 3.2, 3.1), solver.getAbsoluteAccuracy());\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~11,433 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java\t\n+++ /src/main/java/org/apache/commons/math/analysis/solvers/BisectionSolver.java\n@@ -69,7 +69,7 @@\n     /** {@inheritDoc} */\n     public double solve(final UnivariateRealFunction f, double min, double max, double initial)\n         throws MaxIterationsExceededException, FunctionEvaluationException {\n-        return solve(min, max);\n+        return solve(f, min, max);\n     }\n \n     /** {@inheritDoc} */\n@@ -88,7 +88,7 @@\n            fmin = f.value(min);\n            fm = f.value(m);\n \n-            if (fm * fmin > 0.0) {\n+            if (fm * fmin > 0.0||i<0) {\n                 // max and m bracket the root.\n                 min = m;\n             } else {\n",
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
    },
    {
      "func_name": "solve",
      "func_signature": "public double solve(UnivariateRealFunction f, double min, double max) throws MaxIterationsExceededException, FunctionEvaluationException",
      "func_source": "    public double solve(final UnivariateRealFunction f, double min, double max)\n        throws MaxIterationsExceededException, FunctionEvaluationException {\n\n        clearResult();\n        verifyInterval(min,max);\n        double m;\n        double fm;\n        double fmin;\n\n        int i = 0;\n        while (i < maximalIterationCount) {\n            m = UnivariateRealSolverUtils.midpoint(min, max);\n           fmin = f.value(min);\n           fm = f.value(m);\n\n            if (fm * fmin > 0.0) {\n                // max and m bracket the root.\n                min = m;\n            } else {\n                // min and m bracket the root.\n                max = m;\n            }\n\n            if (Math.abs(max - min) <= absoluteAccuracy) {\n                m = UnivariateRealSolverUtils.midpoint(min, max);\n                setResult(m, i);\n                return m;\n            }\n            ++i;\n        }\n\n        throw new MaxIterationsExceededException(maximalIterationCount);\n    }",
      "func_class": "BisectionSolver",
      "func_class_fq": "org.apache.commons.math.analysis.solvers.BisectionSolver",
      "func_param_types": [
        "UnivariateRealFunction",
        "double",
        "double"
      ],
      "fi_name": "[org.apache.commons.math.analysis.solvers.BisectionSolver].solve(org.apache.commons.math.analysis.UnivariateRealFunction,double,double)",
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
      "reachable": [
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].clearResult()",
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].verifyInterval(double,double)",
        "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].midpoint(double,double)",
        "[org.apache.commons.math.analysis.UnivariateRealFunction].value(double)",
        "Math.abs(org.apache.commons.math.analysis.solvers.BisectionSolver)",
        "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils].midpoint(org.apache.commons.math.analysis.solvers.BisectionSolver,org.apache.commons.math.analysis.solvers.BisectionSolver)",
        "[org.apache.commons.math.analysis.solvers.BisectionSolver].setResult(double,org.apache.commons.math.analysis.solvers.BisectionSolver)",
        "[org.apache.commons.math.MaxIterationsExceededException].<init>(org.apache.commons.math.analysis.solvers.BisectionSolver)",
        "[org.apache.commons.math.optimization.univariate.AbstractUnivariateRealOptimizer].clearResult()",
        "[org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl].setResult(double,int)",
        "[org.apache.commons.math.transform.FastFourierTransformer].verifyInterval(double,double)"
      ],
      "related_callees": [
        {
          "name": "clearResult",
          "source_file": "UnivariateRealSolverImpl.java",
          "signature": "final protected void clearResult()",
          "source": "    protected final void clearResult() {\n        this.iterationCount = 0;\n        this.resultComputed = false;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "verifyInterval",
          "source_file": "UnivariateRealSolverImpl.java",
          "signature": "protected void verifyInterval(double lower, double upper)",
          "source": "    protected void verifyInterval(final double lower, final double upper) {\n        if (lower >= upper) {\n            throw MathRuntimeException.createIllegalArgumentException(\n                    \"endpoints do not specify an interval: [{0}, {1}]\",\n                    lower, upper);\n        }\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "midpoint",
          "source_file": "UnivariateRealSolverUtils.java",
          "signature": "public static double midpoint(double a, double b)",
          "source": "    public static double midpoint(double a, double b) {\n        return (a + b) * .5;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "setResult",
          "source_file": "UnivariateRealSolverImpl.java",
          "signature": "final protected void setResult(double newResult, int iterationCount)",
          "source": "    protected final void setResult(final double newResult, final int iterationCount) {\n        this.result         = newResult;\n        this.iterationCount = iterationCount;\n        this.resultComputed = true;\n    }",
          "is_abstract": false,
          "impls": [
            [
              "UnivariateRealSolverImpl.java",
              "    protected final void setResult(final double newResult, final int iterationCount) {\n        this.result         = newResult;\n        this.iterationCount = iterationCount;\n        this.resultComputed = true;\n    }"
            ],
            [
              "UnivariateRealSolverImpl.java",
              "    protected final void setResult(final double x, final double fx,\n                                   final int iterationCount) {\n        this.result         = x;\n        this.functionValue  = fx;\n        this.iterationCount = iterationCount;\n        this.resultComputed = true;\n    }"
            ]
          ]
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.analysis.solvers",
  "root_cause_reachable": [
    "BisectionSolver.solve",
    "BisectionSolver.clearResult",
    "BisectionSolver.verifyInterval",
    "UnivariateRealSolverUtils.midpoint",
    "UnivariateRealFunction.value",
    "BisectionSolver.setResult",
    "MaxIterationsExceededException.<init>",
    "AbstractUnivariateRealOptimizer.clearResult",
    "UnivariateRealSolverImpl.setResult",
    "FastFourierTransformer.verifyInterval"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import org.apache.commons.math.FunctionEvaluationException;",
    "import org.apache.commons.math.MaxIterationsExceededException;",
    "import org.apache.commons.math.analysis.UnivariateRealFunction;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,419 chars)</summary>

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
@@ -88,7 +88,7 @@
            fmin = f.value(min);
            fm = f.value(m);
 
-            if (fm * fmin > 0.0) {
+            if (fm * fmin > 0.0||i<0) {
                 // max and m bracket the root.
                 min = m;
             } else {

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

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
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
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="clearResult" from="UnivariateRealSolverImpl.java">
<signature>
final protected void clearResult()
</signature>
<code>
    protected final void clearResult() {
        this.iterationCount = 0;
        this.resultComputed = false;
    }
</code>
</callee>
<callee name="verifyInterval" from="UnivariateRealSolverImpl.java">
<signature>
protected void verifyInterval(double lower, double upper)
</signature>
<code>
    protected void verifyInterval(final double lower, final double upper) {
        if (lower >= upper) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "endpoints do not specify an interval: [{0}, {1}]",
                    lower, upper);
        }
    }
</code>
</callee>
<callee name="midpoint" from="UnivariateRealSolverUtils.java">
<signature>
public static double midpoint(double a, double b)
</signature>
<code>
    public static double midpoint(double a, double b) {
        return (a + b) * .5;
    }
</code>
</callee>
<callee name="setResult" from="UnivariateRealSolverImpl.java">
<signature>
final protected void setResult(double newResult, int iterationCount)
</signature>
<code>
    protected final void setResult(final double newResult, final int iterationCount) {
        this.result         = newResult;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</code>
<implementation in="UnivariateRealSolverImpl.java">
    protected final void setResult(final double newResult, final int iterationCount) {
        this.result         = newResult;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</implementation>
<implementation in="UnivariateRealSolverImpl.java">
    protected final void setResult(final double x, final double fx,
                                   final int iterationCount) {
        this.result         = x;
        this.functionValue  = fx;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve`, `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
- BisectionSolver.solve
- BisectionSolver.clearResult
- BisectionSolver.verifyInterval
- UnivariateRealSolverUtils.midpoint
- UnivariateRealFunction.value
- BisectionSolver.setResult
- MaxIterationsExceededException.<init>
- AbstractUnivariateRealOptimizer.clearResult
- UnivariateRealSolverImpl.setResult
- FastFourierTransformer.verifyInterval
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
<details open><summary>▸ Output (~8,124 chars)</summary>

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
        runOne(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: generate many valid-by-construction intervals that bracket a real root of sin(x).
        // For any correct implementation, the overloads
        //   solve(f, min, max, initial) and solve(f, min, max)
        // must agree on the same function and interval. A "fix" that only suppresses the throw or
        // bypasses the real solve path breaks this sibling-agreement post-condition.
        int rootIndex = data.consumeInt(-1000, 1000);
        double root = rootIndex * Math.PI;

        int leftMilli = data.consumeInt(1, 1500);
        int rightMilli = data.consumeInt(1, 1500);
        double left = leftMilli / 1000.0;
        double right = rightMilli / 1000.0;

        double min = root - left;
        double max = root + right;

        int initialMode = data.consumeInt(0, 4);
        double initial;
        switch (initialMode) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) * 0.5;
                break;
            case 3:
                initial = root;
                break;
            default:
                double frac = data.consumeInt(0, 1000) / 1000.0;
                initial = min + (max - min) * frac;
                break;
        }

        runOne(f, min, max, initial, false);

        // Additional varied but still valid intervals around neighboring roots.
        int variants = data.consumeInt(0, 3);
        for (int i = 0; i < variants; i++) {
            int deltaRoot = data.consumeInt(-3, 3);
            double r = (rootIndex + deltaRoot) * Math.PI;
            double l = data.consumeInt(1, 1200) / 1000.0;
            double rr = data.consumeInt(1, 1200) / 1000.0;
            double a = r - l;
            double b = r + rr;
            double midFrac = data.consumeInt(0, 1000) / 1000.0;
            double init = a + (b - a) * midFrac;
            runOne(f, a, b, init, false);
        }
    }

    private static void runOne(UnivariateRealFunction f, double min, double max, double initial, boolean anchor) {
        if (f == null || !(min < max) || Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(initial)
                || Double.isInfinite(min) || Double.isInfinite(max) || Double.isInfinite(initial)) {
            return;
        }

        BisectionSolver withInitial = new BisectionSolver();
        BisectionSolver withoutInitial = new BisectionSolver();

        double lhs;
        double rhs;

        try {
            lhs = withInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpe(t)) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            rhs = withoutInitial.solve(f, min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            // If one side rejects/throws, the sibling-agreement oracle does not apply.
            return;
        }

        // Post-condition / metamorphic check:
        // The documented overloads solve(f,min,max,initial) and solve(f,min,max) are the same operation
        // for BisectionSolver on the same function and interval, so for any correct implementation they
        // must return the same root up to solver accuracy.
        double tol = Math.max(Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()), 1e-12);
        if (Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException(
                    "[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=min=" + min + ",max=" + max + ",initial=" + initial
                            + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol + " anchor=" + anchor);
        }

        // Secondary consistency check: after a successful solve, getResult() must report the value returned.
        // This uses only the object's own API and catches fixes that skip bookkeeping.
        try {
            double reported = withInitial.getResult();
            if (Math.abs(reported - lhs) > tol) {
                throw new RuntimeException(
                        "[oracle:result-cache] metamorphic violation: getResult() must equal returned solve result"
                                + " input=min=" + min + ",max=" + max + ",initial=" + initial
                                + " returned=" + lhs + " reported=" + reported + " tol=" + tol + " anchor=" + anchor);
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String cn = t.getClass().getName();
        return cn.contains("ConvergenceException")
                || cn.contains("FunctionEvaluationException")
                || cn.contains("MaxIterationsExceededException")
                || cn.contains("MathRuntimeException");
    }

    private static boolean isRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                    && "solve".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "midpoint".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl".equals(cls)
                    && ("setResult".equals(method) || "clearResult".equals(method) || "verifyInterval".equals(method))) {
                return true;
            }
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                    && ("setResult".equals(method) || "clearResult".equals(method) || "verifyInterval".equals(method))) {
                return true;
            }
            if ("org.apache.commons.math.analysis.solvers.FastFourierTransformer".equals(cls)
                    && "verifyInterval".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.MaxIterationsExceededException".equals(cls)
                    && "<init>".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.optimization.univariate.AbstractUnivariateRealOptimizer".equals(cls)
                    && "clearResult".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls)
                    && "value".equals(method)) {
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
<details><summary>▸ Prompt (2 message(s), ~30,046 chars, 1 new)</summary>

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
@@ -69,7 +69,7 @@
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f, min, max);
     }
 
     /** {@inheritDoc} */
@@ -88,7 +88,7 @@
            fmin = f.value(min);
            fm = f.value(m);
 
-            if (fm * fmin > 0.0) {
+            if (fm * fmin > 0.0||i<0) {
                 // max and m bracket the root.
                 min = m;
             } else {

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

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
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
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="clearResult" from="UnivariateRealSolverImpl.java">
<signature>
final protected void clearResult()
</signature>
<code>
    protected final void clearResult() {
        this.iterationCount = 0;
        this.resultComputed = false;
    }
</code>
</callee>
<callee name="verifyInterval" from="UnivariateRealSolverImpl.java">
<signature>
protected void verifyInterval(double lower, double upper)
</signature>
<code>
    protected void verifyInterval(final double lower, final double upper) {
        if (lower >= upper) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "endpoints do not specify an interval: [{0}, {1}]",
                    lower, upper);
        }
    }
</code>
</callee>
<callee name="midpoint" from="UnivariateRealSolverUtils.java">
<signature>
public static double midpoint(double a, double b)
</signature>
<code>
    public static double midpoint(double a, double b) {
        return (a + b) * .5;
    }
</code>
</callee>
<callee name="setResult" from="UnivariateRealSolverImpl.java">
<signature>
final protected void setResult(double newResult, int iterationCount)
</signature>
<code>
    protected final void setResult(final double newResult, final int iterationCount) {
        this.result         = newResult;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</code>
<implementation in="UnivariateRealSolverImpl.java">
    protected final void setResult(final double newResult, final int iterationCount) {
        this.result         = newResult;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</implementation>
<implementation in="UnivariateRealSolverImpl.java">
    protected final void setResult(final double x, final double fx,
                                   final int iterationCount) {
        this.result         = x;
        this.functionValue  = fx;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve`, `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
- BisectionSolver.solve
- BisectionSolver.clearResult
- BisectionSolver.verifyInterval
- UnivariateRealSolverUtils.midpoint
- UnivariateRealFunction.value
- BisectionSolver.setResult
- MaxIterationsExceededException.<init>
- AbstractUnivariateRealOptimizer.clearResult
- UnivariateRealSolverImpl.setResult
- FastFourierTransformer.verifyInterval
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runOne
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve
- BisectionSolver.clearResult
- BisectionSolver.verifyInterval
- UnivariateRealSolverUtils.midpoint
- UnivariateRealFunction.value
- BisectionSolver.setResult
- MaxIterationsExceededException.<init>
- AbstractUnivariateRealOptimizer.clearResult
- UnivariateRealSolverImpl.setResult
- FastFourierTransformer.verifyInterval
Check FAMILIES already covered by accepted harnesses: {overload-agree, result-cache}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,662 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        // Anchor: exact regression input from the failing test.
        runKnownValidCase(f, 3.0, 3.2, 3.1, Math.PI, "anchor");

        // Explore: many other valid intervals around exact sin roots k*pi.
        int k = data.consumeInt(-1000, 1000);

        double root = k * Math.PI;

        // Keep offsets moderate and strictly positive so [min,max] is a real interval
        // and brackets the known root by construction.
        int leftMilli = data.consumeInt(1, 1500);
        int rightMilli = data.consumeInt(1, 1500);
        double leftOffset = leftMilli / 1000.0;
        double rightOffset = rightMilli / 1000.0;

        double min = root - leftOffset;
        double max = root + rightOffset;

        double initial;
        if (data.consumeBoolean()) {
            // Initial guess inside the interval.
            int pos = data.consumeInt(0, 10000);
            initial = min + (max - min) * (pos / 10000.0);
        } else {
            // Also exercise endpoint and near-endpoint initials; this overload must still solve.
            initial = data.consumeBoolean() ? min : max;
        }

        runKnownValidCase(f, min, max, initial, root, "explore");

        // More valid shapes around the same exact root: narrower intervals and asymmetric initials.
        if (data.remainingBytes() > 0) {
            double narrowLeft = data.consumeInt(1, 200) / 1000.0;
            double narrowRight = data.consumeInt(1, 200) / 1000.0;
            double min2 = root - narrowLeft;
            double max2 = root + narrowRight;
            double initial2 = data.consumeBoolean() ? (root - narrowLeft / 2.0) : (root + narrowRight / 2.0);
            runKnownValidCase(f, min2, max2, initial2, root, "explore-narrow");
        }
    }

    private static void runKnownValidCase(SinFunction f, double min, double max, double initial, double expectedRoot, String tag) {
        BisectionSolver solver = new BisectionSolver();
        try {
            double result = solver.solve(f, min, max, initial);

            // Post-condition / oracle:
            // This solver is for finding a root in [min,max]. We construct inputs so that
            // sin(expectedRoot) == 0 exactly and expectedRoot is strictly inside [min,max].
            // A correct implementation must therefore return a value within the interval and
            // close to that known root; deleting the throw or silently skipping the real solve
            // would violate this observable contract.
            double acc = solver.getAbsoluteAccuracy();
            double tol = Math.max(acc * 8.0, 1e-6);

            if (!(result >= min - tol && result <= max + tol)) {
                throw new RuntimeException(
                    "[oracle:in-interval] metamorphic violation: solved root left interval tag=" + tag +
                    " min=" + min + " max=" + max + " initial=" + initial + " result=" + result);
            }

            if (Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException(
                    "[oracle:known-sin-root] metamorphic violation: expected known sin root tag=" + tag +
                    " min=" + min + " max=" + max + " initial=" + initial +
                    " expected=" + expectedRoot + " result=" + result + " tol=" + tol);
            }

            double residual = Math.abs(f.value(result));
            if (!(residual <= 1e-4)) {
                throw new RuntimeException(
                    "[oracle:small-residual] metamorphic violation: returned value is not a root tag=" + tag +
                    " min=" + min + " max=" + max + " initial=" + initial +
                    " expected=" + expectedRoot + " result=" + result + " residual=" + residual);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantToPatchedRegion(t)) {
                throw new RuntimeException(
                    "[oracle:valid-solve] metamorphic violation: valid bisection input should solve tag=" + tag +
                    " min=" + min + " max=" + max + " initial=" + initial +
                    " expected=" + expectedRoot, t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRelevantToPatchedRegion(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("solve".equals(method)) {
                if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                        || "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                        || "org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl".equals(cls)
                        || "org.apache.commons.math.analysis.solvers.AbstractUnivariateRealOptimizer".equals(cls)
                        || "org.apache.commons.math.transform.FastFourierTransformer".equals(cls)) {
                    return true;
                }
            }
            if ("value".equals(method) && "org.apache.commons.math.analysis.SinFunction".equals(cls)) {
                return true;
            }
            if ("setResult".equals(method) && "org.apache.commons.math.analysis.solvers.UnivariateRealSolverImpl".equals(cls)) {
                return true;
            }
            if ("<init>".equals(method) && "org.apache.commons.math.MaxIterationsExceededException".equals(cls)) {
                return true;
            }
            if ("clearResult".equals(method)
                    && ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                    || "org.apache.commons.math.analysis.solvers.AbstractUnivariateRealOptimizer".equals(cls))) {
                return true;
            }
            if ("verifyInterval".equals(method)
                    && ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(cls)
                    || "org.apache.commons.math.transform.FastFourierTransformer".equals(cls))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:valid-solve] metamorphic violation: valid bisection input should solve tag=anchor min=3.0 max=3.2 initial=3.1 expected=3.141592653589793

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~30,376 chars, 1 new)</summary>

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
@@ -69,7 +69,7 @@
     /** {@inheritDoc} */
     public double solve(final UnivariateRealFunction f, double min, double max, double initial)
         throws MaxIterationsExceededException, FunctionEvaluationException {
-        return solve(min, max);
+        return solve(f, min, max);
     }
 
     /** {@inheritDoc} */
@@ -88,7 +88,7 @@
            fmin = f.value(min);
            fm = f.value(m);
 
-            if (fm * fmin > 0.0) {
+            if (fm * fmin > 0.0||i<0) {
                 // max and m bracket the root.
                 min = m;
             } else {

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

Function `solve`:
<signature>
public double solve(UnivariateRealFunction f, double min, double max) throws MaxIterationsExceededException, FunctionEvaluationException
</signature>
<code>
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
</code>
Methods called by `solve` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="clearResult" from="UnivariateRealSolverImpl.java">
<signature>
final protected void clearResult()
</signature>
<code>
    protected final void clearResult() {
        this.iterationCount = 0;
        this.resultComputed = false;
    }
</code>
</callee>
<callee name="verifyInterval" from="UnivariateRealSolverImpl.java">
<signature>
protected void verifyInterval(double lower, double upper)
</signature>
<code>
    protected void verifyInterval(final double lower, final double upper) {
        if (lower >= upper) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "endpoints do not specify an interval: [{0}, {1}]",
                    lower, upper);
        }
    }
</code>
</callee>
<callee name="midpoint" from="UnivariateRealSolverUtils.java">
<signature>
public static double midpoint(double a, double b)
</signature>
<code>
    public static double midpoint(double a, double b) {
        return (a + b) * .5;
    }
</code>
</callee>
<callee name="setResult" from="UnivariateRealSolverImpl.java">
<signature>
final protected void setResult(double newResult, int iterationCount)
</signature>
<code>
    protected final void setResult(final double newResult, final int iterationCount) {
        this.result         = newResult;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</code>
<implementation in="UnivariateRealSolverImpl.java">
    protected final void setResult(final double newResult, final int iterationCount) {
        this.result         = newResult;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</implementation>
<implementation in="UnivariateRealSolverImpl.java">
    protected final void setResult(final double x, final double fx,
                                   final int iterationCount) {
        this.result         = x;
        this.functionValue  = fx;
        this.iterationCount = iterationCount;
        this.resultComputed = true;
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `BisectionSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `solve`, `solve` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
- BisectionSolver.solve
- BisectionSolver.clearResult
- BisectionSolver.verifyInterval
- UnivariateRealSolverUtils.midpoint
- UnivariateRealFunction.value
- BisectionSolver.setResult
- MaxIterationsExceededException.<init>
- AbstractUnivariateRealOptimizer.clearResult
- UnivariateRealSolverImpl.setResult
- FastFourierTransformer.verifyInterval
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- org.apache.commons.math.analysis.solvers.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.analysis.solvers.FuzzHarness.runKnownValidCase
- org.apache.commons.math.analysis.solvers.FuzzHarness.runOne
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve
- java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.runKnownValidCase
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- BisectionSolver.solve
- BisectionSolver.clearResult
- BisectionSolver.verifyInterval
- UnivariateRealSolverUtils.midpoint
- UnivariateRealFunction.value
- BisectionSolver.setResult
- MaxIterationsExceededException.<init>
- AbstractUnivariateRealOptimizer.clearResult
- UnivariateRealSolverImpl.setResult
- FastFourierTransformer.verifyInterval
Check FAMILIES already covered by accepted harnesses: {in-interval, known-sin-root, overload-agree, result-cache, small-residual, valid-solve}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.math.analysis.solvers.BisectionSolver.solve; java.lang.RuntimeException@org.apache.commons.math.analysis.solvers.FuzzHarness.runKnownValidCase. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,942 chars)</summary>

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

        /* Contract/oracle:
         * Both solve(f,min,max,initial) and solve(f,min,max) are solver entrypoints for the same root-finding task.
         * For SinFunction on two nested intervals that both contain the same unique root k*pi and no other root,
         * every correct implementation must converge to that same root. A patch that merely suppresses the NPE but
         * skips the real solve, uses the wrong function field, or returns a stale/wrong value will violate this.
         */

        // ANCHOR: exact regression-test inputs first.
        double anchorOuter = trySolve4(f, 3.0, 3.2, 3.1);
        if (!Double.isNaN(anchorOuter)) {
            double anchorInner = trySolve3(f, 3.1, 3.2);
            if (!Double.isNaN(anchorInner)) {
                double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy() * 8.0, 1e-12);
                if (Math.abs(anchorOuter - anchorInner) > tol) {
                    throw new RuntimeException(
                        "[oracle:nested-root] metamorphic violation: nested intervals with the same unique sin root disagree"
                            + " outer=" + anchorOuter + " inner=" + anchorInner
                            + " outerInterval=[3.0,3.2] innerInterval=[3.1,3.2]");
                }
            }
        }

        // EXPLORE: many valid-by-construction intervals around k*pi, always with a unique shared root.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double leftWidthOuter = scaledPositive(data.consumeInt(1, 1000), 0.01, 1.0);
        double rightWidthOuter = scaledPositive(data.consumeInt(1, 1000), 0.01, 1.0);

        double leftWidthInner = scaledPositive(data.consumeInt(1, 1000), 0.005, leftWidthOuter);
        double rightWidthInner = scaledPositive(data.consumeInt(1, 1000), 0.005, rightWidthOuter);

        double minOuter = root - leftWidthOuter;
        double maxOuter = root + rightWidthOuter;
        double minInner = root - leftWidthInner;
        double maxInner = root + rightWidthInner;

        if (!(minOuter < minInner && minInner < root && root < maxInner && maxInner < maxOuter)) {
            return;
        }

        double initialOuter = choosePoint(data, minOuter, maxOuter);
        double initialInner = choosePoint(data, minInner, maxInner);

        double outer = trySolve4(f, minOuter, maxOuter, initialOuter);
        if (Double.isNaN(outer)) {
            return;
        }

        // Drive a different reachable public entrypoint in the same region.
        double inner = data.consumeBoolean()
            ? trySolve3(f, minInner, maxInner)
            : trySolve4(f, minInner, maxInner, initialInner);
        if (Double.isNaN(inner)) {
            return;
        }

        double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy() * 8.0, 1e-12);
        if (Math.abs(outer - inner) > tol) {
            throw new RuntimeException(
                "[oracle:nested-root] metamorphic violation: same unique sin root on nested valid intervals produced different answers"
                    + " k=" + k
                    + " outer=" + outer
                    + " inner=" + inner
                    + " outerInterval=[" + minOuter + "," + maxOuter + "]"
                    + " innerInterval=[" + minInner + "," + maxInner + "]"
                    + " initialOuter=" + initialOuter
                    + " initialInner=" + initialInner);
        }
    }

    private static double trySolve4(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isValidation(t)) {
                return Double.NaN;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:nested-root] metamorphic violation: valid sin interval triggered root-cause failure in solve(f,min,max,initial)"
                        + " interval=[" + min + "," + max + "] initial=" + initial,
                    t);
            }
            return Double.NaN;
        }
    }

    private static double trySolve3(UnivariateRealFunction f, double min, double max) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return solver.solve(f, min, max);
        } catch (Throwable t) {
            if (isValidation(t)) {
                return Double.NaN;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:nested-root] metamorphic violation: valid sin interval triggered root-cause failure in solve(f,min,max)"
                        + " interval=[" + min + "," + max + "]",
                    t);
            }
            return Double.NaN;
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

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Illegal") || name.contains("Invalid");
    }

    private static double scaledPositive(int value, double min, double max) {
        if (max <= min) {
            return min;
        }
        double unit = (value - 1) / 999.0;
        return min + unit * (max - min);
    }

    private static double choosePoint(FuzzedDataProvider data, double min, double max) {
        int selector = data.consumeInt(0, 1000);
        double unit = selector / 1000.0;
        double p = min + unit * (max - min);
        if (p <= min) {
            p = Math.nextUp(min);
        }
        if (p >= max) {
            p = Math.nextAfter(max, min);
        }
        return p;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:nested-root] metamorphic violation: valid sin interval triggered root-cause failure in solve(f,min,max,initial) interval=[3.0,3.2] initial=3.1

---
## [9] ⚙️ corpus-seed · `attempt_001`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['3.0', '3.2', '3.1', '3.0L', '3.0l', '3.0F', '3.0f', '3.0D']}

---
## [10] ⚙️ corpus-seed · `attempt_002`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['3.0', '3.2', '3.1', '3.0L', '3.0l', '3.0F', '3.0f', '3.0D']}

---
## [11] ⚙️ corpus-seed · `attempt_003`
**output:** **24 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['3.0', '3.2', '3.1', '3.0L', '3.0l', '3.0F', '3.0f', '3.0D']}

---
## [12] ⚙️ patched-fuzz · `attempt_001`
**output:** **quiet on patched build (no overfit signal)**

---
## [13] ⚙️ patched-fuzz · `attempt_002`
**output:** **quiet on patched build (no overfit signal)**

---
## [14] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**
