package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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
        double[] anchorX = new double[ANCHOR_Y.length];
        for (int i = 0; i < anchorX.length; i++) {
            anchorX[i] = i;
        }

        exercise(anchorX, ANCHOR_Y, true);

        int rounds = 1 + Math.max(0, Math.min(3, data.remainingBytes() / 4));
        for (int r = 0; r < rounds; r++) {
            int n = data.consumeInt(3, 40);
            double[] xs = new double[n];
            double[] ys = new double[n];

            double startX = data.consumeInt(-1000, 1000) / 10.0;
            double step = data.consumeInt(1, 50) / 10.0;
            double mean = startX + step * data.consumeInt(0, n - 1) + data.consumeInt(-50, 50) / 100.0;
            double sigma = data.consumeInt(1, 200) / 20.0;
            double norm = data.consumeInt(1, 10000) / 100.0;
            double baseline = data.consumeInt(0, 100) / 1000.0;

            for (int i = 0; i < n; i++) {
                xs[i] = startX + i * step;
                double z = (xs[i] - mean) / sigma;
                double y = baseline + norm * Math.exp(-0.5 * z * z);

                int ppm = data.consumeInt(-1000, 1000);
                y *= 1.0 + (ppm / 10000.0);
                if (y <= 0.0 || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = baseline + Math.abs(norm) * Math.exp(-0.5 * z * z);
                    if (y <= 0.0) {
                        y = 1.0e-12;
                    }
                }
                ys[i] = y;
            }

            exercise(xs, ys, true);
        }
    }

    private static void exercise(double[] xs, double[] ys, boolean validByConstruction) {
        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() is documented as the overload sibling of fit(double[] initialGuess),
         * and the patched code specifically changes fit() to delegate to fit(guess()).
         * Therefore, for the same observations, fit() must agree with fit(ParameterGuesser(...).guess()).
         * A patch that merely suppresses the exception or changes control flow to skip the real delegation
         * can violate this observable equivalence even if nothing throws.
         */
        try {
            GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitterA.addObservedPoint(xs[i], ys[i]);
                fitterB.addObservedPoint(xs[i], ys[i]);
            }

            double[] a = fitterA.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            double[] b = fitterB.fit(guess);

            if (!sameArray(a, b)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess())" +
                    " lhs=" + arrayToString(a) +
                    " rhs=" + arrayToString(b) +
                    " n=" + ys.length
                );
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        String pkg = p == null ? "" : p.getName();
        return pkg.startsWith("org.apache.commons.math.exception");
    }

    private static boolean sameArray(double[] a, double[] b) {
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
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1.0e-9 * scale) {
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}