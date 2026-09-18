package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        try {
            runExplore(data);
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracleViolation(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        final double[] ys = new double[] {
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
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }

        fitter.fit();
    }

    private static void runExplore(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double amplitude = positiveFromInt(data.consumeInt(), 1.0e-12, 500.0);
        double mean = bounded(data.consumeInt(), -20.0, n - 1 + 20.0);
        double sigma = positiveFromInt(data.consumeInt(), 0.05, 20.0);
        double baseline = positiveFromInt(data.consumeInt(), 0.0, 1.0);

        GaussianFitter fitter1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = baseline + amplitude * Math.exp(-((x - mean) * (x - mean)) / (2.0 * sigma * sigma));
            if (data.consumeBoolean()) {
                double noiseScale = positiveFromInt(data.consumeInt(), 0.0, amplitude * 0.05 + 1.0e-12);
                double noise = signedFromInt(data.consumeInt(), noiseScale);
                y += noise;
                if (y < 0.0) {
                    y = 0.0;
                }
            }
            fitter1.addObservedPoint(x, y);
        }

        double[] fitNoArg;
        try {
            fitNoArg = fitter1.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (WeightedObservedPoint p : fitter1.getObservations()) {
            fitter2.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter2.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        final double[] fitWithGuess;
        try {
            fitWithGuess = fitter2.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/metamorphic check:
           GaussianFitter.fit() computes a ParameterGuesser guess from the current observations
           and then delegates to the same-name overload with that guess. Therefore, for the same
           observations, fit() and fit(new ParameterGuesser(getObservations()).guess()) must agree.
           A patch that merely suppresses the original throw or skips the intended delegation can
           return silently wrong parameters here even when no exception is thrown. */
        if (!sameParameters(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess) input=n=" + n
                    + ",amp=" + amplitude
                    + ",mean=" + mean
                    + ",sigma=" + sigma
                    + ",baseline=" + baseline
                    + " lhs=" + arrayToString(fitNoArg)
                    + " rhs=" + arrayToString(fitWithGuess)
            );
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static double positiveFromInt(int v, double min, double max) {
        double unit = (v & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return min + unit * (max - min);
    }

    private static double signedFromInt(int v, double magnitude) {
        double unit = (v / (double) Integer.MAX_VALUE);
        if (unit < -1.0) {
            unit = -1.0;
        } else if (unit > 1.0) {
            unit = 1.0;
        }
        return unit * magnitude;
    }

    private static double bounded(int v, double min, double max) {
        double unit = (v & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return min + unit * (max - min);
    }

    private static boolean sameParameters(double[] a, double[] b) {
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
            if (diff > 1.0e-6 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}