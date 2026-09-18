package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int pointCount = data.consumeInt(0, 32);
        for (int i = 0; i < pointCount; i++) {
            double x;
            switch (data.consumeInt(0, 9)) {
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
                    x = Double.MIN_VALUE;
                    break;
                case 6:
                    x = Double.MAX_VALUE;
                    break;
                default:
                    x = (double) data.consumeInt();
                    break;
            }

            double y;
            switch (data.consumeInt(0, 9)) {
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
                    y = Double.MIN_VALUE;
                    break;
                case 6:
                    y = Double.MAX_VALUE;
                    break;
                default:
                    y = (double) data.consumeInt();
                    break;
            }

            double weight;
            switch (data.consumeInt(0, 9)) {
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
                    weight = Double.MIN_VALUE;
                    break;
                case 7:
                    weight = Double.MAX_VALUE;
                    break;
                default:
                    weight = (double) data.consumeInt();
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
            fitter.getObservations();
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
            int refillCount = data.consumeInt(0, 32);
            for (int i = 0; i < refillCount; i++) {
                double x = (data.consumeBoolean() ? 1.0d : -1.0d) * data.consumeInt();
                double y = (data.consumeBoolean() ? 1.0d : -1.0d) * data.consumeInt();
                double weight = data.consumeBoolean() ? data.consumeInt() : 1.0d;
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }
        }

        fitter.fit();
    }
}