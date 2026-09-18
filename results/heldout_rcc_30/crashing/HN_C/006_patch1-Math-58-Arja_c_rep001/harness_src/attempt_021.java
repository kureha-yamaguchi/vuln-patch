package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        java.util.function.DoubleSupplier nextDouble = () -> {
            switch (data.consumeInt(0, 8)) {
                case 0:
                    return (double) data.consumeInt();
                case 1:
                    return (double) data.consumeByte();
                case 2: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    return Double.longBitsToDouble(bits);
                }
                case 3:
                    if (data.consumeBoolean()) {
                        return Double.NaN;
                    }
                    return data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                case 4: {
                    int num = data.consumeInt();
                    int den = data.consumeInt();
                    return den == 0 ? (double) num : ((double) num) / den;
                }
                case 5:
                    return data.consumeBoolean() ? 0.0d : -0.0d;
                case 6:
                    return Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                case 7: {
                    byte[] bytes = data.consumeBytes(8);
                    long v = 0L;
                    for (byte b : bytes) {
                        v = (v << 8) ^ (b & 0xffL);
                    }
                    return Double.longBitsToDouble(v);
                }
                default:
                    return (double) (data.consumeBoolean() ? data.consumeString(16).length()
                            : data.consumeAsciiString(16).hashCode());
            }
        };

        int n = data.consumeInt(0, 40);
        boolean structured = data.consumeBoolean();

        double norm = nextDouble.getAsDouble();
        double mean = nextDouble.getAsDouble();
        double sigma = nextDouble.getAsDouble();

        double lastWeight = 1.0d;
        double lastX = 0.0d;
        double lastY = 0.0d;

        for (int i = 0; i < n; i++) {
            double weight;
            double x;
            double y;

            if (structured) {
                if (data.consumeBoolean() && i > 0) {
                    x = lastX;
                } else {
                    x = mean + nextDouble.getAsDouble();
                }

                double delta = x - mean;
                double denom = 2.0d * sigma * sigma;
                double gaussian = norm * Math.exp(-(delta * delta) / denom);

                if (data.consumeBoolean()) {
                    y = gaussian;
                } else {
                    y = gaussian + nextDouble.getAsDouble();
                }

                weight = data.consumeBoolean() ? 1.0d : nextDouble.getAsDouble();
            } else {
                if (data.consumeBoolean() && i > 0) {
                    x = lastX;
                } else {
                    x = nextDouble.getAsDouble();
                }

                if (data.consumeBoolean() && i > 0) {
                    y = lastY;
                } else {
                    y = nextDouble.getAsDouble();
                }

                weight = data.consumeBoolean() ? nextDouble.getAsDouble() : 1.0d;
            }

            switch (data.consumeInt(0, 2)) {
                case 0:
                    fitter.addObservedPoint(x, y);
                    break;
                case 1:
                    fitter.addObservedPoint(weight, x, y);
                    break;
                default:
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                    break;
            }

            lastWeight = weight;
            lastX = x;
            lastY = y;

            if (data.consumeBoolean()) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        fitter.addObservedPoint(lastX, lastY);
                        break;
                    case 1:
                        fitter.addObservedPoint(lastWeight, lastX, lastY);
                        break;
                    default:
                        fitter.addObservedPoint(new WeightedObservedPoint(lastWeight, lastX, lastY));
                        break;
                }
            }
        }

        fitter.fit();
    }
}