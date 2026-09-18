package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int pointCount = data.consumeInt(0, 32);

        for (int i = 0; i < pointCount; i++) {
            long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double x;
            double y;
            double w;

            switch (Math.floorMod(data.consumeInt(), 6)) {
                case 0:
                    x = Double.longBitsToDouble(xb);
                    y = Double.longBitsToDouble(yb);
                    w = Double.longBitsToDouble(wb);
                    break;
                case 1:
                    x = data.consumeInt();
                    y = data.consumeInt();
                    w = data.consumeInt();
                    break;
                case 2:
                    int xd = data.consumeInt();
                    int yd = data.consumeInt();
                    int wd = data.consumeInt();
                    x = xd == 0 ? 0.0 : ((double) data.consumeInt()) / xd;
                    y = yd == 0 ? 0.0 : ((double) data.consumeInt()) / yd;
                    w = wd == 0 ? 0.0 : ((double) data.consumeInt()) / wd;
                    break;
                case 3:
                    x = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    y = Double.longBitsToDouble(yb);
                    w = data.consumeBoolean() ? 0.0 : Double.longBitsToDouble(wb);
                    break;
                case 4:
                    x = (double) data.consumeByte();
                    y = (double) data.consumeByte();
                    w = (double) data.consumeByte();
                    break;
                default:
                    x = Double.longBitsToDouble(xb);
                    y = data.remainingBytes();
                    w = data.consumeBoolean() ? 1.0 : -1.0;
                    break;
            }

            switch (Math.floorMod(data.consumeInt(), 3)) {
                case 0:
                    fitter.addObservedPoint(x, y);
                    break;
                case 1:
                    fitter.addObservedPoint(w, x, y);
                    break;
                default:
                    fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
                    break;
            }

            if (data.consumeBoolean() && data.consumeBoolean()) {
                fitter.clearObservations();
            }
        }

        if (data.consumeBoolean()) {
            int extraPoints = data.consumeInt(0, 8);
            for (int i = 0; i < extraPoints; i++) {
                double x = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                double y = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                fitter.addObservedPoint(x, y);
            }
        }

        double[] params = fitter.fit();

        if (params != null && params.length > 0 && data.consumeBoolean()) {
            fitter.fit(new org.apache.commons.math.analysis.function.Gaussian.Parametric(), params);
        }
    }
}