package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 32);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        int xMode = data.consumeInt(0, 7);
        double baseX;
        {
            int sel = data.consumeInt(0, 9);
            if (sel == 0) {
                baseX = 0.0d;
            } else if (sel == 1) {
                baseX = -0.0d;
            } else if (sel == 2) {
                baseX = Double.NaN;
            } else if (sel == 3) {
                baseX = Double.POSITIVE_INFINITY;
            } else if (sel == 4) {
                baseX = Double.NEGATIVE_INFINITY;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                baseX = Double.longBitsToDouble(bits);
            }
        }

        double currentX = baseX;
        for (int i = 0; i < n; i++) {
            double weight;
            {
                int sel = data.consumeInt(0, 7);
                if (sel == 0) {
                    weight = 0.0d;
                } else if (sel == 1) {
                    weight = 1.0d;
                } else if (sel == 2) {
                    weight = -1.0d;
                } else if (sel == 3) {
                    weight = Double.NaN;
                } else if (sel == 4) {
                    weight = Double.POSITIVE_INFINITY;
                } else if (sel == 5) {
                    weight = Double.NEGATIVE_INFINITY;
                } else {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    weight = Double.longBitsToDouble(bits);
                }
            }

            double x;
            switch (xMode) {
                case 0:
                    x = currentX;
                    break;
                case 1:
                    x = i;
                    break;
                case 2:
                    x = -i;
                    break;
                case 3:
                    x = currentX + data.consumeByte();
                    currentX = x;
                    break;
                case 4:
                    x = currentX - data.consumeByte();
                    currentX = x;
                    break;
                case 5:
                    x = baseX + (i % 2 == 0 ? 0.0d : 1.0d);
                    break;
                case 6:
                    x = (i == 0) ? baseX : observations[i - 1].getX();
                    break;
                default:
                    int sel = data.consumeInt(0, 9);
                    if (sel == 0) {
                        x = 0.0d;
                    } else if (sel == 1) {
                        x = -0.0d;
                    } else if (sel == 2) {
                        x = Double.NaN;
                    } else if (sel == 3) {
                        x = Double.POSITIVE_INFINITY;
                    } else if (sel == 4) {
                        x = Double.NEGATIVE_INFINITY;
                    } else {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        x = Double.longBitsToDouble(bits);
                    }
                    break;
            }

            double y;
            int yMode = data.consumeInt(0, 8);
            if (yMode == 0) {
                y = 0.0d;
            } else if (yMode == 1) {
                y = x;
            } else if (yMode == 2) {
                y = -x;
            } else if (yMode == 3) {
                y = i;
            } else if (yMode == 4) {
                y = data.consumeByte();
            } else if (yMode == 5) {
                y = Double.NaN;
            } else if (yMode == 6) {
                y = Double.POSITIVE_INFINITY;
            } else if (yMode == 7) {
                y = Double.NEGATIVE_INFINITY;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                y = Double.longBitsToDouble(bits);
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        if (data.consumeBoolean() && observations.length > 1) {
            for (int i = 0; i < observations.length / 2; i++) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[observations.length - 1 - i];
                observations[observations.length - 1 - i] = tmp;
            }
        }

        if (data.consumeBoolean() && observations.length > 2) {
            int i = data.consumeInt(0, observations.length - 1);
            int j = data.consumeInt(0, observations.length - 1);
            WeightedObservedPoint tmp = observations[i];
            observations[i] = observations[j];
            observations[j] = tmp;
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        if (data.consumeBoolean()) {
            guesser.guess();
        }
    }
}