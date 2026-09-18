package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

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

        double onePct = f.getPct(1);
        double twoPct = f.getPct(Long.valueOf(2));
        double threePct = f.getPct(threeL);
        double threeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        double fivePct = f.getPct(5);
        double fooPct = f.getPct("foo");
        double oneCumPct = f.getCumPct(1);
        double twoCumPct = f.getCumPct(Long.valueOf(2));
        double integerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        double threeCumPct = f.getCumPct(threeL);
        double fiveCumPct = f.getCumPct(5);
        double zeroCumPct = f.getCumPct(0);
        double fooCumPct = f.getCumPct("foo");

        double seedObjectTwoPct = f.getPct((Object) Integer.valueOf(2));
        double seedObjectTwoCumPct = f.getCumPct((Object) Integer.valueOf(2));

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String strBefore = f.toString();
        f.getPct((Object) Integer.valueOf(2));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String strAfter = f.toString();

        if (sumBefore != sumAfter || hashBefore != hashAfter || !strBefore.equals(strAfter)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-object-pct-hidden-state-axis] semantic mismatch: getPct(Object) is a getter and must be read-only, but state changed sumBefore="
                    + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " strBefore=" + strBefore.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
                    + " strAfter=" + strAfter.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t"));
        }

        // Contract used: getPct returns the percentage of values equal to v; getCumPct returns the cumulative percentage.
        // In this seed fixture, 1, 2, and 3 are all present, so for the middle value 2 the exact-bucket percentage
        // must be strictly smaller than the cumulative percentage because values < 2 contribute to getCumPct but not getPct.
        // A patch that routes getPct(Object) to getCumPct(Object) makes them equal here.
        if (!(seedObjectTwoPct < seedObjectTwoCumPct)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-middle-object-pct-strict-less-than-cumpct] metamorphic violation: expected getPct((Object)2) < getCumPct((Object)2) in the seeded distribution but got pct="
                    + seedObjectTwoPct + " cumPct=" + seedObjectTwoCumPct + " count2=" + f.getCount(2) + " sum=" + f.getSumFreq());
        }

        if (Math.abs(0.25 - onePct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: one pct expected=0.25 actual=" + onePct);
        }
        if (Math.abs(0.25 - twoPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: two pct expected=0.25 actual=" + twoPct);
        }
        if (Math.abs(0.5 - threePct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: three pct expected=0.5 actual=" + threePct);
        }
        if (Math.abs(0.5 - threeObjectPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: three (Object) pct expected=0.5 actual=" + threeObjectPct);
        }
        if (Math.abs(0.0 - fivePct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: five pct expected=0.0 actual=" + fivePct);
        }
        if (Math.abs(0.0 - fooPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: foo pct expected=0.0 actual=" + fooPct);
        }
        if (Math.abs(0.25 - oneCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: one cum pct expected=0.25 actual=" + oneCumPct);
        }
        if (Math.abs(0.50 - twoCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: two cum pct expected=0.50 actual=" + twoCumPct);
        }
        if (Math.abs(0.50 - integerArgumentCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: Integer argument expected=0.50 actual=" + integerArgumentCumPct);
        }
        if (Math.abs(1.0 - threeCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: three cum pct expected=1.0 actual=" + threeCumPct);
        }
        if (Math.abs(1.0 - fiveCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: five cum pct expected=1.0 actual=" + fiveCumPct);
        }
        if (Math.abs(0.0 - zeroCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: zero cum pct expected=0.0 actual=" + zeroCumPct);
        }
        if (Math.abs(0.0 - fooCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-testpcts-bundle-all] semantic mismatch: foo cum pct expected=0.0 actual=" + fooCumPct);
        }

        Frequency g;
        int a;
        int b;
        int c;
        int countA;
        int countB;
        int countC;
        double pctMid;
        double cumPctMid;
        try {
            g = new Frequency();
            a = data.consumeInt(-1000000, 999980);
            b = a + data.consumeInt(1, 10);
            c = b + data.consumeInt(1, 10);
            countA = data.consumeInt(1, 4);
            countB = data.consumeInt(1, 4);
            countC = data.consumeInt(1, 4);

            for (int i = 0; i < countA; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(a);
                } else {
                    g.addValue((long) a);
                }
            }
            for (int i = 0; i < countB; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(b);
                } else {
                    g.addValue((long) b);
                }
            }
            for (int i = 0; i < countC; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(c);
                } else {
                    g.addValue((long) c);
                }
            }

            pctMid = g.getPct((Object) Integer.valueOf(b));
            cumPctMid = g.getCumPct((Object) Integer.valueOf(b));
        } catch (Exception e) {
            return;
        }

        // Contract used: with three distinct present integral values a < b < c, getPct((Object)b) is the proportion equal to b,
        // while getCumPct((Object)b) includes both a and b. Since countA >= 1 by construction, cumPct(b) must be strictly larger.
        // This specifically flips the patched condition on an Object-typed integral query just past the seed and catches overfitted fixes.
        if (!(pctMid < cumPctMid)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fuzzed-middle-object-pct-strict-less-than-cumpct] metamorphic violation: a=" + a + " b=" + b + " c=" + c
                    + " countA=" + countA + " countB=" + countB + " countC=" + countC
                    + " pctMid=" + pctMid + " cumPctMid=" + cumPctMid + " sum=" + g.getSumFreq());
        }
    }
}