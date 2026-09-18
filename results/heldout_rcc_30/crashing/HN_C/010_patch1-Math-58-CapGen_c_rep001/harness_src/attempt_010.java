package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int phases = data.consumeInt(1, 3);
        for (int phase = 0; phase < phases; phase++) {
            if (phase > 0 && data.consumeBoolean()) {
                fitter.clearObservations();
            }

            int points = data.consumeInt(0, 32);
            for (int i = 0; i < points; i++) {
                double weight;
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        weight = Double.NaN;
                        break;
                    case 1:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        weight = 0.0d;
                        break;
                    case 4:
                        weight = -0.0d;
                        break;
                    default:
                        weight = ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 1024.0d);
                        break;
                }

                double x;
                switch (data.consumeInt(0, 11)) {
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
                        x = 0.0d;
                        break;
                    case 4:
                        x = -0.0d;
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
                        x = ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 4096.0d);
                        break;
                }

                double y;
                switch (data.consumeInt(0, 11)) {
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
                        y = 0.0d;
                        break;
                    case 4:
                        y = -0.0d;
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
                        y = ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 4096.0d);
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

            if (phase + 1 < phases && data.consumeBoolean()) {
                fitter.fit();
            }
        }

        fitter.fit();
    }
}