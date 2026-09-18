package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean wopViolation = false;
        String wopMessage = null;
        try {
            double w = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            double x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            double y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            WeightedObservedPoint p = new WeightedObservedPoint(w, x, y);
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                wopViolation = true;
                wopMessage = "relation weighted_observed_point_getters_match_constructor violated: expected ("
                        + w + "," + x + "," + y + ") but got ("
                        + p.getWeight() + "," + p.getX() + "," + p.getY() + ")";
            }
        } catch (Throwable ignored) {
            return;
        }
        if (wopViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:unnamed-check] " + wopMessage);
        }

        try {
            double[] seedY = {
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1,
                    0, -1, -2, -3, -2, -1,
                    0, 1, 2, 3, 2, 1, 0
            };
            WeightedObservedPoint[] seedPoints = new WeightedObservedPoint[seedY.length];
            for (int i = 0; i < seedY.length; i++) {
                seedPoints[i] = new WeightedObservedPoint(1.0, i, seedY[i]);
            }
            HarmonicFitter.ParameterGuesser g = new HarmonicFitter.ParameterGuesser(seedPoints);
            try {
                g.guess();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
            return;
        }

        boolean anchorViolation = false;
        String anchorMessage = null;
        try {
            int periods = data.consumeInt(1, 3);
            int samplesPerPeriod = data.consumeInt(8, 24);
            int n = periods * samplesPerPeriod + 1;
            double amplitude = data.consumeInt(1, 1000);
            double shift = data.consumeInt(-20, 20);
            double omega = 1.0;
            double phi = -shift;
            double step = (2.0 * Math.PI) / samplesPerPeriod;

            WeightedObservedPoint[] points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = shift + i * step;
                double yi = amplitude * Math.cos(omega * xi + phi);
                points[i] = new WeightedObservedPoint(1.0, xi, yi);
            }

            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            double[] guessed = guesser.guess();
            if (guessed == null || guessed.length < 3) {
                return;
            }

            double ga = guessed[0];
            double gw = guessed[1];
            double gp = guessed[2];
            HarmonicOscillator osc = new HarmonicOscillator(ga, gw, gp);

            double x0 = shift;
            double xQuarter = shift + 0.5 * Math.PI;
            double xHalf = shift + Math.PI;

            double y0 = osc.value(x0);
            double yQuarter = osc.value(xQuarter);
            double yHalf = osc.value(xHalf);

            double expected0 = amplitude;
            double expectedQuarter = 0.0;
            double expectedHalf = -amplitude;
            double tol = Math.max(1.0e-6, amplitude * 1.0e-4);

            boolean ok0 = Math.abs(y0 - expected0) <= tol;
            boolean okQuarter = Math.abs(yQuarter - expectedQuarter) <= tol;
            boolean okHalf = Math.abs(yHalf - expectedHalf) <= tol;

            if (!ok0 || !okQuarter || !okHalf) {
                anchorViolation = true;
                anchorMessage =
                        "[oracle:phase-anchor-consistency] consistency violation: reported=(a=" + ga
                                + ",omega=" + gw + ",phi=" + gp + ") anchors=(" + y0 + "," + yQuarter + ","
                                + yHalf + ") expected=(" + expected0 + "," + expectedQuarter + "," + expectedHalf
                                + ") x=(" + x0 + "," + xQuarter + "," + xHalf + ") samplesPerPeriod="
                                + samplesPerPeriod + " periods=" + periods;
            }
        } catch (Throwable ignored) {
            return;
        }
        if (anchorViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(anchorMessage);
        }
    }
}