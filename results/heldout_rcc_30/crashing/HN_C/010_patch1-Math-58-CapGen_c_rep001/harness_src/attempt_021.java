package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int mode = data.consumeInt(0, 5);
        int n;

        if (mode == 0) {
            n = 0;
        } else if (mode == 1) {
            n = 1;
        } else if (mode == 2) {
            n = 2;
        } else if (mode == 3) {
            n = 3;
        } else {
            n = data.consumeInt(0, 64);
        }

        for (int i = 0; i < n; i++) {
            double weight;
            double x;
            double y;

            int pointStyle = data.consumeInt(0, 7);
            switch (pointStyle) {
                case 0:
                    weight = data.consumeInt();
                    x = data.consumeInt();
                    y = data.consumeInt();
                    break;
                case 1:
                    weight = data.consumeBoolean() ? 1.0 : -1.0;
                    x = i;
                    y = data.consumeByte();
                    break;
                case 2:
                    weight = data.consumeBoolean() ? 0.0 : 1.0;
                    x = 0.0;
                    y = 0.0;
                    break;
                case 3:
                    weight = data.consumeInt(-10, 10);
                    x = data.consumeInt(-1000, 1000);
                    y = data.consumeInt(-1000, 1000);
                    break;
                case 4:
                    weight = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                    x = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.NaN;
                    y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NaN;
                    break;
                case 5:
                    weight = data.consumeInt();
                    x = data.consumeBoolean() ? i : -i;
                    y = x;
                    break;
                case 6:
                    weight = data.consumeInt(-2, 2);
                    x = data.consumeInt();
                    y = data.consumeBoolean() ? x : -x;
                    break;
                default:
                    weight = data.consumeByte();
                    x = data.consumeByte();
                    y = data.consumeByte();
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(weight, x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
        } else {
            if (data.consumeBoolean()) {
                fitter.clearObservations();
            }

            int m = data.consumeInt(0, 16);
            for (int i = 0; i < m; i++) {
                double weight = data.consumeBoolean() ? data.consumeInt() : data.consumeByte();
                double x = data.consumeBoolean() ? data.consumeInt() : data.consumeByte();
                double y = data.consumeBoolean() ? data.consumeInt() : data.consumeByte();

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(weight, x, y);
                }
            }

            fitter.fit();
        }
    }
}