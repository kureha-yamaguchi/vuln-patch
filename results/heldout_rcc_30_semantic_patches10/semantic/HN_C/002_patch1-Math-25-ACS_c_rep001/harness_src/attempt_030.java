package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len = data.consumeInt(4, 16);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[len];

        int xMode = data.consumeInt(0, 4);
        double currentX = 0.0;

        for (int i = 0; i < len; i++) {
            double x;
            switch (xMode) {
                case 0: {
                    int delta = data.consumeInt(-2, 2);
                    currentX += delta;
                    x = currentX;
                    break;
                }
                case 1: {
                    int delta = data.consumeInt(-2, 2);
                    currentX -= delta;
                    x = currentX;
                    break;
                }
                case 2: {
                    int raw = data.consumeInt();
                    switch (raw & 7) {
                        case 0:
                            x = 0.0;
                            break;
                        case 1:
                            x = -0.0d;
                            break;
                        case 2:
                            x = raw;
                            break;
                        case 3:
                            x = raw / 16.0;
                            break;
                        case 4:
                            x = raw * 1024.0;
                            break;
                        case 5:
                            x = raw > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                            break;
                        case 6:
                            x = Double.NaN;
                            break;
                        default:
                            x = (raw % 17);
                            break;
                    }
                    break;
                }
                case 3:
                    x = 0.0;
                    break;
                default:
                    x = (i & 1) == 0 ? 0.0 : 1.0;
                    break;
            }

            int yRaw = data.consumeInt();
            double y;
            switch (yRaw & 15) {
                case 0:
                    y = 0.0;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = yRaw;
                    break;
                case 3:
                    y = yRaw / 3.0;
                    break;
                case 4:
                    y = yRaw / 1024.0;
                    break;
                case 5:
                    y = yRaw * 1024.0;
                    break;
                case 6:
                    y = Double.NaN;
                    break;
                case 7:
                    y = yRaw > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    y = i;
                    break;
                case 9:
                    y = -i;
                    break;
                case 10:
                    y = (i & 1) == 0 ? 1.0 : -1.0;
                    break;
                case 11:
                    y = x;
                    break;
                case 12:
                    y = -x;
                    break;
                default:
                    y = (yRaw % 257) - 128.0;
                    break;
            }

            int wRaw = data.consumeInt();
            double weight;
            switch (wRaw & 7) {
                case 0:
                    weight = 1.0;
                    break;
                case 1:
                    weight = 0.0;
                    break;
                case 2:
                    weight = -1.0;
                    break;
                case 3:
                    weight = wRaw;
                    break;
                case 4:
                    weight = wRaw / 64.0;
                    break;
                case 5:
                    weight = Double.NaN;
                    break;
                case 6:
                    weight = wRaw > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default:
                    weight = 1.0e-6;
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();

        if (data.remainingBytes() > 0) {
            int smallLen = data.consumeInt(0, 3);
            WeightedObservedPoint[] small = new WeightedObservedPoint[smallLen];
            for (int i = 0; i < smallLen; i++) {
                small[i] = new WeightedObservedPoint(1.0, data.consumeInt(-1, 1), data.consumeInt());
            }
            new HarmonicFitter.ParameterGuesser(small).guess();
        }
    }
}