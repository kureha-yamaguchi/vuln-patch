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
        runBoundaryVariants(data);
        runKnownNormOracle(data);
    }

    private static void runAnchor() {
        try {
            fitSeries(ANCHOR, 0, ANCHOR.length, 0, 0, 0.25, 0.25);
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void runBoundaryVariants(FuzzedDataProvider data) {
        int leftTrim = data.consumeInt(0, 2);
        int rightTrim = data.consumeInt(0, 2);
        int extraLeft = data.consumeInt(0, 3);
        int extraRight = data.consumeInt(0, 3);

        int start = leftTrim;
        int end = ANCHOR.length - rightTrim;
        if (end - start < 3) {
            start = 0;
            end = ANCHOR.length;
        }

        double leftScale = scaleFromByte(data.consumeByte());
        double rightScale = scaleFromByte(data.consumeByte());

        try {
            fitSeries(ANCHOR, start, end, extraLeft, extraRight, leftScale, rightScale);
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void runKnownNormOracle(FuzzedDataProvider data) {
        int points = data.consumeInt(9, 25);
        double norm = 1.0 + data.consumeInt(0, 500);
        double mean = data.consumeInt(-20, 20) + data.consumeInt(0, 1000) / 1000.0;
        double sigma = 0.5 + data.consumeInt(0, 4500) / 1000.0;

        Gaussian.Parametric g = new Gaussian.Parametric();
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        double minX = mean - 3.0 * sigma;
        double maxX = mean + 3.0 * sigma;
        for (int i = 0; i < points; i++) {
            double x = minX + (maxX - minX) * i / (points - 1.0);
            double y;
            try {
                y = g.value(x, new double[] { norm, mean, sigma });
            } catch (Throwable t) {
                return;
            }
            fitter.addObservedPoint(x, y);
        }

        final double[] p;
        try {
            p = fitter.fit();
        } catch (Throwable t) {
            if (isRootCauseThrowable(t)) {
                throwAsRuntime(t);
            }
            return;
        }
        if (p == null || p.length < 3) {
            return;
        }

        try {
            double reconstructedNorm = g.value(p[1], p);
            if (!closeEnough(reconstructedNorm, p[0], 1e-9, 1e-9)) {
                throw new RuntimeException("[oracle:self-peak] metamorphic violation: gaussian peak must equal norm lhs="
                        + reconstructedNorm + " rhs=" + p[0]);
            }
        } catch (Throwable t) {
            if (isRootCauseThrowable(t)) {
                throwAsRuntime(t);
            }
            return;
        }

        double absTol = Math.max(1e-6, norm * 5e-2);
        if (Math.abs(p[0] - norm) > absTol) {
            throw new RuntimeException("[oracle:known-norm] metamorphic violation: exact gaussian data should recover amplitude inputNorm="
                    + norm + " fittedNorm=" + p[0] + " mean=" + mean + " sigma=" + sigma + " points=" + points);
        }
    }

    private static double[] fitSeries(double[] base, int start, int end, int extraLeft, int extraRight,
                                      double leftScale, double rightScale) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        int x = 0;

        double leftBase = positiveTiny(base[start] * leftScale);
        for (int i = extraLeft; i > 0; --i) {
            fitter.addObservedPoint(x++, leftBase / (i + 1.0));
        }

        for (int i = start; i < end; i++) {
            fitter.addObservedPoint(x++, base[i]);
        }

        double rightBase = positiveTiny(base[end - 1] * rightScale);
        for (int i = 0; i < extraRight; i++) {
            fitter.addObservedPoint(x++, rightBase / (i + 2.0));
        }

        return fitter.fit();
    }

    private static double positiveTiny(double v) {
        double a = Math.abs(v);
        if (a == 0.0 || Double.isNaN(a) || Double.isInfinite(a)) {
            return 1e-30;
        }
        return Math.max(1e-30, a);
    }

    private static double scaleFromByte(byte b) {
        int u = b & 0xFF;
        return 0.05 + (u / 255.0) * 0.95;
    }

    private static boolean closeEnough(double a, double b, double rel, double abs) {
        double diff = Math.abs(a - b);
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return diff <= Math.max(abs, rel * scale);
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCauseThrowable(t)) {
            throwAsRuntime(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!"org.apache.commons.math.exception.NotStrictlyPositiveException".equals(t.getClass().getName())) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && ("fit".equals(m)))
                    || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))
                    || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))
                    || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(c))) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsRuntime(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}