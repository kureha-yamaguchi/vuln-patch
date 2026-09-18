package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();

        int mode = data.consumeInt(0, 2);
        double[] series;
        if (mode == 0) {
            series = buildGaussianTailSeries(data);
        } else if (mode == 1) {
            series = buildPositiveMonotoneSeries(data);
        } else {
            series = buildAnchorLikeSeries(data);
        }

        if (series.length < 3) {
            return;
        }

        checkIndependentGuessDeterminism(series);
        checkCloneRouteConsistency(series);
    }

    private static void exerciseAnchor() {
        final double[] data = {
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
        checkCloneRouteConsistency(data);
    }

    private static void checkIndependentGuessDeterminism(double[] y) {
        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            addSeries(fitter, y);

            WeightedObservedPoint[] a = fitter.getObservations();
            WeightedObservedPoint[] b = fitter.getObservations();

            double[] g1 = new GaussianFitter.ParameterGuesser(a).guess();
            double[] g2 = new GaussianFitter.ParameterGuesser(b).guess();

            if (!closeArray(g1, g2, 0.0, 0.0)) {
                throw new RuntimeException("[oracle:guess-snapshot-recompute] metamorphic violation: identical observation snapshots produced different guesses");
            }
        } catch (RuntimeException t) {
            if (shouldPropagateAsOracle(t)) {
                throw t;
            }
        } catch (Throwable t) {
            // Rejection or unrelated failure: skip.
        }
    }

    private static void checkCloneRouteConsistency(double[] y) {
        GaussianFitter viaPublic = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter viaGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addSeries(viaPublic, y);
        addSeries(viaGuess, y);

        double[] independentGuess;
        double[] resultViaGuess;
        try {
            independentGuess = new GaussianFitter.ParameterGuesser(viaGuess.getObservations()).guess();
            resultViaGuess = viaGuess.fit(independentGuess);
        } catch (Throwable t) {
            return;
        }

        try {
            double[] resultViaPublic = viaPublic.fit();

            /* Contract used for this oracle:
             * GaussianFitter.fit() computes a guess from getObservations() and then fits from it.
             * Recomputing the same guess on an independently constructed fitter with the same observations
             * must therefore lead to the same fitted parameters. A throw-deleting or route-changing patch
             * can hide the known exception while still violating this equality.
             */
            if (!closeArray(resultViaPublic, resultViaGuess, 1e-8, 1e-6)) {
                throw new RuntimeException(
                    "[oracle:clone-route] metamorphic violation: fit() and fit(guess()) on equivalent cloned fitters disagree "
                    + "public=" + render(resultViaPublic) + " clone=" + render(resultViaGuess));
            }
        } catch (RuntimeException t) {
            if (isGroundTruthRootCause(t) && isValidSeries(y)) {
                throw new RuntimeException(
                    "[oracle:clone-route] metamorphic violation: fit() rejected valid data while fit(guess()) on an equivalent clone succeeded; guess="
                    + render(independentGuess) + " result=" + render(resultViaGuess), t);
            }
            if (shouldPropagateAsOracle(t)) {
                throw t;
            }
        } catch (Throwable t) {
            // Skip clean rejection and unrelated failures.
        }
    }

    private static boolean isValidSeries(double[] y) {
        if (y == null || y.length < 3) {
            return false;
        }
        for (int i = 0; i < y.length; i++) {
            if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                return false;
            }
        }
        return true;
    }

    private static void addSeries(GaussianFitter fitter, double[] y) {
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }
    }

    private static double[] buildGaussianTailSeries(FuzzedDataProvider data) {
        int n = data.consumeInt(20, 40);
        double norm = 1.0 + data.consumeInt(0, 10000);
        double sigma = 0.5 + (data.consumeInt(0, 4000) / 200.0);
        double mean = n + data.consumeInt(5, 80);
        double[] out = new double[n];
        org.apache.commons.math.analysis.function.Gaussian.Parametric g =
            new org.apache.commons.math.analysis.function.Gaussian.Parametric();
        double[] p = new double[] { norm, mean, sigma };
        for (int i = 0; i < n; i++) {
            double v = g.value(i, p);
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                v = Double.MIN_NORMAL;
            }
            out[i] = v;
        }
        return out;
    }

    private static double[] buildPositiveMonotoneSeries(FuzzedDataProvider data) {
        int n = data.consumeInt(20, 40);
        double[] out = new double[n];
        double current = Math.pow(10.0, -data.consumeInt(5, 30));
        for (int i = 0; i < n; i++) {
            int stepPow = data.consumeInt(0, 3);
            double mul = 1.0 + stepPow + (data.consumeInt(0, 99) / 100.0);
            current *= mul;
            if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current)) {
                current = Double.MAX_VALUE / 4.0;
            }
            out[i] = current;
        }
        return out;
    }

    private static double[] buildAnchorLikeSeries(FuzzedDataProvider data) {
        final double[] base = {
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
        int extraLeft = data.consumeInt(0, 4);
        int extraRight = data.consumeInt(0, 6);
        double scale = Math.pow(10.0, data.consumeInt(-2, 2));
        double[] out = new double[extraLeft + base.length + extraRight];
        for (int i = 0; i < extraLeft; i++) {
            double v = base[0] / Math.pow(10.0, extraLeft - i);
            out[i] = v > 0.0 ? v : Double.MIN_NORMAL;
        }
        for (int i = 0; i < base.length; i++) {
            double tweak = 1.0 + (data.consumeInt(-5, 5) * 0.01);
            double v = base[i] * scale * tweak;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                v = base[i];
            }
            out[extraLeft + i] = v;
        }
        double last = out[extraLeft + base.length - 1];
        for (int i = 0; i < extraRight; i++) {
            last *= 1.5 + (data.consumeInt(0, 50) / 100.0);
            out[extraLeft + base.length + i] = last;
        }
        return out;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        String name = t.getClass().getName();
        if (!name.endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        boolean hasValidate = false;
        boolean hasReachable = false;
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(c)
                && "validateParameters".equals(m)) {
                hasValidate = true;
            }
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))) {
                hasReachable = true;
            }
        }
        return hasValidate && hasReachable;
    }

    private static boolean shouldPropagateAsOracle(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean closeArray(double[] a, double[] b, double absTol, double relTol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) {
                return false;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(Math.abs(x), Math.abs(y));
            if (diff > absTol + relTol * scale) {
                return false;
            }
        }
        return true;
    }

    private static String render(double[] v) {
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
}