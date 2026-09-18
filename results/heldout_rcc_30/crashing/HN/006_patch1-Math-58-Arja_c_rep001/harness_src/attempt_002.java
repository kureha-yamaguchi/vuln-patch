package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.exception.NotStrictlyPositiveException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchor = new double[] {
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
        runCase(anchor, 0.0, 1.0, true);

        int cases = 1 + data.consumeInt(0, 3);
        for (int c = 0; c < cases; c++) {
            int len = data.consumeInt(5, 40);
            double x0 = data.consumeInt(-50, 50);
            double step = 1.0 + data.consumeInt(0, 4);

            double amplitude = 0.1 + data.consumeInt(0, 1000) / 10.0;
            double sigma = 0.25 + data.consumeInt(0, 400) / 20.0;
            double center = x0 + step * (len - 1 + data.consumeInt(1, 60));
            double baseline = data.consumeBoolean() ? data.consumeInt(0, 10) / 1000.0 : 0.0;
            double noiseScale = data.consumeInt(0, 200) / 10000.0;

            double[] ys = new double[len];
            for (int i = 0; i < len; i++) {
                double x = x0 + step * i;
                double z = (x - center) / sigma;
                double y = amplitude * Math.exp(-0.5 * z * z) + baseline;
                if (noiseScale > 0.0) {
                    double mult = 1.0 + ((double) data.consumeInt(-100, 100)) * noiseScale;
                    if (mult < 0.0) {
                        mult = 0.0;
                    }
                    y *= mult;
                }
                if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = 0.0;
                }
                ys[i] = y;
            }

            boolean reverse = data.consumeBoolean();
            if (reverse) {
                for (int i = 0; i < ys.length / 2; i++) {
                    double tmp = ys[i];
                    ys[i] = ys[ys.length - 1 - i];
                    ys[ys.length - 1 - i] = tmp;
                }
            }

            runCase(ys, x0, step, true);
        }
    }

    private static void runCase(double[] ys, double x0, double step, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(x0 + step * i, ys[i]);
        }

        double[] direct;
        try {
            direct = fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                throwUnchecked(t);
            }
            return;
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter2.addObservedPoint(x0 + step * i, ys[i]);
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitter2.getObservations()).guess();
        } catch (Throwable t) {
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitter2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /* Contract asserted: GaussianFitter.fit() computes ParameterGuesser(...).guess()
           and delegates to the sibling overload fit(double[] initialGuess). Therefore, for
           the same observations, fit() and fit(guess()) must produce the same result.
           A patch that only suppresses the exception or changes the call path incorrectly
           can violate this observable overload-agreement relation without throwing. */
        if (!sameArray(direct, viaGuess)) {
            throw new RuntimeException(
                "[oracle:overload-agreement] metamorphic violation: fit() vs fit(guess()) inputLength="
                    + ys.length + " lhs=" + arrayToString(direct) + " rhs=" + arrayToString(viaGuess));
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                return true;
            }
        }
        if (!validByConstruction) {
            return false;
        }
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

    private static boolean sameArray(double[] a, double[] b) {
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
            double scale = 1.0 + Math.max(Math.abs(x), Math.abs(y));
            if (diff > 1.0e-8 * scale) {
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
                sb.append(", ");
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