package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] exactPoints = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            exactPoints[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter viaPublicApi =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < exactPoints.length; i++) {
                viaPublicApi.addObservedPoint(exactPoints[i]);
            }
            try {
                viaPublicApi.fit();
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }

        HarmonicFitter.ParameterGuesser liftedGuesser;
        try {
            liftedGuesser = new HarmonicFitter.ParameterGuesser(exactPoints);
        } catch (Throwable t) {
            return;
        }
        try {
            liftedGuesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected org.apache.commons.math3.exception.MathIllegalStateException but guess() completed normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected org.apache.commons.math3.exception.MathIllegalStateException but got " + t.getClass().getName(), t);
        }

        double w;
        double x;
        double yy;
        WeightedObservedPoint p;
        try {
            w = data.consumeInt(-1000, 1000) / 10.0;
            x = data.consumeInt(-1000, 1000) / 10.0;
            yy = data.consumeInt(-1000, 1000) / 10.0;
            p = new WeightedObservedPoint(w, x, yy);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(yy)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:wop-roundtrip] relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                    + "weight=" + p.getWeight() + "/" + w
                    + " x=" + p.getX() + "/" + x
                    + " y=" + p.getY() + "/" + yy);
        }

        HarmonicFitter.ParameterGuesser generalizedGuesser = null;
        boolean generalizedBuilt = false;
        Throwable generalizedCaught = null;
        try {
            final int scale = data.consumeInt(1, 5);
            final int step = data.consumeInt(1, 4);
            final int start = data.consumeInt(-20, 20);
            final int repeats = data.consumeInt(2, 4);
            final int[] period = { 0, 1, 2, 3, 2, 1, 0, -1, -2, -3, -2, -1 };
            final boolean appendTerminalZero = data.consumeBoolean();

            final int len = repeats * period.length + (appendTerminalZero ? 1 : 0);
            final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
            int idx = 0;
            for (int r = 0; r < repeats; r++) {
                for (int i = 0; i < period.length; i++) {
                    points[idx] = new WeightedObservedPoint(1.0, start + idx * (double) step, period[i] * (double) scale);
                    idx++;
                }
            }
            if (appendTerminalZero) {
                points[idx] = new WeightedObservedPoint(1.0, start + idx * (double) step, 0.0);
            }
            generalizedGuesser = new HarmonicFitter.ParameterGuesser(points);
            generalizedBuilt = true;
        } catch (Throwable t) {
            generalizedCaught = t;
        }
        if (generalizedCaught != null || !generalizedBuilt) {
            return;
        }
        boolean generalizedViolation = false;
        String generalizedDetail = "completed normally";
        Throwable generalizedCause = null;
        try {
            generalizedGuesser.guess();
            generalizedViolation = true;
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
        } catch (Throwable t) {
            generalizedViolation = true;
            generalizedDetail = "wrong exception class " + t.getClass().getName();
            generalizedCause = t;
        }
        if (generalizedViolation) {
            if (generalizedCause != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:triangular-wave] relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " + generalizedDetail,
                    generalizedCause);
            }
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:triangular-wave] relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but " + generalizedDetail);
        }

        try {
            final int n = data.consumeInt(8, 20);
            final int amplitudeInt = data.consumeInt(1, 5);
            final double amplitude = (double) amplitudeInt;
            final int startX = data.consumeInt(-10, 10);
            final double stepX = data.consumeInt(1, 3) / 2.0;
            final double omega = Math.PI / 6.0;
            final WeightedObservedPoint[] harmonic = new WeightedObservedPoint[n];
            final WeightedObservedPoint[] harmonicCopy = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                final double xi = startX + i * stepX;
                final double yi = amplitude * Math.cos(omega * xi);
                harmonic[i] = new WeightedObservedPoint(1.0, xi, yi);
                harmonicCopy[i] = harmonic[i];
            }

            final HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(harmonic);
            final double[] first = g1.guess();

            final int mutations = data.consumeInt(1, n);
            for (int m = 0; m < mutations; m++) {
                final int target = data.consumeInt(0, n - 1);
                final double mx = data.consumeInt(-50, 50);
                final double my = data.consumeInt(-50, 50);
                harmonic[target] = new WeightedObservedPoint(1.0, mx, my);
            }

            final double[] second = g1.guess();
            final HarmonicFitter.ParameterGuesser g2 = new HarmonicFitter.ParameterGuesser(harmonicCopy);
            final double[] third = g2.guess();

            boolean mismatch = false;
            for (int i = 0; i < first.length; i++) {
                if (Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(second[i])
                        || Double.doubleToLongBits(first[i]) != Double.doubleToLongBits(third[i])) {
                    mismatch = true;
                    break;
                }
            }
            if (mismatch) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone-reader] metamorphic violation: constructor clones the observations array (`this.observations = observations.clone()`), so replacing elements in the caller's array after construction must not change what guess() reports; also guess() is a no-arg reader and repeated calls should agree. first="
                        + java.util.Arrays.toString(first)
                        + " second=" + java.util.Arrays.toString(second)
                        + " third=" + java.util.Arrays.toString(third));
            }
        } catch (Throwable t) {
            if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            }
            return;
        }

        try {
            final int n = data.consumeInt(8, 18);
            final double amplitude = data.consumeInt(1, 4);
            final double omega = Math.PI / data.consumeInt(5, 10);
            final double phase = data.consumeInt(-3, 3) / 10.0;
            final double startX = data.consumeInt(-5, 5);
            final double stepX = data.consumeInt(1, 3) / 3.0;

            final WeightedObservedPoint[] obs = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                final double xi = startX + i * stepX;
                final double yi = amplitude * Math.cos(omega * xi + phase);
                obs[i] = new WeightedObservedPoint(1.0, xi, yi);
            }

            final HarmonicFitter.ParameterGuesser seedGuesser = new HarmonicFitter.ParameterGuesser(obs);
            final double[] initialGuess = seedGuesser.guess();

            final HarmonicFitter f1 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            final HarmonicFitter f2 =
                new HarmonicFitter(new org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer());
            for (int i = 0; i < obs.length; i++) {
                f1.addObservedPoint(obs[i]);
                f2.addObservedPoint(obs[i]);
            }

            final double[] lhs = f1.fit();
            final double[] rhs = f2.fit(initialGuess);

            boolean violated = false;
            for (int i = 0; i < lhs.length; i++) {
                final double diff = Math.abs(lhs[i] - rhs[i]);
                if (!(diff <= 1.0e-7 || (Double.isNaN(lhs[i]) && Double.isNaN(rhs[i])))) {
                    violated = true;
                    break;
                }
            }
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:fit-overloads] metamorphic violation: fit() and fit(double[] initialGuess) are sibling overloads for the same fitting job, and fit() computes a first guess internally; supplying that same real-library guess explicitly must agree. lhs="
                        + java.util.Arrays.toString(lhs)
                        + " rhs=" + java.util.Arrays.toString(rhs)
                        + " initialGuess=" + java.util.Arrays.toString(initialGuess));
            }
        } catch (Throwable t) {
            if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            }
            return;
        }
    }
}