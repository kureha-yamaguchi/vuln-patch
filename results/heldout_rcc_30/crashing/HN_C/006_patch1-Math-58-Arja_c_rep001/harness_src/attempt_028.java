package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int points = data.consumeInt(0, 32);
        for (int i = 0; i < points; i++) {
            double x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 3:
                    x = Double.NaN;
                    break;
                case 4:
                    x = (double) data.consumeInt();
                    break;
                case 5:
                    x = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
                case 6:
                    x = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
                    break;
                default:
                    int xn = data.consumeInt();
                    int xd = data.consumeInt();
                    x = xd == 0 ? (double) xn : ((double) xn) / ((double) xd);
                    break;
            }

            double y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 3:
                    y = Double.NaN;
                    break;
                case 4:
                    y = (double) data.consumeInt();
                    break;
                case 5:
                    y = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
                case 6:
                    y = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
                    break;
                default:
                    int yn = data.consumeInt();
                    int yd = data.consumeInt();
                    y = yd == 0 ? (double) yn : ((double) yn) / ((double) yd);
                    break;
            }

            if (data.consumeBoolean()) {
                double w;
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        w = 0.0d;
                        break;
                    case 1:
                        w = -0.0d;
                        break;
                    case 2:
                        w = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        w = Double.NaN;
                        break;
                    case 4:
                        w = (double) data.consumeInt();
                        break;
                    case 5:
                        w = (double) Float.intBitsToFloat(data.consumeInt());
                        break;
                    case 6:
                        w = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
                        break;
                    default:
                        int wn = data.consumeInt();
                        int wd = data.consumeInt();
                        w = wd == 0 ? (double) wn : ((double) wn) / ((double) wd);
                        break;
                }
                fitter.addObservedPoint(w, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }

            if (data.consumeInt(0, 31) == 0) {
                fitter.clearObservations();
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
        }

        if (data.consumeBoolean()) {
            int extraPoints = data.consumeInt(0, 8);
            for (int i = 0; i < extraPoints; i++) {
                double x = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
                double y = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
                if (data.consumeBoolean()) {
                    double w = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        }

        fitter.fit();
    }
}