package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = {
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
        runAnchor();

        double shift = data.consumeInt(-50, 50);
        if (shift == 0.0) {
            shift = 7.0;
        }
        checkGuesserTranslation(ANCHOR, shift);

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            double[] synthetic = buildSyntheticGaussian(data);
            if (synthetic != null) {
                runValidSyntheticFit(synthetic, data.consumeInt(-20, 20));
                checkGuesserTranslation(synthetic, data.consumeInt(-20, 20) + 0.5);
            }
        } else if (mode == 1) {
            double[] sparse = buildPositiveSeries(data);
            if (sparse != null) {
                runSeriesFit(sparse, data.consumeInt(-10, 10));
                checkGuesserTranslation(sparse, data.consumeInt(-10, 10) + 1.0);
            }
        } else {
            double[] nearBoundary = buildNearBoundarySeries(data);
            if (nearBoundary != null) {
                runSeriesFit(nearBoundary, data.consumeInt(-5, 5));
                checkGuesserTranslation(nearBoundary, data.consumeInt(-5, 5) - 0.5);
            }
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runValidSyntheticFit(double[] y, int xOffset) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i + xOffset, y[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runSeriesFit(double[] y, int xOffset) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i + xOffset, y[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkGuesserTranslation(double[] y, double shift) {
        if (y == null || y.length < 3) {
            return;
        }

        WeightedObservedPoint[] base = new WeightedObservedPoint[y.length];
        WeightedObservedPoint[] moved = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            base[i] = new WeightedObservedPoint(1.0, i, y[i]);
            moved[i] = new WeightedObservedPoint(1.0, i + shift, y[i]);
        }

        final double[] g1;
        final double[] g2;
        try {
            g1 = new GaussianFitter.ParameterGuesser(base).guess();
            g2 = new GaussianFitter.ParameterGuesser(moved).guess();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (g1 == null || g2 == null || g1.length < 3 || g2.length < 3) {
            return;
        }

        if (!isFinite(g1[0]) || !isFinite(g1[1]) || !isFinite(g1[2]) ||
            !isFinite(g2[0]) || !isFinite(g2[1]) || !isFinite(g2[2])) {
            return;
        }

        /*
         * Contract/invariant used for this oracle:
         * ParameterGuesser operates on observation coordinates; translating every x by a constant
         * leaves the sampled shape unchanged, so the guessed norm and sigma must stay the same
         * while the guessed mean shifts by that same constant. This is a pure coordinate change,
         * not a data change. If a patch merely hides the fit-time exception but leaves the
         * upstream parameter estimation inconsistent around the sigma boundary, this relation
         * still exposes it.
         */
        double ampTol = relativeTol(g1[0], g2[0], 1e-9, 1e-6);
        double sigmaTol = relativeTol(g1[2], g2[2], 1e-9, 1e-6);
        double meanTol = Math.max(1e-9, 1e-6 * Math.max(1.0, Math.abs(shift)));

        if (Math.abs(g1[0] - g2[0]) > ampTol ||
            Math.abs(g1[2] - g2[2]) > sigmaTol ||
            Math.abs((g2[1] - g1[1]) - shift) > meanTol) {
            throw new RuntimeException(
                "[oracle:guess-translate] metamorphic violation: translated observations must preserve guessed amplitude/sigma and shift guessed mean " +
                "shift=" + shift +
                " g1=[" + g1[0] + "," + g1[1] + "," + g1[2] + "]" +
                " g2=[" + g2[0] + "," + g2[1] + "," + g2[2] + "]"
            );
        }
    }

    private static double[] buildSyntheticGaussian(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 24);
        int meanIndex = data.consumeInt(1, n - 2);
        double amplitude = 1.0 + data.consumeInt(0, 500);
        double sigma = 0.25 + (data.consumeInt(0, 800) / 200.0);
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double dx = i - meanIndex;
            double base = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noise = data.consumeBoolean() ? 0.0 : base * 1e-12;
            y[i] = Math.max(base + noise, Double.MIN_VALUE);
        }
        return y;
    }

    private static double[] buildPositiveSeries(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 20);
        double[] y = new double[n];
        boolean anyPositive = false;
        for (int i = 0; i < n; i++) {
            int raw = data.consumeInt(0, 1000);
            y[i] = raw == 0 ? Double.MIN_VALUE : raw / 1000.0;
            anyPositive |= y[i] > 0.0;
        }
        return anyPositive ? y : null;
    }

    private static double[] buildNearBoundarySeries(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 18);
        int peak = data.consumeInt(0, n - 1);
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            int dist = Math.abs(i - peak);
            if (dist == 0) {
                y[i] = 1.0 + data.consumeInt(0, 100);
            } else if (dist == 1) {
                y[i] = (1.0 + data.consumeInt(0, 10)) * 1e-9;
            } else {
                y[i] = Double.MIN_VALUE;
            }
        }
        return y;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method)) ||
                ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method)) ||
                ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static double relativeTol(double a, double b, double floor, double rel) {
        return Math.max(floor, rel * Math.max(Math.abs(a), Math.abs(b)));
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