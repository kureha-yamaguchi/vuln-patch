package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int observationCount = data.consumeInt(0, 64);
        for (int i = 0; i < observationCount; i++) {
            double x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 3:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    x = Double.NaN;
                    break;
                case 5:
                    x = (double) data.consumeInt(-10, 10);
                    break;
                case 6:
                    x = (double) data.consumeInt();
                    break;
                default:
                    x = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            double y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 3:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    y = Double.NaN;
                    break;
                case 5:
                    y = (double) data.consumeInt(-10, 10);
                    break;
                case 6:
                    y = (double) data.consumeInt();
                    break;
                default:
                    y = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight;
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        weight = 0.0d;
                        break;
                    case 1:
                        weight = -0.0d;
                        break;
                    case 2:
                        weight = 1.0d;
                        break;
                    case 3:
                        weight = -1.0d;
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
                    default:
                        weight = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            }
        }

        double[] params = fitter.fit();

        if (params != null && params.length > 0 && data.consumeBoolean()) {
            double sink = 0.0d;
            for (int i = 0; i < params.length; i++) {
                sink += params[i];
            }
            if (sink == 123456789.0d) {
                throw new RuntimeException("unreachable");
            }
        }
    }
}