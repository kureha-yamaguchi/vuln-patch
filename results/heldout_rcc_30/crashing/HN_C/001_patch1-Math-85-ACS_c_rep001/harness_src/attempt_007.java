package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final class SneakyThrow {
            @SuppressWarnings("unchecked")
            <T extends Throwable> void rethrow(Throwable t) throws T {
                throw (T) t;
            }
        }

        org.apache.commons.math.analysis.UnivariateRealFunction function;

        int functionChoice = data.consumeInt(0, 3);
        if (functionChoice == 0) {
            function = null;
        } else {
            int len = data.consumeInt(1, 8);
            double[] coeffs = new double[len];
            for (int i = 0; i < len; i++) {
                int kind = data.consumeInt(0, 12);
                switch (kind) {
                    case 0:
                        coeffs[i] = 0.0d;
                        break;
                    case 1:
                        coeffs[i] = -0.0d;
                        break;
                    case 2:
                        coeffs[i] = 1.0d;
                        break;
                    case 3:
                        coeffs[i] = -1.0d;
                        break;
                    case 4:
                        coeffs[i] = (double) data.consumeInt();
                        break;
                    case 5:
                        coeffs[i] = ((double) data.consumeInt()) / 1024.0d;
                        break;
                    case 6:
                        coeffs[i] = ((double) data.consumeByte());
                        break;
                    case 7:
                        coeffs[i] = Double.NaN;
                        break;
                    case 8:
                        coeffs[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        coeffs[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        coeffs[i] = Double.MAX_VALUE;
                        break;
                    case 11:
                        coeffs[i] = -Double.MAX_VALUE;
                        break;
                    default:
                        coeffs[i] = Double.MIN_VALUE;
                        break;
                }
            }

            if (functionChoice == 1) {
                coeffs[0] = 0.0d;
            } else if (functionChoice == 2 && coeffs.length >= 2) {
                coeffs[0] = -1.0d;
                coeffs[1] = 1.0d;
            } else if (functionChoice == 3 && coeffs.length >= 3) {
                coeffs[0] = 1.0d;
                coeffs[1] = 0.0d;
                coeffs[2] = -1.0d;
            }

            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
        }

        double[] pool = new double[3];
        for (int i = 0; i < pool.length; i++) {
            int kind = data.consumeInt(0, 15);
            switch (kind) {
                case 0:
                    pool[i] = 0.0d;
                    break;
                case 1:
                    pool[i] = -0.0d;
                    break;
                case 2:
                    pool[i] = 1.0d;
                    break;
                case 3:
                    pool[i] = -1.0d;
                    break;
                case 4:
                    pool[i] = 2.0d;
                    break;
                case 5:
                    pool[i] = -2.0d;
                    break;
                case 6:
                    pool[i] = (double) data.consumeInt(-10, 10);
                    break;
                case 7:
                    pool[i] = ((double) data.consumeInt()) / 1000.0d;
                    break;
                case 8:
                    pool[i] = (double) data.consumeInt();
                    break;
                case 9:
                    pool[i] = Double.NaN;
                    break;
                case 10:
                    pool[i] = Double.POSITIVE_INFINITY;
                    break;
                case 11:
                    pool[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 12:
                    pool[i] = Double.MAX_VALUE;
                    break;
                case 13:
                    pool[i] = -Double.MAX_VALUE;
                    break;
                case 14:
                    pool[i] = Double.MIN_VALUE;
                    break;
                default:
                    pool[i] = -Double.MIN_VALUE;
                    break;
            }
        }

        double lowerBound = pool[0];
        double initial = pool[1];
        double upperBound = pool[2];

        int ordering = data.consumeInt(0, 5);
        if (ordering == 0) {
            if (lowerBound > upperBound) {
                double tmp = lowerBound;
                lowerBound = upperBound;
                upperBound = tmp;
            }
            initial = data.consumeBoolean() ? lowerBound : upperBound;
        } else if (ordering == 1) {
            if (lowerBound > upperBound) {
                double tmp = lowerBound;
                lowerBound = upperBound;
                upperBound = tmp;
            }
            initial = lowerBound + (upperBound - lowerBound) / 2.0d;
        } else if (ordering == 2) {
            lowerBound = initial;
        } else if (ordering == 3) {
            upperBound = initial;
        } else if (ordering == 4) {
            lowerBound = upperBound;
        }

        int maximumIterations;
        switch (data.consumeInt(0, 6)) {
            case 0:
                maximumIterations = data.consumeInt(-5, 0);
                break;
            case 1:
                maximumIterations = 1;
                break;
            case 2:
                maximumIterations = 2;
                break;
            case 3:
                maximumIterations = data.consumeInt(1, 8);
                break;
            case 4:
                maximumIterations = data.consumeInt(1, 64);
                break;
            case 5:
                maximumIterations = Integer.MAX_VALUE;
                break;
            default:
                maximumIterations = data.consumeInt();
                break;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (Throwable t) {
            new SneakyThrow().rethrow(t);
        }
    }
}