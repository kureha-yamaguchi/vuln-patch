package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int preludeOps = data.consumeInt(0, 4);
        for (int i = 0; i < preludeOps; i++) {
            if (data.consumeBoolean()) {
                fitter.clearObservations();
            } else {
                double x;
                switch (data.consumeInt(0, 9)) {
                    case 0:  x = Double.NaN; break;
                    case 1:  x = Double.POSITIVE_INFINITY; break;
                    case 2:  x = Double.NEGATIVE_INFINITY; break;
                    case 3:  x = 0.0d; break;
                    case 4:  x = -0.0d; break;
                    case 5:  x = Double.MAX_VALUE; break;
                    case 6:  x = -Double.MAX_VALUE; break;
                    case 7:  x = Double.MIN_VALUE; break;
                    case 8:  x = -Double.MIN_VALUE; break;
                    default:
                        x = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }

                double y;
                switch (data.consumeInt(0, 9)) {
                    case 0:  y = Double.NaN; break;
                    case 1:  y = Double.POSITIVE_INFINITY; break;
                    case 2:  y = Double.NEGATIVE_INFINITY; break;
                    case 3:  y = 0.0d; break;
                    case 4:  y = -0.0d; break;
                    case 5:  y = Double.MAX_VALUE; break;
                    case 6:  y = -Double.MAX_VALUE; break;
                    case 7:  y = Double.MIN_VALUE; break;
                    case 8:  y = -Double.MIN_VALUE; break;
                    default:
                        y = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    double w;
                    switch (data.consumeInt(0, 9)) {
                        case 0:  w = Double.NaN; break;
                        case 1:  w = Double.POSITIVE_INFINITY; break;
                        case 2:  w = Double.NEGATIVE_INFINITY; break;
                        case 3:  w = 0.0d; break;
                        case 4:  w = -0.0d; break;
                        case 5:  w = 1.0d; break;
                        case 6:  w = -1.0d; break;
                        case 7:  w = Double.MAX_VALUE; break;
                        case 8:  w = Double.MIN_VALUE; break;
                        default:
                            w = Double.longBitsToDouble(
                                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                            break;
                    }
                    fitter.addObservedPoint(w, x, y);
                }
            }
        }

        int n = data.consumeInt(0, 64);
        boolean monotonicX = data.consumeBoolean();
        boolean duplicateX = data.consumeBoolean();
        double currentX = 0.0d;

        for (int i = 0; i < n; i++) {
            double x;
            if (duplicateX && i > 0 && data.consumeBoolean()) {
                x = currentX;
            } else if (monotonicX) {
                int step = data.consumeInt(-3, 3);
                currentX += step;
                if (data.consumeBoolean()) {
                    currentX += data.consumeByte();
                }
                x = currentX;
            } else {
                switch (data.consumeInt(0, 11)) {
                    case 0:  x = Double.NaN; break;
                    case 1:  x = Double.POSITIVE_INFINITY; break;
                    case 2:  x = Double.NEGATIVE_INFINITY; break;
                    case 3:  x = 0.0d; break;
                    case 4:  x = -0.0d; break;
                    case 5:  x = i; break;
                    case 6:  x = -i; break;
                    case 7:  x = Double.MAX_VALUE; break;
                    case 8:  x = -Double.MAX_VALUE; break;
                    case 9:  x = Double.MIN_VALUE; break;
                    case 10: x = -Double.MIN_VALUE; break;
                    default:
                        x = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }
                currentX = x;
            }

            double y;
            switch (data.consumeInt(0, 11)) {
                case 0:  y = Double.NaN; break;
                case 1:  y = Double.POSITIVE_INFINITY; break;
                case 2:  y = Double.NEGATIVE_INFINITY; break;
                case 3:  y = 0.0d; break;
                case 4:  y = -0.0d; break;
                case 5:  y = i; break;
                case 6:  y = -i; break;
                case 7:  y = Double.MAX_VALUE; break;
                case 8:  y = -Double.MAX_VALUE; break;
                case 9:  y = Double.MIN_VALUE; break;
                case 10: y = -Double.MIN_VALUE; break;
                default:
                    y = Double.longBitsToDouble(
                            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double w;
                switch (data.consumeInt(0, 11)) {
                    case 0:  w = Double.NaN; break;
                    case 1:  w = Double.POSITIVE_INFINITY; break;
                    case 2:  w = Double.NEGATIVE_INFINITY; break;
                    case 3:  w = 0.0d; break;
                    case 4:  w = -0.0d; break;
                    case 5:  w = 1.0d; break;
                    case 6:  w = -1.0d; break;
                    case 7:  w = Double.MAX_VALUE; break;
                    case 8:  w = -Double.MAX_VALUE; break;
                    case 9:  w = Double.MIN_VALUE; break;
                    case 10: w = -Double.MIN_VALUE; break;
                    default:
                        w = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }
                fitter.addObservedPoint(w, x, y);
            }
        }

        if (data.consumeBoolean()) {
            fitter.getObservations();
        }

        double[] params = fitter.fit();
        if (params != null && params.length > 0 && data.consumeBoolean()) {
            double sink = 0.0d;
            for (double v : params) {
                sink += v;
            }
            if (sink == 13.37d) {
                throw new RuntimeException("unreachable");
            }
        }
    }
}