package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function;
        switch (data.consumeInt(0, 5)) {
            case 0:
                function = new PolynomialFunction(new double[] {0.0, 1.0});
                break;
            case 1:
                function = new PolynomialFunction(new double[] {-1.0, 0.0, 1.0});
                break;
            case 2:
                function = new PolynomialFunction(new double[] {0.0});
                break;
            case 3:
                function = new PolynomialFunction(new double[] {1.0});
                break;
            case 4:
                function = new PolynomialFunction(new double[] {-1.0});
                break;
            default:
                int degree = data.consumeInt(0, 8);
                double[] coefficients = new double[degree + 1];
                for (int i = 0; i < coefficients.length; i++) {
                    int mode = data.consumeInt(0, 6);
                    if (mode == 0) {
                        coefficients[i] = 0.0;
                    } else if (mode == 1) {
                        coefficients[i] = -0.0;
                    } else if (mode == 2) {
                        coefficients[i] = data.consumeInt(-16, 16);
                    } else if (mode == 3) {
                        coefficients[i] = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
                    } else if (mode == 4) {
                        coefficients[i] = data.consumeInt();
                    } else if (mode == 5) {
                        coefficients[i] = (double) data.consumeByte();
                    } else {
                        coefficients[i] = data.consumeBoolean() ? 1.0 : -1.0;
                    }
                }
                function = new PolynomialFunction(coefficients);
                break;
        }

        double lowerBound = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
        double upperBound = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
        double initial = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);

        if (data.consumeBoolean()) {
            if (lowerBound > upperBound) {
                double t = lowerBound;
                lowerBound = upperBound;
                upperBound = t;
            }
            if (lowerBound == upperBound) {
                if (data.consumeBoolean()) {
                    upperBound = lowerBound + 1.0;
                } else {
                    lowerBound = upperBound - 1.0;
                }
            }
            if (data.consumeBoolean()) {
                initial = lowerBound;
            } else if (data.consumeBoolean()) {
                initial = upperBound;
            } else {
                initial = lowerBound + (upperBound - lowerBound) / 2.0;
            }
        } else {
            int invalidMode = data.consumeInt(0, 3);
            if (invalidMode == 0) {
                if (lowerBound <= upperBound) {
                    lowerBound = upperBound;
                }
            } else if (invalidMode == 1) {
                initial = lowerBound - 1.0 - Math.abs(data.consumeInt(-10, 10));
            } else if (invalidMode == 2) {
                initial = upperBound + 1.0 + Math.abs(data.consumeInt(-10, 10));
            }
        }

        int maximumIterations;
        if (data.consumeBoolean()) {
            maximumIterations = data.consumeInt(-5, 20);
        } else {
            maximumIterations = data.consumeInt(-100, 100);
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (Throwable t) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}