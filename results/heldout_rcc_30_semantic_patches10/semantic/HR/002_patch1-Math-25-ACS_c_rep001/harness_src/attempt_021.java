package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.util.FastMath;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        reachPatchedMath844Path();

        int n = data.consumeInt(6, 20);
        double amplitude = 0.5 + data.consumeInt(0, 200) / 40.0;
        double omega = 0.2 + data.consumeInt(0, 200) / 80.0;
        double phase = -FastMath.PI + (2.0 * FastMath.PI) * (data.consumeInt(0, 1000000) / 1000000.0);

        WeightedObservedPoint[] pointsPhase = buildExactHarmonicPoints(n, amplitude, omega, phase);
        WeightedObservedPoint[] pointsWrapped = buildExactHarmonicPoints(n, amplitude, omega, phase + 2.0 * FastMath.PI);

        if (!sameSamples(pointsPhase, pointsWrapped, 1.0e-9)) {
            return;
        }

        double[] guess1;
        double[] guess2;
        try {
            guess1 = new HarmonicFitter.ParameterGuesser(pointsPhase).guess();
            guess2 = new HarmonicFitter.ParameterGuesser(pointsWrapped).guess();
        } catch (Throwable t) {
            return;
        }

        if (guess1 == null || guess2 == null || guess1.length != 3 || guess2.length != 3) {
            return;
        }

        if (Double.isNaN(guess1[0]) || Double.isNaN(guess1[1]) || Double.isNaN(guess1[2]) ||
            Double.isNaN(guess2[0]) || Double.isNaN(guess2[1]) || Double.isNaN(guess2[2])) {
            return;
        }

        double ampTol = 1.0e-6 * FastMath.max(1.0, FastMath.max(FastMath.abs(guess1[0]), FastMath.abs(guess2[0])));
        double omegaTol = 1.0e-6 * FastMath.max(1.0, FastMath.max(FastMath.abs(guess1[1]), FastMath.abs(guess2[1])));
        double phaseTol = 1.0e-5;

        /* Contract justification:
         * HarmonicOscillator with phase φ and φ + 2π defines the same function,
         * so the two point sets above are observationally equivalent. ParameterGuesser.guess()
         * is a pure computation from the observations, hence two equivalent inputs must yield
         * equivalent guessed parameters (same amplitude and omega, phase equal modulo 2π).
         * A band-aid patch that merely suppresses the Math844 exception but leaves the c2/c3
         * bookkeeping inconsistent can still perturb these related quantities.
         */
        if (FastMath.abs(guess1[0] - guess2[0]) > ampTol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:phase-wrap-amplitude] metamorphic violation: equivalent phase-wrapped inputs changed amplitude " +
                "a1=" + guess1[0] + " a2=" + guess2[0] +
                " omega=" + omega + " phase=" + phase + " n=" + n);
        }

        if (FastMath.abs(guess1[1] - guess2[1]) > omegaTol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:phase-wrap-omega] metamorphic violation: equivalent phase-wrapped inputs changed omega " +
                "w1=" + guess1[1] + " w2=" + guess2[1] +
                " amplitude=" + amplitude + " phase=" + phase + " n=" + n);
        }

        double wrappedPhaseDiff = normalizeAngle(guess1[2] - guess2[2]);
        if (FastMath.abs(wrappedPhaseDiff) > phaseTol) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:phase-wrap-phase] metamorphic violation: equivalent phase-wrapped inputs changed phase modulo 2pi " +
                "p1=" + guess1[2] + " p2=" + guess2[2] + " normalizedDiff=" + wrappedPhaseDiff +
                " amplitude=" + amplitude + " omega=" + omega + " phase=" + phase + " n=" + n);
        }
    }

    private static void reachPatchedMath844Path() {
        final double[] y = {
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1,
            0, -1, -2, -3, -2, -1,
            0, 1, 2, 3, 2, 1, 0
        };
        final WeightedObservedPoint[] points = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }
        try {
            new HarmonicFitter.ParameterGuesser(points).guess();
        } catch (MathIllegalStateException expected) {
        } catch (Throwable ignored) {
        }
    }

    private static WeightedObservedPoint[] buildExactHarmonicPoints(int n, double amplitude, double omega, double phase) {
        HarmonicOscillator oscillator = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] points = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            double x = i;
            points[i] = new WeightedObservedPoint(1.0, x, oscillator.value(x));
        }
        return points;
    }

    private static boolean sameSamples(WeightedObservedPoint[] a, WeightedObservedPoint[] b, double tol) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i].getX() != b[i].getX()) {
                return false;
            }
            if (FastMath.abs(a[i].getY() - b[i].getY()) > tol) {
                return false;
            }
        }
        return true;
    }

    private static double normalizeAngle(double angle) {
        double twoPi = 2.0 * FastMath.PI;
        double r = angle % twoPi;
        if (r <= -FastMath.PI) {
            r += twoPi;
        } else if (r > FastMath.PI) {
            r -= twoPi;
        }
        return r;
    }
}