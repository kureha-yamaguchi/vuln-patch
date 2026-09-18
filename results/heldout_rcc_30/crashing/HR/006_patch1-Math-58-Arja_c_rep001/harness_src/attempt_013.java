package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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

        int n = bounded(data.consumeInt(8, 40), 8, 40);
        int xStart = data.consumeInt(-20, 20);
        double amplitude = 1.0 + data.consumeInt(1, 5000);
        double sigma = 1.0 + data.consumeInt(1, 20);
        double mean = xStart + (n - 1) + data.consumeInt(5, 60);

        exerciseTailExactGaussian(n, xStart, amplitude, mean, sigma);

        if (data.remainingBytes() > 0) {
            int n2 = bounded(data.consumeInt(9, 30), 9, 30);
            int xStart2 = data.consumeInt(-15, 15);
            double amplitude2 = 1.0 + data.consumeInt(1, 2000);
            double sigma2 = 2.0 + data.consumeInt(1, 12);
            double mean2 = xStart2 + (n2 - 1) + data.consumeInt(3, 35);
            exerciseTailExactGaussian(n2, xStart2, amplitude2, mean2, sigma2);
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void exerciseTailExactGaussian(int n, int xStart, double amplitude, double mean, double sigma) {
        double[] xs = new double[n];
        double[] ys = new double[n];
        Gaussian.Parametric generator = new Gaussian.Parametric();
        double[] params = new double[] { amplitude, mean, sigma };

        for (int i = 0; i < n; i++) {
            xs[i] = xStart + i;
            ys[i] = generator.value(xs[i], params);
        }

        double[] direct = fitWithUnitWeights(xs, ys);
        double[] split = fitWithSplitWeights(xs, ys);

        if (direct != null) {
            /*
             * Oracle from the input itself: these observations were constructed from the real
             * Gaussian model with strictly positive sigma, so a correct GaussianFitter.fit()
             * is obligated to accept them and recover approximately the same parameters.
             * A patch that only suppresses the throw but returns a wrong fit still violates this.
             */
            assertCloseToTruth("tail-recover", params, direct, n);
        }

        if (direct != null && split != null) {
            /*
             * Same least-squares problem two ways: replacing one point of weight 1 by two
             * identical points of weight 0.5 preserves the objective exactly, so fit() must
             * report the same optimum on both datasets.
             */
            assertEquivalentFits("weight-split", direct, split, n);
        }
    }

    private static double[] fitWithUnitWeights(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
        try {
            return fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return null;
        }
    }

    private static double[] fitWithSplitWeights(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitter.addObservedPoint(0.5, xs[i], ys[i]);
            fitter.addObservedPoint(0.5, xs[i], ys[i]);
        }
        try {
            return fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return null;
        }
    }

    private static void assertCloseToTruth(String oracle, double[] expected, double[] actual, int n) {
        if (actual.length < 3) {
            throw new RuntimeException("[oracle:" + oracle + "] metamorphic violation: fit returned too few parameters len=" + actual.length);
        }

        double normTol = Math.max(1e-6, Math.abs(expected[0]) * 0.20);
        double meanTol = Math.max(1e-4, 0.35 + 0.03 * n);
        double sigmaTol = Math.max(1e-4, Math.abs(expected[2]) * 0.25);

        if (!close(actual[0], expected[0], normTol)
                || !close(actual[1], expected[1], meanTol)
                || !close(actual[2], expected[2], sigmaTol)) {
            throw new RuntimeException(
                "[oracle:" + oracle + "] metamorphic violation: exact Gaussian tail not recovered"
                    + " expected=[" + expected[0] + "," + expected[1] + "," + expected[2] + "]"
                    + " actual=[" + actual[0] + "," + actual[1] + "," + actual[2] + "]"
                    + " tolerances=[" + normTol + "," + meanTol + "," + sigmaTol + "]"
            );
        }
    }

    private static void assertEquivalentFits(String oracle, double[] a, double[] b, int n) {
        if (a.length < 3 || b.length < 3) {
            return;
        }

        double normTol = Math.max(1e-6, Math.max(Math.abs(a[0]), Math.abs(b[0])) * 0.05);
        double meanTol = Math.max(1e-4, 0.15 + 0.01 * n);
        double sigmaTol = Math.max(1e-4, Math.max(Math.abs(a[2]), Math.abs(b[2])) * 0.08);

        if (!close(a[0], b[0], normTol)
                || !close(a[1], b[1], meanTol)
                || !close(a[2], b[2], sigmaTol)) {
            throw new RuntimeException(
                "[oracle:" + oracle + "] metamorphic violation: splitting point weights changed fit"
                    + " lhs=[" + a[0] + "," + a[1] + "," + a[2] + "]"
                    + " rhs=[" + b[0] + "," + b[1] + "," + b[2] + "]"
                    + " tolerances=[" + normTol + "," + meanTol + "," + sigmaTol + "]"
            );
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (validByConstruction && t instanceof NotStrictlyPositiveException && hasReachableFrame(t)) {
            sneakyThrow(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean hasReachableFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                    || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                    || (cls.indexOf("GaussianFitter") >= 0 && cls.indexOf("ParameterGuesser") >= 0 && "guess".equals(method))) {
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

    private static boolean close(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    private static int bounded(int v, int min, int max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}