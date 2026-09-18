package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int points = data.consumeInt(0, Math.max(0, Math.min(64, data.remainingBytes() + 1)));

        for (int i = 0; i < points; i++) {
            int rawX = data.consumeInt();
            int rawY = data.consumeInt();
            int rawW = data.consumeInt();

            double x;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    x = rawX;
                    break;
                case 1:
                    x = (double) Float.intBitsToFloat(rawX);
                    break;
                case 2:
                    x = rawX == 0 ? 0.0d : 1.0d / rawX;
                    break;
                case 3:
                    x = rawX % 2 == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    x = Double.NaN;
                    break;
                case 5:
                    x = (rawX % 2 == 0) ? 0.0d : -0.0d;
                    break;
                default:
                    x = rawX % 17;
                    break;
            }

            double y;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    y = rawY;
                    break;
                case 1:
                    y = (double) Float.intBitsToFloat(rawY);
                    break;
                case 2:
                    y = rawY == 0 ? 0.0d : 1.0d / rawY;
                    break;
                case 3:
                    y = rawY % 2 == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    y = Double.NaN;
                    break;
                case 5:
                    y = (rawY % 2 == 0) ? 0.0d : -0.0d;
                    break;
                default:
                    y = rawY % 17;
                    break;
            }

            double weight;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    weight = rawW;
                    break;
                case 1:
                    weight = (double) Float.intBitsToFloat(rawW);
                    break;
                case 2:
                    weight = rawW == 0 ? 0.0d : 1.0d / rawW;
                    break;
                case 3:
                    weight = rawW % 2 == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    weight = Double.NaN;
                    break;
                case 5:
                    weight = 0.0d;
                    break;
                default:
                    weight = -Math.abs(rawW);
                    break;
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
        }

        if (data.consumeBoolean()) {
            int extra = data.consumeInt(0, 4);
            for (int i = 0; i < extra; i++) {
                double v = (double) Float.intBitsToFloat(data.consumeInt());
                fitter.addObservedPoint(v, v);
            }
        }

        fitter.fit();
    }
}