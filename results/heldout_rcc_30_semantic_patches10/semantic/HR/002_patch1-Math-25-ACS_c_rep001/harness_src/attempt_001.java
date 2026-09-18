package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final double[] TRIANGULAR_Y = new double[] {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // WeightedObservedPoint is an immutable container; getters must report exactly
        // the values established by the constructor. This is a direct reader/writer check
        // on shared state and is sound for every correct implementation.
        {
            double w = finiteFromInt(data.consumeInt(-1_000_000, 1_000_000));
            double x = finiteFromInt(data.consumeInt(-1_000_000, 1_000_000));
            double y = finiteFromInt(data.consumeInt(-1_000_000, 1_000_000));
            WeightedObservedPoint p;
            try {
                p = new WeightedObservedPoint(w, x, y);
            } catch (Throwable t) {
                return;
            }
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(w)
                    || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(x)
                    || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(y)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:weighted-point-getters] semantic mismatch: expected=("
                                + w + "," + x + "," + y + ") actual=("
                                + p.getWeight() + "," + p.getX() + "," + p.getY() + ")");
            }
        }

        // Lifted oracle from HarmonicFitterTest.testMath844:
        // for this exact triangular sample, guess() must throw MathIllegalStateException.
        // Buggy builds return normally instead.
        {
            WeightedObservedPoint[] points = buildTriangularPoints(1.0);
            HarmonicFitter.ParameterGuesser g;
            try {
                g = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String got = "completed normally";
            Throwable caught = null;
            try {
                g.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
                // expected
            } catch (Throwable t) {
                violated = true;
                got = "wrong exception " + t.getClass().getName();
                caught = t;
            }
            if (violated) {
                if (caught != null) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException, got " + got,
                            caught);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math844-seed] semantic mismatch: expected MathIllegalStateException, got " + got);
            }
        }

        // Equivalent-input family from the same root cause:
        // guessAOmega reads only getX()/getY() from observations; weights are ignored on this path.
        // Therefore changing only the weights of the exact MATH-844 sample must preserve the same
        // MathIllegalStateException outcome. A throw-deleting or reachability-masking patch breaks this.
        {
            double chosenWeight = finiteFromInt(data.consumeInt(-1000, 1000));
            WeightedObservedPoint[] points = buildTriangularPoints(chosenWeight);
            HarmonicFitter.ParameterGuesser g;
            try {
                g = new HarmonicFitter.ParameterGuesser(points);
            } catch (Throwable t) {
                return;
            }

            boolean violated = false;
            String got = "completed normally";
            Throwable caught = null;
            try {
                g.guess();
                violated = true;
            } catch (MathIllegalStateException expected) {
                // expected
            } catch (Throwable t) {
                violated = true;
                got = "wrong exception " + t.getClass().getName();
                caught = t;
            }
            if (violated) {
                if (caught != null) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:math844-weight-equivalence] semantic mismatch: weight="
                                    + chosenWeight + " expected MathIllegalStateException, got " + got,
                            caught);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math844-weight-equivalence] semantic mismatch: weight="
                                + chosenWeight + " expected MathIllegalStateException, got " + got);
            }
        }

        // Mandatory consistency cross-check on the reachable helper path:
        // guess() sorts observations with respect to abscissa before computing a/omega/phi, so two
        // ParameterGuessers built from the same logical sample in different orders must agree exactly.
        // This checks a helper-visible quantity (guessed parameters) two independent ways and catches
        // masked helper defects even if a later step might absorb them.
        {
            int len = data.consumeInt(6, 20);
            double amplitude = 0.5 + (data.consumeInt(0, 9500) / 1000.0);
            double omega = 0.1 + (data.consumeInt(0, 2900) / 1000.0);
            double phase = -Math.PI + (2.0 * Math.PI * data.consumeInt(0, 10_000) / 10_000.0);
            double x0 = data.consumeInt(-1000, 1000) / 10.0;
            double step = 0.1 + (data.consumeInt(1, 1000) / 100.0);

            HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
            WeightedObservedPoint[] ordered = new WeightedObservedPoint[len];
            WeightedObservedPoint[] reversed = new WeightedObservedPoint[len];
            for (int i = 0; i < len; i++) {
                double x = x0 + i * step;
                double y = osc.value(x);
                ordered[i] = new WeightedObservedPoint(1.0, x, y);
                reversed[len - 1 - i] = new WeightedObservedPoint(1.0, x, y);
            }

            double[] guessOrdered;
            double[] guessReversed;
            try {
                guessOrdered = new HarmonicFitter.ParameterGuesser(ordered).guess();
                guessReversed = new HarmonicFitter.ParameterGuesser(reversed).guess();
            } catch (Throwable t) {
                return;
            }

            if (!sameBitsArray(guessOrdered, guessReversed)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:order-invariance] consistency violation: ordered="
                                + arrayToString(guessOrdered) + " reversed=" + arrayToString(guessReversed));
            }
        }
    }

    private static WeightedObservedPoint[] buildTriangularPoints(double weight) {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(weight, i, TRIANGULAR_Y[i]);
        }
        return points;
    }

    private static double finiteFromInt(int v) {
        return (double) v;
    }

    private static boolean sameBitsArray(double[] a, double[] b) {
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

    private static String arrayToString(double[] a) {
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