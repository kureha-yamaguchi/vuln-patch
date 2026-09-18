package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();
        hiddenStateOracle();

        if (data.remainingBytes() <= 0) {
            return;
        }

        relationCountOverSum(data);
        relationObjectComparableAgree(data);
        relationObjectIntAgreeAndReadOnly(data);
    }

    private static void liftedTestOracles() {
        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        assertApprox("lifted-reprobe-foo-after-add-1", 0.0d, f.getPct("foo"));
        f.addValue(twoL);
        assertApprox("lifted-reprobe-foo-after-add-2", 0.0d, f.getPct("foo"));
        f.addValue(oneI);
        assertApprox("lifted-reprobe-foo-after-add-3", 0.0d, f.getPct("foo"));
        f.addValue(twoI);
        assertApprox("lifted-reprobe-foo-after-add-4", 0.0d, f.getPct("foo"));
        f.addValue(threeL);
        assertApprox("lifted-reprobe-foo-after-add-5", 0.0d, f.getPct("foo"));
        f.addValue(threeL);
        assertApprox("lifted-reprobe-foo-after-add-6", 0.0d, f.getPct("foo"));
        f.addValue(3);
        assertApprox("lifted-reprobe-foo-after-add-7", 0.0d, f.getPct("foo"));
        f.addValue(threeI);
        assertApprox("lifted-reprobe-foo-after-add-8", 0.0d, f.getPct("foo"));

        assertApprox("one-pct", 0.25d, f.getPct(1));
        assertApprox("two-pct", 0.25d, f.getPct(Long.valueOf(2)));
        assertApprox("three-pct", 0.5d, f.getPct(threeL));
        assertApprox("three-object-pct", 0.5d, f.getPct((Object) Integer.valueOf(3)));
        assertApprox("five-pct", 0.0d, f.getPct(5));
        assertApprox("foo-pct", 0.0d, f.getPct("foo"));
        assertApprox("one-cum-pct", 0.25d, f.getCumPct(1));
        assertApprox("two-cum-pct", 0.50d, f.getCumPct(Long.valueOf(2)));
        assertApprox("integer-argument-cum-pct", 0.50d, f.getCumPct(Integer.valueOf(2)));
        assertApprox("three-cum-pct", 1.0d, f.getCumPct(threeL));
        assertApprox("five-cum-pct", 1.0d, f.getCumPct(5));
        assertApprox("zero-cum-pct", 0.0d, f.getCumPct(0));
        assertApprox("foo-cum-pct", 0.0d, f.getCumPct("foo"));
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

        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();

        double objectPct = f.getPct((Object) Integer.valueOf(3));

        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterString = f.toString();

        /* getPct/getCumPct are read-only queries by contract ("Returns the percentage..."),
           so cheap observable state readers must not change across the call. A patch that
           deletes bookkeeping or mutates hidden state while answering would violate this. */
        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state] semantic mismatch: getPct(Object) changed read-only state beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString) + " pct=" + objectPct);
        }

        assertApprox("hidden-state-pct-value", 0.5d, objectPct);
    }

    private static void relationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        long beforeSum;
        int beforeHash;
        String beforeString;
        long afterSum;
        int afterHash;
        String afterString;

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

            beforeSum = f.getSumFreq();
            beforeHash = f.hashCode();
            beforeString = f.toString();
            actual = f.getPct((Object) Integer.valueOf(probe));
            afterSum = f.getSumFreq();
            afterHash = f.hashCode();
            afterString = f.toString();
        } catch (Throwable t) {
            return;
        }

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-count-over-sum-hidden-state] semantic mismatch: getPct(Object) changed read-only state beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString));
        }

        /* Contract: getPct(Object) returns the proportion of values equal to v.
           On a non-empty Frequency, that is exactly getCount(v) / getSumFreq().
           This uses only real API results and a valid Integer object probe. */
        if (!approxEquals(actual, expected)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-count-over-sum] semantic mismatch: getPct(Object)!=getCount/getSumFreq probe=" + probe + " actual=" + actual + " expected=" + expected + " count=" + count + " sum=" + sum);
        }
    }

    private static void relationObjectComparableAgree(FuzzedDataProvider data) {
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

        /* Contract: deprecated getPct(Object) is the Object sibling of getPct(Comparable),
           and integer values are not distinguished by type in Frequency; therefore the same
           Integer logical value must produce the same percentage through both overloads. */
        if (!approxEquals(objectPct, comparablePct)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-object-comparable-agree] semantic mismatch: same Integer value through getPct(Object) and getPct(Comparable) disagreed probe=" + probe + " objectPct=" + objectPct + " comparablePct=" + comparablePct);
        }
    }

    private static void relationObjectIntAgreeAndReadOnly(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        double objectPct;
        double intPct;
        long beforeSum;
        int beforeHash;
        String beforeString;
        long afterSum;
        int afterHash;
        String afterString;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(Integer.valueOf(v));
                } else {
                    f.addValue(Long.valueOf(v));
                }
            }
            probe = data.consumeInt(-6, 6);

            beforeSum = f.getSumFreq();
            beforeHash = f.hashCode();
            beforeString = f.toString();
            objectPct = f.getPct((Object) Integer.valueOf(probe));
            afterSum = f.getSumFreq();
            afterHash = f.hashCode();
            afterString = f.toString();

            intPct = f.getPct(probe);
        } catch (Throwable t) {
            return;
        }

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-object-int-hidden-state] semantic mismatch: getPct(Object) changed read-only state beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString));
        }

        /* Sibling-agreement check: getPct(Object) and getPct(int) are documented overloads
           over the same logical integral input space; Frequency does not distinguish integer
           values by type, so the same numeric value must yield the same percentage. */
        if (!approxEquals(objectPct, intPct)) {
            throw new FuzzerSecurityIssueLow("[oracle:relation-object-int-agree] semantic mismatch: same numeric value through getPct(Object) and getPct(int) disagreed probe=" + probe + " objectPct=" + objectPct + " intPct=" + intPct);
        }
    }

    private static void assertApprox(String oracleId, double expected, double actual) {
        if (!approxEquals(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        double scale = Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= Math.max(TOLERANCE, 1e-12d * scale);
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