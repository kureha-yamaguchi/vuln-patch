package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function;

        int functionChoice = data.consumeInt(0, 4);
        if (functionChoice == 0) {
            function = null;
        } else {
            int degreePlusOne = data.consumeInt(1, 8);
            double[] coeffs = new double[degreePlusOne];
            for (int i = 0; i < degreePlusOne; i++) {
                int raw = data.consumeInt();
                int mode = data.consumeInt(0, 7);
                switch (mode) {
                    case 0:
                        coeffs[i] = raw;
                        break;
                    case 1:
                        coeffs[i] = raw / 1024.0d;
                        break;
                    case 2: {
                        int denom = data.consumeInt();
                        if (denom == Integer.MIN_VALUE) {
                            denom = Integer.MAX_VALUE;
                        }
                        coeffs[i] = raw / (double) (Math.abs(denom) + 1);
                        break;
                    }
                    case 3:
                        coeffs[i] = raw >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 4:
                        coeffs[i] = Double.NaN;
                        break;
                    case 5:
                        coeffs[i] = Math.nextAfter((double) raw,
                                data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                        break;
                    case 6:
                        coeffs[i] = raw == 0 ? -0.0d : 0.0d;
                        break;
                    default: {
                        long bits = (((long) raw) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                        coeffs[i] = Double.longBitsToDouble(bits);
                        break;
                    }
                }
            }
            function = new PolynomialFunction(coeffs);
        }

        double[] vals = new double[4];
        for (int i = 0; i < vals.length; i++) {
            int raw = data.consumeInt();
            int mode = data.consumeInt(0, 8);
            switch (mode) {
                case 0:
                    vals[i] = raw;
                    break;
                case 1:
                    vals[i] = raw / 4096.0d;
                    break;
                case 2: {
                    int denom = data.consumeInt();
                    if (denom == Integer.MIN_VALUE) {
                        denom = Integer.MAX_VALUE;
                    }
                    vals[i] = raw / (double) (Math.abs(denom) + 1);
                    break;
                }
                case 3:
                    vals[i] = raw >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    vals[i] = Double.NaN;
                    break;
                case 5:
                    vals[i] = Math.nextAfter((double) raw,
                            data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                case 6:
                    vals[i] = raw == 0 ? -0.0d : 0.0d;
                    break;
                case 7: {
                    long bits = (((long) raw) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                    vals[i] = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    vals[i] = data.consumeBoolean() ? Double.MIN_VALUE : Double.MAX_VALUE;
                    break;
            }
        }

        double initial;
        double lowerBound;
        double upperBound;

        if (data.consumeBoolean()) {
            initial = vals[0];
            lowerBound = vals[1];
            upperBound = vals[2];
        } else {
            lowerBound = Math.min(vals[0], vals[1]);
            upperBound = Math.max(vals[0], vals[1]);

            if (lowerBound == upperBound && !Double.isNaN(lowerBound)) {
                if (Double.isInfinite(lowerBound)) {
                    lowerBound = -Double.MAX_VALUE;
                    upperBound = Double.MAX_VALUE;
                } else {
                    upperBound = lowerBound + 1.0d;
                }
            }

            int initMode = data.consumeInt(0, 4);
            switch (initMode) {
                case 0:
                    initial = lowerBound;
                    break;
                case 1:
                    initial = upperBound;
                    break;
                case 2:
                    initial = (lowerBound + upperBound) / 2.0d;
                    break;
                case 3:
                    initial = vals[2];
                    break;
                default:
                    initial = vals[3];
                    break;
            }
        }

        int maximumIterations;
        switch (data.consumeInt(0, 4)) {
            case 0:
                maximumIterations = data.consumeInt();
                break;
            case 1:
                maximumIterations = data.consumeInt(-5, 5);
                break;
            case 2:
                maximumIterations = data.consumeInt(1, 100);
                break;
            case 3:
                maximumIterations = 1;
                break;
            default:
                maximumIterations = Integer.MAX_VALUE;
                break;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (ConvergenceException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}