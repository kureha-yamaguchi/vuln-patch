package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        long seedBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double x = Double.longBitsToDouble(seedBits);

        for (int i = 0; i < n; i++) {
            long weightBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double weight = Double.longBitsToDouble(weightBits);
            double y = Double.longBitsToDouble(yBits);

            switch (data.consumeInt(0, 8)) {
                case 0:
                    break;
                case 1:
                    x = x + 1.0;
                    break;
                case 2:
                    x = x - 1.0;
                    break;
                case 3:
                    x = x + (double) data.consumeByte();
                    break;
                case 4:
                    x = x - (double) data.consumeByte();
                    break;
                case 5:
                    x = 0.0;
                    break;
                case 6:
                    x = -0.0;
                    break;
                case 7:
                    x = (double) data.consumeInt();
                    break;
                default:
                    long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    x = Double.longBitsToDouble(xb);
                    break;
            }

            if (data.consumeBoolean()) {
                y = x;
            } else if (data.consumeBoolean()) {
                y = -x;
            } else if (data.consumeBoolean()) {
                y = 0.0;
            } else if (data.consumeBoolean()) {
                y = (double) data.consumeInt();
            }

            if (data.consumeBoolean()) {
                weight = 1.0;
            } else if (data.consumeBoolean()) {
                weight = 0.0;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);

            switch (data.consumeInt(0, 7)) {
                case 0:
                    break;
                case 1:
                    x = x;
                    break;
                case 2:
                    x = x + 0.0;
                    break;
                case 3:
                    x = x - 0.0;
                    break;
                case 4:
                    x = x + (double) data.consumeByte();
                    break;
                case 5:
                    x = x - (double) data.consumeByte();
                    break;
                case 6:
                    x = -x;
                    break;
                default:
                    x = (double) data.consumeInt();
                    break;
            }
        }

        if (data.consumeBoolean()) {
            for (int i = 0, j = observations.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (data.consumeBoolean() && observations.length > 1) {
            for (int i = 0; i < observations.length; i++) {
                int j = data.consumeInt(0, observations.length - 1);
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (data.consumeBoolean() && observations.length > 0) {
            int src = data.consumeInt(0, observations.length - 1);
            int dst = data.consumeInt(0, observations.length - 1);
            observations[dst] = observations[src];
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();
    }
}