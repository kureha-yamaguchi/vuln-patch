# Pipeline trace — Math-33

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
+++ /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
@@ -335,7 +335,7 @@
         // positive cost non-artificial variables
         for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
             final double entry = tableau.getEntry(0, i);
-            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
+            if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                 columnsToDrop.add(i);
             }
         }
```

135 sequential steps — 36 LLM calls, 99 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math3.optimization.linear.SimplexSolverTest', test_method (~1,547 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math3.optimization.linear.SimplexSolverTest', test_method='testMath781', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/src/test/java/org/apache/commons/math3/optimization/linear/SimplexSolverTest.java', method_source='    @Test\\n    public void testMath781() {\\n        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);\\n\\n        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();\\n        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));\\n        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));\\n        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));\\n\\n        double epsilon = 1e-6;\\n        SimplexSolver solver = new SimplexSolver();\\n        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);\\n\\n        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);\\n        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);\\n        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);\\n        Assert.assertEquals(2.0d, solution.getValue(), epsilon);\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math3.optimization.linear.SimplexSolverTest::testMath781",
    "failure_message": "--- org.apache.commons.math3.optimization.linear.SimplexSolverTest::testMath781\njunit.framework.AssertionFailedError",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~26,445 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java\t\n+++ /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java\t\n@@ -335,7 +335,7 @@\n         // positive cost non-artificial variables\n         for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {\n             final double entry = tableau.getEntry(0, i);\n-            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {\n+            if (Precision.compareTo(entry, 0.0, epsilon) > 0) {\n                 columnsToDrop.add(i);\n             }\n         }\n",
  "functions": [
    {
      "func_name": "dropPhase1Objective",
      "func_signature": "protected void dropPhase1Objective()",
      "func_source": "    protected void dropPhase1Objective() {\n        if (getNumObjectiveFunctions() == 1) {\n            return;\n        }\n\n        List<Integer> columnsToDrop = new ArrayList<Integer>();\n        columnsToDrop.add(0);\n\n        // positive cost non-artificial variables\n        for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {\n            final double entry = tableau.getEntry(0, i);\n            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {\n                columnsToDrop.add(i);\n            }\n        }\n\n        // non-basic artificial variables\n        for (int i = 0; i < getNumArtificialVariables(); i++) {\n          int col = i + getArtificialVariableOffset();\n          if (getBasicRow(col) == null) {\n            columnsToDrop.add(col);\n          }\n        }\n\n        double[][] matrix = new double[getHeight() - 1][getWidth() - columnsToDrop.size()];\n        for (int i = 1; i < getHeight(); i++) {\n          int col = 0;\n          for (int j = 0; j < getWidth(); j++) {\n            if (!columnsToDrop.contains(j)) {\n              matrix[i - 1][col++] = tableau.getEntry(i, j);\n            }\n          }\n        }\n\n        for (int i = columnsToDrop.size() - 1; i >= 0; i--) {\n          columnLabels.remove((int) columnsToDrop.get(i));\n        }\n\n        this.tableau = new Array2DRowRealMatrix(matrix);\n        this.numArtificialVariables = 0;\n    }",
      "func_class": "SimplexTableau",
      "func_class_fq": "org.apache.commons.math3.optimization.linear.SimplexTableau",
      "func_param_types": [],
      "fi_name": "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "overload_types": [
        []
      ],
      "xrefs": [
        "@Override\n    public PointValuePair doOptimize()\n        throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException {\n        final SimplexTableau tableau =\n            new SimplexTableau(getFunction(),\n                               getConstraints(),\n                               getGoalType(),\n                               restrictToNonNegative(),\n                               epsilon,\n                               maxUlps);\n\n        solvePhase1(tableau);\n        tableau.dropPhase1Objective();\n\n        while (!tableau.isOptimal()) {\n            doIteration(tableau);\n        }\n        return tableau.getSolution();\n    }",
        "@Test\n    public void testDropPhase1Objective() {\n        LinearObjectiveFunction f = createFunction();\n        Collection<LinearConstraint> constraints = createConstraints();\n        SimplexTableau tableau =\n            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6);\n        double[][] expectedTableau = {\n                                      { 1, -15, -10, 0, 0, 0, 0},\n                                      { 0,   1,   0, 1, 0, 0, 2},\n                                      { 0,   0,   1, 0, 1, 0, 3},\n                                      { 0,   1,   1, 0, 0, 1, 4}\n        };\n        tableau.dropPhase1Objective();\n        assertMatrixEquals(expectedTableau, tableau.getData());\n    }"
      ],
      "xref_names": [
        "[org.apache.commons.math3.optimization.linear.SimplexSolver].doOptimize()",
        "[org.apache.commons.math3.optimization.linear.SimplexTableauTest].testDropPhase1Objective()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].<init>()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].add(int)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.linear.RealMatrix].getEntry(int,int)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.util.Precision].compareTo(double,float,int)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].add(int)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumArtificialVariables()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getBasicRow(org.apache.commons.math3.optimization.linear.SimplexTableau)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].add(org.apache.commons.math3.optimization.linear.SimplexTableau)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].size()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].contains(org.apache.commons.math3.optimization.linear.SimplexTableau)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.linear.RealMatrix].getEntry(org.apache.commons.math3.optimization.linear.SimplexTableau,org.apache.commons.math3.optimization.linear.SimplexTableau)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].size()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.ArrayList].get(org.apache.commons.math3.optimization.linear.SimplexTableau)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[java.util.List<String>].remove(int)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
          "[org.apache.commons.math3.linear.Array2DRowRealMatrix].<init>(org.apache.commons.math3.optimization.linear.SimplexTableau)"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()",
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()",
          "[org.apache.commons.math3.linear.RealMatrix].getRowDimension()"
        ],
        [
          "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()",
          "[org.apache.commons.math3.linear.RealMatrix].getColumnDimension()"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()": 0,
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()": 1,
        "[java.util.ArrayList].<init>()": 1,
        "[java.util.ArrayList].add(int)": 1,
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()": 1,
        "[org.apache.commons.math3.linear.RealMatrix].getEntry(int,int)": 1,
        "[org.apache.commons.math3.util.Precision].compareTo(double,float,int)": 1,
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumArtificialVariables()": 1,
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getBasicRow(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
        "[java.util.ArrayList].add(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()": 1,
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()": 1,
        "[java.util.ArrayList].size()": 1,
        "[java.util.ArrayList].contains(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
        "[org.apache.commons.math3.linear.RealMatrix].getEntry(org.apache.commons.math3.optimization.linear.SimplexTableau,org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
        "[java.util.ArrayList].get(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
        "[java.util.List<String>].remove(int)": 1,
        "[org.apache.commons.math3.linear.Array2DRowRealMatrix].<init>(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
        "[org.apache.commons.math3.linear.RealMatrix].getRowDimension()": 2,
        "[org.apache.commons.math3.linear.RealMatrix].getColumnDimension()": 2
      },
      "reachable": [
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()",
        "[java.util.ArrayList].<init>()",
        "[java.util.ArrayList].add(int)",
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()",
        "[org.apache.commons.math3.linear.RealMatrix].getEntry(int,int)",
        "[org.apache.commons.math3.util.Precision].compareTo(double,float,int)",
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumArtificialVariables()",
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getBasicRow(org.apache.commons.math3.optimization.linear.SimplexTableau)",
        "[java.util.ArrayList].add(org.apache.commons.math3.optimization.linear.SimplexTableau)",
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()",
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()",
        "[java.util.ArrayList].size()",
        "[java.util.ArrayList].contains(org.apache.commons.math3.optimization.linear.SimplexTableau)",
        "[org.apache.commons.math3.linear.RealMatrix].getEntry(org.apache.commons.math3.optimization.linear.SimplexTableau,org.apache.commons.math3.optimization.linear.SimplexTableau)",
        "[java.util.ArrayList].get(org.apache.commons.math3.optimization.linear.SimplexTableau)",
        "[java.util.List<String>].remove(int)",
        "[org.apache.commons.math3.linear.Array2DRowRealMatrix].<init>(org.apache.commons.math3.optimization.linear.SimplexTableau)",
        "[org.apache.commons.math3.linear.RealMatrix].getRowDimension()",
        "[org.apache.commons.math3.linear.RealMatrix].getColumnDimension()",
        "[org.apache.commons.math3.optimization.linear.SimplexTableau].getBasicRow(int)"
      ],
      "related_callees": [
        {
          "name": "getNumObjectiveFunctions",
          "source_file": "SimplexTableau.java",
          "signature": "final protected int getNumObjectiveFunctions()",
          "source": "    protected final int getNumObjectiveFunctions() {\n        return this.numArtificialVariables > 0 ? 2 : 1;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getArtificialVariableOffset",
          "source_file": "SimplexTableau.java",
          "signature": "final protected int getArtificialVariableOffset()",
          "source": "    protected final int getArtificialVariableOffset() {\n        return getNumObjectiveFunctions() + numDecisionVariables + numSlackVariables;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getEntry",
          "source_file": "SimplexTableau.java",
          "signature": "final protected double getEntry(int row, int column)",
          "source": "    protected final double getEntry(final int row, final int column) {\n        return tableau.getEntry(row, column);\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getNumArtificialVariables",
          "source_file": "SimplexTableau.java",
          "signature": "final protected int getNumArtificialVariables()",
          "source": "    protected final int getNumArtificialVariables() {\n        return numArtificialVariables;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getBasicRow",
          "source_file": "SimplexTableau.java",
          "signature": "protected Integer getBasicRow(int col)",
          "source": "    protected Integer getBasicRow(final int col) {\n        Integer row = null;\n        for (int i = 0; i < getHeight(); i++) {\n            final double entry = getEntry(i, col);\n            if (Precision.equals(entry, 1d, maxUlps) && (row == null)) {\n                row = i;\n            } else if (!Precision.equals(entry, 0d, maxUlps)) {\n                return null;\n            }\n        }\n        return row;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getHeight",
          "source_file": "SimplexTableau.java",
          "signature": "final protected int getHeight()",
          "source": "    protected final int getHeight() {\n        return tableau.getRowDimension();\n    }",
          "is_abstract": false,
          "impls": []
        }
      ],
      "field_siblings": [
        {
          "name": "SimplexTableau",
          "signature": "SimplexTableau(LinearObjectiveFunction f, Collection<LinearConstraint> constraints, GoalType goalType, boolean restrictToNonNegative, double epsilon, int maxUlps)",
          "shared_fields": [
            "maxUlps",
            "numArtificialVariables",
            "tableau"
          ],
          "is_constructor": true,
          "source": "    SimplexTableau(final LinearObjectiveFunction f,\n                   final Collection<LinearConstraint> constraints,\n                   final GoalType goalType, final boolean restrictToNonNegative,\n                   final double epsilon,\n                   final int maxUlps) {\n        this.f                      = f;\n        this.constraints            = normalizeConstraints(constraints);\n        this.restrictToNonNegative  = restrictToNonNegative;\n        this.epsilon                = epsilon;\n        this.maxUlps                = maxUlps;\n        this.numDecisionVariables   = f.getCoefficients().getDimension() +\n                                      (restrictToNonNegative ? 0 : 1);\n        this.numSlackVariables      = getConstraintTypeCounts(Relationship.LEQ) +\n                                      getConstraintTypeCounts(Relationship.GEQ);\n        this.numArtificialVariables = getConstraintTypeCounts(Relationship.EQ) +\n                                      getConstraintTypeCounts(Relationship.GEQ);\n        this.tableau = createTableau(goalType == GoalType.MAXIMIZE);\n        initializeColumnLabels();\n    }",
          "javadoc": "Build a tableau for a linear problem. @param f linear objective function @param constraints linear constraints @param goalType type of optimization goal: either {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE} @param restrictToNonNegative whether to restrict the variables to non-negative values @param epsilon amo…"
        },
        {
          "name": "equals",
          "signature": "public boolean equals(Object other)",
          "shared_fields": [
            "maxUlps",
            "numArtificialVariables"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "{@inheritDoc}"
        },
        {
          "name": "hashCode",
          "signature": "public int hashCode()",
          "shared_fields": [
            "maxUlps",
            "numArtificialVariables"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "{@inheritDoc}"
        },
        {
          "name": "createTableau",
          "signature": "protected RealMatrix createTableau(boolean maximize)",
          "shared_fields": [
            "numArtificialVariables"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Create the tableau by itself. @param maximize if true, goal is to maximize the objective function @return created tableau"
        },
        {
          "name": "writeObject",
          "signature": "private void writeObject(ObjectOutputStream oos) throws IOException",
          "shared_fields": [
            "tableau"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Serialize the instance. @param oos stream where object should be written @throws IOException if object cannot be written to stream"
        }
      ]
    }
  ],
  "package": "org.apache.commons.math3.optimization.linear",
  "root_cause_reachable": [
    "SimplexTableau.getNumObjectiveFunctions",
    "SimplexTableau.getArtificialVariableOffset",
    "RealMatrix.getEntry",
    "Precision.compareTo",
    "SimplexTableau.getNumArtificialVariables",
    "SimplexTableau.getBasicRow",
    "SimplexTableau.getHeight",
    "SimplexTableau.getWidth",
    "Array2DRowRealMatrix.<init>",
    "RealMatrix.getRowDimension",
    "RealMatrix.getColumnDimension"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.io.IOException;",
    "import java.io.ObjectInputStream;",
    "import java.io.ObjectOutputStream;",
    "import java.io.Serializable;",
    "import java.util.ArrayList;",
    "import java.util.Collection;",
    "import java.util.HashSet;",
    "import java.util.List;",
    "import java.util.Set;",
    "import org.apache.commons.math3.linear.Array2DRowRealMatrix;",
    "import org.apache.commons.math3.linear.MatrixUtils;",
    "import org.apache.commons.math3.linear.RealMatrix;",
    "import org.apache.commons.math3.linear.RealVector;",
    "import org.apache.commons.math3.optimization.GoalType;",
    "import org.apache.commons.math3.optimization.PointValuePair;",
    "import org.apache.commons.math3.util.Precision;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.ArrayList].<init>()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.ArrayList].add(int)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.linear.RealMatrix].getEntry(int,int)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.util.Precision].compareTo(double,float,int)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumArtificialVariables()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getBasicRow(org.apache.commons.math3.optimization.linear.SimplexTableau)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.ArrayList].add(org.apache.commons.math3.optimization.linear.SimplexTableau)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.ArrayList].size()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.ArrayList].contains(org.apache.commons.math3.optimization.linear.SimplexTableau)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.linear.RealMatrix].getEntry(org.apache.commons.math3.optimization.linear.SimplexTableau,org.apache.commons.math3.optimization.linear.SimplexTableau)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.ArrayList].get(org.apache.commons.math3.optimization.linear.SimplexTableau)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[java.util.List<String>].remove(int)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()",
      "[org.apache.commons.math3.linear.Array2DRowRealMatrix].<init>(org.apache.commons.math3.optimization.linear.SimplexTableau)"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()",
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()",
      "[org.apache.commons.math3.linear.RealMatrix].getRowDimension()"
    ],
    [
      "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()",
      "[org.apache.commons.math3.linear.RealMatrix].getColumnDimension()"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].dropPhase1Objective()": 0,
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumObjectiveFunctions()": 1,
    "[java.util.ArrayList].<init>()": 1,
    "[java.util.ArrayList].add(int)": 1,
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].getArtificialVariableOffset()": 1,
    "[org.apache.commons.math3.linear.RealMatrix].getEntry(int,int)": 1,
    "[org.apache.commons.math3.util.Precision].compareTo(double,float,int)": 1,
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].getNumArtificialVariables()": 1,
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].getBasicRow(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
    "[java.util.ArrayList].add(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].getHeight()": 1,
    "[org.apache.commons.math3.optimization.linear.SimplexTableau].getWidth()": 1,
    "[java.util.ArrayList].size()": 1,
    "[java.util.ArrayList].contains(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
    "[org.apache.commons.math3.linear.RealMatrix].getEntry(org.apache.commons.math3.optimization.linear.SimplexTableau,org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
    "[java.util.ArrayList].get(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
    "[java.util.List<String>].remove(int)": 1,
    "[org.apache.commons.math3.linear.Array2DRowRealMatrix].<init>(org.apache.commons.math3.optimization.linear.SimplexTableau)": 1,
    "[org.apache.commons.math3.linear.RealMatrix].getRowDimension()": 2,
    "[org.apache.commons.math3.linear.RealMatrix].getColumnDimension()": 2
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
**output:** **documented exceptions: {'SimplexTableau#readObject': ['ClassNotFoundException', 'IOException'], 'SimplexTableau#writeObject': ['IOException']}**
- detail: {'documented': {'SimplexTableau#readObject': ['ClassNotFoundException', 'IOException'], 'SimplexTableau#writeObject': ['IOException']}}

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~66,597 chars)</summary>

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
// org.apache.commons.math3.optimization.linear.SimplexSolverTest::testMath781
    @Test
    public void testMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);
        Assert.assertEquals(2.0d, solution.getValue(), epsilon);
    }
// values this test asserts as CORRECT (the patched code must reproduce these): 2.0
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.apache.commons.math3.optimization.linear.SimplexSolverTest::testMath781
// junit.framework.AssertionFailedError
STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): hashCode
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    protected void dropPhase1Objective() {
        if (getNumObjectiveFunctions() == 1) {
            return;
        }

        List<Integer> columnsToDrop = new ArrayList<Integer>();
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
            final double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < getNumArtificialVariables(); i++) {
          int col = i + getArtificialVariableOffset();
          if (getBasicRow(col) == null) {
            columnsToDrop.add(col);
          }
        }

        double[][] matrix = new double[getHeight() - 1][getWidth() - columnsToDrop.size()];
        for (int i = 1; i < getHeight(); i++) {
          int col = 0;
          for (int j = 0; j < getWidth(); j++) {
            if (!columnsToDrop.contains(j)) {
              matrix[i - 1][col++] = tableau.getEntry(i, j);
            }
          }
        }

        for (int i = columnsToDrop.size() - 1; i >= 0; i--) {
          columnLabels.remove((int) columnsToDrop.get(i));
        }

        this.tableau = new Array2DRowRealMatrix(matrix);
        this.numArtificialVariables = 0;
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either {@link GoalType#MAXIMIZE}
     * or {@link GoalType#MINIMIZE}
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) {
     * …
    protected void initializeColumnLabels() { … }
     * Create the tableau by itself.
     * @param maximize if true, goal is to maximize the objective function
     * @return created tableau
    protected RealMatrix createTableau(final boolean maximize) { … }
     * Get new versions of the constraints which have positive right hand sides.
     * @param originalConstraints original (not normalized) constraints
     * @return new versions of the constraints
    public List<LinearConstraint> normalizeConstraints(Collection<LinearConstraint> originalConstraints) { … }
     * Get a new equation equivalent to this one with a positive right hand side.
     * @param constraint reference constraint
     * @return new equation
    private LinearConstraint normalize(final LinearConstraint constraint) { … }
     * Get the number of objective functions in this tableau.
     * @return 2 for Phase 1.  1 for Phase 2.
    protected final int getNumObjectiveFunctions() { … }
     * Get a count of constraints corresponding to a specified relationship.
     * @param relationship relationship to count
     * @return number of constraint with the specified relationship
    private int getConstraintTypeCounts(final Relationship relationship) { … }
     * Get the -1 times the sum of all coefficients in the given array.
     * @param coefficients coefficients to sum
     * @return the -1 times the sum of all coefficients in the given array.
    protected static double getInvertedCoefficientSum(final RealVector coefficients) { … }
     * Checks whether the given column is basic.
     * @param col index of the column to check
     * @return the row that the variable is basic in.  null if the column is not basic
    protected Integer getBasicRow(final int col) { … }
     * Removes the phase 1 objective function, positive cost non-artificial variables,
     * and the non-basic artificial variables from this tableau.
    protected void dropPhase1Objective() {
        if (getNumObjectiveFunctions() == 1) {
            return;
        }

        List<Integer> columnsToDrop = new ArrayList<Integer>();
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
            final double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < getNumArtificialVariables(); i++) {
          int col = i + getArtificialVariableOffset();
          if (getBasicRow(col) == null) {
            columnsToDrop.add(col);
          }
        }

        double[][] matrix = new double[getHeight() - 1][getWidth() - columnsToDrop.size()];
        for (int i = 1; i < getHeight(); i++) {
          int col = 0;
          for (int j = 0; j < getWidth(); j++) {
            if (!columnsToDrop.contains(j)) {
              matrix[i - 1][col++] = tableau.getEntry(i, j);
            }
          }
        }

        for (int i = columnsToDrop.size() - 1; i >= 0; i--) {
          columnLabels.remove((int) columnsToDrop.get(i));
        }

        this.tableau = new Array2DRowRealMatrix(matrix);
        this.numArtificialVariables = 0;
    }
     * @param src the source array
     * @param dest the destination array
    private void copyArray(final double[] src, final double[] dest) { … }
     * Returns whether the problem is at an optimal state.
     * @return whether the model has been solved
     * /
     * boolean isOptimal() {
     * for (int i = getNumObjectiveFunctions(); i < getWidth() - 1; i++) {
     * final double entry = tableau.getEntry(0, i);
     * if (Precision.compareTo(entry, 0d, epsilon) < 0) {
     * return false;
     * }
     * }
     * return true;
     * }
     * …
    protected PointValuePair getSolution() { … }
     * Subtracts a multiple of one row from another.
     * <p>
     * After application of this operation, the following will hold:
     * minuendRow = minuendRow - multiple * subtrahendRow
     * </p>
     * @param dividendRow index of the row
     * @param divisor value of the divisor
    protected void divideRow(final int dividendRow, final double divisor) { … }
     * Subtracts a multiple of one row from another.
     * <p>
     * After application of this operation, the following will hold:
     * minuendRow = minuendRow - multiple * subtrahendRow
     * </p>
     * @param minuendRow row index
     * @param subtrahendRow row index
     * @param multiple multiplication factor
    protected void subtractRow(final int minuendRow, final int subtrahendRow, final double multiple) { … }
     * Get the width of the tableau.
     * @return width of the tableau
    protected final int getWidth() { … }
     * Get the height of the tableau.
     * @return height of the tableau
    protected final int getHeight() { … }
     * Get an entry of the tableau.
     * @param row row index
     * @param column column index
     * @return entry at (row, column)
    protected final double getEntry(final int row, final int column) { … }
     * Set an entry of the tableau.
     * @param row row index
     * @param column column index
     * @param value for the entry
    protected final void setEntry(final int row, final int column, final double value) { … }
     * Get the offset of the first slack variable.
     * @return offset of the first slack variable
    protected final int getSlackVariableOffset() { … }
     * Get the offset of the first artificial variable.
     * @return offset of the first artificial variable
    protected final int getArtificialVariableOffset() { … }
     * Get the offset of the right hand side.
     * @return offset of the right hand side
    protected final int getRhsOffset() { … }
     * Get the number of decision variables.
     * <p>
     * If variables are not restricted to positive values, this will include 1
     * extra decision variable to represent the absolute value of the most
     * negative variable.
     * </p>
     * @return number of decision variables
     * @see #getOriginalNumDecisionVariables()
    protected final int getNumDecisionVariables() { … }
     * Get the original number of decision variables.
     * @return original number of decision variables
     * @see #getNumDecisionVariables()
    protected final int getOriginalNumDecisionVariables() { … }
     * Get the number of slack variables.
     * @return number of slack variables
    protected final int getNumSlackVariables() { … }
     * Get the number of artificial variables.
     * @return number of artificial variables
    protected final int getNumArtificialVariables() { … }
     * Get the tableau data.
     * @return tableau data
    protected final double[][] getData() { … }
     * {@inheritDoc}
    @Override public boolean equals(Object other) { … }
     * {@inheritDoc}
    @Override public int hashCode() { … }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { … }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { … }
}
</class>
<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) {
        this(new ArrayRealVector(coefficients), relationship, value);
    }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) {
        this.coefficients = coefficients;
        this.relationship = relationship;
        this.value        = value;
    }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) {
        double[] sub = new double[lhsCoefficients.length];
        for (int i = 0; i < sub.length; ++i) {
            sub[i] = lhsCoefficients[i] - rhsCoefficients[i];
        }
        this.coefficients = new ArrayRealVector(sub, false);
        this.relationship = relationship;
        this.value        = rhsConstant - lhsConstant;
    }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) {
        this.coefficients = lhsCoefficients.subtract(rhsCoefficients);
        this.relationship = relationship;
        this.value        = rhsConstant - lhsConstant;
    }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { … }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { … }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { … }
     * {@inheritDoc}
    @Override public boolean equals(Object other) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) {
        this(new ArrayRealVector(coefficients), constantTerm);
    }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) {
        this.coefficients = coefficients;
        this.constantTerm = constantTerm;
    }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { … }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { … }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { … }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { … }
     * {@inheritDoc}
    @Override public boolean equals(Object other) { … }
     * {@inheritDoc}
    @Override public int hashCode() { … }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { … }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { … }
}
</class>
<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() {
        this(DEFAULT_EPSILON, DEFAULT_ULPS);
    }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) {
        this.epsilon = epsilon;
        this.maxUlps = maxUlps;
    }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { … }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See {@link #getPivotColumn(SimplexTableau)}
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { … }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { … }
     * {@inheritDoc}
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { … }
}
</class>
<class name="Relationship" role="collaborator">
     * Types of relationships between two cells in a Solver {@link LinearConstraint}.
     * @version $Id$
     * @since 2.0
public enum Relationship {
     * Equality relationship. */
     * EQ("="),
     * /** Lesser than or equal relationship. */
     * LEQ("<="),
     * /** Greater than or equal relationship. */
     * GEQ(">=");
     * /** Display string for the relationship.
    private final String stringValue;
     * Simple constructor.
     * @param stringValue display string for the relationship
    private Relationship(String stringValue) {
        this.stringValue = stringValue;
    }
     * {@inheritDoc}
    @Override public String toString() { … }
     * Get the relationship obtained when multiplying all coefficients by -1.
     * @return relationship obtained when multiplying all coefficients by -1
    public Relationship oppositeRelationship() { … }
}
</class>
<class name="GoalType" role="collaborator">
     * Goal type for an optimization problem.
     * @version $Id$
     * @since 2.0
public enum GoalType implements Serializable {

}
</class>
<class name="Precision" role="collaborator">
     * Utilities for comparing numbers.
     * @since 3.0
     * @version $Id$
public class Precision {
     * Smallest positive number such that {@code 1 - EPSILON} is not
     * numerically equal to 1.
     * <br/>
     * In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
    public static final double EPSILON;
     * Safe minimum, such that {@code 1 / SAFE_MIN} does not overflow.
     * <br/>
     * In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.
    public static final double SAFE_MIN;
     * Exponent offset in IEEE754 representation.
    private static final long EXPONENT_OFFSET = 1023l;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * Offset to order signed double numbers lexicographically.
    private static final int SGN_MASK_FLOAT = 0x80000000;
     * Private constructor.
    private Precision() {}
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  {@link #equals(double, double, double) equals(x, y, eps)}</li>
     * <li>&lt; 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { … }
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)}
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * @return <ul><li>0 if  {@link #equals(double, double, int) equals(x, y, maxUlps)}</li>
     * …
    public static int compareTo(final double x, final double y, final int maxUlps) { … }
     * Returns true iff they are equal as defined by
     * {@link #equals(float,float,int) equals(x, y, 1)}.
     * @param x first value
     * @param y second value
     * @return {@code true} if the values are equal.
    public static boolean equals(float x, float y) { … }
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by {@link #equals(float,float) equals(x, y, 1)}.
     * @param x first value
     * @param y second value
     * @return {@code true} if the values are equal or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y) { … }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other.
     * @since 2.2
    public static boolean equals(float x, float y, float eps) { … }
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other,
     * or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y, float eps) { … }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)}
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * …
    public static boolean equals(float x, float y, int maxUlps) { … }
     * Returns true if both arguments are NaN or if they are equal as defined
     * by {@link #equals(double,double,int) equals(x, y, maxUlps)}.
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * @return {@code true} if both arguments are NaN or if there are less than
     * {@code maxUlps} floating point values between {@code x} and {@code y}.
     * @since 2.2
    public static boolean equalsIncludingNaN(double x, double y, int maxUlps) { … }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
    public static double round(double x, int scale) { … }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
    public static float round(float x, int scale) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="Array2DRowRealMatrix" role="collaborator">
     * Implementation of {@link RealMatrix} using a {@code double[][]} array to
     * store entries.
     * @version $Id$
public class Array2DRowRealMatrix extends AbstractRealMatrix implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1067294169172445528L;
     * Entries of the matrix.
    private double data[][];
     * Creates a matrix with no data
    public Array2DRowRealMatrix() {}
     * Create a new RealMatrix with the supplied row and column dimensions.
     * @param rowDimension Number of rows in the new matrix.
     * @param columnDimension Number of columns in the new matrix.
     * @throws org.apache.commons.math3.exception.NotStrictlyPositiveException
     * if the row or column dimension is not positive.
    public Array2DRowRealMatrix(final int rowDimension, final int columnDimension) {
        super(rowDimension, columnDimension);
        data = new double[rowDimension][columnDimension];
    }
     * Create a new {@code RealMatrix} using the input array as the underlying
     * data array.
     * <p>The input array is copied, not referenced. This constructor has
     * the same effect as calling {@link #Array2DRowRealMatrix(double[][], boolean)}
     * with the second argument set to {@code true}.</p>
     * @param d Data for the new matrix.
     * @throws DimensionMismatchException if {@code d} is not rectangular.
     * @throws NoDataException if {@code d} row or colum dimension is zero.
     * @throws NullArgumentException if {@code d} is {@code null}.
     * @see #Array2DRowRealMatrix(double[][], boolean)
    public Array2DRowRealMatrix(final double[][] d) throws DimensionMismatchException, NoDataException, NullArgumentException {
        copyIn(d);
    }
     * Create a new RealMatrix using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * RealMatrix and not used directly, the {@code copyArray} may be
     * set to {@code false}. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * @param d Data for new matrix.
     * @param copyArray if {@code true}, the input array will be copied,
     * otherwise it will be referenced.
     * @throws DimensionMismatchException if {@code d} is not rectangular
     * (not all rows have the same length) or empty.
     * @throws NullArgumentException if {@code d} is {@code null}.
     * …
    public Array2DRowRealMatrix(final double[][] d, final boolean copyArray) {
        if (copyArray) {
            copyIn(d);
        } else {
            if (d == null) {
                throw new NullArgumentException();
            }
            final int nRows = d.length;
            if (nRows == 0) {
                throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_ROW);
            }
            final int nCols = d[0].length;
            if (nCols == 0) {
                throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_COLUMN);
            }
            for (int r = 1; r < nRows; r++) {
                if (d[r].length != nCols) {
                    throw new DimensionMismatchException(d[r].length, nCols);
                }
            }
            data = d;
        }
    }
     * Create a new (column) RealMatrix using {@code v} as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * @param v Column vector holding data for new matrix.
    public Array2DRowRealMatrix(final double[] v) {
        final int nRows = v.length;
        data = new double[nRows][1];
        for (int row = 0; row < nRows; row++) {
            data[row][0] = v[row];
        }
    }
     * {@inheritDoc}
    @Override public RealMatrix createMatrix(final int rowDimension, final int columnDimension) { … }
     * {@inheritDoc}
    @Override public RealMatrix copy() { … }
     * Compute the sum of this matrix with {@code m}.
     * @param m Matrix to be added.
     * @return {@code this} + m.
     * @throws MatrixDimensionMismatchException
     * if {@code m} is not the same size as this matrix.
    public Array2DRowRealMatrix add(final Array2DRowRealMatrix m) { … }
     * Subtract {@code m} from this matrix.
     * @param m Matrix to be subtracted.
     * @return {@code this} - m.
     * @throws MatrixDimensionMismatchException
     * if {@code m} is not the same size as this matrix.
    public Array2DRowRealMatrix subtract(final Array2DRowRealMatrix m) { … }
     * Postmultiplying this matrix by {@code m}.
     * @param m Matrix to postmultiply by.
     * @return {@code this} * m.
     * @throws DimensionMismatchException if the number of columns of this
     * matrix is not equal to the number of rows of {@code m}.
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m) { … }
     * {@inheritDoc}
    @Override public double[][] getData() { … }
     * Get a reference to the underlying data array.
     * @return 2-dimensional array of entries.
    public double[][] getDataRef() { … }
     * {@inheritDoc}
    @Override public void setSubMatrix(final double[][] subMatrix, final int row, final int column) { … }
     * {@inheritDoc}
    @Override public double getEntry(final int row, final int column) { … }
     * {@inheritDoc}
    @Override public void setEntry(final int row, final int column, final double value) { … }
     * {@inheritDoc}
    @Override public void addToEntry(final int row, final int column, final double increment) { … }
     * {@inheritDoc}
    @Override public void multiplyEntry(final int row, final int column, final double factor) { … }
     * {@inheritDoc}
    @Override public int getRowDimension() { … }
     * {@inheritDoc}
    @Override public int getColumnDimension() { … }
     * {@inheritDoc}
    @Override public double[] operate(final double[] v) { … }
     * {@inheritDoc}
    @Override public double[] preMultiply(final double[] v) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Id$
public class MatrixUtils {
     * Private constructor.
    private MatrixUtils() {
        super();
    }
     * Returns a {@link RealMatrix} with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a {@link
     * Array2DRowRealMatrix} instance is built. Above this threshold a {@link
     * BlockRealMatrix} instance is built.</p>
     * <p>The matrix elements are all set to 0.0.</p>
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  RealMatrix with specified dimensions
     * @see #createRealMatrix(double[][])
    public static RealMatrix createRealMatrix(final int rows, final int columns) { … }
     * Returns a {@link FieldMatrix} with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a {@link FieldMatrix} instance is built. Above
     * this threshold a {@link BlockFieldMatrix} instance is built.</p>
     * <p>The matrix elements are all set to field.getZero().</p>
     * @param <T> the type of the field elements
     * @param field field to which the matrix elements belong
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  FieldMatrix with specified dimensions
     * @see #createFieldMatrix(FieldElement[][])
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(final Field<T> field, final int rows, final int columns) { … }
     * Returns a {@link RealMatrix} whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a {@link
     * Array2DRowRealMatrix} instance is built. Above this threshold a {@link
     * BlockRealMatrix} instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if {@code data} is not rectangular (not all rows have the same length).
     * …
    public static RealMatrix createRealMatrix(double[][] data) { … }
     * Returns a {@link FieldMatrix} whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a {@link FieldMatrix} instance is built. Above
     * this threshold a {@link BlockFieldMatrix} instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param <T> the type of the field elements
     * @param data input array
     * @return a matrix containing the values of the array.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if {@code data} is not rectangular (not all rows have the same length).
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(T[][] data) { … }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
    public static RealMatrix createRealIdentityMatrix(int dimension) { … }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param <T> the type of the field elements
     * @param field field to which the elements belong
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { … }
     * Returns a diagonal matrix with specified elements.
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static RealMatrix createRealDiagonalMatrix(final double[] diagonal) { … }
     * Returns a diagonal matrix with specified elements.
     * @param <T> the type of the field elements
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldDiagonalMatrix(final T[] diagonal) { … }
     * Creates a {@link RealVector} using the data from the input array.
     * @param data the input data
     * @return a data.length RealVector
     * @throws NoDataException if {@code data} is empty.
     * @throws NullArgumentException if {@code data} is {@code null}.
    public static RealVector createRealVector(double[] data) { … }
     * Creates a {@link FieldVector} using the data from the input array.
     * @param <T> the type of the field elements
     * @param data the input data
     * @return a data.length FieldVector
     * @throws NoDataException if {@code data} is empty.
     * @throws NullArgumentException if {@code data} is {@code null}.
     * @throws ZeroException if {@code data} has 0 elements
    public static <T extends FieldElement<T>> FieldVector<T> createFieldVector(final T[] data) { … }
     * Converter for {@link FieldMatrix}/{@link Fraction}.
    private static class FractionMatrixConverter extends DefaultFieldMatrixPreservingVisitor<Fraction> { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
THE PATCH-CHANGED CLASS: SimplexTableau. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
+++ /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
@@ -335,7 +335,7 @@
         // positive cost non-artificial variables
         for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
             final double entry = tableau.getEntry(0, i);
-            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
+            if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                 columnsToDrop.add(i);
             }
         }

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    REMOVED: if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
    ADDED  : if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
A tableau for use in the Simplex method.

<p>
Example:
<pre>
W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
---------------------------------------------------
-1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
0    0    1     0     0     1     0     0     2   &lt;= constraint 1
0    0    0     1     0     0     1     0     3   &lt;= constraint 2
0    0    1     1     0     0     0     1     4   &lt;= constraint 3
</pre>
W: Phase 1 objective function</br>
Z: Phase 2 objective function</br>
x1 &amp; x2: Decision variables</br>
x-: Extra decision variable to allow for negative values</br>
s1 &amp; s2: Slack/Surplus variables</br>
a1: Artificial variable</br>
RHS: Right hand side</br>
</p>
@version $Id$
@since 2.0
/
class SimplexTableau implements Serializable {

/** Column label for negative vars. */
private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";

/** Default amount of error to accept in floating point comparisons (as ulps). */
private static final int DEFAULT_ULPS = 10;

/** Serializable version identifier. */
private static final long serialVersionUID = -1369660067587938365L;

/** Linear objective function. */
private final LinearObjectiveFunction f;

/** Linear constraints. */
private final List<LinearConstraint> constraints;

/** Whether to restrict the variables to non-negative values. */
private final boolean restrictToNonNegative;

/** The variables each col
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    add, optimize, compareTo, getPoint, getValue, LinearObjectiveFunction, LinearConstraint, SimplexSolver

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
<details open><summary>▸ Output (~18,592 chars)</summary>

```
[
  {
    "name": "dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped",
    "kind": "metamorphic",
    "contract": "dropPhase1Objective() \"Removes the phase 1 objective function, positive cost non-artificial variables, and the non-basic artificial variables from this tableau.\" Therefore, changing one non-artificial phase-1 cost entry from 0 to a strictly positive value must cause that column to be removed after the call.",
    "input": "Construct two identical valid phase-1 tableaux through the real SimplexTableau constructor using a linear program that requires artificial variables (e.g. the Math781-style constraints). In both tableaux, force all phase-1-row entries in the non-artificial scanned range to 0, then in exactly one tableau set one scanned non-artificial entry to a tiny strictly positive value drawn from data. This isolates the changed >0 boundary in the patched condition.",
    "check": "Object tZero, tPos; java.lang.reflect.Method getNumObjectiveFunctions, getArtificialVariableOffset, setEntry, getWidth, getHeight, dropPhase1Objective; try { int a = data.consumeInt(-5, 5); int b = data.consumeInt(-5, 5); int c = data.consumeInt(-5, 5); org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0); java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint> constraints = new java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint>(); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 1, 2, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, 2)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { -1, 1, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 2, -3, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); Class<?> cls = Class.forName(\"org.apache.commons.math3.optimization.linear.SimplexTableau\"); java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor(org.apache.commons.math3.optimization.linear.LinearObjectiveFunction.class, java.util.Collection.class, org.apache.commons.math3.optimization.GoalType.class, boolean.class, double.class); ctor.setAccessible(true); tZero = ctor.newInstance(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, 1.0e-6); tPos = ctor.newInstance(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, 1.0e-6); getNumObjectiveFunctions = cls.getDeclaredMethod(\"getNumObjectiveFunctions\"); getArtificialVariableOffset = cls.getDeclaredMethod(\"getArtificialVariableOffset\"); setEntry = cls.getDeclaredMethod(\"setEntry\", int.class, int.class, double.class); getWidth = cls.getDeclaredMethod(\"getWidth\"); getHeight = cls.getDeclaredMethod(\"getHeight\"); dropPhase1Objective = cls.getDeclaredMethod(\"dropPhase1Objective\"); getNumObjectiveFunctions.setAccessible(true); getArtificialVariableOffset.setAccessible(true); setEntry.setAccessible(true); getWidth.setAccessible(true); getHeight.setAccessible(true); dropPhase1Objective.setAccessible(true); int start = ((Integer) getNumObjectiveFunctions.invoke(tZero)).intValue(); int end = ((Integer) getArtificialVariableOffset.invoke(tZero)).intValue(); if (end <= start) { return; } for (int j = start; j < end; j++) { setEntry.invoke(tZero, 0, j, 0.0); setEntry.invoke(tPos, 0, j, 0.0); } int chosen = start + data.consumeInt(0, end - start - 1); double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4)); setEntry.invoke(tPos, 0, chosen, tinyPositive); } catch (Exception e) { return; } int widthZeroBefore; int widthPosBefore; int heightZeroBefore; int heightPosBefore; try { widthZeroBefore = ((Integer) getWidth.invoke(tZero)).intValue(); widthPosBefore = ((Integer) getWidth.invoke(tPos)).intValue(); heightZeroBefore = ((Integer) getHeight.invoke(tZero)).intValue(); heightPosBefore = ((Integer) getHeight.invoke(tPos)).intValue(); dropPhase1Objective.invoke(tZero); dropPhase1Objective.invoke(tPos); } catch (Exception e) { throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage()); } int widthZeroAfter; int widthPosAfter; int heightZeroAfter; int heightPosAfter; try { widthZeroAfter = ((Integer) getWidth.invoke(tZero)).intValue(); widthPosAfter = ((Integer) getWidth.invoke(tPos)).intValue(); heightZeroAfter = ((Integer) getHeight.invoke(tZero)).intValue(); heightPosAfter = ((Integer) getHeight.invoke(tPos)).intValue(); } catch (Exception e) { throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: could not read tableau dimensions after probe: \" + e.getMessage()); } if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: baseline tableaux not identical before probe\"); if (heightZeroAfter != heightPosAfter) throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: changing one scanned coefficient should not change number of removed rows\"); if (widthPosAfter != widthZeroAfter - 1) throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: tiny positive scanned non-artificial column was not additionally dropped: zeroCaseWidth=\" + widthZeroAfter + \", posCaseWidth=\" + widthPosAfter);"
  },
  {
    "name": "dropPhase1Objective_clearsArtificialVariables_and_dropsOneRow",
    "kind": "invariant",
    "contract": "dropPhase1Objective() removes the phase 1 objective function from the tableau, and after the method the tableau is in phase 2 only. The implementation-visible API exposes this as getNumArtificialVariables() == 0, and removing the phase-1 objective row means the tableau height decreases by exactly 1 whenever there were initially 2 objective functions.",
    "input": "Construct a valid tableau through the real constructor for a problem that requires phase 1, using the same kind of constraints with negative right-hand sides that introduce artificial variables.",
    "check": "Object tableau; java.lang.reflect.Method getNumObjectiveFunctions, getNumArtificialVariables, getHeight, dropPhase1Objective; try { org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(new double[] { 2, 6, 7 }, data.consumeInt(-3, 3)); java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint> constraints = new java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint>(); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 1, 2, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, 2)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { -1, 1, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 2, -3, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); Class<?> cls = Class.forName(\"org.apache.commons.math3.optimization.linear.SimplexTableau\"); java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor(org.apache.commons.math3.optimization.linear.LinearObjectiveFunction.class, java.util.Collection.class, org.apache.commons.math3.optimization.GoalType.class, boolean.class, double.class); ctor.setAccessible(true); tableau = ctor.newInstance(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, data.consumeBoolean(), 1.0e-6); getNumObjectiveFunctions = cls.getDeclaredMethod(\"getNumObjectiveFunctions\"); getNumArtificialVariables = cls.getDeclaredMethod(\"getNumArtificialVariables\"); getHeight = cls.getDeclaredMethod(\"getHeight\"); dropPhase1Objective = cls.getDeclaredMethod(\"dropPhase1Objective\"); getNumObjectiveFunctions.setAccessible(true); getNumArtificialVariables.setAccessible(true); getHeight.setAccessible(true); dropPhase1Objective.setAccessible(true); } catch (Exception e) { return; } int objectivesBefore; int artificialsBefore; int heightBefore; try { objectivesBefore = ((Integer) getNumObjectiveFunctions.invoke(tableau)).intValue(); artificialsBefore = ((Integer) getNumArtificialVariables.invoke(tableau)).intValue(); heightBefore = ((Integer) getHeight.invoke(tableau)).intValue(); if (objectivesBefore != 2) { return; } dropPhase1Objective.invoke(tableau); } catch (Exception e) { throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificialVariables_and_dropsOneRow violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage()); } int objectivesAfter; int artificialsAfter; int heightAfter; try { objectivesAfter = ((Integer) getNumObjectiveFunctions.invoke(tableau)).intValue(); artificialsAfter = ((Integer) getNumArtificialVariables.invoke(tableau)).intValue(); heightAfter = ((Integer) getHeight.invoke(tableau)).intValue(); } catch (Exception e) { throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificialVariables_and_dropsOneRow violated: could not read tableau state after probe: \" + e.getMessage()); } if (artificialsBefore < 0 || artificialsAfter != 0) throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificialVariables_and_dropsOneRow violated: artificial-variable count after drop is \" + artificialsAfter); if (heightAfter != heightBefore - 1) throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificialVariables_and_dropsOneRow violated: height did not decrease by exactly one: before=\" + heightBefore + \" after=\" + heightAfter); if (objectivesAfter != 1) throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificialVariables_and_dropsOneRow violated: expected phase-2 tableau with one objective function, got \" + objectivesAfter);"
  },
  {
    "name": "linearObjectiveFunction_valueMatchesDocumentedFormula_array",
    "kind": "invariant",
    "contract": "LinearObjectiveFunction: \"Compute the value of the linear equation at the current point\" for c1*x1 + ... + cn*xn + d. Therefore getValue(double[]) must equal the independently recomputed dot product plus the constant term.",
    "input": "Choose a positive dimension from data, then draw coefficients, point coordinates, and constant term from data to build a valid LinearObjectiveFunction and matching point array.",
    "check": "org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f; double[] coeffs; double[] point; double expected; double actual; try { int n = data.consumeInt(1, 8); coeffs = new double[n]; point = new double[n]; expected = 0.0; for (int i = 0; i < n; i++) { coeffs[i] = data.consumeInt(-1000, 1000); point[i] = data.consumeInt(-1000, 1000); expected += coeffs[i] * point[i]; } double constant = data.consumeInt(-1000, 1000); expected += constant; f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(coeffs, constant); actual = f.getValue(point); } catch (Exception e) { return; } double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(expected), Math.abs(actual))); if (!(Math.abs(expected - actual) <= tol)) throw new RuntimeException(\"relation linearObjectiveFunction_valueMatchesDocumentedFormula_array violated: expected=\" + expected + \" actual=\" + actual);"
  },
  {
    "name": "linearObjectiveFunction_overloadsAgree",
    "kind": "metamorphic",
    "contract": "LinearObjectiveFunction provides two overloads of getValue, one taking double[] and one taking RealVector, both documented as computing the value of the same linear equation at the current point. For equivalent points they must agree.",
    "input": "Choose a positive dimension from data, then draw the same coordinates into both a double[] and an ArrayRealVector, and evaluate the same LinearObjectiveFunction through both overloads.",
    "check": "org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f; double[] coeffs; double[] point; double vArray; double vVector; try { int n = data.consumeInt(1, 8); coeffs = new double[n]; point = new double[n]; for (int i = 0; i < n; i++) { coeffs[i] = data.consumeInt(-1000, 1000); point[i] = data.consumeInt(-1000, 1000); } double constant = data.consumeInt(-1000, 1000); f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(coeffs, constant); vArray = f.getValue(point); vVector = f.getValue(new org.apache.commons.math3.linear.ArrayRealVector(point)); } catch (Exception e) { return; } double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(vArray), Math.abs(vVector))); if (!(Math.abs(vArray - vVector) <= tol)) throw new RuntimeException(\"relation linearObjectiveFunction_overloadsAgree violated: array=\" + vArray + \" vector=\" + vVector);"
  },
  {
    "name": "linearConstraint_twoEquationConstructorMatchesReducedSingleEquation",
    "kind": "invariant",
    "contract": "LinearConstraint's two-equation constructor represents l1*x1+...+lcst REL r1*x1+...+rcst by storing coefficients = lhsCoefficients - rhsCoefficients and value = rhsConstant - lhsConstant. Those getters must expose exactly that reduced single-equation form.",
    "input": "Choose a positive dimension from data, draw lhs and rhs coefficient arrays and both constants from data, choose any Relationship value, and construct a LinearConstraint with the two-equation constructor.",
    "check": "org.apache.commons.math3.optimization.linear.LinearConstraint c; double[] lhs; double[] rhs; double lhsConst; double rhsConst; org.apache.commons.math3.optimization.linear.Relationship rel; try { int n = data.consumeInt(1, 8); lhs = new double[n]; rhs = new double[n]; for (int i = 0; i < n; i++) { lhs[i] = data.consumeInt(-1000, 1000); rhs[i] = data.consumeInt(-1000, 1000); } lhsConst = data.consumeInt(-1000, 1000); rhsConst = data.consumeInt(-1000, 1000); org.apache.commons.math3.optimization.linear.Relationship[] vals = org.apache.commons.math3.optimization.linear.Relationship.values(); rel = vals[data.consumeInt(0, vals.length - 1)]; c = new org.apache.commons.math3.optimization.linear.LinearConstraint(lhs, lhsConst, rel, rhs, rhsConst); } catch (Exception e) { return; } if (c.getRelationship() != rel) throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorMatchesReducedSingleEquation violated: relationship changed from \" + rel + \" to \" + c.getRelationship()); double[] got = c.getCoefficients().toArray(); if (got.length != lhs.length) throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorMatchesReducedSingleEquation violated: coefficient length mismatch\"); for (int i = 0; i < got.length; i++) { double expected = lhs[i] - rhs[i]; double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(expected), Math.abs(got[i]))); if (!(Math.abs(expected - got[i]) <= tol)) throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorMatchesReducedSingleEquation violated at index \" + i + \": expected=\" + expected + \" actual=\" + got[i]); } double expectedValue = rhsConst - lhsConst; double actualValue = c.getValue(); double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(expectedValue), Math.abs(actualValue))); if (!(Math.abs(expectedValue - actualValue) <= tol)) throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorMatchesReducedSingleEquation violated on value: expected=\" + expectedValue + \" actual=\" + actualValue);"
  },
  {
    "name": "simplexSolver_math781RegressionValueAndSigns",
    "kind": "invariant",
    "contract": "For the concrete valid linear program used in the trusted regression test, every correct implementation must reproduce that program's optimum: solution value 2.0, with x0 > 0, x1 > 0, and x2 < 0 within the documented solver epsilon.",
    "input": "Build exactly the Math781 linear objective and constraints through the public API and solve with SimplexSolver.optimize using GoalType.MAXIMIZE and restrictToNonNegative=false.",
    "check": "org.apache.commons.math3.optimization.linear.SimplexSolver solver; org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f; java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint> constraints; org.apache.commons.math3.optimization.PointValuePair solution; try { f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0); constraints = new java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint>(); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 1, 2, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, 2)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { -1, 1, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 2, -3, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); solver = new org.apache.commons.math3.optimization.linear.SimplexSolver(); solution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false); } catch (Exception e) { return; } double[] p = solution.getPoint(); double eps = 1.0e-6; if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, eps) > 0)) throw new RuntimeException(\"relation simplexSolver_math781RegressionValueAndSigns violated: x0=\" + p[0]); if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, eps) > 0)) throw new RuntimeException(\"relation simplexSolver_math781RegressionValueAndSigns violated: x1=\" + p[1]); if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, eps) < 0)) throw new RuntimeException(\"relation simplexSolver_math781RegressionValueAndSigns violated: x2=\" + p[2]); if (!(Math.abs(solution.getValue() - 2.0d) <= eps * Math.max(1.0, Math.max(Math.abs(solution.getValue()), 2.0d)))) throw new RuntimeException(\"relation simplexSolver_math781RegressionValueAndSigns violated: value=\" + solution.getValue());"
  }
]
```

</details>

---
## [5] ⚙️ screen · `dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDro`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [6] ⚙️ screen-fuzz-buggy · `dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDro`
**output:** 
```json
{
  "checked": 20000,
  "violated": 15648,
  "ratio": 0.7824
}
```

---
## [7] ⚙️ screen · `dropPhase1Objective_clearsArtificialVariables_and_dropsOneRo`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [8] ⚙️ screen-fuzz-buggy · `dropPhase1Objective_clearsArtificialVariables_and_dropsOneRo`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen-fuzz-buggy · `linearObjectiveFunction_valueMatchesDocumentedFormula_array`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [10] ⚙️ screen-fuzz-buggy · `linearObjectiveFunction_overloadsAgree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [11] ⚙️ screen-fuzz-buggy · `linearConstraint_twoEquationConstructorMatchesReducedSingleE`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [12] ⚙️ screen-fuzz-buggy · `simplexSolver_math781RegressionValueAndSigns`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [13] ⚙️ screen · `dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDro`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [14] ⚙️ screen · `simplexSolver_math781RegressionValueAndSigns`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [15] ⚙️ screen · `dropPhase1Objective_clearsArtificialVariables_and_dropsOneRo`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [16] ⚙️ screen · `linearObjectiveFunction_valueMatchesDocumentedFormula_array`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [17] ⚙️ screen · `linearObjectiveFunction_overloadsAgree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [18] ⚙️ screen · `linearConstraint_twoEquationConstructorMatchesReducedSingleE`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [19] ⚙️ screening-survivors
<details><summary>▸ output — kept": [ (~414 chars)</summary>

**output:** 
```json
{
  "kept": [
    "dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDro",
    "simplexSolver_math781RegressionValueAndSigns",
    "dropPhase1Objective_clearsArtificialVariables_and_dropsOneRo",
    "linearObjectiveFunction_valueMatchesDocumentedFormula_array",
    "linearObjectiveFunction_overloadsAgree",
    "linearConstraint_twoEquationConstructorMatchesReducedSingleE"
  ],
  "count": 6
}
```

</details>

---
## [20] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~13,570 chars)</summary>

```
[
  {
    "name": "dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn",
    "kind": "metamorphic",
    "contract": "SimplexTableau.dropPhase1Objective() \"Removes the phase 1 objective function, positive cost non-artificial variables, and the non-basic artificial variables from this tableau.\" Therefore, for two otherwise identical phase-1 tableaux, changing one non-artificial phase-1 objective entry from 0.0 to a positive value must cause exactly that extra non-artificial column to be dropped.",
    "input": "Build a valid phase-1 tableau by using an equality constraint, which introduces an artificial variable. Choose a non-artificial column index c in [getNumObjectiveFunctions(), getArtificialVariableOffset()) and create two identical tableaux. In one, set entry (0,c)=0.0; in the other, set entry (0,c) to a tiny positive value below the solver epsilon but still > 0, e.g. 1e-7. All other entries stay identical.",
    "check": "SimplexTableau tZero; SimplexTableau tPos; int beforeWZero; int beforeWPos; int c; try {\n    double a = data.consumeInt(-5, 5);\n    if (a == 0.0) a = 1.0;\n    LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { a }, data.consumeInt(-3, 3));\n    java.util.List<LinearConstraint> cs1 = new java.util.ArrayList<LinearConstraint>();\n    java.util.List<LinearConstraint> cs2 = new java.util.ArrayList<LinearConstraint>();\n    double rhs = data.consumeInt(1, 5);\n    cs1.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, rhs));\n    cs2.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, rhs));\n    tZero = new SimplexTableau(f, cs1, GoalType.MAXIMIZE, true, 1.0e-6);\n    tPos  = new SimplexTableau(f, cs2, GoalType.MAXIMIZE, true, 1.0e-6);\n    if (tZero.getNumObjectiveFunctions() != 2) return;\n    if (tZero.getArtificialVariableOffset() <= tZero.getNumObjectiveFunctions()) return;\n    c = tZero.getNumObjectiveFunctions();\n    tZero.setEntry(0, c, 0.0);\n    tPos.setEntry(0, c, 1.0e-7);\n    beforeWZero = tZero.getWidth();\n    beforeWPos = tPos.getWidth();\n} catch (Exception e) { return; }\ntry {\n    tZero.dropPhase1Objective();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    tPos.dropPhase1Objective();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (beforeWZero != beforeWPos) throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn violated: mismatched initial widths\");\nif (tPos.getWidth() != tZero.getWidth() - 1) {\n    throw new RuntimeException(\"relation dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn violated: width after tiny-positive drop=\" + tPos.getWidth() + \", zero-case width=\" + tZero.getWidth());\n}"
  },
  {
    "name": "dropPhase1Objective_clearsArtificials_and_secondCallNoOp",
    "kind": "invariant",
    "contract": "dropPhase1Objective() removes the phase 1 objective function and artificial-variable columns it is supposed to drop; after that the tableau is in phase 2, so getNumArtificialVariables() must be 0, the height must be reduced by exactly one row, and a second call must return immediately because getNumObjectiveFunctions() == 1.",
    "input": "Build any valid phase-1 tableau by including at least one equality constraint so that artificial variables exist. Record width, height, and hashCode before and after the first drop, then call dropPhase1Objective() again on the already-dropped tableau.",
    "check": "SimplexTableau t; int beforeH; int afterH; int afterW; int hashAfterFirst; try {\n    double coeff = data.consumeInt(-4, 4);\n    if (coeff == 0.0) coeff = 1.0;\n    LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { coeff }, data.consumeInt(-2, 2));\n    java.util.List<LinearConstraint> cs = new java.util.ArrayList<LinearConstraint>();\n    cs.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, data.consumeInt(1, 5)));\n    t = new SimplexTableau(f, cs, data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE, true, 1.0e-6);\n    if (t.getNumObjectiveFunctions() != 2) return;\n    beforeH = t.getHeight();\n} catch (Exception e) { return; }\ntry {\n    t.dropPhase1Objective();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificials_and_secondCallNoOp violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nafterH = t.getHeight();\nafterW = t.getWidth();\nhashAfterFirst = t.hashCode();\nif (t.getNumArtificialVariables() != 0) {\n    throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificials_and_secondCallNoOp violated: artificial variables remain: \" + t.getNumArtificialVariables());\n}\nif (afterH != beforeH - 1) {\n    throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificials_and_secondCallNoOp violated: height \" + beforeH + \" -> \" + afterH);\n}\ntry {\n    t.dropPhase1Objective();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificials_and_secondCallNoOp violated: unexpected second-call \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (t.getNumArtificialVariables() != 0 || t.getHeight() != afterH || t.getWidth() != afterW || t.hashCode() != hashAfterFirst) {\n    throw new RuntimeException(\"relation dropPhase1Objective_clearsArtificials_and_secondCallNoOp violated: second call changed phase-2 tableau state\");\n}"
  },
  {
    "name": "linearObjectiveFunction_arrayAndVectorOverloadsAgree",
    "kind": "metamorphic",
    "contract": "LinearObjectiveFunction has two overloads to compute the value of the same linear equation at a point: getValue(double[]) and getValue(RealVector). Both are documented as computing the value of the linear equation at the current point, so equivalent points must yield the same value.",
    "input": "Build a LinearObjectiveFunction from a non-empty coefficient array and a point array of the same length. Wrap the same point in an ArrayRealVector for the sibling overload.",
    "check": "LinearObjectiveFunction f; double[] point; org.apache.commons.math3.linear.RealVector v; double a; double b; try {\n    int n = data.consumeInt(1, 6);\n    double[] coeffs = new double[n];\n    point = new double[n];\n    for (int i = 0; i < n; i++) {\n        coeffs[i] = data.consumeInt(-20, 20);\n        point[i] = data.consumeInt(-20, 20);\n    }\n    f = new LinearObjectiveFunction(coeffs, data.consumeInt(-20, 20));\n    v = new org.apache.commons.math3.linear.ArrayRealVector(point);\n    a = f.getValue(point);\n    b = f.getValue(v);\n} catch (Exception e) { return; }\ndouble diff = Math.abs(a - b);\ndouble scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));\nif (diff > 1.0e-9 * scale) {\n    throw new RuntimeException(\"relation linearObjectiveFunction_arrayAndVectorOverloadsAgree violated: \" + a + \" vs \" + b);\n}"
  },
  {
    "name": "linearObjectiveFunction_matchesDocumentedFormula",
    "kind": "invariant",
    "contract": "LinearObjectiveFunction is documented as c1*x1 + ... + cn*xn + d, and getValue(point) is documented to compute the value of that linear equation at the current point.",
    "input": "Build a non-empty coefficient array and same-length point array using fuzzed integers of modest magnitude, plus any constant term. Recompute the documented linear-expression formula independently.",
    "check": "LinearObjectiveFunction f; double[] coeffs; double[] point; double observed; double expected; try {\n    int n = data.consumeInt(1, 6);\n    coeffs = new double[n];\n    point = new double[n];\n    for (int i = 0; i < n; i++) {\n        coeffs[i] = data.consumeInt(-50, 50);\n        point[i] = data.consumeInt(-50, 50);\n    }\n    double cst = data.consumeInt(-50, 50);\n    f = new LinearObjectiveFunction(coeffs, cst);\n    observed = f.getValue(point);\n    expected = cst;\n    for (int i = 0; i < n; i++) {\n        expected += coeffs[i] * point[i];\n    }\n} catch (Exception e) { return; }\ndouble diff = Math.abs(observed - expected);\ndouble scale = Math.max(1.0, Math.max(Math.abs(observed), Math.abs(expected)));\nif (diff > 1.0e-9 * scale) {\n    throw new RuntimeException(\"relation linearObjectiveFunction_matchesDocumentedFormula violated: observed=\" + observed + \", expected=\" + expected);\n}"
  },
  {
    "name": "linearConstraint_twoEquationConstructorEquivalentToSingleEquationForm",
    "kind": "metamorphic",
    "contract": "The two-equation LinearConstraint constructor visibly computes coefficients = lhsCoefficients - rhsCoefficients and value = rhsConstant - lhsConstant while preserving the relationship. Therefore it must represent the same normalized constraint as the corresponding single-equation constructor.",
    "input": "Choose a dimension n>=1, lhs and rhs coefficient arrays of that length, lhsConstant, rhsConstant, and a relationship. Construct one constraint with the two-equation constructor and one with the normalized single-equation constructor using lhs-rhs and rhsConstant-lhsConstant.",
    "check": "LinearConstraint c1; LinearConstraint c2; double[] norm; try {\n    int n = data.consumeInt(1, 6);\n    double[] lhs = new double[n];\n    double[] rhs = new double[n];\n    norm = new double[n];\n    for (int i = 0; i < n; i++) {\n        lhs[i] = data.consumeInt(-20, 20);\n        rhs[i] = data.consumeInt(-20, 20);\n        norm[i] = lhs[i] - rhs[i];\n    }\n    double lhsC = data.consumeInt(-20, 20);\n    double rhsC = data.consumeInt(-20, 20);\n    Relationship rel = Relationship.values()[data.consumeInt(0, Relationship.values().length - 1)];\n    c1 = new LinearConstraint(lhs, lhsC, rel, rhs, rhsC);\n    c2 = new LinearConstraint(norm, rel, rhsC - lhsC);\n} catch (Exception e) { return; }\nif (c1.getRelationship() != c2.getRelationship()) {\n    throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorEquivalentToSingleEquationForm violated: relationship mismatch\");\n}\ndouble v1 = c1.getValue();\ndouble v2 = c2.getValue();\ndouble vdiff = Math.abs(v1 - v2);\ndouble vscale = Math.max(1.0, Math.max(Math.abs(v1), Math.abs(v2)));\nif (vdiff > 1.0e-9 * vscale) {\n    throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorEquivalentToSingleEquationForm violated: values \" + v1 + \" vs \" + v2);\n}\norg.apache.commons.math3.linear.RealVector r1 = c1.getCoefficients();\norg.apache.commons.math3.linear.RealVector r2 = c2.getCoefficients();\nif (r1.getDimension() != r2.getDimension()) {\n    throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorEquivalentToSingleEquationForm violated: coefficient dimensions differ\");\n}\nfor (int i = 0; i < r1.getDimension(); i++) {\n    double x = r1.getEntry(i);\n    double y = r2.getEntry(i);\n    double diff = Math.abs(x - y);\n    double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));\n    if (diff > 1.0e-9 * scale) {\n        throw new RuntimeException(\"relation linearConstraint_twoEquationConstructorEquivalentToSingleEquationForm violated at coeff \" + i + \": \" + x + \" vs \" + y);\n    }\n}"
  },
  {
    "name": "solverReturnedValueMatchesObjectiveAtReturnedPoint",
    "kind": "invariant",
    "contract": "LinearObjectiveFunction.getValue(point) computes the objective value at a point, and SimplexSolver.optimize returns a PointValuePair solution. For any correctly reported solution, solution.getValue() must equal the objective function evaluated at solution.getPoint().",
    "input": "Construct a simple bounded feasible LP by maximizing a linear objective over non-negative variables with upper-bound constraints xi <= ui. This avoids infeasible and unbounded cases while using the real solver API.",
    "check": "SimplexSolver solver; LinearObjectiveFunction f; java.util.Collection<LinearConstraint> cs; org.apache.commons.math3.optimization.PointValuePair sol; double reported; double recomputed; try {\n    int n = data.consumeInt(1, 4);\n    double[] coeffs = new double[n];\n    cs = new java.util.ArrayList<LinearConstraint>();\n    for (int i = 0; i < n; i++) {\n        coeffs[i] = data.consumeInt(-10, 10);\n        double[] unit = new double[n];\n        unit[i] = 1.0;\n        cs.add(new LinearConstraint(unit, Relationship.LEQ, data.consumeInt(0, 10)));\n    }\n    f = new LinearObjectiveFunction(coeffs, data.consumeInt(-10, 10));\n    solver = new SimplexSolver();\n    sol = solver.optimize(f, cs, GoalType.MAXIMIZE, true);\n    reported = sol.getValue();\n    recomputed = f.getValue(sol.getPoint());\n} catch (org.apache.commons.math3.exception.MathIllegalStateException e) { return; }\ncatch (org.apache.commons.math3.optimization.linear.UnboundedSolutionException e) { return; }\ncatch (org.apache.commons.math3.optimization.linear.NoFeasibleSolutionException e) { return; }\ncatch (Exception e) { return; }\ndouble diff = Math.abs(reported - recomputed);\ndouble scale = Math.max(1.0, Math.max(Math.abs(reported), Math.abs(recomputed)));\nif (diff > 1.0e-6 * scale) {\n    throw new RuntimeException(\"relation solverReturnedValueMatchesObjectiveAtReturnedPoint violated: reported=\" + reported + \", recomputed=\" + recomputed);\n}"
  }
]
```

</details>

---
## [21] ⚙️ screen · `dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~750 chars)</summary>

- reason: the probe `tZero.getNumObjectiveFunctions(...)` on the patch-changed class `SimplexTableau` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `SimplexTableau` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [22] ⚙️ screen-fuzz-buggy · `dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [23] ⚙️ screen · `dropPhase1Objective_clearsArtificials_and_secondCallNoOp`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~746 chars)</summary>

- reason: the probe `t.getNumObjectiveFunctions(...)` on the patch-changed class `SimplexTableau` runs inside the try at line 1, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `SimplexTableau` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [24] ⚙️ screen-fuzz-buggy · `dropPhase1Objective_clearsArtificials_and_secondCallNoOp`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [25] ⚙️ screen-fuzz-buggy · `linearObjectiveFunction_arrayAndVectorOverloadsAgree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [26] ⚙️ screen-fuzz-buggy · `linearObjectiveFunction_matchesDocumentedFormula`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [27] ⚙️ screen-fuzz-buggy · `linearConstraint_twoEquationConstructorEquivalentToSingleEqu`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [28] 🧠 LLM call — **compile-repair** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~3,889 chars)</summary>

**[system]**
```
You fix a Java snippet that failed to compile. Return ONLY the corrected snippet (the body that runs inside fuzzerTestOneInput, drawing inputs from `data`), no prose, no fences. Keep the SAME property being checked; change only what is needed to compile. Throw the violation with `throw new RuntimeException("relation <name> violated: "+...)` OUTSIDE any catch. Use only real API on the classpath. KEEP THE TWO-TIER CATCH the snippet uses and do NOT introduce a blanket `catch (Exception e) { return; }` around a call on the patch-changed class: setup exceptions are rejections to skip, but an exception from the probe on a valid-by-construction input is the violation and must be re-thrown as `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`.
```
**[user]**
```
Property (keep it): LinearObjectiveFunction.getValue(point) computes the objective value at a point, and SimplexSolver.optimize returns a PointValuePair solution. For any correctly reported solution, solution.getValue() must equal the objective function evaluated at solution.getPoint().
Available imports:
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

Snippet that failed:
SimplexSolver solver; LinearObjectiveFunction f; java.util.Collection<LinearConstraint> cs; org.apache.commons.math3.optimization.PointValuePair sol; double reported; double recomputed; try {
    int n = data.consumeInt(1, 4);
    double[] coeffs = new double[n];
    cs = new java.util.ArrayList<LinearConstraint>();
    for (int i = 0; i < n; i++) {
        coeffs[i] = data.consumeInt(-10, 10);
        double[] unit = new double[n];
        unit[i] = 1.0;
        cs.add(new LinearConstraint(unit, Relationship.LEQ, data.consumeInt(0, 10)));
    }
    f = new LinearObjectiveFunction(coeffs, data.consumeInt(-10, 10));
    solver = new SimplexSolver();
    sol = solver.optimize(f, cs, GoalType.MAXIMIZE, true);
    reported = sol.getValue();
    recomputed = f.getValue(sol.getPoint());
} catch (org.apache.commons.math3.exception.MathIllegalStateException e) { return; }
catch (org.apache.commons.math3.optimization.linear.UnboundedSolutionException e) { return; }
catch (org.apache.commons.math3.optimization.linear.NoFeasibleSolutionException e) { return; }
catch (Exception e) { return; }
double diff = Math.abs(reported - recomputed);
double scale = Math.max(1.0, Math.max(Math.abs(reported), Math.abs(recomputed)));
if (diff > 1.0e-6 * scale) {
    throw new RuntimeException("relation solverReturnedValueMatchesObjectiveAtReturnedPoint violated: reported=" + reported + ", recomputed=" + recomputed);
}

javac error:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/fuzz/relscreen_5/RelScreen5.java:45: error: exception UnboundedSolutionException has already been caught
catch (org.apache.commons.math3.optimization.linear.UnboundedSolutionException e) { return; }
^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/fuzz/relscreen_5/RelScreen5.java:46: error: exception NoFeasibleSolutionException has already been caught
catch (org.apache.commo

Corrected snippet:
```

</details>
<details open><summary>▸ Output (~2,084 chars)</summary>

```
org.apache.commons.math3.optimization.linear.SimplexSolver solver;
org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f;
java.util.Collection<org.apache.commons.math3.optimization.linear.LinearConstraint> cs;
org.apache.commons.math3.optimization.PointValuePair sol;
double reported;
double recomputed;

try {
    int n = data.consumeInt(1, 4);
    double[] coeffs = new double[n];
    cs = new java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint>();
    for (int i = 0; i < n; i++) {
        coeffs[i] = data.consumeInt(-10, 10);
        double[] unit = new double[n];
        unit[i] = 1.0;
        cs.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(
            unit,
            org.apache.commons.math3.optimization.linear.Relationship.LEQ,
            data.consumeInt(0, 10)
        ));
    }
    f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(coeffs, data.consumeInt(-10, 10));
    solver = new org.apache.commons.math3.optimization.linear.SimplexSolver();
    sol = solver.optimize(f, cs, GoalType.MAXIMIZE, true);
    if (sol == null) {
        return;
    }
    reported = sol.getValue();
} catch (org.apache.commons.math3.optimization.linear.UnboundedSolutionException e) {
    return;
} catch (org.apache.commons.math3.optimization.linear.NoFeasibleSolutionException e) {
    return;
} catch (org.apache.commons.math3.exception.MathIllegalStateException e) {
    return;
}

try {
    recomputed = f.getValue(sol.getPoint());
} catch (RuntimeException e) {
    throw new RuntimeException("relation solverReturnedValueMatchesObjectiveAtReturnedPoint violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}

double diff = Math.abs(reported - recomputed);
double scale = Math.max(1.0, Math.max(Math.abs(reported), Math.abs(recomputed)));
if (diff > 1.0e-6 * scale) {
    throw new RuntimeException("relation solverReturnedValueMatchesObjectiveAtReturnedPoint violated: reported=" + reported + ", recomputed=" + recomputed);
}
```

</details>

---
## [29] ⚙️ screen-fuzz-buggy · `solverReturnedValueMatchesObjectiveAtReturnedPoint`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [30] ⚙️ screen · `dropPhase1Objective_tinyPositiveCostDropsOneMoreColumn`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [31] ⚙️ screen · `dropPhase1Objective_clearsArtificials_and_secondCallNoOp`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [32] ⚙️ screen · `linearObjectiveFunction_arrayAndVectorOverloadsAgree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [33] ⚙️ screen · `linearObjectiveFunction_matchesDocumentedFormula`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [34] ⚙️ screen · `linearConstraint_twoEquationConstructorEquivalentToSingleEqu`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [35] ⚙️ screen · `solverReturnedValueMatchesObjectiveAtReturnedPoint`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [36] ⚙️ reference-widening · `enumerate`
**output:** **3 observable(s) to widen onto**
<details><summary>▸ reason (~451 chars)</summary>

- reason: the kept checks probe ['numobjectivefunctions', 'width', 'height', 'artificialvariableoffset', 'data', 'solution'] among the patched class's no-argument readers; 7 of those have no admitted reference and 3 fit under the per-leg cap of 3 (the leg already holds []); 14 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['add', 'for', 'dropphase1objective', 'setentry', 'writeobject', 'readobject'])

</details>
<details><summary>▸ detail (~2,609 chars)</summary>

- detail: {'targets': ['numobjectivefunctions', 'width', 'height'], 'already_admitted': [], 'cap': 3, 'checks_read': 12, 'excluded': [{'key': 'add', 'why': 'the patched class does not declare `add()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'for', 'why': "the patched class declares `for` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'dropphase1objective', 'why': 'the patched class declares `dropPhase1Objective()` as a constructor or a void method — it reports no observable value'}, {'key': 'setentry', 'why': "the patched class declares `setEntry` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'writeobject', 'why': "the patched class declares `writeObject` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'readobject', 'why': "the patched class declares `readObject` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'linearconstraint', 'why': 'the patched class does not declare `LinearConstraint()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'relationship', 'why': 'the patched class does not declare `getRelationship()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'linearobjectivefunction', 'why': 'the patched class does not declare `LinearObjectiveFunction()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'value', 'why': 'the patched class does not declare `getValue()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'simplexsolver', 'why': 'the patched class does not declare `SimplexSolver()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'precision', 'why': 'the patched class does not declare `Precision()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 14}

</details>

---
## [37] ⚙️ reference-widening · `getNumObjectiveFunctions`
**output:** **reference REQUESTED for observable `numobjectivefunctions`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [38] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **widening target requested**
- detail: {'candidates': ['getNumObjectiveFunctions'], 'attempting': 'getNumObjectiveFunctions'}

---
## [39] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **screening surface resolved**
- reason: 1 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['hashCode'], 'patch_touched_exempted': [], 'screened_siblings': ['hashCode'], 'declaring_types': ['SimplexTableau']}

---
## [40] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~50,659 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
**[user]**
```
Implement `getNumObjectiveFunctions` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either { /* body withheld */ }
     * or { /* body withheld */ }
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) { /* body withheld */ }
</class>}

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { /* body withheld */ }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) { /* body withheld */ }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) { /* body withheld */ }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { /* body withheld */ }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { /* body withheld */ }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() { /* body withheld */ }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) { /* body withheld */ }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { /* body withheld */ }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See { /* body withheld */ }
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { /* body withheld */ }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { /* body withheld */ }
     * { /* body withheld */ }
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    @Test
    public void testMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);
        Assert.assertEquals(2.0d, solution.getValue(), epsilon);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either { /* body withheld */ }
     * or { /* body withheld */ }
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) { /* body withheld */ }
</class>

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { /* body withheld */ }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) { /* body withheld */ }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) { /* body withheld */ }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { /* body withheld */ }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { /* body withheld */ }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() { /* body withheld */ }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) { /* body withheld */ }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { /* body withheld */ }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See { /* body withheld */ }
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { /* body withheld */ }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { /* body withheld */ }
     * { /* body withheld */ }
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { /* body withheld */ }
}
</class>

<class name="Relationship" role="collaborator">
     * Types of relationships between two cells in a Solver {@link LinearConstraint}.
     * @version $Id$
     * @since 2.0
public enum Relationship {
     * Equality relationship. */
     * EQ("="),
     * /** Lesser than or equal relationship. */
     * LEQ("<="),
     * /** Greater than or equal relationship. */
     * GEQ(">=");
     * /** Display string for the relationship.
    private final String stringValue;
     * Simple constructor.
     * @param stringValue display string for the relationship
    private Relationship(String stringValue) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String toString() { /* body withheld */ }
     * Get the relationship obtained when multiplying all coefficients by -1.
     * @return relationship obtained when multiplying all coefficients by -1
    public Relationship oppositeRelationship() { /* body withheld */ }
}
</class>

<class name="GoalType" role="collaborator">
     * Goal type for an optimization problem.
     * @version $Id$
     * @since 2.0
public enum GoalType implements Serializable {

}
</class>

<class name="Precision" role="collaborator">
     * Utilities for comparing numbers.
     * @since 3.0
     * @version $Id$
public class Precision {
     * Smallest positive number such that { /* body withheld */ } is not
     * numerically equal to 1.
     * <br/>
     * In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
    public static final double EPSILON;
     * Safe minimum, such that { /* body withheld */ } does not overflow.
     * <br/>
     * In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.
    public static final double SAFE_MIN;
     * Exponent offset in IEEE754 representation.
    private static final long EXPONENT_OFFSET = 1023l;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * Offset to order signed double numbers lexicographically.
    private static final int SGN_MASK_FLOAT = 0x80000000;
     * Private constructor.
    private Precision() { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * <li>&lt; 0 if !{ /* body withheld */ } &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{ /* body withheld */ } &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are { /* body withheld */ }
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * …
    public static int compareTo(final double x, final double y, final int maxUlps) { /* body withheld */ }
     * Returns true iff they are equal as defined by
     * { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @return { /* body withheld */ } if the values are equal.
    public static boolean equals(float x, float y) { /* body withheld */ }
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @return { /* body withheld */ } if the values are equal or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y) { /* body withheld */ }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return { /* body withheld */ } if the values are equal or within range of each other.
     * @since 2.2
    public static boolean equals(float x, float y, float eps) { /* body withheld */ }
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return { /* body withheld */ } if the values are equal or within range of each other,
     * or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y, float eps) { /* body withheld */ }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are { /* body withheld */ }
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * …
    public static boolean equals(float x, float y, int maxUlps) { /* body withheld */ }
     * Returns true if both arguments are NaN or if they are equal as defined
     * by { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * @return { /* body withheld */ } if both arguments are NaN or if there are less than
     * { /* body withheld */ } floating point values between { /* body withheld */ } and { /* body withheld */ }.
     * @since 2.2
    public static boolean equalsIncludingNaN(double x, double y, int maxUlps) { /* body withheld */ }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the { /* body withheld */ } method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in { /* body withheld */ }, moved as of version 3.0)
    public static double round(double x, int scale) { /* body withheld */ }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the { /* body withheld */ } method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in { /* body withheld */ }, moved as of version 3.0)
    public static float round(float x, int scale) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Array2DRowRealMatrix" role="collaborator">
     * Implementation of {@link RealMatrix} using a { /* body withheld */ } array to
     * store entries.
     * @version $Id$
public class Array2DRowRealMatrix extends AbstractRealMatrix implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1067294169172445528L;
     * Entries of the matrix.
    private double data[][];
     * Creates a matrix with no data
    public Array2DRowRealMatrix() { /* body withheld */ }
     * Create a new RealMatrix with the supplied row and column dimensions.
     * @param rowDimension Number of rows in the new matrix.
     * @param columnDimension Number of columns in the new matrix.
     * @throws org.apache.commons.math3.exception.NotStrictlyPositiveException
     * if the row or column dimension is not positive.
    public Array2DRowRealMatrix(final int rowDimension, final int columnDimension) { /* body withheld */ }
     * Create a new { /* body withheld */ } using the input array as the underlying
     * data array.
     * <p>The input array is copied, not referenced. This constructor has
     * the same effect as calling { /* body withheld */ }
     * with the second argument set to { /* body withheld */ }.</p>
     * @param d Data for the new matrix.
     * @throws DimensionMismatchException if { /* body withheld */ } is not rectangular.
     * @throws NoDataException if { /* body withheld */ } row or colum dimension is zero.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * @see #Array2DRowRealMatrix(double[][], boolean)
    public Array2DRowRealMatrix(final double[][] d) throws DimensionMismatchException, NoDataException, NullArgumentException { /* body withheld */ }
     * Create a new RealMatrix using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * RealMatrix and not used directly, the { /* body withheld */ } may be
     * set to { /* body withheld */ }. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * @param d Data for new matrix.
     * @param copyArray if { /* body withheld */ }, the input array will be copied,
     * otherwise it will be referenced.
     * @throws DimensionMismatchException if { /* body withheld */ } is not rectangular
     * (not all rows have the same length) or empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * …
    public Array2DRowRealMatrix(final double[][] d, final boolean copyArray) { /* body withheld */ }
     * Create a new (column) RealMatrix using { /* body withheld */ } as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * @param v Column vector holding data for new matrix.
    public Array2DRowRealMatrix(final double[] v) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public RealMatrix createMatrix(final int rowDimension, final int columnDimension) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public RealMatrix copy() { /* body withheld */ }
     * Compute the sum of this matrix with { /* body withheld */ }.
     * @param m Matrix to be added.
     * @return { /* body withheld */ } + m.
     * @throws MatrixDimensionMismatchException
     * if { /* body withheld */ } is not the same size as this matrix.
    public Array2DRowRealMatrix add(final Array2DRowRealMatrix m) { /* body withheld */ }
     * Subtract { /* body withheld */ } from this matrix.
     * @param m Matrix to be subtracted.
     * @return { /* body withheld */ } - m.
     * @throws MatrixDimensionMismatchException
     * if { /* body withheld */ } is not the same size as this matrix.
    public Array2DRowRealMatrix subtract(final Array2DRowRealMatrix m) { /* body withheld */ }
     * Postmultiplying this matrix by { /* body withheld */ }.
     * @param m Matrix to postmultiply by.
     * @return { /* body withheld */ } * m.
     * @throws DimensionMismatchException if the number of columns of this
     * matrix is not equal to the number of rows of { /* body withheld */ }.
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[][] getData() { /* body withheld */ }
     * Get a reference to the underlying data array.
     * @return 2-dimensional array of entries.
    public double[][] getDataRef() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void setSubMatrix(final double[][] subMatrix, final int row, final int column) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double getEntry(final int row, final int column) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void setEntry(final int row, final int column, final double value) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void addToEntry(final int row, final int column, final double increment) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void multiplyEntry(final int row, final int column, final double factor) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int getRowDimension() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int getColumnDimension() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[] operate(final double[] v) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[] preMultiply(final double[] v) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Id$
public class MatrixUtils {
     * Private constructor.
    private MatrixUtils() { /* body withheld */ }
     * Returns a { /* body withheld */ } with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a { /* body withheld */ } instance is built. Above this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The matrix elements are all set to 0.0.</p>
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  RealMatrix with specified dimensions
     * @see #createRealMatrix(double[][])
    public static RealMatrix createRealMatrix(final int rows, final int columns) { /* body withheld */ }
     * Returns a { /* body withheld */ } with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a { /* body withheld */ } instance is built. Above
     * this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The matrix elements are all set to field.getZero().</p>
     * @param <T> the type of the field elements
     * @param field field to which the matrix elements belong
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  FieldMatrix with specified dimensions
     * @see #createFieldMatrix(FieldElement[][])
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(final Field<T> field, final int rows, final int columns) { /* body withheld */ }
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a { /* body withheld */ } instance is built. Above this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if { /* body withheld */ } is not rectangular (not all rows have the same length).
     * …
    public static RealMatrix createRealMatrix(double[][] data) { /* body withheld */ }
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a { /* body withheld */ } instance is built. Above
     * this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param <T> the type of the field elements
     * @param data input array
     * @return a matrix containing the values of the array.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if { /* body withheld */ } is not rectangular (not all rows have the same length).
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(T[][] data) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
    public static RealMatrix createRealIdentityMatrix(int dimension) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param <T> the type of the field elements
     * @param field field to which the elements belong
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { /* body withheld */ }
     * Returns a diagonal matrix with specified elements.
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static RealMatrix createRealDiagonalMatrix(final double[] diagonal) { /* body withheld */ }
     * Returns a diagonal matrix with specified elements.
     * @param <T> the type of the field elements
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldDiagonalMatrix(final T[] diagonal) { /* body withheld */ }
     * Creates a { /* body withheld */ } using the data from the input array.
     * @param data the input data
     * @return a data.length RealVector
     * @throws NoDataException if { /* body withheld */ } is empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
    public static RealVector createRealVector(double[] data) { /* body withheld */ }
     * Creates a { /* body withheld */ } using the data from the input array.
     * @param <T> the type of the field elements
     * @param data the input data
     * @return a data.length FieldVector
     * @throws NoDataException if { /* body withheld */ } is empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * @throws ZeroException if { /* body withheld */ } has 0 elements
    public static <T extends FieldElement<T>> FieldVector<T> createFieldVector(final T[] data) { /* body withheld */ }
     * Converter for { /* body withheld */ }/{ /* body withheld */ }.
    private static class FractionMatrixConverter extends DefaultFieldMatrixPreservingVisitor<Fraction> { … }
}
// … (non-patched members truncated; patched methods kept)
</class>}

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `hashCode`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~222 chars)</summary>

```
// compute(int) : getNumObjectiveFunctions
public class ReferenceImpl {

    public static int compute_getNumObjectiveFunctions(final int numArtificialVariables) {
        return numArtificialVariables > 0 ? 2 : 1;
    }
}
```

</details>

---
## [41] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **reference generated**
- detail: {'chars': 222}

---
## [42] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **signature declared**
- detail: {'signature': 'int'}

---
## [43] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'int', 'named': 'int numArtificialVariables'}

---
## [44] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **reference observables matched**
- detail: {'matched': {'getNumObjectiveFunctions': 'getNumObjectiveFunctions'}, 'declared_only': []}

---
## [45] ⚙️ reference-impl · `getNumObjectiveFunctions`
**output:** **reference too thin to screen — DISCARDED**
- reason: 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [46] ⚙️ reference-widening · `getNumObjectiveFunctions`
**output:** **widening REJECTED for observable `numobjectivefunctions`**
- reason: reference too thin to screen — DISCARDED — 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [47] ⚙️ reference-widening · `getWidth`
**output:** **reference REQUESTED for observable `width`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [48] ⚙️ reference-impl · `getWidth`
**output:** **widening target requested**
- detail: {'candidates': ['getWidth'], 'attempting': 'getWidth'}

---
## [49] ⚙️ reference-impl · `getWidth`
**output:** **screening surface resolved**
- reason: 1 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['hashCode'], 'patch_touched_exempted': [], 'screened_siblings': ['hashCode'], 'declaring_types': ['SimplexTableau']}

---
## [50] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~50,357 chars, 1 new)</summary>

- *[system] message: identical to step [40] — not reprinted*
**[user]**
```
Implement `getWidth` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either { /* body withheld */ }
     * or { /* body withheld */ }
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) { /* body withheld */ }
</class>}

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { /* body withheld */ }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) { /* body withheld */ }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) { /* body withheld */ }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { /* body withheld */ }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { /* body withheld */ }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() { /* body withheld */ }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) { /* body withheld */ }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { /* body withheld */ }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See { /* body withheld */ }
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { /* body withheld */ }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { /* body withheld */ }
     * { /* body withheld */ }
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    @Test
    public void testMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);
        Assert.assertEquals(2.0d, solution.getValue(), epsilon);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either { /* body withheld */ }
     * or { /* body withheld */ }
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) { /* body withheld */ }
</class>

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { /* body withheld */ }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) { /* body withheld */ }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) { /* body withheld */ }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { /* body withheld */ }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { /* body withheld */ }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() { /* body withheld */ }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) { /* body withheld */ }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { /* body withheld */ }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See { /* body withheld */ }
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { /* body withheld */ }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { /* body withheld */ }
     * { /* body withheld */ }
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { /* body withheld */ }
}
</class>

<class name="Relationship" role="collaborator">
     * Types of relationships between two cells in a Solver {@link LinearConstraint}.
     * @version $Id$
     * @since 2.0
public enum Relationship {
     * Equality relationship. */
     * EQ("="),
     * /** Lesser than or equal relationship. */
     * LEQ("<="),
     * /** Greater than or equal relationship. */
     * GEQ(">=");
     * /** Display string for the relationship.
    private final String stringValue;
     * Simple constructor.
     * @param stringValue display string for the relationship
    private Relationship(String stringValue) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String toString() { /* body withheld */ }
     * Get the relationship obtained when multiplying all coefficients by -1.
     * @return relationship obtained when multiplying all coefficients by -1
    public Relationship oppositeRelationship() { /* body withheld */ }
}
</class>

<class name="GoalType" role="collaborator">
     * Goal type for an optimization problem.
     * @version $Id$
     * @since 2.0
public enum GoalType implements Serializable {

}
</class>

<class name="Precision" role="collaborator">
     * Utilities for comparing numbers.
     * @since 3.0
     * @version $Id$
public class Precision {
     * Smallest positive number such that { /* body withheld */ } is not
     * numerically equal to 1.
     * <br/>
     * In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
    public static final double EPSILON;
     * Safe minimum, such that { /* body withheld */ } does not overflow.
     * <br/>
     * In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.
    public static final double SAFE_MIN;
     * Exponent offset in IEEE754 representation.
    private static final long EXPONENT_OFFSET = 1023l;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * Offset to order signed double numbers lexicographically.
    private static final int SGN_MASK_FLOAT = 0x80000000;
     * Private constructor.
    private Precision() { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * <li>&lt; 0 if !{ /* body withheld */ } &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{ /* body withheld */ } &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are { /* body withheld */ }
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * …
    public static int compareTo(final double x, final double y, final int maxUlps) { /* body withheld */ }
     * Returns true iff they are equal as defined by
     * { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @return { /* body withheld */ } if the values are equal.
    public static boolean equals(float x, float y) { /* body withheld */ }
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @return { /* body withheld */ } if the values are equal or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y) { /* body withheld */ }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return { /* body withheld */ } if the values are equal or within range of each other.
     * @since 2.2
    public static boolean equals(float x, float y, float eps) { /* body withheld */ }
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return { /* body withheld */ } if the values are equal or within range of each other,
     * or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y, float eps) { /* body withheld */ }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are { /* body withheld */ }
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * …
    public static boolean equals(float x, float y, int maxUlps) { /* body withheld */ }
     * Returns true if both arguments are NaN or if they are equal as defined
     * by { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * @return { /* body withheld */ } if both arguments are NaN or if there are less than
     * { /* body withheld */ } floating point values between { /* body withheld */ } and { /* body withheld */ }.
     * @since 2.2
    public static boolean equalsIncludingNaN(double x, double y, int maxUlps) { /* body withheld */ }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the { /* body withheld */ } method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in { /* body withheld */ }, moved as of version 3.0)
    public static double round(double x, int scale) { /* body withheld */ }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the { /* body withheld */ } method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in { /* body withheld */ }, moved as of version 3.0)
    public static float round(float x, int scale) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Array2DRowRealMatrix" role="collaborator">
     * Implementation of {@link RealMatrix} using a { /* body withheld */ } array to
     * store entries.
     * @version $Id$
public class Array2DRowRealMatrix extends AbstractRealMatrix implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1067294169172445528L;
     * Entries of the matrix.
    private double data[][];
     * Creates a matrix with no data
    public Array2DRowRealMatrix() { /* body withheld */ }
     * Create a new RealMatrix with the supplied row and column dimensions.
     * @param rowDimension Number of rows in the new matrix.
     * @param columnDimension Number of columns in the new matrix.
     * @throws org.apache.commons.math3.exception.NotStrictlyPositiveException
     * if the row or column dimension is not positive.
    public Array2DRowRealMatrix(final int rowDimension, final int columnDimension) { /* body withheld */ }
     * Create a new { /* body withheld */ } using the input array as the underlying
     * data array.
     * <p>The input array is copied, not referenced. This constructor has
     * the same effect as calling { /* body withheld */ }
     * with the second argument set to { /* body withheld */ }.</p>
     * @param d Data for the new matrix.
     * @throws DimensionMismatchException if { /* body withheld */ } is not rectangular.
     * @throws NoDataException if { /* body withheld */ } row or colum dimension is zero.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * @see #Array2DRowRealMatrix(double[][], boolean)
    public Array2DRowRealMatrix(final double[][] d) throws DimensionMismatchException, NoDataException, NullArgumentException { /* body withheld */ }
     * Create a new RealMatrix using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * RealMatrix and not used directly, the { /* body withheld */ } may be
     * set to { /* body withheld */ }. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * @param d Data for new matrix.
     * @param copyArray if { /* body withheld */ }, the input array will be copied,
     * otherwise it will be referenced.
     * @throws DimensionMismatchException if { /* body withheld */ } is not rectangular
     * (not all rows have the same length) or empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * …
    public Array2DRowRealMatrix(final double[][] d, final boolean copyArray) { /* body withheld */ }
     * Create a new (column) RealMatrix using { /* body withheld */ } as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * @param v Column vector holding data for new matrix.
    public Array2DRowRealMatrix(final double[] v) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public RealMatrix createMatrix(final int rowDimension, final int columnDimension) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public RealMatrix copy() { /* body withheld */ }
     * Compute the sum of this matrix with { /* body withheld */ }.
     * @param m Matrix to be added.
     * @return { /* body withheld */ } + m.
     * @throws MatrixDimensionMismatchException
     * if { /* body withheld */ } is not the same size as this matrix.
    public Array2DRowRealMatrix add(final Array2DRowRealMatrix m) { /* body withheld */ }
     * Subtract { /* body withheld */ } from this matrix.
     * @param m Matrix to be subtracted.
     * @return { /* body withheld */ } - m.
     * @throws MatrixDimensionMismatchException
     * if { /* body withheld */ } is not the same size as this matrix.
    public Array2DRowRealMatrix subtract(final Array2DRowRealMatrix m) { /* body withheld */ }
     * Postmultiplying this matrix by { /* body withheld */ }.
     * @param m Matrix to postmultiply by.
     * @return { /* body withheld */ } * m.
     * @throws DimensionMismatchException if the number of columns of this
     * matrix is not equal to the number of rows of { /* body withheld */ }.
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[][] getData() { /* body withheld */ }
     * Get a reference to the underlying data array.
     * @return 2-dimensional array of entries.
    public double[][] getDataRef() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void setSubMatrix(final double[][] subMatrix, final int row, final int column) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double getEntry(final int row, final int column) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void setEntry(final int row, final int column, final double value) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void addToEntry(final int row, final int column, final double increment) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void multiplyEntry(final int row, final int column, final double factor) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int getRowDimension() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int getColumnDimension() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[] operate(final double[] v) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[] preMultiply(final double[] v) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Id$
public class MatrixUtils {
     * Private constructor.
    private MatrixUtils() { /* body withheld */ }
     * Returns a { /* body withheld */ } with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a { /* body withheld */ } instance is built. Above this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The matrix elements are all set to 0.0.</p>
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  RealMatrix with specified dimensions
     * @see #createRealMatrix(double[][])
    public static RealMatrix createRealMatrix(final int rows, final int columns) { /* body withheld */ }
     * Returns a { /* body withheld */ } with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a { /* body withheld */ } instance is built. Above
     * this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The matrix elements are all set to field.getZero().</p>
     * @param <T> the type of the field elements
     * @param field field to which the matrix elements belong
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  FieldMatrix with specified dimensions
     * @see #createFieldMatrix(FieldElement[][])
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(final Field<T> field, final int rows, final int columns) { /* body withheld */ }
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a { /* body withheld */ } instance is built. Above this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if { /* body withheld */ } is not rectangular (not all rows have the same length).
     * …
    public static RealMatrix createRealMatrix(double[][] data) { /* body withheld */ }
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a { /* body withheld */ } instance is built. Above
     * this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param <T> the type of the field elements
     * @param data input array
     * @return a matrix containing the values of the array.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if { /* body withheld */ } is not rectangular (not all rows have the same length).
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(T[][] data) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
    public static RealMatrix createRealIdentityMatrix(int dimension) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param <T> the type of the field elements
     * @param field field to which the elements belong
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { /* body withheld */ }
     * Returns a diagonal matrix with specified elements.
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static RealMatrix createRealDiagonalMatrix(final double[] diagonal) { /* body withheld */ }
     * Returns a diagonal matrix with specified elements.
     * @param <T> the type of the field elements
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldDiagonalMatrix(final T[] diagonal) { /* body withheld */ }
     * Creates a { /* body withheld */ } using the data from the input array.
     * @param data the input data
     * @return a data.length RealVector
     * @throws NoDataException if { /* body withheld */ } is empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
    public static RealVector createRealVector(double[] data) { /* body withheld */ }
     * Creates a { /* body withheld */ } using the data from the input array.
     * @param <T> the type of the field elements
     * @param data the input data
     * @return a data.length FieldVector
     * @throws NoDataException if { /* body withheld */ } is empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * @throws ZeroException if { /* body withheld */ } has 0 elements
    public static <T extends FieldElement<T>> FieldVector<T> createFieldVector(final T[] data) { /* body withheld */ }
     * Converter for { /* body withheld */ }/{ /* body withheld */ }.
    private static class FractionMatrixConverter extends DefaultFieldMatrixPreservingVisitor<Fraction> { … }
}
// … (non-patched members truncated; patched methods kept)
</class>}

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `hashCode`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~806 chars)</summary>

```
// compute(int, int, int) : getNumObjectiveFunctions, getWidth
public class ReferenceImpl {
    public static int compute_getNumObjectiveFunctions(
            int numDecisionVariables,
            int numSlackVariables,
            int numArtificialVariables) {
        return numArtificialVariables > 0 ? 2 : 1;
    }

    public static int compute_getWidth(
            int numDecisionVariables,
            int numSlackVariables,
            int numArtificialVariables) {
        return numDecisionVariables
                + numSlackVariables
                + numArtificialVariables
                + compute_getNumObjectiveFunctions(
                        numDecisionVariables,
                        numSlackVariables,
                        numArtificialVariables)
                + 1;
    }
}
```

</details>

---
## [51] ⚙️ reference-impl · `getWidth`
**output:** **reference generated**
- detail: {'chars': 806}

---
## [52] ⚙️ reference-impl · `getWidth`
**output:** **signature declared**
- detail: {'signature': 'int, int, int'}

---
## [53] ⚙️ reference-impl · `getWidth`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'int, int, int', 'named': 'int numDecisionVariables, int numSlackVariables, int numArtificialVariables'}

---
## [54] ⚙️ reference-impl · `getWidth`
**output:** **reference observables matched**
- detail: {'matched': {'getWidth': 'getWidth'}, 'declared_only': ['getNumObjectiveFunctions']}

---
## [55] ⚙️ reference-impl · `getWidth`
**output:** **reference too thin to screen — DISCARDED**
- reason: 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [56] ⚙️ reference-widening · `getWidth`
**output:** **widening REJECTED for observable `width`**
- reason: reference too thin to screen — DISCARDED — 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [57] ⚙️ reference-widening · `getHeight`
**output:** **reference REQUESTED for observable `height`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [58] ⚙️ reference-impl · `getHeight`
**output:** **widening target requested**
- detail: {'candidates': ['getHeight'], 'attempting': 'getHeight'}

---
## [59] ⚙️ reference-impl · `getHeight`
**output:** **screening surface resolved**
- reason: 1 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['hashCode'], 'patch_touched_exempted': [], 'screened_siblings': ['hashCode'], 'declaring_types': ['SimplexTableau']}

---
## [60] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~50,358 chars, 1 new)</summary>

- *[system] message: identical to step [40] — not reprinted*
**[user]**
```
Implement `getHeight` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either { /* body withheld */ }
     * or { /* body withheld */ }
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) { /* body withheld */ }
</class>}

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { /* body withheld */ }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) { /* body withheld */ }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) { /* body withheld */ }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { /* body withheld */ }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { /* body withheld */ }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() { /* body withheld */ }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) { /* body withheld */ }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { /* body withheld */ }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See { /* body withheld */ }
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { /* body withheld */ }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { /* body withheld */ }
     * { /* body withheld */ }
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    @Test
    public void testMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);
        Assert.assertEquals(2.0d, solution.getValue(), epsilon);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either { /* body withheld */ }
     * or { /* body withheld */ }
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) { /* body withheld */ }
</class>

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) { /* body withheld */ }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { /* body withheld */ }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) { /* body withheld */ }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) { /* body withheld */ }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { /* body withheld */ }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { /* body withheld */ }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public boolean equals(Object other) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int hashCode() { /* body withheld */ }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { /* body withheld */ }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { /* body withheld */ }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() { /* body withheld */ }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) { /* body withheld */ }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { /* body withheld */ }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See { /* body withheld */ }
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { /* body withheld */ }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { /* body withheld */ }
     * { /* body withheld */ }
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { /* body withheld */ }
}
</class>

<class name="Relationship" role="collaborator">
     * Types of relationships between two cells in a Solver {@link LinearConstraint}.
     * @version $Id$
     * @since 2.0
public enum Relationship {
     * Equality relationship. */
     * EQ("="),
     * /** Lesser than or equal relationship. */
     * LEQ("<="),
     * /** Greater than or equal relationship. */
     * GEQ(">=");
     * /** Display string for the relationship.
    private final String stringValue;
     * Simple constructor.
     * @param stringValue display string for the relationship
    private Relationship(String stringValue) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String toString() { /* body withheld */ }
     * Get the relationship obtained when multiplying all coefficients by -1.
     * @return relationship obtained when multiplying all coefficients by -1
    public Relationship oppositeRelationship() { /* body withheld */ }
}
</class>

<class name="GoalType" role="collaborator">
     * Goal type for an optimization problem.
     * @version $Id$
     * @since 2.0
public enum GoalType implements Serializable {

}
</class>

<class name="Precision" role="collaborator">
     * Utilities for comparing numbers.
     * @since 3.0
     * @version $Id$
public class Precision {
     * Smallest positive number such that { /* body withheld */ } is not
     * numerically equal to 1.
     * <br/>
     * In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
    public static final double EPSILON;
     * Safe minimum, such that { /* body withheld */ } does not overflow.
     * <br/>
     * In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.
    public static final double SAFE_MIN;
     * Exponent offset in IEEE754 representation.
    private static final long EXPONENT_OFFSET = 1023l;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * Offset to order signed double numbers lexicographically.
    private static final int SGN_MASK_FLOAT = 0x80000000;
     * Private constructor.
    private Precision() { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * <li>&lt; 0 if !{ /* body withheld */ } &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{ /* body withheld */ } &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are { /* body withheld */ }
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * …
    public static int compareTo(final double x, final double y, final int maxUlps) { /* body withheld */ }
     * Returns true iff they are equal as defined by
     * { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @return { /* body withheld */ } if the values are equal.
    public static boolean equals(float x, float y) { /* body withheld */ }
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @return { /* body withheld */ } if the values are equal or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y) { /* body withheld */ }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return { /* body withheld */ } if the values are equal or within range of each other.
     * @since 2.2
    public static boolean equals(float x, float y, float eps) { /* body withheld */ }
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return { /* body withheld */ } if the values are equal or within range of each other,
     * or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y, float eps) { /* body withheld */ }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are { /* body withheld */ }
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * …
    public static boolean equals(float x, float y, int maxUlps) { /* body withheld */ }
     * Returns true if both arguments are NaN or if they are equal as defined
     * by { /* body withheld */ }.
     * @param x first value
     * @param y second value
     * @param maxUlps { /* body withheld */ } is the number of floating point
     * values between { /* body withheld */ } and { /* body withheld */ }.
     * @return { /* body withheld */ } if both arguments are NaN or if there are less than
     * { /* body withheld */ } floating point values between { /* body withheld */ } and { /* body withheld */ }.
     * @since 2.2
    public static boolean equalsIncludingNaN(double x, double y, int maxUlps) { /* body withheld */ }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the { /* body withheld */ } method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in { /* body withheld */ }, moved as of version 3.0)
    public static double round(double x, int scale) { /* body withheld */ }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the { /* body withheld */ } method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in { /* body withheld */ }, moved as of version 3.0)
    public static float round(float x, int scale) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Array2DRowRealMatrix" role="collaborator">
     * Implementation of {@link RealMatrix} using a { /* body withheld */ } array to
     * store entries.
     * @version $Id$
public class Array2DRowRealMatrix extends AbstractRealMatrix implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1067294169172445528L;
     * Entries of the matrix.
    private double data[][];
     * Creates a matrix with no data
    public Array2DRowRealMatrix() { /* body withheld */ }
     * Create a new RealMatrix with the supplied row and column dimensions.
     * @param rowDimension Number of rows in the new matrix.
     * @param columnDimension Number of columns in the new matrix.
     * @throws org.apache.commons.math3.exception.NotStrictlyPositiveException
     * if the row or column dimension is not positive.
    public Array2DRowRealMatrix(final int rowDimension, final int columnDimension) { /* body withheld */ }
     * Create a new { /* body withheld */ } using the input array as the underlying
     * data array.
     * <p>The input array is copied, not referenced. This constructor has
     * the same effect as calling { /* body withheld */ }
     * with the second argument set to { /* body withheld */ }.</p>
     * @param d Data for the new matrix.
     * @throws DimensionMismatchException if { /* body withheld */ } is not rectangular.
     * @throws NoDataException if { /* body withheld */ } row or colum dimension is zero.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * @see #Array2DRowRealMatrix(double[][], boolean)
    public Array2DRowRealMatrix(final double[][] d) throws DimensionMismatchException, NoDataException, NullArgumentException { /* body withheld */ }
     * Create a new RealMatrix using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * RealMatrix and not used directly, the { /* body withheld */ } may be
     * set to { /* body withheld */ }. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * @param d Data for new matrix.
     * @param copyArray if { /* body withheld */ }, the input array will be copied,
     * otherwise it will be referenced.
     * @throws DimensionMismatchException if { /* body withheld */ } is not rectangular
     * (not all rows have the same length) or empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * …
    public Array2DRowRealMatrix(final double[][] d, final boolean copyArray) { /* body withheld */ }
     * Create a new (column) RealMatrix using { /* body withheld */ } as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * @param v Column vector holding data for new matrix.
    public Array2DRowRealMatrix(final double[] v) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public RealMatrix createMatrix(final int rowDimension, final int columnDimension) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public RealMatrix copy() { /* body withheld */ }
     * Compute the sum of this matrix with { /* body withheld */ }.
     * @param m Matrix to be added.
     * @return { /* body withheld */ } + m.
     * @throws MatrixDimensionMismatchException
     * if { /* body withheld */ } is not the same size as this matrix.
    public Array2DRowRealMatrix add(final Array2DRowRealMatrix m) { /* body withheld */ }
     * Subtract { /* body withheld */ } from this matrix.
     * @param m Matrix to be subtracted.
     * @return { /* body withheld */ } - m.
     * @throws MatrixDimensionMismatchException
     * if { /* body withheld */ } is not the same size as this matrix.
    public Array2DRowRealMatrix subtract(final Array2DRowRealMatrix m) { /* body withheld */ }
     * Postmultiplying this matrix by { /* body withheld */ }.
     * @param m Matrix to postmultiply by.
     * @return { /* body withheld */ } * m.
     * @throws DimensionMismatchException if the number of columns of this
     * matrix is not equal to the number of rows of { /* body withheld */ }.
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[][] getData() { /* body withheld */ }
     * Get a reference to the underlying data array.
     * @return 2-dimensional array of entries.
    public double[][] getDataRef() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void setSubMatrix(final double[][] subMatrix, final int row, final int column) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double getEntry(final int row, final int column) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void setEntry(final int row, final int column, final double value) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void addToEntry(final int row, final int column, final double increment) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public void multiplyEntry(final int row, final int column, final double factor) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int getRowDimension() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public int getColumnDimension() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[] operate(final double[] v) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public double[] preMultiply(final double[] v) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Id$
public class MatrixUtils {
     * Private constructor.
    private MatrixUtils() { /* body withheld */ }
     * Returns a { /* body withheld */ } with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a { /* body withheld */ } instance is built. Above this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The matrix elements are all set to 0.0.</p>
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  RealMatrix with specified dimensions
     * @see #createRealMatrix(double[][])
    public static RealMatrix createRealMatrix(final int rows, final int columns) { /* body withheld */ }
     * Returns a { /* body withheld */ } with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a { /* body withheld */ } instance is built. Above
     * this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The matrix elements are all set to field.getZero().</p>
     * @param <T> the type of the field elements
     * @param field field to which the matrix elements belong
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  FieldMatrix with specified dimensions
     * @see #createFieldMatrix(FieldElement[][])
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(final Field<T> field, final int rows, final int columns) { /* body withheld */ }
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a { /* body withheld */ } instance is built. Above this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if { /* body withheld */ } is not rectangular (not all rows have the same length).
     * …
    public static RealMatrix createRealMatrix(double[][] data) { /* body withheld */ }
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a { /* body withheld */ } instance is built. Above
     * this threshold a { /* body withheld */ } instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param <T> the type of the field elements
     * @param data input array
     * @return a matrix containing the values of the array.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if { /* body withheld */ } is not rectangular (not all rows have the same length).
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(T[][] data) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
    public static RealMatrix createRealIdentityMatrix(int dimension) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param <T> the type of the field elements
     * @param field field to which the elements belong
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { /* body withheld */ }
     * Returns a diagonal matrix with specified elements.
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static RealMatrix createRealDiagonalMatrix(final double[] diagonal) { /* body withheld */ }
     * Returns a diagonal matrix with specified elements.
     * @param <T> the type of the field elements
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldDiagonalMatrix(final T[] diagonal) { /* body withheld */ }
     * Creates a { /* body withheld */ } using the data from the input array.
     * @param data the input data
     * @return a data.length RealVector
     * @throws NoDataException if { /* body withheld */ } is empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
    public static RealVector createRealVector(double[] data) { /* body withheld */ }
     * Creates a { /* body withheld */ } using the data from the input array.
     * @param <T> the type of the field elements
     * @param data the input data
     * @return a data.length FieldVector
     * @throws NoDataException if { /* body withheld */ } is empty.
     * @throws NullArgumentException if { /* body withheld */ } is { /* body withheld */ }.
     * @throws ZeroException if { /* body withheld */ } has 0 elements
    public static <T extends FieldElement<T>> FieldVector<T> createFieldVector(final T[] data) { /* body withheld */ }
     * Converter for { /* body withheld */ }/{ /* body withheld */ }.
    private static class FractionMatrixConverter extends DefaultFieldMatrixPreservingVisitor<Fraction> { … }
}
// … (non-patched members truncated; patched methods kept)
</class>}

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `hashCode`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,282 chars)</summary>

```
// compute(java.util.Collection<?>, int, int, int) : getNumObjectiveFunctions, getHeight, getWidth
public class ReferenceImpl {
    public static int compute_getNumObjectiveFunctions(
            java.util.Collection<?> constraints,
            int numArtificialVariables,
            int numDecisionVariables,
            int numSlackVariables) {
        return numArtificialVariables > 0 ? 2 : 1;
    }

    public static int compute_getHeight(
            java.util.Collection<?> constraints,
            int numArtificialVariables,
            int numDecisionVariables,
            int numSlackVariables) {
        return constraints.size() + compute_getNumObjectiveFunctions(
                constraints, numArtificialVariables, numDecisionVariables, numSlackVariables);
    }

    public static int compute_getWidth(
            java.util.Collection<?> constraints,
            int numArtificialVariables,
            int numDecisionVariables,
            int numSlackVariables) {
        return numDecisionVariables
                + numSlackVariables
                + numArtificialVariables
                + compute_getNumObjectiveFunctions(
                        constraints, numArtificialVariables, numDecisionVariables, numSlackVariables)
                + 1;
    }
}
```

</details>

---
## [61] ⚙️ reference-impl · `getHeight`
**output:** **reference generated**
- detail: {'chars': 1282}

---
## [62] ⚙️ reference-impl · `getHeight`
**output:** **signature declared**
- detail: {'signature': 'java.util.Collection<?>, int, int, int'}

---
## [63] ⚙️ reference-impl · `getHeight`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'java.util.Collection<?>, int, int, int', 'named': 'java.util.Collection<?> constraints, int numArtificialVariables, int numDecisionVariables, int numSlackVariables'}

---
## [64] ⚙️ reference-impl · `getHeight`
**output:** **reference observables matched**
- detail: {'matched': {'getHeight': 'getHeight'}, 'declared_only': ['getNumObjectiveFunctions', 'getWidth']}

---
## [65] ⚙️ reference-impl · `getHeight`
**output:** **reference too thin to screen — DISCARDED**
- reason: 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [66] ⚙️ reference-widening · `getHeight`
**output:** **widening REJECTED for observable `height`**
- reason: reference too thin to screen — DISCARDED — 0 shared sibling observable(s) []; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [67] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~34,247 chars)</summary>

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
- Package: `org.apache.commons.math3.optimization.linear` (`package org.apache.commons.math3.optimization.linear;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_33_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
+++ /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
@@ -335,7 +335,7 @@
         // positive cost non-artificial variables
         for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
             final double entry = tableau.getEntry(0, i);
-            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
+            if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                 columnsToDrop.add(i);
             }
         }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;
</source_imports>

Function `dropPhase1Objective`:
<signature>
protected void dropPhase1Objective()
</signature>
<code>
    protected void dropPhase1Objective() {
        if (getNumObjectiveFunctions() == 1) {
            return;
        }

        List<Integer> columnsToDrop = new ArrayList<Integer>();
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
            final double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < getNumArtificialVariables(); i++) {
          int col = i + getArtificialVariableOffset();
          if (getBasicRow(col) == null) {
            columnsToDrop.add(col);
          }
        }

        double[][] matrix = new double[getHeight() - 1][getWidth() - columnsToDrop.size()];
        for (int i = 1; i < getHeight(); i++) {
          int col = 0;
          for (int j = 0; j < getWidth(); j++) {
            if (!columnsToDrop.contains(j)) {
              matrix[i - 1][col++] = tableau.getEntry(i, j);
            }
          }
        }

        for (int i = columnsToDrop.size() - 1; i >= 0; i--) {
          columnLabels.remove((int) columnsToDrop.get(i));
        }

        this.tableau = new Array2DRowRealMatrix(matrix);
        this.numArtificialVariables = 0;
    }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `dropPhase1Objective` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `dropPhase1Objective`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `SimplexTableau(LinearObjectiveFunction f, Collection<LinearConstraint> constraints, GoalType goalType, boolean restrictToNonNegative, double epsilon, int maxUlps)` (shared field(s): maxUlps, numArtificialVariables, tableau)
      doc: Build a tableau for a linear problem. @param f linear objective function @param constraints linear constraints @param goalType type of optimization goal: either {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE} @param restrictToNonNegative whether to restrict the variables to non-negative values @param epsilon amo…
    <constructor_body>
    SimplexTableau(final LinearObjectiveFunction f,
                   final Collection<LinearConstraint> constraints,
                   final GoalType goalType, final boolean restrictToNonNegative,
                   final double epsilon,
                   final int maxUlps) {
        this.f                      = f;
        this.constraints            = normalizeConstraints(constraints);
        this.restrictToNonNegative  = restrictToNonNegative;
        this.epsilon                = epsilon;
        this.maxUlps                = maxUlps;
        this.numDecisionVariables   = f.getCoefficients().getDimension() +
                                      (restrictToNonNegative ? 0 : 1);
        this.numSlackVariables      = getConstraintTypeCounts(Relationship.LEQ) +
                                      getConstraintTypeCounts(Relationship.GEQ);
        this.numArtificialVariables = getConstraintTypeCounts(Relationship.EQ) +
                                      getConstraintTypeCounts(Relationship.GEQ);
        this.tableau = createTableau(goalType == GoalType.MAXIMIZE);
        initializeColumnLabels();
    }
    </constructor_body>
  - `public boolean equals(Object other)` (shared field(s): maxUlps, numArtificialVariables)
      doc: {@inheritDoc}
  - `public int hashCode()` (shared field(s): maxUlps, numArtificialVariables)
      doc: {@inheritDoc}
  - `protected RealMatrix createTableau(boolean maximize)` (shared field(s): numArtificialVariables)
      doc: Create the tableau by itself. @param maximize if true, goal is to maximize the objective function @return created tableau
  - `private void writeObject(ObjectOutputStream oos) throws IOException` (shared field(s): tableau)
      doc: Serialize the instance. @param oos stream where object should be written @throws IOException if object cannot be written to stream

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): hashCode

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `SimplexSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math3.optimization.linear.SimplexSolverTest" method="testMath781">
    @Test
    public void testMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);
        Assert.assertEquals(2.0d, solution.getValue(), epsilon);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math3.optimization.linear.SimplexSolverTest::testMath781
junit.framework.AssertionFailedError
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
<relation name="dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDro" kind="metamorphic">
  // holds because: dropPhase1Objective() "Removes the phase 1 objective function, positive cost non-artificial variables, and the non-basic artificial variables from this tableau." Therefore, changing one non-artificial phase-1 cost entry from 0 to a strictly positive value must cause that column to be removed after the call.
  // valid input: Construct two identical valid phase-1 tableaux through the real SimplexTableau constructor using a linear program that requires artificial variables (e.g. the Math781-style constraints). In both tableaux, force all phase-1-row entries in the non-artificial scanned range to 0, then in exactly one tableau set one scanned non-artificial entry to a tiny strictly positive value drawn from data. This isolates the changed >0 boundary in the patched condition.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 15648/20000 on random inputs
Object tZero, tPos; java.lang.reflect.Method getNumObjectiveFunctions, getArtificialVariableOffset, setEntry, getWidth, getHeight, dropPhase1Objective; try { int a = data.consumeInt(-5, 5); int b = data.consumeInt(-5, 5); int c = data.consumeInt(-5, 5); org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0); java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint> constraints = new java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint>(); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 1, 2, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, 2)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { -1, 1, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 2, -3, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); Class<?> cls = Class.forName("org.apache.commons.math3.optimization.linear.SimplexTableau"); java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor(org.apache.commons.math3.optimization.linear.LinearObjectiveFunction.class, java.util.Collection.class, org.apache.commons.math3.optimization.GoalType.class, boolean.class, double.class); ctor.setAccessible(true); tZero = ctor.newInstance(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, 1.0e-6); tPos = ctor.newInstance(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, 1.0e-6); getNumObjectiveFunctions = cls.getDeclaredMethod("getNumObjectiveFunctions"); getArtificialVariableOffset = cls.getDeclaredMethod("getArtificialVariableOffset"); setEntry = cls.getDeclaredMethod("setEntry", int.class, int.class, double.class); getWidth = cls.getDeclaredMethod("getWidth"); getHeight = cls.getDeclaredMethod("getHeight"); dropPhase1Objective = cls.getDeclaredMethod("dropPhase1Objective"); getNumObjectiveFunctions.setAccessible(true); getArtificialVariableOffset.setAccessible(true); setEntry.setAccessible(true); getWidth.setAccessible(true); getHeight.setAccessible(true); dropPhase1Objective.setAccessible(true); int start = ((Integer) getNumObjectiveFunctions.invoke(tZero)).intValue(); int end = ((Integer) getArtificialVariableOffset.invoke(tZero)).intValue(); if (end <= start) { return; } for (int j = start; j < end; j++) { setEntry.invoke(tZero, 0, j, 0.0); setEntry.invoke(tPos, 0, j, 0.0); } int chosen = start + data.consumeInt(0, end - start - 1); double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4)); setEntry.invoke(tPos, 0, chosen, tinyPositive); } catch (Exception e) { return; } int widthZeroBefore; int widthPosBefore; int heightZeroBefore; int heightPosBefore; try { widthZeroBefore = ((Integer) getWidth.invoke(tZero)).intValue(); widthPosBefore = ((Integer) getWidth.invoke(tPos)).intValue(); heightZeroBefore = ((Integer) getHeight.invoke(tZero)).intValue(); heightPosBefore = ((Integer) getHeight.invoke(tPos)).intValue(); dropPhase1Objective.invoke(tZero); dropPhase1Objective.invoke(tPos); } catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("SimplexTableau") || __rpgc.endsWith(".SimplexTableau") || __rpgc.endsWith("$SimplexTableau")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("SimplexTableau") || __rpgf.getClassName().endsWith(".SimplexTableau") || __rpgf.getClassName().endsWith("$SimplexTableau")) && "readObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ClassNotFoundException") || __rpn.endsWith(".ClassNotFoundException") || __rpn.endsWith("$ClassNotFoundException") || __rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("SimplexTableau") || __rpgf.getClassName().endsWith(".SimplexTableau") || __rpgf.getClassName().endsWith("$SimplexTableau")) && "writeObject".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IOException") || __rpn.endsWith(".IOException") || __rpn.endsWith("$IOException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } } throw new RuntimeException("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); } int widthZeroAfter; int widthPosAfter; int heightZeroAfter; int heightPosAfter; try { widthZeroAfter = ((Integer) getWidth.invoke(tZero)).intValue(); widthPosAfter = ((Integer) getWidth.invoke(tPos)).intValue(); heightZeroAfter = ((Integer) getHeight.invoke(tZero)).intValue(); heightPosAfter = ((Integer) getHeight.invoke(tPos)).intValue(); } catch (Exception e) { throw new RuntimeException("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: could not read tableau dimensions after probe: " + e.getMessage()); } if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) throw new RuntimeException("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: baseline tableaux not identical before probe"); if (heightZeroAfter != heightPosAfter) throw new RuntimeException("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: changing one scanned coefficient should not change number of removed rows"); if (widthPosAfter != widthZeroAfter - 1) throw new RuntimeException("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: tiny positive scanned non-artificial column was not additionally dropped: zeroCaseWidth=" + widthZeroAfter + ", posCaseWidth=" + widthPosAfter);
</relation>
<relation name="simplexSolver_math781RegressionValueAndSigns" kind="invariant">
  // holds because: For the concrete valid linear program used in the trusted regression test, every correct implementation must reproduce that program's optimum: solution value 2.0, with x0 > 0, x1 > 0, and x2 < 0 within the documented solver epsilon.
  // valid input: Build exactly the Math781 linear objective and constraints through the public API and solve with SimplexSolver.optimize using GoalType.MAXIMIZE and restrictToNonNegative=false.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 20000/20000 on random inputs
org.apache.commons.math3.optimization.linear.SimplexSolver solver; org.apache.commons.math3.optimization.linear.LinearObjectiveFunction f; java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint> constraints; org.apache.commons.math3.optimization.PointValuePair solution; try { f = new org.apache.commons.math3.optimization.linear.LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0); constraints = new java.util.ArrayList<org.apache.commons.math3.optimization.linear.LinearConstraint>(); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 1, 2, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, 2)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { -1, 1, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); constraints.add(new org.apache.commons.math3.optimization.linear.LinearConstraint(new double[] { 2, -3, 1 }, org.apache.commons.math3.optimization.linear.Relationship.LEQ, -1)); solver = new org.apache.commons.math3.optimization.linear.SimplexSolver(); solution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false); } catch (Exception e) { return; } double[] p = solution.getPoint(); double eps = 1.0e-6; if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, eps) > 0)) throw new RuntimeException("relation simplexSolver_math781RegressionValueAndSigns violated: x0=" + p[0]); if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, eps) > 0)) throw new RuntimeException("relation simplexSolver_math781RegressionValueAndSigns violated: x1=" + p[1]); if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, eps) < 0)) throw new RuntimeException("relation simplexSolver_math781RegressionValueAndSigns violated: x2=" + p[2]); if (!(Math.abs(solution.getValue() - 2.0d) <= eps * Math.max(1.0, Math.max(Math.abs(solution.getValue()), 2.0d)))) throw new RuntimeException("relation simplexSolver_math781RegressionValueAndSigns violated: value=" + solution.getValue());
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
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~11,780 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;
        final int maxUlps = 10;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue() == 2.0 within 1.0E-6 but actual value=" + solution.getValue());
            }
        } catch (Throwable t) {
            return;
        }

        SimplexTableau tProbe;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);
            LinearObjectiveFunction fProbe = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraintsProbe = new ArrayList<LinearConstraint>();
            constraintsProbe.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraintsProbe.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraintsProbe.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            tProbe = new SimplexTableau(fProbe, constraintsProbe, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return;
        }

        int probeHeightBefore;
        int probeWidthBefore;
        int probeArtsBefore;
        int probeHashA;
        int probeHashB;
        try {
            probeHashA = tProbe.hashCode();
            probeHashB = tProbe.hashCode();
            probeHeightBefore = tProbe.getHeight();
            probeWidthBefore = tProbe.getWidth();
            probeArtsBefore = tProbe.getNumArtificialVariables();
            if (tProbe.getNumObjectiveFunctions() <= 1 || probeArtsBefore <= 0) {
                return;
            }
            tProbe.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        int probeHeightAfter;
        int probeWidthAfter;
        int probeArtsAfter;
        int probeHashAfterA;
        int probeHashAfterB;
        try {
            probeHeightAfter = tProbe.getHeight();
            probeWidthAfter = tProbe.getWidth();
            probeArtsAfter = tProbe.getNumArtificialVariables();
            probeHashAfterA = tProbe.hashCode();
            probeHashAfterB = tProbe.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (probeHashA != probeHashB) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-before] metamorphic violation: repeated hashCode() on the same tableau before mutation disagreed lhs=" + probeHashA + " rhs=" + probeHashB);
        }
        if (probeHeightAfter != probeHeightBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-height] metamorphic violation: dropPhase1Objective must remove the phase-1 objective row, so height decreases by exactly one inputHeight=" + probeHeightBefore + " actualHeight=" + probeHeightAfter);
        }
        if (!(probeWidthAfter <= probeWidthBefore - 1)) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-width] metamorphic violation: dropPhase1Objective always drops at least the phase-1 objective column inputWidth=" + probeWidthBefore + " actualWidth=" + probeWidthAfter);
        }
        if (probeArtsAfter != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-artificial-count] metamorphic violation: dropPhase1Objective sets numArtificialVariables to 0 before=" + probeArtsBefore + " after=" + probeArtsAfter);
        }
        if (probeHashAfterA != probeHashAfterB) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-after] metamorphic violation: repeated hashCode() on the same tableau after mutation disagreed lhs=" + probeHashAfterA + " rhs=" + probeHashAfterB);
        }

        SimplexTableau eq1;
        SimplexTableau eq2;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);
            LinearObjectiveFunction fEq = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraintsEq = new ArrayList<LinearConstraint>();
            constraintsEq.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraintsEq.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraintsEq.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            eq1 = new SimplexTableau(fEq, constraintsEq, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            eq2 = new SimplexTableau(fEq, constraintsEq, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return;
        }

        boolean eqBefore;
        int hashEq1Before;
        int hashEq2Before;
        try {
            eqBefore = eq1.equals(eq2);
            hashEq1Before = eq1.hashCode();
            hashEq2Before = eq2.hashCode();
            if (eq1.getNumObjectiveFunctions() <= 1) {
                return;
            }
            eq1.dropPhase1Objective();
            eq2.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        boolean eqAfter;
        int hashEq1After;
        int hashEq2After;
        try {
            eqAfter = eq1.equals(eq2);
            hashEq1After = eq1.hashCode();
            hashEq2After = eq2.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (!eqBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-constructor] metamorphic violation: two tableaux built from identical constructor arguments must be equal");
        }
        if (hashEq1Before != hashEq2Before) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-constructor] metamorphic violation: equal tableaux from identical constructor arguments must have equal hashCode lhs=" + hashEq1Before + " rhs=" + hashEq2Before);
        }
        if (!eqAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-after-drop] metamorphic violation: applying dropPhase1Objective to two equal tableaux should preserve equality");
        }
        if (hashEq1After != hashEq2After) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-after-drop] metamorphic violation: equal tableaux after identical dropPhase1Objective calls must have equal hashCode lhs=" + hashEq1After + " rhs=" + hashEq2After);
        }

        SimplexTableau tZero;
        SimplexTableau tPos;
        int widthZeroBefore;
        int widthPosBefore;
        int heightZeroBefore;
        int heightPosBefore;
        int widthZeroAfter;
        int widthPosAfter;
        int heightZeroAfter;
        int heightPosAfter;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);
            LinearObjectiveFunction fMeta = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraintsMeta = new ArrayList<LinearConstraint>();
            constraintsMeta.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraintsMeta.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraintsMeta.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            tZero = new SimplexTableau(fMeta, constraintsMeta, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tPos = new SimplexTableau(fMeta, constraintsMeta, GoalType.MAXIMIZE, false, epsilon, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }
            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.setEntry(0, chosen, tinyPositive);

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
        } catch (Throwable t) {
            return;
        }

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-baseline] metamorphic violation: paired tableaux must start with identical dimensions zeroWidth=" + widthZeroBefore + " posWidth=" + widthPosBefore + " zeroHeight=" + heightZeroBefore + " posHeight=" + heightPosBefore);
        }
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-height] metamorphic violation: changing one scanned coefficient should not change number of removed rows zeroHeight=" + heightZeroAfter + " posHeight=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-drop] metamorphic violation: making one scanned non-artificial phase-1 cost entry strictly positive should cause one additional dropped column zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter);
        }
    }
}
```

</details>

---
## [68] ⚙️ harness-repair · `attempt_001`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [69] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [70] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~63,321 chars, 1 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math3.optimization.linear` (`package org.apache.commons.math3.optimization.linear;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_33_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
+++ /src/main/java/org/apache/commons/math3/optimization/linear/SimplexTableau.java	
@@ -335,7 +335,7 @@
         // positive cost non-artificial variables
         for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
             final double entry = tableau.getEntry(0, i);
-            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
+            if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                 columnsToDrop.add(i);
             }
         }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;
</source_imports>

Function `dropPhase1Objective`:
<signature>
protected void dropPhase1Objective()
</signature>
<code>
    protected void dropPhase1Objective() {
        if (getNumObjectiveFunctions() == 1) {
            return;
        }

        List<Integer> columnsToDrop = new ArrayList<Integer>();
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
            final double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < getNumArtificialVariables(); i++) {
          int col = i + getArtificialVariableOffset();
          if (getBasicRow(col) == null) {
            columnsToDrop.add(col);
          }
        }

        double[][] matrix = new double[getHeight() - 1][getWidth() - columnsToDrop.size()];
        for (int i = 1; i < getHeight(); i++) {
          int col = 0;
          for (int j = 0; j < getWidth(); j++) {
            if (!columnsToDrop.contains(j)) {
              matrix[i - 1][col++] = tableau.getEntry(i, j);
            }
          }
        }

        for (int i = columnsToDrop.size() - 1; i >= 0; i--) {
          columnLabels.remove((int) columnsToDrop.get(i));
        }

        this.tableau = new Array2DRowRealMatrix(matrix);
        this.numArtificialVariables = 0;
    }
</code>
STATE COUPLING: these members of the same class share the listed field(s) with `dropPhase1Objective` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `dropPhase1Objective`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `SimplexTableau(LinearObjectiveFunction f, Collection<LinearConstraint> constraints, GoalType goalType, boolean restrictToNonNegative, double epsilon, int maxUlps)` (shared field(s): maxUlps, numArtificialVariables, tableau)
      doc: Build a tableau for a linear problem. @param f linear objective function @param constraints linear constraints @param goalType type of optimization goal: either {@link GoalType#MAXIMIZE} or {@link GoalType#MINIMIZE} @param restrictToNonNegative whether to restrict the variables to non-negative values @param epsilon amo…
    <constructor_body>
    SimplexTableau(final LinearObjectiveFunction f,
                   final Collection<LinearConstraint> constraints,
                   final GoalType goalType, final boolean restrictToNonNegative,
                   final double epsilon,
                   final int maxUlps) {
        this.f                      = f;
        this.constraints            = normalizeConstraints(constraints);
        this.restrictToNonNegative  = restrictToNonNegative;
        this.epsilon                = epsilon;
        this.maxUlps                = maxUlps;
        this.numDecisionVariables   = f.getCoefficients().getDimension() +
                                      (restrictToNonNegative ? 0 : 1);
        this.numSlackVariables      = getConstraintTypeCounts(Relationship.LEQ) +
                                      getConstraintTypeCounts(Relationship.GEQ);
        this.numArtificialVariables = getConstraintTypeCounts(Relationship.EQ) +
                                      getConstraintTypeCounts(Relationship.GEQ);
        this.tableau = createTableau(goalType == GoalType.MAXIMIZE);
        initializeColumnLabels();
    }
    </constructor_body>
  - `public boolean equals(Object other)` (shared field(s): maxUlps, numArtificialVariables)
      doc: {@inheritDoc}
  - `public int hashCode()` (shared field(s): maxUlps, numArtificialVariables)
      doc: {@inheritDoc}
  - `protected RealMatrix createTableau(boolean maximize)` (shared field(s): numArtificialVariables)
      doc: Create the tableau by itself. @param maximize if true, goal is to maximize the objective function @return created tableau
  - `private void writeObject(ObjectOutputStream oos) throws IOException` (shared field(s): tableau)
      doc: Serialize the instance. @param oos stream where object should be written @throws IOException if object cannot be written to stream

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="SimplexTableau" role="patched">
     * A tableau for use in the Simplex method.
     * <p>
     * Example:
     * <pre>
     * W |  Z |  x1 |  x2 |  x- | s1 |  s2 |  a1 |  RHS
     * ---------------------------------------------------
     * -1    0    0     0     0     0     0     1     0   &lt;= phase 1 objective
     * 0    1   -15   -10    0     0     0     0     0   &lt;= phase 2 objective
     * 0    0    1     0     0     1     0     0     2   &lt;= constraint 1
     * 0    0    0     1     0     0     1     0     3   &lt;= constraint 2
     * 0    0    1     1     0     0     0     1     4   &lt;= constraint 3
     * </pre>
     * …
class SimplexTableau implements Serializable {
     * Column label for negative vars.
    private static final String NEGATIVE_VAR_COLUMN_LABEL = "x-";
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Serializable version identifier.
    private static final long serialVersionUID = -1369660067587938365L;
     * Linear objective function.
    private final LinearObjectiveFunction f;
     * Linear constraints.
    private final List<LinearConstraint> constraints;
     * Whether to restrict the variables to non-negative values.
    private final boolean restrictToNonNegative;
     * The variables each column represents
    private final List<String> columnLabels = new ArrayList<String>();
     * Simple tableau.
    private transient RealMatrix tableau;
     * Number of decision variables.
    private final int numDecisionVariables;
     * Number of slack variables.
    private final int numSlackVariables;
     * Number of artificial variables.
    private int numArtificialVariables;
     * Amount of error to accept when checking for optimality.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons.
    private final int maxUlps;
     * Build a tableau for a linear problem.
     * @param f linear objective function
     * @param constraints linear constraints
     * @param goalType type of optimization goal: either {@link GoalType#MAXIMIZE}
     * or {@link GoalType#MINIMIZE}
     * @param restrictToNonNegative whether to restrict the variables to non-negative values
     * @param epsilon amount of error to accept when checking for optimality
     * /
     * SimplexTableau(final LinearObjectiveFunction f,
     * final Collection<LinearConstraint> constraints,
     * final GoalType goalType, final boolean restrictToNonNegative,
     * final double epsilon) {
     * …
    protected void initializeColumnLabels() { … }
     * Create the tableau by itself.
     * @param maximize if true, goal is to maximize the objective function
     * @return created tableau
    protected RealMatrix createTableau(final boolean maximize) { … }
     * Get new versions of the constraints which have positive right hand sides.
     * @param originalConstraints original (not normalized) constraints
     * @return new versions of the constraints
    public List<LinearConstraint> normalizeConstraints(Collection<LinearConstraint> originalConstraints) { … }
     * Get a new equation equivalent to this one with a positive right hand side.
     * @param constraint reference constraint
     * @return new equation
    private LinearConstraint normalize(final LinearConstraint constraint) { … }
     * Get the number of objective functions in this tableau.
     * @return 2 for Phase 1.  1 for Phase 2.
    protected final int getNumObjectiveFunctions() { … }
     * Get a count of constraints corresponding to a specified relationship.
     * @param relationship relationship to count
     * @return number of constraint with the specified relationship
    private int getConstraintTypeCounts(final Relationship relationship) { … }
     * Get the -1 times the sum of all coefficients in the given array.
     * @param coefficients coefficients to sum
     * @return the -1 times the sum of all coefficients in the given array.
    protected static double getInvertedCoefficientSum(final RealVector coefficients) { … }
     * Checks whether the given column is basic.
     * @param col index of the column to check
     * @return the row that the variable is basic in.  null if the column is not basic
    protected Integer getBasicRow(final int col) { … }
     * Removes the phase 1 objective function, positive cost non-artificial variables,
     * and the non-basic artificial variables from this tableau.
    protected void dropPhase1Objective() {
        if (getNumObjectiveFunctions() == 1) {
            return;
        }

        List<Integer> columnsToDrop = new ArrayList<Integer>();
        columnsToDrop.add(0);

        // positive cost non-artificial variables
        for (int i = getNumObjectiveFunctions(); i < getArtificialVariableOffset(); i++) {
            final double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0d, maxUlps) > 0) {
                columnsToDrop.add(i);
            }
        }

        // non-basic artificial variables
        for (int i = 0; i < getNumArtificialVariables(); i++) {
          int col = i + getArtificialVariableOffset();
          if (getBasicRow(col) == null) {
            columnsToDrop.add(col);
          }
        }

        double[][] matrix = new double[getHeight() - 1][getWidth() - columnsToDrop.size()];
        for (int i = 1; i < getHeight(); i++) {
          int col = 0;
          for (int j = 0; j < getWidth(); j++) {
            if (!columnsToDrop.contains(j)) {
              matrix[i - 1][col++] = tableau.getEntry(i, j);
            }
          }
        }

        for (int i = columnsToDrop.size() - 1; i >= 0; i--) {
          columnLabels.remove((int) columnsToDrop.get(i));
        }

        this.tableau = new Array2DRowRealMatrix(matrix);
        this.numArtificialVariables = 0;
    }
     * @param src the source array
     * @param dest the destination array
    private void copyArray(final double[] src, final double[] dest) { … }
     * Returns whether the problem is at an optimal state.
     * @return whether the model has been solved
     * /
     * boolean isOptimal() {
     * for (int i = getNumObjectiveFunctions(); i < getWidth() - 1; i++) {
     * final double entry = tableau.getEntry(0, i);
     * if (Precision.compareTo(entry, 0d, epsilon) < 0) {
     * return false;
     * }
     * }
     * return true;
     * }
     * …
    protected PointValuePair getSolution() { … }
     * Subtracts a multiple of one row from another.
     * <p>
     * After application of this operation, the following will hold:
     * minuendRow = minuendRow - multiple * subtrahendRow
     * </p>
     * @param dividendRow index of the row
     * @param divisor value of the divisor
    protected void divideRow(final int dividendRow, final double divisor) { … }
     * Subtracts a multiple of one row from another.
     * <p>
     * After application of this operation, the following will hold:
     * minuendRow = minuendRow - multiple * subtrahendRow
     * </p>
     * @param minuendRow row index
     * @param subtrahendRow row index
     * @param multiple multiplication factor
    protected void subtractRow(final int minuendRow, final int subtrahendRow, final double multiple) { … }
     * Get the width of the tableau.
     * @return width of the tableau
    protected final int getWidth() { … }
     * Get the height of the tableau.
     * @return height of the tableau
    protected final int getHeight() { … }
     * Get an entry of the tableau.
     * @param row row index
     * @param column column index
     * @return entry at (row, column)
    protected final double getEntry(final int row, final int column) { … }
     * Set an entry of the tableau.
     * @param row row index
     * @param column column index
     * @param value for the entry
    protected final void setEntry(final int row, final int column, final double value) { … }
     * Get the offset of the first slack variable.
     * @return offset of the first slack variable
    protected final int getSlackVariableOffset() { … }
     * Get the offset of the first artificial variable.
     * @return offset of the first artificial variable
    protected final int getArtificialVariableOffset() { … }
     * Get the offset of the right hand side.
     * @return offset of the right hand side
    protected final int getRhsOffset() { … }
     * Get the number of decision variables.
     * <p>
     * If variables are not restricted to positive values, this will include 1
     * extra decision variable to represent the absolute value of the most
     * negative variable.
     * </p>
     * @return number of decision variables
     * @see #getOriginalNumDecisionVariables()
    protected final int getNumDecisionVariables() { … }
     * Get the original number of decision variables.
     * @return original number of decision variables
     * @see #getNumDecisionVariables()
    protected final int getOriginalNumDecisionVariables() { … }
     * Get the number of slack variables.
     * @return number of slack variables
    protected final int getNumSlackVariables() { … }
     * Get the number of artificial variables.
     * @return number of artificial variables
    protected final int getNumArtificialVariables() { … }
     * Get the tableau data.
     * @return tableau data
    protected final double[][] getData() { … }
     * {@inheritDoc}
    @Override public boolean equals(Object other) { … }
     * {@inheritDoc}
    @Override public int hashCode() { … }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { … }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { … }
}
</class>

<class name="LinearConstraint" role="test-subject">
     * A linear constraint for a linear optimization problem.
     * <p>
     * A linear constraint has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * …
public class LinearConstraint implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -764632794033034092L;
     * Coefficients of the constraint (left hand side).
    private final transient RealVector coefficients;
     * Relationship between left and right hand sides (=, &lt;=, >=).
    private final Relationship relationship;
     * Value of the constraint (right hand side).
    private final double value;
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final double[] coefficients, final Relationship relationship, final double value) {
        this(new ArrayRealVector(coefficients), relationship, value);
    }
     * Build a constraint involving a single linear equation.
     * <p>
     * A linear constraint with a single linear equation has one of the forms:
     * <ul>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> = v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> &lt;= v</li>
     * <li>c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> >= v</li>
     * </ul>
     * </p>
     * @param coefficients The coefficients of the constraint (left hand side)
     * @param relationship The type of (in)equality used in the constraint
     * @param value The value of the constraint (right hand side)
    public LinearConstraint(final RealVector coefficients, final Relationship relationship, final double value) {
        this.coefficients = coefficients;
        this.relationship = relationship;
        this.value        = value;
    }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final double[] lhsCoefficients, final double lhsConstant, final Relationship relationship, final double[] rhsCoefficients, final double rhsConstant) {
        double[] sub = new double[lhsCoefficients.length];
        for (int i = 0; i < sub.length; ++i) {
            sub[i] = lhsCoefficients[i] - rhsCoefficients[i];
        }
        this.coefficients = new ArrayRealVector(sub, false);
        this.relationship = relationship;
        this.value        = rhsConstant - lhsConstant;
    }
     * Build a constraint involving two linear equations.
     * <p>
     * A linear constraint with two linear equation has one of the forms:
     * <ul>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> =
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> &lt;=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * <li>l<sub>1</sub>x<sub>1</sub> + ... l<sub>n</sub>x<sub>n</sub> + l<sub>cst</sub> >=
     * r<sub>1</sub>x<sub>1</sub> + ... r<sub>n</sub>x<sub>n</sub> + r<sub>cst</sub></li>
     * </ul>
     * </p>
     * …
    public LinearConstraint(final RealVector lhsCoefficients, final double lhsConstant, final Relationship relationship, final RealVector rhsCoefficients, final double rhsConstant) {
        this.coefficients = lhsCoefficients.subtract(rhsCoefficients);
        this.relationship = relationship;
        this.value        = rhsConstant - lhsConstant;
    }
     * Get the coefficients of the constraint (left hand side).
     * @return coefficients of the constraint (left hand side)
    public RealVector getCoefficients() { … }
     * Get the relationship between left and right hand sides.
     * @return relationship between left and right hand sides
    public Relationship getRelationship() { … }
     * Get the value of the constraint (right hand side).
     * @return value of the constraint (right hand side)
    public double getValue() { … }
     * {@inheritDoc}
    @Override public boolean equals(Object other) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="LinearObjectiveFunction" role="test-subject">
     * An objective function for a linear optimization problem.
     * <p>
     * A linear objective function has one the form:
     * <pre>
     * c<sub>1</sub>x<sub>1</sub> + ... c<sub>n</sub>x<sub>n</sub> + d
     * </pre>
     * The c<sub>i</sub> and d are the coefficients of the equation,
     * the x<sub>i</sub> are the coordinates of the current point.
     * </p>
     * @version $Id$
     * @since 2.0
public class LinearObjectiveFunction implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -4531815507568396090L;
     * Coefficients of the constraint (c<sub>i</sub>).
    private final transient RealVector coefficients;
     * Constant term of the linear equation.
    private final double constantTerm;
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(double[] coefficients, double constantTerm) {
        this(new ArrayRealVector(coefficients), constantTerm);
    }
     * @param coefficients The coefficients for the linear equation being optimized
     * @param constantTerm The constant term of the linear equation
    public LinearObjectiveFunction(RealVector coefficients, double constantTerm) {
        this.coefficients = coefficients;
        this.constantTerm = constantTerm;
    }
     * Get the coefficients of the linear equation being optimized.
     * @return coefficients of the linear equation being optimized
    public RealVector getCoefficients() { … }
     * Get the constant of the linear equation being optimized.
     * @return constant of the linear equation being optimized
    public double getConstantTerm() { … }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final double[] point) { … }
     * Compute the value of the linear equation at the current point
     * @param point point at which linear equation must be evaluated
     * @return value of the linear equation at the current point
    public double getValue(final RealVector point) { … }
     * {@inheritDoc}
    @Override public boolean equals(Object other) { … }
     * {@inheritDoc}
    @Override public int hashCode() { … }
     * Serialize the instance.
     * @param oos stream where object should be written
     * @throws IOException if object cannot be written to stream
    private void writeObject(ObjectOutputStream oos) throws IOException { … }
     * Deserialize the instance.
     * @param ois stream from which the object should be read
     * @throws ClassNotFoundException if a class in the stream cannot be found
     * @throws IOException if object cannot be read from the stream
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { … }
}
</class>

<class name="SimplexSolver" role="test-subject">
     * Solves a linear problem using the Two-Phase Simplex Method.
     * @version $Id$
     * @since 2.0
public class SimplexSolver extends AbstractLinearOptimizer {
     * Default amount of error to accept for algorithm convergence.
    private static final double DEFAULT_EPSILON = 1.0e-6;
     * Default amount of error to accept in floating point comparisons (as ulps).
    private static final int DEFAULT_ULPS = 10;
     * Amount of error to accept for algorithm convergence.
    private final double epsilon;
     * Amount of error to accept in floating point comparisons (as ulps).
    private final int maxUlps;
     * Build a simplex solver with default settings.
    public SimplexSolver() {
        this(DEFAULT_EPSILON, DEFAULT_ULPS);
    }
     * Build a simplex solver with a specified accepted amount of error
     * @param epsilon the amount of error to accept for algorithm convergence
     * @param maxUlps amount of error to accept in floating point comparisons
    public SimplexSolver(final double epsilon, final int maxUlps) {
        this.epsilon = epsilon;
        this.maxUlps = maxUlps;
    }
     * Returns the column with the most negative coefficient in the objective function row.
     * @param tableau simple tableau for the problem
     * @return column with the most negative coefficient
    private Integer getPivotColumn(SimplexTableau tableau) { … }
     * Returns the row with the minimum ratio as given by the minimum ratio test (MRT).
     * @param tableau simple tableau for the problem
     * @param col the column to test the ratio of.  See {@link #getPivotColumn(SimplexTableau)}
     * @return row with the minimum ratio
    private Integer getPivotRow(SimplexTableau tableau, final int col) { … }
     * Runs one iteration of the Simplex method on the given model.
     * @param tableau simple tableau for the problem
     * @throws MaxCountExceededException if the maximal iteration count has been exceeded
     * @throws UnboundedSolutionException if the model is found not to have a bounded solution
    protected void doIteration(final SimplexTableau tableau) throws MaxCountExceededException, UnboundedSolutionException { … }
     * {@inheritDoc}
    @Override public PointValuePair doOptimize() throws MaxCountExceededException, UnboundedSolutionException, NoFeasibleSolutionException { … }
}
</class>

<class name="Relationship" role="collaborator">
     * Types of relationships between two cells in a Solver {@link LinearConstraint}.
     * @version $Id$
     * @since 2.0
public enum Relationship {
     * Equality relationship. */
     * EQ("="),
     * /** Lesser than or equal relationship. */
     * LEQ("<="),
     * /** Greater than or equal relationship. */
     * GEQ(">=");
     * /** Display string for the relationship.
    private final String stringValue;
     * Simple constructor.
     * @param stringValue display string for the relationship
    private Relationship(String stringValue) {
        this.stringValue = stringValue;
    }
     * {@inheritDoc}
    @Override public String toString() { … }
     * Get the relationship obtained when multiplying all coefficients by -1.
     * @return relationship obtained when multiplying all coefficients by -1
    public Relationship oppositeRelationship() { … }
}
</class>

<class name="GoalType" role="collaborator">
     * Goal type for an optimization problem.
     * @version $Id$
     * @since 2.0
public enum GoalType implements Serializable {

}
</class>

<class name="Precision" role="collaborator">
     * Utilities for comparing numbers.
     * @since 3.0
     * @version $Id$
public class Precision {
     * Smallest positive number such that {@code 1 - EPSILON} is not
     * numerically equal to 1.
     * <br/>
     * In IEEE 754 arithmetic, this is 2<sup>-53</sup>.
    public static final double EPSILON;
     * Safe minimum, such that {@code 1 / SAFE_MIN} does not overflow.
     * <br/>
     * In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.
    public static final double SAFE_MIN;
     * Exponent offset in IEEE754 representation.
    private static final long EXPONENT_OFFSET = 1023l;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * Offset to order signed double numbers lexicographically.
    private static final int SGN_MASK_FLOAT = 0x80000000;
     * Private constructor.
    private Precision() {}
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  {@link #equals(double, double, double) equals(x, y, eps)}</li>
     * <li>&lt; 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { … }
     * Compares two numbers given some amount of allowed error.
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)}
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * @return <ul><li>0 if  {@link #equals(double, double, int) equals(x, y, maxUlps)}</li>
     * …
    public static int compareTo(final double x, final double y, final int maxUlps) { … }
     * Returns true iff they are equal as defined by
     * {@link #equals(float,float,int) equals(x, y, 1)}.
     * @param x first value
     * @param y second value
     * @return {@code true} if the values are equal.
    public static boolean equals(float x, float y) { … }
     * Returns true if both arguments are NaN or neither is NaN and they are
     * equal as defined by {@link #equals(float,float) equals(x, y, 1)}.
     * @param x first value
     * @param y second value
     * @return {@code true} if the values are equal or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y) { … }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other.
     * @since 2.2
    public static boolean equals(float x, float y, float eps) { … }
     * Returns true if both arguments are NaN or are equal or within the range
     * of allowed error (inclusive).
     * @param x first value
     * @param y second value
     * @param eps the amount of absolute error to allow.
     * @return {@code true} if the values are equal or within range of each other,
     * or both are NaN.
     * @since 2.2
    public static boolean equalsIncludingNaN(float x, float y, float eps) { … }
     * Returns true if both arguments are equal or within the range of allowed
     * error (inclusive).
     * Two float numbers are considered equal if there are {@code (maxUlps - 1)}
     * (or fewer) floating point numbers between them, i.e. two adjacent floating
     * point numbers are considered equal.
     * Adapted from <a
     * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
     * Bruce Dawson</a>
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * …
    public static boolean equals(float x, float y, int maxUlps) { … }
     * Returns true if both arguments are NaN or if they are equal as defined
     * by {@link #equals(double,double,int) equals(x, y, maxUlps)}.
     * @param x first value
     * @param y second value
     * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
     * values between {@code x} and {@code y}.
     * @return {@code true} if both arguments are NaN or if there are less than
     * {@code maxUlps} floating point values between {@code x} and {@code y}.
     * @since 2.2
    public static boolean equalsIncludingNaN(double x, double y, int maxUlps) { … }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
    public static double round(double x, int scale) { … }
     * Rounds the given value to the specified number of decimal places.
     * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
     * @param x Value to round.
     * @param scale Number of digits to the right of the decimal point.
     * @return the rounded value.
     * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
    public static float round(float x, int scale) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Array2DRowRealMatrix" role="collaborator">
     * Implementation of {@link RealMatrix} using a {@code double[][]} array to
     * store entries.
     * @version $Id$
public class Array2DRowRealMatrix extends AbstractRealMatrix implements Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1067294169172445528L;
     * Entries of the matrix.
    private double data[][];
     * Creates a matrix with no data
    public Array2DRowRealMatrix() {}
     * Create a new RealMatrix with the supplied row and column dimensions.
     * @param rowDimension Number of rows in the new matrix.
     * @param columnDimension Number of columns in the new matrix.
     * @throws org.apache.commons.math3.exception.NotStrictlyPositiveException
     * if the row or column dimension is not positive.
    public Array2DRowRealMatrix(final int rowDimension, final int columnDimension) {
        super(rowDimension, columnDimension);
        data = new double[rowDimension][columnDimension];
    }
     * Create a new {@code RealMatrix} using the input array as the underlying
     * data array.
     * <p>The input array is copied, not referenced. This constructor has
     * the same effect as calling {@link #Array2DRowRealMatrix(double[][], boolean)}
     * with the second argument set to {@code true}.</p>
     * @param d Data for the new matrix.
     * @throws DimensionMismatchException if {@code d} is not rectangular.
     * @throws NoDataException if {@code d} row or colum dimension is zero.
     * @throws NullArgumentException if {@code d} is {@code null}.
     * @see #Array2DRowRealMatrix(double[][], boolean)
    public Array2DRowRealMatrix(final double[][] d) throws DimensionMismatchException, NoDataException, NullArgumentException {
        copyIn(d);
    }
     * Create a new RealMatrix using the input array as the underlying
     * data array.
     * If an array is built specially in order to be embedded in a
     * RealMatrix and not used directly, the {@code copyArray} may be
     * set to {@code false}. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.
     * @param d Data for new matrix.
     * @param copyArray if {@code true}, the input array will be copied,
     * otherwise it will be referenced.
     * @throws DimensionMismatchException if {@code d} is not rectangular
     * (not all rows have the same length) or empty.
     * @throws NullArgumentException if {@code d} is {@code null}.
     * …
    public Array2DRowRealMatrix(final double[][] d, final boolean copyArray) {
        if (copyArray) {
            copyIn(d);
        } else {
            if (d == null) {
                throw new NullArgumentException();
            }
            final int nRows = d.length;
            if (nRows == 0) {
                throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_ROW);
            }
            final int nCols = d[0].length;
            if (nCols == 0) {
                throw new NoDataException(LocalizedFormats.AT_LEAST_ONE_COLUMN);
            }
            for (int r = 1; r < nRows; r++) {
                if (d[r].length != nCols) {
                    throw new DimensionMismatchException(d[r].length, nCols);
                }
            }
            data = d;
        }
    }
     * Create a new (column) RealMatrix using {@code v} as the
     * data for the unique column of the created matrix.
     * The input array is copied.
     * @param v Column vector holding data for new matrix.
    public Array2DRowRealMatrix(final double[] v) {
        final int nRows = v.length;
        data = new double[nRows][1];
        for (int row = 0; row < nRows; row++) {
            data[row][0] = v[row];
        }
    }
     * {@inheritDoc}
    @Override public RealMatrix createMatrix(final int rowDimension, final int columnDimension) { … }
     * {@inheritDoc}
    @Override public RealMatrix copy() { … }
     * Compute the sum of this matrix with {@code m}.
     * @param m Matrix to be added.
     * @return {@code this} + m.
     * @throws MatrixDimensionMismatchException
     * if {@code m} is not the same size as this matrix.
    public Array2DRowRealMatrix add(final Array2DRowRealMatrix m) { … }
     * Subtract {@code m} from this matrix.
     * @param m Matrix to be subtracted.
     * @return {@code this} - m.
     * @throws MatrixDimensionMismatchException
     * if {@code m} is not the same size as this matrix.
    public Array2DRowRealMatrix subtract(final Array2DRowRealMatrix m) { … }
     * Postmultiplying this matrix by {@code m}.
     * @param m Matrix to postmultiply by.
     * @return {@code this} * m.
     * @throws DimensionMismatchException if the number of columns of this
     * matrix is not equal to the number of rows of {@code m}.
    public Array2DRowRealMatrix multiply(final Array2DRowRealMatrix m) { … }
     * {@inheritDoc}
    @Override public double[][] getData() { … }
     * Get a reference to the underlying data array.
     * @return 2-dimensional array of entries.
    public double[][] getDataRef() { … }
     * {@inheritDoc}
    @Override public void setSubMatrix(final double[][] subMatrix, final int row, final int column) { … }
     * {@inheritDoc}
    @Override public double getEntry(final int row, final int column) { … }
     * {@inheritDoc}
    @Override public void setEntry(final int row, final int column, final double value) { … }
     * {@inheritDoc}
    @Override public void addToEntry(final int row, final int column, final double increment) { … }
     * {@inheritDoc}
    @Override public void multiplyEntry(final int row, final int column, final double factor) { … }
     * {@inheritDoc}
    @Override public int getRowDimension() { … }
     * {@inheritDoc}
    @Override public int getColumnDimension() { … }
     * {@inheritDoc}
    @Override public double[] operate(final double[] v) { … }
     * {@inheritDoc}
    @Override public double[] preMultiply(final double[] v) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Id$
public class MatrixUtils {
     * Private constructor.
    private MatrixUtils() {
        super();
    }
     * Returns a {@link RealMatrix} with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a {@link
     * Array2DRowRealMatrix} instance is built. Above this threshold a {@link
     * BlockRealMatrix} instance is built.</p>
     * <p>The matrix elements are all set to 0.0.</p>
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  RealMatrix with specified dimensions
     * @see #createRealMatrix(double[][])
    public static RealMatrix createRealMatrix(final int rows, final int columns) { … }
     * Returns a {@link FieldMatrix} with specified dimensions.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a {@link FieldMatrix} instance is built. Above
     * this threshold a {@link BlockFieldMatrix} instance is built.</p>
     * <p>The matrix elements are all set to field.getZero().</p>
     * @param <T> the type of the field elements
     * @param field field to which the matrix elements belong
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @return  FieldMatrix with specified dimensions
     * @see #createFieldMatrix(FieldElement[][])
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(final Field<T> field, final int rows, final int columns) { … }
     * Returns a {@link RealMatrix} whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix) which can be stored in a 32kB array, a {@link
     * Array2DRowRealMatrix} instance is built. Above this threshold a {@link
     * BlockRealMatrix} instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if {@code data} is not rectangular (not all rows have the same length).
     * …
    public static RealMatrix createRealMatrix(double[][] data) { … }
     * Returns a {@link FieldMatrix} whose entries are the the values in the
     * the input array.
     * <p>The type of matrix returned depends on the dimension. Below
     * 2<sup>12</sup> elements (i.e. 4096 elements or 64&times;64 for a
     * square matrix), a {@link FieldMatrix} instance is built. Above
     * this threshold a {@link BlockFieldMatrix} instance is built.</p>
     * <p>The input array is copied, not referenced.</p>
     * @param <T> the type of the field elements
     * @param data input array
     * @return a matrix containing the values of the array.
     * @throws org.apache.commons.math3.exception.DimensionMismatchException
     * if {@code data} is not rectangular (not all rows have the same length).
     * …
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldMatrix(T[][] data) { … }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
    public static RealMatrix createRealIdentityMatrix(int dimension) { … }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param <T> the type of the field elements
     * @param field field to which the elements belong
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { … }
     * Returns a diagonal matrix with specified elements.
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static RealMatrix createRealDiagonalMatrix(final double[] diagonal) { … }
     * Returns a diagonal matrix with specified elements.
     * @param <T> the type of the field elements
     * @param diagonal diagonal elements of the matrix (the array elements
     * will be copied)
     * @return diagonal matrix
     * @since 2.0
    public static <T extends FieldElement<T>> FieldMatrix<T> createFieldDiagonalMatrix(final T[] diagonal) { … }
     * Creates a {@link RealVector} using the data from the input array.
     * @param data the input data
     * @return a data.length RealVector
     * @throws NoDataException if {@code data} is empty.
     * @throws NullArgumentException if {@code data} is {@code null}.
    public static RealVector createRealVector(double[] data) { … }
     * Creates a {@link FieldVector} using the data from the input array.
     * @param <T> the type of the field elements
     * @param data the input data
     * @return a data.length FieldVector
     * @throws NoDataException if {@code data} is empty.
     * @throws NullArgumentException if {@code data} is {@code null}.
     * @throws ZeroException if {@code data} has 0 elements
    public static <T extends FieldElement<T>> FieldVector<T> createFieldVector(final T[] data) { … }
     * Converter for {@link FieldMatrix}/{@link Fraction}.
    private static class FractionMatrixConverter extends DefaultFieldMatrixPreservingVisitor<Fraction> { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
</class_skeletons>

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): hashCode

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `SimplexSolver`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math3.optimization.linear.SimplexSolverTest" method="testMath781">
    @Test
    public void testMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        Assert.assertTrue(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0);
        Assert.assertTrue(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0);
        Assert.assertEquals(2.0d, solution.getValue(), epsilon);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math3.optimization.linear.SimplexSolverTest::testMath781
junit.framework.AssertionFailedError
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
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,200 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static LinearObjectiveFunction seedObjective() {
        return new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
    }

    private static ArrayList<LinearConstraint> seedConstraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static void fail(String oracle, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + oracle + "] semantic mismatch: " + msg);
    }

    private static void failMeta(String oracle, String msg) {
        throw new RuntimeException("[oracle:" + oracle + "] metamorphic violation: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = seedObjective();
        ArrayList<LinearConstraint> constraints = seedConstraints();

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            fail("math781-x0", "Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true but was false; actual=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            fail("math781-x1", "Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected true but was false; actual=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            fail("math781-x2", "Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected true but was false; actual=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            fail("math781-value", "Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected=2.0 actual=" + solution.getValue());
        }

        try {
            /*
             * Contract/oracle: dropPhase1Objective "removes the phase 1 objective function"
             * and sets numArtificialVariables to 0 in its body. Therefore after one successful
             * call, getNumObjectiveFunctions() must report Phase 2 (1 objective function), and
             * a second call is a no-op because the first line returns when getNumObjectiveFunctions() == 1.
             * A patch that merely avoids the bad comparison or skips required bookkeeping would violate
             * this observable post-condition and/or the idempotence relation.
             */
            SimplexTableau tableau1 =
                new SimplexTableau(seedObjective(), seedConstraints(), GoalType.MAXIMIZE, false, 1e-6, 10);
            SimplexTableau tableau2 =
                new SimplexTableau(seedObjective(), seedConstraints(), GoalType.MAXIMIZE, false, 1e-6, 10);

            tableau1.dropPhase1Objective();
            tableau2.dropPhase1Objective();

            if (tableau1.getNumObjectiveFunctions() != 1) {
                failMeta("drop-phase1-post", "after first dropPhase1Objective, getNumObjectiveFunctions() must be 1 but was " + tableau1.getNumObjectiveFunctions());
            }
            if (tableau1.getNumArtificialVariables() != 0) {
                failMeta("drop-phase1-post", "after first dropPhase1Objective, getNumArtificialVariables() must be 0 but was " + tableau1.getNumArtificialVariables());
            }
            if (!tableau1.equals(tableau2)) {
                failMeta("drop-phase1-equals", "two identically constructed tableaus after dropPhase1Objective must be equal");
            }
            if (tableau1.hashCode() != tableau2.hashCode()) {
                failMeta("drop-phase1-hash", "equal tableaus must have identical hashCode values; lhs=" + tableau1.hashCode() + " rhs=" + tableau2.hashCode());
            }

            int beforeSecondHash = tableau1.hashCode();
            int beforeSecondWidth = tableau1.getWidth();
            int beforeSecondHeight = tableau1.getHeight();
            int beforeSecondObjectives = tableau1.getNumObjectiveFunctions();
            int beforeSecondArtificial = tableau1.getNumArtificialVariables();

            tableau1.dropPhase1Objective();

            if (tableau1.getNumObjectiveFunctions() != beforeSecondObjectives ||
                tableau1.getNumArtificialVariables() != beforeSecondArtificial ||
                tableau1.getWidth() != beforeSecondWidth ||
                tableau1.getHeight() != beforeSecondHeight ||
                tableau1.hashCode() != beforeSecondHash) {
                failMeta(
                    "drop-phase1-idempotent",
                    "second dropPhase1Objective call must be a no-op once getNumObjectiveFunctions()==1"
                        + " objectivesBefore=" + beforeSecondObjectives
                        + " objectivesAfter=" + tableau1.getNumObjectiveFunctions()
                        + " artificialBefore=" + beforeSecondArtificial
                        + " artificialAfter=" + tableau1.getNumArtificialVariables()
                        + " widthBefore=" + beforeSecondWidth
                        + " widthAfter=" + tableau1.getWidth()
                        + " heightBefore=" + beforeSecondHeight
                        + " heightAfter=" + tableau1.getHeight()
                        + " hashBefore=" + beforeSecondHash
                        + " hashAfter=" + tableau1.hashCode());
            }

            if (!tableau1.equals(tableau2) || tableau1.hashCode() != tableau2.hashCode()) {
                failMeta(
                    "drop-phase1-stable",
                    "a no-op second dropPhase1Objective call must preserve equality/hashCode with an equivalent tableau"
                        + " equals=" + tableau1.equals(tableau2)
                        + " lhsHash=" + tableau1.hashCode()
                        + " rhsHash=" + tableau2.hashCode());
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            int tweak = data.consumeInt(-3, 3);
            double[] obj = new double[] { 2 + tweak, 6 - tweak, 7 };
            ArrayList<LinearConstraint> fuzzConstraints = seedConstraints();
            LinearObjectiveFunction fuzzObjective = new LinearObjectiveFunction(obj, 0);
            new SimplexSolver().optimize(fuzzObjective, fuzzConstraints, GoalType.MAXIMIZE, false);
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [71] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true but was false; actual=0.0

---
## [72] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,394 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double regressionEpsilon = 1.0e-6;

        PointValuePair regressionSolution;
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            regressionSolution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = regressionSolution.getPoint();
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, regressionEpsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(regressionSolution.getValue() - 2.0d) <= regressionEpsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within epsilon=1.0E-6 but actual value=" + regressionSolution.getValue());
        }

        boolean postViolation = false;
        String postMessage = null;
        try {
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);
            double eps = 1.0e-6 + (data.consumeInt(0, 1000) * 1.0e-9);
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f1 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);

            java.util.ArrayList<LinearConstraint> constraints1 = new java.util.ArrayList<LinearConstraint>();
            constraints1.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints1.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints1.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t1 = new SimplexTableau(f1, constraints1, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            SimplexTableau t2 = new SimplexTableau(f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);

            int hashBefore1 = t1.hashCode();
            int hashBefore2 = t2.hashCode();
            int hashBefore1Again = t1.hashCode();
            int heightBefore = t1.getHeight();
            int numArtificialBefore = t1.getNumArtificialVariables();

            boolean equalBefore = t1.equals(t2);

            t1.dropPhase1Objective();
            t2.dropPhase1Objective();

            int hashAfter1 = t1.hashCode();
            int hashAfter2 = t2.hashCode();
            int heightAfter = t1.getHeight();
            int numArtificialAfter = t1.getNumArtificialVariables();
            boolean equalAfter = t1.equals(t2);

            // Contract justification: dropPhase1Objective() explicitly removes the phase-1 objective function
            // and sets numArtificialVariables to 0. A patch that merely skips the body or makes it unreachable
            // violates these observable post-conditions even if no exception is thrown.
            if (numArtificialBefore <= 0) {
                return;
            }
            if (heightBefore <= 1) {
                return;
            }
            if (hashBefore1 != hashBefore1Again) {
                postViolation = true;
                postMessage = "[oracle:hidden-hashcode] metamorphic violation: hashCode() is a read-only query and must be stable across repeated reads before mutation; first=" + hashBefore1 + " second=" + hashBefore1Again;
            } else if (!equalBefore) {
                postViolation = true;
                postMessage = "[oracle:twin-before] metamorphic violation: identically constructed tableaux must be equal before mutation";
            } else if (hashBefore1 != hashBefore2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-before] metamorphic violation: equal tableaux must have identical hashCode before mutation; left=" + hashBefore1 + " right=" + hashBefore2;
            } else if (heightAfter != heightBefore - 1) {
                postViolation = true;
                postMessage = "[oracle:drop-height] metamorphic violation: dropPhase1Objective must remove exactly the phase-1 objective row; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (numArtificialAfter != 0) {
                postViolation = true;
                postMessage = "[oracle:drop-artificial-count] metamorphic violation: dropPhase1Objective must leave zero artificial variables; before=" + numArtificialBefore + " after=" + numArtificialAfter;
            } else if (!equalAfter) {
                postViolation = true;
                postMessage = "[oracle:twin-after] metamorphic violation: applying the same real mutation to equal tableaux must leave them equal";
            } else if (hashAfter1 != hashAfter2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-after] metamorphic violation: equal tableaux must have identical hashCode after dropPhase1Objective; left=" + hashAfter1 + " right=" + hashAfter2;
            }
        } catch (Throwable t) {
            return;
        }
        if (postViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(postMessage);
        }

        boolean widthViolation = false;
        String widthMessage = null;
        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            double eps = 1.0e-6;
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            int heightBefore = t.getHeight();
            int widthBefore = t.getWidth();
            int numObjectiveFunctions = t.getNumObjectiveFunctions();
            int artificialOffset = t.getArtificialVariableOffset();

            if (t.getNumArtificialVariables() <= 0 || numObjectiveFunctions != 2 || artificialOffset <= numObjectiveFunctions) {
                return;
            }

            for (int j = numObjectiveFunctions; j < artificialOffset; j++) {
                t.setEntry(0, j, 0.0);
            }

            int widthBeforeDrop = t.getWidth();
            t.dropPhase1Objective();
            int heightAfter = t.getHeight();
            int widthAfter = t.getWidth();

            // Contract justification: with all scanned non-artificial phase-1 costs forced to zero,
            // dropPhase1Objective() still must remove the phase-1 objective row and at least column 0.
            // A throw-deleting/skip-body patch leaves height and width unchanged, violating this observable state.
            if (widthBefore != widthBeforeDrop) {
                return;
            }
            if (heightAfter != heightBefore - 1) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-height] metamorphic violation: dropPhase1Objective must remove the phase-1 objective row even when scanned costs are zero; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (!(widthAfter < widthBefore)) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-width] metamorphic violation: dropPhase1Objective must drop at least one column (column 0); beforeWidth=" + widthBefore + " afterWidth=" + widthAfter;
            }
        } catch (Throwable t) {
            return;
        }
        if (widthViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(widthMessage);
        }
    }
}
```

</details>

---
## [73] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [74] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,691 chars, 2 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double regressionEpsilon = 1.0e-6;

        PointValuePair regressionSolution;
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            regressionSolution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = regressionSolution.getPoint();
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, regressionEpsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(regressionSolution.getValue() - 2.0d) <= regressionEpsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within epsilon=1.0E-6 but actual value=" + regressionSolution.getValue());
        }

        boolean postViolation = false;
        String postMessage = null;
        try {
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);
            double eps = 1.0e-6 + (data.consumeInt(0, 1000) * 1.0e-9);
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f1 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);

            java.util.ArrayList<LinearConstraint> constraints1 = new java.util.ArrayList<LinearConstraint>();
            constraints1.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints1.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints1.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t1 = new SimplexTableau(f1, constraints1, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            SimplexTableau t2 = new SimplexTableau(f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);

            int hashBefore1 = t1.hashCode();
            int hashBefore2 = t2.hashCode();
            int hashBefore1Again = t1.hashCode();
            int heightBefore = t1.getHeight();
            int numArtificialBefore = t1.getNumArtificialVariables();

            boolean equalBefore = t1.equals(t2);

            t1.dropPhase1Objective();
            t2.dropPhase1Objective();

            int hashAfter1 = t1.hashCode();
            int hashAfter2 = t2.hashCode();
            int heightAfter = t1.getHeight();
            int numArtificialAfter = t1.getNumArtificialVariables();
            boolean equalAfter = t1.equals(t2);

            // Contract justification: dropPhase1Objective() explicitly removes the phase-1 objective function
            // and sets numArtificialVariables to 0. A patch that merely skips the body or makes it unreachable
            // violates these observable post-conditions even if no exception is thrown.
            if (numArtificialBefore <= 0) {
                return;
            }
            if (heightBefore <= 1) {
                return;
            }
            if (hashBefore1 != hashBefore1Again) {
                postViolation = true;
                postMessage = "[oracle:hidden-hashcode] metamorphic violation: hashCode() is a read-only query and must be stable across repeated reads before mutation; first=" + hashBefore1 + " second=" + hashBefore1Again;
            } else if (!equalBefore) {
                postViolation = true;
                postMessage = "[oracle:twin-before] metamorphic violation: identically constructed tableaux must be equal before mutation";
            } else if (hashBefore1 != hashBefore2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-before] metamorphic violation: equal tableaux must have identical hashCode before mutation; left=" + hashBefore1 + " right=" + hashBefore2;
            } else if (heightAfter != heightBefore - 1) {
                postViolation = true;
                postMessage = "[oracle:drop-height] metamorphic violation: dropPhase1Objective must remove exactly the phase-1 objective row; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (numArtificialAfter != 0) {
                postViolation = true;
                postMessage = "[oracle:drop-artificial-count] metamorphic violation: dropPhase1Objective must leave zero artificial variables; before=" + numArtificialBefore + " after=" + numArtificialAfter;
            } else if (!equalAfter) {
                postViolation = true;
                postMessage = "[oracle:twin-after] metamorphic violation: applying the same real mutation to equal tableaux must leave them equal";
            } else if (hashAfter1 != hashAfter2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-after] metamorphic violation: equal tableaux must have identical hashCode after dropPhase1Objective; left=" + hashAfter1 + " right=" + hashAfter2;
            }
        } catch (Throwable t) {
            return;
        }
        if (postViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(postMessage);
        }

        boolean widthViolation = false;
        String widthMessage = null;
        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            double eps = 1.0e-6;
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            int heightBefore = t.getHeight();
            int widthBefore = t.getWidth();
            int numObjectiveFunctions = t.getNumObjectiveFunctions();
            int artificialOffset = t.getArtificialVariableOffset();

            if (t.getNumArtificialVariables() <= 0 || numObjectiveFunctions != 2 || artificialOffset <= numObjectiveFunctions) {
                return;
            }

            for (int j = numObjectiveFunctions; j < artificialOffset; j++) {
                t.setEntry(0, j, 0.0);
            }

            int widthBeforeDrop = t.getWidth();
            t.dropPhase1Objective();
            int heightAfter = t.getHeight();
            int widthAfter = t.getWidth();

            // Contract justification: with all scanned non-artificial phase-1 costs forced to zero,
            // dropPhase1Objective() still must remove the phase-1 objective row and at least column 0.
            // A throw-deleting/skip-body patch leaves height and width unchanged, violating this observable state.
            if (widthBefore != widthBeforeDrop) {
                return;
            }
            if (heightAfter != heightBefore - 1) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-height] metamorphic violation: dropPhase1Objective must remove the phase-1 objective row even when scanned costs are zero; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (!(widthAfter < widthBefore)) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-width] metamorphic violation: dropPhase1Objective must drop at least one column (column 0); beforeWidth=" + widthBefore + " afterWidth=" + widthAfter;
            }
        } catch (Throwable t) {
            return;
        }
        if (widthViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(widthMessage);
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/fuzz/attempt_003/FuzzHarness.java:9: error: cannot find symbol
        PointValuePair regressionSolution;
        ^
  symbol:   class PointValuePair
  location: class FuzzHarness
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
<details open><summary>▸ Output (~10,089 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double regressionEpsilon = 1.0e-6;

        org.apache.commons.math3.optimization.PointValuePair regressionSolution;
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            regressionSolution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = regressionSolution.getPoint();
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, regressionEpsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(regressionSolution.getValue() - 2.0d) <= regressionEpsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within epsilon=1.0E-6 but actual value=" + regressionSolution.getValue());
        }

        boolean postViolation = false;
        String postMessage = null;
        try {
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);
            double eps = 1.0e-6 + (data.consumeInt(0, 1000) * 1.0e-9);
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f1 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);

            java.util.ArrayList<LinearConstraint> constraints1 = new java.util.ArrayList<LinearConstraint>();
            constraints1.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints1.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints1.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t1 = new SimplexTableau(f1, constraints1, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            SimplexTableau t2 = new SimplexTableau(f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);

            int hashBefore1 = t1.hashCode();
            int hashBefore2 = t2.hashCode();
            int hashBefore1Again = t1.hashCode();
            int heightBefore = t1.getHeight();
            int numArtificialBefore = t1.getNumArtificialVariables();

            boolean equalBefore = t1.equals(t2);

            t1.dropPhase1Objective();
            t2.dropPhase1Objective();

            int hashAfter1 = t1.hashCode();
            int hashAfter2 = t2.hashCode();
            int heightAfter = t1.getHeight();
            int numArtificialAfter = t1.getNumArtificialVariables();
            boolean equalAfter = t1.equals(t2);

            // Contract justification: dropPhase1Objective() removes the phase-1 objective function and
            // resets numArtificialVariables to 0; skipping or neutering the method would violate these observables.
            if (numArtificialBefore <= 0) {
                return;
            }
            if (heightBefore <= 1) {
                return;
            }
            if (hashBefore1 != hashBefore1Again) {
                postViolation = true;
                postMessage = "[oracle:hidden-hashcode] metamorphic violation: hashCode() changed across repeated read-only calls before mutation; first=" + hashBefore1 + " second=" + hashBefore1Again;
            } else if (!equalBefore) {
                postViolation = true;
                postMessage = "[oracle:twin-before] metamorphic violation: identically constructed tableaux must be equal before mutation";
            } else if (hashBefore1 != hashBefore2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-before] metamorphic violation: equal tableaux must have the same hashCode before mutation; left=" + hashBefore1 + " right=" + hashBefore2;
            } else if (heightAfter != heightBefore - 1) {
                postViolation = true;
                postMessage = "[oracle:drop-height] metamorphic violation: dropPhase1Objective must remove exactly one row; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (numArtificialAfter != 0) {
                postViolation = true;
                postMessage = "[oracle:drop-artificial-count] metamorphic violation: dropPhase1Objective must leave zero artificial variables; before=" + numArtificialBefore + " after=" + numArtificialAfter;
            } else if (!equalAfter) {
                postViolation = true;
                postMessage = "[oracle:twin-after] metamorphic violation: applying the same mutation to equal tableaux must preserve equality";
            } else if (hashAfter1 != hashAfter2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-after] metamorphic violation: equal tableaux must have the same hashCode after dropPhase1Objective; left=" + hashAfter1 + " right=" + hashAfter2;
            }
        } catch (Throwable t) {
            return;
        }
        if (postViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(postMessage);
        }

        boolean widthViolation = false;
        String widthMessage = null;
        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            double eps = 1.0e-6;
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            int heightBefore = t.getHeight();
            int widthBefore = t.getWidth();
            int numObjectiveFunctions = t.getNumObjectiveFunctions();
            int artificialOffset = t.getArtificialVariableOffset();

            if (t.getNumArtificialVariables() <= 0 || numObjectiveFunctions != 2 || artificialOffset <= numObjectiveFunctions) {
                return;
            }

            for (int j = numObjectiveFunctions; j < artificialOffset; j++) {
                t.setEntry(0, j, 0.0);
            }

            int widthBeforeDrop = t.getWidth();
            t.dropPhase1Objective();
            int heightAfter = t.getHeight();
            int widthAfter = t.getWidth();

            // Contract justification: even when all scanned non-artificial phase-1 costs are zero,
            // dropPhase1Objective must still remove the phase-1 objective row and column 0.
            if (widthBefore != widthBeforeDrop) {
                return;
            }
            if (heightAfter != heightBefore - 1) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-height] metamorphic violation: expected one row to be removed; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (!(widthAfter < widthBefore)) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-width] metamorphic violation: expected at least one column to be removed; beforeWidth=" + widthBefore + " afterWidth=" + widthAfter;
            }
        } catch (Throwable t) {
            return;
        }
        if (widthViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(widthMessage);
        }
    }
}
```

</details>

---
## [75] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [76] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,521 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction baseObjective = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> baseConstraints = new ArrayList<LinearConstraint>();
        baseConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        baseConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        baseConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(baseObjective, baseConstraints, GoalType.MAXIMIZE, false);
            double[] point = solution.getPoint();

            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actualPoint0=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actualPoint1=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actualPoint2=" + point[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: expectedValue=2.0 actualValue=" + solution.getValue() + " epsilon=" + epsilon);
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) {
            return;
        }

        try {
            int fuzzConst = data.consumeInt(-1000, 1000);
            LinearObjectiveFunction shiftedObjective = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, fuzzConst);
            SimplexSolver solver = new SimplexSolver();
            PointValuePair shifted = solver.optimize(shiftedObjective, baseConstraints, GoalType.MAXIMIZE, false);

            // Contract used for this oracle:
            // LinearObjectiveFunction represents c*x + d. Changing only the constant term d
            // cannot change the argmax over the same feasible region; it only shifts the
            // optimal value by that same constant. A patch that merely avoids the faulty
            // branch but returns the wrong phase-2 tableau/solution violates this relation.
            double[] point = shifted.getPoint();
            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-x0] metamorphic violation: constant-term shift changed sign property inputConst=" + fuzzConst + " point0=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-x1] metamorphic violation: constant-term shift changed sign property inputConst=" + fuzzConst + " point1=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-x2] metamorphic violation: constant-term shift changed sign property inputConst=" + fuzzConst + " point2=" + point[2]);
            }
            double expectedValue = 2.0d + fuzzConst;
            if (!(Math.abs(shifted.getValue() - expectedValue) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-value] metamorphic violation: constant-term shift relation inputConst=" + fuzzConst + " expectedValue=" + expectedValue + " actualValue=" + shifted.getValue());
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) {
            return;
        }

        try {
            int maxUlps = data.consumeInt(1, 32);
            boolean restrictToNonNegative = false;

            SimplexTableau tableauA = new SimplexTableau(baseObjective, baseConstraints, GoalType.MAXIMIZE, restrictToNonNegative, epsilon, maxUlps);
            SimplexTableau tableauB = new SimplexTableau(baseObjective, baseConstraints, GoalType.MAXIMIZE, restrictToNonNegative, epsilon, maxUlps);

            if (tableauA.getNumObjectiveFunctions() > 1) {
                tableauA.dropPhase1Objective();
                tableauB.dropPhase1Objective();

                int hashAfterFirst = tableauA.hashCode();
                tableauA.dropPhase1Objective();
                int hashAfterSecond = tableauA.hashCode();

                // Contract used for this oracle:
                // dropPhase1Objective removes the phase-1 objective and sets numArtificialVariables to 0.
                // Once the tableau has only one objective function, a second call returns immediately.
                // Therefore the second call is a documented no-op, so public reader hashCode() and equals()
                // must report the same state after one call and after two calls. A patch that skips or
                // corrupts the state update can make these observable readers disagree.
                if (tableauA.getNumObjectiveFunctions() != 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-numObjectives] post-condition violation: expected one objective after drop actual=" + tableauA.getNumObjectiveFunctions());
                }
                if (tableauA.getNumArtificialVariables() != 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-artificial-zero] post-condition violation: expected zero artificial variables after drop actual=" + tableauA.getNumArtificialVariables());
                }
                if (hashAfterFirst != hashAfterSecond) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-idempotent-hash] metamorphic violation: second drop changed hashCode first=" + hashAfterFirst + " second=" + hashAfterSecond);
                }
                if (!tableauA.equals(tableauB)) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-idempotent-equals] metamorphic violation: dropPhase1Objective once vs twice should yield equal tableaus");
                }
                if (tableauA.hashCode() != tableauB.hashCode()) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-idempotent-eqhash] metamorphic violation: equal tableaus must have same hashCode hashA=" + tableauA.hashCode() + " hashB=" + tableauB.hashCode());
                }
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [77] ⚙️ harness-repair · `attempt_005`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [78] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actualPoint0=0.0

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,898 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        String violation = null;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                violation = "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0];
            } else if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                violation = "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1];
            } else if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                violation = "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2];
            } else if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                violation = "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d±1.0E-6 but actual value=" + solution.getValue();
            }
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            int scale = data.consumeInt(1, 16);
            LinearObjectiveFunction scaled = new LinearObjectiveFunction(
                    new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);

            SimplexSolver solver1 = new SimplexSolver();
            SimplexSolver solver2 = new SimplexSolver();
            PointValuePair base = solver1.optimize(f, constraints, GoalType.MAXIMIZE, false);
            PointValuePair scaledSolution = solver2.optimize(scaled, constraints, GoalType.MAXIMIZE, false);

            double expectedScaledValue = base.getValue() * scale;
            double actualScaledValue = scaledSolution.getValue();
            double tol = epsilon * Math.max(1.0d, Math.max(Math.abs(expectedScaledValue), Math.abs(actualScaledValue)));

            // Contract justification: maximizing c^T x over the same feasible region and maximizing (k*c)^T x
            // for any positive k must scale the optimum value by k; deleting/skipping the intended column drop can
            // leave simplex bookkeeping inconsistent and violate this observable post-condition without throwing.
            if (!(Math.abs(actualScaledValue - expectedScaledValue) <= tol)) {
                violation = "[oracle:scaled-objective-value] metamorphic violation: positive objective scaling must scale optimum value inputScale="
                        + scale + " lhs=" + actualScaledValue + " rhs=" + expectedScaledValue;
            }
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);

            LinearObjectiveFunction f = new LinearObjectiveFunction(
                    new double[] { 2.0 + da, 6.0 + db, 7.0 + dc }, 0.0);

            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            SimplexTableau tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            // Contract justification: these are reader methods; a read-only query must not mutate observable state.
            int zeroHashBeforeRead = tZero.hashCode();
            int posHashBeforeRead = tPos.hashCode();
            int zeroWidthBefore = tZero.getWidth();
            int zeroHeightBefore = tZero.getHeight();
            int posWidthBefore = tPos.getWidth();
            int posHeightBefore = tPos.getHeight();
            int zeroHashAfterRead = tZero.hashCode();
            int posHashAfterRead = tPos.hashCode();
            if (zeroHashBeforeRead != zeroHashAfterRead || posHashBeforeRead != posHashAfterRead) {
                violation = "[oracle:readonly-hash-stability] semantic mismatch: read-only tableau queries changed hashCode zeroBefore="
                        + zeroHashBeforeRead + " zeroAfter=" + zeroHashAfterRead + " posBefore=" + posHashBeforeRead
                        + " posAfter=" + posHashAfterRead;
            }
            if (violation != null) {
                throw new FuzzerSecurityIssueLow(violation);
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }

            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            if (!(tinyPositive > 0.0d)) {
                return;
            }
            tPos.setEntry(0, chosen, tinyPositive);

            int hashZeroBeforeDrop = tZero.hashCode();
            int hashPosBeforeDrop = tPos.hashCode();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();

            int zeroWidthAfter = tZero.getWidth();
            int zeroHeightAfter = tZero.getHeight();
            int posWidthAfter = tPos.getWidth();
            int posHeightAfter = tPos.getHeight();

            // Contract justification: dropPhase1Objective() explicitly removes "positive cost non-artificial variables";
            // changing exactly one scanned non-artificial phase-1 entry from 0 to a strictly positive value must
            // therefore remove exactly one additional column and no rows.
            if (zeroWidthBefore != posWidthBefore || zeroHeightBefore != posHeightBefore) {
                violation = "[oracle:drop-baseline] semantic mismatch: baseline tableaux differ before probe zeroWidth="
                        + zeroWidthBefore + " posWidth=" + posWidthBefore + " zeroHeight=" + zeroHeightBefore
                        + " posHeight=" + posHeightBefore;
            } else if (zeroHeightAfter != posHeightAfter) {
                violation = "[oracle:drop-height] semantic mismatch: changing one scanned coefficient should not change row count zeroHeightAfter="
                        + zeroHeightAfter + " posHeightAfter=" + posHeightAfter;
            } else if (posWidthAfter != zeroWidthAfter - 1) {
                violation = "[oracle:tiny-positive-drop] semantic mismatch: tiny positive scanned non-artificial column was not additionally dropped zeroCaseWidth="
                        + zeroWidthAfter + " posCaseWidth=" + posWidthAfter + " chosen=" + chosen + " tinyPositive=" + tinyPositive;
            }

            // Contract justification: equals/hashCode share the same state fields listed for this class; two tableaux
            // built identically and transformed identically must remain equal and keep matching hash codes.
            SimplexTableau a = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            SimplexTableau b = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            for (int j = start; j < end; j++) {
                a.setEntry(0, j, 0.0);
                b.setEntry(0, j, 0.0);
            }
            a.dropPhase1Objective();
            b.dropPhase1Objective();
            if (!a.equals(b)) {
                violation = "[oracle:equals-after-drop] semantic mismatch: identically transformed tableaux are not equal";
            } else if (a.hashCode() != b.hashCode()) {
                violation = "[oracle:hash-agrees-with-equals] semantic mismatch: equal tableaux have different hash codes aHash="
                        + a.hashCode() + " bHash=" + b.hashCode();
            } else if (hashZeroBeforeDrop == tZero.hashCode() && hashPosBeforeDrop == tPos.hashCode()
                    && zeroWidthAfter != zeroWidthBefore && posWidthAfter != posWidthBefore) {
                // Contract justification: dropPhase1Objective mutates tableau and numArtificialVariables;
                // a patch that skips bookkeeping can leave hashCode stale even though dimensions changed.
                violation = "[oracle:hash-reflects-mutation] semantic mismatch: dropPhase1Objective changed tableau dimensions but hashCode stayed unchanged zeroBefore="
                        + hashZeroBeforeDrop + " zeroAfter=" + tZero.hashCode() + " posBefore=" + hashPosBeforeDrop
                        + " posAfter=" + tPos.hashCode();
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }
}
```

</details>

---
## [80] ⚙️ harness-repair · `attempt_006`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [81] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [82] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,612 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double TEST_EPSILON = 1e-6;
    private static final int DEFAULT_ULPS = 10;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted exactly from SimplexSolverTest.testMath781.
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution;
        try {
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (point == null || point.length < 3) {
            return;
        }

        if (!(Precision.compareTo(point[0], 0.0d, TEST_EPSILON) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 was false; actual=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, TEST_EPSILON) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 was false; actual=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, TEST_EPSILON) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 was false; actual=" + point[2]);
        }
        if (Math.abs(solution.getValue() - 2.0d) > TEST_EPSILON) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: expected=2.0 actual=" + solution.getValue() + " epsilon=" + TEST_EPSILON);
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, TEST_EPSILON, DEFAULT_ULPS);

            if (tableau.getNumObjectiveFunctions() != 2) {
                return;
            }

            int widthBefore = tableau.getWidth();
            int heightBefore = tableau.getHeight();
            int numArtificialBefore = tableau.getNumArtificialVariables();

            Set<Integer> expectedDropColumns = new HashSet<Integer>();
            expectedDropColumns.add(Integer.valueOf(0));

            // Contract from dropPhase1Objective javadoc/body:
            // it removes the phase 1 objective function, positive-cost non-artificial variables,
            // and non-basic artificial variables. A patch that merely skips/removes this mutation
            // violates these observable post-conditions.
            for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getArtificialVariableOffset(); i++) {
                final double entry = tableau.getEntry(0, i);
                if (Precision.compareTo(entry, 0.0d, TEST_EPSILON) > 0) {
                    expectedDropColumns.add(Integer.valueOf(i));
                }
            }

            for (int i = 0; i < tableau.getNumArtificialVariables(); i++) {
                int col = i + tableau.getArtificialVariableOffset();
                if (tableau.getBasicRow(col) == null) {
                    expectedDropColumns.add(Integer.valueOf(col));
                }
            }

            int expectedWidthAfter = widthBefore - expectedDropColumns.size();
            int expectedHeightAfter = heightBefore - 1;

            tableau.dropPhase1Objective();

            if (tableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException("[oracle:dropPhase1Objective-artificial] metamorphic violation: dropPhase1Objective must clear artificial variables; before=" + numArtificialBefore + " after=" + tableau.getNumArtificialVariables());
            }
            if (tableau.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException("[oracle:dropPhase1Objective-objectives] metamorphic violation: dropPhase1Objective must remove the phase 1 objective row; actualNumObjectiveFunctions=" + tableau.getNumObjectiveFunctions());
            }
            if (tableau.getWidth() != expectedWidthAfter) {
                throw new RuntimeException("[oracle:dropPhase1Objective-width] metamorphic violation: width after dropping removed columns inputWidth=" + widthBefore + " expected=" + expectedWidthAfter + " actual=" + tableau.getWidth());
            }
            if (tableau.getHeight() != expectedHeightAfter) {
                throw new RuntimeException("[oracle:dropPhase1Objective-height] metamorphic violation: height after removing phase 1 objective row expected=" + expectedHeightAfter + " actual=" + tableau.getHeight());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Generalization without inventing new expected solver outputs:
        // choose a moderate epsilon and verify the same documented post-condition on a related real call.
        try {
            double fuzzEpsilon = 1.0e-9 + (Math.abs(data.consumeInt(-1000, 1000)) / 1000.0) * 1.0e-3;
            int fuzzUlps = Math.max(1, Math.abs(data.consumeInt(-32, 32)));

            SimplexTableau tableau2 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzUlps);
            if (tableau2.getNumObjectiveFunctions() != 2) {
                return;
            }

            Set<Integer> expectedDropColumns2 = new HashSet<Integer>();
            expectedDropColumns2.add(Integer.valueOf(0));
            for (int i = tableau2.getNumObjectiveFunctions(); i < tableau2.getArtificialVariableOffset(); i++) {
                final double entry = tableau2.getEntry(0, i);
                if (Precision.compareTo(entry, 0.0d, fuzzEpsilon) > 0) {
                    expectedDropColumns2.add(Integer.valueOf(i));
                }
            }
            for (int i = 0; i < tableau2.getNumArtificialVariables(); i++) {
                int col = i + tableau2.getArtificialVariableOffset();
                if (tableau2.getBasicRow(col) == null) {
                    expectedDropColumns2.add(Integer.valueOf(col));
                }
            }

            int expectedWidthAfter2 = tableau2.getWidth() - expectedDropColumns2.size();
            tableau2.dropPhase1Objective();

            if (tableau2.getWidth() != expectedWidthAfter2) {
                throw new RuntimeException("[oracle:dropPhase1Objective-width-fuzz] metamorphic violation: width after dropping removed columns expected=" + expectedWidthAfter2 + " actual=" + tableau2.getWidth() + " epsilon=" + fuzzEpsilon + " maxUlps=" + fuzzUlps);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [83] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 was false; actual=0.0

---
## [84] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,600 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Trusted lifted oracle from SimplexSolverTest.testMath781: exact public-API setup and exact expected outcomes.
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1.0e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d ±1.0E-6 but actual value=" + solution.getValue());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Metamorphic/post-condition on the patched method:
        // dropPhase1Objective() explicitly removes "positive cost non-artificial variables".
        // Therefore, for two otherwise-identical valid phase-1 tableaux, changing exactly one scanned
        // non-artificial phase-1 coefficient from 0 to a strictly positive value must remove exactly one
        // additional column. A patch that merely skips/removes the bookkeeping in dropPhase1Objective
        // will violate this observable width relation even without throwing.
        boolean tinyDropReady = false;
        int widthZeroAfter = 0;
        int widthPosAfter = 0;
        int heightZeroAfter = 0;
        int heightPosAfter = 0;
        int widthZeroBefore = 0;
        int widthPosBefore = 0;
        int heightZeroBefore = 0;
        int heightPosBefore = 0;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);

            LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero = new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, 0.0, 10);
            SimplexTableau tPos = new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, 0.0, 10);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }

            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.setEntry(0, chosen, tinyPositive);

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
            tinyDropReady = true;
        } catch (Throwable t) {
            return;
        }
        if (tinyDropReady) {
            if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                throw new FuzzerSecurityIssueLow("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: baseline tableaux not identical before probe: widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore);
            }
            if (heightZeroAfter != heightPosAfter) {
                throw new FuzzerSecurityIssueLow("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: changing one scanned coefficient should not change row count: heightZeroAfter=" + heightZeroAfter + " heightPosAfter=" + heightPosAfter);
            }
            if (widthPosAfter != widthZeroAfter - 1) {
                throw new FuzzerSecurityIssueLow("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: tiny positive scanned non-artificial column was not additionally dropped: zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter);
            }
        }

        // Additional trusted hidden-state / sibling-agreement checks.
        // equals/hashCode must agree for two tableaux built with identical constructor state, and keep agreeing
        // after both undergo the same real library mutation via dropPhase1Objective(). This checks that the
        // state written by the constructor / dropPhase1Objective is consistently reported by readers.
        boolean eqReady = false;
        boolean beforeEquals = false;
        boolean afterEquals = false;
        int beforeHashA = 0;
        int beforeHashB = 0;
        int afterHashA = 0;
        int afterHashB = 0;
        try {
            int aa = data.consumeInt(-3, 3);
            int bb = data.consumeInt(-3, 3);
            int cc = data.consumeInt(-3, 3);

            LinearObjectiveFunction f3 = new LinearObjectiveFunction(new double[] { 2 + aa, 6 + bb, 7 + cc }, 0.0);
            ArrayList<LinearConstraint> constraints3 = new ArrayList<LinearConstraint>();
            constraints3.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints3.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints3.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau ta = new SimplexTableau(f3, constraints3, GoalType.MAXIMIZE, false, 1.0e-6, 10);
            SimplexTableau tb = new SimplexTableau(f3, constraints3, GoalType.MAXIMIZE, false, 1.0e-6, 10);

            beforeHashA = ta.hashCode();
            beforeHashB = tb.hashCode();
            int repeatHashA = ta.hashCode();
            int repeatHashB = tb.hashCode();
            if (beforeHashA != repeatHashA || beforeHashB != repeatHashB) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly] semantic mismatch: repeated hashCode() on unchanged tableau changed state: firstA=" + beforeHashA + " secondA=" + repeatHashA + " firstB=" + beforeHashB + " secondB=" + repeatHashB);
            }

            beforeEquals = ta.equals(tb);

            ta.dropPhase1Objective();
            tb.dropPhase1Objective();

            afterHashA = ta.hashCode();
            afterHashB = tb.hashCode();
            afterEquals = ta.equals(tb);
            eqReady = true;
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
        if (eqReady) {
            if (!beforeEquals || beforeHashA != beforeHashB) {
                throw new FuzzerSecurityIssueLow("relation tableau_equalsHashCode_beforeDrop violated: identical tableaux disagree before mutation: equals=" + beforeEquals + " hashA=" + beforeHashA + " hashB=" + beforeHashB);
            }
            if (!afterEquals || afterHashA != afterHashB) {
                throw new FuzzerSecurityIssueLow("relation tableau_equalsHashCode_afterDrop violated: equally-mutated tableaux disagree after dropPhase1Objective: equals=" + afterEquals + " hashA=" + afterHashA + " hashB=" + afterHashB);
            }
        }
    }
}
```

</details>

---
## [85] ⚙️ harness-repair · `attempt_008`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [86] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,396 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        double[] point = solution.getPoint();
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false actualX0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false actualX1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false actualX2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual=" + solution.getValue());
        }

        int fuzzMaxUlps = data.consumeInt(1, 100);
        double fuzzEpsilon = Math.abs(data.consumeInt(-1000000, 1000000)) / 1000000.0;
        if (fuzzEpsilon == 0.0d) {
            fuzzEpsilon = 1.0e-6;
        }

        try {
            SimplexTableau once = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);
            SimplexTableau twice = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);

            if (once.getNumObjectiveFunctions() > 1) {
                once.dropPhase1Objective();
                twice.dropPhase1Objective();

                int hashAfterFirstDrop = twice.hashCode();
                twice.dropPhase1Objective();
                int hashAfterSecondDrop = twice.hashCode();

                // Contract: dropPhase1Objective "Removes the phase 1 objective function ... and the non-basic artificial variables";
                // its implementation also sets numArtificialVariables = 0. After the first successful drop, Phase 1 is gone,
                // so a second call should be a no-op. A patch that merely skips/deletes the real state update violates this.
                if (once.getNumArtificialVariables() != 0 || twice.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:drop-post-state] metamorphic violation: dropPhase1Objective must leave zero artificial variables once=" +
                        once.getNumArtificialVariables() + " twice=" + twice.getNumArtificialVariables());
                }
                if (once.getNumObjectiveFunctions() != 1 || twice.getNumObjectiveFunctions() != 1) {
                    throw new RuntimeException(
                        "[oracle:drop-phase-switch] metamorphic violation: dropPhase1Objective must remove phase 1 objective onceObj=" +
                        once.getNumObjectiveFunctions() + " twiceObj=" + twice.getNumObjectiveFunctions());
                }
                if (hashAfterFirstDrop != hashAfterSecondDrop) {
                    throw new RuntimeException(
                        "[oracle:drop-idempotent-hash] metamorphic violation: second dropPhase1Objective call should be a no-op after phase 1 is already removed inputMaxUlps=" +
                        fuzzMaxUlps + " inputEpsilon=" + fuzzEpsilon + " lhs=" + hashAfterFirstDrop + " rhs=" + hashAfterSecondDrop);
                }
                if (!once.equals(twice) || once.hashCode() != twice.hashCode()) {
                    throw new RuntimeException(
                        "[oracle:drop-idempotent-equals] metamorphic violation: one drop and two drops from identical constructors must agree inputMaxUlps=" +
                        fuzzMaxUlps + " inputEpsilon=" + fuzzEpsilon + " equals=" + once.equals(twice) +
                        " hash1=" + once.hashCode() + " hash2=" + twice.hashCode());
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
## [88] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false actualX0=0.…

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,739 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            org.apache.commons.math3.optimization.PointValuePair solution;
            try {
                SimplexSolver solver = new SimplexSolver();
                solution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double[] p = solution.getPoint();
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d ± 1.0E-6 but actual value=" + solution.getValue());
            }
        }

        {
            SimplexTableau tZero;
            SimplexTableau tPos;
            try {
                int a = data.consumeInt(-5, 5);
                int b = data.consumeInt(-5, 5);
                int c = data.consumeInt(-5, 5);
                LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
                java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

                int maxUlps = 10 + data.consumeInt(0, 10);
                tZero = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);
                tPos = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);

                int start = tZero.getNumObjectiveFunctions();
                int end = tZero.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }

                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tPos.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                double tinyPositive;
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        tinyPositive = Double.MIN_VALUE;
                        break;
                    case 1:
                        tinyPositive = Math.ulp(1.0);
                        break;
                    case 2:
                        tinyPositive = epsilon / 2.0;
                        break;
                    case 3:
                        tinyPositive = epsilon / 4.0;
                        break;
                    default:
                        tinyPositive = Math.scalb(1.0, -1022 + data.consumeInt(0, 32));
                        break;
                }
                if (!(tinyPositive > 0.0) || Double.isNaN(tinyPositive) || Double.isInfinite(tinyPositive)) {
                    return;
                }
                tPos.setEntry(0, chosen, tinyPositive);
            } catch (Throwable t) {
                return;
            }

            int widthZeroBefore = tZero.getWidth();
            int widthPosBefore = tPos.getWidth();
            int heightZeroBefore = tZero.getHeight();
            int heightPosBefore = tPos.getHeight();
            int zeroHashBefore = tZero.hashCode();
            int posHashBefore = tPos.hashCode();

            if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:baseline-dimensions] semantic mismatch: expected identical freshly-constructed tableaux to have same dimensions but zero=("
                        + widthZeroBefore + "x" + heightZeroBefore + ") pos=(" + widthPosBefore + "x" + heightPosBefore + ")");
            }

            try {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            int widthZeroAfter = tZero.getWidth();
            int widthPosAfter = tPos.getWidth();
            int heightZeroAfter = tZero.getHeight();
            int heightPosAfter = tPos.getHeight();
            int zeroHashAfter = tZero.hashCode();
            int posHashAfter = tPos.hashCode();

            // Contract from dropPhase1Objective(): it removes the phase-1 objective row, so any correct implementation must reduce height by exactly one for both tableaux.
            if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:drop-height] semantic mismatch: expected dropPhase1Objective() to remove exactly one row but before zero/pos=("
                        + heightZeroBefore + "," + heightPosBefore + ") after=(" + heightZeroAfter + "," + heightPosAfter + ")");
            }

            // Contract from dropPhase1Objective(): it removes positive cost non-artificial variables. We changed exactly one scanned non-artificial entry from 0 to a tiny >0 value, so the positive tableau must lose exactly one extra column.
            if (widthPosAfter != widthZeroAfter - 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:tiny-positive-drop] semantic mismatch: tiny positive non-artificial column was not additionally dropped; zeroCaseWidth="
                        + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " widthsBefore=(" + widthZeroBefore + "," + widthPosBefore
                        + ") hashesBefore=(" + zeroHashBefore + "," + posHashBefore + ") hashesAfter=(" + zeroHashAfter + "," + posHashAfter + ")");
            }

            // Hidden-state/sibling-agreement check: constructor establishes identical state readers observe; after both objects undergo the same logical mutation (dropPhase1Objective on the zeroed case replicated from construction), equals/hashCode must agree on identical objects. A patch that only tweaks bookkeeping or skips numArtificialVariables updates breaks this even if dimensions look plausible.
            SimplexTableau tZero2;
            try {
                LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] {
                    tZero.getOriginalNumDecisionVariables() > 0 ? 2 : 2,
                    6,
                    7
                }, 0.0);
                java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
                constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
                tZero2 = new SimplexTableau(f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, 10);
                int start2 = tZero2.getNumObjectiveFunctions();
                int end2 = tZero2.getArtificialVariableOffset();
                if (end2 <= start2) {
                    return;
                }
                for (int j = start2; j < end2; j++) {
                    tZero2.setEntry(0, j, 0.0);
                }
                tZero2.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            // Rebuild an exact twin of the canonical zeroed tableau to ensure equals/hashCode still track the shared fields written by construction and mutation.
            SimplexTableau canonicalA;
            SimplexTableau canonicalB;
            try {
                LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
                java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
                canonicalA = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, 10);
                canonicalB = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, 10);
                int start = canonicalA.getNumObjectiveFunctions();
                int end = canonicalA.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }
                for (int j = start; j < end; j++) {
                    canonicalA.setEntry(0, j, 0.0);
                    canonicalB.setEntry(0, j, 0.0);
                }
                canonicalA.dropPhase1Objective();
                canonicalB.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            if (!canonicalA.equals(canonicalB) || canonicalA.hashCode() != canonicalB.hashCode()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:equals-hash-agreement] semantic mismatch: identical tableaux after identical construction and dropPhase1Objective() do not agree; equals="
                        + canonicalA.equals(canonicalB) + " hashA=" + canonicalA.hashCode() + " hashB=" + canonicalB.hashCode());
            }
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,629 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 was false, actual="
                    + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 was false, actual="
                    + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 was false, actual="
                    + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: expected=2.0 actual=" + solution.getValue());
        }

        try {
            ArrayList<LinearConstraint> equivalentConstraints = new ArrayList<LinearConstraint>();
            equivalentConstraints.add(new LinearConstraint(
                new double[] { 1, 2, 1 }, 0.0, Relationship.LEQ, new double[] { 0, 0, 0 }, 2.0));
            equivalentConstraints.add(new LinearConstraint(
                new double[] { -1, 1, 1 }, 0.0, Relationship.LEQ, new double[] { 0, 0, 0 }, -1.0));
            equivalentConstraints.add(new LinearConstraint(
                new double[] { 2, -3, 1 }, 0.0, Relationship.LEQ, new double[] { 0, 0, 0 }, -1.0));

            PointValuePair equivalentSolution =
                new SimplexSolver().optimize(f, equivalentConstraints, GoalType.MAXIMIZE, false);

            double[] p2 = equivalentSolution.getPoint();
            if (!(Math.abs(solution.getValue() - equivalentSolution.getValue()) <= epsilon
                && Precision.compareTo(point[0], 0.0d, epsilon) == Precision.compareTo(p2[0], 0.0d, epsilon)
                && Precision.compareTo(point[1], 0.0d, epsilon) == Precision.compareTo(p2[1], 0.0d, epsilon)
                && Precision.compareTo(point[2], 0.0d, epsilon) == Precision.compareTo(p2[2], 0.0d, epsilon))) {
                throw new RuntimeException(
                    "[oracle:equiv-constraint-ctor] metamorphic violation: equivalent LinearConstraint constructors should define the same optimization problem input=seed-lp lhsValue="
                        + solution.getValue()
                        + " rhsValue="
                        + equivalentSolution.getValue()
                        + " lhsPoint0="
                        + point[0]
                        + " rhsPoint0="
                        + p2[0]
                        + " lhsPoint1="
                        + point[1]
                        + " rhsPoint1="
                        + p2[1]
                        + " lhsPoint2="
                        + point[2]
                        + " rhsPoint2="
                        + p2[2]);
            }
        } catch (Throwable t) {
        }

        try {
            int fuzzMaxUlps = data.consumeInt(1, 100);
            boolean maximize = true;

            List<LinearConstraint> copiedConstraints = new ArrayList<LinearConstraint>(constraints);
            SimplexTableau tableau =
                new SimplexTableau(f, copiedConstraints, GoalType.MAXIMIZE, false, epsilon, fuzzMaxUlps);

            if (tableau.getNumObjectiveFunctions() == 2 && tableau.getNumArtificialVariables() > 0) {
                int widthBefore = tableau.getWidth();
                int heightBefore = tableau.getHeight();

                tableau.dropPhase1Objective();

                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1Objective-post] metamorphic violation: documented post-condition after dropPhase1Objective is that phase-1/artificial bookkeeping is removed, so getNumArtificialVariables() must become 0 input=maxUlps="
                            + fuzzMaxUlps
                            + " lhs="
                            + tableau.getNumArtificialVariables()
                            + " rhs=0");
                }

                int widthAfterFirst = tableau.getWidth();
                int heightAfterFirst = tableau.getHeight();
                int hashAfterFirst = tableau.hashCode();

                tableau.dropPhase1Objective();

                int widthAfterSecond = tableau.getWidth();
                int heightAfterSecond = tableau.getHeight();
                int hashAfterSecond = tableau.hashCode();

                if (widthAfterFirst != widthAfterSecond
                    || heightAfterFirst != heightAfterSecond
                    || hashAfterFirst != hashAfterSecond) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1Objective-idempotent] metamorphic violation: once dropPhase1Objective has removed phase 1, getNumObjectiveFunctions()==1 makes a second call a no-op, so shape and hashCode must stay unchanged input=maxUlps="
                            + fuzzMaxUlps
                            + " beforeWidth="
                            + widthBefore
                            + " beforeHeight="
                            + heightBefore
                            + " afterFirstWidth="
                            + widthAfterFirst
                            + " afterFirstHeight="
                            + heightAfterFirst
                            + " afterSecondWidth="
                            + widthAfterSecond
                            + " afterSecondHeight="
                            + heightAfterSecond
                            + " hashAfterFirst="
                            + hashAfterFirst
                            + " hashAfterSecond="
                            + hashAfterSecond
                            + " maximize="
                            + maximize);
                }
            }
        } catch (Throwable t) {
        }
    }
}
```

</details>

---
## [92] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 was false, actual=0.0

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,991 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkMath781Regression();
        checkDropPhase1ObjectiveStateAndShape(data);
    }

    private static void checkMath781Regression() {
        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;
        final PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        final double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
        }
    }

    private static void checkDropPhase1ObjectiveStateAndShape(FuzzedDataProvider data) {
        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = data.consumeBoolean() ? 1.0e-6 : 1.0e-7;
        final int maxUlps = data.consumeInt(1, 10);

        final SimplexTableau t1;
        final SimplexTableau t2;
        try {
            t1 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            t2 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return;
        }

        final int hc1a = t1.hashCode();
        final int hc1b = t1.hashCode();
        if (hc1a != hc1b) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-stable-before] semantic mismatch: repeated hashCode() on same tableau changed from " + hc1a + " to " + hc1b);
        }

        if (!t1.equals(t2)) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-constructor] semantic mismatch: two tableaux built from identical constructor arguments are not equal");
        }
        final int hc2a = t2.hashCode();
        if (hc1a != hc2a) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-equals-before] semantic mismatch: equal tableaux must have same hashCode but got " + hc1a + " and " + hc2a);
        }

        final int heightBefore = t1.getHeight();
        final int widthBefore = t1.getWidth();
        final int numObjectivesBefore = t1.getNumObjectiveFunctions();
        final int artificialOffset = t1.getArtificialVariableOffset();

        if (numObjectivesBefore <= 1 || artificialOffset <= numObjectivesBefore) {
            return;
        }

        final int chosen = data.consumeInt(numObjectivesBefore, artificialOffset - 1);
        final double injected = epsilon / 2.0;

        try {
            for (int j = numObjectivesBefore; j < artificialOffset; j++) {
                t1.setEntry(0, j, 0.0);
                t2.setEntry(0, j, 0.0);
            }
            t1.setEntry(0, chosen, injected);
        } catch (Throwable t) {
            return;
        }

        final int widthZeroBefore = t2.getWidth();
        final int widthPosBefore = t1.getWidth();
        final int heightZeroBefore = t2.getHeight();
        final int heightPosBefore = t1.getHeight();

        try {
            t2.dropPhase1Objective();
            t1.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        final int heightZeroAfter = t2.getHeight();
        final int heightPosAfter = t1.getHeight();
        final int widthZeroAfter = t2.getWidth();
        final int widthPosAfter = t1.getWidth();

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:baseline-shape] semantic mismatch: identical tableaux diverged before dropPhase1Objective");
        }

        // Contract from dropPhase1Objective body: it removes the phase-1 objective row and at least column 0.
        // A patch that skips the real work or only suppresses failures breaks these observable shape changes.
        if (heightZeroAfter != heightBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-height-zero] semantic mismatch: dropPhase1Objective should remove exactly one row; before=" + heightBefore + " after=" + heightZeroAfter);
        }
        if (heightPosAfter != heightBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-height-pos] semantic mismatch: dropPhase1Objective should remove exactly one row; before=" + heightBefore + " after=" + heightPosAfter);
        }
        if (!(widthZeroAfter <= widthBefore - 1)) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-width-zero] semantic mismatch: dropPhase1Objective should remove at least column 0; before=" + widthBefore + " after=" + widthZeroAfter);
        }
        if (!(widthPosAfter <= widthBefore - 1)) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-width-pos] semantic mismatch: dropPhase1Objective should remove at least column 0; before=" + widthBefore + " after=" + widthPosAfter);
        }

        // Metamorphic relation justified by the method contract: it removes positive-cost non-artificial variables.
        // We constructed two identical valid phase-1 tableaux, then changed exactly one scanned phase-1 cost
        // entry from 0.0 to a known positive value. The positive-entry case must therefore drop one additional column.
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-same-height] semantic mismatch: changing one scanned coefficient should not change row count; zeroCaseHeight=" + heightZeroAfter + " posCaseHeight=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-kept] semantic mismatch: expected tiny positive cost " + injected + " to be treated like zero under epsilon=" + epsilon + " but widths differed zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen);
        }

        final int droppedHc1a = t1.hashCode();
        final int droppedHc1b = t1.hashCode();
        if (droppedHc1a != droppedHc1b) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-stable-after] semantic mismatch: repeated hashCode() after dropPhase1Objective changed from " + droppedHc1a + " to " + droppedHc1b);
        }

        if (!t1.equals(t2)) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-after-drop] semantic mismatch: tableaux with same post-drop observable shape and coefficients are not equal");
        }
        final int droppedHc2 = t2.hashCode();
        if (droppedHc1a != droppedHc2) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-equals-after] semantic mismatch: equal post-drop tableaux must have same hashCode but got " + droppedHc1a + " and " + droppedHc2);
        }
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,684 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double epsilon = 1e-6d;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

            double x0 = solution.getPoint()[0];
            double x1 = solution.getPoint()[1];
            double x2 = solution.getPoint()[2];
            double value = solution.getValue();

            if (!(Precision.compareTo(x0, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected=true actual=false point0=" + x0);
            }
            if (!(Precision.compareTo(x1, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected=true actual=false point1=" + x1);
            }
            if (!(Precision.compareTo(x2, 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected=true actual=false point2=" + x2);
            }
            if (!(Math.abs(value - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected=2.0 actual=" + value);
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) {
            return;
        }

        int fuzzMaxUlps = data.consumeInt(1, 64);
        double fuzzEpsilon = Math.abs(data.consumeInt(-1000, 1000)) / 1000000.0d;
        if (fuzzEpsilon == 0.0d) {
            fuzzEpsilon = 1.0e-6d;
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);

            // Constructor/body invariant visible in the class skeleton:
            // numArtificialVariables is initialized from the normalized constraints count,
            // and getNumArtificialVariables() is the reader for that state.
            // For this fixed problem, two negative-RHS LEQ constraints normalize to GEQ,
            // so the tableau starts Phase 1 with exactly 2 artificial variables.
            if (tableau.getNumArtificialVariables() != 2) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:state-coupling] semantic mismatch: expected initial artificial variable count 2 actual=" + tableau.getNumArtificialVariables());
            }

            tableau.dropPhase1Objective();

            // Contract from the shown method body: it removes the phase 1 objective and then
            // sets numArtificialVariables = 0. A patch that merely skips bookkeeping or makes
            // the branch unreachable would violate this observable state.
            if (tableau.getNumArtificialVariables() != 0) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:drop-post] semantic mismatch: expected numArtificialVariables to be 0 after dropPhase1Objective actual=" + tableau.getNumArtificialVariables());
            }

            double[][] beforeSecondDrop = tableau.getData();
            int beforeHash = tableau.hashCode();
            int beforeHeight = tableau.getHeight();
            int beforeWidth = tableau.getWidth();

            tableau.dropPhase1Objective();

            double[][] afterSecondDrop = tableau.getData();
            int afterHash = tableau.hashCode();
            int afterHeight = tableau.getHeight();
            int afterWidth = tableau.getWidth();

            // Idempotence/metamorphic check justified by the method's guard:
            // once getNumObjectiveFunctions() == 1 after the first drop, a second call returns immediately.
            // Therefore a correct implementation must leave all observable tableau state unchanged.
            if (beforeHash != afterHash) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent-hash] metamorphic violation: second dropPhase1Objective changed hashCode before=" + beforeHash + " after=" + afterHash);
            }
            if (beforeHeight != afterHeight || beforeWidth != afterWidth) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent-shape] metamorphic violation: second dropPhase1Objective changed dimensions beforeHeight=" + beforeHeight + " beforeWidth=" + beforeWidth + " afterHeight=" + afterHeight + " afterWidth=" + afterWidth);
            }
            if (!java.util.Arrays.deepEquals(beforeSecondDrop, afterSecondDrop)) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent-data] metamorphic violation: second dropPhase1Objective changed tableau data lhs=" + java.util.Arrays.deepToString(beforeSecondDrop) + " rhs=" + java.util.Arrays.deepToString(afterSecondDrop));
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            return;
        }

        try {
            int n = data.consumeInt(1, 4);
            double[] coeffs = new double[n];
            for (int i = 0; i < n; i++) {
                coeffs[i] = data.consumeInt(-20, 20);
            }
            LinearObjectiveFunction fuzzF = new LinearObjectiveFunction(coeffs, data.consumeInt(-20, 20));
            ArrayList<LinearConstraint> fuzzConstraints = new ArrayList<LinearConstraint>();
            int m = data.consumeInt(1, 4);
            for (int i = 0; i < m; i++) {
                double[] c = new double[n];
                for (int j = 0; j < n; j++) {
                    c[j] = data.consumeInt(-20, 20);
                }
                int relPick = data.consumeInt(0, 2);
                Relationship rel = relPick == 0 ? Relationship.LEQ : (relPick == 1 ? Relationship.GEQ : Relationship.EQ);
                fuzzConstraints.add(new LinearConstraint(c, rel, data.consumeInt(-20, 20)));
            }
            boolean nonNegative = data.consumeBoolean();
            new SimplexSolver(fuzzEpsilon, fuzzMaxUlps).optimize(fuzzF, fuzzConstraints, GoalType.MAXIMIZE, nonNegative);
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [96] ⚙️ harness-repair · `attempt_013`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [97] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected=true actual=false point0=0.0

---
## [98] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,812 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        boolean fail = false;
        String message = null;

        try {
            // Lifted regression oracle from SimplexSolverTest.testMath781.
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1.0e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            // Trusted because these are copied verbatim from the failing JUnit regression test.
            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0=" + p[0];
            } else if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false x1=" + p[1];
            } else if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                fail = true;
                message = "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false x2=" + p[2];
            } else if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                fail = true;
                message = "[oracle:math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual=" + solution.getValue();
            }
        } catch (Throwable t) {
            return;
        }
        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }

        fail = false;
        message = null;
        try {
            // Metamorphic check for the patched method.
            // Contract from dropPhase1Objective(): it removes "positive cost non-artificial variables".
            // Therefore two otherwise identical phase-1 tableaux that differ only by one scanned phase-1-row
            // non-artificial entry changing from 0.0 to a strictly positive value must differ by exactly one
            // dropped column after the call. A patch that merely skips the removal or uses the wrong threshold
            // breaks this observable width relation even without throwing.
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);

            LinearObjectiveFunction f2 =
                new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double eps2 = 1.0e-6;
            int maxUlps = data.consumeInt(0, 10);
            SimplexTableau tZero =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);
            SimplexTableau tPos =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.tableau.setEntry(0, j, 0.0);
                tPos.tableau.setEntry(0, j, 0.0);
            }

            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.tableau.setEntry(0, chosen, tinyPositive);

            int widthZeroBefore = tZero.getWidth();
            int widthPosBefore = tPos.getWidth();
            int heightZeroBefore = tZero.getHeight();
            int heightPosBefore = tPos.getHeight();

            // Shared-state / sibling-agreement check:
            // equals() and hashCode() read the same constructor-established state (including maxUlps and
            // numArtificialVariables). For equal tableaux, Java's contract requires equal hash codes.
            // This directly checks that the reader methods agree with constructor-written state.
            if (!tZero.equals(tPos)) {
                return;
            }
            int zeroHash1 = tZero.hashCode();
            int zeroHash2 = tZero.hashCode();
            int posHash1 = tPos.hashCode();
            int posHash2 = tPos.hashCode();
            if (zeroHash1 != zeroHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + zeroHash1 + " hash2=" + zeroHash2;
            } else if (posHash1 != posHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + posHash1 + " hash2=" + posHash2;
            } else if (zeroHash1 != posHash1) {
                fail = true;
                message = "[oracle:equals-hashCode-before] metamorphic violation: equal tableaux had different hashCode() values before dropPhase1Objective lhs=" + zeroHash1 + " rhs=" + posHash1;
            }

            if (!fail) {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                int widthZeroAfter = tZero.getWidth();
                int widthPosAfter = tPos.getWidth();
                int heightZeroAfter = tZero.getHeight();
                int heightPosAfter = tPos.getHeight();

                if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-baseline] metamorphic violation: baseline tableaux not identical before probe widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore;
                } else if (heightZeroAfter != heightPosAfter) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-height] metamorphic violation: changing one scanned coefficient should not change removed row count heightZeroAfter=" + heightZeroAfter + " heightPosAfter=" + heightPosAfter;
                } else if (widthPosAfter != widthZeroAfter - 1) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-width] semantic mismatch: tiny positive scanned non-artificial column was not additionally dropped zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + eps2 + " maxUlps=" + maxUlps;
                } else {
                    // After applying the same transformation to two equal-starting tableaux,
                    // reader methods should still agree: equal objects must keep equal hash codes,
                    // and hashCode() itself is a read-only query so repeated reads must be stable.
                    if (!tZero.equals(tPos)) {
                        return;
                    }
                    int zeroAfter1 = tZero.hashCode();
                    int zeroAfter2 = tZero.hashCode();
                    int posAfter1 = tPos.hashCode();
                    int posAfter2 = tPos.hashCode();
                    if (zeroAfter1 != zeroAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + zeroAfter1 + " hash2=" + zeroAfter2;
                    } else if (posAfter1 != posAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + posAfter1 + " hash2=" + posAfter2;
                    } else if (zeroAfter1 != posAfter1) {
                        fail = true;
                        message = "[oracle:equals-hashCode-after] metamorphic violation: equal tableaux had different hashCode() values after dropPhase1Objective lhs=" + zeroAfter1 + " rhs=" + posAfter1;
                    }
                }
            }
        } catch (Throwable t) {
            return;
        }
        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }
    }
}
```

</details>

---
## [99] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [100] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,695 chars, 2 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
**[assistant]**
```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        boolean fail = false;
        String message = null;

        try {
            // Lifted regression oracle from SimplexSolverTest.testMath781.
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1.0e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            // Trusted because these are copied verbatim from the failing JUnit regression test.
            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0=" + p[0];
            } else if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false x1=" + p[1];
            } else if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                fail = true;
                message = "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false x2=" + p[2];
            } else if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                fail = true;
                message = "[oracle:math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual=" + solution.getValue();
            }
        } catch (Throwable t) {
            return;
        }
        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }

        fail = false;
        message = null;
        try {
            // Metamorphic check for the patched method.
            // Contract from dropPhase1Objective(): it removes "positive cost non-artificial variables".
            // Therefore two otherwise identical phase-1 tableaux that differ only by one scanned phase-1-row
            // non-artificial entry changing from 0.0 to a strictly positive value must differ by exactly one
            // dropped column after the call. A patch that merely skips the removal or uses the wrong threshold
            // breaks this observable width relation even without throwing.
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);

            LinearObjectiveFunction f2 =
                new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double eps2 = 1.0e-6;
            int maxUlps = data.consumeInt(0, 10);
            SimplexTableau tZero =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);
            SimplexTableau tPos =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.tableau.setEntry(0, j, 0.0);
                tPos.tableau.setEntry(0, j, 0.0);
            }

            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.tableau.setEntry(0, chosen, tinyPositive);

            int widthZeroBefore = tZero.getWidth();
            int widthPosBefore = tPos.getWidth();
            int heightZeroBefore = tZero.getHeight();
            int heightPosBefore = tPos.getHeight();

            // Shared-state / sibling-agreement check:
            // equals() and hashCode() read the same constructor-established state (including maxUlps and
            // numArtificialVariables). For equal tableaux, Java's contract requires equal hash codes.
            // This directly checks that the reader methods agree with constructor-written state.
            if (!tZero.equals(tPos)) {
                return;
            }
            int zeroHash1 = tZero.hashCode();
            int zeroHash2 = tZero.hashCode();
            int posHash1 = tPos.hashCode();
            int posHash2 = tPos.hashCode();
            if (zeroHash1 != zeroHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + zeroHash1 + " hash2=" + zeroHash2;
            } else if (posHash1 != posHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + posHash1 + " hash2=" + posHash2;
            } else if (zeroHash1 != posHash1) {
                fail = true;
                message = "[oracle:equals-hashCode-before] metamorphic violation: equal tableaux had different hashCode() values before dropPhase1Objective lhs=" + zeroHash1 + " rhs=" + posHash1;
            }

            if (!fail) {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                int widthZeroAfter = tZero.getWidth();
                int widthPosAfter = tPos.getWidth();
                int heightZeroAfter = tZero.getHeight();
                int heightPosAfter = tPos.getHeight();

                if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-baseline] metamorphic violation: baseline tableaux not identical before probe widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore;
                } else if (heightZeroAfter != heightPosAfter) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-height] metamorphic violation: changing one scanned coefficient should not change removed row count heightZeroAfter=" + heightZeroAfter + " heightPosAfter=" + heightPosAfter;
                } else if (widthPosAfter != widthZeroAfter - 1) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-width] semantic mismatch: tiny positive scanned non-artificial column was not additionally dropped zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + eps2 + " maxUlps=" + maxUlps;
                } else {
                    // After applying the same transformation to two equal-starting tableaux,
                    // reader methods should still agree: equal objects must keep equal hash codes,
                    // and hashCode() itself is a read-only query so repeated reads must be stable.
                    if (!tZero.equals(tPos)) {
                        return;
                    }
                    int zeroAfter1 = tZero.hashCode();
                    int zeroAfter2 = tZero.hashCode();
                    int posAfter1 = tPos.hashCode();
                    int posAfter2 = tPos.hashCode();
                    if (zeroAfter1 != zeroAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + zeroAfter1 + " hash2=" + zeroAfter2;
                    } else if (posAfter1 != posAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + posAfter1 + " hash2=" + posAfter2;
                    } else if (zeroAfter1 != posAfter1) {
                        fail = true;
                        message = "[oracle:equals-hashCode-after] metamorphic violation: equal tableaux had different hashCode() values after dropPhase1Objective lhs=" + zeroAfter1 + " rhs=" + posAfter1;
                    }
                }
            }
        } catch (Throwable t) {
            return;
        }
        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/fuzz/attempt_014/FuzzHarness.java:83: error: tableau has private access in SimplexTableau
                tZero.tableau.setEntry(0, j, 0.0);
                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/fuzz/attempt_014/FuzzHarness.java:84: error: tableau has private access in SimplexTableau
                tPos.tableau.setEntry(0, j, 0.0);
                    ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/Math_33_buggy/fuzz/attempt_014/FuzzHarness.java:89: error: tableau has private access in SimplexTableau
            tPos.tableau.setEntry(0, chosen, tinyPositive);
                ^
3 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~8,395 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        boolean fail = false;
        String message = null;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1.0e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0=" + p[0];
            } else if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false x1=" + p[1];
            } else if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                fail = true;
                message = "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false x2=" + p[2];
            } else if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                fail = true;
                message = "[oracle:math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual=" + solution.getValue();
            }
        } catch (Throwable t) {
            return;
        }
        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }

        fail = false;
        message = null;

        try {
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);

            LinearObjectiveFunction f2 =
                new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double eps2 = 1.0e-6;
            int maxUlps = data.consumeInt(0, 10);

            SimplexTableau tZero =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);
            SimplexTableau tPos =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }

            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.setEntry(0, chosen, tinyPositive);

            int widthZeroBefore = tZero.getWidth();
            int widthPosBefore = tPos.getWidth();
            int heightZeroBefore = tZero.getHeight();
            int heightPosBefore = tPos.getHeight();

            if (!tZero.equals(tPos)) {
                return;
            }

            int zeroHash1 = tZero.hashCode();
            int zeroHash2 = tZero.hashCode();
            int posHash1 = tPos.hashCode();
            int posHash2 = tPos.hashCode();

            if (zeroHash1 != zeroHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + zeroHash1 + " hash2=" + zeroHash2;
            } else if (posHash1 != posHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + posHash1 + " hash2=" + posHash2;
            } else if (zeroHash1 != posHash1) {
                fail = true;
                message = "[oracle:equals-hashCode-before] metamorphic violation: equal tableaux had different hashCode() values before dropPhase1Objective lhs=" + zeroHash1 + " rhs=" + posHash1;
            }

            if (!fail) {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                int widthZeroAfter = tZero.getWidth();
                int widthPosAfter = tPos.getWidth();
                int heightZeroAfter = tZero.getHeight();
                int heightPosAfter = tPos.getHeight();

                if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-baseline] metamorphic violation: baseline tableaux not identical before probe widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore;
                } else if (heightZeroAfter != heightPosAfter) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-height] metamorphic violation: changing one scanned coefficient should not change removed row count heightZeroAfter=" + heightZeroAfter + " heightPosAfter=" + heightPosAfter;
                } else if (widthPosAfter != widthZeroAfter - 1) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-width] semantic mismatch: tiny positive scanned non-artificial column was not additionally dropped zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + eps2 + " maxUlps=" + maxUlps;
                } else {
                    if (!tZero.equals(tPos)) {
                        return;
                    }
                    int zeroAfter1 = tZero.hashCode();
                    int zeroAfter2 = tZero.hashCode();
                    int posAfter1 = tPos.hashCode();
                    int posAfter2 = tPos.hashCode();

                    if (zeroAfter1 != zeroAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + zeroAfter1 + " hash2=" + zeroAfter2;
                    } else if (posAfter1 != posAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + posAfter1 + " hash2=" + posAfter2;
                    } else if (zeroAfter1 != posAfter1) {
                        fail = true;
                        message = "[oracle:equals-hashCode-after] metamorphic violation: equal tableaux had different hashCode() values after dropPhase1Objective lhs=" + zeroAfter1 + " rhs=" + posAfter1;
                    }
                }
            }
        } catch (Throwable t) {
            return;
        }

        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }
    }
}
```

</details>

---
## [101] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0=0.0

---
## [102] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,667 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;
        PointValuePair solution;
        try {
            solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but was point0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but was point1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but was point2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but was value=" + solution.getValue());
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            if (tableau.getNumObjectiveFunctions() == 2) {
                int hashBefore = tableau.hashCode();
                tableau.dropPhase1Objective();
                int hashAfter = tableau.hashCode();

                // Contract from dropPhase1Objective body/javadoc: it removes the phase-1 objective and
                // sets numArtificialVariables to 0; a throw-deleting or bookkeeping-breaking patch can
                // leave stale state observable through getNumObjectiveFunctions/getNumArtificialVariables/hashCode.
                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException("[oracle:drop-phase1-artificial] metamorphic violation: dropPhase1Objective must leave zero artificial variables artificial=" + tableau.getNumArtificialVariables());
                }
                if (tableau.getNumObjectiveFunctions() != 1) {
                    throw new RuntimeException("[oracle:drop-phase1-objectivecount] metamorphic violation: dropPhase1Objective must remove the phase-1 objective objectiveFunctions=" + tableau.getNumObjectiveFunctions());
                }
                if (hashAfter != tableau.hashCode()) {
                    throw new RuntimeException("[oracle:hash-stable] metamorphic violation: hashCode should be stable across repeated reads after mutation hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " hashAgain=" + tableau.hashCode());
                }
            }
        } catch (Throwable t) {
            return;
        }

        int scale = data.consumeInt(1, 16);
        LinearObjectiveFunction scaledObjective =
                new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);

        try {
            PointValuePair scaled = new SimplexSolver().optimize(scaledObjective, constraints, GoalType.MAXIMIZE, false);
            double[] scaledPoint = scaled.getPoint();

            // Linear-programming contract: multiplying the objective by a positive scalar preserves the argmax set;
            // therefore for this seed, whose expected maximizer is fixed by the lifted oracle, the same sign pattern
            // must hold and the optimum value must scale by that positive factor.
            if (!(Precision.compareTo(scaledPoint[0], 0.0d, epsilon) > 0
                    && Precision.compareTo(scaledPoint[1], 0.0d, epsilon) > 0
                    && Precision.compareTo(scaledPoint[2], 0.0d, epsilon) < 0)) {
                throw new RuntimeException("[oracle:scaled-objective-signs] metamorphic violation: positive objective scaling must preserve the seed optimizer sign pattern scale=" + scale + " point0=" + scaledPoint[0] + " point1=" + scaledPoint[1] + " point2=" + scaledPoint[2]);
            }

            double expectedScaledValue = 2.0d * scale;
            if (Math.abs(scaled.getValue() - expectedScaledValue) > epsilon) {
                throw new RuntimeException("[oracle:scaled-objective-value] metamorphic violation: positive objective scaling must scale optimum value linearly scale=" + scale + " lhs=" + scaled.getValue() + " rhs=" + expectedScaledValue);
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [103] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but was point0=0.0

---
## [104] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,630 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution;
            try {
                solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x0-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x1-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x2-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
            }

            int scale = data.consumeInt(1, 8);
            LinearObjectiveFunction scaled = new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);
            PointValuePair scaledSolution;
            try {
                scaledSolution = new SimplexSolver().optimize(scaled, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double expectedScaledValue = 2.0d * scale;
            if (!(Math.abs(scaledSolution.getValue() - expectedScaledValue) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:scaled-objective-value] metamorphic violation: scaling the objective by a positive constant must scale the optimum value for the same feasible region; scale=" + scale + " expectedValue=" + expectedScaledValue + " actualValue=" + scaledSolution.getValue());
            }
        }

        {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            int maxUlps = data.consumeInt(1, 32);
            double positiveProbe = epsilon + data.consumeInt(1, 1000) / 1000.0;

            LinearObjectiveFunction f = new LinearObjectiveFunction(
                    new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero;
            SimplexTableau tPos;
            try {
                tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            } catch (Throwable t) {
                return;
            }

            int start;
            int end;
            int widthBeforeZero;
            int widthBeforePos;
            int heightBeforeZero;
            int heightBeforePos;
            int hashBeforeZero;
            int hashBeforePos;
            try {
                start = tZero.getNumObjectiveFunctions();
                end = tZero.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }

                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tPos.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                tPos.setEntry(0, chosen, positiveProbe);

                widthBeforeZero = tZero.getWidth();
                widthBeforePos = tPos.getWidth();
                heightBeforeZero = tZero.getHeight();
                heightBeforePos = tPos.getHeight();
                hashBeforeZero = tZero.hashCode();
                hashBeforePos = tPos.hashCode();
            } catch (Throwable t) {
                return;
            }

            boolean equalsBefore;
            try {
                equalsBefore = tZero.equals(tPos);
            } catch (Throwable t) {
                return;
            }

            int widthAfterZero;
            int widthAfterPos;
            int heightAfterZero;
            int heightAfterPos;
            int hashAfterZero;
            int hashAfterPos;
            boolean equalsAfter;
            int hashAfterZeroSecondRead;
            int hashAfterPosSecondRead;
            try {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                widthAfterZero = tZero.getWidth();
                widthAfterPos = tPos.getWidth();
                heightAfterZero = tZero.getHeight();
                heightAfterPos = tPos.getHeight();
                hashAfterZero = tZero.hashCode();
                hashAfterPos = tPos.hashCode();

                equalsAfter = tZero.equals(tPos);

                hashAfterZeroSecondRead = tZero.hashCode();
                hashAfterPosSecondRead = tPos.hashCode();
            } catch (Throwable t) {
                return;
            }

            if (widthBeforeZero != widthBeforePos || heightBeforeZero != heightBeforePos) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-baseline] semantic mismatch: baseline tableaux should start with identical dimensions but zero=(" + widthBeforeZero + "x" + heightBeforeZero + ") pos=(" + widthBeforePos + "x" + heightBeforePos + ")");
            }

            if (!equalsBefore) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-baseline-equals] semantic mismatch: tableaux differ before the probe despite identical forced state except for an entry not yet changed");
            }

            if (heightAfterZero != heightBeforeZero - 1 || heightAfterPos != heightBeforePos - 1) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-height-drop] metamorphic violation: dropPhase1Objective() must remove the phase-1 objective row; beforeZero=" + heightBeforeZero + " afterZero=" + heightAfterZero + " beforePos=" + heightBeforePos + " afterPos=" + heightAfterPos);
            }

            if (heightAfterZero != heightAfterPos) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-height-agree] semantic mismatch: changing one scanned phase-1 coefficient should not change row count; zeroHeight=" + heightAfterZero + " posHeight=" + heightAfterPos);
            }

            if (widthAfterPos != widthAfterZero - 1) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-positive-column-drop] semantic mismatch: dropPhase1Objective() is documented to remove positive cost non-artificial variables, so adding one strictly positive scanned coefficient must drop exactly one additional column; probe=" + positiveProbe + " zeroWidth=" + widthAfterZero + " posWidth=" + widthAfterPos);
            }

            if (tZero.getNumArtificialVariables() != 0 || tPos.getNumArtificialVariables() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-artificial-reset] semantic mismatch: dropPhase1Objective() must reset numArtificialVariables to 0 but got zeroCase=" + tZero.getNumArtificialVariables() + " posCase=" + tPos.getNumArtificialVariables());
            }

            if (hashAfterZero != hashAfterZeroSecondRead || hashAfterPos != hashAfterPosSecondRead) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly] hidden-state violation: repeated hashCode() reads after dropPhase1Objective() changed the reported value; zeroFirst=" + hashAfterZero + " zeroSecond=" + hashAfterZeroSecondRead + " posFirst=" + hashAfterPos + " posSecond=" + hashAfterPosSecondRead);
            }

            if (equalsAfter && hashAfterZero != hashAfterPos) {
                throw new FuzzerSecurityIssueLow("[oracle:equals-hashcode-agreement] hidden-state violation: equal tableaux must report identical hashCode values after dropPhase1Objective(); hashZero=" + hashAfterZero + " hashPos=" + hashAfterPos);
            }

            if (hashBeforeZero == hashAfterZero && widthBeforeZero != widthAfterZero && tZero.getNumArtificialVariables() == 0) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-state-coupling] semantic mismatch: constructor/dropPhase1Objective changed shared state (tableau shape and numArtificialVariables) but hashCode did not reflect the new state; before=" + hashBeforeZero + " after=" + hashAfterZero);
            }

            if (hashBeforePos == hashAfterPos && widthBeforePos != widthAfterPos && tPos.getNumArtificialVariables() == 0) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-state-coupling-pos] semantic mismatch: constructor/dropPhase1Objective changed shared state (tableau shape and numArtificialVariables) but hashCode did not reflect the new state; before=" + hashBeforePos + " after=" + hashAfterPos);
            }
        }
    }
}
```

</details>

---
## [105] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [106] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,460 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

            double x0 = solution.getPoint()[0];
            double x1 = solution.getPoint()[1];
            double x2 = solution.getPoint()[2];
            double value = solution.getValue();

            if (!(Precision.compareTo(x0, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point0=" + x0);
            }
            if (!(Precision.compareTo(x1, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point1=" + x1);
            }
            if (!(Precision.compareTo(x2, 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point2=" + x2);
            }
            if (!(Math.abs(value - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + value);
            }
        }

        try {
            int exp = data.consumeInt(4, 8);
            double positiveEntry = 1.0d;
            for (int i = 0; i < exp; i++) {
                positiveEntry /= 10.0d;
            }
            double epsilonLarge = positiveEntry * 10.0d;
            double epsilonSmall = positiveEntry / 10.0d;
            int maxUlps = 1;

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1, 1 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 1 }, Relationship.GEQ, 1));
            constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 3));

            SimplexTableau large = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilonLarge, maxUlps);
            SimplexTableau small = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilonSmall, maxUlps);

            if (large.getNumObjectiveFunctions() != 2 || small.getNumObjectiveFunctions() != 2) {
                return;
            }
            if (large.getArtificialVariableOffset() <= large.getNumObjectiveFunctions()) {
                return;
            }

            int testCol = large.getNumObjectiveFunctions();

            large.setEntry(0, testCol, positiveEntry);
            small.setEntry(0, testCol, positiveEntry);

            int beforeLargeWidth = large.getWidth();
            int beforeSmallWidth = small.getWidth();
            if (beforeLargeWidth != beforeSmallWidth) {
                return;
            }

            large.dropPhase1Objective();
            small.dropPhase1Objective();

            /* Contract from dropPhase1Objective body/javadoc: it "removes the phase 1 objective
               function" and ends with "this.numArtificialVariables = 0". A patch that merely
               skips bookkeeping or makes the body ineffective violates these observable post-states. */
            if (large.getNumArtificialVariables() != 0 || small.getNumArtificialVariables() != 0) {
                throw new RuntimeException(
                    "[oracle:drop-poststate] metamorphic violation: dropPhase1Objective must clear artificial variables entry=" +
                    positiveEntry + " largeArtificial=" + large.getNumArtificialVariables() +
                    " smallArtificial=" + small.getNumArtificialVariables());
            }
            if (large.getNumObjectiveFunctions() != 1 || small.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException(
                    "[oracle:drop-poststate-obj] metamorphic violation: dropPhase1Objective must leave phase 2 only entry=" +
                    positiveEntry + " largeObjectives=" + large.getNumObjectiveFunctions() +
                    " smallObjectives=" + small.getNumObjectiveFunctions());
            }

            /* Metamorphic relation justified by the patched code itself plus isOptimal using epsilon:
               with the same positive entry, a larger epsilon should treat the coefficient as zero
               while a smaller epsilon should treat it as positive. Therefore the larger-epsilon
               tableau must retain one more non-artificial column after dropPhase1Objective. */
            int largeWidth = large.getWidth();
            int smallWidth = small.getWidth();
            if (largeWidth != smallWidth + 1) {
                throw new RuntimeException(
                    "[oracle:epsilon-sensitive-width] metamorphic violation: expected larger epsilon to keep exactly one extra column inputEntry=" +
                    positiveEntry + " epsilonLarge=" + epsilonLarge + " epsilonSmall=" + epsilonSmall +
                    " lhs=" + largeWidth + " rhs=" + smallWidth);
            }

            /* Idempotence follows from the guard at method entry: after the first drop,
               getNumObjectiveFunctions()==1, so a second call returns immediately and must
               not change observable state such as hashCode. */
            int largeHash = large.hashCode();
            int smallHash = small.hashCode();
            large.dropPhase1Objective();
            small.dropPhase1Objective();
            int largeHash2 = large.hashCode();
            int smallHash2 = small.hashCode();
            if (largeHash != largeHash2 || smallHash != smallHash2) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent] metamorphic violation: second dropPhase1Objective call should be a no-op inputEntry=" +
                    positiveEntry + " largeHash1=" + largeHash + " largeHash2=" + largeHash2 +
                    " smallHash1=" + smallHash + " smallHash2=" + smallHash2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [107] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point0=0.0

---
## [108] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,357 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        {
            PointValuePair solution;
            try {
                LinearObjectiveFunction f =
                        new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
                ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

                SimplexSolver solver = new SimplexSolver();
                solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
            }
        }

        {
            String violation = null;
            try {
                int a = data.consumeInt(-5, 5);
                int b = data.consumeInt(-5, 5);
                int c = data.consumeInt(-5, 5);

                LinearObjectiveFunction f =
                        new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
                ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

                int maxUlps = 10;
                SimplexTableau tZero =
                        new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                SimplexTableau tPos =
                        new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);

                int start = tZero.getNumObjectiveFunctions();
                int end = tZero.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }

                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tPos.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                double subEpsilonPositive = epsilon / (2.0 + data.consumeInt(0, 8));
                if (!(subEpsilonPositive > 0.0 && subEpsilonPositive < epsilon)) {
                    return;
                }
                tPos.setEntry(0, chosen, subEpsilonPositive);

                int widthZeroBefore = tZero.getWidth();
                int widthPosBefore = tPos.getWidth();
                int heightZeroBefore = tZero.getHeight();
                int heightPosBefore = tPos.getHeight();

                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                int widthZeroAfter = tZero.getWidth();
                int widthPosAfter = tPos.getWidth();
                int heightZeroAfter = tZero.getHeight();
                int heightPosAfter = tPos.getHeight();

                if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                    violation =
                            "[oracle:dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped] metamorphic violation: baseline tableaux not identical before probe"
                                    + " widthZeroBefore=" + widthZeroBefore
                                    + " widthPosBefore=" + widthPosBefore
                                    + " heightZeroBefore=" + heightZeroBefore
                                    + " heightPosBefore=" + heightPosBefore;
                } else if (heightZeroAfter != heightPosAfter) {
                    violation =
                            "[oracle:dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped] metamorphic violation: changing one scanned phase-1 cost entry should not change dropped-row count"
                                    + " heightZeroAfter=" + heightZeroAfter
                                    + " heightPosAfter=" + heightPosAfter;
                } else if (widthZeroAfter != widthPosAfter) {
                    /*
                     * Contract justification: the patched comparison is Precision.compareTo(entry, 0.0, epsilon) > 0.
                     * Therefore a strictly positive entry that is still below epsilon is treated the same as 0 for
                     * this drop decision. A throw-deleting or wrong-bookkeeping patch would leave an observable width
                     * disagreement after dropPhase1Objective().
                     */
                    violation =
                            "[oracle:dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped] metamorphic violation: sub-epsilon positive scanned non-artificial coefficient changed dropped-column set"
                                    + " chosenColumn=" + chosen
                                    + " subEpsilonPositive=" + subEpsilonPositive
                                    + " widthZeroAfter=" + widthZeroAfter
                                    + " widthPosAfter=" + widthPosAfter;
                }

                if (violation == null) {
                    /*
                     * Hidden-state justification: hashCode() is a public no-arg reader; repeated reads on an unchanged
                     * tableau must agree. This catches silent state corruption that may not show up in dimensions alone.
                     */
                    int h1 = tZero.hashCode();
                    int h2 = tZero.hashCode();
                    if (h1 != h2) {
                        violation =
                                "[oracle:hashCode-read-only] metamorphic violation: repeated hashCode() on unchanged tableau disagreed"
                                        + " first=" + h1 + " second=" + h2;
                    }
                }

                if (violation == null) {
                    /*
                     * Sibling/state-coupling justification: equals and hashCode observe shared state (including
                     * numArtificialVariables/maxUlps). Two tableaux built from identical constructor inputs and mutated
                     * identically by dropPhase1Objective() must still agree on both equals() and hashCode().
                     */
                    SimplexTableau aTab =
                            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                    SimplexTableau bTab =
                            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                    aTab.dropPhase1Objective();
                    bTab.dropPhase1Objective();

                    boolean eq = aTab.equals(bTab);
                    int ha = aTab.hashCode();
                    int hb = bTab.hashCode();
                    if (!eq || ha != hb) {
                        violation =
                                "[oracle:equals-hashCode-agreement] semantic mismatch: identical tableaux after identical construction and dropPhase1Objective() must compare equal and share hashCode"
                                        + " equals=" + eq + " hashA=" + ha + " hashB=" + hb;
                    }
                }
            } catch (Throwable t) {
                return;
            }

            if (violation != null) {
                throw new FuzzerSecurityIssueLow(violation);
            }
        }
    }
}
```

</details>

---
## [109] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [110] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,824 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import java.util.ArrayList;
import java.util.Arrays;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static ArrayList<LinearConstraint> math781Constraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static LinearObjectiveFunction math781Objective() {
        return new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
    }

    private static void failLow(String id, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;
        final LinearObjectiveFunction f = math781Objective();
        final ArrayList<LinearConstraint> constraints = math781Constraints();

        PointValuePair solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);

        if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
            failLow("math781-x0-positive",
                    "Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true but was false; actual="
                            + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            failLow("math781-x1-positive",
                    "Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected true but was false; actual="
                            + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            failLow("math781-x2-negative",
                    "Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected true but was false; actual="
                            + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            failLow("math781-value",
                    "Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected 2.0 but was " + solution.getValue());
        }

        SimplexTableau t1 = new SimplexTableau(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false, 1e-6, 10);
        SimplexTableau t2 = new SimplexTableau(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false, 1e-6, 10);

        int h1a = t1.hashCode();
        int h1b = t1.hashCode();
        if (h1a != h1b) {
            throw new RuntimeException("[oracle:hash-stable] metamorphic violation: repeated hashCode() on unchanged tableau must be stable input=Math781 lhs=" + h1a + " rhs=" + h1b);
        }

        t1.dropPhase1Objective();
        t2.dropPhase1Objective();

        /* Contract from dropPhase1Objective body: after dropping phase 1 it sets numArtificialVariables = 0.
           A patch that merely avoids the failing branch or skips bookkeeping would violate this observable post-condition. */
        if (t1.getNumArtificialVariables() != 0) {
            throw new RuntimeException("[oracle:drop-post-numArtificial] metamorphic violation: dropPhase1Objective must leave zero artificial variables input=Math781 lhs=" + t1.getNumArtificialVariables() + " rhs=0");
        }

        /* Contract from class docs/getNumObjectiveFunctions docs: phase 2 has exactly 1 objective row.
           dropPhase1Objective removes the phase 1 objective, so after the call the tableau must report phase 2. */
        if (t1.getNumObjectiveFunctions() != 1) {
            throw new RuntimeException("[oracle:drop-post-numObjectives] metamorphic violation: after dropPhase1Objective tableau must be in phase 2 input=Math781 lhs=" + t1.getNumObjectiveFunctions() + " rhs=1");
        }

        /* equals/hashCode read the same state written by constructor/dropPhase1Objective (numArtificialVariables, tableau-related content).
           Two identical real tableaus transformed the same way must stay equal and keep matching hashCode. */
        if (!t1.equals(t2)) {
            throw new RuntimeException("[oracle:equals-after-same-transform] metamorphic violation: identical tableaus after same dropPhase1Objective call must be equal input=Math781 lhs=false rhs=true");
        }
        if (t1.hashCode() != t2.hashCode()) {
            throw new RuntimeException("[oracle:hash-after-same-transform] metamorphic violation: equal tableaus must have equal hashCode input=Math781 lhs=" + t1.hashCode() + " rhs=" + t2.hashCode());
        }

        final int beforeSecondHash = t1.hashCode();
        final int beforeWidth = t1.getWidth();
        final int beforeHeight = t1.getHeight();
        final double[][] beforeData = t1.getData();
        t1.dropPhase1Objective();

        /* Method body guarantee: if getNumObjectiveFunctions() == 1, dropPhase1Objective returns immediately.
           Therefore once phase 1 has already been dropped, a second call is a no-op; a throw-deleting or state-skipping patch breaks this idempotence. */
        if (t1.hashCode() != beforeSecondHash) {
            throw new RuntimeException("[oracle:drop-idempotent-hash] metamorphic violation: second dropPhase1Objective on phase-2 tableau must be a no-op input=Math781 lhs=" + t1.hashCode() + " rhs=" + beforeSecondHash);
        }
        if (t1.getWidth() != beforeWidth || t1.getHeight() != beforeHeight) {
            throw new RuntimeException("[oracle:drop-idempotent-dims] metamorphic violation: second dropPhase1Objective on phase-2 tableau must preserve dimensions input=Math781 lhs=" + t1.getWidth() + "x" + t1.getHeight() + " rhs=" + beforeWidth + "x" + beforeHeight);
        }
        if (!Arrays.deepEquals(beforeData, t1.getData())) {
            throw new RuntimeException("[oracle:drop-idempotent-data] metamorphic violation: second dropPhase1Objective on phase-2 tableau must preserve tableau data input=Math781 lhs=" + Arrays.deepToString(t1.getData()) + " rhs=" + Arrays.deepToString(beforeData));
        }

        int fuzzMaxUlps = data.consumeInt(1, 1000);
        int fuzzEpsScale = data.consumeInt(0, 9);
        double fuzzEpsilon = Math.pow(10.0, -fuzzEpsScale);

        try {
            SimplexSolver fuzzSolver = new SimplexSolver(fuzzEpsilon, fuzzMaxUlps);
            fuzzSolver.optimize(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false);
        } catch (Throwable ignored) {
            return;
        }

        try {
            SimplexTableau fuzzTableau = new SimplexTableau(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);
            fuzzTableau.dropPhase1Objective();
            int hc = fuzzTableau.hashCode();
            int w = fuzzTableau.getWidth();
            int h = fuzzTableau.getHeight();
            double[][] d = fuzzTableau.getData();
            fuzzTableau.dropPhase1Objective();
            if (fuzzTableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException("[oracle:fuzz-drop-post-numArtificial] metamorphic violation: dropPhase1Objective must leave zero artificial variables input=epsilon:" + fuzzEpsilon + ",maxUlps:" + fuzzMaxUlps + " lhs=" + fuzzTableau.getNumArtificialVariables() + " rhs=0");
            }
            if (fuzzTableau.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException("[oracle:fuzz-drop-post-numObjectives] metamorphic violation: after dropPhase1Objective tableau must be in phase 2 input=epsilon:" + fuzzEpsilon + ",maxUlps:" + fuzzMaxUlps + " lhs=" + fuzzTableau.getNumObjectiveFunctions() + " rhs=1");
            }
            if (fuzzTableau.hashCode() != hc || fuzzTableau.getWidth() != w || fuzzTableau.getHeight() != h || !Arrays.deepEquals(d, fuzzTableau.getData())) {
                throw new RuntimeException("[oracle:fuzz-drop-idempotent] metamorphic violation: second dropPhase1Objective on phase-2 tableau must be a no-op input=epsilon:" + fuzzEpsilon + ",maxUlps:" + fuzzMaxUlps + " lhsHash=" + fuzzTableau.hashCode() + " rhsHash=" + hc + " lhsDims=" + fuzzTableau.getWidth() + "x" + fuzzTableau.getHeight() + " rhsDims=" + w + "x" + h);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [111] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0-positive] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true but was false; actual…

---
## [112] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,715 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            org.apache.commons.math3.optimization.PointValuePair solution =
                    solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);

            double[] p = solution.getPoint();
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d +/- 1.0E-6 but actual value=" + solution.getValue());
            }
        } catch (Throwable t) {
        }

        boolean dropViolation = false;
        String dropMessage = null;
        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            int maxUlps = Math.max(1, data.consumeInt(1, 8));

            LinearObjectiveFunction f2 = new LinearObjectiveFunction(
                    new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero = new SimplexTableau(
                    f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);
            SimplexTableau tTiny = new SimplexTableau(
                    f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end > start) {
                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tTiny.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                double tinyPositive = epsilon / 2.0;
                tTiny.setEntry(0, chosen, tinyPositive);

                int widthZeroBefore = tZero.getWidth();
                int widthTinyBefore = tTiny.getWidth();
                int heightZeroBefore = tZero.getHeight();
                int heightTinyBefore = tTiny.getHeight();

                int zeroHash1 = tZero.hashCode();
                int zeroHash2 = tZero.hashCode();
                int tinyHash1 = tTiny.hashCode();
                int tinyHash2 = tTiny.hashCode();

                if (zeroHash1 != zeroHash2 || tinyHash1 != tinyHash2) {
                    dropViolation = true;
                    dropMessage =
                            "[oracle:hashcode-readonly-before] metamorphic violation: repeated hashCode() on the same tableau changed without mutation zeroHashes="
                                    + zeroHash1 + "/" + zeroHash2 + " tinyHashes=" + tinyHash1 + "/" + tinyHash2;
                } else if (!tZero.equals(tTiny) || !tTiny.equals(tZero) || zeroHash1 != tinyHash1) {
                    dropViolation = true;
                    dropMessage =
                            "[oracle:equal-before-drop] metamorphic violation: tableaux differing only by a phase-1 entry <= epsilon should be observationally equal before dropPhase1Objective because the method compares positivity against epsilon; zeroHash="
                                    + zeroHash1 + " tinyHash=" + tinyHash1 + " start=" + start + " end=" + end
                                    + " chosen=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + epsilon;
                } else {
                    tZero.dropPhase1Objective();
                    tTiny.dropPhase1Objective();

                    int widthZeroAfter = tZero.getWidth();
                    int widthTinyAfter = tTiny.getWidth();
                    int heightZeroAfter = tZero.getHeight();
                    int heightTinyAfter = tTiny.getHeight();

                    int zeroHashAfter1 = tZero.hashCode();
                    int zeroHashAfter2 = tZero.hashCode();
                    int tinyHashAfter1 = tTiny.hashCode();
                    int tinyHashAfter2 = tTiny.hashCode();

                    if (zeroHashAfter1 != zeroHashAfter2 || tinyHashAfter1 != tinyHashAfter2) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:hashcode-readonly-after] metamorphic violation: repeated hashCode() changed after dropPhase1Objective without intervening mutation zeroHashes="
                                        + zeroHashAfter1 + "/" + zeroHashAfter2 + " tinyHashes=" + tinyHashAfter1 + "/" + tinyHashAfter2;
                    } else if (widthZeroBefore != widthTinyBefore || heightZeroBefore != heightTinyBefore) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:baseline-dimensions] metamorphic violation: valid-by-construction twin tableaux were not identical before dropPhase1Objective"
                                        + " zeroBefore=" + widthZeroBefore + "x" + heightZeroBefore
                                        + " tinyBefore=" + widthTinyBefore + "x" + heightTinyBefore;
                    } else if (heightZeroAfter != heightTinyAfter) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:drop-height] metamorphic violation: changing one non-artificial phase-1 coefficient within epsilon should not change removed-row count"
                                        + " zeroAfterHeight=" + heightZeroAfter + " tinyAfterHeight=" + heightTinyAfter;
                    } else if (widthZeroAfter != widthTinyAfter) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:drop-epsilon-boundary] semantic mismatch: dropPhase1Objective must treat a non-artificial phase-1 entry of epsilon/2 as not positive because it uses Precision.compareTo(entry, 0.0, epsilon) > 0; widths diverged zeroAfterWidth="
                                        + widthZeroAfter + " tinyAfterWidth=" + widthTinyAfter
                                        + " chosen=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + epsilon
                                        + " maxUlps=" + maxUlps;
                    } else if (!tZero.equals(tTiny) || !tTiny.equals(tZero) || zeroHashAfter1 != tinyHashAfter1) {
                        // Contract checked: equals/hashCode read the same shared state (including numArtificialVariables and tableau-related state).
                        // If dropPhase1Objective silently drops an extra column or fails to update shared bookkeeping, twin tableaux that should remain equivalent will disagree here.
                        dropViolation = true;
                        dropMessage =
                                "[oracle:state-coupling] metamorphic violation: equal tableaux after equivalent dropPhase1Objective results must agree in equals/hashCode"
                                        + " zeroHashAfter=" + zeroHashAfter1 + " tinyHashAfter=" + tinyHashAfter1
                                        + " zeroAfterWidth=" + widthZeroAfter + " tinyAfterWidth=" + widthTinyAfter
                                        + " zeroAfterHeight=" + heightZeroAfter + " tinyAfterHeight=" + heightTinyAfter;
                    }
                }
            }
        } catch (Throwable t) {
        }
        if (dropViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(dropMessage);
        }

        try {
            int da2 = data.consumeInt(-2, 2);
            int db2 = data.consumeInt(-2, 2);
            int dc2 = data.consumeInt(-2, 2);
            LinearObjectiveFunction f3 = new LinearObjectiveFunction(
                    new double[] { 2 + da2, 6 + db2, 7 + dc2 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints3 = new java.util.ArrayList<LinearConstraint>();
            constraints3.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints3.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints3.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            new SimplexSolver().optimize(f3, constraints3, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
        }
    }
}
```

</details>

---
## [113] ⚙️ harness-repair · `attempt_021`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [114] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [115] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,978 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double EPS = 1e-6;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = buildSeedConstraints();

        PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, EPS) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= EPS)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, buildSeedConstraints(), GoalType.MAXIMIZE, false, EPS, 10);
            int beforeArtificial = tableau.getNumArtificialVariables();
            int beforeHeight = tableau.getHeight();
            int beforeObjectives = tableau.getNumObjectiveFunctions();

            if (beforeArtificial != 2) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-artificial-count] semantic mismatch: expected seed tableau to start with 2 artificial variables after normalization but actual=" + beforeArtificial);
            }

            tableau.dropPhase1Objective();

            // Contract from dropPhase1Objective body/doc: it removes the phase-1 objective row
            // and sets numArtificialVariables to 0. A patch that only suppresses work/throws
            // would leave these observables inconsistent with the constructor-established state.
            if (tableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-artificial-reset] metamorphic violation: dropPhase1Objective must clear artificial variables input=seed beforeArtificial="
                        + beforeArtificial + " afterArtificial=" + tableau.getNumArtificialVariables());
            }
            if (beforeObjectives == 2 && tableau.getHeight() != beforeHeight - 1) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-height] metamorphic violation: dropPhase1Objective must remove exactly one objective row input=seed beforeHeight="
                        + beforeHeight + " afterHeight=" + tableau.getHeight());
            }
        } catch (Throwable t) {
            if (t instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            return;
        }

        int scale = data.consumeInt(1, 8);
        try {
            LinearObjectiveFunction scaledF = new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0);
            PointValuePair scaledSolution =
                new SimplexSolver().optimize(scaledF, buildSeedConstraints(), GoalType.MAXIMIZE, false);

            // For a fixed feasible region, multiplying the objective by a positive scalar preserves
            // the argmax set and scales the optimum value by the same scalar.
            double expectedScaledValue = solution.getValue() * scale;
            double actualScaledValue = scaledSolution.getValue();
            double tol = EPS * Math.max(1.0d, Math.abs(expectedScaledValue));
            if (Math.abs(actualScaledValue - expectedScaledValue) > tol) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling] metamorphic violation: positive scaling of objective should scale optimum value input=scale="
                        + scale + " lhs=" + actualScaledValue + " rhs=" + expectedScaledValue);
            }

            double[] scaledPoint = scaledSolution.getPoint();
            if (!sameSignRelation(point, scaledPoint)) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling-signs] metamorphic violation: positive scaling of objective should preserve the seed sign pattern input=scale="
                        + scale + " lhs=(" + scaledPoint[0] + "," + scaledPoint[1] + "," + scaledPoint[2]
                        + ") rhs=(" + point[0] + "," + point[1] + "," + point[2] + ")");
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static ArrayList<LinearConstraint> buildSeedConstraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static boolean sameSignRelation(double[] base, double[] other) {
        return Precision.compareTo(base[0], 0.0d, EPS) > 0
            && Precision.compareTo(base[1], 0.0d, EPS) > 0
            && Precision.compareTo(base[2], 0.0d, EPS) < 0
            && Precision.compareTo(other[0], 0.0d, EPS) > 0
            && Precision.compareTo(other[1], 0.0d, EPS) > 0
            && Precision.compareTo(other[2], 0.0d, EPS) < 0;
    }
}
```

</details>

---
## [116] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point0=0.0

---
## [117] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,287 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String issue;

        issue = exactMath781Regression();
        if (issue != null) {
            throw new FuzzerSecurityIssueLow(issue);
        }

        issue = tableauStateAndReaderChecks(data);
        if (issue != null) {
            throw new FuzzerSecurityIssueLow(issue);
        }

        issue = tinyPositiveColumnMetamorphic(data);
        if (issue != null) {
            throw new FuzzerSecurityIssueLow(issue);
        }
    }

    private static String exactMath781Regression() {
        final double epsilon = 1e-6;
        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return null;
        }

        final double[] point = solution.getPoint();

        // Lifted verbatim from the trusted regression test.
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            return "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 was false; actual=" + point[0];
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            return "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 was false; actual=" + point[1];
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            return "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 was false; actual=" + point[2];
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            return "[oracle:math781-value] semantic mismatch: expected 2.0d within 1e-6, actual=" + solution.getValue();
        }

        return null;
    }

    private static String tableauStateAndReaderChecks(FuzzedDataProvider data) {
        final double epsilon = 1e-6;
        final int maxUlps = Math.max(1, data.consumeInt(1, 16));

        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);

        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final SimplexTableau t1;
        final SimplexTableau t2;
        try {
            t1 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            t2 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return null;
        }

        // hashCode() is a read-only query; repeating it must not mutate hidden state.
        final int hc1a;
        final int hc1b;
        try {
            hc1a = t1.hashCode();
            hc1b = t1.hashCode();
        } catch (Throwable t) {
            return null;
        }
        if (hc1a != hc1b) {
            return "[oracle:hashcode-readonly] semantic mismatch: repeated hashCode() changed without mutation; first=" + hc1a + " second=" + hc1b;
        }

        // equals/hashCode must agree for equal objects; this checks reader agreement with constructor-established state.
        final boolean eqBefore;
        final int hc2Before;
        try {
            eqBefore = t1.equals(t2);
            hc2Before = t2.hashCode();
        } catch (Throwable t) {
            return null;
        }
        if (!eqBefore) {
            return "[oracle:equals-before-drop] semantic mismatch: identical tableaux constructed from same inputs were not equal";
        }
        if (hc1a != hc2Before) {
            return "[oracle:hashcode-before-drop] semantic mismatch: equal tableaux had different hash codes; left=" + hc1a + " right=" + hc2Before;
        }

        final int artificialBefore;
        final int heightBefore;
        final int objectivesBefore;
        try {
            artificialBefore = t1.getNumArtificialVariables();
            heightBefore = t1.getHeight();
            objectivesBefore = t1.getNumObjectiveFunctions();
            t1.dropPhase1Objective();
            t2.dropPhase1Objective();
        } catch (Throwable t) {
            return null;
        }

        final int artificialAfter;
        final int heightAfter;
        final boolean eqAfter;
        final int hc1After;
        final int hc2After;
        try {
            artificialAfter = t1.getNumArtificialVariables();
            heightAfter = t1.getHeight();
            eqAfter = t1.equals(t2);
            hc1After = t1.hashCode();
            hc2After = t2.hashCode();
        } catch (Throwable t) {
            return null;
        }

        // Contract from the method body: dropPhase1Objective() ends with this.numArtificialVariables = 0.
        // A patch that skips bookkeeping can leave stale shared state even if the matrix looks plausible.
        if (objectivesBefore > 1 && artificialBefore > 0 && artificialAfter != 0) {
            return "[oracle:drop-clears-artificial-count] semantic mismatch: dropPhase1Objective left numArtificialVariables non-zero; before=" + artificialBefore + " after=" + artificialAfter;
        }

        // Contract from the method body: it removes the phase-1 objective row, so height decreases by exactly 1.
        if (objectivesBefore > 1 && heightAfter != heightBefore - 1) {
            return "[oracle:drop-height] semantic mismatch: tableau height after dropPhase1Objective was wrong; before=" + heightBefore + " after=" + heightAfter;
        }

        // Shared-state agreement check: after identical mutations, equals/hashCode must still agree.
        if (!eqAfter) {
            return "[oracle:equals-after-drop] semantic mismatch: identically dropped tableaux were not equal";
        }
        if (hc1After != hc2After) {
            return "[oracle:hashcode-after-drop] semantic mismatch: equal dropped tableaux had different hash codes; left=" + hc1After + " right=" + hc2After;
        }

        return null;
    }

    private static String tinyPositiveColumnMetamorphic(FuzzedDataProvider data) {
        final double epsilon = 1e-6;
        final int maxUlps = Math.max(1, data.consumeInt(1, 16));
        final int a = data.consumeInt(-5, 5);
        final int b = data.consumeInt(-5, 5);
        final int c = data.consumeInt(-5, 5);

        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);

        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final SimplexTableau tZero;
        final SimplexTableau tPos;
        try {
            tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return null;
        }

        final int start;
        final int end;
        try {
            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
        } catch (Throwable t) {
            return null;
        }
        if (end <= start) {
            return null;
        }

        try {
            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.setEntry(0, chosen, tinyPositive);
        } catch (Throwable t) {
            return null;
        }

        final int widthZeroBefore;
        final int widthPosBefore;
        final int heightZeroBefore;
        final int heightPosBefore;
        try {
            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();
            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();
        } catch (Throwable t) {
            return null;
        }

        final int widthZeroAfter;
        final int widthPosAfter;
        final int heightZeroAfter;
        final int heightPosAfter;
        try {
            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
        } catch (Throwable t) {
            return null;
        }

        // Contract from dropPhase1Objective(): it drops "positive cost non-artificial variables".
        // Therefore changing one scanned non-artificial phase-1-row entry from 0 to a strictly positive value
        // must remove exactly one additional column; a patch that silently keeps the column breaks this.
        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            return "[oracle:tiny-positive-baseline] metamorphic violation: baseline tableaux were not identical before probe; zeroWidth=" + widthZeroBefore + " posWidth=" + widthPosBefore + " zeroHeight=" + heightZeroBefore + " posHeight=" + heightPosBefore;
        }
        if (heightZeroAfter != heightPosAfter) {
            return "[oracle:tiny-positive-height] metamorphic violation: changing one scanned coefficient should not change dropped row count; zeroHeightAfter=" + heightZeroAfter + " posHeightAfter=" + heightPosAfter;
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            return "[oracle:tiny-positive-width] metamorphic violation: tiny positive scanned non-artificial column was not additionally dropped; zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter;
        }

        return null;
    }
}
```

</details>

---
## [118] ⚙️ harness-repair · `attempt_023`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [119] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: missing-alarm-id]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['missing-alarm-id'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:unnamed-check] [oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 was false; actual=0.0

---
## [120] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,380 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true actualPoint0=" + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected true actualPoint1=" + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected true actualPoint2=" + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: solution.getValue() expected=2.0 actual=" + solution.getValue());
        }

        try {
            int scaled = data.consumeInt(1, 1000000);
            double eps = scaled / 1.0e9;
            int maxUlps = data.consumeInt(1, 100);
            double plantedPositiveCost = eps / 2.0;

            LinearObjectiveFunction phase1f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
            ArrayList<LinearConstraint> eqConstraints = new ArrayList<LinearConstraint>();
            eqConstraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, 1.0));

            SimplexTableau tableau = new SimplexTableau(
                    phase1f,
                    eqConstraints,
                    GoalType.MAXIMIZE,
                    true,
                    eps,
                    maxUlps);

            if (tableau.getNumObjectiveFunctions() != 2) {
                return;
            }
            if (tableau.getNumArtificialVariables() != 1) {
                return;
            }

            int beforeWidth = tableau.getWidth();
            int beforeHeight = tableau.getHeight();

            int decisionColumn = tableau.getNumObjectiveFunctions();
            tableau.setEntry(0, decisionColumn, plantedPositiveCost);

            if (!(Precision.compareTo(tableau.getEntry(0, decisionColumn), 0.0, eps) == 0)) {
                return;
            }
            if (!(Precision.compareTo(tableau.getEntry(0, decisionColumn), 0.0, maxUlps) > 0)) {
                return;
            }

            Integer artificialBasicRow = tableau.getBasicRow(tableau.getArtificialVariableOffset());
            if (artificialBasicRow == null) {
                return;
            }

            tableau.dropPhase1Objective();

            // Contract from dropPhase1Objective: it removes the phase 1 objective row/column bookkeeping
            // and explicitly sets numArtificialVariables to 0; a patch that only suppresses/fences the bug
            // without performing the intended state update would violate these observable post-conditions.
            int expectedWidthAfterDrop = beforeWidth - 1;
            int expectedHeightAfterDrop = beforeHeight - 1;
            if (tableau.getWidth() != expectedWidthAfterDrop) {
                throw new RuntimeException("[oracle:dropPhase1-width] metamorphic violation: entry constructed with 0 < cost <= epsilon must not be treated as positive-cost for dropping inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " plantedEntry=" + plantedPositiveCost + " beforeWidth=" + beforeWidth + " expectedWidthAfterDrop=" + expectedWidthAfterDrop + " actualWidthAfterDrop=" + tableau.getWidth());
            }
            if (tableau.getHeight() != expectedHeightAfterDrop) {
                throw new RuntimeException("[oracle:dropPhase1-height] metamorphic violation: dropping phase 1 objective removes exactly one row for this constructed tableau inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " beforeHeight=" + beforeHeight + " expectedHeightAfterDrop=" + expectedHeightAfterDrop + " actualHeightAfterDrop=" + tableau.getHeight());
            }
            if (tableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException("[oracle:dropPhase1-artificial] metamorphic violation: dropPhase1Objective must clear artificial variable count inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " actualArtificialVariables=" + tableau.getNumArtificialVariables());
            }
            if (tableau.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException("[oracle:dropPhase1-objectives] metamorphic violation: after dropping phase 1 objective the tableau must be in phase 2 with one objective function inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " actualNumObjectiveFunctions=" + tableau.getNumObjectiveFunctions());
            }

            // Documented by the method body: if getNumObjectiveFunctions() == 1, dropPhase1Objective returns immediately.
            // Therefore a second call is a no-op, and observable read-only state such as hashCode and dimensions must stay unchanged.
            int hashBeforeSecondDrop = tableau.hashCode();
            int widthBeforeSecondDrop = tableau.getWidth();
            int heightBeforeSecondDrop = tableau.getHeight();
            tableau.dropPhase1Objective();
            int hashAfterSecondDrop = tableau.hashCode();
            if (widthBeforeSecondDrop != tableau.getWidth() || heightBeforeSecondDrop != tableau.getHeight() || hashBeforeSecondDrop != hashAfterSecondDrop) {
                throw new RuntimeException("[oracle:dropPhase1-idempotent] metamorphic violation: second dropPhase1Objective call must be a no-op once phase 1 is already removed inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " widthBefore=" + widthBeforeSecondDrop + " widthAfter=" + tableau.getWidth() + " heightBefore=" + heightBeforeSecondDrop + " heightAfter=" + tableau.getHeight() + " hashBefore=" + hashBeforeSecondDrop + " hashAfter=" + hashAfterSecondDrop);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [121] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true actualPoint0=0.0

---
## [122] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,613 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();
        if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue() == 2.0 within 1.0E-6 but actual value=" + solution.getValue());
        }

        SimplexTableau tZero;
        SimplexTableau tPosA;
        SimplexTableau tPosB;
        int start;
        int end;
        int chosen;
        int widthZeroBefore;
        int widthPosBefore;
        int heightZeroBefore;
        int heightPosBefore;
        int hashBeforeA1;
        int hashBeforeA2;
        boolean equalsBefore;
        int hashBeforeB;
        double positiveEntry = epsilon * (2.0 + data.consumeInt(0, 16));
        try {
            tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            tPosA = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            tPosB = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);

            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPosA.setEntry(0, j, 0.0);
                tPosB.setEntry(0, j, 0.0);
            }

            chosen = start + data.consumeInt(0, end - start - 1);
            tPosA.setEntry(0, chosen, positiveEntry);
            tPosB.setEntry(0, chosen, positiveEntry);

            if (tZero.getNumArtificialVariables() <= 0 || tPosA.getNumArtificialVariables() <= 0 || tPosB.getNumArtificialVariables() <= 0) {
                return;
            }

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPosA.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPosA.getHeight();

            hashBeforeA1 = tPosA.hashCode();
            hashBeforeA2 = tPosA.hashCode();
            equalsBefore = tPosA.equals(tPosB);
            hashBeforeB = tPosB.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (hashBeforeA1 != hashBeforeA2) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-readonly-before] semantic mismatch: hashCode changed across repeated read-only calls before mutation: first=" + hashBeforeA1 + " second=" + hashBeforeA2);
        }
        if (!equalsBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-identical-before] semantic mismatch: identical tableaux built by the same constructor were not equal before dropPhase1Objective");
        }
        if (hashBeforeA1 != hashBeforeB) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-identical-before] semantic mismatch: identical tableaux built by the same constructor had different hashCode values before dropPhase1Objective: a=" + hashBeforeA1 + " b=" + hashBeforeB);
        }

        int widthZeroAfter;
        int widthPosAfter;
        int heightZeroAfter;
        int heightPosAfter;
        int hashAfterA1;
        int hashAfterA2;
        int hashAfterB;
        boolean equalsAfter;
        boolean zeroPosDifferent;
        int numArtificialZeroAfter;
        int numArtificialPosAAfter;
        int numArtificialPosBAfter;
        try {
            tZero.dropPhase1Objective();
            tPosA.dropPhase1Objective();
            tPosB.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPosA.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPosA.getHeight();

            hashAfterA1 = tPosA.hashCode();
            hashAfterA2 = tPosA.hashCode();
            hashAfterB = tPosB.hashCode();
            equalsAfter = tPosA.equals(tPosB);
            zeroPosDifferent = !tZero.equals(tPosA);

            numArtificialZeroAfter = tZero.getNumArtificialVariables();
            numArtificialPosAAfter = tPosA.getNumArtificialVariables();
            numArtificialPosBAfter = tPosB.getNumArtificialVariables();
        } catch (Throwable t) {
            return;
        }

        if (hashAfterA1 != hashAfterA2) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-readonly-after] semantic mismatch: hashCode changed across repeated read-only calls after mutation: first=" + hashAfterA1 + " second=" + hashAfterA2);
        }
        if (!equalsAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-identical-after] semantic mismatch: identical tableaux disagreed after both executed dropPhase1Objective");
        }
        if (hashAfterA1 != hashAfterB) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-identical-after] semantic mismatch: identical tableaux had different hashCode values after both executed dropPhase1Objective: a=" + hashAfterA1 + " b=" + hashAfterB);
        }
        if (!zeroPosDifferent) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-distinguishes-positive-column] semantic mismatch: tableau with an extra positive non-artificial phase-1 cost column should differ from the zeroed tableau after dropPhase1Objective");
        }

        // Contract guarantee used here: dropPhase1Objective() removes the phase-1 objective row and "positive cost non-artificial variables",
        // then sets numArtificialVariables = 0. A patch that merely skips the bookkeeping or neuters the drop would violate these observable post-conditions.
        if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-removes-phase1-row] semantic mismatch: expected height to decrease by exactly one after dropPhase1Objective but zeroCase " + heightZeroBefore + "->" + heightZeroAfter + " posCase " + heightPosBefore + "->" + heightPosAfter);
        }
        if (numArtificialZeroAfter != 0 || numArtificialPosAAfter != 0 || numArtificialPosBAfter != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-clears-artificial-count] semantic mismatch: expected numArtificialVariables to become 0 after dropPhase1Objective but got zeroCase=" + numArtificialZeroAfter + " posA=" + numArtificialPosAAfter + " posB=" + numArtificialPosBAfter);
        }

        // Metamorphic relation: with two tableaux identical except one scanned non-artificial phase-1 cost entry changed from 0 to a clearly positive value (> epsilon),
        // the positive-entry tableau must lose exactly one additional column because the method explicitly drops positive cost non-artificial variables.
        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:baseline-identical] semantic mismatch: baseline tableaux were not identical before the metamorphic probe: zeroWidth=" + widthZeroBefore + " posWidth=" + widthPosBefore + " zeroHeight=" + heightZeroBefore + " posHeight=" + heightPosBefore);
        }
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-same-row-count] semantic mismatch: changing one scanned coefficient should not change the number of rows removed: zeroHeightAfter=" + heightZeroAfter + " posHeightAfter=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-extra-positive-column] semantic mismatch: expected exactly one extra dropped column for a positive non-artificial entry; positiveEntry=" + positiveEntry + " chosenColumn=" + chosen + " zeroWidthAfter=" + widthZeroAfter + " posWidthAfter=" + widthPosAfter);
        }

        try {
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(
                    new double[] {
                            data.consumeInt(-8, 8),
                            data.consumeInt(-8, 8),
                            data.consumeInt(-8, 8)
                    },
                    data.consumeInt(-8, 8));
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            new SimplexSolver().optimize(f2, constraints2, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [123] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [124] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,764 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double EPS = 1.0e-6;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        double x0 = solution.getPoint()[0];
        double x1 = solution.getPoint()[1];
        double x2 = solution.getPoint()[2];
        double value = solution.getValue();

        if (!(Precision.compareTo(x0, 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false point0=" + x0);
        }
        if (!(Precision.compareTo(x1, 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false point1=" + x1);
        }
        if (!(Precision.compareTo(x2, 0.0d, EPS) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false point2=" + x2);
        }
        if (!(Math.abs(value - 2.0d) <= EPS)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: solution.getValue() expected=2.0 actual=" + value + " epsilon=" + EPS);
        }

        try {
            int scale = data.consumeInt(1, 8);
            LinearObjectiveFunction scaled = new LinearObjectiveFunction(
                new double[] { 2.0d * scale, 6.0d * scale, 7.0d * scale }, 0.0d);
            PointValuePair scaledSolution = new SimplexSolver().optimize(scaled, constraints, GoalType.MAXIMIZE, false);

            double scaledValue = scaledSolution.getValue();
            double expectedScaledValue = value * scale;

            if (Math.abs(scaledValue - expectedScaledValue) > EPS) {
                throw new RuntimeException(
                    "[oracle:scaled-objective] metamorphic violation: positive scaling of a linear objective preserves the optimum value up to the same scale for the same feasible region inputScale="
                        + scale + " lhs=" + scaledValue + " rhs=" + expectedScaledValue);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, EPS, 10);

            int artificialBefore = tableau.getNumArtificialVariables();
            int hashBefore = tableau.hashCode();

            if (artificialBefore <= 0) {
                return;
            }

            tableau.dropPhase1Objective();

            int artificialAfterFirst = tableau.getNumArtificialVariables();
            int objectivesAfterFirst = tableau.getNumObjectiveFunctions();
            int hashAfterFirst = tableau.hashCode();
            double[][] dataAfterFirst = tableau.getData();

            // Contract from method body/comment: after removing phase 1 objective and artificial vars,
            // the tableau is in phase 2 only, so getNumArtificialVariables() must be 0 and the
            // derived reader getNumObjectiveFunctions() must agree by reporting 1. A "fix" that
            // skips the mutation or deletes bookkeeping breaks this observable post-condition.
            if (artificialAfterFirst != 0) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-post] metamorphic violation: dropPhase1Objective must clear artificial variables before="
                        + artificialBefore + " after=" + artificialAfterFirst + " hashBefore=" + hashBefore + " hashAfter=" + hashAfterFirst);
            }
            if (objectivesAfterFirst != 1) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-reader-agreement] metamorphic violation: after artificial variables are cleared, getNumObjectiveFunctions() must report phase 2 only artificialAfter="
                        + artificialAfterFirst + " objectivesAfter=" + objectivesAfterFirst);
            }

            tableau.dropPhase1Objective();

            int artificialAfterSecond = tableau.getNumArtificialVariables();
            int hashAfterSecond = tableau.hashCode();
            double[][] dataAfterSecond = tableau.getData();

            // Contract from method body: once getNumObjectiveFunctions() == 1, a later call returns
            // immediately. Therefore the second call is idempotent and must not change observable state.
            if (artificialAfterSecond != artificialAfterFirst
                || hashAfterSecond != hashAfterFirst
                || !sameMatrix(dataAfterFirst, dataAfterSecond)) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-idempotent] metamorphic violation: second dropPhase1Objective call must be a no-op artificial1="
                        + artificialAfterFirst + " artificial2=" + artificialAfterSecond
                        + " hash1=" + hashAfterFirst + " hash2=" + hashAfterSecond);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            ArrayList<LinearConstraint> reversed = new ArrayList<LinearConstraint>(constraints);
            Collections.reverse(reversed);
            PointValuePair reversedSolution = new SimplexSolver().optimize(f, reversed, GoalType.MAXIMIZE, false);

            if (Math.abs(reversedSolution.getValue() - value) > EPS) {
                throw new RuntimeException(
                    "[oracle:constraint-order] metamorphic violation: reordering constraints does not change the feasible region, so the optimal value must be unchanged lhs="
                        + reversedSolution.getValue() + " rhs=" + value);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean sameMatrix(double[][] a, double[][] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || b[i] == null || a[i].length != b[i].length) {
                return false;
            }
            for (int j = 0; j < a[i].length; j++) {
                if (Double.doubleToLongBits(a[i][j]) != Double.doubleToLongBits(b[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }
}
```

</details>

---
## [125] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false point0=0.0

---
## [126] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,602 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkMath781Regression();
        checkPhase1ColumnDropRelation(data);
    }

    private static void checkMath781Regression() {
        LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1.0e-6;
        final PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();

        if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0="
                            + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false x1="
                            + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false x2="
                            + p[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual="
                            + solution.getValue());
        }
    }

    private static void checkPhase1ColumnDropRelation(FuzzedDataProvider data) {
        int a = data.consumeInt(-5, 5);
        int b = data.consumeInt(-5, 5);
        int c = data.consumeInt(-5, 5);
        int maxUlps = data.consumeInt(1, 4);
        int ulpShift = data.consumeInt(0, 4);

        LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final SimplexTableau tZero;
        final SimplexTableau tPos;
        try {
            tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6, maxUlps);
            tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6, maxUlps);
        } catch (Throwable t) {
            return;
        }

        final int start;
        final int end;
        try {
            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
        } catch (Throwable t) {
            return;
        }
        if (end <= start) {
            return;
        }

        int chosen = start + data.consumeInt(0, end - start - 1);
        double tinyPositive = Math.scalb(1.0, -1074 + ulpShift);

        try {
            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            tPos.setEntry(0, chosen, tinyPositive);
        } catch (Throwable t) {
            return;
        }

        final int widthZeroBefore;
        final int widthPosBefore;
        final int heightZeroBefore;
        final int heightPosBefore;
        final int hashZeroBefore;
        final int hashPosBefore;
        try {
            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();
            hashZeroBefore = tZero.hashCode();
            hashPosBefore = tPos.hashCode();
        } catch (Throwable t) {
            return;
        }

        final int hashZeroAfterReadOnly;
        final int hashPosAfterReadOnly;
        try {
            /* hashCode() is a pure reader by contract; a read-only call must not mutate hidden state.
               A patch that "fixes" behavior by silent bookkeeping changes during queries would violate this. */
            hashZeroAfterReadOnly = tZero.hashCode();
            hashPosAfterReadOnly = tPos.hashCode();
        } catch (Throwable t) {
            return;
        }
        if (hashZeroBefore != hashZeroAfterReadOnly) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hashcode-readonly-zero] semantic mismatch: read-only hashCode() changed object state beforePhase1Drop before="
                            + hashZeroBefore + " after=" + hashZeroAfterReadOnly);
        }
        if (hashPosBefore != hashPosAfterReadOnly) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hashcode-readonly-pos] semantic mismatch: read-only hashCode() changed object state beforePhase1Drop before="
                            + hashPosBefore + " after=" + hashPosAfterReadOnly);
        }

        try {
            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        final int widthZeroAfter;
        final int widthPosAfter;
        final int heightZeroAfter;
        final int heightPosAfter;
        final int zeroNumArtificialAfter;
        final int posNumArtificialAfter;
        final int hashZeroAfterDrop;
        final int hashPosAfterDrop;
        try {
            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
            zeroNumArtificialAfter = tZero.getNumArtificialVariables();
            posNumArtificialAfter = tPos.getNumArtificialVariables();
            hashZeroAfterDrop = tZero.hashCode();
            hashPosAfterDrop = tPos.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:baseline-tableaux] semantic mismatch: baseline tableaux not identical before probe widthZeroBefore="
                            + widthZeroBefore + " widthPosBefore=" + widthPosBefore
                            + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore);
        }

        /* dropPhase1Objective() explicitly removes the phase-1 objective row, so height must decrease by 1 in both cases. */
        if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:drop-height] semantic mismatch: phase1 objective row not dropped as documented heightZeroBefore="
                            + heightZeroBefore + " heightZeroAfter=" + heightZeroAfter
                            + " heightPosBefore=" + heightPosBefore + " heightPosAfter=" + heightPosAfter);
        }

        /* dropPhase1Objective() sets numArtificialVariables = 0; hashCode() reads that shared state.
           This checks the writer and reader agree after mutation, catching silent no-op/guard patches. */
        if (zeroNumArtificialAfter != 0 || posNumArtificialAfter != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:numArtificial-zeroed] semantic mismatch: dropPhase1Objective should zero artificial variables zeroCase="
                            + zeroNumArtificialAfter + " posCase=" + posNumArtificialAfter);
        }

        /* The method contract says it removes positive cost non-artificial variables.
           Therefore changing exactly one scanned non-artificial phase-1 cost from 0 to a strictly positive value
           must remove exactly one additional column, while all rows removed stay the same. */
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:drop-same-rows] metamorphic violation: changing one scanned coefficient should not change number of removed rows heightZeroAfter="
                            + heightZeroAfter + " heightPosAfter=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:tiny-positive-column-drop] metamorphic violation: tiny positive scanned non-artificial column was not additionally dropped chosen="
                            + chosen + " tinyPositive=" + tinyPositive + " maxUlps=" + maxUlps
                            + " widthZeroAfter=" + widthZeroAfter + " widthPosAfter=" + widthPosAfter);
        }

        /* Two identical transformations applied to equal-in-shared-state objects should leave shared-state readers stable and consistent. */
        if (tZero.getNumArtificialVariables() == tPos.getNumArtificialVariables()
                && widthZeroAfter == widthPosAfter
                && heightZeroAfter == heightPosAfter
                && hashZeroAfterDrop != hashPosAfterDrop) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-hash-agreement] semantic mismatch: equal observable post-drop state disagrees in hashCode zeroHash="
                            + hashZeroAfterDrop + " posHash=" + hashPosAfterDrop);
        }
    }
}
```

</details>

---
## [127] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0=0.0

---
## [128] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,263 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static ArrayList<LinearConstraint> makeMath781Constraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static LinearObjectiveFunction makeMath781Objective() {
        return new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
    }

    private static void checkMath781Oracle(PointValuePair solution, double epsilon) {
        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected=true actual=false actualValue=" + point[0]
            );
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected=true actual=false actualValue=" + point[1]
            );
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected=true actual=false actualValue=" + point[2]
            );
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected=2.0 actual=" + solution.getValue()
            );
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = makeMath781Objective();
        ArrayList<LinearConstraint> constraints = makeMath781Constraints();

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            checkMath781Oracle(solution, epsilon);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Contract used: dropPhase1Objective "Removes the phase 1 objective function,
            // positive cost non-artificial variables, and the non-basic artificial variables"
            // and the method body sets this.numArtificialVariables = 0. A patch that merely
            // changes reachability or drops bookkeeping can leave stale shared state even if
            // the solver appears to return a point, so we assert the reader agrees after the call.
            SimplexTableau tableau = new SimplexTableau(
                f,
                makeMath781Constraints(),
                GoalType.MAXIMIZE,
                false,
                epsilon,
                10
            );

            if (tableau.getNumObjectiveFunctions() > 1 && tableau.getNumArtificialVariables() > 0) {
                tableau.dropPhase1Objective();
                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:drop-phase1-artificial] metamorphic violation: dropPhase1Objective must clear artificial-variable count input=math781 beforeCount>0 afterCount=" + tableau.getNumArtificialVariables()
                    );
                }

                int hashAfterFirstDrop = tableau.hashCode();
                tableau.dropPhase1Objective();
                int hashAfterSecondDrop = tableau.hashCode();
                if (hashAfterFirstDrop != hashAfterSecondDrop) {
                    throw new RuntimeException(
                        "[oracle:drop-phase1-idempotent] metamorphic violation: second dropPhase1Objective call should be a no-op once getNumObjectiveFunctions()==1 lhs=" + hashAfterFirstDrop + " rhs=" + hashAfterSecondDrop
                    );
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Contract used: optimize takes a Collection<LinearConstraint>; the mathematical
            // problem is defined by the set of constraints, not their iteration order.
            // Reordering the same constraints is therefore an equivalent input that must
            // produce the same optimum and satisfy the same lifted seed assertions.
            ArrayList<LinearConstraint> permuted = makeMath781Constraints();
            int mode = data.consumeInt(0, 5);
            if (mode == 1) {
                LinearConstraint tmp = permuted.get(0);
                permuted.set(0, permuted.get(1));
                permuted.set(1, tmp);
            } else if (mode == 2) {
                LinearConstraint tmp = permuted.get(1);
                permuted.set(1, permuted.get(2));
                permuted.set(2, tmp);
            } else if (mode == 3) {
                LinearConstraint tmp = permuted.get(0);
                permuted.set(0, permuted.get(2));
                permuted.set(2, tmp);
            } else if (mode == 4) {
                LinearConstraint a = permuted.get(0);
                LinearConstraint b = permuted.get(1);
                LinearConstraint c = permuted.get(2);
                permuted.set(0, b);
                permuted.set(1, c);
                permuted.set(2, a);
            } else if (mode == 5) {
                LinearConstraint a = permuted.get(0);
                LinearConstraint b = permuted.get(1);
                LinearConstraint c = permuted.get(2);
                permuted.set(0, c);
                permuted.set(1, a);
                permuted.set(2, b);
            }

            PointValuePair base = new SimplexSolver().optimize(
                makeMath781Objective(),
                makeMath781Constraints(),
                GoalType.MAXIMIZE,
                false
            );
            PointValuePair reordered = new SimplexSolver().optimize(
                makeMath781Objective(),
                permuted,
                GoalType.MAXIMIZE,
                false
            );

            checkMath781Oracle(reordered, epsilon);

            if (Math.abs(base.getValue() - reordered.getValue()) > epsilon) {
                throw new RuntimeException(
                    "[oracle:constraint-order-value] metamorphic violation: reordering equivalent constraint collection changed optimum inputMode=" + mode + " lhs=" + base.getValue() + " rhs=" + reordered.getValue()
                );
            }

            double[] basePoint = base.getPoint();
            double[] reorderedPoint = reordered.getPoint();
            if (basePoint.length == reorderedPoint.length) {
                for (int i = 0; i < basePoint.length; i++) {
                    if (Math.abs(basePoint[i] - reorderedPoint[i]) > epsilon) {
                        throw new RuntimeException(
                            "[oracle:constraint-order-point] metamorphic violation: reordering equivalent constraint collection changed solution coordinate inputMode=" + mode + " index=" + i + " lhs=" + basePoint[i] + " rhs=" + reorderedPoint[i]
                        );
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [129] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected=true actual=false actualValue=0…

---
## [130] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [67] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,331 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import java.util.ArrayList;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double regressionEpsilon = 1.0e-6;

        LinearObjectiveFunction f;
        ArrayList<LinearConstraint> constraints;
        SimplexSolver solver;
        PointValuePair solution;
        try {
            f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();
        if (!(Precision.compareTo(p[0], 0.0d, regressionEpsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, regressionEpsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, regressionEpsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= regressionEpsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected value=2.0 actual=" + solution.getValue());
        }

        int maxUlps = Math.abs(data.consumeInt(-8, 8));
        double epsilon = 0.0;
        int tweakA = data.consumeInt(-3, 3);
        int tweakB = data.consumeInt(-3, 3);
        int tweakC = data.consumeInt(-3, 3);
        double tinyPositive = Math.scalb(1.0, -20 - data.consumeInt(0, 20));

        SimplexTableau tZero;
        SimplexTableau tPos;
        SimplexTableau tTwinA;
        SimplexTableau tTwinB;
        int start;
        int end;
        int chosen;
        int widthZeroBefore;
        int widthPosBefore;
        int heightZeroBefore;
        int heightPosBefore;
        int widthZeroAfter;
        int widthPosAfter;
        int heightZeroAfter;
        int heightPosAfter;
        int numArtificialZeroBefore;
        int numArtificialPosBefore;
        int numArtificialZeroAfter;
        int numArtificialPosAfter;
        int hashRead1;
        int hashRead2;
        int hashAfter1;
        int hashAfter2;
        int twinHashBeforeA;
        int twinHashBeforeB;
        int twinHashAfterA;
        int twinHashAfterB;
        boolean twinsEqualBefore;
        boolean twinsEqualAfter;
        try {
            LinearObjectiveFunction tf = new LinearObjectiveFunction(
                new double[] { 2 + tweakA, 6 + tweakB, 7 + tweakC }, 0.0);
            ArrayList<LinearConstraint> tConstraints = new ArrayList<LinearConstraint>();
            tConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            tConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            tConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            tZero = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tPos = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tTwinA = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tTwinB = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);

            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            chosen = start + data.consumeInt(0, end - start - 1);
            tPos.setEntry(0, chosen, tinyPositive);

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();
            numArtificialZeroBefore = tZero.getNumArtificialVariables();
            numArtificialPosBefore = tPos.getNumArtificialVariables();

            hashRead1 = tZero.hashCode();
            hashRead2 = tZero.hashCode();

            twinsEqualBefore = tTwinA.equals(tTwinB);
            twinHashBeforeA = tTwinA.hashCode();
            twinHashBeforeB = tTwinB.hashCode();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();
            tTwinA.dropPhase1Objective();
            tTwinB.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
            numArtificialZeroAfter = tZero.getNumArtificialVariables();
            numArtificialPosAfter = tPos.getNumArtificialVariables();

            hashAfter1 = tZero.hashCode();
            hashAfter2 = tZero.hashCode();

            twinsEqualAfter = tTwinA.equals(tTwinB);
            twinHashAfterA = tTwinA.hashCode();
            twinHashAfterB = tTwinB.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (hashRead1 != hashRead2) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-before] metamorphic violation: repeated hashCode() on unchanged tableau disagreed before dropPhase1Objective: first=" + hashRead1 + " second=" + hashRead2);
        }

        if (hashAfter1 != hashAfter2) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-after] metamorphic violation: repeated hashCode() on unchanged tableau disagreed after dropPhase1Objective: first=" + hashAfter1 + " second=" + hashAfter2);
        }

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore || numArtificialZeroBefore != numArtificialPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-baseline] metamorphic violation: baseline tableaux should be identical before perturbation but widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore + " artZeroBefore=" + numArtificialZeroBefore + " artPosBefore=" + numArtificialPosBefore);
        }

        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-drop] relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + epsilon + " maxUlps=" + maxUlps);
        }

        if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1 || heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-height] metamorphic violation: dropPhase1Objective() removes the phase-1 objective row, so height must decrease by exactly one for both tableaux; beforeZero=" + heightZeroBefore + " afterZero=" + heightZeroAfter + " beforePos=" + heightPosBefore + " afterPos=" + heightPosAfter);
        }

        if (numArtificialZeroAfter != 0 || numArtificialPosAfter != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-artificial-count] metamorphic violation: dropPhase1Objective() sets numArtificialVariables to 0; observed zeroCase=" + numArtificialZeroAfter + " posCase=" + numArtificialPosAfter);
        }

        if (!twinsEqualBefore || twinHashBeforeA != twinHashBeforeB) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-hash-before] metamorphic violation: identical tableaux built by the same constructor should agree in equals/hashCode before mutation; equals=" + twinsEqualBefore + " hashA=" + twinHashBeforeA + " hashB=" + twinHashBeforeB);
        }

        if (!twinsEqualAfter || twinHashAfterA != twinHashAfterB) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-hash-after] metamorphic violation: identical tableaux subjected to the same dropPhase1Objective() call should still agree in equals/hashCode; equals=" + twinsEqualAfter + " hashA=" + twinHashAfterA + " hashB=" + twinHashAfterB);
        }
    }
}
```

</details>

---
## [131] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=0.0

---
## [132] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [67] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,056 chars)</summary>

```
package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false value=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false value=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false value=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: solution.getValue() expected=2.0 actual=" + solution.getValue() + " epsilon=" + epsilon);
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            int beforeArtificial = tableau.getNumArtificialVariables();
            if (beforeArtificial > 0) {
                int beforeObjectives = tableau.getNumObjectiveFunctions();
                tableau.dropPhase1Objective();

                // Contract from the method body and class docs: dropPhase1Objective removes the phase 1 objective
                // and artificial variables from the tableau, then sets numArtificialVariables = 0.
                // A "fix" that only skips/masks the failing branch or deletes bookkeeping would violate these
                // observable readers even if optimize() stopped throwing.
                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1-artificial] metamorphic violation: after dropPhase1Objective, getNumArtificialVariables() must be 0 beforeArtificial=" +
                        beforeArtificial + " afterArtificial=" + tableau.getNumArtificialVariables());
                }
                if (beforeObjectives != 2 || tableau.getNumObjectiveFunctions() != 1) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1-objectives] metamorphic violation: phase-1 tableau must become phase-2 tableau after dropPhase1Objective beforeObjectives=" +
                        beforeObjectives + " afterObjectives=" + tableau.getNumObjectiveFunctions());
                }
            }
        } catch (Throwable t) {
            return;
        }

        int scale = data.consumeInt(1, 8);
        try {
            LinearObjectiveFunction scaledF = new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);
            SimplexSolver solver2 = new SimplexSolver();
            PointValuePair scaledSolution = solver2.optimize(scaledF, constraints, GoalType.MAXIMIZE, false);
            double[] scaledPoint = scaledSolution.getPoint();

            // Metamorphic relation: multiplying the objective by a positive constant preserves the argmax
            // over the same feasible region, and scales the optimal objective value by that constant.
            // Both sides are computed through the real public API.
            if (Math.abs(scaledPoint[0] - point[0]) > epsilon ||
                Math.abs(scaledPoint[1] - point[1]) > epsilon ||
                Math.abs(scaledPoint[2] - point[2]) > epsilon) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling-point] metamorphic violation: positive objective scaling should preserve optimal point scale=" +
                    scale + " basePoint=[" + point[0] + "," + point[1] + "," + point[2] + "] scaledPoint=[" +
                    scaledPoint[0] + "," + scaledPoint[1] + "," + scaledPoint[2] + "]");
            }
            if (Math.abs(scaledSolution.getValue() - (solution.getValue() * scale)) > epsilon) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling-value] metamorphic violation: positive objective scaling should scale optimal value scale=" +
                    scale + " baseValue=" + solution.getValue() + " scaledValue=" + scaledSolution.getValue() +
                    " expectedScaledValue=" + (solution.getValue() * scale));
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [133] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false value=0.0

---
## [134] ⚙️ coverage · `jazzer coverage dumps`
**output:** **28/28 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov; 28 raw fuzz log(s)**
<details><summary>▸ detail (~15,838 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 1800}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 1792}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 1796}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 1790}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 1797}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 1793}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 1794}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 1788}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 1796}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 1790}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 1793}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 1792}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 1794}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 1788}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 1795}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 1791}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 1793}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 1793}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 1795}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 1792}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 1796}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 1790}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 1795}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 1792}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 1794}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 1794}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 1794}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 1789}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/008_patch1-Math-33-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
