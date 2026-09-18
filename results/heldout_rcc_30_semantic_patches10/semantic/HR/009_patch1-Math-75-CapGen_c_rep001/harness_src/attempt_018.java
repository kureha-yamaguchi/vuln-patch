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

        checkApprox("seed-one-pct-copy2", 0.25, f.getPct(1), "f.getPct(1)");
        checkApprox("seed-two-pct-copy2", 0.25, f.getPct(Long.valueOf(2)), "f.getPct(Long.valueOf(2))");
        checkApprox("seed-three-pct-copy2", 0.5, f.getPct(Long.valueOf(3)), "f.getPct(threeL)");
        checkApprox("seed-three-object-pct-copy2", 0.5, f.getPct((Object) (Integer.valueOf(3))), "f.getPct((Object) Integer.valueOf(3))");
        checkApprox("seed-five-pct-copy2", 0.0, f.getPct(5), "f.getPct(5)");
        checkApprox("seed-foo-pct-copy2", 0.0, f.getPct("foo"), "f.getPct(\"foo\")");
        checkApprox("seed-one-cumpct-copy2", 0.25, f.getCumPct(1), "f.getCumPct(1)");
        checkApprox("seed-two-cumpct-copy2", 0.50, f.getCumPct(Long.valueOf(2)), "f.getCumPct(Long.valueOf(2))");
        checkApprox("seed-integer-argument-cumpct-copy2", 0.50, f.getCumPct(Integer.valueOf(2)), "f.getCumPct(Integer.valueOf(2))");
        checkApprox("seed-three-cumpct-copy2", 1.0, f.getCumPct(threeL), "f.getCumPct(threeL)");
        checkApprox("seed-five-cumpct-copy2", 1.0, f.getCumPct(5), "f.getCumPct(5)");
        checkApprox("seed-zero-cumpct-copy2", 0.0, f.getCumPct(0), "f.getCumPct(0)");
        checkApprox("seed-foo-cumpct-copy2", 0.0, f.getCumPct("foo"), "f.getCumPct(\"foo\")");

        Frequency evolving = new Frequency();
        int mutations = data.consumeInt(1, 12);
        int currentMax = Integer.MIN_VALUE;
        boolean hasValue = false;

        for (int i = 0; i < mutations; i++) {
            int v = data.consumeInt(-1000, 1000);
            switch (data.consumeInt(0, 3)) {
                case 0:
                    evolving.addValue(v);
                    break;
                case 1:
                    evolving.addValue((long) v);
                    break;
                case 2:
                    evolving.addValue(Integer.valueOf(v));
                    break;
                default:
                    evolving.addValue((Object) Integer.valueOf(v));
                    break;
            }

            if (!hasValue || v > currentMax) {
                currentMax = v;
                hasValue = true;
            }

            if (!hasValue || currentMax >= 1000000) {
                continue;
            }

            int absentAboveMax = currentMax + 1;

            try {
                double objectPct = evolving.getPct((Object) Integer.valueOf(absentAboveMax));
                double cumPct = evolving.getCumPct(absentAboveMax);
                long count = evolving.getCount(absentAboveMax);
                long sum = evolving.getSumFreq();

                if (count != 0L) {
                    throw new FuzzerSecurityIssueLow("[oracle:mutated-absent-above-max-count] semantic mismatch: expected count 0 for absentAboveMax=" + absentAboveMax + " but was " + count + " sum=" + sum);
                }

                if (sum > 0L) {
                    if (!approxEq(cumPct, 1.0)) {
                        throw new FuzzerSecurityIssueLow("[oracle:mutated-absent-above-max-cumpct] semantic mismatch: constructed absent query above every inserted value must have cumulative percentage 1.0, absentAboveMax=" + absentAboveMax + " cumPct=" + cumPct + " sum=" + sum);
                    }
                    /* Contract used: getPct returns the percentage of values equal to v.
                       We construct absentAboveMax to be strictly greater than every inserted numeric value after EACH mutation,
                       so no inserted value can equal it and the percentage must stay exactly 0 in every mutated state.
                       A band-aid patch that merely hides the original symptom but still routes object queries through cumulative logic
                       will violate this post-condition without throwing. */
                    if (!approxEq(objectPct, 0.0)) {
                        throw new FuzzerSecurityIssueLow("[oracle:mutated-absent-above-max-object-pct] semantic mismatch: expected object percentage 0.0 for absentAboveMax=" + absentAboveMax + " but was " + objectPct + " cumPct=" + cumPct + " sum=" + sum);
                    }
                }
            } catch (RuntimeException ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
                return;
            }
        }
    }

    private static void checkApprox(String id, double expected, double actual, String expr) {
        if (!approxEq(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEq(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= TOLERANCE;
    }
}