package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();
        relationCountOverSum(data);
        relationObjectComparableAgree(data);
        hiddenStateReadOnlyCheck(data);
        extraSiblingAgreement(data);
    }

    private static void liftedTestOracles() {
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;
        Frequency f = new Frequency();

        try {
            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);
        } catch (Throwable t) {
            return;
        }

        checkLiftedDouble("one-pct", "f.getPct(1)", 0.25, safeGetPctInt(f, 1));
        checkLiftedDouble("two-pct", "f.getPct(Long.valueOf(2))", 0.25, safeGetPctLongObject(f, Long.valueOf(2)));
        checkLiftedDouble("three-pct", "f.getPct(threeL)", 0.5, safeGetPctLongPrimitive(f, threeL));
        checkLiftedDouble("three-object-pct", "f.getPct((Object) Integer.valueOf(3))", 0.5, safeGetPctObject(f, Integer.valueOf(3)));
        checkLiftedDouble("five-pct", "f.getPct(5)", 0.0, safeGetPctInt(f, 5));
        checkLiftedDouble("foo-pct", "f.getPct(\"foo\")", 0.0, safeGetPctObject(f, "foo"));
        checkLiftedDouble("one-cum-pct", "f.getCumPct(1)", 0.25, safeGetCumPctInt(f, 1));
        checkLiftedDouble("two-cum-pct", "f.getCumPct(Long.valueOf(2))", 0.50, safeGetCumPctLongObject(f, Long.valueOf(2)));
        checkLiftedDouble("integer-argument", "f.getCumPct(Integer.valueOf(2))", 0.50, safeGetCumPctObject(f, Integer.valueOf(2)));
        checkLiftedDouble("three-cum-pct", "f.getCumPct(threeL)", 1.0, safeGetCumPctLongPrimitive(f, threeL));
        checkLiftedDouble("five-cum-pct", "f.getCumPct(5)", 1.0, safeGetCumPctInt(f, 5));
        checkLiftedDouble("zero-cum-pct", "f.getCumPct(0)", 0.0, safeGetCumPctInt(f, 0));
        checkLiftedDouble("foo-cum-pct", "f.getCumPct(\"foo\")", 0.0, safeGetCumPctObject(f, "foo"));

        // Documented guarantee used as post-condition: getPct(Object) "Returns the percentage of values that are equal to v"
        // and getCumPct(Object) is the cumulative percentage. Therefore for the same built fixture,
        // getPct((Object) Integer.valueOf(3)) must be the exact per-value percentage, not the cumulative one.
        // A patch that simply redirects to the cumulative path violates this observable contract without throwing.
        double actualPctObj = safeGetPctObject(f, Integer.valueOf(3));
        double expectedFromCount = ((double) f.getCount((Comparable<?>) Integer.valueOf(3))) / ((double) f.getSumFreq());
        if (!doubleEquals(actualPctObj, expectedFromCount, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:post-count-over-sum] semantic mismatch: getPct((Object) Integer.valueOf(3)) expected=" + expectedFromCount + " actual=" + actualPctObj + " count=" + f.getCount((Comparable<?>) Integer.valueOf(3)) + " sum=" + f.getSumFreq());
        }
    }

    private static void relationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
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
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Comparable<?>) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }

        if (!doubleEquals(actual, expected, 1e-9)) {
            throw new FuzzerSecurityIssueLow("relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum + " probe=" + probe);
        }
    }

    private static void relationObjectComparableAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double a;
        double b;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            a = f.getPct((Object) probe);
            b = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        if (!doubleEquals(a, b, 1e-9)) {
            throw new FuzzerSecurityIssueLow("relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a + " comparable=" + b + " probe=" + probe);
        }
    }

    private static void hiddenStateReadOnlyCheck(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        long sumBefore;
        long sumAfter;
        int hashBefore;
        int hashAfter;
        String toStringBefore;
        String toStringAfter;
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

            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            toStringBefore = f.toString();

            f.getPct((Object) probe);
            f.getCumPct((Object) probe);
            f.getPct((Comparable<?>) probe);

            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            toStringAfter = f.toString();
        } catch (Throwable t) {
            return;
        }

        // Documented guarantee: get* methods are read-only queries; they must not mutate observable state.
        if (sumBefore != sumAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-sum] semantic mismatch: read-only get* changed getSumFreq before=" + sumBefore + " after=" + sumAfter + " probe=" + probe);
        }
        if (hashBefore != hashAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-hash] semantic mismatch: read-only get* changed hashCode before=" + hashBefore + " after=" + hashAfter + " probe=" + probe);
        }
        if (!safeStringEquals(toStringBefore, toStringAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-string] semantic mismatch: read-only get* changed toString before=" + escapeOneLine(toStringBefore) + " after=" + escapeOneLine(toStringAfter) + " probe=" + probe);
        }
    }

    private static void extraSiblingAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long c1;
        long c2;
        double p1;
        double p2;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-8, 8);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f.addValue(v);
                        break;
                    default:
                        f.addValue((long) v);
                        break;
                }
            }
            probe = data.consumeInt(-8, 8);
            c1 = f.getCount(probe);
            c2 = f.getCount((long) probe);
            p1 = f.getPct(probe);
            p2 = f.getPct((long) probe);
        } catch (Throwable t) {
            return;
        }

        // Class contract used by the tests: integer values are not distinguished by type for these get* methods.
        if (c1 != c2) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-count] semantic mismatch: getCount(int) and getCount(long) disagree probe=" + probe + " intCount=" + c1 + " longCount=" + c2);
        }
        if (!doubleEquals(p1, p2, 1e-9)) {
            throw new FuzzerSecurityIssueLow("[oracle:sibling-pct] semantic mismatch: getPct(int) and getPct(long) disagree probe=" + probe + " intPct=" + p1 + " longPct=" + p2);
        }
    }

    private static double safeGetPctInt(Frequency f, int v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetPctLongPrimitive(Frequency f, long v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetPctLongObject(Frequency f, Long v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetPctObject(Frequency f, Object v) {
        try {
            return f.getPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctInt(Frequency f, int v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctLongPrimitive(Frequency f, long v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctLongObject(Frequency f, Long v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctObject(Frequency f, Object v) {
        try {
            return f.getCumPct(v);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static void checkLiftedDouble(String id, String what, double expected, double actual) {
        if (!doubleEquals(expected, actual, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean doubleEquals(double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        if (a == b) {
            return true;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= tol * scale;
    }

    private static boolean safeStringEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}