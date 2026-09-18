package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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

        tryFitNoArg(buildXs(anchor.length, 0.0, 1.0), anchor, true);

        int n = data.consumeInt(3, 40);
        double offset = data.consumeInt(-200, 200);
        double step = Math.max(0.25, data.consumeInt(1, 20) / 4.0);
        double amplitude = Math.max(1e-6, data.consumeInt(1, 100000) / 1000.0);
        double sigma = Math.max(0.2, data.consumeInt(1, 200) / 10.0);
        double mean = offset + step * data.consumeInt(0, n - 1);
        double baseline = Math.max(0.0, data.consumeInt(0, 1000) / 1000000.0);
        boolean addShoulder = data.consumeBoolean();
        double shoulderAmp = Math.max(0.0, data.consumeInt(0, 5000) / 100000.0);
        double shoulderSigma = Math.max(0.3, data.consumeInt(1, 100) / 10.0);
        double shoulderMean = mean + step * data.consumeInt(-3, 3);

        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double x = offset + i * step;
            xs[i] = x;
            double z = (x - mean) / sigma;
            double y = baseline + amplitude * Math.exp(-0.5 * z * z);
            if (addShoulder) {
                double z2 = (x - shoulderMean) / shoulderSigma;
                y += shoulderAmp * Math.exp(-0.5 * z2 * z2);
            }
            y *= 1.0 + (data.consumeInt(-20, 20) / 1000.0);
            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline;
            }
            ys[i] = y;
        }

        tryFitNoArg(xs, ys, true);

        GaussianFitter fitterForBoth = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            fitterForBoth.addObservedPoint(xs[i], ys[i]);
        }

        double[] lhs;
        try {
            lhs = fitterForBoth.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] rhs;
        try {
            double[] guess = (new GaussianFitter.ParameterGuesser(fitterForBoth.getObservations())).guess();
            GaussianFitter fitterWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < n; i++) {
                fitterWithGuess.addObservedPoint(xs[i], ys[i]);
            }
            rhs = fitterWithGuess.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null || rhs == null || lhs.length != rhs.length) {
            return;
        }

        for (int i = 0; i < lhs.length; i++) {
            double a = lhs[i];
            double b = rhs[i];
            if (Double.isNaN(a) && Double.isNaN(b)) {
                continue;
            }
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                if (a != b) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) disagree on equivalent observations index=" + i + " lhs=" + a + " rhs=" + b);
                }
                continue;
            }
            double diff = Math.abs(a - b);
            double tol = Math.max(1e-6, 1e-6 * Math.max(Math.abs(a), Math.abs(b)));
            if (!(diff <= tol)) {
                throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(initialGuess) disagree on equivalent observations index=" + i + " lhs=" + a + " rhs=" + b);
            }
        }
    }

    private static void tryFitNoArg(double[] xs, double[] ys, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xs[i], ys[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
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

    private static double[] buildXs(int n, double start, double step) {
        double[] xs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = start + i * step;
        }
        return xs;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}