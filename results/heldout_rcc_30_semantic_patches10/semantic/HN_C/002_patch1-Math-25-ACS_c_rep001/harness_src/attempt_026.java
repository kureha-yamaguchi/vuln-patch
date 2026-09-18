package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[len];

        double prevX = 0.0;
        for (int i = 0; i < len; i++) {
            int modeW = data.consumeInt(0, 9);
            double weight;
            switch (modeW) {
                case 0:
                    weight = 0.0;
                    break;
                case 1:
                    weight = -0.0;
                    break;
                case 2:
                    weight = 1.0;
                    break;
                case 3:
                    weight = -1.0;
                    break;
                case 4:
                    weight = Integer.MAX_VALUE;
                    break;
                case 5:
                    weight = Integer.MIN_VALUE;
                    break;
                case 6:
                    weight = Double.NaN;
                    break;
                case 7:
                    weight = Double.POSITIVE_INFINITY;
                    break;
                case 8:
                    weight = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    weight = ((double) data.consumeInt()) / ((data.consumeInt(1, 1024)));
                    break;
            }

            int modeX = data.consumeInt(0, 13);
            double x;
            switch (modeX) {
                case 0:
                    x = 0.0;
                    break;
                case 1:
                    x = -0.0;
                    break;
                case 2:
                    x = prevX;
                    break;
                case 3:
                    x = prevX + 1.0;
                    break;
                case 4:
                    x = prevX - 1.0;
                    break;
                case 5:
                    x = Integer.MAX_VALUE;
                    break;
                case 6:
                    x = Integer.MIN_VALUE;
                    break;
                case 7:
                    x = Double.NaN;
                    break;
                case 8:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 9:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 10:
                    x = Math.PI;
                    break;
                case 11:
                    x = -Math.PI;
                    break;
                case 12:
                    x = ((double) data.consumeInt()) / ((double) data.consumeInt(1, 1024));
                    break;
                default:
                    x = prevX + (((double) data.consumeInt(-4, 4)) / ((double) data.consumeInt(1, 8)));
                    break;
            }
            prevX = x;

            int modeY = data.consumeInt(0, 12);
            double y;
            switch (modeY) {
                case 0:
                    y = 0.0;
                    break;
                case 1:
                    y = -0.0;
                    break;
                case 2:
                    y = 1.0;
                    break;
                case 3:
                    y = -1.0;
                    break;
                case 4:
                    y = Integer.MAX_VALUE;
                    break;
                case 5:
                    y = Integer.MIN_VALUE;
                    break;
                case 6:
                    y = Double.NaN;
                    break;
                case 7:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 8:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 9:
                    y = Math.E;
                    break;
                case 10:
                    y = -Math.E;
                    break;
                case 11:
                    y = ((double) data.consumeInt()) / ((double) data.consumeInt(1, 1024));
                    break;
                default:
                    y = Math.sin((double) data.consumeInt()) * data.consumeInt();
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guessed = guesser.guess();

        if (data.consumeBoolean() && guessed != null && guessed.length == 3) {
            WeightedObservedPoint[] mutated = new WeightedObservedPoint[observations.length];
            for (int i = 0; i < observations.length; i++) {
                WeightedObservedPoint p = observations[observations.length - 1 - i];
                double x = p.getX();
                double y = p.getY();
                if (data.consumeBoolean()) {
                    x = x + guessed[1];
                }
                if (data.consumeBoolean()) {
                    y = y + guessed[0];
                }
                mutated[i] = new WeightedObservedPoint(p.getWeight(), x, y);
            }
            new HarmonicFitter.ParameterGuesser(mutated).guess();
        }
    }
}