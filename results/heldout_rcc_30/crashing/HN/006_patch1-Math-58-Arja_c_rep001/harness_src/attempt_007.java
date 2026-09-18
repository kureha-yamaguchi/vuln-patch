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
        runExploreIncreasingTail(data);
        runMetamorphicSiblingAgreement(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExploreIncreasingTail(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double v = (data.consumeInt(1, 1000)) / 1_000_000_000.0;
        for (int i = 0; i < len; i++) {
            int num = data.consumeInt(101, 300);
            double factor = num / 100.0;
            v *= factor;
            if (!Double.isFinite(v) || v > 1.0e6) {
                v = 1.0e6;
            }
            ys[i] = v;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runMetamorphicSiblingAgreement(FuzzedDataProvider data) {
        int len = data.consumeInt(5, 30);
        double norm = data.consumeInt(1, 1000) / 10.0;
        double mean = data.consumeInt(-500, 500) / 10.0;
        double sigma = data.consumeInt(1, 200) / 10.0;

        double[] xs = new double[len];
        double[] ys = new double[len];

        double start = mean - 2.0 * sigma;
        double step = (4.0 * sigma) / (len - 1);
        for (int i = 0; i < len; i++) {
            double x = start + i * step;
            double z = (x - mean) / sigma;
            double y = norm * Math.exp(-0.5 * z * z);
            xs[i] = x;
            ys[i] = y;
        }

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < len; i++) {
            fitterA.addObservedPoint(xs[i], ys[i]);
            fitterB.addObservedPoint(xs[i], ys[i]);
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = fitterA.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            rhs = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:sibling-agreement] metamorphic violation: fit() / fit(initialGuess) length mismatch lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            if (!Double.isFinite(lhs[i]) || !Double.isFinite(rhs[i])) {
                throw new RuntimeException("[oracle:sibling-agreement] metamorphic violation: fit() / fit(initialGuess) non-finite lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
            }
            double diff = Math.abs(lhs[i] - rhs[i]);
            double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(lhs[i]), Math.abs(rhs[i])));
            if (diff > tol) {
                throw new RuntimeException("[oracle:sibling-agreement] metamorphic violation: fit() / fit(initialGuess) inputLen=" + len + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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