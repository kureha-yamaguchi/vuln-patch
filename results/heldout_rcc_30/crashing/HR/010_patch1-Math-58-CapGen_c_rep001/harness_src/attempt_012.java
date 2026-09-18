package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
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
        runFuzzedScenario(data);
    }

    private static void runAnchor() {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ANCHOR_DATA.length; i++) {
            fitter.addObservedPoint(i, ANCHOR_DATA[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runFuzzedScenario(FuzzedDataProvider data) {
        int n = data.consumeInt(7, 31);
        double norm = positiveFinite(data.consumeInt(-5000, 5000), 10.0);
        double mean = data.consumeInt(-200, 200);
        double sigma = positiveFinite(data.consumeInt(1, 80), 10.0);
        double scale = positiveFinite(data.consumeInt(1, 40), 10.0);
        double shift = data.consumeInt(-200, 200);
        int spacing = data.consumeInt(1, 5);

        Gaussian.Parametric g = new Gaussian.Parametric();
        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter transformed = new GaussianFitter(new LevenbergMarquardtOptimizer());

        int centerIndex = n / 2;
        for (int i = 0; i < n; i++) {
            double x = mean + (i - centerIndex) * spacing;
            double y;
            try {
                y = g.value(x, new double[] { norm, mean, sigma });
            } catch (Throwable t) {
                return;
            }
            if (!Double.isFinite(y)) {
                return;
            }
            base.addObservedPoint(x, y);
            transformed.addObservedPoint(scale * x + shift, y);
        }

        checkGetObservationsSnapshot(base);
        checkScaledXAxisRelation(base, transformed, scale, shift);
    }

    private static void checkGetObservationsSnapshot(GaussianFitter fitter) {
        WeightedObservedPoint[] first = fitter.getObservations();
        if (first.length == 0) {
            return;
        }
        WeightedObservedPoint[] second = fitter.getObservations();
        if (second.length != first.length) {
            throw new RuntimeException("[oracle:getobs-snapshot] metamorphic violation: lengths disagree before mutation lhs=" + first.length + " rhs=" + second.length);
        }

        first[0] = null;
        WeightedObservedPoint[] third = fitter.getObservations();

        if (third.length != second.length) {
            throw new RuntimeException("[oracle:getobs-snapshot] metamorphic violation: mutation of returned array changed later size before=" + second.length + " after=" + third.length);
        }
        if (third[0] == null) {
            throw new RuntimeException("[oracle:getobs-snapshot] metamorphic violation: getObservations must return a fresh snapshot array, but caller mutation was reflected");
        }
    }

    private static void checkScaledXAxisRelation(GaussianFitter base, GaussianFitter transformed, double scale, double shift) {
        double[] p1;
        double[] p2;
        try {
            p1 = base.fit();
            p2 = transformed.fit();
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (p1 == null || p2 == null || p1.length < 3 || p2.length < 3) {
            return;
        }
        if (!allFinite(p1) || !allFinite(p2)) {
            return;
        }

        double expectedMean2 = scale * p1[1] + shift;
        double expectedSigma2 = Math.abs(scale) * p1[2];
        double expectedNorm2 = p1[0];

        if (!closeEnough(p2[0], expectedNorm2) ||
            !closeEnough(p2[1], expectedMean2) ||
            !closeEnough(p2[2], expectedSigma2)) {
            throw new RuntimeException(
                "[oracle:xscale-fit] metamorphic violation: scaling all x coordinates by s and shifting by b must transform fitted mean/sigma the same way for the same y-values; " +
                "s=" + scale + " b=" + shift +
                " base=[" + p1[0] + "," + p1[1] + "," + p1[2] + "]" +
                " transformed=[" + p2[0] + "," + p2[1] + "," + p2[2] + "]" +
                " expectedTransformed=[" + expectedNorm2 + "," + expectedMean2 + "," + expectedSigma2 + "]"
            );
        }
    }

    private static boolean closeEnough(double actual, double expected) {
        double diff = Math.abs(actual - expected);
        double scale = Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected)));
        return diff <= 1e-2 * scale + 1e-6;
    }

    private static boolean allFinite(double[] a) {
        for (int i = 0; i < a.length; i++) {
            if (!Double.isFinite(a[i])) {
                return false;
            }
        }
        return true;
    }

    private static double positiveFinite(int value, double divisor) {
        double d = Math.abs(value) / divisor;
        return d + 0.5;
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
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(method))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(method))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(method))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls) && "validateParameters".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}