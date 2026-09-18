# Pipeline trace — Math-50

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
@@ -184,10 +184,6 @@
                     break;
                 case REGULA_FALSI:
                     // Nothing.
-                    if (x == x1) {
-                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
-                        f0 = computeObjectiveValue(x0);
-                    }
                     break;
                 default:
                     // Should never happen.
```

102 sequential steps — 32 LLM calls, 70 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest', test_method (~1,043 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest', test_method='testIssue631', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/src/test/java/org/apache/commons/math/analysis/solvers/RegulaFalsiSolverTest.java', method_source='    @Test(expected=TooManyEvaluationsException.class)\\n    public void testIssue631() {\\n        final UnivariateRealFunction f = new UnivariateRealFunction() {\\n                /** {@inheritDoc} */\\n                public double value(double x) {\\n                    return Math.exp(x) - Math.pow(Math.PI, 3.0);\\n                }\\n            };\\n\\n        final UnivariateRealSolver solver = new RegulaFalsiSolver();\\n        final double root = solver.solve(3624, f, 1, 10);\\n        Assert.assertEquals(3.4341896575482003, root, 1e-15);\\n\\t}', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest::testIssue631",
    "failure_message": "--- org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest::testIssue631\njunit.framework.AssertionFailedError: Expected exception: org.apache.commons.math.exception.TooManyEvaluationsException",
    "support_chars": 156
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~25,839 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java\n+++ /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java\n@@ -184,10 +184,6 @@\n                     break;\n                 case REGULA_FALSI:\n                     // Nothing.\n-                    if (x == x1) {\n-                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));\n-                        f0 = computeObjectiveValue(x0);\n-                    }\n                     break;\n                 default:\n                     // Should never happen.\n",
  "functions": [
    {
      "func_name": "doSolve",
      "func_signature": "final protected double doSolve()",
      "func_source": "    protected final double doSolve() {\n        // Get initial solution\n        double x0 = getMin();\n        double x1 = getMax();\n        double f0 = computeObjectiveValue(x0);\n        double f1 = computeObjectiveValue(x1);\n\n        // If one of the bounds is the exact root, return it. Since these are\n        // not under-approximations or over-approximations, we can return them\n        // regardless of the allowed solutions.\n        if (f0 == 0.0) {\n            return x0;\n        }\n        if (f1 == 0.0) {\n            return x1;\n        }\n\n        // Verify bracketing of initial solution.\n        verifyBracketing(x0, x1);\n\n        // Get accuracies.\n        final double ftol = getFunctionValueAccuracy();\n        final double atol = getAbsoluteAccuracy();\n        final double rtol = getRelativeAccuracy();\n\n        // Keep track of inverted intervals, meaning that the left bound is\n        // larger than the right bound.\n        boolean inverted = false;\n\n        // Keep finding better approximations.\n        while (true) {\n            // Calculate the next approximation.\n            final double x = x1 - ((f1 * (x1 - x0)) / (f1 - f0));\n            final double fx = computeObjectiveValue(x);\n\n            // If the new approximation is the exact root, return it. Since\n            // this is not an under-approximation or an over-approximation,\n            // we can return it regardless of the allowed solutions.\n            if (fx == 0.0) {\n                return x;\n            }\n\n            // Update the bounds with the new approximation.\n            if (f1 * fx < 0) {\n                // The value of x1 has switched to the other bound, thus inverting\n                // the interval.\n                x0 = x1;\n                f0 = f1;\n                inverted = !inverted;\n            } else {\n                switch (method) {\n                case ILLINOIS:\n                    f0 *= 0.5;\n                    break;\n                case PEGASUS:\n                    f0 *= f1 / (f1 + fx);\n                    break;\n                case REGULA_FALSI:\n                    // Nothing.\n                    if (x == x1) {\n                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));\n                        f0 = computeObjectiveValue(x0);\n                    }\n                    break;\n                default:\n                    // Should never happen.\n                    throw new MathInternalError();\n                }\n            }\n            // Update from [x0, x1] to [x0, x].\n            x1 = x;\n            f1 = fx;\n\n            // If the function value of the last approximation is too small,\n            // given the function value accuracy, then we can't get closer to\n            // the root than we already are.\n            if (FastMath.abs(f1) <= ftol) {\n                switch (allowed) {\n                case ANY_SIDE:\n                    return x1;\n                case LEFT_SIDE:\n                    if (inverted) {\n                        return x1;\n                    }\n                    break;\n                case RIGHT_SIDE:\n                    if (!inverted) {\n                        return x1;\n                    }\n                    break;\n                case BELOW_SIDE:\n                    if (f1 <= 0) {\n                        return x1;\n                    }\n                    break;\n                case ABOVE_SIDE:\n                    if (f1 >= 0) {\n                        return x1;\n                    }\n                    break;\n                default:\n                    throw new MathInternalError();\n                }\n            }\n\n            // If the current interval is within the given accuracies, we\n            // are satisfied with the current approximation.\n            if (FastMath.abs(x1 - x0) < FastMath.max(rtol * FastMath.abs(x1),\n                                                     atol)) {\n                switch (allowed) {\n                case ANY_SIDE:\n                    return x1;\n                case LEFT_SIDE:\n                    return inverted ? x1 : x0;\n                case RIGHT_SIDE:\n                    return inverted ? x0 : x1;\n                case BELOW_SIDE:\n                    return (f1 <= 0) ? x1 : x0;\n                case ABOVE_SIDE:\n                    return (f1 >= 0) ? x1 : x0;\n                default:\n                    throw new MathInternalError();\n                }\n            }\n        }\n    }",
      "func_class": "BaseSecantSolver",
      "func_class_fq": "org.apache.commons.math.analysis.solvers.BaseSecantSolver",
      "func_param_types": [],
      "fi_name": "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "overload_types": [
        []
      ],
      "xrefs": [],
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMin()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMax()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].verifyBracketing(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getFunctionValueAccuracy()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getAbsoluteAccuracy()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getRelativeAccuracy()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.exception.MathInternalError].<init>()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.exception.MathInternalError].<init>()"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
        ],
        [
          "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
          "[org.apache.commons.math.exception.MathInternalError].<init>()"
        ],
        [
          "[org.apache.commons.math.exception.MathInternalError].<init>()",
          "[org.apache.commons.math.exception.MathInternalError].getContext()"
        ],
        [
          "[org.apache.commons.math.exception.MathInternalError].<init>()",
          "[org.apache.commons.math.exception.MathInternalError].addMessage(org.apache.commons.math.exception.MathInternalError,String)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()": 0,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMin()": 1,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMax()": 1,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].verifyBracketing(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getFunctionValueAccuracy()": 1,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getAbsoluteAccuracy()": 1,
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getRelativeAccuracy()": 1,
        "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
        "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
        "[org.apache.commons.math.exception.MathInternalError].<init>()": 1,
        "[org.apache.commons.math.exception.MathInternalError].getContext()": 2,
        "[org.apache.commons.math.exception.MathInternalError].addMessage(org.apache.commons.math.exception.MathInternalError,String)": 2
      },
      "reachable": [
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMin()",
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMax()",
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)",
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].verifyBracketing(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)",
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getFunctionValueAccuracy()",
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getAbsoluteAccuracy()",
        "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getRelativeAccuracy()",
        "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)",
        "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)",
        "[org.apache.commons.math.exception.MathInternalError].<init>()",
        "[org.apache.commons.math.exception.MathInternalError].getContext()",
        "[org.apache.commons.math.exception.MathInternalError].addMessage(org.apache.commons.math.exception.MathInternalError,String)",
        "[org.apache.commons.math.analysis.solvers.BaseAbstractUnivariateRealSolver].computeObjectiveValue(double)",
        "[org.apache.commons.math.analysis.integration.UnivariateRealIntegrator].getAbsoluteAccuracy()",
        "[org.apache.commons.math.analysis.solvers.BaseAbstractUnivariateRealSolver].getFunctionValueAccuracy()",
        "[org.apache.commons.math.analysis.solvers.BaseAbstractUnivariateRealSolver].getMax()",
        "[org.apache.commons.math.analysis.solvers.BaseAbstractUnivariateRealSolver].getMin()",
        "[org.apache.commons.math.analysis.integration.UnivariateRealIntegrator].getRelativeAccuracy()",
        "[org.apache.commons.math.analysis.solvers.BaseAbstractUnivariateRealSolver].verifyBracketing(double,double)"
      ],
      "related_callees": [
        {
          "name": "getMin",
          "source_file": "BaseAbstractUnivariateRealSolver.java",
          "signature": "public double getMin()",
          "source": "    public double getMin() {\n        return searchMin;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getMax",
          "source_file": "BaseAbstractUnivariateRealSolver.java",
          "signature": "public double getMax()",
          "source": "    public double getMax() {\n        return searchMax;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "computeObjectiveValue",
          "source_file": "BaseAbstractUnivariateRealSolver.java",
          "signature": "protected double computeObjectiveValue(double point) throws TooManyEvaluationsException",
          "source": "    protected double computeObjectiveValue(double point)\n        throws TooManyEvaluationsException {\n        incrementEvaluationCount();\n        return function.value(point);\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "verifyBracketing",
          "source_file": "BaseAbstractUnivariateRealSolver.java",
          "signature": "protected void verifyBracketing(double lower, double upper)",
          "source": "    protected void verifyBracketing(final double lower,\n                                    final double upper) {\n        UnivariateRealSolverUtils.verifyBracketing(function, lower, upper);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "BaseAbstractUnivariateRealSolver.java",
              "    protected void verifyBracketing(final double lower,\n                                    final double upper) {\n        UnivariateRealSolverUtils.verifyBracketing(function, lower, upper);\n    }"
            ],
            [
              "UnivariateRealSolverUtils.java",
              "    public static void verifyBracketing(UnivariateRealFunction function,\n                                        final double lower,\n                                        final double upper) {\n        if (function == null) {\n            throw new NullArgumentException(LocalizedFormats.FUNCTION);\n        }\n        verifyInterval(lower, upper);\n        if (!isBracketing(function, lower, upper)) {\n            throw new NoBracketingException(lower, upper,\n                                            function.value(lower),\n                                            function.value(upper));\n        }\n    }"
            ]
          ]
        },
        {
          "name": "getFunctionValueAccuracy",
          "source_file": "BaseUnivariateRealSolver.java",
          "signature": "double getFunctionValueAccuracy()",
          "source": "    double getFunctionValueAccuracy();",
          "is_abstract": true,
          "impls": [
            [
              "BaseAbstractUnivariateRealSolver.java",
              "    public double getFunctionValueAccuracy() {\n        return functionValueAccuracy;\n    }"
            ]
          ]
        },
        {
          "name": "getAbsoluteAccuracy",
          "source_file": "BaseUnivariateRealSolver.java",
          "signature": "double getAbsoluteAccuracy()",
          "source": "    double getAbsoluteAccuracy();",
          "is_abstract": true,
          "impls": [
            [
              "BaseAbstractUnivariateRealSolver.java",
              "    public double getAbsoluteAccuracy() {\n        return absoluteAccuracy;\n    }"
            ]
          ]
        }
      ],
      "field_siblings": [
        {
          "name": "BaseSecantSolver",
          "signature": "protected BaseSecantSolver(double absoluteAccuracy, Method method)",
          "shared_fields": [
            "allowed",
            "method"
          ],
          "is_constructor": true,
          "source": "    protected BaseSecantSolver(final double absoluteAccuracy, final Method method) {\n        super(absoluteAccuracy);\n        this.allowed = AllowedSolution.ANY_SIDE;\n        this.method = method;\n    }",
          "javadoc": "Construct a solver. @param absoluteAccuracy absolute accuracy @param method <em>Secant</em>-based root-finding method to use"
        },
        {
          "name": "BaseSecantSolver",
          "signature": "protected BaseSecantSolver(double relativeAccuracy, double absoluteAccuracy, Method method)",
          "shared_fields": [
            "allowed",
            "method"
          ],
          "is_constructor": true,
          "source": "    protected BaseSecantSolver(final double relativeAccuracy,\n                               final double absoluteAccuracy,\n                               final Method method) {\n        super(relativeAccuracy, absoluteAccuracy);\n        this.allowed = AllowedSolution.ANY_SIDE;\n        this.method = method;\n    }",
          "javadoc": "Construct a solver. @param relativeAccuracy relative accuracy @param absoluteAccuracy absolute accuracy @param method <em>Secant</em>-based root-finding method to use"
        },
        {
          "name": "BaseSecantSolver",
          "signature": "protected BaseSecantSolver(double relativeAccuracy, double absoluteAccuracy, double functionValueAccuracy, Method method)",
          "shared_fields": [
            "allowed",
            "method"
          ],
          "is_constructor": true,
          "source": "    protected BaseSecantSolver(final double relativeAccuracy,\n                               final double absoluteAccuracy,\n                               final double functionValueAccuracy,\n                               final Method method) {\n        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);\n        this.allowed = AllowedSolution.ANY_SIDE;\n        this.method = method;\n    }",
          "javadoc": "Construct a solver. @param relativeAccuracy Maximum relative error. @param absoluteAccuracy Maximum absolute error. @param functionValueAccuracy Maximum function value error. @param method <em>Secant</em>-based root-finding method to use"
        },
        {
          "name": "solve",
          "signature": "public double solve(int maxEval, UnivariateRealFunction f, double min, double max, double startValue, AllowedSolution allowedSolution)",
          "shared_fields": [
            "allowed"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "{@inheritDoc}"
        }
      ]
    }
  ],
  "package": "org.apache.commons.math.analysis.solvers",
  "root_cause_reachable": [
    "BaseSecantSolver.getMin",
    "BaseSecantSolver.getMax",
    "BaseSecantSolver.computeObjectiveValue",
    "BaseSecantSolver.verifyBracketing",
    "BaseSecantSolver.getFunctionValueAccuracy",
    "BaseSecantSolver.getAbsoluteAccuracy",
    "BaseSecantSolver.getRelativeAccuracy",
    "FastMath.abs",
    "FastMath.max",
    "MathInternalError.<init>",
    "MathInternalError.getContext",
    "MathInternalError.addMessage",
    "BaseAbstractUnivariateRealSolver.computeObjectiveValue",
    "UnivariateRealIntegrator.getAbsoluteAccuracy",
    "BaseAbstractUnivariateRealSolver.getFunctionValueAccuracy",
    "BaseAbstractUnivariateRealSolver.getMax",
    "BaseAbstractUnivariateRealSolver.getMin",
    "UnivariateRealIntegrator.getRelativeAccuracy",
    "BaseAbstractUnivariateRealSolver.verifyBracketing"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import org.apache.commons.math.util.FastMath;",
    "import org.apache.commons.math.analysis.UnivariateRealFunction;",
    "import org.apache.commons.math.exception.MathInternalError;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMin()"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMax()"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].verifyBracketing(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getFunctionValueAccuracy()"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getAbsoluteAccuracy()"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getRelativeAccuracy()"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)"
    ],
    [
      "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()",
      "[org.apache.commons.math.exception.MathInternalError].<init>()"
    ],
    [
      "[org.apache.commons.math.exception.MathInternalError].<init>()",
      "[org.apache.commons.math.exception.MathInternalError].getContext()"
    ],
    [
      "[org.apache.commons.math.exception.MathInternalError].<init>()",
      "[org.apache.commons.math.exception.MathInternalError].addMessage(org.apache.commons.math.exception.MathInternalError,String)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].doSolve()": 0,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMin()": 1,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getMax()": 1,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].computeObjectiveValue(org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].verifyBracketing(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getFunctionValueAccuracy()": 1,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getAbsoluteAccuracy()": 1,
    "[org.apache.commons.math.analysis.solvers.BaseSecantSolver].getRelativeAccuracy()": 1,
    "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
    "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.analysis.solvers.BaseSecantSolver,org.apache.commons.math.analysis.solvers.BaseSecantSolver)": 1,
    "[org.apache.commons.math.exception.MathInternalError].<init>()": 1,
    "[org.apache.commons.math.exception.MathInternalError].getContext()": 2,
    "[org.apache.commons.math.exception.MathInternalError].addMessage(org.apache.commons.math.exception.MathInternalError,String)": 2
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
**output:** **subtypes counted: ['IllinoisSolver', 'PegasusSolver', 'RegulaFalsiSolver']**
- detail: {'patched_classes': ['BaseSecantSolver'], 'subtypes': ['IllinoisSolver', 'PegasusSolver', 'RegulaFalsiSolver']}

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~52,728 chars)</summary>

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
// org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest::testIssue631
    @Test(expected=TooManyEvaluationsException.class)
    public void testIssue631() {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
                /** {@inheritDoc} */
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

        final UnivariateRealSolver solver = new RegulaFalsiSolver();
        final double root = solver.solve(3624, f, 1, 10);
        Assert.assertEquals(3.4341896575482003, root, 1e-15);
	}
// values this test asserts as CORRECT (the patched code must reproduce these): 3.4341896575482003
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest::testIssue631
// junit.framework.AssertionFailedError: Expected exception: org.apache.commons.math.exception.TooManyEvaluationsException
// The test DEPENDS on this setup from its test class (helpers, fields, fixtures). A relation that reconstructs the test's scenario must REPLICATE this setup exactly — a relation that cannot must assert only setup-independent properties (counts, kinds, contract formulas), or it will fire on every build including correct ones:
// --- helper value() ---
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max,
                        final AllowedSolution allowedSolution) / (final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max, final double startValue,
                        final AllowedSolution allowedSolution) / (final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max, final double startValue)
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    protected final double doSolve() {
        // Get initial solution
        double x0 = getMin();
        double x1 = getMax();
        double f0 = computeObjectiveValue(x0);
        double f1 = computeObjectiveValue(x1);

        // If one of the bounds is the exact root, return it. Since these are
        // not under-approximations or over-approximations, we can return them
        // regardless of the allowed solutions.
        if (f0 == 0.0) {
            return x0;
        }
        if (f1 == 0.0) {
            return x1;
        }

        // Verify bracketing of initial solution.
        verifyBracketing(x0, x1);

        // Get accuracies.
        final double ftol = getFunctionValueAccuracy();
        final double atol = getAbsoluteAccuracy();
        final double rtol = getRelativeAccuracy();

        // Keep track of inverted intervals, meaning that the left bound is
        // larger than the right bound.
        boolean inverted = false;

        // Keep finding better approximations.
        while (true) {
            // Calculate the next approximation.
            final double x = x1 - ((f1 * (x1 - x0)) / (f1 - f0));
            final double fx = computeObjectiveValue(x);

            // If the new approximation is the exact root, return it. Since
            // this is not an under-approximation or an over-approximation,
            // we can return it regardless of the allowed solutions.
            if (fx == 0.0) {
                return x;
            }

            // Update the bounds with the new approximation.
            if (f1 * fx < 0) {
                // The value of x1 has switched to the other bound, thus inverting
                // the interval.
                x0 = x1;
                f0 = f1;
                inverted = !inverted;
            } else {
                switch (method) {
                case ILLINOIS:
                    f0 *= 0.5;
                    break;
                case PEGASUS:
                    f0 *= f1 / (f1 + fx);
                    break;
                case REGULA_FALSI:
                    // Nothing.
                    if (x == x1) {
                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
                        f0 = computeObjectiveValue(x0);
                    }
                    break;
                default:
                    // Should never happen.
                    throw new MathInternalError();
                }
            }
            // Update from [x0, x1] to [x0, x].
            x1 = x;
            f1 = fx;

            // If the function value of the last approximation is too small,
            // given the function value accuracy, then we can't get closer to
            // the root than we already are.
            if (FastMath.abs(f1) <= ftol) {
                switch (allowed) {
                case ANY_SIDE:
                    return x1;
                case LEFT_SIDE:
                    if (inverted) {
                        return x1;
                    }
                    break;
                case RIGHT_SIDE:
                    if (!inverted) {
                        return x1;
                    }
                    break;
                case BELOW_SIDE:
                    if (f1 <= 0) {
                        return x1;
                    }
                    break;
                case ABOVE_SIDE:
                    if (f1 >= 0) {
                        return x1;
                    }
                    break;
                default:
                    throw new MathInternalError();
                }
            }

            // If the current interval is within the given accuracies, we
            // are satisfied with the current approximation.
            if (FastMath.abs(x1 - x0) < FastMath.max(rtol * FastMath.abs(x1),
                                                     atol)) {
                switch (allowed) {
                case ANY_SIDE:
                    return x1;
                case LEFT_SIDE:
                    return inverted ? x1 : x0;
                case RIGHT_SIDE:
                    return inverted ? x0 : x1;
                case BELOW_SIDE:
                    return (f1 <= 0) ? x1 : x0;
                case ABOVE_SIDE:
                    return (f1 >= 0) ? x1 : x0;
                default:
                    throw new MathInternalError();
                }
            }
        }
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="BaseSecantSolver" role="patched">
     * Base class for all bracketing <em>Secant</em>-based methods for root-finding
     * (approximating a zero of a univariate real function).
     * <p>Implementation of the {@link RegulaFalsiSolver <em>Regula Falsi</em>} and
     * {@link IllinoisSolver <em>Illinois</em>} methods is based on the
     * following article: M. Dowell and P. Jarratt,
     * <em>A modified regula falsi method for computing the root of an
     * equation</em>, BIT Numerical Mathematics, volume 11, number 2,
     * pages 168-174, Springer, 1971.</p>
     * <p>Implementation of the {@link PegasusSolver <em>Pegasus</em>} method is
     * based on the following article: M. Dowell and P. Jarratt,
     * <em>The "Pegasus" method for computing the root of an equation</em>,
     * BIT Numerical Mathematics, volume 12, number 4, pages 503-508, Springer,
     * …
public abstract class BaseSecantSolver
    extends AbstractUnivariateRealSolver
    implements BracketedUnivariateRealSolver<UnivariateRealFunction> {
     * Default absolute accuracy.
    protected static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;
     * The kinds of solutions that the algorithm may accept.
    private AllowedSolution allowed;
     * The <em>Secant</em>-based root-finding method to use.
    private final Method method;
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
     * @param method <em>Secant</em>-based root-finding method to use
    protected BaseSecantSolver(final double absoluteAccuracy, final Method method) {
        super(absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param method <em>Secant</em>-based root-finding method to use
    protected BaseSecantSolver(final double relativeAccuracy, final double absoluteAccuracy, final Method method) {
        super(relativeAccuracy, absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
     * Construct a solver.
     * @param relativeAccuracy Maximum relative error.
     * @param absoluteAccuracy Maximum absolute error.
     * @param functionValueAccuracy Maximum function value error.
     * @param method <em>Secant</em>-based root-finding method to use
    protected BaseSecantSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy, final Method method) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
     * {@inheritDoc}
    public double solve(final int maxEval, final UnivariateRealFunction f, final double min, final double max, final AllowedSolution allowedSolution) { … }
     * {@inheritDoc}
    public double solve(final int maxEval, final UnivariateRealFunction f, final double min, final double max, final double startValue, final AllowedSolution allowedSolution) { … }
     * {@inheritDoc}
    @Override public double solve(final int maxEval, final UnivariateRealFunction f, final double min, final double max, final double startValue) { … }
     * <em>Secant</em>-based root-finding methods.
    protected enum Method { … }
}
</class>
<class name="AbstractUnivariateRealSolver" role="supertype-contract">
     * Base class for solvers.
     * @version $Id$
     * @since 3.0
public abstract class AbstractUnivariateRealSolver
    extends BaseAbstractUnivariateRealSolver<UnivariateRealFunction>
    implements UnivariateRealSolver {
     * Construct a solver with given absolute accuracy.
     * @param absoluteAccuracy Maximum absolute error.
    protected AbstractUnivariateRealSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }
     * Construct a solver with given accuracies.
     * @param relativeAccuracy Maximum relative error.
     * @param absoluteAccuracy Maximum absolute error.
    protected AbstractUnivariateRealSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }
     * Construct a solver with given accuracies.
     * @param relativeAccuracy Maximum relative error.
     * @param absoluteAccuracy Maximum absolute error.
     * @param functionValueAccuracy Maximum function value error.
    protected AbstractUnivariateRealSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
    }
}
</class>
<class name="BracketedUnivariateRealSolver" role="supertype-contract">
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math.analysis.solvers;

import org.apache.commons.math.analysis.UnivariateRealFunction;

/** Interface for {@link UnivariateRealSolver (univariate real) root-finding
 * algorithms} that maintain a bracketed solution. There are several advantages
 * to having such root-finding algorithms:
 * <ul>
 *  <li>The bracketed solution guarantees that the root is kept within the
 *      interval. As such, these algorithms generally also guarantee
 *      convergence.</li>
 *  <li>The bracketed solution means that we have the opportunity to only
 *      return roots that are greater than or equal to the actual root, or
 *      are less than or equal to the actual root. That is, we can control
 *      whether under-approximations and over-approximations are
 *      {@link AllowedSolution allowed solutions}. Other root-finding
 *      algorithms can usually only guarantee that the solution (the root that
 *      was found) is around the actual root.</li>
 * </ul>
 *
 * <p>For backwards compatibility, all root-finding algorithms must have
 * {@link AllowedSolution#ANY_SIDE ANY_SIDE} as default for the allowed
 * solutions.</p>
 * @param <FUNC> Type of function to solve.
 *
 * @see AllowedSolution
 * @since 3.0
 * @version $Id$
 */
public interface BracketedUnivariateRealSolver<FUNC extends UnivariateRealFunction>
    extends BaseUnivariateRealSolver<FUNC> {

    /**
     * Solve for a zero in the given interval.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     *
     * @param maxEval Maximum number of evaluations.
     * @param f Function to solve.
     * @param min Lower bound for the interval.
     * @param max Upper bound for the interval.
     * @param allowedSolution The kind of solutions that the root-finding algorithm may
     * accept as solutions.
     * @return A value where the function is zero.
     * @throws org.apache.commons.math.exception.MathIllegalArgumentException
     * if the arguments do not satisfy the requirements specified by the solver.
     * @throws org.apache.commons.math.exception.TooManyEvaluationsException if
     * the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, FUNC f, double min, double max,
                 AllowedSolution allowedSolution);

    /**
     * Solve for a zero in the given interval, start at {@code startValue}.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     *
     * @param maxEval Maximum number of evaluations.
     * @param f Function to solve.
     * @param min Lower bound for the interval.
     * @param max Upper bound for the interval.
     * @param startValue Start value to use.
     * @param allowedSolution The kind of solutions that the root-finding algorithm may
     * accept as solutions.
     * @return A value where the function is zero.
     * @throws org.apache.commons.math.exception.MathIllegalArgumentException
     * if the arguments do not satisfy the requirements specified by the solver.
     * @throws org.apache.commons.math.exception.TooManyEvaluationsException if
     * the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, FUNC f, double min, double max, double startValue,
                 AllowedSolution allowedSolution);

}

</class>
<class name="UnivariateRealFunction" role="supertype-contract">
     * An interface representing a univariate real function.
     * @version $Id$
public interface UnivariateRealFunction {

}
</class>
<class name="RegulaFalsiSolver" role="test-subject">
     * Implements the <em>Regula Falsi</em> or <em>False position</em> method for
     * root-finding (approximating a zero of a univariate real function). It is a
     * modified {@link SecantSolver <em>Secant</em>} method. Unlike the
     * <em>Secant</em> method, convergence is guaranteed by maintaining a
     * bracketed solution.
     * <p>The <em>Regula Falsi</em> method assumes that the function is continuous,
     * but not necessarily smooth.</p>
     * <p>Implementation based on the following article: M. Dowell and P. Jarratt,
     * <em>A modified regula falsi method for computing the root of an
     * equation</em>, BIT Numerical Mathematics, volume 11, number 2,
     * pages 168-174, Springer, 1971.</p>
     * @since 3.0
     * …
public class RegulaFalsiSolver extends BaseSecantSolver {
     * Construct a solver with default accuracy (1e-6).
    public RegulaFalsiSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY, Method.REGULA_FALSI);
    }
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
    public RegulaFalsiSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy, Method.REGULA_FALSI);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
    public RegulaFalsiSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, Method.REGULA_FALSI);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param functionValueAccuracy Maximum function value error.
    public RegulaFalsiSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy, Method.REGULA_FALSI);
    }
}
</class>
<class name="TooManyEvaluationsException" role="test-subject">
     * Exception to be thrown when the maximal number of evaluations is exceeded.
     * @since 3.0
     * @version $Id$
public class TooManyEvaluationsException extends MaxCountExceededException {
     * Serializable version Id.
    private static final long serialVersionUID = 4330003017885151975L;
     * Construct the exception.
     * @param max Maximum number of evaluations.
    public TooManyEvaluationsException(Number max) {
        super(max);
        getContext().addMessage(LocalizedFormats.EVALUATIONS);
    }
}
</class>
<class name="UnivariateRealSolver" role="test-subject">
     * Interface for (univariate real) root-finding algorithms.
     * Implementations will search for only one zero in the given interval.
     * @version $Id$
public interface UnivariateRealSolver
    extends BaseUnivariateRealSolver<UnivariateRealFunction> {

}
</class>
<class name="FastMath" role="collaborator">
     * Faster, more accurate, portable alternative to {@link Math} and
     * {@link StrictMath} for large scale computation.
     * <p>
     * FastMath speed is achieved by relying heavily on optimizing compilers
     * to native code present in many JVM todays and use of large tables that
     * are computed once at class loading (regardless of the number of subsequent
     * calls to computation methods). This implies that FastMath is targeted
     * more towards large scale computation (i.e. computation that take at least
     * a handful of seconds to complete) on desktop or server machines rather
     * than very small utilities on devices with limited power (i.e. computation
     * that should return a result almost instantly). Note that FastMath is
     * extensively used inside Apache Commons Math, so by calling some algorithms,
     * …
public class FastMath {
     * Archimede's constant PI, ratio of circle circumference to diameter.
    public static final double PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9;
     * Napier's constant e, base of the natural logarithm.
    public static final double E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8;
     * Exponential evaluated at integer values,
     * exp(x) =  expIntTableA[x + 750] + expIntTableB[x+750].
    private static final double EXP_INT_TABLE_A[] = new double[1500];
     * Exponential evaluated at integer values,
     * exp(x) =  expIntTableA[x + 750] + expIntTableB[x+750]
    private static final double EXP_INT_TABLE_B[] = new double[1500];
     * Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
    private static final double EXP_FRAC_TABLE_A[] = new double[1025];
     * Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
    private static final double EXP_FRAC_TABLE_B[] = new double[1025];
     * Factorial table, for Taylor series expansions.
    private static final double FACT[] = new double[20];
     * Extended precision logarithm table over the range 1 - 2 in increments of 2^-10.
    private static final double LN_MANT[][] = new double[1024][];
     * log(2) (high bits).
    private static final double LN_2_A = 0.693147063255310059;
     * log(2) (low bits).
    private static final double LN_2_B = 1.17304635250823482e-7;
     * Coefficients for slowLog.
    private static final double LN_SPLIT_COEF[][] = { … }
     * Coefficients for log, when input 0.99 < x < 1.01.
    private static final double LN_QUICK_COEF[][] = { … }
     * Coefficients for log in the range of 1.0 < x < 1.0 + 2^-10.
    private static final double LN_HI_PREC_COEF[][] = { … }
     * Sine table (high bits).
    private static final double SINE_TABLE_A[] = new double[14];
     * Sine table (low bits).
    private static final double SINE_TABLE_B[] = new double[14];
     * Cosine table (high bits).
    private static final double COSINE_TABLE_A[] = new double[14];
     * Cosine table (low bits).
    private static final double COSINE_TABLE_B[] = new double[14];
     * Tangent table, used by atan() (high bits).
    private static final double TANGENT_TABLE_A[] = new double[14];
     * Tangent table, used by atan() (low bits).
    private static final double TANGENT_TABLE_B[] = new double[14];
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
     * Private Constructor
    private FastMath() {
    }
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     * @param d the value to split
     * @return the high order part of the mantissa
    private static double doubleHighPart(double d) { … }
     * For x between 0 and 1, returns exp(x), uses extended precision
     * @param x argument of exponential
     * @param result placeholder where to place exp(x) split in two terms
     * for extra precision (i.e. exp(x) = result[0] ° result[1]
     * @return exp(x)
    private static double slowexp(final double x, final double result[]) { … }
     * Compute split[0], split[1] such that their sum is equal to d,
     * and split[0] has its 30 least significant bits as zero.
     * @param d number to split
     * @param split placeholder where to place the result
    private static void split(final double d, final double split[]) { … }
     * Recompute a split.
     * @param a input/out array containing the split, changed
     * on output
    private static void resplit(final double a[]) { … }
     * Multiply two numbers in split form.
     * @param a first term of multiplication
     * @param b second term of multiplication
     * @param ans placeholder where to put the result
    private static void splitMult(double a[], double b[], double ans[]) { … }
     * Add two numbers in split form.
     * @param a first term of addition
     * @param b second term of addition
     * @param ans placeholder where to put the result
    private static void splitAdd(final double a[], final double b[], final double ans[]) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="AllowedSolution" role="collaborator">
     * The kinds of solutions that a {@link BracketedUnivariateRealSolver
     * (bracketed univariate real) root-finding algorithm} may accept as solutions.
     * This basically controls whether or not under-approximations and
     * over-approximations are allowed.
     * <p>If all solutions are accepted ({@link #ANY_SIDE}), then the solution
     * that the root-finding algorithm returns for a given root may be equal to the
     * actual root, but it may also be an approximation that is slightly smaller
     * or slightly larger than the actual root. Root-finding algorithms generally
     * only guarantee that the returned solution is within the requested
     * tolerances. In certain cases however, in particular for
     * {@link org.apache.commons.math.ode.events.EventHandler state events} of
     * {@link org.apache.commons.math.ode.ODEIntegrator ODE solvers}, it
     * …
public enum AllowedSolution {

}
</class>
<class name="MathInternalError" role="collaborator">
     * Exception triggered when something that shouldn't happen does happen.
     * @since 2.2
     * @version $Id$
public class MathInternalError extends MathIllegalStateException {
     * Serializable version Id.
    private static final long serialVersionUID = -6276776513966934846L;
     * URL for reporting problems.
    private static final String REPORT_URL = "https://issues.apache.org/jira/browse/MATH";
     * Simple constructor.
    public MathInternalError() {
        getContext().addMessage(LocalizedFormats.INTERNAL_ERROR, REPORT_URL);
    }
     * Simple constructor.
     * @param cause root cause
    public MathInternalError(final Throwable cause) {
        super(cause, LocalizedFormats.INTERNAL_ERROR, REPORT_URL);
    }
}
</class>
<class name="IllinoisSolver" role="collaborator">
     * Implements the <em>Illinois</em> method for root-finding (approximating
     * a zero of a univariate real function). It is a modified
     * {@link RegulaFalsiSolver <em>Regula Falsi</em>} method.
     * <p>Like the <em>Regula Falsi</em> method, convergence is guaranteed by
     * maintaining a bracketed solution. The <em>Illinois</em> method however,
     * should converge much faster than the original <em>Regula Falsi</em>
     * method.</p>
     * <p>The <em>Illinois</em> method assumes that the function is continuous,
     * but not necessarily smooth.</p>
     * <p>Implementation based on the following article: M. Dowell and P. Jarratt,
     * <em>A modified regula falsi method for computing the root of an
     * equation</em>, BIT Numerical Mathematics, volume 11, number 2,
     * …
public class IllinoisSolver extends BaseSecantSolver {
     * Construct a solver with default accuracy (1e-6).
    public IllinoisSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY, Method.ILLINOIS);
    }
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
    public IllinoisSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy, Method.ILLINOIS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
    public IllinoisSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, Method.ILLINOIS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param functionValueAccuracy Maximum function value error.
    public IllinoisSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy, Method.PEGASUS);
    }
}
</class>
<class name="PegasusSolver" role="collaborator">
     * Implements the <em>Pegasus</em> method for root-finding (approximating
     * a zero of a univariate real function). It is a modified
     * {@link RegulaFalsiSolver <em>Regula Falsi</em>} method.
     * <p>Like the <em>Regula Falsi</em> method, convergence is guaranteed by
     * maintaining a bracketed solution. The <em>Pegasus</em> method however,
     * should converge much faster than the original <em>Regula Falsi</em> method.
     * Furthermore, it should converge faster than the
     * {@link IllinoisSolver <em>Illinois</em>} method, another
     * <em>Regula Falsi</em>-based method.</p>
     * <p>The <em>Pegasus</em> method assumes that the function is continuous,
     * but not necessarily smooth.</p>
     * <p>Implementation based on the following article: M. Dowell and P. Jarratt,
     * …
public class PegasusSolver extends BaseSecantSolver {
     * Construct a solver with default accuracy (1e-6).
    public PegasusSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY, Method.PEGASUS);
    }
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
    public PegasusSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy, Method.PEGASUS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
    public PegasusSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, Method.PEGASUS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param functionValueAccuracy Maximum function value error.
    public PegasusSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy, Method.PEGASUS);
    }
}
</class>
THE PATCH-CHANGED CLASS: BaseSecantSolver. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
@@ -184,10 +184,6 @@
                     break;
                 case REGULA_FALSI:
                     // Nothing.
-                    if (x == x1) {
-                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
-                        f0 = computeObjectiveValue(x0);
-                    }
                     break;
                 default:
                     // Should never happen.

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    REMOVED: if (x == x1) {
    REMOVED: x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
    REMOVED: f0 = computeObjectiveValue(x0);
    REMOVED: }
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
Base class for all bracketing <em>Secant</em>-based methods for root-finding
(approximating a zero of a univariate real function).

<p>Implementation of the {@link RegulaFalsiSolver <em>Regula Falsi</em>} and
{@link IllinoisSolver <em>Illinois</em>} methods is based on the
following article: M. Dowell and P. Jarratt,
<em>A modified regula falsi method for computing the root of an
equation</em>, BIT Numerical Mathematics, volume 11, number 2,
pages 168-174, Springer, 1971.</p>

<p>Implementation of the {@link PegasusSolver <em>Pegasus</em>} method is
based on the following article: M. Dowell and P. Jarratt,
<em>The "Pegasus" method for computing the root of an equation</em>,
BIT Numerical Mathematics, volume 12, number 4, pages 503-508, Springer,
1972.</p>

<p>The {@link SecantSolver <em>Secant</em>} method is <em>not</em> a
bracketing method, so it is not implemented here. It has a separate
implementation.</p>

@since 3.0
@version $Id$
/
public abstract class BaseSecantSolver
extends AbstractUnivariateRealSolver
implements BracketedUnivariateRealSolver<UnivariateRealFunction> {

/** Default absolute accuracy. */
protected static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

/** The kinds of solutions that the algorithm may accept. */
private AllowedSolution allowed;

/** The <em>Secant</em>-based root-finding method to use. */
private final Method method;

/**
Construct a solver.

@param absoluteAccuracy absolute accuracy
@param method <em>Secant</em>-based root-finding met
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.MathInternalError;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    exp, pow, solve, UnivariateRealFunction, RegulaFalsiSolver

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
<details open><summary>▸ Output (~9,425 chars)</summary>

```
[
  {
    "name": "regulafalsi_issue631_must_throw_too_many_evaluations",
    "kind": "invariant",
    "contract": "BracketedUnivariateRealSolver.solve(...) declares `@throws TooManyEvaluationsException if the allowed number of evaluations is exceeded`; the bug's failing test fixes the correct behavior for RegulaFalsiSolver on `f(x)=exp(x)-pi^3`, interval `[1,10]`, and `maxEval=3624`: it must throw `org.apache.commons.math.exception.TooManyEvaluationsException` rather than return normally.",
    "input": "Recreate the failing-test setup exactly: `RegulaFalsiSolver`, `f(x)=Math.exp(x)-Math.pow(Math.PI,3.0)`, `min=1`, `max=10`, `maxEval=3624`. Use a zero-width fuzz draw only to satisfy the harness while keeping the ground-truth input fixed.",
    "check": "int maxEval;\norg.apache.commons.math.analysis.UnivariateRealFunction f;\norg.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;\ntry {\n  maxEval = 3624 + data.consumeInt(0, 0);\n  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n      return Math.exp(x) - Math.pow(Math.PI, 3.0);\n    }\n  };\n  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\n} catch (Exception e) {\n  return;\n}\nboolean violated = false;\nString msg = \"completed normally\";\ntry {\n  solver.solve(maxEval, f, 1.0, 10.0);\n  violated = true;\n} catch (org.apache.commons.math.exception.TooManyEvaluationsException ok) {\n  // expected\n} catch (Throwable t) {\n  violated = true;\n  msg = \"threw wrong exception \" + t.getClass().getName() + \": \" + t.getMessage();\n}\nif (violated) {\n  throw new RuntimeException(\"relation regulafalsi_issue631_must_throw_too_many_evaluations violated: \" + msg);\n}"
  },
  {
    "name": "default_any_side_matches_explicit_any_side",
    "kind": "metamorphic",
    "contract": "BracketedUnivariateRealSolver documentation states: \"For backwards compatibility, all root-finding algorithms must have ANY_SIDE as default for the allowed solutions.\" Therefore `solve(maxEval,f,min,max)` and `solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)` must agree on the same valid problem.",
    "input": "Use a function with a unique root exactly at one endpoint so the returned zero is forced and independent of iteration details: choose `c` from fuzz data, let `f(x)=x-c`, and choose either `[c, c+w]` or `[c-w, c]` with positive integer width `w` from fuzz data.",
    "check": "org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;\norg.apache.commons.math.analysis.UnivariateRealFunction f;\ndouble c;\ndouble min;\ndouble max;\nint w;\nboolean leftRoot;\ndouble r1;\ndouble r2;\ntry {\n  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\n  c = data.consumeInt(-1000, 1000);\n  w = data.consumeInt(1, 1000);\n  leftRoot = data.consumeBoolean();\n  min = leftRoot ? c : c - w;\n  max = leftRoot ? c + w : c;\n  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n      return x - c;\n    }\n  };\n} catch (Exception e) {\n  return;\n}\ntry {\n  r1 = solver.solve(100, f, min, max);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation default_any_side_matches_explicit_any_side violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  r2 = solver.solve(100, f, min, max, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation default_any_side_matches_explicit_any_side violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {\n  throw new RuntimeException(\"relation default_any_side_matches_explicit_any_side violated: \" + r1 + \" vs \" + r2);\n}"
  },
  {
    "name": "default_startvalue_any_side_matches_explicit_any_side",
    "kind": "metamorphic",
    "contract": "The overloads `solve(maxEval,f,min,max,startValue)` and `solve(maxEval,f,min,max,startValue,AllowedSolution)` have the same semantics except that the former uses the documented default `AllowedSolution.ANY_SIDE`; they must therefore agree when given the same valid interval and start value.",
    "input": "Again use `f(x)=x-c` with the unique root at an endpoint chosen from fuzz data, and choose a start value strictly inside the interval from fuzz data. Endpoint-root construction makes the zero unique and exact.",
    "check": "org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;\norg.apache.commons.math.analysis.UnivariateRealFunction f;\ndouble c;\ndouble min;\ndouble max;\ndouble start;\nint w;\nboolean leftRoot;\ndouble r1;\ndouble r2;\ntry {\n  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\n  c = data.consumeInt(-1000, 1000);\n  w = data.consumeInt(2, 1000);\n  leftRoot = data.consumeBoolean();\n  min = leftRoot ? c : c - w;\n  max = leftRoot ? c + w : c;\n  int offset = data.consumeInt(1, w - 1);\n  start = leftRoot ? c + offset : c - offset;\n  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n      return x - c;\n    }\n  };\n} catch (Exception e) {\n  return;\n}\ntry {\n  r1 = solver.solve(100, f, min, max, start);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation default_startvalue_any_side_matches_explicit_any_side violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  r2 = solver.solve(100, f, min, max, start, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation default_startvalue_any_side_matches_explicit_any_side violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {\n  throw new RuntimeException(\"relation default_startvalue_any_side_matches_explicit_any_side violated: \" + r1 + \" vs \" + r2);\n}"
  },
  {
    "name": "endpoint_root_left_is_returned_exactly",
    "kind": "invariant",
    "contract": "BracketedUnivariateRealSolver documentation says solvers that require bracketing \"should be able to handle the case where one of the endpoints is itself a root,\" and the method returns \"a value where the function is zero.\" For `f(x)=x-c` on `[c,c+w]`, the only zero in the interval is exactly `c`, so any correct implementation must return `c`.",
    "input": "Choose `c` and positive width `w` from fuzz data; define `f(x)=x-c`; call `solve(maxEval,f,c,c+w,AllowedSolution.ANY_SIDE)`.",
    "check": "org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;\norg.apache.commons.math.analysis.UnivariateRealFunction f;\ndouble c;\ndouble max;\ndouble r;\ntry {\n  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\n  c = data.consumeInt(-1000, 1000);\n  max = c + data.consumeInt(1, 1000);\n  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n      return x - c;\n    }\n  };\n} catch (Exception e) {\n  return;\n}\ntry {\n  r = solver.solve(100, f, c, max, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation endpoint_root_left_is_returned_exactly violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (Double.doubleToLongBits(r) != Double.doubleToLongBits(c)) {\n  throw new RuntimeException(\"relation endpoint_root_left_is_returned_exactly violated: expected \" + c + \" but got \" + r);\n}"
  },
  {
    "name": "endpoint_root_right_is_returned_exactly",
    "kind": "invariant",
    "contract": "BracketedUnivariateRealSolver documentation says solvers that require bracketing \"should be able to handle the case where one of the endpoints is itself a root,\" and the method returns \"a value where the function is zero.\" For `f(x)=x-c` on `[c-w,c]`, the only zero in the interval is exactly `c`, so any correct implementation must return `c`.",
    "input": "Choose `c` and positive width `w` from fuzz data; define `f(x)=x-c`; call `solve(maxEval,f,c-w,c,AllowedSolution.ANY_SIDE)`.",
    "check": "org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;\norg.apache.commons.math.analysis.UnivariateRealFunction f;\ndouble c;\ndouble min;\ndouble r;\ntry {\n  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\n  c = data.consumeInt(-1000, 1000);\n  min = c - data.consumeInt(1, 1000);\n  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n      return x - c;\n    }\n  };\n} catch (Exception e) {\n  return;\n}\ntry {\n  r = solver.solve(100, f, min, c, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation endpoint_root_right_is_returned_exactly violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (Double.doubleToLongBits(r) != Double.doubleToLongBits(c)) {\n  throw new RuntimeException(\"relation endpoint_root_right_is_returned_exactly violated: expected \" + c + \" but got \" + r);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen-fuzz-buggy · `regulafalsi_issue631_must_throw_too_many_evaluations`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [6] ⚙️ screen-fuzz-buggy · `default_any_side_matches_explicit_any_side`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [7] ⚙️ screen-fuzz-buggy · `default_startvalue_any_side_matches_explicit_any_side`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [8] ⚙️ screen-fuzz-buggy · `endpoint_root_left_is_returned_exactly`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen-fuzz-buggy · `endpoint_root_right_is_returned_exactly`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [10] ⚙️ screen · `regulafalsi_issue631_must_throw_too_many_evaluations`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [11] ⚙️ screen · `default_any_side_matches_explicit_any_side`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [12] ⚙️ screen · `default_startvalue_any_side_matches_explicit_any_side`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [13] ⚙️ screen · `endpoint_root_left_is_returned_exactly`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [14] ⚙️ screen · `endpoint_root_right_is_returned_exactly`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [15] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "regulafalsi_issue631_must_throw_too_many_evaluations",
    "default_any_side_matches_explicit_any_side",
    "default_startvalue_any_side_matches_explicit_any_side",
    "endpoint_root_left_is_returned_exactly",
    "endpoint_root_right_is_returned_exactly"
  ],
  "count": 5
}
```

---
## [16] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,255 chars)</summary>

```
[
  {
    "name": "issue631-must-throw-too-many-evals",
    "kind": "invariant",
    "contract": "BracketedUnivariateRealSolver.solve(...) documents `@throws TooManyEvaluationsException if the allowed number of evaluations is exceeded`; the trusted failing test fixes a concrete valid input for RegulaFalsiSolver on which correct behaviour is to throw that exception.",
    "input": "Recreate the exact Issue 631 scenario from the failing test: `f(x) = exp(x) - pi^3`, `min = 1`, `max = 10`, `maxEval = 3624`, using a real `RegulaFalsiSolver`. These inputs are valid by construction and match the ground-truth test.",
    "check": "int ignored = data.consumeInt(0, 0);\nfinal int maxEval = 3624 + ignored;\nfinal double min = 1.0;\nfinal double max = 10.0;\nfinal org.apache.commons.math.analysis.UnivariateRealFunction f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n        return Math.exp(x) - Math.pow(Math.PI, 3.0);\n    }\n};\nfinal org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\nboolean violated = false;\nString detail = \"completed normally\";\ntry {\n    solver.solve(maxEval, f, min, max);\n    violated = true;\n} catch (org.apache.commons.math.exception.TooManyEvaluationsException ok) {\n    // expected\n} catch (Throwable t) {\n    violated = true;\n    detail = \"threw wrong exception \" + t.getClass().getName() + \": \" + t.getMessage();\n}\nif (violated) {\n    throw new RuntimeException(\"relation issue631-must-throw-too-many-evals violated: \" + detail);\n}"
  },
  {
    "name": "startvalue-default-anyside-agreement-endpoint-root",
    "kind": "metamorphic",
    "contract": "BracketedUnivariateRealSolver states that ANY_SIDE is the default allowed solution for backwards compatibility; therefore `solve(maxEval,f,min,max,startValue)` must agree with `solve(maxEval,f,min,max,startValue,AllowedSolution.ANY_SIDE)` on the same valid input.",
    "input": "Choose an interval `[root, root + width]` with `width >= 1`, and define `f(x) = x - root` so the lower endpoint is an exact root. Pick any integer startValue inside the interval. Because an endpoint is itself a root, the solver must accept the input and both overloads solve the same problem.",
    "check": "final double root;\nfinal double min;\nfinal double max;\nfinal double startValue;\ntry {\n    int base = data.consumeInt(-1000, 1000);\n    int width = data.consumeInt(1, 1000);\n    int offset = data.consumeInt(0, width);\n    root = base;\n    min = root;\n    max = root + width;\n    startValue = root + offset;\n} catch (Exception e) {\n    return;\n}\nfinal org.apache.commons.math.analysis.UnivariateRealFunction f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n        return x - root;\n    }\n};\nfinal org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\ndouble r1;\ndouble r2;\ntry {\n    r1 = solver.solve(50, f, min, max, startValue);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation startvalue-default-anyside-agreement-endpoint-root violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    r2 = solver.solve(50, f, min, max, startValue, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation startvalue-default-anyside-agreement-endpoint-root violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (r1 != r2 || r1 != root) {\n    throw new RuntimeException(\"relation startvalue-default-anyside-agreement-endpoint-root violated: \" + r1 + \" vs \" + r2 + \", expected endpoint root \" + root);\n}"
  },
  {
    "name": "with-and-without-startvalue-agree-on-endpoint-root",
    "kind": "metamorphic",
    "contract": "Both `solve(maxEval,f,min,max,allowedSolution)` and `solve(maxEval,f,min,max,startValue,allowedSolution)` are documented as solving for a zero in the same interval; when one endpoint is itself a root, solvers should handle that case, so adding a start value must not change the returned exact endpoint root.",
    "input": "Choose `root` and `width >= 1`, let `[min,max] = [root, root + width]`, define `f(x)=x-root`, and choose any `startValue` in `[min,max]`. Use the same `AllowedSolution.ANY_SIDE` for both overloads.",
    "check": "final double root;\nfinal double min;\nfinal double max;\nfinal double startValue;\ntry {\n    int base = data.consumeInt(-1000, 1000);\n    int width = data.consumeInt(1, 1000);\n    int offset = data.consumeInt(0, width);\n    root = base;\n    min = root;\n    max = root + width;\n    startValue = root + offset;\n} catch (Exception e) {\n    return;\n}\nfinal org.apache.commons.math.analysis.UnivariateRealFunction f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n        return x - root;\n    }\n};\nfinal org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\ndouble r1;\ndouble r2;\ntry {\n    r1 = solver.solve(50, f, min, max, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation with-and-without-startvalue-agree-on-endpoint-root violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    r2 = solver.solve(50, f, min, max, startValue, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation with-and-without-startvalue-agree-on-endpoint-root violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (r1 != r2 || r1 != root) {\n    throw new RuntimeException(\"relation with-and-without-startvalue-agree-on-endpoint-root violated: \" + r1 + \" vs \" + r2 + \", expected endpoint root \" + root);\n}"
  },
  {
    "name": "successful-result-stays-within-bracket",
    "kind": "invariant",
    "contract": "BracketedUnivariateRealSolver documents that these algorithms maintain a bracketed solution and keep the root within the interval; therefore any successfully returned solution for a valid bracketed input must lie in `[min, max]`.",
    "input": "Choose an integer `root`, set `min = root - 1` and `max = root + 1`, and define `f(x) = x - root`, which is continuous and brackets a unique zero strictly inside the interval. This is a valid input by construction.",
    "check": "final double root;\nfinal double min;\nfinal double max;\ntry {\n    int base = data.consumeInt(-1000, 1000);\n    root = base;\n    min = root - 1.0;\n    max = root + 1.0;\n} catch (Exception e) {\n    return;\n}\nfinal org.apache.commons.math.analysis.UnivariateRealFunction f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n        return x - root;\n    }\n};\nfinal org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\ndouble r;\ntry {\n    r = solver.solve(50, f, min, max, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation successful-result-stays-within-bracket violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (!(r >= min && r <= max)) {\n    throw new RuntimeException(\"relation successful-result-stays-within-bracket violated: result \" + r + \" not in [\" + min + \", \" + max + \"]\");\n}"
  },
  {
    "name": "left-side-not-greater-than-right-side",
    "kind": "metamorphic",
    "contract": "AllowedSolution is documented to control whether under-approximations or over-approximations are accepted. For a strictly increasing function with a unique root, a LEFT_SIDE solution must be on or below the actual root and a RIGHT_SIDE solution must be on or above it, so the LEFT_SIDE result cannot exceed the RIGHT_SIDE result.",
    "input": "Use the increasing continuous function `f(x) = x*x - 2` on `[1,2]`, which brackets the unique positive root `sqrt(2)`. Call the same solver twice with identical `maxEval`, function, and interval, varying only `AllowedSolution` between LEFT_SIDE and RIGHT_SIDE.",
    "check": "final double min;\nfinal double max;\nfinal int maxEval;\ntry {\n    min = 1.0 + data.consumeInt(0, 0);\n    max = 2.0 + data.consumeInt(0, 0);\n    maxEval = 200 + data.consumeInt(0, 200);\n} catch (Exception e) {\n    return;\n}\nfinal org.apache.commons.math.analysis.UnivariateRealFunction f = new org.apache.commons.math.analysis.UnivariateRealFunction() {\n    public double value(double x) {\n        return x * x - 2.0;\n    }\n};\nfinal org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();\ndouble left;\ndouble right;\ntry {\n    left = solver.solve(maxEval, f, min, max, org.apache.commons.math.analysis.solvers.AllowedSolution.LEFT_SIDE);\n} catch (org.apache.commons.math.exception.TooManyEvaluationsException e) {\n    return;\n} catch (Exception e) {\n    throw new RuntimeException(\"relation left-side-not-greater-than-right-side violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    right = solver.solve(maxEval, f, min, max, org.apache.commons.math.analysis.solvers.AllowedSolution.RIGHT_SIDE);\n} catch (org.apache.commons.math.exception.TooManyEvaluationsException e) {\n    return;\n} catch (Exception e) {\n    throw new RuntimeException(\"relation left-side-not-greater-than-right-side violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (left > right) {\n    throw new RuntimeException(\"relation left-side-not-greater-than-right-side violated: LEFT_SIDE=\" + left + \" > RIGHT_SIDE=\" + right);\n}"
  }
]
```

</details>

---
## [17] ⚙️ screen-fuzz-buggy · `issue631-must-throw-too-many-evals`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [18] ⚙️ screen-fuzz-buggy · `startvalue-default-anyside-agreement-endpoint-root`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [19] ⚙️ screen-fuzz-buggy · `with-and-without-startvalue-agree-on-endpoint-root`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [20] ⚙️ screen-fuzz-buggy · `successful-result-stays-within-bracket`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [21] ⚙️ screen-fuzz-buggy · `left-side-not-greater-than-right-side`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [22] ⚙️ screen · `issue631-must-throw-too-many-evals`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [23] ⚙️ screen · `startvalue-default-anyside-agreement-endpoint-root`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [24] ⚙️ screen · `with-and-without-startvalue-agree-on-endpoint-root`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [25] ⚙️ screen · `successful-result-stays-within-bracket`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [26] ⚙️ screen · `left-side-not-greater-than-right-side`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [27] ⚙️ reference-widening · `enumerate`
**output:** **nothing to widen**
- reason: the kept checks name no no-argument reader of the patched class — nothing to request a reference for; 3 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['solve', 'regulafalsisolver', 'toomanyevaluationsexception'])
<details><summary>▸ detail (~763 chars)</summary>

- detail: {'targets': [], 'already_admitted': [], 'cap': 3, 'checks_read': 10, 'excluded': [{'key': 'solve', 'why': "the patched class declares `solve` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'regulafalsisolver', 'why': 'the patched class does not declare `RegulaFalsiSolver()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'toomanyevaluationsexception', 'why': 'the patched class does not declare `TooManyEvaluationsException()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 3}

</details>

---
## [28] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~32,745 chars)</summary>

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

Codebase: `Math_50_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
@@ -184,10 +184,6 @@
                     break;
                 case REGULA_FALSI:
                     // Nothing.
-                    if (x == x1) {
-                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
-                        f0 = computeObjectiveValue(x0);
-                    }
                     break;
                 default:
                     // Should never happen.

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.MathInternalError;
</source_imports>

Function `doSolve`:
<signature>
final protected double doSolve()
</signature>
<code>
    protected final double doSolve() {
        // Get initial solution
        double x0 = getMin();
        double x1 = getMax();
        double f0 = computeObjectiveValue(x0);
        double f1 = computeObjectiveValue(x1);

        // If one of the bounds is the exact root, return it. Since these are
        // not under-approximations or over-approximations, we can return them
        // regardless of the allowed solutions.
        if (f0 == 0.0) {
            return x0;
        }
        if (f1 == 0.0) {
            return x1;
        }

        // Verify bracketing of initial solution.
        verifyBracketing(x0, x1);

        // Get accuracies.
        final double ftol = getFunctionValueAccuracy();
        final double atol = getAbsoluteAccuracy();
        final double rtol = getRelativeAccuracy();

        // Keep track of inverted intervals, meaning that the left bound is
        // larger than the right bound.
        boolean inverted = false;

        // Keep finding better approximations.
        while (true) {
            // Calculate the next approximation.
            final double x = x1 - ((f1 * (x1 - x0)) / (f1 - f0));
            final double fx = computeObjectiveValue(x);

            // If the new approximation is the exact root, return it. Since
            // this is not an under-approximation or an over-approximation,
            // we can return it regardless of the allowed solutions.
            if (fx == 0.0) {
                return x;
            }

            // Update the bounds with the new approximation.
            if (f1 * fx < 0) {
                // The value of x1 has switched to the other bound, thus inverting
                // the interval.
                x0 = x1;
                f0 = f1;
                inverted = !inverted;
            } else {
                switch (method) {
                case ILLINOIS:
                    f0 *= 0.5;
                    break;
                case PEGASUS:
                    f0 *= f1 / (f1 + fx);
                    break;
                case REGULA_FALSI:
                    // Nothing.
                    if (x == x1) {
                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
                        f0 = computeObjectiveValue(x0);
                    }
                    break;
                default:
                    // Should never happen.
                    throw new MathInternalError();
                }
            }
            // Update from [x0, x1] to [x0, x].
            x1 = x;
            f1 = fx;

            // If the function value of the last approximation is too small,
            // given the function value accuracy, then we can't get closer to
            // the root than we already are.
            if (FastMath.abs(f1) <= ftol) {
                switch (allowed) {
                case ANY_SIDE:
                    return x1;
                case LEFT_SIDE:
                    if (inverted) {
                        return x1;
                    }
                    break;
                case RIGHT_SIDE:
                    if (!inverted) {
                        return x1;
                    }
                    break;
                case BELOW_SIDE:
                    if (f1 <= 0) {
                        return x1;
                    }
                    break;
                case ABOVE_SIDE:
                    if (f1 >= 0) {
                        return x1;
                    }
                    break;
                default:
                    throw new MathInternalError();
                }
            }

            // If the current interval is within the given accuracies, we
            // are satisfied with the current approximation.
            if (FastMath.abs(x1 - x0) < FastMath.max(rtol * FastMath.abs(x1),
                                                     atol)) {
                switch (allowed) {
                case ANY_SIDE:
                    return x1;
                case LEFT_SIDE:
                    return inverted ? x1 : x0;
                case RIGHT_SIDE:
                    return inverted ? x0 : x1;
                case BELOW_SIDE:
                    return (f1 <= 0) ? x1 : x0;
                case ABOVE_SIDE:
                    return (f1 >= 0) ? x1 : x0;
                default:
                    throw new MathInternalError();
                }
            }
        }
    }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `doSolve` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `doSolve`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `protected BaseSecantSolver(double absoluteAccuracy, Method method)` (shared field(s): allowed, method)
      doc: Construct a solver. @param absoluteAccuracy absolute accuracy @param method <em>Secant</em>-based root-finding method to use
    <constructor_body>
    protected BaseSecantSolver(final double absoluteAccuracy, final Method method) {
        super(absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
    </constructor_body>
  - `protected BaseSecantSolver(double relativeAccuracy, double absoluteAccuracy, Method method)` (shared field(s): allowed, method)
      doc: Construct a solver. @param relativeAccuracy relative accuracy @param absoluteAccuracy absolute accuracy @param method <em>Secant</em>-based root-finding method to use
    <constructor_body>
    protected BaseSecantSolver(final double relativeAccuracy,
                               final double absoluteAccuracy,
                               final Method method) {
        super(relativeAccuracy, absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
    </constructor_body>
  - `protected BaseSecantSolver(double relativeAccuracy, double absoluteAccuracy, double functionValueAccuracy, Method method)` (shared field(s): allowed, method)
      doc: Construct a solver. @param relativeAccuracy Maximum relative error. @param absoluteAccuracy Maximum absolute error. @param functionValueAccuracy Maximum function value error. @param method <em>Secant</em>-based root-finding method to use
    <constructor_body>
    protected BaseSecantSolver(final double relativeAccuracy,
                               final double absoluteAccuracy,
                               final double functionValueAccuracy,
                               final Method method) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
    </constructor_body>
  - `public double solve(int maxEval, UnivariateRealFunction f, double min, double max, double startValue, AllowedSolution allowedSolution)` (shared field(s): allowed)
      doc: {@inheritDoc}

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param absoluteAccuracy absolute accuracy
  - @param method <em>Secant</em>-based root-finding met
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max,
                        final AllowedSolution allowedSolution) / (final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max, final double startValue,
                        final AllowedSolution allowedSolution) / (final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max, final double startValue)

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `RegulaFalsiSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest" method="testIssue631">
    @Test(expected=TooManyEvaluationsException.class)
    public void testIssue631() {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
                /** {@inheritDoc} */
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

        final UnivariateRealSolver solver = new RegulaFalsiSolver();
        final double root = solver.solve(3624, f, 1, 10);
        Assert.assertEquals(3.4341896575482003, root, 1e-15);
	}
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest::testIssue631
junit.framework.AssertionFailedError: Expected exception: org.apache.commons.math.exception.TooManyEvaluationsException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest">
// --- helper value() ---
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
</test_support>
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
<relation name="regulafalsi_issue631_must_throw_too_many_evaluations" kind="invariant">
  // holds because: BracketedUnivariateRealSolver.solve(...) declares `@throws TooManyEvaluationsException if the allowed number of evaluations is exceeded`; the bug's failing test fixes the correct behavior for RegulaFalsiSolver on `f(x)=exp(x)-pi^3`, interval `[1,10]`, and `maxEval=3624`: it must throw `org.apache.commons.math.exception.TooManyEvaluationsException` rather than return normally.
  // valid input: Recreate the failing-test setup exactly: `RegulaFalsiSolver`, `f(x)=Math.exp(x)-Math.pow(Math.PI,3.0)`, `min=1`, `max=10`, `maxEval=3624`. Use a zero-width fuzz draw only to satisfy the harness while keeping the ground-truth input fixed.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 20000/20000 on random inputs
int maxEval;
org.apache.commons.math.analysis.UnivariateRealFunction f;
org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;
try {
  maxEval = 3624 + data.consumeInt(0, 0);
  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {
    public double value(double x) {
      return Math.exp(x) - Math.pow(Math.PI, 3.0);
    }
  };
  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();
} catch (Exception e) {
  return;
}
boolean violated = false;
String msg = "completed normally";
try {
  solver.solve(maxEval, f, 1.0, 10.0);
  violated = true;
} catch (org.apache.commons.math.exception.TooManyEvaluationsException ok) {
  // expected
} catch (Throwable t) {
  violated = true;
  msg = "threw wrong exception " + t.getClass().getName() + ": " + t.getMessage();
}
if (violated) {
  throw new RuntimeException("relation regulafalsi_issue631_must_throw_too_many_evaluations violated: " + msg);
}
</relation>
<relation name="default_any_side_matches_explicit_any_side" kind="metamorphic">
  // holds because: BracketedUnivariateRealSolver documentation states: "For backwards compatibility, all root-finding algorithms must have ANY_SIDE as default for the allowed solutions." Therefore `solve(maxEval,f,min,max)` and `solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)` must agree on the same valid problem.
  // valid input: Use a function with a unique root exactly at one endpoint so the returned zero is forced and independent of iteration details: choose `c` from fuzz data, let `f(x)=x-c`, and choose either `[c, c+w]` or `[c-w, c]` with positive integer width `w` from fuzz data.
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
org.apache.commons.math.analysis.solvers.RegulaFalsiSolver solver;
org.apache.commons.math.analysis.UnivariateRealFunction f;
double c;
double min;
double max;
int w;
boolean leftRoot;
double r1;
double r2;
try {
  solver = new org.apache.commons.math.analysis.solvers.RegulaFalsiSolver();
  c = data.consumeInt(-1000, 1000);
  w = data.consumeInt(1, 1000);
  leftRoot = data.consumeBoolean();
  min = leftRoot ? c : c - w;
  max = leftRoot ? c + w : c;
  f = new org.apache.commons.math.analysis.UnivariateRealFunction() {
    public double value(double x) {
      return x - c;
    }
  };
} catch (Exception e) {
  return;
}
try {
  r1 = solver.solve(100, f, min, max);
} catch (Exception e) {
  throw new RuntimeException("relation default_any_side_matches_explicit_any_side violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
try {
  r2 = solver.solve(100, f, min, max, org.apache.commons.math.analysis.solvers.AllowedSolution.ANY_SIDE);
} catch (Exception e) {
  throw new RuntimeException("relation default_any_side_matches_explicit_any_side violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {
  throw new RuntimeException("relation default_any_side_matches_explicit_any_side violated: " + r1 + " vs " + r2);
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
<details open><summary>▸ Output (~5,684 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final int sink = data.consumeInt(0, 0);

        // Lifted oracle from RegulaFalsiSolverTest.testIssue631:
        // for this exact public-API call, the documented solve contract says a TooManyEvaluationsException
        // must be thrown when maxEval is exceeded; the upstream test pins that this exact setup must throw.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

            boolean violation = false;
            String message = "completed normally";
            try {
                double root = solver.solve(3624 + sink, f, 1.0, 10.0);
                violation = true;
                message = "completed normally with root=" + root;
            } catch (TooManyEvaluationsException expected) {
                // expected
            } catch (Throwable t) {
                violation = true;
                message = "threw wrong exception " + t.getClass().getName() + ": " + t.getMessage();
            }
            if (violation) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-must-throw] semantic mismatch: " + message +
                    " expected=org.apache.commons.math.exception.TooManyEvaluationsException"
                );
            }
        }

        // Metamorphic sibling-agreement check:
        // BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution for backwards compatibility,
        // so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double rDefault;
            try {
                rDefault = solver.solve(100, f, min, max);
            } catch (Throwable t) {
                return;
            }

            double rExplicit;
            try {
                rExplicit = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-vs-explicit-any-side] metamorphic violation: " +
                    "solve(maxEval,f,min,max) != solve(maxEval,f,min,max,ANY_SIDE)" +
                    " min=" + min + " max=" + max + " c=" + c +
                    " lhs=" + rDefault + " rhs=" + rExplicit
                );
            }
        }

        // Additional sibling-agreement/post-state check:
        // The overload with startValue but without AllowedSolution is also documented to use the default ANY_SIDE.
        // We call the explicit-ANY_SIDE overload first to mutate the solver's internal `allowed` state, then re-probe
        // with the default overload on the same valid problem. A correct implementation must still behave as ANY_SIDE.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(2, 1000);
            final double min = c - w;
            final double max = c + w;
            final double startValue = c + data.consumeInt(-w + 1, w - 1);
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            try {
                solver.solve(100, f, min, max, startValue, AllowedSolution.LEFT_SIDE);
            } catch (Throwable t) {
                return;
            }

            double rDefault;
            try {
                rDefault = solver.solve(100, f, min, max, startValue);
            } catch (Throwable t) {
                return;
            }

            double rExplicitAny;
            try {
                rExplicitAny = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:start-overload-default-any-side] metamorphic violation: " +
                    "solve(maxEval,f,min,max,startValue) != solve(maxEval,f,min,max,startValue,ANY_SIDE)" +
                    " min=" + min + " max=" + max + " startValue=" + startValue + " c=" + c +
                    " lhs=" + rDefault + " rhs=" + rExplicitAny
                );
            }
        }
    }
}
```

</details>

---
## [29] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-must-throw] semantic mismatch: completed normally with root=3.4341896575482003 expected=org.apache.commons.math.exception.TooM…

---
## [30] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~50,878 chars, 1 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
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

Codebase: `Math_50_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
+++ /src/main/java/org/apache/commons/math/analysis/solvers/BaseSecantSolver.java
@@ -184,10 +184,6 @@
                     break;
                 case REGULA_FALSI:
                     // Nothing.
-                    if (x == x1) {
-                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
-                        f0 = computeObjectiveValue(x0);
-                    }
                     break;
                 default:
                     // Should never happen.

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.MathInternalError;
</source_imports>

Function `doSolve`:
<signature>
final protected double doSolve()
</signature>
<code>
    protected final double doSolve() {
        // Get initial solution
        double x0 = getMin();
        double x1 = getMax();
        double f0 = computeObjectiveValue(x0);
        double f1 = computeObjectiveValue(x1);

        // If one of the bounds is the exact root, return it. Since these are
        // not under-approximations or over-approximations, we can return them
        // regardless of the allowed solutions.
        if (f0 == 0.0) {
            return x0;
        }
        if (f1 == 0.0) {
            return x1;
        }

        // Verify bracketing of initial solution.
        verifyBracketing(x0, x1);

        // Get accuracies.
        final double ftol = getFunctionValueAccuracy();
        final double atol = getAbsoluteAccuracy();
        final double rtol = getRelativeAccuracy();

        // Keep track of inverted intervals, meaning that the left bound is
        // larger than the right bound.
        boolean inverted = false;

        // Keep finding better approximations.
        while (true) {
            // Calculate the next approximation.
            final double x = x1 - ((f1 * (x1 - x0)) / (f1 - f0));
            final double fx = computeObjectiveValue(x);

            // If the new approximation is the exact root, return it. Since
            // this is not an under-approximation or an over-approximation,
            // we can return it regardless of the allowed solutions.
            if (fx == 0.0) {
                return x;
            }

            // Update the bounds with the new approximation.
            if (f1 * fx < 0) {
                // The value of x1 has switched to the other bound, thus inverting
                // the interval.
                x0 = x1;
                f0 = f1;
                inverted = !inverted;
            } else {
                switch (method) {
                case ILLINOIS:
                    f0 *= 0.5;
                    break;
                case PEGASUS:
                    f0 *= f1 / (f1 + fx);
                    break;
                case REGULA_FALSI:
                    // Nothing.
                    if (x == x1) {
                        x0 = 0.5 * (x0 + x1 - FastMath.max(rtol * FastMath.abs(x1), atol));
                        f0 = computeObjectiveValue(x0);
                    }
                    break;
                default:
                    // Should never happen.
                    throw new MathInternalError();
                }
            }
            // Update from [x0, x1] to [x0, x].
            x1 = x;
            f1 = fx;

            // If the function value of the last approximation is too small,
            // given the function value accuracy, then we can't get closer to
            // the root than we already are.
            if (FastMath.abs(f1) <= ftol) {
                switch (allowed) {
                case ANY_SIDE:
                    return x1;
                case LEFT_SIDE:
                    if (inverted) {
                        return x1;
                    }
                    break;
                case RIGHT_SIDE:
                    if (!inverted) {
                        return x1;
                    }
                    break;
                case BELOW_SIDE:
                    if (f1 <= 0) {
                        return x1;
                    }
                    break;
                case ABOVE_SIDE:
                    if (f1 >= 0) {
                        return x1;
                    }
                    break;
                default:
                    throw new MathInternalError();
                }
            }

            // If the current interval is within the given accuracies, we
            // are satisfied with the current approximation.
            if (FastMath.abs(x1 - x0) < FastMath.max(rtol * FastMath.abs(x1),
                                                     atol)) {
                switch (allowed) {
                case ANY_SIDE:
                    return x1;
                case LEFT_SIDE:
                    return inverted ? x1 : x0;
                case RIGHT_SIDE:
                    return inverted ? x0 : x1;
                case BELOW_SIDE:
                    return (f1 <= 0) ? x1 : x0;
                case ABOVE_SIDE:
                    return (f1 >= 0) ? x1 : x0;
                default:
                    throw new MathInternalError();
                }
            }
        }
    }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `doSolve` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `doSolve`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `protected BaseSecantSolver(double absoluteAccuracy, Method method)` (shared field(s): allowed, method)
      doc: Construct a solver. @param absoluteAccuracy absolute accuracy @param method <em>Secant</em>-based root-finding method to use
    <constructor_body>
    protected BaseSecantSolver(final double absoluteAccuracy, final Method method) {
        super(absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
    </constructor_body>
  - `protected BaseSecantSolver(double relativeAccuracy, double absoluteAccuracy, Method method)` (shared field(s): allowed, method)
      doc: Construct a solver. @param relativeAccuracy relative accuracy @param absoluteAccuracy absolute accuracy @param method <em>Secant</em>-based root-finding method to use
    <constructor_body>
    protected BaseSecantSolver(final double relativeAccuracy,
                               final double absoluteAccuracy,
                               final Method method) {
        super(relativeAccuracy, absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
    </constructor_body>
  - `protected BaseSecantSolver(double relativeAccuracy, double absoluteAccuracy, double functionValueAccuracy, Method method)` (shared field(s): allowed, method)
      doc: Construct a solver. @param relativeAccuracy Maximum relative error. @param absoluteAccuracy Maximum absolute error. @param functionValueAccuracy Maximum function value error. @param method <em>Secant</em>-based root-finding method to use
    <constructor_body>
    protected BaseSecantSolver(final double relativeAccuracy,
                               final double absoluteAccuracy,
                               final double functionValueAccuracy,
                               final Method method) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
    </constructor_body>
  - `public double solve(int maxEval, UnivariateRealFunction f, double min, double max, double startValue, AllowedSolution allowedSolution)` (shared field(s): allowed)
      doc: {@inheritDoc}

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="BaseSecantSolver" role="patched">
     * Base class for all bracketing <em>Secant</em>-based methods for root-finding
     * (approximating a zero of a univariate real function).
     * <p>Implementation of the {@link RegulaFalsiSolver <em>Regula Falsi</em>} and
     * {@link IllinoisSolver <em>Illinois</em>} methods is based on the
     * following article: M. Dowell and P. Jarratt,
     * <em>A modified regula falsi method for computing the root of an
     * equation</em>, BIT Numerical Mathematics, volume 11, number 2,
     * pages 168-174, Springer, 1971.</p>
     * <p>Implementation of the {@link PegasusSolver <em>Pegasus</em>} method is
     * based on the following article: M. Dowell and P. Jarratt,
     * <em>The "Pegasus" method for computing the root of an equation</em>,
     * BIT Numerical Mathematics, volume 12, number 4, pages 503-508, Springer,
     * …
public abstract class BaseSecantSolver
    extends AbstractUnivariateRealSolver
    implements BracketedUnivariateRealSolver<UnivariateRealFunction> {
     * Default absolute accuracy.
    protected static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;
     * The kinds of solutions that the algorithm may accept.
    private AllowedSolution allowed;
     * The <em>Secant</em>-based root-finding method to use.
    private final Method method;
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
     * @param method <em>Secant</em>-based root-finding method to use
    protected BaseSecantSolver(final double absoluteAccuracy, final Method method) {
        super(absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param method <em>Secant</em>-based root-finding method to use
    protected BaseSecantSolver(final double relativeAccuracy, final double absoluteAccuracy, final Method method) {
        super(relativeAccuracy, absoluteAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
     * Construct a solver.
     * @param relativeAccuracy Maximum relative error.
     * @param absoluteAccuracy Maximum absolute error.
     * @param functionValueAccuracy Maximum function value error.
     * @param method <em>Secant</em>-based root-finding method to use
    protected BaseSecantSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy, final Method method) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
        this.allowed = AllowedSolution.ANY_SIDE;
        this.method = method;
    }
     * {@inheritDoc}
    public double solve(final int maxEval, final UnivariateRealFunction f, final double min, final double max, final AllowedSolution allowedSolution) { … }
     * {@inheritDoc}
    public double solve(final int maxEval, final UnivariateRealFunction f, final double min, final double max, final double startValue, final AllowedSolution allowedSolution) { … }
     * {@inheritDoc}
    @Override public double solve(final int maxEval, final UnivariateRealFunction f, final double min, final double max, final double startValue) { … }
     * <em>Secant</em>-based root-finding methods.
    protected enum Method { … }
}
</class>

<class name="AbstractUnivariateRealSolver" role="supertype-contract">
     * Base class for solvers.
     * @version $Id$
     * @since 3.0
public abstract class AbstractUnivariateRealSolver
    extends BaseAbstractUnivariateRealSolver<UnivariateRealFunction>
    implements UnivariateRealSolver {
     * Construct a solver with given absolute accuracy.
     * @param absoluteAccuracy Maximum absolute error.
    protected AbstractUnivariateRealSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy);
    }
     * Construct a solver with given accuracies.
     * @param relativeAccuracy Maximum relative error.
     * @param absoluteAccuracy Maximum absolute error.
    protected AbstractUnivariateRealSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy);
    }
     * Construct a solver with given accuracies.
     * @param relativeAccuracy Maximum relative error.
     * @param absoluteAccuracy Maximum absolute error.
     * @param functionValueAccuracy Maximum function value error.
    protected AbstractUnivariateRealSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
    }
}
</class>

<class name="BracketedUnivariateRealSolver" role="supertype-contract">
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.math.analysis.solvers;

import org.apache.commons.math.analysis.UnivariateRealFunction;

/** Interface for {@link UnivariateRealSolver (univariate real) root-finding
 * algorithms} that maintain a bracketed solution. There are several advantages
 * to having such root-finding algorithms:
 * <ul>
 *  <li>The bracketed solution guarantees that the root is kept within the
 *      interval. As such, these algorithms generally also guarantee
 *      convergence.</li>
 *  <li>The bracketed solution means that we have the opportunity to only
 *      return roots that are greater than or equal to the actual root, or
 *      are less than or equal to the actual root. That is, we can control
 *      whether under-approximations and over-approximations are
 *      {@link AllowedSolution allowed solutions}. Other root-finding
 *      algorithms can usually only guarantee that the solution (the root that
 *      was found) is around the actual root.</li>
 * </ul>
 *
 * <p>For backwards compatibility, all root-finding algorithms must have
 * {@link AllowedSolution#ANY_SIDE ANY_SIDE} as default for the allowed
 * solutions.</p>
 * @param <FUNC> Type of function to solve.
 *
 * @see AllowedSolution
 * @since 3.0
 * @version $Id$
 */
public interface BracketedUnivariateRealSolver<FUNC extends UnivariateRealFunction>
    extends BaseUnivariateRealSolver<FUNC> {

    /**
     * Solve for a zero in the given interval.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     *
     * @param maxEval Maximum number of evaluations.
     * @param f Function to solve.
     * @param min Lower bound for the interval.
     * @param max Upper bound for the interval.
     * @param allowedSolution The kind of solutions that the root-finding algorithm may
     * accept as solutions.
     * @return A value where the function is zero.
     * @throws org.apache.commons.math.exception.MathIllegalArgumentException
     * if the arguments do not satisfy the requirements specified by the solver.
     * @throws org.apache.commons.math.exception.TooManyEvaluationsException if
     * the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, FUNC f, double min, double max,
                 AllowedSolution allowedSolution);

    /**
     * Solve for a zero in the given interval, start at {@code startValue}.
     * A solver may require that the interval brackets a single zero root.
     * Solvers that do require bracketing should be able to handle the case
     * where one of the endpoints is itself a root.
     *
     * @param maxEval Maximum number of evaluations.
     * @param f Function to solve.
     * @param min Lower bound for the interval.
     * @param max Upper bound for the interval.
     * @param startValue Start value to use.
     * @param allowedSolution The kind of solutions that the root-finding algorithm may
     * accept as solutions.
     * @return A value where the function is zero.
     * @throws org.apache.commons.math.exception.MathIllegalArgumentException
     * if the arguments do not satisfy the requirements specified by the solver.
     * @throws org.apache.commons.math.exception.TooManyEvaluationsException if
     * the allowed number of evaluations is exceeded.
     */
    double solve(int maxEval, FUNC f, double min, double max, double startValue,
                 AllowedSolution allowedSolution);

}

</class>

<class name="UnivariateRealFunction" role="supertype-contract">
     * An interface representing a univariate real function.
     * @version $Id$
public interface UnivariateRealFunction {

}
</class>

<class name="RegulaFalsiSolver" role="test-subject">
     * Implements the <em>Regula Falsi</em> or <em>False position</em> method for
     * root-finding (approximating a zero of a univariate real function). It is a
     * modified {@link SecantSolver <em>Secant</em>} method. Unlike the
     * <em>Secant</em> method, convergence is guaranteed by maintaining a
     * bracketed solution.
     * <p>The <em>Regula Falsi</em> method assumes that the function is continuous,
     * but not necessarily smooth.</p>
     * <p>Implementation based on the following article: M. Dowell and P. Jarratt,
     * <em>A modified regula falsi method for computing the root of an
     * equation</em>, BIT Numerical Mathematics, volume 11, number 2,
     * pages 168-174, Springer, 1971.</p>
     * @since 3.0
     * …
public class RegulaFalsiSolver extends BaseSecantSolver {
     * Construct a solver with default accuracy (1e-6).
    public RegulaFalsiSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY, Method.REGULA_FALSI);
    }
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
    public RegulaFalsiSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy, Method.REGULA_FALSI);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
    public RegulaFalsiSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, Method.REGULA_FALSI);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param functionValueAccuracy Maximum function value error.
    public RegulaFalsiSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy, Method.REGULA_FALSI);
    }
}
</class>

<class name="TooManyEvaluationsException" role="test-subject">
     * Exception to be thrown when the maximal number of evaluations is exceeded.
     * @since 3.0
     * @version $Id$
public class TooManyEvaluationsException extends MaxCountExceededException {
     * Serializable version Id.
    private static final long serialVersionUID = 4330003017885151975L;
     * Construct the exception.
     * @param max Maximum number of evaluations.
    public TooManyEvaluationsException(Number max) {
        super(max);
        getContext().addMessage(LocalizedFormats.EVALUATIONS);
    }
}
</class>

<class name="UnivariateRealSolver" role="test-subject">
     * Interface for (univariate real) root-finding algorithms.
     * Implementations will search for only one zero in the given interval.
     * @version $Id$
public interface UnivariateRealSolver
    extends BaseUnivariateRealSolver<UnivariateRealFunction> {

}
</class>

<class name="FastMath" role="collaborator">
     * Faster, more accurate, portable alternative to {@link Math} and
     * {@link StrictMath} for large scale computation.
     * <p>
     * FastMath speed is achieved by relying heavily on optimizing compilers
     * to native code present in many JVM todays and use of large tables that
     * are computed once at class loading (regardless of the number of subsequent
     * calls to computation methods). This implies that FastMath is targeted
     * more towards large scale computation (i.e. computation that take at least
     * a handful of seconds to complete) on desktop or server machines rather
     * than very small utilities on devices with limited power (i.e. computation
     * that should return a result almost instantly). Note that FastMath is
     * extensively used inside Apache Commons Math, so by calling some algorithms,
     * …
public class FastMath {
     * Archimede's constant PI, ratio of circle circumference to diameter.
    public static final double PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9;
     * Napier's constant e, base of the natural logarithm.
    public static final double E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8;
     * Exponential evaluated at integer values,
     * exp(x) =  expIntTableA[x + 750] + expIntTableB[x+750].
    private static final double EXP_INT_TABLE_A[] = new double[1500];
     * Exponential evaluated at integer values,
     * exp(x) =  expIntTableA[x + 750] + expIntTableB[x+750]
    private static final double EXP_INT_TABLE_B[] = new double[1500];
     * Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
    private static final double EXP_FRAC_TABLE_A[] = new double[1025];
     * Exponential over the range of 0 - 1 in increments of 2^-10
     * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
    private static final double EXP_FRAC_TABLE_B[] = new double[1025];
     * Factorial table, for Taylor series expansions.
    private static final double FACT[] = new double[20];
     * Extended precision logarithm table over the range 1 - 2 in increments of 2^-10.
    private static final double LN_MANT[][] = new double[1024][];
     * log(2) (high bits).
    private static final double LN_2_A = 0.693147063255310059;
     * log(2) (low bits).
    private static final double LN_2_B = 1.17304635250823482e-7;
     * Coefficients for slowLog.
    private static final double LN_SPLIT_COEF[][] = { … }
     * Coefficients for log, when input 0.99 < x < 1.01.
    private static final double LN_QUICK_COEF[][] = { … }
     * Coefficients for log in the range of 1.0 < x < 1.0 + 2^-10.
    private static final double LN_HI_PREC_COEF[][] = { … }
     * Sine table (high bits).
    private static final double SINE_TABLE_A[] = new double[14];
     * Sine table (low bits).
    private static final double SINE_TABLE_B[] = new double[14];
     * Cosine table (high bits).
    private static final double COSINE_TABLE_A[] = new double[14];
     * Cosine table (low bits).
    private static final double COSINE_TABLE_B[] = new double[14];
     * Tangent table, used by atan() (high bits).
    private static final double TANGENT_TABLE_A[] = new double[14];
     * Tangent table, used by atan() (low bits).
    private static final double TANGENT_TABLE_B[] = new double[14];
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
     * Private Constructor
    private FastMath() {
    }
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     * @param d the value to split
     * @return the high order part of the mantissa
    private static double doubleHighPart(double d) { … }
     * For x between 0 and 1, returns exp(x), uses extended precision
     * @param x argument of exponential
     * @param result placeholder where to place exp(x) split in two terms
     * for extra precision (i.e. exp(x) = result[0] ° result[1]
     * @return exp(x)
    private static double slowexp(final double x, final double result[]) { … }
     * Compute split[0], split[1] such that their sum is equal to d,
     * and split[0] has its 30 least significant bits as zero.
     * @param d number to split
     * @param split placeholder where to place the result
    private static void split(final double d, final double split[]) { … }
     * Recompute a split.
     * @param a input/out array containing the split, changed
     * on output
    private static void resplit(final double a[]) { … }
     * Multiply two numbers in split form.
     * @param a first term of multiplication
     * @param b second term of multiplication
     * @param ans placeholder where to put the result
    private static void splitMult(double a[], double b[], double ans[]) { … }
     * Add two numbers in split form.
     * @param a first term of addition
     * @param b second term of addition
     * @param ans placeholder where to put the result
    private static void splitAdd(final double a[], final double b[], final double ans[]) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="AllowedSolution" role="collaborator">
     * The kinds of solutions that a {@link BracketedUnivariateRealSolver
     * (bracketed univariate real) root-finding algorithm} may accept as solutions.
     * This basically controls whether or not under-approximations and
     * over-approximations are allowed.
     * <p>If all solutions are accepted ({@link #ANY_SIDE}), then the solution
     * that the root-finding algorithm returns for a given root may be equal to the
     * actual root, but it may also be an approximation that is slightly smaller
     * or slightly larger than the actual root. Root-finding algorithms generally
     * only guarantee that the returned solution is within the requested
     * tolerances. In certain cases however, in particular for
     * {@link org.apache.commons.math.ode.events.EventHandler state events} of
     * {@link org.apache.commons.math.ode.ODEIntegrator ODE solvers}, it
     * …
public enum AllowedSolution {

}
</class>

<class name="MathInternalError" role="collaborator">
     * Exception triggered when something that shouldn't happen does happen.
     * @since 2.2
     * @version $Id$
public class MathInternalError extends MathIllegalStateException {
     * Serializable version Id.
    private static final long serialVersionUID = -6276776513966934846L;
     * URL for reporting problems.
    private static final String REPORT_URL = "https://issues.apache.org/jira/browse/MATH";
     * Simple constructor.
    public MathInternalError() {
        getContext().addMessage(LocalizedFormats.INTERNAL_ERROR, REPORT_URL);
    }
     * Simple constructor.
     * @param cause root cause
    public MathInternalError(final Throwable cause) {
        super(cause, LocalizedFormats.INTERNAL_ERROR, REPORT_URL);
    }
}
</class>

<class name="IllinoisSolver" role="collaborator">
     * Implements the <em>Illinois</em> method for root-finding (approximating
     * a zero of a univariate real function). It is a modified
     * {@link RegulaFalsiSolver <em>Regula Falsi</em>} method.
     * <p>Like the <em>Regula Falsi</em> method, convergence is guaranteed by
     * maintaining a bracketed solution. The <em>Illinois</em> method however,
     * should converge much faster than the original <em>Regula Falsi</em>
     * method.</p>
     * <p>The <em>Illinois</em> method assumes that the function is continuous,
     * but not necessarily smooth.</p>
     * <p>Implementation based on the following article: M. Dowell and P. Jarratt,
     * <em>A modified regula falsi method for computing the root of an
     * equation</em>, BIT Numerical Mathematics, volume 11, number 2,
     * …
public class IllinoisSolver extends BaseSecantSolver {
     * Construct a solver with default accuracy (1e-6).
    public IllinoisSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY, Method.ILLINOIS);
    }
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
    public IllinoisSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy, Method.ILLINOIS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
    public IllinoisSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, Method.ILLINOIS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param functionValueAccuracy Maximum function value error.
    public IllinoisSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy, Method.PEGASUS);
    }
}
</class>

<class name="PegasusSolver" role="collaborator">
     * Implements the <em>Pegasus</em> method for root-finding (approximating
     * a zero of a univariate real function). It is a modified
     * {@link RegulaFalsiSolver <em>Regula Falsi</em>} method.
     * <p>Like the <em>Regula Falsi</em> method, convergence is guaranteed by
     * maintaining a bracketed solution. The <em>Pegasus</em> method however,
     * should converge much faster than the original <em>Regula Falsi</em> method.
     * Furthermore, it should converge faster than the
     * {@link IllinoisSolver <em>Illinois</em>} method, another
     * <em>Regula Falsi</em>-based method.</p>
     * <p>The <em>Pegasus</em> method assumes that the function is continuous,
     * but not necessarily smooth.</p>
     * <p>Implementation based on the following article: M. Dowell and P. Jarratt,
     * …
public class PegasusSolver extends BaseSecantSolver {
     * Construct a solver with default accuracy (1e-6).
    public PegasusSolver() {
        super(DEFAULT_ABSOLUTE_ACCURACY, Method.PEGASUS);
    }
     * Construct a solver.
     * @param absoluteAccuracy absolute accuracy
    public PegasusSolver(final double absoluteAccuracy) {
        super(absoluteAccuracy, Method.PEGASUS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
    public PegasusSolver(final double relativeAccuracy, final double absoluteAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, Method.PEGASUS);
    }
     * Construct a solver.
     * @param relativeAccuracy relative accuracy
     * @param absoluteAccuracy absolute accuracy
     * @param functionValueAccuracy Maximum function value error.
    public PegasusSolver(final double relativeAccuracy, final double absoluteAccuracy, final double functionValueAccuracy) {
        super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy, Method.PEGASUS);
    }
}
</class>
</class_skeletons>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param absoluteAccuracy absolute accuracy
  - @param method <em>Secant</em>-based root-finding met
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max,
                        final AllowedSolution allowedSolution) / (final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max, final double startValue,
                        final AllowedSolution allowedSolution) / (final int maxEval, final UnivariateRealFunction f,
                        final double min, final double max, final double startValue)

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `RegulaFalsiSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest" method="testIssue631">
    @Test(expected=TooManyEvaluationsException.class)
    public void testIssue631() {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
                /** {@inheritDoc} */
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

        final UnivariateRealSolver solver = new RegulaFalsiSolver();
        final double root = solver.solve(3624, f, 1, 10);
        Assert.assertEquals(3.4341896575482003, root, 1e-15);
	}
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest::testIssue631
junit.framework.AssertionFailedError: Expected exception: org.apache.commons.math.exception.TooManyEvaluationsException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.analysis.solvers.RegulaFalsiSolverTest">
// --- helper value() ---
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
</test_support>
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
<details open><summary>▸ Output (~3,836 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final UnivariateRealSolver issue631Solver = new RegulaFalsiSolver();
        try {
            final double root = issue631Solver.solve(3624, issue631, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exception] semantic mismatch: expected TooManyEvaluationsException but solve returned root="
                    + root + " expectedWrongValueOnBuggyBuild=3.4341896575482003");
        } catch (TooManyEvaluationsException expected) {
            // Correct patched behavior from the lifted test: this call must reject by exhausting evaluations.
        }

        final double absAcc = Math.pow(10.0, -data.consumeInt(4, 8));
        final double relAcc = Math.pow(10.0, -data.consumeInt(4, 8));
        final double fAcc = Math.pow(10.0, -data.consumeInt(4, 8));
        final int maxEval = data.consumeInt(16, 256);

        final RegulaFalsiSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = new RegulaFalsiSolver(absAcc);
                break;
            case 1:
                solver = new RegulaFalsiSolver(relAcc, absAcc);
                break;
            default:
                solver = new RegulaFalsiSolver(relAcc, absAcc, fAcc);
                break;
        }

        final UnivariateRealFunction sqrtTwo = new UnivariateRealFunction() {
            public double value(double x) {
                return x * x - 2.0;
            }
        };

        final AllowedSolution[] allowedValues = AllowedSolution.values();
        final AllowedSolution priorAllowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        try {
            solver.solve(maxEval, sqrtTwo, 0.0, 2.0, priorAllowed);
        } catch (Throwable t) {
            return;
        }

        final double implicitAny;
        try {
            implicitAny = solver.solve(maxEval, sqrtTwo, 0.0, 2.0);
        } catch (Throwable t) {
            return;
        }

        final double explicitAny;
        try {
            explicitAny = solver.solve(maxEval, sqrtTwo, 0.0, 2.0, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        final double tol = Math.max(Math.max(absAcc, relAcc * Math.abs(explicitAny)) * 8.0, 1e-12);
        // Contract justification:
        // BracketedUnivariateRealSolver says ANY_SIDE is the default allowed solution for backwards compatibility.
        // Therefore solve(maxEval,f,min,max) must agree with solve(maxEval,f,min,max,ANY_SIDE),
        // even after a prior call changed the receiver's internal "allowed" field. A patch that merely
        // suppresses/changes behavior in doSolve while leaving stale state can violate this observable equivalence.
        if (Math.abs(implicitAny - explicitAny) > tol) {
            throw new RuntimeException(
                "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must match solve(maxEval,f,min,max,ANY_SIDE)"
                    + " priorAllowed=" + priorAllowed
                    + " implicit=" + implicitAny
                    + " explicit=" + explicitAny
                    + " tol=" + tol);
        }
    }
}
```

</details>

---
## [31] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-exception] semantic mismatch: expected TooManyEvaluationsException but solve returned root=3.4341896575482003 expectedWrongVal…

---
## [32] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,634 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static String safe(Throwable t) {
        return t == null ? "null" : String.valueOf(t.getMessage());
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver issue631Solver = new RegulaFalsiSolver();
        boolean issue631Violated = false;
        String issue631Message = null;
        Throwable issue631Cause = null;
        try {
            final double root = issue631Solver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            issue631Violated = true;
            issue631Message =
                "expected org.apache.commons.math.exception.TooManyEvaluationsException but completed normally with root=" +
                root + " pinnedBuggyRootDelta=" + Math.abs(root - 3.4341896575482003);
        } catch (TooManyEvaluationsException expected) {
            // Trusted oracle lifted verbatim from RegulaFalsiSolverTest.testIssue631:
            // on this exact problem, solve(3624, f, 1, 10) must throw TooManyEvaluationsException.
        } catch (Throwable t) {
            issue631Violated = true;
            issue631Cause = t;
            issue631Message =
                "expected org.apache.commons.math.exception.TooManyEvaluationsException but threw " +
                t.getClass().getName() + ": " + safe(t);
        }
        if (issue631Violated) {
            if (issue631Cause != null) {
                throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: " + issue631Message, issue631Cause);
            }
            throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: " + issue631Message);
        }

        final double c = (double) data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        final double startValue = min + ((max - min) / 2.0);

        final UnivariateRealFunction linearAtEndpoint = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        final RegulaFalsiSolver relationSolver = new RegulaFalsiSolver();
        boolean relation1Ready = false;
        boolean relation1Violated = false;
        String relation1Message = null;
        Throwable relation1Cause = null;
        double relation1Left = 0.0;
        double relation1Right = 0.0;
        try {
            relation1Left = relationSolver.solve(100, linearAtEndpoint, min, max);
            relation1Right = relationSolver.solve(100, linearAtEndpoint, min, max, AllowedSolution.ANY_SIDE);
            relation1Ready = true;
        } catch (Throwable t) {
            relation1Cause = t;
        }
        if (relation1Cause == null && relation1Ready &&
            Double.doubleToLongBits(relation1Left) != Double.doubleToLongBits(relation1Right)) {
            relation1Violated = true;
            relation1Message =
                "solve(maxEval,f,min,max)=" + relation1Left +
                " vs solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)=" + relation1Right +
                " input[min=" + min + ",max=" + max + ",c=" + c + "]";
        }
        if (relation1Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: " + relation1Message);
        }

        final RegulaFalsiSolver relationSolver2 = new RegulaFalsiSolver();
        boolean relation2Ready = false;
        boolean relation2Violated = false;
        String relation2Message = null;
        Throwable relation2Cause = null;
        double relation2Left = 0.0;
        double relation2Right = 0.0;
        try {
            relation2Left = relationSolver2.solve(100, linearAtEndpoint, min, max, AllowedSolution.ANY_SIDE);
            relation2Right = relationSolver2.solve(100, linearAtEndpoint, min, max, startValue, AllowedSolution.ANY_SIDE);
            relation2Ready = true;
        } catch (Throwable t) {
            relation2Cause = t;
        }
        if (relation2Cause == null && relation2Ready &&
            Double.doubleToLongBits(relation2Left) != Double.doubleToLongBits(relation2Right)) {
            relation2Violated = true;
            relation2Message =
                "explicit ANY_SIDE overloads disagree: withoutStart=" + relation2Left +
                " withStart=" + relation2Right +
                " input[min=" + min + ",max=" + max + ",start=" + startValue + ",c=" + c + "]";
        }
        if (relation2Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:overload-agreement] metamorphic violation: " + relation2Message);
        }

        final RegulaFalsiSolver endpointSolver = new RegulaFalsiSolver();
        boolean endpointReady = false;
        boolean endpointViolated = false;
        String endpointMessage = null;
        Throwable endpointCause = null;
        double endpointRoot = 0.0;
        try {
            endpointRoot = endpointSolver.solve(100, linearAtEndpoint, min, max, AllowedSolution.ANY_SIDE);
            endpointReady = true;
        } catch (Throwable t) {
            endpointCause = t;
        }
        // Contract from doSolve shown above: if one bound is an exact root, it returns that bound immediately.
        // This post-condition remains observable through the public solve API; a patch that only suppresses
        // iteration/throw behavior but corrupts solver state or allowed-solution handling would break it.
        if (endpointCause == null && endpointReady &&
            Double.doubleToLongBits(endpointRoot) != Double.doubleToLongBits(c)) {
            endpointViolated = true;
            endpointMessage =
                "endpoint exact-root contract violated: expected " + c + " but got " + endpointRoot +
                " input[min=" + min + ",max=" + max + "]";
        }
        if (endpointViolated) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root] semantic mismatch: " + endpointMessage);
        }
    }
}
```

</details>

---
## [33] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but completed normally with root=3.…

---
## [34] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,869 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final double root = solver.solve(3624, issue631, 1.0, 10.0);
            final double expectedBuggyRoot = 3.4341896575482003;
            if (Math.abs(root - expectedBuggyRoot) > 1e-15) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-returned] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but call returned unexpected value actual=" + root +
                    " expectedBuggyReturn=" + expectedBuggyRoot);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-returned] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but call returned actual=" + root +
                " expectedBuggyReturn=" + expectedBuggyRoot);
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        final int chosenMaxEval = data.consumeInt(20, 2000);
        final double relAcc = 1.0e-14;
        final double absAcc = 1.0e-14;

        final int coeff = data.consumeInt(-100, 100);
        final int offset = data.consumeInt(-100, 100);
        final double rootSeed = data.consumeInt(-1000, 1000) / 10.0;
        final double a = (coeff == 0 ? 1.0 : (double) coeff);
        final double b = (double) offset;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return a * (x - rootSeed) + b;
            }
        };

        final double exactRoot = rootSeed - (b / a);
        final double span = data.consumeInt(2, 100);
        double min = exactRoot - span;
        double max = exactRoot + span;
        if (!(min < max) || Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max)) {
            return;
        }

        try {
            final RegulaFalsiSolver mutated = new RegulaFalsiSolver(relAcc, absAcc);
            try {
                mutated.solve(chosenMaxEval, linear, min, max, AllowedSolution.LEFT_SIDE);
            } catch (Throwable ignored) {
                return;
            }

            final double defaultResult;
            final double explicitAnyResult;
            try {
                defaultResult = mutated.solve(chosenMaxEval, linear, min, max);
                explicitAnyResult = new RegulaFalsiSolver(relAcc, absAcc).solve(chosenMaxEval, linear, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable ignored) {
                return;
            }

            if (Double.doubleToLongBits(defaultResult) != Double.doubleToLongBits(explicitAnyResult)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must behave like solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution, and BaseSecantSolver constructors initialize that state to ANY_SIDE; a patch that silently keeps stale allowed state after a previous solve would break this relation inputRoot=" +
                    exactRoot + " min=" + min + " max=" + max + " lhs=" + defaultResult + " rhs=" + explicitAnyResult);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver(relAcc, absAcc);
            final double recovered = solver.solve(chosenMaxEval, linear, min, max, AllowedSolution.ANY_SIDE);
            if (Math.abs(recovered - exactRoot) > 1e-8) {
                throw new RuntimeException(
                    "[oracle:constructed-root] metamorphic violation: function was constructed from a known root, so solve must recover that root within solver tolerance inputRoot=" +
                    exactRoot + " min=" + min + " max=" + max + " lhs=" + recovered + " rhs=" + exactRoot);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [35] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-returned] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10) but call returned actual=3.4341896575…

---
## [36] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,077 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeInt(0, 0);

        RegulaFalsiSolver fixedSolver = new RegulaFalsiSolver();
        UnivariateRealFunction fixedFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean fixedOracleViolation = false;
        Throwable fixedOracleCause = null;
        String fixedOracleMessage = null;
        try {
            double root = fixedSolver.solve(3624, fixedFunction, 1.0, 10.0);
            fixedOracleViolation = true;
            fixedOracleMessage =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but solve completed normally with root="
                            + root + " (buggy build ground-truth wrong value is 3.4341896575482003)";
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            fixedOracleViolation = true;
            fixedOracleCause = t;
            fixedOracleMessage =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but got "
                            + t.getClass().getName();
        }
        if (fixedOracleViolation) {
            if (fixedOracleCause != null) {
                throw new FuzzerSecurityIssueLow(fixedOracleMessage, fixedOracleCause);
            }
            throw new FuzzerSecurityIssueLow(fixedOracleMessage);
        }

        RegulaFalsiSolver anySideSolver = new RegulaFalsiSolver();
        final double c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        boolean relationViolation = false;
        Throwable relationCause = null;
        String relationMessage = null;
        double rDefault = 0.0;
        double rExplicit = 0.0;
        boolean relationApplicable = true;
        try {
            rDefault = anySideSolver.solve(100, linear, min, max);
            rExplicit = anySideSolver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            relationApplicable = false;
            relationCause = t;
        }
        if (relationApplicable && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
            relationViolation = true;
            relationMessage =
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)"
                            + " inputC=" + c + " min=" + min + " max=" + max
                            + " lhs=" + rDefault + " rhs=" + rExplicit;
        }
        if (relationViolation) {
            if (relationCause != null) {
                throw new FuzzerSecurityIssueLow(relationMessage, relationCause);
            }
            throw new FuzzerSecurityIssueLow(relationMessage);
        }

        RegulaFalsiSolver siblingSolver = new RegulaFalsiSolver();
        final double c2 = data.consumeInt(-1000, 1000);
        final int w2 = data.consumeInt(1, 1000);
        final boolean rootAtLeft = data.consumeBoolean();
        final double min2 = rootAtLeft ? c2 : c2 - w2;
        final double max2 = rootAtLeft ? c2 + w2 : c2;
        final double start2 = rootAtLeft ? min2 : max2;
        UnivariateRealFunction linear2 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c2;
            }
        };

        boolean siblingViolation = false;
        String siblingMessage = null;
        double rNoStart = 0.0;
        double rWithStart = 0.0;
        double rWithStartAny = 0.0;
        boolean siblingApplicable = true;
        try {
            rNoStart = siblingSolver.solve(100, linear2, min2, max2);
            rWithStart = siblingSolver.solve(100, linear2, min2, max2, start2);
            rWithStartAny = siblingSolver.solve(100, linear2, min2, max2, start2, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            siblingApplicable = false;
        }
        if (siblingApplicable) {
            if (Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStart)
                    || Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStartAny)) {
                siblingViolation = true;
                siblingMessage =
                        "[oracle:sibling-overloads] metamorphic violation: equivalent solve overloads must agree on a valid problem with exact endpoint root"
                                + " c=" + c2 + " min=" + min2 + " max=" + max2 + " start=" + start2
                                + " noStart=" + rNoStart + " withStart=" + rWithStart + " withStartAny=" + rWithStartAny;
            }
        }
        if (siblingViolation) {
            throw new FuzzerSecurityIssueLow(siblingMessage);
        }

        RegulaFalsiSolver endpointSolver = new RegulaFalsiSolver();
        final double c3 = data.consumeInt(-1000, 1000);
        final int w3 = data.consumeInt(1, 1000);
        final boolean left = data.consumeBoolean();
        final double min3 = left ? c3 : c3 - w3;
        final double max3 = left ? c3 + w3 : c3;
        final double expectedRoot = c3;
        UnivariateRealFunction linear3 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c3;
            }
        };

        boolean postViolation = false;
        String postMessage = null;
        boolean postApplicable = true;
        double postRoot = 0.0;
        try {
            postRoot = endpointSolver.solve(100, linear3, min3, max3, AllowedSolution.LEFT_SIDE);
        } catch (Throwable t) {
            postApplicable = false;
        }
        if (postApplicable && Double.doubleToLongBits(postRoot) != Double.doubleToLongBits(expectedRoot)) {
            postViolation = true;
            postMessage =
                    "[oracle:endpoint-root] semantic mismatch: doSolve contract returns an endpoint immediately when its function value is exactly zero"
                            + " expected=" + expectedRoot + " actual=" + postRoot
                            + " min=" + min3 + " max=" + max3;
        }
        if (postViolation) {
            throw new FuzzerSecurityIssueLow(postMessage);
        }
    }
}
```

</details>

---
## [37] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but solve completed normally with root=3.4341896575482003 …

---
## [38] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,699 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final int maxEvalSeed = 3624;
        final double min = 1.0;
        final double max = 10.0;
        final double buggyRoot = 3.4341896575482003d;

        boolean defaultThrew = false;
        boolean explicitAnySideThrew = false;

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-default] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException expected) {
            defaultThrew = true;
        } catch (RuntimeException other) {
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max, AllowedSolution.ANY_SIDE);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-anyside] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException expected) {
            explicitAnySideThrew = true;
        } catch (RuntimeException other) {
            return;
        }

        if (defaultThrew != explicitAnySideThrew) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-anyside] semantic mismatch: constructor documents ANY_SIDE as default, so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must reject identically for the same input"
            );
        }

        final int lowerMaxEval = data.consumeInt(1, maxEvalSeed);
        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity] metamorphic violation: same deterministic solve task threw TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " but succeeded at smaller maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max, AllowedSolution.ANY_SIDE);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity-anyside] metamorphic violation: same deterministic solve task with explicit ANY_SIDE threw TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " but succeeded at smaller maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }
    }
}
```

</details>

---
## [39] ⚙️ harness-repair · `attempt_006`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': ['boolean-swallow']}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [40] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `defaultThrew` (literal assignment, no rethrow, the caught e

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,857 chars, 2 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final int maxEvalSeed = 3624;
        final double min = 1.0;
        final double max = 10.0;
        final double buggyRoot = 3.4341896575482003d;

        boolean defaultThrew = false;
        boolean explicitAnySideThrew = false;

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-default] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException expected) {
            defaultThrew = true;
        } catch (RuntimeException other) {
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max, AllowedSolution.ANY_SIDE);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-anyside] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException expected) {
            explicitAnySideThrew = true;
        } catch (RuntimeException other) {
            return;
        }

        if (defaultThrew != explicitAnySideThrew) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-anyside] semantic mismatch: constructor documents ANY_SIDE as default, so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must reject identically for the same input"
            );
        }

        final int lowerMaxEval = data.consumeInt(1, maxEvalSeed);
        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity] metamorphic violation: same deterministic solve task threw TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " but succeeded at smaller maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max, AllowedSolution.ANY_SIDE);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity-anyside] metamorphic violation: same deterministic solve task with explicit ANY_SIDE threw TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " but succeeded at smaller maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `defaultThrew` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 55 is conditioned on `defaultThrew` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~4,353 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final int maxEvalSeed = 3624;
        final double min = 1.0;
        final double max = 10.0;
        final double buggyRoot = 3.4341896575482003d;

        Throwable defaultOutcome = null;
        Throwable anySideOutcome = null;

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-default] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException e) {
            defaultOutcome = e;
        } catch (RuntimeException other) {
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(maxEvalSeed, f, min, max, AllowedSolution.ANY_SIDE);
            if (Math.abs(root - buggyRoot) <= 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testIssue631-anyside] semantic mismatch: expected TooManyEvaluationsException but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException e) {
            anySideOutcome = e;
        } catch (RuntimeException other) {
            return;
        }

        if ((defaultOutcome == null) != (anySideOutcome == null)) {
            if (defaultOutcome != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-vs-anyside] semantic mismatch: BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution, so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree on rejection for the same deterministic input",
                    defaultOutcome
                );
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-anyside] semantic mismatch: BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution, so solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree on rejection for the same deterministic input",
                anySideOutcome
            );
        }

        final int lowerMaxEval = data.consumeInt(1, maxEvalSeed);

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity] metamorphic violation: the same deterministic solve task rejected with TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " in the lifted test, so it cannot succeed with a smaller evaluation budget maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            solver.solve(lowerMaxEval, f, min, max, AllowedSolution.ANY_SIDE);
            throw new RuntimeException(
                "[oracle:maxeval-monotonicity-anyside] metamorphic violation: with explicit ANY_SIDE, the same deterministic solve task rejected with TooManyEvaluationsException at maxEval="
                    + maxEvalSeed + " in the lifted scenario, so it cannot succeed with a smaller evaluation budget maxEval=" + lowerMaxEval
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-repair · `attempt_007`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [43] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testIssue631-default] semantic mismatch: expected TooManyEvaluationsException but got root=3.4341896575482003

---
## [44] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,113 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // Fixed, lifted oracle from RegulaFalsiSolverTest.testIssue631.
        // Contract justification: BracketedUnivariateRealSolver.solve(...) documents that it throws
        // TooManyEvaluationsException if the allowed number of evaluations is exceeded; the failing
        // test pins the exact real-library setup where that must happen.
        final int pinnedMaxEval = 3624 + data.consumeInt(0, 0);
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean oracleIssue631Violated = false;
        String oracleIssue631Msg = null;
        Throwable oracleIssue631Cause = null;
        try {
            UnivariateRealSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(pinnedMaxEval, issue631, 1.0, 10.0);
            oracleIssue631Violated = true;
            oracleIssue631Msg =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException " +
                    "for RegulaFalsiSolver.solve(3624,f,1.0,10.0) but completed normally with root=" + root +
                    " expectedBuggyRoot=3.4341896575482003";
        } catch (TooManyEvaluationsException expected) {
            // expected
        } catch (Throwable t) {
            oracleIssue631Violated = true;
            oracleIssue631Cause = t;
            oracleIssue631Msg =
                    "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException " +
                    "for RegulaFalsiSolver.solve(3624,f,1.0,10.0) but got " + t.getClass().getName();
        }
        if (oracleIssue631Violated) {
            if (oracleIssue631Cause != null) {
                throw new FuzzerSecurityIssueLow(oracleIssue631Msg, oracleIssue631Cause);
            }
            throw new FuzzerSecurityIssueLow(oracleIssue631Msg);
        }

        // Metamorphic/sibling-agreement check from documented default:
        // "For backwards compatibility, all root-finding algorithms must have ANY_SIDE as default
        // for the allowed solutions." Therefore the overload without AllowedSolution and the one
        // with AllowedSolution.ANY_SIDE must agree on the same valid problem.
        // We force a unique exact root at an endpoint, so both correct calls must return that
        // endpoint and any silent state corruption of BaseSecantSolver.allowed becomes observable.
        final int c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : (c - w);
        final double max = leftRoot ? (c + w) : c;
        final UnivariateRealFunction endpointRoot = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        Double defaultAnySideLhs = null;
        Double defaultAnySideRhs = null;
        try {
            RegulaFalsiSolver solver1 = new RegulaFalsiSolver();
            defaultAnySideLhs = solver1.solve(100, endpointRoot, min, max);
            RegulaFalsiSolver solver2 = new RegulaFalsiSolver();
            defaultAnySideRhs = solver2.solve(100, endpointRoot, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(defaultAnySideLhs.doubleValue()) !=
                Double.doubleToLongBits(defaultAnySideRhs.doubleValue())) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) != " +
                    "solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) inputC=" + c +
                    " min=" + min + " max=" + max +
                    " lhs=" + defaultAnySideLhs + " rhs=" + defaultAnySideRhs);
        }

        // Additional sibling-agreement check across the same-name overload taking startValue:
        // all three overloads solve the same bracketed problem; with an exact root at one endpoint,
        // the documented "If one of the bounds is the exact root, return it" path in doSolve()
        // forces the same observable answer regardless of startValue or explicit ANY_SIDE.
        final double startValue = leftRoot ? max : min;
        Double overloadNoStart = null;
        Double overloadWithAllowed = null;
        Double overloadWithStart = null;
        try {
            RegulaFalsiSolver s1 = new RegulaFalsiSolver();
            RegulaFalsiSolver s2 = new RegulaFalsiSolver();
            RegulaFalsiSolver s3 = new RegulaFalsiSolver();
            overloadNoStart = s1.solve(100, endpointRoot, min, max);
            overloadWithAllowed = s2.solve(100, endpointRoot, min, max, AllowedSolution.ANY_SIDE);
            overloadWithStart = s3.solve(100, endpointRoot, min, max, startValue);
        } catch (Throwable t) {
            return;
        }
        long bitsNoStart = Double.doubleToLongBits(overloadNoStart.doubleValue());
        long bitsWithAllowed = Double.doubleToLongBits(overloadWithAllowed.doubleValue());
        long bitsWithStart = Double.doubleToLongBits(overloadWithStart.doubleValue());
        if (bitsNoStart != bitsWithAllowed || bitsNoStart != bitsWithStart) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:solve-overloads-agree] metamorphic violation: overload disagreement " +
                    "c=" + c + " min=" + min + " max=" + max + " startValue=" + startValue +
                    " noStart=" + overloadNoStart +
                    " explicitAnySide=" + overloadWithAllowed +
                    " withStart=" + overloadWithStart);
        }

        // Construct-from-known-answer oracle:
        // we choose the exact root first (c), then build f(x)=x-c with a bracket containing c.
        // For a correct solver, the returned endpoint-root must equal c exactly by the doSolve()
        // contract branch that returns a bound whose function value is exactly zero.
        Double exactRoot = null;
        try {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            exactRoot = solver.solve(100, endpointRoot, min, max);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(exactRoot.doubleValue()) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-exact-root] semantic mismatch: expected exact endpoint root " +
                    c + " but got " + exactRoot + " min=" + min + " max=" + max);
        }
    }
}
```

</details>

---
## [45] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1.0,10.0) but completed…

---
## [46] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,737 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();
        try {
            double root = solver.solve(3624, issue631, 1.0, 10.0);
            if (Double.doubleToLongBits(root) == Double.doubleToLongBits(3.4341896575482003d)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-seed] semantic mismatch: expected TooManyEvaluationsException but solver returned root=" + root
                );
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-seed] semantic mismatch: expected TooManyEvaluationsException but solver returned unexpected root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
            // Expected on the fixed build for the exact lifted test case.
        } catch (RuntimeException other) {
            return;
        }

        final double chosenRoot = data.consumeInt(-1000, 1000) / 16.0;
        double width = data.consumeInt(1, 1000) / 32.0;
        if (width <= 0.0) {
            width = 1.0;
        }
        final double min = chosenRoot - width;
        final double max = chosenRoot + width;
        final double startValue = data.consumeBoolean() ? min : max;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - chosenRoot;
            }
        };

        final RegulaFalsiSolver oracleSolver = new RegulaFalsiSolver();
        try {
            final double r1 = oracleSolver.solve(128, linear, min, max);
            final double r2 = oracleSolver.solve(128, linear, min, max, AllowedSolution.ANY_SIDE);
            final double r3 = oracleSolver.solve(128, linear, min, max, startValue);
            final double r4 = oracleSolver.solve(128, linear, min, max, startValue, AllowedSolution.ANY_SIDE);

            // Contract justification:
            // - BracketedUnivariateRealSolver documents ANY_SIDE as the default allowed solution.
            // - The overloads differ only by explicit start value / allowed solution selection.
            // - For f(x)=x-c, c is the exact zero by construction, so every correct solve overload
            //   must return that same exact root on a valid bracketing interval.
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root] metamorphic violation: solve(maxEval,f,min,max) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r1 + " rhs=" + chosenRoot
                );
            }
            if (Double.doubleToLongBits(r2) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root-allowed] metamorphic violation: solve(maxEval,f,min,max,ANY_SIDE) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r2 + " rhs=" + chosenRoot
                );
            }
            if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root-start] metamorphic violation: solve(maxEval,f,min,max,startValue) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r3 + " rhs=" + chosenRoot
                );
            }
            if (Double.doubleToLongBits(r4) != Double.doubleToLongBits(chosenRoot)) {
                throw new RuntimeException(
                    "[oracle:known-root-start-allowed] metamorphic violation: solve(maxEval,f,min,max,startValue,ANY_SIDE) did not recover constructed exact root input=" +
                    chosenRoot + " lhs=" + r4 + " rhs=" + chosenRoot
                );
            }

            // State-coupling / overload-agreement justification:
            // constructors establish default allowed=ANY_SIDE, and explicit ANY_SIDE should agree
            // with the overloads that rely on that default. A "fix" that merely suppresses the
            // problematic path but corrupts/ignores allowed-solution bookkeeping would break this.
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: default ANY_SIDE overload disagrees with explicit ANY_SIDE input=" +
                    chosenRoot + " lhs=" + r1 + " rhs=" + r2
                );
            }
            if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(r4)) {
                throw new RuntimeException(
                    "[oracle:start-default-any-side] metamorphic violation: startValue overload disagrees with explicit ANY_SIDE input=" +
                    chosenRoot + " lhs=" + r3 + " rhs=" + r4
                );
            }
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r3)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: equivalent solve overloads disagreed on exact linear root input=" +
                    chosenRoot + " lhs=" + r1 + " rhs=" + r3
                );
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        final int fuzzMaxEval = data.consumeInt(1, 5000);
        final double fuzzShift = data.consumeInt(-1000, 1000) / 100.0;
        final double fuzzMin = Math.min(1.0 + fuzzShift, 10.0 + fuzzShift);
        final double fuzzMax = Math.max(1.0 + fuzzShift, 10.0 + fuzzShift);
        final double fuzzStart = data.consumeBoolean() ? fuzzMin : fuzzMax;
        final AllowedSolution[] allowedValues = AllowedSolution.values();
        final AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        try {
            new RegulaFalsiSolver().solve(fuzzMaxEval, issue631, fuzzMin, fuzzMax, fuzzStart, allowed);
        } catch (Throwable ignored) {
        }
    }
}
```

</details>

---
## [47] ⚙️ harness-repair · `attempt_009`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [48] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-seed] semantic mismatch: expected TooManyEvaluationsException but solver returned root=3.4341896575482003

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,463 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Oracle lifted exactly from RegulaFalsiSolverTest.testIssue631:
        // for f(x)=exp(x)-pi^3 on [1,10] with maxEval=3624, solve(...) must throw
        // TooManyEvaluationsException. Returning normally is the documented bug.
        {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean violated = false;
            String message = "completed normally";
            Throwable wrong = null;
            try {
                final double root = solver.solve(3624 + data.consumeInt(0, 0), f, 1.0, 10.0);
                violated = true;
                message = "expected org.apache.commons.math.exception.TooManyEvaluationsException but returned root=" + root;
            } catch (TooManyEvaluationsException expected) {
                // Expected by the failing test.
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                message = "expected org.apache.commons.math.exception.TooManyEvaluationsException but got " + t.getClass().getName();
            }
            if (violated) {
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow("[oracle:issue631-exception] semantic mismatch: " + message, wrong);
                }
                throw new FuzzerSecurityIssueLow("[oracle:issue631-exception] semantic mismatch: " + message);
            }
        }

        // Metamorphic / sibling-agreement check:
        // BracketedUnivariateRealSolver documents ANY_SIDE as the backwards-compatible default.
        // Therefore solve(maxEval,f,min,max) must agree with solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)
        // on the same valid problem. This also checks the constructor-established allowed=ANY_SIDE state.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            Double rDefault = null;
            Double rAny = null;
            Throwable problem = null;
            try {
                rDefault = solver.solve(100, f, min, max);
                rAny = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                problem = t;
            }
            if (problem == null) {
                if (Double.doubleToLongBits(rDefault.doubleValue()) != Double.doubleToLongBits(rAny.doubleValue())) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:default-any-side] metamorphic violation: default solve and explicit ANY_SIDE disagree inputC="
                            + c + " min=" + min + " max=" + max + " lhs=" + rDefault + " rhs=" + rAny);
                }
            }

            // Re-probe after state changes to the same receiver:
            // solve(..., allowedSolution) writes the shared 'allowed' state; a correct implementation's
            // later default solve must still behave as ANY_SIDE, not as the previously written setting.
            final AllowedSolution firstMutation = data.consumeBoolean() ? AllowedSolution.LEFT_SIDE : AllowedSolution.RIGHT_SIDE;
            final AllowedSolution secondMutation = data.consumeBoolean() ? AllowedSolution.ABOVE_SIDE : AllowedSolution.BELOW_SIDE;
            try {
                solver.solve(100, f, min, max, firstMutation);
                solver.solve(100, f, min, max, secondMutation);
            } catch (Throwable t) {
                return;
            }

            final double c2 = data.consumeInt(-1000, 1000);
            final int w2 = data.consumeInt(1, 1000);
            final boolean leftRoot2 = data.consumeBoolean();
            final double min2 = leftRoot2 ? c2 : c2 - w2;
            final double max2 = leftRoot2 ? c2 + w2 : c2;
            final UnivariateRealFunction f2 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c2;
                }
            };

            Double rDefault2 = null;
            Double rAny2 = null;
            Throwable problem2 = null;
            try {
                rDefault2 = solver.solve(100, f2, min2, max2);
                rAny2 = solver.solve(100, f2, min2, max2, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                problem2 = t;
            }
            if (problem2 == null) {
                if (Double.doubleToLongBits(rDefault2.doubleValue()) != Double.doubleToLongBits(rAny2.doubleValue())) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:allowed-state-reset] metamorphic violation: after explicit allowedSolution mutations, default solve no longer matches explicit ANY_SIDE inputC="
                            + c2 + " min=" + min2 + " max=" + max2 + " lhs=" + rDefault2 + " rhs=" + rAny2);
                }
            }
        }

        // Additional sibling-agreement across the same-name solve overloads:
        // On a valid problem with an exact endpoint root, all three public overloads solve the same equation
        // and must return that same exact endpoint root; the result is fixed by construction, not guessed.
        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double start = rootAtLeft ? c + (w / 2.0) : c - (w / 2.0);
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            Double r1 = null;
            Double r2 = null;
            Double r3 = null;
            Throwable problem = null;
            try {
                r1 = solver.solve(100, f, min, max);
                r2 = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
                r3 = solver.solve(100, f, min, max, start);
            } catch (Throwable t) {
                problem = t;
            }
            if (problem == null) {
                long expectedBits = Double.doubleToLongBits(c);
                if (Double.doubleToLongBits(r1.doubleValue()) != expectedBits) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:endpoint-root-overload1] semantic mismatch: expected root=" + c + " actual=" + r1
                            + " min=" + min + " max=" + max + " start=" + start);
                }
                if (Double.doubleToLongBits(r2.doubleValue()) != expectedBits) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:endpoint-root-overload2] semantic mismatch: expected root=" + c + " actual=" + r2
                            + " min=" + min + " max=" + max + " start=" + start);
                }
                if (Double.doubleToLongBits(r3.doubleValue()) != expectedBits) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:endpoint-root-overload3] semantic mismatch: expected root=" + c + " actual=" + r3
                            + " min=" + min + " max=" + max + " start=" + start);
                }
            }
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-exception] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but returned root=3.43418…

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,757 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            UnivariateRealSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(3624, issue631Function, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException from RegulaFalsiSolver.solve(3624,f,1,10) but got root=" +
                root + " expectedBuggyValue=3.4341896575482003");
        } catch (TooManyEvaluationsException expected) {
            // Expected per lifted test oracle.
        } catch (Throwable t) {
            return;
        }

        final double chosenRoot = data.consumeInt(-1000, 1000);
        final double width = data.consumeInt(1, 1000);
        final double min = chosenRoot;
        final double max = chosenRoot + width;
        final double start = min + (max - min) * 0.5;

        final UnivariateRealFunction endpointRootFunction = new UnivariateRealFunction() {
            public double value(double x) {
                return x - chosenRoot;
            }
        };

        final RegulaFalsiSolver solver2 = new RegulaFalsiSolver();

        try {
            // Contract cited from BracketedUnivariateRealSolver: solvers that require bracketing
            // "should be able to handle the case where one of the endpoints is itself a root."
            // Here min is exactly a root, so every correct overload must return that exact endpoint.
            double explicitAny = solver2.solve(32, endpointRootFunction, min, max, AllowedSolution.ANY_SIDE);
            if (Double.doubleToLongBits(explicitAny) != Double.doubleToLongBits(min)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-any] metamorphic violation: endpoint root with explicit ANY_SIDE must return min input=" +
                    min + "," + max + " actual=" + explicitAny + " expected=" + min);
            }

            // Same contract: exact endpoint root is acceptable regardless of allowed side.
            double explicitLeft = solver2.solve(32, endpointRootFunction, min, max, start, AllowedSolution.LEFT_SIDE);
            if (Double.doubleToLongBits(explicitLeft) != Double.doubleToLongBits(min)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-left] metamorphic violation: endpoint root with explicit LEFT_SIDE must return min input=" +
                    min + "," + max + "," + start + " actual=" + explicitLeft + " expected=" + min);
            }

            // State-coupling/post-condition check:
            // BaseSecantSolver constructors establish default allowed == ANY_SIDE, and backwards-compatibility
            // contract says ANY_SIDE is the default. After a prior solve call changed receiver state via
            // solve(..., allowedSolution), the overload without allowedSolution must still behave as the
            // default ANY_SIDE overload on the same valid problem.
            double defaultOverload = solver2.solve(32, endpointRootFunction, min, max, start);
            if (Double.doubleToLongBits(defaultOverload) != Double.doubleToLongBits(min)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-any-after-mutation] metamorphic violation: default overload must preserve ANY_SIDE semantics after prior explicit-allowed solve input=" +
                    min + "," + max + "," + start + " actual=" + defaultOverload + " expected=" + min);
            }

            double explicitAnyWithStart = solver2.solve(32, endpointRootFunction, min, max, start, AllowedSolution.ANY_SIDE);
            if (Double.doubleToLongBits(defaultOverload) != Double.doubleToLongBits(explicitAnyWithStart)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement] metamorphic violation: equivalent overloads must agree on exact endpoint-root problem input=" +
                    min + "," + max + "," + start + " lhs=" + defaultOverload + " rhs=" + explicitAnyWithStart);
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-repair · `attempt_011`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [53] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected TooManyEvaluationsException from RegulaFalsiSolver.solve(3624,f,1,10) but got root=3.434189657548…

---
## [54] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,732 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted oracle from RegulaFalsiSolverTest.testIssue631:
        // For this exact setup, BracketedUnivariateRealSolver.solve(...) must throw
        // TooManyEvaluationsException when maxEval is exceeded. The buggy build instead
        // returns normally with the specific wrong root value from the failing test.
        UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        RegulaFalsiSolver issue631Solver = new RegulaFalsiSolver();
        boolean issue631Violated = false;
        String issue631Message = "completed normally";
        try {
            double root = issue631Solver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            issue631Violated = true;
            issue631Message = "completed normally with root=" + root;
            if (Double.doubleToLongBits(root) == Double.doubleToLongBits(3.4341896575482003d)) {
                issue631Message += " (matches buggy-build ground-truth wrong value)";
            }
        } catch (TooManyEvaluationsException expected) {
            // expected
        } catch (Throwable t) {
            issue631Violated = true;
            issue631Message = "threw wrong exception " + t.getClass().getName() + ": " + String.valueOf(t.getMessage());
        }
        if (issue631Violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10), but " + issue631Message);
        }

        // Metamorphic relation from the documented default: "all root-finding algorithms
        // must have ANY_SIDE as default for the allowed solutions." Therefore the overload
        // without AllowedSolution must agree with the overload using AllowedSolution.ANY_SIDE
        // on the same valid problem. We construct a linear function with a root exactly at
        // one endpoint so the expected answer is forced by the API, not guessed.
        RegulaFalsiSolver anySideSolver = new RegulaFalsiSolver();
        final double c = (double) data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        UnivariateRealFunction endpointRoot = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        Double defaultResult = null;
        try {
            defaultResult = Double.valueOf(anySideSolver.solve(100, endpointRoot, min, max));
        } catch (Throwable t) {
            return;
        }

        Double explicitAnySideResult = null;
        try {
            explicitAnySideResult = Double.valueOf(anySideSolver.solve(100, endpointRoot, min, max, AllowedSolution.ANY_SIDE));
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(defaultResult.doubleValue()) != Double.doubleToLongBits(explicitAnySideResult.doubleValue())) {
            throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) on the same valid problem; inputRoot=" + c + " min=" + min + " max=" + max + " lhs=" + defaultResult + " rhs=" + explicitAnySideResult);
        }

        // Sibling-agreement check across same-name overloads: the overload taking startValue
        // but no AllowedSolution is documented to solve the same problem with the default
        // ANY_SIDE behavior, so on this endpoint-root problem it must return the same exact
        // root as the other overloads. This also re-probes after the previous state-changing
        // explicit-allowed call to ensure solver state does not corrupt later default solves.
        final double startValue = leftRoot ? (c + (w / 2.0)) : (c - (w / 2.0));
        Double startValueResult = null;
        try {
            startValueResult = Double.valueOf(anySideSolver.solve(100, endpointRoot, min, max, startValue));
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(defaultResult.doubleValue()) != Double.doubleToLongBits(startValueResult.doubleValue())) {
            throw new FuzzerSecurityIssueLow("[oracle:overload-agreement] metamorphic violation: default solve overloads must agree on the same valid endpoint-root problem; inputRoot=" + c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + defaultResult + " rhs=" + startValueResult);
        }

        // Post-condition on the real API result: when one endpoint is an exact root, doSolve()
        // returns that bound immediately ("If one of the bounds is the exact root, return it.").
        // A patch that merely suppresses throwing or corrupts state must still preserve this
        // documented observable. Because we constructed f(x)=x-c and chose c as an endpoint,
        // the exact expected root is trusted: it is the endpoint we built first.
        if (Double.doubleToLongBits(defaultResult.doubleValue()) != Double.doubleToLongBits(c)) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root] semantic mismatch: endpoint exact root must be returned; expected=" + c + " actual=" + defaultResult + " min=" + min + " max=" + max);
        }
        if (Double.doubleToLongBits(explicitAnySideResult.doubleValue()) != Double.doubleToLongBits(c)) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-explicit] semantic mismatch: endpoint exact root must be returned for explicit ANY_SIDE; expected=" + c + " actual=" + explicitAnySideResult + " min=" + min + " max=" + max);
        }
        if (Double.doubleToLongBits(startValueResult.doubleValue()) != Double.doubleToLongBits(c)) {
            throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-start] semantic mismatch: endpoint exact root must be returned for startValue overload; expected=" + c + " actual=" + startValueResult + " min=" + min + " max=" + max + " startValue=" + startValue);
        }
    }
}
```

</details>

---
## [55] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,…

---
## [56] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,089 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final double ISSUE631_ROOT = 3.4341896575482003d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            double root = new RegulaFalsiSolver().solve(3624, f, 1.0d, 10.0d);
            if (Double.doubleToLongBits(root) != Double.doubleToLongBits(ISSUE631_ROOT)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10), but call returned a different value root=" + root
                );
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10), but call returned root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        double leftDelta = 0.1d + (data.consumeInt(0, 4900) / 1000.0d);
        double rightDelta = 0.1d + (data.consumeInt(0, 4900) / 1000.0d);
        double min = ISSUE631_ROOT - leftDelta;
        double max = ISSUE631_ROOT + rightDelta;
        double selector = data.consumeInt(0, 10000) / 10000.0d;
        double start = min + (max - min) * selector;
        int maxEval = data.consumeInt(50, 5000);

        try {
            RegulaFalsiSolver explicitAny = new RegulaFalsiSolver();
            RegulaFalsiSolver implicitAny = new RegulaFalsiSolver();
            double lhs = explicitAny.solve(maxEval, f, min, max, start, AllowedSolution.ANY_SIDE);
            double rhs = implicitAny.solve(maxEval, f, min, max, start);
            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
                throw new RuntimeException(
                    "[oracle:any-side-overload] metamorphic violation: solve(maxEval,f,min,max,start,ANY_SIDE) must equal solve(maxEval,f,min,max,start) because the solver contract says ANY_SIDE is the default allowed solution; input=min=" +
                    min + ",max=" + max + ",start=" + start + ",maxEval=" + maxEval + " lhs=" + lhs + " rhs=" + rhs
                );
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:any-side-overload]")) {
                throw e;
            }
            return;
        }

        try {
            RegulaFalsiSolver mutated = new RegulaFalsiSolver();
            mutated.solve(maxEval, f, min, max, start, AllowedSolution.LEFT_SIDE);
            double afterMutation = mutated.solve(maxEval, f, min, max, start);
            double freshDefault = new RegulaFalsiSolver().solve(maxEval, f, min, max, start);
            if (Double.doubleToLongBits(afterMutation) != Double.doubleToLongBits(freshDefault)) {
                throw new RuntimeException(
                    "[oracle:default-reset] metamorphic violation: the no-AllowedSolution overload must still behave as ANY_SIDE even after a prior call changed solver state, because backwards compatibility requires ANY_SIDE as the default allowed solution; input=min=" +
                    min + ",max=" + max + ",start=" + start + ",maxEval=" + maxEval + " lhs=" + afterMutation + " rhs=" + freshDefault
                );
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:default-reset]")) {
                throw e;
            }
            return;
        }
    }
}
```

</details>

---
## [57] ⚙️ harness-repair · `attempt_013`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [58] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624,f,1,10), but call returned root=3.43418965754820…

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,560 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean violated = false;
            double returned = Double.NaN;
            Throwable wrong = null;

            try {
                returned = solver.solve(3624 + data.consumeInt(0, 0), f, 1.0, 10.0);
                violated = true;
            } catch (TooManyEvaluationsException expected) {
            } catch (Throwable t) {
                violated = true;
                wrong = t;
            }

            if (violated) {
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but got "
                            + wrong.getClass().getName(),
                        wrong);
                }
                final double buggyRoot = 3.4341896575482003;
                if (Double.doubleToLongBits(returned) != Double.doubleToLongBits(buggyRoot)) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException; call returned "
                            + returned + " (setup divergence from failing test, expected buggy return " + buggyRoot + ")");
                }
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but call returned buggy root "
                        + returned);
            }
        }

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double r1;
            double r2;
            try {
                r1 = solver.solve(100, f, min, max);
                r2 = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must match solve(maxEval,f,min,max,ANY_SIDE) "
                        + "for the same valid problem; c=" + c + " min=" + min + " max=" + max + " lhs=" + r1 + " rhs=" + r2);
            }
        }

        {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double startValue = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double noStart;
            double withStart;
            double withStartAny;
            try {
                noStart = solver.solve(100, f, min, max);
                withStart = solver.solve(100, f, min, max, startValue);
                withStartAny = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            /* Contract justification:
               - Constructors initialize allowed to ANY_SIDE.
               - The overload without AllowedSolution is documented to use ANY_SIDE for backwards compatibility.
               - For a valid problem whose root is exactly at one endpoint, all overloads must return that exact endpoint root. */
            if (Double.doubleToLongBits(noStart) != Double.doubleToLongBits(c)
                    || Double.doubleToLongBits(withStart) != Double.doubleToLongBits(c)
                    || Double.doubleToLongBits(withStartAny) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:endpoint-root-overloads] metamorphic violation: endpoint root must be preserved across solve overloads "
                        + "c=" + c + " min=" + min + " max=" + max + " startValue=" + startValue
                        + " noStart=" + noStart + " withStart=" + withStart + " withStartAny=" + withStartAny);
            }
        }
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but call returned buggy root 3.4341896575482003

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,161 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final double expectedRoot = 3.4341896575482003;
        final double expectedTol = 1e-15;

        {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            try {
                final double root = solver.solve(3624, f, 1.0, 10.0);
                if (Math.abs(root - expectedRoot) <= expectedTol) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root
                    );
                }
                return;
            } catch (TooManyEvaluationsException expected) {
                // Trusted oracle lifted verbatim from RegulaFalsiSolverTest.testIssue631:
                // the exact public call solver.solve(3624, f, 1, 10) must reject by throwing
                // TooManyEvaluationsException instead of silently returning the known root.
            } catch (RuntimeException other) {
                return;
            }
        }

        final int extraBudget = data.consumeInt(4000, 12000);

        try {
            final RegulaFalsiSolver defaultSolver = new RegulaFalsiSolver();
            final RegulaFalsiSolver explicitAnySideSolver = new RegulaFalsiSolver();

            final double lhs = defaultSolver.solve(extraBudget, f, 1.0, 10.0);
            final double rhs = explicitAnySideSolver.solve(
                extraBudget,
                f,
                1.0,
                10.0,
                AllowedSolution.ANY_SIDE
            );

            // Documented contract: BracketedUnivariateRealSolver states ANY_SIDE is the
            // backwards-compatible default, and BaseSecantSolver constructors set
            // allowed = AllowedSolution.ANY_SIDE. Therefore the default overload and the
            // explicit ANY_SIDE overload must agree on the same interval/function.
            // A patch that silently breaks bookkeeping/state instead of throwing can
            // violate this observable post-condition even when no exception occurs.
            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: default overload must equal explicit ANY_SIDE input=maxEval="
                        + extraBudget + ",min=1.0,max=10.0 lhs=" + lhs + " rhs=" + rhs
                );
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [62] ⚙️ harness-repair · `attempt_015`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [63] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected TooManyEvaluationsException but solve returned root=3.4341896575482003

---
## [64] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,246 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            final org.apache.commons.math.analysis.UnivariateRealFunction f =
                    new org.apache.commons.math.analysis.UnivariateRealFunction() {
                        public double value(double x) {
                            return Math.exp(x) - Math.pow(Math.PI, 3.0);
                        }
                    };

            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final int maxEval = 3624 + data.consumeInt(0, 0);

            boolean violation = false;
            double returned = Double.NaN;
            Throwable wrong = null;

            try {
                returned = solver.solve(maxEval, f, 1.0, 10.0);
                violation = true;
            } catch (org.apache.commons.math.exception.TooManyEvaluationsException expected) {
                // Ground-truth lifted oracle from RegulaFalsiSolverTest.testIssue631:
                // for this exact input the correct behavior is to throw TooManyEvaluationsException.
            } catch (Throwable t) {
                violation = true;
                wrong = t;
            }

            if (violation) {
                if (wrong != null) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but got "
                                    + wrong.getClass().getName(),
                            wrong);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but solve returned "
                                + returned);
            }
        }

        {
            RegulaFalsiSolver solver;
            org.apache.commons.math.analysis.UnivariateRealFunction f;
            double c;
            double min;
            double max;
            double startValue;
            double rDefault;
            double rExplicitAny;
            double rStartAny;

            try {
                solver = new RegulaFalsiSolver();
                c = data.consumeInt(-1000, 1000);
                int w = data.consumeInt(1, 1000);
                boolean leftRoot = data.consumeBoolean();
                min = leftRoot ? c : c - w;
                max = leftRoot ? c + w : c;
                startValue = min + (max - min) / 2.0;
                final double root = c;
                f = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                    public double value(double x) {
                        return x - root;
                    }
                };
            } catch (Throwable t) {
                return;
            }

            try {
                rDefault = solver.solve(100, f, min, max);
                rExplicitAny = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
                rStartAny = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            // Documented contract: all root-finding algorithms must have ANY_SIDE as default.
            // Therefore the default overload and the explicit ANY_SIDE overloads must agree.
            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) != solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE)"
                                + " min=" + min + " max=" + max + " default=" + rDefault + " explicitAny=" + rExplicitAny);
            }

            // Same documented job, same logical input; on an endpoint root, start value is irrelevant.
            if (Double.doubleToLongBits(rExplicitAny) != Double.doubleToLongBits(rStartAny)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:sibling-overloads] metamorphic violation: explicit ANY_SIDE overloads disagree"
                                + " min=" + min + " max=" + max + " startValue=" + startValue
                                + " noStart=" + rExplicitAny + " withStart=" + rStartAny);
            }
        }

        {
            RegulaFalsiSolver solver;
            double c;
            double min;
            double max;
            double expected;
            org.apache.commons.math.analysis.UnivariateRealFunction f;

            try {
                solver = new RegulaFalsiSolver();
                c = data.consumeInt(-1000, 1000);
                int w = data.consumeInt(1, 1000);
                boolean rootOnLeft = data.consumeBoolean();
                min = rootOnLeft ? c : c - w;
                max = rootOnLeft ? c + w : c;
                expected = c;
                final double root = c;
                f = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                    public double value(double x) {
                        return x - root;
                    }
                };
            } catch (Throwable t) {
                return;
            }

            int mutations = data.consumeInt(1, 5);
            for (int i = 0; i < mutations; i++) {
                AllowedSolution chosen = AllowedSolution.values()[
                        data.consumeInt(0, AllowedSolution.values().length - 1)];
                try {
                    solver.solve(100, f, min, max, chosen);
                } catch (Throwable t) {
                    return;
                }

                double afterDefault;
                double afterExplicitAny;
                try {
                    afterDefault = solver.solve(100, f, min, max);
                    afterExplicitAny = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
                } catch (Throwable t) {
                    return;
                }

                // Hidden-state/state-coupling check: solve(..., allowedSolution) writes BaseSecantSolver.allowed,
                // while doSolve reads it. The constructors establish ANY_SIDE as the default, and the default
                // solve overload is documented to behave as ANY_SIDE regardless of prior explicit calls.
                // A patch that leaves stale receiver state would make the post-mutation default call differ.
                if (Double.doubleToLongBits(afterDefault) != Double.doubleToLongBits(afterExplicitAny)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:allowed-state-reset] metamorphic violation: default overload disagrees with explicit ANY_SIDE after prior state-changing solve"
                                    + " mutationIndex=" + i + " previousAllowed=" + chosen
                                    + " min=" + min + " max=" + max
                                    + " default=" + afterDefault + " explicitAny=" + afterExplicitAny);
                }

                // Oracle from known answer: the input is constructed from the chosen root c, so every correct
                // solver must return exactly that endpoint root on this problem.
                if (Double.doubleToLongBits(afterDefault) != Double.doubleToLongBits(expected)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:known-endpoint-root] semantic mismatch: constructed endpoint root not recovered"
                                    + " mutationIndex=" + i + " previousAllowed=" + chosen
                                    + " expected=" + expected + " actual=" + afterDefault
                                    + " min=" + min + " max=" + max);
                }
            }
        }
    }
}
```

</details>

---
## [65] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but solve returned 3.43418965754820…

---
## [66] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,174 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final boolean runIssue631 = data.consumeBoolean();

        if (runIssue631) {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            try {
                final double root = solver.solve(3624, f, 1.0, 10.0);
                if (Math.abs(root - 3.4341896575482003) <= 1e-15) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:issue631-no-exception] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-wrong-return] semantic mismatch: expected TooManyEvaluationsException but solve returned unexpected root=" + root);
            } catch (TooManyEvaluationsException expected) {
            } catch (RuntimeException other) {
                return;
            }
        }

        final double target = data.consumeInt(-1000, 1000) / 100.0;
        final double min = target - 1.0;
        final double max = target + 1.0;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - target;
            }
        };

        try {
            final RegulaFalsiSolver defaultSolver = new RegulaFalsiSolver();
            final double implicitAny = defaultSolver.solve(32, linear, min, max);

            final RegulaFalsiSolver explicitSolver = new RegulaFalsiSolver();
            final double explicitAny = explicitSolver.solve(32, linear, min, max, AllowedSolution.ANY_SIDE);

            if (Double.doubleToLongBits(implicitAny) != Double.doubleToLongBits(explicitAny)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because bracketed solvers document ANY_SIDE as the default allowed solution; inputTarget="
                        + target + " lhs=" + implicitAny + " rhs=" + explicitAny);
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        }

        try {
            final RegulaFalsiSolver statefulSolver = new RegulaFalsiSolver();
            final UnivariateRealFunction quadratic = new UnivariateRealFunction() {
                public double value(double x) {
                    return x * x - 2.0;
                }
            };

            statefulSolver.solve(64, quadratic, 0.0, 2.0, AllowedSolution.LEFT_SIDE);

            final double afterMutationImplicit = statefulSolver.solve(64, quadratic, 0.0, 2.0);
            final double freshExplicitAny = new RegulaFalsiSolver().solve(64, quadratic, 0.0, 2.0, AllowedSolution.ANY_SIDE);

            if (Double.doubleToLongBits(afterMutationImplicit) != Double.doubleToLongBits(freshExplicitAny)) {
                throw new RuntimeException(
                    "[oracle:allowed-state-reset] metamorphic violation: implicit solve must use the documented default ANY_SIDE regardless of prior solve(...) calls that changed allowed-solution state; lhs="
                        + afterMutationImplicit + " rhs=" + freshExplicitAny);
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
        }
    }
}
```

</details>

---
## [67] ⚙️ harness-repair · `attempt_017`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [68] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-no-exception] semantic mismatch: expected TooManyEvaluationsException but solve returned root=3.4341896575482003

---
## [69] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,532 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean violated = false;
            String msg = "completed normally";
            double returned = Double.NaN;
            Throwable wrong = null;
            try {
                returned = solver.solve(3624, issue631Function, 1.0, 10.0);
                violated = true;
                msg = "completed normally with root=" + returned;
            } catch (TooManyEvaluationsException expected) {
                violated = false;
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                msg = "threw wrong exception " + t.getClass().getName() + ": " + t.getMessage();
            }
            if (violated) {
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but " + msg, wrong);
                }
                throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but " + msg);
            }
        }

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            boolean skip = false;
            double rDefault = Double.NaN;
            double rAnySide = Double.NaN;
            try {
                rDefault = solver.solve(100, linear, min, max);
                rAnySide = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                skip = true;
            }
            if (!skip && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAnySide)) {
                throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because ANY_SIDE is the documented default; min=" + min + " max=" + max + " c=" + c + " default=" + rDefault + " explicitAnySide=" + rAnySide);
            }
        }

        {
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double startValue = data.consumeBoolean() ? min : max;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            boolean skip = false;
            double r1 = Double.NaN;
            double r2 = Double.NaN;
            double r3 = Double.NaN;
            try {
                RegulaFalsiSolver s1 = new RegulaFalsiSolver();
                RegulaFalsiSolver s2 = new RegulaFalsiSolver();
                RegulaFalsiSolver s3 = new RegulaFalsiSolver();
                r1 = s1.solve(100, linear, min, max);
                r2 = s2.solve(100, linear, min, max, startValue);
                r3 = s3.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                skip = true;
            }
            if (!skip) {
                // Contract justification: the endpoint is an exact root, and doSolve() returns an exact endpoint root immediately;
                // also BaseSecantSolver constructors/setters establish ANY_SIDE as default, so all solve overloads for the same valid
                // problem must agree on that forced answer. A patch that only suppresses looping/throwing but corrupts allowed-state
                // or overload behavior would violate this observable post-condition.
                if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(c)) {
                    throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload1] semantic mismatch: exact endpoint root must be returned; expected=" + c + " actual=" + r1 + " min=" + min + " max=" + max);
                }
                if (Double.doubleToLongBits(r2) != Double.doubleToLongBits(c)) {
                    throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload2] semantic mismatch: exact endpoint root must be returned even with explicit startValue; expected=" + c + " actual=" + r2 + " min=" + min + " max=" + max + " startValue=" + startValue);
                }
                if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(c)) {
                    throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload3] semantic mismatch: exact endpoint root must be returned with explicit ANY_SIDE; expected=" + c + " actual=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
                }
                if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2) ||
                    Double.doubleToLongBits(r1) != Double.doubleToLongBits(r3)) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-overloads] metamorphic violation: solve overloads must agree on the same valid problem with an exact endpoint root; c=" + c + " r1=" + r1 + " r2=" + r2 + " r3=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
                }
            }
        }
    }
}
```

</details>

---
## [70] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `violated` (literal assignment, no rethrow, the caught excep

---
## [71] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,682 chars, 2 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            boolean violated = false;
            String msg = "completed normally";
            double returned = Double.NaN;
            Throwable wrong = null;
            try {
                returned = solver.solve(3624, issue631Function, 1.0, 10.0);
                violated = true;
                msg = "completed normally with root=" + returned;
            } catch (TooManyEvaluationsException expected) {
                violated = false;
            } catch (Throwable t) {
                violated = true;
                wrong = t;
                msg = "threw wrong exception " + t.getClass().getName() + ": " + t.getMessage();
            }
            if (violated) {
                if (wrong != null) {
                    throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but " + msg, wrong);
                }
                throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but " + msg);
            }
        }

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            boolean skip = false;
            double rDefault = Double.NaN;
            double rAnySide = Double.NaN;
            try {
                rDefault = solver.solve(100, linear, min, max);
                rAnySide = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                skip = true;
            }
            if (!skip && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAnySide)) {
                throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because ANY_SIDE is the documented default; min=" + min + " max=" + max + " c=" + c + " default=" + rDefault + " explicitAnySide=" + rAnySide);
            }
        }

        {
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double startValue = data.consumeBoolean() ? min : max;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            boolean skip = false;
            double r1 = Double.NaN;
            double r2 = Double.NaN;
            double r3 = Double.NaN;
            try {
                RegulaFalsiSolver s1 = new RegulaFalsiSolver();
                RegulaFalsiSolver s2 = new RegulaFalsiSolver();
                RegulaFalsiSolver s3 = new RegulaFalsiSolver();
                r1 = s1.solve(100, linear, min, max);
                r2 = s2.solve(100, linear, min, max, startValue);
                r3 = s3.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                skip = true;
            }
            if (!skip) {
                // Contract justification: the endpoint is an exact root, and doSolve() returns an exact endpoint root immediately;
                // also BaseSecantSolver constructors/setters establish ANY_SIDE as default, so all solve overloads for the same valid
                // problem must agree on that forced answer. A patch that only suppresses looping/throwing but corrupts allowed-state
                // or overload behavior would violate this observable post-condition.
                if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(c)) {
                    throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload1] semantic mismatch: exact endpoint root must be returned; expected=" + c + " actual=" + r1 + " min=" + min + " max=" + max);
                }
                if (Double.doubleToLongBits(r2) != Double.doubleToLongBits(c)) {
                    throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload2] semantic mismatch: exact endpoint root must be returned even with explicit startValue; expected=" + c + " actual=" + r2 + " min=" + min + " max=" + max + " startValue=" + startValue);
                }
                if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(c)) {
                    throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload3] semantic mismatch: exact endpoint root must be returned with explicit ANY_SIDE; expected=" + c + " actual=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
                }
                if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2) ||
                    Double.doubleToLongBits(r1) != Double.doubleToLongBits(r3)) {
                    throw new FuzzerSecurityIssueLow("[oracle:sibling-overloads] metamorphic violation: solve overloads must agree on the same valid problem with an exact endpoint root; c=" + c + " r1=" + r1 + " r2=" + r2 + " r3=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
                }
            }
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `violated` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 37 is conditioned on `violated` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,216 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            Throwable wrong = null;
            boolean completedNormally = false;
            double returned = Double.NaN;
            try {
                returned = solver.solve(3624, issue631Function, 1.0, 10.0);
                completedNormally = true;
            } catch (TooManyEvaluationsException expected) {
                // Expected per the lifted oracle from RegulaFalsiSolverTest.testIssue631.
            } catch (Throwable t) {
                wrong = t;
            }
            if (wrong != null) {
                throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but got " + wrong.getClass().getName() + ": " + wrong.getMessage(), wrong);
            }
            if (completedNormally) {
                throw new FuzzerSecurityIssueLow("[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but call completed normally with root=" + returned);
            }
        }

        {
            RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double rDefault;
            double rAnySide;
            try {
                rDefault = solver.solve(100, linear, min, max);
                rAnySide = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAnySide)) {
                throw new FuzzerSecurityIssueLow("[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because ANY_SIDE is the documented default; min=" + min + " max=" + max + " c=" + c + " default=" + rDefault + " explicitAnySide=" + rAnySide);
            }
        }

        {
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean rootAtLeft = data.consumeBoolean();
            final double min = rootAtLeft ? c : c - w;
            final double max = rootAtLeft ? c + w : c;
            final double startValue = data.consumeBoolean() ? min : max;
            final UnivariateRealFunction linear = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double r1;
            double r2;
            double r3;
            try {
                RegulaFalsiSolver s1 = new RegulaFalsiSolver();
                RegulaFalsiSolver s2 = new RegulaFalsiSolver();
                RegulaFalsiSolver s3 = new RegulaFalsiSolver();
                r1 = s1.solve(100, linear, min, max);
                r2 = s2.solve(100, linear, min, max, startValue);
                r3 = s3.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            // Contract justification: doSolve() returns an exact endpoint root immediately when f(min)==0 or f(max)==0.
            // For f(x)=x-c with c placed exactly at one endpoint, every correct overload must therefore return c.
            // This also checks the hidden shared state established by BaseSecantSolver constructors and solve(..., AllowedSolution):
            // ANY_SIDE is documented as the default, so the overloads must agree observably on the same valid problem.
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload1] semantic mismatch: exact endpoint root must be returned; expected=" + c + " actual=" + r1 + " min=" + min + " max=" + max);
            }
            if (Double.doubleToLongBits(r2) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload2] semantic mismatch: exact endpoint root must be returned even with explicit startValue; expected=" + c + " actual=" + r2 + " min=" + min + " max=" + max + " startValue=" + startValue);
            }
            if (Double.doubleToLongBits(r3) != Double.doubleToLongBits(c)) {
                throw new FuzzerSecurityIssueLow("[oracle:endpoint-root-overload3] semantic mismatch: exact endpoint root must be returned with explicit ANY_SIDE; expected=" + c + " actual=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
            }
            if (Double.doubleToLongBits(r1) != Double.doubleToLongBits(r2) ||
                Double.doubleToLongBits(r1) != Double.doubleToLongBits(r3)) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-overloads] metamorphic violation: solve overloads must agree on the same valid problem with an exact endpoint root; c=" + c + " r1=" + r1 + " r2=" + r2 + " r3=" + r3 + " min=" + min + " max=" + max + " startValue=" + startValue);
            }
        }
    }
}
```

</details>

---
## [72] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException for RegulaFalsiSolver.solve(3624,f,1,10) but call completed no…

---
## [73] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,883 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final UnivariateRealSolver solver = new RegulaFalsiSolver();
        try {
            double root = solver.solve(3624, issue631Function, 1, 10);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        final double knownRoot = 2.0;
        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - knownRoot;
            }
        };

        double min = 0.0;
        double max = 4.0;
        double start = 1.0 + (data.consumeInt(0, 2000) / 1000.0);

        RegulaFalsiSolver s1 = new RegulaFalsiSolver();
        RegulaFalsiSolver s2 = new RegulaFalsiSolver();

        try {
            double lhs = s1.solve(32, linear, min, max, start);
            double rhs = s2.solve(32, linear, min, max, start, AllowedSolution.ANY_SIDE);

            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
                throw new RuntimeException(
                    "[oracle:any-side-overload] metamorphic violation: solve(maxEval,f,min,max,startValue) must agree with " +
                    "solve(maxEval,f,min,max,startValue,AllowedSolution.ANY_SIDE) because BaseSecantSolver constructors set " +
                    "default allowed solution to ANY_SIDE; input=start=" + start + " lhs=" + lhs + " rhs=" + rhs
                );
            }

            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(knownRoot)) {
                throw new RuntimeException(
                    "[oracle:constructed-root] metamorphic violation: for f(x)=x-c, the real root is the chosen constant c; " +
                    "input=c=" + knownRoot + " result=" + lhs
                );
            }
        } catch (RuntimeException ex) {
            String name = ex.getClass().getName();
            if (name.indexOf("Math") >= 0 || name.indexOf("Evaluation") >= 0 || name.indexOf("Convergence") >= 0) {
                return;
            }
            throw ex;
        }
    }
}
```

</details>

---
## [74] ⚙️ harness-repair · `attempt_020`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [75] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected TooManyEvaluationsException but solve returned root=3.4341896575482003

---
## [76] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,429 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        RegulaFalsiSolver fixedSolver = new RegulaFalsiSolver();

        if (fixedSolver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-default-allowed] semantic mismatch: expected ANY_SIDE after construction actual=" + fixedSolver.allowed
            );
        }
        if (fixedSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-method] semantic mismatch: expected REGULA_FALSI after construction actual=" + fixedSolver.method
            );
        }

        final int fixedMaxEval = 3624 + data.consumeInt(0, 0);
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean fixedViolation = false;
        String fixedMessage = null;
        Throwable fixedCause = null;
        double fixedRoot = Double.NaN;
        try {
            fixedRoot = fixedSolver.solve(fixedMaxEval, issue631Function, 1.0, 10.0);
            fixedViolation = true;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but completed normally with root=" + fixedRoot;
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            fixedViolation = true;
            fixedCause = t;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but caught " + t.getClass().getName();
        }
        if (fixedViolation) {
            if (fixedCause != null) {
                throw new FuzzerSecurityIssueLow(fixedMessage, fixedCause);
            }
            throw new FuzzerSecurityIssueLow(fixedMessage);
        }

        if (fixedSolver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:post-issue631-allowed] semantic mismatch: expected ANY_SIDE after solve(maxEval,f,min,max) actual=" + fixedSolver.allowed
            );
        }
        if (fixedSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:post-issue631-method] semantic mismatch: expected REGULA_FALSI after solve actual=" + fixedSolver.method
            );
        }

        final int c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        final double startValue = (min + max) / 2.0;
        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        RegulaFalsiSolver solver = new RegulaFalsiSolver();

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-default-allowed-2] semantic mismatch: expected ANY_SIDE after construction actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-method-2] semantic mismatch: expected REGULA_FALSI after construction actual=" + solver.method
            );
        }

        double rDefault;
        try {
            rDefault = solver.solve(100, linear, min, max);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-overload-state] semantic mismatch: solve(maxEval,f,min,max) should leave/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-overload-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        double rExplicitAny;
        try {
            rExplicitAny = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-state] semantic mismatch: solve(...,ANY_SIDE) should write/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " lhs=" + rDefault + " rhs=" + rExplicitAny
            );
        }

        double rDefaultStart;
        try {
            rDefaultStart = solver.solve(100, linear, min, max, startValue);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-start-state] semantic mismatch: solve(maxEval,f,min,max,startValue) should leave/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-start-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        double rExplicitAnyStart;
        try {
            rExplicitAnyStart = solver.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-start-state] semantic mismatch: solve(...,startValue,ANY_SIDE) should write/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-start-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        if (Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits(rExplicitAnyStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:start-default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max,startValue) must equal solve(maxEval,f,min,max,startValue,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefaultStart + " rhs=" + rExplicitAnyStart
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rDefaultStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-endpoint-root] metamorphic violation: endpoint exact-root problem must return the same root across overloads inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefault + " rhs=" + rDefaultStart
            );
        }

        final AllowedSolution chosenAllowed = AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];
        double rChosen;
        try {
            rChosen = solver.solve(100, linear, min, max, startValue, chosenAllowed);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != chosenAllowed) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:allowed-writer-reader] semantic mismatch: solve(...,allowedSolution) should write/report the passed allowedSolution expected=" +
                chosenAllowed + " actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:chosen-allowed-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        if (Double.doubleToLongBits(rChosen) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-endpoint-root] semantic mismatch: for f(x)=x-c with c as an endpoint, the solver must return that exact endpoint root inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " allowed=" + chosenAllowed + " actual=" + rChosen
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAny) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAnyStart) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-all-overloads] semantic mismatch: endpoint-root construction fixes the trusted answer to c inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue +
                " default=" + rDefault + " explicitAny=" + rExplicitAny +
                " defaultStart=" + rDefaultStart + " explicitAnyStart=" + rExplicitAnyStart
            );
        }
    }
}
```

</details>

---
## [77] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [78] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~15,420 chars, 2 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        RegulaFalsiSolver fixedSolver = new RegulaFalsiSolver();

        if (fixedSolver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-default-allowed] semantic mismatch: expected ANY_SIDE after construction actual=" + fixedSolver.allowed
            );
        }
        if (fixedSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-method] semantic mismatch: expected REGULA_FALSI after construction actual=" + fixedSolver.method
            );
        }

        final int fixedMaxEval = 3624 + data.consumeInt(0, 0);
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean fixedViolation = false;
        String fixedMessage = null;
        Throwable fixedCause = null;
        double fixedRoot = Double.NaN;
        try {
            fixedRoot = fixedSolver.solve(fixedMaxEval, issue631Function, 1.0, 10.0);
            fixedViolation = true;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but completed normally with root=" + fixedRoot;
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            fixedViolation = true;
            fixedCause = t;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but caught " + t.getClass().getName();
        }
        if (fixedViolation) {
            if (fixedCause != null) {
                throw new FuzzerSecurityIssueLow(fixedMessage, fixedCause);
            }
            throw new FuzzerSecurityIssueLow(fixedMessage);
        }

        if (fixedSolver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:post-issue631-allowed] semantic mismatch: expected ANY_SIDE after solve(maxEval,f,min,max) actual=" + fixedSolver.allowed
            );
        }
        if (fixedSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:post-issue631-method] semantic mismatch: expected REGULA_FALSI after solve actual=" + fixedSolver.method
            );
        }

        final int c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        final double startValue = (min + max) / 2.0;
        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        RegulaFalsiSolver solver = new RegulaFalsiSolver();

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-default-allowed-2] semantic mismatch: expected ANY_SIDE after construction actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:ctor-method-2] semantic mismatch: expected REGULA_FALSI after construction actual=" + solver.method
            );
        }

        double rDefault;
        try {
            rDefault = solver.solve(100, linear, min, max);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-overload-state] semantic mismatch: solve(maxEval,f,min,max) should leave/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-overload-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        double rExplicitAny;
        try {
            rExplicitAny = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-state] semantic mismatch: solve(...,ANY_SIDE) should write/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " lhs=" + rDefault + " rhs=" + rExplicitAny
            );
        }

        double rDefaultStart;
        try {
            rDefaultStart = solver.solve(100, linear, min, max, startValue);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-start-state] semantic mismatch: solve(maxEval,f,min,max,startValue) should leave/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-start-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        double rExplicitAnyStart;
        try {
            rExplicitAnyStart = solver.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-start-state] semantic mismatch: solve(...,startValue,ANY_SIDE) should write/report ANY_SIDE actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:explicit-any-start-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        if (Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits(rExplicitAnyStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:start-default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max,startValue) must equal solve(maxEval,f,min,max,startValue,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefaultStart + " rhs=" + rExplicitAnyStart
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rDefaultStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-endpoint-root] metamorphic violation: endpoint exact-root problem must return the same root across overloads inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefault + " rhs=" + rDefaultStart
            );
        }

        final AllowedSolution chosenAllowed = AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];
        double rChosen;
        try {
            rChosen = solver.solve(100, linear, min, max, startValue, chosenAllowed);
        } catch (Throwable t) {
            return;
        }

        if (solver.allowed != chosenAllowed) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:allowed-writer-reader] semantic mismatch: solve(...,allowedSolution) should write/report the passed allowedSolution expected=" +
                chosenAllowed + " actual=" + solver.allowed
            );
        }
        if (solver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:chosen-allowed-method-state] semantic mismatch: expected REGULA_FALSI actual=" + solver.method
            );
        }

        if (Double.doubleToLongBits(rChosen) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-endpoint-root] semantic mismatch: for f(x)=x-c with c as an endpoint, the solver must return that exact endpoint root inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " allowed=" + chosenAllowed + " actual=" + rChosen
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAny) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAnyStart) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-all-overloads] semantic mismatch: endpoint-root construction fixes the trusted answer to c inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue +
                " default=" + rDefault + " explicitAny=" + rExplicitAny +
                " defaultStart=" + rDefaultStart + " explicitAnyStart=" + rExplicitAnyStart
            );
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:12: error: allowed has private access in BaseSecantSolver
        if (fixedSolver.allowed != AllowedSolution.ANY_SIDE) {
                       ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:14: error: allowed has private access in BaseSecantSolver
                "[oracle:ctor-default-allowed] semantic mismatch: expected ANY_SIDE after construction actual=" + fixedSolver.allowed
                                                                                                                             ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:17: error: method has private access in BaseSecantSolver
        if (fixedSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
                       ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:19: error: method has private access in BaseSecantSolver
                "[oracle:ctor-method] semantic mismatch: expected REGULA_FALSI after construction actual=" + fixedSolver.method
                                                                                                                        ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:51: error: allowed has private access in BaseSecantSolver
        if (fixedSolver.allowed != AllowedSolution.ANY_SIDE) {
                       ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:53: error: allowed has private access in BaseSecantSolver
                "[oracle:post-issue631-allowed] semantic mismatch: expected ANY_SIDE after solve(maxEval,f,min,max) actual=" + fixedSolver.allowed
                                                                                                                                          ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:56: error: method has private access in BaseSecantSolver
        if (fixedSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
                       ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:58: error: method has private access in BaseSecantSolver
                "[oracle:post-issue631-method] semantic mismatch: expected REGULA_FALSI after solve actual=" + fixedSolver.method
                                                                                                                          ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:76: error: allowed has private access in BaseSecantSolver
        if (solver.allowed != AllowedSolution.ANY_SIDE) {
                  ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_021/FuzzHarness.java:78: error: allowed has private access in BaseSecantSolver
                "[oracle:ctor-default-allowed-2] semantic mismatch: expected ANY_SIDE after construction actual=" + solver.allowed
                                                                                                                       
... (truncated)

Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~7,318 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int fixedMaxEval = 3624 + data.consumeInt(0, 0);
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver fixedSolver = new RegulaFalsiSolver();
        boolean fixedViolation = false;
        String fixedMessage = null;
        Throwable fixedCause = null;
        double fixedRoot = Double.NaN;
        try {
            fixedRoot = fixedSolver.solve(fixedMaxEval, issue631Function, 1.0, 10.0);
            fixedViolation = true;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but completed normally with root=" + fixedRoot;
        } catch (TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            fixedViolation = true;
            fixedCause = t;
            fixedMessage = "[oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but caught " + t.getClass().getName();
        }
        if (fixedViolation) {
            if (fixedCause != null) {
                throw new FuzzerSecurityIssueLow(fixedMessage, fixedCause);
            }
            throw new FuzzerSecurityIssueLow(fixedMessage);
        }

        final int c = data.consumeInt(-1000, 1000);
        final int w = data.consumeInt(1, 1000);
        final boolean leftRoot = data.consumeBoolean();
        final double min = leftRoot ? c : c - w;
        final double max = leftRoot ? c + w : c;
        final double startValue = (min + max) / 2.0;
        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        double rDefault;
        try {
            rDefault = solver.solve(100, linear, min, max);
        } catch (Throwable t) {
            return;
        }

        double rExplicitAny;
        try {
            rExplicitAny = solver.solve(100, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicitAny)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " lhs=" + rDefault + " rhs=" + rExplicitAny
            );
        }

        double rDefaultStart;
        try {
            rDefaultStart = solver.solve(100, linear, min, max, startValue);
        } catch (Throwable t) {
            return;
        }

        double rExplicitAnyStart;
        try {
            rExplicitAnyStart = solver.solve(100, linear, min, max, startValue, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits(rExplicitAnyStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:start-default-vs-explicit-any] metamorphic violation: solve(maxEval,f,min,max,startValue) must equal solve(maxEval,f,min,max,startValue,ANY_SIDE) inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefaultStart + " rhs=" + rExplicitAnyStart
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rDefaultStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-endpoint-root] metamorphic violation: endpoint exact-root problem must return the same root across overloads inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " lhs=" + rDefault + " rhs=" + rDefaultStart
            );
        }

        final AllowedSolution chosenAllowed = AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];

        double rChosen;
        try {
            rChosen = solver.solve(100, linear, min, max, startValue, chosenAllowed);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(rChosen) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-endpoint-root] semantic mismatch: for f(x)=x-c with c as an endpoint, the solver must return that exact endpoint root inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue + " allowed=" + chosenAllowed + " actual=" + rChosen
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAny) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rDefaultStart) != Double.doubleToLongBits((double) c) ||
            Double.doubleToLongBits(rExplicitAnyStart) != Double.doubleToLongBits((double) c)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:known-answer-all-overloads] semantic mismatch: endpoint-root construction fixes the trusted answer to c inputC=" +
                c + " min=" + min + " max=" + max + " startValue=" + startValue +
                " default=" + rDefault + " explicitAny=" + rExplicitAny +
                " defaultStart=" + rDefaultStart + " explicitAnyStart=" + rExplicitAnyStart
            );
        }

        final int c2 = data.consumeInt(-1000, 1000);
        final int width2 = data.consumeInt(2, 1000);
        final double min2 = c2 - width2;
        final double max2 = c2 + width2;
        final UnivariateRealFunction symmetricLinear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - c2;
            }
        };

        double leftSideRoot;
        double rightSideRoot;
        try {
            leftSideRoot = solver.solve(100, symmetricLinear, min2, max2, c2, AllowedSolution.LEFT_SIDE);
            rightSideRoot = solver.solve(100, symmetricLinear, min2, max2, c2, AllowedSolution.RIGHT_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(leftSideRoot) != Double.doubleToLongBits(rightSideRoot) ||
            Double.doubleToLongBits(leftSideRoot) != Double.doubleToLongBits((double) c2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:allowed-sides-agree-on-exact-root] metamorphic violation: when the exact root is returned, allowed side cannot change the answer inputC=" +
                c2 + " min=" + min2 + " max=" + max2 + " left=" + leftSideRoot + " right=" + rightSideRoot
            );
        }
    }
}
```

</details>

---
## [79] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-must-throw] semantic mismatch: expected TooManyEvaluationsException but completed normally with root=3.4341896575482003

---
## [80] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,933 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver liftedSolver = new RegulaFalsiSolver();

        try {
            final double root = liftedSolver.solve(3624, f, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException for solver.solve(3624,f,1,10) but got root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        double absAcc = data.consumeBoolean() ? 1.0e-6 : (1.0e-12 + (Math.abs((double) data.consumeInt()) % 1000000.0) * 1.0e-12);
        double relAcc = 1.0e-14 + (Math.abs((double) data.consumeInt()) % 1000000.0) * 1.0e-15;
        double fnAcc = 1.0e-15 + (Math.abs((double) data.consumeInt()) % 1000000.0) * 1.0e-16;

        final int lowSlack = data.consumeInt(0, 2);
        final int highSlack = data.consumeInt(0, 6);
        final double min = 1.0 + lowSlack;
        final double max = 4.0 + highSlack;
        if (!(min < 3.4341896575482003 && 3.4341896575482003 < max)) {
            return;
        }

        final int maxEval = data.consumeInt(4000, 12000);

        final RegulaFalsiSolver solverA = new RegulaFalsiSolver(absAcc);
        final RegulaFalsiSolver solverB = new RegulaFalsiSolver(relAcc, absAcc);
        final RegulaFalsiSolver solverC = new RegulaFalsiSolver(relAcc, absAcc, fnAcc);

        checkAnySideDefaultAgreement(solverA, f, min, max, maxEval, "default-anyside-abs");
        checkAnySideDefaultAgreement(solverB, f, min, max, maxEval, "default-anyside-relabs");
        checkAnySideDefaultAgreement(solverC, f, min, max, maxEval, "default-anyside-relabsfn");
    }

    private static void checkAnySideDefaultAgreement(RegulaFalsiSolver solver,
                                                     UnivariateRealFunction f,
                                                     double min,
                                                     double max,
                                                     int maxEval,
                                                     String oracleId) {
        final double lhs;
        try {
            lhs = solver.solve(maxEval, f, min, max);
        } catch (RuntimeException ex) {
            return;
        }

        final double rhs;
        try {
            /* Contract cited from BracketedUnivariateRealSolver javadoc and BaseSecantSolver constructors:
               backwards compatibility requires ANY_SIDE as the default allowed solution, so solve(maxEval,f,min,max)
               must agree with solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE). A patch that merely suppresses
               throwing or corrupts the shared 'allowed' state violates this observable post-condition. */
            rhs = solver.solve(maxEval, f, min, max, AllowedSolution.ANY_SIDE);
        } catch (RuntimeException ex) {
            return;
        }

        if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: default ANY_SIDE overload disagreement input=min=" +
                min + ",max=" + max + ",maxEval=" + maxEval + " lhs=" + lhs + " rhs=" + rhs
            );
        }
    }
}
```

</details>

---
## [81] ⚙️ harness-repair · `attempt_023`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [82] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631] semantic mismatch: expected TooManyEvaluationsException for solver.solve(3624,f,1,10) but got root=3.4341896575482003

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,412 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final RegulaFalsiSolver issue631Solver;
        final org.apache.commons.math.analysis.UnivariateRealFunction issue631Function;
        try {
            issue631Solver = new RegulaFalsiSolver();
            issue631Function = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
        } catch (Throwable t) {
            return;
        }

        boolean issue631Violated = false;
        String issue631Message = null;
        try {
            final int maxEval = 3624 + data.consumeInt(0, 0);
            final double root = issue631Solver.solve(maxEval, issue631Function, 1.0, 10.0);
            final long actualBits = Double.doubleToLongBits(root);
            final long buggyBits = Double.doubleToLongBits(3.4341896575482003);
            if (actualBits == buggyBits) {
                issue631Violated = true;
                issue631Message =
                        "[oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but call completed normally with root="
                                + root;
            } else {
                issue631Violated = true;
                issue631Message =
                        "[oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but call completed normally with unexpected root="
                                + root + " instead of the failing test's pinned buggy value 3.4341896575482003";
            }
        } catch (org.apache.commons.math.exception.TooManyEvaluationsException expected) {
        } catch (Throwable t) {
            issue631Violated = true;
            issue631Message =
                    "[oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but got "
                            + t.getClass().getName() + ": " + String.valueOf(t.getMessage());
        }
        if (issue631Violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(issue631Message);
        }

        final RegulaFalsiSolver solver;
        final double c;
        final int w;
        final boolean leftRoot;
        final double min;
        final double max;
        final double startValue;
        final org.apache.commons.math.analysis.UnivariateRealFunction linearFunction;
        try {
            solver = new RegulaFalsiSolver();
            c = data.consumeInt(-1000, 1000);
            w = data.consumeInt(1, 1000);
            leftRoot = data.consumeBoolean();
            min = leftRoot ? c : c - w;
            max = leftRoot ? c + w : c;
            startValue = min + (max - min) / 2.0;
            linearFunction = new org.apache.commons.math.analysis.UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };
        } catch (Throwable t) {
            return;
        }

        double defaultRoot = 0.0;
        double explicitAnyRoot = 0.0;
        boolean defaultVsExplicitReady = false;
        try {
            defaultRoot = solver.solve(100, linearFunction, min, max);
            explicitAnyRoot = solver.solve(100, linearFunction, min, max, AllowedSolution.ANY_SIDE);
            defaultVsExplicitReady = true;
        } catch (Throwable t) {
            return;
        }
        if (defaultVsExplicitReady &&
                Double.doubleToLongBits(defaultRoot) != Double.doubleToLongBits(explicitAnyRoot)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must match solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) on the same valid problem; min="
                            + min + " max=" + max + " c=" + c + " lhs=" + defaultRoot + " rhs=" + explicitAnyRoot);
        }

        double startRoot = 0.0;
        double startAnyRoot = 0.0;
        boolean startAgreementReady = false;
        try {
            startRoot = solver.solve(100, linearFunction, min, max, startValue);
            startAnyRoot = solver.solve(100, linearFunction, min, max, startValue, AllowedSolution.ANY_SIDE);
            startAgreementReady = true;
        } catch (Throwable t) {
            return;
        }
        if (startAgreementReady &&
                Double.doubleToLongBits(startRoot) != Double.doubleToLongBits(startAnyRoot)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:start-overload-any-side] metamorphic violation: solve overload with default allowed side must match explicit ANY_SIDE; min="
                            + min + " max=" + max + " startValue=" + startValue + " c=" + c
                            + " lhs=" + startRoot + " rhs=" + startAnyRoot);
        }

        double postMutationDefaultRoot = 0.0;
        double postMutationExplicitAnyRoot = 0.0;
        boolean postMutationReady = false;
        try {
            final double c2 = data.consumeInt(-1000, 1000);
            final int w2 = data.consumeInt(1, 1000);
            final boolean leftRoot2 = data.consumeBoolean();
            final double min2 = leftRoot2 ? c2 : c2 - w2;
            final double max2 = leftRoot2 ? c2 + w2 : c2;
            final org.apache.commons.math.analysis.UnivariateRealFunction linearFunction2 =
                    new org.apache.commons.math.analysis.UnivariateRealFunction() {
                        public double value(double x) {
                            return x - c2;
                        }
                    };

            /* Contract justification:
             * BaseSecantSolver constructors set allowed=ANY_SIDE, and BracketedUnivariateRealSolver
             * documents ANY_SIDE as the default for backwards compatibility. The explicit-allowed
             * solve overload writes the shared `allowed` field; re-checking the default overload
             * after that state change ensures this "question-like" behaviour remains consistent and
             * catches a patch that merely suppresses the problematic branch or leaves stale state. */
            postMutationDefaultRoot = solver.solve(100, linearFunction2, min2, max2);
            postMutationExplicitAnyRoot = solver.solve(100, linearFunction2, min2, max2, AllowedSolution.ANY_SIDE);
            postMutationReady = true;
        } catch (Throwable t) {
            return;
        }
        if (postMutationReady &&
                Double.doubleToLongBits(postMutationDefaultRoot) != Double.doubleToLongBits(postMutationExplicitAnyRoot)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:post-state-default-any-side] metamorphic violation: default ANY_SIDE must still agree with explicit ANY_SIDE after prior solve calls mutated solver state; lhs="
                            + postMutationDefaultRoot + " rhs=" + postMutationExplicitAnyRoot);
        }
    }
}
```

</details>

---
## [84] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-throw] semantic mismatch: expected org.apache.commons.math.exception.TooManyEvaluationsException but call completed normally w…

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,022 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final double expectedRoot = 3.4341896575482003d;

        try {
            double root = new RegulaFalsiSolver().solve(3624, f, 1.0d, 10.0d);
            if (Math.abs(root - expectedRoot) <= 1.0e-15d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:ground-truth] semantic mismatch: expected TooManyEvaluationsException from RegulaFalsiSolver.solve(3624,f,1,10) but got root=" + root
                );
            }
            return;
        } catch (TooManyEvaluationsException expected) {
            // Patched behavior for the lifted regression test.
        } catch (Throwable t) {
            return;
        }

        int maxEval = data.consumeInt(1, 5000);
        int startN = data.consumeInt(1, 10);
        double startValue = (double) startN;

        final RegulaFalsiSolver solverImplicit = new RegulaFalsiSolver();
        final RegulaFalsiSolver solverExplicit = new RegulaFalsiSolver();

        final double lhs;
        final double rhs;
        try {
            /*
             * Contract justification:
             * BracketedUnivariateRealSolver states ANY_SIDE is the default allowed solution
             * for backwards compatibility. BaseSecantSolver constructors set allowed to
             * ANY_SIDE, and solve(..., startValue) is the sibling overload of
             * solve(..., startValue, allowedSolution). Therefore, for identical inputs,
             * the overload without AllowedSolution must agree with the one using
             * AllowedSolution.ANY_SIDE. A patch that merely avoids the original bad path
             * but corrupts/ignores allowed-solution state breaks this relation.
             */
            lhs = solverImplicit.solve(maxEval, f, 1.0d, 10.0d, startValue);
            rhs = solverExplicit.solve(maxEval, f, 1.0d, 10.0d, startValue, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
            throw new RuntimeException(
                "[oracle:any-side-overload] metamorphic violation: solve(maxEval,f,min,max,startValue) must equal " +
                "solve(maxEval,f,min,max,startValue,AllowedSolution.ANY_SIDE) input=maxEval=" + maxEval +
                " startValue=" + startValue + " lhs=" + lhs + " rhs=" + rhs
            );
        }
    }
}
```

</details>

---
## [86] ⚙️ harness-repair · `attempt_025`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [87] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ground-truth] semantic mismatch: expected TooManyEvaluationsException from RegulaFalsiSolver.solve(3624,f,1,10) but got root=3.43418965…

---
## [88] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,422 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean fixedOracleViolated = false;
        String fixedOracleMsg = null;
        Throwable fixedOracleCause = null;

        try {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final int maxEval = 3624 + data.consumeInt(0, 0);

            double root = Double.NaN;
            try {
                root = solver.solve(maxEval, f, 1.0, 10.0);
                fixedOracleViolated = true;
                fixedOracleMsg = "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but solve completed normally with root=" + root;
            } catch (TooManyEvaluationsException expected) {
                // expected by the lifted oracle from RegulaFalsiSolverTest.testIssue631
            } catch (Throwable t) {
                fixedOracleViolated = true;
                fixedOracleMsg = "[oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but got " + t.getClass().getName();
                fixedOracleCause = t;
            }

            if (fixedOracleViolated) {
                if (fixedOracleCause != null) {
                    throw new FuzzerSecurityIssueLow(fixedOracleMsg, fixedOracleCause);
                }
                throw new FuzzerSecurityIssueLow(fixedOracleMsg);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }

        boolean anySideViolation = false;
        String anySideMsg = null;
        try {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final double c = data.consumeInt(-1000, 1000);
            final int w = data.consumeInt(1, 1000);
            final boolean leftRoot = data.consumeBoolean();
            final double min = leftRoot ? c : c - w;
            final double max = leftRoot ? c + w : c;
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c;
                }
            };

            double rDefault;
            double rExplicit;
            double rStartExplicit;
            try {
                rDefault = solver.solve(100, f, min, max);
            } catch (Throwable t) {
                return;
            }
            try {
                rExplicit = solver.solve(100, f, min, max, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }
            try {
                final double startValue = leftRoot ? min : max;
                rStartExplicit = solver.solve(100, f, min, max, startValue, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            // Contract justification: BracketedUnivariateRealSolver requires ANY_SIDE as the default allowed solution.
            // Therefore solve(maxEval,f,min,max) and solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) must agree.
            if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
                anySideViolation = true;
                anySideMsg = "[oracle:any-side-default] metamorphic violation: default ANY_SIDE disagrees with explicit ANY_SIDE c="
                        + c + " min=" + min + " max=" + max + " default=" + rDefault + " explicit=" + rExplicit;
            }

            // Contract justification: these overloads solve the same valid problem with the same allowed solution.
            // Using an endpoint as startValue on a problem whose exact root is that endpoint must not change the returned exact root.
            if (!anySideViolation && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rStartExplicit)) {
                anySideViolation = true;
                anySideMsg = "[oracle:start-overload-agreement] metamorphic violation: overload with explicit startValue disagrees c="
                        + c + " min=" + min + " max=" + max + " default=" + rDefault + " explicitStart=" + rStartExplicit;
            }

            // Post-condition justification: doSolve returns an endpoint immediately if its function value is exactly zero.
            // For f(x)=x-c and interval chosen so that one endpoint equals c, every correct implementation must return that exact endpoint/root.
            if (!anySideViolation && Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(c)) {
                anySideViolation = true;
                anySideMsg = "[oracle:endpoint-root] semantic mismatch: expected exact endpoint root c="
                        + c + " but default solve returned " + rDefault + " on [" + min + "," + max + "]";
            }
            if (!anySideViolation && Double.doubleToLongBits(rExplicit) != Double.doubleToLongBits(c)) {
                anySideViolation = true;
                anySideMsg = "[oracle:endpoint-root-explicit] semantic mismatch: expected exact endpoint root c="
                        + c + " but explicit ANY_SIDE solve returned " + rExplicit + " on [" + min + "," + max + "]";
            }
            if (!anySideViolation && Double.doubleToLongBits(rStartExplicit) != Double.doubleToLongBits(c)) {
                anySideViolation = true;
                anySideMsg = "[oracle:endpoint-root-start] semantic mismatch: expected exact endpoint root c="
                        + c + " but explicit-start solve returned " + rStartExplicit + " on [" + min + "," + max + "]";
            }
        } catch (Throwable ignored) {
            return;
        }

        if (anySideViolation) {
            throw new FuzzerSecurityIssueLow(anySideMsg);
        }

        try {
            final RegulaFalsiSolver solver = new RegulaFalsiSolver();
            final int shift = data.consumeInt(-50, 50);
            final int width = data.consumeInt(2, 200);
            final double min1 = -width;
            final double max1 = width;
            final double min2 = min1 + shift;
            final double max2 = max1 + shift;
            final UnivariateRealFunction base = new UnivariateRealFunction() {
                public double value(double x) {
                    return x;
                }
            };
            final UnivariateRealFunction shifted = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - shift;
                }
            };

            double root1;
            double root2;
            try {
                root1 = solver.solve(100, base, min1, max1, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }
            try {
                root2 = solver.solve(100, shifted, min2, max2, AllowedSolution.ANY_SIDE);
            } catch (Throwable t) {
                return;
            }

            boolean translationViolation = false;
            String translationMsg = null;

            // Contract justification: translating both the function f(x)=x and the interval by the same constant
            // translates the unique exact root by that constant; both sides are computed by real solver calls.
            if (Double.doubleToLongBits(root2) != Double.doubleToLongBits(root1 + shift)) {
                translationViolation = true;
                translationMsg = "[oracle:translation] metamorphic violation: translated problem root mismatch shift="
                        + shift + " root1=" + root1 + " root2=" + root2;
            }

            if (translationViolation) {
                throw new FuzzerSecurityIssueLow(translationMsg);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [89] ⚙️ harness-repair · `attempt_026`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [90] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-throws] semantic mismatch: expected TooManyEvaluationsException but solve completed normally with root=3.4341896575482003

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,401 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            new RegulaFalsiSolver().solve(3624, issue631, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624, exp(x)-PI^3, 1, 10) but call returned normally");
        } catch (TooManyEvaluationsException expected) {
        } catch (RuntimeException other) {
            return;
        }

        double center = data.consumeInt(-10000, 10000) / 16.0;
        double leftSpan = 1.0 + (data.consumeInt(0, 10000) / 32.0);
        double rightSpan = 1.0 + (data.consumeInt(0, 10000) / 32.0);
        final double min = center - leftSpan;
        final double max = center + rightSpan;
        final double root = center;
        final double start = min + (max - min) * 0.5;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - root;
            }
        };

        RegulaFalsiSolver solverA = new RegulaFalsiSolver();
        RegulaFalsiSolver solverB = new RegulaFalsiSolver();

        double noAllowed;
        double anySide;
        try {
            noAllowed = solverA.solve(128, linear, min, max);
            anySide = solverB.solve(128, linear, min, max, AllowedSolution.ANY_SIDE);
        } catch (RuntimeException ex) {
            return;
        }

        if (Double.doubleToLongBits(noAllowed) != Double.doubleToLongBits(anySide)) {
            throw new RuntimeException(
                "[oracle:default-any-side] metamorphic violation: documented default allowed solution is ANY_SIDE, so equivalent overloads must agree input=min=" +
                min + ",max=" + max + ",root=" + root + " lhs=" + noAllowed + " rhs=" + anySide);
        }

        RegulaFalsiSolver solverC = new RegulaFalsiSolver();
        RegulaFalsiSolver solverD = new RegulaFalsiSolver();

        double withStartDefault;
        double withStartAnySide;
        try {
            withStartDefault = solverC.solve(128, linear, min, max, start);
            withStartAnySide = solverD.solve(128, linear, min, max, start, AllowedSolution.ANY_SIDE);
        } catch (RuntimeException ex) {
            return;
        }

        if (Double.doubleToLongBits(withStartDefault) != Double.doubleToLongBits(withStartAnySide)) {
            throw new RuntimeException(
                "[oracle:start-overload-any-side] metamorphic violation: equivalent solve overloads with identical startValue must agree because constructors initialize allowed to ANY_SIDE by default input=min=" +
                min + ",max=" + max + ",start=" + start + ",root=" + root + " lhs=" + withStartDefault + " rhs=" + withStartAnySide);
        }
    }
}
```

</details>

---
## [92] ⚙️ harness-repair · `attempt_027`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [93] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624, exp(x)-PI^3, 1, 10) but call returned normally

---
## [94] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,857 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        RegulaFalsiSolver ctorSolver = new RegulaFalsiSolver();

        /* Constructor contract from BaseSecantSolver: all constructors set allowed=ANY_SIDE and method=REGULA_FALSI.
           Same-package access lets us directly check the shared state that doSolve later reads. */
        if (ctorSolver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow("[oracle:ctor-allowed] semantic mismatch: expected ANY_SIDE but was " + ctorSolver.allowed);
        }
        if (ctorSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow("[oracle:ctor-method] semantic mismatch: expected REGULA_FALSI but was " + ctorSolver.method);
        }

        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean groundTruthViolation = false;
        String groundTruthMessage = null;
        try {
            double root = ctorSolver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            groundTruthViolation = true;
            groundTruthMessage =
                    "completed normally with root=" + root +
                    " expected=TooManyEvaluationsException pinnedBuggyRoot=3.4341896575482003";
        } catch (TooManyEvaluationsException expected) {
            // Correct behavior from the failing test.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-throw] semantic mismatch: expected TooManyEvaluationsException but got " + t.getClass().getName(),
                    t);
        }
        if (groundTruthViolation) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: " + groundTruthMessage);
        }

        /* Metamorphic guarantee from BracketedUnivariateRealSolver docs:
           default allowed solution is ANY_SIDE for backwards compatibility, so
           solve(maxEval,f,min,max) and solve(maxEval,f,min,max,ANY_SIDE) must agree on the same valid problem. */
        RegulaFalsiSolver solver1;
        UnivariateRealFunction linearEndpointRoot;
        double c;
        double min;
        double max;
        double rDefault;
        double rExplicit;
        try {
            solver1 = new RegulaFalsiSolver();
            c = data.consumeInt(-1000, 1000);
            int w = data.consumeInt(1, 1000);
            boolean leftRoot = data.consumeBoolean();
            min = leftRoot ? c : c - w;
            max = leftRoot ? c + w : c;
            final double rootAtEndpoint = c;
            linearEndpointRoot = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - rootAtEndpoint;
                }
            };
            rDefault = solver1.solve(100, linearEndpointRoot, min, max);
            rExplicit = solver1.solve(100, linearEndpointRoot, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:default-vs-explicit-any-side] metamorphic violation: input[min=" + min +
                    ",max=" + max + ",c=" + c + "] lhs=" + rDefault + " rhs=" + rExplicit);
        }

        /* Additional sibling-agreement check:
           the overload taking startValue should solve the same valid problem as the overload without startValue.
           With a unique endpoint root for f(x)=x-c, both real API calls must return that same endpoint root. */
        RegulaFalsiSolver solver2;
        UnivariateRealFunction linearEndpointRoot2;
        double c2;
        double min2;
        double max2;
        double start2;
        double rNoStart;
        double rWithStart;
        try {
            solver2 = new RegulaFalsiSolver();
            c2 = data.consumeInt(-1000, 1000);
            int w2 = data.consumeInt(1, 1000);
            boolean leftRoot2 = data.consumeBoolean();
            min2 = leftRoot2 ? c2 : c2 - w2;
            max2 = leftRoot2 ? c2 + w2 : c2;
            start2 = min2 + (max2 - min2) / 2.0;
            final double rootAtEndpoint2 = c2;
            linearEndpointRoot2 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - rootAtEndpoint2;
                }
            };
            rNoStart = solver2.solve(100, linearEndpointRoot2, min2, max2);
            rWithStart = solver2.solve(100, linearEndpointRoot2, min2, max2, start2);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStart)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-startvalue-agreement] metamorphic violation: input[min=" + min2 +
                    ",max=" + max2 + ",start=" + start2 + ",c=" + c2 + "] lhs=" + rNoStart + " rhs=" + rWithStart);
        }

        /* Post-condition check on shared solver state:
           solve(..., allowedSolution) writes BaseSecantSolver.allowed; constructors established ANY_SIDE.
           A correct call with explicit ANY_SIDE must leave the reader-visible field equal to ANY_SIDE afterwards. */
        RegulaFalsiSolver solver3;
        try {
            solver3 = new RegulaFalsiSolver();
            final double c3 = data.consumeInt(-1000, 1000);
            final int w3 = data.consumeInt(1, 1000);
            final boolean leftRoot3 = data.consumeBoolean();
            final double min3 = leftRoot3 ? c3 : c3 - w3;
            final double max3 = leftRoot3 ? c3 + w3 : c3;
            UnivariateRealFunction f3 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c3;
                }
            };
            solver3.solve(100, f3, min3, max3, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (solver3.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:allowed-state-after-explicit] semantic mismatch: expected ANY_SIDE after explicit ANY_SIDE solve but was " +
                    solver3.allowed);
        }
    }
}
```

</details>

---
## [95] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [96] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,060 chars, 2 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [28] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        RegulaFalsiSolver ctorSolver = new RegulaFalsiSolver();

        /* Constructor contract from BaseSecantSolver: all constructors set allowed=ANY_SIDE and method=REGULA_FALSI.
           Same-package access lets us directly check the shared state that doSolve later reads. */
        if (ctorSolver.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow("[oracle:ctor-allowed] semantic mismatch: expected ANY_SIDE but was " + ctorSolver.allowed);
        }
        if (ctorSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
            throw new FuzzerSecurityIssueLow("[oracle:ctor-method] semantic mismatch: expected REGULA_FALSI but was " + ctorSolver.method);
        }

        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        boolean groundTruthViolation = false;
        String groundTruthMessage = null;
        try {
            double root = ctorSolver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            groundTruthViolation = true;
            groundTruthMessage =
                    "completed normally with root=" + root +
                    " expected=TooManyEvaluationsException pinnedBuggyRoot=3.4341896575482003";
        } catch (TooManyEvaluationsException expected) {
            // Correct behavior from the failing test.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-throw] semantic mismatch: expected TooManyEvaluationsException but got " + t.getClass().getName(),
                    t);
        }
        if (groundTruthViolation) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: " + groundTruthMessage);
        }

        /* Metamorphic guarantee from BracketedUnivariateRealSolver docs:
           default allowed solution is ANY_SIDE for backwards compatibility, so
           solve(maxEval,f,min,max) and solve(maxEval,f,min,max,ANY_SIDE) must agree on the same valid problem. */
        RegulaFalsiSolver solver1;
        UnivariateRealFunction linearEndpointRoot;
        double c;
        double min;
        double max;
        double rDefault;
        double rExplicit;
        try {
            solver1 = new RegulaFalsiSolver();
            c = data.consumeInt(-1000, 1000);
            int w = data.consumeInt(1, 1000);
            boolean leftRoot = data.consumeBoolean();
            min = leftRoot ? c : c - w;
            max = leftRoot ? c + w : c;
            final double rootAtEndpoint = c;
            linearEndpointRoot = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - rootAtEndpoint;
                }
            };
            rDefault = solver1.solve(100, linearEndpointRoot, min, max);
            rExplicit = solver1.solve(100, linearEndpointRoot, min, max, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rExplicit)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:default-vs-explicit-any-side] metamorphic violation: input[min=" + min +
                    ",max=" + max + ",c=" + c + "] lhs=" + rDefault + " rhs=" + rExplicit);
        }

        /* Additional sibling-agreement check:
           the overload taking startValue should solve the same valid problem as the overload without startValue.
           With a unique endpoint root for f(x)=x-c, both real API calls must return that same endpoint root. */
        RegulaFalsiSolver solver2;
        UnivariateRealFunction linearEndpointRoot2;
        double c2;
        double min2;
        double max2;
        double start2;
        double rNoStart;
        double rWithStart;
        try {
            solver2 = new RegulaFalsiSolver();
            c2 = data.consumeInt(-1000, 1000);
            int w2 = data.consumeInt(1, 1000);
            boolean leftRoot2 = data.consumeBoolean();
            min2 = leftRoot2 ? c2 : c2 - w2;
            max2 = leftRoot2 ? c2 + w2 : c2;
            start2 = min2 + (max2 - min2) / 2.0;
            final double rootAtEndpoint2 = c2;
            linearEndpointRoot2 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - rootAtEndpoint2;
                }
            };
            rNoStart = solver2.solve(100, linearEndpointRoot2, min2, max2);
            rWithStart = solver2.solve(100, linearEndpointRoot2, min2, max2, start2);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStart)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-startvalue-agreement] metamorphic violation: input[min=" + min2 +
                    ",max=" + max2 + ",start=" + start2 + ",c=" + c2 + "] lhs=" + rNoStart + " rhs=" + rWithStart);
        }

        /* Post-condition check on shared solver state:
           solve(..., allowedSolution) writes BaseSecantSolver.allowed; constructors established ANY_SIDE.
           A correct call with explicit ANY_SIDE must leave the reader-visible field equal to ANY_SIDE afterwards. */
        RegulaFalsiSolver solver3;
        try {
            solver3 = new RegulaFalsiSolver();
            final double c3 = data.consumeInt(-1000, 1000);
            final int w3 = data.consumeInt(1, 1000);
            final boolean leftRoot3 = data.consumeBoolean();
            final double min3 = leftRoot3 ? c3 : c3 - w3;
            final double max3 = leftRoot3 ? c3 + w3 : c3;
            UnivariateRealFunction f3 = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - c3;
                }
            };
            solver3.solve(100, f3, min3, max3, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (solver3.allowed != AllowedSolution.ANY_SIDE) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:allowed-state-after-explicit] semantic mismatch: expected ANY_SIDE after explicit ANY_SIDE solve but was " +
                    solver3.allowed);
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_028/FuzzHarness.java:14: error: allowed has private access in BaseSecantSolver
        if (ctorSolver.allowed != AllowedSolution.ANY_SIDE) {
                      ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_028/FuzzHarness.java:15: error: allowed has private access in BaseSecantSolver
            throw new FuzzerSecurityIssueLow("[oracle:ctor-allowed] semantic mismatch: expected ANY_SIDE but was " + ctorSolver.allowed);
                                                                                                                               ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_028/FuzzHarness.java:17: error: method has private access in BaseSecantSolver
        if (ctorSolver.method != BaseSecantSolver.Method.REGULA_FALSI) {
                      ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_028/FuzzHarness.java:18: error: method has private access in BaseSecantSolver
            throw new FuzzerSecurityIssueLow("[oracle:ctor-method] semantic mismatch: expected REGULA_FALSI but was " + ctorSolver.method);
                                                                                                                                  ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_028/FuzzHarness.java:136: error: allowed has private access in BaseSecantSolver
        if (solver3.allowed != AllowedSolution.ANY_SIDE) {
                   ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/005_patch1-Math-50-Arja_c_rep001/Math_50_buggy/fuzz/attempt_028/FuzzHarness.java:139: error: allowed has private access in BaseSecantSolver
                    solver3.allowed);
                           ^
6 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~5,688 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        RegulaFalsiSolver issue631Solver = new RegulaFalsiSolver();
        boolean violated = false;
        String violationMessage = "completed normally";
        try {
            double root = issue631Solver.solve(3624 + data.consumeInt(0, 0), issue631Function, 1.0, 10.0);
            violated = true;
            violationMessage = "completed normally with root=" + root + " expectedException=" + TooManyEvaluationsException.class.getName() + " pinnedBuggyRoot=3.4341896575482003";
        } catch (TooManyEvaluationsException expected) {
            // Correct behavior pinned by the failing test.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: expected TooManyEvaluationsException", t);
        }
        if (violated) {
            throw new FuzzerSecurityIssueLow("[oracle:issue631-throw] semantic mismatch: " + violationMessage);
        }

        /* Documented backwards-compatibility guarantee: default allowed solution is ANY_SIDE,
           so these sibling overloads must agree on the same valid-by-construction problem. */
        RegulaFalsiSolver solver1 = new RegulaFalsiSolver();
        double c1 = data.consumeInt(-1000, 1000);
        int w1 = data.consumeInt(1, 1000);
        boolean leftRoot1 = data.consumeBoolean();
        double min1 = leftRoot1 ? c1 : c1 - w1;
        double max1 = leftRoot1 ? c1 + w1 : c1;
        final double endpointRoot1 = c1;
        UnivariateRealFunction f1 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - endpointRoot1;
            }
        };
        double rDefault;
        double rAnySide;
        try {
            rDefault = solver1.solve(100, f1, min1, max1);
            rAnySide = solver1.solve(100, f1, min1, max1, AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAnySide)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:default-any-side-agreement] metamorphic violation: min=" + min1 +
                " max=" + max1 + " c=" + c1 + " lhs=" + rDefault + " rhs=" + rAnySide);
        }

        /* Overloads with and without startValue solve the same problem; for a unique endpoint root
           on f(x)=x-c, both real API calls must return that exact root. */
        RegulaFalsiSolver solver2 = new RegulaFalsiSolver();
        double c2 = data.consumeInt(-1000, 1000);
        int w2 = data.consumeInt(1, 1000);
        boolean leftRoot2 = data.consumeBoolean();
        double min2 = leftRoot2 ? c2 : c2 - w2;
        double max2 = leftRoot2 ? c2 + w2 : c2;
        double start2 = min2 + (max2 - min2) / 2.0;
        final double endpointRoot2 = c2;
        UnivariateRealFunction f2 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - endpointRoot2;
            }
        };
        double rNoStart;
        double rWithStart;
        try {
            rNoStart = solver2.solve(100, f2, min2, max2);
            rWithStart = solver2.solve(100, f2, min2, max2, start2);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(rNoStart) != Double.doubleToLongBits(rWithStart)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:start-overload-agreement] metamorphic violation: min=" + min2 +
                " max=" + max2 + " start=" + start2 + " c=" + c2 +
                " lhs=" + rNoStart + " rhs=" + rWithStart);
        }

        /* Post-condition from the solver contract and method body: if one endpoint is an exact root,
           solve returns that endpoint immediately. This observable result would be broken by a patch
           that merely avoids the problematic path by returning some other value. */
        RegulaFalsiSolver solver3 = new RegulaFalsiSolver();
        double c3 = data.consumeInt(-1000, 1000);
        int w3 = data.consumeInt(1, 1000);
        boolean leftRoot3 = data.consumeBoolean();
        double min3 = leftRoot3 ? c3 : c3 - w3;
        double max3 = leftRoot3 ? c3 + w3 : c3;
        final double endpointRoot3 = c3;
        UnivariateRealFunction f3 = new UnivariateRealFunction() {
            public double value(double x) {
                return x - endpointRoot3;
            }
        };
        double expected3 = leftRoot3 ? min3 : max3;
        double actual3;
        try {
            actual3 = solver3.solve(100, f3, min3, max3, startValueInside(min3, max3), AllowedSolution.ANY_SIDE);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(actual3) != Double.doubleToLongBits(expected3)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:endpoint-root] semantic mismatch: min=" + min3 +
                " max=" + max3 + " expected=" + expected3 + " actual=" + actual3);
        }
    }

    private static double startValueInside(double min, double max) {
        return min + (max - min) / 2.0;
    }
}
```

</details>

---
## [97] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-throw] semantic mismatch: completed normally with root=3.4341896575482003 expectedException=org.apache.commons.math.exception.…

---
## [98] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [28] — not reprinted*
- *[user] message: identical to step [30] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,300 chars)</summary>

```
package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction issue631Function = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        try {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            final double root = solver.solve(3624, issue631Function, 1, 10);
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exception] semantic mismatch: expected TooManyEvaluationsException but returned root=" + root
            );
        } catch (TooManyEvaluationsException expected) {
            // Lifted directly from RegulaFalsiSolverTest.testIssue631:
            // for this exact function, interval, and maxEval, the real public API must throw.
        } catch (Throwable ignored) {
            return;
        }

        final int chosenRootInt = data.consumeInt(-1000000, 1000000);
        final double expectedRoot = chosenRootInt;
        final double min = expectedRoot - 1.0;
        final double max = expectedRoot + 1.0;
        final double start = data.consumeBoolean() ? expectedRoot - 0.5 : expectedRoot + 0.5;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - expectedRoot;
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final double rDefault;
        final double rAllowed;
        final double rStartAllowed;
        final double rStartDefault;
        try {
            rDefault = solver.solve(32, linear, min, max);
            rAllowed = solver.solve(32, linear, min, max, AllowedSolution.ANY_SIDE);
            rStartAllowed = solver.solve(32, linear, min, max, start, AllowedSolution.ANY_SIDE);
            rStartDefault = solver.solve(32, linear, min, max, start);
        } catch (Throwable ignored) {
            return;
        }

        // Contract justification:
        // - Bracketed solvers solve for "a value where the function is zero".
        // - Here the input is constructed from a known answer: f(x)=x-expectedRoot, bracketed by [expectedRoot-1, expectedRoot+1].
        //   Any correct solver must return exactly expectedRoot.
        // - BracketedUnivariateRealSolver documents ANY_SIDE as the backwards-compatible default, so the overloads with
        //   implicit default and explicit AllowedSolution.ANY_SIDE must agree.
        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(expectedRoot)
                || Double.doubleToLongBits(rAllowed) != Double.doubleToLongBits(expectedRoot)
                || Double.doubleToLongBits(rStartAllowed) != Double.doubleToLongBits(expectedRoot)
                || Double.doubleToLongBits(rStartDefault) != Double.doubleToLongBits(expectedRoot)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:linear-root] semantic mismatch: expectedRoot=" + expectedRoot
                    + " rDefault=" + rDefault
                    + " rAllowed=" + rAllowed
                    + " rStartAllowed=" + rStartAllowed
                    + " rStartDefault=" + rStartDefault
            );
        }

        if (Double.doubleToLongBits(rDefault) != Double.doubleToLongBits(rAllowed)
                || Double.doubleToLongBits(rStartAllowed) != Double.doubleToLongBits(rStartDefault)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: implicit ANY_SIDE overload disagrees with explicit ANY_SIDE"
                    + " expectedRoot=" + expectedRoot
                    + " rDefault=" + rDefault
                    + " rAllowed=" + rAllowed
                    + " rStartAllowed=" + rStartAllowed
                    + " rStartDefault=" + rStartDefault
            );
        }
    }
}
```

</details>

---
## [99] ⚙️ harness-repair · `attempt_030`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [100] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:issue631-exception] semantic mismatch: expected TooManyEvaluationsException but returned root=3.4341896575482003

---
## [101] ⚙️ coverage · `jazzer coverage dumps`
**output:** **26/26 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov; 26 raw fuzz log(s)**
<details><summary>▸ detail (~14,571 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 1162}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 1161}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 1165}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 1163}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 1167}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 1162}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 1163}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 1164}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 1167}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 1161}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 1163}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 1162}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 1163}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 1160}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 1164}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 942}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 1164}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 1161}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 1164}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 1161}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 1165}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 1160}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 1168}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 1161}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 1164}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 1161}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_019', 'attempt_020', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_019', 'attempt_020', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/005_patch1-Math-50-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
