package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffLen = data.consumeInt(1, 8);
        double[] coefficients = new double[coeffLen];
        for (int i = 0; i < coeffLen; i++) {
            long bits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
            coefficients[i] = Double.longBitsToDouble(bits);
        }

        org.apache.commons.math.analysis.UnivariateRealFunction f =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coefficients);

        long minBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
        long maxBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
        long initialBits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);

        double min = Double.longBitsToDouble(minBits);
        double max = Double.longBitsToDouble(maxBits);
        double initial = Double.longBitsToDouble(initialBits);

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        int initialMode = data.consumeInt(0, 4);
        if (initialMode == 0) {
            initial = min;
        } else if (initialMode == 1) {
            initial = max;
        } else if (initialMode == 2) {
            initial = (min + max) / 2.0;
        } else if (initialMode == 3) {
            initial = 0.0;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (org.apache.commons.math.MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (org.apache.commons.math.FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}