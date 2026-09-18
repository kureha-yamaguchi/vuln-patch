package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double xOffset = data.consumeInt(-3, 3);

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
            points[i] = new WeightedObservedPoint(1.0, i + xOffset, y[i]);
        }

        final HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);

        try {
            final double[] guess = guesser.guess();

            /*
             * Ground-truth oracle lifted from HarmonicFitterTest.testMath844:
             * for this triangular-wave sample, the real API call guesser.guess()
             * must throw MathIllegalStateException. If it returns instead, the
             * buggy build has reached the patched lines and silently produced a
             * wrong result.
             */
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math844-exception] semantic mismatch: expected MathIllegalStateException but guess() returned "
                    + "a=" + guess[0] + ", omega=" + guess[1] + ", phi=" + guess[2]
                    + ", xOffset=" + xOffset);
        } catch (MathIllegalStateException expectedOnFixedBuild) {
            return;
        }
    }
}