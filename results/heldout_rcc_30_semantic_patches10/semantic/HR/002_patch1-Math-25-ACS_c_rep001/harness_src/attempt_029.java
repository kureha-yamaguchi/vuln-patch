package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };

        int xOffset = data.consumeInt(-8, 8);
        int xStep = data.consumeInt(1, 4);

        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, xOffset + ((double) i) * xStep, y[i]);
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(points[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable ignored) {
            return;
        }

        final double[] guess;
        try {
            guess = guesser.guess();
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable ignored) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }

        final HarmonicOscillator model;
        try {
            model = new HarmonicOscillator(guess[0], guess[1], guess[2]);
        } catch (Throwable ignored) {
            return;
        }

        double observedMin = Double.POSITIVE_INFINITY;
        double observedMax = Double.NEGATIVE_INFINITY;
        double modelMin = Double.POSITIVE_INFINITY;
        double modelMax = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < len; i++) {
            double oy = points[i].getY();
            if (oy < observedMin) {
                observedMin = oy;
            }
            if (oy > observedMax) {
                observedMax = oy;
            }

            final double my;
            try {
                my = model.value(points[i].getX());
            } catch (Throwable ignored) {
                return;
            }

            if (my < modelMin) {
                modelMin = my;
            }
            if (my > modelMax) {
                modelMax = my;
            }
        }

        double observedSpan = observedMax - observedMin;
        double modelSpan = modelMax - modelMin;

        if (observedSpan >= 6.0 && !(modelSpan > 1.0e-12)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:model-span] consistency violation: guess() returned coefficients for a flat harmonic model on a sample with wide variation"
                    + " observedSpan=" + observedSpan
                    + " modelSpan=" + modelSpan
                    + " a=" + guess[0]
                    + " omega=" + guess[1]
                    + " phi=" + guess[2]
                    + " xOffset=" + xOffset
                    + " xStep=" + xStep);
        }
    }
}