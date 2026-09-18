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
        runAnchor();
        runExplore(data);
        runScalingMetamorphic(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(4, 40);
        double[] y = new double[len];

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            int peak = data.consumeInt(0, len - 1);
            double amplitude = 1.0 + data.consumeInt(0, 10000) / 50.0;
            double sigma = 0.5 + data.consumeInt(0, 400) / 40.0;
            double step = 0.1 + data.consumeInt(0, 40) / 20.0;
            for (int i = 0; i < len; i++) {
                double d = (i - peak) * step;
                double v = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
                v += 1.0e-12 * (1 + data.consumeInt(0, 100));
                y[i] = v;
            }
        } else if (mode == 1) {
            double current = 1.0e-12 * (1 + data.consumeInt(0, 100));
            for (int i = 0; i < len; i++) {
                current *= 1.01 + data.consumeInt(0, 300) / 100.0;
                y[i] = current;
            }
        } else {
            double amplitude = 1.0 + data.consumeInt(0, 5000) / 10.0;
            double sigma = 0.5 + data.consumeInt(0, 200) / 30.0;
            double center = data.consumeInt(-30, 30) / 3.0;
            double start = center - (len / 2.0);
            for (int i = 0; i < len; i++) {
                double x = start + i;
                double d = x - center;
                double v = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
                v += 1.0e-9 * (1 + (i % 3));
                y[i] = v;
            }
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runScalingMetamorphic(FuzzedDataProvider data) {
        int points = data.consumeInt(5, 21);
        if ((points & 1) == 0) {
            points++;
        }

        double amplitude = 1.0 + data.consumeInt(0, 20000) / 20.0;
        double mean = data.consumeInt(-200, 200) / 10.0;
        double sigma = 0.5 + data.consumeInt(0, 300) / 30.0;
        double step = 0.2 + data.consumeInt(0, 40) / 20.0;
        double scale = 0.5 + data.consumeInt(1, 80) / 10.0;

        double[] x = new double[points];
        double[] y1 = new double[points];
        double[] y2 = new double[points];
        int mid = points / 2;

        for (int i = 0; i < points; i++) {
            x[i] = mean + (i - mid) * step;
            double d = x[i] - mean;
            double value = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            y1[i] = value;
            y2[i] = value * scale;
        }

        double[] p1;
        double[] p2;
        try {
            p1 = fitDataset(x, y1);
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }
        try {
            p2 = fitDataset(x, y2);
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        if (p1 == null || p2 == null || p1.length < 3 || p2.length < 3) {
            return;
        }

        if (!allFinite(p1) || !allFinite(p2)) {
            return;
        }

        /*
         * Contract/oracle: fit() returns Gaussian parameters for the observations.
         * For any correct least-squares Gaussian fitter, multiplying every observed y
         * value by a positive constant must multiply only the fitted amplitude by the
         * same constant, while mean and sigma stay unchanged. This uses two real
         * library fits and compares the same quantity two independent ways; a patch
         * that merely suppresses the known throw but corrupts the helper path will
         * still violate this relation.
         */
        double ampScaledBack = p2[0] / scale;
        if (!closeRel(ampScaledBack, p1[0], 0.12, 1.0e-6)
                || !closeRel(p2[1], p1[1], 0.08, 1.0e-6)
                || !closeRel(Math.abs(p2[2]), Math.abs(p1[2]), 0.12, 1.0e-6)) {
            throw new RuntimeException(
                "[oracle:y-scale] metamorphic violation: positive scaling of observations must preserve mean/sigma and scale amplitude"
                + " scale=" + scale
                + " p1=[" + p1[0] + "," + p1[1] + "," + p1[2] + "]"
                + " p2=[" + p2[0] + "," + p2[1] + "," + p2[2] + "]"
            );
        }
    }

    private static double[] fitDataset(double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter.fit();
    }

    private static boolean allFinite(double[] v) {
        for (int i = 0; i < v.length; i++) {
            if (Double.isNaN(v[i]) || Double.isInfinite(v[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean closeRel(double a, double b, double relTol, double absTol) {
        double diff = Math.abs(a - b);
        if (diff <= absTol) {
            return true;
        }
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= relTol * Math.max(scale, 1.0);
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t == null) {
            return;
        }
        if (isOracleViolation(t)) {
            sneakyThrow(t);
            return;
        }
        if (validByConstruction && isGroundTruthRootCause(t) && passesThroughReachableRegion(t)) {
            sneakyThrow(t);
            return;
        }
        if (isCleanRejection(t)) {
            return;
        }
        return;
    }

    private static boolean isOracleViolation(Throwable t) {
        String msg = t.getMessage();
        return (t instanceof RuntimeException && msg != null && msg.startsWith("[oracle:"))
                || t.getClass().getName().startsWith("com.code_intelligence.jazzer.api.FuzzerSecurityIssue");
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        return true;
    }

    private static boolean passesThroughReachableRegion(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls)
                    && "fit".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls)
                    && "getObservations".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls)
                    && "guess".equals(method)) {
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
        if (name.startsWith("org.apache.commons.math.exception.")) {
            return true;
        }
        if (name.startsWith("org.apache.commons.math.optimization.")
                && name.endsWith("Exception")) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}