package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
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

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchor.length; i++) {
            fitter.addObservedPoint(i, anchor[i]);
        }

        double[] lhs;
        try {
            lhs = fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitter.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        // Contract/oracle: fit() is the no-arg sibling overload that is supposed to use the
        // guessed initial parameters and therefore agree with fit(double[] initialGuess) given
        // that same guessed array. A patch that only suppresses the throw or skips real fitting
        // can violate this observable overload-equivalence without crashing.
        assertEquivalentResults(anchor, lhs, rhs);
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double amplitude = positiveScale(data.consumeInt(0, 60));
        double sigma = 0.2 + (data.consumeInt(0, 500) / 25.0);
        double center = (len - 1) + 1.0 + data.consumeInt(0, 30);
        boolean addNoise = data.consumeBoolean();

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = i;
            double exponent = -((x - center) * (x - center)) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent);
            if (addNoise) {
                int tweak = data.consumeInt(0, 20);
                y *= 1.0 + (tweak / 1000.0);
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(1e-300, amplitude * 1e-12);
            }
            ys[i] = y;
            fitter.addObservedPoint(x, y);
        }

        double[] lhs;
        try {
            lhs = fitter.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitter.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        assertEquivalentResults(ys, lhs, rhs);
    }

    private static double positiveScale(int bucket) {
        return Math.pow(10.0, -bucket / 2.0);
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertEquivalentResults(double[] input, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(guess()) length mismatch inputLen="
                    + (input == null ? -1 : input.length)
                    + " lhsLen=" + (lhs == null ? -1 : lhs.length)
                    + " rhsLen=" + (rhs == null ? -1 : rhs.length));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
                continue;
            }
            if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: non-finite disagreement inputLen="
                        + input.length + " index=" + i + " lhs=" + a + " rhs=" + b);
            }
            double diff = Math.abs(a - b);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (diff > 1.0e-6 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) inputLen="
                        + input.length + " index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }
}