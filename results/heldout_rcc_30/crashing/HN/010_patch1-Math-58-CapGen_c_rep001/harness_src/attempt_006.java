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
        runAnchor();

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int len = data.consumeInt(3, ANCHOR_DATA.length - start);
        double scale = positiveScale(data.consumeInt(-6, 6));
        double xOffset = data.consumeInt(-100, 100);
        double xStep = data.consumeInt(1, 5);
        double shift = data.consumeInt(-50, 50);

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double v = ANCHOR_DATA[start + i] * scale;
            if (data.consumeBoolean()) {
                v *= 1.0 + (0.01 * data.consumeInt(0, 10));
            }
            ys[i] = Math.max(v, Double.MIN_NORMAL);
        }

        runExplorationCase(ys, xOffset, xStep, shift, "start=" + start + ",len=" + len + ",scale=" + scale
                + ",xOffset=" + xOffset + ",xStep=" + xStep + ",shift=" + shift);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }

        try {
            double[] p = fitter.fit();
            if (p == null || p.length < 3 || !isFinite(p[1])) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must produce a finite parameter vector input=anchor lhs=" + formatParams(p) + " rhs=finite-length>=3");
            }
            if (Math.abs(p[1] - 53.1572792) > 1.0e-5) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must match the documented fitted mean input=anchor lhs=" + p[1] + " rhs=53.1572792");
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runExplorationCase(double[] ys, double xOffset, double xStep, double shift, String desc) {
        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter shifted = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < ys.length; i++) {
            double x = xOffset + (i * xStep);
            base.addObservedPoint(x, ys[i]);
            shifted.addObservedPoint(x + shift, ys[i]);
        }

        double[] p1;
        try {
            p1 = base.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double[] p2;
        try {
            p2 = shifted.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (p1 == null || p2 == null || p1.length < 3 || p2.length < 3) {
            return;
        }
        if (!allFinite(p1) || !allFinite(p2)) {
            return;
        }

        /*
         * Contract/oracle:
         * GaussianFitter.fit() fits a Gaussian to the observed (x, y) points.
         * Shifting every observed x-value by a constant preserves the shape and
         * amplitudes of the same data, so a correct fit must return the same norm
         * and sigma, with only the mean shifted by that constant. A "fix" that
         * merely suppresses the failing path or silently returns wrong parameters
         * breaks this observable relation even when no exception is thrown.
         */
        double meanDelta = p2[1] - p1[1];
        if (Math.abs(meanDelta - shift) > toleranceFor(p1[1], p2[1], shift, 1.0e-3)) {
            throw new RuntimeException("[oracle:shift] metamorphic violation: shifted x-values should shift fitted mean input=" + desc + " lhs=" + meanDelta + " rhs=" + shift);
        }
        if (Math.abs(p2[0] - p1[0]) > toleranceFor(p1[0], p2[0], 0.0, 1.0e-3)) {
            throw new RuntimeException("[oracle:shift] metamorphic violation: shifted x-values should preserve fitted norm input=" + desc + " lhs=" + p2[0] + " rhs=" + p1[0]);
        }
        if (Math.abs(p2[2] - p1[2]) > toleranceFor(p1[2], p2[2], 0.0, 1.0e-3)) {
            throw new RuntimeException("[oracle:shift] metamorphic violation: shifted x-values should preserve fitted sigma input=" + desc + " lhs=" + p2[2] + " rhs=" + p1[2]);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double positiveScale(int exp) {
        double s = 1.0;
        if (exp > 0) {
            for (int i = 0; i < exp; i++) {
                s *= 10.0;
            }
        } else if (exp < 0) {
            for (int i = 0; i < -exp; i++) {
                s /= 10.0;
            }
        }
        return s;
    }

    private static boolean allFinite(double[] p) {
        for (int i = 0; i < p.length; i++) {
            if (!isFinite(p[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static double toleranceFor(double a, double b, double c, double floor) {
        double scale = Math.max(Math.max(Math.abs(a), Math.abs(b)), Math.abs(c));
        return Math.max(floor, scale * 1.0e-6);
    }

    private static String formatParams(double[] p) {
        if (p == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < p.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(p[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}