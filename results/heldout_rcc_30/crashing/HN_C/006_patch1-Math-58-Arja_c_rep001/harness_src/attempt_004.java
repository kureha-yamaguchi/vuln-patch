package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int mode = data.consumeInt(0, 5);
        int n = data.consumeInt(0, 64);

        double baseX = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double stepX = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double amplitude = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double mean = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double sigma = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));

        if (data.consumeBoolean()) {
            baseX = data.consumeInt(-1000, 1000);
        }
        if (data.consumeBoolean()) {
            stepX = data.consumeInt(-50, 50);
        }
        if (data.consumeBoolean()) {
            amplitude = data.consumeInt(-1000, 1000);
        }
        if (data.consumeBoolean()) {
            mean = data.consumeInt(-1000, 1000);
        }
        if (data.consumeBoolean()) {
            sigma = data.consumeInt(-100, 100);
        }

        for (int i = 0; i < n; i++) {
            double x;
            double y;
            double weight;

            if (mode == 0) {
                x = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                y = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            } else if (mode == 1) {
                x = baseX + (stepX * i);
                y = amplitude;
            } else if (mode == 2) {
                x = baseX + (stepX * i);
                double dx = x - mean;
                double s = sigma;
                y = amplitude * Math.exp(-(dx * dx) / (2.0 * s * s));
            } else if (mode == 3) {
                x = i == 0 ? baseX : baseX + (stepX * (n - i));
                y = (i % 2 == 0 ? amplitude : -amplitude);
            } else if (mode == 4) {
                x = data.consumeInt(-10, 10);
                y = data.consumeInt(-10, 10);
            } else {
                int selector = data.consumeInt(0, 7);
                switch (selector) {
                    case 0:
                        x = Double.NaN;
                        break;
                    case 1:
                        x = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        x = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        x = 0.0;
                        break;
                    case 4:
                        x = -0.0;
                        break;
                    case 5:
                        x = Double.MAX_VALUE;
                        break;
                    case 6:
                        x = Double.MIN_VALUE;
                        break;
                    default:
                        x = data.consumeInt(-1000, 1000);
                        break;
                }

                selector = data.consumeInt(0, 7);
                switch (selector) {
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
                        y = 0.0;
                        break;
                    case 4:
                        y = -0.0;
                        break;
                    case 5:
                        y = Double.MAX_VALUE;
                        break;
                    case 6:
                        y = Double.MIN_VALUE;
                        break;
                    default:
                        y = data.consumeInt(-1000, 1000);
                        break;
                }
            }

            int wMode = data.consumeInt(0, 7);
            switch (wMode) {
                case 0:
                    weight = 1.0;
                    break;
                case 1:
                    weight = 0.0;
                    break;
                case 2:
                    weight = -1.0;
                    break;
                case 3:
                    weight = data.consumeInt(-1000, 1000);
                    break;
                case 4:
                    weight = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 5:
                    weight = Double.NaN;
                    break;
                case 6:
                    weight = Double.POSITIVE_INFINITY;
                    break;
                default:
                    weight = Double.NEGATIVE_INFINITY;
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(weight, x, y);
            }

            if (data.consumeBoolean() && data.remainingBytes() > 0) {
                int dupCount = data.consumeInt(0, 2);
                for (int j = 0; j < dupCount; j++) {
                    if (data.consumeBoolean()) {
                        fitter.addObservedPoint(x, y);
                    } else {
                        fitter.addObservedPoint(weight, x, y);
                    }
                }
            }
        }

        fitter.fit();
    }
}