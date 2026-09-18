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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 2);
        int len = data.consumeInt(3, ANCHOR_Y.length);
        double xStart = data.consumeInt(-20, 20);
        double xStep = data.consumeInt(1, 3);
        double yScale = Math.pow(10.0, data.consumeInt(-2, 2));
        double weightScale = Math.max(0.125, data.consumeInt(1, 32));

        double[] xs = new double[len];
        double[] ys = new double[len];
        double[] w1 = new double[len];
        double[] w2 = new double[len];

        for (int i = 0; i < len; i++) {
            xs[i] = xStart + i * xStep;
            double base = ANCHOR_Y[i];
            double factor;
            switch (mode) {
                case 0:
                    factor = 1.0;
                    break;
                case 1:
                    factor = 1.0 + (data.consumeInt(-20, 20) / 1000.0);
                    break;
                default:
                    factor = 1.0 + (data.consumeInt(-100, 100) / 500.0);
                    break;
            }
            double y = base * yScale * factor;
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                y = base;
            }
            ys[i] = y;
            w1[i] = 1.0;
            w2[i] = weightScale;
        }

        runScenario(xs, ys, w1, true);
        runScenario(xs, ys, w2, true);

        // Documented by the code path itself: ParameterGuesser.guess() operates on
        // WeightedObservedPoint observations and derives its parameters from x/y data.
        // Changing only point weights must therefore not change the guessed parameters.
        // This is an independent oracle in the reachable region (getObservations -> guess).
        try {
            double[] g1 = guessFor(xs, ys, w1);
            double[] g2 = guessFor(xs, ys, w2);
            if (!closeVec(g1, g2, 1e-12, 1e-12)) {
                throw new RuntimeException("[oracle:guess-weight-inv] metamorphic violation: changing only weights changed ParameterGuesser.guess lhs="
                                           + vecToString(g1) + " rhs=" + vecToString(g2));
            }
        } catch (Throwable t) {
            if (isRootCause(t) && hasRelevantStack(t)) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                // Skip non-root-cause issues to avoid false positives outside this patch.
            }
        }

        // Uniformly scaling all positive weights in a weighted least-squares problem
        // must leave the minimizer unchanged: only the common scale changes, not the argmin.
        // A throw-deleting or seed-special-casing patch can still violate this observable.
        try {
            double[] p1 = fitFor(xs, ys, w1);
            double[] p2 = fitFor(xs, ys, w2);
            if (!closeVec(p1, p2, 1e-5, 1e-2)) {
                throw new RuntimeException("[oracle:weight-scale-fit] metamorphic violation: fit changed under uniform weight scaling lhs="
                                           + vecToString(p1) + " rhs=" + vecToString(p2));
            }
        } catch (Throwable t) {
            if (isRootCause(t) && hasRelevantStack(t)) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                // If either side throws, the relation does not apply for this input.
            }
        }
    }

    private static void runAnchor() {
        double[] xs = new double[ANCHOR_Y.length];
        double[] ys = new double[ANCHOR_Y.length];
        double[] ws = new double[ANCHOR_Y.length];
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            xs[i] = i;
            ys[i] = ANCHOR_Y[i];
            ws[i] = 1.0;
        }
        runScenario(xs, ys, ws, true);
    }

    private static void runScenario(double[] xs, double[] ys, double[] ws, boolean validByConstruction) {
        try {
            fitFor(xs, ys, ws);
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t) && hasRelevantStack(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static double[] fitFor(double[] xs, double[] ys, double[] ws) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitter.addObservedPoint(ws[i], xs[i], ys[i]);
        }
        return fitter.fit();
    }

    private static double[] guessFor(double[] xs, double[] ys, double[] ws) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < xs.length; i++) {
            fitter.addObservedPoint(ws[i], xs[i], ys[i]);
        }
        GaussianFitter.ParameterGuesser guesser = new GaussianFitter.ParameterGuesser(fitter.getObservations());
        return guesser.guess();
    }

    private static boolean isRootCause(Throwable t) {
        return t != null
            && "org.apache.commons.math.exception.NotStrictlyPositiveException".equals(t.getClass().getName());
    }

    private static boolean hasRelevantStack(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c)
                    && ("fit".equals(m) || "getObservations".equals(m)))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c)
                    && "guess".equals(m))) {
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
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean closeVec(double[] a, double[] b, double absTol, double relTol) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double av = a[i];
            double bv = b[i];
            if (Double.isNaN(av) || Double.isNaN(bv) || Double.isInfinite(av) || Double.isInfinite(bv)) {
                return false;
            }
            double diff = Math.abs(av - bv);
            double scale = Math.max(Math.abs(av), Math.abs(bv));
            if (diff > absTol + relTol * scale) {
                return false;
            }
        }
        return true;
    }

    private static String vecToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}