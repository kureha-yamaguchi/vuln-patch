package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            org.apache.commons.math.analysis.UnivariateRealFunction function;

            int functionChoice = data.consumeInt(0, 2);
            if (functionChoice == 0) {
                function = new org.apache.commons.math.analysis.SinFunction();
            } else if (functionChoice == 1) {
                function = new org.apache.commons.math.analysis.QuinticFunction();
            } else {
                int degree = data.consumeInt(0, 8);
                double[] coefficients = new double[degree + 1];
                for (int i = 0; i < coefficients.length; i++) {
                    int numerator = data.consumeInt(-1000, 1000);
                    int denominator = data.consumeInt(1, 1000);
                    double value = numerator / (double) denominator;
                    if (data.consumeBoolean()) {
                        value += data.consumeByte() / 16.0;
                    }
                    coefficients[i] = value;
                }
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);
            }

            double lowerBound;
            double initial;
            double upperBound;

            if (data.consumeBoolean()) {
                int center = data.consumeInt(-1000, 1000);
                int leftWidth = data.consumeInt(0, 1000);
                int rightWidth = data.consumeInt(0, 1000);

                lowerBound = center - leftWidth;
                upperBound = center + rightWidth;

                if (data.consumeBoolean()) {
                    double tmp = lowerBound;
                    lowerBound = upperBound;
                    upperBound = tmp;
                }

                if (data.consumeBoolean()) {
                    initial = lowerBound;
                } else if (data.consumeBoolean()) {
                    initial = upperBound;
                } else {
                    double lo = Math.min(lowerBound, upperBound);
                    double hi = Math.max(lowerBound, upperBound);
                    int step = data.consumeInt(0, 1000);
                    initial = lo + (hi - lo) * (step / 1000.0);
                    if (data.consumeBoolean()) {
                        initial += data.consumeByte() / 32.0;
                    }
                }
            } else {
                long lowerBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long initialBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long upperBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

                lowerBound = Double.longBitsToDouble(lowerBits);
                initial = Double.longBitsToDouble(initialBits);
                upperBound = Double.longBitsToDouble(upperBits);
            }

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    lowerBound = Double.NEGATIVE_INFINITY;
                }
                if (data.consumeBoolean()) {
                    upperBound = Double.POSITIVE_INFINITY;
                }
                if (data.consumeBoolean()) {
                    initial = Double.NaN;
                }
            }

            int maximumIterations;
            if (data.consumeBoolean()) {
                maximumIterations = data.consumeInt(-100, 100);
            } else {
                maximumIterations = data.consumeInt(1, 10000);
            }

            double[] bracket = UnivariateRealSolverUtils.bracket(
                    function, initial, lowerBound, upperBound, maximumIterations);

            if (data.consumeBoolean()) {
                double secondInitial;
                if (data.consumeBoolean()) {
                    secondInitial = bracket[0];
                } else if (data.consumeBoolean()) {
                    secondInitial = bracket[1];
                } else {
                    secondInitial = (bracket[0] + bracket[1]) / 2.0;
                }

                int secondIterations = data.consumeBoolean() ? maximumIterations : data.consumeInt(1, 10000);
                UnivariateRealSolverUtils.bracket(function, secondInitial, bracket[0], bracket[1], secondIterations);
            }
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}