package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function;
        int functionChoice = data.consumeInt(0, 4);
        double suggestedRoot = (double) data.consumeInt(-1000, 1000);

        switch (functionChoice) {
            case 0:
                function = null;
                break;
            case 1:
                function = new PolynomialFunction(new double[] { -suggestedRoot, 1.0d });
                break;
            case 2:
                function = new PolynomialFunction(new double[] { (double) data.consumeInt(-10, 10) });
                break;
            case 3:
                function = new PolynomialFunction(new double[] { -(suggestedRoot * suggestedRoot), 0.0d, 1.0d });
                break;
            default:
                int len = data.consumeInt(1, 6);
                double[] coeffs = new double[len];
                for (int i = 0; i < len; i++) {
                    int coeffMode = data.consumeInt(0, 4);
                    if (coeffMode == 0) {
                        coeffs[i] = (double) data.consumeInt(-1000, 1000);
                    } else if (coeffMode == 1) {
                        coeffs[i] = (double) data.consumeInt(-10, 10);
                    } else if (coeffMode == 2) {
                        coeffs[i] = 0.0d;
                    } else if (coeffMode == 3) {
                        coeffs[i] = (i == len - 1) ? 1.0d : -1.0d;
                    } else {
                        long bits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                        coeffs[i] = Double.longBitsToDouble(bits);
                    }
                }
                function = new PolynomialFunction(coeffs);
                break;
        }

        double lowerBound;
        double upperBound;
        double initial;
        int maximumIterations;

        int paramMode = data.consumeInt(0, 7);
        switch (paramMode) {
            case 0:
                lowerBound = suggestedRoot - 10.0d;
                upperBound = suggestedRoot + 10.0d;
                initial = suggestedRoot;
                maximumIterations = data.consumeInt(1, 20);
                break;
            case 1:
                lowerBound = suggestedRoot;
                upperBound = suggestedRoot;
                initial = suggestedRoot;
                maximumIterations = data.consumeInt(1, 20);
                break;
            case 2:
                lowerBound = suggestedRoot + 1.0d;
                upperBound = suggestedRoot - 1.0d;
                initial = suggestedRoot;
                maximumIterations = data.consumeInt(1, 20);
                break;
            case 3:
                lowerBound = suggestedRoot - 1.0d;
                upperBound = suggestedRoot + 1.0d;
                initial = suggestedRoot + 2.0d;
                maximumIterations = data.consumeInt(1, 20);
                break;
            case 4: {
                long lbBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                long ubBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                long initBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
                lowerBound = Double.longBitsToDouble(lbBits);
                upperBound = Double.longBitsToDouble(ubBits);
                initial = Double.longBitsToDouble(initBits);
                maximumIterations = data.consumeInt();
                break;
            }
            case 5:
                lowerBound = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : -0.0d;
                upperBound = data.consumeBoolean() ? Double.POSITIVE_INFINITY : 0.0d;
                initial = data.consumeBoolean() ? Double.NaN : suggestedRoot;
                maximumIterations = data.consumeBoolean() ? 0 : data.consumeInt(1, 5);
                break;
            case 6:
                lowerBound = -1.0d;
                upperBound = 1.0d;
                initial = 0.0d;
                maximumIterations = data.consumeInt(-5, 5);
                break;
            default:
                lowerBound = suggestedRoot - data.consumeInt(0, 3);
                upperBound = suggestedRoot + data.consumeInt(0, 3);
                initial = data.consumeBoolean() ? lowerBound : upperBound;
                maximumIterations = data.consumeInt(1, 50);
                break;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);

            if (function != null && data.consumeBoolean()) {
                double secondLower = data.consumeBoolean() ? lowerBound : (suggestedRoot - 2.0d);
                double secondUpper = data.consumeBoolean() ? upperBound : (suggestedRoot + 2.0d);
                double secondInitial = data.consumeBoolean() ? initial : suggestedRoot;
                int secondMax = data.consumeBoolean() ? maximumIterations : data.consumeInt(-2, 10);
                UnivariateRealSolverUtils.bracket(function, secondInitial, secondLower, secondUpper, secondMax);
            }
        } catch (ConvergenceException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}