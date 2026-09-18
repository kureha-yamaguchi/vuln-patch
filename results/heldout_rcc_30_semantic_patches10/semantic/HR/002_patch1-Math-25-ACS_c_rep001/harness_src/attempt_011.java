package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedMath844ThroughRealCode();
        checkDuplicateObservationFunctionConsistency(data);
    }

    private static void checkLiftedMath844ThroughRealCode() {
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
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844-direct] semantic mismatch: expected MathIllegalStateException for the documented Math844 seed, but guess() returned normally");
        } catch (MathIllegalStateException expected) {
        } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
        }
    }

    private static void checkDuplicateObservationFunctionConsistency(FuzzedDataProvider data) {
        final int n = data.consumeInt(8, 24);
        final double amplitude = 0.5 + data.consumeInt(0, 9500) / 1000.0;
        final double omega = 0.1 + data.consumeInt(0, 4900) / 1000.0;
        final double phi = -3.0 + data.consumeInt(0, 6000) / 1000.0;
        final double step = 0.1 + data.consumeInt(0, 900) / 1000.0;
        final double xOffset = -5.0 + data.consumeInt(0, 10000) / 1000.0;

        final HarmonicOscillator source = new HarmonicOscillator(amplitude, omega, phi);

        final HarmonicFitter baseFitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
        final HarmonicFitter duplicatedFitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < n; i++) {
            final double x = xOffset + i * step;
            final double y = source.value(x);
            baseFitter.addObservedPoint(1.0, x, y);
            duplicatedFitter.addObservedPoint(1.0, x, y);
            duplicatedFitter.addObservedPoint(1.0, x, y);
        }

        final double[] baseParams;
        final double[] dupParams;
        try {
            baseParams = baseFitter.fit();
            dupParams = duplicatedFitter.fit();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (baseParams == null || dupParams == null || baseParams.length < 3 || dupParams.length < 3) {
            return;
        }

        final HarmonicOscillator baseFn = new HarmonicOscillator(baseParams[0], baseParams[1], baseParams[2]);
        final HarmonicOscillator dupFn = new HarmonicOscillator(dupParams[0], dupParams[1], dupParams[2]);

        double maxDisagreement = 0.0;
        double maxResidualBase = 0.0;
        double maxResidualDup = 0.0;

        for (int i = 0; i < n; i++) {
            final double x = xOffset + i * step;
            final double expected = source.value(x);
            final double baseY = baseFn.value(x);
            final double dupY = dupFn.value(x);

            maxDisagreement = Math.max(maxDisagreement, Math.abs(baseY - dupY));
            maxResidualBase = Math.max(maxResidualBase, Math.abs(baseY - expected));
            maxResidualDup = Math.max(maxResidualDup, Math.abs(dupY - expected));

            final double midpoint = x + 0.5 * step;
            final double expectedMid = source.value(midpoint);
            final double baseMid = baseFn.value(midpoint);
            final double dupMid = dupFn.value(midpoint);

            maxDisagreement = Math.max(maxDisagreement, Math.abs(baseMid - dupMid));
            maxResidualBase = Math.max(maxResidualBase, Math.abs(baseMid - expectedMid));
            maxResidualDup = Math.max(maxResidualDup, Math.abs(dupMid - expectedMid));
        }

        final double tolerance = 1.0e-3 * Math.max(1.0, amplitude);

        if (maxResidualBase > tolerance || maxResidualDup > tolerance) {
            return;
        }

        // Contract used for this oracle:
        // fit() returns parameters of the harmonic function that best fits the observed points.
        // For data constructed exactly from a HarmonicOscillator, duplicating every observation
        // preserves the same exact zero-residual solution, so two real fit() calls must describe
        // the same function on the same domain. A throw-deleting patch in guessAOmega can let one
        // call continue with inconsistent internal a/omega state while the other diverges.
        if (maxDisagreement > tolerance) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:duplicate-observation-consistency] metamorphic violation: duplicating exact observations changed the fitted function"
                    + " amplitude=" + amplitude
                    + " omega=" + omega
                    + " phi=" + phi
                    + " step=" + step
                    + " n=" + n
                    + " maxDisagreement=" + maxDisagreement
                    + " tolerance=" + tolerance
                    + " maxResidualBase=" + maxResidualBase
                    + " maxResidualDup=" + maxResidualDup
                    + " baseA=" + baseParams[0]
                    + " baseOmega=" + baseParams[1]
                    + " basePhi=" + baseParams[2]
                    + " dupA=" + dupParams[0]
                    + " dupOmega=" + dupParams[1]
                    + " dupPhi=" + dupParams[2]);
        }
    }
}