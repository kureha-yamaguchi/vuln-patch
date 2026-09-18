package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int n = data.consumeInt(0, 16);
        final WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        final int xPattern = data.consumeInt(0, 5);
        double sharedX = 0.0;
        if (xPattern == 0 || xPattern == 1) {
            sharedX = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        }

        double runningX = 0.0;
        for (int i = 0; i < n; i++) {
            double weight;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    weight = data.consumeInt(-4, 4);
                    break;
                case 1:
                    int wd = data.consumeInt(-8, 8);
                    weight = wd == 0 ? 0.0 : ((double) data.consumeInt()) / wd;
                    break;
                case 2:
                    weight = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 3:
                    weight = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default:
                    weight = Double.NaN;
                    break;
            }

            double x;
            switch (xPattern) {
                case 0:
                    x = sharedX;
                    break;
                case 1:
                    x = (i == 0) ? sharedX : (data.consumeBoolean() ? sharedX : sharedX + data.consumeInt(-1, 1));
                    break;
                case 2:
                    runningX += data.consumeInt(-3, 3);
                    x = runningX;
                    break;
                case 3:
                    x = n - i;
                    break;
                case 4:
                    x = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                default:
                    int xd = data.consumeInt(-8, 8);
                    x = xd == 0 ? 0.0 : ((double) data.consumeInt()) / xd;
                    break;
            }

            double y;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    y = data.consumeInt(-16, 16);
                    break;
                case 1:
                    int yd = data.consumeInt(-16, 16);
                    y = yd == 0 ? 0.0 : ((double) data.consumeInt()) / yd;
                    break;
                case 2:
                    y = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 3:
                    y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    y = Double.NaN;
                    break;
                case 5:
                    y = (i % 2 == 0 ? 1.0 : -1.0) * data.consumeInt(-8, 8);
                    break;
                default:
                    y = x;
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (data.consumeBoolean()) {
            for (int i = 0; i < observations.length / 2; i++) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[observations.length - 1 - i];
                observations[observations.length - 1 - i] = tmp;
            }
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guess = guesser.guess();

        if (data.consumeBoolean()) {
            HarmonicFitter.ParameterGuesser guesser2 = new HarmonicFitter.ParameterGuesser(observations.clone());
            double[] guess2 = guesser2.guess();
            if (guess.length + guess2.length == 7) {
                throw new AssertionError();
            }
        }
    }
}