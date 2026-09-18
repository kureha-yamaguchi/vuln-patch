package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int observations = data.consumeInt(0, 64);

        for (int i = 0; i < observations; i++) {
            double x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = Double.NaN;
                    break;
                case 1:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 2:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 3:
                    x = 0.0d;
                    break;
                case 4:
                    x = -0.0d;
                    break;
                case 5:
                    x = data.consumeByte();
                    break;
                case 6:
                    x = data.consumeInt();
                    break;
                default:
                    x = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                    break;
            }

            double y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = Double.NaN;
                    break;
                case 1:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 2:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 3:
                    y = 0.0d;
                    break;
                case 4:
                    y = -0.0d;
                    break;
                case 5:
                    y = data.consumeByte();
                    break;
                case 6:
                    y = data.consumeInt();
                    break;
                default:
                    y = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight;
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        weight = Double.NaN;
                        break;
                    case 1:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 2:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 3:
                        weight = 0.0d;
                        break;
                    case 4:
                        weight = -0.0d;
                        break;
                    case 5:
                        weight = data.consumeByte();
                        break;
                    case 6:
                        weight = data.consumeInt();
                        break;
                    default:
                        weight = Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            }
        }

        if (data.consumeBoolean()) {
            double duplicateX = data.consumeBoolean() ? 0.0d : Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
            double duplicateY = data.consumeBoolean() ? 0.0d : Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023));
            int duplicates = data.consumeInt(0, 8);
            for (int i = 0; i < duplicates; i++) {
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(duplicateX, duplicateY);
                } else {
                    fitter.addObservedPoint(
                            data.consumeBoolean() ? 1.0d : Math.scalb((double) data.consumeInt(), data.consumeInt(-1074, 1023)),
                            duplicateX,
                            duplicateY);
                }
            }
        }

        fitter.fit();
    }
}