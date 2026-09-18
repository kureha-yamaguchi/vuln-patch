package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 64);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        int mode = data.consumeInt(0, 7);
        double baseX = data.consumeInt();
        double currentX = baseX;

        for (int i = 0; i < n; i++) {
            double weight = data.consumeInt();
            double y;
            switch (mode & 3) {
                case 0:
                    y = data.consumeInt();
                    break;
                case 1:
                    y = (i == 0 ? data.consumeInt() : observations[i - 1].getY());
                    break;
                case 2:
                    y = (i % 2 == 0 ? data.consumeInt() : -data.consumeInt());
                    break;
                default:
                    y = 0.0;
                    break;
            }

            double x;
            switch (mode) {
                case 0:
                    x = data.consumeInt();
                    break;
                case 1:
                    x = currentX;
                    currentX += data.consumeInt(-3, 3);
                    break;
                case 2:
                    x = baseX;
                    break;
                case 3:
                    x = i;
                    break;
                case 4:
                    x = -i;
                    break;
                case 5:
                    x = (i / 2);
                    break;
                case 6:
                    x = (i == 0) ? baseX : observations[i - 1].getX();
                    break;
                default:
                    x = currentX;
                    currentX += data.consumeBoolean() ? 0 : data.consumeInt(-1, 1);
                    break;
            }

            if (data.consumeBoolean() && i > 0) {
                if (data.consumeBoolean()) {
                    x = observations[i - 1].getX();
                }
                if (data.consumeBoolean()) {
                    y = observations[i - 1].getY();
                }
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guess = guesser.guess();

        if (data.consumeBoolean()) {
            WeightedObservedPoint[] reversed = new WeightedObservedPoint[observations.length];
            for (int i = 0; i < observations.length; i++) {
                reversed[i] = observations[observations.length - 1 - i];
            }
            new HarmonicFitter.ParameterGuesser(reversed).guess();
        }

        if (data.consumeBoolean() && guess != null && guess.length == 3 && observations.length > 0) {
            WeightedObservedPoint[] derived = new WeightedObservedPoint[observations.length];
            double a = guess[0];
            double omega = guess[1];
            double phi = guess[2];
            for (int i = 0; i < observations.length; i++) {
                double x = observations[i].getX();
                double y = a * Math.cos(omega * x + phi);
                if (data.consumeBoolean()) {
                    y += observations[i].getY();
                }
                derived[i] = new WeightedObservedPoint(observations[i].getWeight(), x, y);
            }
            new HarmonicFitter.ParameterGuesser(derived).guess();
        }
    }
}