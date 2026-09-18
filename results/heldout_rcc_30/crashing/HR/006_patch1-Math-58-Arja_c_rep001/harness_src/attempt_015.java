package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.exception.MathIllegalArgumentException;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = {
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
        exerciseAnchor();
        exerciseExplore(data);
    }

    private static void exerciseAnchor() {
        runShiftEquivarianceScenario(ANCHOR_Y, 0.0, 1.0);
        runShiftEquivarianceScenario(ANCHOR_Y, 0.0, 2.0);
    }

    private static void exerciseExplore(FuzzedDataProvider data) {
        int n = data.consumeInt(8, 40);
        double amplitude = 1.0 + data.consumeInt(0, 10000);
        double sigma = 2.0 + data.consumeInt(0, 2000) / 200.0;
        double center = n + 10.0 + data.consumeInt(0, 1200) / 20.0;
        double x0 = data.consumeInt(-20, 20);
        double step = data.consumeBoolean() ? 1.0 : 0.5;
        double shift = data.consumeBoolean() ? 1.0 : 2.0;

        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double x = x0 + i * step;
            double dx = x - center;
            ys[i] = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            if (!(ys[i] >= 0.0) || Double.isNaN(ys[i]) || Double.isInfinite(ys[i])) {
                return;
            }
        }

        runShiftEquivarianceScenario(ys, x0, shift);
    }

    private static void runShiftEquivarianceScenario(double[] ys, double xStart, double shift) {
        FitResult baseWrapped = runWrappedFit(ys, xStart);
        FitResult shiftedWrapped = runWrappedFit(ys, xStart + shift);

        if (!baseWrapped.success || !shiftedWrapped.success) {
            return;
        }

        FitResult baseNoArg = runNoArgFit(ys, xStart);
        FitResult shiftedNoArg = runNoArgFit(ys, xStart + shift);

        if (isRelevantRootCause(baseNoArg.failure) || isRelevantRootCause(shiftedNoArg.failure)) {
            throw new RuntimeException(
                "[oracle:shift-equiv] no-arg fit rejected translated observations although delegated fit(double[]) accepted both variants");
        }

        if (!baseNoArg.success || !shiftedNoArg.success) {
            return;
        }

        /*
         * Sound metamorphic guarantee:
         * The fitted model is y = A * exp(-(x-c)^2 / (2*s^2)).
         * Translating every observation x by a constant must translate the fitted center by
         * the same constant while leaving amplitude and sigma unchanged.
         * This is checked only when both real library calls succeed.
         */
        double[] p0 = baseNoArg.params;
        double[] p1 = shiftedNoArg.params;

        if (!approximatelyEqualRelative(p0[0], p1[0], 1e-3)
                || !approximatelyEqualRelative(p0[2], p1[2], 1e-3)
                || !approximatelyEqualAbsolute((p1[1] - p0[1]), shift, 1e-3)) {
            throw new RuntimeException(
                "[oracle:shift-equiv] translated fits disagree: amp0=" + p0[0]
                    + " amp1=" + p1[0]
                    + " center0=" + p0[1]
                    + " center1=" + p1[1]
                    + " sigma0=" + p0[2]
                    + " sigma1=" + p1[2]
                    + " shift=" + shift);
        }
    }

    private static FitResult runWrappedFit(double[] ys, double xStart) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xStart + i, ys[i]);
        }
        try {
            WeightedObservedPoint[] obs = fitter.getObservations();
            double[] guess = (new GaussianFitter.ParameterGuesser(obs)).guess();
            return FitResult.success(fitter.fit(guess));
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return FitResult.failure(t);
            }
            return FitResult.failure(t);
        }
    }

    private static FitResult runNoArgFit(double[] ys, double xStart) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xStart + i, ys[i]);
        }
        try {
            return FitResult.success(fitter.fit());
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return FitResult.failure(t);
            }
            return FitResult.failure(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof MathIllegalArgumentException;
    }

    private static boolean isRelevantRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cn = e.getClassName();
            String mn = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cn) && "fit".equals(mn))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cn) && "getObservations".equals(mn))
                || (cn != null && cn.contains("GaussianFitter$ParameterGuesser") && "guess".equals(mn))) {
                return true;
            }
        }
        return false;
    }

    private static boolean approximatelyEqualRelative(double a, double b, double relTol) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= relTol * scale;
    }

    private static boolean approximatelyEqualAbsolute(double a, double b, double absTol) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        return Math.abs(a - b) <= absTol;
    }

    private static final class FitResult {
        final boolean success;
        final double[] params;
        final Throwable failure;

        private FitResult(boolean success, double[] params, Throwable failure) {
            this.success = success;
            this.params = params;
            this.failure = failure;
        }

        static FitResult success(double[] params) {
            return new FitResult(true, params, null);
        }

        static FitResult failure(Throwable t) {
            return new FitResult(false, null, t);
        }
    }
}