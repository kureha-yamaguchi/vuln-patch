package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Helper {
            double nextDouble() {
                switch (data.consumeInt(0, 9)) {
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
                        return Integer.MIN_VALUE;
                    case 8:
                        return Integer.MAX_VALUE;
                    default:
                        long hi = ((long) data.consumeInt()) << 32;
                        long lo = data.consumeInt() & 0xffffffffL;
                        return Double.longBitsToDouble(hi | lo);
                }
            }

            UnivariateRealFunction nextFunction() {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        return new PolynomialFunction(new double[] {0.0d, 1.0d});
                    case 1:
                        return new PolynomialFunction(new double[] {0.0d, -1.0d});
                    case 2:
                        return new PolynomialFunction(new double[] {-1.0d, 0.0d, 1.0d});
                    case 3:
                        return new PolynomialFunction(new double[] {1.0d});
                    case 4:
                        return new PolynomialFunction(new double[] {0.0d, 0.0d, 0.0d, 1.0d});
                    default:
                        int len = data.consumeInt(1, 6);
                        double[] coeffs = new double[len];
                        boolean allZero = true;
                        for (int i = 0; i < len; i++) {
                            coeffs[i] = nextDouble();
                            if (coeffs[i] != 0.0d) {
                                allZero = false;
                            }
                        }
                        if (allZero) {
                            coeffs[len - 1] = 1.0d;
                        }
                        return new PolynomialFunction(coeffs);
                }
            }

            @SuppressWarnings("unchecked")
            <T extends Throwable> void sneakyThrow(Throwable t) throws T {
                throw (T) t;
            }
        }

        Helper h = new Helper();

        UnivariateRealFunction callFunction = h.nextFunction();
        UnivariateRealFunction storedFunction = h.nextFunction();

        double min;
        double max;
        switch (data.consumeInt(0, 4)) {
            case 0:
                min = -1.0d;
                max = 1.0d;
                break;
            case 1:
                min = 0.0d;
                max = 0.0d;
                break;
            case 2:
                min = 1.0d;
                max = -1.0d;
                break;
            case 3:
                min = h.nextDouble();
                max = h.nextDouble();
                break;
            default:
                double center = h.nextDouble();
                double span = h.nextDouble();
                min = center - span;
                max = center + span;
                break;
        }

        double initial = h.nextDouble();

        BisectionSolver solver = data.consumeBoolean()
                ? new BisectionSolver()
                : new BisectionSolver(storedFunction);

        try {
            solver.solve(callFunction, min, max, initial);
        } catch (Throwable t) {
            h.sneakyThrow(t);
        }
    }
}