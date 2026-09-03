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
<details><summary>▸ Prompt (2 message(s), ~17,027 chars)</summary>

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
<details open><summary>▸ Output (~7,253 chars)</summary>

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
    }

    private static void runAnchor() {
        List<EuclideanIntegerPoint> points = Arrays.asList(
                new EuclideanIntegerPoint(new int[] {1959, 325100}),
                new EuclideanIntegerPoint(new int[] {1960, 373200})
        );
        runClusterAndCheck(points, 1746432956321L, true);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int dimension = data.consumeInt(1, 4);
        int pointCount = data.consumeInt(2, 6);
        int triggerIndex = data.consumeInt(0, dimension - 1);
        int largeDiff = data.consumeInt(46341, 100000);
        boolean positive = data.consumeBoolean();

        int[] p0 = new int[dimension];
        int[] p1 = new int[dimension];

        for (int i = 0; i < dimension; i++) {
            int base = data.consumeInt(-100000, 100000);
            p0[i] = base;
            p1[i] = base;
        }

        p0[triggerIndex] = 0;
        p1[triggerIndex] = positive ? largeDiff : -largeDiff;

        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>();
        points.add(new EuclideanIntegerPoint(Arrays.copyOf(p0, p0.length)));
        points.add(new EuclideanIntegerPoint(Arrays.copyOf(p1, p1.length)));

        for (int i = 2; i < pointCount; i++) {
            int[] extra = new int[dimension];
            for (int d = 0; d < dimension; d++) {
                int delta = data.consumeInt(-5000, 5000);
                int anchor = ((i + d) & 1) == 0 ? p0[d] : p1[d];
                extra[d] = clamp(anchor + delta, -1000000, 1000000);
            }
            extra[triggerIndex] = clamp(extra[triggerIndex] + i, -1000000, 1000000);
            points.add(new EuclideanIntegerPoint(extra));
        }

        long seed = ((long) data.consumeInt() << 32) ^ (data.consumeInt() & 0xffffffffL);
        runClusterAndCheck(points, seed, false);
    }

    private static void runClusterAndCheck(List<EuclideanIntegerPoint> points, long seed, boolean exactAnchor) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(points, 1, 1);

            /*
             * Contract asserted: clustering a non-empty set of valid points with k == 1 must yield exactly one
             * cluster, and every input point must be assigned to that single cluster. The failing test explicitly
             * checks this for the anchor input. A "fix" that only suppresses the crash or skips assignment would
             * violate this observable post-condition by returning the wrong number of clusters or by dropping points.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: single-cluster run must return exactly one cluster"
                        + " input=" + describePoints(points)
                        + " clusters=" + String.valueOf(clusters));
            }

            List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
            if (assigned == null || assigned.size() != points.size()) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: single cluster must contain all input points"
                        + " input=" + describePoints(points)
                        + " assignedSize=" + (assigned == null ? "null" : String.valueOf(assigned.size()))
                        + " expectedSize=" + points.size());
            }

            for (EuclideanIntegerPoint p : points) {
                if (!assigned.contains(p)) {
                    throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: single cluster lost an input point"
                            + " input=" + describePoints(points)
                            + " missing=" + Arrays.toString(p.getPoint())
                            + " assigned=" + describePoints(assigned));
                }
            }

            if (exactAnchor) {
                EuclideanIntegerPoint pt1 = new EuclideanIntegerPoint(new int[] {1959, 325100});
                EuclideanIntegerPoint pt2 = new EuclideanIntegerPoint(new int[] {1960, 373200});
                if (!assigned.contains(pt1) || !assigned.contains(pt2)) {
                    throw new RuntimeException("[oracle:kmeans-anchor] metamorphic violation: anchor scenario must preserve both original points"
                            + " assigned=" + describePoints(assigned));
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(ste.getClassName())
                    && "assignPointsToClusters".equals(ste.getMethodName())) {
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
        return name.startsWith("org.apache.commons.math.")
                && (name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NullArgument"));
    }

    private static String describePoints(List<EuclideanIntegerPoint> points) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Arrays.toString(points.get(i).getPoint()));
        }
        sb.append(']');
        return sb.toString();
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
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
<details open><summary>▸ Output (~9,549 chars)</summary>

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
                    new EuclideanIntegerPoint(new int[] { 1960, 373200 }),
            };
            try {
                List<Cluster<EuclideanIntegerPoint>> clusters =
                        transformer.cluster(Arrays.asList(points), 1, 1);

                /* Contract/oracle:
                 * With k == 1 and a non-empty input collection, clustering must return exactly one
                 * cluster and every input point must be assigned to that sole cluster. A "fix"
                 * that merely suppresses the crash or skips assignment bookkeeping would violate
                 * this observable post-condition.
                 */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException(
                            "[oracle:kmeans-k1-anchor] metamorphic violation: expected exactly one cluster"
                                    + " inputSize=2 actualClusters=" + (clusters == null ? "null" : clusters.size()));
                }
                List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
                if (assigned == null || assigned.size() != points.length) {
                    throw new RuntimeException(
                            "[oracle:kmeans-k1-anchor] metamorphic violation: all input points must be assigned"
                                    + " inputSize=" + points.length
                                    + " assignedSize=" + (assigned == null ? "null" : assigned.size()));
                }
                if (!assigned.contains(points[0]) || !assigned.contains(points[1])) {
                    throw new RuntimeException(
                            "[oracle:kmeans-k1-anchor] metamorphic violation: sole cluster must contain both input points");
                }
            } catch (RuntimeException t) {
                boolean reachesRoot = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cn = ste.getClassName();
                    String mn = ste.getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn)
                            && "assignPointsToClusters".equals(mn))
                            || ("org.apache.commons.math.util.MathUtils".equals(cn)
                            && "distance".equals(mn))) {
                        reachesRoot = true;
                        break;
                    }
                }
                if (t instanceof NullPointerException && reachesRoot) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
            }
        }

        int dim = data.consumeInt(2, 4);
        int pointCount = data.consumeInt(2, 6);
        int specialPointIndex = data.consumeInt(1, pointCount - 1);
        int specialCoordIndex = data.consumeInt(0, dim - 1);
        int base = data.consumeInt(-200000, 200000);
        int delta = data.consumeInt(50000, 100000);
        if (data.consumeBoolean()) {
            delta = -delta;
        }

        EuclideanIntegerPoint[] fuzzPoints = new EuclideanIntegerPoint[pointCount];
        int[][] raw = new int[pointCount][dim];

        for (int j = 0; j < dim; j++) {
            raw[0][j] = data.consumeInt(-200000, 200000);
        }
        raw[0][specialCoordIndex] = base;

        for (int i = 1; i < pointCount; i++) {
            for (int j = 0; j < dim; j++) {
                raw[i][j] = raw[0][j] + data.consumeInt(-1000, 1000);
            }
        }
        raw[specialPointIndex][specialCoordIndex] = base + delta;

        for (int i = 0; i < pointCount; i++) {
            fuzzPoints[i] = new EuclideanIntegerPoint(raw[i]);
        }

        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random((long) data.consumeInt()));

        try {
            List<Cluster<EuclideanIntegerPoint>> clusters =
                    transformer.cluster(Arrays.asList(fuzzPoints), 1, 1);

            /* Contract/oracle:
             * For the public API exercised by the failing test, clustering a non-empty set of
             * points into exactly one cluster must produce exactly one cluster containing every
             * supplied point once. This catches patches that only avoid the exception by making
             * assignment unreachable or by silently dropping points.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException(
                        "[oracle:kmeans-k1] metamorphic violation: expected exactly one cluster"
                                + " inputSize=" + pointCount
                                + " dim=" + dim
                                + " actualClusters=" + (clusters == null ? "null" : clusters.size()));
            }
            List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
            if (assigned == null || assigned.size() != pointCount) {
                throw new RuntimeException(
                        "[oracle:kmeans-k1] metamorphic violation: all input points must be assigned"
                                + " inputSize=" + pointCount
                                + " dim=" + dim
                                + " assignedSize=" + (assigned == null ? "null" : assigned.size()));
            }

            /* Contract/oracle:
             * SAME-NAME OVERLOADS documented to agree on equivalent inputs:
             * distance(int[], int[]) and distance(double[], double[]) must match when the same
             * integer coordinates are represented exactly as doubles. Both sides are real library
             * calls; no custom oracle is used.
             */
            double[] p1d = new double[dim];
            double[] p2d = new double[dim];
            for (int i = 0; i < dim; i++) {
                p1d[i] = raw[0][i];
                p2d[i] = raw[specialPointIndex][i];
            }
            try {
                double lhs = MathUtils.distance(raw[0], raw[specialPointIndex]);
                double rhs = MathUtils.distance(p1d, p2d);
                if (!(Double.isNaN(lhs) || Double.isNaN(rhs))) {
                    double diff = Math.abs(lhs - rhs);
                    if (diff > 1.0e-9 * Math.max(1.0, Math.max(Math.abs(lhs), Math.abs(rhs)))) {
                        throw new RuntimeException(
                                "[oracle:distance-overload] metamorphic violation: int/double overload disagreement"
                                        + " p1=" + Arrays.toString(raw[0])
                                        + " p2=" + Arrays.toString(raw[specialPointIndex])
                                        + " lhs=" + lhs
                                        + " rhs=" + rhs);
                    }
                } else if (Double.isNaN(lhs) != Double.isNaN(rhs)) {
                    throw new RuntimeException(
                            "[oracle:distance-overload] metamorphic violation: int/double overload disagreement"
                                    + " p1=" + Arrays.toString(raw[0])
                                    + " p2=" + Arrays.toString(raw[specialPointIndex])
                                    + " lhs=" + lhs
                                    + " rhs=" + rhs);
                }
            } catch (RuntimeException ignored) {
                if (ignored.getMessage() != null && ignored.getMessage().startsWith("[oracle:")) {
                    throw ignored;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }

            boolean reachesRoot = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn)
                        && "assignPointsToClusters".equals(mn))
                        || ("org.apache.commons.math.util.MathUtils".equals(cn)
                        && "distance".equals(mn))) {
                    reachesRoot = true;
                    break;
                }
            }

            if (t instanceof NullPointerException && reachesRoot) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }
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
<details open><summary>▸ Output (~8,295 chars)</summary>

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
        runClusterScenario(anchor, true);

        int dim = data.consumeInt(1, 6);
        int hot = data.consumeInt(0, dim - 1);
        int[] a = new int[dim];
        int[] b = new int[dim];

        for (int i = 0; i < dim; i++) {
            a[i] = data.consumeInt(-100000, 100000);
            b[i] = data.consumeInt(-100000, 100000);
        }

        int delta = data.consumeInt(46341, 100000);
        if (data.consumeBoolean()) {
            delta = -delta;
        }

        int base = data.consumeInt(-400000, 400000);
        int other = base + delta;
        if (other > 500000) {
            other = base - delta;
        }
        if (other < -500000) {
            other = base - delta;
        }
        if (other > 500000 || other < -500000) {
            base = 0;
            other = delta;
        }

        a[hot] = base;
        b[hot] = other;

        EuclideanIntegerPoint[] fuzzed = new EuclideanIntegerPoint[] {
            new EuclideanIntegerPoint(a),
            new EuclideanIntegerPoint(b),
        };
        runClusterScenario(fuzzed, true);

        runDistanceSiblingCheck(a, b);
    }

    private static void runClusterScenario(EuclideanIntegerPoint[] points, boolean assertPostCondition) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(Arrays.asList(points), 1, 1);

            if (!assertPostCondition) {
                return;
            }

            /* Contract exercised by the public API path from the failing test:
             * clustering a non-null collection of exactly these two valid points into k=1 cluster
             * should return exactly one cluster containing all input points.
             * A "fix" that merely suppresses the crashing path or skips assignment would violate
             * this observable result even if no exception were thrown.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: expected exactly one cluster for k=1 input=" + Arrays.deepToString(extract(points)) + " clusters=" + String.valueOf(clusters));
            }

            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: missing cluster contents input=" + Arrays.deepToString(extract(points)) + " cluster=" + String.valueOf(only));
            }

            int assigned = only.getPoints().size();
            if (assigned != points.length) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: all input points must be assigned when k=1 input=" + Arrays.deepToString(extract(points)) + " assigned=" + assigned + " expected=" + points.length);
            }

            for (int i = 0; i < points.length; i++) {
                if (!only.getPoints().contains(points[i])) {
                    throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: output cluster lost an input point input=" + Arrays.deepToString(extract(points)) + " missingIndex=" + i + " clusterPoints=" + only.getPoints());
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runDistanceSiblingCheck(int[] a, int[] b) {
        try {
            double intDistance = MathUtils.distance(a, b);
            double[] ad = toDoubleArray(a);
            double[] bd = toDoubleArray(b);
            double doubleDistance = MathUtils.distance(ad, bd);

            /* Documented sibling guarantee:
             * MathUtils.distance(int[], int[]) and MathUtils.distance(double[], double[])
             * are overloads of the same Euclidean-distance operation. For moderate int values,
             * every coordinate is represented exactly as double, so equivalent inputs must agree.
             * A patch that simply avoids the crash path but computes the wrong value violates this.
             */
            if (!sameDouble(intDistance, doubleDistance)) {
                throw new RuntimeException("[oracle:distance-overload] metamorphic violation: equivalent int[]/double[] inputs disagree inputA=" + Arrays.toString(a) + " inputB=" + Arrays.toString(b) + " lhs=" + intDistance + " rhs=" + doubleDistance);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean sameDouble(double x, double y) {
        if (Double.isNaN(x) && Double.isNaN(y)) {
            return true;
        }
        return Double.doubleToLongBits(x) == Double.doubleToLongBits(y);
    }

    private static double[] toDoubleArray(int[] x) {
        double[] out = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            out[i] = x[i];
        }
        return out;
    }

    private static int[][] extract(EuclideanIntegerPoint[] points) {
        int[][] out = new int[points.length][];
        for (int i = 0; i < points.length; i++) {
            out[i] = points[i].getPoint();
        }
        return out;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || hasValidationLikeClassName(t);
    }

    private static boolean hasValidationLikeClassName(Throwable t) {
        String name = t.getClass().getName();
        return name.contains("MathRuntimeException")
            || name.contains("ConvergenceException")
            || name.contains("Invalid")
            || name.contains("Illegal");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException) && !(t instanceof RuntimeException && t.getClass() == RuntimeException.class && hasOracleFrameMessage(t))) {
            if (!(t instanceof RuntimeException && hasOraclePrefix(t))) {
                return false;
            }
        }

        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < frames.length; i++) {
            String cls = frames[i].getClassName();
            String method = frames[i].getMethodName();
            if ("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.util.FuzzHarness".equals(cls)
                    && method != null
                    && method.startsWith("run")) {
                if (hasOraclePrefix(t)) {
                    return true;
                }
            }
        }
        return hasOraclePrefix(t);
    }

    private static boolean hasOraclePrefix(Throwable t) {
        return t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean hasOracleFrameMessage(Throwable t) {
        return hasOraclePrefix(t);
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
<details open><summary>▸ Output (~5,773 chars)</summary>

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

        int dims = data.consumeInt(2, 4);
        int pointCount = data.consumeInt(2, 6);
        int triggerIndex = data.consumeInt(0, dims - 1);

        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(pointCount);

        int[] p0 = new int[dims];
        int[] p1 = new int[dims];
        for (int i = 0; i < dims; i++) {
            p0[i] = data.consumeInt(-100000, 100000);
            p1[i] = data.consumeInt(-100000, 100000);
        }

        // Root-cause property: at least one coordinate delta whose square exceeds Integer.MAX_VALUE
        // (|delta| >= 46341), while all inputs remain otherwise valid, non-null points passed through
        // the real public API KMeansPlusPlusClusterer.cluster(...).
        int largeDelta = data.consumeInt(46341, 200000);
        p0[triggerIndex] = 0;
        p1[triggerIndex] = data.consumeBoolean() ? largeDelta : -largeDelta;

        points.add(new EuclideanIntegerPoint(p0));
        points.add(new EuclideanIntegerPoint(p1));

        for (int i = 2; i < pointCount; i++) {
            int[] p = new int[dims];
            for (int j = 0; j < dims; j++) {
                p[j] = data.consumeInt(-100000, 100000);
            }
            points.add(new EuclideanIntegerPoint(p));
        }

        runScenario(points, data.consumeInt());
    }

    private static void runAnchor() {
        List<EuclideanIntegerPoint> points = Arrays.asList(
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 })
        );
        runScenario(points, 174643295);
    }

    private static void runScenario(List<EuclideanIntegerPoint> points, int seed) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(points, 1, 1);

            // Contract asserted from the public API and the regression test:
            // clustering N valid input points into k=1 cluster must yield exactly one cluster, and that
            // sole cluster must contain all input points. A "fix" that merely avoids the crash by skipping
            // the assignment path would violate this observable post-condition.
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: expected exactly one cluster inputSize="
                        + points.size() + " clusters=" + String.valueOf(clusters));
            }

            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: sole cluster or point list is null inputSize="
                        + points.size());
            }

            if (only.getPoints().size() != points.size()) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: sole cluster must contain all input points inputSize="
                        + points.size() + " assignedSize=" + only.getPoints().size());
            }

            for (EuclideanIntegerPoint p : points) {
                if (!only.getPoints().contains(p)) {
                    throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: sole cluster missing input point input="
                            + Arrays.toString(p.getPoint()) + " assignedSize=" + only.getPoints().size());
                }
            }
        } catch (RuntimeException t) {
            if (t.getClass() == RuntimeException.class && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Exception t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(m))
                    || ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(m))) {
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
                || name.contains("OutOfRange")
                || name.contains("DimensionMismatch")
                || name.contains("MathRuntimeException");
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
<details open><summary>▸ Output (~8,148 chars)</summary>

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
        runAnchor();

        int dimension = data.consumeInt(1, 4);
        int pointCount = data.consumeInt(2, 4);
        long seed = data.consumeInt();

        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[pointCount];
        int[][] raw = new int[pointCount][dimension];

        for (int i = 0; i < pointCount; i++) {
            for (int d = 0; d < dimension; d++) {
                raw[i][d] = data.consumeInt(-500000, 500000);
            }
        }

        int hotPointA = 0;
        int hotPointB = 1;
        int hotDim = data.consumeInt(0, dimension - 1);

        int triggerStyle = data.consumeInt(0, 2);
        if (triggerStyle == 0) {
            raw[hotPointA][hotDim] = 1959;
            raw[hotPointB][hotDim] = 1960;
            if (dimension > 1) {
                raw[hotPointA][1] = 325100;
                raw[hotPointB][1] = 373200;
            } else {
                raw[hotPointA][hotDim] = 325100;
                raw[hotPointB][hotDim] = 373200;
            }
        } else if (triggerStyle == 1) {
            raw[hotPointA][hotDim] = 0;
            raw[hotPointB][hotDim] = 50000;
        } else {
            raw[hotPointA][hotDim] = data.consumeBoolean() ? -500000 : -46341;
            raw[hotPointB][hotDim] = data.consumeBoolean() ? 500000 : 46341;
        }

        for (int i = 0; i < pointCount; i++) {
            points[i] = new EuclideanIntegerPoint(raw[i]);
        }

        runClusterCase(points, seed);

        runDistanceSiblingOracle(raw, data);
    }

    private static void runAnchor() {
        KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
        EuclideanIntegerPoint[] points = new EuclideanIntegerPoint[] {
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 })
        };
        runClusterCase(points, 1746432956321L);
    }

    private static void runClusterCase(EuclideanIntegerPoint[] points, long seed) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));
            List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);

            /* Contract/oracle:
             * For a non-empty valid input and k == 1, clustering must return exactly one cluster,
             * and every supplied point must be assigned to that sole cluster. A "fix" that only
             * suppresses the buggy crash or skips assignment/bookkeeping would violate this
             * observable post-condition even if it no longer throws.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: expected exactly one cluster input="
                        + Arrays.deepToString(toRaw(points)) + " clusters=" + String.valueOf(clusters));
            }
            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null) {
                throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: null cluster or points input="
                        + Arrays.deepToString(toRaw(points)));
            }
            for (EuclideanIntegerPoint p : points) {
                if (!only.getPoints().contains(p)) {
                    throw new RuntimeException("[oracle:kmeans-k1] metamorphic violation: sole cluster missing input point input="
                            + Arrays.deepToString(toRaw(points)) + " assigned=" + only.getPoints());
                }
            }
        } catch (Throwable t) {
            if (isOracleViolation(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runDistanceSiblingOracle(int[][] raw, FuzzedDataProvider data) {
        int len = raw[0].length;
        int[] a = new int[len];
        int[] b = new int[len];
        for (int i = 0; i < len; i++) {
            a[i] = data.consumeInt(-500000, 500000);
            b[i] = data.consumeInt(-500000, 500000);
        }

        int idx = data.consumeInt(0, len - 1);
        a[idx] = 0;
        b[idx] = 50000;

        double[] da = new double[len];
        double[] db = new double[len];
        for (int i = 0; i < len; i++) {
            da[i] = a[i];
            db[i] = b[i];
        }

        try {
            /* Documented sibling-agreement oracle:
             * distance(int[], int[]) and distance(double[], double[]) are same-named overloads
             * for the same metric on equivalent coordinates, so for int values exactly representable
             * as doubles they must agree. A patch that merely avoids the crash but leaves overflow-
             * induced wrong distances would violate this relation.
             */
            double lhs = MathUtils.distance(a, b);
            double rhs = MathUtils.distance(da, db);
            if (!(Double.isNaN(lhs) && Double.isNaN(rhs)) && Math.abs(lhs - rhs) > 0.0d) {
                throw new RuntimeException("[oracle:distance-overload] metamorphic violation: int/double overload disagreement input="
                        + Arrays.deepToString(new int[][] { a, b }) + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (isOracleViolation(t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static int[][] toRaw(EuclideanIntegerPoint[] points) {
        int[][] raw = new int[points.length][];
        for (int i = 0; i < points.length; i++) {
            raw[i] = points[i].getPoint();
        }
        return raw;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("MathRuntimeException")
                || name.contains("ConvergenceException")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoData");
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
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
**output:** **FIRED — [oracle:distance-overload] metamorphic violation: int/double overload disagreement p1=[-200000, -200000] p2=[-150000, -201000] lhs=46340.950001051984 rhs=50009.99900019995**
- reproducing_input_file: /tmp/d4j/Math_79_buggy/fuzz/attempt_002/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [20] ⚙️ patched-fuzz · `attempt_003`
**output:** **FIRED — [oracle:distance-overload] metamorphic violation: equivalent int[]/double[] inputs disagree inputA=[-400000] inputB=[-353659] lhs=46340.950001051984 rhs=46341.0**
- reproducing_input_file: /tmp/d4j/Math_79_buggy/fuzz/attempt_003/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [21] ⚙️ patched-fuzz · `attempt_004`
**output:** **quiet on patched build (no overfit signal)**

---
## [22] ⚙️ patched-fuzz · `attempt_005`
**output:** **FIRED — [oracle:distance-overload] metamorphic violation: int/double overload disagreement input=[[0], [50000]] lhs=46340.950001051984 rhs=50000.0**
- reproducing_input_file: /tmp/d4j/Math_79_buggy/fuzz/attempt_005/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709
