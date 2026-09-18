package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    private static double consumeDoubleLike(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 11)) {
            case 0:
                return Double.NaN;
            case 1:
                return Double.POSITIVE_INFINITY;
            case 2:
                return Double.NEGATIVE_INFINITY;
            case 3:
                return 0.0d;
            case 4:
                return -0.0d;
            case 5:
                return 1.0d;
            case 6:
                return -1.0d;
            case 7:
                return Double.MIN_VALUE;
            case 8:
                return -Double.MIN_VALUE;
            case 9:
                return Double.MAX_VALUE;
            case 10:
                return -Double.MAX_VALUE;
            default:
                return Double.longBitsToDouble(
                    (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL)
                );
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len1 = data.consumeInt(1, 8);
        double[] coeffs1 = new double[len1];
        for (int i = 0; i < len1; i++) {
            coeffs1[i] = consumeDoubleLike(data);
        }

        int len2 = data.consumeInt(1, 8);
        double[] coeffs2 = new double[len2];
        for (int i = 0; i < len2; i++) {
            coeffs2[i] = consumeDoubleLike(data);
        }

        PolynomialFunction f1 = new PolynomialFunction(coeffs1);
        PolynomialFunction f2 = new PolynomialFunction(coeffs2);

        double min = consumeDoubleLike(data);
        double max = consumeDoubleLike(data);
        double initial = consumeDoubleLike(data);

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }
        if (data.consumeBoolean()) {
            initial = min;
        } else if (data.consumeBoolean()) {
            initial = max;
        } else if (data.consumeBoolean()) {
            initial = (min + max) / 2.0d;
        }

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver() : new BisectionSolver(f2);
        try {
            solver.solve(f1, min, max, initial);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}