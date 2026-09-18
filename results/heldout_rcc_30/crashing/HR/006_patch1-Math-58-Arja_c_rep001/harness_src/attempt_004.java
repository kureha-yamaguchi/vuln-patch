package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();
        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            exerciseSynthetic(data);
        }
    }

    private static void exerciseAnchor() {
        final double[] anchor = {
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

        WeightedObservedPoint[] before = fitter.getObservations();
        double[] p1;
        try {
            p1 = fitter.fit();
        } catch (Throwable t) {
            if (isRelevantValidationFailure(t)) {
                throw new RuntimeException("[oracle:anchor-valid] fit() rejected a valid anchor dataset", t);
            }
            return;
        }

        WeightedObservedPoint[] after = fitter.getObservations();
        assertSameObservations(before, after, "anchor");

        double[] p2;
        try {
            p2 = fitter.fit();
        } catch (Throwable t) {
            return;
        }

        assertCloseArray(p1, p2, 1e-10, 1e-8, "anchor-repeat");
        assertApprox(p1[1], 53.1572792, 1e-9, 1e-6, "anchor-center");
    }

    private static void exerciseSynthetic(FuzzedDataProvider data) {
        int n = data.consumeInt(8, 40);
        double norm = 1.0 + data.consumeInt(0, 5000);
        double sigma = 0.5 + data.consumeInt(0, 2000) / 200.0;
        double mean = n + data.consumeInt(1, 60);
        double scale = 0.25 + data.consumeInt(1, 20);

        GaussianFitter base = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter scaled = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < n; i++) {
            double x = i;
            double z = (x - mean) / sigma;
            double y = norm * Math.exp(-0.5 * z * z);
            base.addObservedPoint(x, y);
            scaled.addObservedPoint(x, y * scale);
        }

        WeightedObservedPoint[] before = base.getObservations();
        double[] pBase;
        try {
            pBase = base.fit();
        } catch (Throwable t) {
            if (isRelevantValidationFailure(t)) {
                throw new RuntimeException("[oracle:valid-reject] fit() rejected a valid synthetic Gaussian dataset", t);
            }
            return;
        }
        assertSameObservations(before, base.getObservations(), "synthetic-readonly");

        double[] pScaled;
        try {
            pScaled = scaled.fit();
        } catch (Throwable t) {
            if (isRelevantValidationFailure(t)) {
                throw new RuntimeException("[oracle:valid-reject-scaled] fit() rejected a valid scaled Gaussian dataset", t);
            }
            return;
        }

        if (pBase == null || pScaled == null || pBase.length < 3 || pScaled.length < 3) {
            return;
        }

        /* For exact Gaussian samples, multiplying all y values by a positive constant
           must only scale the fitted amplitude; center and sigma describe x-geometry
           and are unchanged. A throw-deleting patch could still return wrong values. */
        assertApprox(pBase[1], pScaled[1], 1e-6, 5e-2, "scale-center");
        assertApprox(Math.abs(pBase[2]), Math.abs(pScaled[2]), 1e-6, 5e-2, "scale-sigma");
        assertApprox(pBase[0] * scale, pScaled[0], 1e-6, 8e-2, "scale-norm");

        double[] pBase2;
        try {
            pBase2 = base.fit();
        } catch (Throwable t) {
            return;
        }
        assertCloseArray(pBase, pBase2, 1e-10, 1e-8, "synthetic-repeat");
    }

    private static boolean isRelevantValidationFailure(Throwable t) {
        if (t == null || !isValidationFamily(t)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                    || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))
                    || (c != null && c.contains("GaussianFitter$ParameterGuesser") && "guess".equals(m))
                    || (c != null && c.contains("Gaussian$Parametric"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationFamily(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String cn = t.getClass().getName();
        return cn != null && cn.startsWith("org.apache.commons.math.exception");
    }

    private static void assertSameObservations(WeightedObservedPoint[] a, WeightedObservedPoint[] b, String id) {
        if (a == null || b == null || a.length != b.length) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: fit changed observation count");
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i].getX()) != Double.doubleToLongBits(b[i].getX())
                    || Double.doubleToLongBits(a[i].getY()) != Double.doubleToLongBits(b[i].getY())
                    || Double.doubleToLongBits(a[i].getWeight()) != Double.doubleToLongBits(b[i].getWeight())) {
                throw new RuntimeException("[oracle:" + id + "] metamorphic violation: fit changed observations at index " + i);
            }
        }
    }

    private static void assertCloseArray(double[] a, double[] b, double absTol, double relTol, String id) {
        if (a.length != b.length) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: length mismatch " + a.length + " vs " + b.length);
        }
        for (int i = 0; i < a.length; i++) {
            assertApprox(a[i], b[i], absTol, relTol, id + "-" + i);
        }
    }

    private static void assertApprox(double expected, double actual, double absTol, double relTol, String id) {
        if (Double.isNaN(expected) || Double.isNaN(actual) || Double.isInfinite(expected) || Double.isInfinite(actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: non-finite values lhs=" + expected + " rhs=" + actual);
        }
        double diff = Math.abs(expected - actual);
        double limit = Math.max(absTol, relTol * Math.max(Math.abs(expected), Math.abs(actual)));
        if (diff > limit) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: lhs=" + expected + " rhs=" + actual + " diff=" + diff + " limit=" + limit);
        }
    }
}