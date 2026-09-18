package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int n = data.consumeInt(0, 40);
        boolean structured = data.consumeBoolean();

        if (structured) {
            double norm;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    norm = data.consumeInt(-1000, 1000);
                    break;
                case 1:
                    norm = data.consumeByte();
                    break;
                case 2:
                    norm = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                    break;
                case 3:
                    norm = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : -0.0d;
                    break;
                default:
                    norm = data.consumeInt() / 16.0d;
                    break;
            }

            double mean = data.consumeInt() / 32.0d;
            int sigmaSeed = data.consumeInt();
            double sigma;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    sigma = 0.0d;
                    break;
                case 1:
                    sigma = -0.0d;
                    break;
                case 2:
                    sigma = Math.abs(sigmaSeed % 1000) / 32.0d;
                    break;
                case 3:
                    sigma = -Math.abs(sigmaSeed % 1000) / 32.0d;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    sigma = Double.longBitsToDouble(bits);
                    break;
            }

            double xStart = data.consumeInt() / 16.0d;
            double step;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    step = 0.0d;
                    break;
                case 1:
                    step = 1.0d;
                    break;
                case 2:
                    step = -1.0d;
                    break;
                case 3:
                    step = data.consumeByte();
                    break;
                case 4:
                    step = data.consumeInt(-100, 100) / 8.0d;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    step = Double.longBitsToDouble(bits);
                    break;
            }

            boolean reverse = data.consumeBoolean();
            boolean weighted = data.consumeBoolean();

            for (int i = 0; i < n; i++) {
                int idx = reverse ? (n - 1 - i) : i;
                double x = xStart + (idx * step);
                if (data.consumeBoolean()) {
                    x = mean;
                }

                double dx = x - mean;
                double y = norm * Math.exp(-(dx * dx) / (2.0d * sigma * sigma));

                if (data.consumeBoolean()) {
                    y += data.consumeInt(-1000, 1000) / 128.0d;
                }
                if (data.consumeBoolean()) {
                    switch (data.consumeInt(0, 4)) {
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
                            y = -0.0d;
                            break;
                        default:
                            break;
                    }
                }

                if (weighted) {
                    double w;
                    switch (data.consumeInt(0, 6)) {
                        case 0:
                            w = 1.0d;
                            break;
                        case 1:
                            w = 0.0d;
                            break;
                        case 2:
                            w = -1.0d;
                            break;
                        case 3:
                            w = data.consumeInt(-1000, 1000) / 64.0d;
                            break;
                        case 4:
                            w = data.consumeByte();
                            break;
                        case 5:
                            w = Double.NaN;
                            break;
                        default:
                            w = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                            break;
                    }
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double x = Double.longBitsToDouble(xb);
                double y = Double.longBitsToDouble(yb);

                if (data.consumeBoolean()) {
                    x = data.consumeInt(-10000, 10000) / 64.0d;
                }
                if (data.consumeBoolean()) {
                    y = data.consumeInt(-10000, 10000) / 64.0d;
                }

                if (data.consumeBoolean()) {
                    double w;
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            w = data.consumeInt(-10000, 10000) / 128.0d;
                            break;
                        case 1:
                            w = 0.0d;
                            break;
                        case 2:
                            w = -0.0d;
                            break;
                        case 3:
                            w = 1.0d;
                            break;
                        case 4:
                            w = -1.0d;
                            break;
                        case 5:
                            w = Double.NaN;
                            break;
                        case 6:
                            w = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            w = Double.NEGATIVE_INFINITY;
                            break;
                    }
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        }

        fitter.fit();
    }
}