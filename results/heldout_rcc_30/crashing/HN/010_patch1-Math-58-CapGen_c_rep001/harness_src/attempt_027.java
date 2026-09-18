package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
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
        exerciseDataset(buildXs(ANCHOR_DATA.length, 0.0, 1.0), ANCHOR_DATA);

        int n = data.consumeInt(3, 40);
        double startX = data.consumeInt(-100, 100);
        double step = data.consumeInt(1, 5);

        double[] gaussianTail = buildPositiveIncreasingGaussianTail(data, n, startX, step);
        exerciseDataset(buildXs(n, startX, step), gaussianTail);

        int m = data.consumeInt(3, ANCHOR_DATA.length);
        double[] scaledAnchor = buildScaledAnchorPrefix(data, m);
        double scaledStartX = data.consumeInt(-50, 50);
        double scaledStep = data.consumeInt(1, 4);
        exerciseDataset(buildXs(m, scaledStartX, scaledStep), scaledAnchor);
    }

    private static double[] buildXs(int n, double start, double step) {
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
        }
        return xs;
    }

    private static double[] buildPositiveIncreasingGaussianTail(FuzzedDataProvider data, int n, double startX, double step) {
        double[] ys = new double[n];
        double lastX = startX + (n - 1) * step;
        double center = lastX + data.consumeInt(1, 60);
        double sigma = data.consumeInt(1, 50);
        double ampPow = data.consumeInt(-20, 3);
        double amp = Math.pow(10.0, ampPow);
        double floorPow = data.consumeInt(-40, -15);
        double floor = Math.pow(10.0, floorPow);
        for (int i = 0; i < n; i++) {
            double x = startX + i * step;
            double d = (x - center) / sigma;
            double y = floor + amp * Math.exp(-0.5 * d * d);
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = floor;
            }
            ys[i] = y;
        }
        for (int i = 1; i < n; i++) {
            if (!(ys[i] > ys[i - 1])) {
                ys[i] = Math.nextUp(ys[i - 1]);
            }
        }
        return ys;
    }

    private static double[] buildScaledAnchorPrefix(FuzzedDataProvider data, int n) {
        double[] ys = new double[n];
        int scalePow = data.consumeInt(-5, 5);
        double scale = Math.pow(10.0, scalePow);
        double additivePow = data.consumeInt(-40, -20);
        double additive = Math.pow(10.0, additivePow);
        for (int i = 0; i < n; i++) {
            double y = ANCHOR_DATA[i] * scale + additive;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = additive;
            }
            ys[i] = y;
        }
        for (int i = 1; i < n; i++) {
            if (!(ys[i] > ys[i - 1])) {
                ys[i] = Math.nextUp(ys[i - 1]);
            }
        }
        return ys;
    }

    private static GaussianFitter newFitter(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
        return fitter;
    }

    private static void exerciseDataset(double[] xs, double[] ys) {
        GaussianFitter fitter = newFitter(xs, ys);
        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() computes "guess" from the current observations and delegates
         * to fit(double[] initialGuess) with that same guess. Therefore, for the same
         * observations, successful executions of both overloads must agree. A patch that
         * merely suppresses the throw or skips the real delegation path can violate this
         * observable overload-agreement relation even when no exception is raised.
         */
        GaussianFitter noArgFitter = newFitter(xs, ys);
        GaussianFitter explicitGuessFitter = newFitter(xs, ys);

        double[] lhs;
        try {
            lhs = noArgFitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(explicitGuessFitter.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = explicitGuessFitter.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:overload-agree] metamorphic violation: fit() != fit(guess) xs="
                    + arrayToString(xs) + " ys=" + arrayToString(ys)
                    + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static String arrayToString(double[] a) {
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
}