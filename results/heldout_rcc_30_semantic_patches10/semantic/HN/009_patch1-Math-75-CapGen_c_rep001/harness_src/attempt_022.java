package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestPctsOracles();
        generalizedCountOverSumRelation(data);
        generalizedObjectComparableAgreement(data);
    }

    private static void liftedTestPctsOracles() {
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

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));

        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeToString = f.toString();
        double objectPct = f.getPct((Object) (Integer.valueOf(3)));
        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterToString = f.toString();

        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, objectPct);
        if (beforeSum != afterSum) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-sum] semantic mismatch: getPct(Object) is a read-only query, so getSumFreq must not change; before=" + beforeSum + " after=" + afterSum);
        }
        if (beforeHash != afterHash) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-hash] semantic mismatch: getPct(Object) is a read-only query, so hashCode must not change; before=" + beforeHash + " after=" + afterHash);
        }
        if (!safeEquals(beforeToString, afterToString)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-string] semantic mismatch: getPct(Object) is a read-only query, so toString must not change; before=" + escapeOneLine(beforeToString) + " after=" + escapeOneLine(afterToString));
        }

        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-integer-argument-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // The deprecated Object overload is documented as the same percentage query as the Comparable overload.
        // A patch that merely avoids the original wrong path but returns another wrong value would violate this sibling agreement.
        double comparablePct = f.getPct((Comparable<?>) Integer.valueOf(3));
        if (!approxEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-object-vs-comparable] metamorphic violation: getPct(Object) and getPct(Comparable) must agree for the same Integer value input=3 lhs=" + objectPct + " rhs=" + comparablePct);
        }

        // For a non-empty Frequency, percentage is count divided by total frequency by the documented meaning of getPct.
        long countThree = f.getCount((Object) Integer.valueOf(3));
        long sum = f.getSumFreq();
        double expectedFromCounts = (double) countThree / (double) sum;
        if (!approxEquals(objectPct, expectedFromCounts)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-count-over-sum] metamorphic violation: getPct(Object) must equal getCount(v)/getSumFreq() for input=3 lhs=" + objectPct + " rhs=" + expectedFromCounts + " count=" + countThree + " sum=" + sum);
        }
    }

    private static void generalizedCountOverSumRelation(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        long count = 0L;
        long sum = 0L;
        double actual = 0.0;
        double expected = 0.0;
        boolean ready = false;
        Exception caught = null;

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

            // get* methods are read-only queries; capture cheap observable state before/after to detect hidden mutation.
            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String stringBefore = f.toString();

            count = f.getCount((Object) probe);
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) probe);

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String stringAfter = f.toString();
            if (sumBefore != sumAfter) {
                throw new FuzzerSecurityIssueLow("[oracle:general-hidden-state-sum] semantic mismatch: read-only getPct(Object) changed getSumFreq before=" + sumBefore + " after=" + sumAfter);
            }
            if (hashBefore != hashAfter) {
                throw new FuzzerSecurityIssueLow("[oracle:general-hidden-state-hash] semantic mismatch: read-only getPct(Object) changed hashCode before=" + hashBefore + " after=" + hashAfter);
            }
            if (!safeEquals(stringBefore, stringAfter)) {
                throw new FuzzerSecurityIssueLow("[oracle:general-hidden-state-string] semantic mismatch: read-only getPct(Object) changed toString before=" + escapeOneLine(stringBefore) + " after=" + escapeOneLine(stringAfter));
            }

            ready = true;
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            caught = e;
        }

        if (caught != null) {
            return;
        }
        if (ready && !approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void generalizedObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f = null;
        Integer probe = null;
        double objectResult = 0.0;
        double comparableResult = 0.0;
        boolean ready = false;
        Exception caught = null;

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
            objectResult = f.getPct((Object) probe);
            comparableResult = f.getPct((Comparable<?>) probe);
            ready = true;
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            caught = e;
        }

        if (caught != null) {
            return;
        }
        if (ready && !approxEquals(objectResult, comparableResult)) {
            throw new FuzzerSecurityIssueLow("relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe + " object=" + objectResult + " comparable=" + comparableResult);
        }
    }

    private static void assertDoubleEquals(String oracleId, String what, double expected, double actual) {
        if (!approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
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