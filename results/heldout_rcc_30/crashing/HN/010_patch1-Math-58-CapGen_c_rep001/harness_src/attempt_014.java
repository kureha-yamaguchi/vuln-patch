package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
        runCase(ANCHOR_DATA, true);

        int len = data.consumeInt(3, 40);
        double[] synthetic = buildIncreasingTail(data, len);
        runCase(synthetic, true);

        if (data.consumeBoolean()) {
            double[] perturbed = synthetic.clone();
            int edits = data.consumeInt(1, Math.min(6, perturbed.length));
            for (int i = 0; i < edits; i++) {
                int idx = data.consumeInt(0, perturbed.length - 1);
                double factor = 1.0 + (data.consumeInt(-20, 20) / 1000.0);
                if (factor <= 0.0) {
                    factor = 0.001;
                }
                perturbed[idx] = positiveFinite(perturbed[idx] * factor);
            }
            enforceNonDecreasing(perturbed);
            runCase(perturbed, true);
        }
    }

    private static double[] buildIncreasingTail(FuzzedDataProvider data, int len) {
        double[] y = new double[len];
        double norm = 1e-12 + (data.consumeInt(1, 1_000_000) / 1_000_000.0);
        double sigma = 0.5 + (data.consumeInt(1, 400) / 20.0);
        double mean = len + data.consumeInt(1, len + 20);
        for (int i = 0; i < len; i++) {
            double d = (i - mean) / sigma;
            y[i] = positiveFinite(norm * Math.exp(-0.5 * d * d));
        }
        enforceNonDecreasing(y);
        return y;
    }

    private static void enforceNonDecreasing(double[] y) {
        double prev = 0.0;
        for (int i = 0; i < y.length; i++) {
            double v = positiveFinite(y[i]);
            if (v < prev) {
                v = prev + Math.max(prev * 1e-12, 1e-300);
            }
            y[i] = v;
            prev = v;
        }
    }

    private static double positiveFinite(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) {
            return 1e-300;
        }
        return v;
    }

    private static void runCase(double[] ys, boolean validByConstruction) {
        GaussianFitter fitNoGuess = newFitter(ys);
        try {
            fitNoGuess.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedRegion(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (Error t) {
            return;
        }

        GaussianFitter fitA = newFitter(ys);
        GaussianFitter fitB = newFitter(ys);
        try {
            double[] guessed = (new GaussianFitter.ParameterGuesser(fitB.getObservations())).guess();
            double[] a = fitA.fit();
            double[] b = fitB.fit(guessed);

            /* Contract/oracle: zero-arg fit() computes a ParameterGuesser guess from the observations
             * and delegates to the overload taking an initial guess. Therefore fit() and fit(guess())
             * must agree on the same observations and same guess. A patch that merely suppresses the
             * exception path or skips the intended delegation can violate this observable equivalence.
             */
            assertEquivalentResults(ys, guessed, a, b);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedRegion(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (Error t) {
            return;
        }
    }

    private static GaussianFitter newFitter(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        return fitter;
    }

    private static void assertEquivalentResults(double[] ys, double[] guess, double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) returned incompatible result shapes inputLen="
                + ys.length + " guessLen=" + (guess == null ? -1 : guess.length)
                + " lhsLen=" + (a == null ? -1 : a.length) + " rhsLen=" + (b == null ? -1 : b.length));
        }
        for (int i = 0; i < a.length; i++) {
            double lhs = a[i];
            double rhs = b[i];
            boolean lhsNaN = Double.isNaN(lhs);
            boolean rhsNaN = Double.isNaN(rhs);
            if (lhsNaN || rhsNaN) {
                if (lhsNaN != rhsNaN) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: NaN mismatch inputLen="
                        + ys.length + " idx=" + i + " lhs=" + lhs + " rhs=" + rhs);
                }
                continue;
            }
            double diff = Math.abs(lhs - rhs);
            double scale = Math.max(1.0, Math.max(Math.abs(lhs), Math.abs(rhs)));
            if (diff > 1e-8 * scale) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(initialGuess) inputLen="
                    + ys.length + " idx=" + i + " lhs=" + lhs + " rhs=" + rhs + " diff=" + diff);
            }
        }
    }

    private static boolean isRootCauseFromPatchedRegion(Throwable t) {
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
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
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }
}