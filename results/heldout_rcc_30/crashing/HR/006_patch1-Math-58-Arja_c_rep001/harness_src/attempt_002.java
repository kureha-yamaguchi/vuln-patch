package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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

        exercise(anchor, true);

        int rounds = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 2) : 0);
        for (int r = 0; r < rounds; r++) {
            double[] ys = buildPositiveDataset(data);
            exercise(ys, true);
        }
    }

    private static void exercise(double[] ys, boolean validByConstruction) {
        GaussianFitter explicitFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter noArgFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < ys.length; i++) {
            explicitFitter.addObservedPoint(i, ys[i]);
            noArgFitter.addObservedPoint(i, ys[i]);
        }

        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(explicitFitter.getObservations()).guess();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] explicitResult;
        try {
            explicitResult = explicitFitter.fit(guess);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] noArgResult;
        try {
            noArgResult = noArgFitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }

        /*
         * Contract/oracle: GaussianFitter.fit() computes
         * new ParameterGuesser(getObservations()).guess() and then delegates to fit(initialGuess).
         * Therefore, on the same observations, no-arg fit() and explicit fit(guess) must agree.
         * A patch that merely suppresses the exception or changes behavior in only one path violates
         * this equivalence even when both calls return.
         */
        if (!sameParameters(explicitResult, noArgResult)) {
            throw new RuntimeException(
                "[oracle:fit-delegate] metamorphic violation: fit() != fit(guess)"
                    + " inputLength=" + ys.length
                    + " explicit=" + format(explicitResult)
                    + " noArg=" + format(noArgResult));
        }
    }

    private static double[] buildPositiveDataset(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 32);
        double[] ys = new double[n];

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            double amplitude = 1.0 + data.consumeInt(0, 99999) / 1000.0;
            double baseline = data.consumeInt(0, 999) / 1000000.0;
            int center = data.consumeInt(0, n - 1);
            double sigma = 1.0 + data.consumeInt(0, 9);
            for (int i = 0; i < n; i++) {
                double dx = i - center;
                double v = baseline + amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
                double jitter = 1.0 + (data.consumeByte() / 512.0);
                ys[i] = positiveFinite(v * jitter);
            }
        } else if (mode == 1) {
            double start = 1.0e-12 + data.consumeInt(0, 999) / 1.0e12;
            double factor = 1.05 + data.consumeInt(0, 299) / 100.0;
            double v = start;
            for (int i = 0; i < n; i++) {
                ys[i] = positiveFinite(v);
                v *= factor;
            }
        } else {
            double amplitude = 1.0 + data.consumeInt(0, 99999) / 1000.0;
            int leftPeak = data.consumeInt(0, Math.max(0, n / 2));
            int rightPeak = data.consumeInt(Math.min(leftPeak, n - 1), n - 1);
            double sigma1 = 1.0 + data.consumeInt(0, 5);
            double sigma2 = 1.0 + data.consumeInt(0, 5);
            for (int i = 0; i < n; i++) {
                double d1 = i - leftPeak;
                double d2 = i - rightPeak;
                double v = amplitude * Math.exp(-(d1 * d1) / (2.0 * sigma1 * sigma1))
                         + (amplitude / 2.0) * Math.exp(-(d2 * d2) / (2.0 * sigma2 * sigma2))
                         + 1.0e-15;
                ys[i] = positiveFinite(v);
            }
        }

        return ys;
    }

    private static double positiveFinite(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1.0e-15;
        }
        return v;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls)
                    && ("fit".equals(method) || "getObservations".equals(method)))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls)
                    && "guess".equals(method))) {
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

    private static boolean sameParameters(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1.0e-6 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}