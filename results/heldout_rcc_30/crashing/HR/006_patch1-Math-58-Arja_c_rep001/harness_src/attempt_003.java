package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        exerciseAnchor();

        int start = data.consumeInt(0, ANCHOR_Y.length - 3);
        int end = data.consumeInt(start + 2, ANCHOR_Y.length - 1);
        int len = end - start + 1;

        double[] xs = new double[len];
        double[] ys = new double[len];

        int step = data.consumeInt(1, 3);
        int offset = data.consumeInt(-20, 20);
        double scale = 0.5d + (data.consumeInt(0, 300) / 100.0d);

        for (int i = 0; i < len; i++) {
            xs[i] = offset + (double) (i * step);
            double noisePct = data.consumeInt(-10, 10) / 100.0d;
            double y = ANCHOR_Y[start + i] * scale * (1.0d + noisePct);
            if (!(y > 0.0d) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = ANCHOR_Y[start + i];
            }
            ys[i] = y;
        }

        exercise(xs, ys);

        if (data.consumeBoolean()) {
            int extraLeft = data.consumeInt(0, 3);
            int extraRight = data.consumeInt(0, 3);
            double[] xs2 = new double[len + extraLeft + extraRight];
            double[] ys2 = new double[len + extraLeft + extraRight];

            int pos = 0;
            for (int i = extraLeft; i > 0; i--) {
                xs2[pos] = offset - i * step;
                ys2[pos] = ANCHOR_Y[start] * 0.1d;
                pos++;
            }
            for (int i = 0; i < len; i++) {
                xs2[pos] = xs[i];
                ys2[pos] = ys[i];
                pos++;
            }
            for (int i = 0; i < extraRight; i++) {
                xs2[pos] = offset + (len + i) * step;
                ys2[pos] = ys[len - 1] * (1.2d + 0.1d * i);
                if (!(ys2[pos] > 0.0d)) {
                    ys2[pos] = ys[len - 1];
                }
                pos++;
            }

            exercise(xs2, ys2);
        }
    }

    private static void exerciseAnchor() {
        double[] xs = new double[ANCHOR_Y.length];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = i;
        }
        exercise(xs, ANCHOR_Y);
    }

    private static void exercise(double[] xs, double[] ys) {
        if (xs == null || ys == null || xs.length != ys.length || xs.length < 3) {
            return;
        }

        GaussianFitter probeFitter = newFitter(xs, ys);
        try {
            WeightedObservedPoint[] a = probeFitter.getObservations();
            if (a.length > 0) {
                WeightedObservedPoint saved = a[0];
                a[0] = null;
                WeightedObservedPoint[] b = probeFitter.getObservations();
                if (b.length == 0 || b[0] == null || b[0] != saved) {
                    throw new RuntimeException("[oracle:getObservations-copy] metamorphic violation: getObservations must return a fresh array copy");
                }
            }
        } catch (Throwable t) {
            if (isOracle(t)) {
                throw rethrow(t);
            }
            return;
        }

        double[] guessed;
        try {
            GaussianFitter.ParameterGuesser guesser =
                new GaussianFitter.ParameterGuesser(probeFitter.getObservations());
            double[] g1 = guesser.guess();
            double[] g2 = guesser.guess();
            if (g1 == g2) {
                throw new RuntimeException("[oracle:guess-clone] metamorphic violation: repeated guess() calls returned same array reference");
            }
            if (g1.length != g2.length) {
                throw new RuntimeException("[oracle:guess-clone] metamorphic violation: repeated guess() calls changed length");
            }
            if (g1.length > 0) {
                double old = g2[0];
                g1[0] = old + 123.456d;
                double[] g3 = guesser.guess();
                if (g3.length == 0 || g3[0] != old) {
                    throw new RuntimeException("[oracle:guess-clone] metamorphic violation: mutating returned guess array affected later guess()");
                }
            }
            guessed = g2;
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw rethrow(t);
            }
            return;
        }

        GaussianFitter lhsFitter = newFitter(xs, ys);
        GaussianFitter rhsFitter = newFitter(xs, ys);

        double[] lhs = null;
        double[] rhs = null;
        Throwable lhsThrown = null;
        Throwable rhsThrown = null;

        try {
            lhs = lhsFitter.fit();
        } catch (Throwable t) {
            lhsThrown = t;
        }

        try {
            rhs = rhsFitter.fit(guessed);
        } catch (Throwable t) {
            rhsThrown = t;
        }

        if (rhsThrown != null) {
            if (isCleanRejection(rhsThrown)) {
                return;
            }
            if (isOracle(rhsThrown)) {
                throw rethrow(rhsThrown);
            }
            return;
        }

        if (lhsThrown != null) {
            if (isRootCause(lhsThrown)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() threw while fit(guess()) succeeded; throwable="
                        + lhsThrown.getClass().getName());
            }
            if (isCleanRejection(lhsThrown)) {
                return;
            }
            if (isOracle(lhsThrown)) {
                throw rethrow(lhsThrown);
            }
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-overload-agreement] metamorphic violation: result shape mismatch");
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            double tol = 1.0e-8 * Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
            if (Double.isNaN(a) != Double.isNaN(b) || Math.abs(a - b) > tol) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(guess()) disagree at index "
                        + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static GaussianFitter newFitter(double[] xs, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
        return fitter;
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

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!"org.apache.commons.math.exception.NotStrictlyPositiveException".equals(t.getClass().getName())) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(c))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}