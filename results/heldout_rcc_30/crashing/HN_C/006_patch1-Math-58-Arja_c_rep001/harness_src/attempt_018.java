package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int rounds = data.consumeInt(1, 3);
        for (int r = 0; r < rounds; r++) {
            if (r > 0 && data.consumeBoolean()) {
                fitter.clearObservations();
            }

            int n = data.consumeInt(0, 32);
            for (int i = 0; i < n; i++) {
                long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

                double x = Double.longBitsToDouble(xBits);
                double y = Double.longBitsToDouble(yBits);
                double w = Double.longBitsToDouble(wBits);

                if (data.consumeBoolean()) {
                    x = data.consumeInt(-16, 16);
                }
                if (data.consumeBoolean()) {
                    y = data.consumeInt(-16, 16);
                }
                if (data.consumeBoolean()) {
                    w = data.consumeInt(-16, 16);
                }

                int mode = data.consumeInt(0, 2);
                if (mode == 0) {
                    fitter.addObservedPoint(x, y);
                } else if (mode == 1) {
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
                }
            }
        }

        fitter.fit();
    }
}