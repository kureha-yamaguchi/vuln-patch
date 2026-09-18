package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
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
        exerciseAnchor();
        exerciseFuzzed(data);
    }

    private static void exerciseAnchor() {
        double[] x = new double[ANCHOR_Y.length];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        runUnitWeightEquivalence(x, ANCHOR_Y, true);
    }

    private static void exerciseFuzzed(FuzzedDataProvider data) {
        int n = data.consumeInt(4, 32);
        double[] x = new double[n];
        double[] y = new double[n];

        double start = bounded(data.consumeInt(), -50.0, 50.0) / 5.0;
        double step = 0.25 + Math.floorMod(data.consumeInt(), 400) / 100.0;
        double norm = 0.1 + Math.floorMod(data.consumeInt(), 10000) / 10.0;
        double mean = start + step * data.consumeInt(0, n - 1);
        double sigma = 0.5 + Math.floorMod(data.consumeInt(), 2000) / 200.0;
        double floor = Math.floorMod(data.consumeInt(), 1000) * 1e-18;
        double[] params = new double[] { norm, mean, sigma };
        Gaussian.Parametric g = new Gaussian.Parametric();

        for (int i = 0; i < n; i++) {
            x[i] = start + step * i;
            double value;
            try {
                value = g.value(x[i], params);
            } catch (RuntimeException ex) {
                return;
            }
            double noiseScale = ((data.consumeByte() & 0xFF) / 255.0) * 0.02;
            y[i] = Math.max(0.0, value * (1.0 + noiseScale) + floor);
        }

        runUnitWeightEquivalence(x, y, true);

        if (data.consumeBoolean()) {
            double shifted = bounded(data.consumeInt(), -100.0, 100.0) / 7.0;
            double[] xs = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = x[i] + shifted;
            }
            runUnitWeightEquivalence(xs, y, true);
        }
    }

    private static void runUnitWeightEquivalence(double[] x, double[] y, boolean validByConstruction) {
        GaussianFitter implicit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter explicit = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < x.length; i++) {
            implicit.addObservedPoint(x[i], y[i]);
            explicit.addObservedPoint(1.0, x[i], y[i]);
        }

        try {
            implicit.getObservations();
            explicit.getObservations();
        } catch (RuntimeException ignored) {
            return;
        }

        try {
            GaussianFitter.ParameterGuesser pg1 = new GaussianFitter.ParameterGuesser(implicit.getObservations());
            GaussianFitter.ParameterGuesser pg2 = new GaussianFitter.ParameterGuesser(explicit.getObservations());
            pg1.guess();
            pg2.guess();
        } catch (Throwable t) {
            if (shouldReportRootCause(t, validByConstruction)) {
                throwUnchecked(t);
            }
            return;
        }

        double[] r1;
        double[] r2;
        try {
            r1 = implicit.fit();
            r2 = explicit.fit();
        } catch (Throwable t) {
            if (shouldReportRootCause(t, validByConstruction)) {
                throwUnchecked(t);
            }
            return;
        }

        if (!closeArray(r1, r2)) {
            throw new RuntimeException("[oracle:unit-weight] metamorphic violation: fit() on equivalent inputs disagrees for implicit-vs-explicit unit weights");
        }

        try {
            double[] g1 = new GaussianFitter.ParameterGuesser(implicit.getObservations()).guess();
            double[] g2 = new GaussianFitter.ParameterGuesser(explicit.getObservations()).guess();
            double[] viaGuess1 = implicit.fit(g1);
            double[] viaGuess2 = explicit.fit(g2);
            if (!closeArray(viaGuess1, viaGuess2)) {
                throw new RuntimeException("[oracle:unit-weight-fitdouble] metamorphic violation: fit(double[]) disagrees for implicit-vs-explicit unit weights");
            }
        } catch (Throwable t) {
            if (shouldReportRootCause(t, validByConstruction)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean closeArray(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double av = a[i];
            double bv = b[i];
            if (Double.isNaN(av) != Double.isNaN(bv)) {
                return false;
            }
            if (Double.isInfinite(av) || Double.isInfinite(bv)) {
                return false;
            }
            double tol = 1e-6 * Math.max(1.0, Math.max(Math.abs(av), Math.abs(bv))) + 1e-10;
            if (Math.abs(av - bv) > tol) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldReportRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!isNotStrictlyPositiveFamily(t)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cn = e.getClassName();
            String mn = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cn) && "fit".equals(mn))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cn) && ("fit".equals(mn) || "getObservations".equals(mn)))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cn) && "guess".equals(mn))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cn) && "validateParameters".equals(mn))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNotStrictlyPositiveFamily(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            String name = cur.getClass().getName();
            if (name.endsWith("NotStrictlyPositiveException")) {
                return true;
            }
        }
        return false;
    }

    private static double bounded(int value, double min, double max) {
        double span = max - min;
        double normalized = (value & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return min + normalized * span;
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