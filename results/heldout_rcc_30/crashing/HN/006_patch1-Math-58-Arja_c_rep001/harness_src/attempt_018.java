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
        exerciseAnchor();
        int cases = data.consumeInt(1, 3);
        for (int i = 0; i < cases; i++) {
            if (data.remainingBytes() <= 0) {
                break;
            }
            exerciseFuzzCase(data);
        }
    }

    private static void exerciseAnchor() {
        double[] xs = new double[ANCHOR.length];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = i;
        }
        exerciseOne(xs, ANCHOR, true);
    }

    private static void exerciseFuzzCase(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 40);
        double[] xs = new double[len];
        double[] ys = new double[len];

        int xStart = data.consumeInt(-50, 50);
        int step = data.consumeInt(1, 5);
        int centerOffset = data.consumeInt(1, Math.max(2, len * 3));
        double center = xStart + (len - 1 + centerOffset) * (double) step;
        double sigma = 0.5 + (data.consumeInt(1, Math.max(2, len * 4)) / 4.0) * step;
        double amplitude = 0.1 + (data.consumeInt(1, 1000) / 10.0);
        double baseline = 1e-12 * (1 + data.consumeInt(0, 100));

        for (int i = 0; i < len; i++) {
            double x = xStart + i * (double) step;
            xs[i] = x;
            double dx = x - center;
            double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noisePct = data.consumeInt(0, 20) / 100.0;
            if (data.consumeBoolean()) {
                y *= (1.0 + noisePct);
            } else {
                y *= (1.0 - noisePct * 0.5);
            }
            y += baseline;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            ys[i] = y;
        }

        exerciseOne(xs, ys, true);
    }

    private static void exerciseOne(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitterForGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitterForGuess.addObservedPoint(xs[i], ys[i]);
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterForGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] viaNoArg;
        try {
            viaNoArg = fitterForGuess.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedPath(t)) {
                throw t;
            }
            return;
        }

        GaussianFitter fitterForExplicitGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitterForExplicitGuess.addObservedPoint(xs[i], ys[i]);
        }

        final double[] viaExplicitGuess;
        try {
            viaExplicitGuess = fitterForExplicitGuess.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        /*
         * Contract/oracle: GaussianFitter has sibling overloads fit() and fit(double[] initialGuess).
         * The patched implementation of fit() computes ParameterGuesser(...).guess() and delegates to fit(guess).
         * Therefore, for the same observations, fit() must agree with fit(guess()).
         * A throw-deleting or branch-skipping patch can make fit() return a different result while no exception fires.
         */
        if (!sameArray(viaNoArg, viaExplicitGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() must equal fit(ParameterGuesser.guess())"
                    + " inputLen=" + ys.length
                    + " lhs=" + arrayToString(viaNoArg)
                    + " rhs=" + arrayToString(viaExplicitGuess));
        }
    }

    private static boolean isRootCauseFromPatchedPath(Throwable t) {
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

    private static boolean sameArray(double[] a, double[] b) {
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
            if (diff > 1e-9 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}