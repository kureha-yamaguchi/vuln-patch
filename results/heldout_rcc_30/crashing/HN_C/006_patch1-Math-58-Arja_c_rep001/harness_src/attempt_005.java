package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int operations = data.consumeInt(0, 32);
        for (int i = 0; i < operations; i++) {
            int action = data.consumeInt(0, 5);

            if (action == 0) {
                fitter.clearObservations();
                continue;
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
                    x = Double.NaN;
                    break;
                case 3:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    x = data.consumeInt();
                    break;
                case 6:
                    x = data.consumeByte();
                    break;
                case 7:
                    x = data.consumeInt() / 10.0d;
                    break;
                case 8:
                    x = data.consumeInt() / 1000.0d;
                    break;
                case 9:
                    x = Math.scalb((double) data.consumeByte(), data.consumeInt(-16, 16));
                    break;
                case 10:
                    x = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    x = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
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
                    y = data.consumeInt();
                    break;
                case 6:
                    y = data.consumeByte();
                    break;
                case 7:
                    y = data.consumeInt() / 10.0d;
                    break;
                case 8:
                    y = data.consumeInt() / 1000.0d;
                    break;
                case 9:
                    y = Math.scalb((double) data.consumeByte(), data.consumeInt(-16, 16));
                    break;
                case 10:
                    y = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    y = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
            }

            if (action == 1 || action == 2) {
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
                        weight = data.consumeInt();
                        break;
                    case 8:
                        weight = data.consumeByte();
                        break;
                    case 9:
                        weight = data.consumeInt() / 10.0d;
                        break;
                    case 10:
                        weight = Math.scalb((double) data.consumeByte(), data.consumeInt(-16, 16));
                        break;
                    default:
                        weight = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            }

            if (data.consumeBoolean()) {
                fitter.fit();
            }
        }

        fitter.fit();
    }
}