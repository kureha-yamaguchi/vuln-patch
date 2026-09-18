package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 20);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        boolean mostlySorted = data.consumeBoolean();
        double previousX = 0.0;

        for (int i = 0; i < n; i++) {
            double weight;
            switch (data.consumeInt(0, 9)) {
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
                    weight = Double.NaN;
                    break;
                case 5:
                    weight = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    weight = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    weight = Double.MIN_VALUE;
                    break;
                case 8:
                    weight = Double.MAX_VALUE;
                    break;
                default:
                    weight = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            double x;
            if (mostlySorted) {
                if (i == 0) {
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            x = 0.0;
                            break;
                        case 1:
                            x = -0.0;
                            break;
                        case 2:
                            x = Double.NaN;
                            break;
                        case 3:
                            x = Double.POSITIVE_INFINITY;
                            break;
                        case 4:
                            x = Double.NEGATIVE_INFINITY;
                            break;
                        case 5:
                            x = Double.MIN_VALUE;
                            break;
                        case 6:
                            x = Double.MAX_VALUE;
                            break;
                        default:
                            x = Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                            break;
                    }
                } else {
                    double step;
                    switch (data.consumeInt(0, 9)) {
                        case 0:
                            step = 0.0;
                            break;
                        case 1:
                            step = -0.0;
                            break;
                        case 2:
                            step = 1.0;
                            break;
                        case 3:
                            step = -1.0;
                            break;
                        case 4:
                            step = Double.MIN_VALUE;
                            break;
                        case 5:
                            step = -Double.MIN_VALUE;
                            break;
                        case 6:
                            step = Double.MAX_VALUE;
                            break;
                        case 7:
                            step = -Double.MAX_VALUE;
                            break;
                        case 8:
                            step = Double.NaN;
                            break;
                        default:
                            step = Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                            break;
                    }
                    if (data.consumeBoolean()) {
                        x = previousX + step;
                    } else {
                        x = previousX;
                    }
                }
            } else {
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        x = 0.0;
                        break;
                    case 1:
                        x = -0.0;
                        break;
                    case 2:
                        x = Double.NaN;
                        break;
                    case 3:
                        x = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        x = Double.NEGATIVE_INFINITY;
                        break;
                    case 5:
                        x = Double.MIN_VALUE;
                        break;
                    case 6:
                        x = Double.MAX_VALUE;
                        break;
                    case 7:
                        x = -Double.MAX_VALUE;
                        break;
                    case 8:
                        x = data.consumeInt(-16, 16);
                        break;
                    default:
                        x = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                }
            }
            previousX = x;

            double y;
            switch (data.consumeInt(0, 11)) {
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
                    y = Double.NaN;
                    break;
                case 5:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    y = Double.MIN_VALUE;
                    break;
                case 8:
                    y = Double.MAX_VALUE;
                    break;
                case 9:
                    y = -Double.MAX_VALUE;
                    break;
                case 10:
                    y = data.consumeInt(-1024, 1024);
                    break;
                default:
                    y = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        double[] guess = guesser.guess();

        if (data.consumeBoolean() && observations.length > 0) {
            WeightedObservedPoint[] copy = observations.clone();
            for (int i = 0, j = copy.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = copy[i];
                copy[i] = copy[j];
                copy[j] = tmp;
            }
            HarmonicFitter.ParameterGuesser guesser2 = new HarmonicFitter.ParameterGuesser(copy);
            double[] guess2 = guesser2.guess();
            if (guess.length == 3 && guess2.length == 3 && data.consumeBoolean()) {
                double sink = guess[0] + guess[1] + guess[2] + guess2[0] + guess2[1] + guess2[2];
                if (sink == 123456789.0) {
                    throw new RuntimeException();
                }
            }
        }
    }
}