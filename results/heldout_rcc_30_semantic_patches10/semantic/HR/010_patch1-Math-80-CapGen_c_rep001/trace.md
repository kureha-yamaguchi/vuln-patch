# Pipeline trace — Math-80

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
+++ /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
@@ -1132,7 +1132,7 @@ public class EigenDecompositionImpl implements EigenDecomposition {
     private boolean flipIfWarranted(final int n, final int step) {
         if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
             // flip array
-            int j = 4 * n - 1;
+            int j = 4 * (n - 1);
             for (int i = 0; i < j; i += 4) {
                 for (int k = 0; k < 4; k += step) {
                     final double tmp = work[i + k];
```

141 sequential steps — 35 LLM calls, 106 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.linear.EigenDecompositionImplTest', test_method='tes (~3,460 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.linear.EigenDecompositionImplTest', test_method='testMathpbx02', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/src/test/java/org/apache/commons/math/linear/EigenDecompositionImplTest.java', method_source='    public void testMathpbx02() {\\n\\n        double[] mainTridiagonal = {\\n        \\t  7484.860960227216, 18405.28129035345, 13855.225609560746,\\n        \\t 10016.708722343366, 559.8117399576674, 6750.190788301587, \\n        \\t    71.21428769782159\\n        };\\n        double[] secondaryTridiagonal = {\\n        \\t -4175.088570476366,1975.7955858241994,5193.178422374075, \\n        \\t  1995.286659169179,75.34535882933804,-234.0808002076056\\n        };\\n\\n        // the reference values have been computed using routine DSTEMR\\n        // from the fortran library LAPACK version 3.2.1\\n        double[] refEigenValues = {\\n        \\t\\t20654.744890306974412,16828.208208485466457,\\n        \\t\\t6893.155912634994820,6757.083016675340332,\\n        \\t\\t5887.799885688558788,64.309089923240379,\\n        \\t\\t57.992628792736340\\n        };\\n        RealVector[] refEigenVectors = {\\n        \\t\\tnew ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),\\n        \\t\\tnew ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),\\n        \\t\\tnew ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),\\n        \\t\\tnew ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),\\n        \\t\\tnew ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),\\n        \\t\\tnew ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),\\n        \\t\\tnew ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})\\n        };\\n\\n        // the following line triggers the exception\\n        EigenDecomposition decomposition =\\n            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);\\n\\n        double[] eigenValues = decomposition.getRealEigenvalues();\\n        for (int i = 0; i < refEigenValues.length; ++i) {\\n            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);\\n            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {\\n                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);\\n            } else {\\n                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);\\n            }\\n        }\\n\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math.linear.EigenDecompositionImplTest::testMathpbx02",
    "failure_message": "--- org.apache.commons.math.linear.EigenDecompositionImplTest::testMathpbx02\njunit.framework.AssertionFailedError: expected:<16828.208208485466> but was:<20654.74511575945>",
    "support_chars": 359
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~9,556 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java\n+++ /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java\n@@ -1132,7 +1132,7 @@ public class EigenDecompositionImpl implements EigenDecomposition {\n     private boolean flipIfWarranted(final int n, final int step) {\n         if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {\n             // flip array\n-            int j = 4 * n - 1;\n+            int j = 4 * (n - 1);\n             for (int i = 0; i < j; i += 4) {\n                 for (int k = 0; k < 4; k += step) {\n                     final double tmp = work[i + k];\n",
  "functions": [
    {
      "func_name": "flipIfWarranted",
      "func_signature": "private boolean flipIfWarranted(int n, int step)",
      "func_source": "    private boolean flipIfWarranted(final int n, final int step) {\n        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {\n            // flip array\n            int j = 4 * n - 1;\n            for (int i = 0; i < j; i += 4) {\n                for (int k = 0; k < 4; k += step) {\n                    final double tmp = work[i + k];\n                    work[i + k] = work[j - k];\n                    work[j - k] = tmp;\n                }\n                j -= 4;\n            }\n            return true;\n        }\n        return false;\n    }",
      "func_class": "EigenDecompositionImpl",
      "func_class_fq": "org.apache.commons.math.linear.EigenDecompositionImpl",
      "func_param_types": [
        "int",
        "int"
      ],
      "fi_name": "[org.apache.commons.math.linear.EigenDecompositionImpl].flipIfWarranted(int,int)",
      "overload_types": [
        [
          "int",
          "int"
        ]
      ],
      "xrefs": [
        "private void processGeneralBlock(final int n)\n        throws InvalidMatrixException {\n\n        // check decomposed matrix data range\n        double sumOffDiag = 0;\n        for (int i = 0; i < n - 1; ++i) {\n            final int fourI = 4 * i;\n            final double ei = work[fourI + 2];\n            sumOffDiag += ei;\n        }\n\n        if (sumOffDiag == 0) {\n            // matrix is already diagonal\n            return;\n        }\n\n        // initial checks for splits (see Parlett & Marques section 3.3)\n        flipIfWarranted(n, 2);\n\n        // two iterations with Li's test for initial splits\n        initialSplits(n);\n\n        // initialize parameters used by goodStep\n        tType = 0;\n        dMin1 = 0;\n        dMin2 = 0;\n        dN    = 0;\n        dN1   = 0;\n        dN2   = 0;\n        tau   = 0;\n\n        // process split segments\n        int i0 = 0;\n        int n0 = n;\n        while (n0 > 0) {\n\n            // retrieve shift that was temporarily stored as a negative off-diagonal element\n            sigma    = (n0 == n) ? 0 : -work[4 * n0 - 2];\n            sigmaLow = 0;\n\n            // find start of a new split segment to process\n            double offDiagMin = (i0 == n0) ? 0 : work[4 * n0 - 6];\n            double offDiagMax = 0;\n            double diagMax    = work[4 * n0 - 4];\n            double diagMin    = diagMax;\n            i0 = 0;\n            for (int i = 4 * (n0 - 2); i >= 0; i -= 4) {\n                if (work[i + 2] <= 0) {\n                    i0 = 1 + i / 4;\n                    break;\n                }\n                if (diagMin >= 4 * offDiagMax) {\n                    diagMin    = Math.min(diagMin, work[i + 4]);\n                    offDiagMax = Math.max(offDiagMax, work[i + 2]);\n                }\n                diagMax    = Math.max(diagMax, work[i] + work[i + 2]);\n                offDiagMin = Math.min(offDiagMin, work[i + 2]);\n            }\n            work[4 * n0 - 2] = offDiagMin;\n\n            // lower bound of Gershgorin disk\n            dMin = -Math.max(0, diagMin - 2 * Math.sqrt(diagMin * offDiagMax));\n\n            pingPong = 0;\n            int maxIter = 30 * (n0 - i0);\n            for (int k = 0; i0 < n0; ++k) {\n                if (k >= maxIter) {\n                    throw new InvalidMatrixException(new MaxIterationsExceededException(maxIter));\n                }\n\n                // perform one step\n                n0 = goodStep(i0, n0);\n                pingPong = 1 - pingPong;\n\n                // check for new splits after \"ping\" steps\n                // when the last elements of qd array are very small\n                if ((pingPong == 0) && (n0 - i0 > 3) &&\n                    (work[4 * n0 - 1] <= TOLERANCE_2 * diagMax) &&\n                    (work[4 * n0 - 2] <= TOLERANCE_2 * sigma)) {\n                    int split  = i0 - 1;\n                    diagMax    = work[4 * i0];\n                    offDiagMin = work[4 * i0 + 2];\n                    double previousEMin = work[4 * i0 + 3];\n                    for (int i = 4 * i0; i < 4 * n0 - 16; i += 4) {\n                        if ((work[i + 3] <= TOLERANCE_2 * work[i]) ||\n                            (work[i + 2] <= TOLERANCE_2 * sigma)) {\n                            // insert a split\n                            work[i + 2]  = -sigma;\n                            split        = i / 4;\n                            diagMax      = 0;\n                            offDiagMin   = work[i + 6];\n                            previousEMin = work[i + 7];\n                        } else {\n                            diagMax      = Math.max(diagMax, work[i + 4]);\n                            offDiagMin   = Math.min(offDiagMin, work[i + 2]);\n                            previousEMin = Math.min(previousEMin, work[i + 3]);\n                        }\n                    }\n                    work[4 * n0 - 2] = offDiagMin;\n                    work[4 * n0 - 1] = previousEMin;\n                    i0 = split + 1;\n                }\n            }\n\n        }\n\n    }"
      ],
      "xref_names": [
        "[org.apache.commons.math.linear.EigenDecompositionImpl].processGeneralBlock(int)"
      ],
      "reachable_edges": [],
      "reachable_depth": {
        "[org.apache.commons.math.linear.EigenDecompositionImpl].flipIfWarranted(int,int)": 0
      },
      "reachable": [
        "[org.apache.commons.math.linear.EigenDecompositionImpl].flipIfWarranted(int,int)"
      ],
      "related_callees": [],
      "field_siblings": [
        {
          "name": "processGeneralBlock",
          "signature": "private void processGeneralBlock(int n) throws InvalidMatrixException",
          "shared_fields": [
            "pingPong",
            "work"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Find realEigenvalues using dqd/dqds algorithms. <p>This implementation is based on Beresford N. Parlett and Osni A. Marques paper <a href=\"http://www.netlib.org/lapack/lawnspdf/lawn155.pdf\">An Implementation of the dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routine DLASQ2.</p> @param n number of…"
        },
        {
          "name": "initialSplits",
          "signature": "private void initialSplits(int n)",
          "shared_fields": [
            "pingPong",
            "work"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Perform two iterations with Li's tests for initial splits. @param n number of rows of the matrix to process"
        },
        {
          "name": "goodStep",
          "signature": "private int goodStep(int start, int end)",
          "shared_fields": [
            "pingPong",
            "work"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Perform one \"good\" dqd/dqds step. <p>This implementation is based on Beresford N. Parlett and Osni A. Marques paper <a href=\"http://www.netlib.org/lapack/lawnspdf/lawn155.pdf\">An Implementation of the dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routine DLAZQ3.</p> @param start start index @param …"
        },
        {
          "name": "dqds",
          "signature": "private void dqds(int start, int end)",
          "shared_fields": [
            "pingPong",
            "work"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Perform a dqds step, using current shift increment. <p>This implementation is a translation of the LAPACK routine DLASQ5.</p> @param start start index @param end end index"
        },
        {
          "name": "dqd",
          "signature": "private void dqd(int start, int end)",
          "shared_fields": [
            "pingPong",
            "work"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Perform a dqd step. <p>This implementation is a translation of the LAPACK routine DLASQ6.</p> @param start start index @param end end index"
        }
      ]
    }
  ],
  "package": "org.apache.commons.math.linear",
  "root_cause_reachable": [],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.util.ArrayList;",
    "import java.util.Arrays;",
    "import java.util.List;",
    "import org.apache.commons.math.MathRuntimeException;",
    "import org.apache.commons.math.MaxIterationsExceededException;",
    "import org.apache.commons.math.util.MathUtils;"
  ],
  "reachable_edges": [],
  "reachable_depth": {
    "[org.apache.commons.math.linear.EigenDecompositionImpl].flipIfWarranted(int,int)": 0
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
<details><summary>▸ output — documented exceptions: {'EigenDecompositionImpl#decompose': ['InvalidMatrixException'], 'EigenDecomp (~1,176 chars)</summary>

**output:** 
```
documented exceptions: {'EigenDecompositionImpl#decompose': ['InvalidMatrixException'], 'EigenDecompositionImpl#findEigenvalues': ['InvalidMatrixException'], 'EigenDecompositionImpl#getD': ['InvalidMatrixException'], 'EigenDecompositionImpl#getEigenvector': ['ArrayIndexOutOfBoundsException', 'InvalidMatrixException'], 'EigenDecompositionImpl#getImagEigenvalue': ['ArrayIndexOutOfBoundsException', 'InvalidMatrixException'], 'EigenDecompositionImpl#getImagEigenvalues': ['InvalidMatrixException'], 'EigenDecompositionImpl#getInverse': ['InvalidMatrixException'], 'EigenDecompositionImpl#getRealEigenvalue': ['ArrayIndexOutOfBoundsException', 'InvalidMatrixException'], 'EigenDecompositionImpl#getRealEigenvalues': ['InvalidMatrixException'], 'EigenDecompositionImpl#getV': ['InvalidMatrixException'], 'EigenDecompositionImpl#getVT': ['InvalidMatrixException'], 'EigenDecompositionImpl#process2RowsBlock': ['InvalidMatrixException'], 'EigenDecompositionImpl#process3RowsBlock': ['InvalidMatrixException'], 'EigenDecompositionImpl#processGeneralBlock': ['InvalidMatrixException'], 'EigenDecompositionImpl#solve': ['IllegalArgumentException', 'InvalidMatrixException']}
```

</details>
<details><summary>▸ detail (~1,160 chars)</summary>

- detail: {'documented': {'EigenDecompositionImpl#decompose': ['InvalidMatrixException'], 'EigenDecompositionImpl#findEigenvalues': ['InvalidMatrixException'], 'EigenDecompositionImpl#getD': ['InvalidMatrixException'], 'EigenDecompositionImpl#getEigenvector': ['ArrayIndexOutOfBoundsException', 'InvalidMatrixException'], 'EigenDecompositionImpl#getImagEigenvalue': ['ArrayIndexOutOfBoundsException', 'InvalidMatrixException'], 'EigenDecompositionImpl#getImagEigenvalues': ['InvalidMatrixException'], 'EigenDecompositionImpl#getInverse': ['InvalidMatrixException'], 'EigenDecompositionImpl#getRealEigenvalue': ['ArrayIndexOutOfBoundsException', 'InvalidMatrixException'], 'EigenDecompositionImpl#getRealEigenvalues': ['InvalidMatrixException'], 'EigenDecompositionImpl#getV': ['InvalidMatrixException'], 'EigenDecompositionImpl#getVT': ['InvalidMatrixException'], 'EigenDecompositionImpl#process2RowsBlock': ['InvalidMatrixException'], 'EigenDecompositionImpl#process3RowsBlock': ['InvalidMatrixException'], 'EigenDecompositionImpl#processGeneralBlock': ['InvalidMatrixException'], 'EigenDecompositionImpl#solve': ['IllegalArgumentException', 'InvalidMatrixException']}}

</details>

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~66,978 chars)</summary>

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
// org.apache.commons.math.linear.EigenDecompositionImplTest::testMathpbx02
    public void testMathpbx02() {

        double[] mainTridiagonal = {
        	  7484.860960227216, 18405.28129035345, 13855.225609560746,
        	 10016.708722343366, 559.8117399576674, 6750.190788301587, 
        	    71.21428769782159
        };
        double[] secondaryTridiagonal = {
        	 -4175.088570476366,1975.7955858241994,5193.178422374075, 
        	  1995.286659169179,75.34535882933804,-234.0808002076056
        };

        // the reference values have been computed using routine DSTEMR
        // from the fortran library LAPACK version 3.2.1
        double[] refEigenValues = {
        		20654.744890306974412,16828.208208485466457,
        		6893.155912634994820,6757.083016675340332,
        		5887.799885688558788,64.309089923240379,
        		57.992628792736340
        };
        RealVector[] refEigenVectors = {
        		new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        		new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
        		new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
        		new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
        		new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
        		new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
        		new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };

        // the following line triggers the exception
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {
                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            } else {
                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            }
        }

    }
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.apache.commons.math.linear.EigenDecompositionImplTest::testMathpbx02
// junit.framework.AssertionFailedError: expected:<16828.208208485466> but was:<20654.74511575945>
// The test DEPENDS on this setup from its test class (helpers, fields, fixtures). A relation that reconstructs the test's scenario must REPLICATE this setup exactly — a relation that cannot must assert only setup-independent properties (counts, kinds, contract formulas), or it will fire on every build including correct ones:
// --- setUp() (test setup) ---
    @Override
    public void setUp() {
        refValues = new double[] {
                2.003, 2.002, 2.001, 1.001, 1.000, 0.001
        };
        matrix = createTestMatrix(new Random(35992629946426l), refValues);
    }

// --- class fields/constants the test uses ---
private double[] refValues;
private RealMatrix matrix;
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(final double[] b) / (final RealVector b) / (final RealMatrix b)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getD, getDeterminant, getEigenvector, getImagEigenvalue, getImagEigenvalues, getInverse, getRealEigenvalue, getRealEigenvalues, getSolver, getV, getVT

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getD, getDeterminant, getImagEigenvalues, getInverse, getRealEigenvalues, getSolver, getV, getVT, isNonSingular
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    private boolean flipIfWarranted(final int n, final int step) {
        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
            // flip array
            int j = 4 * n - 1;
            for (int i = 0; i < j; i += 4) {
                for (int k = 0; k < 4; k += step) {
                    final double tmp = work[i + k];
                    work[i + k] = work[j - k];
                    work[j - k] = tmp;
                }
                j -= 4;
            }
            return true;
        }
        return false;
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned {@link
     * #getImagEigenvalue(int)} and {@link #getImagEigenvalues()} are always null.</p>
     * <p>When called with a {@link RealMatrix} argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the {@link #getEigenvector(int)},
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * {@link MathUtils#SAFE_MIN})
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException {
        if (isSymmetric(matrix)) {
            this.splitTolerance = splitTolerance;
            transformToTridiagonal(matrix);
            decompose();
        } else {
            // as of 2.0, non-symmetric matrices (i.e. complex eigenvalues) are NOT supported
            // see issue https://issues.apache.org/jira/browse/MATH-235
            throw new InvalidMatrixException("eigen decomposition of assymetric matrices not supported yet");
        }
    }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * {@link MathUtils#SAFE_MIN})
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException {

        this.main      = main.clone();
        this.secondary = secondary.clone();
        transformer    = null;

        // pre-compute some elements
        squaredSecondary = new double[secondary.length];
        for (int i = 0; i < squaredSecondary.length; ++i) {
            final double s = secondary[i];
            squaredSecondary[i] = s * s;
        }

        this.splitTolerance = splitTolerance;
        decompose();

    }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { … }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
    private void decompose() { … }
     * {@inheritDoc}
    public RealMatrix getV() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public RealMatrix getD() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public RealMatrix getVT() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public double[] getRealEigenvalues() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { … }
     * {@inheritDoc}
    public double[] getImagEigenvalues() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { … }
     * {@inheritDoc}
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { … }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { … }
     * {@inheritDoc}
    public DecompositionSolver getSolver() { … }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { … }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { … }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) {
        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
            // flip array
            int j = 4 * n - 1;
            for (int i = 0; i < j; i += 4) {
                for (int k = 0; k < 4; k += step) {
                    final double tmp = work[i + k];
                    work[i + k] = work[j - k];
                    work[j - k] = tmp;
                }
                j -= 4;
            }
            return true;
        }
        return false;
    }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { … }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { … }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { … }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { … }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { … }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { … }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { … }
     * Find eigenvectors.
    private void findEigenVectors() { … }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two {@link #getRealEigenvalue(int) getRealEigenvalue} and {@link #getImagEigenvalue(int)
     * getImagEigenvalue} methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>
<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the {@link
     * #ArrayRealVector(ArrayRealVector, ArrayRealVector)} constructor
     * or one of the <code>append</code> method ({@link #append(double)}, {@link
     * #append(double[])}, {@link #append(ArrayRealVector)}) to gather data
     * into this vector.</p>
    public ArrayRealVector() {
        data = new double[0];
    }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) {
        data = new double[size];
    }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) {
        data = new double[size];
        Arrays.fill(data, preset);
    }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) {
        data = d.clone();
    }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException {
        if (d == null) {
            throw new NullPointerException();
        }
        if (d.length == 0) {
            throw MathRuntimeException.createIllegalArgumentException("vector must have at least one element");
        }
        data = copyArray ? d.clone() :  d;
    }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) {
        if (d.length < pos + size) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "position {0} and size {1} don't fit to the size of the input array {2}",
                    pos, size, d.length);
        }
        data = new double[size];
        System.arraycopy(d, pos, data, 0, size);
    }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) {
        data = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            data[i] = d[i].doubleValue();
        }
    }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) {
        if (d.length < pos + size) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "position {0} and size {1} don't fit to the size of the input array {2}",
                    pos, size, d.length);
        }
        data = new double[size];
        for (int i = pos; i < pos + size; i++) {
            data[i-pos] = d[i].doubleValue();
        }
    }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) {
        data = new double[v.getDimension()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = v.getEntry(i);
        }
    }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) {
        data = v.data.clone();
    }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) {
        data = deep ? v.data.clone() : v.data;
    }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) {
        data = new double[v1.data.length + v2.data.length];
        System.arraycopy(v1.data, 0, data, 0, v1.data.length);
        System.arraycopy(v2.data, 0, data, v1.data.length, v2.data.length);
    }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) {
        data = new double[v1.data.length + v2.length];
        System.arraycopy(v1.data, 0, data, 0, v1.data.length);
        System.arraycopy(v2, 0, data, v1.data.length, v2.length);
    }
     * {@inheritDoc}
    public RealVector copy() { … }
     * {@inheritDoc}
    public RealVector mapExp() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>
<class name="MathUtils" role="test-subject">
     * Some useful additions to the built-in functions in {@link Math}.
     * @version $Revision$ $Date$
public final class MathUtils {
     * Smallest positive number such that 1 - EPSILON is not numerically equal to 1.
    public static final double EPSILON = 0x1.0p-53;
     * Safe minimum, such that 1 / SAFE_MIN does not overflow.
     * <p>In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.</p>
    public static final double SAFE_MIN = 0x1.0p-1022;
     * 2 &pi;.
    public static final double TWO_PI = 2 * Math.PI;
     * -1.0 cast as a byte.
    private static final byte NB = (byte)-1;
     * -1.0 cast as a short.
    private static final short NS = (short)-1;
     * 1.0 cast as a byte.
    private static final byte PB = (byte)1;
     * 1.0 cast as a short.
    private static final short PS = (short)1;
     * 0.0 cast as a byte.
    private static final byte ZB = (byte)0;
     * 0.0 cast as a short.
    private static final short ZS = (short)0;
     * Gap between NaN and regular numbers.
    private static final int NAN_GAP = 4 * 1024 * 1024;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * All long-representable factorials
    private static final long[] FACTORIALS = new long[] { … }
     * Private Constructor
    private MathUtils() {
        super();
    }
     * Add two integers, checking for overflow.
     * @param x an addend
     * @param y an addend
     * @return the sum <code>x+y</code>
     * @throws ArithmeticException if the result can not be represented as an
     * int
     * @since 1.1
    public static int addAndCheck(int x, int y) { … }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    public static long addAndCheck(long a, long b) { … }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @param msg the message to use for any thrown exception.
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    private static long addAndCheck(long a, long b, String msg) { … }
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which all coefficients are
     * …
    public static long binomialCoefficient(final int n, final int k) { … }
     * Returns a <code>double</code> representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which all coefficients are <
     * …
    public static double binomialCoefficientDouble(final int n, final int k) { … }
     * Returns the natural <code>log</code> of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * @param n the size of the set
     * …
    public static double binomialCoefficientLog(final int n, final int k) { … }
     * Check binomial preconditions.
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @exception IllegalArgumentException if preconditions are not met.
    private static void checkBinomial(final int n, final int k) throws IllegalArgumentException { … }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  {@link #equals(double, double, double) equals(x, y, eps)}</li>
     * <li>&lt; 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { … }
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of x.
     * @param x double value for which to find the hyperbolic cosine
     * @return hyperbolic cosine of x
    public static double cosh(double x) { … }
     * Returns true iff both arguments are NaN or neither is NaN and they are
     * equal
     * @param x first value
     * @param y second value
     * @return true if the values are equal or both are NaN
    public static boolean equals(double x, double y) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="InvalidMatrixException" role="collaborator">
     * Thrown when a system attempts an operation on a matrix, and
     * that matrix does not satisfy the preconditions for the
     * aforementioned operation.
     * @version $Revision$ $Date$
public class InvalidMatrixException extends MathRuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = 1135533765052675495L;
     * Construct an exception with the given message.
     * @param pattern format specifier
     * @param arguments format arguments
     * @since 2.0
    public InvalidMatrixException(final String pattern, final Object ... arguments) {
        super(pattern, arguments);
    }
     * Construct an exception with the given message.
     * @param cause the exception or error that caused this exception
     * to be thrown.
     * @since 2.0
    public InvalidMatrixException(final Throwable cause) {
        super(cause);
    }
}
</class>
<class name="RealMatrix" role="collaborator">
     * Interface defining a real-valued matrix with basic algebraic operations.
     * <p>
     * Matrix element indexing is 0-based -- e.g., <code>getEntry(0, 0)</code>
     * returns the element in the first row, first column of the matrix.</p>
     * @version $Revision$ $Date$
public interface RealMatrix extends AnyMatrix {

}
</class>
<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Revision$ $Date$
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
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
    @SuppressWarnings("unchecked") public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { … }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
     * @deprecated since 2.0, replaced by {@link #createFieldIdentityMatrix(Field, int)}
    @Deprecated public static BigMatrix createBigIdentityMatrix(int dimension) { … }
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
     * Returns a {@link BigMatrix} whose entries are the the values in the
     * the input array.  The input array is copied, not referenced.
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
     * @throws NullPointerException if data is null
     * @deprecated since 2.0 replaced by {@link #createFieldMatrix(FieldElement[][])}
    @Deprecated public static BigMatrix createBigMatrix(double[][] data) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) {
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) {
        super(rootCause);
        this.pattern   = getMessage();
        this.arguments = new Object[0];
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) {
        super(rootCause);
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { … }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { … }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { … }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { … }
     * {@inheritDoc}
    @Override public String getMessage() { … }
     * {@inheritDoc}
    @Override public String getLocalizedMessage() { … }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { … }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { … }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="SingularMatrixException" role="collaborator">
     * Thrown when a matrix is singular.
     * @version $Revision$ $Date$
     * @since 2.0
public class SingularMatrixException extends InvalidMatrixException {
     * Serializable version identifier.
    private static final long serialVersionUID = -7379143356784298432L;
     * Construct an exception with a default message.
    public SingularMatrixException() {
        super("matrix is singular");
    }
}
</class>
THE PATCH-CHANGED CLASS: EigenDecompositionImpl. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
+++ /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
@@ -1132,7 +1132,7 @@ public class EigenDecompositionImpl implements EigenDecomposition {
     private boolean flipIfWarranted(final int n, final int step) {
         if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
             // flip array
-            int j = 4 * n - 1;
+            int j = 4 * (n - 1);
             for (int i = 0; i < j; i += 4) {
                 for (int k = 0; k < 4; k += step) {
                     final double tmp = work[i + k];

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    REMOVED: int j = 4 * n - 1;
    ADDED  : int j = 4 * (n - 1);
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
<p>The eigen decomposition of matrix A is a set of two matrices:
V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
matrices.</p>
<p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
and hence computes only real realEigenvalues. This implies the D matrix returned by
{@link #getD()} is always diagonal and the imaginary values returned {@link
#getImagEigenvalue(int)} and {@link #getImagEigenvalues()} are always null.</p>
<p>When called with a {@link RealMatrix} argument, this implementation only uses
the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
<p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
are computed only when required, i.e. only when one of the {@link #getEigenvector(int)},
{@link #getV()}, {@link #getVT()}, {@link #getSolver()} methods is called.</p>
<p>This implementation is based on Inderjit Singh Dhillon thesis
<a href="http://www.cs.utexas.edu/users/inderjit/public_papers/thesis.pdf">A
New O(n<sup>2</sup>) Algorithm for the Symmetric Tridiagonal Eigenvalue/Eigenvector
Problem</a>, on Beresford N. Parlett and Osni A. Marques paper <a
href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An Implementation of the
dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routines (DLARRE,
DLASQ2, DLAZQ3, DLAZQ4, DLASQ5 and DLASQ6).</p>
<p>The authors of the original for
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.util.MathUtils;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    getRealEigenvalues, dotProduct, getEigenvector, add, getNorm, subtract, ArrayRealVector, EigenDecompositionImpl

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
<details open><summary>▸ Output (~13,344 chars)</summary>

```
[
  {
    "name": "reversed-tridiagonal-preserves-eigenvalue-multiset",
    "kind": "metamorphic",
    "contract": "The class calculates the eigen decomposition of a symmetric matrix. Reversing the order of rows and columns of a symmetric tridiagonal matrix is a permutation similarity transform, so any correct decomposition must produce the same real eigenvalues (as a multiset) for the original and the reversed tridiagonal input.",
    "input": "Choose n in [1,8]. Build a valid symmetric tridiagonal matrix by constructing main[0..n-1] and secondary[0..n-2] from fuzzed ints scaled to doubles. Build the reversed tridiagonal representation with main and secondary both reversed. Both inputs are valid by construction for the tridiagonal constructor.",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] main = new double[n];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-1000000, 1000000) / 16.0;\n}\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < secondary.length; i++) {\n  secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;\n}\ndouble[] rMain = new double[n];\ndouble[] rSecondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < n; i++) {\n  rMain[i] = main[n - 1 - i];\n}\nfor (int i = 0; i < rSecondary.length; i++) {\n  rSecondary[i] = secondary[rSecondary.length - 1 - i];\n}\norg.apache.commons.math.linear.EigenDecompositionImpl d1;\norg.apache.commons.math.linear.EigenDecompositionImpl d2;\ndouble[] ev1;\ndouble[] ev2;\ntry {\n  d1 = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  d2 = new org.apache.commons.math.linear.EigenDecompositionImpl(rMain, rSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction reversed input: \" + e.getMessage());\n}\ntry {\n  ev1 = d1.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected \" + e.getClass().getName() + \" from getRealEigenvalues on original input: \" + e.getMessage());\n}\ntry {\n  ev2 = d2.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected \" + e.getClass().getName() + \" from getRealEigenvalues on reversed input: \" + e.getMessage());\n}\nif (ev1.length != ev2.length) {\n  throw new RuntimeException(\"relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts \" + ev1.length + \" vs \" + ev2.length);\n}\ndouble[] a = ev1.clone();\ndouble[] b = ev2.clone();\njava.util.Arrays.sort(a);\njava.util.Arrays.sort(b);\nfor (int i = 0; i < a.length; i++) {\n  double x = a[i];\n  double y = b[i];\n  double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));\n  if (!(Math.abs(x - y) <= tol)) {\n    throw new RuntimeException(\"relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index \" + i + \": \" + x + \" vs \" + y);\n  }\n}"
  },
  {
    "name": "indexed-real-eigenvalue-agrees-with-array",
    "kind": "metamorphic",
    "contract": "The get* family exposes the same real eigenvalues both through getRealEigenvalues() and through getRealEigenvalue(int); the indexed getter must agree with the corresponding element of the returned array.",
    "input": "Choose n in [1,8], build any valid tridiagonal main and secondary arrays, and choose an index i in [0,n-1].",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] main = new double[n];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-500000, 500000) / 32.0;\n}\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < secondary.length; i++) {\n  secondary[i] = data.consumeInt(-500000, 500000) / 32.0;\n}\nint idx = data.consumeInt(0, n - 1);\norg.apache.commons.math.linear.EigenDecompositionImpl dec;\ndouble[] all;\ndouble one;\ntry {\n  dec = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  all = dec.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array violated: unexpected \" + e.getClass().getName() + \" from getRealEigenvalues: \" + e.getMessage());\n}\ntry {\n  one = dec.getRealEigenvalue(idx);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array violated: unexpected \" + e.getClass().getName() + \" from getRealEigenvalue(\" + idx + \"): \" + e.getMessage());\n}\nif (all.length != n) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array violated: expected \" + n + \" eigenvalues but got \" + all.length);\n}\ndouble tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(one), Math.abs(all[idx])));\nif (!(Math.abs(one - all[idx]) <= tol)) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array violated at index \" + idx + \": \" + one + \" vs \" + all[idx]);\n}"
  },
  {
    "name": "imaginary-eigenvalue-array-is-null-for-symmetric-input",
    "kind": "invariant",
    "contract": "For this symmetric-only implementation, 'the imaginary values returned getImagEigenvalue(int) and getImagEigenvalues() are always null.' Therefore getImagEigenvalues() must return null for every valid decomposition produced by this class.",
    "input": "Choose any valid tridiagonal main and secondary arrays with lengths n and n-1, n in [1,8].",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] main = new double[n];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-200000, 200000) / 64.0;\n}\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < secondary.length; i++) {\n  secondary[i] = data.consumeInt(-200000, 200000) / 64.0;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl dec;\ndouble[] imag;\ntry {\n  dec = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  imag = dec.getImagEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: unexpected \" + e.getClass().getName() + \" from getImagEigenvalues: \" + e.getMessage());\n}\nif (imag != null) {\n  throw new RuntimeException(\"relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length \" + imag.length);\n}"
  },
  {
    "name": "d-matrix-is-diagonal-and-matches-real-eigenvalues",
    "kind": "invariant",
    "contract": "The class javadoc states that for symmetric matrices the returned D matrix is always diagonal. Since D is the eigenvalue matrix in A = V D V^T, its diagonal entries must be the real eigenvalues exposed by the getters.",
    "input": "Choose any valid tridiagonal decomposition input with n in [1,6] to keep matrix checks small.",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-300000, 300000) / 32.0;\n}\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < secondary.length; i++) {\n  secondary[i] = data.consumeInt(-300000, 300000) / 32.0;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl dec;\norg.apache.commons.math.linear.RealMatrix d;\ndouble[] evals;\ntry {\n  dec = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation d-matrix-is-diagonal-and-matches-real-eigenvalues violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  d = dec.getD();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation d-matrix-is-diagonal-and-matches-real-eigenvalues violated: unexpected \" + e.getClass().getName() + \" from getD: \" + e.getMessage());\n}\ntry {\n  evals = dec.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation d-matrix-is-diagonal-and-matches-real-eigenvalues violated: unexpected \" + e.getClass().getName() + \" from getRealEigenvalues: \" + e.getMessage());\n}\nfor (int i = 0; i < n; i++) {\n  for (int j = 0; j < n; j++) {\n    double entry = d.getEntry(i, j);\n    if (i == j) {\n      double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(entry), Math.abs(evals[i])));\n      if (!(Math.abs(entry - evals[i]) <= tol)) {\n        throw new RuntimeException(\"relation d-matrix-is-diagonal-and-matches-real-eigenvalues violated on diagonal \" + i + \": \" + entry + \" vs \" + evals[i]);\n      }\n    } else {\n      double tol = 1e-9 * Math.max(1.0, Math.abs(entry));\n      if (!(Math.abs(entry) <= tol)) {\n        throw new RuntimeException(\"relation d-matrix-is-diagonal-and-matches-real-eigenvalues violated: off-diagonal entry (\" + i + \",\" + j + \")=\" + entry);\n      }\n    }\n  }\n}"
  },
  {
    "name": "vt-is-transpose-of-v",
    "kind": "metamorphic",
    "contract": "The interface and class documentation define the decomposition as A = V D V^T and provide both getV() and getVT(); therefore getVT() must agree with the transpose of getV().",
    "input": "Choose any valid tridiagonal decomposition input with n in [1,6].",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-100000, 100000) / 16.0;\n}\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < secondary.length; i++) {\n  secondary[i] = data.consumeInt(-100000, 100000) / 16.0;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl dec;\norg.apache.commons.math.linear.RealMatrix v;\norg.apache.commons.math.linear.RealMatrix vt;\ntry {\n  dec = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n  v = dec.getV();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: unexpected \" + e.getClass().getName() + \" from getV: \" + e.getMessage());\n}\ntry {\n  vt = dec.getVT();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: unexpected \" + e.getClass().getName() + \" from getVT: \" + e.getMessage());\n}\nfor (int i = 0; i < n; i++) {\n  for (int j = 0; j < n; j++) {\n    double a = vt.getEntry(i, j);\n    double b = v.getEntry(j, i);\n    double tol = 1e-8 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));\n    if (!(Math.abs(a - b) <= tol)) {\n      throw new RuntimeException(\"relation vt-is-transpose-of-v violated at (\" + i + \",\" + j + \"): \" + a + \" vs transpose entry \" + b);\n    }\n  }\n}"
  },
  {
    "name": "arrayrealvector-copies-input-array",
    "kind": "invariant",
    "contract": "ArrayRealVector(double[] d) is documented as 'Construct a vector from an array, copying the input array.' A later mutation of the source array must therefore not change the vector contents.",
    "input": "Choose a positive length in [1,8], fill a double[] from fuzzed ints, construct an ArrayRealVector from it, then mutate one chosen array cell.",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] src = new double[n];\nfor (int i = 0; i < n; i++) {\n  src[i] = data.consumeInt(-100000, 100000) / 8.0;\n}\nint idx = data.consumeInt(0, n - 1);\ndouble before = src[idx];\norg.apache.commons.math.linear.ArrayRealVector v;\ntry {\n  v = new org.apache.commons.math.linear.ArrayRealVector(src);\n} catch (Exception e) {\n  return;\n}\nsrc[idx] = before + (data.consumeBoolean() ? 17.0 : -23.0);\ndouble observed;\ntry {\n  observed = v.getEntry(idx);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation arrayrealvector-copies-input-array violated: unexpected \" + e.getClass().getName() + \" from getEntry: \" + e.getMessage());\n}\ndouble tol = 1e-12 * Math.max(1.0, Math.max(Math.abs(before), Math.abs(observed)));\nif (!(Math.abs(observed - before) <= tol)) {\n  throw new RuntimeException(\"relation arrayrealvector-copies-input-array violated at index \" + idx + \": expected preserved value \" + before + \" but saw \" + observed);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `reversed-tridiagonal-preserves-eigenvalue-multiset`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [6] ⚙️ screen-fuzz-buggy · `reversed-tridiagonal-preserves-eigenvalue-multiset`
**output:** 
```json
{
  "checked": 20000,
  "violated": 374,
  "ratio": 0.0187
}
```

---
## [7] ⚙️ screen · `indexed-real-eigenvalue-agrees-with-array`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [8] ⚙️ screen-fuzz-buggy · `indexed-real-eigenvalue-agrees-with-array`
**output:** 
```json
{
  "checked": 20000,
  "violated": 115,
  "ratio": 0.0057
}
```

---
## [9] ⚙️ screen · `imaginary-eigenvalue-array-is-null-for-symmetric-input`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [10] ⚙️ screen-fuzz-buggy · `imaginary-eigenvalue-array-is-null-for-symmetric-input`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [11] ⚙️ screen · `d-matrix-is-diagonal-and-matches-real-eigenvalues`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [12] ⚙️ screen-fuzz-buggy · `d-matrix-is-diagonal-and-matches-real-eigenvalues`
**output:** 
```json
{
  "checked": 20000,
  "violated": 134,
  "ratio": 0.0067
}
```

---
## [13] ⚙️ screen · `vt-is-transpose-of-v`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [14] ⚙️ screen-fuzz-buggy · `vt-is-transpose-of-v`
**output:** 
```json
{
  "checked": 20000,
  "violated": 306,
  "ratio": 0.0153
}
```

---
## [15] ⚙️ screen · `arrayrealvector-copies-input-array`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [16] ⚙️ screen-fuzz-buggy · `arrayrealvector-copies-input-array`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [17] ⚙️ screen · `imaginary-eigenvalue-array-is-null-for-symmetric-input`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [18] ⚙️ screen · `reversed-tridiagonal-preserves-eigenvalue-multiset`
**output:** **kept**
- reason: kept: selective firer

---
## [19] ⚙️ screen · `indexed-real-eigenvalue-agrees-with-array`
**output:** **kept**
- reason: kept: selective firer

---
## [20] ⚙️ screen · `d-matrix-is-diagonal-and-matches-real-eigenvalues`
**output:** **kept**
- reason: kept: selective firer

---
## [21] ⚙️ screen · `vt-is-transpose-of-v`
**output:** **kept**
- reason: kept: selective firer

---
## [22] ⚙️ screen · `arrayrealvector-copies-input-array`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [23] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "imaginary-eigenvalue-array-is-null-for-symmetric-input",
    "reversed-tridiagonal-preserves-eigenvalue-multiset",
    "indexed-real-eigenvalue-agrees-with-array",
    "d-matrix-is-diagonal-and-matches-real-eigenvalues",
    "vt-is-transpose-of-v",
    "arrayrealvector-copies-input-array"
  ],
  "count": 6
}
```

---
## [24] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~16,032 chars)</summary>

```
[
  {
    "name": "tridiagonal-constructor-agrees-with-matrix-constructor-on-sorted-eigenvalues",
    "kind": "metamorphic",
    "contract": "The class provides two ways to calculate the eigen decomposition of the same symmetric matrix: from a symmetric RealMatrix and from its tridiagonal main/secondary diagonals. For equivalent inputs, both decompositions are of the same matrix A, so they must yield the same real eigenvalues.",
    "input": "Choose n in [2, 8]. Build finite moderate-valued main[n] and secondary[n-1] from data.consumeInt(-1000, 1000). Build the corresponding symmetric tridiagonal matrix explicitly, so both constructors receive valid-by-construction representations of the same symmetric matrix.",
    "check": "int n = data.consumeInt(2, 8);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\ndouble[][] mData = new double[n][n];\ntry {\n  for (int i = 0; i < n; i++) {\n    main[i] = (double) data.consumeInt(-1000, 1000);\n    mData[i][i] = main[i];\n  }\n  for (int i = 0; i < n - 1; i++) {\n    secondary[i] = (double) data.consumeInt(-1000, 1000);\n    mData[i][i + 1] = secondary[i];\n    mData[i + 1][i] = secondary[i];\n  }\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.RealMatrix matrix;\ntry {\n  matrix = org.apache.commons.math.linear.MatrixUtils.createRealMatrix(mData);\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl fromTri;\ntry {\n  fromTri = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation tridiagonal-constructor-agrees-with-matrix-constructor-on-sorted-eigenvalues violated: unexpected \" + e.getClass().getName() + \" from tridiagonal constructor on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.EigenDecompositionImpl fromMatrix;\ntry {\n  fromMatrix = new org.apache.commons.math.linear.EigenDecompositionImpl(matrix, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation tridiagonal-constructor-agrees-with-matrix-constructor-on-sorted-eigenvalues violated: unexpected \" + e.getClass().getName() + \" from matrix constructor on valid-by-construction input: \" + e.getMessage());\n}\ndouble[] a;\ndouble[] b;\ntry {\n  a = fromTri.getRealEigenvalues();\n  b = fromMatrix.getRealEigenvalues();\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation tridiagonal-constructor-agrees-with-matrix-constructor-on-sorted-eigenvalues violated: unexpected \" + e.getClass().getName() + \" when reading eigenvalues on valid-by-construction input: \" + e.getMessage());\n}\nif (a.length != b.length) {\n  throw new RuntimeException(\"relation tridiagonal-constructor-agrees-with-matrix-constructor-on-sorted-eigenvalues violated: different eigenvalue counts \" + a.length + \" vs \" + b.length);\n}\ndouble[] sa = a.clone();\ndouble[] sb = b.clone();\njava.util.Arrays.sort(sa);\njava.util.Arrays.sort(sb);\nfor (int i = 0; i < sa.length; i++) {\n  double x = sa[i];\n  double y = sb[i];\n  double tol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));\n  if (Math.abs(x - y) > tol) {\n    throw new RuntimeException(\"relation tridiagonal-constructor-agrees-with-matrix-constructor-on-sorted-eigenvalues violated at \" + i + \": \" + x + \" vs \" + y);\n  }\n}"
  },
  {
    "name": "vt-is-transpose-of-v",
    "kind": "invariant",
    "contract": "The interface and class javadoc define the decomposition as A = V D V^T, and a getVT() method is provided specifically as V^T. Therefore getVT() must equal the transpose of getV().",
    "input": "Choose n in [2, 8] and build a symmetric tridiagonal matrix from moderate finite integers, then decompose it with either constructor. The matrix is symmetric by construction and valid input for this implementation.",
    "check": "int n = data.consumeInt(2, 8);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\ntry {\n  for (int i = 0; i < n; i++) {\n    main[i] = (double) data.consumeInt(-1000, 1000);\n  }\n  for (int i = 0; i < n - 1; i++) {\n    secondary[i] = (double) data.consumeInt(-1000, 1000);\n  }\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\ntry {\n  ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: unexpected \" + e.getClass().getName() + \" from constructor on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.RealMatrix v;\norg.apache.commons.math.linear.RealMatrix vt;\ntry {\n  v = ed.getV();\n  vt = ed.getVT();\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: unexpected \" + e.getClass().getName() + \" when reading V/VT on valid-by-construction input: \" + e.getMessage());\n}\nif (v.getRowDimension() != vt.getColumnDimension() || v.getColumnDimension() != vt.getRowDimension()) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: incompatible dimensions V=\" + v.getRowDimension() + \"x\" + v.getColumnDimension() + \" VT=\" + vt.getRowDimension() + \"x\" + vt.getColumnDimension());\n}\nfor (int i = 0; i < v.getRowDimension(); i++) {\n  for (int j = 0; j < v.getColumnDimension(); j++) {\n    double x = v.getEntry(i, j);\n    double y = vt.getEntry(j, i);\n    double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));\n    if (Math.abs(x - y) > tol) {\n      throw new RuntimeException(\"relation vt-is-transpose-of-v violated at (\" + i + \",\" + j + \"): \" + x + \" vs \" + y);\n    }\n  }\n}"
  },
  {
    "name": "d-is-diagonal-and-its-diagonal-matches-real-eigenvalues",
    "kind": "invariant",
    "contract": "The class javadoc states that for this symmetric-only implementation, the D matrix returned by getD() is always diagonal. Since D is the eigenvalue matrix in A = V D V^T, its diagonal entries must be the real eigenvalues returned by getRealEigenvalues().",
    "input": "Choose n in [1, 8], build finite moderate main and secondary arrays for a symmetric tridiagonal matrix, and decompose them. This stays within the documented supported domain of symmetric matrices with real eigenvalues.",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\ntry {\n  for (int i = 0; i < n; i++) {\n    main[i] = (double) data.consumeInt(-1000, 1000);\n  }\n  for (int i = 0; i < n - 1; i++) {\n    secondary[i] = (double) data.consumeInt(-1000, 1000);\n  }\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\ntry {\n  ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation d-is-diagonal-and-its-diagonal-matches-real-eigenvalues violated: unexpected \" + e.getClass().getName() + \" from constructor on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.RealMatrix d;\ndouble[] evals;\ntry {\n  d = ed.getD();\n  evals = ed.getRealEigenvalues();\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation d-is-diagonal-and-its-diagonal-matches-real-eigenvalues violated: unexpected \" + e.getClass().getName() + \" when reading D/eigenvalues on valid-by-construction input: \" + e.getMessage());\n}\nif (d.getRowDimension() != evals.length || d.getColumnDimension() != evals.length) {\n  throw new RuntimeException(\"relation d-is-diagonal-and-its-diagonal-matches-real-eigenvalues violated: D dimension mismatch\");\n}\nfor (int i = 0; i < evals.length; i++) {\n  for (int j = 0; j < evals.length; j++) {\n    double entry = d.getEntry(i, j);\n    double expected = (i == j) ? evals[i] : 0.0;\n    double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(entry), Math.abs(expected)));\n    if (Math.abs(entry - expected) > tol) {\n      throw new RuntimeException(\"relation d-is-diagonal-and-its-diagonal-matches-real-eigenvalues violated at (\" + i + \",\" + j + \"): \" + entry + \" vs \" + expected);\n    }\n  }\n}"
  },
  {
    "name": "indexed-real-eigenvalue-agrees-with-array-accessor",
    "kind": "metamorphic",
    "contract": "The get* family exposes both getRealEigenvalues() and getRealEigenvalue(int). For the same decomposition, the indexed getter must agree with the corresponding element of the array getter.",
    "input": "Choose n in [1, 8], build a symmetric tridiagonal problem from finite moderate integers, decompose it, then choose an index i uniformly from the valid range [0, n-1].",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\ntry {\n  for (int i = 0; i < n; i++) {\n    main[i] = (double) data.consumeInt(-1000, 1000);\n  }\n  for (int i = 0; i < n - 1; i++) {\n    secondary[i] = (double) data.consumeInt(-1000, 1000);\n  }\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\ntry {\n  ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array-accessor violated: unexpected \" + e.getClass().getName() + \" from constructor on valid-by-construction input: \" + e.getMessage());\n}\ndouble[] evals;\ntry {\n  evals = ed.getRealEigenvalues();\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array-accessor violated: unexpected \" + e.getClass().getName() + \" when reading eigenvalue array on valid-by-construction input: \" + e.getMessage());\n}\nif (evals.length == 0) {\n  return;\n}\nint i = data.consumeInt(0, evals.length - 1);\ndouble single;\ntry {\n  single = ed.getRealEigenvalue(i);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array-accessor violated: unexpected \" + e.getClass().getName() + \" from indexed getter on valid-by-construction input: \" + e.getMessage());\n}\ndouble x = single;\ndouble y = evals[i];\ndouble tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));\nif (Math.abs(x - y) > tol) {\n  throw new RuntimeException(\"relation indexed-real-eigenvalue-agrees-with-array-accessor violated at index \" + i + \": \" + x + \" vs \" + y);\n}"
  },
  {
    "name": "eigenvector-getter-agrees-with-v-column",
    "kind": "metamorphic",
    "contract": "The decomposition is defined by matrices V and D; V is the matrix of eigenvectors, and getEigenvector(int) exposes an individual eigenvector. Therefore the i-th column of V must agree with getEigenvector(i).",
    "input": "Choose n in [1, 8], build a symmetric tridiagonal matrix from finite moderate integers, decompose it, and choose a valid eigenvector index i from the decomposition size.",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\ntry {\n  for (int i = 0; i < n; i++) {\n    main[i] = (double) data.consumeInt(-1000, 1000);\n  }\n  for (int i = 0; i < n - 1; i++) {\n    secondary[i] = (double) data.consumeInt(-1000, 1000);\n  }\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\ntry {\n  ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation eigenvector-getter-agrees-with-v-column violated: unexpected \" + e.getClass().getName() + \" from constructor on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.RealMatrix v;\ndouble[] evals;\ntry {\n  v = ed.getV();\n  evals = ed.getRealEigenvalues();\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation eigenvector-getter-agrees-with-v-column violated: unexpected \" + e.getClass().getName() + \" when reading V/eigenvalues on valid-by-construction input: \" + e.getMessage());\n}\nif (evals.length == 0) {\n  return;\n}\nint i = data.consumeInt(0, evals.length - 1);\norg.apache.commons.math.linear.RealVector vec;\ntry {\n  vec = ed.getEigenvector(i);\n} catch (org.apache.commons.math.linear.InvalidMatrixException e) {\n  return;\n} catch (Exception e) {\n  throw new RuntimeException(\"relation eigenvector-getter-agrees-with-v-column violated: unexpected \" + e.getClass().getName() + \" from getEigenvector on valid-by-construction input: \" + e.getMessage());\n}\nif (vec.getDimension() != v.getRowDimension()) {\n  throw new RuntimeException(\"relation eigenvector-getter-agrees-with-v-column violated: vector dimension \" + vec.getDimension() + \" vs V rows \" + v.getRowDimension());\n}\nfor (int r = 0; r < vec.getDimension(); r++) {\n  double x = vec.getEntry(r);\n  double y = v.getEntry(r, i);\n  double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));\n  if (Math.abs(x - y) > tol) {\n    throw new RuntimeException(\"relation eigenvector-getter-agrees-with-v-column violated at row \" + r + \": \" + x + \" vs \" + y);\n  }\n}"
  },
  {
    "name": "arrayrealvector-array-constructor-copies-input",
    "kind": "invariant",
    "contract": "The ArrayRealVector(double[] d) constructor is documented as 'Construct a vector from an array, copying the input array.' Therefore later mutation of the source array must not change the vector contents.",
    "input": "Choose an array length in [1, 8] and fill it with finite moderate integers converted to doubles. Construct ArrayRealVector from that array, then mutate the original array after construction.",
    "check": "int n = data.consumeInt(1, 8);\ndouble[] src = new double[n];\ntry {\n  for (int i = 0; i < n; i++) {\n    src[i] = (double) data.consumeInt(-1000, 1000);\n  }\n} catch (Exception e) {\n  return;\n}\nint idx = data.consumeInt(0, n - 1);\ndouble before = src[idx];\norg.apache.commons.math.linear.ArrayRealVector v;\ntry {\n  v = new org.apache.commons.math.linear.ArrayRealVector(src);\n} catch (Exception e) {\n  return;\n}\nsrc[idx] = before + 1.0 + Math.abs((double) data.consumeInt(0, 1000));\ndouble observed;\ntry {\n  observed = v.getEntry(idx);\n} catch (Exception e) {\n  throw new RuntimeException(\"relation arrayrealvector-array-constructor-copies-input violated: unexpected \" + e.getClass().getName() + \" when reading vector entry: \" + e.getMessage());\n}\ndouble tol = 1.0e-12 * Math.max(1.0, Math.abs(before));\nif (Math.abs(observed - before) > tol) {\n  throw new RuntimeException(\"relation arrayrealvector-array-constructor-copies-input violated at index \" + idx + \": observed \" + observed + \" but expected preserved value \" + before);\n}"
  }
]
```

</details>

---
## [25] ⚙️ screen · `tridiagonal-constructor-agrees-with-matrix-constructor-on-so`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [26] ⚙️ screen-fuzz-buggy · `tridiagonal-constructor-agrees-with-matrix-constructor-on-so`
**output:** 
```json
{
  "checked": 20000,
  "violated": 27,
  "ratio": 0.0014
}
```

---
## [27] ⚙️ screen · `vt-is-transpose-of-v`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [28] ⚙️ screen-fuzz-buggy · `vt-is-transpose-of-v`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [29] ⚙️ screen · `d-is-diagonal-and-its-diagonal-matches-real-eigenvalues`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [30] ⚙️ screen-fuzz-buggy · `d-is-diagonal-and-its-diagonal-matches-real-eigenvalues`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [31] ⚙️ screen · `indexed-real-eigenvalue-agrees-with-array-accessor`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [32] ⚙️ screen-fuzz-buggy · `indexed-real-eigenvalue-agrees-with-array-accessor`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [33] ⚙️ screen · `eigenvector-getter-agrees-with-v-column`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [34] ⚙️ screen-fuzz-buggy · `eigenvector-getter-agrees-with-v-column`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [35] ⚙️ screen · `arrayrealvector-array-constructor-copies-input`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [36] ⚙️ screen-fuzz-buggy · `arrayrealvector-array-constructor-copies-input`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [37] ⚙️ screen · `tridiagonal-constructor-agrees-with-matrix-constructor-on-so`
**output:** **kept**
- reason: kept: selective firer

---
## [38] ⚙️ screen · `vt-is-transpose-of-v`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [39] ⚙️ screen · `d-is-diagonal-and-its-diagonal-matches-real-eigenvalues`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [40] ⚙️ screen · `indexed-real-eigenvalue-agrees-with-array-accessor`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [41] ⚙️ screen · `eigenvector-getter-agrees-with-v-column`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [42] ⚙️ screen · `arrayrealvector-array-constructor-copies-input`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [43] ⚙️ reference-widening · `enumerate`
**output:** **3 observable(s) to widen onto**
<details><summary>▸ reason (~445 chars)</summary>

- reason: the kept checks probe ['imageigenvalues', 'getv', 'getd', 'getvt', 'realeigenvalues'] among the patched class's no-argument readers; 5 of those have no admitted reference and 3 fit under the per-leg cap of 3 (the leg already holds []); 10 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['message', 'invalidmatrixexception', 'symmetric', 'realeigenvalue', 'imageigenvalue', 'eigenvector'])

</details>
<details><summary>▸ detail (~2,332 chars)</summary>

- detail: {'targets': ['imageigenvalues', 'getv', 'getd'], 'already_admitted': [], 'cap': 3, 'checks_read': 11, 'excluded': [{'key': 'message', 'why': 'the patched class does not declare `getMessage()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'invalidmatrixexception', 'why': "the patched class declares `InvalidMatrixException` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'symmetric', 'why': "the patched class declares `isSymmetric` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'realeigenvalue', 'why': "the patched class declares `getRealEigenvalue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'imageigenvalue', 'why': "the patched class declares `getImagEigenvalue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'eigenvector', 'why': "the patched class declares `getEigenvector` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'mathutils', 'why': 'the patched class does not declare `MathUtils()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'arrayrealvector', 'why': 'the patched class does not declare `ArrayRealVector()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'createrealmatrix', 'why': 'the patched class does not declare `createRealMatrix()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'matrixutils', 'why': 'the patched class does not declare `MatrixUtils()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 10}

</details>

---
## [44] ⚙️ reference-widening · `getImagEigenvalues`
**output:** **reference REQUESTED for observable `imageigenvalues`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [45] ⚙️ reference-impl · `getImagEigenvalues`
**output:** **widening target requested**
- detail: {'candidates': ['getImagEigenvalues'], 'attempting': 'getImagEigenvalues'}

---
## [46] ⚙️ reference-impl · `getImagEigenvalues`
**output:** **screening surface resolved**
- reason: 2 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getDeterminant', 'getRealEigenvalues'], 'patch_touched_exempted': [], 'screened_siblings': ['getDeterminant', 'getRealEigenvalues'], 'declaring_types': ['EigenDecompositionImpl']}

---
## [47] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~59,474 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
**[user]**
```
Implement `getImagEigenvalues` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned { /* body withheld */ } and { /* body withheld */ } are always null.</p>
     * <p>When called with a { /* body withheld */ } argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the { /* body withheld */ },
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { /* body withheld */ }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    private void decompose() { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getV() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getD() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getVT() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getRealEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getImagEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { /* body withheld */ }
     * { /* body withheld */ }
    public DecompositionSolver getSolver() { /* body withheld */ }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { /* body withheld */ }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { /* body withheld */ }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) { /* body withheld */ }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { /* body withheld */ }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { /* body withheld */ }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { /* body withheld */ }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { /* body withheld */ }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { /* body withheld */ }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { /* body withheld */ }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { /* body withheld */ }
     * Find eigenvectors.
    private void findEigenVectors() { /* body withheld */ }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two { /* body withheld */ } and { /* body withheld */ } methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the { /* body withheld */ } constructor
     * or one of the <code>append</code> method ({ /* body withheld */ }, { /* body withheld */ }, { /* body withheld */ }) to gather data
     * into this vector.</p>
    public ArrayRealVector() { /* body withheld */ }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) { /* body withheld */ }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) { /* body withheld */ }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) { /* body withheld */ }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException { /* body withheld */ }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) { /* body withheld */ }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) { /* body withheld */ }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector copy() { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector mapExp() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testMathpbx02() {

        double[] mainTridiagonal = {
        	  7484.860960227216, 18405.28129035345, 13855.225609560746,
        	 10016.708722343366, 559.8117399576674, 6750.190788301587, 
        	    71.21428769782159
        };
        double[] secondaryTridiagonal = {
        	 -4175.088570476366,1975.7955858241994,5193.178422374075, 
        	  1995.286659169179,75.34535882933804,-234.0808002076056
        };

        // the reference values have been computed using routine DSTEMR
        // from the fortran library LAPACK version 3.2.1
        double[] refEigenValues = {
        		20654.744890306974412,16828.208208485466457,
        		6893.155912634994820,6757.083016675340332,
        		5887.799885688558788,64.309089923240379,
        		57.992628792736340
        };
        RealVector[] refEigenVectors = {
        		new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        		new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
        		new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
        		new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
        		new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
        		new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
        		new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };

        // the following line triggers the exception
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {
                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            } else {
                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            }
        }

    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned { /* body withheld */ } and { /* body withheld */ } are always null.</p>
     * <p>When called with a { /* body withheld */ } argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the { /* body withheld */ },
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { /* body withheld */ }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    private void decompose() { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getV() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getD() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getVT() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getRealEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getImagEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { /* body withheld */ }
     * { /* body withheld */ }
    public DecompositionSolver getSolver() { /* body withheld */ }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { /* body withheld */ }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { /* body withheld */ }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) { /* body withheld */ }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { /* body withheld */ }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { /* body withheld */ }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { /* body withheld */ }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { /* body withheld */ }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { /* body withheld */ }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { /* body withheld */ }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { /* body withheld */ }
     * Find eigenvectors.
    private void findEigenVectors() { /* body withheld */ }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two { /* body withheld */ } and { /* body withheld */ } methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the { /* body withheld */ } constructor
     * or one of the <code>append</code> method ({ /* body withheld */ }, { /* body withheld */ }, { /* body withheld */ }) to gather data
     * into this vector.</p>
    public ArrayRealVector() { /* body withheld */ }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) { /* body withheld */ }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) { /* body withheld */ }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) { /* body withheld */ }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException { /* body withheld */ }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) { /* body withheld */ }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) { /* body withheld */ }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector copy() { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector mapExp() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

<class name="MathUtils" role="test-subject">
     * Some useful additions to the built-in functions in {@link Math}.
     * @version $Revision$ $Date$
public final class MathUtils {
     * Smallest positive number such that 1 - EPSILON is not numerically equal to 1.
    public static final double EPSILON = 0x1.0p-53;
     * Safe minimum, such that 1 / SAFE_MIN does not overflow.
     * <p>In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.</p>
    public static final double SAFE_MIN = 0x1.0p-1022;
     * 2 &pi;.
    public static final double TWO_PI = 2 * Math.PI;
     * -1.0 cast as a byte.
    private static final byte NB = (byte)-1;
     * -1.0 cast as a short.
    private static final short NS = (short)-1;
     * 1.0 cast as a byte.
    private static final byte PB = (byte)1;
     * 1.0 cast as a short.
    private static final short PS = (short)1;
     * 0.0 cast as a byte.
    private static final byte ZB = (byte)0;
     * 0.0 cast as a short.
    private static final short ZS = (short)0;
     * Gap between NaN and regular numbers.
    private static final int NAN_GAP = 4 * 1024 * 1024;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * All long-representable factorials
    private static final long[] FACTORIALS = new long[] { /* body withheld */ }
     * Private Constructor
    private MathUtils() { /* body withheld */ }
     * Add two integers, checking for overflow.
     * @param x an addend
     * @param y an addend
     * @return the sum <code>x+y</code>
     * @throws ArithmeticException if the result can not be represented as an
     * int
     * @since 1.1
    public static int addAndCheck(int x, int y) { /* body withheld */ }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    public static long addAndCheck(long a, long b) { /* body withheld */ }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @param msg the message to use for any thrown exception.
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    private static long addAndCheck(long a, long b, String msg) { /* body withheld */ }
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which all coefficients are
     * …
    public static long binomialCoefficient(final int n, final int k) { /* body withheld */ }
     * Returns a <code>double</code> representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which all coefficients are <
     * …
    public static double binomialCoefficientDouble(final int n, final int k) { /* body withheld */ }
     * Returns the natural <code>log</code> of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * @param n the size of the set
     * …
    public static double binomialCoefficientLog(final int n, final int k) { /* body withheld */ }
     * Check binomial preconditions.
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @exception IllegalArgumentException if preconditions are not met.
    private static void checkBinomial(final int n, final int k) throws IllegalArgumentException { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * <li>&lt; 0 if !{ /* body withheld */ } &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{ /* body withheld */ } &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { /* body withheld */ }
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of x.
     * @param x double value for which to find the hyperbolic cosine
     * @return hyperbolic cosine of x
    public static double cosh(double x) { /* body withheld */ }
     * Returns true iff both arguments are NaN or neither is NaN and they are
     * equal
     * @param x first value
     * @param y second value
     * @return true if the values are equal or both are NaN
    public static boolean equals(double x, double y) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="InvalidMatrixException" role="collaborator">
     * Thrown when a system attempts an operation on a matrix, and
     * that matrix does not satisfy the preconditions for the
     * aforementioned operation.
     * @version $Revision$ $Date$
public class InvalidMatrixException extends MathRuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = 1135533765052675495L;
     * Construct an exception with the given message.
     * @param pattern format specifier
     * @param arguments format arguments
     * @since 2.0
    public InvalidMatrixException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Construct an exception with the given message.
     * @param cause the exception or error that caused this exception
     * to be thrown.
     * @since 2.0
    public InvalidMatrixException(final Throwable cause) { /* body withheld */ }
}
</class>

<class name="RealMatrix" role="collaborator">
     * Interface defining a real-valued matrix with basic algebraic operations.
     * <p>
     * Matrix element indexing is 0-based -- e.g., <code>getEntry(0, 0)</code>
     * returns the element in the first row, first column of the matrix.</p>
     * @version $Revision$ $Date$
public interface RealMatrix extends AnyMatrix {

}
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Revision$ $Date$
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
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
    @SuppressWarnings("unchecked") public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
     * @deprecated since 2.0, replaced by { /* body withheld */ }
    @Deprecated public static BigMatrix createBigIdentityMatrix(int dimension) { /* body withheld */ }
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
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.  The input array is copied, not referenced.
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
     * @throws NullPointerException if data is null
     * @deprecated since 2.0 replaced by { /* body withheld */ }
    @Deprecated public static BigMatrix createBigMatrix(double[][] data) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { /* body withheld */ }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { /* body withheld */ }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { /* body withheld */ }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { /* body withheld */ }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="SingularMatrixException" role="collaborator">
     * Thrown when a matrix is singular.
     * @version $Revision$ $Date$
     * @since 2.0
public class SingularMatrixException extends InvalidMatrixException {
     * Serializable version identifier.
    private static final long serialVersionUID = -7379143356784298432L;
     * Construct an exception with a default message.
    public SingularMatrixException() { /* body withheld */ }
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getDeterminant`, `getRealEigenvalues`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,066 chars)</summary>

```
// compute(double[]) : getImagEigenvalues, getRealEigenvalues, getDeterminant, getImagEigenvalue, getRealEigenvalue
public class ReferenceImpl {

    public static double[] compute_getRealEigenvalues(final double[] realEigenvalues) {
        return realEigenvalues == null ? null : realEigenvalues.clone();
    }

    public static double[] compute_getImagEigenvalues(final double[] realEigenvalues) {
        if (realEigenvalues == null) {
            return null;
        }
        return new double[realEigenvalues.length];
    }

    public static double compute_getRealEigenvalue(final double[] realEigenvalues, final int i) {
        return realEigenvalues[i];
    }

    public static double compute_getImagEigenvalue(final double[] realEigenvalues, final int i) {
        return 0.0;
    }

    public static double compute_getDeterminant(final double[] realEigenvalues) {
        double determinant = 1.0;
        for (int i = 0; i < realEigenvalues.length; ++i) {
            determinant *= realEigenvalues[i];
        }
        return determinant;
    }
}
```

</details>

---
## [48] ⚙️ reference-impl · `getImagEigenvalues`
**output:** **reference generated**
- detail: {'chars': 1066}

---
## [49] ⚙️ reference-impl · `getImagEigenvalues`
**output:** **signature declared**
- detail: {'signature': 'double[]'}

---
## [50] ⚙️ reference-impl · `getImagEigenvalues`
**output:** **reference observables matched**
- detail: {'matched': {'getImagEigenvalues': 'getImagEigenvalues', 'getDeterminant': 'getDeterminant', 'getRealEigenvalues': 'getRealEigenvalues'}, 'declared_only': ['getImagEigenvalue', 'getRealEigenvalue']}

---
## [51] ⚙️ reference-impl · `getImagEigenvalues`
**output:** **reference too thin to screen — DISCARDED**
- reason: 2 shared sibling observable(s) ['getDeterminant', 'getRealEigenvalues']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead

---
## [52] ⚙️ reference-widening · `getImagEigenvalues`
**output:** **widening REJECTED for observable `imageigenvalues`**
- reason: reference too thin to screen — DISCARDED — 2 shared sibling observable(s) ['getDeterminant', 'getRealEigenvalues']; 3 required before agreement means anything — the screen would discard this reference after the twin build and two JVM runs, so it is discarded now instead
- detail: {'admitted_observables': [], 'last_step': 'reference too thin to screen — DISCARDED'}

---
## [53] ⚙️ reference-widening · `getV`
**output:** **reference REQUESTED for observable `getv`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [54] ⚙️ reference-impl · `getV`
**output:** **widening target requested**
- detail: {'candidates': ['getV'], 'attempting': 'getV'}

---
## [55] ⚙️ reference-impl · `getV`
**output:** **screening surface resolved**
- reason: 3 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'patch_touched_exempted': [], 'screened_siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'declaring_types': ['EigenDecompositionImpl']}

---
## [56] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~59,196 chars, 1 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
**[user]**
```
Implement `getV` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned { /* body withheld */ } and { /* body withheld */ } are always null.</p>
     * <p>When called with a { /* body withheld */ } argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the { /* body withheld */ },
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { /* body withheld */ }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    private void decompose() { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getV() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getD() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getVT() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getRealEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getImagEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { /* body withheld */ }
     * { /* body withheld */ }
    public DecompositionSolver getSolver() { /* body withheld */ }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { /* body withheld */ }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { /* body withheld */ }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) { /* body withheld */ }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { /* body withheld */ }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { /* body withheld */ }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { /* body withheld */ }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { /* body withheld */ }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { /* body withheld */ }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { /* body withheld */ }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { /* body withheld */ }
     * Find eigenvectors.
    private void findEigenVectors() { /* body withheld */ }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two { /* body withheld */ } and { /* body withheld */ } methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the { /* body withheld */ } constructor
     * or one of the <code>append</code> method ({ /* body withheld */ }, { /* body withheld */ }, { /* body withheld */ }) to gather data
     * into this vector.</p>
    public ArrayRealVector() { /* body withheld */ }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) { /* body withheld */ }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) { /* body withheld */ }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) { /* body withheld */ }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException { /* body withheld */ }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) { /* body withheld */ }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) { /* body withheld */ }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector copy() { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector mapExp() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testMathpbx02() {

        double[] mainTridiagonal = {
        	  7484.860960227216, 18405.28129035345, 13855.225609560746,
        	 10016.708722343366, 559.8117399576674, 6750.190788301587, 
        	    71.21428769782159
        };
        double[] secondaryTridiagonal = {
        	 -4175.088570476366,1975.7955858241994,5193.178422374075, 
        	  1995.286659169179,75.34535882933804,-234.0808002076056
        };

        // the reference values have been computed using routine DSTEMR
        // from the fortran library LAPACK version 3.2.1
        double[] refEigenValues = {
        		20654.744890306974412,16828.208208485466457,
        		6893.155912634994820,6757.083016675340332,
        		5887.799885688558788,64.309089923240379,
        		57.992628792736340
        };
        RealVector[] refEigenVectors = {
        		new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        		new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
        		new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
        		new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
        		new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
        		new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
        		new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };

        // the following line triggers the exception
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {
                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            } else {
                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            }
        }

    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned { /* body withheld */ } and { /* body withheld */ } are always null.</p>
     * <p>When called with a { /* body withheld */ } argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the { /* body withheld */ },
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { /* body withheld */ }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    private void decompose() { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getV() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getD() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getVT() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getRealEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getImagEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { /* body withheld */ }
     * { /* body withheld */ }
    public DecompositionSolver getSolver() { /* body withheld */ }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { /* body withheld */ }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { /* body withheld */ }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) { /* body withheld */ }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { /* body withheld */ }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { /* body withheld */ }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { /* body withheld */ }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { /* body withheld */ }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { /* body withheld */ }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { /* body withheld */ }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { /* body withheld */ }
     * Find eigenvectors.
    private void findEigenVectors() { /* body withheld */ }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two { /* body withheld */ } and { /* body withheld */ } methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the { /* body withheld */ } constructor
     * or one of the <code>append</code> method ({ /* body withheld */ }, { /* body withheld */ }, { /* body withheld */ }) to gather data
     * into this vector.</p>
    public ArrayRealVector() { /* body withheld */ }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) { /* body withheld */ }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) { /* body withheld */ }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) { /* body withheld */ }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException { /* body withheld */ }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) { /* body withheld */ }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) { /* body withheld */ }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector copy() { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector mapExp() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

<class name="MathUtils" role="test-subject">
     * Some useful additions to the built-in functions in {@link Math}.
     * @version $Revision$ $Date$
public final class MathUtils {
     * Smallest positive number such that 1 - EPSILON is not numerically equal to 1.
    public static final double EPSILON = 0x1.0p-53;
     * Safe minimum, such that 1 / SAFE_MIN does not overflow.
     * <p>In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.</p>
    public static final double SAFE_MIN = 0x1.0p-1022;
     * 2 &pi;.
    public static final double TWO_PI = 2 * Math.PI;
     * -1.0 cast as a byte.
    private static final byte NB = (byte)-1;
     * -1.0 cast as a short.
    private static final short NS = (short)-1;
     * 1.0 cast as a byte.
    private static final byte PB = (byte)1;
     * 1.0 cast as a short.
    private static final short PS = (short)1;
     * 0.0 cast as a byte.
    private static final byte ZB = (byte)0;
     * 0.0 cast as a short.
    private static final short ZS = (short)0;
     * Gap between NaN and regular numbers.
    private static final int NAN_GAP = 4 * 1024 * 1024;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * All long-representable factorials
    private static final long[] FACTORIALS = new long[] { /* body withheld */ }
     * Private Constructor
    private MathUtils() { /* body withheld */ }
     * Add two integers, checking for overflow.
     * @param x an addend
     * @param y an addend
     * @return the sum <code>x+y</code>
     * @throws ArithmeticException if the result can not be represented as an
     * int
     * @since 1.1
    public static int addAndCheck(int x, int y) { /* body withheld */ }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    public static long addAndCheck(long a, long b) { /* body withheld */ }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @param msg the message to use for any thrown exception.
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    private static long addAndCheck(long a, long b, String msg) { /* body withheld */ }
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which all coefficients are
     * …
    public static long binomialCoefficient(final int n, final int k) { /* body withheld */ }
     * Returns a <code>double</code> representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which all coefficients are <
     * …
    public static double binomialCoefficientDouble(final int n, final int k) { /* body withheld */ }
     * Returns the natural <code>log</code> of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * @param n the size of the set
     * …
    public static double binomialCoefficientLog(final int n, final int k) { /* body withheld */ }
     * Check binomial preconditions.
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @exception IllegalArgumentException if preconditions are not met.
    private static void checkBinomial(final int n, final int k) throws IllegalArgumentException { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * <li>&lt; 0 if !{ /* body withheld */ } &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{ /* body withheld */ } &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { /* body withheld */ }
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of x.
     * @param x double value for which to find the hyperbolic cosine
     * @return hyperbolic cosine of x
    public static double cosh(double x) { /* body withheld */ }
     * Returns true iff both arguments are NaN or neither is NaN and they are
     * equal
     * @param x first value
     * @param y second value
     * @return true if the values are equal or both are NaN
    public static boolean equals(double x, double y) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="InvalidMatrixException" role="collaborator">
     * Thrown when a system attempts an operation on a matrix, and
     * that matrix does not satisfy the preconditions for the
     * aforementioned operation.
     * @version $Revision$ $Date$
public class InvalidMatrixException extends MathRuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = 1135533765052675495L;
     * Construct an exception with the given message.
     * @param pattern format specifier
     * @param arguments format arguments
     * @since 2.0
    public InvalidMatrixException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Construct an exception with the given message.
     * @param cause the exception or error that caused this exception
     * to be thrown.
     * @since 2.0
    public InvalidMatrixException(final Throwable cause) { /* body withheld */ }
}
</class>

<class name="RealMatrix" role="collaborator">
     * Interface defining a real-valued matrix with basic algebraic operations.
     * <p>
     * Matrix element indexing is 0-based -- e.g., <code>getEntry(0, 0)</code>
     * returns the element in the first row, first column of the matrix.</p>
     * @version $Revision$ $Date$
public interface RealMatrix extends AnyMatrix {

}
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Revision$ $Date$
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
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
    @SuppressWarnings("unchecked") public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
     * @deprecated since 2.0, replaced by { /* body withheld */ }
    @Deprecated public static BigMatrix createBigIdentityMatrix(int dimension) { /* body withheld */ }
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
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.  The input array is copied, not referenced.
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
     * @throws NullPointerException if data is null
     * @deprecated since 2.0 replaced by { /* body withheld */ }
    @Deprecated public static BigMatrix createBigMatrix(double[][] data) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { /* body withheld */ }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { /* body withheld */ }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { /* body withheld */ }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { /* body withheld */ }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="SingularMatrixException" role="collaborator">
     * Thrown when a matrix is singular.
     * @version $Revision$ $Date$
     * @since 2.0
public class SingularMatrixException extends InvalidMatrixException {
     * Serializable version identifier.
    private static final long serialVersionUID = -7379143356784298432L;
     * Construct an exception with a default message.
    public SingularMatrixException() { /* body withheld */ }
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getDeterminant`, `getImagEigenvalues`, `getRealEigenvalues`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,798 chars)</summary>

```
// compute(double[], double[], ArrayRealVector[]) : getV, getDeterminant, getImagEigenvalues, getRealEigenvalues
public class ReferenceImpl {

    public static RealMatrix compute_getV(final double[] realEigenvalues,
                                          final double[] imagEigenvalues,
                                          final ArrayRealVector[] eigenvectors) {
        final int m = eigenvectors.length;
        final RealMatrix v = MatrixUtils.createRealMatrix(m, m);
        for (int k = 0; k < m; ++k) {
            for (int i = 0; i < m; ++i) {
                v.setEntry(i, k, eigenvectors[k].getEntry(i));
            }
        }
        return v;
    }

    public static double compute_getDeterminant(final double[] realEigenvalues,
                                                final double[] imagEigenvalues,
                                                final ArrayRealVector[] eigenvectors) {
        double determinant = 1.0;
        for (int i = 0; i < realEigenvalues.length; ++i) {
            determinant *= realEigenvalues[i];
        }
        return determinant;
    }

    public static double[] compute_getImagEigenvalues(final double[] realEigenvalues,
                                                      final double[] imagEigenvalues,
                                                      final ArrayRealVector[] eigenvectors) {
        return imagEigenvalues == null ? null : imagEigenvalues.clone();
    }

    public static double[] compute_getRealEigenvalues(final double[] realEigenvalues,
                                                      final double[] imagEigenvalues,
                                                      final ArrayRealVector[] eigenvectors) {
        return realEigenvalues == null ? null : realEigenvalues.clone();
    }
}
```

</details>

---
## [57] ⚙️ reference-impl · `getV`
**output:** **reference generated**
- detail: {'chars': 1798}

---
## [58] ⚙️ reference-impl · `getV`
**output:** **signature declared**
- detail: {'signature': 'double[], double[], ArrayRealVector[]'}

---
## [59] ⚙️ reference-impl · `getV`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'double[], double[], ArrayRealVector[]', 'named': 'double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors'}

---
## [60] ⚙️ reference-impl · `getV`
**output:** **reference observables matched**
- detail: {'matched': {'getV': 'getV', 'getDeterminant': 'getDeterminant', 'getImagEigenvalues': 'getImagEigenvalues', 'getRealEigenvalues': 'getRealEigenvalues'}, 'declared_only': []}

---
## [61] ⚙️ reference-impl · `getV`
**output:** **screen bar reachable**
- reason: 3 shared sibling observable(s) — enough for the screen to decide

---
## [62] ⚙️ reference-impl · `getV`
**output:** **signature mapped**
- reason: matched 3 parameter(s) to state fields

---
## [63] ⚙️ reference-impl · `getV`
**output:** **twin underivable — DISCARDED**
- reason: no receiver with type evidence: no variable both reaches the disputed observable and is declared with a type that declares it (declaring types: ['EigenDecompositionImpl']) — DISCARDED rather than guessed
- detail: {'declaring_types': ['EigenDecompositionImpl']}

---
## [64] ⚙️ reference-widening · `getV`
**output:** **widening REJECTED for observable `getv`**
- reason: twin underivable — DISCARDED — no receiver with type evidence: no variable both reaches the disputed observable and is declared with a type that declares it (declaring types: ['EigenDecompositionImpl']) — DISCARDED rather than guessed
- detail: {'admitted_observables': [], 'last_step': 'twin underivable — DISCARDED'}

---
## [65] ⚙️ reference-widening · `getD`
**output:** **reference REQUESTED for observable `getd`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [66] ⚙️ reference-impl · `getD`
**output:** **widening target requested**
- detail: {'candidates': ['getD'], 'attempting': 'getD'}

---
## [67] ⚙️ reference-impl · `getD`
**output:** **screening surface resolved**
- reason: 3 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'patch_touched_exempted': [], 'screened_siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'declaring_types': ['EigenDecompositionImpl']}

---
## [68] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~59,196 chars, 1 new)</summary>

- *[system] message: identical to step [47] — not reprinted*
**[user]**
```
Implement `getD` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned { /* body withheld */ } and { /* body withheld */ } are always null.</p>
     * <p>When called with a { /* body withheld */ } argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the { /* body withheld */ },
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { /* body withheld */ }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    private void decompose() { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getV() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getD() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getVT() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getRealEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getImagEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { /* body withheld */ }
     * { /* body withheld */ }
    public DecompositionSolver getSolver() { /* body withheld */ }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { /* body withheld */ }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { /* body withheld */ }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) { /* body withheld */ }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { /* body withheld */ }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { /* body withheld */ }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { /* body withheld */ }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { /* body withheld */ }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { /* body withheld */ }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { /* body withheld */ }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { /* body withheld */ }
     * Find eigenvectors.
    private void findEigenVectors() { /* body withheld */ }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two { /* body withheld */ } and { /* body withheld */ } methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the { /* body withheld */ } constructor
     * or one of the <code>append</code> method ({ /* body withheld */ }, { /* body withheld */ }, { /* body withheld */ }) to gather data
     * into this vector.</p>
    public ArrayRealVector() { /* body withheld */ }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) { /* body withheld */ }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) { /* body withheld */ }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) { /* body withheld */ }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException { /* body withheld */ }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) { /* body withheld */ }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) { /* body withheld */ }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector copy() { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector mapExp() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testMathpbx02() {

        double[] mainTridiagonal = {
        	  7484.860960227216, 18405.28129035345, 13855.225609560746,
        	 10016.708722343366, 559.8117399576674, 6750.190788301587, 
        	    71.21428769782159
        };
        double[] secondaryTridiagonal = {
        	 -4175.088570476366,1975.7955858241994,5193.178422374075, 
        	  1995.286659169179,75.34535882933804,-234.0808002076056
        };

        // the reference values have been computed using routine DSTEMR
        // from the fortran library LAPACK version 3.2.1
        double[] refEigenValues = {
        		20654.744890306974412,16828.208208485466457,
        		6893.155912634994820,6757.083016675340332,
        		5887.799885688558788,64.309089923240379,
        		57.992628792736340
        };
        RealVector[] refEigenVectors = {
        		new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        		new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
        		new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
        		new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
        		new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
        		new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
        		new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };

        // the following line triggers the exception
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {
                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            } else {
                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            }
        }

    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned { /* body withheld */ } and { /* body withheld */ } are always null.</p>
     * <p>When called with a { /* body withheld */ } argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the { /* body withheld */ },
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * { /* body withheld */ })
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException { /* body withheld */ }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { /* body withheld */ }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a { /* body withheld */ } if algorithm fails to converge
    private void decompose() { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getV() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getD() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public RealMatrix getVT() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getRealEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public double[] getImagEigenvalues() throws InvalidMatrixException { /* body withheld */ }
     * { /* body withheld */ }
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { /* body withheld */ }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { /* body withheld */ }
     * { /* body withheld */ }
    public DecompositionSolver getSolver() { /* body withheld */ }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { /* body withheld */ }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { /* body withheld */ }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) { /* body withheld */ }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { /* body withheld */ }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { /* body withheld */ }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { /* body withheld */ }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { /* body withheld */ }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { /* body withheld */ }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { /* body withheld */ }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { /* body withheld */ }
     * Find eigenvectors.
    private void findEigenVectors() { /* body withheld */ }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two { /* body withheld */ } and { /* body withheld */ } methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the { /* body withheld */ } constructor
     * or one of the <code>append</code> method ({ /* body withheld */ }, { /* body withheld */ }, { /* body withheld */ }) to gather data
     * into this vector.</p>
    public ArrayRealVector() { /* body withheld */ }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) { /* body withheld */ }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) { /* body withheld */ }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) { /* body withheld */ }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException { /* body withheld */ }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) { /* body withheld */ }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) { /* body withheld */ }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) { /* body withheld */ }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) { /* body withheld */ }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector copy() { /* body withheld */ }
     * { /* body withheld */ }
    public RealVector mapExp() { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

<class name="MathUtils" role="test-subject">
     * Some useful additions to the built-in functions in {@link Math}.
     * @version $Revision$ $Date$
public final class MathUtils {
     * Smallest positive number such that 1 - EPSILON is not numerically equal to 1.
    public static final double EPSILON = 0x1.0p-53;
     * Safe minimum, such that 1 / SAFE_MIN does not overflow.
     * <p>In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.</p>
    public static final double SAFE_MIN = 0x1.0p-1022;
     * 2 &pi;.
    public static final double TWO_PI = 2 * Math.PI;
     * -1.0 cast as a byte.
    private static final byte NB = (byte)-1;
     * -1.0 cast as a short.
    private static final short NS = (short)-1;
     * 1.0 cast as a byte.
    private static final byte PB = (byte)1;
     * 1.0 cast as a short.
    private static final short PS = (short)1;
     * 0.0 cast as a byte.
    private static final byte ZB = (byte)0;
     * 0.0 cast as a short.
    private static final short ZS = (short)0;
     * Gap between NaN and regular numbers.
    private static final int NAN_GAP = 4 * 1024 * 1024;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * All long-representable factorials
    private static final long[] FACTORIALS = new long[] { /* body withheld */ }
     * Private Constructor
    private MathUtils() { /* body withheld */ }
     * Add two integers, checking for overflow.
     * @param x an addend
     * @param y an addend
     * @return the sum <code>x+y</code>
     * @throws ArithmeticException if the result can not be represented as an
     * int
     * @since 1.1
    public static int addAndCheck(int x, int y) { /* body withheld */ }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    public static long addAndCheck(long a, long b) { /* body withheld */ }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @param msg the message to use for any thrown exception.
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    private static long addAndCheck(long a, long b, String msg) { /* body withheld */ }
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which all coefficients are
     * …
    public static long binomialCoefficient(final int n, final int k) { /* body withheld */ }
     * Returns a <code>double</code> representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which all coefficients are <
     * …
    public static double binomialCoefficientDouble(final int n, final int k) { /* body withheld */ }
     * Returns the natural <code>log</code> of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * @param n the size of the set
     * …
    public static double binomialCoefficientLog(final int n, final int k) { /* body withheld */ }
     * Check binomial preconditions.
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @exception IllegalArgumentException if preconditions are not met.
    private static void checkBinomial(final int n, final int k) throws IllegalArgumentException { /* body withheld */ }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  { /* body withheld */ }</li>
     * <li>&lt; 0 if !{ /* body withheld */ } &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{ /* body withheld */ } &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { /* body withheld */ }
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of x.
     * @param x double value for which to find the hyperbolic cosine
     * @return hyperbolic cosine of x
    public static double cosh(double x) { /* body withheld */ }
     * Returns true iff both arguments are NaN or neither is NaN and they are
     * equal
     * @param x first value
     * @param y second value
     * @return true if the values are equal or both are NaN
    public static boolean equals(double x, double y) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="InvalidMatrixException" role="collaborator">
     * Thrown when a system attempts an operation on a matrix, and
     * that matrix does not satisfy the preconditions for the
     * aforementioned operation.
     * @version $Revision$ $Date$
public class InvalidMatrixException extends MathRuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = 1135533765052675495L;
     * Construct an exception with the given message.
     * @param pattern format specifier
     * @param arguments format arguments
     * @since 2.0
    public InvalidMatrixException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Construct an exception with the given message.
     * @param cause the exception or error that caused this exception
     * to be thrown.
     * @since 2.0
    public InvalidMatrixException(final Throwable cause) { /* body withheld */ }
}
</class>

<class name="RealMatrix" role="collaborator">
     * Interface defining a real-valued matrix with basic algebraic operations.
     * <p>
     * Matrix element indexing is 0-based -- e.g., <code>getEntry(0, 0)</code>
     * returns the element in the first row, first column of the matrix.</p>
     * @version $Revision$ $Date$
public interface RealMatrix extends AnyMatrix {

}
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Revision$ $Date$
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
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
    @SuppressWarnings("unchecked") public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { /* body withheld */ }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
     * @deprecated since 2.0, replaced by { /* body withheld */ }
    @Deprecated public static BigMatrix createBigIdentityMatrix(int dimension) { /* body withheld */ }
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
     * Returns a { /* body withheld */ } whose entries are the the values in the
     * the input array.  The input array is copied, not referenced.
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
     * @throws NullPointerException if data is null
     * @deprecated since 2.0 replaced by { /* body withheld */ }
    @Deprecated public static BigMatrix createBigMatrix(double[][] data) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { /* body withheld */ }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { /* body withheld */ }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { /* body withheld */ }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getMessage() { /* body withheld */ }
     * { /* body withheld */ }
    @Override public String getLocalizedMessage() { /* body withheld */ }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { /* body withheld */ }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { /* body withheld */ }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to { /* body withheld */ }.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { /* body withheld */ }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { /* body withheld */ }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="SingularMatrixException" role="collaborator">
     * Thrown when a matrix is singular.
     * @version $Revision$ $Date$
     * @since 2.0
public class SingularMatrixException extends InvalidMatrixException {
     * Serializable version identifier.
    private static final long serialVersionUID = -7379143356784298432L;
     * Construct an exception with a default message.
    public SingularMatrixException() { /* body withheld */ }
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getDeterminant`, `getImagEigenvalues`, `getRealEigenvalues`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~2,091 chars)</summary>

```
// compute(double[]) : getD, getDeterminant, getImagEigenvalues, getRealEigenvalues
public class ReferenceImpl {

    public static double[] compute_getRealEigenvalues(final double[] realEigenvalues) {
        if (realEigenvalues == null) {
            return null;
        }
        return realEigenvalues.clone();
    }

    public static double[] compute_getImagEigenvalues(final double[] realEigenvalues) {
        if (realEigenvalues == null) {
            return null;
        }
        return new double[realEigenvalues.length];
    }

    public static double compute_getDeterminant(final double[] realEigenvalues) {
        double determinant = 1.0;
        for (int i = 0; i < realEigenvalues.length; ++i) {
            determinant *= realEigenvalues[i];
        }
        return determinant;
    }

    public static RealMatrix compute_getD(final double[] realEigenvalues) {
        if (realEigenvalues == null) {
            return null;
        }
        final int n = realEigenvalues.length;
        final double[][] data = new double[n][n];
        for (int i = 0; i < n; ++i) {
            data[i][i] = realEigenvalues[i];
        }
        return new RealMatrix(data);
    }

    public static final class RealMatrix {
        private final double[][] data;

        public RealMatrix(final double[][] data) {
            final int rows = data.length;
            this.data = new double[rows][];
            for (int i = 0; i < rows; ++i) {
                this.data[i] = data[i].clone();
            }
        }

        public int getRowDimension() {
            return data.length;
        }

        public int getColumnDimension() {
            return data.length == 0 ? 0 : data[0].length;
        }

        public double getEntry(final int row, final int column) {
            return data[row][column];
        }

        public double[][] getData() {
            final double[][] copy = new double[data.length][];
            for (int i = 0; i < data.length; ++i) {
                copy[i] = data[i].clone();
            }
            return copy;
        }
    }
}
```

</details>

---
## [69] ⚙️ reference-impl · `getD`
**output:** **reference generated**
- detail: {'chars': 2091}

---
## [70] ⚙️ reference-impl · `getD`
**output:** **signature declared**
- detail: {'signature': 'double[]'}

---
## [71] ⚙️ reference-impl · `getD`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'double[]', 'named': 'double[] realEigenvalues'}

---
## [72] ⚙️ reference-impl · `getD`
**output:** **reference observables matched**
- detail: {'matched': {'getD': 'getD', 'getDeterminant': 'getDeterminant', 'getImagEigenvalues': 'getImagEigenvalues', 'getRealEigenvalues': 'getRealEigenvalues'}, 'declared_only': []}

---
## [73] ⚙️ reference-impl · `getD`
**output:** **screen bar reachable**
- reason: 3 shared sibling observable(s) — enough for the screen to decide

---
## [74] ⚙️ reference-impl · `getD`
**output:** **signature mapped**
- reason: matched 1 parameter(s) to state fields

---
## [75] ⚙️ reference-impl · `getD`
**output:** **twin underivable — DISCARDED**
- reason: no receiver with type evidence: no variable both reaches the disputed observable and is declared with a type that declares it (declaring types: ['EigenDecompositionImpl']) — DISCARDED rather than guessed
- detail: {'declaring_types': ['EigenDecompositionImpl']}

---
## [76] ⚙️ reference-widening · `getD`
**output:** **widening REJECTED for observable `getd`**
- reason: twin underivable — DISCARDED — no receiver with type evidence: no variable both reaches the disputed observable and is declared with a type that declares it (declaring types: ['EigenDecompositionImpl']) — DISCARDED rather than guessed
- detail: {'admitted_observables': [], 'last_step': 'twin underivable — DISCARDED'}

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~87,800 chars)</summary>

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
- Package: `org.apache.commons.math.linear` (`package org.apache.commons.math.linear;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_80_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
+++ /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
@@ -1132,7 +1132,7 @@ public class EigenDecompositionImpl implements EigenDecomposition {
     private boolean flipIfWarranted(final int n, final int step) {
         if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
             // flip array
-            int j = 4 * n - 1;
+            int j = 4 * (n - 1);
             for (int i = 0; i < j; i += 4) {
                 for (int k = 0; k < 4; k += step) {
                     final double tmp = work[i + k];

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.util.MathUtils;
</source_imports>

Function `flipIfWarranted`:
<signature>
private boolean flipIfWarranted(int n, int step)
</signature>
<code>
    private boolean flipIfWarranted(final int n, final int step) {
        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
            // flip array
            int j = 4 * n - 1;
            for (int i = 0; i < j; i += 4) {
                for (int k = 0; k < 4; k += step) {
                    final double tmp = work[i + k];
                    work[i + k] = work[j - k];
                    work[j - k] = tmp;
                }
                j -= 4;
            }
            return true;
        }
        return false;
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
private void processGeneralBlock(final int n)
        throws InvalidMatrixException {

        // check decomposed matrix data range
        double sumOffDiag = 0;
        for (int i = 0; i < n - 1; ++i) {
            final int fourI = 4 * i;
            final double ei = work[fourI + 2];
            sumOffDiag += ei;
        }

        if (sumOffDiag == 0) {
            // matrix is already diagonal
            return;
        }

        // initial checks for splits (see Parlett & Marques section 3.3)
        flipIfWarranted(n, 2);

        // two iterations with Li's test for initial splits
        initialSplits(n);

        // initialize parameters used by goodStep
        tType = 0;
        dMin1 = 0;
        dMin2 = 0;
        dN    = 0;
        dN1   = 0;
        dN2   = 0;
        tau   = 0;

        // process split segments
        int i0 = 0;
        int n0 = n;
        while (n0 > 0) {

            // retrieve shift that was temporarily stored as a negative off-diagonal element
            sigma    = (n0 == n) ? 0 : -work[4 * n0 - 2];
            sigmaLow = 0;

            // find start of a new split segment to process
            double offDiagMin = (i0 == n0) ? 0 : work[4 * n0 - 6];
            double offDiagMax = 0;
            double diagMax    = work[4 * n0 - 4];
            double diagMin    = diagMax;
            i0 = 0;
            for (int i = 4 * (n0 - 2); i >= 0; i -= 4) {
                if (work[i + 2] <= 0) {
                    i0 = 1 + i / 4;
                    break;
                }
                if (diagMin >= 4 * offDiagMax) {
                    diagMin    = Math.min(diagMin, work[i + 4]);
                    offDiagMax = Math.max(offDiagMax, work[i + 2]);
                }
                diagMax    = Math.max(diagMax, work[i] + work[i + 2]);
                offDiagMin = Math.min(offDiagMin, work[i + 2]);
            }
            work[4 * n0 - 2] = offDiagMin;

            // lower bound of Gershgorin disk
            dMin = -Math.max(0, diagMin - 2 * Math.sqrt(diagMin * offDiagMax));

            pingPong = 0;
            int maxIter = 30 * (n0 - i0);
            for (int k = 0; i0 < n0; ++k) {
                if (k >= maxIter) {
                    throw new InvalidMatrixException(new MaxIterationsExceededException(maxIter));
                }

                // perform one step
                n0 = goodStep(i0, n0);
                pingPong = 1 - pingPong;

                // check for new splits after "ping" steps
                // when the last elements of qd array are very small
                if ((pingPong == 0) && (n0 - i0 > 3) &&
                    (work[4 * n0 - 1] <= TOLERANCE_2 * diagMax) &&
                    (work[4 * n0 - 2] <= TOLERANCE_2 * sigma)) {
                    int split  = i0 - 1;
                    diagMax    = work[4 * i0];
                    offDiagMin = work[4 * i0 + 2];
                    double previousEMin = work[4 * i0 + 3];
                    for (int i = 4 * i0; i < 4 * n0 - 16; i += 4) {
                        if ((work[i + 3] <= TOLERANCE_2 * work[i]) ||
                            (work[i + 2] <= TOLERANCE_2 * sigma)) {
                            // insert a split
                            work[i + 2]  = -sigma;
                            split        = i / 4;
                            diagMax      = 0;
                            offDiagMin   = work[i + 6];
                            previousEMin = work[i + 7];
                        } else {
                            diagMax      = Math.max(diagMax, work[i + 4]);
                            offDiagMin   = Math.min(offDiagMin, work[i + 2]);
                            previousEMin = Math.min(previousEMin, work[i + 3]);
                        }
                    }
                    work[4 * n0 - 2] = offDiagMin;
                    work[4 * n0 - 1] = previousEMin;
                    i0 = split + 1;
                }
            }

        }

    }
</xref>
STATE COUPLING: these members of the same class share the listed field(s) with `flipIfWarranted` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `flipIfWarranted`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `private void processGeneralBlock(int n) throws InvalidMatrixException` (shared field(s): pingPong, work)
      doc: Find realEigenvalues using dqd/dqds algorithms. <p>This implementation is based on Beresford N. Parlett and Osni A. Marques paper <a href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An Implementation of the dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routine DLASQ2.</p> @param n number of…
  - `private void initialSplits(int n)` (shared field(s): pingPong, work)
      doc: Perform two iterations with Li's tests for initial splits. @param n number of rows of the matrix to process
  - `private int goodStep(int start, int end)` (shared field(s): pingPong, work)
      doc: Perform one "good" dqd/dqds step. <p>This implementation is based on Beresford N. Parlett and Osni A. Marques paper <a href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An Implementation of the dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routine DLAZQ3.</p> @param start start index @param …
  - `private void dqds(int start, int end)` (shared field(s): pingPong, work)
      doc: Perform a dqds step, using current shift increment. <p>This implementation is a translation of the LAPACK routine DLASQ5.</p> @param start start index @param end end index
  - `private void dqd(int start, int end)` (shared field(s): pingPong, work)
      doc: Perform a dqd step. <p>This implementation is a translation of the LAPACK routine DLASQ6.</p> @param start start index @param end end index

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(final double[] b) / (final RealVector b) / (final RealMatrix b)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getD, getDeterminant, getEigenvector, getImagEigenvalue, getImagEigenvalues, getInverse, getRealEigenvalue, getRealEigenvalues, getSolver, getV, getVT

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getD, getDeterminant, getImagEigenvalues, getInverse, getRealEigenvalues, getSolver, getV, getVT, isNonSingular

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `EigenDecompositionImpl`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math.linear.EigenDecompositionImplTest" method="testMathpbx02">
    public void testMathpbx02() {

        double[] mainTridiagonal = {
        	  7484.860960227216, 18405.28129035345, 13855.225609560746,
        	 10016.708722343366, 559.8117399576674, 6750.190788301587, 
        	    71.21428769782159
        };
        double[] secondaryTridiagonal = {
        	 -4175.088570476366,1975.7955858241994,5193.178422374075, 
        	  1995.286659169179,75.34535882933804,-234.0808002076056
        };

        // the reference values have been computed using routine DSTEMR
        // from the fortran library LAPACK version 3.2.1
        double[] refEigenValues = {
        		20654.744890306974412,16828.208208485466457,
        		6893.155912634994820,6757.083016675340332,
        		5887.799885688558788,64.309089923240379,
        		57.992628792736340
        };
        RealVector[] refEigenVectors = {
        		new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        		new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
        		new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
        		new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
        		new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
        		new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
        		new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };

        // the following line triggers the exception
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {
                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            } else {
                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            }
        }

    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.linear.EigenDecompositionImplTest::testMathpbx02
junit.framework.AssertionFailedError: expected:<16828.208208485466> but was:<20654.74511575945>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.linear.EigenDecompositionImplTest">
// --- setUp() (test setup) ---
    @Override
    public void setUp() {
        refValues = new double[] {
                2.003, 2.002, 2.001, 1.001, 1.000, 0.001
        };
        matrix = createTestMatrix(new Random(35992629946426l), refValues);
    }

// --- class fields/constants the test uses ---
private double[] refValues;
private RealMatrix matrix;
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
<relation name="imaginary-eigenvalue-array-is-null-for-symmetric-input" kind="invariant">
  // holds because: For this symmetric-only implementation, 'the imaginary values returned getImagEigenvalue(int) and getImagEigenvalues() are always null.' Therefore getImagEigenvalues() must return null for every valid decomposition produced by this class.
  // valid input: Choose any valid tridiagonal main and secondary arrays with lengths n and n-1, n in [1,8].
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 20000/20000 on random inputs
int n = data.consumeInt(1, 8);
double[] main = new double[n];
for (int i = 0; i < n; i++) {
  main[i] = data.consumeInt(-200000, 200000) / 64.0;
}
double[] secondary = new double[Math.max(0, n - 1)];
for (int i = 0; i < secondary.length; i++) {
  secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
}
org.apache.commons.math.linear.EigenDecompositionImpl dec;
double[] imag;
try {
  dec = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("EigenDecompositionImpl") || __rpgc.endsWith(".EigenDecompositionImpl") || __rpgc.endsWith("$EigenDecompositionImpl")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "decompose".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "findEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getD".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getEigenvector".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getInverse".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getV".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getVT".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process2RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process3RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "processGeneralBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "solve".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
try {
  imag = dec.getImagEigenvalues();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("EigenDecompositionImpl") || __rpgc.endsWith(".EigenDecompositionImpl") || __rpgc.endsWith("$EigenDecompositionImpl")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "decompose".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "findEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getD".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getEigenvector".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getInverse".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getV".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getVT".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process2RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process3RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "processGeneralBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "solve".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: unexpected " + e.getClass().getName() + " from getImagEigenvalues: " + e.getMessage());
}
if (imag != null) {
  throw new RuntimeException("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
}
</relation>
<relation name="reversed-tridiagonal-preserves-eigenvalue-multiset" kind="metamorphic">
  // holds because: The class calculates the eigen decomposition of a symmetric matrix. Reversing the order of rows and columns of a symmetric tridiagonal matrix is a permutation similarity transform, so any correct decomposition must produce the same real eigenvalues (as a multiset) for the original and the reversed tridiagonal input.
  // valid input: Choose n in [1,8]. Build a valid symmetric tridiagonal matrix by constructing main[0..n-1] and secondary[0..n-2] from fuzzed ints scaled to doubles. Build the reversed tridiagonal representation with main and secondary both reversed. Both inputs are valid by construction for the tridiagonal constructor.
  // screen: fired on 374/20000 fuzzed inputs on the buggy build (selective — consistent with the defect region)
int n = data.consumeInt(1, 8);
double[] main = new double[n];
for (int i = 0; i < n; i++) {
  main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
}
double[] secondary = new double[Math.max(0, n - 1)];
for (int i = 0; i < secondary.length; i++) {
  secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
}
double[] rMain = new double[n];
double[] rSecondary = new double[Math.max(0, n - 1)];
for (int i = 0; i < n; i++) {
  rMain[i] = main[n - 1 - i];
}
for (int i = 0; i < rSecondary.length; i++) {
  rSecondary[i] = secondary[rSecondary.length - 1 - i];
}
org.apache.commons.math.linear.EigenDecompositionImpl d1;
org.apache.commons.math.linear.EigenDecompositionImpl d2;
double[] ev1;
double[] ev2;
try {
  d1 = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("EigenDecompositionImpl") || __rpgc.endsWith(".EigenDecompositionImpl") || __rpgc.endsWith("$EigenDecompositionImpl")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "decompose".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "findEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getD".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getEigenvector".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getInverse".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getV".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getVT".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process2RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process3RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "processGeneralBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "solve".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
try {
  d2 = new org.apache.commons.math.linear.EigenDecompositionImpl(rMain, rSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("EigenDecompositionImpl") || __rpgc.endsWith(".EigenDecompositionImpl") || __rpgc.endsWith("$EigenDecompositionImpl")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "decompose".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "findEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getD".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getEigenvector".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getInverse".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getV".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getVT".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process2RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process3RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "processGeneralBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "solve".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected " + e.getClass().getName() + " on valid-by-construction reversed input: " + e.getMessage());
}
try {
  ev1 = d1.getRealEigenvalues();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("EigenDecompositionImpl") || __rpgc.endsWith(".EigenDecompositionImpl") || __rpgc.endsWith("$EigenDecompositionImpl")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "decompose".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "findEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getD".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getEigenvector".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getInverse".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getV".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getVT".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process2RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process3RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "processGeneralBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "solve".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected " + e.getClass().getName() + " from getRealEigenvalues on original input: " + e.getMessage());
}
try {
  ev2 = d2.getRealEigenvalues();
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("EigenDecompositionImpl") || __rpgc.endsWith(".EigenDecompositionImpl") || __rpgc.endsWith("$EigenDecompositionImpl")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "decompose".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "findEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getD".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getEigenvector".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getImagEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getInverse".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArrayIndexOutOfBoundsException") || __rpn.endsWith(".ArrayIndexOutOfBoundsException") || __rpn.endsWith("$ArrayIndexOutOfBoundsException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getRealEigenvalues".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getV".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "getVT".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process2RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "process3RowsBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "processGeneralBlock".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("EigenDecompositionImpl") || __rpgf.getClassName().endsWith(".EigenDecompositionImpl") || __rpgf.getClassName().endsWith("$EigenDecompositionImpl")) && "solve".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException") || __rpn.equals("InvalidMatrixException") || __rpn.endsWith(".InvalidMatrixException") || __rpn.endsWith("$InvalidMatrixException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
  throw new RuntimeException("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: unexpected " + e.getClass().getName() + " from getRealEigenvalues on reversed input: " + e.getMessage());
}
if (ev1.length != ev2.length) {
  throw new RuntimeException("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + ev1.length + " vs " + ev2.length);
}
double[] a = ev1.clone();
double[] b = ev2.clone();
java.util.Arrays.sort(a);
java.util.Arrays.sort(b);
for (int i = 0; i < a.length; i++) {
  double x = a[i];
  double y = b[i];
  double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
  if (!(Math.abs(x - y) <= tol)) {
    throw new RuntimeException("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
  }
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
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~10,734 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryNullInvariant(data);
        runReversalMetamorphic(data);
        runReadOnlyStabilityMetamorphic(data);
    }

    private static void runLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };
        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        final EigenDecomposition decomposition;
        final double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = refEigenVectors[i].dotProduct(actualVector);
            double norm = dot < 0 ? refEigenVectors[i].add(actualVector).getNorm() : refEigenVectors[i].subtract(actualVector).getNorm();
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToReference=0.0 actualNorm=" + norm + " tol=1.0E-5");
            }
        }

        // Contract/oracle from this symmetric-only implementation: imaginary eigenvalues are always null for valid decompositions.
        double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }

        // Hidden-state/read-only check: repeated getters should agree and not mutate reported state.
        // A patch that merely skips or corrupts flip bookkeeping can leave internal work state inconsistent; a read-only getter exposing different answers across calls violates the decomposition contract.
        double[] first = eigenValues.clone();
        double[] second;
        try {
            second = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (second == null || second.length != first.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: firstLength=" + first.length + " secondLength=" + (second == null ? -1 : second.length));
        }
        for (int i = 0; i < first.length; i++) {
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: index=" + i + " first=" + first[i] + " second=" + second[i]);
            }
        }
    }

    private static void runImaginaryNullInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: actualLength=" + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN).getRealEigenvalues();
            ev2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN).getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: len1=" + (ev1 == null ? -1 : ev1.length) + " len2=" + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }

    private static void runReadOnlyStabilityMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-250000, 250000) / 32.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-250000, 250000) / 32.0;
        }

        EigenDecompositionImpl dec;
        double[] evA;
        double[] evB;
        double detA;
        double detB;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            evA = dec.getRealEigenvalues();
            detA = dec.getDeterminant();
            evB = dec.getRealEigenvalues();
            detB = dec.getDeterminant();
        } catch (Throwable t) {
            return;
        }

        // Read-only/post-condition guarantee: get* methods are observers. A throw-deleting or bookkeeping-skipping patch can leave hidden state such that successive reads disagree.
        if (evA == null || evB == null || evA.length != evB.length) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues] metamorphic violation: lenA=" + (evA == null ? -1 : evA.length) + " lenB=" + (evB == null ? -1 : evB.length));
        }
        for (int i = 0; i < evA.length; i++) {
            if (Double.doubleToLongBits(evA[i]) != Double.doubleToLongBits(evB[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues] metamorphic violation: index=" + i + " first=" + evA[i] + " second=" + evB[i]);
            }
        }
        if (Double.doubleToLongBits(detA) != Double.doubleToLongBits(detB)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-determinant] metamorphic violation: first=" + detA + " second=" + detB);
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 57 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEig

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,475 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryNullInvariant(data);
        runReversalMetamorphic(data);
        runReadOnlyStabilityMetamorphic(data);
    }

    private static void runLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };
        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        final EigenDecomposition decomposition;
        final double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = refEigenVectors[i].dotProduct(actualVector);
            double norm = dot < 0 ? refEigenVectors[i].add(actualVector).getNorm() : refEigenVectors[i].subtract(actualVector).getNorm();
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToReference=0.0 actualNorm=" + norm + " tol=1.0E-5");
            }
        }

        // Contract/oracle from this symmetric-only implementation: imaginary eigenvalues are always null for valid decompositions.
        double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }

        // Hidden-state/read-only check: repeated getters should agree and not mutate reported state.
        // A patch that merely skips or corrupts flip bookkeeping can leave internal work state inconsistent; a read-only getter exposing different answers across calls violates the decomposition contract.
        double[] first = eigenValues.clone();
        double[] second;
        try {
            second = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (second == null || second.length != first.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: firstLength=" + first.length + " secondLength=" + (second == null ? -1 : second.length));
        }
        for (int i = 0; i < first.length; i++) {
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: index=" + i + " first=" + first[i] + " second=" + second[i]);
            }
        }
    }

    private static void runImaginaryNullInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: actualLength=" + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN).getRealEigenvalues();
            ev2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN).getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: len1=" + (ev1 == null ? -1 : ev1.length) + " len2=" + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }

    private static void runReadOnlyStabilityMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-250000, 250000) / 32.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-250000, 250000) / 32.0;
        }

        EigenDecompositionImpl dec;
        double[] evA;
        double[] evB;
        double detA;
        double detB;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            evA = dec.getRealEigenvalues();
            detA = dec.getDeterminant();
            evB = dec.getRealEigenvalues();
            detB = dec.getDeterminant();
        } catch (Throwable t) {
            return;
        }

        // Read-only/post-condition guarantee: get* methods are observers. A throw-deleting or bookkeeping-skipping patch can leave hidden state such that successive reads disagree.
        if (evA == null || evB == null || evA.length != evB.length) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues] metamorphic violation: lenA=" + (evA == null ? -1 : evA.length) + " lenB=" + (evB == null ? -1 : evB.length));
        }
        for (int i = 0; i < evA.length; i++) {
            if (Double.doubleToLongBits(evA[i]) != Double.doubleToLongBits(evB[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues] metamorphic violation: index=" + i + " first=" + evA[i] + " second=" + evB[i]);
            }
        }
        if (Double.doubleToLongBits(detA) != Double.doubleToLongBits(detB)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-determinant] metamorphic violation: first=" + detA + " second=" + detB);
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 57 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEigenValues[i]...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~10,833 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryNullInvariant(data);
        runReversalMetamorphic(data);
        runReadOnlyStabilityMetamorphic(data);
    }

    private static void runLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };
        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        final EigenDecomposition decomposition;
        final double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = refEigenVectors[i].dotProduct(actualVector);
            double norm = dot < 0 ? refEigenVectors[i].add(actualVector).getNorm() : refEigenVectors[i].subtract(actualVector).getNorm();
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToReference=0.0 actualNorm=" + norm + " tol=1.0E-5");
            }
        }

        // Contract/oracle from this symmetric-only implementation: imaginary eigenvalues are always null for valid decompositions.
        double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }

        // Hidden-state/read-only check: repeated getters should agree and not mutate reported state.
        // A patch that merely skips or corrupts flip bookkeeping can leave internal work state inconsistent; a read-only getter exposing different answers across calls violates the decomposition contract.
        double[] first = eigenValues.clone();
        double[] second;
        try {
            second = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (second == null || second.length != first.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: firstLength=" + first.length + " secondLength=" + (second == null ? -1 : second.length));
        }
        for (int i = 0; i < first.length; i++) {
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: index=" + i + " first=" + first[i] + " second=" + second[i]);
            }
        }
    }

    private static void runImaginaryNullInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null-invariant] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: actualLength=" + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN).getRealEigenvalues();
            ev2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN).getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reversal-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: len1=" + (ev1 == null ? -1 : ev1.length) + " len2=" + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reversal-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }

    private static void runReadOnlyStabilityMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-250000, 250000) / 32.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-250000, 250000) / 32.0;
        }

        EigenDecompositionImpl dec;
        double[] evA;
        double[] evB;
        double detA;
        double detB;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            evA = dec.getRealEigenvalues();
            detA = dec.getDeterminant();
            evB = dec.getRealEigenvalues();
            detB = dec.getDeterminant();
        } catch (Throwable t) {
            return;
        }

        // Read-only/post-condition guarantee: get* methods are observers. A throw-deleting or bookkeeping-skipping patch can leave hidden state such that successive reads disagree.
        if (evA == null || evB == null || evA.length != evB.length) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-length] metamorphic violation: lenA=" + (evA == null ? -1 : evA.length) + " lenB=" + (evB == null ? -1 : evB.length));
        }
        for (int i = 0; i < evA.length; i++) {
            if (Double.doubleToLongBits(evA[i]) != Double.doubleToLongBits(evB[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-value] metamorphic violation: index=" + i + " first=" + evA[i] + " second=" + evB[i]);
            }
        }
        if (Double.doubleToLongBits(detA) != Double.doubleToLongBits(detB)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-determinant] metamorphic violation: first=" + detA + " second=" + detB);
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [80] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 57 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEig

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~11,154 chars, 1 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [79] — not reprinted*
- *[user] message: identical to step [79] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryNullInvariant(data);
        runReversalMetamorphic(data);
        runReadOnlyStabilityMetamorphic(data);
    }

    private static void runLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };
        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        final EigenDecomposition decomposition;
        final double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = refEigenVectors[i].dotProduct(actualVector);
            double norm = dot < 0 ? refEigenVectors[i].add(actualVector).getNorm() : refEigenVectors[i].subtract(actualVector).getNorm();
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToReference=0.0 actualNorm=" + norm + " tol=1.0E-5");
            }
        }

        // Contract/oracle from this symmetric-only implementation: imaginary eigenvalues are always null for valid decompositions.
        double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }

        // Hidden-state/read-only check: repeated getters should agree and not mutate reported state.
        // A patch that merely skips or corrupts flip bookkeeping can leave internal work state inconsistent; a read-only getter exposing different answers across calls violates the decomposition contract.
        double[] first = eigenValues.clone();
        double[] second;
        try {
            second = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (second == null || second.length != first.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: firstLength=" + first.length + " secondLength=" + (second == null ? -1 : second.length));
        }
        for (int i = 0; i < first.length; i++) {
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues] metamorphic violation: index=" + i + " first=" + first[i] + " second=" + second[i]);
            }
        }
    }

    private static void runImaginaryNullInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null-invariant] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: actualLength=" + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN).getRealEigenvalues();
            ev2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN).getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reversal-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: len1=" + (ev1 == null ? -1 : ev1.length) + " len2=" + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reversal-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }

    private static void runReadOnlyStabilityMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-250000, 250000) / 32.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-250000, 250000) / 32.0;
        }

        EigenDecompositionImpl dec;
        double[] evA;
        double[] evB;
        double detA;
        double detB;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            evA = dec.getRealEigenvalues();
            detA = dec.getDeterminant();
            evB = dec.getRealEigenvalues();
            detB = dec.getDeterminant();
        } catch (Throwable t) {
            return;
        }

        // Read-only/post-condition guarantee: get* methods are observers. A throw-deleting or bookkeeping-skipping patch can leave hidden state such that successive reads disagree.
        if (evA == null || evB == null || evA.length != evB.length) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-length] metamorphic violation: lenA=" + (evA == null ? -1 : evA.length) + " lenB=" + (evB == null ? -1 : evB.length));
        }
        for (int i = 0; i < evA.length; i++) {
            if (Double.doubleToLongBits(evA[i]) != Double.doubleToLongBits(evB[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-value] metamorphic violation: index=" + i + " first=" + evA[i] + " second=" + evB[i]);
            }
        }
        if (Double.doubleToLongBits(detA) != Double.doubleToLongBits(detB)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-determinant] metamorphic violation: first=" + detA + " second=" + detB);
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```
- *[user] message: identical to step [79] — not reprinted*

</details>
<details open><summary>▸ Output (~10,428 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryNullInvariant(data);
        runReversalMetamorphic(data);
        runReadOnlyStabilityMetamorphic(data);
    }

    private static void runLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };
        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        final EigenDecomposition decomposition;
        final double[] eigenValues;
        try {
            decomposition = new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues == null || eigenValues.length != refEigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + refEigenValues.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!withinTol(refEigenValues[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-seed] semantic mismatch: index=" + i + " expected=" + refEigenValues[i] + " actual=" + eigenValues[i] + " tol=0.001");
            }
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = refEigenVectors[i].dotProduct(actualVector);
            double norm = dot < 0 ? refEigenVectors[i].add(actualVector).getNorm() : refEigenVectors[i].subtract(actualVector).getNorm();
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-seed] semantic mismatch: index=" + i + " expectedNormToReference=0.0 actualNorm=" + norm + " tol=1.0E-5");
            }
        }

        double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }

        double[] first = eigenValues.clone();
        double[] second;
        try {
            second = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (second == null || second.length != first.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues-length] metamorphic violation: firstLength=" + first.length + " secondLength=" + (second == null ? -1 : second.length));
        }
        for (int i = 0; i < first.length; i++) {
            if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-repeat-getRealEigenvalues-value] metamorphic violation: index=" + i + " first=" + first[i] + " second=" + second[i]);
            }
        }
    }

    private static void runImaginaryNullInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null-invariant] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: actualLength=" + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            ev1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN).getRealEigenvalues();
            ev2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN).getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reversal-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: len1=" + (ev1 == null ? -1 : ev1.length) + " len2=" + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reversal-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" + i + " lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }

    private static void runReadOnlyStabilityMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-250000, 250000) / 32.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-250000, 250000) / 32.0;
        }

        EigenDecompositionImpl dec;
        double[] evA;
        double[] evB;
        double detA;
        double detB;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            evA = dec.getRealEigenvalues();
            detA = dec.getDeterminant();
            evB = dec.getRealEigenvalues();
            detB = dec.getDeterminant();
        } catch (Throwable t) {
            return;
        }

        // Read-only/post-condition guarantee: get* methods are observers. A throw-deleting or bookkeeping-skipping patch can leave hidden state such that successive reads disagree.
        if (evA == null || evB == null || evA.length != evB.length) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-length] metamorphic violation: lenA=" + (evA == null ? -1 : evA.length) + " lenB=" + (evB == null ? -1 : evB.length));
        }
        for (int i = 0; i < evA.length; i++) {
            if (Double.doubleToLongBits(evA[i]) != Double.doubleToLongBits(evB[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-eigenvalues-value] metamorphic violation: index=" + i + " first=" + evA[i] + " second=" + evB[i]);
            }
        }
        if (Double.doubleToLongBits(detA) != Double.doubleToLongBits(detB)) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-stability-determinant] metamorphic violation: first=" + detA + " second=" + detB);
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [82] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalue-seed] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945 tol=0.001

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~67,064 chars, 1 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.linear` (`package org.apache.commons.math.linear;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_80_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
+++ /src/main/java/org/apache/commons/math/linear/EigenDecompositionImpl.java
@@ -1132,7 +1132,7 @@ public class EigenDecompositionImpl implements EigenDecomposition {
     private boolean flipIfWarranted(final int n, final int step) {
         if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
             // flip array
-            int j = 4 * n - 1;
+            int j = 4 * (n - 1);
             for (int i = 0; i < j; i += 4) {
                 for (int k = 0; k < 4; k += step) {
                     final double tmp = work[i + k];

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.util.MathUtils;
</source_imports>

Function `flipIfWarranted`:
<signature>
private boolean flipIfWarranted(int n, int step)
</signature>
<code>
    private boolean flipIfWarranted(final int n, final int step) {
        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
            // flip array
            int j = 4 * n - 1;
            for (int i = 0; i < j; i += 4) {
                for (int k = 0; k < 4; k += step) {
                    final double tmp = work[i + k];
                    work[i + k] = work[j - k];
                    work[j - k] = tmp;
                }
                j -= 4;
            }
            return true;
        }
        return false;
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
private void processGeneralBlock(final int n)
        throws InvalidMatrixException {

        // check decomposed matrix data range
        double sumOffDiag = 0;
        for (int i = 0; i < n - 1; ++i) {
            final int fourI = 4 * i;
            final double ei = work[fourI + 2];
            sumOffDiag += ei;
        }

        if (sumOffDiag == 0) {
            // matrix is already diagonal
            return;
        }

        // initial checks for splits (see Parlett & Marques section 3.3)
        flipIfWarranted(n, 2);

        // two iterations with Li's test for initial splits
        initialSplits(n);

        // initialize parameters used by goodStep
        tType = 0;
        dMin1 = 0;
        dMin2 = 0;
        dN    = 0;
        dN1   = 0;
        dN2   = 0;
        tau   = 0;

        // process split segments
        int i0 = 0;
        int n0 = n;
        while (n0 > 0) {

            // retrieve shift that was temporarily stored as a negative off-diagonal element
            sigma    = (n0 == n) ? 0 : -work[4 * n0 - 2];
            sigmaLow = 0;

            // find start of a new split segment to process
            double offDiagMin = (i0 == n0) ? 0 : work[4 * n0 - 6];
            double offDiagMax = 0;
            double diagMax    = work[4 * n0 - 4];
            double diagMin    = diagMax;
            i0 = 0;
            for (int i = 4 * (n0 - 2); i >= 0; i -= 4) {
                if (work[i + 2] <= 0) {
                    i0 = 1 + i / 4;
                    break;
                }
                if (diagMin >= 4 * offDiagMax) {
                    diagMin    = Math.min(diagMin, work[i + 4]);
                    offDiagMax = Math.max(offDiagMax, work[i + 2]);
                }
                diagMax    = Math.max(diagMax, work[i] + work[i + 2]);
                offDiagMin = Math.min(offDiagMin, work[i + 2]);
            }
            work[4 * n0 - 2] = offDiagMin;

            // lower bound of Gershgorin disk
            dMin = -Math.max(0, diagMin - 2 * Math.sqrt(diagMin * offDiagMax));

            pingPong = 0;
            int maxIter = 30 * (n0 - i0);
            for (int k = 0; i0 < n0; ++k) {
                if (k >= maxIter) {
                    throw new InvalidMatrixException(new MaxIterationsExceededException(maxIter));
                }

                // perform one step
                n0 = goodStep(i0, n0);
                pingPong = 1 - pingPong;

                // check for new splits after "ping" steps
                // when the last elements of qd array are very small
                if ((pingPong == 0) && (n0 - i0 > 3) &&
                    (work[4 * n0 - 1] <= TOLERANCE_2 * diagMax) &&
                    (work[4 * n0 - 2] <= TOLERANCE_2 * sigma)) {
                    int split  = i0 - 1;
                    diagMax    = work[4 * i0];
                    offDiagMin = work[4 * i0 + 2];
                    double previousEMin = work[4 * i0 + 3];
                    for (int i = 4 * i0; i < 4 * n0 - 16; i += 4) {
                        if ((work[i + 3] <= TOLERANCE_2 * work[i]) ||
                            (work[i + 2] <= TOLERANCE_2 * sigma)) {
                            // insert a split
                            work[i + 2]  = -sigma;
                            split        = i / 4;
                            diagMax      = 0;
                            offDiagMin   = work[i + 6];
                            previousEMin = work[i + 7];
                        } else {
                            diagMax      = Math.max(diagMax, work[i + 4]);
                            offDiagMin   = Math.min(offDiagMin, work[i + 2]);
                            previousEMin = Math.min(previousEMin, work[i + 3]);
                        }
                    }
                    work[4 * n0 - 2] = offDiagMin;
                    work[4 * n0 - 1] = previousEMin;
                    i0 = split + 1;
                }
            }

        }

    }
</xref>
STATE COUPLING: these members of the same class share the listed field(s) with `flipIfWarranted` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `flipIfWarranted`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `private void processGeneralBlock(int n) throws InvalidMatrixException` (shared field(s): pingPong, work)
      doc: Find realEigenvalues using dqd/dqds algorithms. <p>This implementation is based on Beresford N. Parlett and Osni A. Marques paper <a href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An Implementation of the dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routine DLASQ2.</p> @param n number of…
  - `private void initialSplits(int n)` (shared field(s): pingPong, work)
      doc: Perform two iterations with Li's tests for initial splits. @param n number of rows of the matrix to process
  - `private int goodStep(int start, int end)` (shared field(s): pingPong, work)
      doc: Perform one "good" dqd/dqds step. <p>This implementation is based on Beresford N. Parlett and Osni A. Marques paper <a href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An Implementation of the dqds Algorithm (Positive Case)</a> and on the corresponding LAPACK routine DLAZQ3.</p> @param start start index @param …
  - `private void dqds(int start, int end)` (shared field(s): pingPong, work)
      doc: Perform a dqds step, using current shift increment. <p>This implementation is a translation of the LAPACK routine DLASQ5.</p> @param start start index @param end end index
  - `private void dqd(int start, int end)` (shared field(s): pingPong, work)
      doc: Perform a dqd step. <p>This implementation is a translation of the LAPACK routine DLASQ6.</p> @param start start index @param end end index

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="EigenDecompositionImpl" role="patched">
     * Calculates the eigen decomposition of a <strong>symmetric</strong> matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V D V<sup>T</sup>. A, V and D are all m &times; m
     * matrices.</p>
     * <p>As of 2.0, this class supports only <strong>symmetric</strong> matrices,
     * and hence computes only real realEigenvalues. This implies the D matrix returned by
     * {@link #getD()} is always diagonal and the imaginary values returned {@link
     * #getImagEigenvalue(int)} and {@link #getImagEigenvalues()} are always null.</p>
     * <p>When called with a {@link RealMatrix} argument, this implementation only uses
     * the upper part of the matrix, the part below the diagonal is not accessed at all.</p>
     * <p>Eigenvalues are computed as soon as the matrix is decomposed, but eigenvectors
     * are computed only when required, i.e. only when one of the {@link #getEigenvector(int)},
     * …
public class EigenDecompositionImpl implements EigenDecomposition {
     * Tolerance.
    private static final double TOLERANCE = 100 * MathUtils.EPSILON;
     * Squared tolerance.
    private static final double TOLERANCE_2 = TOLERANCE * TOLERANCE;
     * Split tolerance.
    private double splitTolerance;
     * Main diagonal of the tridiagonal matrix.
    private double[] main;
     * Secondary diagonal of the tridiagonal matrix.
    private double[] secondary;
     * Squared secondary diagonal of the tridiagonal matrix.
    private double[] squaredSecondary;
     * Transformer to tridiagonal (may be null if matrix is already tridiagonal).
    private TriDiagonalTransformer transformer;
     * Lower bound of spectra.
    private double lowerSpectra;
     * Upper bound of spectra.
    private double upperSpectra;
     * Minimum pivot in the Sturm sequence.
    private double minPivot;
     * Current shift.
    private double sigma;
     * Low part of the current shift.
    private double sigmaLow;
     * Shift increment to apply.
    private double tau;
     * Work array for all decomposition algorithms.
    private double[] work;
     * Shift within qd array for ping-pong implementation.
    private int pingPong;
     * Max value of diagonal elements in current segment.
    private double qMax;
     * Min value of off-diagonal elements in current segment.
    private double eMin;
     * Type of the last dqds shift.
    private int tType;
     * Minimal value on current state of the diagonal.
    private double dMin;
     * Minimal value on current state of the diagonal, excluding last element.
    private double dMin1;
     * Minimal value on current state of the diagonal, excluding last two elements.
    private double dMin2;
     * Last value on current state of the diagonal.
    private double dN;
     * Last but one value on current state of the diagonal.
    private double dN1;
     * Last but two on current state of the diagonal.
    private double dN2;
     * Shift ratio with respect to dMin used when tType == 6.
    private double g;
     * Real part of the realEigenvalues.
    private double[] realEigenvalues;
     * Imaginary part of the realEigenvalues.
    private double[] imagEigenvalues;
     * Eigenvectors.
    private ArrayRealVector[] eigenvectors;
     * Cached value of V.
    private RealMatrix cachedV;
     * Cached value of D.
    private RealMatrix cachedD;
     * Cached value of Vt.
    private RealMatrix cachedVt;
     * Calculates the eigen decomposition of the given symmetric matrix.
     * @param matrix The <strong>symmetric</strong> matrix to decompose.
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * {@link MathUtils#SAFE_MIN})
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
    public EigenDecompositionImpl(final RealMatrix matrix, final double splitTolerance) throws InvalidMatrixException {
        if (isSymmetric(matrix)) {
            this.splitTolerance = splitTolerance;
            transformToTridiagonal(matrix);
            decompose();
        } else {
            // as of 2.0, non-symmetric matrices (i.e. complex eigenvalues) are NOT supported
            // see issue https://issues.apache.org/jira/browse/MATH-235
            throw new InvalidMatrixException("eigen decomposition of assymetric matrices not supported yet");
        }
    }
     * Calculates the eigen decomposition of the given tridiagonal symmetric matrix.
     * @param main the main diagonal of the matrix (will be copied)
     * @param secondary the secondary diagonal of the matrix (will be copied)
     * @param splitTolerance tolerance on the off-diagonal elements relative to the
     * geometric mean to split the tridiagonal matrix (a suggested value is
     * {@link MathUtils#SAFE_MIN})
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
    public EigenDecompositionImpl(final double[] main, double[] secondary, final double splitTolerance) throws InvalidMatrixException {

        this.main      = main.clone();
        this.secondary = secondary.clone();
        transformer    = null;

        // pre-compute some elements
        squaredSecondary = new double[secondary.length];
        for (int i = 0; i < squaredSecondary.length; ++i) {
            final double s = secondary[i];
            squaredSecondary[i] = s * s;
        }

        this.splitTolerance = splitTolerance;
        decompose();

    }
     * Check if a matrix is symmetric.
     * @param matrix matrix to check
     * @return true if matrix is symmetric
    private boolean isSymmetric(final RealMatrix matrix) { … }
     * Decompose a tridiagonal symmetric matrix.
     * @exception InvalidMatrixException (wrapping a {@link
     * org.apache.commons.math.ConvergenceException} if algorithm fails to converge
    private void decompose() { … }
     * {@inheritDoc}
    public RealMatrix getV() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public RealMatrix getD() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public RealMatrix getVT() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public double[] getRealEigenvalues() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public double getRealEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { … }
     * {@inheritDoc}
    public double[] getImagEigenvalues() throws InvalidMatrixException { … }
     * {@inheritDoc}
    public double getImagEigenvalue(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { … }
     * {@inheritDoc}
    public RealVector getEigenvector(final int i) throws InvalidMatrixException, ArrayIndexOutOfBoundsException { … }
     * Return the determinant of the matrix
     * @return determinant of the matrix
    public double getDeterminant() { … }
     * {@inheritDoc}
    public DecompositionSolver getSolver() { … }
     * Specialized solver.
    private static class Solver implements DecompositionSolver { … }
     * Perform two iterations with Li's tests for initial splits.
     * @param n number of rows of the matrix to process
    private void initialSplits(final int n) { … }
     * Perform one "good" dqd/dqds step.
     * <p>This implementation is based on Beresford N. Parlett
     * and Osni A. Marques paper <a
     * href="http://www.netlib.org/lapack/lawnspdf/lawn155.pdf">An
     * Implementation of the dqds Algorithm (Positive Case)</a> and on the
     * corresponding LAPACK routine DLAZQ3.</p>
     * @param start start index
     * @param end end index
     * @return new end (maybe deflated)
    private int goodStep(final int start, final int end) { … }
     * Flip qd array if warranted.
     * @param n number of rows in the block
     * @param step within the array (1 for flipping all elements, 2 for flipping
     * only every other element)
     * @return true if qd array was flipped
    private boolean flipIfWarranted(final int n, final int step) {
        if (1.5 * work[pingPong] < work[4 * (n - 1) + pingPong]) {
            // flip array
            int j = 4 * n - 1;
            for (int i = 0; i < j; i += 4) {
                for (int k = 0; k < 4; k += step) {
                    final double tmp = work[i + k];
                    work[i + k] = work[j - k];
                    work[j - k] = tmp;
                }
                j -= 4;
            }
            return true;
        }
        return false;
    }
     * Compute an interval containing all realEigenvalues of a block.
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return an interval containing the realEigenvalues
    private double[] eigenvaluesRange(final int index, final int n) { … }
     * Count the number of realEigenvalues below a point.
     * @param t value below which we must count the number of realEigenvalues
     * @param index index of the first row of the block
     * @param n number of rows of the block
     * @return number of realEigenvalues smaller than t
    private int countEigenValues(final double t, final int index, final int n) { … }
     * Decompose the shifted tridiagonal matrix T-&lambda;I as LDL<sup>T</sup>.
     * <p>A shifted symmetric tridiagonal matrix T can be decomposed as
     * LDL<sup>T</sup> where L is a lower bidiagonal matrix with unit diagonal
     * and D is a diagonal matrix. This method is an implementation of
     * algorithm 4.4.7 from Dhillon's thesis.</p>
     * @param lambda shift to add to the matrix before decomposing it
     * to ensure it is positive definite
     * @param index index of the first row of the block
     * @param n number of rows of the block
    private void ldlTDecomposition(final double lambda, final int index, final int n) { … }
     * Perform a dqds step, using current shift increment.
     * <p>This implementation is a translation of the LAPACK routine DLASQ5.</p>
     * @param start start index
     * @param end end index
    private void dqds(final int start, final int end) { … }
     * Perform a dqd step.
     * <p>This implementation is a translation of the LAPACK routine DLASQ6.</p>
     * @param start start index
     * @param end end index
    private void dqd(final int start, final int end) { … }
     * Compute the shift increment as an estimate of the smallest eigenvalue.
     * <p>This implementation is a translation of the LAPACK routine DLAZQ4.</p>
     * @param start start index
     * @param end end index
     * @param deflated number of realEigenvalues just deflated
    private void computeShiftIncrement(final int start, final int end, final int deflated) { … }
     * Update sigma.
     * @param shift shift to apply to sigma
    private void updateSigma(final double shift) { … }
     * Find eigenvectors.
    private void findEigenVectors() { … }
     * Find an eigenvector corresponding to an eigenvalue, using bidiagonals.
     * <p>This method corresponds to algorithm X from Dhillon's thesis.</p>
     * @param eigenvalue eigenvalue for which eigenvector is desired
     * @param d diagonal elements of the initial non-shifted D matrix
     * @param l off-diagonal elements of the initial non-shifted L matrix
     * @return an eigenvector
    private ArrayRealVector findEigenvector(final double eigenvalue, final double[] d, final double[] l) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="EigenDecomposition" role="supertype-contract">
     * An interface to classes that implement an algorithm to calculate the
     * eigen decomposition of a real matrix.
     * <p>The eigen decomposition of matrix A is a set of two matrices:
     * V and D such that A = V &times; D &times; V<sup>T</sup>.
     * A, V and D are all m &times; m matrices.</p>
     * <p>This interface is similar in spirit to the <code>EigenvalueDecomposition</code>
     * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
     * library, with the following changes:</p>
     * <ul>
     * <li>a {@link #getVT() getVt} method has been added,</li>
     * <li>two {@link #getRealEigenvalue(int) getRealEigenvalue} and {@link #getImagEigenvalue(int)
     * getImagEigenvalue} methods to pick up a single eigenvalue have been added,</li>
     * …
public interface EigenDecomposition {

}
</class>

<class name="ArrayRealVector" role="test-subject">
     * This class implements the {@link RealVector} interface with a double array.
     * @version $Revision$ $Date$
     * @since 2.0
public class ArrayRealVector implements RealVector, Serializable {
     * Serializable version identifier.
    private static final long serialVersionUID = -1097961340710804027L;
     * Default format.
    private static final RealVectorFormat DEFAULT_FORMAT = RealVectorFormat.getInstance();
     * Entries of the vector.
    protected double data[];
     * Build a 0-length vector.
     * <p>Zero-length vectors may be used to initialized construction of vectors
     * by data gathering. We start with zero-length and use either the {@link
     * #ArrayRealVector(ArrayRealVector, ArrayRealVector)} constructor
     * or one of the <code>append</code> method ({@link #append(double)}, {@link
     * #append(double[])}, {@link #append(ArrayRealVector)}) to gather data
     * into this vector.</p>
    public ArrayRealVector() {
        data = new double[0];
    }
     * Construct a (size)-length vector of zeros.
     * @param size size of the vector
    public ArrayRealVector(int size) {
        data = new double[size];
    }
     * Construct an (size)-length vector with preset values.
     * @param size size of the vector
     * @param preset fill the vector with this scalar value
    public ArrayRealVector(int size, double preset) {
        data = new double[size];
        Arrays.fill(data, preset);
    }
     * Construct a vector from an array, copying the input array.
     * @param d array of doubles.
    public ArrayRealVector(double[] d) {
        data = d.clone();
    }
     * Create a new ArrayRealVector using the input array as the underlying
     * data array.
     * <p>If an array is built specially in order to be embedded in a
     * ArrayRealVector and not used directly, the <code>copyArray</code> may be
     * set to <code>false</code. This will prevent the copying and improve
     * performance as no new array will be built and no data will be copied.</p>
     * @param d data for new vector
     * @param copyArray if true, the input array will be copied, otherwise
     * it will be referenced
     * @throws IllegalArgumentException if <code>d</code> is empty
     * @throws NullPointerException if <code>d</code> is null
     * @see #ArrayRealVector(double[])
    public ArrayRealVector(double[] d, boolean copyArray) throws NullPointerException, IllegalArgumentException {
        if (d == null) {
            throw new NullPointerException();
        }
        if (d.length == 0) {
            throw MathRuntimeException.createIllegalArgumentException("vector must have at least one element");
        }
        data = copyArray ? d.clone() :  d;
    }
     * Construct a vector from part of a array.
     * @param d array of doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(double[] d, int pos, int size) {
        if (d.length < pos + size) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "position {0} and size {1} don't fit to the size of the input array {2}",
                    pos, size, d.length);
        }
        data = new double[size];
        System.arraycopy(d, pos, data, 0, size);
    }
     * Construct a vector from an array.
     * @param d array of Doubles.
    public ArrayRealVector(Double[] d) {
        data = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            data[i] = d[i].doubleValue();
        }
    }
     * Construct a vector from part of a Double array
     * @param d array of Doubles.
     * @param pos position of first entry
     * @param size number of entries to copy
    public ArrayRealVector(Double[] d, int pos, int size) {
        if (d.length < pos + size) {
            throw MathRuntimeException.createIllegalArgumentException(
                    "position {0} and size {1} don't fit to the size of the input array {2}",
                    pos, size, d.length);
        }
        data = new double[size];
        for (int i = pos; i < pos + size; i++) {
            data[i-pos] = d[i].doubleValue();
        }
    }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(RealVector v) {
        data = new double[v.getDimension()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = v.getEntry(i);
        }
    }
     * Construct a vector from another vector, using a deep copy.
     * @param v vector to copy
    public ArrayRealVector(ArrayRealVector v) {
        data = v.data.clone();
    }
     * Construct a vector from another vector.
     * @param v vector to copy
     * @param deep if true perform a deep copy otherwise perform a shallow copy
    public ArrayRealVector(ArrayRealVector v, boolean deep) {
        data = deep ? v.data.clone() : v.data;
    }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, ArrayRealVector v2) {
        data = new double[v1.data.length + v2.data.length];
        System.arraycopy(v1.data, 0, data, 0, v1.data.length);
        System.arraycopy(v2.data, 0, data, v1.data.length, v2.data.length);
    }
     * Construct a vector by appending one vector to another vector.
     * @param v1 first vector (will be put in front of the new vector)
     * @param v2 second vector (will be put at back of the new vector)
    public ArrayRealVector(ArrayRealVector v1, double[] v2) {
        data = new double[v1.data.length + v2.length];
        System.arraycopy(v1.data, 0, data, 0, v1.data.length);
        System.arraycopy(v2, 0, data, v1.data.length, v2.length);
    }
     * {@inheritDoc}
    public RealVector copy() { … }
     * {@inheritDoc}
    public RealVector mapExp() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="RealVector" role="test-subject">
     * Interface defining a real-valued vector with basic algebraic operations.
     * <p>
     * vector element indexing is 0-based -- e.g., <code>getEntry(0)</code>
     * returns the first element of the vector.
     * </p>
     * <p>
     * The various <code>mapXxx</code> and <code>mapXxxToSelf</code> methods operate
     * on vectors element-wise, i.e. they perform the same operation (adding a scalar,
     * applying a function ...) on each element in turn. The <code>mapXxx</code>
     * versions create a new vector to hold the result and do not change the instance.
     * The <code>mapXxxToSelf</code> versions use the instance itself to store the
     * results, so the instance is changed by these methods. In both cases, the result
     * …
public interface RealVector {

}
</class>

<class name="MathUtils" role="test-subject">
     * Some useful additions to the built-in functions in {@link Math}.
     * @version $Revision$ $Date$
public final class MathUtils {
     * Smallest positive number such that 1 - EPSILON is not numerically equal to 1.
    public static final double EPSILON = 0x1.0p-53;
     * Safe minimum, such that 1 / SAFE_MIN does not overflow.
     * <p>In IEEE 754 arithmetic, this is also the smallest normalized
     * number 2<sup>-1022</sup>.</p>
    public static final double SAFE_MIN = 0x1.0p-1022;
     * 2 &pi;.
    public static final double TWO_PI = 2 * Math.PI;
     * -1.0 cast as a byte.
    private static final byte NB = (byte)-1;
     * -1.0 cast as a short.
    private static final short NS = (short)-1;
     * 1.0 cast as a byte.
    private static final byte PB = (byte)1;
     * 1.0 cast as a short.
    private static final short PS = (short)1;
     * 0.0 cast as a byte.
    private static final byte ZB = (byte)0;
     * 0.0 cast as a short.
    private static final short ZS = (short)0;
     * Gap between NaN and regular numbers.
    private static final int NAN_GAP = 4 * 1024 * 1024;
     * Offset to order signed double numbers lexicographically.
    private static final long SGN_MASK = 0x8000000000000000L;
     * All long-representable factorials
    private static final long[] FACTORIALS = new long[] { … }
     * Private Constructor
    private MathUtils() {
        super();
    }
     * Add two integers, checking for overflow.
     * @param x an addend
     * @param y an addend
     * @return the sum <code>x+y</code>
     * @throws ArithmeticException if the result can not be represented as an
     * int
     * @since 1.1
    public static int addAndCheck(int x, int y) { … }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    public static long addAndCheck(long a, long b) { … }
     * Add two long integers, checking for overflow.
     * @param a an addend
     * @param b an addend
     * @param msg the message to use for any thrown exception.
     * @return the sum <code>a+b</code>
     * @throws ArithmeticException if the result can not be represented as an
     * long
     * @since 1.2
    private static long addAndCheck(long a, long b, String msg) { … }
     * Returns an exact representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>long</code>. The
     * largest value of <code>n</code> for which all coefficients are
     * …
    public static long binomialCoefficient(final int n, final int k) { … }
     * Returns a <code>double</code> representation of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * <li> The result is small enough to fit into a <code>double</code>. The
     * largest value of <code>n</code> for which all coefficients are <
     * …
    public static double binomialCoefficientDouble(final int n, final int k) { … }
     * Returns the natural <code>log</code> of the <a
     * href="http://mathworld.wolfram.com/BinomialCoefficient.html"> Binomial
     * Coefficient</a>, "<code>n choose k</code>", the number of
     * <code>k</code>-element subsets that can be selected from an
     * <code>n</code>-element set.
     * <p>
     * <Strong>Preconditions</strong>:
     * <ul>
     * <li> <code>0 <= k <= n </code> (otherwise
     * <code>IllegalArgumentException</code> is thrown)</li>
     * </ul></p>
     * @param n the size of the set
     * …
    public static double binomialCoefficientLog(final int n, final int k) { … }
     * Check binomial preconditions.
     * @param n the size of the set
     * @param k the size of the subsets to be counted
     * @exception IllegalArgumentException if preconditions are not met.
    private static void checkBinomial(final int n, final int k) throws IllegalArgumentException { … }
     * Compares two numbers given some amount of allowed error.
     * @param x the first number
     * @param y the second number
     * @param eps the amount of error to allow when checking for equality
     * @return <ul><li>0 if  {@link #equals(double, double, double) equals(x, y, eps)}</li>
     * <li>&lt; 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x &lt; y</li>
     * <li>> 0 if !{@link #equals(double, double, double) equals(x, y, eps)} &amp;&amp; x > y</li></ul>
    public static int compareTo(double x, double y, double eps) { … }
     * Returns the <a href="http://mathworld.wolfram.com/HyperbolicCosine.html">
     * hyperbolic cosine</a> of x.
     * @param x double value for which to find the hyperbolic cosine
     * @return hyperbolic cosine of x
    public static double cosh(double x) { … }
     * Returns true iff both arguments are NaN or neither is NaN and they are
     * equal
     * @param x first value
     * @param y second value
     * @return true if the values are equal or both are NaN
    public static boolean equals(double x, double y) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="InvalidMatrixException" role="collaborator">
     * Thrown when a system attempts an operation on a matrix, and
     * that matrix does not satisfy the preconditions for the
     * aforementioned operation.
     * @version $Revision$ $Date$
public class InvalidMatrixException extends MathRuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = 1135533765052675495L;
     * Construct an exception with the given message.
     * @param pattern format specifier
     * @param arguments format arguments
     * @since 2.0
    public InvalidMatrixException(final String pattern, final Object ... arguments) {
        super(pattern, arguments);
    }
     * Construct an exception with the given message.
     * @param cause the exception or error that caused this exception
     * to be thrown.
     * @since 2.0
    public InvalidMatrixException(final Throwable cause) {
        super(cause);
    }
}
</class>

<class name="RealMatrix" role="collaborator">
     * Interface defining a real-valued matrix with basic algebraic operations.
     * <p>
     * Matrix element indexing is 0-based -- e.g., <code>getEntry(0, 0)</code>
     * returns the element in the first row, first column of the matrix.</p>
     * @version $Revision$ $Date$
public interface RealMatrix extends AnyMatrix {

}
</class>

<class name="MatrixUtils" role="collaborator">
     * A collection of static methods that operate on or return matrices.
     * @version $Revision$ $Date$
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
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
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
    @SuppressWarnings("unchecked") public static <T extends FieldElement<T>> FieldMatrix<T> createFieldIdentityMatrix(final Field<T> field, final int dimension) { … }
     * Returns <code>dimension x dimension</code> identity matrix.
     * @param dimension dimension of identity matrix to generate
     * @return identity matrix
     * @throws IllegalArgumentException if dimension is not positive
     * @since 1.1
     * @deprecated since 2.0, replaced by {@link #createFieldIdentityMatrix(Field, int)}
    @Deprecated public static BigMatrix createBigIdentityMatrix(int dimension) { … }
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
     * Returns a {@link BigMatrix} whose entries are the the values in the
     * the input array.  The input array is copied, not referenced.
     * @param data input array
     * @return  RealMatrix containing the values of the array
     * @throws IllegalArgumentException if <code>data</code> is not rectangular
     * (not all rows have the same length) or empty
     * @throws NullPointerException if data is null
     * @deprecated since 2.0 replaced by {@link #createFieldMatrix(FieldElement[][])}
    @Deprecated public static BigMatrix createBigMatrix(double[][] data) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="MathRuntimeException" role="collaborator">
     * Base class for commons-math unchecked exceptions.
     * @version $Revision$ $Date$
     * @since 2.0
public class MathRuntimeException extends RuntimeException {
     * Serializable version identifier.
    private static final long serialVersionUID = -5128983364075381060L;
     * Pattern used to build the message.
    private final String pattern;
     * Arguments used to build the message.
    private final Object[] arguments;
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final String pattern, final Object ... arguments) {
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * nested <code>Throwable</code> root cause.
     * @param rootCause  the exception or error that caused this exception
     * to be thrown.
    public MathRuntimeException(final Throwable rootCause) {
        super(rootCause);
        this.pattern   = getMessage();
        this.arguments = new Object[0];
    }
     * Constructs a new <code>MathRuntimeException</code> with specified
     * formatted detail message and nested <code>Throwable</code> root cause.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @param pattern format specifier
     * @param arguments format arguments
    public MathRuntimeException(final Throwable rootCause, final String pattern, final Object ... arguments) {
        super(rootCause);
        this.pattern   = pattern;
        this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
    }
     * Builds a message string by from a pattern and its arguments.
     * @param locale Locale in which the message should be translated
     * @param pattern format specifier
     * @param arguments format arguments
     * @return a message string
    private static String buildMessage(final Locale locale, final String pattern, final Object ... arguments) { … }
     * Gets the pattern used to build the message of this throwable.
     * @return the pattern used to build the message of this throwable
    public String getPattern() { … }
     * Gets the arguments used to build the message of this throwable.
     * @return the arguments used to build the message of this throwable
    public Object[] getArguments() { … }
     * Gets the message in a specified locale.
     * @param locale Locale in which the message should be translated
     * @return localized message
    public String getMessage(final Locale locale) { … }
     * {@inheritDoc}
    @Override public String getMessage() { … }
     * {@inheritDoc}
    @Override public String getLocalizedMessage() { … }
     * Prints the stack trace of this exception to the standard error stream.
    @Override public void printStackTrace() { … }
     * Prints the stack trace of this exception to the specified stream.
     * @param out  the <code>PrintStream</code> to use for output
    @Override public void printStackTrace(final PrintStream out) { … }
     * Constructs a new <code>ArithmeticException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArithmeticException createArithmeticException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>ArrayIndexOutOfBoundsException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static ArrayIndexOutOfBoundsException createArrayIndexOutOfBoundsException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>EOFException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static EOFException createEOFException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IOException</code> with specified nested
     * <code>Throwable</code> root cause.
     * <p>This factory method allows chaining of other exceptions within an
     * <code>IOException</code> even for Java 5. The constructor for
     * <code>IOException</code> with a cause parameter was introduced only
     * with Java 6.</p>
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IOException createIOException(final Throwable rootCause) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified formatted detail message.
     * Message formatting is delegated to {@link java.text.MessageFormat}.
     * @param pattern format specifier
     * @param arguments format arguments
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final String pattern, final Object ... arguments) { … }
     * Constructs a new <code>IllegalArgumentException</code> with specified nested
     * <code>Throwable</code> root cause.
     * @param rootCause the exception or error that caused this exception
     * to be thrown.
     * @return built exception
    public static IllegalArgumentException createIllegalArgumentException(final Throwable rootCause) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="SingularMatrixException" role="collaborator">
     * Thrown when a matrix is singular.
     * @version $Revision$ $Date$
     * @since 2.0
public class SingularMatrixException extends InvalidMatrixException {
     * Serializable version identifier.
    private static final long serialVersionUID = -7379143356784298432L;
     * Construct an exception with a default message.
    public SingularMatrixException() {
        super("matrix is singular");
    }
}
</class>
</class_skeletons>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  solve(final double[] b) / (final RealVector b) / (final RealMatrix b)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getD, getDeterminant, getEigenvector, getImagEigenvalue, getImagEigenvalues, getInverse, getRealEigenvalue, getRealEigenvalues, getSolver, getV, getVT

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getD, getDeterminant, getImagEigenvalues, getInverse, getRealEigenvalues, getSolver, getV, getVT, isNonSingular

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `EigenDecompositionImpl`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.math.linear.EigenDecompositionImplTest" method="testMathpbx02">
    public void testMathpbx02() {

        double[] mainTridiagonal = {
        	  7484.860960227216, 18405.28129035345, 13855.225609560746,
        	 10016.708722343366, 559.8117399576674, 6750.190788301587, 
        	    71.21428769782159
        };
        double[] secondaryTridiagonal = {
        	 -4175.088570476366,1975.7955858241994,5193.178422374075, 
        	  1995.286659169179,75.34535882933804,-234.0808002076056
        };

        // the reference values have been computed using routine DSTEMR
        // from the fortran library LAPACK version 3.2.1
        double[] refEigenValues = {
        		20654.744890306974412,16828.208208485466457,
        		6893.155912634994820,6757.083016675340332,
        		5887.799885688558788,64.309089923240379,
        		57.992628792736340
        };
        RealVector[] refEigenVectors = {
        		new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        		new ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),
        		new ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),
        		new ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),
        		new ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),
        		new ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),
        		new ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})
        };

        // the following line triggers the exception
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {
                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            } else {
                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
            }
        }

    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.linear.EigenDecompositionImplTest::testMathpbx02
junit.framework.AssertionFailedError: expected:<16828.208208485466> but was:<20654.74511575945>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.math.linear.EigenDecompositionImplTest">
// --- setUp() (test setup) ---
    @Override
    public void setUp() {
        refValues = new double[] {
                2.003, 2.002, 2.001, 1.001, 1.000, 0.001
        };
        matrix = createTestMatrix(new Random(35992629946426l), refValues);
    }

// --- class fields/constants the test uses ---
private double[] refValues;
private RealMatrix matrix;
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
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,956 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeInt();

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            // Lifted directly from EigenDecompositionImplTest.testMathpbx02:
            // assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigval-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            RealVector refVec = REF_EIGENVECTORS[i];
            double norm;
            if (refVec.dotProduct(actualVec) < 0) {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.add(actualVec).getNorm();
            } else {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.subtract(actualVec).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigvec-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }

        // Post-condition / sibling-consistency oracle:
        // The class contract says for symmetric matrices getD() is diagonal and getRealEigenvalue(i)
        // reports the eigenvalues of the same decomposition. A patch that merely avoids the bad flip
        // branch or corrupts work[] can silently produce inconsistent readers even if no exception fires.
        RealMatrix d = decomposition.getD();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double fromScalar = decomposition.getRealEigenvalue(i);
            double fromArray = eigenValues[i];
            double fromMatrix = d.getEntry(i, i);
            if (Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromArray) ||
                Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromMatrix)) {
                throw new RuntimeException(
                    "[oracle:matrix-consistency] metamorphic violation: getRealEigenvalue/getRealEigenvalues/getD diagonal disagree index=" +
                    i + " scalar=" + fromScalar + " array=" + fromArray + " matrixDiag=" + fromMatrix);
            }
            for (int j = 0; j < REF_EIGENVALUES.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0d) {
                    throw new RuntimeException(
                        "[oracle:matrix-consistency] metamorphic violation: getD must be diagonal for symmetric decomposition i=" +
                        i + " j=" + j + " offDiag=" + d.getEntry(i, j));
                }
            }
        }

        int scaleChoice = data.consumeInt(1, 8);
        double scale = scaleChoice / 4.0d;
        double[] scaledMain = new double[MAIN.length];
        double[] scaledSecondary = new double[SECONDARY.length];
        for (int i = 0; i < MAIN.length; ++i) {
            scaledMain[i] = MAIN[i] * scale;
        }
        for (int i = 0; i < SECONDARY.length; ++i) {
            scaledSecondary[i] = SECONDARY[i] * scale;
        }

        try {
            EigenDecomposition scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);
            double[] scaledEigenValues = scaled.getRealEigenvalues();

            // Metamorphic relation from linear algebra for the real API used here:
            // scaling a matrix by positive scalar s scales all eigenvalues by s and keeps eigenvectors
            // unchanged up to sign. This gives a trusted oracle derived from the seed, while still
            // exercising the patched decomposition path on additional real inputs.
            for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
                double expected = REF_EIGENVALUES[i] * scale;
                double actual = scaledEigenValues[i];
                if (Math.abs(expected - actual) > 1.0e-3 * Math.max(1.0d, scale)) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvals] metamorphic violation: positive scaling should scale eigenvalues inputScale=" +
                        scale + " index=" + i + " expected=" + expected + " actual=" + actual);
                }

                RealVector actualVec = scaled.getEigenvector(i);
                RealVector refVec = REF_EIGENVECTORS[i];
                double norm;
                if (refVec.dotProduct(actualVec) < 0) {
                    norm = refVec.add(actualVec).getNorm();
                } else {
                    norm = refVec.subtract(actualVec).getNorm();
                }
                if (Math.abs(norm) > 1.0e-5) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvecs] metamorphic violation: positive scaling should preserve eigenvectors up to sign inputScale=" +
                        scale + " index=" + i + " norm=" + norm);
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
## [84] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 47 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:seed-eigval-" + i + "] semantic m

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,697 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeInt();

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            // Lifted directly from EigenDecompositionImplTest.testMathpbx02:
            // assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigval-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            RealVector refVec = REF_EIGENVECTORS[i];
            double norm;
            if (refVec.dotProduct(actualVec) < 0) {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.add(actualVec).getNorm();
            } else {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.subtract(actualVec).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigvec-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }

        // Post-condition / sibling-consistency oracle:
        // The class contract says for symmetric matrices getD() is diagonal and getRealEigenvalue(i)
        // reports the eigenvalues of the same decomposition. A patch that merely avoids the bad flip
        // branch or corrupts work[] can silently produce inconsistent readers even if no exception fires.
        RealMatrix d = decomposition.getD();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double fromScalar = decomposition.getRealEigenvalue(i);
            double fromArray = eigenValues[i];
            double fromMatrix = d.getEntry(i, i);
            if (Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromArray) ||
                Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromMatrix)) {
                throw new RuntimeException(
                    "[oracle:matrix-consistency] metamorphic violation: getRealEigenvalue/getRealEigenvalues/getD diagonal disagree index=" +
                    i + " scalar=" + fromScalar + " array=" + fromArray + " matrixDiag=" + fromMatrix);
            }
            for (int j = 0; j < REF_EIGENVALUES.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0d) {
                    throw new RuntimeException(
                        "[oracle:matrix-consistency] metamorphic violation: getD must be diagonal for symmetric decomposition i=" +
                        i + " j=" + j + " offDiag=" + d.getEntry(i, j));
                }
            }
        }

        int scaleChoice = data.consumeInt(1, 8);
        double scale = scaleChoice / 4.0d;
        double[] scaledMain = new double[MAIN.length];
        double[] scaledSecondary = new double[SECONDARY.length];
        for (int i = 0; i < MAIN.length; ++i) {
            scaledMain[i] = MAIN[i] * scale;
        }
        for (int i = 0; i < SECONDARY.length; ++i) {
            scaledSecondary[i] = SECONDARY[i] * scale;
        }

        try {
            EigenDecomposition scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);
            double[] scaledEigenValues = scaled.getRealEigenvalues();

            // Metamorphic relation from linear algebra for the real API used here:
            // scaling a matrix by positive scalar s scales all eigenvalues by s and keeps eigenvectors
            // unchanged up to sign. This gives a trusted oracle derived from the seed, while still
            // exercising the patched decomposition path on additional real inputs.
            for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
                double expected = REF_EIGENVALUES[i] * scale;
                double actual = scaledEigenValues[i];
                if (Math.abs(expected - actual) > 1.0e-3 * Math.max(1.0d, scale)) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvals] metamorphic violation: positive scaling should scale eigenvalues inputScale=" +
                        scale + " index=" + i + " expected=" + expected + " actual=" + actual);
                }

                RealVector actualVec = scaled.getEigenvector(i);
                RealVector refVec = REF_EIGENVECTORS[i];
                double norm;
                if (refVec.dotProduct(actualVec) < 0) {
                    norm = refVec.add(actualVec).getNorm();
                } else {
                    norm = refVec.subtract(actualVec).getNorm();
                }
                if (Math.abs(norm) > 1.0e-5) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvecs] metamorphic violation: positive scaling should preserve eigenvectors up to sign inputScale=" +
                        scale + " index=" + i + " norm=" + norm);
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
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 47 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:seed-eigval-" + i + "] semantic mismatch: ex...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,964 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeInt();

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            // Lifted directly from EigenDecompositionImplTest.testMathpbx02:
            // assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigvals] semantic mismatch: index=" + i + " expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            RealVector refVec = REF_EIGENVECTORS[i];
            double norm;
            if (refVec.dotProduct(actualVec) < 0) {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.add(actualVec).getNorm();
            } else {
                // Lifted directly from test:
                // assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);
                norm = refVec.subtract(actualVec).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-eigvecs] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }

        // Post-condition / sibling-consistency oracle:
        // The class contract says for symmetric matrices getD() is diagonal and getRealEigenvalue(i)
        // reports the eigenvalues of the same decomposition. A patch that merely avoids the bad flip
        // branch or corrupts work[] can silently produce inconsistent readers even if no exception fires.
        RealMatrix d = decomposition.getD();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double fromScalar = decomposition.getRealEigenvalue(i);
            double fromArray = eigenValues[i];
            double fromMatrix = d.getEntry(i, i);
            if (Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromArray) ||
                Double.doubleToLongBits(fromScalar) != Double.doubleToLongBits(fromMatrix)) {
                throw new RuntimeException(
                    "[oracle:reader-agreement] metamorphic violation: getRealEigenvalue/getRealEigenvalues/getD diagonal disagree index=" +
                    i + " scalar=" + fromScalar + " array=" + fromArray + " matrixDiag=" + fromMatrix);
            }
            for (int j = 0; j < REF_EIGENVALUES.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0d) {
                    throw new RuntimeException(
                        "[oracle:diagonal-shape] metamorphic violation: getD must be diagonal for symmetric decomposition i=" +
                        i + " j=" + j + " offDiag=" + d.getEntry(i, j));
                }
            }
        }

        int scaleChoice = data.consumeInt(1, 8);
        double scale = scaleChoice / 4.0d;
        double[] scaledMain = new double[MAIN.length];
        double[] scaledSecondary = new double[SECONDARY.length];
        for (int i = 0; i < MAIN.length; ++i) {
            scaledMain[i] = MAIN[i] * scale;
        }
        for (int i = 0; i < SECONDARY.length; ++i) {
            scaledSecondary[i] = SECONDARY[i] * scale;
        }

        try {
            EigenDecomposition scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);
            double[] scaledEigenValues = scaled.getRealEigenvalues();

            // Metamorphic relation from linear algebra for the real API used here:
            // scaling a matrix by positive scalar s scales all eigenvalues by s and keeps eigenvectors
            // unchanged up to sign. This gives a trusted oracle derived from the seed, while still
            // exercising the patched decomposition path on additional real inputs.
            for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
                double expected = REF_EIGENVALUES[i] * scale;
                double actual = scaledEigenValues[i];
                if (Math.abs(expected - actual) > 1.0e-3 * Math.max(1.0d, scale)) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvals] metamorphic violation: positive scaling should scale eigenvalues inputScale=" +
                        scale + " index=" + i + " expected=" + expected + " actual=" + actual);
                }

                RealVector actualVec = scaled.getEigenvector(i);
                RealVector refVec = REF_EIGENVECTORS[i];
                double norm;
                if (refVec.dotProduct(actualVec) < 0) {
                    norm = refVec.add(actualVec).getNorm();
                } else {
                    norm = refVec.subtract(actualVec).getNorm();
                }
                if (Math.abs(norm) > 1.0e-5) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigvecs] metamorphic violation: positive scaling should preserve eigenvectors up to sign inputScale=" +
                        scale + " index=" + i + " norm=" + norm);
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
## [86] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-eigvals] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945 tolerance=0.001

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,070 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exactLiftedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNullForSymmetricInput(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void exactLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!(Math.abs(refEigenValues[i] - eigenValues[i]) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualEigenvector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualEigenvector) < 0) {
                norm = refEigenVectors[i].add(actualEigenvector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualEigenvector).getNorm();
            }
            if (!(Math.abs(0.0 - norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        EigenDecompositionImpl dec =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double determinantBefore = dec.getDeterminant();
        double[] imagBefore = dec.getImagEigenvalues();
        double[] values1 = dec.getRealEigenvalues();
        double determinantAfter = dec.getDeterminant();
        double[] imagAfter = dec.getImagEigenvalues();
        double[] values2 = dec.getRealEigenvalues();

        /* Contract justification:
         * get* readers are documented as queries; a throw-deleting/branch-skipping patch in flip/processGeneralBlock
         * could leave shared state (work/pingPong-derived outputs) inconsistent across readers.
         * So repeated reads must agree, getRealEigenvalue(i) must match getRealEigenvalues()[i], and
         * this symmetric decomposition must keep imaginary eigenvalues null.
         */
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-imag-null] semantic mismatch: expected null imaginary eigenvalues before/after reads but got before=" +
                (imagBefore == null ? "null" : ("len=" + imagBefore.length)) +
                " after=" + (imagAfter == null ? "null" : ("len=" + imagAfter.length)));
        }
        if (values1.length != values2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-length] semantic mismatch: firstLength=" + values1.length +
                " secondLength=" + values2.length);
        }
        for (int i = 0; i < values1.length; i++) {
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(values2[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-repeat-" + i + "] semantic mismatch: first=" +
                    values1[i] + " second=" + values2[i]);
            }
            double scalar = dec.getRealEigenvalue(i);
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(scalar)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue-" + i + "] semantic mismatch: arrayValue=" +
                    values1[i] + " scalarValue=" + scalar);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: determinantBefore=" +
                determinantBefore + " determinantAfter=" + determinantAfter);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNullForSymmetricInput(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": " + x + " vs " + y + " tol=" + tol);
            }
        }
    }
}
```

</details>

---
## [88] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 49 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + refE

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,811 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exactLiftedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNullForSymmetricInput(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void exactLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!(Math.abs(refEigenValues[i] - eigenValues[i]) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualEigenvector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualEigenvector) < 0) {
                norm = refEigenVectors[i].add(actualEigenvector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualEigenvector).getNorm();
            }
            if (!(Math.abs(0.0 - norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        EigenDecompositionImpl dec =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double determinantBefore = dec.getDeterminant();
        double[] imagBefore = dec.getImagEigenvalues();
        double[] values1 = dec.getRealEigenvalues();
        double determinantAfter = dec.getDeterminant();
        double[] imagAfter = dec.getImagEigenvalues();
        double[] values2 = dec.getRealEigenvalues();

        /* Contract justification:
         * get* readers are documented as queries; a throw-deleting/branch-skipping patch in flip/processGeneralBlock
         * could leave shared state (work/pingPong-derived outputs) inconsistent across readers.
         * So repeated reads must agree, getRealEigenvalue(i) must match getRealEigenvalues()[i], and
         * this symmetric decomposition must keep imaginary eigenvalues null.
         */
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-imag-null] semantic mismatch: expected null imaginary eigenvalues before/after reads but got before=" +
                (imagBefore == null ? "null" : ("len=" + imagBefore.length)) +
                " after=" + (imagAfter == null ? "null" : ("len=" + imagAfter.length)));
        }
        if (values1.length != values2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-length] semantic mismatch: firstLength=" + values1.length +
                " secondLength=" + values2.length);
        }
        for (int i = 0; i < values1.length; i++) {
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(values2[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-repeat-" + i + "] semantic mismatch: first=" +
                    values1[i] + " second=" + values2[i]);
            }
            double scalar = dec.getRealEigenvalue(i);
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(scalar)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue-" + i + "] semantic mismatch: arrayValue=" +
                    values1[i] + " scalarValue=" + scalar);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: determinantBefore=" +
                determinantBefore + " determinantAfter=" + determinantAfter);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNullForSymmetricInput(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": " + x + " vs " + y + " tol=" + tol);
            }
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 49 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + refEigenValues[...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~10,176 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exactLiftedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNullForSymmetricInput(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void exactLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!(Math.abs(refEigenValues[i] - eigenValues[i]) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualEigenvector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualEigenvector) < 0) {
                norm = refEigenVectors[i].add(actualEigenvector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualEigenvector).getNorm();
            }
            if (!(Math.abs(0.0 - norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        EigenDecompositionImpl dec =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double determinantBefore = dec.getDeterminant();
        double[] imagBefore = dec.getImagEigenvalues();
        double[] values1 = dec.getRealEigenvalues();
        double determinantAfter = dec.getDeterminant();
        double[] imagAfter = dec.getImagEigenvalues();
        double[] values2 = dec.getRealEigenvalues();

        /* Contract justification:
         * get* readers are documented as queries; a throw-deleting/branch-skipping patch in flip/processGeneralBlock
         * could leave shared state (work/pingPong-derived outputs) inconsistent across readers.
         * So repeated reads must agree, getRealEigenvalue(i) must match getRealEigenvalues()[i], and
         * this symmetric decomposition must keep imaginary eigenvalues null.
         */
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-imag-null] semantic mismatch: expected null imaginary eigenvalues before/after reads but got before=" +
                (imagBefore == null ? "null" : ("len=" + imagBefore.length)) +
                " after=" + (imagAfter == null ? "null" : ("len=" + imagAfter.length)));
        }
        if (values1.length != values2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-length] semantic mismatch: firstLength=" + values1.length +
                " secondLength=" + values2.length);
        }
        for (int i = 0; i < values1.length; i++) {
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(values2[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-repeat-" + i + "] semantic mismatch: first=" +
                    values1[i] + " second=" + values2[i]);
            }
            double scalar = dec.getRealEigenvalue(i);
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(scalar)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue-" + i + "] semantic mismatch: arrayValue=" +
                    values1[i] + " scalarValue=" + scalar);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: determinantBefore=" +
                determinantBefore + " determinantAfter=" + determinantAfter);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNullForSymmetricInput(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reversed-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reversed-multiset-" + i + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": " + x + " vs " + y + " tol=" + tol);
            }
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 49 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEi

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~11,038 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [89] — not reprinted*
- *[user] message: identical to step [89] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exactLiftedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNullForSymmetricInput(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void exactLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!(Math.abs(refEigenValues[i] - eigenValues[i]) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualEigenvector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualEigenvector) < 0) {
                norm = refEigenVectors[i].add(actualEigenvector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualEigenvector).getNorm();
            }
            if (!(Math.abs(0.0 - norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        EigenDecompositionImpl dec =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double determinantBefore = dec.getDeterminant();
        double[] imagBefore = dec.getImagEigenvalues();
        double[] values1 = dec.getRealEigenvalues();
        double determinantAfter = dec.getDeterminant();
        double[] imagAfter = dec.getImagEigenvalues();
        double[] values2 = dec.getRealEigenvalues();

        /* Contract justification:
         * get* readers are documented as queries; a throw-deleting/branch-skipping patch in flip/processGeneralBlock
         * could leave shared state (work/pingPong-derived outputs) inconsistent across readers.
         * So repeated reads must agree, getRealEigenvalue(i) must match getRealEigenvalues()[i], and
         * this symmetric decomposition must keep imaginary eigenvalues null.
         */
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-imag-null] semantic mismatch: expected null imaginary eigenvalues before/after reads but got before=" +
                (imagBefore == null ? "null" : ("len=" + imagBefore.length)) +
                " after=" + (imagAfter == null ? "null" : ("len=" + imagAfter.length)));
        }
        if (values1.length != values2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-length] semantic mismatch: firstLength=" + values1.length +
                " secondLength=" + values2.length);
        }
        for (int i = 0; i < values1.length; i++) {
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(values2[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-repeat-" + i + "] semantic mismatch: first=" +
                    values1[i] + " second=" + values2[i]);
            }
            double scalar = dec.getRealEigenvalue(i);
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(scalar)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue-" + i + "] semantic mismatch: arrayValue=" +
                    values1[i] + " scalarValue=" + scalar);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: determinantBefore=" +
                determinantBefore + " determinantAfter=" + determinantAfter);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNullForSymmetricInput(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reversed-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reversed-multiset-" + i + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": " + x + " vs " + y + " tol=" + tol);
            }
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 49 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + refEigenValues[i...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~10,188 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exactLiftedOracle();
        hiddenStateAndSiblingAgreement();
        relationImaginaryEigenvalueArrayIsNullForSymmetricInput(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void exactLiftedOracle() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < refEigenValues.length; ++i) {
            if (!(Math.abs(refEigenValues[i] - eigenValues[i]) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue] semantic mismatch: index=" + i + " expected=" +
                    refEigenValues[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }
            RealVector actualEigenvector = decomposition.getEigenvector(i);
            double norm;
            if (refEigenVectors[i].dotProduct(actualEigenvector) < 0) {
                norm = refEigenVectors[i].add(actualEigenvector).getNorm();
            } else {
                norm = refEigenVectors[i].subtract(actualEigenvector).getNorm();
            }
            if (!(Math.abs(0.0 - norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector] semantic mismatch: index=" + i + " expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void hiddenStateAndSiblingAgreement() {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        EigenDecompositionImpl dec =
            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);

        double determinantBefore = dec.getDeterminant();
        double[] imagBefore = dec.getImagEigenvalues();
        double[] values1 = dec.getRealEigenvalues();
        double determinantAfter = dec.getDeterminant();
        double[] imagAfter = dec.getImagEigenvalues();
        double[] values2 = dec.getRealEigenvalues();

        /* Contract justification:
         * get* readers are documented as queries; a throw-deleting/branch-skipping patch in flip/processGeneralBlock
         * could leave shared state (work/pingPong-derived outputs) inconsistent across readers.
         * So repeated reads must agree, getRealEigenvalue(i) must match getRealEigenvalues()[i], and
         * this symmetric decomposition must keep imaginary eigenvalues null.
         */
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-imag-null] semantic mismatch: expected null imaginary eigenvalues before/after reads but got before=" +
                (imagBefore == null ? "null" : ("len=" + imagBefore.length)) +
                " after=" + (imagAfter == null ? "null" : ("len=" + imagAfter.length)));
        }
        if (values1.length != values2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-length] semantic mismatch: firstLength=" + values1.length +
                " secondLength=" + values2.length);
        }
        for (int i = 0; i < values1.length; i++) {
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(values2[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-repeat] semantic mismatch: index=" + i + " first=" +
                    values1[i] + " second=" + values2[i]);
            }
            double scalar = dec.getRealEigenvalue(i);
            if (Double.doubleToLongBits(values1[i]) != Double.doubleToLongBits(scalar)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue] semantic mismatch: index=" + i + " arrayValue=" +
                    values1[i] + " scalarValue=" + scalar);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: determinantBefore=" +
                determinantBefore + " determinantAfter=" + determinantAfter);
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNullForSymmetricInput(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reversed-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reversed-multiset] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index=" +
                    i + " x=" + x + " y=" + y + " tol=" + tol);
            }
        }
    }
}
```

</details>

---
## [92] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalue] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945 tolerance=0.001

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,851 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedRegression();
        checkTrustedReaderAgreement();
        exerciseFuzzedInputs(data);
    }

    private static void checkLiftedRegression() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }

            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot;
            try {
                dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            } catch (Throwable t) {
                return;
            }

            final double norm;
            try {
                if (dot < 0) {
                    norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
                } else {
                    norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
                }
            } catch (Throwable t) {
                return;
            }

            if (!withinTol(0.0, norm, 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void checkTrustedReaderAgreement() {
        final EigenDecompositionImpl decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        final RealMatrix d;
        try {
            eigenValues = decomposition.getRealEigenvalues();
            d = decomposition.getD();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            final double single;
            try {
                single = decomposition.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(single) != Double.doubleToLongBits(eigenValues[i]) && single != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:reader-family] metamorphic violation: getRealEigenvalue(i) must agree with getRealEigenvalues()[i] input=i=" +
                    i + " lhs=" + single + " rhs=" + eigenValues[i]);
            }

            final double diagonal;
            try {
                diagonal = d.getEntry(i, i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(diagonal) != Double.doubleToLongBits(eigenValues[i]) && diagonal != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:d-diagonal] metamorphic violation: getD() is documented as the diagonal matrix of eigenvalues, so its diagonal must equal getRealEigenvalues() input=i=" +
                    i + " lhs=" + diagonal + " rhs=" + eigenValues[i]);
            }

            for (int j = 0; j < eigenValues.length; ++j) {
                if (i == j) {
                    continue;
                }
                final double off;
                try {
                    off = d.getEntry(i, j);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(off) > 1.0e-12) {
                    throw new RuntimeException(
                        "[oracle:d-offdiag] metamorphic violation: getD() is documented as diagonal for symmetric matrices input=i=" +
                        i + ",j=" + j + " lhs=" + off + " rhs=0.0");
                }
            }
        }

        final RealMatrix v;
        final RealMatrix vt;
        try {
            v = decomposition.getV();
            vt = decomposition.getVT();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            for (int j = 0; j < eigenValues.length; ++j) {
                final double lhs;
                final double rhs;
                try {
                    lhs = vt.getEntry(i, j);
                    rhs = v.getEntry(j, i);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(lhs - rhs) > 1.0e-10) {
                    throw new RuntimeException(
                        "[oracle:v-vt] metamorphic violation: by the EigenDecomposition contract, getVT() must be the transpose of getV() input=i=" +
                        i + ",j=" + j + " lhs=" + lhs + " rhs=" + rhs);
                }
            }
        }
    }

    private static void exerciseFuzzedInputs(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 10);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];

        for (int i = 0; i < n; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            main[i] = base / 16.0;
        }
        for (int i = 0; i < n - 1; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            secondary[i] = base / 16.0;
        }

        try {
            EigenDecompositionImpl decomposition =
                new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
            decomposition.getRealEigenvalues();
            decomposition.getD();
            decomposition.getV();
            decomposition.getVT();
            int idx = data.consumeInt(0, n - 1);
            decomposition.getRealEigenvalue(idx);
            decomposition.getEigenvector(idx);
        } catch (Throwable t) {
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 57 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalue-" + i + "] sema

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,592 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedRegression();
        checkTrustedReaderAgreement();
        exerciseFuzzedInputs(data);
    }

    private static void checkLiftedRegression() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " tolerance=0.001");
            }

            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot;
            try {
                dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            } catch (Throwable t) {
                return;
            }

            final double norm;
            try {
                if (dot < 0) {
                    norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
                } else {
                    norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
                }
            } catch (Throwable t) {
                return;
            }

            if (!withinTol(0.0, norm, 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" +
                    norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void checkTrustedReaderAgreement() {
        final EigenDecompositionImpl decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        final RealMatrix d;
        try {
            eigenValues = decomposition.getRealEigenvalues();
            d = decomposition.getD();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            final double single;
            try {
                single = decomposition.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(single) != Double.doubleToLongBits(eigenValues[i]) && single != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:reader-family] metamorphic violation: getRealEigenvalue(i) must agree with getRealEigenvalues()[i] input=i=" +
                    i + " lhs=" + single + " rhs=" + eigenValues[i]);
            }

            final double diagonal;
            try {
                diagonal = d.getEntry(i, i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(diagonal) != Double.doubleToLongBits(eigenValues[i]) && diagonal != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:d-diagonal] metamorphic violation: getD() is documented as the diagonal matrix of eigenvalues, so its diagonal must equal getRealEigenvalues() input=i=" +
                    i + " lhs=" + diagonal + " rhs=" + eigenValues[i]);
            }

            for (int j = 0; j < eigenValues.length; ++j) {
                if (i == j) {
                    continue;
                }
                final double off;
                try {
                    off = d.getEntry(i, j);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(off) > 1.0e-12) {
                    throw new RuntimeException(
                        "[oracle:d-offdiag] metamorphic violation: getD() is documented as diagonal for symmetric matrices input=i=" +
                        i + ",j=" + j + " lhs=" + off + " rhs=0.0");
                }
            }
        }

        final RealMatrix v;
        final RealMatrix vt;
        try {
            v = decomposition.getV();
            vt = decomposition.getVT();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            for (int j = 0; j < eigenValues.length; ++j) {
                final double lhs;
                final double rhs;
                try {
                    lhs = vt.getEntry(i, j);
                    rhs = v.getEntry(j, i);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(lhs - rhs) > 1.0e-10) {
                    throw new RuntimeException(
                        "[oracle:v-vt] metamorphic violation: by the EigenDecomposition contract, getVT() must be the transpose of getV() input=i=" +
                        i + ",j=" + j + " lhs=" + lhs + " rhs=" + rhs);
                }
            }
        }
    }

    private static void exerciseFuzzedInputs(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 10);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];

        for (int i = 0; i < n; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            main[i] = base / 16.0;
        }
        for (int i = 0; i < n - 1; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            secondary[i] = base / 16.0;
        }

        try {
            EigenDecompositionImpl decomposition =
                new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
            decomposition.getRealEigenvalues();
            decomposition.getD();
            decomposition.getV();
            decomposition.getVT();
            int idx = data.consumeInt(0, n - 1);
            decomposition.getRealEigenvalue(idx);
            decomposition.getEigenvector(idx);
        } catch (Throwable t) {
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 57 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalue-" + i + "] semantic mismat...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~8,885 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedRegression();
        checkTrustedReaderAgreement();
        exerciseFuzzedInputs(data);
    }

    private static void checkLiftedRegression() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] +
                    " tolerance=0.001");
            }

            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot;
            try {
                dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            } catch (Throwable t) {
                return;
            }

            final double norm;
            try {
                if (dot < 0) {
                    norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
                } else {
                    norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
                }
            } catch (Throwable t) {
                return;
            }

            if (!withinTol(0.0, norm, 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + norm + " tolerance=1.0E-5");
            }
        }
    }

    private static void checkTrustedReaderAgreement() {
        final EigenDecompositionImpl decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        final RealMatrix d;
        try {
            eigenValues = decomposition.getRealEigenvalues();
            d = decomposition.getD();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            final double single;
            try {
                single = decomposition.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(single) != Double.doubleToLongBits(eigenValues[i]) && single != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:reader-family] metamorphic violation: getRealEigenvalue(i) must agree with getRealEigenvalues()[i] input=i=" +
                    i + " lhs=" + single + " rhs=" + eigenValues[i]);
            }

            final double diagonal;
            try {
                diagonal = d.getEntry(i, i);
            } catch (Throwable t) {
                return;
            }

            if (Double.doubleToLongBits(diagonal) != Double.doubleToLongBits(eigenValues[i]) && diagonal != eigenValues[i]) {
                throw new RuntimeException(
                    "[oracle:d-diagonal] metamorphic violation: getD() is documented as the diagonal matrix of eigenvalues, so its diagonal must equal getRealEigenvalues() input=i=" +
                    i + " lhs=" + diagonal + " rhs=" + eigenValues[i]);
            }

            for (int j = 0; j < eigenValues.length; ++j) {
                if (i == j) {
                    continue;
                }
                final double off;
                try {
                    off = d.getEntry(i, j);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(off) > 1.0e-12) {
                    throw new RuntimeException(
                        "[oracle:d-offdiag] metamorphic violation: getD() is documented as diagonal for symmetric matrices input=i=" +
                        i + ",j=" + j + " lhs=" + off + " rhs=0.0");
                }
            }
        }

        final RealMatrix v;
        final RealMatrix vt;
        try {
            v = decomposition.getV();
            vt = decomposition.getVT();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < eigenValues.length; ++i) {
            for (int j = 0; j < eigenValues.length; ++j) {
                final double lhs;
                final double rhs;
                try {
                    lhs = vt.getEntry(i, j);
                    rhs = v.getEntry(j, i);
                } catch (Throwable t) {
                    return;
                }
                if (Math.abs(lhs - rhs) > 1.0e-10) {
                    throw new RuntimeException(
                        "[oracle:v-vt] metamorphic violation: by the EigenDecomposition contract, getVT() must be the transpose of getV() input=i=" +
                        i + ",j=" + j + " lhs=" + lhs + " rhs=" + rhs);
                }
            }
        }
    }

    private static void exerciseFuzzedInputs(FuzzedDataProvider data) {
        int n = data.consumeInt(2, 10);
        double[] main = new double[n];
        double[] secondary = new double[n - 1];

        for (int i = 0; i < n; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            main[i] = base / 16.0;
        }
        for (int i = 0; i < n - 1; ++i) {
            int base = data.consumeInt(-1000000, 1000000);
            secondary[i] = base / 16.0;
        }

        try {
            EigenDecompositionImpl decomposition =
                new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
            decomposition.getRealEigenvalues();
            decomposition.getD();
            decomposition.getV();
            decomposition.getVT();
            int idx = data.consumeInt(0, n - 1);
            decomposition.getRealEigenvalue(idx);
            decomposition.getEigenvector(idx);
        } catch (Throwable t) {
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [96] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalues] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945 tolerance=0.001

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,764 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runReadOnlyAndSiblingChecks();
        runImaginaryNullRelation(data);
        runReversedTridiagonalRelation(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }

            final RealVector expectedVector = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            final double dot = expectedVector.dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = expectedVector.add(actualVector).getNorm();
            } else {
                norm = expectedVector.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvectors-" + i + "] semantic mismatch: expectedNormDistance=0.0 actualNormDistance=" + norm + " tolerance=1.0E-5 dot=" + dot);
            }
        }
    }

    private static void runReadOnlyAndSiblingChecks() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] beforeValues;
        final double determinantBefore;
        final double[] imagBefore;
        try {
            beforeValues = dec.getRealEigenvalues();
            determinantBefore = dec.getDeterminant();
            imagBefore = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        try {
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] afterValues;
        final double determinantAfter;
        final double[] imagAfter;
        try {
            afterValues = dec.getRealEigenvalues();
            determinantAfter = dec.getDeterminant();
            imagAfter = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: get* methods are readers. A throw-deleting or bookkeeping-skipping patch in the decomposition path
           can silently corrupt shared state (work/pingPong-derived results); these read-only calls must still agree before/after. */
        String readOnlyViolation = compareDoubleArrays(beforeValues, afterValues, 0.0, "[oracle:readonly-realEigenvalues] metamorphic violation: get* readers changed real eigenvalues");
        if (readOnlyViolation != null) {
            throw new FuzzerSecurityIssueLow(readOnlyViolation);
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-determinant] metamorphic violation: get* readers changed determinant before=" + determinantBefore + " after=" + determinantAfter);
        }
        if ((imagBefore == null) != (imagAfter == null)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-imaginary] metamorphic violation: get* readers changed imaginary array nullness beforeNull=" + (imagBefore == null) + " afterNull=" + (imagAfter == null));
        }
        if (imagBefore != null) {
            String imagViolation = compareDoubleArrays(imagBefore, imagAfter, 0.0, "[oracle:readonly-imaginary] metamorphic violation: get* readers changed imaginary eigenvalues");
            if (imagViolation != null) {
                throw new FuzzerSecurityIssueLow(imagViolation);
            }
        }

        /* Contract justification: family readers for the same decomposition must agree on the same logical data.
           getRealEigenvalues()[i] and getRealEigenvalue(i) report the same eigenvalue; getV().getColumnVector(i) and getEigenvector(i)
           both expose eigenvectors. Shared-state corruption from flipIfWarranted/processGeneralBlock is observable here even if one reader alone looks plausible. */
        for (int i = 0; i < beforeValues.length; i++) {
            final double byIndex;
            final RealVector ev;
            final RealVector col;
            try {
                byIndex = dec.getRealEigenvalue(i);
                ev = dec.getEigenvector(i);
                col = dec.getV().getColumnVector(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.isNaN(beforeValues[i]) != Double.isNaN(byIndex) || Math.abs(beforeValues[i] - byIndex) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-realEigenvalue-" + i + "] metamorphic violation: getRealEigenvalues()[" + i + "]=" + beforeValues[i] + " getRealEigenvalue(" + i + ")=" + byIndex);
            }
            double dot = ev.dotProduct(col);
            double norm = dot < 0 ? ev.add(col).getNorm() : ev.subtract(col).getNorm();
            if (norm > 1.0e-10) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-eigenvector-" + i + "] metamorphic violation: getEigenvector(" + i + ") and getV().getColumnVector(" + i + ") differ by norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: this symmetric-only implementation returns null imaginary eigenvalue arrays.
           A patch that only suppresses the failing path but leaves internal state inconsistent can violate this observable post-condition. */
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversedTridiagonalRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] ev1;
        final double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + (ev1 == null ? -1 : ev1.length) + " vs " + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }

    private static String compareDoubleArrays(double[] a, double[] b, double tol, String prefix) {
        if (a == null || b == null) {
            return prefix + " nullMismatch aNull=" + (a == null) + " bNull=" + (b == null);
        }
        if (a.length != b.length) {
            return prefix + " lengthMismatch aLen=" + a.length + " bLen=" + b.length;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) != Double.isNaN(y)) {
                return prefix + " index=" + i + " a=" + x + " b=" + y;
            }
            if (!Double.isNaN(x) && Math.abs(x - y) > tol) {
                return prefix + " index=" + i + " a=" + x + " b=" + y + " tol=" + tol;
            }
        }
        return null;
    }
}
```

</details>

---
## [98] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 66 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + expec

---
## [99] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~13,505 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runReadOnlyAndSiblingChecks();
        runImaginaryNullRelation(data);
        runReversedTridiagonalRelation(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }

            final RealVector expectedVector = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            final double dot = expectedVector.dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = expectedVector.add(actualVector).getNorm();
            } else {
                norm = expectedVector.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvectors-" + i + "] semantic mismatch: expectedNormDistance=0.0 actualNormDistance=" + norm + " tolerance=1.0E-5 dot=" + dot);
            }
        }
    }

    private static void runReadOnlyAndSiblingChecks() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] beforeValues;
        final double determinantBefore;
        final double[] imagBefore;
        try {
            beforeValues = dec.getRealEigenvalues();
            determinantBefore = dec.getDeterminant();
            imagBefore = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        try {
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] afterValues;
        final double determinantAfter;
        final double[] imagAfter;
        try {
            afterValues = dec.getRealEigenvalues();
            determinantAfter = dec.getDeterminant();
            imagAfter = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: get* methods are readers. A throw-deleting or bookkeeping-skipping patch in the decomposition path
           can silently corrupt shared state (work/pingPong-derived results); these read-only calls must still agree before/after. */
        String readOnlyViolation = compareDoubleArrays(beforeValues, afterValues, 0.0, "[oracle:readonly-realEigenvalues] metamorphic violation: get* readers changed real eigenvalues");
        if (readOnlyViolation != null) {
            throw new FuzzerSecurityIssueLow(readOnlyViolation);
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-determinant] metamorphic violation: get* readers changed determinant before=" + determinantBefore + " after=" + determinantAfter);
        }
        if ((imagBefore == null) != (imagAfter == null)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-imaginary] metamorphic violation: get* readers changed imaginary array nullness beforeNull=" + (imagBefore == null) + " afterNull=" + (imagAfter == null));
        }
        if (imagBefore != null) {
            String imagViolation = compareDoubleArrays(imagBefore, imagAfter, 0.0, "[oracle:readonly-imaginary] metamorphic violation: get* readers changed imaginary eigenvalues");
            if (imagViolation != null) {
                throw new FuzzerSecurityIssueLow(imagViolation);
            }
        }

        /* Contract justification: family readers for the same decomposition must agree on the same logical data.
           getRealEigenvalues()[i] and getRealEigenvalue(i) report the same eigenvalue; getV().getColumnVector(i) and getEigenvector(i)
           both expose eigenvectors. Shared-state corruption from flipIfWarranted/processGeneralBlock is observable here even if one reader alone looks plausible. */
        for (int i = 0; i < beforeValues.length; i++) {
            final double byIndex;
            final RealVector ev;
            final RealVector col;
            try {
                byIndex = dec.getRealEigenvalue(i);
                ev = dec.getEigenvector(i);
                col = dec.getV().getColumnVector(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.isNaN(beforeValues[i]) != Double.isNaN(byIndex) || Math.abs(beforeValues[i] - byIndex) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-realEigenvalue-" + i + "] metamorphic violation: getRealEigenvalues()[" + i + "]=" + beforeValues[i] + " getRealEigenvalue(" + i + ")=" + byIndex);
            }
            double dot = ev.dotProduct(col);
            double norm = dot < 0 ? ev.add(col).getNorm() : ev.subtract(col).getNorm();
            if (norm > 1.0e-10) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-eigenvector-" + i + "] metamorphic violation: getEigenvector(" + i + ") and getV().getColumnVector(" + i + ") differ by norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: this symmetric-only implementation returns null imaginary eigenvalue arrays.
           A patch that only suppresses the failing path but leaves internal state inconsistent can violate this observable post-condition. */
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversedTridiagonalRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] ev1;
        final double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + (ev1 == null ? -1 : ev1.length) + " vs " + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }

    private static String compareDoubleArrays(double[] a, double[] b, double tol, String prefix) {
        if (a == null || b == null) {
            return prefix + " nullMismatch aNull=" + (a == null) + " bNull=" + (b == null);
        }
        if (a.length != b.length) {
            return prefix + " lengthMismatch aLen=" + a.length + " bLen=" + b.length;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) != Double.isNaN(y)) {
                return prefix + " index=" + i + " a=" + x + " b=" + y;
            }
            if (!Double.isNaN(x) && Math.abs(x - y) > tol) {
                return prefix + " index=" + i + " a=" + x + " b=" + y + " tol=" + tol;
            }
        }
        return null;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 66 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + expected + " act...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~12,881 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runReadOnlyAndSiblingChecks();
        runImaginaryNullRelation(data);
        runReversedTridiagonalRelation(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }

            final RealVector expectedVector = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            final double dot = expectedVector.dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = expectedVector.add(actualVector).getNorm();
            } else {
                norm = expectedVector.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormDistance=0.0 actualNormDistance=" + norm + " tolerance=1.0E-5 dot=" + dot);
            }
        }
    }

    private static void runReadOnlyAndSiblingChecks() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] beforeValues;
        final double determinantBefore;
        final double[] imagBefore;
        try {
            beforeValues = dec.getRealEigenvalues();
            determinantBefore = dec.getDeterminant();
            imagBefore = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        try {
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] afterValues;
        final double determinantAfter;
        final double[] imagAfter;
        try {
            afterValues = dec.getRealEigenvalues();
            determinantAfter = dec.getDeterminant();
            imagAfter = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: get* methods are readers. A throw-deleting or bookkeeping-skipping patch in the decomposition path
           can silently corrupt shared state (work/pingPong-derived results); these read-only calls must still agree before/after. */
        String readOnlyViolation = compareDoubleArrays(beforeValues, afterValues, 0.0, "[oracle:readonly-realEigenvalues] metamorphic violation: get* readers changed real eigenvalues");
        if (readOnlyViolation != null) {
            throw new FuzzerSecurityIssueLow(readOnlyViolation);
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-determinant] metamorphic violation: get* readers changed determinant before=" + determinantBefore + " after=" + determinantAfter);
        }
        if ((imagBefore == null) != (imagAfter == null)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-imaginary-nullness] metamorphic violation: get* readers changed imaginary array nullness beforeNull=" + (imagBefore == null) + " afterNull=" + (imagAfter == null));
        }
        if (imagBefore != null) {
            String imagViolation = compareDoubleArrays(imagBefore, imagAfter, 0.0, "[oracle:readonly-imaginary-values] metamorphic violation: get* readers changed imaginary eigenvalues");
            if (imagViolation != null) {
                throw new FuzzerSecurityIssueLow(imagViolation);
            }
        }

        /* Contract justification: family readers for the same decomposition must agree on the same logical data.
           getRealEigenvalues()[i] and getRealEigenvalue(i) report the same eigenvalue; getV().getColumnVector(i) and getEigenvector(i)
           both expose eigenvectors. Shared-state corruption from flipIfWarranted/processGeneralBlock is observable here even if one reader alone looks plausible. */
        for (int i = 0; i < beforeValues.length; i++) {
            final double byIndex;
            final RealVector ev;
            final RealVector col;
            try {
                byIndex = dec.getRealEigenvalue(i);
                ev = dec.getEigenvector(i);
                col = dec.getV().getColumnVector(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.isNaN(beforeValues[i]) != Double.isNaN(byIndex) || Math.abs(beforeValues[i] - byIndex) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-realEigenvalue-" + i + "] metamorphic violation: getRealEigenvalues()[" + i + "]=" + beforeValues[i] + " getRealEigenvalue(" + i + ")=" + byIndex);
            }
            double dot = ev.dotProduct(col);
            double norm = dot < 0 ? ev.add(col).getNorm() : ev.subtract(col).getNorm();
            if (norm > 1.0e-10) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-eigenvector-" + i + "] metamorphic violation: getEigenvector(" + i + ") and getV().getColumnVector(" + i + ") differ by norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: this symmetric-only implementation returns null imaginary eigenvalue arrays.
           A patch that only suppresses the failing path but leaves internal state inconsistent can violate this observable post-condition. */
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversedTridiagonalRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] ev1;
        final double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + (ev1 == null ? -1 : ev1.length) + " vs " + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-value-" + i + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }

    private static String compareDoubleArrays(double[] a, double[] b, double tol, String prefix) {
        if (a == null || b == null) {
            return prefix + " nullMismatch aNull=" + (a == null) + " bNull=" + (b == null);
        }
        if (a.length != b.length) {
            return prefix + " lengthMismatch aLen=" + a.length + " bLen=" + b.length;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) != Double.isNaN(y)) {
                return prefix + " index=" + i + " a=" + x + " b=" + y;
            }
            if (!Double.isNaN(x) && Math.abs(x - y) > tol) {
                return prefix + " index=" + i + " a=" + x + " b=" + y + " tol=" + tol;
            }
        }
        return null;
    }
}
```

</details>

---
## [100] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 66 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expect

---
## [101] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~13,743 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [99] — not reprinted*
- *[user] message: identical to step [99] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runReadOnlyAndSiblingChecks();
        runImaginaryNullRelation(data);
        runReversedTridiagonalRelation(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }

            final RealVector expectedVector = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            final double dot = expectedVector.dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = expectedVector.add(actualVector).getNorm();
            } else {
                norm = expectedVector.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormDistance=0.0 actualNormDistance=" + norm + " tolerance=1.0E-5 dot=" + dot);
            }
        }
    }

    private static void runReadOnlyAndSiblingChecks() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] beforeValues;
        final double determinantBefore;
        final double[] imagBefore;
        try {
            beforeValues = dec.getRealEigenvalues();
            determinantBefore = dec.getDeterminant();
            imagBefore = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        try {
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] afterValues;
        final double determinantAfter;
        final double[] imagAfter;
        try {
            afterValues = dec.getRealEigenvalues();
            determinantAfter = dec.getDeterminant();
            imagAfter = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: get* methods are readers. A throw-deleting or bookkeeping-skipping patch in the decomposition path
           can silently corrupt shared state (work/pingPong-derived results); these read-only calls must still agree before/after. */
        String readOnlyViolation = compareDoubleArrays(beforeValues, afterValues, 0.0, "[oracle:readonly-realEigenvalues] metamorphic violation: get* readers changed real eigenvalues");
        if (readOnlyViolation != null) {
            throw new FuzzerSecurityIssueLow(readOnlyViolation);
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-determinant] metamorphic violation: get* readers changed determinant before=" + determinantBefore + " after=" + determinantAfter);
        }
        if ((imagBefore == null) != (imagAfter == null)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-imaginary-nullness] metamorphic violation: get* readers changed imaginary array nullness beforeNull=" + (imagBefore == null) + " afterNull=" + (imagAfter == null));
        }
        if (imagBefore != null) {
            String imagViolation = compareDoubleArrays(imagBefore, imagAfter, 0.0, "[oracle:readonly-imaginary-values] metamorphic violation: get* readers changed imaginary eigenvalues");
            if (imagViolation != null) {
                throw new FuzzerSecurityIssueLow(imagViolation);
            }
        }

        /* Contract justification: family readers for the same decomposition must agree on the same logical data.
           getRealEigenvalues()[i] and getRealEigenvalue(i) report the same eigenvalue; getV().getColumnVector(i) and getEigenvector(i)
           both expose eigenvectors. Shared-state corruption from flipIfWarranted/processGeneralBlock is observable here even if one reader alone looks plausible. */
        for (int i = 0; i < beforeValues.length; i++) {
            final double byIndex;
            final RealVector ev;
            final RealVector col;
            try {
                byIndex = dec.getRealEigenvalue(i);
                ev = dec.getEigenvector(i);
                col = dec.getV().getColumnVector(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.isNaN(beforeValues[i]) != Double.isNaN(byIndex) || Math.abs(beforeValues[i] - byIndex) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-realEigenvalue-" + i + "] metamorphic violation: getRealEigenvalues()[" + i + "]=" + beforeValues[i] + " getRealEigenvalue(" + i + ")=" + byIndex);
            }
            double dot = ev.dotProduct(col);
            double norm = dot < 0 ? ev.add(col).getNorm() : ev.subtract(col).getNorm();
            if (norm > 1.0e-10) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-eigenvector-" + i + "] metamorphic violation: getEigenvector(" + i + ") and getV().getColumnVector(" + i + ") differ by norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: this symmetric-only implementation returns null imaginary eigenvalue arrays.
           A patch that only suppresses the failing path but leaves internal state inconsistent can violate this observable post-condition. */
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversedTridiagonalRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] ev1;
        final double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + (ev1 == null ? -1 : ev1.length) + " vs " + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-value-" + i + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }

    private static String compareDoubleArrays(double[] a, double[] b, double tol, String prefix) {
        if (a == null || b == null) {
            return prefix + " nullMismatch aNull=" + (a == null) + " bNull=" + (b == null);
        }
        if (a.length != b.length) {
            return prefix + " lengthMismatch aLen=" + a.length + " bLen=" + b.length;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) != Double.isNaN(y)) {
                return prefix + " index=" + i + " a=" + x + " b=" + y;
            }
            if (!Double.isNaN(x) && Math.abs(x - y) > tol) {
                return prefix + " index=" + i + " a=" + x + " b=" + y + " tol=" + tol;
            }
        }
        return null;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 66 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actu...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~14,486 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    private static final String[] EIGENVALUE_IDS = {
        "lifted-eigenvalue-0",
        "lifted-eigenvalue-1",
        "lifted-eigenvalue-2",
        "lifted-eigenvalue-3",
        "lifted-eigenvalue-4",
        "lifted-eigenvalue-5",
        "lifted-eigenvalue-6"
    };

    private static final String[] EIGENVECTOR_IDS = {
        "lifted-eigenvector-0",
        "lifted-eigenvector-1",
        "lifted-eigenvector-2",
        "lifted-eigenvector-3",
        "lifted-eigenvector-4",
        "lifted-eigenvector-5",
        "lifted-eigenvector-6"
    };

    private static final String[] SIBLING_REAL_IDS = {
        "sibling-realEigenvalue-0",
        "sibling-realEigenvalue-1",
        "sibling-realEigenvalue-2",
        "sibling-realEigenvalue-3",
        "sibling-realEigenvalue-4",
        "sibling-realEigenvalue-5",
        "sibling-realEigenvalue-6"
    };

    private static final String[] SIBLING_VECTOR_IDS = {
        "sibling-eigenvector-0",
        "sibling-eigenvector-1",
        "sibling-eigenvector-2",
        "sibling-eigenvector-3",
        "sibling-eigenvector-4",
        "sibling-eigenvector-5",
        "sibling-eigenvector-6"
    };

    private static final String[] REVERSE_VALUE_IDS = {
        "reverse-multiset-value-0",
        "reverse-multiset-value-1",
        "reverse-multiset-value-2",
        "reverse-multiset-value-3",
        "reverse-multiset-value-4",
        "reverse-multiset-value-5",
        "reverse-multiset-value-6",
        "reverse-multiset-value-7"
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runReadOnlyAndSiblingChecks();
        runImaginaryNullRelation(data);
        runReversedTridiagonalRelation(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (eigenValues == null || eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-count] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + (eigenValues == null ? -1 : eigenValues.length));
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVALUE_IDS[i] + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }

            final RealVector expectedVector = new ArrayRealVector(REF_EIGENVECTORS[i]);
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            final double dot = expectedVector.dotProduct(actualVector);
            final double norm;
            if (dot < 0) {
                norm = expectedVector.add(actualVector).getNorm();
            } else {
                norm = expectedVector.subtract(actualVector).getNorm();
            }
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVECTOR_IDS[i] + "] semantic mismatch: expectedNormDistance=0.0 actualNormDistance=" + norm + " tolerance=1.0E-5 dot=" + dot);
            }
        }
    }

    private static void runReadOnlyAndSiblingChecks() {
        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] beforeValues;
        final double determinantBefore;
        final double[] imagBefore;
        try {
            beforeValues = dec.getRealEigenvalues();
            determinantBefore = dec.getDeterminant();
            imagBefore = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        try {
            dec.getV();
            dec.getVT();
            dec.getD();
            dec.getSolver();
        } catch (Throwable t) {
            return;
        }

        final double[] afterValues;
        final double determinantAfter;
        final double[] imagAfter;
        try {
            afterValues = dec.getRealEigenvalues();
            determinantAfter = dec.getDeterminant();
            imagAfter = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: get* methods are readers. A throw-deleting or bookkeeping-skipping patch in the decomposition path
           can silently corrupt shared state (work/pingPong-derived results); these read-only calls must still agree before/after. */
        String readOnlyViolation = compareDoubleArrays(beforeValues, afterValues, 0.0, "[oracle:readonly-realEigenvalues] metamorphic violation: get* readers changed real eigenvalues");
        if (readOnlyViolation != null) {
            throw new FuzzerSecurityIssueLow(readOnlyViolation);
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-determinant] metamorphic violation: get* readers changed determinant before=" + determinantBefore + " after=" + determinantAfter);
        }
        if ((imagBefore == null) != (imagAfter == null)) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-imaginary-nullness] metamorphic violation: get* readers changed imaginary array nullness beforeNull=" + (imagBefore == null) + " afterNull=" + (imagAfter == null));
        }
        if (imagBefore != null) {
            String imagViolation = compareDoubleArrays(imagBefore, imagAfter, 0.0, "[oracle:readonly-imaginary-values] metamorphic violation: get* readers changed imaginary eigenvalues");
            if (imagViolation != null) {
                throw new FuzzerSecurityIssueLow(imagViolation);
            }
        }

        /* Contract justification: family readers for the same decomposition must agree on the same logical data.
           getRealEigenvalues()[i] and getRealEigenvalue(i) report the same eigenvalue; getV().getColumnVector(i) and getEigenvector(i)
           both expose eigenvectors. Shared-state corruption from flipIfWarranted/processGeneralBlock is observable here even if one reader alone looks plausible. */
        for (int i = 0; i < beforeValues.length; i++) {
            final double byIndex;
            final RealVector ev;
            final RealVector col;
            try {
                byIndex = dec.getRealEigenvalue(i);
                ev = dec.getEigenvector(i);
                col = dec.getV().getColumnVector(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.isNaN(beforeValues[i]) != Double.isNaN(byIndex) || Math.abs(beforeValues[i] - byIndex) > 0.0) {
                throw new FuzzerSecurityIssueLow("[oracle:" + SIBLING_REAL_IDS[i] + "] metamorphic violation: getRealEigenvalues()[" + i + "]=" + beforeValues[i] + " getRealEigenvalue(" + i + ")=" + byIndex);
            }
            double dot = ev.dotProduct(col);
            double norm = dot < 0 ? ev.add(col).getNorm() : ev.subtract(col).getNorm();
            if (norm > 1.0e-10) {
                throw new FuzzerSecurityIssueLow("[oracle:" + SIBLING_VECTOR_IDS[i] + "] metamorphic violation: getEigenvector(" + i + ") and getV().getColumnVector(" + i + ") differ by norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] imag;
        try {
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification: this symmetric-only implementation returns null imaginary eigenvalue arrays.
           A patch that only suppresses the failing path but leaves internal state inconsistent can violate this observable post-condition. */
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imaginary-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversedTridiagonalRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] ev1;
        final double[] ev2;
        try {
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1 == null || ev2 == null || ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + (ev1 == null ? -1 : ev1.length) + " vs " + (ev2 == null ? -1 : ev2.length));
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                String id = i < REVERSE_VALUE_IDS.length ? REVERSE_VALUE_IDS[i] : "reverse-multiset-value-extra";
                throw new FuzzerSecurityIssueLow("[oracle:" + id + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }

    private static String compareDoubleArrays(double[] a, double[] b, double tol, String prefix) {
        if (a == null || b == null) {
            return prefix + " nullMismatch aNull=" + (a == null) + " bNull=" + (b == null);
        }
        if (a.length != b.length) {
            return prefix + " lengthMismatch aLen=" + a.length + " bLen=" + b.length;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) != Double.isNaN(y)) {
                return prefix + " index=" + i + " a=" + x + " b=" + y;
            }
            if (!Double.isNaN(x) && Math.abs(x - y) > tol) {
                return prefix + " index=" + i + " a=" + x + " b=" + y + " tol=" + tol;
            }
        }
        return null;
    }
}
```

</details>

---
## [102] ⚙️ harness-repair · `attempt_013`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [103] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: missing-alarm-id]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['missing-alarm-id'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalue-1] semantic mismatch: expected=16828.208208485466 actual=20654.74511575945 tolerance=0.001

---
## [104] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,764 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN_TRIDIAGONAL = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY_TRIDIAGONAL = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN_TRIDIAGONAL, SECONDARY_TRIDIAGONAL, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTolerance(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eigenvalue-" + i + "] semantic mismatch: expected=" + REF_EIGENVALUES[i] +
                    " actual=" + eigenValues[i]);
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eigenvector-" + i + "] semantic mismatch: expectedReference=" +
                    vectorToString(REF_EIGENVECTORS[i]) + " actual=" + vectorToString(actualVector) +
                    " normDiff=" + norm + " dot=" + dot);
            }
        }

        RealMatrix d = decomposition.getD();
        double[] eigenValuesAgain = decomposition.getRealEigenvalues();
        for (int i = 0; i < eigenValuesAgain.length; ++i) {
            double byIndex = decomposition.getRealEigenvalue(i);
            double onDiagonal = d.getEntry(i, i);
            if (!withinTolerance(eigenValuesAgain[i], byIndex, 1.0e-12) ||
                !withinTolerance(eigenValuesAgain[i], onDiagonal, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:accessor-agreement] metamorphic violation: getRealEigenvalues/getRealEigenvalue/getD diagonal disagree index=" +
                    i + " arrayValue=" + eigenValuesAgain[i] + " indexedValue=" + byIndex +
                    " diagonalValue=" + onDiagonal);
            }
            for (int j = 0; j < eigenValuesAgain.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0) {
                    throw new RuntimeException(
                        "[oracle:d-diagonal] metamorphic violation: getD must be diagonal for symmetric decomposition index=(" +
                        i + "," + j + ") value=" + d.getEntry(i, j));
                }
            }
        }

        try {
            int n = data.consumeInt(2, 8);
            double[] main = new double[n];
            double[] secondary = new double[n - 1];
            for (int i = 0; i < n; ++i) {
                main[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }
            for (int i = 0; i < n - 1; ++i) {
                secondary[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }

            EigenDecomposition fuzzDecomposition =
                new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);

            double[] vals = fuzzDecomposition.getRealEigenvalues();
            RealMatrix fuzzD = fuzzDecomposition.getD();

            for (int i = 0; i < vals.length; ++i) {
                double indexed = fuzzDecomposition.getRealEigenvalue(i);
                double diag = fuzzD.getEntry(i, i);
                if (!withinTolerance(vals[i], indexed, 1.0e-9) ||
                    !withinTolerance(vals[i], diag, 1.0e-9)) {
                    throw new RuntimeException(
                        "[oracle:fuzz-accessor-agreement] metamorphic violation: eigenvalue readers disagree n=" +
                        n + " index=" + i + " arrayValue=" + vals[i] + " indexedValue=" + indexed +
                        " diagonalValue=" + diag);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean withinTolerance(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }

    private static String vectorToString(RealVector v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.getDimension(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v.getEntry(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [105] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 42 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:eigenvalue-" + i + "] semantic mi

---
## [106] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,505 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN_TRIDIAGONAL = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY_TRIDIAGONAL = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN_TRIDIAGONAL, SECONDARY_TRIDIAGONAL, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTolerance(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eigenvalue-" + i + "] semantic mismatch: expected=" + REF_EIGENVALUES[i] +
                    " actual=" + eigenValues[i]);
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eigenvector-" + i + "] semantic mismatch: expectedReference=" +
                    vectorToString(REF_EIGENVECTORS[i]) + " actual=" + vectorToString(actualVector) +
                    " normDiff=" + norm + " dot=" + dot);
            }
        }

        RealMatrix d = decomposition.getD();
        double[] eigenValuesAgain = decomposition.getRealEigenvalues();
        for (int i = 0; i < eigenValuesAgain.length; ++i) {
            double byIndex = decomposition.getRealEigenvalue(i);
            double onDiagonal = d.getEntry(i, i);
            if (!withinTolerance(eigenValuesAgain[i], byIndex, 1.0e-12) ||
                !withinTolerance(eigenValuesAgain[i], onDiagonal, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:accessor-agreement] metamorphic violation: getRealEigenvalues/getRealEigenvalue/getD diagonal disagree index=" +
                    i + " arrayValue=" + eigenValuesAgain[i] + " indexedValue=" + byIndex +
                    " diagonalValue=" + onDiagonal);
            }
            for (int j = 0; j < eigenValuesAgain.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0) {
                    throw new RuntimeException(
                        "[oracle:d-diagonal] metamorphic violation: getD must be diagonal for symmetric decomposition index=(" +
                        i + "," + j + ") value=" + d.getEntry(i, j));
                }
            }
        }

        try {
            int n = data.consumeInt(2, 8);
            double[] main = new double[n];
            double[] secondary = new double[n - 1];
            for (int i = 0; i < n; ++i) {
                main[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }
            for (int i = 0; i < n - 1; ++i) {
                secondary[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }

            EigenDecomposition fuzzDecomposition =
                new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);

            double[] vals = fuzzDecomposition.getRealEigenvalues();
            RealMatrix fuzzD = fuzzDecomposition.getD();

            for (int i = 0; i < vals.length; ++i) {
                double indexed = fuzzDecomposition.getRealEigenvalue(i);
                double diag = fuzzD.getEntry(i, i);
                if (!withinTolerance(vals[i], indexed, 1.0e-9) ||
                    !withinTolerance(vals[i], diag, 1.0e-9)) {
                    throw new RuntimeException(
                        "[oracle:fuzz-accessor-agreement] metamorphic violation: eigenvalue readers disagree n=" +
                        n + " index=" + i + " arrayValue=" + vals[i] + " indexedValue=" + indexed +
                        " diagonalValue=" + diag);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean withinTolerance(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }

    private static String vectorToString(RealVector v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.getDimension(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v.getEntry(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 42 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:eigenvalue-" + i + "] semantic mismatch: exp...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,789 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN_TRIDIAGONAL = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY_TRIDIAGONAL = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final ArrayRealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN_TRIDIAGONAL, SECONDARY_TRIDIAGONAL, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTolerance(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]);
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedReference=" + vectorToString(REF_EIGENVECTORS[i]) +
                    " actual=" + vectorToString(actualVector) +
                    " normDiff=" + norm + " dot=" + dot);
            }
        }

        RealMatrix d = decomposition.getD();
        double[] eigenValuesAgain = decomposition.getRealEigenvalues();
        for (int i = 0; i < eigenValuesAgain.length; ++i) {
            double byIndex = decomposition.getRealEigenvalue(i);
            double onDiagonal = d.getEntry(i, i);
            if (!withinTolerance(eigenValuesAgain[i], byIndex, 1.0e-12) ||
                !withinTolerance(eigenValuesAgain[i], onDiagonal, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:seed-accessor-agreement] metamorphic violation: getRealEigenvalues/getRealEigenvalue/getD diagonal disagree index=" +
                    i + " arrayValue=" + eigenValuesAgain[i] + " indexedValue=" + byIndex +
                    " diagonalValue=" + onDiagonal);
            }
            for (int j = 0; j < eigenValuesAgain.length; ++j) {
                if (i != j && d.getEntry(i, j) != 0.0) {
                    throw new RuntimeException(
                        "[oracle:seed-d-diagonal] metamorphic violation: getD must be diagonal for symmetric decomposition index=(" +
                        i + "," + j + ") value=" + d.getEntry(i, j));
                }
            }
        }

        try {
            int n = data.consumeInt(2, 8);
            double[] main = new double[n];
            double[] secondary = new double[n - 1];
            for (int i = 0; i < n; ++i) {
                main[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }
            for (int i = 0; i < n - 1; ++i) {
                secondary[i] = data.consumeInt(-1000000, 1000000) / 100.0;
            }

            EigenDecomposition fuzzDecomposition =
                new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);

            double[] vals = fuzzDecomposition.getRealEigenvalues();
            RealMatrix fuzzD = fuzzDecomposition.getD();

            for (int i = 0; i < vals.length; ++i) {
                double indexed = fuzzDecomposition.getRealEigenvalue(i);
                double diag = fuzzD.getEntry(i, i);
                if (!withinTolerance(vals[i], indexed, 1.0e-9) ||
                    !withinTolerance(vals[i], diag, 1.0e-9)) {
                    throw new RuntimeException(
                        "[oracle:fuzz-accessor-agreement] metamorphic violation: eigenvalue readers disagree n=" +
                        n + " index=" + i + " arrayValue=" + vals[i] + " indexedValue=" + indexed +
                        " diagonalValue=" + diag);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean withinTolerance(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }

    private static String vectorToString(RealVector v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.getDimension(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v.getEntry(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
```

</details>

---
## [107] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalues] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945

---
## [108] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,227 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = new double[] {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = new double[] {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = new double[] {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = new RealVector[] {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedOracleFromFailingTest();
        readOnlyAndSiblingAgreementOnSeed();
        relationImaginaryEigenvaluesNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedOracleFromFailingTest() {
        final EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected="
                        + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            final RealVector actualEigenvector = decomposition.getEigenvector(i);
            final double dot = REF_EIGENVECTORS[i].dotProduct(actualEigenvector);
            final double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualEigenvector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualEigenvector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvector-" + i + "] semantic mismatch: expectedNormToReference=0.0 actualNorm="
                        + norm + " index=" + i + " dot=" + dot);
            }
        }
    }

    private static void readOnlyAndSiblingAgreementOnSeed() {
        final EigenDecompositionImpl dec =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] before = dec.getRealEigenvalues();
        final double[] beforeClone = before.clone();
        final double determinantBefore = dec.getDeterminant();

        final double[] after = dec.getRealEigenvalues();
        final double determinantAfter = dec.getDeterminant();

        /* Contract justification: get* methods are readers; repeated reads on the same decomposition
           should not mutate observable state. A patch that only suppresses/skirts the buggy branch can
           still leave corrupted work/pingPong state that later readers expose differently. */
        if (before.length != after.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-length] semantic mismatch: beforeLength=" + before.length
                    + " afterLength=" + after.length);
        }
        for (int i = 0; i < before.length; i++) {
            if (Double.doubleToLongBits(beforeClone[i]) != Double.doubleToLongBits(after[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-readonly-eigenvalues] semantic mismatch: index=" + i
                        + " before=" + beforeClone[i] + " after=" + after[i]);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-determinant] semantic mismatch: before=" + determinantBefore
                    + " after=" + determinantAfter);
        }

        /* Contract justification: getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers
           for the same decomposition and must agree on each index for any correct implementation. */
        for (int i = 0; i < before.length; i++) {
            final double scalar = dec.getRealEigenvalue(i);
            final double arrayValue = after[i];
            if (Double.doubleToLongBits(scalar) != Double.doubleToLongBits(arrayValue)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-sibling-real-eigenvalue-" + i + "] semantic mismatch: scalar="
                        + scalar + " array=" + arrayValue + " index=" + i);
            }
        }

        /* Contract justification: this symmetric-only implementation reports no imaginary eigenvalues;
           getImagEigenvalues() must therefore be null for a valid symmetric tridiagonal decomposition. */
        final double[] imag = dec.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }
    }

    private static void relationImaginaryEigenvaluesNull(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        final double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length "
                    + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        final double[] rMain = new double[n];
        final double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        final double[] ev1;
        final double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                    + ev1.length + " vs " + ev2.length);
        }

        final double[] a = ev1.clone();
        final double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            final double x = a[i];
            final double y = b[i];
            final double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index="
                        + i + " lhs=" + x + " rhs=" + y);
            }
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [109] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 51 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected=" + REF_EIG

---
## [110] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,968 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = new double[] {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = new double[] {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = new double[] {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = new RealVector[] {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedOracleFromFailingTest();
        readOnlyAndSiblingAgreementOnSeed();
        relationImaginaryEigenvaluesNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedOracleFromFailingTest() {
        final EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected="
                        + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            final RealVector actualEigenvector = decomposition.getEigenvector(i);
            final double dot = REF_EIGENVECTORS[i].dotProduct(actualEigenvector);
            final double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualEigenvector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualEigenvector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvector-" + i + "] semantic mismatch: expectedNormToReference=0.0 actualNorm="
                        + norm + " index=" + i + " dot=" + dot);
            }
        }
    }

    private static void readOnlyAndSiblingAgreementOnSeed() {
        final EigenDecompositionImpl dec =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] before = dec.getRealEigenvalues();
        final double[] beforeClone = before.clone();
        final double determinantBefore = dec.getDeterminant();

        final double[] after = dec.getRealEigenvalues();
        final double determinantAfter = dec.getDeterminant();

        /* Contract justification: get* methods are readers; repeated reads on the same decomposition
           should not mutate observable state. A patch that only suppresses/skirts the buggy branch can
           still leave corrupted work/pingPong state that later readers expose differently. */
        if (before.length != after.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-length] semantic mismatch: beforeLength=" + before.length
                    + " afterLength=" + after.length);
        }
        for (int i = 0; i < before.length; i++) {
            if (Double.doubleToLongBits(beforeClone[i]) != Double.doubleToLongBits(after[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-readonly-eigenvalues] semantic mismatch: index=" + i
                        + " before=" + beforeClone[i] + " after=" + after[i]);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-determinant] semantic mismatch: before=" + determinantBefore
                    + " after=" + determinantAfter);
        }

        /* Contract justification: getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers
           for the same decomposition and must agree on each index for any correct implementation. */
        for (int i = 0; i < before.length; i++) {
            final double scalar = dec.getRealEigenvalue(i);
            final double arrayValue = after[i];
            if (Double.doubleToLongBits(scalar) != Double.doubleToLongBits(arrayValue)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-sibling-real-eigenvalue-" + i + "] semantic mismatch: scalar="
                        + scalar + " array=" + arrayValue + " index=" + i);
            }
        }

        /* Contract justification: this symmetric-only implementation reports no imaginary eigenvalues;
           getImagEigenvalues() must therefore be null for a valid symmetric tridiagonal decomposition. */
        final double[] imag = dec.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }
    }

    private static void relationImaginaryEigenvaluesNull(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        final double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length "
                    + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        final double[] rMain = new double[n];
        final double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        final double[] ev1;
        final double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                    + ev1.length + " vs " + ev2.length);
        }

        final double[] a = ev1.clone();
        final double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            final double x = a[i];
            final double y = b[i];
            final double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: index="
                        + i + " lhs=" + x + " rhs=" + y);
            }
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 51 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected=" + REF_EIGENVALUES[i]...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~10,137 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = new double[] {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = new double[] {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = new double[] {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = new RealVector[] {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedOracleFromFailingTest();
        readOnlyAndSiblingAgreementOnSeed();
        relationImaginaryEigenvaluesNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedOracleFromFailingTest() {
        final EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvalue] semantic mismatch: expected="
                        + REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            final RealVector actualEigenvector = decomposition.getEigenvector(i);
            final double dot = REF_EIGENVECTORS[i].dotProduct(actualEigenvector);
            final double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualEigenvector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualEigenvector).getNorm();
            }
            if (!(norm <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-eigenvector] semantic mismatch: expectedNormToReference=0.0 actualNorm="
                        + norm + " index=" + i + " dot=" + dot);
            }
        }
    }

    private static void readOnlyAndSiblingAgreementOnSeed() {
        final EigenDecompositionImpl dec =
            new EigenDecompositionImpl(TEST_MAIN, TEST_SECONDARY, MathUtils.SAFE_MIN);

        final double[] before = dec.getRealEigenvalues();
        final double[] beforeClone = before.clone();
        final double determinantBefore = dec.getDeterminant();

        final double[] after = dec.getRealEigenvalues();
        final double determinantAfter = dec.getDeterminant();

        /* Contract justification: get* methods are readers; repeated reads on the same decomposition
           should not mutate observable state. A patch that only suppresses/skirts the buggy branch can
           still leave corrupted work/pingPong state that later readers expose differently. */
        if (before.length != after.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-length] semantic mismatch: beforeLength=" + before.length
                    + " afterLength=" + after.length);
        }
        for (int i = 0; i < before.length; i++) {
            if (Double.doubleToLongBits(beforeClone[i]) != Double.doubleToLongBits(after[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-readonly-eigenvalues] semantic mismatch: index=" + i
                        + " before=" + beforeClone[i] + " after=" + after[i]);
            }
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-readonly-determinant] semantic mismatch: before=" + determinantBefore
                    + " after=" + determinantAfter);
        }

        /* Contract justification: getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers
           for the same decomposition and must agree on each index for any correct implementation. */
        for (int i = 0; i < before.length; i++) {
            final double scalar = dec.getRealEigenvalue(i);
            final double arrayValue = after[i];
            if (Double.doubleToLongBits(scalar) != Double.doubleToLongBits(arrayValue)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-sibling-real-eigenvalue] semantic mismatch: scalar="
                        + scalar + " array=" + arrayValue + " index=" + i);
            }
        }

        /* Contract justification: this symmetric-only implementation reports no imaginary eigenvalues;
           getImagEigenvalues() must therefore be null for a valid symmetric tridiagonal decomposition. */
        final double[] imag = dec.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-imag-null] semantic mismatch: expected=null actualLength=" + imag.length);
        }
    }

    private static void relationImaginaryEigenvaluesNull(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        final EigenDecompositionImpl dec;
        final double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] semantic mismatch: expected null but got array length "
                    + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        final int n = data.consumeInt(1, 8);
        final double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        final double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        final double[] rMain = new double[n];
        final double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        final EigenDecompositionImpl d1;
        final EigenDecompositionImpl d2;
        final double[] ev1;
        final double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reversed-count] semantic mismatch: different eigenvalue counts "
                    + ev1.length + " vs " + ev2.length);
        }

        final double[] a = ev1.clone();
        final double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            final double x = a[i];
            final double y = b[i];
            final double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reversed-multiset] semantic mismatch: index="
                        + i + " lhs=" + x + " rhs=" + y);
            }
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [111] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-eigenvalue] semantic mismatch: expected=16828.208208485466 actual=20654.74511575945 index=1

---
## [112] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,395 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!close(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eig-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i]
                );
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double norm;
            if (REF_EIGENVECTORS[i].dotProduct(actualVector) < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!close(0.0, norm, 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-vec-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" + norm
                );
            }
        }

        // Contract check from EigenDecomposition docs: A = V * D * V^T and D is diagonal for this implementation.
        // A patch that merely avoids the bad branch without preserving state would make these public readers disagree.
        checkReadersAgree((EigenDecompositionImpl) decomposition, "seed");

        int scaleInt = data.consumeInt(1, 8);
        double scale = scaleInt;
        try {
            double[] scaledMain = new double[MAIN.length];
            double[] scaledSecondary = new double[SECONDARY.length];
            for (int i = 0; i < MAIN.length; i++) {
                scaledMain[i] = MAIN[i] * scale;
            }
            for (int i = 0; i < SECONDARY.length; i++) {
                scaledSecondary[i] = SECONDARY[i] * scale;
            }

            EigenDecompositionImpl scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);

            double[] scaledEigenValues = scaled.getRealEigenvalues();
            for (int i = 0; i < REF_EIGENVALUES.length; i++) {
                double expected = REF_EIGENVALUES[i] * scale;
                if (!close(expected, scaledEigenValues[i], Math.max(1.0e-3 * scale, 1.0e-6))) {
                    throw new RuntimeException(
                        "[oracle:scaled-eig-" + i + "] metamorphic violation: positive scaling should scale eigenvalues inputScale=" +
                        scale + " expected=" + expected + " actual=" + scaledEigenValues[i]
                    );
                }

                RealVector seedVec = decomposition.getEigenvector(i);
                RealVector scaledVec = scaled.getEigenvector(i);
                double alignedNorm;
                if (seedVec.dotProduct(scaledVec) < 0) {
                    alignedNorm = seedVec.add(scaledVec).getNorm();
                } else {
                    alignedNorm = seedVec.subtract(scaledVec).getNorm();
                }
                if (!close(0.0, alignedNorm, 1.0e-5)) {
                    throw new RuntimeException(
                        "[oracle:scaled-vec-" + i + "] metamorphic violation: positive scaling should preserve eigenvectors up to sign inputScale=" +
                        scale + " norm=" + alignedNorm
                    );
                }
            }

            checkReadersAgree(scaled, "scaled-" + scaleInt);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkReadersAgree(EigenDecompositionImpl decomposition, String tag) {
        double[] eigenValues = decomposition.getRealEigenvalues();
        RealMatrix d = decomposition.getD();
        RealMatrix v = decomposition.getV();
        RealMatrix vt = decomposition.getVT();

        for (int i = 0; i < eigenValues.length; i++) {
            double byArray = eigenValues[i];
            double byScalar = decomposition.getRealEigenvalue(i);
            double byD = d.getEntry(i, i);

            if (!close(byArray, byScalar, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-eig-scalar-" + tag + "-" + i + "] metamorphic violation: getRealEigenvalues and getRealEigenvalue must agree index=" +
                    i + " lhs=" + byArray + " rhs=" + byScalar
                );
            }
            if (!close(byArray, byD, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-d-diag-" + tag + "-" + i + "] metamorphic violation: getD diagonal and eigenvalue readers must agree index=" +
                    i + " lhs=" + byArray + " rhs=" + byD
                );
            }

            for (int j = 0; j < eigenValues.length; j++) {
                if (i != j) {
                    double offDiag = d.getEntry(i, j);
                    if (!close(0.0, offDiag, 0.0)) {
                        throw new RuntimeException(
                            "[oracle:d-diagonal-" + tag + "-" + i + "-" + j + "] metamorphic violation: D must be diagonal row=" +
                            i + " col=" + j + " value=" + offDiag
                        );
                    }
                }

                double vtEntry = vt.getEntry(i, j);
                double transposed = v.getEntry(j, i);
                if (!close(vtEntry, transposed, 1.0e-12)) {
                    throw new RuntimeException(
                        "[oracle:v-vt-transpose-" + tag + "-" + i + "-" + j + "] metamorphic violation: getVT must equal transpose(getV) row=" +
                        i + " col=" + j + " lhs=" + vtEntry + " rhs=" + transposed
                    );
                }
            }
        }
    }

    private static boolean close(double expected, double actual, double tol) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return false;
        }
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [113] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 43 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eig-" + i + "] semantic mismatch: expected=" + REF_EIGENVAL

---
## [114] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,136 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!close(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eig-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i]
                );
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double norm;
            if (REF_EIGENVECTORS[i].dotProduct(actualVector) < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!close(0.0, norm, 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-vec-" + i + "] semantic mismatch: expectedNorm=0.0 actualNorm=" + norm
                );
            }
        }

        // Contract check from EigenDecomposition docs: A = V * D * V^T and D is diagonal for this implementation.
        // A patch that merely avoids the bad branch without preserving state would make these public readers disagree.
        checkReadersAgree((EigenDecompositionImpl) decomposition, "seed");

        int scaleInt = data.consumeInt(1, 8);
        double scale = scaleInt;
        try {
            double[] scaledMain = new double[MAIN.length];
            double[] scaledSecondary = new double[SECONDARY.length];
            for (int i = 0; i < MAIN.length; i++) {
                scaledMain[i] = MAIN[i] * scale;
            }
            for (int i = 0; i < SECONDARY.length; i++) {
                scaledSecondary[i] = SECONDARY[i] * scale;
            }

            EigenDecompositionImpl scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);

            double[] scaledEigenValues = scaled.getRealEigenvalues();
            for (int i = 0; i < REF_EIGENVALUES.length; i++) {
                double expected = REF_EIGENVALUES[i] * scale;
                if (!close(expected, scaledEigenValues[i], Math.max(1.0e-3 * scale, 1.0e-6))) {
                    throw new RuntimeException(
                        "[oracle:scaled-eig-" + i + "] metamorphic violation: positive scaling should scale eigenvalues inputScale=" +
                        scale + " expected=" + expected + " actual=" + scaledEigenValues[i]
                    );
                }

                RealVector seedVec = decomposition.getEigenvector(i);
                RealVector scaledVec = scaled.getEigenvector(i);
                double alignedNorm;
                if (seedVec.dotProduct(scaledVec) < 0) {
                    alignedNorm = seedVec.add(scaledVec).getNorm();
                } else {
                    alignedNorm = seedVec.subtract(scaledVec).getNorm();
                }
                if (!close(0.0, alignedNorm, 1.0e-5)) {
                    throw new RuntimeException(
                        "[oracle:scaled-vec-" + i + "] metamorphic violation: positive scaling should preserve eigenvectors up to sign inputScale=" +
                        scale + " norm=" + alignedNorm
                    );
                }
            }

            checkReadersAgree(scaled, "scaled-" + scaleInt);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkReadersAgree(EigenDecompositionImpl decomposition, String tag) {
        double[] eigenValues = decomposition.getRealEigenvalues();
        RealMatrix d = decomposition.getD();
        RealMatrix v = decomposition.getV();
        RealMatrix vt = decomposition.getVT();

        for (int i = 0; i < eigenValues.length; i++) {
            double byArray = eigenValues[i];
            double byScalar = decomposition.getRealEigenvalue(i);
            double byD = d.getEntry(i, i);

            if (!close(byArray, byScalar, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-eig-scalar-" + tag + "-" + i + "] metamorphic violation: getRealEigenvalues and getRealEigenvalue must agree index=" +
                    i + " lhs=" + byArray + " rhs=" + byScalar
                );
            }
            if (!close(byArray, byD, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-d-diag-" + tag + "-" + i + "] metamorphic violation: getD diagonal and eigenvalue readers must agree index=" +
                    i + " lhs=" + byArray + " rhs=" + byD
                );
            }

            for (int j = 0; j < eigenValues.length; j++) {
                if (i != j) {
                    double offDiag = d.getEntry(i, j);
                    if (!close(0.0, offDiag, 0.0)) {
                        throw new RuntimeException(
                            "[oracle:d-diagonal-" + tag + "-" + i + "-" + j + "] metamorphic violation: D must be diagonal row=" +
                            i + " col=" + j + " value=" + offDiag
                        );
                    }
                }

                double vtEntry = vt.getEntry(i, j);
                double transposed = v.getEntry(j, i);
                if (!close(vtEntry, transposed, 1.0e-12)) {
                    throw new RuntimeException(
                        "[oracle:v-vt-transpose-" + tag + "-" + i + "-" + j + "] metamorphic violation: getVT must equal transpose(getV) row=" +
                        i + " col=" + j + " lhs=" + vtEntry + " rhs=" + transposed
                    );
                }
            }
        }
    }

    private static boolean close(double expected, double actual, double tol) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return false;
        }
        return Math.abs(expected - actual) <= tol;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 43 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eig-" + i + "] semantic mismatch: expected=" + REF_EIGENVALUES[i] + " ...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~8,445 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(MAIN, SECONDARY, MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!close(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]
                );
            }

            RealVector actualVector = decomposition.getEigenvector(i);
            double norm;
            if (REF_EIGENVECTORS[i].dotProduct(actualVector) < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!close(0.0, norm, 1.0e-5)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + norm
                );
            }
        }

        // Documented contract: A = V D V^T, D is diagonal, and scalar/array readers
        // expose the same decomposition state. A patch that only suppresses the bad
        // path but leaves corrupted work/pingPong state can make these public readers disagree.
        checkReadersAgree((EigenDecompositionImpl) decomposition, "seed");

        int scaleInt = data.consumeInt(1, 8);
        double scale = scaleInt;
        try {
            double[] scaledMain = new double[MAIN.length];
            double[] scaledSecondary = new double[SECONDARY.length];
            for (int i = 0; i < MAIN.length; i++) {
                scaledMain[i] = MAIN[i] * scale;
            }
            for (int i = 0; i < SECONDARY.length; i++) {
                scaledSecondary[i] = SECONDARY[i] * scale;
            }

            EigenDecompositionImpl scaled =
                new EigenDecompositionImpl(scaledMain, scaledSecondary, MathUtils.SAFE_MIN);

            double[] scaledEigenValues = scaled.getRealEigenvalues();
            for (int i = 0; i < REF_EIGENVALUES.length; i++) {
                double expected = REF_EIGENVALUES[i] * scale;
                if (!close(expected, scaledEigenValues[i], Math.max(1.0e-3 * scale, 1.0e-6))) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigenvalues] metamorphic violation: positive scaling should scale eigenvalues index=" +
                        i + " inputScale=" + scale + " expected=" + expected + " actual=" + scaledEigenValues[i]
                    );
                }

                RealVector seedVec = decomposition.getEigenvector(i);
                RealVector scaledVec = scaled.getEigenvector(i);
                double alignedNorm;
                if (seedVec.dotProduct(scaledVec) < 0) {
                    alignedNorm = seedVec.add(scaledVec).getNorm();
                } else {
                    alignedNorm = seedVec.subtract(scaledVec).getNorm();
                }
                if (!close(0.0, alignedNorm, 1.0e-5)) {
                    throw new RuntimeException(
                        "[oracle:scaled-eigenvectors] metamorphic violation: positive scaling should preserve eigenvectors up to sign index=" +
                        i + " inputScale=" + scale + " norm=" + alignedNorm
                    );
                }
            }

            checkReadersAgree(scaled, "scaled-" + scaleInt);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkReadersAgree(EigenDecompositionImpl decomposition, String tag) {
        double[] eigenValues = decomposition.getRealEigenvalues();
        RealMatrix d = decomposition.getD();
        RealMatrix v = decomposition.getV();
        RealMatrix vt = decomposition.getVT();

        for (int i = 0; i < eigenValues.length; i++) {
            double byArray = eigenValues[i];
            double byScalar = decomposition.getRealEigenvalue(i);
            double byD = d.getEntry(i, i);

            if (!close(byArray, byScalar, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-array-vs-scalar] metamorphic violation: getRealEigenvalues and getRealEigenvalue must agree tag=" +
                    tag + " index=" + i + " lhs=" + byArray + " rhs=" + byScalar
                );
            }
            if (!close(byArray, byD, 0.0)) {
                throw new RuntimeException(
                    "[oracle:reader-array-vs-d] metamorphic violation: getD diagonal and eigenvalue readers must agree tag=" +
                    tag + " index=" + i + " lhs=" + byArray + " rhs=" + byD
                );
            }

            for (int j = 0; j < eigenValues.length; j++) {
                if (i != j) {
                    double offDiag = d.getEntry(i, j);
                    if (!close(0.0, offDiag, 0.0)) {
                        throw new RuntimeException(
                            "[oracle:d-must-be-diagonal] metamorphic violation: D must be diagonal tag=" +
                            tag + " row=" + i + " col=" + j + " value=" + offDiag
                        );
                    }
                }

                double vtEntry = vt.getEntry(i, j);
                double transposed = v.getEntry(j, i);
                if (!close(vtEntry, transposed, 1.0e-12)) {
                    throw new RuntimeException(
                        "[oracle:v-vt-transpose] metamorphic violation: getVT must equal transpose(getV) tag=" +
                        tag + " row=" + i + " col=" + j + " lhs=" + vtEntry + " rhs=" + transposed
                    );
                }
            }
        }
    }

    private static boolean close(double expected, double actual, double tol) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return false;
        }
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [115] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalues] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945

---
## [116] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,162 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617},
        {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014},
        {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096},
        {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196},
        {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344},
        {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-" + i
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
           Re-reading via public readers must report the same decomposition, not mutate or drift. */
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
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
## [117] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 59 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected=" + expected

---
## [118] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,903 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617},
        {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014},
        {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096},
        {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196},
        {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344},
        {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-" + i
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
           Re-reading via public readers must report the same decomposition, not mutate or drift. */
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
                    return false;
                }
            }
        }
        return true;
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 59 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actual...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~11,253 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617},
        {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014},
        {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096},
        {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196},
        {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344},
        {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-" + i
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
           Re-reading via public readers must report the same decomposition, not mutate or drift. */
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-random] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
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
## [119] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 59 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i + "] semantic mismatch: expected=" + expected

---
## [120] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~11,577 chars, 1 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [118] — not reprinted*
- *[user] message: identical to step [118] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617},
        {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014},
        {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096},
        {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196},
        {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344},
        {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-" + i
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvector-" + i
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
           Re-reading via public readers must report the same decomposition, not mutate or drift. */
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-random] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
                    return false;
                }
            }
        }
        return true;
    }
}
```
- *[user] message: identical to step [118] — not reprinted*

</details>
<details open><summary>▸ Output (~11,815 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final String[] EIGENVALUE_ORACLE_IDS = {
        "seed-eigenvalue-0",
        "seed-eigenvalue-1",
        "seed-eigenvalue-2",
        "seed-eigenvalue-3",
        "seed-eigenvalue-4",
        "seed-eigenvalue-5",
        "seed-eigenvalue-6"
    };

    private static final String[] EIGENVECTOR_ORACLE_IDS = {
        "seed-eigenvector-0",
        "seed-eigenvector-1",
        "seed-eigenvector-2",
        "seed-eigenvector-3",
        "seed-eigenvector-4",
        "seed-eigenvector-5",
        "seed-eigenvector-6"
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617},
        {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014},
        {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096},
        {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196},
        {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344},
        {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVALUE_ORACLE_IDS[i]
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVECTOR_ORACLE_IDS[i]
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
           Re-reading via public readers must report the same decomposition, not mutate or drift. */
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-random] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
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
## [121] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [122] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~17,052 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [118] — not reprinted*
- *[user] message: identical to step [118] — not reprinted*
- *[assistant] message: identical to step [120] — not reprinted*
- *[user] message: identical to step [118] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final String[] EIGENVALUE_ORACLE_IDS = {
        "seed-eigenvalue-0",
        "seed-eigenvalue-1",
        "seed-eigenvalue-2",
        "seed-eigenvalue-3",
        "seed-eigenvalue-4",
        "seed-eigenvalue-5",
        "seed-eigenvalue-6"
    };

    private static final String[] EIGENVECTOR_ORACLE_IDS = {
        "seed-eigenvector-0",
        "seed-eigenvector-1",
        "seed-eigenvector-2",
        "seed-eigenvector-3",
        "seed-eigenvector-4",
        "seed-eigenvector-5",
        "seed-eigenvector-6"
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617},
        {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014},
        {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096},
        {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196},
        {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344},
        {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVALUE_ORACLE_IDS[i]
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVECTOR_ORACLE_IDS[i]
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
           Re-reading via public readers must report the same decomposition, not mutate or drift. */
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-random] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
                    return false;
                }
            }
        }
        return true;
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:121: error: not a statement
        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
                                                               ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:121: error: ';' expected
        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
                                                                           ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:121: error: ';' expected
        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
                                                                                          ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:121: error: ';' expected
        /* Contract justification: these are documented get*/is* readers. A throw-deleting or bookkeeping-skipping
                                                                                                         ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: ';' expected
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
                ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: ';' expected
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
                       ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: ';' expected
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
                                    ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: ';' expected
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
                                                 ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: ';' expected
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
                                                                ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: ';' expected
           patch in the patched path may silently corrupt shared state (work/pingPong) while still returning once.
                                                                                      ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_022/FuzzHarness.java:122: error: '(' expected
           patch in the patched path may silently corrupt shared state (work/pingPong)
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
<details open><summary>▸ Output (~11,785 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final String[] EIGENVALUE_ORACLE_IDS = {
        "seed-eigenvalue-0",
        "seed-eigenvalue-1",
        "seed-eigenvalue-2",
        "seed-eigenvalue-3",
        "seed-eigenvalue-4",
        "seed-eigenvalue-5",
        "seed-eigenvalue-6"
    };

    private static final String[] EIGENVECTOR_ORACLE_IDS = {
        "seed-eigenvector-0",
        "seed-eigenvector-1",
        "seed-eigenvector-2",
        "seed-eigenvector-3",
        "seed-eigenvector-4",
        "seed-eigenvector-5",
        "seed-eigenvector-6"
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracles();
        invariantImaginaryArrayNullOnSeed();
        hiddenStateReaderStabilityOnSeed();
        relationImaginaryEigenvalueArrayIsNull(data);
        relationReversedTridiagonalPreservesEigenvalueMultiset(data);
    }

    private static void liftedSeedOracles() {
        EigenDecomposition decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] eigenValues = decomposition.getRealEigenvalues();
        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-eigenvalue-count] semantic mismatch: expected="
                + REF_EIGENVALUES.length + " actual=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (Math.abs(expected - actual) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVALUE_ORACLE_IDS[i]
                    + "] semantic mismatch: expected=" + expected + " actual=" + actual);
            }

            RealVector ref = new ArrayRealVector(REF_EIGENVECTORS[i]);
            RealVector got = decomposition.getEigenvector(i);
            double dot = ref.dotProduct(got);
            double norm = (dot < 0) ? ref.add(got).getNorm() : ref.subtract(got).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow("[oracle:" + EIGENVECTOR_ORACLE_IDS[i]
                    + "] semantic mismatch: norm=" + norm + " dot=" + dot);
            }
        }
    }

    private static void invariantImaginaryArrayNullOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-imag-null] semantic mismatch: expected null actualLength=" + imag.length);
        }
    }

    private static void hiddenStateReaderStabilityOnSeed() {
        EigenDecompositionImpl decomposition =
            new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeValues = decomposition.getRealEigenvalues();
        double determinantBefore = decomposition.getDeterminant();
        RealMatrix dBefore = decomposition.getD();
        RealMatrix vBefore = decomposition.getV();
        RealMatrix vtBefore = decomposition.getVT();
        double[] imagBefore = decomposition.getImagEigenvalues();

        double[] afterValues = decomposition.getRealEigenvalues();
        double determinantAfter = decomposition.getDeterminant();
        RealMatrix dAfter = decomposition.getD();
        RealMatrix vAfter = decomposition.getV();
        RealMatrix vtAfter = decomposition.getVT();
        double[] imagAfter = decomposition.getImagEigenvalues();

        // Contract justification: these are documented getter/is-reader methods.
        // A patch that merely avoids the failing path but corrupts shared internal state
        // would violate read-only stability; repeated reads must agree for the same object.
        if (!sameArray(beforeValues, afterValues, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-values] semantic mismatch: before="
                + Arrays.toString(beforeValues) + " after=" + Arrays.toString(afterValues));
        }
        if (Double.doubleToLongBits(determinantBefore) != Double.doubleToLongBits(determinantAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-determinant] semantic mismatch: before="
                + determinantBefore + " after=" + determinantAfter);
        }
        if (!matrixEquals(dBefore, dAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-d] semantic mismatch");
        }
        if (!matrixEquals(vBefore, vAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-v] semantic mismatch");
        }
        if (!matrixEquals(vtBefore, vtAfter, 0.0)) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-vt] semantic mismatch");
        }
        if (imagBefore != null || imagAfter != null) {
            throw new FuzzerSecurityIssueLow("[oracle:reader-stability-imag] semantic mismatch: before="
                + (imagBefore == null ? "null" : Arrays.toString(imagBefore)) + " after="
                + (imagAfter == null ? "null" : Arrays.toString(imagAfter)));
        }
    }

    private static void relationImaginaryEigenvalueArrayIsNull(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-random] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void relationReversedTridiagonalPreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-count] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts "
                + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("[oracle:reverse-multiset-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index "
                    + i + ": " + x + " vs " + y);
            }
        }
    }

    private static boolean sameArray(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) != Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean matrixEquals(RealMatrix a, RealMatrix b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getRowDimension() != b.getRowDimension() || a.getColumnDimension() != b.getColumnDimension()) {
            return false;
        }
        for (int i = 0; i < a.getRowDimension(); i++) {
            for (int j = 0; j < a.getColumnDimension(); j++) {
                double x = a.getEntry(i, j);
                double y = b.getEntry(i, j);
                if (Double.isNaN(x) != Double.isNaN(y)) {
                    return false;
                }
                if (Math.abs(x - y) > tol) {
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
## [123] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-eigenvalue-1] semantic mismatch: expected=16828.208208485466 actual=20654.74511575945

---
## [124] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,134 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 2);

        if (mode == 0) {
            runFixedOracle();
            return;
        }

        if (mode == 1) {
            runSignFlipGeneralization(data);
            return;
        }

        runPerturbedExercise(data);
    }

    private static void runFixedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (!withinTol(REF_EIGENVALUES[i], eigenValues[i], 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: index=" + i +
                    " expected=" + REF_EIGENVALUES[i] + " actual=" + eigenValues[i]);
            }

            final RealVector actualEigenvector;
            try {
                actualEigenvector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }

            final double dot;
            try {
                dot = REF_EIGENVECTORS[i].dotProduct(actualEigenvector);
            } catch (Throwable t) {
                return;
            }

            final double normDiff;
            try {
                if (dot < 0) {
                    normDiff = REF_EIGENVECTORS[i].add(actualEigenvector).getNorm();
                } else {
                    normDiff = REF_EIGENVECTORS[i].subtract(actualEigenvector).getNorm();
                }
            } catch (Throwable t) {
                return;
            }

            if (!withinTol(0.0, normDiff, 1.0e-5)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: index=" + i +
                    " expectedNorm=0.0 actualNorm=" + normDiff + " dot=" + dot);
            }
        }

        // Contract justification: getRealEigenvalues(), getRealEigenvalue(i), and getD()
        // are sibling readers over the same decomposition state; for a correct implementation,
        // the diagonal entries reported by getD() and the indexed getter must agree with the
        // array getter. A patch that merely avoids the bad flip path but leaves corrupted
        // internal work/state would break this observable agreement without throwing.
        try {
            RealMatrix d = decomposition.getD();
            double[] values = decomposition.getRealEigenvalues();
            for (int i = 0; i < values.length; i++) {
                double fromIndexed = decomposition.getRealEigenvalue(i);
                double fromDiagonal = d.getEntry(i, i);
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(fromIndexed)) {
                    throw new RuntimeException(
                        "[oracle:getters-agree] metamorphic violation: getRealEigenvalues vs getRealEigenvalue input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + fromIndexed);
                }
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(fromDiagonal)) {
                    throw new RuntimeException(
                        "[oracle:getters-agree] metamorphic violation: getRealEigenvalues vs getD diagonal input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + fromDiagonal);
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }

        // Contract justification: getVT() is the transpose view of V by the EigenDecomposition API.
        // This post-condition is directly observable and independent of the reference values.
        try {
            RealMatrix v = decomposition.getV();
            RealMatrix vt = decomposition.getVT();
            int n = v.getRowDimension();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    double lhs = v.getEntry(r, c);
                    double rhs = vt.getEntry(c, r);
                    if (!withinTol(lhs, rhs, 1.0e-12)) {
                        throw new RuntimeException(
                            "[oracle:v-vt-transpose] metamorphic violation: getVT must equal transpose(getV) input=(" +
                            r + "," + c + ") lhs=" + lhs + " rhs=" + rhs);
                    }
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runSignFlipGeneralization(FuzzedDataProvider data) {
        int idx = data.consumeInt(0, SECONDARY.length - 1);
        double[] flippedSecondary = SECONDARY.clone();
        flippedSecondary[idx] = -flippedSecondary[idx];

        final EigenDecomposition base;
        final EigenDecomposition flipped;
        try {
            base = new EigenDecompositionImpl(MAIN, SECONDARY, org.apache.commons.math.util.MathUtils.SAFE_MIN);
            flipped = new EigenDecompositionImpl(MAIN, flippedSecondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: for a symmetric tridiagonal matrix, changing the sign of one
        // off-diagonal entry is a similarity transform by a diagonal matrix of +/-1 signs, so the
        // eigenvalues must stay identical. Both sides are real library calls; the expected answer
        // is trusted by the algebraic equivalence of the two inputs.
        try {
            double[] a = base.getRealEigenvalues();
            double[] b = flipped.getRealEigenvalues();
            if (a.length != b.length) {
                throw new RuntimeException(
                    "[oracle:secondary-sign-flip] metamorphic violation: eigenvalue array lengths differ input=index=" +
                    idx + " lhs=" + a.length + " rhs=" + b.length);
            }
            for (int i = 0; i < a.length; i++) {
                if (!withinTol(a[i], b[i], 1.0e-8)) {
                    throw new RuntimeException(
                        "[oracle:secondary-sign-flip] metamorphic violation: sign flip of one secondary entry must preserve eigenvalues input=flipIndex=" +
                        idx + " pos=" + i + " lhs=" + a[i] + " rhs=" + b[i]);
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runPerturbedExercise(FuzzedDataProvider data) {
        double[] main = MAIN.clone();
        double[] secondary = SECONDARY.clone();

        int changes = data.consumeInt(1, 4);
        for (int i = 0; i < changes; i++) {
            int which = data.consumeBoolean() ? 0 : 1;
            int idx = which == 0 ? data.consumeInt(0, main.length - 1) : data.consumeInt(0, secondary.length - 1);
            int delta = data.consumeInt(-1000, 1000);
            double scaled = delta / 100.0;
            if (which == 0) {
                main[idx] += scaled;
            } else {
                secondary[idx] += scaled;
            }
        }

        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(main, secondary, org.apache.commons.math.util.MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        // Exercise the patched path on fuzzed but moderate inputs, and only assert trusted
        // sibling-agreement properties from the public API.
        try {
            double[] values = decomposition.getRealEigenvalues();
            RealMatrix d = decomposition.getD();
            for (int i = 0; i < values.length; i++) {
                double vi = decomposition.getRealEigenvalue(i);
                double di = d.getEntry(i, i);
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(vi)) {
                    throw new RuntimeException(
                        "[oracle:fuzzed-getters-agree] metamorphic violation: getRealEigenvalues vs getRealEigenvalue input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + vi);
                }
                if (Double.doubleToLongBits(values[i]) != Double.doubleToLongBits(di)) {
                    throw new RuntimeException(
                        "[oracle:fuzzed-getters-agree] metamorphic violation: getRealEigenvalues vs getD diagonal input=index=" +
                        i + " lhs=" + values[i] + " rhs=" + di);
                }
            }

            RealMatrix v = decomposition.getV();
            RealMatrix vt = decomposition.getVT();
            int n = v.getRowDimension();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    double lhs = v.getEntry(r, c);
                    double rhs = vt.getEntry(c, r);
                    if (!withinTol(lhs, rhs, 1.0e-10)) {
                        throw new RuntimeException(
                            "[oracle:fuzzed-v-vt-transpose] metamorphic violation: getVT must equal transpose(getV) input=(" +
                            r + "," + c + ") lhs=" + lhs + " rhs=" + rhs);
                    }
                }
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean withinTol(double expected, double actual, double tol) {
        return Math.abs(expected - actual) <= tol;
    }
}
```

</details>

---
## [125] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalues] semantic mismatch: index=1 expected=16828.208208485466 actual=20654.74511575945

---
## [126] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,223 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleAndStateChecks();
        runImaginaryNullRelation(data);
        runReversePreservesEigenvalueMultiset(data);
    }

    private static void runLiftedOracleAndStateChecks() {
        EigenDecomposition decomposition = new EigenDecompositionImpl(
            TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeReal = decomposition.getRealEigenvalues();
        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            ArrayRealVector refVec = new ArrayRealVector(REF_EIGENVECTORS[i], true);
            double dot = refVec.dotProduct(actualVec);
            double norm = (dot < 0) ? refVec.add(actualVec).getNorm() : refVec.subtract(actualVec).getNorm();
            if (Math.abs(norm - 0.0) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToBe=0.0 actualNorm=" +
                    norm + " dot=" + dot + " index=" + i);
            }
        }

        double[] afterReal = decomposition.getRealEigenvalues();
        if (beforeReal.length != afterReal.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-realEigenvalues] semantic mismatch: lengthBefore=" +
                beforeReal.length + " lengthAfter=" + afterReal.length);
        }
        for (int i = 0; i < beforeReal.length; i++) {
            if (Double.doubleToLongBits(beforeReal[i]) != Double.doubleToLongBits(afterReal[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-realEigenvalues] semantic mismatch: read-only getRealEigenvalues changed observable state at index=" +
                    i + " before=" + beforeReal[i] + " after=" + afterReal[i]);
            }
        }

        for (int i = 0; i < afterReal.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            if (Math.abs(scalar - afterReal[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(afterReal[i])))) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-realEigenvalue] semantic mismatch: getRealEigenvalue(" + i +
                    ")=" + scalar + " getRealEigenvalues()[" + i + "]=" + afterReal[i]);
            }
        }

        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:imag-null-seed] semantic mismatch: expected null imaginary eigenvalue array for symmetric input but got length=" +
                imag.length);
        }

        double determinant1 = decomposition.getDeterminant();
        double determinant2 = decomposition.getDeterminant();
        if (Double.doubleToLongBits(determinant1) != Double.doubleToLongBits(determinant2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: read-only getDeterminant changed observable result before=" +
                determinant1 + " after=" + determinant2);
        }

        /* Contract/metamorphic justification:
         * getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers over the same decomposition state,
         * so any correct implementation must report the same real eigenvalue through both APIs.
         * A patch that merely suppresses the buggy path or silently corrupts bookkeeping/work-array state can still
         * return an array while leaving per-index reads inconsistent; this observable agreement check catches that.
         */
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void runReversePreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }
}
```

</details>

---
## [127] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 52 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + REF_

---
## [128] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,964 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleAndStateChecks();
        runImaginaryNullRelation(data);
        runReversePreservesEigenvalueMultiset(data);
    }

    private static void runLiftedOracleAndStateChecks() {
        EigenDecomposition decomposition = new EigenDecompositionImpl(
            TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeReal = decomposition.getRealEigenvalues();
        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            ArrayRealVector refVec = new ArrayRealVector(REF_EIGENVECTORS[i], true);
            double dot = refVec.dotProduct(actualVec);
            double norm = (dot < 0) ? refVec.add(actualVec).getNorm() : refVec.subtract(actualVec).getNorm();
            if (Math.abs(norm - 0.0) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToBe=0.0 actualNorm=" +
                    norm + " dot=" + dot + " index=" + i);
            }
        }

        double[] afterReal = decomposition.getRealEigenvalues();
        if (beforeReal.length != afterReal.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-realEigenvalues] semantic mismatch: lengthBefore=" +
                beforeReal.length + " lengthAfter=" + afterReal.length);
        }
        for (int i = 0; i < beforeReal.length; i++) {
            if (Double.doubleToLongBits(beforeReal[i]) != Double.doubleToLongBits(afterReal[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-realEigenvalues] semantic mismatch: read-only getRealEigenvalues changed observable state at index=" +
                    i + " before=" + beforeReal[i] + " after=" + afterReal[i]);
            }
        }

        for (int i = 0; i < afterReal.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            if (Math.abs(scalar - afterReal[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(afterReal[i])))) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-realEigenvalue] semantic mismatch: getRealEigenvalue(" + i +
                    ")=" + scalar + " getRealEigenvalues()[" + i + "]=" + afterReal[i]);
            }
        }

        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:imag-null-seed] semantic mismatch: expected null imaginary eigenvalue array for symmetric input but got length=" +
                imag.length);
        }

        double determinant1 = decomposition.getDeterminant();
        double determinant2 = decomposition.getDeterminant();
        if (Double.doubleToLongBits(determinant1) != Double.doubleToLongBits(determinant2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: read-only getDeterminant changed observable result before=" +
                determinant1 + " after=" + determinant2);
        }

        /* Contract/metamorphic justification:
         * getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers over the same decomposition state,
         * so any correct implementation must report the same real eigenvalue through both APIs.
         * A patch that merely suppresses the buggy path or silently corrupts bookkeeping/work-array state can still
         * return an array while leaving per-index reads inconsistent; this observable agreement check catches that.
         */
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void runReversePreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 52 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + REF_EIGENVALUES...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~9,369 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleAndStateChecks();
        runImaginaryNullRelation(data);
        runReversePreservesEigenvalueMultiset(data);
    }

    private static void runLiftedOracleAndStateChecks() {
        EigenDecomposition decomposition = new EigenDecompositionImpl(
            TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeReal = decomposition.getRealEigenvalues();
        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            ArrayRealVector refVec = new ArrayRealVector(REF_EIGENVECTORS[i], true);
            double dot = refVec.dotProduct(actualVec);
            double norm = (dot < 0) ? refVec.add(actualVec).getNorm() : refVec.subtract(actualVec).getNorm();
            if (Math.abs(norm - 0.0) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToBe=0.0 actualNorm=" +
                    norm + " dot=" + dot + " index=" + i);
            }
        }

        double[] afterReal = decomposition.getRealEigenvalues();
        if (beforeReal.length != afterReal.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-realEigenvalues-length] semantic mismatch: lengthBefore=" +
                beforeReal.length + " lengthAfter=" + afterReal.length);
        }
        for (int i = 0; i < beforeReal.length; i++) {
            if (Double.doubleToLongBits(beforeReal[i]) != Double.doubleToLongBits(afterReal[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-realEigenvalues-content] semantic mismatch: read-only getRealEigenvalues changed observable state at index=" +
                    i + " before=" + beforeReal[i] + " after=" + afterReal[i]);
            }
        }

        for (int i = 0; i < afterReal.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            if (Math.abs(scalar - afterReal[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(afterReal[i])))) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-realEigenvalue-" + i + "] semantic mismatch: getRealEigenvalue(" + i +
                    ")=" + scalar + " getRealEigenvalues()[" + i + "]=" + afterReal[i]);
            }
        }

        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:imag-null-seed] semantic mismatch: expected null imaginary eigenvalue array for symmetric input but got length=" +
                imag.length);
        }

        double determinant1 = decomposition.getDeterminant();
        double determinant2 = decomposition.getDeterminant();
        if (Double.doubleToLongBits(determinant1) != Double.doubleToLongBits(determinant2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: read-only getDeterminant changed observable result before=" +
                determinant1 + " after=" + determinant2);
        }

        /* Contract/metamorphic justification:
         * getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers over the same decomposition state,
         * so any correct implementation must report the same real eigenvalue through both APIs.
         * A patch that merely suppresses the buggy path or silently corrupts bookkeeping/work-array state can still
         * return an array while leaving per-index reads inconsistent; this observable agreement check catches that.
         */
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void runReversePreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reverse-multiset-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reverse-multiset-value-" + i + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }
}
```

</details>

---
## [129] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 52 has no oracle ID (`throw new FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" + REF_

---
## [130] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~9,693 chars, 1 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [128] — not reprinted*
- *[user] message: identical to step [128] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleAndStateChecks();
        runImaginaryNullRelation(data);
        runReversePreservesEigenvalueMultiset(data);
    }

    private static void runLiftedOracleAndStateChecks() {
        EigenDecomposition decomposition = new EigenDecompositionImpl(
            TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeReal = decomposition.getRealEigenvalues();
        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues-" + i + "] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            ArrayRealVector refVec = new ArrayRealVector(REF_EIGENVECTORS[i], true);
            double dot = refVec.dotProduct(actualVec);
            double norm = (dot < 0) ? refVec.add(actualVec).getNorm() : refVec.subtract(actualVec).getNorm();
            if (Math.abs(norm - 0.0) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToBe=0.0 actualNorm=" +
                    norm + " dot=" + dot + " index=" + i);
            }
        }

        double[] afterReal = decomposition.getRealEigenvalues();
        if (beforeReal.length != afterReal.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-realEigenvalues-length] semantic mismatch: lengthBefore=" +
                beforeReal.length + " lengthAfter=" + afterReal.length);
        }
        for (int i = 0; i < beforeReal.length; i++) {
            if (Double.doubleToLongBits(beforeReal[i]) != Double.doubleToLongBits(afterReal[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-realEigenvalues-content] semantic mismatch: read-only getRealEigenvalues changed observable state at index=" +
                    i + " before=" + beforeReal[i] + " after=" + afterReal[i]);
            }
        }

        for (int i = 0; i < afterReal.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            if (Math.abs(scalar - afterReal[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(afterReal[i])))) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-realEigenvalue-" + i + "] semantic mismatch: getRealEigenvalue(" + i +
                    ")=" + scalar + " getRealEigenvalues()[" + i + "]=" + afterReal[i]);
            }
        }

        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:imag-null-seed] semantic mismatch: expected null imaginary eigenvalue array for symmetric input but got length=" +
                imag.length);
        }

        double determinant1 = decomposition.getDeterminant();
        double determinant2 = decomposition.getDeterminant();
        if (Double.doubleToLongBits(determinant1) != Double.doubleToLongBits(determinant2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: read-only getDeterminant changed observable result before=" +
                determinant1 + " after=" + determinant2);
        }

        /* Contract/metamorphic justification:
         * getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers over the same decomposition state,
         * so any correct implementation must report the same real eigenvalue through both APIs.
         * A patch that merely suppresses the buggy path or silently corrupts bookkeeping/work-array state can still
         * return an array while leaving per-index reads inconsistent; this observable agreement check catches that.
         */
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void runReversePreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reverse-multiset-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reverse-multiset-value-" + i + "] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }
}
```
- *[user] message: identical to step [128] — not reprinted*

</details>
<details open><summary>▸ Output (~9,285 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final double[][] REF_EIGENVECTORS = {
        {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321},
        {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617},
        {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014},
        {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096},
        {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196},
        {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344},
        {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958}
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleAndStateChecks();
        runImaginaryNullRelation(data);
        runReversePreservesEigenvalueMultiset(data);
    }

    private static void runLiftedOracleAndStateChecks() {
        EigenDecomposition decomposition = new EigenDecompositionImpl(
            TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);

        double[] beforeReal = decomposition.getRealEigenvalues();
        double[] eigenValues = decomposition.getRealEigenvalues();

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            if (Math.abs(REF_EIGENVALUES[i] - eigenValues[i]) > 1.0e-3) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: expected=" +
                    REF_EIGENVALUES[i] + " actual=" + eigenValues[i] + " index=" + i);
            }

            RealVector actualVec = decomposition.getEigenvector(i);
            ArrayRealVector refVec = new ArrayRealVector(REF_EIGENVECTORS[i], true);
            double dot = refVec.dotProduct(actualVec);
            double norm = (dot < 0) ? refVec.add(actualVec).getNorm() : refVec.subtract(actualVec).getNorm();
            if (Math.abs(norm) > 1.0e-5) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvectors] semantic mismatch: expectedNormToBe=0.0 actualNorm=" +
                    norm + " dot=" + dot + " index=" + i);
            }
        }

        double[] afterReal = decomposition.getRealEigenvalues();
        if (beforeReal.length != afterReal.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-real-length] semantic mismatch: lengthBefore=" +
                beforeReal.length + " lengthAfter=" + afterReal.length);
        }
        for (int i = 0; i < beforeReal.length; i++) {
            if (Double.doubleToLongBits(beforeReal[i]) != Double.doubleToLongBits(afterReal[i])) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-real-content] semantic mismatch: read-only getRealEigenvalues changed observable state at index=" +
                    i + " before=" + beforeReal[i] + " after=" + afterReal[i]);
            }
        }

        /* Contract/metamorphic justification:
         * getRealEigenvalue(i) and getRealEigenvalues()[i] are sibling readers over the same decomposition state,
         * so any correct implementation must report the same real eigenvalue through both APIs.
         * A patch that merely suppresses the buggy path or silently corrupts bookkeeping/work-array state can still
         * return an array while leaving per-index reads inconsistent; this observable agreement check catches that.
         */
        for (int i = 0; i < afterReal.length; i++) {
            double scalar = decomposition.getRealEigenvalue(i);
            if (Math.abs(scalar - afterReal[i]) > 1.0e-12 * Math.max(1.0, Math.max(Math.abs(scalar), Math.abs(afterReal[i])))) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-real-eigenvalue] semantic mismatch: getRealEigenvalue(" + i +
                    ")=" + scalar + " getRealEigenvalues()[" + i + "]=" + afterReal[i]);
            }
        }

        double[] imag = decomposition.getImagEigenvalues();
        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-imag-null] semantic mismatch: expected null imaginary eigenvalue array for symmetric input but got length=" +
                imag.length);
        }

        double determinant1 = decomposition.getDeterminant();
        double determinant2 = decomposition.getDeterminant();
        if (Double.doubleToLongBits(determinant1) != Double.doubleToLongBits(determinant2)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-determinant] semantic mismatch: read-only getDeterminant changed observable result before=" +
                determinant1 + " after=" + determinant2);
        }
    }

    private static void runImaginaryNullRelation(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-imag-null] relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " +
                imag.length);
        }
    }

    private static void runReversePreservesEigenvalueMultiset(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        double[] ev1;
        double[] ev2;
        try {
            EigenDecompositionImpl d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            EigenDecompositionImpl d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable e) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:relation-reverse-length] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " +
                ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:relation-reverse-values] relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " +
                    i + ": lhs=" + x + " rhs=" + y + " tol=" + tol);
            }
        }
    }
}
```

</details>

---
## [131] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalues] semantic mismatch: expected=16828.208208485466 actual=20654.74511575945 index=1

---
## [132] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,839 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(
                mainTridiagonal,
                secondaryTridiagonal,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
        } catch (Throwable t) {
            return;
        }

        double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            double actual = eigenValues[i];
            double expected = refEigenValues[i];
            if (!(Math.abs(expected - actual) <= 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001 index=" + i
                );
            }
            try {
                RealVector actualVector = decomposition.getEigenvector(i);
                double dot = refEigenVectors[i].dotProduct(actualVector);
                double norm;
                if (dot < 0) {
                    norm = refEigenVectors[i].add(actualVector).getNorm();
                } else {
                    norm = refEigenVectors[i].subtract(actualVector).getNorm();
                }
                if (!(Math.abs(norm) <= 1.0e-5)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: norm=" + norm + " tolerance=1.0E-5 index=" + i + " dot=" + dot
                    );
                }
            } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
                throw finding;
            } catch (Throwable t) {
                return;
            }
        }

        try {
            // Contract from EigenDecomposition API family: getRealEigenvalues() and getRealEigenvalue(i)
            // report the same decomposition result, and getD() is the diagonal eigenvalue matrix.
            // A patch that merely suppresses/avoids the buggy branch can still leave inconsistent state
            // in work/pingPong and thus make sibling readers disagree, so we assert they stay consistent.
            RealMatrix d = decomposition.getD();
            double[] all = decomposition.getRealEigenvalues();
            for (int i = 0; i < all.length; i++) {
                double one = decomposition.getRealEigenvalue(i);
                double diag = d.getEntry(i, i);
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(one)) {
                    throw new RuntimeException(
                        "[oracle:sibling-eigenvalue-reader-" + i + "] metamorphic violation: getRealEigenvalues()[i] != getRealEigenvalue(i) index=" + i + " lhs=" + all[i] + " rhs=" + one
                    );
                }
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(diag)) {
                    throw new RuntimeException(
                        "[oracle:diagonal-matrix-consistency-" + i + "] metamorphic violation: getD diagonal entry != real eigenvalue index=" + i + " lhs=" + all[i] + " rhs=" + diag
                    );
                }
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            return;
        }

        int n = data.consumeInt(2, 8);
        double[] fuzzMain = new double[n];
        double[] fuzzSecondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            fuzzMain[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        for (int i = 0; i < n - 1; i++) {
            double v = data.consumeInt(-1000000, 1000000) / 16.0;
            if (v == 0.0) {
                v = data.consumeBoolean() ? 1.0 : -1.0;
            }
            fuzzSecondary[i] = v;
        }
        try {
            EigenDecomposition fuzzDecomposition = new EigenDecompositionImpl(
                fuzzMain,
                fuzzSecondary,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
            fuzzDecomposition.getRealEigenvalues();
            fuzzDecomposition.getD();
            fuzzDecomposition.getV();
            fuzzDecomposition.getVT();
            if (n > 0) {
                fuzzDecomposition.getRealEigenvalue(0);
                fuzzDecomposition.getEigenvector(0);
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [133] ⚙️ harness-repair · `attempt_028`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': ['missing-alarm-id']}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [134] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 55 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalue-" + i + "] sema

---
## [135] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,580 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(
                mainTridiagonal,
                secondaryTridiagonal,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
        } catch (Throwable t) {
            return;
        }

        double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            double actual = eigenValues[i];
            double expected = refEigenValues[i];
            if (!(Math.abs(expected - actual) <= 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001 index=" + i
                );
            }
            try {
                RealVector actualVector = decomposition.getEigenvector(i);
                double dot = refEigenVectors[i].dotProduct(actualVector);
                double norm;
                if (dot < 0) {
                    norm = refEigenVectors[i].add(actualVector).getNorm();
                } else {
                    norm = refEigenVectors[i].subtract(actualVector).getNorm();
                }
                if (!(Math.abs(norm) <= 1.0e-5)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-eigenvector-" + i + "] semantic mismatch: norm=" + norm + " tolerance=1.0E-5 index=" + i + " dot=" + dot
                    );
                }
            } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
                throw finding;
            } catch (Throwable t) {
                return;
            }
        }

        try {
            // Contract from EigenDecomposition API family: getRealEigenvalues() and getRealEigenvalue(i)
            // report the same decomposition result, and getD() is the diagonal eigenvalue matrix.
            // A patch that merely suppresses/avoids the buggy branch can still leave inconsistent state
            // in work/pingPong and thus make sibling readers disagree, so we assert they stay consistent.
            RealMatrix d = decomposition.getD();
            double[] all = decomposition.getRealEigenvalues();
            for (int i = 0; i < all.length; i++) {
                double one = decomposition.getRealEigenvalue(i);
                double diag = d.getEntry(i, i);
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(one)) {
                    throw new RuntimeException(
                        "[oracle:sibling-eigenvalue-reader-" + i + "] metamorphic violation: getRealEigenvalues()[i] != getRealEigenvalue(i) index=" + i + " lhs=" + all[i] + " rhs=" + one
                    );
                }
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(diag)) {
                    throw new RuntimeException(
                        "[oracle:diagonal-matrix-consistency-" + i + "] metamorphic violation: getD diagonal entry != real eigenvalue index=" + i + " lhs=" + all[i] + " rhs=" + diag
                    );
                }
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            return;
        }

        int n = data.consumeInt(2, 8);
        double[] fuzzMain = new double[n];
        double[] fuzzSecondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            fuzzMain[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        for (int i = 0; i < n - 1; i++) {
            double v = data.consumeInt(-1000000, 1000000) / 16.0;
            if (v == 0.0) {
                v = data.consumeBoolean() ? 1.0 : -1.0;
            }
            fuzzSecondary[i] = v;
        }
        try {
            EigenDecomposition fuzzDecomposition = new EigenDecompositionImpl(
                fuzzMain,
                fuzzSecondary,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
            fuzzDecomposition.getRealEigenvalues();
            fuzzDecomposition.getD();
            fuzzDecomposition.getV();
            fuzzDecomposition.getVT();
            if (n > 0) {
                fuzzDecomposition.getRealEigenvalue(0);
                fuzzDecomposition.getEigenvector(0);
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 55 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-eigenvalue-" + i + "] semantic mismat...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,676 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] mainTridiagonal = {
            7484.860960227216, 18405.28129035345, 13855.225609560746,
            10016.708722343366, 559.8117399576674, 6750.190788301587,
            71.21428769782159
        };
        double[] secondaryTridiagonal = {
            -4175.088570476366, 1975.7955858241994, 5193.178422374075,
            1995.286659169179, 75.34535882933804, -234.0808002076056
        };

        double[] refEigenValues = {
            20654.744890306974412, 16828.208208485466457,
            6893.155912634994820, 6757.083016675340332,
            5887.799885688558788, 64.309089923240379,
            57.992628792736340
        };
        RealVector[] refEigenVectors = {
            new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
            new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
            new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
            new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
            new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
            new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
            new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
        };

        EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(
                mainTridiagonal,
                secondaryTridiagonal,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
        } catch (Throwable t) {
            return;
        }

        double[] eigenValues;
        try {
            eigenValues = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        for (int i = 0; i < refEigenValues.length; ++i) {
            double actual = eigenValues[i];
            double expected = refEigenValues[i];
            if (!(Math.abs(expected - actual) <= 1.0e-3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-eigenvalues] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001 index=" + i
                );
            }
            try {
                RealVector actualVector = decomposition.getEigenvector(i);
                double dot = refEigenVectors[i].dotProduct(actualVector);
                double norm;
                if (dot < 0) {
                    norm = refEigenVectors[i].add(actualVector).getNorm();
                } else {
                    norm = refEigenVectors[i].subtract(actualVector).getNorm();
                }
                if (!(Math.abs(norm) <= 1.0e-5)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-eigenvectors] semantic mismatch: norm=" + norm + " tolerance=1.0E-5 index=" + i + " dot=" + dot
                    );
                }
            } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
                throw finding;
            } catch (Throwable t) {
                return;
            }
        }

        try {
            // Documented family consistency: getRealEigenvalues(), getRealEigenvalue(i), and getD()
            // all expose the decomposition result. If a patch merely suppresses the buggy path or
            // leaves corrupted internal work/pingPong state, these sibling readers can disagree.
            RealMatrix d = decomposition.getD();
            double[] all = decomposition.getRealEigenvalues();
            for (int i = 0; i < all.length; i++) {
                double one = decomposition.getRealEigenvalue(i);
                double diag = d.getEntry(i, i);
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(one)) {
                    throw new RuntimeException(
                        "[oracle:sibling-reader-consistency] metamorphic violation: getRealEigenvalues()[i] != getRealEigenvalue(i) index=" + i + " lhs=" + all[i] + " rhs=" + one
                    );
                }
                if (Double.doubleToLongBits(all[i]) != Double.doubleToLongBits(diag)) {
                    throw new RuntimeException(
                        "[oracle:diagonal-consistency] metamorphic violation: getD diagonal entry != real eigenvalue index=" + i + " lhs=" + all[i] + " rhs=" + diag
                    );
                }
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            return;
        }

        int n = data.consumeInt(2, 8);
        double[] fuzzMain = new double[n];
        double[] fuzzSecondary = new double[n - 1];
        for (int i = 0; i < n; i++) {
            fuzzMain[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        for (int i = 0; i < n - 1; i++) {
            double v = data.consumeInt(-1000000, 1000000) / 16.0;
            if (v == 0.0) {
                v = data.consumeBoolean() ? 1.0 : -1.0;
            }
            fuzzSecondary[i] = v;
        }
        try {
            EigenDecomposition fuzzDecomposition = new EigenDecompositionImpl(
                fuzzMain,
                fuzzSecondary,
                org.apache.commons.math.util.MathUtils.SAFE_MIN
            );
            fuzzDecomposition.getRealEigenvalues();
            fuzzDecomposition.getD();
            fuzzDecomposition.getV();
            fuzzDecomposition.getVT();
            if (n > 0) {
                fuzzDecomposition.getRealEigenvalue(0);
                fuzzDecomposition.getEigenvector(0);
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [136] ⚙️ harness-repair · `attempt_029`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [137] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-eigenvalues] semantic mismatch: expected=16828.208208485466 actual=20654.74511575945 tolerance=0.001 index=1

---
## [138] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,332 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Arrays;
import org.apache.commons.math.util.MathUtils;

public class FuzzHarness {
    private static final double[] TEST_MAIN = {
        7484.860960227216, 18405.28129035345, 13855.225609560746,
        10016.708722343366, 559.8117399576674, 6750.190788301587,
        71.21428769782159
    };

    private static final double[] TEST_SECONDARY = {
        -4175.088570476366, 1975.7955858241994, 5193.178422374075,
        1995.286659169179, 75.34535882933804, -234.0808002076056
    };

    private static final double[] REF_EIGENVALUES = {
        20654.744890306974412, 16828.208208485466457,
        6893.155912634994820, 6757.083016675340332,
        5887.799885688558788, 64.309089923240379,
        57.992628792736340
    };

    private static final RealVector[] REF_EIGENVECTORS = {
        new ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),
        new ArrayRealVector(new double[] {0.179995273578326, -0.402807848153042, 0.701870993525734, 0.555058211014888, 0.068079148898236, 0.000509139115227, -0.000007112235617}),
        new ArrayRealVector(new double[] {-0.399582721284727, -0.056629954519333, -0.514406488522827, 0.711168164518580, 0.225548081276367, 0.125943999652923, -0.004321507456014}),
        new ArrayRealVector(new double[] {0.058515721572821, 0.010200130057739, 0.063516274916536, -0.090696087449378, -0.017148420432597, 0.991318870265707, -0.034707338554096}),
        new ArrayRealVector(new double[] {0.855205995537564, 0.327134656629775, -0.265382397060548, 0.282690729026706, 0.105736068025572, -0.009138126622039, 0.000367751821196}),
        new ArrayRealVector(new double[] {-0.002913069901144, -0.005177515777101, 0.041906334478672, -0.109315918416258, 0.436192305456741, 0.026307315639535, 0.891797507436344}),
        new ArrayRealVector(new double[] {-0.005738311176435, -0.010207611670378, 0.082662420517928, -0.215733886094368, 0.861606487840411, -0.025478530652759, -0.451080697503958})
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedOracle();
        runImaginaryInvariant(data);
        runReversalMetamorphic(data);
    }

    private static void runLiftedOracle() {
        final EigenDecomposition decomposition;
        try {
            decomposition = new EigenDecompositionImpl(TEST_MAIN.clone(), TEST_SECONDARY.clone(), MathUtils.SAFE_MIN);
        } catch (Throwable t) {
            return;
        }

        final double[] eigenValues;
        final double[] eigenValuesAgain;
        try {
            eigenValues = decomposition.getRealEigenvalues();
            eigenValuesAgain = decomposition.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (eigenValues.length != REF_EIGENVALUES.length) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count] semantic mismatch: expectedLength=" + REF_EIGENVALUES.length + " actualLength=" + eigenValues.length);
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            double expected = REF_EIGENVALUES[i];
            double actual = eigenValues[i];
            if (!(Math.abs(expected - actual) <= 1.0e-3)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expected + " actual=" + actual + " tolerance=0.001");
            }
        }

        for (int i = 0; i < REF_EIGENVALUES.length; ++i) {
            final RealVector actualVector;
            try {
                actualVector = decomposition.getEigenvector(i);
            } catch (Throwable t) {
                return;
            }
            double dot = REF_EIGENVECTORS[i].dotProduct(actualVector);
            double norm;
            if (dot < 0) {
                norm = REF_EIGENVECTORS[i].add(actualVector).getNorm();
            } else {
                norm = REF_EIGENVECTORS[i].subtract(actualVector).getNorm();
            }
            if (!(Math.abs(norm) <= 1.0e-5)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvector-" + i + "] semantic mismatch: expectedNormToBeZero actualNorm=" + norm + " signAdjustedByDot=" + dot);
            }
        }

        // Contract justification: getRealEigenvalues()/getRealEigenvalue(i) are readers over the same decomposition result;
        // a correct implementation must report the same eigenvalues through both public readers.
        for (int i = 0; i < eigenValues.length; i++) {
            final double scalar;
            try {
                scalar = decomposition.getRealEigenvalue(i);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(scalar) != Double.doubleToLongBits(eigenValues[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:sibling-real-eigenvalue-" + i + "] semantic mismatch: arrayValue=" + eigenValues[i] + " scalarValue=" + scalar);
            }
        }

        // Contract justification: getters are read-only; repeated getRealEigenvalues() calls should not mutate hidden state.
        if (eigenValuesAgain.length != eigenValues.length) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-length] semantic mismatch: firstLength=" + eigenValues.length + " secondLength=" + eigenValuesAgain.length);
        }
        for (int i = 0; i < eigenValues.length; i++) {
            if (Double.doubleToLongBits(eigenValues[i]) != Double.doubleToLongBits(eigenValuesAgain[i])) {
                throw new FuzzerSecurityIssueLow("[oracle:hidden-state-repeat-" + i + "] semantic mismatch: first=" + eigenValues[i] + " second=" + eigenValuesAgain[i]);
            }
        }

        // Contract justification: for this symmetric-only implementation imaginary eigenvalues are always null.
        final double[] imag;
        try {
            imag = decomposition.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }
        if (imag != null) {
            throw new FuzzerSecurityIssueLow("[oracle:imag-null-lifted] semantic mismatch: expectedNull actualLength=" + imag.length);
        }
    }

    private static void runImaginaryInvariant(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-200000, 200000) / 64.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-200000, 200000) / 64.0;
        }

        EigenDecompositionImpl dec;
        double[] imag;
        try {
            dec = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            imag = dec.getImagEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (imag != null) {
            throw new FuzzerSecurityIssueLow("relation imaginary-eigenvalue-array-is-null-for-symmetric-input violated: expected null but got array length " + imag.length);
        }
    }

    private static void runReversalMetamorphic(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 8);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }
        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = data.consumeInt(-1000000, 1000000) / 16.0;
        }

        double[] rMain = new double[n];
        double[] rSecondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n; i++) {
            rMain[i] = main[n - 1 - i];
        }
        for (int i = 0; i < rSecondary.length; i++) {
            rSecondary[i] = secondary[rSecondary.length - 1 - i];
        }

        EigenDecompositionImpl d1;
        EigenDecompositionImpl d2;
        double[] ev1;
        double[] ev2;
        try {
            d1 = new EigenDecompositionImpl(main, secondary, MathUtils.SAFE_MIN);
            d2 = new EigenDecompositionImpl(rMain, rSecondary, MathUtils.SAFE_MIN);
            ev1 = d1.getRealEigenvalues();
            ev2 = d2.getRealEigenvalues();
        } catch (Throwable t) {
            return;
        }

        if (ev1.length != ev2.length) {
            throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated: different eigenvalue counts " + ev1.length + " vs " + ev2.length);
        }

        double[] a = ev1.clone();
        double[] b = ev2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (!(Math.abs(x - y) <= tol)) {
                throw new FuzzerSecurityIssueLow("relation reversed-tridiagonal-preserves-eigenvalue-multiset violated at index " + i + ": " + x + " vs " + y);
            }
        }
    }
}
```

</details>

---
## [139] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 68 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-eigenvalue-" + i + "] semantic mismatch: expected=" + expect

---
## [140] ⚙️ coverage · `jazzer coverage dumps`
**output:** **12/12 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov; 12 raw fuzz log(s)**
<details><summary>▸ detail (~6,978 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 499}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 491}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 495}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 495}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 503}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 493}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 495}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 493}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 502}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 499}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 495}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 492}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_003', 'attempt_005', 'attempt_008', 'attempt_010', 'attempt_013', 'attempt_015', 'attempt_017', 'attempt_019', 'attempt_023', 'attempt_024', 'attempt_027', 'attempt_029'], 'accepted_attempts': ['attempt_003', 'attempt_005', 'attempt_008', 'attempt_010', 'attempt_013', 'attempt_015', 'attempt_017', 'attempt_019', 'attempt_023', 'attempt_024', 'attempt_027', 'attempt_029'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_029.java']}

</details>
