package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function;
        int functionChoice = data.consumeInt(0, 3);
        if (functionChoice == 0) {
            function = new SinFunction();
        } else {
            int degree = data.consumeInt(1, 8);
            double[] coefficients = new double[degree];
            boolean allZero = true;
            for (int i = 0; i < degree; i++) {
                int mode = data.consumeInt(0, 7);
                double value;
                switch (mode) {
                    case 0:
                        value = data.consumeInt();
                        break;
                    case 1:
                        value = data.consumeInt() / (double) data.consumeInt(1, 1024);
                        break;
                    case 2:
                        value = -data.consumeInt() / (double) data.consumeInt(1, 1024);
                        break;
                    case 3:
                        value = Double.NaN;
                        break;
                    case 4:
                        value = Double.POSITIVE_INFINITY;
                        break;
                    case 5:
                        value = Double.NEGATIVE_INFINITY;
                        break;
                    case 6:
                        value = Double.longBitsToDouble(
                                ((long) data.consumeInt() << 32) | (data.consumeInt() & 0xffffffffL));
                        break;
                    default:
                        value = data.consumeBoolean() ? 0.0d : -0.0d;
                        break;
                }
                coefficients[i] = value;
                if (value != 0.0d) {
                    allZero = false;
                }
            }
            if (allZero) {
                coefficients[degree - 1] = 1.0d;
            }
            function = new PolynomialFunction(coefficients);
        }

        double[] values = new double[3];
        for (int i = 0; i < values.length; i++) {
            int mode = data.consumeInt(0, 8);
            switch (mode) {
                case 0:
                    values[i] = data.consumeInt();
                    break;
                case 1:
                    values[i] = data.consumeInt() / (double) data.consumeInt(1, 4096);
                    break;
                case 2:
                    values[i] = -data.consumeInt() / (double) data.consumeInt(1, 4096);
                    break;
                case 3:
                    values[i] = Double.NaN;
                    break;
                case 4:
                    values[i] = Double.POSITIVE_INFINITY;
                    break;
                case 5:
                    values[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 6:
                    values[i] = Double.longBitsToDouble(
                            ((long) data.consumeInt() << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
                case 7:
                    values[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                    break;
                default:
                    values[i] = data.consumeBoolean() ? Math.PI : -Math.PI;
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
        if (data.consumeBoolean()) {
            initial = min;
        }
        if (data.consumeBoolean()) {
            initial = max;
        }
        if (data.consumeBoolean()) {
            initial = (min + max) / 2.0d;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(function, min, max, initial);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}