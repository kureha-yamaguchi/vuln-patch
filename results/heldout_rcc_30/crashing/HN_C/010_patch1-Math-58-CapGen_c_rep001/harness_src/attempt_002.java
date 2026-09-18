package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int count = data.consumeInt(0, 64);
        for (int i = 0; i < count; i++) {
            double x;
            switch (data.consumeInt(0, 11)) {
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
                case 9:
                    x = Double.MIN_NORMAL;
                    break;
                case 10:
                    x = -Double.MIN_NORMAL;
                    break;
                default:
                    x = (double) data.consumeInt();
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
                case 9:
                    y = Double.MIN_NORMAL;
                    break;
                case 10:
                    y = -Double.MIN_NORMAL;
                    break;
                default:
                    y = (double) data.consumeInt();
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight;
                switch (data.consumeInt(0, 11)) {
                    case 0:
                        weight = 0.0d;
                        break;
                    case 1:
                        weight = -0.0d;
                        break;
                    case 2:
                        weight = Double.NaN;
                        break;
                    case 3:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 5:
                        weight = 1.0d;
                        break;
                    case 6:
                        weight = -1.0d;
                        break;
                    case 7:
                        weight = Double.MAX_VALUE;
                        break;
                    case 8:
                        weight = -Double.MAX_VALUE;
                        break;
                    case 9:
                        weight = Double.MIN_VALUE;
                        break;
                    case 10:
                        weight = -Double.MIN_VALUE;
                        break;
                    default:
                        weight = (double) data.consumeInt();
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            }
        }

        if (data.consumeBoolean() && count > 0) {
            fitter.clearObservations();
            int count2 = data.consumeInt(0, 64);
            for (int i = 0; i < count2; i++) {
                double x = (data.consumeBoolean() ? 1.0d : -1.0d) * data.consumeInt();
                double y = (data.consumeBoolean() ? 1.0d : -1.0d) * data.consumeInt();
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    double weight = data.consumeBoolean() ? data.consumeInt() : 1.0d;
                    fitter.addObservedPoint(weight, x, y);
                }
            }
        }

        fitter.fit();
    }
}