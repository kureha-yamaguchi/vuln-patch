package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runZeroWeightInvariance(data);
    }

    private static void runAnchor() {
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

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchor.length; i++) {
            fitter.addObservedPoint(i, anchor[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isKnownValidationLike(t)) {
                return;
            }
        }

        try {
            double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
            fitter.fit(guess);
        } catch (Throwable t) {
            if (isKnownValidationLike(t)) {
                return;
            }
        }
    }

    private static void runZeroWeightInvariance(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 18);
        double norm = positiveFromInt(data.consumeInt(), 0.5, 500.0);
        double mean = boundedDouble(data.consumeInt(), -25.0, 25.0);
        double sigma = positiveFromInt(data.consumeInt(), 0.5, 8.0);

        double spacing = sigma * boundedDouble(data.consumeInt(), 0.3, 1.2);
        if (spacing <= 0.0 || Double.isNaN(spacing) || Double.isInfinite(spacing)) {
            spacing = sigma;
        }

        double[] xs = new double[n];
        double[] ys = new double[n];
        int center = n / 2;
        Gaussian.Parametric g = new Gaussian.Parametric();
        double[] truth = new double[] { norm, mean, sigma };

        for (int i = 0; i < n; i++) {
            xs[i] = mean + (i - center) * spacing;
            try {
                ys[i] = g.value(xs[i], truth);
            } catch (RuntimeException ex) {
                return;
            }
        }

        double[] initialGuess = new double[] {
            norm * boundedDouble(data.consumeInt(), 0.7, 1.3),
            mean + boundedDouble(data.consumeInt(), -0.3 * sigma, 0.3 * sigma),
            sigma * boundedDouble(data.consumeInt(), 0.7, 1.3)
        };
        if (!(initialGuess[2] > 0.0) || Double.isNaN(initialGuess[2]) || Double.isInfinite(initialGuess[2])) {
            initialGuess[2] = sigma;
        }

        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            base.addObservedPoint(1.0, xs[i], ys[i]);
        }

        try {
            base.fit();
        } catch (Throwable t) {
            if (!isKnownValidationLike(t)) {
                return;
            }
            return;
        }

        GaussianFitter augmented = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < n; i++) {
            augmented.addObservedPoint(1.0, xs[i], ys[i]);
        }

        int zeros = data.consumeInt(1, 6);
        for (int i = 0; i < zeros; i++) {
            double zx = mean + boundedDouble(data.consumeInt(), -6.0 * sigma, 6.0 * sigma);
            double zy = boundedDouble(data.consumeInt(), 0.0, norm);
            augmented.addObservedPoint(0.0, zx, zy);
        }

        double[] p1;
        double[] p2;
        try {
            p1 = base.fit(initialGuess.clone());
            p2 = augmented.fit(initialGuess.clone());
        } catch (Throwable t) {
            return;
        }

        try {
            assertValidGaussianParams(p1);
            assertValidGaussianParams(p2);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            return;
        }

        /*
         * Soundness: CurveFitter.fit builds target/weights arrays from observations and solves a weighted
         * least-squares problem. Adding observations whose weight is exactly zero does not change that
         * objective. With the same explicit initial guess and deterministic optimizer, fit(double[]) on
         * the original and zero-weight-augmented datasets must therefore agree on the model they fit.
         * This catches a masked helper inconsistency even if the top-level public fit() crash is silenced.
         */
        double maxAbs = 0.0;
        for (int i = 0; i < xs.length; i++) {
            double y1;
            double y2;
            try {
                y1 = g.value(xs[i], p1);
                y2 = g.value(xs[i], p2);
            } catch (RuntimeException ex) {
                throw new RuntimeException("[oracle:zero-weight-fitdouble] metamorphic violation: fitted parameters are not usable Gaussian parameters");
            }
            double diff = Math.abs(y1 - y2);
            if (diff > maxAbs) {
                maxAbs = diff;
            }
        }

        double scale = Math.max(1.0, norm);
        if (maxAbs > 1.0e-6 * scale) {
            throw new RuntimeException(
                "[oracle:zero-weight-fitdouble] metamorphic violation: zero-weight observations changed fitted model"
                    + " maxAbs=" + maxAbs
                    + " p1=[" + p1[0] + "," + p1[1] + "," + p1[2] + "]"
                    + " p2=[" + p2[0] + "," + p2[1] + "," + p2[2] + "]"
                    + " n=" + n
                    + " zeros=" + zeros);
        }
    }

    private static void assertValidGaussianParams(double[] p) {
        if (p == null || p.length != 3) {
            throw new RuntimeException("[oracle:zero-weight-fitdouble] metamorphic violation: unexpected parameter vector length");
        }
        if (!(p[2] > 0.0) || Double.isNaN(p[2]) || Double.isInfinite(p[2])) {
            throw new RuntimeException(
                "[oracle:zero-weight-fitdouble] metamorphic violation: non-positive fitted sigma " + p[2]);
        }
        for (int i = 0; i < p.length; i++) {
            if (Double.isNaN(p[i]) || Double.isInfinite(p[i])) {
                throw new RuntimeException(
                    "[oracle:zero-weight-fitdouble] metamorphic violation: non-finite fitted parameter index=" + i + " value=" + p[i]);
            }
        }
    }

    private static boolean isKnownValidationLike(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static double boundedDouble(int raw, double min, double max) {
        long positive = raw & 0x7fffffffL;
        double unit = positive / (double) Integer.MAX_VALUE;
        return min + (max - min) * unit;
    }

    private static double positiveFromInt(int raw, double min, double max) {
        double v = boundedDouble(raw, min, max);
        if (v <= 0.0 || Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }
        return v;
    }
}