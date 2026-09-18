package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        if (data.consumeBoolean()) {
            fitter.clearObservations();
        }

        int phases = data.consumeInt(1, 4);
        for (int phase = 0; phase < phases; phase++) {
            int points = data.consumeInt(0, 32);
            for (int i = 0; i < points; i++) {
                double weight = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                double x = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                double y = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));

                switch (data.consumeInt(0, 5)) {
                    case 0:
                        fitter.addObservedPoint(x, y);
                        break;
                    case 1:
                        fitter.addObservedPoint(weight, x, y);
                        break;
                    case 2:
                        fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                        break;
                    case 3:
                        fitter.addObservedPoint(0.0, x, y);
                        break;
                    case 4:
                        fitter.addObservedPoint(weight, 0.0, y);
                        break;
                    default:
                        fitter.addObservedPoint(weight, x, 0.0);
                        break;
                }

                if (data.consumeInt(0, 31) == 0) {
                    fitter.addObservedPoint(weight, x, y);
                }
            }

            if (data.consumeInt(0, 7) == 0) {
                fitter.clearObservations();
            }
        }

        switch (data.consumeInt(0, 4)) {
            case 0:
                break;
            case 1:
                fitter.addObservedPoint(1.0, 0.0, 0.0);
                break;
            case 2:
                fitter.addObservedPoint(1.0, -0.0, -0.0);
                fitter.addObservedPoint(1.0, -0.0, -0.0);
                break;
            case 3:
                fitter.addObservedPoint(Double.NaN, Double.NaN, Double.NaN);
                break;
            default:
                fitter.addObservedPoint(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                break;
        }

        fitter.fit();
    }
}