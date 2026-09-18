package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
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
        exerciseAnchor();

        int mode = data.consumeInt(0, 3);
        double[] ys;
        double xStart = data.consumeInt(-1000, 1000);
        double xStep = Math.max(1.0, data.consumeInt(1, 5));

        if (mode == 0) {
            ys = mutateAnchor(data);
        } else {
            ys = buildPositiveBellLikeSeries(data);
        }

        if (ys == null || ys.length < 3) {
            return;
        }

        exerciseFitOnSeries(xStart, xStep, ys, true);
        checkSiblingAgreement(xStart, xStep, ys);
    }

    private static void exerciseAnchor() {
        exerciseFitOnSeries(0.0, 1.0, ANCHOR_Y, true);
        checkSiblingAgreement(0.0, 1.0, ANCHOR_Y);
    }

    private static void exerciseFitOnSeries(double xStart, double xStep, double[] ys, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xStart + i * xStep, ys[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCauseFromFit(t)) {
                throwAsUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkSiblingAgreement(double xStart, double xStep, double[] ys) {
        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xStart + i * xStep;
            fitterA.addObservedPoint(x, ys[i]);
            fitterB.addObservedPoint(x, ys[i]);
        }

        final double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitterB.getObservations()).guess();
        } catch (Throwable t) {
            return;
        }

        final double[] lhs;
        final double[] rhs;
        try {
            lhs = fitterA.fit();
        } catch (Throwable t) {
            if (isRootCauseFromFit(t)) {
                throwAsUnchecked(t);
            }
            return;
        }
        try {
            rhs = fitterB.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() / fit(initialGuess) length mismatch input="
                + Arrays.toString(ys) + " lhs=" + Arrays.toString(lhs) + " rhs=" + Arrays.toString(rhs));
        }

        /* Contract/oracle: fit() is the no-arg overload whose implementation is documented by the code under patch:
           it computes ParameterGuesser(...).guess() and then delegates to fit(initialGuess). Therefore, for the same
           observations and the exact same guessed initial parameters, both overloads must produce the same result or
           both reject. A patch that merely deletes/avoids the throwing path can violate this without crashing. */
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > 1e-9 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() should agree with fit(initialGuess) input="
                    + Arrays.toString(ys) + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs) + " rhs=" + Arrays.toString(rhs));
            }
        }
    }

    private static double[] mutateAnchor(FuzzedDataProvider data) {
        int extraLeft = data.consumeInt(0, 5);
        int extraRight = data.consumeInt(0, 5);
        int len = extraLeft + ANCHOR_Y.length + extraRight;
        double[] ys = new double[len];

        for (int i = 0; i < extraLeft; i++) {
            ys[i] = positiveNoise(data, i + 1);
        }
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            double base = ANCHOR_Y[i];
            int tweak = data.consumeInt(-20, 20);
            double factor = 1.0 + (tweak / 1000.0);
            double v = base * factor;
            ys[extraLeft + i] = ensurePositiveFinite(v, base);
        }
        for (int i = 0; i < extraRight; i++) {
            ys[extraLeft + ANCHOR_Y.length + i] = positiveNoise(data, i + 17);
        }
        return ys;
    }

    private static double[] buildPositiveBellLikeSeries(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double mean = data.consumeInt(0, len - 1);
        double sigma = 0.5 + data.consumeInt(1, 200) / 20.0;
        double baseline = data.consumeInt(0, 1000) / 1_000_000.0;

        for (int i = 0; i < len; i++) {
            double dx = (i - mean) / sigma;
            double gaussian = amplitude * Math.exp(-0.5 * dx * dx);
            double noise = data.consumeInt(0, 1000) / 1_000_000.0;
            ys[i] = ensurePositiveFinite(baseline + gaussian + noise, 1e-12);
        }

        if (data.consumeBoolean()) {
            int idx = data.consumeInt(0, len - 1);
            ys[idx] = ensurePositiveFinite(ys[idx] * (1.0 + data.consumeInt(0, 100) / 1000.0), ys[idx]);
        }

        return ys;
    }

    private static double positiveNoise(FuzzedDataProvider data, int salt) {
        double exp = data.consumeInt(-30, -3);
        double mant = 1.0 + ((salt + data.consumeInt(0, 999)) / 1000.0);
        return ensurePositiveFinite(mant * Math.pow(10.0, exp), 1e-12);
    }

    private static double ensurePositiveFinite(double v, double fallback) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return fallback > 0.0 ? fallback : 1e-12;
        }
        return v;
    }

    private static boolean isRootCauseFromFit(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                && "fit".equals(st[i].getMethodName())) {
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

    private static void throwAsUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}