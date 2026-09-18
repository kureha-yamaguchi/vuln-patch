package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] SEED_TRIANGULAR_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkWeightedObservedPointGetters(data);
        checkNearBoundaryAnalyticAgreement(data);
    }

    private static void checkWeightedObservedPointGetters(FuzzedDataProvider data) {
        double w = boundedFinite(data);
        double x = boundedFinite(data);
        double y = boundedFinite(data);

        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean violated =
            Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);

        if (violated) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation weighted_observed_point_getters_match_constructor violated: expected (" +
                w + "," + x + "," + y + ") but got (" +
                p.getWeight() + "," + p.getX() + "," + p.getY() + ")"
            );
        }
    }

    private static void checkNearBoundaryAnalyticAgreement(FuzzedDataProvider data) {
        WeightedObservedPoint[] exactSeed = buildPerturbedSeed(data, 0.0);
        HarmonicFitter.ParameterGuesser exactGuesser;
        boolean exactCompleted = false;
        Throwable exactThrowable = null;
        try {
            exactGuesser = new HarmonicFitter.ParameterGuesser(exactSeed);
            exactGuesser.guess();
            exactCompleted = true;
        } catch (Throwable t) {
            exactThrowable = t;
        }

        boolean exactViolation = false;
        String exactObserved = null;
        if (exactCompleted) {
            exactViolation = true;
            exactObserved = "completed normally";
        } else if (!(exactThrowable instanceof org.apache.commons.math3.exception.MathIllegalStateException)) {
            exactViolation = true;
            exactObserved = "wrong exception " + exactThrowable.getClass().getName();
        }
        if (exactViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-exact-throws] semantic mismatch: expected MathIllegalStateException for the exact MATH-844 triangular sample, got " +
                exactObserved,
                exactThrowable instanceof Exception ? (Exception) exactThrowable : null
            );
        }

        double delta = chooseBoundaryDelta(data);
        WeightedObservedPoint[] points = buildPerturbedSeed(data, delta);
        Coefficients coeffs = computeCoefficients(points);
        if (coeffs == null) {
            return;
        }

        if (coeffs.c2 == 0.0) {
            return;
        }
        if (!(coeffs.c1 / coeffs.c2 >= 0.0 && coeffs.c2 / coeffs.c3 >= 0.0)) {
            return;
        }

        double expectedA = org.apache.commons.math3.util.FastMath.sqrt(coeffs.c1 / coeffs.c2);
        double expectedOmega = org.apache.commons.math3.util.FastMath.sqrt(coeffs.c2 / coeffs.c3);
        if (!Double.isFinite(expectedA) || !Double.isFinite(expectedOmega)) {
            return;
        }

        double[] guess;
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guess = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        if (guess == null || guess.length < 2) {
            return;
        }

        double actualA = guess[0];
        double actualOmega = guess[1];

        // Contract/implementation guarantee from the shown code:
        // when (c1/c2 < 0) and (c2/c3 < 0) are both false and c2 != 0, guessAOmega()
        // takes the analytic branch and sets a = sqrt(c1/c2), omega = sqrt(c2/c3).
        // We recompute the same branch predicates and coefficients independently from the
        // public observations, then assert the returned a and omega agree. A patch that
        // overfits the exact seed by broadening the c2==0 guard or silently taking the
        // wrong branch near that boundary would break this equality even if it hides the
        // original exception symptom.
        boolean mismatch =
            !approximatelyEqual(actualA, expectedA) ||
            !approximatelyEqual(actualOmega, expectedOmega);

        if (mismatch) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:analytic-branch-agreement] consistency violation: delta=" + delta +
                " c1=" + coeffs.c1 +
                " c2=" + coeffs.c2 +
                " c3=" + coeffs.c3 +
                " expectedA=" + expectedA +
                " actualA=" + actualA +
                " expectedOmega=" + expectedOmega +
                " actualOmega=" + actualOmega
            );
        }
    }

    private static WeightedObservedPoint[] buildPerturbedSeed(FuzzedDataProvider data, double delta) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[SEED_TRIANGULAR_Y.length];
        int idx = data.consumeInt(1, SEED_TRIANGULAR_Y.length - 2);
        for (int i = 0; i < SEED_TRIANGULAR_Y.length; i++) {
            double y = SEED_TRIANGULAR_Y[i];
            if (i == idx) {
                y += delta;
            }
            points[i] = new WeightedObservedPoint(1.0, i, y);
        }
        return points;
    }

    private static double chooseBoundaryDelta(FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 5);
        int sign = data.consumeBoolean() ? 1 : -1;
        switch (mode) {
            case 0:
                return sign * 1.0e-12;
            case 1:
                return sign * 1.0e-9;
            case 2:
                return sign * 1.0e-6;
            case 3:
                return sign * 1.0e-3;
            case 4:
                return sign * 0.1;
            default:
                return sign * ((data.consumeInt(1, 1000)) / 1000.0);
        }
    }

    private static double boundedFinite(FuzzedDataProvider data) {
        long bits = data.consumeInt();
        double v = (bits % 2000001) / 1000.0;
        return v;
    }

    private static boolean approximatelyEqual(double a, double b) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return false;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= 1.0e-9 * scale;
    }

    private static Coefficients computeCoefficients(WeightedObservedPoint[] observations) {
        if (observations == null || observations.length < 4) {
            return null;
        }

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
            if (dx == 0.0 || !Double.isFinite(dx)) {
                return null;
            }
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

        if (!Double.isFinite(c1) || !Double.isFinite(c2) || !Double.isFinite(c3) || c3 == 0.0) {
            return null;
        }

        Coefficients coeffs = new Coefficients();
        coeffs.c1 = c1;
        coeffs.c2 = c2;
        coeffs.c3 = c3;
        return coeffs;
    }

    private static final class Coefficients {
        double c1;
        double c2;
        double c3;
    }
}