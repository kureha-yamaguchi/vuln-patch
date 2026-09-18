package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int degreePlusOne = data.consumeInt(1, 8);
        double[] coeffs = new double[degreePlusOne];
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = consumeDoubleLike(data);
        }

        UnivariateRealFunction f = new PolynomialFunction(coeffs);

        double min = consumeDoubleLike(data);
        double max = consumeDoubleLike(data);
        double initial = consumeDoubleLike(data);

        BisectionSolver solver = new BisectionSolver();

        try {
            if (data.consumeBoolean()) {
                int seedLen = data.consumeInt(1, 4);
                double[] seedCoeffs = new double[seedLen];
                for (int i = 0; i < seedCoeffs.length; i++) {
                    seedCoeffs[i] = consumeDoubleLike(data);
                }
                UnivariateRealFunction seedFunction = new PolynomialFunction(seedCoeffs);

                double seedMin;
                double seedMax;
                if (data.consumeBoolean()) {
                    seedMin = -1.0d;
                    seedMax = 1.0d;
                } else {
                    seedMin = consumeDoubleLike(data);
                    seedMax = consumeDoubleLike(data);
                }

                solver.solve(seedFunction, seedMin, seedMax);
            }

            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            throwUnchecked(t);
        }
    }

    private static double consumeDoubleLike(FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 9);
        if (mode == 0) {
            return 0.0d;
        } else if (mode == 1) {
            return -0.0d;
        } else if (mode == 2) {
            return 1.0d;
        } else if (mode == 3) {
            return -1.0d;
        } else if (mode == 4) {
            return Double.NaN;
        } else if (mode == 5) {
            return Double.POSITIVE_INFINITY;
        } else if (mode == 6) {
            return Double.NEGATIVE_INFINITY;
        } else if (mode == 7) {
            return Double.MAX_VALUE;
        } else if (mode == 8) {
            return -Double.MAX_VALUE;
        }
        long bits = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
        return Double.longBitsToDouble(bits);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}