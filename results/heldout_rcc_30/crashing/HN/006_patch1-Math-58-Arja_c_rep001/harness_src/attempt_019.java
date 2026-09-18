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
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        invokeFitValid(fitter, true);

        // Contract/oracle: GaussianFitter.fit() is specified by its own body to compute
        // a ParameterGuesser guess and delegate to fit(double[] initialGuess). Therefore,
        // for any input where both calls succeed, they must agree on the fitted parameters.
        // A patch that merely suppresses the exception or changes behavior on only one path
        // would violate this sibling-overload relation.
        checkSiblingAgreement(copyAnchorFitter(), true);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double norm = positiveFromInt(data.consumeInt(), 1.0e-6, 1.0e3);
        double mean = boundedFromInt(data.consumeInt(), -100.0, 100.0);
        double sigma = positiveFromInt(data.consumeInt(), 1.0e-3, 20.0);
        double startX = boundedFromInt(data.consumeInt(), -100.0, 100.0);
        double step = positiveFromInt(data.consumeInt(), 1.0e-3, 5.0);
        double shift = boundedFromInt(data.consumeInt(), -3.0, 3.0);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            double x = startX + (i * step) + shift;
            double dx = x - mean;
            double y = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            fitter.addObservedPoint(x, y);
        }

        invokeFitValid(fitter, true);
        checkSiblingAgreement(fitter, true);
    }

    private static GaussianFitter copyAnchorFitter() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        return fitter;
    }

    private static void invokeFitValid(GaussianFitter fitter, boolean validByConstruction) {
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static void checkSiblingAgreement(GaussianFitter fitter, boolean validByConstruction) {
        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitter.fit(guess);
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                rethrowUnchecked(t);
            }
            return;
        }

        if (viaNoArg == null || viaGuess == null || viaNoArg.length != viaGuess.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() and fit(double[]) returned incompatible arrays");
        }

        for (int i = 0; i < viaNoArg.length; i++) {
            double a = viaNoArg[i];
            double b = viaGuess[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            double diff = Math.abs(a - b);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (diff > 1.0e-8 * scale) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() must agree with fit(guess()) index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!isRootCauseFamily(t)) {
            return false;
        }
        return stackPassesThroughGaussianFitterFit(t);
    }

    private static boolean isRootCauseFamily(Throwable t) {
        String name = t.getClass().getName();
        return name.equals("org.apache.commons.math.exception.NotStrictlyPositiveException")
            || name.equals("org.apache.commons.math.exception.NumberIsTooSmallException")
            || name.equals("org.apache.commons.math.exception.MathIllegalNumberException")
            || name.equals("org.apache.commons.math.exception.MathIllegalArgumentException")
            || t instanceof IllegalArgumentException;
    }

    private static boolean stackPassesThroughGaussianFitterFit(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static double boundedFromInt(int v, double min, double max) {
        long u = v & 0xffffffffL;
        double r = u / (double) 0xffffffffL;
        return min + (max - min) * r;
    }

    private static double positiveFromInt(int v, double min, double max) {
        double x = boundedFromInt(v, min, max);
        return x <= 0.0 ? min : x;
    }
}