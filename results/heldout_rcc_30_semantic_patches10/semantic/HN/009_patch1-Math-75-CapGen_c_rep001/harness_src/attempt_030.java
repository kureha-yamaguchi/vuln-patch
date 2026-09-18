package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracleFromTest();
        runCountOverSumRelation(data);
        runObjectComparableAgreementRelation(data);
        runSeedShapedGeneralization(data);
    }

    private static void runLiftedOracleFromTest() {
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        Frequency f = new Frequency();
        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertApprox("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        assertApprox("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        assertApprox("lifted-three-pct", "three pct", 0.5, f.getPct(threeL));
        assertApprox("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        assertApprox("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertApprox("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertApprox("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertApprox("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertApprox("lifted-integer-arg-cum-pct", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertApprox("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertApprox("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertApprox("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertApprox("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        // Contract-based hidden-state check: get* methods are read-only queries, so cheap public readers
        // (sumFreq/hashCode/toString) must not change across getPct/getCumPct calls. A "fix" that silently mutates
        // state instead of returning the correct value would violate this observable post-condition.
        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();

        f.getPct((Object) Integer.valueOf(3));
        f.getPct((Comparable<?>) Integer.valueOf(3));
        f.getCumPct(Integer.valueOf(2));

        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterString = f.toString();

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:hidden-state-lifted] semantic mismatch: read-only get* changed state beforeSum=" + beforeSum
                    + " afterSum=" + afterSum
                    + " beforeHash=" + beforeHash
                    + " afterHash=" + afterHash
                    + " beforeString=" + escape(beforeString)
                    + " afterString=" + escape(afterString));
        }
    }

    private static void runCountOverSumRelation(FuzzedDataProvider data) {
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

            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            count = f.getCount((Object) probe);
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) probe);

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            // Contract-based hidden-state check: on a non-empty distribution, getPct(v) returns the percentage of
            // values equal to v, i.e. getCount(v) / getSumFreq(). Also, query methods are non-mutating.
            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-relation1] semantic mismatch: getCount/getPct changed state beforeSum=" + beforeSum
                        + " afterSum=" + afterSum
                        + " beforeHash=" + beforeHash
                        + " afterHash=" + afterHash
                        + " beforeString=" + escape(beforeString)
                        + " afterString=" + escape(afterString));
            }
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (!approxEqual(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:count-over-sum] metamorphic violation: getPct((Object) Integer) != getCount((Object) Integer)/getSumFreq probe="
                    + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void runObjectComparableAgreementRelation(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        double objectPct;
        double comparablePct;
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

            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            objectPct = f.getPct((Object) probe);
            comparablePct = f.getPct((Comparable<?>) probe);

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            // Contract-based sibling agreement: deprecated getPct(Object) is replaced by getPct(Comparable),
            // so the same Integer logical input must produce the same percentage through both overloads.
            // Query methods are also read-only, so public readers must remain unchanged.
            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-relation2] semantic mismatch: getPct overloads changed state beforeSum=" + beforeSum
                        + " afterSum=" + afterSum
                        + " beforeHash=" + beforeHash
                        + " afterHash=" + afterHash
                        + " beforeString=" + escape(beforeString)
                        + " afterString=" + escape(afterString));
            }
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (!approxEqual(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:object-vs-comparable] metamorphic violation: getPct(Object) and getPct(Comparable) disagree for same Integer probe="
                    + probe + " objectPct=" + objectPct + " comparablePct=" + comparablePct);
        }
    }

    private static void runSeedShapedGeneralization(FuzzedDataProvider data) {
        Frequency f;
        Integer probe;
        double objectPct;
        double expected;
        try {
            int extraCount = data.consumeInt(0, 6);
            int[] extras = new int[extraCount];
            for (int i = 0; i < extraCount; i++) {
                extras[i] = data.consumeInt(-4, 4);
            }
            probe = Integer.valueOf(data.consumeInt(-4, 4));

            f = new Frequency();

            // Seed-shaped family: create distributions where the chosen probe appears exactly four times out of eight
            // baseline insertions, mirroring the failing test's "same logical integer across int/long/Integer".
            f.addValue((long) probe.intValue());
            f.addValue((long) probe.intValue());
            f.addValue(probe.intValue());
            f.addValue(Integer.valueOf(probe.intValue()));

            int other1 = probe.intValue() == 0 ? 1 : 0;
            int other2 = probe.intValue() == 1 ? 2 : 1;
            f.addValue((long) other1);
            f.addValue(other1);
            f.addValue((long) other2);
            f.addValue(other2);

            for (int i = 0; i < extraCount; i++) {
                int e = extras[i];
                if (data.consumeBoolean()) {
                    f.addValue(e);
                } else {
                    f.addValue((long) e);
                }
            }

            long count = f.getCount((Comparable<?>) probe);
            long sum = f.getSumFreq();
            expected = (double) count / (double) sum;

            objectPct = f.getPct((Object) probe);
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (!approxEqual(objectPct, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-family] metamorphic violation: object overload disagrees with seeded cross-type distribution probe="
                    + probe + " objectPct=" + objectPct + " expected=" + expected);
        }
    }

    private static void assertApprox(String oracleId, String label, double expected, double actual) {
        if (!approxEqual(actual, expected)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEqual(double a, double b) {
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