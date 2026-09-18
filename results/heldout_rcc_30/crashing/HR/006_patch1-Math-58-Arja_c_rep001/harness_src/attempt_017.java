package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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

        int points = data.consumeInt(7, 25);
        if ((points & 1) == 0) {
            points++;
        }

        double norm = 1.0 + (data.consumeInt(0, 5000) / 10.0);
        double mean = data.consumeInt(-200, 200) / 10.0;
        double sigma = 0.25 + (data.consumeInt(0, 400) / 20.0);
        double step = 0.1 + (data.consumeInt(1, 30) / 10.0);

        Gaussian.Parametric parametric = new Gaussian.Parametric();
        double[] truth = new double[] { norm, mean, sigma };

        GaussianFitter unitWeight = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter variedWeight = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int mid = points / 2;
        for (int i = 0; i < points; i++) {
            double x = mean + (i - mid) * step;
            double y;
            try {
                y = parametric.value(x, truth);
            } catch (RuntimeException e) {
                return;
            }
            unitWeight.addObservedPoint(x, y);
            double w = 0.1 + (data.consumeInt(0, 1000) / 100.0);
            variedWeight.addObservedPoint(w, x, y);
        }

        double[] fitUnit;
        double[] fitVaried;
        try {
            fitUnit = unitWeight.fit();
            fitVaried = variedWeight.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            assertExactResidual("unit-exact-residual", parametric, truth, fitUnit, points, mean, step);
            assertExactResidual("varied-weight-exact-residual", parametric, truth, fitVaried, points, mean, step);

            /* For exact Gaussian data, any strictly positive reweighting preserves the same zero-residual optimum.
             * This cross-check is independent from the known crash symptom and can catch silent wrong-output fixes.
             */
            if (Math.abs(fitUnit[1] - fitVaried[1]) > 1e-3 ||
                Math.abs(fitUnit[2] - fitVaried[2]) > 1e-3 ||
                Math.abs(fitUnit[0] - fitVaried[0]) > 1e-2) {
                throw new RuntimeException(
                    "[oracle:weight-invariant-exact] metamorphic violation: positive weights changed exact-fit result " +
                    "truth=(" + truth[0] + "," + truth[1] + "," + truth[2] + ")" +
                    " unit=(" + fitUnit[0] + "," + fitUnit[1] + "," + fitUnit[2] + ")" +
                    " varied=(" + fitVaried[0] + "," + fitVaried[1] + "," + fitVaried[2] + ")"
                );
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
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
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void assertExactResidual(String id, Gaussian.Parametric p, double[] truth, double[] fit,
                                            int points, double mean, double step) {
        if (fit == null || fit.length < 3) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: missing fit result");
        }
        int mid = points / 2;
        double sse = 0.0;
        double baseline = 0.0;
        for (int i = 0; i < points; i++) {
            double x = mean + (i - mid) * step;
            double expected = p.value(x, truth);
            double actual = p.value(x, fit);
            double d = expected - actual;
            sse += d * d;
            baseline += expected * expected;
        }
        double rel = sse / Math.max(1e-30, baseline);
        if (!(rel <= 1e-8)) {
            throw new RuntimeException(
                "[oracle:" + id + "] metamorphic violation: exact Gaussian data not reproduced by fit " +
                "truth=(" + truth[0] + "," + truth[1] + "," + truth[2] + ")" +
                " fit=(" + fit[0] + "," + fit[1] + "," + fit[2] + ")" +
                " relSSE=" + rel
            );
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof MathIllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(c)
                    && ("value".equals(m) || "gradient".equals(m) || "validateParameters".equals(m)))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}