package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 5);

        double rootBits = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double rootFinite = data.consumeInt(-1024, 1024) / 8.0;
        double root = (mode == 0) ? rootBits : rootFinite;

        double slope = data.consumeInt(-32, 32);
        if (slope == 0.0) {
            slope = data.consumeBoolean() ? 1.0 : -1.0;
        }

        double width = Math.abs(data.consumeInt(-256, 256)) + 1.0;

        UnivariateRealFunction f;
        if (data.consumeBoolean()) {
            f = new PolynomialFunction(new double[] { -slope * rootFinite, slope });
        } else {
            int degree = data.consumeInt(1, 6);
            double[] coeffs = new double[degree];
            for (int i = 0; i < degree; i++) {
                int coeffMode = data.consumeInt(0, 4);
                if (coeffMode == 0) {
                    coeffs[i] = data.consumeInt(-1000, 1000);
                } else if (coeffMode == 1) {
                    coeffs[i] = data.consumeInt(-1000, 1000) / 16.0;
                } else if (coeffMode == 2) {
                    coeffs[i] = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                } else if (coeffMode == 3) {
                    coeffs[i] = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                } else {
                    coeffs[i] = data.consumeBoolean() ? 0.0 : -0.0;
                }
            }
            f = new PolynomialFunction(coeffs);
        }

        double min;
        double max;
        if (data.consumeBoolean()) {
            min = rootFinite - width;
            max = rootFinite + width;
        } else {
            int endpointMode = data.consumeInt(0, 4);
            if (endpointMode == 0) {
                min = data.consumeInt(-4096, 4096);
                max = data.consumeInt(-4096, 4096);
            } else if (endpointMode == 1) {
                min = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                max = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            } else if (endpointMode == 2) {
                min = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
                max = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            } else if (endpointMode == 3) {
                min = data.consumeBoolean() ? -0.0 : 0.0;
                max = data.consumeBoolean() ? -0.0 : 0.0;
            } else {
                min = Double.NaN;
                max = Double.NaN;
            }
        }

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        double initialBits = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        double initial;
        int initialMode = data.consumeInt(0, 4);
        if (initialMode == 0) {
            initial = root;
        } else if (initialMode == 1) {
            initial = (min + max) / 2.0;
        } else if (initialMode == 2) {
            initial = data.consumeInt(-4096, 4096) / 4.0;
        } else if (initialMode == 3) {
            initial = initialBits;
        } else {
            initial = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }

        UnivariateRealFunction g;
        if (data.consumeBoolean()) {
            double constant = Math.abs(data.consumeInt(-1024, 1024)) + 1.0;
            g = new PolynomialFunction(new double[] { constant });
        } else {
            double otherRoot = rootFinite + Math.abs(data.consumeInt(1, 128)) + 1.0;
            double otherSlope = data.consumeInt(-32, 32);
            if (otherSlope == 0.0) {
                otherSlope = 1.0;
            }
            g = new PolynomialFunction(new double[] { -otherSlope * otherRoot, otherSlope });
        }

        BisectionSolver solver;
        int solverMode = data.consumeInt(0, 2);
        if (solverMode == 0) {
            solver = new BisectionSolver();
        } else if (solverMode == 1) {
            solver = new BisectionSolver(f);
        } else {
            solver = new BisectionSolver(g);
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}