package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int rounds = data.consumeInt(0, 3);
        for (int r = 0; r < rounds; r++) {
            int observationCount = data.consumeInt(0, 32);
            for (int i = 0; i < observationCount; i++) {
                double weight;
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        weight = Double.NaN;
                        break;
                    case 1:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        weight = 0.0d;
                        break;
                    case 4:
                        weight = -0.0d;
                        break;
                    case 5:
                        weight = Double.MAX_VALUE;
                        break;
                    case 6:
                        weight = -Double.MAX_VALUE;
                        break;
                    case 7:
                        weight = Double.MIN_VALUE;
                        break;
                    case 8:
                        weight = -Double.MIN_VALUE;
                        break;
                    default:
                        weight = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                }

                double x;
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        x = Double.NaN;
                        break;
                    case 1:
                        x = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        x = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        x = 0.0d;
                        break;
                    case 4:
                        x = -0.0d;
                        break;
                    case 5:
                        x = Double.MAX_VALUE;
                        break;
                    case 6:
                        x = -Double.MAX_VALUE;
                        break;
                    case 7:
                        x = Double.MIN_VALUE;
                        break;
                    case 8:
                        x = -Double.MIN_VALUE;
                        break;
                    case 9:
                        x = data.consumeInt(-10, 10);
                        break;
                    case 10:
                        x = data.consumeInt() / 1024.0d;
                        break;
                    default:
                        x = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                }

                double y;
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        y = Double.NaN;
                        break;
                    case 1:
                        y = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        y = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        y = 0.0d;
                        break;
                    case 4:
                        y = -0.0d;
                        break;
                    case 5:
                        y = Double.MAX_VALUE;
                        break;
                    case 6:
                        y = -Double.MAX_VALUE;
                        break;
                    case 7:
                        y = Double.MIN_VALUE;
                        break;
                    case 8:
                        y = -Double.MIN_VALUE;
                        break;
                    case 9:
                        y = data.consumeInt(-10, 10);
                        break;
                    case 10:
                        y = data.consumeInt() / 1024.0d;
                        break;
                    default:
                        y = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                }

                int mode = data.consumeInt(0, 2);
                if (mode == 0) {
                    fitter.addObservedPoint(x, y);
                } else if (mode == 1) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }

            if (data.consumeBoolean()) {
                fitter.clearObservations();
            }
        }

        fitter.fit();
    }
}