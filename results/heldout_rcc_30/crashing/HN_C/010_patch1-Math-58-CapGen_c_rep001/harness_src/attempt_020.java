package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int numPoints = data.consumeInt(0, 32);
        for (int i = 0; i < numPoints; i++) {
            double weight;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    weight = 0.0d;
                    break;
                case 1:
                    weight = -0.0d;
                    break;
                case 2:
                    weight = 1.0d;
                    break;
                case 3:
                    weight = -1.0d;
                    break;
                case 4:
                    weight = Double.NaN;
                    break;
                case 5:
                    weight = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    weight = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    weight = (double) Integer.MAX_VALUE;
                    break;
                case 8:
                    weight = (double) Integer.MIN_VALUE;
                    break;
                default:
                    weight = ((double) data.consumeInt()) / ((data.consumeByte() == 0) ? 1.0d : (double) data.consumeByte());
                    break;
            }

            double x;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = 1.0d;
                    break;
                case 3:
                    x = -1.0d;
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
                    x = Double.MIN_VALUE;
                    break;
                case 8:
                    x = Double.MAX_VALUE;
                    break;
                case 9:
                    x = (double) Integer.MAX_VALUE;
                    break;
                case 10:
                    x = (double) Integer.MIN_VALUE;
                    break;
                default:
                    x = data.consumeInt() + (data.consumeByte() / 128.0d);
                    break;
            }

            double y;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = 1.0d;
                    break;
                case 3:
                    y = -1.0d;
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
                case 8:
                    y = Double.MAX_VALUE;
                    break;
                case 9:
                    y = (double) Integer.MAX_VALUE;
                    break;
                case 10:
                    y = (double) Integer.MIN_VALUE;
                    break;
                default:
                    y = data.consumeInt() + (data.consumeByte() / 128.0d);
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(weight, x, y);
            }
        }

        fitter.fit();
    }
}