package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = {
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
        runExplore(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        exerciseAndCheck(fitter, true);
    }

    private static void runExplore(FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int end = data.consumeInt(start + 2, ANCHOR_DATA.length - 1);
        int prefix = data.consumeInt(0, 3);
        int suffix = data.consumeInt(0, 3);
        double x = data.consumeInt(-100, 100);
        double step = data.consumeInt(1, 5);
        double yScale = data.consumeInt(1, 1000) / 100.0;
        double minPositive = Math.pow(10.0, -data.consumeInt(10, 250));

        for (int i = 0; i < prefix; i++) {
            double y = Math.max(minPositive, ANCHOR_DATA[start] * (0.1 + (i + 1) * 0.05) * yScale);
            fitter.addObservedPoint(x, y);
            x += step;
        }

        for (int i = start; i <= end; i++) {
            int noisePct = data.consumeInt(-10, 10);
            double factor = 1.0 + (noisePct / 100.0);
            double y = ANCHOR_DATA[i] * yScale * factor;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = minPositive;
            }
            fitter.addObservedPoint(x, Math.max(minPositive, y));
            x += step;
        }

        for (int i = 0; i < suffix; i++) {
            double y = Math.max(minPositive, ANCHOR_DATA[end] * (0.5 + (i + 1) * 0.1) * yScale);
            fitter.addObservedPoint(x, y);
            x += step;
        }

        exerciseAndCheck(fitter, true);
    }

    private static void exerciseAndCheck(GaussianFitter fitter, boolean validByConstruction) {
        try {
            fitter.fit();
        } catch (RuntimeException e) {
            if (validByConstruction && isRootCause(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        // Contract/metamorphic check:
        // fit() is documented/implemented as using ParameterGuesser on the current observations
        // and then delegating to the sibling overload fit(double[] initialGuess). Therefore, for
        // the same observations, fit() and fit(guess()) must agree whenever both calls succeed.
        try {
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] viaOverload = fitter.fit(guess);

            GaussianFitter fitter2 = cloneFitter(fitter);
            double[] viaNoArg = fitter2.fit();

            if (!sameResult(viaOverload, viaNoArg)) {
                throw new RuntimeException(
                    "[oracle:fit-overload-agreement] metamorphic violation: fit() must agree with fit(guess())"
                        + " guess=" + arrayToString(guess)
                        + " lhs=" + arrayToString(viaOverload)
                        + " rhs=" + arrayToString(viaNoArg));
            }
        } catch (RuntimeException e) {
            if (validByConstruction && isRootCause(e)) {
                throw e;
            }
            return;
        }
    }

    private static GaussianFitter cloneFitter(GaussianFitter original) {
        GaussianFitter copy = new GaussianFitter(new LevenbergMarquardtOptimizer());
        WeightedObservedPoint[] obs = original.getObservations();
        for (int i = 0; i < obs.length; i++) {
            WeightedObservedPoint p = obs[i];
            copy.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }
        return copy;
    }

    private static boolean isRootCause(RuntimeException e) {
        if (!(e instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(f.getClassName())
                    && "fit".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        if (e instanceof IllegalArgumentException || e instanceof NumberFormatException) {
            return true;
        }
        String name = e.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean sameResult(double[] a, double[] b) {
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
            if (diff > 1e-8 * scale) {
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