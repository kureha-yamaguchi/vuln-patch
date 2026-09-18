package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            runExploreAnchorDerived(data);
        } else if (mode == 1) {
            runExploreSyntheticGaussian(data);
        } else {
            runExploreAnchorDerived(data);
            runExploreSyntheticGaussian(data);
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
        }
    }

    private static void runExploreAnchorDerived(FuzzedDataProvider data) {
        int start = data.consumeInt(0, ANCHOR.length - 3);
        int end = data.consumeInt(start + 3, ANCHOR.length);
        int prefix = data.consumeInt(0, 4);
        int suffix = data.consumeInt(0, 4);
        double xStart = data.consumeInt(-1000, 1000);
        double xStep = 1.0 + data.consumeInt(0, 5);
        double scale = pow2(data.consumeInt(-8, 8));
        double floor = pow2(data.consumeInt(-80, -20));

        int n = prefix + (end - start) + suffix;
        double[] xs = new double[n];
        double[] ys = new double[n];

        int k = 0;
        for (int i = 0; i < prefix; i++, k++) {
            xs[k] = xStart + k * xStep;
            ys[k] = floor * (1.0 + i);
        }
        for (int i = start; i < end; i++, k++) {
            xs[k] = xStart + k * xStep;
            ys[k] = clampPositiveFinite(ANCHOR[i] * scale + floor);
        }
        for (int i = 0; i < suffix; i++, k++) {
            xs[k] = xStart + k * xStep;
            ys[k] = clampPositiveFinite(ANCHOR[Math.max(0, end - 1)] * scale + floor * (i + 1));
        }

        runScenario(xs, ys);
    }

    private static void runExploreSyntheticGaussian(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double[] xs = new double[n];
        double[] ys = new double[n];

        double x0 = data.consumeInt(-200, 200);
        double step = 1.0 + data.consumeInt(0, 4);
        double mean = x0 + step * data.consumeInt(0, n - 1);
        double sigma = 0.5 + data.consumeInt(1, 40) / 4.0;
        double amp = 1e-6 + pow2(data.consumeInt(-10, 10));
        double baseline = 1e-18 + pow2(data.consumeInt(-60, -20));
        boolean rightTailOnly = data.consumeBoolean();
        boolean leftTailOnly = data.consumeBoolean();

        int visibleStart = 0;
        int visibleEnd = n;
        if (rightTailOnly) {
            visibleEnd = data.consumeInt(3, n);
            visibleStart = 0;
            mean = x0 + step * (visibleEnd + data.consumeInt(5, 20));
        } else if (leftTailOnly) {
            visibleStart = data.consumeInt(0, n - 3);
            visibleEnd = n;
            mean = x0 - step * (data.consumeInt(5, 20) + (n - visibleStart));
        }

        for (int i = 0; i < n; i++) {
            xs[i] = x0 + i * step;
            double dx = xs[i] - mean;
            double y = amp * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
            if (i < visibleStart || i >= visibleEnd) {
                y = baseline * (1.0 + i);
            }
            ys[i] = clampPositiveFinite(y);
        }

        runScenario(xs, ys);
    }

    private static void runScenario(double[] xs, double[] ys) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            f1.addObservedPoint(xs[i], ys[i]);
            f2.addObservedPoint(xs[i], ys[i]);
        }

        double[] a;
        try {
            a = f1.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] b;
        try {
            b = f2.fit(guess);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                rethrow(t);
            }
            return;
        }

        /* Contract/oracle: GaussianFitter exposes sibling overloads fit() and fit(double[] initialGuess).
           The no-arg overload computes ParameterGuesser(...).guess() and must therefore be equivalent to
           explicitly computing that guess and calling fit(guess) on the same observations. A patch that
           merely suppresses the exception or routes through the wrong implementation can make these
           overloads diverge even when neither throws. */
        if (!sameParams(a, b)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) xs="
                    + summarize(xs) + " ys=" + summarize(ys)
                    + " lhs=" + summarize(a) + " rhs=" + summarize(b));
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) {
                return false;
            }
            double diff = Math.abs(x - y);
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (diff > 1e-6 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static double clampPositiveFinite(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1e-18;
        }
        if (v > 1.0e6) {
            return 1.0e6;
        }
        return v;
    }

    private static double pow2(int e) {
        return Math.scalb(1.0, e);
    }

    private static String summarize(double[] v) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int m = Math.min(v.length, 8);
        for (int i = 0; i < m; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        if (v.length > m) {
            sb.append("...len=").append(v.length);
        }
        sb.append(']');
        return sb.toString();
    }

    private static void rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}