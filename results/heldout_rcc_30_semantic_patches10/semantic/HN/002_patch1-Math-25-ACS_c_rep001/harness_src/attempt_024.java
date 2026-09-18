package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final int[] EXACT_TRIANGULAR_Y = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    private static WeightedObservedPoint[] buildExactTriangularPoints(int start, int step, int scale) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[EXACT_TRIANGULAR_Y.length];
        for (int i = 0; i < EXACT_TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, EXACT_TRIANGULAR_Y[i] * (double) scale);
        }
        return points;
    }

    private static WeightedObservedPoint[] buildGeneralTriangularPoints(int start, int step, int scale, int periods, int extra) {
        int[] basePeriod = new int[] { 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -2, -1 };
        int len = periods * basePeriod.length + extra;
        if (len < 4) {
            len = 4;
        }
        WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            int y = basePeriod[i % basePeriod.length];
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, y * (double) scale);
        }
        return points;
    }

    private static Throwable probeGuesser(HarmonicFitter.ParameterGuesser guesser) {
        try {
            guesser.guess();
            return null;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return t;
        }
    }

    private static void assertMath844Rejects(HarmonicFitter.ParameterGuesser guesser, String oracleId, String context) {
        Throwable t = probeGuesser(guesser);
        if (t == null) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected MathIllegalStateException but guess() completed normally; " + context);
        }
        if (!(t instanceof org.apache.commons.math3.exception.MathIllegalStateException)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected MathIllegalStateException but got " + t.getClass().getName() + "; " + context, t);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p = new WeightedObservedPoint(w, x, y);
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
                Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
                Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                    "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                    "weight=" + p.getWeight() + "/" + w +
                    " x=" + p.getX() + "/" + x +
                    " y=" + p.getY() + "/" + y);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        HarmonicFitter.ParameterGuesser exactGuesser;
        WeightedObservedPoint[] exactPoints = buildExactTriangularPoints(0, 1, 1);
        try {
            exactGuesser = new HarmonicFitter.ParameterGuesser(exactPoints);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        assertMath844Rejects(exactGuesser, "math844-exact", "input=exact failing test sample");

        int mutations = data.consumeInt(1, exactPoints.length);
        for (int i = 0; i < mutations; i++) {
            int idx = data.consumeInt(0, exactPoints.length - 1);
            double newWeight = data.consumeInt(-20, 20);
            double newX = data.consumeInt(-50, 50);
            double newY = data.consumeInt(-50, 50);
            exactPoints[idx] = new WeightedObservedPoint(newWeight, newX, newY);

            // Constructor contract: ParameterGuesser stores observations.clone().
            // So replacing slots in the caller's array after construction must not change the guesser's later observable behaviour.
            // A patch that silently keeps/uses caller-owned state, or a throw-deleting patch that returns coefficients instead of rejecting,
            // breaks this post-condition when we re-probe after every external state change.
            assertMath844Rejects(exactGuesser, "constructor-clone", "after external array mutation " + i + " at index " + idx);
        }

        try {
            org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer optimizer =
                new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer();
            HarmonicFitter fitter = new HarmonicFitter(optimizer);
            for (WeightedObservedPoint point : buildExactTriangularPoints(0, 1, 1)) {
                fitter.addObservedPoint(point);
            }
            try {
                fitter.fit();
            } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            }
            try {
                fitter.fit(new double[] { 1.0, 1.0, 0.0 });
            } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }

        HarmonicFitter.ParameterGuesser variedGuesser;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            int periods = data.consumeInt(2, 4);
            int extra = data.consumeInt(1, 7);
            WeightedObservedPoint[] variedPoints = buildGeneralTriangularPoints(start, step, scale, periods, extra);
            variedGuesser = new HarmonicFitter.ParameterGuesser(variedPoints);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        Throwable varied = probeGuesser(variedGuesser);
        if (varied == null) {
            throw new FuzzerSecurityIssueLow(
                "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but completed normally");
        }
        if (!(varied instanceof org.apache.commons.math3.exception.MathIllegalStateException)) {
            throw new FuzzerSecurityIssueLow(
                "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but got " +
                varied.getClass().getName(), varied);
        }
    }
}