package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int phases = data.consumeInt(1, 3);
        for (int phase = 0; phase < phases; phase++) {
            if (phase > 0 && data.consumeBoolean()) {
                fitter.clearObservations();
            }

            int points = data.consumeInt(0, 32);
            for (int i = 0; i < points; i++) {
                long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

                double x = Double.longBitsToDouble(xBits);
                double y = Double.longBitsToDouble(yBits);
                double w = Double.longBitsToDouble(wBits);

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(w, x, y);
                }
            }

            if (data.consumeBoolean()) {
                fitter.fit();
            }
        }

        fitter.fit();
    }
}