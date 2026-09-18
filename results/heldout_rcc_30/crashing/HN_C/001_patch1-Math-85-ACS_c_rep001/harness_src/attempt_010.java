package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            int scenario = data.consumeInt(0, 7);

            org.apache.commons.math.analysis.UnivariateRealFunction function = null;
            double initial;
            double lowerBound;
            double upperBound;
            int maximumIterations;

            if (scenario == 0) {
                long initialBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long lowerBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                long upperBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                initial = Double.longBitsToDouble(initialBits);
                lowerBound = Double.longBitsToDouble(lowerBits);
                upperBound = Double.longBitsToDouble(upperBits);
                maximumIterations = data.consumeInt();
                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            if (scenario == 1) {
                double c = data.consumeInt(-1000, 1000) / 10.0;
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { c });
                double center = data.consumeInt(-1000, 1000) / 10.0;
                double span = data.consumeInt(0, 1000) / 10.0;
                lowerBound = center - span;
                upperBound = center + span + 1.0;
                initial = center;
                maximumIterations = data.consumeInt(-5, 20);
                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            if (scenario == 2) {
                double root = data.consumeInt(-1000, 1000) / 10.0;
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { -root, 1.0 });
                double left = Math.abs(data.consumeInt(-1000, 1000)) / 10.0 + 0.1;
                double right = Math.abs(data.consumeInt(-1000, 1000)) / 10.0 + 0.1;
                lowerBound = root - left;
                upperBound = root + right;
                if (data.consumeBoolean()) {
                    initial = root;
                } else {
                    initial = root + (data.consumeInt(-100, 100) / 100.0);
                    if (initial < lowerBound) {
                        initial = lowerBound;
                    }
                    if (initial > upperBound) {
                        initial = upperBound;
                    }
                }
                maximumIterations = data.consumeInt(1, 50);
                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            if (scenario == 3) {
                double shift = data.consumeInt(-1000, 1000) / 10.0;
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { -(shift * shift), 0.0, 1.0 });
                double extra = Math.abs(data.consumeInt(-1000, 1000)) / 10.0;
                lowerBound = -Math.abs(shift) - extra - 1.0;
                upperBound = Math.abs(shift) + extra + 1.0;
                initial = data.consumeBoolean() ? 0.0 : data.consumeInt(-100, 100) / 10.0;
                if (initial < lowerBound) {
                    initial = lowerBound;
                }
                if (initial > upperBound) {
                    initial = upperBound;
                }
                maximumIterations = data.consumeInt(1, 100);
                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            if (scenario == 4) {
                int degree = data.consumeInt(0, 8);
                double[] coefficients = new double[degree + 1];
                for (int i = 0; i < coefficients.length; i++) {
                    if (data.consumeBoolean()) {
                        coefficients[i] = data.consumeInt(-1000, 1000) / 10.0;
                    } else {
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        coefficients[i] = Double.longBitsToDouble(bits);
                    }
                }
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);

                if (data.consumeBoolean()) {
                    double a = data.consumeInt(-1000, 1000) / 10.0;
                    double b = data.consumeInt(-1000, 1000) / 10.0;
                    lowerBound = Math.min(a, b);
                    upperBound = Math.max(a, b);
                    if (lowerBound == upperBound) {
                        upperBound = lowerBound + 1.0;
                    }
                    initial = data.consumeInt(-1000, 1000) / 10.0;
                    maximumIterations = data.consumeInt(-10, 50);
                } else {
                    long initialBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    long lowerBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    long upperBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    initial = Double.longBitsToDouble(initialBits);
                    lowerBound = Double.longBitsToDouble(lowerBits);
                    upperBound = Double.longBitsToDouble(upperBits);
                    maximumIterations = data.consumeInt();
                }

                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            if (scenario == 5) {
                double root1 = data.consumeInt(-500, 500) / 10.0;
                double root2 = data.consumeInt(-500, 500) / 10.0;
                double c0 = root1 * root2;
                double c1 = -(root1 + root2);
                double c2 = 1.0;
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { c0, c1, c2 });

                double a = data.consumeInt(-1000, 1000) / 10.0;
                double b = data.consumeInt(-1000, 1000) / 10.0;
                lowerBound = Math.min(a, b);
                upperBound = Math.max(a, b);
                if (data.consumeBoolean()) {
                    initial = root1;
                } else if (data.consumeBoolean()) {
                    initial = root2;
                } else {
                    initial = data.consumeInt(-1000, 1000) / 10.0;
                }
                maximumIterations = data.consumeBoolean() ? data.consumeInt(1, 100) : data.consumeInt(-20, 20);
                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            if (scenario == 6) {
                int degree = data.consumeInt(1, 6);
                double[] coefficients = new double[degree + 1];
                for (int i = 0; i < coefficients.length; i++) {
                    coefficients[i] = data.consumeInt(-50, 50);
                }

                if (data.consumeBoolean()) {
                    coefficients[0] = 0.0;
                } else if (coefficients.length > 1 && data.consumeBoolean()) {
                    double chosenRoot = data.consumeInt(-100, 100) / 10.0;
                    coefficients[0] = -chosenRoot;
                    coefficients[1] = 1.0;
                    for (int i = 2; i < coefficients.length; i++) {
                        coefficients[i] = 0.0;
                    }
                }

                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);

                double center = data.consumeInt(-200, 200) / 10.0;
                double left = Math.abs(data.consumeInt(-200, 200)) / 10.0;
                double right = Math.abs(data.consumeInt(-200, 200)) / 10.0;
                lowerBound = center - left;
                upperBound = center + right;
                if (data.consumeBoolean()) {
                    initial = center;
                } else if (data.consumeBoolean()) {
                    initial = lowerBound;
                } else if (data.consumeBoolean()) {
                    initial = upperBound;
                } else {
                    initial = data.consumeInt(-500, 500) / 10.0;
                }
                maximumIterations = data.consumeBoolean() ? data.consumeInt(1, 30) : data.consumeInt();

                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
                return;
            }

            {
                String mode = data.consumeAsciiString(16);
                if ("sin".equals(mode)) {
                    function = new org.apache.commons.math.analysis.SinFunction();
                } else if ("exp".equals(mode)) {
                    function = new org.apache.commons.math.analysis.Expm1Function();
                } else {
                    function = new org.apache.commons.math.analysis.QuinticFunction();
                }

                if (data.consumeBoolean()) {
                    double center = data.consumeInt(-1000, 1000) / 10.0;
                    double width = Math.abs(data.consumeInt(-1000, 1000)) / 10.0;
                    lowerBound = center - width;
                    upperBound = center + width;
                    initial = data.consumeBoolean() ? center : data.consumeInt(-2000, 2000) / 10.0;
                } else {
                    long initialBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    long lowerBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    long upperBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    initial = Double.longBitsToDouble(initialBits);
                    lowerBound = Double.longBitsToDouble(lowerBits);
                    upperBound = Double.longBitsToDouble(upperBits);
                }
                maximumIterations = data.consumeBoolean() ? data.consumeInt(1, 100) : data.consumeInt();

                UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}