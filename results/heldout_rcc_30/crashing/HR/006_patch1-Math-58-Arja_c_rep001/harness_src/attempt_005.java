package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 30);
        double[] xs = new double[len];
        double[] ys = new double[len];

        double norm = 0.1d + (data.consumeInt(0, 100000) / 100.0d);
        double mean = data.consumeInt(0, len - 1) + (data.consumeInt(-100, 100) / 100.0d);
        double sigma = 0.1d + (data.consumeInt(0, 5000) / 200.0d);
        double scale = 0.5d + (data.consumeInt(0, 4000) / 1000.0d);
        boolean reverse = data.consumeBoolean();

        for (int i = 0; i < len; i++) {
            int pos = reverse ? (len - 1 - i) : i;
            double x = pos + (data.consumeInt(-50, 50) / 1000.0d);
            double dx = x - mean;
            double y = norm * Math.exp(-(dx * dx) / (2.0d * sigma * sigma));
            double perturb = 1.0d + (data.consumeInt(-20, 20) / 1000.0d);
            y = y * perturb * scale;
            if (!(y > 0.0d) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1.0e-12d;
            }
            xs[i] = x;
            ys[i] = y;
        }

        runScenario(xs, ys, true);
    }

    private static void runAnchor() {
        double[] data = new double[] {
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

        double[] xs = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            xs[i] = i;
        }
        runScenario(xs, data, true);
    }

    private static void runScenario(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitterForGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitterForGuess.addObservedPoint(xs[i], ys[i]);
        }

        WeightedObservedPoint[] beforeGuess = fitterForGuess.getObservations();
        double[] guessed;
        try {
            guessed = new GaussianFitter.ParameterGuesser(fitterForGuess.getObservations()).guess();
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            return;
        }
        WeightedObservedPoint[] afterGuess = fitterForGuess.getObservations();

        /* Contract/oracle:
         * getObservations() exposes the fitter's stored observations, and ParameterGuesser.guess()
         * computes parameters from those observations. Neither operation is documented or implemented
         * as mutating the receiver's stored sample points. If a "fix" merely suppresses the throw by
         * corrupting or rewriting observations, this before/after snapshot check fires.
         */
        if (!sameObservations(beforeGuess, afterGuess)) {
            throw new RuntimeException("[oracle:obs-stable-guess] metamorphic violation: observations changed across guess()");
        }

        GaussianFitter fitterForFitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitterForFitWithGuess.addObservedPoint(xs[i], ys[i]);
        }
        WeightedObservedPoint[] beforeFitWithGuess = fitterForFitWithGuess.getObservations();
        try {
            fitterForFitWithGuess.fit(guessed.clone());
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            return;
        }
        WeightedObservedPoint[] afterFitWithGuess = fitterForFitWithGuess.getObservations();
        if (!sameObservations(beforeFitWithGuess, afterFitWithGuess)) {
            throw new RuntimeException("[oracle:obs-stable-fitguess] metamorphic violation: observations changed across fit(double[])");
        }

        GaussianFitter fitterForPatchedEntry = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitterForPatchedEntry.addObservedPoint(xs[i], ys[i]);
        }
        WeightedObservedPoint[] beforeFit = fitterForPatchedEntry.getObservations();
        try {
            fitterForPatchedEntry.fit();
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            return;
        }
        WeightedObservedPoint[] afterFit = fitterForPatchedEntry.getObservations();
        if (!sameObservations(beforeFit, afterFit)) {
            throw new RuntimeException("[oracle:obs-stable-fit] metamorphic violation: observations changed across fit()");
        }
    }

    private static boolean sameObservations(WeightedObservedPoint[] a, WeightedObservedPoint[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i].getWeight()) != Double.doubleToLongBits(b[i].getWeight())) {
                return false;
            }
            if (Double.doubleToLongBits(a[i].getX()) != Double.doubleToLongBits(b[i].getX())) {
                return false;
            }
            if (Double.doubleToLongBits(a[i].getY()) != Double.doubleToLongBits(b[i].getY())) {
                return false;
            }
        }
        return true;
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (validByConstruction && isRootCause(t)) {
            rethrowUnchecked(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        String name = t.getClass().getName();
        if (!name.endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
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
}