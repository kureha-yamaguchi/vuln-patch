package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 40);
        double[] y = new double[len];

        int ampNum = data.consumeInt(1, 1000);
        int ampScale = data.consumeInt(0, 6);
        double amplitude = ampNum / Math.pow(10.0, ampScale);

        int meanExtra = data.consumeInt(5, 80);
        int sigmaInt = data.consumeInt(1, 20);
        int sigmaFrac = data.consumeInt(0, 999);
        double sigma = sigmaInt + (sigmaFrac / 1000.0);

        double xStep = data.consumeBoolean() ? 1.0 : (data.consumeInt(1, 10) / 10.0);
        double xStart = data.consumeInt(-50, 50);
        double mean = xStart + (len - 1) * xStep + meanExtra;

        boolean addFloor = data.consumeBoolean();
        double floor = addFloor ? Math.pow(10.0, -data.consumeInt(12, 30)) : 0.0;

        for (int i = 0; i < len; i++) {
            double x = xStart + i * xStep;
            double z = (x - mean) / sigma;
            double v = amplitude * Math.exp(-0.5 * z * z) + floor;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                return;
            }
            y[i] = v;
        }

        runExplore(y, xStart, xStep);

        if (data.remainingBytes() > 0) {
            int trimLeft = data.consumeInt(0, Math.max(0, len - 3));
            int trimRight = data.consumeInt(0, Math.max(0, len - trimLeft - 3));
            int newLen = len - trimLeft - trimRight;
            if (newLen >= 3) {
                double[] y2 = new double[newLen];
                for (int i = 0; i < newLen; i++) {
                    y2[i] = y[trimLeft + i];
                }
                runExplore(y2, xStart + trimLeft * xStep, xStep);
            }
        }
    }

    private static void runAnchor() {
        double[] y = new double[] {
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
        GaussianFitter fitter = newFitter(y, 0.0, 1.0);
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        try {
            GaussianFitter f1 = newFitter(y, 0.0, 1.0);
            double[] r1 = f1.fit();

            GaussianFitter f2 = newFitter(y, 0.0, 1.0);
            double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
            double[] r2 = f2.fit(guess);

            if (!sameArray(r1, r2)) {
                throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input=anchor lhs="
                        + java.util.Arrays.toString(r1) + " rhs=" + java.util.Arrays.toString(r2));
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runExplore(double[] y, double xStart, double xStep) {
        try {
            GaussianFitter fitter = newFitter(y, xStart, xStep);
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isValidation(t)) {
                return;
            }
            return;
        }

        try {
            GaussianFitter f1 = newFitter(y, xStart, xStep);
            double[] r1 = f1.fit();

            GaussianFitter f2 = newFitter(y, xStart, xStep);
            double[] guess = new GaussianFitter.ParameterGuesser(f2.getObservations()).guess();
            double[] r2 = f2.fit(guess);

            if (!sameArray(r1, r2)) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() must agree with fit(initialGuess) built from ParameterGuesser input="
                        + java.util.Arrays.toString(y) + " lhs=" + java.util.Arrays.toString(r1)
                        + " rhs=" + java.util.Arrays.toString(r2));
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static GaussianFitter newFitter(double[] y, double xStart, double xStep) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(xStart + i * xStep, y[i]);
        }
        return fitter;
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof NotStrictlyPositiveException && hasFitFrame(t);
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
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.doubleToLongBits(x) != Double.doubleToLongBits(y)) {
                return false;
            }
        }
        return true;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>rethrowUnchecked(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void rethrowUnchecked(Throwable t) throws E {
        throw (E) t;
    }
}