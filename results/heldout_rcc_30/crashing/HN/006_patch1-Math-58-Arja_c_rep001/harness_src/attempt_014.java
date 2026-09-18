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
        runScenario(buildAnchorX(), ANCHOR_Y, true);

        int mode = data.consumeInt(0, 3);
        double[] y;
        double[] x;

        if (mode == 0) {
            y = mutateAnchorData(data);
            x = sequentialX(y.length, data.consumeInt(-20, 20), positiveStep(data));
        } else if (mode == 1) {
            y = buildPositiveData(data);
            x = sequentialX(y.length, data.consumeInt(-50, 50), positiveStep(data));
        } else if (mode == 2) {
            y = prefixOrSuffixAroundAnchor(data);
            x = sequentialX(y.length, data.consumeInt(-30, 30), positiveStep(data));
        } else {
            y = scaledAnchorWindow(data);
            x = sequentialX(y.length, data.consumeInt(-10, 10), positiveStep(data));
        }

        runScenario(x, y, true);
    }

    private static void runScenario(double[] x, double[] y, boolean validByConstruction) {
        if (x == null || y == null || x.length != y.length || y.length < 3) {
            return;
        }

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitterA.addObservedPoint(x[i], y[i]);
        }

        double[] fitNoArg;
        try {
            fitNoArg = fitterA.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
            return;
        }

        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitterB.addObservedPoint(x[i], y[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        final double[] fitWithGuess;
        try {
            fitWithGuess = fitterB.fit(guess);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        /* Contract/oracle:
           GaussianFitter.fit() is documented/implemented as the overload that computes an initial
           guess from the observations and then delegates to fit(double[] initialGuess). Therefore,
           for the same observations and the same guess produced by the real ParameterGuesser,
           fit() and fit(guess) must agree. A patch that merely suppresses the crash or changes
           the path without preserving the delegation semantics will violate this sibling-agreement. */
        if (!sameResult(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:sibling-fit] metamorphic violation: fit() must agree with fit(guess())"
                    + " xLen=" + x.length
                    + " yLen=" + y.length
                    + " lhs=" + render(fitNoArg)
                    + " rhs=" + render(fitWithGuess)
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String pn = p.getName();
            if ("org.apache.commons.math.exception".equals(pn)
                    || pn.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
    }

    private static double[] buildAnchorX() {
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        return x;
    }

    private static double positiveStep(FuzzedDataProvider data) {
        return 1.0 + data.consumeInt(0, 4);
    }

    private static double[] sequentialX(int len, int start, double step) {
        double[] x = new double[len];
        double v = start;
        for (int i = 0; i < len; i++) {
            x[i] = v;
            v += step;
        }
        return x;
    }

    private static double[] mutateAnchorData(FuzzedDataProvider data) {
        double scale = pow2(data.consumeInt(-3, 3));
        int len = ANCHOR_Y.length;
        double[] y = new double[len];
        for (int i = 0; i < len; i++) {
            double m = 1.0 + (data.consumeInt(-200, 200) / 1000.0);
            double v = ANCHOR_Y[i] * scale * m;
            y[i] = positive(v);
        }
        return y;
    }

    private static double[] prefixOrSuffixAroundAnchor(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 8);
        int suffix = data.consumeInt(0, 8);
        double[] y = new double[prefix + ANCHOR_Y.length + suffix];
        int idx = 0;
        for (int i = 0; i < prefix; i++) {
            y[idx++] = tinyPositive(data, i + 1);
        }
        double scale = pow2(data.consumeInt(-2, 2));
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            double tweak = 1.0 + (data.consumeInt(-100, 100) / 2000.0);
            y[idx++] = positive(ANCHOR_Y[i] * scale * tweak);
        }
        for (int i = 0; i < suffix; i++) {
            y[idx++] = tinyPositive(data, prefix + i + 1);
        }
        return y;
    }

    private static double[] scaledAnchorWindow(FuzzedDataProvider data) {
        int start = data.consumeInt(0, ANCHOR_Y.length - 3);
        int end = data.consumeInt(start + 2, ANCHOR_Y.length - 1);
        double[] y = new double[end - start + 1];
        double scale = pow2(data.consumeInt(-4, 4));
        for (int i = 0; i < y.length; i++) {
            double tweak = 1.0 + (data.consumeInt(-150, 150) / 1500.0);
            y[i] = positive(ANCHOR_Y[start + i] * scale * tweak);
        }
        return y;
    }

    private static double[] buildPositiveData(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] y = new double[len];

        int center = data.consumeInt(0, len - 1);
        double amplitude = 0.01 + (data.consumeInt(1, 1000) / 10.0);
        double sigma = 0.5 + (data.consumeInt(1, 200) / 20.0);
        double floor = 1e-18 * (1 + data.consumeInt(0, 100));

        for (int i = 0; i < len; i++) {
            double dx = i - center;
            double gaussian = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noiseFactor = 1.0 + (data.consumeInt(-200, 200) / 1000.0);
            y[i] = positive((gaussian + floor) * noiseFactor);
        }
        return y;
    }

    private static double tinyPositive(FuzzedDataProvider data, int salt) {
        int exp = data.consumeInt(6, 18);
        return (salt + 1) * Math.pow(10.0, -exp);
    }

    private static double positive(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1e-300;
        }
        if (v > 1_000_000.0) {
            return 1_000_000.0;
        }
        return v;
    }

    private static double pow2(int e) {
        return Math.pow(2.0, e);
    }

    private static boolean sameResult(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) || Double.isNaN(y)) {
                if (!(Double.isNaN(x) && Double.isNaN(y))) {
                    return false;
                }
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double tol = 1e-8 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > tol) {
                return false;
            }
        }
        return true;
    }

    private static String render(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}