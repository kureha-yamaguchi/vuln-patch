package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Exact reconstruction of FrequencyTest.testPcts setup and assertions.
        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
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

        checkDouble("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        checkDouble("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        checkDouble("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));
        checkDouble("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        checkDouble("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        checkDouble("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        checkDouble("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        checkDouble("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        checkDouble("lifted-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        checkDouble("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        checkDouble("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        checkDouble("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        checkDouble("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // Hidden-state/read-only check:
        // getPct/getCumPct are query methods ("Returns ..."); they must not mutate observable state.
        // A patch that "fixes" the bug by changing bookkeeping or internal contents during a read would violate this.
        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();
        double ignored = f.getPct((Object) Integer.valueOf(3));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-readonly] semantic mismatch: read-only getPct(Object) changed observable state"
                    + " sumBefore=" + sumBefore
                    + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashAfter
                    + " toStringBefore=" + String.valueOf(stringBefore)
                    + " toStringAfter=" + String.valueOf(stringAfter));
        }

        relationGetPctObjectMatchesCountOverSumForIntegerObject(data);
        relationGetPctObjectAndComparableAgreeOnSameIntegerValue(data);

        // Additional trusted generalisation:
        // For any non-empty Frequency, getPct(v) = getCount(v)/getSumFreq by the documented meaning of percentage.
        // We construct the distribution ourselves from chosen integral values, so the expected answer is trusted.
        additionalConstructedOracle(data);
    }

    private static void additionalConstructedOracle(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        long count;
        long sum;
        double actual;
        double expected;
        try {
            f = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
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
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            count = f.getCount((Comparable<?>) probe);
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:constructed-count-over-sum] semantic mismatch: getPct(Comparable) disagrees with getCount/getSumFreq"
                    + " probe=" + probe
                    + " actual=" + actual
                    + " expected=" + expected
                    + " count=" + count
                    + " sum=" + sum);
        }
    }

    private static void relationGetPctObjectMatchesCountOverSumForIntegerObject(FuzzedDataProvider data) {
        Frequency f = null;
        int probe = 0;
        long count = 0L;
        long sum = 0L;
        double expected = 0.0;
        double actual = 0.0;
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
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
            ready = true;
        } catch (Throwable t) {
            return;
        }
        if (ready && !closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: probe="
                    + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void relationGetPctObjectAndComparableAgreeOnSameIntegerValue(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        double a = 0.0;
        double b = 0.0;
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
            a = f.getPct((Object) probe);
            b = f.getPct((Comparable<?>) probe);
            ready = true;
        } catch (Throwable t) {
            return;
        }
        if (ready && !closeEnough(a, b)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe="
                    + probe + " object=" + a + " comparable=" + b);
        }
    }

    private static void checkDouble(String oracleId, String label, double expected, double actual) {
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean closeEnough(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}