package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int observationCount = data.consumeInt(0, 32);
        for (int i = 0; i < observationCount; i++) {
            int xKind = data.consumeInt(0, 9);
            int xA = data.consumeInt();
            int xB = data.consumeInt();
            double x;
            switch (xKind) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = Double.NaN;
                    break;
                case 3:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    x = (double) xA;
                    break;
                case 6:
                    x = ((double) xA) / (xB == 0 ? 1.0d : (double) xB);
                    break;
                case 7:
                    x = Math.scalb((double) xA, data.consumeInt(-1074, 1023));
                    break;
                case 8:
                    x = Double.longBitsToDouble((((long) xA) << 32) ^ (xB & 0xffffffffL));
                    break;
                default:
                    x = (double) data.consumeByte();
                    break;
            }

            int yKind = data.consumeInt(0, 9);
            int yA = data.consumeInt();
            int yB = data.consumeInt();
            double y;
            switch (yKind) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = Double.NaN;
                    break;
                case 3:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    y = (double) yA;
                    break;
                case 6:
                    y = ((double) yA) / (yB == 0 ? 1.0d : (double) yB);
                    break;
                case 7:
                    y = Math.scalb((double) yA, data.consumeInt(-1074, 1023));
                    break;
                case 8:
                    y = Double.longBitsToDouble((((long) yA) << 32) ^ (yB & 0xffffffffL));
                    break;
                default:
                    y = (double) data.consumeByte();
                    break;
            }

            if (data.consumeBoolean()) {
                int wKind = data.consumeInt(0, 9);
                int wA = data.consumeInt();
                int wB = data.consumeInt();
                double weight;
                switch (wKind) {
                    case 0:
                        weight = 0.0d;
                        break;
                    case 1:
                        weight = -0.0d;
                        break;
                    case 2:
                        weight = Double.NaN;
                        break;
                    case 3:
                        weight = Double.POSITIVE_INFINITY;
                        break;
                    case 4:
                        weight = Double.NEGATIVE_INFINITY;
                        break;
                    case 5:
                        weight = (double) wA;
                        break;
                    case 6:
                        weight = ((double) wA) / (wB == 0 ? 1.0d : (double) wB);
                        break;
                    case 7:
                        weight = Math.scalb((double) wA, data.consumeInt(-1074, 1023));
                        break;
                    case 8:
                        weight = Double.longBitsToDouble((((long) wA) << 32) ^ (wB & 0xffffffffL));
                        break;
                    default:
                        weight = (double) data.consumeByte();
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }
        }

        fitter.fit();
    }
}