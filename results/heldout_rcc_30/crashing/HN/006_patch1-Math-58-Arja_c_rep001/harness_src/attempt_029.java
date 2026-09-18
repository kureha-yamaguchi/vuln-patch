package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
            }
        }

        {
            double[] lhs = null;
            double[] rhs = null;
            boolean lhsOk = false;
            boolean rhsOk = false;

            try {
                GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f1.addObservedPoint(i, anchor[i]);
                }
                lhs = f1.fit();
                lhsOk = true;
            } catch (Throwable t) {
                boolean hasFitFrame = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        hasFitFrame = true;
                        break;
                    }
                }
                if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                    throw (RuntimeException) t;
                }
                return;
            }

            try {
                GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    g.addObservedPoint(i, anchor[i]);
                }
                double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

                GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
                for (int i = 0; i < anchor.length; i++) {
                    f2.addObservedPoint(i, anchor[i]);
                }
                rhs = f2.fit(guess);
                rhsOk = true;
            } catch (Throwable t) {
                return;
            }

            if (lhsOk && rhsOk) {
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                }
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
                    if (Math.abs(a - b) > 1.0e-8 * scale) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                }
            }
        }

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        double[] xs = new double[n];

        int center = data.consumeInt(0, n - 1);
        double sigma = data.consumeInt(1, 2000) / 200.0;
        double amplitude = 1.0e-12 + data.consumeInt(0, 1000000) / 1000.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double start = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;

        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
            double d = i - center;
            double bell = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            double noise = data.consumeInt(0, 1000) / 1000000000.0;
            ys[i] = baseline + bell + noise;
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1.0e-12;
            }
        }

        double[] lhs = null;
        double[] rhs = null;

        try {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f1.addObservedPoint(xs[i], ys[i]);
            }
            lhs = f1.fit();
        } catch (Throwable t) {
            boolean hasFitFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                        && "fit".equals(ste.getMethodName())) {
                    hasFitFrame = true;
                    break;
                }
            }
            if (hasFitFrame && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw (RuntimeException) t;
            }
            return;
        }

        try {
            GaussianFitter g = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                g.addObservedPoint(xs[i], ys[i]);
            }
            double[] guess = new GaussianFitter.ParameterGuesser(g.getObservations()).guess();

            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                f2.addObservedPoint(xs[i], ys[i]);
            }
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * The no-arg overload is specified by its own implementation to compute
         * ParameterGuesser(getObservations()).guess() and then delegate to fit(initialGuess).
         * Therefore, for the same observations, whenever both calls return normally,
         * their parameter arrays must agree. A patch that merely suppresses the crash or
         * changes delegation would violate this observable relation without throwing.
         */
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
        }
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
            if (Math.abs(a - b) > 1.0e-8 * scale) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(initialGuess) must agree on the same observations input=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
            }
        }
    }
}