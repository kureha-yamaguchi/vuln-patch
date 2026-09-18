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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        tryFitOnSeries(ANCHOR_DATA, 0.0, true, true);

        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 1.0 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.5 + data.consumeInt(0, 2000) / 100.0;
        double startX = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.1 + data.consumeInt(0, 1000) / 100.0;
        double centerShift = 1.0 + data.consumeInt(0, 3000) / 50.0;
        double center = startX + (len - 1) * step + centerShift;
        double baseline = data.consumeBoolean() ? 0.0 : data.consumeInt(0, 1000) / 1_000_000.0;

        for (int i = 0; i < len; i++) {
            double x = startX + i * step;
            double dx = x - center;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent) + baseline;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12 * (i + 1);
            }
            ys[i] = y;
        }

        if (data.consumeBoolean()) {
            int bumpIndex = data.consumeInt(0, len - 1);
            ys[bumpIndex] += data.consumeInt(1, 1000) / 1_000_000.0;
        }

        tryFitOnSeries(ys, startX, step > 0.0, true);
    }

    private static void tryFitOnSeries(double[] ys, double startX, boolean distinctIncreasingX, boolean validByConstruction) {
        GaussianFitter fitterForFit = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterForOverload = new GaussianFitter(new LevenbergMarquardtOptimizer());

        double x = startX;
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            fitterForFit.addObservedPoint(x, y);
            fitterForOverload.addObservedPoint(x, y);
            x += 1.0;
        }

        double[] viaFit = null;
        boolean fitSucceeded = false;
        try {
            viaFit = fitterForFit.fit();
            fitSucceeded = true;
        } catch (RuntimeException t) {
            if (validByConstruction && isGroundTruthRootCauseFromFit(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterForOverload.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] viaOverload;
        try {
            viaOverload = fitterForOverload.fit(guess);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (fitSucceeded && distinctIncreasingX) {
            /* Contract/oracle:
             * GaussianFitter.fit() computes a guess from the current observations and delegates to
             * the same-named overload fit(double[] initialGuess). Therefore, for the same observations,
             * fit() and fit(new ParameterGuesser(getObservations()).guess()) must agree whenever both
             * calls return normally. A patch that merely suppresses the buggy throw or changes the call
             * target can violate this observable equivalence without throwing.
             */
            if (!sameParams(viaFit, viaOverload)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() != fit(guess()) ysLen=" + ys.length
                        + " lhs=" + format(viaFit) + " rhs=" + format(viaOverload));
            }
        }
    }

    private static boolean isGroundTruthRootCauseFromFit(RuntimeException t) {
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

    private static boolean isCleanRejection(RuntimeException t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] a) {
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