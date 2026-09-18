package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final java.util.function.DoubleSupplier nextDouble = new java.util.function.DoubleSupplier() {
            @Override
            public double getAsDouble() {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        return (double) data.consumeInt();
                    case 1:
                        return ((double) data.consumeInt(-1000000, 1000000)) /
                               (data.consumeBoolean() ? 1.0 : 10.0);
                    case 2:
                        long bits = (((long) data.consumeInt()) << 32)
                                ^ (((long) data.consumeInt()) & 0xffffffffL);
                        return Double.longBitsToDouble(bits);
                    case 3:
                        if (data.consumeBoolean()) {
                            return Double.NaN;
                        }
                        return data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    case 4:
                        return data.consumeBoolean() ? 0.0d : -0.0d;
                    case 5:
                        return data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    case 6:
                        return data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    default:
                        return data.consumeAsciiString(8).length() * (data.consumeBoolean() ? 1.0 : -1.0);
                }
            }
        };

        org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int maxPoints = Math.min(64, data.remainingBytes() + 1);
        int n = data.consumeInt(0, maxPoints);

        for (int i = 0; i < n; i++) {
            double x = nextDouble.getAsDouble();
            double y = nextDouble.getAsDouble();
            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight = nextDouble.getAsDouble();
                fitter.addObservedPoint(weight, x, y);
            }
        }

        if (data.consumeBoolean() && n > 0) {
            double x = nextDouble.getAsDouble();
            double y = nextDouble.getAsDouble();
            fitter.addObservedPoint(x, y);
        }

        fitter.fit();
    }
}