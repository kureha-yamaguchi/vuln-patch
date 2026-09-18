package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String relationViolation = null;

        try {
            Frequency g = new Frequency();
            int base = data.consumeInt(-1000, 1000);
            int x = base;
            int y = base + 1;
            int z = base + 2;
            int cx = data.consumeInt(1, 4);
            int cy = data.consumeInt(1, 4);
            int cz = data.consumeInt(1, 4);

            for (int i = 0; i < cx; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(x);
                } else {
                    g.addValue((long) x);
                }
            }
            for (int i = 0; i < cy; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(y);
                } else {
                    g.addValue((long) y);
                }
            }
            for (int i = 0; i < cz; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(y);
                } else {
                    g.addValue((long) y);
                }
            }
            for (int i = 0; i < cz; i++) {
                if (data.consumeBoolean()) {
                    g.addValue(z);
                } else {
                    g.addValue((long) z);
                }
            }

            long sum = g.getSumFreq();
            if (sum > 0) {
                java.util.Iterator<?> it = g.valuesIterator();
                double previousCumPct = 0.0;
                while (it.hasNext()) {
                    Comparable<?> key = (Comparable<?>) it.next();
                    double currentCumPct = g.getCumPct(key);
                    double objectPct = g.getPct((Object) key);
                    double expectedStep = currentCumPct - previousCumPct;
                    double tol = 1e-12 * Math.max(1.0,
                            Math.max(Math.abs(expectedStep), Math.abs(objectPct)));
                    if (Math.abs(expectedStep - objectPct) > tol) {
                        relationViolation =
                                "[oracle:iterator-cumpct-step-object-pct] consistency violation: "
                                        + "for key=" + key
                                        + " expectedStep=" + expectedStep
                                        + " objectPct=" + objectPct
                                        + " previousCumPct=" + previousCumPct
                                        + " currentCumPct=" + currentCumPct
                                        + " sum=" + sum;
                        break;
                    }
                    previousCumPct = currentCumPct;
                }
            }
        } catch (Throwable t) {
            return;
        }

        if (relationViolation != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(relationViolation);
        }

        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;
        double tolerance = 10E-15;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        // getPct/getCumPct are documented accessors; these checks replicate the trusted test oracle exactly.
        double actualOnePct = f.getPct(1);
        if (Math.abs(0.25 - actualOnePct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-one-pct-exact] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(0.25 - actualTwoPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-two-pct-exact] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (Math.abs(0.5 - actualThreePct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-three-pct-exact] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(0.5 - actualThreeObjectPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-three-object-pct-groundtruth] semantic mismatch: expected=0.5 actual="
                            + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(0.0 - actualFivePct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-five-pct-exact] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(0.0 - actualFooPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-foo-pct-exact] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(0.25 - actualOneCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-one-cumpct-exact] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(0.50 - actualTwoCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-two-cumpct-exact] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(0.50 - actualIntegerArgCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-integer-argument-cumpct-exact] semantic mismatch: expected=0.5 actual="
                            + actualIntegerArgCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (Math.abs(1.0 - actualThreeCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-three-cumpct-exact] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(1.0 - actualFiveCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-five-cumpct-exact] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(0.0 - actualZeroCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-zero-cumpct-exact] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(0.0 - actualFooCumPct) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-foo-cumpct-exact] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        // Post-condition/metamorphic check:
        // For any correct implementation, the cumulative percentage over the ordered distinct values
        // increases in per-bucket steps, and each step equals the local percentage for that value.
        // A band-aid patch that merely hides the known seed symptom but leaves getPct(Object) returning
        // cumulative mass would still violate this relation on later buckets.
        String seedStepViolation = null;
        try {
            java.util.Iterator<?> it = f.valuesIterator();
            double previousCumPct = 0.0;
            while (it.hasNext()) {
                Comparable<?> key = (Comparable<?>) it.next();
                double currentCumPct = f.getCumPct(key);
                double objectPct = f.getPct((Object) key);
                double expectedStep = currentCumPct - previousCumPct;
                if (Math.abs(expectedStep - objectPct) > tolerance) {
                    seedStepViolation =
                            "[oracle:seed-step-width-vs-object-pct] metamorphic violation: key=" + key
                                    + " expectedStep=" + expectedStep
                                    + " objectPct=" + objectPct
                                    + " previousCumPct=" + previousCumPct
                                    + " currentCumPct=" + currentCumPct;
                    break;
                }
                previousCumPct = currentCumPct;
            }
        } catch (Throwable t) {
            return;
        }

        if (seedStepViolation != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(seedStepViolation);
        }
    }
}