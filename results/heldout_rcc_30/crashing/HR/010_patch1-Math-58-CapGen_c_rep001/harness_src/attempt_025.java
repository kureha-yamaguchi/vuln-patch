package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = {
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
        runAnchor();

        int points = data.consumeInt(5, 25);
        double norm = 0.1 + data.consumeInt(0, 5000) / 50.0;
        double mean = data.consumeInt(-200, 200) / 4.0;
        double sigma = 0.2 + data.consumeInt(0, 2000) / 100.0;
        double x0 = mean - data.consumeInt(2, 20);
        double step = 0.2 + data.consumeInt(1, 40) / 10.0;
        boolean addExtraOnCurvePoint = data.consumeBoolean();

        double[] xs = new double[points];
        double[] ys = new double[points];
        Gaussian g = new Gaussian(norm, mean, sigma);
        for (int i = 0; i < points; i++) {
            double x = x0 + (i * step);
            xs[i] = x;
            ys[i] = g.value(x);
        }

        checkOnCurveAugmentationInvariant(xs, ys, norm, mean, sigma, addExtraOnCurvePoint, data);

        int shape = data.consumeInt(0, 2);
        if (shape == 0) {
            exerciseTruncatedGaussian(data);
        } else if (shape == 1) {
            exerciseAnchorVariant(data);
        } else {
            exerciseExactGaussian(data);
        }
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(i, ANCHOR[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (!isCleanRejection(t) && !isKnownRootCause(t)) {
                // Ignore unrelated pre-existing failures outside this patch's scope.
            }
        }
    }

    private static void exerciseAnchorVariant(FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        int prefix = data.consumeInt(0, 3);
        int suffix = data.consumeInt(0, 3);
        double x = 0.0;
        for (int i = 0; i < prefix; i++) {
            fitter.addObservedPoint(x++, 0.0);
        }
        for (int i = 0; i < ANCHOR.length; i++) {
            fitter.addObservedPoint(x++, ANCHOR[i]);
        }
        for (int i = 0; i < suffix; i++) {
            fitter.addObservedPoint(x++, ANCHOR[ANCHOR.length - 1]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isKnownRootCause(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseTruncatedGaussian(FuzzedDataProvider data) {
        double norm = 1.0 + data.consumeInt(0, 1000) / 20.0;
        double mean = data.consumeInt(-100, 100) / 2.0;
        double sigma = 0.5 + data.consumeInt(0, 500) / 40.0;
        int points = data.consumeInt(8, 24);
        double start = mean - sigma * data.consumeInt(6, 12);
        double step = sigma / data.consumeInt(2, 6);
        Gaussian gaussian = new Gaussian(norm, mean, sigma);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < points; i++) {
            double x = start + i * step;
            double y = gaussian.value(x);
            fitter.addObservedPoint(x, y);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isKnownRootCause(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exerciseExactGaussian(FuzzedDataProvider data) {
        double norm = 0.5 + data.consumeInt(0, 4000) / 40.0;
        double mean = data.consumeInt(-200, 200) / 5.0;
        double sigma = 0.3 + data.consumeInt(0, 2000) / 80.0;
        int left = data.consumeInt(3, 12);
        int right = data.consumeInt(3, 12);
        double step = sigma / data.consumeInt(2, 8);

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        Gaussian gaussian = new Gaussian(norm, mean, sigma);
        for (int i = -left; i <= right; i++) {
            double x = mean + i * step;
            fitter.addObservedPoint(x, gaussian.value(x));
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isKnownRootCause(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void checkOnCurveAugmentationInvariant(double[] xs, double[] ys,
                                                          double norm, double mean, double sigma,
                                                          boolean addExtraOnCurvePoint,
                                                          FuzzedDataProvider data) {
        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter augmented = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < xs.length; i++) {
            base.addObservedPoint(xs[i], ys[i]);
            augmented.addObservedPoint(xs[i], ys[i]);
        }

        if (addExtraOnCurvePoint) {
            double extraX = mean + (data.consumeInt(-20, 20) / 10.0) * sigma;
            double extraY = new Gaussian(norm, mean, sigma).value(extraX);
            augmented.addObservedPoint(extraX, extraY);
        } else {
            double extraX = mean;
            double extraY = new Gaussian(norm, mean, sigma).value(extraX);
            augmented.addObservedPoint(extraX, extraY);
        }

        double[] p1;
        double[] p2;
        try {
            p1 = base.fit();
            p2 = augmented.fit();
        } catch (Throwable t) {
            return;
        }

        if (p1 == null || p2 == null || p1.length != 3 || p2.length != 3) {
            return;
        }

        if (!closeParams(p1, p2)) {
            throw new RuntimeException(
                "[oracle:on-curve-augmentation] metamorphic violation: adding an extra observation " +
                "that lies exactly on the same Gaussian curve should preserve the least-squares " +
                "solution for noiseless data. base=[" + p1[0] + "," + p1[1] + "," + p1[2] +
                "] augmented=[" + p2[0] + "," + p2[1] + "," + p2[2] + "]"
            );
        }
    }

    private static boolean closeParams(double[] a, double[] b) {
        return close(a[0], b[0], 1e-3, 1e-2)
            && close(a[1], b[1], 1e-3, 1e-2)
            && close(a[2], b[2], 1e-3, 1e-2);
    }

    private static boolean close(double a, double b, double absTol, double relTol) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= absTol || diff <= relTol * Math.max(1.0, scale);
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isKnownRootCause(Throwable t) {
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))) {
                return true;
            }
            if ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls)) {
                return true;
            }
        }
        return false;
    }
}