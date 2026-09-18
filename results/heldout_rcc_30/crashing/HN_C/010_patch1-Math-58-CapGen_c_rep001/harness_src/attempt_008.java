package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int preOps = data.consumeInt(0, 3);
        for (int i = 0; i < preOps; i++) {
            if (data.consumeBoolean()) {
                fitter.clearObservations();
            } else {
                fitter.getObservations();
            }
        }

        int n = data.consumeInt(0, 32);
        for (int i = 0; i < n; i++) {
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
                    fitter.addObservedPoint((double) data.consumeInt(), x, y);
                    break;
                case 3:
                    fitter.addObservedPoint((double) data.consumeByte(), x, y);
                    break;
                case 4:
                    fitter.addObservedPoint(x, y);
                    if (data.consumeBoolean()) {
                        fitter.getObservations();
                    }
                    break;
                default:
                    fitter.addObservedPoint(w, x, y);
                    if (data.consumeBoolean()) {
                        fitter.clearObservations();
                    }
                    break;
            }
        }

        if (data.consumeBoolean()) {
            fitter.getObservations();
        }

        int tailOps = data.consumeInt(0, 4);
        for (int i = 0; i < tailOps; i++) {
            if (data.consumeBoolean()) {
                long xb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long yb = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double x = Double.longBitsToDouble(xb);
                double y = Double.longBitsToDouble(yb);
                fitter.addObservedPoint(x, y);
            } else if (data.consumeBoolean()) {
                fitter.clearObservations();
            } else {
                fitter.getObservations();
            }
        }

        fitter.fit();
    }
}