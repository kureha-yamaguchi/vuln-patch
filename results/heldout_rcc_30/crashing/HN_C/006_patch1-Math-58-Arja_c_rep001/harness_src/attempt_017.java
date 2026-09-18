package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter =
                new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        if (data.consumeBoolean()) {
            int meanSeed = data.consumeInt();
            int sigmaSeed = data.consumeInt();
            int normSeed = data.consumeInt();

            double mean;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    mean = meanSeed;
                    break;
                case 1:
                    mean = -meanSeed;
                    break;
                case 2:
                    mean = meanSeed / 10.0;
                    break;
                case 3:
                    mean = (double) (meanSeed % 1000) / 3.0;
                    break;
                case 4:
                    mean = Float.intBitsToFloat(meanSeed);
                    break;
                default:
                    mean = 0.0;
                    break;
            }

            double sigma;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    sigma = Math.abs(sigmaSeed % 50) + 1.0;
                    break;
                case 1:
                    sigma = Math.abs(sigmaSeed / 1000.0) + 1.0e-6;
                    break;
                case 2:
                    sigma = Math.abs(Float.intBitsToFloat(sigmaSeed)) + 1.0e-6;
                    break;
                case 3:
                    sigma = 1.0;
                    break;
                case 4:
                    sigma = 0.0;
                    break;
                case 5:
                    sigma = Double.POSITIVE_INFINITY;
                    break;
                default:
                    sigma = Double.NaN;
                    break;
            }

            double norm;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    norm = Math.abs(normSeed % 1000) + 1.0;
                    break;
                case 1:
                    norm = Math.abs(normSeed / 100.0);
                    break;
                case 2:
                    norm = Math.abs(Float.intBitsToFloat(normSeed));
                    break;
                case 3:
                    norm = 0.0;
                    break;
                case 4:
                    norm = Double.POSITIVE_INFINITY;
                    break;
                default:
                    norm = Double.NaN;
                    break;
            }

            int n = data.consumeInt(0, 12);
            for (int i = 0; i < n; i++) {
                double x;
                if (data.consumeBoolean()) {
                    x = mean + (i - n / 2.0);
                } else {
                    x = mean - (i - n / 2.0);
                }

                double diff = x - mean;
                double y;
                if (sigma == 0.0) {
                    y = diff == 0.0 ? norm : 0.0;
                } else {
                    y = norm * Math.exp(-(diff * diff) / (2.0 * sigma * sigma));
                }

                if (data.consumeBoolean()) {
                    fitter.addObservedPoint(x, y);
                } else {
                    double weight;
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            weight = 1.0;
                            break;
                        case 1:
                            weight = 0.0;
                            break;
                        case 2:
                            weight = -1.0;
                            break;
                        case 3:
                            weight = Float.intBitsToFloat(data.consumeInt());
                            break;
                        default:
                            weight = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                            break;
                    }
                    fitter.addObservedPoint(weight, x, y);
                }
            }
        }

        int extraPoints = data.consumeInt(0, Math.max(0, Math.min(64, data.remainingBytes() + 1)));
        for (int i = 0; i < extraPoints; i++) {
            double x;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    x = data.consumeInt();
                    break;
                case 1:
                    x = -data.consumeInt();
                    break;
                case 2: {
                    int denom = data.consumeInt(-3, 3);
                    x = data.consumeInt() / (double) denom;
                    break;
                }
                case 3:
                    x = Float.intBitsToFloat(data.consumeInt());
                    break;
                case 4:
                    x = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                case 5:
                    x = data.consumeAsciiString(24).length();
                    break;
                case 6:
                    x = 0.0;
                    break;
                case 7:
                    x = (double) (data.consumeInt() % 1000000) / 1000.0;
                    break;
                default:
                    x = data.consumeInt() == 0 ? 0.0 : 1.0 / data.consumeInt();
                    break;
            }

            double y;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    y = data.consumeInt();
                    break;
                case 1:
                    y = -data.consumeInt();
                    break;
                case 2: {
                    int denom = data.consumeInt(-2, 2);
                    y = data.consumeInt() / (double) denom;
                    break;
                }
                case 3:
                    y = Float.intBitsToFloat(data.consumeInt());
                    break;
                case 4:
                    y = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                case 5:
                    y = data.consumeString(24).length();
                    break;
                case 6:
                    y = 0.0;
                    break;
                case 7:
                    y = Math.abs(data.consumeInt() % 1000000) / 1000.0;
                    break;
                case 8:
                    y = Math.exp((data.consumeInt() % 32) - 16);
                    break;
                default:
                    y = data.consumeInt() == 0 ? 0.0 : 1.0 / data.consumeInt();
                    break;
            }

            if (data.consumeBoolean()) {
                fitter.addObservedPoint(x, y);
            } else {
                double weight;
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        weight = 1.0;
                        break;
                    case 1:
                        weight = 0.0;
                        break;
                    case 2:
                        weight = -1.0;
                        break;
                    case 3:
                        weight = data.consumeInt();
                        break;
                    case 4:
                        weight = Float.intBitsToFloat(data.consumeInt());
                        break;
                    case 5:
                        weight = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        weight = data.consumeAsciiString(16).length();
                        break;
                    case 7: {
                        int denom = data.consumeInt(-2, 2);
                        weight = data.consumeInt() / (double) denom;
                        break;
                    }
                    default:
                        weight = data.consumeInt() == 0 ? 0.0 : 1.0 / data.consumeInt();
                        break;
                }
                fitter.addObservedPoint(weight, x, y);
            }
        }

        fitter.fit();

        if (data.consumeBoolean()) {
            fitter.fit();
        }
    }
}