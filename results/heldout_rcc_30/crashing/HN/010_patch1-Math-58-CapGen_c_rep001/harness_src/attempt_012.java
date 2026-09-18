package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            double[] ys = buildTailLikeData(data);
            if (ys != null) {
                runScenario(ys, 0.0, 1.0, true);
            }
        } else if (mode == 1) {
            double[] ys = buildBellData(data);
            if (ys != null) {
                runScenario(ys, 0.0, 1.0, true);
            }
        } else {
            double[] ys = buildPositiveArbitraryData(data);
            if (ys != null) {
                runScenario(ys, data.consumeInt(-50, 50), data.consumeInt(1, 5), true);
            }
        }
    }

    private static void runAnchor() {
        double[] data = new double[] {
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
        runScenario(data, 0.0, 1.0, true);
    }

    private static void runScenario(double[] ys, double xStart, double xStep, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xStart + i * xStep;
            fitter.addObservedPoint(x, ys[i]);
        }

        try {
            fitter.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isGroundTruthRootCauseFromFit(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (Error e) {
            return;
        }

        tryMetamorphicSiblingAgreement(ys, xStart, xStep);
    }

    private static void tryMetamorphicSiblingAgreement(double[] ys, double xStart, double xStep) {
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = xStart + i * xStep;
            f1.addObservedPoint(x, ys[i]);
            f2.addObservedPoint(x, ys[i]);
        }

        double[] guess;
        double[] lhs;
        double[] rhs;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f1.getObservations())).guess();
            lhs = f1.fit();
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() / fit(initialGuess) shape mismatch");
        }

        /* Contract/oracle: the implementation of fit() is documented/shown to compute
         * ParameterGuesser(getObservations()).guess() and delegate to the overload with that
         * initial guess. Therefore, on the same observations, fit() and fit(guess()) must agree.
         * A patch that only suppresses the throw or skips the real overload would break this.
         */
        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: infinite mismatch index=" + i + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            double diff = Math.abs(a - b);
            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
            if (diff > 1.0e-7 * scale) {
                throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(guess()) index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static boolean isGroundTruthRootCauseFromFit(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static double[] buildTailLikeData(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 40);
        double amplitude = positiveRange(data.consumeInt(1, 1000)) * 1.0e-6;
        double mean = data.consumeInt(n + 5, n + 80);
        double sigma = positiveRange(data.consumeInt(1, 50));
        double xStart = data.consumeInt(-20, 20);
        double step = data.consumeInt(1, 3);

        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double x = xStart + i * step;
            double z = (x - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z);
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                return null;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double[] buildBellData(FuzzedDataProvider data) {
        int n = data.consumeInt(7, 41);
        double amplitude = positiveRange(data.consumeInt(1, 1000)) * 1.0e-3;
        double mean = data.consumeInt(2, n - 3);
        double sigma = positiveRange(data.consumeInt(1, 10));
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double z = (i - mean) / sigma;
            double y = amplitude * Math.exp(-0.5 * z * z);
            if (data.consumeBoolean()) {
                y *= 1.0 + (data.consumeInt(-5, 5) * 1.0e-3);
            }
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                return null;
            }
            ys[i] = y;
        }
        return ys;
    }

    private static double[] buildPositiveArbitraryData(FuzzedDataProvider data) {
        int n = data.consumeInt(3, 30);
        double[] ys = new double[n];
        boolean anyPositive = false;
        for (int i = 0; i < n; i++) {
            int mag = data.consumeInt(1, 1000000);
            int exp = data.consumeInt(-12, 3);
            double y = mag * Math.pow(10.0, exp);
            if (data.consumeBoolean()) {
                y *= 1.0e-6;
            }
            if (!(y > 0.0) || Double.isInfinite(y) || Double.isNaN(y)) {
                return null;
            }
            ys[i] = y;
            anyPositive = true;
        }
        return anyPositive ? ys : null;
    }

    private static double positiveRange(int v) {
        return Math.max(1.0, Math.min(1000.0, Math.abs((double) v)));
    }
}