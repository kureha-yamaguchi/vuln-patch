package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int observationCount = data.consumeInt(0, Math.max(0, Math.min(64, data.remainingBytes() + 1)));

        for (int i = 0; i < observationCount; i++) {
            int xKind = data.consumeInt(0, 11);
            double x;
            switch (xKind) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = 1.0d;
                    break;
                case 3:
                    x = -1.0d;
                    break;
                case 4:
                    x = Double.NaN;
                    break;
                case 5:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    x = Double.MAX_VALUE;
                    break;
                case 8:
                    x = -Double.MAX_VALUE;
                    break;
                case 9:
                    x = Double.MIN_VALUE;
                    break;
                case 10:
                    x = -Double.MIN_VALUE;
                    break;
                default:
                    x = data.consumeInt() / (data.consumeBoolean() ? 1.0d : 1024.0d);
                    break;
            }

            int yKind = data.consumeInt(0, 13);
            double y;
            switch (yKind) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = 1.0d;
                    break;
                case 3:
                    y = -1.0d;
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
                    y = Double.MAX_VALUE;
                    break;
                case 8:
                    y = -Double.MAX_VALUE;
                    break;
                case 9:
                    y = Double.MIN_VALUE;
                    break;
                case 10:
                    y = -Double.MIN_VALUE;
                    break;
                case 11:
                    y = data.consumeByte();
                    break;
                case 12:
                    y = data.consumeInt() / 3.0d;
                    break;
                default:
                    int base = data.consumeInt();
                    y = (double) (base * base);
                    if (data.consumeBoolean()) {
                        y = -y;
                    }
                    break;
            }

            if (data.consumeBoolean()) {
                int wKind = data.consumeInt(0, 11);
                double weight;
                switch (wKind) {
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
                    case 7:
                        weight = Double.MAX_VALUE;
                        break;
                    case 8:
                        weight = -Double.MAX_VALUE;
                        break;
                    case 9:
                        weight = Double.MIN_VALUE;
                        break;
                    case 10:
                        weight = -Double.MIN_VALUE;
                        break;
                    default:
                        weight = data.consumeInt() / (data.consumeBoolean() ? 1.0d : 256.0d);
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }

            if (data.consumeBoolean() && i > 0) {
                fitter.addObservedPoint(x, y);
            }
        }

        if (data.consumeBoolean() && observationCount > 0) {
            fitter.fit();
        }

        double[] params = fitter.fit();

        if (data.consumeBoolean()) {
            double sink = 0.0d;
            for (double p : params) {
                sink += p;
            }
            if (sink == 123456789.0d) {
                throw new IllegalStateException();
            }
        }
    }
}