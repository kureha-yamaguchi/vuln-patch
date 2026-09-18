package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 3);
        if (mode == 0) {
            runGeneratedTail(data, true);
        } else if (mode == 1) {
            runGeneratedTail(data, false);
        } else if (mode == 2) {
            runBoundaryGuessPath(data);
        } else {
            runGeneratedBell(data);
        }
    }

    private static void runAnchor() {
        final double[] y = {
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

        double[] x = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            x[i] = i;
        }

        try {
            double[] p = fitByPublicApi(x, y);
            checkFitQuality("[anchor]", x, y, p, 1e-2);
            if (!(p[1] > x[x.length - 1])) {
                throw new RuntimeException("[oracle:anchor-tail-mean] fitted mean should lie to the right of an increasing left-tail sample; mean=" + p[1]);
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromReachableRegion(t)) {
                throw new RuntimeException("[oracle:anchor-valid-input] GaussianFitter.fit() rejected the exact valid regression-test data through the patched call chain", t);
            }
            if (!isCleanRejection(t)) {
                throw t;
            }
        }

        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            addPoints(fitter, x, y);
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] p2 = fitter.fit(guess);
            checkFitQuality("[anchor-fitguess]", x, y, p2, 1e-2);
        } catch (RuntimeException t) {
            if (isRootCauseFromReachableRegion(t)) {
                throw new RuntimeException("[oracle:anchor-fitdouble] GaussianFitter.fit(double[]) should mask negative-sigma trials for valid anchor data", t);
            }
            if (!isCleanRejection(t)) {
                throw t;
            }
        }
    }

    private static void runGeneratedTail(FuzzedDataProvider data, boolean padded) {
        int n = data.consumeInt(12, 36);
        int step = data.consumeInt(1, 3);
        int start = data.consumeInt(-20, 20);
        double amplitude = 1.0 + data.consumeInt(0, 2000);
        double sigma = 0.75 + (data.consumeInt(0, 1200) / 100.0);
        double edgeGap = 0.2 + (data.consumeInt(0, 600) / 100.0);

        double[] x = new double[n];
        double[] y = new double[n];
        double lastX = start + (n - 1) * step;
        double mean = lastX + edgeGap * step;

        for (int i = 0; i < n; i++) {
            x[i] = start + i * step;
            y[i] = gaussian(amplitude, mean, sigma, x[i]);
        }

        if (padded) {
            int leftPad = data.consumeInt(0, 2);
            int rightPad = data.consumeInt(0, 2);
            double[] px = new double[n + leftPad + rightPad];
            double[] py = new double[px.length];
            int k = 0;
            for (int i = leftPad; i > 0; i--) {
                px[k] = start - i * step;
                py[k] = gaussian(amplitude, mean, sigma, px[k]);
                k++;
            }
            for (int i = 0; i < n; i++, k++) {
                px[k] = x[i];
                py[k] = y[i];
            }
            for (int i = 1; i <= rightPad; i++, k++) {
                px[k] = lastX + i * step;
                py[k] = gaussian(amplitude, mean, sigma, px[k]);
            }
            x = px;
            y = py;
        }

        executeValidScenario("[tail]", x, y, amplitude, mean, sigma, step);
    }

    private static void runBoundaryGuessPath(FuzzedDataProvider data) {
        int n = data.consumeInt(10, 24);
        int step = data.consumeInt(1, 2);
        int start = data.consumeInt(-10, 10);
        double amplitude = 10.0 + data.consumeInt(0, 500);
        double sigma = 0.9 + (data.consumeInt(0, 400) / 100.0);

        double[] multipliers = {0.01, 0.10, 0.50, 0.99, 1.01, 1.50, 2.00};
        double factor = multipliers[data.consumeInt(0, multipliers.length - 1)];

        double[] x = new double[n];
        double[] y = new double[n];
        double lastX = start + (n - 1) * step;
        double mean = lastX + factor * step;

        for (int i = 0; i < n; i++) {
            x[i] = start + i * step;
            y[i] = gaussian(amplitude, mean, sigma, x[i]);
        }

        executeValidScenario("[boundary]", x, y, amplitude, mean, sigma, step);
    }

    private static void runGeneratedBell(FuzzedDataProvider data) {
        int n = data.consumeInt(15, 41);
        int step = data.consumeInt(1, 3);
        int start = data.consumeInt(-20, 20);
        double amplitude = 5.0 + data.consumeInt(0, 2000);
        double sigma = 1.0 + (data.consumeInt(0, 1000) / 100.0);
        double mean = start + (n / 2.0) * step + data.consumeInt(-2, 2) * 0.25 * step;

        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = start + i * step;
            y[i] = gaussian(amplitude, mean, sigma, x[i]);
        }

        executeValidScenario("[bell]", x, y, amplitude, mean, sigma, step);
    }

    private static void executeValidScenario(String tag, double[] x, double[] y, double amplitude, double mean, double sigma, int step) {
        try {
            double[] p = fitByPublicApi(x, y);

            /*
             * Contract/oracle: these samples are generated from an exact Gaussian with positive sigma.
             * A correct GaussianFitter.fit() is supposed to fit such valid data rather than reject it.
             * Even if a patch merely suppresses the known exception, it must still return parameters that
             * reproduce the supplied curve. We therefore compare the returned model against the input-built
             * Gaussian data itself, which is an independent observable from the crash symptom.
             */
            checkFitQuality(tag, x, y, p, 5e-3);

            double meanTol = Math.max(2.5 * step, 0.20 * Math.abs(mean) + 0.5);
            if (Math.abs(p[1] - mean) > meanTol) {
                throw new RuntimeException("[oracle:known-mean] metamorphic violation: exact Gaussian input should recover its generating mean within tolerance inputMean=" + mean + " fittedMean=" + p[1] + " tol=" + meanTol + " tag=" + tag);
            }

            if (!(p[2] > 0.0)) {
                throw new RuntimeException("[oracle:positive-sigma] metamorphic violation: fitted sigma must stay positive for exact Gaussian data fittedSigma=" + p[2] + " tag=" + tag);
            }

            double sigmaTol = Math.max(1.0 * step, 0.35 * sigma);
            if (Math.abs(p[2] - sigma) > sigmaTol) {
                throw new RuntimeException("[oracle:known-sigma] metamorphic violation: exact Gaussian input should recover its generating sigma within tolerance inputSigma=" + sigma + " fittedSigma=" + p[2] + " tol=" + sigmaTol + " tag=" + tag);
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromReachableRegion(t)) {
                throw new RuntimeException("[oracle:valid-gaussian-rejected] valid exact Gaussian data reached the patched call chain but was rejected", t);
            }
            if (!isCleanRejection(t)) {
                throw t;
            }
        }

        try {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            addPoints(fitter, x, y);
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            double[] p2 = fitter.fit(guess);
            checkFitQuality(tag + "-fitguess", x, y, p2, 5e-3);

            double maxX = x[0];
            for (int i = 1; i < x.length; i++) {
                if (x[i] > maxX) {
                    maxX = x[i];
                }
            }
            if (isMonotoneIncreasing(y) && !(p2[1] >= maxX - step)) {
                throw new RuntimeException("[oracle:tail-side] metamorphic violation: for an increasing left-tail sample, fitted mean should not move left of the observed support maxX=" + maxX + " fittedMean=" + p2[1] + " tag=" + tag);
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromReachableRegion(t)) {
                throw new RuntimeException("[oracle:fitdouble-valid-gaussian] GaussianFitter.fit(double[]) should handle optimizer negative-sigma probes for valid data", t);
            }
            if (!isCleanRejection(t)) {
                throw t;
            }
        }
    }

    private static double[] fitByPublicApi(double[] x, double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        addPoints(fitter, x, y);
        return fitter.fit();
    }

    private static void addPoints(GaussianFitter fitter, double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) {
            fitter.addObservedPoint(x[i], y[i]);
        }
    }

    private static void checkFitQuality(String tag, double[] x, double[] y, double[] p, double relRmseTol) {
        Gaussian.Parametric g = new Gaussian.Parametric();
        double sumSq = 0.0;
        double sumY2 = 0.0;
        double maxY = 0.0;
        for (int i = 0; i < x.length; i++) {
            double predicted;
            try {
                predicted = g.value(x[i], p);
            } catch (RuntimeException e) {
                throw new RuntimeException("[oracle:model-eval] metamorphic violation: fitted parameters should define a usable Gaussian model tag=" + tag, e);
            }
            double d = predicted - y[i];
            sumSq += d * d;
            sumY2 += y[i] * y[i];
            if (y[i] > maxY) {
                maxY = y[i];
            }
        }

        double scale = Math.max(1e-300, Math.max(maxY * maxY, sumY2 / Math.max(1, y.length)));
        double relRmse = Math.sqrt(sumSq / Math.max(1, y.length)) / Math.sqrt(scale);
        if (!(relRmse <= relRmseTol)) {
            throw new RuntimeException("[oracle:fit-rmse] metamorphic violation: exact Gaussian samples should be reproduced by fitted parameters tag=" + tag + " relRmse=" + relRmse + " tol=" + relRmseTol);
        }
    }

    private static boolean isMonotoneIncreasing(double[] y) {
        for (int i = 1; i < y.length; i++) {
            if (y[i] < y[i - 1]) {
                return false;
            }
        }
        return true;
    }

    private static double gaussian(double norm, double mean, double sigma, double x) {
        double z = (x - mean) / sigma;
        return norm * Math.exp(-0.5 * z * z);
    }

    private static boolean isRootCauseFromReachableRegion(Throwable t) {
        if (t == null) {
            return false;
        }
        String name = t.getClass().getName();
        boolean rootFamily =
                name.endsWith("NotStrictlyPositiveException") ||
                name.endsWith("ZeroException") ||
                name.endsWith("OutOfRangeException") ||
                name.endsWith("NumberIsTooSmallException");
        if (!rootFamily && !(t instanceof RuntimeException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(m)) ||
                ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(m)) ||
                (cls.contains("GaussianFitter$ParameterGuesser") && "guess".equals(m)) ||
                ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls) && "validateParameters".equals(m))) {
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
        if (n.startsWith("org.apache.commons.math.exception.")) {
            return true;
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isCleanRejection(cause);
    }
}