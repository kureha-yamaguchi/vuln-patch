package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int pointCount = data.consumeInt(0, 64);
        for (int i = 0; i < pointCount; i++) {
            double x;
            double y;
            double w;

            switch (data.consumeInt(0, 11)) {
                case 0:
                    x = 0.0;
                    break;
                case 1:
                    x = -0.0;
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
                default:
                    x = (double) data.consumeInt();
                    break;
            }

            switch (data.consumeInt(0, 11)) {
                case 0:
                    y = 0.0;
                    break;
                case 1:
                    y = -0.0;
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
                default:
                    y = (double) data.consumeInt();
                    break;
            }

            switch (data.consumeInt(0, 11)) {
                case 0:
                    w = 0.0;
                    break;
                case 1:
                    w = -0.0;
                    break;
                case 2:
                    w = 1.0;
                    break;
                case 3:
                    w = -1.0;
                    break;
                case 4:
                    w = Double.NaN;
                    break;
                case 5:
                    w = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    w = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    w = (double) data.consumeInt();
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(w, x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
            return;
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
        }

        int pointCount2 = data.consumeInt(0, 64);
        for (int i = 0; i < pointCount2; i++) {
            double x;
            double y;
            double w;

            switch (data.consumeInt(0, 9)) {
                case 0:
                    x = Double.NaN;
                    break;
                case 1:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 2:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    x = (double) data.consumeInt();
                    break;
            }

            switch (data.consumeInt(0, 9)) {
                case 0:
                    y = Double.NaN;
                    break;
                case 1:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 2:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    y = (double) data.consumeInt();
                    break;
            }

            switch (data.consumeInt(0, 9)) {
                case 0:
                    w = Double.NaN;
                    break;
                case 1:
                    w = Double.POSITIVE_INFINITY;
                    break;
                case 2:
                    w = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    w = (double) data.consumeInt();
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(w, x, y);
            }
        }

        fitter.fit();
    }
}