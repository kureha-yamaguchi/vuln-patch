package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            int mode = data.consumeInt(0, 5);

            org.apache.commons.math.analysis.UnivariateRealFunction function = null;
            double root = Double.longBitsToDouble(
                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));

            if (mode == 0) {
                function = null;
            } else if (mode == 1) {
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { -root, 1.0d });
            } else if (mode == 2) {
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { root * root, -2.0d * root, 1.0d });
            } else {
                int len;
                if (mode == 5 && data.consumeBoolean()) {
                    len = 0;
                } else {
                    len = data.consumeInt(1, 8);
                }

                double[] coefficients = new double[len];
                for (int i = 0; i < len; i++) {
                    int choice = data.consumeInt(0, 9);
                    if (choice == 0) {
                        coefficients[i] = 0.0d;
                    } else if (choice == 1) {
                        coefficients[i] = -0.0d;
                    } else if (choice == 2) {
                        coefficients[i] = 1.0d;
                    } else if (choice == 3) {
                        coefficients[i] = -1.0d;
                    } else if (choice == 4) {
                        coefficients[i] = Double.NaN;
                    } else if (choice == 5) {
                        coefficients[i] = Double.POSITIVE_INFINITY;
                    } else if (choice == 6) {
                        coefficients[i] = Double.NEGATIVE_INFINITY;
                    } else {
                        coefficients[i] = Double.longBitsToDouble(
                                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    }
                }
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);
            }

            double initial;
            double lowerBound;
            double upperBound;
            int maximumIterations;

            if (data.consumeBoolean()) {
                double center;
                if (mode == 1 && data.consumeBoolean()) {
                    center = root;
                } else {
                    center = (double) data.consumeInt(-1000, 1000);
                }

                double leftSpan = Math.abs((double) data.consumeInt(-1000, 1000));
                double rightSpan = Math.abs((double) data.consumeInt(-1000, 1000));

                lowerBound = center - leftSpan - 1.0d;
                upperBound = center + rightSpan + 1.0d;

                int initMode = data.consumeInt(0, 4);
                if (initMode == 0) {
                    initial = lowerBound;
                } else if (initMode == 1) {
                    initial = upperBound;
                } else if (initMode == 2) {
                    initial = center;
                } else if (initMode == 3) {
                    initial = lowerBound + (upperBound - lowerBound) / 2.0d;
                } else {
                    initial = mode == 1 ? root : center;
                }

                maximumIterations = data.consumeInt(1, 64);
            } else {
                lowerBound = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                upperBound = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                initial = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));

                if (data.consumeBoolean()) {
                    maximumIterations = data.consumeInt(-16, 16);
                } else {
                    maximumIterations = data.consumeInt();
                }
            }

            if (data.consumeBoolean()) {
                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
            } else {
                double[] result = UnivariateRealSolverUtils.bracket(
                        function, initial, lowerBound, upperBound, maximumIterations);

                if (result != null && result.length == 2 && function != null && data.consumeBoolean()) {
                    function.value(result[0]);
                    function.value(result[1]);
                }
            }
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}