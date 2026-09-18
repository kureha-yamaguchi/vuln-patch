package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        assertDoubleEquals("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertDoubleEquals("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();
        double objectPct = f.getPct((Object) (Integer.valueOf(3)));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();

        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, objectPct);
        // Contract justification: getPct is a query ("Returns the percentage..."), and getSumFreq/hashCode/toString are observable readers.
        // A throw-deleting or wrong-routing patch that mutates internal state during a read-only query would violate this post-condition.
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state] semantic mismatch: getPct(Object) changed read-only observable state sumBefore=" + sumBefore
                    + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashAfter
                    + " toStringBefore=" + escape(stringBefore)
                    + " toStringAfter=" + escape(stringAfter)
            );
        }

        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-integer-arg-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // Extra trusted metamorphic/oracle: the class contract says integer values are not distinguished by type.
        // This reproduces the fixture through the real API and checks getPct(Object Integer) against count/sum on the same Frequency.
        long countThree = f.getCount((Object) Integer.valueOf(3));
        long sum = f.getSumFreq();
        if (sum > 0) {
            double expectedFromCount = (double) countThree / (double) sum;
            double actualFromObject = f.getPct((Object) Integer.valueOf(3));
            if (!closeEnough(actualFromObject, expectedFromCount)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:count-over-sum-seed] semantic mismatch: getPct(Object Integer.valueOf(3))="
                        + actualFromObject + " expected=" + expectedFromCount + " count=" + countThree + " sum=" + sum
                );
            }
        }

        relationGetPctObjectMatchesCountOverSumForIntegerObject(data);
        relationGetPctObjectAndComparableAgreeOnSameIntegerValue(data);
    }

    private static void relationGetPctObjectMatchesCountOverSumForIntegerObject(FuzzedDataProvider data) {
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
        } catch (Exception e) {
            return;
        }

        // Contract justification: getPct(Object) returns the percentage equal to v; on any non-empty Frequency this equals getCount(v)/getSumFreq().
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPctObject_matches_count_over_sum_for_integer_object violated: actual="
                    + actual + " expected=" + expected + " count=" + count + " sum=" + sum + " probe=" + probe
            );
        }
    }

    private static void relationGetPctObjectAndComparableAgreeOnSameIntegerValue(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double a;
        double b;
        long sumBeforeObject;
        long sumAfterObject;
        int hashBeforeObject;
        int hashAfterObject;
        String stringBeforeObject;
        String stringAfterObject;

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

            sumBeforeObject = f.getSumFreq();
            hashBeforeObject = f.hashCode();
            stringBeforeObject = f.toString();
            a = f.getPct((Object) probe);
            sumAfterObject = f.getSumFreq();
            hashAfterObject = f.hashCode();
            stringAfterObject = f.toString();

            b = f.getPct((Comparable<?>) probe);
        } catch (Exception e) {
            return;
        }

        // Contract justification: deprecated getPct(Object) and getPct(Comparable) are sibling overloads for the same logical query.
        if (!closeEnough(a, b)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a
                    + " comparable=" + b + " probe=" + probe
            );
        }

        // Hidden-state check on the fuzzed path as well: read-only query must not alter other public readers.
        if (sumBeforeObject != sumAfterObject
            || hashBeforeObject != hashAfterObject
            || !safeEquals(stringBeforeObject, stringAfterObject)) {
            throw new FuzzerSecurityIssueLow(
                "relation getPct_object_and_comparable_agree_on_same_integer_value violated: hidden-state mutation"
                    + " sumBefore=" + sumBeforeObject
                    + " sumAfter=" + sumAfterObject
                    + " hashBefore=" + hashBeforeObject
                    + " hashAfter=" + hashAfterObject
                    + " toStringBefore=" + escape(stringBeforeObject)
                    + " toStringAfter=" + escape(stringAfterObject)
            );
        }
    }

    private static void assertDoubleEquals(String oracleId, String label, double expected, double actual) {
        if (!closeEnough(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static boolean closeEnough(double a, double b) {
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