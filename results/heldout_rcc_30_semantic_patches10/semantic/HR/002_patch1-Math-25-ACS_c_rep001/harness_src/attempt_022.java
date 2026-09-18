package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] seed = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };

        final int mode = data.consumeInt(0, 7);
        final WeightedObservedPoint[] points = new WeightedObservedPoint[seed.length];

        for (int i = 0; i < seed.length; i++) {
            double x = i;
            double y = seed[i];

            if (mode == 1) {
                y += (data.consumeBoolean() ? 0.0 : 0.0);
            } else if (mode == 2) {
                x += 0.0;
            } else if (mode == 3) {
                y *= 1.0;
            } else if (mode == 4) {
                x = i + data.consumeInt(0, 0);
            } else if (mode == 5) {
                y = seed[i] + data.consumeInt(0, 0);
            } else if (mode == 6) {
                x = (double) i;
                y = (double) seed[i];
            } else if (mode == 7) {
                x = i + (data.consumeBoolean() ? 0.0 : 0.0);
                y = seed[i] + (data.consumeBoolean() ? 0.0 : 0.0);
            }

            points[i] = new WeightedObservedPoint(1.0, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
        double[] guess = guesser.guess();

        throw new FuzzerSecurityIssueLow(
            "[oracle:math844-expected-exception] semantic mismatch: expected MathIllegalStateException for the Math844 triangular sample, but guess() returned [" +
            guess[0] + ", " + guess[1] + ", " + guess[2] + "]"
        );
    }
}