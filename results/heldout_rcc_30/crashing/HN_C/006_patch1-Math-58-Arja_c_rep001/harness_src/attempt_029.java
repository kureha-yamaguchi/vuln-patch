package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(
                        new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int mode = data.consumeInt(0, 3);
        int n = data.consumeInt(0, 32);

        if (mode == 0) {
            double x = 0.0;
            for (int i = 0; i < n; i++) {
                x += data.consumeInt(-3, 3);
                double y;
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        y = 0.0;
                        break;
                    case 1:
                        y = data.consumeInt(-1000, 1000);
                        break;
                    case 2:
                        y = data.consumeInt() / (double) (data.consumeInt(1, Integer.MAX_VALUE));
                        break;
                    case 3:
                        y = Double.NaN;
                        break;
                    case 4:
                        y = Double.POSITIVE_INFINITY;
                        break;
                    case 5:
                        y = Double.NEGATIVE_INFINITY;
                        break;
                    case 6:
                        y = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                    default:
                        y = -0.0d;
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    double w;
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            w = 1.0;
                            break;
                        case 1:
                            w = 0.0;
                            break;
                        case 2:
                            w = -1.0;
                            break;
                        case 3:
                            w = data.consumeInt(-1000, 1000);
                            break;
                        case 4:
                            w = Double.NaN;
                            break;
                        case 5:
                            w = Double.POSITIVE_INFINITY;
                            break;
                        case 6:
                            w = Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            w = Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                            break;
                    }
                    fitter.addObservedPoint(w, x, y);
                }
            }
        } else if (mode == 1) {
            double norm = Math.abs(data.consumeInt(-1000, 1000));
            double mean = data.consumeInt(-1000, 1000);
            double sigma = data.consumeInt(-1000, 1000);
            double x = mean + data.consumeInt(-10, 10);
            for (int i = 0; i < n; i++) {
                x += data.consumeInt(-5, 5);
                double dx = x - mean;
                double y;
                if (sigma == 0.0) {
                    y = (dx == 0.0) ? norm : 0.0;
                } else {
                    y = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
                }

                switch (data.consumeInt(0, 4)) {
                    case 0:
                        y += data.consumeInt(-10, 10);
                        break;
                    case 1:
                        y = -y;
                        break;
                    case 2:
                        y = 0.0;
                        break;
                    case 3:
                        y = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                    default:
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(data.consumeInt(-5, 5), x, y);
                }
            }
        } else if (mode == 2) {
            for (int i = 0; i < n; i++) {
                double x;
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        x = 0.0;
                        break;
                    case 1:
                        x = -0.0d;
                        break;
                    case 2:
                        x = data.consumeInt();
                        break;
                    case 3:
                        x = data.consumeInt(-10, 10);
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
                        x = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                    default:
                        x = i;
                        break;
                }

                double y;
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        y = 0.0;
                        break;
                    case 1:
                        y = -0.0d;
                        break;
                    case 2:
                        y = data.consumeInt();
                        break;
                    case 3:
                        y = data.consumeInt(-10, 10);
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
                        y = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                    default:
                        y = i;
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    double w = data.consumeBoolean()
                            ? data.consumeInt(-100, 100)
                            : Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    fitter.addObservedPoint(w, x, y);
                }
            }
        } else {
            int base = data.consumeInt(-100, 100);
            for (int i = 0; i < n; i++) {
                double x = base + (data.consumeBoolean() ? i : -i);
                double y = (i == n / 2) ? Math.abs(data.consumeInt(-1000, 1000)) : data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(data.consumeInt(-10, 10), x, y);
                }
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
            int m = data.consumeInt(0, 16);
            double x = data.consumeInt(-50, 50);
            for (int i = 0; i < m; i++) {
                x += data.consumeInt(-2, 2);
                double y = data.consumeBoolean() ? data.consumeInt(-100, 100) : Math.abs(data.consumeInt(-100, 100));
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(data.consumeInt(-3, 3), x, y);
                }
            }
        }

        fitter.fit();
    }
}