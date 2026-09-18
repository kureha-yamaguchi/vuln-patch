package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TEST_TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency exact = new Frequency();

        exact.addValue(1L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(2L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(1);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(2);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(3L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(3L);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(3);
        reprobeAbsentNumericAfterMutation(exact);
        exact.addValue(Integer.valueOf(3));
        reprobeAbsentNumericAfterMutation(exact);

        checkApprox("lifted-one-pct", "one pct", 0.25, exact.getPct(1), TEST_TOLERANCE);
        checkApprox("lifted-two-pct", "two pct", 0.25, exact.getPct(Long.valueOf(2)), TEST_TOLERANCE);
        checkApprox("lifted-three-pct", "three pct", 0.5, exact.getPct(3L), TEST_TOLERANCE);

        long beforeSum = exact.getSumFreq();
        int beforeHash = exact.hashCode();
        String beforeString = exact.toString();
        double objectThreePct = exact.getPct((Object) (Integer.valueOf(3)));
        long afterSum = exact.getSumFreq();
        int afterHash = exact.hashCode();
        String afterString = exact.toString();
        checkApprox("lifted-three-object-pct", "three (Object) pct", 0.5, objectThreePct, TEST_TOLERANCE);
        // Frequency#getPct is a read-only query ("Returns the percentage..."), so observable state readers must not change.
        // A throw-deleting or miswired patch that mutates bookkeeping while answering would violate this post-condition.
        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-getPctObject] semantic mismatch: read-only getPct(Object) changed state beforeSum="
                    + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                    + " beforeToString=" + beforeString + " afterToString=" + afterString);
        }

        checkApprox("lifted-five-pct", "five pct", 0.0, exact.getPct(5), TEST_TOLERANCE);
        checkApprox("lifted-foo-pct", "foo pct", 0.0, exact.getPct("foo"), TEST_TOLERANCE);
        checkApprox("lifted-one-cum-pct", "one cum pct", 0.25, exact.getCumPct(1), TEST_TOLERANCE);
        checkApprox("lifted-two-cum-pct", "two cum pct", 0.50, exact.getCumPct(Long.valueOf(2)), TEST_TOLERANCE);
        checkApprox("lifted-integer-arg-cum-pct", "Integer argument", 0.50, exact.getCumPct(Integer.valueOf(2)), TEST_TOLERANCE);
        checkApprox("lifted-three-cum-pct", "three cum pct", 1.0, exact.getCumPct(3L), TEST_TOLERANCE);
        checkApprox("lifted-five-cum-pct", "five cum pct", 1.0, exact.getCumPct(5), TEST_TOLERANCE);
        checkApprox("lifted-zero-cum-pct", "zero cum pct", 0.0, exact.getCumPct(0), TEST_TOLERANCE);
        checkApprox("lifted-foo-cum-pct", "foo cum pct", 0.0, exact.getCumPct("foo"), TEST_TOLERANCE);

        relationGetPctMatchesCountOverSum(data);
        relationObjectAndComparableAgree(data);
        relationCrossTypeIntegerAndLongAgree(data);
        relationReadOnlyGettersDoNotMutate(data);
    }

    private static void reprobeAbsentNumericAfterMutation(Frequency f) {
        checkApprox("reprobe-five-pct", "five pct after mutation", 0.0, f.getPct(5), TEST_TOLERANCE);
        checkApprox("reprobe-five-cum-pct", "five cum pct after mutation", 1.0, f.getCumPct(5), TEST_TOLERANCE);
    }

    private static void relationGetPctMatchesCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double actual;
        double expected;
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
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }
        if (!approxEquals(actual, expected, 1e-9)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe
                    + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void relationObjectAndComparableAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double objectResult;
        double comparableResult;
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
            objectResult = f.getPct((Object) probe);
            comparableResult = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }
        if (!approxEquals(objectResult, comparableResult, 1e-9)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe
                    + " object=" + objectResult + " comparable=" + comparableResult);
        }
    }

    private static void relationCrossTypeIntegerAndLongAgree(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double integerPct;
        double longPct;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-8, 8);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = data.consumeInt(-8, 8);
            // The Frequency class docs state integer values are not distinguished by type for getPct/getCumPct/getCount.
            integerPct = f.getPct((Object) Integer.valueOf(probe));
            longPct = f.getPct(Long.valueOf(probe));
        } catch (Throwable t) {
            return;
        }
        if (!approxEquals(integerPct, longPct, 1e-9)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:cross-type-pct] metamorphic violation: equivalent integer and long inputs must have same percentage probe="
                    + probe + " integerPct=" + integerPct + " longPct=" + longPct);
        }
    }

    private static void relationReadOnlyGettersDoNotMutate(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        long sumBefore;
        int hashBefore;
        String stringBefore;
        double pctObject;
        double pctComparable;
        double cumPctObject;
        long sumAfter;
        int hashAfter;
        String stringAfter;
        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-4, 4);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f.addValue(v);
                        break;
                    default:
                        f.addValue((long) v);
                        break;
                }
            }
            probe = Integer.valueOf(data.consumeInt(-4, 4));
            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            stringBefore = f.toString();
            pctObject = f.getPct((Object) probe);
            pctComparable = f.getPct((Comparable<?>) probe);
            cumPctObject = f.getCumPct((Object) probe);
            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            stringAfter = f.toString();
            if (Double.isNaN(pctObject) || Double.isNaN(pctComparable) || Double.isNaN(cumPctObject)) {
                return;
            }
        } catch (Throwable t) {
            return;
        }
        // get* methods are queries over existing counts; they are documented as returning frequencies/percentages, not mutating.
        // Therefore these reads must leave cheap observable state unchanged.
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:read-only-getters] metamorphic violation: read-only getters changed receiver state probe=" + probe
                    + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " stringBefore=" + stringBefore + " stringAfter=" + stringAfter);
        }
    }

    private static void checkApprox(String oracleId, String what, double expected, double actual, double tolerance) {
        if (!approxEquals(actual, expected, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b, double tolerance) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= tolerance * scale;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}