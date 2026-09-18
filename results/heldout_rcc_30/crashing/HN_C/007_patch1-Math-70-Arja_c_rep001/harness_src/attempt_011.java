package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len = data.consumeInt(0, 8);
        double[] coeffs = new double[len == 0 ? 1 : len];
        for (int i = 0; i < coeffs.length; i++) {
            long bits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
            coeffs[i] = Double.longBitsToDouble(bits);
        }

        org.apache.commons.math.analysis.UnivariateRealFunction f;
        int kind = data.consumeInt(0, 4);
        if (kind == 0) {
            f = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] {0.0d, 1.0d});
        } else if (kind == 1) {
            f = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] {0.0d});
        } else if (kind == 2) {
            f = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] {-1.0d, 0.0d, 1.0d});
        } else if (kind == 3) {
            f = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] {1.0d, -1.0d});
        } else {
            f = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
        }

        long minBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
        long maxBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
        long initialBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);

        double min = Double.longBitsToDouble(minBits);
        double max = Double.longBitsToDouble(maxBits);
        double initial = Double.longBitsToDouble(initialBits);

        int shape = data.consumeInt(0, 7);
        if (shape == 0) {
            min = -1.0d;
            max = 1.0d;
            initial = 0.0d;
        } else if (shape == 1) {
            min = 0.0d;
            max = 0.0d;
            initial = 0.0d;
        } else if (shape == 2) {
            min = max;
            initial = min;
        } else if (shape == 3) {
            double t = min;
            min = max;
            max = t;
        } else if (shape == 4) {
            initial = min;
        } else if (shape == 5) {
            initial = max;
        } else if (shape == 6) {
            min = -0.0d;
            max = 0.0d;
            initial = Double.NaN;
        }

        BisectionSolver solver;
        if (data.consumeBoolean()) {
            solver = new BisectionSolver();
        } else {
            solver = new BisectionSolver(f);
        }

        try {
            if (data.consumeBoolean()) {
                solver.solve(f, min, max);
            }
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