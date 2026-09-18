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

        int n = data.consumeInt(5, 40);
        double amplitude = positiveScaled(data.consumeInt(), 1.0e-6, 1.0e3);
        double sigma = positiveScaled(data.consumeInt(), 0.2, 20.0);
        double mean = n + positiveScaled(data.consumeInt(), 0.5, 60.0);
        double baseline = positiveScaled(data.consumeInt(), 0.0, 1.0);
        double noiseScale = positiveScaled(data.consumeInt(), 0.0, 0.15);

        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double x = i;
            double z = (x - mean) / sigma;
            double gaussian = amplitude * Math.exp(-0.5 * z * z);
            double signedNoise = (data.consumeInt(-1000, 1000) / 1000.0) * noiseScale * Math.max(gaussian, 1.0e-12);
            double v = baseline + gaussian + signedNoise;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                v = Math.max(1.0e-300, baseline + gaussian);
            }
            y[i] = v;
        }

        runFitOnce(y, true);
        checkFitOverloadAgreement(y);
    }

    private static void runAnchor() {
        runFitOnce(ANCHOR_DATA, true);
        checkFitOverloadAgreement(ANCHOR_DATA);
    }

    private static double[] runFitOnce(double[] y, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }
        try {
            return fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        } catch (Error t) {
            return null;
        }
    }

    private static void checkFitOverloadAgreement(double[] y) {
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitterA.addObservedPoint(i, y[i]);
            fitterB.addObservedPoint(i, y[i]);
        }

        try {
            double[] lhs = fitterA.fit();
            double[] guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
            double[] rhs = fitterB.fit(guess);

            /* Oracle: GaussianFitter.fit() is defined in the patched source as
             * computing ParameterGuesser(getObservations()).guess() and delegating
             * to the same-name overload fit(double[]). Therefore, for the same
             * observations on fresh fitters, fit() and fit(guessedInitialGuess)
             * must agree. A patch that merely suppresses the exception path or
             * routes fit() differently can violate this without throwing.
             */
            if (!sameResult(lhs, rhs)) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(ParameterGuesser(...).guess()) inputLength="
                        + y.length + " lhs=" + arrayToString(lhs) + " rhs=" + arrayToString(rhs));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            return;
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
        Package p = t.getClass().getPackage();
        if (p != null) {
            String name = p.getName();
            if ("org.apache.commons.math.exception".equals(name)
                    || name.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
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
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1.0e-7 * scale) {
                return false;
            }
        }
        return true;
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

    private static double positiveScaled(int raw, double min, double max) {
        long u = raw & 0xffffffffL;
        double f = u / (double) 0xffffffffL;
        return min + (max - min) * f;
    }
}