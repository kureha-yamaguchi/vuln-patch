package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;

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

        int exploreCount = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < exploreCount; i++) {
            double[] ys = buildPositiveTailDataset(data);
            runOneDataset(ys);
        }
    }

    private static void runAnchor() {
        runOneDataset(ANCHOR_DATA);
    }

    private static void runOneDataset(double[] ys) {
        if (ys == null || ys.length < 3) {
            return;
        }

        try {
            GaussianFitter fitter = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
            );
            for (int i = 0; i < ys.length; i++) {
                if (!isUsableFinitePositive(ys[i])) {
                    return;
                }
                fitter.addObservedPoint((double) i, ys[i]);
            }

            double[] params = fitter.fit();
            if (params == null || params.length != 3) {
                throw new RuntimeException("[oracle:fit-shape] metamorphic violation: fit() must return 3 Gaussian parameters inputLen=" + ys.length);
            }

            // Contract/oracle: the no-arg overload computes ParameterGuesser(getObservations()).guess()
            // and is intended to agree with the sibling overload fit(double[] initialGuess) when passed
            // that exact guess. A patch that merely suppresses the throw, skips delegation, or returns a
            // different fallback would break this overload-agreement relation.
            try {
                GaussianFitter fitterA = new GaussianFitter(
                    new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
                );
                GaussianFitter fitterB = new GaussianFitter(
                    new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer()
                );
                for (int i = 0; i < ys.length; i++) {
                    fitterA.addObservedPoint((double) i, ys[i]);
                    fitterB.addObservedPoint((double) i, ys[i]);
                }

                double[] lhs = fitterA.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
                double[] rhs = fitterB.fit(guess);

                if (!sameParams(lhs, rhs)) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: fit() must agree with fit(ParameterGuesser(...).guess()) inputLen="
                            + ys.length + " lhs=" + format(lhs) + " rhs=" + format(rhs)
                    );
                }
            } catch (RuntimeException ignored) {
                if (ignored instanceof NotStrictlyPositiveException && isRootCause(ignored)) {
                    throw ignored;
                }
                if (ignored.getMessage() != null && ignored.getMessage().startsWith("[oracle:")) {
                    throw ignored;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (t instanceof NotStrictlyPositiveException && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
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
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static double[] buildPositiveTailDataset(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);

        double norm = 1.0 + data.consumeInt(0, 500);
        double mean = 10.0 + data.consumeInt(0, 120);
        double sigma = 0.5 + (data.consumeInt(0, 400) / 20.0);
        double xStart = -20.0 + data.consumeInt(0, 40);
        double step = 0.2 + (data.consumeInt(0, 50) / 10.0);
        boolean reverse = data.consumeBoolean();

        double[] ys = new double[len];
        Gaussian.Parametric g = new Gaussian.Parametric();

        for (int i = 0; i < len; i++) {
            int idx = reverse ? (len - 1 - i) : i;
            double x = xStart + idx * step;
            double y;
            try {
                y = g.value(x, new double[] { norm, mean, sigma });
            } catch (RuntimeException ex) {
                return null;
            }

            if (!Double.isFinite(y) || y <= 0.0) {
                return null;
            }

            int noiseBucket = data.consumeInt(0, 20);
            double factor = 1.0 + (noiseBucket - 10) * 0.005;
            y *= factor;

            if (!Double.isFinite(y) || y <= 0.0) {
                return null;
            }

            ys[i] = y;
        }

        return ys;
    }

    private static boolean isUsableFinitePositive(double v) {
        return Double.isFinite(v) && v > 0.0;
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != 3 || b.length != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                return false;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-7 * scale) {
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
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}