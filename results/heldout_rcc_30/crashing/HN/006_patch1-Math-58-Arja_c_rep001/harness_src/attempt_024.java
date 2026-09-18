package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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
        // ANCHOR: exact trigger from the regression test.
        exercise(buildAnchorX(), ANCHOR_Y, true);

        // EXPLORE: same root-cause property with real, valid-by-construction inputs:
        // positive gaussian-like observations, varied length/position/scale/surrounding content.
        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            exercise(buildTransformedAnchorX(data), buildTransformedAnchorY(data), true);
        } else if (mode == 1) {
            exercise(buildSyntheticGaussianX(data), buildSyntheticGaussianY(data), true);
        } else {
            double[][] mixed = buildMixedAnchorWithPadding(data);
            exercise(mixed[0], mixed[1], true);
        }
    }

    private static double[] buildAnchorX() {
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        return x;
    }

    private static double[] buildTransformedAnchorX(FuzzedDataProvider data) {
        double offset = data.consumeInt(-100, 100);
        double step = 1.0 + (data.consumeInt(0, 50) / 25.0);
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = offset + step * i;
        }
        return x;
    }

    private static double[] buildTransformedAnchorY(FuzzedDataProvider data) {
        double scale = Math.pow(10.0, data.consumeInt(-3, 3));
        double jitter = data.consumeInt(0, 20) / 1000.0;
        double[] y = new double[ANCHOR_Y.length];
        for (int i = 0; i < y.length; i++) {
            double factor = 1.0 + (((i & 1) == 0) ? jitter : -jitter);
            double v = ANCHOR_Y[i] * scale * factor;
            y[i] = (v > 0.0 && Double.isFinite(v)) ? v : Math.abs(ANCHOR_Y[i]) + 1e-300;
        }
        return y;
    }

    private static double[] buildSyntheticGaussianX(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double offset = data.consumeInt(-100, 100);
        double step = 1.0 + (data.consumeInt(0, 20) / 10.0);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = offset + i * step;
        }
        return x;
    }

    private static double[] buildSyntheticGaussianY(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double norm = 1e-6 * (1 + data.consumeInt(0, 1_000_000));
        double mean = data.consumeInt(0, n - 1);
        double sigma = 0.2 + (data.consumeInt(0, 200) / 20.0);
        double noise = data.consumeInt(0, 20) / 1000.0;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double z = (i - mean) / sigma;
            double base = norm * Math.exp(-0.5 * z * z);
            double wobble = 1.0 + (((i % 3) - 1) * noise);
            double v = base * wobble;
            y[i] = (v > 0.0 && Double.isFinite(v)) ? v : 1e-300;
        }
        return y;
    }

    private static double[][] buildMixedAnchorWithPadding(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 6);
        int suffix = data.consumeInt(0, 6);
        double offset = data.consumeInt(-50, 50);
        double step = 1.0 + (data.consumeInt(0, 20) / 10.0);
        double scale = Math.pow(10.0, data.consumeInt(-2, 2));
        int n = prefix + ANCHOR_Y.length + suffix;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = offset + i * step;
        }
        for (int i = 0; i < prefix; i++) {
            y[i] = tinyPositive(i + 1);
        }
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            y[prefix + i] = Math.max(ANCHOR_Y[i] * scale, tinyPositive(i + 10));
        }
        for (int i = 0; i < suffix; i++) {
            y[prefix + ANCHOR_Y.length + i] = tinyPositive(i + 100);
        }
        return new double[][] { x, y };
    }

    private static double tinyPositive(int n) {
        return 1e-300 * (1.0 + (n % 10));
    }

    private static void exercise(double[] x, double[] y, boolean validByConstruction) {
        if (x == null || y == null || x.length != y.length || x.length < 3) {
            return;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i]) || y[i] <= 0.0) {
                return;
            }
            fitter.addObservedPoint(x[i], y[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract/oracle: no-arg fit() computes a guess from the observations and should agree
        // with fit(double[] initialGuess) when given that same guess. A throw-deleting or
        // wrong-delegation patch can violate this observable equivalence.
        GaussianFitter lhsFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter rhsFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length; i++) {
            lhsFitter.addObservedPoint(x[i], y[i]);
            rhsFitter.addObservedPoint(x[i], y[i]);
        }

        try {
            double[] lhs = lhsFitter.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(rhsFitter.getObservations())).guess();
            double[] rhs = rhsFitter.fit(guess);
            assertArrayEquivalent(x, y, lhs, rhs);
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertArrayEquivalent(double[] x, double[] y, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) length mismatch inputLen=" + x.length);
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) || Double.isNaN(b)) {
                if (!(Double.isNaN(a) && Double.isNaN(b))) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) NaN mismatch index=" + i + " inputLen=" + x.length + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) infinity mismatch index=" + i + " inputLen=" + x.length + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            double tol = 1e-7 * (1.0 + Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(guess) differ index=" + i + " inputLen=" + x.length + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}