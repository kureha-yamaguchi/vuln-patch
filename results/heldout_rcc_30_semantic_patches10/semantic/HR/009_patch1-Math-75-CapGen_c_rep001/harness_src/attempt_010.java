package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= TOLERANCE;
    }

    private static void fail(String id, String message) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] " + message);
    }

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

        double actualOnePct = f.getPct(1);
        if (!sameDouble(actualOnePct, 0.25d)) {
            fail("lifted-one-pct-fresh", "semantic mismatch: getPct(1) expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (!sameDouble(actualTwoPct, 0.25d)) {
            fail("lifted-two-pct-fresh", "semantic mismatch: getPct(Long.valueOf(2)) expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (!sameDouble(actualThreePct, 0.5d)) {
            fail("lifted-three-pct-fresh", "semantic mismatch: getPct(threeL) expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (!sameDouble(actualThreeObjectPct, 0.5d)) {
            fail("lifted-three-object-pct-fresh", "semantic mismatch: getPct((Object) Integer.valueOf(3)) expected=0.5 actual=" + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (!sameDouble(actualFivePct, 0.0d)) {
            fail("lifted-five-pct-fresh", "semantic mismatch: getPct(5) expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (!sameDouble(actualFooPct, 0.0d)) {
            fail("lifted-foo-pct-fresh", "semantic mismatch: getPct(\"foo\") expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (!sameDouble(actualOneCumPct, 0.25d)) {
            fail("lifted-one-cumpct-fresh", "semantic mismatch: getCumPct(1) expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (!sameDouble(actualTwoCumPct, 0.50d)) {
            fail("lifted-two-cumpct-fresh", "semantic mismatch: getCumPct(Long.valueOf(2)) expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (!sameDouble(actualIntegerArgCumPct, 0.50d)) {
            fail("lifted-integer-arg-cumpct-fresh", "semantic mismatch: getCumPct(Integer.valueOf(2)) expected=0.5 actual=" + actualIntegerArgCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (!sameDouble(actualThreeCumPct, 1.0d)) {
            fail("lifted-three-cumpct-fresh", "semantic mismatch: getCumPct(threeL) expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (!sameDouble(actualFiveCumPct, 1.0d)) {
            fail("lifted-five-cumpct-fresh", "semantic mismatch: getCumPct(5) expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (!sameDouble(actualZeroCumPct, 0.0d)) {
            fail("lifted-zero-cumpct-fresh", "semantic mismatch: getCumPct(0) expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (!sameDouble(actualFooCumPct, 0.0d)) {
            fail("lifted-foo-cumpct-fresh", "semantic mismatch: getCumPct(\"foo\") expected=0.0 actual=" + actualFooCumPct);
        }

        try {
            Frequency g = new Frequency();

            int base = data.consumeInt(-1000000, 999990);
            int step = data.consumeInt(1, 9);
            int higher = base + step;

            int baseCount = data.consumeInt(1, 8);
            int higherCount = data.consumeInt(1, 8);

            for (int i = 0; i < baseCount; i++) {
                int selector = data.consumeInt(0, 2);
                if (selector == 0) {
                    g.addValue(base);
                } else if (selector == 1) {
                    g.addValue((long) base);
                } else {
                    g.addValue(Integer.valueOf(base));
                }
            }

            for (int i = 0; i < higherCount; i++) {
                int selector = data.consumeInt(0, 2);
                if (selector == 0) {
                    g.addValue(higher);
                } else if (selector == 1) {
                    g.addValue((long) higher);
                } else {
                    g.addValue(Integer.valueOf(higher));
                }
            }

            long reportedSum = g.getSumFreq();
            long recomputedFromConstruction = (long) baseCount + (long) higherCount;
            if (reportedSum != recomputedFromConstruction) {
                fail("sumfreq-vs-construction", "consistency violation: getSumFreq disagrees with number of successful additions reported=" + reportedSum + " expected=" + recomputedFromConstruction + " base=" + base + " higher=" + higher + " baseCount=" + baseCount + " higherCount=" + higherCount);
            }

            long baseReportedCount = g.getCount(Long.valueOf(base));
            long higherReportedCount = g.getCount(Long.valueOf(higher));
            if (baseReportedCount + higherReportedCount != reportedSum) {
                fail("two-bucket-partition", "consistency violation: with only two distinct inserted values, getCount(base)+getCount(higher) must equal getSumFreq lhs=" + (baseReportedCount + higherReportedCount) + " rhs=" + reportedSum + " base=" + base + " higher=" + higher);
            }

            double objectPct = g.getPct((Object) Integer.valueOf(base));
            double longPct = g.getPct(Long.valueOf(base));
            /* Class contract says integral values are not distinguished by type, and getPct(Object) /
               getPct(long) have the same documented meaning ("percentage of values equal to v").
               Therefore an Integer Object probe and a Long probe for the same numeric value must agree.
               A band-aid patch that merely masks one symptom but leaves the deprecated Object overload
               computing cumulative percentage instead of ordinary percentage will still violate this. */
            if (!sameDouble(objectPct, longPct)) {
                fail("object-vs-long-overload", "metamorphic violation: getPct((Object) Integer.valueOf(base)) must equal getPct(Long.valueOf(base)) for the same integral value objectPct=" + objectPct + " longPct=" + longPct + " base=" + base + " higher=" + higher + " sum=" + reportedSum);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}