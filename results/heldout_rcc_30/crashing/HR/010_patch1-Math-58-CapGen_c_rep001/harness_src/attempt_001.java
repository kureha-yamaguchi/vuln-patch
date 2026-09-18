package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        runScenario(ANCHOR_DATA, 0.0, 1.0, true);

        double[] fuzzed = buildValidTailData(data);
        if (fuzzed != null) {
            int x0Int = data.consumeInt(-50, 50);
            int stepInt = data.consumeInt(1, 5);
            runScenario(fuzzed, x0Int, stepInt, true);
        }
    }

    private static void runScenario(double[] y, double x0, double step, boolean validByConstruction) {
        double[] implicit = null;
        Throwable implicitFailure = null;

        GaussianFitter fitterImplicit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(fitterImplicit, y, x0, step);
        try {
            implicit = fitterImplicit.fit();
        } catch (Throwable t) {
            if (isRootCause(t) && validByConstruction) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
            implicitFailure = t;
        }

        double[] explicit = null;
        Throwable explicitFailure = null;

        GaussianFitter fitterExplicit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(fitterExplicit, y, x0, step);
        try {
            double[] guess = new GaussianFitter.ParameterGuesser(fitterExplicit.getObservations()).guess();
            explicit = fitterExplicit.fit(guess);
        } catch (Throwable t) {
            if (isRootCause(t) && validByConstruction) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
            explicitFailure = t;
        }

        if (implicitFailure != null || explicitFailure != null) {
            return;
        }
        if (implicit == null || explicit == null) {
            return;
        }

        /* Contract used for this oracle:
           GaussianFitter.fit() computes a guess from getObservations() and then fits.
           GaussianFitter.fit(double[]) fits from an explicit initial guess.
           Therefore, for the same observations and the same guessed initial parameters,
           fit() and fit(new ParameterGuesser(getObservations()).guess()) must agree. */
        if (!sameParameters(implicit, explicit)) {
            throw new RuntimeException(
                "[oracle:fit-overload-consistency] metamorphic violation: fit() != fit(guess())"
                    + " implicit=" + formatArray(implicit)
                    + " explicit=" + formatArray(explicit)
                    + " x0=" + x0
                    + " step=" + step
                    + " n=" + y.length);
        }
    }

    private static void addPoints(GaussianFitter fitter, double[] y, double x0, double step) {
        double x = x0;
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(x, y[i]);
            x += step;
        }
    }

    private static double[] buildValidTailData(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double[] y = new double[n];

        double sigma = data.consumeInt(1, 20) + (data.consumeInt(0, 999) / 1000.0);
        double amplitude = Math.pow(10.0, data.consumeInt(-12, 6));
        double step = data.consumeInt(1, 5);
        double start = data.consumeInt(-50, 50);

        boolean centerAfter = data.consumeBoolean();
        double center;
        if (centerAfter) {
            center = start + step * (n - 1) + data.consumeInt(1, 40);
        } else {
            center = start - data.consumeInt(1, 40);
        }

        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            double d = x - center;
            double base = amplitude * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            double scale = 0.8 + (data.consumeInt(0, 400) / 1000.0);
            double v = base * scale;
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                v = Double.MIN_NORMAL;
            }
            y[i] = v;
        }
        return y;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(c) && "validateParameters".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))) {
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
        if (a.length != b.length) {
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
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1.0e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String formatArray(double[] a) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}