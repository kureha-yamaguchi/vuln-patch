package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int degree = data.consumeInt(0, 8);
        double[] coefficients = new double[degree + 1];
        for (int i = 0; i < coefficients.length; i++) {
            long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double value = Double.longBitsToDouble(bits);
            if (data.consumeBoolean()) {
                value = data.consumeInt(-8, 8);
            }
            coefficients[i] = value;
        }

        if (data.consumeBoolean()) {
            int root = data.consumeInt(-8, 8);
            coefficients = new double[] { -root, 1.0 };
        }

        UnivariateRealFunction function = new PolynomialFunction(coefficients);

        long minBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long maxBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long initialBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        double min = Double.longBitsToDouble(minBits);
        double max = Double.longBitsToDouble(maxBits);
        double initial = Double.longBitsToDouble(initialBits);

        switch (data.consumeInt(0, 6)) {
            case 0:
                if (min > max) {
                    double t = min;
                    min = max;
                    max = t;
                }
                break;
            case 1:
                if (min > max) {
                    double t = min;
                    min = max;
                    max = t;
                }
                initial = min;
                break;
            case 2:
                if (min > max) {
                    double t = min;
                    min = max;
                    max = t;
                }
                initial = max;
                break;
            case 3:
                if (min > max) {
                    double t = min;
                    min = max;
                    max = t;
                }
                initial = min + (max - min) / 2.0;
                break;
            case 4:
                max = min;
                break;
            case 5:
                min = data.consumeInt(-16, 16);
                max = data.consumeInt(-16, 16);
                initial = data.consumeInt(-16, 16);
                break;
            default:
                break;
        }

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver() : new BisectionSolver(function);

        try {
            solver.solve(function, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}