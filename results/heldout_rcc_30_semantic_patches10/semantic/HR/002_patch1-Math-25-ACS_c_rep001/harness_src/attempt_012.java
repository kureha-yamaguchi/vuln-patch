package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final double[] MATH844_Y = new double[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkWeightedObservedPointGettersMatchConstructor(data);
        checkNegationBoundaryConsistency(data);
        checkMath844ReturnIsNotDegenerateModel();
    }

    private static void checkWeightedObservedPointGettersMatchConstructor(FuzzedDataProvider data) {
        double w = finiteModerateDouble(data);
        double x = finiteModerateDouble(data);
        double y = finiteModerateDouble(data);

        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        boolean mismatch =
                Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y);

        if (mismatch) {
            throw new FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_match_constructor violated: expected=(" +
                    w + "," + x + "," + y + ") actual=(" +
                    p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
        }
    }

    private static void checkNegationBoundaryConsistency(FuzzedDataProvider data) {
        int perturbIndex = data.consumeInt(0, MATH844_Y.length - 1);
        int numerator = data.consumeInt(-1000, 1000);
        int denominator = data.consumeInt(1, 1000);
        double epsilon = ((double) numerator) / ((double) denominator);

        WeightedObservedPoint[] original = buildPerturbedPoints(perturbIndex, epsilon, false);
        WeightedObservedPoint[] negated = buildPerturbedPoints(perturbIndex, epsilon, true);

        HarmonicFitter.ParameterGuesser g1;
        HarmonicFitter.ParameterGuesser g2;
        try {
            g1 = new HarmonicFitter.ParameterGuesser(original);
            g2 = new HarmonicFitter.ParameterGuesser(negated);
        } catch (Throwable t) {
            return;
        }

        double[] r1;
        double[] r2;
        try {
            r1 = g1.guess();
        } catch (Throwable t) {
            return;
        }
        try {
            r2 = g2.guess();
        } catch (Throwable t) {
            return;
        }

        if (r1 == null || r2 == null || r1.length < 2 || r2.length < 2) {
            return;
        }

        double a1 = r1[0];
        double w1 = r1[1];
        double a2 = r2[0];
        double w2 = r2[1];

        /* Soundness: negating every observed y-value corresponds to the same harmonic family
           with a phase shift of pi; amplitude and angular frequency must therefore be unchanged
           for any correct parameter guesser. This specifically probes both sides of the patched
           c2==0.0 boundary by perturbing one seed point around the MATH-844 sample. */
        boolean amplitudeMismatch = !close(a1, a2);
        boolean omegaMismatch = !close(w1, w2);

        if (amplitudeMismatch || omegaMismatch) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:negation-boundary] metamorphic violation: negating all y values changed amplitude/frequency" +
                    " perturbIndex=" + perturbIndex +
                    " epsilon=" + epsilon +
                    " amplitude1=" + a1 +
                    " amplitude2=" + a2 +
                    " omega1=" + w1 +
                    " omega2=" + w2);
        }
    }

    private static void checkMath844ReturnIsNotDegenerateModel() {
        WeightedObservedPoint[] points = buildSeedPoints();

        HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        double[] guess;
        try {
            guess = guesser.guess();
        } catch (MathIllegalStateException expected) {
            return;
        } catch (Throwable t) {
            return;
        }

        if (guess == null || guess.length < 3) {
            return;
        }

        double a = guess[0];
        double omega = guess[1];
        double phi = guess[2];

        /* Contract/post-condition: guess() estimates coefficients of a HarmonicOscillator from the
           observations. For the trusted MATH-844 sample, the documented correct behavior is to reject
           the ill-conditioned input with MathIllegalStateException; a throw-deleting patch can instead
           return a degenerate oscillator. We therefore check an independent observable: if parameters
           are returned for this non-constant sample, the implied oscillator must itself be non-constant
           over x=0 and x=1 where observed y values are 0 and 1. Returning a constant/degenerate model
           is a semantic error even if the exception symptom was masked. */
        HarmonicOscillator oscillator;
        double v0;
        double v1;
        try {
            oscillator = new HarmonicOscillator(a, omega, phi);
            v0 = oscillator.value(0.0);
            v1 = oscillator.value(1.0);
        } catch (Throwable t) {
            return;
        }

        boolean degenerate;
        if (Double.isNaN(v0) || Double.isNaN(v1)) {
            degenerate = true;
        } else if (!Double.isFinite(v0) || !Double.isFinite(v1)) {
            degenerate = true;
        } else {
            degenerate = close(v0, v1);
        }

        if (degenerate) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math844-degenerate-return] semantic mismatch: MATH-844 sample should not yield a degenerate returned oscillator" +
                    " a=" + a +
                    " omega=" + omega +
                    " phi=" + phi +
                    " valueAt0=" + v0 +
                    " valueAt1=" + v1 +
                    " observedY0=0.0 observedY1=1.0");
        }
    }

    private static WeightedObservedPoint[] buildSeedPoints() {
        WeightedObservedPoint[] points = new WeightedObservedPoint[MATH844_Y.length];
        for (int i = 0; i < MATH844_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, MATH844_Y[i]);
        }
        return points;
    }

    private static WeightedObservedPoint[] buildPerturbedPoints(int perturbIndex, double epsilon, boolean negateY) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[MATH844_Y.length];
        for (int i = 0; i < MATH844_Y.length; i++) {
            double y = MATH844_Y[i];
            if (i == perturbIndex) {
                y += epsilon;
            }
            if (negateY) {
                y = -y;
            }
            points[i] = new WeightedObservedPoint(1.0, i, y);
        }
        return points;
    }

    private static double finiteModerateDouble(FuzzedDataProvider data) {
        int whole = data.consumeInt(-1_000_000, 1_000_000);
        int frac = data.consumeInt(0, 999_999);
        double sign = data.consumeBoolean() ? 1.0 : -1.0;
        return sign * (whole + (frac / 1_000_000.0));
    }

    private static boolean close(double a, double b) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= 1.0e-8 * scale;
    }
}