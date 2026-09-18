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
        runAnchor();

        int len = data.consumeInt(5, 40);
        double amplitude = 1.0 + data.consumeInt(0, 100000);
        double sigma = 0.5 + (data.consumeInt(0, 4000) / 200.0);
        double x0 = data.consumeInt(-20, 20);
        double step = 0.25 + (data.consumeInt(0, 150) / 100.0);

        /* One-sided valid Gaussian samples: the peak lies to the right of all observed x values.
         * This flips the patched boundary: the buggy fit() passes Gaussian.Parametric directly and
         * leaks when the optimizer explores sigma <= 0; the fixed fit() delegates to fit(double[])
         * which masks that during optimization. */
        double mean = x0 + step * (len - 1) + (0.25 + data.consumeInt(0, 4000) / 100.0) * sigma;

        double[] tail = new double[len];
        for (int i = 0; i < len; i++) {
            double x = x0 + step * i;
            double z = (x - mean) / sigma;
            tail[i] = amplitude * Math.exp(-0.5 * z * z);
        }

        runScenario(tail, x0, step, true);

        if (data.consumeBoolean()) {
            int extra = data.consumeInt(0, 10);
            double[] varied = new double[len + extra];
            for (int i = 0; i < varied.length; i++) {
                double x = x0 + step * i;
                double z = (x - mean) / sigma;
                varied[i] = amplitude * Math.exp(-0.5 * z * z);
            }
            runScenario(varied, x0, step, true);
        }
    }

    private static void runAnchor() {
        runScenario(ANCHOR, 0.0, 1.0, true);
    }

    private static void runScenario(double[] y, double x0, double step, boolean validByConstruction) {
        if (y == null || y.length < 3) {
            return;
        }

        GaussianFitter fitter = makeFitter(y, x0, step);

        try {
            /* Post-condition oracle:
             * ParameterGuesser.guess() returns Gaussian parameters [norm, mean, sigma].
             * Gaussian's sigma parameter is required to be strictly positive by the real library
             * validation in Gaussian.Parametric.validateParameters. For valid observations, a
             * correct guess() must therefore never hand back sigma <= 0. This catches a band-aid
             * that merely suppresses the known throw in fit() while still producing an invalid
             * starting point through the real helper reached by fit(). */
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            if (guess == null || guess.length < 3 || !(guess[2] > 0.0)) {
                throw new RuntimeException(
                    "[oracle:guess-pos-sigma] metamorphic violation: valid observations produced non-positive sigma guess="
                    + (guess == null ? "null" : String.valueOf(guess[2])) + " len=" + y.length);
            }
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            return;
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
        }
    }

    private static GaussianFitter makeFitter(double[] y, double x0, double step) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(x0 + step * i, y[i]);
        }
        return fitter;
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }

        if (isCleanRejection(t)) {
            return;
        }

        return;
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }

        String name = t.getClass().getName();
        if (!name.endsWith("NotStrictlyPositiveException")) {
            return false;
        }

        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String m = trace[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(m))) {
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
}