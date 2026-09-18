package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedTestOracles();
        runExactPostConditionChecks();
        runRelationCountOverSum(data);
        runRelationObjectComparableAgreement(data);
        runExtraSiblingAgreement(data);
    }

    private static void runLiftedTestOracles() {
        final long oneL = 1;
        final long twoL = 2;
        final long threeL = 3;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

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
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:setup-fixture] unexpected exception during exact FrequencyTest fixture setup", e);
        }

        assertDoubleEquals("oracle-one-pct", "one pct", 0.25, safeGetPctInt(f, 1));
        assertDoubleEquals("oracle-two-pct", "two pct", 0.25, safeGetPctLongObject(f, Long.valueOf(2)));
        assertDoubleEquals("oracle-three-pct", "three pct", 0.5, safeGetPctLongObject(f, Long.valueOf(threeL)));
        assertDoubleEquals("oracle-three-object-pct", "three (Object) pct", 0.5, safeGetPctObject(f, (Object) Integer.valueOf(3)));
        assertDoubleEquals("oracle-five-pct", "five pct", 0.0, safeGetPctInt(f, 5));
        assertDoubleEquals("oracle-foo-pct", "foo pct", 0.0, safeGetPctObject(f, "foo"));
        assertDoubleEquals("oracle-one-cum-pct", "one cum pct", 0.25, safeGetCumPctInt(f, 1));
        assertDoubleEquals("oracle-two-cum-pct", "two cum pct", 0.50, safeGetCumPctLongObject(f, Long.valueOf(2)));
        assertDoubleEquals("oracle-integer-argument", "Integer argument", 0.50, safeGetCumPctObject(f, Integer.valueOf(2)));
        assertDoubleEquals("oracle-three-cum-pct", "three cum pct", 1.0, safeGetCumPctLongObject(f, Long.valueOf(threeL)));
        assertDoubleEquals("oracle-five-cum-pct", "five cum pct", 1.0, safeGetCumPctInt(f, 5));
        assertDoubleEquals("oracle-zero-cum-pct", "zero cum pct", 0.0, safeGetCumPctInt(f, 0));
        assertDoubleEquals("oracle-foo-cum-pct", "foo cum pct", 0.0, safeGetCumPctObject(f, "foo"));
    }

    private static void runExactPostConditionChecks() {
        final long oneL = 1;
        final long twoL = 2;
        final long threeL = 3;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

        Frequency f = new Frequency();
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();

        double pctObject;
        double pctComparable;
        long sumMid;
        int hashMid;
        String stringMid;
        try {
            pctObject = f.getPct((Object) Integer.valueOf(3));
            sumMid = f.getSumFreq();
            hashMid = f.hashCode();
            stringMid = f.toString();
            pctComparable = f.getPct((Comparable<?>) Integer.valueOf(3));
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:readonly-post] unexpected exception during read-only post-condition check", e);
        }

        /* Contract justification: getPct/getCumPct/getCount/getSumFreq are query methods ("Returns ...");
           they are read-only. A patch that merely redirects or deletes bookkeeping can silently mutate hidden
           state; therefore cheap observable readers must remain unchanged across the call. */
        if (sumBefore != sumMid || hashBefore != hashMid || !eq(stringBefore, stringMid)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:readonly-post] semantic mismatch: read-only getPct(Object) changed observable state"
                    + " sumBefore=" + sumBefore
                    + " sumAfter=" + sumMid
                    + " hashBefore=" + hashBefore
                    + " hashAfter=" + hashMid
                    + " toStringBefore=" + escapeOneLine(stringBefore)
                    + " toStringAfter=" + escapeOneLine(stringMid));
        }

        /* Contract justification: deprecated getPct(Object) is the sibling of getPct(Comparable), and the
           class/test contract states integer values are not distinguished by type for get* queries. Therefore
           the same Integer logical input through both overloads must agree. */
        if (!approxEqual(pctObject, pctComparable, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exact-sibling] semantic mismatch: getPct(Object) and getPct(Comparable) disagree on Integer.valueOf(3)"
                    + " object=" + pctObject + " comparable=" + pctComparable);
        }
    }

    private static void runRelationCountOverSum(FuzzedDataProvider data) {
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
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (RuntimeException e) {
            return;
        }

        /* Contract justification: getPct(Object) "Returns the percentage of values that are equal to v".
           Together with getCount and getSumFreq, on any non-empty Frequency this equals count(v)/sumFreq.
           This directly checks the patched Object overload on valid integral inputs. */
        if (!approxEqual(actual, expected, 1e-9)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: "
                    + "probe=" + probe + " actual=" + actual + " expected=" + expected
                    + " count=" + count + " sum=" + sum);
        }
    }

    private static void runRelationObjectComparableAgreement(FuzzedDataProvider data) {
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
        } catch (RuntimeException e) {
            return;
        }

        /* Contract justification: deprecated getPct(Object) is replaced by getPct(Comparable), and integer
           values are documented/tested as not distinguished by type for these queries; equivalent inputs must agree. */
        if (!approxEqual(objectPct, comparablePct, 1e-9)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:object-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: "
                    + "probe=" + probe + " object=" + objectPct + " comparable=" + comparablePct);
        }
    }

    private static void runExtraSiblingAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double pctInt;
        double pctObject;
        long sumBefore;
        long sumAfter;
        int hashBefore;
        int hashAfter;
        String strBefore;
        String strAfter;
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
            sumBefore = f.getSumFreq();
            hashBefore = f.hashCode();
            strBefore = f.toString();
            pctInt = f.getPct(probe);
            pctObject = f.getPct((Object) Integer.valueOf(probe));
            sumAfter = f.getSumFreq();
            hashAfter = f.hashCode();
            strAfter = f.toString();
        } catch (RuntimeException e) {
            return;
        }

        /* Contract justification: same logical integer value through same-name overloads getPct(int) and
           getPct(Object with Integer) must agree; this is a sibling-agreement check over the public API. */
        if (!approxEqual(pctInt, pctObject, 1e-9)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:int-object-sibling] metamorphic violation: getPct(int) and getPct(Object(Integer)) disagree"
                    + " probe=" + probe + " int=" + pctInt + " object=" + pctObject);
        }

        /* Hidden-state check on another read-only query path. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || !eq(strBefore, strAfter)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:hidden-state] metamorphic violation: read-only getPct queries changed state"
                    + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " toStringBefore=" + escapeOneLine(strBefore)
                    + " toStringAfter=" + escapeOneLine(strAfter));
        }
    }

    private static double safeGetPctInt(Frequency f, int v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getPct(int)", e);
        }
    }

    private static double safeGetPctLongObject(Frequency f, Long v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getPct(Long)", e);
        }
    }

    private static double safeGetPctObject(Frequency f, Object v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getPct(Object)", e);
        }
    }

    private static double safeGetCumPctInt(Frequency f, int v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getCumPct(int)", e);
        }
    }

    private static double safeGetCumPctLongObject(Frequency f, Long v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getCumPct(Long)", e);
        }
    }

    private static double safeGetCumPctObject(Frequency f, Object v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-call] unexpected exception from getCumPct(Object)", e);
        }
    }

    private static void assertDoubleEquals(String oracleId, String what, double expected, double actual) {
        if (!approxEqual(actual, expected, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEqual(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}