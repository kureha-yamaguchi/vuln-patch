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

138 sequential steps — 35 LLM calls, 103 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.linear.EigenDecompositionImplTest', test_method='tes (~3,462 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.linear.EigenDecompositionImplTest', test_method='testMathpbx02', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/src/test/java/org/apache/commons/math/linear/EigenDecompositionImplTest.java', method_source='    public void testMathpbx02() {\\n\\n        double[] mainTridiagonal = {\\n        \\t  7484.860960227216, 18405.28129035345, 13855.225609560746,\\n        \\t 10016.708722343366, 559.8117399576674, 6750.190788301587, \\n        \\t    71.21428769782159\\n        };\\n        double[] secondaryTridiagonal = {\\n        \\t -4175.088570476366,1975.7955858241994,5193.178422374075, \\n        \\t  1995.286659169179,75.34535882933804,-234.0808002076056\\n        };\\n\\n        // the reference values have been computed using routine DSTEMR\\n        // from the fortran library LAPACK version 3.2.1\\n        double[] refEigenValues = {\\n        \\t\\t20654.744890306974412,16828.208208485466457,\\n        \\t\\t6893.155912634994820,6757.083016675340332,\\n        \\t\\t5887.799885688558788,64.309089923240379,\\n        \\t\\t57.992628792736340\\n        };\\n        RealVector[] refEigenVectors = {\\n        \\t\\tnew ArrayRealVector(new double[] {-0.270356342026904, 0.852811091326997, 0.399639490702077, 0.198794657813990, 0.019739323307666, 0.000106983022327, -0.000001216636321}),\\n        \\t\\tnew ArrayRealVector(new double[] {0.179995273578326,-0.402807848153042,0.701870993525734,0.555058211014888,0.068079148898236,0.000509139115227,-0.000007112235617}),\\n        \\t\\tnew ArrayRealVector(new double[] {-0.399582721284727,-0.056629954519333,-0.514406488522827,0.711168164518580,0.225548081276367,0.125943999652923,-0.004321507456014}),\\n        \\t\\tnew ArrayRealVector(new double[] {0.058515721572821,0.010200130057739,0.063516274916536,-0.090696087449378,-0.017148420432597,0.991318870265707,-0.034707338554096}),\\n        \\t\\tnew ArrayRealVector(new double[] {0.855205995537564,0.327134656629775,-0.265382397060548,0.282690729026706,0.105736068025572,-0.009138126622039,0.000367751821196}),\\n        \\t\\tnew ArrayRealVector(new double[] {-0.002913069901144,-0.005177515777101,0.041906334478672,-0.109315918416258,0.436192305456741,0.026307315639535,0.891797507436344}),\\n        \\t\\tnew ArrayRealVector(new double[] {-0.005738311176435,-0.010207611670378,0.082662420517928,-0.215733886094368,0.861606487840411,-0.025478530652759,-0.451080697503958})\\n        };\\n\\n        // the following line triggers the exception\\n        EigenDecomposition decomposition =\\n            new EigenDecompositionImpl(mainTridiagonal, secondaryTridiagonal, MathUtils.SAFE_MIN);\\n\\n        double[] eigenValues = decomposition.getRealEigenvalues();\\n        for (int i = 0; i < refEigenValues.length; ++i) {\\n            assertEquals(refEigenValues[i], eigenValues[i], 1.0e-3);\\n            if (refEigenVectors[i].dotProduct(decomposition.getEigenvector(i)) < 0) {\\n                assertEquals(0, refEigenVectors[i].add(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);\\n            } else {\\n                assertEquals(0, refEigenVectors[i].subtract(decomposition.getEigenvector(i)).getNorm(), 1.0e-5);\\n            }\\n        }\\n\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~17,756 chars)</summary>

**[system]**
```
You are a software-verification expert. Given a patched Java method and its API, you propose RELATIONS that must hold for EVERY correct implementation — never merely for the reference or for the tested inputs. Two kinds only:
  * METAMORPHIC: two real API calls whose results must agree (or relate in a fixed way) for any valid input — e.g. parse(format(x)) == x; f(a,b) == f(b,a) when documented commutative; reduce(reduce(x)) == reduce(x).
  * INVARIANT: a property the output must satisfy for every valid input, read straight off the documented contract — e.g. a reduced fraction has gcd(numerator,denominator)==1; a returned index is within bounds.
Each relation must be SOUND: if any correct implementation could violate it, do not propose it. Avoid comparisons to a different library, and avoid boundary-fragile claims (division by zero, NaN, overflow, empty/null) unless you explicitly exclude those inputs.
```
**[user]**
```
METHOD(S) UNDER TEST — source as it stands in the checkout. Use it to see the code shape and API, NEVER as a model of correct behaviour (it is the code as written, not a specification):
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
THE CLASS UNDER TEST: EigenDecompositionImpl. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.

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
<details open><summary>▸ Output (~10,483 chars)</summary>

```
[
  {
    "name": "reverse_tridiagonal_preserves_eigenvalues_when_last_dominates_first",
    "kind": "metamorphic",
    "contract": "EigenDecompositionImpl computes the eigen-decomposition of a real symmetric tridiagonal matrix; reversing the basis order produces a permutation-similar matrix, so the eigenvalue multiset is unchanged. This directly exercises the internal flip/no-flip case where the trailing work entry is much larger than the leading one.",
    "input": "Choose n in [2, 6]. Build a finite symmetric tridiagonal matrix by arrays main[0..n-1] and secondary[0..n-2]. Force the changed-condition shape by making main[n-1] > 1.6 * main[0], with moderate magnitudes to avoid overflow. Build the reversed tridiagonal by reversing main and secondary.",
    "check": "int n = data.consumeInt(2, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n    main[i] = 1.0 + data.consumeInt(0, 20) + i;\n}\nmain[0] = 1.0 + data.consumeInt(0, 5);\nmain[n - 1] = Math.max(main[n - 1], 1.6 * main[0] + 1.0 + data.consumeInt(0, 5));\nfor (int i = 0; i < n - 1; i++) {\n    secondary[i] = data.consumeInt(-3, 3);\n}\ndouble[] rMain = new double[n];\ndouble[] rSecondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n    rMain[i] = main[n - 1 - i];\n}\nfor (int i = 0; i < n - 1; i++) {\n    rSecondary[i] = secondary[n - 2 - i];\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed1;\norg.apache.commons.math.linear.EigenDecompositionImpl ed2;\ndouble[] e1 = new double[n];\ndouble[] e2 = new double[n];\ntry {\n    ed1 = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, 0.0);\n    ed2 = new org.apache.commons.math.linear.EigenDecompositionImpl(rMain, rSecondary, 0.0);\n    for (int i = 0; i < n; i++) {\n        e1[i] = ed1.getRealEigenvalue(i);\n        e2[i] = ed2.getRealEigenvalue(i);\n    }\n} catch (Exception e) {\n    throw new RuntimeException(\"relation reverse_tridiagonal_preserves_eigenvalues_when_last_dominates_first violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\njava.util.Arrays.sort(e1);\njava.util.Arrays.sort(e2);\nfor (int i = 0; i < n; i++) {\n    double a = e1[i];\n    double b = e2[i];\n    double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));\n    if (Math.abs(a - b) > tol) {\n        throw new RuntimeException(\"relation reverse_tridiagonal_preserves_eigenvalues_when_last_dominates_first violated: eigenvalue[\" + i + \"] \" + a + \" vs \" + b);\n    }\n}"
  },
  {
    "name": "determinant_matches_product_of_real_eigenvalues",
    "kind": "invariant",
    "contract": "For an eigen-decomposition of a real matrix, the determinant equals the product of its eigenvalues; for this symmetric decomposition the exposed real eigenvalues must therefore multiply to getDeterminant().",
    "input": "Choose n in [1, 6]. Build finite main and secondary arrays with moderate integer-valued entries so the tridiagonal matrix is valid and products stay in a safe double range.",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < n; i++) {\n    main[i] = data.consumeInt(-5, 5);\n}\nfor (int i = 0; i < n - 1; i++) {\n    secondary[i] = data.consumeInt(-3, 3);\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\ndouble det;\ndouble prod = 1.0;\ntry {\n    ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, 0.0);\n    det = ed.getDeterminant();\n    for (int i = 0; i < n; i++) {\n        prod *= ed.getRealEigenvalue(i);\n    }\n} catch (Exception e) {\n    throw new RuntimeException(\"relation determinant_matches_product_of_real_eigenvalues violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ndouble tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(det), Math.abs(prod)));\nif (Math.abs(det - prod) > tol) {\n    throw new RuntimeException(\"relation determinant_matches_product_of_real_eigenvalues violated: determinant=\" + det + \" product=\" + prod);\n}"
  },
  {
    "name": "symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_D",
    "kind": "invariant",
    "contract": "This class decomposes a real symmetric matrix. Therefore all eigenvalues are real, getImagEigenvalue(i) must be zero, and the D matrix exposed by the decomposition must be diagonal with those real eigenvalues on its diagonal.",
    "input": "Choose n in [1, 6]. Build any finite symmetric tridiagonal input from main and secondary arrays with moderate values.",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < n; i++) {\n    main[i] = data.consumeInt(-7, 7);\n}\nfor (int i = 0; i < n - 1; i++) {\n    secondary[i] = data.consumeInt(-4, 4);\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\norg.apache.commons.math.linear.RealMatrix d;\ndouble[] re = new double[n];\ndouble[] im = new double[n];\ntry {\n    ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, 0.0);\n    d = ed.getD();\n    for (int i = 0; i < n; i++) {\n        re[i] = ed.getRealEigenvalue(i);\n        im[i] = ed.getImagEigenvalue(i);\n    }\n} catch (Exception e) {\n    throw new RuntimeException(\"relation symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_D violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nfor (int i = 0; i < n; i++) {\n    double tolIm = 1e-9 * Math.max(1.0, Math.abs(im[i]));\n    if (Math.abs(im[i]) > tolIm) {\n        throw new RuntimeException(\"relation symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_D violated: imag[\" + i + \"]=\" + im[i]);\n    }\n    double ddiag = d.getEntry(i, i);\n    double tolDiag = 1e-9 * Math.max(1.0, Math.max(Math.abs(ddiag), Math.abs(re[i])));\n    if (Math.abs(ddiag - re[i]) > tolDiag) {\n        throw new RuntimeException(\"relation symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_D violated: D(\" + i + \",\" + i + \")=\" + ddiag + \" real=\" + re[i]);\n    }\n    for (int j = 0; j < n; j++) {\n        if (i != j) {\n            double off = d.getEntry(i, j);\n            double tolOff = 1e-9 * Math.max(1.0, Math.abs(off));\n            if (Math.abs(off) > tolOff) {\n                throw new RuntimeException(\"relation symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_D violated: off-diagonal D(\" + i + \",\" + j + \")=\" + off);\n            }\n        }\n    }\n}"
  },
  {
    "name": "v_d_vt_reconstructs_original_tridiagonal",
    "kind": "metamorphic",
    "contract": "An eigen-decomposition exposes matrices V, D, and VT representing the original matrix as V * D * VT.",
    "input": "Choose n in [1, 5]. Build a finite symmetric tridiagonal matrix from main and secondary arrays. Also build that original matrix explicitly through the public matrix API.",
    "check": "int n = data.consumeInt(1, 5);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\norg.apache.commons.math.linear.Array2DRowRealMatrix original;\ntry {\n    for (int i = 0; i < n; i++) {\n        main[i] = data.consumeInt(-6, 6);\n    }\n    for (int i = 0; i < n - 1; i++) {\n        secondary[i] = data.consumeInt(-3, 3);\n    }\n    double[][] a = new double[n][n];\n    for (int i = 0; i < n; i++) {\n        a[i][i] = main[i];\n    }\n    for (int i = 0; i < n - 1; i++) {\n        a[i][i + 1] = secondary[i];\n        a[i + 1][i] = secondary[i];\n    }\n    original = new org.apache.commons.math.linear.Array2DRowRealMatrix(a);\n} catch (Exception e) {\n    return;\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\norg.apache.commons.math.linear.RealMatrix v;\norg.apache.commons.math.linear.RealMatrix d;\norg.apache.commons.math.linear.RealMatrix vt;\ntry {\n    ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, 0.0);\n    v = ed.getV();\n    d = ed.getD();\n    vt = ed.getVT();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation v_d_vt_reconstructs_original_tridiagonal violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.RealMatrix rebuilt;\ntry {\n    rebuilt = v.multiply(d).multiply(vt);\n} catch (Exception e) {\n    return;\n}\nfor (int i = 0; i < n; i++) {\n    for (int j = 0; j < n; j++) {\n        double aij = original.getEntry(i, j);\n        double bij = rebuilt.getEntry(i, j);\n        double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(aij), Math.abs(bij)));\n        if (Math.abs(aij - bij) > tol) {\n            throw new RuntimeException(\"relation v_d_vt_reconstructs_original_tridiagonal violated: entry (\" + i + \",\" + j + \") \" + aij + \" vs \" + bij);\n        }\n    }\n}"
  },
  {
    "name": "v_and_vt_are_transposes",
    "kind": "metamorphic",
    "contract": "The API exposes both V and VT, where VT is the transpose of V.",
    "input": "Choose n in [1, 6]. Build any finite symmetric tridiagonal matrix from moderate main and secondary values.",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[Math.max(0, n - 1)];\nfor (int i = 0; i < n; i++) {\n    main[i] = data.consumeInt(-8, 8);\n}\nfor (int i = 0; i < n - 1; i++) {\n    secondary[i] = data.consumeInt(-4, 4);\n}\norg.apache.commons.math.linear.EigenDecompositionImpl ed;\norg.apache.commons.math.linear.RealMatrix v;\norg.apache.commons.math.linear.RealMatrix vt;\ntry {\n    ed = new org.apache.commons.math.linear.EigenDecompositionImpl(main, secondary, 0.0);\n    v = ed.getV();\n    vt = ed.getVT();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation v_and_vt_are_transposes violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nfor (int i = 0; i < n; i++) {\n    for (int j = 0; j < n; j++) {\n        double a = v.getEntry(i, j);\n        double b = vt.getEntry(j, i);\n        double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));\n        if (Math.abs(a - b) > tol) {\n            throw new RuntimeException(\"relation v_and_vt_are_transposes violated: V(\" + i + \",\" + j + \")=\" + a + \" but VT(\" + j + \",\" + i + \")=\" + b);\n        }\n    }\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `reverse_tridiagonal_preserves_eigenvalues_when_last_dominate`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [6] ⚙️ screen-fuzz-buggy · `reverse_tridiagonal_preserves_eigenvalues_when_last_dominate`
**output:** 
```json
{
  "checked": 20000,
  "violated": 1033,
  "ratio": 0.0517
}
```

---
## [7] ⚙️ screen · `determinant_matches_product_of_real_eigenvalues`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [8] ⚙️ screen-fuzz-buggy · `determinant_matches_product_of_real_eigenvalues`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen · `symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [10] ⚙️ screen-fuzz-buggy · `symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [11] ⚙️ screen · `v_d_vt_reconstructs_original_tridiagonal`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [12] ⚙️ screen-fuzz-buggy · `v_d_vt_reconstructs_original_tridiagonal`
**output:** 
```json
{
  "checked": 20000,
  "violated": 90,
  "ratio": 0.0045
}
```

---
## [13] ⚙️ screen · `v_and_vt_are_transposes`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [14] ⚙️ screen-fuzz-buggy · `v_and_vt_are_transposes`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [15] ⚙️ screen · `reverse_tridiagonal_preserves_eigenvalues_when_last_dominate`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [16] ⚙️ screen · `v_d_vt_reconstructs_original_tridiagonal`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [17] ⚙️ screen · `determinant_matches_product_of_real_eigenvalues`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [18] ⚙️ screen · `symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [19] ⚙️ screen · `v_and_vt_are_transposes`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [20] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "reverse_tridiagonal_preserves_eigenvalues_when_last_dominate",
    "v_d_vt_reconstructs_original_tridiagonal",
    "determinant_matches_product_of_real_eigenvalues",
    "symmetric_tridiagonal_has_zero_imaginary_parts_and_diagonal_",
    "v_and_vt_are_transposes"
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
<details open><summary>▸ Output (~10,723 chars)</summary>

```
[
  {
    "name": "diagonal-spectrum-multiset-under-flip-trigger",
    "kind": "invariant",
    "contract": "For a real diagonal matrix, the eigenvalues are exactly its diagonal entries. EigenDecompositionImpl decomposes a symmetric tridiagonal matrix given by main/secondary diagonals, so when secondary is all zeros the returned real eigenvalues must be exactly the main diagonal values (up to permutation).",
    "input": "Choose n >= 2. Build a diagonal symmetric tridiagonal matrix by taking a strictly ascending positive main diagonal and an all-zero secondary diagonal, with the extra boundary-shaping condition 1.5 * main[0] < main[n-1] so the patched flip condition is exercised. This is valid by construction for the tridiagonal-array constructor.",
    "check": "int n = data.consumeInt(2, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < secondary.length; i++) {\n  secondary[i] = 0.0;\n}\ndouble first = 1.0 + data.consumeInt(0, 20);\nmain[0] = first;\nfor (int i = 1; i < n; i++) {\n  main[i] = main[i - 1] + 1.0 + data.consumeInt(0, 3);\n}\nif (!(1.5 * main[0] < main[n - 1])) {\n  main[n - 1] = Math.floor(1.5 * main[0]) + 2.0;\n  for (int i = 1; i < n - 1; i++) {\n    if (main[i] >= main[i + 1]) {\n      main[i + 1] = main[i] + 1.0;\n    }\n  }\n}\nEigenDecompositionImpl ed;\ntry {\n  ed = new EigenDecompositionImpl(main, secondary, 0.0);\n} catch (Exception e) {\n  return;\n}\ndouble[] got;\ntry {\n  got = ed.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation diagonal-spectrum-multiset-under-flip-trigger violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ndouble[] expected = main.clone();\ndouble[] actual = got.clone();\njava.util.Arrays.sort(expected);\njava.util.Arrays.sort(actual);\nif (expected.length != actual.length) {\n  throw new RuntimeException(\"relation diagonal-spectrum-multiset-under-flip-trigger violated: length \" + actual.length + \" vs \" + expected.length);\n}\nfor (int i = 0; i < expected.length; i++) {\n  double a = expected[i];\n  double b = actual[i];\n  double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));\n  if (Math.abs(a - b) > tol) {\n    throw new RuntimeException(\"relation diagonal-spectrum-multiset-under-flip-trigger violated at sorted index \" + i + \": expected \" + a + \" but got \" + b);\n  }\n}"
  },
  {
    "name": "imaginary-parts-zero-for-symmetric-tridiagonal",
    "kind": "invariant",
    "contract": "A real symmetric matrix has only real eigenvalues, so the imaginary part returned for every eigenvalue must be 0.",
    "input": "Choose n >= 1. Build any finite real symmetric tridiagonal matrix through main and secondary arrays of lengths n and n-1 respectively, with moderate magnitudes to avoid overflow/NaN. This is valid input to the constructor.",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-20, 20) / 3.0;\n}\nfor (int i = 0; i < n - 1; i++) {\n  secondary[i] = data.consumeInt(-10, 10) / 4.0;\n}\nEigenDecompositionImpl ed;\ntry {\n  ed = new EigenDecompositionImpl(main, secondary, 0.0);\n} catch (Exception e) {\n  return;\n}\nfor (int i = 0; i < n; i++) {\n  double im;\n  try {\n    im = ed.getImagEigenvalue(i);\n  } catch (Exception e) {\n    throw new RuntimeException(\"relation imaginary-parts-zero-for-symmetric-tridiagonal violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input at index \" + i + \": \" + e.getMessage());\n  }\n  double tol = 1e-9 * Math.max(1.0, Math.abs(im));\n  if (Math.abs(im) > tol) {\n    throw new RuntimeException(\"relation imaginary-parts-zero-for-symmetric-tridiagonal violated at index \" + i + \": imag=\" + im);\n  }\n}"
  },
  {
    "name": "determinant-equals-product-of-eigenvalues",
    "kind": "metamorphic",
    "contract": "The determinant of a matrix equals the product of its eigenvalues; for this symmetric real decomposition the determinant returned by getDeterminant must agree with the product of the returned real eigenvalues.",
    "input": "Choose a small finite symmetric tridiagonal matrix from main and secondary arrays with moderate entries so all computations remain finite. This is valid by construction.",
    "check": "int n = data.consumeInt(1, 5);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-9, 9) / 2.0;\n}\nfor (int i = 0; i < n - 1; i++) {\n  secondary[i] = data.consumeInt(-6, 6) / 3.0;\n}\nEigenDecompositionImpl ed;\ntry {\n  ed = new EigenDecompositionImpl(main, secondary, 0.0);\n} catch (Exception e) {\n  return;\n}\ndouble det;\ndouble[] vals;\ntry {\n  det = ed.getDeterminant();\n  vals = ed.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation determinant-equals-product-of-eigenvalues violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ndouble prod = 1.0;\nfor (int i = 0; i < vals.length; i++) {\n  prod *= vals[i];\n}\ndouble tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(det), Math.abs(prod)));\nif (Math.abs(det - prod) > tol) {\n  throw new RuntimeException(\"relation determinant-equals-product-of-eigenvalues violated: determinant=\" + det + \" product=\" + prod);\n}\n"
  },
  {
    "name": "reconstructs-original-matrix-as-vdvT",
    "kind": "invariant",
    "contract": "For the eigen decomposition of a real symmetric matrix, the returned matrices satisfy A = V D V^T.",
    "input": "Choose n >= 1 and build any finite real symmetric tridiagonal matrix using main and secondary arrays with moderate values. This is valid constructor input.",
    "check": "int n = data.consumeInt(1, 5);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-12, 12) / 2.0;\n}\nfor (int i = 0; i < n - 1; i++) {\n  secondary[i] = data.consumeInt(-8, 8) / 4.0;\n}\nEigenDecompositionImpl ed;\ntry {\n  ed = new EigenDecompositionImpl(main, secondary, 0.0);\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.RealMatrix v;\norg.apache.commons.math.linear.RealMatrix d;\norg.apache.commons.math.linear.RealMatrix vt;\ntry {\n  v = ed.getV();\n  d = ed.getD();\n  vt = ed.getVT();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation reconstructs-original-matrix-as-vdvT violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.RealMatrix recon;\ntry {\n  recon = v.multiply(d).multiply(vt);\n} catch (Exception e) {\n  return;\n}\nfor (int r = 0; r < n; r++) {\n  for (int c = 0; c < n; c++) {\n    double expected;\n    if (r == c) {\n      expected = main[r];\n    } else if (Math.abs(r - c) == 1) {\n      expected = secondary[Math.min(r, c)];\n    } else {\n      expected = 0.0;\n    }\n    double actual = recon.getEntry(r, c);\n    double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(expected), Math.abs(actual)));\n    if (Math.abs(expected - actual) > tol) {\n      throw new RuntimeException(\"relation reconstructs-original-matrix-as-vdvT violated at (\" + r + \",\" + c + \"): expected=\" + expected + \" actual=\" + actual);\n    }\n  }\n}"
  },
  {
    "name": "real-eigenvalue-accessors-agree",
    "kind": "metamorphic",
    "contract": "The indexed accessor getRealEigenvalue(i) and the bulk accessor getRealEigenvalues()[i] expose the same real eigenvalues of the decomposition.",
    "input": "Choose any valid finite symmetric tridiagonal matrix through main and secondary arrays with moderate magnitudes.",
    "check": "int n = data.consumeInt(1, 6);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-15, 15) / 3.0;\n}\nfor (int i = 0; i < n - 1; i++) {\n  secondary[i] = data.consumeInt(-9, 9) / 3.0;\n}\nEigenDecompositionImpl ed;\ntry {\n  ed = new EigenDecompositionImpl(main, secondary, 0.0);\n} catch (Exception e) {\n  return;\n}\ndouble[] all;\ntry {\n  all = ed.getRealEigenvalues();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation real-eigenvalue-accessors-agree violated: unexpected \" + e.getClass().getName() + \" from bulk accessor on valid-by-construction input: \" + e.getMessage());\n}\nfor (int i = 0; i < all.length; i++) {\n  double one;\n  try {\n    one = ed.getRealEigenvalue(i);\n  } catch (Exception e) {\n    throw new RuntimeException(\"relation real-eigenvalue-accessors-agree violated: unexpected \" + e.getClass().getName() + \" from indexed accessor at index \" + i + \" on valid-by-construction input: \" + e.getMessage());\n  }\n  double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(one), Math.abs(all[i])));\n  if (Math.abs(one - all[i]) > tol) {\n    throw new RuntimeException(\"relation real-eigenvalue-accessors-agree violated at index \" + i + \": indexed=\" + one + \" bulk=\" + all[i]);\n  }\n}"
  },
  {
    "name": "vt-is-transpose-of-v",
    "kind": "metamorphic",
    "contract": "For a real symmetric eigen decomposition, the returned VT matrix is V transposed.",
    "input": "Choose any valid finite symmetric tridiagonal matrix through main and secondary arrays with moderate magnitudes.",
    "check": "int n = data.consumeInt(1, 5);\ndouble[] main = new double[n];\ndouble[] secondary = new double[n - 1];\nfor (int i = 0; i < n; i++) {\n  main[i] = data.consumeInt(-10, 10) / 2.0;\n}\nfor (int i = 0; i < n - 1; i++) {\n  secondary[i] = data.consumeInt(-8, 8) / 4.0;\n}\nEigenDecompositionImpl ed;\ntry {\n  ed = new EigenDecompositionImpl(main, secondary, 0.0);\n} catch (Exception e) {\n  return;\n}\norg.apache.commons.math.linear.RealMatrix v;\norg.apache.commons.math.linear.RealMatrix vt;\ntry {\n  v = ed.getV();\n  vt = ed.getVT();\n} catch (Exception e) {\n  throw new RuntimeException(\"relation vt-is-transpose-of-v violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\norg.apache.commons.math.linear.RealMatrix vT;\ntry {\n  vT = v.transpose();\n} catch (Exception e) {\n  return;\n}\nfor (int r = 0; r < n; r++) {\n  for (int c = 0; c < n; c++) {\n    double a = vt.getEntry(r, c);\n    double b = vT.getEntry(r, c);\n    double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));\n    if (Math.abs(a - b) > tol) {\n      throw new RuntimeException(\"relation vt-is-transpose-of-v violated at (\" + r + \",\" + c + \"): getVT=\" + a + \" transpose(getV)=\" + b);\n    }\n  }\n}"
  }
]
```

</details>

---
## [22] ⚙️ screen · `diagonal-spectrum-multiset-under-flip-trigger`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [23] ⚙️ screen-fuzz-buggy · `diagonal-spectrum-multiset-under-flip-trigger`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [24] ⚙️ screen · `imaginary-parts-zero-for-symmetric-tridiagonal`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [25] ⚙️ screen-fuzz-buggy · `imaginary-parts-zero-for-symmetric-tridiagonal`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [26] ⚙️ screen · `determinant-equals-product-of-eigenvalues`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [27] ⚙️ screen-fuzz-buggy · `determinant-equals-product-of-eigenvalues`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [28] ⚙️ screen · `reconstructs-original-matrix-as-vdvT`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [29] ⚙️ screen-fuzz-buggy · `reconstructs-original-matrix-as-vdvT`
**output:** 
```json
{
  "checked": 20000,
  "violated": 590,
  "ratio": 0.0295
}
```

---
## [30] ⚙️ screen · `real-eigenvalue-accessors-agree`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [31] ⚙️ screen-fuzz-buggy · `real-eigenvalue-accessors-agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [32] ⚙️ screen · `vt-is-transpose-of-v`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [33] ⚙️ screen-fuzz-buggy · `vt-is-transpose-of-v`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [34] ⚙️ screen · `reconstructs-original-matrix-as-vdvT`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [35] ⚙️ screen · `diagonal-spectrum-multiset-under-flip-trigger`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [36] ⚙️ screen · `imaginary-parts-zero-for-symmetric-tridiagonal`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [37] ⚙️ screen · `determinant-equals-product-of-eigenvalues`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [38] ⚙️ screen · `real-eigenvalue-accessors-agree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [39] ⚙️ screen · `vt-is-transpose-of-v`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [40] ⚙️ reference-widening · `enumerate`
**output:** **3 observable(s) to widen onto**
<details><summary>▸ reason (~459 chars)</summary>

- reason: the kept checks probe ['getv', 'getd', 'getvt', 'realeigenvalues', 'imageigenvalues', 'determinant'] among the patched class's no-argument readers; 6 of those have no admitted reference and 3 fit under the per-leg cap of 3 (the leg already holds []); 6 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['realeigenvalue', 'message', 'invalidmatrixexception', 'imageigenvalue', 'eigenvector', 'symmetric'])

</details>
<details><summary>▸ detail (~1,511 chars)</summary>

- detail: {'targets': ['getv', 'getd', 'getvt'], 'already_admitted': [], 'cap': 3, 'checks_read': 11, 'excluded': [{'key': 'realeigenvalue', 'why': "the patched class declares `getRealEigenvalue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'message', 'why': 'the patched class does not declare `getMessage()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'invalidmatrixexception', 'why': "the patched class declares `InvalidMatrixException` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'imageigenvalue', 'why': "the patched class declares `getImagEigenvalue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'eigenvector', 'why': "the patched class declares `getEigenvector` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'symmetric', 'why': "the patched class declares `isSymmetric` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}], 'excluded_count': 6}

</details>

---
## [41] ⚙️ reference-widening · `getV`
**output:** **reference REQUESTED for observable `getv`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [42] ⚙️ reference-impl · `getV`
**output:** **widening target requested**
- detail: {'candidates': ['getV'], 'attempting': 'getV'}

---
## [43] ⚙️ reference-impl · `getV`
**output:** **screening surface resolved**
- reason: 3 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'patch_touched_exempted': [], 'screened_siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'declaring_types': ['EigenDecompositionImpl']}

---
## [44] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~59,482 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
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
<details open><summary>▸ Output (~1,328 chars)</summary>

```
// compute(various state parameters) : getV, getDeterminant, getImagEigenvalues, getRealEigenvalues
public class ReferenceImpl {

    public static RealMatrix compute_getV(final ArrayRealVector[] eigenvectors,
                                          final RealMatrix cachedV) {
        if (cachedV != null) {
            return cachedV;
        }

        final int m = eigenvectors.length;
        final double[][] data = new double[m][m];
        for (int k = 0; k < m; ++k) {
            final ArrayRealVector eigenvector = eigenvectors[k];
            for (int i = 0; i < m; ++i) {
                data[i][k] = eigenvector.getEntry(i);
            }
        }

        return MatrixUtils.createRealMatrix(data);
    }

    public static double[] compute_getRealEigenvalues(final double[] realEigenvalues) {
        return realEigenvalues == null ? null : realEigenvalues.clone();
    }

    public static double[] compute_getImagEigenvalues(final double[] imagEigenvalues) {
        return imagEigenvalues == null ? null : imagEigenvalues.clone();
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
## [45] ⚙️ reference-impl · `getV`
**output:** **reference generated**
- detail: {'chars': 1328}

---
## [46] ⚙️ reference-impl · `getV`
**output:** **signature declared**
- detail: {'signature': 'various state parameters'}

---
## [47] ⚙️ reference-impl · `getV`
**output:** **reference observables matched**
- detail: {'matched': {'getV': 'getV', 'getDeterminant': 'getDeterminant', 'getImagEigenvalues': 'getImagEigenvalues', 'getRealEigenvalues': 'getRealEigenvalues'}, 'declared_only': []}

---
## [48] ⚙️ reference-impl · `getV`
**output:** **screen bar reachable**
- reason: 3 shared sibling observable(s) — enough for the screen to decide

---
## [49] ⚙️ reference-impl · `getV`
**output:** **signature unmappable — DISCARDED**
- reason: parameter `various state parameters` matches no canonical state field. Fields read by the method: []; all fields (47): ['TOLERANCE', 'TOLERANCE_2', 'splitTolerance', 'main', 'secondary', 'squaredSecondary', 'transformer', 'lowerSpectra', 'upperSpectra', 'minPivot', 'sigma', 'sigmaLow']… — signature unmappable, DISCARDED

---
## [50] ⚙️ reference-widening · `getV`
**output:** **widening REJECTED for observable `getv`**
- reason: signature unmappable — DISCARDED — parameter `various state parameters` matches no canonical state field. Fields read by the method: []; all fields (47): ['TOLERANCE', 'TOLERANCE_2', 'splitTolerance', 'main', 'secondary', 'squaredSecondary', 'transformer', 'lowerSpectra', 'upperSpectra', 'minPivot', 'sigma', 'sigmaLow']… — signature unmappable, DISCARDED
- detail: {'admitted_observables': [], 'last_step': 'signature unmappable — DISCARDED'}

---
## [51] ⚙️ reference-widening · `getD`
**output:** **reference REQUESTED for observable `getd`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [52] ⚙️ reference-impl · `getD`
**output:** **widening target requested**
- detail: {'candidates': ['getD'], 'attempting': 'getD'}

---
## [53] ⚙️ reference-impl · `getD`
**output:** **screening surface resolved**
- reason: 3 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'patch_touched_exempted': [], 'screened_siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'declaring_types': ['EigenDecompositionImpl']}

---
## [54] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~59,196 chars, 1 new)</summary>

- *[system] message: identical to step [44] — not reprinted*
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
<details open><summary>▸ Output (~2,343 chars)</summary>

```
// compute(double[], double[]) : getD, getDeterminant, getImagEigenvalues, getRealEigenvalues
public class ReferenceImpl {

    public static RealMatrix compute_getD(final double[] realEigenvalues,
                                          final double[] imagEigenvalues) {
        final int n = realEigenvalues.length;
        if (imagEigenvalues != null && imagEigenvalues.length != n) {
            throw new IllegalArgumentException("dimension mismatch");
        }
        return MatrixUtils.createRealDiagonalMatrix(realEigenvalues.clone());
    }

    public static double compute_getDeterminant(final double[] realEigenvalues,
                                                final double[] imagEigenvalues) {
        final int n = realEigenvalues.length;
        if (imagEigenvalues != null && imagEigenvalues.length != n) {
            throw new IllegalArgumentException("dimension mismatch");
        }

        double determinant = 1.0;
        if (imagEigenvalues == null) {
            for (int i = 0; i < n; ++i) {
                determinant *= realEigenvalues[i];
            }
        } else {
            for (int i = 0; i < n; ++i) {
                final double im = imagEigenvalues[i];
                if (im == 0.0) {
                    determinant *= realEigenvalues[i];
                } else {
                    determinant *= (realEigenvalues[i] * realEigenvalues[i]) + (im * im);
                    ++i;
                }
            }
        }
        return determinant;
    }

    public static double[] compute_getImagEigenvalues(final double[] realEigenvalues,
                                                      final double[] imagEigenvalues) {
        if (imagEigenvalues == null) {
            return null;
        }
        if (realEigenvalues.length != imagEigenvalues.length) {
            throw new IllegalArgumentException("dimension mismatch");
        }
        return imagEigenvalues.clone();
    }

    public static double[] compute_getRealEigenvalues(final double[] realEigenvalues,
                                                      final double[] imagEigenvalues) {
        if (imagEigenvalues != null && realEigenvalues.length != imagEigenvalues.length) {
            throw new IllegalArgumentException("dimension mismatch");
        }
        return realEigenvalues.clone();
    }
}
```

</details>

---
## [55] ⚙️ reference-impl · `getD`
**output:** **reference generated**
- detail: {'chars': 2343}

---
## [56] ⚙️ reference-impl · `getD`
**output:** **signature declared**
- detail: {'signature': 'double[], double[]'}

---
## [57] ⚙️ reference-impl · `getD`
**output:** **parameter names recovered from the declarations**
- reason: the `// compute(...)` line is bare types; every compute_* declaration agrees on one named list
- detail: {'declared': 'double[], double[]', 'named': 'double[] realEigenvalues, double[] imagEigenvalues'}

---
## [58] ⚙️ reference-impl · `getD`
**output:** **reference observables matched**
- detail: {'matched': {'getD': 'getD', 'getDeterminant': 'getDeterminant', 'getImagEigenvalues': 'getImagEigenvalues', 'getRealEigenvalues': 'getRealEigenvalues'}, 'declared_only': []}

---
## [59] ⚙️ reference-impl · `getD`
**output:** **screen bar reachable**
- reason: 3 shared sibling observable(s) — enough for the screen to decide

---
## [60] ⚙️ reference-impl · `getD`
**output:** **signature mapped**
- reason: matched 2 parameter(s) to state fields

---
## [61] ⚙️ reference-impl · `getD`
**output:** **twin underivable — DISCARDED**
- reason: no receiver with type evidence: no variable both reaches the disputed observable and is declared with a type that declares it (declaring types: ['EigenDecompositionImpl']) — DISCARDED rather than guessed
- detail: {'declaring_types': ['EigenDecompositionImpl']}

---
## [62] ⚙️ reference-widening · `getD`
**output:** **widening REJECTED for observable `getd`**
- reason: twin underivable — DISCARDED — no receiver with type evidence: no variable both reaches the disputed observable and is declared with a type that declares it (declaring types: ['EigenDecompositionImpl']) — DISCARDED rather than guessed
- detail: {'admitted_observables': [], 'last_step': 'twin underivable — DISCARDED'}

---
## [63] ⚙️ reference-widening · `getVT`
**output:** **reference REQUESTED for observable `getvt`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [64] ⚙️ reference-impl · `getVT`
**output:** **widening target requested**
- detail: {'candidates': ['getVT'], 'attempting': 'getVT'}

---
## [65] ⚙️ reference-impl · `getVT`
**output:** **screening surface resolved**
- reason: 3 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'patch_touched_exempted': [], 'screened_siblings': ['getDeterminant', 'getImagEigenvalues', 'getRealEigenvalues'], 'declaring_types': ['EigenDecomposition', 'EigenDecompositionImpl']}

---
## [66] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~59,197 chars, 1 new)</summary>

- *[system] message: identical to step [44] — not reprinted*
**[user]**
```
Implement `getVT` from its specification.

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
<details open><summary>▸ Output (~1,656 chars)</summary>

```
// compute(double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors) : getVT, getDeterminant, getImagEigenvalues, getRealEigenvalues
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealMatrix;

public class ReferenceImpl {

    public static RealMatrix compute_getVT(double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors) {
        if (eigenvectors == null) {
            return null;
        }
        final int m = eigenvectors.length;
        final double[][] data = new double[m][m];
        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < m; ++j) {
                data[i][j] = eigenvectors[i].getEntry(j);
            }
        }
        return MatrixUtils.createRealMatrix(data);
    }

    public static double compute_getDeterminant(double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors) {
        double determinant = 1.0;
        for (int i = 0; i < realEigenvalues.length; ++i) {
            determinant *= realEigenvalues[i];
        }
        return determinant;
    }

    public static double[] compute_getImagEigenvalues(double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors) {
        if (imagEigenvalues != null) {
            return imagEigenvalues.clone();
        }
        return new double[realEigenvalues.length];
    }

    public static double[] compute_getRealEigenvalues(double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors) {
        return realEigenvalues.clone();
    }
}
```

</details>

---
## [67] ⚙️ reference-impl · `getVT`
**output:** **reference generated**
- detail: {'chars': 1656}

---
## [68] ⚙️ reference-impl · `getVT`
**output:** **signature declared**
- detail: {'signature': 'double[] realEigenvalues, double[] imagEigenvalues, ArrayRealVector[] eigenvectors'}

---
## [69] ⚙️ reference-impl · `getVT`
**output:** **reference observables matched**
- detail: {'matched': {'getVT': 'getVT', 'getDeterminant': 'getDeterminant', 'getImagEigenvalues': 'getImagEigenvalues', 'getRealEigenvalues': 'getRealEigenvalues'}, 'declared_only': []}

---
## [70] ⚙️ reference-impl · `getVT`
**output:** **screen bar reachable**
- reason: 3 shared sibling observable(s) — enough for the screen to decide

---
## [71] ⚙️ reference-impl · `getVT`
**output:** **signature mapped**
- reason: matched 3 parameter(s) to state fields

---
## [72] ⚙️ reference-impl · `getVT`
**output:** **twin setup extracted**
- reason: setup extracted; receiver `decomposition`
- detail: {'declaring_types': ['EigenDecomposition', 'EigenDecompositionImpl']}

---
## [73] ⚙️ reference-impl · `getVT`
**output:** **twin built**
- detail: {'helpers': 0, 'imports': 9, 'package': 'org.apache.commons.math.linear', 'receiver': 'decomposition'}

---
## [74] ⚙️ reference-impl · `getVT`
**output:** **buggy twin ran**
- reason: twin ran; 8 key(s)

---
## [75] ⚙️ reference-impl · `getVT`
**output:** **reference DISCARDED**
<details><summary>▸ reason (~1,246 chars)</summary>

- reason: driver did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: '}' expected | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: ';' expected | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement

</details>

---
## [76] ⚙️ reference-widening · `getVT`
**output:** **widening REJECTED for observable `getvt`**
<details><summary>▸ reason (~1,268 chars)</summary>

- reason: reference DISCARDED — driver did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: '}' expected | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: ';' expected | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/reference/ReferenceDriver.java:21: error: not a statement

</details>
- detail: {'admitted_observables': [], 'last_step': 'reference DISCARDED'}

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~3,469 chars)</summary>

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

Codebase: `Math_80_buggy`. Write a fuzz harness for the function(s) shown below.

Call them through the library's real public API, with inputs built from the FuzzedDataProvider, and exercise as much of each function's behaviour as you can: vary lengths, sizes, signs and contents, cover empty and boundary values, and try the argument combinations the signature allows. Construct whatever real library objects the call needs. A throwable that escapes fuzzerTestOneInput is reported as a finding, so let exceptions from the code under test propagate.

Function `flipIfWarranted` — declared in `org.apache.commons.math.linear.EigenDecompositionImpl`:
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
<details open><summary>▸ Output (~5,165 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];

        for (int i = 0; i < main.length; i++) {
            int kind = data.consumeInt(0, 11);
            switch (kind) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 5:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 6:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    main[i] = Double.NaN;
                    break;
                case 9:
                    main[i] = (double) data.consumeInt();
                    break;
                case 10: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    main[i] = (double) num / (den == 0 ? 1 : den);
                    break;
                }
                default: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
                }
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int kind = data.consumeInt(0, 11);
            switch (kind) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 5:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 6:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    secondary[i] = Double.NaN;
                    break;
                case 9:
                    secondary[i] = (double) data.consumeInt();
                    break;
                case 10: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    secondary[i] = (double) num / (den == 0 ? 1 : den);
                    break;
                }
                default: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
                }
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 6:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 7:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                splitTolerance = Double.NaN;
                break;
            default: {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
            }
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getRealEigenvalues();
        for (int i = 0; i < n; i++) {
            ed.getRealEigenvalue(i);
            ed.getImagEigenvalue(i);
            ed.getEigenvector(i);
        }
        ed.getImagEigenvalues();
        ed.getDeterminant();
        ed.getD();
        ed.getV();
        ed.getVT();
        ed.getSolver();
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.InvalidMatrixException:

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,931 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        boolean useValidSecondaryLength = data.consumeBoolean();
        int secondaryLength;
        if (useValidSecondaryLength) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else {
            secondaryLength = data.consumeInt(0, 32);
        }

        double[] main = new double[n];
        double[] secondary = new double[secondaryLength];

        for (int i = 0; i < main.length; i++) {
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (a & 7) {
                case 0:
                    main[i] = a;
                    break;
                case 1:
                    main[i] = -a;
                    break;
                case 2:
                    main[i] = b == 0 ? a : ((double) a) / b;
                    break;
                case 3:
                    main[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 4:
                    main[i] = 0.0;
                    break;
                case 5:
                    main[i] = -0.0;
                    break;
                case 6:
                    main[i] = (double) (byte) a;
                    break;
                default:
                    main[i] = (double) a * (double) b;
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (b & 7) {
                case 0:
                    secondary[i] = a;
                    break;
                case 1:
                    secondary[i] = -a;
                    break;
                case 2:
                    secondary[i] = a == 0 ? b : ((double) b) / a;
                    break;
                case 3:
                    secondary[i] = Double.longBitsToDouble((((long) b) << 32) ^ (a & 0xffffffffL));
                    break;
                case 4:
                    secondary[i] = 0.0;
                    break;
                case 5:
                    secondary[i] = -0.0;
                    break;
                case 6:
                    secondary[i] = (double) (byte) b;
                    break;
                default:
                    secondary[i] = (double) a * (double) b;
                    break;
            }
        }

        int sa = data.consumeInt();
        int sb = data.consumeInt();
        double splitTolerance;
        switch (sa & 7) {
            case 0:
                splitTolerance = sa;
                break;
            case 1:
                splitTolerance = -sa;
                break;
            case 2:
                splitTolerance = sb == 0 ? sa : ((double) sa) / sb;
                break;
            case 3:
                splitTolerance = Double.longBitsToDouble((((long) sa) << 32) ^ (sb & 0xffffffffL));
                break;
            case 4:
                splitTolerance = 0.0;
                break;
            case 5:
                splitTolerance = -0.0;
                break;
            case 6:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                splitTolerance = Double.NaN;
                break;
        }

        if (data.consumeBoolean()) {
            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getVT();
            ed.getV();
            ed.getD();

            if (n > 0) {
                int index = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(index);
                ed.getImagEigenvalue(index);
                ed.getEigenvector(index);

                double[] rhs = new double[n];
                for (int i = 0; i < rhs.length; i++) {
                    int x = data.consumeInt();
                    int y = data.consumeInt();
                    rhs[i] = ((x & 1) == 0) ? x : Double.longBitsToDouble((((long) x) << 32) ^ (y & 0xffffffffL));
                }

                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
                solver.solve(rhs);
                solver.solve(new ArrayRealVector(rhs, true));

                double[][] rhsMatrix = new double[n][n == 0 ? 0 : data.consumeInt(1, Math.max(1, n))];
                for (int r = 0; r < rhsMatrix.length; r++) {
                    for (int c = 0; c < rhsMatrix[r].length; c++) {
                        int x = data.consumeInt();
                        rhsMatrix[r][c] = ((x & 3) == 0) ? 0.0 : x;
                    }
                }
                solver.solve(new Array2DRowRealMatrix(rhsMatrix, false));
            }
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                matrixData[i][i] = main[i];
            }
            int bound = Math.min(n - 1, secondary.length);
            for (int i = 0; i < bound; i++) {
                matrixData[i][i + 1] = secondary[i];
                matrixData[i + 1][i] = secondary[i];
            }
            if (data.consumeBoolean()) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 2; j < n; j++) {
                        int x = data.consumeInt();
                        double v = ((x & 7) == 0) ? Double.longBitsToDouble((((long) x) << 32) ^ (data.consumeInt() & 0xffffffffL)) : 0.0;
                        matrixData[i][j] = v;
                        matrixData[j][i] = v;
                    }
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);

            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getVT();
            ed.getV();
            ed.getD();

            if (n > 0) {
                int index = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(index);
                ed.getImagEigenvalue(index);
                ed.getEigenvector(index);

                double[] rhs = new double[n];
                for (int i = 0; i < rhs.length; i++) {
                    rhs[i] = data.consumeInt();
                }

                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
                solver.solve(rhs);
                solver.solve(new ArrayRealVector(rhs, true));
            }
        }
    }
}
```

</details>

---
## [80] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathRuntimeException$4: matrix must have at least one row

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,064 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = data.consumeInt();
                break;
            case 3:
                splitTolerance = data.consumeInt() / (double) data.consumeInt(1, 1024);
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            default:
                splitTolerance = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                break;
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int choice = data.consumeInt(0, 9);
            int raw = data.consumeInt();
            switch (choice) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = raw;
                    break;
                case 3:
                    main[i] = raw / (double) data.consumeInt(1, 4096);
                    break;
                case 4:
                    main[i] = Math.scalb((double) raw, data.consumeInt(-1074, 1023));
                    break;
                case 5:
                    main[i] = Double.NaN;
                    break;
                case 6:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    main[i] = (raw & 1) == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    break;
                default:
                    main[i] = (double) data.consumeByte();
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int choice = data.consumeInt(0, 9);
            int raw = data.consumeInt();
            switch (choice) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = raw;
                    break;
                case 3:
                    secondary[i] = raw / (double) data.consumeInt(1, 4096);
                    break;
                case 4:
                    secondary[i] = Math.scalb((double) raw, data.consumeInt(-1074, 1023));
                    break;
                case 5:
                    secondary[i] = Double.NaN;
                    break;
                case 6:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    secondary[i] = (double) data.consumeByte();
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                    break;
            }
        }

        boolean useMatrixPath = data.consumeBoolean();

        EigenDecompositionImpl eig;
        if (useMatrixPath) {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                matrixData[i][i] = main[i];
                if (i + 1 < n) {
                    double v = secondary[i];
                    matrixData[i][i + 1] = v;
                    matrixData[i + 1][i] = v;
                }
            }

            int extraEdits = data.consumeInt(0, n == 0 ? 0 : Math.min(n * n, 16));
            for (int e = 0; e < extraEdits; e++) {
                int r = data.consumeInt(0, n - 1);
                int c = data.consumeInt(0, n - 1);
                double v;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        v = data.consumeInt();
                        break;
                    case 1:
                        v = data.consumeInt() / (double) data.consumeInt(1, 256);
                        break;
                    case 2:
                        v = Double.NaN;
                        break;
                    case 3:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        v = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                        break;
                }
                matrixData[r][c] = v;
                matrixData[c][r] = v;
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            eig = new EigenDecompositionImpl(matrix, splitTolerance);
        } else {
            eig = new EigenDecompositionImpl(main, secondary, splitTolerance);
        }

        if (data.consumeBoolean()) {
            eig.getD();
        }
        if (data.consumeBoolean()) {
            eig.getV();
        }
        if (data.consumeBoolean()) {
            eig.getVT();
        }
        if (data.consumeBoolean()) {
            eig.getDeterminant();
        }

        int queries = n == 0 ? 0 : data.consumeInt(0, Math.min(n + 4, 20));
        for (int i = 0; i < queries; i++) {
            int index;
            if (data.consumeBoolean()) {
                index = data.consumeInt(-2, n + 1);
            } else {
                index = data.consumeInt(0, n - 1);
            }
            switch (data.consumeInt(0, 4)) {
                case 0:
                    eig.getRealEigenvalue(index);
                    break;
                case 1:
                    eig.getImagEigenvalue(index);
                    break;
                case 2:
                    eig.getEigenvector(index);
                    break;
                case 3:
                    eig.getSolver();
                    break;
                default:
                    eig.getDeterminant();
                    break;
            }
        }

        if (data.consumeBoolean()) {
            eig.getRealEigenvalues();
        }
        if (data.consumeBoolean()) {
            eig.getImagEigenvalues();
        }
    }
}
```

</details>

---
## [82] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,160 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean useMatrixPath = data.consumeBoolean();

        if (useMatrixPath) {
            int n = data.consumeInt(1, 8);
            double[][] m = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    switch (data.consumeInt(0, 9)) {
                        case 0:
                            v = 0.0d;
                            break;
                        case 1:
                            v = -0.0d;
                            break;
                        case 2:
                            v = 1.0d;
                            break;
                        case 3:
                            v = -1.0d;
                            break;
                        case 4:
                            v = Double.MIN_VALUE;
                            break;
                        case 5:
                            v = Double.MAX_VALUE;
                            break;
                        case 6:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        case 7:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                        case 8:
                            v = Double.NaN;
                            break;
                        default:
                            break;
                    }
                    m[i][j] = v;
                    m[j][i] = v;
                }
            }

            long splitBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double splitTolerance = Double.longBitsToDouble(splitBits);
            switch (data.consumeInt(0, 5)) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                default:
                    break;
            }

            RealMatrix matrix = new Array2DRowRealMatrix(m);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);
        } else {
            int n = data.consumeInt(1, 32);
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < n; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double v = Double.longBitsToDouble(bits);
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        v = 0.0d;
                        break;
                    case 1:
                        v = -0.0d;
                        break;
                    case 2:
                        v = 1.0d;
                        break;
                    case 3:
                        v = -1.0d;
                        break;
                    case 4:
                        v = Double.MIN_VALUE;
                        break;
                    case 5:
                        v = Double.MAX_VALUE;
                        break;
                    case 6:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 8:
                        v = Double.NaN;
                        break;
                    default:
                        break;
                }
                main[i] = v;
            }

            for (int i = 0; i < secondary.length; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double v = Double.longBitsToDouble(bits);
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        v = 0.0d;
                        break;
                    case 1:
                        v = -0.0d;
                        break;
                    case 2:
                        v = 1.0d;
                        break;
                    case 3:
                        v = -1.0d;
                        break;
                    case 4:
                        v = Double.MIN_VALUE;
                        break;
                    case 5:
                        v = Double.MAX_VALUE;
                        break;
                    case 6:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 8:
                        v = Double.NaN;
                        break;
                    default:
                        break;
                }
                secondary[i] = v;
            }

            long splitBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double splitTolerance = Double.longBitsToDouble(splitBits);
            switch (data.consumeInt(0, 5)) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                default:
                    break;
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);
        }
    }
}
```

</details>

---
## [84] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.InvalidMatrixException:

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,482 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen = data.consumeInt(0, 32);
        int secondaryLen;
        switch (Math.floorMod(data.consumeInt(), 5)) {
            case 0:
                secondaryLen = Math.max(0, mainLen - 1);
                break;
            case 1:
                secondaryLen = mainLen;
                break;
            case 2:
                secondaryLen = Math.max(0, mainLen - 2);
                break;
            case 3:
                secondaryLen = 0;
                break;
            default:
                secondaryLen = data.consumeInt(0, 32);
                break;
        }

        double[] main = new double[mainLen];
        double[] secondary = new double[secondaryLen];

        for (int i = 0; i < main.length; i++) {
            switch (Math.floorMod(data.consumeInt(), 12)) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
                    break;
                case 2:
                    main[i] = 1.0;
                    break;
                case 3:
                    main[i] = -1.0;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = data.consumeInt();
                    break;
                case 10:
                    main[i] = data.consumeInt(-1024, 1024) / (double) (data.consumeInt(1, 32));
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            switch (Math.floorMod(data.consumeInt(), 12)) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = 1.0;
                    break;
                case 3:
                    secondary[i] = -1.0;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = data.consumeInt();
                    break;
                case 10:
                    secondary[i] = data.consumeInt(-1024, 1024) / (double) (data.consumeInt(1, 32));
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        if (main.length > 0 && data.consumeBoolean()) {
            main[0] = data.consumeBoolean() ? -0.0 : 0.0;
        }
        if (main.length > 1 && data.consumeBoolean()) {
            main[main.length - 1] = data.consumeBoolean() ? Double.MAX_VALUE : Double.MIN_VALUE;
        }
        if (secondary.length > 0 && data.consumeBoolean()) {
            secondary[0] = data.consumeBoolean() ? 0.0 : -0.0;
        }
        if (secondary.length > 1 && data.consumeBoolean()) {
            secondary[secondary.length - 1] = data.consumeBoolean() ? Double.MAX_VALUE : Double.MIN_VALUE;
        }

        double splitTolerance;
        switch (Math.floorMod(data.consumeInt(), 8)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = 1.0;
                break;
            case 3:
                splitTolerance = -1.0;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 6:
                splitTolerance = Double.NaN;
                break;
            default:
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
        }

        EigenDecompositionImpl eig = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            eig.getRealEigenvalues();
        }
        if (data.consumeBoolean()) {
            eig.getImagEigenvalues();
        }
        if (data.consumeBoolean()) {
            eig.getD();
        }
        if (data.consumeBoolean()) {
            eig.getV();
        }
        if (data.consumeBoolean()) {
            eig.getVT();
        }
        if (data.consumeBoolean()) {
            eig.getDeterminant();
        }
        if (data.consumeBoolean()) {
            eig.getSolver();
        }

        int dimension = main.length;
        if (dimension > 0) {
            int index = data.consumeInt(0, dimension - 1);
            if (data.consumeBoolean()) {
                eig.getRealEigenvalue(index);
            }
            if (data.consumeBoolean()) {
                eig.getImagEigenvalue(index);
            }
            if (data.consumeBoolean()) {
                eig.getEigenvector(index);
            }
        }
    }
}
```

</details>

---
## [86] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,018 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int mainLen = data.consumeInt(0, 32);

        final boolean useValidSecondaryLength = data.consumeBoolean();
        final int secondaryLen;
        if (useValidSecondaryLength) {
            secondaryLen = mainLen > 0 ? mainLen - 1 : 0;
        } else {
            secondaryLen = data.consumeInt(0, 32);
        }

        final double[] main = new double[mainLen];
        final double[] secondary = new double[secondaryLen];

        for (int i = 0; i < main.length; i++) {
            int choice = data.consumeInt(0, 7);
            if (choice == 0) {
                main[i] = 0.0;
            } else if (choice == 1) {
                main[i] = -0.0;
            } else if (choice == 2) {
                main[i] = Double.POSITIVE_INFINITY;
            } else if (choice == 3) {
                main[i] = Double.NEGATIVE_INFINITY;
            } else if (choice == 4) {
                main[i] = Double.NaN;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                main[i] = Double.longBitsToDouble(bits);
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int choice = data.consumeInt(0, 7);
            if (choice == 0) {
                secondary[i] = 0.0;
            } else if (choice == 1) {
                secondary[i] = -0.0;
            } else if (choice == 2) {
                secondary[i] = Double.POSITIVE_INFINITY;
            } else if (choice == 3) {
                secondary[i] = Double.NEGATIVE_INFINITY;
            } else if (choice == 4) {
                secondary[i] = Double.NaN;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                secondary[i] = Double.longBitsToDouble(bits);
            }
        }

        final double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = 1.0;
                break;
            case 3:
                splitTolerance = -1.0;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 6:
                splitTolerance = Double.NaN;
                break;
            default:
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (data.consumeBoolean() && mainLen > 0) {
            int idx = data.consumeInt(0, mainLen - 1);
            ed.getRealEigenvalue(idx);
        }
        if (data.consumeBoolean() && mainLen > 0) {
            int idx = data.consumeInt(0, mainLen - 1);
            ed.getImagEigenvalue(idx);
        }
        if (data.consumeBoolean()) {
            ed.getImagEigenvalues();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean() && mainLen > 0) {
            int idx = data.consumeInt(0, mainLen - 1);
            ed.getEigenvector(idx);
        }
        if (data.consumeBoolean()) {
            ed.getSolver();
        }
    }
}
```

</details>

---
## [88] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,554 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < n; i++) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double v = Double.longBitsToDouble(bits);
            if (data.consumeBoolean()) {
                v = (double) data.consumeInt();
            }
            main[i] = v;
        }

        for (int i = 0; i < secondary.length; i++) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double v = Double.longBitsToDouble(bits);
            if (data.consumeBoolean()) {
                v = (double) data.consumeInt();
            }
            secondary[i] = v;
        }

        long splitBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double splitTolerance = Double.longBitsToDouble(splitBits);
        if (data.consumeBoolean()) {
            splitTolerance = (double) data.consumeInt();
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        double[] real = ed.getRealEigenvalues();
        for (int i = 0; i < real.length; i++) {
            ed.getRealEigenvalue(i);
            ed.getImagEigenvalue(i);
            if (data.consumeBoolean()) {
                ed.getEigenvector(i);
            }
        }

        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            DecompositionSolver solver = ed.getSolver();
            if (n > 0) {
                double[] b = new double[n];
                for (int i = 0; i < n; i++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    if (data.consumeBoolean()) {
                        v = (double) data.consumeInt();
                    }
                    b[i] = v;
                }
                if (data.consumeBoolean()) {
                    solver.solve(b);
                } else {
                    RealVector rv = new ArrayRealVector(b, true);
                    solver.solve(rv);
                }
            }
        }

        if (data.consumeBoolean()) {
            int m = data.consumeInt(0, 16);
            double[][] matrixData = new double[m][m];
            for (int i = 0; i < m; i++) {
                for (int j = i; j < m; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    if (data.consumeBoolean()) {
                        v = (double) data.consumeInt();
                    }
                    matrixData[i][j] = v;
                    matrixData[j][i] = v;
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData, false);
            long splitBits2 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double splitTolerance2 = Double.longBitsToDouble(splitBits2);
            if (data.consumeBoolean()) {
                splitTolerance2 = (double) data.consumeInt();
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(matrix, splitTolerance2);
            double[] real2 = ed2.getRealEigenvalues();
            for (int i = 0; i < real2.length; i++) {
                ed2.getRealEigenvalue(i);
                ed2.getImagEigenvalue(i);
                if (data.consumeBoolean()) {
                    ed2.getEigenvector(i);
                }
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,379 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int runs = 1;
        if (data.remainingBytes() > 0) {
            runs += (data.consumeByte() & 0x03);
        }

        for (int run = 0; run < runs; run++) {
            int n = 1;
            if (data.remainingBytes() > 0) {
                n = data.consumeInt(1, 32);
            }

            double[] main = new double[n];
            double[] secondary = new double[n > 0 ? n - 1 : 0];

            for (int i = 0; i < main.length; i++) {
                double v;
                int mode = data.remainingBytes() > 0 ? data.consumeInt(0, 6) : 0;
                switch (mode) {
                    case 0: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                    }
                    case 1:
                        v = (double) data.consumeInt(-16, 16);
                        break;
                    case 2:
                        v = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                    case 3:
                        v = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = ((double) data.consumeInt(-1000000, 1000000)) / (data.consumeBoolean() ? 1.0d : 1024.0d);
                        break;
                    default:
                        v = (i == 0 ? Double.MIN_VALUE : Double.MAX_VALUE);
                        if (data.consumeBoolean()) {
                            v = -v;
                        }
                        break;
                }
                main[i] = v;
            }

            for (int i = 0; i < secondary.length; i++) {
                double v;
                int mode = data.remainingBytes() > 0 ? data.consumeInt(0, 6) : 0;
                switch (mode) {
                    case 0: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                    }
                    case 1:
                        v = (double) data.consumeInt(-16, 16);
                        break;
                    case 2:
                        v = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                    case 3:
                        v = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = ((double) data.consumeInt(-1000000, 1000000)) / (data.consumeBoolean() ? 1.0d : 2048.0d);
                        break;
                    default:
                        v = (i == 0 ? Double.MIN_VALUE : Double.MAX_VALUE);
                        if (data.consumeBoolean()) {
                            v = -v;
                        }
                        break;
                }
                secondary[i] = v;
            }

            if (n > 1 && data.consumeBoolean()) {
                main[0] = data.consumeBoolean() ? 0.0d : Double.MIN_VALUE;
                main[n - 1] = data.consumeBoolean() ? Double.MAX_VALUE : 1.0e300d;
            }

            double splitTolerance;
            int tolMode = data.remainingBytes() > 0 ? data.consumeInt(0, 5) : 0;
            switch (tolMode) {
                case 0: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
                }
                case 1:
                    splitTolerance = (double) data.consumeInt(-16, 16);
                    break;
                case 2:
                    splitTolerance = 0.0d;
                    break;
                case 3:
                    splitTolerance = -0.0d;
                    break;
                case 4:
                    splitTolerance = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                    break;
                default:
                    splitTolerance = ((double) data.consumeInt(-1000000, 1000000)) / 65536.0d;
                    break;
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getDeterminant();
            ed.getD();
            ed.getV();
            ed.getVT();

            if (n > 0) {
                int index = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(index);
                ed.getImagEigenvalue(index);
                ed.getEigenvector(index);
            }

            if (data.consumeBoolean()) {
                ed.getSolver();
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
- trigger: org.apache.commons.math.linear.InvalidMatrixException:

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,109 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    main[i] = data.consumeInt();
                    break;
                case 1: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    main[i] = den == 0 ? num : ((double) num) / den;
                    break;
                }
                case 2: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
                }
                case 3:
                    main[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                    break;
                case 4:
                    main[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default:
                    main[i] = Double.NaN;
                    break;
            }
        }

        double[] secondary = new double[Math.max(0, n - 1)];
        for (int i = 0; i < secondary.length; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    secondary[i] = data.consumeInt();
                    break;
                case 1: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    secondary[i] = den == 0 ? num : ((double) num) / den;
                    break;
                }
                case 2: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
                }
                case 3:
                    secondary[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                    break;
                case 4:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 5)) {
            case 0:
                splitTolerance = data.consumeInt();
                break;
            case 1: {
                int num = data.consumeInt();
                int den = data.consumeInt();
                splitTolerance = den == 0 ? num : ((double) num) / den;
                break;
            }
            case 2: {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
            }
            case 3:
                splitTolerance = data.consumeBoolean() ? 0.0d : -0.0d;
                break;
            case 4:
                splitTolerance = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                break;
            default:
                splitTolerance = Double.NaN;
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        double[] realEigenvalues = ed.getRealEigenvalues();
        int dim = realEigenvalues.length;

        ed.getDeterminant();
        RealMatrix v = ed.getV();
        RealMatrix vt = ed.getVT();

        for (int i = 0; i < dim; i++) {
            ed.getRealEigenvalue(i);
            ed.getImagEigenvalue(i);
            ed.getEigenvector(i);
        }

        if (dim > 0) {
            int idx = data.consumeInt(0, dim - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);

            DecompositionSolver solver = ed.getSolver();
            solver.isNonSingular();

            double[] rhs = new double[dim];
            for (int i = 0; i < dim; i++) {
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        rhs[i] = data.consumeInt();
                        break;
                    case 1: {
                        int num = data.consumeInt();
                        int den = data.consumeInt();
                        rhs[i] = den == 0 ? num : ((double) num) / den;
                        break;
                    }
                    case 2: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        rhs[i] = Double.longBitsToDouble(bits);
                        break;
                    }
                    case 3:
                        rhs[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                        break;
                    default:
                        rhs[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                }
            }

            solver.solve(rhs);
            solver.solve(new ArrayRealVector(rhs, true));

            int cols = data.consumeInt(1, 4);
            double[][] rhsMatrixData = new double[dim][cols];
            for (int r = 0; r < dim; r++) {
                for (int c = 0; c < cols; c++) {
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            rhsMatrixData[r][c] = data.consumeInt();
                            break;
                        case 1: {
                            int num = data.consumeInt();
                            int den = data.consumeInt();
                            rhsMatrixData[r][c] = den == 0 ? num : ((double) num) / den;
                            break;
                        }
                        case 2: {
                            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                            rhsMatrixData[r][c] = Double.longBitsToDouble(bits);
                            break;
                        }
                        case 3:
                            rhsMatrixData[r][c] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            rhsMatrixData[r][c] = data.consumeBoolean() ? 0.0d : -0.0d;
                            break;
                    }
                }
            }

            solver.solve(new Array2DRowRealMatrix(rhsMatrixData, false));
        }

        if (v != null) {
            v.getRowDimension();
            v.getColumnDimension();
        }
        if (vt != null) {
            vt.getRowDimension();
            vt.getColumnDimension();
        }
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.SingularMatrixException: matrix is singular

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,632 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 24);

        double splitTolerance;
        {
            int mode = data.consumeInt(0, 7);
            if (mode == 0) {
                splitTolerance = 0.0;
            } else if (mode == 1) {
                splitTolerance = -0.0;
            } else if (mode == 2) {
                splitTolerance = Double.NaN;
            } else if (mode == 3) {
                splitTolerance = Double.POSITIVE_INFINITY;
            } else if (mode == 4) {
                splitTolerance = Double.NEGATIVE_INFINITY;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
            }
        }

        if (data.consumeBoolean()) {
            double[] main = new double[n];
            double[] secondary = new double[n > 0 ? n - 1 : 0];

            for (int i = 0; i < main.length; i++) {
                int shape = data.consumeInt(0, 9);
                if (shape == 0) {
                    main[i] = 0.0;
                } else if (shape == 1) {
                    main[i] = -0.0;
                } else if (shape == 2) {
                    main[i] = 1.0;
                } else if (shape == 3) {
                    main[i] = -1.0;
                } else if (shape == 4) {
                    main[i] = data.consumeInt();
                } else if (shape == 5) {
                    main[i] = data.consumeInt(-8, 8);
                } else if (shape == 6) {
                    main[i] = Double.NaN;
                } else if (shape == 7) {
                    main[i] = Double.POSITIVE_INFINITY;
                } else if (shape == 8) {
                    main[i] = Double.NEGATIVE_INFINITY;
                } else {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                }
            }

            for (int i = 0; i < secondary.length; i++) {
                int shape = data.consumeInt(0, 9);
                if (shape == 0) {
                    secondary[i] = 0.0;
                } else if (shape == 1) {
                    secondary[i] = -0.0;
                } else if (shape == 2) {
                    secondary[i] = 1.0;
                } else if (shape == 3) {
                    secondary[i] = -1.0;
                } else if (shape == 4) {
                    secondary[i] = data.consumeInt();
                } else if (shape == 5) {
                    secondary[i] = data.consumeInt(-8, 8);
                } else if (shape == 6) {
                    secondary[i] = Double.NaN;
                } else if (shape == 7) {
                    secondary[i] = Double.POSITIVE_INFINITY;
                } else if (shape == 8) {
                    secondary[i] = Double.NEGATIVE_INFINITY;
                } else {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                }
            }

            if (n > 0) {
                if (data.consumeBoolean()) {
                    main[0] = data.consumeBoolean() ? 0.0 : main[0];
                }
                if (data.consumeBoolean()) {
                    main[n - 1] = data.consumeBoolean() ? main[0] : -main[n - 1];
                }
            }
            if (secondary.length > 0) {
                if (data.consumeBoolean()) {
                    secondary[0] = 0.0;
                }
                if (data.consumeBoolean()) {
                    secondary[secondary.length - 1] = -secondary[secondary.length - 1];
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);
            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            for (int i = 0; i < n; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (data.consumeBoolean()) {
                ed.getSolver();
            }
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    double v;
                    int shape = data.consumeInt(0, 9);
                    if (shape == 0) {
                        v = 0.0;
                    } else if (shape == 1) {
                        v = -0.0;
                    } else if (shape == 2) {
                        v = 1.0;
                    } else if (shape == 3) {
                        v = -1.0;
                    } else if (shape == 4) {
                        v = data.consumeInt();
                    } else if (shape == 5) {
                        v = data.consumeInt(-8, 8);
                    } else if (shape == 6) {
                        v = Double.NaN;
                    } else if (shape == 7) {
                        v = Double.POSITIVE_INFINITY;
                    } else if (shape == 8) {
                        v = Double.NEGATIVE_INFINITY;
                    } else {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                    }
                    matrixData[i][j] = v;
                    matrixData[j][i] = v;
                }
            }

            if (n > 0) {
                if (data.consumeBoolean()) {
                    matrixData[0][0] = 0.0;
                }
                if (data.consumeBoolean()) {
                    matrixData[n - 1][n - 1] = -matrixData[n - 1][n - 1];
                }
            }

            RealMatrix m = new Array2DRowRealMatrix(matrixData, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, splitTolerance);
            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            for (int i = 0; i < n; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (data.consumeBoolean()) {
                ed.getSolver();
            }
        }
    }
}
```

</details>

---
## [96] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathRuntimeException$4: matrix must have at least one row

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,681 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 2);

        if (mode <= 1) {
            int n = data.consumeInt(1, 32);
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < main.length; i++) {
                int choice = data.consumeInt(0, 11);
                double v;
                switch (choice) {
                    case 0:
                        v = 0.0d;
                        break;
                    case 1:
                        v = -0.0d;
                        break;
                    case 2:
                        v = 1.0d;
                        break;
                    case 3:
                        v = -1.0d;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.MIN_VALUE;
                        break;
                    case 8:
                        v = -Double.MIN_VALUE;
                        break;
                    case 9:
                        v = Double.MAX_VALUE;
                        break;
                    case 10:
                        v = -Double.MAX_VALUE;
                        break;
                    default:
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                }
                main[i] = v;
            }

            for (int i = 0; i < secondary.length; i++) {
                int choice = data.consumeInt(0, 11);
                double v;
                switch (choice) {
                    case 0:
                        v = 0.0d;
                        break;
                    case 1:
                        v = -0.0d;
                        break;
                    case 2:
                        v = 1.0d;
                        break;
                    case 3:
                        v = -1.0d;
                        break;
                    case 4:
                        v = Double.NaN;
                        break;
                    case 5:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        v = Double.MIN_VALUE;
                        break;
                    case 8:
                        v = -Double.MIN_VALUE;
                        break;
                    case 9:
                        v = Double.MAX_VALUE;
                        break;
                    case 10:
                        v = -Double.MAX_VALUE;
                        break;
                    default:
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        break;
                }
                secondary[i] = v;
            }

            double splitTolerance;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                case 5:
                    splitTolerance = Double.MAX_VALUE;
                    break;
                case 6:
                    splitTolerance = Double.NaN;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            double[] reals = ed.getRealEigenvalues();
            for (int i = 0; i < reals.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            ed.getRealEigenvalues();
            ed.getDeterminant();
        } else {
            int n = data.consumeInt(1, 16);
            double[][] matrix = new double[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    int choice = data.consumeInt(0, 11);
                    double v;
                    switch (choice) {
                        case 0:
                            v = 0.0d;
                            break;
                        case 1:
                            v = -0.0d;
                            break;
                        case 2:
                            v = 1.0d;
                            break;
                        case 3:
                            v = -1.0d;
                            break;
                        case 4:
                            v = Double.NaN;
                            break;
                        case 5:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        case 6:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                        case 7:
                            v = Double.MIN_VALUE;
                            break;
                        case 8:
                            v = -Double.MIN_VALUE;
                            break;
                        case 9:
                            v = Double.MAX_VALUE;
                            break;
                        case 10:
                            v = -Double.MAX_VALUE;
                            break;
                        default:
                            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                            v = Double.longBitsToDouble(bits);
                            break;
                    }
                    matrix[i][j] = v;
                    matrix[j][i] = v;
                }
            }

            double splitTolerance;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                case 5:
                    splitTolerance = Double.MAX_VALUE;
                    break;
                case 6:
                    splitTolerance = Double.NaN;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }

            RealMatrix m = new Array2DRowRealMatrix(matrix);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, splitTolerance);

            ed.getD();
            ed.getV();
            ed.getVT();
            ed.getDeterminant();
            double[] reals = ed.getRealEigenvalues();
            for (int i = 0; i < reals.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            ed.getRealEigenvalues();
            ed.getDeterminant();
        }
    }
}
```

</details>

---
## [98] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.InvalidMatrixException:

---
## [99] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,431 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen = data.consumeInt(0, 32);
        double[] main = new double[mainLen];

        for (int i = 0; i < main.length; i++) {
            int kind = Math.abs((int) data.consumeByte()) % 8;
            switch (kind) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
                    break;
                case 2:
                    main[i] = Double.NaN;
                    break;
                case 3:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    main[i] = data.consumeInt();
                    break;
                default:
                    main[i] = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
            }
        }

        boolean useValidSecondaryLength = data.consumeBoolean();
        int secondaryLen;
        if (useValidSecondaryLength && mainLen > 0) {
            secondaryLen = mainLen - 1;
        } else {
            secondaryLen = data.consumeInt(0, 32);
        }

        double[] secondary = new double[secondaryLen];
        for (int i = 0; i < secondary.length; i++) {
            int kind = Math.abs((int) data.consumeByte()) % 8;
            switch (kind) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = Double.NaN;
                    break;
                case 3:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    secondary[i] = data.consumeInt();
                    break;
                default:
                    secondary[i] = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
            }
        }

        double splitTolerance;
        {
            int kind = Math.abs((int) data.consumeByte()) % 8;
            switch (kind) {
                case 0:
                    splitTolerance = 0.0;
                    break;
                case 1:
                    splitTolerance = -0.0;
                    break;
                case 2:
                    splitTolerance = Double.NaN;
                    break;
                case 3:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    splitTolerance = data.consumeInt();
                    break;
                default:
                    splitTolerance = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
            }
        }

        if (data.consumeBoolean()) {
            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

            if (data.consumeBoolean()) {
                ed.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed.getImagEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed.getDeterminant();
            }
            if (data.consumeBoolean()) {
                ed.getV();
            }
            if (data.consumeBoolean()) {
                ed.getVT();
            }
            if (data.consumeBoolean()) {
                ed.getD();
            }
            if (mainLen > 0 && data.consumeBoolean()) {
                ed.getRealEigenvalue(data.consumeInt(0, mainLen - 1));
            }
            if (mainLen > 0 && data.consumeBoolean()) {
                ed.getImagEigenvalue(data.consumeInt(0, mainLen - 1));
            }
            if (mainLen > 0 && data.consumeBoolean()) {
                ed.getEigenvector(data.consumeInt(0, mainLen - 1));
            }
        }

        int dim = data.consumeInt(0, 12);
        double[][] matrixData = new double[dim][dim];
        boolean makeSymmetric = data.consumeBoolean();

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if (makeSymmetric && j < i) {
                    matrixData[i][j] = matrixData[j][i];
                    continue;
                }
                int kind = Math.abs((int) data.consumeByte()) % 8;
                double v;
                switch (kind) {
                    case 0:
                        v = 0.0;
                        break;
                    case 1:
                        v = -0.0;
                        break;
                    case 2:
                        v = Double.NaN;
                        break;
                    case 3:
                        v = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        v = Double.NEGATIVE_INFINITY;
                        break;
                    case 5:
                        v = data.consumeInt();
                        break;
                    default:
                        v = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                        );
                        break;
                }
                matrixData[i][j] = v;
                if (makeSymmetric && i != j) {
                    matrixData[j][i] = v;
                }
            }
        }

        if (data.consumeBoolean()) {
            RealMatrix m = new Array2DRowRealMatrix(matrixData);
            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(m, splitTolerance);

            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getImagEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (dim > 0 && data.consumeBoolean()) {
                ed2.getRealEigenvalue(data.consumeInt(0, dim - 1));
            }
            if (dim > 0 && data.consumeBoolean()) {
                ed2.getImagEigenvalue(data.consumeInt(0, dim - 1));
            }
            if (dim > 0 && data.consumeBoolean()) {
                ed2.getEigenvector(data.consumeInt(0, dim - 1));
            }
        }
    }
}
```

</details>

---
## [100] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathRuntimeException$4: matrix must have at least one row

---
## [101] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,040 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int n = data.consumeInt(0, 32);

        final double[] main = new double[n];
        final double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            final int mode = data.consumeInt(0, 7);
            switch (mode) {
                case 0:
                    main[i] = data.consumeInt(-4, 4);
                    break;
                case 1:
                    main[i] = data.consumeInt(-1000, 1000);
                    break;
                case 2:
                    main[i] = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 3:
                    main[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = data.consumeByte();
                    break;
                case 6: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    main[i] = data.consumeInt() / (double) (data.consumeInt(-16, 16) == 0 ? 1 : data.consumeInt(-16, 16));
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            final int mode = data.consumeInt(0, 7);
            switch (mode) {
                case 0:
                    secondary[i] = data.consumeInt(-4, 4);
                    break;
                case 1:
                    secondary[i] = data.consumeInt(-1000, 1000);
                    break;
                case 2:
                    secondary[i] = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 3:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
                case 4:
                    secondary[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    secondary[i] = data.consumeByte();
                    break;
                case 6: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    secondary[i] = data.consumeInt() * (data.consumeBoolean() ? 1.0 : -1.0);
                    break;
            }
        }

        final double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = data.consumeInt(-10, 10);
                break;
            case 3:
                splitTolerance = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                break;
            case 4:
                splitTolerance = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                break;
            case 5:
                splitTolerance = Double.NaN;
                break;
            case 6: {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                splitTolerance = Double.longBitsToDouble(bits);
                break;
            }
            default:
                splitTolerance = data.consumeByte();
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getD();
        ed.getV();
        ed.getVT();
        ed.getDeterminant();
        ed.getRealEigenvalues();

        if (n > 0) {
            for (int i = 0; i < Math.min(n, 4); i++) {
                int idx = data.consumeInt(0, n - 1);
                ed.getRealEigenvalue(idx);
                ed.getImagEigenvalue(idx);
                ed.getEigenvector(idx);
            }

            DecompositionSolver solver = ed.getSolver();
            solver.isNonSingular();

            double[] rhs = new double[n];
            for (int i = 0; i < n; i++) {
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        rhs[i] = data.consumeInt(-8, 8);
                        break;
                    case 1:
                        rhs[i] = data.consumeByte();
                        break;
                    case 2:
                        rhs[i] = data.consumeBoolean() ? 0.0 : -0.0;
                        break;
                    case 3:
                        rhs[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                        break;
                    default: {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        rhs[i] = Double.longBitsToDouble(bits);
                        break;
                    }
                }
            }

            solver.solve(rhs);
            solver.solve(new ArrayRealVector(rhs, true));

            double[][] rhsMatrixData = new double[n][data.consumeInt(1, 3)];
            for (int r = 0; r < rhsMatrixData.length; r++) {
                for (int c = 0; c < rhsMatrixData[r].length; c++) {
                    rhsMatrixData[r][c] = (r < rhs.length) ? rhs[r] : 0.0;
                }
            }
            solver.solve(new Array2DRowRealMatrix(rhsMatrixData, false));
            solver.getInverse();
        }
    }
}
```

</details>

---
## [102] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [103] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,323 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen;
        if (data.consumeBoolean()) {
            mainLen = data.consumeInt(0, 32);
        } else {
            mainLen = Math.abs(data.consumeByte()) % 33;
        }

        double[] main = new double[mainLen];
        for (int i = 0; i < mainLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
                    break;
                case 2:
                    main[i] = 1.0;
                    break;
                case 3:
                    main[i] = -1.0;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = (double) data.consumeInt();
                    break;
                case 8:
                    main[i] = ((double) data.consumeInt()) / (Math.abs(data.consumeByte()) + 1.0);
                    break;
                default:
                    main[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
            }
        }

        int secondaryLen;
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                secondaryLen = Math.max(0, mainLen - 1);
            } else {
                secondaryLen = data.consumeInt(0, 32);
            }
        } else {
            secondaryLen = Math.abs(data.consumeByte()) % 33;
        }

        double[] secondary = new double[secondaryLen];
        for (int i = 0; i < secondaryLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = 2.0;
                    break;
                case 3:
                    secondary[i] = -2.0;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = (double) data.consumeInt();
                    break;
                case 8:
                    secondary[i] = ((double) data.consumeInt()) / (Math.abs(data.consumeByte()) + 1.0);
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_NORMAL : -Double.MAX_VALUE;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = 1.0;
                break;
            case 3:
                splitTolerance = -1.0;
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                splitTolerance = (double) data.consumeInt();
                break;
            case 8:
                splitTolerance = ((double) data.consumeInt()) / (Math.abs(data.consumeByte()) + 1.0);
                break;
            default:
                splitTolerance = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }

        int iterations = data.consumeInt(0, 12);
        for (int t = 0; t < iterations; t++) {
            int op = data.consumeInt(0, 7);
            switch (op) {
                case 0:
                    ed.getRealEigenvalues();
                    break;
                case 1:
                    ed.getImagEigenvalues();
                    break;
                case 2:
                    if (mainLen > 0) {
                        ed.getRealEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getRealEigenvalue(0);
                    }
                    break;
                case 3:
                    if (mainLen > 0) {
                        ed.getImagEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getImagEigenvalue(0);
                    }
                    break;
                case 4:
                    if (mainLen > 0) {
                        ed.getEigenvector(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getEigenvector(0);
                    }
                    break;
                case 5:
                    ed.getSolver();
                    break;
                case 6:
                    ed.getDeterminant();
                    break;
                default:
                    ed.getV();
                    ed.getD();
                    ed.getVT();
                    break;
            }
        }
    }
}
```

</details>

---
## [104] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 11: `Math.abs(data.consumeByte()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds an

---
## [105] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,889 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen;
        if (data.consumeBoolean()) {
            mainLen = data.consumeInt(0, 32);
        } else {
            mainLen = Math.abs(data.consumeByte()) % 33;
        }

        double[] main = new double[mainLen];
        for (int i = 0; i < mainLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
                    break;
                case 2:
                    main[i] = 1.0;
                    break;
                case 3:
                    main[i] = -1.0;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = (double) data.consumeInt();
                    break;
                case 8:
                    main[i] = ((double) data.consumeInt()) / (Math.abs(data.consumeByte()) + 1.0);
                    break;
                default:
                    main[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
            }
        }

        int secondaryLen;
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                secondaryLen = Math.max(0, mainLen - 1);
            } else {
                secondaryLen = data.consumeInt(0, 32);
            }
        } else {
            secondaryLen = Math.abs(data.consumeByte()) % 33;
        }

        double[] secondary = new double[secondaryLen];
        for (int i = 0; i < secondaryLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = 2.0;
                    break;
                case 3:
                    secondary[i] = -2.0;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = (double) data.consumeInt();
                    break;
                case 8:
                    secondary[i] = ((double) data.consumeInt()) / (Math.abs(data.consumeByte()) + 1.0);
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_NORMAL : -Double.MAX_VALUE;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = 1.0;
                break;
            case 3:
                splitTolerance = -1.0;
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                splitTolerance = (double) data.consumeInt();
                break;
            case 8:
                splitTolerance = ((double) data.consumeInt()) / (Math.abs(data.consumeByte()) + 1.0);
                break;
            default:
                splitTolerance = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }

        int iterations = data.consumeInt(0, 12);
        for (int t = 0; t < iterations; t++) {
            int op = data.consumeInt(0, 7);
            switch (op) {
                case 0:
                    ed.getRealEigenvalues();
                    break;
                case 1:
                    ed.getImagEigenvalues();
                    break;
                case 2:
                    if (mainLen > 0) {
                        ed.getRealEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getRealEigenvalue(0);
                    }
                    break;
                case 3:
                    if (mainLen > 0) {
                        ed.getImagEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getImagEigenvalue(0);
                    }
                    break;
                case 4:
                    if (mainLen > 0) {
                        ed.getEigenvector(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getEigenvector(0);
                    }
                    break;
                case 5:
                    ed.getSolver();
                    break;
                case 6:
                    ed.getDeterminant();
                    break;
                default:
                    ed.getV();
                    ed.getD();
                    ed.getVT();
                    break;
            }
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 11: `Math.abs(data.consumeByte()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,391 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mainLen;
        if (data.consumeBoolean()) {
            mainLen = data.consumeInt(0, 32);
        } else {
            mainLen = Math.floorMod((int) data.consumeByte(), 33);
        }

        double[] main = new double[mainLen];
        for (int i = 0; i < mainLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0;
                    break;
                case 2:
                    main[i] = 1.0;
                    break;
                case 3:
                    main[i] = -1.0;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = (double) data.consumeInt();
                    break;
                case 8:
                    main[i] = ((double) data.consumeInt()) / (Math.floorMod((int) data.consumeByte(), 255) + 1.0);
                    break;
                default:
                    main[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
            }
        }

        int secondaryLen;
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                secondaryLen = Math.max(0, mainLen - 1);
            } else {
                secondaryLen = data.consumeInt(0, 32);
            }
        } else {
            secondaryLen = Math.floorMod((int) data.consumeByte(), 33);
        }

        double[] secondary = new double[secondaryLen];
        for (int i = 0; i < secondaryLen; i++) {
            int selector = data.consumeInt(0, 9);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0;
                    break;
                case 2:
                    secondary[i] = 2.0;
                    break;
                case 3:
                    secondary[i] = -2.0;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = (double) data.consumeInt();
                    break;
                case 8:
                    secondary[i] = ((double) data.consumeInt()) / (Math.floorMod((int) data.consumeByte(), 255) + 1.0);
                    break;
                default:
                    secondary[i] = data.consumeBoolean() ? Double.MIN_NORMAL : -Double.MAX_VALUE;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0;
                break;
            case 2:
                splitTolerance = 1.0;
                break;
            case 3:
                splitTolerance = -1.0;
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                splitTolerance = (double) data.consumeInt();
                break;
            case 8:
                splitTolerance = ((double) data.consumeInt()) / (Math.floorMod((int) data.consumeByte(), 255) + 1.0);
                break;
            default:
                splitTolerance = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }

        int iterations = data.consumeInt(0, 12);
        for (int t = 0; t < iterations; t++) {
            int op = data.consumeInt(0, 7);
            switch (op) {
                case 0:
                    ed.getRealEigenvalues();
                    break;
                case 1:
                    ed.getImagEigenvalues();
                    break;
                case 2:
                    if (mainLen > 0) {
                        ed.getRealEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getRealEigenvalue(0);
                    }
                    break;
                case 3:
                    if (mainLen > 0) {
                        ed.getImagEigenvalue(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getImagEigenvalue(0);
                    }
                    break;
                case 4:
                    if (mainLen > 0) {
                        ed.getEigenvector(data.consumeInt(0, mainLen - 1));
                    } else {
                        ed.getEigenvector(0);
                    }
                    break;
                case 5:
                    ed.getSolver();
                    break;
                case 6:
                    ed.getDeterminant();
                    break;
                default:
                    ed.getV();
                    ed.getD();
                    ed.getVT();
                    break;
            }
        }
    }
}
```

</details>

---
## [106] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [107] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,189 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n;
        if (data.remainingBytes() <= 0) {
            n = 0;
        } else {
            n = data.consumeInt(0, Math.min(64, data.remainingBytes() + 2));
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.abs(data.consumeByte()) % 12 : (i % 12);
            switch (selector) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1103515245 + 12345);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (~a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.abs(data.consumeByte()) % 12 : ((i + 5) % 12);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1664525 + 1013904223);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (a ^ 0x5a5a5a5a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        int splitSelector = data.remainingBytes() > 0 ? Math.abs(data.consumeByte()) % 10 : 0;
        switch (splitSelector) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = Double.NaN;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                int a = data.remainingBytes() >= 4 ? data.consumeInt() : 0x7ff80000;
                int b = data.remainingBytes() >= 4 ? data.consumeInt() : 0;
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getImagEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getEigenvector(index);
        }
        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            ed.hasComplexEigenvalues();
        }

        if (n >= 2 && data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length; i++) {
                if ((i & 1) == 0) {
                    main2[i] = -main2[i];
                }
            }
            for (int i = 0; i < secondary2.length; i++) {
                if ((i & 1) == 1) {
                    secondary2[i] = -secondary2[i];
                }
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, -splitTolerance);
            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}
```

</details>

---
## [108] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 18: `Math.abs(data.consumeByte()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds an

---
## [109] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,755 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n;
        if (data.remainingBytes() <= 0) {
            n = 0;
        } else {
            n = data.consumeInt(0, Math.min(64, data.remainingBytes() + 2));
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.abs(data.consumeByte()) % 12 : (i % 12);
            switch (selector) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1103515245 + 12345);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (~a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.abs(data.consumeByte()) % 12 : ((i + 5) % 12);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1664525 + 1013904223);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (a ^ 0x5a5a5a5a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        int splitSelector = data.remainingBytes() > 0 ? Math.abs(data.consumeByte()) % 10 : 0;
        switch (splitSelector) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = Double.NaN;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                int a = data.remainingBytes() >= 4 ? data.consumeInt() : 0x7ff80000;
                int b = data.remainingBytes() >= 4 ? data.consumeInt() : 0;
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getImagEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getEigenvector(index);
        }
        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            ed.hasComplexEigenvalues();
        }

        if (n >= 2 && data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length; i++) {
                if ((i & 1) == 0) {
                    main2[i] = -main2[i];
                }
            }
            for (int i = 0; i < secondary2.length; i++) {
                if ((i & 1) == 1) {
                    secondary2[i] = -secondary2[i];
                }
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, -splitTolerance);
            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 18: `Math.abs(data.consumeByte()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,219 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n;
        if (data.remainingBytes() <= 0) {
            n = 0;
        } else {
            n = data.consumeInt(0, Math.min(64, data.remainingBytes() + 2));
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : (i % 12);
            switch (selector) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1103515245 + 12345);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (~a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : ((i + 5) % 12);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1664525 + 1013904223);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (a ^ 0x5a5a5a5a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        int splitSelector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 10) : 0;
        switch (splitSelector) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = Double.NaN;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                int a = data.remainingBytes() >= 4 ? data.consumeInt() : 0x7ff80000;
                int b = data.remainingBytes() >= 4 ? data.consumeInt() : 0;
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getImagEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getEigenvector(index);
        }
        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            ed.hasComplexEigenvalues();
        }

        if (n >= 2 && data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length; i++) {
                if ((i & 1) == 0) {
                    main2[i] = -main2[i];
                }
            }
            for (int i = 0; i < secondary2.length; i++) {
                if ((i & 1) == 1) {
                    secondary2[i] = -secondary2[i];
                }
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, -splitTolerance);
            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}
```

</details>

---
## [110] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [111] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~8,681 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [109] — not reprinted*
- *[user] message: identical to step [109] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n;
        if (data.remainingBytes() <= 0) {
            n = 0;
        } else {
            n = data.consumeInt(0, Math.min(64, data.remainingBytes() + 2));
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : (i % 12);
            switch (selector) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1103515245 + 12345);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (~a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : ((i + 5) % 12);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1664525 + 1013904223);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (a ^ 0x5a5a5a5a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        int splitSelector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 10) : 0;
        switch (splitSelector) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = Double.NaN;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                int a = data.remainingBytes() >= 4 ? data.consumeInt() : 0x7ff80000;
                int b = data.remainingBytes() >= 4 ? data.consumeInt() : 0;
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getImagEigenvalue(index);
        }
        if (n > 0 && data.consumeBoolean()) {
            int index = data.consumeInt(0, n - 1);
            ed.getEigenvector(index);
        }
        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.consumeBoolean()) {
            ed.hasComplexEigenvalues();
        }

        if (n >= 2 && data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length; i++) {
                if ((i & 1) == 0) {
                    main2[i] = -main2[i];
                }
            }
            for (int i = 0; i < secondary2.length; i++) {
                if ((i & 1) == 1) {
                    secondary2[i] = -secondary2[i];
                }
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, -splitTolerance);
            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_017/FuzzHarness.java:174: error: cannot find symbol
            ed.hasComplexEigenvalues();
              ^
  symbol:   method hasComplexEigenvalues()
  location: variable ed of type EigenDecompositionImpl
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
<details open><summary>▸ Output (~7,426 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n;
        if (data.remainingBytes() <= 0) {
            n = 0;
        } else {
            n = data.consumeInt(0, Math.min(64, data.remainingBytes() + 2));
        }

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];

        for (int i = 0; i < main.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : (i % 12);
            switch (selector) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1103515245 + 12345);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (~a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 12) : ((i + 5) % 12);
            switch (selector) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                default:
                    int a = data.remainingBytes() >= 4 ? data.consumeInt() : (i * 1664525 + 1013904223);
                    int b = data.remainingBytes() >= 4 ? data.consumeInt() : (a ^ 0x5a5a5a5a);
                    long bits = (((long) a) << 32) ^ (b & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        int splitSelector = data.remainingBytes() > 0 ? Math.floorMod((int) data.consumeByte(), 10) : 0;
        switch (splitSelector) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = Double.NaN;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                int a = data.remainingBytes() >= 4 ? data.consumeInt() : 0x7ff80000;
                int b = data.remainingBytes() >= 4 ? data.consumeInt() : 0;
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (n > 0 && data.consumeBoolean()) {
            ed.getRealEigenvalue(data.consumeInt(0, n - 1));
        }
        if (n > 0 && data.consumeBoolean()) {
            ed.getImagEigenvalue(data.consumeInt(0, n - 1));
        }
        if (n > 0 && data.consumeBoolean()) {
            ed.getEigenvector(data.consumeInt(0, n - 1));
        }
        if (data.consumeBoolean()) {
            ed.getD();
        }
        if (data.consumeBoolean()) {
            ed.getV();
        }
        if (data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.consumeBoolean()) {
            ed.getDeterminant();
        }

        if (n >= 2 && data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length; i++) {
                if ((i & 1) == 0) {
                    main2[i] = -main2[i];
                }
            }
            for (int i = 0; i < secondary2.length; i++) {
                if ((i & 1) == 1) {
                    secondary2[i] = -secondary2[i];
                }
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, -splitTolerance);
            if (data.consumeBoolean()) {
                ed2.getRealEigenvalues();
            }
            if (n > 0 && data.consumeBoolean()) {
                ed2.getRealEigenvalue(data.consumeInt(0, n - 1));
            }
            if (n > 0 && data.consumeBoolean()) {
                ed2.getImagEigenvalue(data.consumeInt(0, n - 1));
            }
            if (n > 0 && data.consumeBoolean()) {
                ed2.getEigenvector(data.consumeInt(0, n - 1));
            }
            if (data.consumeBoolean()) {
                ed2.getD();
            }
            if (data.consumeBoolean()) {
                ed2.getV();
            }
            if (data.consumeBoolean()) {
                ed2.getVT();
            }
            if (data.consumeBoolean()) {
                ed2.getDeterminant();
            }
        }
    }
}
```

</details>

---
## [112] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [113] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,832 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean useMatrixCtor = data.consumeBoolean();

        if (useMatrixCtor) {
            int n = data.consumeInt(0, 8);
            double[][] raw = new double[n][n];

            boolean symmetric = data.consumeBoolean();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v;
                    switch (Math.abs(data.consumeInt()) % 8) {
                        case 0:
                            v = Double.longBitsToDouble(bits);
                            break;
                        case 1:
                            v = data.consumeInt();
                            break;
                        case 2:
                            v = -data.consumeInt();
                            break;
                        case 3:
                            v = 0.0;
                            break;
                        case 4:
                            v = -0.0;
                            break;
                        case 5:
                            v = Double.NaN;
                            break;
                        case 6:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                    }
                    raw[i][j] = v;
                }
            }

            if (symmetric) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        raw[j][i] = raw[i][j];
                    }
                }
            }

            RealMatrix m = new Array2DRowRealMatrix(raw, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            int dim = 0;
            try {
                dim = ed.getD().getRowDimension();
            } catch (Throwable t) {
                throw t;
            }

            for (int i = 0; i < dim; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        } else {
            int n = data.consumeInt(0, 16);
            double[] main = new double[n];

            for (int i = 0; i < n; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.abs(data.consumeInt()) % 8) {
                    case 0:
                        main[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        main[i] = data.consumeInt();
                        break;
                    case 2:
                        main[i] = -data.consumeInt();
                        break;
                    case 3:
                        main[i] = 0.0;
                        break;
                    case 4:
                        main[i] = -0.0;
                        break;
                    case 5:
                        main[i] = Double.NaN;
                        break;
                    case 6:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            int secondaryLen;
            if (n == 0) {
                secondaryLen = data.consumeBoolean() ? 0 : data.consumeInt(0, 4);
            } else {
                secondaryLen = data.consumeBoolean() ? (n - 1) : data.consumeInt(0, 20);
            }

            double[] secondary = new double[secondaryLen];
            for (int i = 0; i < secondaryLen; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.abs(data.consumeInt()) % 8) {
                    case 0:
                        secondary[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        secondary[i] = data.consumeInt();
                        break;
                    case 2:
                        secondary[i] = -data.consumeInt();
                        break;
                    case 3:
                        secondary[i] = 0.0;
                        break;
                    case 4:
                        secondary[i] = -0.0;
                        break;
                    case 5:
                        secondary[i] = Double.NaN;
                        break;
                    case 6:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            for (int i = 0; i < main.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        }
    }
}
```

</details>

---
## [114] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 18: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and

---
## [115] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,397 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean useMatrixCtor = data.consumeBoolean();

        if (useMatrixCtor) {
            int n = data.consumeInt(0, 8);
            double[][] raw = new double[n][n];

            boolean symmetric = data.consumeBoolean();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v;
                    switch (Math.abs(data.consumeInt()) % 8) {
                        case 0:
                            v = Double.longBitsToDouble(bits);
                            break;
                        case 1:
                            v = data.consumeInt();
                            break;
                        case 2:
                            v = -data.consumeInt();
                            break;
                        case 3:
                            v = 0.0;
                            break;
                        case 4:
                            v = -0.0;
                            break;
                        case 5:
                            v = Double.NaN;
                            break;
                        case 6:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                    }
                    raw[i][j] = v;
                }
            }

            if (symmetric) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        raw[j][i] = raw[i][j];
                    }
                }
            }

            RealMatrix m = new Array2DRowRealMatrix(raw, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            int dim = 0;
            try {
                dim = ed.getD().getRowDimension();
            } catch (Throwable t) {
                throw t;
            }

            for (int i = 0; i < dim; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        } else {
            int n = data.consumeInt(0, 16);
            double[] main = new double[n];

            for (int i = 0; i < n; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.abs(data.consumeInt()) % 8) {
                    case 0:
                        main[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        main[i] = data.consumeInt();
                        break;
                    case 2:
                        main[i] = -data.consumeInt();
                        break;
                    case 3:
                        main[i] = 0.0;
                        break;
                    case 4:
                        main[i] = -0.0;
                        break;
                    case 5:
                        main[i] = Double.NaN;
                        break;
                    case 6:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            int secondaryLen;
            if (n == 0) {
                secondaryLen = data.consumeBoolean() ? 0 : data.consumeInt(0, 4);
            } else {
                secondaryLen = data.consumeBoolean() ? (n - 1) : data.consumeInt(0, 20);
            }

            double[] secondary = new double[secondaryLen];
            for (int i = 0; i < secondaryLen; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.abs(data.consumeInt()) % 8) {
                    case 0:
                        secondary[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        secondary[i] = data.consumeInt();
                        break;
                    case 2:
                        secondary[i] = -data.consumeInt();
                        break;
                    case 3:
                        secondary[i] = 0.0;
                        break;
                    case 4:
                        secondary[i] = -0.0;
                        break;
                    case 5:
                        secondary[i] = Double.NaN;
                        break;
                    case 6:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            for (int i = 0; i < main.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 18: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~5,725 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean useMatrixCtor = data.consumeBoolean();

        if (useMatrixCtor) {
            int n = data.consumeInt(0, 8);
            double[][] raw = new double[n][n];

            boolean symmetric = data.consumeBoolean();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v;
                    switch (Math.floorMod(data.consumeInt(), 8)) {
                        case 0:
                            v = Double.longBitsToDouble(bits);
                            break;
                        case 1:
                            v = data.consumeInt();
                            break;
                        case 2:
                            v = -data.consumeInt();
                            break;
                        case 3:
                            v = 0.0;
                            break;
                        case 4:
                            v = -0.0;
                            break;
                        case 5:
                            v = Double.NaN;
                            break;
                        case 6:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                    }
                    raw[i][j] = v;
                }
            }

            if (symmetric) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        raw[j][i] = raw[i][j];
                    }
                }
            }

            RealMatrix m = new Array2DRowRealMatrix(raw, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            int dim = ed.getD().getRowDimension();
            for (int i = 0; i < dim; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        } else {
            int n = data.consumeInt(0, 16);
            double[] main = new double[n];

            for (int i = 0; i < n; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.floorMod(data.consumeInt(), 8)) {
                    case 0:
                        main[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        main[i] = data.consumeInt();
                        break;
                    case 2:
                        main[i] = -data.consumeInt();
                        break;
                    case 3:
                        main[i] = 0.0;
                        break;
                    case 4:
                        main[i] = -0.0;
                        break;
                    case 5:
                        main[i] = Double.NaN;
                        break;
                    case 6:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            int secondaryLen;
            if (n == 0) {
                secondaryLen = data.consumeBoolean() ? 0 : data.consumeInt(0, 4);
            } else {
                secondaryLen = data.consumeBoolean() ? (n - 1) : data.consumeInt(0, 20);
            }

            double[] secondary = new double[secondaryLen];
            for (int i = 0; i < secondaryLen; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.floorMod(data.consumeInt(), 8)) {
                    case 0:
                        secondary[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        secondary[i] = data.consumeInt();
                        break;
                    case 2:
                        secondary[i] = -data.consumeInt();
                        break;
                    case 3:
                        secondary[i] = 0.0;
                        break;
                    case 4:
                        secondary[i] = -0.0;
                        break;
                    case 5:
                        secondary[i] = Double.NaN;
                        break;
                    case 6:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            for (int i = 0; i < main.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        }
    }
}
```

</details>

---
## [116] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [117] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,543 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
- *[assistant] message: identical to step [115] — not reprinted*
- *[user] message: identical to step [115] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean useMatrixCtor = data.consumeBoolean();

        if (useMatrixCtor) {
            int n = data.consumeInt(0, 8);
            double[][] raw = new double[n][n];

            boolean symmetric = data.consumeBoolean();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v;
                    switch (Math.floorMod(data.consumeInt(), 8)) {
                        case 0:
                            v = Double.longBitsToDouble(bits);
                            break;
                        case 1:
                            v = data.consumeInt();
                            break;
                        case 2:
                            v = -data.consumeInt();
                            break;
                        case 3:
                            v = 0.0;
                            break;
                        case 4:
                            v = -0.0;
                            break;
                        case 5:
                            v = Double.NaN;
                            break;
                        case 6:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                    }
                    raw[i][j] = v;
                }
            }

            if (symmetric) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        raw[j][i] = raw[i][j];
                    }
                }
            }

            RealMatrix m = new Array2DRowRealMatrix(raw, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(m, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            int dim = ed.getD().getRowDimension();
            for (int i = 0; i < dim; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        } else {
            int n = data.consumeInt(0, 16);
            double[] main = new double[n];

            for (int i = 0; i < n; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.floorMod(data.consumeInt(), 8)) {
                    case 0:
                        main[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        main[i] = data.consumeInt();
                        break;
                    case 2:
                        main[i] = -data.consumeInt();
                        break;
                    case 3:
                        main[i] = 0.0;
                        break;
                    case 4:
                        main[i] = -0.0;
                        break;
                    case 5:
                        main[i] = Double.NaN;
                        break;
                    case 6:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            int secondaryLen;
            if (n == 0) {
                secondaryLen = data.consumeBoolean() ? 0 : data.consumeInt(0, 4);
            } else {
                secondaryLen = data.consumeBoolean() ? (n - 1) : data.consumeInt(0, 20);
            }

            double[] secondary = new double[secondaryLen];
            for (int i = 0; i < secondaryLen; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                switch (Math.floorMod(data.consumeInt(), 8)) {
                    case 0:
                        secondary[i] = Double.longBitsToDouble(bits);
                        break;
                    case 1:
                        secondary[i] = data.consumeInt();
                        break;
                    case 2:
                        secondary[i] = -data.consumeInt();
                        break;
                    case 3:
                        secondary[i] = 0.0;
                        break;
                    case 4:
                        secondary[i] = -0.0;
                        break;
                    case 5:
                        secondary[i] = Double.NaN;
                        break;
                    case 6:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, data.consumeInt());

            ed.getV();
            ed.getVT();
            ed.getD();
            ed.getDeterminant();
            ed.hasComplexEigenvalues();

            for (int i = 0; i < main.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_020/FuzzHarness.java:63: error: cannot find symbol
            ed.hasComplexEigenvalues();
              ^
  symbol:   method hasComplexEigenvalues()
  location: variable ed of type EigenDecompositionImpl
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_020/FuzzHarness.java:149: error: cannot find symbol
            ed.hasComplexEigenvalues();
              ^
  symbol:   method hasComplexEigenvalues()
  location: variable ed of type EigenDecompositionImpl
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
<details open><summary>▸ Output (~3,757 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static double fuzzDouble(FuzzedDataProvider data) {
        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        switch (Math.floorMod(data.consumeInt(), 8)) {
            case 0:
                return Double.longBitsToDouble(bits);
            case 1:
                return (double) data.consumeInt();
            case 2:
                return (double) (-data.consumeInt());
            case 3:
                return 0.0;
            case 4:
                return -0.0;
            case 5:
                return Double.NaN;
            case 6:
                return Double.POSITIVE_INFINITY;
            default:
                return Double.NEGATIVE_INFINITY;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            int n = data.consumeInt(0, 8);
            double[][] raw = new double[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    raw[i][j] = fuzzDouble(data);
                }
            }

            if (data.consumeBoolean()) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        raw[j][i] = raw[i][j];
                    }
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(raw, false);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, fuzzDouble(data));

            ed.getV();
            RealMatrix d = ed.getD();
            ed.getVT();
            ed.getDeterminant();

            int dim = d.getRowDimension();
            for (int i = 0; i < dim; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                double[] b = new double[dim];
                for (int i = 0; i < dim; i++) {
                    b[i] = fuzzDouble(data);
                }
                solver.solve(new ArrayRealVector(b, false));
            }
        } else {
            int n = data.consumeInt(0, 16);
            double[] main = new double[n];
            for (int i = 0; i < n; i++) {
                main[i] = fuzzDouble(data);
            }

            int secondaryLen;
            if (n == 0) {
                secondaryLen = data.consumeBoolean() ? 0 : data.consumeInt(0, 4);
            } else {
                secondaryLen = data.consumeBoolean() ? (n - 1) : data.consumeInt(0, 20);
            }

            double[] secondary = new double[secondaryLen];
            for (int i = 0; i < secondaryLen; i++) {
                secondary[i] = fuzzDouble(data);
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, fuzzDouble(data));

            ed.getV();
            ed.getD();
            ed.getVT();
            ed.getDeterminant();

            for (int i = 0; i < main.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                double[] b = new double[main.length];
                for (int i = 0; i < main.length; i++) {
                    b[i] = fuzzDouble(data);
                }
                solver.solve(new ArrayRealVector(b, false));
            }
        }
    }
}
```

</details>

---
## [118] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [119] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,293 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        int secondaryLength;
        if (n == 0) {
            secondaryLength = data.consumeInt(0, 8);
        } else if (data.consumeBoolean()) {
            secondaryLength = n - 1;
        } else {
            secondaryLength = data.consumeInt(0, 32);
        }

        double[] main = new double[n];
        double[] secondary = new double[secondaryLength];

        for (int i = 0; i < main.length; i++) {
            int choice = data.consumeInt(0, 11);
            switch (choice) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = Double.MIN_NORMAL;
                    break;
                case 10:
                    main[i] = -Double.MIN_VALUE;
                    break;
                default:
                    main[i] = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int choice = data.consumeInt(0, 11);
            switch (choice) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MIN_NORMAL;
                    break;
                case 10:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                default:
                    secondary[i] = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                splitTolerance = Double.MIN_NORMAL;
                break;
            case 8:
                splitTolerance = Double.MAX_VALUE;
                break;
            default:
                splitTolerance = (double) Float.intBitsToFloat(data.consumeInt());
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getDeterminant();
        RealMatrix d = ed.getD();
        RealMatrix v = ed.getV();
        RealMatrix vt = ed.getVT();

        if (n > 0) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
            ed.getImagEigenvalue(index);
            ed.getEigenvector(index);
        }

        DecompositionSolver solver = ed.getSolver();
        solver.isNonSingular();

        double[] rhs = new double[n];
        for (int i = 0; i < rhs.length; i++) {
            int choice = data.consumeInt(0, 7);
            switch (choice) {
                case 0:
                    rhs[i] = 0.0d;
                    break;
                case 1:
                    rhs[i] = 1.0d;
                    break;
                case 2:
                    rhs[i] = -1.0d;
                    break;
                case 3:
                    rhs[i] = Double.NaN;
                    break;
                case 4:
                    rhs[i] = Double.POSITIVE_INFINITY;
                    break;
                case 5:
                    rhs[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 6:
                    rhs[i] = Double.MIN_NORMAL;
                    break;
                default:
                    rhs[i] = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
            }
        }

        RealVector b = new ArrayRealVector(rhs, false);
        solver.solve(b);

        double[][] rhsMatrixData = new double[n][];
        int cols = data.consumeInt(0, 8);
        for (int r = 0; r < n; r++) {
            rhsMatrixData[r] = new double[cols];
            for (int c = 0; c < cols; c++) {
                int choice = data.consumeInt(0, 5);
                switch (choice) {
                    case 0:
                        rhsMatrixData[r][c] = 0.0d;
                        break;
                    case 1:
                        rhsMatrixData[r][c] = 1.0d;
                        break;
                    case 2:
                        rhsMatrixData[r][c] = -1.0d;
                        break;
                    case 3:
                        rhsMatrixData[r][c] = Double.NaN;
                        break;
                    case 4:
                        rhsMatrixData[r][c] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        rhsMatrixData[r][c] = (double) Float.intBitsToFloat(data.consumeInt());
                        break;
                }
            }
        }

        RealMatrix rhsMatrix = new Array2DRowRealMatrix(rhsMatrixData, false);
        solver.solve(rhsMatrix);

        d.getRowDimension();
        v.getColumnDimension();
        vt.getRowDimension();
    }
}
```

</details>

---
## [120] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [121] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,857 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = data.consumeInt();
                break;
            case 3:
                splitTolerance = data.consumeInt(-16, 16);
                break;
            case 4:
                splitTolerance = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 5:
                splitTolerance = Double.NaN;
                break;
            case 6:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 7:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                splitTolerance = Double.MIN_VALUE;
                break;
            default:
                splitTolerance = Double.MAX_VALUE;
                break;
        }

        if (data.consumeBoolean()) {
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < main.length; i++) {
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        main[i] = 0.0d;
                        break;
                    case 1:
                        main[i] = -0.0d;
                        break;
                    case 2:
                        main[i] = i;
                        break;
                    case 3:
                        main[i] = -i;
                        break;
                    case 4:
                        main[i] = data.consumeInt();
                        break;
                    case 5:
                        main[i] = data.consumeInt(-32, 32);
                        break;
                    case 6:
                        main[i] = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 7:
                        main[i] = Double.NaN;
                        break;
                    case 8:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        main[i] = Double.MIN_VALUE;
                        break;
                    default:
                        main[i] = Double.MAX_VALUE;
                        break;
                }
            }

            for (int i = 0; i < secondary.length; i++) {
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        secondary[i] = 0.0d;
                        break;
                    case 1:
                        secondary[i] = -0.0d;
                        break;
                    case 2:
                        secondary[i] = i;
                        break;
                    case 3:
                        secondary[i] = -i;
                        break;
                    case 4:
                        secondary[i] = data.consumeInt();
                        break;
                    case 5:
                        secondary[i] = data.consumeInt(-32, 32);
                        break;
                    case 6:
                        secondary[i] = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 7:
                        secondary[i] = Double.NaN;
                        break;
                    case 8:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        secondary[i] = Double.MIN_VALUE;
                        break;
                    default:
                        secondary[i] = Double.MAX_VALUE;
                        break;
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getDeterminant();
            ed.getD();
            ed.getV();
            ed.getVT();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
            }
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    double v;
                    switch (data.consumeInt(0, 11)) {
                        case 0:
                            v = 0.0d;
                            break;
                        case 1:
                            v = -0.0d;
                            break;
                        case 2:
                            v = i - j;
                            break;
                        case 3:
                            v = j - i;
                            break;
                        case 4:
                            v = data.consumeInt();
                            break;
                        case 5:
                            v = data.consumeInt(-32, 32);
                            break;
                        case 6:
                            v = Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                            break;
                        case 7:
                            v = Double.NaN;
                            break;
                        case 8:
                            v = Double.POSITIVE_INFINITY;
                            break;
                        case 9:
                            v = Double.NEGATIVE_INFINITY;
                            break;
                        case 10:
                            v = Double.MIN_VALUE;
                            break;
                        default:
                            v = Double.MAX_VALUE;
                            break;
                    }
                    matrixData[i][j] = v;
                    matrixData[j][i] = v;
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);
            ed.getRealEigenvalues();
            ed.getImagEigenvalues();
            ed.getDeterminant();
            ed.getD();
            ed.getV();
            ed.getVT();

            int idx = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(idx);
            ed.getImagEigenvalue(idx);
            ed.getEigenvector(idx);

            if (data.consumeBoolean()) {
                DecompositionSolver solver = ed.getSolver();
                solver.isNonSingular();
            }
        }
    }
}
```

</details>

---
## [122] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.InvalidMatrixException:

---
## [123] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,816 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double splitTolerance;
        if (data.consumeBoolean()) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            splitTolerance = Double.longBitsToDouble(bits);
        } else {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.NaN;
                    break;
                case 5:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    splitTolerance = (double) data.consumeInt();
                    break;
            }
        }

        if (data.consumeBoolean()) {
            int n = data.consumeInt(0, 32);
            double[] main = new double[n];
            for (int i = 0; i < n; i++) {
                if (data.consumeBoolean()) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                } else {
                    switch (data.consumeInt(0, 9)) {
                        case 0:
                            main[i] = 0.0d;
                            break;
                        case 1:
                            main[i] = -0.0d;
                            break;
                        case 2:
                            main[i] = Double.NaN;
                            break;
                        case 3:
                            main[i] = Double.POSITIVE_INFINITY;
                            break;
                        case 4:
                            main[i] = Double.NEGATIVE_INFINITY;
                            break;
                        case 5:
                            main[i] = Integer.MIN_VALUE;
                            break;
                        case 6:
                            main[i] = Integer.MAX_VALUE;
                            break;
                        case 7:
                            main[i] = data.consumeByte();
                            break;
                        case 8:
                            main[i] = data.consumeInt(-16, 16);
                            break;
                        default:
                            main[i] = data.consumeInt();
                            break;
                    }
                }
            }

            int secondaryLength;
            if (n == 0) {
                secondaryLength = data.consumeInt(0, 2);
            } else if (data.consumeBoolean()) {
                secondaryLength = n - 1;
            } else {
                secondaryLength = Math.max(0, n - 1 + data.consumeInt(-2, 2));
            }

            double[] secondary = new double[secondaryLength];
            for (int i = 0; i < secondaryLength; i++) {
                if (data.consumeBoolean()) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                } else {
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            secondary[i] = 0.0d;
                            break;
                        case 1:
                            secondary[i] = -0.0d;
                            break;
                        case 2:
                            secondary[i] = Double.NaN;
                            break;
                        case 3:
                            secondary[i] = Double.POSITIVE_INFINITY;
                            break;
                        case 4:
                            secondary[i] = Double.NEGATIVE_INFINITY;
                            break;
                        case 5:
                            secondary[i] = data.consumeInt(-4, 4);
                            break;
                        case 6:
                            secondary[i] = data.consumeByte();
                            break;
                        default:
                            secondary[i] = data.consumeInt();
                            break;
                    }
                }
            }

            EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);
            ed.getV();
            ed.getD();
            ed.getVT();
            ed.getDeterminant();
            ed.getSolver();

            double[] real = ed.getRealEigenvalues();
            for (int i = 0; i < real.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (real.length > 0) {
                int idx = data.consumeInt(0, real.length - 1);
                ed.getRealEigenvalue(idx);
                ed.getImagEigenvalue(idx);
                ed.getEigenvector(idx);
            }
        } else {
            int n = data.consumeInt(0, 10);
            double[][] matrixData = new double[n][n];
            boolean symmetric = data.consumeBoolean();

            for (int i = 0; i < n; i++) {
                for (int j = (symmetric ? i : 0); j < n; j++) {
                    double value;
                    if (data.consumeBoolean()) {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        value = Double.longBitsToDouble(bits);
                    } else {
                        switch (data.consumeInt(0, 8)) {
                            case 0:
                                value = 0.0d;
                                break;
                            case 1:
                                value = -0.0d;
                                break;
                            case 2:
                                value = Double.NaN;
                                break;
                            case 3:
                                value = Double.POSITIVE_INFINITY;
                                break;
                            case 4:
                                value = Double.NEGATIVE_INFINITY;
                                break;
                            case 5:
                                value = data.consumeInt(-8, 8);
                                break;
                            case 6:
                                value = data.consumeByte();
                                break;
                            case 7:
                                value = Integer.MIN_VALUE;
                                break;
                            default:
                                value = Integer.MAX_VALUE;
                                break;
                        }
                    }

                    matrixData[i][j] = value;
                    if (symmetric && i != j) {
                        matrixData[j][i] = value;
                    }
                }
            }

            if (!symmetric && n > 0 && data.consumeBoolean()) {
                int i = data.consumeInt(0, n - 1);
                int j = data.consumeInt(0, n - 1);
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                matrixData[i][j] = Double.longBitsToDouble(bits);
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, splitTolerance);
            ed.getV();
            ed.getD();
            ed.getVT();
            ed.getDeterminant();
            ed.getSolver();

            double[] real = ed.getRealEigenvalues();
            for (int i = 0; i < real.length; i++) {
                ed.getRealEigenvalue(i);
                ed.getImagEigenvalue(i);
                ed.getEigenvector(i);
            }
            if (real.length > 0) {
                int idx = data.consumeInt(0, real.length - 1);
                ed.getRealEigenvalue(idx);
                ed.getImagEigenvalue(idx);
                ed.getEigenvector(idx);
            }
        }
    }
}
```

</details>

---
## [124] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.MathRuntimeException$4: matrix must have at least one row

---
## [125] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,036 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final FuzzedDataProvider d = data;

        class Gen {
            double nextDouble() {
                switch (d.consumeInt(0, 11)) {
                    case 0:
                        return 0.0d;
                    case 1:
                        return -0.0d;
                    case 2:
                        return 1.0d;
                    case 3:
                        return -1.0d;
                    case 4:
                        return Double.NaN;
                    case 5:
                        return Double.POSITIVE_INFINITY;
                    case 6:
                        return Double.NEGATIVE_INFINITY;
                    case 7:
                        return (double) d.consumeInt();
                    case 8:
                        return ((double) d.consumeInt()) / 3.0d;
                    case 9:
                        return ((double) d.consumeInt()) * ((double) d.consumeInt());
                    case 10:
                        return Math.scalb((double) d.consumeInt(), d.consumeInt(-1074, 1023));
                    default:
                        long bits = (((long) d.consumeInt()) << 32) ^ (((long) d.consumeInt()) & 0xffffffffL);
                        return Double.longBitsToDouble(bits);
                }
            }
        }

        Gen gen = new Gen();

        int n = d.consumeInt(0, 32);
        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            main[i] = gen.nextDouble();
        }

        int secondaryLength;
        if (d.consumeBoolean()) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else {
            secondaryLength = d.consumeInt(0, 32);
        }
        double[] secondary = new double[secondaryLength];
        for (int i = 0; i < secondary.length; i++) {
            secondary[i] = gen.nextDouble();
        }

        double splitTolerance = gen.nextDouble();

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (d.consumeBoolean()) {
            ed.getV();
        }
        if (d.consumeBoolean()) {
            ed.getD();
        }
        if (d.consumeBoolean()) {
            ed.getVT();
        }
        if (d.consumeBoolean()) {
            ed.getDeterminant();
        }

        double[] real = ed.getRealEigenvalues();
        double[] imag = ed.getImagEigenvalues();

        if (real != null) {
            for (int i = 0; i < real.length; i++) {
                if (d.consumeBoolean()) {
                    ed.getRealEigenvalue(i);
                }
                if (d.consumeBoolean()) {
                    ed.getImagEigenvalue(i);
                }
                if (d.consumeBoolean()) {
                    ed.getEigenvector(i);
                }
            }
        }

        if (d.consumeBoolean()) {
            DecompositionSolver solver = ed.getSolver();
            if (d.consumeBoolean()) {
                solver.isNonSingular();
            }
            if (d.consumeBoolean()) {
                int dim = real == null ? 0 : real.length;
                double[] b = new double[dim];
                for (int i = 0; i < dim; i++) {
                    b[i] = gen.nextDouble();
                }
                if (d.consumeBoolean()) {
                    solver.solve(new ArrayRealVector(b, true));
                } else {
                    solver.solve(b);
                }
            }
            if (d.consumeBoolean()) {
                int dim = real == null ? 0 : real.length;
                double[][] rhs = new double[dim][dim == 0 ? 0 : d.consumeInt(0, Math.min(8, dim + 2))];
                for (int i = 0; i < rhs.length; i++) {
                    for (int j = 0; j < rhs[i].length; j++) {
                        rhs[i][j] = gen.nextDouble();
                    }
                }
                solver.solve(new Array2DRowRealMatrix(rhs, false));
            }
            if (d.consumeBoolean()) {
                solver.getInverse();
            }
        }

        if (d.consumeBoolean()) {
            int m = d.consumeInt(0, 12);
            double[][] matrix = new double[m][m];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < m; j++) {
                    matrix[i][j] = gen.nextDouble();
                }
            }

            if (d.consumeBoolean()) {
                for (int i = 0; i < m; i++) {
                    for (int j = i + 1; j < m; j++) {
                        double v;
                        switch (d.consumeInt(0, 2)) {
                            case 0:
                                v = matrix[i][j];
                                break;
                            case 1:
                                v = matrix[j][i];
                                break;
                            default:
                                v = (matrix[i][j] + matrix[j][i]) / 2.0d;
                                break;
                        }
                        matrix[i][j] = v;
                        matrix[j][i] = v;
                    }
                }
            }

            EigenDecompositionImpl ed2 =
                new EigenDecompositionImpl(new Array2DRowRealMatrix(matrix, false), splitTolerance);

            if (d.consumeBoolean()) {
                ed2.getV();
            }
            if (d.consumeBoolean()) {
                ed2.getD();
            }
            if (d.consumeBoolean()) {
                ed2.getVT();
            }
            if (d.consumeBoolean()) {
                ed2.getDeterminant();
            }

            double[] real2 = ed2.getRealEigenvalues();
            if (real2 != null) {
                for (int i = 0; i < real2.length; i++) {
                    if (d.consumeBoolean()) {
                        ed2.getRealEigenvalue(i);
                    }
                    if (d.consumeBoolean()) {
                        ed2.getImagEigenvalue(i);
                    }
                    if (d.consumeBoolean()) {
                        ed2.getEigenvector(i);
                    }
                }
            }

            if (d.consumeBoolean()) {
                DecompositionSolver solver2 = ed2.getSolver();
                if (d.consumeBoolean()) {
                    solver2.isNonSingular();
                }
                if (d.consumeBoolean()) {
                    int dim = real2 == null ? 0 : real2.length;
                    double[] b2 = new double[dim];
                    for (int i = 0; i < dim; i++) {
                        b2[i] = gen.nextDouble();
                    }
                    solver2.solve(b2);
                }
                if (d.consumeBoolean()) {
                    solver2.getInverse();
                }
            }
        }
    }
}
```

</details>

---
## [126] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [127] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,090 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 16);

        double splitTolerance;
        switch (data.consumeInt(0, 9)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = -Double.MIN_VALUE;
                break;
            case 6:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 7:
                splitTolerance = -Double.MAX_VALUE;
                break;
            case 8:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                splitTolerance = data.consumeInt() / 1024.0d;
                break;
        }

        EigenDecompositionImpl decomposition;

        if (data.consumeBoolean()) {
            double[] main = new double[n];
            double[] secondary = new double[Math.max(0, n - 1)];

            for (int i = 0; i < main.length; i++) {
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        main[i] = 0.0d;
                        break;
                    case 1:
                        main[i] = -0.0d;
                        break;
                    case 2:
                        main[i] = 1.0d;
                        break;
                    case 3:
                        main[i] = -1.0d;
                        break;
                    case 4:
                        main[i] = Double.MIN_VALUE;
                        break;
                    case 5:
                        main[i] = -Double.MIN_VALUE;
                        break;
                    case 6:
                        main[i] = Double.MAX_VALUE;
                        break;
                    case 7:
                        main[i] = -Double.MAX_VALUE;
                        break;
                    case 8:
                        main[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        main[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        main[i] = Double.NaN;
                        break;
                    default:
                        main[i] = data.consumeInt() / ((data.consumeBoolean() ? 1.0d : 1024.0d));
                        break;
                }
            }

            for (int i = 0; i < secondary.length; i++) {
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        secondary[i] = 0.0d;
                        break;
                    case 1:
                        secondary[i] = -0.0d;
                        break;
                    case 2:
                        secondary[i] = 1.0d;
                        break;
                    case 3:
                        secondary[i] = -1.0d;
                        break;
                    case 4:
                        secondary[i] = Double.MIN_VALUE;
                        break;
                    case 5:
                        secondary[i] = -Double.MIN_VALUE;
                        break;
                    case 6:
                        secondary[i] = Double.MAX_VALUE;
                        break;
                    case 7:
                        secondary[i] = -Double.MAX_VALUE;
                        break;
                    case 8:
                        secondary[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        secondary[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        secondary[i] = Double.NaN;
                        break;
                    default:
                        secondary[i] = data.consumeInt() / ((data.consumeBoolean() ? 1.0d : 1024.0d));
                        break;
                }
            }

            decomposition = new EigenDecompositionImpl(main, secondary, splitTolerance);
        } else {
            double[][] matrixData = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i; j < n; j++) {
                    double value;
                    switch (data.consumeInt(0, 11)) {
                        case 0:
                            value = 0.0d;
                            break;
                        case 1:
                            value = -0.0d;
                            break;
                        case 2:
                            value = 1.0d;
                            break;
                        case 3:
                            value = -1.0d;
                            break;
                        case 4:
                            value = Double.MIN_VALUE;
                            break;
                        case 5:
                            value = -Double.MIN_VALUE;
                            break;
                        case 6:
                            value = Double.MAX_VALUE;
                            break;
                        case 7:
                            value = -Double.MAX_VALUE;
                            break;
                        case 8:
                            value = Double.POSITIVE_INFINITY;
                            break;
                        case 9:
                            value = Double.NEGATIVE_INFINITY;
                            break;
                        case 10:
                            value = Double.NaN;
                            break;
                        default:
                            value = data.consumeInt() / ((data.consumeBoolean() ? 1.0d : 1024.0d));
                            break;
                    }
                    matrixData[i][j] = value;
                    matrixData[j][i] = value;
                }
            }

            RealMatrix matrix = new Array2DRowRealMatrix(matrixData);
            decomposition = new EigenDecompositionImpl(matrix, splitTolerance);
        }

        decomposition.getDeterminant();
        decomposition.getD();
        decomposition.getV();
        decomposition.getVT();

        for (int i = 0; i < n; i++) {
            decomposition.getRealEigenvalue(i);
            decomposition.getImagEigenvalue(i);
            decomposition.getEigenvector(i);
        }

        DecompositionSolver solver = decomposition.getSolver();
        solver.isNonSingular();

        double[] rhs = new double[n];
        for (int i = 0; i < n; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    rhs[i] = 0.0d;
                    break;
                case 1:
                    rhs[i] = 1.0d;
                    break;
                case 2:
                    rhs[i] = -1.0d;
                    break;
                case 3:
                    rhs[i] = Double.MIN_VALUE;
                    break;
                case 4:
                    rhs[i] = -Double.MIN_VALUE;
                    break;
                case 5:
                    rhs[i] = Double.MAX_VALUE;
                    break;
                case 6:
                    rhs[i] = -Double.MAX_VALUE;
                    break;
                default:
                    rhs[i] = data.consumeInt() / 256.0d;
                    break;
            }
        }

        solver.solve(rhs);
        solver.solve(new ArrayRealVector(rhs));

        int cols = data.consumeInt(1, 4);
        double[][] rhsMatrixData = new double[n][cols];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < cols; j++) {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        rhsMatrixData[i][j] = 0.0d;
                        break;
                    case 1:
                        rhsMatrixData[i][j] = 1.0d;
                        break;
                    case 2:
                        rhsMatrixData[i][j] = -1.0d;
                        break;
                    case 3:
                        rhsMatrixData[i][j] = Double.MIN_VALUE;
                        break;
                    case 4:
                        rhsMatrixData[i][j] = Double.MAX_VALUE;
                        break;
                    default:
                        rhsMatrixData[i][j] = data.consumeInt() / 256.0d;
                        break;
                }
            }
        }

        solver.solve(new Array2DRowRealMatrix(rhsMatrixData));
        solver.getInverse();
    }
}
```

</details>

---
## [128] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.SingularMatrixException: matrix is singular

---
## [129] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,880 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int sizeSelector = data.remainingBytes() > 0 ? data.consumeInt(0, 64) : 0;
        int n = sizeSelector;

        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
            switch (kind) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        int secondaryLength;
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else if (data.remainingBytes() > 0) {
            secondaryLength = data.consumeInt(0, 64);
        } else {
            secondaryLength = n > 0 ? n - 1 : 0;
        }

        double[] secondary = new double[secondaryLength];
        for (int i = 0; i < secondaryLength; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
            switch (kind) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        if (data.remainingBytes() > 0) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.NaN;
                    break;
                case 5:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                case 8:
                    splitTolerance = Double.MAX_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }
        } else {
            splitTolerance = 0.0d;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getV();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getD();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getImagEigenvalues();
        }

        int queries = data.remainingBytes() > 0 ? data.consumeInt(0, 16) : 0;
        for (int i = 0; i < queries; i++) {
            int index;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                index = data.consumeInt();
            } else if (n > 0 && data.remainingBytes() > 0) {
                index = data.consumeInt(0, n - 1);
            } else {
                index = 0;
            }

            int action = data.remainingBytes() > 0 ? data.consumeInt(0, 4) : 0;
            switch (action) {
                case 0:
                    ed.getRealEigenvalue(index);
                    break;
                case 1:
                    ed.getImagEigenvalue(index);
                    break;
                case 2:
                    ed.getEigenvector(index);
                    break;
                case 3:
                    ed.getSolver();
                    break;
                default:
                    ed.hasComplexEigenvalues();
                    break;
            }
        }
    }
}
```

</details>

---
## [130] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [131] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,235 chars, 2 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
**[assistant]**
```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int sizeSelector = data.remainingBytes() > 0 ? data.consumeInt(0, 64) : 0;
        int n = sizeSelector;

        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
            switch (kind) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        int secondaryLength;
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else if (data.remainingBytes() > 0) {
            secondaryLength = data.consumeInt(0, 64);
        } else {
            secondaryLength = n > 0 ? n - 1 : 0;
        }

        double[] secondary = new double[secondaryLength];
        for (int i = 0; i < secondaryLength; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
            switch (kind) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        if (data.remainingBytes() > 0) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.NaN;
                    break;
                case 5:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                case 8:
                    splitTolerance = Double.MAX_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }
        } else {
            splitTolerance = 0.0d;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getV();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getD();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getImagEigenvalues();
        }

        int queries = data.remainingBytes() > 0 ? data.consumeInt(0, 16) : 0;
        for (int i = 0; i < queries; i++) {
            int index;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                index = data.consumeInt();
            } else if (n > 0 && data.remainingBytes() > 0) {
                index = data.consumeInt(0, n - 1);
            } else {
                index = 0;
            }

            int action = data.remainingBytes() > 0 ? data.consumeInt(0, 4) : 0;
            switch (action) {
                case 0:
                    ed.getRealEigenvalue(index);
                    break;
                case 1:
                    ed.getImagEigenvalue(index);
                    break;
                case 2:
                    ed.getEigenvector(index);
                    break;
                case 3:
                    ed.getSolver();
                    break;
                default:
                    ed.hasComplexEigenvalues();
                    break;
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/Math_80_buggy/fuzz/attempt_027/FuzzHarness.java:194: error: cannot find symbol
                    ed.hasComplexEigenvalues();
                      ^
  symbol:   method hasComplexEigenvalues()
  location: variable ed of type EigenDecompositionImpl
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
<details open><summary>▸ Output (~6,844 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.remainingBytes() > 0 ? data.consumeInt(0, 64) : 0;

        double[] main = new double[n];
        for (int i = 0; i < n; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
            switch (kind) {
                case 0:
                    main[i] = 0.0d;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = 1.0d;
                    break;
                case 3:
                    main[i] = -1.0d;
                    break;
                case 4:
                    main[i] = Double.NaN;
                    break;
                case 5:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    main[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    main[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    main[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    main[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        int secondaryLength;
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            secondaryLength = n > 0 ? n - 1 : 0;
        } else if (data.remainingBytes() > 0) {
            secondaryLength = data.consumeInt(0, 64);
        } else {
            secondaryLength = n > 0 ? n - 1 : 0;
        }

        double[] secondary = new double[secondaryLength];
        for (int i = 0; i < secondaryLength; i++) {
            int kind = data.remainingBytes() > 0 ? data.consumeInt(0, 11) : 0;
            switch (kind) {
                case 0:
                    secondary[i] = 0.0d;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = 1.0d;
                    break;
                case 3:
                    secondary[i] = -1.0d;
                    break;
                case 4:
                    secondary[i] = Double.NaN;
                    break;
                case 5:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    secondary[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    secondary[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    secondary[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    secondary[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double splitTolerance;
        if (data.remainingBytes() > 0) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
                case 0:
                    splitTolerance = 0.0d;
                    break;
                case 1:
                    splitTolerance = -0.0d;
                    break;
                case 2:
                    splitTolerance = 1.0d;
                    break;
                case 3:
                    splitTolerance = -1.0d;
                    break;
                case 4:
                    splitTolerance = Double.NaN;
                    break;
                case 5:
                    splitTolerance = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    splitTolerance = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    splitTolerance = Double.MIN_VALUE;
                    break;
                case 8:
                    splitTolerance = Double.MAX_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    splitTolerance = Double.longBitsToDouble(bits);
                    break;
            }
        } else {
            splitTolerance = 0.0d;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getV();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getD();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getVT();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getDeterminant();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getRealEigenvalues();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getImagEigenvalues();
        }
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            ed.getSolver();
        }

        int queries = data.remainingBytes() > 0 ? data.consumeInt(0, 16) : 0;
        for (int i = 0; i < queries; i++) {
            int index;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                index = data.consumeInt();
            } else if (n > 0 && data.remainingBytes() > 0) {
                index = data.consumeInt(0, n - 1);
            } else {
                index = 0;
            }

            int action = data.remainingBytes() > 0 ? data.consumeInt(0, 3) : 0;
            switch (action) {
                case 0:
                    ed.getRealEigenvalue(index);
                    break;
                case 1:
                    ed.getImagEigenvalue(index);
                    break;
                case 2:
                    ed.getEigenvector(index);
                    break;
                default:
                    ed.getSolver();
                    break;
            }
        }
    }
}
```

</details>

---
## [132] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [133] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,087 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(1, 32);

        double[] main = new double[n];
        double[] secondary = new double[Math.max(0, n - 1)];

        for (int i = 0; i < main.length; i++) {
            int mode = data.consumeInt(0, 11);
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (mode) {
                case 0:
                    main[i] = 0.0;
                    break;
                case 1:
                    main[i] = -0.0d;
                    break;
                case 2:
                    main[i] = a;
                    break;
                case 3:
                    main[i] = (double) a / (double) (b == 0 ? 1 : b);
                    break;
                case 4:
                    main[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 5:
                    main[i] = Double.NaN;
                    break;
                case 6:
                    main[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    main[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    main[i] = Math.abs(a);
                    break;
                case 9:
                    main[i] = -Math.abs(a);
                    break;
                case 10:
                    main[i] = (a % 2 == 0) ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    main[i] = (a % 2 == 0) ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
            }
        }

        for (int i = 0; i < secondary.length; i++) {
            int mode = data.consumeInt(0, 11);
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (mode) {
                case 0:
                    secondary[i] = 0.0;
                    break;
                case 1:
                    secondary[i] = -0.0d;
                    break;
                case 2:
                    secondary[i] = a;
                    break;
                case 3:
                    secondary[i] = (double) a / (double) (b == 0 ? 1 : b);
                    break;
                case 4:
                    secondary[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 5:
                    secondary[i] = Double.NaN;
                    break;
                case 6:
                    secondary[i] = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    secondary[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    secondary[i] = Math.abs(a);
                    break;
                case 9:
                    secondary[i] = -Math.abs(a);
                    break;
                case 10:
                    secondary[i] = (a % 2 == 0) ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    secondary[i] = (a % 2 == 0) ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
            }
        }

        double splitTolerance;
        switch (data.consumeInt(0, 7)) {
            case 0:
                splitTolerance = 0.0;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = data.consumeInt();
                break;
            case 3:
                splitTolerance = (double) data.consumeInt() / (double) (data.consumeInt() == 0 ? 1 : data.consumeInt());
                break;
            case 4:
                splitTolerance = Double.NaN;
                break;
            case 5:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            case 6:
                splitTolerance = Double.NEGATIVE_INFINITY;
                break;
            default:
                int a = data.consumeInt();
                int b = data.consumeInt();
                splitTolerance = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                break;
        }

        EigenDecompositionImpl ed = new EigenDecompositionImpl(main, secondary, splitTolerance);

        ed.getRealEigenvalues();
        ed.getImagEigenvalues();
        ed.getDeterminant();
        ed.getV();
        ed.getD();
        ed.getVT();

        if (n > 0) {
            int index = data.consumeInt(0, n - 1);
            ed.getRealEigenvalue(index);
            ed.getImagEigenvalue(index);
            ed.getEigenvector(index);
        }

        if (data.consumeBoolean()) {
            double[] main2 = main.clone();
            double[] secondary2 = secondary.clone();

            for (int i = 0; i < main2.length / 2; i++) {
                double t = main2[i];
                main2[i] = main2[main2.length - 1 - i];
                main2[main2.length - 1 - i] = t;
            }
            for (int i = 0; i < secondary2.length / 2; i++) {
                double t = secondary2[i];
                secondary2[i] = secondary2[secondary2.length - 1 - i];
                secondary2[secondary2.length - 1 - i] = t;
            }

            EigenDecompositionImpl ed2 = new EigenDecompositionImpl(main2, secondary2, splitTolerance);
            ed2.getRealEigenvalues();
            ed2.getImagEigenvalues();
            ed2.getDeterminant();
            ed2.getV();
            ed2.getD();
            ed2.getVT();

            if (n > 0) {
                int index2 = data.consumeInt(0, n - 1);
                ed2.getRealEigenvalue(index2);
                ed2.getImagEigenvalue(index2);
                ed2.getEigenvector(index2);
            }
        }
    }
}
```

</details>

---
## [134] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: org.apache.commons.math.linear.InvalidMatrixException:

---
## [135] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [77] — not reprinted*
- *[user] message: identical to step [77] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,206 chars)</summary>

```
package org.apache.commons.math.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);

        double[] main = new double[n];
        double[] secondary = new double[n > 0 ? n - 1 : 0];
        double[] rhs = new double[n];

        for (int i = 0; i < n; i++) {
            int selector = data.consumeInt(0, 11);
            double value;
            switch (selector) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = -0.0d;
                    break;
                case 2:
                    value = Double.NaN;
                    break;
                case 3:
                    value = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    value = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    value = Double.MIN_VALUE;
                    break;
                case 6:
                    value = -Double.MIN_VALUE;
                    break;
                case 7:
                    value = Double.MAX_VALUE;
                    break;
                case 8:
                    value = -Double.MAX_VALUE;
                    break;
                case 9:
                    value = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
                case 10:
                    value = (double) data.consumeInt();
                    break;
                default:
                    value = (double) data.consumeByte();
                    break;
            }
            main[i] = value;
        }

        for (int i = 0; i < secondary.length; i++) {
            int selector = data.consumeInt(0, 11);
            double value;
            switch (selector) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = -0.0d;
                    break;
                case 2:
                    value = Double.NaN;
                    break;
                case 3:
                    value = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    value = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    value = Double.MIN_VALUE;
                    break;
                case 6:
                    value = -Double.MIN_VALUE;
                    break;
                case 7:
                    value = Double.MAX_VALUE;
                    break;
                case 8:
                    value = -Double.MAX_VALUE;
                    break;
                case 9:
                    value = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
                case 10:
                    value = (double) data.consumeInt();
                    break;
                default:
                    value = (double) data.consumeByte();
                    break;
            }
            secondary[i] = value;
        }

        for (int i = 0; i < rhs.length; i++) {
            int selector = data.consumeInt(0, 5);
            double value;
            switch (selector) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = 1.0d;
                    break;
                case 2:
                    value = -1.0d;
                    break;
                case 3:
                    value = (double) data.consumeInt();
                    break;
                case 4:
                    value = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                    );
                    break;
                default:
                    value = (double) data.consumeByte();
                    break;
            }
            rhs[i] = value;
        }

        double splitTolerance;
        switch (data.consumeInt(0, 8)) {
            case 0:
                splitTolerance = 0.0d;
                break;
            case 1:
                splitTolerance = -0.0d;
                break;
            case 2:
                splitTolerance = 1.0d;
                break;
            case 3:
                splitTolerance = -1.0d;
                break;
            case 4:
                splitTolerance = Double.MIN_VALUE;
                break;
            case 5:
                splitTolerance = Double.MAX_VALUE;
                break;
            case 6:
                splitTolerance = Double.NaN;
                break;
            case 7:
                splitTolerance = Double.POSITIVE_INFINITY;
                break;
            default:
                splitTolerance = Double.longBitsToDouble(
                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                );
                break;
        }

        EigenDecompositionImpl eig = new EigenDecompositionImpl(main, secondary, splitTolerance);

        eig.getD();
        eig.getV();
        eig.getVT();
        eig.getDeterminant();

        double[] real = eig.getRealEigenvalues();
        double[] imag = eig.getImagEigenvalues();

        for (int i = 0; i < real.length; i++) {
            eig.getRealEigenvalue(i);
        }
        for (int i = 0; i < imag.length; i++) {
            eig.getImagEigenvalue(i);
        }

        if (n > 0) {
            int idx = data.consumeInt(0, n - 1);
            eig.getRealEigenvalue(idx);
            eig.getImagEigenvalue(idx);
            eig.getEigenvector(idx);
        }

        DecompositionSolver solver = eig.getSolver();
        solver.isNonSingular();
        if (n > 0) {
            solver.solve(rhs);
        }
        solver.getInverse();
    }
}
```

</details>

---
## [136] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [137] ⚙️ coverage · `jazzer coverage dumps`
**output:** **24/24 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov; 24 raw fuzz log(s)**
<details><summary>▸ detail (~13,752 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 754}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 444}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 249}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 940}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 251}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 748}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 1045}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 443}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 758}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 686}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 249}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 256}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 915}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 444}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 1259}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 252}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 672}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 248}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_015', 'attempt_018', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_015', 'attempt_018', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN_C/010_patch1-Math-80-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
