package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
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

        int mode = data.consumeInt(0, 3);
        if (mode == 0) {
            double[] ys = buildMonotonePositiveSeries(data);
            runExplore(ys, data.consumeInt(-50, 50));
        } else if (mode == 1) {
            double[] ys = mutateAnchor(data);
            runExplore(ys, data.consumeInt(-50, 50));
        } else if (mode == 2) {
            double[] ys = buildMonotonePositiveSeries(data);
            reverseIfRequested(ys, data.consumeBoolean());
            runExplore(ys, data.consumeInt(-50, 50));
        } else {
            double[] ys = buildPlateauThenRise(data);
            runExplore(ys, data.consumeInt(-50, 50));
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        // Contract/metamorphic check:
        // no-arg fit() computes a guess from the current observations and delegates to the
        // overload fit(double[] initialGuess). Therefore, for the same observations and the
        // same real guess produced by ParameterGuesser, both overloads must agree.
        // A "fix" that merely deletes the throwing path or changes behavior only in fit()
        // would violate this sibling-agreement relation.
        assertSiblingAgreement(ANCHOR_DATA, 0.0);
    }

    private static void runExplore(double[] ys, double xOffset) {
        if (ys == null || ys.length < 3) {
            return;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xOffset + i, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        assertSiblingAgreement(ys, xOffset);
    }

    private static void assertSiblingAgreement(double[] ys, double xOffset) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xOffset + i;
            f1.addObservedPoint(x, ys[i]);
            f2.addObservedPoint(x, ys[i]);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] a;
        double[] b;
        try {
            a = f1.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }
        try {
            b = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (!sameParams(a, b)) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() must agree with fit(guess()) inputLen=" + ys.length + " lhs=" + format(a) + " rhs=" + format(b));
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1.0e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
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
            String pn = p.getName();
            if (pn.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
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

    private static double[] buildMonotonePositiveSeries(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        double exp = -data.consumeInt(5, 35);
        double current = Math.pow(10.0, exp);
        double ratioBase = 1.05 + (data.consumeInt(0, 500) / 100.0);
        for (int i = 0; i < n; i++) {
            double localRatio = ratioBase + (data.consumeInt(0, 20) / 100.0);
            if (localRatio <= 1.0) {
                localRatio = 1.01;
            }
            current *= localRatio;
            if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current) || current > 1.0e100) {
                current = ys[Math.max(0, i - 1)] * 1.5;
                if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current)) {
                    current = 1.0;
                }
            }
            ys[i] = current;
        }
        return ys;
    }

    private static double[] mutateAnchor(FuzzedDataProvider data) {
        int prefixDrop = data.consumeInt(0, Math.min(8, ANCHOR_DATA.length - 3));
        int suffixDrop = data.consumeInt(0, Math.min(8, ANCHOR_DATA.length - prefixDrop - 3));
        int n = ANCHOR_DATA.length - prefixDrop - suffixDrop;
        if (n < 3) {
            n = 3;
        }
        double[] ys = new double[n];
        double scale = Math.pow(10.0, data.consumeInt(-3, 3));
        if (!(scale > 0.0) || Double.isInfinite(scale) || Double.isNaN(scale)) {
            scale = 1.0;
        }
        for (int i = 0; i < n; i++) {
            double v = ANCHOR_DATA[prefixDrop + i] * scale;
            double tweak = 1.0 + (data.consumeInt(-5, 5) / 1000.0);
            v *= tweak;
            if (i > 0 && v <= ys[i - 1]) {
                v = ys[i - 1] * (1.001 + (data.consumeInt(0, 50) / 1000.0));
            }
            if (!(v > 0.0) || Double.isInfinite(v) || Double.isNaN(v)) {
                v = (i == 0) ? 1e-30 : ys[i - 1] * 1.01;
            }
            ys[i] = v;
        }
        return ys;
    }

    private static double[] buildPlateauThenRise(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        int plateau = data.consumeInt(0, n - 1);
        double[] ys = new double[n];
        double base = Math.pow(10.0, -data.consumeInt(5, 30));
        if (!(base > 0.0) || Double.isInfinite(base) || Double.isNaN(base)) {
            base = 1e-10;
        }
        double current = base;
        for (int i = 0; i < n; i++) {
            if (i < plateau) {
                ys[i] = current;
            } else {
                current *= 1.2 + (data.consumeInt(0, 400) / 100.0);
                if (!(current > 0.0) || Double.isInfinite(current) || Double.isNaN(current) || current > 1.0e100) {
                    current = ys[Math.max(0, i - 1)] * 1.5;
                    if (!(current > 0.0)) {
                        current = 1.0;
                    }
                }
                ys[i] = current;
            }
        }
        for (int i = 1; i < ys.length; i++) {
            if (ys[i] < ys[i - 1]) {
                ys[i] = ys[i - 1];
            }
            if (!(ys[i] > 0.0)) {
                ys[i] = ys[i - 1] + 1e-12;
            }
        }
        return ys;
    }

    private static void reverseIfRequested(double[] ys, boolean reverse) {
        if (!reverse) {
            return;
        }
        for (int i = 0, j = ys.length - 1; i < j; i++, j--) {
            double tmp = ys[i];
            ys[i] = ys[j];
            ys[j] = tmp;
        }
        for (int i = 1; i < ys.length; i++) {
            if (ys[i] <= ys[i - 1]) {
                ys[i] = ys[i - 1] * 1.01;
            }
        }
    }
}