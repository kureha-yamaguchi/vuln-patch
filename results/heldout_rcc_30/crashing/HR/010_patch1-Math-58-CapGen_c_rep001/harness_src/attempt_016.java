package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }

        tryGuess(fitter);
        double[] fit1 = tryFit(fitter, true);
        if (fit1 != null) {
            double[] fit2 = tryFit(buildDuplicatedWeighted(ANCHOR_DATA), true);
            if (fit2 != null) {
                assertEquivalentFit("anchor-dup-weight", fit1, fit2, ANCHOR_DATA.length);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 24);
        double[] series = new double[n];

        double amplitude = 1e-3 + (Math.abs(data.consumeInt(-200000, 200000)) / 200.0);
        double sigma = 0.2 + (data.consumeInt(1, 2000) / 200.0);
        double center = data.consumeInt(-2 * n, 3 * n) / 2.0;
        double baseline = Math.abs(data.consumeInt(-1000, 1000)) / 1000000.0;
        double xShift = data.consumeInt(-1000, 1000) / 50.0;

        for (int i = 0; i < n; i++) {
            double x = xShift + i;
            double d = (x - center) / sigma;
            double y = amplitude * Math.exp(-0.5 * d * d) + baseline;
            if (data.consumeBoolean()) {
                y += (Math.abs(data.consumeInt(-1000, 1000)) / 1000000000.0);
            }
            series[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < series.length; i++) {
            fitter.addObservedPoint(xShift + i, series[i]);
        }

        tryGuess(fitter);
        double[] fit1 = tryFit(fitter, true);
        if (fit1 == null) {
            return;
        }

        GaussianFitter dup = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < series.length; i++) {
            double x = xShift + i;
            dup.addObservedPoint(0.5, x, series[i]);
            dup.addObservedPoint(0.5, x, series[i]);
        }

        double[] fit2 = tryFit(dup, true);
        if (fit2 == null) {
            return;
        }

        /*
         * Sound metamorphic oracle:
         * least-squares fitting depends on the weighted residual objective.
         * Replacing each observed point (w, x, y) by two identical points
         * (w/2, x, y) and (w/2, x, y) preserves that objective exactly, so any
         * correct implementation should produce an equivalent optimum.
         * A throw-deleting patch can still violate this by routing fit() through
         * the wrong function and optimizing a different objective.
         */
        assertEquivalentFit("dup-weight-fit", fit1, fit2, series.length);
    }

    private static GaussianFitter buildDuplicatedWeighted(double[] data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < data.length; i++) {
            fitter.addObservedPoint(0.5, i, data[i]);
            fitter.addObservedPoint(0.5, i, data[i]);
        }
        return fitter;
    }

    private static void tryGuess(GaussianFitter fitter) {
        try {
            WeightedObservedPoint[] obs = fitter.getObservations();
            GaussianFitter.ParameterGuesser guesser = new GaussianFitter.ParameterGuesser(obs);
            double[] g1 = guesser.guess();
            double[] g2 = guesser.guess();
            if (g1 == g2) {
                throw new RuntimeException("[oracle:guess-clone-identity] guess returned same array instance twice");
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static double[] tryFit(GaussianFitter fitter, boolean validByConstruction) {
        try {
            return fitter.fit();
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                rethrowUnchecked(t);
            }
            return null;
        }
    }

    private static boolean isRootCause(Throwable t) {
        String name = t.getClass().getName();
        if (!name.endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls) && "validateParameters".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static void assertEquivalentFit(String oracleId, double[] a, double[] b, int n) {
        if (a.length != b.length) {
            throw new RuntimeException("[oracle:" + oracleId + "] parameter length mismatch " + a.length + " vs " + b.length);
        }
        for (int i = 0; i < a.length; i++) {
            double av = a[i];
            double bv = b[i];
            double scale = Math.max(1.0, Math.max(Math.abs(av), Math.abs(bv)));
            double tol = Math.max(1e-6, 0.05 * scale + 1e-4 * n);
            if (Double.isNaN(av) || Double.isNaN(bv) || Math.abs(av - bv) > tol) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation param[" + i + "] lhs=" + av + " rhs=" + bv + " tol=" + tol);
            }
        }
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}