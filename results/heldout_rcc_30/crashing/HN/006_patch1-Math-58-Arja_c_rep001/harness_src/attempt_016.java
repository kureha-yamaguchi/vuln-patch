package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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
        exerciseValidInput(ANCHOR);

        int rounds = 1 + Math.min(4, Math.max(0, data.remainingBytes() / 8));
        for (int i = 0; i < rounds; i++) {
            double[] ys = data.consumeBoolean() ? generateTailData(data) : mutateAnchor(data);
            exerciseValidInput(ys);
        }
    }

    private static void exerciseValidInput(double[] ys) {
        double[] fitNoArg;
        try {
            fitNoArg = runFitNoArg(ys);
        } catch (Throwable t) {
            if (isRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] fitWithGuess;
        try {
            fitWithGuess = runFitWithGuessedInitialGuess(ys);
        } catch (Throwable t) {
            if (isRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        /* Oracle: fit() and fit(double[] initialGuess) are documented sibling overloads with the
         * same fitting contract; fit() computes the initial guess from ParameterGuesser and should
         * therefore be observationally equivalent to explicitly passing that same guessed array into
         * fit(double[]). A "fix" that merely suppresses the throw or skips the real fitting path
         * would break this relation even when no exception fires.
         */
        if (!sameParams(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) input="
                    + java.util.Arrays.toString(ys)
                    + " lhs=" + java.util.Arrays.toString(fitNoArg)
                    + " rhs=" + java.util.Arrays.toString(fitWithGuess));
        }
    }

    private static double[] runFitNoArg(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        return fitter.fit();
    }

    private static double[] runFitWithGuessedInitialGuess(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        return fitter.fit(guess);
    }

    private static double[] mutateAnchor(FuzzedDataProvider data) {
        int len = 3 + data.consumeInt(0, ANCHOR.length + 10);
        double[] ys = new double[len];
        int offset = data.consumeInt(0, Math.max(0, ANCHOR.length - 1));
        double scale = pow10(data.consumeInt(-3, 3));
        for (int i = 0; i < len; i++) {
            double base = ANCHOR[(offset + i) % ANCHOR.length];
            double tweak = 0.75 + (data.consumeInt(0, 1000) / 1000.0) * 0.5;
            double y = base * scale * tweak;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Double.MIN_VALUE;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double[] generateTailData(FuzzedDataProvider data) {
        int len = data.consumeInt(5, 40);
        double[] ys = new double[len];
        double amplitude = pow10(data.consumeInt(-6, 6));
        double sigma = data.consumeInt(1, 200) / 10.0;
        double mean = len + data.consumeInt(1, len * 3 + 5);
        for (int i = 0; i < len; i++) {
            double dx = i - mean;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent);
            double noise = 0.9 + (data.consumeInt(0, 1000) / 1000.0) * 0.2;
            y *= noise;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Double.MIN_VALUE;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double pow10(int exp) {
        return Math.pow(10.0, exp);
    }

    private static boolean sameParams(double[] a, double[] b) {
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
            if (diff > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction || t == null) {
            return false;
        }
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}