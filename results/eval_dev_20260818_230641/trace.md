# Pipeline trace — Math-79

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** overfit MISSED (all harnesses quiet on the patched build). [evaluated; 5 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/math/util/MathUtils.java
+++ /src/main/java/org/apache/commons/math/util/MathUtils.java
@@ -1623,7 +1623,7 @@
     public static double distance(int[] p1, int[] p2) {
       int sum = 0;
       for (int i = 0; i < p1.length; i++) {
-          final int dp = p1[i] - p2[i];
+          final double dp = p1[i] - p2[i];
           sum += dp * dp;
       }
       return Math.sqrt(sum);
```

23 sequential steps — 5 LLM calls, 18 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.math.stat.clustering.KMeansPlusPlusClustererTest', test_m (~1,405 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.math.stat.clustering.KMeansPlusPlusClustererTest', test_method='testPerformClusterAnalysisDegenerate', source_path='/tmp/d4j/Math_79_buggy/src/test/java/org/apache/commons/math/stat/clustering/KMeansPlusPlusClustererTest.java', method_source='    @Test\\n    public void testPerformClusterAnalysisDegenerate() {\\n        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer = new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(\\n                new Random(1746432956321l));\\n        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {\\n                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),\\n                new EuclideanIntegerPoint(new int[] { 1960, 373200 }), };\\n        List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);\\n        assertEquals(1, clusters.size());\\n        assertEquals(2, (clusters.get(0).getPoints().size()));\\n        EuclideanIntegerPoint pt1 = new EuclideanIntegerPoint(new int[] { 1959, 325100 });\\n        EuclideanIntegerPoint pt2 = new EuclideanIntegerPoint(new int[] { 1960, 373200 });\\n        assertTrue(clusters.get(0).getPoints().contains(pt1));\\n        assertTrue(clusters.get(0).getPoints().contains(pt2));\\n\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.math.stat.clustering.KMeansPlusPlusClustererTest::testPerformClusterAnalysisDegenerate",
    "failure_message": "--- org.apache.commons.math.stat.clustering.KMeansPlusPlusClustererTest::testPerformClusterAnalysisDegenerate\njava.lang.NullPointerException",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~1,966 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/math/util/MathUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/math/util/MathUtils.java\n+++ /src/main/java/org/apache/commons/math/util/MathUtils.java\n@@ -1623,7 +1623,7 @@\n     public static double distance(int[] p1, int[] p2) {\n       int sum = 0;\n       for (int i = 0; i < p1.length; i++) {\n-          final int dp = p1[i] - p2[i];\n+          final double dp = p1[i] - p2[i];\n           sum += dp * dp;\n       }\n       return Math.sqrt(sum);\n",
  "functions": [
    {
      "func_name": "distance",
      "func_signature": "public static double distance(int[] p1, int[] p2)",
      "func_source": "    public static double distance(int[] p1, int[] p2) {\n      int sum = 0;\n      for (int i = 0; i < p1.length; i++) {\n          final int dp = p1[i] - p2[i];\n          sum += dp * dp;\n      }\n      return Math.sqrt(sum);\n    }",
      "func_class": "MathUtils",
      "func_class_fq": "org.apache.commons.math.util.MathUtils",
      "func_param_types": [
        "int[]",
        "int[]"
      ],
      "fi_name": "[org.apache.commons.math.util.MathUtils].distance(int[],int[])",
      "overload_types": [
        [
          "double[]",
          "double[]"
        ],
        [
          "int[]",
          "int[]"
        ]
      ],
      "xrefs": [
        "public double distanceFrom(final EuclideanIntegerPoint p) {\n        return MathUtils.distance(point, p.getPoint());\n    }"
      ],
      "reachable": [
        "Math.sqrt(org.apache.commons.math.util.MathUtils)"
      ],
      "related_callees": [],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.util",
  "root_cause_reachable": [],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.math.BigDecimal;",
    "import java.math.BigInteger;",
    "import java.util.Arrays;",
    "import org.apache.commons.math.MathRuntimeException;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~12,746 chars)</summary>

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
- Package: `org.apache.commons.math.util` (`package org.apache.commons.math.util;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Math_79_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/math/util/MathUtils.java
+++ /src/main/java/org/apache/commons/math/util/MathUtils.java
@@ -1623,7 +1623,7 @@
     public static double distance(int[] p1, int[] p2) {
       int sum = 0;
       for (int i = 0; i < p1.length; i++) {
-          final int dp = p1[i] - p2[i];
+          final double dp = p1[i] - p2[i];
           sum += dp * dp;
       }
       return Math.sqrt(sum);

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import org.apache.commons.math.MathRuntimeException;
</source_imports>

Function `distance`:
<signature>
public static double distance(int[] p1, int[] p2)
</signature>
<code>
    public static double distance(int[] p1, int[] p2) {
      int sum = 0;
      for (int i = 0; i < p1.length; i++) {
          final int dp = p1[i] - p2[i];
          sum += dp * dp;
      }
      return Math.sqrt(sum);
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public double distanceFrom(final EuclideanIntegerPoint p) {
        return MathUtils.distance(point, p.getPoint());
    }
</xref>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `KMeansPlusPlusClusterer`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `distance` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is itself a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer.assignPointsToClusters(KMeansPlusPlusClusterer.java:91)
</ground_truth_crash>
<failing_test class="org.apache.commons.math.stat.clustering.KMeansPlusPlusClustererTest" method="testPerformClusterAnalysisDegenerate">
    @Test
    public void testPerformClusterAnalysisDegenerate() {
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer = new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(
                new Random(1746432956321l));
        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 }), };
        List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);
        assertEquals(1, clusters.size());
        assertEquals(2, (clusters.get(0).getPoints().size()));
        EuclideanIntegerPoint pt1 = new EuclideanIntegerPoint(new int[] { 1959, 325100 });
        EuclideanIntegerPoint pt2 = new EuclideanIntegerPoint(new int[] { 1960, 373200 });
        assertTrue(clusters.get(0).getPoints().contains(pt1));
        assertTrue(clusters.get(0).getPoints().contains(pt2));

    }
</failing_test>

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
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~9,064 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint> transformer =
                    new org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint>(
                            new java.util.Random(1746432956321L));
            org.apache.commons.math.stat.clustering.EuclideanIntegerPoint pt1 =
                    new org.apache.commons.math.stat.clustering.EuclideanIntegerPoint(new int[] { 1959, 325100 });
            org.apache.commons.math.stat.clustering.EuclideanIntegerPoint pt2 =
                    new org.apache.commons.math.stat.clustering.EuclideanIntegerPoint(new int[] { 1960, 373200 });
            java.util.List<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint> input =
                    java.util.Arrays.asList(new org.apache.commons.math.stat.clustering.EuclideanIntegerPoint[] { pt1, pt2 });
            try {
                java.util.List<org.apache.commons.math.stat.clustering.Cluster<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint>> clusters =
                        transformer.cluster(input, 1, 1);

                /* Contract/oracle: clustering 2 non-null points into k=1 cluster must return exactly one cluster
                 * containing all input points. A "fix" that merely skips the crashing assignment path can return
                 * a malformed result that omits points or produces the wrong number of clusters.
                 */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: single-cluster result size input=2 lhs=" +
                            (clusters == null ? "null" : String.valueOf(clusters.size())) + " rhs=1");
                }
                java.util.List<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
                if (assigned == null || assigned.size() != 2 || !assigned.contains(pt1) || !assigned.contains(pt2)) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: sole cluster must contain all input points input=2 lhs=" +
                            String.valueOf(assigned) + " rhs=[" + String.valueOf(pt1) + "," + String.valueOf(pt2) + "]");
                }
            } catch (Throwable t) {
                boolean inPatchedRegion = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn) && "assignPointsToClusters".equals(mn))
                            || ("org.apache.commons.math.util.MathUtils".equals(cn) && "distance".equals(mn))) {
                        inPatchedRegion = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && inPatchedRegion) {
                    throw (NullPointerException) t;
                }
                return;
            }
        }

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases += data.consumeInt(1, 4);
        }

        for (int c = 0; c < cases; c++) {
            int dim = 1;
            int numPoints = 2;
            if (data.remainingBytes() > 0) {
                dim = data.consumeInt(1, 6);
            }
            if (data.remainingBytes() > 0) {
                numPoints = data.consumeInt(2, 6);
            }

            int[][] coords = new int[numPoints][dim];
            for (int i = 0; i < numPoints; i++) {
                for (int j = 0; j < dim; j++) {
                    if (data.remainingBytes() > 0) {
                        coords[i][j] = data.consumeInt();
                    } else {
                        coords[i][j] = 0;
                    }
                }
            }

            int hotDim = 0;
            if (dim > 1 && data.remainingBytes() > 0) {
                hotDim = data.consumeInt(0, dim - 1);
            }

            int base;
            if (data.remainingBytes() > 0) {
                base = data.consumeInt(-1000000, 1000000);
            } else {
                base = 0;
            }
            int deltaMag;
            if (data.remainingBytes() > 0) {
                deltaMag = data.consumeInt(46341, 100000);
            } else {
                deltaMag = 50000;
            }
            int deltaSign = 1;
            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                deltaSign = -1;
            }
            int delta = deltaSign * deltaMag;

            coords[0][hotDim] = base;
            coords[1][hotDim] = base + delta;

            if (numPoints > 2) {
                for (int i = 2; i < numPoints; i++) {
                    if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                        coords[i][hotDim] = coords[i - 1][hotDim];
                    }
                }
            }

            java.util.ArrayList<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint> input =
                    new java.util.ArrayList<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint>(numPoints);
            for (int i = 0; i < numPoints; i++) {
                input.add(new org.apache.commons.math.stat.clustering.EuclideanIntegerPoint(coords[i]));
            }

            java.util.Random rnd;
            if (data.remainingBytes() > 0) {
                rnd = new java.util.Random((long) data.consumeInt());
            } else {
                rnd = new java.util.Random(1L);
            }

            org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint> transformer =
                    new org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint>(rnd);

            try {
                java.util.List<org.apache.commons.math.stat.clustering.Cluster<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint>> clusters =
                        transformer.cluster(input, 1, 1);

                /* Contract/oracle: for any valid non-empty input and k=1, the public API must return one cluster
                 * containing every input point exactly as cluster members. A patch that suppresses the faulty path
                 * without performing assignment correctly breaks this observable result even if it no longer throws.
                 */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-explore] metamorphic violation: single-cluster result size input=" +
                            input + " lhs=" + (clusters == null ? "null" : String.valueOf(clusters.size())) + " rhs=1");
                }
                java.util.List<org.apache.commons.math.stat.clustering.EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
                if (assigned == null || assigned.size() != input.size()) {
                    throw new RuntimeException("[oracle:k1-explore] metamorphic violation: sole cluster cardinality input=" +
                            input + " lhs=" + String.valueOf(assigned) + " rhsSize=" + input.size());
                }
                for (int i = 0; i < input.size(); i++) {
                    if (!assigned.contains(input.get(i))) {
                        throw new RuntimeException("[oracle:k1-explore] metamorphic violation: sole cluster must contain all input points input=" +
                                input + " lhs=" + assigned + " missing=" + input.get(i));
                    }
                }
            } catch (Throwable t) {
                boolean inPatchedRegion = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn) && "assignPointsToClusters".equals(mn))
                            || ("org.apache.commons.math.util.MathUtils".equals(cn) && "distance".equals(mn))) {
                        inPatchedRegion = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && inPatchedRegion) {
                    throw (NullPointerException) t;
                }
                return;
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
- trigger: java.lang.NullPointerException

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,807 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        List<EuclideanIntegerPoint> points = buildFuzzPoints(data);
        runClusterAndCheck(points);
    }

    private static void runAnchor() {
        List<EuclideanIntegerPoint> points = Arrays.asList(
                new EuclideanIntegerPoint(new int[] {1959, 325100}),
                new EuclideanIntegerPoint(new int[] {1960, 373200})
        );
        runClusterAndCheck(points);
    }

    private static List<EuclideanIntegerPoint> buildFuzzPoints(FuzzedDataProvider data) {
        int dims = data.consumeInt(1, 6);
        int count = data.consumeInt(2, 8);
        int hotA = data.consumeBoolean() ? Integer.MIN_VALUE : data.consumeInt();
        int hotB;
        if (data.consumeBoolean()) {
            hotB = Integer.MAX_VALUE;
        } else {
            hotB = hotA + (hotA >= 0 ? -50000 : 50000);
        }
        int hotDim = data.consumeInt(0, dims - 1);
        int posA = data.consumeInt(0, count - 1);
        int posB = data.consumeInt(0, count - 1);
        if (posB == posA) {
            posB = (posA + 1) % count;
        }

        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(count);
        for (int i = 0; i < count; i++) {
            int[] coords = new int[dims];
            for (int d = 0; d < dims; d++) {
                coords[d] = data.consumeInt();
            }
            points.add(new EuclideanIntegerPoint(coords));
        }

        int[] a = copyPoint(points.get(posA).getPoint());
        int[] b = copyPoint(points.get(posB).getPoint());
        a[hotDim] = hotA;
        b[hotDim] = hotB;
        points.set(posA, new EuclideanIntegerPoint(a));
        points.set(posB, new EuclideanIntegerPoint(b));

        if (data.consumeBoolean()) {
            int[] seed1 = new int[dims];
            int[] seed2 = new int[dims];
            seed1[0] = 1959;
            seed2[0] = 1960;
            if (dims > 1) {
                seed1[1] = 325100;
                seed2[1] = 373200;
            }
            points.set(0, new EuclideanIntegerPoint(seed1));
            if (count > 1) {
                points.set(1, new EuclideanIntegerPoint(seed2));
            }
        }

        return points;
    }

    private static int[] copyPoint(int[] p) {
        int[] copy = new int[p.length];
        System.arraycopy(p, 0, copy, 0, p.length);
        return copy;
    }

    private static void runClusterAndCheck(List<EuclideanIntegerPoint> points) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(points, 1, 1);

            /* Contract/oracle: with k == 1, clustering must produce exactly one cluster and every
             * input point must be assigned to that single cluster. A patch that merely suppresses
             * the failing path or skips assignment/bookkeeping could return the wrong cluster count
             * or lose points without throwing.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:k1-single] metamorphic violation: k=1 must yield exactly one cluster inputSize="
                        + points.size() + " clusters=" + (clusters == null ? "null" : Integer.toString(clusters.size())));
            }
            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null) {
                throw new RuntimeException("[oracle:k1-points] metamorphic violation: single cluster must expose assigned points inputSize="
                        + points.size() + " cluster=" + only);
            }
            if (only.getPoints().size() != points.size()) {
                throw new RuntimeException("[oracle:k1-assign] metamorphic violation: all input points must be assigned for k=1 inputSize="
                        + points.size() + " assigned=" + only.getPoints().size() + " input=" + points + " assignedPoints=" + only.getPoints());
            }
            for (EuclideanIntegerPoint p : points) {
                if (!only.getPoints().contains(p)) {
                    throw new RuntimeException("[oracle:k1-membership] metamorphic violation: single cluster must contain each input point missing="
                            + p + " input=" + points + " assignedPoints=" + only.getPoints());
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwAsUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("MathRuntimeException")
                    || name.contains("Invalid")
                    || name.contains("NoData")
                    || name.contains("NullArgument")
                    || name.contains("OutOfRange")
                    || name.contains("DimensionMismatch"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(method))
                    || ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(method))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAsUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,958 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int cases = 1 + Math.min(3, Math.max(0, data.remainingBytes() / 8));
        for (int i = 0; i < cases; i++) {
            List<EuclideanIntegerPoint> points = buildOverflowTriggeringPoints(data);
            long seed = data.consumeInt();
            runClusterCase(points, seed);
        }
    }

    private static void runAnchor() {
        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(2);
        points.add(new EuclideanIntegerPoint(new int[] {1959, 325100}));
        points.add(new EuclideanIntegerPoint(new int[] {1960, 373200}));
        runClusterCase(points, 1746432956321L);
    }

    private static List<EuclideanIntegerPoint> buildOverflowTriggeringPoints(FuzzedDataProvider data) {
        int dims = data.consumeInt(1, 6);
        int pointCount = data.consumeInt(2, 6);
        int triggerIndex = data.consumeInt(0, dims - 1);

        int[] a = new int[dims];
        int[] b = new int[dims];
        for (int i = 0; i < dims; i++) {
            a[i] = data.consumeInt();
            b[i] = data.consumeInt();
        }

        int magnitude = 46341 + Math.abs(data.consumeInt() % 1000000);
        if (data.consumeBoolean()) {
            a[triggerIndex] = 0;
            b[triggerIndex] = magnitude;
        } else {
            a[triggerIndex] = 0;
            b[triggerIndex] = -magnitude;
        }

        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(pointCount);
        points.add(new EuclideanIntegerPoint(a));
        points.add(new EuclideanIntegerPoint(b));

        for (int p = 2; p < pointCount; p++) {
            int[] extra = new int[dims];
            for (int d = 0; d < dims; d++) {
                extra[d] = data.consumeInt();
            }
            if (data.consumeBoolean()) {
                extra[triggerIndex] = a[triggerIndex];
            } else if (data.consumeBoolean()) {
                extra[triggerIndex] = b[triggerIndex];
            }
            points.add(new EuclideanIntegerPoint(extra));
        }

        return points;
    }

    private static void runClusterCase(List<EuclideanIntegerPoint> points, long seed) {
        List<Cluster<EuclideanIntegerPoint>> clusters;
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));
            clusters = clusterer.cluster(points, 1, 1);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
            return;
        }

        /*
         * Contract/oracle:
         * KMeans clustering assigns each input point to a cluster. With k == 1 and a non-empty
         * valid input list, there must be exactly one resulting cluster, and that sole cluster
         * must contain exactly the multiset of input points. A "fix" that merely skips the faulty
         * assignment/update path would violate this observable post-condition without throwing.
         */
        if (clusters == null || clusters.size() != 1) {
            throw new RuntimeException("[oracle:k1-partition] metamorphic violation: expected exactly one cluster inputSize="
                    + points.size() + " actualClusters=" + (clusters == null ? "null" : Integer.toString(clusters.size())));
        }

        List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
        if (!sameMultiset(points, assigned)) {
            throw new RuntimeException("[oracle:k1-partition] metamorphic violation: sole cluster must contain exactly all input points input="
                    + points + " assigned=" + assigned);
        }
    }

    private static boolean sameMultiset(List<EuclideanIntegerPoint> a, List<EuclideanIntegerPoint> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        Map<EuclideanIntegerPoint, Integer> counts = new HashMap<EuclideanIntegerPoint, Integer>();
        for (EuclideanIntegerPoint p : a) {
            Integer c = counts.get(p);
            counts.put(p, c == null ? 1 : c + 1);
        }
        for (EuclideanIntegerPoint p : b) {
            Integer c = counts.get(p);
            if (c == null) {
                return false;
            }
            if (c == 1) {
                counts.remove(p);
            } else {
                counts.put(p, c - 1);
            }
        }
        return counts.isEmpty();
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(method))
                    || ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(method))) {
                return true;
            }
        }
        return false;
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
- trigger: java.lang.NullPointerException

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,652 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            anchor();
        } catch (RuntimeException t) {
            if (isBugNpe(t)) {
                throw t;
            }
        }

        int dimension = 1 + data.consumeInt(0, 3);
        int pointCount = 2 + data.consumeInt(0, 6);
        int[][] raw = new int[pointCount][dimension];

        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < dimension; j++) {
                raw[i][j] = data.consumeInt();
            }
        }

        int pA = data.consumeInt(0, pointCount - 1);
        int pB = data.consumeInt(0, pointCount - 1);
        if (pA == pB) {
            pB = (pB + 1) % pointCount;
        }
        int coord = data.consumeInt(0, dimension - 1);

        int base = data.consumeInt(-100000, 100000);
        int delta = 46341 + data.consumeInt(0, 1000000);
        if (data.consumeBoolean()) {
            delta = -delta;
        }
        raw[pA][coord] = base;
        raw[pB][coord] = base + delta;

        if (data.consumeBoolean()) {
            for (int j = 0; j < dimension; j++) {
                if (j != coord) {
                    raw[pB][j] = raw[pA][j];
                }
            }
        }

        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            points.add(new EuclideanIntegerPoint(raw[i]));
        }

        exercise(points, new Random(1746432956321L + data.consumeInt()));
        if (data.consumeBoolean()) {
            List<EuclideanIntegerPoint> shuffled = new ArrayList<EuclideanIntegerPoint>(points);
            Collections.shuffle(shuffled, new Random(data.consumeInt()));
            exercise(shuffled, new Random(1746432956321L + data.consumeInt()));
        }
    }

    private static void anchor() {
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 }),
        };
        List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);
        assertSingleClusterContainsAll(Arrays.asList(points), clusters);
    }

    private static void exercise(List<EuclideanIntegerPoint> points, Random rnd) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(rnd);
            List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(points, 1, 1);

            /*
             * Contract/invariant used as the oracle:
             * clustering a non-empty collection into k=1 cluster must return exactly one cluster,
             * and every input point must be assigned to that single cluster exactly once.
             * A "fix" that merely skips the failing assignment path or drops bookkeeping can avoid
             * the crash but will violate this observable result.
             */
            assertSingleClusterContainsAll(points, clusters);
        } catch (RuntimeException t) {
            if (isBugNpe(t)) {
                throw t;
            }
        }
    }

    private static void assertSingleClusterContainsAll(List<EuclideanIntegerPoint> points,
                                                       List<Cluster<EuclideanIntegerPoint>> clusters) {
        if (clusters == null || clusters.size() != 1) {
            throw new RuntimeException("[oracle:k1-size] metamorphic violation: k=1 must yield exactly one cluster inputSize="
                    + points.size() + " clusterCount=" + (clusters == null ? "null" : String.valueOf(clusters.size())));
        }

        List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
        int assignedCount = assigned == null ? -1 : assigned.size();
        if (assigned == null || assignedCount != points.size()) {
            throw new RuntimeException("[oracle:k1-assign] metamorphic violation: all input points must be assigned to the single cluster inputSize="
                    + points.size() + " assignedSize=" + assignedCount + " input=" + points + " assigned=" + assigned);
        }
    }

    private static boolean isBugNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement f = stack[i];
            String cls = f.getClassName();
            String m = f.getMethodName();
            if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(m))
                || ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(m))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,349 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        EuclideanIntegerPoint[] anchor = new EuclideanIntegerPoint[] {
            new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
            new EuclideanIntegerPoint(new int[] { 1960, 373200 }),
        };
        runCase(anchor, 1746432956321L);

        int cases = 1 + data.consumeInt(0, 3);
        for (int c = 0; c < cases; c++) {
            int dims = 1 + data.consumeInt(0, 5);
            int numPoints = 2 + data.consumeInt(0, 4);
            EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[numPoints];

            for (int i = 0; i < numPoints; i++) {
                int[] coords = new int[dims];
                for (int d = 0; d < dims; d++) {
                    coords[d] = data.consumeInt();
                }
                points[i] = new EuclideanIntegerPoint(coords);
            }

            int p1 = data.consumeInt(0, numPoints - 1);
            int p2 = data.consumeInt(0, numPoints - 1);
            if (p1 == p2) {
                p2 = (p2 + 1) % numPoints;
            }
            int overflowDim = data.consumeInt(0, dims - 1);

            int delta = 46341 + data.consumeInt(0, 200000);
            int sign = data.consumeBoolean() ? 1 : -1;

            int[] a = points[p1].getPoint().clone();
            int[] b = points[p2].getPoint().clone();

            a[overflowDim] = 0;
            b[overflowDim] = sign * delta;

            for (int d = 0; d < dims; d++) {
                if (d != overflowDim) {
                    if (data.consumeBoolean()) {
                        a[d] = data.consumeInt();
                    }
                    if (data.consumeBoolean()) {
                        b[d] = data.consumeInt();
                    }
                }
            }

            points[p1] = new EuclideanIntegerPoint(a);
            points[p2] = new EuclideanIntegerPoint(b);

            long seed = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            runCase(points, seed);
        }
    }

    private static void runCase(EuclideanIntegerPoint[] points, long seed) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(Arrays.asList(points), 1, 1);

            /*
             * Contract/oracle: clustering with k=1 partitions the input into exactly one cluster,
             * so every input point must be assigned to that sole cluster. The upstream regression
             * test asserts exactly this for the anchor input. A "fix" that merely avoids the NPE by
             * skipping assignment/bookkeeping would return but lose points, violating this observable
             * post-condition.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:k1-partition] metamorphic violation: expected exactly one cluster inputPoints="
                    + points.length + " actualClusters=" + (clusters == null ? "null" : Integer.toString(clusters.size())));
            }
            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null) {
                throw new RuntimeException("[oracle:k1-partition] metamorphic violation: sole cluster or point list is null inputPoints="
                    + points.length);
            }
            if (only.getPoints().size() != points.length) {
                throw new RuntimeException("[oracle:k1-partition] metamorphic violation: sole cluster must contain all input points inputPoints="
                    + points.length + " clusteredPoints=" + only.getPoints().size());
            }
            for (int i = 0; i < points.length; i++) {
                if (!only.getPoints().contains(points[i])) {
                    throw new RuntimeException("[oracle:k1-partition] metamorphic violation: missing input point at index="
                        + i + " point=" + Arrays.toString(points[i].getPoint()) + " clusteredPoints=" + only.getPoints());
                }
            }
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
            if (isValidationLike(t)) {
                return;
            }
            return;
        }
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isValidationLike(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid")
            || name.contains("validation")
            || name.contains("illegal")
            || name.contains("argument");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(method))
                || ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(method))
                || ("org.apache.commons.math.stat.clustering.EuclideanIntegerPoint".equals(cls)
                    && "distanceFrom".equals(method))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [13] ⚙️ corpus-seed · `attempt_001`
**output:** **48 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['1746432956321l', '1959', '325100', '1960', '373200', '1746432956321L', '1746432956321', '1746432956321F']}

---
## [14] ⚙️ corpus-seed · `attempt_002`
**output:** **48 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['1746432956321l', '1959', '325100', '1960', '373200', '1746432956321L', '1746432956321', '1746432956321F']}

---
## [15] ⚙️ corpus-seed · `attempt_003`
**output:** **48 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['1746432956321l', '1959', '325100', '1960', '373200', '1746432956321L', '1746432956321', '1746432956321F']}

---
## [16] ⚙️ corpus-seed · `attempt_004`
**output:** **48 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['1746432956321l', '1959', '325100', '1960', '373200', '1746432956321L', '1746432956321', '1746432956321F']}

---
## [17] ⚙️ corpus-seed · `attempt_005`
**output:** **48 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['1746432956321l', '1959', '325100', '1960', '373200', '1746432956321L', '1746432956321', '1746432956321F']}

---
## [18] ⚙️ patched-fuzz · `attempt_001`
**output:** **quiet on patched build (no overfit signal)**

---
## [19] ⚙️ patched-fuzz · `attempt_002`
**output:** **quiet on patched build (no overfit signal)**

---
## [20] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**

---
## [21] ⚙️ patched-fuzz · `attempt_004`
**output:** **quiet on patched build (no overfit signal)**

---
## [22] ⚙️ patched-fuzz · `attempt_005`
**output:** **quiet on patched build (no overfit signal)**
