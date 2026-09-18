package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static double nextDouble(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 9);
        switch (selector) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return Double.NaN;
            case 3:
                return Double.POSITIVE_INFINITY;
            case 4:
                return Double.NEGATIVE_INFINITY;
            case 5:
                return Double.MAX_VALUE;
            case 6:
                return -Double.MAX_VALUE;
            case 7:
                return Double.MIN_VALUE;
            case 8:
                return -Double.MIN_VALUE;
            default:
                long bits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
                return Double.longBitsToDouble(bits);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int observationCount = data.consumeInt(0, 32);
        double lastX = 0.0d;
        double lastY = 0.0d;
        double lastW = 1.0d;

        for (int i = 0; i < observationCount; i++) {
            double weight;
            double x;
            double y;

            int shape = data.consumeInt(0, 7);
            switch (shape) {
                case 0:
                    weight = nextDouble(data);
                    x = nextDouble(data);
                    y = nextDouble(data);
                    break;
                case 1:
                    weight = data.consumeInt(-5, 5);
                    x = data.consumeInt(-1000, 1000);
                    y = data.consumeInt(-1000, 1000);
                    break;
                case 2:
                    weight = data.consumeBoolean() ? 1.0d : -1.0d;
                    x = lastX;
                    y = nextDouble(data);
                    break;
                case 3:
                    weight = nextDouble(data);
                    x = nextDouble(data);
                    y = lastY;
                    break;
                case 4:
                    weight = 1.0d;
                    x = i;
                    y = data.consumeInt(-100, 100);
                    break;
                case 5:
                    weight = lastW;
                    x = lastX;
                    y = lastY;
                    break;
                case 6:
                    weight = 0.0d;
                    x = data.consumeInt(-10, 10);
                    y = data.consumeInt(-10, 10);
                    break;
                default:
                    weight = nextDouble(data);
                    x = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                    y = nextDouble(data);
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }

            lastW = weight;
            lastX = x;
            lastY = y;
        }

        if (data.consumeBoolean()) {
            fitter.fit();
            return;
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
        }

        int extraCount = data.consumeInt(0, 16);
        for (int i = 0; i < extraCount; i++) {
            double x;
            double y;
            double w;

            if (data.consumeBoolean()) {
                x = data.consumeInt(-50, 50);
                y = data.consumeInt(-50, 50);
                w = data.consumeInt(-3, 3);
            } else {
                x = nextDouble(data);
                y = nextDouble(data);
                w = nextDouble(data);
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(w, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }
        }

        fitter.fit();
    }
}