package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 7);
        int len;
        if ((mode & 1) == 0) {
            len = data.consumeInt(4, 20);
        } else {
            len = data.consumeInt(0, 20);
        }

        WeightedObservedPoint[] observations = new WeightedObservedPoint[len];

        double baseX;
        if (data.consumeBoolean()) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            baseX = Double.longBitsToDouble(bits);
        } else {
            baseX = data.consumeInt(-1000, 1000);
        }

        double currentX = baseX;
        for (int i = 0; i < len; i++) {
            double weight;
            if (data.consumeBoolean()) {
                long wbits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                weight = Double.longBitsToDouble(wbits);
            } else {
                weight = data.consumeInt(-10, 10);
            }

            double y;
            if (data.consumeBoolean()) {
                long ybits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                y = Double.longBitsToDouble(ybits);
            } else {
                y = data.consumeInt(-1000, 1000);
            }

            switch (mode) {
                case 0:
                    currentX = baseX + i;
                    break;
                case 1:
                    currentX = baseX;
                    break;
                case 2:
                    currentX = baseX + (i / 2);
                    break;
                case 3:
                    currentX = baseX - i;
                    break;
                case 4:
                    currentX = baseX + data.consumeInt(-2, 2);
                    break;
                case 5:
                    currentX = baseX + (long) i * data.consumeInt(-3, 3);
                    break;
                case 6:
                    if (data.consumeBoolean()) {
                        currentX = baseX + i;
                    } else {
                        long xbits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        currentX = Double.longBitsToDouble(xbits);
                    }
                    break;
                default:
                    long xbits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    currentX = Double.longBitsToDouble(xbits);
                    break;
            }

            if (data.consumeBoolean()) {
                y = currentX;
            } else if (data.consumeBoolean()) {
                y = -currentX;
            }

            observations[i] = new WeightedObservedPoint(weight, currentX, y);
        }

        if (data.consumeBoolean()) {
            for (int i = 0; i < len / 2; i++) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[len - 1 - i];
                observations[len - 1 - i] = tmp;
            }
        }

        if (data.consumeBoolean()) {
            for (int i = 0; i < len; i++) {
                for (int j = i + 1; j < len; j++) {
                    if (observations[j].getX() < observations[i].getX()) {
                        WeightedObservedPoint tmp = observations[i];
                        observations[i] = observations[j];
                        observations[j] = tmp;
                    }
                }
            }
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();
    }
}