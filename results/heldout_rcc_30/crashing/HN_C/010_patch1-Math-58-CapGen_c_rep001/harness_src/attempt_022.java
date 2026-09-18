package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int initialPoints = data.consumeInt(0, 40);
        for (int i = 0; i < initialPoints; i++) {
            double x = fuzzDouble(data);
            double y = fuzzDouble(data);
            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight = fuzzDouble(data);
                fitter.addObservedPoint(weight, x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
            int secondPoints = data.consumeInt(0, 40);
            for (int i = 0; i < secondPoints; i++) {
                double x = fuzzDouble(data);
                double y = fuzzDouble(data);
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    double weight = fuzzDouble(data);
                    fitter.addObservedPoint(weight, x, y);
                }
            }
        }

        if (data.consumeBoolean()) {
            WeightedObservedPoint[] observations = fitter.getObservations();
            if (observations.length > 1 && data.consumeBoolean()) {
                for (int i = 0, j = observations.length - 1; i < j; i++, j--) {
                    WeightedObservedPoint tmp = observations[i];
                    observations[i] = observations[j];
                    observations[j] = tmp;
                }
            }
        }

        fitter.fit();
    }

    private static double fuzzDouble(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 9)) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return Double.NaN;
            case 3:
                return Double.POSITIVE_INFINITY;
            case 4:
                return Double.NEGATIVE_INFINITY;
            case 5:
                return (double) data.consumeInt();
            case 6: {
                long hi = ((long) data.consumeInt()) << 32;
                long lo = data.consumeInt() & 0xffffffffL;
                return Double.longBitsToDouble(hi | lo);
            }
            case 7: {
                int numerator = data.consumeInt();
                int denominator = data.consumeInt();
                if (denominator == 0) {
                    denominator = 1;
                }
                return ((double) numerator) / denominator;
            }
            case 8:
                return Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
            default:
                return data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
        }
    }
}