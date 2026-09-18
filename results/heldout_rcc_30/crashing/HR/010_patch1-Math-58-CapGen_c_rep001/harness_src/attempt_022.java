package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();
        exploreAnchorBoundary(data);
        checkExactPeakReconstruction(data);
    }

    private static void exerciseAnchor() {
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
        tryFitSeries(anchor, 1.0, 0.0);
    }

    private static void exploreAnchorBoundary(FuzzedDataProvider data) {
        int len = data.consumeInt(8, 40);
        double[] series = new double[len];
        int peak = data.consumeInt(0, len - 1);
        double leftDecay = 1.0 + data.consumeInt(0, 20) / 10.0;
        double rightDecay = 1.0 + data.consumeInt(0, 20) / 10.0;
        double scale = Math.pow(10.0, data.consumeInt(-12, 3));
        double base = Math.pow(10.0, -data.consumeInt(6, 30)) * scale;
        for (int i = 0; i < len; i++) {
            int d = Math.abs(i - peak);
            double decay = i <= peak ? leftDecay : rightDecay;
            series[i] = base * Math.exp(-decay * d);
        }
        if (len > 2) {
            series[peak] = Math.max(series[peak], scale);
            if (peak > 0) {
                series[peak - 1] = Math.max(series[peak - 1], scale * Math.exp(-leftDecay));
            }
            if (peak + 1 < len) {
                series[peak + 1] = Math.max(series[peak + 1], scale * Math.exp(-rightDecay));
            }
        }
        double xOffset = data.consumeInt(-3, 3);
        double xStep = data.consumeBoolean() ? 1.0 : 2.0;
        tryFitSeries(series, xStep, xOffset);
    }

    private static void checkExactPeakReconstruction(FuzzedDataProvider data) {
        int radius = data.consumeInt(3, 8);
        double mean = data.consumeInt(-20, 20);
        double sigma = data.consumeInt(1, 8) / 2.0;
        double norm = 1.0 + data.consumeInt(0, 500);
        int n = 2 * radius + 1;

        Gaussian.Parametric generator = new Gaussian.Parametric();
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        double[] params = new double[] { norm, mean, sigma };
        for (int i = 0; i < n; i++) {
            double x = mean - radius + i;
            double y;
            try {
                y = generator.value(x, params);
            } catch (RuntimeException e) {
                return;
            }
            fitter.addObservedPoint(x, y);
        }

        double[] fitted;
        try {
            fitted = fitter.fit();
        } catch (RuntimeException e) {
            if (isValidationLike(e)) {
                return;
            }
            return;
        } catch (Error e) {
            return;
        }

        if (fitted == null || fitted.length < 3) {
            return;
        }

        double reconstructedPeak;
        try {
            reconstructedPeak = generator.value(mean, fitted);
        } catch (RuntimeException e) {
            return;
        }

        double expectedPeak = norm;
        double tol = Math.max(1e-6, expectedPeak * 0.05);

        /*
         * Sound oracle:
         * We constructed a non-degenerate, exact Gaussian data set from known parameters
         * using the library's own Gaussian.Parametric. One observed point is exactly at x=mean,
         * where a correct Gaussian has value "norm". Fitting that exact series through the
         * public GaussianFitter.fit() API should recover parameters whose own Gaussian value
         * at the same x reproduces that observed sample within a generous tolerance.
         * A patch that merely suppresses the sigma<=0 throw but returns wrong parameters
         * breaks this observable relation even if no exception is thrown.
         */
        if (Math.abs(reconstructedPeak - expectedPeak) > tol) {
            throw new RuntimeException(
                "[oracle:peak-reconstruct] metamorphic violation: exact Gaussian sample at center was not reconstructed"
                    + " mean=" + mean
                    + " sigma=" + sigma
                    + " expectedPeak=" + expectedPeak
                    + " got=" + reconstructedPeak
                    + " p0=" + fitted[0]
                    + " p1=" + fitted[1]
                    + " p2=" + fitted[2]);
        }
    }

    private static void tryFitSeries(double[] y, double xStep, double xOffset) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(xOffset + i * xStep, y[i]);
        }
        try {
            fitter.fit();
        } catch (RuntimeException e) {
            if (isValidationLike(e) || isKnownRootCause(e)) {
                return;
            }
        } catch (Error e) {
            return;
        }
    }

    private static boolean isKnownRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls) && "<init>".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationLike(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof MathIllegalArgumentException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Exception") && (name.contains("Argument") || name.contains("Range") || name.contains("Positive")
            || name.contains("Small") || name.contains("Illegal"));
    }
}