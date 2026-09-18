package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        int xMode = data.consumeInt(0, 5);
        double currentX = 0.0;

        for (int i = 0; i < n; i++) {
            long weightBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double weight;
            if (data.consumeBoolean()) {
                weight = Double.longBitsToDouble(weightBits);
            } else {
                weight = data.consumeInt();
            }

            double y;
            if (data.consumeBoolean()) {
                y = Double.longBitsToDouble(yBits);
            } else {
                y = data.consumeInt();
            }

            double x;
            switch (xMode) {
                case 0:
                    x = 0.0;
                    break;
                case 1:
                    currentX += data.consumeInt(-2, 2);
                    x = currentX;
                    break;
                case 2:
                    currentX -= data.consumeInt(-2, 2);
                    x = currentX;
                    break;
                case 3:
                    long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    x = Double.longBitsToDouble(xBits);
                    break;
                case 4:
                    x = i;
                    break;
                default:
                    currentX += data.consumeInt(0, 1);
                    x = currentX;
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (data.consumeBoolean()) {
            for (int i = 0, j = observations.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (data.consumeBoolean() && observations.length > 1) {
            int src = data.consumeInt(0, observations.length - 1);
            int dst = data.consumeInt(0, observations.length - 1);
            observations[dst] = observations[src];
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();
    }
}