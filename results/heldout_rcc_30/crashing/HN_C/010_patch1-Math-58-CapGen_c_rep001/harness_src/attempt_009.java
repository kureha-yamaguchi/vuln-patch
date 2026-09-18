package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int phases = data.consumeInt(1, 3);
        for (int phase = 0; phase < phases; phase++) {
            int count = data.consumeInt(0, 64);

            for (int i = 0; i < count; i++) {
                int selector = data.consumeInt(0, 15);
                int wx = data.consumeInt();
                int xx = data.consumeInt();
                int yx = data.consumeInt();

                double weight;
                switch (selector & 7) {
                    case 0:
                        weight = wx;
                        break;
                    case 1:
                        weight = -wx;
                        break;
                    case 2:
                        weight = 0.0;
                        break;
                    case 3:
                        weight = 1.0;
                        break;
                    case 4:
                        weight = -1.0;
                        break;
                    case 5:
                        weight = Double.NaN;
                        break;
                    case 6:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                }

                double x;
                switch ((selector >> 1) & 7) {
                    case 0:
                        x = xx;
                        break;
                    case 1:
                        x = xx / 10.0;
                        break;
                    case 2:
                        x = 0.0;
                        break;
                    case 3:
                        x = -0.0;
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
                        x = (double) (xx % 5);
                        break;
                }

                double y;
                switch ((selector >> 2) & 7) {
                    case 0:
                        y = yx;
                        break;
                    case 1:
                        y = yx / 10.0;
                        break;
                    case 2:
                        y = 0.0;
                        break;
                    case 3:
                        y = -0.0;
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
                        y = (double) (yx % 5);
                        break;
                }

                int addMode = data.consumeInt(0, 2);
                if (addMode == 0) {
                    fitter.addObservedPoint(x, y);
                } else if (addMode == 1) {
                    fitter.addObservedPoint(weight, x, y);
                } else {
                    fitter.addObservedPoint(new WeightedObservedPoint(weight, x, y));
                }
            }

            if (data.consumeBoolean()) {
                fitter.clearObservations();
            }
        }

        if (data.consumeBoolean()) {
            double repeatedX;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    repeatedX = 0.0;
                    break;
                case 1:
                    repeatedX = -0.0;
                    break;
                case 2:
                    repeatedX = data.consumeInt();
                    break;
                case 3:
                    repeatedX = data.consumeInt() / 1000.0;
                    break;
                case 4:
                    repeatedX = Double.POSITIVE_INFINITY;
                    break;
                default:
                    repeatedX = Double.NaN;
                    break;
            }
            int extra = data.consumeInt(0, 8);
            for (int i = 0; i < extra; i++) {
                double y = (i % 2 == 0) ? i : -i;
                fitter.addObservedPoint(repeatedX, y);
            }
        }

        fitter.fit();
    }
}