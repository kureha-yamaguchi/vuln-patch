package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.exception.NotStrictlyPositiveException;

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
        runAnchor();

        int n = data.consumeInt(4, 24);
        double norm = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double mean = data.consumeInt(-200, 200) / 4.0;
        double sigma;
        if (data.consumeBoolean()) {
            sigma = 0.001 + data.consumeInt(0, 2000) / 100000.0;
        } else {
            sigma = 0.05 + data.consumeInt(0, 5000) / 200.0;
        }
        double step = 0.02 + data.consumeInt(0, 2000) / 200.0;
        double start = mean - step * data.consumeInt(0, n);

        boolean reverse = data.consumeBoolean();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double x = start + i * step;
            double dx = x - mean;
            double y = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            y *= 1.0 + (data.consumeInt(-2, 2) * 1.0e-12);
            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            xs[i] = x;
            ys[i] = y;
        }

        try {
            exercisePermutationInvariant(xs, ys, reverse);
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCause(e) && isValidGaussianDataset(xs, ys)) {
                throw e;
            }
        } catch (Error e) {
            if (isRootCause(e) && isValidGaussianDataset(xs, ys)) {
                throw e;
            }
        } catch (Throwable t) {
            if (isRootCause(t) && isValidGaussianDataset(xs, ys)) {
                throw sneaky(t);
            }
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_Y[i]);
        }
        try {
            fitter.fit();
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw sneaky(t);
            }
        }
    }

    private static void exercisePermutationInvariant(double[] xs, double[] ys, boolean reverse) {
        GaussianFitter a = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter b = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < xs.length; i++) {
            a.addObservedPoint(xs[i], ys[i]);
        }

        if (reverse) {
            for (int i = xs.length - 1; i >= 0; i--) {
                b.addObservedPoint(xs[i], ys[i]);
            }
        } else {
            int pivot = xs.length / 2;
            for (int i = pivot; i < xs.length; i++) {
                b.addObservedPoint(xs[i], ys[i]);
            }
            for (int i = 0; i < pivot; i++) {
                b.addObservedPoint(xs[i], ys[i]);
            }
        }

        double[] pa;
        try {
            pa = a.fit();
        } catch (Throwable t) {
            if (isRootCause(t) && isValidGaussianDataset(xs, ys)) {
                throw sneaky(t);
            }
            return;
        }

        double[] pb;
        try {
            pb = b.fit();
        } catch (Throwable t) {
            if (isRootCause(t) && isValidGaussianDataset(xs, ys)) {
                throw sneaky(t);
            }
            return;
        }

        if (pa == null || pb == null || pa.length != 3 || pb.length != 3) {
            return;
        }

        if (!close(pa[0], pb[0], 1.0e-6, 1.0e-4)
                || !close(pa[1], pb[1], 1.0e-6, 1.0e-4)
                || !close(pa[2], pb[2], 1.0e-6, 1.0e-4)) {
            throw new RuntimeException(
                "[oracle:perm-order] metamorphic violation: fitting the same observation multiset "
                + "with a fresh identically-constructed fitter must not depend on insertion order; "
                + "lhs=[" + pa[0] + "," + pa[1] + "," + pa[2] + "] "
                + "rhs=[" + pb[0] + "," + pb[1] + "," + pb[2] + "] "
                + "n=" + xs.length + " reverse=" + reverse);
        }
    }

    private static boolean isValidGaussianDataset(double[] xs, double[] ys) {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 4) {
            return false;
        }
        for (int i = 0; i < xs.length; i++) {
            if (Double.isNaN(xs[i]) || Double.isInfinite(xs[i])) {
                return false;
            }
            if (Double.isNaN(ys[i]) || Double.isInfinite(ys[i]) || ys[i] < 0.0) {
                return false;
            }
            if (i > 0 && !(xs[i] > xs[i - 1])) {
                return false;
            }
        }
        return true;
    }

    private static boolean close(double a, double b, double absTol, double relTol) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= absTol || diff <= relTol * Math.max(1.0, scale);
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String m = trace[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls)
                    && ("fit".equals(m)))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls)
                    && "getObservations".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls)
                    && "guess".equals(m))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls)
                    && "<init>".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException sneaky(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}