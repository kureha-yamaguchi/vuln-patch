package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffCount = data.consumeInt(1, 8);
        double[] coefficients = new double[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            int raw = data.consumeInt();
            switch (Math.abs(raw % 12)) {
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
                    coefficients[i] = Double.NaN;
                    break;
                case 5:
                    coefficients[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    coefficients[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    coefficients[i] = Double.MAX_VALUE;
                    break;
                case 8:
                    coefficients[i] = -Double.MAX_VALUE;
                    break;
                case 9:
                    coefficients[i] = Double.MIN_VALUE;
                    break;
                case 10:
                    coefficients[i] = -Double.MIN_VALUE;
                    break;
                default:
                    coefficients[i] = raw / 1024.0d;
                    break;
            }
        }

        org.apache.commons.math.analysis.UnivariateRealFunction f =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);

        double min = fuzzDouble(data.consumeInt());
        double max = fuzzDouble(data.consumeInt());
        double initial = fuzzDouble(data.consumeInt());

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver() : new BisectionSolver(f);

        try {
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    private static double fuzzDouble(int raw) {
        switch (Math.abs(raw % 10)) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return 1.0d;
            case 3:
                return -1.0d;
            case 4:
                return Double.NaN;
            case 5:
                return Double.POSITIVE_INFINITY;
            case 6:
                return Double.NEGATIVE_INFINITY;
            case 7:
                return Double.MAX_VALUE;
            case 8:
                return -Double.MAX_VALUE;
            default:
                return raw / 256.0d;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}