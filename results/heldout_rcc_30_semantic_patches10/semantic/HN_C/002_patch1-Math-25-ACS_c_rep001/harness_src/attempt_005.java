package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int n = data.consumeInt(0, 16);
        final WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        final int xMode = data.consumeInt(0, 5);
        final int yMode = data.consumeInt(0, 5);

        double prevX = 0.0;
        double prevY = 0.0;

        for (int i = 0; i < n; i++) {
            double x;
            switch (xMode) {
                case 0: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    x = Double.longBitsToDouble(bits);
                    break;
                }
                case 1: {
                    if (i == 0) {
                        prevX = data.consumeInt(-10, 10);
                    } else {
                        prevX += data.consumeInt(-1, 3);
                    }
                    x = prevX;
                    break;
                }
                case 2: {
                    if (i == 0) {
                        prevX = data.consumeInt(-10, 10);
                    } else {
                        prevX -= data.consumeInt(-1, 3);
                    }
                    x = prevX;
                    break;
                }
                case 3: {
                    x = 0.0;
                    break;
                }
                case 4: {
                    x = i;
                    if (data.consumeBoolean()) {
                        x = -x;
                    }
                    break;
                }
                default: {
                    if (i == 0 || data.consumeBoolean()) {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        prevX = Double.longBitsToDouble(bits);
                    }
                    x = prevX;
                    break;
                }
            }

            double y;
            switch (yMode) {
                case 0: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    y = Double.longBitsToDouble(bits);
                    break;
                }
                case 1: {
                    y = data.consumeInt(-1000, 1000);
                    break;
                }
                case 2: {
                    if (i == 0) {
                        prevY = data.consumeInt(-10, 10);
                    } else {
                        prevY += data.consumeInt(-20, 20);
                    }
                    y = prevY;
                    break;
                }
                case 3: {
                    y = (i % 2 == 0 ? 1.0 : -1.0) * data.consumeInt(0, 1000);
                    break;
                }
                case 4: {
                    if (data.consumeBoolean()) {
                        y = Double.NaN;
                    } else {
                        y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    }
                    break;
                }
                default: {
                    y = 0.0;
                    break;
                }
            }

            double weight;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    weight = data.consumeInt(-10, 10);
                    break;
                case 1:
                    weight = 0.0;
                    break;
                case 2:
                    weight = 1.0;
                    break;
                case 3:
                    weight = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    weight = Double.longBitsToDouble(bits);
                    break;
                }
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();
    }
}