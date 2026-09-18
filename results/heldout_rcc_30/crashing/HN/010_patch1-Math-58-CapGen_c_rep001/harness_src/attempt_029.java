package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
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
        runCase(anchorX, ANCHOR_Y, true);

        int cases = 1 + (data.remainingBytes() > 0 ? data.consumeInt(0, 2) : 0);
        for (int c = 0; c < cases; c++) {
            double[][] generated = buildIncreasingOneSidedGaussian(data);
            if (generated == null) {
                return;
            }
            runCase(generated[0], generated[1], true);
        }
    }

    private static double[][] buildIncreasingOneSidedGaussian(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return null;
        }

        int len = data.consumeInt(3, 40);
        double[] x = new double[len];
        double[] y = new double[len];

        int start = data.consumeInt(-100, 100);
        int step = data.consumeInt(1, 5);
        int extraToMean = data.consumeInt(1, 50);
        double norm = data.consumeInt(1, 1_000_000) / 1000.0;
        double sigma = data.consumeInt(1, 50_000) / 1000.0 + 0.001;

        for (int i = 0; i < len; i++) {
            x[i] = start + ((double) i) * step;
        }

        double mean = x[len - 1] + ((double) extraToMean) * step;
        Gaussian.Parametric g = new Gaussian.Parametric();
        double[] params = new double[] { norm, mean, sigma };

        try {
            for (int i = 0; i < len; i++) {
                y[i] = g.value(x[i], params);
                if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                    return null;
                }
                if (i > 0 && !(y[i] > y[i - 1])) {
                    return null;
                }
            }
        } catch (RuntimeException e) {
            return null;
        }

        return new double[][] { x, y };
    }

    private static void runCase(double[] x, double[] y, boolean validByConstruction) {
        try {
            GaussianFitter crashProbe = newFitterWithPoints(x, y);
            crashProbe.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Oracle: fit() and fit(double[] initialGuess) are documented sibling overloads for the
           same fitting operation, and the patched fit() implementation is supposed to delegate using
           the guessed initial parameters. On the same valid observations, successful executions must
           therefore agree on the returned parameters; a patch that merely suppresses the failure or
           bypasses the intended call path can violate this relation without throwing. */
        try {
            GaussianFitter f1 = newFitterWithPoints(x, y);
            double[] lhs = f1.fit();

            GaussianFitter f2 = newFitterWithPoints(x, y);
            double[] guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            double[] rhs = f2.fit(guess);

            assertArrayClose(x, y, lhs, rhs);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            return;
        }
    }

    private static GaussianFitter newFitterWithPoints(double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < x.length && i < y.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
        return fitter;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        return validByConstruction
            && (t instanceof NotStrictlyPositiveException)
            && hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
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
        return p != null && "org.apache.commons.math.exception".equals(p.getName());
    }

    private static void assertArrayClose(double[] x, double[] y, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload-len] metamorphic violation: length mismatch inputLen="
                + x.length + " aLen=" + (a == null ? -1 : a.length) + " bLen=" + (b == null ? -1 : b.length));
        }
        for (int i = 0; i < a.length; i++) {
            double da = a[i];
            double db = b[i];
            if (Double.isNaN(da) && Double.isNaN(db)) {
                continue;
            }
            if (Double.isInfinite(da) || Double.isInfinite(db)) {
                if (da == db) {
                    continue;
                }
                throw new RuntimeException("[oracle:fit-overload-finite] metamorphic violation: non-finite mismatch idx=" + i
                    + " inputLen=" + x.length + " lhs=" + da + " rhs=" + db);
            }
            double tol = 1.0e-7 * Math.max(1.0, Math.max(Math.abs(da), Math.abs(db)));
            if (Math.abs(da - db) > tol) {
                throw new RuntimeException("[oracle:fit-overload-value] metamorphic violation: fit() != fit(guess) idx=" + i
                    + " inputLen=" + x.length + " firstX=" + x[0] + " lastX=" + x[x.length - 1]
                    + " firstY=" + y[0] + " lastY=" + y[y.length - 1]
                    + " lhs=" + da + " rhs=" + db);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}