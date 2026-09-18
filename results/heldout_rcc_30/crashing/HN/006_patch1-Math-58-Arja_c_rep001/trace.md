# Pipeline trace — Math-58

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
+++ /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
@@ -118,7 +118,7 @@
      */
     public double[] fit() {
         final double[] guess = (new ParameterGuesser(getObservations())).guess();
-        return fit(new Gaussian.Parametric(), guess);
+        return fit((new ParameterGuesser(getObservations())).guess());
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.optimization.fitting.GaussianFitterTest', test_metho (~2,123 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.optimization.fitting.GaussianFitterTest', test_method='testMath519', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/006_patch1-Math-58-Arja_c_rep001/Math_58_buggy/src/test/java/org/apache/commons/math/optimization/fitting/GaussianFitterTest.java', method_source='    @Test\\n    public void testMath519() {\\n        // The optimizer will try negative sigma values but \"GaussianFitter\"\\n        // will catch the raised exceptions and return NaN values instead.\\n\\n        final double[] data = { \\n            1.1143831578403364E-29,\\n            4.95281403484594E-28,\\n            1.1171347211930288E-26,\\n            1.7044813962636277E-25,\\n            1.9784716574832164E-24,\\n            1.8630236407866774E-23,\\n            1.4820532905097742E-22,\\n            1.0241963854632831E-21,\\n            6.275077366673128E-21,\\n            3.461808994532493E-20,\\n            1.7407124684715706E-19,\\n            8.056687953553974E-19,\\n            3.460193945992071E-18,\\n            1.3883326374011525E-17,\\n            5.233894983671116E-17,\\n            1.8630791465263745E-16,\\n            6.288759227922111E-16,\\n            2.0204433920597856E-15,\\n            6.198768938576155E-15,\\n            1.821419346860626E-14,\\n            5.139176445538471E-14,\\n            1.3956427429045787E-13,\\n            3.655705706448139E-13,\\n            9.253753324779779E-13,\\n            2.267636001476696E-12,\\n            5.3880460095836855E-12,\\n            1.2431632654852931E-11\\n        };\\n\\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\\n        for (int i = 0; i < data.length; i++) {\\n            fitter.addObservedPoint(i, data[i]);\\n        }\\n        final double[] p = fitter.fit();\\n\\n        Assert.assertEquals(53.1572792, p[1], 1e-7);\\n        Assert.assertEquals(5.75214622, p[2], 1e-8);\\n    }', exception_type='org.apache.commons.math.exception.NotStrictlyPositiveException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~12,434 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java\n+++ /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java\n@@ -118,7 +118,7 @@\n      */\n     public double[] fit() {\n         final double[] guess = (new ParameterGuesser(getObservations())).guess();\n-        return fit(new Gaussian.Parametric(), guess);\n+        return fit((new ParameterGuesser(getObservations())).guess());\n     }\n \n     /**\n",
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
<details><summary>▸ Prompt (2 message(s), ~19,105 chars)</summary>

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
@@ -118,7 +118,7 @@
      */
     public double[] fit() {
         final double[] guess = (new ParameterGuesser(getObservations())).guess();
-        return fit(new Gaussian.Parametric(), guess);
+        return fit((new ParameterGuesser(getObservations())).guess());
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
<details open><summary>▸ Output (~11,957 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() and GaussianFitter.fit(double[] initialGuess) are same-name overloads
         * documented to perform the fit on the same observations, and the patched implementation of fit()
         * is exactly "return fit((new ParameterGuesser(getObservations())).guess());".
         * Therefore, for any observation set where both calls return normally, fit() must produce the same
         * result as fit(new ParameterGuesser(getObservations()).guess()).
         * A patch that merely deletes/avoids the throwing path but returns a different result would violate this.
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

        // ANCHOR: exact failing test input first.
        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                String cn = t.getClass().getName();
                boolean rootCause = cn.endsWith("NotStrictlyPositiveException");
                boolean cleanRejection =
                        t instanceof IllegalArgumentException
                        || cn.contains(".exception.")
                        || cn.contains("NumberFormat");
                if (rootCause && inFit) {
                    throw t;
                }
                if (cleanRejection) {
                    return;
                }
                // Swallow out-of-scope failures.
            }
        }

        // Post-condition / sibling-overload agreement on the exact anchor input.
        {
            GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitterA.addObservedPoint(i, anchor[i]);
                fitterB.addObservedPoint(i, anchor[i]);
            }
            try {
                double[] lhs = fitterA.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
                double[] rhs = fitterB.fit(guess);
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess()) length/null mismatch");
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
                        same = Math.abs(a - b) <= 1.0e-8 * scale;
                    }
                    if (!same) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) input=anchor idx=" + i + " lhs=" + a + " rhs=" + b);
                    }
                }
            } catch (RuntimeException t) {
                String cn = t.getClass().getName();
                if (cn.startsWith("java.lang.RuntimeException") && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                // If either side rejects/throws, the relation does not apply for this input.
            } catch (Exception t) {
                // The relation does not apply if either side throws.
            }
        }

        // EXPLORE: generate many valid positive observation sets with the same root property:
        // real observations with increasing x and strictly positive y values, including
        // anchor-derived tails and synthetic Gaussian/tail shapes at moderate magnitudes.
        int variants = 1 + data.consumeInt(1, 4);
        for (int v = 0; v < variants; v++) {
            int mode = data.consumeInt(0, 2);
            double[] y;
            double xStart;
            double xStep;

            if (mode == 0) {
                int start = data.consumeInt(0, anchor.length - 3);
                int len = data.consumeInt(3, anchor.length - start);
                y = new double[len];
                double scale = Math.pow(10.0, data.consumeInt(-3, 3));
                for (int i = 0; i < len; i++) {
                    double val = anchor[start + i] * scale;
                    if (val <= 0.0 || Double.isNaN(val) || Double.isInfinite(val)) {
                        val = Math.abs(anchor[start + i]) + 1.0e-300;
                    }
                    y[i] = val;
                }
                xStart = data.consumeInt(-20, 20);
                xStep = data.consumeBoolean() ? 1.0 : data.consumeInt(1, 5);
            } else if (mode == 1) {
                int len = data.consumeInt(3, 40);
                y = new double[len];
                double amp = Math.pow(10.0, data.consumeInt(-6, 6));
                double sigma = data.consumeInt(1, 50);
                double center = data.consumeInt(-20, len + 20);
                for (int i = 0; i < len; i++) {
                    double d = i - center;
                    double val = amp * Math.exp(-(d * d) / (2.0 * sigma * sigma));
                    if (val <= 0.0 || Double.isNaN(val) || Double.isInfinite(val)) {
                        val = 1.0e-300;
                    }
                    y[i] = val;
                }
                xStart = data.consumeInt(-50, 50);
                xStep = data.consumeBoolean() ? 1.0 : data.consumeInt(1, 3);
            } else {
                int len = data.consumeInt(3, 35);
                y = new double[len];
                double scale = Math.pow(10.0, data.consumeInt(-10, 2));
                double growth = 1.05 + (data.consumeInt(0, 50) / 100.0);
                double cur = scale;
                for (int i = 0; i < len; i++) {
                    cur *= growth;
                    if (cur <= 0.0 || Double.isNaN(cur) || Double.isInfinite(cur)) {
                        cur = 1.0e-300 * (i + 1);
                    }
                    y[i] = cur;
                }
                xStart = data.consumeInt(-30, 30);
                xStep = data.consumeBoolean() ? 1.0 : data.consumeInt(1, 4);
            }

            GaussianFitter fitNoGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < y.length; i++) {
                double x = xStart + i * xStep;
                if (Double.isNaN(x) || Double.isInfinite(x)) {
                    x = i;
                }
                double obs = y[i];
                if (!(obs > 0.0) || Double.isNaN(obs) || Double.isInfinite(obs)) {
                    obs = 1.0e-300;
                }
                fitNoGuess.addObservedPoint(x, obs);
                fitWithGuess.addObservedPoint(x, obs);
            }

            try {
                fitNoGuess.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                String cn = t.getClass().getName();
                boolean rootCause = cn.endsWith("NotStrictlyPositiveException");
                boolean cleanRejection =
                        t instanceof IllegalArgumentException
                        || cn.contains(".exception.")
                        || cn.contains("NumberFormat");
                if (rootCause && inFit) {
                    throw t;
                }
                if (cleanRejection) {
                    continue;
                }
                continue;
            }

            try {
                double[] lhs = fitNoGuess.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(fitWithGuess.getObservations())).guess();
                double[] rhs = fitWithGuess.fit(guess);
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess()) length/null mismatch variant=" + v);
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
                        same = Math.abs(a - b) <= 1.0e-8 * scale;
                    }
                    if (!same) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) variant=" + v + " idx=" + i + " lhs=" + a + " rhs=" + b + " len=" + y.length);
                    }
                }
            } catch (RuntimeException t) {
                String cn = t.getClass().getName();
                if (cn.startsWith("java.lang.RuntimeException") && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                // If either side rejects/throws, the relation does not apply for this input.
            } catch (Exception t) {
                // The relation does not apply if either side throws.
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,120 chars)</summary>

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
        runCase(anchor, 0.0, 1.0, true);

        int cases = 1 + data.consumeInt(0, 3);
        for (int c = 0; c < cases; c++) {
            int len = data.consumeInt(5, 40);
            double x0 = data.consumeInt(-50, 50);
            double step = 1.0 + data.consumeInt(0, 4);

            double amplitude = 0.1 + data.consumeInt(0, 1000) / 10.0;
            double sigma = 0.25 + data.consumeInt(0, 400) / 20.0;
            double center = x0 + step * (len - 1 + data.consumeInt(1, 60));
            double baseline = data.consumeBoolean() ? data.consumeInt(0, 10) / 1000.0 : 0.0;
            double noiseScale = data.consumeInt(0, 200) / 10000.0;

            double[] ys = new double[len];
            for (int i = 0; i < len; i++) {
                double x = x0 + step * i;
                double z = (x - center) / sigma;
                double y = amplitude * Math.exp(-0.5 * z * z) + baseline;
                if (noiseScale > 0.0) {
                    double mult = 1.0 + ((double) data.consumeInt(-100, 100)) * noiseScale;
                    if (mult < 0.0) {
                        mult = 0.0;
                    }
                    y *= mult;
                }
                if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = 0.0;
                }
                ys[i] = y;
            }

            boolean reverse = data.consumeBoolean();
            if (reverse) {
                for (int i = 0; i < ys.length / 2; i++) {
                    double tmp = ys[i];
                    ys[i] = ys[ys.length - 1 - i];
                    ys[ys.length - 1 - i] = tmp;
                }
            }

            runCase(ys, x0, step, true);
        }
    }

    private static void runCase(double[] ys, double x0, double step, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(x0 + step * i, ys[i]);
        }

        double[] direct;
        try {
            direct = fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                throwUnchecked(t);
            }
            return;
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter2.addObservedPoint(x0 + step * i, ys[i]);
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitter2.getObservations()).guess();
        } catch (Throwable t) {
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitter2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /* Contract asserted: GaussianFitter.fit() computes ParameterGuesser(...).guess()
           and delegates to the sibling overload fit(double[] initialGuess). Therefore, for
           the same observations, fit() and fit(guess()) must produce the same result.
           A patch that only suppresses the exception or changes the call path incorrectly
           can violate this observable overload-agreement relation without throwing. */
        if (!sameArray(direct, viaGuess)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() vs fit(guess()) inputLength="
                    + ys.length + " lhs=" + arrayToString(direct) + " rhs=" + arrayToString(viaGuess));
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                return true;
            }
        }
        if (!validByConstruction) {
            return false;
        }
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

    private static boolean sameArray(double[] a, double[] b) {
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
            double scale = 1.0 + Math.max(Math.abs(x), Math.abs(y));
            if (diff > 1.0e-8 * scale) {
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
- trigger: org.apache.commons.math.exception.NotStrictlyPositiveException: -1.277 is smaller than, or equal to, the minimum (0)

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,713 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            runExploreAnchorDerived(data);
        } else if (mode == 1) {
            runExploreSyntheticGaussian(data);
        } else {
            runExploreAnchorDerived(data);
            runExploreSyntheticGaussian(data);
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
        }
    }

    private static void runExploreAnchorDerived(FuzzedDataProvider data) {
        int start = data.consumeInt(0, ANCHOR.length - 3);
        int end = data.consumeInt(start + 3, ANCHOR.length);
        int prefix = data.consumeInt(0, 4);
        int suffix = data.consumeInt(0, 4);
        double xStart = data.consumeInt(-1000, 1000);
        double xStep = 1.0 + data.consumeInt(0, 5);
        double scale = pow2(data.consumeInt(-8, 8));
        double floor = pow2(data.consumeInt(-80, -20));

        int n = prefix + (end - start) + suffix;
        double[] xs = new double[n];
        double[] ys = new double[n];

        int k = 0;
        for (int i = 0; i < prefix; i++, k++) {
            xs[k] = xStart + k * xStep;
            ys[k] = floor * (1.0 + i);
        }
        for (int i = start; i < end; i++, k++) {
            xs[k] = xStart + k * xStep;
            ys[k] = clampPositiveFinite(ANCHOR[i] * scale + floor);
        }
        for (int i = 0; i < suffix; i++, k++) {
            xs[k] = xStart + k * xStep;
            ys[k] = clampPositiveFinite(ANCHOR[Math.max(0, end - 1)] * scale + floor * (i + 1));
        }

        runScenario(xs, ys);
    }

    private static void runExploreSyntheticGaussian(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double[] xs = new double[n];
        double[] ys = new double[n];

        double x0 = data.consumeInt(-200, 200);
        double step = 1.0 + data.consumeInt(0, 4);
        double mean = x0 + step * data.consumeInt(0, n - 1);
        double sigma = 0.5 + data.consumeInt(1, 40) / 4.0;
        double amp = 1e-6 + pow2(data.consumeInt(-10, 10));
        double baseline = 1e-18 + pow2(data.consumeInt(-60, -20));
        boolean rightTailOnly = data.consumeBoolean();
        boolean leftTailOnly = data.consumeBoolean();

        int visibleStart = 0;
        int visibleEnd = n;
        if (rightTailOnly) {
            visibleEnd = data.consumeInt(3, n);
            visibleStart = 0;
            mean = x0 + step * (visibleEnd + data.consumeInt(5, 20));
        } else if (leftTailOnly) {
            visibleStart = data.consumeInt(0, n - 3);
            visibleEnd = n;
            mean = x0 - step * (data.consumeInt(5, 20) + (n - visibleStart));
        }

        for (int i = 0; i < n; i++) {
            xs[i] = x0 + i * step;
            double dx = xs[i] - mean;
            double y = amp * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
            if (i < visibleStart || i >= visibleEnd) {
                y = baseline * (1.0 + i);
            }
            ys[i] = clampPositiveFinite(y);
        }

        runScenario(xs, ys);
    }

    private static void runScenario(double[] xs, double[] ys) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            f1.addObservedPoint(xs[i], ys[i]);
            f2.addObservedPoint(xs[i], ys[i]);
        }

        double[] a;
        try {
            a = f1.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] b;
        try {
            b = f2.fit(guess);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
            return;
        }

        /* Contract/oracle: GaussianFitter exposes sibling overloads fit() and fit(double[] initialGuess).
           The no-arg overload computes ParameterGuesser(...).guess() and must therefore be equivalent to
           explicitly computing that guess and calling fit(guess) on the same observations. A patch that
           merely suppresses the exception or routes through the wrong implementation can make these
           overloads diverge even when neither throws. */
        if (!sameParams(a, b)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) xs="
                    + summarize(xs) + " ys=" + summarize(ys)
                    + " lhs=" + summarize(a) + " rhs=" + summarize(b));
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) {
                return false;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-6 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
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

    private static double clampPositiveFinite(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1e-18;
        }
        if (v > 1.0e6) {
            return 1.0e6;
        }
        return v;
    }

    private static double pow2(int e) {
        return Math.scalb(1.0, e);
    }

    private static String summarize(double[] v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int m = Math.min(v.length, 8);
        for (int i = 0; i < m; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        if (v.length > m) {
            sb.append("...len=").append(v.length);
        }
        sb.append(']');
        return sb.toString();
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
<details open><summary>▸ Output (~8,246 chars)</summary>

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
        runAnchor();

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int len = data.consumeInt(3, ANCHOR_DATA.length - start);

        int xShift = data.consumeInt(-1000, 1000);
        int xStep = data.consumeInt(1, 10);

        int scalePow = data.consumeInt(-6, 6);
        double yScale = pow10(scalePow);

        int baselinePow = data.consumeInt(-18, -6);
        double baseline = pow10(baselinePow);

        boolean addJitter = data.consumeBoolean();
        int jitterDiv = data.consumeInt(2, 32);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < len; i++) {
            double x = xShift + (double) i * xStep;
            double y = ANCHOR_DATA[start + i] * yScale + baseline;
            if (addJitter) {
                int signed = data.consumeInt(-1000, 1000);
                double jitter = Math.abs(y) * signed / (1000.0 * jitterDiv);
                y += jitter;
                if (y <= 0.0) {
                    y = baseline > 0.0 ? baseline : 1e-18;
                }
            }
            fitter.addObservedPoint(x, y);
        }

        runFitWithRootCauseFiltering(fitter, true);

        runSiblingAgreementOracle(start, len, xShift, xStep, yScale, baseline, addJitter, jitterDiv, data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        runFitWithRootCauseFiltering(fitter, true);
    }

    private static void runFitWithRootCauseFiltering(GaussianFitter fitter, boolean validByConstruction) {
        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (Error e) {
            if (validByConstruction && isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void runSiblingAgreementOracle(int start, int len, int xShift, int xStep,
                                                  double yScale, double baseline,
                                                  boolean addJitter, int jitterDiv,
                                                  FuzzedDataProvider data) {
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < len; i++) {
            double x = xShift + (double) i * xStep;
            double y = ANCHOR_DATA[start + i] * yScale + baseline;
            if (addJitter) {
                int signed = data.consumeInt(-1000, 1000);
                double jitter = Math.abs(y) * signed / (1000.0 * jitterDiv);
                y += jitter;
                if (y <= 0.0) {
                    y = baseline > 0.0 ? baseline : 1e-18;
                }
            }
            fitterA.addObservedPoint(x, y);
            fitterB.addObservedPoint(x, y);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = fitterA.fit();
        } catch (Throwable t) {
            return;
        }
        try {
            rhs = fitterB.fit(guess);
        } catch (Throwable t) {
            return;
        }

        // Documented sibling-agreement guarantee: fit() computes an initial guess from the
        // current observations and then delegates to the overload taking that initial guess.
        // Therefore, for the same observations and the same guessed parameters, fit() and
        // fit(initialGuess) must agree whenever both calls succeed. A throw-deleting or
        // wrong-delegation patch can violate this without crashing.
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) returned different shapes input=len="
                    + len + " lhs=" + describe(lhs) + " rhs=" + describe(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                return;
            }
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(initialGuess) input=len="
                        + len + ",start=" + start + ",xShift=" + xShift + ",xStep=" + xStep
                        + ",yScale=" + yScale + ",baseline=" + baseline
                        + " lhs=" + describe(lhs) + " rhs=" + describe(rhs));
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!isGroundTruthFamily(t)) {
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

    private static boolean isGroundTruthFamily(Throwable t) {
        Class<?> c = t.getClass();
        while (c != null) {
            String n = c.getName();
            if ("org.apache.commons.math.exception.NotStrictlyPositiveException".equals(n)) {
                return true;
            }
            c = c.getSuperclass();
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

    private static double pow10(int exp) {
        double v = 1.0;
        if (exp >= 0) {
            for (int i = 0; i < exp; i++) {
                v *= 10.0;
            }
        } else {
            for (int i = 0; i < -exp; i++) {
                v /= 10.0;
            }
        }
        return v;
    }

    private static String describe(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
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
<details open><summary>▸ Output (~6,861 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.MathIllegalArgumentException;
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
        runCase(ANCHOR_DATA, true);

        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 1.0 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 1000.0;
        double center = (len - 1) + 0.5 + data.consumeInt(0, 4000) / 200.0;
        double floor = data.consumeBoolean() ? 0.0 : data.consumeInt(0, 100) * 1e-18;

        for (int i = 0; i < len; i++) {
            double dx = i - center;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = floor + amplitude * Math.exp(exponent);
            if (data.consumeBoolean()) {
                y *= 1.0 + (data.consumeInt(0, 20) / 1000.0);
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-300;
            }
            ys[i] = y;
        }

        if (!isStrictlyIncreasing(ys)) {
            for (int i = 1; i < ys.length; i++) {
                if (!(ys[i] > ys[i - 1])) {
                    ys[i] = Math.nextUp(ys[i - 1]);
                }
            }
        }

        runCase(ys, true);
    }

    private static void runCase(double[] ys, boolean validByConstruction) {
        GaussianFitter fitNoArg = buildFitter(ys);
        try {
            fitNoArg.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromFit(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Oracle: the documented same-name overloads fit() and fit(double[] initialGuess)
         * must agree on equivalent inputs. GaussianFitter.fit() computes a guess from the
         * observations and delegates to the overload taking that guess; a patch that merely
         * deletes the throw or changes behavior without preserving delegation will break this.
         * If either side throws, the relation does not apply and we skip it.
         */
        GaussianFitter fitWithGuess = buildFitter(ys);
        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitWithGuess.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = buildFitter(ys).fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromFit(t)) {
                throw t;
            }
            return;
        }
        try {
            rhs = fitWithGuess.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(initialGuess) " +
                "inputLen=" + ys.length +
                " lhs=" + arrayToString(lhs) +
                " rhs=" + arrayToString(rhs)
            );
        }
    }

    private static GaussianFitter buildFitter(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        return fitter;
    }

    private static boolean isRootCauseFromFit(RuntimeException t) {
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

    private static boolean isCleanRejection(RuntimeException t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof MathIllegalArgumentException;
    }

    private static boolean isStrictlyIncreasing(double[] ys) {
        for (int i = 1; i < ys.length; i++) {
            if (!(ys[i] > ys[i - 1])) {
                return false;
            }
        }
        return true;
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
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-8 * scale) {
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
<details open><summary>▸ Output (~6,438 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
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

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchor.length; i++) {
            fitter.addObservedPoint(i, anchor[i]);
        }

        double[] lhs;
        try {
            lhs = fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitter.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        // Contract/oracle: fit() is the no-arg sibling overload that is supposed to use the
        // guessed initial parameters and therefore agree with fit(double[] initialGuess) given
        // that same guessed array. A patch that only suppresses the throw or skips real fitting
        // can violate this observable overload-equivalence without crashing.
        assertEquivalentResults(anchor, lhs, rhs);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double amplitude = positiveScale(data.consumeInt(0, 60));
        double sigma = 0.2 + (data.consumeInt(0, 500) / 25.0);
        double center = (len - 1) + 1.0 + data.consumeInt(0, 30);
        boolean addNoise = data.consumeBoolean();

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = i;
            double exponent = -((x - center) * (x - center)) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent);
            if (addNoise) {
                int tweak = data.consumeInt(0, 20);
                y *= 1.0 + (tweak / 1000.0);
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(1e-300, amplitude * 1e-12);
            }
            ys[i] = y;
            fitter.addObservedPoint(x, y);
        }

        double[] lhs;
        try {
            lhs = fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitter.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        assertEquivalentResults(ys, lhs, rhs);
    }

    private static double positiveScale(int bucket) {
        return Math.pow(10.0, -bucket / 2.0);
    }

    private static boolean isRootCause(RuntimeException t) {
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

    private static void assertEquivalentResults(double[] input, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(guess()) length mismatch inputLen="
                    + (input == null ? -1 : input.length)
                    + " lhsLen=" + (lhs == null ? -1 : lhs.length)
                    + " rhsLen=" + (rhs == null ? -1 : rhs.length));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: non-finite disagreement inputLen="
                        + input.length + " index=" + i + " lhs=" + a + " rhs=" + b);
            }
            double diff = Math.abs(a - b);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (diff > 1.0e-6 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) inputLen="
                        + input.length + " index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
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
<details open><summary>▸ Output (~6,868 chars)</summary>

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
        runExploreIncreasingTail(data);
        runMetamorphicSiblingAgreement(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExploreIncreasingTail(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double v = (data.consumeInt(1, 1000)) / 1_000_000_000.0;
        for (int i = 0; i < len; i++) {
            int num = data.consumeInt(101, 300);
            double factor = num / 100.0;
            v *= factor;
            if (!Double.isFinite(v) || v > 1.0e6) {
                v = 1.0e6;
            }
            ys[i] = v;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runMetamorphicSiblingAgreement(FuzzedDataProvider data) {
        int len = data.consumeInt(5, 30);
        double norm = data.consumeInt(1, 1000) / 10.0;
        double mean = data.consumeInt(-500, 500) / 10.0;
        double sigma = data.consumeInt(1, 200) / 10.0;

        double[] xs = new double[len];
        double[] ys = new double[len];

        double start = mean - 2.0 * sigma;
        double step = (4.0 * sigma) / (len - 1);
        for (int i = 0; i < len; i++) {
            double x = start + i * step;
            double z = (x - mean) / sigma;
            double y = norm * Math.exp(-0.5 * z * z);
            xs[i] = x;
            ys[i] = y;
        }

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < len; i++) {
            fitterA.addObservedPoint(xs[i], ys[i]);
            fitterB.addObservedPoint(xs[i], ys[i]);
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = fitterA.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            rhs = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:sibling-agreement] metamorphic violation: fit() / fit(initialGuess) length mismatch lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            if (!Double.isFinite(lhs[i]) || !Double.isFinite(rhs[i])) {
                throw new RuntimeException("[oracle:sibling-agreement] metamorphic violation: fit() / fit(initialGuess) non-finite lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
            }
            double diff = Math.abs(lhs[i] - rhs[i]);
            double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(lhs[i]), Math.abs(rhs[i])));
            if (diff > tol) {
                throw new RuntimeException("[oracle:sibling-agreement] metamorphic violation: fit() / fit(initialGuess) inputLen=" + len + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
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
        Package p = t.getClass().getPackage();
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.exception.NotStrictlyPositiveException: -1.277 is smaller than, or equal to, the minimum (0)

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,262 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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

        tryFitNoArg(buildXs(anchor.length, 0.0, 1.0), anchor, true);

        int n = data.consumeInt(3, 40);
        double offset = data.consumeInt(-200, 200);
        double step = Math.max(0.25, data.consumeInt(1, 20) / 4.0);
        double amplitude = Math.max(1e-6, data.consumeInt(1, 100000) / 1000.0);
        double sigma = Math.max(0.2, data.consumeInt(1, 200) / 10.0);
        double mean = offset + step * data.consumeInt(0, n - 1);
        double baseline = Math.max(0.0, data.consumeInt(0, 1000) / 1000000.0);
        boolean addShoulder = data.consumeBoolean();
        double shoulderAmp = Math.max(0.0, data.consumeInt(0, 5000) / 100000.0);
        double shoulderSigma = Math.max(0.3, data.consumeInt(1, 100) / 10.0);
        double shoulderMean = mean + step * data.consumeInt(-3, 3);

        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double x = offset + i * step;
            xs[i] = x;
            double z = (x - mean) / sigma;
            double y = baseline + amplitude * Math.exp(-0.5 * z * z);
            if (addShoulder) {
                double z2 = (x - shoulderMean) / shoulderSigma;
                y += shoulderAmp * Math.exp(-0.5 * z2 * z2);
            }
            y *= 1.0 + (data.consumeInt(-20, 20) / 1000.0);
            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline;
            }
            ys[i] = y;
        }

        tryFitNoArg(xs, ys, true);

        GaussianFitter fitterForBoth = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            fitterForBoth.addObservedPoint(xs[i], ys[i]);
        }

        double[] lhs;
        try {
            lhs = fitterForBoth.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] rhs;
        try {
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterForBoth.getObservations())).guess();
            GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                fitterWithGuess.addObservedPoint(xs[i], ys[i]);
            }
            rhs = fitterWithGuess.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            return;
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) disagree on equivalent observations index=" + i + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            double diff = Math.abs(a - b);
            double tol = Math.max(1e-6, 1e-6 * Math.max(Math.abs(a), Math.abs(b)));
            if (!(diff <= tol)) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) disagree on equivalent observations index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static void tryFitNoArg(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
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

    private static double[] buildXs(int n, double start, double step) {
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
        }
        return xs;
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,558 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases = 1 + Math.abs(data.consumeByte()) % 3;
        }

        for (int c = 0; c < cases; c++) {
            int n = boundedInt(data, 5, 30);
            double start = boundedInt(data, -1000, 1000) / 10.0;
            double step = boundedInt(data, 1, 50) / 10.0;
            double amplitude = boundedInt(data, 1, 1000) / 10.0;
            double sigma = boundedInt(data, 1, 200) / 10.0;
            double mean = start + step * boundedInt(data, 0, n - 1) + boundedInt(data, -20, 20) / 10.0;
            double baseline = boundedInt(data, 0, 50) / 1000.0;

            double[] xs = new double[n];
            double[] ys = new double[n];

            for (int i = 0; i < n; i++) {
                xs[i] = start + i * step;
                double dx = xs[i] - mean;
                double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
                ys[i] = y;
            }

            runValidDataset(xs, ys);
        }
    }

    private static void runAnchor() {
        double[] data = {
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

        GaussianFitter fitter = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        for (int i = 0; i < data.length; i++) {
            fitter.addObservedPoint(i, data[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidDataset(double[] xs, double[] ys) {
        GaussianFitter fitterA = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        GaussianFitter fitterB = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );

        for (int i = 0; i < xs.length; i++) {
            fitterA.addObservedPoint(xs[i], ys[i]);
            fitterB.addObservedPoint(xs[i], ys[i]);
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
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

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() is documented/implemented as obtaining
         * new ParameterGuesser(getObservations()).guess() and delegating to
         * the sibling overload fit(double[] initialGuess). Therefore, for the
         * same observations, fit() and fit(guess()) must agree. A patch that
         * merely suppresses the exception or changes fit()'s behavior without
         * preserving that delegation violates this sibling-agreement relation.
         */
        if (!sameArray(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess()) " +
                "inputXs=" + Arrays.toString(xs) +
                " inputYs=" + Arrays.toString(ys) +
                " lhs=" + Arrays.toString(viaNoArg) +
                " rhs=" + Arrays.toString(viaGuess)
            );
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
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
            if (diff > 1.0e-9 * scale) {
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

    private static int boundedInt(FuzzedDataProvider data, int min, int max) {
        if (data.remainingBytes() <= 0) {
            return min;
        }
        return data.consumeInt(min, max);
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 12: `Math.abs(data.consumeByte()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds an

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,122 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases = 1 + Math.abs(data.consumeByte()) % 3;
        }

        for (int c = 0; c < cases; c++) {
            int n = boundedInt(data, 5, 30);
            double start = boundedInt(data, -1000, 1000) / 10.0;
            double step = boundedInt(data, 1, 50) / 10.0;
            double amplitude = boundedInt(data, 1, 1000) / 10.0;
            double sigma = boundedInt(data, 1, 200) / 10.0;
            double mean = start + step * boundedInt(data, 0, n - 1) + boundedInt(data, -20, 20) / 10.0;
            double baseline = boundedInt(data, 0, 50) / 1000.0;

            double[] xs = new double[n];
            double[] ys = new double[n];

            for (int i = 0; i < n; i++) {
                xs[i] = start + i * step;
                double dx = xs[i] - mean;
                double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
                ys[i] = y;
            }

            runValidDataset(xs, ys);
        }
    }

    private static void runAnchor() {
        double[] data = {
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

        GaussianFitter fitter = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        for (int i = 0; i < data.length; i++) {
            fitter.addObservedPoint(i, data[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidDataset(double[] xs, double[] ys) {
        GaussianFitter fitterA = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        GaussianFitter fitterB = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );

        for (int i = 0; i < xs.length; i++) {
            fitterA.addObservedPoint(xs[i], ys[i]);
            fitterB.addObservedPoint(xs[i], ys[i]);
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
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

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() is documented/implemented as obtaining
         * new ParameterGuesser(getObservations()).guess() and delegating to
         * the sibling overload fit(double[] initialGuess). Therefore, for the
         * same observations, fit() and fit(guess()) must agree. A patch that
         * merely suppresses the exception or changes fit()'s behavior without
         * preserving that delegation violates this sibling-agreement relation.
         */
        if (!sameArray(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess()) " +
                "inputXs=" + Arrays.toString(xs) +
                " inputYs=" + Arrays.toString(ys) +
                " lhs=" + Arrays.toString(viaNoArg) +
                " rhs=" + Arrays.toString(viaGuess)
            );
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
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
            if (diff > 1.0e-9 * scale) {
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

    private static int boundedInt(FuzzedDataProvider data, int min, int max) {
        if (data.remainingBytes() <= 0) {
            return min;
        }
        return data.consumeInt(min, max);
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 12: `Math.abs(data.consumeByte()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,525 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases = data.consumeInt(1, 3);
        }

        for (int c = 0; c < cases; c++) {
            int n = boundedInt(data, 5, 30);
            double start = boundedInt(data, -1000, 1000) / 10.0;
            double step = boundedInt(data, 1, 50) / 10.0;
            double amplitude = boundedInt(data, 1, 1000) / 10.0;
            double sigma = boundedInt(data, 1, 200) / 10.0;
            double mean = start + step * boundedInt(data, 0, n - 1) + boundedInt(data, -20, 20) / 10.0;
            double baseline = boundedInt(data, 0, 50) / 1000.0;

            double[] xs = new double[n];
            double[] ys = new double[n];

            for (int i = 0; i < n; i++) {
                xs[i] = start + i * step;
                double dx = xs[i] - mean;
                double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
                ys[i] = y;
            }

            runValidDataset(xs, ys);
        }
    }

    private static void runAnchor() {
        double[] data = {
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

        GaussianFitter fitter = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        for (int i = 0; i < data.length; i++) {
            fitter.addObservedPoint(i, data[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidDataset(double[] xs, double[] ys) {
        GaussianFitter fitterA = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        GaussianFitter fitterB = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );

        for (int i = 0; i < xs.length; i++) {
            fitterA.addObservedPoint(xs[i], ys[i]);
            fitterB.addObservedPoint(xs[i], ys[i]);
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
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

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() is implemented as computing
         * new ParameterGuesser(getObservations()).guess() and delegating to
         * the sibling overload fit(double[] initialGuess). Therefore, for the
         * same observations, fit() and fit(guess()) must agree. A patch that
         * suppresses the exception or changes fit()'s behavior without
         * preserving that delegation violates this sibling-agreement relation.
         */
        if (!sameArray(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess()) " +
                "inputXs=" + Arrays.toString(xs) +
                " inputYs=" + Arrays.toString(ys) +
                " lhs=" + Arrays.toString(viaNoArg) +
                " rhs=" + Arrays.toString(viaGuess)
            );
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
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
            if (diff > 1.0e-9 * scale) {
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

    private static int boundedInt(FuzzedDataProvider data, int min, int max) {
        if (data.remainingBytes() <= 0) {
            return min;
        }
        return data.consumeInt(min, max);
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
<details open><summary>▸ Output (~8,611 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
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
        exerciseAnchor();

        int mode = data.consumeInt(0, 3);
        double[] ys;
        double xStart = data.consumeInt(-1000, 1000);
        double xStep = Math.max(1.0, data.consumeInt(1, 5));

        if (mode == 0) {
            ys = mutateAnchor(data);
        } else {
            ys = buildPositiveBellLikeSeries(data);
        }

        if (ys == null || ys.length < 3) {
            return;
        }

        exerciseFitOnSeries(xStart, xStep, ys, true);
        checkSiblingAgreement(xStart, xStep, ys);
    }

    private static void exerciseAnchor() {
        exerciseFitOnSeries(0.0, 1.0, ANCHOR_Y, true);
        checkSiblingAgreement(0.0, 1.0, ANCHOR_Y);
    }

    private static void exerciseFitOnSeries(double xStart, double xStep, double[] ys, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xStart + i * xStep, ys[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCauseFromFit(t)) {
                throwAsUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkSiblingAgreement(double xStart, double xStep, double[] ys) {
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xStart + i * xStep;
            fitterA.addObservedPoint(x, ys[i]);
            fitterB.addObservedPoint(x, ys[i]);
        }

        final double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
        } catch (Throwable t) {
            return;
        }

        final double[] lhs;
        final double[] rhs;
        try {
            lhs = fitterA.fit();
        } catch (Throwable t) {
            if (isRootCauseFromFit(t)) {
                throwAsUnchecked(t);
            }
            return;
        }
        try {
            rhs = fitterB.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() / fit(initialGuess) length mismatch input="
                + Arrays.toString(ys) + " lhs=" + Arrays.toString(lhs) + " rhs=" + Arrays.toString(rhs));
        }

        /* Contract/oracle: fit() is the no-arg overload whose implementation is documented by the code under patch:
           it computes ParameterGuesser(...).guess() and then delegates to fit(initialGuess). Therefore, for the same
           observations and the exact same guessed initial parameters, both overloads must produce the same result or
           both reject. A patch that merely deletes/avoids the throwing path can violate this without crashing. */
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > 1e-9 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() should agree with fit(initialGuess) input="
                    + Arrays.toString(ys) + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs) + " rhs=" + Arrays.toString(rhs));
            }
        }
    }

    private static double[] mutateAnchor(FuzzedDataProvider data) {
        int extraLeft = data.consumeInt(0, 5);
        int extraRight = data.consumeInt(0, 5);
        int len = extraLeft + ANCHOR_Y.length + extraRight;
        double[] ys = new double[len];

        for (int i = 0; i < extraLeft; i++) {
            ys[i] = positiveNoise(data, i + 1);
        }
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            double base = ANCHOR_Y[i];
            int tweak = data.consumeInt(-20, 20);
            double factor = 1.0 + (tweak / 1000.0);
            double v = base * factor;
            ys[extraLeft + i] = ensurePositiveFinite(v, base);
        }
        for (int i = 0; i < extraRight; i++) {
            ys[extraLeft + ANCHOR_Y.length + i] = positiveNoise(data, i + 17);
        }
        return ys;
    }

    private static double[] buildPositiveBellLikeSeries(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double mean = data.consumeInt(0, len - 1);
        double sigma = 0.5 + data.consumeInt(1, 200) / 20.0;
        double baseline = data.consumeInt(0, 1000) / 1_000_000.0;

        for (int i = 0; i < len; i++) {
            double dx = (i - mean) / sigma;
            double gaussian = amplitude * Math.exp(-0.5 * dx * dx);
            double noise = data.consumeInt(0, 1000) / 1_000_000.0;
            ys[i] = ensurePositiveFinite(baseline + gaussian + noise, 1e-12);
        }

        if (data.consumeBoolean()) {
            int idx = data.consumeInt(0, len - 1);
            ys[idx] = ensurePositiveFinite(ys[idx] * (1.0 + data.consumeInt(0, 100) / 1000.0), ys[idx]);
        }

        return ys;
    }

    private static double positiveNoise(FuzzedDataProvider data, int salt) {
        double exp = data.consumeInt(-30, -3);
        double mant = 1.0 + ((salt + data.consumeInt(0, 999)) / 1000.0);
        return ensurePositiveFinite(mant * Math.pow(10.0, exp), 1e-12);
    }

    private static double ensurePositiveFinite(double v, double fallback) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return fallback > 0.0 ? fallback : 1e-12;
        }
        return v;
    }

    private static boolean isRootCauseFromFit(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                && "fit".equals(st[i].getMethodName())) {
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
<details open><summary>▸ Output (~9,331 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class H {
            GaussianFitter newFitter(double[] y, double xStart, double xStep) {
                GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < y.length; i++) {
                    fitter.addObservedPoint(xStart + i * xStep, y[i]);
                }
                return fitter;
            }

            boolean hasFitFrame(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                            && "fit".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isValidation(Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                String n = t.getClass().getName();
                return n.startsWith("org.apache.commons.math.exception.");
            }

            boolean isRootCause(Throwable t) {
                return t instanceof NotStrictlyPositiveException && hasFitFrame(t);
            }

            boolean sameArray(double[] a, double[] b) {
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
                    if (Double.doubleToLongBits(x) != Double.doubleToLongBits(y)) {
                        return false;
                    }
                }
                return true;
            }

            void runAnchor() {
                double[] y = new double[] {
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
                GaussianFitter fitter = newFitter(y, 0.0, 1.0);
                try {
                    fitter.fit();
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                }

                // Documented sibling-agreement oracle: fit() computes a ParameterGuesser-derived
                // initial guess and then must behave like fit(double[] initialGuess) on that same
                // guess. The patch changes exactly this delegation. A "fix" that merely suppresses
                // the throw or skips the real fitting path can violate this relation without
                // crashing, so we assert agreement on the real API.
                try {
                    GaussianFitter f1 = newFitter(y, 0.0, 1.0);
                    double[] r1 = f1.fit();

                    GaussianFitter f2 = newFitter(y, 0.0, 1.0);
                    double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
                    double[] r2 = f2.fit(guess);

                    if (!sameArray(r1, r2)) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input=anchor lhs="
                                + java.util.Arrays.toString(r1) + " rhs=" + java.util.Arrays.toString(r2));
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                }
            }

            void runExplore(double[] y, double xStart, double xStep) {
                try {
                    GaussianFitter fitter = newFitter(y, xStart, xStep);
                    fitter.fit();
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (isValidation(t)) {
                        return;
                    }
                    return;
                }

                // Same documented sibling-agreement oracle as above, applied to fuzz-generated
                // valid-by-construction positive Gaussian-tail observations.
                try {
                    GaussianFitter f1 = newFitter(y, xStart, xStep);
                    double[] r1 = f1.fit();

                    GaussianFitter f2 = newFitter(y, xStart, xStep);
                    double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
                    double[] r2 = f2.fit(guess);

                    if (!sameArray(r1, r2)) {
                        throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input="
                                + java.util.Arrays.toString(y) + " lhs=" + java.util.Arrays.toString(r1)
                                + " rhs=" + java.util.Arrays.toString(r2));
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                }
            }

            void throwUnchecked(Throwable t) {
                H.<RuntimeException>rethrow(t);
            }

            @SuppressWarnings("unchecked")
            <E extends Throwable> void rethrow(Throwable t) throws E {
                throw (E) t;
            }
        }

        H h = new H();

        h.runAnchor();

        int len = data.consumeInt(3, 40);
        double[] y = new double[len];

        int ampNum = data.consumeInt(1, 1000);
        int ampScale = data.consumeInt(0, 6);
        double amplitude = ampNum / Math.pow(10.0, ampScale);

        int meanExtra = data.consumeInt(5, 80);
        int sigmaInt = data.consumeInt(1, 20);
        int sigmaFrac = data.consumeInt(0, 999);
        double sigma = sigmaInt + (sigmaFrac / 1000.0);

        double xStep = data.consumeBoolean() ? 1.0 : (data.consumeInt(1, 10) / 10.0);
        double xStart = data.consumeInt(-50, 50);
        double mean = xStart + (len - 1) * xStep + meanExtra;

        boolean addFloor = data.consumeBoolean();
        double floor = addFloor ? Math.pow(10.0, -data.consumeInt(12, 30)) : 0.0;

        for (int i = 0; i < len; i++) {
            double x = xStart + i * xStep;
            double z = (x - mean) / sigma;
            double v = amplitude * Math.exp(-0.5 * z * z) + floor;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                return;
            }
            y[i] = v;
        }

        h.runExplore(y, xStart, xStep);

        if (data.remainingBytes() > 0) {
            int trimLeft = data.consumeInt(0, Math.max(0, len - 3));
            int trimRight = data.consumeInt(0, Math.max(0, len - trimLeft - 3));
            int newLen = len - trimLeft - trimRight;
            if (newLen >= 3) {
                double[] y2 = new double[newLen];
                for (int i = 0; i < newLen; i++) {
                    y2[i] = y[trimLeft + i];
                }
                h.runExplore(y2, xStart + trimLeft * xStep, xStep);
            }
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
<details><summary>▸ Prompt (4 message(s), ~10,717 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class H {
            GaussianFitter newFitter(double[] y, double xStart, double xStep) {
                GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < y.length; i++) {
                    fitter.addObservedPoint(xStart + i * xStep, y[i]);
                }
                return fitter;
            }

            boolean hasFitFrame(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                            && "fit".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isValidation(Throwable t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                String n = t.getClass().getName();
                return n.startsWith("org.apache.commons.math.exception.");
            }

            boolean isRootCause(Throwable t) {
                return t instanceof NotStrictlyPositiveException && hasFitFrame(t);
            }

            boolean sameArray(double[] a, double[] b) {
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
                    if (Double.doubleToLongBits(x) != Double.doubleToLongBits(y)) {
                        return false;
                    }
                }
                return true;
            }

            void runAnchor() {
                double[] y = new double[] {
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
                GaussianFitter fitter = newFitter(y, 0.0, 1.0);
                try {
                    fitter.fit();
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                }

                // Documented sibling-agreement oracle: fit() computes a ParameterGuesser-derived
                // initial guess and then must behave like fit(double[] initialGuess) on that same
                // guess. The patch changes exactly this delegation. A "fix" that merely suppresses
                // the throw or skips the real fitting path can violate this relation without
                // crashing, so we assert agreement on the real API.
                try {
                    GaussianFitter f1 = newFitter(y, 0.0, 1.0);
                    double[] r1 = f1.fit();

                    GaussianFitter f2 = newFitter(y, 0.0, 1.0);
                    double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
                    double[] r2 = f2.fit(guess);

                    if (!sameArray(r1, r2)) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input=anchor lhs="
                                + java.util.Arrays.toString(r1) + " rhs=" + java.util.Arrays.toString(r2));
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                }
            }

            void runExplore(double[] y, double xStart, double xStep) {
                try {
                    GaussianFitter fitter = newFitter(y, xStart, xStep);
                    fitter.fit();
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (isValidation(t)) {
                        return;
                    }
                    return;
                }

                // Same documented sibling-agreement oracle as above, applied to fuzz-generated
                // valid-by-construction positive Gaussian-tail observations.
                try {
                    GaussianFitter f1 = newFitter(y, xStart, xStep);
                    double[] r1 = f1.fit();

                    GaussianFitter f2 = newFitter(y, xStart, xStep);
                    double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
                    double[] r2 = f2.fit(guess);

                    if (!sameArray(r1, r2)) {
                        throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input="
                                + java.util.Arrays.toString(y) + " lhs=" + java.util.Arrays.toString(r1)
                                + " rhs=" + java.util.Arrays.toString(r2));
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                }
            }

            void throwUnchecked(Throwable t) {
                H.<RuntimeException>rethrow(t);
            }

            @SuppressWarnings("unchecked")
            <E extends Throwable> void rethrow(Throwable t) throws E {
                throw (E) t;
            }
        }

        H h = new H();

        h.runAnchor();

        int len = data.consumeInt(3, 40);
        double[] y = new double[len];

        int ampNum = data.consumeInt(1, 1000);
        int ampScale = data.consumeInt(0, 6);
        double amplitude = ampNum / Math.pow(10.0, ampScale);

        int meanExtra = data.consumeInt(5, 80);
        int sigmaInt = data.consumeInt(1, 20);
        int sigmaFrac = data.consumeInt(0, 999);
        double sigma = sigmaInt + (sigmaFrac / 1000.0);

        double xStep = data.consumeBoolean() ? 1.0 : (data.consumeInt(1, 10) / 10.0);
        double xStart = data.consumeInt(-50, 50);
        double mean = xStart + (len - 1) * xStep + meanExtra;

        boolean addFloor = data.consumeBoolean();
        double floor = addFloor ? Math.pow(10.0, -data.consumeInt(12, 30)) : 0.0;

        for (int i = 0; i < len; i++) {
            double x = xStart + i * xStep;
            double z = (x - mean) / sigma;
            double v = amplitude * Math.exp(-0.5 * z * z) + floor;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                return;
            }
            y[i] = v;
        }

        h.runExplore(y, xStart, xStep);

        if (data.remainingBytes() > 0) {
            int trimLeft = data.consumeInt(0, Math.max(0, len - 3));
            int trimRight = data.consumeInt(0, Math.max(0, len - trimLeft - 3));
            int newLen = len - trimLeft - trimRight;
            if (newLen >= 3) {
                double[] y2 = new double[newLen];
                for (int i = 0; i < newLen; i++) {
                    y2[i] = y[trimLeft + i];
                }
                h.runExplore(y2, xStart + trimLeft * xStep, xStep);
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/006_patch1-Math-58-Arja_c_rep001/Math_58_buggy/fuzz/attempt_012/FuzzHarness.java:166: error: non-static method <E>rethrow(Throwable) cannot be referenced from a static context
                H.<RuntimeException>rethrow(t);
                 ^
  where E is a type-variable:
    E extends Throwable declared in method <E>rethrow(Throwable)
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
<details open><summary>▸ Output (~7,666 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 40);
        double[] y = new double[len];

        int ampNum = data.consumeInt(1, 1000);
        int ampScale = data.consumeInt(0, 6);
        double amplitude = ampNum / Math.pow(10.0, ampScale);

        int meanExtra = data.consumeInt(5, 80);
        int sigmaInt = data.consumeInt(1, 20);
        int sigmaFrac = data.consumeInt(0, 999);
        double sigma = sigmaInt + (sigmaFrac / 1000.0);

        double xStep = data.consumeBoolean() ? 1.0 : (data.consumeInt(1, 10) / 10.0);
        double xStart = data.consumeInt(-50, 50);
        double mean = xStart + (len - 1) * xStep + meanExtra;

        boolean addFloor = data.consumeBoolean();
        double floor = addFloor ? Math.pow(10.0, -data.consumeInt(12, 30)) : 0.0;

        for (int i = 0; i < len; i++) {
            double x = xStart + i * xStep;
            double z = (x - mean) / sigma;
            double v = amplitude * Math.exp(-0.5 * z * z) + floor;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                return;
            }
            y[i] = v;
        }

        runExplore(y, xStart, xStep);

        if (data.remainingBytes() > 0) {
            int trimLeft = data.consumeInt(0, Math.max(0, len - 3));
            int trimRight = data.consumeInt(0, Math.max(0, len - trimLeft - 3));
            int newLen = len - trimLeft - trimRight;
            if (newLen >= 3) {
                double[] y2 = new double[newLen];
                for (int i = 0; i < newLen; i++) {
                    y2[i] = y[trimLeft + i];
                }
                runExplore(y2, xStart + trimLeft * xStep, xStep);
            }
        }
    }

    private static void runAnchor() {
        double[] y = new double[] {
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
        GaussianFitter fitter = newFitter(y, 0.0, 1.0);
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        try {
            GaussianFitter f1 = newFitter(y, 0.0, 1.0);
            double[] r1 = f1.fit();

            GaussianFitter f2 = newFitter(y, 0.0, 1.0);
            double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
            double[] r2 = f2.fit(guess);

            if (!sameArray(r1, r2)) {
                throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input=anchor lhs="
                        + java.util.Arrays.toString(r1) + " rhs=" + java.util.Arrays.toString(r2));
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runExplore(double[] y, double xStart, double xStep) {
        try {
            GaussianFitter fitter = newFitter(y, xStart, xStep);
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isValidation(t)) {
                return;
            }
            return;
        }

        try {
            GaussianFitter f1 = newFitter(y, xStart, xStep);
            double[] r1 = f1.fit();

            GaussianFitter f2 = newFitter(y, xStart, xStep);
            double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
            double[] r2 = f2.fit(guess);

            if (!sameArray(r1, r2)) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input="
                        + java.util.Arrays.toString(y) + " lhs=" + java.util.Arrays.toString(r1)
                        + " rhs=" + java.util.Arrays.toString(r2));
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static GaussianFitter newFitter(double[] y, double xStart, double xStep) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(xStart + i * xStep, y[i]);
        }
        return fitter;
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof NotStrictlyPositiveException && hasFitFrame(t);
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
            if (Double.doubleToLongBits(x) != Double.doubleToLongBits(y)) {
                return false;
            }
        }
        return true;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>rethrowUnchecked(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void rethrowUnchecked(Throwable t) throws E {
        throw (E) t;
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
<details open><summary>▸ Output (~10,641 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        runScenario(buildAnchorX(), ANCHOR_Y, true);

        int mode = data.consumeInt(0, 3);
        double[] y;
        double[] x;

        if (mode == 0) {
            y = mutateAnchorData(data);
            x = sequentialX(y.length, data.consumeInt(-20, 20), positiveStep(data));
        } else if (mode == 1) {
            y = buildPositiveData(data);
            x = sequentialX(y.length, data.consumeInt(-50, 50), positiveStep(data));
        } else if (mode == 2) {
            y = prefixOrSuffixAroundAnchor(data);
            x = sequentialX(y.length, data.consumeInt(-30, 30), positiveStep(data));
        } else {
            y = scaledAnchorWindow(data);
            x = sequentialX(y.length, data.consumeInt(-10, 10), positiveStep(data));
        }

        runScenario(x, y, true);
    }

    private static void runScenario(double[] x, double[] y, boolean validByConstruction) {
        if (x == null || y == null || x.length != y.length || y.length < 3) {
            return;
        }

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitterA.addObservedPoint(x[i], y[i]);
        }

        double[] fitNoArg;
        try {
            fitNoArg = fitterA.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
            return;
        }

        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitterB.addObservedPoint(x[i], y[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        final double[] fitWithGuess;
        try {
            fitWithGuess = fitterB.fit(guess);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        /* Contract/oracle:
           GaussianFitter.fit() is documented/implemented as the overload that computes an initial
           guess from the observations and then delegates to fit(double[] initialGuess). Therefore,
           for the same observations and the same guess produced by the real ParameterGuesser,
           fit() and fit(guess) must agree. A patch that merely suppresses the crash or changes
           the path without preserving the delegation semantics will violate this sibling-agreement. */
        if (!sameResult(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:sibling-fit] metamorphic violation: fit() must agree with fit(guess())"
                    + " xLen=" + x.length
                    + " yLen=" + y.length
                    + " lhs=" + render(fitNoArg)
                    + " rhs=" + render(fitWithGuess)
            );
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String pn = p.getName();
            if ("org.apache.commons.math.exception".equals(pn)
                    || pn.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
    }

    private static double[] buildAnchorX() {
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        return x;
    }

    private static double positiveStep(FuzzedDataProvider data) {
        return 1.0 + data.consumeInt(0, 4);
    }

    private static double[] sequentialX(int len, int start, double step) {
        double[] x = new double[len];
        double v = start;
        for (int i = 0; i < len; i++) {
            x[i] = v;
            v += step;
        }
        return x;
    }

    private static double[] mutateAnchorData(FuzzedDataProvider data) {
        double scale = pow2(data.consumeInt(-3, 3));
        int len = ANCHOR_Y.length;
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double m = 1.0 + (data.consumeInt(-200, 200) / 1000.0);
            double v = ANCHOR_Y[i] * scale * m;
            y[i] = positive(v);
        }
        return y;
    }

    private static double[] prefixOrSuffixAroundAnchor(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 8);
        int suffix = data.consumeInt(0, 8);
        double[] y = new double[prefix + ANCHOR_Y.length + suffix];
        int idx = 0;
        for (int i = 0; i < prefix; i++) {
            y[idx++] = tinyPositive(data, i + 1);
        }
        double scale = pow2(data.consumeInt(-2, 2));
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            double tweak = 1.0 + (data.consumeInt(-100, 100) / 2000.0);
            y[idx++] = positive(ANCHOR_Y[i] * scale * tweak);
        }
        for (int i = 0; i < suffix; i++) {
            y[idx++] = tinyPositive(data, prefix + i + 1);
        }
        return y;
    }

    private static double[] scaledAnchorWindow(FuzzedDataProvider data) {
        int start = data.consumeInt(0, ANCHOR_Y.length - 3);
        int end = data.consumeInt(start + 2, ANCHOR_Y.length - 1);
        double[] y = new double[end - start + 1];
        double scale = pow2(data.consumeInt(-4, 4));
        for (int i = 0; i < y.length; i++) {
            double tweak = 1.0 + (data.consumeInt(-150, 150) / 1500.0);
            y[i] = positive(ANCHOR_Y[start + i] * scale * tweak);
        }
        return y;
    }

    private static double[] buildPositiveData(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] y = new double[len];

        int center = data.consumeInt(0, len - 1);
        double amplitude = 0.01 + (data.consumeInt(1, 1000) / 10.0);
        double sigma = 0.5 + (data.consumeInt(1, 200) / 20.0);
        double floor = 1e-18 * (1 + data.consumeInt(0, 100));

        for (int i = 0; i < len; i++) {
            double dx = i - center;
            double gaussian = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noiseFactor = 1.0 + (data.consumeInt(-200, 200) / 1000.0);
            y[i] = positive((gaussian + floor) * noiseFactor);
        }
        return y;
    }

    private static double tinyPositive(FuzzedDataProvider data, int salt) {
        int exp = data.consumeInt(6, 18);
        return (salt + 1) * Math.pow(10.0, -exp);
    }

    private static double positive(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1e-300;
        }
        if (v > 1_000_000.0) {
            return 1_000_000.0;
        }
        return v;
    }

    private static double pow2(int e) {
        return Math.pow(2.0, e);
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
            double tol = 1e-8 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > tol) {
                return false;
            }
        }
        return true;
    }

    private static String render(double[] a) {
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
<details open><summary>▸ Output (~9,904 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Utils {
            boolean stackHasGaussianFitterFit(Throwable t) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isMathValidation(Throwable t) {
                if (t == null) {
                    return false;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                Package p = t.getClass().getPackage();
                String pkg = p == null ? "" : p.getName();
                return pkg.startsWith("org.apache.commons.math.exception");
            }

            boolean isGroundTruth(Throwable t) {
                return t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            }

            void maybePropagate(Throwable t, boolean validByConstruction) {
                if (t == null) {
                    return;
                }
                if (isGroundTruth(t) && stackHasGaussianFitterFit(t) && validByConstruction) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new RuntimeException(t);
                }
            }

            GaussianFitter buildFitter(double[] ys, double startX, double step) {
                GaussianFitter fitter =
                        new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
                for (int i = 0; i < ys.length; i++) {
                    fitter.addObservedPoint(startX + i * step, ys[i]);
                }
                return fitter;
            }

            void assertOverloadAgreement(GaussianFitter fitter, String inputDesc) {
                /*
                 * Contract/oracle: fit() and fit(double[] initialGuess) are same-name overloads whose
                 * documented behavior agrees when initialGuess is the guess that fit() itself derives.
                 * A correct implementation of fit() is therefore equivalent to:
                 *   guess = new ParameterGuesser(getObservations()).guess();
                 *   fit(guess)
                 * The patch changes exactly that delegation. If a patch merely deletes the throw or
                 * changes control flow to avoid the crashing branch while returning a different result,
                 * this overload-agreement oracle detects it.
                 *
                 * Hygiene: if either side throws, this relation does not apply for that input and we skip.
                 */
                final double[] guess;
                try {
                    guess = (new GaussianFitter.ParameterGuesser(fitter.getObservations())).guess();
                } catch (RuntimeException e) {
                    if (!isMathValidation(e)) {
                        maybePropagate(e, false);
                    }
                    return;
                }

                final double[] lhs;
                try {
                    lhs = fitter.fit();
                } catch (RuntimeException e) {
                    if (isMathValidation(e)) {
                        maybePropagate(e, true);
                    }
                    return;
                }

                final double[] rhs;
                try {
                    rhs = fitter.fit(guess);
                } catch (RuntimeException e) {
                    if (isMathValidation(e)) {
                        maybePropagate(e, true);
                    }
                    return;
                }

                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException(
                            "[oracle:fit-overload] metamorphic violation: fit() and fit(guess) returned different shapes input="
                                    + inputDesc
                                    + " lhsLen=" + (lhs == null ? -1 : lhs.length)
                                    + " rhsLen=" + (rhs == null ? -1 : rhs.length));
                }

                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    boolean sameNaN = Double.isNaN(a) && Double.isNaN(b);
                    boolean sameInf = Double.isInfinite(a) || Double.isInfinite(b);
                    if (sameNaN) {
                        continue;
                    }
                    if (sameInf) {
                        if (a != b) {
                            throw new RuntimeException(
                                    "[oracle:fit-overload] metamorphic violation: fit() and fit(guess) disagree input="
                                            + inputDesc + " idx=" + i + " lhs=" + a + " rhs=" + b);
                        }
                        continue;
                    }
                    double tol = 1.0e-10 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (Math.abs(a - b) > tol) {
                        throw new RuntimeException(
                                "[oracle:fit-overload] metamorphic violation: fit() and fit(guess) differ input="
                                        + inputDesc + " idx=" + i + " lhs=" + a + " rhs=" + b + " tol=" + tol);
                    }
                }
            }
        }

        Utils u = new Utils();

        // ANCHOR: exact regression input from GaussianFitterTest.testMath519.
        {
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
            GaussianFitter anchorFitter = u.buildFitter(anchorY, 0.0, 1.0);
            try {
                anchorFitter.fit();
            } catch (RuntimeException e) {
                if (u.isMathValidation(e)) {
                    u.maybePropagate(e, true);
                    return;
                }
                return;
            }
            u.assertOverloadAgreement(anchorFitter, "anchor");
        }

        // EXPLORE: valid-by-construction positive bell-shaped datasets with varying length/position/scale.
        int len = data.consumeInt(3, 40);
        int centerTicks = data.consumeInt(-20, len * 20 + 20);
        int sigmaTicks = data.consumeInt(5, 200);
        int ampTicks = data.consumeInt(1, 10000);
        int baseTicks = data.consumeInt(0, 1000);
        int stepTicks = data.consumeInt(1, 30);
        int skewTicks = data.consumeInt(-200, 200);
        int rippleTicks = data.consumeInt(0, 500);

        double center = centerTicks / 10.0;
        double sigma = sigmaTicks / 10.0;
        double amplitude = ampTicks / 10.0;
        double baseline = baseTicks / 1000.0;
        double step = stepTicks / 10.0;
        double skew = skewTicks / 1000.0;
        double ripple = rippleTicks / 10000.0;

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = i * step;
            double dx = x - center;
            double gaussian = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double asymmetric = 1.0 + skew * dx;
            if (asymmetric < 0.1) {
                asymmetric = 0.1;
            }
            double wave = 1.0 + ripple * Math.cos(i);
            if (wave < 0.1) {
                wave = 0.1;
            }
            double y = baseline + gaussian * asymmetric * wave;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + Math.abs(gaussian) + 1.0e-9;
            }
            ys[i] = y;
        }

        GaussianFitter fuzzFitter = u.buildFitter(ys, 0.0, step);
        try {
            fuzzFitter.fit();
        } catch (RuntimeException e) {
            if (u.isMathValidation(e)) {
                u.maybePropagate(e, true);
                return;
            }
            return;
        }

        u.assertOverloadAgreement(
                fuzzFitter,
                "len=" + len
                        + ",center=" + center
                        + ",sigma=" + sigma
                        + ",amp=" + amplitude
                        + ",base=" + baseline
                        + ",step=" + step
                        + ",skew=" + skew
                        + ",ripple=" + ripple);
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
<details open><summary>▸ Output (~6,706 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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
        exerciseValidInput(ANCHOR);

        int rounds = 1 + Math.min(4, Math.max(0, data.remainingBytes() / 8));
        for (int i = 0; i < rounds; i++) {
            double[] ys = data.consumeBoolean() ? generateTailData(data) : mutateAnchor(data);
            exerciseValidInput(ys);
        }
    }

    private static void exerciseValidInput(double[] ys) {
        double[] fitNoArg;
        try {
            fitNoArg = runFitNoArg(ys);
        } catch (Throwable t) {
            if (isRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] fitWithGuess;
        try {
            fitWithGuess = runFitWithGuessedInitialGuess(ys);
        } catch (Throwable t) {
            if (isRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        /* Oracle: fit() and fit(double[] initialGuess) are documented sibling overloads with the
         * same fitting contract; fit() computes the initial guess from ParameterGuesser and should
         * therefore be observationally equivalent to explicitly passing that same guessed array into
         * fit(double[]). A "fix" that merely suppresses the throw or skips the real fitting path
         * would break this relation even when no exception fires.
         */
        if (!sameParams(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) input="
                    + java.util.Arrays.toString(ys)
                    + " lhs=" + java.util.Arrays.toString(fitNoArg)
                    + " rhs=" + java.util.Arrays.toString(fitWithGuess));
        }
    }

    private static double[] runFitNoArg(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        return fitter.fit();
    }

    private static double[] runFitWithGuessedInitialGuess(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        return fitter.fit(guess);
    }

    private static double[] mutateAnchor(FuzzedDataProvider data) {
        int len = 3 + data.consumeInt(0, ANCHOR.length + 10);
        double[] ys = new double[len];
        int offset = data.consumeInt(0, Math.max(0, ANCHOR.length - 1));
        double scale = pow10(data.consumeInt(-3, 3));
        for (int i = 0; i < len; i++) {
            double base = ANCHOR[(offset + i) % ANCHOR.length];
            double tweak = 0.75 + (data.consumeInt(0, 1000) / 1000.0) * 0.5;
            double y = base * scale * tweak;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Double.MIN_VALUE;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double[] generateTailData(FuzzedDataProvider data) {
        int len = data.consumeInt(5, 40);
        double[] ys = new double[len];
        double amplitude = pow10(data.consumeInt(-6, 6));
        double sigma = data.consumeInt(1, 200) / 10.0;
        double mean = len + data.consumeInt(1, len * 3 + 5);
        for (int i = 0; i < len; i++) {
            double dx = i - mean;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent);
            double noise = 0.9 + (data.consumeInt(0, 1000) / 1000.0) * 0.2;
            y *= noise;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Double.MIN_VALUE;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double pow10(int exp) {
        return Math.pow(10.0, exp);
    }

    private static boolean sameParams(double[] a, double[] b) {
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
            if (diff > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction || t == null) {
            return false;
        }
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,020 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
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

        int exploreCount = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < exploreCount; i++) {
            double[] ys = buildPositiveTailDataset(data);
            runOneDataset(ys);
        }
    }

    private static void runAnchor() {
        runOneDataset(ANCHOR_DATA);
    }

    private static void runOneDataset(double[] ys) {
        if (ys == null || ys.length < 3) {
            return;
        }

        try {
            GaussianFitter fitter = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < ys.length; i++) {
                if (!isUsableFinitePositive(ys[i])) {
                    return;
                }
                fitter.addObservedPoint((double) i, ys[i]);
            }

            double[] params = fitter.fit();
            if (params == null || params.length != 3) {
                throw new RuntimeException("[oracle:fit-shape] metamorphic violation: fit() must return 3 Gaussian parameters inputLen=" + ys.length);
            }

            // Contract/oracle: the no-arg overload computes ParameterGuesser(getObservations()).guess()
            // and is intended to agree with the sibling overload fit(double[] initialGuess) when passed
            // that exact guess. A patch that merely suppresses the throw, skips delegation, or returns a
            // different fallback would break this overload-agreement relation.
            try {
                GaussianFitter fitterA = new GaussianFitter(
                    new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
                );
                GaussianFitter fitterB = new GaussianFitter(
                    new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
                );
                for (int i = 0; i < ys.length; i++) {
                    fitterA.addObservedPoint((double) i, ys[i]);
                    fitterB.addObservedPoint((double) i, ys[i]);
                }

                double[] lhs = fitterA.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
                double[] rhs = fitterB.fit(guess);

                if (!sameParams(lhs, rhs)) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(ParameterGuesser(...).guess()) inputLen="
                            + ys.length + " lhs=" + format(lhs) + " rhs=" + format(rhs)
                    );
                }
            } catch (RuntimeException ignored) {
                if (ignored instanceof NotStrictlyPositiveException && isRootCause(ignored)) {
                    throw ignored;
                }
                if (ignored.getMessage() != null && ignored.getMessage().startsWith("[oracle:")) {
                    throw ignored;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (t instanceof NotStrictlyPositiveException && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
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
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static double[] buildPositiveTailDataset(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);

        double norm = 1.0 + data.consumeInt(0, 500);
        double mean = 10.0 + data.consumeInt(0, 120);
        double sigma = 0.5 + (data.consumeInt(0, 400) / 20.0);
        double xStart = -20.0 + data.consumeInt(0, 40);
        double step = 0.2 + (data.consumeInt(0, 50) / 10.0);
        boolean reverse = data.consumeBoolean();

        double[] ys = new double[len];
        Gaussian.Parametric g = new Gaussian.Parametric();

        for (int i = 0; i < len; i++) {
            int idx = reverse ? (len - 1 - i) : i;
            double x = xStart + idx * step;
            double y;
            try {
                y = g.value(x, new double[] { norm, mean, sigma });
            } catch (RuntimeException ex) {
                return null;
            }

            if (!Double.isFinite(y) || y <= 0.0) {
                return null;
            }

            int noiseBucket = data.consumeInt(0, 20);
            double factor = 1.0 + (noiseBucket - 10) * 0.005;
            y *= factor;

            if (!Double.isFinite(y) || y <= 0.0) {
                return null;
            }

            ys[i] = y;
        }

        return ys;
    }

    private static boolean isUsableFinitePositive(double v) {
        return Double.isFinite(v) && v > 0.0;
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != 3 || b.length != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                return false;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-7 * scale) {
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
            if (i > 0) {
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
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.exception.NotStrictlyPositiveException: -1.277 is smaller than, or equal to, the minimum (0)

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,874 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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
        exerciseAnchor();
        int cases = data.consumeInt(1, 3);
        for (int i = 0; i < cases; i++) {
            if (data.remainingBytes() <= 0) {
                break;
            }
            exerciseFuzzCase(data);
        }
    }

    private static void exerciseAnchor() {
        double[] xs = new double[ANCHOR.length];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = i;
        }
        exerciseOne(xs, ANCHOR, true);
    }

    private static void exerciseFuzzCase(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] xs = new double[len];
        double[] ys = new double[len];

        int xStart = data.consumeInt(-50, 50);
        int step = data.consumeInt(1, 5);
        int centerOffset = data.consumeInt(1, Math.max(2, len * 3));
        double center = xStart + (len - 1 + centerOffset) * (double) step;
        double sigma = 0.5 + (data.consumeInt(1, Math.max(2, len * 4)) / 4.0) * step;
        double amplitude = 0.1 + (data.consumeInt(1, 1000) / 10.0);
        double baseline = 1e-12 * (1 + data.consumeInt(0, 100));

        for (int i = 0; i < len; i++) {
            double x = xStart + i * (double) step;
            xs[i] = x;
            double dx = x - center;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noisePct = data.consumeInt(0, 20) / 100.0;
            if (data.consumeBoolean()) {
                y *= (1.0 + noisePct);
            } else {
                y *= (1.0 - noisePct * 0.5);
            }
            y += baseline;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            ys[i] = y;
        }

        exerciseOne(xs, ys, true);
    }

    private static void exerciseOne(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitterForGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitterForGuess.addObservedPoint(xs[i], ys[i]);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterForGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] viaNoArg;
        try {
            viaNoArg = fitterForGuess.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedPath(t)) {
                throw t;
            }
            return;
        }

        GaussianFitter fitterForExplicitGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitterForExplicitGuess.addObservedPoint(xs[i], ys[i]);
        }

        final double[] viaExplicitGuess;
        try {
            viaExplicitGuess = fitterForExplicitGuess.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        /*
         * Contract/oracle: GaussianFitter has sibling overloads fit() and fit(double[] initialGuess).
         * The patched implementation of fit() computes ParameterGuesser(...).guess() and delegates to fit(guess).
         * Therefore, for the same observations, fit() must agree with fit(guess()).
         * A throw-deleting or branch-skipping patch can make fit() return a different result while no exception fires.
         */
        if (!sameArray(viaNoArg, viaExplicitGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() must equal fit(ParameterGuesser.guess())"
                    + " inputLen=" + ys.length
                    + " lhs=" + arrayToString(viaNoArg)
                    + " rhs=" + arrayToString(viaExplicitGuess));
        }
    }

    private static boolean isRootCauseFromPatchedPath(Throwable t) {
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

    private static boolean sameArray(double[] a, double[] b) {
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
            if (diff > 1e-9 * scale) {
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
<details open><summary>▸ Output (~7,336 chars)</summary>

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
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        invokeFitValid(fitter, true);

        // Contract/oracle: GaussianFitter.fit() is specified by its own body to compute
        // a ParameterGuesser guess and delegate to fit(double[] initialGuess). Therefore,
        // for any input where both calls succeed, they must agree on the fitted parameters.
        // A patch that merely suppresses the exception or changes behavior on only one path
        // would violate this sibling-overload relation.
        checkSiblingAgreement(copyAnchorFitter(), true);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double norm = positiveFromInt(data.consumeInt(), 1.0e-6, 1.0e3);
        double mean = boundedFromInt(data.consumeInt(), -100.0, 100.0);
        double sigma = positiveFromInt(data.consumeInt(), 1.0e-3, 20.0);
        double startX = boundedFromInt(data.consumeInt(), -100.0, 100.0);
        double step = positiveFromInt(data.consumeInt(), 1.0e-3, 5.0);
        double shift = boundedFromInt(data.consumeInt(), -3.0, 3.0);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            double x = startX + (i * step) + shift;
            double dx = x - mean;
            double y = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            fitter.addObservedPoint(x, y);
        }

        invokeFitValid(fitter, true);
        checkSiblingAgreement(fitter, true);
    }

    private static GaussianFitter copyAnchorFitter() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        return fitter;
    }

    private static void invokeFitValid(GaussianFitter fitter, boolean validByConstruction) {
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static void checkSiblingAgreement(GaussianFitter fitter, boolean validByConstruction) {
        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitter.fit(guess);
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return;
        }

        if (viaNoArg == null || viaGuess == null || viaNoArg.length != viaGuess.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(double[]) returned incompatible arrays");
        }

        for (int i = 0; i < viaNoArg.length; i++) {
            double a = viaNoArg[i];
            double b = viaGuess[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double diff = Math.abs(a - b);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (diff > 1.0e-8 * scale) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() must agree with fit(guess()) index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!isRootCauseFamily(t)) {
            return false;
        }
        return stackPassesThroughGaussianFitterFit(t);
    }

    private static boolean isRootCauseFamily(Throwable t) {
        String name = t.getClass().getName();
        return name.equals("org.apache.commons.math.exception.NotStrictlyPositiveException")
            || name.equals("org.apache.commons.math.exception.NumberIsTooSmallException")
            || name.equals("org.apache.commons.math.exception.MathIllegalNumberException")
            || name.equals("org.apache.commons.math.exception.MathIllegalArgumentException")
            || t instanceof IllegalArgumentException;
    }

    private static boolean stackPassesThroughGaussianFitterFit(Throwable t) {
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

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static double boundedFromInt(int v, double min, double max) {
        long u = v & 0xffffffffL;
        double r = u / (double) 0xffffffffL;
        return min + (max - min) * r;
    }

    private static double positiveFromInt(int v, double min, double max) {
        double x = boundedFromInt(v, min, max);
        return x <= 0.0 ? min : x;
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
<details open><summary>▸ Output (~12,384 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Contract/oracle used below:
         * GaussianFitter.fit() computes an initial guess from the observations and then performs fitting.
         * The overload fit(double[] initialGuess) is the same operation when given that very guess.
         * Therefore, for the same observations, fit() and fit(new ParameterGuesser(getObservations()).guess())
         * must agree whenever both calls return normally. A patch that merely deletes/avoids the failing call path
         * can violate this sibling-overload agreement without throwing.
         */

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

        // ANCHOR: exact failing test inputs first.
        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                                && "fit".equals(st[i].getMethodName())) {
                            throughPatchedFit = true;
                            break;
                        }
                    }
                }
                boolean validationFamily =
                        (t instanceof IllegalArgumentException)
                        || (t instanceof NumberFormatException)
                        || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
                boolean rootCause =
                        (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                        && throughPatchedFit;
                if (rootCause) {
                    throw (RuntimeException) t;
                }
                if (validationFamily) {
                    return;
                }
                return;
            }
        }

        // ANCHOR metamorphic sibling-agreement check: fit() must agree with fit(guessedInitialParameters).
        {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                f1.addObservedPoint(i, anchor[i]);
                f2.addObservedPoint(i, anchor[i]);
            }

            double[] guess;
            try {
                guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            } catch (Throwable t) {
                return;
            }

            double[] r1;
            double[] r2;
            try {
                r1 = f1.fit();
                r2 = f2.fit(guess);
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                                && "fit".equals(st[i].getMethodName())) {
                            throughPatchedFit = true;
                            break;
                        }
                    }
                }
                boolean validationFamily =
                        (t instanceof IllegalArgumentException)
                        || (t instanceof NumberFormatException)
                        || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
                boolean rootCause =
                        (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                        && throughPatchedFit;
                if (rootCause) {
                    throw (RuntimeException) t;
                }
                if (validationFamily) {
                    return;
                }
                return;
            }

            if (r1 != null && r2 != null && r1.length == r2.length) {
                for (int i = 0; i < r1.length; i++) {
                    double a = r1[i];
                    double b = r2[i];
                    boolean equal;
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        equal = true;
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        equal = (a == b);
                    } else {
                        double diff = Math.abs(a - b);
                        double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        equal = diff <= tol;
                    }
                    if (!equal) {
                        throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(guess()) input=anchor lhs=" + a + " rhs=" + b + " index=" + i);
                    }
                }
            }
        }

        // EXPLORE: varied, valid-by-construction, non-negative bell-shaped observations.
        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        int xOffset = data.consumeInt(-20, 20);
        int step = data.consumeInt(1, 3);
        int centerIndex = data.consumeInt(0, n - 1);
        double center = xOffset + centerIndex * step + (data.consumeInt(-100, 100) / 100.0);
        double sigma = data.consumeInt(10, 800) / 100.0;
        double amplitude = Math.pow(10.0, data.consumeInt(-20, 3));
        double baseline = data.consumeBoolean() ? 0.0 : Math.pow(10.0, data.consumeInt(-25, -5));

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - center) / sigma;
            double y = baseline + amplitude * Math.exp(-0.5 * z * z);

            // Small multiplicative noise while preserving non-negativity and moderate magnitudes.
            int noiseBucket = data.consumeInt(70, 130);
            y *= (noiseBucket / 100.0);

            // Optional zero padding / sparse tails to vary surrounding content.
            if ((i < 3 || i >= n - 3) && data.consumeBoolean()) {
                y *= data.consumeInt(0, 10) / 1000.0;
            }

            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 0.0;
            }
            ys[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xOffset + i * step, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            boolean throughPatchedFit = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        throughPatchedFit = true;
                        break;
                    }
                }
            }
            boolean validationFamily =
                    (t instanceof IllegalArgumentException)
                    || (t instanceof NumberFormatException)
                    || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
            boolean rootCause =
                    (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                    && throughPatchedFit;
            if (rootCause) {
                throw (RuntimeException) t;
            }
            if (validationFamily) {
                return;
            }
            return;
        }

        // EXPLORE sibling-agreement oracle.
        {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                double x = xOffset + i * step;
                f1.addObservedPoint(x, ys[i]);
                f2.addObservedPoint(x, ys[i]);
            }

            double[] guess;
            try {
                guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            } catch (Throwable t) {
                return;
            }

            double[] r1;
            double[] r2;
            try {
                r1 = f1.fit();
                r2 = f2.fit(guess);
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                                && "fit".equals(st[i].getMethodName())) {
                            throughPatchedFit = true;
                            break;
                        }
                    }
                }
                boolean validationFamily =
                        (t instanceof IllegalArgumentException)
                        || (t instanceof NumberFormatException)
                        || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
                boolean rootCause =
                        (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                        && throughPatchedFit;
                if (rootCause) {
                    throw (RuntimeException) t;
                }
                if (validationFamily) {
                    return;
                }
                return;
            }

            if (r1 == null || r2 == null || r1.length != r2.length) {
                return;
            }

            for (int i = 0; i < r1.length; i++) {
                double a = r1[i];
                double b = r2[i];
                boolean equal;
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    equal = true;
                } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    equal = (a == b);
                } else {
                    double diff = Math.abs(a - b);
                    double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    equal = diff <= tol;
                }
                if (!equal) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(guess()) input=n=" + ys.length + ",xOffset=" + xOffset + ",step=" + step + " lhs=" + a + " rhs=" + b + " index=" + i);
                }
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,518 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 3);
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int n = data.consumeInt(3, 40);
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;

        double center;
        if (mode == 0) {
            center = -1.0 - data.consumeInt(0, 200);
        } else if (mode == 1) {
            center = (n - 1) + 1.0 + data.consumeInt(0, 200);
        } else {
            center = data.consumeInt(-100, 100);
        }

        for (int i = 0; i < n; i++) {
            double x = i * step;
            double dx = x - center;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));

            int tweak = data.consumeInt(0, 20);
            if (tweak > 0) {
                y += tweak * 1e-12;
            }

            if (!(Double.isNaN(y) || Double.isInfinite(y)) && y > 0.0) {
                fitterA.addObservedPoint(x, y);
                fitterB.addObservedPoint(x, y);
            }
        }

        if (fitterA.getObservations().length < 3) {
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        boolean guessValid = guess != null
                && guess.length == 3
                && isFinite(guess[0])
                && isFinite(guess[1])
                && isFinite(guess[2])
                && guess[0] > 0.0
                && guess[2] > 0.0;
        if (!guessValid) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        if (!sameParams(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(initialGuess) "
                            + "when initialGuess is exactly GaussianFitter.ParameterGuesser(getObservations()).guess() "
                            + "inputPoints=" + fitterA.getObservations().length
                            + " lhs=" + format(viaNoArg)
                            + " rhs=" + format(viaGuess));
        }
    }

    private static void runAnchor() {
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

        GaussianFitter fitterFit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < data.length; i++) {
            fitterFit.addObservedPoint(i, data[i]);
            fitterGuess.addObservedPoint(i, data[i]);
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterGuess.getObservations()).guess();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            fitterFit.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterGuess.fit(guess);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        double[] viaNoArg;
        try {
            GaussianFitter fitterAgain = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < data.length; i++) {
                fitterAgain.addObservedPoint(i, data[i]);
            }
            viaNoArg = fitterAgain.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        if (!sameParams(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(initialGuess) "
                            + "for the regression-test observations lhs=" + format(viaNoArg)
                            + " rhs=" + format(viaGuess));
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof IllegalArgumentException)) {
            return false;
        }
        if (!t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
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
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!isFinite(a[i]) || !isFinite(b[i])) {
                return false;
            }
            double diff = Math.abs(a[i] - b[i]);
            double scale = Math.max(1.0, Math.max(Math.abs(a[i]), Math.abs(b[i])));
            if (diff > 1.0e-9 * scale) {
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
                sb.append(", ");
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
## [44] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,384 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 3);
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int n = data.consumeInt(3, 40);
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;

        double center;
        if (mode == 0) {
            center = -1.0 - data.consumeInt(0, 200);
        } else if (mode == 1) {
            center = (n - 1) + 1.0 + data.consumeInt(0, 200);
        } else {
            center = data.consumeInt(-100, 100);
        }

        for (int i = 0; i < n; i++) {
            double x = i * step;
            double dx = x - center;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));

            int tweak = data.consumeInt(0, 20);
            if (tweak > 0) {
                y += tweak * 1e-12;
            }

            if (!(Double.isNaN(y) || Double.isInfinite(y)) && y > 0.0) {
                fitterA.addObservedPoint(x, y);
                fitterB.addObservedPoint(x, y);
            }
        }

        if (fitterA.getObservations().length < 3) {
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        boolean guessValid = guess != null
                && guess.length == 3
                && isFinite(guess[0])
                && isFinite(guess[1])
                && isFinite(guess[2])
                && guess[0] > 0.0
                && guess[2] > 0.0;
        if (!guessValid) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        if (!sameParams(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(initialGuess) "
                            + "when initialGuess is exactly GaussianFitter.ParameterGuesser(getObservations()).guess() "
                            + "inputPoints=" + fitterA.getObservations().length
                            + " lhs=" + format(viaNoArg)
                            + " rhs=" + format(viaGuess));
        }
    }

    private static void runAnchor() {
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

        GaussianFitter fitterFit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < data.length; i++) {
            fitterFit.addObservedPoint(i, data[i]);
            fitterGuess.addObservedPoint(i, data[i]);
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterGuess.getObservations()).guess();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            fitterFit.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterGuess.fit(guess);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        double[] viaNoArg;
        try {
            GaussianFitter fitterAgain = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < data.length; i++) {
                fitterAgain.addObservedPoint(i, data[i]);
            }
            viaNoArg = fitterAgain.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        if (!sameParams(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(initialGuess) "
                            + "for the regression-test observations lhs=" + format(viaNoArg)
                            + " rhs=" + format(viaGuess));
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof IllegalArgumentException)) {
            return false;
        }
        if (!t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
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
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!isFinite(a[i]) || !isFinite(b[i])) {
                return false;
            }
            double diff = Math.abs(a[i] - b[i]);
            double scale = Math.max(1.0, Math.max(Math.abs(a[i]), Math.abs(b[i])));
            if (diff > 1.0e-9 * scale) {
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
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
+++ /src/main/java/org/apache/commons/math/optimization/fitting/GaussianFitter.java
@@ -118,7 +118,7 @@
      */
     public double[] fit() {
         final double[] guess = (new ParameterGuesser(getObservations())).guess();
-        return fit(new Gaussian.Parametric(), guess);
+        return fit((new ParameterGuesser(getObservations())).guess());
     }
 
     /**


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
<details open><summary>▸ Output (~5,514 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = {
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
        GaussianFitter anchor = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            anchor.addObservedPoint(i, ANCHOR_Y[i]);
        }

        // Anchor: this exact public API call reproduces the known buggy failure.
        // On the fixed build it should behave like fit(ParameterGuesser(...).guess()) instead.
        anchor.fit();

        // Explore the same root-cause property with real inputs:
        // positive observed data shaped like the regression case, but with varied x scale/offset
        // and mild positive rescaling of y, then compare the documented same-name overloads.
        int copies = data.consumeInt(1, 3);
        double xScale = 0.25 + (data.consumeInt(0, 400) / 100.0);
        double xOffset = data.consumeInt(-200, 200);
        double yScale = 0.5 + (data.consumeInt(0, 300) / 100.0);
        double baseline = data.consumeInt(0, 1000) * 1e-18;

        int trimLeft = data.consumeInt(0, 2);
        int trimRight = data.consumeInt(0, 2);
        int start = trimLeft;
        int end = ANCHOR_Y.length - trimRight;
        if (end - start < 3) {
            start = 0;
            end = ANCHOR_Y.length;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int idx = 0;
        for (int c = 0; c < copies; c++) {
            for (int i = start; i < end; i++) {
                double x = xOffset + idx * xScale;
                double y = ANCHOR_Y[i] * yScale + baseline;
                fitterNoArg.addObservedPoint(x, y);
                fitterWithGuess.addObservedPoint(x, y);
                idx++;
            }
        }

        double[] guessed;
        try {
            guessed = new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations()).guess();
        } catch (RuntimeException e) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterNoArg.fit();
        } catch (RuntimeException e) {
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterWithGuess.fit(guessed);
        } catch (RuntimeException e) {
            return;
        }

        // Contract/oracle: fit() and fit(double[] initialGuess) are same-name overloads for the
        // same fitting operation. In the fixed code, fit() delegates to fit(guessedInitialGuess).
        // Therefore, when we supply exactly the same guessed initial parameters explicitly, both
        // calls must produce the same result on any correct implementation.
        if (!sameArray(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() != fit(guessedInitialGuess)"
                    + " start=" + start
                    + " end=" + end
                    + " copies=" + copies
                    + " xScale=" + xScale
                    + " xOffset=" + xOffset
                    + " yScale=" + yScale
                    + " baseline=" + baseline
                    + " lhs=" + format(viaNoArg)
                    + " rhs=" + format(viaGuess)
            );
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) || Double.isNaN(b[i]) || Double.isInfinite(a[i]) || Double.isInfinite(b[i])) {
                return false;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a[i]), Math.abs(b[i])));
            if (Math.abs(a[i] - b[i]) > 1e-8 * scale) {
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
            if (i > 0) {
                sb.append(", ");
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
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,669 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        try {
            runExplore(data);
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracleViolation(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        final double[] ys = new double[] {
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
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        fitter.fit();
    }

    private static void runExplore(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double amplitude = positiveFromInt(data.consumeInt(), 1.0e-12, 500.0);
        double mean = bounded(data.consumeInt(), -20.0, n - 1 + 20.0);
        double sigma = positiveFromInt(data.consumeInt(), 0.05, 20.0);
        double baseline = positiveFromInt(data.consumeInt(), 0.0, 1.0);

        GaussianFitter fitter1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = baseline + amplitude * Math.exp(-((x - mean) * (x - mean)) / (2.0 * sigma * sigma));
            if (data.consumeBoolean()) {
                double noiseScale = positiveFromInt(data.consumeInt(), 0.0, amplitude * 0.05 + 1.0e-12);
                double noise = signedFromInt(data.consumeInt(), noiseScale);
                y += noise;
                if (y < 0.0) {
                    y = 0.0;
                }
            }
            fitter1.addObservedPoint(x, y);
        }

        double[] fitNoArg;
        try {
            fitNoArg = fitter1.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (WeightedObservedPoint p : fitter1.getObservations()) {
            fitter2.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter2.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        final double[] fitWithGuess;
        try {
            fitWithGuess = fitter2.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/metamorphic check:
           GaussianFitter.fit() computes a ParameterGuesser guess from the current observations
           and then delegates to the same-name overload with that guess. Therefore, for the same
           observations, fit() and fit(new ParameterGuesser(getObservations()).guess()) must agree.
           A patch that merely suppresses the original throw or skips the intended delegation can
           return silently wrong parameters here even when no exception is thrown. */
        if (!sameParameters(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess) input=n=" + n
                    + ",amp=" + amplitude
                    + ",mean=" + mean
                    + ",sigma=" + sigma
                    + ",baseline=" + baseline
                    + " lhs=" + arrayToString(fitNoArg)
                    + " rhs=" + arrayToString(fitWithGuess)
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static double positiveFromInt(int v, double min, double max) {
        double unit = (v & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return min + unit * (max - min);
    }

    private static double signedFromInt(int v, double magnitude) {
        double unit = (v / (double) Integer.MAX_VALUE);
        if (unit < -1.0) {
            unit = -1.0;
        } else if (unit > 1.0) {
            unit = 1.0;
        }
        return unit * magnitude;
    }

    private static double bounded(int v, double min, double max) {
        double unit = (v & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return min + unit * (max - min);
    }

    private static boolean sameParameters(double[] a, double[] b) {
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
            if (diff > 1.0e-6 * scale) {
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
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,555 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        // ANCHOR: exact trigger from the regression test.
        exercise(buildAnchorX(), ANCHOR_Y, true);

        // EXPLORE: same root-cause property with real, valid-by-construction inputs:
        // positive gaussian-like observations, varied length/position/scale/surrounding content.
        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            exercise(buildTransformedAnchorX(data), buildTransformedAnchorY(data), true);
        } else if (mode == 1) {
            exercise(buildSyntheticGaussianX(data), buildSyntheticGaussianY(data), true);
        } else {
            double[][] mixed = buildMixedAnchorWithPadding(data);
            exercise(mixed[0], mixed[1], true);
        }
    }

    private static double[] buildAnchorX() {
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        return x;
    }

    private static double[] buildTransformedAnchorX(FuzzedDataProvider data) {
        double offset = data.consumeInt(-100, 100);
        double step = 1.0 + (data.consumeInt(0, 50) / 25.0);
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = offset + step * i;
        }
        return x;
    }

    private static double[] buildTransformedAnchorY(FuzzedDataProvider data) {
        double scale = Math.pow(10.0, data.consumeInt(-3, 3));
        double jitter = data.consumeInt(0, 20) / 1000.0;
        double[] y = new double[ANCHOR_Y.length];
        for (int i = 0; i < y.length; i++) {
            double factor = 1.0 + (((i & 1) == 0) ? jitter : -jitter);
            double v = ANCHOR_Y[i] * scale * factor;
            y[i] = (v > 0.0 && Double.isFinite(v)) ? v : Math.abs(ANCHOR_Y[i]) + 1e-300;
        }
        return y;
    }

    private static double[] buildSyntheticGaussianX(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double offset = data.consumeInt(-100, 100);
        double step = 1.0 + (data.consumeInt(0, 20) / 10.0);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = offset + i * step;
        }
        return x;
    }

    private static double[] buildSyntheticGaussianY(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double norm = 1e-6 * (1 + data.consumeInt(0, 1_000_000));
        double mean = data.consumeInt(0, n - 1);
        double sigma = 0.2 + (data.consumeInt(0, 200) / 20.0);
        double noise = data.consumeInt(0, 20) / 1000.0;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double z = (i - mean) / sigma;
            double base = norm * Math.exp(-0.5 * z * z);
            double wobble = 1.0 + (((i % 3) - 1) * noise);
            double v = base * wobble;
            y[i] = (v > 0.0 && Double.isFinite(v)) ? v : 1e-300;
        }
        return y;
    }

    private static double[][] buildMixedAnchorWithPadding(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 6);
        int suffix = data.consumeInt(0, 6);
        double offset = data.consumeInt(-50, 50);
        double step = 1.0 + (data.consumeInt(0, 20) / 10.0);
        double scale = Math.pow(10.0, data.consumeInt(-2, 2));
        int n = prefix + ANCHOR_Y.length + suffix;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = offset + i * step;
        }
        for (int i = 0; i < prefix; i++) {
            y[i] = tinyPositive(i + 1);
        }
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            y[prefix + i] = Math.max(ANCHOR_Y[i] * scale, tinyPositive(i + 10));
        }
        for (int i = 0; i < suffix; i++) {
            y[prefix + ANCHOR_Y.length + i] = tinyPositive(i + 100);
        }
        return new double[][] { x, y };
    }

    private static double tinyPositive(int n) {
        return 1e-300 * (1.0 + (n % 10));
    }

    private static void exercise(double[] x, double[] y, boolean validByConstruction) {
        if (x == null || y == null || x.length != y.length || x.length < 3) {
            return;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i]) || y[i] <= 0.0) {
                return;
            }
            fitter.addObservedPoint(x[i], y[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract/oracle: no-arg fit() computes a guess from the observations and should agree
        // with fit(double[] initialGuess) when given that same guess. A throw-deleting or
        // wrong-delegation patch can violate this observable equivalence.
        GaussianFitter lhsFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter rhsFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length; i++) {
            lhsFitter.addObservedPoint(x[i], y[i]);
            rhsFitter.addObservedPoint(x[i], y[i]);
        }

        try {
            double[] lhs = lhsFitter.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(rhsFitter.getObservations())).guess();
            double[] rhs = rhsFitter.fit(guess);
            assertArrayEquivalent(x, y, lhs, rhs);
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertArrayEquivalent(double[] x, double[] y, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) length mismatch inputLen=" + x.length);
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) || Double.isNaN(b)) {
                if (!(Double.isNaN(a) && Double.isNaN(b))) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) NaN mismatch index=" + i + " inputLen=" + x.length + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) infinity mismatch index=" + i + " inputLen=" + x.length + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            double tol = 1e-7 * (1.0 + Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) differ index=" + i + " inputLen=" + x.length + " lhs=" + a + " rhs=" + b);
            }
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
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
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
## [50] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,929 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * The no-arg fit() overload and fit(double[] initialGuess) must agree when
         * initialGuess is exactly the ParameterGuesser-produced guess for the same
         * observations, because the patched implementation is literally:
         *   return fit((new ParameterGuesser(getObservations())).guess());
         * A patch that merely suppresses the throw or returns a wrong result from
         * fit() would violate this sibling-overload agreement.
         */

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

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchorData.length; i++) {
                fitter.addObservedPoint(i, anchorData[i]);
            }

            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
            }

            try {
                double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
                double[] lhs = fitter.fit();
                double[] rhs = fitter.fit(guess);
                if (lhs != null && rhs != null) {
                    if (lhs.length != rhs.length) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) length mismatch lhs="
                                + lhs.length + " rhs=" + rhs.length);
                    }
                    for (int i = 0; i < lhs.length; i++) {
                        double a = lhs[i];
                        double b = rhs[i];
                        boolean equal;
                        if (Double.isNaN(a) || Double.isNaN(b)) {
                            equal = Double.isNaN(a) && Double.isNaN(b);
                        } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                            equal = a == b;
                        } else {
                            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                            equal = Math.abs(a - b) <= 1.0e-7 * scale;
                        }
                        if (!equal) {
                            throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() != fit(guess) index="
                                    + i + " lhs=" + a + " rhs=" + b);
                        }
                    }
                }
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t instanceof RuntimeException
                        && t.getMessage() != null
                        && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
            }
        }

        int cases = 1 + data.consumeInt(1, 3);
        Gaussian.Parametric generator = new Gaussian.Parametric();

        for (int c = 0; c < cases; c++) {
            int n = data.consumeInt(3, 60);
            double norm = 1.0e-6 + data.consumeInt(1, 1_000_000) / 10_000.0;
            double sigma = 0.2 + data.consumeInt(0, 2000) / 100.0;
            double mean = data.consumeInt(0, n - 1) + data.consumeInt(0, 1000) / 1000.0;
            double xStart = data.consumeInt(-1000, 1000) / 10.0;
            double step = 0.1 + data.consumeInt(0, 1000) / 100.0;
            double baseline = data.consumeInt(0, 1000) / 1_000_000.0;
            double jitterScale = data.consumeInt(0, 1000) / 1_000_000.0;

            double[] params = new double[] { norm, mean, sigma };
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                double x = xStart + i * step;
                double mappedX = (x - xStart) / step;
                double y;
                try {
                    y = generator.value(mappedX, params);
                } catch (RuntimeException impossibleForValidParams) {
                    return;
                }
                y += baseline;
                if (jitterScale > 0.0) {
                    int signed = data.consumeInt(-1000, 1000);
                    double factor = 1.0 + signed * jitterScale / 1000.0;
                    if (factor <= 0.01) {
                        factor = 0.01;
                    }
                    y *= factor;
                }
                if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = baseline + 1.0e-12;
                }
                fitter.addObservedPoint(x, y);
            }

            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
                continue;
            }

            try {
                double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
                double[] lhs = fitter.fit();
                double[] rhs = fitter.fit(guess);

                if (lhs == null || rhs == null) {
                    continue;
                }
                if (lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) length mismatch n="
                            + n + " lhs=" + lhs.length + " rhs=" + rhs.length);
                }

                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    boolean equal;
                    if (Double.isNaN(a) || Double.isNaN(b)) {
                        equal = Double.isNaN(a) && Double.isNaN(b);
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        equal = a == b;
                    } else {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        equal = Math.abs(a - b) <= 1.0e-6 * scale;
                    }
                    if (!equal) {
                        throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() != fit(guess) n="
                                + n + " index=" + i + " lhs=" + a + " rhs=" + b);
                    }
                }
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
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
<details open><summary>▸ Output (~6,698 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        exerciseScenario(anchorX, ANCHOR_Y, true);

        int start = data.consumeInt(0, ANCHOR_Y.length - 3);
        int end = data.consumeInt(start + 3, ANCHOR_Y.length);
        int prefix = data.consumeInt(0, 5);
        int suffix = data.consumeInt(0, 5);
        int total = prefix + (end - start) + suffix;

        double[] xs = new double[total];
        double[] ys = new double[total];

        int x0 = data.consumeInt(-100, 100);
        int step = data.consumeInt(1, 5);
        double yScale = pow10(data.consumeInt(-3, 3));

        int pos = 0;
        for (int i = 0; i < prefix; i++, pos++) {
            xs[pos] = x0 + pos * step;
            ys[pos] = strictlyPositive(ANCHOR_Y[start] / pow10(data.consumeInt(1, 6)) * yScale);
        }
        for (int i = start; i < end; i++, pos++) {
            xs[pos] = x0 + pos * step;
            ys[pos] = strictlyPositive(ANCHOR_Y[i] * yScale);
        }
        for (int i = 0; i < suffix; i++, pos++) {
            xs[pos] = x0 + pos * step;
            ys[pos] = strictlyPositive(ANCHOR_Y[end - 1] * pow10(data.consumeInt(0, 3)) * yScale);
        }

        exerciseScenario(xs, ys, true);
    }

    private static void exerciseScenario(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isGroundTruthFromFit(t)) {
                throw t;
            }
            return;
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter2.addObservedPoint(xs[i], ys[i]);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter2.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] viaExplicit;
        try {
            viaExplicit = fitter2.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/oracle: GaussianFitter.fit() is documented as the no-arg overload of
           fit(double[] initialGuess); the implementation computes a guess, then delegates.
           Therefore a correct implementation must agree with an explicit call using that
           same guessed parameter vector. A patch that only suppresses the throw or changes
           the call path to silently produce different results breaks this sibling-agreement. */
        assertArrayClose(xs, ys, viaNoArg, viaExplicit);
    }

    private static boolean isGroundTruthFromFit(Throwable t) {
        if (t == null) {
            return false;
        }
        String name = t.getClass().getName();
        if (!name.endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertArrayClose(double[] xs, double[] ys, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) length mismatch inputPoints="
                    + ys.length);
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) infinite mismatch index="
                            + i + " lhs=" + x + " rhs=" + y + " points=" + ys.length);
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > tol) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(initialGuess) index="
                        + i + " lhs=" + x + " rhs=" + y + " points=" + ys.length
                        + " firstX=" + xs[0] + " lastX=" + xs[xs.length - 1]
                        + " firstY=" + ys[0] + " lastY=" + ys[ys.length - 1]);
            }
        }
    }

    private static double pow10(int exp) {
        double v = 1.0;
        if (exp >= 0) {
            for (int i = 0; i < exp; i++) {
                v *= 10.0;
            }
        } else {
            for (int i = 0; i < -exp; i++) {
                v /= 10.0;
            }
        }
        return v;
    }

    private static double strictlyPositive(double v) {
        return v > 0.0 ? v : Double.MIN_VALUE;
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
<details open><summary>▸ Output (~6,785 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
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
        runOneCase(anchor, true);

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];

        double norm = (double) Integer.MAX_VALUE;
        double amplitude = 0.1 + 50.0 * (Math.abs((double) data.consumeInt()) / norm);
        double sigma = 0.2 + 20.0 * (Math.abs((double) data.consumeInt()) / norm);
        boolean tailShape = data.consumeBoolean();
        double center;
        if (tailShape) {
            center = n + data.consumeInt(1, 20);
        } else {
            center = data.consumeInt(0, n - 1);
        }

        Gaussian.Parametric gp = new Gaussian.Parametric();
        for (int i = 0; i < n; i++) {
            double base;
            try {
                base = gp.value((double) i, new double[] { amplitude, center, sigma });
            } catch (RuntimeException e) {
                return;
            }

            int tweakRaw = data.consumeInt(-1000, 1000);
            double tweak = 1.0 + (tweakRaw / 5000.0);
            if (tweak <= 0.05) {
                tweak = 0.05;
            }

            double baseline = data.consumeInt(0, 1000) / 1_000_000.0;
            double y = base * tweak + baseline;

            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                y = baseline + 1.0e-12;
            }
            ys[i] = y;
        }

        runOneCase(ys, true);

        int prefix = data.consumeInt(0, 5);
        int suffix = data.consumeInt(0, 5);
        double[] padded = new double[prefix + n + suffix];
        for (int i = 0; i < prefix; i++) {
            padded[i] = data.consumeInt(0, 1000) / 1_000_000.0 + 1.0e-12;
        }
        System.arraycopy(ys, 0, padded, prefix, n);
        for (int i = 0; i < suffix; i++) {
            padded[prefix + n + i] = data.consumeInt(0, 1000) / 1_000_000.0 + 1.0e-12;
        }
        runOneCase(padded, true);
    }

    private static void runOneCase(double[] yValues, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < yValues.length; i++) {
            double y = yValues[i];
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            fitter.addObservedPoint((double) i, y);
        }

        double[] guessed;
        try {
            guessed = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (RuntimeException e) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] viaExplicitGuess;
        try {
            viaExplicitGuess = fitter.fit(guessed);
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() computes ParameterGuesser(...).guess() and then delegates to fit(initialGuess).
         * Therefore, for the same observations and the exact same guessed parameters, fit() and fit(guess)
         * must produce the same result on every correct implementation. A throw-deleting or branch-skipping
         * patch can make fit() silently diverge from fit(initialGuess), so we check sibling-agreement here.
         */
        if (viaNoArg.length != viaExplicitGuess.length) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) returned different lengths inputLen="
                    + yValues.length + " lhsLen=" + viaNoArg.length + " rhsLen=" + viaExplicitGuess.length);
        }

        for (int i = 0; i < viaNoArg.length; i++) {
            double a = viaNoArg[i];
            double b = viaExplicitGuess[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b) || Double.isNaN(a) || Double.isNaN(b)) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) disagreed on finiteness inputLen="
                        + yValues.length + " index=" + i + " lhs=" + a + " rhs=" + b);
            }
            double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                        + yValues.length + " index=" + i + " lhs=" + a + " rhs=" + b + " tol=" + tol);
            }
        }
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~9,443 chars)</summary>

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

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
            }
        }

        {
            double[] lhs = null;
            double[] rhs = null;
            boolean lhsOk = false;
            boolean rhsOk = false;

            try {
                GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f1.addObservedPoint(i, anchor[i]);
                }
                lhs = f1.fit();
                lhsOk = true;
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
                return;
            }

            try {
                GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    g.addObservedPoint(i, anchor[i]);
                }
                double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

                GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f2.addObservedPoint(i, anchor[i]);
                }
                rhs = f2.fit(guess);
                rhsOk = true;
            } catch (Throwable t) {
                return;
            }

            if (lhsOk && rhsOk) {
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                }
                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                        continue;
                    }
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        continue;
                    }
                    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (Math.abs(a - b) > 1.0e-8 * scale) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                }
            }
        }

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        double[] xs = new double[n];

        int center = data.consumeInt(0, n - 1);
        double sigma = 0.5 + (Math.abs(data.consumeInt()) % 2000) / 200.0;
        double amplitude = 1.0e-12 + (Math.abs(data.consumeInt()) % 1000000) / 1000.0;
        double baseline = (Math.abs(data.consumeInt()) % 1000) / 1000000.0;
        double start = (data.consumeInt(-1000, 1000)) / 10.0;
        double step = 0.1 + (Math.abs(data.consumeInt()) % 1000) / 100.0;

        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
            double d = i - center;
            double bell = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            double noise = (Math.abs(data.consumeInt()) % 1000) / 1000000000.0;
            ys[i] = baseline + bell + noise;
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1.0e-12;
            }
        }

        double[] lhs = null;
        double[] rhs = null;

        try {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f1.addObservedPoint(xs[i], ys[i]);
            }
            lhs = f1.fit();
        } catch (Throwable t) {
            boolean hasFitFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    hasFitFrame = true;
                    break;
                }
            }
            if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                g.addObservedPoint(xs[i], ys[i]);
            }
            double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f2.addObservedPoint(xs[i], ys[i]);
            }
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * The no-arg overload is specified by its own implementation to compute
         * ParameterGuesser(getObservations()).guess() and then delegate to fit(initialGuess).
         * Therefore, for the same observations, whenever both calls return normally,
         * their parameter arrays must agree. A patch that merely suppresses the crash or
         * changes delegation would violate this observable relation without throwing.
         */
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > 1.0e-8 * scale) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
            }
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 131: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds an

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,007 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
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

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
            }
        }

        {
            double[] lhs = null;
            double[] rhs = null;
            boolean lhsOk = false;
            boolean rhsOk = false;

            try {
                GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f1.addObservedPoint(i, anchor[i]);
                }
                lhs = f1.fit();
                lhsOk = true;
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
                return;
            }

            try {
                GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    g.addObservedPoint(i, anchor[i]);
                }
                double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

                GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f2.addObservedPoint(i, anchor[i]);
                }
                rhs = f2.fit(guess);
                rhsOk = true;
            } catch (Throwable t) {
                return;
            }

            if (lhsOk && rhsOk) {
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                }
                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                        continue;
                    }
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        continue;
                    }
                    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (Math.abs(a - b) > 1.0e-8 * scale) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                }
            }
        }

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        double[] xs = new double[n];

        int center = data.consumeInt(0, n - 1);
        double sigma = 0.5 + (Math.abs(data.consumeInt()) % 2000) / 200.0;
        double amplitude = 1.0e-12 + (Math.abs(data.consumeInt()) % 1000000) / 1000.0;
        double baseline = (Math.abs(data.consumeInt()) % 1000) / 1000000.0;
        double start = (data.consumeInt(-1000, 1000)) / 10.0;
        double step = 0.1 + (Math.abs(data.consumeInt()) % 1000) / 100.0;

        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
            double d = i - center;
            double bell = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            double noise = (Math.abs(data.consumeInt()) % 1000) / 1000000000.0;
            ys[i] = baseline + bell + noise;
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1.0e-12;
            }
        }

        double[] lhs = null;
        double[] rhs = null;

        try {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f1.addObservedPoint(xs[i], ys[i]);
            }
            lhs = f1.fit();
        } catch (Throwable t) {
            boolean hasFitFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    hasFitFrame = true;
                    break;
                }
            }
            if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                g.addObservedPoint(xs[i], ys[i]);
            }
            double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f2.addObservedPoint(xs[i], ys[i]);
            }
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * The no-arg overload is specified by its own implementation to compute
         * ParameterGuesser(getObservations()).guess() and then delegate to fit(initialGuess).
         * Therefore, for the same observations, whenever both calls return normally,
         * their parameter arrays must agree. A patch that merely suppresses the crash or
         * changes delegation would violate this observable relation without throwing.
         */
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > 1.0e-8 * scale) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
            }
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 131: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~9,375 chars)</summary>

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

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
            }
        }

        {
            double[] lhs = null;
            double[] rhs = null;
            boolean lhsOk = false;
            boolean rhsOk = false;

            try {
                GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f1.addObservedPoint(i, anchor[i]);
                }
                lhs = f1.fit();
                lhsOk = true;
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
                return;
            }

            try {
                GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    g.addObservedPoint(i, anchor[i]);
                }
                double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

                GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f2.addObservedPoint(i, anchor[i]);
                }
                rhs = f2.fit(guess);
                rhsOk = true;
            } catch (Throwable t) {
                return;
            }

            if (lhsOk && rhsOk) {
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                }
                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                        continue;
                    }
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        continue;
                    }
                    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (Math.abs(a - b) > 1.0e-8 * scale) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                }
            }
        }

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        double[] xs = new double[n];

        int center = data.consumeInt(0, n - 1);
        double sigma = data.consumeInt(1, 2000) / 200.0;
        double amplitude = 1.0e-12 + data.consumeInt(0, 1000000) / 1000.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double start = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;

        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
            double d = i - center;
            double bell = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            double noise = data.consumeInt(0, 1000) / 1000000000.0;
            ys[i] = baseline + bell + noise;
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1.0e-12;
            }
        }

        double[] lhs = null;
        double[] rhs = null;

        try {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f1.addObservedPoint(xs[i], ys[i]);
            }
            lhs = f1.fit();
        } catch (Throwable t) {
            boolean hasFitFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    hasFitFrame = true;
                    break;
                }
            }
            if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                g.addObservedPoint(xs[i], ys[i]);
            }
            double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f2.addObservedPoint(xs[i], ys[i]);
            }
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * The no-arg overload is specified by its own implementation to compute
         * ParameterGuesser(getObservations()).guess() and then delegate to fit(initialGuess).
         * Therefore, for the same observations, whenever both calls return normally,
         * their parameter arrays must agree. A patch that merely suppresses the crash or
         * changes delegation would violate this observable relation without throwing.
         */
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > 1.0e-8 * scale) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
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
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,271 chars)</summary>

```
package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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

        exercise(anchorX, ANCHOR_Y, true);

        int rounds = 1 + Math.max(0, Math.min(3, data.remainingBytes() / 4));
        for (int r = 0; r < rounds; r++) {
            int n = data.consumeInt(3, 40);
            double[] xs = new double[n];
            double[] ys = new double[n];

            double startX = data.consumeInt(-1000, 1000) / 10.0;
            double step = data.consumeInt(1, 50) / 10.0;
            double mean = startX + step * data.consumeInt(0, n - 1) + data.consumeInt(-50, 50) / 100.0;
            double sigma = data.consumeInt(1, 200) / 20.0;
            double norm = data.consumeInt(1, 10000) / 100.0;
            double baseline = data.consumeInt(0, 100) / 1000.0;

            for (int i = 0; i < n; i++) {
                xs[i] = startX + i * step;
                double z = (xs[i] - mean) / sigma;
                double y = baseline + norm * Math.exp(-0.5 * z * z);

                int ppm = data.consumeInt(-1000, 1000);
                y *= 1.0 + (ppm / 10000.0);
                if (y <= 0.0 || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = baseline + Math.abs(norm) * Math.exp(-0.5 * z * z);
                    if (y <= 0.0) {
                        y = 1.0e-12;
                    }
                }
                ys[i] = y;
            }

            exercise(xs, ys, true);
        }
    }

    private static void exercise(double[] xs, double[] ys, boolean validByConstruction) {
        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() is documented as the overload sibling of fit(double[] initialGuess),
         * and the patched code specifically changes fit() to delegate to fit(guess()).
         * Therefore, for the same observations, fit() must agree with fit(ParameterGuesser(...).guess()).
         * A patch that merely suppresses the exception or changes control flow to skip the real delegation
         * can violate this observable equivalence even if nothing throws.
         */
        try {
            GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitterA.addObservedPoint(xs[i], ys[i]);
                fitterB.addObservedPoint(xs[i], ys[i]);
            }

            double[] a = fitterA.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            double[] b = fitterB.fit(guess);

            if (!sameArray(a, b)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess())" +
                    " lhs=" + arrayToString(a) +
                    " rhs=" + arrayToString(b) +
                    " n=" + ys.length
                );
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
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
        String pkg = p == null ? "" : p.getName();
        return pkg.startsWith("org.apache.commons.math.exception");
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
            if (Math.abs(x - y) > 1.0e-9 * scale) {
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: INFO: Instrumented org.apache.commons.math.exception.util.MessageFactory (took 1 ms, size +11%)

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **27/27 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov; 27 raw fuzz log(s)**
<details><summary>▸ detail (~13,527 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 2293}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 2280}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 2286}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 2283}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 2278}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 2282}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 2279}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 2279}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 2283}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 2284}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 2289}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 2358}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 2280}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 2283}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 2279}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 2280}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 2293}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 2294}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 2275}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 2281}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 2286}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 2289}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 2278}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 2278}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 2284}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 2281}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_010', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_010', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/006_patch1-Math-58-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
