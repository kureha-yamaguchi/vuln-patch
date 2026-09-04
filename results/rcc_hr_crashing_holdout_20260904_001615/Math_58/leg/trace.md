# Pipeline trace — Math-58

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java	
+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java
@@ -617,7 +617,9 @@
             }
 
             // compute an improved estimate for lmPar
+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {
             lmPar = FastMath.max(parl, lmPar + correction);
+            }
 
         }
     }
```

19 sequential steps — 5 LLM calls, 14 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.optimization.fitting.GaussianFitterTest', test_metho (~2,032 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.optimization.fitting.GaussianFitterTest', test_method='testMath519', source_path='/tmp/d4j/Math_58_buggy/src/test/java/org/apache/commons/math/optimization/fitting/GaussianFitterTest.java', method_source='    @Test\\n    public void testMath519() {\\n        // The optimizer will try negative sigma values but \"GaussianFitter\"\\n        // will catch the raised exceptions and return NaN values instead.\\n\\n        final double[] data = { \\n            1.1143831578403364E-29,\\n            4.95281403484594E-28,\\n            1.1171347211930288E-26,\\n            1.7044813962636277E-25,\\n            1.9784716574832164E-24,\\n            1.8630236407866774E-23,\\n            1.4820532905097742E-22,\\n            1.0241963854632831E-21,\\n            6.275077366673128E-21,\\n            3.461808994532493E-20,\\n            1.7407124684715706E-19,\\n            8.056687953553974E-19,\\n            3.460193945992071E-18,\\n            1.3883326374011525E-17,\\n            5.233894983671116E-17,\\n            1.8630791465263745E-16,\\n            6.288759227922111E-16,\\n            2.0204433920597856E-15,\\n            6.198768938576155E-15,\\n            1.821419346860626E-14,\\n            5.139176445538471E-14,\\n            1.3956427429045787E-13,\\n            3.655705706448139E-13,\\n            9.253753324779779E-13,\\n            2.267636001476696E-12,\\n            5.3880460095836855E-12,\\n            1.2431632654852931E-11\\n        };\\n\\n        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());\\n        for (int i = 0; i < data.length; i++) {\\n            fitter.addObservedPoint(i, data[i]);\\n        }\\n        final double[] p = fitter.fit();\\n\\n        Assert.assertEquals(53.1572792, p[1], 1e-7);\\n        Assert.assertEquals(5.75214622, p[2], 1e-8);\\n    }', exception_type='org.apache.commons.math.exception.NotStrictlyPositiveException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~16,801 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java\t\n+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java\n@@ -617,7 +617,9 @@\n             }\n \n             // compute an improved estimate for lmPar\n+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {\n             lmPar = FastMath.max(parl, lmPar + correction);\n+            }\n \n         }\n     }\n",
  "functions": [
    {
      "func_name": "determineLMParameter",
      "func_signature": "private void determineLMParameter(double[] qy, double delta, double[] diag, double[] work1, double[] work2, double[] work3)",
      "func_source": "    private void determineLMParameter(double[] qy, double delta, double[] diag,\n            double[] work1, double[] work2, double[] work3) {\n\n        // compute and store in x the gauss-newton direction, if the\n        // jacobian is rank-deficient, obtain a least squares solution\n        for (int j = 0; j < rank; ++j) {\n            lmDir[permutation[j]] = qy[j];\n        }\n        for (int j = rank; j < cols; ++j) {\n            lmDir[permutation[j]] = 0;\n        }\n        for (int k = rank - 1; k >= 0; --k) {\n            int pk = permutation[k];\n            double ypk = lmDir[pk] / diagR[pk];\n            for (int i = 0; i < k; ++i) {\n                lmDir[permutation[i]] -= ypk * weightedResidualJacobian[i][pk];\n            }\n            lmDir[pk] = ypk;\n        }\n\n        // evaluate the function at the origin, and test\n        // for acceptance of the Gauss-Newton direction\n        double dxNorm = 0;\n        for (int j = 0; j < solvedCols; ++j) {\n            int pj = permutation[j];\n            double s = diag[pj] * lmDir[pj];\n            work1[pj] = s;\n            dxNorm += s * s;\n        }\n        dxNorm = FastMath.sqrt(dxNorm);\n        double fp = dxNorm - delta;\n        if (fp <= 0.1 * delta) {\n            lmPar = 0;\n            return;\n        }\n\n        // if the jacobian is not rank deficient, the Newton step provides\n        // a lower bound, parl, for the zero of the function,\n        // otherwise set this bound to zero\n        double sum2;\n        double parl = 0;\n        if (rank == solvedCols) {\n            for (int j = 0; j < solvedCols; ++j) {\n                int pj = permutation[j];\n                work1[pj] *= diag[pj] / dxNorm;\n            }\n            sum2 = 0;\n            for (int j = 0; j < solvedCols; ++j) {\n                int pj = permutation[j];\n                double sum = 0;\n                for (int i = 0; i < j; ++i) {\n                    sum += weightedResidualJacobian[i][pj] * work1[permutation[i]];\n                }\n                double s = (work1[pj] - sum) / diagR[pj];\n                work1[pj] = s;\n                sum2 += s * s;\n            }\n            parl = fp / (delta * sum2);\n        }\n\n        // calculate an upper bound, paru, for the zero of the function\n        sum2 = 0;\n        for (int j = 0; j < solvedCols; ++j) {\n            int pj = permutation[j];\n            double sum = 0;\n            for (int i = 0; i <= j; ++i) {\n                sum += weightedResidualJacobian[i][pj] * qy[i];\n            }\n            sum /= diag[pj];\n            sum2 += sum * sum;\n        }\n        double gNorm = FastMath.sqrt(sum2);\n        double paru = gNorm / delta;\n        if (paru == 0) {\n            // 2.2251e-308 is the smallest positive real for IEE754\n            paru = 2.2251e-308 / FastMath.min(delta, 0.1);\n        }\n\n        // if the input par lies outside of the interval (parl,paru),\n        // set par to the closer endpoint\n        lmPar = FastMath.min(paru, FastMath.max(lmPar, parl));\n        if (lmPar == 0) {\n            lmPar = gNorm / dxNorm;\n        }\n\n        for (int countdown = 10; countdown >= 0; --countdown) {\n\n            // evaluate the function at the current value of lmPar\n            if (lmPar == 0) {\n                lmPar = FastMath.max(2.2251e-308, 0.001 * paru);\n            }\n            double sPar = FastMath.sqrt(lmPar);\n            for (int j = 0; j < solvedCols; ++j) {\n                int pj = permutation[j];\n                work1[pj] = sPar * diag[pj];\n            }\n            determineLMDirection(qy, work1, work2, work3);\n\n            dxNorm = 0;\n            for (int j = 0; j < solvedCols; ++j) {\n                int pj = permutation[j];\n                double s = diag[pj] * lmDir[pj];\n                work3[pj] = s;\n                dxNorm += s * s;\n            }\n            dxNorm = FastMath.sqrt(dxNorm);\n            double previousFP = fp;\n            fp = dxNorm - delta;\n\n            // if the function is small enough, accept the current value\n            // of lmPar, also test for the exceptional cases where parl is zero\n            if ((FastMath.abs(fp) <= 0.1 * delta) ||\n                    ((parl == 0) && (fp <= previousFP) && (previousFP < 0))) {\n                return;\n            }\n\n            // compute the Newton correction\n            for (int j = 0; j < solvedCols; ++j) {\n                int pj = permutation[j];\n                work1[pj] = work3[pj] * diag[pj] / dxNorm;\n            }\n            for (int j = 0; j < solvedCols; ++j) {\n                int pj = permutation[j];\n                work1[pj] /= work2[j];\n                double tmp = work1[pj];\n                for (int i = j + 1; i < solvedCols; ++i) {\n                    work1[permutation[i]] -= weightedResidualJacobian[i][pj] * tmp;\n                }\n            }\n            sum2 = 0;\n            for (int j = 0; j < solvedCols; ++j) {\n                double s = work1[permutation[j]];\n                sum2 += s * s;\n            }\n            double correction = fp / (delta * sum2);\n\n            // depending on the sign of the function, update parl or paru.\n            if (fp > 0) {\n                parl = FastMath.max(parl, lmPar);\n            } else if (fp < 0) {\n                paru = FastMath.min(paru, lmPar);\n            }\n\n            // compute an improved estimate for lmPar\n            lmPar = FastMath.max(parl, lmPar + correction);\n\n        }\n    }",
      "func_class": "LevenbergMarquardtOptimizer",
      "func_class_fq": "org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer",
      "func_param_types": [
        "double[]",
        "double",
        "double[]",
        "double[]",
        "double[]",
        "double[]"
      ],
      "fi_name": "[org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer].determineLMParameter(double[],double,double[],double[],double[],double[])",
      "overload_types": [
        [
          "double[]",
          "double",
          "double[]",
          "double[]",
          "double[]",
          "double[]"
        ]
      ],
      "xrefs": [],
      "reachable": [
        "[org.apache.commons.math.util.FastMath].sqrt(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer)",
        "[org.apache.commons.math.util.FastMath].min(double,float)",
        "[org.apache.commons.math.util.FastMath].max(double,org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer)",
        "[org.apache.commons.math.util.FastMath].min(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer,int)",
        "[org.apache.commons.math.util.FastMath].max(float,float)",
        "[org.apache.commons.math.util.FastMath].sqrt(int)",
        "[org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer].determineLMDirection(double[],double[],double[],double[])",
        "[org.apache.commons.math.util.FastMath].abs(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer)",
        "[org.apache.commons.math.util.FastMath].max(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer,int)",
        "[org.apache.commons.math.util.FastMath].max(int,org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer)",
        "Float.floatToRawIntBits(float)",
        "[java.util.Arrays].fill(double[],int,org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer,int)",
        "[org.apache.commons.math.util.FastMath].abs()",
        "[org.apache.commons.math.util.FastMath].sqrt(float)"
      ],
      "related_callees": [
        {
          "name": "determineLMDirection",
          "source_file": "LevenbergMarquardtOptimizer.java",
          "signature": "private void determineLMDirection(double[] qy, double[] diag, double[] lmDiag, double[] work)",
          "source": "    private void determineLMDirection(double[] qy, double[] diag,\n            double[] lmDiag, double[] work) {\n\n        // copy R and Qty to preserve input and initialize s\n        //  in particular, save the diagonal elements of R in lmDir\n        for (int j = 0; j < solvedCols; ++j) {\n            int pj = permutation[j];\n            for (int i = j + 1; i < solvedCols; ++i) {\n                weightedResidualJacobian[i][pj] = weightedResidualJacobian[j][permutation[i]];\n            }\n            lmDir[j] = diagR[pj];\n            work[j]  = qy[j];\n        }\n\n        // eliminate the diagonal matrix d using a Givens rotation\n        for (int j = 0; j < solvedCols; ++j) {\n\n            // prepare the row of d to be eliminated, locating the\n            // diagonal element using p from the Q.R. factorization\n            int pj = permutation[j];\n            double dpj = diag[pj];\n            if (dpj != 0) {\n                Arrays.fill(lmDiag, j + 1, lmDiag.length, 0);\n            }\n            lmDiag[j] = dpj;\n\n            //  the transformations to eliminate the row of d\n            // modify only a single element of Qty\n            // beyond the first n, which is initially zero.\n            double qtbpj = 0;\n            for (int k = j; k < solvedCols; ++k) {\n                int pk = permutation[k];\n\n                // determine a Givens rotation which eliminates the\n                // appropriate element in the current row of d\n                if (lmDiag[k] != 0) {\n\n                    final double sin;\n                    final double cos;\n                    double rkk = weightedResidualJacobian[k][pk];\n                    if (FastMath.abs(rkk) < FastMath.abs(lmDiag[k])) {\n                        final double cotan = rkk / lmDiag[k];\n                        sin   = 1.0 / FastMath.sqrt(1.0 + cotan * cotan);\n                        cos   = sin * cotan;\n                    } else {\n                        final double tan = lmDiag[k] / rkk;\n                        cos = 1.0 / FastMath.sqrt(1.0 + tan * tan);\n                        sin = cos * tan;\n                    }\n\n                    // compute the modified diagonal element of R and\n                    // the modified element of (Qty,0)\n                    weightedResidualJacobian[k][pk] = cos * rkk + sin * lmDiag[k];\n                    final double temp = cos * work[k] + sin * qtbpj;\n                    qtbpj = -sin * work[k] + cos * qtbpj;\n                    work[k] = temp;\n\n                    // accumulate the tranformation in the row of s\n                    for (int i = k + 1; i < solvedCols; ++i) {\n                        double rik = weightedResidualJacobian[i][pk];\n                        final double temp2 = cos * rik + sin * lmDiag[i];\n                        lmDiag[i] = -sin * rik + cos * lmDiag[i];\n                        weightedResidualJacobian[i][pk] = temp2;\n                    }\n                }\n            }\n\n            // store the diagonal element of s and restore\n            // the corresponding diagonal element of R\n            lmDiag[j] = weightedResidualJacobian[j][permutation[j]];\n            weightedResidualJacobian[j][permutation[j]] = lmDir[j];\n        }\n\n        // solve the triangular system for z, if the system is\n        // singular, then obtain a least squares solution\n        int nSing = solvedCols;\n        for (int j = 0; j < solvedCols; ++j) {\n            if ((lmDiag[j] == 0) && (nSing == solvedCols)) {\n                nSing = j;\n            }\n            if (nSing < solvedCols) {\n                work[j] = 0;\n            }\n        }\n        if (nSing > 0) {\n            for (int j = nSing - 1; j >= 0; --j) {\n                int pj = permutation[j];\n                double sum = 0;\n                for (int i = j + 1; i < nSing; ++i) {\n                    sum += weightedResidualJacobian[i][pj] * work[i];\n                }\n                work[j] = (work[j] - sum) / lmDiag[j];\n            }\n        }\n\n        // permute the components of z back to components of lmDir\n        for (int j = 0; j < lmDir.length; ++j) {\n            lmDir[permutation[j]] = work[j];\n        }\n    }",
          "is_abstract": false,
          "impls": []
        }
      ],
      "field_siblings": [
        {
          "name": "doOptimize",
          "signature": "protected VectorialPointValuePair doOptimize() throws MathUserException",
          "shared_fields": [
            "diagR",
            "lmDir",
            "lmPar",
            "permutation",
            "solvedCols"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "{@inheritDoc}"
        },
        {
          "name": "qrDecomposition",
          "signature": "private void qrDecomposition() throws ConvergenceException",
          "shared_fields": [
            "diagR",
            "permutation",
            "rank",
            "solvedCols"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Decompose a matrix A as A.P = Q.R using Householder transforms. <p>As suggested in the P. Lascaux and R. Theodor book <i>Analyse num&eacute;rique matricielle appliqu&eacute;e &agrave; l'art de l'ing&eacute;nieur</i> (Masson, 1986), instead of representing the Householder transforms with u<sub>k</sub> unit vectors such …"
        },
        {
          "name": "AbstractLeastSquaresOptimizer.updateJacobian",
          "signature": "protected void updateJacobian()",
          "shared_fields": [
            "cols",
            "weightedResidualJacobian"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Update the jacobian matrix. @throws DimensionMismatchException if the Jacobian dimension does not match problem dimension. @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one."
        },
        {
          "name": "AbstractLeastSquaresOptimizer.getCovariances",
          "signature": "public double[][] getCovariances()",
          "shared_fields": [
            "cols",
            "weightedResidualJacobian"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Get the covariance matrix of the optimized parameters. @return the covariance matrix. @throws org.apache.commons.math.exception.SingularMatrixException if the covariance matrix cannot be computed (singular problem). @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one."
        },
        {
          "name": "AbstractLeastSquaresOptimizer.optimize",
          "signature": "public VectorialPointValuePair optimize(int maxEval, DifferentiableMultivariateVectorialFunction f, double[] target, double[] weights, double[] startPoint)",
          "shared_fields": [
            "cols",
            "weightedResidualJacobian"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "{@inheritDoc}"
        }
      ]
    }
  ],
  "package": "org.apache.commons.math.optimization.general",
  "root_cause_reachable": [
    "FastMath.sqrt",
    "FastMath.min",
    "FastMath.max",
    "LevenbergMarquardtOptimizer.determineLMDirection",
    "FastMath.abs(LevenbergMarquardtOptimizer)",
    "FastMath.abs()"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.util.Arrays;",
    "import org.apache.commons.math.exception.ConvergenceException;",
    "import org.apache.commons.math.exception.MathUserException;",
    "import org.apache.commons.math.exception.util.LocalizedFormats;",
    "import org.apache.commons.math.optimization.VectorialPointValuePair;",
    "import org.apache.commons.math.optimization.ConvergenceChecker;",
    "import org.apache.commons.math.util.MathUtils;",
    "import org.apache.commons.math.util.FastMath;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~33,850 chars)</summary>

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
- Package: `org.apache.commons.math.optimization.general` (`package org.apache.commons.math.optimization.general;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java	
+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java
@@ -617,7 +617,9 @@
             }
 
             // compute an improved estimate for lmPar
+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {
             lmPar = FastMath.max(parl, lmPar + correction);
+            }
 
         }
     }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.util.Arrays;
import org.apache.commons.math.exception.ConvergenceException;
import org.apache.commons.math.exception.MathUserException;
import org.apache.commons.math.exception.util.LocalizedFormats;
import org.apache.commons.math.optimization.VectorialPointValuePair;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.util.MathUtils;
import org.apache.commons.math.util.FastMath;
</source_imports>

Function `determineLMParameter`:
<signature>
private void determineLMParameter(double[] qy, double delta, double[] diag, double[] work1, double[] work2, double[] work3)
</signature>
<code>
    private void determineLMParameter(double[] qy, double delta, double[] diag,
            double[] work1, double[] work2, double[] work3) {

        // compute and store in x the gauss-newton direction, if the
        // jacobian is rank-deficient, obtain a least squares solution
        for (int j = 0; j < rank; ++j) {
            lmDir[permutation[j]] = qy[j];
        }
        for (int j = rank; j < cols; ++j) {
            lmDir[permutation[j]] = 0;
        }
        for (int k = rank - 1; k >= 0; --k) {
            int pk = permutation[k];
            double ypk = lmDir[pk] / diagR[pk];
            for (int i = 0; i < k; ++i) {
                lmDir[permutation[i]] -= ypk * weightedResidualJacobian[i][pk];
            }
            lmDir[pk] = ypk;
        }

        // evaluate the function at the origin, and test
        // for acceptance of the Gauss-Newton direction
        double dxNorm = 0;
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            double s = diag[pj] * lmDir[pj];
            work1[pj] = s;
            dxNorm += s * s;
        }
        dxNorm = FastMath.sqrt(dxNorm);
        double fp = dxNorm - delta;
        if (fp <= 0.1 * delta) {
            lmPar = 0;
            return;
        }

        // if the jacobian is not rank deficient, the Newton step provides
        // a lower bound, parl, for the zero of the function,
        // otherwise set this bound to zero
        double sum2;
        double parl = 0;
        if (rank == solvedCols) {
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] *= diag[pj] / dxNorm;
            }
            sum2 = 0;
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                double sum = 0;
                for (int i = 0; i < j; ++i) {
                    sum += weightedResidualJacobian[i][pj] * work1[permutation[i]];
                }
                double s = (work1[pj] - sum) / diagR[pj];
                work1[pj] = s;
                sum2 += s * s;
            }
            parl = fp / (delta * sum2);
        }

        // calculate an upper bound, paru, for the zero of the function
        sum2 = 0;
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            double sum = 0;
            for (int i = 0; i <= j; ++i) {
                sum += weightedResidualJacobian[i][pj] * qy[i];
            }
            sum /= diag[pj];
            sum2 += sum * sum;
        }
        double gNorm = FastMath.sqrt(sum2);
        double paru = gNorm / delta;
        if (paru == 0) {
            // 2.2251e-308 is the smallest positive real for IEE754
            paru = 2.2251e-308 / FastMath.min(delta, 0.1);
        }

        // if the input par lies outside of the interval (parl,paru),
        // set par to the closer endpoint
        lmPar = FastMath.min(paru, FastMath.max(lmPar, parl));
        if (lmPar == 0) {
            lmPar = gNorm / dxNorm;
        }

        for (int countdown = 10; countdown >= 0; --countdown) {

            // evaluate the function at the current value of lmPar
            if (lmPar == 0) {
                lmPar = FastMath.max(2.2251e-308, 0.001 * paru);
            }
            double sPar = FastMath.sqrt(lmPar);
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] = sPar * diag[pj];
            }
            determineLMDirection(qy, work1, work2, work3);

            dxNorm = 0;
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                double s = diag[pj] * lmDir[pj];
                work3[pj] = s;
                dxNorm += s * s;
            }
            dxNorm = FastMath.sqrt(dxNorm);
            double previousFP = fp;
            fp = dxNorm - delta;

            // if the function is small enough, accept the current value
            // of lmPar, also test for the exceptional cases where parl is zero
            if ((FastMath.abs(fp) <= 0.1 * delta) ||
                    ((parl == 0) && (fp <= previousFP) && (previousFP < 0))) {
                return;
            }

            // compute the Newton correction
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] = work3[pj] * diag[pj] / dxNorm;
            }
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] /= work2[j];
                double tmp = work1[pj];
                for (int i = j + 1; i < solvedCols; ++i) {
                    work1[permutation[i]] -= weightedResidualJacobian[i][pj] * tmp;
                }
            }
            sum2 = 0;
            for (int j = 0; j < solvedCols; ++j) {
                double s = work1[permutation[j]];
                sum2 += s * s;
            }
            double correction = fp / (delta * sum2);

            // depending on the sign of the function, update parl or paru.
            if (fp > 0) {
                parl = FastMath.max(parl, lmPar);
            } else if (fp < 0) {
                paru = FastMath.min(paru, lmPar);
            }

            // compute an improved estimate for lmPar
            lmPar = FastMath.max(parl, lmPar + correction);

        }
    }
</code>
Methods called by `determineLMParameter` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="determineLMDirection" from="LevenbergMarquardtOptimizer.java">
<signature>
private void determineLMDirection(double[] qy, double[] diag, double[] lmDiag, double[] work)
</signature>
<code>
    private void determineLMDirection(double[] qy, double[] diag,
            double[] lmDiag, double[] work) {

        // copy R and Qty to preserve input and initialize s
        //  in particular, save the diagonal elements of R in lmDir
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            for (int i = j + 1; i < solvedCols; ++i) {
                weightedResidualJacobian[i][pj] = weightedResidualJacobian[j][permutation[i]];
            }
            lmDir[j] = diagR[pj];
            work[j]  = qy[j];
        }

        // eliminate the diagonal matrix d using a Givens rotation
        for (int j = 0; j < solvedCols; ++j) {

            // prepare the row of d to be eliminated, locating the
            // diagonal element using p from the Q.R. factorization
            int pj = permutation[j];
            double dpj = diag[pj];
            if (dpj != 0) {
                Arrays.fill(lmDiag, j + 1, lmDiag.length, 0);
            }
            lmDiag[j] = dpj;

            //  the transformations to eliminate the row of d
            // modify only a single element of Qty
            // beyond the first n, which is initially zero.
            double qtbpj = 0;
            for (int k = j; k < solvedCols; ++k) {
                int pk = permutation[k];

                // determine a Givens rotation which eliminates the
                // appropriate element in the current row of d
                if (lmDiag[k] != 0) {

                    final double sin;
                    final double cos;
                    double rkk = weightedResidualJacobian[k][pk];
                    if (FastMath.abs(rkk) < FastMath.abs(lmDiag[k])) {
                        final double cotan = rkk / lmDiag[k];
                        sin   = 1.0 / FastMath.sqrt(1.0 + cotan * cotan);
                        cos   = sin * cotan;
                    } else {
                        final double tan = lmDiag[k] / rkk;
                        cos = 1.0 / FastMath.sqrt(1.0 + tan * tan);
                        sin = cos * tan;
                    }

                    // compute the modified diagonal element of R and
                    // the modified element of (Qty,0)
                    weightedResidualJacobian[k][pk] = cos * rkk + sin * lmDiag[k];
                    final double temp = cos * work[k] + sin * qtbpj;
                    qtbpj = -sin * work[k] + cos * qtbpj;
                    work[k] = temp;

                    // accumulate the tranformation in the row of s
                    for (int i = k + 1; i < solvedCols; ++i) {
                        double rik = weightedResidualJacobian[i][pk];
                        final double temp2 = cos * rik + sin * lmDiag[i];
                        lmDiag[i] = -sin * rik + cos * lmDiag[i];
                        weightedResidualJacobian[i][pk] = temp2;
                    }
                }
            }

            // store the diagonal element of s and restore
            // the corresponding diagonal element of R
            lmDiag[j] = weightedResidualJacobian[j][permutation[j]];
            weightedResidualJacobian[j][permutation[j]] = lmDir[j];
        }

        // solve the triangular system for z, if the system is
        // singular, then obtain a least squares solution
        int nSing = solvedCols;
        for (int j = 0; j < solvedCols; ++j) {
            if ((lmDiag[j] == 0) && (nSing == solvedCols)) {
                nSing = j;
            }
            if (nSing < solvedCols) {
                work[j] = 0;
            }
        }
        if (nSing > 0) {
            for (int j = nSing - 1; j >= 0; --j) {
                int pj = permutation[j];
                double sum = 0;
                for (int i = j + 1; i < nSing; ++i) {
                    sum += weightedResidualJacobian[i][pj] * work[i];
                }
                work[j] = (work[j] - sum) / lmDiag[j];
            }
        }

        // permute the components of z back to components of lmDir
        for (int j = 0; j < lmDir.length; ++j) {
            lmDir[permutation[j]] = work[j];
        }
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `determineLMParameter` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `determineLMParameter`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `protected VectorialPointValuePair doOptimize() throws MathUserException` (shared field(s): diagR, lmDir, lmPar, permutation, solvedCols)
      doc: {@inheritDoc}
  - `private void qrDecomposition() throws ConvergenceException` (shared field(s): diagR, permutation, rank, solvedCols)
      doc: Decompose a matrix A as A.P = Q.R using Householder transforms. <p>As suggested in the P. Lascaux and R. Theodor book <i>Analyse num&eacute;rique matricielle appliqu&eacute;e &agrave; l'art de l'ing&eacute;nieur</i> (Masson, 1986), instead of representing the Householder transforms with u<sub>k</sub> unit vectors such …
  - `protected void updateJacobian()` (shared field(s): cols, weightedResidualJacobian)
      doc: Update the jacobian matrix. @throws DimensionMismatchException if the Jacobian dimension does not match problem dimension. @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one.
  - `public double[][] getCovariances()` (shared field(s): cols, weightedResidualJacobian)
      doc: Get the covariance matrix of the optimized parameters. @return the covariance matrix. @throws org.apache.commons.math.exception.SingularMatrixException if the covariance matrix cannot be computed (singular problem). @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one.
  - `public VectorialPointValuePair optimize(int maxEval, DifferentiableMultivariateVectorialFunction f, double[] target, double[] weights, double[] startPoint)` (shared field(s): cols, weightedResidualJacobian)
      doc: {@inheritDoc}

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `GaussianFitter`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.exception.NotStrictlyPositiveException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `determineLMParameter` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- FastMath.sqrt
- FastMath.min
- FastMath.max
- LevenbergMarquardtOptimizer.determineLMDirection
- FastMath.abs(LevenbergMarquardtOptimizer)
- FastMath.abs()
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
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,686 chars)</summary>

```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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

        int n = data.consumeInt(10, 40);
        double amplitude = positiveFromInt(data.consumeInt(), 1.0e-12, 1.0e3);
        double sigma = positiveFromInt(data.consumeInt(), 0.5, 20.0);
        double offset = positiveFromInt(data.consumeInt(), 1.0, 40.0);
        double center = (n - 1) + offset;

        double[] y = new double[n];
        double prev = -1.0;
        for (int i = 0; i < n; i++) {
            double dx = i - center;
            double v = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (data.consumeBoolean()) {
                double bump = positiveFromInt(data.consumeInt(), 1.0, 1.000001);
                v *= bump;
            }
            if (v <= prev) {
                v = Math.nextUp(prev);
            }
            y[i] = v;
            prev = v;
        }

        double[] fitA;
        try {
            fitA = fit(y, false);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        double[] fitB;
        try {
            fitB = fit(y, true);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (fitA == null || fitB == null) {
            return;
        }
        if (fitA.length != fitB.length) {
            throw new RuntimeException("[oracle:gauss-fit-arity] metamorphic violation: same observations yielded different parameter counts lhs=" + fitA.length + " rhs=" + fitB.length);
        }

        /* Contract used for this oracle:
         * both fitters receive the same real observations and differ only in using
         * the two equivalent public APIs for unit weights:
         *   addObservedPoint(x, y)
         *   addObservedPoint(1.0, x, y)
         * For any correct implementation, these represent the same weighted problem,
         * so fitting must report the same parameters. A patch that only suppresses
         * the throw but corrupts optimizer state can make the two equivalent calls
         * disagree even when no exception is raised.
         */
        for (int i = 0; i < fitA.length; i++) {
            double a = fitA[i];
            double b = fitB[i];
            if (!close(a, b, 1.0e-7, 1.0e-7)) {
                throw new RuntimeException("[oracle:unit-weight-equiv] metamorphic violation: equivalent unit-weight APIs disagree param=" + i + " lhs=" + a + " rhs=" + b + " n=" + n + " center=" + center + " sigma=" + sigma + " amplitude=" + amplitude);
            }
        }
    }

    private static void runAnchor() {
        double[] p;
        try {
            p = fit(ANCHOR_DATA, false);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (p == null || p.length < 2) {
            throw new RuntimeException("[oracle:anchor-shape] metamorphic violation: anchor fit returned invalid parameter array");
        }

        /* Regression/post-condition from the project's failing test:
         * for this exact valid data set, GaussianFitter.fit() must recover a center
         * near 53.1572792. A throw-deleting or branch-skipping patch could avoid the
         * original exception yet silently return the wrong fit, which this check catches.
         */
        if (!close(p[1], 53.1572792, 1.0e-6, 1.0e-6)) {
            throw new RuntimeException("[oracle:anchor-center] metamorphic violation: anchor center mismatch got=" + p[1] + " expected=53.1572792");
        }

        double[] q;
        try {
            q = fit(ANCHOR_DATA, true);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (q == null || q.length != p.length) {
            throw new RuntimeException("[oracle:anchor-arity] metamorphic violation: anchor equivalent fits have different lengths");
        }
        for (int i = 0; i < p.length; i++) {
            if (!close(p[i], q[i], 1.0e-8, 1.0e-8)) {
                throw new RuntimeException("[oracle:anchor-unit-weight-equiv] metamorphic violation: anchor equivalent unit-weight APIs disagree param=" + i + " lhs=" + p[i] + " rhs=" + q[i]);
            }
        }
    }

    private static double[] fit(double[] y, boolean explicitUnitWeight) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            if (explicitUnitWeight) {
                fitter.addObservedPoint(1.0, (double) i, y[i]);
            } else {
                fitter.addObservedPoint((double) i, y[i]);
            }
        }
        return fitter.fit();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String m = t.getMessage();
        return m != null && m.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null || !t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer".equals(cls)
                    && ("determineLMParameter".equals(method) || "determineLMDirection".equals(method))) {
                return true;
            }
            if ("org.apache.commons.math.util.FastMath".equals(cls)
                    && ("sqrt".equals(method) || "min".equals(method) || "max".equals(method) || "abs".equals(method))) {
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
        return name.contains("MathIllegal") || name.contains("Convergence") || name.contains("DimensionMismatch")
                || name.contains("NoData") || name.contains("NullArgument") || name.contains("Singular");
    }

    private static boolean close(double a, double b, double absTol, double relTol) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        if (diff <= absTol) {
            return true;
        }
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= relTol * Math.max(1.0, scale);
    }

    private static double positiveFromInt(int raw, double min, double max) {
        long v = raw;
        if (v == Integer.MIN_VALUE) {
            v = 0;
        }
        double unit = (Math.abs(v) % 1_000_000L) / 999_999.0;
        return min + (max - min) * unit;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,619 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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

        int n = data.consumeInt(10, 40);
        double amplitude = positiveFromInt(data.consumeInt(), 1.0e-12, 1.0e3);
        double sigma = positiveFromInt(data.consumeInt(), 0.5, 20.0);
        double offset = positiveFromInt(data.consumeInt(), 1.0, 40.0);
        double center = (n - 1) + offset;

        double[] y = new double[n];
        double prev = -1.0;
        for (int i = 0; i < n; i++) {
            double dx = i - center;
            double v = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (data.consumeBoolean()) {
                double bump = positiveFromInt(data.consumeInt(), 1.0, 1.000001);
                v *= bump;
            }
            if (v <= prev) {
                v = Math.nextUp(prev);
            }
            y[i] = v;
            prev = v;
        }

        double[] fitA;
        try {
            fitA = fit(y, false);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        double[] fitB;
        try {
            fitB = fit(y, true);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (fitA == null || fitB == null) {
            return;
        }
        if (fitA.length != fitB.length) {
            throw new RuntimeException("[oracle:gauss-fit-arity] metamorphic violation: same observations yielded different parameter counts lhs=" + fitA.length + " rhs=" + fitB.length);
        }

        /* Contract used for this oracle:
         * both fitters receive the same real observations and differ only in using
         * the two equivalent public APIs for unit weights:
         *   addObservedPoint(x, y)
         *   addObservedPoint(1.0, x, y)
         * For any correct implementation, these represent the same weighted problem,
         * so fitting must report the same parameters. A patch that only suppresses
         * the throw but corrupts optimizer state can make the two equivalent calls
         * disagree even when no exception is raised.
         */
        for (int i = 0; i < fitA.length; i++) {
            double a = fitA[i];
            double b = fitB[i];
            if (!close(a, b, 1.0e-7, 1.0e-7)) {
                throw new RuntimeException("[oracle:unit-weight-equiv] metamorphic violation: equivalent unit-weight APIs disagree param=" + i + " lhs=" + a + " rhs=" + b + " n=" + n + " center=" + center + " sigma=" + sigma + " amplitude=" + amplitude);
            }
        }
    }

    private static void runAnchor() {
        double[] p;
        try {
            p = fit(ANCHOR_DATA, false);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (p == null || p.length < 2) {
            throw new RuntimeException("[oracle:anchor-shape] metamorphic violation: anchor fit returned invalid parameter array");
        }

        /* Regression/post-condition from the project's failing test:
         * for this exact valid data set, GaussianFitter.fit() must recover a center
         * near 53.1572792. A throw-deleting or branch-skipping patch could avoid the
         * original exception yet silently return the wrong fit, which this check catches.
         */
        if (!close(p[1], 53.1572792, 1.0e-6, 1.0e-6)) {
            throw new RuntimeException("[oracle:anchor-center] metamorphic violation: anchor center mismatch got=" + p[1] + " expected=53.1572792");
        }

        double[] q;
        try {
            q = fit(ANCHOR_DATA, true);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (q == null || q.length != p.length) {
            throw new RuntimeException("[oracle:anchor-arity] metamorphic violation: anchor equivalent fits have different lengths");
        }
        for (int i = 0; i < p.length; i++) {
            if (!close(p[i], q[i], 1.0e-8, 1.0e-8)) {
                throw new RuntimeException("[oracle:anchor-unit-weight-equiv] metamorphic violation: anchor equivalent unit-weight APIs disagree param=" + i + " lhs=" + p[i] + " rhs=" + q[i]);
            }
        }
    }

    private static double[] fit(double[] y, boolean explicitUnitWeight) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            if (explicitUnitWeight) {
                fitter.addObservedPoint(1.0, (double) i, y[i]);
            } else {
                fitter.addObservedPoint((double) i, y[i]);
            }
        }
        return fitter.fit();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String m = t.getMessage();
        return m != null && m.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null || !t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer".equals(cls)
                    && ("determineLMParameter".equals(method) || "determineLMDirection".equals(method))) {
                return true;
            }
            if ("org.apache.commons.math.util.FastMath".equals(cls)
                    && ("sqrt".equals(method) || "min".equals(method) || "max".equals(method) || "abs".equals(method))) {
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
        return name.contains("MathIllegal") || name.contains("Convergence") || name.contains("DimensionMismatch")
                || name.contains("NoData") || name.contains("NullArgument") || name.contains("Singular");
    }

    private static boolean close(double a, double b, double absTol, double relTol) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        if (diff <= absTol) {
            return true;
        }
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= relTol * Math.max(1.0, scale);
    }

    private static double positiveFromInt(int raw, double min, double max) {
        long v = raw;
        if (v == Integer.MIN_VALUE) {
            v = 0;
        }
        double unit = (Math.abs(v) % 1_000_000L) / 999_999.0;
        return min + (max - min) * unit;
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generated never drove execution into the changed code, or always took a path that does not fault.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java	
+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java
@@ -617,7 +617,9 @@
             }
 
             // compute an improved estimate for lmPar
+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {
             lmPar = FastMath.max(parl, lmPar + correction);
+            }
 
         }
     }


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
<details open><summary>▸ Output (~7,064 chars)</summary>

```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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
        runFit(ANCHOR_DATA);

        int mode = data.consumeInt(0, 5);
        double[] series = buildSeries(data, mode);
        runFit(series);
    }

    private static void runFit(double[] values) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < values.length; i++) {
            fitter.addObservedPoint(i, values[i]);
        }
        double[] p = fitter.fit();

        if (p == null || p.length != 3) {
            throw new RuntimeException("[oracle:param-count] expected 3 Gaussian parameters, got " + (p == null ? -1 : p.length));
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < values.length; i++) {
            fitter2.addObservedPoint(1.0, i, values[i]);
        }
        double[] q = fitter2.fit();

        if (q == null || q.length != 3) {
            throw new RuntimeException("[oracle:param-count-2] expected 3 Gaussian parameters, got " + (q == null ? -1 : q.length));
        }

        for (int i = 0; i < 3; i++) {
            if (!closeEnough(p[i], q[i])) {
                throw new RuntimeException("[oracle:unit-weight-equiv] equivalent GaussianFitter APIs disagree at index " + i + ": " + p[i] + " vs " + q[i]);
            }
        }
    }

    private static double[] buildSeries(FuzzedDataProvider data, int mode) {
        int n = data.consumeInt(27, 64);
        double[] y = new double[n];

        switch (mode) {
            case 0:
                return mutateAnchorPrefix(data, n);
            case 1:
                return monotoneTinyExponential(data, n);
            case 2:
                return monotoneTinyPolynomial(data, n);
            case 3:
                return risingTailOfGaussian(data, n);
            case 4:
                return nearlyAnchorScaled(data, n);
            default:
                for (int i = 0; i < n; i++) {
                    double base = Math.pow(10.0, -30.0 + (20.0 * i) / Math.max(1.0, n - 1.0));
                    double tweak = 1.0 + (data.consumeInt(-1000, 1000) / 1_000_000.0);
                    double v = base * tweak;
                    if (i > 0 && v <= y[i - 1]) {
                        v = Math.nextUp(y[i - 1]);
                    }
                    y[i] = v;
                }
                return y;
        }
    }

    private static double[] mutateAnchorPrefix(FuzzedDataProvider data, int n) {
        double[] y = new double[n];
        int copy = Math.min(ANCHOR_DATA.length, n);
        for (int i = 0; i < copy; i++) {
            double scale = 1.0 + (data.consumeInt(-50, 50) / 10000.0);
            y[i] = ANCHOR_DATA[i] * scale;
            if (i > 0 && y[i] <= y[i - 1]) {
                y[i] = Math.nextUp(y[i - 1]);
            }
        }
        for (int i = copy; i < n; i++) {
            double growth = 1.2 + data.consumeInt(0, 2000) / 1000.0;
            y[i] = Math.nextUp(y[i - 1] * growth);
        }
        return y;
    }

    private static double[] monotoneTinyExponential(FuzzedDataProvider data, int n) {
        double startExp = -40.0 + data.consumeInt(0, 20);
        double step = 0.2 + data.consumeInt(0, 3000) / 1000.0;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double v = Math.pow(10.0, startExp + step * i);
            if (i > 0 && v <= y[i - 1]) {
                v = Math.nextUp(y[i - 1]);
            }
            y[i] = v;
        }
        return y;
    }

    private static double[] monotoneTinyPolynomial(FuzzedDataProvider data, int n) {
        double scale = Math.pow(10.0, -35.0 + data.consumeInt(0, 10));
        int power = data.consumeInt(3, 8);
        double bias = 1.0 + data.consumeInt(0, 100);
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double x = i + bias;
            double v = scale * Math.pow(x, power);
            if (i > 0 && v <= y[i - 1]) {
                v = Math.nextUp(y[i - 1]);
            }
            y[i] = v;
        }
        return y;
    }

    private static double[] risingTailOfGaussian(FuzzedDataProvider data, int n) {
        double amplitude = Math.pow(10.0, -25.0 + data.consumeInt(0, 10));
        double sigma = 2.0 + data.consumeInt(0, 2000) / 100.0;
        double center = n + data.consumeInt(5, 80);
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double dx = i - center;
            double v = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (i > 0 && v <= y[i - 1]) {
                v = Math.nextUp(y[i - 1]);
            }
            y[i] = v;
        }
        return y;
    }

    private static double[] nearlyAnchorScaled(FuzzedDataProvider data, int n) {
        double[] y = new double[n];
        double scale = 1.0 + data.consumeInt(-500, 500) / 100000.0;
        int copy = Math.min(n, ANCHOR_DATA.length);
        for (int i = 0; i < copy; i++) {
            double local = 1.0 + data.consumeInt(-20, 20) / 100000.0;
            y[i] = ANCHOR_DATA[i] * scale * local;
            if (i > 0 && y[i] <= y[i - 1]) {
                y[i] = Math.nextUp(y[i - 1]);
            }
        }
        for (int i = copy; i < n; i++) {
            y[i] = Math.nextUp(y[i - 1] * (1.5 + data.consumeInt(0, 500) / 1000.0));
        }
        return y;
    }

    private static boolean closeEnough(double a, double b) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return diff <= 1e-7 * scale;
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
<details><summary>▸ Prompt (2 message(s), ~36,048 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.optimization.general` (`package org.apache.commons.math.optimization.general;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java	
+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java
@@ -617,7 +617,9 @@
             }
 
             // compute an improved estimate for lmPar
+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {
             lmPar = FastMath.max(parl, lmPar + correction);
+            }
 
         }
     }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.util.Arrays;
import org.apache.commons.math.exception.ConvergenceException;
import org.apache.commons.math.exception.MathUserException;
import org.apache.commons.math.exception.util.LocalizedFormats;
import org.apache.commons.math.optimization.VectorialPointValuePair;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.util.MathUtils;
import org.apache.commons.math.util.FastMath;
</source_imports>

Function `determineLMParameter`:
<signature>
private void determineLMParameter(double[] qy, double delta, double[] diag, double[] work1, double[] work2, double[] work3)
</signature>
<code>
    private void determineLMParameter(double[] qy, double delta, double[] diag,
            double[] work1, double[] work2, double[] work3) {

        // compute and store in x the gauss-newton direction, if the
        // jacobian is rank-deficient, obtain a least squares solution
        for (int j = 0; j < rank; ++j) {
            lmDir[permutation[j]] = qy[j];
        }
        for (int j = rank; j < cols; ++j) {
            lmDir[permutation[j]] = 0;
        }
        for (int k = rank - 1; k >= 0; --k) {
            int pk = permutation[k];
            double ypk = lmDir[pk] / diagR[pk];
            for (int i = 0; i < k; ++i) {
                lmDir[permutation[i]] -= ypk * weightedResidualJacobian[i][pk];
            }
            lmDir[pk] = ypk;
        }

        // evaluate the function at the origin, and test
        // for acceptance of the Gauss-Newton direction
        double dxNorm = 0;
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            double s = diag[pj] * lmDir[pj];
            work1[pj] = s;
            dxNorm += s * s;
        }
        dxNorm = FastMath.sqrt(dxNorm);
        double fp = dxNorm - delta;
        if (fp <= 0.1 * delta) {
            lmPar = 0;
            return;
        }

        // if the jacobian is not rank deficient, the Newton step provides
        // a lower bound, parl, for the zero of the function,
        // otherwise set this bound to zero
        double sum2;
        double parl = 0;
        if (rank == solvedCols) {
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] *= diag[pj] / dxNorm;
            }
            sum2 = 0;
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                double sum = 0;
                for (int i = 0; i < j; ++i) {
                    sum += weightedResidualJacobian[i][pj] * work1[permutation[i]];
                }
                double s = (work1[pj] - sum) / diagR[pj];
                work1[pj] = s;
                sum2 += s * s;
            }
            parl = fp / (delta * sum2);
        }

        // calculate an upper bound, paru, for the zero of the function
        sum2 = 0;
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            double sum = 0;
            for (int i = 0; i <= j; ++i) {
                sum += weightedResidualJacobian[i][pj] * qy[i];
            }
            sum /= diag[pj];
            sum2 += sum * sum;
        }
        double gNorm = FastMath.sqrt(sum2);
        double paru = gNorm / delta;
        if (paru == 0) {
            // 2.2251e-308 is the smallest positive real for IEE754
            paru = 2.2251e-308 / FastMath.min(delta, 0.1);
        }

        // if the input par lies outside of the interval (parl,paru),
        // set par to the closer endpoint
        lmPar = FastMath.min(paru, FastMath.max(lmPar, parl));
        if (lmPar == 0) {
            lmPar = gNorm / dxNorm;
        }

        for (int countdown = 10; countdown >= 0; --countdown) {

            // evaluate the function at the current value of lmPar
            if (lmPar == 0) {
                lmPar = FastMath.max(2.2251e-308, 0.001 * paru);
            }
            double sPar = FastMath.sqrt(lmPar);
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] = sPar * diag[pj];
            }
            determineLMDirection(qy, work1, work2, work3);

            dxNorm = 0;
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                double s = diag[pj] * lmDir[pj];
                work3[pj] = s;
                dxNorm += s * s;
            }
            dxNorm = FastMath.sqrt(dxNorm);
            double previousFP = fp;
            fp = dxNorm - delta;

            // if the function is small enough, accept the current value
            // of lmPar, also test for the exceptional cases where parl is zero
            if ((FastMath.abs(fp) <= 0.1 * delta) ||
                    ((parl == 0) && (fp <= previousFP) && (previousFP < 0))) {
                return;
            }

            // compute the Newton correction
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] = work3[pj] * diag[pj] / dxNorm;
            }
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] /= work2[j];
                double tmp = work1[pj];
                for (int i = j + 1; i < solvedCols; ++i) {
                    work1[permutation[i]] -= weightedResidualJacobian[i][pj] * tmp;
                }
            }
            sum2 = 0;
            for (int j = 0; j < solvedCols; ++j) {
                double s = work1[permutation[j]];
                sum2 += s * s;
            }
            double correction = fp / (delta * sum2);

            // depending on the sign of the function, update parl or paru.
            if (fp > 0) {
                parl = FastMath.max(parl, lmPar);
            } else if (fp < 0) {
                paru = FastMath.min(paru, lmPar);
            }

            // compute an improved estimate for lmPar
            lmPar = FastMath.max(parl, lmPar + correction);

        }
    }
</code>
Methods called by `determineLMParameter` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="determineLMDirection" from="LevenbergMarquardtOptimizer.java">
<signature>
private void determineLMDirection(double[] qy, double[] diag, double[] lmDiag, double[] work)
</signature>
<code>
    private void determineLMDirection(double[] qy, double[] diag,
            double[] lmDiag, double[] work) {

        // copy R and Qty to preserve input and initialize s
        //  in particular, save the diagonal elements of R in lmDir
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            for (int i = j + 1; i < solvedCols; ++i) {
                weightedResidualJacobian[i][pj] = weightedResidualJacobian[j][permutation[i]];
            }
            lmDir[j] = diagR[pj];
            work[j]  = qy[j];
        }

        // eliminate the diagonal matrix d using a Givens rotation
        for (int j = 0; j < solvedCols; ++j) {

            // prepare the row of d to be eliminated, locating the
            // diagonal element using p from the Q.R. factorization
            int pj = permutation[j];
            double dpj = diag[pj];
            if (dpj != 0) {
                Arrays.fill(lmDiag, j + 1, lmDiag.length, 0);
            }
            lmDiag[j] = dpj;

            //  the transformations to eliminate the row of d
            // modify only a single element of Qty
            // beyond the first n, which is initially zero.
            double qtbpj = 0;
            for (int k = j; k < solvedCols; ++k) {
                int pk = permutation[k];

                // determine a Givens rotation which eliminates the
                // appropriate element in the current row of d
                if (lmDiag[k] != 0) {

                    final double sin;
                    final double cos;
                    double rkk = weightedResidualJacobian[k][pk];
                    if (FastMath.abs(rkk) < FastMath.abs(lmDiag[k])) {
                        final double cotan = rkk / lmDiag[k];
                        sin   = 1.0 / FastMath.sqrt(1.0 + cotan * cotan);
                        cos   = sin * cotan;
                    } else {
                        final double tan = lmDiag[k] / rkk;
                        cos = 1.0 / FastMath.sqrt(1.0 + tan * tan);
                        sin = cos * tan;
                    }

                    // compute the modified diagonal element of R and
                    // the modified element of (Qty,0)
                    weightedResidualJacobian[k][pk] = cos * rkk + sin * lmDiag[k];
                    final double temp = cos * work[k] + sin * qtbpj;
                    qtbpj = -sin * work[k] + cos * qtbpj;
                    work[k] = temp;

                    // accumulate the tranformation in the row of s
                    for (int i = k + 1; i < solvedCols; ++i) {
                        double rik = weightedResidualJacobian[i][pk];
                        final double temp2 = cos * rik + sin * lmDiag[i];
                        lmDiag[i] = -sin * rik + cos * lmDiag[i];
                        weightedResidualJacobian[i][pk] = temp2;
                    }
                }
            }

            // store the diagonal element of s and restore
            // the corresponding diagonal element of R
            lmDiag[j] = weightedResidualJacobian[j][permutation[j]];
            weightedResidualJacobian[j][permutation[j]] = lmDir[j];
        }

        // solve the triangular system for z, if the system is
        // singular, then obtain a least squares solution
        int nSing = solvedCols;
        for (int j = 0; j < solvedCols; ++j) {
            if ((lmDiag[j] == 0) && (nSing == solvedCols)) {
                nSing = j;
            }
            if (nSing < solvedCols) {
                work[j] = 0;
            }
        }
        if (nSing > 0) {
            for (int j = nSing - 1; j >= 0; --j) {
                int pj = permutation[j];
                double sum = 0;
                for (int i = j + 1; i < nSing; ++i) {
                    sum += weightedResidualJacobian[i][pj] * work[i];
                }
                work[j] = (work[j] - sum) / lmDiag[j];
            }
        }

        // permute the components of z back to components of lmDir
        for (int j = 0; j < lmDir.length; ++j) {
            lmDir[permutation[j]] = work[j];
        }
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `determineLMParameter` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `determineLMParameter`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `protected VectorialPointValuePair doOptimize() throws MathUserException` (shared field(s): diagR, lmDir, lmPar, permutation, solvedCols)
      doc: {@inheritDoc}
  - `private void qrDecomposition() throws ConvergenceException` (shared field(s): diagR, permutation, rank, solvedCols)
      doc: Decompose a matrix A as A.P = Q.R using Householder transforms. <p>As suggested in the P. Lascaux and R. Theodor book <i>Analyse num&eacute;rique matricielle appliqu&eacute;e &agrave; l'art de l'ing&eacute;nieur</i> (Masson, 1986), instead of representing the Householder transforms with u<sub>k</sub> unit vectors such …
  - `protected void updateJacobian()` (shared field(s): cols, weightedResidualJacobian)
      doc: Update the jacobian matrix. @throws DimensionMismatchException if the Jacobian dimension does not match problem dimension. @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one.
  - `public double[][] getCovariances()` (shared field(s): cols, weightedResidualJacobian)
      doc: Get the covariance matrix of the optimized parameters. @return the covariance matrix. @throws org.apache.commons.math.exception.SingularMatrixException if the covariance matrix cannot be computed (singular problem). @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one.
  - `public VectorialPointValuePair optimize(int maxEval, DifferentiableMultivariateVectorialFunction f, double[] target, double[] weights, double[] startPoint)` (shared field(s): cols, weightedResidualJacobian)
      doc: {@inheritDoc}

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `GaussianFitter`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.exception.NotStrictlyPositiveException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `determineLMParameter` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- FastMath.sqrt
- FastMath.min
- FastMath.max
- LevenbergMarquardtOptimizer.determineLMDirection
- FastMath.abs(LevenbergMarquardtOptimizer)
- FastMath.abs()
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters
- org.apache.commons.math.analysis.function.Gaussian$Parametric.value
- org.apache.commons.math.optimization.direct.BaseAbstractVectorialOptimizer.computeObjectiveValue
- org.apache.commons.math.optimization.direct.BaseAbstractVectorialOptimizer.optimize
- org.apache.commons.math.optimization.fitting.CurveFitter$TheoreticalValuesFunction.value
- org.apache.commons.math.optimization.fitting.CurveFitter.fit
- org.apache.commons.math.optimization.fitting.GaussianFitter.fit
- org.apache.commons.math.optimization.general.AbstractLeastSquaresOptimizer.optimize
- org.apache.commons.math.optimization.general.AbstractLeastSquaresOptimizer.updateResidualsAndCost
- org.apache.commons.math.optimization.general.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.optimization.general.FuzzHarness.runFit
- org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.doOptimize
Crashes already found:
- INFO@org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- FastMath.sqrt
- FastMath.min
- FastMath.max
- LevenbergMarquardtOptimizer.determineLMDirection
- FastMath.abs(LevenbergMarquardtOptimizer)
- FastMath.abs()
Check FAMILIES already covered by accepted harnesses: {param-count, unit-weight-equiv}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): INFO@org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~10,155 chars)</summary>

```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        LevenbergMarquardtOptimizer opt = new LevenbergMarquardtOptimizer();
        try {
            double[] fit = runFit(opt, buildX(ANCHOR_Y.length, 0.0, 1.0), ANCHOR_Y);

            if (fit == null || fit.length < 3) {
                throw new RuntimeException("[oracle:anchor-shape] metamorphic violation: fit returned invalid parameter array");
            }

            if (Math.abs(fit[1] - 53.1572792) > 1.0e-5) {
                throw new RuntimeException("[oracle:anchor-center] metamorphic violation: exact regression input should recover documented center input=Math519 lhs=" + fit[1] + " rhs=53.1572792");
            }

            double[] fit2 = runFit(new LevenbergMarquardtOptimizer(), buildX(ANCHOR_Y.length, 0.0, 1.0), ANCHOR_Y);
            assertClose("repeat-center", fit[1], fit2[1], 1.0e-10);
            assertClose("repeat-sigma", fit[2], fit2[2], 1.0e-10);

            double scale = 7.0;
            double[] scaledY = scaleY(ANCHOR_Y, scale);
            double[] scaledFit = runFit(new LevenbergMarquardtOptimizer(), buildX(ANCHOR_Y.length, 0.0, 1.0), scaledY);

            /* Contract/oracle:
             * GaussianFitter fits a parametric Gaussian y = norm * exp(-(x-mean)^2 / (2*sigma^2)).
             * Multiplying every observed y by a positive constant yields an equivalent least-squares
             * problem where only "norm" scales; mean and sigma must stay the same for any correct implementation.
             * A throw-deleting or branch-skipping patch in LM parameter updates can silently return wrong mean/sigma.
             */
            assertRelativeClose("scale-center", fit[1], scaledFit[1], 1.0e-6);
            assertRelativeClose("scale-sigma", fit[2], scaledFit[2], 1.0e-6);
            assertRelativeClose("scale-norm", fit[0] * scale, scaledFit[0], 1.0e-5);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(8, 27);
        int offset = data.consumeInt(0, ANCHOR_Y.length - len);
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double base = ANCHOR_Y[offset + i];
            int pct = data.consumeInt(50, 150);
            y[i] = base * (pct / 100.0);
            if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                y[i] = base;
            }
        }

        double startX = data.consumeInt(-50, 50);
        double step = data.consumeInt(1, 5);
        double[] x = buildX(len, startX, step);

        double yScale = data.consumeInt(1, 20);
        double[] scaledY = scaleY(y, yScale);

        double orthoBase;
        switch (data.consumeInt(0, 5)) {
            case 0:
                orthoBase = 0.0;
                break;
            case 1:
                orthoBase = 1.0e-20;
                break;
            case 2:
                orthoBase = 1.0e-14;
                break;
            case 3:
                orthoBase = 1.0e-10;
                break;
            case 4:
                orthoBase = 1.0e-6;
                break;
            default:
                orthoBase = Math.abs(data.consumeInt(-1000, 1000)) * 1.0e-8;
                break;
        }

        double tweak = 1.0 + (data.consumeInt(-100, 100) * 1.0e-4);
        double orthoTolerance = Math.abs(orthoBase * tweak);
        if (Double.isNaN(orthoTolerance) || Double.isInfinite(orthoTolerance)) {
            orthoTolerance = 1.0e-10;
        }

        LevenbergMarquardtOptimizer opt1 = new LevenbergMarquardtOptimizer(
                100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);
        LevenbergMarquardtOptimizer opt2 = new LevenbergMarquardtOptimizer(
                100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);
        LevenbergMarquardtOptimizer opt3 = new LevenbergMarquardtOptimizer(
                100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);

        try {
            double[] fit1 = runFit(opt1, x, y);
            double[] fit2 = runFit(opt2, x, y);

            assertRelativeClose("det-center", fit1[1], fit2[1], 1.0e-8);
            assertRelativeClose("det-sigma", fit1[2], fit2[2], 1.0e-8);

            double[] fitScaled = runFit(opt3, x, scaledY);
            assertRelativeClose("fuzz-scale-center", fit1[1], fitScaled[1], 2.0e-5);
            assertRelativeClose("fuzz-scale-sigma", fit1[2], fitScaled[2], 2.0e-5);
            assertRelativeClose("fuzz-scale-norm", fit1[0] * yScale, fitScaled[0], 1.0e-4);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static double[] runFit(LevenbergMarquardtOptimizer optimizer, double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(optimizer);
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter.fit();
    }

    private static double[] buildX(int len, double start, double step) {
        double[] x = new double[len];
        for (int i = 0; i < len; i++) {
            x[i] = start + step * i;
        }
        return x;
    }

    private static double[] scaleY(double[] y, double scale) {
        double[] out = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            out[i] = y[i] * scale;
        }
        return out;
    }

    private static void assertClose(String id, double lhs, double rhs, double tol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)
                || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
        }
    }

    private static void assertRelativeClose(String id, double lhs, double rhs, double relTol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: non-finite lhs=" + lhs + " rhs=" + rhs);
        }
        double scale = Math.max(1.0, Math.max(Math.abs(lhs), Math.abs(rhs)));
        if (Math.abs(lhs - rhs) > relTol * scale) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: lhs=" + lhs + " rhs=" + rhs + " relTol=" + relTol);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }

        if (t instanceof IllegalArgumentException) {
            return;
        }
        if (t instanceof NumberFormatException) {
            return;
        }
        if (isValidationFamily(t)) {
            return;
        }
    }

    private static boolean isValidationFamily(Throwable t) {
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            if (n.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }

        boolean classMatches = false;
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            if ("org.apache.commons.math.exception.NotStrictlyPositiveException".equals(n)
                    || "org.apache.commons.math.exception.NumberIsTooSmallException".equals(n)
                    || "org.apache.commons.math.exception.MathIllegalNumberException".equals(n)) {
                classMatches = true;
                break;
            }
        }
        if (!classMatches) {
            return false;
        }

        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer".equals(cls)
                    && ("determineLMParameter".equals(m) || "determineLMDirection".equals(m))) {
                return true;
            }
            if ("org.apache.commons.math.util.FastMath".equals(cls)
                    && ("sqrt".equals(m) || "min".equals(m) || "max".equals(m) || "abs".equals(m))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~13,070 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        LevenbergMarquardtOptimizer opt = new LevenbergMarquardtOptimizer();
        try {
            double[] fit = runFit(opt, buildX(ANCHOR_Y.length, 0.0, 1.0), ANCHOR_Y);

            if (fit == null || fit.length < 3) {
                throw new RuntimeException("[oracle:anchor-shape] metamorphic violation: fit returned invalid parameter array");
            }

            if (Math.abs(fit[1] - 53.1572792) > 1.0e-5) {
                throw new RuntimeException("[oracle:anchor-center] metamorphic violation: exact regression input should recover documented center input=Math519 lhs=" + fit[1] + " rhs=53.1572792");
            }

            double[] fit2 = runFit(new LevenbergMarquardtOptimizer(), buildX(ANCHOR_Y.length, 0.0, 1.0), ANCHOR_Y);
            assertClose("repeat-center", fit[1], fit2[1], 1.0e-10);
            assertClose("repeat-sigma", fit[2], fit2[2], 1.0e-10);

            double scale = 7.0;
            double[] scaledY = scaleY(ANCHOR_Y, scale);
            double[] scaledFit = runFit(new LevenbergMarquardtOptimizer(), buildX(ANCHOR_Y.length, 0.0, 1.0), scaledY);

            /* Contract/oracle:
             * GaussianFitter fits a parametric Gaussian y = norm * exp(-(x-mean)^2 / (2*sigma^2)).
             * Multiplying every observed y by a positive constant yields an equivalent least-squares
             * problem where only "norm" scales; mean and sigma must stay the same for any correct implementation.
             * A throw-deleting or branch-skipping patch in LM parameter updates can silently return wrong mean/sigma.
             */
            assertRelativeClose("scale-center", fit[1], scaledFit[1], 1.0e-6);
            assertRelativeClose("scale-sigma", fit[2], scaledFit[2], 1.0e-6);
            assertRelativeClose("scale-norm", fit[0] * scale, scaledFit[0], 1.0e-5);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(8, 27);
        int offset = data.consumeInt(0, ANCHOR_Y.length - len);
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double base = ANCHOR_Y[offset + i];
            int pct = data.consumeInt(50, 150);
            y[i] = base * (pct / 100.0);
            if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                y[i] = base;
            }
        }

        double startX = data.consumeInt(-50, 50);
        double step = data.consumeInt(1, 5);
        double[] x = buildX(len, startX, step);

        double yScale = data.consumeInt(1, 20);
        double[] scaledY = scaleY(y, yScale);

        double orthoBase;
        switch (data.consumeInt(0, 5)) {
            case 0:
                orthoBase = 0.0;
                break;
            case 1:
                orthoBase = 1.0e-20;
                break;
            case 2:
                orthoBase = 1.0e-14;
                break;
            case 3:
                orthoBase = 1.0e-10;
                break;
            case 4:
                orthoBase = 1.0e-6;
                break;
            default:
                orthoBase = Math.abs(data.consumeInt(-1000, 1000)) * 1.0e-8;
                break;
        }

        double tweak = 1.0 + (data.consumeInt(-100, 100) * 1.0e-4);
        double orthoTolerance = Math.abs(orthoBase * tweak);
        if (Double.isNaN(orthoTolerance) || Double.isInfinite(orthoTolerance)) {
            orthoTolerance = 1.0e-10;
        }

        LevenbergMarquardtOptimizer opt1 = new LevenbergMarquardtOptimizer(
                100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);
        LevenbergMarquardtOptimizer opt2 = new LevenbergMarquardtOptimizer(
                100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);
        LevenbergMarquardtOptimizer opt3 = new LevenbergMarquardtOptimizer(
                100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);

        try {
            double[] fit1 = runFit(opt1, x, y);
            double[] fit2 = runFit(opt2, x, y);

            assertRelativeClose("det-center", fit1[1], fit2[1], 1.0e-8);
            assertRelativeClose("det-sigma", fit1[2], fit2[2], 1.0e-8);

            double[] fitScaled = runFit(opt3, x, scaledY);
            assertRelativeClose("fuzz-scale-center", fit1[1], fitScaled[1], 2.0e-5);
            assertRelativeClose("fuzz-scale-sigma", fit1[2], fitScaled[2], 2.0e-5);
            assertRelativeClose("fuzz-scale-norm", fit1[0] * yScale, fitScaled[0], 1.0e-4);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static double[] runFit(LevenbergMarquardtOptimizer optimizer, double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(optimizer);
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter.fit();
    }

    private static double[] buildX(int len, double start, double step) {
        double[] x = new double[len];
        for (int i = 0; i < len; i++) {
            x[i] = start + step * i;
        }
        return x;
    }

    private static double[] scaleY(double[] y, double scale) {
        double[] out = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            out[i] = y[i] * scale;
        }
        return out;
    }

    private static void assertClose(String id, double lhs, double rhs, double tol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)
                || Math.abs(lhs - rhs) > tol) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: lhs=" + lhs + " rhs=" + rhs + " tol=" + tol);
        }
    }

    private static void assertRelativeClose(String id, double lhs, double rhs, double relTol) {
        if (Double.isNaN(lhs) || Double.isNaN(rhs) || Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: non-finite lhs=" + lhs + " rhs=" + rhs);
        }
        double scale = Math.max(1.0, Math.max(Math.abs(lhs), Math.abs(rhs)));
        if (Math.abs(lhs - rhs) > relTol * scale) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: lhs=" + lhs + " rhs=" + rhs + " relTol=" + relTol);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }

        if (t instanceof IllegalArgumentException) {
            return;
        }
        if (t instanceof NumberFormatException) {
            return;
        }
        if (isValidationFamily(t)) {
            return;
        }
    }

    private static boolean isValidationFamily(Throwable t) {
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            if (n.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }

        boolean classMatches = false;
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            if ("org.apache.commons.math.exception.NotStrictlyPositiveException".equals(n)
                    || "org.apache.commons.math.exception.NumberIsTooSmallException".equals(n)
                    || "org.apache.commons.math.exception.MathIllegalNumberException".equals(n)) {
                classMatches = true;
                break;
            }
        }
        if (!classMatches) {
            return false;
        }

        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer".equals(cls)
                    && ("determineLMParameter".equals(m) || "determineLMDirection".equals(m))) {
                return true;
            }
            if ("org.apache.commons.math.util.FastMath".equals(cls)
                    && ("sqrt".equals(m) || "min".equals(m) || "max".equals(m) || "abs".equals(m))) {
                return true;
            }
        }
        return false;
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java	
+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java
@@ -617,7 +617,9 @@
             }
 
             // compute an improved estimate for lmPar
+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {
             lmPar = FastMath.max(parl, lmPar + correction);
+            }
 
         }
     }


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
<details open><summary>▸ Output (~4,689 chars)</summary>

```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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

        int choice = data.consumeInt(0, 3);
        double scale = data.consumeInt(1, 20);
        int start = data.consumeInt(-10, 10);
        int stride = data.consumeInt(1, 4);
        double orthoTolerance;
        if (choice == 0) {
            orthoTolerance = 0.0;
        } else if (choice == 1) {
            orthoTolerance = 1.0e-20;
        } else if (choice == 2) {
            orthoTolerance = 1.0e-12;
        } else {
            orthoTolerance = Math.abs(data.consumeInt(-1000, 1000)) * 1.0e-8;
        }

        double[] varied = new double[ANCHOR_DATA.length];
        for (int i = 0; i < varied.length; i++) {
            int pct = data.consumeInt(80, 120);
            varied[i] = ANCHOR_DATA[i] * (pct / 100.0);
            if (!(varied[i] > 0.0) || Double.isNaN(varied[i]) || Double.isInfinite(varied[i])) {
                varied[i] = ANCHOR_DATA[i];
            }
        }

        double[] fit1 = runFit(varied, start, stride, 1.0, orthoTolerance);
        double[] fit2 = runFit(varied, start, stride, scale, orthoTolerance);

        /* Contract used for the post-condition:
         * Fitting the same x-values with all y-values multiplied by a positive constant
         * must preserve the Gaussian mean and sigma; only the amplitude scales.
         * This is a sound metamorphic relation for any correct implementation of Gaussian fitting.
         * A patch that merely suppresses the crash but corrupts the LM update can violate it.
         */
        assertCloseRelative("mean-scale", fit1[1], fit2[1], 1.0e-5);
        assertCloseRelative("sigma-scale", fit1[2], fit2[2], 1.0e-5);
    }

    private static void runAnchor() {
        double[] fit = runFit(ANCHOR_DATA, 0, 1, 1.0, 1.0e-10);

        if (fit == null || fit.length < 3) {
            throw new RuntimeException("[oracle:anchor-shape] metamorphic violation: invalid parameter array");
        }

        if (Math.abs(fit[1] - 53.1572792) > 1.0e-7) {
            throw new RuntimeException("[oracle:anchor-center] metamorphic violation: input=Math519 lhs=" + fit[1] + " rhs=53.1572792");
        }

        double[] scaledFit = runFit(ANCHOR_DATA, 0, 1, 7.0, 1.0e-10);
        assertCloseRelative("anchor-mean-scale", fit[1], scaledFit[1], 1.0e-6);
        assertCloseRelative("anchor-sigma-scale", fit[2], scaledFit[2], 1.0e-6);
    }

    private static double[] runFit(double[] y, int xStart, int xStride, double yScale, double orthoTolerance) {
        LevenbergMarquardtOptimizer optimizer =
                new LevenbergMarquardtOptimizer(100.0, 1.0e-10, 1.0e-10, orthoTolerance, 1.0e-14);
        GaussianFitter fitter = new GaussianFitter(optimizer);
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(xStart + (double) (i * xStride), y[i] * yScale);
        }
        return fitter.fit();
    }

    private static void assertCloseRelative(String id, double a, double b, double relTol) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: non-finite values lhs=" + a + " rhs=" + b);
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        if (Math.abs(a - b) > relTol * scale) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: lhs=" + a + " rhs=" + b + " relTol=" + relTol);
        }
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:anchor-center] metamorphic violation: input=Math519 lhs=26.0 rhs=53.1572792

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,401 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.math.optimization.general` (`package org.apache.commons.math.optimization.general;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java	
+++ /src/main/java/org/apache/commons/math/optimization/general/LevenbergMarquardtOptimizer.java
@@ -617,7 +617,9 @@
             }
 
             // compute an improved estimate for lmPar
+            if(org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.this.orthoTolerance < gNorm) {
             lmPar = FastMath.max(parl, lmPar + correction);
+            }
 
         }
     }

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.util.Arrays;
import org.apache.commons.math.exception.ConvergenceException;
import org.apache.commons.math.exception.MathUserException;
import org.apache.commons.math.exception.util.LocalizedFormats;
import org.apache.commons.math.optimization.VectorialPointValuePair;
import org.apache.commons.math.optimization.ConvergenceChecker;
import org.apache.commons.math.util.MathUtils;
import org.apache.commons.math.util.FastMath;
</source_imports>

Function `determineLMParameter`:
<signature>
private void determineLMParameter(double[] qy, double delta, double[] diag, double[] work1, double[] work2, double[] work3)
</signature>
<code>
    private void determineLMParameter(double[] qy, double delta, double[] diag,
            double[] work1, double[] work2, double[] work3) {

        // compute and store in x the gauss-newton direction, if the
        // jacobian is rank-deficient, obtain a least squares solution
        for (int j = 0; j < rank; ++j) {
            lmDir[permutation[j]] = qy[j];
        }
        for (int j = rank; j < cols; ++j) {
            lmDir[permutation[j]] = 0;
        }
        for (int k = rank - 1; k >= 0; --k) {
            int pk = permutation[k];
            double ypk = lmDir[pk] / diagR[pk];
            for (int i = 0; i < k; ++i) {
                lmDir[permutation[i]] -= ypk * weightedResidualJacobian[i][pk];
            }
            lmDir[pk] = ypk;
        }

        // evaluate the function at the origin, and test
        // for acceptance of the Gauss-Newton direction
        double dxNorm = 0;
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            double s = diag[pj] * lmDir[pj];
            work1[pj] = s;
            dxNorm += s * s;
        }
        dxNorm = FastMath.sqrt(dxNorm);
        double fp = dxNorm - delta;
        if (fp <= 0.1 * delta) {
            lmPar = 0;
            return;
        }

        // if the jacobian is not rank deficient, the Newton step provides
        // a lower bound, parl, for the zero of the function,
        // otherwise set this bound to zero
        double sum2;
        double parl = 0;
        if (rank == solvedCols) {
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] *= diag[pj] / dxNorm;
            }
            sum2 = 0;
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                double sum = 0;
                for (int i = 0; i < j; ++i) {
                    sum += weightedResidualJacobian[i][pj] * work1[permutation[i]];
                }
                double s = (work1[pj] - sum) / diagR[pj];
                work1[pj] = s;
                sum2 += s * s;
            }
            parl = fp / (delta * sum2);
        }

        // calculate an upper bound, paru, for the zero of the function
        sum2 = 0;
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            double sum = 0;
            for (int i = 0; i <= j; ++i) {
                sum += weightedResidualJacobian[i][pj] * qy[i];
            }
            sum /= diag[pj];
            sum2 += sum * sum;
        }
        double gNorm = FastMath.sqrt(sum2);
        double paru = gNorm / delta;
        if (paru == 0) {
            // 2.2251e-308 is the smallest positive real for IEE754
            paru = 2.2251e-308 / FastMath.min(delta, 0.1);
        }

        // if the input par lies outside of the interval (parl,paru),
        // set par to the closer endpoint
        lmPar = FastMath.min(paru, FastMath.max(lmPar, parl));
        if (lmPar == 0) {
            lmPar = gNorm / dxNorm;
        }

        for (int countdown = 10; countdown >= 0; --countdown) {

            // evaluate the function at the current value of lmPar
            if (lmPar == 0) {
                lmPar = FastMath.max(2.2251e-308, 0.001 * paru);
            }
            double sPar = FastMath.sqrt(lmPar);
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] = sPar * diag[pj];
            }
            determineLMDirection(qy, work1, work2, work3);

            dxNorm = 0;
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                double s = diag[pj] * lmDir[pj];
                work3[pj] = s;
                dxNorm += s * s;
            }
            dxNorm = FastMath.sqrt(dxNorm);
            double previousFP = fp;
            fp = dxNorm - delta;

            // if the function is small enough, accept the current value
            // of lmPar, also test for the exceptional cases where parl is zero
            if ((FastMath.abs(fp) <= 0.1 * delta) ||
                    ((parl == 0) && (fp <= previousFP) && (previousFP < 0))) {
                return;
            }

            // compute the Newton correction
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] = work3[pj] * diag[pj] / dxNorm;
            }
            for (int j = 0; j < solvedCols; ++j) {
                int pj = permutation[j];
                work1[pj] /= work2[j];
                double tmp = work1[pj];
                for (int i = j + 1; i < solvedCols; ++i) {
                    work1[permutation[i]] -= weightedResidualJacobian[i][pj] * tmp;
                }
            }
            sum2 = 0;
            for (int j = 0; j < solvedCols; ++j) {
                double s = work1[permutation[j]];
                sum2 += s * s;
            }
            double correction = fp / (delta * sum2);

            // depending on the sign of the function, update parl or paru.
            if (fp > 0) {
                parl = FastMath.max(parl, lmPar);
            } else if (fp < 0) {
                paru = FastMath.min(paru, lmPar);
            }

            // compute an improved estimate for lmPar
            lmPar = FastMath.max(parl, lmPar + correction);

        }
    }
</code>
Methods called by `determineLMParameter` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="determineLMDirection" from="LevenbergMarquardtOptimizer.java">
<signature>
private void determineLMDirection(double[] qy, double[] diag, double[] lmDiag, double[] work)
</signature>
<code>
    private void determineLMDirection(double[] qy, double[] diag,
            double[] lmDiag, double[] work) {

        // copy R and Qty to preserve input and initialize s
        //  in particular, save the diagonal elements of R in lmDir
        for (int j = 0; j < solvedCols; ++j) {
            int pj = permutation[j];
            for (int i = j + 1; i < solvedCols; ++i) {
                weightedResidualJacobian[i][pj] = weightedResidualJacobian[j][permutation[i]];
            }
            lmDir[j] = diagR[pj];
            work[j]  = qy[j];
        }

        // eliminate the diagonal matrix d using a Givens rotation
        for (int j = 0; j < solvedCols; ++j) {

            // prepare the row of d to be eliminated, locating the
            // diagonal element using p from the Q.R. factorization
            int pj = permutation[j];
            double dpj = diag[pj];
            if (dpj != 0) {
                Arrays.fill(lmDiag, j + 1, lmDiag.length, 0);
            }
            lmDiag[j] = dpj;

            //  the transformations to eliminate the row of d
            // modify only a single element of Qty
            // beyond the first n, which is initially zero.
            double qtbpj = 0;
            for (int k = j; k < solvedCols; ++k) {
                int pk = permutation[k];

                // determine a Givens rotation which eliminates the
                // appropriate element in the current row of d
                if (lmDiag[k] != 0) {

                    final double sin;
                    final double cos;
                    double rkk = weightedResidualJacobian[k][pk];
                    if (FastMath.abs(rkk) < FastMath.abs(lmDiag[k])) {
                        final double cotan = rkk / lmDiag[k];
                        sin   = 1.0 / FastMath.sqrt(1.0 + cotan * cotan);
                        cos   = sin * cotan;
                    } else {
                        final double tan = lmDiag[k] / rkk;
                        cos = 1.0 / FastMath.sqrt(1.0 + tan * tan);
                        sin = cos * tan;
                    }

                    // compute the modified diagonal element of R and
                    // the modified element of (Qty,0)
                    weightedResidualJacobian[k][pk] = cos * rkk + sin * lmDiag[k];
                    final double temp = cos * work[k] + sin * qtbpj;
                    qtbpj = -sin * work[k] + cos * qtbpj;
                    work[k] = temp;

                    // accumulate the tranformation in the row of s
                    for (int i = k + 1; i < solvedCols; ++i) {
                        double rik = weightedResidualJacobian[i][pk];
                        final double temp2 = cos * rik + sin * lmDiag[i];
                        lmDiag[i] = -sin * rik + cos * lmDiag[i];
                        weightedResidualJacobian[i][pk] = temp2;
                    }
                }
            }

            // store the diagonal element of s and restore
            // the corresponding diagonal element of R
            lmDiag[j] = weightedResidualJacobian[j][permutation[j]];
            weightedResidualJacobian[j][permutation[j]] = lmDir[j];
        }

        // solve the triangular system for z, if the system is
        // singular, then obtain a least squares solution
        int nSing = solvedCols;
        for (int j = 0; j < solvedCols; ++j) {
            if ((lmDiag[j] == 0) && (nSing == solvedCols)) {
                nSing = j;
            }
            if (nSing < solvedCols) {
                work[j] = 0;
            }
        }
        if (nSing > 0) {
            for (int j = nSing - 1; j >= 0; --j) {
                int pj = permutation[j];
                double sum = 0;
                for (int i = j + 1; i < nSing; ++i) {
                    sum += weightedResidualJacobian[i][pj] * work[i];
                }
                work[j] = (work[j] - sum) / lmDiag[j];
            }
        }

        // permute the components of z back to components of lmDir
        for (int j = 0; j < lmDir.length; ++j) {
            lmDir[permutation[j]] = work[j];
        }
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `determineLMParameter` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `determineLMParameter`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `protected VectorialPointValuePair doOptimize() throws MathUserException` (shared field(s): diagR, lmDir, lmPar, permutation, solvedCols)
      doc: {@inheritDoc}
  - `private void qrDecomposition() throws ConvergenceException` (shared field(s): diagR, permutation, rank, solvedCols)
      doc: Decompose a matrix A as A.P = Q.R using Householder transforms. <p>As suggested in the P. Lascaux and R. Theodor book <i>Analyse num&eacute;rique matricielle appliqu&eacute;e &agrave; l'art de l'ing&eacute;nieur</i> (Masson, 1986), instead of representing the Householder transforms with u<sub>k</sub> unit vectors such …
  - `protected void updateJacobian()` (shared field(s): cols, weightedResidualJacobian)
      doc: Update the jacobian matrix. @throws DimensionMismatchException if the Jacobian dimension does not match problem dimension. @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one.
  - `public double[][] getCovariances()` (shared field(s): cols, weightedResidualJacobian)
      doc: Get the covariance matrix of the optimized parameters. @return the covariance matrix. @throws org.apache.commons.math.exception.SingularMatrixException if the covariance matrix cannot be computed (singular problem). @throws org.apache.commons.math.exception.MathUserException if the jacobian function throws one.
  - `public VectorialPointValuePair optimize(int maxEval, DifferentiableMultivariateVectorialFunction f, double[] target, double[] weights, double[] startPoint)` (shared field(s): cols, weightedResidualJacobian)
      doc: {@inheritDoc}

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `GaussianFitter`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: org.apache.commons.math.exception.NotStrictlyPositiveException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `determineLMParameter` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- FastMath.sqrt
- FastMath.min
- FastMath.max
- LevenbergMarquardtOptimizer.determineLMDirection
- FastMath.abs(LevenbergMarquardtOptimizer)
- FastMath.abs()
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters
- org.apache.commons.math.analysis.function.Gaussian$Parametric.value
- org.apache.commons.math.optimization.direct.BaseAbstractVectorialOptimizer.computeObjectiveValue
- org.apache.commons.math.optimization.direct.BaseAbstractVectorialOptimizer.optimize
- org.apache.commons.math.optimization.fitting.CurveFitter$TheoreticalValuesFunction.value
- org.apache.commons.math.optimization.fitting.CurveFitter.fit
- org.apache.commons.math.optimization.fitting.GaussianFitter.fit
- org.apache.commons.math.optimization.general.AbstractLeastSquaresOptimizer.optimize
- org.apache.commons.math.optimization.general.AbstractLeastSquaresOptimizer.updateResidualsAndCost
- org.apache.commons.math.optimization.general.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.math.optimization.general.FuzzHarness.runAnchor
- org.apache.commons.math.optimization.general.FuzzHarness.runFit
- org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer.doOptimize
Crashes already found:
- INFO@org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters
- java.lang.RuntimeException@org.apache.commons.math.optimization.general.FuzzHarness.runAnchor
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- FastMath.sqrt
- FastMath.min
- FastMath.max
- LevenbergMarquardtOptimizer.determineLMDirection
- FastMath.abs(LevenbergMarquardtOptimizer)
- FastMath.abs()
Check FAMILIES already covered by accepted harnesses: {anchor-center, anchor-mean-scale, anchor-shape, anchor-sigma-scale, mean-scale, param-count, sigma-scale, unit-weight-equiv}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): INFO@org.apache.commons.math.analysis.function.Gaussian$Parametric.validateParameters; java.lang.RuntimeException@org.apache.commons.math.optimization.general.FuzzHarness.runAnchor. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~9,863 chars)</summary>

```
package org.apache.commons.math.optimization.general;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.fitting.GaussianFitter;

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
        runAnchor();

        GeneratedDataset ds = buildTailDataset(data);
        double shift = boundedShift(data);

        FitResult base;
        FitResult shifted;
        try {
            base = runFit(ds.x, ds.y);
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                rethrowUnchecked(t);
            }
            return;
        }

        double[] shiftedX = new double[ds.x.length];
        for (int i = 0; i < ds.x.length; i++) {
            shiftedX[i] = ds.x[i] + shift;
        }

        try {
            shifted = runFit(shiftedX, ds.y);
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                rethrowUnchecked(t);
            }
            return;
        }

        if (base == null || shifted == null || base.params == null || shifted.params == null) {
            return;
        }
        if (base.params.length < 3 || shifted.params.length < 3) {
            return;
        }

        double a1 = base.params[0];
        double m1 = base.params[1];
        double s1 = Math.abs(base.params[2]);
        double a2 = shifted.params[0];
        double m2 = shifted.params[1];
        double s2 = Math.abs(shifted.params[2]);

        if (!allFinite(a1, m1, s1, a2, m2, s2)) {
            return;
        }

        // Contract/oracle: translating every observed x by a constant while keeping y unchanged
        // represents the same Gaussian shape in translated coordinates. A correct fitter must
        // therefore preserve norm and sigma and shift only the mean by that constant.
        if (!closeRel(a1, a2, 0.20, 1e-6) ||
            !closeRel(s1, s2, 0.20, 1e-6) ||
            !closeRel(m1 + shift, m2, 0.20, 1e-4)) {
            throw new RuntimeException(
                "[oracle:x-shift] metamorphic violation: translated fit inconsistent"
                    + " shift=" + shift
                    + " base=[" + a1 + "," + m1 + "," + s1 + "]"
                    + " shifted=[" + a2 + "," + m2 + "," + s2 + "]");
        }

        // Contract/oracle: covariance is derived from the same least-squares problem geometry.
        // Pure translation of x changes only the coordinate origin, so the covariance matrix for
        // the fitted parameters should agree up to numerical tolerance.
        if (base.cov != null && shifted.cov != null &&
            base.cov.length == shifted.cov.length && base.cov.length > 0) {
            for (int i = 0; i < base.cov.length; i++) {
                if (base.cov[i] == null || shifted.cov[i] == null ||
                    base.cov[i].length != shifted.cov[i].length) {
                    return;
                }
                for (int j = 0; j < base.cov[i].length; j++) {
                    double c1 = base.cov[i][j];
                    double c2 = shifted.cov[i][j];
                    if (!allFinite(c1, c2)) {
                        return;
                    }
                    if (!closeRel(c1, c2, 0.35, 1e-6)) {
                        throw new RuntimeException(
                            "[oracle:cov-shift] metamorphic violation: covariance changed under x-translation"
                                + " shift=" + shift
                                + " at=(" + i + "," + j + ")"
                                + " lhs=" + c1 + " rhs=" + c2);
                    }
                }
            }
        }
    }

    private static void runAnchor() {
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        try {
            runFit(x, ANCHOR_Y);
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static FitResult runFit(double[] x, double[] y) {
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);
        for (int i = 0; i < x.length && i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        double[] params = fitter.fit();
        double[][] cov = null;
        try {
            cov = optimizer.getCovariances();
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                rethrowUnchecked(t);
            }
        }
        return new FitResult(params, cov);
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof RuntimeException) {
            String name = t.getClass().getName();
            if (name.equals("org.apache.commons.math.exception.NotStrictlyPositiveException")
                && stackHasRootRegion(t)) {
                return true;
            }
            if (name.startsWith("java.lang.RuntimeException")
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:")
                && stackHasHarness(t)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stackHasRootRegion(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer".equals(c)) {
                if ("determineLMParameter".equals(m) || "determineLMDirection".equals(m) || "doOptimize".equals(m)) {
                    return true;
                }
            }
            if ("org.apache.commons.math.util.FastMath".equals(c)) {
                if ("sqrt".equals(m) || "min".equals(m) || "max".equals(m) || "abs".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean stackHasHarness(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.general.FuzzHarness".equals(e.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static GeneratedDataset buildTailDataset(FuzzedDataProvider data) {
        int n = data.consumeInt(6, 32);
        double start = data.consumeInt(-50, 50);
        double step = 0.25 + (data.consumeInt(0, 300) / 100.0);
        double sigma = 0.5 + (data.consumeInt(0, 950) / 100.0);
        double amplitude = 1e-6 + (data.consumeInt(0, 1_000_000) / 10_000.0);
        double centerOffsetSigmas = 0.2 + (data.consumeInt(0, 480) / 100.0);

        double[] x = new double[n];
        double[] y = new double[n];

        double lastX = start + (n - 1) * step;
        double center = lastX + centerOffsetSigmas * sigma;

        for (int i = 0; i < n; i++) {
            x[i] = start + i * step;
            double z = (x[i] - center) / sigma;
            double base = amplitude * Math.exp(-0.5 * z * z);
            double noiseScale = 0.90 + (data.consumeInt(0, 20) / 100.0);
            double yi = base * noiseScale;
            if (!(yi > 0.0) || Double.isNaN(yi) || Double.isInfinite(yi)) {
                yi = amplitude * 1e-12;
            }
            y[i] = yi;
        }

        return new GeneratedDataset(x, y);
    }

    private static double boundedShift(FuzzedDataProvider data) {
        return data.consumeInt(-40, 40) + (data.consumeInt(0, 999) / 1000.0);
    }

    private static boolean closeRel(double a, double b, double relTol, double absTol) {
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.max(Math.abs(a), Math.abs(b)), 1.0);
        return diff <= absTol || diff <= relTol * scale;
    }

    private static boolean allFinite(double... v) {
        for (double d : v) {
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return false;
            }
        }
        return true;
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

    private static final class FitResult {
        final double[] params;
        final double[][] cov;

        FitResult(double[] params, double[][] cov) {
            this.params = params;
            this.cov = cov;
        }
    }

    private static final class GeneratedDataset {
        final double[] x;
        final double[] y;

        GeneratedDataset(double[] x, double[] y) {
            this.x = x;
            this.y = y;
        }
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
## [13] ⚙️ corpus-seed · `attempt_002`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['GaussianFitter', '1.1143831578403364E-29', '4.95281403484594E-28', '1.1171347211930288E-26', '1.7044813962636277E-25', '1.9784716574832164E-24', '1.8630236407866774E-23', '1.4820532905097742E-22']}

---
## [14] ⚙️ corpus-seed · `attempt_004`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['GaussianFitter', '1.1143831578403364E-29', '4.95281403484594E-28', '1.1171347211930288E-26', '1.7044813962636277E-25', '1.9784716574832164E-24', '1.8630236407866774E-23', '1.4820532905097742E-22']}

---
## [15] ⚙️ corpus-seed · `attempt_005`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['GaussianFitter', '1.1143831578403364E-29', '4.95281403484594E-28', '1.1171347211930288E-26', '1.7044813962636277E-25', '1.9784716574832164E-24', '1.8630236407866774E-23', '1.4820532905097742E-22']}

---
## [16] ⚙️ patched-fuzz · `attempt_002`
**output:** **quiet on patched build (no overfit signal)**

---
## [17] ⚙️ patched-fuzz · `attempt_004`
**output:** **FIRED — [oracle:anchor-center] metamorphic violation: input=Math519 lhs=26.0 rhs=53.1572792**
- reproducing_input_file: /tmp/d4j/Math_58_buggy/fuzz/attempt_004/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [18] ⚙️ patched-fuzz · `attempt_005`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Math_58_buggy/fuzz/attempt_005/crashes/crash-23582738b6cd0f5621f29cbfeb5820b0ea32ae32
