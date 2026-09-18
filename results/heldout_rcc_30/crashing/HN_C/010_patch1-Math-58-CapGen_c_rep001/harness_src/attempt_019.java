package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int points = data.consumeInt(0, 32);

        for (int i = 0; i < points; i++) {
            double x;
            if (data.consumeBoolean()) {
                x = (double) data.consumeInt();
                if (data.consumeBoolean()) {
                    x = x / (double) data.consumeInt(1, 1024);
                }
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                x = Double.longBitsToDouble(bits);
            }

            double y;
            if (data.consumeBoolean()) {
                y = (double) data.consumeInt();
                if (data.consumeBoolean()) {
                    y = y / (double) data.consumeInt(1, 1024);
                }
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                y = Double.longBitsToDouble(bits);
            }

            double w;
            if (data.consumeBoolean()) {
                w = (double) data.consumeInt();
                if (data.consumeBoolean()) {
                    w = w / (double) data.consumeInt(1, 1024);
                }
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                w = Double.longBitsToDouble(bits);
            }

            int how = data.consumeInt(0, 2);
            if (how == 0) {
                fitter.addObservedPoint(x, y);
            } else if (how == 1) {
                fitter.addObservedPoint(w, x, y);
            } else {
                fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
            }
        }

        fitter.fit();
    }
}