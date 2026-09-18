package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        weightedObservedPointGettersRoundtrip(data);
        exactMath844Oracle();
        generalizedTriangularWaveOracle(data);
        constructorCloneAndGuessReadOnlyOracle(data);
    }

    private static void weightedObservedPointGettersRoundtrip(FuzzedDataProvider data) {
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
                            + "weight=" + p.getWeight() + "/" + w
                            + " x=" + p.getX() + "/" + x
                            + " y=" + p.getY() + "/" + y);
        }
    }

    private static void exactMath844Oracle() {
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

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        try {
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but guess() completed normally");
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Trusted oracle copied verbatim from HarmonicFitterTest.testMath844:
            // this exact sample must be rejected as ill-conditioned/non-harmonic.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:testMath844-exact] semantic mismatch: expected MathIllegalStateException but got "
                            + t.getClass().getName(), t);
        }
    }

    private static void generalizedTriangularWaveOracle(FuzzedDataProvider data) {
        final int scale = data.consumeInt(1, 5);
        final int step = data.consumeInt(1, 4);
        final int start = data.consumeInt(-20, 20);

        final int[] base = {
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1,
                0, -1, -2, -3, -2, -1,
                0, 1, 2, 3, 2, 1, 0
        };

        final WeightedObservedPoint[] points = new WeightedObservedPoint[base.length];
        for (int i = 0; i < base.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, start + (double) i * step, base[i] * (double) scale);
        }

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        try {
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but completed normally"
                            + " scale=" + scale + " step=" + step + " start=" + start);
        } catch (org.apache.commons.math3.exception.MathIllegalStateException expected) {
            // Equivalent-input oracle: the trusted failing test shows this triangular wave shape
            // is far from harmonic and must be rejected; translating x, rescaling x by a
            // positive step, and scaling y by a positive factor preserve that ill-conditioned
            // shape, so a correct implementation must still reject rather than synthesize coefficients.
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "relation guesser_rejects_triangular_wave violated: expected MathIllegalStateException, but got "
                            + t.getClass().getName()
                            + " scale=" + scale + " step=" + step + " start=" + start, t);
        }
    }

    private static void constructorCloneAndGuessReadOnlyOracle(FuzzedDataProvider data) {
        final int n = data.consumeInt(8, 32);
        final int start = data.consumeInt(-20, 20);
        final int step = data.consumeInt(1, 3);
        final int amplitudeInt = data.consumeInt(1, 5);
        final double amplitude = (double) amplitudeInt;
        final int period = data.consumeInt(6, 18);
        final double omega = 2.0 * Math.PI / period;
        final int phiChoice = data.consumeInt(0, 3);
        final double phi;
        if (phiChoice == 0) {
            phi = 0.0;
        } else if (phiChoice == 1) {
            phi = Math.PI / 6.0;
        } else if (phiChoice == 2) {
            phi = Math.PI / 4.0;
        } else {
            phi = Math.PI / 3.0;
        }

        final WeightedObservedPoint[] points = new WeightedObservedPoint[n];
        for (int i = 0; i < n; i++) {
            final double x = start + (double) i * step;
            final double y = amplitude * Math.cos(omega * x + phi);
            points[i] = new WeightedObservedPoint(1.0, x, y);
        }

        final HarmonicFitter.ParameterGuesser guesser;
        try {
            guesser = new HarmonicFitter.ParameterGuesser(points);
        } catch (Throwable t) {
            return;
        }

        final double[] before;
        try {
            before = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        final int mutations = data.consumeInt(1, Math.max(1, n));
        for (int i = 0; i < mutations; i++) {
            int idx = data.consumeInt(0, n - 1);
            double mx = data.consumeInt(-50, 50);
            double my = data.consumeInt(-50, 50);
            double mw = data.consumeBoolean() ? 1.0 : 2.0;
            points[idx] = new WeightedObservedPoint(mw, mx, my);
        }

        final double[] after;
        try {
            after = guesser.guess();
        } catch (Throwable t) {
            return;
        }

        // Contract justification:
        // (1) ParameterGuesser(WeightedObservedPoint[] observations) stores observations.clone(),
        // so later mutations to the caller's array must not change the guesser's state.
        // (2) guess() is a read-only query over the stored observations; a throw-deleting or
        // otherwise state-corrupting patch in guessAOmega/guess() would make repeated reads disagree.
        if (!sameArray(before, after)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-clone-readonly] metamorphic violation: repeated guess() on unchanged receiver disagreed after caller-array mutation"
                            + " before=" + formatArray(before)
                            + " after=" + formatArray(after)
                            + " n=" + n
                            + " start=" + start
                            + " step=" + step
                            + " amplitude=" + amplitude
                            + " period=" + period
                            + " phi=" + phi
                            + " mutations=" + mutations);
        }
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
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

    private static String formatArray(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}