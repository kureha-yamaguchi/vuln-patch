package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int pointCount = data.consumeInt(0, 64);
        for (int i = 0; i < pointCount; i++) {
            double weight = fuzzDouble(data);
            double x = fuzzDouble(data);
            double y = fuzzDouble(data);

            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                fitter.addObservedPoint(weight, x, y);
            } else if (mode == 1) {
                fitter.addObservedPoint(x, y);
            } else {
                fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();

            int refillCount = data.consumeInt(0, 32);
            for (int i = 0; i < refillCount; i++) {
                double weight = fuzzDouble(data);
                double x = fuzzDouble(data);
                double y = fuzzDouble(data);

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }
        }

        fitter.fit();
    }

    private static double fuzzDouble(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 15);
        switch (selector) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return 1.0d;
            case 3:
                return -1.0d;
            case 4:
                return Double.NaN;
            case 5:
                return Double.POSITIVE_INFINITY;
            case 6:
                return Double.NEGATIVE_INFINITY;
            case 7:
                return Double.MAX_VALUE;
            case 8:
                return -Double.MAX_VALUE;
            case 9:
                return Double.MIN_VALUE;
            case 10:
                return -Double.MIN_VALUE;
            case 11:
                return Integer.MIN_VALUE;
            case 12:
                return Integer.MAX_VALUE;
            default:
                int numerator = data.consumeInt();
                int denominator = data.consumeInt();
                if (denominator == 0) {
                    return numerator;
                }
                return ((double) numerator) / ((double) denominator);
        }
    }
}