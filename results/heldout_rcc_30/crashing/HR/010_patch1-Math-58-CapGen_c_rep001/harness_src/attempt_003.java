package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.MathIllegalStateException;
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
        runAnchor();
        runExplore(data);
        runMetamorphicShift(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCauseFromReachableRegion(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 3);
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        if (mode == 0) {
            int start = data.consumeInt(0, Math.max(0, ANCHOR_DATA.length - 3));
            int len = data.consumeInt(3, ANCHOR_DATA.length - start);
            double scale = pow10(data.consumeInt(-3, 3));
            double shift = data.consumeInt(-5, 5);
            for (int i = 0; i < len; i++) {
                double y = ANCHOR_DATA[start + i] * scale;
                if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = Math.abs(ANCHOR_DATA[start + i]) + 1e-300;
                }
                fitter.addObservedPoint(i + shift, y);
            }
        } else if (mode == 1) {
            int len = data.consumeInt(3, 32);
            double base = Math.pow(10.0, -data.consumeInt(3, 20));
            double growth = 1.0 + (data.consumeInt(0, 500) / 1000.0);
            double x0 = data.consumeInt(-20, 20);
            double step = 1.0 + data.consumeInt(0, 3);
            double y = base;
            for (int i = 0; i < len; i++) {
                y *= growth;
                double yy = y;
                if (!(yy > 0.0) || Double.isNaN(yy) || Double.isInfinite(yy)) {
                    yy = base + (i + 1) * 1e-20;
                }
                fitter.addObservedPoint(x0 + i * step, yy);
            }
        } else if (mode == 2) {
            int len = data.consumeInt(3, ANCHOR_DATA.length);
            int peak = data.consumeInt(0, len - 1);
            double scale = pow10(data.consumeInt(-2, 2));
            double x0 = data.consumeInt(-10, 10);
            for (int i = 0; i < len; i++) {
                int src = Math.abs(i - peak);
                if (src >= ANCHOR_DATA.length) {
                    src = ANCHOR_DATA.length - 1;
                }
                double y = ANCHOR_DATA[src] * scale;
                if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = 1e-300;
                }
                fitter.addObservedPoint(x0 + i, y);
            }
        } else {
            int len = data.consumeInt(3, 24);
            double amp = 1.0 + data.consumeInt(0, 1000);
            double mean = data.consumeInt(-10, 10);
            double sigma = 0.5 + (data.consumeInt(1, 50) / 10.0);
            double x0 = mean - len / 2.0;
            for (int i = 0; i < len; i++) {
                double x = x0 + i;
                double dx = x - mean;
                double y = amp * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
                if (!(y > 0.0)) {
                    y = 1e-300;
                }
                fitter.addObservedPoint(x, y);
            }
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCauseFromReachableRegion(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runMetamorphicShift(FuzzedDataProvider data) {
        int len = data.consumeInt(7, 25);
        double amplitude = 1.0 + data.consumeInt(0, 10000);
        double mean = data.consumeInt(-20, 20);
        double sigma = 1.0 + (data.consumeInt(1, 100) / 20.0);
        double spacing = 0.25 + (data.consumeInt(0, 20) / 20.0);
        double shift = data.consumeInt(-20, 20);

        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter shifted = new GaussianFitter(new LevenbergMarquardtOptimizer());

        double start = mean - spacing * (len - 1) / 2.0;
        for (int i = 0; i < len; i++) {
            double x = start + i * spacing;
            double dx = x - mean;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            base.addObservedPoint(x, y);
            shifted.addObservedPoint(x + shift, y);
        }

        double[] p1;
        double[] p2;
        try {
            p1 = base.fit();
        } catch (Throwable t) {
            if (isRootCauseFromReachableRegion(t)) {
                sneakyThrow(t);
            }
            return;
        }
        try {
            p2 = shifted.fit();
        } catch (Throwable t) {
            if (isRootCauseFromReachableRegion(t)) {
                sneakyThrow(t);
            }
            return;
        }

        if (p1 == null || p2 == null || p1.length < 3 || p2.length < 3) {
            return;
        }
        if (hasBadNumber(p1) || hasBadNumber(p2)) {
            return;
        }

        /* Contract/oracle:
         * Both objects are fitted to the same exact Gaussian samples, with only a constant x-translation.
         * For any correct Gaussian fitter, translating every observation abscissa by c must translate only the
         * fitted mean by c, while amplitude and sigma stay the same. This uses two real library calls and would
         * still fail if a patch merely suppresses the exception but returns silently wrong parameters.
         */
        double ampTol = Math.max(1e-6, Math.abs(p1[0]) * 5e-2);
        double meanTol = Math.max(1e-6, Math.abs(shift) * 5e-2 + spacing);
        double sigmaTol = Math.max(1e-6, Math.abs(p1[2]) * 5e-2);

        if (Math.abs(p1[0] - p2[0]) > ampTol ||
            Math.abs((p1[1] + shift) - p2[1]) > meanTol ||
            Math.abs(p1[2] - p2[2]) > sigmaTol) {
            throw new RuntimeException(
                "[oracle:shift-gaussian] metamorphic violation: translated observations should preserve amplitude/sigma and shift mean " +
                "amp1=" + p1[0] + " mean1=" + p1[1] + " sigma1=" + p1[2] +
                " amp2=" + p2[0] + " mean2=" + p2[1] + " sigma2=" + p2[2] +
                " expectedShift=" + shift + " len=" + len + " spacing=" + spacing);
        }
    }

    private static boolean isRootCauseFromReachableRegion(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!t.getClass().getName().endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(m)) ||
                ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(m)) ||
                ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException ||
               t instanceof NumberFormatException ||
               t instanceof MathIllegalArgumentException ||
               t instanceof MathIllegalStateException;
    }

    private static boolean hasBadNumber(double[] v) {
        for (int i = 0; i < v.length; i++) {
            if (Double.isNaN(v[i]) || Double.isInfinite(v[i])) {
                return true;
            }
        }
        return false;
    }

    private static double pow10(int e) {
        double r = 1.0;
        if (e >= 0) {
            for (int i = 0; i < e; i++) {
                r *= 10.0;
            }
        } else {
            for (int i = 0; i < -e; i++) {
                r /= 10.0;
            }
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}