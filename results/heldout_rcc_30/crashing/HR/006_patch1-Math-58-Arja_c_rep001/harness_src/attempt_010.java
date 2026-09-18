package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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
        runScenario(ANCHOR, true);

        double[] fuzz = buildGaussianLikeData(data);
        if (fuzz != null) {
            runScenario(fuzz, false);
        }
    }

    private static double[] buildGaussianLikeData(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return null;
        }

        int n = data.consumeInt(3, 40);
        double amplitude = 1.0 + data.consumeInt(0, 1_000_000);
        double sigma = 0.5 + data.consumeInt(0, 2000) / 100.0;
        boolean centerRight = data.consumeBoolean();
        double center;
        if (centerRight) {
            center = n - 1 + 1.0 + data.consumeInt(0, 2000) / 50.0;
        } else {
            center = -1.0 - data.consumeInt(0, 2000) / 50.0;
        }
        double floor = 1e-18 * (1 + data.consumeInt(0, 1000));
        double noiseScale = data.consumeInt(0, 1000) / 1_000_000.0;

        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double z = (i - center) / sigma;
            double base = amplitude * Math.exp(-0.5 * z * z) + floor;
            double perturb = 1.0 + ((data.consumeInt(-1000, 1000)) * noiseScale);
            if (perturb <= 0.01) {
                perturb = 0.01;
            }
            y[i] = base * perturb;
            if (!(y[i] > 0.0) || Double.isNaN(y[i]) || Double.isInfinite(y[i])) {
                y[i] = floor;
            }
        }

        if (data.consumeBoolean()) {
            for (int i = 1; i < n; i++) {
                if (centerRight) {
                    if (y[i] < y[i - 1]) {
                        y[i] = y[i - 1] + floor;
                    }
                } else {
                    int j = n - 1 - i;
                    if (y[j] < y[j + 1]) {
                        y[j] = y[j + 1] + floor;
                    }
                }
            }
        }

        return y;
    }

    private static void runScenario(double[] ys, boolean anchor) {
        if (ys == null || ys.length < 3) {
            return;
        }

        WeightedObservedPoint[] originalObs = buildObservations(ys, false);
        WeightedObservedPoint[] reversedObs = buildObservations(ys, true);

        checkGuesserPermutationInvariant(originalObs, reversedObs, anchor);
        checkFitPermutationInvariant(originalObs, reversedObs, anchor);
    }

    private static WeightedObservedPoint[] buildObservations(double[] ys, boolean reversed) {
        WeightedObservedPoint[] obs = new WeightedObservedPoint[ys.length];
        for (int i = 0; i < ys.length; i++) {
            int src = reversed ? (ys.length - 1 - i) : i;
            obs[i] = new WeightedObservedPoint(1.0, src, ys[src]);
        }
        return obs;
    }

    private static void checkGuesserPermutationInvariant(WeightedObservedPoint[] a, WeightedObservedPoint[] b, boolean anchor) {
        try {
            double[] g1 = new GaussianFitter.ParameterGuesser(a).guess();
            double[] g2 = new GaussianFitter.ParameterGuesser(b).guess();

            if (g1.length != g2.length) {
                throw new RuntimeException("[oracle:guess-perm] metamorphic violation: guess length differs lhs="
                        + g1.length + " rhs=" + g2.length);
            }

            for (int i = 0; i < g1.length; i++) {
                if (!closeEnough(g1[i], g2[i], 1e-10, 1e-10)) {
                    throw new RuntimeException("[oracle:guess-perm] metamorphic violation: "
                            + "ParameterGuesser.guess() must depend only on the observation set, not insertion order; "
                            + "lhs=" + Arrays.toString(g1) + " rhs=" + Arrays.toString(g2));
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:" + (anchor ? "anchor-guess-root" : "guess-root")
                        + "] valid observations triggered root-cause exception in guess/fit path", t);
            }
        }
    }

    private static void checkFitPermutationInvariant(WeightedObservedPoint[] a, WeightedObservedPoint[] b, boolean anchor) {
        double[] p1;
        double[] p2;
        try {
            p1 = fitFromObservations(a);
            p2 = fitFromObservations(b);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:" + (anchor ? "anchor-fit-root" : "fit-root")
                        + "] valid observations triggered root-cause exception through GaussianFitter.fit()", t);
            }
            return;
        }

        try {
            Gaussian.Parametric gp = new Gaussian.Parametric();
            for (WeightedObservedPoint p : a) {
                double y1 = gp.value(p.getX(), p1);
                double y2 = gp.value(p.getX(), p2);
                if (!closeEnough(y1, y2, 1e-6, 1e-2)) {
                    throw new RuntimeException("[oracle:fit-perm] metamorphic violation: "
                            + "fitting the same observation set in a different insertion order must yield the same fitted curve; "
                            + "x=" + p.getX() + " lhs=" + y1 + " rhs=" + y2
                            + " p1=" + Arrays.toString(p1) + " p2=" + Arrays.toString(p2));
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:" + (anchor ? "anchor-fit-post-root" : "fit-post-root")
                        + "] fitted parameters caused the root-cause validation failure", t);
            }
        }
    }

    private static double[] fitFromObservations(WeightedObservedPoint[] obs) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (WeightedObservedPoint p : obs) {
            fitter.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }
        return fitter.fit();
    }

    private static boolean closeEnough(double a, double b, double absTol, double relTol) {
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        double diff = Math.abs(a - b);
        if (diff <= absTol) {
            return true;
        }
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= relTol * Math.max(1.0, scale);
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.apache.commons.math.exception.")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean classMatch = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if ("org.apache.commons.math.exception.NotStrictlyPositiveException".equals(cur.getClass().getName())) {
                classMatch = true;
                break;
            }
        }
        if (!classMatch) {
            return false;
        }

        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && method.startsWith("fit"))
                    || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                    || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))
                    || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls) && "validateParameters".equals(method))) {
                return true;
            }
        }
        return false;
    }
}