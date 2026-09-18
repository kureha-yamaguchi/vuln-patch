package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        fixedMath844Oracle();
        permutationInvarianceOracle(data);
    }

    private static void fixedMath844Oracle() {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave sample, but call returned normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Exact lifted oracle from HarmonicFitterTest.testMath844.
        } catch (RuntimeException other) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() for the triangular-wave sample, but got " +
                other.getClass().getName() + ": " + other.getMessage());
        }
    }

    private static void permutationInvarianceOracle(FuzzedDataProvider data) {
        final int n = data.consumeInt(4, 16);
        final double amplitude = 0.25 + data.consumeInt(0, 400) / 20.0;
        final int period = data.consumeInt(4, 24);
        final double omega = (2.0 * Math.PI) / period;
        final double phase = data.consumeInt(-314, 314) / 100.0;

        final org.apache.commons.math3.analysis.function.HarmonicOscillator oscillator =
            new org.apache.commons.math3.analysis.function.HarmonicOscillator(amplitude, omega, phase);

        final WeightedObservedPoint[] sorted = new WeightedObservedPoint[n];
        double x = 0.0;
        for (int i = 0; i < n; i++) {
            x += data.consumeInt(1, 3);
            sorted[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }

        final WeightedObservedPoint[] permuted = sorted.clone();
        for (int i = permuted.length - 1; i > 0; i--) {
            final int j = data.consumeInt(0, i);
            final WeightedObservedPoint tmp = permuted[i];
            permuted[i] = permuted[j];
            permuted[j] = tmp;
        }

        final double[] guessSorted;
        final double[] guessPermuted;
        try {
            guessSorted = new HarmonicFitter.ParameterGuesser(sorted).guess();
            guessPermuted = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guessSorted == null || guessPermuted == null || guessSorted.length != 3 || guessPermuted.length != 3) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (!sameDouble(guessSorted[i], guessPermuted[i])) {
                // Contract justification: ParameterGuesser works from "sampled observations",
                // and the class contains a dedicated sortObservations() because the algorithm
                // assumes observations are sorted by abscissa. Therefore two inputs containing
                // the same observation set in different orders must yield the same guessed
                // parameters after internal sorting. A patch that merely deletes the
                // MathIllegalStateException or otherwise perturbs shared state can break this
                // observable agreement without throwing.
                throw new RuntimeException(
                    "[oracle:permutation-invariance] metamorphic violation: guess() must be invariant under permutation of the same observation set inputIndex=" +
                    i + " lhs=" + guessSorted[i] + " rhs=" + guessPermuted[i]);
            }
        }
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }
}