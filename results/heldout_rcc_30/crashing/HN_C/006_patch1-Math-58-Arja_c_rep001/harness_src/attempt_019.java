package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int n = data.consumeInt(0, 64);

        final double[] xs = new double[n];
        final double[] ys = new double[n];
        final double[] ws = new double[n];

        final boolean structured = data.consumeBoolean();

        if (structured) {
            double baseX;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    baseX = 0.0;
                    break;
                case 1:
                    baseX = data.consumeInt(-10, 10);
                    break;
                case 2:
                    baseX = data.consumeByte();
                    break;
                case 3:
                    baseX = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 4:
                    baseX = Double.NaN;
                    break;
                default:
                    baseX = Double.POSITIVE_INFINITY;
                    break;
            }

            double step;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    step = 0.0;
                    break;
                case 1:
                    step = 1.0;
                    break;
                case 2:
                    step = -1.0;
                    break;
                case 3:
                    step = data.consumeInt(-5, 5);
                    break;
                case 4:
                    step = data.consumeByte();
                    break;
                case 5:
                    step = Double.MIN_VALUE;
                    break;
                default:
                    step = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
            }

            double norm = data.consumeInt(-100, 100);
            double mean = data.consumeInt(-100, 100);
            double sigma = data.consumeInt(-10, 10);
            double offset = data.consumeInt(-100, 100);

            for (int i = 0; i < n; i++) {
                double x = baseX + (i * step);
                if (data.consumeBoolean()) {
                    x = baseX - (i * step);
                }

                double y;
                if (sigma == 0.0 || Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(mean) || Double.isInfinite(mean)) {
                    y = offset;
                } else {
                    double dx = x - mean;
                    y = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + offset;
                }

                if (data.consumeBoolean()) {
                    y += data.consumeByte();
                }
                if (data.consumeBoolean()) {
                    y = -y;
                }

                double w;
                switch (data.consumeInt(0, 5)) {
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
                        w = data.consumeInt(-10, 10);
                        break;
                    case 4:
                        w = data.consumeByte();
                        break;
                    default:
                        w = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }

                xs[i] = x;
                ys[i] = y;
                ws[i] = w;
            }
        } else {
            for (int i = 0; i < n; i++) {
                double x;
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        x = 0.0;
                        break;
                    case 1:
                        x = data.consumeInt();
                        break;
                    case 2:
                        x = data.consumeInt(-1000, 1000);
                        break;
                    case 3:
                        x = data.consumeByte();
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
                    default:
                        x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }

                double y;
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        y = 0.0;
                        break;
                    case 1:
                        y = data.consumeInt();
                        break;
                    case 2:
                        y = data.consumeInt(-1000, 1000);
                        break;
                    case 3:
                        y = data.consumeByte();
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
                    default:
                        y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }

                double w;
                switch (data.consumeInt(0, 8)) {
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
                        w = data.consumeInt();
                        break;
                    case 4:
                        w = data.consumeInt(-1000, 1000);
                        break;
                    case 5:
                        w = data.consumeByte();
                        break;
                    case 6:
                        w = Double.NaN;
                        break;
                    case 7:
                        w = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        w = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }

                xs[i] = x;
                ys[i] = y;
                ws[i] = w;
            }
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            switch (data.consumeInt(0, 2)) {
                case 0:
                    fitter.addObservedPoint(xs[i], ys[i]);
                    break;
                case 1:
                    fitter.addObservedPoint(ws[i], xs[i], ys[i]);
                    break;
                default:
                    fitter.addObservedPoint(new WeightedObservedPoint(ws[i], xs[i], ys[i]));
                    break;
            }
        }
        fitter.fit();

        if (data.consumeBoolean()) {
            GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = n - 1; i >= 0; i--) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        fitter2.addObservedPoint(xs[i], ys[i]);
                        break;
                    case 1:
                        fitter2.addObservedPoint(ws[i], xs[i], ys[i]);
                        break;
                    default:
                        fitter2.addObservedPoint(new WeightedObservedPoint(ws[i], xs[i], ys[i]));
                        break;
                }
            }
            fitter2.fit();
        }
    }
}