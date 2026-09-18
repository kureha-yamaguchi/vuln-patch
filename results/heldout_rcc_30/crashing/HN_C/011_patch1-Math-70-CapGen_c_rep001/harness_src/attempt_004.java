package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction passedFunction;
        if (data.consumeBoolean()) {
            passedFunction = new SinFunction();
        } else {
            int degree = data.consumeInt(1, 8);
            double[] coefficients = new double[degree];
            for (int i = 0; i < degree; i++) {
                int kind = data.consumeInt(0, 6);
                if (kind == 0) {
                    coefficients[i] = 0.0d;
                } else if (kind == 1) {
                    coefficients[i] = -0.0d;
                } else if (kind == 2) {
                    coefficients[i] = (double) data.consumeInt();
                } else if (kind == 3) {
                    long bits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                    coefficients[i] = Double.longBitsToDouble(bits);
                } else if (kind == 4) {
                    coefficients[i] = Double.POSITIVE_INFINITY;
                } else if (kind == 5) {
                    coefficients[i] = Double.NEGATIVE_INFINITY;
                } else {
                    coefficients[i] = Double.NaN;
                }
            }
            passedFunction = new PolynomialFunction(coefficients);
        }

        UnivariateRealFunction storedFunction;
        if (data.consumeBoolean()) {
            storedFunction = new SinFunction();
        } else {
            int degree = data.consumeInt(1, 8);
            double[] coefficients = new double[degree];
            for (int i = 0; i < degree; i++) {
                int kind = data.consumeInt(0, 6);
                if (kind == 0) {
                    coefficients[i] = 0.0d;
                } else if (kind == 1) {
                    coefficients[i] = -0.0d;
                } else if (kind == 2) {
                    coefficients[i] = (double) data.consumeInt();
                } else if (kind == 3) {
                    long bits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                    coefficients[i] = Double.longBitsToDouble(bits);
                } else if (kind == 4) {
                    coefficients[i] = Double.POSITIVE_INFINITY;
                } else if (kind == 5) {
                    coefficients[i] = Double.NEGATIVE_INFINITY;
                } else {
                    coefficients[i] = Double.NaN;
                }
            }
            storedFunction = new PolynomialFunction(coefficients);
        }

        long minBits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
        long maxBits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
        long initialBits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);

        double min = Double.longBitsToDouble(minBits);
        double max = Double.longBitsToDouble(maxBits);
        double initial = Double.longBitsToDouble(initialBits);

        switch (data.consumeInt(0, 9)) {
            case 0:
                break;
            case 1:
                min = 0.0d;
                max = 0.0d;
                initial = 0.0d;
                break;
            case 2:
                min = -1.0d;
                max = 1.0d;
                initial = 0.0d;
                break;
            case 3:
                min = max;
                break;
            case 4: {
                double t = min;
                min = max;
                max = t;
                break;
            }
            case 5:
                min = Double.NEGATIVE_INFINITY;
                max = Double.POSITIVE_INFINITY;
                break;
            case 6:
                min = Double.NaN;
                break;
            case 7:
                max = Double.NaN;
                break;
            case 8:
                initial = min;
                break;
            case 9:
                initial = max;
                break;
            default:
                break;
        }

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver() : new BisectionSolver(storedFunction);
        try {
            solver.solve(passedFunction, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}