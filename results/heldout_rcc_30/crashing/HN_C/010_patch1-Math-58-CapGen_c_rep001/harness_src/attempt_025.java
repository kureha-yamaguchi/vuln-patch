package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int observationCount = data.consumeInt(0, 64);
        for (int i = 0; i < observationCount; i++) {
            long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double x = Double.longBitsToDouble(xBits);
            double y = Double.longBitsToDouble(yBits);

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double weight = Double.longBitsToDouble(wBits);
                fitter.addObservedPoint(weight, x, y);
            }
        }

        if (data.consumeBoolean()) {
            long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            fitter.addObservedPoint(Double.longBitsToDouble(xBits), Double.longBitsToDouble(yBits));
        }

        if (data.consumeBoolean()) {
            int mode = data.consumeInt(0, 5);
            if (mode == 0) {
                fitter.addObservedPoint(1.0, 0.0, 0.0);
            } else if (mode == 1) {
                fitter.addObservedPoint(1.0, 0.0, 1.0);
            } else if (mode == 2) {
                fitter.addObservedPoint(1.0, -1.0, 1.0);
                fitter.addObservedPoint(1.0, 0.0, 0.0);
                fitter.addObservedPoint(1.0, 1.0, 1.0);
            } else if (mode == 3) {
                fitter.addObservedPoint(1.0, Double.NEGATIVE_INFINITY, 0.0);
                fitter.addObservedPoint(1.0, Double.POSITIVE_INFINITY, 0.0);
            } else if (mode == 4) {
                fitter.addObservedPoint(1.0, Double.NaN, Double.NaN);
            } else {
                fitter.addObservedPoint(-1.0, 0.0, 1.0);
            }
        }

        fitter.fit();
    }
}