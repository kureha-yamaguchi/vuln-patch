package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int mode = data.consumeInt(0, 6);
        int n = data.consumeInt(0, 32);

        double base = interestingDouble(data);
        double step = interestingDouble(data);
        double amplitude = interestingDouble(data);
        double center = interestingDouble(data);
        double sigma = interestingDouble(data);

        for (int i = 0; i < n; i++) {
            double x;
            double y;
            double w = interestingWeight(data);

            switch (mode) {
                case 0:
                    x = interestingDouble(data);
                    y = interestingDouble(data);
                    break;
                case 1:
                    x = base;
                    y = interestingDouble(data);
                    break;
                case 2:
                    x = base + (step * i);
                    y = amplitude * Math.exp(-square(x - center) / safeDenominator(sigma));
                    if (data.consumeBoolean()) {
                        y += interestingDouble(data);
                    }
                    break;
                case 3:
                    x = base + (i / 2);
                    y = (i % 2 == 0) ? amplitude : -amplitude;
                    if (data.consumeBoolean()) {
                        y = interestingDouble(data);
                    }
                    break;
                case 4:
                    x = i;
                    y = i == 0 ? amplitude : amplitude / i;
                    if (data.consumeBoolean()) {
                        x = -x;
                    }
                    if (data.consumeBoolean()) {
                        y = -y;
                    }
                    break;
                case 5:
                    x = (i == 0) ? 0.0 : base + ((i - (n / 2.0)) * step);
                    y = amplitude * Math.exp(-square(x - center) / safeDenominator(sigma));
                    break;
                default:
                    x = chooseBoundaryDouble(data, i, n);
                    y = chooseBoundaryDouble(data, n - i, i + 1);
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(w, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }

            if (data.consumeBoolean() && data.remainingBytes() > 0) {
                mode = data.consumeInt(0, 6);
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
        } else {
            fitter.fit();
        }
    }

    private static double interestingDouble(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 11)) {
            case 0:
                return 0.0;
            case 1:
                return -0.0;
            case 2:
                return 1.0;
            case 3:
                return -1.0;
            case 4:
                return Double.NaN;
            case 5:
                return Double.POSITIVE_INFINITY;
            case 6:
                return Double.NEGATIVE_INFINITY;
            case 7:
                return Double.MAX_VALUE;
            case 8:
                return -Double.MAX_VALUE;
            case 9:
                return Double.MIN_VALUE;
            case 10:
                return -Double.MIN_VALUE;
            default:
                long hi = ((long) data.consumeInt()) << 32;
                long lo = data.consumeInt() & 0xffffffffL;
                return Double.longBitsToDouble(hi | lo);
        }
    }

    private static double chooseBoundaryDouble(FuzzedDataProvider data, int a, int b) {
        switch (data.consumeInt(0, 9)) {
            case 0:
                return a;
            case 1:
                return -a;
            case 2:
                return b;
            case 3:
                return -b;
            case 4:
                return a - b;
            case 5:
                return b - a;
            case 6:
                return Integer.MAX_VALUE;
            case 7:
                return Integer.MIN_VALUE;
            case 8:
                return 0.5 * (a + b);
            default:
                return interestingDouble(data);
        }
    }

    private static double interestingWeight(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 7)) {
            case 0:
                return 0.0;
            case 1:
                return 1.0;
            case 2:
                return -1.0;
            case 3:
                return Double.NaN;
            case 4:
                return Double.POSITIVE_INFINITY;
            case 5:
                return Double.NEGATIVE_INFINITY;
            case 6:
                return Double.MIN_VALUE;
            default:
                return interestingDouble(data);
        }
    }

    private static double square(double v) {
        return v * v;
    }

    private static double safeDenominator(double sigma) {
        return 2.0 * sigma * sigma;
    }
}