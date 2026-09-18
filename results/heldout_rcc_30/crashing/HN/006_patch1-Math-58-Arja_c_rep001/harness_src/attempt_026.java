package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
        1.1143831578403364E-29,
        4.95281403484594E-28,
        1.1171347211930288E-26,
        1.7044813962636277E-25,
        1.9784716574832164E-24,
        1.8630236407866774E-23,
        1.4820532905097742E-22,
        1.0241963854632831E-21,
        6.275077366673128E-21,
        3.461808994532493E-20,
        1.7407124684715706E-19,
        8.056687953553974E-19,
        3.460193945992071E-18,
        1.3883326374011525E-17,
        5.233894983671116E-17,
        1.8630791465263745E-16,
        6.288759227922111E-16,
        2.0204433920597856E-15,
        6.198768938576155E-15,
        1.821419346860626E-14,
        5.139176445538471E-14,
        1.3956427429045787E-13,
        3.655705706448139E-13,
        9.253753324779779E-13,
        2.267636001476696E-12,
        5.3880460095836855E-12,
        1.2431632654852931E-11
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchorX = new double[ANCHOR_Y.length];
        for (int i = 0; i < anchorX.length; i++) {
            anchorX[i] = i;
        }
        exerciseScenario(anchorX, ANCHOR_Y, true);

        int start = data.consumeInt(0, ANCHOR_Y.length - 3);
        int end = data.consumeInt(start + 3, ANCHOR_Y.length);
        int prefix = data.consumeInt(0, 5);
        int suffix = data.consumeInt(0, 5);
        int total = prefix + (end - start) + suffix;

        double[] xs = new double[total];
        double[] ys = new double[total];

        int x0 = data.consumeInt(-100, 100);
        int step = data.consumeInt(1, 5);
        double yScale = pow10(data.consumeInt(-3, 3));

        int pos = 0;
        for (int i = 0; i < prefix; i++, pos++) {
            xs[pos] = x0 + pos * step;
            ys[pos] = strictlyPositive(ANCHOR_Y[start] / pow10(data.consumeInt(1, 6)) * yScale);
        }
        for (int i = start; i < end; i++, pos++) {
            xs[pos] = x0 + pos * step;
            ys[pos] = strictlyPositive(ANCHOR_Y[i] * yScale);
        }
        for (int i = 0; i < suffix; i++, pos++) {
            xs[pos] = x0 + pos * step;
            ys[pos] = strictlyPositive(ANCHOR_Y[end - 1] * pow10(data.consumeInt(0, 3)) * yScale);
        }

        exerciseScenario(xs, ys, true);
    }

    private static void exerciseScenario(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isGroundTruthFromFit(t)) {
                throw t;
            }
            return;
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter2.addObservedPoint(xs[i], ys[i]);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter2.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] viaExplicit;
        try {
            viaExplicit = fitter2.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/oracle: GaussianFitter.fit() is documented as the no-arg overload of
           fit(double[] initialGuess); the implementation computes a guess, then delegates.
           Therefore a correct implementation must agree with an explicit call using that
           same guessed parameter vector. A patch that only suppresses the throw or changes
           the call path to silently produce different results breaks this sibling-agreement. */
        assertArrayClose(xs, ys, viaNoArg, viaExplicit);
    }

    private static boolean isGroundTruthFromFit(Throwable t) {
        if (t == null) {
            return false;
        }
        String name = t.getClass().getName();
        if (!name.endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertArrayClose(double[] xs, double[] ys, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) length mismatch inputPoints="
                    + ys.length);
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) infinite mismatch index="
                            + i + " lhs=" + x + " rhs=" + y + " points=" + ys.length);
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > tol) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(initialGuess) index="
                        + i + " lhs=" + x + " rhs=" + y + " points=" + ys.length
                        + " firstX=" + xs[0] + " lastX=" + xs[xs.length - 1]
                        + " firstY=" + ys[0] + " lastY=" + ys[ys.length - 1]);
            }
        }
    }

    private static double pow10(int exp) {
        double v = 1.0;
        if (exp >= 0) {
            for (int i = 0; i < exp; i++) {
                v *= 10.0;
            }
        } else {
            for (int i = 0; i < -exp; i++) {
                v /= 10.0;
            }
        }
        return v;
    }

    private static double strictlyPositive(double v) {
        return v > 0.0 ? v : Double.MIN_VALUE;
    }
}