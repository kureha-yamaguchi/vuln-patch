package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        double v1 = f.getPct(1);
        if (!sameDouble(v1, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-one-pct-bundle] semantic mismatch: expected=0.25 actual=" + v1);
        }

        double v2 = f.getPct(Long.valueOf(2));
        if (!sameDouble(v2, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-two-pct-bundle] semantic mismatch: expected=0.25 actual=" + v2);
        }

        double v3 = f.getPct(threeL);
        if (!sameDouble(v3, 0.5, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-three-pct-bundle] semantic mismatch: expected=0.5 actual=" + v3);
        }

        double v4 = f.getPct((Object) (Integer.valueOf(3)));
        if (!sameDouble(v4, 0.5, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-three-object-pct-bundle] semantic mismatch: expected=0.5 actual=" + v4);
        }

        double v5 = f.getPct(5);
        if (!sameDouble(v5, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-five-pct-bundle] semantic mismatch: expected=0.0 actual=" + v5);
        }

        double v6 = f.getPct("foo");
        if (!sameDouble(v6, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-foo-pct-bundle] semantic mismatch: expected=0.0 actual=" + v6);
        }

        double v7 = f.getCumPct(1);
        if (!sameDouble(v7, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-one-cumpct-bundle] semantic mismatch: expected=0.25 actual=" + v7);
        }

        double v8 = f.getCumPct(Long.valueOf(2));
        if (!sameDouble(v8, 0.50, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-two-cumpct-bundle] semantic mismatch: expected=0.50 actual=" + v8);
        }

        double v9 = f.getCumPct(Integer.valueOf(2));
        if (!sameDouble(v9, 0.50, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-integer-argument-cumpct-bundle] semantic mismatch: expected=0.50 actual=" + v9);
        }

        double v10 = f.getCumPct(threeL);
        if (!sameDouble(v10, 1.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-three-cumpct-bundle] semantic mismatch: expected=1.0 actual=" + v10);
        }

        double v11 = f.getCumPct(5);
        if (!sameDouble(v11, 1.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-five-cumpct-bundle] semantic mismatch: expected=1.0 actual=" + v11);
        }

        double v12 = f.getCumPct(0);
        if (!sameDouble(v12, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-zero-cumpct-bundle] semantic mismatch: expected=0.0 actual=" + v12);
        }

        double v13 = f.getCumPct("foo");
        if (!sameDouble(v13, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-foo-cumpct-bundle] semantic mismatch: expected=0.0 actual=" + v13);
        }

        Frequency g;
        int low;
        int high;
        int lowCount;
        int highCount;
        double actualMaxPct;
        double expectedMaxPct;
        try {
            g = new Frequency();
            low = data.consumeInt(-1000, 999);
            high = low + data.consumeInt(1, 20);
            lowCount = data.consumeInt(1, 8);
            highCount = data.consumeInt(1, 8);

            for (int i = 0; i < lowCount; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(low);
                } else {
                    g.addValue((long) low);
                }
            }
            for (int i = 0; i < highCount; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(high);
                } else {
                    g.addValue((long) high);
                }
            }

            actualMaxPct = g.getPct((Object) Integer.valueOf(high));
            expectedMaxPct = ((double) highCount) / ((double) (lowCount + highCount));
        } catch (Exception e) {
            return;
        }
        if (!sameDouble(actualMaxPct, expectedMaxPct, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:constructed-two-bucket-max-object-pct] semantic mismatch: low=" + low + " high=" + high + " lowCount=" + lowCount + " highCount=" + highCount + " expected=" + expectedMaxPct + " actual=" + actualMaxPct);
        }

        Frequency h;
        int onlyValue;
        int repetitions;
        long sumBefore;
        int hashBefore;
        String strBefore;
        double onlyPct;
        long onlyCumFreq;
        long sumAfter;
        int hashAfter;
        String strAfter;
        try {
            h = new Frequency();
            onlyValue = data.consumeInt(-1000, 1000);
            repetitions = data.consumeInt(1, 8);
            for (int i = 0; i < repetitions; i++) {
                if (data.consumeBoolean()) {
                    h.addValue(onlyValue);
                } else {
                    h.addValue((long) onlyValue);
                }
            }

            sumBefore = h.getSumFreq();
            hashBefore = h.hashCode();
            strBefore = h.toString();

            onlyPct = h.getPct((Object) Integer.valueOf(onlyValue));
            onlyCumFreq = h.getCumFreq(Integer.valueOf(onlyValue));

            sumAfter = h.getSumFreq();
            hashAfter = h.hashCode();
            strAfter = h.toString();
        } catch (Exception e) {
            return;
        }

        if (!sameDouble(onlyPct, 1.0, TOLERANCE) || onlyCumFreq != sumBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:single-bucket-object-pct-and-cumfreq] metamorphic violation: value=" + onlyValue + " repetitions=" + repetitions + " expectedPct=1.0 actualPct=" + onlyPct + " expectedCumFreq=" + sumBefore + " actualCumFreq=" + onlyCumFreq);
        }

        /* Contract justification: getPct(Object) is a getter ("Returns the percentage of values that are equal to v")
           and therefore must not mutate observable receiver state. A band-aid patch that skips logic or mutates
           bookkeeping to hide the wrong percentage would violate this read-only post-condition. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || !strBefore.equals(strAfter)) {
            throw new FuzzerSecurityIssueLow("[oracle:single-bucket-getpct-readonly-state] metamorphic violation: sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " strChanged=" + (!strBefore.equals(strAfter)));
        }
    }

    private static boolean sameDouble(double actual, double expected, double tol) {
        return Math.abs(actual - expected) <= tol;
    }
}