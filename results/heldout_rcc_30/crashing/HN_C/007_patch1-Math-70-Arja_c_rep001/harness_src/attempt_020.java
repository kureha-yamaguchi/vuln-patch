package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            int degree = data.consumeInt(0, 7);
            double[] coefficients = new double[degree + 1];
            for (int i = 0; i < coefficients.length; i++) {
                long high = ((long) data.consumeInt()) << 32;
                long low = data.consumeInt() & 0xffffffffL;
                coefficients[i] = Double.longBitsToDouble(high | low);
            }

            PolynomialFunction f = new PolynomialFunction(coefficients);

            double min = nextDouble(data);
            double max = nextDouble(data);
            double initial = nextDouble(data);

            int shape = data.consumeInt(0, 8);
            switch (shape) {
                case 0:
                    min = 0.0;
                    max = 0.0;
                    break;
                case 1:
                    max = min;
                    break;
                case 2:
                    max = Math.nextUp(min);
                    break;
                case 3:
                    min = -1.0;
                    max = 1.0;
                    initial = 0.0;
                    break;
                case 4:
                    min = -Math.abs(min);
                    max = Math.abs(max);
                    break;
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
                default:
                    break;
            }

            if (data.consumeBoolean()) {
                double t = min;
                min = max;
                max = t;
            }

            BisectionSolver solver = new BisectionSolver();

            if (data.consumeBoolean()) {
                solver.setAbsoluteAccuracy(Math.abs(nextDouble(data)));
            }
            if (data.consumeBoolean()) {
                solver.setFunctionValueAccuracy(Math.abs(nextDouble(data)));
            }
            if (data.consumeBoolean()) {
                solver.setMaximalIterationCount(data.consumeInt(0, 10000));
            }

            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    private static double nextDouble(FuzzedDataProvider data) {
        long high = ((long) data.consumeInt()) << 32;
        long low = data.consumeInt() & 0xffffffffL;
        return Double.longBitsToDouble(high | low);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}