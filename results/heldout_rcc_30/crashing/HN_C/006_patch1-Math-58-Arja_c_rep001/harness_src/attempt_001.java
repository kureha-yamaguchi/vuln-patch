package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int firstBatch = data.consumeInt(0, 64);
        for (int i = 0; i < firstBatch; i++) {
            double x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
                case 1:
                    x = data.consumeInt();
                    break;
                case 2:
                    x = 0.0d;
                    break;
                case 3:
                    x = -0.0d;
                    break;
                case 4:
                    x = Double.NaN;
                    break;
                case 5:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    x = (double) data.consumeByte();
                    break;
            }

            double y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = (double) Float.intBitsToFloat(data.consumeInt());
                    break;
                case 1:
                    y = data.consumeInt();
                    break;
                case 2:
                    y = 0.0d;
                    break;
                case 3:
                    y = -0.0d;
                    break;
                case 4:
                    y = Double.NaN;
                    break;
                case 5:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    y = (double) data.consumeByte();
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight;
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        weight = (double) Float.intBitsToFloat(data.consumeInt());
                        break;
                    case 1:
                        weight = data.consumeInt();
                        break;
                    case 2:
                        weight = 0.0d;
                        break;
                    case 3:
                        weight = -0.0d;
                        break;
                    case 4:
                        weight = -1.0d;
                        break;
                    case 5:
                        weight = 1.0d;
                        break;
                    case 6:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        weight = Double.NaN;
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
        }

        int secondBatch = data.consumeInt(0, 64);
        for (int i = 0; i < secondBatch; i++) {
            double x;
            if (data.consumeBoolean()) {
                x = (double) Float.intBitsToFloat(data.consumeInt());
            } else {
                x = data.consumeInt(data.consumeBoolean() ? -4 : Integer.MIN_VALUE, data.consumeBoolean() ? 4 : Integer.MAX_VALUE);
            }

            double y;
            if (data.consumeBoolean()) {
                y = (double) Float.intBitsToFloat(data.consumeInt());
            } else {
                y = data.consumeInt(data.consumeBoolean() ? -4 : Integer.MIN_VALUE, data.consumeBoolean() ? 4 : Integer.MAX_VALUE);
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight = data.consumeBoolean() ? (double) Float.intBitsToFloat(data.consumeInt()) : (double) data.consumeInt();
                fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
            }
        }

        fitter.fit();
    }
}