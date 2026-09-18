package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() computes a ParameterGuesser guess from the observations and delegates
         * to the sibling overload fit(double[] initialGuess). For the same observations and the exact
         * same guessed parameters, both overloads must produce the same result whenever both calls
         * succeed. A patch that only suppresses the crash or skips the real delegation breaks this.
         */
        runAnchor();
        runFuzz(data);
    }

    private static void runAnchor() {
        final double[] anchorData = new double[] {
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

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterNoArg.addObservedPoint(i, anchorData[i]);
            fitterWithGuess.addObservedPoint(i, anchorData[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResultAnchor(anchorData, guess, lhs, rhs);
    }

    private static void runFuzz(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 40);
        boolean tailShape = data.consumeBoolean();
        double amplitude = 0.1 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 250.0;
        double baseline = data.consumeInt(0, 1000) / 1000000.0;
        double xOffset = data.consumeInt(-1000, 1000) / 10.0;
        double step = 0.25 + data.consumeInt(0, 40) / 4.0;

        double mean;
        if (tailShape) {
            mean = xOffset + (n - 1) * step + (0.5 + data.consumeInt(0, 500) / 10.0);
        } else {
            mean = xOffset + data.consumeInt(0, n - 1) * step;
        }

        GaussianFitter fitterNoArg = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] observedY = new double[n];

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z) + baseline;

            if (data.consumeBoolean()) {
                y += data.consumeInt(0, 1000) / 1000000000.0;
            }

            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + 1.0e-12;
            }

            observedY[i] = y;
            fitterNoArg.addObservedPoint(x, y);
            fitterWithGuess.addObservedPoint(x, y);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterWithGuess.getObservations())).guess();
        } catch (RuntimeException e) {
            return;
        }

        if (guess == null || guess.length != 3) {
            return;
        }
        for (int i = 0; i < guess.length; i++) {
            if (Double.isNaN(guess[i]) || Double.isInfinite(guess[i])) {
                return;
            }
        }

        final double[] lhs;
        try {
            lhs = fitterNoArg.fit();
        } catch (RuntimeException e) {
            if (isRootCauseFromFit(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        final double[] rhs;
        try {
            rhs = fitterWithGuess.fit(guess);
        } catch (RuntimeException e) {
            return;
        }

        assertSameResultFuzz(observedY, guess, lhs, rhs);
    }

    private static boolean isRootCauseFromFit(RuntimeException e) {
        if (!(e instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                    && "fit".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException;
    }

    private static void assertSameResultAnchor(double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-anchor-len] metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException("[oracle:fit-anchor-eq] metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }

    private static void assertSameResultFuzz(double[] input, double[] guess, double[] lhs, double[] rhs) {
        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:fit-fuzz-len] metamorphic violation: fit() and fit(guess) length mismatch input="
                    + Arrays.toString(input)
                    + " guess=" + Arrays.toString(guess)
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            boolean same;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                same = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                same = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                same = Math.abs(a - b) <= 1.0e-7 * scale;
            }
            if (!same) {
                throw new RuntimeException("[oracle:fit-fuzz-eq] metamorphic violation: fit() and fit(guess) disagree input="
                        + Arrays.toString(input)
                        + " guess=" + Arrays.toString(guess)
                        + " lhs=" + Arrays.toString(lhs)
                        + " rhs=" + Arrays.toString(rhs));
            }
        }
    }
}