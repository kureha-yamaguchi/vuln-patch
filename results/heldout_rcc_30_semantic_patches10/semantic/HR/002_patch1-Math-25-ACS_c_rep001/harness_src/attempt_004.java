package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] MATH844_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    private static WeightedObservedPoint[] buildPoints(double xShift) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[MATH844_Y.length];
        for (int i = 0; i < MATH844_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, xShift + i, MATH844_Y[i]);
        }
        return points;
    }

    private static boolean parameterGuesserThrowsMathIllegalState(WeightedObservedPoint[] points) {
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            return false;
        } catch (MathIllegalStateException expected) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean publicFitThrowsMathIllegalState(WeightedObservedPoint[] points) {
        try {
            HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < points.length; i++) {
                fitter.addObservedPoint(points[i]);
            }
            fitter.fit();
            return false;
        } catch (MathIllegalStateException expected) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        WeightedObservedPoint[] seedPoints = buildPoints(0.0);

        boolean seedGuesserThrows = parameterGuesserThrowsMathIllegalState(seedPoints);
        if (!seedGuesserThrows) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math844-seed] semantic mismatch: HarmonicFitter.ParameterGuesser.guess() on the exact Math844 test input did not throw MathIllegalStateException"
            );
        }

        boolean seedPublicFitThrows = publicFitThrowsMathIllegalState(seedPoints);
        if (!seedPublicFitThrows) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:public-fit-seed] semantic mismatch: HarmonicFitter.fit() on the exact Math844 test input did not throw MathIllegalStateException"
            );
        }

        int shiftInt = data.consumeInt(-1000, 1000);
        if (shiftInt == 0) {
            shiftInt = 1;
        }
        WeightedObservedPoint[] translatedPoints = buildPoints((double) shiftInt);

        boolean translatedGuesserThrows = parameterGuesserThrowsMathIllegalState(translatedPoints);
        try {
            HarmonicFitter.ParameterGuesser translatedGuesser = new HarmonicFitter.ParameterGuesser(translatedPoints);
            double[] translatedGuess = translatedGuesser.guess();
            if (translatedGuess != null) {
                return;
            }
        } catch (Throwable t) {
        }

        /* Documented/code-visible guarantee behind this check:
         * guess() first calls sortObservations() then guessAOmega(); inside guessAOmega,
         * only dx and (currentX - startX) are used. Translating every abscissa by the same
         * constant preserves both quantities exactly, so a correct implementation must make
         * the same throw/no-throw decision on the translated sample as on the seed sample.
         * A band-aid fix that special-cases only the literal seed coordinates breaks this.
         */
        if (seedGuesserThrows != translatedGuesserThrows) {
            throw new RuntimeException(
                "[oracle:x-translation-exn] metamorphic violation: uniform x-translation must preserve the guess() throw/no-throw outcome inputShift="
                    + shiftInt + " seedThrows=" + seedGuesserThrows + " translatedThrows=" + translatedGuesserThrows
            );
        }

        boolean translatedPublicFitThrows = publicFitThrowsMathIllegalState(translatedPoints);
        /* Same guarantee through the real public API:
         * HarmonicFitter.fit() with no initial guess reaches ParameterGuesser on the stored
         * observations. Since the translated observations differ only by a uniform abscissa
         * shift, fit() must preserve the same exception outcome as the seed.
         */
        if (seedPublicFitThrows != translatedPublicFitThrows) {
            throw new RuntimeException(
                "[oracle:public-fit-x-translation] metamorphic violation: uniform x-translation must preserve HarmonicFitter.fit() throw/no-throw outcome inputShift="
                    + shiftInt + " seedThrows=" + seedPublicFitThrows + " translatedThrows=" + translatedPublicFitThrows
            );
        }
    }
}