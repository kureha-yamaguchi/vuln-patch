package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
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
            return;
        }

        runOverloadAgreement(ANCHOR_DATA);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double amplitude = 0.1 + (data.consumeInt(0, 10000) / 100.0);
        double mean = data.consumeInt(0, len - 1) + (data.consumeInt(-100, 100) / 100.0);
        double sigma = 0.1 + (data.consumeInt(0, 2000) / 200.0);
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double noiseScale = data.consumeInt(0, 1000) / 1000000.0;
        boolean reverseX = data.consumeBoolean();

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = reverseX ? (len - 1 - i) : i;
            double dx = x - mean;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
            if (noiseScale > 0.0) {
                y += (data.consumeInt(0, 1000) / 1000.0) * noiseScale;
            }
            if (y < 0.0) {
                y = 0.0;
            }
            ys[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = reverseX ? (ys.length - 1 - i) : i;
            fitter.addObservedPoint(x, ys[i]);
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
            return;
        }

        runOverloadAgreement(ys);
    }

    private static void runOverloadAgreement(double[] ys) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            f1.addObservedPoint(i, ys[i]);
            f2.addObservedPoint(i, ys[i]);
        }

        double[] guess;
        double[] lhs;
        double[] rhs;

        try {
            WeightedObservedPoint[] observations = f1.getObservations();
            guess = (new GaussianFitter.ParameterGuesser(observations)).guess();
            lhs = f1.fit();
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /* GaussianFitter.fit() computes a ParameterGuesser guess from the current observations
           and delegates to the overload taking an initial guess. Therefore, when both succeed
           on the same observations, fit() and fit(guess) must produce the same parameter array. */
        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(guess) inputLen="
                    + ys.length + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
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
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-8 * scale) {
                return false;
            }
        }
        return true;
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}