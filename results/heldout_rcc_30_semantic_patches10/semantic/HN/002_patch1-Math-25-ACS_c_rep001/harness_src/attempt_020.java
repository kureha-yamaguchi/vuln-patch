package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
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
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but the call returned normally");
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math844] semantic mismatch: expected MathIllegalStateException from ParameterGuesser.guess() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but the call returned normally");
        } catch (MathIllegalStateException expected) {
        } catch (Throwable other) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:fit-noarg] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.fit() on the Math844 sample, but got "
                    + other.getClass().getName(),
                other);
        }

        int sampleCount = 4 + data.consumeInt(0, 12);
        double amplitude = 1.0 + data.consumeInt(0, 200) / 20.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 50.0;
        double phase = data.consumeInt(-314, 314) / 100.0;
        int xShift = data.consumeInt(-50, 50);
        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);

        WeightedObservedPoint[] original = new WeightedObservedPoint[sampleCount];
        WeightedObservedPoint[] pristine = new WeightedObservedPoint[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double x = xShift + i;
            double yy = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, yy);
            original[i] = p;
            pristine[i] = p;
        }

        HarmonicFitter.ParameterGuesser cloned;
        HarmonicFitter.ParameterGuesser pristineGuesser;
        try {
            cloned = new HarmonicFitter.ParameterGuesser(original);
            pristineGuesser = new HarmonicFitter.ParameterGuesser(pristine);
        } catch (Throwable t) {
            return;
        }

        int mutations = data.consumeInt(1, sampleCount);
        for (int m = 0; m < mutations; m++) {
            int idx = data.consumeInt(0, sampleCount - 1);
            double mx = data.consumeInt(-100, 100);
            double my = data.consumeInt(-1000, 1000) / 10.0;
            double mw = 1.0 + data.consumeInt(0, 10);
            original[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        try {
            double[] lhs1 = cloned.guess();
            double[] lhs2 = cloned.guess();
            double[] rhs = pristineGuesser.guess();

            if (lhs1.length != 3 || lhs2.length != 3 || rhs.length != 3) {
                throw new RuntimeException(
                    "[oracle:guess-shape] metamorphic violation: guess() must return three coefficients input="
                        + sampleCount + " lhs1Len=" + lhs1.length + " lhs2Len=" + lhs2.length + " rhsLen=" + rhs.length);
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(lhs2[i])) {
                    throw new RuntimeException(
                        "[oracle:guess-idempotent] metamorphic violation: repeated guess() on the same ParameterGuesser changed result input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + lhs2[i]);
                }
            }

            for (int i = 0; i < 3; i++) {
                if (Double.doubleToLongBits(lhs1[i]) != Double.doubleToLongBits(rhs[i])) {
                    throw new RuntimeException(
                        "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents that it clones the observations array, so mutating the caller's array after construction must not affect guess(); input=index"
                            + i + " lhs=" + lhs1[i] + " rhs=" + rhs[i]);
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}