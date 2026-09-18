package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int pointCount = data.consumeInt(0, 32);
        for (int i = 0; i < pointCount; i++) {
            int ax = data.consumeInt();
            int bx = data.consumeInt();
            int ay = data.consumeInt();
            int by = data.consumeInt();

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
                    x = (double) ax;
                    break;
                case 6:
                    x = bx == 0 ? (double) ax : ((double) ax) / ((double) bx);
                    break;
                case 7:
                    x = Math.scalb((double) ax, data.consumeInt(-20, 20));
                    break;
                case 8: {
                    byte[] xb = data.consumeBytes(8);
                    long bits = 0L;
                    for (int j = 0; j < xb.length; j++) {
                        bits = (bits << 8) | (xb[j] & 0xffL);
                    }
                    x = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    x = (double) (ax - bx);
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
                    y = (double) ay;
                    break;
                case 6:
                    y = by == 0 ? (double) ay : ((double) ay) / ((double) by);
                    break;
                case 7:
                    y = Math.scalb((double) ay, data.consumeInt(-20, 20));
                    break;
                case 8: {
                    byte[] yb = data.consumeBytes(8);
                    long bits = 0L;
                    for (int j = 0; j < yb.length; j++) {
                        bits = (bits << 8) | (yb[j] & 0xffL);
                    }
                    y = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    y = (double) (ay - by);
                    break;
            }

            int aw = data.consumeInt();
            int bw = data.consumeInt();
            double weight;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    weight = 1.0d;
                    break;
                case 1:
                    weight = 0.0d;
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
                    weight = (double) aw;
                    break;
                case 7:
                    weight = bw == 0 ? (double) aw : ((double) aw) / ((double) bw);
                    break;
                case 8: {
                    byte[] wb = data.consumeBytes(8);
                    long bits = 0L;
                    for (int j = 0; j < wb.length; j++) {
                        bits = (bits << 8) | (wb[j] & 0xffL);
                    }
                    weight = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    weight = Math.scalb((double) aw, data.consumeInt(-20, 20));
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
            fitter.fit();
            return;
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
            int extraCount = data.consumeInt(0, 16);
            for (int i = 0; i < extraCount; i++) {
                int xNum = data.consumeInt();
                int xDen = data.consumeInt();
                int yNum = data.consumeInt();
                int yDen = data.consumeInt();
                int wNum = data.consumeInt();
                int wDen = data.consumeInt();

                double x = xDen == 0 ? (double) xNum : ((double) xNum) / ((double) xDen);
                double y = yDen == 0 ? (double) yNum : ((double) yNum) / ((double) yDen);
                double w = wDen == 0 ? (double) wNum : ((double) wNum) / ((double) wDen);

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else if (data.consumeBoolean()) {
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
                }
            }
        }

        double[] params = fitter.fit();
        if (params != null && params.length > 0 && data.consumeBoolean()) {
            Gaussian.Parametric parametric = new Gaussian.Parametric();
            int px = data.consumeInt();
            int py = data.consumeInt();
            double probeX = data.consumeBoolean()
                    ? (py == 0 ? (double) px : ((double) px) / ((double) py))
                    : (double) px;
            parametric.value(probeX, params);
        }
    }
}