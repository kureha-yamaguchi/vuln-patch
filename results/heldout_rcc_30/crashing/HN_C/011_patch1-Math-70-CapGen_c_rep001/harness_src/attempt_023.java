package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len1 = data.consumeInt(1, 8);
        double[] coeffs1 = new double[len1];
        for (int i = 0; i < len1; i++) {
            int mode = data.consumeInt(0, 4);
            if (mode == 0) {
                coeffs1[i] = 0.0;
            } else if (mode == 1) {
                coeffs1[i] = data.consumeInt(-16, 16);
            } else if (mode == 2) {
                coeffs1[i] = data.consumeInt() / (double) data.consumeInt(1, 1024);
            } else if (mode == 3) {
                coeffs1[i] = data.consumeByte();
            } else {
                coeffs1[i] = data.consumeBoolean() ? 1.0 : -1.0;
            }
        }

        int len2 = data.consumeInt(1, 8);
        double[] coeffs2 = new double[len2];
        for (int i = 0; i < len2; i++) {
            int mode = data.consumeInt(0, 4);
            if (mode == 0) {
                coeffs2[i] = 0.0;
            } else if (mode == 1) {
                coeffs2[i] = data.consumeInt(-16, 16);
            } else if (mode == 2) {
                coeffs2[i] = data.consumeInt() / (double) data.consumeInt(1, 1024);
            } else if (mode == 3) {
                coeffs2[i] = data.consumeByte();
            } else {
                coeffs2[i] = data.consumeBoolean() ? 1.0 : -1.0;
            }
        }

        if (data.consumeBoolean()) {
            coeffs1[0] = 0.0;
        }
        if (data.consumeBoolean()) {
            coeffs2[0] = 0.0;
        }
        if (data.consumeBoolean() && len1 > 1) {
            coeffs1[1] = data.consumeBoolean() ? 1.0 : -1.0;
        }
        if (data.consumeBoolean() && len2 > 1) {
            coeffs2[1] = data.consumeBoolean() ? 1.0 : -1.0;
        }

        UnivariateRealFunction f1 = new PolynomialFunction(coeffs1);
        UnivariateRealFunction f2 = new PolynomialFunction(coeffs2);

        double x1 = data.consumeInt();
        double x2 = data.consumeInt();
        double scale1 = data.consumeInt(1, 1024);
        double scale2 = data.consumeInt(1, 1024);

        double min = x1 / scale1;
        double max = x2 / scale2;

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        } else {
            double lo = Math.min(min, max);
            double hi = Math.max(min, max);
            min = lo;
            max = hi;
        }

        if (data.consumeBoolean()) {
            min = 0.0;
        }
        if (data.consumeBoolean()) {
            max = 0.0;
        }
        if (data.consumeBoolean()) {
            max = min;
        }

        double initial;
        switch (data.consumeInt(0, 5)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) / 2.0;
                break;
            case 3:
                initial = data.consumeInt() / (double) data.consumeInt(1, 1024);
                break;
            case 4:
                initial = min - data.consumeInt(0, 8);
                break;
            default:
                initial = max + data.consumeInt(0, 8);
                break;
        }

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver() : new BisectionSolver(f1);

        if (data.consumeBoolean()) {
            solver.setAbsoluteAccuracy(Math.abs(data.consumeInt()) / (double) data.consumeInt(1, 1024));
        }
        if (data.consumeBoolean()) {
            solver.setMaximalIterationCount(data.consumeInt(0, 1000));
        }

        try {
            solver.solve(f2, min, max, initial);
        } catch (Throwable t) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}