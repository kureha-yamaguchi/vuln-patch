package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final int[] TRIANGULAR_Y = new int[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    private static final class GuessProbe {
        final boolean completedNormally;
        final Throwable thrown;

        GuessProbe(boolean completedNormally, Throwable thrown) {
            this.completedNormally = completedNormally;
            this.thrown = thrown;
        }
    }

    private static WeightedObservedPoint[] buildTriangularPoints(int start, int step, int scale) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, TRIANGULAR_Y[i] * (double) scale);
        }
        return points;
    }

    private static GuessProbe probeGuess(HarmonicFitter.ParameterGuesser guesser) {
        try {
            guesser.guess();
            return new GuessProbe(true, null);
        } catch (Throwable t) {
            return new GuessProbe(false, t);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            double w = data.consumeInt(-1000, 1000) / 10.0;
            double x = data.consumeInt(-1000, 1000) / 10.0;
            double y = data.consumeInt(-1000, 1000) / 10.0;
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new FuzzerSecurityIssueLow(
                        "relation weighted_observed_point_getters_roundtrip_constructor_args violated: "
                                + "weight=" + p.getWeight() + " expectedWeight=" + w
                                + " x=" + p.getX() + " expectedX=" + x
                                + " y=" + p.getY() + " expectedY=" + y);
            }
        }

        {
            WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
            for (int i = 0; i < TRIANGULAR_Y.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, i, TRIANGULAR_Y[i]);
            }

            HarmonicFitter.ParameterGuesser guesser;
            try {
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            GuessProbe exact = probeGuess(guesser);
            if (exact.completedNormally) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, actual=completed normally");
            }
            if (!(exact.thrown instanceof MathIllegalStateException)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException, actual="
                                + exact.thrown.getClass().getName(),
                        exact.thrown);
            }
        }

        {
            WeightedObservedPoint[] points;
            HarmonicFitter.ParameterGuesser guesser;
            try {
                int scale = data.consumeInt(1, 5);
                int step = data.consumeInt(1, 4);
                int start = data.consumeInt(-20, 20);
                points = buildTriangularPoints(start, step, scale);
                guesser = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            GuessProbe before = probeGuess(guesser);

            int mutations = data.consumeInt(1, Math.max(1, points.length));
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, points.length - 1);
                double mw = data.consumeInt(-50, 50);
                double mx = data.consumeInt(-100, 100);
                double my = data.consumeInt(-100, 100);
                points[idx] = new WeightedObservedPoint(mw, mx, my);
            }

            GuessProbe after = probeGuess(guesser);

            if (before.completedNormally) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:guesser_rejects_triangular_wave] semantic mismatch: expected MathIllegalStateException before external-array mutation, actual=completed normally");
            }
            if (!(before.thrown instanceof MathIllegalStateException)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:guesser_rejects_triangular_wave] semantic mismatch: expected MathIllegalStateException before external-array mutation, actual="
                                + before.thrown.getClass().getName(),
                        before.thrown);
            }

            /*
             * Contract justification:
             * ParameterGuesser(WeightedObservedPoint[] observations) clones the input array
             * ("this.observations = observations.clone();"). Therefore, replacing elements in
             * the caller's original array after construction must not change what guess() sees.
             * For this trusted triangular-wave family, guess() must reject with
             * MathIllegalStateException both before and after those external mutations.
             * A throw-deleting patch or an aliasing/state bug breaks this observable agreement.
             */
            if (after.completedNormally) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone-postcondition] metamorphic violation: guess() rejected before mutation but completed normally after mutating the caller array");
            }
            if (!(after.thrown instanceof MathIllegalStateException)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-clone-postcondition] metamorphic violation: expected MathIllegalStateException after mutating the caller array, actual="
                                + after.thrown.getClass().getName(),
                        after.thrown);
            }
        }
    }
}