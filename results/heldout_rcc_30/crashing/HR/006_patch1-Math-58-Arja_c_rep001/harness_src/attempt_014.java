package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.exception.NumberIsTooSmallException;
import org.apache.commons.math.exception.OutOfRangeException;
import org.apache.commons.math.exception.ZeroException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
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
        runSyntheticMirrorOracle(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_Y.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_Y[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runSyntheticMirrorOracle(FuzzedDataProvider data) {
        int points = data.consumeInt(9, 21);
        if ((points & 1) == 0) {
            points++;
        }

        double norm = positive(data.consumeInt(1, 5000) / 10.0);
        double mean = data.consumeInt(-200, 200) / 10.0;
        double sigma = positive(data.consumeInt(5, 120) / 10.0);
        double step = positive(data.consumeInt(1, 30) / 10.0);

        double[] truth = new double[] { norm, mean, sigma };
        double[] fitA;
        double[] fitB;

        try {
            fitA = fitExactGaussian(points, truth, step, false);
        } catch (Throwable t) {
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            fitB = fitExactGaussian(points, truth, step, true);
        } catch (Throwable t) {
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double[] mirroredTruth = new double[] { norm, -mean, sigma };

        /* Contract used for this oracle:
         * We construct noiseless samples exactly from the library's own Gaussian.Parametric model.
         * A correct GaussianFitter.fit() should recover the generating parameters on such valid data,
         * and reflecting x -> -x must reflect the fitted center while preserving norm and sigma.
         * A patch that merely suppresses the throw but feeds the optimizer the wrong delegate can
         * still return numerically wrong parameters, which these checks detect.
         */
        if (!close(fitA[0], truth[0]) || !close(fitA[1], truth[1]) || !close(fitA[2], truth[2])) {
            throw new RuntimeException("[oracle:exact-recover] metamorphic violation: fit() failed to recover exact generating Gaussian"
                + " truth=[" + truth[0] + "," + truth[1] + "," + truth[2] + "]"
                + " fit=[" + fitA[0] + "," + fitA[1] + "," + fitA[2] + "]");
        }

        if (!close(fitB[0], mirroredTruth[0]) || !close(fitB[1], mirroredTruth[1]) || !close(fitB[2], mirroredTruth[2])) {
            throw new RuntimeException("[oracle:mirror-recover] metamorphic violation: fit() failed on reflected exact Gaussian"
                + " truth=[" + mirroredTruth[0] + "," + mirroredTruth[1] + "," + mirroredTruth[2] + "]"
                + " fit=[" + fitB[0] + "," + fitB[1] + "," + fitB[2] + "]");
        }

        if (!close(fitA[0], fitB[0]) || !close(fitA[1], -fitB[1]) || !close(fitA[2], fitB[2])) {
            throw new RuntimeException("[oracle:mirror-equiv] metamorphic violation: reflecting x should negate center only"
                + " fitA=[" + fitA[0] + "," + fitA[1] + "," + fitA[2] + "]"
                + " fitB=[" + fitB[0] + "," + fitB[1] + "," + fitB[2] + "]");
        }
    }

    private static double[] fitExactGaussian(int points, double[] truth, double step, boolean mirror) throws Exception {
        Gaussian.Parametric model = new Gaussian.Parametric();
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        int half = points / 2;
        for (int i = -half; i <= half; i++) {
            double x = truth[1] + (i * step);
            if (mirror) {
                x = -x;
            }
            double y = model.value(x, mirror ? new double[] { truth[0], -truth[1], truth[2] } : truth);
            fitter.addObservedPoint(x, y);
        }
        return fitter.fit();
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean close(double a, double b) {
        double diff = Math.abs(a - b);
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return diff <= 1.0e-4 * scale;
    }

    private static double positive(double v) {
        return v <= 0.0 ? 0.1 : v;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}