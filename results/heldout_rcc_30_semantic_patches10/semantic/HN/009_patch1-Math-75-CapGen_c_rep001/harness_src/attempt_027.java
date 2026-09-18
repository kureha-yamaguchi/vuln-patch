package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

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
        assertDoubleEquals("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        assertDoubleEquals("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        assertDoubleEquals("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        assertDoubleEquals("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        assertDoubleEquals("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        assertDoubleEquals("lifted-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        assertDoubleEquals("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL));
        assertDoubleEquals("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        assertDoubleEquals("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        assertDoubleEquals("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        Frequency g = new Frequency();
        int base = data.consumeInt(-1000000, 999999);
        int lower = base;
        int higher = base + 1;
        int lowerCopies = data.consumeInt(1, 5);
        int higherCopies = data.consumeInt(1, 5);

        for (int i = 0; i < lowerCopies; i++) {
            if (data.consumeBoolean()) {
                g.addValue((long) lower);
            } else {
                g.addValue(lower);
            }
        }
        for (int i = 0; i < higherCopies; i++) {
            if (data.consumeBoolean()) {
                g.addValue((long) higher);
            } else {
                g.addValue(higher);
            }
        }

        try {
            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            String strBefore = g.toString();

            double objectPct = g.getPct((Object) Integer.valueOf(higher));
            double comparablePct = g.getPct(Integer.valueOf(higher));

            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            String strAfter = g.toString();

            if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(strBefore, strAfter)) {
                throw new RuntimeException(
                    "[oracle:read-only-state] metamorphic violation: getPct(Object) / getPct(Comparable) are documented read-only accessors, but observable state changed input=" +
                    higher + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter +
                    " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " toStringBefore=" + escape(strBefore) + " toStringAfter=" + escape(strAfter));
            }

            if (!sameDouble(objectPct, comparablePct)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: getPct(Object) and getPct(Comparable) have the same documented meaning for a Comparable argument input=" +
                    higher + " lhs=" + objectPct + " rhs=" + comparablePct +
                    " lowerCopies=" + lowerCopies + " higherCopies=" + higherCopies);
            }

            double expectedPct = ((double) g.getCount(Integer.valueOf(higher))) / ((double) g.getSumFreq());
            if (!sameDouble(comparablePct, expectedPct)) {
                throw new RuntimeException(
                    "[oracle:pct-count-sum] metamorphic violation: getPct(v) must equal getCount(v)/getSumFreq() when sumFreq>0 input=" +
                    higher + " lhs=" + comparablePct + " rhs=" + expectedPct +
                    " count=" + g.getCount(Integer.valueOf(higher)) + " sum=" + g.getSumFreq());
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void assertDoubleEquals(String oracleId, String label, double expected, double actual) {
        if (Double.isNaN(expected) ? !Double.isNaN(actual) : Math.abs(expected - actual) > TOLERANCE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label +
                " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
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