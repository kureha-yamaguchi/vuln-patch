package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases = data.consumeInt(1, 3);
        }

        for (int c = 0; c < cases; c++) {
            int n = boundedInt(data, 5, 30);
            double start = boundedInt(data, -1000, 1000) / 10.0;
            double step = boundedInt(data, 1, 50) / 10.0;
            double amplitude = boundedInt(data, 1, 1000) / 10.0;
            double sigma = boundedInt(data, 1, 200) / 10.0;
            double mean = start + step * boundedInt(data, 0, n - 1) + boundedInt(data, -20, 20) / 10.0;
            double baseline = boundedInt(data, 0, 50) / 1000.0;

            double[] xs = new double[n];
            double[] ys = new double[n];

            for (int i = 0; i < n; i++) {
                xs[i] = start + i * step;
                double dx = xs[i] - mean;
                double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
                ys[i] = y;
            }

            runValidDataset(xs, ys);
        }
    }

    private static void runAnchor() {
        double[] data = {
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

        GaussianFitter fitter = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        for (int i = 0; i < data.length; i++) {
            fitter.addObservedPoint(i, data[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidDataset(double[] xs, double[] ys) {
        GaussianFitter fitterA = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );
        GaussianFitter fitterB = new GaussianFitter(
            new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
        );

        for (int i = 0; i < xs.length; i++) {
            fitterA.addObservedPoint(xs[i], ys[i]);
            fitterB.addObservedPoint(xs[i], ys[i]);
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitterA.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] viaGuess;
        try {
            viaGuess = fitterB.fit(guess);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() is implemented as computing
         * new ParameterGuesser(getObservations()).guess() and delegating to
         * the sibling overload fit(double[] initialGuess). Therefore, for the
         * same observations, fit() and fit(guess()) must agree. A patch that
         * suppresses the exception or changes fit()'s behavior without
         * preserving that delegation violates this sibling-agreement relation.
         */
        if (!sameArray(viaNoArg, viaGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess()) " +
                "inputXs=" + Arrays.toString(xs) +
                " inputYs=" + Arrays.toString(ys) +
                " lhs=" + Arrays.toString(viaNoArg) +
                " rhs=" + Arrays.toString(viaGuess)
            );
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
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
            if (diff > 1.0e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static int boundedInt(FuzzedDataProvider data, int min, int max) {
        if (data.remainingBytes() <= 0) {
            return min;
        }
        return data.consumeInt(min, max);
    }
}