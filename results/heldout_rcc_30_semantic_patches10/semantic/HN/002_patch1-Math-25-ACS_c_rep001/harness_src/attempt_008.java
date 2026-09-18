package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int shift = data.consumeInt(-1000, 1000);

        final WeightedObservedPoint[] exact = triangularPoints(0.0);
        try {
            final HarmonicFitter.ParameterGuesser guesser =
                new HarmonicFitter.ParameterGuesser(exact);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException from HarmonicFitter.ParameterGuesser.guess() on the triangular sample from HarmonicFitterTest.testMath844");
        } catch (MathIllegalStateException expected) {
            // Ground-truth oracle lifted verbatim from the failing test.
        }

        try {
            final HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < exact.length; i++) {
                fitter.addObservedPoint(exact[i].getWeight(), exact[i].getX(), exact[i].getY());
            }
            fitter.fit();
            throw new FuzzerSecurityIssueLow(
                "[oracle:public-fit] semantic mismatch: expected rejection to remain reachable through HarmonicFitter.fit() for the same triangular sample");
        } catch (MathIllegalStateException expected) {
            // Real public API path: fit() computes its initial guess through ParameterGuesser.
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            // If some other rejection happens, this semantic check does not apply.
        }

        final WeightedObservedPoint[] shifted = triangularPoints((double) shift);
        try {
            final HarmonicFitter.ParameterGuesser shiftedGuesser =
                new HarmonicFitter.ParameterGuesser(shifted);
            shiftedGuesser.guess();
            throw new RuntimeException(
                "[oracle:x-shift-rejection] metamorphic violation: translating all abscissae must preserve this rejection because guessAOmega uses only relative x (currentX - startX) and xRange; inputShift="
                    + shift + " relation=reject(base)==reject(shifted) lhs=MathIllegalStateException rhs=returned");
        } catch (MathIllegalStateException expected) {
            // Trusted equivalent-input oracle: same y sequence, same spacing, only translated x.
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            // Any other exception means this relation is not applicable for this run.
        }

        final WeightedObservedPoint[] mutatedArray = triangularPoints(0.0);
        final HarmonicFitter.ParameterGuesser clonedGuesser =
            new HarmonicFitter.ParameterGuesser(mutatedArray);
        for (int i = 0; i < mutatedArray.length; i++) {
            mutatedArray[i] = new WeightedObservedPoint(1.0, shift + i, 123.0 + i);
        }
        try {
            clonedGuesser.guess();
            throw new RuntimeException(
                "[oracle:constructor-clone] metamorphic violation: ParameterGuesser constructor documents it clones the observations array, so replacing caller array slots after construction must not change the outcome; expected=MathIllegalStateException actual=returned");
        } catch (MathIllegalStateException expected) {
            // Trusted post-condition on shared state: guess() must observe the constructor-established clone.
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            // Any other exception means this relation is not applicable for this run.
        }
    }

    private static WeightedObservedPoint[] triangularPoints(double xOffset) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, xOffset + i, y[i]);
        }
        return points;
    }
}