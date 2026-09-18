package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        if (data.consumeBoolean()) {
            fitter.clearObservations();
        }

        int pointCount = data.consumeInt(0, 64);
        for (int i = 0; i < pointCount; i++) {
            long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double x = Double.longBitsToDouble(xb);
            double y = Double.longBitsToDouble(yb);
            double w = Double.longBitsToDouble(wb);

            int mode = data.consumeInt(0, 5);
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
                    fitter.addObservedPoint(new WeightedObservedPoint(1.0, x, y));
                    break;
                case 4:
                    fitter.addObservedPoint(new WeightedObservedPoint(w, x, x));
                    break;
                default:
                    fitter.addObservedPoint(w, y, x);
                    break;
            }

            if (data.consumeBoolean() && data.remainingBytes() > 0) {
                fitter.getObservations();
            }
            if (data.consumeBoolean()) {
                fitter.clearObservations();
            }
        }

        if (data.consumeBoolean()) {
            int extraPoints = data.consumeInt(0, 8);
            for (int i = 0; i < extraPoints; i++) {
                int base = data.consumeInt();
                double x = base;
                double y = data.consumeBoolean() ? base : -base;
                double w = data.consumeBoolean() ? 1.0 : 0.0;
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(w, x, y);
                }
            }
        }

        fitter.fit();
    }
}