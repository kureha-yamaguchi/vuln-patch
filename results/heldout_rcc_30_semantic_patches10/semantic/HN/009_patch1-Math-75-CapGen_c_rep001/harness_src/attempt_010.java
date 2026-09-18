package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkHiddenStateOnReadOnlyCall();
        checkRelationCountOverSum(data);
        checkRelationObjectComparableAgreement(data);
        checkAdditionalSiblingAgreement(data);
    }

    private static void checkLiftedTestOracles() {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;
        double tolerance = 10E-15;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1), tolerance);
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)), tolerance);
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL), tolerance);
        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance);
        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5), tolerance);
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"), tolerance);
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1), tolerance);
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance);
        assertDoubleEquals("lifted-integer-argument-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance);
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL), tolerance);
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5), tolerance);
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0), tolerance);
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"), tolerance);
    }

    private static void checkHiddenStateOnReadOnlyCall() {
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
        String toStringBefore = f.toString();

        double actual = f.getPct((Object) Integer.valueOf(3));

        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String toStringAfter = f.toString();

        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(toStringBefore, toStringAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-readonly] semantic mismatch: getPct(Object) is a read-only query, so getSumFreq/hashCode/toString must remain unchanged; pct=" + actual + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " toStringBefore=" + escapeOneLine(toStringBefore) + " toStringAfter=" + escapeOneLine(toStringAfter));
        }
    }

    private static void checkRelationCountOverSum(FuzzedDataProvider data) {
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
            if (sum <= 0) {
                return;
            }
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }

        if (!approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void checkRelationObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double objectPct;
        double comparablePct;

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
            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        if (!approxEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow("[oracle:object-vs-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe + " objectPct=" + objectPct + " comparablePct=" + comparablePct);
        }
    }

    private static void checkAdditionalSiblingAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double intPct;
        double longPct;
        long sumBefore;
        long sumAfter;
        int hashBefore;
        int hashAfter;
        String stringBefore;
        String stringAfter;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-7, 7);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = data.consumeInt(-7, 7);

            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            stringBefore = f.toString();

            intPct = f.getPct(probe);
            longPct = f.getPct(Long.valueOf(probe));

            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            stringAfter = f.toString();
        } catch (Throwable t) {
            return;
        }

        if (!approxEquals(intPct, longPct)) {
            throw new FuzzerSecurityIssueLow("[oracle:int-vs-long-sibling] metamorphic violation: equivalent integer inputs through getPct(int) and getPct(Long) must agree; probe=" + probe + " lhs=" + intPct + " rhs=" + longPct);
        }

        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:fuzz-hidden-state] metamorphic violation: read-only getPct overloads must not mutate observable state; probe=" + probe + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " toStringBefore=" + escapeOneLine(stringBefore) + " toStringAfter=" + escapeOneLine(stringAfter));
        }
    }

    private static void assertDoubleEquals(String oracleId, String label, double expected, double actual, double tolerance) {
        if (!(Math.abs(expected - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual + " tolerance=" + tolerance);
        }
    }

    private static boolean approxEquals(double a, double b) {
        return Math.abs(a - b) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}