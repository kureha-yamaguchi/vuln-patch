# Pipeline trace — Math-25

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
+++ /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
@@ -324,6 +324,9 @@
                     // procedure cannot produce sensible results.
 
                 a = FastMath.sqrt(c1 / c2);
+//ACS's patch begin
+	if (c2==0.0){throw new MathIllegalStateException();}
+//ACS's patch end
                 omega = FastMath.sqrt(c2 / c3);
             }
         }
```

117 sequential steps — 33 LLM calls, 84 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **rule synthesis (LLM)** — proposes candidate RELATIONS — invariants / metamorphic properties a correct implementation must satisfy — from the documented contract
- **screen-fuzz-buggy** — compiles each candidate rule and fuzzes it many times on the BUGGY build; the output is checked/violated/fire-ratio
- **screen** — the keep/drop decision for each rule (direction-confirmed, selective, silent, or dropped by a lint/compile/cap)
- **screening-survivors** — the final set of rules kept — these are passed to replay / harness generation
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math3.optimization.fitting.HarmonicFitterTest', test_meth (~1,562 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math3.optimization.fitting.HarmonicFitterTest', test_method='testMath844', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/002_patch1-Math-25-ACS_c_rep001/Math_25_buggy/src/test/java/org/apache/commons/math3/optimization/fitting/HarmonicFitterTest.java', method_source='    @Test(expected=MathIllegalStateException.class)\\n    public void testMath844() {\\n        final double[] y = { 0, 1, 2, 3, 2, 1,\\n                             0, -1, -2, -3, -2, -1,\\n                             0, 1, 2, 3, 2, 1,\\n                             0, -1, -2, -3, -2, -1,\\n                             0, 1, 2, 3, 2, 1, 0 };\\n        final int len = y.length;\\n        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];\\n        for (int i = 0; i < len; i++) {\\n            points[i] = new WeightedObservedPoint(1, i, y[i]);\\n        }\\n\\n        final HarmonicFitter.ParameterGuesser guesser\\n            = new HarmonicFitter.ParameterGuesser(points);\\n\\n        // The guesser fails because the function is far from an harmonic\\n        // function: It is a triangular periodic function with amplitude 3\\n        // and period 12, and all sample points are taken at integer abscissae\\n        // so function values all belong to the integer subset {-3, -2, -1, 0,\\n        // 1, 2, 3}.\\n        guesser.guess();\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math3.optimization.fitting.HarmonicFitterTest::testMath844",
    "failure_message": "--- org.apache.commons.math3.optimization.fitting.HarmonicFitterTest::testMath844\njunit.framework.AssertionFailedError: Expected exception: org.apache.commons.math3.exception.MathIllegalStateException",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~14,031 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java\n+++ /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java\n@@ -324,6 +324,9 @@\n                     // procedure cannot produce sensible results.\n \n                 a = FastMath.sqrt(c1 / c2);\n+//ACS's patch begin\n+\tif (c2==0.0){throw new MathIllegalStateException();}\n+//ACS's patch end\n                 omega = FastMath.sqrt(c2 / c3);\n             }\n         }\n",
  "functions": [
    {
      "func_name": "guessAOmega",
      "func_signature": "private void guessAOmega()",
      "func_source": "        private void guessAOmega() {\n            // initialize the sums for the linear model between the two integrals\n            double sx2 = 0;\n            double sy2 = 0;\n            double sxy = 0;\n            double sxz = 0;\n            double syz = 0;\n\n            double currentX = observations[0].getX();\n            double currentY = observations[0].getY();\n            double f2Integral = 0;\n            double fPrime2Integral = 0;\n            final double startX = currentX;\n            for (int i = 1; i < observations.length; ++i) {\n                // one step forward\n                final double previousX = currentX;\n                final double previousY = currentY;\n                currentX = observations[i].getX();\n                currentY = observations[i].getY();\n\n                // update the integrals of f<sup>2</sup> and f'<sup>2</sup>\n                // considering a linear model for f (and therefore constant f')\n                final double dx = currentX - previousX;\n                final double dy = currentY - previousY;\n                final double f2StepIntegral =\n                    dx * (previousY * previousY + previousY * currentY + currentY * currentY) / 3;\n                final double fPrime2StepIntegral = dy * dy / dx;\n\n                final double x = currentX - startX;\n                f2Integral += f2StepIntegral;\n                fPrime2Integral += fPrime2StepIntegral;\n\n                sx2 += x * x;\n                sy2 += f2Integral * f2Integral;\n                sxy += x * f2Integral;\n                sxz += x * fPrime2Integral;\n                syz += f2Integral * fPrime2Integral;\n            }\n\n            // compute the amplitude and pulsation coefficients\n            double c1 = sy2 * sxz - sxy * syz;\n            double c2 = sxy * sxz - sx2 * syz;\n            double c3 = sx2 * sy2 - sxy * sxy;\n            if ((c1 / c2 < 0) || (c2 / c3 < 0)) {\n                final int last = observations.length - 1;\n                // Range of the observations, assuming that the\n                // observations are sorted.\n                final double xRange = observations[last].getX() - observations[0].getX();\n                if (xRange == 0) {\n                    throw new ZeroException();\n                }\n                omega = 2 * Math.PI / xRange;\n\n                double yMin = Double.POSITIVE_INFINITY;\n                double yMax = Double.NEGATIVE_INFINITY;\n                for (int i = 1; i < observations.length; ++i) {\n                    final double y = observations[i].getY();\n                    if (y < yMin) {\n                        yMin = y;\n                    }\n                    if (y > yMax) {\n                        yMax = y;\n                    }\n                }\n                a = 0.5 * (yMax - yMin);\n            } else {\n                    // In some ill-conditioned cases (cf. MATH-844), the guesser\n                    // procedure cannot produce sensible results.\n\n                a = FastMath.sqrt(c1 / c2);\n                omega = FastMath.sqrt(c2 / c3);\n            }\n        }",
      "func_class": "HarmonicFitter.ParameterGuesser",
      "func_class_fq": "org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser",
      "func_param_types": [],
      "fi_name": "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "overload_types": [
        []
      ],
      "xrefs": [
        "public double[] guess() {\n            sortObservations();\n            guessAOmega();\n            guessPhi();\n            return new double[] { a, omega, phi };\n        }"
      ],
      "xref_names": [
        "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guess()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[0].getX()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[0].getY()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[i].getX()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[i].getY()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[last].getX()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[0].getX()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "[org.apache.commons.math3.exception.ZeroException].<init>()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "observations[i].getY()"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "[org.apache.commons.math3.util.FastMath].sqrt(org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser)"
        ],
        [
          "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
          "[org.apache.commons.math3.util.FastMath].sqrt(org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser)"
        ],
        [
          "[org.apache.commons.math3.exception.ZeroException].<init>()",
          "[org.apache.commons.math3.exception.ZeroException].<init>(org.apache.commons.math3.exception.ZeroException)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()": 0,
        "observations[0].getX()": 1,
        "observations[0].getY()": 1,
        "observations[i].getX()": 1,
        "observations[i].getY()": 1,
        "observations[last].getX()": 1,
        "[org.apache.commons.math3.exception.ZeroException].<init>()": 1,
        "[org.apache.commons.math3.util.FastMath].sqrt(org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser)": 1,
        "[org.apache.commons.math3.exception.ZeroException].<init>(org.apache.commons.math3.exception.ZeroException)": 2
      },
      "reachable": [
        "observations[0].getX()",
        "observations[0].getY()",
        "observations[i].getX()",
        "observations[i].getY()",
        "observations[last].getX()",
        "[org.apache.commons.math3.exception.ZeroException].<init>()",
        "[org.apache.commons.math3.util.FastMath].sqrt(org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser)",
        "[org.apache.commons.math3.exception.ZeroException].<init>(org.apache.commons.math3.exception.ZeroException)",
        "[org.apache.commons.math3.geometry.euclidean.oned.Vector1D].getX()",
        "[org.apache.commons.math3.geometry.euclidean.threed.Vector3D].getY()"
      ],
      "related_callees": [
        {
          "name": "getX",
          "source_file": "WeightedObservedPoint.java",
          "signature": "public double getX()",
          "source": "    public double getX() {\n        return x;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getY",
          "source_file": "WeightedObservedPoint.java",
          "signature": "public double getY()",
          "source": "    public double getY() {\n        return y;\n    }",
          "is_abstract": false,
          "impls": []
        }
      ],
      "field_siblings": [
        {
          "name": "guess",
          "signature": "public double[] guess()",
          "shared_fields": [
            "a",
            "omega"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Estimate a first guess of the coefficients. @return the guessed coefficients, in the following order: <ul> <li>Amplitude</li> <li>Angular frequency</li> <li>Phase</li> </ul>"
        },
        {
          "name": "guessPhi",
          "signature": "private void guessPhi()",
          "shared_fields": [
            "observations",
            "omega"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Estimate a first guess of the phase."
        },
        {
          "name": "ParameterGuesser",
          "signature": "public ParameterGuesser(WeightedObservedPoint[] observations)",
          "shared_fields": [
            "observations"
          ],
          "is_constructor": true,
          "source": "        public ParameterGuesser(WeightedObservedPoint[] observations) {\n            if (observations.length < 4) {\n                throw new NumberIsTooSmallException(LocalizedFormats.INSUFFICIENT_OBSERVED_POINTS_IN_SAMPLE,\n                                                    observations.length, 4, true);\n            }\n\n            this.observations = observations.clone();\n        }",
          "javadoc": "Simple constructor. @param observations sampled observations @throws NumberIsTooSmallException if the sample is too short or if the first guess cannot be computed."
        },
        {
          "name": "CurveFitter.CurveFitter",
          "signature": "public CurveFitter(DifferentiableMultivariateVectorOptimizer optimizer)",
          "shared_fields": [
            "observations"
          ],
          "is_constructor": true,
          "source": "    public CurveFitter(final DifferentiableMultivariateVectorOptimizer optimizer) {\n        this.optimizer = optimizer;\n        observations = new ArrayList<WeightedObservedPoint>();\n    }",
          "javadoc": "Simple constructor. @param optimizer optimizer to use for the fitting"
        },
        {
          "name": "sortObservations",
          "signature": "private void sortObservations()",
          "shared_fields": [
            "observations"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Sort the observations with respect to the abscissa."
        }
      ]
    }
  ],
  "package": "org.apache.commons.math3.optimization.fitting",
  "root_cause_reachable": [
    "ZeroException.<init>()",
    "FastMath.sqrt",
    "ZeroException.<init>(ZeroException)",
    "Vector1D.getX",
    "Vector3D.getY"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import org.apache.commons.math3.optimization.DifferentiableMultivariateVectorOptimizer;",
    "import org.apache.commons.math3.analysis.function.HarmonicOscillator;",
    "import org.apache.commons.math3.exception.ZeroException;",
    "import org.apache.commons.math3.exception.NumberIsTooSmallException;",
    "import org.apache.commons.math3.exception.MathIllegalStateException;",
    "import org.apache.commons.math3.exception.util.LocalizedFormats;",
    "import org.apache.commons.math3.util.FastMath;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "observations[0].getX()"
    ],
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "observations[0].getY()"
    ],
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "observations[i].getX()"
    ],
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "observations[i].getY()"
    ],
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "observations[last].getX()"
    ],
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "[org.apache.commons.math3.exception.ZeroException].<init>()"
    ],
    [
      "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()",
      "[org.apache.commons.math3.util.FastMath].sqrt(org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser)"
    ],
    [
      "[org.apache.commons.math3.exception.ZeroException].<init>()",
      "[org.apache.commons.math3.exception.ZeroException].<init>(org.apache.commons.math3.exception.ZeroException)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser].guessAOmega()": 0,
    "observations[0].getX()": 1,
    "observations[0].getY()": 1,
    "observations[i].getX()": 1,
    "observations[i].getY()": 1,
    "observations[last].getX()": 1,
    "[org.apache.commons.math3.exception.ZeroException].<init>()": 1,
    "[org.apache.commons.math3.util.FastMath].sqrt(org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser)": 1,
    "[org.apache.commons.math3.exception.ZeroException].<init>(org.apache.commons.math3.exception.ZeroException)": 2
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
**output:** **documented exceptions: {'HarmonicFitter#fit': ['NumberIsTooSmallException', 'ZeroException'], 'HarmonicFitter#guessAOmega': ['MathIllegalStateException', 'ZeroException']}**
- detail: {'documented': {'HarmonicFitter#fit': ['NumberIsTooSmallException', 'ZeroException'], 'HarmonicFitter#guessAOmega': ['MathIllegalStateException', 'ZeroException']}}

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~43,267 chars)</summary>

**[system]**
```
You are a software-verification expert. Given a patched Java method and its API, you propose RELATIONS that must hold for EVERY correct implementation — never merely for the reference or for the tested inputs. Two kinds only:
  * METAMORPHIC: two real API calls whose results must agree (or relate in a fixed way) for any valid input — e.g. parse(format(x)) == x; f(a,b) == f(b,a) when documented commutative; reduce(reduce(x)) == reduce(x).
  * INVARIANT: a property the output must satisfy for every valid input, read straight off the documented contract — e.g. a reduced fraction has gcd(numerator,denominator)==1; a returned index is within bounds.
Each relation must be SOUND: if any correct implementation could violate it, do not propose it. Avoid comparisons to a different library, and avoid boundary-fragile claims (division by zero, NaN, overflow, empty/null) unless you explicitly exclude those inputs.
```
**[user]**
```
THE BUG'S OWN FAILING TEST (most trusted — this is ground truth for the correct behaviour on its inputs). The patch under analysis MAY be an overfit; where this test pins a direction, the TEST is right and the patched code is not evidence. Read the exact call it makes and the value it expects, and make your relations agree with it:
<failing_test>
// org.apache.commons.math3.optimization.fitting.HarmonicFitterTest::testMath844
    @Test(expected=MathIllegalStateException.class)
    public void testMath844() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser guesser
            = new HarmonicFitter.ParameterGuesser(points);

        // The guesser fails because the function is far from an harmonic
        // function: It is a triangular periodic function with amplitude 3
        // and period 12, and all sample points are taken at integer abscissae
        // so function values all belong to the integer subset {-3, -2, -1, 0,
        // 1, 2, 3}.
        guesser.guess();
    }
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.apache.commons.math3.optimization.fitting.HarmonicFitterTest::testMath844
// junit.framework.AssertionFailedError: Expected exception: org.apache.commons.math3.exception.MathIllegalStateException
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  fit(double[] initialGuess) / ()

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): fit, guess
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
        private void guessAOmega() {
            // initialize the sums for the linear model between the two integrals
            double sx2 = 0;
            double sy2 = 0;
            double sxy = 0;
            double sxz = 0;
            double syz = 0;

            double currentX = observations[0].getX();
            double currentY = observations[0].getY();
            double f2Integral = 0;
            double fPrime2Integral = 0;
            final double startX = currentX;
            for (int i = 1; i < observations.length; ++i) {
                // one step forward
                final double previousX = currentX;
                final double previousY = currentY;
                currentX = observations[i].getX();
                currentY = observations[i].getY();

                // update the integrals of f<sup>2</sup> and f'<sup>2</sup>
                // considering a linear model for f (and therefore constant f')
                final double dx = currentX - previousX;
                final double dy = currentY - previousY;
                final double f2StepIntegral =
                    dx * (previousY * previousY + previousY * currentY + currentY * currentY) / 3;
                final double fPrime2StepIntegral = dy * dy / dx;

                final double x = currentX - startX;
                f2Integral += f2StepIntegral;
                fPrime2Integral += fPrime2StepIntegral;

                sx2 += x * x;
                sy2 += f2Integral * f2Integral;
                sxy += x * f2Integral;
                sxz += x * fPrime2Integral;
                syz += f2Integral * fPrime2Integral;
            }

            // compute the amplitude and pulsation coefficients
            double c1 = sy2 * sxz - sxy * syz;
            double c2 = sxy * sxz - sx2 * syz;
            double c3 = sx2 * sy2 - sxy * sxy;
            if ((c1 / c2 < 0) || (c2 / c3 < 0)) {
                final int last = observations.length - 1;
                // Range of the observations, assuming that the
                // observations are sorted.
                final double xRange = observations[last].getX() - observations[0].getX();
                if (xRange == 0) {
                    throw new ZeroException();
                }
                omega = 2 * Math.PI / xRange;

                double yMin = Double.POSITIVE_INFINITY;
                double yMax = Double.NEGATIVE_INFINITY;
                for (int i = 1; i < observations.length; ++i) {
                    final double y = observations[i].getY();
                    if (y < yMin) {
                        yMin = y;
                    }
                    if (y > yMax) {
                        yMax = y;
                    }
                }
                a = 0.5 * (yMax - yMin);
            } else {
                    // In some ill-conditioned cases (cf. MATH-844), the guesser
                    // procedure cannot produce sensible results.

                a = FastMath.sqrt(c1 / c2);
                omega = FastMath.sqrt(c2 / c3);
            }
        }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="HarmonicFitter" role="patched">
     * Class that implements a curve fitting specialized for sinusoids.
     * Harmonic fitting is a very simple case of curve fitting. The
     * estimated coefficients are the amplitude a, the pulsation &omega; and
     * the phase &phi;: <code>f (t) = a cos (&omega; t + &phi;)</code>. They are
     * searched by a least square estimator initialized with a rough guess
     * based on integrals.
     * @version $Id$
     * @since 2.0
public class HarmonicFitter extends CurveFitter<HarmonicOscillator.Parametric> {
     * Simple constructor.
     * @param optimizer Optimizer to use for the fitting.
    public HarmonicFitter(final DifferentiableMultivariateVectorOptimizer optimizer) {
        super(optimizer);
    }
     * Fit an harmonic function to the observed points.
     * @param initialGuess First guess values in the following order:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
     * @return the parameters of the harmonic function that best fits the
     * observed points (in the same order as above).
    public double[] fit(double[] initialGuess) { … }
     * Fit an harmonic function to the observed points.
     * An initial guess will be automatically computed.
     * @return the parameters of the harmonic function that best fits the
     * observed points (see the other {@link #fit(double[]) fit} method.
     * @throws NumberIsTooSmallException if the sample is too short for the
     * the first guess to be computed.
     * @throws ZeroException if the first guess cannot be computed because
     * the abscissa range is zero.
    public double[] fit() { … }
     * This class guesses harmonic coefficients from a sample.
     * <p>The algorithm used to guess the coefficients is as follows:</p>
     * <p>We know f (t) at some sampling points t<sub>i</sub> and want to find a,
     * &omega; and &phi; such that f (t) = a cos (&omega; t + &phi;).
     * </p>
     * <p>From the analytical expression, we can compute two primitives :
     * <pre>
     * If2  (t) = &int; f<sup>2</sup>  = a<sup>2</sup> &times; [t + S (t)] / 2
     * If'2 (t) = &int; f'<sup>2</sup> = a<sup>2</sup> &omega;<sup>2</sup> &times; [t - S (t)] / 2
     * where S (t) = sin (2 (&omega; t + &phi;)) / (2 &omega;)
     * </pre>
     * </p>
     * …
    public static class ParameterGuesser { … }
}
</class>
<class name="CurveFitter" role="supertype-contract">
class TheoreticalValuesFunction
        implements DifferentiableMultivariateVectorFunction {
     * Function to fit.
    private final ParametricUnivariateFunction f;
     * Simple constructor.
     * @param f function to fit.
    public TheoreticalValuesFunction(final ParametricUnivariateFunction f) {
            this.f = f;
        }
     * {@inheritDoc}
    public MultivariateMatrixFunction jacobian() { … }
     * {@inheritDoc}
    public double[] value(double[] point) { … }
}
</class>
<class name="WeightedObservedPoint" role="test-subject">
     * This class is a simple container for weighted observed point in
     * {@link CurveFitter curve fitting}.
     * <p>Instances of this class are guaranteed to be immutable.</p>
     * @version $Id$
     * @since 2.0
public class WeightedObservedPoint implements Serializable {
     * Serializable version id.
    private static final long serialVersionUID = 5306874947404636157L;
     * Weight of the measurement in the fitting process.
    private final double weight;
     * Abscissa of the point.
    private final double x;
     * Observed value of the function at x.
    private final double y;
     * Simple constructor.
     * @param weight weight of the measurement in the fitting process
     * @param x abscissa of the measurement
     * @param y ordinate of the measurement
    public WeightedObservedPoint(final double weight, final double x, final double y) {
        this.weight = weight;
        this.x      = x;
        this.y      = y;
    }
     * Get the weight of the measurement in the fitting process.
     * @return weight of the measurement in the fitting process
    public double getWeight() { … }
     * Get the abscissa of the point.
     * @return abscissa of the point
    public double getX() { … }
     * Get the observed value of the function at x.
     * @return observed value of the function at x
    public double getY() { … }
}
</class>
<class name="MathIllegalStateException" role="test-subject">
     * Base class for all exceptions that signal a mismatch between the
     * current state and the user's expectations.
     * @since 2.2
     * @version $Id$
public class MathIllegalStateException extends IllegalStateException
    implements ExceptionContextProvider {
     * Serializable version Id.
    private static final long serialVersionUID = -6024911025449780478L;
     * Context.
    private final ExceptionContext context;
     * Simple constructor.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Localizable pattern, Object ... args) {
        context = new ExceptionContext(this);
        context.addMessage(pattern, args);
    }
     * Simple constructor.
     * @param cause Root cause.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Throwable cause, Localizable pattern, Object ... args) {
        super(cause);
        context = new ExceptionContext(this);
        context.addMessage(pattern, args);
    }
     * Default constructor.
    public MathIllegalStateException() {
        this(LocalizedFormats.ILLEGAL_STATE);
    }
     * {@inheritDoc}
    public ExceptionContext getContext() { … }
     * {@inheritDoc}
    @Override public String getMessage() { … }
     * {@inheritDoc}
    @Override public String getLocalizedMessage() { … }
}
</class>
<class name="FastMath" role="collaborator">
     * Faster, more accurate, portable alternative to {@link Math} and
     * {@link StrictMath} for large scale computation.
     * <p>
     * FastMath is a drop-in replacement for both Math and StrictMath. This
     * means that for any method in Math (say {@code Math.sin(x)} or
     * {@code Math.cbrt(y)}), user can directly change the class and use the
     * methods as is (using {@code FastMath.sin(x)} or {@code FastMath.cbrt(y)}
     * in the previous example).
     * </p>
     * <p>
     * FastMath speed is achieved by relying heavily on optimizing compilers
     * to native code present in many JVMs today and use of large tables.
     * …
public class FastMath {
     * Archimede's constant PI, ratio of circle circumference to diameter.
    public static final double PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9;
     * Napier's constant e, base of the natural logarithm.
    public static final double E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8;
     * Index of exp(0) in the array of integer exponentials. */
     * static final int EXP_INT_TABLE_MAX_INDEX = 750;
     * /** Length of the array of integer exponentials. */
     * static final int EXP_INT_TABLE_LEN = EXP_INT_TABLE_MAX_INDEX * 2;
     * /** Logarithm table length. */
     * static final int LN_MANT_LEN = 1024;
     * /** Exponential fractions table length. */
     * static final int EXP_FRAC_TABLE_LEN = 1025; // 0, 1/1024, ... 1024/1024
     * /** Indicator for tables initialization.
     * <p>
     * This compile-time constant should be set to true only if one explicitly
     * wants to compute the tables at class loading time instead of using the
     * …
    private static final boolean RECOMPUTE_TABLES_AT_RUNTIME = false;
     * log(2) (high bits).
    private static final double LN_2_A = 0.693147063255310059;
     * log(2) (low bits).
    private static final double LN_2_B = 1.17304635250823482e-7;
     * Coefficients for log, when input 0.99 < x < 1.01.
    private static final double LN_QUICK_COEF[][] = { … }
     * Coefficients for log in the range of 1.0 < x < 1.0 + 2^-10.
    private static final double LN_HI_PREC_COEF[][] = { … }
     * Sine, Cosine, Tangent tables are for 0, 1/8, 2/8, ... 13/8 = PI/2 approx.
    private static final int SINE_TABLE_LEN = 14;
     * Sine table (high bits).
    private static final double SINE_TABLE_A[] = { … }
     * Sine table (low bits).
    private static final double SINE_TABLE_B[] = { … }
     * Cosine table (high bits).
    private static final double COSINE_TABLE_A[] = { … }
     * Cosine table (low bits).
    private static final double COSINE_TABLE_B[] = { … }
     * Tangent table, used by atan() (high bits).
    private static final double TANGENT_TABLE_A[] = { … }
     * Tangent table, used by atan() (low bits).
    private static final double TANGENT_TABLE_B[] = { … }
     * Bits of 1/(2*pi), need for reducePayneHanek().
    private static final long RECIP_2PI[] = new long[] { … }
     * Bits of pi/4, need for reducePayneHanek().
    private static final long PI_O_4_BITS[] = new long[] { … }
     * Eighths.
     * This is used by sinQ, because its faster to do a table lookup than
     * a multiply in this time-critical routine
    private static final double EIGHTHS[] = { … }
     * Table of 2^((n+2)/3)
    private static final double CBRTTWO[] = { … }
     * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
     * Equivalent to 2^30.
    private static final long HEX_40000000 = 0x40000000L;
     * Mask used to clear low order 30 bits
    private static final long MASK_30BITS = -1L - (HEX_40000000 -1);
     * 2^52 - double numbers this large must be integral (no fraction) or NaN or Infinite
    private static final double TWO_POWER_52 = 4503599627370496.0;
     * Constant: {@value}.
    private static final double F_1_3 = 1d / 3d;
     * Constant: {@value}.
    private static final double F_1_5 = 1d / 5d;
     * Constant: {@value}.
    private static final double F_1_7 = 1d / 7d;
     * Constant: {@value}.
    private static final double F_1_9 = 1d / 9d;
     * Constant: {@value}.
    private static final double F_1_11 = 1d / 11d;
     * Constant: {@value}.
    private static final double F_1_13 = 1d / 13d;
     * Constant: {@value}.
    private static final double F_1_15 = 1d / 15d;
     * Constant: {@value}.
    private static final double F_1_17 = 1d / 17d;
     * Constant: {@value}.
    private static final double F_3_4 = 3d / 4d;
     * Constant: {@value}.
    private static final double F_15_16 = 15d / 16d;
     * Constant: {@value}.
    private static final double F_13_14 = 13d / 14d;
     * Constant: {@value}.
    private static final double F_11_12 = 11d / 12d;
     * Constant: {@value}.
    private static final double F_9_10 = 9d / 10d;
     * Constant: {@value}.
    private static final double F_7_8 = 7d / 8d;
     * Constant: {@value}.
    private static final double F_5_6 = 5d / 6d;
     * Constant: {@value}.
    private static final double F_1_2 = 1d / 2d;
     * Constant: {@value}.
    private static final double F_1_4 = 1d / 4d;
     * Private Constructor
    private FastMath() {}
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     * @param d the value to split
     * @return the high order part of the mantissa
    private static double doubleHighPart(double d) { … }
     * Natural logarithm.
     * @param x   a double
     * @return log(x)
    public static double log(final double x) { … }
     * Internal helper method for natural logarithm function.
     * @param x original argument of the natural logarithm function
     * @param hiPrec extra bits of precision on output (To Be Confirmed)
     * @return log(x)
    private static double log(final double x, final double[] hiPrec) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="ZeroException" role="collaborator">
     * Exception to be thrown when zero is provided where it is not allowed.
     * @since 2.2
     * @version $Id$
public class ZeroException extends MathIllegalNumberException {
     * Serializable version identifier
    private static final long serialVersionUID = -1960874856936000015L;
     * Construct the exception.
    public ZeroException() {
        this(LocalizedFormats.ZERO_NOT_ALLOWED);
    }
     * Construct the exception with a specific context.
     * @param specific Specific context pattern.
     * @param arguments Arguments.
    public ZeroException(Localizable specific, Object ... arguments) {
        super(specific, 0, arguments);
    }
}
</class>
<class name="NumberIsTooSmallException" role="collaborator">
     * Exception to be thrown when a number is too small.
     * @since 2.2
     * @version $Id$
public class NumberIsTooSmallException extends MathIllegalNumberException {
     * Serializable version Id.
    private static final long serialVersionUID = -6100997100383932834L;
     * Higher bound.
    private final Number min;
     * Whether the maximum is included in the allowed range.
    private final boolean boundIsAllowed;
     * Construct the exception.
     * @param wrong Value that is smaller than the minimum.
     * @param min Minimum.
     * @param boundIsAllowed Whether {@code min} is included in the allowed range.
    public NumberIsTooSmallException(Number wrong, Number min, boolean boundIsAllowed) {
        this(boundIsAllowed ?
             LocalizedFormats.NUMBER_TOO_SMALL :
             LocalizedFormats.NUMBER_TOO_SMALL_BOUND_EXCLUDED,
             wrong, min, boundIsAllowed);
    }
     * Construct the exception with a specific context.
     * @param specific Specific context pattern.
     * @param wrong Value that is smaller than the minimum.
     * @param min Minimum.
     * @param boundIsAllowed Whether {@code min} is included in the allowed range.
    public NumberIsTooSmallException(Localizable specific, Number wrong, Number min, boolean boundIsAllowed) {
        super(specific, wrong, min);

        this.min = min;
        this.boundIsAllowed = boundIsAllowed;
    }
     * @return {@code true} if the minimum is included in the allowed range.
    public boolean getBoundIsAllowed() { … }
     * @return the minimum.
    public Number getMin() { … }
}
</class>
<class name="HarmonicOscillator" role="collaborator">
     * <a href="http://en.wikipedia.org/wiki/Harmonic_oscillator">
     * simple harmonic oscillator</a> function.
     * @since 3.0
     * @version $Id$
public class HarmonicOscillator implements DifferentiableUnivariateFunction {
     * Amplitude.
    private final double amplitude;
     * Angular frequency.
    private final double omega;
     * Phase.
    private final double phase;
     * Harmonic oscillator function.
     * @param amplitude Amplitude.
     * @param omega Angular frequency.
     * @param phase Phase.
    public HarmonicOscillator(double amplitude, double omega, double phase) {
        this.amplitude = amplitude;
        this.omega = omega;
        this.phase = phase;
    }
     * {@inheritDoc}
    public double value(double x) { … }
     * {@inheritDoc}
    public UnivariateFunction derivative() { … }
     * Parametric function where the input array contains the parameters of
     * the harmonic oscillator function, ordered as follows:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
    public static class Parametric implements ParametricUnivariateFunction { … }
     * @param xTimesOmegaPlusPhase {@code omega * x + phase}.
     * @param amplitude Amplitude.
     * @return the value of the harmonic oscillator function at {@code x}.
    private static double value(double xTimesOmegaPlusPhase, double amplitude) { … }
}
</class>
<class name="DifferentiableMultivariateVectorOptimizer" role="collaborator">
     * This interface represents an optimization algorithm for
     * {@link DifferentiableMultivariateVectorFunction vectorial differentiable
     * objective functions}.
     * @version $Id$
     * @since 3.0
public interface DifferentiableMultivariateVectorOptimizer
    extends BaseMultivariateVectorOptimizer<DifferentiableMultivariateVectorFunction> {

}
</class>
THE PATCH-CHANGED CLASS: HarmonicFitter. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
+++ /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
@@ -324,6 +324,9 @@
                     // procedure cannot produce sensible results.
 
                 a = FastMath.sqrt(c1 / c2);
+//ACS's patch begin
+	if (c2==0.0){throw new MathIllegalStateException();}
+//ACS's patch end
                 omega = FastMath.sqrt(c2 / c3);
             }
         }

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    ADDED  : if (c2==0.0){throw new MathIllegalStateException();}
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
Class that implements a curve fitting specialized for sinusoids.

Harmonic fitting is a very simple case of curve fitting. The
estimated coefficients are the amplitude a, the pulsation &omega; and
the phase &phi;: <code>f (t) = a cos (&omega; t + &phi;)</code>. They are
searched by a least square estimator initialized with a rough guess
based on integrals.

@version $Id$
@since 2.0
/
public class HarmonicFitter extends CurveFitter<HarmonicOscillator.Parametric> {
/**
Simple constructor.
@param optimizer Optimizer to use for the fitting.
/
public HarmonicFitter(final DifferentiableMultivariateVectorOptimizer optimizer) {
super(optimizer);
}

/**
Fit an harmonic function to the observed points.

@param initialGuess First guess values in the following order:
<ul>
<li>Amplitude</li>
<li>Angular frequency</li>
<li>Phase</li>
</ul>
@return the parameters of the harmonic function that best fits the
observed points (in the same order as above).
/
public double[] fit(double[] initialGuess) {
return fit(new HarmonicOscillator.Parametric(), initialGuess);
}

/**
Fit an harmonic function to the observed points.
An initial guess will be automatically computed.

@return the parameters of the harmonic function that best fits the
observed points (see the other {@link #fit(double[]) fit} method.
@throws NumberIsTooSmallException if the sample is too short for the
the first guess to be computed.
@throws ZeroException if the first guess cannot be computed because
the abscissa range is zero.
/
p
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import org.apache.commons.math3.optimization.DifferentiableMultivariateVectorOptimizer;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    ParameterGuesser, guess, WeightedObservedPoint

ANCHOR REQUIREMENT — READ FIRST. The patch changed a SPECIFIC expression in a SPECIFIC method. An overfit usually differs from a correct implementation WHERE the code was changed (the one exception: a method the FAILING TEST reads can stay wrong when the patch edited elsewhere — that is what the secondary list below is for). Your FIRST relation MUST directly constrain the OUTPUT of a changed method, exercising the exact input the change is about — the prefix / sign / boundary value / edge case named in the changed condition — and asserting only what that method's OWN documented contract guarantees there. (E.g. if the change adds a guard on inputs starting with a token, your relation must feed such inputs and assert the documented result; if the change rewrites a formula the javadoc states, assert the method's output equals that formula recomputed independently.) Do NOT fill the slots with generic round-trip or bound properties of untouched sibling methods.
If the changed expression is a NUMERIC formula, a correct fix may reassociate it (a*(b/c) vs (a*b)/c) and differ by rounding, so assert APPROXIMATE equality with a GENEROUS relative tolerance — never exact ==, or you will falsely flag a correct reassociation. Use `Math.abs(a-b) <= tol * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)))` with tol AT LEAST 1e-9; and when the computation multiplies or sums large integer/long intermediates (products near or above 2^31, or billion-scale inputs), use tol = 1e-6 — at those magnitudes the library's OWN double rounding exceeds 1e-9, so a tighter tolerance false-fires on a correct patch. A too-tight tolerance is as unsound as exact ==; the real bug you are hunting diverges by a LARGE amount (e.g. a sign flip or a value 100x off), not by rounding.
STANDING STRATEGY — DOCUMENTED FORMULAS (checked first, before anything else): scan the javadoc of the touched class's numeric getters AND of every class shown with role="test-subject" (the classes the failing test itself constructs and reads — the defect's observable surface often lives there, not in the patched class) for a stated closed-form formula ('the value equals a * b / c of the constructor parameters', 'returns x * (1 - x)', ...). If ANY such formula exists, your FIRST relation MUST be that formula: recompute it independently from the object's own parameters and compare with the generous magnitude-scaled tolerance above. A documented formula is the strongest relation class there is — it is deterministic, it holds for every correct implementation by definition, and a patch that leaves the value wrong anywhere in the domain cannot pass it. Only when no documented formula exists does the anchor requirement below decide your first relation.
STANDING STRATEGY — DOCUMENTED @THROWS (checked second): scan the touched method's javadoc (and its parent declaration's, if shown) for declared exceptions — '@throws X if/when <condition>'. For EACH such declaration whose triggering input you can construct (a null argument, a malformed string, an out-of-range value), one relation MUST construct exactly that input and assert the documented throw: call the method, and if it COMPLETES NORMALLY (or throws a different, non-subclass exception), throw the violation. MANDATORY check shape for these documented-throw relations — catching broadly and returning DEFEATS the relation (a patch whose reordered guard throws the WRONG exception class then silently skips instead of firing):
    try { obj.method(rejectedInput); violated = true; /* completed normally */ }
    catch (TheDocumentedException ok) { /* contract honoured */ }
    catch (Throwable t) { violated = true; /* wrong exception class — also a contract violation */ }
  then throw the violation OUTSIDE the try when `violated` is set, naming what actually happened (completed vs wrong class <type>). This is a tripwire: it stays silent on any build that honours the contract and fires on a patch whose added or reordered guard swallows the documented rejection — a classic overfit shape (a guard inserted BEFORE an argument check silently returns where the contract says throw). Use ONLY declared @throws with their stated conditions — never invent an undocumented rejection; a subclass of the declared exception counts as honouring the contract. ISOLATE THE ONE VIOLATION: build the input so it breaks ONLY this @throws condition and satisfies every OTHER documented precondition — when several are broken at once a correct implementation may throw a different documented exception first, making a wrong-class assertion unsound (it fires on correct code). IMPORTANT — vary the RECEIVER and the OTHER arguments across their special states while holding the rejected input fixed: an overfit often reorders the check behind a guard that only triggers for a special receiver state (a NaN/empty/zero receiver), so a plain rejected input alone misses it — assert the documented throw on an ORDINARY receiver AND on each special-state receiver the class admits (empty, extreme, identity, NaN), since a patch may return early only in the special-state case.
STANDING STRATEGY — REJECTION INDEPENDENCE (receiver-state variation): for ANY relation that asserts a rejection/error contract — asking for something absent, invalid, or out of range MUST throw — also emit a companion relation that holds the probe input FIXED and instead varies the RECEIVER / CONTAINER state around it: its contents, its size, which elements are installed / registered / present, whether it is empty or populated, freshly built or already mutated. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so the documented throw MUST fire identically across every such state; a patch that makes the rejection conditional on the container's contents or size is exactly the overfit this companion catches. Build each varied state by construction through the real public API (draw the variation from `data`) and assert the SAME documented throw on each — one extra relation per rejection contract.
    ORDERING (mandatory, and the half most checks get wrong): re-run the rejection probe AFTER EVERY state-changing call in the check body, not once on the freshly built receiver. Mutate, then probe; mutate again, then probe again — asserting the same documented outcome each time. A probe that runs before the mutations has only ever observed the state a state-conditional patch still gets right, so the check is silent on both builds no matter how well the container is fuzzed; the divergence lives in the state AFTER the container has been filled, emptied, or left with a gap. Draw WHICH slot/key/index each mutation targets, and how many mutations happen, from `data` (never from literals), so the sequence of states the probe sees varies across iterations. This is checked mechanically at screening: a rejection probe with no probe after the receiver's last state change is reported as an ordering blind spot.
STANDING STRATEGY — STRUCTURE FROM DATA (how the varied state is built): whenever a relation's property depends on the RECEIVER / CONTAINER state — a rejection contract, an index/position/lookup contract, a size/emptiness contract — the container's STRUCTURE must be drawn from `data`, not written as compile-time constants. Structure means: HOW MANY elements it holds, AT WHICH indices or keys they are installed, whether there are GAPS/HOLES between them, whether it is EMPTY or populated, whether it is freshly built or already mutated, and in what ORDER those operations happened. Concretely, the element count, every install index, every loop bound and every branch that decides whether an element is added at all must come from a `data.consume*` draw (e.g. `int n = data.consumeInt(0, 4);` then install at `data.consumeInt(0, n)`), NOT from literals. Fuzzing only the labels/names/values INSIDE a fixed structure is COSMETIC: every iteration rebuilds the same shape, so a check that looks fuzzed is really a single hard-coded scenario, and a patch that misbehaves only for a different shape — a container with a hole in it, an empty one, a larger one, an element installed at a different position — is never exercised, on either build. This is checked mechanically at screening: a receiver-state relation whose construction consumes no fuzz bytes in any index, count or loop bound is reported as a structural blind spot.
Propose up to 6 relations. COVERAGE REQUIREMENT (R2): the set must cover DISTINCT documented observables — after the documented-formula and @throws slots above, spread the remaining slots across different contract sources (a formula, a throws clause, a state/read-only guarantee, a sibling-agreement rule) rather than proposing several variants of one observable. Do NOT anchor every relation on the patched method alone: a patch to one method can leave a DOCUMENTED observable of the test-subject class wrong away from the trigger, and a set of six relations about the patched method's own edge cases will all stay quiet while that documented observable diverges. At least ONE relation must assert a documented guarantee of a test-subject class when any is shown. And when the context lists SAME-NAME OVERLOADS or a METHOD FAMILY that contains the patched method, write one agreement relation PER SIBLING PAIR (patched method, sibling) for up to two siblings — prioritising the siblings that share the most behaviour with the patched method (same output shape, documented shared semantics), NOT just any family member: a patch that changes one member while a close sibling keeps the shared behaviour breaks exactly that pairwise agreement, and an agreement relation aimed at a distant family member can hold on a patch that a close-sibling relation convicts. Each pair relation: the two members given equivalent inputs must produce the documented consistent outcome — identical results up to the documented difference between them.
DEEP-DIVE PROTOCOL — do this enumeration BEFORE proposing: (1) LIST every documented observable in scope — each stated formula, each declared @throws, each documented range/format, each documented family agreement, each read-only/state guarantee — from the patched class AND every test-subject class shown, each with the doc line that states it; (2) MARK which of these the patch text could plausibly affect, directly or through shared state; (3) SPEND your slots on the marked ones per the coverage requirement above. Relations aimed at observables no documentation states are last-resort fills, never first choices. For each relation, give:
  - name: a short slug
  - kind: metamorphic | invariant
  - contract: the ONE documented guarantee that makes it hold for every correct implementation (javadoc sentence or a visible code invariant)
  - input: how to build a VALID input by construction (magnitudes, signs, shapes) that a correct implementation MUST accept — so a violation is a real defect, not a rejection of bad input.
    From the changed condition in the patch, the documented ranges (@param/@throws), and any bounds/size accessors this class exposes, identify the input-domain BOUNDARIES this code defines, and construct each check's inputs to PROBE at and just past them — but assert only what the contract guarantees there. Probing a boundary is free; asserting an unspecified boundary value is the classic unsound oracle.
  - check: a self-contained Java snippet that builds the input from an `int`/`long`/`String` chosen with the FuzzedDataProvider `data`, computes both sides (or the property) via the REAL public API, and `throw new RuntimeException("relation <name> violated: "+...)` on disagreement.
    TWO-TIER CATCH (mandatory; checked mechanically). The two tiers treat an exception differently, because they mean different things:
      TIER 1 — SETUP (constructing the inputs and the receivers): `try { ... } catch (Exception e) { return; }`. An input or receiver that will not build is an input this relation skips — there a caught exception IS a rejection.
      TIER 2 — THE PROBE (the call(s) on the PATCH-CHANGED class named in the context above): its OWN separate try, NOT nested inside the setup try, whose catch RE-THROWS instead of returning:
        try { r = changed.method(validInput); }
        catch (Exception e) { throw new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); }
      You have already declared this input VALID BY CONSTRUCTION, so an exception raised inside the changed code on it is a RESULT the relation must report, not a rejection to skip. A blanket catch-and-return around the probe makes the check quiet on BOTH builds, so a patch that ADDS or MOVES a throw — refusing an input the contract requires it to accept — is invisible to every relation shaped that way. Setup exceptions stay rejections; probe exceptions are reportable.
      EXCEPTION TO TIER 2: a relation whose CONTRACT IS a rejection (the documented-@throws shapes above) keeps its TARGETED `catch (TheDocumentedException ok) { }` — that catch names the ONE exception the contract promises, which is evidence, not a swallow. Never a blanket catch-and-return around a probe either way. And tier 2 covers only exceptions the documentation does NOT permit: an exception the probed method DECLARES or documents (@throws — e.g. a solver's documented non-convergence exception on a hard input) is a legitimate outcome of a correct implementation — treat it exactly like a setup rejection (catch that named type and return), and reserve the tier-2 rethrow for exceptions no documentation permits.
    STRUCTURE RULE (checked mechanically; violations are rejected): the setup try/catch goes ONLY around the API calls that build/compute the values. The comparison and its `throw new RuntimeException(...) violated ...` must sit AFTER/OUTSIDE that try block — a violation thrown inside a catch-everything block that returns is caught by it and silently discarded, making the whole check dead (this is why the tier-2 try must not be nested inside the tier-1 try: its rethrow would land in the setup catch and die there). Pattern:
      T a, b;
      try { a = api(...); b = api2(...); } catch (Exception e) { return; }
      try { b = changed.method(a); } catch (Exception e) { throw new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); }
      if (!close(a, b)) throw new RuntimeException("relation <name> violated: " + a + " vs " + b);
Draw inputs ONLY with the Jazzer FuzzedDataProvider API on `data`: data.consumeInt(), data.consumeInt(lo,hi), data.consumeLong(), data.consumeBoolean(), data.consumeAsciiString(n), data.consumeRemainingAsString() — there is NO drawInt/nextInt. The check body runs INSIDE fuzzerTestOneInput, so use the real library types directly (no wrapper class, no imports block).
IMPLEMENTABILITY (checked mechanically; violations are dropped): the check may NOT define an anonymous or local SUBCLASS of a library class to reach a code path (e.g. `new AbstractIntegerDistribution(...){...}`) — a hand-built stand-in is forbidden and the relation will be discarded. If the behaviour you want lives on an abstract base, reach it through a REAL concrete library subclass that already exists (e.g. use `new UniformIntegerDistribution(min, max)` to get an integer distribution over a chosen support, rather than subclassing the abstract base yourself). If no real subclass can construct the input you need, choose a different relation.
Return ONLY a JSON array of objects with keys name, kind, contract, input, check. No prose outside the JSON.
```

</details>
<details open><summary>▸ Output (~11,170 chars)</summary>

```
[
  {
    "name": "guesser_rejects_triangular_wave",
    "kind": "invariant",
    "contract": "For the ill-conditioned non-harmonic sample exercised by the trusted failing test, `HarmonicFitter.ParameterGuesser.guess()` must signal failure with `org.apache.commons.math3.exception.MathIllegalStateException` rather than returning coefficients.",
    "input": "Build a triangular periodic sample like the failing test, but vary the x-origin and x-step from fuzz data while keeping strictly increasing abscissae and non-zero range; scale the ordinates by a positive integer factor so the sample stays far from a cosine.",
    "check": "org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;\norg.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser guesser;\ntry {\n  int scale = data.consumeInt(1, 5);\n  int step = data.consumeInt(1, 4);\n  int start = data.consumeInt(-20, 20);\n  int[] base = new int[] { 0, 1, 2, 3, 2, 1,\n                           0, -1, -2, -3, -2, -1,\n                           0, 1, 2, 3, 2, 1,\n                           0, -1, -2, -3, -2, -1,\n                           0, 1, 2, 3, 2, 1, 0 };\n  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[base.length];\n  for (int i = 0; i < base.length; i++) {\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);\n  }\n  guesser = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points);\n} catch (Exception e) {\n  return;\n}\nboolean violated = false;\nString detail = \"completed normally\";\ntry {\n  guesser.guess();\n  violated = true;\n} catch (org.apache.commons.math3.exception.MathIllegalStateException ok) {\n} catch (Throwable t) {\n  violated = true;\n  detail = \"wrong exception class \" + t.getClass().getName();\n}\nif (violated) {\n  throw new RuntimeException(\"relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but \" + detail);\n}"
  },
  {
    "name": "weighted_observed_point_getters_roundtrip_constructor_args",
    "kind": "invariant",
    "contract": "`WeightedObservedPoint` is an immutable container whose getters return the weight, abscissa, and ordinate supplied to the constructor.",
    "input": "Choose arbitrary finite constructor arguments from fuzz data and instantiate one `WeightedObservedPoint`.",
    "check": "double w;\ndouble x;\ndouble y;\norg.apache.commons.math3.optimization.fitting.WeightedObservedPoint p;\ntry {\n  w = data.consumeInt(-1000, 1000) / 10.0;\n  x = data.consumeInt(-1000, 1000) / 10.0;\n  y = data.consumeInt(-1000, 1000) / 10.0;\n  p = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(w, x, y);\n} catch (Exception e) {\n  return;\n}\nif (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||\n    Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||\n    Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {\n  throw new RuntimeException(\"relation weighted_observed_point_getters_roundtrip_constructor_args violated: getters did not return constructor arguments\");\n}"
  },
  {
    "name": "fit_overloads_agree_with_auto_guess",
    "kind": "metamorphic",
    "contract": "`fit()` is documented as the same harmonic fit as `fit(double[])`, differing only in that `fit()` automatically computes the initial guess.",
    "input": "Construct an exact non-degenerate cosine sample with positive amplitude, positive angular frequency, strictly increasing x-values, and enough points for guessing; compute the automatic initial guess with `ParameterGuesser` and compare the two overloads on equivalent inputs.",
    "check": "org.apache.commons.math3.optimization.fitting.HarmonicFitter fitterAuto;\norg.apache.commons.math3.optimization.fitting.HarmonicFitter fitterManual;\norg.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;\ndouble[] autoGuess;\ntry {\n  double amplitude = data.consumeInt(1, 8);\n  double omega = data.consumeInt(1, 6) / 5.0;\n  double phase = data.consumeInt(-30, 30) / 10.0;\n  int n = data.consumeInt(8, 20);\n  double step = data.consumeInt(1, 5) / 10.0;\n  double start = data.consumeInt(-20, 20) / 10.0;\n  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[n];\n  fitterAuto = new org.apache.commons.math3.optimization.fitting.HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());\n  fitterManual = new org.apache.commons.math3.optimization.fitting.HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());\n  for (int i = 0; i < n; i++) {\n    double x = start + i * step;\n    double y = amplitude * Math.cos(omega * x + phase);\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, x, y);\n    fitterAuto.addObservedPoint(1.0, x, y);\n    fitterManual.addObservedPoint(1.0, x, y);\n  }\n  autoGuess = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points).guess();\n} catch (Exception e) {\n  return;\n}\ndouble[] r1;\ndouble[] r2;\ntry {\n  r1 = fitterAuto.fit();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation fit_overloads_agree_with_auto_guess violated: unexpected \" + e.getClass().getName() + \" from fit() on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  r2 = fitterManual.fit(autoGuess);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation fit_overloads_agree_with_auto_guess violated: unexpected \" + e.getClass().getName() + \" from fit(double[]) on valid-by-construction input: \" + e.getMessage());\n}\nif (r1 == null || r2 == null || r1.length != 3 || r2.length != 3) {\n  throw new RuntimeException(\"relation fit_overloads_agree_with_auto_guess violated: both overloads must return 3 parameters\");\n}\nfor (int i = 0; i < 3; i++) {\n  double a = r1[i];\n  double b = r2[i];\n  if (!(Math.abs(a - b) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) {\n    throw new RuntimeException(\"relation fit_overloads_agree_with_auto_guess violated at index \" + i + \": \" + a + \" vs \" + b);\n  }\n}"
  },
  {
    "name": "fit_exact_harmonic_reproduces_samples",
    "kind": "invariant",
    "contract": "`fit` returns the parameters of the harmonic function that best fits the observed points; for observations taken exactly from a harmonic function, a correct fit must reproduce those samples.",
    "input": "Construct exact samples from `y = a cos(omega x + phase)` with positive amplitude, positive omega, non-zero x-range, and enough points; then fit them.",
    "check": "org.apache.commons.math3.optimization.fitting.HarmonicFitter fitter;\ndouble[] xs;\ndouble[] ys;\ntry {\n  double amplitude = data.consumeInt(1, 8);\n  double omega = data.consumeInt(1, 8) / 4.0;\n  double phase = data.consumeInt(-30, 30) / 10.0;\n  int n = data.consumeInt(8, 20);\n  double step = data.consumeInt(1, 5) / 10.0;\n  double start = data.consumeInt(-20, 20) / 10.0;\n  xs = new double[n];\n  ys = new double[n];\n  fitter = new org.apache.commons.math3.optimization.fitting.HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());\n  for (int i = 0; i < n; i++) {\n    xs[i] = start + i * step;\n    ys[i] = amplitude * Math.cos(omega * xs[i] + phase);\n    fitter.addObservedPoint(1.0, xs[i], ys[i]);\n  }\n} catch (Exception e) {\n  return;\n}\ndouble[] params;\ntry {\n  params = fitter.fit();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation fit_exact_harmonic_reproduces_samples violated: unexpected \" + e.getClass().getName() + \" from fit() on valid-by-construction input: \" + e.getMessage());\n}\nif (params == null || params.length != 3) {\n  throw new RuntimeException(\"relation fit_exact_harmonic_reproduces_samples violated: fit() must return 3 parameters\");\n}\norg.apache.commons.math3.analysis.function.HarmonicOscillator osc = new org.apache.commons.math3.analysis.function.HarmonicOscillator(params[0], params[1], params[2]);\nfor (int i = 0; i < xs.length; i++) {\n  double predicted = osc.value(xs[i]);\n  double expected = ys[i];\n  if (!(Math.abs(predicted - expected) <= 1e-6 * Math.max(1.0, Math.max(Math.abs(predicted), Math.abs(expected))))) {\n    throw new RuntimeException(\"relation fit_exact_harmonic_reproduces_samples violated at sample \" + i + \": predicted=\" + predicted + \", expected=\" + expected);\n  }\n}"
  },
  {
    "name": "guesser_returns_three_real_positive_shape_params_on_exact_cosine",
    "kind": "invariant",
    "contract": "`ParameterGuesser` guesses the harmonic coefficients amplitude `a`, pulsation `omega`, and phase `phi`; for a valid non-degenerate harmonic sample it must return those three coefficients as real values, with non-negative amplitude and positive pulsation.",
    "input": "Build an exact cosine sample with positive amplitude, positive omega, strictly increasing x-values, and enough points for guessing.",
    "check": "org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser guesser;\ntry {\n  double amplitude = data.consumeInt(1, 8);\n  double omega = data.consumeInt(1, 8) / 4.0;\n  double phase = data.consumeInt(-30, 30) / 10.0;\n  int n = data.consumeInt(8, 20);\n  double step = data.consumeInt(1, 5) / 10.0;\n  double start = data.consumeInt(-20, 20) / 10.0;\n  org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[n];\n  for (int i = 0; i < n; i++) {\n    double x = start + i * step;\n    double y = amplitude * Math.cos(omega * x + phase);\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, x, y);\n  }\n  guesser = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points);\n} catch (Exception e) {\n  return;\n}\ndouble[] guess;\ntry {\n  guess = guesser.guess();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation guesser_returns_three_real_positive_shape_params_on_exact_cosine violated: unexpected \" + e.getClass().getName() + \" from guess() on valid-by-construction input: \" + e.getMessage());\n}\nif (guess == null || guess.length != 3) {\n  throw new RuntimeException(\"relation guesser_returns_three_real_positive_shape_params_on_exact_cosine violated: guess() must return 3 parameters\");\n}\nfor (int i = 0; i < 3; i++) {\n  if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {\n    throw new RuntimeException(\"relation guesser_returns_three_real_positive_shape_params_on_exact_cosine violated: non-finite parameter at index \" + i);\n  }\n}\nif (guess[0] < 0.0) {\n  throw new RuntimeException(\"relation guesser_returns_three_real_positive_shape_params_on_exact_cosine violated: amplitude must be non-negative, got \" + guess[0]);\n}\nif (!(guess[1] > 0.0)) {\n  throw new RuntimeException(\"relation guesser_returns_three_real_positive_shape_params_on_exact_cosine violated: omega must be positive, got \" + guess[1]);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `guesser_rejects_triangular_wave`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~751 chars)</summary>

- reason: the probe `HarmonicFitter.ParameterGuesser(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 3, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [6] ⚙️ screen · `guesser_rejects_triangular_wave`
**output:** **demoted**
<details><summary>▸ reason (~729 chars)</summary>

- reason: constant receiver state: receiver-state probe `guesser.guess(...)` runs against a container whose STRUCTURE is compile-time constant: `guesser = new ParameterGuesser(...)` — every element count, index and position is a literal. The check draws data.consumeInt(), none of which reaches an index, a count, a loop bound, or a branch that governs construction, so every fuzz iteration rebuilds the SAME shape. A patch that only misbehaves for a different shape (a gap/hole, an empty or larger container, an element installed at a different index) is never exercised. Draw the structure from `data`: the number of elements, the indices they are installed at, whether the container is empty, and whether it is freshly built or mutated.

</details>

---
## [7] ⚙️ screen-fuzz-buggy · `guesser_rejects_triangular_wave`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [8] ⚙️ screen-fuzz-buggy · `weighted_observed_point_getters_roundtrip_constructor_args`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen · `fit_overloads_agree_with_auto_guess`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~751 chars)</summary>

- reason: the probe `HarmonicFitter.ParameterGuesser(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 5, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [10] ⚙️ screen-fuzz-buggy · `fit_overloads_agree_with_auto_guess`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [11] ⚙️ screen · `fit_exact_harmonic_reproduces_samples`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~743 chars)</summary>

- reason: the probe `fitter.addObservedPoint(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 4, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [12] ⚙️ screen-fuzz-buggy · `fit_exact_harmonic_reproduces_samples`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [13] ⚙️ screen · `guesser_returns_three_real_positive_shape_params_on_exact_co`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~751 chars)</summary>

- reason: the probe `HarmonicFitter.ParameterGuesser(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 2, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [14] ⚙️ screen-fuzz-buggy · `guesser_returns_three_real_positive_shape_params_on_exact_co`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [15] ⚙️ screen · `guesser_rejects_triangular_wave`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [16] ⚙️ screen · `weighted_observed_point_getters_roundtrip_constructor_args`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [17] ⚙️ screen · `fit_overloads_agree_with_auto_guess`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [18] ⚙️ screen · `fit_exact_harmonic_reproduces_samples`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [19] ⚙️ screen · `guesser_returns_three_real_positive_shape_params_on_exact_co`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [20] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "guesser_rejects_triangular_wave",
    "weighted_observed_point_getters_roundtrip_constructor_args",
    "fit_overloads_agree_with_auto_guess",
    "fit_exact_harmonic_reproduces_samples",
    "guesser_returns_three_real_positive_shape_params_on_exact_co"
  ],
  "count": 5
}
```

---
## [21] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,126 chars)</summary>

```
[
  {
    "name": "triangular-sample-must-raise-illegal-state",
    "kind": "invariant",
    "contract": "Ground-truth failing test for HarmonicFitter.ParameterGuesser.guess(): on the non-harmonic triangular periodic sample used in testMath844, a correct implementation must throw org.apache.commons.math3.exception.MathIllegalStateException rather than returning a guess.",
    "input": "Build the exact triangular-wave ordinates from the failing test, with unit weights and strictly increasing abscissae x = offset + i where offset is drawn from data. This preserves the same non-harmonic shape while probing the changed c2==0.0 boundary through the public guess() method.",
    "check": "org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;\norg.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser guesser;\ntry {\n  double[] y = new double[] { 0, 1, 2, 3, 2, 1,\n                              0, -1, -2, -3, -2, -1,\n                              0, 1, 2, 3, 2, 1,\n                              0, -1, -2, -3, -2, -1,\n                              0, 1, 2, 3, 2, 1, 0 };\n  int offset = data.consumeInt(-20, 20);\n  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[y.length];\n  for (int i = 0; i < y.length; i++) {\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, offset + i, y[i]);\n  }\n  guesser = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points);\n} catch (Exception e) {\n  return;\n}\nboolean violated = false;\nString what = \"completed normally\";\ntry {\n  guesser.guess();\n  violated = true;\n} catch (org.apache.commons.math3.exception.MathIllegalStateException ok) {\n} catch (Throwable t) {\n  violated = true;\n  what = \"threw wrong exception \" + t.getClass().getName();\n}\nif (violated) {\n  throw new RuntimeException(\"relation triangular-sample-must-raise-illegal-state violated: guess() \" + what);\n}"
  },
  {
    "name": "weighted-observed-point-getters-roundtrip",
    "kind": "invariant",
    "contract": "WeightedObservedPoint is a simple immutable container whose getters return the constructor-supplied weight, x, and y.",
    "input": "Draw weight, x, and y from data as finite doubles synthesized from ints, then construct a WeightedObservedPoint directly.",
    "check": "org.apache.commons.math3.optimization.fitting.WeightedObservedPoint p;\ndouble w = data.consumeInt(-1000, 1000) / 10.0;\ndouble x = data.consumeInt(-1000, 1000) / 10.0;\ndouble y = data.consumeInt(-1000, 1000) / 10.0;\ntry {\n  p = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(w, x, y);\n} catch (Exception e) {\n  return;\n}\nif (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||\n    Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||\n    Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {\n  throw new RuntimeException(\"relation weighted-observed-point-getters-roundtrip violated: expected (\" + w + \",\" + x + \",\" + y + \") but got (\" + p.getWeight() + \",\" + p.getX() + \",\" + p.getY() + \")\");\n}"
  },
  {
    "name": "weighted-observed-point-immutable-across-guesser-use",
    "kind": "invariant",
    "contract": "WeightedObservedPoint instances are guaranteed to be immutable; using them to build a HarmonicFitter.ParameterGuesser and calling guess() must not change any point's getter-visible state.",
    "input": "Construct a valid-by-construction exact harmonic sample from data with strictly increasing x values. Snapshot one chosen point's getters before creating the guesser and again after guess() completes.",
    "check": "org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;\norg.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser guesser;\nint idx;\ndouble bw, bx, by;\ntry {\n  int n = data.consumeInt(8, 20);\n  double a = 0.5 + data.consumeInt(0, 50) / 10.0;\n  double omega = 0.2 + data.consumeInt(0, 30) / 10.0;\n  double phase = data.consumeInt(-314, 314) / 100.0;\n  double step = 0.2 + data.consumeInt(0, 20) / 10.0;\n  double start = data.consumeInt(-50, 50) / 10.0;\n  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[n];\n  for (int i = 0; i < n; i++) {\n    double xv = start + i * step;\n    double yv = a * Math.cos(omega * xv + phase);\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, xv, yv);\n  }\n  idx = data.consumeInt(0, n - 1);\n  bw = points[idx].getWeight();\n  bx = points[idx].getX();\n  by = points[idx].getY();\n  guesser = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points);\n} catch (Exception e) {\n  return;\n}\ntry {\n  guesser.guess();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation weighted-observed-point-immutable-across-guesser-use violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (Double.doubleToLongBits(points[idx].getWeight()) != Double.doubleToLongBits(bw) ||\n    Double.doubleToLongBits(points[idx].getX()) != Double.doubleToLongBits(bx) ||\n    Double.doubleToLongBits(points[idx].getY()) != Double.doubleToLongBits(by)) {\n  throw new RuntimeException(\"relation weighted-observed-point-immutable-across-guesser-use violated: point changed from (\" + bw + \",\" + bx + \",\" + by + \") to (\" + points[idx].getWeight() + \",\" + points[idx].getX() + \",\" + points[idx].getY() + \")\");\n}"
  },
  {
    "name": "guesser-returns-three-parameters-on-valid-harmonic-sample",
    "kind": "invariant",
    "contract": "ParameterGuesser guesses the harmonic coefficients amplitude, angular frequency, and phase; therefore on a valid harmonic sample it must produce exactly three parameters.",
    "input": "Build an exact, non-degenerate harmonic sample from data: amplitude > 0, omega > 0, at least 8 strictly increasing x values, all unit weights.",
    "check": "org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;\norg.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser guesser;\ntry {\n  int n = data.consumeInt(8, 24);\n  double a = 0.5 + data.consumeInt(0, 80) / 10.0;\n  double omega = 0.2 + data.consumeInt(0, 40) / 10.0;\n  double phase = data.consumeInt(-628, 628) / 100.0;\n  double step = 0.1 + data.consumeInt(1, 20) / 10.0;\n  double start = data.consumeInt(-100, 100) / 10.0;\n  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[n];\n  for (int i = 0; i < n; i++) {\n    double xv = start + i * step;\n    double yv = a * Math.cos(omega * xv + phase);\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, xv, yv);\n  }\n  guesser = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points);\n} catch (Exception e) {\n  return;\n}\ndouble[] guess;\ntry {\n  guess = guesser.guess();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation guesser-returns-three-parameters-on-valid-harmonic-sample violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (guess == null || guess.length != 3) {\n  throw new RuntimeException(\"relation guesser-returns-three-parameters-on-valid-harmonic-sample violated: expected length 3 but got \" + (guess == null ? \"null\" : String.valueOf(guess.length)));\n}"
  },
  {
    "name": "fit-overloads-agree-when-using-auto-guess",
    "kind": "metamorphic",
    "contract": "The two HarmonicFitter.fit overloads have the same documented result ('the parameters of the harmonic function that best fits the observed points'); fit() differs only by automatically computing the initial guess, so it must agree with fit(double[]) when passed that same computed guess.",
    "input": "Using reflection to obtain a real optimizer instance, build a HarmonicFitter populated with an exact harmonic sample from data. Independently compute ParameterGuesser.guess() on the same points, then compare fit() with fit(theSameGuess).",
    "check": "org.apache.commons.math3.optimization.fitting.HarmonicFitter fitter;\norg.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;\ndouble[] initialGuess;\ntry {\n  Class<?> c = Class.forName(\"org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer\");\n  Object optimizer = c.getConstructor().newInstance();\n  fitter = new org.apache.commons.math3.optimization.fitting.HarmonicFitter((org.apache.commons.math3.optimization.DifferentiableMultivariateVectorOptimizer) optimizer);\n\n  int n = data.consumeInt(8, 20);\n  double a = 0.5 + data.consumeInt(0, 50) / 10.0;\n  double omega = 0.2 + data.consumeInt(0, 20) / 10.0;\n  double phase = data.consumeInt(-314, 314) / 100.0;\n  double step = 0.2 + data.consumeInt(0, 10) / 10.0;\n  double start = data.consumeInt(-50, 50) / 10.0;\n  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[n];\n  java.lang.reflect.Method add3 = null;\n  java.lang.reflect.Method add1 = null;\n  for (java.lang.reflect.Method m : fitter.getClass().getMethods()) {\n    if (m.getName().equals(\"addObservedPoint\")) {\n      Class<?>[] pt = m.getParameterTypes();\n      if (pt.length == 3) {\n        add3 = m;\n      } else if (pt.length == 1) {\n        add1 = m;\n      }\n    }\n  }\n  if (add3 == null && add1 == null) {\n    return;\n  }\n  for (int i = 0; i < n; i++) {\n    double xv = start + i * step;\n    double yv = a * Math.cos(omega * xv + phase);\n    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, xv, yv);\n    if (add3 != null) {\n      add3.invoke(fitter, 1.0, xv, yv);\n    } else {\n      add1.invoke(fitter, points[i]);\n    }\n  }\n  initialGuess = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points).guess();\n} catch (Exception e) {\n  return;\n}\ndouble[] r1;\ntry {\n  r1 = fitter.fit();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation fit-overloads-agree-when-using-auto-guess violated: unexpected \" + e.getClass().getName() + \" from fit() on valid-by-construction input: \" + e.getMessage());\n}\ndouble[] r2;\ntry {\n  r2 = fitter.fit(initialGuess);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation fit-overloads-agree-when-using-auto-guess violated: unexpected \" + e.getClass().getName() + \" from fit(double[]) on valid-by-construction input: \" + e.getMessage());\n}\nif (r1 == null || r2 == null || r1.length != 3 || r2.length != 3) {\n  throw new RuntimeException(\"relation fit-overloads-agree-when-using-auto-guess violated: bad result lengths\");\n}\nfor (int i = 0; i < 3; i++) {\n  double diff = Math.abs(r1[i] - r2[i]);\n  double scale = Math.max(1.0, Math.max(Math.abs(r1[i]), Math.abs(r2[i])));\n  if (diff > 1e-6 * scale) {\n    throw new RuntimeException(\"relation fit-overloads-agree-when-using-auto-guess violated at index \" + i + \": \" + r1[i] + \" vs \" + r2[i]);\n  }\n}"
  }
]
```

</details>

---
## [22] ⚙️ screen · `triangular-sample-must-raise-illegal-state`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~751 chars)</summary>

- reason: the probe `HarmonicFitter.ParameterGuesser(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 3, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [23] ⚙️ screen · `triangular-sample-must-raise-illegal-state`
**output:** **demoted**
<details><summary>▸ reason (~729 chars)</summary>

- reason: constant receiver state: receiver-state probe `guesser.guess(...)` runs against a container whose STRUCTURE is compile-time constant: `guesser = new ParameterGuesser(...)` — every element count, index and position is a literal. The check draws data.consumeInt(), none of which reaches an index, a count, a loop bound, or a branch that governs construction, so every fuzz iteration rebuilds the SAME shape. A patch that only misbehaves for a different shape (a gap/hole, an empty or larger container, an element installed at a different index) is never exercised. Draw the structure from `data`: the number of elements, the indices they are installed at, whether the container is empty, and whether it is freshly built or mutated.

</details>

---
## [24] ⚙️ screen-fuzz-buggy · `triangular-sample-must-raise-illegal-state`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [25] ⚙️ screen-fuzz-buggy · `weighted-observed-point-getters-roundtrip`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [26] ⚙️ screen · `weighted-observed-point-immutable-across-guesser-use`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~751 chars)</summary>

- reason: the probe `HarmonicFitter.ParameterGuesser(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 5, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [27] ⚙️ screen-fuzz-buggy · `weighted-observed-point-immutable-across-guesser-use`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [28] ⚙️ screen · `guesser-returns-three-parameters-on-valid-harmonic-sample`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~751 chars)</summary>

- reason: the probe `HarmonicFitter.ParameterGuesser(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 3, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [29] ⚙️ screen-fuzz-buggy · `guesser-returns-three-parameters-on-valid-harmonic-sample`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [30] ⚙️ screen · `fit-overloads-agree-when-using-auto-guess`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~735 chars)</summary>

- reason: the probe `fitter.getClass(...)` on the patch-changed class `HarmonicFitter` runs inside the try at line 4, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `HarmonicFitter` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [31] ⚙️ screen-fuzz-buggy · `fit-overloads-agree-when-using-auto-guess`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [32] ⚙️ screen · `triangular-sample-must-raise-illegal-state`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [33] ⚙️ screen · `weighted-observed-point-getters-roundtrip`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [34] ⚙️ screen · `weighted-observed-point-immutable-across-guesser-use`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [35] ⚙️ screen · `guesser-returns-three-parameters-on-valid-harmonic-sample`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [36] ⚙️ screen · `fit-overloads-agree-when-using-auto-guess`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [37] ⚙️ reference-widening · `enumerate`
**output:** **1 observable(s) to widen onto**
<details><summary>▸ reason (~417 chars)</summary>

- reason: the kept checks probe ['fit'] among the patched class's no-argument readers; 1 of those have no admitted reference and 1 fit under the per-leg cap of 3 (the leg already holds []); 11 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['message', 'harmonicfitter', 'weightedobservedpoint', 'mathillegalstateexception', 'zeroexception', 'numberistoosmallexception'])

</details>
<details><summary>▸ detail (~2,377 chars)</summary>

- detail: {'targets': ['fit'], 'already_admitted': [], 'cap': 3, 'checks_read': 10, 'excluded': [{'key': 'message', 'why': 'the patched class does not declare `getMessage()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'harmonicfitter', 'why': "the patched class declares `HarmonicFitter` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'weightedobservedpoint', 'why': 'the patched class does not declare `WeightedObservedPoint()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'mathillegalstateexception', 'why': 'the patched class does not declare `MathIllegalStateException()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'zeroexception', 'why': 'the patched class does not declare `ZeroException()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'numberistoosmallexception', 'why': 'the patched class does not declare `NumberIsTooSmallException()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'weight', 'why': 'the patched class does not declare `getWeight()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'getx', 'why': 'the patched class does not declare `getX()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'gety', 'why': 'the patched class does not declare `getY()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'value', 'why': 'the patched class does not declare `value()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'harmonicoscillator', 'why': 'the patched class does not declare `HarmonicOscillator()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 11}

</details>

---
## [38] ⚙️ reference-widening · `fit`
**output:** **reference REQUESTED for observable `fit`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [39] ⚙️ reference-impl · `fit`
**output:** **widening target requested**
- detail: {'candidates': ['fit'], 'attempting': 'fit'}

---
## [40] ⚙️ reference-impl · `fit`
**output:** **screening surface resolved**
- reason: 0 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': [], 'patch_touched_exempted': [], 'screened_siblings': [], 'declaring_types': ['HarmonicFitter']}

---
## [41] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~25,774 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
**[user]**
```
Implement `fit` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="HarmonicFitter" role="patched">
     * Class that implements a curve fitting specialized for sinusoids.
     * Harmonic fitting is a very simple case of curve fitting. The
     * estimated coefficients are the amplitude a, the pulsation &omega; and
     * the phase &phi;: <code>f (t) = a cos (&omega; t + &phi;)</code>. They are
     * searched by a least square estimator initialized with a rough guess
     * based on integrals.
     * @version $Id$
     * @since 2.0
public class HarmonicFitter extends CurveFitter<HarmonicOscillator.Parametric> {
     * Simple constructor.
     * @param optimizer Optimizer to use for the fitting.
    public HarmonicFitter(final DifferentiableMultivariateVectorOptimizer optimizer) { /* body withheld */ }
     * Fit an harmonic function to the observed points.
     * @param initialGuess First guess values in the following order:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
     * @return the parameters of the harmonic function that best fits the
     * observed points (in the same order as above).
    public double[] fit(double[] initialGuess) { /* body withheld */ }
     * Fit an harmonic function to the observed points.
     * An initial guess will be automatically computed.
     * @return the parameters of the harmonic function that best fits the
     * observed points (see the other { /* body withheld */ } method.
     * @throws NumberIsTooSmallException if the sample is too short for the
     * the first guess to be computed.
     * @throws ZeroException if the first guess cannot be computed because
     * the abscissa range is zero.
    public double[] fit() { /* body withheld */ }
     * This class guesses harmonic coefficients from a sample.
     * <p>The algorithm used to guess the coefficients is as follows:</p>
     * <p>We know f (t) at some sampling points t<sub>i</sub> and want to find a,
     * &omega; and &phi; such that f (t) = a cos (&omega; t + &phi;).
     * </p>
     * <p>From the analytical expression, we can compute two primitives :
     * <pre>
     * If2  (t) = &int; f<sup>2</sup>  = a<sup>2</sup> &times; [t + S (t)] / 2
     * If'2 (t) = &int; f'<sup>2</sup> = a<sup>2</sup> &omega;<sup>2</sup> &times; [t - S (t)] / 2
     * where S (t) = sin (2 (&omega; t + &phi;)) / (2 &omega;)
     * </pre>
     * </p>
     * …
    public static class ParameterGuesser { … }
}
</class>

<class name="CurveFitter" role="supertype-contract">
class TheoreticalValuesFunction
        implements DifferentiableMultivariateVectorFunction {
     * Function to fit.
    private final ParametricUnivariateFunction f;
     * Simple constructor.
     * @param f function to fit.
    public TheoreticalValuesFunction(final ParametricUnivariateFunction f) { /* body withheld */ }
     * { /* body withheld */ }
    public MultivariateMatrixFunction jacobian() { /* body withheld */ }
     * { /* body withheld */ }
    public double[] value(double[] point) { /* body withheld */ }
}
</class>

<class name="WeightedObservedPoint" role="test-subject">
     * This class is a simple container for weighted observed point in
     * {@link CurveFitter curve fitting}.
     * <p>Instances of this class are guaranteed to be immutable.</p>
     * @version $Id$
     * @since 2.0
public class WeightedObservedPoint implements Serializable {
     * Serializable version id.
    private static final long serialVersionUID = 5306874947404636157L;
     * Weight of the measurement in the fitting process.
    private final double weight;
     * Abscissa of the point.
    private final double x;
     * Observed value of the function at x.
    private final double y;
     * Simple constructor.
     * @param weight weight of the measurement in the fitting process
     * @param x abscissa of the measurement
     * @param y ordinate of the measurement
    public WeightedObservedPoint(final double weight, final double x, final double y) { /* body withheld */ }
     * Get the weight of the measurement in the fitting process.
     * @return weight of the measurement in the fitting process
    public double getWeight() { /* body withheld */ }
     * Get the abscissa of the point.
     * @return abscissa of the point
    public double getX() { /* body withheld */ }
     * Get the observed value of the function at x.
     * @return observed value of the function at x
    public double getY() { /* body withheld */ }
}
</class>

<class name="MathIllegalStateException" role="test-subject">
     * Base class for all exceptions that signal a mismatch between the
     * current state and the user's expectations.
     * @since 2.2
     * @version $Id$
public class MathIllegalStateException extends IllegalStateException
    implements ExceptionContextProvider {
     * Serializable version Id.
    private static final long serialVersionUID = -6024911025449780478L;
     * Context.
    private final ExceptionContext context;
     * Simple constructor.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Localizable pattern, Object ... args) { /* body withheld */ }
     * Simple constructor.
     * @param cause Root cause.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Throwable cause, Localizable pattern, Object ... args) { /* body withheld */ }
     * Default constructor.
    public MathIllegalStateException() { /* body withheld */ }
     * { /* body withheld */ }
    public ExceptionContext getContext() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    @Test(expected=MathIllegalStateException.class)
    public void testMath844() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser guesser
            = new HarmonicFitter.ParameterGuesser(points);

        // The guesser fails because the function is far from an harmonic
        // function: It is a triangular periodic function with amplitude 3
        // and period 12, and all sample points are taken at integer abscissae
        // so function values all belong to the integer subset {-3, -2, -1, 0,
        // 1, 2, 3}.
        guesser.guess();
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="HarmonicFitter" role="patched">
     * Class that implements a curve fitting specialized for sinusoids.
     * Harmonic fitting is a very simple case of curve fitting. The
     * estimated coefficients are the amplitude a, the pulsation &omega; and
     * the phase &phi;: <code>f (t) = a cos (&omega; t + &phi;)</code>. They are
     * searched by a least square estimator initialized with a rough guess
     * based on integrals.
     * @version $Id$
     * @since 2.0
public class HarmonicFitter extends CurveFitter<HarmonicOscillator.Parametric> {
     * Simple constructor.
     * @param optimizer Optimizer to use for the fitting.
    public HarmonicFitter(final DifferentiableMultivariateVectorOptimizer optimizer) { /* body withheld */ }
     * Fit an harmonic function to the observed points.
     * @param initialGuess First guess values in the following order:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
     * @return the parameters of the harmonic function that best fits the
     * observed points (in the same order as above).
    public double[] fit(double[] initialGuess) { /* body withheld */ }
     * Fit an harmonic function to the observed points.
     * An initial guess will be automatically computed.
     * @return the parameters of the harmonic function that best fits the
     * observed points (see the other { /* body withheld */ } method.
     * @throws NumberIsTooSmallException if the sample is too short for the
     * the first guess to be computed.
     * @throws ZeroException if the first guess cannot be computed because
     * the abscissa range is zero.
    public double[] fit() { /* body withheld */ }
     * This class guesses harmonic coefficients from a sample.
     * <p>The algorithm used to guess the coefficients is as follows:</p>
     * <p>We know f (t) at some sampling points t<sub>i</sub> and want to find a,
     * &omega; and &phi; such that f (t) = a cos (&omega; t + &phi;).
     * </p>
     * <p>From the analytical expression, we can compute two primitives :
     * <pre>
     * If2  (t) = &int; f<sup>2</sup>  = a<sup>2</sup> &times; [t + S (t)] / 2
     * If'2 (t) = &int; f'<sup>2</sup> = a<sup>2</sup> &omega;<sup>2</sup> &times; [t - S (t)] / 2
     * where S (t) = sin (2 (&omega; t + &phi;)) / (2 &omega;)
     * </pre>
     * </p>
     * …
    public static class ParameterGuesser { … }
}
</class>

<class name="CurveFitter" role="supertype-contract">
class TheoreticalValuesFunction
        implements DifferentiableMultivariateVectorFunction {
     * Function to fit.
    private final ParametricUnivariateFunction f;
     * Simple constructor.
     * @param f function to fit.
    public TheoreticalValuesFunction(final ParametricUnivariateFunction f) { /* body withheld */ }
     * { /* body withheld */ }
    public MultivariateMatrixFunction jacobian() { /* body withheld */ }
     * { /* body withheld */ }
    public double[] value(double[] point) { /* body withheld */ }
}
</class>

<class name="WeightedObservedPoint" role="test-subject">
     * This class is a simple container for weighted observed point in
     * {@link CurveFitter curve fitting}.
     * <p>Instances of this class are guaranteed to be immutable.</p>
     * @version $Id$
     * @since 2.0
public class WeightedObservedPoint implements Serializable {
     * Serializable version id.
    private static final long serialVersionUID = 5306874947404636157L;
     * Weight of the measurement in the fitting process.
    private final double weight;
     * Abscissa of the point.
    private final double x;
     * Observed value of the function at x.
    private final double y;
     * Simple constructor.
     * @param weight weight of the measurement in the fitting process
     * @param x abscissa of the measurement
     * @param y ordinate of the measurement
    public WeightedObservedPoint(final double weight, final double x, final double y) { /* body withheld */ }
     * Get the weight of the measurement in the fitting process.
     * @return weight of the measurement in the fitting process
    public double getWeight() { /* body withheld */ }
     * Get the abscissa of the point.
     * @return abscissa of the point
    public double getX() { /* body withheld */ }
     * Get the observed value of the function at x.
     * @return observed value of the function at x
    public double getY() { /* body withheld */ }
}
</class>

<class name="MathIllegalStateException" role="test-subject">
     * Base class for all exceptions that signal a mismatch between the
     * current state and the user's expectations.
     * @since 2.2
     * @version $Id$
public class MathIllegalStateException extends IllegalStateException
    implements ExceptionContextProvider {
     * Serializable version Id.
    private static final long serialVersionUID = -6024911025449780478L;
     * Context.
    private final ExceptionContext context;
     * Simple constructor.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Localizable pattern, Object ... args) { /* body withheld */ }
     * Simple constructor.
     * @param cause Root cause.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Throwable cause, Localizable pattern, Object ... args) { /* body withheld */ }
     * Default constructor.
    public MathIllegalStateException() { /* body withheld */ }
     * { /* body withheld */ }
    public ExceptionContext getContext() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
}
</class>

<class name="FastMath" role="collaborator">
     * Faster, more accurate, portable alternative to {@link Math} and
     * { /* body withheld */ } for large scale computation.
     * <p>
     * FastMath is a drop-in replacement for both Math and StrictMath. This
     * means that for any method in Math (say { /* body withheld */ } or
     * { /* body withheld */ }), user can directly change the class and use the
     * methods as is (using {@code FastMath.sin(x)} or { /* body withheld */ }
     * in the previous example).
     * </p>
     * <p>
     * FastMath speed is achieved by relying heavily on optimizing compilers
     * to native code present in many JVMs today and use of large tables.
     * …
public class FastMath {
     * Archimede's constant PI, ratio of circle circumference to diameter.
    public static final double PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9;
     * Napier's constant e, base of the natural logarithm.
    public static final double E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8;
     * Index of exp(0) in the array of integer exponentials. */
     * static final int EXP_INT_TABLE_MAX_INDEX = 750;
     * /** Length of the array of integer exponentials. */
     * static final int EXP_INT_TABLE_LEN = EXP_INT_TABLE_MAX_INDEX * 2;
     * /** Logarithm table length. */
     * static final int LN_MANT_LEN = 1024;
     * /** Exponential fractions table length. */
     * static final int EXP_FRAC_TABLE_LEN = 1025; // 0, 1/1024, ... 1024/1024
     * /** Indicator for tables initialization.
     * <p>
     * This compile-time constant should be set to true only if one explicitly
     * wants to compute the tables at class loading time instead of using the
     * …
    private static final boolean RECOMPUTE_TABLES_AT_RUNTIME = false;
     * log(2) (high bits).
    private static final double LN_2_A = 0.693147063255310059;
     * log(2) (low bits).
    private static final double LN_2_B = 1.17304635250823482e-7;
     * Coefficients for log, when input 0.99 < x < 1.01.
    private static final double LN_QUICK_COEF[][] = { /* body withheld */ }
     * Coefficients for log in the range of 1.0 < x < 1.0 + 2^-10.
    private static final double LN_HI_PREC_COEF[][] = { /* body withheld */ }
     * Sine, Cosine, Tangent tables are for 0, 1/8, 2/8, ... 13/8 = PI/2 approx.
    private static final int SINE_TABLE_LEN = 14;
     * Sine table (high bits).
    private static final double SINE_TABLE_A[] = { /* body withheld */ }
     * Sine table (low bits).
    private static final double SINE_TABLE_B[] = { /* body withheld */ }
     * Cosine table (high bits).
    private static final double COSINE_TABLE_A[] = { /* body withheld */ }
     * Cosine table (low bits).
    private static final double COSINE_TABLE_B[] = { /* body withheld */ }
     * Tangent table, used by atan() (high bits).
    private static final double TANGENT_TABLE_A[] = { /* body withheld */ }
     * Tangent table, used by atan() (low bits).
    private static final double TANGENT_TABLE_B[] = { /* body withheld */ }
     * Bits of 1/(2*pi), need for reducePayneHanek().
    private static final long RECIP_2PI[] = new long[] { /* body withheld */ }
     * Bits of pi/4, need for reducePayneHanek().
    private static final long PI_O_4_BITS[] = new long[] { /* body withheld */ }
     * Eighths.
     * This is used by sinQ, because its faster to do a table lookup than
     * a multiply in this time-critical routine
    private static final double EIGHTHS[] = { /* body withheld */ }
     * Table of 2^((n+2)/3)
    private static final double CBRTTWO[] = { /* body withheld */ }
     * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
     * Equivalent to 2^30.
    private static final long HEX_40000000 = 0x40000000L;
     * Mask used to clear low order 30 bits
    private static final long MASK_30BITS = -1L - (HEX_40000000 -1);
     * 2^52 - double numbers this large must be integral (no fraction) or NaN or Infinite
    private static final double TWO_POWER_52 = 4503599627370496.0;
     * Constant: { /* body withheld */ }.
    private static final double F_1_3 = 1d / 3d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_5 = 1d / 5d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_7 = 1d / 7d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_9 = 1d / 9d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_11 = 1d / 11d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_13 = 1d / 13d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_15 = 1d / 15d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_17 = 1d / 17d;
     * Constant: { /* body withheld */ }.
    private static final double F_3_4 = 3d / 4d;
     * Constant: { /* body withheld */ }.
    private static final double F_15_16 = 15d / 16d;
     * Constant: { /* body withheld */ }.
    private static final double F_13_14 = 13d / 14d;
     * Constant: { /* body withheld */ }.
    private static final double F_11_12 = 11d / 12d;
     * Constant: { /* body withheld */ }.
    private static final double F_9_10 = 9d / 10d;
     * Constant: { /* body withheld */ }.
    private static final double F_7_8 = 7d / 8d;
     * Constant: { /* body withheld */ }.
    private static final double F_5_6 = 5d / 6d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_2 = 1d / 2d;
     * Constant: { /* body withheld */ }.
    private static final double F_1_4 = 1d / 4d;
     * Private Constructor
    private FastMath() { /* body withheld */ }
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     * @param d the value to split
     * @return the high order part of the mantissa
    private static double doubleHighPart(double d) { /* body withheld */ }
     * Natural logarithm.
     * @param x   a double
     * @return log(x)
    public static double log(final double x) { /* body withheld */ }
     * Internal helper method for natural logarithm function.
     * @param x original argument of the natural logarithm function
     * @param hiPrec extra bits of precision on output (To Be Confirmed)
     * @return log(x)
    private static double log(final double x, final double[] hiPrec) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="ZeroException" role="collaborator">
     * Exception to be thrown when zero is provided where it is not allowed.
     * @since 2.2
     * @version $Id$
public class ZeroException extends MathIllegalNumberException {
     * Serializable version identifier
    private static final long serialVersionUID = -1960874856936000015L;
     * Construct the exception.
    public ZeroException() { /* body withheld */ }
     * Construct the exception with a specific context.
     * @param specific Specific context pattern.
     * @param arguments Arguments.
    public ZeroException(Localizable specific, Object ... arguments) { /* body withheld */ }
}
</class>

<class name="NumberIsTooSmallException" role="collaborator">
     * Exception to be thrown when a number is too small.
     * @since 2.2
     * @version $Id$
public class NumberIsTooSmallException extends MathIllegalNumberException {
     * Serializable version Id.
    private static final long serialVersionUID = -6100997100383932834L;
     * Higher bound.
    private final Number min;
     * Whether the maximum is included in the allowed range.
    private final boolean boundIsAllowed;
     * Construct the exception.
     * @param wrong Value that is smaller than the minimum.
     * @param min Minimum.
     * @param boundIsAllowed Whether { /* body withheld */ } is included in the allowed range.
    public NumberIsTooSmallException(Number wrong, Number min, boolean boundIsAllowed) { /* body withheld */ }
     * Construct the exception with a specific context.
     * @param specific Specific context pattern.
     * @param wrong Value that is smaller than the minimum.
     * @param min Minimum.
     * @param boundIsAllowed Whether { /* body withheld */ } is included in the allowed range.
    public NumberIsTooSmallException(Localizable specific, Number wrong, Number min, boolean boundIsAllowed) { /* body withheld */ }
     * @return { /* body withheld */ } if the minimum is included in the allowed range.
    public boolean getBoundIsAllowed() { /* body withheld */ }
     * @return the minimum.
    public Number getMin() { /* body withheld */ }
}
</class>

<class name="HarmonicOscillator" role="collaborator">
     * <a href="http://en.wikipedia.org/wiki/Harmonic_oscillator">
     * simple harmonic oscillator</a> function.
     * @since 3.0
     * @version $Id$
public class HarmonicOscillator implements DifferentiableUnivariateFunction {
     * Amplitude.
    private final double amplitude;
     * Angular frequency.
    private final double omega;
     * Phase.
    private final double phase;
     * Harmonic oscillator function.
     * @param amplitude Amplitude.
     * @param omega Angular frequency.
     * @param phase Phase.
    public HarmonicOscillator(double amplitude, double omega, double phase) { /* body withheld */ }
     * { /* body withheld */ }
    public double value(double x) { /* body withheld */ }
     * { /* body withheld */ }
    public UnivariateFunction derivative() { /* body withheld */ }
     * Parametric function where the input array contains the parameters of
     * the harmonic oscillator function, ordered as follows:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
    public static class Parametric implements ParametricUnivariateFunction { … }
     * @param xTimesOmegaPlusPhase { /* body withheld */ }.
     * @param amplitude Amplitude.
     * @return the value of the harmonic oscillator function at { /* body withheld */ }.
    private static double value(double xTimesOmegaPlusPhase, double amplitude) { /* body withheld */ }
}
</class>

<class name="DifferentiableMultivariateVectorOptimizer" role="collaborator">
     * This interface represents an optimization algorithm for
     * {@link DifferentiableMultivariateVectorFunction vectorial differentiable
     * objective functions}.
     * @version $Id$
     * @since 3.0
public interface DifferentiableMultivariateVectorOptimizer
    extends BaseMultivariateVectorOptimizer<DifferentiableMultivariateVectorFunction> {

}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,422 chars)</summary>

```
// compute(DifferentiableMultivariateVectorOptimizer optimizer, WeightedObservedPoint[] observations, double[] initialGuess) : fit, fit_auto
public class ReferenceImpl {

    public static double[] compute_fit(
            final DifferentiableMultivariateVectorOptimizer optimizer,
            final WeightedObservedPoint[] observations,
            final double[] initialGuess) {

        final CurveFitter<HarmonicOscillator.Parametric> fitter =
                new CurveFitter<HarmonicOscillator.Parametric>(optimizer);

        if (observations != null) {
            for (WeightedObservedPoint p : observations) {
                fitter.addObservedPoint(p);
            }
        }

        return fitter.fit(new HarmonicOscillator.Parametric(), initialGuess);
    }

    public static double[] compute_fit_auto(
            final DifferentiableMultivariateVectorOptimizer optimizer,
            final WeightedObservedPoint[] observations) {

        final double[] guess = new HarmonicFitter.ParameterGuesser(observations).guess();
        final CurveFitter<HarmonicOscillator.Parametric> fitter =
                new CurveFitter<HarmonicOscillator.Parametric>(optimizer);

        if (observations != null) {
            for (WeightedObservedPoint p : observations) {
                fitter.addObservedPoint(p);
            }
        }

        return fitter.fit(new HarmonicOscillator.Parametric(), guess);
    }
}
```

</details>

---
## [42] ⚙️ reference-impl · `fit`
**output:** **reference generated**
- detail: {'chars': 1422}

---
## [43] ⚙️ reference-impl · `fit`
**output:** **signature declared**
- detail: {'signature': 'DifferentiableMultivariateVectorOptimizer optimizer, WeightedObservedPoint[] observations, double[] initialGuess'}

---
## [44] ⚙️ reference-impl · `fit`
**output:** **reference observables matched**
- detail: {'matched': {'fit': 'fit'}, 'declared_only': ['fit_auto']}

---
## [45] ⚙️ reference-impl · `fit`
**output:** **reference too thin to screen — DISCARDED**
- reason: 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [46] ⚙️ reference-widening · `fit`
**output:** **widening REJECTED for observable `fit`**
- reason: reference too thin to screen — DISCARDED — 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~32,574 chars)</summary>

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
- Package: `org.apache.commons.math3.optimization.fitting` (`package org.apache.commons.math3.optimization.fitting;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_25_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
+++ /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
@@ -324,6 +324,9 @@
                     // procedure cannot produce sensible results.
 
                 a = FastMath.sqrt(c1 / c2);
+//ACS's patch begin
+	if (c2==0.0){throw new MathIllegalStateException();}
+//ACS's patch end
                 omega = FastMath.sqrt(c2 / c3);
             }
         }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math3.optimization.DifferentiableMultivariateVectorOptimizer;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
</source_imports>

Function `guessAOmega`:
<signature>
private void guessAOmega()
</signature>
<code>
        private void guessAOmega() {
            // initialize the sums for the linear model between the two integrals
            double sx2 = 0;
            double sy2 = 0;
            double sxy = 0;
            double sxz = 0;
            double syz = 0;

            double currentX = observations[0].getX();
            double currentY = observations[0].getY();
            double f2Integral = 0;
            double fPrime2Integral = 0;
            final double startX = currentX;
            for (int i = 1; i < observations.length; ++i) {
                // one step forward
                final double previousX = currentX;
                final double previousY = currentY;
                currentX = observations[i].getX();
                currentY = observations[i].getY();

                // update the integrals of f<sup>2</sup> and f'<sup>2</sup>
                // considering a linear model for f (and therefore constant f')
                final double dx = currentX - previousX;
                final double dy = currentY - previousY;
                final double f2StepIntegral =
                    dx * (previousY * previousY + previousY * currentY + currentY * currentY) / 3;
                final double fPrime2StepIntegral = dy * dy / dx;

                final double x = currentX - startX;
                f2Integral += f2StepIntegral;
                fPrime2Integral += fPrime2StepIntegral;

                sx2 += x * x;
                sy2 += f2Integral * f2Integral;
                sxy += x * f2Integral;
                sxz += x * fPrime2Integral;
                syz += f2Integral * fPrime2Integral;
            }

            // compute the amplitude and pulsation coefficients
            double c1 = sy2 * sxz - sxy * syz;
            double c2 = sxy * sxz - sx2 * syz;
            double c3 = sx2 * sy2 - sxy * sxy;
            if ((c1 / c2 < 0) || (c2 / c3 < 0)) {
                final int last = observations.length - 1;
                // Range of the observations, assuming that the
                // observations are sorted.
                final double xRange = observations[last].getX() - observations[0].getX();
                if (xRange == 0) {
                    throw new ZeroException();
                }
                omega = 2 * Math.PI / xRange;

                double yMin = Double.POSITIVE_INFINITY;
                double yMax = Double.NEGATIVE_INFINITY;
                for (int i = 1; i < observations.length; ++i) {
                    final double y = observations[i].getY();
                    if (y < yMin) {
                        yMin = y;
                    }
                    if (y > yMax) {
                        yMax = y;
                    }
                }
                a = 0.5 * (yMax - yMin);
            } else {
                    // In some ill-conditioned cases (cf. MATH-844), the guesser
                    // procedure cannot produce sensible results.

                a = FastMath.sqrt(c1 / c2);
                omega = FastMath.sqrt(c2 / c3);
            }
        }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `guessAOmega` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `guessAOmega`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public double[] guess()` (shared field(s): a, omega)
      doc: Estimate a first guess of the coefficients. @return the guessed coefficients, in the following order: <ul> <li>Amplitude</li> <li>Angular frequency</li> <li>Phase</li> </ul>
  - `private void guessPhi()` (shared field(s): observations, omega)
      doc: Estimate a first guess of the phase.
  - `public ParameterGuesser(WeightedObservedPoint[] observations)` (shared field(s): observations)
      doc: Simple constructor. @param observations sampled observations @throws NumberIsTooSmallException if the sample is too short or if the first guess cannot be computed.
    <constructor_body>
        public ParameterGuesser(WeightedObservedPoint[] observations) {
            if (observations.length < 4) {
                throw new NumberIsTooSmallException(LocalizedFormats.INSUFFICIENT_OBSERVED_POINTS_IN_SAMPLE,
                                                    observations.length, 4, true);
            }

            this.observations = observations.clone();
        }
    </constructor_body>
  - `public CurveFitter(DifferentiableMultivariateVectorOptimizer optimizer)` (shared field(s): observations)
      doc: Simple constructor. @param optimizer optimizer to use for the fitting
    <constructor_body>
    public CurveFitter(final DifferentiableMultivariateVectorOptimizer optimizer) {
        this.optimizer = optimizer;
        observations = new ArrayList<WeightedObservedPoint>();
    }
    </constructor_body>
  - `private void sortObservations()` (shared field(s): observations)
      doc: Sort the observations with respect to the abscissa.

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param optimizer Optimizer to use for the fitting.
  - @param initialGuess First guess values in the following order:
  - @throws NumberIsTooSmallException if the sample is too short for the
  - @throws ZeroException if the first guess cannot be computed because
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  fit(double[] initialGuess) / ()

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): fit, guess

THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does not throw — it fails a JUnit assertion because the code returns a WRONG value. There is therefore no exception to catch. Your harness must SUPPLY the oracle:

1. LIFT EVERY ASSERTION (not just one): the failing test almost always checks SEVERAL input/output pairs (e.g. many assertEquals lines). Each pair is a SEPARATE trusted oracle — the correct code is known to produce that exact expected value for that exact input. Find ALL of them (assertEquals / assertTrue / assertSame / assertNull ...), and for each identify (a) the call it makes on the real API and (b) the EXPECTED value it checks against. Copy both VERBATIM — do not infer or recompute; use the literals the test hard-codes.

2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls through the real public API, exactly as the test does, to get each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the test's SETUP: if the expected value depends on environment the test arranges (a registered source file, a Locale or TimeZone, a formatting mode, a fixture object), replicate that setup faithfully — and if you cannot replicate it, DROP that pair rather than assert it, because a half-reconstructed scenario fires on correct code too (that exact mistake caused two false accusations of correct patches).

3. ASSERT ALL OF THEM: if ANY actual value does not equal its expected value, `throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] semantic mismatch: <what differed>")`. Every DISTINCT check gets its own [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs], [oracle:round-trip]) — alarms without an ID are rejected mechanically; the ID is how the pipeline tells which check fired. Check every lifted pair — an overfitting patch that fixed only the one reported input still fails the others, so the more pairs you assert the more likely it is caught. Jazzer reports the throw as a finding, exactly like a crash, so scoring is unchanged. If all match, continue. WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT (compiler/printer output, rendered source, messages with layout): never compare raw strings — normalize BOTH sides first (collapse all whitespace runs, e.g. s.replaceAll("\\s+", "")) so the check fires only on a CONTENT difference. A correct implementation may legitimately differ in spaces, newlines or statement-separator placement, and a raw-string firing over formatting will be dismissed in review — burying the real content difference the same comparison would have caught. COMPARE NORMALIZED, BUT RECORD RAW: whenever you normalize before comparing, the alarm message MUST also carry the PRE-normalization values, under these exact key names: `expectedNormalized=` `actualNormalized=` `expectedRaw=` `actualRaw=`. Emit the two Raw keys ONLY when you actually normalized, so their absence means no normalization happened. Reason: a downstream check asks whether your reported value equals a value the failing test pins, and the test pins the RAW form — a message carrying only the normalized form can never match it, so that check is dead for your alarm and a real dismissal is lost. Named keys, never positional: the reader must never have to guess which form it is holding. KEEP THE WHOLE MESSAGE ON ONE LINE: the raw form of formatted text usually contains newlines and tabs, and the alarm is read line by line — a literal newline inside a value silently cuts the message there, discarding every key after it. So ESCAPE them: emit `\n` and `\t` as the two-character sequences (backslash then n / t), never as real line breaks. In Java: `s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")`. This is also the form the failing test's own source literal is written in, so the comparison lines up exactly.

4. THEN GENERALISE — but keep every assertion TRUSTED. The lifted pair tells you the answer for ONE input only. Do not invent an expected value for an arbitrary fuzzed input: an assertion you cannot justify will fire on CORRECT patches too (a false positive, the worst outcome). Extend coverage only in ways where the correct answer is independently known:

  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with FuzzedDataProvider, build the canonical input that must map to it, and assert you recover it — the answer is trusted because you chose it first. E.g. fuzz an int n, format it, parse it back, assert == n.

  (b) EQUIVALENCE TO THE SEED. The seed input has properties that fix its answer. Generate other inputs that SHARE those properties and must therefore share the seed's expected value, and assert they equal it. E.g. if the contract is case-insensitive and foo("abc")==X, then foo("ABC") and foo("Abc") must also ==X. This turns the one lifted value into an oracle for a whole family of inputs.

  (c) METAMORPHIC RELATION between two real calls (see below) — trusted because it holds for ANY correct implementation.

  GUARDRAIL: for any fuzzed input where you cannot justify the expected value by (a), (b), or (c), still CALL the API on it (this exercises the patched path), but do NOT assert on its result — just return. Assert only trusted answers; explore everything else without asserting.
  CALIBRATION: your assertions will be screened by an automated soundness reviewer before any firing counts, so do NOT hedge to trivially-weak checks to be safe. Write the strongest check you can JUSTIFY from the documented contract, and state that justification in a comment next to the check (the reviewer reads it). The reviewer is a safety net, not a licence to guess — it is imperfect, so an assertion still needs a real justification — but a vacuous assertion (isFinite / not-null / no-throw) can never catch a wrong value and guarantees a miss, which nothing downstream can recover.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `HarmonicFitter`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math3.optimization.fitting.HarmonicFitterTest" method="testMath844">
    @Test(expected=MathIllegalStateException.class)
    public void testMath844() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser guesser
            = new HarmonicFitter.ParameterGuesser(points);

        // The guesser fails because the function is far from an harmonic
        // function: It is a triangular periodic function with amplitude 3
        // and period 12, and all sample points are taken at integer abscissae
        // so function values all belong to the integer subset {-3, -2, -1, 0,
        // 1, 2, 3}.
        guesser.guess();
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math3.optimization.fitting.HarmonicFitterTest::testMath844
junit.framework.AssertionFailedError: Expected exception: org.apache.commons.math3.exception.MathIllegalStateException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
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

SYNTHESIZED RELATION CANDIDATES (mechanically pre-screened) — invariants/metamorphic relations derived from the documented contract of the patched code. Each was compiled and exercised over many fuzzed inputs on the current build; candidates that fired indiscriminately were dropped as out-of-domain. They are STRONG CANDIDATES, not verified ground truth: implement each one, keep its justification comment next to the check, and fence its inputs exactly as instructed below. These generalise BEYOND the tested inputs, so they can catch an overfit that only special-cased the reported input. Implement each one in your harness in addition to the lifted/mined oracles — but these are EXTRAS, not your whole job: KEEP INVENTING YOUR OWN checks beyond them. Two self-invented shapes have repeatedly been the ones that catch what nothing else does: (a) HIDDEN-STATE checks — after any call documented as a question (get*/is*/contains/indexOf/size, no mention of modifying), re-read the object's cheap observable properties (capacity, size, length, later lookups) and assert they did not change; a 'read-only' call that silently mutates internal state is a classic defect no provided relation will mention. (b) SIBLING-AGREEMENT checks — two methods documented to do the same job (overloads taking char vs String, Class vs class name, etc.) must agree on the same logical input:
<relation name="guesser_rejects_triangular_wave" kind="invariant">
  // holds because: For the ill-conditioned non-harmonic sample exercised by the trusted failing test, `HarmonicFitter.ParameterGuesser.guess()` must signal failure with `org.apache.commons.math3.exception.MathIllegalStateException` rather than returning coefficients.
  // valid input: Build a triangular periodic sample like the failing test, but vary the x-origin and x-step from fuzz data while keeping strictly increasing abscissae and non-zero range; scale the ordinates by a positive integer factor so the sample stays far from a cosine.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 20000/20000 on random inputs | CONSTANT-RECEIVER-STATE (structural blind spot): receiver-state probe `guesser.guess(...)` runs against a container whose STRUCTURE is compile-time constant: `guesser = new ParameterGuesser(...)` — every element count, index and position is a literal. The check draws data.consumeInt(), none of which reaches an index, a count, a loop bound, or a branch that governs construction, so every fuzz iteration rebuilds the SAME shape. A patch that only misbehaves for a different shape (a gap/hole, an empty or larger container, an element installed at a different index) is never exercised. Draw the structure from `data`: the number of elements, the indices they are installed at, whether the container is empty, and whether it is freshly built or mutated.
org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[] points;
org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser guesser;
try {
  int scale = data.consumeInt(1, 5);
  int step = data.consumeInt(1, 4);
  int start = data.consumeInt(-20, 20);
  int[] base = new int[] { 0, 1, 2, 3, 2, 1,
                           0, -1, -2, -3, -2, -1,
                           0, 1, 2, 3, 2, 1,
                           0, -1, -2, -3, -2, -1,
                           0, 1, 2, 3, 2, 1, 0 };
  points = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint[base.length];
  for (int i = 0; i < base.length; i++) {
    points[i] = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
  }
  guesser = new org.apache.commons.math3.optimization.fitting.HarmonicFitter.ParameterGuesser(points);
} catch (Exception e) { for (StackTraceElement __rpf : e.getStackTrace()) { String __rpc = __rpf.getClassName(); if ((__rpc.equals("HarmonicFitter") || __rpc.endsWith(".HarmonicFitter") || __rpc.endsWith("$HarmonicFitter")) && !"<init>".equals(__rpf.getMethodName())) { boolean __rpdoc = false; if (!__rpdoc && (__rpf.getClassName().equals("HarmonicFitter") || __rpf.getClassName().endsWith(".HarmonicFitter") || __rpf.getClassName().endsWith("$HarmonicFitter")) && "fit".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberIsTooSmallException") || __rpn.endsWith(".NumberIsTooSmallException") || __rpn.endsWith("$NumberIsTooSmallException") || __rpn.equals("ZeroException") || __rpn.endsWith(".ZeroException") || __rpn.endsWith("$ZeroException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("HarmonicFitter") || __rpf.getClassName().endsWith(".HarmonicFitter") || __rpf.getClassName().endsWith("$HarmonicFitter")) && "guessAOmega".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("MathIllegalStateException") || __rpn.endsWith(".MathIllegalStateException") || __rpn.endsWith("$MathIllegalStateException") || __rpn.equals("ZeroException") || __rpn.endsWith(".ZeroException") || __rpn.endsWith("$ZeroException")) { __rpdoc = true; break; } } } if (!__rpdoc) throw new RuntimeException("relation guesser_rejects_triangular_wave violated: unexpected " + e.getClass().getName() + " thrown by the patch-changed class on a valid-by-construction input: " + e.getMessage()); break; } }
  return;
}
boolean violated = false;
String detail = "completed normally";
try {
  guesser.guess();
  violated = true;
} catch (org.apache.commons.math3.exception.MathIllegalStateException ok) {
} catch (Throwable t) {
  violated = true;
  detail = "wrong exception class " + t.getClass().getName();
}
if (violated) {
  throw new RuntimeException("relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " + detail);
}
</relation>
<relation name="weighted_observed_point_getters_roundtrip_constructor_args" kind="invariant">
  // holds because: `WeightedObservedPoint` is an immutable container whose getters return the weight, abscissa, and ordinate supplied to the constructor.
  // valid input: Choose arbitrary finite constructor arguments from fuzz data and instantiate one `WeightedObservedPoint`.
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
double w;
double x;
double y;
org.apache.commons.math3.optimization.fitting.WeightedObservedPoint p;
try {
  w = data.consumeInt(-1000, 1000) / 10.0;
  x = data.consumeInt(-1000, 1000) / 10.0;
  y = data.consumeInt(-1000, 1000) / 10.0;
  p = new org.apache.commons.math3.optimization.fitting.WeightedObservedPoint(w, x, y);
} catch (Exception e) {
  return;
}
if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
    Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
    Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
  throw new RuntimeException("relation weighted_observed_point_getters_roundtrip_constructor_args violated: getters did not return constructor arguments");
}
</relation>
Build each relation's input with the FuzzedDataProvider so it is VALID BY CONSTRUCTION, wrap the API calls in try/catch and SKIP (return) on any caught exception — an exception is a rejection, never a violation. Throw com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("relation <name> violated: ...") only on a genuine disagreement, and include the observed values in the message. Two structural rules (checked mechanically, violations are rejected): (1) the violation throw goes OUTSIDE/AFTER the try/catch that wraps the API calls — thrown inside your own catch-everything block it is caught and discarded, dead code; (2) if you ever convert a CAUGHT exception into your own alarm, pass it as the cause: new FuzzerSecurityIssueLow("...", e) — never bury it in the message text.

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
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~12,697 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final int[] TRIANGULAR_BASE = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        relationWeightedObservedPointRoundtrip(data);
        oracleTriangularWaveMustThrowAndNotMutateCallerArray(data);
        oracleTriangularWaveViaPublicHarmonicFitter(data);
        relationFitOverloadsAgreeOnEquivalentInput(data);
        relationGuesserIsStableOnFixedObservations(data);
    }

    private static void relationWeightedObservedPointRoundtrip(FuzzedDataProvider data) {
        double w;
        double x;
        double y;
        WeightedObservedPoint p;
        try {
            w = data.consumeInt(-1000, 1000) / 10.0;
            x = data.consumeInt(-1000, 1000) / 10.0;
            y = data.consumeInt(-1000, 1000) / 10.0;
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                "weight=" + p.getWeight() + "/" + w +
                " x=" + p.getX() + "/" + x +
                " y=" + p.getY() + "/" + y);
        }
    }

    private static void oracleTriangularWaveMustThrowAndNotMutateCallerArray(FuzzedDataProvider data) {
        WeightedObservedPoint[] points;
        double[] originalXs;
        double[] originalYs;
        HarmonicFitter.ParameterGuesser guesser;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            points = new WeightedObservedPoint[TRIANGULAR_BASE.length];
            for (int i = 0; i < TRIANGULAR_BASE.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, TRIANGULAR_BASE[i] * (double) scale);
            }

            int swaps = data.consumeInt(0, TRIANGULAR_BASE.length * 2);
            for (int i = 0; i < swaps; i++) {
                int a = data.consumeInt(0, points.length - 1);
                int b = data.consumeInt(0, points.length - 1);
                WeightedObservedPoint tmp = points[a];
                points[a] = points[b];
                points[b] = tmp;
            }

            originalXs = new double[points.length];
            originalYs = new double[points.length];
            for (int i = 0; i < points.length; i++) {
                originalXs[i] = points[i].getX();
                originalYs[i] = points[i].getY();
            }

            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        Throwable wrong = null;
        try {
            guesser.guess();
            violated = true;
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            violated = true;
            wrong = t;
        }

        if (violated) {
            String msg = "[oracle:math844-guesser] semantic mismatch: expected MathIllegalStateException for the triangular-wave sample";
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    msg + ", but caught " + wrong.getClass().getName(), wrong);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                msg + ", but guess() completed normally");
        }

        boolean mutated = false;
        int mutatedIndex = -1;
        for (int i = 0; i < points.length; i++) {
            if (Double.doubleToLongBits(points[i].getX()) != Double.doubleToLongBits(originalXs[i]) ||
                Double.doubleToLongBits(points[i].getY()) != Double.doubleToLongBits(originalYs[i])) {
                mutated = true;
                mutatedIndex = i;
                break;
            }
        }
        if (mutated) {
            // Contract justification: ParameterGuesser constructor clones the provided array
            // (`this.observations = observations.clone();`), so later sorting/guessing must not
            // mutate the caller's array. A patch that merely deletes the throw but reuses/mutates
            // caller state would violate this observable post-condition.
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:constructor-clone-postcondition] semantic mismatch: caller array mutated at index " +
                mutatedIndex + " beforeX=" + originalXs[mutatedIndex] +
                " afterX=" + points[mutatedIndex].getX() +
                " beforeY=" + originalYs[mutatedIndex] +
                " afterY=" + points[mutatedIndex].getY());
        }
    }

    private static void oracleTriangularWaveViaPublicHarmonicFitter(FuzzedDataProvider data) {
        HarmonicFitter fitter;
        try {
            fitter = new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);

            int[] order = new int[TRIANGULAR_BASE.length];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            int swaps = data.consumeInt(0, order.length * 2);
            for (int i = 0; i < swaps; i++) {
                int a = data.consumeInt(0, order.length - 1);
                int b = data.consumeInt(0, order.length - 1);
                int tmp = order[a];
                order[a] = order[b];
                order[b] = tmp;
            }

            for (int i = 0; i < order.length; i++) {
                int idx = order[i];
                fitter.addObservedPoint(1.0, start + (double) idx * step, TRIANGULAR_BASE[idx] * (double) scale);
            }
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        Throwable wrong = null;
        try {
            fitter.fit();
            violated = true;
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            violated = true;
            wrong = t;
        }

        if (violated) {
            String msg = "[oracle:math844-public-fit] semantic mismatch: HarmonicFitter.fit() should reach ParameterGuesser and reject the same triangular-wave sample with MathIllegalStateException";
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    msg + ", but caught " + wrong.getClass().getName(), wrong);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                msg + ", but fit() completed normally");
        }
    }

    private static void relationFitOverloadsAgreeOnEquivalentInput(FuzzedDataProvider data) {
        WeightedObservedPoint[] points;
        double[] initialGuess;
        double[] fitWithoutGuess;
        double[] fitWithGuess;
        try {
            int n = data.consumeInt(8, 20);
            double a = data.consumeInt(1, 5);
            double omega = data.consumeInt(2, 12) / 10.0;
            double phi = data.consumeInt(-10, 10) / 10.0;
            double step = data.consumeInt(1, 10) / 10.0;
            double start = data.consumeInt(-20, 20) / 10.0;

            org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(a, omega, phi);
            points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
            }

            initialGuess = new HarmonicFitter.ParameterGuesser(points).guess();

            HarmonicFitter fitter1 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            HarmonicFitter fitter2 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < points.length; i++) {
                fitter1.addObservedPoint(points[i]);
                fitter2.addObservedPoint(points[i]);
            }

            fitWithoutGuess = fitter1.fit();
            fitWithGuess = fitter2.fit(initialGuess);
        } catch (Throwable t) {
            return;
        }

        if (!sameArrayWithin(fitWithoutGuess, fitWithGuess, 1.0e-8)) {
            // Contract justification: the no-arg fit() overload is the same logical operation as
            // fit(double[] initialGuess), with the initial guess supplied by the library's own
            // ParameterGuesser. On the same observations, these sibling overloads must agree.
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagreed " +
                "lhs=" + formatArray(fitWithoutGuess) +
                " rhs=" + formatArray(fitWithGuess) +
                " seedGuess=" + formatArray(initialGuess));
        }
    }

    private static void relationGuesserIsStableOnFixedObservations(FuzzedDataProvider data) {
        WeightedObservedPoint[] points;
        double[] g1;
        double[] g2;
        try {
            int n = data.consumeInt(8, 24);
            double a = data.consumeInt(1, 5);
            double omega = data.consumeInt(2, 12) / 10.0;
            double phi = data.consumeInt(-10, 10) / 10.0;
            double step = data.consumeInt(1, 10) / 10.0;
            double start = data.consumeInt(-20, 20) / 10.0;

            org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(a, omega, phi);
            points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
            }

            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            g1 = guesser.guess();
            g2 = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        if (!sameArrayWithin(g1, g2, 1.0e-12)) {
            // Contract justification: guess() is a reader-style estimation method over fixed
            // observations; with no state change to the observations between calls, repeating it
            // must report the same coefficients. This hidden-state check catches silent corruption
            // even if no exception is thrown.
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:guess-idempotence] metamorphic violation: repeated guess() calls on the same observations disagreed " +
                "first=" + formatArray(g1) + " second=" + formatArray(g2));
        }
    }

    private static boolean sameArrayWithin(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) || Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static String formatArray(double[] a) {
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
## [48] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-guesser] semantic mismatch: expected MathIllegalStateException for the triangular-wave sample, but guess() completed normally

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~41,114 chars, 1 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math3.optimization.fitting` (`package org.apache.commons.math3.optimization.fitting;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_25_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
+++ /src/main/java/org/apache/commons/math3/optimization/fitting/HarmonicFitter.java
@@ -324,6 +324,9 @@
                     // procedure cannot produce sensible results.
 
                 a = FastMath.sqrt(c1 / c2);
+//ACS's patch begin
+	if (c2==0.0){throw new MathIllegalStateException();}
+//ACS's patch end
                 omega = FastMath.sqrt(c2 / c3);
             }
         }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math3.optimization.DifferentiableMultivariateVectorOptimizer;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
</source_imports>

Function `guessAOmega`:
<signature>
private void guessAOmega()
</signature>
<code>
        private void guessAOmega() {
            // initialize the sums for the linear model between the two integrals
            double sx2 = 0;
            double sy2 = 0;
            double sxy = 0;
            double sxz = 0;
            double syz = 0;

            double currentX = observations[0].getX();
            double currentY = observations[0].getY();
            double f2Integral = 0;
            double fPrime2Integral = 0;
            final double startX = currentX;
            for (int i = 1; i < observations.length; ++i) {
                // one step forward
                final double previousX = currentX;
                final double previousY = currentY;
                currentX = observations[i].getX();
                currentY = observations[i].getY();

                // update the integrals of f<sup>2</sup> and f'<sup>2</sup>
                // considering a linear model for f (and therefore constant f')
                final double dx = currentX - previousX;
                final double dy = currentY - previousY;
                final double f2StepIntegral =
                    dx * (previousY * previousY + previousY * currentY + currentY * currentY) / 3;
                final double fPrime2StepIntegral = dy * dy / dx;

                final double x = currentX - startX;
                f2Integral += f2StepIntegral;
                fPrime2Integral += fPrime2StepIntegral;

                sx2 += x * x;
                sy2 += f2Integral * f2Integral;
                sxy += x * f2Integral;
                sxz += x * fPrime2Integral;
                syz += f2Integral * fPrime2Integral;
            }

            // compute the amplitude and pulsation coefficients
            double c1 = sy2 * sxz - sxy * syz;
            double c2 = sxy * sxz - sx2 * syz;
            double c3 = sx2 * sy2 - sxy * sxy;
            if ((c1 / c2 < 0) || (c2 / c3 < 0)) {
                final int last = observations.length - 1;
                // Range of the observations, assuming that the
                // observations are sorted.
                final double xRange = observations[last].getX() - observations[0].getX();
                if (xRange == 0) {
                    throw new ZeroException();
                }
                omega = 2 * Math.PI / xRange;

                double yMin = Double.POSITIVE_INFINITY;
                double yMax = Double.NEGATIVE_INFINITY;
                for (int i = 1; i < observations.length; ++i) {
                    final double y = observations[i].getY();
                    if (y < yMin) {
                        yMin = y;
                    }
                    if (y > yMax) {
                        yMax = y;
                    }
                }
                a = 0.5 * (yMax - yMin);
            } else {
                    // In some ill-conditioned cases (cf. MATH-844), the guesser
                    // procedure cannot produce sensible results.

                a = FastMath.sqrt(c1 / c2);
                omega = FastMath.sqrt(c2 / c3);
            }
        }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `guessAOmega` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `guessAOmega`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public double[] guess()` (shared field(s): a, omega)
      doc: Estimate a first guess of the coefficients. @return the guessed coefficients, in the following order: <ul> <li>Amplitude</li> <li>Angular frequency</li> <li>Phase</li> </ul>
  - `private void guessPhi()` (shared field(s): observations, omega)
      doc: Estimate a first guess of the phase.
  - `public ParameterGuesser(WeightedObservedPoint[] observations)` (shared field(s): observations)
      doc: Simple constructor. @param observations sampled observations @throws NumberIsTooSmallException if the sample is too short or if the first guess cannot be computed.
    <constructor_body>
        public ParameterGuesser(WeightedObservedPoint[] observations) {
            if (observations.length < 4) {
                throw new NumberIsTooSmallException(LocalizedFormats.INSUFFICIENT_OBSERVED_POINTS_IN_SAMPLE,
                                                    observations.length, 4, true);
            }

            this.observations = observations.clone();
        }
    </constructor_body>
  - `public CurveFitter(DifferentiableMultivariateVectorOptimizer optimizer)` (shared field(s): observations)
      doc: Simple constructor. @param optimizer optimizer to use for the fitting
    <constructor_body>
    public CurveFitter(final DifferentiableMultivariateVectorOptimizer optimizer) {
        this.optimizer = optimizer;
        observations = new ArrayList<WeightedObservedPoint>();
    }
    </constructor_body>
  - `private void sortObservations()` (shared field(s): observations)
      doc: Sort the observations with respect to the abscissa.

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="HarmonicFitter" role="patched">
     * Class that implements a curve fitting specialized for sinusoids.
     * Harmonic fitting is a very simple case of curve fitting. The
     * estimated coefficients are the amplitude a, the pulsation &omega; and
     * the phase &phi;: <code>f (t) = a cos (&omega; t + &phi;)</code>. They are
     * searched by a least square estimator initialized with a rough guess
     * based on integrals.
     * @version $Id$
     * @since 2.0
public class HarmonicFitter extends CurveFitter<HarmonicOscillator.Parametric> {
     * Simple constructor.
     * @param optimizer Optimizer to use for the fitting.
    public HarmonicFitter(final DifferentiableMultivariateVectorOptimizer optimizer) {
        super(optimizer);
    }
     * Fit an harmonic function to the observed points.
     * @param initialGuess First guess values in the following order:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
     * @return the parameters of the harmonic function that best fits the
     * observed points (in the same order as above).
    public double[] fit(double[] initialGuess) { … }
     * Fit an harmonic function to the observed points.
     * An initial guess will be automatically computed.
     * @return the parameters of the harmonic function that best fits the
     * observed points (see the other {@link #fit(double[]) fit} method.
     * @throws NumberIsTooSmallException if the sample is too short for the
     * the first guess to be computed.
     * @throws ZeroException if the first guess cannot be computed because
     * the abscissa range is zero.
    public double[] fit() { … }
     * This class guesses harmonic coefficients from a sample.
     * <p>The algorithm used to guess the coefficients is as follows:</p>
     * <p>We know f (t) at some sampling points t<sub>i</sub> and want to find a,
     * &omega; and &phi; such that f (t) = a cos (&omega; t + &phi;).
     * </p>
     * <p>From the analytical expression, we can compute two primitives :
     * <pre>
     * If2  (t) = &int; f<sup>2</sup>  = a<sup>2</sup> &times; [t + S (t)] / 2
     * If'2 (t) = &int; f'<sup>2</sup> = a<sup>2</sup> &omega;<sup>2</sup> &times; [t - S (t)] / 2
     * where S (t) = sin (2 (&omega; t + &phi;)) / (2 &omega;)
     * </pre>
     * </p>
     * …
    public static class ParameterGuesser { … }
}
</class>

<class name="CurveFitter" role="supertype-contract">
class TheoreticalValuesFunction
        implements DifferentiableMultivariateVectorFunction {
     * Function to fit.
    private final ParametricUnivariateFunction f;
     * Simple constructor.
     * @param f function to fit.
    public TheoreticalValuesFunction(final ParametricUnivariateFunction f) {
            this.f = f;
        }
     * {@inheritDoc}
    public MultivariateMatrixFunction jacobian() { … }
     * {@inheritDoc}
    public double[] value(double[] point) { … }
}
</class>

<class name="WeightedObservedPoint" role="test-subject">
     * This class is a simple container for weighted observed point in
     * {@link CurveFitter curve fitting}.
     * <p>Instances of this class are guaranteed to be immutable.</p>
     * @version $Id$
     * @since 2.0
public class WeightedObservedPoint implements Serializable {
     * Serializable version id.
    private static final long serialVersionUID = 5306874947404636157L;
     * Weight of the measurement in the fitting process.
    private final double weight;
     * Abscissa of the point.
    private final double x;
     * Observed value of the function at x.
    private final double y;
     * Simple constructor.
     * @param weight weight of the measurement in the fitting process
     * @param x abscissa of the measurement
     * @param y ordinate of the measurement
    public WeightedObservedPoint(final double weight, final double x, final double y) {
        this.weight = weight;
        this.x      = x;
        this.y      = y;
    }
     * Get the weight of the measurement in the fitting process.
     * @return weight of the measurement in the fitting process
    public double getWeight() { … }
     * Get the abscissa of the point.
     * @return abscissa of the point
    public double getX() { … }
     * Get the observed value of the function at x.
     * @return observed value of the function at x
    public double getY() { … }
}
</class>

<class name="MathIllegalStateException" role="test-subject">
     * Base class for all exceptions that signal a mismatch between the
     * current state and the user's expectations.
     * @since 2.2
     * @version $Id$
public class MathIllegalStateException extends IllegalStateException
    implements ExceptionContextProvider {
     * Serializable version Id.
    private static final long serialVersionUID = -6024911025449780478L;
     * Context.
    private final ExceptionContext context;
     * Simple constructor.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Localizable pattern, Object ... args) {
        context = new ExceptionContext(this);
        context.addMessage(pattern, args);
    }
     * Simple constructor.
     * @param cause Root cause.
     * @param pattern Message pattern explaining the cause of the error.
     * @param args Arguments.
    public MathIllegalStateException(Throwable cause, Localizable pattern, Object ... args) {
        super(cause);
        context = new ExceptionContext(this);
        context.addMessage(pattern, args);
    }
     * Default constructor.
    public MathIllegalStateException() {
        this(LocalizedFormats.ILLEGAL_STATE);
    }
     * {@inheritDoc}
    public ExceptionContext getContext() { … }
     * {@inheritDoc}
    @Override public String getMessage() { … }
     * {@inheritDoc}
    @Override public String getLocalizedMessage() { … }
}
</class>

<class name="FastMath" role="collaborator">
     * Faster, more accurate, portable alternative to {@link Math} and
     * {@link StrictMath} for large scale computation.
     * <p>
     * FastMath is a drop-in replacement for both Math and StrictMath. This
     * means that for any method in Math (say {@code Math.sin(x)} or
     * {@code Math.cbrt(y)}), user can directly change the class and use the
     * methods as is (using {@code FastMath.sin(x)} or {@code FastMath.cbrt(y)}
     * in the previous example).
     * </p>
     * <p>
     * FastMath speed is achieved by relying heavily on optimizing compilers
     * to native code present in many JVMs today and use of large tables.
     * …
public class FastMath {
     * Archimede's constant PI, ratio of circle circumference to diameter.
    public static final double PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9;
     * Napier's constant e, base of the natural logarithm.
    public static final double E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8;
     * Index of exp(0) in the array of integer exponentials. */
     * static final int EXP_INT_TABLE_MAX_INDEX = 750;
     * /** Length of the array of integer exponentials. */
     * static final int EXP_INT_TABLE_LEN = EXP_INT_TABLE_MAX_INDEX * 2;
     * /** Logarithm table length. */
     * static final int LN_MANT_LEN = 1024;
     * /** Exponential fractions table length. */
     * static final int EXP_FRAC_TABLE_LEN = 1025; // 0, 1/1024, ... 1024/1024
     * /** Indicator for tables initialization.
     * <p>
     * This compile-time constant should be set to true only if one explicitly
     * wants to compute the tables at class loading time instead of using the
     * …
    private static final boolean RECOMPUTE_TABLES_AT_RUNTIME = false;
     * log(2) (high bits).
    private static final double LN_2_A = 0.693147063255310059;
     * log(2) (low bits).
    private static final double LN_2_B = 1.17304635250823482e-7;
     * Coefficients for log, when input 0.99 < x < 1.01.
    private static final double LN_QUICK_COEF[][] = { … }
     * Coefficients for log in the range of 1.0 < x < 1.0 + 2^-10.
    private static final double LN_HI_PREC_COEF[][] = { … }
     * Sine, Cosine, Tangent tables are for 0, 1/8, 2/8, ... 13/8 = PI/2 approx.
    private static final int SINE_TABLE_LEN = 14;
     * Sine table (high bits).
    private static final double SINE_TABLE_A[] = { … }
     * Sine table (low bits).
    private static final double SINE_TABLE_B[] = { … }
     * Cosine table (high bits).
    private static final double COSINE_TABLE_A[] = { … }
     * Cosine table (low bits).
    private static final double COSINE_TABLE_B[] = { … }
     * Tangent table, used by atan() (high bits).
    private static final double TANGENT_TABLE_A[] = { … }
     * Tangent table, used by atan() (low bits).
    private static final double TANGENT_TABLE_B[] = { … }
     * Bits of 1/(2*pi), need for reducePayneHanek().
    private static final long RECIP_2PI[] = new long[] { … }
     * Bits of pi/4, need for reducePayneHanek().
    private static final long PI_O_4_BITS[] = new long[] { … }
     * Eighths.
     * This is used by sinQ, because its faster to do a table lookup than
     * a multiply in this time-critical routine
    private static final double EIGHTHS[] = { … }
     * Table of 2^((n+2)/3)
    private static final double CBRTTWO[] = { … }
     * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
     * Equivalent to 2^30.
    private static final long HEX_40000000 = 0x40000000L;
     * Mask used to clear low order 30 bits
    private static final long MASK_30BITS = -1L - (HEX_40000000 -1);
     * 2^52 - double numbers this large must be integral (no fraction) or NaN or Infinite
    private static final double TWO_POWER_52 = 4503599627370496.0;
     * Constant: {@value}.
    private static final double F_1_3 = 1d / 3d;
     * Constant: {@value}.
    private static final double F_1_5 = 1d / 5d;
     * Constant: {@value}.
    private static final double F_1_7 = 1d / 7d;
     * Constant: {@value}.
    private static final double F_1_9 = 1d / 9d;
     * Constant: {@value}.
    private static final double F_1_11 = 1d / 11d;
     * Constant: {@value}.
    private static final double F_1_13 = 1d / 13d;
     * Constant: {@value}.
    private static final double F_1_15 = 1d / 15d;
     * Constant: {@value}.
    private static final double F_1_17 = 1d / 17d;
     * Constant: {@value}.
    private static final double F_3_4 = 3d / 4d;
     * Constant: {@value}.
    private static final double F_15_16 = 15d / 16d;
     * Constant: {@value}.
    private static final double F_13_14 = 13d / 14d;
     * Constant: {@value}.
    private static final double F_11_12 = 11d / 12d;
     * Constant: {@value}.
    private static final double F_9_10 = 9d / 10d;
     * Constant: {@value}.
    private static final double F_7_8 = 7d / 8d;
     * Constant: {@value}.
    private static final double F_5_6 = 5d / 6d;
     * Constant: {@value}.
    private static final double F_1_2 = 1d / 2d;
     * Constant: {@value}.
    private static final double F_1_4 = 1d / 4d;
     * Private Constructor
    private FastMath() {}
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     * @param d the value to split
     * @return the high order part of the mantissa
    private static double doubleHighPart(double d) { … }
     * Natural logarithm.
     * @param x   a double
     * @return log(x)
    public static double log(final double x) { … }
     * Internal helper method for natural logarithm function.
     * @param x original argument of the natural logarithm function
     * @param hiPrec extra bits of precision on output (To Be Confirmed)
     * @return log(x)
    private static double log(final double x, final double[] hiPrec) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="ZeroException" role="collaborator">
     * Exception to be thrown when zero is provided where it is not allowed.
     * @since 2.2
     * @version $Id$
public class ZeroException extends MathIllegalNumberException {
     * Serializable version identifier
    private static final long serialVersionUID = -1960874856936000015L;
     * Construct the exception.
    public ZeroException() {
        this(LocalizedFormats.ZERO_NOT_ALLOWED);
    }
     * Construct the exception with a specific context.
     * @param specific Specific context pattern.
     * @param arguments Arguments.
    public ZeroException(Localizable specific, Object ... arguments) {
        super(specific, 0, arguments);
    }
}
</class>

<class name="NumberIsTooSmallException" role="collaborator">
     * Exception to be thrown when a number is too small.
     * @since 2.2
     * @version $Id$
public class NumberIsTooSmallException extends MathIllegalNumberException {
     * Serializable version Id.
    private static final long serialVersionUID = -6100997100383932834L;
     * Higher bound.
    private final Number min;
     * Whether the maximum is included in the allowed range.
    private final boolean boundIsAllowed;
     * Construct the exception.
     * @param wrong Value that is smaller than the minimum.
     * @param min Minimum.
     * @param boundIsAllowed Whether {@code min} is included in the allowed range.
    public NumberIsTooSmallException(Number wrong, Number min, boolean boundIsAllowed) {
        this(boundIsAllowed ?
             LocalizedFormats.NUMBER_TOO_SMALL :
             LocalizedFormats.NUMBER_TOO_SMALL_BOUND_EXCLUDED,
             wrong, min, boundIsAllowed);
    }
     * Construct the exception with a specific context.
     * @param specific Specific context pattern.
     * @param wrong Value that is smaller than the minimum.
     * @param min Minimum.
     * @param boundIsAllowed Whether {@code min} is included in the allowed range.
    public NumberIsTooSmallException(Localizable specific, Number wrong, Number min, boolean boundIsAllowed) {
        super(specific, wrong, min);

        this.min = min;
        this.boundIsAllowed = boundIsAllowed;
    }
     * @return {@code true} if the minimum is included in the allowed range.
    public boolean getBoundIsAllowed() { … }
     * @return the minimum.
    public Number getMin() { … }
}
</class>

<class name="HarmonicOscillator" role="collaborator">
     * <a href="http://en.wikipedia.org/wiki/Harmonic_oscillator">
     * simple harmonic oscillator</a> function.
     * @since 3.0
     * @version $Id$
public class HarmonicOscillator implements DifferentiableUnivariateFunction {
     * Amplitude.
    private final double amplitude;
     * Angular frequency.
    private final double omega;
     * Phase.
    private final double phase;
     * Harmonic oscillator function.
     * @param amplitude Amplitude.
     * @param omega Angular frequency.
     * @param phase Phase.
    public HarmonicOscillator(double amplitude, double omega, double phase) {
        this.amplitude = amplitude;
        this.omega = omega;
        this.phase = phase;
    }
     * {@inheritDoc}
    public double value(double x) { … }
     * {@inheritDoc}
    public UnivariateFunction derivative() { … }
     * Parametric function where the input array contains the parameters of
     * the harmonic oscillator function, ordered as follows:
     * <ul>
     * <li>Amplitude</li>
     * <li>Angular frequency</li>
     * <li>Phase</li>
     * </ul>
    public static class Parametric implements ParametricUnivariateFunction { … }
     * @param xTimesOmegaPlusPhase {@code omega * x + phase}.
     * @param amplitude Amplitude.
     * @return the value of the harmonic oscillator function at {@code x}.
    private static double value(double xTimesOmegaPlusPhase, double amplitude) { … }
}
</class>

<class name="DifferentiableMultivariateVectorOptimizer" role="collaborator">
     * This interface represents an optimization algorithm for
     * {@link DifferentiableMultivariateVectorFunction vectorial differentiable
     * objective functions}.
     * @version $Id$
     * @since 3.0
public interface DifferentiableMultivariateVectorOptimizer
    extends BaseMultivariateVectorOptimizer<DifferentiableMultivariateVectorFunction> {

}
</class>
</class_skeletons>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param optimizer Optimizer to use for the fitting.
  - @param initialGuess First guess values in the following order:
  - @throws NumberIsTooSmallException if the sample is too short for the
  - @throws ZeroException if the first guess cannot be computed because
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  fit(double[] initialGuess) / ()

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): fit, guess

THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does not throw — it fails a JUnit assertion because the code returns a WRONG value. There is therefore no exception to catch. Your harness must SUPPLY the oracle:

1. LIFT EVERY ASSERTION (not just one): the failing test almost always checks SEVERAL input/output pairs (e.g. many assertEquals lines). Each pair is a SEPARATE trusted oracle — the correct code is known to produce that exact expected value for that exact input. Find ALL of them (assertEquals / assertTrue / assertSame / assertNull ...), and for each identify (a) the call it makes on the real API and (b) the EXPECTED value it checks against. Copy both VERBATIM — do not infer or recompute; use the literals the test hard-codes.

2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls through the real public API, exactly as the test does, to get each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the test's SETUP: if the expected value depends on environment the test arranges (a registered source file, a Locale or TimeZone, a formatting mode, a fixture object), replicate that setup faithfully — and if you cannot replicate it, DROP that pair rather than assert it, because a half-reconstructed scenario fires on correct code too (that exact mistake caused two false accusations of correct patches).

3. ASSERT ALL OF THEM: if ANY actual value does not equal its expected value, `throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] semantic mismatch: <what differed>")`. Every DISTINCT check gets its own [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs], [oracle:round-trip]) — alarms without an ID are rejected mechanically; the ID is how the pipeline tells which check fired. Check every lifted pair — an overfitting patch that fixed only the one reported input still fails the others, so the more pairs you assert the more likely it is caught. Jazzer reports the throw as a finding, exactly like a crash, so scoring is unchanged. If all match, continue. WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT (compiler/printer output, rendered source, messages with layout): never compare raw strings — normalize BOTH sides first (collapse all whitespace runs, e.g. s.replaceAll("\\s+", "")) so the check fires only on a CONTENT difference. A correct implementation may legitimately differ in spaces, newlines or statement-separator placement, and a raw-string firing over formatting will be dismissed in review — burying the real content difference the same comparison would have caught. COMPARE NORMALIZED, BUT RECORD RAW: whenever you normalize before comparing, the alarm message MUST also carry the PRE-normalization values, under these exact key names: `expectedNormalized=` `actualNormalized=` `expectedRaw=` `actualRaw=`. Emit the two Raw keys ONLY when you actually normalized, so their absence means no normalization happened. Reason: a downstream check asks whether your reported value equals a value the failing test pins, and the test pins the RAW form — a message carrying only the normalized form can never match it, so that check is dead for your alarm and a real dismissal is lost. Named keys, never positional: the reader must never have to guess which form it is holding. KEEP THE WHOLE MESSAGE ON ONE LINE: the raw form of formatted text usually contains newlines and tabs, and the alarm is read line by line — a literal newline inside a value silently cuts the message there, discarding every key after it. So ESCAPE them: emit `\n` and `\t` as the two-character sequences (backslash then n / t), never as real line breaks. In Java: `s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")`. This is also the form the failing test's own source literal is written in, so the comparison lines up exactly.

4. THEN GENERALISE — but keep every assertion TRUSTED. The lifted pair tells you the answer for ONE input only. Do not invent an expected value for an arbitrary fuzzed input: an assertion you cannot justify will fire on CORRECT patches too (a false positive, the worst outcome). Extend coverage only in ways where the correct answer is independently known:

  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with FuzzedDataProvider, build the canonical input that must map to it, and assert you recover it — the answer is trusted because you chose it first. E.g. fuzz an int n, format it, parse it back, assert == n.

  (b) EQUIVALENCE TO THE SEED. The seed input has properties that fix its answer. Generate other inputs that SHARE those properties and must therefore share the seed's expected value, and assert they equal it. E.g. if the contract is case-insensitive and foo("abc")==X, then foo("ABC") and foo("Abc") must also ==X. This turns the one lifted value into an oracle for a whole family of inputs.

  (c) METAMORPHIC RELATION between two real calls (see below) — trusted because it holds for ANY correct implementation.

  GUARDRAIL: for any fuzzed input where you cannot justify the expected value by (a), (b), or (c), still CALL the API on it (this exercises the patched path), but do NOT assert on its result — just return. Assert only trusted answers; explore everything else without asserting.
  CALIBRATION: your assertions will be screened by an automated soundness reviewer before any firing counts, so do NOT hedge to trivially-weak checks to be safe. Write the strongest check you can JUSTIFY from the documented contract, and state that justification in a comment next to the check (the reviewer reads it). The reviewer is a safety net, not a licence to guess — it is imperfect, so an assertion still needs a real justification — but a vacuous assertion (isFinite / not-null / no-throw) can never catch a wrong value and guarantees a miss, which nothing downstream can recover.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `HarmonicFitter`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math3.optimization.fitting.HarmonicFitterTest" method="testMath844">
    @Test(expected=MathIllegalStateException.class)
    public void testMath844() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser guesser
            = new HarmonicFitter.ParameterGuesser(points);

        // The guesser fails because the function is far from an harmonic
        // function: It is a triangular periodic function with amplitude 3
        // and period 12, and all sample points are taken at integer abscissae
        // so function values all belong to the integer subset {-3, -2, -1, 0,
        // 1, 2, 3}.
        guesser.guess();
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math3.optimization.fitting.HarmonicFitterTest::testMath844
junit.framework.AssertionFailedError: Expected exception: org.apache.commons.math3.exception.MathIllegalStateException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
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
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,803 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.ZeroException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            double[] actual = guesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but guess() returned "
                    + java.util.Arrays.toString(actual));
        } catch (MathIllegalStateException expected) {
            // Lifted exactly from HarmonicFitterTest.testMath844:
            // this specific sample must throw MathIllegalStateException.
        } catch (Throwable other) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but got "
                    + other.getClass().getName() + ": " + String.valueOf(other.getMessage()));
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(null);
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(1.0, i, y[i]);
            }
            fitter.fit();
        } catch (Throwable ignored) {
            // This public API call is only to ensure the patched path is also reached through HarmonicFitter.
        }

        int n = data.consumeInt(6, 20);
        double amplitude = data.consumeInt(1, 20);
        double omega = data.consumeInt(1, 10);
        double phase = data.consumeInt(-30, 30) / 10.0;
        double step = data.consumeInt(1, 10) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] ordered = new WeightedObservedPoint[n];
        WeightedObservedPoint[] permuted = new WeightedObservedPoint[n];
        double[] permutedXBefore = new double[n];
        double[] permutedYBefore = new double[n];
        double[] permutedWBefore = new double[n];

        for (int i = 0; i < n; i++) {
            double x = i * step;
            double val = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, val);
            ordered[i] = p;
            permuted[i] = p;
            permutedXBefore[i] = p.getX();
            permutedYBefore[i] = p.getY();
            permutedWBefore[i] = p.getWeight();
        }

        for (int i = 0; i < n; i++) {
            int j = data.consumeInt(0, n - 1);
            WeightedObservedPoint tmp = permuted[i];
            permuted[i] = permuted[j];
            permuted[j] = tmp;
        }

        for (int i = 0; i < n; i++) {
            permutedXBefore[i] = permuted[i].getX();
            permutedYBefore[i] = permuted[i].getY();
            permutedWBefore[i] = permuted[i].getWeight();
        }

        try {
            HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(ordered);
            HarmonicFitter.ParameterGuesser g2 = new HarmonicFitter.ParameterGuesser(permuted);

            double[] a1 = g1.guess();
            double[] a2 = g2.guess();

            // Contract justification:
            // ParameterGuesser stores a clone of the caller array in its constructor, and
            // sortObservations() sorts observations by abscissa internally. Therefore two arrays
            // containing the same points in different orders are equivalent inputs and must yield
            // the same guess. A patch that silently skips/changes the real computation can violate
            // this observable equivalence even when no exception is thrown.
            if (a1.length != a2.length) {
                throw new RuntimeException(
                    "[oracle:permutation-equivalence] metamorphic violation: different result lengths input=n=" + n
                        + " lhs=" + java.util.Arrays.toString(a1)
                        + " rhs=" + java.util.Arrays.toString(a2));
            }
            for (int i = 0; i < a1.length; i++) {
                if (Double.doubleToLongBits(a1[i]) != Double.doubleToLongBits(a2[i])) {
                    throw new RuntimeException(
                        "[oracle:permutation-equivalence] metamorphic violation: guess(points)==guess(permutation(points)) "
                            + "input=n=" + n
                            + ", amplitude=" + amplitude
                            + ", omega=" + omega
                            + ", phase=" + phase
                            + ", step=" + step
                            + " lhs=" + java.util.Arrays.toString(a1)
                            + " rhs=" + java.util.Arrays.toString(a2));
                }
            }

            // Contract justification:
            // The constructor clones the observations array, so calling guess() must not reorder or
            // replace elements in the caller-provided array.
            for (int i = 0; i < n; i++) {
                if (Double.doubleToLongBits(permuted[i].getX()) != Double.doubleToLongBits(permutedXBefore[i]) ||
                    Double.doubleToLongBits(permuted[i].getY()) != Double.doubleToLongBits(permutedYBefore[i]) ||
                    Double.doubleToLongBits(permuted[i].getWeight()) != Double.doubleToLongBits(permutedWBefore[i])) {
                    throw new RuntimeException(
                        "[oracle:caller-array-unchanged] metamorphic violation: guess() modified caller observations array "
                            + "at index=" + i);
                }
            }
        } catch (NumberIsTooSmallException ignored) {
            return;
        } catch (ZeroException ignored) {
            return;
        } catch (MathIllegalStateException ignored) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException but got com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [or…

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,186 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final int[] TRIANGULAR_BASE = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Trusted lifted oracle from HarmonicFitterTest.testMath844:
        // for this triangular-wave sample, ParameterGuesser.guess() must throw MathIllegalStateException.
        WeightedObservedPoint[] exactTestPoints = new WeightedObservedPoint[TRIANGULAR_BASE.length];
        for (int i = 0; i < TRIANGULAR_BASE.length; i++) {
            exactTestPoints[i] = new WeightedObservedPoint(1.0, i, TRIANGULAR_BASE[i]);
        }
        HarmonicFitter.ParameterGuesser exactGuesser = new HarmonicFitter.ParameterGuesser(exactTestPoints);

        // Also drive the same data through the real public API path.
        try {
            HarmonicFitter publicApiFitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < exactTestPoints.length; i++) {
                publicApiFitter.addObservedPoint(exactTestPoints[i]);
            }
            try {
                publicApiFitter.fit();
            } catch (Throwable ignored) {
                // This call is for reachability through the real public API.
            }
        } catch (Throwable ignored) {
            // If optimizer setup fails in this environment, keep the trusted direct oracle below.
        }

        boolean exactOracleViolated = false;
        String exactDetail = "completed normally";
        Throwable exactCause = null;
        try {
            exactGuesser.guess();
            exactOracleViolated = true;
        } catch (MathIllegalStateException expected) {
            // Expected per the trusted failing test.
        } catch (Throwable t) {
            exactOracleViolated = true;
            exactDetail = "threw wrong exception " + t.getClass().getName();
            exactCause = t;
        }
        if (exactOracleViolated) {
            if (exactCause != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + exactDetail,
                    exactCause);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + exactDetail);
        }

        // Generalized trusted family from the same seed properties: same triangular waveform shape,
        // with fuzzed positive scale, strictly increasing x, and non-zero range.
        // For this ill-conditioned non-harmonic sample family, guess() must reject with MathIllegalStateException.
        WeightedObservedPoint[] generalizedPoints;
        HarmonicFitter.ParameterGuesser generalizedGuesser;
        Throwable generalizedUnexpected = null;
        String generalizedDetail = "completed normally";
        boolean generalizedViolated = false;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);

            generalizedPoints = new WeightedObservedPoint[TRIANGULAR_BASE.length];
            for (int i = 0; i < TRIANGULAR_BASE.length; i++) {
                generalizedPoints[i] = new WeightedObservedPoint(
                    1.0,
                    start + (double) i * step,
                    TRIANGULAR_BASE[i] * (double) scale);
            }
            generalizedGuesser = new HarmonicFitter.ParameterGuesser(generalizedPoints);
        } catch (Throwable e) {
            return;
        }
        try {
            generalizedGuesser.guess();
            generalizedViolated = true;
        } catch (MathIllegalStateException expected) {
            // Expected for this seeded family.
        } catch (Throwable t) {
            generalizedViolated = true;
            generalizedDetail = "threw wrong exception " + t.getClass().getName();
            generalizedUnexpected = t;
        }
        if (generalizedViolated) {
            if (generalizedUnexpected != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:triangular-family] semantic mismatch: expected MathIllegalStateException, but " + generalizedDetail,
                    generalizedUnexpected);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:triangular-family] semantic mismatch: expected MathIllegalStateException, but " + generalizedDetail);
        }

        // Invariant from WeightedObservedPoint's immutable container contract:
        // getters must return exactly the constructor arguments.
        double w;
        double x;
        double y;
        WeightedObservedPoint p;
        try {
            w = data.consumeInt(-1000, 1000) / 10.0;
            x = data.consumeInt(-1000, 1000) / 10.0;
            y = data.consumeInt(-1000, 1000) / 10.0;
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable e) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:wop-roundtrip] semantic mismatch: weight/x/y getters did not return constructor arguments"
                    + " expected=(" + w + "," + x + "," + y + ")"
                    + " actual=(" + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }

        // Post-condition / hidden-state check:
        // ParameterGuesser(WeightedObservedPoint[]) clones the input array ("this.observations = observations.clone();").
        // Therefore replacing elements in the caller's original array after construction must not change what guess() reads.
        // A patch that silently corrupts shared state or stops using the constructor-established observation set breaks this.
        try {
            int n = data.consumeInt(6, 14);
            int xStep = data.consumeInt(1, 4);
            int xStart = data.consumeInt(-10, 10);
            double amplitude = data.consumeInt(1, 5);
            double omega = data.consumeInt(1, 5) / 5.0;
            double phi = data.consumeInt(-15, 15) / 10.0;

            HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phi);
            WeightedObservedPoint[] original = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = xStart + i * xStep;
                original[i] = new WeightedObservedPoint(1.0, xi, oscillator.value(xi));
            }

            HarmonicFitter.ParameterGuesser cloneCheckGuesser = new HarmonicFitter.ParameterGuesser(original);
            double[] before = cloneCheckGuesser.guess();
            double[] beforeAgain = cloneCheckGuesser.guess();

            if (!sameBits(before, beforeAgain)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same receiver changed result"
                        + " first=" + format(before) + " second=" + format(beforeAgain));
            }

            int mutations = data.consumeInt(1, n);
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, n - 1);
                double xi = xStart + (n + i + 1) * xStep;
                original[idx] = new WeightedObservedPoint(
                    7.0,
                    xi,
                    data.consumeInt(-50, 50));
            }

            double[] after = cloneCheckGuesser.guess();
            if (!sameBits(before, after)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone] metamorphic violation: mutating caller array after ParameterGuesser construction changed guess()"
                        + " before=" + format(before) + " after=" + format(after));
            }
        } catch (Throwable e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) e;
            }
            return;
        }

        // Sibling-agreement check on the real public API:
        // fit() and fit(double[] initialGuess) do the same fitting job; using the guess produced from the same observations
        // should make the overloads agree. If either side throws, the check does not apply and we skip.
        try {
            int n = data.consumeInt(6, 16);
            int xStep = data.consumeInt(1, 3);
            int xStart = data.consumeInt(-8, 8);
            double amplitude = data.consumeInt(1, 4);
            double omega = data.consumeInt(1, 6) / 4.0;
            double phi = data.consumeInt(-20, 20) / 10.0;
            HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phi);

            WeightedObservedPoint[] pts = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = xStart + i * xStep;
                pts[i] = new WeightedObservedPoint(1.0, xi, oscillator.value(xi));
            }

            HarmonicFitter.ParameterGuesser pg = new HarmonicFitter.ParameterGuesser(pts);
            double[] initialGuess = pg.guess();

            HarmonicFitter fitterA = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            HarmonicFitter fitterB = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < pts.length; i++) {
                fitterA.addObservedPoint(pts[i]);
                fitterB.addObservedPoint(pts[i]);
            }

            double[] fitNoArg = fitterA.fit();
            double[] fitWithGuess = fitterB.fit(initialGuess);

            if (!approximatelyEqual(fitNoArg, fitWithGuess, 1e-6)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:fit-overloads] metamorphic violation: fit() and fit(initialGuess) disagreed"
                        + " fit()=" + format(fitNoArg)
                        + " fit(initialGuess)=" + format(fitWithGuess));
            }
        } catch (Throwable e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) e;
            }
            return;
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean approximatelyEqual(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) || Double.isNaN(b[i]) || Double.isInfinite(a[i]) || Double.isInfinite(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
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
## [52] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but completed normally

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,403 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave sample used by HarmonicFitterTest.testMath844"
            );
        } catch (MathIllegalStateException expected) {
        } catch (RuntimeException other) {
            return;
        }

        int sampleCount = data.consumeInt(8, 20);
        double amplitude = 0.5 + (data.consumeInt(0, 4500) / 1000.0);
        double omega = 0.2 + (data.consumeInt(0, 2800) / 1000.0);
        double phase = -Math.PI + (data.consumeInt(0, 6283) / 1000.0);
        double step = 0.1 + (data.consumeInt(0, 900) / 1000.0);
        double start = data.consumeInt(-1000, 1000) / 100.0;

        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = start + i * step;
            harmonicPoints[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }

        try {
            HarmonicFitter.ParameterGuesser sortedGuesser = new HarmonicFitter.ParameterGuesser(harmonicPoints);
            double[] sortedGuess = sortedGuesser.guess();

            WeightedObservedPoint[] reversed = harmonicPoints.clone();
            for (int i = 0, j = reversed.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = reversed[i];
                reversed[i] = reversed[j];
                reversed[j] = tmp;
            }
            HarmonicFitter.ParameterGuesser reversedGuesser = new HarmonicFitter.ParameterGuesser(reversed);
            double[] reversedGuess = reversedGuesser.guess();

            if (sortedGuess.length != reversedGuess.length) {
                throw new RuntimeException(
                    "[oracle:guess-permutation] metamorphic violation: guess() length changed under permutation lhs="
                        + Arrays.toString(sortedGuess) + " rhs=" + Arrays.toString(reversedGuess)
                );
            }

            for (int i = 0; i < sortedGuess.length; i++) {
                double a = sortedGuess[i];
                double b = reversedGuess[i];
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Double.isNaN(a) != Double.isNaN(b) || Math.abs(a - b) > 1.0e-7 * scale) {
                    throw new RuntimeException(
                        "[oracle:guess-permutation] metamorphic violation: guess() should be permutation-invariant because ParameterGuesser sorts observations with respect to abscissa before computing a and omega; input="
                            + Arrays.toString(harmonicPoints) + " lhs=" + Arrays.toString(sortedGuess)
                            + " rhs=" + Arrays.toString(reversedGuess)
                    );
                }
            }
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("[oracle:")) {
                throw ex;
            }
            return;
        }

        try {
            double[] initialGuess = new HarmonicFitter.ParameterGuesser(harmonicPoints).guess();

            HarmonicFitter fitterAuto = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            HarmonicFitter fitterExplicit = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < harmonicPoints.length; i++) {
                WeightedObservedPoint p = harmonicPoints[i];
                fitterAuto.addObservedPoint(p.getWeight(), p.getX(), p.getY());
                fitterExplicit.addObservedPoint(p.getWeight(), p.getX(), p.getY());
            }

            double[] autoFit = fitterAuto.fit();
            double[] explicitFit = fitterExplicit.fit(initialGuess);

            if (autoFit.length != explicitFit.length) {
                throw new RuntimeException(
                    "[oracle:fit-overloads] metamorphic violation: fit() and fit(initialGuess) returned different lengths input="
                        + Arrays.toString(initialGuess) + " lhs=" + Arrays.toString(autoFit)
                        + " rhs=" + Arrays.toString(explicitFit)
                );
            }

            for (int i = 0; i < autoFit.length; i++) {
                double a = autoFit[i];
                double b = explicitFit[i];
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Double.isNaN(a) != Double.isNaN(b) || Math.abs(a - b) > 1.0e-6 * scale) {
                    throw new RuntimeException(
                        "[oracle:fit-overloads] metamorphic violation: the public overloads are documented as equivalent when fit(initialGuess) is given the same first guess that fit() computes internally; input="
                            + Arrays.toString(initialGuess) + " lhs=" + Arrays.toString(autoFit)
                            + " rhs=" + Arrays.toString(explicitFit)
                    );
                }
            }
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("[oracle:")) {
                throw ex;
            }
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-repair · `attempt_004`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [55] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave…

---
## [56] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,949 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        weightedObservedPointGettersRoundtrip(data);
        exactMath844Oracle();
        generalizedTriangularWaveOracle(data);
        constructorCloneAndGuessReadOnlyOracle(data);
    }

    private static void weightedObservedPointGettersRoundtrip(FuzzedDataProvider data) {
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;

        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                            + "weight=" + p.getWeight() + "/" + w
                            + " x=" + p.getX() + "/" + x
                            + " y=" + p.getY() + "/" + y);
        }
    }

    private static void exactMath844Oracle() {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        try {
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but guess() completed normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Trusted oracle copied verbatim from HarmonicFitterTest.testMath844:
            // this exact sample must be rejected as ill-conditioned/non-harmonic.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but got "
                            + t.getClass().getName(), t);
        }
    }

    private static void generalizedTriangularWaveOracle(FuzzedDataProvider data) {
        final int scale = data.consumeInt(1, 5);
        final int step = data.consumeInt(1, 4);
        final int start = data.consumeInt(-20, 20);

        final int[] base = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };

        final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
        for (int i = 0; i < base.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
        }

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        try {
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but completed normally"
                            + " scale=" + scale + " step=" + step + " start=" + start);
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Equivalent-input oracle: the trusted failing test shows this triangular wave shape
            // is far from harmonic and must be rejected; translating x, rescaling x by a
            // positive step, and scaling y by a positive factor preserve that ill-conditioned
            // shape, so a correct implementation must still reject rather than synthesize coefficients.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but got "
                            + t.getClass().getName()
                            + " scale=" + scale + " step=" + step + " start=" + start, t);
        }
    }

    private static void constructorCloneAndGuessReadOnlyOracle(FuzzedDataProvider data) {
        final int n = data.consumeInt(8, 32);
        final int start = data.consumeInt(-20, 20);
        final int step = data.consumeInt(1, 3);
        final int amplitudeInt = data.consumeInt(1, 5);
        final double amplitude = (double) amplitudeInt;
        final int period = data.consumeInt(6, 18);
        final double omega = 2.0 * Math.PI / period;
        final int phiChoice = data.consumeInt(0, 3);
        final double phi;
        if (phiChoice == 0) {
            phi = 0.0;
        } else if (phiChoice == 1) {
            phi = Math.PI / 6.0;
        } else if (phiChoice == 2) {
            phi = Math.PI / 4.0;
        } else {
            phi = Math.PI / 3.0;
        }

        final WeightedObservedPoint[] points = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + (double) i * step;
            final double y = amplitude * Math.cos(omega * x + phi);
            points[i] = new WeightedObservedPoint(1.0, x, y);
        }

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        final double[] before;
        try {
            before = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        final int mutations = data.consumeInt(1, Math.max(1, n));
        for (int i = 0; i < mutations; i++) {
            int idx = data.consumeInt(0, n - 1);
            double mx = data.consumeInt(-50, 50);
            double my = data.consumeInt(-50, 50);
            double mw = data.consumeBoolean() ? 1.0 : 2.0;
            points[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        final double[] after;
        try {
            after = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        // Contract justification:
        // (1) ParameterGuesser(WeightedObservedPoint[] observations) stores observations.clone(),
        // so later mutations to the caller's array must not change the guesser's state.
        // (2) guess() is a read-only query over the stored observations; a throw-deleting or
        // otherwise state-corrupting patch in guessAOmega/guess() would make repeated reads disagree.
        if (!sameArray(before, after)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone-readonly] metamorphic violation: repeated guess() on unchanged receiver disagreed after caller-array mutation"
                            + " before=" + formatArray(before)
                            + " after=" + formatArray(after)
                            + " n=" + n
                            + " start=" + start
                            + " step=" + step
                            + " amplitude=" + amplitude
                            + " period=" + period
                            + " phi=" + phi
                            + " mutations=" + mutations);
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
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
                return false;
            }
        }
        return true;
    }

    private static String formatArray(double[] a) {
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
## [57] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but got com.code_intelligence.jazzer.api.FuzzerSecurityIssueLo…

---
## [58] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,327 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            final HarmonicFitter.ParameterGuesser guesser =
                    new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-exception] semantic mismatch: expected MathIllegalStateException for the exact testMath844 sample, but guess() returned normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Ground-truth lifted from HarmonicFitterTest.testMath844.
        } catch (Throwable other) {
            return;
        }

        final int n = data.consumeInt(4, 24);
        double amplitude = data.consumeInt(-500, 500) / 50.0;
        if (amplitude == 0.0) {
            amplitude = 1.0;
        }
        final double omega = data.consumeInt(1, 300) / 100.0;
        final double phase = data.consumeInt(-314, 314) / 100.0;
        final double start = data.consumeInt(-100, 100) / 10.0;
        final double step = data.consumeInt(1, 50) / 10.0;

        final org.apache.commons.math3.analysis.function.HarmonicOscillator oscillator =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(amplitude, omega, phase);

        final WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + i * step;
            harmonicPoints[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }

        final double[] initialGuess;
        try {
            initialGuess = new HarmonicFitter.ParameterGuesser(harmonicPoints).guess();
        } catch (Throwable t) {
            return;
        }

        final double[] autoFit;
        final double[] explicitFit;
        try {
            final HarmonicFitter fitterAuto =
                    new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            final HarmonicFitter fitterExplicit =
                    new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                fitterAuto.addObservedPoint(harmonicPoints[i]);
                fitterExplicit.addObservedPoint(harmonicPoints[i]);
            }

            // Contract: fit() "An initial guess will be automatically computed"
            // and refers to the other fit(double[]) method, so on the same observations
            // fit() must agree with fit(ParameterGuesser.guess()). A patch that merely
            // suppresses/avoids the bad-state detection in guessAOmega can make fit()
            // proceed with a wrong auto-generated guess and diverge from the explicit path.
            autoFit = fitterAuto.fit();
            explicitFit = fitterExplicit.fit(initialGuess);
        } catch (Throwable t) {
            return;
        }

        if (autoFit == null || explicitFit == null || autoFit.length != 3 || explicitFit.length != 3) {
            return;
        }

        final org.apache.commons.math3.analysis.function.HarmonicOscillator autoOsc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(autoFit[0], autoFit[1], autoFit[2]);
        final org.apache.commons.math3.analysis.function.HarmonicOscillator explicitOsc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(explicitFit[0], explicitFit[1], explicitFit[2]);

        double maxDiff = 0.0;
        for (int i = 0; i < n; i++) {
            final double x = harmonicPoints[i].getX();
            final double diff = Math.abs(autoOsc.value(x) - explicitOsc.value(x));
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }

        if (maxDiff > 1.0e-6) {
            throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() disagrees with fit(ParameterGuesser.guess()) on the same observations input="
                            + "n=" + n
                            + ",amplitude=" + amplitude
                            + ",omega=" + omega
                            + ",phase=" + phase
                            + ",start=" + start
                            + ",step=" + step
                            + " lhs=" + autoFit[0] + "," + autoFit[1] + "," + autoFit[2]
                            + " rhs=" + explicitFit[0] + "," + explicitFit[1] + "," + explicitFit[2]
                            + " maxPredictionDiff=" + maxDiff);
        }
    }
}
```

</details>

---
## [59] ⚙️ harness-repair · `attempt_006`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [60] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-exception] semantic mismatch: expected MathIllegalStateException for the exact testMath844 sample, but guess() returned normall…

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,084 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        weightedObservedPointRoundTrip(data);
        exactMath844Oracle();
        generalizedTriangularWaveOracle(data);
        harmonicKnownAnswerOracle(data);
        harmonicFitterPublicApiOracle();
    }

    private static void weightedObservedPointRoundTrip(FuzzedDataProvider data) {
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean violated =
                Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);
        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                    "weight=" + p.getWeight() + "/" + w +
                    " x=" + p.getX() + "/" + x +
                    " y=" + p.getY() + "/" + y);
        }
    }

    private static void exactMath844Oracle() {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        try {
            guesser.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but got " +
                    t.getClass().getName(), t);
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but guess() completed normally");
        }
    }

    private static void generalizedTriangularWaveOracle(FuzzedDataProvider data) {
        final int[] base = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        final int scale = data.consumeInt(1, 5);
        final int step = data.consumeInt(1, 4);
        final int start = data.consumeInt(-20, 20);

        WeightedObservedPoint[] points;
        HarmonicFitter.ParameterGuesser guesser;
        try {
            points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
            }
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        String detail = "completed normally";
        Throwable wrong = null;
        try {
            guesser.guess();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            violated = true;
            detail = "wrong exception class " + t.getClass().getName();
            wrong = t;
        }

        if (violated) {
            if (wrong != null) {
                throw new FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " +
                        detail + " start=" + start + " step=" + step + " scale=" + scale, wrong);
            }
            throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " +
                    detail + " start=" + start + " step=" + step + " scale=" + scale);
        }
    }

    private static void harmonicKnownAnswerOracle(FuzzedDataProvider data) {
        final double amplitude = data.consumeInt(1, 5);
        final double omega = data.consumeInt(1, 10) / 10.0;
        final double phi = 0.0;
        final int n = data.consumeInt(12, 40);
        final double step = data.consumeInt(1, 5) / 10.0;
        final int start = data.consumeInt(-20, 20);

        final HarmonicOscillator osc;
        final WeightedObservedPoint[] points;
        final HarmonicFitter.ParameterGuesser guesser;
        final double[] first;
        final double[] second;
        try {
            osc = new HarmonicOscillator(amplitude, omega, phi);
            points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
            }
            guesser = new HarmonicFitter.ParameterGuesser(points);
            first = guesser.guess();
            second = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        // Contract justification: guess() estimates coefficients from the fixed observation sample.
        // For a sample constructed exactly from a harmonic oscillator with known parameters, a correct
        // implementation must recover those established amplitude/frequency values (oracle from input itself).
        if (!isFinite(first[0]) || !isFinite(first[1]) ||
            !close(first[0], amplitude, 1e-2) || !close(first[1], omega, 1e-2)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:known-harmonic] semantic mismatch: expected amplitude=" + amplitude +
                    " omega=" + omega + " but got amplitude=" + first[0] + " omega=" + first[1]);
        }

        // Contract justification: repeated guess() on the same immutable logical sample must agree.
        // The method may sort internally, but that normalization cannot change the coefficients it reports.
        if (!sameTriple(first, second, 1e-12)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:guess-idempotence] metamorphic violation: repeated guess() disagreed " +
                    "first=[" + first[0] + "," + first[1] + "," + first[2] + "]" +
                    " second=[" + second[0] + "," + second[1] + "," + second[2] + "]");
        }
    }

    private static void harmonicFitterPublicApiOracle() {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        final HarmonicFitter fitter;
        try {
            fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < y.length; i++) {
                fitter.addObservedPoint(1.0, i, y[i]);
            }
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        try {
            fitter.fit();
            violated = true;
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            return;
        }

        if (violated) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:public-fit-path] semantic mismatch: HarmonicFitter.fit() should propagate the MathIllegalStateException from parameter guessing on the Math844 sample");
        }
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static boolean close(double a, double b, double tol) {
        return isFinite(a) && isFinite(b) && Math.abs(a - b) <= tol;
    }

    private static boolean sameTriple(double[] a, double[] b, double tol) {
        return a != null && b != null && a.length == 3 && b.length == 3 &&
               close(a[0], b[0], tol) &&
               close(a[1], b[1], tol) &&
               close(a[2], b[2], tol);
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException but guess() completed normally

---
## [63] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,387 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int shift = data.consumeInt(-1000, 1000);

        final WeightedObservedPoint[] exact = triangularPoints(0.0);
        try {
            final HarmonicFitter.ParameterGuesser guesser =
                new HarmonicFitter.ParameterGuesser(exact);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular sample from HarmonicFitterTest.testMath844");
        } catch (MathIllegalStateException expected) {
            // Ground-truth oracle lifted verbatim from the failing test.
        }

        try {
            final HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < exact.length; i++) {
                fitter.addObservedPoint(exact[i].getWeight(), exact[i].getX(), exact[i].getY());
            }
            fitter.fit();
            throw new FuzzerSecurityIssueLow(
                "[oracle:public-fit] semantic mismatch: expected rejection to remain reachable through HarmonicFitter.fit() for the same triangular sample");
        } catch (MathIllegalStateException expected) {
            // Real public API path: fit() computes its initial guess through ParameterGuesser.
        } catch (Throwable ignored) {
            // If some other rejection happens, this semantic check does not apply.
        }

        final WeightedObservedPoint[] shifted = triangularPoints((double) shift);
        try {
            final HarmonicFitter.ParameterGuesser shiftedGuesser =
                new HarmonicFitter.ParameterGuesser(shifted);
            shiftedGuesser.guess();
            throw new RuntimeException(
                "[oracle:x-shift-rejection] metamorphic violation: translating all abscissae must preserve this rejection because guessAOmega uses only relative x (currentX - startX) and xRange; inputShift="
                    + shift + " relation=reject(base)==reject(shifted) lhs=MathIllegalStateException rhs=returned");
        } catch (MathIllegalStateException expected) {
            // Trusted equivalent-input oracle: same y sequence, same spacing, only translated x.
        } catch (Throwable ignored) {
            // Any other exception means this relation is not applicable for this run.
        }

        final WeightedObservedPoint[] mutatedArray = triangularPoints(0.0);
        final HarmonicFitter.ParameterGuesser clonedGuesser =
            new HarmonicFitter.ParameterGuesser(mutatedArray);
        for (int i = 0; i < mutatedArray.length; i++) {
            mutatedArray[i] = new WeightedObservedPoint(1.0, shift + i, 123.0 + i);
        }
        try {
            clonedGuesser.guess();
            throw new RuntimeException(
                "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents it clones the observations array, so replacing caller array slots after construction must not change the outcome; expected=MathIllegalStateException actual=returned");
        } catch (MathIllegalStateException expected) {
            // Trusted post-condition on shared state: guess() must observe the constructor-established clone.
        } catch (Throwable ignored) {
            // Any other exception means this relation is not applicable for this run.
        }
    }

    private static WeightedObservedPoint[] triangularPoints(double xOffset) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, xOffset + i, y[i]);
        }
        return points;
    }
}
```

</details>

---
## [64] ⚙️ harness-repair · `attempt_008`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [65] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular sampl…

---
## [66] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,694 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static WeightedObservedPoint[] exactMath844Points() {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        return points;
    }

    private static WeightedObservedPoint[] generalizedTriangularPoints(FuzzedDataProvider data) {
        final int[] base = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int scale = data.consumeInt(1, 5);
        final int step = data.consumeInt(1, 4);
        final int start = data.consumeInt(-20, 20);
        final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
        for (int i = 0; i < base.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
        }
        return points;
    }

    private static WeightedObservedPoint[] exactHarmonicPoints(FuzzedDataProvider data, double[] paramsOut) {
        final double a = data.consumeInt(1, 5);
        final double omega = data.consumeInt(1, 4) / 3.0;
        final double phi = data.consumeInt(-20, 20) / 10.0;
        final int n = data.consumeInt(8, 16);
        final int step = data.consumeInt(1, 3);
        final int start = data.consumeInt(-10, 10);

        paramsOut[0] = a;
        paramsOut[1] = omega;
        paramsOut[2] = phi;

        final org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(a, omega, phi);
        final WeightedObservedPoint[] points = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + (double) i * step;
            points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }
        return points;
    }

    private static double sse(double[] params, WeightedObservedPoint[] points) {
        final org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(params[0], params[1], params[2]);
        double sum = 0.0;
        for (WeightedObservedPoint p : points) {
            final double d = osc.value(p.getX()) - p.getY();
            sum += d * d;
        }
        return sum;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w;
            double x;
            double y;
            WeightedObservedPoint p;
            try {
                w = data.consumeInt(-1000, 1000) / 10.0;
                x = data.consumeInt(-1000, 1000) / 10.0;
                y = data.consumeInt(-1000, 1000) / 10.0;
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                p = null;
                w = x = y = 0.0;
            }
            if (p != null) {
                if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                    Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                    Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                            "weight=" + p.getWeight() + "/" + w +
                            " x=" + p.getX() + "/" + x +
                            " y=" + p.getY() + "/" + y);
                }
            }
        }

        {
            Throwable wrong = null;
            boolean completedNormally = false;
            try {
                final HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(exactMath844Points());
                guesser.guess();
                completedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                wrong = t;
            }
            if (completedNormally) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, but guess() completed normally");
            }
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, but got " +
                        wrong.getClass().getName(), wrong);
            }
        }

        {
            Throwable wrong = null;
            boolean completedNormally = false;
            try {
                final WeightedObservedPoint[] points = generalizedTriangularPoints(data);
                final HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                completedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                wrong = t;
            }
            if (completedNormally) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but completed normally");
            }
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but got " +
                        wrong.getClass().getName(), wrong);
            }
        }

        {
            Throwable wrong = null;
            boolean completedNormally = false;
            try {
                final HarmonicFitter fitter =
                        new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                for (WeightedObservedPoint p : exactMath844Points()) {
                    fitter.addObservedPoint(p);
                }
                // Contract we assert: no-arg fit() computes its starting point via the real HarmonicFitter
                // guesser path. For the trusted MATH-844 triangular sample that path must reject with
                // MathIllegalStateException; a patch that merely deletes the throw would let fit() continue.
                fitter.fit();
                completedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                wrong = t;
            }
            if (completedNormally) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-propagates-guess-failure] semantic mismatch: expected MathIllegalStateException from no-arg fit(), but it completed normally");
            }
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-propagates-guess-failure] semantic mismatch: expected MathIllegalStateException from no-arg fit(), but got " +
                        wrong.getClass().getName(), wrong);
            }
        }

        {
            final double[] knownParams = new double[3];
            final WeightedObservedPoint[] points;
            try {
                points = exactHarmonicPoints(data, knownParams);
            } catch (Throwable t) {
                return;
            }

            double[] fitAuto;
            double[] fitSeeded;
            try {
                final HarmonicFitter fitterAuto =
                        new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                final HarmonicFitter fitterSeeded =
                        new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                for (WeightedObservedPoint p : points) {
                    fitterAuto.addObservedPoint(p);
                    fitterSeeded.addObservedPoint(p);
                }

                // Documented sibling-agreement check: fit() and fit(double[] initialGuess) are same-name
                // overloads documented to do the same fitting job on the same observations. On a noiseless
                // harmonic sample constructed from a known answer, both real-library calls should fit the
                // same curve; compare observable residuals rather than raw parameters because harmonic
                // parameters have equivalent representations (phase shifts/sign changes).
                fitAuto = fitterAuto.fit();
                fitSeeded = fitterSeeded.fit(new double[] { knownParams[0], knownParams[1], knownParams[2] });
            } catch (Throwable t) {
                return;
            }

            final double autoSse;
            final double seededSse;
            try {
                autoSse = sse(fitAuto, points);
                seededSse = sse(fitSeeded, points);
            } catch (Throwable t) {
                return;
            }

            if (!(Double.isFinite(autoSse) && Double.isFinite(seededSse))) {
                return;
            }

            final double tolerance = 1.0e-6;
            if (autoSse > tolerance || seededSse > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-overloads-agree-on-noiseless-harmonic] metamorphic violation: " +
                        "both overloads should fit the exact harmonic sample with near-zero residuals " +
                        "autoSse=" + autoSse + " seededSse=" + seededSse +
                        " a=" + knownParams[0] + " omega=" + knownParams[1] + " phi=" + knownParams[2] +
                        " n=" + points.length);
            }
        }
    }
}
```

</details>

---
## [67] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, but guess() completed normally

---
## [68] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,800 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        boolean sawExpected = false;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            double[] guess = guesser.guess();
            throw new FuzzerSecurityIssueLow("[oracle:math844-parameter-guesser] semantic mismatch: expected MathIllegalStateException but guess() returned amplitude=" + guess[0] + " omega=" + guess[1] + " phase=" + guess[2]);
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (RuntimeException other) {
            // Other rejections are not a trusted oracle for this input.
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            double[] fit = fitter.fit();
            throw new FuzzerSecurityIssueLow("[oracle:math844-fit-noarg] semantic mismatch: expected MathIllegalStateException but fit() returned amplitude=" + fit[0] + " omega=" + fit[1] + " phase=" + fit[2]);
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (RuntimeException other) {
            // Skip untrusted outcomes.
        }

        int n = data.consumeInt(4, 12);
        double amplitude = data.consumeInt(-20, 20);
        if (amplitude == 0.0) {
            amplitude = 1.0;
        }
        double omega = data.consumeInt(1, 20) / 5.0;
        double phase = data.consumeInt(-20, 20) / 7.0;
        double step = data.consumeInt(1, 5) / 4.0;
        double start = data.consumeInt(-10, 10);

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            harmonicPoints[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        try {
            HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(harmonicPoints);
            double[] before = g1.guess();

            // Constructor body shown in the prompt clones the observations array.
            // Therefore later mutations of the caller-owned array must not affect
            // what guess() reads from the already-constructed guesser.
            int slot = data.consumeInt(0, n - 1);
            harmonicPoints[slot] = new WeightedObservedPoint(1.0, start + 123.0, amplitude * 17.0);

            double[] after = g1.guess();
            if (!sameTriple(before, after)) {
                throw new RuntimeException("[oracle:constructor-clone] metamorphic violation: ParameterGuesser must be insulated from later caller array mutations because constructor clones observations input; slot=" + slot + " before=" + tripleToString(before) + " after=" + tripleToString(after));
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }

        try {
            HarmonicFitter.ParameterGuesser seedGuesser = new HarmonicFitter.ParameterGuesser(harmonicPoints.clone());
            double[] guessed = seedGuesser.guess();

            HarmonicFitter fitterAuto = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            HarmonicFitter fitterManual = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                fitterAuto.addObservedPoint(harmonicPoints[i]);
                fitterManual.addObservedPoint(harmonicPoints[i]);
            }

            // The no-arg overload is documented as the same fit operation with an
            // automatically computed initial guess. Supplying exactly that guessed
            // array to the other overload must therefore yield the same result on a
            // fresh fitter with the same observations.
            double[] auto = fitterAuto.fit();
            double[] manual = fitterManual.fit(guessed);
            if (!sameTriple(auto, manual)) {
                throw new RuntimeException("[oracle:fit-overload-equivalence] metamorphic violation: fit() must agree with fit(ParameterGuesser(points).guess()) on identical observations input=" + pointsToString(harmonicPoints) + " lhs=" + tripleToString(auto) + " rhs=" + tripleToString(manual));
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }

        if (!sawExpected && data.consumeBoolean()) {
            // Keep exercising the exact regression input even when only the public API path ran.
        }
    }

    private static boolean sameTriple(double[] a, double[] b) {
        if (a == null || b == null || a.length != 3 || b.length != 3) {
            return false;
        }
        return sameDouble(a[0], b[0]) && sameDouble(a[1], b[1]) && sameDouble(a[2], b[2]);
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private static String tripleToString(double[] v) {
        if (v == null) {
            return "null";
        }
        return "[" + v[0] + "," + v[1] + "," + v[2] + "]";
    }

    private static String pointsToString(WeightedObservedPoint[] pts) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < pts.length; i++) {
            if (i != 0) {
                sb.append(";");
            }
            sb.append("(").append(pts[i].getWeight()).append(",").append(pts[i].getX()).append(",").append(pts[i].getY()).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}
```

</details>

---
## [69] ⚙️ harness-repair · `attempt_010`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [70] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-parameter-guesser] semantic mismatch: expected MathIllegalStateException but guess() returned amplitude=Infinity omega=0.0 phas…

---
## [71] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~13,541 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TWO_PI = 2.0 * Math.PI;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Invariant: WeightedObservedPoint is an immutable value holder whose getters
        // must return the constructor arguments.
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "weight=" + p.getWeight() + "/" + w
                                + " x=" + p.getX() + "/" + x
                                + " y=" + p.getY() + "/" + y);
            }
        }

        // Lifted oracle from HarmonicFitterTest.testMath844: this exact sample must
        // throw MathIllegalStateException from ParameterGuesser.guess().
        {
            final double[] y = {
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1, 0
            };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
            for (int i = 0; i < y.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, y[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            Throwable cause = null;
            try {
                guesser.guess();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
                cause = t;
            }
            if (violated) {
                if (cause != null) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail,
                            cause);
                }
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail);
            }
        }

        // Generalized trusted oracle: same non-harmonic triangular-wave shape as the
        // failing test, with fuzzed positive scale, start, and step. The test's reason
        // still applies: this sample is far from harmonic, so guess() must reject with
        // MathIllegalStateException rather than silently returning coefficients.
        {
            final int[] base = {
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1, 0
            };
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            Throwable cause = null;
            try {
                guesser.guess();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
                cause = t;
            }
            if (violated) {
                if (cause != null) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:triangular-wave] semantic mismatch: expected MathIllegalStateException, but " + detail,
                            cause);
                }
                throw new FuzzerSecurityIssueLow(
                        "[oracle:triangular-wave] semantic mismatch: expected MathIllegalStateException, but " + detail);
            }

            // Drive the same state through the real public API entry point: HarmonicFitter.fit()
            // without an explicit initial guess internally uses ParameterGuesser.guess().
            HarmonicFitter fitter;
            try {
                fitter = new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                int mutations = data.consumeInt(1, 3);
                for (int m = 0; m < mutations; m++) {
                    int idx = data.consumeInt(0, points.length - 1);
                    fitter.addObservedPoint(points[idx]);
                }
                for (WeightedObservedPoint p : points) {
                    fitter.addObservedPoint(p);
                }
            } catch (Throwable t) {
                return;
            }

            violated = false;
            detail = "completed normally";
            cause = null;
            try {
                fitter.fit();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
                cause = t;
            }
            if (violated) {
                if (cause != null) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:harmonic-fitter-fit] semantic mismatch: expected MathIllegalStateException, but " + detail,
                            cause);
                }
                throw new FuzzerSecurityIssueLow(
                        "[oracle:harmonic-fitter-fit] semantic mismatch: expected MathIllegalStateException, but " + detail);
            }
        }

        // Hidden-state / state-coupling check:
        // ParameterGuesser(WeightedObservedPoint[] observations) clones the observations array.
        // Therefore, replacing entries in the caller's array after construction must not change
        // what the later reader guess() reports. This directly checks that the constructor's
        // established state and the guess() reader agree on the same internal observations.
        {
            int n = data.consumeInt(8, 20);
            int period = data.consumeInt(6, 20);
            double omega = TWO_PI / period;
            double amplitude = data.consumeInt(1, 5);
            double phi = data.consumeInt(-30, 30) / 10.0;
            int start = data.consumeInt(-10, 10);
            int step = data.consumeInt(1, 3);
            WeightedObservedPoint[] original = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + (double) i * step;
                double y = amplitude * Math.cos(omega * x + phi);
                original[i] = new WeightedObservedPoint(1.0, x, y);
            }
            WeightedObservedPoint[] snapshot = original.clone();

            HarmonicFitter.ParameterGuesser constructedBeforeMutation;
            HarmonicFitter.ParameterGuesser expectedFromSnapshot;
            try {
                constructedBeforeMutation = new HarmonicFitter.ParameterGuesser(original);
                expectedFromSnapshot = new HarmonicFitter.ParameterGuesser(snapshot);
            } catch (Throwable t) {
                return;
            }

            int mutations = data.consumeInt(1, Math.max(1, n));
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, n - 1);
                double mx = data.consumeInt(-50, 50);
                double my = data.consumeInt(-50, 50);
                original[idx] = new WeightedObservedPoint(1.0, mx, my);
            }

            double[] lhs;
            double[] rhs;
            try {
                lhs = constructedBeforeMutation.guess();
                rhs = expectedFromSnapshot.guess();
            } catch (Throwable t) {
                return;
            }

            if (!sameTriplet(lhs, rhs, 1e-8, 1e-6)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone] metamorphic violation: guess() changed after caller mutated original observations array "
                                + "lhs=" + formatArray(lhs) + " rhs=" + formatArray(rhs));
            }
        }

        // Metamorphic relation over real library calls:
        // guess() returns [Amplitude, Angular frequency, Phase]. Shifting every x by the
        // same constant changes only the phase of a harmonic function, not its amplitude
        // or angular frequency. A correct implementation of the same estimation procedure
        // must therefore report the same a and omega on these two equivalent inputs.
        {
            int n = data.consumeInt(8, 24);
            int period = data.consumeInt(6, 18);
            double omega = TWO_PI / period;
            double amplitude = data.consumeInt(1, 5);
            double phi = data.consumeInt(-25, 25) / 10.0;
            int start = data.consumeInt(-10, 10);
            int shift = data.consumeInt(-10, 10);
            int step = data.consumeInt(1, 3);

            WeightedObservedPoint[] p1 = new WeightedObservedPoint[n];
            WeightedObservedPoint[] p2 = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x1 = start + (double) i * step;
                double x2 = x1 + shift;
                double y1 = amplitude * Math.cos(omega * x1 + phi);
                double y2 = amplitude * Math.cos(omega * x2 + (phi - omega * shift));
                p1[i] = new WeightedObservedPoint(1.0, x1, y1);
                p2[i] = new WeightedObservedPoint(1.0, x2, y2);
            }

            double[] g1;
            double[] g2;
            try {
                g1 = new HarmonicFitter.ParameterGuesser(p1).guess();
                g2 = new HarmonicFitter.ParameterGuesser(p2).guess();
            } catch (Throwable t) {
                return;
            }

            if (!finite(g1) || !finite(g2)) {
                return;
            }

            if (!close(g1[0], g2[0], 1e-8, 1e-5) || !close(g1[1], g2[1], 1e-8, 1e-5)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:x-shift-a-omega] metamorphic violation: shifting x changed guessed amplitude/frequency "
                                + "g1=" + formatArray(g1) + " g2=" + formatArray(g2));
            }
        }
    }

    private static boolean finite(double[] v) {
        if (v == null) {
            return false;
        }
        for (double d : v) {
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return false;
            }
        }
        return true;
    }

    private static boolean close(double a, double b, double abs, double rel) {
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= abs || diff <= rel * Math.max(1.0, scale);
    }

    private static boolean sameTriplet(double[] a, double[] b, double abs, double rel) {
        return a != null && b != null
                && a.length == 3 && b.length == 3
                && close(a[0], b[0], abs, rel)
                && close(a[1], b[1], abs, rel)
                && closeAngle(a[2], b[2], 1e-5);
    }

    private static boolean closeAngle(double a, double b, double tol) {
        double d = a - b;
        while (d <= -Math.PI) {
            d += TWO_PI;
        }
        while (d > Math.PI) {
            d -= TWO_PI;
        }
        return Math.abs(d) <= tol;
    }

    private static String formatArray(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
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
## [72] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but completed normally

---
## [73] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,552 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        fixedMath844Oracle();
        permutationInvarianceOracle(data);
    }

    private static void fixedMath844Oracle() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave sample, but call returned normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Exact lifted oracle from HarmonicFitterTest.testMath844.
        } catch (RuntimeException other) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave sample, but got " +
                other.getClass().getName() + ": " + other.getMessage());
        }
    }

    private static void permutationInvarianceOracle(FuzzedDataProvider data) {
        final int n = data.consumeInt(4, 16);
        final double amplitude = 0.25 + data.consumeInt(0, 400) / 20.0;
        final int period = data.consumeInt(4, 24);
        final double omega = (2.0 * Math.PI) / period;
        final double phase = data.consumeInt(-314, 314) / 100.0;

        final org.apache.commons.math3.analysis.function.HarmonicOscillator oscillator =
            new org.apache.commons.math3.analysis.function.HarmonicOscillator(amplitude, omega, phase);

        final WeightedObservedPoint[] sorted = new WeightedObservedPoint[n];
        double x = 0.0;
        for (int i = 0; i < n; i++) {
            x += data.consumeInt(1, 3);
            sorted[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }

        final WeightedObservedPoint[] permuted = sorted.clone();
        for (int i = permuted.length - 1; i > 0; i--) {
            final int j = data.consumeInt(0, i);
            final WeightedObservedPoint tmp = permuted[i];
            permuted[i] = permuted[j];
            permuted[j] = tmp;
        }

        final double[] guessSorted;
        final double[] guessPermuted;
        try {
            guessSorted = new HarmonicFitter.ParameterGuesser(sorted).guess();
            guessPermuted = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guessSorted == null || guessPermuted == null || guessSorted.length != 3 || guessPermuted.length != 3) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (!sameDouble(guessSorted[i], guessPermuted[i])) {
                // Contract justification: ParameterGuesser works from "sampled observations",
                // and the class contains a dedicated sortObservations() because the algorithm
                // assumes observations are sorted by abscissa. Therefore two inputs containing
                // the same observation set in different orders must yield the same guessed
                // parameters after internal sorting. A patch that merely deletes the
                // MathIllegalStateException or otherwise perturbs shared state can break this
                // observable agreement without throwing.
                throw new RuntimeException(
                    "[oracle:permutation-invariance] metamorphic violation: guess() must be invariant under permutation of the same observation set inputIndex=" +
                    i + " lhs=" + guessSorted[i] + " rhs=" + guessPermuted[i]);
            }
        }
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }
}
```

</details>

---
## [74] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave…

---
## [75] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,290 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "w=" + w + " gotW=" + p.getWeight()
                                + " x=" + x + " gotX=" + p.getX()
                                + " y=" + y + " gotY=" + p.getY());
            }
        }

        {
            final double[] y = { 0, 1, 2, 3, 2, 1,
                                 0, -1, -2, -3, -2, -1,
                                 0, 1, 2, 3, 2, 1,
                                 0, -1, -2, -3, -2, -1,
                                 0, 1, 2, 3, 2, 1, 0 };
            final int len = y.length;
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            for (int i = 0; i < len; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, y[i]);
            }

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < len; i++) {
                    fitter.addObservedPoint(points[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:harmonicfitter-fit-seed] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit(), but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }
        }

        {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            int mutateIndex = data.consumeInt(0, 30);
            int mutateDeltaX = data.consumeInt(1, 7);
            int mutateDeltaY = data.consumeInt(-9, 9);

            final int[] base = new int[] { 0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1, 0 };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            final WeightedObservedPoint[] snapshot = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                WeightedObservedPoint p =
                        new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
                points[i] = p;
                snapshot[i] = p;
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            points[mutateIndex] = new WeightedObservedPoint(
                    1.0,
                    points[mutateIndex].getX() + mutateDeltaX,
                    points[mutateIndex].getY() + mutateDeltaY);

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-guesser] semantic mismatch: expected MathIllegalStateException on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < snapshot.length; i++) {
                    fitter.addObservedPoint(snapshot[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-fit] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            boolean leftThrows = false;
            boolean rightThrows = false;
            Throwable sideEx = null;
            try {
                HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(snapshot);
                g1.guess();
            } catch (MathIllegalStateException expected) {
                leftThrows = true;
            } catch (Throwable t) {
                return;
            }
            try {
                guesser.guess();
            } catch (MathIllegalStateException expected) {
                rightThrows = true;
            } catch (Throwable t) {
                sideEx = t;
                return;
            }

            /* Constructor contract: ParameterGuesser(WeightedObservedPoint[] observations) clones the array.
               Therefore mutating the caller's array after construction must not change what the existing guesser
               observes. A patch that simply deletes the throw or otherwise corrupts the shared a/omega state can
               break this agreement even when no exception is raised. */
            if (leftThrows != rightThrows) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone-isolation] metamorphic violation: mutating caller array after ParameterGuesser construction changed rejection behavior"
                                + " scale=" + scale + " step=" + step + " start=" + start
                                + " mutateIndex=" + mutateIndex + " mutateDeltaX=" + mutateDeltaX + " mutateDeltaY=" + mutateDeltaY
                                + " lhsThrows=" + leftThrows + " rhsThrows=" + rightThrows,
                        sideEx);
            }
        }
    }
}
```

</details>

---
## [76] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `leftThrows` (literal assignment, no rethrow, the caught exc

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,445 chars, 2 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "w=" + w + " gotW=" + p.getWeight()
                                + " x=" + x + " gotX=" + p.getX()
                                + " y=" + y + " gotY=" + p.getY());
            }
        }

        {
            final double[] y = { 0, 1, 2, 3, 2, 1,
                                 0, -1, -2, -3, -2, -1,
                                 0, 1, 2, 3, 2, 1,
                                 0, -1, -2, -3, -2, -1,
                                 0, 1, 2, 3, 2, 1, 0 };
            final int len = y.length;
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            for (int i = 0; i < len; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, y[i]);
            }

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < len; i++) {
                    fitter.addObservedPoint(points[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:harmonicfitter-fit-seed] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit(), but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }
        }

        {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            int mutateIndex = data.consumeInt(0, 30);
            int mutateDeltaX = data.consumeInt(1, 7);
            int mutateDeltaY = data.consumeInt(-9, 9);

            final int[] base = new int[] { 0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1, 0 };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            final WeightedObservedPoint[] snapshot = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                WeightedObservedPoint p =
                        new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
                points[i] = p;
                snapshot[i] = p;
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            points[mutateIndex] = new WeightedObservedPoint(
                    1.0,
                    points[mutateIndex].getX() + mutateDeltaX,
                    points[mutateIndex].getY() + mutateDeltaY);

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-guesser] semantic mismatch: expected MathIllegalStateException on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < snapshot.length; i++) {
                    fitter.addObservedPoint(snapshot[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-fit] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            boolean leftThrows = false;
            boolean rightThrows = false;
            Throwable sideEx = null;
            try {
                HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(snapshot);
                g1.guess();
            } catch (MathIllegalStateException expected) {
                leftThrows = true;
            } catch (Throwable t) {
                return;
            }
            try {
                guesser.guess();
            } catch (MathIllegalStateException expected) {
                rightThrows = true;
            } catch (Throwable t) {
                sideEx = t;
                return;
            }

            /* Constructor contract: ParameterGuesser(WeightedObservedPoint[] observations) clones the array.
               Therefore mutating the caller's array after construction must not change what the existing guesser
               observes. A patch that simply deletes the throw or otherwise corrupts the shared a/omega state can
               break this agreement even when no exception is raised. */
            if (leftThrows != rightThrows) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone-isolation] metamorphic violation: mutating caller array after ParameterGuesser construction changed rejection behavior"
                                + " scale=" + scale + " step=" + step + " start=" + start
                                + " mutateIndex=" + mutateIndex + " mutateDeltaX=" + mutateDeltaX + " mutateDeltaY=" + mutateDeltaY
                                + " lhsThrows=" + leftThrows + " rhsThrows=" + rightThrows,
                        sideEx);
            }
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `leftThrows` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 191 is conditioned on `leftThrows` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~12,199 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "w=" + w + " gotW=" + p.getWeight()
                                + " x=" + x + " gotX=" + p.getX()
                                + " y=" + y + " gotY=" + p.getY());
            }
        }

        {
            final double[] y = { 0, 1, 2, 3, 2, 1,
                                 0, -1, -2, -3, -2, -1,
                                 0, 1, 2, 3, 2, 1,
                                 0, -1, -2, -3, -2, -1,
                                 0, 1, 2, 3, 2, 1, 0 };
            final int len = y.length;
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            for (int i = 0; i < len; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, y[i]);
            }

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < len; i++) {
                    fitter.addObservedPoint(points[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:harmonicfitter-fit-seed] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit(), but " + detail;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }
        }

        {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);

            final int[] base = new int[] { 0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1,
                                           0, -1, -2, -3, -2, -1,
                                           0, 1, 2, 3, 2, 1, 0 };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
            }

            boolean violated = false;
            Throwable wrong = null;
            String detail = "completed normally";
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-guesser] semantic mismatch: expected MathIllegalStateException on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }

            violated = false;
            wrong = null;
            detail = "completed normally";
            try {
                HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < points.length; i++) {
                    fitter.addObservedPoint(points[i]);
                }
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                String msg = "[oracle:triangular-family-fit] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on valid triangular-wave family input, but " + detail
                        + " scale=" + scale + " step=" + step + " start=" + start;
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(msg, wrong);
                }
                throw new FuzzerSecurityIssueLow(msg);
            }
        }

        {
            int amplitudeInt = data.consumeInt(1, 8);
            int omegaInt = data.consumeInt(1, 5);
            int phaseInt = data.consumeInt(-3, 3);
            int sampleCount = data.consumeInt(8, 20);

            double amplitude = amplitudeInt;
            double omega = omegaInt / 5.0;
            double phase = phaseInt / 4.0;

            WeightedObservedPoint[] points = new WeightedObservedPoint[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                double x = i;
                double y = amplitude * Math.cos(omega * x + phase);
                points[i] = new WeightedObservedPoint(1.0, x, y);
            }

            double[] guessed;
            double[] guessedAgain;
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
                guessed = guesser.guess();
                guessedAgain = guesser.guess();
            } catch (Throwable t) {
                return;
            }

            if (guessed == null || guessedAgain == null || guessed.length != 3 || guessedAgain.length != 3) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:guess-shape] semantic mismatch: guess() must return 3 coefficients, got "
                                + (guessed == null ? "null" : guessed.length) + " and "
                                + (guessedAgain == null ? "null" : guessedAgain.length));
            }

            /* Documented contract: ParameterGuesser.guess() estimates coefficients from the stored observations.
               Constructor clones the observations array, and guess() is a reader of that fixed sample.
               Therefore repeated guess() calls on the same object and unchanged state must agree; a throw-deleting
               or state-corrupting patch in guessAOmega can silently change shared fields a/omega between reads. */
            for (int i = 0; i < 3; i++) {
                long a = Double.doubleToLongBits(guessed[i]);
                long b = Double.doubleToLongBits(guessedAgain[i]);
                if (a != b) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:guess-idempotence] metamorphic violation: repeated guess() on identical state disagreed"
                                    + " idx=" + i
                                    + " first=" + guessed[i]
                                    + " second=" + guessedAgain[i]
                                    + " amplitude=" + amplitude
                                    + " omega=" + omega
                                    + " phase=" + phase
                                    + " sampleCount=" + sampleCount);
                }
            }

            double[] fitAuto;
            double[] fitManual;
            try {
                HarmonicFitter fitterAuto = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                HarmonicFitter fitterManual = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < points.length; i++) {
                    fitterAuto.addObservedPoint(points[i]);
                    fitterManual.addObservedPoint(points[i]);
                }
                fitAuto = fitterAuto.fit();
                fitManual = fitterManual.fit(guessed.clone());
            } catch (Throwable t) {
                return;
            }

            if (fitAuto == null || fitManual == null || fitAuto.length != 3 || fitManual.length != 3) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:fit-shape] semantic mismatch: fit overloads must return 3 coefficients, got "
                                + (fitAuto == null ? "null" : fitAuto.length) + " and "
                                + (fitManual == null ? "null" : fitManual.length));
            }

            /* Sibling-agreement check: fit() and fit(double[] initialGuess) are same-name overloads documented to do
               the same fitting job, differing only in whether the initial guess is supplied. Using the library's own
               ParameterGuesser output as the explicit initialGuess reproduces fit()'s public setup, so both overloads
               should agree on the same logical input. */
            for (int i = 0; i < 3; i++) {
                double lhs = fitAuto[i];
                double rhs = fitManual[i];
                if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > 1.0e-6) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagreed"
                                    + " idx=" + i
                                    + " lhs=" + lhs
                                    + " rhs=" + rhs
                                    + " guess0=" + guessed[0]
                                    + " guess1=" + guessed[1]
                                    + " guess2=" + guessed[2]
                                    + " amplitude=" + amplitude
                                    + " omega=" + omega
                                    + " phase=" + phase
                                    + " sampleCount=" + sampleCount);
                }
            }
        }
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException, but completed normally

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,573 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but the call returned normally"
            );
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but got " + other.getClass().getName()
            );
        }

        int n = data.consumeInt(8, 24);
        double amplitude = 0.5 + data.consumeInt(0, 400) / 40.0;
        double omega = 0.1 + data.consumeInt(0, 200) / 40.0;
        double phase = -Math.PI + (2.0 * Math.PI * data.consumeInt(0, 1000) / 1000.0);
        double step = 0.2 + data.consumeInt(0, 20) / 10.0;
        double start = -5.0 + data.consumeInt(0, 100) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] sorted = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            sorted[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        WeightedObservedPoint[] permuted = new WeightedObservedPoint[n];
        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[n - 1 - i];
            }
        } else if (mode == 1) {
            int shift = data.consumeInt(0, n - 1);
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[(i + shift) % n];
            }
        } else {
            int left = 0;
            int right = n - 1;
            int idx = 0;
            while (left <= right) {
                permuted[idx++] = sorted[right--];
                if (left <= right) {
                    permuted[idx++] = sorted[left++];
                }
            }
        }

        double[] g1;
        double[] g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(sorted).guess();
            g2 = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (Throwable ignored) {
            return;
        }

        if (!sameBits(g1, g2)) {
            throw new RuntimeException(
                "[oracle:order-invariance] metamorphic violation: ParameterGuesser.guess() must be invariant under reordering of the same observation set because the implementation sorts observations by abscissa before computing shared state; inputSize=" + n +
                " lhs=" + arrayToString(g1) +
                " rhs=" + arrayToString(g2)
            );
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
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
## [80] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness re-throws a caught exception as its own alarm without preserving the original: a FuzzerSecurityIssueLow is thrown inside `catch (Throwable other)` without passing `other` as its cause arg

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,180 chars, 2 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but the call returned normally"
            );
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but got " + other.getClass().getName()
            );
        }

        int n = data.consumeInt(8, 24);
        double amplitude = 0.5 + data.consumeInt(0, 400) / 40.0;
        double omega = 0.1 + data.consumeInt(0, 200) / 40.0;
        double phase = -Math.PI + (2.0 * Math.PI * data.consumeInt(0, 1000) / 1000.0);
        double step = 0.2 + data.consumeInt(0, 20) / 10.0;
        double start = -5.0 + data.consumeInt(0, 100) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] sorted = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            sorted[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        WeightedObservedPoint[] permuted = new WeightedObservedPoint[n];
        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[n - 1 - i];
            }
        } else if (mode == 1) {
            int shift = data.consumeInt(0, n - 1);
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[(i + shift) % n];
            }
        } else {
            int left = 0;
            int right = n - 1;
            int idx = 0;
            while (left <= right) {
                permuted[idx++] = sorted[right--];
                if (left <= right) {
                    permuted[idx++] = sorted[left++];
                }
            }
        }

        double[] g1;
        double[] g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(sorted).guess();
            g2 = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (Throwable ignored) {
            return;
        }

        if (!sameBits(g1, g2)) {
            throw new RuntimeException(
                "[oracle:order-invariance] metamorphic violation: ParameterGuesser.guess() must be invariant under reordering of the same observation set because the implementation sorts observations by abscissa before computing shared state; inputSize=" + n +
                " lhs=" + arrayToString(g1) +
                " rhs=" + arrayToString(g2)
            );
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
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
**[user]**
```
Your harness re-throws a caught exception as its own alarm without preserving the original: a FuzzerSecurityIssueLow is thrown inside `catch (Throwable other)` without passing `other` as its cause argument — the caught crash's identity is erased and attribution cannot check it against the unpatched build. Use `new FuzzerSecurityIssueLow("<message>", other)`.
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~4,596 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but the call returned normally"
            );
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave sample from HarmonicFitterTest.testMath844, but got " + other.getClass().getName(),
                other
            );
        }

        int n = data.consumeInt(8, 24);
        double amplitude = 0.5 + data.consumeInt(0, 400) / 40.0;
        double omega = 0.1 + data.consumeInt(0, 200) / 40.0;
        double phase = -Math.PI + (2.0 * Math.PI * data.consumeInt(0, 1000) / 1000.0);
        double step = 0.2 + data.consumeInt(0, 20) / 10.0;
        double start = -5.0 + data.consumeInt(0, 100) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] sorted = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            sorted[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
        }

        WeightedObservedPoint[] permuted = new WeightedObservedPoint[n];
        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[n - 1 - i];
            }
        } else if (mode == 1) {
            int shift = data.consumeInt(0, n - 1);
            for (int i = 0; i < n; i++) {
                permuted[i] = sorted[(i + shift) % n];
            }
        } else {
            int left = 0;
            int right = n - 1;
            int idx = 0;
            while (left <= right) {
                permuted[idx++] = sorted[right--];
                if (left <= right) {
                    permuted[idx++] = sorted[left++];
                }
            }
        }

        double[] g1;
        double[] g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(sorted).guess();
            g2 = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (Throwable ignored) {
            return;
        }

        if (!sameBits(g1, g2)) {
            throw new RuntimeException(
                "[oracle:order-invariance] metamorphic violation: ParameterGuesser.guess() must be invariant under reordering of the same observation set because the implementation sorts observations by abscissa before computing shared state; inputSize=" + n +
                " lhs=" + arrayToString(g1) +
                " rhs=" + arrayToString(g2)
            );
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
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
## [82] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular-wave …

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,736 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                            + "expected=(" + w + "," + x + "," + y + ")"
                            + " actual=(" + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }

        {
            final double[] exactY = { 0, 1, 2, 3, 2, 1,
                                      0, -1, -2, -3, -2, -1,
                                      0, 1, 2, 3, 2, 1,
                                      0, -1, -2, -3, -2, -1,
                                      0, 1, 2, 3, 2, 1, 0 };
            final int len = exactY.length;
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            for (int i = 0; i < len; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, exactY[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception class " + t.getClass().getName();
            }
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math844-exact] semantic mismatch: expected MathIllegalStateException but " + detail);
            }
        }

        int scale = data.consumeInt(1, 5);
        int step = data.consumeInt(1, 4);
        int start = data.consumeInt(-20, 20);
        int repetitions = data.consumeInt(2, 4);
        int[] period = new int[] { 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -2, -1 };
        WeightedObservedPoint[] triPoints;
        HarmonicFitter.ParameterGuesser triGuesser;
        Throwable triangularWrong = null;
        boolean triangularCompletedNormally = false;
        try {
            int len = repetitions * period.length + 1;
            triPoints = new WeightedObservedPoint[len];
            for (int i = 0; i < len - 1; i++) {
                triPoints[i] = new WeightedObservedPoint(1.0, start + (double) i * step, period[i % period.length] * (double) scale);
            }
            triPoints[len - 1] = new WeightedObservedPoint(1.0, start + (double) (len - 1) * step, 0.0);
            triGuesser = new HarmonicFitter.ParameterGuesser(triPoints);
            try {
                triGuesser.guess();
                triangularCompletedNormally = true;
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            } catch (Throwable t) {
                triangularWrong = t;
            }
        } catch (Throwable t) {
            return;
        }
        if (triangularWrong != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException on valid triangular sample",
                    triangularWrong);
        }
        if (triangularCompletedNormally) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but completed normally"
                            + " scale=" + scale + " step=" + step + " start=" + start + " repetitions=" + repetitions);
        }

        {
            double amp = data.consumeInt(1, 5);
            double omega = data.consumeInt(1, 20) / 10.0;
            double phi = data.consumeInt(-30, 30) / 10.0;
            double x0 = data.consumeInt(-20, 20) / 10.0;
            double dx = data.consumeInt(1, 10) / 10.0;
            int n = data.consumeInt(8, 20);

            WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[n];
            org.apache.commons.math3.analysis.function.HarmonicOscillator oscillator =
                    new org.apache.commons.math3.analysis.function.HarmonicOscillator(amp, omega, phi);
            for (int i = 0; i < n; i++) {
                double xi = x0 + i * dx;
                harmonicPoints[i] = new WeightedObservedPoint(1.0, xi, oscillator.value(xi));
            }

            double[] g1;
            double[] g2;
            try {
                HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(harmonicPoints);
                g1 = guesser.guess();
                g2 = guesser.guess();
            } catch (Throwable t) {
                return;
            }

            boolean mismatch = g1.length != g2.length;
            if (!mismatch) {
                for (int i = 0; i < g1.length; i++) {
                    if (Double.doubleToLongBits(g1[i]) != Double.doubleToLongBits(g2[i])) {
                        mismatch = true;
                        break;
                    }
                }
            }
            if (mismatch) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:guess-idempotence] metamorphic violation: repeated guess() on the same immutable observations must agree"
                                + " first=[" + g1[0] + "," + g1[1] + "," + g1[2] + "]"
                                + " second=[" + g2[0] + "," + g2[1] + "," + g2[2] + "]");
            }

            double[] fitNoArg;
            double[] fitWithGuess;
            try {
                HarmonicFitter f1 = new HarmonicFitter(
                        new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                HarmonicFitter f2 = new HarmonicFitter(
                        new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
                for (int i = 0; i < harmonicPoints.length; i++) {
                    WeightedObservedPoint pt = harmonicPoints[i];
                    f1.addObservedPoint(pt.getWeight(), pt.getX(), pt.getY());
                    f2.addObservedPoint(pt.getWeight(), pt.getX(), pt.getY());
                }

                double[] initialGuess = new HarmonicFitter.ParameterGuesser(harmonicPoints).guess();
                fitNoArg = f1.fit();
                fitWithGuess = f2.fit(initialGuess);
            } catch (Throwable t) {
                return;
            }

            boolean fitMismatch = fitNoArg.length != fitWithGuess.length;
            if (!fitMismatch) {
                for (int i = 0; i < fitNoArg.length; i++) {
                    double a = fitNoArg[i];
                    double b = fitWithGuess[i];
                    double diff = Math.abs(a - b);
                    double tol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (!(diff <= tol || (Double.isNaN(a) && Double.isNaN(b)))) {
                        fitMismatch = true;
                        break;
                    }
                }
            }
            if (fitMismatch) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:fit-overloads-agree] metamorphic violation: fit() and fit(initialGuess) document the same fitting job and must agree when initialGuess is exactly the one fit() derives"
                                + " fit()=[" + fitNoArg[0] + "," + fitNoArg[1] + "," + fitNoArg[2] + "]"
                                + " fit(initialGuess)=[" + fitWithGuess[0] + "," + fitWithGuess[1] + "," + fitWithGuess[2] + "]");
            }
        }
    }
}
```

</details>

---
## [84] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-exact] semantic mismatch: expected MathIllegalStateException but completed normally

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,812 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        boolean sawExpected = false;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got "
                    + other.getClass().getName());
        }
        if (!sawExpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but the call returned normally");
        }

        boolean sawExpectedViaPublicApi = false;
        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
        } catch (MathIllegalStateException expected) {
            sawExpectedViaPublicApi = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but got "
                    + other.getClass().getName());
        }
        if (!sawExpectedViaPublicApi) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but the call returned normally");
        }

        int sampleCount = 4 + data.consumeInt(0, 12);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 20.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 50.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        int xShift = data.consumeInt(-50, 50);
        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

        WeightedObservedPoint[] original = new WeightedObservedPoint[sampleCount];
        WeightedObservedPoint[] pristine = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = xShift + i;
            double yy = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, yy);
            original[i] = p;
            pristine[i] = p;
        }

        HarmonicFitter.ParameterGuesser cloned = null;
        HarmonicFitter.ParameterGuesser pristineGuesser = null;
        try {
            cloned = new HarmonicFitter.ParameterGuesser(original);
            pristineGuesser = new HarmonicFitter.ParameterGuesser(pristine);
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, sampleCount);
        for (int m = 0; m < mutations; m++) {
            int idx = data.consumeInt(0, sampleCount - 1);
            double mx = data.consumeInt(-100, 100);
            double my = data.consumeInt(-1000, 1000) / 10.0;
            double mw = 1.0 + data.consumeInt(0, 10);
            original[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        try {
            double[] lhs1 = cloned.guess();
            double[] lhs2 = cloned.guess();
            double[] rhs = pristineGuesser.guess();

            if (lhs1.length != 3 || lhs2.length != 3 || rhs.length != 3) {
                throw new RuntimeException(
                    "[oracle:guess-shape] metamorphic violation: guess() must return three coefficients input="
                        + sampleCount + " lhs1Len=" + lhs1.length + " lhs2Len=" + lhs2.length + " rhsLen=" + rhs.length);
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(lhs2[i])) {
                    throw new RuntimeException(
                        "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same ParameterGuesser changed result input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + lhs2[i]);
                }
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(rhs[i])) {
                    throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents that it clones the observations array, so mutating the caller's array after construction must not affect guess(); input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + rhs[i]);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [86] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness re-throws a caught exception as its own alarm without preserving the original: a FuzzerSecurityIssueLow is thrown inside `catch (Throwable other)` without passing `other` as its cause arg

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,012 chars, 1 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        boolean sawExpected = false;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got "
                    + other.getClass().getName());
        }
        if (!sawExpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but the call returned normally");
        }

        boolean sawExpectedViaPublicApi = false;
        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
        } catch (MathIllegalStateException expected) {
            sawExpectedViaPublicApi = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but got "
                    + other.getClass().getName());
        }
        if (!sawExpectedViaPublicApi) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but the call returned normally");
        }

        int sampleCount = 4 + data.consumeInt(0, 12);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 20.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 50.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        int xShift = data.consumeInt(-50, 50);
        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

        WeightedObservedPoint[] original = new WeightedObservedPoint[sampleCount];
        WeightedObservedPoint[] pristine = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = xShift + i;
            double yy = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, yy);
            original[i] = p;
            pristine[i] = p;
        }

        HarmonicFitter.ParameterGuesser cloned = null;
        HarmonicFitter.ParameterGuesser pristineGuesser = null;
        try {
            cloned = new HarmonicFitter.ParameterGuesser(original);
            pristineGuesser = new HarmonicFitter.ParameterGuesser(pristine);
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, sampleCount);
        for (int m = 0; m < mutations; m++) {
            int idx = data.consumeInt(0, sampleCount - 1);
            double mx = data.consumeInt(-100, 100);
            double my = data.consumeInt(-1000, 1000) / 10.0;
            double mw = 1.0 + data.consumeInt(0, 10);
            original[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        try {
            double[] lhs1 = cloned.guess();
            double[] lhs2 = cloned.guess();
            double[] rhs = pristineGuesser.guess();

            if (lhs1.length != 3 || lhs2.length != 3 || rhs.length != 3) {
                throw new RuntimeException(
                    "[oracle:guess-shape] metamorphic violation: guess() must return three coefficients input="
                        + sampleCount + " lhs1Len=" + lhs1.length + " lhs2Len=" + lhs2.length + " rhsLen=" + rhs.length);
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(lhs2[i])) {
                    throw new RuntimeException(
                        "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same ParameterGuesser changed result input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + lhs2[i]);
                }
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(rhs[i])) {
                    throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents that it clones the observations array, so mutating the caller's array after construction must not affect guess(); input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + rhs[i]);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
- *[user] message: identical to step [81] — not reprinted*

</details>
<details open><summary>▸ Output (~5,844 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        boolean sawExpected = false;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }
        if (!sawExpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but the call returned normally");
        }

        boolean sawExpectedViaPublicApi = false;
        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
        } catch (MathIllegalStateException expected) {
            sawExpectedViaPublicApi = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }
        if (!sawExpectedViaPublicApi) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but the call returned normally");
        }

        int sampleCount = 4 + data.consumeInt(0, 12);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 20.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 50.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        int xShift = data.consumeInt(-50, 50);
        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

        WeightedObservedPoint[] original = new WeightedObservedPoint[sampleCount];
        WeightedObservedPoint[] pristine = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = xShift + i;
            double yy = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, yy);
            original[i] = p;
            pristine[i] = p;
        }

        HarmonicFitter.ParameterGuesser cloned;
        HarmonicFitter.ParameterGuesser pristineGuesser;
        try {
            cloned = new HarmonicFitter.ParameterGuesser(original);
            pristineGuesser = new HarmonicFitter.ParameterGuesser(pristine);
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, sampleCount);
        for (int m = 0; m < mutations; m++) {
            int idx = data.consumeInt(0, sampleCount - 1);
            double mx = data.consumeInt(-100, 100);
            double my = data.consumeInt(-1000, 1000) / 10.0;
            double mw = 1.0 + data.consumeInt(0, 10);
            original[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        try {
            double[] lhs1 = cloned.guess();
            double[] lhs2 = cloned.guess();
            double[] rhs = pristineGuesser.guess();

            if (lhs1.length != 3 || lhs2.length != 3 || rhs.length != 3) {
                throw new RuntimeException(
                    "[oracle:guess-shape] metamorphic violation: guess() must return three coefficients input="
                        + sampleCount + " lhs1Len=" + lhs1.length + " lhs2Len=" + lhs2.length + " rhsLen=" + rhs.length);
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(lhs2[i])) {
                    throw new RuntimeException(
                        "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same ParameterGuesser changed result input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + lhs2[i]);
                }
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(rhs[i])) {
                    throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents that it clones the observations array, so mutating the caller's array after construction must not affect guess(); input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + rhs[i]);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [88] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawExpected` (literal assignment, no rethrow, the caught ex

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,121 chars, 2 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
- *[assistant] message: identical to step [87] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        boolean sawExpected = false;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
        } catch (MathIllegalStateException expected) {
            sawExpected = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }
        if (!sawExpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but the call returned normally");
        }

        boolean sawExpectedViaPublicApi = false;
        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
        } catch (MathIllegalStateException expected) {
            sawExpectedViaPublicApi = true;
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }
        if (!sawExpectedViaPublicApi) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but the call returned normally");
        }

        int sampleCount = 4 + data.consumeInt(0, 12);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 20.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 50.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        int xShift = data.consumeInt(-50, 50);
        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

        WeightedObservedPoint[] original = new WeightedObservedPoint[sampleCount];
        WeightedObservedPoint[] pristine = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = xShift + i;
            double yy = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, yy);
            original[i] = p;
            pristine[i] = p;
        }

        HarmonicFitter.ParameterGuesser cloned;
        HarmonicFitter.ParameterGuesser pristineGuesser;
        try {
            cloned = new HarmonicFitter.ParameterGuesser(original);
            pristineGuesser = new HarmonicFitter.ParameterGuesser(pristine);
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, sampleCount);
        for (int m = 0; m < mutations; m++) {
            int idx = data.consumeInt(0, sampleCount - 1);
            double mx = data.consumeInt(-100, 100);
            double my = data.consumeInt(-1000, 1000) / 10.0;
            double mw = 1.0 + data.consumeInt(0, 10);
            original[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        try {
            double[] lhs1 = cloned.guess();
            double[] lhs2 = cloned.guess();
            double[] rhs = pristineGuesser.guess();

            if (lhs1.length != 3 || lhs2.length != 3 || rhs.length != 3) {
                throw new RuntimeException(
                    "[oracle:guess-shape] metamorphic violation: guess() must return three coefficients input="
                        + sampleCount + " lhs1Len=" + lhs1.length + " lhs2Len=" + lhs2.length + " rhsLen=" + rhs.length);
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(lhs2[i])) {
                    throw new RuntimeException(
                        "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same ParameterGuesser changed result input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + lhs2[i]);
                }
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(rhs[i])) {
                    throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents that it clones the observations array, so mutating the caller's array after construction must not affect guess(); input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + rhs[i]);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawExpected` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 37 is conditioned on `sawExpected` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~5,594 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but the call returned normally");
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but the call returned normally");
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }

        int sampleCount = 4 + data.consumeInt(0, 12);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 20.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 50.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        int xShift = data.consumeInt(-50, 50);
        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

        WeightedObservedPoint[] original = new WeightedObservedPoint[sampleCount];
        WeightedObservedPoint[] pristine = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = xShift + i;
            double yy = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, yy);
            original[i] = p;
            pristine[i] = p;
        }

        HarmonicFitter.ParameterGuesser cloned;
        HarmonicFitter.ParameterGuesser pristineGuesser;
        try {
            cloned = new HarmonicFitter.ParameterGuesser(original);
            pristineGuesser = new HarmonicFitter.ParameterGuesser(pristine);
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, sampleCount);
        for (int m = 0; m < mutations; m++) {
            int idx = data.consumeInt(0, sampleCount - 1);
            double mx = data.consumeInt(-100, 100);
            double my = data.consumeInt(-1000, 1000) / 10.0;
            double mw = 1.0 + data.consumeInt(0, 10);
            original[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        try {
            double[] lhs1 = cloned.guess();
            double[] lhs2 = cloned.guess();
            double[] rhs = pristineGuesser.guess();

            if (lhs1.length != 3 || lhs2.length != 3 || rhs.length != 3) {
                throw new RuntimeException(
                    "[oracle:guess-shape] metamorphic violation: guess() must return three coefficients input="
                        + sampleCount + " lhs1Len=" + lhs1.length + " lhs2Len=" + lhs2.length + " rhsLen=" + rhs.length);
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(lhs2[i])) {
                    throw new RuntimeException(
                        "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same ParameterGuesser changed result input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + lhs2[i]);
                }
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(rhs[i])) {
                    throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents that it clones the observations array, so mutating the caller's array after construction must not affect guess(); input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + rhs[i]);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got com.code_in…

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,810 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final int[] TRIANGULAR_Y = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    private static final class GuessProbe {
        final boolean completedNormally;
        final Throwable thrown;

        GuessProbe(boolean completedNormally, Throwable thrown) {
            this.completedNormally = completedNormally;
            this.thrown = thrown;
        }
    }

    private static WeightedObservedPoint[] buildTriangularPoints(int start, int step, int scale) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, TRIANGULAR_Y[i] * (double) scale);
        }
        return points;
    }

    private static GuessProbe probeGuess(HarmonicFitter.ParameterGuesser guesser) {
        try {
            guesser.guess();
            return new GuessProbe(true, null);
        } catch (Throwable t) {
            return new GuessProbe(false, t);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "weight=" + p.getWeight() + " expectedWeight=" + w
                                + " x=" + p.getX() + " expectedX=" + x
                                + " y=" + p.getY() + " expectedY=" + y);
            }
        }

        {
            WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
            for (int i = 0; i < TRIANGULAR_Y.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, TRIANGULAR_Y[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            GuessProbe exact = probeGuess(guesser);
            if (exact.completedNormally) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, actual=completed normally");
            }
            if (!(exact.thrown instanceof MathIllegalStateException)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, actual="
                                + exact.thrown.getClass().getName(),
                        exact.thrown);
            }
        }

        {
            WeightedObservedPoint[] points;
            HarmonicFitter.ParameterGuesser guesser;
            try {
                int scale = data.consumeInt(1, 5);
                int step = data.consumeInt(1, 4);
                int start = data.consumeInt(-20, 20);
                points = buildTriangularPoints(start, step, scale);
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            GuessProbe before = probeGuess(guesser);

            int mutations = data.consumeInt(1, Math.max(1, points.length));
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, points.length - 1);
                double mw = data.consumeInt(-50, 50);
                double mx = data.consumeInt(-100, 100);
                double my = data.consumeInt(-100, 100);
                points[idx] = new WeightedObservedPoint(mw, mx, my);
            }

            GuessProbe after = probeGuess(guesser);

            if (before.completedNormally) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:guesser_rejects_triangular_wave] semantic mismatch: expected MathIllegalStateException before external-array mutation, actual=completed normally");
            }
            if (!(before.thrown instanceof MathIllegalStateException)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:guesser_rejects_triangular_wave] semantic mismatch: expected MathIllegalStateException before external-array mutation, actual="
                                + before.thrown.getClass().getName(),
                        before.thrown);
            }

            /*
             * Contract justification:
             * ParameterGuesser(WeightedObservedPoint[] observations) clones the input array
             * ("this.observations = observations.clone();"). Therefore, replacing elements in
             * the caller's original array after construction must not change what guess() sees.
             * For this trusted triangular-wave family, guess() must reject with
             * MathIllegalStateException both before and after those external mutations.
             * A throw-deleting patch or an aliasing/state bug breaks this observable agreement.
             */
            if (after.completedNormally) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone-postcondition] metamorphic violation: guess() rejected before mutation but completed normally after mutating the caller array");
            }
            if (!(after.thrown instanceof MathIllegalStateException)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone-postcondition] metamorphic violation: expected MathIllegalStateException after mutating the caller array, actual="
                                + after.thrown.getClass().getName(),
                        after.thrown);
            }
        }
    }
}
```

</details>

---
## [92] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, actual=completed normally

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,677 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;

        final WeightedObservedPoint[] seedPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            seedPoints[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser seedGuesser =
                new HarmonicFitter.ParameterGuesser(seedPoints);

        boolean sawExpected = false;
        try {
            seedGuesser.guess();
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            sawExpected = true;
        }
        if (!sawExpected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact Math844 triangular sample, but guess() returned normally");
        }

        final WeightedObservedPoint[] clonedInput = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            clonedInput[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        final HarmonicFitter.ParameterGuesser clonedGuesser =
                new HarmonicFitter.ParameterGuesser(clonedInput);

        final int mutations = data.consumeInt(1, len);
        for (int m = 0; m < mutations; m++) {
            final int idx = data.consumeInt(0, len - 1);
            final double newWeight = data.consumeInt(-1000, 1000);
            final double newX = data.consumeInt(-1000, 1000);
            final double newY = data.consumeInt(-1000, 1000);
            clonedInput[idx] = new WeightedObservedPoint(newWeight, newX, newY);

            boolean stillThrows = false;
            try {
                // Contract used: ParameterGuesser(WeightedObservedPoint[] observations)
                // stores "this.observations = observations.clone();".
                // Therefore later caller-side array mutations must not change the
                // already-constructed guesser's behavior. A patch that simply deletes
                // the throw in guessAOmega() would break this observable post-condition:
                // after every external mutation, the original guesser would return
                // normally instead of still rejecting the original Math844 sample.
                clonedGuesser.guess();
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
                stillThrows = true;
            }
            if (!stillThrows) {
                throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser must be isolated from later caller-array mutations, but guess() stopped throwing after mutationIndex="
                                + m + " targetSlot=" + idx);
            }
        }

        int scale = data.consumeInt(-8, 8);
        if (scale == 0) {
            scale = 2;
        }

        final WeightedObservedPoint[] scaledPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            scaledPoints[i] = new WeightedObservedPoint(1.0, i, y[i] * scale);
        }
        final HarmonicFitter.ParameterGuesser scaledGuesser =
                new HarmonicFitter.ParameterGuesser(scaledPoints);

        boolean scaledThrows = false;
        try {
            // Metamorphic relation justified from the shown guessAOmega() body:
            // with unchanged x-values, multiplying every y by a non-zero constant
            // scales f2Integral and fPrime2Integral uniformly, so c1/c2 and c2/c3
            // preserve the same ill-conditioned c2 == 0 state as the seed.
            // Hence this whole family of inputs must be rejected the same way.
            scaledGuesser.guess();
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            scaledThrows = true;
        }
        if (!scaledThrows) {
            throw new RuntimeException(
                    "[oracle:nonzero-scale] metamorphic violation: non-zero scaling of the Math844 triangular sample must preserve MathIllegalStateException, scale="
                            + scale);
        }
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawExpected` (literal assignment, no rethrow, the caught ex

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,833 chars, 2 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;

        final WeightedObservedPoint[] seedPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            seedPoints[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser seedGuesser =
                new HarmonicFitter.ParameterGuesser(seedPoints);

        boolean sawExpected = false;
        try {
            seedGuesser.guess();
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            sawExpected = true;
        }
        if (!sawExpected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact Math844 triangular sample, but guess() returned normally");
        }

        final WeightedObservedPoint[] clonedInput = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            clonedInput[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        final HarmonicFitter.ParameterGuesser clonedGuesser =
                new HarmonicFitter.ParameterGuesser(clonedInput);

        final int mutations = data.consumeInt(1, len);
        for (int m = 0; m < mutations; m++) {
            final int idx = data.consumeInt(0, len - 1);
            final double newWeight = data.consumeInt(-1000, 1000);
            final double newX = data.consumeInt(-1000, 1000);
            final double newY = data.consumeInt(-1000, 1000);
            clonedInput[idx] = new WeightedObservedPoint(newWeight, newX, newY);

            boolean stillThrows = false;
            try {
                // Contract used: ParameterGuesser(WeightedObservedPoint[] observations)
                // stores "this.observations = observations.clone();".
                // Therefore later caller-side array mutations must not change the
                // already-constructed guesser's behavior. A patch that simply deletes
                // the throw in guessAOmega() would break this observable post-condition:
                // after every external mutation, the original guesser would return
                // normally instead of still rejecting the original Math844 sample.
                clonedGuesser.guess();
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
                stillThrows = true;
            }
            if (!stillThrows) {
                throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser must be isolated from later caller-array mutations, but guess() stopped throwing after mutationIndex="
                                + m + " targetSlot=" + idx);
            }
        }

        int scale = data.consumeInt(-8, 8);
        if (scale == 0) {
            scale = 2;
        }

        final WeightedObservedPoint[] scaledPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            scaledPoints[i] = new WeightedObservedPoint(1.0, i, y[i] * scale);
        }
        final HarmonicFitter.ParameterGuesser scaledGuesser =
                new HarmonicFitter.ParameterGuesser(scaledPoints);

        boolean scaledThrows = false;
        try {
            // Metamorphic relation justified from the shown guessAOmega() body:
            // with unchanged x-values, multiplying every y by a non-zero constant
            // scales f2Integral and fPrime2Integral uniformly, so c1/c2 and c2/c3
            // preserve the same ill-conditioned c2 == 0 state as the seed.
            // Hence this whole family of inputs must be rejected the same way.
            scaledGuesser.guess();
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            scaledThrows = true;
        }
        if (!scaledThrows) {
            throw new RuntimeException(
                    "[oracle:nonzero-scale] metamorphic violation: non-zero scaling of the Math844 triangular sample must preserve MathIllegalStateException, scale="
                            + scale);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawExpected` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 29 is conditioned on `sawExpected` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~4,670 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;

        final WeightedObservedPoint[] seedPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            seedPoints[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser seedGuesser =
                new HarmonicFitter.ParameterGuesser(seedPoints);

        try {
            seedGuesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact Math844 triangular sample, but guess() returned normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // This is the exact observable pinned by the failing test.
        } catch (RuntimeException other) {
            return;
        }

        final WeightedObservedPoint[] externalArray = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            externalArray[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        final HarmonicFitter.ParameterGuesser clonedGuesser =
                new HarmonicFitter.ParameterGuesser(externalArray);

        final int mutations = data.consumeInt(1, len);
        for (int m = 0; m < mutations; m++) {
            final int idx = data.consumeInt(0, len - 1);
            final double newWeight = data.consumeInt(-1000, 1000);
            final double newX = data.consumeInt(-1000, 1000);
            final double newY = data.consumeInt(-1000, 1000);
            externalArray[idx] = new WeightedObservedPoint(newWeight, newX, newY);

            try {
                /*
                 * Documented constructor body shows: this.observations = observations.clone();
                 * Therefore later caller-side mutations of the original array must not affect
                 * the already-constructed guesser. This is a post-condition observable through
                 * guess(): the cloned guesser must keep rejecting the original Math844 sample
                 * after every external mutation.
                 */
                clonedGuesser.guess();
                throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser must be isolated from later caller-array mutations, but guess() returned normally after mutationIndex="
                                + m + " targetSlot=" + idx);
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
                // Expected: constructor cloned the original contents.
            } catch (RuntimeException other) {
                return;
            }
        }

        int scale = data.consumeInt(-8, 8);
        if (scale == 0) {
            scale = 2;
        }

        final WeightedObservedPoint[] scaledPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            scaledPoints[i] = new WeightedObservedPoint(1.0, i, y[i] * scale);
        }
        final HarmonicFitter.ParameterGuesser scaledGuesser =
                new HarmonicFitter.ParameterGuesser(scaledPoints);

        try {
            /*
             * Metamorphic relation from the shown guessAOmega() body: with x unchanged and all
             * y multiplied by one non-zero constant k, both integrated quantities scale by k^2.
             * Hence c1 scales by k^6, c2 by k^4, c3 by k^4, so the ill-conditioned c2 == 0
             * state of the Math844 sample is preserved. A correct implementation must therefore
             * reject every non-zero scaled variant the same way.
             */
            scaledGuesser.guess();
            throw new RuntimeException(
                    "[oracle:nonzero-scale] metamorphic violation: non-zero scaling of the Math844 triangular sample must preserve MathIllegalStateException, scale="
                            + scale);
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Expected.
        } catch (RuntimeException other) {
            return;
        }
    }
}
```

</details>

---
## [96] ⚙️ harness-repair · `attempt_023`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [97] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact Math844 triangular sample, but guess() returned norma…

---
## [98] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,721 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final int[] EXACT_TRIANGULAR_Y = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    private static WeightedObservedPoint[] buildExactTriangularPoints(int start, int step, int scale) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[EXACT_TRIANGULAR_Y.length];
        for (int i = 0; i < EXACT_TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, EXACT_TRIANGULAR_Y[i] * (double) scale);
        }
        return points;
    }

    private static WeightedObservedPoint[] buildGeneralTriangularPoints(int start, int step, int scale, int periods, int extra) {
        int[] basePeriod = new int[] { 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -2, -1 };
        int len = periods * basePeriod.length + extra;
        if (len < 4) {
            len = 4;
        }
        WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            int y = basePeriod[i % basePeriod.length];
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, y * (double) scale);
        }
        return points;
    }

    private static Throwable probeGuesser(HarmonicFitter.ParameterGuesser guesser) {
        try {
            guesser.guess();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static void assertMath844Rejects(HarmonicFitter.ParameterGuesser guesser, String oracleId, String context) {
        Throwable t = probeGuesser(guesser);
        if (t == null) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected MathIllegalStateException but guess() completed normally; " + context);
        }
        if (!(t instanceof org.apache.commons.math3.exception.MathIllegalStateException)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected MathIllegalStateException but got " + t.getClass().getName() + "; " + context, t);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p = new WeightedObservedPoint(w, x, y);
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                    "weight=" + p.getWeight() + "/" + w +
                    " x=" + p.getX() + "/" + x +
                    " y=" + p.getY() + "/" + y);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        HarmonicFitter.ParameterGuesser exactGuesser;
        WeightedObservedPoint[] exactPoints = buildExactTriangularPoints(0, 1, 1);
        try {
            exactGuesser = new HarmonicFitter.ParameterGuesser(exactPoints);
        } catch (Throwable t) {
            return;
        }

        assertMath844Rejects(exactGuesser, "math844-exact", "input=exact failing test sample");

        int mutations = data.consumeInt(1, exactPoints.length);
        for (int i = 0; i < mutations; i++) {
            int idx = data.consumeInt(0, exactPoints.length - 1);
            double newWeight = data.consumeInt(-20, 20);
            double newX = data.consumeInt(-50, 50);
            double newY = data.consumeInt(-50, 50);
            exactPoints[idx] = new WeightedObservedPoint(newWeight, newX, newY);

            // Constructor contract: ParameterGuesser stores observations.clone().
            // So replacing slots in the caller's array after construction must not change the guesser's later observable behaviour.
            // A patch that silently keeps/uses caller-owned state, or a throw-deleting patch that returns coefficients instead of rejecting,
            // breaks this post-condition when we re-probe after every external state change.
            assertMath844Rejects(exactGuesser, "constructor-clone", "after external array mutation " + i + " at index " + idx);
        }

        try {
            org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer();
            HarmonicFitter fitter = new HarmonicFitter(optimizer);
            for (WeightedObservedPoint point : buildExactTriangularPoints(0, 1, 1)) {
                fitter.addObservedPoint(point);
            }
            try {
                fitter.fit();
            } catch (Throwable ignored) {
            }
            try {
                fitter.fit(new double[] { 1.0, 1.0, 0.0 });
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        HarmonicFitter.ParameterGuesser variedGuesser;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            int periods = data.consumeInt(2, 4);
            int extra = data.consumeInt(1, 7);
            WeightedObservedPoint[] variedPoints = buildGeneralTriangularPoints(start, step, scale, periods, extra);
            variedGuesser = new HarmonicFitter.ParameterGuesser(variedPoints);
        } catch (Throwable t) {
            return;
        }

        Throwable varied = probeGuesser(variedGuesser);
        if (varied == null) {
            throw new FuzzerSecurityIssueLow(
                "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but completed normally");
        }
        if (!(varied instanceof org.apache.commons.math3.exception.MathIllegalStateException)) {
            throw new FuzzerSecurityIssueLow(
                "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but got " +
                varied.getClass().getName(), varied);
        }
    }
}
```

</details>

---
## [99] ⚙️ harness-repair · `attempt_024`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [100] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-exact] semantic mismatch: expected MathIllegalStateException but guess() completed normally; input=exact failing test sample

---
## [101] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,874 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for HarmonicFitter.ParameterGuesser.guess() on the Math844 triangular-wave sample, but no exception was thrown");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (org.apache.commons.math3.exception.NumberIsTooSmallException unexpected) {
            return;
        } catch (org.apache.commons.math3.exception.ZeroException unexpected) {
            return;
        }

        int n = data.consumeInt(4, 20);
        double amplitude = data.consumeInt(-20, 20);
        if (amplitude == 0.0) {
            amplitude = 1.0;
        }
        double omega = data.consumeInt(1, 20) / 10.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        double step = data.consumeInt(1, 10) / 10.0;
        double start = data.consumeInt(-50, 50) / 10.0;

        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] ordered = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            double value = oscillator.value(x);
            ordered[i] = new WeightedObservedPoint(1.0, x, value);
        }

        WeightedObservedPoint[] permuted = ordered.clone();
        int swaps = data.consumeInt(0, n * 2);
        for (int i = 0; i < swaps; i++) {
            int a = data.consumeInt(0, n - 1);
            int b = data.consumeInt(0, n - 1);
            WeightedObservedPoint tmp = permuted[a];
            permuted[a] = permuted[b];
            permuted[b] = tmp;
        }

        double[] guessOrdered;
        double[] guessPermuted;
        try {
            guessOrdered = new HarmonicFitter.ParameterGuesser(ordered).guess();
            guessPermuted = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (Throwable t) {
            return;
        }

        if (guessOrdered.length != guessPermuted.length) {
            throw new RuntimeException(
                "[oracle:sort-invariance] metamorphic violation: reordered observations changed guess array length inputLength="
                    + n + " lhsLen=" + guessOrdered.length + " rhsLen=" + guessPermuted.length);
        }

        for (int i = 0; i < guessOrdered.length; i++) {
            if (!sameDouble(guessOrdered[i], guessPermuted[i])) {
                throw new RuntimeException(
                    "[oracle:sort-invariance] metamorphic violation: ParameterGuesser sorts observations by abscissa, so the same observation set must yield the same guess regardless of input order inputLength="
                        + n + " index=" + i + " lhs=" + guessOrdered[i] + " rhs=" + guessPermuted[i]);
            }
        }
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }
}
```

</details>

---
## [102] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for HarmonicFitter.ParameterGuesser.guess() on the Math844 triangul…

---
## [103] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,272 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Trusted invariant: WeightedObservedPoint is an immutable value holder,
        // so getters must return exactly the constructor arguments.
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new FuzzerSecurityIssueLow(
                "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                "weight=" + p.getWeight() + "/" + w +
                " x=" + p.getX() + "/" + x +
                " y=" + p.getY() + "/" + y);
        }

        // Lifted oracle from HarmonicFitterTest.testMath844: this exact sample must
        // make ParameterGuesser.guess() throw MathIllegalStateException.
        boolean exactViolated = false;
        Throwable exactWrong = null;
        try {
            final double[] exactY = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
            };
            final WeightedObservedPoint[] exactPoints = new WeightedObservedPoint[exactY.length];
            for (int i = 0; i < exactY.length; i++) {
                exactPoints[i] = new WeightedObservedPoint(1.0, i, exactY[i]);
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(exactPoints);
            guesser.guess();
            exactViolated = true;
        } catch (MathIllegalStateException ok) {
        } catch (Throwable t) {
            exactViolated = true;
            exactWrong = t;
        }
        if (exactViolated) {
            if (exactWrong != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but got " +
                    exactWrong.getClass().getName(), exactWrong);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but guess() completed normally");
        }

        // Generalization of the trusted failing shape: same triangular wave, but with
        // fuzzed positive scale, x-origin, and x-step while keeping strictly increasing x.
        boolean triViolated = false;
        Throwable triWrong = null;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            final int[] base = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
            };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + ((double) i * step), base[i] * (double) scale);
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            triViolated = true;
        } catch (MathIllegalStateException ok) {
        } catch (Throwable t) {
            triViolated = true;
            triWrong = t;
        }
        if (triViolated) {
            if (triWrong != null) {
                throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but got " +
                    triWrong.getClass().getName(), triWrong);
            }
            throw new FuzzerSecurityIssueLow(
                "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but guess() completed normally");
        }

        // Mandatory post-condition / hidden-state check:
        // ParameterGuesser's constructor clones the observations array ("this.observations = observations.clone();"),
        // and guess() is a reader of the already-established state (a, omega, phi). Therefore:
        //   (1) calling guess() twice without changing the guesser must return the same coefficients, and
        //   (2) mutating the caller's original array after construction must not change later guess() results.
        // A patch that silently skips the intended rejection and corrupts/stomps shared state can violate this
        // observable stability even when no exception is thrown.
        boolean cloneStateViolated = false;
        String cloneStateDetail = null;
        try {
            int n = data.consumeInt(8, 20);
            int step = data.consumeInt(1, 5);
            int start = data.consumeInt(-30, 30);
            int amplitude = data.consumeInt(1, 6);
            int omegaDen = data.consumeInt(2, 10);
            int phiNum = data.consumeInt(-6, 6);

            double a = amplitude;
            double omega = Math.PI / omegaDen;
            double phi = phiNum * (Math.PI / 8.0);

            HarmonicOscillator osc = new HarmonicOscillator(a, omega, phi);
            WeightedObservedPoint[] src = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = start + (double) i * step;
                src[i] = new WeightedObservedPoint(1.0, xi, osc.value(xi));
            }

            HarmonicFitter.ParameterGuesser stableGuesser = new HarmonicFitter.ParameterGuesser(src);
            double[] g1 = stableGuesser.guess();
            double[] g2 = stableGuesser.guess();

            if (!sameBits(g1, g2)) {
                cloneStateViolated = true;
                cloneStateDetail = "repeated guess() changed result first=" + formatArray(g1) + " second=" + formatArray(g2);
            }

            int mutateIndex = data.consumeInt(0, n - 1);
            src[mutateIndex] = new WeightedObservedPoint(
                7.0,
                src[mutateIndex].getX() + 123.0,
                src[mutateIndex].getY() - 456.0
            );

            double[] g3 = stableGuesser.guess();
            if (!cloneStateViolated && !sameBits(g1, g3)) {
                cloneStateViolated = true;
                cloneStateDetail = "mutating caller array changed guesser state before=" + formatArray(g1) + " after=" + formatArray(g3);
            }
        } catch (Throwable t) {
            return;
        }
        if (cloneStateViolated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:constructor-clone-state] metamorphic violation: " + cloneStateDetail);
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
                return false;
            }
        }
        return true;
    }

    private static String formatArray(double[] v) {
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
## [104] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but guess() completed normally

---
## [105] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,997 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact testMath844 sample, but guess() returned normally"
            );
        } catch (MathIllegalStateException expected) {
            // Ground-truth lifted from HarmonicFitterTest.testMath844.
        } catch (RuntimeException other) {
            return;
        }

        final int n = 4 + data.consumeInt(0, 12);
        final double amplitude = data.consumeInt(-20, 20);
        final double omega = (data.consumeInt(1, 1000)) / 100.0;
        final double phase = (data.consumeInt(-314, 314)) / 100.0;
        final double step = (data.consumeInt(1, 100)) / 10.0;
        final double start = data.consumeInt(-100, 100);

        if (amplitude == 0.0 || omega == 0.0 || step == 0.0) {
            return;
        }

        final HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        final WeightedObservedPoint[] original = new WeightedObservedPoint[n];
        final WeightedObservedPoint[] snapshot = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + i * step;
            final double v = oscillator.value(x);
            final WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, v);
            original[i] = p;
            snapshot[i] = p;
        }

        final HarmonicFitter.ParameterGuesser constructed;
        try {
            constructed = new HarmonicFitter.ParameterGuesser(original);
        } catch (RuntimeException e) {
            return;
        }

        if (original.length == 0) {
            return;
        }

        final int mutations = 1 + data.consumeInt(0, Math.max(1, n) - 1);
        for (int m = 0; m < mutations; m++) {
            final int idx = data.consumeInt(0, n - 1);
            final double mx = start + data.consumeInt(-100, 100);
            final double my = data.consumeInt(-1000, 1000) / 10.0;
            original[idx] = new WeightedObservedPoint(1.0, mx, my);
        }

        final double[] lhs;
        final double[] rhs;
        try {
            lhs = constructed.guess();
            rhs = new HarmonicFitter.ParameterGuesser(snapshot).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != 3 || rhs.length != 3) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (Double.doubleToLongBits(lhs[i]) != Double.doubleToLongBits(rhs[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone] metamorphic violation: ParameterGuesser(WeightedObservedPoint[] observations) documents and implements cloning of the input array, so replacing slots in the caller's array after construction must not change what guess() reads from internal state; index="
                        + i + " lhs=" + lhs[i] + " rhs=" + rhs[i]
                );
            }
        }
    }
}
```

</details>

---
## [106] ⚙️ harness-repair · `attempt_027`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [107] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact testMath844 sample, but guess() returned normally

---
## [108] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,084 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] exactPoints = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            exactPoints[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter viaPublicApi =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < exactPoints.length; i++) {
                viaPublicApi.addObservedPoint(exactPoints[i]);
            }
            try {
                viaPublicApi.fit();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        HarmonicFitter.ParameterGuesser liftedGuesser;
        try {
            liftedGuesser = new HarmonicFitter.ParameterGuesser(exactPoints);
        } catch (Throwable t) {
            return;
        }
        try {
            liftedGuesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected org.apache.commons.math3.exception.MathIllegalStateException but guess() completed normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected org.apache.commons.math3.exception.MathIllegalStateException but got " + t.getClass().getName(), t);
        }

        double w;
        double x;
        double yy;
        WeightedObservedPoint p;
        try {
            w = data.consumeInt(-1000, 1000) / 10.0;
            x = data.consumeInt(-1000, 1000) / 10.0;
            yy = data.consumeInt(-1000, 1000) / 10.0;
            p = new WeightedObservedPoint(w, x, yy);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(yy)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:wop-roundtrip] relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                    + "weight=" + p.getWeight() + "/" + w
                    + " x=" + p.getX() + "/" + x
                    + " y=" + p.getY() + "/" + yy);
        }

        HarmonicFitter.ParameterGuesser generalizedGuesser = null;
        boolean generalizedBuilt = false;
        Throwable generalizedCaught = null;
        try {
            final int scale = data.consumeInt(1, 5);
            final int step = data.consumeInt(1, 4);
            final int start = data.consumeInt(-20, 20);
            final int repeats = data.consumeInt(2, 4);
            final int[] period = { 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -2, -1 };
            final boolean appendTerminalZero = data.consumeBoolean();

            final int len = repeats * period.length + (appendTerminalZero ? 1 : 0);
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            int idx = 0;
            for (int r = 0; r < repeats; r++) {
                for (int i = 0; i < period.length; i++) {
                    points[idx] = new WeightedObservedPoint(1.0, start + idx * (double) step, period[i] * (double) scale);
                    idx++;
                }
            }
            if (appendTerminalZero) {
                points[idx] = new WeightedObservedPoint(1.0, start + idx * (double) step, 0.0);
            }
            generalizedGuesser = new HarmonicFitter.ParameterGuesser(points);
            generalizedBuilt = true;
        } catch (Throwable t) {
            generalizedCaught = t;
        }
        if (generalizedCaught != null || !generalizedBuilt) {
            return;
        }
        boolean generalizedViolation = false;
        String generalizedDetail = "completed normally";
        Throwable generalizedCause = null;
        try {
            generalizedGuesser.guess();
            generalizedViolation = true;
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            generalizedViolation = true;
            generalizedDetail = "wrong exception class " + t.getClass().getName();
            generalizedCause = t;
        }
        if (generalizedViolation) {
            if (generalizedCause != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:triangular-wave] relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " + generalizedDetail,
                    generalizedCause);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:triangular-wave] relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " + generalizedDetail);
        }

        try {
            final int n = data.consumeInt(8, 20);
            final int amplitudeInt = data.consumeInt(1, 5);
            final double amplitude = (double) amplitudeInt;
            final int startX = data.consumeInt(-10, 10);
            final double stepX = data.consumeInt(1, 3) / 2.0;
            final double omega = Math.PI / 6.0;
            final WeightedObservedPoint[] harmonic = new WeightedObservedPoint[n];
            final WeightedObservedPoint[] harmonicCopy = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                final double xi = startX + i * stepX;
                final double yi = amplitude * Math.cos(omega * xi);
                harmonic[i] = new WeightedObservedPoint(1.0, xi, yi);
                harmonicCopy[i] = harmonic[i];
            }

            final HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(harmonic);
            final double[] first = g1.guess();

            final int mutations = data.consumeInt(1, n);
            for (int m = 0; m < mutations; m++) {
                final int target = data.consumeInt(0, n - 1);
                final double mx = data.consumeInt(-50, 50);
                final double my = data.consumeInt(-50, 50);
                harmonic[target] = new WeightedObservedPoint(1.0, mx, my);
            }

            final double[] second = g1.guess();
            final HarmonicFitter.ParameterGuesser g2 = new HarmonicFitter.ParameterGuesser(harmonicCopy);
            final double[] third = g2.guess();

            boolean mismatch = false;
            for (int i = 0; i < first.length; i++) {
                if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])
                        || Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(third[i])) {
                    mismatch = true;
                    break;
                }
            }
            if (mismatch) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone-reader] metamorphic violation: constructor clones the observations array (`this.observations = observations.clone()`), so replacing elements in the caller's array after construction must not change what guess() reports; also guess() is a no-arg reader and repeated calls should agree. first="
                        + java.util.Arrays.toString(first)
                        + " second=" + java.util.Arrays.toString(second)
                        + " third=" + java.util.Arrays.toString(third));
            }
        } catch (Throwable t) {
            if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            }
            return;
        }

        try {
            final int n = data.consumeInt(8, 18);
            final double amplitude = data.consumeInt(1, 4);
            final double omega = Math.PI / data.consumeInt(5, 10);
            final double phase = data.consumeInt(-3, 3) / 10.0;
            final double startX = data.consumeInt(-5, 5);
            final double stepX = data.consumeInt(1, 3) / 3.0;

            final WeightedObservedPoint[] obs = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                final double xi = startX + i * stepX;
                final double yi = amplitude * Math.cos(omega * xi + phase);
                obs[i] = new WeightedObservedPoint(1.0, xi, yi);
            }

            final HarmonicFitter.ParameterGuesser seedGuesser = new HarmonicFitter.ParameterGuesser(obs);
            final double[] initialGuess = seedGuesser.guess();

            final HarmonicFitter f1 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            final HarmonicFitter f2 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < obs.length; i++) {
                f1.addObservedPoint(obs[i]);
                f2.addObservedPoint(obs[i]);
            }

            final double[] lhs = f1.fit();
            final double[] rhs = f2.fit(initialGuess);

            boolean violated = false;
            for (int i = 0; i < lhs.length; i++) {
                final double diff = Math.abs(lhs[i] - rhs[i]);
                if (!(diff <= 1.0e-7 || (Double.isNaN(lhs[i]) && Double.isNaN(rhs[i])))) {
                    violated = true;
                    break;
                }
            }
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:fit-overloads] metamorphic violation: fit() and fit(double[] initialGuess) are sibling overloads for the same fitting job, and fit() computes a first guess internally; supplying that same real-library guess explicitly must agree. lhs="
                        + java.util.Arrays.toString(lhs)
                        + " rhs=" + java.util.Arrays.toString(rhs)
                        + " initialGuess=" + java.util.Arrays.toString(initialGuess));
            }
        } catch (Throwable t) {
            if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            }
            return;
        }
    }
}
```

</details>

---
## [109] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected org.apache.commons.math3.exception.MathIllegalStateException but got com.code_intelligence.jaz…

---
## [110] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,776 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final double[] TRIANGULAR_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        WeightedObservedPoint[] seedPoints = buildSeedPoints();

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(seedPoints);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException for the exact "
                    + "HarmonicFitterTest.testMath844 sample, but guess() returned normally");
        } catch (MathIllegalStateException expected) {
            // Exact lifted oracle from HarmonicFitterTest.testMath844:
            // for this fixed triangular sample, guess() must throw MathIllegalStateException.
        } catch (Throwable other) {
            return;
        }

        try {
            WeightedObservedPoint[] permuted = buildSeedPoints();
            shuffle(permuted, data);
            HarmonicFitter.ParameterGuesser permutedGuesser = new HarmonicFitter.ParameterGuesser(permuted);
            permutedGuesser.guess();
            // Contract justification: ParameterGuesser has a private sortObservations() method whose purpose is
            // "Sort the observations with respect to the abscissa." Therefore, reordering the same observations
            // is an equivalent input and must preserve the outcome. A throw-deleting patch breaks this too.
            throw new RuntimeException(
                "[oracle:sort-invariance] metamorphic violation: equivalent inputs under observation permutation "
                    + "must preserve the documented test outcome; sortedSample=throws(MathIllegalStateException) "
                    + "permutedSample=returned");
        } catch (MathIllegalStateException expected) {
            // Same dataset, different order: sortObservations() should make the behavior equivalent.
        } catch (Throwable other) {
            return;
        }

        try {
            WeightedObservedPoint[] original = buildSeedPoints();
            HarmonicFitter.ParameterGuesser clonedGuesser = new HarmonicFitter.ParameterGuesser(original);

            int mutations = 1;
            if (original.length > 0) {
                mutations = data.consumeInt(1, original.length);
            }
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, original.length - 1);
                double x = data.consumeInt(-1000, 1000);
                double y = data.consumeInt(-1000, 1000);
                original[idx] = new WeightedObservedPoint(1.0, x, y);
            }

            clonedGuesser.guess();
            // Contract justification: the constructor body shown in the prompt does
            // "this.observations = observations.clone();"
            // so later mutations to the caller-owned array must not affect the guesser's state.
            // Since the pre-mutation seed is the exact failing test sample, the post-mutation call must still
            // throw MathIllegalStateException. A patch that merely deletes the throw will violate this.
            throw new RuntimeException(
                "[oracle:ctor-clone] metamorphic violation: ParameterGuesser constructor clones observations, "
                    + "so mutating the original array after construction must not change the outcome; "
                    + "expected=MathIllegalStateException actual=returned");
        } catch (MathIllegalStateException expected) {
            // Constructor clone post-condition holds.
        } catch (Throwable other) {
            return;
        }
    }

    private static WeightedObservedPoint[] buildSeedPoints() {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, TRIANGULAR_Y[i]);
        }
        return points;
    }

    private static void shuffle(WeightedObservedPoint[] points, FuzzedDataProvider data) {
        for (int i = points.length - 1; i > 0; i--) {
            int j = data.consumeInt(0, i);
            WeightedObservedPoint tmp = points[i];
            points[i] = points[j];
            points[j] = tmp;
        }
    }
}
```

</details>

---
## [111] ⚙️ harness-repair · `attempt_029`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [112] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844] semantic mismatch: expected MathIllegalStateException for the exact HarmonicFitterTest.testMath844 sample, but guess() ret…

---
## [113] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,664 chars)</summary>

```
package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p = new WeightedObservedPoint(w, x, y);
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "weight=" + p.getWeight() + "/" + w
                                + " x=" + p.getX() + "/" + x
                                + " y=" + p.getY() + "/" + y);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        final double[] exactY = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };

        {
            WeightedObservedPoint[] points = new WeightedObservedPoint[exactY.length];
            for (int i = 0; i < exactY.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, exactY[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            int mutations = data.consumeInt(1, Math.max(1, points.length));
            for (int m = 0; m < mutations; m++) {
                int idx = data.consumeInt(0, points.length - 1);
                double mx = data.consumeInt(-100, 100);
                double my = data.consumeInt(-100, 100);
                double mw = data.consumeInt(-10, 10);
                points[idx] = new WeightedObservedPoint(mw, mx, my);
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception " + t.getClass().getName();
            }
            if (violated) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-parameter-guesser] semantic mismatch: expected MathIllegalStateException but "
                                + detail);
            }
        }

        {
            HarmonicFitter fitter;
            try {
                fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < exactY.length; i++) {
                    fitter.addObservedPoint(1.0, i, exactY[i]);
                }
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                fitter.fit();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception " + t.getClass().getName();
            }
            if (violated) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-harmonic-fitter-fit] semantic mismatch: expected MathIllegalStateException but "
                                + detail);
            }
        }

        {
            WeightedObservedPoint[] points;
            HarmonicFitter.ParameterGuesser guesser;
            try {
                int scale = data.consumeInt(1, 5);
                int step = data.consumeInt(1, 4);
                int start = data.consumeInt(-20, 20);
                points = new WeightedObservedPoint[exactY.length];
                for (int i = 0; i < exactY.length; i++) {
                    points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, exactY[i] * (double) scale);
                }
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String detail = "completed normally";
            try {
                guesser.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
            } catch (Throwable t) {
                violated = true;
                detail = "wrong exception " + t.getClass().getName();
            }
            if (violated) {
                throw new FuzzerSecurityIssueLow(
                        "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but "
                                + detail);
            }
        }

        {
            try {
                int count = data.consumeInt(8, 20);
                double amplitude = data.consumeInt(1, 50) / 10.0;
                double omega = data.consumeInt(1, 30) / 10.0;
                double phase = data.consumeInt(-30, 30) / 10.0;
                double start = data.consumeInt(-20, 20) / 10.0;
                double step = data.consumeInt(1, 10) / 10.0;
                HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

                WeightedObservedPoint[] points = new WeightedObservedPoint[count];
                HarmonicFitter.ParameterGuesser guesser = null;
                try {
                    for (int i = 0; i < count; i++) {
                        double px = start + i * step;
                        points[i] = new WeightedObservedPoint(1.0, px, osc.value(px));
                    }
                    guesser = new HarmonicFitter.ParameterGuesser(points);
                } catch (Throwable t) {
                    return;
                }

                double[] g1;
                double[] g2;
                try {
                    g1 = guesser.guess();
                    g2 = guesser.guess();
                } catch (Throwable t) {
                    return;
                }

                // The constructor clones observations and guess() only estimates coefficients
                // from that fixed sample; calling guess() twice on the same instance should
                // therefore be stable. A patch that silently corrupts shared state instead of
                // rejecting bad data would violate this read-only/idempotent observable.
                boolean same = g1 != null && g2 != null && g1.length == g2.length;
                if (same) {
                    for (int i = 0; i < g1.length; i++) {
                        if (Double.doubleToLongBits(g1[i]) != Double.doubleToLongBits(g2[i])) {
                            same = false;
                            break;
                        }
                    }
                }
                if (!same) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:guess-idempotence] metamorphic violation: repeated guess() changed result "
                                    + "first=[" + arrayToString(g1) + "] second=[" + arrayToString(g2) + "]");
                }

                HarmonicFitter fitter1;
                HarmonicFitter fitter2;
                double[] fitNoArg;
                double[] fitWithGuess;
                try {
                    fitter1 = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                    fitter2 = new HarmonicFitter(new LevenbergMarquardtOptimizer());
                    for (int i = 0; i < count; i++) {
                        fitter1.addObservedPoint(points[i]);
                        fitter2.addObservedPoint(points[i]);
                    }
                    fitNoArg = fitter1.fit();
                    fitWithGuess = fitter2.fit(g1);
                } catch (Throwable t) {
                    return;
                }

                // The two fit overloads are documented to perform the same fitting job; on the
                // same exact harmonic sample, using fit() or fit(initialGuess) where the initial
                // guess comes from the library's own ParameterGuesser must agree up to numerical
                // tolerance. This exercises the patched path through the public HarmonicFitter API.
                if (!closeArrays(fitNoArg, fitWithGuess, 1e-6)) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree "
                                    + "fit()=[" + arrayToString(fitNoArg) + "] fit(guess)=[" + arrayToString(fitWithGuess) + "]");
                }
            } catch (FuzzerSecurityIssueLow e) {
                throw e;
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static boolean closeArrays(double[] a, double[] b, double tol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isNaN(da) || Double.isNaN(db) || Double.isInfinite(da) || Double.isInfinite(db)) {
                return false;
            }
            if (Math.abs(da - db) > tol) {
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
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }
}
```

</details>

---
## [114] ⚙️ harness-repair · `attempt_030`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [115] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath844-parameter-guesser] semantic mismatch: expected MathIllegalStateException but completed normally

---
## [116] ⚙️ coverage · `jazzer coverage dumps`
**output:** **25/25 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov; 25 raw fuzz log(s)**
<details><summary>▸ detail (~13,925 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 470}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 460}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 1969}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 460}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 464}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 457}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 465}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 456}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 464}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 462}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 471}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 457}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 467}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 458}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 463}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 459}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 540}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 456}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 461}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 456}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 462}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 457}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 1969}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 456}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 470}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_020', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_020', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/002_patch1-Math-25-ACS_c_rep001/harness_src/attempt_030.java']}

</details>
