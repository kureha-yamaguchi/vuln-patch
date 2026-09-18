package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        int phases = data.consumeInt(1, 3);
        for (int phase = 0; phase < phases; phase++) {
            if (data.consumeBoolean()) {
                fitter.clearObservations();
            }

            int count = data.consumeInt(0, 32);
            for (int i = 0; i < count; i++) {
                int ai = data.consumeInt();
                int bi = data.consumeInt();
                int ci = data.consumeInt();
                int di = data.consumeInt();

                double x;
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        x = ai;
                        break;
                    case 1:
                        x = bi == 0 ? ai / 0.0 : ((double) ai) / bi;
                        break;
                    case 2:
                        x = (double) (ai % 3 == 0 ? 0 : ai % 1024);
                        break;
                    case 3:
                        x = -0.0;
                        break;
                    case 4:
                        x = 0.0;
                        break;
                    case 5:
                        x = Math.abs(ai);
                        break;
                    case 6:
                        x = -Math.abs(ai);
                        break;
                    case 7:
                        x = ((double) ai) * ((double) bi);
                        break;
                    case 8:
                        x = ((double) ai) / (data.consumeBoolean() ? 0.0 : 1.0);
                        break;
                    default:
                        x = (double) (byte) data.consumeByte();
                        break;
                }

                double y;
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        y = ci;
                        break;
                    case 1:
                        y = di == 0 ? ci / 0.0 : ((double) ci) / di;
                        break;
                    case 2:
                        y = (double) (ci % 3 == 0 ? 0 : ci % 1024);
                        break;
                    case 3:
                        y = -0.0;
                        break;
                    case 4:
                        y = 0.0;
                        break;
                    case 5:
                        y = Math.abs(ci);
                        break;
                    case 6:
                        y = -Math.abs(ci);
                        break;
                    case 7:
                        y = ((double) ci) * ((double) di);
                        break;
                    case 8:
                        y = ((double) ci) / (data.consumeBoolean() ? 0.0 : 1.0);
                        break;
                    default:
                        y = (double) (byte) data.consumeByte();
                        break;
                }

                double w;
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        w = 1.0;
                        break;
                    case 1:
                        w = 0.0;
                        break;
                    case 2:
                        w = -1.0;
                        break;
                    case 3:
                        w = ai;
                        break;
                    case 4:
                        w = bi == 0 ? ai / 0.0 : ((double) ai) / bi;
                        break;
                    case 5:
                        w = Math.abs(ai);
                        break;
                    case 6:
                        w = -Math.abs(ai);
                        break;
                    case 7:
                        w = ((double) ai) * ((double) bi);
                        break;
                    case 8:
                        w = ((double) ai) / (data.consumeBoolean() ? 0.0 : 1.0);
                        break;
                    default:
                        w = (double) (byte) data.consumeByte();
                        break;
                }

                switch (data.consumeInt(0, 2)) {
                    case 0:
                        fitter.addObservedPoint(x, y);
                        break;
                    case 1:
                        fitter.addObservedPoint(w, x, y);
                        break;
                    default:
                        fitter.addObservedPoint(new WeightedObservedPoint(w, x, y));
                        break;
                }
            }
        }

        fitter.fit();
    }
}