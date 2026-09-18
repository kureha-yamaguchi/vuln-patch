package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int pointCount = data.consumeInt(0, 64);

        for (int i = 0; i < pointCount; i++) {
            int mode = data.consumeInt(0, 5);

            long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double x = Double.longBitsToDouble(xBits);
            double y = Double.longBitsToDouble(yBits);
            double w = Double.longBitsToDouble(wBits);

            switch (mode) {
                case 0:
                    fitter.addObservedPoint(x, y);
                    break;
                case 1:
                    fitter.addObservedPoint(w, x, y);
                    break;
                case 2:
                    fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
                    break;
                case 3:
                    fitter.addObservedPoint((double) data.consumeByte(), (double) data.consumeByte());
                    break;
                case 4:
                    fitter.addObservedPoint((double) data.consumeByte(), (double) data.consumeByte(), (double) data.consumeByte());
                    break;
                default:
                    if (data.consumeBoolean()) {
                        fitter.addObservedPoint(0.0, x, y);
                    } else {
                        fitter.addObservedPoint(-1.0, x, y);
                    }
                    break;
            }

            if (data.consumeBoolean() && i < pointCount - 1) {
                fitter.clearObservations();
            }
        }

        if (data.consumeBoolean()) {
            int extra = data.consumeInt(0, 8);
            for (int i = 0; i < extra; i++) {
                double x = data.consumeInt();
                double y = data.consumeInt();
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(data.consumeInt(), x, y);
                }
            }
        }

        fitter.fit();
    }
}