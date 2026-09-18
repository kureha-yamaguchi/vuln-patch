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

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int len = data.consumeInt(3, ANCHOR_DATA.length - start);

        int xShift = data.consumeInt(-1000, 1000);
        int xStep = data.consumeInt(1, 10);

        int scalePow = data.consumeInt(-6, 6);
        double yScale = pow10(scalePow);

        int baselinePow = data.consumeInt(-18, -6);
        double baseline = pow10(baselinePow);

        boolean addJitter = data.consumeBoolean();
        int jitterDiv = data.consumeInt(2, 32);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < len; i++) {
            double x = xShift + (double) i * xStep;
            double y = ANCHOR_DATA[start + i] * yScale + baseline;
            if (addJitter) {
                int signed = data.consumeInt(-1000, 1000);
                double jitter = Math.abs(y) * signed / (1000.0 * jitterDiv);
                y += jitter;
                if (y <= 0.0) {
                    y = baseline > 0.0 ? baseline : 1e-18;
                }
            }
            fitter.addObservedPoint(x, y);
        }

        runFitWithRootCauseFiltering(fitter, true);

        runSiblingAgreementOracle(start, len, xShift, xStep, yScale, baseline, addJitter, jitterDiv, data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        runFitWithRootCauseFiltering(fitter, true);
    }

    private static void runFitWithRootCauseFiltering(GaussianFitter fitter, boolean validByConstruction) {
        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (Error e) {
            if (validByConstruction && isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void runSiblingAgreementOracle(int start, int len, int xShift, int xStep,
                                                  double yScale, double baseline,
                                                  boolean addJitter, int jitterDiv,
                                                  FuzzedDataProvider data) {
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < len; i++) {
            double x = xShift + (double) i * xStep;
            double y = ANCHOR_DATA[start + i] * yScale + baseline;
            if (addJitter) {
                int signed = data.consumeInt(-1000, 1000);
                double jitter = Math.abs(y) * signed / (1000.0 * jitterDiv);
                y += jitter;
                if (y <= 0.0) {
                    y = baseline > 0.0 ? baseline : 1e-18;
                }
            }
            fitterA.addObservedPoint(x, y);
            fitterB.addObservedPoint(x, y);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = fitterA.fit();
        } catch (Throwable t) {
            return;
        }
        try {
            rhs = fitterB.fit(guess);
        } catch (Throwable t) {
            return;
        }

        // Documented sibling-agreement guarantee: fit() computes an initial guess from the
        // current observations and then delegates to the overload taking that initial guess.
        // Therefore, for the same observations and the same guessed parameters, fit() and
        // fit(initialGuess) must agree whenever both calls succeed. A throw-deleting or
        // wrong-delegation patch can violate this without crashing.
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(initialGuess) returned different shapes input=len="
                    + len + " lhs=" + describe(lhs) + " rhs=" + describe(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                return;
            }
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(initialGuess) input=len="
                        + len + ",start=" + start + ",xShift=" + xShift + ",xStep=" + xStep
                        + ",yScale=" + yScale + ",baseline=" + baseline
                        + " lhs=" + describe(lhs) + " rhs=" + describe(rhs));
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!isGroundTruthFamily(t)) {
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

    private static boolean isGroundTruthFamily(Throwable t) {
        Class<?> c = t.getClass();
        while (c != null) {
            String n = c.getName();
            if ("org.apache.commons.math.exception.NotStrictlyPositiveException".equals(n)) {
                return true;
            }
            c = c.getSuperclass();
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

    private static String describe(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}