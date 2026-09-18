package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] anchor = new double[] {
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

        try {
            GaussianFitter anchorCrash = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                anchorCrash.addObservedPoint(i, anchor[i]);
            }
            anchorCrash.fit();
        } catch (RuntimeException t) {
            boolean rootType = t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            if (rootType && throughFit) {
                throw t;
            }
        }

        try {
            GaussianFitter anchorA = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            GaussianFitter anchorB = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                anchorA.addObservedPoint(i, anchor[i]);
                anchorB.addObservedPoint(i, anchor[i]);
            }
            double[] guess = (new GaussianFitter.ParameterGuesser(anchorB.getObservations())).guess();
            double[] viaNoArg = anchorA.fit();
            double[] viaGuess = anchorB.fit(guess);

            /* Contract/oracle: the no-arg fit() computes a guess from the observations and then delegates
               to the overload taking the initial guess. Therefore fit() and fit(guess()) must agree on the
               same observations. A patch that only suppresses the exception or skips the intended overload
               breaks this sibling-agreement relation without necessarily throwing. */
            if (viaNoArg != null && viaGuess != null) {
                if (viaNoArg.length != viaGuess.length) {
                    throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess()) length mismatch input=anchor lhs="
                            + viaNoArg.length + " rhs=" + viaGuess.length);
                }
                for (int i = 0; i < viaNoArg.length; i++) {
                    double a = viaNoArg[i];
                    double b = viaGuess[i];
                    double diff = Math.abs(a - b);
                    double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (!(diff <= 1.0e-8 * scale || (Double.isNaN(a) && Double.isNaN(b)))) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() != fit(guess()) input=anchor index="
                                + i + " lhs=" + a + " rhs=" + b);
                    }
                }
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            boolean rootType = t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }
            if (rootType && throughFit) {
                throw t;
            }
            return;
        }

        int n = data.consumeInt(5, 40);
        int centerIndex = data.consumeInt(0, n - 1);
        double mean = centerIndex + (data.consumeInt(-1000, 1000) / 1000.0);
        double sigma = data.consumeInt(1, 5000) / 1000.0;
        double normExp = data.consumeInt(-300, -5);
        double norm = Math.pow(10.0, normExp);
        double noiseFraction = data.consumeInt(0, 100) / 10000.0;
        boolean reverse = data.consumeBoolean();

        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            int xIndex = reverse ? (n - 1 - i) : i;
            double dx = xIndex - mean;
            double base = norm * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double noise = base * noiseFraction * ((i % 2 == 0) ? 1.0 : 0.5);
            double y = base + noise;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            ys[i] = y;
        }

        try {
            GaussianFitter fuzzA = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            GaussianFitter fuzzB = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                fuzzA.addObservedPoint(i, ys[i]);
                fuzzB.addObservedPoint(i, ys[i]);
            }

            double[] guess = (new GaussianFitter.ParameterGuesser(fuzzB.getObservations())).guess();
            for (int i = 0; i < guess.length; i++) {
                double g = guess[i];
                if (Double.isNaN(g) || Double.isInfinite(g)) {
                    return;
                }
            }

            double[] viaNoArg = fuzzA.fit();
            double[] viaGuess = fuzzB.fit(guess);

            /* Contract/oracle: fit() is documented/implemented as using a first guess and then invoking
               the overload that accepts the initial guess. For the exact same observations and derived guess,
               both overloads must produce the same fitted parameters. */
            if (viaNoArg == null || viaGuess == null) {
                return;
            }
            if (viaNoArg.length != viaGuess.length) {
                throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess()) length mismatch input=n="
                        + n + ",mean=" + mean + ",sigma=" + sigma + ",norm=" + norm + " lhs="
                        + viaNoArg.length + " rhs=" + viaGuess.length);
            }
            for (int i = 0; i < viaNoArg.length; i++) {
                double a = viaNoArg[i];
                double b = viaGuess[i];
                double diff = Math.abs(a - b);
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (!(diff <= 1.0e-7 * scale || (Double.isNaN(a) && Double.isNaN(b)))) {
                    throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() != fit(guess()) input=n="
                            + n + ",mean=" + mean + ",sigma=" + sigma + ",norm=" + norm
                            + " index=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }

            boolean rootType = t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            boolean throughFit = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                        && "fit".equals(st[i].getMethodName())) {
                    throughFit = true;
                    break;
                }
            }

            if (rootType && throughFit) {
                throw t;
            }

            return;
        }
    }
}