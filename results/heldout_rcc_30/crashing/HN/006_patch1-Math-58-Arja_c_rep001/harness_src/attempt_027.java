package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchor = new double[] {
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
        runOneCase(anchor, true);

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];

        double norm = (double) Integer.MAX_VALUE;
        double amplitude = 0.1 + 50.0 * (Math.abs((double) data.consumeInt()) / norm);
        double sigma = 0.2 + 20.0 * (Math.abs((double) data.consumeInt()) / norm);
        boolean tailShape = data.consumeBoolean();
        double center;
        if (tailShape) {
            center = n + data.consumeInt(1, 20);
        } else {
            center = data.consumeInt(0, n - 1);
        }

        Gaussian.Parametric gp = new Gaussian.Parametric();
        for (int i = 0; i < n; i++) {
            double base;
            try {
                base = gp.value((double) i, new double[] { amplitude, center, sigma });
            } catch (RuntimeException e) {
                return;
            }

            int tweakRaw = data.consumeInt(-1000, 1000);
            double tweak = 1.0 + (tweakRaw / 5000.0);
            if (tweak <= 0.05) {
                tweak = 0.05;
            }

            double baseline = data.consumeInt(0, 1000) / 1_000_000.0;
            double y = base * tweak + baseline;

            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                y = baseline + 1.0e-12;
            }
            ys[i] = y;
        }

        runOneCase(ys, true);

        int prefix = data.consumeInt(0, 5);
        int suffix = data.consumeInt(0, 5);
        double[] padded = new double[prefix + n + suffix];
        for (int i = 0; i < prefix; i++) {
            padded[i] = data.consumeInt(0, 1000) / 1_000_000.0 + 1.0e-12;
        }
        System.arraycopy(ys, 0, padded, prefix, n);
        for (int i = 0; i < suffix; i++) {
            padded[prefix + n + i] = data.consumeInt(0, 1000) / 1_000_000.0 + 1.0e-12;
        }
        runOneCase(padded, true);
    }

    private static void runOneCase(double[] yValues, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < yValues.length; i++) {
            double y = yValues[i];
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            fitter.addObservedPoint((double) i, y);
        }

        double[] guessed;
        try {
            guessed = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (RuntimeException e) {
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] viaExplicitGuess;
        try {
            viaExplicitGuess = fitter.fit(guessed);
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() computes ParameterGuesser(...).guess() and then delegates to fit(initialGuess).
         * Therefore, for the same observations and the exact same guessed parameters, fit() and fit(guess)
         * must produce the same result on every correct implementation. A throw-deleting or branch-skipping
         * patch can make fit() silently diverge from fit(initialGuess), so we check sibling-agreement here.
         */
        if (viaNoArg.length != viaExplicitGuess.length) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) returned different lengths inputLen="
                    + yValues.length + " lhsLen=" + viaNoArg.length + " rhsLen=" + viaExplicitGuess.length);
        }

        for (int i = 0; i < viaNoArg.length; i++) {
            double a = viaNoArg[i];
            double b = viaExplicitGuess[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b) || Double.isNaN(a) || Double.isNaN(b)) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) disagreed on finiteness inputLen="
                        + yValues.length + " index=" + i + " lhs=" + a + " rhs=" + b);
            }
            double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (Math.abs(a - b) > tol) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) differ inputLen="
                        + yValues.length + " index=" + i + " lhs=" + a + " rhs=" + b + " tol=" + tol);
            }
        }
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}