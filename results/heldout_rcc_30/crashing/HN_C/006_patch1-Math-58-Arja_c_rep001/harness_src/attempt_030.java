package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int count = data.consumeInt(0, 64);
        for (int i = 0; i < count; i++) {
            if (data.consumeBoolean()) {
                fitter.clearObservations();
            }

            long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double x = Double.longBitsToDouble(xb);
            double y = Double.longBitsToDouble(yb);
            double w = Double.longBitsToDouble(wb);

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(w, x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
            int extraCount = data.consumeInt(0, 16);
            for (int i = 0; i < extraCount; i++) {
                long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long wb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

                double x = Double.longBitsToDouble(xb);
                double y = Double.longBitsToDouble(yb);
                double w = Double.longBitsToDouble(wb);

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