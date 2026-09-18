package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffLen = data.consumeInt(1, 8);
        double[] coefficients = new double[coeffLen];
        for (int i = 0; i < coeffLen; i++) {
            switch (data.consumeInt(0, 9)) {
                case 0:
                    coefficients[i] = 0.0d;
                    break;
                case 1:
                    coefficients[i] = -0.0d;
                    break;
                case 2:
                    coefficients[i] = Double.NaN;
                    break;
                case 3:
                    coefficients[i] = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    coefficients[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    coefficients[i] = Integer.MIN_VALUE;
                    break;
                case 6:
                    coefficients[i] = Integer.MAX_VALUE;
                    break;
                default:
                    coefficients[i] = (double) data.consumeInt();
                    break;
            }
        }

        UnivariateRealFunction f = new PolynomialFunction(coefficients);

        double min;
        switch (data.consumeInt(0, 7)) {
            case 0:
                min = 0.0d;
                break;
            case 1:
                min = -0.0d;
                break;
            case 2:
                min = Double.NaN;
                break;
            case 3:
                min = Double.POSITIVE_INFINITY;
                break;
            case 4:
                min = Double.NEGATIVE_INFINITY;
                break;
            case 5:
                min = Integer.MIN_VALUE;
                break;
            case 6:
                min = Integer.MAX_VALUE;
                break;
            default:
                min = (double) data.consumeInt();
                break;
        }

        double max;
        switch (data.consumeInt(0, 7)) {
            case 0:
                max = 0.0d;
                break;
            case 1:
                max = -0.0d;
                break;
            case 2:
                max = Double.NaN;
                break;
            case 3:
                max = Double.POSITIVE_INFINITY;
                break;
            case 4:
                max = Double.NEGATIVE_INFINITY;
                break;
            case 5:
                max = Integer.MIN_VALUE;
                break;
            case 6:
                max = Integer.MAX_VALUE;
                break;
            default:
                max = (double) data.consumeInt();
                break;
        }

        double initial;
        switch (data.consumeInt(0, 7)) {
            case 0:
                initial = 0.0d;
                break;
            case 1:
                initial = -0.0d;
                break;
            case 2:
                initial = Double.NaN;
                break;
            case 3:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 4:
                initial = Double.NEGATIVE_INFINITY;
                break;
            case 5:
                initial = Integer.MIN_VALUE;
                break;
            case 6:
                initial = Integer.MAX_VALUE;
                break;
            default:
                initial = (double) data.consumeInt();
                break;
        }

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver() : new BisectionSolver(f);
        try {
            solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}