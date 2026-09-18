package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] TRIANGULAR_Y = new double[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    private static WeightedObservedPoint[] buildTriangularPoints(double xOffset, double step, double weight) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(weight, xOffset + (i * step), TRIANGULAR_Y[i]);
        }
        return points;
    }

    private static HarmonicFitter buildFitter(WeightedObservedPoint[] points) {
        HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
        for (WeightedObservedPoint p : points) {
            fitter.addObservedPoint(p);
        }
        return fitter;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        WeightedObservedPoint[] exactSeedPoints;
        try {
            exactSeedPoints = buildTriangularPoints(0.0, 1.0, 1.0);
        } catch (Throwable t) {
            return;
        }

        HarmonicFitter.ParameterGuesser seedGuesser;
        try {
            seedGuesser = new HarmonicFitter.ParameterGuesser(exactSeedPoints);
        } catch (Throwable t) {
            return;
        }

        boolean seedViolation = false;
        Throwable seedWrong = null;
        try {
            seedGuesser.guess();
            seedViolation = true;
        } catch (MathIllegalStateException expected) {
        } catch (Throwable t) {
            seedViolation = true;
            seedWrong = t;
        }
        if (seedViolation) {
            if (seedWrong != null) {
                throw new FuzzerSecurityIssueLow("[oracle:math844-direct] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the trusted MATH-844 triangular sample, got " + seedWrong.getClass().getName(), seedWrong);
            }
            throw new FuzzerSecurityIssueLow("[oracle:math844-direct] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the trusted MATH-844 triangular sample, but the call completed normally");
        }

        double offset = data.consumeInt(-1000, 1000);
        int stepNumerator = data.consumeInt(1, 5);
        double step = stepNumerator;
        double weight = data.consumeBoolean() ? 1.0 : 2.0;

        WeightedObservedPoint[] translatedPoints;
        try {
            translatedPoints = buildTriangularPoints(offset, step, weight);
        } catch (Throwable t) {
            return;
        }

        HarmonicFitter fitter;
        try {
            fitter = buildFitter(translatedPoints);
        } catch (Throwable t) {
            return;
        }

        boolean publicViolation = false;
        Throwable publicWrong = null;
        try {
            fitter.fit();
            publicViolation = true;
        } catch (MathIllegalStateException expected) {
        } catch (Throwable t) {
            publicViolation = true;
            publicWrong = t;
        }
        if (publicViolation) {
            if (publicWrong != null) {
                throw new FuzzerSecurityIssueLow("[oracle:translated-public-fit] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on an x-translated copy of the trusted triangular sample, got " + publicWrong.getClass().getName() + " offset=" + offset + " step=" + step + " weight=" + weight, publicWrong);
            }
            throw new FuzzerSecurityIssueLow("[oracle:translated-public-fit] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on an x-translated copy of the trusted triangular sample, but the call completed normally; offset=" + offset + " step=" + step + " weight=" + weight);
        }

        boolean repeatViolation = false;
        Throwable repeatWrong = null;
        try {
            fitter.fit();
            repeatViolation = true;
        } catch (MathIllegalStateException expected) {
        } catch (Throwable t) {
            repeatViolation = true;
            repeatWrong = t;
        }
        if (repeatViolation) {
            if (repeatWrong != null) {
                throw new FuzzerSecurityIssueLow("[oracle:repeat-fit-rejection] metamorphic violation: HarmonicFitter.fit() is a read-only query over the accumulated observations, so after one rejected fit on unchanged data the same call must reject the same way again; got " + repeatWrong.getClass().getName() + " on the second call with offset=" + offset + " step=" + step + " weight=" + weight, repeatWrong);
            }
            throw new FuzzerSecurityIssueLow("[oracle:repeat-fit-rejection] metamorphic violation: HarmonicFitter.fit() is a read-only query over the accumulated observations, so after one rejected fit on unchanged data the same call must reject the same way again; second call completed normally with offset=" + offset + " step=" + step + " weight=" + weight);
        }

        double amplitude = (data.consumeInt(1, 20)) / 3.0;
        double omega = (data.consumeInt(1, 30)) / 10.0;
        double phi = data.consumeInt(-314, 314) / 100.0;
        int n = data.consumeInt(8, 24);
        double xStart = data.consumeInt(-50, 50) / 10.0;
        double xStep = data.consumeInt(1, 10) / 10.0;

        HarmonicOscillator oscillator;
        try {
            oscillator = new HarmonicOscillator(amplitude, omega, phi);
        } catch (Throwable t) {
            return;
        }

        HarmonicFitter exactFitter;
        try {
            exactFitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                double x = xStart + (i * xStep);
                double y = oscillator.value(x);
                exactFitter.addObservedPoint(1.0, x, y);
            }
        } catch (Throwable t) {
            return;
        }

        double[] fit1;
        double[] fit2;
        try {
            fit1 = exactFitter.fit();
            fit2 = exactFitter.fit();
        } catch (Throwable t) {
            return;
        }

        if (fit1 == null || fit2 == null || fit1.length != 3 || fit2.length != 3) {
            return;
        }

        double maxDelta = 0.0;
        for (int i = 0; i < 3; i++) {
            double d = Math.abs(fit1[i] - fit2[i]);
            if (d > maxDelta) {
                maxDelta = d;
            }
        }

        // Contract justification: fit() is a query over stored observations; with no intervening mutation,
        // repeated calls on the same exact data must be stable. A patch that masks the exception path by
        // corrupting internal state or silently changing bookkeeping can violate this even if one call returns.
        if (maxDelta > 1.0e-7) {
            throw new FuzzerSecurityIssueLow("[oracle:repeat-fit-stability] consistency violation: repeated HarmonicFitter.fit() calls on unchanged exact harmonic data disagreed; first=[" + fit1[0] + "," + fit1[1] + "," + fit1[2] + "] second=[" + fit2[0] + "," + fit2[1] + "," + fit2[2] + "] amplitude=" + amplitude + " omega=" + omega + " phi=" + phi + " n=" + n + " xStart=" + xStart + " xStep=" + xStep);
        }
    }
}