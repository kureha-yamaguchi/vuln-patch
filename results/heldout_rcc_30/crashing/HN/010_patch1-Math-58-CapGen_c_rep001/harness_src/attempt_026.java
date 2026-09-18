package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() is documented/implemented as "compute a guess from the current
         * observations, then delegate to fit(double[] initialGuess)". Therefore, for the same
         * observation set, fit() must produce the same result as fit(new ParameterGuesser(...).guess()).
         * A patch that merely suppresses the crashing path, skips delegation, or otherwise changes
         * behavior without preserving that contract will violate this equivalence.
         */

        final double[] anchor = new double[] {
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

        boolean stackHasFit = false;
        try {
            GaussianFitter fitter = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            fitter.fit();
        } catch (Throwable t) {
            stackHasFit = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    stackHasFit = true;
                    break;
                }
            }
            if (stackHasFit
                    && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException
                    && t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }

        try {
            GaussianFitter f1 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            GaussianFitter f2 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < anchor.length; i++) {
                f1.addObservedPoint(i, anchor[i]);
                f2.addObservedPoint(i, anchor[i]);
            }
            double[] lhs = f1.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            if (lhs == null || rhs == null || lhs.length != rhs.length) {
                throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) returned incompatible result shapes");
            }
            for (int i = 0; i < lhs.length; i++) {
                double a = lhs[i];
                double b = rhs[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    if (a != b) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() vs fit(guess) input=anchor idx=" + i + " lhs=" + a + " rhs=" + b);
                    }
                    continue;
                }
                double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > tol) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() vs fit(guess) input=anchor idx=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        int n = data.consumeInt(3, 32);
        double[] ys = new double[n];
        double[] xs = new double[n];

        int exp = data.consumeInt(8, 40);
        double y = Math.pow(10.0, -exp);
        if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
            y = 1.0e-12;
        }

        double x = 0.0;
        for (int i = 0; i < n; i++) {
            x += 1.0 + data.consumeInt(0, 3);
            xs[i] = x;

            int num = data.consumeInt(105, 400);
            double factor = num / 100.0;
            y *= factor;
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y) || y > 1.0e100) {
                y = Math.max(1.0e-300, Math.abs(y % 1.0e6) + 1.0e-12);
            }
            ys[i] = y;
        }

        try {
            GaussianFitter fitter = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < n; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
            fitter.fit();
        } catch (Throwable t) {
            boolean hasFit = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    hasFit = true;
                    break;
                }
            }
            if (hasFit
                    && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException
                    && t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            GaussianFitter f1 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            GaussianFitter f2 = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < n; i++) {
                f1.addObservedPoint(xs[i], ys[i]);
                f2.addObservedPoint(xs[i], ys[i]);
            }

            double[] lhs = f1.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            if (lhs == null || rhs == null || lhs.length != rhs.length) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) returned incompatible result shapes");
            }
            for (int i = 0; i < lhs.length; i++) {
                double a = lhs[i];
                double b = rhs[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    if (a != b) {
                        throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() vs fit(guess) inputLength=" + n + " idx=" + i + " lhs=" + a + " rhs=" + b);
                    }
                    continue;
                }
                double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > tol) {
                    throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() vs fit(guess) inputLength=" + n + " idx=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }
}