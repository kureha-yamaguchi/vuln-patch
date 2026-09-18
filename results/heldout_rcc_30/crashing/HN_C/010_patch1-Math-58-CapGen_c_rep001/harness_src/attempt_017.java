package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int phases = data.consumeInt(1, 3);
        for (int phase = 0; phase < phases; phase++) {
            if (phase > 0 && data.consumeBoolean()) {
                fitter.clearObservations();
            }

            int points = data.consumeInt(0, 40);
            for (int i = 0; i < points; i++) {
                double[] vals = new double[3];
                for (int j = 0; j < vals.length; j++) {
                    int selector = data.consumeInt(0, 11);
                    double v;
                    switch (selector) {
                        case 0:
                            v = 0.0d;
                            break;
                        case 1:
                            v = -0.0d;
                            break;
                        case 2:
                            v = 1.0d;
                            break;
                        case 3:
                            v = -1.0d;
                            break;
                        case 4:
                            v = (double) Integer.MIN_VALUE;
                            break;
                        case 5:
                            v = (double) Integer.MAX_VALUE;
                            break;
                        case 6:
                            v = (double) data.consumeInt();
                            break;
                        case 7:
                            v = (double) data.consumeByte();
                            break;
                        case 8:
                            v = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                            break;
                        case 9:
                            v = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.MIN_VALUE;
                            break;
                        case 10: {
                            int numerator = data.consumeInt();
                            int denomSelector = data.consumeInt(0, 3);
                            double denominator;
                            switch (denomSelector) {
                                case 0:
                                    denominator = 0.0d;
                                    break;
                                case 1:
                                    denominator = -0.0d;
                                    break;
                                case 2:
                                    denominator = 1.0d;
                                    break;
                                default:
                                    denominator = (double) data.consumeByte();
                                    break;
                            }
                            v = ((double) numerator) / denominator;
                            break;
                        }
                        default:
                            v = Math.scalb((double) data.consumeByte(), data.consumeInt(-32, 32));
                            break;
                    }
                    vals[j] = v;
                }

                double weight = vals[0];
                double x = vals[1];
                double y = vals[2];

                int addMode = data.consumeInt(0, 2);
                if (addMode == 0) {
                    fitter.addObservedPoint(x, y);
                } else if (addMode == 1) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }
        }

        if (data.consumeBoolean()) {
            fitter.clearObservations();
        }

        int tailPoints = data.consumeInt(0, 8);
        for (int i = 0; i < tailPoints; i++) {
            double weight = data.consumeBoolean() ? data.consumeInt() : (double) data.consumeByte();
            double x = data.consumeBoolean() ? data.consumeInt() : (double) data.consumeByte();
            double y = data.consumeBoolean() ? data.consumeInt() : (double) data.consumeByte();
            fitter.addObservedPoint(weight, x, y);
        }

        fitter.fit();
    }
}