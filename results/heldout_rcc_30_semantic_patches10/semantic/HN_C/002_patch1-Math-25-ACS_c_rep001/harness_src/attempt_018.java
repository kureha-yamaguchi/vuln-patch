package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        int xLayout = data.consumeInt(0, 7);
        int yLayout = data.consumeInt(0, 7);

        long seedXBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long seedYBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long stepBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        double seedX = Double.longBitsToDouble(seedXBits);
        double seedY = Double.longBitsToDouble(seedYBits);
        double step = Double.longBitsToDouble(stepBits);

        if (step == 0.0 && data.consumeBoolean()) {
            step = data.consumeByte();
        }

        for (int i = 0; i < n; i++) {
            long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double rawX = Double.longBitsToDouble(xBits);
            double rawY = Double.longBitsToDouble(yBits);
            double rawW = Double.longBitsToDouble(wBits);

            double x;
            switch (xLayout) {
                case 0:
                    x = rawX;
                    break;
                case 1:
                    x = seedX;
                    break;
                case 2:
                    x = seedX + i * step;
                    break;
                case 3:
                    x = seedX - i * step;
                    break;
                case 4:
                    x = seedX + (i % 2) * step;
                    break;
                case 5:
                    x = i == 0 ? seedX : observations[i - 1].getX();
                    break;
                case 6:
                    x = i;
                    break;
                default:
                    x = -i;
                    break;
            }

            double y;
            switch (yLayout) {
                case 0:
                    y = rawY;
                    break;
                case 1:
                    y = seedY;
                    break;
                case 2:
                    y = seedY + i;
                    break;
                case 3:
                    y = seedY - i;
                    break;
                case 4:
                    y = (i % 2 == 0) ? rawY : -rawY;
                    break;
                case 5:
                    y = x;
                    break;
                case 6:
                    y = -x;
                    break;
                default:
                    y = 0.0;
                    break;
            }

            double weight;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    weight = rawW;
                    break;
                case 1:
                    weight = 1.0;
                    break;
                case 2:
                    weight = 0.0;
                    break;
                case 3:
                    weight = -1.0;
                    break;
                case 4:
                    weight = data.consumeByte();
                    break;
                default:
                    weight = seedY;
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] params = guesser.guess();

        if (data.consumeBoolean() && params != null && params.length == 3) {
            double sum = params[0] + params[1] + params[2];
            if (sum == Double.POSITIVE_INFINITY) {
                throw new RuntimeException("unreachable");
            }
        }
    }
}