package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.analysis.function.HarmonicOscillator;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.ZeroException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double[] y = { 0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1,
                             0, -1, -2, -3, -2, -1,
                             0, 1, 2, 3, 2, 1, 0 };
        final int len = y.length;
        final WeightedObservedPoint[] points = new WeightedObservedPoint[len];
        for (int i = 0; i < len; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            double[] actual = guesser.guess();
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but guess() returned "
                    + java.util.Arrays.toString(actual));
        } catch (MathIllegalStateException expected) {
            // Lifted exactly from HarmonicFitterTest.testMath844:
            // this specific sample must throw MathIllegalStateException.
        } catch (Throwable other) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException but got "
                    + other.getClass().getName() + ": " + String.valueOf(other.getMessage()));
        }

        try {
            HarmonicFitter fitter = new HarmonicFitter(null);
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(1.0, i, y[i]);
            }
            fitter.fit();
        } catch (Throwable ignored) {
            // This public API call is only to ensure the patched path is also reached through HarmonicFitter.
        }

        int n = data.consumeInt(6, 20);
        double amplitude = data.consumeInt(1, 20);
        double omega = data.consumeInt(1, 10);
        double phase = data.consumeInt(-30, 30) / 10.0;
        double step = data.consumeInt(1, 10) / 10.0;

        HarmonicOscillator osc = new HarmonicOscillator(amplitude, omega, phase);
        WeightedObservedPoint[] ordered = new WeightedObservedPoint[n];
        WeightedObservedPoint[] permuted = new WeightedObservedPoint[n];
        double[] permutedXBefore = new double[n];
        double[] permutedYBefore = new double[n];
        double[] permutedWBefore = new double[n];

        for (int i = 0; i < n; i++) {
            double x = i * step;
            double val = osc.value(x);
            WeightedObservedPoint p = new WeightedObservedPoint(1.0, x, val);
            ordered[i] = p;
            permuted[i] = p;
            permutedXBefore[i] = p.getX();
            permutedYBefore[i] = p.getY();
            permutedWBefore[i] = p.getWeight();
        }

        for (int i = 0; i < n; i++) {
            int j = data.consumeInt(0, n - 1);
            WeightedObservedPoint tmp = permuted[i];
            permuted[i] = permuted[j];
            permuted[j] = tmp;
        }

        for (int i = 0; i < n; i++) {
            permutedXBefore[i] = permuted[i].getX();
            permutedYBefore[i] = permuted[i].getY();
            permutedWBefore[i] = permuted[i].getWeight();
        }

        try {
            HarmonicFitter.ParameterGuesser g1 = new HarmonicFitter.ParameterGuesser(ordered);
            HarmonicFitter.ParameterGuesser g2 = new HarmonicFitter.ParameterGuesser(permuted);

            double[] a1 = g1.guess();
            double[] a2 = g2.guess();

            // Contract justification:
            // ParameterGuesser stores a clone of the caller array in its constructor, and
            // sortObservations() sorts observations by abscissa internally. Therefore two arrays
            // containing the same points in different orders are equivalent inputs and must yield
            // the same guess. A patch that silently skips/changes the real computation can violate
            // this observable equivalence even when no exception is thrown.
            if (a1.length != a2.length) {
                throw new RuntimeException(
                    "[oracle:permutation-equivalence] metamorphic violation: different result lengths input=n=" + n
                        + " lhs=" + java.util.Arrays.toString(a1)
                        + " rhs=" + java.util.Arrays.toString(a2));
            }
            for (int i = 0; i < a1.length; i++) {
                if (Double.doubleToLongBits(a1[i]) != Double.doubleToLongBits(a2[i])) {
                    throw new RuntimeException(
                        "[oracle:permutation-equivalence] metamorphic violation: guess(points)==guess(permutation(points)) "
                            + "input=n=" + n
                            + ", amplitude=" + amplitude
                            + ", omega=" + omega
                            + ", phase=" + phase
                            + ", step=" + step
                            + " lhs=" + java.util.Arrays.toString(a1)
                            + " rhs=" + java.util.Arrays.toString(a2));
                }
            }

            // Contract justification:
            // The constructor clones the observations array, so calling guess() must not reorder or
            // replace elements in the caller-provided array.
            for (int i = 0; i < n; i++) {
                if (Double.doubleToLongBits(permuted[i].getX()) != Double.doubleToLongBits(permutedXBefore[i]) ||
                    Double.doubleToLongBits(permuted[i].getY()) != Double.doubleToLongBits(permutedYBefore[i]) ||
                    Double.doubleToLongBits(permuted[i].getWeight()) != Double.doubleToLongBits(permutedWBefore[i])) {
                    throw new RuntimeException(
                        "[oracle:caller-array-unchanged] metamorphic violation: guess() modified caller observations array "
                            + "at index=" + i);
                }
            }
        } catch (NumberIsTooSmallException ignored) {
            return;
        } catch (ZeroException ignored) {
            return;
        } catch (MathIllegalStateException ignored) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}