# Pipeline trace — Math-79

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 5 harness(es) fuzzed on the patched build; campaign converged=True]

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
<details><summary>▸ output — modified_files": [ (~1,389 chars)</summary>

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
      "xrefs": [],
      "reachable": [
        "Math.sqrt(org.apache.commons.math.util.MathUtils)"
      ],
      "related_callees": [],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.math.util",
  "root_cause_reachable": [],
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
<details><summary>▸ Prompt (2 message(s), ~16,814 chars)</summary>

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
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
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
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.math.stat.clustering.KMeansPlusPlusClustererTest::testPerformClusterAnalysisDegenerate
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  addAndCheck(int x, int y) / (long a, long b)
  distance(double[] p1, double[] p2) / (int[] p1, int[] p2)
  distance1(double[] p1, double[] p2) / (int[] p1, int[] p2)
  distanceInf(double[] p1, double[] p2) / (int[] p1, int[] p2)
  equals(double x, double y) / (double x, double y, double eps) / (double x, double y, int maxUlps) / (double[] x, double[] y)
  hash(double value) / (double[] value)
  indicator(final byte x) / (final double x) / (final float x) / (final int x) / (final long x) / (final short x)
  mulAndCheck(int x, int y) / (long a, long b)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  binomial* family: binomialCoefficient, binomialCoefficientDouble, binomialCoefficientLog

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
<details open><summary>▸ Output (~7,964 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact reproducer from the failing test. These inputs are valid by construction:
        // non-null points, same dimension, k=1, maxIterations=1.
        // Post-condition asserted below: with k=1, clustering must return exactly one cluster
        // containing all supplied points; a "fix" that merely suppresses the crash or skips
        // assignment would violate this observable contract.
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                    new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                    new EuclideanIntegerPoint(new int[] { 1960, 373200 }),
            };
            List<Cluster<EuclideanIntegerPoint>> clusters =
                    transformer.cluster(Arrays.asList(points), 1, 1);

            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException(
                        "[oracle:k1-all-points] metamorphic violation: k=1 must yield exactly one cluster input="
                                + Arrays.deepToString(points) + " clusters=" + String.valueOf(clusters));
            }
            Cluster<EuclideanIntegerPoint> c = clusters.get(0);
            if (c == null || c.getPoints() == null || c.getPoints().size() != 2
                    || !c.getPoints().contains(points[0]) || !c.getPoints().contains(points[1])) {
                throw new RuntimeException(
                        "[oracle:k1-all-points] metamorphic violation: k=1 cluster must contain all input points input="
                                + Arrays.deepToString(points) + " clusterPoints="
                                + (c == null ? "null" : String.valueOf(c.getPoints())));
            }
        } catch (Throwable t) {
            boolean validation = t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof MathRuntimeException;
            boolean rootCauseType = t instanceof NullPointerException;
            boolean rootCauseLocation = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cls = ste.getClassName();
                String m = ste.getMethodName();
                if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                        && "assignPointsToClusters".equals(m))
                        || ("org.apache.commons.math.util.MathUtils".equals(cls)
                        && "distance".equals(m))) {
                    rootCauseLocation = true;
                    break;
                }
            }
            if (rootCauseType && rootCauseLocation) {
                throw t;
            }
            if (validation) {
                return;
            }
            return;
        }

        // EXPLORE: generate many valid point sets that preserve the root-cause property:
        // at least one coordinate difference whose square exceeds Integer.MAX_VALUE when
        // computed in int arithmetic (|delta| >= 46341), while keeping inputs moderate.
        int dimension = data.consumeInt(1, 6);
        int numPoints = data.consumeInt(2, 8);
        int specialIndex = data.consumeInt(0, numPoints - 1);
        int specialCoord = data.consumeInt(0, dimension - 1);

        EuclideanIntegerPoint[] fuzzPoints = new EuclideanIntegerPoint[numPoints];
        int[][] raw = new int[numPoints][dimension];

        for (int i = 0; i < numPoints; i++) {
            for (int d = 0; d < dimension; d++) {
                raw[i][d] = data.consumeInt(-100000, 100000);
            }
        }

        // Make point 0 a baseline and ensure one point differs enough on one coordinate
        // to exercise the patched arithmetic, while staying valid-by-construction.
        raw[0][specialCoord] = data.consumeInt(-1000, 1000);
        if (specialIndex == 0) {
            specialIndex = 1;
        }
        int largeDelta = data.consumeBoolean()
                ? data.consumeInt(46341, 100000)
                : -data.consumeInt(46341, 100000);
        raw[specialIndex][specialCoord] = raw[0][specialCoord] + largeDelta;

        for (int i = 0; i < numPoints; i++) {
            fuzzPoints[i] = new EuclideanIntegerPoint(raw[i]);
        }

        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(data.consumeInt()));
            List<Cluster<EuclideanIntegerPoint>> clusters =
                    transformer.cluster(Arrays.asList(fuzzPoints), 1, 1);

            // Same documented/observable guarantee as above:
            // for a valid non-empty input and k=1, all points must end up in the sole cluster.
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException(
                        "[oracle:k1-all-points] metamorphic violation: k=1 must yield exactly one cluster input="
                                + Arrays.deepToString(raw) + " clusters=" + String.valueOf(clusters));
            }
            Cluster<EuclideanIntegerPoint> c = clusters.get(0);
            if (c == null || c.getPoints() == null || c.getPoints().size() != fuzzPoints.length) {
                throw new RuntimeException(
                        "[oracle:k1-all-points] metamorphic violation: sole cluster must contain every input point input="
                                + Arrays.deepToString(raw) + " expectedSize=" + fuzzPoints.length
                                + " actualPoints=" + (c == null ? "null" : String.valueOf(c.getPoints())));
            }
            for (int i = 0; i < fuzzPoints.length; i++) {
                if (!c.getPoints().contains(fuzzPoints[i])) {
                    throw new RuntimeException(
                            "[oracle:k1-all-points] metamorphic violation: missing input point from sole cluster input="
                                    + Arrays.deepToString(raw) + " missingIndex=" + i + " clusterPoints="
                                    + String.valueOf(c.getPoints()));
                }
            }
        } catch (Throwable t) {
            boolean validation = t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof MathRuntimeException;
            boolean rootCauseType = t instanceof NullPointerException;
            boolean rootCauseLocation = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cls = ste.getClassName();
                String m = ste.getMethodName();
                if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                        && "assignPointsToClusters".equals(m))
                        || ("org.apache.commons.math.util.MathUtils".equals(cls)
                        && "distance".equals(m))) {
                    rootCauseLocation = true;
                    break;
                }
            }
            if (rootCauseType && rootCauseLocation) {
                throw t;
            }
            if (validation) {
                return;
            }
            return;
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
<details open><summary>▸ Output (~8,658 chars)</summary>

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
        runExplore(data);
        runDistanceSiblingAgreement(data);
    }

    private static void runAnchor() {
        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
            new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
            new EuclideanIntegerPoint(new int[] { 1960, 373200 }),
        };
        List<EuclideanIntegerPoint> input = Arrays.asList(points);
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
        try {
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(input, 1, 1);
            assertSingleClusterContainsAll(input, clusters, "anchor");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int dim = data.consumeInt(2, 5);
        int pointCount = data.consumeInt(2, 6);
        int specialDim = data.consumeInt(0, dim - 1);

        int[] p0 = new int[dim];
        int[] p1 = new int[dim];

        for (int i = 0; i < dim; i++) {
            p0[i] = data.consumeInt(-100000, 100000);
            p1[i] = data.consumeInt(-100000, 100000);
        }

        int base = data.consumeInt(-200000, 200000);
        int magnitude = data.consumeInt(46341, 1000000);
        int sign = data.consumeBoolean() ? 1 : -1;
        p0[specialDim] = base;
        p1[specialDim] = base + sign * magnitude;

        List<EuclideanIntegerPoint> input = new ArrayList<EuclideanIntegerPoint>();
        input.add(new EuclideanIntegerPoint(p0));
        input.add(new EuclideanIntegerPoint(p1));

        for (int n = 2; n < pointCount; n++) {
            int[] coords = new int[dim];
            for (int i = 0; i < dim; i++) {
                coords[i] = data.consumeInt(-300000, 300000);
            }
            if (data.consumeBoolean()) {
                coords[specialDim] = base + data.consumeInt(-1000, 1000);
            }
            input.add(new EuclideanIntegerPoint(coords));
        }

        int maxIterations = data.consumeInt(1, 3);
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(data.consumeInt()));
        try {
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(input, 1, maxIterations);
            /*
             * Contract asserted: clustering with k == 1 must yield exactly one cluster containing
             * every supplied point. The failing regression test itself checks this for the public API.
             * A throw-deleting or branch-skipping "fix" that avoids the crash by silently failing to
             * assign points would violate this observable result.
             */
            assertSingleClusterContainsAll(input, clusters, "explore");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runDistanceSiblingAgreement(FuzzedDataProvider data) {
        int dim = data.consumeInt(1, 5);
        int[] a = new int[dim];
        int[] b = new int[dim];
        double[] ad = new double[dim];
        double[] bd = new double[dim];

        for (int i = 0; i < dim; i++) {
            a[i] = data.consumeInt(-1000, 1000);
            b[i] = data.consumeInt(-1000, 1000);
            ad[i] = a[i];
            bd[i] = b[i];
        }

        try {
            /*
             * Contract asserted: the int[] and double[] overloads of MathUtils.distance represent
             * the same Euclidean distance on equivalent coordinates. For these moderate magnitudes,
             * a correct implementation must agree on exactly the same mathematical result.
             */
            double lhs = MathUtils.distance(a, b);
            double rhs = MathUtils.distance(ad, bd);
            if (Double.isNaN(lhs) != Double.isNaN(rhs) || Math.abs(lhs - rhs) > 1.0e-9) {
                throw new RuntimeException(
                        "[oracle:distance-overload] metamorphic violation: equivalent int[]/double[] distance disagree"
                                + " inputA=" + Arrays.toString(a)
                                + " inputB=" + Arrays.toString(b)
                                + " lhs=" + lhs
                                + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                    && ((RuntimeException) t).getMessage().startsWith("[oracle:distance-overload]")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void assertSingleClusterContainsAll(
            List<EuclideanIntegerPoint> input,
            List<Cluster<EuclideanIntegerPoint>> clusters,
            String label) {
        if (clusters == null || clusters.size() != 1) {
            throw new RuntimeException(
                    "[oracle:k1-partition] metamorphic violation: k=1 must produce exactly one cluster"
                            + " label=" + label
                            + " inputSize=" + input.size()
                            + " clusterCount=" + (clusters == null ? "null" : Integer.toString(clusters.size())));
        }
        List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
        if (assigned == null || assigned.size() != input.size()) {
            throw new RuntimeException(
                    "[oracle:k1-partition] metamorphic violation: single cluster must contain all input points"
                            + " label=" + label
                            + " inputSize=" + input.size()
                            + " assignedSize=" + (assigned == null ? "null" : Integer.toString(assigned.size())));
        }
        for (int i = 0; i < input.size(); i++) {
            EuclideanIntegerPoint p = input.get(i);
            int count = 0;
            for (int j = 0; j < assigned.size(); j++) {
                if (assigned.get(j) == p) {
                    count++;
                }
            }
            if (count != 1) {
                throw new RuntimeException(
                        "[oracle:k1-partition] metamorphic violation: each input point must appear exactly once in the only cluster"
                                + " label=" + label
                                + " pointIndex=" + i
                                + " occurrences=" + count
                                + " point=" + Arrays.toString(p.getPoint()));
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(method))
                    || ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(method))) {
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
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NullArgument");
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
<details open><summary>▸ Output (~8,085 chars)</summary>

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
        {
            EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 }),
            };
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            try {
                List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);

                /*
                 * Contract/oracle: with k == 1 and a non-empty valid input collection, clustering must
                 * return exactly one cluster and every supplied point must be assigned to that only
                 * cluster. A "fix" that merely suppresses the failing assignment path would leave the
                 * cluster empty or missing points, violating this observable post-condition.
                 */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: k=1 must yield exactly one cluster input=2 lhs=" +
                        (clusters == null ? "null" : Integer.toString(clusters.size())) + " rhs=1");
                }
                List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
                if (assigned == null || assigned.size() != 2 ||
                    !assigned.contains(points[0]) || !assigned.contains(points[1])) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: k=1 cluster must contain all input points input=2 lhs=" +
                        (assigned == null ? "null" : assigned.toString()) + " rhs=" + Arrays.asList(points).toString());
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t instanceof org.apache.commons.math.MathRuntimeException ||
                    t.getClass().getName().contains("Invalid") ||
                    t.getClass().getName().contains("Illegal");
                if (cleanRejection) {
                    return;
                }

                boolean inPatchedRegion = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cn = ste.getClassName();
                    String mn = ste.getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn)
                            && "assignPointsToClusters".equals(mn))
                        || ("org.apache.commons.math.util.MathUtils".equals(cn)
                            && "distance".equals(mn))) {
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

        int dim = data.consumeInt(1, 4);
        int triggerIndex = data.consumeInt(0, dim - 1);
        int[] a = new int[dim];
        int[] b = new int[dim];
        for (int i = 0; i < dim; i++) {
            int base = data.consumeInt(-1_000_000, 1_000_000);
            a[i] = base;
            b[i] = base;
        }

        int delta = data.consumeInt(46341, 1_000_000);
        if (data.consumeBoolean()) {
            delta = -delta;
        }

        long candidate = (long) a[triggerIndex] + (long) delta;
        if (candidate > 1_000_000L) {
            candidate = 1_000_000L;
            a[triggerIndex] = (int) candidate - delta;
            if (a[triggerIndex] < -1_000_000) {
                a[triggerIndex] = -1_000_000;
                candidate = (long) a[triggerIndex] + (long) delta;
            }
        } else if (candidate < -1_000_000L) {
            candidate = -1_000_000L;
            a[triggerIndex] = (int) candidate - delta;
            if (a[triggerIndex] > 1_000_000) {
                a[triggerIndex] = 1_000_000;
                candidate = (long) a[triggerIndex] + (long) delta;
            }
        }
        b[triggerIndex] = (int) candidate;

        if (Math.abs((long) a[triggerIndex] - (long) b[triggerIndex]) <= 46340L) {
            if (a[triggerIndex] <= 0) {
                a[triggerIndex] = -500_000;
                b[triggerIndex] = 500_000;
            } else {
                a[triggerIndex] = 500_000;
                b[triggerIndex] = -500_000;
            }
        }

        EuclideanIntegerPoint p0 = new EuclideanIntegerPoint(a);
        EuclideanIntegerPoint p1 = new EuclideanIntegerPoint(b);
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
            new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(data.consumeInt()));

        try {
            List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(p0, p1), 1, 1);

            /*
             * Contract/oracle: for the real public API cluster(points, 1, 1), there is only one cluster
             * to assign to, so after a successful call every non-null input point must appear in that
             * single cluster. This remains true regardless of point dimension, differing coordinate
             * position, or surrounding coordinates.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:k1-explore] metamorphic violation: k=1 must yield exactly one cluster input=" +
                    Arrays.asList(p0, p1).toString() + " lhs=" + (clusters == null ? "null" : Integer.toString(clusters.size())) + " rhs=1");
            }
            List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
            if (assigned == null || assigned.size() != 2 || !assigned.contains(p0) || !assigned.contains(p1)) {
                throw new RuntimeException("[oracle:k1-explore] metamorphic violation: k=1 cluster must contain all input points input=" +
                    Arrays.asList(p0, p1).toString() + " lhs=" + (assigned == null ? "null" : assigned.toString()) +
                    " rhs=" + Arrays.asList(p0, p1).toString());
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException ||
                t instanceof org.apache.commons.math.MathRuntimeException ||
                t.getClass().getName().contains("Invalid") ||
                t.getClass().getName().contains("Illegal");
            if (cleanRejection) {
                return;
            }

            boolean inPatchedRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn)
                        && "assignPointsToClusters".equals(mn))
                    || ("org.apache.commons.math.util.MathUtils".equals(cn)
                        && "distance".equals(mn))) {
                    inPatchedRegion = true;
                    break;
                }
            }

            if (t instanceof NullPointerException && inPatchedRegion) {
                throw (NullPointerException) t;
            }
        }
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
<details open><summary>▸ Output (~9,480 chars)</summary>

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
        {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                    new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                    new EuclideanIntegerPoint(new int[] { 1960, 373200 })
            };
            try {
                List<Cluster<EuclideanIntegerPoint>> clusters =
                        transformer.cluster(Arrays.asList(points), 1, 1);

                /* Contract asserted: with k = 1 and a non-empty valid input collection, clustering must
                 * return exactly one cluster and every input point must be assigned to that cluster.
                 * A throw-deleting or assignment-skipping patch would violate this observable result. */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: k=1 must yield exactly one cluster input="
                            + Arrays.asList(points) + " clusters=" + clusters);
                }
                Cluster<EuclideanIntegerPoint> only = clusters.get(0);
                if (only == null || only.getPoints() == null || only.getPoints().size() != points.length) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: k=1 single cluster must contain all points input="
                            + Arrays.asList(points) + " clusterPoints=" + (only == null ? null : only.getPoints()));
                }
                for (int i = 0; i < points.length; i++) {
                    if (!only.getPoints().contains(points[i])) {
                        throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: output cluster lost an input point input="
                                + Arrays.asList(points) + " missing=" + points[i] + " clusterPoints=" + only.getPoints());
                    }
                }
            } catch (RuntimeException t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                boolean reachable = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn)
                            && "assignPointsToClusters".equals(mn))
                            || ("org.apache.commons.math.util.MathUtils".equals(cn)
                            && "distance".equals(mn))) {
                        reachable = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && reachable) {
                    throw t;
                }
                return;
            }
        }

        int dims = data.consumeInt(2, 5);
        int pointCount = data.consumeInt(2, 6);
        int[][] raw = new int[pointCount][dims];

        int pivotA = data.consumeInt(-200000, 200000);
        int pivotB = data.consumeInt(-200000, 200000);
        int overflowDim = data.consumeInt(0, dims - 1);
        int delta = data.consumeInt(46341, 100000);
        if (data.consumeBoolean()) {
            delta = -delta;
        }

        for (int d = 0; d < dims; d++) {
            raw[0][d] = (d == overflowDim) ? pivotA : data.consumeInt(-1000, 1000);
            raw[1][d] = (d == overflowDim) ? (pivotA + delta) : ((d == 0) ? pivotB : data.consumeInt(-1000, 1000));
        }

        for (int i = 2; i < pointCount; i++) {
            for (int d = 0; d < dims; d++) {
                if (data.consumeBoolean()) {
                    raw[i][d] = raw[i % 2][d];
                } else {
                    raw[i][d] = raw[i % 2][d] + data.consumeInt(-50, 50);
                }
            }
        }

        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[pointCount];
        for (int i = 0; i < pointCount; i++) {
            points[i] = new EuclideanIntegerPoint(raw[i]);
        }

        {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            try {
                List<Cluster<EuclideanIntegerPoint>> clusters =
                        transformer.cluster(Arrays.asList(points), 1, 1);

                /* Contract asserted: for valid non-empty inputs and k = 1, the public API must assign
                 * all points to the single returned cluster. This directly checks the real call chain
                 * that reaches MathUtils.distance via EuclideanIntegerPoint/KMeansPlusPlusClusterer. */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-explore] metamorphic violation: k=1 must yield exactly one cluster input="
                            + Arrays.asList(points) + " clusters=" + clusters);
                }
                Cluster<EuclideanIntegerPoint> only = clusters.get(0);
                if (only == null || only.getPoints() == null || only.getPoints().size() != points.length) {
                    throw new RuntimeException("[oracle:k1-explore] metamorphic violation: k=1 single cluster must contain all points input="
                            + Arrays.asList(points) + " clusterPoints=" + (only == null ? null : only.getPoints()));
                }
                for (int i = 0; i < points.length; i++) {
                    if (!only.getPoints().contains(points[i])) {
                        throw new RuntimeException("[oracle:k1-explore] metamorphic violation: output cluster lost an input point input="
                                + Arrays.asList(points) + " missing=" + points[i] + " clusterPoints=" + only.getPoints());
                    }
                }
            } catch (RuntimeException t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                boolean reachable = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn)
                            && "assignPointsToClusters".equals(mn))
                            || ("org.apache.commons.math.util.MathUtils".equals(cn)
                            && "distance".equals(mn))) {
                        reachable = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && reachable) {
                    throw t;
                }
                return;
            }
        }

        {
            int[] p1 = raw[0];
            int[] p2 = raw[1];
            double[] d1 = new double[p1.length];
            double[] d2 = new double[p2.length];
            for (int i = 0; i < p1.length; i++) {
                d1[i] = p1[i];
                d2[i] = p2[i];
            }

            try {
                /* Contract asserted: the int[] and double[] Euclidean distance overloads describe the
                 * same mathematical operation on equivalent numeric inputs, so converting the same
                 * integer coordinates to doubles must preserve the result. A patch that merely avoids
                 * the crash but computes the wrong distance would violate this sibling agreement. */
                double lhs = MathUtils.distance(p1, p2);
                double rhs = MathUtils.distance(d1, d2);

                boolean equal;
                if (Double.isNaN(lhs) && Double.isNaN(rhs)) {
                    equal = true;
                } else if (Double.isInfinite(lhs) || Double.isInfinite(rhs)) {
                    equal = lhs == rhs;
                } else {
                    double diff = Math.abs(lhs - rhs);
                    equal = diff <= 1.0e-9;
                }

                if (!equal) {
                    throw new RuntimeException("[oracle:distance-overload] metamorphic violation: distance(int[]) must agree with distance(double[]) input="
                            + Arrays.toString(p1) + " vs " + Arrays.toString(p2) + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                throw t;
            }
        }
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
<details open><summary>▸ Output (~6,602 chars)</summary>

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
        // Anchor: exact regression input from the failing test.
        List<EuclideanIntegerPoint> anchorPoints = Arrays.asList(
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 })
        );
        runClusterScenario(anchorPoints);

        // Explore: build valid-by-construction inputs for the real public API.
        // Root-cause property: at least one coordinate difference has |dp| > 46340,
        // so dp*dp overflows int in MathUtils.distance(int[], int[]) in the buggy build.
        int dims = data.consumeInt(2, 6);
        int nPoints = data.consumeInt(2, 6);
        int triggerDim = data.consumeInt(0, dims - 1);

        int[] base = new int[dims];
        for (int i = 0; i < dims; i++) {
            base[i] = data.consumeInt(-500_000, 500_000);
        }

        int[] trigger = base.clone();
        int delta = data.consumeInt(46_341, 100_000);
        if (data.consumeBoolean()) {
            trigger[triggerDim] += delta;
        } else {
            trigger[triggerDim] -= delta;
        }

        List<EuclideanIntegerPoint> fuzzPoints = new ArrayList<EuclideanIntegerPoint>();
        int anchorIndex = data.consumeInt(0, nPoints - 1);
        int triggerIndex = (anchorIndex + 1 + data.consumeInt(0, nPoints - 2)) % nPoints;

        for (int p = 0; p < nPoints; p++) {
            int[] coords;
            if (p == anchorIndex) {
                coords = base.clone();
            } else if (p == triggerIndex) {
                coords = trigger.clone();
            } else {
                coords = base.clone();
                for (int d = 0; d < dims; d++) {
                    if (d == triggerDim && data.consumeBoolean()) {
                        coords[d] += data.consumeInt(-10, 10);
                    } else {
                        coords[d] += data.consumeInt(-1000, 1000);
                    }
                }
            }
            fuzzPoints.add(new EuclideanIntegerPoint(coords));
        }

        runClusterScenario(fuzzPoints);
    }

    private static void runClusterScenario(List<EuclideanIntegerPoint> points) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(points, 1, 1);

            // Contract asserted:
            // With k == 1, the result must be exactly one cluster, and all supplied points
            // must be assigned to that single cluster. The regression test itself checks this
            // for the anchor input. A throw-deleting or branch-skipping patch could avoid the
            // crash yet silently lose assignments, so we assert observable assignment fidelity.
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:k1-all-assigned] metamorphic violation: expected exactly one cluster inputSize="
                        + points.size() + " actualClusters=" + (clusters == null ? "null" : Integer.toString(clusters.size())));
            }

            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            List<EuclideanIntegerPoint> assigned = only.getPoints();
            if (assigned == null) {
                throw new RuntimeException("[oracle:k1-all-assigned] metamorphic violation: single cluster has null point list inputSize="
                        + points.size());
            }
            if (assigned.size() != points.size()) {
                throw new RuntimeException("[oracle:k1-all-assigned] metamorphic violation: for k=1 all input points must be assigned to the only cluster inputSize="
                        + points.size() + " assignedSize=" + assigned.size() + " assigned=" + assigned + " input=" + points);
            }
            for (EuclideanIntegerPoint p : points) {
                if (!assigned.contains(p)) {
                    throw new RuntimeException("[oracle:k1-all-assigned] metamorphic violation: returned cluster missing input point point="
                            + p + " assigned=" + assigned + " input=" + points);
                }
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                throw t;
            }
            // Swallow unrelated runtime exceptions to avoid false positives outside this patch.
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("NotStrictlyPositive")
                || name.contains("OutOfRange")
                || name.contains("Invalid")
                || name.contains("NoData")
                || name.contains("NullArgument")
                || name.contains("MathIllegal");
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
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
**output:** **FIRED — [oracle:distance-overload] metamorphic violation: distance(int[]) must agree with distance(double[]) input=[-200000, -1000] vs [-153659, -1000] lhs=46340.950001051984 rhs=46341.0**
- reproducing_input_file: /tmp/d4j/Math_79_buggy/fuzz/attempt_004/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [22] ⚙️ patched-fuzz · `attempt_005`
**output:** **quiet on patched build (no overfit signal)**
