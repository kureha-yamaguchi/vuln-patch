package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int observationCount = data.consumeInt(0, 64);
        int pattern = data.consumeInt(0, 5);

        double center = data.consumeInt();
        double scale = data.consumeInt(-1000, 1000);
        if (scale == 0) {
            scale = 1;
        }
        double amplitude = data.consumeInt();
        double width = Math.abs(data.consumeInt(-1000, 1000)) + 1.0;

        for (int i = 0; i < observationCount; i++) {
            double x;
            double y;
            double weight;

            switch (pattern) {
                case 0:
                    x = i - (observationCount / 2.0);
                    y = amplitude;
                    break;
                case 1:
                    x = center + (i * scale);
                    double dx1 = x - center;
                    y = amplitude * Math.exp(-(dx1 * dx1) / width);
                    break;
                case 2:
                    x = center + data.consumeInt(-50, 50);
                    double dx2 = x - center;
                    y = amplitude * Math.exp(-(dx2 * dx2) / width) + data.consumeInt(-10, 10);
                    break;
                case 3:
                    x = data.consumeInt();
                    y = data.consumeInt();
                    break;
                case 4:
                    x = (double) data.consumeByte();
                    y = (double) data.consumeByte();
                    break;
                default:
                    x = center + ((i % 3) - 1);
                    y = (i % 2 == 0) ? amplitude : -amplitude;
                    break;
            }

            int special = data.consumeInt(0, 15);
            if (special == 0) {
                x = Double.NaN;
            } else if (special == 1) {
                x = Double.POSITIVE_INFINITY;
            } else if (special == 2) {
                x = Double.NEGATIVE_INFINITY;
            } else if (special == 3) {
                y = Double.NaN;
            } else if (special == 4) {
                y = Double.POSITIVE_INFINITY;
            } else if (special == 5) {
                y = Double.NEGATIVE_INFINITY;
            } else if (special == 6) {
                x = 0.0;
                y = 0.0;
            }

            if (data.consumeBoolean()) {
                weight = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    int wSpecial = data.consumeInt(0, 7);
                    if (wSpecial == 0) {
                        weight = 0.0;
                    } else if (wSpecial == 1) {
                        weight = -0.0;
                    } else if (wSpecial == 2) {
                        weight = Double.NaN;
                    } else if (wSpecial == 3) {
                        weight = Double.POSITIVE_INFINITY;
                    } else if (wSpecial == 4) {
                        weight = Double.NEGATIVE_INFINITY;
                    }
                }
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }

            if (data.consumeBoolean() && i > 0) {
                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    fitter.addObservedPoint(1.0, x, y);
                }
            }
        }

        if (data.consumeBoolean()) {
            fitter.fit();
        } else {
            fitter.fit();
        }
    }
}