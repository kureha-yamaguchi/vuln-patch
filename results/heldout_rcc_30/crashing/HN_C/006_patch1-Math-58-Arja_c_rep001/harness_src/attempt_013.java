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

            int points = data.consumeInt(0, 40);
            for (int i = 0; i < points; i++) {
                double weight = fuzzDouble(data);
                double x = fuzzDouble(data);
                double y = fuzzDouble(data);

                switch (data.consumeInt(0, 2)) {
                    case 0:
                        fitter.addObservedPoint(x, y);
                        break;
                    case 1:
                        fitter.addObservedPoint(weight, x, y);
                        break;
                    default:
                        fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                        break;
                }
            }

            if (data.consumeBoolean()) {
                fitter.getObservations();
            }

            if (data.consumeBoolean()) {
                fitter.fit();
            }
        }

        fitter.fit();
    }

    private static double fuzzDouble(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 11)) {
            case 0:
                return Double.NaN;
            case 1:
                return Double.POSITIVE_INFINITY;
            case 2:
                return Double.NEGATIVE_INFINITY;
            case 3:
                return 0.0d;
            case 4:
                return -0.0d;
            case 5:
                return Double.MAX_VALUE;
            case 6:
                return -Double.MAX_VALUE;
            case 7:
                return Double.MIN_VALUE;
            case 8:
                return -Double.MIN_VALUE;
            case 9:
                return (double) data.consumeInt();
            case 10:
                return ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 1024.0d);
            default:
                long high = ((long) data.consumeInt()) << 32;
                long low = data.consumeInt() & 0xffffffffL;
                return Double.longBitsToDouble(high | low);
        }
    }
}