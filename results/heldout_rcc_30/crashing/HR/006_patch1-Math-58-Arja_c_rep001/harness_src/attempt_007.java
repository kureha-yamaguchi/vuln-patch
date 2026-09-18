package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double[] ANCHOR = {
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
        double[] xs = new double[ANCHOR.length];
        double[] ys = new double[ANCHOR.length];
        for (int i = 0; i < ANCHOR.length; i++) {
            xs[i] = i;
            ys[i] = ANCHOR[i];
        }
        exerciseScenario(xs, ys, true);
    }

    private static void exerciseFuzzed(FuzzedDataProvider data) {
        int coreLen = data.consumeInt(3, ANCHOR.length);
        int start = data.consumeInt(0, ANCHOR.length - coreLen);
        int prefix = data.consumeInt(0, 3);
        int suffix = data.consumeInt(0, 3);

        double scale = 0.25 + (data.consumeInt(0, 700) / 100.0);
        double shift = data.consumeInt(-50, 50) + (data.consumeInt(0, 999) / 1000.0);
        double step = 0.25 + (data.consumeInt(0, 300) / 100.0);

        double minPositive = Double.POSITIVE_INFINITY;
        for (double v : ANCHOR) {
            if (v > 0.0 && v < minPositive) {
                minPositive = v;
            }
        }
        if (!Double.isFinite(minPositive) || minPositive <= 0.0) {
            minPositive = 1e-30;
        }

        int n = prefix + coreLen + suffix;
        double[] xs = new double[n];
        double[] ys = new double[n];

        int idx = 0;
        for (int i = prefix; i > 0; i--) {
            xs[idx] = shift + (idx - prefix) * step;
            ys[idx] = minPositive * scale * (1.0 + 0.1 * i);
            idx++;
        }
        for (int i = 0; i < coreLen; i++) {
            xs[idx] = shift + (idx - prefix) * step;
            ys[idx] = ANCHOR[start + i] * scale;
            idx++;
        }
        for (int i = 0; i < suffix; i++) {
            xs[idx] = shift + (idx - prefix) * step;
            ys[idx] = minPositive * scale * (1.0 + 0.1 * (i + 1));
            idx++;
        }

        exerciseScenario(xs, ys, false);
    }

    private static void exerciseScenario(double[] xs, double[] ys, boolean anchor) {
        if (!isValidConstructedInput(xs, ys)) {
            return;
        }

        try {
            double[] forward = fitNoArg(xs, ys, false);
            double[] reversed = fitNoArg(xs, ys, true);

            if (forward != null && reversed != null) {
                assertOrderInvariance(xs, ys, forward, reversed, anchor);
            }
        } catch (RuntimeException e) {
            if (isRootCause(e) && isValidConstructedInput(xs, ys)) {
                throw e;
            }
        } catch (Error e) {
            if (isRootCause(e) && isValidConstructedInput(xs, ys)) {
                throw e;
            }
        }
    }

    private static double[] fitNoArg(double[] xs, double[] ys, boolean reverse) {
        GaussianFitter fitter =
            new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

        if (reverse) {
            for (int i = xs.length - 1; i >= 0; i--) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
        } else {
            for (int i = 0; i < xs.length; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
        }

        try {
            return fitter.fit();
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            throw e;
        } catch (Error e) {
            throw e;
        }
    }

    private static void assertOrderInvariance(double[] xs, double[] ys, double[] a, double[] b, boolean anchor) {
        if (a.length < 3 || b.length < 3) {
            return;
        }

        /* Least-squares fitting depends on the observed points, not the insertion order
           used to populate the fitter. Reversing the same valid observation set must
           produce the same Gaussian parameters up to small numerical noise. A patch that
           merely suppresses the old exception but changes the call path can violate this. */
        checkClose("norm", a[0], b[0], 1e-6, 1e-2, xs, ys, anchor);
        checkClose("mean", a[1], b[1], 1e-6, 1e-6, xs, ys, anchor);
        checkClose("sigma", a[2], b[2], 1e-6, 1e-2, xs, ys, anchor);

        if (!(a[2] > 0.0) || !(b[2] > 0.0)) {
            throw new RuntimeException(
                "[oracle:order-positive-sigma] metamorphic violation: valid positive observations produced non-positive sigma"
                    + " sigma1=" + a[2] + " sigma2=" + b[2] + " n=" + xs.length + " anchor=" + anchor);
        }
    }

    private static void checkClose(String name, double x, double y, double absTol, double relTol,
                                   double[] xs, double[] ys, boolean anchor) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            String oracleId;
            if ("norm".equals(name)) {
                oracleId = "order-norm-finite";
            } else if ("mean".equals(name)) {
                oracleId = "order-mean-finite";
            } else if ("sigma".equals(name)) {
                oracleId = "order-sigma-finite";
            } else {
                oracleId = "order-param-finite";
            }
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: non-finite parameter"
                    + " lhs=" + x + " rhs=" + y + " n=" + xs.length + " anchor=" + anchor);
        }
        double diff = Math.abs(x - y);
        double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
        if (diff > Math.max(absTol, relTol * scale)) {
            String oracleId;
            if ("norm".equals(name)) {
                oracleId = "order-norm-close";
            } else if ("mean".equals(name)) {
                oracleId = "order-mean-close";
            } else if ("sigma".equals(name)) {
                oracleId = "order-sigma-close";
            } else {
                oracleId = "order-param-close";
            }
            throw new RuntimeException(
                "[oracle:" + oracleId + "] metamorphic violation: reversing identical observations changed fitted "
                    + name + " lhs=" + x + " rhs=" + y + " diff=" + diff + " n=" + xs.length + " anchor=" + anchor);
        }
    }

    private static boolean isValidConstructedInput(double[] xs, double[] ys) {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 3) {
            return false;
        }
        double prev = Double.NEGATIVE_INFINITY;
        boolean anyPositive = false;
        for (int i = 0; i < xs.length; i++) {
            if (!Double.isFinite(xs[i]) || !Double.isFinite(ys[i])) {
                return false;
            }
            if (!(ys[i] >= 0.0)) {
                return false;
            }
            if (i > 0 && !(xs[i] > prev)) {
                return false;
            }
            prev = xs[i];
            if (ys[i] > 0.0) {
                anyPositive = true;
            }
        }
        return anyPositive;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
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
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))) {
                return true;
            }
        }
        return false;
    }
}