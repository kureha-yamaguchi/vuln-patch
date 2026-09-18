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
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            final HarmonicFitter.ParameterGuesser guesser =
                    new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math844-exception] semantic mismatch: expected MathIllegalStateException for the exact testMath844 sample, but guess() returned normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Ground-truth lifted from HarmonicFitterTest.testMath844.
        } catch (Throwable other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        final int n = data.consumeInt(4, 24);
        double amplitude = data.consumeInt(-500, 500) / 50.0;
        if (amplitude == 0.0) {
            amplitude = 1.0;
        }
        final double omega = data.consumeInt(1, 300) / 100.0;
        final double phase = data.consumeInt(-314, 314) / 100.0;
        final double start = data.consumeInt(-100, 100) / 10.0;
        final double step = data.consumeInt(1, 50) / 10.0;

        final org.apache.commons.math3.analysis.function.HarmonicOscillator oscillator =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(amplitude, omega, phase);

        final WeightedObservedPoint[] harmonicPoints = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + i * step;
            harmonicPoints[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }

        final double[] initialGuess;
        try {
            initialGuess = new HarmonicFitter.ParameterGuesser(harmonicPoints).guess();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final double[] autoFit;
        final double[] explicitFit;
        try {
            final HarmonicFitter fitterAuto =
                    new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            final HarmonicFitter fitterExplicit =
                    new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                fitterAuto.addObservedPoint(harmonicPoints[i]);
                fitterExplicit.addObservedPoint(harmonicPoints[i]);
            }

            // Contract: fit() "An initial guess will be automatically computed"
            // and refers to the other fit(double[]) method, so on the same observations
            // fit() must agree with fit(ParameterGuesser.guess()). A patch that merely
            // suppresses/avoids the bad-state detection in guessAOmega can make fit()
            // proceed with a wrong auto-generated guess and diverge from the explicit path.
            autoFit = fitterAuto.fit();
            explicitFit = fitterExplicit.fit(initialGuess);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (autoFit == null || explicitFit == null || autoFit.length != 3 || explicitFit.length != 3) {
            return;
        }

        final org.apache.commons.math3.analysis.function.HarmonicOscillator autoOsc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(autoFit[0], autoFit[1], autoFit[2]);
        final org.apache.commons.math3.analysis.function.HarmonicOscillator explicitOsc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(explicitFit[0], explicitFit[1], explicitFit[2]);

        double maxDiff = 0.0;
        for (int i = 0; i < n; i++) {
            final double x = harmonicPoints[i].getX();
            final double diff = Math.abs(autoOsc.value(x) - explicitOsc.value(x));
            if (diff > maxDiff) {
                maxDiff = diff;
            }
        }

        if (maxDiff > 1.0e-6) {
            throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() disagrees with fit(ParameterGuesser.guess()) on the same observations input="
                            + "n=" + n
                            + ",amplitude=" + amplitude
                            + ",omega=" + omega
                            + ",phase=" + phase
                            + ",start=" + start
                            + ",step=" + step
                            + " lhs=" + autoFit[0] + "," + autoFit[1] + "," + autoFit[2]
                            + " rhs=" + explicitFit[0] + "," + explicitFit[1] + "," + explicitFit[2]
                            + " maxPredictionDiff=" + maxDiff);
        }
    }
}