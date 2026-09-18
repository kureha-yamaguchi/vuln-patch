package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        int mode = data.consumeInt(0, 7);
        double baseX = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double step = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double currentX = baseX;

        for (int i = 0; i < n; i++) {
            double x;
            switch (mode) {
                case 0:
                    x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 1:
                    x = currentX;
                    currentX += step;
                    break;
                case 2:
                    x = baseX;
                    break;
                case 3:
                    x = i;
                    break;
                case 4:
                    x = n - i;
                    break;
                case 5:
                    x = (i == 0 || data.consumeBoolean()) ? baseX : currentX;
                    currentX += step;
                    break;
                case 6:
                    x = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                default:
                    x = currentX;
                    currentX += (data.consumeBoolean() ? 1.0 : -1.0) * (double) data.consumeInt(-3, 3);
                    break;
            }

            double y;
            int yMode = data.consumeInt(0, 6);
            if (yMode == 0) {
                y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            } else if (yMode == 1) {
                y = data.consumeInt();
            } else if (yMode == 2) {
                y = i;
            } else if (yMode == 3) {
                y = -i;
            } else if (yMode == 4) {
                y = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
            } else if (yMode == 5) {
                y = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : 0.0;
            } else {
                y = Math.sin(i) * data.consumeInt(-10, 10);
            }

            double weight;
            int wMode = data.consumeInt(0, 4);
            if (wMode == 0) {
                weight = 1.0;
            } else if (wMode == 1) {
                weight = 0.0;
            } else if (wMode == 2) {
                weight = -1.0;
            } else if (wMode == 3) {
                weight = data.consumeInt(-1000, 1000);
            } else {
                weight = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (data.consumeBoolean()) {
            new HarmonicFitter.ParameterGuesser(observations).guess();
        } else {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
            double[] guess = guesser.guess();
            if (data.consumeBoolean() && guess.length == 3) {
                double a = guess[0];
                double omega = guess[1];
                double phi = guess[2];
                if (a == 0.0 || omega == 0.0 || phi == 0.0) {
                    new HarmonicFitter.ParameterGuesser(observations).guess();
                }
            }
        }
    }
}