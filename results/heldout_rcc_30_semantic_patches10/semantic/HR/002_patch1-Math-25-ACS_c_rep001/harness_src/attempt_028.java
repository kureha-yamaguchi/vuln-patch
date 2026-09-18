package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkWeightedObservedPointConstructorGetters(data);
        checkSeedMath844Oracle();
        checkConcatenatedTriangularFamilyOracle(data);
    }

    private static void checkWeightedObservedPointConstructorGetters(FuzzedDataProvider data) {
        double w = finiteFromInt(data.consumeInt(-1000000, 1000000));
        double x = finiteFromInt(data.consumeInt(-1000000, 1000000));
        double y = finiteFromInt(data.consumeInt(-1000000, 1000000));

        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean bad =
                Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);
        if (bad) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:point-ctor-getters] semantic mismatch: expected=(" +
                    w + "," + x + "," + y + ") actual=(" +
                    p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }
    }

    private static void checkSeedMath844Oracle() {
        HarmonicFitter.ParameterGuesser g;
        try {
            g = new HarmonicFitter.ParameterGuesser(buildSeedPoints(1.0));
        } catch (Throwable t) {
            return;
        }

        try {
            g.guess();
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException, got " + t.getClass().getName(), t);
        }

        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException, got completed normally");
    }

    private static void checkConcatenatedTriangularFamilyOracle(FuzzedDataProvider data) {
        int amplitude = data.consumeInt(1, 1000);
        int repeats = data.consumeInt(2, 5);

        HarmonicFitter.ParameterGuesser g;
        try {
            g = new HarmonicFitter.ParameterGuesser(buildConcatenatedSeedPoints(amplitude, repeats));
        } catch (Throwable t) {
            return;
        }

        try {
            g.guess();
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:concat-triangular-family] semantic mismatch: expected MathIllegalStateException for concatenated triangular family, amplitude=" +
                    amplitude + " repeats=" + repeats + " got " + t.getClass().getName(), t);
        }

        /* Contract/justification:
         * The trusted failing test pins that this integer-sampled triangular waveform is too far from
         * harmonic for ParameterGuesser.guess() and must be rejected with MathIllegalStateException.
         * Scaling the ordinates by a positive factor and concatenating additional copies preserves that
         * same non-harmonic triangular family while exercising the real guessAOmega path on a different
         * valid sample shape. A seed-only or throw-deleting patch can let this related family return
         * parameters instead of rejecting it.
         */
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:concat-triangular-family] semantic mismatch: expected MathIllegalStateException for concatenated triangular family, amplitude=" +
                amplitude + " repeats=" + repeats + " got completed normally");
    }

    private static WeightedObservedPoint[] buildSeedPoints(double scale) {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };
        WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i] * scale);
        }
        return points;
    }

    private static WeightedObservedPoint[] buildConcatenatedSeedPoints(double scale, int repeats) {
        final double[] y = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };

        int totalLength = y.length + (repeats - 1) * (y.length - 1);
        WeightedObservedPoint[] points = new WeightedObservedPoint[totalLength];

        int out = 0;
        int x = 0;
        for (int r = 0; r < repeats; r++) {
            int start = (r == 0) ? 0 : 1;
            for (int i = start; i < y.length; i++) {
                points[out++] = new WeightedObservedPoint(1.0, x++, y[i] * scale);
            }
        }
        return points;
    }

    private static double finiteFromInt(int v) {
        return (double) v;
    }
}