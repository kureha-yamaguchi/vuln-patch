package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;

        final WeightedObservedPoint[] seedPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            seedPoints[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        final HarmonicFitter.ParameterGuesser seedGuesser =
                new HarmonicFitter.ParameterGuesser(seedPoints);

        try {
            seedGuesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for the exact Math844 triangular sample, but guess() returned normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // This is the exact observable pinned by the failing test.
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        final WeightedObservedPoint[] externalArray = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            externalArray[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        final HarmonicFitter.ParameterGuesser clonedGuesser =
                new HarmonicFitter.ParameterGuesser(externalArray);

        final int mutations = data.consumeInt(1, len);
        for (int m = 0; m < mutations; m++) {
            final int idx = data.consumeInt(0, len - 1);
            final double newWeight = data.consumeInt(-1000, 1000);
            final double newX = data.consumeInt(-1000, 1000);
            final double newY = data.consumeInt(-1000, 1000);
            externalArray[idx] = new WeightedObservedPoint(newWeight, newX, newY);

            try {
                /*
                 * Documented constructor body shows: this.observations = observations.clone();
                 * Therefore later caller-side mutations of the original array must not affect
                 * the already-constructed guesser. This is a post-condition observable through
                 * guess(): the cloned guesser must keep rejecting the original Math844 sample
                 * after every external mutation.
                 */
                clonedGuesser.guess();
                throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser must be isolated from later caller-array mutations, but guess() returned normally after mutationIndex="
                                + m + " targetSlot=" + idx);
            } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
                // Expected: constructor cloned the original contents.
            } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
                return;
            }
        }

        int scale = data.consumeInt(-8, 8);
        if (scale == 0) {
            scale = 2;
        }

        final WeightedObservedPoint[] scaledPoints = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            scaledPoints[i] = new WeightedObservedPoint(1.0, i, y[i] * scale);
        }
        final HarmonicFitter.ParameterGuesser scaledGuesser =
                new HarmonicFitter.ParameterGuesser(scaledPoints);

        try {
            /*
             * Metamorphic relation from the shown guessAOmega() body: with x unchanged and all
             * y multiplied by one non-zero constant k, both integrated quantities scale by k^2.
             * Hence c1 scales by k^6, c2 by k^4, c3 by k^4, so the ill-conditioned c2 == 0
             * state of the Math844 sample is preserved. A correct implementation must therefore
             * reject every non-zero scaled variant the same way.
             */
            scaledGuesser.guess();
            throw new RuntimeException(
                    "[oracle:nonzero-scale] metamorphic violation: non-zero scaling of the Math844 triangular sample must preserve MathIllegalStateException, scale="
                            + scale);
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Expected.
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }
    }
}