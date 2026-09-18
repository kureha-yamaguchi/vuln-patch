package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final int[] TRIANGULAR_BASE = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        relationWeightedObservedPointRoundtrip(data);
        oracleTriangularWaveMustThrowAndNotMutateCallerArray(data);
        oracleTriangularWaveViaPublicHarmonicFitter(data);
        relationFitOverloadsAgreeOnEquivalentInput(data);
        relationGuesserIsStableOnFixedObservations(data);
    }

    private static void relationWeightedObservedPointRoundtrip(FuzzedDataProvider data) {
        double w;
        double x;
        double y;
        WeightedObservedPoint p;
        try {
            w = data.consumeInt(-1000, 1000) / 10.0;
            x = data.consumeInt(-1000, 1000) / 10.0;
            y = data.consumeInt(-1000, 1000) / 10.0;
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }

        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                "weight=" + p.getWeight() + "/" + w +
                " x=" + p.getX() + "/" + x +
                " y=" + p.getY() + "/" + y);
        }
    }

    private static void oracleTriangularWaveMustThrowAndNotMutateCallerArray(FuzzedDataProvider data) {
        WeightedObservedPoint[] points;
        double[] originalXs;
        double[] originalYs;
        HarmonicFitter.ParameterGuesser guesser;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            points = new WeightedObservedPoint[TRIANGULAR_BASE.length];
            for (int i = 0; i < TRIANGULAR_BASE.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, TRIANGULAR_BASE[i] * (double) scale);
            }

            int swaps = data.consumeInt(0, TRIANGULAR_BASE.length * 2);
            for (int i = 0; i < swaps; i++) {
                int a = data.consumeInt(0, points.length - 1);
                int b = data.consumeInt(0, points.length - 1);
                WeightedObservedPoint tmp = points[a];
                points[a] = points[b];
                points[b] = tmp;
            }

            originalXs = new double[points.length];
            originalYs = new double[points.length];
            for (int i = 0; i < points.length; i++) {
                originalXs[i] = points[i].getX();
                originalYs[i] = points[i].getY();
            }

            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        Throwable wrong = null;
        try {
            guesser.guess();
            violated = true;
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            violated = true;
            wrong = t;
        }

        if (violated) {
            String msg = "[oracle:math844-guesser] semantic mismatch: expected MathIllegalStateException for the triangular-wave sample";
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    msg + ", but caught " + wrong.getClass().getName(), wrong);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                msg + ", but guess() completed normally");
        }

        boolean mutated = false;
        int mutatedIndex = -1;
        for (int i = 0; i < points.length; i++) {
            if (Double.doubleToLongBits(points[i].getX()) != Double.doubleToLongBits(originalXs[i]) ||
                Double.doubleToLongBits(points[i].getY()) != Double.doubleToLongBits(originalYs[i])) {
                mutated = true;
                mutatedIndex = i;
                break;
            }
        }
        if (mutated) {
            // Contract justification: ParameterGuesser constructor clones the provided array
            // (`this.observations = observations.clone();`), so later sorting/guessing must not
            // mutate the caller's array. A patch that merely deletes the throw but reuses/mutates
            // caller state would violate this observable post-condition.
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:constructor-clone-postcondition] semantic mismatch: caller array mutated at index " +
                mutatedIndex + " beforeX=" + originalXs[mutatedIndex] +
                " afterX=" + points[mutatedIndex].getX() +
                " beforeY=" + originalYs[mutatedIndex] +
                " afterY=" + points[mutatedIndex].getY());
        }
    }

    private static void oracleTriangularWaveViaPublicHarmonicFitter(FuzzedDataProvider data) {
        HarmonicFitter fitter;
        try {
            fitter = new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);

            int[] order = new int[TRIANGULAR_BASE.length];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            int swaps = data.consumeInt(0, order.length * 2);
            for (int i = 0; i < swaps; i++) {
                int a = data.consumeInt(0, order.length - 1);
                int b = data.consumeInt(0, order.length - 1);
                int tmp = order[a];
                order[a] = order[b];
                order[b] = tmp;
            }

            for (int i = 0; i < order.length; i++) {
                int idx = order[i];
                fitter.addObservedPoint(1.0, start + (double) idx * step, TRIANGULAR_BASE[idx] * (double) scale);
            }
        } catch (Throwable t) {
            return;
        }

        boolean violated = false;
        Throwable wrong = null;
        try {
            fitter.fit();
            violated = true;
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            violated = true;
            wrong = t;
        }

        if (violated) {
            String msg = "[oracle:math844-public-fit] semantic mismatch: HarmonicFitter.fit() should reach ParameterGuesser and reject the same triangular-wave sample with MathIllegalStateException";
            if (wrong != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    msg + ", but caught " + wrong.getClass().getName(), wrong);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                msg + ", but fit() completed normally");
        }
    }

    private static void relationFitOverloadsAgreeOnEquivalentInput(FuzzedDataProvider data) {
        WeightedObservedPoint[] points;
        double[] initialGuess;
        double[] fitWithoutGuess;
        double[] fitWithGuess;
        try {
            int n = data.consumeInt(8, 20);
            double a = data.consumeInt(1, 5);
            double omega = data.consumeInt(2, 12) / 10.0;
            double phi = data.consumeInt(-10, 10) / 10.0;
            double step = data.consumeInt(1, 10) / 10.0;
            double start = data.consumeInt(-20, 20) / 10.0;

            org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(a, omega, phi);
            points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
            }

            initialGuess = new HarmonicFitter.ParameterGuesser(points).guess();

            HarmonicFitter fitter1 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            HarmonicFitter fitter2 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < points.length; i++) {
                fitter1.addObservedPoint(points[i]);
                fitter2.addObservedPoint(points[i]);
            }

            fitWithoutGuess = fitter1.fit();
            fitWithGuess = fitter2.fit(initialGuess);
        } catch (Throwable t) {
            return;
        }

        if (!sameArrayWithin(fitWithoutGuess, fitWithGuess, 1.0e-8)) {
            // Contract justification: the no-arg fit() overload is the same logical operation as
            // fit(double[] initialGuess), with the initial guess supplied by the library's own
            // ParameterGuesser. On the same observations, these sibling overloads must agree.
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:fit-overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagreed " +
                "lhs=" + formatArray(fitWithoutGuess) +
                " rhs=" + formatArray(fitWithGuess) +
                " seedGuess=" + formatArray(initialGuess));
        }
    }

    private static void relationGuesserIsStableOnFixedObservations(FuzzedDataProvider data) {
        WeightedObservedPoint[] points;
        double[] g1;
        double[] g2;
        try {
            int n = data.consumeInt(8, 24);
            double a = data.consumeInt(1, 5);
            double omega = data.consumeInt(2, 12) / 10.0;
            double phi = data.consumeInt(-10, 10) / 10.0;
            double step = data.consumeInt(1, 10) / 10.0;
            double start = data.consumeInt(-20, 20) / 10.0;

            org.apache.commons.math3.analysis.function.HarmonicOscillator osc =
                new org.apache.commons.math3.analysis.function.HarmonicOscillator(a, omega, phi);
            points = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                points[i] = new WeightedObservedPoint(1.0, x, osc.value(x));
            }

            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            g1 = guesser.guess();
            g2 = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        if (!sameArrayWithin(g1, g2, 1.0e-12)) {
            // Contract justification: guess() is a reader-style estimation method over fixed
            // observations; with no state change to the observations between calls, repeating it
            // must report the same coefficients. This hidden-state check catches silent corruption
            // even if no exception is thrown.
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:guess-idempotence] metamorphic violation: repeated guess() calls on the same observations disagreed " +
                "first=" + formatArray(g1) + " second=" + formatArray(g2));
        }
    }

    private static boolean sameArrayWithin(double[] a, double[] b, double tol) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.isNaN(a[i]) || Double.isNaN(b[i])) {
                return false;
            }
            if (Math.abs(a[i] - b[i]) > tol) {
                return false;
            }
        }
        return true;
    }

    private static String formatArray(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}