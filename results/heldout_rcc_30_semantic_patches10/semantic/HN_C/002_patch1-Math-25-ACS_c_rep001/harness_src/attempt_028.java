package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        double lastX = 0.0;
        double lastY = 0.0;

        for (int i = 0; i < n; i++) {
            double weight;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    weight = data.consumeInt();
                    break;
                case 1:
                    weight = data.consumeByte();
                    break;
                case 2:
                    weight = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 3:
                    weight = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    weight = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    weight = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
                case 6:
                    weight = (double) data.consumeInt() / (double) data.consumeByte();
                    break;
                default:
                    weight = i == 0 ? 1.0 : observations[i - 1].getWeight();
                    break;
            }

            double x;
            switch (data.consumeInt(0, 10)) {
                case 0:
                    x = data.consumeInt();
                    break;
                case 1:
                    x = data.consumeByte();
                    break;
                case 2:
                    x = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
                case 3:
                    x = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    x = Double.NaN;
                    break;
                case 5:
                    x = i == 0 ? 0.0 : lastX;
                    break;
                case 6:
                    x = i == 0 ? 0.0 : lastX + data.consumeByte();
                    break;
                case 7:
                    x = i == 0 ? 0.0 : lastX - data.consumeByte();
                    break;
                case 8:
                    x = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 9:
                    x = (double) data.consumeInt() / (double) data.consumeByte();
                    break;
                default:
                    x = i;
                    break;
            }

            double y;
            switch (data.consumeInt(0, 10)) {
                case 0:
                    y = data.consumeInt();
                    break;
                case 1:
                    y = data.consumeByte();
                    break;
                case 2:
                    y = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
                case 3:
                    y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    y = Double.NaN;
                    break;
                case 5:
                    y = i == 0 ? 0.0 : lastY;
                    break;
                case 6:
                    y = i == 0 ? 0.0 : lastY + data.consumeByte();
                    break;
                case 7:
                    y = i == 0 ? 0.0 : lastY - data.consumeByte();
                    break;
                case 8:
                    y = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                case 9:
                    y = (double) data.consumeInt() / (double) data.consumeByte();
                    break;
                default:
                    y = 0.0;
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
            lastX = x;
            lastY = y;
        }

        if (observations.length > 1 && data.consumeBoolean()) {
            for (int i = 0, j = observations.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (observations.length > 1 && data.consumeBoolean()) {
            int idx0 = data.consumeInt(0, observations.length - 1);
            int idx1 = data.consumeInt(0, observations.length - 1);
            WeightedObservedPoint p0 = observations[idx0];
            WeightedObservedPoint p1 = observations[idx1];
            observations[idx1] = new WeightedObservedPoint(p1.getWeight(), p0.getX(), p1.getY());
        }

        if (observations.length > 1 && data.consumeBoolean()) {
            int idx0 = data.consumeInt(0, observations.length - 1);
            int idx1 = data.consumeInt(0, observations.length - 1);
            WeightedObservedPoint p0 = observations[idx0];
            WeightedObservedPoint p1 = observations[idx1];
            observations[idx1] = new WeightedObservedPoint(p1.getWeight(), p1.getX(), p0.getY());
        }

        if (observations.length > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, observations.length - 1);
            WeightedObservedPoint p = observations[idx];
            observations[idx] = new WeightedObservedPoint(p.getWeight(), observations[0].getX(), p.getY());
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guess = guesser.guess();

        if (guess != null && guess.length == 3 && observations.length > 0 && data.consumeBoolean()) {
            int idx = data.consumeInt(0, observations.length - 1);
            double x = observations[idx].getX();
            double a = guess[0];
            double omega = guess[1];
            double phi = guess[2];
            double value = a * Math.cos(omega * x + phi);
            if (Double.isNaN(value)) {
                throw new IllegalStateException("NaN");
            }
        }
    }
}