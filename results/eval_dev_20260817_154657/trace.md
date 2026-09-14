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
<details><summary>▸ Prompt (2 message(s), ~15,312 chars)</summary>

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
<details open><summary>▸ Output (~6,755 chars)</summary>

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
        runScenario(
                new int[][] {
                        new int[] {1959, 325100},
                        new int[] {1960, 373200}
                },
                1L,
                true);

        int dimension = data.consumeInt(2, 4);
        int pointCount = data.consumeInt(2, 6);
        int triggerCoord = data.consumeInt(0, dimension - 1);

        int[][] raw = new int[pointCount][dimension];
        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < dimension; j++) {
                raw[i][j] = data.consumeInt(-1_000_000, 1_000_000);
            }
        }

        int base = data.consumeInt(-500_000, 500_000);
        raw[0][triggerCoord] = base;
        int delta = data.consumeBoolean()
                ? data.consumeInt(46341, 900000)
                : -data.consumeInt(46341, 900000);
        long candidate = (long) base + (long) delta;
        if (candidate > 1_000_000L) {
            candidate = 1_000_000L;
        } else if (candidate < -1_000_000L) {
            candidate = -1_000_000L;
        }
        if (candidate == base) {
            candidate = base + (delta > 0 ? 46341 : -46341);
            if (candidate > 1_000_000L) {
                candidate = base - 46341L;
            }
            if (candidate < -1_000_000L) {
                candidate = base + 46341L;
            }
        }
        raw[1][triggerCoord] = (int) candidate;

        long seed = data.consumeInt();
        runScenario(raw, seed, true);
    }

    private static void runScenario(int[][] coords, long seed, boolean validByConstruction) {
        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(coords.length);
        for (int i = 0; i < coords.length; i++) {
            points.add(new EuclideanIntegerPoint(Arrays.copyOf(coords[i], coords[i].length)));
        }

        KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));

        List<Cluster<EuclideanIntegerPoint>> clusters;
        try {
            clusters = clusterer.cluster(points, 1, 1);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (clusters == null) {
            throw new RuntimeException("[oracle:k1-partition] metamorphic violation: cluster(points,1,1) returned null inputPoints=" + describe(points));
        }

        /*
         * Contract/oracle: k-means clustering partitions the input points into k clusters.
         * For k == 1 and a non-empty valid input list, a correct implementation must return exactly
         * one cluster containing every input point. A throw-deleting/branch-skipping patch could
         * avoid the crash yet silently drop assignments, which this observable post-condition catches.
         */
        if (clusters.size() != 1) {
            throw new RuntimeException("[oracle:k1-partition] metamorphic violation: expected exactly one cluster for k=1 inputPoints="
                    + describe(points) + " clusterCount=" + clusters.size());
        }

        Cluster<EuclideanIntegerPoint> only = clusters.get(0);
        if (only == null || only.getPoints() == null) {
            throw new RuntimeException("[oracle:k1-partition] metamorphic violation: sole cluster or its point list is null inputPoints="
                    + describe(points));
        }

        List<EuclideanIntegerPoint> assigned = only.getPoints();
        if (assigned.size() != points.size()) {
            throw new RuntimeException("[oracle:k1-partition] metamorphic violation: all input points must be assigned to the sole cluster for k=1 inputPoints="
                    + describe(points) + " assigned=" + assigned.size());
        }

        for (int i = 0; i < points.size(); i++) {
            EuclideanIntegerPoint p = points.get(i);
            if (!assigned.contains(p)) {
                throw new RuntimeException("[oracle:k1-partition] metamorphic violation: missing point from sole cluster inputPoints="
                        + describe(points) + " missing=" + p);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Validation")
                || name.contains("OutOfRange")
                || name.contains("NotPositive")
                || name.contains("NotStrictlyPositive")
                || name.contains("NullArgument")
                || name.contains("NoData")
                || name.contains("DimensionMismatch")
                || name.contains("Math");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
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

    private static String describe(List<EuclideanIntegerPoint> points) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(points.get(i));
        }
        sb.append(']');
        return sb.toString();
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
<details open><summary>▸ Output (~6,598 chars)</summary>

```
package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math.MathRuntimeException;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.EuclideanIntegerPoint;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runValidCase(
                new int[][] {
                        new int[] {1959, 325100},
                        new int[] {1960, 373200}
                },
                1L);

        int rounds = 1 + Math.max(0, Math.min(4, data.remainingBytes()));
        for (int r = 0; r < rounds; r++) {
            int pointCount = data.consumeInt(2, 6);
            int dims = data.consumeInt(1, 4);
            int overflowPoint = data.consumeInt(1, pointCount - 1);
            int overflowDim = data.consumeInt(0, dims - 1);

            int[][] coords = new int[pointCount][dims];

            for (int d = 0; d < dims; d++) {
                coords[0][d] = data.consumeInt(-200000, 200000);
            }

            for (int i = 1; i < pointCount; i++) {
                for (int d = 0; d < dims; d++) {
                    coords[i][d] = coords[0][d] + data.consumeInt(-1000, 1000);
                }
            }

            int base = coords[0][overflowDim];
            int delta = data.consumeBoolean() ? data.consumeInt(46341, 60000) : -data.consumeInt(46341, 60000);
            long candidate = (long) base + delta;
            if (candidate > 1000000L) {
                candidate = 1000000L;
            }
            if (candidate < -1000000L) {
                candidate = -1000000L;
            }
            coords[overflowPoint][overflowDim] = (int) candidate;

            for (int i = 1; i < pointCount; i++) {
                boolean allEqual = true;
                for (int d = 0; d < dims; d++) {
                    if (coords[i][d] != coords[0][d]) {
                        allEqual = false;
                        break;
                    }
                }
                if (allEqual) {
                    int nd = (overflowDim + 1) % dims;
                    if (coords[i][nd] < 1000000) {
                        coords[i][nd]++;
                    } else {
                        coords[i][nd]--;
                    }
                }
            }

            long seed = data.consumeInt();
            runValidCase(coords, seed);
        }
    }

    private static void runValidCase(int[][] rawPoints, long seed) {
        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(rawPoints.length);
        for (int i = 0; i < rawPoints.length; i++) {
            points.add(new EuclideanIntegerPoint(rawPoints[i]));
        }

        KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));

        List<Cluster<EuclideanIntegerPoint>> clusters;
        try {
            clusters = clusterer.cluster(points, 1, 1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            /* Contract/oracle: clustering with k=1 must produce exactly one cluster and all input points
             * must be assigned to that single cluster. A throw-deleting or branch-skipping patch could avoid
             * the buggy path but silently drop assignments, so we assert the observable result. */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException(
                        "[oracle:k1-all-assigned] metamorphic violation: k=1 must yield exactly one cluster input="
                                + Arrays.deepToString(rawPoints)
                                + " clusterCount=" + (clusters == null ? "null" : Integer.toString(clusters.size())));
            }
            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null || only.getPoints().size() != points.size()) {
                throw new RuntimeException(
                        "[oracle:k1-all-assigned] metamorphic violation: k=1 must assign every input point to the sole cluster input="
                                + Arrays.deepToString(rawPoints)
                                + " assigned=" + (only == null || only.getPoints() == null ? "null" : Integer.toString(only.getPoints().size()))
                                + " expected=" + points.size());
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof MathRuntimeException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("Illegal") || name.contains("MathRuntimeException");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(e.getClassName())
                    && "assignPointsToClusters".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.util.MathUtils".equals(e.getClassName())
                    && "distance".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
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
- trigger: java.lang.NullPointerException

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,155 chars)</summary>

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
        runAnchor();

        int dims = data.consumeInt(2, 6);
        int pointCount = data.consumeInt(2, 6);

        int triggerDim = data.consumeInt(0, dims - 1);
        int bigDiff = data.consumeInt(46341, 70000);
        if (data.consumeBoolean()) {
            bigDiff = -bigDiff;
        }

        int[][] raw = new int[pointCount][dims];
        for (int i = 0; i < pointCount; i++) {
            for (int d = 0; d < dims; d++) {
                raw[i][d] = data.consumeInt(-200000, 200000);
            }
        }

        int base = data.consumeInt(-200000, 200000);
        raw[0][triggerDim] = base;
        raw[1][triggerDim] = base + bigDiff;
        for (int d = 0; d < dims; d++) {
            if (d != triggerDim && data.consumeBoolean()) {
                raw[1][d] = raw[0][d];
            }
        }

        ArrayList<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>();
        for (int i = 0; i < pointCount; i++) {
            points.add(new EuclideanIntegerPoint(raw[i]));
        }

        long seed = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        runClusterAndCheck(points, seed);

        ArrayList<EuclideanIntegerPoint> reversed = new ArrayList<EuclideanIntegerPoint>(points);
        Collections.reverse(reversed);
        runClusterAndCheck(reversed, seed);
    }

    private static void runAnchor() {
        ArrayList<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>();
        points.add(new EuclideanIntegerPoint(new int[] { 1959, 325100 }));
        points.add(new EuclideanIntegerPoint(new int[] { 1960, 373200 }));
        runClusterAndCheck(points, 1746432956321L);
    }

    private static void runClusterAndCheck(List<EuclideanIntegerPoint> points, long seed) {
        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(seed));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(points, 1, 1);

            /* Contract/post-condition:
             * For k == 1, clustering a non-empty valid collection must yield exactly one cluster,
             * and every input point must be assigned to that single cluster. A throw-deleting or
             * branch-skipping patch that makes the crashing path unreachable would violate this by
             * returning the wrong number of clusters or silently dropping points.
             */
            if (points != null && !points.isEmpty()) {
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-single] metamorphic violation: k=1 must yield exactly one cluster input="
                            + points + " clusters=" + clusters);
                }
                List<EuclideanIntegerPoint> assigned = clusters.get(0).getPoints();
                if (assigned == null || assigned.size() != points.size()) {
                    throw new RuntimeException("[oracle:k1-size] metamorphic violation: all points must be assigned to the single cluster input="
                            + points + " assigned=" + assigned);
                }
                for (EuclideanIntegerPoint p : points) {
                    if (!assigned.contains(p)) {
                        throw new RuntimeException("[oracle:k1-membership] metamorphic violation: single cluster must contain every input point input="
                                + points + " assigned=" + assigned + " missing=" + p);
                    }
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.apache.commons.math")) {
                String simple = cur.getClass().getSimpleName().toLowerCase();
                if (simple.contains("illegal")
                        || simple.contains("invalid")
                        || simple.contains("range")
                        || simple.contains("argument")
                        || simple.contains("number")
                        || simple.contains("convergence")
                        || simple.contains("configuration")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        boolean assignFrame = false;
        boolean distanceFrame = false;
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls)
                    && "assignPointsToClusters".equals(method)) {
                assignFrame = true;
            }
            if ("org.apache.commons.math.util.MathUtils".equals(cls)
                    && "distance".equals(method)) {
                distanceFrame = true;
            }
        }
        return assignFrame || distanceFrame;
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
<details open><summary>▸ Output (~7,643 chars)</summary>

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
        {
            List<EuclideanIntegerPoint> anchorPoints = Arrays.asList(
                new EuclideanIntegerPoint(new int[] { 1959, 325100 }),
                new EuclideanIntegerPoint(new int[] { 1960, 373200 })
            );
            try {
                KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(1746432956321L));
                List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(anchorPoints, 1, 1);

                /* Contract/oracle:
                 * With k == 1 and valid non-null input points, the clustering result must contain exactly one
                 * cluster, and every input point must be assigned to that sole cluster. A "fix" that merely
                 * skips the failing assignment path or silently drops points would violate this observable API
                 * post-condition without throwing.
                 */
                if (clusters != null && clusters.size() == 1) {
                    Cluster<EuclideanIntegerPoint> only = clusters.get(0);
                    if (only == null || only.getPoints() == null || only.getPoints().size() != anchorPoints.size()) {
                        throw new RuntimeException("[oracle:kmeans-k1-anchor] metamorphic violation: sole cluster must contain all input points input=" + anchorPoints + " clusterCount=" + (clusters == null ? -1 : clusters.size()) + " assigned=" + (only == null || only.getPoints() == null ? -1 : only.getPoints().size()));
                    }
                    for (EuclideanIntegerPoint p : anchorPoints) {
                        if (!only.getPoints().contains(p)) {
                            throw new RuntimeException("[oracle:kmeans-k1-anchor] metamorphic violation: sole cluster missing input point input=" + anchorPoints + " missing=" + p + " assigned=" + only.getPoints());
                        }
                    }
                } else {
                    throw new RuntimeException("[oracle:kmeans-k1-anchor] metamorphic violation: k=1 must yield exactly one cluster input=" + anchorPoints + " clusterCount=" + (clusters == null ? -1 : clusters.size()));
                }
            } catch (RuntimeException t) {
                boolean rootCause = t instanceof NullPointerException;
                if (rootCause) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        String cls = st[i].getClassName();
                        String m = st[i].getMethodName();
                        if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls) && "assignPointsToClusters".equals(m))
                                || ("org.apache.commons.math.util.MathUtils".equals(cls) && "distance".equals(m))) {
                            throw t;
                        }
                    }
                }
            }
        }

        int dim = data.consumeInt(1, 4);
        int n = data.consumeInt(2, 6);
        int overflowIndex = data.consumeInt(0, dim - 1);

        List<EuclideanIntegerPoint> points = new ArrayList<EuclideanIntegerPoint>(n);

        int[] a = new int[dim];
        int[] b = new int[dim];
        for (int i = 0; i < dim; i++) {
            a[i] = data.consumeInt(-1_000_000, 1_000_000);
            b[i] = data.consumeInt(-1_000_000, 1_000_000);
        }

        int delta = data.consumeInt(46341, 100000);
        int sign = data.consumeBoolean() ? 1 : -1;
        int baseMin = -1_000_000;
        int baseMax = 1_000_000;
        if (sign > 0) {
            baseMax = 1_000_000 - delta;
        } else {
            baseMin = -1_000_000 + delta;
        }
        int base = data.consumeInt(baseMin, baseMax);
        a[overflowIndex] = base;
        b[overflowIndex] = base + sign * delta;

        points.add(new EuclideanIntegerPoint(a));
        points.add(new EuclideanIntegerPoint(b));

        for (int pi = 2; pi < n; pi++) {
            int[] coords = new int[dim];
            for (int j = 0; j < dim; j++) {
                coords[j] = data.consumeInt(-1_000_000, 1_000_000);
            }
            points.add(new EuclideanIntegerPoint(coords));
        }

        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> clusterer =
                new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(data.consumeInt()));
            List<Cluster<EuclideanIntegerPoint>> clusters = clusterer.cluster(points, 1, 1);

            /* Contract/oracle:
             * For any valid non-empty list of non-null points and k == 1, there is exactly one resulting cluster
             * and all input points must be assigned to it. This directly checks that the real clustering logic
             * still performs point assignment after traversing the distance computation path, instead of "fixing"
             * the crash by making that path unreachable or by dropping assignments.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:kmeans-k1-fuzz] metamorphic violation: k=1 must yield exactly one cluster inputSize=" + points.size() + " dim=" + dim + " clusterCount=" + (clusters == null ? -1 : clusters.size()));
            }
            Cluster<EuclideanIntegerPoint> only = clusters.get(0);
            if (only == null || only.getPoints() == null) {
                throw new RuntimeException("[oracle:kmeans-k1-fuzz] metamorphic violation: sole cluster and its point list must be non-null inputSize=" + points.size() + " dim=" + dim);
            }
            if (only.getPoints().size() != points.size()) {
                throw new RuntimeException("[oracle:kmeans-k1-fuzz] metamorphic violation: sole cluster must contain all input points inputSize=" + points.size() + " dim=" + dim + " assigned=" + only.getPoints().size() + " points=" + points + " assignedPoints=" + only.getPoints());
            }
            for (EuclideanIntegerPoint p : points) {
                if (!only.getPoints().contains(p)) {
                    throw new RuntimeException("[oracle:kmeans-k1-fuzz] metamorphic violation: sole cluster missing input point inputSize=" + points.size() + " dim=" + dim + " missing=" + p + " assignedPoints=" + only.getPoints());
                }
            }
        } catch (RuntimeException t) {
            boolean rootCause = t instanceof NullPointerException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cls = st[i].getClassName();
                    String m = st[i].getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cls) && "assignPointsToClusters".equals(m))
                            || ("org.apache.commons.math.util.MathUtils".equals(cls) && "distance".equals(m))) {
                        throw t;
                    }
                }
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
<details open><summary>▸ Output (~8,153 chars)</summary>

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
                    new EuclideanIntegerPoint(new int[] {1959, 325100}),
                    new EuclideanIntegerPoint(new int[] {1960, 373200}),
            };
            try {
                List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(points), 1, 1);

                /* Contract/post-condition:
                 * Clustering a non-empty input into exactly one cluster must return exactly one cluster
                 * containing all input points. The upstream failing test asserts this directly.
                 * A throw-deleting or branch-skipping patch could avoid the crash but silently lose
                 * assignments/bookkeeping, which this observable size check catches.
                 */
                if (clusters == null || clusters.size() != 1) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: k=1 must yield exactly one cluster input=2 lhs=" + (clusters == null ? "null" : clusters.size()) + " rhs=1");
                }
                if (clusters.get(0) == null || clusters.get(0).getPoints() == null || clusters.get(0).getPoints().size() != 2) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: single cluster must contain all input points input=2 lhs=" + (clusters.get(0) == null || clusters.get(0).getPoints() == null ? "null" : clusters.get(0).getPoints().size()) + " rhs=2");
                }
                if (!clusters.get(0).getPoints().contains(points[0]) || !clusters.get(0).getPoints().contains(points[1])) {
                    throw new RuntimeException("[oracle:k1-anchor] metamorphic violation: single cluster must contain each original point input=2 lhs=" + clusters.get(0).getPoints() + " rhs=" + Arrays.asList(points));
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException
                        || t instanceof NumberFormatException
                        || (t.getClass().getName().startsWith("org.apache.commons.math")
                            && t.getClass().getName().endsWith("Exception")
                            && !(t instanceof NullPointerException));
                if (cleanRejection) {
                    return;
                }

                boolean rootCause = t instanceof NullPointerException;
                if (rootCause) {
                    boolean sawRelevantFrame = false;
                    for (StackTraceElement ste : t.getStackTrace()) {
                        String cn = ste.getClassName();
                        String mn = ste.getMethodName();
                        if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn) && "assignPointsToClusters".equals(mn))
                                || ("org.apache.commons.math.util.MathUtils".equals(cn) && "distance".equals(mn))) {
                            sawRelevantFrame = true;
                            break;
                        }
                    }
                    rootCause = sawRelevantFrame;
                }
                if (rootCause) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                }
                return;
            }
        }

        int pointCount = data.consumeInt(2, 6);
        int dim = data.consumeInt(2, 5);
        int overflowPos = data.consumeInt(0, dim - 1);

        EuclideanIntegerPoint[] fuzzPoints = new EuclideanIntegerPoint[pointCount];
        int[][] raw = new int[pointCount][dim];

        for (int i = 0; i < pointCount; i++) {
            for (int j = 0; j < dim; j++) {
                raw[i][j] = data.consumeInt(-200000, 200000);
            }
            raw[i][0] += i;
        }

        int base = data.consumeInt(-200000, 200000);
        int diff = data.consumeInt(46341, 100000);
        if (data.consumeBoolean()) {
            diff = -diff;
        }
        raw[0][overflowPos] = base;
        raw[1][overflowPos] = base + diff;

        for (int i = 0; i < pointCount; i++) {
            fuzzPoints[i] = new EuclideanIntegerPoint(raw[i]);
        }

        try {
            KMeansPlusPlusClusterer<EuclideanIntegerPoint> transformer =
                    new KMeansPlusPlusClusterer<EuclideanIntegerPoint>(new Random(data.consumeInt()));
            List<Cluster<EuclideanIntegerPoint>> clusters = transformer.cluster(Arrays.asList(fuzzPoints), 1, 1);

            /* Contract/post-condition:
             * For a valid non-empty point list and k=1, all points must be assigned to the sole cluster.
             * These inputs are valid by construction: non-null list, non-null points, uniform dimensions,
             * pointCount >= 2, k == 1, maxIterations == 1. If a patch merely suppresses the buggy path,
             * it may return an incomplete assignment instead of crashing; this check exposes that.
             */
            if (clusters == null || clusters.size() != 1) {
                throw new RuntimeException("[oracle:k1-explore] metamorphic violation: k=1 must yield exactly one cluster input=" + Arrays.deepToString(raw) + " lhs=" + (clusters == null ? "null" : clusters.size()) + " rhs=1");
            }
            if (clusters.get(0) == null || clusters.get(0).getPoints() == null || clusters.get(0).getPoints().size() != pointCount) {
                throw new RuntimeException("[oracle:k1-explore] metamorphic violation: single cluster must contain all input points input=" + Arrays.deepToString(raw) + " lhs=" + (clusters.get(0) == null || clusters.get(0).getPoints() == null ? "null" : clusters.get(0).getPoints().size()) + " rhs=" + pointCount);
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || (t.getClass().getName().startsWith("org.apache.commons.math")
                        && t.getClass().getName().endsWith("Exception")
                        && !(t instanceof NullPointerException));
            if (cleanRejection) {
                return;
            }

            boolean rootCause = t instanceof NullPointerException;
            if (rootCause) {
                boolean sawRelevantFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cn = ste.getClassName();
                    String mn = ste.getMethodName();
                    if (("org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer".equals(cn) && "assignPointsToClusters".equals(mn))
                            || ("org.apache.commons.math.util.MathUtils".equals(cn) && "distance".equals(mn))) {
                        sawRelevantFrame = true;
                        break;
                    }
                }
                rootCause = sawRelevantFrame;
            }
            if (rootCause) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
            }
        }
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
