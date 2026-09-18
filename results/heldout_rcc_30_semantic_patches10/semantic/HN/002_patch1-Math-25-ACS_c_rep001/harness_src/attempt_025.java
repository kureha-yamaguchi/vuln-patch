package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException for HarmonicFitter.ParameterGuesser.guess() on the Math844 triangular-wave sample, but no exception was thrown");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (org.apache.commons.math3.exception.NumberIsTooSmallException unexpected) {
            return;
        } catch (org.apache.commons.math3.exception.ZeroException unexpected) {
            return;
        }

        int n = data.consumeInt(4, 20);
        double amplitude = data.consumeInt(-20, 20);
        if (amplitude == 0.0) {
            amplitude = 1.0;
        }
        double omega = data.consumeInt(1, 20) / 10.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        double step = data.consumeInt(1, 10) / 10.0;
        double start = data.consumeInt(-50, 50) / 10.0;

        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] ordered = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            double value = oscillator.value(x);
            ordered[i] = new WeightedObservedPoint(1.0, x, value);
        }

        WeightedObservedPoint[] permuted = ordered.clone();
        int swaps = data.consumeInt(0, n * 2);
        for (int i = 0; i < swaps; i++) {
            int a = data.consumeInt(0, n - 1);
            int b = data.consumeInt(0, n - 1);
            WeightedObservedPoint tmp = permuted[a];
            permuted[a] = permuted[b];
            permuted[b] = tmp;
        }

        double[] guessOrdered;
        double[] guessPermuted;
        try {
            guessOrdered = new HarmonicFitter.ParameterGuesser(ordered).guess();
            guessPermuted = new HarmonicFitter.ParameterGuesser(permuted).guess();
        } catch (Throwable t) {
            return;
        }

        if (guessOrdered.length != guessPermuted.length) {
            throw new RuntimeException(
                "[oracle:sort-invariance] metamorphic violation: reordered observations changed guess array length inputLength="
                    + n + " lhsLen=" + guessOrdered.length + " rhsLen=" + guessPermuted.length);
        }

        for (int i = 0; i < guessOrdered.length; i++) {
            if (!sameDouble(guessOrdered[i], guessPermuted[i])) {
                throw new RuntimeException(
                    "[oracle:sort-invariance] metamorphic violation: ParameterGuesser sorts observations by abscissa, so the same observation set must yield the same guess regardless of input order inputLength="
                        + n + " index=" + i + " lhs=" + guessOrdered[i] + " rhs=" + guessPermuted[i]);
            }
        }
    }

    private static boolean sameDouble(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }
}