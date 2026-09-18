package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.analysis.function.Gaussian;
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
        runScenario(ANCHOR_DATA);
        runScenario(buildPositiveGaussianLikeSeries(data, data.consumeInt(3, 40)));
    }

    private static void runScenario(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        try {
            double[] viaNoArg = fitter.fit();

            // Contract/oracle: fit() computes ParameterGuesser(getObservations()).guess() and then
            // delegates to the sibling overload fit(double[] initialGuess), so both overloads must
            // agree for the same observations and the exact computed guess.
            WeightedObservedPoint[] observations = fitter.getObservations();
            double[] guessed = (new GaussianFitter.ParameterGuesser(observations)).guess();

            GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter2.addObservedPoint(i, ys[i]);
            }

            double[] viaExplicitGuess = fitter2.fit(guessed);
            assertSameResult(ys, viaNoArg, viaExplicitGuess);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static double[] buildPositiveGaussianLikeSeries(FuzzedDataProvider data, int len) {
        double[] ys = new double[len];

        double norm = 0.1 + (data.consumeInt(1, 5000) / 1000.0);
        double mean = data.consumeInt(0, len - 1);
        double sigma = 0.2 + (data.consumeInt(1, 2000) / 1000.0);
        double baseline = data.consumeBoolean() ? (data.consumeInt(0, 100) / 1000000.0) : 0.0;

        Gaussian gaussian = new Gaussian(norm, mean, sigma);
        for (int i = 0; i < len; i++) {
            double y = gaussian.value(i) + baseline;
            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 100) / 1000000000.0;
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(1e-12, baseline + 1e-12);
            }
            ys[i] = y;
        }

        int extraBumps = data.consumeInt(0, 2);
        for (int b = 0; b < extraBumps; b++) {
            int idx = data.consumeInt(0, len - 1);
            ys[idx] += data.consumeInt(0, 100) / 1000000.0;
        }

        for (int i = 0; i < len; i++) {
            if (!(ys[i] > 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                ys[i] = 1e-12;
            }
        }

        return ys;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertSameResult(double[] input, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) length mismatch inputLen="
                    + input.length + " lhsLen=" + (a == null ? -1 : a.length) + " rhsLen=" + (b == null ? -1 : b.length));
        }

        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                            + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-7 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                        + input.length + " idx=" + i + " lhs=" + x + " rhs=" + y);
            }
        }
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}