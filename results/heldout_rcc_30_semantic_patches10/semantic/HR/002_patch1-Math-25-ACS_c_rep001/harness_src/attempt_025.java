package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] TRIANGULAR_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int pivot = data.consumeInt(1, TRIANGULAR_Y.length - 2);
        int pow = data.consumeInt(4, 40);
        double delta = Math.scalb(1.0, -pow);
        if (data.consumeBoolean()) {
            delta = -delta;
        }

        WeightedObservedPoint[] seed = makePoints(0.0, -1);
        callThroughPublicApi(seed);

        WeightedObservedPoint[] plus = makePoints(Math.abs(delta), pivot);
        WeightedObservedPoint[] minus = makePoints(-Math.abs(delta), pivot);

        checkBoundaryFormulaConsistency(plus);
        checkBoundaryFormulaConsistency(minus);
    }

    private static WeightedObservedPoint[] makePoints(double delta, int pivot) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            double y = TRIANGULAR_Y[i];
            if (i == pivot) {
                y += delta;
            }
            points[i] = new WeightedObservedPoint(1.0, i, y);
        }
        return points;
    }

    private static void callThroughPublicApi(WeightedObservedPoint[] points) {
        try {
            org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer();
            HarmonicFitter fitter = new HarmonicFitter(optimizer);
            for (int i = 0; i < points.length; i++) {
                fitter.addObservedPoint(points[i].getWeight(), points[i].getX(), points[i].getY());
            }
            try {
                fitter.fit();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private static void checkBoundaryFormulaConsistency(WeightedObservedPoint[] points) {
        double[] coeffs = computeCoefficients(points);
        double c1 = coeffs[0];
        double c2 = coeffs[1];
        double c3 = coeffs[2];

        if (!isFinite(c1) || !isFinite(c2) || !isFinite(c3)) {
            return;
        }
        if (c2 == 0.0 || c3 == 0.0) {
            return;
        }

        double ratioA = c1 / c2;
        double ratioOmega = c2 / c3;
        if (!isFinite(ratioA) || !isFinite(ratioOmega)) {
            return;
        }
        if (ratioA < 0.0 || ratioOmega < 0.0) {
            return;
        }

        double expectedA;
        double expectedOmega;
        try {
            expectedA = org.apache.commons.math3.util.FastMath.sqrt(ratioA);
            expectedOmega = org.apache.commons.math3.util.FastMath.sqrt(ratioOmega);
        } catch (Throwable t) {
            return;
        }
        if (!isFinite(expectedA) || !isFinite(expectedOmega)) {
            return;
        }

        double[] guessed;
        try {
            guessed = new HarmonicFitter.ParameterGuesser(points).guess();
        } catch (Throwable t) {
            return;
        }
        if (guessed == null || guessed.length < 2) {
            return;
        }

        // Contract from the shown real code: when guessAOmega() takes the else-branch
        // (i.e. c1/c2 >= 0 and c2/c3 >= 0 with c2 != 0), it sets fields a and omega
        // exactly to sqrt(c1/c2) and sqrt(c2/c3), and guess() returns those fields.
        // A patch that overfits the exact c2 == 0 seed by clamping/skipping/changing the
        // boundary behavior just past zero violates this direct formula agreement.
        if (!closeEnough(guessed[0], expectedA) || !closeEnough(guessed[1], expectedOmega)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:c2-boundary-formula] consistency violation: " +
                "c1=" + c1 + " c2=" + c2 + " c3=" + c3 +
                " expectedA=" + expectedA + " actualA=" + guessed[0] +
                " expectedOmega=" + expectedOmega + " actualOmega=" + guessed[1]);
        }
    }

    private static double[] computeCoefficients(WeightedObservedPoint[] observations) {
        double sx2 = 0.0;
        double sy2 = 0.0;
        double sxy = 0.0;
        double sxz = 0.0;
        double syz = 0.0;

        double currentX = observations[0].getX();
        double currentY = observations[0].getY();
        double f2Integral = 0.0;
        double fPrime2Integral = 0.0;
        final double startX = currentX;

        for (int i = 1; i < observations.length; ++i) {
            final double previousX = currentX;
            final double previousY = currentY;
            currentX = observations[i].getX();
            currentY = observations[i].getY();

            final double dx = currentX - previousX;
            final double dy = currentY - previousY;
            final double f2StepIntegral =
                dx * (previousY * previousY + previousY * currentY + currentY * currentY) / 3.0;
            final double fPrime2StepIntegral = dy * dy / dx;

            final double x = currentX - startX;
            f2Integral += f2StepIntegral;
            fPrime2Integral += fPrime2StepIntegral;

            sx2 += x * x;
            sy2 += f2Integral * f2Integral;
            sxy += x * f2Integral;
            sxz += x * fPrime2Integral;
            syz += f2Integral * fPrime2Integral;
        }

        double c1 = sy2 * sxz - sxy * syz;
        double c2 = sxy * sxz - sx2 * syz;
        double c3 = sx2 * sy2 - sxy * sxy;
        return new double[] { c1, c2, c3 };
    }

    private static boolean closeEnough(double actual, double expected) {
        double diff = Math.abs(actual - expected);
        double scale = Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected)));
        return diff <= 1e-12 * scale;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}