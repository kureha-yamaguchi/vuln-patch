package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();
        hiddenStateOracle();
        relationGetPctObjectMatchesCountOverSumForIntegerObject(data);
        relationGetPctObjectAndComparableAgreeOnSameIntegerValue(data);
    }

    private static void liftedTestOracles() {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("test-one-pct", 0.25, f.getPct(1), "f.getPct(1)");
        assertDoubleEquals("test-two-pct", 0.25, f.getPct(Long.valueOf(2)), "f.getPct(Long.valueOf(2))");
        assertDoubleEquals("test-three-pct", 0.5, f.getPct(threeL), "f.getPct(threeL)");
        assertDoubleEquals("test-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), "f.getPct((Object) Integer.valueOf(3))");
        assertDoubleEquals("test-five-pct", 0.0, f.getPct(5), "f.getPct(5)");
        assertDoubleEquals("test-foo-pct", 0.0, f.getPct("foo"), "f.getPct(\"foo\")");
        assertDoubleEquals("test-one-cum-pct", 0.25, f.getCumPct(1), "f.getCumPct(1)");
        assertDoubleEquals("test-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)), "f.getCumPct(Long.valueOf(2))");
        assertDoubleEquals("test-integer-argument-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)), "f.getCumPct(Integer.valueOf(2))");
        assertDoubleEquals("test-three-cum-pct", 1.0, f.getCumPct(threeL), "f.getCumPct(threeL)");
        assertDoubleEquals("test-five-cum-pct", 1.0, f.getCumPct(5), "f.getCumPct(5)");
        assertDoubleEquals("test-zero-cum-pct", 0.0, f.getCumPct(0), "f.getCumPct(0)");
        assertDoubleEquals("test-foo-cum-pct", 0.0, f.getCumPct("foo"), "f.getCumPct(\"foo\")");
    }

    private static void hiddenStateOracle() {
        Frequency f = new Frequency();
        f.addValue(1L);
        f.addValue(2L);
        f.addValue(1);
        f.addValue(2);
        f.addValue(3L);
        f.addValue(3L);
        f.addValue(3);
        f.addValue(Integer.valueOf(3));

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();

        double ignored = f.getPct((Object) Integer.valueOf(3));

        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();

        if (ignored < -1.0) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-unreachable] semantic mismatch: impossible guard");
        }

        /* getPct is a read-only query ("Returns the percentage..."), so it must not mutate visible state.
           A patch that dodges the wrong branch by modifying internal bookkeeping would violate these cheap readers. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-getPctObject-readonly] semantic mismatch: getPct((Object) Integer.valueOf(3)) changed read-only state"
                    + " sumBefore=" + sumBefore
                    + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashAfter
                    + " toStringBefore=" + escape(stringBefore)
                    + " toStringAfter=" + escape(stringAfter)
            );
        }
    }

    private static void relationGetPctObjectMatchesCountOverSumForIntegerObject(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        long count = 0L;
        long sum = 0L;
        double actual = Double.NaN;
        double expected = Double.NaN;
        boolean ready = false;

        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = Integer.valueOf(data.consumeInt(-5, 5));
            count = f.getCount((Object) probe);
            sum = f.getSumFreq();
            if (sum <= 0) {
                return;
            }
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) probe);
            ready = true;
        } catch (Throwable e) {
            return;
        }

        /* Contract: getPct(Object) returns the percentage of values equal to v; with non-empty frequency data,
           that observable percentage is exactly getCount(v) / getSumFreq(). This catches a wrong-value patch
           even if it merely suppresses the buggy branch instead of throwing. */
        if (ready && !doubleEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe
                    + " actual=" + actual
                    + " expected=" + expected
                    + " count=" + count
                    + " sum=" + sum
            );
        }
    }

    private static void relationGetPctObjectAndComparableAgreeOnSameIntegerValue(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        double objectPct = Double.NaN;
        double comparablePct = Double.NaN;
        boolean ready = false;

        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);
            ready = true;
        } catch (Throwable e) {
            return;
        }

        /* The deprecated Object overload is documented as the same query as the Comparable overload on the same
           logical value; integer values are not distinguished by numeric wrapper type in Frequency. */
        if (ready && !doubleEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe
                    + " object=" + objectPct
                    + " comparable=" + comparablePct
            );
        }
    }

    private static void assertDoubleEquals(String oracleId, double expected, double actual, String what) {
        if (!doubleEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " expected=" + expected
                    + " actual=" + actual
            );
        }
    }

    private static boolean doubleEquals(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}