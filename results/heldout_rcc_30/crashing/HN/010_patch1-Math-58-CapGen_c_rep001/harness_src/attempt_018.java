package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 40);
        double[] xs = new double[len];
        double[] ys = new double[len];

        int step = data.consumeInt(1, 3);
        int start = data.consumeInt(-50, 50);
        boolean tailOnly = data.consumeBoolean();

        double amplitude = 1.0 + (data.consumeInt(0, 100000) / 1000.0);
        double sigma = 0.1 + (data.consumeInt(0, 20000) / 1000.0);

        double center;
        if (tailOnly) {
            center = start + (len - 1) * step + 5.0 + (data.consumeInt(0, 200) / 10.0);
        } else {
            center = start + (data.consumeInt(0, Math.max(1, (len - 1) * step * 10)) / 10.0);
        }

        double noiseScale = data.consumeInt(0, 1000) / 1_000_000.0;
        boolean forceMonotonePositive = data.consumeBoolean();

        for (int i = 0; i < len; i++) {
            double x = start + (double) i * step;
            xs[i] = x;
            double z = (x - center) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z);
            if (noiseScale > 0.0) {
                y += Math.abs(data.consumeInt(-1000, 1000)) * noiseScale;
            }
            if (forceMonotonePositive && i > 0 && y <= ys[i - 1]) {
                y = ys[i - 1] + 1e-12;
            }
            if (y <= 0.0 || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12 * (i + 1);
            }
            ys[i] = y;
        }

        runScenario(xs, ys);
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

        runScenario(xs, data);
    }

    private static void runScenario(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(fitter, xs, ys);

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // Documented relation: no-arg fit() computes a guess from the current observations
        // and then fits; fit(double[] initialGuess) is the same operation with that guess
        // supplied explicitly. For the exact same observations and exact same guess, both
        // overloads must agree. A patch that merely suppresses the exception path, skips
        // optimization, or returns a different result from the no-arg overload breaks this.
        GaussianFitter forGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(forGuess, xs, ys);

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(forGuess.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        GaussianFitter noArgFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter explicitGuessFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(noArgFitter, xs, ys);
        addPoints(explicitGuessFitter, xs, ys);

        final double[] lhs;
        final double[] rhs;
        try {
            lhs = noArgFitter.fit();
            rhs = explicitGuessFitter.fit(guess);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (!sameParams(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(guess()) " +
                "input=" + Arrays.toString(ys) +
                " lhs=" + Arrays.toString(lhs) +
                " rhs=" + Arrays.toString(rhs)
            );
        }
    }

    private static void addPoints(GaussianFitter fitter, double[] xs, double[] ys) {
        int n = Math.min(xs.length, ys.length);
        for (int i = 0; i < n; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                return x == y;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}