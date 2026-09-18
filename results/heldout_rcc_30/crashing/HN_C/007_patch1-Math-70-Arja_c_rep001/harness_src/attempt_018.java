package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double root = ((double) data.consumeInt()) / 1024.0;

        double span = Math.abs((double) data.consumeInt()) / 1024.0;
        if (span == 0.0) {
            span = 1.0 / 1024.0;
        }

        double min = root - span;
        double max = root + span;
        if (min > max) {
            double t = min;
            min = max;
            max = t;
        }

        long initialBits = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
        double initialFromBits = Double.longBitsToDouble(initialBits);

        double initial;
        switch (data.consumeInt(0, 5)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = root;
                break;
            case 3:
                initial = root + span * 2.0;
                break;
            case 4:
                initial = root - span * 2.0;
                break;
            default:
                initial = initialFromBits;
                break;
        }

        org.apache.commons.math.analysis.UnivariateRealFunction targetFunction =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { -root, 1.0 });

        BisectionSolver solver = new BisectionSolver();

        try {
            if (data.consumeBoolean()) {
                double staleRoot = root + span + 1.0;
                org.apache.commons.math.analysis.UnivariateRealFunction staleFunction =
                        new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                                new double[] { -staleRoot, 1.0 });
                solver.solve(staleFunction, staleRoot - span, staleRoot + span);
            }

            solver.solve(targetFunction, min, max, initial);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}