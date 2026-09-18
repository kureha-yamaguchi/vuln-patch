package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int n = data.consumeInt(0, 40);
        final WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        final int xMode = data.consumeInt(0, 5);
        double runningX;
        {
            int sel = data.consumeInt(0, 9);
            int raw = data.consumeInt();
            int raw2 = data.consumeInt();
            switch (sel) {
                case 0:
                    runningX = 0.0;
                    break;
                case 1:
                    runningX = -0.0d;
                    break;
                case 2:
                    runningX = Double.NaN;
                    break;
                case 3:
                    runningX = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    runningX = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    runningX = raw;
                    break;
                case 6:
                    runningX = raw / 1024.0;
                    break;
                case 7:
                    runningX = (double) Integer.MAX_VALUE;
                    break;
                case 8:
                    runningX = (double) Integer.MIN_VALUE;
                    break;
                default:
                    runningX = raw2 == 0 ? raw : ((double) raw) / raw2;
                    break;
            }
        }

        for (int i = 0; i < n; i++) {
            double x;
            switch (xMode) {
                case 0: {
                    int sel = data.consumeInt(0, 9);
                    int raw = data.consumeInt();
                    int raw2 = data.consumeInt();
                    switch (sel) {
                        case 0:
                            x = 0.0;
                            break;
                        case 1:
                            x = -0.0d;
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
                            x = raw;
                            break;
                        case 6:
                            x = raw / 16.0;
                            break;
                        case 7:
                            x = (double) Integer.MAX_VALUE;
                            break;
                        case 8:
                            x = (double) Integer.MIN_VALUE;
                            break;
                        default:
                            x = raw2 == 0 ? raw : ((double) raw) / raw2;
                            break;
                    }
                    break;
                }
                case 1: {
                    int step = data.consumeInt(-3, 3);
                    runningX += step;
                    x = runningX;
                    break;
                }
                case 2: {
                    x = runningX;
                    break;
                }
                case 3: {
                    int step = data.consumeInt(-3, 3);
                    runningX -= step;
                    x = runningX;
                    break;
                }
                case 4: {
                    x = i;
                    break;
                }
                default: {
                    x = n == 0 ? 0.0 : (double) (n - i - 1);
                    break;
                }
            }

            double y;
            {
                int sel = data.consumeInt(0, 11);
                int raw = data.consumeInt();
                int raw2 = data.consumeInt();
                switch (sel) {
                    case 0:
                        y = 0.0;
                        break;
                    case 1:
                        y = -0.0d;
                        break;
                    case 2:
                        y = Double.NaN;
                        break;
                    case 3:
                        y = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        y = Double.NEGATIVE_INFINITY;
                        break;
                    case 5:
                        y = raw;
                        break;
                    case 6:
                        y = raw / 16.0;
                        break;
                    case 7:
                        y = Math.sin(raw);
                        break;
                    case 8:
                        y = Math.cos(raw);
                        break;
                    case 9:
                        y = raw2 == 0 ? raw : ((double) raw) / raw2;
                        break;
                    case 10:
                        y = i;
                        break;
                    default:
                        y = -i;
                        break;
                }
            }

            double weight;
            {
                int sel = data.consumeInt(0, 8);
                int raw = data.consumeInt();
                int raw2 = data.consumeInt();
                switch (sel) {
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
                        weight = Double.NaN;
                        break;
                    case 4:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 5:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 6:
                        weight = raw;
                        break;
                    case 7:
                        weight = raw / 8.0;
                        break;
                    default:
                        weight = raw2 == 0 ? raw : ((double) raw) / raw2;
                        break;
                }
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();
    }
}