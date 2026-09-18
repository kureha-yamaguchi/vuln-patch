package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final double[] SEED_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int pivot = data.consumeInt(0, SEED_Y.length - 1);
        int mode = data.consumeInt(0, 2);
        int mag = data.consumeInt(0, 4);

        double delta;
        if (mode == 0) {
            delta = 0.0;
        } else if (mode == 1) {
            delta = Math.scalb(1.0, -(20 + mag));
        } else {
            delta = -Math.scalb(1.0, -(20 + mag));
        }

        WeightedObservedPoint[] pointsA = buildPoints(delta, pivot, false);
        WeightedObservedPoint[] pointsB = buildPoints(delta, pivot, true);

        // Lifted ground-truth oracle from HarmonicFitterTest.testMath844:
        // for the exact seed (delta == 0), guess() must throw MathIllegalStateException.
        if (delta == 0.0) {
            try {
                new HarmonicFitter.ParameterGuesser(pointsA).guess();
                throw new FuzzerSecurityIssueLow(
                    "[oracle:math844-exact-seed] semantic mismatch: expected MathIllegalStateException for the Math844 seed, but guess() returned normally");
            } catch (MathIllegalStateException expected) {
                // expected on patched builds
            } catch (Throwable other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
                return;
            }
        }

        checkValueIdentityInvariance(pointsA, pointsB);
    }

    private static WeightedObservedPoint[] buildPoints(double delta, int pivot, boolean varyWeights) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[SEED_Y.length];
        for (int i = 0; i < SEED_Y.length; i++) {
            double y = SEED_Y[i];
            if (i == pivot) {
                y += delta;
            }
            double weight = varyWeights ? (1.0 + (i % 5)) : 1.0;
            points[i] = new WeightedObservedPoint(weight, i, y);
        }
        return points;
    }

    private static void checkValueIdentityInvariance(WeightedObservedPoint[] pointsA,
                                                     WeightedObservedPoint[] pointsB) {
        double[] guessA;
        double[] guessB;
        Throwable exA = null;
        Throwable exB = null;

        try {
            guessA = new HarmonicFitter.ParameterGuesser(pointsA).guess();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            exA = t;
            guessA = null;
        }

        try {
            guessB = new HarmonicFitter.ParameterGuesser(pointsB).guess();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            exB = t;
            guessB = null;
        }

        // Contract justification:
        // WeightedObservedPoint is immutable, and ParameterGuesser reads only getX()/getY()
        // from the observations shown in guessAOmega/guessPhi. Therefore replacing observations
        // by fresh instances with the same x/y values must not change either the rejection
        // behaviour or the returned coefficients. A throw-deleting or instance-sensitive patch
        // would violate this equivalence.
        if ((exA == null) != (exB == null)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:value-identity-invariance] metamorphic violation: equivalent observations disagreed on throwing, exA="
                    + className(exA) + " exB=" + className(exB));
        }

        if (exA != null) {
            if (!exA.getClass().equals(exB.getClass())) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:value-identity-invariance] metamorphic violation: equivalent observations threw different exception classes, exA="
                        + exA.getClass().getName() + " exB=" + exB.getClass().getName());
            }
            return;
        }

        if (guessA.length != guessB.length) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:value-identity-invariance] metamorphic violation: equivalent observations returned arrays of different lengths, lhs="
                    + guessA.length + " rhs=" + guessB.length);
        }

        for (int i = 0; i < guessA.length; i++) {
            long aBits = Double.doubleToLongBits(guessA[i]);
            long bBits = Double.doubleToLongBits(guessB[i]);
            if (aBits != bBits) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:value-identity-invariance] metamorphic violation: equivalent observations returned different coefficient at index "
                        + i + " lhs=" + guessA[i] + " rhs=" + guessB[i]);
            }
        }
    }

    private static String className(Throwable t) {
        return t == null ? "null" : t.getClass().getName();
    }
}