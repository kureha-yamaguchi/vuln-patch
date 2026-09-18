package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.util.FastMath;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseLiftedMath844Path();

        int sampleCount = data.consumeInt(24, 96);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 10.0;
        double omega = 0.15 + data.consumeInt(0, 120) / 40.0;
        double phase = -FastMath.PI + (2.0 * FastMath.PI * data.consumeInt(0, 1000) / 1000.0);
        double step = 0.05 + data.consumeInt(1, 40) / 20.0;
        double scale = 2.0 + data.consumeInt(0, 20) / 5.0;

        WeightedObservedPoint[] original = buildExactHarmonicPoints(sampleCount, step, amplitude, omega, phase);
        WeightedObservedPoint[] scaled = buildExactHarmonicPoints(sampleCount, step * scale, amplitude, omega / scale, phase);

        double[] guessOriginal;
        double[] guessScaled;
        try {
            guessOriginal = new HarmonicFitter.ParameterGuesser(original).guess();
            guessScaled = new HarmonicFitter.ParameterGuesser(scaled).guess();
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract justification:
         * HarmonicFitter models samples as f(t) = a cos(omega * t + phi).
         * If we scale every abscissa by s and keep ordinates identical, the same
         * exact signal is represented by a' = a, omega' = omega / s, phi' = phi.
         * For exact harmonic samples, ParameterGuesser is estimating the same
         * underlying quantity from two equivalent representations, so the two
         * guesses must agree under that change of variables. A patch that merely
         * suppresses the Math844 exception or masks a wrong c2 handling can leave
         * helper state inconsistent here even when top-level callers stop throwing.
         */
        double ampTol = scaledTolerance(guessOriginal[0], guessScaled[0]);
        if (FastMath.abs(guessOriginal[0] - guessScaled[0]) > ampTol) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:x-scale-guesser-amplitude] consistency violation: amplitude should be invariant under x scaling"
                    + " scale=" + scale
                    + " a1=" + guessOriginal[0]
                    + " a2=" + guessScaled[0]
                    + " tol=" + ampTol);
        }

        double rescaledOmega = guessScaled[1] * scale;
        double omegaTol = scaledTolerance(guessOriginal[1], rescaledOmega);
        if (FastMath.abs(guessOriginal[1] - rescaledOmega) > omegaTol) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:x-scale-guesser-omega] consistency violation: omega should scale inversely with x"
                    + " scale=" + scale
                    + " omega1=" + guessOriginal[1]
                    + " omega2=" + guessScaled[1]
                    + " omega2Rescaled=" + rescaledOmega
                    + " tol=" + omegaTol);
        }

        double phaseDiff = normalizedAngle(guessOriginal[2] - guessScaled[2]);
        double phaseTol = 5e-2;
        if (FastMath.abs(phaseDiff) > phaseTol) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:x-scale-guesser-phase] consistency violation: phase should be preserved under pure x scaling"
                    + " scale=" + scale
                    + " phi1=" + guessOriginal[2]
                    + " phi2=" + guessScaled[2]
                    + " normalizedDiff=" + phaseDiff
                    + " tol=" + phaseTol);
        }
    }

    private static void exerciseLiftedMath844Path() {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable ignored) {
            return;
        }
    }

    private static WeightedObservedPoint[] buildExactHarmonicPoints(
            int sampleCount, double step, double amplitude, double omega, double phase) {
        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] points = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = i * step;
            points[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }
        return points;
    }

    private static double normalizedAngle(double angle) {
        double twoPi = 2.0 * FastMath.PI;
        double a = angle % twoPi;
        if (a <= -FastMath.PI) {
            a += twoPi;
        } else if (a > FastMath.PI) {
            a -= twoPi;
        }
        return a;
    }

    private static double scaledTolerance(double a, double b) {
        double scale = FastMath.max(1.0, FastMath.max(FastMath.abs(a), FastMath.abs(b)));
        return 1e-2 * scale;
    }
}