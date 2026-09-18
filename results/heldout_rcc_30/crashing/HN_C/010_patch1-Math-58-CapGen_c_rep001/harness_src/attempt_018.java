package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int points = data.consumeInt(0, Math.max(0, Math.min(64, data.remainingBytes() / 2 + 1)));
        for (int i = 0; i < points; i++) {
            int xKind = data.consumeInt(0, 9);
            double x;
            switch (xKind) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = Double.NaN;
                    break;
                case 3:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    x = Double.MAX_VALUE;
                    break;
                case 6:
                    x = -Double.MAX_VALUE;
                    break;
                case 7:
                    x = Double.MIN_VALUE;
                    break;
                case 8:
                    x = -Double.MIN_VALUE;
                    break;
                default:
                    x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
            }

            int yKind = data.consumeInt(0, 9);
            double y;
            switch (yKind) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = Double.NaN;
                    break;
                case 3:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    y = Double.MAX_VALUE;
                    break;
                case 6:
                    y = -Double.MAX_VALUE;
                    break;
                case 7:
                    y = Double.MIN_VALUE;
                    break;
                case 8:
                    y = -Double.MIN_VALUE;
                    break;
                default:
                    y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
            }

            if (data.consumeBoolean()) {
                int wKind = data.consumeInt(0, 9);
                double weight;
                switch (wKind) {
                    case 0:
                        weight = 0.0d;
                        break;
                    case 1:
                        weight = 1.0d;
                        break;
                    case 2:
                        weight = -1.0d;
                        break;
                    case 3:
                        weight = Double.NaN;
                        break;
                    case 4:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 5:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 6:
                        weight = Double.MAX_VALUE;
                        break;
                    case 7:
                        weight = -Double.MAX_VALUE;
                        break;
                    case 8:
                        weight = Double.MIN_VALUE;
                        break;
                    default:
                        weight = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
        } else {
            double[] params = fitter.fit();
            if (params != null && params.length > 0 && data.consumeBoolean()) {
                double sink = 0.0d;
                for (int i = 0; i < params.length; i++) {
                    sink += params[i];
                }
                if (sink == 1.23456789d) {
                    throw new RuntimeException();
                }
            }
        }
    }
}