package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 3);
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int n = data.consumeInt(3, 40);
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;

        double center;
        if (mode == 0) {
            center = -1.0 - data.consumeInt(0, 200);
        } else if (mode == 1) {
            center = (n - 1) + 1.0 + data.consumeInt(0, 200);
        } else {
            center = data.consumeInt(-100, 100);
        }

        for (int i = 0; i < n; i++) {
            double x = i * step;
            double dx = x - center;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));

            int tweak = data.consumeInt(0, 20);
            if (tweak > 0) {
                y += tweak * 1e-12;
            }

            if (!(Double.isNaN(y) || Double.isInfinite(y)) && y > 0.0) {
                fitterA.addObservedPoint(x, y);
                fitterB.addObservedPoint(x, y);
            }
        }

        if (fitterA.getObservations().length < 3) {
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        boolean guessValid = guess != null
                && guess.length == 3
                && isFinite(guess[0])
                && isFinite(guess[1])
                && isFinite(guess[2])
                && guess[0] > 0.0
                && guess[2] > 0.0;
        if (!guessValid) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        if (!sameParams(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(initialGuess) "
                            + "when initialGuess is exactly GaussianFitter.ParameterGuesser(getObservations()).guess() "
                            + "inputPoints=" + fitterA.getObservations().length
                            + " lhs=" + format(viaNoArg)
                            + " rhs=" + format(viaGuess));
        }
    }

    private static void runAnchor() {
        final double[] data = {
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

        GaussianFitter fitterFit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < data.length; i++) {
            fitterFit.addObservedPoint(i, data[i]);
            fitterGuess.addObservedPoint(i, data[i]);
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterGuess.getObservations()).guess();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            fitterFit.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterGuess.fit(guess);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        double[] viaNoArg;
        try {
            GaussianFitter fitterAgain = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < data.length; i++) {
                fitterAgain.addObservedPoint(i, data[i]);
            }
            viaNoArg = fitterAgain.fit();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t, true)) {
                throw t;
            }
            return;
        }

        if (!sameParams(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(initialGuess) "
                            + "for the regression-test observations lhs=" + format(viaNoArg)
                            + " rhs=" + format(viaGuess));
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof IllegalArgumentException)) {
            return false;
        }
        if (!t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
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
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!isFinite(a[i]) || !isFinite(b[i])) {
                return false;
            }
            double diff = Math.abs(a[i] - b[i]);
            double scale = Math.max(1.0, Math.max(Math.abs(a[i]), Math.abs(b[i])));
            if (diff > 1.0e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}