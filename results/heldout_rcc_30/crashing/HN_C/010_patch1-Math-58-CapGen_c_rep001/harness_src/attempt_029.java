package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int observationCount = data.consumeInt(0, 64);
        for (int i = 0; i < observationCount; i++) {
            double x;
            double y;
            double weight;

            int xKind = data.consumeInt(0, 11);
            switch (xKind) {
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
                case 9:
                    x = (double) data.consumeInt();
                    break;
                case 10:
                    x = ((double) data.consumeInt()) / 1024.0d;
                    break;
                default:
                    x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            int yKind = data.consumeInt(0, 11);
            switch (yKind) {
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
                case 9:
                    y = (double) data.consumeInt();
                    break;
                case 10:
                    y = ((double) data.consumeInt()) / 1024.0d;
                    break;
                default:
                    y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            int wKind = data.consumeInt(0, 11);
            switch (wKind) {
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
                case 5:
                    weight = 1.0d;
                    break;
                case 6:
                    weight = -1.0d;
                    break;
                case 7:
                    weight = Double.MIN_VALUE;
                    break;
                case 8:
                    weight = Double.MAX_VALUE;
                    break;
                case 9:
                    weight = (double) data.consumeInt();
                    break;
                case 10:
                    weight = ((double) data.consumeInt()) / 1024.0d;
                    break;
                default:
                    weight = Double.longBitsToDouble((((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }

            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                fitter.addObservedPoint(x, y);
            } else if (mode == 1) {
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();

            int extraCount = data.consumeInt(0, 16);
            for (int i = 0; i < extraCount; i++) {
                double x;
                if (data.consumeBoolean()) {
                    x = i;
                } else if (data.consumeBoolean()) {
                    x = -i;
                } else if (data.consumeBoolean()) {
                    x = data.consumeInt();
                } else {
                    x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                }

                double y;
                if (data.consumeBoolean()) {
                    y = i * i;
                } else if (data.consumeBoolean()) {
                    y = -(i * i);
                } else if (data.consumeBoolean()) {
                    y = data.consumeInt();
                } else {
                    y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                }

                double w;
                if (data.consumeBoolean()) {
                    w = 1.0d;
                } else if (data.consumeBoolean()) {
                    w = -1.0d;
                } else if (data.consumeBoolean()) {
                    w = 0.0d;
                } else {
                    w = Double.longBitsToDouble((((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(w, x, y);
                } else if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
                }
            }
        }

        fitter.fit();
    }
}