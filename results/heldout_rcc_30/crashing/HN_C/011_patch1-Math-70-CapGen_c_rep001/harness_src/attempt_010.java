package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int degree = data.consumeInt(0, 8);
        double[] coefficients = new double[degree + 1];
        for (int i = 0; i < coefficients.length; i++) {
            switch (data.consumeInt(0, 9)) {
                case 0:
                    coefficients[i] = 0.0d;
                    break;
                case 1:
                    coefficients[i] = -0.0d;
                    break;
                case 2:
                    coefficients[i] = 1.0d;
                    break;
                case 3:
                    coefficients[i] = -1.0d;
                    break;
                case 4:
                    coefficients[i] = Integer.MAX_VALUE;
                    break;
                case 5:
                    coefficients[i] = Integer.MIN_VALUE;
                    break;
                case 6:
                    coefficients[i] = Double.NaN;
                    break;
                case 7:
                    coefficients[i] = Double.POSITIVE_INFINITY;
                    break;
                case 8:
                    coefficients[i] = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    coefficients[i] = (double) data.consumeInt();
                    break;
            }
        }

        UnivariateRealFunction f = new PolynomialFunction(coefficients);

        double min;
        switch (data.consumeInt(0, 9)) {
            case 0:
                min = 0.0d;
                break;
            case 1:
                min = -0.0d;
                break;
            case 2:
                min = 1.0d;
                break;
            case 3:
                min = -1.0d;
                break;
            case 4:
                min = Integer.MAX_VALUE;
                break;
            case 5:
                min = Integer.MIN_VALUE;
                break;
            case 6:
                min = Double.NaN;
                break;
            case 7:
                min = Double.POSITIVE_INFINITY;
                break;
            case 8:
                min = Double.NEGATIVE_INFINITY;
                break;
            default:
                min = (double) data.consumeInt();
                break;
        }

        double max;
        switch (data.consumeInt(0, 9)) {
            case 0:
                max = 0.0d;
                break;
            case 1:
                max = -0.0d;
                break;
            case 2:
                max = 1.0d;
                break;
            case 3:
                max = -1.0d;
                break;
            case 4:
                max = Integer.MAX_VALUE;
                break;
            case 5:
                max = Integer.MIN_VALUE;
                break;
            case 6:
                max = Double.NaN;
                break;
            case 7:
                max = Double.POSITIVE_INFINITY;
                break;
            case 8:
                max = Double.NEGATIVE_INFINITY;
                break;
            default:
                max = (double) data.consumeInt();
                break;
        }

        double initial;
        switch (data.consumeInt(0, 11)) {
            case 0:
                initial = 0.0d;
                break;
            case 1:
                initial = -0.0d;
                break;
            case 2:
                initial = 1.0d;
                break;
            case 3:
                initial = -1.0d;
                break;
            case 4:
                initial = min;
                break;
            case 5:
                initial = max;
                break;
            case 6:
                initial = (min + max) / 2.0d;
                break;
            case 7:
                initial = Double.NaN;
                break;
            case 8:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 9:
                initial = Double.NEGATIVE_INFINITY;
                break;
            case 10:
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
            sneakyThrow(e);
        } catch (FunctionEvaluationException e) {
            sneakyThrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}