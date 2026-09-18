package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.util.FastMath;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedMath844Oracle();
        checkPositiveYScalingMetamorphic(data);
    }

    private static void checkLiftedMath844Oracle() {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math844-exn] semantic mismatch: expected MathIllegalStateException for the triangular periodic sample from HarmonicFitterTest.testMath844, but guess() returned normally");
        } catch (MathIllegalStateException expected) {
            // Ground-truth oracle lifted verbatim from the failing regression test.
        } catch (Throwable other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            // Any other rejection means this input does not satisfy the exact test setup/result.
            return;
        }
    }

    private static void checkPositiveYScalingMetamorphic(FuzzedDataProvider data) {
        final int n = data.consumeInt(6, 24);
        final double amplitude = 0.5 + (data.consumeInt(0, 2000) / 100.0);
        final double omega = 0.1 + (data.consumeInt(0, 2000) / 500.0);
        final double phi = -FastMath.PI + (2.0 * FastMath.PI * data.consumeInt(0, 2000) / 2000.0);
        final double step = 0.1 + (data.consumeInt(0, 2000) / 200.0);
        final double scale = 0.5 + (data.consumeInt(1, 2000) / 200.0);

        final HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phi);
        final WeightedObservedPoint[] base = new WeightedObservedPoint[n];
        final WeightedObservedPoint[] scaled = new WeightedObservedPoint[n];

        for (int i = 0; i < n; i++) {
            final double x = i * step;
            final double y = osc.value(x);
            base[i] = new WeightedObservedPoint(1.0, x, y);
            scaled[i] = new WeightedObservedPoint(1.0, x, y * scale);
        }

        final double[] g1;
        final double[] g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(base).guess();
            g2 = new HarmonicFitter.ParameterGuesser(scaled).guess();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            // Per hygiene rules: if either real call rejects, this metamorphic check does not apply.
            return;
        }

        final double amp1 = g1[0];
        final double omega1 = g1[1];
        final double amp2 = g2[0];
        final double omega2 = g2[1];

        // Contract justification:
        // ParameterGuesser.guess() estimates harmonic coefficients from the provided observations.
        // For two samples with identical x values and y values related by a positive scalar factor s,
        // they represent the same waveform shape with only amplitude scaled by s. A correct guesser
        // must therefore report the same angular frequency and an amplitude scaled by s.
        // This uses two independent real calls and reaches guessAOmega/FastMath.sqrt on harmonic data.
        final double expectedAmp2 = amp1 * scale;
        if (!closeRelative(expectedAmp2, amp2, 1e-6, 1e-8)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:y-positive-scaling] metamorphic violation: positive y-scaling must scale guessed amplitude by the same factor"
                    + " scale=" + scale
                    + " amp1=" + amp1
                    + " expectedAmp2=" + expectedAmp2
                    + " actualAmp2=" + amp2
                    + " omega1=" + omega1
                    + " omega2=" + omega2);
        }

        if (!closeRelative(omega1, omega2, 1e-6, 1e-8)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:y-positive-scaling] consistency violation: positive y-scaling must preserve guessed angular frequency"
                    + " scale=" + scale
                    + " amp1=" + amp1
                    + " amp2=" + amp2
                    + " omega1=" + omega1
                    + " omega2=" + omega2);
        }
    }

    private static boolean closeRelative(double expected, double actual, double relTol, double absTol) {
        final double diff = FastMath.abs(expected - actual);
        final double bound = FastMath.max(absTol, relTol * FastMath.max(FastMath.abs(expected), FastMath.abs(actual)));
        return diff <= bound;
    }
}