package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Numbers {
            double anyDouble() {
                long hi = ((long) data.consumeInt()) << 32;
                long lo = data.consumeInt() & 0xffffffffL;
                return Double.longBitsToDouble(hi | lo);
            }

            double smallFiniteDouble() {
                int scale = data.consumeInt(-1000000, 1000000);
                int div = data.consumeInt(1, 1000);
                return ((double) scale) / div;
            }
        }

        Numbers numbers = new Numbers();

        for (int scenario = 0; scenario < 3; scenario++) {
            GaussianFitter fitter =
                    new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

            int maxPoints = 20;
            int n = data.consumeInt(0, maxPoints);

            if (scenario == 0) {
                for (int i = 0; i < n; i++) {
                    double x = numbers.anyDouble();
                    double y = numbers.anyDouble();
                    if (data.consumeBoolean()) {
                        fitter.addObservedPoint(x, y);
                    } else {
                        double w = numbers.anyDouble();
                        fitter.addObservedPoint(w, x, y);
                    }
                }
            } else if (scenario == 1) {
                double x = numbers.smallFiniteDouble();
                for (int i = 0; i < n; i++) {
                    int stepKind = data.consumeInt(-2, 2);
                    if (stepKind > 0) {
                        x += stepKind;
                    } else if (stepKind < 0 && data.consumeBoolean()) {
                        x += stepKind;
                    }
                    double y = data.consumeBoolean() ? numbers.smallFiniteDouble() : numbers.anyDouble();
                    if (data.consumeBoolean()) {
                        fitter.addObservedPoint(x, y);
                    } else {
                        double w = data.consumeBoolean() ? numbers.smallFiniteDouble() : numbers.anyDouble();
                        fitter.addObservedPoint(w, x, y);
                    }
                }
            } else {
                double norm = data.consumeBoolean() ? numbers.smallFiniteDouble() : numbers.anyDouble();
                double mean = data.consumeBoolean() ? numbers.smallFiniteDouble() : numbers.anyDouble();
                double sigma = data.consumeBoolean() ? numbers.smallFiniteDouble() : numbers.anyDouble();

                for (int i = 0; i < n; i++) {
                    double offset = data.consumeInt(-10, 10);
                    double x = mean + offset;
                    double y;
                    if (sigma == 0.0 || Double.isNaN(sigma)) {
                        y = numbers.anyDouble();
                    } else {
                        double z = (x - mean) / sigma;
                        y = norm * Math.exp(-0.5 * z * z);
                        if (data.consumeBoolean()) {
                            y += numbers.smallFiniteDouble();
                        }
                    }

                    if (data.consumeBoolean()) {
                        fitter.addObservedPoint(x, y);
                    } else {
                        double w = data.consumeBoolean() ? numbers.smallFiniteDouble() : numbers.anyDouble();
                        fitter.addObservedPoint(w, x, y);
                    }
                }
            }

            fitter.fit();
        }
    }
}