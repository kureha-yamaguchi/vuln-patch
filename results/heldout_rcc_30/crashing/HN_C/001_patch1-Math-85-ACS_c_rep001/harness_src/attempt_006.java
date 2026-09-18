package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction function;

        int functionChoice = data.consumeInt(0, 2);
        if (functionChoice == 0) {
            function = null;
        } else if (functionChoice == 1) {
            function = new org.apache.commons.math.analysis.SinFunction();
        } else {
            int degree = data.consumeInt(0, 7);
            double[] coefficients = new double[degree + 1];
            for (int i = 0; i < coefficients.length; i++) {
                int mode = data.consumeInt(0, 7);
                if (mode == 0) {
                    coefficients[i] = 0.0d;
                } else if (mode == 1) {
                    coefficients[i] = 1.0d;
                } else if (mode == 2) {
                    coefficients[i] = -1.0d;
                } else if (mode == 3) {
                    coefficients[i] = data.consumeByte();
                } else if (mode == 4) {
                    coefficients[i] = data.consumeInt() / (double) data.consumeInt(1, 32);
                } else if (mode == 5) {
                    coefficients[i] = data.consumeInt(-1000, 1000);
                } else if (mode == 6) {
                    coefficients[i] = Double.NaN;
                } else {
                    coefficients[i] = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }
            }
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);
        }

        double v1;
        switch (data.consumeInt(0, 9)) {
            case 0:
                v1 = data.consumeInt();
                break;
            case 1:
                v1 = data.consumeInt() / (double) data.consumeInt(1, Integer.MAX_VALUE);
                break;
            case 2:
                v1 = 0.0d;
                break;
            case 3:
                v1 = -0.0d;
                break;
            case 4:
                v1 = Double.MIN_VALUE;
                break;
            case 5:
                v1 = Double.MAX_VALUE;
                break;
            case 6:
                v1 = Double.NaN;
                break;
            case 7:
                v1 = Double.POSITIVE_INFINITY;
                break;
            case 8:
                v1 = Double.NEGATIVE_INFINITY;
                break;
            default:
                v1 = data.consumeByte();
                break;
        }

        double v2;
        switch (data.consumeInt(0, 9)) {
            case 0:
                v2 = data.consumeInt();
                break;
            case 1:
                v2 = data.consumeInt() / (double) data.consumeInt(1, Integer.MAX_VALUE);
                break;
            case 2:
                v2 = 0.0d;
                break;
            case 3:
                v2 = -0.0d;
                break;
            case 4:
                v2 = Double.MIN_VALUE;
                break;
            case 5:
                v2 = Double.MAX_VALUE;
                break;
            case 6:
                v2 = Double.NaN;
                break;
            case 7:
                v2 = Double.POSITIVE_INFINITY;
                break;
            case 8:
                v2 = Double.NEGATIVE_INFINITY;
                break;
            default:
                v2 = data.consumeByte();
                break;
        }

        double v3;
        switch (data.consumeInt(0, 9)) {
            case 0:
                v3 = data.consumeInt();
                break;
            case 1:
                v3 = data.consumeInt() / (double) data.consumeInt(1, Integer.MAX_VALUE);
                break;
            case 2:
                v3 = 0.0d;
                break;
            case 3:
                v3 = -0.0d;
                break;
            case 4:
                v3 = Double.MIN_VALUE;
                break;
            case 5:
                v3 = Double.MAX_VALUE;
                break;
            case 6:
                v3 = Double.NaN;
                break;
            case 7:
                v3 = Double.POSITIVE_INFINITY;
                break;
            case 8:
                v3 = Double.NEGATIVE_INFINITY;
                break;
            default:
                v3 = data.consumeByte();
                break;
        }

        double lowerBound;
        double upperBound;
        double initial;

        int boundsMode = data.consumeInt(0, 6);
        if (boundsMode == 0) {
            lowerBound = v1;
            upperBound = v2;
            initial = v3;
        } else if (boundsMode == 1) {
            lowerBound = Math.min(v1, v2);
            upperBound = Math.max(v1, v2);
            initial = v3;
        } else if (boundsMode == 2) {
            lowerBound = Math.min(v1, v2);
            upperBound = Math.max(v1, v2);
            initial = lowerBound;
        } else if (boundsMode == 3) {
            lowerBound = Math.min(v1, v2);
            upperBound = Math.max(v1, v2);
            initial = upperBound;
        } else if (boundsMode == 4) {
            lowerBound = v1;
            upperBound = v1;
            initial = v3;
        } else if (boundsMode == 5) {
            lowerBound = Math.max(v1, v2);
            upperBound = Math.min(v1, v2);
            initial = v3;
        } else {
            lowerBound = Math.min(v1, v2);
            upperBound = Math.max(v1, v2);
            if (Double.isNaN(lowerBound) || Double.isNaN(upperBound)
                    || lowerBound == Double.NEGATIVE_INFINITY
                    || upperBound == Double.POSITIVE_INFINITY) {
                initial = v3;
            } else {
                double span = upperBound - lowerBound;
                if (Double.isNaN(span) || Double.isInfinite(span)) {
                    initial = v3;
                } else {
                    int frac = data.consumeInt(0, 100);
                    initial = lowerBound + (span * frac) / 100.0d;
                }
            }
        }

        int maximumIterations;
        int iterMode = data.consumeInt(0, 4);
        if (iterMode == 0) {
            maximumIterations = data.consumeInt();
        } else if (iterMode == 1) {
            maximumIterations = data.consumeInt(-3, 3);
        } else if (iterMode == 2) {
            maximumIterations = data.consumeInt(1, 5);
        } else if (iterMode == 3) {
            maximumIterations = data.consumeInt(1, 100);
        } else {
            maximumIterations = Integer.MAX_VALUE;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (org.apache.commons.math.ConvergenceException e) {
            throw new RuntimeException(e);
        } catch (org.apache.commons.math.FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}