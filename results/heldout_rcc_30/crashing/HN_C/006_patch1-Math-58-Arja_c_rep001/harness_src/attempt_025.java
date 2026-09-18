package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int n = data.consumeInt(0, 40);

        for (int i = 0; i < n; i++) {
            double weight;
            {
                int kind = data.consumeInt(0, 9);
                int a = data.consumeInt();
                int b = data.consumeInt();
                switch (kind) {
                    case 0:
                        weight = 0.0;
                        break;
                    case 1:
                        weight = -0.0;
                        break;
                    case 2:
                        weight = a;
                        break;
                    case 3:
                        weight = (double) a / (b == 0 ? 1 : b);
                        break;
                    case 4:
                        weight = (double) a * (double) b;
                        break;
                    case 5:
                        weight = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                        break;
                    case 6:
                        weight = a >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        weight = Double.NaN;
                        break;
                    case 8:
                        weight = Double.MIN_VALUE;
                        break;
                    default:
                        weight = Double.MAX_VALUE;
                        break;
                }
            }

            double x;
            {
                int kind = data.consumeInt(0, 9);
                int a = data.consumeInt();
                int b = data.consumeInt();
                switch (kind) {
                    case 0:
                        x = 0.0;
                        break;
                    case 1:
                        x = -0.0;
                        break;
                    case 2:
                        x = a;
                        break;
                    case 3:
                        x = (double) a / (b == 0 ? 1 : b);
                        break;
                    case 4:
                        x = (double) a * (double) b;
                        break;
                    case 5:
                        x = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                        break;
                    case 6:
                        x = a >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        x = Double.NaN;
                        break;
                    case 8:
                        x = Double.MIN_NORMAL;
                        break;
                    default:
                        x = (i == 0) ? Double.MAX_VALUE : i;
                        break;
                }
            }

            double y;
            {
                int kind = data.consumeInt(0, 9);
                int a = data.consumeInt();
                int b = data.consumeInt();
                switch (kind) {
                    case 0:
                        y = 0.0;
                        break;
                    case 1:
                        y = -0.0;
                        break;
                    case 2:
                        y = a;
                        break;
                    case 3:
                        y = (double) a / (b == 0 ? 1 : b);
                        break;
                    case 4:
                        y = (double) a * (double) b;
                        break;
                    case 5:
                        y = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                        break;
                    case 6:
                        y = a >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        y = Double.NaN;
                        break;
                    case 8:
                        y = -Double.MAX_VALUE;
                        break;
                    default:
                        y = Double.MIN_VALUE;
                        break;
                }
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(weight, x, y);
            }

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(weight, x, y);
                }
            }
        }

        if (data.consumeBoolean()) {
            fitter.addObservedPoint(0.0, 0.0);
        }
        if (data.consumeBoolean()) {
            fitter.addObservedPoint(1.0, 0.0, 0.0);
        }
        if (data.consumeBoolean()) {
            fitter.addObservedPoint(1.0, -1.0, 1.0);
            fitter.addObservedPoint(1.0, 0.0, 2.0);
            fitter.addObservedPoint(1.0, 1.0, 1.0);
        }

        fitter.fit();
    }
}