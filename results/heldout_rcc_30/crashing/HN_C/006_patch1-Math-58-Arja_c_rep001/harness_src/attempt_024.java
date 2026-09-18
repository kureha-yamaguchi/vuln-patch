package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
        GaussianFitter fitter = new GaussianFitter(optimizer);

        int firstBatch = data.consumeInt(0, 32);
        for (int i = 0; i < firstBatch; i++) {
            double weight;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    weight = 0.0d;
                    break;
                case 1:
                    weight = -0.0d;
                    break;
                case 2:
                    weight = (double) data.consumeInt(-1000, 1000);
                    break;
                case 3:
                    weight = ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 1024.0d);
                    break;
                case 4:
                    weight = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    weight = Double.NaN;
                    break;
                case 6:
                    weight = ((double) data.consumeByte()) * ((double) data.consumeInt(-16, 16));
                    break;
                default:
                    long wBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                    weight = Double.longBitsToDouble(wBits);
                    break;
            }

            double x;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = (double) data.consumeInt(-10000, 10000);
                    break;
                case 3:
                    x = ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 4096.0d);
                    break;
                case 4:
                    x = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
                case 5:
                    x = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                case 6:
                    x = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    x = Double.NaN;
                    break;
                default:
                    long xBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                    x = Double.longBitsToDouble(xBits);
                    break;
            }

            double y;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = (double) data.consumeInt(-10000, 10000);
                    break;
                case 3:
                    y = ((double) data.consumeInt()) / (data.consumeBoolean() ? 1.0d : 4096.0d);
                    break;
                case 4:
                    y = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
                case 5:
                    y = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                case 6:
                    y = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    y = Double.NaN;
                    break;
                default:
                    long yBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                    y = Double.longBitsToDouble(yBits);
                    break;
            }

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
            fitter.clearObservations();
            int secondBatch = data.consumeInt(0, 32);
            for (int i = 0; i < secondBatch; i++) {
                long xBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                long yBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                long wBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);

                double x = data.consumeBoolean() ? Double.longBitsToDouble(xBits) : (double) data.consumeInt(-256, 256);
                double y = data.consumeBoolean() ? Double.longBitsToDouble(yBits) : (double) data.consumeInt(-256, 256);
                double weight = data.consumeBoolean() ? Double.longBitsToDouble(wBits) : (double) data.consumeInt(-16, 16);

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }
        }

        if (data.consumeBoolean()) {
            WeightedObservedPoint[] observations = fitter.getObservations();
            if (observations.length > 0 && data.consumeBoolean()) {
                WeightedObservedPoint p = observations[data.consumeInt(0, observations.length - 1)];
                if (p != null && data.consumeBoolean()) {
                    p.getWeight();
                    p.getX();
                    p.getY();
                }
            }
        }

        fitter.fit();
    }
}