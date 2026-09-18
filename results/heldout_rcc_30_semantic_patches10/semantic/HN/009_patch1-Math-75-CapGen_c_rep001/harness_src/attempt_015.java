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

        assertApprox("pcts-one", 0.25, f.getPct(1), "f.getPct(1)");
        assertApprox("pcts-two", 0.25, f.getPct(Long.valueOf(2)), "f.getPct(Long.valueOf(2))");
        assertApprox("pcts-three", 0.5, f.getPct(Long.valueOf(threeL)), "f.getPct(threeL)");
        assertApprox("pcts-three-object", 0.5, f.getPct((Object) Integer.valueOf(3)), "f.getPct((Object) Integer.valueOf(3))");
        assertApprox("pcts-five", 0.0, f.getPct(5), "f.getPct(5)");
        assertApprox("pcts-foo", 0.0, f.getPct("foo"), "f.getPct(\"foo\")");
        assertApprox("cumpct-one", 0.25, f.getCumPct(1), "f.getCumPct(1)");
        assertApprox("cumpct-two", 0.50, f.getCumPct(Long.valueOf(2)), "f.getCumPct(Long.valueOf(2))");
        assertApprox("cumpct-two-integer", 0.50, f.getCumPct(Integer.valueOf(2)), "f.getCumPct(Integer.valueOf(2))");
        assertApprox("cumpct-three", 1.0, f.getCumPct(Long.valueOf(threeL)), "f.getCumPct(threeL)");
        assertApprox("cumpct-five", 1.0, f.getCumPct(5), "f.getCumPct(5)");
        assertApprox("cumpct-zero", 0.0, f.getCumPct(0), "f.getCumPct(0)");
        assertApprox("cumpct-foo", 0.0, f.getCumPct("foo"), "f.getCumPct(\"foo\")");

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String strBefore = f.toString();
        double objectPct = f.getPct((Object) Integer.valueOf(3));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String strAfter = f.toString();

        if (sumBefore != sumAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-sum] semantic mismatch: getPct(Object) changed getSumFreq before=" + sumBefore + " after=" + sumAfter);
        }
        if (hashBefore != hashAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-hash] semantic mismatch: getPct(Object) changed hashCode before=" + hashBefore + " after=" + hashAfter);
        }
        if ((strBefore == null && strAfter != null) || (strBefore != null && !strBefore.equals(strAfter))) {
            throw new FuzzerSecurityIssueLow("[oracle:readonly-string] semantic mismatch: getPct(Object) changed toString before=" + String.valueOf(strBefore) + " after=" + String.valueOf(strAfter));
        }

        // Documented guarantee: getPct returns "the proportion of values that are equal to v".
        // For non-empty frequency tables this observable post-condition is getCount(v) / getSumFreq().
        double expectedFromCounts = (double) f.getCount(Integer.valueOf(3)) / (double) f.getSumFreq();
        if (!approxEquals(objectPct, expectedFromCounts)) {
            throw new RuntimeException("[oracle:pct-count-ratio] metamorphic violation: getPct(Object 3) must equal getCount(3)/getSumFreq() input=3 lhs=" + objectPct + " rhs=" + expectedFromCounts);
        }

        // Documented guarantee: deprecated getPct(Object) is replaced by getPct(Comparable),
        // so equivalent Integer inputs must produce the same result through both real overloads.
        double comparablePct = f.getPct((Comparable<?>) Integer.valueOf(3));
        if (!approxEquals(objectPct, comparablePct)) {
            throw new RuntimeException("[oracle:object-vs-comparable] metamorphic violation: equivalent getPct overloads disagree input=3 lhs=" + objectPct + " rhs=" + comparablePct);
        }

        try {
            int probe = data.consumeInt(-1000, 1000);
            int lowerCount = data.consumeInt(1, 5);
            int equalCount = data.consumeInt(1, 5);
            int higherCount = data.consumeInt(1, 5);

            Frequency g = new Frequency();

            for (int i = 0; i < lowerCount; i++) {
                g.addValue((long) (probe - 1));
            }
            for (int i = 0; i < equalCount; i++) {
                if ((i & 1) == 0) {
                    g.addValue(probe);
                } else {
                    g.addValue(Integer.valueOf(probe));
                }
            }
            for (int i = 0; i < higherCount; i++) {
                g.addValue((Object) Integer.valueOf(probe + 1));
            }

            double lhs = g.getPct((Object) Integer.valueOf(probe));
            double rhs = g.getPct((Comparable<?>) Integer.valueOf(probe));
            if (!approxEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:fuzz-overload-agreement] metamorphic violation: equivalent getPct overloads disagree input=" + probe + " lhs=" + lhs + " rhs=" + rhs);
            }

            double ratio = (double) g.getCount(Integer.valueOf(probe)) / (double) g.getSumFreq();
            if (!approxEquals(lhs, ratio)) {
                throw new RuntimeException("[oracle:fuzz-pct-ratio] metamorphic violation: getPct(Object) must equal getCount/getSumFreq input=" + probe + " lhs=" + lhs + " rhs=" + ratio);
            }

            long gSumBefore = g.getSumFreq();
            int gHashBefore = g.hashCode();
            String gStrBefore = g.toString();
            g.getPct((Object) Integer.valueOf(probe));
            long gSumAfter = g.getSumFreq();
            int gHashAfter = g.hashCode();
            String gStrAfter = g.toString();
            if (gSumBefore != gSumAfter || gHashBefore != gHashAfter || (gStrBefore == null ? gStrAfter != null : !gStrBefore.equals(gStrAfter))) {
                throw new RuntimeException("[oracle:fuzz-readonly] metamorphic violation: getPct(Object) must be read-only input=" + probe + " sumBefore=" + gSumBefore + " sumAfter=" + gSumAfter + " hashBefore=" + gHashBefore + " hashAfter=" + gHashAfter + " strBefore=" + String.valueOf(gStrBefore) + " strAfter=" + String.valueOf(gStrAfter));
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void assertApprox(String oracleId, double expected, double actual, String what) {
        if (!approxEquals(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
        }
        return Math.abs(a - b) <= TOLERANCE;
    }
}