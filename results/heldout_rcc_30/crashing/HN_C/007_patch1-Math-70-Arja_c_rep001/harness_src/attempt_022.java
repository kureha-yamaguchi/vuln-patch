package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffCount = data.consumeInt(1, 8);
        double[] coeffs = new double[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            switch (data.consumeInt(0, 11)) {
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
                    coeffs[i] = Double.NaN;
                    break;
                case 5:
                    coeffs[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    coeffs[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    coeffs[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    coeffs[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    coeffs[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    coeffs[i] = -Double.MIN_VALUE;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    coeffs[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        UnivariateRealFunction f = new PolynomialFunction(coeffs);

        double[] values = new double[3];
        for (int i = 0; i < values.length; i++) {
            switch (data.consumeInt(0, 13)) {
                case 0:
                    values[i] = 0.0d;
                    break;
                case 1:
                    values[i] = -0.0d;
                    break;
                case 2:
                    values[i] = 1.0d;
                    break;
                case 3:
                    values[i] = -1.0d;
                    break;
                case 4:
                    values[i] = Double.NaN;
                    break;
                case 5:
                    values[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    values[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    values[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    values[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    values[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    values[i] = -Double.MIN_VALUE;
                    break;
                case 11:
                    values[i] = (double) data.consumeInt();
                    break;
                case 12:
                    values[i] = data.consumeBoolean() ? Math.PI : -Math.PI;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    values[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double min = values[0];
        double max = values[1];
        double initial = values[2];

        if (data.consumeBoolean() && min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) / 2.0d;
                break;
            case 3:
                if (min <= max && !Double.isNaN(min) && !Double.isNaN(max)
                        && !Double.isInfinite(min) && !Double.isInfinite(max)) {
                    initial = min + (max - min) * 0.5d;
                }
                break;
            default:
                break;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}