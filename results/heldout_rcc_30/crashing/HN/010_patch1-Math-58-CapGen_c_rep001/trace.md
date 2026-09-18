# Pipeline trace — Math-58

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
+++ /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
@@ -118,7 +118,8 @@ public class GaussianFitter extends CurveFitter {
      */
     public double[] fit() {
         final double[] guess = (new ParameterGuesser(getObservations())).guess();
-        return fit(new Gaussian.Parametric(), guess);
+        return fit( guess)
+;
     }
 
     /**
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.optimization.fitting.GaussianFitterTest', test_metho (~2,125 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.optimization.fitting.GaussianFitterTest', test_method='testMath519', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/Math_58_buggy/src/test/java/org/apache/commons/math/optimization/fitting/GaussianFitterTest.java', method_source='    @Test\\n    public void testMath519() {\\n        // The optimizer will try negative sigma values but \"GaussianFitter\"\\n        // will catch the raised exceptions and return NaN values instead.\\n\\n        final double[] data = { \\n            1.1143831578403364E-29,\\n            4.95281403484594E-28,\\n            1.1171347211930288E-26,\\n            1.7044813962636277E-25,\\n            1.9784716574832164E-24,\\n            1.8630236407866774E-23,\\n            1.4820532905097742E-22,\\n            1.0241963854632831E-21,\\n            6.275077366673128E-21,\\n            3.461808994532493E-20,\\n            1.7407124684715706E-19,\\n            8.056687953553974E-19,\\n            3.460193945992071E-18,\\n            1.3883326374011525E-17,\\n            5.233894983671116E-17,\\n            1.8630791465263745E-16,\\n            6.288759227922111E-16,\\n            2.0204433920597856E-15,\\n            6.198768938576155E-15,\\n            1.821419346860626E-14,\\n            5.139176445538471E-14,\\n            1.3956427429045787E-13,\\n            3.655705706448139E-13,\\n            9.253753324779779E-13,\\n            2.267636001476696E-12,\\n            5.3880460095836855E-12,\\n            1.2431632654852931E-11\\n        };\\n\\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\\n        for (int i = 0; i < data.length; i++) {\\n            fitter.addObservedPoint(i, data[i]);\\n        }\\n        final double[] p = fitter.fit();\\n\\n        Assert.assertEquals(53.1572792, p[1], 1e-7);\\n        Assert.assertEquals(5.75214622, p[2], 1e-8);\\n    }', exception_type='org.apache.commons.math.exception.NotStrictlyPositiveException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math.optimization.fitting.GaussianFitterTest::testMath519",
    "failure_message": "--- org.apache.commons.math.optimization.fitting.GaussianFitterTest::testMath519\norg.apache.commons.math.exception.NotStrictlyPositiveException: -1.277 is smaller than, or equal to, the minimum (0)",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~12,442 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java\n+++ /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java\n@@ -118,7 +118,8 @@ public class GaussianFitter extends CurveFitter {\n      */\n     public double[] fit() {\n         final double[] guess = (new ParameterGuesser(getObservations())).guess();\n-        return fit(new Gaussian.Parametric(), guess);\n+        return fit( guess)\n+;\n     }\n \n     /**",
  "functions": [
    {
      "func_name": "fit",
      "func_signature": "public double[] fit()",
      "func_source": "    public double[] fit() {\n        final double[] guess = (new ParameterGuesser(getObservations())).guess();\n        return fit(new Gaussian.Parametric(), guess);\n    }",
      "func_class": "GaussianFitter",
      "func_class_fq": "org.apache.commons.math.optimization.fitting.GaussianFitter",
      "func_param_types": [],
      "fi_name": "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
      "overload_types": [
        [
          "double[]"
        ],
        []
      ],
      "xrefs": [
        "@Test\n    public void testFit01()\n    throws OptimizationException {\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\n        addDatasetToGaussianFitter(DATASET1, fitter);\n        double[] parameters = fitter.fit();\n\n        Assert.assertEquals(3496978.1837704973, parameters[0], 1e-4);\n        Assert.assertEquals(4.054933085999146, parameters[1], 1e-4);\n        Assert.assertEquals(0.015039355620304326, parameters[2], 1e-4);\n    }",
        "@Test(expected=MathIllegalArgumentException.class)\n    public void testFit02()\n    throws OptimizationException {\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\n        fitter.fit();\n    }",
        "@Test(expected=MathIllegalArgumentException.class)\n    public void testFit03()\n    throws OptimizationException {\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\n        addDatasetToGaussianFitter(new double[][] {\n            {4.0254623,  531026.0},\n            {4.02804905, 664002.0}},\n            fitter);\n        fitter.fit();\n    }",
        "@Test\n    public void testFit04()\n    throws OptimizationException \n    {\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\n        addDatasetToGaussianFitter(DATASET2, fitter);\n        double[] parameters = fitter.fit();\n\n        Assert.assertEquals(233003.2967252038, parameters[0], 1e-4);\n        Assert.assertEquals(-10.654887521095983, parameters[1], 1e-4);\n        Assert.assertEquals(4.335937353196641, parameters[2], 1e-4);\n    }",
        "@Test\n    public void testFit05()\n    throws OptimizationException {\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\n        addDatasetToGaussianFitter(DATASET3, fitter);\n        double[] parameters = fitter.fit();\n\n        Assert.assertEquals(283863.81929180305, parameters[0], 1e-4);\n        Assert.assertEquals(-13.29641995105174, parameters[1], 1e-4);\n        Assert.assertEquals(1.7297330293549908, parameters[2], 1e-4);\n    }"
      ],
      "xref_names": [
        "[org.apache.commons.math.optimization.fitting.GaussianFitterTest].testFit01()",
        "[org.apache.commons.math.optimization.fitting.GaussianFitterTest].testFit02()",
        "[org.apache.commons.math.optimization.fitting.GaussianFitterTest].testFit03()",
        "[org.apache.commons.math.optimization.fitting.GaussianFitterTest].testFit04()",
        "[org.apache.commons.math.optimization.fitting.GaussianFitterTest].testFit05()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
          "(new ParameterGuesser(getObservations())).guess()"
        ],
        [
          "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
          "[org.apache.commons.math.analysis.function.Gaussian.Parametric].<init>()"
        ],
        [
          "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
          "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit(org.apache.commons.math.analysis.function.Gaussian.Parametric,org.apache.commons.math.optimization.fitting.GaussianFitter)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()": 0,
        "(new ParameterGuesser(getObservations())).guess()": 1,
        "[org.apache.commons.math.analysis.function.Gaussian.Parametric].<init>()": 1,
        "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit(org.apache.commons.math.analysis.function.Gaussian.Parametric,org.apache.commons.math.optimization.fitting.GaussianFitter)": 1
      },
      "reachable": [
        "(new ParameterGuesser(getObservations())).guess()",
        "[org.apache.commons.math.analysis.function.Gaussian.Parametric].<init>()",
        "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit(org.apache.commons.math.analysis.function.Gaussian.Parametric,org.apache.commons.math.optimization.fitting.GaussianFitter)",
        "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit(double[])",
        "[org.apache.commons.math.optimization.fitting.CurveFitter].getObservations()",
        "[org.apache.commons.math.optimization.fitting.HarmonicFitter.ParameterGuesser].guess()"
      ],
      "related_callees": [
        {
          "name": "guess",
          "source_file": "GaussianFitter.java",
          "signature": "public double[] guess()",
          "source": "        public double[] guess() {\n            if (parameters == null) {\n                parameters = basicGuess(observations);\n            }\n            return parameters.clone();\n        }",
          "is_abstract": false,
          "impls": [
            [
              "GaussianFitter.java",
              "        public double[] guess() {\n            if (parameters == null) {\n                parameters = basicGuess(observations);\n            }\n            return parameters.clone();\n        }"
            ],
            [
              "HarmonicFitter.java",
              "        public double[] guess() {\n            sortObservations();\n            guessAOmega();\n            guessPhi();\n            return new double[] { a, omega, phi };\n        }"
            ]
          ]
        },
        {
          "name": "getObservations",
          "source_file": "CurveFitter.java",
          "signature": "public WeightedObservedPoint[] getObservations()",
          "source": "    public WeightedObservedPoint[] getObservations() {\n        return observations.toArray(new WeightedObservedPoint[observations.size()]);\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "fit",
          "source_file": "CurveFitter.java",
          "signature": "public double[] fit(ParametricUnivariateRealFunction f, double[] initialGuess)",
          "source": "    public double[] fit(final ParametricUnivariateRealFunction f, final double[] initialGuess) {\n        return fit(Integer.MAX_VALUE, f, initialGuess);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "CurveFitter.java",
              "    public double[] fit(final ParametricUnivariateRealFunction f, final double[] initialGuess) {\n        return fit(Integer.MAX_VALUE, f, initialGuess);\n    }"
            ],
            [
              "CurveFitter.java",
              "    public double[] fit(int maxEval, final ParametricUnivariateRealFunction f,\n                        final double[] initialGuess) {\n        // prepare least squares problem\n        double[] target  = new double[observations.size()];\n        double[] weights = new double[observations.size()];\n        int i = 0;\n        for (WeightedObservedPoint point : observations) {\n            target[i]  = point.getY();\n            weights[i] = point.getWeight();\n            ++i;\n        }\n\n        // perform the fit\n        VectorialPointValuePair optimum =\n            optimizer.optimize(maxEval, new TheoreticalValuesFunction(f),\n                               target, weights, initialGuess);\n\n        // extract the coefficients\n        return optimum.getPointRef();\n    }"
            ],
            [
              "GaussianFitter.java",
              "    public double[] fit(double[] initialGuess) {\n        final ParametricUnivariateRealFunction f = new ParametricUnivariateRealFunction() {\n                private final ParametricUnivariateRealFunction g = new Gaussian.Parametric();\n\n                public double value(double x, double[] p) {\n                    double v = Double.POSITIVE_INFINITY;\n                    try {\n                        v = g.value(x, p);\n                    } catch (NotStrictlyPositiveException e) {\n                        // Do nothing.\n                    }\n                    return v;\n                }\n\n                public double[] gradient(double x, double[] p) {\n                    double[] v = { Double.POSITIVE_INFINITY,\n                                   Double.POSITIVE_INFINITY,\n                                   Double.POSITIVE_INFINITY };\n                    try {\n                        v = g.gradient(x, p);\n                    } catch (NotStrictlyPositiveException e) {\n                        // Do nothing.\n                    }\n                    return v;\n                }\n            };\n\n        return fit(f, initialGuess);\n    }"
            ],
            [
              "HarmonicFitter.java",
              "    public double[] fit(double[] initialGuess) {\n        return fit(new HarmonicOscillator.Parametric(), initialGuess);\n    }"
            ]
          ]
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.optimization.fitting",
  "root_cause_reachable": [
    "Parametric.<init>",
    "GaussianFitter.fit(Parametric, GaussianFitter)",
    "GaussianFitter.fit(double[])",
    "CurveFitter.getObservations",
    "ParameterGuesser.guess"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.util.Arrays;",
    "import java.util.Comparator;",
    "import org.apache.commons.math.analysis.function.Gaussian;",
    "import org.apache.commons.math.analysis.ParametricUnivariateRealFunction;",
    "import org.apache.commons.math.exception.NullArgumentException;",
    "import org.apache.commons.math.exception.NumberIsTooSmallException;",
    "import org.apache.commons.math.exception.OutOfRangeException;",
    "import org.apache.commons.math.exception.ZeroException;",
    "import org.apache.commons.math.exception.NotStrictlyPositiveException;",
    "import org.apache.commons.math.exception.util.LocalizedFormats;",
    "import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;",
    "import org.apache.commons.math.optimization.fitting.CurveFitter;",
    "import org.apache.commons.math.optimization.fitting.WeightedObservedPoint;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
      "(new ParameterGuesser(getObservations())).guess()"
    ],
    [
      "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
      "[org.apache.commons.math.analysis.function.Gaussian.Parametric].<init>()"
    ],
    [
      "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()",
      "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit(org.apache.commons.math.analysis.function.Gaussian.Parametric,org.apache.commons.math.optimization.fitting.GaussianFitter)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit()": 0,
    "(new ParameterGuesser(getObservations())).guess()": 1,
    "[org.apache.commons.math.analysis.function.Gaussian.Parametric].<init>()": 1,
    "[org.apache.commons.math.optimization.fitting.GaussianFitter].fit(org.apache.commons.math.analysis.function.Gaussian.Parametric,org.apache.commons.math.optimization.fitting.GaussianFitter)": 1
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~19,113 chars)</summary>

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
- Package: `org.apache.commons.math.optimization.fitting` (`package org.apache.commons.math.optimization.fitting;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
+++ /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
@@ -118,7 +118,8 @@ public class GaussianFitter extends CurveFitter {
      */
     public double[] fit() {
         final double[] guess = (new ParameterGuesser(getObservations())).guess();
-        return fit(new Gaussian.Parametric(), guess);
+        return fit( guess)
+;
     }
 
     /**
</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.util.Arrays;
import java.util.Comparator;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.analysis.ParametricUnivariateRealFunction;
import org.apache.commons.math.exception.NullArgumentException;
import org.apache.commons.math.exception.NumberIsTooSmallException;
import org.apache.commons.math.exception.OutOfRangeException;
import org.apache.commons.math.exception.ZeroException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.exception.util.LocalizedFormats;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.fitting.CurveFitter;
import org.apache.commons.math.optimization.fitting.WeightedObservedPoint;
</source_imports>

Function `fit`:
<signature>
public double[] fit()
</signature>
<code>
    public double[] fit() {
        final double[] guess = (new ParameterGuesser(getObservations())).guess();
        return fit(new Gaussian.Parametric(), guess);
    }
</code>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `GaussianFitter`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.exception.NotStrictlyPositiveException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `fit`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: org.apache.commons.math.exception.NotStrictlyPositiveException
message: -1.277 is smaller than, or equal to, the minimum (0)
thrown_at: org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters(Gaussian.java:183)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.optimization.fitting.GaussianFitterTest" method="testMath519">
    @Test
    public void testMath519() {
        // The optimizer will try negative sigma values but "GaussianFitter"
        // will catch the raised exceptions and return NaN values instead.

        final double[] data = { 
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < data.length; i++) {
            fitter.addObservedPoint(i, data[i]);
        }
        final double[] p = fitter.fit();

        Assert.assertEquals(53.1572792, p[1], 1e-7);
        Assert
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.optimization.fitting.GaussianFitterTest::testMath519
org.apache.commons.math.exception.NotStrictlyPositiveException: -1.277 is smaller than, or equal to, the minimum (0)
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param optimizer Optimizer to use for the fitting.
  - @param initialGuess First guess values in the following order:
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  fit(double[] initialGuess) / ()

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): fit, guess

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
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,042 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        runOverloadAgreement(ANCHOR_DATA);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double mean = data.consumeInt(0, Math.max(0, len - 1)) + data.consumeInt(-100, 100) / 100.0;
        double sigma = 0.1 + data.consumeInt(0, 2000) / 200.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double noiseScale = data.consumeInt(0, 1000) / 1000000.0;
        boolean reverseX = data.consumeBoolean();

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = reverseX ? (len - 1 - i) : i;
            double dx = x - mean;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
            if (noiseScale > 0.0) {
                y += (data.consumeInt(0, 1000) / 1000.0) * noiseScale;
            }
            if (y < 0.0) {
                y = 0.0;
            }
            ys[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = reverseX ? (ys.length - 1 - i) : i;
            fitter.addObservedPoint(x, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        runOverloadAgreement(ys);
    }

    private static void runOverloadAgreement(double[] ys) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            f1.addObservedPoint(i, ys[i]);
            f2.addObservedPoint(i, ys[i]);
        }

        double[] guess;
        double[] lhs;
        double[] rhs;

        try {
            List<WeightedObservedPoint> observations = f1.getObservations();
            guess = (new GaussianFitter.ParameterGuesser(observations)).guess();
            lhs = f1.fit();
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle: the no-arg fit implementation computes guess() and delegates to fit(guess).
           Therefore, for the same observations, fit() and fit(guess) must agree whenever both calls succeed.
           A patch that merely suppresses the exception path or changes delegation behavior would violate this. */
        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(guess) inputLen="
                    + ys.length + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
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
        return name.startsWith("org.apache.commons.math.exception.");
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

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,922 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        runOverloadAgreement(ANCHOR_DATA);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double mean = data.consumeInt(0, Math.max(0, len - 1)) + data.consumeInt(-100, 100) / 100.0;
        double sigma = 0.1 + data.consumeInt(0, 2000) / 200.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double noiseScale = data.consumeInt(0, 1000) / 1000000.0;
        boolean reverseX = data.consumeBoolean();

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = reverseX ? (len - 1 - i) : i;
            double dx = x - mean;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
            if (noiseScale > 0.0) {
                y += (data.consumeInt(0, 1000) / 1000.0) * noiseScale;
            }
            if (y < 0.0) {
                y = 0.0;
            }
            ys[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = reverseX ? (ys.length - 1 - i) : i;
            fitter.addObservedPoint(x, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        runOverloadAgreement(ys);
    }

    private static void runOverloadAgreement(double[] ys) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            f1.addObservedPoint(i, ys[i]);
            f2.addObservedPoint(i, ys[i]);
        }

        double[] guess;
        double[] lhs;
        double[] rhs;

        try {
            List<WeightedObservedPoint> observations = f1.getObservations();
            guess = (new GaussianFitter.ParameterGuesser(observations)).guess();
            lhs = f1.fit();
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle: the no-arg fit implementation computes guess() and delegates to fit(guess).
           Therefore, for the same observations, fit() and fit(guess) must agree whenever both calls succeed.
           A patch that merely suppresses the exception path or changes delegation behavior would violate this. */
        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(guess) inputLen="
                    + ys.length + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
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
        return name.startsWith("org.apache.commons.math.exception.");
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

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/Math_58_buggy/fuzz/attempt_001/FuzzHarness.java:122: error: incompatible types: WeightedObservedPoint[] cannot be converted to List<WeightedObservedPoint>
            List<WeightedObservedPoint> observations = f1.getObservations();
                                                                         ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/Math_58_buggy/fuzz/attempt_001/FuzzHarness.java:123: error: incompatible types: List<WeightedObservedPoint> cannot be converted to WeightedObservedPoint[]
            guess = (new GaussianFitter.ParameterGuesser(observations)).guess();
                                                         ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
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
<details open><summary>▸ Output (~6,974 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        runOverloadAgreement(ANCHOR_DATA);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double amplitude = 0.1 + (data.consumeInt(0, 10000) / 100.0);
        double mean = data.consumeInt(0, len - 1) + (data.consumeInt(-100, 100) / 100.0);
        double sigma = 0.1 + (data.consumeInt(0, 2000) / 200.0);
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double noiseScale = data.consumeInt(0, 1000) / 1000000.0;
        boolean reverseX = data.consumeBoolean();

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = reverseX ? (len - 1 - i) : i;
            double dx = x - mean;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
            if (noiseScale > 0.0) {
                y += (data.consumeInt(0, 1000) / 1000.0) * noiseScale;
            }
            if (y < 0.0) {
                y = 0.0;
            }
            ys[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = reverseX ? (ys.length - 1 - i) : i;
            fitter.addObservedPoint(x, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        runOverloadAgreement(ys);
    }

    private static void runOverloadAgreement(double[] ys) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            f1.addObservedPoint(i, ys[i]);
            f2.addObservedPoint(i, ys[i]);
        }

        double[] guess;
        double[] lhs;
        double[] rhs;

        try {
            WeightedObservedPoint[] observations = f1.getObservations();
            guess = (new GaussianFitter.ParameterGuesser(observations)).guess();
            lhs = f1.fit();
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /* GaussianFitter.fit() computes a ParameterGuesser guess from the current observations
           and delegates to the overload taking an initial guess. Therefore, when both succeed
           on the same observations, fit() and fit(guess) must produce the same parameter array. */
        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(guess) inputLen="
                    + ys.length + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
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
        return name.startsWith("org.apache.commons.math.exception.");
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

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,253 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int n = data.consumeInt(5, 40);
        double amplitude = positiveScaled(data.consumeInt(), 1.0e-6, 1.0e3);
        double sigma = positiveScaled(data.consumeInt(), 0.2, 20.0);
        double mean = n + positiveScaled(data.consumeInt(), 0.5, 60.0);
        double baseline = positiveScaled(data.consumeInt(), 0.0, 1.0);
        double noiseScale = positiveScaled(data.consumeInt(), 0.0, 0.15);

        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double x = i;
            double z = (x - mean) / sigma;
            double gaussian = amplitude * Math.exp(-0.5 * z * z);
            double signedNoise = (data.consumeInt(-1000, 1000) / 1000.0) * noiseScale * Math.max(gaussian, 1.0e-12);
            double v = baseline + gaussian + signedNoise;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                v = Math.max(1.0e-300, baseline + gaussian);
            }
            y[i] = v;
        }

        runFitOnce(y, true);
        checkFitOverloadAgreement(y);
    }

    private static void runAnchor() {
        runFitOnce(ANCHOR_DATA, true);
        checkFitOverloadAgreement(ANCHOR_DATA);
    }

    private static double[] runFitOnce(double[] y, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }
        try {
            return fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        } catch (Error t) {
            return null;
        }
    }

    private static void checkFitOverloadAgreement(double[] y) {
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitterA.addObservedPoint(i, y[i]);
            fitterB.addObservedPoint(i, y[i]);
        }

        try {
            double[] lhs = fitterA.fit();
            double[] guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
            double[] rhs = fitterB.fit(guess);

            /* Oracle: GaussianFitter.fit() is defined in the patched source as
             * computing ParameterGuesser(getObservations()).guess() and delegating
             * to the same-name overload fit(double[]). Therefore, for the same
             * observations on fresh fitters, fit() and fit(guessedInitialGuess)
             * must agree. A patch that merely suppresses the exception path or
             * routes fit() differently can violate this without throwing.
             */
            if (!sameResult(lhs, rhs)) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(ParameterGuesser(...).guess()) inputLength="
                        + y.length + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String name = p.getName();
            if ("org.apache.commons.math.exception".equals(name)
                    || name.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameResult(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) || Double.isNaN(y)) {
                if (!(Double.isNaN(x) && Double.isNaN(y))) {
                    return false;
                }
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1.0e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static double positiveScaled(int raw, double min, double max) {
        long u = raw & 0xffffffffL;
        double f = u / (double) 0xffffffffL;
        return min + (max - min) * f;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,303 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runFitAndOracle(ANCHOR_DATA, 0.0);

        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 0.1 + data.consumeInt(0, 10000) / 200.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 1000.0;
        double center = data.consumeInt(0, len - 1) + (data.consumeInt(-500, 500) / 1000.0);
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000);

        for (int i = 0; i < len; i++) {
            double x = i;
            double z = (x - center) / sigma;
            double y = baseline + amplitude * Math.exp(-0.5 * z * z);

            int noiseMode = data.consumeInt(0, 3);
            if (noiseMode == 1) {
                y *= 1.0 + (data.consumeInt(-200, 200) / 10000.0);
            } else if (noiseMode == 2) {
                y += data.consumeInt(0, 1000) / 100000000.0;
            } else if (noiseMode == 3) {
                y *= 1.0 + (i % 2 == 0 ? 0.01 : -0.01);
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12;
            }
            ys[i] = y;
        }

        runFitAndOracle(ys, xOffset);
    }

    private static void runFitAndOracle(double[] ys, double xOffset) {
        GaussianFitter fitNoArg = buildFitter(ys, xOffset);
        try {
            fitNoArg.fit();
        } catch (Throwable t) {
            if (isBugRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract/oracle: GaussianFitter.fit() computes a parameter guess from the observations
           and then delegates to the overload taking that initial guess. Therefore, for the same
           observations, fit() and fit(guess) must agree. A patch that merely suppresses the throw
           or changes behavior on that path can violate this even when no exception is raised. */
        GaussianFitter fitterA = buildFitter(ys, xOffset);
        GaussianFitter fitterB = buildFitter(ys, xOffset);
        try {
            double[] lhs = fitterA.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            double[] rhs = fitterB.fit(guess);
            assertApproxEqual(lhs, rhs, ys, xOffset);
        } catch (Throwable t) {
            if (isBugRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static GaussianFitter buildFitter(double[] ys, double xOffset) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12;
            }
            fitter.addObservedPoint(xOffset + i, y);
        }
        return fitter;
    }

    private static boolean isBugRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String name = p.getName();
            if (name.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
    }

    private static void assertApproxEqual(double[] lhs, double[] rhs, double[] ys, double xOffset) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess) length mismatch inputLen="
                    + ys.length + " xOffset=" + xOffset + " lhs=" + arr(lhs) + " rhs=" + arr(rhs));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess) mismatch at index "
                            + i + " inputLen=" + ys.length + " xOffset=" + xOffset + " lhs=" + arr(lhs) + " rhs=" + arr(rhs));
                }
                continue;
            }
            double tol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess) mismatch at index "
                        + i + " inputLen=" + ys.length + " xOffset=" + xOffset + " lhs=" + arr(lhs) + " rhs=" + arr(rhs));
            }
        }
    }

    private static String arr(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,608 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 40);
        double amplitude = positiveFromInt(data.consumeInt(1, 1_000_000)) / 10.0;
        double sigma = 0.5 + (positiveFromInt(data.consumeInt(1, 1_000_000)) % 1000) / 100.0;
        double offset = data.consumeInt(-1000, 1000);
        double centerBeyondRight = offset + (len - 1) + 1.0 + (positiveFromInt(data.consumeInt(1, 1_000_000)) % 3000) / 100.0;
        double baseline = (positiveFromInt(data.consumeInt(0, 100_000)) % 1000) / 1.0e12;

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < len; i++) {
            double x = offset + i;
            double delta = x - centerBeyondRight;
            double y = amplitude * Math.exp(-(delta * delta) / (2.0 * sigma * sigma)) + baseline;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            fitterA.addObservedPoint(x, y);
            fitterB.addObservedPoint(x, y);
        }

        double[] lhs;
        try {
            lhs = fitterA.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitterB.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        // Contract/oracle: the no-arg fit() is defined by computing ParameterGuesser(...).guess()
        // and then delegating to the sibling overload fit(double[] initialGuess). Therefore, whenever
        // both calls return normally on the same observations, they must agree. A patch that merely
        // suppresses the crashing path or changes behavior instead of delegating correctly will break this.
        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(ParameterGuesser.guess()) "
                    + "input=len=" + len
                    + ",offset=" + offset
                    + ",center=" + centerBeyondRight
                    + ",sigma=" + sigma
                    + ",amplitude=" + amplitude
                    + ",lhs=" + arrayToString(lhs)
                    + ",rhs=" + arrayToString(rhs));
        }
    }

    private static void runAnchor() {
        final double[] anchorData = {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterA.addObservedPoint(i, anchorData[i]);
            fitterB.addObservedPoint(i, anchorData[i]);
        }

        double[] lhs;
        try {
            lhs = fitterA.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitterB.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(ParameterGuesser.guess()) "
                    + "input=anchor"
                    + ",lhs=" + arrayToString(lhs)
                    + ",rhs=" + arrayToString(rhs));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String n = p.getName();
            if (n != null && n.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
    }

    private static double positiveFromInt(int v) {
        return v < 0 ? -(double) v : (double) v;
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1.0e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,688 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int len = data.consumeInt(3, ANCHOR_DATA.length - start);
        double scale = positiveScale(data.consumeInt(-6, 6));
        double xOffset = data.consumeInt(-100, 100);
        double xStep = data.consumeInt(1, 5);
        double shift = data.consumeInt(-50, 50);

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double v = ANCHOR_DATA[start + i] * scale;
            if (data.consumeBoolean()) {
                v *= 1.0 + (0.01 * data.consumeInt(0, 10));
            }
            ys[i] = Math.max(v, Double.MIN_NORMAL);
        }

        runExplorationCase(ys, xOffset, xStep, shift, "start=" + start + ",len=" + len + ",scale=" + scale
                + ",xOffset=" + xOffset + ",xStep=" + xStep + ",shift=" + shift);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }

        try {
            double[] p = fitter.fit();
            if (p == null || p.length < 3 || !isFinite(p[1])) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must produce a finite parameter vector input=anchor lhs=" + formatParams(p) + " rhs=finite-length>=3");
            }
            if (Math.abs(p[1] - 53.1572792) > 1.0e-5) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must match the documented fitted mean input=anchor lhs=" + p[1] + " rhs=53.1572792");
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runExplorationCase(double[] ys, double xOffset, double xStep, double shift, String desc) {
        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter shifted = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < ys.length; i++) {
            double x = xOffset + (i * xStep);
            base.addObservedPoint(x, ys[i]);
            shifted.addObservedPoint(x + shift, ys[i]);
        }

        double[] p1;
        try {
            p1 = base.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double[] p2;
        try {
            p2 = shifted.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (p1 == null || p2 == null || p1.length < 3 || p2.length < 3) {
            return;
        }
        if (!allFinite(p1) || !allFinite(p2)) {
            return;
        }

        /*
         * Contract/oracle:
         * GaussianFitter.fit() fits a Gaussian to the observed (x, y) points.
         * Shifting every observed x-value by a constant preserves the shape and
         * amplitudes of the same data, so a correct fit must return the same norm
         * and sigma, with only the mean shifted by that constant. A "fix" that
         * merely suppresses the failing path or silently returns wrong parameters
         * breaks this observable relation even when no exception is thrown.
         */
        double meanDelta = p2[1] - p1[1];
        if (Math.abs(meanDelta - shift) > toleranceFor(p1[1], p2[1], shift, 1.0e-3)) {
            throw new RuntimeException("[oracle:shift] metamorphic violation: shifted x-values should shift fitted mean input=" + desc + " lhs=" + meanDelta + " rhs=" + shift);
        }
        if (Math.abs(p2[0] - p1[0]) > toleranceFor(p1[0], p2[0], 0.0, 1.0e-3)) {
            throw new RuntimeException("[oracle:shift] metamorphic violation: shifted x-values should preserve fitted norm input=" + desc + " lhs=" + p2[0] + " rhs=" + p1[0]);
        }
        if (Math.abs(p2[2] - p1[2]) > toleranceFor(p1[2], p2[2], 0.0, 1.0e-3)) {
            throw new RuntimeException("[oracle:shift] metamorphic violation: shifted x-values should preserve fitted sigma input=" + desc + " lhs=" + p2[2] + " rhs=" + p1[2]);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double positiveScale(int exp) {
        double s = 1.0;
        if (exp > 0) {
            for (int i = 0; i < exp; i++) {
                s *= 10.0;
            }
        } else if (exp < 0) {
            for (int i = 0; i < -exp; i++) {
                s /= 10.0;
            }
        }
        return s;
    }

    private static boolean allFinite(double[] p) {
        for (int i = 0; i < p.length; i++) {
            if (!isFinite(p[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static double toleranceFor(double a, double b, double c, double floor) {
        double scale = Math.max(Math.max(Math.abs(a), Math.abs(b)), Math.abs(c));
        return Math.max(floor, scale * 1.0e-6);
    }

    private static String formatParams(double[] p) {
        if (p == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < p.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(p[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,265 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        try {
            GaussianFitter anchorCrash = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                anchorCrash.addObservedPoint(i, anchor[i]);
            }
            anchorCrash.fit();
        } catch (RuntimeException t) {
            boolean rootType = t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            if (rootType && throughFit) {
                throw t;
            }
        }

        try {
            GaussianFitter anchorA = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            GaussianFitter anchorB = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                anchorA.addObservedPoint(i, anchor[i]);
                anchorB.addObservedPoint(i, anchor[i]);
            }
            double[] guess = (new GaussianFitter.ParameterGuesser(anchorB.getObservations())).guess();
            double[] viaNoArg = anchorA.fit();
            double[] viaGuess = anchorB.fit(guess);

            /* Contract/oracle: the no-arg fit() computes a guess from the observations and then delegates
               to the overload taking the initial guess. Therefore fit() and fit(guess()) must agree on the
               same observations. A patch that only suppresses the exception or skips the intended overload
               breaks this sibling-agreement relation without necessarily throwing. */
            if (viaNoArg != null && viaGuess != null) {
                if (viaNoArg.length != viaGuess.length) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess()) length mismatch input=anchor lhs="
                            + viaNoArg.length + " rhs=" + viaGuess.length);
                }
                for (int i = 0; i < viaNoArg.length; i++) {
                    double a = viaNoArg[i];
                    double b = viaGuess[i];
                    double diff = Math.abs(a - b);
                    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (!(diff <= 1.0e-8 * scale || (Double.isNaN(a) && Double.isNaN(b)))) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() != fit(guess()) input=anchor index="
                                + i + " lhs=" + a + " rhs=" + b);
                    }
                }
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            boolean rootType = t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            if (rootType && throughFit) {
                throw t;
            }
            return;
        }

        int n = data.consumeInt(5, 40);
        int centerIndex = data.consumeInt(0, n - 1);
        double mean = centerIndex + (data.consumeInt(-1000, 1000) / 1000.0);
        double sigma = data.consumeInt(1, 5000) / 1000.0;
        double normExp = data.consumeInt(-300, -5);
        double norm = Math.pow(10.0, normExp);
        double noiseFraction = data.consumeInt(0, 100) / 10000.0;
        boolean reverse = data.consumeBoolean();

        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            int xIndex = reverse ? (n - 1 - i) : i;
            double dx = xIndex - mean;
            double base = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noise = base * noiseFraction * ((i % 2 == 0) ? 1.0 : 0.5);
            double y = base + noise;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            ys[i] = y;
        }

        try {
            GaussianFitter fuzzA = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            GaussianFitter fuzzB = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                fuzzA.addObservedPoint(i, ys[i]);
                fuzzB.addObservedPoint(i, ys[i]);
            }

            double[] guess = (new GaussianFitter.ParameterGuesser(fuzzB.getObservations())).guess();
            for (int i = 0; i < guess.length; i++) {
                double g = guess[i];
                if (Double.isNaN(g) || Double.isInfinite(g)) {
                    return;
                }
            }

            double[] viaNoArg = fuzzA.fit();
            double[] viaGuess = fuzzB.fit(guess);

            /* Contract/oracle: fit() is documented/implemented as using a first guess and then invoking
               the overload that accepts the initial guess. For the exact same observations and derived guess,
               both overloads must produce the same fitted parameters. */
            if (viaNoArg == null || viaGuess == null) {
                return;
            }
            if (viaNoArg.length != viaGuess.length) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess()) length mismatch input=n="
                        + n + ",mean=" + mean + ",sigma=" + sigma + ",norm=" + norm + " lhs="
                        + viaNoArg.length + " rhs=" + viaGuess.length);
            }
            for (int i = 0; i < viaNoArg.length; i++) {
                double a = viaNoArg[i];
                double b = viaGuess[i];
                double diff = Math.abs(a - b);
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (!(diff <= 1.0e-7 * scale || (Double.isNaN(a) && Double.isNaN(b)))) {
                    throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() != fit(guess()) input=n="
                            + n + ",mean=" + mean + ",sigma=" + sigma + ",norm=" + norm
                            + " index=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }

            boolean rootType = t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }

            if (rootType && throughFit) {
                throw t;
            }

            return;
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,365 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchorY = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };
        double[] anchorX = new double[anchorY.length];
        for (int i = 0; i < anchorY.length; i++) {
            anchorX[i] = i;
        }
        runScenario(anchorX, anchorY, true, true);

        int n = data.consumeInt(3, 40);
        double[] xs = new double[n];
        double[] ys = new double[n];

        int xStart = data.consumeInt(-50, 50);
        int step = data.consumeInt(1, 4);
        boolean peakRight = data.consumeBoolean();
        double sigma = 0.5 + data.consumeInt(1, 200) / 10.0;
        double norm = Math.pow(10.0, data.consumeInt(-12, 6));
        double margin = data.consumeInt(1, 80);
        double mean = peakRight ? (xStart + (n - 1) * step + margin) : (xStart - margin);

        for (int i = 0; i < n; i++) {
            double x = xStart + i * step;
            xs[i] = x;
            double z = (x - mean) / sigma;
            double base = norm * Math.exp(-0.5 * z * z);
            double noiseScale = 1.0 + (data.consumeInt(-20, 20) / 100.0);
            double y = base * noiseScale;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(base, 1e-300);
            }
            ys[i] = y;
        }

        runScenario(xs, ys, true, true);
    }

    private static void runScenario(double[] xs, double[] ys, boolean validByConstruction, boolean checkOracle) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }

        double[] fitNoArg;
        try {
            fitNoArg = fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            return;
        }

        if (!checkOracle) {
            return;
        }

        GaussianFitter guessFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter overloadFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            guessFitter.addObservedPoint(xs[i], ys[i]);
            overloadFitter.addObservedPoint(xs[i], ys[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(guessFitter.getObservations())).guess();
        } catch (Throwable t) {
            handleThrowable(t, false);
            return;
        }

        final double[] fitWithGuess;
        try {
            fitWithGuess = overloadFitter.fit(guess);
        } catch (Throwable t) {
            handleThrowable(t, false);
            return;
        }

        /* Contract asserted: fit() and fit(double[] initialGuess) are same-name overloads documented to agree
           on equivalent inputs. Here fit() computes an initial guess from the observations, so comparing it
           against fit(guessedInitialGuess) detects a patch that merely deletes/avoids the failing path but
           returns a different result instead of preserving the overload's behavior. */
        if (!sameVector(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(initialGuess) " +
                "xLen=" + xs.length +
                " lhs=" + vectorToString(fitNoArg) +
                " rhs=" + vectorToString(fitWithGuess)
            );
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && isRootCause((RuntimeException) t) && validByConstruction) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
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
        Package p = t.getClass().getPackage();
        if (p != null) {
            String name = p.getName();
            if (name != null && name.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameVector(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String vectorToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,462 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            final double[] anchor = new double[] {
                1.1143831578403364E-29,
                4.95281403484594E-28,
                1.1171347211930288E-26,
                1.7044813962636277E-25,
                1.9784716574832164E-24,
                1.8630236407866774E-23,
                1.4820532905097742E-22,
                1.0241963854632831E-21,
                6.275077366673128E-21,
                3.461808994532493E-20,
                1.7407124684715706E-19,
                8.056687953553974E-19,
                3.460193945992071E-18,
                1.3883326374011525E-17,
                5.233894983671116E-17,
                1.8630791465263745E-16,
                6.288759227922111E-16,
                2.0204433920597856E-15,
                6.198768938576155E-15,
                1.821419346860626E-14,
                5.139176445538471E-14,
                1.3956427429045787E-13,
                3.655705706448139E-13,
                9.253753324779779E-13,
                2.267636001476696E-12,
                5.3880460095836855E-12,
                1.2431632654852931E-11
            };

            GaussianFitter anchorFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                anchorFitter.addObservedPoint(i, anchor[i]);
            }

            try {
                anchorFitter.fit();
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                            && "fit".equals(e.getMethodName())) {
                        throughPatchedFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughPatchedFit) {
                    throw (RuntimeException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof RuntimeException) {
                    return;
                }
                return;
            }
        }

        int cases = 1;
        if (data.remainingBytes() > 16) {
            cases += data.consumeInt(0, 2);
        }

        for (int c = 0; c < cases; c++) {
            int len = data.consumeInt(3, 40);
            double amplitude = Math.pow(10.0, data.consumeInt(-20, 3));
            if (!(amplitude > 0.0) || Double.isInfinite(amplitude) || Double.isNaN(amplitude)) {
                amplitude = 1.0;
            }

            double sigma = data.consumeInt(1, 200) / 10.0;
            double center = len - 1 + data.consumeInt(1, len + 20);

            double[] ys = new double[len];
            for (int i = 0; i < len; i++) {
                double dx = i - center;
                double base = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
                double tweak = 1.0 + (data.consumeInt(-30, 30) / 100.0);
                if (tweak <= 0.0) {
                    tweak = 0.1;
                }
                double y = base * tweak;
                if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = Math.max(base, Double.MIN_NORMAL);
                }
                ys[i] = y;
            }

            GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitterA.addObservedPoint(i, ys[i]);
                fitterB.addObservedPoint(i, ys[i]);
            }

            double[] lhs;
            try {
                lhs = fitterA.fit();
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                            && "fit".equals(e.getMethodName())) {
                        throughPatchedFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughPatchedFit) {
                    throw (RuntimeException) t;
                }
                continue;
            }

            double[] guess;
            try {
                guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            } catch (Throwable t) {
                continue;
            }

            double[] rhs;
            try {
                rhs = fitterB.fit(guess);
            } catch (Throwable t) {
                continue;
            }

            /* Contract/oracle: GaussianFitter.fit() is documented as the no-arg sibling of fit(double[]),
               and the patched body itself computes ParameterGuesser(...).guess() then delegates to fit(guess).
               Therefore, for the same observations, fit() must produce the same result as fit(guess) built from
               those observations. A throw-deleting or branch-skipping patch that changes the delegation path
               can violate this observable equivalence without crashing. */
            if (lhs != null && rhs != null && lhs.length == rhs.length) {
                for (int i = 0; i < lhs.length; i++) {
                    long a = Double.doubleToLongBits(lhs[i]);
                    long b = Double.doubleToLongBits(rhs[i]);
                    if (a != b) {
                        throw new RuntimeException(
                            "[oracle:fit-overload] metamorphic violation: fit() != fit(guess) input=len=" + len
                                + ", amp=" + amplitude + ", sigma=" + sigma + ", center=" + center
                                + " lhs[" + i + "]=" + lhs[i] + " rhs[" + i + "]=" + rhs[i]);
                    }
                }
            }
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,167 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        exerciseAndCheck(fitter, true);
    }

    private static void runExplore(FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int end = data.consumeInt(start + 2, ANCHOR_DATA.length - 1);
        int prefix = data.consumeInt(0, 3);
        int suffix = data.consumeInt(0, 3);
        double x = data.consumeInt(-100, 100);
        double step = data.consumeInt(1, 5);
        double yScale = data.consumeInt(1, 1000) / 100.0;
        double minPositive = Math.pow(10.0, -data.consumeInt(10, 250));

        for (int i = 0; i < prefix; i++) {
            double y = Math.max(minPositive, ANCHOR_DATA[start] * (0.1 + (i + 1) * 0.05) * yScale);
            fitter.addObservedPoint(x, y);
            x += step;
        }

        for (int i = start; i <= end; i++) {
            int noisePct = data.consumeInt(-10, 10);
            double factor = 1.0 + (noisePct / 100.0);
            double y = ANCHOR_DATA[i] * yScale * factor;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = minPositive;
            }
            fitter.addObservedPoint(x, Math.max(minPositive, y));
            x += step;
        }

        for (int i = 0; i < suffix; i++) {
            double y = Math.max(minPositive, ANCHOR_DATA[end] * (0.5 + (i + 1) * 0.1) * yScale);
            fitter.addObservedPoint(x, y);
            x += step;
        }

        exerciseAndCheck(fitter, true);
    }

    private static void exerciseAndCheck(GaussianFitter fitter, boolean validByConstruction) {
        try {
            fitter.fit();
        } catch (RuntimeException e) {
            if (validByConstruction && isRootCause(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        // Contract/metamorphic check:
        // fit() is documented/implemented as using ParameterGuesser on the current observations
        // and then delegating to the sibling overload fit(double[] initialGuess). Therefore, for
        // the same observations, fit() and fit(guess()) must agree whenever both calls succeed.
        try {
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] viaOverload = fitter.fit(guess);

            GaussianFitter fitter2 = cloneFitter(fitter);
            double[] viaNoArg = fitter2.fit();

            if (!sameResult(viaOverload, viaNoArg)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess())"
                        + " guess=" + arrayToString(guess)
                        + " lhs=" + arrayToString(viaOverload)
                        + " rhs=" + arrayToString(viaNoArg));
            }
        } catch (RuntimeException e) {
            if (validByConstruction && isRootCause(e)) {
                throw e;
            }
            return;
        }
    }

    private static GaussianFitter cloneFitter(GaussianFitter original) {
        GaussianFitter copy = new GaussianFitter(new LevenbergMarquardtOptimizer());
        WeightedObservedPoint[] obs = original.getObservations();
        for (int i = 0; i < obs.length; i++) {
            WeightedObservedPoint p = obs[i];
            copy.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }
        return copy;
    }

    private static boolean isRootCause(RuntimeException e) {
        if (!(e instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(f.getClassName())
                    && "fit".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        if (e instanceof IllegalArgumentException || e instanceof NumberFormatException) {
            return true;
        }
        String name = e.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean sameResult(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,324 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.exception.NotStrictlyPositiveException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint((double) i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                if (inFit && t instanceof NotStrictlyPositiveException) {
                    throw t;
                }
            }
        }

        {
            GaussianFitter left = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter right = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                left.addObservedPoint((double) i, anchor[i]);
                right.addObservedPoint((double) i, anchor[i]);
            }
            try {
                double[] lhs = left.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(right.getObservations())).guess();
                double[] rhs = right.fit(guess);
                if (lhs != null && rhs != null && lhs.length == rhs.length && lhs.length > 0) {
                    for (int i = 0; i < lhs.length; i++) {
                        double a = lhs[i];
                        double b = rhs[i];
                        if (Double.isNaN(a) != Double.isNaN(b)) {
                            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree on NaN state input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                        }
                        if (!Double.isNaN(a) && !Double.isNaN(b)) {
                            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                            if (Math.abs(a - b) > 1.0e-6 * scale) {
                                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                            }
                        }
                    }
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
            } catch (Error t) {
            }
        }

        int len = data.consumeInt(8, 40);
        int xStart = data.consumeInt(-50, 50);
        int xStep = data.consumeInt(1, 4);

        double amplitudeBase = (double) data.consumeInt(1, 1000000);
        double amplitudeScale = Math.pow(10.0, -data.consumeInt(0, 12));
        double amplitude = amplitudeBase * amplitudeScale;
        if (!(amplitude > 0.0) || Double.isInfinite(amplitude) || Double.isNaN(amplitude)) {
            amplitude = 1.0;
        }

        double sigma = ((double) data.consumeInt(1, 5000)) / 100.0;
        if (!(sigma > 0.0) || Double.isInfinite(sigma) || Double.isNaN(sigma)) {
            sigma = 1.0;
        }

        int peakBeyondLast = data.consumeInt(1, 60);
        double center = xStart + (len - 1) * xStep + peakBeyondLast;

        double[] ys = new double[len];
        double[] xs = new double[len];
        for (int i = 0; i < len; i++) {
            double x = xStart + i * xStep;
            xs[i] = x;
            double dx = x - center;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent);
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Double.MIN_VALUE;
            }
            ys[i] = y;
        }

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                if (inFit && t instanceof NotStrictlyPositiveException) {
                    throw t;
                }
            }
        }

        {
            GaussianFitter left = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter right = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                left.addObservedPoint(xs[i], ys[i]);
                right.addObservedPoint(xs[i], ys[i]);
            }

            try {
                /*
                 * Contract/oracle: the documented same-name overloads fit() / fit(double[] initialGuess)
                 * must agree when fit() itself computes that same initial guess. The patched code changes
                 * fit() exactly to delegate to fit(guess), so deleting/short-circuiting that behavior would
                 * make these two real-library calls disagree observably.
                 */
                double[] lhs = left.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(right.getObservations())).guess();
                double[] rhs = right.fit(guess);

                if (lhs == null || rhs == null || lhs.length != rhs.length || lhs.length == 0) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) shape mismatch inputLen=" + len + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                }

                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    if (Double.isNaN(a) != Double.isNaN(b)) {
                        throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree on NaN state inputLen=" + len + " xs=" + java.util.Arrays.toString(xs) + " ys=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                    if (Double.isInfinite(a) != Double.isInfinite(b)) {
                        throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree on infinity state inputLen=" + len + " xs=" + java.util.Arrays.toString(xs) + " ys=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                    if (!Double.isNaN(a) && !Double.isNaN(b) && !Double.isInfinite(a) && !Double.isInfinite(b)) {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        if (Math.abs(a - b) > 1.0e-6 * scale) {
                            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree inputLen=" + len + " xs=" + java.util.Arrays.toString(xs) + " ys=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                        }
                    }
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                boolean inFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                if (inFit && t instanceof NotStrictlyPositiveException) {
                    throw t;
                }
            } catch (Error t) {
            }
        }
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,367 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            double[] ys = buildTailLikeData(data);
            if (ys != null) {
                runScenario(ys, 0.0, 1.0, true);
            }
        } else if (mode == 1) {
            double[] ys = buildBellData(data);
            if (ys != null) {
                runScenario(ys, 0.0, 1.0, true);
            }
        } else {
            double[] ys = buildPositiveArbitraryData(data);
            if (ys != null) {
                runScenario(ys, data.consumeInt(-50, 50), data.consumeInt(1, 5), true);
            }
        }
    }

    private static void runAnchor() {
        double[] data = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };
        runScenario(data, 0.0, 1.0, true);
    }

    private static void runScenario(double[] ys, double xStart, double xStep, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xStart + i * xStep;
            fitter.addObservedPoint(x, ys[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isGroundTruthRootCauseFromFit(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (Error e) {
            return;
        }

        tryMetamorphicSiblingAgreement(ys, xStart, xStep);
    }

    private static void tryMetamorphicSiblingAgreement(double[] ys, double xStart, double xStep) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xStart + i * xStep;
            f1.addObservedPoint(x, ys[i]);
            f2.addObservedPoint(x, ys[i]);
        }

        double[] guess;
        double[] lhs;
        double[] rhs;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f1.getObservations())).guess();
            lhs = f1.fit();
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() / fit(initialGuess) shape mismatch");
        }

        /* Contract/oracle: the implementation of fit() is documented/shown to compute
         * ParameterGuesser(getObservations()).guess() and delegate to the overload with that
         * initial guess. Therefore, on the same observations, fit() and fit(guess()) must agree.
         * A patch that only suppresses the throw or skips the real overload would break this.
         */
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: infinite mismatch index=" + i + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            double diff = Math.abs(a - b);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (diff > 1.0e-7 * scale) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(guess()) index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static boolean isGroundTruthRootCauseFromFit(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
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
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static double[] buildTailLikeData(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double amplitude = positiveRange(data.consumeInt(1, 1000)) * 1.0e-6;
        double mean = data.consumeInt(n + 5, n + 80);
        double sigma = positiveRange(data.consumeInt(1, 50));
        double xStart = data.consumeInt(-20, 20);
        double step = data.consumeInt(1, 3);

        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double x = xStart + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z);
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                return null;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double[] buildBellData(FuzzedDataProvider data) {
        int n = data.consumeInt(7, 41);
        double amplitude = positiveRange(data.consumeInt(1, 1000)) * 1.0e-3;
        double mean = data.consumeInt(2, n - 3);
        double sigma = positiveRange(data.consumeInt(1, 10));
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double z = (i - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z);
            if (data.consumeBoolean()) {
                y *= 1.0 + (data.consumeInt(-5, 5) * 1.0e-3);
            }
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                return null;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double[] buildPositiveArbitraryData(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 30);
        double[] ys = new double[n];
        boolean anyPositive = false;
        for (int i = 0; i < n; i++) {
            int mag = data.consumeInt(1, 1000000);
            int exp = data.consumeInt(-12, 3);
            double y = mag * Math.pow(10.0, exp);
            if (data.consumeBoolean()) {
                y *= 1.0e-6;
            }
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                return null;
            }
            ys[i] = y;
            anyPositive = true;
        }
        return anyPositive ? ys : null;
    }

    private static double positiveRange(int v) {
        return Math.max(1.0, Math.min(1000.0, Math.abs((double) v)));
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,860 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(ANCHOR_DATA, 0.0, 1.0, true, true);

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int len = data.consumeInt(3, ANCHOR_DATA.length - start);
        double[] variant = new double[len];

        double scalePow = data.consumeInt(-6, 6);
        double scale = Math.pow(10.0, scalePow);
        double tilt = 1.0 + (data.consumeInt(-5, 5) * 0.005);
        if (!(tilt > 0.0) || Double.isNaN(tilt) || Double.isInfinite(tilt)) {
            tilt = 1.0;
        }

        double previous = 0.0;
        for (int i = 0; i < len; i++) {
            double base = ANCHOR_DATA[start + i] * scale;
            double adjusted = base * Math.pow(tilt, i);
            if (!(adjusted > 0.0) || Double.isNaN(adjusted) || Double.isInfinite(adjusted)) {
                adjusted = Math.abs(base);
                if (!(adjusted > 0.0) || Double.isNaN(adjusted) || Double.isInfinite(adjusted)) {
                    adjusted = Math.pow(10.0, -12 + i);
                }
            }
            if (i > 0 && adjusted <= previous) {
                adjusted = Math.nextUp(previous);
            }
            variant[i] = adjusted;
            previous = adjusted;
        }

        double xOffset = data.consumeInt(-1000, 1000);
        double xStep = data.consumeInt(1, 10);
        runScenario(variant, xOffset, xStep, true, true);

        int synthLen = data.consumeInt(3, 40);
        double[] synthetic = new double[synthLen];
        double mean = synthLen + data.consumeInt(1, 40);
        double sigma = data.consumeInt(1, 50) / 10.0;
        double norm = Math.pow(10.0, data.consumeInt(-8, 4));
        previous = 0.0;
        for (int i = 0; i < synthLen; i++) {
            double x = i;
            double exponent = -((x - mean) * (x - mean)) / (2.0 * sigma * sigma);
            double y = norm * Math.exp(exponent);
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.pow(10.0, -12 + Math.min(i, 12));
            }
            if (i > 0 && y <= previous) {
                y = Math.nextUp(previous);
            }
            synthetic[i] = y;
            previous = y;
        }
        runScenario(synthetic, data.consumeInt(-1000, 1000), data.consumeInt(1, 5), true, true);
    }

    private static void runScenario(double[] y, double xOffset, double xStep, boolean doOracle, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(xOffset + (i * xStep), y[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t) || t instanceof RuntimeException || t instanceof Error) {
                return;
            }
            return;
        }

        if (!doOracle) {
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() computes a guess from the observations and then fits using that guess.
         * Therefore fit() and fit(guessFromSameObservations) must agree on the same data.
         * A patch that merely suppresses/circumvents the failing path in fit() but changes behavior
         * will violate this sibling-overload agreement.
         */
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            double x = xOffset + (i * xStep);
            f1.addObservedPoint(x, y[i]);
            f2.addObservedPoint(x, y[i]);
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = f1.fit();
        } catch (Throwable t) {
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        try {
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) input="
                    + Arrays.toString(y)
                    + " xOffset=" + xOffset
                    + " xStep=" + xStep
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        return hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
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
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > tol) {
                return false;
            }
        }
        return true;
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,224 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase(ANCHOR_DATA, true);

        int len = data.consumeInt(3, 40);
        double[] synthetic = buildIncreasingTail(data, len);
        runCase(synthetic, true);

        if (data.consumeBoolean()) {
            double[] perturbed = synthetic.clone();
            int edits = data.consumeInt(1, Math.min(6, perturbed.length));
            for (int i = 0; i < edits; i++) {
                int idx = data.consumeInt(0, perturbed.length - 1);
                double factor = 1.0 + (data.consumeInt(-20, 20) / 1000.0);
                if (factor <= 0.0) {
                    factor = 0.001;
                }
                perturbed[idx] = positiveFinite(perturbed[idx] * factor);
            }
            enforceNonDecreasing(perturbed);
            runCase(perturbed, true);
        }
    }

    private static double[] buildIncreasingTail(FuzzedDataProvider data, int len) {
        double[] y = new double[len];
        double norm = 1e-12 + (data.consumeInt(1, 1_000_000) / 1_000_000.0);
        double sigma = 0.5 + (data.consumeInt(1, 400) / 20.0);
        double mean = len + data.consumeInt(1, len + 20);
        for (int i = 0; i < len; i++) {
            double d = (i - mean) / sigma;
            y[i] = positiveFinite(norm * Math.exp(-0.5 * d * d));
        }
        enforceNonDecreasing(y);
        return y;
    }

    private static void enforceNonDecreasing(double[] y) {
        double prev = 0.0;
        for (int i = 0; i < y.length; i++) {
            double v = positiveFinite(y[i]);
            if (v < prev) {
                v = prev + Math.max(prev * 1e-12, 1e-300);
            }
            y[i] = v;
            prev = v;
        }
    }

    private static double positiveFinite(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1e-300;
        }
        return v;
    }

    private static void runCase(double[] ys, boolean validByConstruction) {
        GaussianFitter fitNoGuess = newFitter(ys);
        try {
            fitNoGuess.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedRegion(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (Error t) {
            return;
        }

        GaussianFitter fitA = newFitter(ys);
        GaussianFitter fitB = newFitter(ys);
        try {
            double[] guessed = (new GaussianFitter.ParameterGuesser(fitB.getObservations())).guess();
            double[] a = fitA.fit();
            double[] b = fitB.fit(guessed);

            /* Contract/oracle: zero-arg fit() computes a ParameterGuesser guess from the observations
             * and delegates to the overload taking an initial guess. Therefore fit() and fit(guess())
             * must agree on the same observations and same guess. A patch that merely suppresses the
             * exception path or skips the intended delegation can violate this observable equivalence.
             */
            assertEquivalentResults(ys, guessed, a, b);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedRegion(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (Error t) {
            return;
        }
    }

    private static GaussianFitter newFitter(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        return fitter;
    }

    private static void assertEquivalentResults(double[] ys, double[] guess, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) returned incompatible result shapes inputLen="
                + ys.length + " guessLen=" + (guess == null ? -1 : guess.length)
                + " lhsLen=" + (a == null ? -1 : a.length) + " rhsLen=" + (b == null ? -1 : b.length));
        }
        for (int i = 0; i < a.length; i++) {
            double lhs = a[i];
            double rhs = b[i];
            boolean lhsNaN = Double.isNaN(lhs);
            boolean rhsNaN = Double.isNaN(rhs);
            if (lhsNaN || rhsNaN) {
                if (lhsNaN != rhsNaN) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: NaN mismatch inputLen="
                        + ys.length + " idx=" + i + " lhs=" + lhs + " rhs=" + rhs);
                }
                continue;
            }
            double diff = Math.abs(lhs - rhs);
            double scale = Math.max(1.0, Math.max(Math.abs(lhs), Math.abs(rhs)));
            if (diff > 1e-8 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(initialGuess) inputLen="
                    + ys.length + " idx=" + i + " lhs=" + lhs + " rhs=" + rhs + " diff=" + diff);
            }
        }
    }

    private static boolean isRootCauseFromPatchedRegion(Throwable t) {
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
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
        return name.startsWith("org.apache.commons.math.exception.");
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,673 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }

            double[] pNoArg = fitter.fit();

            /* Contract/oracle: the same-name overloads fit() and fit(double[] initialGuess)
             * are documented to perform the same fit, with fit() obtaining a guess internally.
             * A throw-deleting or branch-skipping patch in fit() would make the no-arg path
             * diverge observably from the overload taking the same internally-derived guess. */
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] pGuess = fitter.fit(guess);

            if (pNoArg != null && pGuess != null && pNoArg.length == pGuess.length && pNoArg.length > 0) {
                for (int i = 0; i < pNoArg.length; i++) {
                    double a = pNoArg[i];
                    double b = pGuess[i];
                    boolean equal;
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        equal = true;
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        equal = (a == b);
                    } else {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        equal = Math.abs(a - b) <= 1.0e-8 * scale;
                    }
                    if (!equal) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() must agree with fit(double[] guess) on the internally-derived guess input=anchor lhs="
                            + java.util.Arrays.toString(pNoArg) + " rhs=" + java.util.Arrays.toString(pGuess));
                    }
                }
            }
        } catch (RuntimeException t) {
            boolean throughFit = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
            if (throughFit && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw t;
            }
            if (!cleanRejection) {
                // Swallow unrelated runtime failures outside the patched scope.
            }
        }

        int mode = data.consumeInt(0, 3);
        int n;
        if (mode == 0) {
            n = anchor.length;
        } else {
            n = data.consumeInt(5, 40);
        }

        double[] ys = new double[n];
        if (mode == 0) {
            for (int i = 0; i < n; i++) {
                ys[i] = anchor[i];
            }
        } else if (mode == 1) {
            int offset = data.consumeInt(0, 10);
            int take = Math.min(n, Math.max(5, anchor.length - offset));
            for (int i = 0; i < n; i++) {
                double base = anchor[Math.min(anchor.length - 1, offset + (i % take))];
                double factor = 0.8 + (data.consumeInt(0, 40) / 100.0);
                ys[i] = Math.max(1.0e-300, base * factor);
            }
        } else {
            double amplitude = Math.max(1.0e-12, data.consumeInt(1, 1000) / 100.0);
            double mean = data.consumeInt(0, Math.max(4, n - 1));
            double sigma = 0.5 + (data.consumeInt(1, 800) / 100.0);
            boolean leftTailOnly = data.consumeBoolean();
            for (int i = 0; i < n; i++) {
                double x = i;
                double z = (x - mean) / sigma;
                double v = amplitude * Math.exp(-0.5 * z * z);
                if (leftTailOnly) {
                    if (x > mean) {
                        v *= 1.0e-6;
                    }
                }
                double tweak = 0.95 + (data.consumeInt(0, 10) / 100.0);
                ys[i] = Math.max(1.0e-300, v * tweak);
            }
            if (mode == 3) {
                for (int i = 1; i < n; i++) {
                    if (ys[i] < ys[i - 1]) {
                        ys[i] = ys[i - 1] + Math.max(1.0e-300, ys[i - 1] * 0.1);
                    }
                }
            }
        }

        double xStart = data.consumeInt(-20, 20);
        double xStep = data.consumeBoolean() ? 1.0 : (0.5 + data.consumeInt(0, 10));
        if (!(xStep > 0.0) || Double.isNaN(xStep) || Double.isInfinite(xStep)) {
            xStep = 1.0;
        }

        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                double x = xStart + i * xStep;
                double y = ys[i];
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    return;
                }
                fitter.addObservedPoint(x, y);
            }

            double[] pNoArg = fitter.fit();

            /* Contract/oracle: fit() computes an internal initial guess from the current
             * observations, so calling fit(double[] initialGuess) with that same guess must
             * agree with fit() on the same receiver state. This catches silent wrong-output
             * fixes that merely suppress the exception path in fit(). */
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] pGuess = fitter.fit(guess);

            if (pNoArg == null || pGuess == null || pNoArg.length != pGuess.length || pNoArg.length == 0) {
                return;
            }
            for (int i = 0; i < pNoArg.length; i++) {
                double a = pNoArg[i];
                double b = pGuess[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    if (a != b) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(double[] guess) infinity mismatch input="
                            + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(pNoArg)
                            + " rhs=" + java.util.Arrays.toString(pGuess));
                    }
                    continue;
                }
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > 1.0e-8 * scale) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() must agree with fit(double[] guess) input="
                        + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(pNoArg)
                        + " rhs=" + java.util.Arrays.toString(pGuess));
                }
            }
        } catch (RuntimeException t) {
            boolean throughFit = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
            if (throughFit && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (!cleanRejection) {
                // Swallow unrelated runtime failures outside the patched scope.
            }
        }
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,298 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(ANCHOR_DATA);

        int len = data.consumeInt(3, 40);
        double[] generated = buildPositiveGaussianLikeSeries(data, len);
        runScenario(generated);
    }

    private static void runScenario(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        try {
            double[] viaNoArg = fitter.fit();

            // Contract/oracle: fit() computes a parameter guess from the current observations and
            // then fits using that guess; therefore fit() and fit(initialGuess) must agree when
            // initialGuess is exactly ParameterGuesser(getObservations()).guess(). A throw-deleting
            // or branch-skipping patch can violate this observable sibling-overload agreement.
            List<WeightedObservedPoint> observations = fitter.getObservations();
            double[] guessed = (new GaussianFitter.ParameterGuesser(observations)).guess();

            GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter2.addObservedPoint(i, ys[i]);
            }

            double[] viaExplicitGuess = fitter2.fit(guessed);
            assertSameResult(ys, viaNoArg, viaExplicitGuess);
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static double[] buildPositiveGaussianLikeSeries(FuzzedDataProvider data, int len) {
        double[] ys = new double[len];

        double norm = 0.1 + (data.consumeInt(1, 5000) / 1000.0);
        double mean = data.consumeInt(0, len - 1);
        double sigma = 0.2 + (data.consumeInt(1, 2000) / 1000.0);
        double baseline = data.consumeBoolean() ? (data.consumeInt(0, 100) / 1_000_000.0) : 0.0;

        Gaussian gaussian = new Gaussian(norm, mean, sigma);
        for (int i = 0; i < len; i++) {
            double y = gaussian.value(i) + baseline;
            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 100) / 1_000_000_000.0;
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(1e-12, baseline + 1e-12);
            }
            ys[i] = y;
        }

        int extraBumps = data.consumeInt(0, 2);
        for (int b = 0; b < extraBumps; b++) {
            int idx = data.consumeInt(0, len - 1);
            ys[idx] += data.consumeInt(0, 100) / 1_000_000.0;
        }

        for (int i = 0; i < len; i++) {
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1e-12;
            }
        }

        return ys;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement ste : stack) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertSameResult(double[] input, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) length mismatch inputLen="
                    + input.length + " lhsLen=" + (a == null ? -1 : a.length) + " rhsLen=" + (b == null ? -1 : b.length));
        }

        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                            + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-7 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                        + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
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
<details><summary>▸ Prompt (4 message(s), ~8,206 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(ANCHOR_DATA);

        int len = data.consumeInt(3, 40);
        double[] generated = buildPositiveGaussianLikeSeries(data, len);
        runScenario(generated);
    }

    private static void runScenario(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        try {
            double[] viaNoArg = fitter.fit();

            // Contract/oracle: fit() computes a parameter guess from the current observations and
            // then fits using that guess; therefore fit() and fit(initialGuess) must agree when
            // initialGuess is exactly ParameterGuesser(getObservations()).guess(). A throw-deleting
            // or branch-skipping patch can violate this observable sibling-overload agreement.
            List<WeightedObservedPoint> observations = fitter.getObservations();
            double[] guessed = (new GaussianFitter.ParameterGuesser(observations)).guess();

            GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter2.addObservedPoint(i, ys[i]);
            }

            double[] viaExplicitGuess = fitter2.fit(guessed);
            assertSameResult(ys, viaNoArg, viaExplicitGuess);
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static double[] buildPositiveGaussianLikeSeries(FuzzedDataProvider data, int len) {
        double[] ys = new double[len];

        double norm = 0.1 + (data.consumeInt(1, 5000) / 1000.0);
        double mean = data.consumeInt(0, len - 1);
        double sigma = 0.2 + (data.consumeInt(1, 2000) / 1000.0);
        double baseline = data.consumeBoolean() ? (data.consumeInt(0, 100) / 1_000_000.0) : 0.0;

        Gaussian gaussian = new Gaussian(norm, mean, sigma);
        for (int i = 0; i < len; i++) {
            double y = gaussian.value(i) + baseline;
            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 100) / 1_000_000_000.0;
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(1e-12, baseline + 1e-12);
            }
            ys[i] = y;
        }

        int extraBumps = data.consumeInt(0, 2);
        for (int b = 0; b < extraBumps; b++) {
            int idx = data.consumeInt(0, len - 1);
            ys[idx] += data.consumeInt(0, 100) / 1_000_000.0;
        }

        for (int i = 0; i < len; i++) {
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1e-12;
            }
        }

        return ys;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement ste : stack) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertSameResult(double[] input, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) length mismatch inputLen="
                    + input.length + " lhsLen=" + (a == null ? -1 : a.length) + " rhsLen=" + (b == null ? -1 : b.length));
        }

        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                            + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-7 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                        + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/Math_58_buggy/fuzz/attempt_016/FuzzHarness.java:61: error: incompatible types: WeightedObservedPoint[] cannot be converted to List<WeightedObservedPoint>
            List<WeightedObservedPoint> observations = fitter.getObservations();
                                                                             ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/Math_58_buggy/fuzz/attempt_016/FuzzHarness.java:62: error: incompatible types: List<WeightedObservedPoint> cannot be converted to WeightedObservedPoint[]
            double[] guessed = (new GaussianFitter.ParameterGuesser(observations)).guess();
                                                                    ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
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
<details open><summary>▸ Output (~6,283 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(ANCHOR_DATA);
        runScenario(buildPositiveGaussianLikeSeries(data, data.consumeInt(3, 40)));
    }

    private static void runScenario(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        try {
            double[] viaNoArg = fitter.fit();

            // Contract/oracle: fit() computes ParameterGuesser(getObservations()).guess() and then
            // delegates to the sibling overload fit(double[] initialGuess), so both overloads must
            // agree for the same observations and the exact computed guess.
            WeightedObservedPoint[] observations = fitter.getObservations();
            double[] guessed = (new GaussianFitter.ParameterGuesser(observations)).guess();

            GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter2.addObservedPoint(i, ys[i]);
            }

            double[] viaExplicitGuess = fitter2.fit(guessed);
            assertSameResult(ys, viaNoArg, viaExplicitGuess);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double[] buildPositiveGaussianLikeSeries(FuzzedDataProvider data, int len) {
        double[] ys = new double[len];

        double norm = 0.1 + (data.consumeInt(1, 5000) / 1000.0);
        double mean = data.consumeInt(0, len - 1);
        double sigma = 0.2 + (data.consumeInt(1, 2000) / 1000.0);
        double baseline = data.consumeBoolean() ? (data.consumeInt(0, 100) / 1000000.0) : 0.0;

        Gaussian gaussian = new Gaussian(norm, mean, sigma);
        for (int i = 0; i < len; i++) {
            double y = gaussian.value(i) + baseline;
            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 100) / 1000000000.0;
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(1e-12, baseline + 1e-12);
            }
            ys[i] = y;
        }

        int extraBumps = data.consumeInt(0, 2);
        for (int b = 0; b < extraBumps; b++) {
            int idx = data.consumeInt(0, len - 1);
            ys[idx] += data.consumeInt(0, 100) / 1000000.0;
        }

        for (int i = 0; i < len; i++) {
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1e-12;
            }
        }

        return ys;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertSameResult(double[] input, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) length mismatch inputLen="
                    + input.length + " lhsLen=" + (a == null ? -1 : a.length) + " rhsLen=" + (b == null ? -1 : b.length));
        }

        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                            + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-7 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                        + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
            }
        }
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,797 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 40);
        double[] xs = new double[len];
        double[] ys = new double[len];

        int step = data.consumeInt(1, 3);
        int start = data.consumeInt(-50, 50);
        boolean tailOnly = data.consumeBoolean();

        double amplitude = 1.0 + (data.consumeInt(0, 100000) / 1000.0);
        double sigma = 0.1 + (data.consumeInt(0, 20000) / 1000.0);

        double center;
        if (tailOnly) {
            center = start + (len - 1) * step + 5.0 + (data.consumeInt(0, 200) / 10.0);
        } else {
            center = start + (data.consumeInt(0, Math.max(1, (len - 1) * step * 10)) / 10.0);
        }

        double noiseScale = data.consumeInt(0, 1000) / 1_000_000.0;
        boolean forceMonotonePositive = data.consumeBoolean();

        for (int i = 0; i < len; i++) {
            double x = start + (double) i * step;
            xs[i] = x;
            double z = (x - center) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z);
            if (noiseScale > 0.0) {
                y += Math.abs(data.consumeInt(-1000, 1000)) * noiseScale;
            }
            if (forceMonotonePositive && i > 0 && y <= ys[i - 1]) {
                y = ys[i - 1] + 1e-12;
            }
            if (y <= 0.0 || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12 * (i + 1);
            }
            ys[i] = y;
        }

        runScenario(xs, ys);
    }

    private static void runAnchor() {
        double[] data = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        double[] xs = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            xs[i] = i;
        }

        runScenario(xs, data);
    }

    private static void runScenario(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(fitter, xs, ys);

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // Documented relation: no-arg fit() computes a guess from the current observations
        // and then fits; fit(double[] initialGuess) is the same operation with that guess
        // supplied explicitly. For the exact same observations and exact same guess, both
        // overloads must agree. A patch that merely suppresses the exception path, skips
        // optimization, or returns a different result from the no-arg overload breaks this.
        GaussianFitter forGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(forGuess, xs, ys);

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(forGuess.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        GaussianFitter noArgFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter explicitGuessFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(noArgFitter, xs, ys);
        addPoints(explicitGuessFitter, xs, ys);

        final double[] lhs;
        final double[] rhs;
        try {
            lhs = noArgFitter.fit();
            rhs = explicitGuessFitter.fit(guess);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (!sameParams(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(guess()) " +
                "input=" + Arrays.toString(ys) +
                " lhs=" + Arrays.toString(lhs) +
                " rhs=" + Arrays.toString(rhs)
            );
        }
    }

    private static void addPoints(GaussianFitter fitter, double[] xs, double[] ys) {
        int n = Math.min(xs.length, ys.length);
        for (int i = 0; i < n; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                return x == y;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
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
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,735 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        tryFitOnSeries(ANCHOR_DATA, 0.0, true, true);

        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 1.0 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.5 + data.consumeInt(0, 2000) / 100.0;
        double startX = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;
        double centerShift = 1.0 + data.consumeInt(0, 3000) / 50.0;
        double center = startX + (len - 1) * step + centerShift;
        double baseline = data.consumeBoolean() ? 0.0 : data.consumeInt(0, 1000) / 1_000_000.0;

        for (int i = 0; i < len; i++) {
            double x = startX + i * step;
            double dx = x - center;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent) + baseline;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12 * (i + 1);
            }
            ys[i] = y;
        }

        if (data.consumeBoolean()) {
            int bumpIndex = data.consumeInt(0, len - 1);
            ys[bumpIndex] += data.consumeInt(1, 1000) / 1_000_000.0;
        }

        tryFitOnSeries(ys, startX, step > 0.0, true);
    }

    private static void tryFitOnSeries(double[] ys, double startX, boolean distinctIncreasingX, boolean validByConstruction) {
        GaussianFitter fitterForFit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterForOverload = new GaussianFitter(new LevenbergMarquardtOptimizer());

        double x = startX;
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            fitterForFit.addObservedPoint(x, y);
            fitterForOverload.addObservedPoint(x, y);
            x += 1.0;
        }

        double[] viaFit = null;
        boolean fitSucceeded = false;
        try {
            viaFit = fitterForFit.fit();
            fitSucceeded = true;
        } catch (RuntimeException t) {
            if (validByConstruction && isGroundTruthRootCauseFromFit(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterForOverload.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] viaOverload;
        try {
            viaOverload = fitterForOverload.fit(guess);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (fitSucceeded && distinctIncreasingX) {
            /* Contract/oracle:
             * GaussianFitter.fit() computes a guess from the current observations and delegates to
             * the same-named overload fit(double[] initialGuess). Therefore, for the same observations,
             * fit() and fit(new ParameterGuesser(getObservations()).guess()) must agree whenever both
             * calls return normally. A patch that merely suppresses the buggy throw or changes the call
             * target can violate this observable equivalence without throwing.
             */
            if (!sameParams(viaFit, viaOverload)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() != fit(guess()) ysLen=" + ys.length
                        + " lhs=" + format(viaFit) + " rhs=" + format(viaOverload));
            }
        }
    }

    private static boolean isGroundTruthRootCauseFromFit(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,642 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        runCase(anchor, true);

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];

        boolean useMonotoneIncreasing = data.consumeBoolean();
        if (useMonotoneIncreasing) {
            double current = Math.abs((double) data.consumeInt(-1000, 1000)) / 1000.0 + 1e-12;
            for (int i = 0; i < n; i++) {
                int stepRaw = data.consumeInt(0, 1000);
                current += (stepRaw + 1) * 1e-6;
                ys[i] = current;
            }
        } else {
            int center = data.consumeInt(0, n - 1);
            double amplitude = Math.abs((double) data.consumeInt(-100000, 100000)) / 1000.0 + 1e-6;
            double sigma = Math.abs((double) data.consumeInt(1, 2000)) / 200.0 + 0.1;
            double baseline = Math.abs((double) data.consumeInt(-1000, 1000)) / 1000.0;
            Gaussian.Parametric g = new Gaussian.Parametric();
            for (int i = 0; i < n; i++) {
                double x = i;
                double y;
                try {
                    y = g.value(x, new double[] { amplitude, center, sigma });
                } catch (RuntimeException e) {
                    return;
                }
                double noise = ((double) data.consumeInt(-1000, 1000)) / 1_000_000.0;
                ys[i] = Math.max(1e-12, baseline + y + noise);
            }
        }

        runCase(ys, false);
    }

    private static void runCase(double[] ys, boolean propagateRootCause) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            if (Double.isNaN(y) || Double.isInfinite(y) || y <= 0.0) {
                y = Math.abs(y);
                if (Double.isNaN(y) || Double.isInfinite(y) || y == 0.0) {
                    y = 1e-12;
                }
            }
            fitter.addObservedPoint(i, y);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, propagateRootCause)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] viaOverload;
        try {
            viaOverload = fitter.fit(guess);
        } catch (Throwable t) {
            if (shouldPropagate(t, false)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, propagateRootCause)) {
                sneakyThrow(t);
            }
            return;
        }

        /* Contract/oracle: GaussianFitter.fit() computes a ParameterGuesser guess and then delegates to
           the same fitting operation as fit(double[] initialGuess). Therefore, for the same observations,
           fit() and fit(guess()) must agree whenever both calls succeed. A patch that merely suppresses
           the exception path or changes the delegation logic can violate this observable equivalence. */
        if (viaNoArg == null || viaOverload == null || viaNoArg.length != viaOverload.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(guess()) returned incompatible shapes");
        }
        for (int i = 0; i < viaNoArg.length; i++) {
            double a = viaNoArg[i];
            double b = viaOverload[i];
            boolean equal;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                equal = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                equal = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                equal = Math.abs(a - b) <= 1e-8 * scale;
            }
            if (!equal) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) inputLen=" + ys.length +
                    " index=" + i + " lhs=" + a + " rhs=" + b
                );
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean allowRootCause) {
        if (!allowRootCause) {
            return false;
        }
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,927 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.exception.NotStrictlyPositiveException;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 3);
        if (mode == 0) {
            double[] ys = buildMonotonePositiveSeries(data);
            runExplore(ys, data.consumeInt(-50, 50));
        } else if (mode == 1) {
            double[] ys = mutateAnchor(data);
            runExplore(ys, data.consumeInt(-50, 50));
        } else if (mode == 2) {
            double[] ys = buildMonotonePositiveSeries(data);
            reverseIfRequested(ys, data.consumeBoolean());
            runExplore(ys, data.consumeInt(-50, 50));
        } else {
            double[] ys = buildPlateauThenRise(data);
            runExplore(ys, data.consumeInt(-50, 50));
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        // Contract/metamorphic check:
        // no-arg fit() computes a guess from the current observations and delegates to the
        // overload fit(double[] initialGuess). Therefore, for the same observations and the
        // same real guess produced by ParameterGuesser, both overloads must agree.
        // A "fix" that merely deletes the throwing path or changes behavior only in fit()
        // would violate this sibling-agreement relation.
        assertSiblingAgreement(ANCHOR_DATA, 0.0);
    }

    private static void runExplore(double[] ys, double xOffset) {
        if (ys == null || ys.length < 3) {
            return;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xOffset + i, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        assertSiblingAgreement(ys, xOffset);
    }

    private static void assertSiblingAgreement(double[] ys, double xOffset) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xOffset + i;
            f1.addObservedPoint(x, ys[i]);
            f2.addObservedPoint(x, ys[i]);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] a;
        double[] b;
        try {
            a = f1.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }
        try {
            b = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (!sameParams(a, b)) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() must agree with fit(guess()) inputLen=" + ys.length + " lhs=" + format(a) + " rhs=" + format(b));
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1.0e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String pn = p.getName();
            if (pn.startsWith("org.apache.commons.math.exception")) {
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

    private static double[] buildMonotonePositiveSeries(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        double exp = -data.consumeInt(5, 35);
        double current = Math.pow(10.0, exp);
        double ratioBase = 1.05 + (data.consumeInt(0, 500) / 100.0);
        for (int i = 0; i < n; i++) {
            double localRatio = ratioBase + (data.consumeInt(0, 20) / 100.0);
            if (localRatio <= 1.0) {
                localRatio = 1.01;
            }
            current *= localRatio;
            if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current) || current > 1.0e100) {
                current = ys[Math.max(0, i - 1)] * 1.5;
                if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current)) {
                    current = 1.0;
                }
            }
            ys[i] = current;
        }
        return ys;
    }

    private static double[] mutateAnchor(FuzzedDataProvider data) {
        int prefixDrop = data.consumeInt(0, Math.min(8, ANCHOR_DATA.length - 3));
        int suffixDrop = data.consumeInt(0, Math.min(8, ANCHOR_DATA.length - prefixDrop - 3));
        int n = ANCHOR_DATA.length - prefixDrop - suffixDrop;
        if (n < 3) {
            n = 3;
        }
        double[] ys = new double[n];
        double scale = Math.pow(10.0, data.consumeInt(-3, 3));
        if (!(scale > 0.0) || Double.isInfinite(scale) || Double.isNaN(scale)) {
            scale = 1.0;
        }
        for (int i = 0; i < n; i++) {
            double v = ANCHOR_DATA[prefixDrop + i] * scale;
            double tweak = 1.0 + (data.consumeInt(-5, 5) / 1000.0);
            v *= tweak;
            if (i > 0 && v <= ys[i - 1]) {
                v = ys[i - 1] * (1.001 + (data.consumeInt(0, 50) / 1000.0));
            }
            if (!(v > 0.0) || Double.isInfinite(v) || Double.isNaN(v)) {
                v = (i == 0) ? 1e-30 : ys[i - 1] * 1.01;
            }
            ys[i] = v;
        }
        return ys;
    }

    private static double[] buildPlateauThenRise(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        int plateau = data.consumeInt(0, n - 1);
        double[] ys = new double[n];
        double base = Math.pow(10.0, -data.consumeInt(5, 30));
        if (!(base > 0.0) || Double.isInfinite(base) || Double.isNaN(base)) {
            base = 1e-10;
        }
        double current = base;
        for (int i = 0; i < n; i++) {
            if (i < plateau) {
                ys[i] = current;
            } else {
                current *= 1.2 + (data.consumeInt(0, 400) / 100.0);
                if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current) || current > 1.0e100) {
                    current = ys[Math.max(0, i - 1)] * 1.5;
                    if (!(current > 0.0)) {
                        current = 1.0;
                    }
                }
                ys[i] = current;
            }
        }
        for (int i = 1; i < ys.length; i++) {
            if (ys[i] < ys[i - 1]) {
                ys[i] = ys[i - 1];
            }
            if (!(ys[i] > 0.0)) {
                ys[i] = ys[i - 1] + 1e-12;
            }
        }
        return ys;
    }

    private static void reverseIfRequested(double[] ys, boolean reverse) {
        if (!reverse) {
            return;
        }
        for (int i = 0, j = ys.length - 1; i < j; i++, j--) {
            double tmp = ys[i];
            ys[i] = ys[j];
            ys[j] = tmp;
        }
        for (int i = 1; i < ys.length; i++) {
            if (ys[i] <= ys[i - 1]) {
                ys[i] = ys[i - 1] * 1.01;
            }
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,269 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and is supposed
         * to delegate to the same fitting logic as fit(double[] initialGuess). Therefore, for the same
         * valid observations and the exact same guessed initial parameters, fit() and fit(guess) must
         * agree. A "fix" that merely suppresses the exception or skips the real delegation would break
         * this sibling-overload agreement.
         */

        {
            final double[] anchorData = new double[] {
                1.1143831578403364E-29,
                4.95281403484594E-28,
                1.1171347211930288E-26,
                1.7044813962636277E-25,
                1.9784716574832164E-24,
                1.8630236407866774E-23,
                1.4820532905097742E-22,
                1.0241963854632831E-21,
                6.275077366673128E-21,
                3.461808994532493E-20,
                1.7407124684715706E-19,
                8.056687953553974E-19,
                3.460193945992071E-18,
                1.3883326374011525E-17,
                5.233894983671116E-17,
                1.8630791465263745E-16,
                6.288759227922111E-16,
                2.0204433920597856E-15,
                6.198768938576155E-15,
                1.821419346860626E-14,
                5.139176445538471E-14,
                1.3956427429045787E-13,
                3.655705706448139E-13,
                9.253753324779779E-13,
                2.267636001476696E-12,
                5.3880460095836855E-12,
                1.2431632654852931E-11
            };

            GaussianFitter anchorFitNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter anchorFitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            int i;
            for (i = 0; i < anchorData.length; i++) {
                anchorFitNoArg.addObservedPoint(i, anchorData[i]);
                anchorFitWithGuess.addObservedPoint(i, anchorData[i]);
            }

            double[] anchorGuess;
            try {
                anchorGuess = (new GaussianFitter.ParameterGuesser(anchorFitWithGuess.getObservations())).guess();
            } catch (RuntimeException t) {
                anchorGuess = null;
            }

            double[] anchorNoArgResult = null;
            boolean anchorNoArgOk = false;
            try {
                anchorNoArgResult = anchorFitNoArg.fit();
                anchorNoArgOk = true;
            } catch (RuntimeException t) {
                boolean throughFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if ((t instanceof NotStrictlyPositiveException) && throughFit) {
                    throw t;
                }
            }

            if (anchorGuess != null) {
                double[] anchorWithGuessResult = null;
                boolean anchorWithGuessOk = false;
                try {
                    anchorWithGuessResult = anchorFitWithGuess.fit(anchorGuess);
                    anchorWithGuessOk = true;
                } catch (RuntimeException t) {
                    if (!(t instanceof IllegalArgumentException)) {
                        // Skip oracle on unrelated failures.
                    }
                }

                if (anchorNoArgOk && anchorWithGuessOk) {
                    if (anchorNoArgResult == null || anchorWithGuessResult == null
                        || anchorNoArgResult.length != anchorWithGuessResult.length) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) length mismatch input="
                            + Arrays.toString(anchorData)
                            + " lhs=" + Arrays.toString(anchorNoArgResult)
                            + " rhs=" + Arrays.toString(anchorWithGuessResult));
                    }
                    for (i = 0; i < anchorNoArgResult.length; i++) {
                        double a = anchorNoArgResult[i];
                        double b = anchorWithGuessResult[i];
                        boolean same;
                        if (Double.isNaN(a) && Double.isNaN(b)) {
                            same = true;
                        } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                            same = a == b;
                        } else {
                            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                            same = Math.abs(a - b) <= 1.0e-9 * scale;
                        }
                        if (!same) {
                            throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) disagree input="
                                + Arrays.toString(anchorData)
                                + " lhs=" + Arrays.toString(anchorNoArgResult)
                                + " rhs=" + Arrays.toString(anchorWithGuessResult));
                        }
                    }
                }
            }
        }

        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        int j;
        for (j = 0; j < n; j++) {
            double x = xOffset + j * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[j] = y;
            fitNoArg.addObservedPoint(x, y);
            fitWithGuess.addObservedPoint(x, y);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitWithGuess.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (j = 0; j < guess.length; j++) {
            if (Double.isNaN(guess[j]) || Double.isInfinite(guess[j])) {
                return;
            }
        }

        double[] lhs = null;
        boolean lhsOk = false;
        try {
            lhs = fitNoArg.fit();
            lhsOk = true;
        } catch (RuntimeException t) {
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (j = 0; j < st.length; j++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[j].getClassName())
                    && "fit".equals(st[j].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }

            if ((t instanceof NotStrictlyPositiveException) && throughFit) {
                throw t;
            }

            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        double[] rhs = null;
        boolean rhsOk = false;
        try {
            rhs = fitWithGuess.fit(guess);
            rhsOk = true;
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsOk || !rhsOk) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) length mismatch input="
                + Arrays.toString(observedY)
                + " guess=" + Arrays.toString(guess)
                + " lhs=" + Arrays.toString(lhs)
                + " rhs=" + Arrays.toString(rhs));
        }

        for (j = 0; j < lhs.length; j++) {
            double a = lhs[j];
            double b = rhs[j];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) disagree input="
                    + Arrays.toString(observedY)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `anchorGuess` (literal assignment, no rethrow, the caught ex

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,423 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and is supposed
         * to delegate to the same fitting logic as fit(double[] initialGuess). Therefore, for the same
         * valid observations and the exact same guessed initial parameters, fit() and fit(guess) must
         * agree. A "fix" that merely suppresses the exception or skips the real delegation would break
         * this sibling-overload agreement.
         */

        {
            final double[] anchorData = new double[] {
                1.1143831578403364E-29,
                4.95281403484594E-28,
                1.1171347211930288E-26,
                1.7044813962636277E-25,
                1.9784716574832164E-24,
                1.8630236407866774E-23,
                1.4820532905097742E-22,
                1.0241963854632831E-21,
                6.275077366673128E-21,
                3.461808994532493E-20,
                1.7407124684715706E-19,
                8.056687953553974E-19,
                3.460193945992071E-18,
                1.3883326374011525E-17,
                5.233894983671116E-17,
                1.8630791465263745E-16,
                6.288759227922111E-16,
                2.0204433920597856E-15,
                6.198768938576155E-15,
                1.821419346860626E-14,
                5.139176445538471E-14,
                1.3956427429045787E-13,
                3.655705706448139E-13,
                9.253753324779779E-13,
                2.267636001476696E-12,
                5.3880460095836855E-12,
                1.2431632654852931E-11
            };

            GaussianFitter anchorFitNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter anchorFitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            int i;
            for (i = 0; i < anchorData.length; i++) {
                anchorFitNoArg.addObservedPoint(i, anchorData[i]);
                anchorFitWithGuess.addObservedPoint(i, anchorData[i]);
            }

            double[] anchorGuess;
            try {
                anchorGuess = (new GaussianFitter.ParameterGuesser(anchorFitWithGuess.getObservations())).guess();
            } catch (RuntimeException t) {
                anchorGuess = null;
            }

            double[] anchorNoArgResult = null;
            boolean anchorNoArgOk = false;
            try {
                anchorNoArgResult = anchorFitNoArg.fit();
                anchorNoArgOk = true;
            } catch (RuntimeException t) {
                boolean throughFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if ((t instanceof NotStrictlyPositiveException) && throughFit) {
                    throw t;
                }
            }

            if (anchorGuess != null) {
                double[] anchorWithGuessResult = null;
                boolean anchorWithGuessOk = false;
                try {
                    anchorWithGuessResult = anchorFitWithGuess.fit(anchorGuess);
                    anchorWithGuessOk = true;
                } catch (RuntimeException t) {
                    if (!(t instanceof IllegalArgumentException)) {
                        // Skip oracle on unrelated failures.
                    }
                }

                if (anchorNoArgOk && anchorWithGuessOk) {
                    if (anchorNoArgResult == null || anchorWithGuessResult == null
                        || anchorNoArgResult.length != anchorWithGuessResult.length) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) length mismatch input="
                            + Arrays.toString(anchorData)
                            + " lhs=" + Arrays.toString(anchorNoArgResult)
                            + " rhs=" + Arrays.toString(anchorWithGuessResult));
                    }
                    for (i = 0; i < anchorNoArgResult.length; i++) {
                        double a = anchorNoArgResult[i];
                        double b = anchorWithGuessResult[i];
                        boolean same;
                        if (Double.isNaN(a) && Double.isNaN(b)) {
                            same = true;
                        } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                            same = a == b;
                        } else {
                            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                            same = Math.abs(a - b) <= 1.0e-9 * scale;
                        }
                        if (!same) {
                            throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) disagree input="
                                + Arrays.toString(anchorData)
                                + " lhs=" + Arrays.toString(anchorNoArgResult)
                                + " rhs=" + Arrays.toString(anchorWithGuessResult));
                        }
                    }
                }
            }
        }

        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        int j;
        for (j = 0; j < n; j++) {
            double x = xOffset + j * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[j] = y;
            fitNoArg.addObservedPoint(x, y);
            fitWithGuess.addObservedPoint(x, y);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitWithGuess.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (j = 0; j < guess.length; j++) {
            if (Double.isNaN(guess[j]) || Double.isInfinite(guess[j])) {
                return;
            }
        }

        double[] lhs = null;
        boolean lhsOk = false;
        try {
            lhs = fitNoArg.fit();
            lhsOk = true;
        } catch (RuntimeException t) {
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (j = 0; j < st.length; j++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[j].getClassName())
                    && "fit".equals(st[j].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }

            if ((t instanceof NotStrictlyPositiveException) && throughFit) {
                throw t;
            }

            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        double[] rhs = null;
        boolean rhsOk = false;
        try {
            rhs = fitWithGuess.fit(guess);
            rhsOk = true;
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsOk || !rhsOk) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) length mismatch input="
                + Arrays.toString(observedY)
                + " guess=" + Arrays.toString(guess)
                + " lhs=" + Arrays.toString(lhs)
                + " rhs=" + Arrays.toString(rhs));
        }

        for (j = 0; j < lhs.length; j++) {
            double a = lhs[j];
            double b = rhs[j];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) disagree input="
                    + Arrays.toString(observedY)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `anchorGuess` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 93 is conditioned on `anchorGuess` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,738 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and delegates
         * to the sibling overload fit(double[] initialGuess). For the same observations and the exact
         * same guessed parameters, both overloads must produce the same result whenever both calls
         * succeed. A patch that only suppresses the crash or skips the real delegation breaks this.
         */

        runAnchor();
        runFuzz(data);
    }

    private static void runAnchor() {
        final double[] anchorData = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterNoArg.addObservedPoint(i, anchorData[i]);
            fitterWithGuess.addObservedPoint(i, anchorData[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult("[oracle:fit-overload-anchor]", anchorData, guess, lhs, rhs);
    }

    private static void runFuzz(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[i] = y;
            fitterNoArg.addObservedPoint(x, y);
            fitterWithGuess.addObservedPoint(x, y);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (int i = 0; i < guess.length; i++) {
            if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {
                return;
            }
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult("[oracle:fit-overload-fuzz]", observedY, guess, lhs, rhs);
    }

    private static boolean isRootCauseFromFit(RuntimeException e) {
        if (!(e instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException;
    }

    private static void assertSameResult(String oracleId, double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException(oracleId + " metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException(oracleId + " metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 181 has no oracle ID (`throw new RuntimeException(oracleId + " metamorphic violation: fit() and fit(guess) length mismatch input=" 

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~8,599 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and delegates
         * to the sibling overload fit(double[] initialGuess). For the same observations and the exact
         * same guessed parameters, both overloads must produce the same result whenever both calls
         * succeed. A patch that only suppresses the crash or skips the real delegation breaks this.
         */

        runAnchor();
        runFuzz(data);
    }

    private static void runAnchor() {
        final double[] anchorData = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterNoArg.addObservedPoint(i, anchorData[i]);
            fitterWithGuess.addObservedPoint(i, anchorData[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult("[oracle:fit-overload-anchor]", anchorData, guess, lhs, rhs);
    }

    private static void runFuzz(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[i] = y;
            fitterNoArg.addObservedPoint(x, y);
            fitterWithGuess.addObservedPoint(x, y);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (int i = 0; i < guess.length; i++) {
            if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {
                return;
            }
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult("[oracle:fit-overload-fuzz]", observedY, guess, lhs, rhs);
    }

    private static boolean isRootCauseFromFit(RuntimeException e) {
        if (!(e instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException;
    }

    private static void assertSameResult(String oracleId, double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException(oracleId + " metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException(oracleId + " metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 181 has no oracle ID (`throw new RuntimeException(oracleId + " metamorphic violation: fit() and fit(guess) length mismatch input=" + Arrays.toS...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~8,039 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and delegates
         * to the sibling overload fit(double[] initialGuess). For the same observations and the exact
         * same guessed parameters, both overloads must produce the same result whenever both calls
         * succeed. A patch that only suppresses the crash or skips the real delegation breaks this.
         */
        runAnchor();
        runFuzz(data);
    }

    private static void runAnchor() {
        final double[] anchorData = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterNoArg.addObservedPoint(i, anchorData[i]);
            fitterWithGuess.addObservedPoint(i, anchorData[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult(
                "[oracle:fit-anchor-len]",
                "[oracle:fit-anchor-eq]",
                anchorData,
                guess,
                lhs,
                rhs);
    }

    private static void runFuzz(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[i] = y;
            fitterNoArg.addObservedPoint(x, y);
            fitterWithGuess.addObservedPoint(x, y);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (int i = 0; i < guess.length; i++) {
            if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {
                return;
            }
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult(
                "[oracle:fit-fuzz-len]",
                "[oracle:fit-fuzz-eq]",
                observedY,
                guess,
                lhs,
                rhs);
    }

    private static boolean isRootCauseFromFit(RuntimeException e) {
        if (!(e instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException;
    }

    private static void assertSameResult(String lenOracleId, String eqOracleId,
                                         double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException(lenOracleId + " metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException(eqOracleId + " metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 193 has no oracle ID (`throw new RuntimeException(lenOracleId + " metamorphic violation: fit() and fit(guess) length mismatch input

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~9,021 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
- *[assistant] message: identical to step [49] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and delegates
         * to the sibling overload fit(double[] initialGuess). For the same observations and the exact
         * same guessed parameters, both overloads must produce the same result whenever both calls
         * succeed. A patch that only suppresses the crash or skips the real delegation breaks this.
         */
        runAnchor();
        runFuzz(data);
    }

    private static void runAnchor() {
        final double[] anchorData = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterNoArg.addObservedPoint(i, anchorData[i]);
            fitterWithGuess.addObservedPoint(i, anchorData[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult(
                "[oracle:fit-anchor-len]",
                "[oracle:fit-anchor-eq]",
                anchorData,
                guess,
                lhs,
                rhs);
    }

    private static void runFuzz(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[i] = y;
            fitterNoArg.addObservedPoint(x, y);
            fitterWithGuess.addObservedPoint(x, y);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (int i = 0; i < guess.length; i++) {
            if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {
                return;
            }
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResult(
                "[oracle:fit-fuzz-len]",
                "[oracle:fit-fuzz-eq]",
                observedY,
                guess,
                lhs,
                rhs);
    }

    private static boolean isRootCauseFromFit(RuntimeException e) {
        if (!(e instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException;
    }

    private static void assertSameResult(String lenOracleId, String eqOracleId,
                                         double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException(lenOracleId + " metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException(eqOracleId + " metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 193 has no oracle ID (`throw new RuntimeException(lenOracleId + " metamorphic violation: fit() and fit(guess) length mismatch input=" + Arrays....`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~9,128 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and delegates
         * to the sibling overload fit(double[] initialGuess). For the same observations and the exact
         * same guessed parameters, both overloads must produce the same result whenever both calls
         * succeed. A patch that only suppresses the crash or skips the real delegation breaks this.
         */
        runAnchor();
        runFuzz(data);
    }

    private static void runAnchor() {
        final double[] anchorData = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterNoArg.addObservedPoint(i, anchorData[i]);
            fitterWithGuess.addObservedPoint(i, anchorData[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResultAnchor(anchorData, guess, lhs, rhs);
    }

    private static void runFuzz(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[i] = y;
            fitterNoArg.addObservedPoint(x, y);
            fitterWithGuess.addObservedPoint(x, y);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (int i = 0; i < guess.length; i++) {
            if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {
                return;
            }
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResultFuzz(observedY, guess, lhs, rhs);
    }

    private static boolean isRootCauseFromFit(RuntimeException e) {
        if (!(e instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException;
    }

    private static void assertSameResultAnchor(double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-anchor-len] metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException("[oracle:fit-anchor-eq] metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }

    private static void assertSameResultFuzz(double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-fuzz-len] metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException("[oracle:fit-fuzz-eq] metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,685 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() is documented/implemented as "compute a guess from the current
         * observations, then delegate to fit(double[] initialGuess)". Therefore, for the same
         * observation set, fit() must produce the same result as fit(new ParameterGuesser(...).guess()).
         * A patch that merely suppresses the crashing path, skips delegation, or otherwise changes
         * behavior without preserving that contract will violate this equivalence.
         */

        final double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        boolean stackHasFit = false;
        try {
            GaussianFitter fitter = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            fitter.fit();
        } catch (Throwable t) {
            stackHasFit = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    stackHasFit = true;
                    break;
                }
            }
            if (stackHasFit
                    && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException
                    && t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }

        try {
            GaussianFitter f1 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            GaussianFitter f2 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < anchor.length; i++) {
                f1.addObservedPoint(i, anchor[i]);
                f2.addObservedPoint(i, anchor[i]);
            }
            double[] lhs = f1.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            if (lhs == null || rhs == null || lhs.length != rhs.length) {
                throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) returned incompatible result shapes");
            }
            for (int i = 0; i < lhs.length; i++) {
                double a = lhs[i];
                double b = rhs[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    if (a != b) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() vs fit(guess) input=anchor idx=" + i + " lhs=" + a + " rhs=" + b);
                    }
                    continue;
                }
                double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > tol) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() vs fit(guess) input=anchor idx=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        int n = data.consumeInt(3, 32);
        double[] ys = new double[n];
        double[] xs = new double[n];

        int exp = data.consumeInt(8, 40);
        double y = Math.pow(10.0, -exp);
        if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
            y = 1.0e-12;
        }

        double x = 0.0;
        for (int i = 0; i < n; i++) {
            x += 1.0 + data.consumeInt(0, 3);
            xs[i] = x;

            int num = data.consumeInt(105, 400);
            double factor = num / 100.0;
            y *= factor;
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y) || y > 1.0e100) {
                y = Math.max(1.0e-300, Math.abs(y % 1.0e6) + 1.0e-12);
            }
            ys[i] = y;
        }

        try {
            GaussianFitter fitter = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < n; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
            fitter.fit();
        } catch (Throwable t) {
            boolean hasFit = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    hasFit = true;
                    break;
                }
            }
            if (hasFit
                    && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException
                    && t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            GaussianFitter f1 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            GaussianFitter f2 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < n; i++) {
                f1.addObservedPoint(xs[i], ys[i]);
                f2.addObservedPoint(xs[i], ys[i]);
            }

            double[] lhs = f1.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            if (lhs == null || rhs == null || lhs.length != rhs.length) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) returned incompatible result shapes");
            }
            for (int i = 0; i < lhs.length; i++) {
                double a = lhs[i];
                double b = rhs[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    if (a != b) {
                        throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() vs fit(guess) inputLength=" + n + " idx=" + i + " lhs=" + a + " rhs=" + b);
                    }
                    continue;
                }
                double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > tol) {
                    throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() vs fit(guess) inputLength=" + n + " idx=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,098 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseDataset(buildXs(ANCHOR_DATA.length, 0.0, 1.0), ANCHOR_DATA);

        int n = data.consumeInt(3, 40);
        double startX = data.consumeInt(-100, 100);
        double step = data.consumeInt(1, 5);

        double[] gaussianTail = buildPositiveIncreasingGaussianTail(data, n, startX, step);
        exerciseDataset(buildXs(n, startX, step), gaussianTail);

        int m = data.consumeInt(3, ANCHOR_DATA.length);
        double[] scaledAnchor = buildScaledAnchorPrefix(data, m);
        double scaledStartX = data.consumeInt(-50, 50);
        double scaledStep = data.consumeInt(1, 4);
        exerciseDataset(buildXs(m, scaledStartX, scaledStep), scaledAnchor);
    }

    private static double[] buildXs(int n, double start, double step) {
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
        }
        return xs;
    }

    private static double[] buildPositiveIncreasingGaussianTail(FuzzedDataProvider data, int n, double startX, double step) {
        double[] ys = new double[n];
        double lastX = startX + (n - 1) * step;
        double center = lastX + data.consumeInt(1, 60);
        double sigma = data.consumeInt(1, 50);
        double ampPow = data.consumeInt(-20, 3);
        double amp = Math.pow(10.0, ampPow);
        double floorPow = data.consumeInt(-40, -15);
        double floor = Math.pow(10.0, floorPow);
        for (int i = 0; i < n; i++) {
            double x = startX + i * step;
            double d = (x - center) / sigma;
            double y = floor + amp * Math.exp(-0.5 * d * d);
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = floor;
            }
            ys[i] = y;
        }
        for (int i = 1; i < n; i++) {
            if (!(ys[i] > ys[i - 1])) {
                ys[i] = Math.nextUp(ys[i - 1]);
            }
        }
        return ys;
    }

    private static double[] buildScaledAnchorPrefix(FuzzedDataProvider data, int n) {
        double[] ys = new double[n];
        int scalePow = data.consumeInt(-5, 5);
        double scale = Math.pow(10.0, scalePow);
        double additivePow = data.consumeInt(-40, -20);
        double additive = Math.pow(10.0, additivePow);
        for (int i = 0; i < n; i++) {
            double y = ANCHOR_DATA[i] * scale + additive;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = additive;
            }
            ys[i] = y;
        }
        for (int i = 1; i < n; i++) {
            if (!(ys[i] > ys[i - 1])) {
                ys[i] = Math.nextUp(ys[i - 1]);
            }
        }
        return ys;
    }

    private static GaussianFitter newFitter(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
        return fitter;
    }

    private static void exerciseDataset(double[] xs, double[] ys) {
        GaussianFitter fitter = newFitter(xs, ys);
        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() computes "guess" from the current observations and delegates
         * to fit(double[] initialGuess) with that same guess. Therefore, for the same
         * observations, successful executions of both overloads must agree. A patch that
         * merely suppresses the throw or skips the real delegation path can violate this
         * observable overload-agreement relation even when no exception is raised.
         */
        GaussianFitter noArgFitter = newFitter(xs, ys);
        GaussianFitter explicitGuessFitter = newFitter(xs, ys);

        double[] lhs;
        try {
            lhs = noArgFitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(explicitGuessFitter.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = explicitGuessFitter.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:overload-agree] metamorphic violation: fit() != fit(guess) xs="
                    + arrayToString(xs) + " ys=" + arrayToString(ys)
                    + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,535 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchorX = new double[ANCHOR_Y.length];
        for (int i = 0; i < anchorX.length; i++) {
            anchorX[i] = i;
        }
        runCase(anchorX, ANCHOR_Y, true);

        int cases = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 2) : 0);
        for (int c = 0; c < cases; c++) {
            double[][] generated = buildIncreasingOneSidedGaussian(data);
            if (generated == null) {
                return;
            }
            runCase(generated[0], generated[1], true);
        }
    }

    private static double[][] buildIncreasingOneSidedGaussian(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return null;
        }

        int len = data.consumeInt(3, 40);
        double[] x = new double[len];
        double[] y = new double[len];

        int start = data.consumeInt(-100, 100);
        int step = data.consumeInt(1, 5);
        int extraToMean = data.consumeInt(1, 50);
        double norm = data.consumeInt(1, 1_000_000) / 1000.0;
        double sigma = data.consumeInt(1, 50_000) / 1000.0 + 0.001;

        for (int i = 0; i < len; i++) {
            x[i] = start + ((double) i) * step;
        }

        double mean = x[len - 1] + ((double) extraToMean) * step;
        Gaussian.Parametric g = new Gaussian.Parametric();
        double[] params = new double[] { norm, mean, sigma };

        try {
            for (int i = 0; i < len; i++) {
                y[i] = g.value(x[i], params);
                if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                    return null;
                }
                if (i > 0 && !(y[i] > y[i - 1])) {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }

        return new double[][] { x, y };
    }

    private static void runCase(double[] x, double[] y, boolean validByConstruction) {
        try {
            GaussianFitter crashProbe = newFitterWithPoints(x, y);
            crashProbe.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Oracle: GaussianFitter.fit() and GaussianFitter.fit(double[] initialGuess) are sibling
           overloads for the same operation; the patched method is literally supposed to delegate to
           the overload with the guessed parameters. Therefore, when both calls succeed on the same
           valid observations, their returned parameter arrays must agree. A "fix" that merely hides
           the exception or skips the intended path can violate this without throwing. */
        try {
            GaussianFitter f1 = newFitterWithPoints(x, y);
            double[] lhs = f1.fit();

            GaussianFitter f2 = newFitterWithPoints(x, y);
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            assertArrayClose("[oracle:fit-overload-agreement]", x, y, lhs, rhs);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }
    }

    private static GaussianFitter newFitterWithPoints(double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length && i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        return validByConstruction
            && (t instanceof NotStrictlyPositiveException)
            && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static void assertArrayClose(String prefix, double[] x, double[] y, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException(prefix + " metamorphic violation: length mismatch inputLen="
                + x.length + " aLen=" + (a == null ? -1 : a.length) + " bLen=" + (b == null ? -1 : b.length));
        }
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isNaN(da) && Double.isNaN(db)) {
                continue;
            }
            if (Double.isInfinite(da) || Double.isInfinite(db)) {
                if (da == db) {
                    continue;
                }
                throw new RuntimeException(prefix + " metamorphic violation: non-finite mismatch idx=" + i
                    + " inputLen=" + x.length + " lhs=" + da + " rhs=" + db);
            }
            double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(da), Math.abs(db)));
            if (Math.abs(da - db) > tol) {
                throw new RuntimeException(prefix + " metamorphic violation: fit() != fit(guess) idx=" + i
                    + " inputLen=" + x.length + " firstX=" + x[0] + " lastX=" + x[x.length - 1]
                    + " firstY=" + y[0] + " lastY=" + y[y.length - 1]
                    + " lhs=" + da + " rhs=" + db);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 162 has no oracle ID (`throw new RuntimeException(prefix + " metamorphic violation: length mismatch inputLen=" + x.length + " aLen=

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,275 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchorX = new double[ANCHOR_Y.length];
        for (int i = 0; i < anchorX.length; i++) {
            anchorX[i] = i;
        }
        runCase(anchorX, ANCHOR_Y, true);

        int cases = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 2) : 0);
        for (int c = 0; c < cases; c++) {
            double[][] generated = buildIncreasingOneSidedGaussian(data);
            if (generated == null) {
                return;
            }
            runCase(generated[0], generated[1], true);
        }
    }

    private static double[][] buildIncreasingOneSidedGaussian(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return null;
        }

        int len = data.consumeInt(3, 40);
        double[] x = new double[len];
        double[] y = new double[len];

        int start = data.consumeInt(-100, 100);
        int step = data.consumeInt(1, 5);
        int extraToMean = data.consumeInt(1, 50);
        double norm = data.consumeInt(1, 1_000_000) / 1000.0;
        double sigma = data.consumeInt(1, 50_000) / 1000.0 + 0.001;

        for (int i = 0; i < len; i++) {
            x[i] = start + ((double) i) * step;
        }

        double mean = x[len - 1] + ((double) extraToMean) * step;
        Gaussian.Parametric g = new Gaussian.Parametric();
        double[] params = new double[] { norm, mean, sigma };

        try {
            for (int i = 0; i < len; i++) {
                y[i] = g.value(x[i], params);
                if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                    return null;
                }
                if (i > 0 && !(y[i] > y[i - 1])) {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }

        return new double[][] { x, y };
    }

    private static void runCase(double[] x, double[] y, boolean validByConstruction) {
        try {
            GaussianFitter crashProbe = newFitterWithPoints(x, y);
            crashProbe.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Oracle: GaussianFitter.fit() and GaussianFitter.fit(double[] initialGuess) are sibling
           overloads for the same operation; the patched method is literally supposed to delegate to
           the overload with the guessed parameters. Therefore, when both calls succeed on the same
           valid observations, their returned parameter arrays must agree. A "fix" that merely hides
           the exception or skips the intended path can violate this without throwing. */
        try {
            GaussianFitter f1 = newFitterWithPoints(x, y);
            double[] lhs = f1.fit();

            GaussianFitter f2 = newFitterWithPoints(x, y);
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            assertArrayClose("[oracle:fit-overload-agreement]", x, y, lhs, rhs);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }
    }

    private static GaussianFitter newFitterWithPoints(double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length && i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        return validByConstruction
            && (t instanceof NotStrictlyPositiveException)
            && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static void assertArrayClose(String prefix, double[] x, double[] y, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException(prefix + " metamorphic violation: length mismatch inputLen="
                + x.length + " aLen=" + (a == null ? -1 : a.length) + " bLen=" + (b == null ? -1 : b.length));
        }
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isNaN(da) && Double.isNaN(db)) {
                continue;
            }
            if (Double.isInfinite(da) || Double.isInfinite(db)) {
                if (da == db) {
                    continue;
                }
                throw new RuntimeException(prefix + " metamorphic violation: non-finite mismatch idx=" + i
                    + " inputLen=" + x.length + " lhs=" + da + " rhs=" + db);
            }
            double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(da), Math.abs(db)));
            if (Math.abs(da - db) > tol) {
                throw new RuntimeException(prefix + " metamorphic violation: fit() != fit(guess) idx=" + i
                    + " inputLen=" + x.length + " firstX=" + x[0] + " lastX=" + x[x.length - 1]
                    + " firstY=" + y[0] + " lastY=" + y[y.length - 1]
                    + " lhs=" + da + " rhs=" + db);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 162 has no oracle ID (`throw new RuntimeException(prefix + " metamorphic violation: length mismatch inputLen=" + x.length + " aLen=" + (a == nu...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,540 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchorX = new double[ANCHOR_Y.length];
        for (int i = 0; i < anchorX.length; i++) {
            anchorX[i] = i;
        }
        runCase(anchorX, ANCHOR_Y, true);

        int cases = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 2) : 0);
        for (int c = 0; c < cases; c++) {
            double[][] generated = buildIncreasingOneSidedGaussian(data);
            if (generated == null) {
                return;
            }
            runCase(generated[0], generated[1], true);
        }
    }

    private static double[][] buildIncreasingOneSidedGaussian(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return null;
        }

        int len = data.consumeInt(3, 40);
        double[] x = new double[len];
        double[] y = new double[len];

        int start = data.consumeInt(-100, 100);
        int step = data.consumeInt(1, 5);
        int extraToMean = data.consumeInt(1, 50);
        double norm = data.consumeInt(1, 1_000_000) / 1000.0;
        double sigma = data.consumeInt(1, 50_000) / 1000.0 + 0.001;

        for (int i = 0; i < len; i++) {
            x[i] = start + ((double) i) * step;
        }

        double mean = x[len - 1] + ((double) extraToMean) * step;
        Gaussian.Parametric g = new Gaussian.Parametric();
        double[] params = new double[] { norm, mean, sigma };

        try {
            for (int i = 0; i < len; i++) {
                y[i] = g.value(x[i], params);
                if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                    return null;
                }
                if (i > 0 && !(y[i] > y[i - 1])) {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }

        return new double[][] { x, y };
    }

    private static void runCase(double[] x, double[] y, boolean validByConstruction) {
        try {
            GaussianFitter crashProbe = newFitterWithPoints(x, y);
            crashProbe.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Oracle: fit() and fit(double[] initialGuess) are documented sibling overloads for the
           same fitting operation, and the patched fit() implementation is supposed to delegate using
           the guessed initial parameters. On the same valid observations, successful executions must
           therefore agree on the returned parameters; a patch that merely suppresses the failure or
           bypasses the intended call path can violate this relation without throwing. */
        try {
            GaussianFitter f1 = newFitterWithPoints(x, y);
            double[] lhs = f1.fit();

            GaussianFitter f2 = newFitterWithPoints(x, y);
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            assertArrayClose(x, y, lhs, rhs);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }
    }

    private static GaussianFitter newFitterWithPoints(double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length && i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        return validByConstruction
            && (t instanceof NotStrictlyPositiveException)
            && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static void assertArrayClose(double[] x, double[] y, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload-len] metamorphic violation: length mismatch inputLen="
                + x.length + " aLen=" + (a == null ? -1 : a.length) + " bLen=" + (b == null ? -1 : b.length));
        }
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isNaN(da) && Double.isNaN(db)) {
                continue;
            }
            if (Double.isInfinite(da) || Double.isInfinite(db)) {
                if (da == db) {
                    continue;
                }
                throw new RuntimeException("[oracle:fit-overload-finite] metamorphic violation: non-finite mismatch idx=" + i
                    + " inputLen=" + x.length + " lhs=" + da + " rhs=" + db);
            }
            double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(da), Math.abs(db)));
            if (Math.abs(da - db) > tol) {
                throw new RuntimeException("[oracle:fit-overload-value] metamorphic violation: fit() != fit(guess) idx=" + i
                    + " inputLen=" + x.length + " firstX=" + x[0] + " lastX=" + x[x.length - 1]
                    + " firstY=" + y[0] + " lastY=" + y[y.length - 1]
                    + " lhs=" + da + " rhs=" + db);
            }
        }
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,772 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (IllegalArgumentException e) {
                boolean throughFit = false;
                for (StackTraceElement ste : e.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (e instanceof NotStrictlyPositiveException && throughFit) {
                    throw e;
                }
            } catch (RuntimeException e) {
                boolean throughFit = false;
                for (StackTraceElement ste : e.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (e instanceof NotStrictlyPositiveException && throughFit) {
                    throw e;
                }
            }
        }

        int cases = 1 + data.consumeInt(0, 3);
        for (int c = 0; c < cases; c++) {
            int len = data.consumeInt(3, anchor.length);
            int start = data.consumeInt(0, anchor.length - len);
            int xStart = data.consumeInt(-20, 20);
            int xStep = data.consumeInt(1, 3);
            double scalePow = data.consumeInt(-6, 6);
            double scale = Math.pow(10.0, scalePow);
            double jitterBase = data.consumeInt(80, 120) / 100.0;

            double[] ys = new double[len];
            for (int i = 0; i < len; i++) {
                double v = anchor[start + i] * scale;
                double jitter = jitterBase;
                if (data.remainingBytes() > 0) {
                    jitter = data.consumeInt(80, 120) / 100.0;
                }
                v *= jitter;
                if (!(v > 0.0) || Double.isInfinite(v) || Double.isNaN(v)) {
                    v = anchor[start + i];
                }
                ys[i] = v;
            }

            GaussianFitter fitter1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                double x = xStart + (double) (i * xStep);
                fitter1.addObservedPoint(x, ys[i]);
                fitter2.addObservedPoint(x, ys[i]);
            }

            double[] lhs;
            try {
                lhs = fitter1.fit();
            } catch (IllegalArgumentException e) {
                boolean throughFit = false;
                for (StackTraceElement ste : e.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (e instanceof NotStrictlyPositiveException && throughFit) {
                    throw e;
                }
                continue;
            } catch (RuntimeException e) {
                boolean throughFit = false;
                for (StackTraceElement ste : e.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (e instanceof NotStrictlyPositiveException && throughFit) {
                    throw e;
                }
                continue;
            }

            double[] guess;
            double[] rhs;
            try {
                /* Contract/oracle: GaussianFitter.fit() is documented/implemented as using
                 * ParameterGuesser(getObservations()).guess() and then delegating to the
                 * sibling overload fit(double[] initialGuess). Therefore, for the same
                 * observations, fit() and fit(guessedInitialParameters) must agree.
                 * A patch that merely deletes/avoids the failing path can silently change
                 * the result of fit() without throwing; this sibling-agreement check catches that.
                 */
                guess = (new GaussianFitter.ParameterGuesser(fitter2.getObservations())).guess();
                rhs = fitter2.fit(guess);
            } catch (Throwable t) {
                continue;
            }

            if (lhs == null || rhs == null || lhs.length != rhs.length) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) length mismatch inputLen="
                        + len + " lhsLen=" + (lhs == null ? -1 : lhs.length) + " rhsLen=" + (rhs == null ? -1 : rhs.length));
            }

            for (int i = 0; i < lhs.length; i++) {
                double a = lhs[i];
                double b = rhs[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b) || Double.isNaN(a) || Double.isNaN(b)) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) disagree inputLen="
                            + len + " index=" + i + " lhs=" + a + " rhs=" + b);
                }
                double tol = Math.max(1.0e-7, 1.0e-7 * Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > tol) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) disagree inputLen="
                            + len + " index=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
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
**output:** **23/23 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov; 23 raw fuzz log(s)**
<details><summary>▸ detail (~11,724 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 2282}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 2282}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 2284}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 2279}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 2287}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 2283}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 2283}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 2288}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 2280}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 2280}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 2280}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 2279}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 2293}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 2286}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 2284}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 2283}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 2282}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029'], 'accepted_attempts': ['attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/010_patch1-Math-58-CapGen_c_rep001/harness_src/attempt_029.java']}

</details>
