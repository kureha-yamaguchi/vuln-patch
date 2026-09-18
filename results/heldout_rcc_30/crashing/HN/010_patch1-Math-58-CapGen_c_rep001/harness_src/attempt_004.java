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
        runFitAndOracle(ANCHOR_DATA, 0.0);

        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 0.1 + data.consumeInt(0, 10000) / 200.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 1000.0;
        double center = data.consumeInt(0, len - 1) + (data.consumeInt(-500, 500) / 1000.0);
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000);

        for (int i = 0; i < len; i++) {
            double x = i;
            double z = (x - center) / sigma;
            double y = baseline + amplitude * Math.exp(-0.5 * z * z);

            int noiseMode = data.consumeInt(0, 3);
            if (noiseMode == 1) {
                y *= 1.0 + (data.consumeInt(-200, 200) / 10000.0);
            } else if (noiseMode == 2) {
                y += data.consumeInt(0, 1000) / 100000000.0;
            } else if (noiseMode == 3) {
                y *= 1.0 + (i % 2 == 0 ? 0.01 : -0.01);
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12;
            }
            ys[i] = y;
        }

        runFitAndOracle(ys, xOffset);
    }

    private static void runFitAndOracle(double[] ys, double xOffset) {
        GaussianFitter fitNoArg = buildFitter(ys, xOffset);
        try {
            fitNoArg.fit();
        } catch (Throwable t) {
            if (isBugRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract/oracle: GaussianFitter.fit() computes a parameter guess from the observations
           and then delegates to the overload taking that initial guess. Therefore, for the same
           observations, fit() and fit(guess) must agree. A patch that merely suppresses the throw
           or changes behavior on that path can violate this even when no exception is raised. */
        GaussianFitter fitterA = buildFitter(ys, xOffset);
        GaussianFitter fitterB = buildFitter(ys, xOffset);
        try {
            double[] lhs = fitterA.fit();
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            double[] rhs = fitterB.fit(guess);
            assertApproxEqual(lhs, rhs, ys, xOffset);
        } catch (Throwable t) {
            if (isBugRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static GaussianFitter buildFitter(double[] ys, double xOffset) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-12;
            }
            fitter.addObservedPoint(xOffset + i, y);
        }
        return fitter;
    }

    private static boolean isBugRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
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
        if (p != null) {
            String name = p.getName();
            if (name.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
    }

    private static void assertApproxEqual(double[] lhs, double[] rhs, double[] ys, double xOffset) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess) length mismatch inputLen="
                    + ys.length + " xOffset=" + xOffset + " lhs=" + arr(lhs) + " rhs=" + arr(rhs));
        }
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess) mismatch at index "
                            + i + " inputLen=" + ys.length + " xOffset=" + xOffset + " lhs=" + arr(lhs) + " rhs=" + arr(rhs));
                }
                continue;
            }
            double tol = 1.0e-6 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess) mismatch at index "
                        + i + " inputLen=" + ys.length + " xOffset=" + xOffset + " lhs=" + arr(lhs) + " rhs=" + arr(rhs));
            }
        }
    }

    private static String arr(double[] a) {
        if (a == null) {
            return "null";
        }
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