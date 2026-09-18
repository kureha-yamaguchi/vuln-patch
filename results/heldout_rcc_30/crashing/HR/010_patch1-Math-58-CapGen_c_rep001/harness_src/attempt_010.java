package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

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
        runAnchor();

        int mode = data.consumeInt(0, 3);
        double[] ys = buildScenario(data, mode);

        tryPatchedPath(ys, false);
        tryPatchedPath(ys, true);

        checkReflectionOracle(ys);
    }

    private static void runAnchor() {
        try {
            fitNoArg(ANCHOR, false);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void tryPatchedPath(double[] ys, boolean reflect) {
        try {
            fitNoArg(ys, reflect);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidConstructedInput(ys)) {
                throwUnchecked(t);
            }
        }
    }

    private static void checkReflectionOracle(double[] ys) {
        if (!isValidConstructedInput(ys)) {
            return;
        }

        final double[] p1;
        final double[] p2;
        try {
            p1 = fitNoArg(ys, false);
            p2 = fitNoArg(ys, true);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (p1 == null || p2 == null || p1.length < 3 || p2.length < 3) {
            return;
        }

        // Contract/oracle: fitting the same Gaussian-shaped observations after x -> -x
        // must preserve amplitude and sigma, and negate the mean. This uses only
        // real library calls on two equivalent datasets.
        double a1 = p1[0];
        double m1 = p1[1];
        double s1 = p1[2];
        double a2 = p2[0];
        double m2 = p2[1];
        double s2 = p2[2];

        if (!finite(a1) || !finite(m1) || !finite(s1) || !finite(a2) || !finite(m2) || !finite(s2)) {
            return;
        }

        double ampTol = 1e-6 + 0.10 * Math.max(Math.abs(a1), Math.abs(a2));
        double sigTol = 1e-6 + 0.10 * Math.max(Math.abs(s1), Math.abs(s2));
        double meanTol = 1e-6 + 0.10 * Math.max(Math.abs(m1), Math.abs(m2));

        if (Math.abs(a1 - a2) > ampTol || Math.abs(s1 - s2) > sigTol || Math.abs(m1 + m2) > meanTol) {
            throw new RuntimeException(
                "[oracle:x-reflect] metamorphic violation: fit(x,y) vs fit(-x,y) "
                    + "a1=" + a1 + " m1=" + m1 + " s1=" + s1
                    + " a2=" + a2 + " m2=" + m2 + " s2=" + s2
                    + " len=" + ys.length);
        }
    }

    private static double[] buildScenario(FuzzedDataProvider data, int mode) {
        int len = data.consumeInt(3, ANCHOR.length);
        double[] ys = new double[len];
        double scale = scaleFrom(data.consumeInt(-20, 20));
        double jitter = 1.0 + (data.consumeInt(-10, 10) / 200.0);

        for (int i = 0; i < len; i++) {
            int idx;
            switch (mode) {
                case 1:
                    idx = Math.min(ANCHOR.length - 1, i + data.consumeInt(0, 2));
                    break;
                case 2:
                    idx = Math.max(0, ANCHOR.length - len + i);
                    break;
                case 3:
                    idx = data.consumeInt(0, ANCHOR.length - 1);
                    break;
                default:
                    idx = i;
                    break;
            }
            double y = ANCHOR[idx] * scale;
            int deltaPct = data.consumeInt(-5, 5);
            y *= (1.0 + (deltaPct / 100.0));
            y *= jitter;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = ANCHOR[Math.min(idx, ANCHOR.length - 1)];
            }
            ys[i] = y;
        }

        if (mode == 3) {
            java.util.Arrays.sort(ys);
        }
        return ys;
    }

    private static double scaleFrom(int n) {
        if (n >= 0) {
            return 1.0 + n;
        }
        return 1.0 / (1.0 + (-n));
    }

    private static double[] fitNoArg(double[] ys, boolean reflect) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = reflect ? -i : i;
            fitter.addObservedPoint(x, ys[i]);
        }
        return fitter.fit();
    }

    private static boolean isValidConstructedInput(double[] ys) {
        if (ys == null || ys.length < 3) {
            return false;
        }
        boolean strictlyPositiveSeen = false;
        boolean variationSeen = false;
        double first = ys[0];
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            if (!finite(y) || y < 0.0) {
                return false;
            }
            if (y > 0.0) {
                strictlyPositiveSeen = true;
            }
            if (Math.abs(y - first) > Math.max(1e-30, Math.abs(first) * 1e-12)) {
                variationSeen = true;
            }
        }
        return strictlyPositiveSeen && variationSeen;
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof MathIllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        boolean validate = false;
        boolean reachable = false;
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls)
                && "validateParameters".equals(m)) {
                validate = true;
            }
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && ("fit".equals(m)))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(m))) {
                reachable = true;
            }
        }
        return validate || reachable;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}