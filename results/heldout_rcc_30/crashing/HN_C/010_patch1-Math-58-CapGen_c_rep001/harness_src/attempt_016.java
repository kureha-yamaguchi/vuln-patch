package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int n = data.consumeInt(0, 64);
        for (int i = 0; i < n; i++) {
            double x;
            switch (data.consumeInt(0, 9)) {
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
                    x = data.consumeInt();
                    break;
                case 6: {
                    byte b = data.consumeByte();
                    x = data.consumeInt() / (double) (b == 0 ? 1 : b);
                    break;
                }
                case 7:
                    x = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                    break;
                case 8:
                    x = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
                default:
                    x = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
            }

            double y;
            switch (data.consumeInt(0, 9)) {
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
                    y = data.consumeInt();
                    break;
                case 6: {
                    byte b = data.consumeByte();
                    y = data.consumeInt() / (double) (b == 0 ? 1 : b);
                    break;
                }
                case 7:
                    y = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                    break;
                case 8:
                    y = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
                default:
                    y = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double w;
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        w = Double.NaN;
                        break;
                    case 1:
                        w = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        w = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        w = 0.0d;
                        break;
                    case 4:
                        w = -0.0d;
                        break;
                    case 5:
                        w = data.consumeInt();
                        break;
                    case 6: {
                        byte b = data.consumeByte();
                        w = data.consumeInt() / (double) (b == 0 ? 1 : b);
                        break;
                    }
                    case 7:
                        w = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                        break;
                    case 8:
                        w = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                        break;
                    default:
                        w = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                        break;
                }
                fitter.addObservedPoint(w, x, y);
            }
        }

        fitter.fit();
    }
}