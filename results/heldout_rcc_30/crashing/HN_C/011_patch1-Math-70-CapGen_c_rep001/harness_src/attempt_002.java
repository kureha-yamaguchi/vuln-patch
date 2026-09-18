package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffCount1 = data.consumeInt(1, 8);
        double[] coeffs1 = new double[coeffCount1];
        for (int i = 0; i < coeffCount1; i++) {
            coeffs1[i] = nextDouble(data);
        }

        int coeffCount2 = data.consumeInt(1, 8);
        double[] coeffs2 = new double[coeffCount2];
        for (int i = 0; i < coeffCount2; i++) {
            coeffs2[i] = nextDouble(data);
        }

        UnivariateRealFunction f1 = new PolynomialFunction(coeffs1);
        UnivariateRealFunction f2 = new PolynomialFunction(coeffs2);

        double min = nextDouble(data);
        double max = nextDouble(data);
        double initial = nextDouble(data);

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        BisectionSolver solver;
        UnivariateRealFunction argFunction;

        if (data.consumeBoolean()) {
            solver = new BisectionSolver();
            argFunction = data.consumeBoolean() ? f1 : f2;
        } else {
            solver = new BisectionSolver(data.consumeBoolean() ? f1 : f2);
            argFunction = data.consumeBoolean() ? f1 : f2;
        }

        try {
            solver.solve(argFunction, min, max, initial);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    private static double nextDouble(FuzzedDataProvider data) {
        int kind = data.consumeInt(0, 12);
        int a = data.consumeInt();
        int b = data.consumeInt();
        switch (kind) {
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
                return Double.MAX_VALUE;
            case 6:
                return -Double.MAX_VALUE;
            case 7:
                return Double.MIN_VALUE;
            case 8:
                return -Double.MIN_VALUE;
            case 9:
                return (double) a;
            case 10:
                return (double) a / (double) (b == 0 ? 1 : b);
            case 11:
                return Math.scalb((double) a, data.consumeInt(-1074, 1023));
            default:
                return Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}