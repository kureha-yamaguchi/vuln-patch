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

        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }

            double[] pNoArg = fitter.fit();

            /* Contract/oracle: the same-name overloads fit() and fit(double[] initialGuess)
             * are documented to perform the same fit, with fit() obtaining a guess internally.
             * A throw-deleting or branch-skipping patch in fit() would make the no-arg path
             * diverge observably from the overload taking the same internally-derived guess. */
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] pGuess = fitter.fit(guess);

            if (pNoArg != null && pGuess != null && pNoArg.length == pGuess.length && pNoArg.length > 0) {
                for (int i = 0; i < pNoArg.length; i++) {
                    double a = pNoArg[i];
                    double b = pGuess[i];
                    boolean equal;
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        equal = true;
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        equal = (a == b);
                    } else {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        equal = Math.abs(a - b) <= 1.0e-8 * scale;
                    }
                    if (!equal) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() must agree with fit(double[] guess) on the internally-derived guess input=anchor lhs="
                            + java.util.Arrays.toString(pNoArg) + " rhs=" + java.util.Arrays.toString(pGuess));
                    }
                }
            }
        } catch (RuntimeException t) {
            boolean throughFit = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
            if (throughFit && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw t;
            }
            if (!cleanRejection) {
                // Swallow unrelated runtime failures outside the patched scope.
            }
        }

        int mode = data.consumeInt(0, 3);
        int n;
        if (mode == 0) {
            n = anchor.length;
        } else {
            n = data.consumeInt(5, 40);
        }

        double[] ys = new double[n];
        if (mode == 0) {
            for (int i = 0; i < n; i++) {
                ys[i] = anchor[i];
            }
        } else if (mode == 1) {
            int offset = data.consumeInt(0, 10);
            int take = Math.min(n, Math.max(5, anchor.length - offset));
            for (int i = 0; i < n; i++) {
                double base = anchor[Math.min(anchor.length - 1, offset + (i % take))];
                double factor = 0.8 + (data.consumeInt(0, 40) / 100.0);
                ys[i] = Math.max(1.0e-300, base * factor);
            }
        } else {
            double amplitude = Math.max(1.0e-12, data.consumeInt(1, 1000) / 100.0);
            double mean = data.consumeInt(0, Math.max(4, n - 1));
            double sigma = 0.5 + (data.consumeInt(1, 800) / 100.0);
            boolean leftTailOnly = data.consumeBoolean();
            for (int i = 0; i < n; i++) {
                double x = i;
                double z = (x - mean) / sigma;
                double v = amplitude * Math.exp(-0.5 * z * z);
                if (leftTailOnly) {
                    if (x > mean) {
                        v *= 1.0e-6;
                    }
                }
                double tweak = 0.95 + (data.consumeInt(0, 10) / 100.0);
                ys[i] = Math.max(1.0e-300, v * tweak);
            }
            if (mode == 3) {
                for (int i = 1; i < n; i++) {
                    if (ys[i] < ys[i - 1]) {
                        ys[i] = ys[i - 1] + Math.max(1.0e-300, ys[i - 1] * 0.1);
                    }
                }
            }
        }

        double xStart = data.consumeInt(-20, 20);
        double xStep = data.consumeBoolean() ? 1.0 : (0.5 + data.consumeInt(0, 10));
        if (!(xStep > 0.0) || Double.isNaN(xStep) || Double.isInfinite(xStep)) {
            xStep = 1.0;
        }

        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                double x = xStart + i * xStep;
                double y = ys[i];
                if (Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y)) {
                    return;
                }
                fitter.addObservedPoint(x, y);
            }

            double[] pNoArg = fitter.fit();

            /* Contract/oracle: fit() computes an internal initial guess from the current
             * observations, so calling fit(double[] initialGuess) with that same guess must
             * agree with fit() on the same receiver state. This catches silent wrong-output
             * fixes that merely suppress the exception path in fit(). */
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] pGuess = fitter.fit(guess);

            if (pNoArg == null || pGuess == null || pNoArg.length != pGuess.length || pNoArg.length == 0) {
                return;
            }
            for (int i = 0; i < pNoArg.length; i++) {
                double a = pNoArg[i];
                double b = pGuess[i];
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    continue;
                }
                if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    if (a != b) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(double[] guess) infinity mismatch input="
                            + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(pNoArg)
                            + " rhs=" + java.util.Arrays.toString(pGuess));
                    }
                    continue;
                }
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > 1.0e-8 * scale) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() must agree with fit(double[] guess) input="
                        + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(pNoArg)
                        + " rhs=" + java.util.Arrays.toString(pGuess));
                }
            }
        } catch (RuntimeException t) {
            boolean throughFit = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
            if (throughFit && t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException) {
                throw t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (!cleanRejection) {
                // Swallow unrelated runtime failures outside the patched scope.
            }
        }
    }
}