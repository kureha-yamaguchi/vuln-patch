package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int n = data.consumeInt(0, 64);
        double[] xs = new double[n];
        double[] ys = new double[n];

        for (int i = 0; i < n; i++) {
            long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = Double.longBitsToDouble(xBits);
                    break;
                case 1:
                    x = data.consumeInt();
                    break;
                case 2:
                    x = data.consumeInt() / (double) (Math.abs(data.consumeInt()) + 1);
                    break;
                case 3:
                    x = i;
                    break;
                case 4:
                    x = -i;
                    break;
                case 5:
                    x = 0.0d;
                    break;
                case 6:
                    x = (i == 0) ? 0.0d : xs[data.consumeInt(0, i - 1)];
                    break;
                default:
                    x = (double) (byte) data.consumeByte();
                    break;
            }

            double y;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    y = Double.longBitsToDouble(yBits);
                    break;
                case 1:
                    y = data.consumeInt();
                    break;
                case 2:
                    y = data.consumeInt() / (double) (Math.abs(data.consumeInt()) + 1);
                    break;
                case 3:
                    y = 0.0d;
                    break;
                case 4:
                    y = 1.0d;
                    break;
                case 5:
                    y = -1.0d;
                    break;
                case 6:
                    y = Math.abs(data.consumeInt());
                    break;
                case 7:
                    y = (i == 0) ? 0.0d : ys[data.consumeInt(0, i - 1)];
                    break;
                default:
                    y = (double) (byte) data.consumeByte();
                    break;
            }

            double weight;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    weight = Double.longBitsToDouble(wBits);
                    break;
                case 1:
                    weight = data.consumeInt();
                    break;
                case 2:
                    weight = data.consumeInt() / (double) (Math.abs(data.consumeInt()) + 1);
                    break;
                case 3:
                    weight = 1.0d;
                    break;
                case 4:
                    weight = 0.0d;
                    break;
                case 5:
                    weight = -1.0d;
                    break;
                case 6:
                    weight = Math.abs((double) data.consumeInt());
                    break;
                case 7:
                    weight = Double.POSITIVE_INFINITY;
                    break;
                default:
                    weight = Double.NaN;
                    break;
            }

            xs[i] = x;
            ys[i] = y;

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
            return;
        }

        if (n >= 3 && data.consumeBoolean()) {
            fitter.clearObservations();

            int mode = data.consumeInt(0, 5);
            for (int i = 0; i < n; i++) {
                double x;
                double y;
                switch (mode) {
                    case 0:
                        x = i - (n / 2.0d);
                        y = Math.abs((n / 2.0d) - i);
                        break;
                    case 1:
                        x = i;
                        y = i;
                        break;
                    case 2:
                        x = i;
                        y = n - i;
                        break;
                    case 3:
                        x = (i % 2 == 0) ? i : -i;
                        y = (i % 3 == 0) ? 0.0d : 1.0d;
                        break;
                    case 4:
                        x = xs[n - 1 - i];
                        y = ys[i];
                        break;
                    default:
                        x = 0.0d;
                        y = 0.0d;
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(1.0d, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        }

        fitter.fit();
    }
}