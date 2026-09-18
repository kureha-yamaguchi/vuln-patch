package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchorY = new double[] {
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
        double[] anchorX = new double[anchorY.length];
        for (int i = 0; i < anchorY.length; i++) {
            anchorX[i] = i;
        }
        runScenario(anchorX, anchorY, true, true);

        int n = data.consumeInt(3, 40);
        double[] xs = new double[n];
        double[] ys = new double[n];

        int xStart = data.consumeInt(-50, 50);
        int step = data.consumeInt(1, 4);
        boolean peakRight = data.consumeBoolean();
        double sigma = 0.5 + data.consumeInt(1, 200) / 10.0;
        double norm = Math.pow(10.0, data.consumeInt(-12, 6));
        double margin = data.consumeInt(1, 80);
        double mean = peakRight ? (xStart + (n - 1) * step + margin) : (xStart - margin);

        for (int i = 0; i < n; i++) {
            double x = xStart + i * step;
            xs[i] = x;
            double z = (x - mean) / sigma;
            double base = norm * Math.exp(-0.5 * z * z);
            double noiseScale = 1.0 + (data.consumeInt(-20, 20) / 100.0);
            double y = base * noiseScale;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.max(base, 1e-300);
            }
            ys[i] = y;
        }

        runScenario(xs, ys, true, true);
    }

    private static void runScenario(double[] xs, double[] ys, boolean validByConstruction, boolean checkOracle) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }

        double[] fitNoArg;
        try {
            fitNoArg = fitter.fit();
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            return;
        }

        if (!checkOracle) {
            return;
        }

        GaussianFitter guessFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter overloadFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            guessFitter.addObservedPoint(xs[i], ys[i]);
            overloadFitter.addObservedPoint(xs[i], ys[i]);
        }

        final double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(guessFitter.getObservations())).guess();
        } catch (Throwable t) {
            handleThrowable(t, false);
            return;
        }

        final double[] fitWithGuess;
        try {
            fitWithGuess = overloadFitter.fit(guess);
        } catch (Throwable t) {
            handleThrowable(t, false);
            return;
        }

        /* Contract asserted: fit() and fit(double[] initialGuess) are same-name overloads documented to agree
           on equivalent inputs. Here fit() computes an initial guess from the observations, so comparing it
           against fit(guessedInitialGuess) detects a patch that merely deletes/avoids the failing path but
           returns a different result instead of preserving the overload's behavior. */
        if (!sameVector(fitNoArg, fitWithGuess)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(initialGuess) " +
                "xLen=" + xs.length +
                " lhs=" + vectorToString(fitNoArg) +
                " rhs=" + vectorToString(fitWithGuess)
            );
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && isRootCause((RuntimeException) t) && validByConstruction) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
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
        Package p = t.getClass().getPackage();
        if (p != null) {
            String name = p.getName();
            if (name != null && name.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameVector(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
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

    private static String vectorToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}