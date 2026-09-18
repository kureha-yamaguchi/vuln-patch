package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Trusted invariant: WeightedObservedPoint is an immutable value holder,
        // so getters must return exactly the constructor arguments.
        double w = data.consumeInt(-1000, 1000) / 10.0;
        double x = data.consumeInt(-1000, 1000) / 10.0;
        double y = data.consumeInt(-1000, 1000) / 10.0;
        WeightedObservedPoint p;
        try {
            p = new WeightedObservedPoint(w, x, y);
        } catch (Throwable t) {
            return;
        }
        if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w) ||
            Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x) ||
            Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
            throw new FuzzerSecurityIssueLow(
                "relation weighted_observed_point_getters_roundtrip_constructor_args violated: " +
                "weight=" + p.getWeight() + "/" + w +
                " x=" + p.getX() + "/" + x +
                " y=" + p.getY() + "/" + y);
        }

        // Lifted oracle from HarmonicFitterTest.testMath844: this exact sample must
        // make ParameterGuesser.guess() throw MathIllegalStateException.
        boolean exactViolated = false;
        Throwable exactWrong = null;
        try {
            final double[] exactY = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
            };
            final WeightedObservedPoint[] exactPoints = new WeightedObservedPoint[exactY.length];
            for (int i = 0; i < exactY.length; i++) {
                exactPoints[i] = new WeightedObservedPoint(1.0, i, exactY[i]);
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(exactPoints);
            guesser.guess();
            exactViolated = true;
        } catch (MathIllegalStateException ok) {
        } catch (Throwable t) {
            exactViolated = true;
            exactWrong = t;
        }
        if (exactViolated) {
            if (exactWrong != null) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but got " +
                    exactWrong.getClass().getName(), exactWrong);
            }
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but guess() completed normally");
        }

        // Generalization of the trusted failing shape: same triangular wave, but with
        // fuzzed positive scale, x-origin, and x-step while keeping strictly increasing x.
        boolean triViolated = false;
        Throwable triWrong = null;
        try {
            int scale = data.consumeInt(1, 5);
            int step = data.consumeInt(1, 4);
            int start = data.consumeInt(-20, 20);
            final int[] base = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
            };
            final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
            for (int i = 0; i < base.length; i++) {
                points[i] = new WeightedObservedPoint(1.0, start + ((double) i * step), base[i] * (double) scale);
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            guesser.guess();
            triViolated = true;
        } catch (MathIllegalStateException ok) {
        } catch (Throwable t) {
            triViolated = true;
            triWrong = t;
        }
        if (triViolated) {
            if (triWrong != null) {
                throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but got " +
                    triWrong.getClass().getName(), triWrong);
            }
            throw new FuzzerSecurityIssueLow(
                "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException but guess() completed normally");
        }

        // Mandatory post-condition / hidden-state check:
        // ParameterGuesser's constructor clones the observations array ("this.observations = observations.clone();"),
        // and guess() is a reader of the already-established state (a, omega, phi). Therefore:
        //   (1) calling guess() twice without changing the guesser must return the same coefficients, and
        //   (2) mutating the caller's original array after construction must not change later guess() results.
        // A patch that silently skips the intended rejection and corrupts/stomps shared state can violate this
        // observable stability even when no exception is thrown.
        boolean cloneStateViolated = false;
        String cloneStateDetail = null;
        try {
            int n = data.consumeInt(8, 20);
            int step = data.consumeInt(1, 5);
            int start = data.consumeInt(-30, 30);
            int amplitude = data.consumeInt(1, 6);
            int omegaDen = data.consumeInt(2, 10);
            int phiNum = data.consumeInt(-6, 6);

            double a = amplitude;
            double omega = Math.PI / omegaDen;
            double phi = phiNum * (Math.PI / 8.0);

            HarmonicOscillator osc = new HarmonicOscillator(a, omega, phi);
            WeightedObservedPoint[] src = new WeightedObservedPoint[n];
            for (int i = 0; i < n; i++) {
                double xi = start + (double) i * step;
                src[i] = new WeightedObservedPoint(1.0, xi, osc.value(xi));
            }

            HarmonicFitter.ParameterGuesser stableGuesser = new HarmonicFitter.ParameterGuesser(src);
            double[] g1 = stableGuesser.guess();
            double[] g2 = stableGuesser.guess();

            if (!sameBits(g1, g2)) {
                cloneStateViolated = true;
                cloneStateDetail = "repeated guess() changed result first=" + formatArray(g1) + " second=" + formatArray(g2);
            }

            int mutateIndex = data.consumeInt(0, n - 1);
            src[mutateIndex] = new WeightedObservedPoint(
                7.0,
                src[mutateIndex].getX() + 123.0,
                src[mutateIndex].getY() - 456.0
            );

            double[] g3 = stableGuesser.guess();
            if (!cloneStateViolated && !sameBits(g1, g3)) {
                cloneStateViolated = true;
                cloneStateDetail = "mutating caller array changed guesser state before=" + formatArray(g1) + " after=" + formatArray(g3);
            }
        } catch (Throwable t) {
            return;
        }
        if (cloneStateViolated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:constructor-clone-state] metamorphic violation: " + cloneStateDetail);
        }
    }

    private static boolean sameBits(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (Double.doubleToLongBits(a[i]) != Double.doubleToLongBits(b[i])) {
                return false;
            }
        }
        return true;
    }

    private static String formatArray(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}