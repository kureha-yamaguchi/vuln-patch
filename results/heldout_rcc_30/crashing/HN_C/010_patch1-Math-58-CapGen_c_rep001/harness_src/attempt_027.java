package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int count = data.consumeInt(0, 16);
        for (int i = 0; i < count; i++) {
            long wBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
            long xBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);

            double weight = Double.longBitsToDouble(wBits);
            double x = Double.longBitsToDouble(xBits);
            double y = Double.longBitsToDouble(yBits);

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
            }
        }

        if (data.consumeBoolean()) {
            long xBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
            fitter.addObservedPoint(Double.longBitsToDouble(xBits), Double.longBitsToDouble(yBits));
        }

        fitter.fit();
    }
}